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
package ai.bosses.DrChaos;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;

/**
 * AI for Dr. Chaos boss in Pavel's Ruins.<br>
 * Handles paranoia behavior, cinematic transformation and respawn logic.
 * <ul>
 * <li>Detects players staying near Dr. Chaos and escalates paranoia over time.</li>
 * <li>Triggers a cinematic sequence and spawns the Chaos Golem when he becomes angry.</li>
 * <li>Persists boss state and respawn time using the GrandBossManager table.</li>
 * <li>Handles spawning and despawning of both Dr. Chaos and his Chaos Golem form.</li>
 * </ul>
 * @author BazookaRpm
 */
public class DrChaos extends Script
{
	// NPC identifiers.
	private static final int DOCTOR_CHAOS_NPC_ID = 32033;
	private static final int CHAOS_GOLEM_NPC_ID = 25512;
	
	// Boss states.
	private static final byte STATUS_NORMAL = 0; // Dr. Chaos is in NPC form.
	private static final byte STATUS_CRAZY = 1; // Dr. Chaos is in Chaos Golem form.
	private static final byte STATUS_DEAD = 2; // Dr. Chaos is dead and waiting for respawn.
	
	// Locations.
	private static final Location CHAOS_SPAWN_LOCATION = new Location(96320, -110912, -3328, 8191);
	private static final Location CHAOS_RETURN_LOCATION = new Location(96320, -110912, -3328);
	private static final Location CHAOS_MOVE_LOCATION = new Location(95928, -110671, -3340);
	private static final Location CHAOS_BOX_LOCATION = new Location(96323, -110914, -3328);
	private static final Location CHAOS_GOLEM_SPAWN_LOCATION = new Location(96080, -110822, -3343);
	
	// Timers and delays.
	private static final int GOLEM_DESPAWN_CHECK_DELAY_MS = 60000;
	private static final int GOLEM_INACTIVITY_LIMIT_MS = 1800000; // 30 minutes.
	private static final int PARANOIA_TICK_DELAY_MS = 1000;
	private static final int DISTANCE_CHECK_DELAY_MS = 10000;
	
	// Paranoia configuration.
	private static final int PARANOIA_INITIAL_TIME_SECONDS = 30;
	private static final int PARANOIA_FIRST_MESSAGE_THRESHOLD = 15;
	private static final int PARANOIA_SECOND_MESSAGE_UPPER_BOUND = 20;
	private static final int PARANOIA_THIRD_MESSAGE_UPPER_BOUND = 10;
	private static final int PARANOIA_RANGE = 500;
	
	// Distance limits.
	private static final int CHAOS_RETURN_DISTANCE = 2000;
	private static final int DISTANCE_CHECK_LIMIT = 10000;
	
	// Respawn configuration.
	private static final int BASE_RESPAWN_HOURS = 36;
	private static final int RANDOM_RESPAWN_VARIATION_HOURS = 24;
	private static final int MILLIS_PER_HOUR = 3600000;
	
	// Runtime state.
	private long _lastGolemAttackTime = 0;
	private int _paranoiaCountdown;
	
