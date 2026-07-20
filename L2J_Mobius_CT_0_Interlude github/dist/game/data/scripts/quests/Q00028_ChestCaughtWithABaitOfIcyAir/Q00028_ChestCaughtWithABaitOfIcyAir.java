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
package quests.Q00028_ChestCaughtWithABaitOfIcyAir;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00051_OFullesSpecialBait.Q00051_OFullesSpecialBait;

public class Q00028_ChestCaughtWithABaitOfIcyAir extends Quest
{
	// NPCs
	private static final int OFULLE = 31572;
	private static final int KIKI = 31442;
	
	// Items
	private static final int BIG_YELLOW_TREASURE_CHEST = 6503;
	private static final int KIKI_LETTER = 7626;
	private static final int ELVEN_RING = 881;
	
	public Q00028_ChestCaughtWithABaitOfIcyAir()
	{
		super(28, "Chest caught with a bait of icy air");
		registerQuestItems(KIKI_LETTER);
		addStartNpc(OFULLE);
		addTalkId(OFULLE, KIKI);
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
			case "31572-04.htm":
			{
				st.startQuest();
				break;
			}
			case "31572-07.htm":
			{
				if (hasQuestItems(player, BIG_YELLOW_TREASURE_CHEST))
				{
					st.setCond(2);
					takeItems(player, BIG_YELLOW_TREASURE_CHEST, 1);
					giveItems(player, KIKI_LETTER, 1);
				}
				else
				{
					htmltext = "31572-08.htm";
				}
				break;
			}
			case "31442-02.htm":
			{
				if (hasQuestItems(player, KIKI_LETTER))
				{
					htmltext = "31442-02.htm";
					takeItems(player, KIKI_LETTER, 1);
					giveItems(player, ELVEN_RING, 1);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "31442-03.htm";
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
				if (player.getLevel() < 36)
				{
					htmltext = "31572-02.htm";
				}
				else
				{
					final QuestState st2 = player.getQuestState(Q00051_OFullesSpecialBait.class.getSimpleName());
					if ((st2 != null) && st2.isCompleted())
					{
						htmltext = "31572-01.htm";
					}
					else
					{
						htmltext = "31572-03.htm";
					}
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case OFULLE:
					{
						if (cond == 1)
						{
							htmltext = (!hasQuestItems(player, BIG_YELLOW_TREASURE_CHEST)) ? "31572-06.htm" : "31572-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31572-09.htm";
						}
						break;
					}
					case KIKI:
					{
						if (cond == 2)
						{
							htmltext = "31442-01.htm";
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
