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
package ai.bosses.Zaken;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Controls Zaken raid boss behavior, including form switching, teleport logic and minion spawning.<br>
 * Persists boss state across restarts and cleans up scheduled tasks on death.
 * <ul>
 * <li>Day/night face management and regeneration handling.</li>
 * <li>Teleport logic based on combat state, distance and HP thresholds.</li>
 * <li>Minion wave spawning and delayed minion respawn scheduling.</li>
 * </ul>
 * @author BazookaRpm
 */
public class Zaken extends Script
{
	// Logging.
	private static final Logger LOGGER = Logger.getLogger(Zaken.class.getName());
	
	// NPC identifiers.
	private static final int ZAKEN_NPC_ID = 29022;
	private static final int DOLL_BLADER_B_NPC_ID = 29023;
	private static final int VALE_MASTER_B_NPC_ID = 29024;
	private static final int PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID = 29026;
	private static final int PIRATES_ZOMBIE_B_NPC_ID = 29027;
	
	// Boss status flags.
	private static final byte STATUS_ALIVE = 0;
	private static final byte STATUS_DEAD = 1;
	
	// Event identifiers.
	private static final String EVENT_ZAKEN_UNLOCK = "zaken_unlock";
	private static final String TIMER_MAIN = "1001";
	private static final String TIMER_FACTION_TELEPORT = "1002";
	private static final String TIMER_MINION_WAVES = "1003";
	private static final String MINION_RESPAWN_PREFIX = "RESPAWN_MINION:";
	
	// Skill identifiers.
	private static final int SKILL_TELEPORT_SELF = 4222;
	private static final int SKILL_FACE_DAY = 4223;
	private static final int SKILL_REGEN_NIGHT = 4227;
	private static final int SKILL_TELEPORT_SINGLE = 4216;
	private static final int SKILL_TELEPORT_AREA = 4217;
	private static final int SKILL_4218 = 4218;
	private static final int SKILL_4219 = 4219;
	private static final int SKILL_4220 = 4220;
	private static final int SKILL_4221 = 4221;
	private static final int SKILL_MOUNT_DEBUFF = 4258;
	private static final int SKILL_REGEN_CANCEL = 4242;
	private static final int SKILL_FACE_NIGHT = 4224;
	
	// Gameplay constants.
	private static final int NIGHT_END_HOUR = 5;
	private static final int TELEPORT_RANGE = 1500;
	private static final int AREA_TELEPORT_RANGE = 250;
	private static final int Z_LEVEL_TOLERANCE = 100;
	private static final int FACTION_CALL_Z_TOLERANCE = 200;
	private static final int SKILL_RANGE_CHECK = 100;
	private static final int TELEPORT_OFFSET_MAX = 650;
	private static final int TELEPORT_LOCATION_COUNT = 15;
	private static final int AREA_TELEPORT_TRACK_LIMIT = 5;
	private static final int MAX_HEADING = 65536;
	private static final int SKILL_ROLL_MAX = 15 * 15;
	
	// Timing constants.
	private static final long MILLIS_PER_SECOND = 1000L;
	private static final long MILLIS_PER_HOUR = 3600000L;
	
	// Minion respawn scheduling.
	private static final AtomicInteger MINION_RESPAWN_ID = new AtomicInteger(0);
	
	// Teleport locations.
	private static final Location[] TELEPORT_LOCATIONS =
	{
		new Location(53950, 219860, -3488),
		new Location(55980, 219820, -3488),
		new Location(54950, 218790, -3488),
		new Location(55970, 217770, -3488),
		new Location(53930, 217760, -3488),
		new Location(55970, 217770, -3216),
		new Location(55980, 219920, -3216),
		new Location(54960, 218790, -3216),
		new Location(53950, 219860, -3216),
		new Location(53930, 217760, -3216),
		new Location(55970, 217770, -2944),
		new Location(55980, 219920, -2944),
		new Location(54960, 218790, -2944),
		new Location(53950, 219860, -2944),
		new Location(53930, 217760, -2944)
	};
	
	// Zone management.
	private static BossZone _zone;
	
	// Concurrency control.
	private final Object _stateLock = new Object();
	
	// Boss instance tracking.
	private GrandBoss _zakenBoss = null;
	
	// Respawn management.
	private final Set<Integer> _pendingMinionRespawnIds = ConcurrentHashMap.newKeySet();
	private final ConcurrentHashMap<Integer, MinionRespawnData> _minionRespawnData = new ConcurrentHashMap<>();
	
	// AI runtime state.
	private int _firstMainTick = 0;
	private int _teleportInProgress = 0;
	private int _teleportTargetX = 0;
	private int _teleportTargetY = 0;
	private int _teleportTargetZ = 0;
	private int _minionSpawnCycle = 0;
	private int _areaTeleportTrackedCount = 0;
	private int _mostHatedTicks = 0;
	private int _daytimeTeleportHpStage = 0;
	private Creature _mostHatedTarget = null;
	
	// Area teleport tracking.
	private Player _areaTeleportPlayer1 = null;
	private Player _areaTeleportPlayer2 = null;
	private Player _areaTeleportPlayer3 = null;
	private Player _areaTeleportPlayer4 = null;
	private Player _areaTeleportPlayer5 = null;
	
