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
package quests.Q00007_ATripBegins;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00007_ATripBegins extends Quest
{
	// NPCs
	private static final int MIRABEL = 30146;
	private static final int ARIEL = 30148;
	private static final int ASTERIOS = 30154;
	
	// Items
	private static final int ARIEL_RECO = 7572;
	
	// Rewards
	private static final int MARK_TRAVELER = 7570;
	private static final int SOE_GIRAN = 7559;
	
	public Q00007_ATripBegins()
	{
		super(7, "A Trip Begins");
		registerQuestItems(ARIEL_RECO);
		addStartNpc(MIRABEL);
		addTalkId(MIRABEL, ARIEL, ASTERIOS);
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
			case "30146-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30148-02.htm":
			{
				st.setCond(2, true);
				giveItems(player, ARIEL_RECO, 1);
				break;
			}
			case "30154-02.htm":
			{
				st.setCond(3, true);
				takeItems(player, ARIEL_RECO, 1);
				break;
			}
			case "30146-06.htm":
			{
				giveItems(player, MARK_TRAVELER, 1);
				rewardItems(player, SOE_GIRAN, 1);
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
				if (player.getRace() != Race.ELF)
				{
					htmltext = "30146-01.htm";
				}
				else if (player.getLevel() < 3)
				{
					htmltext = "30146-01a.htm";
				}
				else
				{
					htmltext = "30146-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MIRABEL:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "30146-04.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30146-05.htm";
						}
						break;
					}
					case ARIEL:
					{
						if (cond == 1)
						{
							htmltext = "30148-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30148-03.htm";
						}
						break;
					}
					case ASTERIOS:
					{
						if (cond == 2)
						{
							htmltext = "30154-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30154-03.htm";
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
