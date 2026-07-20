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
package quests.Q00109_InSearchOfTheNest;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00109_InSearchOfTheNest extends Quest
{
	// NPCs
	private static final int PIERCE = 31553;
	private static final int KAHMAN = 31554;
	private static final int SCOUT_CORPSE = 32015;
	
	// Items
	private static final int SCOUT_MEMO = 8083;
	private static final int RECRUIT_BADGE = 7246;
	private static final int SOLDIER_BADGE = 7247;
	
	public Q00109_InSearchOfTheNest()
	{
		super(109, "In Search of the Nest");
		registerQuestItems(SCOUT_MEMO);
		addStartNpc(PIERCE);
		addTalkId(PIERCE, SCOUT_CORPSE, KAHMAN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31553-01.htm":
			{
				st.startQuest();
				break;
			}
			case "32015-02.htm":
			{
				st.setCond(2, true);
				giveItems(player, SCOUT_MEMO, 1);
				break;
			}
			case "31553-03.htm":
			{
				st.setCond(3, true);
				takeItems(player, SCOUT_MEMO, 1);
				break;
			}
			case "31554-02.htm":
			{
				giveAdena(player, 5168, true);
				st.exitQuest(false, true);
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
				// Must worn one or other Golden Ram Badge in order to be accepted.
				if ((player.getLevel() >= 66) && hasAtLeastOneQuestItem(player, RECRUIT_BADGE, SOLDIER_BADGE))
				{
					htmltext = "31553-00.htm";
				}
				else
				{
					htmltext = "31553-00a.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case PIERCE:
					{
						if (cond == 1)
						{
							htmltext = "31553-01a.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31553-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "31553-03.htm";
						}
						break;
					}
					case SCOUT_CORPSE:
					{
						if (cond == 1)
						{
							htmltext = "32015-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "32015-02.htm";
						}
						break;
					}
					case KAHMAN:
					{
						if (cond == 3)
						{
							htmltext = "31554-01.htm";
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
