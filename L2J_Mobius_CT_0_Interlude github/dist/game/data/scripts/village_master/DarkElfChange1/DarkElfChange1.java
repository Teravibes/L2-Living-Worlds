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
package village_master.DarkElfChange1;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Dark Elven Change Part 1.
 * @author nonom
 */
public class DarkElfChange1 extends Script
{
	// NPCs
	private static int[] NPCS =
	{
		30290, // Xenos
		30297, // Tobias
		30462, // Tronix
	};
	
	// Items
	private static int GAZE_OF_ABYSS = 1244;
	private static int IRON_HEART = 1252;
	private static int JEWEL_OF_DARKNESS = 1261;
	private static int ORB_OF_ABYSS = 1270;
	
	// Rewards
	private static int SHADOW_WEAPON_COUPON_DGRADE = 8869;
	// @formatter:off
	private static int[][] CLASSES = 
	{
		{ 32, 31, 15, 16, 17, 18, GAZE_OF_ABYSS }, // PK
		{ 35, 31, 19, 20, 21, 22, IRON_HEART }, // AS
		{ 39, 38, 23, 24, 25, 26, JEWEL_OF_DARKNESS }, // DW
		{ 42, 38, 27, 28, 29, 30, ORB_OF_ABYSS }, // SO
	};
	// @formatter:on
	private DarkElfChange1()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (StringUtil.isNumeric(event))
		{
			final int i = Integer.parseInt(event);
			final PlayerClass cid = player.getPlayerClass();
			if ((cid.getRace() == Race.DARK_ELF) && (cid.getId() == CLASSES[i][1]))
			{
				int suffix;
				final boolean item = hasQuestItems(player, CLASSES[i][6]);
				if (player.getLevel() < 20)
				{
					suffix = (!item) ? CLASSES[i][2] : CLASSES[i][3];
				}
				else
				{
					if (!item)
					{
						suffix = CLASSES[i][4];
					}
					else
					{
						suffix = CLASSES[i][5];
						giveItems(player, SHADOW_WEAPON_COUPON_DGRADE, 15);
						takeItems(player, CLASSES[i][6], -1);
						player.setPlayerClass(CLASSES[i][0]);
						player.setBaseClass(CLASSES[i][0]);
						playSound(player, QuestSound.ITEMSOUND_QUEST_FANFARE_2);
						player.broadcastUserInfo();
					}
				}
				
				return npc.getId() + "-" + suffix + ".html";
			}
		}
		
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		if (player.isSubClassActive())
		{
			return htmltext;
		}
		
		final PlayerClass cid = player.getPlayerClass();
		if (cid.getRace() == Race.DARK_ELF)
		{
			switch (cid)
			{
				case DARK_FIGHTER:
				{
					htmltext = npc.getId() + "-01.html";
					break;
				}
				case DARK_MAGE:
				{
					htmltext = npc.getId() + "-08.html";
					break;
				}
				default:
				{
					if (cid.level() == 1)
					{
						// first occupation change already made
						return npc.getId() + "-32.html";
					}
					else if (cid.level() >= 2)
					{
						// second/third occupation change already made
						return npc.getId() + "-31.html";
					}
				}
			}
		}
		else
		{
			htmltext = npc.getId() + "-33.html"; // other races
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new DarkElfChange1();
	}
}
