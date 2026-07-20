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
package quests.Q00331_ArrowOfVengeance;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00331_ArrowOfVengeance extends Quest
{
	// Items
	private static final int HARPY_FEATHER = 1452;
	private static final int MEDUSA_VENOM = 1453;
	private static final int WYRM_TOOTH = 1454;
	
	public Q00331_ArrowOfVengeance()
	{
		super(331, "Arrow of Vengeance");
		registerQuestItems(HARPY_FEATHER, MEDUSA_VENOM, WYRM_TOOTH);
		addStartNpc(30125); // Belton
		addTalkId(30125);
		addKillId(20145, 20158, 20176);
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
		
		if (event.equals("30125-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30125-06.htm"))
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
				htmltext = (player.getLevel() < 32) ? "30125-01.htm" : "30125-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int harpyFeather = getQuestItemsCount(player, HARPY_FEATHER);
				final int medusaVenom = getQuestItemsCount(player, MEDUSA_VENOM);
				final int wyrmTooth = getQuestItemsCount(player, WYRM_TOOTH);
				
				if ((harpyFeather + medusaVenom + wyrmTooth) > 0)
				{
					htmltext = "30125-05.htm";
					takeItems(player, HARPY_FEATHER, -1);
					takeItems(player, MEDUSA_VENOM, -1);
					takeItems(player, WYRM_TOOTH, -1);
					
					int reward = (harpyFeather * 80) + (medusaVenom * 90) + (wyrmTooth * 100);
					if ((harpyFeather + medusaVenom + wyrmTooth) > 10)
					{
						reward += 3100;
					}
					
					giveAdena(player, reward, true);
				}
				else
				{
					htmltext = "30125-04.htm";
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
		
		if (getRandomBoolean())
		{
			switch (npc.getId())
			{
				case 20145:
				{
					giveItems(player, HARPY_FEATHER, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					break;
				}
				case 20158:
				{
					giveItems(player, MEDUSA_VENOM, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					break;
				}
				case 20176:
				{
					giveItems(player, WYRM_TOOTH, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					break;
				}
			}
		}
	}
}
