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
package quests.Q00625_TheFinestIngredientsPart2;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * The Finest Ingredients Part - 2 (625)
 * @author Janiko
 */
public class Q00625_TheFinestIngredientsPart2 extends Quest
{
	// NPCs
	private static final int JEREMY = 31521;
	private static final int YETIS_TABLE = 31542;
	
	// Monster
	private static final int ICICLE_EMPEROR_BUMBALUMP = 25296;
	
	// Required Item
	private static final ItemHolder SOY_SOURCE_JAR = new ItemHolder(7205, 1);
	
	// Quest Items
	private static final ItemHolder FOOD_FOR_BUMBALUMP = new ItemHolder(7209, 1);
	private static final ItemHolder SPECIAL_YETI_MEAT = new ItemHolder(7210, 1);
	
	// Rewards
	private static final ItemHolder GREATER_DYE_OF_STR_1 = new ItemHolder(4589, 5);
	private static final ItemHolder GREATER_DYE_OF_STR_2 = new ItemHolder(4590, 5);
	private static final ItemHolder GREATER_DYE_OF_CON_1 = new ItemHolder(4591, 5);
	private static final ItemHolder GREATER_DYE_OF_CON_2 = new ItemHolder(4592, 5);
	private static final ItemHolder GREATER_DYE_OF_DEX_1 = new ItemHolder(4593, 5);
	private static final ItemHolder GREATER_DYE_OF_DEX_2 = new ItemHolder(4594, 5);
	
	// Location
	private static final Location ICICLE_EMPEROR_BUMBALUMP_LOC = new Location(158240, -121536, -2222);
	
	// Misc
	private static final String ICICLE_EMPEROR_BUMBALUMP_RESPAWN_TIME = "ICICLE_EMPEROR_BUMBALUMP_RESPAWN_TIME";
	private static final int MIN_LEVEL = 73;
	
	public Q00625_TheFinestIngredientsPart2()
	{
		super(625, "The Finest Ingredients - Part 2");
		addStartNpc(JEREMY);
		addTalkId(JEREMY, YETIS_TABLE);
		addSpawnId(ICICLE_EMPEROR_BUMBALUMP);
		addKillId(ICICLE_EMPEROR_BUMBALUMP);
		registerQuestItems(FOOD_FOR_BUMBALUMP.getId(), SPECIAL_YETI_MEAT.getId());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("NPC_TALK"))
		{
			if (isBumbalumpSpawned())
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getTemplate().getDisplayId(), "Oooh!"));
			}
			
			return null;
		}
		
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31521-04.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					takeItem(player, SOY_SOURCE_JAR);
					giveItems(player, FOOD_FOR_BUMBALUMP);
					htmltext = event;
				}
				break;
			}
			case "31521-08.html":
			{
				if (qs.isCond(3))
				{
					if (hasItem(player, SPECIAL_YETI_MEAT))
					{
						final int random = getRandom(1000);
						if (random < 167)
						{
							rewardItems(player, GREATER_DYE_OF_STR_1);
						}
						else if (random < 334)
						{
							rewardItems(player, GREATER_DYE_OF_STR_2);
						}
						else if (random < 501)
						{
							rewardItems(player, GREATER_DYE_OF_CON_1);
						}
						else if (random < 668)
						{
							rewardItems(player, GREATER_DYE_OF_CON_2);
						}
						else if (random < 835)
						{
							rewardItems(player, GREATER_DYE_OF_DEX_1);
						}
						else if (random < 1000)
						{
							rewardItems(player, GREATER_DYE_OF_DEX_2);
						}
						
						qs.exitQuest(true, true);
						htmltext = event;
					}
					else
					{
						htmltext = "31521-09.html";
					}
				}
				break;
			}
			case "31542-02.html":
			{
				if (qs.isCond(1))
				{
					if (hasItem(player, FOOD_FOR_BUMBALUMP))
					{
						qs.setCond(2, true);
						if (!isBumbalumpSpawned())
						{
							takeItem(player, FOOD_FOR_BUMBALUMP);
							addSpawn(ICICLE_EMPEROR_BUMBALUMP, ICICLE_EMPEROR_BUMBALUMP_LOC);
							htmltext = event;
						}
						else
						{
							htmltext = "31542-03.html";
						}
					}
					else
					{
						htmltext = "31542-04.html";
					}
				}
				else if (qs.isCond(2))
				{
					if (hasItem(player, FOOD_FOR_BUMBALUMP))
					{
						if (!isBumbalumpSpawned())
						{
							takeItem(player, FOOD_FOR_BUMBALUMP);
							addSpawn(ICICLE_EMPEROR_BUMBALUMP, ICICLE_EMPEROR_BUMBALUMP_LOC);
							htmltext = event;
						}
						else
						{
							htmltext = "31542-03.html";
						}
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		switch (npc.getId())
		{
			case JEREMY:
			{
				if (qs.isCreated())
				{
					if (talker.getLevel() >= MIN_LEVEL)
					{
						htmltext = hasItem(talker, SOY_SOURCE_JAR) ? "31521-01.htm" : "31521-02.htm";
					}
					else
					{
						htmltext = "31521-03.htm";
					}
				}
				else if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "31521-05.html";
							break;
						}
						case 2:
						{
							htmltext = "31521-06.html";
							break;
						}
						case 3:
						{
							htmltext = "31521-07.html";
							break;
						}
					}
				}
				else if (qs.isCompleted())
				{
					htmltext = getAlreadyCompletedMsg(talker);
				}
				break;
			}
			case YETIS_TABLE:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						if (!isBumbalumpSpawned())
						{
							htmltext = "31542-01.html";
						}
						else
						{
							htmltext = "31542-05.html";
						}
						break;
					}
					case 2:
					{
						if (!isBumbalumpSpawned())
						{
							if (hasItem(talker, FOOD_FOR_BUMBALUMP))
							{
								takeItem(talker, FOOD_FOR_BUMBALUMP);
								addSpawn(ICICLE_EMPEROR_BUMBALUMP, ICICLE_EMPEROR_BUMBALUMP_LOC);
								htmltext = "31542-02.html";
							}
							else
							{
								htmltext = "31542-04.html";
							}
						}
						else
						{
							htmltext = "31542-03.html";
						}
						break;
					}
					case 3:
					{
						htmltext = "31542-05.html";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("NPC_TALK", 1000 * 1200, npc, null);
		npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getTemplate().getDisplayId(), "I smell something delicious..."));
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
		
		final int respawnMinDelay = (int) (43200000 * NpcConfig.RAID_MIN_RESPAWN_MULTIPLIER);
		final int respawnMaxDelay = (int) (129600000 * NpcConfig.RAID_MAX_RESPAWN_MULTIPLIER);
		final int respawnDelay = getRandom(respawnMinDelay, respawnMaxDelay);
		GlobalVariablesManager.getInstance().set(ICICLE_EMPEROR_BUMBALUMP_RESPAWN_TIME, System.currentTimeMillis() + respawnDelay);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(2) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, player, true))
		{
			qs.setCond(3, true);
			giveItems(player, SPECIAL_YETI_MEAT);
		}
	}
	
	private static boolean isBumbalumpSpawned()
	{
		if (System.currentTimeMillis() > GlobalVariablesManager.getInstance().getLong(ICICLE_EMPEROR_BUMBALUMP_RESPAWN_TIME, 0))
		{
			return World.getInstance().getVisibleObjects().stream().anyMatch(object -> object.getId() == ICICLE_EMPEROR_BUMBALUMP);
		}
		
		return true;
	}
}
