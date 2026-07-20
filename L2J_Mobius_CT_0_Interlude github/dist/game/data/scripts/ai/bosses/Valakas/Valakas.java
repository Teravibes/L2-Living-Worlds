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
package ai.bosses.Valakas;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Valakas grand boss AI handler.<br>
 * Controls Fire Dragon spawn, lair access, cinematics and combat AI.
 * <ul>
 * <li>Restores persisted Valakas state and schedules respawn.</li>
 * <li>Manages lair zone, entry window, inactivity reset and player ejection.</li>
 * <li>Implements HP-based regeneration, Lava Skin and skill rotation logic.</li>
 * </ul>
 * @author BazookaRpm
 */
public class Valakas extends Script
{
	// Logger.
	private static final Logger LOGGER = Logger.getLogger(Valakas.class.getName());
	
	// NPCs.
	private static final int VALAKAS = 29028;
	private static final int TELEPORT_CUBE = 31759;
	
	// Skills.
	private static final int SKILL_REGEN_ID = 4691;
	private static final SkillHolder SKILL_LAVA_SKIN = new SkillHolder(4680, 1);
	private static final SkillHolder SKILL_ANTI_STRIDER = new SkillHolder(4258, 1);
	
	private static final SkillHolder[] SKILL_TABLE_REGULAR =
	{
		new SkillHolder(4681, 1), // Valakas Trample.
		new SkillHolder(4682, 1), // Valakas Trample.
		new SkillHolder(4683, 1), // Valakas Dragon Breath.
		new SkillHolder(4689, 1), // Valakas Fear (level 1 used only).
	};
	
	private static final SkillHolder[] SKILL_TABLE_LOW_HP =
	{
		new SkillHolder(4681, 1), // Valakas Trample.
		new SkillHolder(4682, 1), // Valakas Trample.
		new SkillHolder(4683, 1), // Valakas Dragon Breath.
		new SkillHolder(4689, 1), // Valakas Fear (level 1 used only).
		new SkillHolder(4690, 1), // Valakas Meteor Storm.
	};
	
	private static final SkillHolder[] SKILL_TABLE_AOE =
	{
		new SkillHolder(4683, 1), // Valakas Dragon Breath.
		new SkillHolder(4684, 1), // Valakas Dragon Breath.
		new SkillHolder(4685, 1), // Valakas Tail Stomp.
		new SkillHolder(4686, 1), // Valakas Tail Stomp.
		new SkillHolder(4688, 1), // Valakas Stun.
		new SkillHolder(4689, 1), // Valakas Fear (level 1 used only).
		new SkillHolder(4690, 1), // Valakas Meteor Storm.
	};
	
	// Status.
	private static final byte STATUS_DORMANT = 0; // Spawned, no one inside, entry open.
	private static final byte STATUS_WAITING = 1; // Player entered, entry open, cinematic pending.
	private static final byte STATUS_FIGHTING = 2; // In combat, entry locked.
	private static final byte STATUS_DEAD = 3; // Dead, entry locked.
	
	// Zones.
	private static final int ZONE_ID_LAIR = 12010;
	private static final int ZONE_ID_GROUND = 13010;
	private static final BossZone ZONE_LAIR = ZoneManager.getInstance().getZoneById(ZONE_ID_LAIR, BossZone.class);
	private static final NoRestartZone ZONE_GROUND = ZoneManager.getInstance().getZoneById(ZONE_ID_GROUND, NoRestartZone.class);
	
	// Locations.
	private static final Location[] TELEPORT_CUBE_LOCATIONS =
	{
		new Location(214880, -116144, -1644),
		new Location(213696, -116592, -1644),
		new Location(212112, -116688, -1644),
		new Location(211184, -115472, -1664),
		new Location(210336, -114592, -1644),
		new Location(211360, -113904, -1644),
		new Location(213152, -112352, -1644),
		new Location(214032, -113232, -1644),
		new Location(214752, -114592, -1644),
		new Location(209824, -115568, -1421),
		new Location(210528, -112192, -1403),
		new Location(213120, -111136, -1408),
		new Location(215184, -111504, -1392),
		new Location(215456, -117328, -1392),
		new Location(213200, -118160, -1424)
	};
	
