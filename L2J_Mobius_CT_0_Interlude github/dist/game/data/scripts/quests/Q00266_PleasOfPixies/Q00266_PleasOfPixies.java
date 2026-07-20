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
package quests.Q00266_PleasOfPixies;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00266_PleasOfPixies extends Quest
{
	// Items
	private static final int PREDATOR_FANG = 1334;
	
	// Rewards
	private static final int GLASS_SHARD = 1336;
	private static final int EMERALD = 1337;
	private static final int BLUE_ONYX = 1338;
	private static final int ONYX = 1339;
	
	public Q00266_PleasOfPixies()
	{
		super(266, "Pleas of Pixies");
		registerQuestItems(PREDATOR_FANG);
		addStartNpc(31852); // Murika
		addTalkId(31852);
		addKillId(20525, 20530, 20534, 20537);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("31852-03.htm"))
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
				if (player.getRace() != Race.ELF)
				{
					htmltext = "31852-00.htm";
				}
				else if (player.getLevel() < 3)
				{
					htmltext = "31852-01.htm";
				}
				else
				{
					htmltext = "31852-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (getQuestItemsCount(player, PREDATOR_FANG) < 100)
				{
					htmltext = "31852-04.htm";
				}
				else
				{
					htmltext = "31852-05.htm";
					takeItems(player, PREDATOR_FANG, -1);
					
					final int n = getRandom(100);
					if (n < 10)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_JACKPOT);
						rewardItems(player, EMERALD, 1);
					}
					else if (n < 30)
					{
						rewardItems(player, BLUE_ONYX, 1);
					}
					else if (n < 60)
					{
						rewardItems(player, ONYX, 1);
					}
					else
					{
						rewardItems(player, GLASS_SHARD, 1);
					}
					
					st.exitQuest(true, true);
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
		
		switch (npc.getId())
		{
			case 20525:
			{
				giveItems(player, PREDATOR_FANG, getRandom(2, 3));
				if (getQuestItemsCount(player, PREDATOR_FANG) < 100)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				else
				{
					st.setCond(2, true);
				}
				break;
			}
			case 20530:
			{
				if (getRandom(10) < 8)
				{
					giveItems(player, PREDATOR_FANG, 1);
					if (getQuestItemsCount(player, PREDATOR_FANG) < 100)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(2, true);
					}
				}
				break;
			}
			case 20534:
			{
				if (getRandom(10) < 6)
				{
					giveItems(player, PREDATOR_FANG, getRandom(3) == 0 ? 1 : 2);
					if (getQuestItemsCount(player, PREDATOR_FANG) < 100)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(2, true);
					}
				}
				break;
			}
			case 20537:
			{
				giveItems(player, PREDATOR_FANG, 2);
				if (getQuestItemsCount(player, PREDATOR_FANG) < 100)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				else
				{
					st.setCond(2, true);
				}
				break;
			}
		}
	}
}
