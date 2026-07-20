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
package ai.bosses.Orfen;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Orfen grand boss AI handler.<br>
 * Controls Orfen spawn cycle, teleport behavior, minion management and voice reactions.
 * <ul>
 * <li>Restores Orfen state from database and schedules respawn.</li>
 * <li>Spawns and tracks Raikel Leos minions with distance checks.</li>
 * <li>Handles skill reactions, faction heal/attack and HP-based teleports.</li>
 * </ul>
 * @author BazookaRpm
 */
public class Orfen extends Script
{
	// Logging.
	private static final Logger LOGGER = Logger.getLogger(Orfen.class.getName());
	
	// NPC identifiers.
	private static final int ORFEN_NPC_ID = 29014;
	// private static final int RAIKEL_NPC_ID = 29015;
	private static final int RAIKEL_LEOS_NPC_ID = 29016;
	// private static final int RIBA_NPC_ID = 29017;
	private static final int RIBA_IREN_NPC_ID = 29018;
	
	// Boss status flags.
	private static final byte STATUS_ALIVE = 0;
	private static final byte STATUS_DEAD = 1;
	
	// Event identifiers.
	private static final String EVENT_ORFEN_UNLOCK = "orfen_unlock";
	private static final String EVENT_CHECK_ORFEN_POSITION = "check_orfen_pos";
	private static final String EVENT_CHECK_MINION_LOCATION = "check_minion_loc";
	private static final String EVENT_DESPAWN_MINIONS = "despawn_minions";
	private static final String EVENT_SPAWN_MINION = "spawn_minion";
	
	// Timing configuration.
	private static final long MILLIS_PER_HOUR = 3600000L;
	private static final int CHECK_POSITION_INTERVAL_MS = 10000;
	private static final int CHECK_MINION_INTERVAL_MS = 10000;
	private static final int MINION_DESPAWN_DELAY_MS = 20000;
	private static final int MINION_RESPAWN_DELAY_MS = 360000;
	
	// Distance thresholds.
	private static final int MINION_FOLLOW_RADIUS = 3000;
	private static final int TELEPORT_SKILL_MAX_RANGE = 1000;
	private static final int TELEPORT_SKILL_MIN_RANGE = 300;
	
	// HP thresholds (fractions).
	private static final double ORFEN_TELEPORT_HP_FRACTION = 0.5;
	private static final double ORFEN_RETURN_HP_FRACTION = 0.95;
	private static final double RIBA_IREN_HEAL_HP_FRACTION = 0.5;
	
	// Probabilities (1 / N).
	private static final int SKILL_SEE_REACTION_CHANCE = 5; // 1 / 5.
	private static final int ORFEN_ATTACK_TELEPORT_CHANCE = 10; // 1 / 10.
	private static final int RAIKEL_FACTION_SKILL_CHANCE = 20; // 1 / 20.
	private static final int RIBA_IREN_BASE_HEAL_CHANCE = 10; // getRandom(10) < chance.
	private static final int RIBA_IREN_ORFEN_HEAL_CHANCE = 9;
	
	// Skill holders.
	private static final SkillHolder PARALYSIS = new SkillHolder(4064, 1);
	private static final SkillHolder BLOW = new SkillHolder(4067, 4);
	private static final SkillHolder ORFEN_HEAL = new SkillHolder(4516, 1);
	
	// Spawn locations (nest and roaming positions).
	private static final Location[] ORFEN_SPAWN_LOCATIONS =
	{
		new Location(43728, 17220, -4342),
		new Location(55024, 17368, -5412),
		new Location(53504, 21248, -5486),
		new Location(53248, 24576, -5262)
	};
	
	// Dialog lines.
	private static final String[] ORFEN_MESSAGES =
	{
		"$s1. Stop kidding yourself about your own powerlessness!",
		"$s1. I'll make you feel what true fear is!",
		"You're really stupid to have challenged me. $s1! Get ready!",
		"$s1. Do you think that's going to work?!"
	};
	
