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
package quests.Q00617_GatherTheFlames;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00617_GatherTheFlames extends Quest
{
	// NPCs
	private static final int HILDA = 31271;
	private static final int VULCAN = 31539;
	private static final int ROONEY = 32049;
	
	// Items
	private static final int TORCH = 7264;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(21381, 510000);
		CHANCES.put(21653, 510000);
		CHANCES.put(21387, 530000);
		CHANCES.put(21655, 530000);
		CHANCES.put(21390, 560000);
		CHANCES.put(21656, 690000);
		CHANCES.put(21389, 550000);
		CHANCES.put(21388, 530000);
		CHANCES.put(21383, 510000);
		CHANCES.put(21392, 560000);
		CHANCES.put(21382, 600000);
		CHANCES.put(21654, 520000);
		CHANCES.put(21384, 640000);
		CHANCES.put(21394, 510000);
		CHANCES.put(21395, 560000);
		CHANCES.put(21385, 520000);
		CHANCES.put(21391, 550000);
		CHANCES.put(21393, 580000);
		CHANCES.put(21657, 570000);
		CHANCES.put(21386, 520000);
		CHANCES.put(21652, 490000);
		CHANCES.put(21378, 490000);
		CHANCES.put(21376, 480000);
		CHANCES.put(21377, 480000);
		CHANCES.put(21379, 590000);
		CHANCES.put(21380, 490000);
	}
	
	// Rewards
	private static final int[] REWARD =
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
	
	public Q00617_GatherTheFlames()
	{
		super(617, "Gather the Flames");
		registerQuestItems(TORCH);
		addStartNpc(VULCAN, HILDA);
		addTalkId(VULCAN, HILDA, ROONEY);
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
		
		if (event.equals("31539-03.htm") || event.equals("31271-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31539-05.htm"))
		{
			if (getQuestItemsCount(player, TORCH) >= 1000)
			{
				htmltext = "31539-07.htm";
				takeItems(player, TORCH, 1000);
				giveItems(player, REWARD[getRandom(REWARD.length)], 1);
			}
		}
		else if (event.equals("31539-08.htm"))
		{
			takeItems(player, TORCH, -1);
			st.exitQuest(true, true);
		}
		else if (StringUtil.isNumeric(event))
		{
			if (getQuestItemsCount(player, TORCH) >= 1200)
			{
				htmltext = "32049-03.htm";
				takeItems(player, TORCH, 1200);
				giveItems(player, Integer.parseInt(event), 1);
			}
			else
			{
				htmltext = "32049-02.htm";
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
				htmltext = npc.getId() + ((player.getLevel() >= 74) ? "-01.htm" : "-02.htm");
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case VULCAN:
					{
						htmltext = (getQuestItemsCount(player, TORCH) >= 1000) ? "31539-04.htm" : "31539-05.htm";
						break;
					}
					case HILDA:
					{
						htmltext = "31271-04.htm";
						break;
					}
					case ROONEY:
					{
						htmltext = (getQuestItemsCount(player, TORCH) >= 1200) ? "32049-01.htm" : "32049-02.htm";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMemberState(killer, State.STARTED);
		if (partyMember == null)
		{
			return;
		}
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			giveItemRandomly(partyMember, npc, TORCH, 1, 0, 1, true);
		}
	}
}
