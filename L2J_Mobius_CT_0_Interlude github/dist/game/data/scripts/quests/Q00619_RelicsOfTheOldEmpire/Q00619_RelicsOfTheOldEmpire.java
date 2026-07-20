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
package quests.Q00619_RelicsOfTheOldEmpire;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00619_RelicsOfTheOldEmpire extends Quest
{
	// NPC
	private static int GHOST_OF_ADVENTURER = 31538;
	
	// Items
	private static int RELICS = 7254;
	private static int ENTRANCE = 7075;
	
	// Rewards ; all S grade weapons recipe (60%)
	private static int[] RCP_REWARDS =
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
	
	public Q00619_RelicsOfTheOldEmpire()
	{
		super(619, "Relics of the Old Empire");
		registerQuestItems(RELICS);
		addStartNpc(GHOST_OF_ADVENTURER);
		addTalkId(GHOST_OF_ADVENTURER);
		for (int id = 21396; id <= 21434; id++)
		{
			// IT monsters
			addKillId(id);
		}
		
		// monsters at IT entrance
		addKillId(21798, 21799, 21800);
		for (int id = 18120; id <= 18256; id++)
		{
			// Sepulchers monsters
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
		
		switch (event)
		{
			case "31538-03.htm":
			{
				st.startQuest();
				break;
			}
			case "31538-09.htm":
			{
				if (getQuestItemsCount(player, RELICS) >= 1000)
				{
					htmltext = "31538-09.htm";
					takeItems(player, RELICS, 1000);
					giveItems(player, RCP_REWARDS[getRandom(RCP_REWARDS.length)], 1);
				}
				else
				{
					htmltext = "31538-06.htm";
				}
				break;
			}
			case "31538-10.htm":
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
				htmltext = (player.getLevel() < 74) ? "31538-02.htm" : "31538-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (getQuestItemsCount(player, RELICS) >= 1000)
				{
					htmltext = "31538-04.htm";
				}
				else if (hasQuestItems(player, ENTRANCE))
				{
					htmltext = "31538-06.htm";
				}
				else
				{
					htmltext = "31538-07.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if ((qs == null) || !qs.isStarted())
		{
			return;
		}
		
		final Player partyMember = qs.getPlayer();
		
		giveItemRandomly(partyMember, npc, RELICS, 1, 0, 1, true);
		
		if (getRandom(100) < 30)
		{
			giveItemRandomly(partyMember, npc, ENTRANCE, 1, 0, 1, true);
		}
	}
}
