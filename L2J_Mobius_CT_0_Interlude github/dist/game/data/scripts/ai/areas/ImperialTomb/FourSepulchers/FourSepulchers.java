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
package ai.areas.ImperialTomb.FourSepulchers;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.EffectZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

import quests.Q00620_FourGoblets.Q00620_FourGoblets;

/**
 * Four Selpuchers AI
 * @author Mobius
 */
public class FourSepulchers extends Script implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FourSepulchers.class.getName());
	
	// NPCs
	private static final int CONQUEROR_MANAGER = 31921;
	private static final int EMPEROR_MANAGER = 31922;
	private static final int GREAT_SAGES_MANAGER = 31923;
	private static final int JUDGE_MANAGER = 31924;
	private static final int MYSTERIOUS_CHEST = 31468;
	private static final int KEY_CHEST = 31467;
	private static final int ROOM_3_VICTIM = 18150;
	private static final int ROOM_3_CHEST_REWARDER = 18158;
	private static final int ROOM_4_CHARM_1 = 18196;
	private static final int ROOM_4_CHARM_2 = 18197;
	private static final int ROOM_4_CHARM_3 = 18198;
	private static final int ROOM_4_CHARM_4 = 18199;
	private static final int ROOM_5_STATUE_GUARD = 18232;
	private static final int ROOM_6_REWARD_CHEST = 18256;
	private static final int CONQUEROR_BOSS = 25346;
	private static final int EMPEROR_BOSS = 25342;
	private static final int GREAT_SAGES_BOSS = 25339;
	private static final int JUDGE_BOSS = 25349;
	private static final int TELEPORTER = 31452;
	// @formatter:off
	private static final int[] FIRST_TALK_NPCS =
	{
		TELEPORTER,
		31453, 31454, 31919, 31920, 31925, 31926, 31927, 31928,
		31929, 31930, 31931, 31932, 31933, 31934, 31935, 31936,
		31937, 31938, 31939, 31940, 31941, 31942, 31943, 31944
	};
	// @formatter:on
	private static final int[] CHEST_REWARD_MONSTERS =
	{
		18120, // room 1
		ROOM_3_CHEST_REWARDER,
		18177, // room 4
		18212, // room 5 - wave 2
	};
	
	// Items
	private static final int ENTRANCE_PASS = 7075;
	private static final int USED_PASS = 7261;
	private static final int CHAPEL_KEY = 7260;
	private static final int ANTIQUE_BROOCH = 7262;
	
	// Locations
	private static final Map<Integer, Location> START_HALL_SPAWNS = new HashMap<>();
	static
	{
		START_HALL_SPAWNS.put(CONQUEROR_MANAGER, new Location(181632, -85587, -7218));
		START_HALL_SPAWNS.put(EMPEROR_MANAGER, new Location(179963, -88978, -7218));
		START_HALL_SPAWNS.put(GREAT_SAGES_MANAGER, new Location(173217, -86132, -7218));
		START_HALL_SPAWNS.put(JUDGE_MANAGER, new Location(175608, -82296, -7218));
	}
	
	// Zones
	private static final int CONQUEROR_ZONE = 200221;
	private static final int EMPEROR_ZONE = 200222;
	private static final int GREAT_SAGES_ZONE = 200224;
	private static final int JUDGE_ZONE = 200223;
	private static final Map<Integer, Integer> MANAGER_ZONES = new HashMap<>();
	static
	{
		MANAGER_ZONES.put(CONQUEROR_MANAGER, CONQUEROR_ZONE);
		MANAGER_ZONES.put(EMPEROR_MANAGER, EMPEROR_ZONE);
		MANAGER_ZONES.put(GREAT_SAGES_MANAGER, GREAT_SAGES_ZONE);
		MANAGER_ZONES.put(JUDGE_MANAGER, JUDGE_ZONE);
	}
	
	// Spawns
	private static List<int[]> ROOM_SPAWN_DATA = new ArrayList<>();
	private static final Map<Integer, List<Npc>> STORED_MONSTER_SPAWNS = new HashMap<>();
	static
	{
		STORED_MONSTER_SPAWNS.put(1, new CopyOnWriteArrayList<>());
		STORED_MONSTER_SPAWNS.put(2, new CopyOnWriteArrayList<>());
		STORED_MONSTER_SPAWNS.put(3, new CopyOnWriteArrayList<>());
		STORED_MONSTER_SPAWNS.put(4, new CopyOnWriteArrayList<>());
	}
	// @formatter:off
	private static final int[][] CHEST_SPAWN_LOCATIONS =
	{
		// sepulcherId, roomNumber, npcLocX, npcLocY, npcLocZ, npcLocHeading
		{1, 1, 182074, -85579, -7216, 32768},
		{1, 2, 183868, -85577, -7216, 32768},
		{1, 3, 185681, -85573, -7216, 32768},
		{1, 4, 187498, -85566, -7216, 32768},
		{1, 5, 189306, -85571, -7216, 32768},
		{2, 1, 180375, -88968, -7216, 32768},
		{2, 2, 182151, -88962, -7216, 32768},
		{2, 3, 183960, -88964, -7216, 32768},
		{2, 4, 185792, -88966, -7216, 32768},
		{2, 5, 187625, -88953, -7216, 32768},
		{3, 1, 173218, -85703, -7216, 49152},
		{3, 2, 173206, -83929, -7216, 49152},
		{3, 3, 173208, -82085, -7216, 49152},
		{3, 4, 173191, -80290, -7216, 49152},
		{3, 5, 173198, -78465, -7216, 49152},
		{4, 1, 175601, -81905, -7216, 49152},
		{4, 2, 175619, -80094, -7216, 49152},
		{4, 3, 175608, -78268, -7216, 49152},
		{4, 4, 175588, -76472, -7216, 49152},
		{4, 5, 175594, -74655, -7216, 49152},
	};
	
	// Doors
	private static final int[][] DOORS =
	{
		// sepulcherId, waveNumber, doorId
		{1, 2, 25150012}, {1, 3, 25150013}, {1, 4, 25150014}, {1, 5, 25150015}, {1, 7, 25150016},
		{2, 2, 25150002}, {2, 3, 25150003}, {2, 4, 25150004}, {2, 5, 25150005}, {2, 7, 25150006},
		{3, 2, 25150032}, {3, 3, 25150033}, {3, 4, 25150034}, {3, 5, 25150035}, {3, 7, 25150036},
		{4, 2, 25150022}, {4, 3, 25150023}, {4, 4, 25150024}, {4, 5, 25150025}, {4, 7, 25150026},
	};
	// @formatter:on
	
	// Skill
	private static final SkillHolder PETRIFY = new SkillHolder(4616, 1);
	private static final Map<Integer, Integer> CHARM_SKILLS = new HashMap<>();
	static
	{
		CHARM_SKILLS.put(ROOM_4_CHARM_1, 4146);
		CHARM_SKILLS.put(ROOM_4_CHARM_2, 4145);
		CHARM_SKILLS.put(ROOM_4_CHARM_3, 4148);
		CHARM_SKILLS.put(ROOM_4_CHARM_4, 4624);
	}
	
	// Misc
	private static final Map<Integer, String> CHARM_MSG = new HashMap<>();
	static
	{
		CHARM_MSG.put(ROOM_4_CHARM_1, "The P. Atk. reduction device has now been destroyed.");
		CHARM_MSG.put(ROOM_4_CHARM_2, "The Defense reduction device has been destroyed.");
		CHARM_MSG.put(ROOM_4_CHARM_3, "The poison device has now been destroyed.");
		CHARM_MSG.put(ROOM_4_CHARM_4, "The poison device has now been destroyed."); // TODO: THE_HP_REGENERATION_REDUCTION_DEVICE_WILL_BE_ACTIVATED_IN_3_MINUTES2
	}
	private static final String[] VICTIM_MSG =
	{
		"Help me!",
		"Don't miss!",
		"Keep pushing!",
	};
	private static final String[] MANAGER_MESSAGES =
	{
		"You may now enter the Sepulcher.",
		"If you place your hand on the stone statue in front of each sepulcher, you will be able to enter."
	};
	private static final Map<Integer, Integer> STORED_PROGRESS = new HashMap<>();
	static
	{
		STORED_PROGRESS.put(1, 1);
		STORED_PROGRESS.put(2, 1);
		STORED_PROGRESS.put(3, 1);
		STORED_PROGRESS.put(4, 1);
	}
	private static final int PARTY_MEMBER_COUNT = 4;
	private static final int ENTRY_TIME = 55; // Minute for entry.
	private static final int EVENT_TIME = 50; // Minutes for the event.
	
	private FourSepulchers()
	{
		load();
		addFirstTalkId(CONQUEROR_MANAGER, EMPEROR_MANAGER, GREAT_SAGES_MANAGER, JUDGE_MANAGER, MYSTERIOUS_CHEST, KEY_CHEST);
		addTalkId(CONQUEROR_MANAGER, EMPEROR_MANAGER, GREAT_SAGES_MANAGER, JUDGE_MANAGER, MYSTERIOUS_CHEST, KEY_CHEST);
		addFirstTalkId(FIRST_TALK_NPCS);
		addTalkId(FIRST_TALK_NPCS);
		addKillId(CHEST_REWARD_MONSTERS);
		addKillId(ROOM_3_VICTIM, ROOM_4_CHARM_1, ROOM_4_CHARM_2, ROOM_4_CHARM_3, ROOM_4_CHARM_4, ROOM_6_REWARD_CHEST, CONQUEROR_BOSS, EMPEROR_BOSS, GREAT_SAGES_BOSS, JUDGE_BOSS);
		addSpawnId(ROOM_3_VICTIM, ROOM_4_CHARM_1, ROOM_4_CHARM_2, ROOM_4_CHARM_3, ROOM_4_CHARM_4, ROOM_5_STATUE_GUARD, ROOM_6_REWARD_CHEST, CONQUEROR_BOSS, EMPEROR_BOSS, GREAT_SAGES_BOSS, JUDGE_BOSS);
		
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		if (calendar.get(Calendar.MINUTE) >= ENTRY_TIME)
		{
			calendar.add(Calendar.HOUR_OF_DAY, 1);
		}
		
		calendar.set(Calendar.MINUTE, ENTRY_TIME);
		startQuestTimer("ANNOUNCE", calendar.getTimeInMillis() - System.currentTimeMillis(), null, null);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		
		switch (event)
		{
			case "ENTER":
			{
				final QuestState qs = player.getQuestState(Q00620_FourGoblets.class.getSimpleName());
				if (qs == null)
				{
					return getNoQuestMsg(player);
				}
				
				if (qs.isStarted())
				{
					tryEnter(npc, player);
					return null;
				}
				break;
			}
			case "OPEN_GATE":
			{
				final QuestState qs = player.getQuestState(Q00620_FourGoblets.class.getSimpleName());
				if (qs == null)
				{
					return getNoQuestMsg(player);
				}
				
				if (qs.isStarted() && npc.isScriptValue(0))
				{
					if (hasQuestItems(player, CHAPEL_KEY))
					{
						npc.setScriptValue(1);
						takeItems(player, CHAPEL_KEY, -1);
						final int sepulcherId = getSepulcherId(player);
						final int currentWave = STORED_PROGRESS.get(sepulcherId) + 1;
						STORED_PROGRESS.put(sepulcherId, currentWave); // update progress
						for (int[] doorInfo : DOORS)
						{
							if ((doorInfo[0] == sepulcherId) && (doorInfo[1] == currentWave))
							{
								openDoor(doorInfo[2], 0);
								ThreadPool.schedule(() -> closeDoor(doorInfo[2], 0), 15000);
								break;
							}
						}
						
						if (currentWave < 7)
						{
							spawnMysteriousChest(player);
						}
						else
						{
							spawnNextWave(player);
						}
						
						npc.broadcastSay(ChatType.NPC_GENERAL, "The monsters have spawned!");
					}
					else
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setFile(player, "data/scripts/ai/areas/ImperialTomb/FourSepulchers/Gatekeeper-no.html");
						html.replace("%npcname%", npc.getName());
						player.sendPacket(html);
					}
					
					return null;
				}
				
				htmltext = getNoQuestMsg(player); // TODO: Replace with proper html?
				break;
			}
			case "SPAWN_MYSTERIOUS_CHEST":
			{
				spawnMysteriousChest(player);
				return null;
			}
			case "VICTIM_FLEE":
			{
				if ((npc != null) && !npc.isDead())
				{
					final Location destination = GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), npc.getSpawn().getLocation().getX() + getRandom(-400, 400), npc.getSpawn().getLocation().getY() + getRandom(-400, 400), npc.getZ(), npc.getInstanceId());
					if (LocationUtil.calculateDistance(npc, npc.getSpawn().getLocation(), false, false) < 600)
					{
						npc.getAI().setIntention(Intention.MOVE_TO, destination);
					}
					
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(VICTIM_MSG));
					startQuestTimer("VICTIM_FLEE", 3000, npc, null, false);
				}
				
				return null;
			}
			case "REMOVE_PETRIFY":
			{
				npc.stopSkillEffects(SkillFinishType.REMOVED, PETRIFY.getSkillId());
				// npc.setTargetable(true);
				npc.setInvul(false);
				return null;
			}
			case "WAVE_DEFEATED_CHECK":
			{
				final int sepulcherId = getSepulcherId(player);
				final int currentWave = STORED_PROGRESS.get(sepulcherId);
				Location lastLocation = null;
				for (Npc spawn : STORED_MONSTER_SPAWNS.get(sepulcherId))
				{
					lastLocation = spawn.getLocation();
					if (spawn.isDead())
					{
						STORED_MONSTER_SPAWNS.get(sepulcherId).remove(spawn);
					}
				}
				
				if (STORED_MONSTER_SPAWNS.get(sepulcherId).isEmpty())
				{
					if (currentWave == 2)
					{
						if (getRandomBoolean())
						{
							spawnNextWave(player);
						}
						else
						{
							spawnKeyChest(player, lastLocation);
						}
					}
					else if (currentWave == 5)
					{
						STORED_PROGRESS.put(sepulcherId, currentWave + 1);
						spawnNextWave(player);
					}
				}
				else if (sepulcherId > 0)
				{
					startQuestTimer("WAVE_DEFEATED_CHECK", 5000, null, player, false);
				}
				
				return null;
			}
			case "ANNOUNCE":
			{
				for (int managerId : MANAGER_ZONES.keySet())
				{
					Npc manager = World.getInstance().getNpc(managerId);
					if (manager != null)
					{
						for (String message : MANAGER_MESSAGES)
						{
							manager.broadcastSay(ChatType.NPC_SHOUT, message);
						}
					}
					else
					{
						LOGGER.warning("Four Sepulcher Manager with ID " + managerId + " is missing!");
					}
				}
				
				final Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				if (calendar.get(Calendar.MINUTE) >= ENTRY_TIME)
				{
					calendar.add(Calendar.HOUR_OF_DAY, 1);
				}
				
				calendar.set(Calendar.MINUTE, ENTRY_TIME);
				startQuestTimer("ANNOUNCE", calendar.getTimeInMillis() - System.currentTimeMillis(), null, null);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc == null)
		{
			return null;
		}
		
		if (npc.getId() == MYSTERIOUS_CHEST)
		{
			if (npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				npc.deleteMe();
				spawnNextWave(player);
			}
			
			return null;
		}
		
		if (npc.getId() == KEY_CHEST)
		{
			if (npc.isScriptValue(0))
			{
				npc.setScriptValue(1);
				npc.deleteMe();
				giveItems(player, CHAPEL_KEY, 1);
			}
			
			return null;
		}
		
		return npc.getId() + ".html";
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
		if (npc.getId() == ROOM_3_VICTIM)
		{
			npc.disableCoreAI(true);
			npc.setRunning();
			startQuestTimer("VICTIM_FLEE", 1000, npc, null, false);
		}
		
		if (npc.getId() == ROOM_5_STATUE_GUARD)
		{
			npc.setTarget(npc);
			npc.doCast(PETRIFY.getSkill());
			npc.asAttackable().setCanReturnToSpawnPoint(false);
			// npc.setTargetable(false);
			// npc.setInvul(true);
			cancelQuestTimer("REMOVE_PETRIFY", npc, null);
			startQuestTimer("REMOVE_PETRIFY", 5 * 60 * 1000, npc, null, false); // 5 minutes
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case ROOM_3_VICTIM:
			{
				addSpawn(ROOM_3_CHEST_REWARDER, npc);
				break;
			}
			case ROOM_4_CHARM_1:
			case ROOM_4_CHARM_2:
			case ROOM_4_CHARM_3:
			case ROOM_4_CHARM_4:
			{
				for (ZoneType zone : ZoneManager.getInstance().getZones(killer))
				{
					if ((zone instanceof EffectZone) && (((EffectZone) zone).getSkillLevel(CHARM_SKILLS.get(npc.getId())) > 0))
					{
						zone.setEnabled(false);
						break;
					}
				}
				
				npc.broadcastSay(ChatType.NPC_GENERAL, CHARM_MSG.get(npc.getId()));
				break;
			}
			case CONQUEROR_BOSS:
			case EMPEROR_BOSS:
			case GREAT_SAGES_BOSS:
			case JUDGE_BOSS:
			{
				final int sepulcherId = getSepulcherId(killer);
				final int currentWave = STORED_PROGRESS.get(sepulcherId);
				STORED_PROGRESS.put(sepulcherId, currentWave + 1);
				
				if ((killer.getParty() != null) && (sepulcherId > 0))
				{
					for (Player mem : killer.getParty().getMembers())
					{
						if (LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, killer, mem, true))
						{
							final QuestState qs = mem.getQuestState(Q00620_FourGoblets.class.getSimpleName());
							if ((qs != null) && qs.isStarted() && !hasAtLeastOneQuestItem(mem, ANTIQUE_BROOCH) && !hasAtLeastOneQuestItem(mem, 7255 + sepulcherId))
							{
								giveItems(mem, 7255 + sepulcherId, 1);
								mem.sendPacket(QuestSound.ITEMSOUND_QUEST_MIDDLE.getPacket());
							}
						}
					}
				}
				
				spawnNextWave(killer);
				
				addSpawn(TELEPORTER, npc, true, 0, false);
				break;
			}
			case ROOM_6_REWARD_CHEST:
			{
				npc.dropItem(killer, 57, getRandom(300, 1300));
				break;
			}
			default:
			{
				spawnKeyChest(killer, npc.getLocation());
				break;
			}
		}
	}
	
	private void tryEnter(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		final Party party = player.getParty();
		if (party == null)
		{
			showHtmlFile(player, npcId + "-SP.html", npc, null);
			return;
		}
		
		if (!party.isLeader(player))
		{
			showHtmlFile(player, npcId + "-NL.html", npc, null);
			return;
		}
		
		if (!ZoneManager.getInstance().getZoneById(MANAGER_ZONES.get(npcId)).getPlayersInside().isEmpty())
		{
			showHtmlFile(player, npcId + "-FULL.html", npc, null);
			return;
		}
		
		if (!player.isInParty() || (party.getMemberCount() < PARTY_MEMBER_COUNT))
		{
			showHtmlFile(player, npcId + "-SP.html", npc, null);
			return;
		}
		
		// Get current time in minutes for entering.
		final int currentMinute = (int) ((System.currentTimeMillis() / 1000 / 60) % 60);
		if ((currentMinute >= 0) && (currentMinute < ENTRY_TIME))
		{
			showHtmlFile(player, npcId + "-NE.html", npc, null);
			return;
		}
		
		// Set the timer to spawn the chest when event start.
		final int timeUntilNextHour = (60 - currentMinute) * 60 * 1000;
		startQuestTimer("SPAWN_MYSTERIOUS_CHEST", timeUntilNextHour, npc, player, false);
		
		for (Player mem : party.getMembers())
		{
			final QuestState qs = mem.getQuestState(Q00620_FourGoblets.class.getSimpleName());
			if ((qs == null) || (!qs.isStarted() && !qs.isCompleted()))
			{
				showHtmlFile(player, npcId + "-NS.html", npc, mem);
				return;
			}
			
			if (!hasQuestItems(mem, ENTRANCE_PASS))
			{
				showHtmlFile(player, npcId + "-SE.html", npc, mem);
				return;
			}
			
			if (player.getWeightPenalty() >= 3)
			{
				mem.sendPacket(SystemMessageId.PROGRESS_IN_A_QUEST_IS_POSSIBLE_ONLY_WHEN_YOUR_INVENTORY_S_WEIGHT_AND_VOLUME_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
				return;
			}
		}
		
		final int sepulcherId = getSepulcherId(player);
		
		// Delete any existing spawns.
		for (Creature creature : ZoneManager.getInstance().getZoneById(MANAGER_ZONES.get(npcId)).getCharactersInside())
		{
			if (creature.isMonster() || creature.isRaid() || (creature.isNpc() && ((creature.asNpc().getId() == MYSTERIOUS_CHEST) || (creature.asNpc().getId() == KEY_CHEST) || (creature.asNpc().getId() == TELEPORTER))))
			{
				creature.deleteMe();
			}
		}
		
		// Disable EffectZones.
		for (int[] spawnInfo : CHEST_SPAWN_LOCATIONS)
		{
			if ((spawnInfo[0] == sepulcherId) && (spawnInfo[1] == 4))
			{
				for (ZoneType zone : ZoneManager.getInstance().getZones(spawnInfo[2], spawnInfo[3], spawnInfo[4]))
				{
					if (zone instanceof EffectZone)
					{
						zone.setEnabled(false);
					}
				}
				break;
			}
		}
		
		// Close all doors.
		for (int[] doorInfo : DOORS)
		{
			if (doorInfo[0] == sepulcherId)
			{
				closeDoor(doorInfo[2], 0);
			}
		}
		
		// Teleport players inside.
		final List<Player> members = new ArrayList<>();
		for (Player mem : party.getMembers())
		{
			if (LocationUtil.checkIfInRange(700, player, mem, true))
			{
				members.add(mem);
			}
		}
		
		for (Player mem : members)
		{
			mem.teleToLocation(START_HALL_SPAWNS.get(npcId), 80);
			takeItems(mem, ENTRANCE_PASS, 1);
			takeItems(mem, CHAPEL_KEY, -1);
			if (!hasQuestItems(mem, ANTIQUE_BROOCH))
			{
				giveItems(mem, USED_PASS, 1);
			}
		}
		
		showHtmlFile(player, npcId + "-OK.html", npc, null);
		
		// Kick all players when/if time is over.
		ThreadPool.schedule(() -> ZoneManager.getInstance().getZoneById(MANAGER_ZONES.get(npcId)).oustAllPlayers(), EVENT_TIME * 60 * 1000);
		
		// Init progress.
		STORED_PROGRESS.put(sepulcherId, 1); // Start from 1.
	}
	
	private void spawnNextWave(Player player)
	{
		final int sepulcherId = getSepulcherId(player);
		final int currentWave = STORED_PROGRESS.get(sepulcherId);
		for (int[] spawnInfo : ROOM_SPAWN_DATA)
		{
			if ((spawnInfo[0] == sepulcherId) && (spawnInfo[1] == currentWave))
			{
				STORED_MONSTER_SPAWNS.get(sepulcherId).add(addSpawn(spawnInfo[2], spawnInfo[3], spawnInfo[4], spawnInfo[5], spawnInfo[6], false, 0));
			}
		}
		
		if (currentWave == 4)
		{
			for (ZoneType zone : ZoneManager.getInstance().getZones(player))
			{
				if (zone instanceof EffectZone)
				{
					zone.setEnabled(true);
				}
			}
		}
		
		if ((currentWave == 2) || (currentWave == 5))
		{
			startQuestTimer("WAVE_DEFEATED_CHECK", 5000, null, player, false);
		}
		else
		{
			STORED_MONSTER_SPAWNS.get(sepulcherId).clear(); // no need check for these waves
		}
	}
	
	private void spawnMysteriousChest(Player player)
	{
		final int sepulcherId = getSepulcherId(player);
		final int currentWave = STORED_PROGRESS.get(sepulcherId);
		for (int[] spawnInfo : CHEST_SPAWN_LOCATIONS)
		{
			if ((spawnInfo[0] == sepulcherId) && (spawnInfo[1] == currentWave))
			{
				addSpawn(MYSTERIOUS_CHEST, spawnInfo[2], spawnInfo[3], spawnInfo[4], spawnInfo[5], false, 0);
				break;
			}
		}
	}
	
	private void spawnKeyChest(Player player, Location loc)
	{
		addSpawn(KEY_CHEST, loc != null ? loc : player);
	}
	
	private int getSepulcherId(Player player)
	{
		if (ZoneManager.getInstance().getZoneById(CONQUEROR_ZONE).getPlayersInside().contains(player))
		{
			return 1;
		}
		
		if (ZoneManager.getInstance().getZoneById(EMPEROR_ZONE).getPlayersInside().contains(player))
		{
			return 2;
		}
		
		if (ZoneManager.getInstance().getZoneById(GREAT_SAGES_ZONE).getPlayersInside().contains(player))
		{
			return 3;
		}
		
		if (ZoneManager.getInstance().getZoneById(JUDGE_ZONE).getPlayersInside().contains(player))
		{
			return 4;
		}
		
		return 0;
	}
	
	private void showHtmlFile(Player player, String file, Npc npc, Player member)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/ai/areas/ImperialTomb/FourSepulchers/" + file);
		if (member != null)
		{
			html.replace("%member%", member.getName());
		}
		
		player.sendPacket(html);
	}
	
	@Override
	public void load()
	{
		ROOM_SPAWN_DATA.clear();
		parseDatapackFile("data/scripts/ai/areas/ImperialTomb/FourSepulchers/FourSepulchers.xml");
		LOGGER.info("[Four Sepulchers] Loaded " + ROOM_SPAWN_DATA.size() + " spawn zones data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("spawn".equalsIgnoreCase(b.getNodeName()))
					{
						final NamedNodeMap attrs = b.getAttributes();
						final int[] info =
						{
							parseInteger(attrs, "sepulcherId"),
							parseInteger(attrs, "wave"),
							parseInteger(attrs, "npcId"),
							parseInteger(attrs, "x"),
							parseInteger(attrs, "y"),
							parseInteger(attrs, "z"),
							parseInteger(attrs, "heading")
						};
						ROOM_SPAWN_DATA.add(info);
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new FourSepulchers();
	}
}
