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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.PhantomOlympiadConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.groups.PartyDistributionType;
import org.l2jmobius.gameserver.model.groups.PartyMessageType;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.olympiad.PhantomOlympiadBridge;
import org.l2jmobius.gameserver.model.spawns.Spawn;

/**
 * Phantom nobles in the Grand Olympiad. A fixed roster of phantom nobles lives on its own account (see
 * {@link PhantomManager#ACCOUNT_NAME_NOBLE}), so their Olympiad points, record and Hero status survive restarts. While
 * the competition is open they log in near the Olympiad Managers and sign up through the stock registration, so a
 * solo player's pools fill and phantom-only matches run for anyone watching. The stock Olympiad runs every match
 * unchanged; {@link PhantomManager#serviceOlympian} drives each noble's side of the fight.
 * <p>
 * It also keeps a real player's party together across a match: stock Interlude removes a player from their party
 * when the match starts, and {@link #holdsPartyFor} tells the party and buddy managers to wait instead of releasing
 * the player's phantoms, then {@link #rejoinParty} puts them back.
 */
public class PhantomOlympiadManager
{
	private static final Logger LOGGER = Logger.getLogger(PhantomOlympiadManager.class.getName());

	// Olympiad Manager NPC; the roster gathers near its spawns.
	private static final int OLYMPIAD_MANAGER_NPC_ID = 31688;
	// Fast tick: finalizes the stock teleports, drives match fights and walks idle nobles.
	private static final long TICK_MS = 1000;
	// Sign-up pass cadence (the stock matcher itself runs every 30 seconds).
	private static final long SIGN_UP_PASS_MS = 30000;
	// Boot delay before the first tick, so the Olympiad and the datapack spawns are loaded.
	private static final long START_DELAY_MS = 60000;
	// Nobles gather this far from an Olympiad Manager.
	private static final int GATHER_MIN_RADIUS = 120;
	private static final int GATHER_MAX_RADIUS = 450;
	// Chance a noble logs in at the manager nearest a real player rather than a random one.
	private static final int NEAR_PLAYER_GATHER_PERCENT = 50;
	// How long after a match the party hold lasts, so the stock return teleport lands before the rejoin.
	private static final long PARTY_REJOIN_DELAY_MS = 3000;

	// Roster: charId -> class id, loaded once from the database, extended as nobles are created.
	private final Map<Integer, Integer> _roster = new ConcurrentHashMap<>();
	private volatile boolean _rosterLoaded = false;
	// Nobles resting after a match: objectId -> earliest time they may sign up again.
	private final Map<Integer, Long> _restUntil = new ConcurrentHashMap<>();
	// Real players seen in a match: objectId -> 0 while in the match, or the time they left it.
	private final Map<Integer, Long> _ownerMatchState = new ConcurrentHashMap<>();
	private long _nextSignUpPass = 0;
	private boolean _started = false;

	protected PhantomOlympiadManager()
	{
	}

	/** Starts the tick once. Called from {@link PhantomManager#load()}. */
	public synchronized void start()
	{
		if (_started)
		{
			return;
		}
		_started = true;
		ThreadPool.scheduleAtFixedRate(this::tick, START_DELAY_MS, TICK_MS);
	}

	/** @return {@code true} while the feature is switched on and the stock Olympiad is enabled. */
	public static boolean enabled()
	{
		return PhantomOlympiadConfig.PHANTOM_OLYMPIAD_ENABLED && OlympiadConfig.OLYMPIAD_ENABLED;
	}

