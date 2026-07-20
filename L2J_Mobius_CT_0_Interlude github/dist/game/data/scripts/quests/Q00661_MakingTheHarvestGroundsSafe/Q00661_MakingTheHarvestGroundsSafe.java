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
package quests.Q00661_MakingTheHarvestGroundsSafe;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00661_MakingTheHarvestGroundsSafe extends Quest
{
	// NPC
	private static final int NORMAN = 30210;
	
	// Monsters
	private static final int GIANT_POISON_BEE = 21095;
	private static final int CLOUDY_BEAST = 21096;
	private static final int YOUNG_ARANEID = 21097;
	
	// Items
	private static final int STING_OF_GIANT_POISON_BEE = 8283;
	private static final int CLOUDY_GEM = 8284;
	private static final int TALON_OF_YOUNG_ARANEID = 8285;
	
	// Reward
	private static final int ADENA = 57;
	
	public Q00661_MakingTheHarvestGroundsSafe()
	{
		super(661, "Making the Harvest Grounds Safe");
		registerQuestItems(STING_OF_GIANT_POISON_BEE, CLOUDY_GEM, TALON_OF_YOUNG_ARANEID);
		addStartNpc(NORMAN);
		addTalkId(NORMAN);
		addKillId(GIANT_POISON_BEE, CLOUDY_BEAST, YOUNG_ARANEID);
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
			case "30210-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30210-04.htm":
			{
				final int item1 = getQuestItemsCount(player, STING_OF_GIANT_POISON_BEE);
				final int item2 = getQuestItemsCount(player, CLOUDY_GEM);
				final int item3 = getQuestItemsCount(player, TALON_OF_YOUNG_ARANEID);
				int sum = 0;
				sum = (item1 * 57) + (item2 * 56) + (item3 * 60);
				if ((item1 + item2 + item3) >= 10)
				{
					sum += 2871;
				}
				
				takeItems(player, STING_OF_GIANT_POISON_BEE, item1);
				takeItems(player, CLOUDY_GEM, item2);
				takeItems(player, TALON_OF_YOUNG_ARANEID, item3);
				rewardItems(player, ADENA, sum);
				break;
			}
			case "30210-06.htm":
			{
				st.exitQuest(true);
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
				htmltext = (player.getLevel() < 21) ? "30210-01a.htm" : "30210-01.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (hasAtLeastOneQuestItem(player, STING_OF_GIANT_POISON_BEE, CLOUDY_GEM, TALON_OF_YOUNG_ARANEID)) ? "30210-03.htm" : "30210-05.htm";
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
			giveItems(player, npc.getId() - 12812, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
