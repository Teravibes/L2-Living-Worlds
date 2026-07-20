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
package quests.Q00005_MinersFavor;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00005_MinersFavor extends Quest
{
	// NPCs
	private static final int BOLTER = 30554;
	private static final int SHARI = 30517;
	private static final int GARITA = 30518;
	private static final int REED = 30520;
	private static final int BRUNON = 30526;
	
	// Items
	private static final int BOLTERS_LIST = 1547;
	private static final int MINING_BOOTS = 1548;
	private static final int MINERS_PICK = 1549;
	private static final int BOOMBOOM_POWDER = 1550;
	private static final int REDSTONE_BEER = 1551;
	private static final int BOLTERS_SMELLY_SOCKS = 1552;
	
	// Reward
	private static final int NECKLACE = 906;
	
	public Q00005_MinersFavor()
	{
		super(5, "Miner's Favor");
		registerQuestItems(BOLTERS_LIST, MINING_BOOTS, MINERS_PICK, BOOMBOOM_POWDER, REDSTONE_BEER, BOLTERS_SMELLY_SOCKS);
		addStartNpc(BOLTER);
		addTalkId(BOLTER, SHARI, GARITA, REED, BRUNON);
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
		
		if (event.equals("30554-03.htm"))
		{
			st.startQuest();
			giveItems(player, BOLTERS_LIST, 1);
			giveItems(player, BOLTERS_SMELLY_SOCKS, 1);
		}
		else if (event.equals("30526-02.htm"))
		{
			takeItems(player, BOLTERS_SMELLY_SOCKS, 1);
			giveItems(player, MINERS_PICK, 1);
			if (hasQuestItems(player, MINING_BOOTS, BOOMBOOM_POWDER, REDSTONE_BEER))
			{
				st.setCond(2, true);
			}
			else
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
				htmltext = (player.getLevel() < 2) ? "30554-01.htm" : "30554-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case BOLTER:
					{
						if (cond == 1)
						{
							htmltext = "30554-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30554-06.htm";
							takeItems(player, BOLTERS_LIST, 1);
							takeItems(player, BOOMBOOM_POWDER, 1);
							takeItems(player, MINERS_PICK, 1);
							takeItems(player, MINING_BOOTS, 1);
							takeItems(player, REDSTONE_BEER, 1);
							giveItems(player, NECKLACE, 1);
							st.exitQuest(false, true);
						}
						break;
					}
					case SHARI:
					{
						if ((cond == 1) && !hasQuestItems(player, BOOMBOOM_POWDER))
						{
							htmltext = "30517-01.htm";
							giveItems(player, BOOMBOOM_POWDER, 1);
							if (hasQuestItems(player, MINING_BOOTS, MINERS_PICK, REDSTONE_BEER))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else
						{
							htmltext = "30517-02.htm";
						}
						break;
					}
					case GARITA:
					{
						if ((cond == 1) && !hasQuestItems(player, MINING_BOOTS))
						{
							htmltext = "30518-01.htm";
							giveItems(player, MINING_BOOTS, 1);
							if (hasQuestItems(player, MINERS_PICK, BOOMBOOM_POWDER, REDSTONE_BEER))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else
						{
							htmltext = "30518-02.htm";
						}
						break;
					}
					case REED:
					{
						if ((cond == 1) && !hasQuestItems(player, REDSTONE_BEER))
						{
							htmltext = "30520-01.htm";
							giveItems(player, REDSTONE_BEER, 1);
							if (hasQuestItems(player, MINING_BOOTS, MINERS_PICK, BOOMBOOM_POWDER))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else
						{
							htmltext = "30520-02.htm";
						}
						break;
					}
					case BRUNON:
					{
						if ((cond == 1) && !hasQuestItems(player, MINERS_PICK))
						{
							htmltext = "30526-01.htm";
						}
						else
						{
							htmltext = "30526-03.htm";
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
}
