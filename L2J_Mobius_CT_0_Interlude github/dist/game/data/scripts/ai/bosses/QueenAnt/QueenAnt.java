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
package ai.bosses.QueenAnt;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.type.BossZone;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Queen Ant grand boss AI handler.<br>
 * Controls boss lifecycle, minion healing, raid curse and nest constraints.
 * @author BazookaRpm
 */
public class QueenAnt extends Script
{
	private static final Logger LOGGER = Logger.getLogger(QueenAnt.class.getName());
	
	// Boss and minion identifiers.
	private static final int QUEEN_ANT_ID = 29001;
	private static final int LARVA_ID = 29002;
	private static final int NURSE_ID = 29003;
	private static final int GUARD_ID = 29004;
	private static final int ROYAL_ID = 29005;
	
	// NPC identifiers array for event registration.
	private static final int[] MONITORED_NPC_IDS =
	{
		QUEEN_ANT_ID,
		LARVA_ID,
		NURSE_ID,
		GUARD_ID,
		ROYAL_ID
	};
	
	// Nest and spawn coordinates.
	private static final int QUEEN_NEST_X = -21610;
	private static final int QUEEN_NEST_Y = 181594;
	private static final int QUEEN_NEST_Z = -5734;
	
	private static final int LARVA_SPAWN_X = -21600;
	private static final int LARVA_SPAWN_Y = 179482;
	private static final int LARVA_SPAWN_Z = -5846;
	
	// Teleport destinations when the boss spawns.
	private static final Location TELEPORT_LOCATION_ONE = new Location(-19480, 187344, -5600);
	private static final Location TELEPORT_LOCATION_TWO = new Location(-17928, 180912, -5520);
	private static final Location TELEPORT_LOCATION_THREE = new Location(-23808, 182368, -5600);
	
	// Boss status flags.
	private static final byte STATUS_ALIVE = 0;
	private static final byte STATUS_DEAD = 1;
	
	// Timed event identifiers.
	private static final String EVENT_HEAL = "heal";
	private static final String EVENT_ACTION = "action";
	private static final String EVENT_QUEEN_UNLOCK = "queen_unlock";
	private static final String EVENT_DISTANCE_CHECK = "DISTANCE_CHECK";
	
	// Timers and behavior thresholds.
	private static final int HEAL_INTERVAL_MILLIS = 1000;
	private static final int ACTION_INTERVAL_MILLIS = 10000;
	private static final int DISTANCE_CHECK_INTERVAL_MILLIS = 5000;
	private static final int QUEEN_LEASH_MAX_DISTANCE = 2000;
	private static final int RAID_CURSE_LEVEL_DIFFERENCE = 8;
	private static final int RAID_CURSE_CAST_TIME_MILLIS = 300;
	
	// Respawn related constants.
	private static final long MILLIS_PER_HOUR = 3600000L;
	private static final int ROYAL_RESPAWN_BASE_SECONDS = 280;
	private static final int ROYAL_RESPAWN_VARIATION_SECONDS = 40;
	private static final int NURSE_RESPAWN_DELAY_MILLIS = 10000;
	
	// Skill holders.
	private static final SkillHolder NURSE_HEAL_SKILL_ONE = new SkillHolder(4020, 1);
	private static final SkillHolder NURSE_HEAL_SKILL_TWO = new SkillHolder(4024, 1);
	
	// Sound identifiers.
	private static final String SOUND_SPAWN = "BS01_A";
	private static final String SOUND_DEATH = "BS02_D";
	
	// Database field keys.
	private static final String VAR_RESPAWN_TIME = "respawn_time";
	private static final String VAR_HEADING = "heading";
	private static final String VAR_CURRENT_HP = "currentHP";
	private static final String VAR_CURRENT_MP = "currentMP";
	
	// Boss zone.
	private static BossZone _queenNestZone;
	
	// Runtime references.
	private Monster _queenInstance;
	private Monster _larvaInstance;
	private final Set<Monster> _activeNurses = ConcurrentHashMap.newKeySet();
	
