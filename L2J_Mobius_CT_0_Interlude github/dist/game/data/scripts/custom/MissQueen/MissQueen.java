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
package custom.MissQueen;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.script.State;

public class MissQueen extends Script
{
	// NPC
	private static final int MISS_QUEEN = 31760;
	
	// Rewards
	private static final int COUPON_ONE = 7832;
	private static final int COUPON_TWO = 7833;
	
	// Locations
	private static final Location[] LOCATIONS =
	{
		// new Location(116224, -181728, -1378, 0), // Dwarven Village
		new Location(114885, -178092, -832, 0), // Dwarven Village
		new Location(45472, 49312, -3072, 53000), // Elven Village
		// new Location(47648, 51296, -2994, 38500), // Elven Village
		// new Location(11340, 15972, -4582, 14000), // Dark Elf Village
		new Location(10968, 17540, -4572, 55000), // Dark Elf Village
		new Location(-14048, 123184, -3120, 32000), // Gludio Village
		new Location(-44979, -113508, -199, 32000), // Orc Village
		new Location(-84119, 243254, -3730, 8000), // Talking Island
		// new Location(-84336, 242156, -3730, 24500), // Talking Island
		new Location(-82032, 150160, -3127, 16500) // Gludin Village
	};
	
	public MissQueen()
	{
		// Spawn the 11 NPCs.
		for (Location loc : LOCATIONS)
		{
			addSpawn(MISS_QUEEN, loc, false, 0);
		}
		
		addStartNpc(MISS_QUEEN);
		addTalkId(MISS_QUEEN);
		addFirstTalkId(MISS_QUEEN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		if (event.equals("newbie_coupon"))
		{
			if ((player.getPlayerClass().level() == 0) && (player.getLevel() >= 6) && (player.getLevel() <= 25) && (player.getPkKills() <= 0))
			{
				final QuestState qs = getQuestState(player, true);
				if (qs.getInt("reward_1") == 1)
				{
					htmltext = "31760-01.htm";
				}
				else
				{
					qs.setState(State.STARTED);
					htmltext = "31760-02.htm";
					qs.set("reward_1", "1");
					giveItems(player, COUPON_ONE, 1);
				}
			}
			else
			{
				htmltext = "31760-03.htm";
			}
		}
		else if (event.equals("traveller_coupon"))
		{
			if ((player.getPlayerClass().level() == 1) && (player.getLevel() >= 6) && (player.getLevel() <= 25) && (player.getPkKills() <= 0))
			{
				final QuestState qs = getQuestState(player, true);
				if (qs.getInt("reward_2") == 1)
				{
					htmltext = "31760-04.htm";
				}
				else
				{
					qs.setState(State.STARTED);
					htmltext = "31760-05.htm";
					qs.set("reward_2", "1");
					giveItems(player, COUPON_TWO, 1);
				}
			}
			else
			{
				htmltext = "31760-06.htm";
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "31760.htm";
	}
	
	public static void main(String[] args)
	{
		new MissQueen();
	}
}
