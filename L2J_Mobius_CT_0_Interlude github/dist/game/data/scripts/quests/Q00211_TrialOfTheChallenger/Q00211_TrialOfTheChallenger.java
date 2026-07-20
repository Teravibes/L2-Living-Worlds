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
package quests.Q00211_TrialOfTheChallenger;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00211_TrialOfTheChallenger extends Quest
{
	// NPCs
	private static final int FILAUR = 30535;
	private static final int KASH = 30644;
	private static final int MARTIEN = 30645;
	private static final int RALDO = 30646;
	private static final int CHEST_OF_SHYSLASSYS = 30647;
	
	// Monsters
	private static final int SHYSLASSYS = 27110;
	private static final int GORR = 27112;
	private static final int BARAHAM = 27113;
	private static final int SUCCUBUS_QUEEN = 27114;
	
	// Items
	private static final int LETTER_OF_KASH = 2628;
	private static final int WATCHER_EYE_1 = 2629;
	private static final int WATCHER_EYE_2 = 2630;
	private static final int SCROLL_OF_SHYSLASSYS = 2631;
	private static final int BROKEN_KEY = 2632;
	
	// Rewards
	private static final int ADENA = 57;
	private static final int ELVEN_NECKLACE_BEADS = 1904;
	private static final int WHITE_TUNIC_PATTERN = 1936;
	private static final int IRON_BOOTS_DESIGN = 1940;
	private static final int MANTICOR_SKIN_GAITERS_PATTERN = 1943;
	private static final int RIP_GAUNTLETS_PATTERN = 1946;
	private static final int TOME_OF_BLOOD_PAGE = 2030;
	private static final int MITHRIL_SCALE_GAITERS_MATERIAL = 2918;
	private static final int BRIGANDINE_GAUNTLETS_PATTERN = 2927;
	private static final int MARK_OF_CHALLENGER = 2627;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00211_TrialOfTheChallenger()
	{
		super(211, "Trial of the Challenger");
		registerQuestItems(LETTER_OF_KASH, WATCHER_EYE_1, WATCHER_EYE_2, SCROLL_OF_SHYSLASSYS, BROKEN_KEY);
		addStartNpc(KASH);
		addTalkId(FILAUR, KASH, MARTIEN, RALDO, CHEST_OF_SHYSLASSYS);
		addKillId(SHYSLASSYS, GORR, BARAHAM, SUCCUBUS_QUEEN);
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
			case "30644-05.htm":
			{
				st.startQuest();
				if (!player.getVariables().getBoolean("secondClassChange35", false))
				{
					htmltext = "30644-05a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_35.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange35", true);
				}
				break;
			}
			case "30645-02.htm":
			{
				st.setCond(4, true);
				takeItems(player, LETTER_OF_KASH, 1);
				break;
			}
			case "30646-04.htm":
			case "30646-06.htm":
			{
				st.setCond(8, true);
				takeItems(player, WATCHER_EYE_2, 1);
				break;
			}
			case "30647-04.htm":
			{
				if (hasQuestItems(player, BROKEN_KEY))
				{
					if (getRandom(10) < 2)
					{
						htmltext = "30647-03.htm";
						playSound(player, QuestSound.ITEMSOUND_QUEST_JACKPOT);
						takeItems(player, BROKEN_KEY, 1);
						final int chance = getRandom(100);
						if (chance > 90)
						{
							rewardItems(player, BRIGANDINE_GAUNTLETS_PATTERN, 1);
							rewardItems(player, IRON_BOOTS_DESIGN, 1);
							rewardItems(player, MANTICOR_SKIN_GAITERS_PATTERN, 1);
							rewardItems(player, MITHRIL_SCALE_GAITERS_MATERIAL, 1);
							rewardItems(player, RIP_GAUNTLETS_PATTERN, 1);
						}
						else if (chance > 70)
						{
							rewardItems(player, ELVEN_NECKLACE_BEADS, 1);
							rewardItems(player, TOME_OF_BLOOD_PAGE, 1);
						}
						else if (chance > 40)
						{
							rewardItems(player, WHITE_TUNIC_PATTERN, 1);
						}
						else
						{
							rewardItems(player, IRON_BOOTS_DESIGN, 1);
						}
					}
					else
					{
						htmltext = "30647-02.htm";
						takeItems(player, BROKEN_KEY, 1);
						rewardItems(player, ADENA, getRandom(1, 1000));
					}
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
				if ((player.getPlayerClass() != PlayerClass.WARRIOR) && (player.getPlayerClass() != PlayerClass.ELVEN_KNIGHT) && (player.getPlayerClass() != PlayerClass.PALUS_KNIGHT) && (player.getPlayerClass() != PlayerClass.ORC_RAIDER) && (player.getPlayerClass() != PlayerClass.ORC_MONK))
				{
					htmltext = "30644-02.htm";
				}
				else if (player.getLevel() < 35)
				{
					htmltext = "30644-01.htm";
				}
				else
				{
					htmltext = "30644-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case KASH:
					{
						if (cond == 1)
						{
							htmltext = "30644-06.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30644-07.htm";
							st.setCond(3, true);
							takeItems(player, SCROLL_OF_SHYSLASSYS, 1);
							giveItems(player, LETTER_OF_KASH, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30644-08.htm";
						}
						else if (cond > 3)
						{
							htmltext = "30644-09.htm";
						}
						break;
					}
					case CHEST_OF_SHYSLASSYS:
					{
						htmltext = "30647-01.htm";
						break;
					}
					case MARTIEN:
					{
						if (cond == 3)
						{
							htmltext = "30645-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30645-03.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30645-04.htm";
							st.setCond(6, true);
							takeItems(player, WATCHER_EYE_1, 1);
						}
						else if (cond == 6)
						{
							htmltext = "30645-05.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30645-07.htm";
						}
						else if (cond > 7)
						{
							htmltext = "30645-06.htm";
						}
						break;
					}
					case RALDO:
					{
						if (cond == 7)
						{
							htmltext = "30646-01.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30646-06a.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30646-07.htm";
							takeItems(player, BROKEN_KEY, 1);
							giveItems(player, MARK_OF_CHALLENGER, 1);
							addExpAndSp(player, 72394, 11250);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case FILAUR:
					{
						if (cond == 8)
						{
							if (player.getLevel() >= 36)
							{
								htmltext = "30535-01.htm";
								st.setCond(9, true);
							}
							else
							{
								htmltext = "30535-03.htm";
							}
						}
						else if (cond == 9)
						{
							htmltext = "30535-02.htm";
							addRadar(player, 176560, -184969, -3729);
						}
						else if (cond == 10)
						{
							htmltext = "30535-04.htm";
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
			case SHYSLASSYS:
			{
				if (st.isCond(1))
				{
					st.setCond(2, true);
					giveItems(player, BROKEN_KEY, 1);
					giveItems(player, SCROLL_OF_SHYSLASSYS, 1);
					addSpawn(CHEST_OF_SHYSLASSYS, npc, false, 200000);
				}
				break;
			}
			case GORR:
			{
				if (st.isCond(4))
				{
					giveItems(player, WATCHER_EYE_1, 1);
					st.setCond(5, true);
				}
				break;
			}
			case BARAHAM:
			{
				if (st.isCond(6))
				{
					giveItems(player, WATCHER_EYE_2, 1);
					st.setCond(7, true);
				}
				
				addSpawn(RALDO, npc, false, 100000);
				break;
			}
			case SUCCUBUS_QUEEN:
			{
				if (st.isCond(9))
				{
					st.setCond(10, true);
				}
				
				addSpawn(RALDO, npc, false, 100000);
				break;
			}
		}
	}
}
