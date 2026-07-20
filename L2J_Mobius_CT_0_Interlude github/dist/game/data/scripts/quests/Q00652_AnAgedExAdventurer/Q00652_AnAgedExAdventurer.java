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
package quests.Q00652_AnAgedExAdventurer;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00652_AnAgedExAdventurer extends Quest
{
	// NPCs
	private static final int TANTAN = 32012;
	private static final int SARA = 30180;
	
	// Item
	private static final int SOULSHOT_C = 1464;
	
	// Reward
	private static final int ENCHANT_ARMOR_D = 956;
	
	// Table of possible spawns
	private static final Location[] SPAWNS =
	{
		new Location(78355, -1325, -3659, 0),
		new Location(79890, -6132, -2922, 0),
		new Location(90012, -7217, -3085, 0),
		new Location(94500, -10129, -3290, 0),
		new Location(96534, -1237, -3677, 0)
	};
	
	// Current position
	private int _currentPosition = 0;
	
	public Q00652_AnAgedExAdventurer()
	{
		super(652, "An Aged Ex-Adventurer");
		addStartNpc(TANTAN);
		addTalkId(TANTAN, SARA);
		addSpawn(TANTAN, 78355, -1325, -3659, 0, false, 0);
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
		
		if (event.equals("32012-02.htm"))
		{
			if (getQuestItemsCount(player, SOULSHOT_C) >= 100)
			{
				st.startQuest();
				takeItems(player, SOULSHOT_C, 100);
				npc.getAI().setIntention(Intention.MOVE_TO, new Location(85326, 7869, -3620));
				startQuestTimer("apparition_npc", 6000, npc, player, false);
			}
			else
			{
				htmltext = "32012-02a.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equals("apparition_npc"))
		{
			int chance = getRandom(5);
			
			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
			{
				chance = getRandom(5);
			}
			
			// Register new position.
			_currentPosition = chance;
			
			npc.deleteMe();
			addSpawn(TANTAN, SPAWNS[chance].getX(), SPAWNS[chance].getY(), SPAWNS[chance].getZ(), SPAWNS[chance].getHeading(), false, 0);
			return null;
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
				htmltext = (player.getLevel() < 46) ? "32012-00.htm" : "32012-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case SARA:
					{
						if (getRandom(100) < 50)
						{
							htmltext = "30180-01.htm";
							giveAdena(player, 5026, true);
							giveItems(player, ENCHANT_ARMOR_D, 1);
						}
						else
						{
							htmltext = "30180-02.htm";
							giveAdena(player, 10000, true);
						}
						
						st.exitQuest(true, true);
						break;
					}
					case TANTAN:
					{
						htmltext = "32012-04a.htm";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
