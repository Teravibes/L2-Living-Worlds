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
package quests.Q00353_PowerOfDarkness;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00353_PowerOfDarkness extends Quest
{
	// Item
	private static final int STONE = 5862;
	
	public Q00353_PowerOfDarkness()
	{
		super(353, "Power of Darkness");
		registerQuestItems(STONE);
		addStartNpc(31044); // Galman
		addTalkId(31044);
		addKillId(20244, 20245, 20283, 20284);
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
		
		if (event.equals("31044-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31044-08.htm"))
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
				htmltext = (player.getLevel() < 55) ? "31044-01.htm" : "31044-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int stones = getQuestItemsCount(player, STONE);
				if (stones == 0)
				{
					htmltext = "31044-05.htm";
				}
				else
				{
					htmltext = "31044-06.htm";
					takeItems(player, STONE, -1);
					giveAdena(player, 2500 + (230 * stones), true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final QuestState st = getQuestState(killer, false);
		if (st == null)
		{
			return;
		}
		
		if (st.isCond(1) && (Rnd.get(100) < ((npc.getId() == 20244) || (npc.getId() == 20283) ? 48 : 50)))
		{
			giveItems(killer, STONE, 1);
		}
	}
}
