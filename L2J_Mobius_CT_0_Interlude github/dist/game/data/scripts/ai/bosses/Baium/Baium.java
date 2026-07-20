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
package ai.bosses.Baium;

import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.variables.NpcVariables;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * Baium grand boss AI implementation for the tower rooftop encounter.<br>
 * Provides a complete controller for access flow, cinematic sequence, combat logic and cleanup.
 * <ul>
 * <li>Restores persistent Baium state and reconstructs on-going fights from {@link GrandBossManager}.</li>
 * <li>Coordinates angelic vortex entry, fabric consumption and teleportation to the rooftop arena.</li>
 * <li>Runs the wake-up sequence including earthquakes, social actions and Baium's punitive gift.</li>
 * <li>Spawns and manages archangel minions using a three-slot custom aggro model.</li>
 * <li>Applies inactivity timeouts, self-heal checks, respawn scheduling and full zone clearing.</li>
 * </ul>
 * @author BazookaRpm
 */
public class Baium extends Script
{
	// Logging facilities.
	private static final Logger LOGGER = Logger.getLogger(Baium.class.getName());
	
	// NPC definitions.
	private static final int BAIUM_NPC_ID = 29020; // GranbossBaium Npc
	private static final int BAIUM_STONE_NPC_ID = 29025; // GranbossBaium Stone Npc
	private static final int ANGELIC_VORTEX_NPC_ID = 31862; // Vortex Teleport Npc
	private static final int ARCHANGEL_NPC_ID = 29021; // Archangel Minion Npc
	private static final int TELEPORT_CUBE_NPC_ID = 31842; // Teleport Cube Exit
	
	// Item definitions.
	private static final int BLOODED_FABRIC_ITEM_ID = 4295;
	
	// Skill templates.
	private static final SkillHolder SKILL_BAIUM_ATTACK = new SkillHolder(4127, 1); // Baium: General Attack.
	private static final SkillHolder SKILL_ENERGY_WAVE = new SkillHolder(4128, 1); // Wind Of Force.
	private static final SkillHolder SKILL_EARTHQUAKE = new SkillHolder(4129, 1); // Earthquake.
	private static final SkillHolder SKILL_THUNDERBOLT = new SkillHolder(4130, 1); // Striking of Thunderbolt.
	private static final SkillHolder SKILL_GROUP_HOLD = new SkillHolder(4131, 1); // Stun.
	private static final SkillHolder SKILL_SPEAR_ATTACK = new SkillHolder(4132, 1); // Spear: Pound the Ground.
	private static final SkillHolder SKILL_ANGEL_HEAL = new SkillHolder(4133, 1); // Angel Heal.
	private static final SkillHolder SKILL_HEAL_OF_BAIUM = new SkillHolder(4135, 1); // Baium Heal.
	private static final SkillHolder SKILL_BAIUM_PRESENT = new SkillHolder(4136, 1); // Baium's Gift.
	private static final SkillHolder SKILL_ANTI_STRIDER = new SkillHolder(4258, 1); // Hinder Strider.
	
	// Zone configuration and rooftop area.
	private static final int BAIUM_RIM_ZONE_ID = 70051;
	private static final NoRestartZone BAIUM_RIM_ZONE = ZoneManager.getInstance().getZoneById(BAIUM_RIM_ZONE_ID, NoRestartZone.class);
	
	// Status flags for Baium lifecycle.
	private static final int STATUS_ALIVE = 0;
	private static final int STATUS_WAITING = 1;
	private static final int STATUS_IN_FIGHT = 2;
	private static final int STATUS_DEAD = 3;
	
	// Time configuration in milliseconds.
	private static final int CHECK_ATTACK_INTERVAL_MILLIS = 60000;
	private static final int INACTIVITY_LIMIT_MILLIS = 1800000;
	private static final int TELEPORT_CUBE_LIFETIME_MILLIS = 900000;
	private static final int SELECT_TARGET_INTERVAL_MILLIS = 5000;
	
	// Distance configuration.
	private static final int PLAYER_PORT_RADIUS = 16000;
	
