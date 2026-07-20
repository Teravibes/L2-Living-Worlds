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
package quests.Q00377_ExplorationOfTheGiantsCavePart2;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00377_ExplorationOfTheGiantsCavePart2 extends Quest
{
	// Items
	private static final int ANCIENT_BOOK = 5955;
	private static final int DICTIONARY_INTERMEDIATE = 5892;
	private static final int[][] BOOKS =
	{
		// @formatter:off
		// science & technology -> majestic leather, leather armor of nightmare
		{5945, 5946, 5947, 5948, 5949},
		// culture -> armor of nightmare, majestic plate
		{5950, 5951, 5952, 5953, 5954}
		// @formatter:on
	};
	
	// Rewards
	private static final int[][] RECIPES =
	{
		// @formatter:off
		// science & technology -> majestic leather, leather armor of nightmare
		{5338, 5336},
		// culture -> armor of nightmare, majestic plate
		{5420, 5422}
		// @formatter:on
	};
	
	public Q00377_ExplorationOfTheGiantsCavePart2()
	{
		super(377, "Exploration of the Giants Cave, Part 2");
		addStartNpc(31147); // Sobling
		addTalkId(31147);
		addKillId(20654, 20656, 20657, 20658);
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
			case "31147-03.htm":
			{
				st.startQuest();
				break;
			}
			case "31147-04.htm":
			{
				htmltext = checkItems(player);
				break;
			}
			case "31147-07.htm":
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
				htmltext = ((player.getLevel() < 57) || !hasQuestItems(player, DICTIONARY_INTERMEDIATE)) ? "31147-01.htm" : "31147-02.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = checkItems(player);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember != null)
		{
			giveItemRandomly(partyMember, npc, ANCIENT_BOOK, 1, 0, 0.8, true);
		}
	}
	
	private static String checkItems(Player player)
	{
		for (int type = 0; type < BOOKS.length; type++)
		{
			boolean complete = true;
			for (int book : BOOKS[type])
			{
				if (!hasQuestItems(player, book))
				{
					complete = false;
				}
			}
			
			if (complete)
			{
				for (int book : BOOKS[type])
				{
					takeItems(player, book, 1);
				}
				
				giveItems(player, RECIPES[type][getRandom(RECIPES[type].length)], 1);
				return "31147-04.htm";
			}
		}
		
		return "31147-05.htm";
	}
}
