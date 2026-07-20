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
package quests.Q00650_ABrokenDream;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00117_TheOceanOfDistantStars.Q00117_TheOceanOfDistantStars;

public class Q00650_ABrokenDream extends Quest
{
	// NPC
	private static final int GHOST = 32054;
	
	// Monsters
	private static final int CREWMAN = 22027;
	private static final int VAGABOND = 22028;
	
	// Item
	private static final int DREAM_FRAGMENT = 8514;
	
	public Q00650_ABrokenDream()
	{
		super(650, "A Broken Dream");
		registerQuestItems(DREAM_FRAGMENT);
		addStartNpc(GHOST);
		addTalkId(GHOST);
		addKillId(CREWMAN, VAGABOND);
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
			case "32054-01a.htm":
			{
				st.startQuest();
				break;
			}
			case "32054-03.htm":
			{
				if (!hasQuestItems(player, DREAM_FRAGMENT))
				{
					htmltext = "32054-04.htm";
				}
				break;
			}
			case "32054-05.htm":
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
				final QuestState st2 = player.getQuestState(Q00117_TheOceanOfDistantStars.class.getSimpleName());
				if ((st2 != null) && st2.isCompleted() && (player.getLevel() >= 39))
				{
					htmltext = "32054-01.htm";
				}
				else
				{
					htmltext = "32054-00.htm";
					st.exitQuest(true);
				}
				break;
			}
			case State.STARTED:
			{
				htmltext = "32054-02.htm";
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
		
		if (getRandom(100) < 25)
		{
			giveItems(player, DREAM_FRAGMENT, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
