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
package quests.Q00326_VanquishRemnants;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00326_VanquishRemnants extends Quest
{
	// Items
	private static final int RED_CROSS_BADGE = 1359;
	private static final int BLUE_CROSS_BADGE = 1360;
	private static final int BLACK_CROSS_BADGE = 1361;
	
	// Reward
	private static final int BLACK_LION_MARK = 1369;
	
	public Q00326_VanquishRemnants()
	{
		super(326, "Vanquish Remnants");
		registerQuestItems(RED_CROSS_BADGE, BLUE_CROSS_BADGE, BLACK_CROSS_BADGE);
		addStartNpc(30435); // Leopold
		addTalkId(30435);
		addKillId(20053, 20437, 20058, 20436, 20061, 20439, 20063, 20066, 20438);
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
		
		if (event.equals("30435-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30435-07.htm"))
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
				htmltext = (player.getLevel() < 21) ? "30435-01.htm" : "30435-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int redBadges = getQuestItemsCount(player, RED_CROSS_BADGE);
				final int blueBadges = getQuestItemsCount(player, BLUE_CROSS_BADGE);
				final int blackBadges = getQuestItemsCount(player, BLACK_CROSS_BADGE);
				final int badgesSum = redBadges + blueBadges + blackBadges;
				if (badgesSum > 0)
				{
					takeItems(player, RED_CROSS_BADGE, -1);
					takeItems(player, BLUE_CROSS_BADGE, -1);
					takeItems(player, BLACK_CROSS_BADGE, -1);
					giveAdena(player, ((redBadges * 60) + (blueBadges * 65) + (blackBadges * 70) + ((badgesSum >= 10) ? 4320 : 0)), true);
					if (badgesSum >= 100)
					{
						if (!hasQuestItems(player, BLACK_LION_MARK))
						{
							htmltext = "30435-06.htm";
							giveItems(player, BLACK_LION_MARK, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else
						{
							htmltext = "30435-09.htm";
						}
					}
					else
					{
						htmltext = "30435-05.htm";
					}
				}
				else
				{
					htmltext = "30435-04.htm";
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
		
		switch (npc.getId())
		{
			case 20053:
			case 20437:
			case 20058:
			{
				if (getRandom(100) < 33)
				{
					giveItems(player, RED_CROSS_BADGE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20436:
			case 20061:
			case 20439:
			case 20063:
			{
				if (getRandom(100) < 16)
				{
					giveItems(player, BLUE_CROSS_BADGE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20066:
			case 20438:
			{
				if (getRandom(100) < 12)
				{
					giveItems(player, BLACK_CROSS_BADGE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
