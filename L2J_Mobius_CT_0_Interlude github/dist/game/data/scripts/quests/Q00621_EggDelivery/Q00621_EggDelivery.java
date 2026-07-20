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
package quests.Q00621_EggDelivery;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00621_EggDelivery extends Quest
{
	// NPCs
	private static final int JEREMY = 31521;
	private static final int PULIN = 31543;
	private static final int NAFF = 31544;
	private static final int CROCUS = 31545;
	private static final int KUBER = 31546;
	private static final int BEOLIN = 31547;
	private static final int VALENTINE = 31584;
	
	// Items
	private static final int BOILED_EGGS = 7195;
	private static final int FEE_OF_BOILED_EGG = 7196;
	
	// Rewards
	private static final int HASTE_POTION = 1062;
	private static final int[] RECIPES =
	{
		6847,
		6849,
		6851
	};
	
	public Q00621_EggDelivery()
	{
		super(621, "Egg Delivery");
		registerQuestItems(BOILED_EGGS, FEE_OF_BOILED_EGG);
		addStartNpc(JEREMY);
		addTalkId(JEREMY, PULIN, NAFF, CROCUS, KUBER, BEOLIN, VALENTINE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31521-02.htm":
			{
				st.startQuest();
				giveItems(player, BOILED_EGGS, 5);
				break;
			}
			case "31543-02.htm":
			{
				st.setCond(2);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, BOILED_EGGS, 1);
				giveItems(player, FEE_OF_BOILED_EGG, 1);
				break;
			}
			case "31544-02.htm":
			{
				st.setCond(3);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, BOILED_EGGS, 1);
				giveItems(player, FEE_OF_BOILED_EGG, 1);
				break;
			}
			case "31545-02.htm":
			{
				st.setCond(4);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, BOILED_EGGS, 1);
				giveItems(player, FEE_OF_BOILED_EGG, 1);
				break;
			}
			case "31546-02.htm":
			{
				st.setCond(5);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, BOILED_EGGS, 1);
				giveItems(player, FEE_OF_BOILED_EGG, 1);
				break;
			}
			case "31547-02.htm":
			{
				st.setCond(6);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, BOILED_EGGS, 1);
				giveItems(player, FEE_OF_BOILED_EGG, 1);
				break;
			}
			case "31521-06.htm":
			{
				if (getQuestItemsCount(player, FEE_OF_BOILED_EGG) < 5)
				{
					htmltext = "31521-08.htm";
					playSound(player, QuestSound.ITEMSOUND_QUEST_GIVEUP);
					st.exitQuest(true);
				}
				else
				{
					st.setCond(7, true);
					takeItems(player, FEE_OF_BOILED_EGG, 5);
				}
				break;
			}
			case "31584-02.htm":
			{
				if (getRandom(5) < 1)
				{
					rewardItems(player, RECIPES[getRandom(3)], 1);
					st.exitQuest(true, true);
				}
				else
				{
					giveAdena(player, 18800, true);
					rewardItems(player, HASTE_POTION, 1);
					st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < 68) ? "31521-03.htm" : "31521-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case JEREMY:
					{
						if (cond == 1)
						{
							htmltext = "31521-04.htm";
						}
						else if (cond == 6)
						{
							htmltext = "31521-05.htm";
						}
						else if (cond == 7)
						{
							htmltext = "31521-07.htm";
						}
						break;
					}
					case PULIN:
					{
						if ((cond == 1) && (getQuestItemsCount(player, BOILED_EGGS) == 5))
						{
							htmltext = "31543-01.htm";
						}
						else if (cond > 1)
						{
							htmltext = "31543-03.htm";
						}
						break;
					}
					case NAFF:
					{
						if ((cond == 2) && (getQuestItemsCount(player, BOILED_EGGS) == 4))
						{
							htmltext = "31544-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "31544-03.htm";
						}
						break;
					}
					case CROCUS:
					{
						if ((cond == 3) && (getQuestItemsCount(player, BOILED_EGGS) == 3))
						{
							htmltext = "31545-01.htm";
						}
						else if (cond > 3)
						{
							htmltext = "31545-03.htm";
						}
						break;
					}
					case KUBER:
					{
						if ((cond == 4) && (getQuestItemsCount(player, BOILED_EGGS) == 2))
						{
							htmltext = "31546-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "31546-03.htm";
						}
						break;
					}
					case BEOLIN:
					{
						if ((cond == 5) && (getQuestItemsCount(player, BOILED_EGGS) == 1))
						{
							htmltext = "31547-01.htm";
						}
						else if (cond > 5)
						{
							htmltext = "31547-03.htm";
						}
						break;
					}
					case VALENTINE:
					{
						if (cond == 7)
						{
							htmltext = "31584-01.htm";
						}
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
