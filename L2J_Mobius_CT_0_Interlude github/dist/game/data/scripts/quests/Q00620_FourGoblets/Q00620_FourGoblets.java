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
package quests.Q00620_FourGoblets;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.util.ArrayUtil;

public class Q00620_FourGoblets extends Quest
{
	// NPCs
	private static final int GHOST_OF_WIGOTH_1 = 31452;
	private static final int NAMELESS_SPIRIT = 31453;
	private static final int GHOST_OF_WIGOTH_2 = 31454;
	private static final int GHOST_CHAMBERLAIN_1 = 31919;
	private static final int GHOST_CHAMBERLAIN_2 = 31920;
	private static final int CONQUERORS_SEPULCHER_MANAGER = 31921;
	private static final int EMPERORS_SEPULCHER_MANAGER = 31922;
	private static final int GREAT_SAGES_SEPULCHER_MANAGER = 31923;
	private static final int JUDGES_SEPULCHER_MANAGER = 31924;
	
	// Items
	private static final int RELIC = 7254;
	private static final int SEALED_BOX = 7255;
	private static final int GOBLET_OF_ALECTIA = 7256;
	private static final int GOBLET_OF_TISHAS = 7257;
	private static final int GOBLET_OF_MEKARA = 7258;
	private static final int GOBLET_OF_MORIGUL = 7259;
	private static final int USED_GRAVE_PASS = 7261;
	
	// Rewards
	private static final int ANTIQUE_BROOCH = 7262;
	private static final int[] RCP_REWARDS =
	{
		6881,
		6883,
		6885,
		6887,
		6891,
		6893,
		6895,
		6897,
		6899,
		7580
	};
	
