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
package quests.Q00640_TheZeroHour;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00109_InSearchOfTheNest.Q00109_InSearchOfTheNest;

public class Q00640_TheZeroHour extends Quest
{
	// NPC
	private static final int KAHMAN = 31554;
	
	// Item
	private static final int FANG_OF_STAKATO = 8085;
	private static final int[][] REWARDS =
	{
		// @formatter:off
		{12, 4042, 1},
		{6, 4043, 1},
		{6, 4044, 1},
		{81, 1887, 10},
		{33, 1888, 5},
		{30, 1889, 10},
		{150, 5550, 10},
		{131, 1890, 10},
		{123, 1893, 5}
		// @formatter:on
	};
	
	public Q00640_TheZeroHour()
	{
		super(640, "The Zero Hour");
		registerQuestItems(FANG_OF_STAKATO);
		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);
		
		// All "spiked" stakatos types, except babies and cannibalistic followers.
		addKillId(22105, 22106, 22107, 22108, 22109, 22110, 22111, 22113, 22114, 22115, 22116, 22117, 22118, 22119, 22121);
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
		
		if (event.equals("31554-02.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31554-05.htm"))
		{
			if (!hasQuestItems(player, FANG_OF_STAKATO))
			{
				htmltext = "31554-06.htm";
			}
		}
		else if (event.equals("31554-08.htm"))
		{
			st.exitQuest(true, true);
		}
		else if (StringUtil.isNumeric(event))
		{
			final int[] reward = REWARDS[Integer.parseInt(event)];
			if (getQuestItemsCount(player, FANG_OF_STAKATO) >= reward[0])
			{
				htmltext = "31554-09.htm";
				takeItems(player, FANG_OF_STAKATO, reward[0]);
				rewardItems(player, reward[1], reward[2]);
			}
			else
			{
				htmltext = "31554-06.htm";
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
				if (player.getLevel() < 66)
				{
					htmltext = "31554-00.htm";
				}
				else
				{
					final QuestState st2 = player.getQuestState(Q00109_InSearchOfTheNest.class.getSimpleName());
					htmltext = ((st2 != null) && st2.isCompleted()) ? "31554-01.htm" : "31554-10.htm";
				}
				break;
			}
			case State.STARTED:
			{
				htmltext = (hasQuestItems(player, FANG_OF_STAKATO)) ? "31554-04.htm" : "31554-03.htm";
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 1, 3, npc);
		if (qs != null)
		{
			giveItemRandomly(qs.getPlayer(), npc, FANG_OF_STAKATO, 1, 0, 1, true);
		}
	}
}
