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
package quests.Q00354_ConquestOfAlligatorIsland;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00354_ConquestOfAlligatorIsland extends Quest
{
	// Items
	private static final int ALLIGATOR_TOOTH = 5863;
	private static final int TORN_MAP_FRAGMENT = 5864;
	private static final int PIRATE_TREASURE_MAP = 5915;
	
	// Drops
	private static final Map<Integer, Double> MOB1 = new HashMap<>();
	private static final Map<Integer, Integer> MOB2 = new HashMap<>();
	static
	{
		MOB1.put(20804, 0.84); // crokian_lad
		MOB1.put(20805, 0.91); // dailaon_lad
		MOB1.put(20806, 0.88); // crokian_lad_warrior
		MOB1.put(20807, 0.92); // farhite_lad
		MOB2.put(20808, 14); // nos_lad
		MOB2.put(20991, 69); // tribe_of_swamp
	}
	
	public Q00354_ConquestOfAlligatorIsland()
	{
		super(354, "Conquest of Alligator Island");
		registerQuestItems(ALLIGATOR_TOOTH, TORN_MAP_FRAGMENT);
		addStartNpc(30895); // Kluck
		addTalkId(30895);
		addKillId(MOB1.keySet());
		addKillId(MOB2.keySet());
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
			case "30895-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30895-03.htm":
			{
				if (hasQuestItems(player, TORN_MAP_FRAGMENT))
				{
					htmltext = "30895-03a.htm";
				}
				break;
			}
			case "30895-05.htm":
			{
				final int amount = getQuestItemsCount(player, ALLIGATOR_TOOTH);
				if (amount > 0)
				{
					int reward = (amount * 220) + 3100;
					if (amount >= 100)
					{
						reward += 7600;
						htmltext = "30895-05b.htm";
					}
					else
					{
						htmltext = "30895-05a.htm";
					}
					
					takeItems(player, ALLIGATOR_TOOTH, -1);
					giveAdena(player, reward, true);
				}
				break;
			}
			case "30895-07.htm":
			{
				if (getQuestItemsCount(player, TORN_MAP_FRAGMENT) >= 10)
				{
					htmltext = "30895-08.htm";
					takeItems(player, TORN_MAP_FRAGMENT, 10);
					giveItems(player, PIRATE_TREASURE_MAP, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30895-09.htm":
			{
				st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < 38) ? "30895-00.htm" : "30895-01.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (hasQuestItems(player, TORN_MAP_FRAGMENT)) ? "30895-03a.htm" : "30895-03.htm";
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(player, -1, 3, npc);
		if (qs != null)
		{
			final int npcId = npc.getId();
			if (MOB1.containsKey(npcId))
			{
				giveItemRandomly(player, npc, ALLIGATOR_TOOTH, 1, 0, MOB1.get(npcId), true);
			}
			else
			{
				final int itemCount = ((getRandom(100) < MOB2.get(npcId)) ? 2 : 1);
				giveItemRandomly(player, npc, ALLIGATOR_TOOTH, itemCount, 0, 1, true);
			}
			
			giveItemRandomly(player, npc, TORN_MAP_FRAGMENT, 1, 0, 0.1, false);
		}
	}
}