	/**
	 * Initializes Queen Ant AI and restores boss state from the database.
	 */
	private QueenAnt()
	{
		addSpawnId(MONITORED_NPC_IDS);
		addKillId(MONITORED_NPC_IDS);
		addAggroRangeEnterId(MONITORED_NPC_IDS);
		addFactionCallId(NURSE_ID);
		
		_queenNestZone = ZoneManager.getInstance().getZoneById(12012, BossZone.class);
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		final StatSet bossInfo = bossManager.getStatSet(QUEEN_ANT_ID);
		final int currentStatus = bossManager.getStatus(QUEEN_ANT_ID);
		
		if (currentStatus == STATUS_DEAD)
		{
			final long respawnTime = bossInfo.getLong(VAR_RESPAWN_TIME);
			final long remainingDelay = respawnTime - System.currentTimeMillis();
			if (remainingDelay > 0)
			{
				startQuestTimer(EVENT_QUEEN_UNLOCK, remainingDelay, null, null);
			}
			else
			{
				final GrandBoss queen = (GrandBoss) addSpawn(QUEEN_ANT_ID, QUEEN_NEST_X, QUEEN_NEST_Y, QUEEN_NEST_Z, 0, false, 0);
				bossManager.setStatus(QUEEN_ANT_ID, STATUS_ALIVE);
				spawnBoss(queen);
			}
		}
		else
		{
			final int heading = bossInfo.getInt(VAR_HEADING);
			final double storedHp = bossInfo.getDouble(VAR_CURRENT_HP);
			final double storedMp = bossInfo.getDouble(VAR_CURRENT_MP);
			
			int locX = bossInfo.getInt("loc_x");
			int locY = bossInfo.getInt("loc_y");
			int locZ = bossInfo.getInt("loc_z");
			if (!_queenNestZone.isInsideZone(locX, locY, locZ))
			{
				locX = QUEEN_NEST_X;
				locY = QUEEN_NEST_Y;
				locZ = QUEEN_NEST_Z;
			}
			
			final GrandBoss queen = (GrandBoss) addSpawn(QUEEN_ANT_ID, locX, locY, locZ, heading, false, 0);
			queen.setCurrentHpMp(storedHp, storedMp);
			spawnBoss(queen);
		}
	}
	
	/**
	 * Spawns the boss, teleports players out of the nest and starts periodic tasks.
	 * @param boss the queen boss instance.
	 */
	private void spawnBoss(GrandBoss boss)
	{
		GrandBossManager.getInstance().addBoss(boss);
		
		if (_queenNestZone != null)
		{
			final int initialRoll = getRandom(100);
			if (initialRoll < 33)
			{
				_queenNestZone.movePlayersTo(TELEPORT_LOCATION_ONE);
			}
			else if (getRandom(100) < 50)
			{
				_queenNestZone.movePlayersTo(TELEPORT_LOCATION_TWO);
			}
			else
			{
				_queenNestZone.movePlayersTo(TELEPORT_LOCATION_THREE);
			}
		}
		
		GrandBossManager.getInstance().addBoss(boss);
		
		startQuestTimer(EVENT_ACTION, ACTION_INTERVAL_MILLIS, boss, null, true);
		startQuestTimer(EVENT_HEAL, HEAL_INTERVAL_MILLIS, null, null, true);
		
		boss.broadcastPacket(new PlaySound(1, SOUND_SPAWN, 1, boss.getObjectId(), boss.getX(), boss.getY(), boss.getZ()));
		
		_queenInstance = boss;
		_larvaInstance = addSpawn(LARVA_ID, LARVA_SPAWN_X, LARVA_SPAWN_Y, LARVA_SPAWN_Z, getRandom(360), false, 0).asMonster();
	}
	
