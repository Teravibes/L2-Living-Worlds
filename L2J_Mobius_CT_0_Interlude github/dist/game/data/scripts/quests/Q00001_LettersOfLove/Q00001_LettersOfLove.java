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
package quests.Q00001_LettersOfLove;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00001_LettersOfLove extends Quest
{
	// NPCs
	private static final int DARIN = 30048;
	private static final int ROXXY = 30006;
	private static final int BAULRO = 30033;
	
	// Items
	private static final int DARIN_LETTER = 687;
	private static final int ROXXY_KERCHIEF = 688;
	private static final int DARIN_RECEIPT = 1079;
	private static final int BAULRO_POTION = 1080;
	
	// Reward
	private static final int NECKLACE = 906;
	
	public Q00001_LettersOfLove()
	{
		super(1, "Letters of Love");
		registerQuestItems(DARIN_LETTER, ROXXY_KERCHIEF, DARIN_RECEIPT, BAULRO_POTION);
		addStartNpc(DARIN);
		addTalkId(DARIN, ROXXY, BAULRO);
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
		
		if (event.equals("30048-06.htm"))
		{
			st.startQuest();
			giveItems(player, DARIN_LETTER, 1);
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
				htmltext = (player.getLevel() < 2) ? "30048-01.htm" : "30048-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case DARIN:
					{
						if (cond == 1)
						{
							htmltext = "30048-07.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30048-08.htm";
							st.setCond(3, true);
							takeItems(player, ROXXY_KERCHIEF, 1);
							giveItems(player, DARIN_RECEIPT, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30048-09.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30048-10.htm";
							takeItems(player, BAULRO_POTION, 1);
							giveItems(player, NECKLACE, 1);
							st.exitQuest(false, true);
						}
						break;
					}
					case ROXXY:
					{
						if (cond == 1)
						{
							htmltext = "30006-01.htm";
							st.setCond(2, true);
							takeItems(player, DARIN_LETTER, 1);
							giveItems(player, ROXXY_KERCHIEF, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30006-02.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30006-03.htm";
						}
						break;
					}
					case BAULRO:
					{
						if (cond == 3)
						{
							htmltext = "30033-01.htm";
							st.setCond(4, true);
							takeItems(player, DARIN_RECEIPT, 1);
							giveItems(player, BAULRO_POTION, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30033-02.htm";
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