	/**
	 * Instantiates the Dr. Chaos AI and restores boss state from persistent storage.
	 */
	public DrChaos()
	{
		addFirstTalkId(DOCTOR_CHAOS_NPC_ID);
		addSpawnId(DOCTOR_CHAOS_NPC_ID);
		addKillId(CHAOS_GOLEM_NPC_ID);
		addAttackId(CHAOS_GOLEM_NPC_ID);
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		final StatSet info = bossManager.getStatSet(CHAOS_GOLEM_NPC_ID);
		final int status = bossManager.getStatus(CHAOS_GOLEM_NPC_ID);
		
		if (status == STATUS_DEAD)
		{
			if (info != null)
			{
				final long remaining = info.getLong("respawn_time") - System.currentTimeMillis();
				if (remaining > 0)
				{
					startQuestTimer("reset_drchaos", remaining, null, null, false);
				}
				else
				{
					addSpawn(DOCTOR_CHAOS_NPC_ID, CHAOS_SPAWN_LOCATION, false, 0, false);
					bossManager.setStatus(CHAOS_GOLEM_NPC_ID, STATUS_NORMAL);
				}
			}
			else
			{
				// Missing StatSet for DEAD status, fallback to NORMAL spawn.
				addSpawn(DOCTOR_CHAOS_NPC_ID, CHAOS_SPAWN_LOCATION, false, 0, false);
				bossManager.setStatus(CHAOS_GOLEM_NPC_ID, STATUS_NORMAL);
			}
		}
		else if (status == STATUS_CRAZY)
		{
			if (info != null)
			{
				final int locX = info.getInt("loc_x");
				final int locY = info.getInt("loc_y");
				final int locZ = info.getInt("loc_z");
				final int heading = info.getInt("heading");
				final int currentHp = info.getInt("currentHP");
				final int currentMp = info.getInt("currentMP");
				
				final GrandBoss golem = (GrandBoss) addSpawn(CHAOS_GOLEM_NPC_ID, new Location(locX, locY, locZ, heading), false, 0, false);
				bossManager.addBoss(golem);
				
				golem.setCurrentHpMp(currentHp, currentMp);
				golem.setRunning();
				
				_lastGolemAttackTime = System.currentTimeMillis();
				startQuestTimer("golem_despawn", GOLEM_DESPAWN_CHECK_DELAY_MS, golem, null, true);
			}
			else
			{
				// Missing StatSet for CRAZY status, fallback to NORMAL spawn.
				addSpawn(DOCTOR_CHAOS_NPC_ID, CHAOS_SPAWN_LOCATION, false, 0, false);
				bossManager.setStatus(CHAOS_GOLEM_NPC_ID, STATUS_NORMAL);
			}
		}
		else
		{
			addSpawn(DOCTOR_CHAOS_NPC_ID, CHAOS_SPAWN_LOCATION, false, 0, false);
		}
	}
	
