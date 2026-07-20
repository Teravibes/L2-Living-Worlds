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
package ai.bosses.Sailren;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;

/**
 * Sailren raid boss AI handler.<br>
 * Controls party entry, staged dinosaur spawns, cinematic sequence and timeout logic.
 * <ul>
 * <li>Enforces quest item requirement and single-party access.</li>
 * <li>Spawns Velociraptor -> Pterosaur -> Tyrannosaurus -> Sailren.</li>
 * <li>Handles inactivity checks, instance timeout and respawn scheduling.</li>
 * </ul>
 * This raid is managed as a normal raid using a {@link NoRestartZone} and global variables instead of the grand boss subsystem.
 * @author BazookaRpm
 */
public class Sailren extends Script
{
	// Logging.
	private static final Logger LOGGER = Logger.getLogger(Sailren.class.getName());
	
	// NPC identifiers.
	private static final int STATUE_NPC_ID = 32109; // Shilen's Stone Statue.
	private static final int MOVIE_NPC_ID = 32110; // Invisible NPC used for camera.
	private static final int SAILREN_NPC_ID = 29065;
	private static final int VELOCIRAPTOR_NPC_ID = 22218;
	private static final int PTEROSAUR_NPC_ID = 22199;
	private static final int TREX_NPC_ID = 22217;
	private static final int TELEPORT_CUBE_NPC_ID = 32107;
	
	// Item identifiers.
	private static final int GAZKH_ITEM_ID = 8784;
	
	// Skill holders.
	private static final SkillHolder CINEMATIC_ANIMATION_SKILL = new SkillHolder(5090, 1);
	
	// Zone configuration.
	private static final int SAILREN_ZONE_ID = 70049;
	private static final NoRestartZone SAILREN_ZONE = ZoneManager.getInstance().getZoneById(SAILREN_ZONE_ID, NoRestartZone.class);
	
	// Location constants.
	private static final Location LAIR_CENTER = new Location(27549, -6638, -2008);
	
	private static final Location VELOCIRAPTOR_BASE_LOCATION = new Location(27313, -6766, -1975);
	private static final int VELOCIRAPTOR_SPAWN_RANDOM = 150;
	
	private static final Location PTEROSAUR_SPAWN_LOCATION = new Location(27313, -6766, -1975);
	private static final Location TREX_SPAWN_LOCATION = new Location(27313, -6766, -1975);
	
	private static final Location TELEPORT_CUBE_SPAWN_LOCATION = new Location(27644, -6638, -2008);
	
	private static final int TELEPORT_STATUE_RADIUS = 1000;
	
	// Time and delay configuration.
	private static final long RESPAWN_DELAY_MILLIS = 3600000L; // 1 hour respawn, retail-like.
	private static final long MAX_FIGHT_DURATION_MILLIS = 3200L * 1000L; // ~53 minutes, retail-like.
	private static final long INACTIVITY_LIMIT_MILLIS = 600000L; // 10 minutes without attacks.
	private static final long INACTIVITY_CHECK_INTERVAL_MILLIS = 120000L; // 2 minutes.
	private static final long INITIAL_VELOCIRAPTOR_DELAY_MILLIS = 60000L; // 1 minute after entry.
	private static final long TREX_TO_SAILREN_DELAY_MILLIS = 180000L; // 3 minutes after T-Rex death.
	private static final long TELEPORT_CUBE_LIFETIME_MILLIS = 300000L; // 5 minutes.
	private static final long SAILREN_INTRO_INVUL_DELAY_MILLIS = 24600L;
	private static final long CAMERA_STEP_ONE_DELAY_MILLIS = 4100L;
	private static final long CAMERA_STEP_INTERVAL_MILLIS = 3000L;
	private static final long CAMERA_STEP_FIVE_TO_SIX_DELAY_MILLIS = 7000L;
	private static final long ANIMATION_INTERVAL_MILLIS = 2000L;
	private static final long MOVIE_NPC_LIFETIME_MILLIS = 26000L; // Respetar valor original.
	
