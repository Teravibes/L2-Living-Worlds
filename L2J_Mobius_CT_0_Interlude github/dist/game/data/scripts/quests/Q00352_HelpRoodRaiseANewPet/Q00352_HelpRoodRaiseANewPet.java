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
package quests.Q00352_HelpRoodRaiseANewPet;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00352_HelpRoodRaiseANewPet extends Quest
{
	// Items
	private static final int LIENRIK_EGG_1 = 5860;
	private static final int LIENRIK_EGG_2 = 5861;
	
	public Q00352_HelpRoodRaiseANewPet()
	{
		super(352, "Help Rood Raise A New Pet!");
		registerQuestItems(LIENRIK_EGG_1, LIENRIK_EGG_2);
		addStartNpc(31067); // Rood
		addTalkId(31067);
		addKillId(20786, 20787, 21644, 21645);
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
		
		if (event.equals("31067-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31067-09.htm"))
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
				htmltext = (player.getLevel() < 39) ? "31067-00.htm" : "31067-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int eggs1 = getQuestItemsCount(player, LIENRIK_EGG_1);
				final int eggs2 = getQuestItemsCount(player, LIENRIK_EGG_2);
				if ((eggs1 + eggs2) == 0)
				{
					htmltext = "31067-05.htm";
				}
				else
				{
					int reward = 2000;
					if ((eggs1 > 0) && (eggs2 == 0))
					{
						htmltext = "31067-06.htm";
						reward += eggs1 * 34;
						takeItems(player, LIENRIK_EGG_1, -1);
						giveAdena(player, reward, true);
					}
					else if ((eggs1 == 0) && (eggs2 > 0))
					{
						htmltext = "31067-08.htm";
						reward += eggs2 * 1025;
						takeItems(player, LIENRIK_EGG_2, -1);
						giveAdena(player, reward, true);
					}
					else if ((eggs1 > 0) && (eggs2 > 0))
					{
						htmltext = "31067-08.htm";
						reward += (eggs1 * 34) + (eggs2 * 1025) + 2000;
						takeItems(player, LIENRIK_EGG_1, -1);
						takeItems(player, LIENRIK_EGG_2, -1);
						giveAdena(player, reward, true);
					}
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
		
		final int npcId = npc.getId();
		final int random = getRandom(100);
		final int chance = ((npcId == 20786) || (npcId == 21644)) ? 44 : 58;
		if (random < chance)
		{
			giveItems(player, LIENRIK_EGG_1, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else if (random < (chance + 4))
		{
			giveItems(player, LIENRIK_EGG_2, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
