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
package quests.Q00653_WildMaiden;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

public class Q00653_WildMaiden extends Quest
{
	// NPCs
	private static final int SUKI = 32013;
	private static final int GALIBREDO = 30181;
	
	// Item
	private static final int SCROLL_OF_ESCAPE = 736;
	
	// Table of possible spawns
	private static final Location[] SPAWNS =
	{
		new Location(66578, 72351, -3731, 0),
		new Location(77189, 73610, -3708, 2555),
		new Location(71809, 67377, -3675, 29130),
		new Location(69166, 88825, -3447, 43886)
	};
	
	// Current position
	private int _currentPosition = 0;
	
	public Q00653_WildMaiden()
	{
		super(653, "Wild Maiden");
		addStartNpc(SUKI);
		addTalkId(SUKI, GALIBREDO);
		addSpawn(SUKI, 66578, 72351, -3731, 0, false, 0);
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
		
		if (event.equals("32013-03.htm"))
		{
			if (hasQuestItems(player, SCROLL_OF_ESCAPE))
			{
				st.startQuest();
				takeItems(player, SCROLL_OF_ESCAPE, 1);
				npc.broadcastPacket(new MagicSkillUse(npc, npc, 2013, 1, 3500, 0));
				startQuestTimer("apparition_npc", 4000, npc, player, false);
			}
			else
			{
				htmltext = "32013-03a.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equals("apparition_npc"))
		{
			int chance = getRandom(4);
			
			// Loop to avoid to spawn to the same place.
			while (chance == _currentPosition)
			{
				chance = getRandom(4);
			}
			
			// Register new position.
			_currentPosition = chance;
			
			npc.deleteMe();
			addSpawn(SUKI, SPAWNS[chance], false, 0);
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
				htmltext = (player.getLevel() < 36) ? "32013-01.htm" : "32013-02.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case GALIBREDO:
					{
						htmltext = "30181-01.htm";
						giveAdena(player, 2883, true);
						st.exitQuest(true, true);
						break;
					}
					case SUKI:
					{
						htmltext = "32013-04a.htm";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