	// Event identifiers.
	private static final String EVENT_ENTER = "ENTER";
	private static final String EVENT_EXIT = "EXIT";
	private static final String EVENT_SPAWN_VELOCIRAPTORS = "SPAWN_VELOCIRAPTORS";
	private static final String EVENT_SPAWN_SAILREN = "SPAWN_SAILREN";
	private static final String EVENT_ANIMATION = "ANIMATION";
	private static final String EVENT_CAMERA_STEP_ONE = "CAMERA_1";
	private static final String EVENT_CAMERA_STEP_TWO = "CAMERA_2";
	private static final String EVENT_CAMERA_STEP_THREE = "CAMERA_3";
	private static final String EVENT_CAMERA_STEP_FOUR = "CAMERA_4";
	private static final String EVENT_CAMERA_STEP_FIVE = "CAMERA_5";
	private static final String EVENT_CAMERA_STEP_SIX = "CAMERA_6";
	private static final String EVENT_ATTACK = "ATTACK";
	private static final String EVENT_CLEAR_STATUS = "CLEAR_STATUS";
	private static final String EVENT_TIMEOUT = "TIME_OUT";
	private static final String EVENT_CHECK_INACTIVITY = "CHECK_ATTACK";
	
	// Global variable key.
	private static final String GLOBAL_VAR_RESPAWN_TIME = "SailrenRespawn";
	
	// Status flags.
	private static final byte STATUS_ALIVE = 0;
	private static final byte STATUS_IN_FIGHT = 1;
	private static final byte STATUS_DEAD = 2;
	
	// Runtime state.
	private static byte _raidStatus = STATUS_ALIVE;
	private static int _killCount = 0;
	private static long _lastAttackTime = 0;
	
	/**
	 * Initializes Sailren AI, validates the zone and restores respawn state from global variables.<br>
	 * If the respawn time is in the future, the raid is marked as dead and a status clear timer is scheduled.<br>
	 * Otherwise, Sailren is considered available.
	 */
	public Sailren()
	{
		// Zone must exist; otherwise, disable AI cleanly.
		if (SAILREN_ZONE == null)
		{
			LOGGER.warning(Sailren.class.getSimpleName() + ": Sailren zone " + SAILREN_ZONE_ID + " not found. AI disabled.");
			return;
		}
		
		addStartNpc(STATUE_NPC_ID, TELEPORT_CUBE_NPC_ID);
		addTalkId(STATUE_NPC_ID, TELEPORT_CUBE_NPC_ID);
		addFirstTalkId(STATUE_NPC_ID);
		addKillId(VELOCIRAPTOR_NPC_ID, PTEROSAUR_NPC_ID, TREX_NPC_ID, SAILREN_NPC_ID);
		addAttackId(VELOCIRAPTOR_NPC_ID, PTEROSAUR_NPC_ID, TREX_NPC_ID, SAILREN_NPC_ID);
		
		final long storedRespawnTime = GlobalVariablesManager.getInstance().getLong(GLOBAL_VAR_RESPAWN_TIME, 0);
		final long remainingDelay = storedRespawnTime - System.currentTimeMillis();
		if (remainingDelay > 0)
		{
			_raidStatus = STATUS_DEAD;
			startQuestTimer(EVENT_CLEAR_STATUS, remainingDelay, null, null);
		}
		else
		{
			_raidStatus = STATUS_ALIVE;
		}
	}
	
