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
package quests.Q00151_CureForFever;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00151_CureForFever extends Quest
{
	// NPCs
	private static final int ELIAS = 30050;
	private static final int YOHANES = 30032;
	
	// Items
	private static final int POISON_SAC = 703;
	private static final int FEVER_MEDICINE = 704;
	
	public Q00151_CureForFever()
	{
		super(151, "Cure for Fever Disease");
		registerQuestItems(FEVER_MEDICINE, POISON_SAC);
		addStartNpc(ELIAS);
		addTalkId(ELIAS, YOHANES);
		addKillId(20103, 20106, 20108);
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
		
		if (event.equals("30050-03.htm"))
		{
			st.startQuest();
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
				htmltext = (player.getLevel() < 15) ? "30050-01.htm" : "30050-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ELIAS:
					{
						if (cond == 1)
						{
							htmltext = "30050-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30050-05.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30050-06.htm";
							takeItems(player, FEVER_MEDICINE, 1);
							giveItems(player, 102, 1);
							st.exitQuest(false, true);
						}
						break;
					}
					case YOHANES:
					{
						if (cond == 2)
						{
							htmltext = "30032-01.htm";
							st.setCond(3, true);
							takeItems(player, POISON_SAC, 1);
							giveItems(player, FEVER_MEDICINE, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30032-02.htm";
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
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1) && (getRandom(10) < 2))
		{
			giveItems(killer, POISON_SAC, 1);
			qs.setCond(2, true);
		}
	}
}
