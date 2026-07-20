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
package quests.Q00168_DeliverSupplies;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00168_DeliverSupplies extends Quest
{
	// NPCs
	private static final int JENNA = 30349;
	private static final int ROSELYN = 30355;
	private static final int KRISTIN = 30357;
	private static final int HARANT = 30360;
	
	// Items
	private static final int JENNA_LETTER = 1153;
	private static final int SENTRY_BLADE_1 = 1154;
	private static final int SENTRY_BLADE_2 = 1155;
	private static final int SENTRY_BLADE_3 = 1156;
	private static final int OLD_BRONZE_SWORD = 1157;
	
	public Q00168_DeliverSupplies()
	{
		super(168, "Deliver Supplies");
		registerQuestItems(JENNA_LETTER, SENTRY_BLADE_1, SENTRY_BLADE_2, SENTRY_BLADE_3, OLD_BRONZE_SWORD);
		addStartNpc(JENNA);
		addTalkId(JENNA, ROSELYN, KRISTIN, HARANT);
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
		
		if (event.equals("30349-03.htm"))
		{
			st.startQuest();
			giveItems(player, JENNA_LETTER, 1);
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
				if (player.getRace() != Race.DARK_ELF)
				{
					htmltext = "30349-00.htm";
				}
				else if (player.getLevel() < 3)
				{
					htmltext = "30349-01.htm";
				}
				else
				{
					htmltext = "30349-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case JENNA:
					{
						if (cond == 1)
						{
							htmltext = "30349-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30349-05.htm";
							st.setCond(3, true);
							takeItems(player, SENTRY_BLADE_1, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30349-07.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30349-06.htm";
							takeItems(player, OLD_BRONZE_SWORD, 2);
							giveAdena(player, 820, true);
							st.exitQuest(false, true);
						}
						break;
					}
					case HARANT:
					{
						if (cond == 1)
						{
							htmltext = "30360-01.htm";
							st.setCond(2, true);
							takeItems(player, JENNA_LETTER, 1);
							giveItems(player, SENTRY_BLADE_1, 1);
							giveItems(player, SENTRY_BLADE_2, 1);
							giveItems(player, SENTRY_BLADE_3, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30360-02.htm";
						}
						break;
					}
					case ROSELYN:
					{
						if (cond == 3)
						{
							if (hasQuestItems(player, SENTRY_BLADE_2))
							{
								htmltext = "30355-01.htm";
								takeItems(player, SENTRY_BLADE_2, 1);
								giveItems(player, OLD_BRONZE_SWORD, 1);
								if (getQuestItemsCount(player, OLD_BRONZE_SWORD) == 2)
								{
									st.setCond(4, true);
								}
							}
							else
							{
								htmltext = "30355-02.htm";
							}
						}
						else if (cond == 4)
						{
							htmltext = "30355-02.htm";
						}
						break;
					}
					case KRISTIN:
					{
						if (cond == 3)
						{
							if (hasQuestItems(player, SENTRY_BLADE_3))
							{
								htmltext = "30357-01.htm";
								takeItems(player, SENTRY_BLADE_3, 1);
								giveItems(player, OLD_BRONZE_SWORD, 1);
								if (getQuestItemsCount(player, OLD_BRONZE_SWORD) == 2)
								{
									st.setCond(4, true);
								}
							}
							else
							{
								htmltext = "30357-02.htm";
							}
						}
						else if (cond == 4)
						{
							htmltext = "30357-02.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		
		return htmltext;
	}
}
