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
package ai.bosses.Antharas;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * Antharas AI script.<br>
 * Handles state transitions, cinematic sequence, minion management and skill usage patterns.
 * <ul>
 * <li>Spawn and respawn scheduling with GrandBossManager integration.</li>
 * <li>Dynamic minion wave control based on fight duration and minion density.</li>
 * <li>Advanced skill rotation driven by distance, direction and custom hate model.</li>
 * <li>Fear-based movement phase with scripted pathing and invisible NPC triggers.</li>
 * </ul>
 * @author BazookaRpm
 */
public class Antharas extends Script
{
	// NPC IDs.
	private static final int ANTHARAS_NPC_ID = 29019; // Antharas.
	private static final int ANTHARAS_BEHEMOTH_ID = 29069; // Behemoth Dragon.
	private static final int ANTHARAS_BOMBER_ID = 29070; // Dragon Bomber.
	private static final int ANTHARAS_HEART_ID = 13001; // Heart of Warding.
	private static final int ANTHARAS_TELEPORT_CUBE_ID = 31859; // Teleportation Cubic.
	
	// Invisible NPC spawn locations (fear phase).
	private static final Map<Integer, Location> INVISIBLE_NPC_SPAWN_LOCATIONS = new HashMap<>();
	
	static
	{
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29077, new Location(177229, 113298, -7735)); // antaras_clear_npc_1.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29078, new Location(176707, 113585, -7735)); // antaras_clear_npc_2.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29079, new Location(176385, 113889, -7735)); // antaras_clear_npc_3.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29080, new Location(176082, 114241, -7735)); // antaras_clear_npc_4.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29081, new Location(176066, 114802, -7735)); // antaras_clear_npc_5.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29082, new Location(176095, 115313, -7735)); // antaras_clear_npc_6.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29083, new Location(176425, 115829, -7735)); // antaras_clear_npc_7.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29084, new Location(176949, 116378, -7735)); // antaras_clear_npc_8.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29085, new Location(177655, 116402, -7735)); // antaras_clear_npc_9.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29086, new Location(178248, 116395, -7735)); // antaras_clear_npc_10.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29087, new Location(178706, 115998, -7735)); // antaras_clear_npc_11.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29088, new Location(179208, 115452, -7735)); // antaras_clear_npc_12.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29089, new Location(179191, 115079, -7735)); // antaras_clear_npc_13.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29090, new Location(179221, 114546, -7735)); // antaras_clear_npc_14.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29091, new Location(178916, 113925, -7735)); // antaras_clear_npc_15.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29092, new Location(178782, 113814, -7735)); // antaras_clear_npc_16.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29093, new Location(178419, 113417, -7735)); // antaras_clear_npc_17.
		INVISIBLE_NPC_SPAWN_LOCATIONS.put(29094, new Location(177855, 113282, -7735)); // antaras_clear_npc_18.
	}
	
	// Item IDs.
	private static final int ANTHARAS_STONE_ITEM_ID = 3865; // Portal Stone.
	
	// Antharas skills.
	private static final SkillHolder ANTHARAS_JUMP = new SkillHolder(4106, 1); // Antharas Stun.
	private static final SkillHolder ANTHARAS_TAIL_ATTACK = new SkillHolder(4107, 1); // Antharas Stun (tail).
	private static final SkillHolder ANTHARAS_FEAR = new SkillHolder(4108, 1); // Antharas Terror.
	private static final SkillHolder ANTHARAS_DEBUFF = new SkillHolder(4109, 1); // Curse of Antharas.
	private static final SkillHolder ANTHARAS_MOUTH_ATTACK = new SkillHolder(4110, 2); // Breath Attack.
	private static final SkillHolder ANTHARAS_BREATH_ATTACK = new SkillHolder(4111, 1); // Antharas Fossilization.
	private static final SkillHolder ANTHARAS_NORMAL_ATTACK = new SkillHolder(4112, 1); // Ordinary Attack.
	private static final SkillHolder ANTHARAS_NORMAL_ATTACK_EX = new SkillHolder(4113, 1); // Ordinary Attack (extra).
	private static final SkillHolder ANTHARAS_REGEN_PHASE_1 = new SkillHolder(4125, 1); // Antharas Regeneration.
	private static final SkillHolder ANTHARAS_REGEN_PHASE_2 = new SkillHolder(4239, 1); // Antharas Regeneration.
	private static final SkillHolder ANTHARAS_REGEN_PHASE_3 = new SkillHolder(4240, 1); // Antharas Regeneration.
	private static final SkillHolder ANTHARAS_REGEN_PHASE_4 = new SkillHolder(4241, 1); // Antharas Regeneration.
	private static final SkillHolder ANTHARAS_BOMBER_DISPEL = new SkillHolder(5042, 1); // NPC Dispel Bomb.
	private static final SkillHolder ANTHARAS_ANTI_STRIDER = new SkillHolder(4258, 1); // Hinder Strider.
	private static final SkillHolder ANTHARAS_FEAR_SHORT = new SkillHolder(5092, 1); // Antharas Terror (short).
	private static final SkillHolder ANTHARAS_METEOR = new SkillHolder(5093, 1); // Antharas Meteor.
	
	// Zone.
	private static final NoRestartZone ANTHARAS_NEST_ZONE = ZoneManager.getInstance().getZoneById(70050, NoRestartZone.class); // Antharas Nest zone.
	
	// Grand boss status.
	private static final int STATUS_ALIVE = 0;
	private static final int STATUS_WAITING = 1;
	private static final int STATUS_IN_FIGHT = 2;
	private static final int STATUS_DEAD = 3;
	
	// Event names.
	private static final String EVENT_ENTER = "ENTER";
	private static final String EVENT_EXIT = "EXIT";
	private static final String EVENT_SPAWN_ANTHARAS = "SPAWN_ANTHARAS";
	private static final String EVENT_CAMERA_1 = "CAMERA_1";
	private static final String EVENT_CAMERA_2 = "CAMERA_2";
	private static final String EVENT_CAMERA_3 = "CAMERA_3";
	private static final String EVENT_CAMERA_4 = "CAMERA_4";
	private static final String EVENT_CAMERA_5 = "CAMERA_5";
	private static final String EVENT_SOCIAL = "SOCIAL";
	private static final String EVENT_START_MOVE = "START_MOVE";
	private static final String EVENT_SET_REGEN = "SET_REGEN";
	private static final String EVENT_CHECK_ATTACK = "CHECK_ATTACK";
	private static final String EVENT_SPAWN_MINION = "SPAWN_MINION";
	private static final String EVENT_CLEAR_ZONE = "CLEAR_ZONE";
	private static final String EVENT_TID_USED_FEAR = "TID_USED_FEAR";
	private static final String EVENT_TID_FEAR_MOVE_TIMEOVER = "TID_FEAR_MOVE_TIMEOVER";
	private static final String EVENT_TID_FEAR_COOLTIME = "TID_FEAR_COOLTIME";
	private static final String EVENT_CLEAR_STATUS = "CLEAR_STATUS";
	private static final String EVENT_SKIP_WAITING = "SKIP_WAITING";
	private static final String EVENT_RESPAWN_ANTHARAS = "RESPAWN_ANTHARAS";
	private static final String EVENT_DESPAWN_MINIONS = "DESPAWN_MINIONS";
	private static final String EVENT_ABORT_FIGHT = "ABORT_FIGHT";
	private static final String EVENT_MANAGE_SKILL = "MANAGE_SKILL";
	
	// Teleport and position constants.
	private static final Location ANTHARAS_IDLE_LOCATION = new Location(185708, 114298, -8221);
	private static final Location ANTHARAS_SPAWN_LOCATION = new Location(181323, 114850, -7623, 32542);
	private static final Location ANTHARAS_MOVE_TARGET_LOCATION = new Location(179011, 114871, -7704);
	private static final int ANTHARAS_FEAR_MOVE_X = 177648;
	private static final int ANTHARAS_FEAR_MOVE_Y = 114816;
	private static final int ANTHARAS_FEAR_MOVE_Z = -7735;
	private static final int TELEPORT_EXIT_BASE_X = 79800;
	private static final int TELEPORT_EXIT_BASE_Y = 151200;
	private static final int TELEPORT_EXIT_Z = -3534;
	
	// Timing constants (milliseconds).
	private static final long CHECK_ATTACK_INTERVAL = 60000L;
	private static final long MINION_SPAWN_INTERVAL = 300000L;
	private static final long REGEN_INTERVAL = 60000L;
	private static final long INACTIVITY_LIMIT = 900000L;
	private static final long TELEPORT_CUBE_DURATION = 900000L;
	private static final long FEAR_COOLDOWN = 300000L;
	private static final long FEAR_MOVE_FIRST_CHECK = 2000L;
	private static final long FEAR_MOVE_RETRY_INTERVAL = 5000L;
	private static final long FEAR_MOVE_FORCE_DELAY = 1000L;
	private static final long FEAR_USED_DELAY = 7000L;
	private static final int MAX_MOVE_RETRY_COUNT = 3;
	
	// Distance / range constants.
	private static final int MAX_THREAT_DISTANCE = 9000;
	private static final int BOMBER_EXPLOSION_RANGE = 230;
	
	// Misc.
	private GrandBoss _antharasBoss = null;
	private static long _lastAttackTime = 0;
	private static int _minionCount = 0;
	private static int _minionMultiplier = 0;
	private static int _moveRetryCount = 0;
	private static int _sandStormState = 0;
	private static Player _primaryAttacker = null;
	private static Player _secondaryAttacker = null;
	private static Player _tertiaryAttacker = null;
	private static int _primaryAttackerHate = 0;
	private static int _secondaryAttackerHate = 0;
	private static int _tertiaryAttackerHate = 0;
	
	/**
	 * Instantiates Antharas AI and restores persisted state from GrandBossManager.
	 */
	private Antharas()
	{
		addStartNpc(ANTHARAS_HEART_ID, ANTHARAS_TELEPORT_CUBE_ID);
		addTalkId(ANTHARAS_HEART_ID, ANTHARAS_TELEPORT_CUBE_ID);
		addFirstTalkId(ANTHARAS_HEART_ID);
		addSpawnId(INVISIBLE_NPC_SPAWN_LOCATIONS.keySet());
		addSpawnId(ANTHARAS_NPC_ID);
		addMoveFinishedId(ANTHARAS_BOMBER_ID);
		addAggroRangeEnterId(ANTHARAS_BOMBER_ID);
		addSpellFinishedId(ANTHARAS_NPC_ID);
		addAttackId(ANTHARAS_NPC_ID, ANTHARAS_BOMBER_ID, ANTHARAS_BEHEMOTH_ID);
		addKillId(ANTHARAS_NPC_ID, ANTHARAS_BEHEMOTH_ID);
		
		final StatSet antharasInfo = GrandBossManager.getInstance().getStatSet(ANTHARAS_NPC_ID);
		final double currentHp = antharasInfo.getDouble("currentHP");
		final double currentMp = antharasInfo.getDouble("currentMP");
		final int savedX = antharasInfo.getInt("loc_x");
		final int savedY = antharasInfo.getInt("loc_y");
		final int savedZ = antharasInfo.getInt("loc_z");
		final int savedHeading = antharasInfo.getInt("heading");
		final long respawnTime = antharasInfo.getLong("respawn_time");
		
		switch (getStatus())
		{
			case STATUS_ALIVE:
			{
				resetFightState();
				_antharasBoss = (GrandBoss) addSpawn(ANTHARAS_NPC_ID, ANTHARAS_IDLE_LOCATION, false, 0);
				_antharasBoss.setCurrentHpMp(currentHp, currentMp);
				addBoss(_antharasBoss);
				break;
			}
			case STATUS_WAITING:
			{
				resetFightState();
				_antharasBoss = (GrandBoss) addSpawn(ANTHARAS_NPC_ID, ANTHARAS_IDLE_LOCATION, false, 0);
				_antharasBoss.setCurrentHpMp(currentHp, currentMp);
				addBoss(_antharasBoss);
				startQuestTimer(EVENT_SPAWN_ANTHARAS, GrandBossConfig.ANTHARAS_WAIT_TIME * 60000L, null, null);
				break;
			}
			case STATUS_IN_FIGHT:
			{
				_antharasBoss = (GrandBoss) addSpawn(ANTHARAS_NPC_ID, savedX, savedY, savedZ, savedHeading, false, 0);
				_antharasBoss.setCurrentHpMp(currentHp, currentMp);
				addBoss(_antharasBoss);
				_lastAttackTime = System.currentTimeMillis();
				startQuestTimer(EVENT_CHECK_ATTACK, CHECK_ATTACK_INTERVAL, _antharasBoss, null);
				startQuestTimer(EVENT_SPAWN_MINION, MINION_SPAWN_INTERVAL, _antharasBoss, null);
				break;
			}
			case STATUS_DEAD:
			{
				final long remainingTime = respawnTime - System.currentTimeMillis();
				if (remainingTime > 0)
				{
					startQuestTimer(EVENT_CLEAR_STATUS, remainingTime, null, null);
				}
				else
				{
					setStatus(STATUS_ALIVE);
					resetFightState();
					_antharasBoss = (GrandBoss) addSpawn(ANTHARAS_NPC_ID, ANTHARAS_IDLE_LOCATION, false, 0);
					addBoss(_antharasBoss);
				}
				break;
			}
		}
	}
	
	/**
	 * Handles all scheduled events and core AI transitions.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case EVENT_ENTER:
			{
				String htmlText = null;
				final int status = getStatus();
				if (status == STATUS_DEAD)
				{
					htmlText = "13001-01.html";
				}
				else if (status == STATUS_IN_FIGHT)
				{
					htmlText = "13001-02.html";
				}
				else if (!hasQuestItems(player, ANTHARAS_STONE_ITEM_ID))
				{
					htmlText = "13001-03.html";
				}
				else if (hasQuestItems(player, ANTHARAS_STONE_ITEM_ID))
				{
					takeItems(player, ANTHARAS_STONE_ITEM_ID, 1);
					player.teleToLocation(179700 + getRandom(700), 113800 + getRandom(2100), -7709);
					if (status != STATUS_WAITING)
					{
						setStatus(STATUS_WAITING);
						startQuestTimer(EVENT_SPAWN_ANTHARAS, GrandBossConfig.ANTHARAS_WAIT_TIME * 60000L, null, null);
					}
				}
				
				return htmlText;
			}
			case EVENT_EXIT:
			{
				player.teleToLocation(TELEPORT_EXIT_BASE_X + getRandom(600), TELEPORT_EXIT_BASE_Y + getRandom(1100), TELEPORT_EXIT_Z);
				break;
			}
			case EVENT_SPAWN_ANTHARAS:
			{
				if (_antharasBoss == null)
				{
					break;
				}
				
				_antharasBoss.disableCoreAI(true);
				_antharasBoss.setRandomWalking(false);
				_antharasBoss.teleToLocation(ANTHARAS_SPAWN_LOCATION);
				setStatus(STATUS_IN_FIGHT);
				_lastAttackTime = System.currentTimeMillis();
				ANTHARAS_NEST_ZONE.broadcastPacket(new PlaySound("BS02_A"));
				startQuestTimer(EVENT_CAMERA_1, 23, _antharasBoss, null);
				break;
			}
			case EVENT_CAMERA_1:
			{
				if (npc == null)
				{
					break;
				}
				
				ANTHARAS_NEST_ZONE.broadcastPacket(new SpecialCamera(npc, 700, 13, -19, 0, 10000, 20000, 0, 0, 0, 0, 0));
				startQuestTimer(EVENT_CAMERA_2, 3000, npc, null);
				break;
			}
			case EVENT_CAMERA_2:
			{
				if (npc == null)
				{
					break;
				}
				
				ANTHARAS_NEST_ZONE.broadcastPacket(new SpecialCamera(npc, 700, 13, 0, 6000, 10000, 20000, 0, 0, 0, 0, 0));
				startQuestTimer(EVENT_CAMERA_3, 10000, npc, null);
				break;
			}
			case EVENT_CAMERA_3:
			{
				if (npc == null)
				{
					break;
				}
				
				ANTHARAS_NEST_ZONE.broadcastPacket(new SpecialCamera(npc, 3700, 0, -3, 0, 10000, 10000, 0, 0, 0, 0, 0));
				ANTHARAS_NEST_ZONE.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
				startQuestTimer(EVENT_CAMERA_4, 200, npc, null);
				startQuestTimer(EVENT_SOCIAL, 5200, npc, null);
				break;
			}
			case EVENT_CAMERA_4:
			{
				if (npc == null)
				{
					break;
				}
				
				ANTHARAS_NEST_ZONE.broadcastPacket(new SpecialCamera(npc, 1100, 0, -3, 22000, 10000, 30000, 0, 0, 0, 0, 0));
				startQuestTimer(EVENT_CAMERA_5, 10800, npc, null);
				break;
			}
			case EVENT_CAMERA_5:
			{
				if (npc == null)
				{
					break;
				}
				
				ANTHARAS_NEST_ZONE.broadcastPacket(new SpecialCamera(npc, 1100, 0, -3, 300, 10000, 7000, 0, 0, 0, 0, 0));
				startQuestTimer(EVENT_START_MOVE, 1900, npc, null);
				break;
			}
			case EVENT_SOCIAL:
			{
				if (npc == null)
				{
					break;
				}
				
				ANTHARAS_NEST_ZONE.broadcastPacket(new SocialAction(npc.getObjectId(), 2));
				break;
			}
			case EVENT_START_MOVE:
			{
				if (npc == null)
				{
					break;
				}
				
				npc.disableCoreAI(false);
				npc.setRandomWalking(true);
				
				for (Player nearbyPlayer : World.getInstance().getVisibleObjectsInRange(npc, Player.class, 4000))
				{
					if (nearbyPlayer.isHero() && GrandBossConfig.ANTHARAS_RECOGNIZE_HERO)
					{
						ANTHARAS_NEST_ZONE.broadcastPacket(new ExShowScreenMessage(nearbyPlayer.getName() + "!!!! You cannot hope to defeat me with your meager strength.", 2, 4000));
						break;
					}
				}
				
				npc.getAI().setIntention(Intention.MOVE_TO, ANTHARAS_MOVE_TARGET_LOCATION);
				startQuestTimer(EVENT_CHECK_ATTACK, CHECK_ATTACK_INTERVAL, npc, null);
				startQuestTimer(EVENT_SPAWN_MINION, MINION_SPAWN_INTERVAL, npc, null);
				break;
			}
			case EVENT_SET_REGEN:
			{
				if (npc == null)
				{
					break;
				}
				
				if (npc.getCurrentHp() < (npc.getMaxHp() * 0.25))
				{
					if (!npc.isAffectedBySkill(ANTHARAS_REGEN_PHASE_4.getSkillId()))
					{
						npc.getAI().setIntention(Intention.CAST, ANTHARAS_REGEN_PHASE_4.getSkill(), npc);
					}
				}
				else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.5))
				{
					if (!npc.isAffectedBySkill(ANTHARAS_REGEN_PHASE_3.getSkillId()))
					{
						npc.getAI().setIntention(Intention.CAST, ANTHARAS_REGEN_PHASE_3.getSkill(), npc);
					}
				}
				else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.75))
				{
					if (!npc.isAffectedBySkill(ANTHARAS_REGEN_PHASE_2.getSkillId()))
					{
						npc.getAI().setIntention(Intention.CAST, ANTHARAS_REGEN_PHASE_2.getSkill(), npc);
					}
				}
				else if (!npc.isAffectedBySkill(ANTHARAS_REGEN_PHASE_1.getSkillId()))
				{
					npc.getAI().setIntention(Intention.CAST, ANTHARAS_REGEN_PHASE_1.getSkill(), npc);
				}
				
				startQuestTimer(EVENT_SET_REGEN, REGEN_INTERVAL, npc, null);
				break;
			}
			case EVENT_CHECK_ATTACK:
			{
				if (npc == null)
				{
					break;
				}
				
				if ((_lastAttackTime + INACTIVITY_LIMIT) < System.currentTimeMillis())
				{
					setStatus(STATUS_ALIVE);
					for (Creature creature : ANTHARAS_NEST_ZONE.getCharactersInside())
					{
						if (creature == null)
						{
							continue;
						}
						
						if (creature.isNpc())
						{
							if (creature.getId() == ANTHARAS_NPC_ID)
							{
								creature.teleToLocation(ANTHARAS_IDLE_LOCATION);
							}
							else
							{
								creature.deleteMe();
							}
						}
						else if (creature.isPlayer())
						{
							creature.teleToLocation(TELEPORT_EXIT_BASE_X + getRandom(600), TELEPORT_EXIT_BASE_Y + getRandom(1100), TELEPORT_EXIT_Z);
						}
					}
					
					resetFightState();
					cancelQuestTimer(EVENT_CHECK_ATTACK, npc, null);
					cancelQuestTimer(EVENT_SPAWN_MINION, npc, null);
					cancelQuestTimer(EVENT_SET_REGEN, npc, null);
					cancelQuestTimer(EVENT_TID_USED_FEAR, npc, null);
					cancelQuestTimer(EVENT_TID_FEAR_MOVE_TIMEOVER, npc, null);
					cancelQuestTimer(EVENT_TID_FEAR_COOLTIME, npc, null);
				}
				else
				{
					if (_primaryAttackerHate > 10)
					{
						_primaryAttackerHate -= getRandom(10);
					}
					
					if (_secondaryAttackerHate > 10)
					{
						_secondaryAttackerHate -= getRandom(10);
					}
					
					if (_tertiaryAttackerHate > 10)
					{
						_tertiaryAttackerHate -= getRandom(10);
					}
					
					manageSkills(npc);
					startQuestTimer(EVENT_CHECK_ATTACK, CHECK_ATTACK_INTERVAL, npc, null);
				}
				break;
			}
			case EVENT_SPAWN_MINION:
			{
				if (npc == null)
				{
					break;
				}
				
				if ((_minionMultiplier > 1) && (_minionCount < (100 - (_minionMultiplier * 2))))
				{
					for (int i = 0; i < _minionMultiplier; i++)
					{
						addSpawn(ANTHARAS_BEHEMOTH_ID, npc, true);
						addSpawn(ANTHARAS_BEHEMOTH_ID, npc, true);
					}
					
					_minionCount += _minionMultiplier * 2;
				}
				else if (_minionCount < 98)
				{
					addSpawn(ANTHARAS_BEHEMOTH_ID, npc, true);
					addSpawn(ANTHARAS_BEHEMOTH_ID, npc, true);
					_minionCount += 2;
				}
				else if (_minionCount < 99)
				{
					addSpawn(getRandomBoolean() ? ANTHARAS_BEHEMOTH_ID : ANTHARAS_BOMBER_ID, npc, true);
					_minionCount++;
				}
				
				if ((getRandom(100) > 10) && (_minionMultiplier < 4))
				{
					_minionMultiplier++;
				}
				
				startQuestTimer(EVENT_SPAWN_MINION, MINION_SPAWN_INTERVAL, npc, null);
				break;
			}
			case EVENT_CLEAR_ZONE:
			{
				for (Creature creature : ANTHARAS_NEST_ZONE.getCharactersInside())
				{
					if (creature == null)
					{
						continue;
					}
					
					if (creature.isNpc())
					{
						creature.deleteMe();
					}
					else if (creature.isPlayer())
					{
						creature.teleToLocation(TELEPORT_EXIT_BASE_X + getRandom(600), TELEPORT_EXIT_BASE_Y + getRandom(1100), TELEPORT_EXIT_Z);
					}
				}
				break;
			}
			case EVENT_TID_USED_FEAR:
			{
				if ((npc != null) && (_sandStormState == 0))
				{
					_sandStormState = 1;
					npc.disableCoreAI(true);
					npc.getAI().setIntention(Intention.MOVE_TO, new Location(ANTHARAS_FEAR_MOVE_X, ANTHARAS_FEAR_MOVE_Y, ANTHARAS_FEAR_MOVE_Z));
					startQuestTimer(EVENT_TID_FEAR_MOVE_TIMEOVER, FEAR_MOVE_FIRST_CHECK, npc, null);
					startQuestTimer(EVENT_TID_FEAR_COOLTIME, FEAR_COOLDOWN, npc, null);
				}
				break;
			}
			case EVENT_TID_FEAR_COOLTIME:
			{
				_sandStormState = 0;
				break;
			}
			case EVENT_TID_FEAR_MOVE_TIMEOVER:
			{
				if (npc == null)
				{
					break;
				}
				
				if ((_sandStormState == 1) && (npc.getX() == ANTHARAS_FEAR_MOVE_X) && (npc.getY() == ANTHARAS_FEAR_MOVE_Y))
				{
					_sandStormState = 2;
					_moveRetryCount = 0;
					npc.disableCoreAI(false);
					INVISIBLE_NPC_SPAWN_LOCATIONS.entrySet().forEach(entry -> addSpawn(entry.getKey(), entry.getValue()));
				}
				else if (_sandStormState == 1)
				{
					if (_moveRetryCount <= MAX_MOVE_RETRY_COUNT)
					{
						_moveRetryCount++;
						npc.getAI().setIntention(Intention.MOVE_TO, new Location(ANTHARAS_FEAR_MOVE_X, ANTHARAS_FEAR_MOVE_Y, ANTHARAS_FEAR_MOVE_Z));
						startQuestTimer(EVENT_TID_FEAR_MOVE_TIMEOVER, FEAR_MOVE_RETRY_INTERVAL, npc, null);
					}
					else
					{
						npc.teleToLocation(ANTHARAS_FEAR_MOVE_X, ANTHARAS_FEAR_MOVE_Y, ANTHARAS_FEAR_MOVE_Z, npc.getHeading());
						startQuestTimer(EVENT_TID_FEAR_MOVE_TIMEOVER, FEAR_MOVE_FORCE_DELAY, npc, null);
					}
				}
				break;
			}
			case EVENT_CLEAR_STATUS:
			{
				_antharasBoss = (GrandBoss) addSpawn(ANTHARAS_NPC_ID, ANTHARAS_IDLE_LOCATION, false, 0);
				addBoss(_antharasBoss);
				Broadcast.toAllOnlinePlayers(new Earthquake(ANTHARAS_IDLE_LOCATION.getX(), ANTHARAS_IDLE_LOCATION.getY(), ANTHARAS_IDLE_LOCATION.getZ(), 20, 10));
				setStatus(STATUS_ALIVE);
				resetFightState();
				break;
			}
			case EVENT_SKIP_WAITING:
			{
				if (getStatus() == STATUS_WAITING)
				{
					cancelQuestTimer(EVENT_SPAWN_ANTHARAS, null, null);
					notifyEvent(EVENT_SPAWN_ANTHARAS, null, null);
					if (player != null)
					{
						player.sendMessage(getClass().getSimpleName() + ": Skipping waiting time ...");
					}
				}
				else if (player != null)
				{
					player.sendMessage(getClass().getSimpleName() + ": You can't skip waiting time right now!");
				}
				break;
			}
			case EVENT_RESPAWN_ANTHARAS:
			{
				if (getStatus() == STATUS_DEAD)
				{
					setRespawn(0);
					cancelQuestTimer(EVENT_CLEAR_STATUS, null, null);
					notifyEvent(EVENT_CLEAR_STATUS, null, null);
					if (player != null)
					{
						player.sendMessage(getClass().getSimpleName() + ": Antharas has been respawned.");
					}
				}
				else if (player != null)
				{
					player.sendMessage(getClass().getSimpleName() + ": You can't respawn antharas while antharas is alive!");
				}
				break;
			}
			case EVENT_DESPAWN_MINIONS:
			{
				if (getStatus() == STATUS_IN_FIGHT)
				{
					_minionCount = 0;
					for (Creature creature : ANTHARAS_NEST_ZONE.getCharactersInside())
					{
						if ((creature != null) && creature.isNpc() && ((creature.getId() == ANTHARAS_BEHEMOTH_ID) || (creature.getId() == ANTHARAS_BOMBER_ID)))
						{
							creature.deleteMe();
						}
					}
					
					if (player != null)
					{
						player.sendMessage(getClass().getSimpleName() + ": All minions have been deleted!");
					}
				}
				else if (player != null)
				{
					player.sendMessage(getClass().getSimpleName() + ": You can't despawn minions right now!");
				}
				break;
			}
			case EVENT_ABORT_FIGHT:
			{
				if (getStatus() == STATUS_IN_FIGHT)
				{
					setStatus(STATUS_ALIVE);
					cancelQuestTimer(EVENT_CHECK_ATTACK, _antharasBoss, null);
					cancelQuestTimer(EVENT_SPAWN_MINION, _antharasBoss, null);
					cancelQuestTimer(EVENT_SET_REGEN, _antharasBoss, null);
					cancelQuestTimer(EVENT_TID_USED_FEAR, _antharasBoss, null);
					cancelQuestTimer(EVENT_TID_FEAR_MOVE_TIMEOVER, _antharasBoss, null);
					cancelQuestTimer(EVENT_TID_FEAR_COOLTIME, _antharasBoss, null);
					
					for (Creature creature : ANTHARAS_NEST_ZONE.getCharactersInside())
					{
						if (creature == null)
						{
							continue;
						}
						
						if (creature.isNpc())
						{
							if (creature.getId() == ANTHARAS_NPC_ID)
							{
								creature.teleToLocation(ANTHARAS_IDLE_LOCATION);
							}
							else
							{
								creature.deleteMe();
							}
						}
						else if (creature.isPlayer() && !creature.isGM())
						{
							creature.teleToLocation(TELEPORT_EXIT_BASE_X + getRandom(600), TELEPORT_EXIT_BASE_Y + getRandom(1100), TELEPORT_EXIT_Z);
						}
					}
					
					resetFightState();
					if (player != null)
					{
						player.sendMessage(getClass().getSimpleName() + ": Fight has been aborted!");
					}
				}
				else if (player != null)
				{
					player.sendMessage(getClass().getSimpleName() + ": You can't abort fight right now!");
				}
				break;
			}
			case EVENT_MANAGE_SKILL:
			{
				if (npc != null)
				{
					manageSkills(npc);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles bomber aggro and AI behavior when entering aggro range.
	 */
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (npc.getId() != ANTHARAS_BOMBER_ID)
		{
			return;
		}
		
		npc.doCast(ANTHARAS_BOMBER_DISPEL.getSkill());
		npc.doDie(player);
	}
	
	/**
	 * Handles all attack events for Antharas and its minions.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final int npcId = npc.getId();
		if (npcId == ANTHARAS_BOMBER_ID)
		{
			if (npc.calculateDistance3D(attacker) < BOMBER_EXPLOSION_RANGE)
			{
				npc.doCast(ANTHARAS_BOMBER_DISPEL.getSkill());
				npc.doDie(attacker);
			}
		}
		else if (npcId == ANTHARAS_NPC_ID)
		{
			_lastAttackTime = System.currentTimeMillis();
			
			if (!ANTHARAS_NEST_ZONE.isCharacterInZone(attacker) || (getStatus() != STATUS_IN_FIGHT))
			{
				LOGGER.warning(getClass().getSimpleName() + ": Player " + attacker.getName() + " attacked Antharas in invalid conditions!");
				attacker.teleToLocation(80464, 152294, -3534);
			}
			
			if ((attacker.getMountType() == MountType.STRIDER) && !attacker.isAffectedBySkill(ANTHARAS_ANTI_STRIDER.getSkillId()) && npc.checkDoCastConditions(ANTHARAS_ANTI_STRIDER.getSkill()))
			{
				addSkillCastDesire(npc, attacker, ANTHARAS_ANTI_STRIDER.getSkill(), 100);
			}
			
			if (skill == null)
			{
				refreshAiParams(attacker, damage * 1000);
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.25))
			{
				refreshAiParams(attacker, (damage / 3) * 100);
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.5))
			{
				refreshAiParams(attacker, damage * 20);
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.75))
			{
				refreshAiParams(attacker, damage * 10);
			}
			else
			{
				refreshAiParams(attacker, (damage / 3) * 20);
			}
			
			manageSkills(npc);
		}
	}
	
	/**
	 * Handles Antharas and minion death.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == ANTHARAS_NPC_ID)
		{
			if ((killer == null) || !ANTHARAS_NEST_ZONE.isCharacterInZone(killer))
			{
				LOGGER.warning(getClass().getSimpleName() + ": Antharas was killed by " + ((killer != null) ? killer.getName() : "unknown") + " outside of nest zone. Forcing death handling.");
			}
			
			_antharasBoss = null;
			notifyEvent(EVENT_DESPAWN_MINIONS, null, null);
			ANTHARAS_NEST_ZONE.broadcastPacket(new SpecialCamera(npc, 1200, 20, -10, 0, 10000, 13000, 0, 0, 0, 0, 0));
			ANTHARAS_NEST_ZONE.broadcastPacket(new PlaySound("BS01_D"));
			addSpawn(ANTHARAS_TELEPORT_CUBE_ID, 177615, 114941, -7709, 0, false, TELEPORT_CUBE_DURATION);
			
			final long baseIntervalMillis = GrandBossConfig.ANTHARAS_SPAWN_INTERVAL * 3600000L;
			final long randomRangeMillis = GrandBossConfig.ANTHARAS_SPAWN_RANDOM * 3600000L;
			final long respawnDelay = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			setRespawn(respawnDelay);
			startQuestTimer(EVENT_CLEAR_STATUS, respawnDelay, null, null);
			cancelQuestTimer(EVENT_SET_REGEN, npc, null);
			cancelQuestTimer(EVENT_CHECK_ATTACK, npc, null);
			cancelQuestTimer(EVENT_SPAWN_MINION, npc, null);
			startQuestTimer(EVENT_CLEAR_ZONE, TELEPORT_CUBE_DURATION, null, null);
			setStatus(STATUS_DEAD);
			
			final long nextRespawnTime = System.currentTimeMillis() + respawnDelay;
			LOGGER.info("Antharas will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			resetFightState();
		}
		else if ((npcId == ANTHARAS_BEHEMOTH_ID) && (killer != null) && ANTHARAS_NEST_ZONE.isCharacterInZone(killer))
		{
			_minionCount--;
		}
	}
	
	/**
	 * Handles bomber movement completion (self-destruction).
	 */
	@Override
	public void onMoveFinished(Npc npc)
	{
		npc.doCast(ANTHARAS_BOMBER_DISPEL.getSkill());
		npc.doDie(null);
	}
	
	/**
	 * Handles spawn initialization for Antharas and invisible fear-phase triggers.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == ANTHARAS_NPC_ID)
		{
			final Attackable antharasAttackable = npc.asAttackable();
			antharasAttackable.setCanReturnToSpawnPoint(false);
			npc.setRandomWalking(false);
			
			cancelQuestTimer(EVENT_SET_REGEN, npc, null);
			startQuestTimer(EVENT_SET_REGEN, REGEN_INTERVAL, npc, null);
			antharasAttackable.setOnKillDelay(0);
		}
		else
		{
			for (int i = 1; i <= 6; i++)
			{
				final int suicideX = npc.getTemplate().getParameters().getInt("suicide" + i + "_x");
				final int suicideY = npc.getTemplate().getParameters().getInt("suicide" + i + "_y");
				final Attackable bomber = addSpawn(ANTHARAS_BOMBER_ID, npc.getX(), npc.getY(), npc.getZ(), 0, true, 15000, true).asAttackable();
				bomber.getAI().setIntention(Intention.MOVE_TO, new Location(suicideX, suicideY, npc.getZ()));
			}
			
			npc.deleteMe();
		}
	}
	
	/**
	 * Handles completion of Antharas spells to chain follow-up AI events.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (skill != null)
		{
			final int skillId = skill.getId();
			if ((skillId == ANTHARAS_FEAR.getSkillId()) || (skillId == ANTHARAS_FEAR_SHORT.getSkillId()))
			{
				startQuestTimer(EVENT_TID_USED_FEAR, FEAR_USED_DELAY, npc, null);
			}
		}
		
		startQuestTimer(EVENT_MANAGE_SKILL, 1000, npc, null);
	}
	
	/**
	 * Cleans Antharas instance on unload.
	 */
	@Override
	public void unload(boolean removeFromList)
	{
		if (_antharasBoss != null)
		{
			_antharasBoss.deleteMe();
			_antharasBoss = null;
		}
		
		super.unload(removeFromList);
	}
	
	private int getStatus()
	{
		return GrandBossManager.getInstance().getStatus(ANTHARAS_NPC_ID);
	}
	
	private void addBoss(GrandBoss grandBoss)
	{
		GrandBossManager.getInstance().addBoss(grandBoss);
	}
	
	private void setStatus(int status)
	{
		GrandBossManager.getInstance().setStatus(ANTHARAS_NPC_ID, status);
	}
	
	private void setRespawn(long respawnTime)
	{
		GrandBossManager.getInstance().getStatSet(ANTHARAS_NPC_ID).set("respawn_time", System.currentTimeMillis() + respawnTime);
	}
	
	private void resetFightState()
	{
		_lastAttackTime = 0;
		_minionCount = 0;
		_minionMultiplier = 0;
		_moveRetryCount = 0;
		_sandStormState = 0;
		_primaryAttacker = null;
		_secondaryAttacker = null;
		_tertiaryAttacker = null;
		_primaryAttackerHate = 0;
		_secondaryAttackerHate = 0;
		_tertiaryAttackerHate = 0;
	}
	
	private void refreshAiParams(Player attacker, int damage)
	{
		if ((_primaryAttacker != null) && (attacker == _primaryAttacker))
		{
			if (_primaryAttackerHate < (damage + 1000))
			{
				_primaryAttackerHate = damage + getRandom(3000);
			}
		}
		else if ((_secondaryAttacker != null) && (attacker == _secondaryAttacker))
		{
			if (_secondaryAttackerHate < (damage + 1000))
			{
				_secondaryAttackerHate = damage + getRandom(3000);
			}
		}
		else if ((_tertiaryAttacker != null) && (attacker == _tertiaryAttacker))
		{
			if (_tertiaryAttackerHate < (damage + 1000))
			{
				_tertiaryAttackerHate = damage + getRandom(3000);
			}
		}
		else
		{
			final int minimumHate = MathUtil.min(_primaryAttackerHate, _secondaryAttackerHate, _tertiaryAttackerHate);
			if (_primaryAttackerHate == minimumHate)
			{
				_primaryAttackerHate = damage + getRandom(3000);
				_primaryAttacker = attacker;
			}
			else if (_secondaryAttackerHate == minimumHate)
			{
				_secondaryAttackerHate = damage + getRandom(3000);
				_secondaryAttacker = attacker;
			}
			else if (_tertiaryAttackerHate == minimumHate)
			{
				_tertiaryAttackerHate = damage + getRandom(3000);
				_tertiaryAttacker = attacker;
			}
		}
	}
	
	private void manageSkills(Npc npc)
	{
		if (npc.isCastingNow() || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		Player selectedTarget = null;
		int selectedSlot = 0;
		int selectedHate = 0;
		
		if ((_primaryAttacker == null) || (npc.calculateDistance3D(_primaryAttacker) > MAX_THREAT_DISTANCE) || _primaryAttacker.isDead())
		{
			_primaryAttackerHate = 0;
		}
		
		if ((_secondaryAttacker == null) || (npc.calculateDistance3D(_secondaryAttacker) > MAX_THREAT_DISTANCE) || _secondaryAttacker.isDead())
		{
			_secondaryAttackerHate = 0;
		}
		
		if ((_tertiaryAttacker == null) || (npc.calculateDistance3D(_tertiaryAttacker) > MAX_THREAT_DISTANCE) || _tertiaryAttacker.isDead())
		{
			_tertiaryAttackerHate = 0;
		}
		
		if (_primaryAttackerHate > _secondaryAttackerHate)
		{
			selectedSlot = 2;
			selectedHate = _primaryAttackerHate;
			selectedTarget = _primaryAttacker;
		}
		else if (_secondaryAttackerHate > 0)
		{
			selectedSlot = 3;
			selectedHate = _secondaryAttackerHate;
			selectedTarget = _secondaryAttacker;
		}
		
		if (_tertiaryAttackerHate > selectedHate)
		{
			selectedSlot = 4;
			selectedHate = _tertiaryAttackerHate;
			selectedTarget = _tertiaryAttacker;
		}
		
		if (selectedHate > 0)
		{
			if (getRandom(100) < 70)
			{
				switch (selectedSlot)
				{
					case 2:
					{
						_primaryAttackerHate = 500;
						break;
					}
					case 3:
					{
						_secondaryAttackerHate = 500;
						break;
					}
					case 4:
					{
						_tertiaryAttackerHate = 500;
						break;
					}
				}
			}
			
			final double distanceToTarget = npc.calculateDistance3D(selectedTarget);
			final double directionToTarget = npc.calculateDirectionTo(selectedTarget);
			SkillHolder skillToCast = null;
			boolean castOnTarget = false;
			
			if (npc.getCurrentHp() < (npc.getMaxHp() * 0.25))
			{
				if (getRandom(100) < 30)
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_MOUTH_ATTACK;
				}
				else if ((getRandom(100) < 80) && (((distanceToTarget < 1423) && (directionToTarget < 188) && (directionToTarget > 172)) || ((distanceToTarget < 802) && (directionToTarget < 194) && (directionToTarget > 166))))
				{
					skillToCast = ANTHARAS_TAIL_ATTACK;
				}
				else if ((getRandom(100) < 40) && (((distanceToTarget < 850) && (directionToTarget < 210) && (directionToTarget > 150)) || ((distanceToTarget < 425) && (directionToTarget < 270) && (directionToTarget > 90))))
				{
					skillToCast = ANTHARAS_DEBUFF;
				}
				else if ((getRandom(100) < 10) && (distanceToTarget < 1100))
				{
					skillToCast = ANTHARAS_JUMP;
				}
				else if (getRandom(100) < 10)
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_METEOR;
				}
				else if (getRandom(100) < 6)
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_BREATH_ATTACK;
				}
				else if (getRandomBoolean())
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_NORMAL_ATTACK_EX;
				}
				else if (getRandom(100) < 5)
				{
					castOnTarget = true;
					skillToCast = getRandomBoolean() ? ANTHARAS_FEAR : ANTHARAS_FEAR_SHORT;
				}
				else
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_NORMAL_ATTACK;
				}
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.5))
			{
				if ((getRandom(100) < 80) && (((distanceToTarget < 1423) && (directionToTarget < 188) && (directionToTarget > 172)) || ((distanceToTarget < 802) && (directionToTarget < 194) && (directionToTarget > 166))))
				{
					skillToCast = ANTHARAS_TAIL_ATTACK;
				}
				else if ((getRandom(100) < 40) && (((distanceToTarget < 850) && (directionToTarget < 210) && (directionToTarget > 150)) || ((distanceToTarget < 425) && (directionToTarget < 270) && (directionToTarget > 90))))
				{
					skillToCast = ANTHARAS_DEBUFF;
				}
				else if ((getRandom(100) < 10) && (distanceToTarget < 1100))
				{
					skillToCast = ANTHARAS_JUMP;
				}
				else if (getRandom(100) < 7)
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_METEOR;
				}
				else if (getRandom(100) < 6)
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_BREATH_ATTACK;
				}
				else if (getRandomBoolean())
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_NORMAL_ATTACK_EX;
				}
				else if (getRandom(100) < 5)
				{
					castOnTarget = true;
					skillToCast = getRandomBoolean() ? ANTHARAS_FEAR : ANTHARAS_FEAR_SHORT;
				}
				else
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_NORMAL_ATTACK;
				}
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.75))
			{
				if ((getRandom(100) < 80) && (((distanceToTarget < 1423) && (directionToTarget < 188) && (directionToTarget > 172)) || ((distanceToTarget < 802) && (directionToTarget < 194) && (directionToTarget > 166))))
				{
					skillToCast = ANTHARAS_TAIL_ATTACK;
				}
				else if ((getRandom(100) < 10) && (distanceToTarget < 1100))
				{
					skillToCast = ANTHARAS_JUMP;
				}
				else if (getRandom(100) < 5)
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_METEOR;
				}
				else if (getRandom(100) < 6)
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_BREATH_ATTACK;
				}
				else if (getRandomBoolean())
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_NORMAL_ATTACK_EX;
				}
				else if (getRandom(100) < 5)
				{
					castOnTarget = true;
					skillToCast = getRandomBoolean() ? ANTHARAS_FEAR : ANTHARAS_FEAR_SHORT;
				}
				else
				{
					castOnTarget = true;
					skillToCast = ANTHARAS_NORMAL_ATTACK;
				}
			}
			else if ((getRandom(100) < 80) && (((distanceToTarget < 1423) && (directionToTarget < 188) && (directionToTarget > 172)) || ((distanceToTarget < 802) && (directionToTarget < 194) && (directionToTarget > 166))))
			{
				skillToCast = ANTHARAS_TAIL_ATTACK;
			}
			else if (getRandom(100) < 3)
			{
				castOnTarget = true;
				skillToCast = ANTHARAS_METEOR;
			}
			else if (getRandom(100) < 6)
			{
				castOnTarget = true;
				skillToCast = ANTHARAS_BREATH_ATTACK;
			}
			else if (getRandomBoolean())
			{
				castOnTarget = true;
				skillToCast = ANTHARAS_NORMAL_ATTACK_EX;
			}
			else if (getRandom(100) < 5)
			{
				castOnTarget = true;
				skillToCast = getRandomBoolean() ? ANTHARAS_FEAR : ANTHARAS_FEAR_SHORT;
			}
			else
			{
				castOnTarget = true;
				skillToCast = ANTHARAS_NORMAL_ATTACK;
			}
			
			if ((skillToCast != null) && npc.checkDoCastConditions(skillToCast.getSkill()))
			{
				if (castOnTarget)
				{
					addSkillCastDesire(npc, selectedTarget, skillToCast.getSkill(), 100);
				}
				else
				{
					npc.getAI().setIntention(Intention.CAST, skillToCast.getSkill(), npc);
				}
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new Antharas();
	}
}