	/**
	 * Handles scheduled events and quest timers for Dr. Chaos and the Chaos Golem.
	 * @param event The event name.
	 * @param npc The NPC associated with the event.
	 * @param player The player associated with the event.
	 * @return The HTML text to return, if any.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "reset_drchaos":
			{
				GrandBossManager.getInstance().setStatus(CHAOS_GOLEM_NPC_ID, STATUS_NORMAL);
				addSpawn(DOCTOR_CHAOS_NPC_ID, CHAOS_SPAWN_LOCATION, false, 0, false);
				break;
			}
			case "golem_despawn":
			{
				// Despawn the Chaos Golem after 30 minutes of inactivity since last hit.
				if ((npc != null) && (npc.getId() == CHAOS_GOLEM_NPC_ID) && ((_lastGolemAttackTime + GOLEM_INACTIVITY_LIMIT_MS) < System.currentTimeMillis()))
				{
					final Npc chaos = addSpawn(DOCTOR_CHAOS_NPC_ID, CHAOS_SPAWN_LOCATION, false, 0, false);
					GrandBossManager.getInstance().setStatus(CHAOS_GOLEM_NPC_ID, STATUS_NORMAL);
					
					// Cancel the despawn timer associated with the golem.
					cancelQuestTimer("golem_despawn", chaos, null);
					cancelQuestTimers("DISTANCE_CHECK");
					
					npc.deleteMe();
				}
				break;
			}
			case "1":
			{
				npc.broadcastPacket(new SocialAction(npc.getObjectId(), 2));
				npc.broadcastPacket(new SpecialCamera(npc, 1, -200, 15, 5500, 1000, 13500, 0, 0, 0, 0, 0));
				break;
			}
			case "2":
			{
				npc.broadcastPacket(new SocialAction(npc.getObjectId(), 3));
				break;
			}
			case "3":
			{
				npc.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
				break;
			}
			case "4":
			{
				npc.broadcastPacket(new SpecialCamera(npc, 1, -150, 10, 3500, 1000, 5000, 0, 0, 0, 0, 0));
				npc.getAI().setIntention(Intention.MOVE_TO, CHAOS_MOVE_LOCATION);
				break;
			}
			case "5":
			{
				final GrandBoss golem = (GrandBoss) addSpawn(CHAOS_GOLEM_NPC_ID, CHAOS_GOLEM_SPAWN_LOCATION, false, 0, false);
				GrandBossManager.getInstance().addBoss(golem);
				startQuestTimer("DISTANCE_CHECK", DISTANCE_CHECK_DELAY_MS, golem, null, true);
				golem.broadcastPacket(new SpecialCamera(npc, 30, 200, 20, 6000, 700, 8000, 0, 0, 0, 0, 0));
				golem.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
				golem.broadcastPacket(new PlaySound(1, "Rm03_A", 0, 0, 0, 0, 0));
				
				_lastGolemAttackTime = System.currentTimeMillis();
				startQuestTimer("golem_despawn", GOLEM_DESPAWN_CHECK_DELAY_MS, golem, null, true);
				
				npc.deleteMe();
				break;
			}
			case "paranoia_activity":
			{
				if (GrandBossManager.getInstance().getStatus(CHAOS_GOLEM_NPC_ID) == STATUS_NORMAL)
				{
					for (Player nearbyPlayer : World.getInstance().getVisibleObjectsInRange(npc, Player.class, PARANOIA_RANGE))
					{
						if (nearbyPlayer.isDead())
						{
							continue;
						}
						
						_paranoiaCountdown -= 1;
						
						if (_paranoiaCountdown == PARANOIA_FIRST_MESSAGE_THRESHOLD)
						{
							npc.broadcastSay(ChatType.NPC_GENERAL, "How dare you trespass into my territory! Have you no fear?");
						}
						
						if (_paranoiaCountdown <= 0)
						{
							crazyMidgetBecomesAngry(npc);
						}
					}
				}
				
				if (npc.calculateDistance2D(CHAOS_RETURN_LOCATION) > CHAOS_RETURN_DISTANCE)
				{
					if (npc.isAttackable())
					{
						npc.asAttackable().clearAggroList();
					}
					npc.getAI().setIntention(Intention.MOVE_TO, CHAOS_RETURN_LOCATION);
				}
				break;
			}
			case "DISTANCE_CHECK":
			{
				if ((npc == null) || npc.isDead())
				{
					cancelQuestTimers("DISTANCE_CHECK");
				}
				else if (npc.calculateDistance2D(npc.getSpawn()) > DISTANCE_CHECK_LIMIT)
				{
					if (npc.isAttackable())
					{
						npc.asAttackable().clearAggroList();
					}
					npc.teleToLocation(npc.getSpawn(), false);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles first talk interactions, advancing Dr. Chaos paranoia and dialog.
	 * @param npc The NPC being spoken to.
	 * @param player The player initiating the conversation.
	 * @return The HTML dialog content.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmlText = "";
		if (GrandBossManager.getInstance().getStatus(CHAOS_GOLEM_NPC_ID) == STATUS_NORMAL)
		{
			_paranoiaCountdown -= 1 + getRandom(5);
			
			if ((_paranoiaCountdown > PARANOIA_SECOND_MESSAGE_UPPER_BOUND) && (_paranoiaCountdown <= PARANOIA_INITIAL_TIME_SECONDS))
			{
				htmlText = "<html><body>Doctor Chaos:<br>What?! Who are you? How did you come here?<br>You really look suspicious... Aren't those filthy members of Black Anvil guild send you? No? Mhhhhh... I don't trust you!</body></html>";
			}
			else if ((_paranoiaCountdown > PARANOIA_THIRD_MESSAGE_UPPER_BOUND) && (_paranoiaCountdown <= PARANOIA_SECOND_MESSAGE_UPPER_BOUND))
			{
				htmlText = "<html><body>Doctor Chaos:<br>Why are you standing here? Don't you see it's a private propertie? Don't look at him with those eyes... Did you smile?! Don't make fun of me! He will ... destroy ... you ... if you continue!</body></html>";
			}
			else if ((_paranoiaCountdown > 0) && (_paranoiaCountdown <= PARANOIA_THIRD_MESSAGE_UPPER_BOUND))
			{
				htmlText = "<html><body>Doctor Chaos:<br>I know why you are here, traitor! He discovered your plans! You are assassin ... sent by the Black Anvil guild! But you won't kill the Emperor of Evil!</body></html>";
			}
			else if (_paranoiaCountdown <= 0)
			{
				crazyMidgetBecomesAngry(npc);
			}
		}
		
		return htmlText;
	}
	
	/**
	 * Initializes paranoia countdown and distance checks when Dr. Chaos spawns.
	 * @param npc The spawned Dr. Chaos NPC.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		_paranoiaCountdown = PARANOIA_INITIAL_TIME_SECONDS;
		
		startQuestTimer("paranoia_activity", PARANOIA_TICK_DELAY_MS, npc, null, true);
		
		cancelQuestTimer("DISTANCE_CHECK", npc, null);
		startQuestTimer("DISTANCE_CHECK", DISTANCE_CHECK_DELAY_MS, npc, null, true);
	}
	
	/**
	 * Handles Chaos Golem death, schedules Dr. Chaos respawn and stores respawn time.
	 * @param npc The Chaos Golem NPC that died.
	 * @param player The killer player.
	 * @param isPet True if the killer is a pet.
	 */
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		cancelQuestTimer("golem_despawn", npc, null);
		npc.broadcastSay(ChatType.NPC_GENERAL, "Urggh! You will pay dearly for this insult.");
		