	/**
	 * Handles scheduled events such as healing, social actions, respawn and distance checks.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case EVENT_HEAL:
			{
				final boolean larvaNeedsHeal = (_larvaInstance != null) && (_larvaInstance.getCurrentHp() < _larvaInstance.getMaxHp());
				final boolean queenNeedsHeal = (_queenInstance != null) && (_queenInstance.getCurrentHp() < _queenInstance.getMaxHp());
				
				for (Monster nurse : _activeNurses)
				{
					if ((nurse == null) || nurse.isDead() || nurse.isCastingNow())
					{
						continue;
					}
					
					final boolean nurseNotCasting = nurse.getAI().getIntention() != Intention.CAST;
					
					if (larvaNeedsHeal)
					{
						if ((nurse.getTarget() != _larvaInstance) || nurseNotCasting)
						{
							nurse.setTarget(_larvaInstance);
							
							final Skill healSkill = getRandomBoolean() ? NURSE_HEAL_SKILL_ONE.getSkill() : NURSE_HEAL_SKILL_TWO.getSkill();
							if (healSkill != null)
							{
								nurse.useMagic(healSkill);
							}
						}
						continue;
					}
					
					if (queenNeedsHeal)
					{
						if (nurse.getLeader() == _larvaInstance)
						{
							continue;
						}
						
						if ((_queenInstance != null) && ((nurse.getTarget() != _queenInstance) || nurseNotCasting))
						{
							nurse.setTarget(_queenInstance);
							
							final Skill healSkill = NURSE_HEAL_SKILL_ONE.getSkill();
							if (healSkill != null)
							{
								nurse.useMagic(healSkill);
							}
						}
						continue;
					}
					
					if (nurseNotCasting && (nurse.getTarget() != null))
					{
						nurse.setTarget(null);
					}
				}
				break;
			}
			case EVENT_ACTION:
			{
				if ((npc != null) && (getRandom(3) == 0))
				{
					if (getRandom(2) == 0)
					{
						npc.broadcastSocialAction(3);
					}
					else
					{
						npc.broadcastSocialAction(4);
					}
				}
				break;
			}
			case EVENT_QUEEN_UNLOCK:
			{
				final GrandBoss queen = (GrandBoss) addSpawn(QUEEN_ANT_ID, QUEEN_NEST_X, QUEEN_NEST_Y, QUEEN_NEST_Z, 0, false, 0);
				GrandBossManager.getInstance().setStatus(QUEEN_ANT_ID, STATUS_ALIVE);
				spawnBoss(queen);
				break;
			}
			case EVENT_DISTANCE_CHECK:
			{
				if ((_queenInstance == null) || _queenInstance.isDead())
				{
					cancelQuestTimers(EVENT_DISTANCE_CHECK);
				}
				else if (_queenInstance.calculateDistance2D(QUEEN_NEST_X, QUEEN_NEST_Y, QUEEN_NEST_Z) > QUEEN_LEASH_MAX_DISTANCE)
				{
					_queenInstance.clearAggroList();
					_queenInstance.getAI().setIntention(Intention.MOVE_TO, new Location(QUEEN_NEST_X, QUEEN_NEST_Y, QUEEN_NEST_Z, 0));
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Applies initial configuration when an NPC of this script spawns.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Monster monster = npc.asMonster();
		
		switch (npc.getId())
		{
			case LARVA_ID:
			{
				monster.setImmobilized(true);
				monster.setMortal(false);
				monster.setIsRaidMinion(true);
				break;
			}
			case NURSE_ID:
			{
				monster.disableCoreAI(true);
				monster.setIsRaidMinion(true);
				_activeNurses.add(monster);
				break;
			}
			case ROYAL_ID:
			case GUARD_ID:
			{
				monster.setIsRaidMinion(true);
				break;
			}
			case QUEEN_ANT_ID:
			{
				if (monster.getMinionList().getSpawnedMinions().isEmpty())
				{
					monster.getMinionList().spawnMinions(npc.getTemplate().getParameters().getMinionList("Privates"));
				}
				
				cancelQuestTimer(EVENT_DISTANCE_CHECK, npc, null);
				startQuestTimer(EVENT_DISTANCE_CHECK, DISTANCE_CHECK_INTERVAL_MILLIS, npc, null, true);
				break;
			}
		}
	}
	
	/**
	 * Handles faction calls, typically healing requests from allied NPCs.
	 */
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		if ((caller == null) || (npc == null))
		{
			return;
		}
		
