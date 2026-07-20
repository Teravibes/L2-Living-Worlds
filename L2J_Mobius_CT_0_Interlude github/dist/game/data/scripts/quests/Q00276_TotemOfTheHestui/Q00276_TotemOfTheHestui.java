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
package quests.Q00276_TotemOfTheHestui;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00276_TotemOfTheHestui extends Quest
{
	// Items
	private static final int KASHA_PARASITE = 1480;
	private static final int KASHA_CRYSTAL = 1481;
	
	// Rewards
	private static final int HESTUI_TOTEM = 1500;
	private static final int LEATHER_PANTS = 29;
	
	public Q00276_TotemOfTheHestui()
	{
		super(276, "Totem of the Hestui");
		registerQuestItems(KASHA_PARASITE, KASHA_CRYSTAL);
		addStartNpc(30571); // Tanapi
		addTalkId(30571);
		addKillId(20479, 27044);
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
		
		if (event.equals("30571-03.htm"))
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
					htmltext = "30571-00.htm";
				}
				else if (player.getLevel() < 15)
				{
					htmltext = "30571-01.htm";
				}
				else
				{
					htmltext = "30571-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1))
				{
					htmltext = "30571-04.htm";
				}
				else
				{
					htmltext = "30571-05.htm";
					takeItems(player, KASHA_CRYSTAL, -1);
					takeItems(player, KASHA_PARASITE, -1);
					giveItems(player, HESTUI_TOTEM, 1);
					giveItems(player, LEATHER_PANTS, 1);
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
		
		if (!hasQuestItems(player, KASHA_CRYSTAL))
		{
			switch (npc.getId())
			{
				case 20479:
				{
					final int count = getQuestItemsCount(player, KASHA_PARASITE);
					final int random = getRandom(100);
					if ((count >= 79) || ((count >= 69) && (random <= 20)) || ((count >= 59) && (random <= 15)) || ((count >= 49) && (random <= 10)) || ((count >= 39) && (random < 2)))
					{
						addSpawn(27044, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
						takeItems(player, KASHA_PARASITE, count);
					}
					else
					{
						giveItems(player, KASHA_PARASITE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case 27044:
				{
					st.setCond(2, true);
					giveItems(player, KASHA_CRYSTAL, 1);
					break;
				}
			}
		}
	}
}
