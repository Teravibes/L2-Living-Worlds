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
package quests.Q00033_MakeAPairOfDressShoes;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00037_MakeFormalWear.Q00037_MakeFormalWear;

public class Q00033_MakeAPairOfDressShoes extends Quest
{
	// NPCs
	private static final int WOODLEY = 30838;
	private static final int IAN = 30164;
	private static final int LEIKAR = 31520;
	
	// Items
	private static final int LEATHER = 1882;
	private static final int THREAD = 1868;
	private static final int ADENA = 57;
	
	// Rewards
	public static final int DRESS_SHOES_BOX = 7113;
	
	public Q00033_MakeAPairOfDressShoes()
	{
		super(33, "Make a Pair of Dress Shoes");
		addStartNpc(WOODLEY);
		addTalkId(WOODLEY, IAN, LEIKAR);
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
			case "30838-1.htm":
			{
				st.startQuest();
				break;
			}
			case "31520-1.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "30838-3.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "30838-5.htm":
			{
				if ((getQuestItemsCount(player, LEATHER) >= 200) && (getQuestItemsCount(player, THREAD) >= 600) && (getQuestItemsCount(player, ADENA) >= 200000))
				{
					st.setCond(4, true);
					takeItems(player, ADENA, 200000);
					takeItems(player, LEATHER, 200);
					takeItems(player, THREAD, 600);
				}
				else
				{
					htmltext = "30838-4a.htm";
				}
				break;
			}
			case "30164-1.htm":
			{
				if (getQuestItemsCount(player, ADENA) >= 300000)
				{
					st.setCond(5, true);
					takeItems(player, ADENA, 300000);
				}
				else
				{
					htmltext = "30164-1a.htm";
				}
				break;
			}
			case "30838-7.htm":
			{
				giveItems(player, DRESS_SHOES_BOX, 1);
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
				if (player.getLevel() >= 60)
				{
					final QuestState fwear = player.getQuestState(Q00037_MakeFormalWear.class.getSimpleName());
					if ((fwear != null) && fwear.isCond(7))
					{
						htmltext = "30838-0.htm";
					}
					else
					{
						htmltext = "30838-0a.htm";
					}
				}
				else
				{
					htmltext = "30838-0b.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case WOODLEY:
					{
						if (cond == 1)
						{
							htmltext = "30838-1.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30838-2.htm";
						}
						else if (cond == 3)
						{
							if ((getQuestItemsCount(player, LEATHER) >= 200) && (getQuestItemsCount(player, THREAD) >= 600) && (getQuestItemsCount(player, ADENA) >= 200000))
							{
								htmltext = "30838-4.htm";
							}
							else
							{
								htmltext = "30838-4a.htm";
							}
						}
						else if (cond == 4)
						{
							htmltext = "30838-5a.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30838-6.htm";
						}
						break;
					}
					case LEIKAR:
					{
						if (cond == 1)
						{
							htmltext = "31520-0.htm";
						}
						else if (cond > 1)
						{
							htmltext = "31520-1a.htm";
						}
						break;
					}
					case IAN:
					{
						if (cond == 4)
						{
							htmltext = "30164-0.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30164-2.htm";
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