		if (!npc.isCastingNow() && (npc.getAI().getIntention() != Intention.CAST) && (caller.getCurrentHp() < caller.getMaxHp()))
		{
			npc.setTarget(caller);
			
			final Skill healSkill = NURSE_HEAL_SKILL_ONE.getSkill();
			if (healSkill != null)
			{
				npc.asAttackable().useMagic(healSkill);
			}
		}
	}
	
	/**
	 * Handles raid curse logic when a player or summon enters aggro range.
	 */
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (npc == null)
		{
			return;
		}
		
		if (player.isGM() && player.isInvisible())
		{
			return;
		}
		
		boolean attackerIsMage;
		Playable attackerCharacter;
		
		if (isSummon)
		{
			attackerIsMage = false;
			attackerCharacter = player.getSummon();
		}
		else
		{
			attackerIsMage = player.isMageClass();
			attackerCharacter = player;
		}
		
		if (attackerCharacter == null)
		{
			return;
		}
		
		if (!NpcConfig.RAID_DISABLE_CURSE && ((attackerCharacter.getLevel() - npc.getLevel()) > RAID_CURSE_LEVEL_DIFFERENCE))
		{
			Skill curse = null;
			
			if (attackerIsMage)
			{
				if (!attackerCharacter.isMuted() && (getRandom(4) == 0))
				{
					curse = CommonSkill.RAID_CURSE.getSkill();
				}
			}
			else if (!attackerCharacter.isParalyzed() && (getRandom(4) == 0))
			{
				curse = CommonSkill.RAID_CURSE2.getSkill();
			}
			
			if (curse != null)
			{
				npc.broadcastPacket(new MagicSkillUse(npc, attackerCharacter, curse.getId(), curse.getLevel(), RAID_CURSE_CAST_TIME_MILLIS, 0));
				curse.applyEffects(npc, attackerCharacter);
			}
			
			npc.asAttackable().stopHating(attackerCharacter);
		}
	}
	
	/**
	 * Handles kill events for Queen Ant and her minions.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == QUEEN_ANT_ID)
		{
			npc.broadcastPacket(new PlaySound(1, SOUND_DEATH, 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setStatus(QUEEN_ANT_ID, STATUS_DEAD);
			
			final long baseIntervalMillis = GrandBossConfig.QUEEN_ANT_SPAWN_INTERVAL * MILLIS_PER_HOUR;
			final long randomRangeMillis = GrandBossConfig.QUEEN_ANT_SPAWN_RANDOM * MILLIS_PER_HOUR;
			final long respawnDelay = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			final long nextRespawnTime = System.currentTimeMillis() + respawnDelay;
			LOGGER.info("Queen Ant will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer(EVENT_QUEEN_UNLOCK, respawnDelay, null, null);
			cancelQuestTimer(EVENT_ACTION, npc, null);
			cancelQuestTimer(EVENT_HEAL, null, null);
			
			final StatSet bossInfo = GrandBossManager.getInstance().getStatSet(QUEEN_ANT_ID);
			bossInfo.set(VAR_RESPAWN_TIME, nextRespawnTime);
			GrandBossManager.getInstance().setStatSet(QUEEN_ANT_ID, bossInfo);
			
			_activeNurses.clear();
			
			if (_larvaInstance != null)
			{
				_larvaInstance.deleteMe();
			}
			
			_larvaInstance = null;
			_queenInstance = null;
			
			cancelQuestTimers(EVENT_DISTANCE_CHECK);
		}
		else if ((_queenInstance != null) && !_queenInstance.isAlikeDead())
		{
			if (npcId == ROYAL_ID)
			{
				final Monster royal = npc.asMonster();
				if (royal.getLeader() != null)
				{
					int respawnDelay = (ROYAL_RESPAWN_BASE_SECONDS + getRandom(ROYAL_RESPAWN_VARIATION_SECONDS)) * 1000;
					royal.getLeader().getMinionList().onMinionDie(royal, respawnDelay);
				}
			}
			else if (npcId == NURSE_ID)
			{
				final Monster nurse = npc.asMonster();
				_activeNurses.remove(nurse);
				
				if (nurse.getLeader() != null)
				{
					nurse.getLeader().getMinionList().onMinionDie(nurse, NURSE_RESPAWN_DELAY_MILLIS);
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new QueenAnt();
	}
}
