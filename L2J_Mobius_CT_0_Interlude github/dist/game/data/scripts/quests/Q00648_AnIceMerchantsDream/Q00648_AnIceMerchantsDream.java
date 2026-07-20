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
package quests.Q00648_AnIceMerchantsDream;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00115_TheOtherSideOfTruth.Q00115_TheOtherSideOfTruth;

public class Q00648_AnIceMerchantsDream extends Quest
{
	// NPCs
	private static final int RAFFORTY = 32020;
	private static final int ICE_SHELF = 32023;
	
	// Items
	private static final int SILVER_HEMOCYTE = 8057;
	private static final int SILVER_ICE_CRYSTAL = 8077;
	private static final int BLACK_ICE_CRYSTAL = 8078;
	
	// Rewards
	private static final Map<String, int[]> REWARDS = new HashMap<>();
	static
	{
		// @formatter:off
		REWARDS.put("a", new int[]{SILVER_ICE_CRYSTAL, 23, 1894}); // Crafted Leather
		REWARDS.put("b", new int[]{SILVER_ICE_CRYSTAL, 6, 1881}); // Coarse Bone Powder
		REWARDS.put("c", new int[]{SILVER_ICE_CRYSTAL, 8, 1880}); // Steel
		REWARDS.put("d", new int[]{BLACK_ICE_CRYSTAL, 1800, 729}); // Scroll: Enchant Weapon (A-Grade)
		REWARDS.put("e", new int[]{BLACK_ICE_CRYSTAL, 240, 730}); // Scroll: Enchant Armor (A-Grade)
		REWARDS.put("f", new int[]{BLACK_ICE_CRYSTAL, 500, 947}); // Scroll: Enchant Weapon (B-Grade)
		REWARDS.put("g", new int[]{BLACK_ICE_CRYSTAL, 80, 948}); // Scroll: Enchant Armor (B-Grade)
	}
	