	// Location constants for teleport and spawn points.
	private static final Location LOCATION_BAIUM_GIFT = new Location(115910, 17337, 10105);
	private static final Location LOCATION_BAIUM_SPAWN = new Location(116033, 17447, 10107, 40188);
	private static final Location LOCATION_TELEPORT_CUBE = new Location(115017, 15549, 10090);
	private static final Location LOCATION_TELEPORT_IN = new Location(114077, 15882, 10078);
	private static final Location[] TELEPORT_OUT_LOCATIONS =
	{
		new Location(108784, 16000, -4928),
		new Location(113824, 10448, -5164),
		new Location(115488, 22096, -5168)
	};
	private static final Location[] ARCHANGEL_SPAWN_LOCATIONS =
	{
		new Location(115792, 16608, 10136, 0),
		new Location(115168, 17200, 10136, 0),
		new Location(115780, 15564, 10136, 13620),
		new Location(114880, 16236, 10136, 5400),
		new Location(114239, 17168, 10136, -1992)
	};
	
	// String identifiers for internal scripted events.
	private static final String EVENT_HTML_VORTEX_READY = "31862-04.html";
	private static final String EVENT_ENTER = "ENTER";
	private static final String EVENT_EXIT = "EXIT";
	private static final String EVENT_WAKE_UP = "WAKE_UP";
	private static final String EVENT_WAKEUP_ACTION = "WAKEUP_ACTION";
	private static final String EVENT_MANAGE_EARTHQUAKE = "MANAGE_EARTHQUAKE";
	private static final String EVENT_SOCIAL_ACTION = "SOCIAL_ACTION";
	private static final String EVENT_PLAYER_PORT = "PLAYER_PORT";
	private static final String EVENT_PLAYER_KILL = "PLAYER_KILL";
	private static final String EVENT_SPAWN_ARCHANGELS = "SPAWN_ARCHANGEL";
	private static final String EVENT_SELECT_TARGET = "SELECT_TARGET";
	private static final String EVENT_CHECK_ATTACK = "CHECK_ATTACK";
	private static final String EVENT_CLEAR_STATUS = "CLEAR_STATUS";
	private static final String EVENT_CLEAR_ZONE = "CLEAR_ZONE";
	private static final String EVENT_RESPAWN_BAIUM = "RESPAWN_BAIUM";
	private static final String EVENT_ABORT_FIGHT = "ABORT_FIGHT";
	private static final String EVENT_DESPAWN_MINIONS = "DESPAWN_MINIONS";
	private static final String EVENT_MANAGE_SKILLS = "MANAGE_SKILLS";
	
	// Runtime state for Baium encounter.
	private volatile GrandBoss _baiumInstance;
	private static volatile long _lastAttackTime;
	
	/**
	 * Initializes Baium AI, registers handlers and restores state from boss manager.<br>
	 * Reconstructs Baium on-going fights and schedules respawn timers when dead.
	 */
	private Baium()
	{
		addFirstTalkId(ANGELIC_VORTEX_NPC_ID);
		addTalkId(ANGELIC_VORTEX_NPC_ID, TELEPORT_CUBE_NPC_ID, BAIUM_STONE_NPC_ID);
		addStartNpc(ANGELIC_VORTEX_NPC_ID, TELEPORT_CUBE_NPC_ID, BAIUM_STONE_NPC_ID);
		addAttackId(BAIUM_NPC_ID, ARCHANGEL_NPC_ID);
		addKillId(BAIUM_NPC_ID);
		addSpellFinishedId(BAIUM_NPC_ID);
		addCreatureSeeId(BAIUM_NPC_ID);
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		final StatSet bossInfo = bossManager.getStatSet(BAIUM_NPC_ID);
		
		final double storedHp = bossInfo.getDouble("currentHP");
		final double storedMp = bossInfo.getDouble("currentMP");
		final int storedX = bossInfo.getInt("loc_x");
		final int storedY = bossInfo.getInt("loc_y");
		final int storedZ = bossInfo.getInt("loc_z");
		final int storedHeading = bossInfo.getInt("heading");
		final long storedRespawnTime = bossInfo.getLong("respawn_time");
		
		final int currentStatus = getStatus();
		switch (currentStatus)
		{
			case STATUS_WAITING:
			{
				setStatus(STATUS_ALIVE);
				break;
			}
			case STATUS_ALIVE:
			{
				addSpawn(BAIUM_STONE_NPC_ID, LOCATION_BAIUM_SPAWN, false, 0);
				break;
			}
			case STATUS_IN_FIGHT:
			{
				final GrandBoss restoredBaium = (GrandBoss) addSpawn(BAIUM_NPC_ID, storedX, storedY, storedZ, storedHeading, false, 0);
				_baiumInstance = restoredBaium;
				_baiumInstance.setCurrentHpMp(storedHp, storedMp);
				_lastAttackTime = System.currentTimeMillis();
				addBoss(_baiumInstance);
				
				for (Location archangelLocation : ARCHANGEL_SPAWN_LOCATIONS)
				{
					final Npc archangelNpc = addSpawn(ARCHANGEL_NPC_ID, archangelLocation, false, 0, true);
					startQuestTimer(EVENT_SELECT_TARGET, SELECT_TARGET_INTERVAL_MILLIS, archangelNpc, null);
				}
				
				startQuestTimer(EVENT_CHECK_ATTACK, CHECK_ATTACK_INTERVAL_MILLIS, _baiumInstance, null);
				break;
			}
			case STATUS_DEAD:
			{
				final long remainingDelay = storedRespawnTime - System.currentTimeMillis();
				if (remainingDelay > 0)
				{
					startQuestTimer(EVENT_CLEAR_STATUS, remainingDelay, null, null);
				}
				else
				{
					notifyEvent(EVENT_CLEAR_STATUS, null, null);
				}
				break;
			}
		}
	}
	
