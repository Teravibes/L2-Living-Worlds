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
package quests.Q00651_RunawayYouth;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

public class Q00651_RunawayYouth extends Quest
{
	// NPCs
	private static final int IVAN = 32014;
	private static final int BATIDAE = 31989;
	
	// Item
	private static final int SCROLL_OF_ESCAPE = 736;
	
	// Table of possible spawns
	private static final Location[] SPAWNS =
	{
		new Location(118600, -161235, -1119, 0),
		new Location(108380, -150268, -2376, 0),
		new Location(123254, -148126, -3425, 0)
	};
	
	// Current position
	private int _currentPosition = 0;
	
	public Q00651_RunawayYouth()
	{
		super(651, "Runaway Youth");
		addStartNpc(IVAN);
		addTalkId(IVAN, BATIDAE);
		addSpawn(IVAN, 118600, -161235, -1119, 0, false, 0);
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
		
		if (event.equals("32014-04.htm"))
		{
			if (hasQuestItems(player, SCROLL_OF_ESCAPE))
			{
				st.startQuest();
				takeItems(player, SCROLL_OF_ESCAPE, 1);
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 2013, 1, 3500, 0));
				startQuestTimer("apparition_npc", 4000, npc, player, false);
				htmltext = "32014-03.htm";
			}
			else
			{
				st.exitQuest(true);
			}
		}
		else if (event.equals("apparition_npc"))
		{
			int chance = getRandom(3);
			
			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
			{
				chance = getRandom(3);
			}
			
			// Register new position.
			_currentPosition = chance;
			
			npc.deleteMe();
			addSpawn(IVAN, SPAWNS[chance], false, 0);
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
				htmltext = (player.getLevel() < 26) ? "32014-01.htm" : "32014-02.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case BATIDAE:
					{
						htmltext = "31989-01.htm";
						giveAdena(player, 2883, true);
						st.exitQuest(true, true);
						break;
					}
					case IVAN:
					{
						htmltext = "32014-04a.htm";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
