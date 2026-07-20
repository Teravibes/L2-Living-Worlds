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
package quests.Q00622_SpecialtyLiquorDelivery;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00622_SpecialtyLiquorDelivery extends Quest
{
	// NPCs
	private static final int JEREMY = 31521;
	private static final int PULIN = 31543;
	private static final int NAFF = 31544;
	private static final int CROCUS = 31545;
	private static final int KUBER = 31546;
	private static final int BEOLIN = 31547;
	private static final int LIETTA = 31267;
	
	// Items
	private static final int SPECIAL_DRINK = 7197;
	private static final int FEE_OF_SPECIAL_DRINK = 7198;
	
	// Rewards
	private static final int ADENA = 57;
	private static final int HASTE_POTION = 1062;
	private static final int[] RECIPES =
	{
		6847,
		6849,
		6851
	};
	
	public Q00622_SpecialtyLiquorDelivery()
	{
		super(622, "Specialty Liquor Delivery");
		registerQuestItems(SPECIAL_DRINK, FEE_OF_SPECIAL_DRINK);
		addStartNpc(JEREMY);
		addTalkId(JEREMY, PULIN, NAFF, CROCUS, KUBER, BEOLIN, LIETTA);
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
			case "31521-02.htm":
			{
				st.startQuest();
				giveItems(player, SPECIAL_DRINK, 5);
				break;
			}
			case "31547-02.htm":
			{
				st.setCond(2, true);
				takeItems(player, SPECIAL_DRINK, 1);
				giveItems(player, FEE_OF_SPECIAL_DRINK, 1);
				break;
			}
			case "31546-02.htm":
			{
				st.setCond(3, true);
				takeItems(player, SPECIAL_DRINK, 1);
				giveItems(player, FEE_OF_SPECIAL_DRINK, 1);
				break;
			}
			case "31545-02.htm":
			{
				st.setCond(4, true);
				takeItems(player, SPECIAL_DRINK, 1);
				giveItems(player, FEE_OF_SPECIAL_DRINK, 1);
				break;
			}
			case "31544-02.htm":
			{
				st.setCond(5, true);
				takeItems(player, SPECIAL_DRINK, 1);
				giveItems(player, FEE_OF_SPECIAL_DRINK, 1);
				break;
			}
			case "31543-02.htm":
			{
				st.setCond(6, true);
				takeItems(player, SPECIAL_DRINK, 1);
				giveItems(player, FEE_OF_SPECIAL_DRINK, 1);
				break;
			}
			case "31521-06.htm":
			{
				st.setCond(7, true);
				takeItems(player, FEE_OF_SPECIAL_DRINK, 5);
				break;
			}
			case "31267-02.htm":
			{
				if (getRandom(5) < 1)
				{
					giveItems(player, RECIPES[getRandom(RECIPES.length)], 1);
				}
				else
				{
					rewardItems(player, ADENA, 18800);
					rewardItems(player, HASTE_POTION, 1);
				}
				
				st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < 68) ? "31521-03.htm" : "31521-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case JEREMY:
					{
						if (cond < 6)
						{
							htmltext = "31521-04.htm";
						}
						else if (cond == 6)
						{
							htmltext = "31521-05.htm";
						}
						else if (cond == 7)
						{
							htmltext = "31521-06.htm";
						}
						break;
					}
					case BEOLIN:
					{
						if ((cond == 1) && (getQuestItemsCount(player, SPECIAL_DRINK) == 5))
						{
							htmltext = "31547-01.htm";
						}
						else if (cond > 1)
						{
							htmltext = "31547-03.htm";
						}
						break;
					}
					case KUBER:
					{
						if ((cond == 2) && (getQuestItemsCount(player, SPECIAL_DRINK) == 4))
						{
							htmltext = "31546-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "31546-03.htm";
						}
						break;
					}
					case CROCUS:
					{
						if ((cond == 3) && (getQuestItemsCount(player, SPECIAL_DRINK) == 3))
						{
							htmltext = "31545-01.htm";
						}
						else if (cond > 3)
						{
							htmltext = "31545-03.htm";
						}
						break;
					}
					case NAFF:
					{
						if ((cond == 4) && (getQuestItemsCount(player, SPECIAL_DRINK) == 2))
						{
							htmltext = "31544-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "31544-03.htm";
						}
						break;
					}
					case PULIN:
					{
						if ((cond == 5) && (getQuestItemsCount(player, SPECIAL_DRINK) == 1))
						{
							htmltext = "31543-01.htm";
						}
						else if (cond > 5)
						{
							htmltext = "31543-03.htm";
						}
						break;
					}
					case LIETTA:
					{
						if (cond == 7)
						{
							htmltext = "31267-01.htm";
						}
						break;
					}
				}
			}
		}
		
		return htmltext;
	}
}