	/**
	 * Handles quest events and internal timers for Sailren.
	 * <ul>
	 * <li>HTML events from the statue.</li>
	 * <li>Party entry/exit.</li>
	 * <li>Dinosaur spawn steps and Sailren cinematic.</li>
	 * <li>Camera sequence and animation loop for the intro.</li>
	 * <li>Attack unlock, timeout and inactivity checks.</li>
	 * </ul>
	 * @param event the event name
	 * @param npc the NPC associated with the event (may be {@code null} for some timers)
	 * @param player the player triggering the event, when applicable
	 * @return HTML filename to send to the client, or the result of the parent implementation
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "32109-01.html":
			case "32109-01a.html":
			case "32109-02a.html":
			case "32109-03a.html":
			{
				return event;
			}
			case EVENT_ENTER:
			{
				return handleEnterEvent(npc, player);
			}
			case EVENT_EXIT:
			{
				if (player != null)
				{
					player.teleToLocation(TeleportWhereType.TOWN);
				}
				break;
			}
			case EVENT_SPAWN_VELOCIRAPTORS:
			{
				spawnVelociraptors();
				break;
			}
			case EVENT_SPAWN_SAILREN:
			{
				spawnSailrenWithCinematic();
				break;
			}
			case EVENT_ANIMATION:
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(CINEMATIC_ANIMATION_SKILL.getSkill());
					startQuestTimer(EVENT_ANIMATION, ANIMATION_INTERVAL_MILLIS, npc, null);
				}
				break;
			}
			case EVENT_CAMERA_STEP_ONE:
			{
				if (npc != null)
				{
					SAILREN_ZONE.broadcastPacket(new SpecialCamera(npc, 100, 180, 30, 3000, 1500, 20000, 0, 50, 1, 0, 0));
					startQuestTimer(EVENT_CAMERA_STEP_TWO, CAMERA_STEP_INTERVAL_MILLIS, npc, null);
				}
				break;
			}
			case EVENT_CAMERA_STEP_TWO:
			{
				if (npc != null)
				{
					SAILREN_ZONE.broadcastPacket(new SpecialCamera(npc, 150, 270, 25, 3000, 1500, 20000, 0, 30, 1, 0, 0));
					startQuestTimer(EVENT_CAMERA_STEP_THREE, CAMERA_STEP_INTERVAL_MILLIS, npc, null);
				}
				break;
			}
			case EVENT_CAMERA_STEP_THREE:
			{
				if (npc != null)
				{
					SAILREN_ZONE.broadcastPacket(new SpecialCamera(npc, 160, 360, 20, 3000, 1500, 20000, 10, 15, 1, 0, 0));
					startQuestTimer(EVENT_CAMERA_STEP_FOUR, CAMERA_STEP_INTERVAL_MILLIS, npc, null);
				}
				break;
			}
			case EVENT_CAMERA_STEP_FOUR:
			{
				if (npc != null)
				{
					SAILREN_ZONE.broadcastPacket(new SpecialCamera(npc, 160, 450, 10, 3000, 1500, 20000, 0, 10, 1, 0, 0));
					startQuestTimer(EVENT_CAMERA_STEP_FIVE, CAMERA_STEP_INTERVAL_MILLIS, npc, null);
				}
				break;
			}
			case EVENT_CAMERA_STEP_FIVE:
			{
				if (npc != null)
				{
					SAILREN_ZONE.broadcastPacket(new SpecialCamera(npc, 160, 560, 0, 3000, 1500, 20000, 0, 10, 1, 0, 0));
					startQuestTimer(EVENT_CAMERA_STEP_SIX, CAMERA_STEP_FIVE_TO_SIX_DELAY_MILLIS, npc, null);
				}
				break;
			}
			case EVENT_CAMERA_STEP_SIX:
			{
				if (npc != null)
				{
					SAILREN_ZONE.broadcastPacket(new SpecialCamera(npc, 70, 560, 0, 500, 1500, 7000, -15, 20, 1, 0, 0));
				}
				break;
			}
			case EVENT_ATTACK:
			{
				if ((npc != null) && npc.isRaid())
				{
					npc.setInvul(false);
					npc.setImmobilized(false);
				}
				break;
			}
			case EVENT_CLEAR_STATUS:
			{
				_raidStatus = STATUS_ALIVE;
				_killCount = 0;
				_lastAttackTime = 0;
				break;
			}
			case EVENT_TIMEOUT:
			{
				handleTimeout();
				break;
			}
			case EVENT_CHECK_INACTIVITY:
			{
				handleInactivityCheck();
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles party entry validation and teleport to Sailren's lair.<br>
	 * Verifies party presence, raid status, party leader and quest item, then:
	 * <ul>
	 * <li>Consumes Gazkh from the party leader.</li>
	 * <li>Teleports nearby party members to the lair center.</li>
	 * <li>Schedules Velociraptor spawn, fight timeout and inactivity checks.</li>
	 * </ul>
	 * @param npc the statue NPC
	 * @param player the player requesting entry (usually the party leader)
	 * @return HTML to show on the statue interaction
	 */
	private String handleEnterEvent(Npc npc, Player player)
	{
		String html = null;
		final Party party = (player != null) ? player.getParty() : null;
		
		if (party == null)
		{
			html = "32109-01.html";
		}
		else if (_raidStatus == STATUS_DEAD)
		{
			html = "32109-04.html";
		}
		else if (_raidStatus == STATUS_IN_FIGHT)
		{
			html = "32109-05.html";
		}
		else if (!party.isLeader(player))
		{
			html = "32109-03.html";
		}
		else if (!hasQuestItems(player, GAZKH_ITEM_ID))
		{
			html = "32109-02.html";
		}
		else
		{
			// Valid entry: consume Gazkh and move the party inside.
			takeItems(player, GAZKH_ITEM_ID, 1);
			_raidStatus = STATUS_IN_FIGHT;
			_killCount = 0;
			_lastAttackTime = System.currentTimeMillis();
			
			for (Player member : party.getMembers())
			{
				if ((member != null) && member.isInsideRadius3D(npc, TELEPORT_STATUE_RADIUS))
				{
					member.teleToLocation(LAIR_CENTER);
				}
			}
			
			startQuestTimer(EVENT_SPAWN_VELOCIRAPTORS, INITIAL_VELOCIRAPTOR_DELAY_MILLIS, null, null);
			startQuestTimer(EVENT_TIMEOUT, MAX_FIGHT_DURATION_MILLIS, null, null);
			startQuestTimer(EVENT_CHECK_INACTIVITY, INACTIVITY_CHECK_INTERVAL_MILLIS, null, null);
		}
		
		return html;
	}
	
