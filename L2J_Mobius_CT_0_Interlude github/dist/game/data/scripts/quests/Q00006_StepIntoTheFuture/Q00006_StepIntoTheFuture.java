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
package quests.Q00006_StepIntoTheFuture;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00006_StepIntoTheFuture extends Quest
{
	// NPCs
	private static final int ROXXY = 30006;
	private static final int BAULRO = 30033;
	private static final int SIR_COLLIN = 30311;
	
	// Items
	private static final int BAULRO_LETTER = 7571;
	
	// Rewards
	private static final int MARK_TRAVELER = 7570;
	private static final int SOE_GIRAN = 7559;
	
	public Q00006_StepIntoTheFuture()
	{
		super(6, "Step into the Future");
		registerQuestItems(BAULRO_LETTER);
		addStartNpc(ROXXY);
		addTalkId(ROXXY, BAULRO, SIR_COLLIN);
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
			case "30006-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30033-02.htm":
			{
				st.setCond(2, true);
				giveItems(player, BAULRO_LETTER, 1);
				break;
			}
			case "30311-02.htm":
			{
				if (hasQuestItems(player, BAULRO_LETTER))
				{
					st.setCond(3, true);
					takeItems(player, BAULRO_LETTER, 1);
				}
				else
				{
					htmltext = "30311-03.htm";
				}
				break;
			}
			case "30006-06.htm":
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
				if ((player.getRace() != Race.HUMAN) || (player.getLevel() < 3))
				{
					htmltext = "30006-01.htm";
				}
				else
				{
					htmltext = "30006-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ROXXY:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "30006-04.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30006-05.htm";
						}
						break;
					}
					case BAULRO:
					{
						if (cond == 1)
						{
							htmltext = "30033-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30033-03.htm";
						}
						else
						{
							htmltext = "30033-04.htm";
						}
						break;
					}
					case SIR_COLLIN:
					{
						if (cond == 2)
						{
							htmltext = "30311-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30311-03a.htm";
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
