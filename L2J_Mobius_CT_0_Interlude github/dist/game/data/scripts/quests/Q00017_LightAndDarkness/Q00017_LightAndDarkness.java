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
package quests.Q00017_LightAndDarkness;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00017_LightAndDarkness extends Quest
{
	// NPCs
	private static final int HIERARCH = 31517;
	private static final int SAINT_ALTAR_1 = 31508;
	private static final int SAINT_ALTAR_2 = 31509;
	private static final int SAINT_ALTAR_3 = 31510;
	private static final int SAINT_ALTAR_4 = 31511;
	
	// Items
	private static final int BLOOD_OF_SAINT = 7168;
	
	public Q00017_LightAndDarkness()
	{
		super(17, "Light and Darkness");
		registerQuestItems(BLOOD_OF_SAINT);
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH, SAINT_ALTAR_1, SAINT_ALTAR_2, SAINT_ALTAR_3, SAINT_ALTAR_4);
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
			case "31517-04.htm":
			{
				st.startQuest();
				giveItems(player, BLOOD_OF_SAINT, 4);
				break;
			}
			case "31508-02.htm":
			{
				if (hasQuestItems(player, BLOOD_OF_SAINT))
				{
					st.setCond(2, true);
					takeItems(player, BLOOD_OF_SAINT, 1);
				}
				else
				{
					htmltext = "31508-03.htm";
				}
				break;
			}
			case "31509-02.htm":
			{
				if (hasQuestItems(player, BLOOD_OF_SAINT))
				{
					st.setCond(3, true);
					takeItems(player, BLOOD_OF_SAINT, 1);
				}
				else
				{
					htmltext = "31509-03.htm";
				}
				break;
			}
			case "31510-02.htm":
			{
				if (hasQuestItems(player, BLOOD_OF_SAINT))
				{
					st.setCond(4, true);
					takeItems(player, BLOOD_OF_SAINT, 1);
				}
				else
				{
					htmltext = "31510-03.htm";
				}
				break;
			}
			case "31511-02.htm":
			{
				if (hasQuestItems(player, BLOOD_OF_SAINT))
				{
					st.setCond(5, true);
					takeItems(player, BLOOD_OF_SAINT, 1);
				}
				else
				{
					htmltext = "31511-03.htm";
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
				htmltext = (player.getLevel() < 61) ? "31517-03.htm" : "31517-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case HIERARCH:
					{
						if (cond == 5)
						{
							htmltext = "31517-07.htm";
							addExpAndSp(player, 105527, 0);
							st.exitQuest(false, true);
						}
						else
						{
							if (hasQuestItems(player, BLOOD_OF_SAINT))
							{
								htmltext = "31517-05.htm";
							}
							else
							{
								htmltext = "31517-06.htm";
								st.exitQuest(true);
							}
						}
						break;
					}
					case SAINT_ALTAR_1:
					{
						if (cond == 1)
						{
							htmltext = "31508-01.htm";
						}
						else if (cond > 1)
						{
							htmltext = "31508-04.htm";
						}
						break;
					}
					case SAINT_ALTAR_2:
					{
						if (cond == 2)
						{
							htmltext = "31509-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "31509-04.htm";
						}
						break;
					}
					case SAINT_ALTAR_3:
					{
						if (cond == 3)
						{
							htmltext = "31510-01.htm";
						}
						else if (cond > 3)
						{
							htmltext = "31510-04.htm";
						}
						break;
					}
					case SAINT_ALTAR_4:
					{
						if (cond == 4)
						{
							htmltext = "31511-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "31511-04.htm";
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
