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
package quests.Q00258_BringWolfPelts;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00258_BringWolfPelts extends Quest
{
	// Item
	private static final int WOLF_PELT = 702;
	
	// Rewards
	private static final int COTTON_SHIRT = 390;
	private static final int LEATHER_PANTS = 29;
	private static final int LEATHER_SHIRT = 22;
	private static final int SHORT_LEATHER_GLOVES = 1119;
	private static final int TUNIC = 426;
	
	public Q00258_BringWolfPelts()
	{
		super(258, "Bring Wolf Pelts");
		registerQuestItems(WOLF_PELT);
		addStartNpc(30001); // Lector
		addTalkId(30001);
		addKillId(20120, 20442); // Wolf, Elder Wolf
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
		
		if (event.equals("30001-03.htm"))
		{
			st.startQuest();
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
				htmltext = (player.getLevel() < 3) ? "30001-01.htm" : "30001-02.htm";
				break;
			}
			case State.STARTED:
			{
				if (getQuestItemsCount(player, WOLF_PELT) < 40)
				{
					htmltext = "30001-05.htm";
				}
				else
				{
					takeItems(player, WOLF_PELT, -1);
					final int randomNumber = getRandom(16);
					
					// Reward is based on a random number (1D16).
					if (randomNumber == 0)
					{
						giveItems(player, COTTON_SHIRT, 1);
					}
					else if (randomNumber < 6)
					{
						giveItems(player, LEATHER_PANTS, 1);
					}
					else if (randomNumber < 9)
					{
						giveItems(player, LEATHER_SHIRT, 1);
					}
					else if (randomNumber < 13)
					{
						giveItems(player, SHORT_LEATHER_GLOVES, 1);
					}
					else
					{
						giveItems(player, TUNIC, 1);
					}
					
					htmltext = "30001-06.htm";
					
					if (randomNumber == 0)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_JACKPOT);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
					}
					
					st.exitQuest(true);
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
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		giveItems(player, WOLF_PELT, 1);
		if (getQuestItemsCount(player, WOLF_PELT) >= 40)
		{
			st.setCond(2, true);
		}
		else
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