		final long respawnTime = (BASE_RESPAWN_HOURS + getRandom(-RANDOM_RESPAWN_VARIATION_HOURS, RANDOM_RESPAWN_VARIATION_HOURS)) * MILLIS_PER_HOUR;
		
		final GrandBossManager bossManager = GrandBossManager.getInstance();
		bossManager.setStatus(CHAOS_GOLEM_NPC_ID, STATUS_DEAD);
		startQuestTimer("reset_drchaos", respawnTime, null, null, false);
		
		StatSet info = bossManager.getStatSet(CHAOS_GOLEM_NPC_ID);
		if (info == null)
		{
			info = new StatSet();
		}
		info.set("respawn_time", System.currentTimeMillis() + respawnTime);
		bossManager.setStatSet(CHAOS_GOLEM_NPC_ID, info);
		
		cancelQuestTimers("DISTANCE_CHECK");
	}
	
	/**
	 * Handles Chaos Golem attacks, with random taunts during combat and inactivity reset.
	 * @param npc The Chaos Golem NPC.
	 * @param victim The attacked player.
	 * @param damage The inflicted damage.
	 * @param isPet True if the attacker is a pet.
	 */
	@Override
	public void onAttack(Npc npc, Player victim, int damage, boolean isPet)
	{
		if (npc == null)
		{
			return;
		}
		
		_lastGolemAttackTime = System.currentTimeMillis();
		
		final int chance = getRandom(300);
		if (chance < 3)
		{
			String message;
			switch (chance)
			{
				case 0:
				{
					message = "Bwah-ha-ha! Your doom is at hand! Behold the Ultra Secret Super Weapon!";
					break;
				}
				case 1:
				{
					message = "Foolish, insignificant creatures! How dare you challenge me!";
					break;
				}
				default:
				{
					message = "I see that none will challenge me now!";
					break;
				}
			}
			
			npc.broadcastSay(ChatType.NPC_GENERAL, message);
		}
	}
	
	/**
	 * Launches the full cinematic sequence and transforms Dr. Chaos into his Chaos Golem form.
	 * @param npc The Dr. Chaos NPC.
	 */
	private void crazyMidgetBecomesAngry(Npc npc)
	{
		if (GrandBossManager.getInstance().getStatus(CHAOS_GOLEM_NPC_ID) == STATUS_NORMAL)
		{
			GrandBossManager.getInstance().setStatus(CHAOS_GOLEM_NPC_ID, STATUS_CRAZY);
			
			cancelQuestTimer("paranoia_activity", npc, null);
			
			npc.getAI().setIntention(Intention.MOVE_TO, CHAOS_BOX_LOCATION);
			npc.broadcastSay(ChatType.NPC_GENERAL, "Fools! Why haven't you fled yet? Prepare to learn a lesson!");
			
			startQuestTimer("1", 2000, npc, null, false);
			startQuestTimer("2", 4000, npc, null, false);
			startQuestTimer("3", 6500, npc, null, false);
			startQuestTimer("4", 12500, npc, null, false);
			startQuestTimer("5", 17000, npc, null, false);
		}
	}
	
	public static void main(String[] args)
	{
		new DrChaos();
	}
}
