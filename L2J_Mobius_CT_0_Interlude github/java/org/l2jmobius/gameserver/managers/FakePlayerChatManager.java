/*
 * Copyright (c) 2013 L2jMobius
 * ... (license header unchanged) ...
 */
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.managers.PhantomManager.PartyRole;
import org.l2jmobius.gameserver.managers.PhantomManager.Recruit;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.holders.FakePlayerChatHolder;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerStoreItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.L2FriendSay;

/**
 * @author Mobius
 */
public class FakePlayerChatManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerChatManager.class.getName());
	
	private static final List<FakePlayerChatHolder> MESSAGES = new ArrayList<>();
	private static final int MIN_DELAY = 5000;
	private static final int MAX_DELAY = 15000;
	// Friend PMs are a direct 1:1 chat, so they use a much shorter "read + react" pause than the ambient
	// whisper delays above - a friend replies in a beat or two, then the length-scaled typing time is added
	// on top (typingDelayMillis). This lands a reply in roughly 1-4s, the way a person actually messages back.
	private static final int FRIEND_THINK_MIN = 800;
	private static final int FRIEND_THINK_MAX = 2600;
	
	// LLM bridge
	private static final HttpClient BRAIN_HTTP = HttpClient.newHttpClient();
	private static final String BRAIN_URL = "http://127.0.0.1:5000/chat";
	// Per-request timeout waiting for the brain's reply. A local Ollama model can take ~30s per generation, and
	// the old 20s cap silently dropped every reply (Java gave up, then the brain returned 200 to nobody), which
	// looked like "bots never reply in-game". Sized to outlast a slow local model; a fast provider (DeepSeek) or
	// a smaller Ollama model returns well under this, so it only ever bites when generation is genuinely slow.
	private static final int BRAIN_TIMEOUT_SECONDS = 45;

	// FPC-020: a dedicated, bounded executor for the blocking brain HTTP calls, kept OFF the shared game ThreadPool.
	// FPC-020: blocking brain HTTP must never run on the shared game ThreadPool (a slow or unreachable brain would
	// occupy threads that combat AI, buff ticks and respawns also need). All blocking brain work is handed to the
	// shared BrainExecutor (a bounded, daemon-threaded pool shared by every brain client); the game ThreadPool is
	// used only for the non-blocking "thinking" delay before hand-off.
	// Thin delegators to the shared BrainExecutor so the many call sites in this class stay readable.
	private static void runBrainWork(Runnable work)
	{
		BrainExecutor.runBrainWork(work);
	}

	private static void scheduleBrainWork(Runnable work, long delayMs)
	{
		BrainExecutor.scheduleBrainWork(work, delayMs);
	}

	// As above, but with a guaranteed completion callback (used to release the next conversation turn, FPC-064).
	private static void scheduleBrainWork(Runnable work, long delayMs, Runnable onComplete)
	{
		ThreadPool.schedule(() -> BrainExecutor.runBrainWork(work, onComplete), delayMs);
	}

	/**
	 * FPC-021: atomically reserve one unit of a per-minute cap BEFORE starting expensive brain work. A plain
	 * check-then-increment (get() &gt;= cap, then a later incrementAndGet()) let several concurrent brain workers all
	 * observe free capacity before any of them counted, so the configured cap did not actually bound concurrent
	 * model calls. This compare-and-set reserve either takes a slot or reports the cap is full, with no window in
	 * between.
	 * <p>
	 * A reserved slot is NEVER refunded (the "count admitted attempts" policy). An earlier version released a slot when
	 * the brain returned nothing to say, but a release that crossed the per-minute reset refunded a slot the NEW window
	 * owned, so a window could still over-admit (the counter carried no window identity to make a refund safe). Not
	 * refunding removes that cross-window race entirely and also bounds brain load directly: an attempt that produces
	 * no line still consumes its slot, which throttles pointless retries while a provider is misbehaving. The cost is
	 * that a rare "chose to stay silent" reply also consumes a slot, which the per-minute reset clears.
	 * @param counter the per-minute counter
	 * @param cap the configured maximum
	 * @return {@code true} if a slot was reserved (caller may proceed), {@code false} if the cap is already full
	 */
	private static boolean tryReserve(AtomicInteger counter, int cap)
	{
		int current = counter.get();
		while (current < cap)
		{
			if (counter.compareAndSet(current, current + 1))
			{
				return true;
			}
			current = counter.get();
		}
		return false;
	}

	// ===== Social tuning knobs =====
	private static final boolean SOCIAL_ENABLED = true;
	private static final int REPLY_CHANCE_TO_PLAYER = 50; // % chance a nearby bot reacts to a player (throttle 1)
	private static final int MAX_REPLIERS = 2; // at most N bots answer one line
	// Bot-to-bot chain leash: a message a human (or an ambient seed) started can be answered by a bot, and that
	// answer by one more bot, but the chain stops at the configured depth. Without this a single line spirals into a
	// self-sustaining echo loop ("xD" -> "lol classic" -> "seriously 😂" ...) until the per-minute cap runs out.
	// Ambient timers still seed fresh chatter independently, so idle chat stays alive; only the runaway depth is
	// capped. The depth and the bot-to-bot reply chance are data-driven (FakePlayersConfig), read live so a
	// //reload config applies them: lower them if bots chatter among themselves too much, raise for a livelier feel.
	// A bot should not seed a reply chain off a content-free line (pure laughter, an emote, a one-word ack). These
	// tokens are stripped when deciding whether a line carries enough substance to be worth reacting to.
	private static final Set<String> LOW_CONTENT_TOKENS = Set.of(
		"lol", "lmao", "lmfao", "lmf", "rofl", "haha", "hah", "hehe", "heh", "xd", "xdd", "xddd", "p", "d",
		"gg", "ez", "ya", "yea", "yeah", "ye", "yep", "yup", "nah", "na", "same", "ikr", "fr", "tru", "true",
		"nice", "cool", "wow", "oof", "bruh", "bro", "dude", "mate", "classic", "seriously", "duh", "sup",
		"hi", "hey", "hello", "yo", "ty", "np", "gl", "hf", "gj", "o", "u", "ur", "r");
	private static final int REPLY_STAGGER_MS = 4000; // each extra replier waits this much longer, so a 2nd bot
	// clearly chimes in AFTER the first instead of a simultaneous chorus (the most bot-like tell on a channel).
	// Rough human "typing" time for a chat line: a short pause that scales with length, so a one-word reply pops
	// out fast and a long sentence takes a beat longer - bots no longer all answer at one fixed instant speed.
	private static final long TYPE_BASE_MS = 400;
	private static final long TYPE_PER_CHAR_MS = 45;
	private static final long TYPE_MAX_MS = 4000;
	private static final int SOCIAL_RANGE = 3000; // trade: how close a bot must be to react
	private static final int SAY_RANGE = 1250; // say: local hearing/broadcast range
	// Ambient chatter cadence is data-driven (Custom/FakePlayers.ini, editable in the Config Editor's Fake Players
	// panel) rather than hardcoded: the public rate cap and the two spontaneous-chatter intervals come from
	// FakePlayersConfig. The rate cap is read live at each use, so a //reload config applies it; the two intervals are
	// read once when the timers are scheduled at startup (see startSocial), so a change to them applies on restart.
	private static final int MAX_TRADE_OFFERS_PER_MINUTE = 20; // important WTB/WTS responder PM cap
	private static final AtomicInteger MESSAGES_THIS_MINUTE = new AtomicInteger();
	private static final AtomicInteger TRADE_OFFERS_THIS_MINUTE = new AtomicInteger();
	private static boolean SOCIAL_STARTED = false;
	// Base ambient intervals in ms, captured once at startup from config; each cycle self-reschedules with jitter.
	private static long AMBIENT_TRADE_BASE_MS;
	private static long AMBIENT_SHOUT_BASE_MS;

	// Structured deal context kept while a trade responder is negotiating with a player.
	// Keyed by playerName|botName so follow-up whispers can include the same exact item/count/price.
	private static final Map<String, BrainDealContext> ACTIVE_DEALS = new ConcurrentHashMap<>();

	private static class BrainDealContext
	{
		final String side; // Bot perspective: SELL means bot sells to player, BUY means bot buys from player.
		final String item;
		final int itemId;
		final int count;
		final int unitPrice;
		final long totalPrice;
		// A stackable deal where the player named no amount: the bot asks "how many?" and we hold off on a firm
		// count until they answer. Cleared once the player states an amount (see the whisper handler).
		final boolean needsCount;
		// True once Java has deterministically parsed AND accepted the player's counteroffer (see
		// maybeHandleCounterOffer). From then on unitPrice is server-authoritative and the brain's SHOP-tag price is
		// ignored, so a weak model that re-quotes the pre-haggle price (or any plausible-but-stale number) cannot set
		// the charge. Setting this means "the bot agreed to this price", not merely "the player asked for it".
		final boolean priceLocked;
		// The outcome of the player's most recent price counteroffer, decided by Java: "ACCEPT" (unitPrice is now the
		// agreed price), "REJECT" (too far from the bot's ask; unitPrice still holds the bot's price), or "" (no
		// counter this turn). Passed to the brain so the bot SAYS the decision instead of making it.
		final String counterDecision;
		// The unit price the player last proposed, so the brain can name it ("you wanted 10k, but ...") when refusing.
		final int lastCounter;

		BrainDealContext(String side, String item, int itemId, int count, int unitPrice, boolean needsCount)
		{
			this(side, item, itemId, count, unitPrice, needsCount, false, "", 0);
		}

		BrainDealContext(String side, String item, int itemId, int count, int unitPrice, boolean needsCount, boolean priceLocked)
		{
			this(side, item, itemId, count, unitPrice, needsCount, priceLocked, "", 0);
		}

		BrainDealContext(String side, String item, int itemId, int count, int unitPrice, boolean needsCount, boolean priceLocked, String counterDecision, int lastCounter)
		{
			this.side = side;
			this.item = item;
			this.itemId = itemId;
			this.count = Math.max(1, count);
			this.unitPrice = Math.max(1, unitPrice);
			this.needsCount = needsCount;
			this.priceLocked = priceLocked;
			this.counterDecision = (counterDecision == null) ? "" : counterDecision;
			this.lastCounter = Math.max(0, lastCounter);
			totalPrice = (long) this.count * this.unitPrice;
		}
	}

	/**
	 * A bot's real self-knowledge (level, class, race, gear grade), sent to the brain so it answers "what lvl/class
	 * are you?" truthfully instead of inventing an impossible value (an Interlude character tops out at level 80).
	 * Everything is best-effort: any field the source can't provide is left blank and the brain simply stays vague
	 * about it. Fields are display-ready strings so the brain can drop them straight into the prompt.
	 */
	private static final class BotIdentity
	{
		final String level;
		final String clazz;
		final String race;
		final String gear;

		private BotIdentity(String level, String clazz, String race, String gear)
		{
			this.level = level;
			this.clazz = clazz;
			this.race = race;
			this.gear = gear;
		}

		boolean isEmpty()
		{
			return level.isEmpty() && clazz.isEmpty() && race.isEmpty() && gear.isEmpty();
		}

		/** Identity for an NPC fake player, read from its generated {@link FakePlayerAppearance}. */
		static BotIdentity of(Npc bot)
		{
			if (bot == null)
			{
				return null;
			}
			final FakePlayerAppearance look = bot.getFakePlayerAppearance();
			if (look == null)
			{
				return null;
			}
			final int level = look.getLevel();
			return new BotIdentity(level > 1 ? Integer.toString(level) : "", titleCase(look.getPlayerClass() == null ? "" : look.getPlayerClass().name()), raceName(look.getRace()), npcGearGrade(look));
		}

		/** Identity for a clientless {@link Player} (a regular friend, buddy, or recruited party member). */
		static BotIdentity of(Player player)
		{
			if (player == null)
			{
				return null;
			}
			return new BotIdentity(Integer.toString(player.getLevel()), titleCase(player.getPlayerClass() == null ? "" : player.getPlayerClass().name()), raceName(player.getRace()), playerGearGrade(player));
		}
	}

	/** "DARK_ELF" -&gt; "Dark Elf"; blank stays blank. Shared by race and class formatting. */
	private static String titleCase(String enumName)
	{
		if ((enumName == null) || enumName.isEmpty())
		{
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		for (String word : enumName.toLowerCase().split("_"))
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return sb.toString();
	}

	private static String raceName(Race race)
	{
		return race == null ? "" : titleCase(race.name());
	}

	/** The highest gear grade an NPC visibly wears (weapon vs. chest), e.g. "C grade"; blank if no-grade/unknown. */
	private static String npcGearGrade(FakePlayerAppearance look)
	{
		return gradeLabel(Math.max(crystalOrdinal(look.getEquipRHand()), crystalOrdinal(look.getEquipChest())));
	}

	/** The grade of a real phantom's equipped weapon, e.g. "C grade"; blank if unarmed/no-grade. */
	private static String playerGearGrade(Player player)
	{
		final ItemTemplate weapon = player.getActiveWeaponItem();
		return weapon == null ? "" : gradeLabel(weapon.getCrystalType().ordinal());
	}

	private static int crystalOrdinal(int itemId)
	{
		if (itemId <= 0)
		{
			return 0;
		}
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return template == null ? 0 : template.getCrystalType().ordinal();
	}

	/** CrystalType ordinal (NONE, D, C, B, A, S) -&gt; a readable grade label, blank for no-grade. */
	private static String gradeLabel(int ordinal)
	{
		switch (ordinal)
		{
			case 1:
			{
				return "D grade";
			}
			case 2:
			{
				return "C grade";
			}
			case 3:
			{
				return "B grade";
			}
			case 4:
			{
				return "A grade";
			}
			case 0:
			{
				return "";
			}
			default:
			{
				return "S grade";
			}
		}
	}

	// Control-tag patterns live in FakePlayerChatParsing (unit-tested there); aliased so call sites are unchanged.
	private static final Pattern MEET_TAG = FakePlayerChatParsing.MEET_TAG;
	private static final Pattern SHOP_TAG = FakePlayerChatParsing.SHOP_TAG;

	// Datapack-verified town centres, so a bot can truthfully say where it is when asked.
	private static final String[] TOWN_NAMES =
	{
		"Talking Island", "Gludin", "Gludio", "Dion", "Giran", "Oren", "Aden", "Heine",
		"Goddard", "Rune", "Schuttgart", "the Elven Village", "the Dark Elf Village",
		"the Dwarven Village", "the Orc Village"
	};
	private static final int[][] TOWN_COORDS =
	{
		{ -83990, 243336 }, { -83520, 150560 }, { -14288, 122752 }, { 15670, 142980 }, { 83400, 147600 },
		{ 82200, 53500 }, { 146680, 25800 }, { 111360, 220890 }, { 147300, -56570 }, { 43800, -47700 },
		{ 87386, -143246 }, { 46926, 51511 }, { 12501, 16768 }, { 115072, -178176 }, { -44316, -113136 }
	};
	
	protected FakePlayerChatManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED)
		{
			FakePlayerData.getInstance().report();
			if (FakePlayersConfig.FAKE_PLAYER_CHAT)
			{
				MESSAGES.clear();
				parseDatapackFile("data/FakePlayerChatData.xml");
				LOGGER.info(getClass().getSimpleName() + ": Loaded " + MESSAGES.size() + " chat templates.");
				startSocial();
			}
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "fakePlayerChat", fakePlayerChatNode ->
		{
			final StatSet set = new StatSet(parseAttributes(fakePlayerChatNode));
			MESSAGES.add(new FakePlayerChatHolder(set.getString("fpcName"), set.getString("searchMethod"), set.getString("searchText"), set.getString("answers")));
		}));
	}
	
	public void manageChat(Player player, String fpcName, String message)
	{
		enqueuePrivateTurn(player, fpcName, message, MIN_DELAY, MAX_DELAY);
	}

	public void manageChat(Player player, String fpcName, String message, int minDelay, int maxDelay)
	{
		enqueuePrivateTurn(player, fpcName, message, minDelay, maxDelay);
	}

	// FPC-059/FPC-064: serialize private whisper turns per (player, bot) BEFORE the brain request starts, through the
	// shared BrainConversationExecutor (the same mechanism PARTY/BUDDY/FRIEND use). Each whisper was scheduled with its
	// own random "thinking" delay, so two quick messages could reach the brain (and the shared conversation history)
	// out of send order; the Python per-conversation lock only orders by arrival. With one outstanding brain request
	// per conversation here, turns run in send order, a slow turn never leaves a later one waiting inside Python, and
	// the executor bounds/ages the queue so a message cannot sit for minutes and then run as if fresh (FPC-063).
	private void enqueuePrivateTurn(Player player, String fpcName, String message, int minDelay, int maxDelay)
	{
		if (player == null)
		{
			return;
		}
		final String key = BrainConversationExecutor.key(player.getName(), fpcName);
		// The turn keeps its own human-like delay (a non-blocking timer), then the blocking brain call runs on the
		// shared brain executor with a guaranteed completion callback that releases the next queued turn - even if this
		// turn is shed under load, so the conversation can never stall behind a dropped reply.
		BrainConversationExecutor.submit(key, onComplete -> ThreadPool.schedule( //
			() -> BrainExecutor.runBrainWork(() -> manageResponce(player, fpcName, message), onComplete), //
			Rnd.get(minDelay, maxDelay)));
	}

	private void manageResponce(Player player, String fpcName, String message)
	{
		if (player == null)
		{
			return;
		}

		// Static AFK store vendors are treated as offline shops: they do not answer whispers. A bot running a
		// temporary deal store (a trade arranged from chat) DOES still answer, so the player can renegotiate
		// or call the trade off.
		final Npc bot = resolveBot(fpcName);
		if ((bot != null) && isStoreVendor(bot) && !FakePlayerBehaviorManager.getInstance().isDealVendor(bot))
		{
			return;
		}

		// If this bot has a pending deal that was waiting on a quantity ("how many?"), read the amount from this
		// whisper first and lock it in, so the deal context the brain sees (and the shop opened later) reflect it.
		maybeSetDealCount(player, fpcName, bot, message);

		// Then, once the amount is settled, read any price counteroffer the player just made ("can you do 12k?")
		// and let Java decide accept/reject, recording the outcome on the deal so the bot says the decision and the
		// charge no longer depends on the brain echoing the right number back in its SHOP tag.
		maybeHandleCounterOffer(player, fpcName, bot, message);

		// LLM brain hook (private whisper). Falls back to canned chat if the bridge is offline.
		// The bot's whereabouts go along so it can truthfully answer "where are you?".
		final String aiReply = askBrain(player, fpcName, message, bot);
		if (aiReply != null)
		{
			sendChat(player, fpcName, handleMeetRequest(aiReply, message, player, bot));
			return;
		}
		
		final String text = message.toLowerCase();
		
		if (text.contains("can you see me"))
		{
			if (bot != null)
			{
				if (bot.calculateDistance2D(player) < 3000)
				{
					if (GeoEngine.getInstance().canSeeTarget(bot, player) && !player.isInvisible())
					{
						sendChat(player, fpcName, Rnd.nextBoolean() ? "i am not blind" : Rnd.nextBoolean() ? "of course i can" : "yes");
					}
					else
					{
						sendChat(player, fpcName, Rnd.nextBoolean() ? "i know you are around" : Rnd.nextBoolean() ? "not at the moment :P" : "no, where are you?");
					}
				}
				else
				{
					sendChat(player, fpcName, Rnd.nextBoolean() ? "nope, can't see you" : Rnd.nextBoolean() ? "nope" : "no");
				}
				return;
			}
		}
		
		for (FakePlayerChatHolder chatHolder : MESSAGES)
		{
			if (!chatHolder.getFpcName().equals(fpcName) && !chatHolder.getFpcName().equals("ALL"))
			{
				continue;
			}
			switch (chatHolder.getSearchMethod())
			{
				case "EQUALS":
				{
					if (text.equals(chatHolder.getSearchText().get(0)))
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
				case "STARTS_WITH":
				{
					if (text.startsWith(chatHolder.getSearchText().get(0)))
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
				case "CONTAINS":
				{
					boolean allFound = true;
					for (String word : chatHolder.getSearchText())
					{
						if (!text.contains(word))
						{
							allFound = false;
						}
					}
					if (allFound)
					{
						sendChat(player, fpcName, chatHolder.getAnswers().get(Rnd.get(chatHolder.getAnswers().size())));
					}
					break;
				}
			}
		}
	}
	
	// ===== Social: trade (global) + say (local) + ambient + bot banter =====
	
	private void startSocial()
	{
		if (!SOCIAL_ENABLED || SOCIAL_STARTED)
		{
			return;
		}
		SOCIAL_STARTED = true;
		ThreadPool.scheduleAtFixedRate(() ->
		{
			MESSAGES_THIS_MINUTE.set(0);
			TRADE_OFFERS_THIS_MINUTE.set(0);
		}, 60000, 60000); // reset rate caps each minute
		// The timers stay on the game ThreadPool, but the ambient body does blocking brain work, so it runs on the
		// dedicated brain executor (FPC-020). Cadence comes from config (seconds -> ms), clamped to a 5s floor so a
		// mistyped 0 cannot busy-spin the timer. The base interval is read once here, so an interval change applies on
		// restart (as the ini documents). Each cycle is then self-rescheduled with a fresh jitter instead of firing on a
		// fixed beat, so ambient posts do not arrive in a robotic lockstep burst and idle chat feels less mechanical.
		AMBIENT_TRADE_BASE_MS = Math.max(5, FakePlayersConfig.FAKE_PLAYER_AMBIENT_TRADE_INTERVAL_SECONDS) * 1000L;
		AMBIENT_SHOUT_BASE_MS = Math.max(5, FakePlayersConfig.FAKE_PLAYER_AMBIENT_SHOUT_INTERVAL_SECONDS) * 1000L;
		scheduleAmbientTrade();
		scheduleAmbientShout();
		LOGGER.info(getClass().getSimpleName() + ": Server-wide social chat enabled.");
	}

	/**
	 * A randomized delay around a base interval: uniform in [base*0.6, base*1.4]. Used to jitter the ambient timers so
	 * spontaneous chatter does not land on a fixed beat.
	 * @param baseMs the configured interval in milliseconds
	 * @return a jittered delay in milliseconds
	 */
	private static long jitteredDelay(long baseMs)
	{
		return Rnd.get((long) (baseMs * 0.6), (long) (baseMs * 1.4));
	}

	/** Post one spontaneous trade line after a jittered delay, then reschedule itself for the next one. */
	private void scheduleAmbientTrade()
	{
		ThreadPool.schedule(() ->
		{
			runBrainWork(this::ambientTradeChat);
			scheduleAmbientTrade();
		}, jitteredDelay(AMBIENT_TRADE_BASE_MS));
	}

	/** Post one spontaneous shout line after a jittered delay, then reschedule itself for the next one. */
	private void scheduleAmbientShout()
	{
		ThreadPool.schedule(() ->
		{
			runBrainWork(this::ambientShoutChat);
			scheduleAmbientShout();
		}, jitteredDelay(AMBIENT_SHOUT_BASE_MS));
	}
	
	// Called from ChatTrade when a real player uses trade (+) chat.
	public void overheardTradeChat(Player speaker, String text)
	{
		if (SOCIAL_ENABLED && (speaker != null) && (text != null) && !text.isEmpty())
		{
			// A WTS/WTB ad may make one relevant bot PM the player to set up a real trade. The responder
			// calls the LLM (to read slang) so it runs off-thread; plain banter handles everything else.
			if (TRADE_AD.matcher(text).find())
			{
				final Player who = speaker;
				scheduleBrainWork(() -> respondToTradeAd(who, text), Rnd.get(MIN_DELAY, MAX_DELAY));
			}
			else
			{
				reactToChat(speaker, speaker.getName(), text, false, "TRADE");
			}
		}
	}

	// Matches a trade ad and captures the direction (sell/buy) plus the item phrase after it.
	// Pattern + quantity parsing live in FakePlayerChatParsing (unit-tested there); aliased so call sites are unchanged.
	private static final Pattern TRADE_AD = FakePlayerChatParsing.TRADE_AD;

	/**
	 * Handles a WTS/WTB ad (off the network thread): the AI translates the slang to an item name, we
	 * ground it to a real item, then arm a nearby roaming bot to PM the player, walk to a meet spot, and
	 * open a real store on arrival. Falls back to plain trade banter if nothing usable matched.
	 */
	private void respondToTradeAd(Player player, String text)
	{
		if (player == null)
		{
			return;
		}
		if (TRADE_OFFERS_THIS_MINUTE.get() >= MAX_TRADE_OFFERS_PER_MINUTE)
		{
			return;
		}
		final Matcher matcher = TRADE_AD.matcher(text);
		if (!matcher.find())
		{
			return;
		}
		final String token = matcher.group(1).toLowerCase();
		final boolean playerSelling = token.startsWith("wts") || token.startsWith("selling") || token.equals("s>");

		// Stage 1: ask the AI to turn shorthand ("ssd") into a plain item name; fall back to the raw words.
		final String rawPhrase = matcher.group(2).replaceAll("[0-9]+(k|kk)?", " ").replaceAll("[^a-zA-Z ]", " ").trim();
		final String aiName = askBrainItem(text);
		// Stage 2: ground whatever we got to a real datapack item (deterministic search).
		ItemTemplate item = aiName == null ? null : FakePlayerStoreFactory.findItemByName(aiName);
		if (item == null)
		{
			item = FakePlayerStoreFactory.findItemByName(rawPhrase);
		}
		if (item == null)
		{
			// The player named a real item that bots simply do not trade (not in the allow-list): hint that it will
			// not sell, in-character, instead of silent banter. A phrase that names no real item falls through to banter.
			ItemTemplate known = (aiName == null) ? null : FakePlayerStoreFactory.findKnownItemByName(aiName);
			if (known == null)
			{
				known = FakePlayerStoreFactory.findKnownItemByName(rawPhrase);
			}
			if (known != null)
			{
				noSellHint(player, known, playerSelling);
				return;
			}
			reactToChat(player, player.getName(), text, false, "TRADE"); // nothing recognisable -> banter
			return;
		}

		// FPC-066: atomically claim the responder, so two concurrent trade ads cannot both select the same bot and set
		// up two deals (with two ACTIVE_DEALS entries) against one reservation. setupDeal below supersedes the claim.
		final Npc bot = FakePlayerBehaviorManager.getInstance().tryClaimTradeResponder(player);
		if (bot == null)
		{
			reactToChat(player, player.getName(), text, false, "TRADE"); // no roaming bot around -> banter
			return;
		}

		// Player selling -> bot buys it; player buying -> bot sells it.
		final int storeType = playerSelling ? PrivateStoreType.BUY.getId() : PrivateStoreType.SELL.getId();
		final int requestedCount = parseTradeQuantity(FakePlayerChatParsing.stripStatedPrices(matcher.group(2)), item);
		// Honor the price the player named in the ad ("wtb ssd 300 adena") when they gave one; the factory clamps it
		// into a sane band around the item's value, so the bot deals at the desired price without opening an exploit.
		// A bare quantity is never read as a price; 0 here means "no stated price", so the factory auto-prices.
		final int playerUnitPrice = FakePlayerChatParsing.parseTradeUnitPrice(matcher.group(2));
		final List<FakePlayerStoreItem> stock = playerSelling ? FakePlayerStoreFactory.dealBuyStock(item.getId(), playerUnitPrice, requestedCount) : FakePlayerStoreFactory.dealSellStock(item.getId(), playerUnitPrice, requestedCount);
		if (stock.isEmpty())
		{
			FakePlayerBehaviorManager.getInstance().releaseTradeClaim(bot, player); // FPC-066: no deal after all, free the claim now
			return;
		}
		// FPC-021: reserve a trade-offer slot atomically before arming the bot and calling the brain, so concurrent
		// responders cannot all pass a check-then-increment and overshoot the per-minute offer cap.
		if (!tryReserve(TRADE_OFFERS_THIS_MINUTE, MAX_TRADE_OFFERS_PER_MINUTE))
		{
			FakePlayerBehaviorManager.getInstance().releaseTradeClaim(bot, player); // FPC-066: offer cap hit, free the claim now
			return;
		}
		final String title = FakePlayerStoreFactory.title(playerSelling ? "BUY" : "SELL", stock);
		// Stash the deal terms and reserve the bot, but do NOT walk yet. The bot quotes a price and waits;
		// the player agrees (or haggles) and picks a meet spot over whisper. The whisper handler then walks
		// the bot once a [[MEET:spot]] is agreed, applying any haggled price from the [[SHOP:...]] tag.
		// FPC-078: only create the chat deal context and send the offer if the reservation actually took (the bot was
		// still ours). If setupDeal was refused (someone else's claim slipped in), free our claim and stay quiet.
		if (!FakePlayerBehaviorManager.getInstance().setupDeal(bot, player, storeType, stock, title))
		{
			FakePlayerBehaviorManager.getInstance().releaseTradeClaim(bot, player);
			return;
		}

		final int unit = stock.get(0).getPrice();
		final int actualCount = stock.get(0).getCount();
		final String botSide = playerSelling ? "BUY" : "SELL";
		// A stackable good with no amount named: ask how many rather than pushing a random stack, and reflect the
		// answer in the real shop later (see the whisper handler). A non-stackable is always a single piece.
		final boolean needsCount = item.isStackable() && (requestedCount <= 0);
		final String unitText = FakePlayerStorePricing.priceText(unit);
		// "each" fits only a per-unit deal: a stackable good whose amount is still open, or more than one piece. A
		// single non-stackable item (a weapon, a piece of armor) is one flat price, so the bot must not say "400k
		// each" for one Artisan's Sword.
		final boolean perUnit = needsCount || (actualCount > 1);
		final String eachWord = perUnit ? " each" : "";
		// The spoken name never carries the '*' Common Item marker (FPC-043/FPC-052), and it is what we store on the
		// deal, hand the brain, and match its reply against, so chat, X-Deal-Item and any SHOP re-resolution stay clean.
		final String itemName = spokenItemName(item);
		// The opener only quotes a price and asks if they want to deal. The player already named the item in their
		// post, so the bot must not repeat it, and the meeting place is settled later (only after a price is agreed),
		// so the opener never asks where to meet (FPC-054).
		final String deal = needsCount //
			? ((playerSelling ? "buy their " : "sell them ") + itemName + " for about " + unitText
				+ " adena each; ask HOW MANY they want and state your price, and ask if they want to deal - do NOT"
				+ " repeat the item name (they already posted it), do NOT ask where to meet yet, and do not commit to"
				+ " an amount or to walking anywhere yet") //
			: ((playerSelling ? "buy their " : "sell them ")
				+ (actualCount > 1 ? (FakePlayerStorePricing.priceText(actualCount) + "x ") : "")
				+ itemName + " for about " + unitText + " adena" + eachWord
				+ (perUnit ? "" : " (a single item - state one flat price, do not say 'each')")
				+ "; state your price and ask if they want to deal - do NOT repeat the item name (they already posted"
				+ " it), do NOT ask where to meet yet, and do not commit to walking anywhere yet");
		final String fpcName = bot.getName();
		final BrainDealContext dealContext = new BrainDealContext(botSide, itemName, item.getId(), actualCount, unit, needsCount);
		ACTIVE_DEALS.put(dealKey(player.getName(), fpcName), dealContext);
		final String fallback = needsCount //
			? ("saw ur post - i " + (playerSelling ? "buy" : "sell") + " at " + unitText + " adena each, how many u want?") //
			: ("saw ur post - i can do " + unitText + " adena" + eachWord + ", wanna deal?");
		// FPC-073: the OFFER opener writes the (player, bot) conversation history that later WHISPER turns read, so it
		// must run on the SAME per-conversation lane as those whispers. If it stayed a bare inline brain call, a fast
		// follow-up whisper (already routed through the executor) could reach the brain and be appended to the shared
		// Python history before this opener is - reordering the very history those fixes protect. Route it through the
		// conversation executor keyed (player, bot-name), with the blocking call on the shared brain executor and a
		// guaranteed completion callback that releases the next queued turn on every path.
		final String convKey = BrainConversationExecutor.key(player.getName(), fpcName);
		BrainConversationExecutor.submit(convKey, onComplete -> BrainExecutor.runBrainWork(() ->
		{
			final String line = callBridge(fpcName, "OFFER", player.getName(), "", text, nearestLocation(bot), deal, dealContext, BotIdentity.of(bot));
			// Prefer the model's natural line and fall back to the canned one only when it gave nothing. An OFFER is a
			// direct reply to the player's own post, so the line need not repeat the item name to be clear; requiring it
			// forced the canned template on every reply that phrased the deal naturally (FPC-043 follow-up). The store
			// Java opens always uses the authoritative item, and sanitize()/spokenItemName keep a corrupted name out of
			// chat, so trusting the line here costs at most an occasionally vague line, never a wrong deal.
			sendChat(player, fpcName, ((line == null) || line.isEmpty()) ? fallback : line);
		}, onComplete));
		// The offer slot was reserved atomically before the brain call (FPC-021); it is not counted again here.
	}

	/**
	 * The item name as it should ever appear in chat: the '*' Common Item marker is a stock-data artefact, not part of
	 * the real name, so it is never spoken (FPC-043/FPC-052). Normal names pass through unchanged.
	 * @param item the deal item
	 * @return the name with any '*' folded to a space and runs of whitespace collapsed
	 */
	private static String spokenItemName(ItemTemplate item)
	{
		return item.getName().replace('*', ' ').replaceAll("\\s+", " ").trim();
	}

	/** Asks the brain to translate trade-chat shorthand into a plain item name; {@code null} if unclear. */
	/**
	 * Tells a player, in-character, that a real item they posted for trade is not something the bots deal, so it will
	 * not sell here. Java owns the fact (the item is outside the trade allow-list); a nearby bot only phrases it, via
	 * the brain's NOSELL mode, with a canned fallback when the brain is offline (FPC-052 follow-up).
	 * @param player the advertiser
	 * @param item the real item they named
	 * @param playerSelling {@code true} for a WTS (nobody buys it), {@code false} for a WTB (nobody sells it)
	 */
	private void noSellHint(Player player, ItemTemplate item, boolean playerSelling)
	{
		final Npc bot = FakePlayerBehaviorManager.getInstance().pickTradeResponder(player);
		if (bot == null)
		{
			return; // no bot nearby to answer - stay quiet rather than force a line
		}
		final String fpcName = bot.getName();
		final String itemName = spokenItemName(item);
		final String side = playerSelling ? "SELL" : "BUY";
		final String line = callBridge(fpcName, "NOSELL", player.getName(), "", itemName, nearestLocation(bot), side, BotIdentity.of(bot));
		final String fallback = playerSelling //
			? ("nobody here's really buying " + itemName + " tbh") //
			: ("dont think anyone round here sells " + itemName);
		sendChat(player, fpcName, ((line == null) || line.isEmpty()) ? fallback : line);
	}

	private String askBrainItem(String adText)
	{
		final String reply = callBridge("", "ITEM", "", "", adText, "", "");
		if (reply == null)
		{
			return null;
		}
		final String name = reply.trim();
		return (name.isEmpty() || name.equalsIgnoreCase("none")) ? null : name;
	}

	/**
	 * Drops the stored structured trade context for a player/bot deal. Called when the deal lifecycle ends
	 * (store sold out / closed, meet hard-cap, grace timeout, or cancel - all funnel through the behavior
	 * manager's endMeet) so a finished deal's stale X-Deal-* terms never get injected into a later, unrelated
	 * whisper to the same bot. Safe no-op when there is no active deal for that pair.
	 * @param playerName the player the deal was with
	 * @param fpcName the bot that held the deal
	 */
	public void clearDeal(String playerName, String fpcName)
	{
		if ((playerName != null) && (fpcName != null))
		{
			ACTIVE_DEALS.remove(dealKey(playerName, fpcName));
		}
	}

	// FPC-068: a deal whose current turn Java classified as REJECT or CLARIFY is being held in negotiation, so no
	// SHOP/MEET commit may happen this turn even if the player's message also carries a stray acceptance word (e.g.
	// "ok but can you do 5k?"). The deterministic counter decision outranks generic acceptance. counterDecision is
	// turn-local (maybeHandleCounterOffer clears a stale one when a turn carries no counteroffer).
	private static boolean isNegotiationHold(BrainDealContext deal)
	{
		return (deal != null) && ("REJECT".equals(deal.counterDecision) || "CLARIFY".equals(deal.counterDecision));
	}

	private static String dealKey(String playerName, String fpcName)
	{
		return ((playerName == null ? "" : playerName.trim().toLowerCase()) + "|" + (fpcName == null ? "" : fpcName.trim().toLowerCase()));
	}

	/**
	 * When a stackable deal is still waiting on an amount (the bot asked "how many?"), read the quantity the player
	 * just whispered and lock it into both the bot's pending stock and the shared deal context. Priced numbers are
	 * stripped first so "5k, 300 adena each" is read as 5000, not 300. Once set, the deal no longer needs a count,
	 * so the real shop opened at meet time (from {@code getPendingDealCount}) reflects the agreed amount. No-op when
	 * there is no pending deal, the deal already has a count, the item is not stackable, or no amount was stated.
	 */
	private void maybeSetDealCount(Player player, String fpcName, Npc bot, String message)
	{
		if ((bot == null) || (player == null) || (message == null))
		{
			return;
		}
		final String key = dealKey(player.getName(), fpcName);
		final BrainDealContext deal = ACTIVE_DEALS.get(key);
		if ((deal == null) || !deal.needsCount)
		{
			return;
		}
		final ItemTemplate item = ItemData.getInstance().getTemplate(deal.itemId);
		if ((item == null) || !item.isStackable())
		{
			return;
		}
		final int spoken = FakePlayerChatParsing.parseSpokenQuantity(FakePlayerChatParsing.stripStatedPrices(message));
		if (spoken == 0)
		{
			return; // no amount in this line - keep waiting / let the bot ask again
		}
		// A vague "some / a stack / whatever" answer means "a normal amount", so let the factory auto-size (0).
		final int requested = (spoken == FakePlayerChatParsing.SPOKEN_QUANTITY_DEFAULT) ? 0 : spoken;
		final boolean botSells = "SELL".equalsIgnoreCase(deal.side);
		final List<FakePlayerStoreItem> stock = botSells ? FakePlayerStoreFactory.dealSellStock(deal.itemId, deal.unitPrice, requested) : FakePlayerStoreFactory.dealBuyStock(deal.itemId, deal.unitPrice, requested);
		if (stock.isEmpty())
		{
			return;
		}
		final int storeType = botSells ? PrivateStoreType.SELL.getId() : PrivateStoreType.BUY.getId();
		final String title = FakePlayerStoreFactory.title(botSells ? "SELL" : "BUY", stock);
		FakePlayerBehaviorManager.getInstance().updateDealStock(bot, storeType, stock, title);
		// Preserve any price already locked before the amount was known (FPC-040), so a price agreed first survives
		// this quantity turn. The stock was restocked at deal.unitPrice, so its price equals the locked price.
		ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, stock.get(0).getCount(), stock.get(0).getPrice(), false, deal.priceLocked));
	}

	/**
	 * When an active deal has a settled amount and the player whispers a price counteroffer ("can you do 12k?",
	 * "make it 10k"), parse it deterministically and let Java - not the model - decide the outcome. A counter within
	 * the bot's haggle tolerance (see {@link FakePlayerChatParsing#acceptsCounter}) is ACCEPTED: the new price is
	 * locked into the deal and the store restocked at it. A counter beyond tolerance is REJECTED: the bot keeps its
	 * quoted price. Either way the decision is recorded on the deal so the brain SAYS it ("ye 12k works" /
	 * "nah, 14k's my price") instead of inventing an answer. This distinguishes "player proposed X" from "bot agreed
	 * to X"; a proposal alone never sets the charge. No-op when there is no deal, the amount is still pending, or the
	 * player named no price this turn.
	 */
	private void maybeHandleCounterOffer(Player player, String fpcName, Npc bot, String message)
	{
		if ((bot == null) || (player == null) || (message == null))
		{
			return;
		}
		final String key = dealKey(player.getName(), fpcName);
		final BrainDealContext deal = ACTIVE_DEALS.get(key);
		if (deal == null)
		{
			return;
		}
		final boolean botSells = "SELL".equalsIgnoreCase(deal.side);
		final int counter = FakePlayerChatParsing.parseCounterOffer(message);
		if (counter <= 0)
		{
			// No explicit price. A bare number with a price cue ("17?", "make it 17") is ambiguous with a plain
			// quantity, so Java never commits it (FPC-042): it records a CLARIFY with the plausible scaled value so
			// the bot asks the player to confirm instead of guessing or silently keeping the old price. A truly
			// ambiguous bare number (no cue) leaves the deal unchanged.
			final int bare = FakePlayerChatParsing.parseBareCounterCandidate(message);
			if (bare > 0)
			{
				final int candidate = FakePlayerChatParsing.scaleBareCounter(bare, deal.unitPrice);
				if ((candidate > 0) && (candidate != deal.unitPrice))
				{
					ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, deal.count, deal.unitPrice, deal.needsCount, deal.priceLocked, "CLARIFY", candidate));
					return;
				}
			}
			// FPC-068: the negotiation decision is turn-local. With no counteroffer this turn, clear a stale
			// REJECT/CLARIFY from a previous turn so it cannot linger and wrongly veto a later accept or colour the
			// prompt. The persistent deal keeps its agreed price/quantity/lock; only "what happened this turn" resets.
			if (!deal.counterDecision.isEmpty())
			{
				ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, deal.count, deal.unitPrice, deal.needsCount, deal.priceLocked, "", 0));
			}
			return;
		}
		// Java's negotiation decision. A counter equal to the current price is not a change, but is still an accept.
		if (!FakePlayerChatParsing.acceptsCounter(counter, deal.unitPrice, botSells))
		{
			// REJECT: hold the bot's quoted price, but record the refused counter so the brain declines and restates
			// the price rather than silently opening a store at the player's number.
			ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, deal.count, deal.unitPrice, deal.needsCount, deal.priceLocked, "REJECT", counter));
			return;
		}
		if (counter == deal.unitPrice)
		{
			// ACCEPT at the price already on the deal: just lock it and mark accepted; no restock needed.
			ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, deal.count, deal.unitPrice, deal.needsCount, true, "ACCEPT", counter));
			return;
		}
		// Exact-ACCEPT invariant (FPC-041): an accepted price must be exactly executable. A counter outside the economy
		// band would be silently clamped to a different number, so it is REJECTED here rather than accepted-then-changed.
		if (!FakePlayerStoreFactory.dealPriceWithinBand(deal.itemId, counter, botSells))
		{
			ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, deal.count, deal.unitPrice, deal.needsCount, deal.priceLocked, "REJECT", counter));
			return;
		}
		if (deal.needsCount)
		{
			// Price agreed before the amount (FPC-040): lock the exact in-band price now and keep waiting on the
			// quantity. No stack is built yet; maybeSetDealCount sizes it later at this locked price.
			ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, deal.count, counter, true, true, "ACCEPT", counter));
			return;
		}
		// ACCEPT at a new in-band price: restock at exactly the agreed price (the clamp is a no-op here) and lock it.
		final List<FakePlayerStoreItem> stock = botSells ? FakePlayerStoreFactory.dealSellStock(deal.itemId, counter, deal.count) : FakePlayerStoreFactory.dealBuyStock(deal.itemId, counter, deal.count);
		if (stock.isEmpty())
		{
			return;
		}
		final int storeType = botSells ? PrivateStoreType.SELL.getId() : PrivateStoreType.BUY.getId();
		final String title = FakePlayerStoreFactory.title(botSells ? "SELL" : "BUY", stock);
		FakePlayerBehaviorManager.getInstance().updateDealStock(bot, storeType, stock, title);
		ACTIVE_DEALS.put(key, new BrainDealContext(deal.side, deal.item, deal.itemId, stock.get(0).getCount(), stock.get(0).getPrice(), false, true, "ACCEPT", counter));
	}

	private static int parseTradeQuantity(String phrase, ItemTemplate item)
	{
		return FakePlayerChatParsing.parseTradeQuantity(phrase, (item != null) && item.isStackable());
	}
	
	// Called from ChatGeneral when a real player uses normal (say) chat.
	public void overheardSay(Player speaker, String text)
	{
		if (SOCIAL_ENABLED && (speaker != null) && (text != null) && !text.isEmpty())
		{
			reactToChat(speaker, speaker.getName(), text, false, "SAY");
		}
	}

	// Called from ChatShout when a real player uses shout (!) chat - the global world channel for chit-chat
	// and LFM/looking-for-party ads. Bots banter back or answer the call (party/raid mechanics are not wired
	// yet, so this is conversational fluff for now).
	public void overheardShout(Player speaker, String text)
	{
		if (!SOCIAL_ENABLED || (speaker == null) || (text == null) || text.isEmpty())
		{
			return;
		}

		// LFM/LFP: a real player looking for party members. Parse the wanted roles and recruit a party that walks
		// over (works brain-off via keywords; with the brain online a free-form call is classified for its roles).
		// On a recruit we do NOT also run plain shout banter - the answer IS the bots showing up.
		final List<String> unresolved = new ArrayList<>();
		final List<String> impossible = new ArrayList<>();
		final List<Recruit> roles = parseLfp(text, unresolved, impossible);
		final int wantedLevel = parseLfpLevel(text); // optional "lvl 57"; 0 = match the recruiter's level
		if (!roles.isEmpty())
		{
			PhantomPartyManager.getInstance().recruitFromShout(speaker, roles, wantedLevel);
			if (!unresolved.isEmpty())
			{
				whisperRecruitClarification(speaker, unresolved); // recruited what we could read; ask about the garbled one(s)
			}
			if (!impossible.isEmpty())
			{
				whisperImpossibleCombos(speaker, impossible); // ... and call out any race/class combo that can't exist
			}
			return;
		}
		if (!impossible.isEmpty())
		{
			whisperImpossibleCombos(speaker, impossible); // nothing spawnable - tell them the combo doesn't exist
			if (!unresolved.isEmpty())
			{
				whisperRecruitClarification(speaker, unresolved);
			}
			return;
		}
		if (!unresolved.isEmpty())
		{
			whisperRecruitClarification(speaker, unresolved); // a party call whose class word(s) we couldn't read - ask, don't spawn a default
			return;
		}
		if (looksLikeLfp(text))
		{
			final Player who = speaker;
			runBrainWork(() ->
			{
				final List<Recruit> aiRoles = askBrainLfp(text);
				if (!aiRoles.isEmpty())
				{
					PhantomPartyManager.getInstance().recruitFromShout(who, aiRoles, wantedLevel);
				}
				else
				{
					reactToPlayerShout(who, text); // brain says it wasn't really an LFP -> normal banter
				}
			});
			return;
		}

		// Shout is a GLOBAL world channel, so a real player's shout should reach bots anywhere - not
		// just the (mostly store-vendor) crowd within SOCIAL_RANGE. Pull from a world-wide pool and
		// guarantee at least one bot answers a human.
		reactToPlayerShout(speaker, text);
	}

	// LFM/LFP triggers - a line must contain one of these to be treated as a party call (so ordinary chat that
	// happens to mention "tank" or "healer" is not mistaken for recruiting).
	/** @return {@code true} if the shout reads like a party call (has an LFM/LFP trigger). */
	private static boolean looksLikeLfp(String text)
	{
		return FakePlayerChatParsing.looksLikeLfp(text);
	}

	/** @return the level requested in an LFP shout (1-80), or 0 when none is given (match the recruiter). */
	private static int parseLfpLevel(String text)
	{
		return FakePlayerChatParsing.parseLfpLevel(text);
	}

	// Specific occupations a player can request by name ("shillien elder", "gladiator") OR by community alias
	// ("se", "wc", "ee"). An alias resolves to the EXACT class it means, so "lfm se" spawns a Shillien Elder and
	// "lfm wc" a Warcryer instead of the role's default class (an Elven Elder / a Prophet) - the tester's "aliases
	// give the wrong mob" bug. Matched longest-first so a multi-word class wins over a shorter substring.
	private static final Map<String, PlayerClass> CLASS_BY_NAME = new LinkedHashMap<>();
	private static final Pattern CLASS_PATTERN;
	// Single-word class names/aliases a one-typo fuzzy match may correct to, restricted to the DISTINCTIVE ones
	// (6+ letters) so an ordinary shout word is never mistaken for a class: e.g. "party"/"more"/"danger"/"fast"
	// stay clear of "pally"/"muse"/"dancer"/"fist". Generic role words ("tank", "dd", "healer") are matched only
	// exactly by PartyRole.fromToken - they are short, common, and easy to type, and fuzzing them collides with
	// everyday words. Multi-word class names ("shillien elder") are matched only exactly, by CLASS_PATTERN.
	private static final Set<String> CLASS_FUZZY;
	static
	{
		for (PlayerClass pc : PlayerClass.values())
		{
			// 2nd+ occupations plus the distinctive multi-word 1st classes (Palus Knight, Elven Knight, ...); generic
			// single-word 1st/base names stay race-flexible role tokens. Every match is level-adjusted at spawn time.
			if (PhantomManager.isRequestableByName(pc))
			{
				CLASS_BY_NAME.put(pc.name().toLowerCase().replace('_', ' '), pc);
			}
		}
		// Community aliases -> the exact class they name (the value must be a full-name key added just above).
		putClassAlias("pp", "prophet");
		putClassAlias("wc", "warcryer");
		putClassAlias("dc", "doomcryer");
		putClassAlias("ol", "overlord");
		putClassAlias("dom", "dominator");
		putClassAlias("ee", "elder"); // Elven Elder
		putClassAlias("se", "shillien elder");
		putClassAlias("shil", "shillien elder");
		putClassAlias("shillien", "shillien elder"); // in a support call "shillien" reads as Shillien Elder
		putClassAlias("bp", "bishop");
		putClassAlias("bish", "bishop");
		putClassAlias("card", "cardinal");
		putClassAlias("hiero", "hierophant");
		putClassAlias("eva", "eva saint");
		putClassAlias("evas", "eva saint");
		putClassAlias("muse", "sword muse");
		putClassAlias("sws", "swordsinger");
		putClassAlias("bd", "bladedancer");
		putClassAlias("spectral", "spectral dancer");
		// "spoiler" is what players ask for when they want a dwarf: it names the Bounty Hunter (Scavenger) line, and
		// the level walk resolves it to Scavenger / Bounty Hunter / Fortune Seeker for the party's level. This is the
		// only way a dwarf now joins as a damage dealer - the generic "dd" pool no longer rolls them (see DPS_RACES).
		putClassAlias("spoiler", "bounty hunter");
		putClassAlias("spoil", "bounty hunter");
		putClassAlias("bh", "bounty hunter");
		// Build the exact-match pattern from every key (names + aliases), longest first so a multi-word class wins
		// over a shorter substring. Trailing "s?" so a plural request ("2 bishops", "3 hawkeyes") still matches.
		final List<String> keys = new ArrayList<>(CLASS_BY_NAME.keySet());
		keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
		final StringBuilder sb = new StringBuilder();
		for (String key : keys)
		{
			if (sb.length() > 0)
			{
				sb.append('|');
			}
			sb.append(Pattern.quote(key));
		}
		CLASS_PATTERN = Pattern.compile("\\b(" + sb + ")s?\\b", Pattern.CASE_INSENSITIVE);
		// Fuzzy targets: distinctive single-word class keys/aliases only (6+ letters).
		final Set<String> fuzzy = new HashSet<>();
		for (String key : CLASS_BY_NAME.keySet())
		{
			if ((key.indexOf(' ') < 0) && (key.length() >= 6))
			{
				fuzzy.add(key);
			}
		}
		CLASS_FUZZY = Set.copyOf(fuzzy);
	}

	/** Registers a community alias -&gt; the class named by an existing full-name key (no-op if the name is unknown). */
	private static void putClassAlias(String alias, String canonicalName)
	{
		final PlayerClass pc = CLASS_BY_NAME.get(canonicalName);
		if (pc != null)
		{
			CLASS_BY_NAME.put(alias, pc);
		}
	}

	/**
	 * Deterministically extracts the recruits a player is shouting for. Specific class names ("lfm 1 shillien
	 * elder") become that exact class; generic role words ("2 dd + healer") become level-appropriate roles.
	 * Numbers before a class/role repeat it. Empty when the line is not an explicit party call.
	 */
	private static List<Recruit> parseLfp(String text, List<String> unresolvedOut, List<String> impossibleOut)
	{
		final List<Recruit> recruits = new ArrayList<>();
		if (!looksLikeLfp(text))
		{
			return recruits;
		}
		// Phase 1: specific class names. Replace each match with spaces so phase 2 doesn't re-read its words
		// (e.g. "shillien elder" must not also count as a generic "elder").
		final StringBuilder working = new StringBuilder(text.toLowerCase());
		final Matcher matcher = CLASS_PATTERN.matcher(working.toString());
		while (matcher.find())
		{
			final PlayerClass pc = CLASS_BY_NAME.get(matcher.group(1).toLowerCase());
			if (pc != null)
			{
				final int count = countBefore(working, matcher.start());
				final PartyRole role = PhantomManager.roleForClass(pc);
				for (int i = 0; (i < count) && (recruits.size() < 8); i++)
				{
					recruits.add(new Recruit(role, pc.getId()));
				}
			}
			for (int i = matcher.start(); i < matcher.end(); i++)
			{
				working.setCharAt(i, ' ');
			}
		}

		// Phase 2: generic role words + one-typo tolerance on whatever text is left. The pure counting state machine
		// (level-token skipping, per-word counts) lives in FakePlayerChatParsing; here we resolve each token to a
		// class/role, forgive a single typo, and collect the ones that look like a garbled class so the caller can
		// ask instead of silently spawning a wrong default.
		for (FakePlayerChatParsing.RoleRequest request : FakePlayerChatParsing.parseRoleRequests(working.toString()))
		{
			final Recruit resolved = resolveRecruitToken(request.token);
			if (resolved != null)
			{
				// A race adjective ("elf archer") re-homes a GENERIC role recruit to that race; a specifically named
				// class (classId > 0) already carries its own race, so the adjective is ignored for it.
				final Race race = (resolved.classId == 0) ? raceFromToken(request.race) : null;
				// A generic "dd"/"dps" is re-rolled FRESH per slot, so a bulk request ("5 dd") fills with a varied mix
				// of races and archetypes instead of N identical clones (Trello #11). A specifically named class or an
				// exact role word keeps its identity across the count ("5 gladiators" stays five gladiators).
				if (isGenericDd(request.token))
				{
					// An explicit race with no damage archetype at all (can't happen for a real race) is called out.
					if ((race != null) && (PhantomManager.randomDpsForRace(race) == null))
					{
						if (impossibleOut != null)
						{
							impossibleOut.add(comboLabel(request.race, resolved.role));
						}
						continue;
					}
					for (int i = 0; (i < request.count) && (recruits.size() < 8); i++)
					{
						final Recruit dd = PhantomManager.rollDpsRecruit(race);
						if (dd != null)
						{
							recruits.add(dd);
						}
					}
					continue;
				}
				final PartyRole role = resolved.role;
				if ((race != null) && !PhantomManager.comboExists(race, role))
				{
					// A specific impossible archetype (e.g. "orc archer") is called out, not spawned in the wrong race.
					if (impossibleOut != null)
					{
						impossibleOut.add(comboLabel(request.race, role));
					}
					continue;
				}
				final Recruit toAdd = (race != null) ? new Recruit(role, 0, race) : resolved;
				for (int i = 0; (i < request.count) && (recruits.size() < 8); i++)
				{
					recruits.add(toAdd);
				}
			}
			else if ((unresolvedOut != null) && looksLikeGarbledClass(request.token))
			{
				unresolvedOut.add(request.token); // close to a class/role but too garbled to be sure - ask, don't guess
			}
			// else: an ordinary non-role word ("cruma", "pls", "for") - ignore silently as before
		}
		return recruits;
	}

	/** Maps a parsed race adjective ("elf", "de", "dark_elf", ...) to the game Race, or {@code null} if unrecognised. */
	private static Race raceFromToken(String token)
	{
		if (token == null)
		{
			return null;
		}
		switch (token)
		{
			case "human":
			{
				return Race.HUMAN;
			}
			case "elf":
			case "elven":
			{
				return Race.ELF;
			}
			case "de":
			case "delf":
			case "darkelf":
			case "dark_elf":
			{
				return Race.DARK_ELF;
			}
			case "orc":
			case "orcish":
			{
				return Race.ORC;
			}
			case "dwarf":
			case "dwarven":
			{
				return Race.DWARF;
			}
			default:
			{
				return null;
			}
		}
	}

	/** A readable "&lt;race&gt; &lt;role&gt;" label for an impossible combo, used in the clarification shout. */
	private static String comboLabel(String raceToken, PartyRole role)
	{
		final String race = "dark_elf".equals(raceToken) ? "dark elf" : (raceToken == null ? "" : raceToken);
		return (race + " " + role.name().toLowerCase()).trim();
	}

	/** @return {@code true} if a token is a generic "any damage dealer" word (whose archetype can be re-rolled for a race). */
	private static boolean isGenericDd(String token)
	{
		return "dd".equals(token) || "dps".equals(token) || "damage".equals(token) || "dealer".equals(token);
	}

	/**
	 * Resolves one phase-2 recruit token to a specific recruit: an exact class name/alias (spawns that class), an
	 * exact generic role word (spawns the role's default class), or a single-typo correction of either. Plurals
	 * fall back to their singular. Returns {@code null} when the word names no class/role - the caller decides
	 * whether it is a garbled class worth a clarification, or just noise.
	 */
	private static Recruit resolveRecruitToken(String token)
	{
		// Exact class name/alias (phase 1 blanks these, but a plural or an alias by punctuation can survive).
		PlayerClass pc = classByNameOrSingular(token);
		if (pc != null)
		{
			return new Recruit(PhantomManager.roleForClass(pc), pc.getId());
		}
		// Exact generic role word.
		PartyRole role = roleByTokenOrSingular(token);
		if (role != null)
		{
			return new Recruit(role, 0); // 0 = the role's default level-appropriate class
		}
		// One-typo correction against the distinctive class names ("warcyer" -> "warcryer", "bishpo" -> "bishop").
		final String near = FakePlayerChatParsing.nearestWithin(token, CLASS_FUZZY, FakePlayerChatParsing.fuzzyBudget(token.length()));
		if (near != null)
		{
			pc = CLASS_BY_NAME.get(near);
			if (pc != null)
			{
				return new Recruit(PhantomManager.roleForClass(pc), pc.getId());
			}
		}
		return null;
	}

	private static PlayerClass classByNameOrSingular(String token)
	{
		PlayerClass pc = CLASS_BY_NAME.get(token);
		if ((pc == null) && (token.length() > 1) && token.endsWith("s"))
		{
			pc = CLASS_BY_NAME.get(token.substring(0, token.length() - 1));
		}
		return pc;
	}

	private static PartyRole roleByTokenOrSingular(String token)
	{
		PartyRole role = PartyRole.fromToken(token);
		if ((role == null) && (token.length() > 1) && token.endsWith("s"))
		{
			role = PartyRole.fromToken(token.substring(0, token.length() - 1));
		}
		return role;
	}

	/**
	 * @return {@code true} if an unresolved token is close enough to a distinctive class name to be a garbled
	 *         request worth a clarification whisper (rather than an ordinary word in the shout) - within one edit
	 *         over the auto-correct budget of a 6+ letter class name. Ordinary shout words ("party", "more",
	 *         "danger") stay clear of the distinctive class set, so they never trigger a spurious "which class?".
	 */
	private static boolean looksLikeGarbledClass(String token)
	{
		if ((token == null) || (token.length() < 4))
		{
			return false; // too short to tell a typo'd class from an ordinary word
		}
		return FakePlayerChatParsing.minDistance(token, CLASS_FUZZY) <= (FakePlayerChatParsing.fuzzyBudget(token.length()) + 1);
	}

	/** @return the count number immediately before position {@code pos} in the text (e.g. "2 dd" -> 2), else 1. */
	private static int countBefore(CharSequence text, int pos)
	{
		return FakePlayerChatParsing.countBefore(text, pos);
	}

	/** Asks the brain (LFP mode) to classify a free-form party call into role tokens; empty when it isn't one. */
	private List<Recruit> askBrainLfp(String text)
	{
		final List<Recruit> recruits = new ArrayList<>();
		final String reply = callBridge("", "LFP", "", "", text, "", "");
		if (reply == null)
		{
			return recruits;
		}
		// The brain answers with bare role words, so any race the player asked for ("lf dwarf dd") survives only in
		// the ORIGINAL text - carry it across or the request silently loses its race and spawns the wrong one.
		final Race race = soleRaceIn(text);
		for (String token : reply.toLowerCase().split("[^a-z]+"))
		{
			if (recruits.size() >= 8)
			{
				break;
			}
			// Same rule as the deterministic path (Trello #11): a generic "dd" is re-rolled FRESH per slot, so a
			// bulk call fills with a varied mix. Resolving it once through fromToken would pick ONE random
			// archetype for every slot - and would ignore the race, which is how "dwarf dd" produced an archer.
			if (isGenericDd(token))
			{
				final Recruit dd = PhantomManager.rollDpsRecruit(((race != null) && (PhantomManager.randomDpsForRace(race) != null)) ? race : null);
				if (dd != null)
				{
					recruits.add(dd);
				}
				continue;
			}
			final PartyRole role = PartyRole.fromToken(token);
			if (role != null)
			{
				// Honour the race only where that archetype actually exists for it (no orc archers).
				recruits.add(((race != null) && PhantomManager.comboExists(race, role)) ? new Recruit(role, 0, race) : new Recruit(role, 0));
			}
		}
		return recruits;
	}

	/** The one race adjective in a free-form party call ("lf dwarf dd"), or {@code null} if there is none or several. */
	private static Race soleRaceIn(String text)
	{
		Race found = null;
		for (String word : text.toLowerCase().split("[^a-z]+"))
		{
			final Race race = raceFromToken(word);
			if (race != null)
			{
				if ((found != null) && (found != race))
				{
					return null; // a mixed-race call - don't guess one for everybody
				}
				found = race;
			}
		}
		return found;
	}

	/**
	 * A nearby fake player whispers the recruiter to clarify the class word(s) the recruit parser couldn't read,
	 * rather than silently spawning the wrong "default" class. Handles the mixed case too: when a shout asked for
	 * several classes and only one or two were garbled, the readable ones are already being recruited and this only
	 * asks about the rest. Rate-limited like other public bot chatter; if no fake player is around to voice it, a
	 * plain "Party" system whisper still gives the player an answer.
	 */
	private void whisperRecruitClarification(Player speaker, List<String> tokens)
	{
		if ((speaker == null) || (tokens == null) || tokens.isEmpty())
		{
			return;
		}
		// FPC-021: reserve the message slot atomically instead of check-then-increment; this line always sends, so
		// the reserved slot is never released.
		if (!tryReserve(MESSAGES_THIS_MINUTE, FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE))
		{
			return;
		}
		final String line = recruitClarifyLine(tokens);
		final Npc voice = pickShoutResponder(speaker);
		if (voice != null)
		{
			sendChat(speaker, voice.getName(), line); // reuses the length-scaled "typing" whisper path
		}
		else
		{
			speaker.sendPacket(new CreatureSay(speaker, ChatType.WHISPER, "Party", line));
		}
	}

	/**
	 * Tells the recruiter, in a nearby fake player's voice, that a requested race/class combo can't exist (an Orc
	 * archer, an Elf buffer, a Dwarf mage), so nothing is silently spawned in the wrong race. Rate-limited like the
	 * other public bot chatter; falls back to a plain "Party" system whisper if no fake player is around to say it.
	 */
	private void whisperImpossibleCombos(Player speaker, List<String> combos)
	{
		if ((speaker == null) || (combos == null) || combos.isEmpty())
		{
			return;
		}
		// FPC-021: reserve the message slot atomically instead of check-then-increment; this line always sends.
		if (!tryReserve(MESSAGES_THIS_MINUTE, FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE))
		{
			return;
		}
		final List<String> shown = new ArrayList<>();
		for (String combo : combos)
		{
			if (!shown.contains(combo))
			{
				shown.add(combo);
			}
			if (shown.size() >= 3)
			{
				break;
			}
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < shown.size(); i++)
		{
			if (i > 0)
			{
				sb.append((i == (shown.size() - 1)) ? " or a " : ", a ");
			}
			sb.append(shown.get(i));
		}
		final String line = "there's no such thing as a " + sb + " m8, doesn't exist in this game";
		final Npc voice = pickShoutResponder(speaker);
		if (voice != null)
		{
			sendChat(speaker, voice.getName(), line);
		}
		else
		{
			speaker.sendPacket(new CreatureSay(speaker, ChatType.WHISPER, "Party", line));
		}
	}

	/** Builds the clarification line naming the class word(s) that couldn't be read (de-duped, at most three). */
	private static String recruitClarifyLine(List<String> tokens)
	{
		final List<String> shown = new ArrayList<>();
		for (String token : tokens)
		{
			if (!shown.contains(token))
			{
				shown.add(token);
			}
			if (shown.size() >= 3)
			{
				break;
			}
		}
		if (shown.size() == 1)
		{
			return "which class did you want? didn't catch '" + shown.get(0) + "'";
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < shown.size(); i++)
		{
			if (i > 0)
			{
				sb.append((i == (shown.size() - 1)) ? " or " : ", ");
			}
			sb.append('\'').append(shown.get(i)).append('\'');
		}
		return "which classes did you want? didn't catch " + sb;
	}

	/** Picks a fake player to voice a recruit clarification: one near the recruiter, else any online fake player. */
	private Npc pickShoutResponder(Player speaker)
	{
		final String speakerName = speaker.getName();
		final List<Npc> near = new ArrayList<>();
		World.getInstance().forEachVisibleObjectInRange(speaker, Npc.class, SOCIAL_RANGE, npc ->
		{
			if (npc.isFakePlayer() && !isStoreVendor(npc) && !npc.getName().equals(speakerName))
			{
				near.add(npc);
			}
		});
		if (!near.isEmpty())
		{
			return near.get(Rnd.get(near.size()));
		}
		final List<Npc> all = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		for (WorldObject object : World.getInstance().getVisibleObjects())
		{
			if (object.isNpc())
			{
				final Npc npc = object.asNpc();
				if (npc.isFakePlayer() && !isStoreVendor(npc) && !npc.getName().equals(speakerName) && seen.add(npc.getName()))
				{
					all.add(npc);
				}
			}
		}
		return all.isEmpty() ? null : all.get(Rnd.get(all.size()));
	}

	/**
	 * A real player shouted on the global '!' channel. Gather non-vendor fake players world-wide, then
	 * schedule a guaranteed first reply (the brain is told not to "pass" to a human) plus up to one extra
	 * bot on the usual chance. Bot-to-bot shout banter still flows through {@link #reactToChat} (local).
	 */
	private void reactToPlayerShout(Player speaker, String text)
	{
		if (MESSAGES_THIS_MINUTE.get() >= FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE)
		{
			return;
		}
		final String speakerName = speaker.getName();
		final List<Npc> bots = new ArrayList<>();
		final Set<String> seenNames = new HashSet<>();
		for (WorldObject object : World.getInstance().getVisibleObjects()) // world-wide: shout is global
		{
			if (object.isNpc())
			{
				final Npc npc = object.asNpc();
				// Dedupe by name; AFK store vendors stay silent.
				if (npc.isFakePlayer() && !isStoreVendor(npc) && !npc.getName().equals(speakerName) && seenNames.add(npc.getName()))
				{
					bots.add(npc);
				}
			}
		}
		if (bots.isEmpty())
		{
			return;
		}
		Collections.shuffle(bots);
		int repliers = 0;
		for (Npc bot : bots)
		{
			if (repliers >= MAX_REPLIERS)
			{
				break;
			}
			// The first eligible bot always answers a real player's shout; any extra rolls the normal chance.
			if ((repliers > 0) && (Rnd.get(100) >= REPLY_CHANCE_TO_PLAYER))
			{
				continue;
			}
			final int slot = repliers++;
			final Npc replier = bot;
			// Stagger extra repliers so a 2nd bot answers a few seconds after the 1st, not in lockstep.
			scheduleBrainWork(() -> botSpeaks(replier, speakerName, text, "SHOUT", true), Rnd.get(MIN_DELAY, MAX_DELAY) + ((long) slot * REPLY_STAGGER_MS));
		}
	}
	
	private void reactToChat(Creature origin, String speakerName, String text, boolean speakerIsBot, String channel)
	{
		// A message from a human (or an ambient seed) starts a fresh chain at depth 0.
		reactToChat(origin, speakerName, text, speakerIsBot, channel, 0);
	}

	private void reactToChat(Creature origin, String speakerName, String text, boolean speakerIsBot, String channel, int depth)
	{
		if (MESSAGES_THIS_MINUTE.get() >= FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE)
		{
			return; // throttle 3: global cap
		}
		// Chain leash: stop a bot-to-bot echo once it has run its allotted hops. Reactions to a human are never
		// gated here (depth is 0 for those), so players always get answered.
		if (speakerIsBot && (depth >= FakePlayersConfig.FAKE_PLAYER_BOT_CHAT_CHAIN_DEPTH))
		{
			return;
		}
		// Defense in depth: a content-free bot line must never become the seed of a new reaction chain, even if a
		// future caller reaches reactToChat directly instead of going through botSpeaks' pre-broadcast quality gate.
		if (speakerIsBot && isLowContentBanter(text))
		{
			return;
		}

		final int range = channel.equals("SAY") ? SAY_RANGE : SOCIAL_RANGE;
		final List<Npc> bots = new ArrayList<>();
		final Set<String> seenNames = new HashSet<>();
		World.getInstance().forEachVisibleObjectInRange(origin, Npc.class, range, npc ->
		{
			// Dedupe by name: several spawns can share one fake player name, but a given
			// character should answer a message only once. AFK store vendors stay silent.
			if (npc.isFakePlayer() && !isStoreVendor(npc) && (npc != origin) && !npc.getName().equals(speakerName) && seenNames.add(npc.getName()))
			{
				bots.add(npc);
			}
		});
		if (bots.isEmpty())
		{
			return;
		}
		
		final int chance = speakerIsBot ? FakePlayersConfig.FAKE_PLAYER_BOT_CHAT_REPLY_CHANCE : REPLY_CHANCE_TO_PLAYER;
		Collections.shuffle(bots);
		int repliers = 0;
		for (Npc bot : bots)
		{
			if (repliers >= MAX_REPLIERS)
			{
				break;
			}
			if (Rnd.get(100) >= chance) // throttles 1 & 2
			{
				continue;
			}
			final int slot = repliers++;
			final Npc replier = bot;
			// Stagger extra repliers so a 2nd bot answers a few seconds after the 1st, not in lockstep.
			scheduleBrainWork(() -> botSpeaks(replier, speakerName, text, channel, !speakerIsBot, depth), Rnd.get(MIN_DELAY, MAX_DELAY) + ((long) slot * REPLY_STAGGER_MS));
		}
	}

	private void botSpeaks(Npc bot, String speakerName, String overheard, String channel, boolean human)
	{
		botSpeaks(bot, speakerName, overheard, channel, human, 0);
	}

	private void botSpeaks(Npc bot, String speakerName, String overheard, String channel, boolean human, int depth)
	{
		if (MESSAGES_THIS_MINUTE.get() >= FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE)
		{
			return;
		}
		// SAY is local: don't waste an LLM call if no player is close enough to hear it.
		if (channel.equals("SAY") && !hasPlayerInRange(bot, SAY_RANGE))
		{
			return;
		}
		// FPC-021: reserve a message slot atomically BEFORE the brain call, so concurrent brain workers cannot all
		// pass a check-then-increment and overshoot the per-minute cap. The slot is never refunded (see tryReserve):
		// an empty reply still consumes it, which bounds brain load and keeps the reservation window-safe.
		if (!tryReserve(MESSAGES_THIS_MINUTE, FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE))
		{
			return;
		}
		final String line = askBrainPublic(bot, speakerName, overheard, channel, human);
		if ((line == null) || line.isEmpty())
		{
			return; // nothing to say; the reserved slot stays consumed (no cross-window refund)
		}
		// A prompt telling the model to be meaningful is not an enforcement mechanism. Previously a bot could still
		// broadcast "yeah lol" / "same" / "lol classic" and only AFTER the player saw it would isLowContentBanter
		// prevent that weak line from spawning another bot reply. For autonomous/bot-to-bot chatter, reject the weak
		// line BEFORE broadcast. A reply to a real human is deliberately exempt because short answers such as "np",
		// "sure" or "omw" can be perfectly natural when directly answering a player.
		final boolean lowContent = isLowContentBanter(line);
		if (!human && lowContent)
		{
			return;
		}
		// Defer the broadcast by a length-scaled "typing" time so the line doesn't appear the instant the brain
		// returns, and follow-up bot banter only kicks off once the line is actually visible.
		ThreadPool.schedule(() ->
		{
			if (channel.equals("SAY"))
			{
				sendSayChat(bot, line);
			}
			else if (channel.startsWith("SHOUT"))
			{
				sendShoutChat(bot, line); // SHOUT and SHOUTAMBIENT broadcast to the global shout channel
			}
			else
			{
				sendTradeChat(bot, line); // TRADE and AMBIENT both broadcast to global trade
			}
			// The message slot was reserved atomically before the brain call (FPC-021); it is not counted again here.

			// Bot-to-bot banter (damped): let another bot pick this up only while the chain has hops left AND the
			// line actually carries substance. `lowContent` was calculated before broadcast: autonomous low-content
			// lines never reach this point, while a short human-directed reply may be visible but still must not seed a
			// bot echo chain. Ambient timers keep seeding fresh topics independently.
			if (!lowContent)
			{
				reactToChat(bot, bot.getName(), line, true, channel.equals("SAY") ? "SAY" : channel.startsWith("SHOUT") ? "SHOUT" : "TRADE", depth + 1);
			}
		}, typingDelayMillis(line));
	}

	/**
	 * A line that is only laughter, an emote, or a one-word acknowledgement ("xD", "lol classic", "sup dude") - too
	 * thin to be autonomous living-world chatter. Autonomous/bot-to-bot lines are now dropped before broadcast;
	 * human-directed replies are allowed through because a short answer can be natural, but they still cannot seed
	 * another bot reply.
	 * @param line the bot's outgoing chat line
	 * @return {@code true} when the line has fewer than two substantive words
	 */
	private static boolean isLowContentBanter(String line)
	{
		if ((line == null) || line.isBlank())
		{
			return true;
		}
		// Reduce to letters only (drops emojis, digits, punctuation), then count words that are not filler.
		final String[] words = line.toLowerCase().replaceAll("[^a-z]+", " ").trim().split("\\s+");
		int substantive = 0;
		for (String word : words)
		{
			if ((word.length() >= 2) && !LOW_CONTENT_TOKENS.contains(word))
			{
				substantive++;
			}
		}
		return substantive < 2;
	}
	
	private boolean hasPlayerInRange(Npc npc, int range)
	{
		final boolean[] found =
		{
			false
		};
		World.getInstance().forEachVisibleObjectInRange(npc, Player.class, range, player -> found[0] = true);
		return found[0];
	}

	private void sendTradeChat(Npc npc, String text)
	{
		final CreatureSay cs = new CreatureSay(npc, ChatType.TRADE, npc.getName(), text);
		for (Player player : World.getInstance().getPlayers()) // GLOBAL
		{
			player.sendPacket(cs);
		}
	}

	private void sendSayChat(Npc npc, String text)
	{
		final CreatureSay cs = new CreatureSay(npc, ChatType.GENERAL, npc.getName(), text);
		World.getInstance().forEachVisibleObjectInRange(npc, Player.class, SAY_RANGE, player -> player.sendPacket(cs)); // LOCAL
	}

	private void sendShoutChat(Npc npc, String text)
	{
		final CreatureSay cs = new CreatureSay(npc, ChatType.SHOUT, npc.getName(), text);
		for (Player player : World.getInstance().getPlayers()) // GLOBAL world channel
		{
			player.sendPacket(cs);
		}
	}
	
	/**
	 * All non-vendor fake players in the world - the pool allowed to post ambient trade/shout. Trade and shout
	 * are broadcast to every player globally, so the speaker no longer has to be standing next to someone; drawing
	 * world-wide keeps global chat alive even when the only player is off in a hunting zone with no bots nearby.
	 */
	private List<Npc> collectAmbientSpeakers()
	{
		final List<Npc> bots = new ArrayList<>();
		for (WorldObject object : World.getInstance().getVisibleObjects())
		{
			if (object.isNpc())
			{
				final Npc npc = object.asNpc();
				if (npc.isFakePlayer() && !isStoreVendor(npc))
				{
					bots.add(npc);
				}
			}
		}
		return bots;
	}

	private void ambientTradeChat()
	{
		if (!SOCIAL_ENABLED || (MESSAGES_THIS_MINUTE.get() >= FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE))
		{
			return;
		}
		if (World.getInstance().getPlayers().isEmpty())
		{
			return; // nobody online to hear it
		}
		final List<Npc> bots = collectAmbientSpeakers();
		if (!bots.isEmpty())
		{
			botSpeaks(bots.get(Rnd.get(bots.size())), "", "", "AMBIENT", false);
		}
	}

	/** Spontaneous shout: a random bot posts global chit-chat or an LFM/looking-for-party ad now and then. */
	private void ambientShoutChat()
	{
		if (!SOCIAL_ENABLED || (MESSAGES_THIS_MINUTE.get() >= FakePlayersConfig.FAKE_PLAYER_MAX_PUBLIC_CHATS_PER_MINUTE))
		{
			return;
		}
		if (World.getInstance().getPlayers().isEmpty())
		{
			return; // nobody online to hear it
		}
		final List<Npc> bots = collectAmbientSpeakers();
		if (!bots.isEmpty())
		{
			botSpeaks(bots.get(Rnd.get(bots.size())), "", "", "SHOUTAMBIENT", false);
		}
	}
	
	private String askBrain(Player player, String fpcName, String message, Npc bot)
	{
		// Tell the brain whether this bot is walking to or waiting at a meet with this player, so "where are u" gets a
		// truthful answer instead of a guess from the town name.
		final FakePlayerBehaviorManager behavior = FakePlayerBehaviorManager.getInstance();
		final String meetState = behavior.getMeetState(bot, player);
		final String meetSpot = meetState.isEmpty() ? "" : behavior.getMeetSpot(bot);
		return callBridge(fpcName, "WHISPER", player.getName(), "", message, nearestLocation(bot), "", false, ACTIVE_DEALS.get(dealKey(player.getName(), fpcName)), BotIdentity.of(bot), meetState, meetSpot);
	}

	private String askBrainPublic(Npc bot, String speakerName, String overheard, String mode, boolean human)
	{
		return callBridge(bot.getName(), mode, "", speakerName, overheard, nearestLocation(bot), "", human, BotIdentity.of(bot));
	}

	private String callBridge(String fpcName, String mode, String playerName, String speakerName, String body, String location, String deal)
	{
		// Utility calls (ITEM / LFP) carry no bot identity.
		return callBridge(fpcName, mode, playerName, speakerName, body, location, deal, (BotIdentity) null);
	}

	private String callBridge(String fpcName, String mode, String playerName, String speakerName, String body, String location, String deal, BotIdentity identity)
	{
		return callBridge(fpcName, mode, playerName, speakerName, body, location, deal, false, null, identity);
	}

	private String callBridge(String fpcName, String mode, String playerName, String speakerName, String body, String location, String deal, BrainDealContext dealContext, BotIdentity identity)
	{
		return callBridge(fpcName, mode, playerName, speakerName, body, location, deal, false, dealContext, identity);
	}

	private String callBridge(String fpcName, String mode, String playerName, String speakerName, String body, String location, String deal, boolean human, BotIdentity identity)
	{
		return callBridge(fpcName, mode, playerName, speakerName, body, location, deal, human, null, identity);
	}

	private String callBridge(String fpcName, String mode, String playerName, String speakerName, String body, String location, String deal, boolean human, BrainDealContext dealContext, BotIdentity identity)
	{
		return callBridge(fpcName, mode, playerName, speakerName, body, location, deal, human, dealContext, identity, "", "");
	}

	private String callBridge(String fpcName, String mode, String playerName, String speakerName, String body, String location, String deal, boolean human, BrainDealContext dealContext, BotIdentity identity, String meetState, String meetSpot)
	{
		try
		{
			final HttpRequest.Builder builder = HttpRequest.newBuilder() //
				.uri(URI.create(BRAIN_URL)) //
				.timeout(Duration.ofSeconds(BRAIN_TIMEOUT_SECONDS)) //
				.header("X-FPC", fpcName) //
				.header("X-Mode", mode) //
				.header("X-Player", playerName) //
				.header("X-Speaker", speakerName) //
				.header("X-Location", location == null ? "" : location) //
				.header("X-Deal", deal == null ? "" : deal) //
				.header("X-Human", human ? "true" : "false") //
				.header("X-Meet-State", meetState == null ? "" : meetState) //
				.header("X-Meet-Spot", meetSpot == null ? "" : meetSpot);

			if ((identity != null) && !identity.isEmpty())
			{
				builder.header("X-Bot-Level", identity.level);
				builder.header("X-Bot-Class", identity.clazz);
				builder.header("X-Bot-Race", identity.race);
				builder.header("X-Bot-Gear", identity.gear);
			}

			if (dealContext != null)
			{
				builder.header("X-Deal-Side", dealContext.side);
				builder.header("X-Deal-Item", dealContext.item);
				builder.header("X-Deal-Count", Integer.toString(dealContext.count));
				builder.header("X-Deal-Unit-Price", Integer.toString(dealContext.unitPrice));
				builder.header("X-Deal-Total-Price", Long.toString(dealContext.totalPrice));
				builder.header("X-Deal-Needs-Count", dealContext.needsCount ? "true" : "false");
				// Java's accept/reject decision on the player's last counteroffer, so the bot voices it (see the
				// brain's deal_note). Empty when no counter was made this turn.
				builder.header("X-Deal-Decision", dealContext.counterDecision == null ? "" : dealContext.counterDecision);
				builder.header("X-Deal-Last-Counter", Integer.toString(dealContext.lastCounter));
			}

			final HttpRequest request = builder //
				.header("Content-Type", "text/plain; charset=utf-8") //
				.POST(HttpRequest.BodyPublishers.ofString(body)) //
				.build();

			final HttpResponse<String> response = BRAIN_HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200)
			{
				final String reply = response.body().trim();
				if (!reply.isEmpty())
				{
					return reply;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Brain bridge unreachable: " + e.getMessage());
		}
		return null;
	}
	
	/**
	 * @return a human-like "typing" delay in ms for {@code line}: a short base pause plus time proportional to
	 *         length, capped, with +-25% jitter so two bots typing the same-length line don't fire in lockstep.
	 */
	private static long typingDelayMillis(String line)
	{
		final int len = (line == null) ? 0 : line.length();
		long ms = TYPE_BASE_MS + (len * TYPE_PER_CHAR_MS);
		if (ms > TYPE_MAX_MS)
		{
			ms = TYPE_MAX_MS;
		}
		final int jitter = (int) (ms * 0.25);
		return Math.max(0, ms + Rnd.get(-jitter, jitter));
	}

	public void sendChat(Player player, String fpcName, String message)
	{
		if ((player == null) || (message == null) || message.isEmpty())
		{
			return;
		}
		// Defer the whisper by a length-scaled "typing" time so a reply doesn't pop out instantly (the bot tell):
		// a quick "np" lands fast, a longer sentence takes a beat. Bot is resolved at send time in case it despawns.
		ThreadPool.schedule(() ->
		{
			final Npc npc = resolveBot(fpcName);
			if (npc != null)
			{
				player.sendPacket(new CreatureSay(npc, ChatType.WHISPER, fpcName, message));
			}
		}, typingDelayMillis(message));
	}

	/**
	 * Answers a friend private-message sent to a persistent "regular" phantom (Phase 3 friend tier). A regular
	 * is a clientless Player, not an Npc, so it sits outside the normal whisper path ({@link #resolveBot} only
	 * finds Npc fake players): we call the brain in FRIEND mode with the regular's name - the same stable
	 * persona a whisper would get (the brain's {@code _voice()} hashes the name), but with explicit friendship
	 * awareness (warmer tone, remembered in the brain's memory) - and send the reply back
	 * over the friend channel as an {@link L2FriendSay}, after a short human "read + react" pause plus the
	 * length-scaled typing time (so a friend replies in roughly 1-4s, not the slow ambient-whisper delay).
	 * Falls back to a short canned line if the brain is offline, so a friend PM is never met with silence.
	 * @param player the real player who sent the friend PM
	 * @param regular the regular phantom being messaged
	 * @param message the player's message text
	 */
	public void handleFriendMessage(Player player, Player regular, String message)
	{
		if ((player == null) || (regular == null) || (message == null) || message.isEmpty())
		{
			return;
		}
		final String regularName = regular.getName();
		final String playerName = player.getName();
		// FPC-064: serialize FRIEND turns per (player, regular) like the other private modes, so two quick friend PMs
		// are not reordered against the shared conversation history. FPC-073: keyed (player, bot-name) with no mode, so
		// this shares one lane with WHISPER/OFFER to the same regular (the brain keeps a single history for the pair).
		final String key = BrainConversationExecutor.key(playerName, regularName);
		BrainConversationExecutor.submit(key, onComplete -> scheduleBrainWork(() ->
		{
			// FRIEND mode: same stable persona as a whisper, but the brain knows you two are friends (warmer
			// tone) and writes the friendship into its memory so other channels pick it up too.
			final String aiReply = callBridge(regularName, "FRIEND", playerName, "", message, nearestLocation(regular), "", BotIdentity.of(regular));
			final String reply = ((aiReply != null) && !aiReply.isEmpty()) //
				? aiReply //
				: (Rnd.nextBoolean() ? "hey :)" : Rnd.nextBoolean() ? "sup" : "one sec, kinda busy");
			ThreadPool.schedule(() ->
			{
				if (player.isOnline())
				{
					player.sendPacket(new L2FriendSay(regularName, playerName, reply));
				}
			}, typingDelayMillis(reply));
		}, Rnd.get(FRIEND_THINK_MIN, FRIEND_THINK_MAX), onComplete));
	}

	/**
	 * Answers a whisper (PM) sent to a befriended regular phantom - the same FRIEND-mode brain call as
	 * {@link #handleFriendMessage}, but the reply returns over the whisper channel the player used (before
	 * this hook a PM to a friend hit the clientless-target guard and reported "Player is in offline mode").
	 * @param player the real player who whispered
	 * @param regular the befriended regular being messaged
	 * @param message the player's message text
	 */
	public void handleFriendWhisper(Player player, Player regular, String message)
	{
		if ((player == null) || (regular == null) || (message == null) || message.isEmpty())
		{
			return;
		}
		final String regularName = regular.getName();
		final String playerName = player.getName();
		// FPC-064: serialize FRIEND turns per (player, regular), like the other private modes. FPC-073: keyed
		// (player, bot-name) with no mode, so it shares one lane with WHISPER/OFFER to the same regular.
		final String key = BrainConversationExecutor.key(playerName, regularName);
		BrainConversationExecutor.submit(key, onComplete -> scheduleBrainWork(() ->
		{
			final String aiReply = callBridge(regularName, "FRIEND", playerName, "", message, nearestLocation(regular), "", BotIdentity.of(regular));
			final String reply = ((aiReply != null) && !aiReply.isEmpty()) //
				? aiReply //
				: (Rnd.nextBoolean() ? "hey :)" : Rnd.nextBoolean() ? "sup" : "one sec, kinda busy");
			ThreadPool.schedule(() ->
			{
				if (player.isOnline())
				{
					player.sendPacket(new CreatureSay(regular, ChatType.WHISPER, regularName, reply));
				}
			}, typingDelayMillis(reply));
		}, Rnd.get(FRIEND_THINK_MIN, FRIEND_THINK_MAX), onComplete));
	}

	/**
	 * @return the proper-cased name of a whisper-able roaming bot, or {@code null} if the name is not a
	 *         live fake player or it is an AFK store vendor (those are treated as offline shops)
	 */
	public String talkableBotName(String name)
	{
		final Npc bot = resolveBot(name);
		if (bot == null)
		{
			return null;
		}
		// AFK vendors are unreachable; a temporary deal vendor stays reachable for cancel/renegotiate.
		if (isStoreVendor(bot) && !FakePlayerBehaviorManager.getInstance().isDealVendor(bot))
		{
			return null;
		}
		return bot.getName();
	}

	/**
	 * Resolves a fake player by name: template bots through the spawn table, procedurally generated bots
	 * by scanning the live world (their names are not in {@link FakePlayerData}).
	 */
	private Npc resolveBot(String name)
	{
		if ((name == null) || name.isEmpty())
		{
			return null;
		}
		// Template fake players are the only ones registered in FakePlayerData. Guard with getProperName
		// first: getNpcIdByName unboxes a null Integer (NPE) for unknown/generated names.
		final String proper = FakePlayerData.getInstance().getProperName(name);
		if (proper != null)
		{
			final Spawn spawn = SpawnTable.getInstance().getAnySpawn(FakePlayerData.getInstance().getNpcIdByName(proper));
			if ((spawn != null) && (spawn.getLastSpawn() != null))
			{
				return spawn.getLastSpawn();
			}
		}
		// Procedurally generated bots: their names are not in FakePlayerData, so scan the live world.
		for (WorldObject object : World.getInstance().getVisibleObjects())
		{
			if (object.isNpc())
			{
				final Npc npc = object.asNpc();
				if (npc.isFakePlayer() && name.equalsIgnoreCase(npc.getName()))
				{
					return npc;
				}
			}
		}
		return null;
	}

	private static String normalizeMeetSpot(String spot)
	{
		return FakePlayerChatParsing.normalizeMeetSpot(spot);
	}

	/**
	 * If the bot's whisper reply carries a {@code [[MEET:spot]]} tag, send it walking to that spot, then
	 * strip the tag so the player only sees the natural line.
	 * @return the cleaned reply text
	 */
	private String handleMeetRequest(String reply, String playerMessage, Player player, Npc bot)
	{
		if (reply == null)
		{
			return "";
		}

		boolean cancelled = false;
		boolean handledShop = false;
		boolean moved = false; // the bot was actually sent to meet / a store was opened this line (FPC-051 review, finding 6)
		// Java owns what the bot says about where it is once a meet action runs: the model's line is written before
		// the action and often claims arrival ("im at gk") while the bot has only just started walking.
		boolean travelling = false; // a meet started and the bot is walking there now
		String failedSpot = null; // a meet was agreed but no spot could be found beside that landmark
		boolean cancelledByWords = false; // the player's own words called the deal off (no tag needed)
		boolean alreadyThere = false; // a repeat meet for the spot the bot is already waiting at
		BrainDealContext deal = null;
		// A SHOP tag, or a MEET tag naming a place: the model proposed an action. If nothing ran, Java says so instead of
		// sending the model's line (or an empty line when the reply was only the tag).
		final Matcher anyMeet = MEET_TAG.matcher(reply);
		final boolean proposedAction = SHOP_TAG.matcher(reply).find() || (anyMeet.find() && !"cancel".equalsIgnoreCase(normalizeMeetSpot(anyMeet.group(1))));

		// Roaming bots and bots running a temporary deal store both negotiate here (the latter so the player
		// can renegotiate or cancel mid-deal); only static AFK vendors are excluded.
		if ((bot != null) && (!isStoreVendor(bot) || FakePlayerBehaviorManager.getInstance().isDealVendor(bot)))
		{
			// SHOP tag: the bot commits to a real store for a specific item/price. Open it now if it is
			// already waiting with the player, otherwise arm it and make sure it walks over to meet.
			//
			// A SHOP tag only ever means "the deal we already negotiated is agreed, open it now". It is NEVER the
			// source of truth for direction, item, or price: a weak model routinely flips SELL/BUY, mangles the item
			// name, and drops the "k" on the price inside the tag. Those terms come only from the Java-owned deal
			// context that the original WTS/WTB ad (or a later accepted counteroffer) stored. So:
			// - If there is NO stored deal for this (player, bot), the tag is unanchored (a model hallucination or an
			//   expired deal); Java refuses to create any real store state from it. The bot may still speak; it just
			//   cannot open a store no server-side deal backs (FPC-051 review, section 6.1).
			// - The executable price is ALWAYS the server-side deal price: the opening quote, or the negotiated price
			//   once maybeHandleCounterOffer locked it. The model can never choose a money value (section 6.2).
			// FPC-060: the model tag is a proposal, not the authority for WHEN the deal commits. Java opens/arms the
			// store only when the player's own latest message reads like acceptance (isDealAccept). A [[SHOP]] paired
			// with a "nah, not interested" is ignored and the deal stays in negotiation, so the model no longer owns
			// the state transition (it never owned the item/side/price, which come from the deal context).
			final Matcher shop = SHOP_TAG.matcher(reply);
			deal = ACTIVE_DEALS.get(dealKey(player.getName(), bot.getName()));

			// The player's own words decide a cancel. "sorry no deal" ends the deal even when the model forgets the
			// [[MEET:cancel]] tag; before, the bot said goodbye while the server kept the offer, meet or store alive. A
			// message that also names a new price ("no deal at 15k, 12k?") is haggling, not a cancel, and is left to the
			// counteroffer handling.
			if ((deal != null) && FakePlayerChatParsing.isDealCancel(playerMessage) && (FakePlayerChatParsing.parseCounterOffer(playerMessage) <= 0))
			{
				FakePlayerBehaviorManager.getInstance().cancelDeal(bot, player);
				clearDeal(player.getName(), bot.getName()); // also when the bot held no behavior state for it
				cancelled = true;
				cancelledByWords = true;
			}

			if (!cancelled && shop.find() && (deal != null) && FakePlayerChatParsing.isDealAccept(playerMessage) && !isNegotiationHold(deal))
			{
				final boolean botSells = "SELL".equalsIgnoreCase(deal.side);
				final ItemTemplate item = FakePlayerStoreFactory.findItemByName(deal.item);
				if (item != null)
				{
					final int price = deal.unitPrice;
					final int storeType = botSells ? PrivateStoreType.SELL.getId() : PrivateStoreType.BUY.getId();
					final FakePlayerBehaviorManager behavior = FakePlayerBehaviorManager.getInstance();
					final int requestedCount = behavior.getPendingDealCount(bot, item.getId());
					final List<FakePlayerStoreItem> stock = botSells ? FakePlayerStoreFactory.dealSellStock(item.getId(), price, requestedCount) : FakePlayerStoreFactory.dealBuyStock(item.getId(), price, requestedCount);
					if (!stock.isEmpty())
					{
						final String title = FakePlayerStoreFactory.title(botSells ? "SELL" : "BUY", stock);
						handledShop = true;
						moved = true; // a store was opened or armed and the bot is heading to the meet
						if (behavior.isWaitingAtMeet(bot))
						{
							behavior.openDealNow(bot, storeType, stock, title); // already here -> open immediately
						}
						else if (behavior.setupDeal(bot, player, storeType, stock, title))
						{
							final Matcher meet = MEET_TAG.matcher(reply);
							final String spot = (meet.find() && !"cancel".equalsIgnoreCase(normalizeMeetSpot(meet.group(1)))) ? normalizeMeetSpot(meet.group(1)) : "gatekeeper";
							if (behavior.requestMeet(bot, spot, player))
							{
								travelling = true;
							}
							else
							{
								// The deal stays pending (it lapses on the offer timer) so the player can name another spot.
								moved = false;
								failedSpot = spot;
							}
						}
						else
						{
							moved = false; // the bot is reserved by someone else: nothing was armed
						}
					}
				}
			}

			if (!handledShop && !cancelled)
			{
				// Plain MEET handling (no shop committed this line).
				final Matcher meet = MEET_TAG.matcher(reply);
				if (meet.find())
				{
					final String spot = normalizeMeetSpot(meet.group(1));
					if ("cancel".equalsIgnoreCase(spot))
					{
						// FPC-061: tear the deal down only when a deal actually exists AND the player's own message
						// expresses cancellation. A bare [[MEET:cancel]] with no player cancel intent (a hallucinated
						// or injected tag) must not stand the bot down or drop a live deal.
						if ((deal != null) && FakePlayerChatParsing.isDealCancel(playerMessage))
						{
							// FPC-067: cancel the WHOLE deal lifecycle (pending offer, active meet, or open store) and
							// clear the chat context in one authoritative call, so a pre-meet cancel does not leave the
							// bot reserved until the offer TTL. cancelDeal clears ACTIVE_DEALS itself.
							FakePlayerBehaviorManager.getInstance().cancelDeal(bot, player);
							cancelled = true;
						}
					}
					else if ((deal != null) && FakePlayerChatParsing.isDealAccept(playerMessage) && !isNegotiationHold(deal))
					{
						// Positive anchor (FPC-051 review, finding 6) plus player-intent gate (FPC-060): only move the
						// bot when a real, server-owned deal exists for this (player, bot) AND the player's latest
						// message reads like agreement to meet. Without a deal a [[MEET]] is unanchored, and without
						// acceptance the model does not get to decide the bot walks over.
						final FakePlayerBehaviorManager behavior = FakePlayerBehaviorManager.getInstance();
						if (behavior.isWaitingAtMeet(bot) && spot.equals(behavior.getMeetSpot(bot)))
						{
							// Already standing at that very spot for this meet: nothing to walk to, just keep waiting.
							behavior.noteMeetInteraction(bot, player);
							alreadyThere = true;
						}
						else if (behavior.requestMeet(bot, spot, player))
						{
							moved = true;
							travelling = true;
						}
						else
						{
							failedSpot = spot;
						}
					}
				}
				else
				{
					// No tag: if already waiting for this player, they are still engaged -> keep waiting.
					FakePlayerBehaviorManager.getInstance().noteMeetInteraction(bot, player);
				}
			}
		}

		if (failedSpot != null)
		{
			return meetFailedLine(failedSpot);
		}
		if (travelling)
		{
			return meetTransitLine();
		}
		final String cleaned = stripOwnName(MEET_TAG.matcher(SHOP_TAG.matcher(reply).replaceAll("")).replaceAll("").trim(), bot);
		if (cancelledByWords && proposedAction)
		{
			return dealCancelledLine(); // the model's line was written for the meet or store the player just called off
		}
		if (proposedAction && !moved && !alreadyThere && !cancelled)
		{
			// The model proposed a meet or store but Java ran nothing. Without a deal there is nothing to meet for, so
			// the bot must not sound like it is coming; with a deal still in negotiation, keep its line or ask.
			if (deal == null)
			{
				return noDealLine();
			}
			if (cleaned.isEmpty())
			{
				return Rnd.nextBoolean() ? "so we got a deal or not?" : "u want it or not?";
			}
		}
		if (!cleaned.isEmpty())
		{
			return cleaned;
		}
		if (cancelled)
		{
			return "k np";
		}
		// Only answer "omw" when the bot was actually sent somewhere / a store was opened. If the model returned only
		// tags that resolved to no action (an unanchored SHOP/MEET, a foreign tag), stay silent rather than claiming
		// to be on the way when nothing happened (FPC-051 review, finding 6).
		return moved ? "omw" : "";
	}

	/**
	 * Drops a leading "botname:" the model sometimes writes before its line. With a tag-only reply such as
	 * "zephdil: [[MEET:cancel]]" the player otherwise received just "zephdil:" once the tag was removed.
	 * @param line the reply with action tags already removed
	 * @param bot the answering bot, may be null
	 * @return the line without the bot's own name prefix
	 */
	private static String stripOwnName(String line, Npc bot)
	{
		if ((bot == null) || (bot.getName() == null) || bot.getName().isEmpty())
		{
			return line;
		}
		final String name = bot.getName();
		if ((line.length() > name.length()) && line.regionMatches(true, 0, name, 0, name.length()) && (line.charAt(name.length()) == ':'))
		{
			return line.substring(name.length() + 1).trim();
		}
		return line;
	}

	/** What the bot says when a meet has just started: it is on its way, never already there. */
	private static String meetTransitLine()
	{
		switch (Rnd.get(4))
		{
			case 0:
			{
				return "omw";
			}
			case 1:
			{
				return "k omw";
			}
			case 2:
			{
				return "on my way";
			}
			default:
			{
				return "heading there now";
			}
		}
	}

	/** What the bot says when no spot could be found beside the agreed landmark (none nearby, or no free ground). */
	private static String meetFailedLine(String spot)
	{
		final String place = "warehouse".equals(spot) ? "wh" : "shop".equals(spot) ? "shop" : "gk";
		return Rnd.nextBoolean() ? ("cant find the " + place + " from here, where r u?") : ("no " + place + " near me, meet somewhere else?");
	}

	/** What the bot says when the player called the deal off. */
	private static String dealCancelledLine()
	{
		switch (Rnd.get(3))
		{
			case 0:
			{
				return "k np";
			}
			case 1:
			{
				return "np, maybe next time";
			}
			default:
			{
				return "all good, cya";
			}
		}
	}

	/** What the bot says when the model agreed to a meet or store but no server-side deal exists with this player. */
	private static String noDealLine()
	{
		return Rnd.nextBoolean() ? "wait what deal? we never set anything up" : "we got nothing lined up tho, post it in trade";
	}

	/** A seated private-store vendor is an AFK shop and never chats. */
	private static boolean isStoreVendor(Npc npc)
	{
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		return (look != null) && (look.getPrivateStoreType() != 0);
	}

	/** @return a short phrase like "in Giran" or "near Aden" for the bot's current position (NPC or phantom). */
	public static String nearestLocation(Creature creature)
	{
		if (creature == null)
		{
			return "";
		}
		int best = -1;
		long bestDistanceSq = Long.MAX_VALUE;
		for (int i = 0; i < TOWN_COORDS.length; i++)
		{
			final long dx = creature.getX() - TOWN_COORDS[i][0];
			final long dy = creature.getY() - TOWN_COORDS[i][1];
			final long distanceSq = (dx * dx) + (dy * dy);
			if (distanceSq < bestDistanceSq)
			{
				bestDistanceSq = distanceSq;
				best = i;
			}
		}
		if (best < 0)
		{
			return "";
		}
		return (Math.sqrt(bestDistanceSq) < 3000 ? "in " : "near ") + TOWN_NAMES[best];
	}
	
	public static FakePlayerChatManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FakePlayerChatManager INSTANCE = new FakePlayerChatManager();
	}
}
