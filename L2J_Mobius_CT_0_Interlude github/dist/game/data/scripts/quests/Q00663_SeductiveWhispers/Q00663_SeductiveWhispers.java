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
package quests.Q00663_SeductiveWhispers;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00663_SeductiveWhispers extends Quest
{
	// NPC
	private static final int WILBERT = 30846;
	
	// Quest item
	private static final int SPIRIT_BEAD = 8766;
	
	// Rewards
	private static final int ADENA = 57;
	private static final int ENCHANT_WEAPON_A = 729;
	private static final int ENCHANT_ARMOR_A = 730;
	private static final int ENCHANT_WEAPON_B = 947;
	private static final int ENCHANT_ARMOR_B = 948;
	private static final int ENCHANT_WEAPON_C = 951;
	private static final int ENCHANT_WEAPON_D = 955;
	private static final int[] RECIPES =
	{
		2353,
		4963,
		4967,
		5000,
		5001,
		5002,
		5004,
		5005,
		5006,
		5007
	};
	private static final int[] BLADES =
	{
		2115,
		4104,
		4108,
		4114,
		4115,
		4116,
		4118,
		4119,
		4120,
		4121
	};
	
	// Text of cards
	private static final Map<Integer, String> CARDS = new HashMap<>();
	static
	{
		CARDS.put(0, "No such card");
		CARDS.put(11, "<font color=\"ff453d\"> Sun Card: 1 </font>");
		CARDS.put(12, "<font color=\"ff453d\"> Sun Card: 2 </font>");
		CARDS.put(13, "<font color=\"ff453d\"> Sun Card: 3 </font>");
		CARDS.put(14, "<font color=\"ff453d\"> Sun Card: 4 </font>");
		CARDS.put(15, "<font color=\"ff453d\"> Sun Card: 5 </font>");
		CARDS.put(21, "<font color=\"fff802\"> Moon Card: 1 </font>");
		CARDS.put(22, "<font color=\"fff802\"> Moon Card: 2 </font>");
		CARDS.put(23, "<font color=\"fff802\"> Moon Card: 3 </font>");
		CARDS.put(24, "<font color=\"fff802\"> Moon Card: 4 </font>");
		CARDS.put(25, "<font color=\"fff802\"> Moon Card: 5 </font>");
	}
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(20674, 807000); // Doom Knight
		CHANCES.put(20678, 372000); // Tortured Undead
		CHANCES.put(20954, 460000); // Hungered Corpse
		CHANCES.put(20955, 537000); // Ghost War
		CHANCES.put(20956, 540000); // Past Knight
		CHANCES.put(20957, 565000); // Nihil Invader
		CHANCES.put(20958, 425000); // Death Agent
		CHANCES.put(20959, 682000); // Dark Guard
		CHANCES.put(20960, 372000); // Bloody Ghost
		CHANCES.put(20961, 547000); // Bloody Knight
		CHANCES.put(20962, 522000); // Bloody Priest
		CHANCES.put(20963, 498000); // Bloody Lord
		CHANCES.put(20974, 1000000); // Spiteful Soul Leader
		CHANCES.put(20975, 975000); // Spiteful Soul Wizard
		CHANCES.put(20976, 825000); // Spiteful Soul Fighter
		CHANCES.put(20996, 385000); // Spiteful Ghost of Ruins
		CHANCES.put(20997, 342000); // Soldier of Grief
		CHANCES.put(20998, 377000); // Cruel Punisher
		CHANCES.put(20999, 450000); // Roving Soul
		CHANCES.put(21000, 395000); // Soul of Ruins
		CHANCES.put(21001, 535000); // Wretched Archer
		CHANCES.put(21002, 472000); // Doom Scout
		CHANCES.put(21006, 502000); // Doom Servant
		CHANCES.put(21007, 540000); // Doom Guard
		CHANCES.put(21008, 692000); // Doom Archer
		CHANCES.put(21009, 740000); // Doom Trooper
		CHANCES.put(21010, 595000); // Doom Warrior
	}
	
	public Q00663_SeductiveWhispers()
	{
		super(663, "Seductive Whispers");
		registerQuestItems(SPIRIT_BEAD);
		addStartNpc(WILBERT);
		addTalkId(WILBERT);
		addKillId(CHANCES.keySet());
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
		
		final int state = st.getInt("state");
		switch (event)
		{
			case "30846-03.htm":
			{
				st.startQuest();
				st.set("state", "1");
				break;
			}
			case "30846-09.htm":
			{
				if (((state % 10) <= 4))
				{
					if ((state / 10) < 1)
					{
						if (getQuestItemsCount(player, SPIRIT_BEAD) >= 50)
						{
							takeItems(player, SPIRIT_BEAD, 50);
							st.set("state", "5");
						}
						else
						{
							htmltext = "30846-10.htm";
						}
					}
					else
					{
						st.set("state", String.valueOf(((state / 10) * 10) + 5));
						st.set("stateEx", "0");
						htmltext = "30846-09a.htm";
					}
				}
				break;
			}
			case "30846-14.htm":
			{
				if (((state % 10) == 5) && ((state / 1000) == 0))
				{
					final int i0 = st.getInt("stateEx");
					final int i1 = i0 % 10;
					final int i2 = (i0 - i1) / 10;
					final int param1 = getRandom(2) + 1;
					final int param2 = getRandom(5) + 1;
					final int i5 = state / 10;
					final int param3 = (param1 * 10) + param2;
					if (param1 == i2)
					{
						final int i3 = param2 + i1;
						if (((i3 % 5) == 0) && (i3 != 10))
						{
							if (((state % 100) / 10) >= 7)
							{
								st.set("state", "4");
								rewardItems(player, ADENA, 2384000);
								rewardItems(player, ENCHANT_WEAPON_A, 1);
								rewardItems(player, ENCHANT_ARMOR_A, 1);
								rewardItems(player, ENCHANT_ARMOR_A, 1);
								htmltext = getHtm(player, "30846-14.htm", i0, param3, player.getName());
							}
							else
							{
								st.set("state", String.valueOf(((state / 10) * 10) + 7));
								htmltext = getHtm(player, "30846-13.htm", i0, param3, player.getName()).replace("%wincount%", String.valueOf(i5 + 1));
							}
						}
						else
						{
							st.set("state", String.valueOf(((state / 10) * 10) + 6));
							st.set("stateEx", String.valueOf(param3));
							htmltext = getHtm(player, "30846-12.htm", i0, param3, player.getName());
						}
					}
					else
					{
						if ((param2 == 5) || (i1 == 5))
						{
							if (((state % 100) / 10) >= 7)
							{
								st.set("state", "4");
								rewardItems(player, ADENA, 2384000);
								rewardItems(player, ENCHANT_WEAPON_A, 1);
								rewardItems(player, ENCHANT_ARMOR_A, 1);
								rewardItems(player, ENCHANT_ARMOR_A, 1);
								htmltext = getHtm(player, "30846-14.htm", i0, param3, player.getName());
							}
							else
							{
								st.set("state", String.valueOf(((state / 10) * 10) + 7));
								htmltext = getHtm(player, "30846-13.htm", i0, param3, player.getName()).replace("%wincount%", String.valueOf(i5 + 1));
							}
						}
						else
						{
							st.set("state", String.valueOf(((state / 10) * 10) + 6));
							st.set("stateEx", String.valueOf((param1 * 10) + param2));
							htmltext = getHtm(player, "30846-12.htm", i0, param3, player.getName());
						}
					}
				}
				break;
			}
			case "30846-19.htm":
			{
				if (((state % 10) == 6) && ((state / 1000) == 0))
				{
					final int i0 = st.getInt("stateEx");
					final int i1 = i0 % 10;
					final int i2 = (i0 - i1) / 10;
					final int param1 = getRandom(2) + 1;
					final int param2 = getRandom(5) + 1;
					final int param3 = (param1 * 10) + param2;
					if (param1 == i2)
					{
						final int i3 = param1 + i1;
						if (((i3 % 5) == 0) && (i3 != 10))
						{
							st.set("state", "1");
							st.set("stateEx", "0");
							htmltext = getHtm(player, "30846-19.htm", i0, param3, player.getName());
						}
						else
						{
							st.set("state", String.valueOf(((state / 10) * 10) + 5));
							st.set("stateEx", String.valueOf(param3));
							htmltext = getHtm(player, "30846-18.htm", i0, param3, player.getName());
						}
					}
					else
					{
						if ((param2 == 5) || (i1 == 5))
						{
							st.set("state", "1");
							htmltext = getHtm(player, "30846-19.htm", i0, param3, player.getName());
						}
						else
						{
							st.set("state", String.valueOf(((state / 10) * 10) + 5));
							st.set("stateEx", String.valueOf(param3));
							htmltext = getHtm(player, "30846-18.htm", i0, param3, player.getName());
						}
					}
				}
				break;
			}
			case "30846-20.htm":
			{
				if (((state % 10) == 7) && ((state / 1000) == 0))
				{
					st.set("state", String.valueOf((((state / 10) + 1) * 10) + 4));
					st.set("stateEx", "0");
				}
				break;
			}
			case "30846-21.htm":
			{
				if (((state % 10) == 7) && ((state / 1000) == 0))
				{
					final int round = state / 10;
					if (round == 0)
					{
						rewardItems(player, ADENA, 40000);
					}
					else if (round == 1)
					{
						rewardItems(player, ADENA, 80000);
					}
					else if (round == 2)
					{
						rewardItems(player, ADENA, 110000);
						rewardItems(player, ENCHANT_WEAPON_D, 1);
					}
					else if (round == 3)
					{
						rewardItems(player, ADENA, 199000);
						rewardItems(player, ENCHANT_WEAPON_C, 1);
					}
					else if (round == 4)
					{
						rewardItems(player, ADENA, 388000);
						rewardItems(player, RECIPES[getRandom(RECIPES.length)], 1);
					}
					else if (round == 5)
					{
						rewardItems(player, ADENA, 675000);
						rewardItems(player, BLADES[getRandom(BLADES.length)], 1);
					}
					else if (round == 6)
					{
						rewardItems(player, ADENA, 1284000);
						rewardItems(player, ENCHANT_WEAPON_B, 1);
						rewardItems(player, ENCHANT_ARMOR_B, 1);
						rewardItems(player, ENCHANT_WEAPON_B, 1);
						rewardItems(player, ENCHANT_ARMOR_B, 1);
					}
					
					st.set("state", "1");
					st.set("stateEx", "0");
				}
				break;
			}
			case "30846-22.htm":
			{
				if ((state % 10) == 1)
				{
					if (hasQuestItems(player, SPIRIT_BEAD))
					{
						st.set("state", "1005");
						takeItems(player, SPIRIT_BEAD, 1);
					}
					else
					{
						htmltext = "30846-22a.htm";
					}
				}
				break;
			}
			case "30846-25.htm":
			{
				if (state == 1005)
				{
					final int i0 = st.getInt("stateEx");
					final int i1 = i0 % 10;
					final int i2 = (i0 - i1) / 10;
					final int param1 = getRandom(2) + 1;
					final int param2 = getRandom(5) + 1;
					final int param3 = (param1 * 10) + param2;
					if (param1 == i2)
					{
						final int i3 = param2 + i1;
						if (((i3 % 5) == 0) && (i3 != 10))
						{
							st.set("state", "1");
							st.set("stateEx", "0");
							rewardItems(player, ADENA, 800);
							htmltext = getHtm(player, "30846-25.htm", i0, param3, player.getName()).replace("%card1%", String.valueOf(i1));
						}
						else
						{
							st.set("state", "1006");
							st.set("stateEx", String.valueOf(param3));
							htmltext = getHtm(player, "30846-24.htm", i0, param3, player.getName());
						}
					}
					else
					{
						if ((param2 == 5) || (i2 == 5))
						{
							st.set("state", "1");
							st.set("stateEx", "0");
							rewardItems(player, ADENA, 800);
							htmltext = getHtm(player, "30846-25.htm", i0, param3, player.getName()).replace("%card1%", String.valueOf(i1));
						}
						else
						{
							st.set("state", "1006");
							st.set("stateEx", String.valueOf(param3));
							htmltext = getHtm(player, "30846-24.htm", i0, param3, player.getName());
						}
					}
				}
				break;
			}
			case "30846-29.htm":
			{
				if (state == 1006)
				{
					final int i0 = st.getInt("stateEx");
					final int i1 = i0 % 10;
					final int i2 = (i0 - i1) / 10;
					final int param1 = getRandom(2) + 1;
					final int param2 = getRandom(5) + 1;
					final int param3 = (param1 * 10) + param2;
					if (param1 == i2)
					{
						final int i3 = param2 + i1;
						if (((i3 % 5) == 0) && (i3 != 10))
						{
							st.set("state", "1");
							st.set("stateEx", "0");
							rewardItems(player, ADENA, 800);
							htmltext = getHtm(player, "30846-29.htm", i0, param3, player.getName()).replace("%card1%", String.valueOf(i1));
						}
						else
						{
							st.set("state", "1005");
							st.set("stateEx", String.valueOf(param3));
							htmltext = getHtm(player, "30846-28.htm", i0, param3, player.getName());
						}
					}
					else
					{
						if ((param2 == 5) || (i1 == 5))
						{
							st.set("state", "1");
							st.set("stateEx", "0");
							htmltext = getHtm(player, "30846-29.htm", i0, param3, player.getName());
						}
						else
						{
							st.set("state", "1005");
							st.set("stateEx", String.valueOf(param3));
							htmltext = getHtm(player, "30846-28.htm", i0, param3, player.getName());
						}
					}
				}
				break;
			}
			case "30846-30.htm":
			{
				st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < 50) ? "30846-02.htm" : "30846-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int state = st.getInt("state");
				if (state < 4)
				{
					if (hasQuestItems(player, SPIRIT_BEAD))
					{
						htmltext = "30846-05.htm";
					}
					else
					{
						htmltext = "30846-04.htm";
					}
				}
				else if ((state % 10) == 4)
				{
					htmltext = "30846-05a.htm";
				}
				else if ((state % 10) == 5)
				{
					htmltext = "30846-11.htm";
				}
				else if ((state % 10) == 6)
				{
					htmltext = "30846-15.htm";
				}
				else if ((state % 10) == 7)
				{
					final int round = (state % 100) / 10;
					if (round >= 7)
					{
						rewardItems(player, ADENA, 2384000);
						rewardItems(player, ENCHANT_WEAPON_A, 1);
						rewardItems(player, ENCHANT_ARMOR_A, 1);
						rewardItems(player, ENCHANT_ARMOR_A, 1);
						htmltext = "30846-17.htm";
					}
					else
					{
						htmltext = getHtm(player, "30846-16.htm").replace("%wincount%", String.valueOf((state / 10) + 1));
					}
				}
				else if (state == 1005)
				{
					htmltext = "30846-23.htm";
				}
				else if (state == 1006)
				{
					htmltext = "30846-26.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getRandomPartyMemberState(player, -1, 3, npc);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			final Player partyMember = st.getPlayer();
			giveItems(partyMember, SPIRIT_BEAD, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
	
	private String getHtm(Player player, String html, int index, int param3, String name)
	{
		return getHtm(player, html).replace("%card1pic%", CARDS.get(index)).replace("%card2pic%", CARDS.get(param3)).replace("%name%", name);
	}
}