	// Drop chances
	private static final Map<Integer, int[]> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(22080, new int[]{285000, 48000}); // Massive Maze Bandersnatch
		CHANCES.put(22081, new int[]{443000, 0}); // Lost Watcher
		CHANCES.put(22082, new int[]{510000, 0}); // Baby Panthera
		CHANCES.put(22083, new int[]{510000, 0}); // Elder Lost Watcher
		CHANCES.put(22084, new int[]{477000, 49000}); // Panthera
		CHANCES.put(22085, new int[]{420000, 43000}); // Lost Gargoyle
		CHANCES.put(22086, new int[]{490000, 50000}); // Lost Gargoyle Youngling
		CHANCES.put(22087, new int[]{787000, 81000}); // Pronghorn Spirit
		CHANCES.put(22088, new int[]{480000, 49000}); // Pronghorn
		CHANCES.put(22089, new int[]{550000, 56000}); // Ice Tarantula
		CHANCES.put(22090, new int[]{570000, 58000}); // Frost Tarantula
		CHANCES.put(22092, new int[]{623000, 0}); // Frost Iron Golem
		CHANCES.put(22093, new int[]{910000, 93000}); // Lost Buffalo
		CHANCES.put(22094, new int[]{553000, 57000}); // Frost Buffalo
		CHANCES.put(22096, new int[]{593000, 61000}); // Ursus
		CHANCES.put(22097, new int[]{693000, 71000}); // Lost Yeti
		CHANCES.put(22098, new int[]{717000, 74000}); // Frost Yeti
		// @formatter:on
	}
	
	public Q00648_AnIceMerchantsDream()
	{
		super(648, "An Ice Merchant's Dream");
		addStartNpc(RAFFORTY, ICE_SHELF);
		addTalkId(RAFFORTY, ICE_SHELF);
		addKillId(CHANCES.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "32020-04.htm":
			{
				st.startQuest();
				break;
			}
			case "32020-05.htm":
			{
				st.setState(State.STARTED);
				st.setCond(2);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
				break;
			}
			default:
			{
				int exCond;
				int val;
				if (!event.equals("32020-14.htm") && !event.equals("32020-15.htm"))
				{
					if (event.startsWith("32020-17"))
					{
						
						int[] reward = REWARDS.get(event.substring(8, 9));
						if (getQuestItemsCount(player, reward[0]) >= reward[1])
						{
							takeItems(player, reward[0], reward[1]);
							rewardItems(player, reward[2], 1);
						}
						else
						{
							htmltext = "32020-15a.htm";
						}
					}
					else if (!event.equals("32020-20.htm") && !event.equals("32020-22.htm"))
					{
						if (event.equals("32023-05.htm"))
						{
							if (st.getInt("exCond") == 0)
							{
								st.set("exCond", String.valueOf((getRandom(4) + 1) * 10));
							}
						}
						else if (event.startsWith("32023-06-"))
						{
							exCond = st.getInt("exCond");
							if (exCond > 0)
							{
								htmltext = "32023-06.htm";
								st.set("exCond", String.valueOf(exCond + (event.endsWith("chisel") ? 1 : 2)));
								playSound(player, QuestSound.ITEMSOUND_BROKEN_KEY);
								takeItems(player, 8077, 1);
							}
						}
						else if (event.startsWith("32023-07-"))
						{
							exCond = st.getInt("exCond");
							if (exCond > 0)
							{
								val = exCond / 10;
								if (val == ((exCond - (val * 10)) + (event.endsWith("knife") ? 0 : 2)))
								{
									htmltext = "32023-07.htm";
									playSound(player, QuestSound.ITEMSOUND_ENCHANT_SUCCESS);
									rewardItems(player, 8078, 1);
								}
								else
								{
									htmltext = "32023-08.htm";
									playSound(player, QuestSound.ITEMSOUND_ENCHANT_FAILED);
								}
								
								st.set("exCond", "0");
							}
						}
					}
					else
					{
						st.exitQuest(true, true);
					}
				}
				else
				{
					exCond = getQuestItemsCount(player, 8078);
					val = getQuestItemsCount(player, 8077);
					if ((val + exCond) > 0)
					{
						takeItems(player, 8078, -1);
						takeItems(player, 8077, -1);
						giveAdena(player, (val * 300) + (exCond * 1200), true);
					}
					else
					{
						htmltext = "32020-16a.htm";
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == RAFFORTY)
				{
					if (player.getLevel() < 53)
					{
						htmltext = "32020-01.htm";
					}
					else
					{
						QuestState st2 = player.getQuestState(Q00115_TheOtherSideOfTruth.class.getSimpleName());
						htmltext = ((st2 != null) && st2.isCompleted()) ? "32020-02.htm" : "32020-03.htm";
					}
				}
				else
				{
					htmltext = "32023-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (npc.getId() == RAFFORTY)
				{
					final boolean hasItem = (hasAtLeastOneQuestItem(player, SILVER_ICE_CRYSTAL, BLACK_ICE_CRYSTAL));
					QuestState st2 = player.getQuestState(Q00115_TheOtherSideOfTruth.class.getSimpleName());
					if ((st2 != null) && st2.isCompleted())
					{
						htmltext = (hasItem) ? "32020-11.htm" : "32020-09.htm";
						if (st.isCond(1))
						{
							st.setCond(2, true);
						}
					}
					else
					{
						htmltext = (hasItem) ? "32020-10.htm" : "32020-08.htm";
					}
				}
				else
				{
					if (!hasQuestItems(player, SILVER_ICE_CRYSTAL))
					{
						htmltext = "32023-02.htm";
					}
					else
					{
						if ((st.getInt("exCond") % 10) == 0)
						{
							htmltext = "32023-03.htm";
							st.set("exCond", "0");
						}
						else
						{
							htmltext = "32023-04.htm";
						}
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getRandomPartyMemberState(player, -1, 3, npc);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		final Player partyMember = st.getPlayer();
		final int[] chance = CHANCES.get(npc.getId());
		if (getRandom(1000000) < chance[0])
		{
			giveItems(partyMember, SILVER_ICE_CRYSTAL, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		
		if (st.isCond(2) && (chance[1] > 0) && (getRandom(1000000) < chance[1]))
		{
			giveItems(partyMember, SILVER_HEMOCYTE, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
