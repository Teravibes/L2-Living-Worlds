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
package quests.Q00366_SilverHairedShaman;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00366_SilverHairedShaman extends Quest
{
	// NPC
	private static final int DIETER = 30111;
	
	// Item
	private static final int SAIRONS_SILVER_HAIR = 5874;
	
	// Misc
	private static final int MIN_LEVEL = 48;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(20986, 56);
		CHANCES.put(20987, 66);
		CHANCES.put(20988, 62);
	}
	
	public Q00366_SilverHairedShaman()
	{
		super(366, "Silver Haired Shaman");
		registerQuestItems(SAIRONS_SILVER_HAIR);
		addStartNpc(DIETER);
		addTalkId(DIETER);
		addKillId(20986, 20987, 20988);
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
		
		if (event.equals("30111-2.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30111-6.htm"))
		{
			st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < MIN_LEVEL) ? "30111-0.htm" : "30111-1.htm";
				break;
			}
			case State.STARTED:
			{
				final int count = getQuestItemsCount(player, SAIRONS_SILVER_HAIR);
				if (count == 0)
				{
					htmltext = "30111-3.htm";
				}
				else
				{
					htmltext = "30111-4.htm";
					takeItems(player, SAIRONS_SILVER_HAIR, -1);
					giveAdena(player, 12070 + (500 * count), true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 1, 3, npc);
		if ((qs != null) && (getRandom(100) < CHANCES.get(npc.getId())))
		{
			giveItemRandomly(qs.getPlayer(), npc, SAIRONS_SILVER_HAIR, 1, 0, 1, true);
		}
	}
}
