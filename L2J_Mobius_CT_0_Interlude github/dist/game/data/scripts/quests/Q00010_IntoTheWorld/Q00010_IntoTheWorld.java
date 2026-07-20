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
package quests.Q00010_IntoTheWorld;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00010_IntoTheWorld extends Quest
{
	// Items
	private static final int VERY_EXPENSIVE_NECKLACE = 7574;
	
	// Rewards
	private static final int SOE_GIRAN = 7559;
	private static final int MARK_OF_TRAVELER = 7570;
	
	// NPCs
	private static final int REED = 30520;
	private static final int BALANKI = 30533;
	private static final int GERALD = 30650;
	
	public Q00010_IntoTheWorld()
	{
		super(10, "Into the World");
		registerQuestItems(VERY_EXPENSIVE_NECKLACE);
		addStartNpc(BALANKI);
		addTalkId(BALANKI, REED, GERALD);
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
			case "30533-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30520-02.htm":
			{
				st.setCond(2, true);
				giveItems(player, VERY_EXPENSIVE_NECKLACE, 1);
				break;
			}
			case "30650-02.htm":
			{
				st.setCond(3, true);
				takeItems(player, VERY_EXPENSIVE_NECKLACE, 1);
				break;
			}
			case "30520-04.htm":
			{
				st.setCond(4, true);
				break;
			}
			case "30533-05.htm":
			{
				giveItems(player, SOE_GIRAN, 1);
				rewardItems(player, MARK_OF_TRAVELER, 1);
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
				if ((player.getLevel() >= 3) && (player.getRace() == Race.DWARF))
				{
					htmltext = "30533-01.htm";
				}
				else
				{
					htmltext = "30533-01a.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case BALANKI:
					{
						if (cond < 4)
						{
							htmltext = "30533-03.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30533-04.htm";
						}
						break;
					}
					case REED:
					{
						if (cond == 1)
						{
							htmltext = "30520-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30520-02a.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30520-03.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30520-04a.htm";
						}
						break;
					}
					case GERALD:
					{
						if (cond == 2)
						{
							htmltext = "30650-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30650-04.htm";
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
