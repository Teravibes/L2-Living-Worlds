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
package quests.Q00213_TrialOfTheSeeker;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00213_TrialOfTheSeeker extends Quest
{
	// NPCs
	private static final int TERRY = 30064;
	private static final int DUFNER = 30106;
	private static final int BRUNON = 30526;
	private static final int VIKTOR = 30684;
	private static final int MARINA = 30715;
	
	// Monsters
	private static final int NEER_GHOUL_BERSERKER = 20198;
	private static final int ANT_CAPTAIN = 20080;
	private static final int OL_MAHUM_CAPTAIN = 20211;
	private static final int TURAK_BUGBEAR_WARRIOR = 20249;
	private static final int TUREK_ORC_WARLORD = 20495;
	private static final int MEDUSA = 20158;
	private static final int ANT_WARRIOR_CAPTAIN = 20088;
	private static final int MARSH_STAKATO_DRONE = 20234;
	private static final int BREKA_ORC_OVERLORD = 20270;
	private static final int LETO_LIZARDMAN_WARRIOR = 20580;
	
	// Items
	private static final int DUFNER_LETTER = 2647;
	private static final int TERRY_ORDER_1 = 2648;
	private static final int TERRY_ORDER_2 = 2649;
	private static final int TERRY_LETTER = 2650;
	private static final int VIKTOR_LETTER = 2651;
	private static final int HAWKEYE_LETTER = 2652;
	private static final int MYSTERIOUS_RUNESTONE = 2653;
	private static final int OL_MAHUM_RUNESTONE = 2654;
	private static final int TUREK_RUNESTONE = 2655;
	private static final int ANT_RUNESTONE = 2656;
	private static final int TURAK_BUGBEAR_RUNESTONE = 2657;
	private static final int TERRY_BOX = 2658;
	private static final int VIKTOR_REQUEST = 2659;
	private static final int MEDUSA_SCALES = 2660;
	private static final int SHILEN_RUNESTONE = 2661;
	private static final int ANALYSIS_REQUEST = 2662;
	private static final int MARINA_LETTER = 2663;
	private static final int EXPERIMENT_TOOLS = 2664;
	private static final int ANALYSIS_RESULT = 2665;
	private static final int TERRY_ORDER_3 = 2666;
	private static final int LIST_OF_HOST = 2667;
	private static final int ABYSS_RUNESTONE_1 = 2668;
	private static final int ABYSS_RUNESTONE_2 = 2669;
	private static final int ABYSS_RUNESTONE_3 = 2670;
	private static final int ABYSS_RUNESTONE_4 = 2671;
	private static final int TERRY_REPORT = 2672;
	
	// Rewards
	private static final int MARK_OF_SEEKER = 2673;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00213_TrialOfTheSeeker()
	{
		super(213, "Trial of the Seeker");
		registerQuestItems(DUFNER_LETTER, TERRY_ORDER_1, TERRY_ORDER_2, TERRY_LETTER, VIKTOR_LETTER, HAWKEYE_LETTER, MYSTERIOUS_RUNESTONE, OL_MAHUM_RUNESTONE, TUREK_RUNESTONE, ANT_RUNESTONE, TURAK_BUGBEAR_RUNESTONE, TERRY_BOX, VIKTOR_REQUEST, MEDUSA_SCALES, SHILEN_RUNESTONE, ANALYSIS_REQUEST, MARINA_LETTER, EXPERIMENT_TOOLS, ANALYSIS_RESULT, TERRY_ORDER_3, LIST_OF_HOST, ABYSS_RUNESTONE_1, ABYSS_RUNESTONE_2, ABYSS_RUNESTONE_3, ABYSS_RUNESTONE_4, TERRY_REPORT);
		addStartNpc(DUFNER);
		addTalkId(TERRY, DUFNER, BRUNON, VIKTOR, MARINA);
		addKillId(NEER_GHOUL_BERSERKER, ANT_CAPTAIN, OL_MAHUM_CAPTAIN, TURAK_BUGBEAR_WARRIOR, TUREK_ORC_WARLORD, ANT_WARRIOR_CAPTAIN, MARSH_STAKATO_DRONE, BREKA_ORC_OVERLORD, LETO_LIZARDMAN_WARRIOR, MEDUSA);
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
			case "30106-05.htm":
			{
				st.startQuest();
				giveItems(player, DUFNER_LETTER, 1);
				if (!player.getVariables().getBoolean("secondClassChange35", false))
				{
					htmltext = "30106-05a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_35.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange35", true);
				}
				break;
			}
			case "30064-03.htm":
			{
				st.setCond(2, true);
				takeItems(player, DUFNER_LETTER, 1);
				giveItems(player, TERRY_ORDER_1, 1);
				break;
			}
			case "30064-06.htm":
			{
				st.setCond(4, true);
				takeItems(player, MYSTERIOUS_RUNESTONE, 1);
				takeItems(player, TERRY_ORDER_1, 1);
				giveItems(player, TERRY_ORDER_2, 1);
				break;
			}
			case "30064-10.htm":
			{
				st.setCond(6, true);
				takeItems(player, ANT_RUNESTONE, 1);
				takeItems(player, OL_MAHUM_RUNESTONE, 1);
				takeItems(player, TURAK_BUGBEAR_RUNESTONE, 1);
				takeItems(player, TUREK_RUNESTONE, 1);
				takeItems(player, TERRY_ORDER_2, 1);
				giveItems(player, TERRY_BOX, 1);
				giveItems(player, TERRY_LETTER, 1);
				break;
			}
			case "30064-18.htm":
			{
				if (player.getLevel() < 36)
				{
					htmltext = "30064-17.htm";
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, ANALYSIS_RESULT, 1);
					giveItems(player, TERRY_ORDER_3, 1);
				}
				else
				{
					st.setCond(16, true);
					takeItems(player, ANALYSIS_RESULT, 1);
					giveItems(player, LIST_OF_HOST, 1);
				}
				break;
			}
			case "30684-05.htm":
			{
				st.setCond(7, true);
				takeItems(player, TERRY_LETTER, 1);
				giveItems(player, VIKTOR_LETTER, 1);
				break;
			}
			case "30684-11.htm":
			{
				st.setCond(9, true);
				takeItems(player, TERRY_LETTER, 1);
				takeItems(player, TERRY_BOX, 1);
				takeItems(player, HAWKEYE_LETTER, 1);
				takeItems(player, VIKTOR_LETTER, 1);
				giveItems(player, VIKTOR_REQUEST, 1);
				break;
			}
			case "30684-15.htm":
			{
				st.setCond(11, true);
				takeItems(player, VIKTOR_REQUEST, 1);
				takeItems(player, MEDUSA_SCALES, 10);
				giveItems(player, ANALYSIS_REQUEST, 1);
				giveItems(player, SHILEN_RUNESTONE, 1);
				break;
			}
			case "30715-02.htm":
			{
				st.setCond(12, true);
				takeItems(player, SHILEN_RUNESTONE, 1);
				takeItems(player, ANALYSIS_REQUEST, 1);
				giveItems(player, MARINA_LETTER, 1);
				break;
			}
			case "30715-05.htm":
			{
				st.setCond(14, true);
				takeItems(player, EXPERIMENT_TOOLS, 1);
				giveItems(player, ANALYSIS_RESULT, 1);
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
				if ((player.getPlayerClass() == PlayerClass.ROGUE) || (player.getPlayerClass() == PlayerClass.ELVEN_SCOUT) || (player.getPlayerClass() == PlayerClass.ASSASSIN))
				{
					htmltext = (player.getLevel() < 35) ? "30106-02.htm" : "30106-03.htm";
				}
				else
				{
					htmltext = "30106-00.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case DUFNER:
					{
						if (cond == 1)
						{
							htmltext = "30106-06.htm";
						}
						else if (cond > 1)
						{
							if (!hasQuestItems(player, TERRY_REPORT))
							{
								htmltext = "30106-07.htm";
							}
							else
							{
								htmltext = "30106-08.htm";
								takeItems(player, TERRY_REPORT, 1);
								giveItems(player, MARK_OF_SEEKER, 1);
								addExpAndSp(player, 72126, 11000);
								player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
								st.exitQuest(false, true);
							}
						}
						break;
					}
					case TERRY:
					{
						if (cond == 1)
						{
							htmltext = "30064-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30064-04.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30064-05.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30064-08.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30064-09.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30064-11.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30064-12.htm";
							st.setCond(8, true);
							takeItems(player, VIKTOR_LETTER, 1);
							giveItems(player, HAWKEYE_LETTER, 1);
						}
						else if (cond == 8)
						{
							htmltext = "30064-13.htm";
						}
						else if ((cond > 8) && (cond < 14))
						{
							htmltext = "30064-14.htm";
						}
						else if (cond == 14)
						{
							if (!hasQuestItems(player, TERRY_ORDER_3))
							{
								htmltext = "30064-15.htm";
							}
							else if (player.getLevel() < 36)
							{
								htmltext = "30064-20.htm";
							}
							else
							{
								htmltext = "30064-21.htm";
								st.setCond(15, true);
								takeItems(player, TERRY_ORDER_3, 1);
								giveItems(player, LIST_OF_HOST, 1);
							}
						}
						else if ((cond == 15) || (cond == 16))
						{
							htmltext = "30064-22.htm";
						}
						else if (cond == 17)
						{
							if (!hasQuestItems(player, TERRY_REPORT))
							{
								htmltext = "30064-23.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
								takeItems(player, LIST_OF_HOST, 1);
								takeItems(player, ABYSS_RUNESTONE_1, 1);
								takeItems(player, ABYSS_RUNESTONE_2, 1);
								takeItems(player, ABYSS_RUNESTONE_3, 1);
								takeItems(player, ABYSS_RUNESTONE_4, 1);
								giveItems(player, TERRY_REPORT, 1);
							}
							else
							{
								htmltext = "30064-24.htm";
							}
						}
						break;
					}
					case VIKTOR:
					{
						if (cond == 6)
						{
							htmltext = "30684-01.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30684-05.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30684-12.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30684-13.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30684-14.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30684-16.htm";
						}
						else if (cond > 11)
						{
							htmltext = "30684-17.htm";
						}
						break;
					}
					case MARINA:
					{
						if (cond == 11)
						{
							htmltext = "30715-01.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30715-03.htm";
						}
						else if (cond == 13)
						{
							htmltext = "30715-04.htm";
						}
						else if (hasQuestItems(player, ANALYSIS_RESULT))
						{
							htmltext = "30715-06.htm";
						}
						break;
					}
					case BRUNON:
					{
						if (cond == 12)
						{
							htmltext = "30526-01.htm";
							st.setCond(13, true);
							takeItems(player, MARINA_LETTER, 1);
							giveItems(player, EXPERIMENT_TOOLS, 1);
						}
						else if (cond == 13)
						{
							htmltext = "30526-02.htm";
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
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		switch (npc.getId())
		{
			case NEER_GHOUL_BERSERKER:
			{
				if (st.isCond(2) && (getRandom(100) < 10))
				{
					giveItems(player, MYSTERIOUS_RUNESTONE, 1);
					st.setCond(3, true);
				}
				break;
			}
			case ANT_CAPTAIN:
			{
				if (st.isCond(4) && !hasQuestItems(player, ANT_RUNESTONE) && (getRandom(100) < 25))
				{
					giveItems(player, ANT_RUNESTONE, 1);
					if (hasQuestItems(player, OL_MAHUM_RUNESTONE, TURAK_BUGBEAR_RUNESTONE, TUREK_RUNESTONE))
					{
						st.setCond(5, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case OL_MAHUM_CAPTAIN:
			{
				if (st.isCond(4) && !hasQuestItems(player, OL_MAHUM_RUNESTONE) && (getRandom(100) < 25))
				{
					giveItems(player, OL_MAHUM_RUNESTONE, 1);
					if (hasQuestItems(player, ANT_RUNESTONE, TURAK_BUGBEAR_RUNESTONE, TUREK_RUNESTONE))
					{
						st.setCond(5, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case TURAK_BUGBEAR_WARRIOR:
			{
				if (st.isCond(4) && !hasQuestItems(player, TURAK_BUGBEAR_RUNESTONE) && (getRandom(100) < 25))
				{
					giveItems(player, TURAK_BUGBEAR_RUNESTONE, 1);
					if (hasQuestItems(player, ANT_RUNESTONE, OL_MAHUM_RUNESTONE, TUREK_RUNESTONE))
					{
						st.setCond(5, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case TUREK_ORC_WARLORD:
			{
				if (st.isCond(4) && !hasQuestItems(player, TUREK_RUNESTONE) && (getRandom(100) < 25))
				{
					giveItems(player, TUREK_RUNESTONE, 1);
					if (hasQuestItems(player, ANT_RUNESTONE, OL_MAHUM_RUNESTONE, TURAK_BUGBEAR_RUNESTONE))
					{
						st.setCond(5, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case MEDUSA:
			{
				if (st.isCond(9) && (getRandom(100) < 30))
				{
					giveItems(player, MEDUSA_SCALES, 1);
					if (getQuestItemsCount(player, MEDUSA_SCALES) >= 10)
					{
						st.setCond(10, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case MARSH_STAKATO_DRONE:
			{
				if ((st.isCond(15) || st.isCond(16)) && !hasQuestItems(player, ABYSS_RUNESTONE_1) && (getRandom(100) < 25))
				{
					giveItems(player, ABYSS_RUNESTONE_1, 1);
					if (hasQuestItems(player, ABYSS_RUNESTONE_2, ABYSS_RUNESTONE_3, ABYSS_RUNESTONE_4))
					{
						st.setCond(17, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case BREKA_ORC_OVERLORD:
			{
				if ((st.isCond(15) || st.isCond(16)) && !hasQuestItems(player, ABYSS_RUNESTONE_2) && (getRandom(100) < 25))
				{
					giveItems(player, ABYSS_RUNESTONE_2, 1);
					if (hasQuestItems(player, ABYSS_RUNESTONE_1, ABYSS_RUNESTONE_3, ABYSS_RUNESTONE_4))
					{
						st.setCond(17, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case ANT_WARRIOR_CAPTAIN:
			{
				if ((st.isCond(15) || st.isCond(16)) && !hasQuestItems(player, ABYSS_RUNESTONE_3) && (getRandom(100) < 25))
				{
					giveItems(player, ABYSS_RUNESTONE_3, 1);
					if (hasQuestItems(player, ABYSS_RUNESTONE_1, ABYSS_RUNESTONE_2, ABYSS_RUNESTONE_4))
					{
						st.setCond(17, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case LETO_LIZARDMAN_WARRIOR:
			{
				if ((st.isCond(15) || st.isCond(16)) && !hasQuestItems(player, ABYSS_RUNESTONE_4) && (getRandom(100) < 25))
				{
					giveItems(player, ABYSS_RUNESTONE_4, 1);
					if (hasQuestItems(player, ABYSS_RUNESTONE_1, ABYSS_RUNESTONE_2, ABYSS_RUNESTONE_3))
					{
						st.setCond(17, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
		}
	}
}
