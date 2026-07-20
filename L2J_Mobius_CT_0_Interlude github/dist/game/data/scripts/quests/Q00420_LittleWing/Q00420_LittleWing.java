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
package quests.Q00420_LittleWing;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class Q00420_LittleWing extends Quest
{
	// NPCs
	private static final int MARIA = 30608;
	private static final int CRONOS = 30610;
	private static final int BYRON = 30711;
	private static final int MIMYU = 30747;
	private static final int EXARION = 30748;
	private static final int ZWOV = 30749;
	private static final int KALIBRAN = 30750;
	private static final int SUZET = 30751;
	private static final int SHAMHAI = 30752;
	private static final int COOPER = 30829;
	
	// Items
	private static final int FAIRY_DUST = 3499;
	private static final int FAIRY_STONE = 3816;
	private static final int DELUXE_FAIRY_STONE = 3817;
	private static final int FAIRY_STONE_LIST = 3818;
	private static final int DELUXE_FAIRY_STONE_LIST = 3819;
	private static final int TOAD_LORD_BACK_SKIN = 3820;
	private static final int JUICE_OF_MONKSHOOD = 3821;
	private static final int SCALE_OF_DRAKE_EXARION = 3822;
	private static final int EGG_OF_DRAKE_EXARION = 3823;
	private static final int SCALE_OF_DRAKE_ZWOV = 3824;
	private static final int EGG_OF_DRAKE_ZWOV = 3825;
	private static final int SCALE_OF_DRAKE_KALIBRAN = 3826;
	private static final int EGG_OF_DRAKE_KALIBRAN = 3827;
	private static final int SCALE_OF_WYVERN_SUZET = 3828;
	private static final int EGG_OF_WYVERN_SUZET = 3829;
	private static final int SCALE_OF_WYVERN_SHAMHAI = 3830;
	private static final int EGG_OF_WYVERN_SHAMHAI = 3831;
	
	// Needed items
	private static final int COAL = 1870;
	private static final int CHARCOAL = 1871;
	private static final int SILVER_NUGGET = 1873;
	private static final int STONE_OF_PURITY = 1875;
	private static final int GEMSTONE_D = 2130;
	private static final int GEMSTONE_C = 2131;
	
	// Rewards
	private static final int DRAGONFLUTE_OF_WIND = 3500;
	private static final int DRAGONFLUTE_OF_STAR = 3501;
	private static final int DRAGONFLUTE_OF_TWILIGHT = 3502;
	private static final int HATCHLING_SOFT_LEATHER = 3912;
	private static final int FOOD_FOR_HATCHLING = 4038;
	
	// Spawn Points
	private static final Location[] LOCATIONS =
	{
		new Location(109816, 40854, -4640, 0),
		new Location(108940, 41615, -4643, 0),
		new Location(110395, 41625, -4642, 0)
	};
	
	// Misc
	private static int _counter = 0;
	
	public Q00420_LittleWing()
	{
		super(420, "Little Wing");
		registerQuestItems(FAIRY_STONE, DELUXE_FAIRY_STONE, FAIRY_STONE_LIST, DELUXE_FAIRY_STONE_LIST, TOAD_LORD_BACK_SKIN, JUICE_OF_MONKSHOOD, SCALE_OF_DRAKE_EXARION, EGG_OF_DRAKE_EXARION, SCALE_OF_DRAKE_ZWOV, EGG_OF_DRAKE_ZWOV, SCALE_OF_DRAKE_KALIBRAN, EGG_OF_DRAKE_KALIBRAN, SCALE_OF_WYVERN_SUZET, EGG_OF_WYVERN_SUZET, SCALE_OF_WYVERN_SHAMHAI, EGG_OF_WYVERN_SHAMHAI);
		addStartNpc(COOPER, MIMYU);
		addTalkId(MARIA, CRONOS, BYRON, MIMYU, EXARION, ZWOV, KALIBRAN, SUZET, SHAMHAI, COOPER);
		addKillId(20202, 20231, 20233, 20270, 20551, 20580, 20589, 20590, 20591, 20592, 20593, 20594, 20595, 20596, 20597, 20598, 20599);
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
			case "30829-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30610-05.htm":
			{
				st.setCond(2, true);
				giveItems(player, FAIRY_STONE_LIST, 1);
				break;
			}
			case "30610-06.htm":
			{
				st.setCond(2, true);
				giveItems(player, DELUXE_FAIRY_STONE_LIST, 1);
				break;
			}
			case "30610-12.htm":
			{
				st.setCond(2, true);
				st.set("deluxestone", "1");
				giveItems(player, FAIRY_STONE_LIST, 1);
				break;
			}
			case "30610-13.htm":
			{
				st.setCond(2, true);
				st.set("deluxestone", "1");
				giveItems(player, DELUXE_FAIRY_STONE_LIST, 1);
				break;
			}
			case "30608-03.htm":
			{
				if (!checkItems(player, false))
				{
					htmltext = "30608-01.htm"; // Avoid to continue while trade or drop mats before clicking bypass
				}
				else
				{
					takeItems(player, COAL, 10);
					takeItems(player, CHARCOAL, 10);
					takeItems(player, GEMSTONE_D, 1);
					takeItems(player, SILVER_NUGGET, 3);
					takeItems(player, TOAD_LORD_BACK_SKIN, -1);
					takeItems(player, FAIRY_STONE_LIST, 1);
					giveItems(player, FAIRY_STONE, 1);
				}
				break;
			}
			case "30608-05.htm":
			{
				if (!checkItems(player, true))
				{
					htmltext = "30608-01.htm"; // Avoid to continue while trade or drop mats before clicking bypass
				}
				else
				{
					takeItems(player, COAL, 10);
					takeItems(player, CHARCOAL, 10);
					takeItems(player, GEMSTONE_C, 1);
					takeItems(player, STONE_OF_PURITY, 1);
					takeItems(player, SILVER_NUGGET, 5);
					takeItems(player, TOAD_LORD_BACK_SKIN, -1);
					takeItems(player, DELUXE_FAIRY_STONE_LIST, 1);
					giveItems(player, DELUXE_FAIRY_STONE, 1);
				}
				break;
			}
			case "30711-03.htm":
			{
				st.setCond(4, true);
				if (hasQuestItems(player, DELUXE_FAIRY_STONE))
				{
					htmltext = "30711-04.htm";
				}
				break;
			}
			case "30747-02.htm":
			{
				st.set("mimyu", "1");
				takeItems(player, FAIRY_STONE, 1);
				break;
			}
			case "30747-04.htm":
			{
				st.set("mimyu", "1");
				takeItems(player, DELUXE_FAIRY_STONE, 1);
				giveItems(player, FAIRY_DUST, 1);
				break;
			}
			case "30747-07.htm":
			{
				st.setCond(5, true);
				giveItems(player, JUICE_OF_MONKSHOOD, 1);
				break;
			}
			case "30747-12.htm":
			{
				if (!hasQuestItems(player, FAIRY_DUST))
				{
					htmltext = "30747-15.htm";
					giveRandomPet(player, false);
					st.exitQuest(true, true);
				}
				break;
			}
			case "30747-13.htm":
			{
				giveRandomPet(player, hasQuestItems(player, FAIRY_DUST));
				st.exitQuest(true, true);
				break;
			}
			case "30747-14.htm":
			{
				if (hasQuestItems(player, FAIRY_DUST))
				{
					takeItems(player, FAIRY_DUST, 1);
					giveRandomPet(player, true);
					if (getRandom(20) == 1)
					{
						giveItems(player, HATCHLING_SOFT_LEATHER, 1);
					}
					else
					{
						htmltext = "30747-14t.htm";
						giveItems(player, FOOD_FOR_HATCHLING, 20);
					}
					
					st.exitQuest(true, true);
				}
				else
				{
					htmltext = "30747-13.htm";
				}
				break;
			}
			case "30748-02.htm":
			{
				st.setCond(6, true);
				takeItems(player, JUICE_OF_MONKSHOOD, 1);
				giveItems(player, SCALE_OF_DRAKE_EXARION, 1);
				break;
			}
			case "30749-02.htm":
			{
				st.setCond(6, true);
				takeItems(player, JUICE_OF_MONKSHOOD, 1);
				giveItems(player, SCALE_OF_DRAKE_ZWOV, 1);
				break;
			}
			case "30750-02.htm":
			{
				st.setCond(6, true);
				takeItems(player, JUICE_OF_MONKSHOOD, 1);
				giveItems(player, SCALE_OF_DRAKE_KALIBRAN, 1);
				break;
			}
			case "30750-05.htm":
			{
				st.setCond(7, true);
				takeItems(player, EGG_OF_DRAKE_KALIBRAN, 19);
				takeItems(player, SCALE_OF_DRAKE_KALIBRAN, 1);
				break;
			}
			case "30751-03.htm":
			{
				st.setCond(6, true);
				takeItems(player, JUICE_OF_MONKSHOOD, 1);
				giveItems(player, SCALE_OF_WYVERN_SUZET, 1);
				break;
			}
			case "30752-02.htm":
			{
				st.setCond(6, true);
				takeItems(player, JUICE_OF_MONKSHOOD, 1);
				giveItems(player, SCALE_OF_WYVERN_SHAMHAI, 1);
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
				switch (npc.getId())
				{
					case COOPER:
					{
						htmltext = (player.getLevel() >= 35) ? "30829-01.htm" : "30829-03.htm";
						break;
					}
					case MIMYU:
					{
						_counter += 1;
						final Location loc = LOCATIONS[_counter % 3];
						npc.teleToLocation(loc.getX(), loc.getY(), loc.getZ());
						return null;
					}
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case COOPER:
					{
						htmltext = "30829-04.htm";
						break;
					}
					case CRONOS:
					{
						if (cond == 1)
						{
							htmltext = "30610-01.htm";
						}
						else if (st.getInt("deluxestone") == 2)
						{
							htmltext = "30610-10.htm";
						}
						else if (cond == 2)
						{
							if (hasAtLeastOneQuestItem(player, FAIRY_STONE, DELUXE_FAIRY_STONE))
							{
								if (st.getInt("deluxestone") == 1)
								{
									htmltext = "30610-14.htm";
								}
								else
								{
									htmltext = "30610-08.htm";
									st.setCond(3, true);
								}
							}
							else
							{
								htmltext = "30610-07.htm";
							}
						}
						else if (cond == 3)
						{
							htmltext = "30610-09.htm";
						}
						else if ((cond == 4) && hasAtLeastOneQuestItem(player, FAIRY_STONE, DELUXE_FAIRY_STONE))
						{
							htmltext = "30610-11.htm";
						}
						break;
					}
					case MARIA:
					{
						if (hasAtLeastOneQuestItem(player, FAIRY_STONE, DELUXE_FAIRY_STONE))
						{
							htmltext = "30608-06.htm";
						}
						else if (cond == 2)
						{
							if (hasQuestItems(player, FAIRY_STONE_LIST))
							{
								htmltext = checkItems(player, false) ? "30608-02.htm" : "30608-01.htm";
							}
							else if (hasQuestItems(player, DELUXE_FAIRY_STONE_LIST))
							{
								htmltext = checkItems(player, true) ? "30608-04.htm" : "30608-01.htm";
							}
						}
						break;
					}
					case BYRON:
					{
						final int deluxestone = st.getInt("deluxestone");
						if (deluxestone == 1)
						{
							if (hasQuestItems(player, FAIRY_STONE))
							{
								htmltext = "30711-05.htm";
								st.setCond(4, true);
								st.unset("deluxestone");
							}
							else if (hasQuestItems(player, DELUXE_FAIRY_STONE))
							{
								htmltext = "30711-06.htm";
								st.setCond(4, true);
								st.unset("deluxestone");
							}
							else
							{
								htmltext = "30711-10.htm";
							}
						}
						else if (deluxestone == 2)
						{
							htmltext = "30711-09.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30711-01.htm";
						}
						else if (cond == 4)
						{
							if (hasQuestItems(player, FAIRY_STONE))
							{
								htmltext = "30711-07.htm";
							}
							else if (hasQuestItems(player, DELUXE_FAIRY_STONE))
							{
								htmltext = "30711-08.htm";
							}
						}
						break;
					}
					case MIMYU:
					{
						if (cond == 4)
						{
							if (st.getInt("mimyu") == 1)
							{
								htmltext = "30747-06.htm";
							}
							else if (hasQuestItems(player, FAIRY_STONE))
							{
								htmltext = "30747-01.htm";
							}
							else if (hasQuestItems(player, DELUXE_FAIRY_STONE))
							{
								htmltext = "30747-03.htm";
							}
						}
						else if (cond == 5)
						{
							htmltext = "30747-08.htm";
						}
						else if (cond == 6)
						{
							final int eggs = getQuestItemsCount(player, EGG_OF_DRAKE_EXARION) + getQuestItemsCount(player, EGG_OF_DRAKE_ZWOV) + getQuestItemsCount(player, EGG_OF_DRAKE_KALIBRAN) + getQuestItemsCount(player, EGG_OF_WYVERN_SUZET) + getQuestItemsCount(player, EGG_OF_WYVERN_SHAMHAI);
							if (eggs < 20)
							{
								htmltext = "30747-09.htm";
							}
							else
							{
								htmltext = "30747-10.htm";
							}
						}
						else if (cond == 7)
						{
							htmltext = "30747-11.htm";
						}
						else
						{
							_counter += 1;
							final Location loc = LOCATIONS[_counter % 3];
							npc.teleToLocation(loc.getX(), loc.getY(), loc.getZ());
							return null;
						}
						break;
					}
					case EXARION:
					{
						if (cond == 5)
						{
							htmltext = "30748-01.htm";
						}
						else if (cond == 6)
						{
							if (getQuestItemsCount(player, EGG_OF_DRAKE_EXARION) < 20)
							{
								htmltext = "30748-03.htm";
							}
							else
							{
								htmltext = "30748-04.htm";
								st.setCond(7, true);
								takeItems(player, EGG_OF_DRAKE_EXARION, 19);
								takeItems(player, SCALE_OF_DRAKE_EXARION, 1);
							}
						}
						else if (cond == 7)
						{
							htmltext = "30748-05.htm";
						}
						break;
					}
					case ZWOV:
					{
						if (cond == 5)
						{
							htmltext = "30749-01.htm";
						}
						else if (cond == 6)
						{
							if (getQuestItemsCount(player, EGG_OF_DRAKE_ZWOV) < 20)
							{
								htmltext = "30749-03.htm";
							}
							else
							{
								htmltext = "30749-04.htm";
								st.setCond(7, true);
								takeItems(player, EGG_OF_DRAKE_ZWOV, 19);
								takeItems(player, SCALE_OF_DRAKE_ZWOV, 1);
							}
						}
						else if (cond == 7)
						{
							htmltext = "30749-05.htm";
						}
						break;
					}
					case KALIBRAN:
					{
						if (cond == 5)
						{
							htmltext = "30750-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = (getQuestItemsCount(player, EGG_OF_DRAKE_KALIBRAN) < 20) ? "30750-03.htm" : "30750-04.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30750-06.htm";
						}
						break;
					}
					case SUZET:
					{
						if (cond == 5)
						{
							htmltext = "30751-01.htm";
						}
						else if (cond == 6)
						{
							if (getQuestItemsCount(player, EGG_OF_WYVERN_SUZET) < 20)
							{
								htmltext = "30751-04.htm";
							}
							else
							{
								htmltext = "30751-05.htm";
								st.setCond(7, true);
								takeItems(player, EGG_OF_WYVERN_SUZET, 19);
								takeItems(player, SCALE_OF_WYVERN_SUZET, 1);
							}
						}
						else if (cond == 7)
						{
							htmltext = "30751-06.htm";
						}
						break;
					}
					case SHAMHAI:
					{
						if (cond == 5)
						{
							htmltext = "30752-01.htm";
						}
						else if (cond == 6)
						{
							if (getQuestItemsCount(player, EGG_OF_WYVERN_SHAMHAI) < 20)
							{
								htmltext = "30752-03.htm";
							}
							else
							{
								htmltext = "30752-04.htm";
								st.setCond(7, true);
								takeItems(player, EGG_OF_WYVERN_SHAMHAI, 19);
								takeItems(player, SCALE_OF_WYVERN_SHAMHAI, 1);
							}
						}
						else if (cond == 7)
						{
							htmltext = "30752-05.htm";
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
		
		switch (npc.getId())
		{
			case 20231:
			{
				if (hasQuestItems(player, FAIRY_STONE_LIST))
				{
					if ((getQuestItemsCount(player, TOAD_LORD_BACK_SKIN) < 10) && (getRandom(10) < 3))
					{
						giveItems(player, TOAD_LORD_BACK_SKIN, 1);
						if (getQuestItemsCount(player, TOAD_LORD_BACK_SKIN) < 10)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						}
					}
				}
				else if (hasQuestItems(player, DELUXE_FAIRY_STONE_LIST) && (getQuestItemsCount(player, TOAD_LORD_BACK_SKIN) < 20) && (getRandom(10) < 3))
				{
					giveItems(player, TOAD_LORD_BACK_SKIN, 1);
					if (getQuestItemsCount(player, TOAD_LORD_BACK_SKIN) < 20)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
			case 20580:
			{
				if (hasQuestItems(player, SCALE_OF_DRAKE_EXARION) && (getQuestItemsCount(player, EGG_OF_DRAKE_EXARION) < 20))
				{
					if (getRandom(10) < 5)
					{
						giveItems(player, EGG_OF_DRAKE_EXARION, 1);
						if (getQuestItemsCount(player, EGG_OF_DRAKE_EXARION) < 20)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						}
					}
					else
					{
						npc.broadcastSay(ChatType.GENERAL, "If the eggs get taken, we're dead!");
					}
				}
				break;
			}
			case 20233:
			{
				if (hasQuestItems(player, SCALE_OF_DRAKE_ZWOV) && (getQuestItemsCount(player, EGG_OF_DRAKE_ZWOV) < 20) && (getRandom(10) < 5))
				{
					giveItems(player, EGG_OF_DRAKE_ZWOV, 1);
					if (getQuestItemsCount(player, EGG_OF_DRAKE_ZWOV) < 20)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
			case 20551:
			{
				if (hasQuestItems(player, SCALE_OF_DRAKE_KALIBRAN) && (getQuestItemsCount(player, EGG_OF_DRAKE_KALIBRAN) < 20))
				{
					if (getRandom(10) < 5)
					{
						giveItems(player, EGG_OF_DRAKE_KALIBRAN, 1);
						if (getQuestItemsCount(player, EGG_OF_DRAKE_KALIBRAN) < 20)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						}
					}
					else
					{
						npc.broadcastSay(ChatType.GENERAL, "Hey! Everybody watch the eggs!");
					}
				}
				break;
			}
			case 20270:
			{
				if (hasQuestItems(player, SCALE_OF_WYVERN_SUZET) && (getQuestItemsCount(player, EGG_OF_WYVERN_SUZET) < 20))
				{
					if (getRandom(10) < 5)
					{
						giveItems(player, EGG_OF_WYVERN_SUZET, 1);
						if (getQuestItemsCount(player, EGG_OF_WYVERN_SUZET) < 20)
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						}
					}
					else
					{
						npc.broadcastSay(ChatType.GENERAL, "I thought I'd caught one share... Whew!");
					}
				}
				break;
			}
			case 20202:
			{
				if (hasQuestItems(player, SCALE_OF_WYVERN_SHAMHAI) && (getQuestItemsCount(player, EGG_OF_WYVERN_SHAMHAI) < 20))
				{
					giveItems(player, EGG_OF_WYVERN_SHAMHAI, 1);
					if (getQuestItemsCount(player, EGG_OF_WYVERN_SHAMHAI) < 20)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
			case 20589:
			case 20590:
			case 20591:
			case 20592:
			case 20593:
			case 20594:
			case 20595:
			case 20596:
			case 20597:
			case 20598:
			case 20599:
			{
				if (hasQuestItems(player, DELUXE_FAIRY_STONE) && (getRandom(100) < 30))
				{
					st.set("deluxestone", "2");
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					takeItems(player, DELUXE_FAIRY_STONE, 1);
					npc.broadcastSay(ChatType.GENERAL, "The stone... the Elven stone... broke...");
				}
				break;
			}
		}
	}
	
	private static boolean checkItems(Player player, boolean isDeluxe)
	{
		// Conditions required for both cases.
		if ((getQuestItemsCount(player, COAL) < 10) || (getQuestItemsCount(player, CHARCOAL) < 10))
		{
			return false;
		}
		
		if (isDeluxe)
		{
			if ((getQuestItemsCount(player, GEMSTONE_C) >= 1) && (getQuestItemsCount(player, SILVER_NUGGET) >= 5) && (getQuestItemsCount(player, STONE_OF_PURITY) >= 1) && (getQuestItemsCount(player, TOAD_LORD_BACK_SKIN) >= 20))
			{
				return true;
			}
		}
		else
		{
			if ((getQuestItemsCount(player, GEMSTONE_D) >= 1) && (getQuestItemsCount(player, SILVER_NUGGET) >= 3) && (getQuestItemsCount(player, TOAD_LORD_BACK_SKIN) >= 10))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private void giveRandomPet(Player player, boolean hasFairyDust)
	{
		int pet = DRAGONFLUTE_OF_TWILIGHT;
		final int chance = getRandom(100);
		if (hasQuestItems(player, EGG_OF_DRAKE_EXARION))
		{
			takeItems(player, EGG_OF_DRAKE_EXARION, 1);
			if (hasFairyDust)
			{
				if (chance < 45)
				{
					pet = DRAGONFLUTE_OF_WIND;
				}
				else if (chance < 75)
				{
					pet = DRAGONFLUTE_OF_STAR;
				}
			}
			else if (chance < 50)
			{
				pet = DRAGONFLUTE_OF_WIND;
			}
			else if (chance < 85)
			{
				pet = DRAGONFLUTE_OF_STAR;
			}
		}
		else if (hasQuestItems(player, EGG_OF_WYVERN_SUZET))
		{
			takeItems(player, EGG_OF_WYVERN_SUZET, 1);
			if (hasFairyDust)
			{
				if (chance < 55)
				{
					pet = DRAGONFLUTE_OF_WIND;
				}
				else if (chance < 85)
				{
					pet = DRAGONFLUTE_OF_STAR;
				}
			}
			else if (chance < 65)
			{
				pet = DRAGONFLUTE_OF_WIND;
			}
			else if (chance < 95)
			{
				pet = DRAGONFLUTE_OF_STAR;
			}
		}
		else if (hasQuestItems(player, EGG_OF_DRAKE_KALIBRAN))
		{
			takeItems(player, EGG_OF_DRAKE_KALIBRAN, 1);
			if (hasFairyDust)
			{
				if (chance < 60)
				{
					pet = DRAGONFLUTE_OF_WIND;
				}
				else if (chance < 90)
				{
					pet = DRAGONFLUTE_OF_STAR;
				}
			}
			else if (chance < 70)
			{
				pet = DRAGONFLUTE_OF_WIND;
			}
			else
			{
				pet = DRAGONFLUTE_OF_STAR;
			}
		}
		else if (hasQuestItems(player, EGG_OF_WYVERN_SHAMHAI))
		{
			takeItems(player, EGG_OF_WYVERN_SHAMHAI, 1);
			if (hasFairyDust)
			{
				if (chance < 70)
				{
					pet = DRAGONFLUTE_OF_WIND;
				}
				else
				{
					pet = DRAGONFLUTE_OF_STAR;
				}
			}
			else if (chance < 85)
			{
				pet = DRAGONFLUTE_OF_WIND;
			}
			else
			{
				pet = DRAGONFLUTE_OF_STAR;
			}
		}
		else if (hasQuestItems(player, EGG_OF_DRAKE_ZWOV))
		{
			takeItems(player, EGG_OF_DRAKE_ZWOV, 1);
			if (hasFairyDust)
			{
				if (chance < 90)
				{
					pet = DRAGONFLUTE_OF_WIND;
				}
				else
				{
					pet = DRAGONFLUTE_OF_STAR;
				}
			}
			else
			{
				pet = DRAGONFLUTE_OF_WIND;
			}
		}
		
		giveItems(player, pet, 1);
	}
}
