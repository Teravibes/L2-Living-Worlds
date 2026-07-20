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
package quests.Q00626_ADarkTwilight;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00626_ADarkTwilight extends Quest
{
	// NPC
	private static final int HIERARCH = 31517;
	
	// Items
	private static final int BLOOD_OF_SAINT = 7169;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(21520, 533000);
		CHANCES.put(21523, 566000);
		CHANCES.put(21524, 603000);
		CHANCES.put(21525, 603000);
		CHANCES.put(21526, 587000);
		CHANCES.put(21529, 606000);
		CHANCES.put(21530, 560000);
		CHANCES.put(21531, 669000);
		CHANCES.put(21532, 651000);
		CHANCES.put(21535, 672000);
		CHANCES.put(21536, 597000);
		CHANCES.put(21539, 739000);
		CHANCES.put(21540, 739000);
		CHANCES.put(21658, 669000);
	}
	
	public Q00626_ADarkTwilight()
	{
		super(626, "A Dark Twilight");
		registerQuestItems(BLOOD_OF_SAINT);
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH);
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
			case "31517-03.htm":
			{
				st.startQuest();
				break;
			}
			case "reward1":
			{
				if (getQuestItemsCount(player, BLOOD_OF_SAINT) == 300)
				{
					htmltext = "31517-07.htm";
					takeItems(player, BLOOD_OF_SAINT, 300);
					addExpAndSp(player, 162773, 12500);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "31517-08.htm";
				}
				break;
			}
			case "reward2":
			{
				if (getQuestItemsCount(player, BLOOD_OF_SAINT) == 300)
				{
					htmltext = "31517-07.htm";
					takeItems(player, BLOOD_OF_SAINT, 300);
					giveAdena(player, 100000, true);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "31517-08.htm";
				}
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
				htmltext = (player.getLevel() < 60) ? "31517-02.htm" : "31517-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "31517-05.htm";
				}
				else
				{
					htmltext = "31517-04.htm";
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			giveItems(player, BLOOD_OF_SAINT, 1);
			if (getQuestItemsCount(player, BLOOD_OF_SAINT) < 300)
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(2, true);
			}
		}
	}
}
