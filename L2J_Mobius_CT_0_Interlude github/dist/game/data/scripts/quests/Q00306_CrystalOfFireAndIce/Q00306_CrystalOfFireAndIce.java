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
package quests.Q00306_CrystalOfFireAndIce;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00306_CrystalOfFireAndIce extends Quest
{
	// Items
	private static final int FLAME_SHARD = 1020;
	private static final int ICE_SHARD = 1021;
	
	// Droplist (npcId, itemId, chance)
	private static final int[][] DROPLIST =
	{
		// @formatter:off
		{20109, FLAME_SHARD, 300000},
		{20110, ICE_SHARD, 300000},
		{20112, FLAME_SHARD, 400000},
		{20113, ICE_SHARD, 400000},
		{20114, FLAME_SHARD, 500000},
		{20115, ICE_SHARD, 500000}
		// @formatter:on
	};
	
	public Q00306_CrystalOfFireAndIce()
	{
		super(306, "Crystals of Fire and Ice");
		registerQuestItems(FLAME_SHARD, ICE_SHARD);
		addStartNpc(30004); // Katerina
		addTalkId(30004);
		addKillId(20109, 20110, 20112, 20113, 20114, 20115);
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
		
		if (event.equals("30004-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30004-06.htm"))
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
				htmltext = (player.getLevel() < 17) ? "30004-01.htm" : "30004-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int totalItems = getQuestItemsCount(player, FLAME_SHARD) + getQuestItemsCount(player, ICE_SHARD);
				if (totalItems == 0)
				{
					htmltext = "30004-04.htm";
				}
				else
				{
					htmltext = "30004-05.htm";
					takeItems(player, FLAME_SHARD, -1);
					takeItems(player, ICE_SHARD, -1);
					
					int reward = totalItems * 60;
					if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD && (totalItems >= 10))
					{
						reward += 5000;
					}
					
					giveAdena(player, reward, true);
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
		
		for (int[] drop : DROPLIST)
		{
			if (npc.getId() == drop[0])
			{
				if (getRandom(1000000) < drop[2])
				{
					giveItems(player, drop[1], 1);
				}
				break;
			}
		}
	}
}
