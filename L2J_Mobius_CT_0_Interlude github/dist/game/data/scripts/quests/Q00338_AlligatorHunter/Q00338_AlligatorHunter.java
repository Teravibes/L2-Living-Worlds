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
package quests.Q00338_AlligatorHunter;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00338_AlligatorHunter extends Quest
{
	// Item
	private static final int ALLIGATOR_PELT = 4337;
	
	public Q00338_AlligatorHunter()
	{
		super(338, "Alligator Hunter");
		registerQuestItems(ALLIGATOR_PELT);
		addStartNpc(30892); // Enverun
		addTalkId(30892);
		addKillId(20135); // Alligator
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
			case "30892-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30892-05.htm":
			{
				final int pelts = getQuestItemsCount(player, ALLIGATOR_PELT);
				int reward = pelts * 40;
				if (pelts > 10)
				{
					reward += 3430;
				}
				
				takeItems(player, ALLIGATOR_PELT, -1);
				giveAdena(player, reward, true);
				break;
			}
			case "30892-08.htm":
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
				htmltext = (player.getLevel() < 40) ? "30892-00.htm" : "30892-01.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (hasQuestItems(player, ALLIGATOR_PELT)) ? "30892-03.htm" : "30892-04.htm";
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
		
		giveItems(player, ALLIGATOR_PELT, 1);
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
	}
}
