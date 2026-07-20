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
package quests.Q00002_WhatWomenWant;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00002_WhatWomenWant extends Quest
{
	// NPCs
	private static final int ARUJIEN = 30223;
	private static final int MIRABEL = 30146;
	private static final int HERBIEL = 30150;
	private static final int GREENIS = 30157;
	
	// Items
	private static final int ARUJIEN_LETTER_1 = 1092;
	private static final int ARUJIEN_LETTER_2 = 1093;
	private static final int ARUJIEN_LETTER_3 = 1094;
	private static final int POETRY_BOOK = 689;
	private static final int GREENIS_LETTER = 693;
	
	// Rewards
	private static final int MYSTICS_EARRING = 113;
	
	public Q00002_WhatWomenWant()
	{
		super(2, "What Women Want");
		registerQuestItems(ARUJIEN_LETTER_1, ARUJIEN_LETTER_2, ARUJIEN_LETTER_3, POETRY_BOOK, GREENIS_LETTER);
		addStartNpc(ARUJIEN);
		addTalkId(ARUJIEN, MIRABEL, HERBIEL, GREENIS);
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
			case "30223-04.htm":
			{
				st.startQuest();
				giveItems(player, ARUJIEN_LETTER_1, 1);
				break;
			}
			case "30223-08.htm":
			{
				st.setCond(4, true);
				takeItems(player, ARUJIEN_LETTER_3, 1);
				giveItems(player, POETRY_BOOK, 1);
				break;
			}
			case "30223-09.htm":
			{
				takeItems(player, ARUJIEN_LETTER_3, 1);
				giveAdena(player, 450, true);
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
				if ((player.getRace() != Race.ELF) && (player.getRace() != Race.HUMAN))
				{
					htmltext = "30223-00.htm";
				}
				else if (player.getLevel() < 2)
				{
					htmltext = "30223-01.htm";
				}
				else
				{
					htmltext = "30223-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ARUJIEN:
					{
						if (hasQuestItems(player, ARUJIEN_LETTER_1))
						{
							htmltext = "30223-05.htm";
						}
						else if (hasQuestItems(player, ARUJIEN_LETTER_3))
						{
							htmltext = "30223-07.htm";
						}
						else if (hasQuestItems(player, ARUJIEN_LETTER_2))
						{
							htmltext = "30223-06.htm";
						}
						else if (hasQuestItems(player, POETRY_BOOK))
						{
							htmltext = "30223-11.htm";
						}
						else if (hasQuestItems(player, GREENIS_LETTER))
						{
							htmltext = "30223-10.htm";
							takeItems(player, GREENIS_LETTER, 1);
							giveItems(player, MYSTICS_EARRING, 1);
							st.exitQuest(false, true);
						}
						break;
					}
					case MIRABEL:
					{
						if (cond == 1)
						{
							htmltext = "30146-01.htm";
							st.setCond(2, true);
							takeItems(player, ARUJIEN_LETTER_1, 1);
							giveItems(player, ARUJIEN_LETTER_2, 1);
						}
						else if (cond > 1)
						{
							htmltext = "30146-02.htm";
						}
						break;
					}
					case HERBIEL:
					{
						if (cond == 2)
						{
							htmltext = "30150-01.htm";
							st.setCond(3, true);
							takeItems(player, ARUJIEN_LETTER_2, 1);
							giveItems(player, ARUJIEN_LETTER_3, 1);
						}
						else if (cond > 2)
						{
							htmltext = "30150-02.htm";
						}
						break;
					}
					case GREENIS:
					{
						if (cond < 4)
						{
							htmltext = "30157-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30157-02.htm";
							st.setCond(5, true);
							takeItems(player, POETRY_BOOK, 1);
							giveItems(player, GREENIS_LETTER, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30157-03.htm";
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
