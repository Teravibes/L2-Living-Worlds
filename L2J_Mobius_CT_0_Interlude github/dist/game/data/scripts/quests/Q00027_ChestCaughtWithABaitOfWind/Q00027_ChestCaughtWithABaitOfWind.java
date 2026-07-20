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
package quests.Q00027_ChestCaughtWithABaitOfWind;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00050_LanoscosSpecialBait.Q00050_LanoscosSpecialBait;

public class Q00027_ChestCaughtWithABaitOfWind extends Quest
{
	// NPCs
	private static final int LANOSCO = 31570;
	private static final int SHALING = 31434;
	
	// Items
	private static final int LARGE_BLUE_TREASURE_CHEST = 6500;
	private static final int STRANGE_BLUEPRINT = 7625;
	private static final int BLACK_PEARL_RING = 880;
	
	public Q00027_ChestCaughtWithABaitOfWind()
	{
		super(27, "Chest caught with a bait of wind");
		
		registerQuestItems(STRANGE_BLUEPRINT);
		
		addStartNpc(LANOSCO);
		addTalkId(LANOSCO, SHALING);
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
			case "31570-04.htm":
			{
				st.startQuest();
				break;
			}
			case "31570-07.htm":
			{
				if (hasQuestItems(player, LARGE_BLUE_TREASURE_CHEST))
				{
					st.setCond(2);
					takeItems(player, LARGE_BLUE_TREASURE_CHEST, 1);
					giveItems(player, STRANGE_BLUEPRINT, 1);
				}
				else
				{
					htmltext = "31570-08.htm";
				}
				break;
			}
			case "31434-02.htm":
			{
				if (hasQuestItems(player, STRANGE_BLUEPRINT))
				{
					htmltext = "31434-02.htm";
					takeItems(player, STRANGE_BLUEPRINT, 1);
					giveItems(player, BLACK_PEARL_RING, 1);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "31434-03.htm";
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
				if (player.getLevel() < 27)
				{
					htmltext = "31570-02.htm";
				}
				else
				{
					final QuestState st2 = player.getQuestState(Q00050_LanoscosSpecialBait.class.getSimpleName());
					if ((st2 != null) && st2.isCompleted())
					{
						htmltext = "31570-01.htm";
					}
					else
					{
						htmltext = "31570-03.htm";
					}
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case LANOSCO:
					{
						if (cond == 1)
						{
							htmltext = (!hasQuestItems(player, LARGE_BLUE_TREASURE_CHEST)) ? "31570-06.htm" : "31570-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31570-09.htm";
						}
						break;
					}
					case SHALING:
					{
						if (cond == 2)
						{
							htmltext = "31434-01.htm";
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
