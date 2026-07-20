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
package quests.Q00369_CollectorOfJewels;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00369_CollectorOfJewels extends Quest
{
	// NPC
	private static final int NELL = 30376;
	
	// Items
	private static final int FLARE_SHARD = 5882;
	private static final int FREEZING_SHARD = 5883;
	
	// Reward
	private static final int ADENA = 57;
	
	// Droplist
	private static final Map<Integer, ItemChanceHolder> MOBS_DROP_CHANCES = new HashMap<>();
	static
	{
		MOBS_DROP_CHANCES.put(20609, new ItemChanceHolder(FLARE_SHARD, 75, 1)); // salamander_lakin
		MOBS_DROP_CHANCES.put(20612, new ItemChanceHolder(FLARE_SHARD, 91, 1)); // salamander_rowin
		MOBS_DROP_CHANCES.put(20749, new ItemChanceHolder(FLARE_SHARD, 100, 2)); // death_fire
		MOBS_DROP_CHANCES.put(20616, new ItemChanceHolder(FREEZING_SHARD, 81, 1)); // undine_lakin
		MOBS_DROP_CHANCES.put(20619, new ItemChanceHolder(FREEZING_SHARD, 87, 1)); // undine_rowin
		MOBS_DROP_CHANCES.put(20747, new ItemChanceHolder(FREEZING_SHARD, 100, 2)); // roxide
	}
	
	public Q00369_CollectorOfJewels()
	{
		super(369, "Collector of Jewels");
		registerQuestItems(FLARE_SHARD, FREEZING_SHARD);
		addStartNpc(NELL);
		addTalkId(NELL);
		addKillId(MOBS_DROP_CHANCES.keySet());
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
			case "30376-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30376-07.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case "30376-08.htm":
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
				htmltext = (player.getLevel() < 25) ? "30376-01.htm" : "30376-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				final int flare = getQuestItemsCount(player, FLARE_SHARD);
				final int freezing = getQuestItemsCount(player, FREEZING_SHARD);
				if (cond == 1)
				{
					htmltext = "30376-04.htm";
				}
				else if ((cond == 2) && (flare >= 50) && (freezing >= 50))
				{
					htmltext = "30376-05.htm";
					st.setCond(3, true);
					takeItems(player, FLARE_SHARD, -1);
					takeItems(player, FREEZING_SHARD, -1);
					rewardItems(player, ADENA, 12500);
				}
				else if (cond == 3)
				{
					htmltext = "30376-09.htm";
				}
				else if ((cond == 4) && (flare >= 200) && (freezing >= 200))
				{
					htmltext = "30376-10.htm";
					takeItems(player, FLARE_SHARD, -1);
					takeItems(player, FREEZING_SHARD, -1);
					rewardItems(player, ADENA, 63500);
					st.exitQuest(true, true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final ItemChanceHolder item = MOBS_DROP_CHANCES.get(npc.getId());
		if (getRandom(100) < item.getChance())
		{
			final Player luckyPlayer = getRandomPartyMember(player, npc);
			if (luckyPlayer != null)
			{
				final QuestState qs = getQuestState(luckyPlayer, false);
				if (qs != null)
				{
					final int itemCount = (qs.isMemoState(1) ? 50 : 200);
					final int cond = (qs.isMemoState(1) ? 2 : 4);
					if (giveItemRandomly(luckyPlayer, npc, item.getId(), item.getCount(), itemCount, 1, true) //
						&& (getQuestItemsCount(luckyPlayer, FLARE_SHARD, FREEZING_SHARD) >= (itemCount * 2)))
					{
						qs.setCond(cond);
					}
				}
			}
		}
	}
}
