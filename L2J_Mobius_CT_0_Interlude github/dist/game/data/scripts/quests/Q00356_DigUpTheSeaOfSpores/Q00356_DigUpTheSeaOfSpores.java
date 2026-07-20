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
package quests.Q00356_DigUpTheSeaOfSpores;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00356_DigUpTheSeaOfSpores extends Quest
{
	// Items
	private static final int HERB_SPORE = 5866;
	private static final int CARN_SPORE = 5865;
	
	// Monsters
	private static final int ROTTING_TREE = 20558;
	private static final int SPORE_ZOMBIE = 20562;
	
	public Q00356_DigUpTheSeaOfSpores()
	{
		super(356, "Dig Up the Sea of Spores!");
		registerQuestItems(HERB_SPORE, CARN_SPORE);
		addStartNpc(30717); // Gauen
		addTalkId(30717);
		addKillId(ROTTING_TREE, SPORE_ZOMBIE);
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
			case "30717-06.htm":
			{
				st.startQuest();
				break;
			}
			case "30717-16.htm":
			{
				takeItems(player, HERB_SPORE, -1);
				takeItems(player, CARN_SPORE, -1);
				giveAdena(player, 44000, true);
				st.exitQuest(true, true);
				break;
			}
			case "30717-14.htm":
			{
				takeItems(player, HERB_SPORE, -1);
				takeItems(player, CARN_SPORE, -1);
				addExpAndSp(player, 35000, 2600);
				st.exitQuest(true, true);
				break;
			}
			case "30717-12.htm":
			{
				takeItems(player, HERB_SPORE, -1);
				addExpAndSp(player, 24500, 0);
				break;
			}
			case "30717-13.htm":
			{
				takeItems(player, CARN_SPORE, -1);
				addExpAndSp(player, 0, 1820);
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
				htmltext = (player.getLevel() < 43) ? "30717-01.htm" : "30717-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "30717-07.htm";
				}
				else if (cond == 2)
				{
					if (getQuestItemsCount(player, HERB_SPORE) >= 50)
					{
						htmltext = "30717-08.htm";
					}
					else if (getQuestItemsCount(player, CARN_SPORE) >= 50)
					{
						htmltext = "30717-09.htm";
					}
					else
					{
						htmltext = "30717-07.htm";
					}
				}
				else if (cond == 3)
				{
					htmltext = "30717-10.htm";
				}
				break;
			}
			
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		final int cond = st.getCond();
		if (cond < 3)
		{
			switch (npc.getId())
			{
				case ROTTING_TREE:
				{
					if (getRandom(100) < 63)
					{
						giveItems(player, HERB_SPORE, 1);
						if (getQuestItemsCount(player, HERB_SPORE) < 50)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							st.setCond(cond == 2 ? 3 : 2, true);
						}
					}
					break;
				}
				case SPORE_ZOMBIE:
				{
					if (getRandom(100) < 76)
					{
						giveItems(player, CARN_SPORE, 1);
						if (getQuestItemsCount(player, CARN_SPORE) < 50)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							st.setCond(cond == 2 ? 3 : 2, true);
						}
					}
					break;
				}
			}
		}
	}
}