	public Q00620_FourGoblets()
	{
		super(620, "Four Goblets");
		registerQuestItems(SEALED_BOX, USED_GRAVE_PASS, GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL);
		addStartNpc(NAMELESS_SPIRIT, CONQUERORS_SEPULCHER_MANAGER, EMPERORS_SEPULCHER_MANAGER, GREAT_SAGES_SEPULCHER_MANAGER, JUDGES_SEPULCHER_MANAGER, GHOST_CHAMBERLAIN_1, GHOST_CHAMBERLAIN_2);
		addTalkId(NAMELESS_SPIRIT, CONQUERORS_SEPULCHER_MANAGER, EMPERORS_SEPULCHER_MANAGER, GREAT_SAGES_SEPULCHER_MANAGER, JUDGES_SEPULCHER_MANAGER, GHOST_CHAMBERLAIN_1, GHOST_CHAMBERLAIN_2, GHOST_OF_WIGOTH_1, GHOST_OF_WIGOTH_2);
		for (int id = 18120; id <= 18256; id++)
		{
			addKillId(id);
		}
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
		
		if (event.equals("31452-05.htm"))
		{
			if (getRandomBoolean())
			{
				htmltext = (getRandomBoolean()) ? "31452-03.htm" : "31452-04.htm";
			}
		}
		else if (event.equals("EXIT"))
		{
			player.teleToLocation(169590, -90218, -2914); // Wigoth : Teleport back to Pilgrim's Temple
		}
		else if (event.equals("31453-13.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31453-16.htm"))
		{
			if (hasQuestItems(player, GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL))
			{
				st.setCond(2, true);
				takeItems(player, GOBLET_OF_ALECTIA, -1);
				takeItems(player, GOBLET_OF_TISHAS, -1);
				takeItems(player, GOBLET_OF_MEKARA, -1);
				takeItems(player, GOBLET_OF_MORIGUL, -1);
				giveItems(player, ANTIQUE_BROOCH, 1);
			}
			else
			{
				htmltext = "31453-14.htm";
			}
		}
		// else if (event.equals("31453-13.htm"))
		// {
		// if (st.getCond()s == 2)
		// {
		// htmltext = "31453-19.htm";
		// }
		// }
		else if (event.equals("31453-18.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (event.equals("boxes"))
		{
			if (hasQuestItems(player, SEALED_BOX))
			{
				takeItems(player, SEALED_BOX, 1);
				if (!calculateBoxReward(player))
				{
					htmltext = (getRandomBoolean()) ? "31454-09.htm" : "31454-10.htm";
				}
				else
				{
					htmltext = "31454-08.htm";
				}
			}
		}
		// Ghost Chamberlain of Elmoreden: Teleport to 4s.
		else if (event.equals("TELEPORT_TO_4S"))
		{
			if (hasQuestItems(player, ANTIQUE_BROOCH))
			{
				player.teleToLocation(178298, -84574, -7216);
				return null;
			}
			
			if (hasQuestItems(player, USED_GRAVE_PASS))
			{
				takeItems(player, USED_GRAVE_PASS, 1);
				player.teleToLocation(178298, -84574, -7216);
				return null;
			}
			
			htmltext = npc.getId() + "-00.htm";
		}
		// Ghost Chamberlain of Elmoreden: Teleport to Imperial Tomb Entrance.
		else if (event.equals("TELEPORT_TO_IT"))
		{
			if (hasQuestItems(player, ANTIQUE_BROOCH))
			{
				player.teleToLocation(186942, -75602, -2834);
				return null;
			}
			
			if (hasQuestItems(player, USED_GRAVE_PASS))
			{
				takeItems(player, USED_GRAVE_PASS, 1);
				player.teleToLocation(186942, -75602, -2834);
				return null;
			}
			
			htmltext = npc.getId() + "-00.htm";
		}
		else if (event.equals("31919-06.htm"))
		{
			if (hasQuestItems(player, SEALED_BOX))
			{
				takeItems(player, SEALED_BOX, 1);
				if (!calculateBoxReward(player))
				{
					htmltext = (getRandomBoolean()) ? "31919-04.htm" : "31919-05.htm";
				}
				else
				{
					htmltext = "31919-03.htm";
				}
			}
		}
		// If event is a simple digit, parse it to get an integer form, then test the reward list
		else if (StringUtil.isNumeric(event))
		{
			final int id = Integer.parseInt(event);
			if (ArrayUtil.contains(RCP_REWARDS, id) && (getQuestItemsCount(player, RELIC) >= 1000))
			{
				takeItems(player, RELIC, 1000);
				giveItems(player, id, 1);
			}
			
			htmltext = "31454-12.htm";
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		if (npcId == GHOST_OF_WIGOTH_1)
		{
			if (cond == 1)
			{
				htmltext = "31452-01.htm";
			}
			else if (cond == 2)
			{
				htmltext = "31452-02.htm";
			}
		}
		else if (npcId == NAMELESS_SPIRIT)
		{
			if (cond == 0)
			{
				htmltext = (player.getLevel() >= 74) ? "31453-01.htm" : "31453-12.htm";
			}
			else if (cond == 1)
			{
				htmltext = (hasQuestItems(player, GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL)) ? "31453-15.htm" : "31453-14.htm";
			}
			else if (cond == 2)
			{
				htmltext = "31453-17.htm";
			}
		}
		else if (npcId == GHOST_OF_WIGOTH_2)
		{
			// Possibilities : 0 = nothing, 1 = seal boxes only, 2 = relics only, 3 = both, 4/5/6/7 = "4 goblets" versions of 0/1/2/3.
			int index = 0;
			if (hasQuestItems(player, GOBLET_OF_ALECTIA, GOBLET_OF_TISHAS, GOBLET_OF_MEKARA, GOBLET_OF_MORIGUL))
			{
				index = 4;
			}
			
			final boolean gotSealBoxes = hasQuestItems(player, SEALED_BOX);
			final boolean gotEnoughRelics = getQuestItemsCount(player, RELIC) >= 1000;
			if (gotSealBoxes && gotEnoughRelics)
			{
				index += 3;
			}
			else if (!gotSealBoxes && gotEnoughRelics)
			{
				index += 2;
			}
			else if (gotSealBoxes)
			{
				index += 1;
			}
			
			htmltext = "31454-0" + index + ".htm";
		}
		else
		{
			htmltext = npcId + "-01.htm";
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
		
		final Player partyMember = st.getPlayer();
		
		if (getRandom(10) < 3)
		{
			giveItems(partyMember, SEALED_BOX, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
	
	/**
	 * Calculate boxes rewards, then return if there was a reward.
	 * @param player the Player, used to reward him.
	 * @return true if there was a reward, false if not (used to call a "no-reward" html)
	 */
	private static boolean calculateBoxReward(Player player)
	{
		boolean reward = false;
		final int rnd = getRandom(5);
		if (rnd == 0)
		{
			giveAdena(player, 10000, true);
			reward = true;
		}
		else if (rnd == 1)
		{
			if (getRandom(1000) < 848)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 43)
				{
					giveItems(player, 1884, 42);
				}
				else if (i < 66)
				{
					giveItems(player, 1895, 36);
				}
				else if (i < 184)
				{
					giveItems(player, 1876, 4);
				}
				else if (i < 250)
				{
					giveItems(player, 1881, 6);
				}
				else if (i < 287)
				{
					giveItems(player, 5549, 8);
				}
				else if (i < 484)
				{
					giveItems(player, 1874, 1);
				}
				else if (i < 681)
				{
					giveItems(player, 1889, 1);
				}
				else if (i < 799)
				{
					giveItems(player, 1877, 1);
				}
				else if (i < 902)
				{
					giveItems(player, 1894, 1);
				}
				else
				{
					giveItems(player, 4043, 1);
				}
			}
			
			if (getRandom(1000) < 323)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 335)
				{
					giveItems(player, 1888, 1);
				}
				else if (i < 556)
				{
					giveItems(player, 4040, 1);
				}
				else if (i < 725)
				{
					giveItems(player, 1890, 1);
				}
				else if (i < 872)
				{
					giveItems(player, 5550, 1);
				}
				else if (i < 962)
				{
					giveItems(player, 1893, 1);
				}
				else if (i < 986)
				{
					giveItems(player, 4046, 1);
				}
				else
				{
					giveItems(player, 4048, 1);
				}
			}
		}
		else if (rnd == 2)
		{
			if (getRandom(1000) < 847)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 148)
				{
					giveItems(player, 1878, 8);
				}
				else if (i < 175)
				{
					giveItems(player, 1882, 24);
				}
				else if (i < 273)
				{
					giveItems(player, 1879, 4);
				}
				else if (i < 322)
				{
					giveItems(player, 1880, 6);
				}
				else if (i < 357)
				{
					giveItems(player, 1885, 6);
				}
				else if (i < 554)
				{
					giveItems(player, 1875, 1);
				}
				else if (i < 685)
				{
					giveItems(player, 1883, 1);
				}
				else if (i < 803)
				{
					giveItems(player, 5220, 1);
				}
				else if (i < 901)
				{
					giveItems(player, 4039, 1);
				}
				else
				{
					giveItems(player, 4044, 1);
				}
			}
			
			if (getRandom(1000) < 251)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 350)
				{
					giveItems(player, 1887, 1);
				}
				else if (i < 587)
				{
					giveItems(player, 4042, 1);
				}
				else if (i < 798)
				{
					giveItems(player, 1886, 1);
				}
				else if (i < 922)
				{
					giveItems(player, 4041, 1);
				}
				else if (i < 966)
				{
					giveItems(player, 1892, 1);
				}
				else if (i < 996)
				{
					giveItems(player, 1891, 1);
				}
				else
				{
					giveItems(player, 4047, 1);
				}
			}
		}
		else if (rnd == 3)
		{
			if (getRandom(1000) < 31)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 223)
				{
					giveItems(player, 730, 1);
				}
				else if (i < 893)
				{
					giveItems(player, 948, 1);
				}
				else
				{
					giveItems(player, 960, 1);
				}
			}
			
			if (getRandom(1000) < 5)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 202)
				{
					giveItems(player, 729, 1);
				}
				else if (i < 928)
				{
					giveItems(player, 947, 1);
				}
				else
				{
					giveItems(player, 959, 1);
				}
			}
		}
		else if (rnd == 4)
		{
			if (getRandom(1000) < 329)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 88)
				{
					giveItems(player, 6698, 1);
				}
				else if (i < 185)
				{
					giveItems(player, 6699, 1);
				}
				else if (i < 238)
				{
					giveItems(player, 6700, 1);
				}
				else if (i < 262)
				{
					giveItems(player, 6701, 1);
				}
				else if (i < 292)
				{
					giveItems(player, 6702, 1);
				}
				else if (i < 356)
				{
					giveItems(player, 6703, 1);
				}
				else if (i < 420)
				{
					giveItems(player, 6704, 1);
				}
				else if (i < 482)
				{
					giveItems(player, 6705, 1);
				}
				else if (i < 554)
				{
					giveItems(player, 6706, 1);
				}
				else if (i < 576)
				{
					giveItems(player, 6707, 1);
				}
				else if (i < 640)
				{
					giveItems(player, 6708, 1);
				}
				else if (i < 704)
				{
					giveItems(player, 6709, 1);
				}
				else if (i < 777)
				{
					giveItems(player, 6710, 1);
				}
				else if (i < 799)
				{
					giveItems(player, 6711, 1);
				}
				else if (i < 863)
				{
					giveItems(player, 6712, 1);
				}
				else if (i < 927)
				{
					giveItems(player, 6713, 1);
				}
				else
				{
					giveItems(player, 6714, 1);
				}
			}
			
			if (getRandom(1000) < 54)
			{
				reward = true;
				final int i = getRandom(1000);
				if (i < 100)
				{
					giveItems(player, 6688, 1);
				}
				else if (i < 198)
				{
					giveItems(player, 6689, 1);
				}
				else if (i < 298)
				{
					giveItems(player, 6690, 1);
				}
				else if (i < 398)
				{
					giveItems(player, 6691, 1);
				}
				else if (i < 499)
				{
					giveItems(player, 7579, 1);
				}
				else if (i < 601)
				{
					giveItems(player, 6693, 1);
				}
				else if (i < 703)
				{
					giveItems(player, 6694, 1);
				}
				else if (i < 801)
				{
					giveItems(player, 6695, 1);
				}
				else if (i < 902)
				{
					giveItems(player, 6696, 1);
				}
				else
				{
					giveItems(player, 6697, 1);
				}
			}
		}
		
		return reward;
	}
}
