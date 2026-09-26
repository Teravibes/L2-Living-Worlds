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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.RouteData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerCraftItem;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerStoreItem;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.actor.instance.Teleporter;
import org.l2jmobius.gameserver.model.actor.instance.Warehouse;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;

/**
 * Gives fake players a sense of purpose: instead of standing still (or following a single hand-drawn
 * route), each fake player is assigned a lightweight behavior profile and driven by a cheap finite
 * state machine. The state machine only issues high-level movement goals through the normal AI
 * intention system, so all pathfinding, geodata and combat are handled by the existing engine.
 * <p>
 * Design notes:
 * <ul>
 * <li>No LLM / heavy logic is used in the tick loop - this scales to hundreds of bots.</li>
 * <li>Bots already controlled by {@link WalkingManager} routes are left untouched.</li>
 * <li>Combat is delegated to {@code AttackableAI}; the FSM simply backs off while fighting.</li>
 * </ul>
 */
public class FakePlayerBehaviorManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerBehaviorManager.class.getName());

	// How often we look for newly spawned / despawned fake players.
	private static final long DISCOVERY_INTERVAL = 30000;
	// How often each bot's state machine is evaluated.
	private static final long BEHAVIOR_INTERVAL = 3000;
	// After a fight we wait this long before resuming wandering.
	private static final long COMBAT_BACKOFF = 6000;
	// A killed field bot is replaced (with a fresh identity) after roughly this long.
	private static final long RESPAWN_DELAY = 45000;

	// Route following: how close (2D) counts as "reached this waypoint", how many times to re-issue a
	// stalled move before recovering, and the max distance we will teleport-recover across. Mirrors the
	// native WalkingManager: confirm real arrival, re-kick interrupted moves, teleport onto the node as a
	// last resort instead of silently skipping it.
	private static final int ARRIVE_DIST = 70;
	private static final int MAX_MOVE_ATTEMPTS = 3;
	private static final int STUCK_TELEPORT_DIST = 3000;

	// "Come meet me" override: how long a summon lasts, how close counts as arrived, and how far we look
	// for the requested landmark NPC (kept tight so meetups stay within the bot's own town).
	private static final int SUMMON_ARRIVE_DIST = 120;
	private static final int SUMMON_SEARCH_RANGE = 6000;
	// Wider than landmark search: used only to find a roaming fake player who can answer a WTB/WTS ad.
	private static final int TRADE_RESPONDER_SEARCH_RANGE = 12000;
	private static final long TRADE_CLAIM_TTL = 30000; // FPC-066: how long an atomic responder claim holds a bot before setupDeal supersedes it, or it lapses if the deal never sets up
	private final Object _tradeClaimLock = new Object(); // guards the check-and-claim in tryClaimTradeResponder (FPC-066)
	// Meet beside a landmark NPC instead of on top of it, so the fake player does not overlap the gatekeeper,
	// warehouse keeper or merchant model.
	private static final int MEET_OFFSET_MIN = 160;
	private static final int MEET_OFFSET_MAX = 260;
	// A meet spot must sit on roughly the same floor as the landmark NPC (not on a roof or a lower terrace).
	private static final int MEET_MAX_DZ = 150;
	// How much closer the bot must get to count as progress on its way to the meet spot.
	private static final int SUMMON_PROGRESS_STEP = 50;
	// Give up when the bot has made no progress toward the meet spot for this long (e.g. no path), instead of
	// wall-banging. Measured from the last progress, not from the start, so a long walk across town is fine.
	private static final long SUMMON_GIVEUP = 45000;
	// While waiting at the meet spot: ask "still coming?" after this, then leave if no reply within the grace.
	private static final long MEET_NUDGE_AFTER = 300000;
	private static final long MEET_NUDGE_GRACE = 180000;
	// Absolute safety: never hold a bot at a meet spot longer than this, whatever happens.
	private static final long MEET_HARD_CAP = 1800000;
	// A trade offer was PMed but the player never agreed a meet: release the bot's reservation after this.
	private static final long OFFER_TTL = 420000; // 7 min

	// Procedural deployment: where auto-spawned bots are scattered and how wide.
	// Default center is the Giran town square area; tune to taste.
	private static final Location DEPLOY_CENTER = new Location(83400, 147600, -3400);
	private static final int DEPLOY_RADIUS = 1500;
	// Delay before deploying, to let the world and geodata finish loading.
	private static final long DEPLOY_DELAY = 15000;
	// Level range rolled for generated bots (used for flavor now; drives zone choice in Phase 2b).
	private static final int DEPLOY_MIN_LEVEL = 1;
	private static final int DEPLOY_MAX_LEVEL = 40;

	private enum ProfileType
	{
		/** Random short hops around a home anchor (town life, idle farming spot). */
		WANDER,
		/** Cycles through an ordered list of points (guards, travellers). */
		PATROL,
		/** Moves to a RANDOM point from the list each time (with long idles): "purposeful" town movement
		 * between points of interest like the gatekeeper, warehouse and shops. */
		VISIT
	}

	private enum Phase
	{
		IDLE,
		MOVING
	}

	private static class Profile
	{
		String name;
		ProfileType type = ProfileType.WANDER;
		int radius = 600; // WANDER: how far from the anchor a hop may land
		boolean run = false;
		int pauseMin = 8; // seconds to idle between hops
		int pauseMax = 25;
		final List<Location> points = new ArrayList<>(); // PATROL/VISIT waypoints
		final Map<Integer, Integer> pointDelays = new HashMap<>(); // index -> delay seconds (0 = use default pause)
	}

	private static class BotState
	{
		final Profile profile;
		final Location home;
		final int radius; // wander radius around the home anchor (from the population, else the profile)
		final Population population; // owning group (null for discovered bots); used for respawn
		Phase phase = Phase.IDLE;
		long nextActionTime;
		int patrolIndex;
		int pendingArrivalDelay = -1; // seconds to pause after next arrival (-1 = use profile default)
		Location moveTarget; // the waypoint currently being travelled to (for arrival check + re-kick)
		int moveAttempts; // consecutive re-issues of a move that stalled before reaching moveTarget

		// Player-requested "come meet me" override: while set, the bot walks to this spot and then waits
		// there (pinned) until the player shows up, calls it off, or stops answering.
		Location summonTarget;
		long summonStart; // travel start
		String summonSpot; // the agreed landmark keyword ("gatekeeper", "warehouse", "shop"), for the brain's meet note
		double summonBestDistance; // closest the bot has come to summonTarget on this trip
		long summonLastProgress; // when the bot last got closer (for the give-up timer)
		long summonHardExpire; // absolute safety cap
		boolean summonArrived;
		long waitingSince; // start of the current wait window (reset whenever the player interacts)
		boolean summonNudged; // already asked "still coming?" for this window
		Player summonPlayer;

		// A trade arranged from chat: the store to open when the bot reaches the meet spot.
		int pendingStoreType; // PrivateStoreType id to activate on arrival, or 0 for a plain meet
		List<FakePlayerStoreItem> pendingStock;
		String pendingTitle;
		long pendingDealExpire; // when an offered-but-not-yet-agreed deal reservation lapses (0 = none)
		Player dealPlayer; // the player an offered/pending deal is with, so an abandoned offer can clear its chat context (FPC-019)
		long dealClaimExpire; // FPC-066: a short atomic hold taken when this bot is selected as a trade responder, before setupDeal fills the deal in (0 = none)
		boolean dealActive; // a deal store is currently open on this bot

		BotState(Profile profile, Location home, int radius, Population population)
		{
			this.profile = profile;
			this.home = home;
			this.radius = radius;
			this.population = population;
		}
	}

	private static class Population
	{
		String name;
		Location center;
		int radius = 1000;
		int count;
		int minLevel = 1;
		int maxLevel = 60;
		String profileName;
		String routeName; // optional: name of a route from data/routes/ to use instead of inline points
		boolean routeReversed; // traverse the named route in reverse order
		Race race; // optional dominant race for this group (e.g. a Dwarven village)
		boolean respawn; // field bots die; respawn a fresh replacement to keep the zone populated
		String storeType; // null, or SELL / BUY / PACKAGE / CRAFT -> seated private-store vendors
		boolean fullStock; // market hub (e.g. Giran): vendors ignore the level cap and stock every grade
		final List<Location> polygon = new ArrayList<>(); // optional area; bots spawn inside it instead of a circle
	}

	private final Map<String, Profile> _profiles = new HashMap<>();
	private final Map<String, String> _assignByName = new HashMap<>(); // lowercase fpc name -> profile
	private final Map<Integer, String> _assignByNpcId = new HashMap<>(); // npc id -> profile
	private final List<Population> _populations = new ArrayList<>(); // procedural deployment groups
	private String _defaultProfile = null;

	private final Map<Integer, BotState> _bots = new ConcurrentHashMap<>(); // objectId -> state
	// FPC-018: the recurring discovery/tick loops are scheduled ONCE for the JVM lifetime; a reload replaces data and
	// bots, not these loops, so reloads no longer stack duplicate loops. Delayed deploy/respawn work carries the
	// generation it was scheduled under and no-ops if a reload has since bumped it.
	private boolean _loopsStarted = false;
	private volatile int _generation = 0;
	private int _baseId; // base template id used for all generated bots (resolved at deploy)

	protected FakePlayerBehaviorManager()
	{
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED && FakePlayersConfig.FAKE_PLAYER_BEHAVIOR)
		{
			load();
		}
	}

	@Override
	public void load()
	{
		_generation++; // FPC-018: a new (re)load; delayed deploy/respawn work from the previous generation now no-ops.
		_profiles.clear();
		_assignByName.clear();
		_assignByNpcId.clear();
		_populations.clear();
		parseDatapackFile("data/FakePlayerBehavior.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _profiles.size() + " behavior profiles and " + _populations.size() + " populations.");
		start();
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "profile", profileNode ->
			{
				final StatSet set = new StatSet(parseAttributes(profileNode));
				final Profile profile = new Profile();
				profile.name = set.getString("name");
				profile.type = parseProfileType(set.getString("type", "WANDER"), profile.name);
				profile.radius = set.getInt("radius", 600);
				profile.run = set.getBoolean("run", false);
				profile.pauseMin = set.getInt("pauseMin", 8);
				profile.pauseMax = set.getInt("pauseMax", 25);
				forEach(profileNode, "point", pointNode ->
				{
					final StatSet p = new StatSet(parseAttributes(pointNode));
					final int idx = profile.points.size();
					profile.points.add(new Location(p.getInt("x"), p.getInt("y"), p.getInt("z")));
					if (p.contains("delay"))
					{
						profile.pointDelays.put(idx, p.getInt("delay"));
					}
				});
				_profiles.put(profile.name, profile);
			});

			forEach(listNode, "assign", assignNode ->
			{
				final StatSet set = new StatSet(parseAttributes(assignNode));
				final String profileName = set.getString("profile");
				if (set.contains("npcId"))
				{
					_assignByNpcId.put(set.getInt("npcId"), profileName);
				}
				if (set.contains("name"))
				{
					_assignByName.put(set.getString("name").toLowerCase(), profileName);
				}
			});

			forEach(listNode, "default", defaultNode ->
			{
				_defaultProfile = new StatSet(parseAttributes(defaultNode)).getString("profile");
			});

			forEach(listNode, "population", populationNode ->
			{
				final StatSet set = new StatSet(parseAttributes(populationNode));
				final Population population = new Population();
				population.name = set.getString("name", "unnamed");
				population.center = new Location(set.getInt("x"), set.getInt("y"), set.getInt("z"));
				population.radius = set.getInt("radius", 1000);
				population.count = set.getInt("count", 0);
				population.minLevel = set.getInt("minLevel", 1);
				population.maxLevel = set.getInt("maxLevel", 60);
				population.respawn = set.getBoolean("respawn", false);
				population.storeType = normalizeStoreType(population.name, set.contains("store") ? set.getString("store") : null);
				population.fullStock = set.getBoolean("fullStock", false);
				population.profileName = set.getString("profile", _defaultProfile);
				population.routeName = set.contains("route") ? set.getString("route") : null;
			population.routeReversed = set.getBoolean("reversed", false);
				forEach(populationNode, "point", pointNode ->
				{
					final StatSet p = new StatSet(parseAttributes(pointNode));
					population.polygon.add(new Location(p.getInt("x"), p.getInt("y"), p.getInt("z", population.center.getZ())));
				});
				if (set.contains("race"))
				{
					try
					{
						population.race = Race.valueOf(set.getString("race").toUpperCase());
					}
					catch (Exception e)
					{
						LOGGER.warning(getClass().getSimpleName() + ": Unknown race '" + set.getString("race") + "' in population '" + population.name + "'.");
					}
				}
				_populations.add(population);
			});
		});
	}

	// The population store types the deployment code understands. Anything else must be rejected at load, not fall
	// through to a SELL vendor (FPC-017).
	private static final java.util.Set<String> SUPPORTED_STORE_TYPES = java.util.Set.of("SELL", "BUY", "PACKAGE", "CRAFT", "MANUFACTURE", "AASELL", "AABUY");

	/**
	 * Validate and canonicalize a population's {@code store} attribute (FPC-017). The XSD only says {@code xs:string},
	 * so a typo like {@code store="BUYY"} used to pass validation and silently deploy a SELL vendor. This normalizes
	 * to the documented upper-case form, accepts only the supported set, and disables the store (returns {@code null})
	 * with a named warning for anything else - so a mistake never reverses the intended market direction unnoticed.
	 * @param populationName the population the value came from, for the warning
	 * @param raw the raw store attribute, or {@code null} when none was set
	 * @return the canonical upper-case store type, or {@code null} to run the population with no store
	 */
	private static String normalizeStoreType(String populationName, String raw)
	{
		if (raw == null)
		{
			return null;
		}
		final String kind = raw.trim().toUpperCase(java.util.Locale.ROOT);
		if (kind.isEmpty() || !SUPPORTED_STORE_TYPES.contains(kind))
		{
			LOGGER.warning(FakePlayerBehaviorManager.class.getSimpleName() + ": population \"" + populationName //
				+ "\" has an unsupported store type \"" + raw + "\"; running it with no store. Supported: " + SUPPORTED_STORE_TYPES + ".");
			return null;
		}
		return kind;
	}

	private void start()
	{
		if (_profiles.isEmpty())
		{
			return;
		}
		// FPC-018: schedule the recurring loops exactly once. A reload replaces data and bots but must NOT schedule a
		// second discovery/tick pair - that was the leak that made every reload permanently raise the tick frequency
		// and let several workers process one BotState at once.
		if (!_loopsStarted)
		{
			_loopsStarted = true;
			ThreadPool.scheduleAtFixedRate(this::discover, DISCOVERY_INTERVAL, DISCOVERY_INTERVAL);
			ThreadPool.scheduleAtFixedRate(this::tick, BEHAVIOR_INTERVAL, BEHAVIOR_INTERVAL);
			LOGGER.info(getClass().getSimpleName() + ": Fake player behavior enabled.");
		}
		// Deploy this (re)load's population after the usual delay, tagged with the current generation so a reload
		// issued before it fires cancels it instead of deploying a now-stale population on top of the new one.
		if (!_populations.isEmpty() || (FakePlayersConfig.FAKE_PLAYER_DEPLOY_COUNT > 0))
		{
			final int generation = _generation;
			ThreadPool.schedule(() ->
			{
				if (generation == _generation)
				{
					deploy();
				}
			}, DEPLOY_DELAY);
		}
	}

	/**
	 * Procedurally deploys fake players. Each {@code <population>} defines a group (center, radius,
	 * count, level range, behavior profile); bots are scattered within the group, anchored to the
	 * group center so clusters stay tight, and registered with the behavior FSM. If no populations are
	 * defined it falls back to a single group of {@code FakePlayerDeployCount} bots at {@link #DEPLOY_CENTER}.
	 */
	private void deploy()
	{
		// All generated bots share one base template; their look is overridden per-instance.
		_baseId = FakePlayersConfig.FAKE_PLAYER_BASE_NPC_ID;
		if (_baseId <= 0)
		{
			final List<Integer> templateIds = new ArrayList<>(FakePlayerData.getInstance().getFakePlayerIds());
			if (templateIds.isEmpty())
			{
				LOGGER.warning(getClass().getSimpleName() + ": No fake player templates found - cannot deploy bots.");
				return;
			}
			_baseId = templateIds.get(0);
		}

		int deployed = 0;
		if (_populations.isEmpty())
		{
			final Population fallback = new Population();
			fallback.name = "default";
			fallback.center = DEPLOY_CENTER;
			fallback.radius = DEPLOY_RADIUS;
			fallback.count = FakePlayersConfig.FAKE_PLAYER_DEPLOY_COUNT;
			fallback.minLevel = DEPLOY_MIN_LEVEL;
			fallback.maxLevel = DEPLOY_MAX_LEVEL;
			fallback.profileName = _defaultProfile;
			for (int i = 0; i < fallback.count; i++)
			{
				if (deployOne(fallback))
				{
					deployed++;
				}
			}
		}
		else
		{
			for (Population population : _populations)
			{
				int groupDeployed = 0;
				for (int i = 0; i < population.count; i++)
				{
					if (deployOne(population))
					{
						groupDeployed++;
					}
				}
				deployed += groupDeployed;
				if (groupDeployed < population.count)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Population '" + population.name + "' deployed " + groupDeployed + "/" + population.count + " (bad anchor/geodata?).");
				}
			}
		}

		LOGGER.info("===== " + deployed + " BOTS DEPLOYED =====");
	}

	/**
	 * Spawns a single bot in the given population: scatters it within the group, gives it a generated
	 * identity, and registers it with the behavior FSM.
	 * @param population the group to spawn into
	 * @return {@code true} if a bot was spawned
	 */
	private boolean deployOne(Population population)
	{
		Profile profile = population.profileName == null ? null : _profiles.get(population.profileName);
		// If the population references a named route, create a per-population profile copy with those
		// waypoints injected so bots follow the recorded path.
		if ((population.routeName != null) && (profile != null))
		{
			final List<Location> routePoints = RouteData.getInstance().getRoute(population.routeName);
			if (routePoints != null && !routePoints.isEmpty())
			{
				final Profile routed = new Profile();
				routed.name = profile.name;
				routed.type = profile.type == ProfileType.WANDER ? ProfileType.VISIT : profile.type;
				routed.radius = profile.radius;
				routed.run = profile.run;
				routed.pauseMin = profile.pauseMin;
				routed.pauseMax = profile.pauseMax;
				if (population.routeReversed)
				{
					final List<Location> reversed = new ArrayList<>(routePoints);
					java.util.Collections.reverse(reversed);
					routed.points.addAll(reversed);
				}
				else
				{
					routed.points.addAll(routePoints);
				}
				// Route files store only geometry; the per-waypoint delays live on the editor-generated
				// profile, keyed in the same effective (reversal-applied) order as routed.points. Carry them
				// over so named/recorded routes honor their delays (manual routes already keep this profile).
				for (Map.Entry<Integer, Integer> delayEntry : profile.pointDelays.entrySet())
				{
					if (delayEntry.getKey() < routed.points.size())
					{
						routed.pointDelays.put(delayEntry.getKey(), delayEntry.getValue());
					}
				}
				profile = routed;
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": Population '" + population.name + "' references unknown route '" + population.routeName + "'.");
			}
		}
		try
		{
			final int x;
			final int y;
			if (population.polygon.size() >= 3)
			{
				// Area population: spawn at a random point inside the drawn shape.
				final Location pt = randomPointInPolygon(population);
				x = pt.getX();
				y = pt.getY();
			}
			else
			{
				final double angle = Rnd.nextDouble() * 2 * Math.PI;
				final int distance = Rnd.get(0, population.radius);
				x = population.center.getX() + (int) (Math.cos(angle) * distance);
				y = population.center.getY() + (int) (Math.sin(angle) * distance);
			}
			final Location loc = GeoEngine.getInstance().getValidLocation(population.center, new Location(x, y, population.center.getZ()));
			// Snap to the ground height. With geodata loaded this corrects open-field Z automatically;
			// without geodata it is a no-op (so field bots need geodata to place reliably outdoors).
			final int groundZ = GeoEngine.getInstance().getHeight(loc.getX(), loc.getY(), loc.getZ());

			final Spawn spawn = new Spawn(_baseId);
			spawn.setXYZ(loc.getX(), loc.getY(), groundZ);
			spawn.setHeading(Rnd.get(65536));
			spawn.setAmount(1);
			// We own respawn ourselves (with a fresh identity) so the engine's template respawn is off.
			spawn.stopRespawn();
			final Npc npc = spawn.doSpawn(false);
			if (npc != null)
			{
				// Give the bot its own procedurally generated identity and broadcast the new look.
				// Only dwarves craft, so a crafter population is locked to the Dwarven race.
					final boolean isCrafter = (population.storeType != null) && (population.storeType.equalsIgnoreCase("CRAFT") || population.storeType.equalsIgnoreCase("MANUFACTURE"));
					final FakePlayerAppearance look = FakePlayerAppearanceFactory.generate(population.minLevel, population.maxLevel, isCrafter ? Race.DWARF : population.race, isCrafter);
					// A share of the town crowd belongs to a random bot clan (BotClans.xml fakePlayerClanChance), so their
					// crest shows over their head. The live bot-clan id is stamped on the look and read by FakePlayerInfo.
					final int fpClanChance = BotClanManager.getInstance().getFakePlayerClanChance();
					if ((fpClanChance > 0) && (Rnd.get(100) < fpClanChance))
					{
						final Clan botClan = BotClanManager.getInstance().getRandomClan();
						if (botClan != null)
						{
							look.setClanId(botClan.getId());
							look.setTitle(BotClanManager.getInstance().randomTitle()); // clan members bear a generated title
						}
					}
				if (population.storeType != null)
				{
					final String kind = population.storeType.toUpperCase();
					final int level = look.getLevel();
					// A market-hub shop (e.g. Giran) ignores the level cap and stocks every grade; elsewhere
					// the population's level range gates the tier, so towns sell region-appropriate goods.
					final boolean fullStock = population.fullStock;
					if (kind.equals("CRAFT") || kind.equals("MANUFACTURE"))
					{
						// A real manufacture store: offers recipes; the customer brings the materials.
						final List<FakePlayerCraftItem> recipes = FakePlayerStoreFactory.generateCraftRecipes(level, fullStock);
						look.setCraftItems(recipes);
						look.setStore(PrivateStoreType.MANUFACTURE.getId(), FakePlayerStoreFactory.craftTitle(recipes));
					}
					else
					{
						final List<FakePlayerStoreItem> stock;
						final int storeId;
						// Sign logic in title() only distinguishes BUY from everything-else, so dedicated
						// Ancient Adena vendors map onto the plain BUY/SELL title kind.
						String titleKind = kind;
						if (kind.equals("AABUY"))
						{
							// Vendor buys Ancient Adena for adena: the player sells their seal-stone winnings.
							stock = FakePlayerStoreFactory.generateAncientAdenaBuy();
							storeId = PrivateStoreType.BUY.getId();
							titleKind = "BUY";
						}
						else if (kind.equals("AASELL"))
						{
							// Vendor sells Ancient Adena for adena: the player buys some to spend at the Mammon merchants.
							stock = FakePlayerStoreFactory.generateAncientAdenaSell();
							storeId = PrivateStoreType.SELL.getId();
							titleKind = "SELL";
						}
						else if (kind.equals("BUY"))
						{
							stock = FakePlayerStoreFactory.generateBuy(level, fullStock);
							storeId = PrivateStoreType.BUY.getId();
						}
						else
						{
							stock = FakePlayerStoreFactory.generateSell(level, fullStock);
							storeId = kind.equals("PACKAGE") ? PrivateStoreType.PACKAGE_SELL.getId() : PrivateStoreType.SELL.getId();
						}
						look.setStoreItems(stock);
						look.setStore(storeId, FakePlayerStoreFactory.title(titleKind, stock));
					}
				}
				npc.setFakePlayerAppearance(look);
				if (look.getPrivateStoreType() != 0)
				{
					// Seated vendors must never move: stop their own NPC AI and pin them in place.
					npc.disableCoreAI(true);
					npc.setImmobilized(true);
				}
				else
				{
					// Movers: flag as a walker and kill template random-walk, exactly like the native
					// WalkingManager. Otherwise the core NPC AI drags the bot back to its spawn the moment it
					// travels past MaxDriftRange (default 300) and issues its own random walks, both of which
					// fight our route MOVE_TO commands - the "walks out then snaps back home" behavior.
					npc.setWalker();
					npc.setRandomWalking(false);
				}
				npc.broadcastInfo();

				if (profile != null)
				{
					// Anchor to the population center (not the scatter point) so clusters stay tight.
					_bots.put(npc.getObjectId(), new BotState(profile, population.center, population.radius, population));
				}
				return true;
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to deploy bot in '" + population.name + "' (baseId=" + _baseId + "): " + e.getMessage());
		}
		return false;
	}

	/**
	 * Scans the world for fake players, registering newly spawned ones and dropping despawned ones.
	 * Runs infrequently; the per-bot tick works off the registered map.
	 */
	private void discover()
	{
		for (WorldObject object : World.getInstance().getVisibleObjects())
		{
			if (!object.isNpc())
			{
				continue;
			}
			final Npc npc = object.asNpc();
			if (!npc.isFakePlayer() || _bots.containsKey(npc.getObjectId()))
			{
				continue;
			}
			// Leave route-driven fake players to the WalkingManager.
			if (WalkingManager.getInstance().isTargeted(npc))
			{
				continue;
			}
			final Profile profile = resolveProfile(npc);
			if (profile != null)
			{
				// Same as deployOne: non-vendor movers must be walkers so the core AI does not drag them
				// home past MaxDriftRange or random-walk over our route commands.
				final FakePlayerAppearance look = npc.getFakePlayerAppearance();
				if ((look == null) || (look.getPrivateStoreType() == 0))
				{
					npc.setWalker();
					npc.setRandomWalking(false);
				}
				_bots.put(npc.getObjectId(), new BotState(profile, new Location(npc.getX(), npc.getY(), npc.getZ()), profile.radius, null));
			}
		}
	}

	/**
	 * Picks a random point inside a population's polygon (rejection sampling), falling back to the
	 * centroid if sampling fails.
	 * @param pop the area population
	 * @return a location inside the polygon
	 */
	private Location randomPointInPolygon(Population pop)
	{
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		for (Location v : pop.polygon)
		{
			minX = Math.min(minX, v.getX());
			maxX = Math.max(maxX, v.getX());
			minY = Math.min(minY, v.getY());
			maxY = Math.max(maxY, v.getY());
		}
		for (int i = 0; i < 30; i++)
		{
			final int rx = Rnd.get(minX, maxX);
			final int ry = Rnd.get(minY, maxY);
			if (isInPolygon(rx, ry, pop.polygon))
			{
				return new Location(rx, ry, pop.center.getZ());
			}
		}
		return pop.center;
	}

	private static boolean isInPolygon(int px, int py, List<Location> poly)
	{
		boolean inside = false;
		for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++)
		{
			final int xi = poly.get(i).getX(), yi = poly.get(i).getY();
			final int xj = poly.get(j).getX(), yj = poly.get(j).getY();
			if (((yi > py) != (yj > py)) && (px < ((double) (xj - xi) * (py - yi) / (yj - yi)) + xi))
			{
				inside = !inside;
			}
		}
		return inside;
	}

	/**
	 * Resolves a profile type name to its enum, tolerating a value that is no longer supported. The retired
	 * FARM ("field fake player") type is the main case: field phantoms cover that role far better, so a fake
	 * player is never allowed to hunt the field. An unknown or retired type quietly falls back to WANDER (a
	 * harmless idle drift) with a warning, rather than throwing and failing the whole file load.
	 * @param raw the type attribute as written in the XML
	 * @param profileName the owning profile name, for the warning message
	 * @return the resolved type, or {@link ProfileType#WANDER} when the value is not recognised
	 */
	private ProfileType parseProfileType(String raw, String profileName)
	{
		try
		{
			return Enum.valueOf(ProfileType.class, raw.toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Profile '" + profileName + "' uses unsupported type '" + raw + "'; falling back to WANDER.");
			return ProfileType.WANDER;
		}
	}

	private Profile resolveProfile(Npc npc)
	{
		String name = _assignByName.get(npc.getName().toLowerCase());
		if (name == null)
		{
			name = _assignByNpcId.get(npc.getId());
		}
		if (name == null)
		{
			name = _defaultProfile;
		}
		return name == null ? null : _profiles.get(name);
	}

	private void tick()
	{
		final long now = System.currentTimeMillis();
		for (Map.Entry<Integer, BotState> entry : _bots.entrySet())
		{
			final WorldObject object = World.getInstance().findObject(entry.getKey());
			if ((object == null) || !object.isNpc())
			{
				_bots.remove(entry.getKey());
				continue;
			}

			final Npc npc = object.asNpc();
			if (npc.isDead())
			{
				final BotState dead = _bots.remove(entry.getKey());
				// Replace fallen field bots with a fresh hunter so the zone stays populated. FPC-018: tag the respawn
				// with the current generation, so a reload before it fires drops it instead of spawning a bot from a
				// population that no longer exists.
				if ((dead != null) && (dead.population != null) && dead.population.respawn)
				{
					final int generation = _generation;
					ThreadPool.schedule(() ->
					{
						if (generation == _generation)
						{
							deployOne(dead.population);
						}
					}, RESPAWN_DELAY);
				}
				continue;
			}

			try
			{
				process(npc, entry.getValue(), now);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Behavior error for " + npc.getName() + ": " + e.getMessage());
			}
		}
	}

	private void process(Npc npc, BotState state, long now)
	{
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();

		// "Come meet me" override takes priority over the normal routine while it is active. (Checked
		// before the vendor short-circuit so a bot running a temporary deal store still runs this loop.)
		if (state.summonTarget != null)
		{
			final Player who = state.summonPlayer;
			if (now >= state.summonHardExpire)
			{
				endMeet(npc, state); // safety cap; fall through to normal behavior
			}
			else if (state.summonArrived)
			{
				// A deal store that has sold out / been satisfied closes itself: end the meet and resume.
				if (state.dealActive && (look != null) && (look.getPrivateStoreType() == 0))
				{
					endMeet(npc, state);
					return;
				}
				// Waiting at the spot (pinned). Nudge once after a while, then leave if still no answer.
				if (!state.summonNudged && ((now - state.waitingSince) > MEET_NUDGE_AFTER))
				{
					state.summonNudged = true;
					state.waitingSince = now; // start the grace countdown
					if (who != null)
					{
						final String nudge = state.dealActive ? (Rnd.nextBoolean() ? "u buying or what?" : "anything else? bout to close up") : (Rnd.nextBoolean() ? "u still coming?" : "still waiting, u coming or not?");
						FakePlayerChatManager.getInstance().sendChat(who, npc.getName(), nudge);
					}
					return;
				}
				if (state.summonNudged && ((now - state.waitingSince) > MEET_NUDGE_GRACE))
				{
					if (who != null)
					{
						final String bye = state.dealActive ? (Rnd.nextBoolean() ? "closing up, hit me later" : "im done, cya") : (Rnd.nextBoolean() ? "guess not, im off" : "k im leaving, hit me up later");
						FakePlayerChatManager.getInstance().sendChat(who, npc.getName(), bye);
					}
					endMeet(npc, state); // fall through to normal behavior
				}
				else
				{
					return; // keep waiting
				}
			}
			else if (npc.isInCombat() || npc.isAttackingNow())
			{
				state.summonLastProgress = now; // a fight is not a stall
				return; // let it fight; it resumes heading over afterwards
			}
			else if (npc.calculateDistance3D(state.summonTarget) <= SUMMON_ARRIVE_DIST)
			{
				// 3D so a spot on another floor (a terrace above or below the landmark) does not count as arrived.
				// Arrived: pin in place so the core AI doesn't immediately walk it back to its spawn.
				state.summonArrived = true;
				state.waitingSince = now;
				state.summonNudged = false;
				state.phase = Phase.IDLE;
				npc.disableCoreAI(true);
				npc.setImmobilized(true);
				// If a trade was arranged, open the real store now so the player can buy/sell.
				if ((state.pendingStoreType != 0) && (look != null))
				{
					look.setStoreItems(state.pendingStock);
					look.setStore(state.pendingStoreType, state.pendingTitle);
					state.dealActive = true;

					LOGGER.info("FPC_DEAL_OPEN_ARRIVAL bot=" + npc.getName()
						+ " objId=" + npc.getObjectId()
						+ " storeType=" + look.getPrivateStoreType()
						+ " sitting=" + look.isSitting()
						+ " title=\"" + look.getStoreMessage() + "\""
						+ " stock=" + look.getStoreItems().size()
						+ " decayed=" + npc.isDecayed()
						+ " moving=" + npc.isMoving()
						+ " immobilized=" + npc.isImmobilized()
						+ " x=" + npc.getX()
						+ " y=" + npc.getY()
						+ " z=" + npc.getZ());

					state.pendingStoreType = 0;
					state.pendingStock = null;
					state.pendingTitle = null;
					refreshFakePlayerVisual(npc); // force client to rebuild fake-player visual with sitting/store state
				}
				LOGGER.info("FPC_MEET_ARRIVAL bot=" + npc.getName()
					+ " spot=" + state.summonSpot
					+ " distance2D=" + (int) npc.calculateDistance2D(state.summonTarget)
					+ " distance3D=" + (int) npc.calculateDistance3D(state.summonTarget)
					+ " x=" + npc.getX() + " y=" + npc.getY() + " z=" + npc.getZ()
					+ " targetX=" + state.summonTarget.getX() + " targetY=" + state.summonTarget.getY() + " targetZ=" + state.summonTarget.getZ());
				if (who != null)
				{
					final String line = state.dealActive ? (Rnd.nextBoolean() ? "im here, check my store" : "here, open my shop") : (Rnd.nextBoolean() ? "im here" : "here, where are u");
					FakePlayerChatManager.getInstance().sendChat(who, npc.getName(), line);
				}
				return;
			}
			else if (!madeMeetProgress(npc, state, now) && ((now - state.summonLastProgress) > SUMMON_GIVEUP))
			{
				// No progress toward the spot for a while (likely no path); stop trying and say so.
				LOGGER.info("FPC_MEET_GIVEUP bot=" + npc.getName()
					+ " spot=" + state.summonSpot
					+ " distance3D=" + (int) npc.calculateDistance3D(state.summonTarget)
					+ " x=" + npc.getX() + " y=" + npc.getY() + " z=" + npc.getZ()
					+ " targetX=" + state.summonTarget.getX() + " targetY=" + state.summonTarget.getY() + " targetZ=" + state.summonTarget.getZ());
				if (who != null)
				{
					FakePlayerChatManager.getInstance().sendChat(who, npc.getName(), Rnd.nextBoolean() ? "cant get there, come to me?" : "im stuck, where r u exactly");
				}
				endMeet(npc, state); // fall through to normal behavior
			}
			else
			{
				if (!npc.isMoving())
				{
					// Aim at the real destination (not a wall-clamped point) so the engine pathfinds around
					// obstacles instead of repeatedly walking into a wall.
					npc.setRunning();
					npc.getAI().setIntention(Intention.MOVE_TO, state.summonTarget);
				}
				return;
			}
		}

		// A trade offer was PMed but the player never agreed a meet: drop the stale reservation so the bot
		// can take new ads again. (Once a meet starts, pendingDealExpire is cleared, so this never fires
		// mid-deal.)
		if ((state.pendingStoreType != 0) && (state.pendingDealExpire > 0) && (now > state.pendingDealExpire))
		{
			// FPC-019: also drop the structured chat context for this pair. Without this the ACTIVE_DEALS entry
			// outlived the reservation, so a later unrelated whisper was grounded with the dead deal's terms and the
			// SHOP/MEET auth gate still saw deal != null. This fires only when THIS bot's own reservation lapsed (a
			// newer deal resets pendingDealExpire), so an old timeout cannot clear a fresher deal.
			if (state.dealPlayer != null)
			{
				FakePlayerChatManager.getInstance().clearDeal(state.dealPlayer.getName(), npc.getName());
			}
			state.pendingStoreType = 0;
			state.pendingStock = null;
			state.pendingTitle = null;
			state.pendingDealExpire = 0;
			state.dealPlayer = null;
		}

		// Seated private-store vendors are otherwise stationary (static vendors, or a bot mid-deal whose
		// meet just ended this tick has already cleared its store).
		if ((look != null) && (look.getPrivateStoreType() != 0))
		{
			return;
		}

		// Let the combat AI run uninterrupted; resume wandering shortly after the fight.
		if (npc.isInCombat() || npc.isAttackingNow())
		{
			state.phase = Phase.IDLE;
			state.nextActionTime = now + COMBAT_BACKOFF;
			return;
		}

		if (state.phase == Phase.MOVING)
		{
			if (npc.isMoving())
			{
				return; // still travelling
			}

			// Movement ended. Before treating it as an arrival, confirm the bot actually reached the
			// waypoint. A bare !isMoving() also fires when the move never started or was interrupted (blocked
			// path, geo step at a doorway/stairs), and counting that as "arrived" silently skips the point.
			final Location target = state.moveTarget;
			if ((target != null) && (npc.calculateDistance2D(target) > ARRIVE_DIST))
			{
				// Not there yet: re-issue the move toward the SAME waypoint and let the engine pathfind
				// (around obstacles, up stairs) rather than advancing to the next point.
				if (state.moveAttempts < MAX_MOVE_ATTEMPTS)
				{
					state.moveAttempts++;
					if (state.profile.run)
					{
						npc.setRunning();
					}
					else
					{
						npc.setWalking();
					}
					npc.getAI().setIntention(Intention.MOVE_TO, target);
					return;
				}
				// Repeatedly stuck a short hop away (e.g. a doorway sill geodata won't let an NPC cross):
				// teleport onto the waypoint to recover, exactly as the native WalkingManager does. If it is
				// far/unreachable, fall through and accept arrival so the route keeps progressing.
				if (npc.calculateDistance3D(target) < STUCK_TELEPORT_DIST)
				{
					npc.teleToLocation(target);
				}
			}

			// Arrived (or recovered). Decide how long to pause here before the next goal:
			// - an explicit per-waypoint delay always wins (0 = no stop);
			// - PATROL otherwise defaults to 0 so guards walk their loop continuously;
			// - VISIT/WANDER otherwise use the profile's loiter pause (pauseMin..pauseMax).
			state.phase = Phase.IDLE;
			state.moveTarget = null;
			state.moveAttempts = 0;
			final long pauseMs;
			if (state.pendingArrivalDelay >= 0)
			{
				pauseMs = state.pendingArrivalDelay * 1000L;
			}
			else if (state.profile.type == ProfileType.PATROL)
			{
				pauseMs = 0L;
			}
			else
			{
				pauseMs = Rnd.get(state.profile.pauseMin, state.profile.pauseMax) * 1000L;
			}
			state.pendingArrivalDelay = -1;
			state.nextActionTime = now + pauseMs;
			if (pauseMs > 0)
			{
				return;
			}
			// No pause: fall through and pick the next waypoint this same tick for seamless movement.
		}

		// IDLE: wait out the pause, then pick the next destination.
		if (now < state.nextActionTime)
		{
			return;
		}

		final Location destination = nextDestination(npc, state);
		if (destination == null)
		{
			state.nextActionTime = now + (state.profile.pauseMin * 1000L);
			return;
		}

		if (state.profile.run)
		{
			npc.setRunning();
		}
		else
		{
			npc.setWalking();
		}
		state.moveTarget = destination;
		state.moveAttempts = 0;
		npc.getAI().setIntention(Intention.MOVE_TO, destination);
		state.phase = Phase.MOVING;
	}

	private Location nextDestination(Npc npc, BotState state)
	{
		final Profile profile = state.profile;
		if (profile.type == ProfileType.PATROL)
		{
			if (profile.points.isEmpty())
			{
				return null;
			}
			final int idx = state.patrolIndex % profile.points.size();
			final Location point = profile.points.get(idx);
			state.patrolIndex++;
			if (profile.pointDelays.containsKey(idx))
			{
				state.pendingArrivalDelay = profile.pointDelays.get(idx);
			}
			// Hand the AI the true waypoint and let Creature.moveToLocation pathfind the whole way (this is
			// how the native walking NPCs climb stairs and round corners). Do NOT pre-clamp with
			// getValidLocation: that returns the last point on a straight line, which pins the goal to the
			// bottom of stairs or a doorway and makes the bot stop short.
			return point;
		}

		if (profile.type == ProfileType.VISIT)
		{
			if (profile.points.isEmpty())
			{
				return null;
			}
			// Head to a random point of interest, so movement looks purposeful (and idles long on arrival).
			final int idx = Rnd.get(profile.points.size());
			if (profile.pointDelays.containsKey(idx))
			{
				state.pendingArrivalDelay = profile.pointDelays.get(idx);
			}
			// As above: give the engine the real point and let it pathfind, no straight-line pre-clamp.
			return profile.points.get(idx);
		}

		// WANDER: random reachable point within the bot's radius of the home anchor.
		final int radius = Math.max(100, state.radius);
		for (int attempt = 0; attempt < 5; attempt++)
		{
			final double angle = Rnd.nextDouble() * 2 * Math.PI;
			final int distance = Rnd.get(radius / 4, radius);
			final int x = state.home.getX() + (int) (Math.cos(angle) * distance);
			final int y = state.home.getY() + (int) (Math.sin(angle) * distance);
			final Location candidate = GeoEngine.getInstance().getValidLocation(npc, new Location(x, y, state.home.getZ()));
			// Only accept a spot the bot can walk to in a straight line (no wall between), so they stop
			// picking points behind buildings and running into walls.
			if (npc.isInsideRadius2D(candidate, radius + 100) && GeoEngine.getInstance().canMoveToTarget(npc, candidate))
			{
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Asks a roaming bot to walk to a named meet spot near it (same-town only) and wait there a while.
	 * Driven by the chat AI when the bot agrees in a whisper to "come meet" the player.
	 * @param bot the bot to summon (must be one the behavior FSM controls; vendors/route bots are ignored)
	 * @param spot a keyword like "gatekeeper", "warehouse" or "shop"
	 * @param player who it is going to meet (gets an "im here" whisper on arrival)
	 * @return {@code true} if the bot accepted and a destination was found
	 */
	public boolean requestMeet(Npc bot, String spot, Player player)
	{
		if ((bot == null) || (spot == null))
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		if (state == null)
		{
			return false; // not a behavior-controlled roaming bot
		}
		// FPC-074: do not redirect a meet the caller does not own. If the bot has a pending/active deal for a DIFFERENT
		// player, a stale chat context must not be able to overwrite that player's meet target.
		if (((state.dealPlayer != null) && (state.dealPlayer != player)) || ((state.summonPlayer != null) && (state.summonPlayer != player)))
		{
			return false;
		}
		final Location destination = resolveMeetSpot(bot, spot);
		if (destination == null)
		{
			return false; // no such landmark nearby (different town / unknown spot), or no free spot beside it
		}
		// Make sure it can move again (in case it was pinned waiting at a previous meet spot).
		bot.setImmobilized(false);
		bot.disableCoreAI(false);
		state.summonTarget = destination;
		state.summonSpot = spot;
		state.summonStart = System.currentTimeMillis();
		state.summonBestDistance = bot.calculateDistance3D(destination);
		state.summonLastProgress = state.summonStart;
		state.summonHardExpire = state.summonStart + MEET_HARD_CAP;
		state.pendingDealExpire = 0; // meet underway: the offer reservation no longer lapses
		state.summonArrived = false;
		state.waitingSince = 0;
		state.summonNudged = false;
		state.summonPlayer = player;
		LOGGER.info("FPC_MEET_START bot=" + bot.getName()
			+ " player=" + (player == null ? "" : player.getName())
			+ " spot=" + spot
			+ " x=" + bot.getX() + " y=" + bot.getY() + " z=" + bot.getZ()
			+ " targetX=" + destination.getX() + " targetY=" + destination.getY() + " targetZ=" + destination.getZ());
		return true;
	}

	/**
	 * Records progress toward the meet spot: the bot counts as progressing when it is at least
	 * {@link #SUMMON_PROGRESS_STEP} closer than its best distance so far on this trip.
	 * @return {@code true} if the bot got closer this tick
	 */
	private static boolean madeMeetProgress(Npc npc, BotState state, long now)
	{
		final double distance = npc.calculateDistance3D(state.summonTarget);
		if (distance <= (state.summonBestDistance - SUMMON_PROGRESS_STEP))
		{
			state.summonBestDistance = distance;
			state.summonLastProgress = now;
			return true;
		}
		return false;
	}

	/**
	 * Where this bot stands in a meet with {@code player}, so the brain can answer "where are u" truthfully.
	 * @return {@code "travelling"} while walking to the spot, {@code "waiting"} once there, or {@code ""} when the
	 *         bot has no meet with this player
	 */
	public String getMeetState(Npc bot, Player player)
	{
		if ((bot == null) || (player == null))
		{
			return "";
		}
		final BotState state = _bots.get(bot.getObjectId());
		if ((state == null) || (state.summonTarget == null) || (state.summonPlayer != player))
		{
			return "";
		}
		return state.summonArrived ? "waiting" : "travelling";
	}

	/** @return the agreed meet spot keyword for {@code bot}'s current meet, or {@code ""} when it has none. */
	public String getMeetSpot(Npc bot)
	{
		if (bot == null)
		{
			return "";
		}
		final BotState state = _bots.get(bot.getObjectId());
		return ((state == null) || (state.summonTarget == null) || (state.summonSpot == null)) ? "" : state.summonSpot;
	}

	/**
	 * Player called the meetup off (or the bot decided to stop waiting): drop it and resume normal life.
	 * @return {@code true} if the bot actually had a meetup to cancel
	 */
	public boolean cancelMeet(Npc bot)
	{
		if (bot == null)
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		if ((state == null) || (state.summonTarget == null))
		{
			return false;
		}
		endMeet(bot, state);
		return true;
	}

	/**
	 * FPC-067: authoritatively cancel a deal at ANY stage - a pending offer that has not reached a meet, an active
	 * meet, or an open temporary store - clearing both the behavior reservation and the chat deal context. {@link
	 * #cancelMeet} only handled an active meet ({@code summonTarget != null}), so a pre-meet cancel left the offer
	 * reservation ({@code pendingStoreType}/{@code dealPlayer}/{@code pendingDealExpire}) alive until the offer TTL.
	 * @param bot the bot holding the deal
	 * @param player the player cancelling (its chat context is also cleared, in case a claim race left two entries)
	 * @return {@code true} if the bot had any deal state to cancel
	 */
	public boolean cancelDeal(Npc bot, Player player)
	{
		if (bot == null)
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		if (state == null)
		{
			return false;
		}
		if ((state.summonTarget == null) && (state.pendingStoreType == 0) && (state.dealPlayer == null) && !state.dealActive)
		{
			return false; // nothing to cancel
		}
		// FPC-074: only the deal's owner may tear it down. If the caller does NOT own the bot's real deal (a stale chat
		// context while the behavior deal belongs to another player - reachable through the claim window, FPC-066/078),
		// clear only the caller's own stale ACTIVE_DEALS entry and leave the bot's state untouched.
		if ((player == null) || ((state.dealPlayer != player) && (state.summonPlayer != player)))
		{
			if (player != null)
			{
				FakePlayerChatManager.getInstance().clearDeal(player.getName(), bot.getName());
			}
			return false;
		}
		// Capture the offer's player before endMeet nulls it. endMeet tears down an active meet, a temporary store, and
		// the pending offer fields, and clears the chat context for summonPlayer (null pre-meet), so we clear the
		// offer pair here to cover the pre-meet case.
		final Player offerPlayer = state.dealPlayer;
		endMeet(bot, state);
		if (offerPlayer != null)
		{
			FakePlayerChatManager.getInstance().clearDeal(offerPlayer.getName(), bot.getName());
		}
		return true;
	}

	/**
	 * The player just interacted with a bot that is waiting at a meet spot, so treat them as still
	 * coming: reset the "still coming?" nudge timer and keep waiting.
	 */
	public void noteMeetInteraction(Npc bot, Player player)
	{
		if (bot == null)
		{
			return;
		}
		final BotState state = _bots.get(bot.getObjectId());
		if ((state != null) && state.summonArrived && (state.summonPlayer == player))
		{
			state.waitingSince = System.currentTimeMillis();
			state.summonNudged = false;
		}
	}

	/** Forces nearby clients to rebuild this fake player's visual state. */
	private void refreshFakePlayerVisual(Npc bot)
	{
		World.getInstance().forEachVisibleObjectInRange(bot, Player.class, 2000, player ->
		{
			player.sendPacket(new DeleteObject(bot));
			bot.sendInfo(player);
		});
	}

	/** Clears a meetup (and any deal store it opened) and un-pins the bot so its routine takes back over. */	
	private void endMeet(Npc bot, BotState state)
	{
		// Drop any structured trade context for this pair before we forget who we were dealing with, so a
		// finished/abandoned deal can't leak stale terms into a later whisper to this bot.
		final Player dealPlayer = state.summonPlayer;
		if (dealPlayer != null)
		{
			FakePlayerChatManager.getInstance().clearDeal(dealPlayer.getName(), bot.getName());
		}
		state.summonTarget = null;
		state.summonSpot = null;
		state.summonPlayer = null;
		state.summonArrived = false;
		state.summonNudged = false;
		state.pendingStoreType = 0;
		state.pendingStock = null;
		state.pendingTitle = null;
		state.pendingDealExpire = 0;
		state.dealPlayer = null; // deal ended: forget the offer's player too (FPC-019)
		state.dealClaimExpire = 0; // and release any transient responder claim (FPC-066)
		// Tear down a temporary deal store so the bot becomes a normal roamer again.
		final FakePlayerAppearance look = bot.getFakePlayerAppearance();
		if (state.dealActive && (look != null))
		{
			LOGGER.info("FPC_DEAL_CLOSE bot=" + bot.getName()
			+ " objId=" + bot.getObjectId()
			+ " oldStoreType=" + look.getPrivateStoreType()
			+ " oldSitting=" + look.isSitting()
			+ " oldTitle=\"" + look.getStoreMessage() + "\"");

		look.setStore(0, "");
		look.setStoreItems(null);

		LOGGER.info("FPC_DEAL_CLOSED bot=" + bot.getName()
			+ " objId=" + bot.getObjectId()
			+ " storeType=" + look.getPrivateStoreType()
			+ " sitting=" + look.isSitting());

		refreshFakePlayerVisual(bot); // force client to rebuild fake-player visual without store state
		}
		state.dealActive = false;
		bot.setImmobilized(false);
		bot.disableCoreAI(false);
	}

	/**
	 * Picks a roaming bot near a player to play the counterparty for a trade-chat ad: must be behavior
	 * controlled, not already a vendor / in a meet / holding a deal, and within the same town.
	 * @return a suitable bot, or {@code null} if none is around
	 */
	/** @return {@code true} when this bot is free to take a new trade deal (not meeting, reserved, claimed, or a vendor). */
	private static boolean isTradeResponderFree(BotState state, FakePlayerAppearance look, long now)
	{
		if ((state == null) || (state.summonTarget != null) || (state.pendingStoreType != 0) || state.dealActive || (now <= state.dealClaimExpire))
		{
			return false;
		}
		return (look == null) || (look.getPrivateStoreType() == 0); // an existing vendor is not free
	}

	private List<Npc> freeTradeResponders(Player player)
	{
		final List<Npc> candidates = new ArrayList<>();
		final long now = System.currentTimeMillis();
		World.getInstance().forEachVisibleObjectInRange(player, Npc.class, TRADE_RESPONDER_SEARCH_RANGE, npc ->
		{
			if (npc.isFakePlayer() && isTradeResponderFree(_bots.get(npc.getObjectId()), npc.getFakePlayerAppearance(), now))
			{
				candidates.add(npc);
			}
		});
		return candidates;
	}

	/**
	 * Pick a nearby free bot to SPEAK for a trade (no reservation). Used for a "won't sell here" hint that opens no
	 * deal; a real deal must use {@link #tryClaimTradeResponder} so selection and reservation are atomic (FPC-066).
	 */
	public Npc pickTradeResponder(Player player)
	{
		if (player == null)
		{
			return null;
		}
		final List<Npc> candidates = freeTradeResponders(player);
		return candidates.isEmpty() ? null : candidates.get(Rnd.get(candidates.size()));
	}

	/**
	 * FPC-066: atomically pick AND claim a free nearby bot as this player's trade responder, so two concurrent trade
	 * ads can never select the same bot and create two `ACTIVE_DEALS` entries for one reservation. The claim is a short
	 * hold ({@link #TRADE_CLAIM_TTL}) that {@link #setupDeal} supersedes; if the deal never sets up, it lapses and the
	 * bot frees again. Returns the claimed bot, or {@code null} if none is free.
	 * @param player the advertiser
	 * @return the claimed bot, reserved for this player, or {@code null}
	 */
	/**
	 * FPC-066: release a bare responder claim taken by {@link #tryClaimTradeResponder} when the caller decides not to
	 * set up a deal after all (no stock, offer cap hit), so the bot frees immediately instead of waiting out the claim
	 * TTL. Never touches a bot that has since set up a real deal.
	 * @param bot the claimed bot to release
	 * @param player the player whose claim it should be (only that player's bare claim is released)
	 */
	public void releaseTradeClaim(Npc bot, Player player)
	{
		if (bot == null)
		{
			return;
		}
		final BotState state = _bots.get(bot.getObjectId());
		// FPC-078: only release a bare claim that still belongs to this player, and never one that has since become a
		// real deal (pendingStoreType set) or been re-claimed by someone else.
		if ((state != null) && (state.pendingStoreType == 0) && (state.dealPlayer == player))
		{
			state.dealClaimExpire = 0;
			state.dealPlayer = null;
		}
	}

	public Npc tryClaimTradeResponder(Player player)
	{
		if (player == null)
		{
			return null;
		}
		final List<Npc> candidates = freeTradeResponders(player);
		// Randomize so concurrent ads do not all try the same first candidate, then claim the first one that is still
		// free under the lock (its state may have changed since the scan).
		java.util.Collections.shuffle(candidates);
		synchronized (_tradeClaimLock)
		{
			final long now = System.currentTimeMillis();
			for (Npc npc : candidates)
			{
				final BotState state = _bots.get(npc.getObjectId());
				if (isTradeResponderFree(state, npc.getFakePlayerAppearance(), now))
				{
					state.dealClaimExpire = now + TRADE_CLAIM_TTL;
					state.dealPlayer = player;
					return npc;
				}
			}
		}
		return null;
	}

	/**
	 * Arms a bot with a store to open when it next reaches a meet spot (set up by the trade-ad responder).
	 * @param bot the responder bot
	 * @param storeType a {@code PrivateStoreType} id (SELL or BUY)
	 * @param stock the one-item deal stock
	 * @param title the store sign
	 * @return {@code true} if armed
	 */
	public boolean setupDeal(Npc bot, Player player, int storeType, List<FakePlayerStoreItem> stock, String title)
	{
		if (bot == null)
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		if (state == null)
		{
			return false;
		}
		// FPC-078: only set up a deal for the player who holds the bot's claim/deal. If the bot is already claimed or
		// reserved by a DIFFERENT player, refuse rather than overwrite their reservation.
		if ((state.dealPlayer != null) && (player != null) && (state.dealPlayer != player))
		{
			return false;
		}
		state.pendingStoreType = storeType;
		state.pendingStock = stock;
		state.pendingTitle = title;
		// Remember who the offer is with so an abandoned deal can clear its exact chat context on expiry (FPC-019).
		state.dealPlayer = (storeType != 0) ? player : null;
		state.dealClaimExpire = 0; // FPC-066: the durable pending reservation now supersedes any transient claim
		// Reserve the bot for this offer, but let the reservation lapse if no meet is agreed in time. Cleared
		// the moment a meet actually starts (requestMeet) so a walking/trading bot never lapses mid-deal.
		state.pendingDealExpire = (storeType != 0) ? (System.currentTimeMillis() + OFFER_TTL) : 0;
		return true;
	}

	/**
	 * Replace the stock of a deal that is already pending (e.g. after the player finally states how many they want),
	 * so the shop opened at the meet reflects the renegotiated amount/price. Only updates an existing pending deal;
	 * it never arms a new one, and it leaves the reservation timer untouched.
	 * @return {@code true} if a pending deal existed and was updated
	 */
	public boolean updateDealStock(Npc bot, int storeType, List<FakePlayerStoreItem> stock, String title)
	{
		if ((bot == null) || (stock == null) || stock.isEmpty())
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		if ((state == null) || (state.pendingStoreType == 0))
		{
			return false; // no deal waiting to update
		}
		state.pendingStoreType = storeType;
		state.pendingStock = stock;
		state.pendingTitle = title;
		return true;
	}

	public int getPendingDealCount(Npc bot, int itemId)
	{
		if (bot == null)
		{
			return 0;
		}
		final BotState state = _bots.get(bot.getObjectId());
		if ((state == null) || (state.pendingStock == null))
		{
			return 0;
		}
		for (FakePlayerStoreItem entry : state.pendingStock)
		{
			if ((entry.getItemId() == itemId) && (entry.getCount() > 0))
			{
				return entry.getCount();
			}
		}
		return 0;
	}

	/** @return {@code true} if the bot is parked at a meet spot waiting for the player. */
	public boolean isWaitingAtMeet(Npc bot)
	{
		if (bot == null)
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		return (state != null) && state.summonArrived;
	}

	/**
	 * Opens a deal store on a bot that is already waiting at a meet spot (player asked it to "open shop"
	 * once it arrived). Keeps it pinned and resets the wait so it does not time out mid-trade.
	 */
	public boolean openDealNow(Npc bot, int storeType, List<FakePlayerStoreItem> stock, String title)
	{
		if (bot == null)
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		final FakePlayerAppearance look = bot.getFakePlayerAppearance();
		if ((state == null) || !state.summonArrived || (look == null))
		{
			return false;
		}
		look.setStoreItems(stock);
		look.setStore(storeType, title);
		state.dealActive = true;

		LOGGER.info("FPC_DEAL_OPEN_NOW bot=" + bot.getName()
			+ " objId=" + bot.getObjectId()
			+ " storeType=" + look.getPrivateStoreType()
			+ " sitting=" + look.isSitting()
			+ " title=\"" + look.getStoreMessage() + "\""
			+ " stock=" + look.getStoreItems().size()
			+ " decayed=" + bot.isDecayed()
			+ " moving=" + bot.isMoving()
			+ " immobilized=" + bot.isImmobilized()
			+ " x=" + bot.getX()
			+ " y=" + bot.getY()
			+ " z=" + bot.getZ());
		state.pendingStoreType = 0;
		state.pendingStock = null;
		state.pendingTitle = null;
		state.pendingDealExpire = 0;
		state.waitingSince = System.currentTimeMillis();
		state.summonNudged = false;
		bot.disableCoreAI(true);
		bot.setImmobilized(true);
		refreshFakePlayerVisual(bot); // force client to rebuild fake-player visual with sitting/store state
		return true;
	}

	/**
	 * @return {@code true} if the bot currently holds a TEMPORARY deal store (a trade arranged from chat).
	 *         Unlike a static AFK vendor, a deal bot stays talkable so the player can cancel or renegotiate.
	 */
	public boolean isDealVendor(Npc bot)
	{
		if (bot == null)
		{
			return false;
		}
		final BotState state = _bots.get(bot.getObjectId());
		return (state != null) && state.dealActive;
	}

	/** Resolves a meet-spot keyword to the nearest matching town NPC's location, searching near the bot. */
	private Location resolveMeetSpot(Npc bot, String spot)
	{
		final String s = spot.toLowerCase();
		if (s.contains("gate") || s.equals("gk") || s.contains("teleport"))
		{
			return nearestNpcLocation(bot, Teleporter.class, false);
		}
		if (s.contains("ware") || s.equals("wh") || s.contains("freight"))
		{
			return nearestNpcLocation(bot, Warehouse.class, false);
		}
		if (s.contains("shop") || s.contains("merchant") || s.contains("store") || s.contains("grocer") || s.contains("smith"))
		{
			return nearestNpcLocation(bot, Merchant.class, true); // a plain merchant, not a gatekeeper
		}
		return null;
	}

	/**
	 * @param excludeTeleporters {@code true} to skip Teleporters (they are Merchants too) when looking
	 *            for a plain shop
	 * @return the nearest in-range NPC of the given type, or {@code null}
	 */
	private Location nearestNpcLocation(Npc bot, Class<? extends Npc> type, boolean excludeTeleporters)
	{
		final List<Npc> found = new ArrayList<>();
		World.getInstance().forEachVisibleObjectInRange(bot, type, SUMMON_SEARCH_RANGE, n ->
		{
			if (!excludeTeleporters || !(n instanceof Teleporter))
			{
				found.add(n);
			}
		});
		Npc nearest = null;
		double best = Double.MAX_VALUE;
		for (Npc n : found)
		{
			final double distance = bot.calculateDistance2D(n);
			if (distance < best)
			{
				best = distance;
				nearest = n;
			}
		}
		return nearest == null ? null : nearbyMeetLocation(bot, nearest);
	}

	/**
	 * Picks a standing spot beside {@code landmark}. The spot is only checked locally around the landmark (ground
	 * height, same floor, walkable straight from the NPC); it is never traced from the bot, because the bot may be
	 * across town with buildings in between. Getting there is left to the movement pathfinding.
	 * @return a spot beside the landmark, or {@code null} if none of the sampled spots is usable
	 */
	private Location nearbyMeetLocation(Npc bot, Npc landmark)
	{
		// Prefer standing on the side facing the approaching bot, so the spot visually reads as "next to the NPC"
		// on the bot's way in instead of hidden behind it. The samples still fan out all the way around.
		final GeoEngine geo = GeoEngine.getInstance();
		final double baseAngle = Math.atan2(bot.getY() - landmark.getY(), bot.getX() - landmark.getX());
		for (int attempt = 0; attempt < 12; attempt++)
		{
			final double angle = baseAngle + ((attempt % 2 == 0 ? 1 : -1) * ((attempt + 1) / 2) * (Math.PI / 6));
			final int distance = Rnd.get(MEET_OFFSET_MIN, MEET_OFFSET_MAX);
			final int x = landmark.getX() + (int) (Math.cos(angle) * distance);
			final int y = landmark.getY() + (int) (Math.sin(angle) * distance);
			final int z = geo.getHeight(x, y, landmark.getZ());
			if (Math.abs(z - landmark.getZ()) > MEET_MAX_DZ)
			{
				continue; // another floor (a roof, a terrace, a drop)
			}
			final Location candidate = new Location(x, y, z);
			if (geo.canMoveToTarget(landmark, candidate))
			{
				return candidate;
			}
		}
		return null; // no usable spot beside this landmark: the meet is refused rather than aimed somewhere wrong
	}

	/**
	 * Despawns all managed bots, clears state, reloads FakePlayerBehavior.xml and redeploys.
	 * Safe to call from the admin command handler on the game thread.
	 * @return number of bots that were removed before redeployment.
	 */
	public int reload()
	{
		// Despawn every bot we own.
		int removed = 0;
		for (int objectId : new java.util.ArrayList<>(_bots.keySet()))
		{
			final org.l2jmobius.gameserver.model.WorldObject obj = World.getInstance().findObject(objectId);
			if (obj instanceof Npc)
			{
				((Npc) obj).deleteMe();
				removed++;
			}
		}
		_bots.clear();
		load(); // bumps the generation (stale delayed work no-ops) and redeploys; the recurring loops keep running.
		return removed;
	}

	public static FakePlayerBehaviorManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final FakePlayerBehaviorManager INSTANCE = new FakePlayerBehaviorManager();
	}
}
