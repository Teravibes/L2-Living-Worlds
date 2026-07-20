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
package quests.Q00030_ChestCaughtWithABaitOfFire;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00053_LinnaeusSpecialBait.Q00053_LinnaeusSpecialBait;

public class Q00030_ChestCaughtWithABaitOfFire extends Quest
{
	// NPCs
	private static final int LINNAEUS = 31577;
	private static final int RUKAL = 30629;
	
	// Items
	private static final int RED_TREASURE_BOX = 6511;
	private static final int MUSICAL_SCORE = 7628;
	private static final int NECKLACE_OF_PROTECTION = 916;
	
	public Q00030_ChestCaughtWithABaitOfFire()
	{
		super(30, "Chest caught with a bait of fire");
		registerQuestItems(MUSICAL_SCORE);
		addStartNpc(LINNAEUS);
		addTalkId(LINNAEUS, RUKAL);
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
			case "31577-04.htm":
			{
				st.startQuest();
				break;
			}
			case "31577-07.htm":
			{
				if (hasQuestItems(player, RED_TREASURE_BOX))
				{
					st.setCond(2);
					takeItems(player, RED_TREASURE_BOX, 1);
					giveItems(player, MUSICAL_SCORE, 1);
				}
				else
				{
					htmltext = "31577-08.htm";
				}
				break;
			}
			case "30629-02.htm":
			{
				if (hasQuestItems(player, MUSICAL_SCORE))
				{
					htmltext = "30629-02.htm";
					takeItems(player, MUSICAL_SCORE, 1);
					giveItems(player, NECKLACE_OF_PROTECTION, 1);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "30629-03.htm";
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
				if (player.getLevel() < 60)
				{
					htmltext = "31577-02.htm";
				}
				else
				{
					final QuestState st2 = player.getQuestState(Q00053_LinnaeusSpecialBait.class.getSimpleName());
					if ((st2 != null) && st2.isCompleted())
					{
						htmltext = "31577-01.htm";
					}
					else
					{
						htmltext = "31577-03.htm";
					}
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case LINNAEUS:
					{
						if (cond == 1)
						{
							htmltext = (!hasQuestItems(player, RED_TREASURE_BOX)) ? "31577-06.htm" : "31577-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31577-09.htm";
						}
						break;
					}
					case RUKAL:
					{
						if (cond == 2)
						{
							htmltext = "30629-01.htm";
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