	private static final Location LOC_VALAKAS_HIDE = new Location(220963, -104895, -1620);
	private static final Location LOC_EJECT_ATTACKER = new Location(150037, -57255, -2976);
	private static final Location LOC_VALAKAS_LAIR = new Location(212852, -114842, -1632);
	private static final Location LOC_VALAKAS_REGEN = new Location(-105200, -253104, -15264);
	
	// Events.
	private static final String EVT_START = "VALAKAS_START_ENCOUNTER";
	private static final String EVT_REGEN_TICK = "VALAKAS_REGEN_TICK";
	private static final String EVT_SKILL_TICK = "VALAKAS_SKILL_TICK";
	private static final String EVT_BROADCAST_SPAWN = "VALAKAS_BROADCAST_SPAWN";
	private static final String EVT_UNLOCK = "VALAKAS_UNLOCK";
	private static final String EVT_REMOVE_PLAYERS = "VALAKAS_REMOVE_PLAYERS";
	private static final String EVT_SPAWN_STEP_1 = "VALAKAS_SPAWN_1";
	private static final String EVT_SPAWN_STEP_2 = "VALAKAS_SPAWN_2";
	private static final String EVT_SPAWN_STEP_3 = "VALAKAS_SPAWN_3";
	private static final String EVT_SPAWN_STEP_4 = "VALAKAS_SPAWN_4";
	private static final String EVT_SPAWN_STEP_5 = "VALAKAS_SPAWN_5";
	private static final String EVT_SPAWN_STEP_6 = "VALAKAS_SPAWN_6";
	private static final String EVT_SPAWN_STEP_7 = "VALAKAS_SPAWN_7";
	private static final String EVT_SPAWN_STEP_8 = "VALAKAS_SPAWN_8";
	private static final String EVT_SPAWN_STEP_9 = "VALAKAS_SPAWN_9";
	private static final String EVT_SPAWN_STEP_10 = "VALAKAS_SPAWN_10";
	private static final String EVT_DIE_STEP_1 = "VALAKAS_DIE_1";
	private static final String EVT_DIE_STEP_2 = "VALAKAS_DIE_2";
	private static final String EVT_DIE_STEP_3 = "VALAKAS_DIE_3";
	private static final String EVT_DIE_STEP_4 = "VALAKAS_DIE_4";
	private static final String EVT_DIE_STEP_5 = "VALAKAS_DIE_5";
	private static final String EVT_DIE_STEP_6 = "VALAKAS_DIE_6";
	private static final String EVT_DIE_STEP_7 = "VALAKAS_DIE_7";
	private static final String EVT_DIE_STEP_8 = "VALAKAS_DIE_8";
	
	// Timings.
	private static final long MILLIS_PER_HOUR = 3600000L;
	private static final long INACTIVITY_TIMEOUT = 900000L; // 15 minutes.
	private static final long REGEN_TICK_DELAY = 60000L;
	private static final long SKILL_TICK_DELAY = 2000L;
	private static final long TELEPORT_CUBE_LIFETIME = 900000L;
	private static final long BROADCAST_SPAWN_DELAY = 100L;
	private static final long SPAWN_STEP_1_DELAY = 1700L;
	private static final long SPAWN_STEP_2_DELAY = 3200L;
	private static final long SPAWN_STEP_3_DELAY = 6500L;
	private static final long SPAWN_STEP_4_DELAY = 9400L;
	private static final long SPAWN_STEP_5_DELAY = 12100L;
	private static final long SPAWN_STEP_6_DELAY = 12430L;
	private static final long SPAWN_STEP_7_DELAY = 15430L;
	private static final long SPAWN_STEP_8_DELAY = 16830L;
	private static final long SPAWN_STEP_9_DELAY = 23530L;
	private static final long SPAWN_STEP_10_DELAY = 26000L;
	private static final long DIE_STEP_1_DELAY = 300L;
	private static final long DIE_STEP_2_DELAY = 600L;
	private static final long DIE_STEP_3_DELAY = 3800L;
	private static final long DIE_STEP_4_DELAY = 8200L;
	private static final long DIE_STEP_5_DELAY = 8700L;
	private static final long DIE_STEP_6_DELAY = 13300L;
	private static final long DIE_STEP_7_DELAY = 14000L;
	private static final long DIE_STEP_8_DELAY = 16500L;
	
	// Runtime state.
	private GrandBoss _valakas;
	private Playable _currentTarget;
	private long _lastAttackTime;
	
