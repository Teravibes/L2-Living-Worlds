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
package village_master.DarkElfChange2;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Dark Elven Change Part 2.
 * @author nonom
 */
public class DarkElfChange2 extends Script
{
	// NPCs
	private static int[] NPCS =
	{
		30195, // Brecson
		30699, // Medown
		30474, // Angus
		31324, // Andromeda
		30862, // Oltran
		30910, // Xairakin
		31285, // Samael
		31334, // Tifaren
		31974, // Drizzit
		32096, // Helminter
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE = 8870;
	private static int MARK_OF_CHALLENGER = 2627;
	private static int MARK_OF_DUTY = 2633;
	private static int MARK_OF_SEEKER = 2673;
	private static int MARK_OF_SCHOLAR = 2674;
	private static int MARK_OF_PILGRIM = 2721;
	private static int MARK_OF_DUELIST = 2762;
	private static int MARK_OF_SEARCHER = 2809;
	private static int MARK_OF_REFORMER = 2821;
	private static int MARK_OF_MAGUS = 2840;
	private static int MARK_OF_FATE = 3172;
	private static int MARK_OF_SAGITTARIUS = 3293;
	private static int MARK_OF_WITCHCRAFT = 3307;
	private static int MARK_OF_SUMMONER = 3336;
	// @formatter:off
	private static int[][] CLASSES = 
	{
		{ 33, 32, 26, 27, 28, 29, MARK_OF_DUTY, MARK_OF_FATE, MARK_OF_WITCHCRAFT }, // SK
		{ 34, 32, 30, 31, 32, 33, MARK_OF_CHALLENGER, MARK_OF_FATE, MARK_OF_DUELIST }, // BD
		{ 43, 42, 34, 35, 36, 37, MARK_OF_PILGRIM, MARK_OF_FATE, MARK_OF_REFORMER }, // SE
		{ 36, 35, 38, 39, 40, 41, MARK_OF_SEEKER, MARK_OF_FATE, MARK_OF_SEARCHER }, // AW
		{ 37, 35, 42, 43, 44, 45, MARK_OF_SEEKER, MARK_OF_FATE, MARK_OF_SAGITTARIUS }, // PR
		{ 40, 39, 46, 47, 48, 49, MARK_OF_SCHOLAR, MARK_OF_FATE, MARK_OF_MAGUS }, // SH
		{ 41, 39, 50, 51, 52, 53, MARK_OF_SCHOLAR, MARK_OF_FATE, MARK_OF_SUMMONER }, // PS
	};
	// @formatter:on
	private DarkElfChange2()
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
				final boolean item1 = hasQuestItems(player, CLASSES[i][6]);
				final boolean item2 = hasQuestItems(player, CLASSES[i][7]);
				final boolean item3 = hasQuestItems(player, CLASSES[i][8]);
				if (player.getLevel() < 40)
				{
					suffix = (!item1 || !item2 || !item3) ? CLASSES[i][2] : CLASSES[i][3];
				}
				else
				{
					if (!item1 || !item2 || !item3)
					{
						suffix = CLASSES[i][4];
					}
					else
					{
						suffix = CLASSES[i][5];
						takeItems(player, CLASSES[i][6], -1);
						takeItems(player, CLASSES[i][7], -1);
						takeItems(player, CLASSES[i][8], -1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_FANFARE_2);
						player.setPlayerClass(CLASSES[i][0]);
						player.setBaseClass(CLASSES[i][0]);
						giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
						player.broadcastUserInfo();
					}
				}
				
				return "30474-" + suffix + ".html";
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
				case PALUS_KNIGHT:
				{
					htmltext = "30474-01.html";
					break;
				}
				case SHILLIEN_ORACLE:
				{
					htmltext = "30474-08.html";
					break;
				}
				case ASSASSIN:
				{
					htmltext = "30474-12.html";
					break;
				}
				case DARK_WIZARD:
				{
					htmltext = "30474-19.html";
					break;
				}
				default:
				{
					if (cid.level() == 0)
					{
						// first occupation not made yet
						htmltext = "30474-55.html";
					}
					else if (cid.level() >= 2)
					{
						// second/third occupation change already made
						htmltext = "30474-54.html";
					}
					else
					{
						htmltext = "30474-56.html";
					}
				}
			}
		}
		else
		{
			htmltext = "30474-56.html"; // other races
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new DarkElfChange2();
	}
}
