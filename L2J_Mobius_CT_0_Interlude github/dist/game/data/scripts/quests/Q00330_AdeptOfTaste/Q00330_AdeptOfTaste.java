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
package quests.Q00330_AdeptOfTaste;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00330_AdeptOfTaste extends Quest
{
	// NPCs
	private static final int SONIA = 30062;
	private static final int GLYVKA = 30067;
	private static final int ROLLANT = 30069;
	private static final int JACOB = 30073;
	private static final int PANO = 30078;
	private static final int MIRIEN = 30461;
	private static final int JONAS = 30469;
	
	// Items
	private static final int INGREDIENT_LIST = 1420;
	private static final int SONIA_BOTANY_BOOK = 1421;
	private static final int RED_MANDRAGORA_ROOT = 1422;
	private static final int WHITE_MANDRAGORA_ROOT = 1423;
	private static final int RED_MANDRAGORA_SAP = 1424;
	private static final int WHITE_MANDRAGORA_SAP = 1425;
	private static final int JACOB_INSECT_BOOK = 1426;
	private static final int NECTAR = 1427;
	private static final int ROYAL_JELLY = 1428;
	private static final int HONEY = 1429;
	private static final int GOLDEN_HONEY = 1430;
	private static final int PANO_CONTRACT = 1431;
	private static final int HOBGOBLIN_AMULET = 1432;
	private static final int DIONIAN_POTATO = 1433;
	private static final int GLYVKA_BOTANY_BOOK = 1434;
	private static final int GREEN_MARSH_MOSS = 1435;
	private static final int BROWN_MARSH_MOSS = 1436;
	private static final int GREEN_MOSS_BUNDLE = 1437;
	private static final int BROWN_MOSS_BUNDLE = 1438;
	private static final int ROLANT_CREATURE_BOOK = 1439;
	private static final int MONSTER_EYE_BODY = 1440;
	private static final int MONSTER_EYE_MEAT = 1441;
	private static final int JONAS_STEAK_DISH_1 = 1442;
	private static final int JONAS_STEAK_DISH_2 = 1443;
	private static final int JONAS_STEAK_DISH_3 = 1444;
	private static final int JONAS_STEAK_DISH_4 = 1445;
	private static final int JONAS_STEAK_DISH_5 = 1446;
	private static final int MIRIEN_REVIEW_1 = 1447;
	private static final int MIRIEN_REVIEW_2 = 1448;
	private static final int MIRIEN_REVIEW_3 = 1449;
	private static final int MIRIEN_REVIEW_4 = 1450;
	private static final int MIRIEN_REVIEW_5 = 1451;
	
	// Rewards
	private static final int JONAS_SALAD_RECIPE = 1455;
	private static final int JONAS_SAUCE_RECIPE = 1456;
	private static final int JONAS_STEAK_RECIPE = 1457;
	
	// Drop chances
	private static final Map<Integer, int[]> CHANCES = new HashMap<>();
	static
	{
		// @formatter:off
		CHANCES.put(20204, new int[]{92, 100});
		CHANCES.put(20229, new int[]{80, 95});
		CHANCES.put(20223, new int[]{70, 77});
		CHANCES.put(20154, new int[]{70, 77});
		CHANCES.put(20155, new int[]{87, 96});
		CHANCES.put(20156, new int[]{77, 85});
		// @formatter:on
	}
	
	public Q00330_AdeptOfTaste()
	{
		super(330, "Adept of Taste");
		registerQuestItems(INGREDIENT_LIST, RED_MANDRAGORA_SAP, WHITE_MANDRAGORA_SAP, HONEY, GOLDEN_HONEY, DIONIAN_POTATO, GREEN_MOSS_BUNDLE, BROWN_MOSS_BUNDLE, MONSTER_EYE_MEAT, MIRIEN_REVIEW_1, MIRIEN_REVIEW_2, MIRIEN_REVIEW_3, MIRIEN_REVIEW_4, MIRIEN_REVIEW_5, JONAS_STEAK_DISH_1, JONAS_STEAK_DISH_2, JONAS_STEAK_DISH_3, JONAS_STEAK_DISH_4, JONAS_STEAK_DISH_5, SONIA_BOTANY_BOOK, RED_MANDRAGORA_ROOT, WHITE_MANDRAGORA_ROOT, JACOB_INSECT_BOOK, NECTAR, ROYAL_JELLY, PANO_CONTRACT, HOBGOBLIN_AMULET, GLYVKA_BOTANY_BOOK, GREEN_MARSH_MOSS, BROWN_MARSH_MOSS, ROLANT_CREATURE_BOOK, MONSTER_EYE_BODY);
		addStartNpc(JONAS); // Jonas
		addTalkId(JONAS, SONIA, GLYVKA, ROLLANT, JACOB, PANO, MIRIEN);
		addKillId(20147, 20154, 20155, 20156, 20204, 20223, 20226, 20228, 20229, 20265, 20266);
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
			case "30469-03.htm":
			{
				st.startQuest();
				giveItems(player, INGREDIENT_LIST, 1);
				break;
			}
			case "30062-05.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, SONIA_BOTANY_BOOK, 1);
				takeItems(player, RED_MANDRAGORA_ROOT, -1);
				takeItems(player, WHITE_MANDRAGORA_ROOT, -1);
				giveItems(player, RED_MANDRAGORA_SAP, 1);
				break;
			}
			case "30073-05.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, JACOB_INSECT_BOOK, 1);
				takeItems(player, NECTAR, -1);
				takeItems(player, ROYAL_JELLY, -1);
				giveItems(player, HONEY, 1);
				break;
			}
			case "30067-05.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, GLYVKA_BOTANY_BOOK, 1);
				takeItems(player, GREEN_MARSH_MOSS, -1);
				takeItems(player, BROWN_MARSH_MOSS, -1);
				giveItems(player, GREEN_MOSS_BUNDLE, 1);
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
				htmltext = (player.getLevel() < 24) ? "30469-01.htm" : "30469-02.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case JONAS:
					{
						if (hasQuestItems(player, INGREDIENT_LIST))
						{
							if (!hasAllIngredients(player))
							{
								htmltext = "30469-04.htm";
							}
							else
							{
								int dish;
								
								final int specialIngredientsNumber = getQuestItemsCount(player, WHITE_MANDRAGORA_SAP) + getQuestItemsCount(player, GOLDEN_HONEY) + getQuestItemsCount(player, BROWN_MOSS_BUNDLE);
								if (getRandomBoolean())
								{
									htmltext = "30469-05t" + Integer.toString(specialIngredientsNumber + 2) + ".htm";
									dish = 1443 + specialIngredientsNumber;
								}
								else
								{
									htmltext = "30469-05t" + Integer.toString(specialIngredientsNumber + 1) + ".htm";
									dish = 1442 + specialIngredientsNumber;
								}
								
								// Sound according dish.
								playSound(player, (dish == JONAS_STEAK_DISH_5) ? QuestSound.ITEMSOUND_QUEST_JACKPOT : QuestSound.ITEMSOUND_QUEST_ITEMGET);
								takeItems(player, INGREDIENT_LIST, 1);
								takeItems(player, RED_MANDRAGORA_SAP, 1);
								takeItems(player, WHITE_MANDRAGORA_SAP, 1);
								takeItems(player, HONEY, 1);
								takeItems(player, GOLDEN_HONEY, 1);
								takeItems(player, DIONIAN_POTATO, 1);
								takeItems(player, GREEN_MOSS_BUNDLE, 1);
								takeItems(player, BROWN_MOSS_BUNDLE, 1);
								takeItems(player, MONSTER_EYE_MEAT, 1);
								giveItems(player, dish, 1);
							}
						}
						else if (hasAtLeastOneQuestItem(player, JONAS_STEAK_DISH_1, JONAS_STEAK_DISH_2, JONAS_STEAK_DISH_3, JONAS_STEAK_DISH_4, JONAS_STEAK_DISH_5))
						{
							htmltext = "30469-06.htm";
						}
						else if (hasAtLeastOneQuestItem(player, MIRIEN_REVIEW_1, MIRIEN_REVIEW_2, MIRIEN_REVIEW_3, MIRIEN_REVIEW_4, MIRIEN_REVIEW_5))
						{
							if (hasQuestItems(player, MIRIEN_REVIEW_1))
							{
								htmltext = "30469-06t1.htm";
								takeItems(player, MIRIEN_REVIEW_1, 1);
								giveAdena(player, 7500, true);
								addExpAndSp(player, 6000, 0);
							}
							else if (hasQuestItems(player, MIRIEN_REVIEW_2))
							{
								htmltext = "30469-06t2.htm";
								takeItems(player, MIRIEN_REVIEW_2, 1);
								giveAdena(player, 9000, true);
								addExpAndSp(player, 7000, 0);
							}
							else if (hasQuestItems(player, MIRIEN_REVIEW_3))
							{
								htmltext = "30469-06t3.htm";
								takeItems(player, MIRIEN_REVIEW_3, 1);
								giveAdena(player, 5800, true);
								giveItems(player, JONAS_SALAD_RECIPE, 1);
								addExpAndSp(player, 9000, 0);
							}
							else if (hasQuestItems(player, MIRIEN_REVIEW_4))
							{
								htmltext = "30469-06t4.htm";
								takeItems(player, MIRIEN_REVIEW_4, 1);
								giveAdena(player, 6800, true);
								giveItems(player, JONAS_SAUCE_RECIPE, 1);
								addExpAndSp(player, 10500, 0);
							}
							else if (hasQuestItems(player, MIRIEN_REVIEW_5))
							{
								htmltext = "30469-06t5.htm";
								takeItems(player, MIRIEN_REVIEW_5, 1);
								giveAdena(player, 7800, true);
								giveItems(player, JONAS_STEAK_RECIPE, 1);
								addExpAndSp(player, 12000, 0);
							}
							
							st.exitQuest(true, true);
						}
						break;
					}
					case MIRIEN:
					{
						if (hasQuestItems(player, INGREDIENT_LIST))
						{
							htmltext = "30461-01.htm";
						}
						else if (hasAtLeastOneQuestItem(player, JONAS_STEAK_DISH_1, JONAS_STEAK_DISH_2, JONAS_STEAK_DISH_3, JONAS_STEAK_DISH_4, JONAS_STEAK_DISH_5))
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							if (hasQuestItems(player, JONAS_STEAK_DISH_1))
							{
								htmltext = "30461-02t1.htm";
								takeItems(player, JONAS_STEAK_DISH_1, 1);
								giveItems(player, MIRIEN_REVIEW_1, 1);
							}
							else if (hasQuestItems(player, JONAS_STEAK_DISH_2))
							{
								htmltext = "30461-02t2.htm";
								takeItems(player, JONAS_STEAK_DISH_2, 1);
								giveItems(player, MIRIEN_REVIEW_2, 1);
							}
							else if (hasQuestItems(player, JONAS_STEAK_DISH_3))
							{
								htmltext = "30461-02t3.htm";
								takeItems(player, JONAS_STEAK_DISH_3, 1);
								giveItems(player, MIRIEN_REVIEW_3, 1);
							}
							else if (hasQuestItems(player, JONAS_STEAK_DISH_4))
							{
								htmltext = "30461-02t4.htm";
								takeItems(player, JONAS_STEAK_DISH_4, 1);
								giveItems(player, MIRIEN_REVIEW_4, 1);
							}
							else if (hasQuestItems(player, JONAS_STEAK_DISH_5))
							{
								htmltext = "30461-02t5.htm";
								takeItems(player, JONAS_STEAK_DISH_5, 1);
								giveItems(player, MIRIEN_REVIEW_5, 1);
							}
						}
						else if (hasAtLeastOneQuestItem(player, MIRIEN_REVIEW_1, MIRIEN_REVIEW_2, MIRIEN_REVIEW_3, MIRIEN_REVIEW_4, MIRIEN_REVIEW_5))
						{
							htmltext = "30461-04.htm";
						}
						break;
					}
					case SONIA:
					{
						if (!hasQuestItems(player, RED_MANDRAGORA_SAP) && !hasQuestItems(player, WHITE_MANDRAGORA_SAP))
						{
							if (!hasQuestItems(player, SONIA_BOTANY_BOOK))
							{
								htmltext = "30062-01.htm";
								giveItems(player, SONIA_BOTANY_BOOK, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
							else
							{
								if ((getQuestItemsCount(player, RED_MANDRAGORA_ROOT) < 40) || (getQuestItemsCount(player, WHITE_MANDRAGORA_ROOT) < 40))
								{
									htmltext = "30062-02.htm";
								}
								else if (getQuestItemsCount(player, WHITE_MANDRAGORA_ROOT) >= 40)
								{
									htmltext = "30062-06.htm";
									takeItems(player, SONIA_BOTANY_BOOK, 1);
									takeItems(player, RED_MANDRAGORA_ROOT, -1);
									takeItems(player, WHITE_MANDRAGORA_ROOT, -1);
									giveItems(player, WHITE_MANDRAGORA_SAP, 1);
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
								else
								{
									htmltext = "30062-03.htm";
								}
							}
						}
						else
						{
							htmltext = "30062-07.htm";
						}
						break;
					}
					case JACOB:
					{
						if (!hasQuestItems(player, HONEY) && !hasQuestItems(player, GOLDEN_HONEY))
						{
							if (!hasQuestItems(player, JACOB_INSECT_BOOK))
							{
								htmltext = "30073-01.htm";
								giveItems(player, JACOB_INSECT_BOOK, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
							else
							{
								if (getQuestItemsCount(player, NECTAR) < 20)
								{
									htmltext = "30073-02.htm";
								}
								else
								{
									if (getQuestItemsCount(player, ROYAL_JELLY) < 10)
									{
										htmltext = "30073-03.htm";
									}
									else
									{
										htmltext = "30073-06.htm";
										takeItems(player, JACOB_INSECT_BOOK, 1);
										takeItems(player, NECTAR, -1);
										takeItems(player, ROYAL_JELLY, -1);
										giveItems(player, GOLDEN_HONEY, 1);
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
							}
						}
						else
						{
							htmltext = "30073-07.htm";
						}
						break;
					}
					case PANO:
					{
						if (!hasQuestItems(player, DIONIAN_POTATO))
						{
							if (!hasQuestItems(player, PANO_CONTRACT))
							{
								htmltext = "30078-01.htm";
								giveItems(player, PANO_CONTRACT, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
							else
							{
								if (getQuestItemsCount(player, HOBGOBLIN_AMULET) < 30)
								{
									htmltext = "30078-02.htm";
								}
								else
								{
									htmltext = "30078-03.htm";
									takeItems(player, PANO_CONTRACT, 1);
									takeItems(player, HOBGOBLIN_AMULET, -1);
									giveItems(player, DIONIAN_POTATO, 1);
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
						}
						else
						{
							htmltext = "30078-04.htm";
						}
						break;
					}
					case GLYVKA:
					{
						if (!hasQuestItems(player, GREEN_MOSS_BUNDLE) && !hasQuestItems(player, BROWN_MOSS_BUNDLE))
						{
							if (!hasQuestItems(player, GLYVKA_BOTANY_BOOK))
							{
								giveItems(player, GLYVKA_BOTANY_BOOK, 1);
								htmltext = "30067-01.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
							else
							{
								if ((getQuestItemsCount(player, GREEN_MARSH_MOSS) < 20) || (getQuestItemsCount(player, BROWN_MARSH_MOSS) < 20))
								{
									htmltext = "30067-02.htm";
								}
								else if (getQuestItemsCount(player, BROWN_MARSH_MOSS) >= 20)
								{
									htmltext = "30067-06.htm";
									takeItems(player, GLYVKA_BOTANY_BOOK, 1);
									takeItems(player, GREEN_MARSH_MOSS, -1);
									takeItems(player, BROWN_MARSH_MOSS, -1);
									giveItems(player, BROWN_MOSS_BUNDLE, 1);
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
								else
								{
									htmltext = "30067-03.htm";
								}
							}
						}
						else
						{
							htmltext = "30067-07.htm";
						}
						break;
					}
					case ROLLANT:
					{
						if (!hasQuestItems(player, MONSTER_EYE_MEAT))
						{
							if (!hasQuestItems(player, ROLANT_CREATURE_BOOK))
							{
								htmltext = "30069-01.htm";
								giveItems(player, ROLANT_CREATURE_BOOK, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
							else
							{
								if (getQuestItemsCount(player, MONSTER_EYE_BODY) < 30)
								{
									htmltext = "30069-02.htm";
								}
								else
								{
									htmltext = "30069-03.htm";
									takeItems(player, ROLANT_CREATURE_BOOK, 1);
									takeItems(player, MONSTER_EYE_BODY, -1);
									giveItems(player, MONSTER_EYE_MEAT, 1);
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
						}
						else
						{
							htmltext = "30069-04.htm";
						}
						break;
					}
				}
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
		
		final int npcId = npc.getId();
		switch (npcId)
		{
			case 20265:
			{
				if (hasQuestItems(player, ROLANT_CREATURE_BOOK) && (getQuestItemsCount(player, MONSTER_EYE_BODY) < 30) && (getRandom(100) < 97))
				{
					giveItems(player, MONSTER_EYE_BODY, getRandom(97) < 77 ? 2 : 3);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20266:
			{
				if (hasQuestItems(player, ROLANT_CREATURE_BOOK) && (getQuestItemsCount(player, MONSTER_EYE_BODY) < 30))
				{
					giveItems(player, MONSTER_EYE_BODY, getRandom(10) < 7 ? 1 : 2);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20226:
			{
				if (hasQuestItems(player, GLYVKA_BOTANY_BOOK))
				{
					final int itemId = getRandom(96) < 87 ? GREEN_MARSH_MOSS : BROWN_MARSH_MOSS;
					if ((getQuestItemsCount(player, itemId) < 20) && (getRandom(100) < 96))
					{
						giveItems(player, itemId, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20228:
			{
				if (hasQuestItems(player, GLYVKA_BOTANY_BOOK))
				{
					final int itemId = getRandom(10) < 9 ? GREEN_MARSH_MOSS : BROWN_MARSH_MOSS;
					if (getQuestItemsCount(player, itemId) < 20)
					{
						giveItems(player, itemId, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20147:
			{
				if (hasQuestItems(player, PANO_CONTRACT) && (getQuestItemsCount(player, HOBGOBLIN_AMULET) < 30))
				{
					giveItems(player, HOBGOBLIN_AMULET, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20204:
			case 20229:
			{
				if (hasQuestItems(player, JACOB_INSECT_BOOK))
				{
					final int random = getRandom(100);
					final int[] chances = CHANCES.get(npcId);
					if (random < chances[0])
					{
						if (getQuestItemsCount(player, NECTAR) < 20)
						{
							giveItems(player, NECTAR, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					else if ((random < chances[1]) && (getQuestItemsCount(player, ROYAL_JELLY) < 10))
					{
						giveItems(player, ROYAL_JELLY, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20223:
			case 20154:
			case 20155:
			case 20156:
			{
				if (hasQuestItems(player, SONIA_BOTANY_BOOK))
				{
					final int random = getRandom(100);
					final int[] chances = CHANCES.get(npcId);
					if (random < chances[1])
					{
						final int itemId = random < chances[0] ? RED_MANDRAGORA_ROOT : WHITE_MANDRAGORA_ROOT;
						if (getQuestItemsCount(player, itemId) < 40)
						{
							giveItems(player, itemId, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
		}
	}
	
	private boolean hasAllIngredients(Player player)
	{
		return hasQuestItems(player, DIONIAN_POTATO, MONSTER_EYE_MEAT) && hasAtLeastOneQuestItem(player, WHITE_MANDRAGORA_SAP, RED_MANDRAGORA_SAP) && hasAtLeastOneQuestItem(player, GOLDEN_HONEY, HONEY) && hasAtLeastOneQuestItem(player, BROWN_MOSS_BUNDLE, GREEN_MOSS_BUNDLE);
	}
}
