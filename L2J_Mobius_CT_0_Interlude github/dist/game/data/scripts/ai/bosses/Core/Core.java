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
package ai.bosses.Core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Core grand boss AI handler.<br>
 * Manages Core lifecycle, minion spawning and Cruma teleport shortcuts.
 * <ul>
 * <li>Restores Core state from database and schedules respawn.</li>
 * <li>Spawns and respawns minions with per-position tracking.</li>
 * <li>Controls Core voice lines and teleport cubes on death.</li>
 * </ul>
 * @author BazookaRpm
 */
public class Core extends Script
{
	// Logging.
	private static final Logger LOGGER = Logger.getLogger(Core.class.getName());
	
	// NPC identifiers.
	private static final int CORE_NPC_ID = 29006;
	private static final int MINION_DEATH_KNIGHT_ID = 29007;
	private static final int MINION_DOOM_WRAITH_ID = 29008;
	private static final int MINION_SUSCEPTOR_ID = 29011;
	private static final int TELEPORT_CUBE_NPC_ID = 899099;
	
	// Boss status flags.
	private static final byte STATUS_ALIVE = 0;
	private static final byte STATUS_DEAD = 1;
	
	// Event identifiers.
	private static final String EVENT_CORE_UNLOCK = "core_unlock";
	private static final String EVENT_SPAWN_MINION_LEGACY = "spawn_minion";
	private static final String EVENT_DESPAWN_MINIONS = "despawn_minions";
	private static final String EVENT_TELEPORT_PREFIX = "TELEPORT";
	private static final String EVENT_SPAWN_MINION_PREFIX = "spawn_minion_";
	
	// Timing constants.
	private static final long MILLIS_PER_HOUR = 3600000L;
	private static final int MINION_RESPAWN_DELAY_MS = 60000;
	private static final int MINION_DESPAWN_DELAY_MS = 20000;
	private static final int TELEPORT_CUBE_DESPAWN_MS = 900000;
	
	// Teleport destinations.
	private static final Location LOCATION_CRUMA_ENTRANCE = new Location(17253, 114232, -3440); // Cruma Tower entrance.
	private static final Location LOCATION_CRUMA_CORE_FLOOR3 = new Location(17719, 115590, -6584); // Cruma Core (3rd floor).
	
	// Core spawn position.
	private static final Location CORE_DEFAULT_SPAWN = new Location(17726, 108915, -6480);
	
