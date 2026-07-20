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
package quests.Q00126_TheNameOfEvil2;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00125_TheNameOfEvil1.Q00125_TheNameOfEvil1;

public class Q00126_TheNameOfEvil2 extends Quest
{
	private static final int MUSHIKA = 32114;
	private static final int ASAMANAH = 32115;
	private static final int ULU_KAIMU = 32119;
	private static final int BALU_KAIMU = 32120;
	private static final int CHUTA_KAIMU = 32121;
	private static final int WARRIOR_GRAVE = 32122;
	private static final int SHILEN_STONE_STATUE = 32109;
	private static final int BONEPOWDER = 8783;
	private static final int EWA = 729;
	
	public Q00126_TheNameOfEvil2()
	{
		super(126, "The Name of Evil - 2");
		addStartNpc(ASAMANAH);
		addTalkId(ASAMANAH, MUSHIKA, ULU_KAIMU, BALU_KAIMU, CHUTA_KAIMU, WARRIOR_GRAVE, SHILEN_STONE_STATUE);
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
			case "32115-05.htm":
			{
				st.startQuest();
				break;
			}
			case "32115-10.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "32119-02.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "32119-09.htm":
			{
				st.setCond(4, true);
				break;
			}
			case "32119-11.htm":
			{
				st.setCond(5, true);
				break;
			}
			case "32120-07.htm":
			{
				st.setCond(6, true);
				break;
			}
			case "32120-09.htm":
			{
				st.setCond(7, true);
				break;
			}
			case "32120-11.htm":
			{
				st.setCond(8, true);
				break;
			}
			case "32121-07.htm":
			{
				st.setCond(9, true);
				break;
			}
			case "32121-10.htm":
			{
				st.setCond(10, true);
				break;
			}
			case "32121-15.htm":
			{
				st.setCond(11, true);
				break;
			}
			case "32122-03.htm":
			{
				st.setCond(12, true);
				break;
			}
			case "32122-15.htm":
			{
				st.setCond(13, true);
				break;
			}
			case "32122-18.htm":
			{
				st.setCond(14, true);
				break;
			}
			case "32122-87.htm":
			{
				giveItems(player, BONEPOWDER, 1);
				break;
			}
			case "32122-90.htm":
			{
				st.setCond(18, true);
				break;
			}
			case "32109-02.htm":
			{
				st.setCond(19, true);
				break;
			}
			case "32109-19.htm":
			{
				st.setCond(20, true);
				takeItems(player, BONEPOWDER, 1);
				break;
			}
			case "32115-21.htm":
			{
				st.setCond(21, true);
				break;
			}
			case "32115-28.htm":
			{
				st.setCond(22, true);
				break;
			}
			case "32114-08.htm":
			{
				st.setCond(23, true);
				break;
			}
			case "32114-09.htm":
			{
				giveItems(player, EWA, 1);
				st.exitQuest(false, true);
				break;
			}
			case "DOOne":
			{
				htmltext = "32122-26.htm";
				if (st.getInt("DO") < 1)
				{
					st.set("DO", "1");
				}
				break;
			}
			case "MIOne":
			{
				htmltext = "32122-30.htm";
				if (st.getInt("MI") < 1)
				{
					st.set("MI", "1");
				}
				break;
			}
			case "FAOne":
			{
				htmltext = "32122-34.htm";
				if (st.getInt("FA") < 1)
				{
					st.set("FA", "1");
				}
				break;
			}
			case "SOLOne":
			{
				htmltext = "32122-38.htm";
				if (st.getInt("SOL") < 1)
				{
					st.set("SOL", "1");
				}
				break;
			}
			case "FA_2One":
			{
				if (st.getInt("FA_2") < 1)
				{
					st.set("FA_2", "1");
				}
				
				htmltext = getSongOne(st);
				break;
			}
			case "FATwo":
			{
				htmltext = "32122-47.htm";
				if (st.getInt("FA") < 1)
				{
					st.set("FA", "1");
				}
				break;
			}
			case "SOLTwo":
			{
				htmltext = "32122-51.htm";
				if (st.getInt("SOL") < 1)
				{
					st.set("SOL", "1");
				}
				break;
			}
			case "TITwo":
			{
				htmltext = "32122-55.htm";
				if (st.getInt("TI") < 1)
				{
					st.set("TI", "1");
				}
				break;
			}
			case "SOL_2Two":
			{
				htmltext = "32122-59.htm";
				if (st.getInt("SOL_2") < 1)
				{
					st.set("SOL_2", "1");
				}
				break;
			}
			case "FA_2Two":
			{
				if (st.getInt("FA_2") < 1)
				{
					st.set("FA_2", "1");
				}
				
				htmltext = getSongTwo(st);
				break;
			}
			case "SOLTri":
			{
				htmltext = "32122-68.htm";
				if (st.getInt("SOL") < 1)
				{
					st.set("SOL", "1");
				}
				break;
			}
			case "FATri":
			{
				htmltext = "32122-72.htm";
				if (st.getInt("FA") < 1)
				{
					st.set("FA", "1");
				}
				break;
			}
			case "MITri":
			{
				htmltext = "32122-76.htm";
				if (st.getInt("MI") < 1)
				{
					st.set("MI", "1");
				}
				break;
			}
			case "FA_2Tri":
			{
				htmltext = "32122-80.htm";
				if (st.getInt("FA_2") < 1)
				{
					st.set("FA_2", "1");
				}
				break;
			}
			case "MI_2Tri":
			{
				if (st.getInt("MI_2") < 1)
				{
					st.set("MI_2", "1");
				}
				
				htmltext = getSongTri(st);
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
				if (player.getLevel() < 77)
				{
					htmltext = "32115-02.htm";
				}
				else
				{
					final QuestState st2 = player.getQuestState(Q00125_TheNameOfEvil1.class.getSimpleName());
					if ((st2 != null) && st2.isCompleted())
					{
						htmltext = "32115-01.htm";
					}
					else
					{
						htmltext = "32115-04.htm";
					}
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ASAMANAH:
					{
						if (cond == 1)
						{
							htmltext = "32115-11.htm";
							st.setCond(2, true);
						}
						else if ((cond > 1) && (cond < 20))
						{
							htmltext = "32115-12.htm";
						}
						else if (cond == 20)
						{
							htmltext = "32115-13.htm";
						}
						else if (cond == 21)
						{
							htmltext = "32115-22.htm";
						}
						else if (cond == 22)
						{
							htmltext = "32115-29.htm";
						}
						break;
					}
					case ULU_KAIMU:
					{
						if (cond == 1)
						{
							htmltext = "32119-01a.htm";
						}
						else if (cond == 2)
						{
							htmltext = "32119-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "32119-08.htm";
						}
						else if (cond == 4)
						{
							htmltext = "32119-09.htm";
						}
						else if (cond > 4)
						{
							htmltext = "32119-12.htm";
						}
						break;
					}
					case BALU_KAIMU:
					{
						if (cond < 5)
						{
							htmltext = "32120-02.htm";
						}
						else if (cond == 5)
						{
							htmltext = "32120-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = "32120-03.htm";
						}
						else if (cond == 7)
						{
							htmltext = "32120-08.htm";
						}
						else if (cond > 7)
						{
							htmltext = "32120-12.htm";
						}
						break;
					}
					case CHUTA_KAIMU:
					{
						if (cond < 8)
						{
							htmltext = "32121-02.htm";
						}
						else if (cond == 8)
						{
							htmltext = "32121-01.htm";
						}
						else if (cond == 9)
						{
							htmltext = "32121-03.htm";
						}
						else if (cond == 10)
						{
							htmltext = "32121-10.htm";
						}
						else if (cond > 10)
						{
							htmltext = "32121-16.htm";
						}
						break;
					}
					case WARRIOR_GRAVE:
					{
						if (cond < 11)
						{
							htmltext = "32122-02.htm";
						}
						else if (cond == 11)
						{
							htmltext = "32122-01.htm";
						}
						else if (cond == 12)
						{
							htmltext = "32122-15.htm";
						}
						else if (cond == 13)
						{
							htmltext = "32122-18.htm";
						}
						else if (cond == 14)
						{
							htmltext = "32122-24.htm";
						}
						else if (cond == 15)
						{
							htmltext = "32122-45.htm";
						}
						else if (cond == 16)
						{
							htmltext = "32122-66.htm";
						}
						else if (cond == 17)
						{
							htmltext = "32122-84.htm";
						}
						else if (cond == 18)
						{
							htmltext = "32122-91.htm";
						}
						break;
					}
					case SHILEN_STONE_STATUE:
					{
						if (cond < 18)
						{
							htmltext = "32109-03.htm";
						}
						else if (cond == 18)
						{
							htmltext = "32109-02.htm";
						}
						else if (cond == 19)
						{
							htmltext = "32109-05.htm";
						}
						else if (cond > 19)
						{
							htmltext = "32109-04.htm";
						}
						break;
					}
					case MUSHIKA:
					{
						if (cond < 22)
						{
							htmltext = "32114-02.htm";
						}
						else if (cond == 22)
						{
							htmltext = "32114-01.htm";
						}
						else if (cond == 23)
						{
							htmltext = "32114-04.htm";
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
	
	private static String getSongOne(QuestState st)
	{
		String htmltext = "32122-24.htm";
		if (st.isCond(14) && (st.getInt("DO") > 0) && (st.getInt("MI") > 0) && (st.getInt("FA") > 0) && (st.getInt("SOL") > 0) && (st.getInt("FA_2") > 0))
		{
			htmltext = "32122-42.htm";
			st.setCond(15, true);
			st.unset("DO");
			st.unset("MI");
			st.unset("FA");
			st.unset("SOL");
			st.unset("FA_2");
		}
		
		return htmltext;
	}
	
	private static String getSongTwo(QuestState st)
	{
		String htmltext = "32122-45.htm";
		if (st.isCond(15) && (st.getInt("FA") > 0) && (st.getInt("SOL") > 0) && (st.getInt("TI") > 0) && (st.getInt("SOL_2") > 0) && (st.getInt("FA_2") > 0))
		{
			htmltext = "32122-63.htm";
			st.setCond(16, true);
			st.unset("FA");
			st.unset("SOL");
			st.unset("TI");
			st.unset("SOL_2");
			st.unset("FA3_2");
		}
		
		return htmltext;
	}
	
	private static String getSongTri(QuestState st)
	{
		String htmltext = "32122-66.htm";
		if (st.isCond(16) && (st.getInt("SOL") > 0) && (st.getInt("FA") > 0) && (st.getInt("MI") > 0) && (st.getInt("FA_2") > 0) && (st.getInt("MI_2") > 0))
		{
			htmltext = "32122-84.htm";
			st.setCond(17, true);
			st.unset("SOL");
			st.unset("FA");
			st.unset("MI");
			st.unset("FA_2");
			st.unset("MI_2");
		}
		
		return htmltext;
	}
}