	/**
	 * Spawns the initial Velociraptor wave inside Sailren's lair.<br>
	 * Three Velociraptors are spawned around {@link #VELOCIRAPTOR_BASE_LOCATION} with a small random offset to avoid stacking.
	 */
	private void spawnVelociraptors()
	{
		for (int index = 0; index < 3; index++)
		{
			final int spawnX = VELOCIRAPTOR_BASE_LOCATION.getX() + getRandom(VELOCIRAPTOR_SPAWN_RANDOM);
			final int spawnY = VELOCIRAPTOR_BASE_LOCATION.getY() + getRandom(VELOCIRAPTOR_SPAWN_RANDOM);
			addSpawn(VELOCIRAPTOR_NPC_ID, spawnX, spawnY, VELOCIRAPTOR_BASE_LOCATION.getZ(), 0, false, 0);
		}
	}
	
	/**
	 * Spawns Sailren at the lair center and plays the introductory cinematic.<br>
	 * Sailren is initially invulnerable and immobilized until the cinematic finishes. A movie NPC is used as the camera pivot, and camera packets are broadcasted to all players inside the zone.
	 */
	private void spawnSailrenWithCinematic()
	{
		final Npc sailrenNpc = addSpawn(SAILREN_NPC_ID, LAIR_CENTER, false, 0);
		if (!(sailrenNpc instanceof RaidBoss))
		{
			LOGGER.warning(Sailren.class.getSimpleName() + ": Failed to spawn Sailren raid boss (ID: " + SAILREN_NPC_ID + ").");
			return;
		}
		
		final RaidBoss sailren = (RaidBoss) sailrenNpc;
		final Npc movieNpc = addSpawn(MOVIE_NPC_ID, sailren.getX(), sailren.getY(), sailren.getZ() + 30, 0, false, MOVIE_NPC_LIFETIME_MILLIS);
		if (movieNpc == null)
		{
			LOGGER.warning(Sailren.class.getSimpleName() + ": Failed to spawn Sailren cinematic NPC (ID: " + MOVIE_NPC_ID + ").");
			return;
		}
		
		sailren.setInvul(true);
		sailren.setImmobilized(true);
		
		// Initial camera shot.
		SAILREN_ZONE.broadcastPacket(new SpecialCamera(movieNpc, 60, 110, 30, 4000, 1500, 20000, 0, 65, 1, 0, 0));
		
		// Start cinematic sequence.
		startQuestTimer(EVENT_ATTACK, SAILREN_INTRO_INVUL_DELAY_MILLIS, sailren, null);
		startQuestTimer(EVENT_ANIMATION, ANIMATION_INTERVAL_MILLIS, movieNpc, null);
		startQuestTimer(EVENT_CAMERA_STEP_ONE, CAMERA_STEP_ONE_DELAY_MILLIS, movieNpc, null);
	}
	
	/**
	 * Handles global timeout of the Sailren encounter.<br>
	 * This method:
	 * <ul>
	 * <li>Resets raid status flags.</li>
	 * <li>Teleports all players in the zone back to town.</li>
	 * <li>Removes all NPCs present inside the zone.</li>
	 * </ul>
	 * It is invoked either when the fight duration expires or when an inactivity timeout is triggered.
	 */
	private void handleTimeout()
	{
		if (_raidStatus == STATUS_IN_FIGHT)
		{
			_raidStatus = STATUS_ALIVE;
		}
		
		if (SAILREN_ZONE != null)
		{
			for (Creature creature : SAILREN_ZONE.getCharactersInside())
			{
				if (creature == null)
				{
					continue;
				}
				
				if (creature.isPlayer())
				{
					creature.teleToLocation(TeleportWhereType.TOWN);
				}
				else if (creature.isNpc())
				{
					creature.deleteMe();
				}
			}
		}
		
		_killCount = 0;
		_lastAttackTime = 0;
	}
	
