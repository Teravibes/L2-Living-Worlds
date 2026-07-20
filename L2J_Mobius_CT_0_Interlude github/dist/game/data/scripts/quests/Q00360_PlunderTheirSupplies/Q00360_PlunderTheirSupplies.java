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
package quests.Q00360_PlunderTheirSupplies;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.util.LocationUtil;

public class Q00360_PlunderTheirSupplies extends Quest
{
	// Items
	private static final int SUPPLY_ITEM = 5872;
	private static final int SUSPICIOUS_DOCUMENT = 5871;
	private static final int RECIPE_OF_SUPPLY = 5870;
	
	// Drops
	private static final Map<Integer, Integer> MONSTER_DROP_CHANCES = new HashMap<>();
	static
	{
		MONSTER_DROP_CHANCES.put(20666, 50); // Taik Orc Seeker
		MONSTER_DROP_CHANCES.put(20669, 75); // Taik Orc Supply Leader
	}
	
	public Q00360_PlunderTheirSupplies()
	{
		super(360, "Plunder Their Supplies");
		registerQuestItems(RECIPE_OF_SUPPLY, SUPPLY_ITEM, SUSPICIOUS_DOCUMENT);
		addStartNpc(30873); // Coleman
		addTalkId(30873);
		addKillId(MONSTER_DROP_CHANCES.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30873-2.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30873-6.htm"))
		{
			takeItems(player, SUPPLY_ITEM, -1);
			takeItems(player, SUSPICIOUS_DOCUMENT, -1);
			takeItems(player, RECIPE_OF_SUPPLY, -1);
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
				htmltext = (player.getLevel() < 52) ? "30873-0a.htm" : "30873-0.htm";
				break;
			}
			case State.STARTED:
			{
				final int supplyItems = getQuestItemsCount(player, SUPPLY_ITEM);
				if (supplyItems == 0)
				{
					htmltext = "30873-3.htm";
				}
				else
				{
					final int reward = 6000 + (supplyItems * 100) + (getQuestItemsCount(player, RECIPE_OF_SUPPLY) * 6000);
					htmltext = "30873-5.htm";
					takeItems(player, SUPPLY_ITEM, -1);
					takeItems(player, RECIPE_OF_SUPPLY, -1);
					giveAdena(player, reward, true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs == null) || !LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, false))
		{
			return;
		}
		
		if (getRandom(100) < MONSTER_DROP_CHANCES.get(npc.getId()))
		{
			giveItems(killer, SUPPLY_ITEM, 1);
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		
		if (getRandom(100) < 10)
		{
			if (getQuestItemsCount(killer, SUSPICIOUS_DOCUMENT) < 4)
			{
				giveItems(killer, SUSPICIOUS_DOCUMENT, 1);
			}
			else
			{
				giveItems(killer, RECIPE_OF_SUPPLY, 1);
				takeItems(killer, SUSPICIOUS_DOCUMENT, -1);
			}
			
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
