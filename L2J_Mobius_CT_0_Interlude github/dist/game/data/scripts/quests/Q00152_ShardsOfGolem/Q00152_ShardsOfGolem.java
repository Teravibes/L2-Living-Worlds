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
package quests.Q00152_ShardsOfGolem;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00152_ShardsOfGolem extends Quest
{
	// NPCs
	private static final int HARRIS = 30035;
	private static final int ALTRAN = 30283;
	
	// Monster
	private static final int STONE_GOLEM = 20016;
	
	// Items
	private static final int HARRIS_RECEIPT_1 = 1008;
	private static final int HARRIS_RECEIPT_2 = 1009;
	private static final int GOLEM_SHARD = 1010;
	private static final int TOOL_BOX = 1011;
	
	// Reward
	private static final int WOODEN_BREASTPLATE = 23;
	
	public Q00152_ShardsOfGolem()
	{
		super(152, "Shards of Golem");
		registerQuestItems(HARRIS_RECEIPT_1, HARRIS_RECEIPT_2, GOLEM_SHARD, TOOL_BOX);
		addStartNpc(HARRIS);
		addTalkId(HARRIS, ALTRAN);
		addKillId(STONE_GOLEM);
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
		
		if (event.equals("30035-02.htm"))
		{
			st.startQuest();
			giveItems(player, HARRIS_RECEIPT_1, 1);
		}
		else if (event.equals("30283-02.htm"))
		{
			st.setCond(2, true);
			takeItems(player, HARRIS_RECEIPT_1, 1);
			giveItems(player, HARRIS_RECEIPT_2, 1);
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
				htmltext = (player.getLevel() < 10) ? "30035-01a.htm" : "30035-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case HARRIS:
					{
						if (cond < 4)
						{
							htmltext = "30035-03.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30035-04.htm";
							takeItems(player, HARRIS_RECEIPT_2, 1);
							takeItems(player, TOOL_BOX, 1);
							giveItems(player, WOODEN_BREASTPLATE, 1);
							addExpAndSp(player, 5000, 0);
							st.exitQuest(false, true);
						}
						break;
					}
					case ALTRAN:
					{
						if (cond == 1)
						{
							htmltext = "30283-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30283-03.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30283-04.htm";
							st.setCond(4, true);
							takeItems(player, GOLEM_SHARD, -1);
							giveItems(player, TOOL_BOX, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30283-05.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(2) && (getRandom(100) < 30) && (getQuestItemsCount(killer, GOLEM_SHARD) < 5))
		{
			giveItems(killer, GOLEM_SHARD, 1);
			if (getQuestItemsCount(killer, GOLEM_SHARD) >= 5)
			{
				qs.setCond(3, true);
			}
			else
			{
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
}
