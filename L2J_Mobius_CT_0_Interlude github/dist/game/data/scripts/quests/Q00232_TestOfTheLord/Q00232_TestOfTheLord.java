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
package quests.Q00232_TestOfTheLord;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00232_TestOfTheLord extends Quest
{
	// NPCs
	private static final int SOMAK = 30510;
	private static final int MANAKIA = 30515;
	private static final int JAKAL = 30558;
	private static final int SUMARI = 30564;
	private static final int KAKAI = 30565;
	private static final int VARKEES = 30566;
	private static final int TANTUS = 30567;
	private static final int HATOS = 30568;
	private static final int TAKUNA = 30641;
	private static final int CHIANTA = 30642;
	private static final int FIRST_ORC = 30643;
	private static final int ANCESTOR_MARTANKUS = 30649;
	
	// Items
	private static final int ORDEAL_NECKLACE = 3391;
	private static final int VARKEES_CHARM = 3392;
	private static final int TANTUS_CHARM = 3393;
	private static final int HATOS_CHARM = 3394;
	private static final int TAKUNA_CHARM = 3395;
	private static final int CHIANTA_CHARM = 3396;
	private static final int MANAKIAS_ORDERS = 3397;
	private static final int BREKA_ORC_FANG = 3398;
	private static final int MANAKIAS_AMULET = 3399;
	private static final int HUGE_ORC_FANG = 3400;
	private static final int SUMARIS_LETTER = 3401;
	private static final int URUTU_BLADE = 3402;
	private static final int TIMAK_ORC_SKULL = 3403;
	private static final int SWORD_INTO_SKULL = 3404;
	private static final int NERUGA_AXE_BLADE = 3405;
	private static final int AXE_OF_CEREMONY = 3406;
	private static final int MARSH_SPIDER_FEELER = 3407;
	private static final int MARSH_SPIDER_FEET = 3408;
	private static final int HANDIWORK_SPIDER_BROOCH = 3409;
	private static final int MONSTEREYE_CORNEA = 3410;
	private static final int MONSTEREYE_WOODCARVING = 3411;
	private static final int BEAR_FANG_NECKLACE = 3412;
	private static final int MARTANKUS_CHARM = 3413;
	private static final int RAGNA_ORC_HEAD = 3414;
	private static final int RAGNA_CHIEF_NOTICE = 3415;
	private static final int BONE_ARROW = 1341;
	private static final int IMMORTAL_FLAME = 3416;
	
	// Rewards
	private static final int MARK_LORD = 3390;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	// Misc
	private static Npc _firstOrc; // Used to avoid to spawn multiple instances.
	
	public Q00232_TestOfTheLord()
	{
		super(232, "Test of the Lord");
		registerQuestItems(VARKEES_CHARM, TANTUS_CHARM, HATOS_CHARM, TAKUNA_CHARM, CHIANTA_CHARM, MANAKIAS_ORDERS, BREKA_ORC_FANG, MANAKIAS_AMULET, HUGE_ORC_FANG, SUMARIS_LETTER, URUTU_BLADE, TIMAK_ORC_SKULL, SWORD_INTO_SKULL, NERUGA_AXE_BLADE, AXE_OF_CEREMONY, MARSH_SPIDER_FEELER, MARSH_SPIDER_FEET, HANDIWORK_SPIDER_BROOCH, MONSTEREYE_CORNEA, MONSTEREYE_WOODCARVING, BEAR_FANG_NECKLACE, MARTANKUS_CHARM, RAGNA_ORC_HEAD, RAGNA_ORC_HEAD, IMMORTAL_FLAME);
		addStartNpc(KAKAI);
		addTalkId(KAKAI, CHIANTA, HATOS, SOMAK, SUMARI, TAKUNA, TANTUS, JAKAL, VARKEES, MANAKIA, ANCESTOR_MARTANKUS, FIRST_ORC);
		addKillId(20233, 20269, 20270, 20564, 20583, 20584, 20585, 20586, 20587, 20588, 20778, 20779);
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
			case "30565-05.htm":
			{
				st.startQuest();
				giveItems(player, ORDEAL_NECKLACE, 1);
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30565-05b.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30565-08.htm":
			{
				st.setCond(3, true);
				takeItems(player, SWORD_INTO_SKULL, 1);
				takeItems(player, AXE_OF_CEREMONY, 1);
				takeItems(player, MONSTEREYE_WOODCARVING, 1);
				takeItems(player, HANDIWORK_SPIDER_BROOCH, 1);
				takeItems(player, ORDEAL_NECKLACE, 1);
				takeItems(player, HUGE_ORC_FANG, 1);
				giveItems(player, BEAR_FANG_NECKLACE, 1);
				break;
			}
			case "30566-02.htm":
			{
				giveItems(player, VARKEES_CHARM, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case "30567-02.htm":
			{
				giveItems(player, TANTUS_CHARM, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case "30558-02.htm":
			{
				takeItems(player, 57, 1000);
				giveItems(player, NERUGA_AXE_BLADE, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case "30568-02.htm":
			{
				giveItems(player, HATOS_CHARM, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case "30641-02.htm":
			{
				giveItems(player, TAKUNA_CHARM, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case "30642-02.htm":
			{
				giveItems(player, CHIANTA_CHARM, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case "30643-02.htm":
			{
				st.setCond(7, true);
				startQuestTimer("f_orc_despawn", 10000, null, player, false);
				break;
			}
			case "30649-04.htm":
			{
				st.setCond(4, true);
				takeItems(player, BEAR_FANG_NECKLACE, 1);
				giveItems(player, MARTANKUS_CHARM, 1);
				break;
			}
			case "30649-07.htm":
			{
				if (_firstOrc == null)
				{
					_firstOrc = addSpawn(FIRST_ORC, 21036, -107690, -3038, 200000, false, 0);
				}
				break;
			}
			case "f_orc_despawn":
			{
				if (_firstOrc != null)
				{
					_firstOrc.deleteMe();
					_firstOrc = null;
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
				if (player.getRace() != Race.ORC)
				{
					htmltext = "30565-01.htm";
				}
				else if (player.getPlayerClass() != PlayerClass.ORC_SHAMAN)
				{
					htmltext = "30565-02.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30565-03.htm";
				}
				else
				{
					htmltext = "30565-04.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case VARKEES:
					{
						if (hasQuestItems(player, HUGE_ORC_FANG))
						{
							htmltext = "30566-05.htm";
						}
						else if (hasQuestItems(player, VARKEES_CHARM))
						{
							if (hasQuestItems(player, MANAKIAS_AMULET))
							{
								htmltext = "30566-04.htm";
								takeItems(player, VARKEES_CHARM, -1);
								takeItems(player, MANAKIAS_AMULET, -1);
								giveItems(player, HUGE_ORC_FANG, 1);
								
								if (hasQuestItems(player, SWORD_INTO_SKULL, AXE_OF_CEREMONY, MONSTEREYE_WOODCARVING, HANDIWORK_SPIDER_BROOCH, ORDEAL_NECKLACE))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30566-03.htm";
							}
						}
						else
						{
							htmltext = "30566-01.htm";
						}
						break;
					}
					case MANAKIA:
					{
						if (hasQuestItems(player, HUGE_ORC_FANG))
						{
							htmltext = "30515-05.htm";
						}
						else if (hasQuestItems(player, MANAKIAS_AMULET))
						{
							htmltext = "30515-04.htm";
						}
						else if (hasQuestItems(player, MANAKIAS_ORDERS))
						{
							if (getQuestItemsCount(player, BREKA_ORC_FANG) >= 20)
							{
								htmltext = "30515-03.htm";
								takeItems(player, MANAKIAS_ORDERS, -1);
								takeItems(player, BREKA_ORC_FANG, -1);
								giveItems(player, MANAKIAS_AMULET, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
							else
							{
								htmltext = "30515-02.htm";
							}
						}
						else
						{
							htmltext = "30515-01.htm";
							giveItems(player, MANAKIAS_ORDERS, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						break;
					}
					case TANTUS:
					{
						if (hasQuestItems(player, AXE_OF_CEREMONY))
						{
							htmltext = "30567-05.htm";
						}
						else if (hasQuestItems(player, TANTUS_CHARM))
						{
							if (getQuestItemsCount(player, BONE_ARROW) >= 1000)
							{
								htmltext = "30567-04.htm";
								takeItems(player, BONE_ARROW, 1000);
								takeItems(player, NERUGA_AXE_BLADE, 1);
								takeItems(player, TANTUS_CHARM, 1);
								giveItems(player, AXE_OF_CEREMONY, 1);
								
								if (hasQuestItems(player, SWORD_INTO_SKULL, MONSTEREYE_WOODCARVING, HANDIWORK_SPIDER_BROOCH, ORDEAL_NECKLACE, HUGE_ORC_FANG))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30567-03.htm";
							}
						}
						else
						{
							htmltext = "30567-01.htm";
						}
						break;
					}
					case JAKAL:
					{
						if (hasQuestItems(player, AXE_OF_CEREMONY))
						{
							htmltext = "30558-05.htm";
						}
						else if (hasQuestItems(player, NERUGA_AXE_BLADE))
						{
							htmltext = "30558-04.htm";
						}
						else if (hasQuestItems(player, TANTUS_CHARM))
						{
							if (getQuestItemsCount(player, 57) >= 1000)
							{
								htmltext = "30558-01.htm";
							}
							else
							{
								htmltext = "30558-03.htm";
							}
						}
						break;
					}
					case HATOS:
					{
						if (hasQuestItems(player, SWORD_INTO_SKULL))
						{
							htmltext = "30568-05.htm";
						}
						else if (hasQuestItems(player, HATOS_CHARM))
						{
							if (hasQuestItems(player, URUTU_BLADE) && (getQuestItemsCount(player, TIMAK_ORC_SKULL) >= 10))
							{
								htmltext = "30568-04.htm";
								takeItems(player, HATOS_CHARM, 1);
								takeItems(player, URUTU_BLADE, 1);
								takeItems(player, TIMAK_ORC_SKULL, -1);
								giveItems(player, SWORD_INTO_SKULL, 1);
								
								if (hasQuestItems(player, AXE_OF_CEREMONY, MONSTEREYE_WOODCARVING, HANDIWORK_SPIDER_BROOCH, ORDEAL_NECKLACE, HUGE_ORC_FANG))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30568-03.htm";
							}
						}
						else
						{
							htmltext = "30568-01.htm";
						}
						break;
					}
					case SUMARI:
					{
						if (hasQuestItems(player, URUTU_BLADE))
						{
							htmltext = "30564-03.htm";
						}
						else if (hasQuestItems(player, SUMARIS_LETTER))
						{
							htmltext = "30564-02.htm";
						}
						else if (hasQuestItems(player, HATOS_CHARM))
						{
							htmltext = "30564-01.htm";
							giveItems(player, SUMARIS_LETTER, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						break;
					}
					case SOMAK:
					{
						if (hasQuestItems(player, SWORD_INTO_SKULL))
						{
							htmltext = "30510-03.htm";
						}
						else if (hasQuestItems(player, URUTU_BLADE))
						{
							htmltext = "30510-02.htm";
						}
						else if (hasQuestItems(player, SUMARIS_LETTER))
						{
							htmltext = "30510-01.htm";
							takeItems(player, SUMARIS_LETTER, 1);
							giveItems(player, URUTU_BLADE, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						break;
					}
					case TAKUNA:
					{
						if (hasQuestItems(player, HANDIWORK_SPIDER_BROOCH))
						{
							htmltext = "30641-05.htm";
						}
						else if (hasQuestItems(player, TAKUNA_CHARM))
						{
							if ((getQuestItemsCount(player, MARSH_SPIDER_FEELER) >= 10) && (getQuestItemsCount(player, MARSH_SPIDER_FEET) >= 10))
							{
								htmltext = "30641-04.htm";
								takeItems(player, MARSH_SPIDER_FEELER, -1);
								takeItems(player, MARSH_SPIDER_FEET, -1);
								takeItems(player, TAKUNA_CHARM, 1);
								giveItems(player, HANDIWORK_SPIDER_BROOCH, 1);
								
								if (hasQuestItems(player, SWORD_INTO_SKULL, AXE_OF_CEREMONY, MONSTEREYE_WOODCARVING, ORDEAL_NECKLACE, HUGE_ORC_FANG))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30641-03.htm";
							}
						}
						else
						{
							htmltext = "30641-01.htm";
						}
						break;
					}
					case CHIANTA:
					{
						if (hasQuestItems(player, MONSTEREYE_WOODCARVING))
						{
							htmltext = "30642-05.htm";
						}
						else if (hasQuestItems(player, CHIANTA_CHARM))
						{
							if (getQuestItemsCount(player, MONSTEREYE_CORNEA) >= 20)
							{
								htmltext = "30642-04.htm";
								takeItems(player, MONSTEREYE_CORNEA, -1);
								takeItems(player, CHIANTA_CHARM, 1);
								giveItems(player, MONSTEREYE_WOODCARVING, 1);
								
								if (hasQuestItems(player, SWORD_INTO_SKULL, AXE_OF_CEREMONY, HANDIWORK_SPIDER_BROOCH, ORDEAL_NECKLACE, HUGE_ORC_FANG))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30642-03.htm";
							}
						}
						else
						{
							htmltext = "30642-01.htm";
						}
						break;
					}
					case KAKAI:
					{
						if (cond == 1)
						{
							htmltext = "30565-06.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30565-07.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30565-09.htm";
						}
						else if ((cond > 3) && (cond < 7))
						{
							htmltext = "30565-10.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30565-11.htm";
							
							takeItems(player, IMMORTAL_FLAME, 1);
							giveItems(player, MARK_LORD, 1);
							addExpAndSp(player, 92955, 16250);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case ANCESTOR_MARTANKUS:
					{
						if (cond == 3)
						{
							htmltext = "30649-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30649-05.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30649-06.htm";
							st.setCond(6, true);
							
							takeItems(player, MARTANKUS_CHARM, 1);
							takeItems(player, RAGNA_ORC_HEAD, 1);
							takeItems(player, RAGNA_CHIEF_NOTICE, 1);
							giveItems(player, IMMORTAL_FLAME, 1);
						}
						else if (cond == 6)
						{
							htmltext = "30649-07.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30649-08.htm";
						}
						break;
					}
					case FIRST_ORC:
					{
						if (cond == 6)
						{
							htmltext = "30643-01.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30643-03.htm";
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
			case 20564:
			{
				if (hasQuestItems(player, CHIANTA_CHARM) && (getQuestItemsCount(player, MONSTEREYE_CORNEA) < 20))
				{
					giveItems(player, MONSTEREYE_CORNEA, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20583:
			case 20584:
			case 20585:
			{
				if (hasQuestItems(player, HATOS_CHARM) && (getRandom(100) < 71) && (getQuestItemsCount(player, TIMAK_ORC_SKULL) < 10))
				{
					giveItems(player, TIMAK_ORC_SKULL, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20586:
			{
				if (hasQuestItems(player, HATOS_CHARM) && (getRandom(100) < 81) && (getQuestItemsCount(player, TIMAK_ORC_SKULL) < 10))
				{
					giveItems(player, TIMAK_ORC_SKULL, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20587:
			case 20588:
			{
				if (hasQuestItems(player, HATOS_CHARM) && (getQuestItemsCount(player, TIMAK_ORC_SKULL) < 10))
				{
					giveItems(player, TIMAK_ORC_SKULL, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20233:
			{
				if (hasQuestItems(player, TAKUNA_CHARM))
				{
					if (getQuestItemsCount(player, MARSH_SPIDER_FEELER) >= 10)
					{
						giveItems(player, MARSH_SPIDER_FEET, 1);
					}
					else
					{
						giveItems(player, MARSH_SPIDER_FEELER, 1);
					}
					
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20269:
			{
				if (hasQuestItems(player, MANAKIAS_ORDERS) && (getRandom(100) < 41) && (getQuestItemsCount(player, BREKA_ORC_FANG) < 20))
				{
					giveItems(player, BREKA_ORC_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20270:
			{
				if (hasQuestItems(player, MANAKIAS_ORDERS) && (getRandom(100) < 51) && (getQuestItemsCount(player, BREKA_ORC_FANG) < 20))
				{
					giveItems(player, BREKA_ORC_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20778:
			case 20779:
			{
				if (hasQuestItems(player, MARTANKUS_CHARM))
				{
					if (!hasQuestItems(player, RAGNA_CHIEF_NOTICE))
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						giveItems(player, RAGNA_CHIEF_NOTICE, 1);
					}
					else if (!hasQuestItems(player, RAGNA_ORC_HEAD))
					{
						st.setCond(5, true);
						giveItems(player, RAGNA_ORC_HEAD, 1);
					}
				}
				break;
			}
		}
	}
}
