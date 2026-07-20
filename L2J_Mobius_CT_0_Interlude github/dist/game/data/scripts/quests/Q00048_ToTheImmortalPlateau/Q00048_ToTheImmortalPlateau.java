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
package quests.Q00048_ToTheImmortalPlateau;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00048_ToTheImmortalPlateau extends Quest
{
	// NPCs
	private static final int GALLADUCCI = 30097;
	private static final int GENTLER = 30094;
	private static final int SANDRA = 30090;
	private static final int DUSTIN = 30116;
	
	// Items
	private static final int ORDER_DOCUMENT_1 = 7563;
	private static final int ORDER_DOCUMENT_2 = 7564;
	private static final int ORDER_DOCUMENT_3 = 7565;
	private static final int MAGIC_SWORD_HILT = 7568;
	private static final int GEMSTONE_POWDER = 7567;
	private static final int PURIFIED_MAGIC_NECKLACE = 7566;
	private static final int MARK_OF_TRAVELER = 7570;
	private static final int SCROLL_OF_ESCAPE_SPECIAL = 7557;
	
	public Q00048_ToTheImmortalPlateau()
	{
		super(48, "To the Immortal Plateau");
		registerQuestItems(ORDER_DOCUMENT_1, ORDER_DOCUMENT_2, ORDER_DOCUMENT_3, MAGIC_SWORD_HILT, GEMSTONE_POWDER, PURIFIED_MAGIC_NECKLACE);
		addStartNpc(GALLADUCCI);
		addTalkId(GALLADUCCI, SANDRA, DUSTIN, GENTLER);
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
			case "30097-03.htm":
			{
				st.startQuest();
				giveItems(player, ORDER_DOCUMENT_1, 1);
				break;
			}
			case "30094-02.htm":
			{
				st.setCond(2, true);
				takeItems(player, ORDER_DOCUMENT_1, 1);
				giveItems(player, MAGIC_SWORD_HILT, 1);
				break;
			}
			case "30097-06.htm":
			{
				st.setCond(3, true);
				takeItems(player, MAGIC_SWORD_HILT, 1);
				giveItems(player, ORDER_DOCUMENT_2, 1);
				break;
			}
			case "30090-02.htm":
			{
				st.setCond(4, true);
				takeItems(player, ORDER_DOCUMENT_2, 1);
				giveItems(player, GEMSTONE_POWDER, 1);
				break;
			}
			case "30097-09.htm":
			{
				st.setCond(5, true);
				takeItems(player, GEMSTONE_POWDER, 1);
				giveItems(player, ORDER_DOCUMENT_3, 1);
				break;
			}
			case "30116-02.htm":
			{
				st.setCond(6, true);
				takeItems(player, ORDER_DOCUMENT_3, 1);
				giveItems(player, PURIFIED_MAGIC_NECKLACE, 1);
				break;
			}
			case "30097-12.htm":
			{
				takeItems(player, MARK_OF_TRAVELER, -1);
				takeItems(player, PURIFIED_MAGIC_NECKLACE, 1);
				rewardItems(player, SCROLL_OF_ESCAPE_SPECIAL, 1);
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
				if ((player.getRace() == Race.ORC) && (player.getLevel() >= 3))
				{
					if (hasQuestItems(player, MARK_OF_TRAVELER))
					{
						htmltext = "30097-02.htm";
					}
					else
					{
						htmltext = "30097-01.htm";
					}
				}
				else
				{
					htmltext = "30097-01a.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case GALLADUCCI:
					{
						if (cond == 1)
						{
							htmltext = "30097-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30097-05.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30097-07.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30097-08.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30097-10.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30097-11.htm";
						}
						break;
					}
					case GENTLER:
					{
						if (cond == 1)
						{
							htmltext = "30094-01.htm";
						}
						else if (cond > 1)
						{
							htmltext = "30094-03.htm";
						}
						break;
					}
					case SANDRA:
					{
						if (cond == 3)
						{
							htmltext = "30090-01.htm";
						}
						else if (cond > 3)
						{
							htmltext = "30090-03.htm";
						}
						break;
					}
					case DUSTIN:
					{
						if (cond == 5)
						{
							htmltext = "30116-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30116-03.htm";
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
