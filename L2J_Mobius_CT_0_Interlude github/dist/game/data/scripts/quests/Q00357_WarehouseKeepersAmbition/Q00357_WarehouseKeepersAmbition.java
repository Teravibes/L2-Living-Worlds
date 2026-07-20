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
package quests.Q00357_WarehouseKeepersAmbition;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00357_WarehouseKeepersAmbition extends Quest
{
	// Monsters
	private static final int FOREST_RUNNER = 20594;
	private static final int FLINE_ELDER = 20595;
	private static final int LIELE_ELDER = 20596;
	private static final int VALLEY_TREANT_ELDER = 20597;
	
	// Item
	private static final int JADE_CRYSTAL = 5867;
	
	// Drop chances
	private static final Map<Integer, Double> DROP_DATA = new HashMap<>();
	static
	{
		DROP_DATA.put(FOREST_RUNNER, 0.577); // Forest Runner
		DROP_DATA.put(FLINE_ELDER, 0.6); // Fline Elder
		DROP_DATA.put(LIELE_ELDER, 0.638); // Liele Elder
		DROP_DATA.put(VALLEY_TREANT_ELDER, 0.062); // Valley Treant Elder
	}
	
	public Q00357_WarehouseKeepersAmbition()
	{
		super(357, "Warehouse Keeper's Ambition");
		registerQuestItems(JADE_CRYSTAL);
		addStartNpc(30686); // Silva
		addTalkId(30686);
		addKillId(FOREST_RUNNER, FLINE_ELDER, LIELE_ELDER, VALLEY_TREANT_ELDER);
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
			case "30686-2.htm":
			{
				st.startQuest();
				break;
			}
			case "30686-7.htm":
			{
				final int count = getQuestItemsCount(player, JADE_CRYSTAL);
				if (count == 0)
				{
					htmltext = "30686-4.htm";
				}
				else
				{
					int reward = (count * 425) + 3500;
					if (count >= 100)
					{
						reward += 7400;
					}
					
					takeItems(player, JADE_CRYSTAL, -1);
					giveAdena(player, reward, true);
				}
				break;
			}
			case "30686-8.htm":
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
				htmltext = (player.getLevel() < 47) ? "30686-0a.htm" : "30686-0.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (!hasQuestItems(player, JADE_CRYSTAL)) ? "30686-4.htm" : "30686-6.htm";
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, -1, 3, npc);
		if (qs != null)
		{
			giveItemRandomly(qs.getPlayer(), npc, JADE_CRYSTAL, 1, 0, DROP_DATA.get(npc.getId()), true);
		}
	}
}