	// Runtime flags and collections.
	private static final Collection<Attackable> MINIONS = ConcurrentHashMap.newKeySet();
	private static boolean _hasTeleported;
	private static BossZone ZONE;
	
	/**
	 * Initializes Orfen AI, restores boss state and schedules respawn if required.
	 */
	private Orfen()
	{
		addSkillSeeId(ORFEN_NPC_ID);
		addFactionCallId(ORFEN_NPC_ID, RAIKEL_LEOS_NPC_ID, RIBA_IREN_NPC_ID);
		addAttackId(ORFEN_NPC_ID, RAIKEL_LEOS_NPC_ID, RIBA_IREN_NPC_ID);
		addKillId(ORFEN_NPC_ID);
		
		_hasTeleported = false;
		ZONE = GrandBossManager.getInstance().getZone(ORFEN_SPAWN_LOCATIONS[0]);
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		final StatSet info = bossManager.getStatSet(ORFEN_NPC_ID);
		if (bossManager.getStatus(ORFEN_NPC_ID) == STATUS_DEAD)
		{
			// Load the unlock date and time for Orfen from DB.
			final long remainingMillis = info.getLong("respawn_time") - System.currentTimeMillis();
			
			// If Orfen is locked until a certain time, start unlock timer if time has not yet expired.
			if (remainingMillis > 0)
			{
				startQuestTimer(EVENT_ORFEN_UNLOCK, remainingMillis, null, null);
			}
			else
			{
				// Respawn time has already elapsed while the server was offline. Spawn Orfen immediately.
				final int randomIndex = getRandom(10);
				Location spawnLocation;
				if (randomIndex < 4)
				{
					spawnLocation = ORFEN_SPAWN_LOCATIONS[1];
				}
				else if (randomIndex < 7)
				{
					spawnLocation = ORFEN_SPAWN_LOCATIONS[2];
				}
				else
				{
					spawnLocation = ORFEN_SPAWN_LOCATIONS[3];
				}
				
				final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN_NPC_ID, spawnLocation, false, 0);
				bossManager.setStatus(ORFEN_NPC_ID, STATUS_ALIVE);
				spawnBoss(orfen);
			}
		}
		else
		{
			final int locX = info.getInt("loc_x");
			final int locY = info.getInt("loc_y");
			final int locZ = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			
			final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN_NPC_ID, locX, locY, locZ, heading, false, 0);
			orfen.setCurrentHpMp(hp, mp);
			spawnBoss(orfen);
		}
	}
	
	/**
	 * Updates Orfen spawn point and teleports it to a predefined location index.
	 * @param npc Orfen instance.
	 * @param index Predefined location index.
	 */
	public void setSpawnPoint(Npc npc, int index)
	{
		npc.asAttackable().clearAggroList();
		npc.getAI().setIntention(Intention.IDLE, null, null);
		
		final Spawn spawn = npc.getSpawn();
		spawn.setLocation(ORFEN_SPAWN_LOCATIONS[index]);
		npc.teleToLocation(ORFEN_SPAWN_LOCATIONS[index], false);
	}
	
	/**
	 * Registers Orfen in the boss manager, plays spawn sound and spawns minions.
	 * @param npc Orfen boss instance.
	 */
	public void spawnBoss(GrandBoss npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		
		startQuestTimer(EVENT_CHECK_ORFEN_POSITION, CHECK_POSITION_INTERVAL_MS, npc, null, true);
		
		// Spawn minions.
		final int bossX = npc.getX();
		final int bossY = npc.getY();
		final int bossZ = npc.getZ();
		
		Attackable minion;
		
		minion = addSpawn(RAIKEL_LEOS_NPC_ID, bossX + 100, bossY + 100, bossZ, 0, false, 0).asAttackable();
		minion.setIsRaidMinion(true);
		MINIONS.add(minion);
		
		minion = addSpawn(RAIKEL_LEOS_NPC_ID, bossX + 100, bossY - 100, bossZ, 0, false, 0).asAttackable();
		minion.setIsRaidMinion(true);
		MINIONS.add(minion);
		
		minion = addSpawn(RAIKEL_LEOS_NPC_ID, bossX - 100, bossY + 100, bossZ, 0, false, 0).asAttackable();
		minion.setIsRaidMinion(true);
		MINIONS.add(minion);
		
		minion = addSpawn(RAIKEL_LEOS_NPC_ID, bossX - 100, bossY - 100, bossZ, 0, false, 0).asAttackable();
		minion.setIsRaidMinion(true);
		MINIONS.add(minion);
		
		startQuestTimer(EVENT_CHECK_MINION_LOCATION, CHECK_MINION_INTERVAL_MS, npc, null, true);
	}
	
	/**
	 * Handles timed events such as unlock, position checks and minion management.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case EVENT_ORFEN_UNLOCK:
			{
				final int randomIndex = getRandom(10);
				final Location spawnLocation;
				if (randomIndex < 4)
				{
					spawnLocation = ORFEN_SPAWN_LOCATIONS[1];
				}
				else if (randomIndex < 7)
				{
					spawnLocation = ORFEN_SPAWN_LOCATIONS[2];
				}
				else
				{
					spawnLocation = ORFEN_SPAWN_LOCATIONS[3];
				}
				
				final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN_NPC_ID, spawnLocation, false, 0);
				GrandBossManager.getInstance().setStatus(ORFEN_NPC_ID, STATUS_ALIVE);
				spawnBoss(orfen);
				break;
			}
			case EVENT_CHECK_ORFEN_POSITION:
			{
				if ((_hasTeleported && (npc.getCurrentHp() > (npc.getMaxHp() * ORFEN_RETURN_HP_FRACTION))) || (!ZONE.isInsideZone(npc) && !_hasTeleported))
				{
					setSpawnPoint(npc, getRandom(3) + 1);
					_hasTeleported = false;
				}
				else if (_hasTeleported && !ZONE.isInsideZone(npc))
				{
					setSpawnPoint(npc, 0);
				}
				break;
			}
			case EVENT_CHECK_MINION_LOCATION:
			{
				for (Attackable minion : MINIONS)
				{
					if (!npc.isInsideRadius2D(minion, MINION_FOLLOW_RADIUS))
					{
						minion.teleToLocation(npc.getLocation());
						npc.asAttackable().clearAggroList();
						npc.getAI().setIntention(Intention.IDLE, null, null);
					}
				}
				break;
			}
			case EVENT_DESPAWN_MINIONS:
			{
				for (Attackable minion : MINIONS)
				{
					if (minion != null)
					{
						minion.decayMe();
					}
				}
				
				MINIONS.clear();
				break;
			}
			case EVENT_SPAWN_MINION:
			{
				final Attackable minion = addSpawn(RAIKEL_LEOS_NPC_ID, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0).asAttackable();
				minion.setIsRaidMinion(true);
				MINIONS.add(minion);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles skills seen by Orfen and its minions, including teleport reaction.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		final Creature originalCaster = isSummon ? caster.getSummon() : caster;
		if ((skill.getEffectPoint() > 0) && (getRandom(SKILL_SEE_REACTION_CHANCE) == 0) && npc.isInsideRadius2D(originalCaster, TELEPORT_SKILL_MAX_RANGE))
		{
			final int messageIndex = getRandom(4);
			final String messageText = ORFEN_MESSAGES[messageIndex].replace("$s1", caster.getName());
			final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), messageText);
			npc.broadcastPacket(packet);
			
			originalCaster.teleToLocation(npc.getLocation());
			npc.setTarget(originalCaster);
			npc.doCast(PARALYSIS.getSkill());
		}
	}
	
	/**
	 * Handles faction call logic for Raikel Leos and Riba Iren.
	 */
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		if ((caller == null) || (npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		final int npcId = npc.getId();
		final int callerId = caller.getId();
		if ((npcId == RAIKEL_LEOS_NPC_ID) && (getRandom(RAIKEL_FACTION_SKILL_CHANCE) == 0))
		{
			npc.setTarget(attacker);
			npc.doCast(BLOW.getSkill());
		}
		else if (npcId == RIBA_IREN_NPC_ID)
		{
			int healChance = 1;
			if (callerId == ORFEN_NPC_ID)
			{
				healChance = RIBA_IREN_ORFEN_HEAL_CHANCE;
			}
			
			if ((callerId != RIBA_IREN_NPC_ID) && (caller.getCurrentHp() < (caller.getMaxHp() / 2.0)) && (getRandom(RIBA_IREN_BASE_HEAL_CHANCE) < healChance))
			{
				npc.getAI().setIntention(Intention.IDLE, null, null);
				npc.setTarget(caller);
				npc.doCast(ORFEN_HEAL.getSkill());
			}
		}
	}
	
	/**
	 * Handles attack events for Orfen and Riba Iren.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == ORFEN_NPC_ID)
		{
			if (!_hasTeleported && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() * ORFEN_TELEPORT_HP_FRACTION)))
			{
				_hasTeleported = true;
				setSpawnPoint(npc, 0);
			}
			else if (npc.isInsideRadius2D(attacker, TELEPORT_SKILL_MAX_RANGE) && !npc.isInsideRadius2D(attacker, TELEPORT_SKILL_MIN_RANGE) && (getRandom(ORFEN_ATTACK_TELEPORT_CHANCE) == 0))
			{
				final int messageIndex = getRandom(3);
				final String messageText = ORFEN_MESSAGES[messageIndex].replace("$s1", attacker.getName());
				final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npcId, messageText);
				npc.broadcastPacket(packet);
				
				attacker.teleToLocation(npc.getLocation());
				npc.setTarget(attacker);
				npc.doCast(PARALYSIS.getSkill());
			}
		}
		else if ((npcId == RIBA_IREN_NPC_ID) && !npc.isCastingNow() && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() * RIBA_IREN_HEAL_HP_FRACTION)))
		{
			npc.setTarget(attacker);
			npc.doCast(ORFEN_HEAL.getSkill());
		}
	}
	
	/**
	 * Handles Orfen and Raikel Leos death, including respawn scheduling.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == ORFEN_NPC_ID)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setStatus(ORFEN_NPC_ID, STATUS_DEAD);
			
			final long baseIntervalMillis = GrandBossConfig.ORFEN_SPAWN_INTERVAL * MILLIS_PER_HOUR;
			final long randomRangeMillis = GrandBossConfig.ORFEN_SPAWN_RANDOM * MILLIS_PER_HOUR;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			// Next respawn time.
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info("Orfen will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer(EVENT_ORFEN_UNLOCK, respawnTime, null, null);
			
			// Save respawn time so that the info is maintained past reboots.
			final StatSet info = GrandBossManager.getInstance().getStatSet(ORFEN_NPC_ID);
			info.set("respawn_time", nextRespawnTime);
			GrandBossManager.getInstance().setStatSet(ORFEN_NPC_ID, info);
			
			cancelQuestTimer(EVENT_CHECK_MINION_LOCATION, npc, null);
			cancelQuestTimer(EVENT_CHECK_ORFEN_POSITION, npc, null);
			startQuestTimer(EVENT_DESPAWN_MINIONS, MINION_DESPAWN_DELAY_MS, null, null);
			cancelQuestTimers(EVENT_SPAWN_MINION);
		}
		else if ((GrandBossManager.getInstance().getStatus(ORFEN_NPC_ID) == STATUS_ALIVE) && (npcId == RAIKEL_LEOS_NPC_ID))
		{
			MINIONS.remove(npc.asAttackable());
			startQuestTimer(EVENT_SPAWN_MINION, MINION_RESPAWN_DELAY_MS, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new Orfen();
	}
}