	/**
	 * Central dispatcher for all Baium script events and scheduled timers.<br>
	 * Handles HTML interactions, cinematic sequence, minion behavior, inactivity checks and GM control hooks.
	 * @param event The event name.
	 * @param npc The NPC reference.
	 * @param player The player reference.
	 * @return The HTML file name or parent result.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			// Static HTML.
			case EVENT_HTML_VORTEX_READY:
			{
				return EVENT_HTML_VORTEX_READY;
			}
			// Player entry and exit handling.
			case EVENT_ENTER:
			{
				String htmlResponse = null;
				final int baiumStatus = getStatus();
				
				if (baiumStatus == STATUS_DEAD)
				{
					htmlResponse = "31862-03.html";
				}
				else if (baiumStatus == STATUS_IN_FIGHT)
				{
					htmlResponse = "31862-02.html";
				}
				else if (!hasQuestItems(player, BLOODED_FABRIC_ITEM_ID))
				{
					htmlResponse = "31862-01.html";
				}
				else
				{
					takeItems(player, BLOODED_FABRIC_ITEM_ID, 1);
					player.teleToLocation(LOCATION_TELEPORT_IN);
				}
				return htmlResponse;
			}
			case EVENT_EXIT:
			{
				final Location exitDestination = TELEPORT_OUT_LOCATIONS[getRandom(TELEPORT_OUT_LOCATIONS.length)];
				final int randomOffsetX = getRandom(100);
				final int randomOffsetY = getRandom(100);
				player.teleToLocation(exitDestination.getX() + randomOffsetX, exitDestination.getY() + randomOffsetY, exitDestination.getZ());
				break;
			}
			// Wake-up flow, cinematic and initial actions.
			case EVENT_WAKE_UP:
			{
				if (getStatus() == STATUS_ALIVE)
				{
					setStatus(STATUS_IN_FIGHT);
					
					_baiumInstance = (GrandBoss) addSpawn(BAIUM_NPC_ID, LOCATION_BAIUM_SPAWN, false, 0);
					_baiumInstance.disableCoreAI(true);
					_baiumInstance.setRandomWalking(false);
					
					addBoss(_baiumInstance);
					_lastAttackTime = System.currentTimeMillis();
					
					startQuestTimer(EVENT_WAKEUP_ACTION, 50, _baiumInstance, null);
					startQuestTimer(EVENT_MANAGE_EARTHQUAKE, 2000, _baiumInstance, null);
					startQuestTimer(EVENT_SOCIAL_ACTION, 10000, _baiumInstance, player);
					startQuestTimer(EVENT_CHECK_ATTACK, CHECK_ATTACK_INTERVAL_MILLIS, _baiumInstance, null);
					
					npc.deleteMe();
				}
				break;
			}
			case EVENT_WAKEUP_ACTION:
			{
				if ((npc != null) && (_baiumInstance != null) && !_baiumInstance.isDead())
				{
					BAIUM_RIM_ZONE.broadcastPacket(new SocialAction(_baiumInstance.getObjectId(), 2));
				}
				break;
			}
			case EVENT_MANAGE_EARTHQUAKE:
			{
				if (npc != null)
				{
					BAIUM_RIM_ZONE.broadcastPacket(new Earthquake(npc.getX(), npc.getY(), npc.getZ(), 40, 10));
					BAIUM_RIM_ZONE.broadcastPacket(new PlaySound("BS02_A"));
				}
				break;
			}
			case EVENT_SOCIAL_ACTION:
			{
				if (npc != null)
				{
					BAIUM_RIM_ZONE.broadcastPacket(new SocialAction(npc.getObjectId(), 3));
					startQuestTimer(EVENT_PLAYER_PORT, 6000, npc, player);
				}
				break;
			}
			case EVENT_PLAYER_PORT:
			{
				if (npc != null)
				{
					if ((player != null) && player.isInsideRadius3D(npc, PLAYER_PORT_RADIUS))
					{
						player.teleToLocation(LOCATION_BAIUM_GIFT);
						startQuestTimer(EVENT_PLAYER_KILL, 3000, npc, player);
					}
					else
					{
						final Player randomPlayer = getRandomPlayer(npc);
						if (randomPlayer != null)
						{
							randomPlayer.teleToLocation(LOCATION_BAIUM_GIFT);
							startQuestTimer(EVENT_PLAYER_KILL, 3000, npc, randomPlayer);
						}
						else
						{
							startQuestTimer(EVENT_PLAYER_KILL, 3000, npc, null);
						}
					}
				}
				break;
			}
			case EVENT_PLAYER_KILL:
			{
				if ((npc != null) && (player != null) && player.isInsideRadius3D(npc, PLAYER_PORT_RADIUS))
				{
					BAIUM_RIM_ZONE.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
					npc.broadcastSay(ChatType.NPC_GENERAL, "How dare you wake me! Now you shall die!");
					npc.setTarget(player);
					npc.doCast(SKILL_BAIUM_PRESENT.getSkill());
				}
				
				for (Player insidePlayer : BAIUM_RIM_ZONE.getPlayersInside())
				{
					if (insidePlayer.isHero() && GrandBossConfig.BAIUM_RECOGNIZE_HERO)
					{
						final String heroMessage = "Not even the gods themselves could touch me. But you, " + insidePlayer.getName() + ", you dare challenge me?! Ignorant mortal!";
						BAIUM_RIM_ZONE.broadcastPacket(new ExShowScreenMessage(heroMessage, 2, 4000));
						break;
					}
				}
				
				startQuestTimer(EVENT_SPAWN_ARCHANGELS, 8000, npc, player);
				break;
			}
			// Archangel minion management.
			case EVENT_SPAWN_ARCHANGELS:
			{
				if ((_baiumInstance == null) || _baiumInstance.isDead() || (getStatus() != STATUS_IN_FIGHT))
				{
					break;
				}
				
				_baiumInstance.disableCoreAI(false);
				_baiumInstance.setRandomWalking(true);
				
				for (Location archangelLocation : ARCHANGEL_SPAWN_LOCATIONS)
				{
					final Npc archangelNpc = addSpawn(ARCHANGEL_NPC_ID, archangelLocation, false, 0, true);
					startQuestTimer(EVENT_SELECT_TARGET, SELECT_TARGET_INTERVAL_MILLIS, archangelNpc, null);
				}
				
				if ((player != null) && !player.isDead())
				{
					addAttackDesire(npc, player);
				}
				else
				{
					final Player randomTarget = getRandomPlayer(npc);
					if (randomTarget != null)
					{
						addAttackDesire(npc, randomTarget);
					}
				}
				break;
			}
			case EVENT_SELECT_TARGET:
			{
				if (npc != null)
				{
					final Attackable archangel = npc.asAttackable();
					final Creature currentMostHated = archangel.getMostHated();
					
					if ((_baiumInstance == null) || _baiumInstance.isDead())
					{
						archangel.deleteMe();
						break;
					}
					
					if ((currentMostHated != null) && currentMostHated.isPlayer() && BAIUM_RIM_ZONE.isInsideZone(currentMostHated))
					{
						if (archangel.getTarget() != currentMostHated)
						{
							archangel.clearAggroList();
						}
						
						addAttackDesire(archangel, currentMostHated);
					}
					else
					{
						boolean foundTarget = false;
						for (Playable candidate : World.getInstance().getVisibleObjectsInRange(archangel, Playable.class, 1000))
						{
							if (BAIUM_RIM_ZONE.isInsideZone(candidate) && !candidate.isDead())
							{
								if (archangel.getTarget() != candidate)
								{
									archangel.clearAggroList();
								}
								
								addAttackDesire(archangel, candidate);
								foundTarget = true;
								break;
							}
						}
						
						if (!foundTarget)
						{
							if (archangel.isInsideRadius3D(_baiumInstance, 40))
							{
								if (archangel.getTarget() != _baiumInstance)
								{
									archangel.clearAggroList();
								}
								
								addAttackDesire(archangel, _baiumInstance);
							}
							else
							{
								archangel.getAI().setIntention(Intention.FOLLOW, _baiumInstance);
							}
						}
					}
					
					startQuestTimer(EVENT_SELECT_TARGET, SELECT_TARGET_INTERVAL_MILLIS, npc, null);
				}
				break;
			}
			// Inactivity handling, self-heal checks and status reset.
			case EVENT_CHECK_ATTACK:
			{
				if ((npc != null) && ((_lastAttackTime + INACTIVITY_LIMIT_MILLIS) < System.currentTimeMillis()))
				{
					cancelQuestTimers(EVENT_SELECT_TARGET);
					notifyEvent(EVENT_CLEAR_ZONE, null, null);
					addSpawn(BAIUM_STONE_NPC_ID, LOCATION_BAIUM_SPAWN, false, 0);
					setStatus(STATUS_ALIVE);
				}
				else if (npc != null)
				{
					final long healCheckTime = _lastAttackTime + 300000;
					final double healThresholdHp = npc.getMaxHp() * 0.75;
					
					if ((healCheckTime < System.currentTimeMillis()) && (npc.getCurrentHp() < healThresholdHp))
					{
						npc.setTarget(npc);
						npc.doCast(SKILL_HEAL_OF_BAIUM.getSkill());
					}
					
					startQuestTimer(EVENT_CHECK_ATTACK, CHECK_ATTACK_INTERVAL_MILLIS, npc, null);
				}
				break;
			}
			case EVENT_CLEAR_STATUS:
			{
				setStatus(STATUS_ALIVE);
				addSpawn(BAIUM_STONE_NPC_ID, LOCATION_BAIUM_SPAWN, false, 0);
				break;
			}
			case EVENT_CLEAR_ZONE:
			{
				for (Creature creature : BAIUM_RIM_ZONE.getCharactersInside())
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
						notifyEvent(EVENT_EXIT, null, creature.asPlayer());
					}
				}
				break;
			}
			// GM control utilities for Baium.
			case EVENT_RESPAWN_BAIUM:
			{
				if (getStatus() == STATUS_DEAD)
				{
					setRespawn(0);
					cancelQuestTimer(EVENT_CLEAR_STATUS, null, null);
					cancelQuestTimers(EVENT_CLEAR_ZONE);
					
					notifyEvent(EVENT_CLEAR_STATUS, null, null);
				}
				else if (player != null)
				{
					player.sendMessage(getClass().getSimpleName() + ": You cannot respawn Baium while Baium is alive!");
				}
				break;
			}
			case EVENT_ABORT_FIGHT:
			{
				if (getStatus() == STATUS_IN_FIGHT)
				{
					_baiumInstance = null;
					notifyEvent(EVENT_CLEAR_ZONE, null, null);
					notifyEvent(EVENT_CLEAR_STATUS, null, null);
					
					if (player != null)
					{
						player.sendMessage(getClass().getSimpleName() + ": Aborting fight!");
					}
				}
				else if (player != null)
				{
					player.sendMessage(getClass().getSimpleName() + ": You cannot abort attack right now!");
				}
				
				cancelQuestTimers(EVENT_CHECK_ATTACK);
				cancelQuestTimers(EVENT_SELECT_TARGET);
				break;
			}
			case EVENT_DESPAWN_MINIONS:
			{
				if (getStatus() == STATUS_IN_FIGHT)
				{
					for (Creature creature : BAIUM_RIM_ZONE.getCharactersInside())
					{
						if ((creature != null) && creature.isNpc() && (creature.getId() == ARCHANGEL_NPC_ID))
						{
							creature.deleteMe();
						}
					}
					
					if (player != null)
					{
						player.sendMessage(getClass().getSimpleName() + ": All archangels has been deleted!");
					}
				}
				else if (player != null)
				{
					player.sendMessage(getClass().getSimpleName() + ": You cannot despawn archangels right now!");
				}
				break;
			}
			// Periodic Baium skill management.
			case EVENT_MANAGE_SKILLS:
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
	 * Handles attacks against Baium and archangels.<br>
	 * Updates internal aggro tables, triggers counter-skills based on HP thresholds and refreshes activity time.
	 * @param npc The attacked NPC.
	 * @param attacker The attacker.
	 * @param damage The dealt damage.
	 * @param isSummon Flag indicating summon attack.
	 * @param skill The used skill, may be {@code null}.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		_lastAttackTime = System.currentTimeMillis();
		final int npcId = npc.getId();
		
		if (npcId == BAIUM_NPC_ID)
		{
			if ((attacker.getMountType() == MountType.STRIDER) && !attacker.isAffectedBySkill(SKILL_ANTI_STRIDER.getSkillId()) && !npc.isSkillDisabled(SKILL_ANTI_STRIDER.getSkill()))
			{
				npc.setTarget(attacker);
				npc.doCast(SKILL_ANTI_STRIDER.getSkill());
			}
			
			final double healthRatio = npc.getCurrentHp() / npc.getMaxHp();
			
			if (skill == null)
			{
				refreshAiParams(attacker, npc, damage * 1000);
			}
			else if (healthRatio < 0.25)
			{
				refreshAiParams(attacker, npc, (damage / 3) * 100);
			}
			else if (healthRatio < 0.5)
			{
				refreshAiParams(attacker, npc, damage * 20);
			}
			else if (healthRatio < 0.75)
			{
				refreshAiParams(attacker, npc, damage * 10);
			}
			else
			{
				refreshAiParams(attacker, npc, (damage / 3) * 20);
			}
			
			manageSkills(npc);
		}
		else if (npcId == ARCHANGEL_NPC_ID)
		{
			final Attackable archangel = npc.asAttackable();
			final Creature mostHated = archangel.getMostHated();
			
			if ((getRandom(100) < 10) && archangel.checkDoCastConditions(SKILL_SPEAR_ATTACK.getSkill()))
			{
				if ((mostHated != null) && (npc.calculateDistance3D(mostHated) < 1000) && BAIUM_RIM_ZONE.isCharacterInZone(mostHated))
				{
					archangel.setTarget(mostHated);
					archangel.doCast(SKILL_SPEAR_ATTACK.getSkill());
				}
				else if (BAIUM_RIM_ZONE.isCharacterInZone(attacker))
				{
					archangel.setTarget(attacker);
					archangel.doCast(SKILL_SPEAR_ATTACK.getSkill());
				}
			}
			
			final double archangelHpRatio = npc.getCurrentHp() / npc.getMaxHp();
			if ((getRandom(100) < 5) && (archangelHpRatio < 0.5) && archangel.checkDoCastConditions(SKILL_ANGEL_HEAL.getSkill()))
			{
				npc.setTarget(npc);
				npc.doCast(SKILL_ANGEL_HEAL.getSkill());
			}
		}
	}
	
	/**
	 * Handles Baium death, teleport cubes spawn and respawn scheduling.<br>
	 * Marks Baium as dead, persists next respawn time, spawns teleport cubes and initiates rooftop cleanup.
	 * @param npc The killed NPC.
	 * @param killer The player who killed.
	 * @param isSummon Flag for summon kill.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (!BAIUM_RIM_ZONE.isCharacterInZone(killer))
		{
			return;
		}
		
		setStatus(STATUS_DEAD);
		addSpawn(TELEPORT_CUBE_NPC_ID, LOCATION_TELEPORT_CUBE, false, TELEPORT_CUBE_LIFETIME_MILLIS);
		BAIUM_RIM_ZONE.broadcastPacket(new PlaySound("BS01_D"));
		
		final long baseIntervalMillis = GrandBossConfig.BAIUM_SPAWN_INTERVAL * 3600000L;
		final long randomRangeMillis = GrandBossConfig.BAIUM_SPAWN_RANDOM * 3600000L;
		final long respawnDelayMillis = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
		final long nextRespawnTime = System.currentTimeMillis() + respawnDelayMillis;
		
		LOGGER.info("Baium will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
		
		setRespawn(respawnDelayMillis);
		startQuestTimer(EVENT_CLEAR_STATUS, respawnDelayMillis, null, null);
		startQuestTimer(EVENT_CLEAR_ZONE, TELEPORT_CUBE_LIFETIME_MILLIS, null, null);
		cancelQuestTimer(EVENT_CHECK_ATTACK, npc, null);
		cancelQuestTimers(EVENT_SELECT_TARGET);
	}
	
	/**
	 * Handles visible creatures around Baium and updates custom aggro slots.<br>
	 * Prioritizes healer-type classes and scales custom aggro weights using Baium current HP ratio.
	 * @param npc The Baium NPC.
	 * @param creature The seen creature.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (!BAIUM_RIM_ZONE.isInsideZone(creature) || (creature.isNpc() && (creature.getId() == BAIUM_STONE_NPC_ID)))
		{
			return;
		}
		
		final double healthRatio = npc.getCurrentHp() / npc.getMaxHp();
		
		if (creature.isInCategory(CategoryType.CLERIC_GROUP))
		{
			if (healthRatio < 0.25)
			{
				refreshAiParams(creature, npc, 10000);
			}
			else if (healthRatio < 0.5)
			{
				refreshAiParams(creature, npc, 10000, 6000);
			}
			else if (healthRatio < 0.75)
			{
				refreshAiParams(creature, npc, 10000, 3000);
			}
			else
			{
				refreshAiParams(creature, npc, 10000, 2000);
			}
		}
		else
		{
			refreshAiParams(creature, npc, 10000, 1000);
		}
		
		manageSkills(npc);
	}
	
	/**
	 * Handles spell completion for Baium and schedules skill management.<br>
	 * Also relocates Baium back to the rooftop arena if pulled or dragged outside the configured zone.
	 * @param npc The caster NPC.
	 * @param player The casting player.
	 * @param skill The finished skill.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (npc != null)
		{
			startQuestTimer(EVENT_MANAGE_SKILLS, 1000, npc, null);
		}
		
		if ((npc != null) && (_baiumInstance != null) && !_baiumInstance.isDead() && !BAIUM_RIM_ZONE.isCharacterInZone(npc))
		{
			_baiumInstance.teleToLocation(LOCATION_BAIUM_SPAWN);
		}
	}
	
	/**
	 * Ensures any active Baium instance is removed safely when the script is unloaded by the engine.<br>
	 * Deletes Baium NPC instance if still spawned and avoids leaving stale references in the world.
	 * @param removeFromList Flag to remove script from list.
	 */
	@Override
	public void unload(boolean removeFromList)
	{
		if (_baiumInstance != null)
		{
			_baiumInstance.deleteMe();
		}
		
		super.unload(removeFromList);
	}
	