	/**
	 * Periodically checks for player inactivity inside the lair.<br>
	 * If no attacks have occurred during {@link #INACTIVITY_LIMIT_MILLIS}, the fight is forcefully terminated by triggering {@link #handleTimeout()}. If players are still active, the check is rescheduled.
	 */
	private void handleInactivityCheck()
	{
		if (_raidStatus != STATUS_IN_FIGHT)
		{
			return;
		}
		
		if ((SAILREN_ZONE == null) || SAILREN_ZONE.getPlayersInside().isEmpty())
		{
			// No players inside: no reschedule.
			return;
		}
		
		if ((_lastAttackTime + INACTIVITY_LIMIT_MILLIS) < System.currentTimeMillis())
		{
			cancelQuestTimer(EVENT_TIMEOUT, null, null);
			cancelQuestTimer(EVENT_CHECK_INACTIVITY, null, null);
			notifyEvent(EVENT_TIMEOUT, null, null);
		}
		else
		{
			startQuestTimer(EVENT_CHECK_INACTIVITY, INACTIVITY_CHECK_INTERVAL_MILLIS, null, null);
		}
	}
	
	/**
	 * Handles attack events against Sailren and its staged dinosaurs.<br>
	 * The only logic required here is updating the last attack timestamp to drive the inactivity timeout checks.
	 * @param npc the NPC being attacked
	 * @param attacker the attacking player
	 * @param damage the inflicted damage
	 * @param isSummon flag indicating whether the attack came from a summon
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((attacker != null) && (SAILREN_ZONE != null) && SAILREN_ZONE.isCharacterInZone(attacker))
		{
			_lastAttackTime = System.currentTimeMillis();
		}
	}
	
	/**
	 * Handles kill events for all actors involved in the Sailren encounter.<br>
	 * Implements the staged progression:
	 * <ul>
	 * <li>Three Velociraptors → spawn Pterosaur.</li>
	 * <li>Pterosaur → spawn Tyrannosaurus.</li>
	 * <li>Tyrannosaurus → delayed Sailren spawn with cinematic.</li>
	 * <li>Sailren → spawn teleport cube, schedule respawn and cleanup timers.</li>
	 * </ul>
	 * @param npc the NPC that has been killed
	 * @param killer the killing player
	 * @param isSummon flag indicating whether the kill came from a summon
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if ((killer == null) || (SAILREN_ZONE == null) || !SAILREN_ZONE.isCharacterInZone(killer))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case SAILREN_NPC_ID:
			{
				_raidStatus = STATUS_DEAD;
				
				addSpawn(TELEPORT_CUBE_NPC_ID, TELEPORT_CUBE_SPAWN_LOCATION, false, TELEPORT_CUBE_LIFETIME_MILLIS);
				
				final long respawnTime = System.currentTimeMillis() + RESPAWN_DELAY_MILLIS;
				GlobalVariablesManager.getInstance().set(GLOBAL_VAR_RESPAWN_TIME, respawnTime);
				
				cancelQuestTimer(EVENT_CHECK_INACTIVITY, null, null);
				cancelQuestTimer(EVENT_TIMEOUT, null, null);
				
				startQuestTimer(EVENT_CLEAR_STATUS, RESPAWN_DELAY_MILLIS, null, null);
				startQuestTimer(EVENT_TIMEOUT, TELEPORT_CUBE_LIFETIME_MILLIS, null, null);
				break;
			}
			case VELOCIRAPTOR_NPC_ID:
			{
				_killCount++;
				if (_killCount == 3)
				{
					final Npc pterosaur = addSpawn(PTEROSAUR_NPC_ID, PTEROSAUR_SPAWN_LOCATION, false, 0);
					if (pterosaur != null)
					{
						addAttackDesire(pterosaur, killer);
					}
					_killCount = 0;
				}
				break;
			}
			case PTEROSAUR_NPC_ID:
			{
				final Npc trex = addSpawn(TREX_NPC_ID, TREX_SPAWN_LOCATION, false, 0);
				if (trex != null)
				{
					addAttackDesire(trex, killer);
				}
				break;
			}
			case TREX_NPC_ID:
			{
				startQuestTimer(EVENT_SPAWN_SAILREN, TREX_TO_SAILREN_DELAY_MILLIS, null, null);
				break;
			}
		}
	}
	
	/**
	 * Ensures proper cleanup if the script is unloaded while the encounter is active.<br>
	 * If Sailren is currently in fight status, a timeout event is triggered to reset the lair and teleport players out before the script is fully unloaded.
	 * @param removeFromList whether the script should be removed from the script list
	 */
	@Override
	public void unload(boolean removeFromList)
	{
		if (_raidStatus == STATUS_IN_FIGHT)
		{
			LOGGER.info(Sailren.class.getSimpleName() + ": Unloading script while Sailren is active. Forcing instance cleanup.");
			notifyEvent(EVENT_TIMEOUT, null, null);
		}
		
		super.unload(removeFromList);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new Sailren();
	}
}
