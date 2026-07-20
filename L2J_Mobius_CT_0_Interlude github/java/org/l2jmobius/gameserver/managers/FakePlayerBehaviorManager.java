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
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerCraftItem;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerStoreItem;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
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
	// Meet beside a landmark NPC instead of on top of it, so the fake player does not overlap the gatekeeper,
	// warehouse keeper or merchant model.
	private static final int MEET_OFFSET_MIN = 160;
	private static final int MEET_OFFSET_MAX = 260;
	// Travel time before giving up (e.g. no path) instead of wall-banging.
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
		VISIT,
		/** Hunts: heads toward the nearest monster in the zone; if none is near, roams elsewhere within
		 * the zone to find some. Spreads bots out across a hunting ground instead of clustering. */
		FARM
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
		long summonStart; // travel start (for the give-up timer)
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
	private boolean _started = false;
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
				profile.type = Enum.valueOf(ProfileType.class, set.getString("type", "WANDER").toUpperCase());
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
				population.storeType = set.contains("store") ? set.getString("store") : null;
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

	private void start()
	{
		if (_started || _profiles.isEmpty())
		{
			return;
		}
		_started = true;
		if (!_populations.isEmpty() || (FakePlayersConfig.FAKE_PLAYER_DEPLOY_COUNT > 0))
		{
			ThreadPool.schedule(this::deploy, DEPLOY_DELAY);
		}
		ThreadPool.scheduleAtFixedRate(this::discover, DISCOVERY_INTERVAL, DISCOVERY_INTERVAL);
		ThreadPool.scheduleAtFixedRate(this::tick, BEHAVIOR_INTERVAL, BEHAVIOR_INTERVAL);
		LOGGER.info(getClass().getSimpleName() + ": Fake player behavior enabled.");
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
	 * Finds the closest living monster within range that is still inside the bot's zone.
	 * @param npc the hunting bot
	 * @param state its behavior state (for the zone anchor/radius)
	 * @param range how far to look
	 * @return the nearest in-zone monster, or {@code null} if none
	 */
	private Monster nearestMonster(Npc npc, BotState state, int range)
	{
		final List<Monster> found = new ArrayList<>();
		World.getInstance().forEachVisibleObjectInRange(npc, Monster.class, range, monster ->
		{
			if (!monster.isDead() && monster.isInsideRadius2D(state.home, state.radius + 400))
			{
				found.add(monster);
			}
		});
		Monster nearest = null;
		double best = Double.MAX_VALUE;
		for (Monster monster : found)
		{
			final double distance = npc.calculateDistance2D(monster);
			if (distance < best)
			{
				best = distance;
				nearest = monster;
			}
		}
		return nearest;
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
				// Replace fallen field bots with a fresh hunter so the zone stays populated.
				if ((dead != null) && (dead.population != null) && dead.population.respawn)
				{
					ThreadPool.schedule(() -> deployOne(dead.population), RESPAWN_DELAY);
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
				return; // let it fight; it resumes heading over afterwards
			}
			else if (npc.calculateDistance2D(state.summonTarget) <= SUMMON_ARRIVE_DIST)
			{
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
				if (who != null)
				{
					final String line = state.dealActive ? (Rnd.nextBoolean() ? "im here, check my store" : "here, open my shop") : (Rnd.nextBoolean() ? "im here" : "here, where are u");
					FakePlayerChatManager.getInstance().sendChat(who, npc.getName(), line);
				}
				return;
			}
			else if ((now - state.summonStart) > SUMMON_GIVEUP)
			{
				// Couldn't reach the spot (likely no path); stop trying and say so.
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
			state.pendingStoreType = 0;
			state.pendingStock = null;
			state.pendingTitle = null;
			state.pendingDealExpire = 0;
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
			// - VISIT/WANDER/FARM otherwise use the profile's loiter pause (pauseMin..pauseMax).
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

		if (profile.type == ProfileType.FARM)
		{
			// Head toward the nearest live monster inside the zone; if there is none, fall through to a
			// roam so the bot relocates and looks for mobs elsewhere within the zone.
			final int searchRange = Math.min(2200, Math.max(800, state.radius));
			final Monster target = nearestMonster(npc, state, searchRange);
			if (target != null)
			{
				return GeoEngine.getInstance().getValidLocation(npc, new Location(target.getX(), target.getY(), target.getZ()));
			}
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
		final Location destination = resolveMeetSpot(bot, spot);
		if (destination == null)
		{
			return false; // no such landmark nearby (different town / unknown spot)
		}
		// Make sure it can move again (in case it was pinned waiting at a previous meet spot).
		bot.setImmobilized(false);
		bot.disableCoreAI(false);
		state.summonTarget = destination;
		state.summonStart = System.currentTimeMillis();
		state.summonHardExpire = state.summonStart + MEET_HARD_CAP;
		state.pendingDealExpire = 0; // meet underway: the offer reservation no longer lapses
		state.summonArrived = false;
		state.waitingSince = 0;
		state.summonNudged = false;
		state.summonPlayer = player;
		return true;
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
		state.summonPlayer = null;
		state.summonArrived = false;
		state.summonNudged = false;
		state.pendingStoreType = 0;
		state.pendingStock = null;
		state.pendingTitle = null;
		state.pendingDealExpire = 0;
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
	public Npc pickTradeResponder(Player player)
	{
		if (player == null)
		{
			return null;
		}
		final List<Npc> candidates = new ArrayList<>();
		World.getInstance().forEachVisibleObjectInRange(player, Npc.class, TRADE_RESPONDER_SEARCH_RANGE, npc ->
		{
			if (!npc.isFakePlayer())
			{
				return;
			}
			final BotState state = _bots.get(npc.getObjectId());
			final FakePlayerAppearance look = npc.getFakePlayerAppearance();
			if ((state == null) || (state.summonTarget != null) || (state.pendingStoreType != 0) || state.dealActive)
			{
				return;
			}
			if ((look != null) && (look.getPrivateStoreType() != 0))
			{
				return; // already a vendor
			}
			candidates.add(npc);
		});
		return candidates.isEmpty() ? null : candidates.get(Rnd.get(candidates.size()));
	}

	/**
	 * Arms a bot with a store to open when it next reaches a meet spot (set up by the trade-ad responder).
	 * @param bot the responder bot
	 * @param storeType a {@code PrivateStoreType} id (SELL or BUY)
	 * @param stock the one-item deal stock
	 * @param title the store sign
	 * @return {@code true} if armed
	 */
	public boolean setupDeal(Npc bot, int storeType, List<FakePlayerStoreItem> stock, String title)
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
		state.pendingStoreType = storeType;
		state.pendingStock = stock;
		state.pendingTitle = title;
		// Reserve the bot for this offer, but let the reservation lapse if no meet is agreed in time. Cleared
		// the moment a meet actually starts (requestMeet) so a walking/trading bot never lapses mid-deal.
		state.pendingDealExpire = (storeType != 0) ? (System.currentTimeMillis() + OFFER_TTL) : 0;
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

	private Location nearbyMeetLocation(Npc bot, Npc landmark)
	{
		// Prefer standing on the side facing the approaching bot, so the destination is usually reachable
		// and visually reads as "next to the NPC" instead of hidden behind it.
		final double baseAngle = Math.atan2(bot.getY() - landmark.getY(), bot.getX() - landmark.getX());
		for (int attempt = 0; attempt < 12; attempt++)
		{
			final double angle = baseAngle + ((attempt % 2 == 0 ? 1 : -1) * ((attempt + 1) / 2) * (Math.PI / 6));
			final int distance = Rnd.get(MEET_OFFSET_MIN, MEET_OFFSET_MAX);
			final int x = landmark.getX() + (int) (Math.cos(angle) * distance);
			final int y = landmark.getY() + (int) (Math.sin(angle) * distance);
			final Location candidate = GeoEngine.getInstance().getValidLocation(bot, new Location(x, y, landmark.getZ()));

			// Keep the chosen spot close to the landmark, but avoid picking the exact landmark tile again.
			final int dx = candidate.getX() - landmark.getX();
			final int dy = candidate.getY() - landmark.getY();
			final double distanceFromLandmark = Math.sqrt((dx * dx) + (dy * dy));
			if ((distanceFromLandmark >= MEET_OFFSET_MIN / 2) && GeoEngine.getInstance().canMoveToTarget(bot, candidate))
			{
				return candidate;
			}
		}

		// Fallback: still offset from the NPC even if geodata rejects every sampled point.
		final int x = landmark.getX() + MEET_OFFSET_MIN;
		final int y = landmark.getY();
		return GeoEngine.getInstance().getValidLocation(bot, new Location(x, y, landmark.getZ()));
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
		_started = false;
		load();
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