	/**
	 * Updates Baium aggro using incoming damage as base value for both damage and aggro weight.<br>
	 * @param attacker The attacker.
	 * @param npc The Baium NPC.
	 * @param damage The damage value.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage)
	{
		refreshAiParams(attacker, npc, damage, damage);
	}
	
	/**
	 * Updates Baium aggro table using separate damage and aggro scores.<br>
	 * Maintains three tracked attackers and replaces the one with minimal weight.
	 * @param attacker The attacker.
	 * @param npc The Baium NPC.
	 * @param damage The damage value.
	 * @param aggro The aggro value.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage, int aggro)
	{
		final int newAggroValue = damage + getRandom(3000);
		final int aggroThreshold = aggro + 1000;
		final NpcVariables variables = npc.getVariables();
		
		for (int slotIndex = 0; slotIndex < 3; slotIndex++)
		{
			if (attacker == variables.getObject("c_quest" + slotIndex, Creature.class))
			{
				if (variables.getInt("i_quest" + slotIndex) < aggroThreshold)
				{
					variables.set("i_quest" + slotIndex, newAggroValue);
				}
				return;
			}
		}
		
		final int minIndex = MathUtil.getIndexOfMinValue(variables.getInt("i_quest0"), variables.getInt("i_quest1"), variables.getInt("i_quest2"));
		variables.set("i_quest" + minIndex, newAggroValue);
		variables.set("c_quest" + minIndex, attacker);
	}
	
	/**
	 * Gets current Baium status from boss manager.<br>
	 * @return The status flag.
	 */
	private int getStatus()
	{
		return GrandBossManager.getInstance().getStatus(BAIUM_NPC_ID);
	}
	
