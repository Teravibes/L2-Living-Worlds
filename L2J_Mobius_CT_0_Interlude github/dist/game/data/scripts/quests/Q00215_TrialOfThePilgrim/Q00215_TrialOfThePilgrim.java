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
package quests.Q00215_TrialOfThePilgrim;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00215_TrialOfThePilgrim extends Quest
{
	// NPCs
	private static final int SANTIAGO = 30648;
	private static final int TANAPI = 30571;
	private static final int ANCESTOR_MARTANKUS = 30649;
	private static final int GAURI_TWINKLEROCK = 30550;
	private static final int DORF = 30651;
	private static final int GERALD = 30650;
	private static final int PRIMOS = 30117;
	private static final int PETRON = 30036;
	private static final int ANDELLIA = 30362;
	private static final int URUHA = 30652;
	private static final int CASIAN = 30612;
	
	// Monsters
	private static final int LAVA_SALAMANDER = 27116;
	private static final int NAHIR = 27117;
	private static final int BLACK_WILLOW = 27118;
	
	// Items
	private static final int BOOK_OF_SAGE = 2722;
	private static final int VOUCHER_OF_TRIAL = 2723;
	private static final int SPIRIT_OF_FLAME = 2724;
	private static final int ESSENCE_OF_FLAME = 2725;
	private static final int BOOK_OF_GERALD = 2726;
	private static final int GRAY_BADGE = 2727;
	private static final int PICTURE_OF_NAHIR = 2728;
	private static final int HAIR_OF_NAHIR = 2729;
	private static final int STATUE_OF_EINHASAD = 2730;
	private static final int BOOK_OF_DARKNESS = 2731;
	private static final int DEBRIS_OF_WILLOW = 2732;
	private static final int TAG_OF_RUMOR = 2733;
	
	// Rewards
	private static final int MARK_OF_PILGRIM = 2721;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00215_TrialOfThePilgrim()
	{
		super(215, "Trial of the Pilgrim");
		registerQuestItems(BOOK_OF_SAGE, VOUCHER_OF_TRIAL, SPIRIT_OF_FLAME, ESSENCE_OF_FLAME, BOOK_OF_GERALD, GRAY_BADGE, PICTURE_OF_NAHIR, HAIR_OF_NAHIR, STATUE_OF_EINHASAD, BOOK_OF_DARKNESS, DEBRIS_OF_WILLOW, TAG_OF_RUMOR);
		addStartNpc(SANTIAGO);
		addTalkId(SANTIAGO, TANAPI, ANCESTOR_MARTANKUS, GAURI_TWINKLEROCK, DORF, GERALD, PRIMOS, PETRON, ANDELLIA, URUHA, CASIAN);
		addKillId(LAVA_SALAMANDER, NAHIR, BLACK_WILLOW);
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
			case "30648-04.htm":
			{
				st.startQuest();
				giveItems(player, VOUCHER_OF_TRIAL, 1);
				if (!player.getVariables().getBoolean("secondClassChange35", false))
				{
					htmltext = "30648-04a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_35.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange35", true);
				}
				break;
			}
			case "30649-04.htm":
			{
				st.setCond(5, true);
				takeItems(player, ESSENCE_OF_FLAME, 1);
				giveItems(player, SPIRIT_OF_FLAME, 1);
				break;
			}
			case "30650-02.htm":
			{
				if (getQuestItemsCount(player, 57) >= 100000)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, 57, 100000);
					giveItems(player, BOOK_OF_GERALD, 1);
				}
				else
				{
					htmltext = "30650-03.htm";
				}
				break;
			}
			case "30652-02.htm":
			{
				st.setCond(15, true);
				takeItems(player, DEBRIS_OF_WILLOW, 1);
				giveItems(player, BOOK_OF_DARKNESS, 1);
				break;
			}
			case "30362-04.htm":
			{
				st.setCond(16, true);
				break;
			}
			case "30362-05.htm":
			{
				st.setCond(16, true);
				takeItems(player, BOOK_OF_DARKNESS, 1);
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
				if ((player.getPlayerClass() != PlayerClass.CLERIC) && (player.getPlayerClass() != PlayerClass.ORACLE) && (player.getPlayerClass() != PlayerClass.SHILLIEN_ORACLE) && (player.getPlayerClass() != PlayerClass.ORC_SHAMAN))
				{
					htmltext = "30648-02.htm";
				}
				else if (player.getLevel() < 35)
				{
					htmltext = "30648-01.htm";
				}
				else
				{
					htmltext = "30648-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case SANTIAGO:
					{
						if (cond < 17)
						{
							htmltext = "30648-09.htm";
						}
						else if (cond == 17)
						{
							htmltext = "30648-10.htm";
							takeItems(player, BOOK_OF_SAGE, 1);
							giveItems(player, MARK_OF_PILGRIM, 1);
							addExpAndSp(player, 77382, 16000);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case TANAPI:
					{
						if (cond == 1)
						{
							htmltext = "30571-01.htm";
							st.setCond(2, true);
							takeItems(player, VOUCHER_OF_TRIAL, 1);
						}
						else if (cond < 5)
						{
							htmltext = "30571-02.htm";
						}
						else if (cond >= 5)
						{
							htmltext = "30571-03.htm";
							
							if (cond == 5)
							{
								st.setCond(6, true);
							}
						}
						break;
					}
					case ANCESTOR_MARTANKUS:
					{
						if (cond == 2)
						{
							htmltext = "30649-01.htm";
							st.setCond(3, true);
						}
						else if (cond == 3)
						{
							htmltext = "30649-02.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30649-03.htm";
						}
						break;
					}
					case GAURI_TWINKLEROCK:
					{
						if (cond == 6)
						{
							htmltext = "30550-01.htm";
							st.setCond(7, true);
							giveItems(player, TAG_OF_RUMOR, 1);
						}
						else if (cond > 6)
						{
							htmltext = "30550-02.htm";
						}
						break;
					}
					case DORF:
					{
						if (cond == 7)
						{
							htmltext = (!hasQuestItems(player, BOOK_OF_GERALD)) ? "30651-01.htm" : "30651-02.htm";
							st.setCond(8, true);
							takeItems(player, TAG_OF_RUMOR, 1);
							giveItems(player, GRAY_BADGE, 1);
						}
						else if (cond > 7)
						{
							htmltext = "30651-03.htm";
						}
						break;
					}
					case GERALD:
					{
						if ((cond == 7) && !hasQuestItems(player, BOOK_OF_GERALD))
						{
							htmltext = "30650-01.htm";
						}
						else if ((cond == 8) && hasQuestItems(player, BOOK_OF_GERALD))
						{
							htmltext = "30650-04.htm";
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							takeItems(player, BOOK_OF_GERALD, 1);
							giveAdena(player, 100000, true);
						}
						break;
					}
					case PRIMOS:
					{
						if (cond == 8)
						{
							htmltext = "30117-01.htm";
							st.setCond(9, true);
						}
						else if (cond > 8)
						{
							htmltext = "30117-02.htm";
						}
						break;
					}
					case PETRON:
					{
						if (cond == 9)
						{
							htmltext = "30036-01.htm";
							st.setCond(10, true);
							giveItems(player, PICTURE_OF_NAHIR, 1);
						}
						else if (cond == 10)
						{
							htmltext = "30036-02.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30036-03.htm";
							st.setCond(12, true);
							takeItems(player, HAIR_OF_NAHIR, 1);
							takeItems(player, PICTURE_OF_NAHIR, 1);
							giveItems(player, STATUE_OF_EINHASAD, 1);
						}
						else if (cond > 11)
						{
							htmltext = "30036-04.htm";
						}
						break;
					}
					case ANDELLIA:
					{
						if (cond == 12)
						{
							if (player.getLevel() < 36)
							{
								htmltext = "30362-01a.htm";
							}
							else
							{
								htmltext = "30362-01.htm";
								st.setCond(13, true);
							}
						}
						else if (cond == 13)
						{
							htmltext = (getRandomBoolean()) ? "30362-02.htm" : "30362-02a.htm";
						}
						else if (cond == 14)
						{
							htmltext = "30362-07.htm";
						}
						else if (cond == 15)
						{
							htmltext = "30362-03.htm";
						}
						else if (cond == 16)
						{
							htmltext = "30362-06.htm";
						}
						break;
					}
					case URUHA:
					{
						if (cond == 14)
						{
							htmltext = "30652-01.htm";
						}
						else if (cond == 15)
						{
							htmltext = "30652-03.htm";
						}
						break;
					}
					case CASIAN:
					{
						if (cond == 16)
						{
							htmltext = "30612-01.htm";
							st.setCond(17, true);
							takeItems(player, BOOK_OF_DARKNESS, 1);
							takeItems(player, GRAY_BADGE, 1);
							takeItems(player, SPIRIT_OF_FLAME, 1);
							takeItems(player, STATUE_OF_EINHASAD, 1);
							giveItems(player, BOOK_OF_SAGE, 1);
						}
						else if (cond == 17)
						{
							htmltext = "30612-02.htm";
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
			case LAVA_SALAMANDER:
			{
				if (st.isCond(3) && (getRandom(10) < 2))
				{
					giveItems(player, ESSENCE_OF_FLAME, 1);
					st.setCond(4, true);
				}
				break;
			}
			case NAHIR:
			{
				if (st.isCond(10) && (getRandom(10) < 2))
				{
					giveItems(player, HAIR_OF_NAHIR, 1);
					st.setCond(11, true);
				}
				break;
			}
			case BLACK_WILLOW:
			{
				if (st.isCond(13) && (getRandom(10) < 2))
				{
					giveItems(player, DEBRIS_OF_WILLOW, 1);
					st.setCond(14, true);
				}
				break;
			}
		}
	}
}
