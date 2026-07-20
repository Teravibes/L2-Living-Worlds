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
package quests.Q00347_GoGetTheCalculator;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00347_GoGetTheCalculator extends Quest
{
	// NPCs
	private static final int BRUNON = 30526;
	private static final int SILVERA = 30527;
	private static final int SPIRON = 30532;
	private static final int BALANKI = 30533;
	
	// Items
	private static final int GEMSTONE_BEAST_CRYSTAL = 4286;
	private static final int CALCULATOR_QUEST = 4285;
	private static final int CALCULATOR_REAL = 4393;
	
	public Q00347_GoGetTheCalculator()
	{
		super(347, "Go Get the Calculator");
		registerQuestItems(GEMSTONE_BEAST_CRYSTAL, CALCULATOR_QUEST);
		addStartNpc(BRUNON);
		addTalkId(BRUNON, SILVERA, SPIRON, BALANKI);
		addKillId(20540);
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
			case "30526-05.htm":
			{
				st.startQuest();
				break;
			}
			case "30533-03.htm":
			{
				if (getQuestItemsCount(player, 57) >= 100)
				{
					htmltext = "30533-02.htm";
					takeItems(player, 57, 100);
					
					if (st.isCond(3))
					{
						st.setCond(4, true);
					}
					else
					{
						st.setCond(2, true);
					}
				}
				break;
			}
			case "30532-02.htm":
			{
				if (st.isCond(2))
				{
					st.setCond(4, true);
				}
				else
				{
					st.setCond(3, true);
				}
				break;
			}
			case "30526-08.htm":
			{
				takeItems(player, CALCULATOR_QUEST, -1);
				giveItems(player, CALCULATOR_REAL, 1);
				st.exitQuest(true, true);
				break;
			}
			case "30526-09.htm":
			{
				takeItems(player, CALCULATOR_QUEST, -1);
				giveAdena(player, 1000, true);
				st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < 12) ? "30526-00.htm" : "30526-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case BRUNON:
					{
						htmltext = (!hasQuestItems(player, CALCULATOR_QUEST)) ? "30526-06.htm" : "30526-07.htm";
						break;
					}
					case SPIRON:
					{
						htmltext = (cond < 4) ? "30532-01.htm" : "30532-05.htm";
						break;
					}
					case BALANKI:
					{
						htmltext = (cond < 4) ? "30533-01.htm" : "30533-04.htm";
						break;
					}
					case SILVERA:
					{
						if (cond < 4)
						{
							htmltext = "30527-00.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30527-01.htm";
							st.setCond(5, true);
						}
						else if (cond == 5)
						{
							if (getQuestItemsCount(player, GEMSTONE_BEAST_CRYSTAL) < 10)
							{
								htmltext = "30527-02.htm";
							}
							else
							{
								htmltext = "30527-03.htm";
								st.setCond(6, true);
								takeItems(player, GEMSTONE_BEAST_CRYSTAL, -1);
								giveItems(player, CALCULATOR_QUEST, 1);
							}
						}
						else if (cond == 6)
						{
							htmltext = "30527-04.htm";
						}
						break;
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
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isCond(5))
		{
			return;
		}
		
		if (getRandomBoolean() && (getQuestItemsCount(player, GEMSTONE_BEAST_CRYSTAL) < 10))
		{
			giveItems(player, GEMSTONE_BEAST_CRYSTAL, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
