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
package quests.Q00404_PathOfTheHumanWizard;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00404_PathOfTheHumanWizard extends Quest
{
	// NPCs
	private static final int PARINA = 30391;
	private static final int EARTH_SNAKE = 30409;
	private static final int WASTELAND_LIZARDMAN = 30410;
	private static final int FLAME_SALAMANDER = 30411;
	private static final int WIND_SYLPH = 30412;
	private static final int WATER_UNDINE = 30413;
	
	// Items
	private static final int MAP_OF_LUSTER = 1280;
	private static final int KEY_OF_FLAME = 1281;
	private static final int FLAME_EARING = 1282;
	private static final int BROKEN_BRONZE_MIRROR = 1283;
	private static final int WIND_FEATHER = 1284;
	private static final int WIND_BANGEL = 1285;
	private static final int RAMA_DIARY = 1286;
	private static final int SPARKLE_PEBBLE = 1287;
	private static final int WATER_NECKLACE = 1288;
	private static final int RUST_GOLD_COIN = 1289;
	private static final int RED_SOIL = 1290;
	private static final int EARTH_RING = 1291;
	private static final int BEAD_OF_SEASON = 1292;
	
	public Q00404_PathOfTheHumanWizard()
	{
		super(404, "Path to a Human Wizard");
		registerQuestItems(MAP_OF_LUSTER, KEY_OF_FLAME, FLAME_EARING, BROKEN_BRONZE_MIRROR, WIND_FEATHER, WIND_BANGEL, RAMA_DIARY, SPARKLE_PEBBLE, WATER_NECKLACE, RUST_GOLD_COIN, RED_SOIL, EARTH_RING);
		addStartNpc(PARINA);
		addTalkId(PARINA, EARTH_SNAKE, WASTELAND_LIZARDMAN, FLAME_SALAMANDER, WIND_SYLPH, WATER_UNDINE);
		addKillId(20021, 20359, 27030);
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
		
		if (event.equals("30391-08.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30410-03.htm"))
		{
			st.setCond(6, true);
			takeItems(player, BROKEN_BRONZE_MIRROR, 1);
			giveItems(player, WIND_FEATHER, 1);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		final int cond = st.getCond();
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (player.getPlayerClass() != PlayerClass.MAGE)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.WIZARD) ? "30391-02a.htm" : "30391-01.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30391-02.htm";
				}
				else if (hasQuestItems(player, BEAD_OF_SEASON))
				{
					htmltext = "30391-03.htm";
				}
				else
				{
					htmltext = "30391-04.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case PARINA:
					{
						if (cond < 13)
						{
							htmltext = "30391-05.htm";
						}
						else if (cond == 13)
						{
							htmltext = "30391-06.htm";
							takeItems(player, EARTH_RING, 1);
							takeItems(player, FLAME_EARING, 1);
							takeItems(player, WATER_NECKLACE, 1);
							takeItems(player, WIND_BANGEL, 1);
							giveItems(player, BEAD_OF_SEASON, 1);
							addExpAndSp(player, 3200, 2020);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case FLAME_SALAMANDER:
					{
						if (cond == 1)
						{
							htmltext = "30411-01.htm";
							st.setCond(2, true);
							giveItems(player, MAP_OF_LUSTER, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30411-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30411-03.htm";
							st.setCond(4, true);
							takeItems(player, KEY_OF_FLAME, 1);
							takeItems(player, MAP_OF_LUSTER, 1);
							giveItems(player, FLAME_EARING, 1);
						}
						else if (cond > 3)
						{
							htmltext = "30411-04.htm";
						}
						break;
					}
					case WIND_SYLPH:
					{
						if (cond == 4)
						{
							htmltext = "30412-01.htm";
							st.setCond(5, true);
							giveItems(player, BROKEN_BRONZE_MIRROR, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30412-02.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30412-03.htm";
							st.setCond(7, true);
							takeItems(player, WIND_FEATHER, 1);
							giveItems(player, WIND_BANGEL, 1);
						}
						else if (cond > 6)
						{
							htmltext = "30412-04.htm";
						}
						break;
					}
					case WASTELAND_LIZARDMAN:
					{
						if (cond == 5)
						{
							htmltext = "30410-01.htm";
						}
						else if (cond > 5)
						{
							htmltext = "30410-04.htm";
						}
						break;
					}
					case WATER_UNDINE:
					{
						if (cond == 7)
						{
							htmltext = "30413-01.htm";
							st.setCond(8, true);
							giveItems(player, RAMA_DIARY, 1);
						}
						else if (cond == 8)
						{
							htmltext = "30413-02.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30413-03.htm";
							st.setCond(10, true);
							takeItems(player, RAMA_DIARY, 1);
							takeItems(player, SPARKLE_PEBBLE, -1);
							giveItems(player, WATER_NECKLACE, 1);
						}
						else if (cond > 9)
						{
							htmltext = "30413-04.htm";
						}
						break;
					}
					case EARTH_SNAKE:
					{
						if (cond == 10)
						{
							htmltext = "30409-01.htm";
							st.setCond(11, true);
							giveItems(player, RUST_GOLD_COIN, 1);
						}
						else if (cond == 11)
						{
							htmltext = "30409-02.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30409-03.htm";
							st.setCond(13, true);
							takeItems(player, RED_SOIL, 1);
							takeItems(player, RUST_GOLD_COIN, 1);
							giveItems(player, EARTH_RING, 1);
						}
						else if (cond > 12)
						{
							htmltext = "30409-04.htm";
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
			case 20359: // Ratman Warrior
			{
				if (st.isCond(2) && (getRandom(10) < 8))
				{
					giveItems(player, KEY_OF_FLAME, 1);
					st.setCond(3, true);
				}
				break;
			}
			case 27030: // Water Seer
			{
				if (st.isCond(8) && (getRandom(10) < 8))
				{
					if (getQuestItemsCount(player, SPARKLE_PEBBLE) < 2)
					{
						giveItems(player, SPARKLE_PEBBLE, 1);
						
						if (getQuestItemsCount(player, SPARKLE_PEBBLE) >= 2)
						{
							st.setCond(9, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case 20021: // Red Bear
			{
				if (st.isCond(11) && (getRandom(10) < 2))
				{
					giveItems(player, RED_SOIL, 1);
					st.setCond(12, true);
				}
				break;
			}
		}
	}
}