	// Minion spawn templates.
	private static final Map<Integer, List<Location>> MINION_SPAWN_POINTS = new HashMap<>();
	static
	{
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(17191, 109298, -6488));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(17564, 109548, -6488));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(17855, 109552, -6488));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(18280, 109202, -6488));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(18784, 109253, -6488));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(18059, 108314, -6488));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(17300, 108444, -6488));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(17148, 110071, -6648));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(18318, 110077, -6648));
		addMinionSpawn(MINION_DEATH_KNIGHT_ID, new Location(17726, 110391, -6648));
		
		addMinionSpawn(MINION_DOOM_WRAITH_ID, new Location(17113, 110970, -6648));
		addMinionSpawn(MINION_DOOM_WRAITH_ID, new Location(17496, 110880, -6648));
		addMinionSpawn(MINION_DOOM_WRAITH_ID, new Location(18061, 110990, -6648));
		addMinionSpawn(MINION_DOOM_WRAITH_ID, new Location(18384, 110698, -6648));
		addMinionSpawn(MINION_DOOM_WRAITH_ID, new Location(17993, 111458, -6584));
		
		addMinionSpawn(MINION_SUSCEPTOR_ID, new Location(17297, 111470, -6584));
		addMinionSpawn(MINION_SUSCEPTOR_ID, new Location(17893, 110198, -6648));
		addMinionSpawn(MINION_SUSCEPTOR_ID, new Location(17706, 109423, -6488));
		addMinionSpawn(MINION_SUSCEPTOR_ID, new Location(17849, 109388, -6480));
	}
	
	// Runtime minion tracking.
	private static final Collection<Attackable> _minions = ConcurrentHashMap.newKeySet();
	private static final Map<Integer, MinionRespawnData> _pendingRespawns = new ConcurrentHashMap<>();
	private static final AtomicInteger _respawnIdGenerator = new AtomicInteger(0);
	
	// Attack state.
	private static boolean _firstAttacked;
	
	/**
	 * Holds respawn data for a minion instance.
	 */
	private static final class MinionRespawnData
	{
		final int _npcId;
		final Location _location;
		
		MinionRespawnData(int npcId, Location location)
		{
			_npcId = npcId;
			_location = location;
		}
	}
	
	/**
	 * Registers a minion spawn position for the given NPC identifier.
	 * @param npcId The minion NPC identifier.
	 * @param location The spawn location.
	 */
	private static void addMinionSpawn(int npcId, Location location)
	{
		MINION_SPAWN_POINTS.computeIfAbsent(npcId, key -> new ArrayList<>()).add(location);
	}
	
	/**
	 * Initializes Core AI, restores boss state and schedules respawn if needed.
	 */
	private Core()
	{
		addKillId(CORE_NPC_ID, MINION_DEATH_KNIGHT_ID, MINION_DOOM_WRAITH_ID, MINION_SUSCEPTOR_ID);
		addAttackId(CORE_NPC_ID);
		addSpawnId(CORE_NPC_ID);
		addStartNpc(TELEPORT_CUBE_NPC_ID);
		addFirstTalkId(TELEPORT_CUBE_NPC_ID);
		addTalkId(TELEPORT_CUBE_NPC_ID);
		
		_firstAttacked = false;
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		final StatSet info = bossManager.getStatSet(CORE_NPC_ID);
		final int status = bossManager.getStatus(CORE_NPC_ID);
		if (status == STATUS_DEAD)
		{
			final long remaining = info.getLong("respawn_time") - System.currentTimeMillis();
			if (remaining > 0)
			{
				startQuestTimer(EVENT_CORE_UNLOCK, remaining, null, null);
			}
			else
			{
				final GrandBoss core = (GrandBoss) addSpawn(CORE_NPC_ID, CORE_DEFAULT_SPAWN, false, 0);
				bossManager.setStatus(CORE_NPC_ID, STATUS_ALIVE);
				spawnBoss(core);
			}
		}
		else
		{
			final boolean attacked = GlobalVariablesManager.getInstance().getBoolean("Core_Attacked", false);
			if (attacked)
			{
				_firstAttacked = true;
			}
			
			final int locX = info.getInt("loc_x");
			final int locY = info.getInt("loc_y");
			final int locZ = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			
			final GrandBoss core = (GrandBoss) addSpawn(CORE_NPC_ID, locX, locY, locZ, heading, false, 0);
			core.setCurrentHpMp(hp, mp);
			spawnBoss(core);
		}
	}
	
	/**
	 * Persists Core attack state to global variables.
	 */
	@Override
	public void onSave()
	{
		GlobalVariablesManager.getInstance().set("Core_Attacked", _firstAttacked);
	}
	
	/**
	 * Registers Core in the boss manager and spawns all configured minions.
	 * @param npc The Core boss instance.
	 */
	public void spawnBoss(GrandBoss npc)
	{
		if (npc == null)
		{
			return;
		}
		
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		
		for (Entry<Integer, List<Location>> spawnEntry : MINION_SPAWN_POINTS.entrySet())
		{
			final int minionId = spawnEntry.getKey();
			for (Location location : spawnEntry.getValue())
			{
				final Attackable minion = addSpawn(minionId, location, false, 0).asAttackable();
				minion.setIsRaidMinion(true);
				_minions.add(minion);
			}
		}
		
		_pendingRespawns.clear();
		_respawnIdGenerator.set(0);
	}
	
	/**
	 * Handles timed events such as Core unlock, minion respawn and teleport.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event == null)
		{
			return super.onEvent(null, npc, player);
		}
		
		// Robust minion respawn events of the form "spawn_minion_X".
		if (event.startsWith(EVENT_SPAWN_MINION_PREFIX))
		{
			final String idToken = event.substring(EVENT_SPAWN_MINION_PREFIX.length());
			try
			{
				final int respawnId = Integer.parseInt(idToken);
				final MinionRespawnData data = _pendingRespawns.remove(respawnId);
				if ((data != null) && (GrandBossManager.getInstance().getStatus(CORE_NPC_ID) == STATUS_ALIVE))
				{
					final Location location = data._location;
					final Attackable minion = addSpawn(data._npcId, location, false, 0).asAttackable();
					minion.setIsRaidMinion(true);
					_minions.add(minion);
				}
			}
			catch (NumberFormatException e)
			{
				LOGGER.log(Level.WARNING, Core.class.getSimpleName() + ": Failed to parse minion respawn id from event '" + event + "'.", e);
			}
		}
		else if (EVENT_CORE_UNLOCK.equals(event))
		{
			final GrandBoss core = (GrandBoss) addSpawn(CORE_NPC_ID, CORE_DEFAULT_SPAWN, false, 0);
			GrandBossManager.getInstance().setStatus(CORE_NPC_ID, STATUS_ALIVE);
			spawnBoss(core);
		}
		else if (EVENT_SPAWN_MINION_LEGACY.equals(event))
		{
			// Legacy simple minion respawn using NPC position from timer context.
			if (npc != null)
			{
				final Attackable minion = addSpawn(npc.getId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0).asAttackable();
				minion.setIsRaidMinion(true);
				_minions.add(minion);
			}
		}
		else if (EVENT_DESPAWN_MINIONS.equals(event))
		{
			for (Attackable minion : _minions)
			{
				if (minion != null)
				{
					minion.decayMe();
				}
			}
			
			_minions.clear();
			_pendingRespawns.clear();
			_respawnIdGenerator.set(0);
		}
		else if (event.startsWith(EVENT_TELEPORT_PREFIX))
		{
			if (player != null)
			{
				final String[] split = event.split(" ");
				if (split.length >= 2)
				{
					final String destination = split[1];
					switch (destination)
					{
						case "floor3":
						{
							player.teleToLocation(LOCATION_CRUMA_CORE_FLOOR3);
							break;
						}
						case "ground":
						{
							player.teleToLocation(LOCATION_CRUMA_ENTRANCE);
							break;
						}
						default:
						{
							LOGGER.warning(Core.class.getSimpleName() + ": Unknown teleport destination '" + destination + "' in event '" + event + "'.");
							break;
						}
					}
				}
				else
				{
					LOGGER.warning(Core.class.getSimpleName() + ": TELEPORT event missing destination token: '" + event + "'.");
				}
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles Core attack events and broadcasts combat voice lines.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc == null)
		{
			return;
		}
		
		if (_firstAttacked)
		{
			if (getRandom(100) == 0)
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Removing intruders."));
			}
		}
		else
		{
			_firstAttacked = true;
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "A non-permitted target has been discovered."));
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), "Intruder removal system initiated."));
		}
	}
	
	/**
	 * Handles Core and minion kill events, including respawn scheduling.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc == null)
		{
			return;
		}
		
		final int npcId = npc.getId();
		if (npcId == CORE_NPC_ID)
		{
			final int objectId = npc.getObjectId();
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, objectId, npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, "A fatal error has occurred."));
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, "System is being shut down..."));
			npc.broadcastPacket(new NpcSay(objectId, ChatType.NPC_GENERAL, npcId, "......"));
			
			_firstAttacked = false;
			
			addSpawn(TELEPORT_CUBE_NPC_ID, 16502, 110165, -6394, 0, false, TELEPORT_CUBE_DESPAWN_MS);
			addSpawn(TELEPORT_CUBE_NPC_ID, 18948, 110166, -6397, 0, false, TELEPORT_CUBE_DESPAWN_MS);
			
			final GrandBossManager bossManager = GrandBossManager.getInstance();
			bossManager.setStatus(CORE_NPC_ID, STATUS_DEAD);
			
			final long baseIntervalMillis = GrandBossConfig.CORE_SPAWN_INTERVAL * MILLIS_PER_HOUR;
			final long randomRangeMillis = GrandBossConfig.CORE_SPAWN_RANDOM * MILLIS_PER_HOUR;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info("Core will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer(EVENT_CORE_UNLOCK, respawnTime, null, null);
			
			final StatSet info = bossManager.getStatSet(CORE_NPC_ID);
			info.set("respawn_time", nextRespawnTime);
			bossManager.setStatSet(CORE_NPC_ID, info);
			
			startQuestTimer(EVENT_DESPAWN_MINIONS, MINION_DESPAWN_DELAY_MS, null, null);
			
			for (int i = 0; i < _respawnIdGenerator.get(); i++)
			{
				cancelQuestTimer(EVENT_SPAWN_MINION_PREFIX + i, null, null);
			}
			
			_pendingRespawns.clear();
			_respawnIdGenerator.set(0);
			
			// Cancel legacy spawn_minion timers as well.
			cancelQuestTimers(EVENT_SPAWN_MINION_LEGACY);
		}
		else if ((GrandBossManager.getInstance().getStatus(CORE_NPC_ID) == STATUS_ALIVE) && _minions.contains(npc))
		{
			_minions.remove(npc);
			
			final int respawnId = _respawnIdGenerator.getAndIncrement();
			final Location respawnLocation = new Location(npc.getX(), npc.getY(), npc.getZ());
			
			_pendingRespawns.put(respawnId, new MinionRespawnData(npcId, respawnLocation));
			startQuestTimer(EVENT_SPAWN_MINION_PREFIX + respawnId, MINION_RESPAWN_DELAY_MS, null, null);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setImmobilized(true);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new Core();
	}
}
