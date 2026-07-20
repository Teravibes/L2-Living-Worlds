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
package quests.Q00638_SeekersOfTheHolyGrail;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00638_SeekersOfTheHolyGrail extends Quest
{
	// NPC
	private static final int INNOCENTIN = 31328;
	
	// Item
	private static final int PAGAN_TOTEM = 8068;
	
	public Q00638_SeekersOfTheHolyGrail()
	{
		super(638, "Seekers of the Holy Grail");
		registerQuestItems(PAGAN_TOTEM);
		addStartNpc(INNOCENTIN);
		addTalkId(INNOCENTIN);
		for (int i = 22138; i < 22175; i++)
		{
			addKillId(i);
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
		
		if (event.equals("31328-02.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31328-06.htm"))
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
				htmltext = (player.getLevel() < 73) ? "31328-00.htm" : "31328-01.htm";
				break;
			}
			case State.STARTED:
			{
				if (getQuestItemsCount(player, PAGAN_TOTEM) >= 2000)
				{
					htmltext = "31328-03.htm";
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					takeItems(player, PAGAN_TOTEM, 2000);
					
					final int chance = getRandom(3);
					if (chance == 0)
					{
						rewardItems(player, 959, 1);
					}
					else if (chance == 1)
					{
						rewardItems(player, 960, 1);
					}
					else
					{
						giveAdena(player, 3576000, true);
					}
				}
				else
				{
					htmltext = "31328-04.htm";
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
		if ((qs != null))
		{
			giveItemRandomly(qs.getPlayer(), npc, PAGAN_TOTEM, 1, 0, 1, true);
		}
	}
}
