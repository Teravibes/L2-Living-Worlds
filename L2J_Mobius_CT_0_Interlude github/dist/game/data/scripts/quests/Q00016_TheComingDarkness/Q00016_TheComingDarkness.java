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
package quests.Q00016_TheComingDarkness;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00016_TheComingDarkness extends Quest
{
	// NPCs
	private static final int HIERARCH = 31517;
	private static final int EVIL_ALTAR_1 = 31512;
	private static final int EVIL_ALTAR_2 = 31513;
	private static final int EVIL_ALTAR_3 = 31514;
	private static final int EVIL_ALTAR_4 = 31515;
	private static final int EVIL_ALTAR_5 = 31516;
	
	// Item
	private static final int CRYSTAL_OF_SEAL = 7167;
	
	public Q00016_TheComingDarkness()
	{
		super(16, "The Coming Darkness");
		registerQuestItems(CRYSTAL_OF_SEAL);
		addStartNpc(HIERARCH);
		addTalkId(HIERARCH, EVIL_ALTAR_1, EVIL_ALTAR_2, EVIL_ALTAR_3, EVIL_ALTAR_4, EVIL_ALTAR_5);
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
			case "31517-2.htm":
			{
				st.startQuest();
				giveItems(player, CRYSTAL_OF_SEAL, 5);
				break;
			}
			case "31512-1.htm":
			{
				st.setCond(2, true);
				takeItems(player, CRYSTAL_OF_SEAL, 1);
				break;
			}
			case "31513-1.htm":
			{
				st.setCond(3, true);
				takeItems(player, CRYSTAL_OF_SEAL, 1);
				break;
			}
			case "31514-1.htm":
			{
				st.setCond(4, true);
				takeItems(player, CRYSTAL_OF_SEAL, 1);
				break;
			}
			case "31515-1.htm":
			{
				st.setCond(5, true);
				takeItems(player, CRYSTAL_OF_SEAL, 1);
				break;
			}
			case "31516-1.htm":
			{
				st.setCond(6, true);
				takeItems(player, CRYSTAL_OF_SEAL, 1);
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
				htmltext = (player.getLevel() < 62) ? "31517-0a.htm" : "31517-0.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				final int npcId = npc.getId();
				
				switch (npcId)
				{
					case HIERARCH:
					{
						if (cond == 6)
						{
							htmltext = "31517-4.htm";
							addExpAndSp(player, 221958, 0);
							st.exitQuest(false, true);
						}
						else
						{
							if (hasQuestItems(player, CRYSTAL_OF_SEAL))
							{
								htmltext = "31517-3.htm";
							}
							else
							{
								htmltext = "31517-3a.htm";
								st.exitQuest(true);
							}
						}
						break;
					}
					case EVIL_ALTAR_1:
					case EVIL_ALTAR_2:
					case EVIL_ALTAR_3:
					case EVIL_ALTAR_4:
					case EVIL_ALTAR_5:
					{
						final int condAltar = npcId - 31511;
						if (cond == condAltar)
						{
							if (hasQuestItems(player, CRYSTAL_OF_SEAL))
							{
								htmltext = npcId + "-0.htm";
							}
							else
							{
								htmltext = "altar_nocrystal.htm";
							}
						}
						else if (cond > condAltar)
						{
							htmltext = npcId + "-2.htm";
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
