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
package quests.Q00032_AnObviousLie;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00032_AnObviousLie extends Quest
{
	// NPCs
	private static final int GENTLER = 30094;
	private static final int MAXIMILIAN = 30120;
	private static final int MIKI_THE_CAT = 31706;
	
	// Items
	private static final int SUEDE = 1866;
	private static final int THREAD = 1868;
	private static final int SPIRIT_ORE = 3031;
	private static final int MAP = 7165;
	private static final int MEDICINAL_HERB = 7166;
	
	// Rewards
	private static final int CAT_EARS = 6843;
	private static final int RACOON_EARS = 7680;
	private static final int RABBIT_EARS = 7683;
	
	public Q00032_AnObviousLie()
	{
		super(32, "An Obvious Lie");
		registerQuestItems(MAP, MEDICINAL_HERB);
		addStartNpc(MAXIMILIAN);
		addTalkId(MAXIMILIAN, GENTLER, MIKI_THE_CAT);
		addKillId(20135); // Alligator
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
			case "30120-1.htm":
			{
				st.startQuest();
				break;
			}
			case "30094-1.htm":
			{
				st.setCond(2, true);
				giveItems(player, MAP, 1);
				break;
			}
			case "31706-1.htm":
			{
				st.setCond(3, true);
				takeItems(player, MAP, 1);
				break;
			}
			case "30094-4.htm":
			{
				st.setCond(5, true);
				takeItems(player, MEDICINAL_HERB, 20);
				break;
			}
			case "30094-7.htm":
			{
				if (getQuestItemsCount(player, SPIRIT_ORE) < 500)
				{
					htmltext = "30094-5.htm";
				}
				else
				{
					st.setCond(6, true);
					takeItems(player, SPIRIT_ORE, 500);
				}
				break;
			}
			case "31706-4.htm":
			{
				st.setCond(7, true);
				break;
			}
			case "30094-10.htm":
			{
				st.setCond(8, true);
				break;
			}
			case "30094-13.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				break;
			}
			case "cat":
			{
				if ((getQuestItemsCount(player, THREAD) < 1000) || (getQuestItemsCount(player, SUEDE) < 500))
				{
					htmltext = "30094-11.htm";
				}
				else
				{
					htmltext = "30094-14.htm";
					takeItems(player, SUEDE, 500);
					takeItems(player, THREAD, 1000);
					giveItems(player, CAT_EARS, 1);
					st.exitQuest(false, true);
				}
				break;
			}
			case "racoon":
			{
				if ((getQuestItemsCount(player, THREAD) < 1000) || (getQuestItemsCount(player, SUEDE) < 500))
				{
					htmltext = "30094-11.htm";
				}
				else
				{
					htmltext = "30094-14.htm";
					takeItems(player, SUEDE, 500);
					takeItems(player, THREAD, 1000);
					giveItems(player, RACOON_EARS, 1);
					st.exitQuest(false, true);
				}
				break;
			}
			case "rabbit":
			{
				if ((getQuestItemsCount(player, THREAD) < 1000) || (getQuestItemsCount(player, SUEDE) < 500))
				{
					htmltext = "30094-11.htm";
				}
				else
				{
					htmltext = "30094-14.htm";
					takeItems(player, SUEDE, 500);
					takeItems(player, THREAD, 1000);
					giveItems(player, RABBIT_EARS, 1);
					st.exitQuest(false, true);
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
				htmltext = (player.getLevel() < 45) ? "30120-0a.htm" : "30120-0.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MAXIMILIAN:
					{
						htmltext = "30120-2.htm";
						break;
					}
					case GENTLER:
					{
						if (cond == 1)
						{
							htmltext = "30094-0.htm";
						}
						else if ((cond == 2) || (cond == 3))
						{
							htmltext = "30094-2.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30094-3.htm";
						}
						else if (cond == 5)
						{
							htmltext = (getQuestItemsCount(player, SPIRIT_ORE) < 500) ? "30094-5.htm" : "30094-6.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30094-8.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30094-9.htm";
						}
						else if (cond == 8)
						{
							htmltext = ((getQuestItemsCount(player, THREAD) < 1000) || (getQuestItemsCount(player, SUEDE) < 500)) ? "30094-11.htm" : "30094-12.htm";
						}
						break;
					}
					case MIKI_THE_CAT:
					{
						if (cond == 2)
						{
							htmltext = "31706-0.htm";
						}
						else if ((cond > 2) && (cond < 6))
						{
							htmltext = "31706-2.htm";
						}
						else if (cond == 6)
						{
							htmltext = "31706-3.htm";
						}
						else if (cond > 6)
						{
							htmltext = "31706-5.htm";
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
		final QuestState qs = getRandomPartyMemberState(killer, 3, 3, npc);
		if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, MEDICINAL_HERB, 1, 20, 1, true))
		{
			qs.setCond(4, true);
		}
	}
}
