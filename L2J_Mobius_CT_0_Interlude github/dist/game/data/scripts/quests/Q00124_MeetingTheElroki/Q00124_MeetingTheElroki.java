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
package quests.Q00124_MeetingTheElroki;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00124_MeetingTheElroki extends Quest
{
	// NPCs
	private static final int MARQUEZ = 32113;
	private static final int MUSHIKA = 32114;
	private static final int ASAMAH = 32115;
	private static final int KARAKAWEI = 32117;
	private static final int MANTARASA = 32118;
	
	public Q00124_MeetingTheElroki()
	{
		super(124, "Meeting the Elroki");
		addStartNpc(MARQUEZ);
		addTalkId(MARQUEZ, MUSHIKA, ASAMAH, KARAKAWEI, MANTARASA);
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
			case "32113-03.htm":
			{
				st.startQuest();
				break;
			}
			case "32113-04.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "32114-02.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "32115-04.htm":
			{
				st.setCond(4, true);
				break;
			}
			case "32117-02.htm":
			{
				if (st.isCond(4))
				{
					st.set("progress", "1");
				}
				break;
			}
			case "32117-03.htm":
			{
				st.setCond(5, true);
				break;
			}
			case "32118-02.htm":
			{
				st.setCond(6, true);
				giveItems(player, 8778, 1); // Egg
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
				htmltext = (player.getLevel() < 75) ? "32113-01a.htm" : "32113-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MARQUEZ:
					{
						if (cond == 1)
						{
							htmltext = "32113-03.htm";
						}
						else if (cond > 1)
						{
							htmltext = "32113-04a.htm";
						}
						break;
					}
					case MUSHIKA:
					{
						if (cond == 2)
						{
							htmltext = "32114-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "32114-03.htm";
						}
						break;
					}
					case ASAMAH:
					{
						if (cond == 3)
						{
							htmltext = "32115-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = "32115-05.htm";
							takeItems(player, 8778, -1);
							giveAdena(player, 71318, true);
							st.exitQuest(false, true);
						}
						break;
					}
					case KARAKAWEI:
					{
						if (cond == 4)
						{
							htmltext = "32117-01.htm";
							if (st.getInt("progress") == 1)
							{
								htmltext = "32117-02.htm";
							}
						}
						else if (cond > 4)
						{
							htmltext = "32117-04.htm";
						}
						break;
					}
					case MANTARASA:
					{
						if (cond == 5)
						{
							htmltext = "32118-01.htm";
						}
						else if (cond > 5)
						{
							htmltext = "32118-03.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (npc.getId() == ASAMAH)
				{
					htmltext = "32115-06.htm";
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
		}
		
		return htmltext;
	}
}
