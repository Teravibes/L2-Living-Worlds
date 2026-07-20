/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.model.groups.PartyMessageType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;


/**
 * Personal support buddy brain.
 * <p>
 * A buddy is a support-class phantom (Elven Elder / Prophet / Warcryer) spawned idle in a town by
 * {@link PhantomManager}. A real player can whisper it, party it (the invite is auto-accepted server-side
 * by {@code RequestJoinParty}), and use it as a personal buffer/healer. Once partied this manager keeps the
 * owner buffed and topped up to roughly half health, keeps itself buffed, follows the owner, warns when its
 * MP runs low and sits to recover when safe, teleports to a named gatekeeper destination on command, and
 * survives a grace period if the owner teleports away or briefly logs off.
 * <p>
 * Everything here is rule-based and works with the LLM brain offline; the whisper command parser is
 * deterministic. (Phase 2 will route natural language through the brain on top of this.)
 */
public class PhantomBuddyManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PhantomBuddyManager.class.getName());

	// Fast behaviour tick for engaged (partied) buddies: heal/buff/follow need to feel responsive.
	private static final long TICK_INTERVAL = 1000;
	// Heal the owner when below this, the "50% and above" the user asked for. Self-heal a bit lower.
	private static final int OWNER_HEAL_PERCENT = 50;
	private static final int SELF_HEAL_PERCENT = 40;
	// Re-cast a buff when it is missing or has less than this many seconds left, so it never fully drops.
	private static final int BUFF_REFRESH_SECONDS = 20;
	// Chant of Life is a short (~10s) HP-regen buff; maintaining it like a normal buff means recasting it
	// constantly. Treat it as needed only while the target is actually hurt (below this HP%).
	private static final int CHANT_OF_LIFE_ID = 1229;
	private static final int CHANT_OF_LIFE_HP_PERCENT = 80;
	// MP watch: mention it and sit when MP drops to ~the low mark, then stay seated until MP is fully restored
	// (only stands early if attacked, the owner runs off, or it's ordered up / to heal/buff).
	private static final int MP_LOW_PERCENT = 30; // whisper "low on mp" and sit at this
	private static final int MP_REST_SIT = 30; // sit to recover when it drops to ~this (and safe)
	private static final int MP_REST_STAND = 100; // and stay seated until MP is fully restored
	private static final long MP_WARN_COOLDOWN = 30000;
	private static final long STAND_SUPPRESS = 30000; // a "stand" order keeps it on its feet this long before auto-rest resumes
	// Only act on buffs/heals when the owner is close enough that a cast makes sense (else just follow).
	private static final int SUPPORT_RANGE = 900;
	private static final int FOLLOW_RANGE = 250;
	private static final int DANGER_RANGE = 700; // a monster this close means "don't sit to rest"
	// Grace before despawning an engaged buddy whose owner went offline; "brb"/"5 min" extends it.
	private static final long OFFLINE_GRACE = 120000;
	private static final long BRB_GRACE = 360000;
	// Proactive party chatter: now and then a partied buddy opens some small talk in party chat (only with the
	// LLM brain online) so it doesn't feel like a silent bot. Long, jittered gap; downtime only. If the moment
	// is bad (owner fighting / resting) it retries after CHATTER_RETRY instead of waiting a whole window.
	private static final long CHATTER_MIN = 7 * 60000L;
	private static final long CHATTER_MAX = 18 * 60000L;
	private static final long CHATTER_RETRY = 60000;
	// LLM brain bridge (optional). When a whisper isn't a recognised command, it is sent here for a natural
	// reply that can carry an action tag ([[FOLLOW]] / [[STAY]] / [[TP:place]] / [[GRACE:n]] / [[BUFF]] /
	// [[DISBAND]]). If the brain is offline the buddy just falls back to a short canned line.
	private static final HttpClient BRAIN_HTTP = HttpClient.newHttpClient();
	private static final String BRAIN_URL = "http://127.0.0.1:5000/chat";
	// Outlast a slow local Ollama model (~30s); the old 20s cap silently dropped every reply. See the same
	// constant in FakePlayerChatManager.
	private static final int BRAIN_TIMEOUT_SECONDS = 45;
	// Closing bracket is tolerant ([\]\)]{1,2}) because local Ollama models sometimes emit a malformed close
	// (e.g. "[[TP:roa)"); a strict "\]\]" would neither act on the tag nor strip it, leaking it into player chat.
	private static final Pattern TAG_FOLLOW = Pattern.compile("\\[\\[\\s*FOLLOW\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_STAY = Pattern.compile("\\[\\[\\s*STAY\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_BUFF = Pattern.compile("\\[\\[\\s*BUFF\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_DISBAND = Pattern.compile("\\[\\[\\s*DISBAND\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_TP = Pattern.compile("\\[\\[\\s*TP\\s*:\\s*([^\\]]+?)\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern TAG_GRACE = Pattern.compile("\\[\\[\\s*GRACE\\s*:\\s*(\\d+)\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	private static final Pattern ANY_TAG = Pattern.compile("\\[\\[[^\\]]*[\\]\\)]{1,2}");
	private static final Pattern HTML_TAG = Pattern.compile("</?\\s*[a-zA-Z][a-zA-Z0-9]*\\s*/?>");

	// Heal skills a buddy may know, best (highest id here = strongest) first.
	private static final int[] HEAL_PRIORITY =
	{
		1218, // Greater Battle Heal
		1217, // Greater Heal
		1015, // Battle Heal
		1011 // Heal
	};

	/** Per-buddy state. */
	private static class Buddy
	{
		final Player npc;
		Player owner; // null while idle in town; set when partied
		boolean following = true;
		long graceUntil; // 0 unless the owner is offline / said brb; despawn when exceeded
		long lastMpWarn;
		List<Skill> buffs; // beneficial buffs to maintain (lazy)
		Skill heal; // best heal it knows (lazy)
		Location pendingDestination; // a place the buddy proposed; teleports only once the owner confirms
		String pendingDestName;
		long nextChatter; // when the buddy may next open small talk in party chat (0 = not scheduled)
		int lastX; // follow-stuck watchdog: last position + how many ticks it failed to move while trailing
		int lastY;
		int stuckTicks;
		long castSince; // when the current cast began, so a wedged cast can be aborted instead of freezing the buddy
		boolean rebuffing; // "rebuff" order: recast the whole kit regardless of time left
		int rebuffIdx; // which buff in the list is next for the current target
		List<Player> rebuffQueue; // targets still owed a full kit (owner only, a named member, or the whole party)
		boolean healNow; // "heal me" order: heal the owner once even at full HP
		Skill pendingBuff; // "give me X" / "X on <name>" order: a specific buff to cast next tick
		Player pendingBuffTarget; // who that on-demand buff goes on (the owner, or a named party member)
		long noSitUntil; // "stand" order: don't auto-sit for MP until this time (so it doesn't pop straight back down)

		Buddy(Player npc)
		{
			this.npc = npc;
		}
	}

	private final ConcurrentHashMap<Integer, Buddy> _buddies = new ConcurrentHashMap<>();
	private final Map<String, Location> _destinations = new HashMap<>(); // gatekeeper destination name -> spot
	private boolean _ticking = false;

	protected PhantomBuddyManager()
	{
		load();
	}

	@Override
	public void load()
	{
		// Reuse the real gatekeeper destination data so a buddy teleports to the exact spot a gatekeeper would
		// drop the player (Ruins of Agony, Cruma Tower, ...). All town teleporter lists are scanned into one
		// name -> location index.
		_destinations.clear();
		parseDatapackDirectory("data/teleporters/town", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _destinations.size() + " teleport destinations for buddies.");
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "npc", npcNode -> forEach(npcNode, "teleport", teleportNode -> forEach(teleportNode, "location", locationNode ->
		{
			final StatSet set = new StatSet(parseAttributes(locationNode));
			final String name = set.getString("name", "");
			if (!name.isEmpty() && set.contains("x"))
			{
				_destinations.putIfAbsent(normalize(name), new Location(set.getInt("x"), set.getInt("y"), set.getInt("z")));
			}
		}))));
	}

	// ===== Lifecycle (called by PhantomManager) =====

	/** Registers a freshly spawned buddy and gives it its starting self-buffs. */
	public void onBuddySpawned(Player buddy, String roleName)
	{
		final Buddy state = new Buddy(buddy);
		_buddies.put(buddy.getObjectId(), state);
		startTicking();
	}

	/**
	 * Per-buddy housekeeping on the slow {@link PhantomManager} supervisor tick (~5s). An idle (unpartied)
	 * buddy just stands in town doing nothing - it does NOT self-buff until a real player parties it, at which
	 * point the fast tick ({@link #serveOwner}) takes over buffing/healing/following.
	 */
	public void supervise(Player buddy)
	{
		final Buddy state = _buddies.get(buddy.getObjectId());
		if (state == null)
		{
			return; // not tracked (e.g. just despawned)
		}
		if (buddy.isDead())
		{
			// A dead buddy is of no use; release the party bond and let PhantomManager recycle the corpse.
			release(state, false);
		}
	}

	// ===== Whisper command handling (called from ChatWhisper via FakePlayerChatManager) =====

	/** @return {@code true} if the named player is a live buddy this manager owns (so whispers route here). */
	public boolean isBuddy(Player player)
	{
		return (player != null) && _buddies.containsKey(player.getObjectId());
	}

	/**
	 * Handles a whisper a player sent to a buddy and returns the buddy's reply (delivered as a whisper back).
	 * Deterministic keyword parsing - works with the LLM brain offline.
	 */
	public String handleWhisper(Player owner, Player buddy, String message)
	{
		final Buddy state = _buddies.get(buddy.getObjectId());
		if ((state == null) || (owner == null) || (message == null))
		{
			return null;
		}
		return parseCommand(state, owner, buddy, message, false);
	}

	/**
	 * Handles a party-channel line: every buddy in the speaker's party that is bound to that speaker reacts to
	 * it with the same commands as a whisper and answers in party chat. Lets you drive your buffers from party
	 * chat, not only private whisper.
	 */
	public void handlePartyChat(Player speaker, String message)
	{
		if ((speaker == null) || (message == null) || !speaker.isInParty())
		{
			return;
		}
		for (Player member : speaker.getParty().getMembers())
		{
			final Buddy state = _buddies.get(member.getObjectId());
			if ((state == null) || (state.owner != speaker))
			{
				continue; // not a buddy, or a buddy serving someone else
			}
			final String reply = parseCommand(state, speaker, member, message, true);
			if ((reply != null) && !reply.isEmpty())
			{
				partyChat(member, reply);
			}
		}
	}

	/**
	 * Deterministic command parser shared by the whisper and party-chat paths. Returns the buddy's reply for
	 * the caller to deliver on the right channel, or {@code null} when the line was handed to the LLM brain
	 * (which delivers its own reply asynchronously on the same channel).
	 */
	private String parseCommand(Buddy state, Player owner, Player buddy, String message, boolean party)
	{
		final String text = message.toLowerCase().trim();

		// Pending teleport confirmation: if the buddy already proposed a place, a yes/no settles it before any
		// other keyword is parsed (so a bare "go"/"yes" finishes the trip rather than matching something else).
		if (state.pendingDestination != null)
		{
			if (isAffirmative(text))
			{
				final Location destination = state.pendingDestination;
				final String name = state.pendingDestName;
				state.pendingDestination = null;
				state.pendingDestName = null;
				teleportBuddy(state, owner, destination);
				deliver(state, owner, party, "k, heading to " + name + " now");
				return null;
			}
			if (isNegative(text))
			{
				state.pendingDestination = null;
				state.pendingDestName = null;
				deliver(state, owner, party, "k, staying put");
				return null;
			}
			// Neither: fall through (they may have changed the subject or named a different place).
		}

		// A specific buff by name ("give me might", "ww pls") - checked early so "give me"/"gimme" aren't eaten by
		// the grace ("brb") matcher. Grant it if the buddy knows it, else say it doesn't have it.
		final String requested = PhantomBuffs.requestedBuff(text);
		if (requested != null)
		{
			if (!isPartiedWith(state, owner))
			{
				deliver(state, owner, party, "party me first :)");
				return null;
			}
			final Skill known = PhantomBuffs.findKnown(buffList(state, buddy), requested);
			if (known != null)
			{
				// "<buff> on me" / "<buff> on <member>": a named party member is the target, otherwise the owner.
				final Player named = findPartyMemberByName(owner, text);
				final Player target = ((named != null) && (named != buddy)) ? named : owner;
				state.pendingBuff = known;
				state.pendingBuffTarget = target;
				deliver(state, owner, party, "sure, " + known.getName().toLowerCase() + ((target == owner) ? "" : " on " + target.getName()));
			}
			else
			{
				deliver(state, owner, party, "i don't have " + requested);
			}
			return null;
		}

		// Party / buff-service request: the buddy always agrees - that's the whole point. The real bond forms
		// when the player sends the invite (RequestJoinParty auto-accepts and routes to onInvited). Catch broad
		// "give me buffs" phrasing here before the later AFK/grace matcher can mistake "give me" for "give me a min".
		if (containsAny(text, "party", "group", "join me", "wanna pt", "want to pt", "lets party", "let's party", "lets pt",
			"lf buff", "lf buffs", "need buff", "need buffs", "need some buffs", "want buff", "want buffs",
			"can you buff", "can u buff", "can you give me buff", "can you give me buffs", "can you give me some buff",
			"can you give me some buffs", "can u give me buff", "can u give me buffs", "can u give me some buff",
			"can u give me some buffs", "give me buff", "give me buffs", "give me some buff", "give me some buffs",
			"gimme buff", "gimme buffs", "gimme some buff", "gimme some buffs", "some buffs",
			"mind if you give me buff", "mind if you give me buffs", "mind if you give me some buff",
			"mind if you give me some buffs", "mind giving me buff", "mind giving me buffs",
			"come buff", "come give me buff", "come give me buffs", "come give me some buff", "come give me some buffs"))
		{
			if (isPartiedWith(state, owner))
			{
				startRebuff(state, new ArrayList<>(List.of(owner)));
				deliver(state, owner, party, "buffing you up");
			}
			else
			{
				deliver(state, owner, party, "sure, invite me first");
			}
			return null;
		}

		// Follow / stay.
		if (containsAny(text, "follow", "come with", "stick with me", "on me"))
		{
			if (!isPartiedWith(state, owner))
			{
				deliver(state, owner, party, "party me first :)");
				return null;
			}
			state.following = true;
			deliver(state, owner, party, "ok, following you");
			return null;
		}
		if (containsAny(text, "stay", "wait here", "hold here", "stop following"))
		{
			state.following = false;
			buddy.getAI().setIntention(Intention.IDLE);
			deliver(state, owner, party, "k, waiting here");
			return null;
		}

		// Stand up on demand (interrupts an MP rest).
		if (containsAny(text, "stand up", "stand", "get up", "on your feet", "feet"))
		{
			state.noSitUntil = System.currentTimeMillis() + STAND_SUPPRESS; // don't pop straight back down
			if (buddy.isSitting())
			{
				buddy.standUp();
			}
			deliver(state, owner, party, "up");
			return null;
		}

		// Grace extension (brb / give me a minute). Do NOT match plain "give me" or "gimme" because that steals
		// "give me buffs" / "mind if you give me some buffs" from the buff-service parser above.
		if (containsAny(text, "brb", "be right back", "afk", "bio", "give me a min", "give me a minute",
			"give me a sec", "give me a second", "gimme a min", "gimme a minute", "gimme a sec",
			"gimme a second", "one min", "one minute", "one sec", "one second", "wait a min",
			"wait a minute", "hold on", "hold up"))
		{
			state.graceUntil = System.currentTimeMillis() + BRB_GRACE;
			deliver(state, owner, party, "np, take your time");
			return null;
		}

		// Disband / dismiss.
		if (containsAny(text, "disband", "leave party", "leave the party", "dismiss", "bye", "you can go", "thanks bye"))
		{
			if (isPartiedWith(state, owner))
			{
				release(state, true);
				deliver(state, owner, party, "gl hf o/");
				return null;
			}
			deliver(state, owner, party, "we're not partied");
			return null;
		}

		// Re-buff on demand: actually recast the whole kit (not just acknowledge).
		if (containsAny(text, "buff", "rebuff", "rebuf"))
		{
			if (!isPartiedWith(state, owner))
			{
				deliver(state, owner, party, "party me first :)");
				return null;
			}
			// "buff all / everyone / the party": fully (re)buff every living party member, not just the owner.
			if (containsAny(text, "buff all", "buff everyone", "buff every", "buff the party", "buff party", "buff us all", "buff whole party", "full buff all", "fully buff"))
			{
				startRebuff(state, partyTargets(owner));
				deliver(state, owner, party, "buffing everyone");
				return null;
			}
			// "buff <name>": fully buff one named party member.
			final Player named = findPartyMemberByName(owner, text);
			if (named != null)
			{
				startRebuff(state, new ArrayList<>(List.of(named)));
				deliver(state, owner, party, "buffing " + named.getName());
				return null;
			}
			startRebuff(state, new ArrayList<>(List.of(owner)));
			deliver(state, owner, party, "buffing you up");
			return null;
		}

		// Heal on demand: heal the owner once even if not hurt.
		if (containsAny(text, "heal me", "heal us", "heal"))
		{
			if (isPartiedWith(state, owner))
			{
				state.healNow = true;
				deliver(state, owner, party, "healing you");
			}
			else
			{
				deliver(state, owner, party, "party me first :)");
			}
			return null;
		}

		// Teleport to a named gatekeeper destination. An explicit travel order ("tp to X", "go to X", "take me
		// to X") goes now; a bare mention is treated as a proposal and waits for the owner to confirm, so the
		// buddy never yanks them somewhere uninvited.
		final Map.Entry<String, Location> dest = matchDestinationEntry(text);
		if (dest != null)
		{
			if (!isPartiedWith(state, owner))
			{
				deliver(state, owner, party, "party me first :)");
				return null;
			}
			if (isTravelOrder(text))
			{
				state.pendingDestination = null;
				state.pendingDestName = null;
				teleportBuddy(state, owner, dest.getValue());
				deliver(state, owner, party, "ok, heading to " + dest.getKey() + " now");
			}
			else
			{
				state.pendingDestination = dest.getValue();
				state.pendingDestName = dest.getKey();
				deliver(state, owner, party, "wanna head to " + dest.getKey() + "? say the word");
			}
			return null;
		}

		// Status query.
		if (containsAny(text, "mp?", "your mp", "how is your mp", "hp?", "status"))
		{
			deliver(state, owner, party, "hp " + buddy.getCurrentHpPercent() + "% / mp " + buddy.getCurrentMpPercent() + "%");
			return null;
		}

		// Not a recognised command: hand it to the LLM brain (off-thread) for a natural reply that may carry an
		// action tag. The async task delivers the buddy's reply (on this same channel) when it comes back.
		askBrainAsync(owner, buddy, message, party);
		return null;
	}

	/** Sends a buddy reply after a short, human-like pause, on the channel the order arrived on. */
	private void deliver(Buddy state, Player owner, boolean party, String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return;
		}
		final long delay = 900 + Rnd.get(1300); // ~0.9-2.2s, so it doesn't fire the instant you hit enter
		ThreadPool.schedule(() ->
		{
			if (state.npc.isDead())
			{
				return;
			}
			if (party && state.npc.isInParty())
			{
				partyChat(state.npc, text);
			}
			else
			{
				whisper(state.npc, owner, text);
			}
		}, delay);
	}

	/** A short word/phrase a player would use to confirm a proposed teleport. */
	private static boolean isAffirmative(String text)
	{
		if (containsAny(text, "lets go", "let's go", "sounds good", "do it", "go for it", "head there", "take me"))
		{
			return true;
		}
		for (String word : text.split("[^a-z]+"))
		{
			switch (word)
			{
				case "yes":
				case "yep":
				case "yeah":
				case "yea":
				case "ye":
				case "yup":
				case "ok":
				case "okay":
				case "sure":
				case "go":
				case "y":
				case "alright":
				case "aight":
				{
					return true;
				}
			}
		}
		return false;
	}

	/** A short word/phrase a player would use to decline a proposed teleport. */
	private static boolean isNegative(String text)
	{
		if (containsAny(text, "not yet", "no thanks", "never mind", "nvm", "hold on", "stay here", "stay put"))
		{
			return true;
		}
		for (String word : text.split("[^a-z]+"))
		{
			switch (word)
			{
				case "no":
				case "nah":
				case "nope":
				case "wait":
				case "cancel":
				{
					return true;
				}
			}
		}
		return false;
	}

	/** {@code true} when the message is an explicit order to travel now, not just mentioning a place. */
	private static boolean isTravelOrder(String text)
	{
		return containsAny(text, "tp", "teleport", "port to", "port us", "go to", "lets go", "let's go", "take me", "bring me", "head to", "lets head", "move to", "warp", "send us", "lets tp", "go there");
	}

	/**
	 * Asks the LLM brain to interpret a free-form whisper (slang, abbreviations, chit-chat) and reply, off the
	 * network thread. The reply may carry an action tag which is executed and stripped before the buddy speaks.
	 */
	private void askBrainAsync(Player owner, Player buddy, String message, boolean party)
	{
		final int ownerId = owner.getObjectId();
		final int buddyId = buddy.getObjectId();
		ThreadPool.execute(() ->
		{
			final Buddy state = _buddies.get(buddyId);
			final Player ownerNow = (Player) World.getInstance().findObject(ownerId);
			if ((state == null) || (ownerNow == null) || state.npc.isDead())
			{
				return;
			}

			String reply = callBrain(state.npc.getName(), ownerNow.getName(), isPartiedWith(state, ownerNow), message);
			if ((reply == null) || reply.isEmpty())
			{
				// Brain was slow/offline or chose silence. Do not send a canned command-help line into live chat;
				// it breaks immersion, especially for simple greetings like "hey man".
				return;
			}

			reply = applyBuddyTags(reply, state, ownerNow, state.npc, message);
			if (!reply.isEmpty())
			{
				// Answer on the same channel the order came in on.
				if (party && state.npc.isInParty())
				{
					partyChat(state.npc, reply);
				}
				else
				{
					whisper(state.npc, ownerNow, reply);
				}
			}
		});
	}

	/** Executes any action tag in the brain's reply and returns the cleaned, speakable text. */
	private String applyBuddyTags(String reply, Buddy state, Player owner, Player buddy, String playerMessage)
	{
		final boolean partied = isPartiedWith(state, owner);
		if (partied && TAG_FOLLOW.matcher(reply).find())
		{
			state.following = true;
			ensureFollow(state, owner);
		}
		if (partied && TAG_STAY.matcher(reply).find())
		{
			state.following = false;
			buddy.getAI().setIntention(Intention.IDLE);
		}
		final Matcher grace = TAG_GRACE.matcher(reply);
		if (grace.find())
		{
			state.graceUntil = System.currentTimeMillis() + (Math.min(30, Integer.parseInt(grace.group(1))) * 60000L);
		}
		final Matcher tp = TAG_TP.matcher(reply);
		if (partied && tp.find())
		{
			final Map.Entry<String, Location> dest = matchDestinationEntry(tp.group(1));
			if (dest != null)
			{
				// Only teleport straight away if the player actually ordered the trip. When the buddy is just
				// recommending a spot ("cruma's good xp, wanna go?"), stash it and wait for them to confirm
				// instead of yanking them there mid-sentence.
				if (isTravelOrder(playerMessage.toLowerCase()))
				{
					state.pendingDestination = null;
					state.pendingDestName = null;
					teleportBuddy(state, owner, dest.getValue());
				}
				else
				{
					state.pendingDestination = dest.getValue();
					state.pendingDestName = dest.getKey();
				}
			}
		}
		if (partied && TAG_DISBAND.matcher(reply).find())
		{
			release(state, true);
		}
		// [[BUFF]] needs no action: the buff loop already keeps the owner topped up.
		reply = ANY_TAG.matcher(reply).replaceAll("");
		reply = HTML_TAG.matcher(reply).replaceAll("");
		return reply.trim();
	}

	/** Calls the brain bridge in BUDDY mode; returns the raw reply (possibly with tags), or null if offline. */
	private String callBrain(String buddyName, String ownerName, boolean partied, String message)
	{
		try
		{
			final Player buddy = World.getInstance().getPlayer(buddyName);
			final String buddyLevel = (buddy != null) ? Integer.toString(buddy.getLevel()) : "";
			final String buddyClass = className(buddy);
			final String location = (buddy != null) ? nearestTownLocation(buddy) : "";
			final String state = partied ? "partied support buddy" : "idle support buddy waiting to be invited";

			final HttpRequest request = HttpRequest.newBuilder() //
				.uri(URI.create(BRAIN_URL)) //
				.timeout(Duration.ofSeconds(BRAIN_TIMEOUT_SECONDS)) //
				.header("X-FPC", buddyName) //
				.header("X-Mode", "BUDDY") //
				.header("X-Player", ownerName) //
				.header("X-Partied", Boolean.toString(partied)) //
				.header("X-Buddy-Level", buddyLevel) //
				.header("X-Buddy-Class", buddyClass) //
				.header("X-Location", location) //
				.header("X-Buddy-State", state) //
				.header("Content-Type", "text/plain; charset=utf-8") //
				.POST(HttpRequest.BodyPublishers.ofString(message)) //
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
			LOGGER.fine(getClass().getSimpleName() + ": Brain bridge unreachable: " + e.getMessage());
		}
		return null;
	}

	private static String className(Player player)
	{
		if (player == null)
		{
			return "";
		}

		final PlayerClass playerClass = player.getPlayerClass();
		if (playerClass == null)
		{
			return "";
		}

		final String[] words = playerClass.name().toLowerCase().split("_");
		final StringBuilder name = new StringBuilder();
		for (String word : words)
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (name.length() > 0)
			{
				name.append(' ');
			}
			name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return name.toString();
	}

	private static String nearestTownLocation(Player player)
	{
		if (player == null)
		{
			return "";
		}

		final String[] names =
		{
			"Talking Island Village", "Elven Village", "Dark Elven Village", "Orc Village", "Dwarven Village",
			"Gludin Village", "Gludio", "Dion", "Giran", "Oren", "Aden", "Heine", "Hunter's Village", "Rune", "Goddard"
		};
		final int[][] coords =
		{
			{-84176, 243382, -3126}, {46934, 51567, -2976}, {9709, 15566, -4500}, {-44836, -112352, -240}, {115113, -178212, -901},
			{-80826, 149775, -3043}, {-12672, 122776, -3116}, {15670, 142983, -2705}, {83400, 147943, -3404}, {82956, 53162, -1495},
			{147450, 27064, -2208}, {111396, 219254, -3543}, {117088, 76931, -2695}, {43835, -47749, -792}, {147725, -56517, -2776}
		};

		int best = -1;
		double bestDist = Double.MAX_VALUE;
		for (int i = 0; i < names.length; i++)
		{
			final long dx = player.getX() - coords[i][0];
			final long dy = player.getY() - coords[i][1];
			final double dist = Math.sqrt((dx * dx) + (dy * dy));
			if (dist < bestDist)
			{
				bestDist = dist;
				best = i;
			}
		}

		if (best < 0)
		{
			return "";
		}
		return (bestDist <= 2500) ? ("in " + names[best]) : ("near " + names[best]);
	}

	/** Picks the next time this buddy may open small talk: a long, jittered gap so it stays occasional. */
	private static void scheduleNextChatter(Buddy state, long now)
	{
		state.nextChatter = now + CHATTER_MIN + Rnd.get((int) (CHATTER_MAX - CHATTER_MIN));
	}

	/**
	 * When its chatter window is up and it's a calm moment, the buddy starts a little small talk in party chat
	 * (off-thread, via the LLM brain - silent if the brain is offline). Keeps a partied buddy from feeling like
	 * a mute bot. A bad moment (owner fighting / buddy resting or casting) just defers it a minute.
	 */
	private void maybeChatter(Buddy state, Player buddy, Player owner, long now)
	{
		if ((state.nextChatter == 0) || (now < state.nextChatter))
		{
			return;
		}
		if (!owner.isOnline() || owner.isInCombat() || buddy.isCastingNow() || buddy.isSitting() || isMonsterNear(buddy))
		{
			state.nextChatter = now + CHATTER_RETRY; // not a good moment; try again shortly
			return;
		}
		scheduleNextChatter(state, now); // set the next window now so a slow brain call can't double-fire
		final int buddyId = buddy.getObjectId();
		final int ownerId = owner.getObjectId();
		ThreadPool.execute(() ->
		{
			final Buddy s = _buddies.get(buddyId);
			final Player o = (Player) World.getInstance().findObject(ownerId);
			if ((s == null) || (o == null) || s.npc.isDead() || !s.npc.isInParty())
			{
				return;
			}
			String line = callBrainChatter(s.npc.getName(), o.getName());
			if (line != null)
			{
				line = ANY_TAG.matcher(line).replaceAll("").trim(); // chatter carries no action tags
			}
			if ((line != null) && !line.isEmpty() && s.npc.isInParty())
			{
				partyChat(s.npc, line);
			}
		});
	}

	/** Asks the brain (BUDDYCHAT mode) for a spontaneous small-talk opener; null if the brain is offline. */
	private String callBrainChatter(String buddyName, String ownerName)
	{
		try
		{
			final HttpRequest request = HttpRequest.newBuilder() //
				.uri(URI.create(BRAIN_URL)) //
				.timeout(Duration.ofSeconds(BRAIN_TIMEOUT_SECONDS)) //
				.header("X-FPC", buddyName) //
				.header("X-Mode", "BUDDYCHAT") //
				.header("X-Player", ownerName) //
				.header("X-Partied", "true") //
				.header("Content-Type", "text/plain; charset=utf-8") //
				.POST(HttpRequest.BodyPublishers.ofString("")) //
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
			LOGGER.fine(getClass().getSimpleName() + ": Brain bridge unreachable: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Forms the party server-side and binds the buddy to its owner. Called by {@code RequestJoinParty} when a
	 * player invites a buddy (a clientless player cannot answer the normal invite dialog, so we accept here).
	 * @return {@code true} if the buddy joined the player's party
	 */
	public boolean onInvited(Player owner, Player buddy)
	{
		final Buddy state = _buddies.get(buddy.getObjectId());
		if ((state == null) || (owner == null) || buddy.isDead())
		{
			return false;
		}
		if (buddy.isInParty())
		{
			return false; // already serving someone
		}
		try
		{
			if (!owner.isInParty())
			{
				final PartyDistributionType type = (owner.getPartyDistributionType() != null) ? owner.getPartyDistributionType() : PartyDistributionType.FINDERS_KEEPERS;
				owner.setParty(new Party(owner, type));
			}
			else if (owner.getParty().getMemberCount() >= 9)
			{
				return false;
			}
			buddy.joinParty(owner.getParty());
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to party buddy " + buddy.getName() + ": " + e.getMessage());
			return false;
		}
		state.owner = owner;
		state.following = true;
		state.graceUntil = 0;
		scheduleNextChatter(state, System.currentTimeMillis());
		PhantomManager.getInstance().setBuddyEngaged(buddy, true); // exempt from the proximity despawn
		ensureFollow(state, owner);
		// Greet after a short pause rather than the instant the invite lands, so it feels like a person.
		deliver(state, owner, false, "ok partied, i'll keep you buffed. say a place to tp, or brb if you need a sec");
		startTicking();
		return true;
	}

	// ===== Fast behaviour tick =====

	private synchronized void startTicking()
	{
		if (_ticking)
		{
			return;
		}
		_ticking = true;
		ThreadPool.scheduleAtFixedRate(this::tick, TICK_INTERVAL, TICK_INTERVAL);
	}

	private void tick()
	{
		final long now = System.currentTimeMillis();
		for (Buddy state : _buddies.values())
		{
			final Player buddy = state.npc;
			try
			{
				if ((buddy == null) || (World.getInstance().findObject(buddy.getObjectId()) == null))
				{
					_buddies.remove(buddy == null ? -1 : buddy.getObjectId());
					continue;
				}
				if (state.owner == null)
				{
					continue; // idle in town; does nothing (no self-buff) until a player parties it
				}
				if (buddy.isDead())
				{
					release(state, false);
					continue;
				}
				if (!serveOwner(state, buddy, now))
				{
					continue; // released (party gone / owner offline past grace)
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Buddy tick error for " + (buddy == null ? "?" : buddy.getName()) + ": " + e.getMessage());
			}
		}
	}

	/**
	 * One service step for a partied buddy: validate the bond, manage offline grace, then (when the owner is
	 * present) heal &gt; buff &gt; recover MP &gt; follow, in that priority.
	 * @return {@code false} if the buddy was released and should be skipped
	 */
	private boolean serveOwner(Buddy state, Player buddy, long now)
	{
		final Player owner = state.owner;

		// Party bond broken (owner disbanded, kicked the buddy, or left): stop serving.
		if (!isPartiedWith(state, owner))
		{
			release(state, false);
			return false;
		}

		// Owner offline (logged off): keep the buddy for a grace window so a quick relog / teleport desync
		// doesn't lose it, then despawn. "brb" extends graceUntil beyond the default.
		if (!owner.isOnline() || (World.getInstance().findObject(owner.getObjectId()) == null))
		{
			if (state.graceUntil == 0)
			{
				state.graceUntil = now + OFFLINE_GRACE;
			}
			if (now >= state.graceUntil)
			{
				release(state, false);
				return false;
			}
			return true; // wait out the grace
		}
		// Owner is back / present: clear the offline grace (a manual brb window is left until it elapses).
		if ((state.graceUntil != 0) && (state.graceUntil <= now))
		{
			state.graceUntil = 0;
		}

		// Don't act while casting; let the current spell finish. Watchdog: a clientless caster can wedge with its
		// casting flag stuck (interrupted mid-cast), which would freeze it forever here - abort a cast that has
		// run far too long so the buddy recovers instead of going inert (it stops buffing AND following).
		if (buddy.isCastingNow())
		{
			if (state.castSince == 0)
			{
				state.castSince = now;
			}
			else if ((now - state.castSince) > 6000)
			{
				LOGGER.info("===== BUDDY-MP [cast-watchdog] " + buddy.getName() + " ===== cast wedged > 6s, aborting");
				buddy.abortCast();
				state.castSince = 0;
			}
			return true;
		}
		state.castSince = 0;

		// Occasionally open some small talk in party chat (async, never blocks the support work below).
		maybeChatter(state, buddy, owner, now);

		final boolean ownerInRange = buddy.calculateDistance2D(owner) <= SUPPORT_RANGE;

		// On-demand specific buff ("give me X" / "greater might on <name>"): cast it on the requested target (the
		// owner, or a named party member), honoured even if that target's archetype would normally skip it.
		if (state.pendingBuff != null)
		{
			final Skill buff = state.pendingBuff;
			final Player target = (state.pendingBuffTarget != null) ? state.pendingBuffTarget : owner;
			final boolean targetOk = (target != null) && !target.isDead() && (buddy.calculateDistance2D(target) <= SUPPORT_RANGE);
			if (targetOk && !buddy.isSkillDisabled(buff) && (buddy.getCurrentMp() >= buff.getMpConsume()))
			{
				if (!readyToCast(buddy))
				{
					return true; // getting up first; cast next tick (pendingBuff kept so the order isn't lost)
				}
				state.pendingBuff = null;
				state.pendingBuffTarget = null;
				buddy.setTarget(target);
				buddy.doCast(buff);
				return true;
			}
			state.pendingBuff = null; // can't satisfy it right now (out of range / dead / oom / disabled)
			state.pendingBuffTarget = null;
		}

		// On-demand "heal me": heal the owner once if not hurt.
		if (state.healNow)
		{
			if (ownerInRange && !owner.isDead())
			{
				if (buddy.isSitting())
				{
					buddy.standUp();
					return true; // getting up first; heal next tick (healNow kept so the order isn't lost)
				}
				if (tryHeal(state, buddy, owner))
				{
					state.healNow = false;
					return true;
				}
			}
			state.healNow = false; // can't satisfy it right now
		}

		// 1) Heal the owner up to ~half health, then self if low.
		if (ownerInRange && !owner.isDead() && (owner.getCurrentHpPercent() < OWNER_HEAL_PERCENT) && tryHeal(state, buddy, owner))
		{
			return true;
		}
		if ((buddy.getCurrentHpPercent() < SELF_HEAL_PERCENT) && tryHeal(state, buddy, buddy))
		{
			return true;
		}

		// 2) On-demand "(re)buff <who>": recast a full kit on each queued target, one buff per tick. Below heal so a
		// long "buff all" can't leave the owner dying while it grinds through buffs, above the normal upkeep.
		if (state.rebuffing && forceRebuff(state, buddy))
		{
			return true;
		}

		// 3) Keep the owner (and self) buffed.
		if (ownerInRange && maintainBuffs(state, buddy, true))
		{
			return true;
		}

		// 3) MP watch: warn, and when it is safe (owner not in combat, no monster near) sit to recover.
		if (handleMp(state, buddy, owner, now))
		{
			return true;
		}

		// 4) Default: keep following the owner, with a stuck watchdog so it never freezes far behind you.
		if (state.following && !buddy.isSitting())
		{
			final double dist = buddy.calculateDistance2D(owner);
			if (dist > FOLLOW_RANGE)
			{
				// If it isn't closing the gap (pathfinding stalled) re-kick the follow; if it's badly stuck or has
				// fallen a long way behind, teleport it to your side to catch up.
				if ((Math.abs(buddy.getX() - state.lastX) + Math.abs(buddy.getY() - state.lastY)) < 30)
				{
					state.stuckTicks++;
				}
				else
				{
					state.stuckTicks = 0;
				}
				if ((state.stuckTicks >= 4) || (dist > 2500))
				{
					buddy.abortCast();
					buddy.teleToLocation(new Location(owner.getX() + Rnd.get(-60, 60), owner.getY() + Rnd.get(-60, 60), owner.getZ()));
					buddy.onTeleported();
					buddy.broadcastUserInfo();
					state.stuckTicks = 0;
				}
				else
				{
					ensureFollow(state, owner);
				}
			}
			else
			{
				state.stuckTicks = 0;
			}
			state.lastX = buddy.getX();
			state.lastY = buddy.getY();
		}
		return true;
	}

	/** Heals a target with the best heal the buddy knows. @return true if a heal was cast. */
	private boolean tryHeal(Buddy state, Player buddy, Player target)
	{
		final Skill heal = bestHeal(state, buddy);
		if ((heal == null) || (buddy.getCurrentMp() < heal.getMpConsume()) || buddy.isSkillDisabled(heal))
		{
			return false;
		}
		if (!readyToCast(buddy))
		{
			return true; // getting up first; the heal lands next tick. Return "busy" so the caller doesn't fall
			// through to the MP-rest and sit the buddy straight back down while it's trying to stand and heal.
		}
		buddy.setTarget(target);
		buddy.doCast(heal);
		return true;
	}

	/**
	 * Casts the next buff that the owner (or self) is missing or that is about to expire. Casts at most one
	 * per call so the buddy works through its list over several ticks instead of locking up.
	 * @param withOwner {@code true} to buff the owner too (false = idle, self only)
	 * @return {@code true} if a buff was cast this call
	 */
	private boolean maintainBuffs(Buddy state, Player buddy, boolean withOwner)
	{
		final List<Skill> buffs = buffList(state, buddy);
		if (buffs.isEmpty())
		{
			return false;
		}
		// Self first (so Acumen etc. land and it casts the rest faster - only a couple of self buffs anyway),
		// then the owner gets the full archetype-appropriate kit.
		final Skill selfMissing = firstMissingBuff(buffs, buddy, buddy, PhantomBuffs.Tier.SELF);
		if (selfMissing != null)
		{
			return castBuff(buddy, buddy, selfMissing);
		}
		if (withOwner && (state.owner != null) && !state.owner.isDead())
		{
			final Skill missing = firstMissingBuff(buffs, buddy, state.owner, PhantomBuffs.Tier.LEADER);
			if (missing != null)
			{
				return castBuff(buddy, state.owner, missing);
			}
		}
		return false;
	}

	private Skill firstMissingBuff(List<Skill> buffs, Player buddy, Player target, PhantomBuffs.Tier tier)
	{
		final boolean caster = PhantomBuffs.isCaster(target);
		for (Skill buff : buffs)
		{
			// Only the buffs this target's archetype/tier actually wants (a caster owner gets no Haste, etc.).
			// Chant of Life is exempt - it is a situational HP-regen handled by the HP gate just below.
			if (!PhantomBuffs.wanted(buff.getId(), caster, tier) && (buff.getId() != CHANT_OF_LIFE_ID))
			{
				continue;
			}
			if (buddy.getCurrentMp() < buff.getMpConsume())
			{
				continue;
			}
			// Out of the buff's item reagent (Spirit Ore for Greater Might/Shield/Clarity): skip it, don't loop
			// re-casting a buff the engine will reject every tick. Buffers are stocked at spawn, so this is a backstop.
			if (!PhantomBuffs.canAffordReagent(buddy, buff))
			{
				continue;
			}
			// Don't keep re-applying the short Chant of Life on a healthy target; only top it up when hurt.
			if ((buff.getId() == CHANT_OF_LIFE_ID) && (target.getCurrentHpPercent() >= CHANT_OF_LIFE_HP_PERCENT))
			{
				continue;
			}
			final BuffInfo info = target.getEffectList().getBuffInfoBySkillId(buff.getId());
			if ((info == null) || (info.getTime() <= BUFF_REFRESH_SECONDS))
			{
				return buff;
			}
		}
		return null;
	}

	/**
	 * Begins a forced (re)buff: every {@code targets} entry is owed a full archetype kit, served one cast per tick.
	 */
	private void startRebuff(Buddy state, List<Player> targets)
	{
		state.rebuffQueue = targets;
		state.rebuffIdx = 0;
		state.rebuffing = !targets.isEmpty();
	}

	/** Every member of a partied owner's group (or just the owner if solo) - the target list for a "buff all" order. */
	private static List<Player> partyTargets(Player owner)
	{
		final List<Player> targets = new ArrayList<>();
		if (owner.isInParty())
		{
			targets.addAll(owner.getParty().getMembers());
		}
		else
		{
			targets.add(owner);
		}
		return targets;
	}

	/** The party member whose name appears in the order text ("buff Trevor"), or {@code null} if none is named. */
	private static Player findPartyMemberByName(Player owner, String text)
	{
		if (!owner.isInParty())
		{
			return null;
		}
		for (Player member : owner.getParty().getMembers())
		{
			final String name = member.getName().toLowerCase();
			if (!name.isEmpty() && text.contains(name))
			{
				return member;
			}
		}
		return null;
	}

	/**
	 * "(re)buff" order: recast a full archetype kit on each queued target one buff per tick (ignoring time left),
	 * walking an index across the list per target so each wanted buff fires once, advancing to the next target (or
	 * skipping one that's dead / out of range) until the queue empties, then clearing the flag.
	 * @return {@code true} while still rebuffing (a cast was issued or the buddy is standing up to cast)
	 */
	private boolean forceRebuff(Buddy state, Player buddy)
	{
		final List<Skill> all = buffList(state, buddy);
		while ((state.rebuffQueue != null) && !state.rebuffQueue.isEmpty())
		{
			final Player target = state.rebuffQueue.get(0);
			if ((target == null) || target.isDead() || (buddy.calculateDistance2D(target) > SUPPORT_RANGE))
			{
				state.rebuffQueue.remove(0); // can't reach/buff this one now - skip to the next target
				state.rebuffIdx = 0;
				continue;
			}
			final boolean caster = PhantomBuffs.isCaster(target);
			while (state.rebuffIdx < all.size())
			{
				final Skill buff = all.get(state.rebuffIdx);
				final boolean wanted = PhantomBuffs.wanted(buff.getId(), caster, PhantomBuffs.Tier.LEADER) || (buff.getId() == CHANT_OF_LIFE_ID);
				final boolean chantNotNeeded = (buff.getId() == CHANT_OF_LIFE_ID) && (target.getCurrentHpPercent() >= CHANT_OF_LIFE_HP_PERCENT);
				if (!wanted || chantNotNeeded || buddy.isSkillDisabled(buff) || (buddy.getCurrentMp() < buff.getMpConsume()) || !PhantomBuffs.canAffordReagent(buddy, buff))
				{
					state.rebuffIdx++; // skip this one
					continue;
				}
				if (!readyToCast(buddy))
				{
					return true; // getting up first; recast this same buff next tick (index NOT advanced, so none is skipped)
				}
				state.rebuffIdx++;
				buddy.setTarget(target);
				buddy.doCast(buff);
				return true;
			}
			state.rebuffQueue.remove(0); // finished this target's full kit - on to the next
			state.rebuffIdx = 0;
		}
		state.rebuffing = false;
		state.rebuffQueue = null;
		return false;
	}

	private boolean castBuff(Player buddy, Player target, Skill buff)
	{
		if (buddy.isSkillDisabled(buff))
		{
			return false;
		}
		if (!readyToCast(buddy))
		{
			return true; // getting up first; the buff lands next tick. Return "busy" so the caller doesn't fall
			// through to the MP-rest and sit the buddy straight back down while it's trying to stand and buff.
		}
		buddy.setTarget(target);
		buddy.doCast(buff);
		return true;
	}

	/**
	 * Gate a cast behind standing up. standUp() is a ~2.5s animation and a resting buddy is paralyzed while
	 * seated, so a spell fired in the same tick would silently fail.
	 * @return {@code true} if the buddy is already standing and may cast now; {@code false} if it just began
	 *         getting up - the caller must retry next tick (without consuming any one-shot order).
	 */
	private static boolean readyToCast(Player buddy)
	{
		if (buddy.isSitting())
		{
			buddy.standUp();
			return false;
		}
		return true;
	}

	/**
	 * MP sustain: warn the owner once it dips, and when it is safe (owner not fighting, no monster near) sit
	 * to recover; stand once MP is back up or a threat appears.
	 * @return {@code true} if the buddy is resting (caller should not also follow)
	 */
	private boolean handleMp(Buddy state, Player buddy, Player owner, long now)
	{
		final int mp = buddy.getCurrentMpPercent();
		// A real support player sits to recharge during downtime and only stays standing if a monster is on the
		// buddy itself (or the owner ran off). Keying on owner.isInCombat() / a 25% floor meant it almost never
		// sat - it just stood around (even idle in town) and, when farming, only whispered "low on mp".
		// "threat" must mean "something is actually attacking ME", not just "a monster exists nearby". While the
		// owner farms there is always a live mob within DANGER_RANGE, so the old isMonsterNear() check left the
		// buddy permanently "in danger" and it never sat - it just drained to empty. A real support sits to
		// recharge between casts and only pops up when a mob comes at it directly (underAttack()).
		final boolean threat = underAttack(buddy);
		final boolean ownerFar = buddy.calculateDistance2D(owner) > SUPPORT_RANGE;
		if (buddy.isSitting())
		{
			if ((mp >= MP_REST_STAND) || threat || ownerFar)
			{
				LOGGER.info("===== BUDDY-MP [stand] " + buddy.getName() + " ===== standing up (recovered=" + (mp >= MP_REST_STAND) + " underAttack=" + threat + " ownerFar=" + ownerFar + " mp=" + mp + "%)");
				buddy.standUp();
				return false;
			}
			return true; // still recovering
		}
		if ((mp < MP_LOW_PERCENT) && ((now - state.lastMpWarn) >= MP_WARN_COOLDOWN))
		{
			whisper(buddy, owner, "i'm low on mp, sitting to recover");
			state.lastMpWarn = now;
		}
		// Sit to recover MP when it drops to the low mark, it's safe, the owner is close, and a recent "stand"
		// order isn't still holding it up. Reached after heals/buffs above, so sitting never blocks supporting.
		if ((mp < MP_REST_SIT) && !threat && !ownerFar && (now >= state.noSitUntil))
		{
			// sitDown() defaults to sitDown(true), which refuses to sit while casting ("Cannot sit while casting")
			// - a clientless caster's cast flag can linger, so that path silently fails forever (it warns but
			// never sits). Abort any cast and use sitDown(false) to bypass that guard.
			buddy.abortCast();
			buddy.getAI().setIntention(Intention.IDLE);
			buddy.sitDown(false);
			if (buddy.isSitting())
			{
				LOGGER.info("===== BUDDY-MP [sit] " + buddy.getName() + " ===== sitting to recover MP (mp=" + mp + "%)");
			}
			else
			{
				LOGGER.info("===== BUDDY-MP [sit-FAILED] " + buddy.getName() + " ===== sitDown(false) rejected at mp=" + mp + "%:"
					+ " sitInProgress=" + buddy.isSittingProgress()
					+ " castingNow=" + buddy.isCastingNow()
					+ " attackingNow=" + buddy.isAttackingNow()
					+ " attackDisabled=" + buddy.isAttackDisabled()
					+ " immobilized=" + buddy.isImmobilized()
					+ " outOfControl=" + buddy.isOutOfControl()
					+ " paralyzed=" + buddy.isParalyzed());
			}
			return true;
		}
		return false;
	}

	// ===== Helpers =====

	/** Lazily collect the buddy's maintainable buffs: active, continuous, beneficial, not the heal. */
	private List<Skill> buffList(Buddy state, Player buddy)
	{
		if (state.buffs == null)
		{
			final List<Skill> list = new ArrayList<>();
			for (Skill skill : buddy.getAllSkills())
			{
				if (skill.isPassive() || skill.isToggle() || !skill.isActive() || skill.isDebuff())
				{
					continue;
				}
				// Exclude heals (incl. a continuous group heal/HoT) so they aren't recast on cooldown like a buff.
				if (skill.isContinuous() && (skill.getEffectPoint() >= 0) && !isHealSkill(skill.getId()) && !skill.hasEffectType(EffectType.HEAL, EffectType.CPHEAL, EffectType.MANAHEAL_PERCENT))
				{
					list.add(skill);
				}
			}
			state.buffs = list;
		}
		return state.buffs;
	}

	private Skill bestHeal(Buddy state, Player buddy)
	{
		if (state.heal == null)
		{
			for (int id : HEAL_PRIORITY)
			{
				final Skill known = buddy.getKnownSkill(id);
				if (known != null)
				{
					state.heal = known;
					break;
				}
			}
		}
		return state.heal;
	}

	private static boolean isHealSkill(int id)
	{
		for (int healId : HEAL_PRIORITY)
		{
			if (healId == id)
			{
				return true;
			}
		}
		return false;
	}

	/** Re-issues the follow intention toward the owner. */
	private void ensureFollow(Buddy state, Player owner)
	{
		if (!state.following || (state.npc == null) || state.npc.isDead())
		{
			return;
		}
		state.npc.setRunning();
		state.npc.getAI().setIntention(Intention.FOLLOW, owner);
	}

	private boolean isPartiedWith(Buddy state, Player owner)
	{
		return (owner != null) && state.npc.isInParty() && owner.isInParty() && (state.npc.getParty() == owner.getParty());
	}

	private static boolean isMonsterNear(Player buddy)
	{
		for (Monster monster : World.getInstance().getVisibleObjectsInRange(buddy, Monster.class, DANGER_RANGE))
		{
			if (!monster.isDead())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return {@code true} if a live monster is actually engaged on {@code buddy} (targeting it). Unlike
	 *         {@link #isMonsterNear(Player)} (any mob in the area), this stays {@code false} while the owner simply
	 *         farms nearby, so the buddy can sit to recover MP between casts and only stands when a mob turns on it.
	 */
	private static boolean underAttack(Player buddy)
	{
		for (Monster monster : World.getInstance().getVisibleObjectsInRange(buddy, Monster.class, DANGER_RANGE))
		{
			if (!monster.isDead() && (monster.getTarget() == buddy))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Releases the buddy from its party and (when appropriate) sends it home / despawns it.
	 * @param state the buddy
	 * @param graceful {@code true} for an owner-requested disband (just leave + despawn); {@code false} for an
	 *            involuntary end (party gone, owner offline past grace, buddy dead)
	 */
	private void release(Buddy state, boolean graceful)
	{
		final Player buddy = state.npc;
		state.owner = null;
		state.graceUntil = 0;
		try
		{
			if (buddy.isInParty())
			{
				buddy.getParty().removePartyMember(buddy, PartyMessageType.LEFT);
			}
		}
		catch (Exception e)
		{
			// best effort
		}
		PhantomManager.getInstance().setBuddyEngaged(buddy, false); // proximity despawn may reclaim it now
		// Hand the body back to PhantomManager: it will be cleaned up by the normal proximity deactivate, but
		// if the buddy is far from its home town (followed the owner out) despawn it now so it doesn't linger.
		PhantomManager.getInstance().despawnBuddy(buddy);
		_buddies.remove(buddy.getObjectId());
	}

	private void whisper(Player buddy, Player owner, String text)
	{
		if ((owner != null) && owner.isOnline())
		{
			owner.sendPacket(new CreatureSay(buddy, ChatType.WHISPER, buddy.getName(), text));
		}
	}

	/** Speaks a line into the buddy's party channel so every member sees it (used when ordered via party chat). */
	private void partyChat(Player buddy, String text)
	{
		if (buddy.isInParty())
		{
			buddy.getParty().broadcastCreatureSay(new CreatureSay(buddy, ChatType.PARTY, buddy.getName(), text), buddy);
		}
	}

	/**
	 * Teleports a buddy to a destination and finalizes it for a clientless player. {@code teleToLocation} only
	 * calls {@code onTeleported()} (which re-spawns + broadcasts) for players whose client is <i>detached</i>;
	 * a buddy has no client at all, so without this call it would be {@code decayMe()}'d but never re-spawned -
	 * it shows on the radar but renders for nobody. We finalize it ourselves and re-broadcast its appearance,
	 * then re-grab follow once it arrives.
	 */
	private void teleportBuddy(Buddy state, Player owner, Location destination)
	{
		final Player buddy = state.npc;
		buddy.abortCast();
		buddy.teleToLocation(destination);
		buddy.onTeleported(); // no-op if teleToLocation already finalized it; required for the clientless buddy
		buddy.broadcastUserInfo();
		if (state.following)
		{
			ThreadPool.schedule(() -> ensureFollow(state, owner), 1500); // re-grab follow once arrived
		}
	}

	/**
	 * Public reuse of the gatekeeper destination index (shared with {@link PhantomPartyManager} so recruited
	 * party members teleport to the same named spots as buddies, without re-parsing the data).
	 * @return the matched destination entry (normalized name -> spot), or {@code null} if no place is named
	 */
	public Map.Entry<String, Location> findDestination(String text)
	{
		return matchDestinationEntry(text);
	}

	/** @return the matched destination entry (normalized name -> spot), or {@code null} if no place is named. */
	private Map.Entry<String, Location> matchDestinationEntry(String text)
	{
		final String normalized = normalize(text);
		// Direct contains: "going to ruins of agony" contains "ruins of agony".
		for (Map.Entry<String, Location> entry : _destinations.entrySet())
		{
			if (normalized.contains(entry.getKey()))
			{
				return entry;
			}
		}
		return null;
	}

	/** Lowercases and strips common filler ("the", "town/village of", punctuation) for forgiving matching. */
	private static String normalize(String value)
	{
		return value.toLowerCase().replace("town of", " ").replace("village of", " ").replace("the ", " ").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
	}

	private static boolean containsAny(String text, String... needles)
	{
		for (String needle : needles)
		{
			if (text.contains(needle))
			{
				return true;
			}
		}
		return false;
	}

	public static PhantomBuddyManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final PhantomBuddyManager INSTANCE = new PhantomBuddyManager();
	}
}
