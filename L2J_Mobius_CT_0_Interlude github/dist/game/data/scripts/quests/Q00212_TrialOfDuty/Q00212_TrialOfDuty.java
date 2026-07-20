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
package quests.Q00212_TrialOfDuty;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00212_TrialOfDuty extends Quest
{
	// NPCs
	private static final int HANNAVALT = 30109;
	private static final int DUSTIN = 30116;
	private static final int SIR_COLLIN = 30311;
	private static final int SIR_ARON = 30653;
	private static final int SIR_KIEL = 30654;
	private static final int SILVERSHADOW = 30655;
	private static final int SPIRIT_TALIANUS = 30656;
	
	// Items
	private static final int LETTER_OF_DUSTIN = 2634;
	private static final int KNIGHTS_TEAR = 2635;
	private static final int MIRROR_OF_ORPIC = 2636;
	private static final int TEAR_OF_CONFESSION = 2637;
	private static final int REPORT_PIECE_1 = 2638;
	private static final int REPORT_PIECE_2 = 2639;
	private static final int TEAR_OF_LOYALTY = 2640;
	private static final int MILITAS_ARTICLE = 2641;
	private static final int SAINTS_ASHES_URN = 2642;
	private static final int ATHEBALDT_SKULL = 2643;
	private static final int ATHEBALDT_RIBS = 2644;
	private static final int ATHEBALDT_SHIN = 2645;
	private static final int LETTER_OF_WINDAWOOD = 2646;
	private static final int OLD_KNIGHT_SWORD = 3027;
	
	// Rewards
	private static final int MARK_OF_DUTY = 2633;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00212_TrialOfDuty()
	{
		super(212, "Trial of Duty");
		registerQuestItems(LETTER_OF_DUSTIN, KNIGHTS_TEAR, MIRROR_OF_ORPIC, TEAR_OF_CONFESSION, REPORT_PIECE_1, REPORT_PIECE_2, TEAR_OF_LOYALTY, MILITAS_ARTICLE, SAINTS_ASHES_URN, ATHEBALDT_SKULL, ATHEBALDT_RIBS, ATHEBALDT_SHIN, LETTER_OF_WINDAWOOD, OLD_KNIGHT_SWORD);
		addStartNpc(HANNAVALT);
		addTalkId(HANNAVALT, DUSTIN, SIR_COLLIN, SIR_ARON, SIR_KIEL, SILVERSHADOW, SPIRIT_TALIANUS);
		addKillId(20144, 20190, 20191, 20200, 20201, 20270, 27119, 20577, 20578, 20579, 20580, 20581, 20582);
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
		
		if (event.equals("30109-04.htm"))
		{
			st.startQuest();
			if (!player.getVariables().getBoolean("secondClassChange35", false))
			{
				htmltext = "30109-04a.htm";
				giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_35.get(player.getPlayerClass().getId()));
				player.getVariables().set("secondClassChange35", true);
			}
		}
		else if (event.equals("30116-05.htm"))
		{
			st.setCond(14, true);
			takeItems(player, TEAR_OF_LOYALTY, 1);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if ((player.getPlayerClass() != PlayerClass.KNIGHT) && (player.getPlayerClass() != PlayerClass.ELVEN_KNIGHT) && (player.getPlayerClass() != PlayerClass.PALUS_KNIGHT))
				{
					htmltext = "30109-02.htm";
				}
				else if (player.getLevel() < 35)
				{
					htmltext = "30109-01.htm";
				}
				else
				{
					htmltext = "30109-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case HANNAVALT:
					{
						if (cond == 18)
						{
							htmltext = "30109-05.htm";
							takeItems(player, LETTER_OF_DUSTIN, 1);
							giveItems(player, MARK_OF_DUTY, 1);
							addExpAndSp(player, 79832, 3750);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						else
						{
							htmltext = "30109-04a.htm";
						}
						break;
					}
					case SIR_ARON:
					{
						if (cond == 1)
						{
							htmltext = "30653-01.htm";
							st.setCond(2, true);
							giveItems(player, OLD_KNIGHT_SWORD, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30653-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30653-03.htm";
							st.setCond(4, true);
							takeItems(player, KNIGHTS_TEAR, 1);
							takeItems(player, OLD_KNIGHT_SWORD, 1);
						}
						else if (cond > 3)
						{
							htmltext = "30653-04.htm";
						}
						break;
					}
					case SIR_KIEL:
					{
						if (cond == 4)
						{
							htmltext = "30654-01.htm";
							st.setCond(5, true);
						}
						else if (cond == 5)
						{
							htmltext = "30654-02.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30654-03.htm";
							st.setCond(7, true);
							giveItems(player, MIRROR_OF_ORPIC, 1);
						}
						else if (cond == 7)
						{
							htmltext = "30654-04.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30654-05.htm";
							st.setCond(10, true);
							takeItems(player, TEAR_OF_CONFESSION, 1);
						}
						else if (cond > 9)
						{
							htmltext = "30654-06.htm";
						}
						break;
					}
					case SPIRIT_TALIANUS:
					{
						if (cond == 8)
						{
							htmltext = "30656-01.htm";
							st.setCond(9, true);
							takeItems(player, MIRROR_OF_ORPIC, 1);
							takeItems(player, REPORT_PIECE_2, 1);
							giveItems(player, TEAR_OF_CONFESSION, 1);
							
							// Despawn the spirit.
							npc.deleteMe();
						}
						break;
					}
					case SILVERSHADOW:
					{
						if (cond == 10)
						{
							if (player.getLevel() < 35)
							{
								htmltext = "30655-01.htm";
							}
							else
							{
								htmltext = "30655-02.htm";
								st.setCond(11, true);
							}
						}
						else if (cond == 11)
						{
							htmltext = "30655-03.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30655-04.htm";
							st.setCond(13, true);
							takeItems(player, MILITAS_ARTICLE, -1);
							giveItems(player, TEAR_OF_LOYALTY, 1);
						}
						else if (cond == 13)
						{
							htmltext = "30655-05.htm";
						}
						break;
					}
					case DUSTIN:
					{
						if (cond == 13)
						{
							htmltext = "30116-01.htm";
						}
						else if (cond == 14)
						{
							htmltext = "30116-06.htm";
						}
						else if (cond == 15)
						{
							htmltext = "30116-07.htm";
							st.setCond(16, true);
							takeItems(player, ATHEBALDT_SKULL, 1);
							takeItems(player, ATHEBALDT_RIBS, 1);
							takeItems(player, ATHEBALDT_SHIN, 1);
							giveItems(player, SAINTS_ASHES_URN, 1);
						}
						else if (cond == 16)
						{
							htmltext = "30116-09.htm";
						}
						else if (cond == 17)
						{
							htmltext = "30116-08.htm";
							st.setCond(18, true);
							takeItems(player, LETTER_OF_WINDAWOOD, 1);
							giveItems(player, LETTER_OF_DUSTIN, 1);
						}
						else if (cond == 18)
						{
							htmltext = "30116-10.htm";
						}
						break;
					}
					case SIR_COLLIN:
					{
						if (cond == 16)
						{
							htmltext = "30311-01.htm";
							st.setCond(17, true);
							takeItems(player, SAINTS_ASHES_URN, 1);
							giveItems(player, LETTER_OF_WINDAWOOD, 1);
						}
						else if (cond > 16)
						{
							htmltext = "30311-02.htm";
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
			case 20190:
			case 20191:
			{
				if (st.isCond(2) && (getRandom(10) < 1))
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_BEFORE_BATTLE);
					addSpawn(27119, npc, false, 120000);
				}
				break;
			}
			case 27119:
			{
				if (st.isCond(2) && (player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND) == OLD_KNIGHT_SWORD))
				{
					st.setCond(3, true);
					giveItems(player, KNIGHTS_TEAR, 1);
				}
				break;
			}
			case 20201:
			case 20200:
			{
				if (st.isCond(5))
				{
					giveItems(player, REPORT_PIECE_1, 1);
					if (getQuestItemsCount(player, REPORT_PIECE_1) >= 10)
					{
						st.setCond(6);
						takeItems(player, REPORT_PIECE_1, -1);
						giveItems(player, REPORT_PIECE_2, 1);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20144:
			{
				if ((st.isCond(7) || st.isCond(8)) && (getRandom(100) < 33))
				{
					if (st.isCond(7))
					{
						st.setCond(8, true);
					}
					
					addSpawn(30656, npc, false, 300000);
				}
				break;
			}
			case 20577:
			case 20578:
			case 20579:
			case 20580:
			case 20581:
			case 20582:
			{
				if (st.isCond(11))
				{
					giveItems(player, MILITAS_ARTICLE, 1);
					if (getQuestItemsCount(player, MILITAS_ARTICLE) >= 20)
					{
						st.setCond(12, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20270:
			{
				if (st.isCond(14) && getRandomBoolean())
				{
					if (!hasQuestItems(player, ATHEBALDT_SKULL))
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, ATHEBALDT_SKULL, 1);
					}
					else if (!hasQuestItems(player, ATHEBALDT_RIBS))
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, ATHEBALDT_RIBS, 1);
					}
					else if (!hasQuestItems(player, ATHEBALDT_SHIN))
					{
						st.setCond(15, true);
						giveItems(player, ATHEBALDT_SHIN, 1);
					}
				}
				break;
			}
		}
	}
}
