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
package quests.Q00328_SenseForBusiness;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00328_SenseForBusiness extends Quest
{
	// Items
	private static final int MONSTER_EYE_LENS = 1366;
	private static final int MONSTER_EYE_CARCASS = 1347;
	private static final int BASILISK_GIZZARD = 1348;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(20055, 48);
		CHANCES.put(20059, 52);
		CHANCES.put(20067, 68);
		CHANCES.put(20068, 76);
		CHANCES.put(20070, 500000);
		CHANCES.put(20072, 510000);
	}
	
	public Q00328_SenseForBusiness()
	{
		super(328, "Sense for Business");
		registerQuestItems(MONSTER_EYE_LENS, MONSTER_EYE_CARCASS, BASILISK_GIZZARD);
		addStartNpc(30436); // Sarien
		addTalkId(30436);
		addKillId(20055, 20059, 20067, 20068, 20070, 20072);
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
		
		if (event.equals("30436-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30436-06.htm"))
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
				htmltext = (player.getLevel() < 21) ? "30436-01.htm" : "30436-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int carcasses = getQuestItemsCount(player, MONSTER_EYE_CARCASS);
				final int lenses = getQuestItemsCount(player, MONSTER_EYE_LENS);
				final int gizzards = getQuestItemsCount(player, BASILISK_GIZZARD);
				final int all = carcasses + lenses + gizzards;
				if (all == 0)
				{
					htmltext = "30436-04.htm";
				}
				else
				{
					htmltext = "30436-05.htm";
					takeItems(player, MONSTER_EYE_CARCASS, -1);
					takeItems(player, MONSTER_EYE_LENS, -1);
					takeItems(player, BASILISK_GIZZARD, -1);
					giveAdena(player, (30 * carcasses) + (2000 * lenses) + (75 * gizzards) + ((all >= 10) ? 618 : 0), true);
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
		final int chance = CHANCES.get(npcId);
		if (npcId < 20069)
		{
			final int rnd = getRandom(100);
			if (rnd < (chance + 1))
			{
				giveItems(player, rnd < chance ? MONSTER_EYE_CARCASS : MONSTER_EYE_LENS, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		else if (getRandom(1000000) < chance)
		{
			giveItems(player, BASILISK_GIZZARD, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