	private void tick()
	{
		final long now = System.currentTimeMillis();
		try
		{
			trackOwners(now);
			final PhantomManager phantoms = PhantomManager.getInstance();
			for (Player noble : phantoms.onlineOlympians())
			{
				phantoms.serviceOlympian(noble, now);
			}
			final Olympiad olympiad = Olympiad.getInstance();
			final boolean open = enabled() && olympiad.inCompPeriod() && !olympiad.isOlympiadEnd();
			if (!open)
			{
				logOutIdle(!enabled());
				return;
			}
			if (!_rosterLoaded)
			{
				// Support-class and summoner nobles made before they were left out keep their rows but are never logged in.
				phantoms.loadOlympiadRoster().forEach((charId, classId) ->
				{
					if (PhantomOlympiadRules.isRosterClass(classId))
					{
						_roster.put(charId, classId);
					}
				});
				_rosterLoaded = true;
			}
			// At most one creation and one login per tick, so a big roster spreads its database and gearing cost.
			if (!createOne())
			{
				logInOne();
			}
			if (now >= _nextSignUpPass)
			{
				_nextSignUpPass = now + SIGN_UP_PASS_MS;
				signUpPass(now);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": tick error: " + e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// Roster
	// ---------------------------------------------------------------------

	/** Creates one missing noble: first rivals for a waiting real player's class, then the base roster. */
	private boolean createOne()
	{
		final int[] counts = new int[PhantomOlympiadRules.THIRD_CLASS_COUNT];
		for (int classId : _roster.values())
		{
			if (PhantomOlympiadRules.isThirdClass(classId))
			{
				counts[classId - PhantomOlympiadRules.FIRST_THIRD_CLASS_ID]++;
			}
		}
		int classId = 0;
		for (Player player : waitingRealPlayers(PhantomOlympiadBridge.pools()))
		{
			final int playerClass = player.getBaseClass();
			if (PhantomOlympiadRules.isRosterClass(playerClass) && (PhantomOlympiadRules.missing(counts[playerClass - PhantomOlympiadRules.FIRST_THIRD_CLASS_ID], PhantomOlympiadConfig.PHANTOM_OLYMPIAD_RIVALS_PER_CLASS) > 0))
			{
				classId = playerClass;
				break;
			}
		}
		if (classId == 0)
		{
			classId = PhantomOlympiadRules.nextBaseClass(counts, PhantomOlympiadConfig.PHANTOM_OLYMPIAD_ROSTER_SIZE);
		}
		if (classId == 0)
		{
			return false;
		}
		final int level = PhantomOlympiadRules.rollLevel(PhantomOlympiadConfig.PHANTOM_OLYMPIAD_MIN_LEVEL, PhantomOlympiadConfig.PHANTOM_OLYMPIAD_MAX_LEVEL);
		final Location location = gatherLocation();
		if (location == null)
		{
			return false;
		}
		final Player noble = PhantomManager.getInstance().createOlympiadNoble(classId, level, location);
		if (noble == null)
		{
			return false;
		}
		_roster.put(noble.getObjectId(), classId);
		LOGGER.info(getClass().getSimpleName() + ": Created phantom noble '" + noble.getName() + "' (class " + classId + ", level " + level + "). Roster: " + _roster.size() + ".");
		return true;
	}

	/** Logs in one roster noble that is not online yet. */
	private void logInOne()
	{
		final PhantomManager phantoms = PhantomManager.getInstance();
		for (Integer charId : _roster.keySet())
		{
			if ((World.getInstance().getPlayer(charId) != null) || phantoms.isOlympianLoggedIn(charId))
			{
				continue; // online, or logged in but decayed mid-teleport
			}
			final Location location = gatherLocation();
			if (location == null)
			{
				return;
			}
			if (phantoms.spawnOlympiadNoble(charId, location) == null)
			{
				// The row is gone (deleted by hand) or failed to load: drop it, the roster tops itself back up.
				if (!phantoms.olympiadNobleExists(charId))
				{
					_roster.remove(charId);
				}
			}
			return;
		}
	}

	/**
	 * Outside the competition (or with the feature off): logs out every noble that is not in a match. With the feature
	 * off, waiting nobles are taken off the waiting lists first.
	 */
	private void logOutIdle(boolean disabled)
	{
		final PhantomManager phantoms = PhantomManager.getInstance();
		final List<Player> online = phantoms.onlineOlympians();
		if (online.isEmpty())
		{
			return;
		}
		final Olympiad olympiad = Olympiad.getInstance();
		final PhantomOlympiadBridge.Pools pools = PhantomOlympiadBridge.pools();
		for (Player noble : online)
		{
			if (noble.isInOlympiadMode() || isInGame(olympiad, noble))
			{
				continue; // finish the match first
			}
			if (pools.nonClassed.contains(noble.getObjectId()) || (pools.classedPoolOf(noble.getObjectId()) >= 0))
			{
				if (!disabled)
				{
					continue; // the stock competition end clears the lists itself
				}
				PhantomOlympiadBridge.unregister(noble);
			}
			phantoms.despawnOlympian(noble);
		}
	}

	/** @return a spot near an Olympiad Manager: half the time the one nearest a real player, else a random one. */
	private Location gatherLocation()
	{
		final List<Spawn> managers = new ArrayList<>(SpawnTable.getInstance().getSpawns(OLYMPIAD_MANAGER_NPC_ID));
		if (managers.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": No Olympiad Manager (" + OLYMPIAD_MANAGER_NPC_ID + ") spawns found; phantom nobles cannot gather.");
			return null;
		}
		Spawn manager = managers.get(Rnd.get(managers.size()));
		final Player player = anyRealPlayer();
		if ((player != null) && (Rnd.get(100) < NEAR_PLAYER_GATHER_PERCENT))
		{
			double best = Double.MAX_VALUE;
			for (Spawn spawn : managers)
			{
				final double distance = player.calculateDistance2D(spawn);
				if (distance < best)
				{
					best = distance;
					manager = spawn;
				}
			}
		}
		final double angle = Rnd.nextDouble() * 2 * Math.PI;
		final int radius = Rnd.get(GATHER_MIN_RADIUS, GATHER_MAX_RADIUS);
		final int x = manager.getX() + (int) (Math.cos(angle) * radius);
		final int y = manager.getY() + (int) (Math.sin(angle) * radius);
		return GeoEngine.getInstance().getValidLocation(manager.getX(), manager.getY(), manager.getZ(), x, y, manager.getZ(), 0);
	}

	// ---------------------------------------------------------------------
	// Sign-up
	// ---------------------------------------------------------------------

	/**
	 * One sign-up pass. A real player waiting in a pool makes free nobles fill that pool (their own class for a
	 * classed pool); otherwise free nobles sign up for non-classed matches on the background chance. Nobles left in a
	 * classed pool nobody real is waiting in are taken back off it, so they don't sit out the evening.
	 */
	private void signUpPass(long now)
	{
		final PhantomManager phantoms = PhantomManager.getInstance();
		final Olympiad olympiad = Olympiad.getInstance();
		final PhantomOlympiadBridge.Pools pools = PhantomOlympiadBridge.pools();

		// How many real players wait in each pool, and how many roster nobles already wait in each pool.
		int realNonClassed = 0;
		final Map<Integer, Integer> realClassed = new HashMap<>();
		for (Player player : waitingRealPlayers(pools))
		{
			if (pools.nonClassed.contains(player.getObjectId()))
			{
				realNonClassed++;
			}
			final int classId = pools.classedPoolOf(player.getObjectId());
			if (classId >= 0)
			{
				realClassed.merge(classId, 1, Integer::sum);
			}
		}
		int nonClassedWaiting = 0;
		for (int objectId : pools.nonClassed)
		{
			if (_roster.containsKey(objectId))
			{
				nonClassedWaiting++;
			}
		}

		final List<Player> nobles = new ArrayList<>(phantoms.onlineOlympians());
		Collections.shuffle(nobles);
		// Per class a real player waits in: why its nobles did or did not join, for the diagnostic line below.
		final Map<Integer, int[]> why = new HashMap<>(); // online, joined, moved from open queue, resting, in match, low points, dead
		for (Player noble : nobles)
		{
			final int objectId = noble.getObjectId();
			final int classId = noble.getBaseClass();
			final int[] tally = realClassed.containsKey(classId) ? why.computeIfAbsent(classId, k -> new int[7]) : null;
			if (tally != null)
			{
				tally[0]++;
			}
			final int waitingIn = pools.classedPoolOf(objectId);
			if ((waitingIn >= 0) && !realClassed.containsKey(waitingIn))
			{
				PhantomOlympiadBridge.unregister(noble); // nobody real to fight in that pool any more
				continue;
			}
			final boolean inMatch = noble.isInOlympiadMode() || isInGame(olympiad, noble);
			// A noble of a real player's class waiting in the open queue moves to the player's class pool, which the
			// player is waiting on; otherwise every noble of that class could sit in the open queue (FPC-130).
			if ((tally != null) && !inMatch && pools.nonClassed.contains(objectId) && PhantomOlympiadBridge.unregister(noble))
			{
				pools.nonClassed.remove(Integer.valueOf(objectId));
				nonClassedWaiting--;
				tally[2]++;
			}
			final boolean registered = pools.nonClassed.contains(objectId) || (waitingIn >= 0);
			if (!PhantomOlympiadRules.isFree(true, registered, inMatch, noble.isDead(), now, _restUntil.getOrDefault(objectId, 0L)))
			{
				if (tally != null)
				{
					if (inMatch)
					{
						tally[4]++;
					}
					else if (noble.isDead())
					{
						tally[6]++;
					}
					else if (!registered)
					{
						tally[3]++;
					}
				}
				continue;
			}
			// A new noble has no record yet; stock gives it the starting points on its first sign-up.
			final int points = olympiad.getNoblePoints(objectId);
			final int effectivePoints = (points > 0) ? points : OlympiadConfig.OLYMPIAD_START_POINTS;
			if ((tally != null) && !PhantomOlympiadRules.hasPointsFor(true, effectivePoints))
			{
				tally[5]++;
			}
			if (realClassed.containsKey(classId) && PhantomOlympiadRules.hasPointsFor(true, effectivePoints))
			{
				final List<Integer> pool = pools.classed.get(classId);
				final int waiting = countRoster(pool);
				if (PhantomOlympiadRules.shouldSignUp(true, waiting, PhantomOlympiadRules.poolCap(OlympiadConfig.OLYMPIAD_CLASSED, realClassed.get(classId)), 0, 0) && PhantomOlympiadBridge.register(noble, true))
				{
					pools.classed.computeIfAbsent(classId, k -> new ArrayList<>()).add(objectId);
					tally[1]++;
					continue;
				}
			}
			if (PhantomOlympiadRules.hasPointsFor(false, effectivePoints))
			{
				final int cap = PhantomOlympiadRules.poolCap(OlympiadConfig.OLYMPIAD_NONCLASSED, realNonClassed);
				if (PhantomOlympiadRules.shouldSignUp(realNonClassed > 0, nonClassedWaiting, cap, PhantomOlympiadConfig.PHANTOM_OLYMPIAD_SIGN_UP_CHANCE_PERCENT, Rnd.get(100)) && PhantomOlympiadBridge.register(noble, false))
				{
					pools.nonClassed.add(objectId);
					nonClassedWaiting++;
				}
			}
		}
		// A real player waiting in a class pool that is still short: say how that class's nobles stand (FPC-130).
		for (Map.Entry<Integer, Integer> entry : realClassed.entrySet())
		{
			final int classId = entry.getKey();
			final List<Integer> pool = pools.classed.get(classId);
			final int size = (pool == null) ? 0 : pool.size();
			if (size < OlympiadConfig.OLYMPIAD_CLASSED)
			{
				final int[] tally = why.getOrDefault(classId, new int[7]);
				LOGGER.info(getClass().getSimpleName() + ": Class " + classId + " pool has " + size + "/" + OlympiadConfig.OLYMPIAD_CLASSED + " (" + entry.getValue() + " real). Nobles of the class: roster " + countRosterClass(classId) + ", online " + tally[0] + ", joined " + tally[1] + ", moved from open queue " + tally[2] + ", resting " + tally[3] + ", in match " + tally[4] + ", low points " + tally[5] + ", dead " + tally[6] + ".");
			}
		}
	}

	/** Called by {@link PhantomManager} when a noble's match is over, so it rests before signing up again. */
	void onMatchEnded(Player noble, long now)
	{
		_restUntil.put(noble.getObjectId(), now + (Math.max(0, PhantomOlympiadConfig.PHANTOM_OLYMPIAD_REST_SECONDS) * 1000L));
	}

	private int countRosterClass(int classId)
	{
		int count = 0;
		for (int rosterClass : _roster.values())
		{
			if (rosterClass == classId)
			{
				count++;
			}
		}
		return count;
	}

	private int countRoster(List<Integer> pool)
	{
		int count = 0;
		if (pool != null)
		{
			for (int objectId : pool)
			{
				if (_roster.containsKey(objectId))
				{
					count++;
				}
			}
		}
		return count;
	}

	private static boolean isInGame(Olympiad olympiad, Player noble)
	{
		return olympiad.isRegisteredInComp(noble) && !olympiad.isRegistered(noble);
	}

	/** @return online real (non-phantom) players on either waiting list of the given snapshot */
	private static List<Player> waitingRealPlayers(PhantomOlympiadBridge.Pools pools)
	{
		final List<Integer> ids = new ArrayList<>(pools.nonClassed);
		for (List<Integer> pool : pools.classed.values())
		{
			ids.addAll(pool);
		}
		final List<Player> players = new ArrayList<>();
		final PhantomManager phantoms = PhantomManager.getInstance();
		for (int objectId : ids)
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if ((player != null) && player.isOnline() && !phantoms.isPhantom(player) && !players.contains(player))
			{
				players.add(player);
			}
		}
		return players;
	}

	private static Player anyRealPlayer()
	{
		final PhantomManager phantoms = PhantomManager.getInstance();
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline() && !phantoms.isPhantom(player))
			{
				return player;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------------
	// Keeping a real player's party together across a match
	// ---------------------------------------------------------------------

	/** Notes when each real player enters and leaves a match, for {@link #holdsPartyFor}. */
	private void trackOwners(long now)
	{
		final PhantomManager phantoms = PhantomManager.getInstance();
		for (Player player : World.getInstance().getPlayers())
		{
			if (phantoms.isPhantom(player) || !ownerInMatch(player))
			{
				continue;
			}
			_ownerMatchState.put(player.getObjectId(), 0L);
		}
		for (Map.Entry<Integer, Long> entry : _ownerMatchState.entrySet())
		{
			final Player owner = World.getInstance().getPlayer(entry.getKey());
			if ((owner == null) || !owner.isOnline())
			{
				_ownerMatchState.remove(entry.getKey());
			}
			else if (!ownerInMatch(owner))
			{
				if (entry.getValue() == 0)
				{
					entry.setValue(now); // just left the match
				}
				else if ((now - entry.getValue()) >= PARTY_REJOIN_DELAY_MS)
				{
					_ownerMatchState.remove(entry.getKey());
				}
			}
		}
	}

	/**
	 * @return {@code true} from the moment the stock matcher puts the player in a game until the game is cleared. The
	 *         game holds the player's id before the stock flow removes the party ({@code removals}) and until after it
	 *         turns Olympiad mode off, so this covers the whole match, whatever order the stock steps run in.
	 */
	private static boolean ownerInMatch(Player owner)
	{
		return owner.isInOlympiadMode() || isInGame(Olympiad.getInstance(), owner);
	}

	/**
	 * @return {@code true} while the owner is in a match, or just back from one, and the rejoin option is on. A party
	 *         member or buddy of this owner then waits where it is instead of being released for losing its party.
	 */
	public boolean holdsPartyFor(Player owner)
	{
		if (!PhantomOlympiadConfig.PHANTOM_OLYMPIAD_REJOIN_PARTY || (owner == null) || !owner.isOnline())
		{
			return false;
		}
		if (ownerInMatch(owner))
		{
			return true; // read live, so the hold starts the moment the stock matcher picks the owner
		}
		final Long state = _ownerMatchState.get(owner.getObjectId());
		if (state == null)
		{
			return false; // no recent match
		}
		final long now = System.currentTimeMillis();
		long leftAt = state;
		if (leftAt == 0)
		{
			// Left the match since the last tick noticed: start the rejoin delay now.
			leftAt = now;
			_ownerMatchState.put(owner.getObjectId(), now);
		}
		return PhantomOlympiadRules.holdsParty(false, leftAt, now, PARTY_REJOIN_DELAY_MS);
	}

	/**
	 * Puts a held party member back in its owner's party after the match: leaves whatever party it was left in, then
	 * joins the owner's (creating it with the owner as leader if the owner is solo now).
	 * @return {@code true} if the member is partied with the owner afterwards
	 */
	public static boolean rejoinParty(Player owner, Player member)
	{
		if ((owner == null) || (member == null) || !owner.isOnline())
		{
			return false;
		}
		try
		{
			if (member.isInParty() && owner.isInParty() && (member.getParty() == owner.getParty()))
			{
				return true;
			}
			if (member.isInParty())
			{
				member.getParty().removePartyMember(member, PartyMessageType.LEFT);
			}
			if (!owner.isInParty())
			{
				final PartyDistributionType type = (owner.getPartyDistributionType() != null) ? owner.getPartyDistributionType() : PartyDistributionType.FINDERS_KEEPERS;
				owner.setParty(new Party(owner, type));
			}
			if (!TownFakeInviteRules.mayAddMember(true, owner.getParty().isLeader(owner), owner.getParty().getMemberCount()))
			{
				return false;
			}
			member.joinParty(owner.getParty());
			return member.isInParty() && (member.getParty() == owner.getParty());
		}
		catch (Exception e)
		{
			LOGGER.warning(PhantomOlympiadManager.class.getSimpleName() + ": Failed to rejoin " + member.getName() + " to " + owner.getName() + "'s party: " + e.getMessage());
			return false;
		}
	}

	public static PhantomOlympiadManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final PhantomOlympiadManager INSTANCE = new PhantomOlympiadManager();
	}
}