	/**
	 * Initializes listeners and restores Zaken state from persistent grand boss data.
	 */
	public Zaken()
	{
		addAttackId(ZAKEN_NPC_ID);
		addKillId(ZAKEN_NPC_ID, DOLL_BLADER_B_NPC_ID, VALE_MASTER_B_NPC_ID, PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, PIRATES_ZOMBIE_B_NPC_ID);
		addSpellFinishedId(ZAKEN_NPC_ID);
		addAggroRangeEnterId(ZAKEN_NPC_ID);
		addFactionCallId(ZAKEN_NPC_ID);
		
		_zone = GrandBossManager.getInstance().getZone(55312, 219168, -3223);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(ZAKEN_NPC_ID);
		if (GrandBossManager.getInstance().getStatus(ZAKEN_NPC_ID) == STATUS_DEAD)
		{
			final long remainingDelay = info.getLong("respawn_time") - System.currentTimeMillis();
			if (remainingDelay > 0)
			{
				startQuestTimer(EVENT_ZAKEN_UNLOCK, remainingDelay, null, null);
			}
			else
			{
				final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN_NPC_ID, 55312, 219168, -3223, 0, false, 0);
				GrandBossManager.getInstance().setStatus(ZAKEN_NPC_ID, STATUS_ALIVE);
				spawnBoss(zaken);
			}
		}
		else
		{
			final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN_NPC_ID, info.getInt("loc_x"), info.getInt("loc_y"), info.getInt("loc_z"), info.getInt("heading"), false, 0);
			zaken.setCurrentHpMp(info.getInt("currentHP"), info.getInt("currentMP"));
			spawnBoss(zaken);
		}
	}
	
	/**
	 * Handles scheduled events, including minion respawns and boss timers.
	 * @param event
	 * @param npc
	 * @param player
	 * @return Event result.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((event != null) && event.startsWith(MINION_RESPAWN_PREFIX))
		{
			final int id;
			try
			{
				id = Integer.parseInt(event.substring(MINION_RESPAWN_PREFIX.length()));
			}
			catch (Exception e)
			{
				LOGGER.warning("Zaken minion respawn event parse failed: event=" + event + ".");
				return null;
			}
			
			_pendingMinionRespawnIds.remove(id);
			final MinionRespawnData data = _minionRespawnData.remove(id);
			if (data == null)
			{
				return null;
			}
			
			if (GrandBossManager.getInstance().getStatus(ZAKEN_NPC_ID) != STATUS_ALIVE)
			{
				return null;
			}
			
			addSpawn(data.getNpcId(), data.getX(), data.getY(), data.getZ(), data.getHeading(), false, 0);
			return null;
		}
		
		if (event == null)
		{
			return null;
		}
		
		synchronized (_stateLock)
		{
			switch (event)
			{
				case TIMER_MAIN:
				{
					if (_firstMainTick == 1)
					{
						_firstMainTick = 0;
						cancelQuestTimer(TIMER_MAIN, npc, null);
					}
					
					int hasDayFace = 0;
					int hasNightRegen = 0;
					for (BuffInfo effect : npc.getEffectList().getEffects())
					{
						if (effect.getSkill().getId() == SKILL_REGEN_NIGHT)
						{
							hasNightRegen = 1;
						}
						
						if (effect.getSkill().getId() == SKILL_FACE_DAY)
						{
							hasDayFace = 1;
						}
					}
					
					if (getTimeHour() < NIGHT_END_HOUR)
					{
						if (hasDayFace == 1)
						{
							npc.setTarget(npc);
							npc.doCast(SkillData.getInstance().getSkill(SKILL_FACE_NIGHT, 1));
							_teleportTargetX = npc.getX();
							_teleportTargetY = npc.getY();
							_teleportTargetZ = npc.getZ();
						}
						
						if (hasNightRegen == 0)
						{
							npc.setTarget(npc);
							npc.doCast(SkillData.getInstance().getSkill(SKILL_REGEN_NIGHT, 1));
						}
						
						if ((npc.getAI().getIntention() == Intention.ATTACK) && (_teleportInProgress == 0))
						{
							int isTargetOutOfRange = 0;
							int areAllTargetsOutOfRange = 1;
							
							final Creature mostHatedForTeleport = npc.asAttackable().getMostHated();
							if (mostHatedForTeleport != null)
							{
								{
									final long dx = (long) mostHatedForTeleport.getX() - _teleportTargetX;
									final long dy = (long) mostHatedForTeleport.getY() - _teleportTargetY;
									if (((dx * dx) + (dy * dy)) > (TELEPORT_RANGE * (long) TELEPORT_RANGE))
									{
										isTargetOutOfRange = 1;
									}
									else
									{
										isTargetOutOfRange = 0;
									}
								}
								
								if (isTargetOutOfRange == 0)
								{
									areAllTargetsOutOfRange = 0;
								}
								
								if (_areaTeleportTrackedCount > 0)
								{
									if (_areaTeleportPlayer1 == null)
									{
										isTargetOutOfRange = 0;
									}
									else
									{
										final long dx = (long) _areaTeleportPlayer1.getX() - _teleportTargetX;
										final long dy = (long) _areaTeleportPlayer1.getY() - _teleportTargetY;
										if (((dx * dx) + (dy * dy)) > (TELEPORT_RANGE * (long) TELEPORT_RANGE))
										{
											isTargetOutOfRange = 1;
										}
										else
										{
											isTargetOutOfRange = 0;
										}
									}
									
									if (isTargetOutOfRange == 0)
									{
										areAllTargetsOutOfRange = 0;
									}
								}
								
								if (_areaTeleportTrackedCount > 1)
								{
									if (_areaTeleportPlayer2 == null)
									{
										isTargetOutOfRange = 0;
									}
									else
									{
										final long dx = (long) _areaTeleportPlayer2.getX() - _teleportTargetX;
										final long dy = (long) _areaTeleportPlayer2.getY() - _teleportTargetY;
										if (((dx * dx) + (dy * dy)) > (TELEPORT_RANGE * (long) TELEPORT_RANGE))
										{
											isTargetOutOfRange = 1;
										}
										else
										{
											isTargetOutOfRange = 0;
										}
									}
									
									if (isTargetOutOfRange == 0)
									{
										areAllTargetsOutOfRange = 0;
									}
								}
								
								if (_areaTeleportTrackedCount > 2)
								{
									if (_areaTeleportPlayer3 == null)
									{
										isTargetOutOfRange = 0;
									}
									else
									{
										final long dx = (long) _areaTeleportPlayer3.getX() - _teleportTargetX;
										final long dy = (long) _areaTeleportPlayer3.getY() - _teleportTargetY;
										if (((dx * dx) + (dy * dy)) > (TELEPORT_RANGE * (long) TELEPORT_RANGE))
										{
											isTargetOutOfRange = 1;
										}
										else
										{
											isTargetOutOfRange = 0;
										}
									}
									
									if (isTargetOutOfRange == 0)
									{
										areAllTargetsOutOfRange = 0;
									}
								}
								
								if (_areaTeleportTrackedCount > 3)
								{
									if (_areaTeleportPlayer4 == null)
									{
										isTargetOutOfRange = 0;
									}
									else
									{
										final long dx = (long) _areaTeleportPlayer4.getX() - _teleportTargetX;
										final long dy = (long) _areaTeleportPlayer4.getY() - _teleportTargetY;
										if (((dx * dx) + (dy * dy)) > (TELEPORT_RANGE * (long) TELEPORT_RANGE))
										{
											isTargetOutOfRange = 1;
										}
										else
										{
											isTargetOutOfRange = 0;
										}
									}
									
									if (isTargetOutOfRange == 0)
									{
										areAllTargetsOutOfRange = 0;
									}
								}
								
								if (_areaTeleportTrackedCount >= AREA_TELEPORT_TRACK_LIMIT)
								{
									if (_areaTeleportPlayer5 == null)
									{
										isTargetOutOfRange = 0;
									}
									else
									{
										final long dx = (long) _areaTeleportPlayer5.getX() - _teleportTargetX;
										final long dy = (long) _areaTeleportPlayer5.getY() - _teleportTargetY;
										if (((dx * dx) + (dy * dy)) > (TELEPORT_RANGE * (long) TELEPORT_RANGE))
										{
											isTargetOutOfRange = 1;
										}
										else
										{
											isTargetOutOfRange = 0;
										}
									}
									
									if (isTargetOutOfRange == 0)
									{
										areAllTargetsOutOfRange = 0;
									}
								}
								
								if (areAllTargetsOutOfRange == 1)
								{
									_areaTeleportTrackedCount = 0;
									final Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
									_teleportTargetX = location.getX() + getRandom(TELEPORT_OFFSET_MAX);
									_teleportTargetY = location.getY() + getRandom(TELEPORT_OFFSET_MAX);
									_teleportTargetZ = location.getZ();
									npc.setTarget(npc);
									npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_SELF, 1));
								}
							}
						}
						
						if ((getRandom(20) < 1) && (_teleportInProgress == 0))
						{
							_teleportTargetX = npc.getX();
							_teleportTargetY = npc.getY();
							_teleportTargetZ = npc.getZ();
						}
						
						final Creature mostHated = npc.asAttackable().getMostHated();
						if ((npc.getAI().getIntention() == Intention.ATTACK) && (_mostHatedTicks == 0) && (mostHated != null))
						{
							_mostHatedTarget = mostHated;
							_mostHatedTicks = 1;
						}
						else if ((npc.getAI().getIntention() == Intention.ATTACK) && (_mostHatedTicks != 0) && (mostHated != null))
						{
							if (mostHated == _mostHatedTarget)
							{
								_mostHatedTicks++;
							}
							else
							{
								_mostHatedTarget = mostHated;
								_mostHatedTicks = 1;
							}
						}
						
						if (npc.getAI().getIntention() == Intention.IDLE)
						{
							_mostHatedTicks = 0;
							_mostHatedTarget = null;
						}
						
						if (_mostHatedTicks > 5)
						{
							if (_mostHatedTarget != null)
							{
								npc.asAttackable().stopHating(_mostHatedTarget);
							}
							final Creature nextTarget = npc.asAttackable().getMostHated();
							if (nextTarget != null)
							{
								npc.getAI().setIntention(Intention.ATTACK, nextTarget);
							}
							
							_mostHatedTicks = 0;
							_mostHatedTarget = null;
						}
					}
					else if (hasDayFace == 0)
					{
						npc.setTarget(npc);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_FACE_DAY, 1));
						_daytimeTeleportHpStage = 3;
					}
					
					if ((getTimeHour() >= NIGHT_END_HOUR) && (hasNightRegen == 1))
					{
						npc.setTarget(npc);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_REGEN_CANCEL, 1));
					}
					
					if (getRandom(40) < 1)
					{
						final Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
						_teleportTargetX = location.getX() + getRandom(TELEPORT_OFFSET_MAX);
						_teleportTargetY = location.getY() + getRandom(TELEPORT_OFFSET_MAX);
						_teleportTargetZ = location.getZ();
						npc.setTarget(npc);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_SELF, 1));
					}
					
					startQuestTimer(TIMER_MAIN, 30 * MILLIS_PER_SECOND, npc, null);
					break;
				}
				case TIMER_FACTION_TELEPORT:
				{
					_areaTeleportTrackedCount = 0;
					npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_SELF, 1));
					_teleportInProgress = 0;
					break;
				}
				case TIMER_MINION_WAVES:
				{
					switch (_minionSpawnCycle)
					{
						case 1:
						{
							final Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							_minionSpawnCycle = 2;
							break;
						}
						case 2:
						{
							final Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(DOLL_BLADER_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							_minionSpawnCycle = 3;
							break;
						}
						case 3:
						{
							Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(VALE_MASTER_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(VALE_MASTER_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							_minionSpawnCycle = 4;
							break;
						}
						case 4:
						{
							Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ(), getRandom(MAX_HEADING), false, 0);
							_minionSpawnCycle = 5;
							break;
						}
						case 5:
						{
							addSpawn(DOLL_BLADER_B_NPC_ID, 52675, 219371, -3290, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 52687, 219596, -3368, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 52672, 219740, -3418, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 52857, 219992, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 52959, 219997, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 53381, 220151, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 54236, 220948, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54885, 220144, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55264, 219860, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 55399, 220263, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55679, 220129, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 56276, 220783, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 57173, 220234, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 56267, 218826, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 56294, 219482, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 56094, 219113, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 56364, 218967, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 57113, 218079, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 56186, 217153, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55440, 218081, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 55202, 217940, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55225, 218236, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54973, 218075, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 53412, 218077, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54226, 218797, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54394, 219067, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54139, 219253, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 54262, 219480, -3488, getRandom(MAX_HEADING), false, 0);
							_minionSpawnCycle = 6;
							break;
						}
						case 6:
						{
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 53412, 218077, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54413, 217132, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 54841, 217132, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 55372, 217128, -3343, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 55893, 217122, -3488, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 56282, 217237, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 56963, 218080, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 56267, 218826, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 56294, 219482, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 56094, 219113, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 56364, 218967, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 56276, 220783, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 57173, 220234, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54885, 220144, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55264, 219860, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 55399, 220263, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55679, 220129, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 54236, 220948, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 54464, 219095, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54226, 218797, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54394, 219067, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54139, 219253, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 54262, 219480, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 53412, 218077, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55440, 218081, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 55202, 217940, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55225, 218236, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54973, 218075, -3216, getRandom(MAX_HEADING), false, 0);
							_minionSpawnCycle = 7;
							break;
						}
						case 7:
						{
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54228, 217504, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54181, 217168, -3216, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 54714, 217123, -3168, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 55298, 217127, -3073, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 55787, 217130, -2993, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 56284, 217216, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 56963, 218080, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 56267, 218826, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 56294, 219482, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 56094, 219113, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 56364, 218967, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 56276, 220783, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 57173, 220234, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54885, 220144, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55264, 219860, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 55399, 220263, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55679, 220129, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 54236, 220948, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 54464, 219095, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54226, 218797, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(VALE_MASTER_B_NPC_ID, 54394, 219067, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54139, 219253, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(DOLL_BLADER_B_NPC_ID, 54262, 219480, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 53412, 218077, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 54280, 217200, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55440, 218081, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_CAPTAIN_B_NPC_ID, 55202, 217940, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 55225, 218236, -2944, getRandom(MAX_HEADING), false, 0);
							addSpawn(PIRATES_ZOMBIE_B_NPC_ID, 54973, 218075, -2944, getRandom(MAX_HEADING), false, 0);
							_minionSpawnCycle = 8;
							cancelQuestTimer(TIMER_MINION_WAVES, null, null);
							break;
						}
					}
					break;
				}
				case EVENT_ZAKEN_UNLOCK:
				{
					if (GrandBossManager.getInstance().getStatus(ZAKEN_NPC_ID) == STATUS_DEAD)
					{
						final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN_NPC_ID, 55312, 219168, -3223, 0, false, 0);
						GrandBossManager.getInstance().setStatus(ZAKEN_NPC_ID, STATUS_ALIVE);
						spawnBoss(zaken);
					}
					break;
				}
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles faction assistance calls and triggers conditional boss teleports.
	 * @param npc
	 * @param caller
	 * @param attacker
	 * @param isPet
	 */
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isPet)
	{
		if ((caller == null) || (npc == null) || (attacker == null))
		{
			return;
		}
		
		synchronized (_stateLock)
		{
			if ((getTimeHour() < NIGHT_END_HOUR) && (caller.getId() != ZAKEN_NPC_ID) && (npc.getId() == ZAKEN_NPC_ID))
			{
				final Attackable attackableCaller = caller.asAttackable();
				if (attackableCaller == null)
				{
					return;
				}
				
				final Creature originalAttacker;
				if (isPet)
				{
					final Creature summon = attacker.getSummon();
					originalAttacker = (summon != null) ? summon : attacker;
				}
				else
				{
					originalAttacker = attacker;
				}
				
				if (!attackableCaller.getAggroList().containsKey(originalAttacker))
				{
					return;
				}
				
				final long dx = (long) originalAttacker.getX() - caller.getX();
				final long dy = (long) originalAttacker.getY() - caller.getY();
				long dz = (long) originalAttacker.getZ() - caller.getZ();
				if (dz < 0)
				{
					dz = -dz;
				}
				
				if ((dz <= FACTION_CALL_Z_TOLERANCE) && (((dx * dx) + (dy * dy)) <= (TELEPORT_RANGE * (long) TELEPORT_RANGE)))
				{
					if ((npc.getAI().getIntention() == Intention.IDLE) && (_teleportInProgress == 0) && (getRandom(30 * 15) < 1))
					{
						_teleportInProgress = 1;
						_teleportTargetX = caller.getX();
						_teleportTargetY = caller.getY();
						_teleportTargetZ = caller.getZ();
						startQuestTimer(TIMER_FACTION_TELEPORT, (MILLIS_PER_SECOND * 3) / 10, npc, null);
					}
				}
			}
		}
	}
	
	/**
	 * Handles post-cast actions for teleport-related skills.
	 * @param npc
	 * @param player
	 * @param skill
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		synchronized (_stateLock)
		{
			if (npc.getId() == ZAKEN_NPC_ID)
			{
				final int skillId = skill.getId();
				if (skillId == SKILL_TELEPORT_SELF)
				{
					npc.teleToLocation(_teleportTargetX, _teleportTargetY, _teleportTargetZ);
					npc.getAI().setIntention(Intention.IDLE);
				}
				else if (skillId == SKILL_TELEPORT_SINGLE)
				{
					final Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
					player.teleToLocation(location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ());
					npc.asAttackable().stopHating(player);
					final Creature nextTarget = npc.asAttackable().getMostHated();
					if (nextTarget != null)
					{
						npc.getAI().setIntention(Intention.ATTACK, nextTarget);
					}
				}
				else if (skillId == SKILL_TELEPORT_AREA)
				{
					int shouldSkipOtherTeleport = 0;
					Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
					player.teleToLocation(location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ());
					npc.asAttackable().stopHating(player);
					
					if ((_areaTeleportPlayer1 != null) && (_areaTeleportTrackedCount > 0) && (_areaTeleportPlayer1 != player) && (_areaTeleportPlayer1.getZ() > (player.getZ() - Z_LEVEL_TOLERANCE)) && (_areaTeleportPlayer1.getZ() < (player.getZ() + Z_LEVEL_TOLERANCE)))
					{
						final long dx = (long) _areaTeleportPlayer1.getX() - player.getX();
						final long dy = (long) _areaTeleportPlayer1.getY() - player.getY();
						if (((dx * dx) + (dy * dy)) > (AREA_TELEPORT_RANGE * (long) AREA_TELEPORT_RANGE))
						{
							shouldSkipOtherTeleport = 1;
						}
						else
						{
							shouldSkipOtherTeleport = 0;
						}
						
						if (shouldSkipOtherTeleport == 0)
						{
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							_areaTeleportPlayer1.teleToLocation(location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ());
							npc.asAttackable().stopHating(_areaTeleportPlayer1);
						}
					}
					
					if ((_areaTeleportPlayer2 != null) && (_areaTeleportTrackedCount > 1) && (_areaTeleportPlayer2 != player) && (_areaTeleportPlayer2.getZ() > (player.getZ() - Z_LEVEL_TOLERANCE)) && (_areaTeleportPlayer2.getZ() < (player.getZ() + Z_LEVEL_TOLERANCE)))
					{
						final long dx = (long) _areaTeleportPlayer2.getX() - player.getX();
						final long dy = (long) _areaTeleportPlayer2.getY() - player.getY();
						if (((dx * dx) + (dy * dy)) > (AREA_TELEPORT_RANGE * (long) AREA_TELEPORT_RANGE))
						{
							shouldSkipOtherTeleport = 1;
						}
						else
						{
							shouldSkipOtherTeleport = 0;
						}
						
						if (shouldSkipOtherTeleport == 0)
						{
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							_areaTeleportPlayer2.teleToLocation(location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ());
							npc.asAttackable().stopHating(_areaTeleportPlayer2);
						}
					}
					
					if ((_areaTeleportPlayer3 != null) && (_areaTeleportTrackedCount > 2) && (_areaTeleportPlayer3 != player) && (_areaTeleportPlayer3.getZ() > (player.getZ() - Z_LEVEL_TOLERANCE)) && (_areaTeleportPlayer3.getZ() < (player.getZ() + Z_LEVEL_TOLERANCE)))
					{
						final long dx = (long) _areaTeleportPlayer3.getX() - player.getX();
						final long dy = (long) _areaTeleportPlayer3.getY() - player.getY();
						if (((dx * dx) + (dy * dy)) > (AREA_TELEPORT_RANGE * (long) AREA_TELEPORT_RANGE))
						{
							shouldSkipOtherTeleport = 1;
						}
						else
						{
							shouldSkipOtherTeleport = 0;
						}
						
						if (shouldSkipOtherTeleport == 0)
						{
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							_areaTeleportPlayer3.teleToLocation(location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ());
							npc.asAttackable().stopHating(_areaTeleportPlayer3);
						}
					}
					
					if ((_areaTeleportPlayer4 != null) && (_areaTeleportTrackedCount > 3) && (_areaTeleportPlayer4 != player) && (_areaTeleportPlayer4.getZ() > (player.getZ() - Z_LEVEL_TOLERANCE)) && (_areaTeleportPlayer4.getZ() < (player.getZ() + Z_LEVEL_TOLERANCE)))
					{
						final long dx = (long) _areaTeleportPlayer4.getX() - player.getX();
						final long dy = (long) _areaTeleportPlayer4.getY() - player.getY();
						if (((dx * dx) + (dy * dy)) > (AREA_TELEPORT_RANGE * (long) AREA_TELEPORT_RANGE))
						{
							shouldSkipOtherTeleport = 1;
						}
						else
						{
							shouldSkipOtherTeleport = 0;
						}
						
						if (shouldSkipOtherTeleport == 0)
						{
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							_areaTeleportPlayer4.teleToLocation(location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ());
							npc.asAttackable().stopHating(_areaTeleportPlayer4);
						}
					}
					
					if ((_areaTeleportPlayer5 != null) && (_areaTeleportTrackedCount >= AREA_TELEPORT_TRACK_LIMIT) && (_areaTeleportPlayer5 != player) && (_areaTeleportPlayer5.getZ() > (player.getZ() - Z_LEVEL_TOLERANCE)) && (_areaTeleportPlayer5.getZ() < (player.getZ() + Z_LEVEL_TOLERANCE)))
					{
						final long dx = (long) _areaTeleportPlayer5.getX() - player.getX();
						final long dy = (long) _areaTeleportPlayer5.getY() - player.getY();
						if (((dx * dx) + (dy * dy)) > (AREA_TELEPORT_RANGE * (long) AREA_TELEPORT_RANGE))
						{
							shouldSkipOtherTeleport = 1;
						}
						else
						{
							shouldSkipOtherTeleport = 0;
						}
						
						if (shouldSkipOtherTeleport == 0)
						{
							location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
							_areaTeleportPlayer5.teleToLocation(location.getX() + getRandom(TELEPORT_OFFSET_MAX), location.getY() + getRandom(TELEPORT_OFFSET_MAX), location.getZ());
							npc.asAttackable().stopHating(_areaTeleportPlayer5);
						}
					}
					
					final Creature nextTarget = npc.asAttackable().getMostHated();
					if (nextTarget != null)
					{
						npc.getAI().setIntention(Intention.ATTACK, nextTarget);
					}
				}
			}
		}
	}
	
	/**
	 * Handles Zaken combat actions and conditional skill usage.
	 * @param npc
	 * @param attacker
	 * @param damage
	 * @param isPet
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		synchronized (_stateLock)
		{
			if (npc.getId() == ZAKEN_NPC_ID)
			{
				if (attacker.isMounted())
				{
					int hasMountDebuff = 0;
					for (BuffInfo effect : attacker.getEffectList().getEffects())
					{
						if (effect.getSkill().getId() == SKILL_MOUNT_DEBUFF)
						{
							hasMountDebuff = 1;
						}
					}
					
					if (hasMountDebuff == 0)
					{
						npc.setTarget(attacker);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_MOUNT_DEBUFF, 1));
					}
				}
				
				final Creature originalAttacker;
				if (isPet)
				{
					final Creature summon = attacker.getSummon();
					originalAttacker = (summon != null) ? summon : attacker;
				}
				else
				{
					originalAttacker = attacker;
				}
				
				npc.asAttackable().addDamageHate(originalAttacker, 0, (int) ((((double) damage / npc.getMaxHp()) / 0.05) * 20000));
				
				if (getRandom(10) < 1)
				{
					final int roll = getRandom(SKILL_ROLL_MAX);
					if (roll < 1)
					{
						npc.setTarget(attacker);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_SINGLE, 1));
					}
					else if (roll < 2)
					{
						npc.setTarget(attacker);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_AREA, 1));
					}
					else if (roll < 4)
					{
						npc.setTarget(attacker);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_4219, 1));
					}
					else if (roll < 8)
					{
						npc.setTarget(attacker);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_4218, 1));
					}
					else if (roll < 15)
					{
						if (attacker != npc.asAttackable().getMostHated())
						{
							final long dx = (long) attacker.getX() - npc.getX();
							final long dy = (long) attacker.getY() - npc.getY();
							final long dz = (long) attacker.getZ() - npc.getZ();
							if (((dx * dx) + (dy * dy) + (dz * dz)) <= (SKILL_RANGE_CHECK * (long) SKILL_RANGE_CHECK))
							{
								npc.setTarget(attacker);
								npc.doCast(SkillData.getInstance().getSkill(SKILL_4221, 1));
							}
						}
					}
					
					if (getRandomBoolean() && (attacker == npc.asAttackable().getMostHated()))
					{
						npc.setTarget(attacker);
						npc.doCast(SkillData.getInstance().getSkill(SKILL_4220, 1));
					}
				}
				
				if ((getTimeHour() >= NIGHT_END_HOUR) && (npc.getCurrentHp() < ((npc.getMaxHp() * _daytimeTeleportHpStage) / 4.0)))
				{
					_daytimeTeleportHpStage = (_daytimeTeleportHpStage - 1);
					final Location location = TELEPORT_LOCATIONS[getRandom(TELEPORT_LOCATION_COUNT)];
					_teleportTargetX = location.getX() + getRandom(TELEPORT_OFFSET_MAX);
					_teleportTargetY = location.getY() + getRandom(TELEPORT_OFFSET_MAX);
					_teleportTargetZ = location.getZ();
					npc.setTarget(npc);
					npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_SELF, 1));
				}
			}
		}
	}
	
	/**
	 * Handles boss and minion deaths, including respawn scheduling.
	 * @param npc
	 * @param killer
	 * @param isPet
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		synchronized (_stateLock)
		{
			final int npcId = npc.getId();
			
			if (npcId == ZAKEN_NPC_ID)
			{
				npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
				GrandBossManager.getInstance().setStatus(ZAKEN_NPC_ID, STATUS_DEAD);
				
				cancelQuestTimer(TIMER_MAIN, npc, null);
				cancelQuestTimer(TIMER_MINION_WAVES, npc, null);
				
				for (Integer pendingId : new HashSet<>(_pendingMinionRespawnIds))
				{
					cancelQuestTimer(MINION_RESPAWN_PREFIX + pendingId, npc, null);
				}
				_pendingMinionRespawnIds.clear();
				_minionRespawnData.clear();
				_zakenBoss = null;
				
				final long currentTime = System.currentTimeMillis();
				final long baseIntervalMillis = GrandBossConfig.ZAKEN_SPAWN_INTERVAL * MILLIS_PER_HOUR;
				final long randomRangeMillis = GrandBossConfig.ZAKEN_SPAWN_RANDOM * MILLIS_PER_HOUR;
				final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
				
				LOGGER.info("Zaken (npcId=" + ZAKEN_NPC_ID + ") will respawn at: " + TimeUtil.getDateTimeString(currentTime + respawnTime) + " (delayMs=" + respawnTime + ", baseMs=" + baseIntervalMillis + ", randomRangeMs=" + randomRangeMillis + ").");
				
				startQuestTimer(EVENT_ZAKEN_UNLOCK, respawnTime, null, null);
				
				final StatSet info = GrandBossManager.getInstance().getStatSet(ZAKEN_NPC_ID);
				info.set("respawn_time", currentTime + respawnTime);
				GrandBossManager.getInstance().setStatSet(ZAKEN_NPC_ID, info);
				return;
			}
			
			if (GrandBossManager.getInstance().getStatus(ZAKEN_NPC_ID) == STATUS_ALIVE)
			{
				if (_zakenBoss == null)
				{
					LOGGER.warning("Zaken minion respawn skipped: boss instance is null while status is alive (minionNpcId=" + npcId + ").");
					return;
				}
				
				final int id = MINION_RESPAWN_ID.incrementAndGet();
				final String eventName = MINION_RESPAWN_PREFIX + id;
				_pendingMinionRespawnIds.add(id);
				_minionRespawnData.put(id, new MinionRespawnData(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading()));
				startQuestTimer(eventName, (30 + getRandom(60)) * MILLIS_PER_SECOND, _zakenBoss, null);
			}
		}
	}
	
	/**
	 * Handles aggro range entry logic and conditional skill usage.
	 * @param npc
	 * @param player
	 * @param isPet
	 */
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isPet)
	{
		synchronized (_stateLock)
		{
			if (npc.getId() == ZAKEN_NPC_ID)
			{
				if ((_zone != null) && _zone.isInsideZone(npc))
				{
					final Creature target;
					if (isPet)
					{
						final Creature summon = player.getSummon();
						target = (summon != null) ? summon : player;
					}
					else
					{
						target = player;
					}
					
					npc.asAttackable().addDamageHate(target, 1, 200);
				}
				
				if ((player.getZ() > (npc.getZ() - Z_LEVEL_TOLERANCE)) && (player.getZ() < (npc.getZ() + Z_LEVEL_TOLERANCE)))
				{
					if ((_areaTeleportTrackedCount < AREA_TELEPORT_TRACK_LIMIT) && (getRandom(3) < 1))
					{
						if (_areaTeleportTrackedCount == 0)
						{
							_areaTeleportPlayer1 = player;
						}
						else if (_areaTeleportTrackedCount == 1)
						{
							_areaTeleportPlayer2 = player;
						}
						else if (_areaTeleportTrackedCount == 2)
						{
							_areaTeleportPlayer3 = player;
						}
						else if (_areaTeleportTrackedCount == 3)
						{
							_areaTeleportPlayer4 = player;
						}
						else if (_areaTeleportTrackedCount == 4)
						{
							_areaTeleportPlayer5 = player;
						}
						
						_areaTeleportTrackedCount++;
					}
					
					if (getRandom(15) < 1)
					{
						final int roll = getRandom(SKILL_ROLL_MAX);
						if (roll < 1)
						{
							npc.setTarget(player);
							npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_SINGLE, 1));
						}
						else if (roll < 2)
						{
							npc.setTarget(player);
							npc.doCast(SkillData.getInstance().getSkill(SKILL_TELEPORT_AREA, 1));
						}
						else if (roll < 4)
						{
							npc.setTarget(player);
							npc.doCast(SkillData.getInstance().getSkill(SKILL_4219, 1));
						}
						else if (roll < 8)
						{
							npc.setTarget(player);
							npc.doCast(SkillData.getInstance().getSkill(SKILL_4218, 1));
						}
						else if (roll < 15)
						{
							if (player != npc.asAttackable().getMostHated())
							{
								final long dx = (long) player.getX() - npc.getX();
								final long dy = (long) player.getY() - npc.getY();
								if (((dx * dx) + (dy * dy)) <= (SKILL_RANGE_CHECK * (long) SKILL_RANGE_CHECK))
								{
									npc.setTarget(player);
									npc.doCast(SkillData.getInstance().getSkill(SKILL_4221, 1));
								}
							}
						}
						
						if (getRandomBoolean() && (player == npc.asAttackable().getMostHated()))
						{
							npc.setTarget(player);
							npc.doCast(SkillData.getInstance().getSkill(SKILL_4220, 1));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Registers Zaken in the grand boss manager and initializes runtime state.
	 * @param npc
	 */
	public void spawnBoss(GrandBoss npc)
	{
		synchronized (_stateLock)
		{
			if (npc == null)
			{
				LOGGER.warning("Zaken AI failed to load: missing grand boss entry for npcId=" + ZAKEN_NPC_ID + " (Zaken) in grandboss_data.sql.");
				return;
			}
			
			GrandBossManager.getInstance().addBoss(npc);
			_zakenBoss = npc;
			
			_pendingMinionRespawnIds.clear();
			_minionRespawnData.clear();
			
			npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			_teleportInProgress = 0;
			_teleportTargetX = npc.getX();
			_teleportTargetY = npc.getY();
			_teleportTargetZ = npc.getZ();
			_minionSpawnCycle = 0;
			_areaTeleportTrackedCount = 0;
			_mostHatedTicks = 0;
			_daytimeTeleportHpStage = 3;
			_mostHatedTarget = null;
			_areaTeleportPlayer1 = null;
			_areaTeleportPlayer2 = null;
			_areaTeleportPlayer3 = null;
			_areaTeleportPlayer4 = null;
			_areaTeleportPlayer5 = null;
			
			if (_zone == null)
			{
				LOGGER.warning("Zaken AI failed to load: missing BossZone for npcId=" + ZAKEN_NPC_ID + " at seed loc (55312,219168,-3223).");
				return;
			}
			
			if (_zone.isInsideZone(npc))
			{
				_minionSpawnCycle = 1;
				startQuestTimer(TIMER_MINION_WAVES, (MILLIS_PER_SECOND * 17) / 10, npc, null, true);
			}
			
			_firstMainTick = 1;
			startQuestTimer(TIMER_MAIN, MILLIS_PER_SECOND, npc, null);
		}
	}
	
	// Internal data containers.
	private static final class MinionRespawnData
	{
		private final int _npcId;
		private final int _x;
		private final int _y;
		private final int _z;
		private final int _heading;
		
		public MinionRespawnData(int npcId, int x, int y, int z, int heading)
		{
			_npcId = npcId;
			_x = x;
			_y = y;
			_z = z;
			_heading = heading;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public int getX()
		{
			return _x;
		}
		
		public int getY()
		{
			return _y;
		}
		
		public int getZ()
		{
			return _z;
		}
		
		public int getHeading()
		{
			return _heading;
		}
	}
	
	/**
	 * Returns the current in-game hour.
	 * @return Game hour.
	 */
	public int getTimeHour()
	{
		return (GameTimeTaskManager.getInstance().getGameTime() / 60) % 24;
	}
	
	public static void main(String[] args)
	{
		new Zaken();
	}
}
