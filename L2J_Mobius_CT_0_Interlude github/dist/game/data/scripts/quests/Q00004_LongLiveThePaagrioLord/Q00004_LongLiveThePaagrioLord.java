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
package quests.Q00004_LongLiveThePaagrioLord;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00004_LongLiveThePaagrioLord extends Quest
{
	private static final int NAKUSIN = 30578;
	private static final Map<Integer, Integer> NPC_GIFTS = new HashMap<>();
	static
	{
		NPC_GIFTS.put(30585, 1542);
		NPC_GIFTS.put(30566, 1541);
		NPC_GIFTS.put(30562, 1543);
		NPC_GIFTS.put(30560, 1544);
		NPC_GIFTS.put(30559, 1545);
		NPC_GIFTS.put(30587, 1546);
	}
	
	public Q00004_LongLiveThePaagrioLord()
	{
		super(4, "Long live the Pa'agrio Lord!");
		registerQuestItems(1541, 1542, 1543, 1544, 1545, 1546);
		addStartNpc(30578); // Nakusin
		addTalkId(30578, 30585, 30566, 30562, 30560, 30559, 30587);
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
		
		if (event.equals("30578-03.htm"))
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
				if (player.getRace() != Race.ORC)
				{
					htmltext = "30578-00.htm";
				}
				else if (player.getLevel() < 2)
				{
					htmltext = "30578-01.htm";
				}
				else
				{
					htmltext = "30578-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				final int npcId = npc.getId();
				if (npcId == NAKUSIN)
				{
					if (cond == 1)
					{
						htmltext = "30578-04.htm";
					}
					else if (cond == 2)
					{
						htmltext = "30578-06.htm";
						giveItems(player, 4, 1);
						for (int item : NPC_GIFTS.values())
						{
							takeItems(player, item, -1);
						}
						
						st.exitQuest(false, true);
					}
				}
				else
				{
					final int i = NPC_GIFTS.get(npcId);
					if (hasQuestItems(player, i))
					{
						htmltext = "30585-02.htm";
					}
					else
					{
						giveItems(player, i, 1);
						htmltext = "30585-01.htm";
						
						int count = 0;
						for (int item : NPC_GIFTS.values())
						{
							count += getQuestItemsCount(player, item);
						}
						
						if (count == 6)
						{
							st.setCond(2, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
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
}
