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
package quests.Q00228_TestOfMagus;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00228_TestOfMagus extends Quest
{
	// NPCs
	private static final int PARINA = 30391;
	private static final int EARTH_SNAKE = 30409;
	private static final int FLAME_SALAMANDER = 30411;
	private static final int WIND_SYLPH = 30412;
	private static final int WATER_UNDINE = 30413;
	private static final int CASIAN = 30612;
	private static final int RUKAL = 30629;
	
	// Monsters
	private static final int HARPY = 20145;
	private static final int MARSH_STAKATO = 20157;
	private static final int WYRM = 20176;
	private static final int MARSH_STAKATO_WORKER = 20230;
	private static final int TOAD_LORD = 20231;
	private static final int MARSH_STAKATO_SOLDIER = 20232;
	private static final int MARSH_STAKATO_DRONE = 20234;
	private static final int WINDSUS = 20553;
	private static final int ENCHANTED_MONSTEREYE = 20564;
	private static final int ENCHANTED_STONE_GOLEM = 20565;
	private static final int ENCHANTED_IRON_GOLEM = 20566;
	private static final int SINGING_FLOWER_PHANTASM = 27095;
	private static final int SINGING_FLOWER_NIGHTMARE = 27096;
	private static final int SINGING_FLOWER_DARKLING = 27097;
	private static final int GHOST_FIRE = 27098;
	
	// Items
	private static final int RUKAL_LETTER = 2841;
	private static final int PARINA_LETTER = 2842;
	private static final int LILAC_CHARM = 2843;
	private static final int GOLDEN_SEED_1 = 2844;
	private static final int GOLDEN_SEED_2 = 2845;
	private static final int GOLDEN_SEED_3 = 2846;
	private static final int SCORE_OF_ELEMENTS = 2847;
	private static final int DAZZLING_DROP = 2848;
	private static final int FLAME_CRYSTAL = 2849;
	private static final int HARPY_FEATHER = 2850;
	private static final int WYRM_WINGBONE = 2851;
	private static final int WINDSUS_MANE = 2852;
	private static final int EN_MONSTEREYE_SHELL = 2853;
	private static final int EN_STONEGOLEM_POWDER = 2854;
	private static final int EN_IRONGOLEM_SCRAP = 2855;
	private static final int TONE_OF_WATER = 2856;
	private static final int TONE_OF_FIRE = 2857;
	private static final int TONE_OF_WIND = 2858;
	private static final int TONE_OF_EARTH = 2859;
	private static final int SALAMANDER_CHARM = 2860;
	private static final int SYLPH_CHARM = 2861;
	private static final int UNDINE_CHARM = 2862;
	private static final int SERPENT_CHARM = 2863;
	
	// Rewards
	private static final int MARK_OF_MAGUS = 2840;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00228_TestOfMagus()
	{
		super(228, "Test of Magus");
		registerQuestItems(RUKAL_LETTER, PARINA_LETTER, LILAC_CHARM, GOLDEN_SEED_1, GOLDEN_SEED_2, GOLDEN_SEED_3, SCORE_OF_ELEMENTS, DAZZLING_DROP, FLAME_CRYSTAL, HARPY_FEATHER, WYRM_WINGBONE, WINDSUS_MANE, EN_MONSTEREYE_SHELL, EN_STONEGOLEM_POWDER, EN_IRONGOLEM_SCRAP, TONE_OF_WATER, TONE_OF_FIRE, TONE_OF_WIND, TONE_OF_EARTH, SALAMANDER_CHARM, SYLPH_CHARM, UNDINE_CHARM, SERPENT_CHARM);
		addStartNpc(RUKAL);
		addTalkId(PARINA, EARTH_SNAKE, FLAME_SALAMANDER, WIND_SYLPH, WATER_UNDINE, CASIAN, RUKAL);
		addKillId(HARPY, MARSH_STAKATO, WYRM, MARSH_STAKATO_WORKER, TOAD_LORD, MARSH_STAKATO_SOLDIER, MARSH_STAKATO_DRONE, WINDSUS, ENCHANTED_MONSTEREYE, ENCHANTED_STONE_GOLEM, ENCHANTED_IRON_GOLEM, SINGING_FLOWER_PHANTASM, SINGING_FLOWER_NIGHTMARE, SINGING_FLOWER_DARKLING, GHOST_FIRE);
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
			case "30629-04.htm":
			{
				st.startQuest();
				giveItems(player, RUKAL_LETTER, 1);
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30629-04a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30629-10.htm":
			{
				st.setCond(5, true);
				takeItems(player, GOLDEN_SEED_1, 1);
				takeItems(player, GOLDEN_SEED_2, 1);
				takeItems(player, GOLDEN_SEED_3, 1);
				takeItems(player, LILAC_CHARM, 1);
				giveItems(player, SCORE_OF_ELEMENTS, 1);
				break;
			}
			case "30391-02.htm":
			{
				st.setCond(2, true);
				takeItems(player, RUKAL_LETTER, 1);
				giveItems(player, PARINA_LETTER, 1);
				break;
			}
			case "30612-02.htm":
			{
				st.setCond(3, true);
				takeItems(player, PARINA_LETTER, 1);
				giveItems(player, LILAC_CHARM, 1);
				break;
			}
			case "30412-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, SYLPH_CHARM, 1);
				break;
			}
			case "30409-03.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, SERPENT_CHARM, 1);
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
				if ((player.getPlayerClass() != PlayerClass.WIZARD) && (player.getPlayerClass() != PlayerClass.ELVEN_WIZARD) && (player.getPlayerClass() != PlayerClass.DARK_WIZARD))
				{
					htmltext = "30629-01.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30629-02.htm";
				}
				else
				{
					htmltext = "30629-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case RUKAL:
					{
						if (cond == 1)
						{
							htmltext = "30629-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30629-06.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30629-07.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30629-08.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30629-11.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30629-12.htm";
							takeItems(player, SCORE_OF_ELEMENTS, 1);
							takeItems(player, TONE_OF_EARTH, 1);
							takeItems(player, TONE_OF_FIRE, 1);
							takeItems(player, TONE_OF_WATER, 1);
							takeItems(player, TONE_OF_WIND, 1);
							giveItems(player, MARK_OF_MAGUS, 1);
							addExpAndSp(player, 139039, 40000);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case PARINA:
					{
						if (cond == 1)
						{
							htmltext = "30391-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30391-03.htm";
						}
						else if ((cond == 3) || (cond == 4))
						{
							htmltext = "30391-04.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30391-05.htm";
						}
						break;
					}
					case CASIAN:
					{
						if (cond == 2)
						{
							htmltext = "30612-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30612-03.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30612-04.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30612-05.htm";
						}
						break;
					}
					case WATER_UNDINE:
					{
						if (cond == 5)
						{
							if (hasQuestItems(player, UNDINE_CHARM))
							{
								if (getQuestItemsCount(player, DAZZLING_DROP) < 20)
								{
									htmltext = "30413-02.htm";
								}
								else
								{
									htmltext = "30413-03.htm";
									takeItems(player, DAZZLING_DROP, 20);
									takeItems(player, UNDINE_CHARM, 1);
									giveItems(player, TONE_OF_WATER, 1);
									
									if (hasQuestItems(player, TONE_OF_FIRE, TONE_OF_WIND, TONE_OF_EARTH))
									{
										st.setCond(6, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
							}
							else if (!hasQuestItems(player, TONE_OF_WATER))
							{
								htmltext = "30413-01.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								giveItems(player, UNDINE_CHARM, 1);
							}
							else
							{
								htmltext = "30413-04.htm";
							}
						}
						else if (cond == 6)
						{
							htmltext = "30413-04.htm";
						}
						break;
					}
					case FLAME_SALAMANDER:
					{
						if (cond == 5)
						{
							if (hasQuestItems(player, SALAMANDER_CHARM))
							{
								if (getQuestItemsCount(player, FLAME_CRYSTAL) < 5)
								{
									htmltext = "30411-02.htm";
								}
								else
								{
									htmltext = "30411-03.htm";
									takeItems(player, FLAME_CRYSTAL, 5);
									takeItems(player, SALAMANDER_CHARM, 1);
									giveItems(player, TONE_OF_FIRE, 1);
									
									if (hasQuestItems(player, TONE_OF_WATER, TONE_OF_WIND, TONE_OF_EARTH))
									{
										st.setCond(6, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
							}
							else if (!hasQuestItems(player, TONE_OF_FIRE))
							{
								htmltext = "30411-01.htm";
								giveItems(player, SALAMANDER_CHARM, 1);
							}
							else
							{
								htmltext = "30411-04.htm";
							}
						}
						else if (cond == 6)
						{
							htmltext = "30411-04.htm";
						}
						break;
					}
					case WIND_SYLPH:
					{
						if (cond == 5)
						{
							if (hasQuestItems(player, SYLPH_CHARM))
							{
								if ((getQuestItemsCount(player, HARPY_FEATHER) + getQuestItemsCount(player, WYRM_WINGBONE) + getQuestItemsCount(player, WINDSUS_MANE)) < 40)
								{
									htmltext = "30412-03.htm";
								}
								else
								{
									htmltext = "30412-04.htm";
									takeItems(player, HARPY_FEATHER, 20);
									takeItems(player, SYLPH_CHARM, 1);
									takeItems(player, WINDSUS_MANE, 10);
									takeItems(player, WYRM_WINGBONE, 10);
									giveItems(player, TONE_OF_WIND, 1);
									
									if (hasQuestItems(player, TONE_OF_WATER, TONE_OF_FIRE, TONE_OF_EARTH))
									{
										st.setCond(6, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
							}
							else if (!hasQuestItems(player, TONE_OF_WIND))
							{
								htmltext = "30412-01.htm";
							}
							else
							{
								htmltext = "30412-05.htm";
							}
						}
						else if (cond == 6)
						{
							htmltext = "30412-05.htm";
						}
						break;
					}
					case EARTH_SNAKE:
					{
						if (cond == 5)
						{
							if (hasQuestItems(player, SERPENT_CHARM))
							{
								if ((getQuestItemsCount(player, EN_MONSTEREYE_SHELL) + getQuestItemsCount(player, EN_STONEGOLEM_POWDER) + getQuestItemsCount(player, EN_IRONGOLEM_SCRAP)) < 30)
								{
									htmltext = "30409-04.htm";
								}
								else
								{
									htmltext = "30409-05.htm";
									takeItems(player, EN_IRONGOLEM_SCRAP, 10);
									takeItems(player, EN_MONSTEREYE_SHELL, 10);
									takeItems(player, EN_STONEGOLEM_POWDER, 10);
									takeItems(player, SERPENT_CHARM, 1);
									giveItems(player, TONE_OF_EARTH, 1);
									
									if (hasQuestItems(player, TONE_OF_WATER, TONE_OF_FIRE, TONE_OF_WIND))
									{
										st.setCond(6, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
							}
							else if (!hasQuestItems(player, TONE_OF_EARTH))
							{
								htmltext = "30409-01.htm";
							}
							else
							{
								htmltext = "30409-06.htm";
							}
						}
						else if (cond == 6)
						{
							htmltext = "30409-06.htm";
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
		
		final int cond = st.getCond();
		if (cond == 3)
		{
			switch (npc.getId())
			{
				case SINGING_FLOWER_PHANTASM:
				{
					if (!hasQuestItems(player, GOLDEN_SEED_1))
					{
						npc.broadcastSay(ChatType.GENERAL, "I am a tree of nothing... a tree... that knows where to return...");
						giveItems(player, GOLDEN_SEED_1, 1);
						if (hasQuestItems(player, GOLDEN_SEED_2, GOLDEN_SEED_3))
						{
							st.setCond(4, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case SINGING_FLOWER_NIGHTMARE:
				{
					if (!hasQuestItems(player, GOLDEN_SEED_2))
					{
						npc.broadcastSay(ChatType.GENERAL, "I am a creature that shows the truth of the place deep in my heart...");
						giveItems(player, GOLDEN_SEED_2, 1);
						if (hasQuestItems(player, GOLDEN_SEED_1, GOLDEN_SEED_3))
						{
							st.setCond(4, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case SINGING_FLOWER_DARKLING:
				{
					if (!hasQuestItems(player, GOLDEN_SEED_3))
					{
						npc.broadcastSay(ChatType.GENERAL, "I am a mirror of darkness... a virtual image of darkness...");
						giveItems(player, GOLDEN_SEED_3, 1);
						if (hasQuestItems(player, GOLDEN_SEED_1, GOLDEN_SEED_2))
						{
							st.setCond(4, true);
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
		else if (cond == 5)
		{
			switch (npc.getId())
			{
				case GHOST_FIRE:
				{
					if (hasQuestItems(player, SALAMANDER_CHARM) && (getRandom(10) < 5) && (getQuestItemsCount(player, FLAME_CRYSTAL) < 5))
					{
						giveItems(player, FLAME_CRYSTAL, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case TOAD_LORD:
				case MARSH_STAKATO:
				case MARSH_STAKATO_WORKER:
				{
					if (hasQuestItems(player, UNDINE_CHARM) && (getRandom(10) < 3) && (getQuestItemsCount(player, DAZZLING_DROP) < 20))
					{
						giveItems(player, DAZZLING_DROP, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case MARSH_STAKATO_SOLDIER:
				{
					if (hasQuestItems(player, UNDINE_CHARM) && (getRandom(10) < 4) && (getQuestItemsCount(player, DAZZLING_DROP) < 20))
					{
						giveItems(player, DAZZLING_DROP, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case MARSH_STAKATO_DRONE:
				{
					if (hasQuestItems(player, UNDINE_CHARM) && (getRandom(10) < 5) && (getQuestItemsCount(player, DAZZLING_DROP) < 20))
					{
						giveItems(player, DAZZLING_DROP, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case HARPY:
				{
					if (hasQuestItems(player, SYLPH_CHARM))
					{
						if (getQuestItemsCount(player, HARPY_FEATHER) < 20)
						{
							giveItems(player, HARPY_FEATHER, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case WYRM:
				{
					if (hasQuestItems(player, SYLPH_CHARM) && (getRandom(10) < 5) && (getQuestItemsCount(player, WYRM_WINGBONE) < 10))
					{
						giveItems(player, WYRM_WINGBONE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case WINDSUS:
				{
					if (hasQuestItems(player, SYLPH_CHARM) && (getRandom(10) < 5) && (getQuestItemsCount(player, WINDSUS_MANE) < 10))
					{
						giveItems(player, WINDSUS_MANE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case ENCHANTED_MONSTEREYE:
				{
					if (hasQuestItems(player, SERPENT_CHARM) && (getQuestItemsCount(player, EN_MONSTEREYE_SHELL) < 10))
					{
						giveItems(player, EN_MONSTEREYE_SHELL, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case ENCHANTED_STONE_GOLEM:
				{
					if (hasQuestItems(player, SERPENT_CHARM) && (getQuestItemsCount(player, EN_STONEGOLEM_POWDER) < 10))
					{
						giveItems(player, EN_STONEGOLEM_POWDER, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case ENCHANTED_IRON_GOLEM:
				{
					if (hasQuestItems(player, SERPENT_CHARM) && (getQuestItemsCount(player, EN_IRONGOLEM_SCRAP) < 10))
					{
						giveItems(player, EN_IRONGOLEM_SCRAP, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
			}
		}
	}
}
