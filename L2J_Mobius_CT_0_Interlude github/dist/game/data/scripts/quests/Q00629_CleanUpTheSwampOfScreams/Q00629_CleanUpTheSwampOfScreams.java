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
package quests.Q00629_CleanUpTheSwampOfScreams;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00629_CleanUpTheSwampOfScreams extends Quest
{
	// NPC
	private static final int PIERCE = 31553;
	
	// Items
	private static final int TALON_OF_STAKATO = 7250;
	private static final int GOLDEN_RAM_COIN = 7251;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(21508, 500000);
		CHANCES.put(21509, 431000);
		CHANCES.put(21510, 521000);
		CHANCES.put(21511, 576000);
		CHANCES.put(21512, 746000);
		CHANCES.put(21513, 530000);
		CHANCES.put(21514, 538000);
		CHANCES.put(21515, 545000);
		CHANCES.put(21516, 553000);
		CHANCES.put(21517, 560000);
	}
	
	public Q00629_CleanUpTheSwampOfScreams()
	{
		super(629, "Clean up the Swamp of Screams");
		registerQuestItems(TALON_OF_STAKATO, GOLDEN_RAM_COIN);
		addStartNpc(PIERCE);
		addTalkId(PIERCE);
		addKillId(CHANCES.keySet());
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
			case "31553-1.htm":
			{
				if (player.getLevel() >= 66)
				{
					st.startQuest();
				}
				else
				{
					htmltext = "31553-0a.htm";
					st.exitQuest(true);
				}
				break;
			}
			case "31553-3.htm":
			{
				if (getQuestItemsCount(player, TALON_OF_STAKATO) >= 100)
				{
					takeItems(player, TALON_OF_STAKATO, 100);
					giveItems(player, GOLDEN_RAM_COIN, 20);
				}
				else
				{
					htmltext = "31553-3a.htm";
				}
				break;
			}
			case "31553-5.htm":
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
		
		if (!hasAtLeastOneQuestItem(player, 7246, 7247))
		{
			return "31553-6.htm";
		}
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() < 66) ? "31553-0a.htm" : "31553-0.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (getQuestItemsCount(player, TALON_OF_STAKATO) >= 100) ? "31553-2.htm" : "31553-1a.htm";
				break;
			}
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
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			final Player partyMember = st.getPlayer();
			giveItems(partyMember, TALON_OF_STAKATO, 1);
			if (getQuestItemsCount(partyMember, TALON_OF_STAKATO) < 100)
			{
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(2, true);
			}
		}
	}
}