	private Valakas()
	{
		addAttackId(VALAKAS);
		addKillId(VALAKAS);
		addSpawnId(VALAKAS);
		addSpellFinishedId(VALAKAS);
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		final StatSet info = bossManager.getStatSet(VALAKAS);
		final int status = bossManager.getStatus(VALAKAS);
		
		if (status == STATUS_DEAD)
		{
			final long respawnTime = info.getLong("respawn_time");
			final long remaining = respawnTime - System.currentTimeMillis();
			if (remaining > 0)
			{
				startQuestTimer(EVT_UNLOCK, remaining, null, null);
			}
			else
			{
				spawnDormantValakas();
			}
		}
		else
		{
			final int locX = info.getInt("loc_x");
			final int locY = info.getInt("loc_y");
			final int locZ = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double currentHp = info.getDouble("currentHP");
			final double currentMp = info.getDouble("currentMP");
			
			_valakas = (GrandBoss) addSpawn(VALAKAS, locX, locY, locZ, heading, false, 0);
			bossManager.addBoss(_valakas);
			_valakas.setCurrentHpMp(currentHp, currentMp);
			_valakas.setRunning();
			
			if (status == STATUS_FIGHTING)
			{
				_lastAttackTime = System.currentTimeMillis();
				startQuestTimer(EVT_REGEN_TICK, REGEN_TICK_DELAY, _valakas, null, true);
				startQuestTimer(EVT_SKILL_TICK, SKILL_TICK_DELAY, _valakas, null, true);
			}
			else
			{
				_valakas.teleToLocation(LOC_VALAKAS_HIDE);
				_valakas.setInvul(true);
				_valakas.getAI().setIntention(Intention.IDLE);
				
				if (status == STATUS_WAITING)
				{
					final long waitMillis = GrandBossConfig.VALAKAS_WAIT_TIME * 60000L;
					startQuestTimer(EVT_START, waitMillis, _valakas, null);
				}
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event == null)
		{
			return super.onEvent(null, npc, player);
		}
		
		if (npc != null)
		{
			switch (event)
			{
				case EVT_START:
				{
					handleEncounterStart(npc);
					break;
				}
				case EVT_REGEN_TICK:
				{
					handleRegenTick(npc);
					break;
				}
				case EVT_SKILL_TICK:
				{
					handleSkillTick(npc);
					break;
				}
				case EVT_BROADCAST_SPAWN:
				{
					broadcastSpawnIntro(npc);
					break;
				}
				case EVT_SPAWN_STEP_1:
				{
					handleSpawnStep1(npc);
					break;
				}
				case EVT_SPAWN_STEP_2:
				{
					handleSpawnStep2(npc);
					break;
				}
				case EVT_SPAWN_STEP_3:
				{
					handleSpawnStep3(npc);
					break;
				}
				case EVT_SPAWN_STEP_4:
				{
					handleSpawnStep4(npc);
					break;
				}
				case EVT_SPAWN_STEP_5:
				{
					handleSpawnStep5(npc);
					break;
				}
				case EVT_SPAWN_STEP_6:
				{
					handleSpawnStep6(npc);
					break;
				}
				case EVT_SPAWN_STEP_7:
				{
					handleSpawnStep7(npc);
					break;
				}
				case EVT_SPAWN_STEP_8:
				{
					handleSpawnStep8(npc);
					break;
				}
				case EVT_SPAWN_STEP_9:
				{
					handleSpawnStep9(npc);
					break;
				}
				case EVT_SPAWN_STEP_10:
				{
					handleSpawnStep10(npc);
					break;
				}
				case EVT_DIE_STEP_1:
				{
					handleDieStep1(npc);
					break;
				}
				case EVT_DIE_STEP_2:
				{
					handleDieStep2(npc);
					break;
				}
				case EVT_DIE_STEP_3:
				{
					handleDieStep3(npc);
					break;
				}
				case EVT_DIE_STEP_4:
				{
					handleDieStep4(npc);
					break;
				}
				case EVT_DIE_STEP_5:
				{
					handleDieStep5(npc);
					break;
				}
				case EVT_DIE_STEP_6:
				{
					handleDieStep6(npc);
					break;
				}
				case EVT_DIE_STEP_7:
				{
					handleDieStep7(npc);
					break;
				}
				case EVT_DIE_STEP_8:
				{
					handleDieStep8(npc);
					break;
				}
				default:
				{
					break;
				}
			}
		}
		else
		{
			switch (event)
			{
				case EVT_UNLOCK:
				{
					spawnDormantValakas();
					break;
				}
				case EVT_REMOVE_PLAYERS:
				{
					oustPlayersFromLair();
					break;
				}
				default:
				{
					break;
				}
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc == null)
		{
			return;
		}
		
		npc.asAttackable().setCanReturnToSpawnPoint(false);
		npc.setRandomWalking(false);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((npc == null) || (attacker == null))
		{
			return;
		}
		
		if ((ZONE_LAIR != null) && !ZONE_LAIR.isInsideZone(attacker))
		{
			attacker.doDie(attacker);
			return;
		}
		
		if (npc.isInvul())
		{
			return;
		}
		
		if (GrandBossManager.getInstance().getStatus(VALAKAS) != STATUS_FIGHTING)
		{
			attacker.teleToLocation(LOC_EJECT_ATTACKER);
			return;
		}
		
		if ((attacker.getMountType() == MountType.STRIDER) && !attacker.isAffectedBySkill(SKILL_ANTI_STRIDER.getSkillId()))
		{
			npc.setTarget(attacker);
			npc.doCast(SKILL_ANTI_STRIDER.getSkill());
		}
		
		_lastAttackTime = System.currentTimeMillis();
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc == null)
		{
			return;
		}
		
		cancelQuestTimer(EVT_REGEN_TICK, npc, null);
		cancelQuestTimer(EVT_SKILL_TICK, npc, null);
		
		if (ZONE_LAIR != null)
		{
			ZONE_LAIR.broadcastPacket(new PlaySound(1, "B03_D", 0, 0, 0, 0, 0));
			ZONE_LAIR.broadcastPacket(new SpecialCamera(npc, 1200, 20, -10, 0, 10000, 13000, 0, 0, 0, 0, 0));
		}
		
		startQuestTimer(EVT_DIE_STEP_1, DIE_STEP_1_DELAY, npc, null);
		startQuestTimer(EVT_DIE_STEP_2, DIE_STEP_2_DELAY, npc, null);
		startQuestTimer(EVT_DIE_STEP_3, DIE_STEP_3_DELAY, npc, null);
		startQuestTimer(EVT_DIE_STEP_4, DIE_STEP_4_DELAY, npc, null);
		startQuestTimer(EVT_DIE_STEP_5, DIE_STEP_5_DELAY, npc, null);
		startQuestTimer(EVT_DIE_STEP_6, DIE_STEP_6_DELAY, npc, null);
		startQuestTimer(EVT_DIE_STEP_7, DIE_STEP_7_DELAY, npc, null);
		startQuestTimer(EVT_DIE_STEP_8, DIE_STEP_8_DELAY, npc, null);
		
		GrandBossManager.getInstance().setStatus(VALAKAS, STATUS_DEAD);
		
		final long baseIntervalMillis = GrandBossConfig.VALAKAS_SPAWN_INTERVAL * MILLIS_PER_HOUR;
		final long randomRangeMillis = GrandBossConfig.VALAKAS_SPAWN_RANDOM * MILLIS_PER_HOUR;
		final long respawnDelay = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
		final long nextRespawnTime = System.currentTimeMillis() + respawnDelay;
		
		LOGGER.info("Valakas will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
		
		startQuestTimer(EVT_UNLOCK, respawnDelay, null, null);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(VALAKAS);
		info.set("respawn_time", nextRespawnTime);
		GrandBossManager.getInstance().setStatSet(VALAKAS, info);
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (npc == null)
		{
			return;
		}
		
		startQuestTimer(EVT_SKILL_TICK, 1000L, npc, null);
		
		if ((ZONE_GROUND != null) && !ZONE_GROUND.isCharacterInZone(npc) && (_valakas != null))
		{
			_valakas.teleToLocation(LOC_VALAKAS_LAIR);
		}
	}
	
	/**
	 * Spawns dormant Valakas and initializes boss status to DORMANT.
	 */
	private void spawnDormantValakas()
	{
		_valakas = (GrandBoss) addSpawn(VALAKAS, LOC_VALAKAS_REGEN, false, 0);
		_valakas.teleToLocation(LOC_VALAKAS_HIDE);
		_valakas.setInvul(true);
		_valakas.setRunning();
		_valakas.getAI().setIntention(Intention.IDLE);
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		bossManager.addBoss(_valakas);
		bossManager.setStatus(VALAKAS, STATUS_DORMANT);
	}
	
	/**
	 * Starts encounter: moves Valakas into lair, runs intro cinematic and schedules AI tasks.
	 * @param npc Valakas instance.
	 */
	private void handleEncounterStart(Npc npc)
	{
		_lastAttackTime = System.currentTimeMillis();
		npc.teleToLocation(LOC_VALAKAS_LAIR);
		
		startQuestTimer(EVT_BROADCAST_SPAWN, BROADCAST_SPAWN_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_1, SPAWN_STEP_1_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_2, SPAWN_STEP_2_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_3, SPAWN_STEP_3_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_4, SPAWN_STEP_4_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_5, SPAWN_STEP_5_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_6, SPAWN_STEP_6_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_7, SPAWN_STEP_7_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_8, SPAWN_STEP_8_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_9, SPAWN_STEP_9_DELAY, npc, null);
		startQuestTimer(EVT_SPAWN_STEP_10, SPAWN_STEP_10_DELAY, npc, null);
	}
	
	/**
	 * Regeneration task and inactivity handling.
	 * @param npc Valakas instance.
	 */
	private void handleRegenTick(Npc npc)
	{
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		if ((bossManager.getStatus(VALAKAS) == STATUS_FIGHTING) && ((_lastAttackTime + INACTIVITY_TIMEOUT) < System.currentTimeMillis()))
		{
			npc.getAI().setIntention(Intention.IDLE);
			npc.teleToLocation(LOC_VALAKAS_REGEN);
			bossManager.setStatus(VALAKAS, STATUS_DORMANT);
			npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
			
			if (ZONE_LAIR != null)
			{
				ZONE_LAIR.oustAllPlayers();
			}
			
			cancelQuestTimer(EVT_REGEN_TICK, npc, null);
			cancelQuestTimer(EVT_SKILL_TICK, npc, null);
			return;
		}
		
		final BuffInfo effectInfo = npc.getEffectList().getBuffInfoBySkillId(SKILL_REGEN_ID);
		final int currentRegenLevel = (effectInfo != null) ? effectInfo.getSkill().getLevel() : 0;
		
		final double maxHp = npc.getMaxHp();
		final double currentHp = npc.getCurrentHp();
		
		if ((currentHp < (maxHp / 4.0)) && (currentRegenLevel != 4))
		{
			npc.setTarget(npc);
			npc.doCast(SkillData.getInstance().getSkill(SKILL_REGEN_ID, 4));
		}
		else if ((currentHp < ((maxHp * 2.0) / 4.0)) && (currentRegenLevel != 3))
		{
			npc.setTarget(npc);
			npc.doCast(SkillData.getInstance().getSkill(SKILL_REGEN_ID, 3));
		}
		else if ((currentHp < ((maxHp * 3.0) / 4.0)) && (currentRegenLevel != 2))
		{
			npc.setTarget(npc);
			npc.doCast(SkillData.getInstance().getSkill(SKILL_REGEN_ID, 2));
		}
		else if (currentRegenLevel != 1)
		{
			npc.setTarget(npc);
			npc.doCast(SkillData.getInstance().getSkill(SKILL_REGEN_ID, 1));
		}
	}
	
	/**
	 * Schedules skill AI evaluation.
	 * @param npc Valakas instance.
	 */
	private void handleSkillTick(Npc npc)
	{
		callSkillAI(npc);
	}
	
	/**
	 * Plays spawn sound and social action for all players inside the lair.
	 * @param npc Valakas instance.
	 */
	private void broadcastSpawnIntro(Npc npc)
	{
		if (ZONE_LAIR == null)
		{
			return;
		}
		
		for (Player player : ZONE_LAIR.getPlayersInside())
		{
			player.sendPacket(new PlaySound(1, "BS03_A", 0, 0, 0, 0, 0));
			player.sendPacket(new SocialAction(npc.getObjectId(), 3));
		}
	}
	
	private void handleSpawnStep1(Npc npc)
	{
		broadcastCamera(npc, 1800, 180, -1, 1500, 15000, 10000, 0, 0, 1, 0, 0);
	}
	
	private void handleSpawnStep2(Npc npc)
	{
		broadcastCamera(npc, 1300, 180, -5, 3000, 15000, 10000, 0, -5, 1, 0, 0);
	}
	
	private void handleSpawnStep3(Npc npc)
	{
		broadcastCamera(npc, 500, 180, -8, 600, 15000, 10000, 0, 60, 1, 0, 0);
	}
	
	private void handleSpawnStep4(Npc npc)
	{
		broadcastCamera(npc, 800, 180, -8, 2700, 15000, 10000, 0, 30, 1, 0, 0);
	}
	
	private void handleSpawnStep5(Npc npc)
	{
		broadcastCamera(npc, 200, 250, 70, 0, 15000, 10000, 30, 80, 1, 0, 0);
	}
	
	private void handleSpawnStep6(Npc npc)
	{
		broadcastCamera(npc, 1100, 250, 70, 2500, 15000, 10000, 30, 80, 1, 0, 0);
	}
	
	private void handleSpawnStep7(Npc npc)
	{
		broadcastCamera(npc, 700, 150, 30, 0, 15000, 10000, -10, 60, 1, 0, 0);
	}
	
	private void handleSpawnStep8(Npc npc)
	{
		broadcastCamera(npc, 1200, 150, 20, 2900, 15000, 10000, -10, 30, 1, 0, 0);
	}
	
	private void handleSpawnStep9(Npc npc)
	{
		broadcastCamera(npc, 750, 170, -10, 3400, 15000, 4000, 10, -15, 1, 0, 0);
	}
	
	private void handleSpawnStep10(Npc npc)
	{
		GrandBossManager.getInstance().setStatus(VALAKAS, STATUS_FIGHTING);
		npc.setInvul(false);
		
		startQuestTimer(EVT_REGEN_TICK, REGEN_TICK_DELAY, npc, null, true);
		startQuestTimer(EVT_SKILL_TICK, SKILL_TICK_DELAY, npc, null, true);
		
		if ((ZONE_LAIR != null) && GrandBossConfig.VALAKAS_RECOGNIZE_HERO)
		{
			for (Player player : ZONE_LAIR.getPlayersInside())
			{
				if (player.isHero())
				{
					ZONE_LAIR.broadcastPacket(new ExShowScreenMessage(player.getName() + "!!!! You cannot hope to defeat me with your meager strength.", 2, 4000));
					break;
				}
			}
		}
	}
	
	private void handleDieStep1(Npc npc)
	{
		broadcastCamera(npc, 2000, 130, -1, 0, 15000, 10000, 0, 0, 1, 1, 0);
	}
	
	private void handleDieStep2(Npc npc)
	{
		broadcastCamera(npc, 1100, 210, -5, 3000, 15000, 10000, -13, 0, 1, 1, 0);
	}
	
	private void handleDieStep3(Npc npc)
	{
		broadcastCamera(npc, 1300, 200, -8, 3000, 15000, 10000, 0, 15, 1, 1, 0);
	}
	
	private void handleDieStep4(Npc npc)
	{
		broadcastCamera(npc, 1000, 190, 0, 500, 15000, 10000, 0, 10, 1, 1, 0);
	}
	
	private void handleDieStep5(Npc npc)
	{
		broadcastCamera(npc, 1700, 120, 0, 2500, 15000, 10000, 12, 40, 1, 1, 0);
	}
	
	private void handleDieStep6(Npc npc)
	{
		broadcastCamera(npc, 1700, 20, 0, 700, 15000, 10000, 10, 10, 1, 1, 0);
	}
	
	private void handleDieStep7(Npc npc)
	{
		broadcastCamera(npc, 1700, 10, 0, 1000, 15000, 10000, 20, 70, 1, 1, 0);
	}
	
	private void handleDieStep8(Npc npc)
	{
		broadcastCamera(npc, 1700, 10, 0, 300, 15000, 250, 20, -20, 1, 1, 0);
		
		for (Location location : TELEPORT_CUBE_LOCATIONS)
		{
			addSpawn(TELEPORT_CUBE, location, false, TELEPORT_CUBE_LIFETIME);
		}
		
		startQuestTimer(EVT_REMOVE_PLAYERS, TELEPORT_CUBE_LIFETIME, null, null);
	}
	
	private void broadcastCamera(Npc npc, int distance, int yaw, int pitch, int time, int duration, int unk, int relYaw, int relPitch, int isWide, int unk2, int unk3)
	{
		if (ZONE_LAIR == null)
		{
			return;
		}
		
		ZONE_LAIR.broadcastPacket(new SpecialCamera(npc, distance, yaw, pitch, time, duration, unk, relYaw, relPitch, isWide, unk2, unk3));
	}
	
	/**
	 * Ejects all players from lair and returns them to town.
	 */
	private void oustPlayersFromLair()
	{
		if (ZONE_LAIR != null)
		{
			ZONE_LAIR.oustAllPlayers();
		}
	}
	
	/**
	 * Core AI: selects targets, chooses skill and handles movement / casting decisions.
	 * @param npc Valakas instance.
	 */
	private void callSkillAI(Npc npc)
	{
		if ((npc == null) || npc.isInvul() || npc.isCastingNow())
		{
			return;
		}
		
		if ((_currentTarget == null) || _currentTarget.isDead() || !npc.isInSurroundingRegion(_currentTarget) || (getRandom(10) == 0))
		{
			_currentTarget = getRandomTarget(npc);
		}
		
		if (_currentTarget == null)
		{
			if (getRandom(10) == 0)
			{
				final int currentX = npc.getX();
				final int currentY = npc.getY();
				final int currentZ = npc.getZ();
				final int nextX = currentX + getRandom(-1400, 1400);
				final int nextY = currentY + getRandom(-1400, 1400);
				
				if (GeoEngine.getInstance().canMoveToTarget(currentX, currentY, currentZ, nextX, nextY, currentZ, npc.getInstanceId()))
				{
					npc.getAI().setIntention(Intention.MOVE_TO, new Location(nextX, nextY, currentZ, 0));
				}
			}
			return;
		}
		
		final SkillHolder chosenHolder = getRandomSkill(npc);
		final Skill chosenSkill = (chosenHolder != null) ? chosenHolder.getSkill() : null;
		if (chosenSkill == null)
		{
			return;
		}
		
		final int baseRange = chosenSkill.getCastRange();
		final int effectiveRange = (baseRange < 600) ? 600 : baseRange;
		if (LocationUtil.checkIfInRange(effectiveRange, npc, _currentTarget, true))
		{
			npc.getAI().setIntention(Intention.IDLE);
			npc.setCastingNow(true);
			npc.setTarget(_currentTarget);
			npc.doCast(chosenSkill);
		}
		else
		{
			npc.getAI().setIntention(Intention.FOLLOW, _currentTarget, null);
			npc.setCastingNow(false);
		}
	}
	
	/**
	 * Selects a random skill based on HP and nearby players.
	 * @param npc Valakas instance.
	 * @return skill holder or {@code null}.
	 */
	private SkillHolder getRandomSkill(Npc npc)
	{
		if (npc == null)
		{
			return null;
		}
		
		final double hpRatio = npc.getCurrentHp() / npc.getMaxHp();
		final int hpPercent = (int) (hpRatio * 100);
		
		if ((hpPercent < 75) && (getRandom(150) == 0) && !npc.isAffectedBySkill(SKILL_LAVA_SKIN.getSkillId()))
		{
			return SKILL_LAVA_SKIN;
		}
		
		final int nearbyPlayers = World.getInstance().getVisibleObjectsInRange(npc, Player.class, 1200).size();
		if (nearbyPlayers >= 20)
		{
			return getRandomEntry(SKILL_TABLE_AOE);
		}
		
		if (hpPercent > 50)
		{
			return getRandomEntry(SKILL_TABLE_REGULAR);
		}
		
		return getRandomEntry(SKILL_TABLE_LOW_HP);
	}
	
	/**
	 * Selects a random valid playable target in Valakas surroundings.
	 * @param npc Valakas instance.
	 * @return target or {@code null}.
	 */
	private Playable getRandomTarget(Npc npc)
	{
		if (npc == null)
		{
			return null;
		}
		
		final List<Playable> candidates = new ArrayList<>();
		World.getInstance().forEachVisibleObject(npc, Playable.class, playable ->
		{
			if ((playable != null) && !playable.isDead() && !playable.isInvisible() && !playable.isPet() && playable.isPlayable())
			{
				candidates.add(playable);
			}
		});
		
		return getRandomEntry(candidates);
	}
	
	@Override
	public void unload(boolean removeFromList)
	{
		if (_valakas != null)
		{
			_valakas.deleteMe();
			_valakas = null;
		}
		
		super.unload(removeFromList);
	}
	
	public static void main(String[] args)
	{
		new Valakas();
	}
}
