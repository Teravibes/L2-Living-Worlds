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
package quests.Q00348_AnArrogantSearch;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class Q00348_AnArrogantSearch extends Quest
{
	// NPCs
	private static final int HANELLIN = 30864;
	private static final int CLAUDIA_ATHEBALDT = 31001;
	private static final int MARTIEN = 30645;
	private static final int HARNE = 30144;
	private static final int ARK_GUARDIAN_CORPSE = 30980;
	private static final int HOLY_ARK_OF_SECRECY_1 = 30977;
	private static final int HOLY_ARK_OF_SECRECY_2 = 30978;
	private static final int HOLY_ARK_OF_SECRECY_3 = 30979;
	private static final int GUSTAV_ATHEBALDT = 30760;
	private static final int HARDIN = 30832;
	private static final int IASON_HEINE = 30969;
	
	// Monsters
	private static final int LESSER_GIANT_MAGE = 20657;
	private static final int LESSER_GIANT_ELDER = 20658;
	private static final int PLATINUM_TRIBE_SHAMAN = 20828;
	private static final int PLATINUM_TRIBE_OVERLORD = 20829;
	private static final int GUARDIAN_ANGEL = 20859;
	private static final int SEAL_ANGEL = 20860;
	
	// Quest Monsters
	private static final int ANGEL_KILLER = 27184;
	private static final int ARK_GUARDIAN_ELBEROTH = 27182;
	private static final int ARK_GUARDIAN_SHADOW_FANG = 27183;
	
	// Items
	private static final int TITAN_POWERSTONE = 4287;
	private static final int HANELLIN_FIRST_LETTER = 4288;
	private static final int HANELLIN_SECOND_LETTER = 4289;
	private static final int HANELLIN_THIRD_LETTER = 4290;
	private static final int FIRST_KEY_OF_ARK = 4291;
	private static final int SECOND_KEY_OF_ARK = 4292;
	private static final int THIRD_KEY_OF_ARK = 4293;
	private static final int BOOK_OF_SAINT = 4397;
	private static final int BLOOD_OF_SAINT = 4398;
	private static final int BOUGH_OF_SAINT = 4399;
	private static final int WHITE_FABRIC_TRIBE = 4294;
	private static final int WHITE_FABRIC_ANGELS = 5232;
	private static final int BLOODED_FABRIC = 4295;
	private static final int ANTIDOTE = 1831;
	private static final int HEALING_POTION = 1061;
	
	// NPCs instances, in order to avoid infinite instances creation speaking to chests.
	private Npc _elberoth;
	private Npc _shadowFang;
	private Npc _angelKiller;
	
	public Q00348_AnArrogantSearch()
	{
		super(348, "An Arrogant Search");
		registerQuestItems(TITAN_POWERSTONE, HANELLIN_FIRST_LETTER, HANELLIN_SECOND_LETTER, HANELLIN_THIRD_LETTER, FIRST_KEY_OF_ARK, SECOND_KEY_OF_ARK, THIRD_KEY_OF_ARK, BOOK_OF_SAINT, BLOOD_OF_SAINT, BOUGH_OF_SAINT, WHITE_FABRIC_TRIBE, WHITE_FABRIC_ANGELS);
		addStartNpc(HANELLIN);
		addTalkId(HANELLIN, CLAUDIA_ATHEBALDT, MARTIEN, HARNE, HOLY_ARK_OF_SECRECY_1, HOLY_ARK_OF_SECRECY_2, HOLY_ARK_OF_SECRECY_3, ARK_GUARDIAN_CORPSE, GUSTAV_ATHEBALDT, HARDIN, IASON_HEINE);
		addSpawnId(ARK_GUARDIAN_ELBEROTH, ARK_GUARDIAN_SHADOW_FANG, ANGEL_KILLER);
		addAttackId(ARK_GUARDIAN_ELBEROTH, ARK_GUARDIAN_SHADOW_FANG, ANGEL_KILLER, PLATINUM_TRIBE_SHAMAN, PLATINUM_TRIBE_OVERLORD);
		addKillId(LESSER_GIANT_MAGE, LESSER_GIANT_ELDER, ARK_GUARDIAN_ELBEROTH, ARK_GUARDIAN_SHADOW_FANG, ANGEL_KILLER, PLATINUM_TRIBE_SHAMAN, PLATINUM_TRIBE_OVERLORD, GUARDIAN_ANGEL, SEAL_ANGEL);
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
			case "30864-05.htm":
			{
				st.startQuest();
				st.setCond(2);
				st.set("points", "0");
				break;
			}
			case "30864-09.htm":
			{
				st.setCond(4, true);
				takeItems(player, TITAN_POWERSTONE, 1);
				break;
			}
			case "30864-17.htm":
			{
				st.setCond(5, true);
				giveItems(player, HANELLIN_FIRST_LETTER, 1);
				giveItems(player, HANELLIN_SECOND_LETTER, 1);
				giveItems(player, HANELLIN_THIRD_LETTER, 1);
				break;
			}
			case "30864-36.htm":
			{
				st.setCond(24, true);
				giveAdena(player, getRandom(1, 2) * 12000, true);
				break;
			}
			case "30864-37.htm":
			{
				st.setCond(25, true);
				break;
			}
			case "30864-51.htm":
			{
				st.setCond(26, true);
				giveItems(player, WHITE_FABRIC_ANGELS, (hasQuestItems(player, BLOODED_FABRIC)) ? 9 : 10);
				break;
			}
			case "30864-58.htm":
			{
				st.setCond(27, true);
				break;
			}
			case "30864-57.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30864-56.htm":
			{
				st.setCond(29, true);
				st.set("gustav", "0"); // st.unset doesn't work.
				st.set("hardin", "0");
				st.set("iason", "0");
				giveItems(player, WHITE_FABRIC_ANGELS, 10);
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
				if (hasQuestItems(player, BLOODED_FABRIC))
				{
					htmltext = "30864-00.htm";
				}
				else if (player.getLevel() < 60)
				{
					htmltext = "30864-01.htm";
				}
				else
				{
					htmltext = "30864-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case HANELLIN:
					{
						if (cond == 1)
						{
							htmltext = "30864-02.htm";
						}
						else if (cond == 2)
						{
							htmltext = (!hasQuestItems(player, TITAN_POWERSTONE)) ? "30864-06.htm" : "30864-07.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30864-09.htm";
						}
						else if ((cond > 4) && (cond < 21))
						{
							htmltext = (hasAtLeastOneQuestItem(player, BOOK_OF_SAINT, BLOOD_OF_SAINT, BOUGH_OF_SAINT)) ? "30864-28.htm" : "30864-24.htm";
						}
						else if (cond == 21)
						{
							htmltext = "30864-29.htm";
							st.setCond(22, true);
							takeItems(player, BOOK_OF_SAINT, 1);
							takeItems(player, BLOOD_OF_SAINT, 1);
							takeItems(player, BOUGH_OF_SAINT, 1);
						}
						else if (cond == 22)
						{
							if (hasQuestItems(player, WHITE_FABRIC_TRIBE))
							{
								htmltext = "30864-31.htm";
							}
							else if ((getQuestItemsCount(player, ANTIDOTE) < 5) || !hasQuestItems(player, HEALING_POTION))
							{
								htmltext = "30864-30.htm";
							}
							else
							{
								htmltext = "30864-31.htm";
								takeItems(player, ANTIDOTE, 5);
								takeItems(player, HEALING_POTION, 1);
								giveItems(player, WHITE_FABRIC_TRIBE, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if (cond == 24)
						{
							htmltext = "30864-38.htm";
						}
						else if (cond == 25)
						{
							if (hasQuestItems(player, WHITE_FABRIC_TRIBE))
							{
								htmltext = "30864-39.htm";
							}
							else if (hasQuestItems(player, BLOODED_FABRIC))
							{
								htmltext = "30864-49.htm";
								// Use the only fabric on Baium, drop the quest.
							}
							else
							{
								st.exitQuest(true, true);
							}
						}
						else if (cond == 26)
						{
							final int count = getQuestItemsCount(player, BLOODED_FABRIC);
							if ((count + getQuestItemsCount(player, WHITE_FABRIC_ANGELS)) < 10)
							{
								htmltext = "30864-54.htm";
								takeItems(player, BLOODED_FABRIC, -1);
								giveAdena(player, (1000 * count) + 4000, true);
								st.exitQuest(true);
							}
							else if (count < 10)
							{
								htmltext = "30864-52.htm";
							}
							else if (count >= 10)
							{
								htmltext = "30864-53.htm";
							}
						}
						else if (cond == 27)
						{
							if ((st.getInt("gustav") + st.getInt("hardin") + st.getInt("iason")) == 3)
							{
								htmltext = "30864-60.htm";
								st.setCond(28, true);
								giveAdena(player, 49000, true);
							}
							else if (hasQuestItems(player, BLOODED_FABRIC) && (st.getInt("usedonbaium") != 1))
							{
								htmltext = "30864-59.htm";
							}
							else
							{
								htmltext = "30864-61.htm";
								st.exitQuest(true, true);
							}
						}
						else if (cond == 28)
						{
							htmltext = "30864-55.htm";
						}
						else if (cond == 29)
						{
							final int count = getQuestItemsCount(player, BLOODED_FABRIC);
							if ((count + getQuestItemsCount(player, WHITE_FABRIC_ANGELS)) < 10)
							{
								htmltext = "30864-54.htm";
								takeItems(player, BLOODED_FABRIC, -1);
								giveAdena(player, 5000 * count, true);
								st.exitQuest(true, true);
							}
							else if (count < 10)
							{
								htmltext = "30864-52.htm";
							}
							else if (count >= 10)
							{
								htmltext = "30864-53.htm";
							}
						}
						break;
					}
					case GUSTAV_ATHEBALDT:
					{
						if (cond == 27)
						{
							if ((getQuestItemsCount(player, BLOODED_FABRIC) >= 3) && (st.getInt("gustav") == 0))
							{
								st.set("gustav", "1");
								htmltext = "30760-01.htm";
								takeItems(player, BLOODED_FABRIC, 3);
							}
							else if (st.getInt("gustav") == 1)
							{
								htmltext = "30760-02.htm";
							}
							else
							{
								htmltext = "30760-03.htm";
								st.set("usedonbaium", "1");
							}
						}
						break;
					}
					case HARDIN:
					{
						if (cond == 27)
						{
							if (hasQuestItems(player, BLOODED_FABRIC) && (st.getInt("hardin") == 0))
							{
								st.set("hardin", "1");
								htmltext = "30832-01.htm";
								takeItems(player, BLOODED_FABRIC, 1);
							}
							else if (st.getInt("hardin") == 1)
							{
								htmltext = "30832-02.htm";
							}
							else
							{
								htmltext = "30832-03.htm";
								st.set("usedonbaium", "1");
							}
						}
						break;
					}
					case IASON_HEINE:
					{
						if (cond == 27)
						{
							if ((getQuestItemsCount(player, BLOODED_FABRIC) >= 6) && (st.getInt("iason") == 0))
							{
								st.set("iason", "1");
								htmltext = "30969-01.htm";
								takeItems(player, BLOODED_FABRIC, 6);
							}
							else if (st.getInt("iason") == 1)
							{
								htmltext = "30969-02.htm";
							}
							else
							{
								htmltext = "30969-03.htm";
								st.set("usedonbaium", "1");
							}
						}
						break;
					}
					case HARNE:
					{
						if ((cond >= 5) && (cond <= 22))
						{
							if (!hasQuestItems(player, BLOOD_OF_SAINT))
							{
								if (hasQuestItems(player, HANELLIN_FIRST_LETTER))
								{
									htmltext = "30144-01.htm";
									st.setCond(17, true);
									takeItems(player, HANELLIN_FIRST_LETTER, 1);
									addRadar(player, -418, 44174, -3568);
								}
								else if (!hasQuestItems(player, FIRST_KEY_OF_ARK))
								{
									htmltext = "30144-03.htm";
									addRadar(player, -418, 44174, -3568);
								}
								else
								{
									htmltext = "30144-04.htm";
								}
							}
							else
							{
								htmltext = "30144-05.htm";
							}
						}
						break;
					}
					case CLAUDIA_ATHEBALDT:
					{
						if ((cond >= 5) && (cond <= 22))
						{
							if (!hasQuestItems(player, BOOK_OF_SAINT))
							{
								if (hasQuestItems(player, HANELLIN_SECOND_LETTER))
								{
									htmltext = "31001-01.htm";
									st.setCond(9, true);
									takeItems(player, HANELLIN_SECOND_LETTER, 1);
									addRadar(player, 181472, 7158, -2725);
								}
								else if (!hasQuestItems(player, SECOND_KEY_OF_ARK))
								{
									htmltext = "31001-03.htm";
									addRadar(player, 181472, 7158, -2725);
								}
								else
								{
									htmltext = "31001-04.htm";
								}
							}
							else
							{
								htmltext = "31001-05.htm";
							}
						}
						break;
					}
					case MARTIEN:
					{
						if ((cond >= 5) && (cond <= 22))
						{
							if (!hasQuestItems(player, BOUGH_OF_SAINT))
							{
								if (hasQuestItems(player, HANELLIN_THIRD_LETTER))
								{
									htmltext = "30645-01.htm";
									st.setCond(13, true);
									takeItems(player, HANELLIN_THIRD_LETTER, 1);
									addRadar(player, 50693, 158674, 376);
								}
								else if (!hasQuestItems(player, THIRD_KEY_OF_ARK))
								{
									htmltext = "30645-03.htm";
									addRadar(player, 50693, 158674, 376);
								}
								else
								{
									htmltext = "30645-04.htm";
								}
							}
							else
							{
								htmltext = "30645-05.htm";
							}
						}
						break;
					}
					case ARK_GUARDIAN_CORPSE:
					{
						if (!hasQuestItems(player, HANELLIN_FIRST_LETTER) && (cond >= 5) && (cond <= 22))
						{
							if (!hasQuestItems(player, FIRST_KEY_OF_ARK) && !hasQuestItems(player, BLOOD_OF_SAINT))
							{
								if (st.getInt("angelkiller") == 0)
								{
									htmltext = "30980-01.htm";
									if (_angelKiller == null)
									{
										_angelKiller = addSpawn(ANGEL_KILLER, npc, false, 0);
									}
									
									if (!st.isCond(18))
									{
										st.setCond(18, true);
									}
								}
								else
								{
									htmltext = "30980-02.htm";
									giveItems(player, FIRST_KEY_OF_ARK, 1);
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									st.unset("angelkiller");
								}
							}
							else
							{
								htmltext = "30980-03.htm";
							}
						}
						break;
					}
					case HOLY_ARK_OF_SECRECY_1:
					{
						if (!hasQuestItems(player, HANELLIN_FIRST_LETTER) && (cond >= 5) && (cond <= 22))
						{
							if (!hasQuestItems(player, BLOOD_OF_SAINT))
							{
								if (hasQuestItems(player, FIRST_KEY_OF_ARK))
								{
									htmltext = "30977-02.htm";
									st.setCond(20, true);
									
									takeItems(player, FIRST_KEY_OF_ARK, 1);
									giveItems(player, BLOOD_OF_SAINT, 1);
									
									if (hasQuestItems(player, BOOK_OF_SAINT, BOUGH_OF_SAINT))
									{
										st.setCond(21);
									}
								}
								else
								{
									htmltext = "30977-04.htm";
								}
							}
							else
							{
								htmltext = "30977-03.htm";
							}
						}
						break;
					}
					case HOLY_ARK_OF_SECRECY_2:
					{
						if (!hasQuestItems(player, HANELLIN_SECOND_LETTER) && (cond >= 5) && (cond <= 22))
						{
							if (!hasQuestItems(player, BOOK_OF_SAINT))
							{
								if (!hasQuestItems(player, SECOND_KEY_OF_ARK))
								{
									htmltext = "30978-01.htm";
									if (_elberoth == null)
									{
										_elberoth = addSpawn(ARK_GUARDIAN_ELBEROTH, npc, false, 0);
									}
								}
								else
								{
									htmltext = "30978-02.htm";
									st.setCond(12, true);
									
									takeItems(player, SECOND_KEY_OF_ARK, 1);
									giveItems(player, BOOK_OF_SAINT, 1);
									
									if (hasQuestItems(player, BLOOD_OF_SAINT, BOUGH_OF_SAINT))
									{
										st.setCond(21);
									}
								}
							}
							else
							{
								htmltext = "30978-03.htm";
							}
						}
						break;
					}
					case HOLY_ARK_OF_SECRECY_3:
					{
						if (!hasQuestItems(player, HANELLIN_THIRD_LETTER) && (cond >= 5) && (cond <= 22))
						{
							if (!hasQuestItems(player, BOUGH_OF_SAINT))
							{
								if (!hasQuestItems(player, THIRD_KEY_OF_ARK))
								{
									htmltext = "30979-01.htm";
									if (_shadowFang == null)
									{
										_shadowFang = addSpawn(ARK_GUARDIAN_SHADOW_FANG, npc, false, 0);
									}
								}
								else
								{
									htmltext = "30979-02.htm";
									st.setCond(16, true);
									
									takeItems(player, THIRD_KEY_OF_ARK, 1);
									giveItems(player, BOUGH_OF_SAINT, 1);
									
									if (hasQuestItems(player, BLOOD_OF_SAINT, BOOK_OF_SAINT))
									{
										st.setCond(21);
									}
								}
							}
							else
							{
								htmltext = "30979-03.htm";
							}
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
	public void onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case ARK_GUARDIAN_ELBEROTH:
			{
				npc.broadcastSay(ChatType.GENERAL, "This does not belong to you. Take your hands out!");
				break;
			}
			case ARK_GUARDIAN_SHADOW_FANG:
			{
				npc.broadcastSay(ChatType.GENERAL, "I don't believe it! Grrr!");
				break;
			}
			case ANGEL_KILLER:
			{
				npc.broadcastSay(ChatType.GENERAL, "I have the key, do you wish to steal it?");
				break;
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		final QuestState st = getQuestState(attacker, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		switch (npc.getId())
		{
			case ARK_GUARDIAN_ELBEROTH:
			{
				if (npc.isScriptValue(0))
				{
					npc.broadcastSay(ChatType.GENERAL, "...I feel very sorry, but I have taken your life.");
					npc.setScriptValue(1);
				}
				break;
			}
			case ARK_GUARDIAN_SHADOW_FANG:
			{
				if (npc.isScriptValue(0))
				{
					npc.broadcastSay(ChatType.GENERAL, "I will cover this mountain with your blood!");
					npc.setScriptValue(1);
				}
				break;
			}
			case ANGEL_KILLER:
			{
				if (npc.isScriptValue(0))
				{
					npc.broadcastSay(ChatType.GENERAL, "Haha.. Really amusing! As for the key, search the corpse!");
					npc.setScriptValue(1);
				}
				
				if ((npc.getCurrentHp() / npc.getMaxHp()) < 0.50)
				{
					npc.abortAttack();
					npc.broadcastSay(ChatType.GENERAL, "Can't get rid of you... Did you get the key from the corpse?");
					npc.decayMe();
					
					st.setCond(19, true);
					st.set("angelkiller", "1");
					
					_angelKiller = null;
				}
				break;
			}
			case PLATINUM_TRIBE_OVERLORD:
			case PLATINUM_TRIBE_SHAMAN:
			{
				final int cond = st.getCond();
				if (((cond == 24) || (cond == 25)) && hasQuestItems(attacker, WHITE_FABRIC_TRIBE))
				{
					final int points = st.getInt("points") + ((npc.getId() == PLATINUM_TRIBE_SHAMAN) ? 60 : 70);
					if (points > ((cond == 24) ? 80000 : 100000))
					{
						st.set("points", Integer.toString(0));
						
						takeItems(attacker, WHITE_FABRIC_TRIBE, 1);
						giveItems(attacker, BLOODED_FABRIC, 1);
						
						if (cond != 24)
						{
							playSound(attacker, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							st.exitQuest(true, true);
						}
					}
					else
					{
						st.set("points", Integer.toString(points));
					}
				}
				break;
			}
		}
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
		switch (npc.getId())
		{
			case LESSER_GIANT_ELDER:
			case LESSER_GIANT_MAGE:
			{
				if ((cond == 2) && (getRandom(10) < 1) && !hasQuestItems(player, TITAN_POWERSTONE))
				{
					giveItems(player, TITAN_POWERSTONE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case ARK_GUARDIAN_ELBEROTH:
			{
				if ((cond >= 5) && (cond <= 22) && !hasQuestItems(player, SECOND_KEY_OF_ARK))
				{
					st.setCond(11, true);
					giveItems(player, SECOND_KEY_OF_ARK, 1);
					npc.broadcastSay(ChatType.GENERAL, "Oh, dull-witted.. God, they...");
				}
				
				_elberoth = null;
				break;
			}
			case ARK_GUARDIAN_SHADOW_FANG:
			{
				if ((cond >= 5) && (cond <= 22) && !hasQuestItems(player, THIRD_KEY_OF_ARK))
				{
					st.setCond(15, true);
					giveItems(player, THIRD_KEY_OF_ARK, 1);
					npc.broadcastSay(ChatType.GENERAL, "You do not know.. Seven seals are.. coughs");
				}
				
				_shadowFang = null;
				break;
			}
			case PLATINUM_TRIBE_OVERLORD:
			case PLATINUM_TRIBE_SHAMAN:
			{
				if (((cond == 24) || (cond == 25)) && hasQuestItems(player, WHITE_FABRIC_TRIBE))
				{
					final int points = st.getInt("points") + ((npc.getId() == PLATINUM_TRIBE_SHAMAN) ? 600 : 700);
					if (points > ((cond == 24) ? 80000 : 100000))
					{
						st.set("points", Integer.toString(0));
						
						takeItems(player, WHITE_FABRIC_TRIBE, 1);
						giveItems(player, BLOODED_FABRIC, 1);
						
						if (cond != 24)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							st.exitQuest(true, true);
						}
					}
					else
					{
						st.set("points", Integer.toString(points));
					}
				}
				break;
			}
			case SEAL_ANGEL:
			case GUARDIAN_ANGEL:
			{
				if (((cond == 26) || (cond == 29)) && (getRandom(4) < 1) && hasQuestItems(player, WHITE_FABRIC_ANGELS))
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, WHITE_FABRIC_ANGELS, 1);
					giveItems(player, BLOODED_FABRIC, 1);
				}
				break;
			}
			case ANGEL_KILLER:
			{
				_angelKiller = null;
				break;
			}
		}
	}
}