	/**
	 * Registers Baium instance in the grand boss manager.<br>
	 * @param grandBoss The Baium instance.
	 */
	private void addBoss(GrandBoss grandBoss)
	{
		GrandBossManager.getInstance().addBoss(grandBoss);
	}
	
	/**
	 * Updates Baium status in boss manager.<br>
	 * @param status The new status.
	 */
	private void setStatus(int status)
	{
		GrandBossManager.getInstance().setStatus(BAIUM_NPC_ID, status);
	}
	
	/**
	 * Stores Baium respawn time in boss manager.<br>
	 * @param respawnDelayMillis The respawn delay.
	 */
	private void setRespawn(long respawnDelayMillis)
	{
		GrandBossManager.getInstance().getStatSet(BAIUM_NPC_ID).set("respawn_time", System.currentTimeMillis() + respawnDelayMillis);
	}
	
	/**
	 * Selects and casts Baium skills based on HP ratio and aggro slots.<br>
	 * Uses a three-slot table to select the primary target.
	 * @param npc The Baium NPC.
	 */
	private void manageSkills(Npc npc)
	{
		if (npc.isCastingNow() || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		final NpcVariables variables = npc.getVariables();
		for (int slotIndex = 0; slotIndex < 3; slotIndex++)
		{
			final Creature attacker = variables.getObject("c_quest" + slotIndex, Creature.class);
			if ((attacker == null) || (npc.calculateDistance3D(attacker) > 9000) || attacker.isDead())
			{
				variables.set("i_quest" + slotIndex, 0);
			}
		}
		
		final int maxIndex = MathUtil.getIndexOfMaxValue(variables.getInt("i_quest0"), variables.getInt("i_quest1"), variables.getInt("i_quest2"));
		final Creature primaryTarget = variables.getObject("c_quest" + maxIndex, Creature.class);
		final int currentAggro = variables.getInt("i_quest" + maxIndex);
		
		if ((currentAggro > 0) && (getRandom(100) < 70))
		{
			variables.set("i_quest" + maxIndex, 500);
		}
		
		SkillHolder skillToCast = null;
		if ((primaryTarget != null) && !primaryTarget.isDead())
		{
			final double healthRatio = npc.getCurrentHp() / npc.getMaxHp();
			
			if (healthRatio > 0.75)
			{
				if (getRandom(100) < 10)
				{
					skillToCast = SKILL_ENERGY_WAVE;
				}
				else if (getRandom(100) < 10)
				{
					skillToCast = SKILL_EARTHQUAKE;
				}
				else
				{
					skillToCast = SKILL_BAIUM_ATTACK;
				}
			}
			else if (healthRatio > 0.5)
			{
				if (getRandom(100) < 10)
				{
					skillToCast = SKILL_GROUP_HOLD;
				}
				else if (getRandom(100) < 10)
				{
					skillToCast = SKILL_ENERGY_WAVE;
				}
				else if (getRandom(100) < 10)
				{
					skillToCast = SKILL_EARTHQUAKE;
				}
				else
				{
					skillToCast = SKILL_BAIUM_ATTACK;
				}
			}
			else if (healthRatio > 0.25)
			{
				if (getRandom(100) < 10)
				{
					skillToCast = SKILL_THUNDERBOLT;
				}
				else if (getRandom(100) < 10)
				{
					skillToCast = SKILL_GROUP_HOLD;
				}
				else if (getRandom(100) < 10)
				{
					skillToCast = SKILL_ENERGY_WAVE;
				}
				else if (getRandom(100) < 10)
				{
					skillToCast = SKILL_EARTHQUAKE;
				}
				else
				{
					skillToCast = SKILL_BAIUM_ATTACK;
				}
			}
			else if (getRandom(100) < 10)
			{
				skillToCast = SKILL_THUNDERBOLT;
			}
			else if (getRandom(100) < 10)
			{
				skillToCast = SKILL_GROUP_HOLD;
			}
			else if (getRandom(100) < 10)
			{
				skillToCast = SKILL_ENERGY_WAVE;
			}
			else if (getRandom(100) < 10)
			{
				skillToCast = SKILL_EARTHQUAKE;
			}
			else
			{
				skillToCast = SKILL_BAIUM_ATTACK;
			}
		}
		
		if ((skillToCast != null) && npc.checkDoCastConditions(skillToCast.getSkill()))
		{
			npc.setTarget(primaryTarget);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	/**
	 * Gets a random valid player inside the Baium zone around given NPC.<br>
	 * @param npc The Baium NPC reference.
	 * @return The chosen player or {@code null}.
	 */
	private Player getRandomPlayer(Npc npc)
	{
		for (Player candidate : World.getInstance().getVisibleObjectsInRange(npc, Player.class, 2000))
		{
			if ((candidate != null) && BAIUM_RIM_ZONE.isInsideZone(candidate) && !candidate.isDead())
			{
				return candidate;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new Baium();
	}
}
