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
package quests.Q00264_KeenClaws;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00264_KeenClaws extends Quest
{
	// Item
	private static final int WOLF_CLAW = 1367;
	
	// Rewards
	private static final int LEATHER_SANDALS = 36;
	private static final int WOODEN_HELMET = 43;
	private static final int STOCKINGS = 462;
	private static final int HEALING_POTION = 1061;
	private static final int SHORT_GLOVES = 48;
	private static final int CLOTH_SHOES = 35;
	
	public Q00264_KeenClaws()
	{
		super(264, "Keen Claws");
		registerQuestItems(WOLF_CLAW);
		addStartNpc(30136); // Payne
		addTalkId(30136);
		addKillId(20003, 20456); // Goblin, Wolf
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
		
		if (event.equals("30136-03.htm"))
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
				htmltext = (player.getLevel() < 3) ? "30136-01.htm" : "30136-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int count = getQuestItemsCount(player, WOLF_CLAW);
				if (count < 50)
				{
					htmltext = "30136-04.htm";
				}
				else
				{
					htmltext = "30136-05.htm";
					takeItems(player, WOLF_CLAW, -1);
					
					final int n = getRandom(17);
					if (n == 0)
					{
						giveItems(player, WOODEN_HELMET, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_JACKPOT);
					}
					else if (n < 2)
					{
						giveAdena(player, 1000, true);
					}
					else if (n < 5)
					{
						giveItems(player, LEATHER_SANDALS, 1);
					}
					else if (n < 8)
					{
						giveItems(player, STOCKINGS, 1);
						giveAdena(player, 50, true);
					}
					else if (n < 11)
					{
						giveItems(player, HEALING_POTION, 1);
					}
					else if (n < 14)
					{
						giveItems(player, SHORT_GLOVES, 1);
					}
					else
					{
						giveItems(player, CLOTH_SHOES, 1);
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
		
		giveItems(player, WOLF_CLAW, (getRandomBoolean() ? 1 : 2) * ((npc.getId() == 20003) ? 2 : 1));
		if (getQuestItemsCount(player, WOLF_CLAW) >= 50)
		{
			st.setCond(2, true);
		}
		else
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
