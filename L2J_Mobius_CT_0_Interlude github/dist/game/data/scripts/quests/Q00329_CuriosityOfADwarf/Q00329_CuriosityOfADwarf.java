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
package quests.Q00329_CuriosityOfADwarf;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00329_CuriosityOfADwarf extends Quest
{
	// Items
	private static final int GOLEM_HEARTSTONE = 1346;
	private static final int BROKEN_HEARTSTONE = 1365;
	
	public Q00329_CuriosityOfADwarf()
	{
		super(329, "Curiosity of a Dwarf");
		addStartNpc(30437); // Rolento
		addTalkId(30437);
		addKillId(20083, 20085); // Granite golem, Puncher
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
		
		if (event.equals("30437-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30437-06.htm"))
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
				htmltext = (player.getLevel() < 33) ? "30437-01.htm" : "30437-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int golem = getQuestItemsCount(player, GOLEM_HEARTSTONE);
				final int broken = getQuestItemsCount(player, BROKEN_HEARTSTONE);
				if ((golem + broken) == 0)
				{
					htmltext = "30437-04.htm";
				}
				else
				{
					htmltext = "30437-05.htm";
					takeItems(player, GOLEM_HEARTSTONE, -1);
					takeItems(player, BROKEN_HEARTSTONE, -1);
					giveAdena(player, (broken * 50) + (golem * 1000) + (((golem + broken) > 10) ? 1183 : 0), true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		final int chance = getRandom(100);
		if (chance < 2)
		{
			giveItems(player, GOLEM_HEARTSTONE, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if (chance < ((npc.getId() == 20083) ? 44 : 50))
		{
			giveItems(player, BROKEN_HEARTSTONE, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
