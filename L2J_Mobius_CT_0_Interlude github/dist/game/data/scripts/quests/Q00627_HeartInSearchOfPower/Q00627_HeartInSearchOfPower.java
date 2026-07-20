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
package quests.Q00627_HeartInSearchOfPower;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00627_HeartInSearchOfPower extends Quest
{
	// NPCs
	private static final int NECROMANCER = 31518;
	private static final int ENFEUX = 31519;
	
	// Items
	private static final int SEAL_OF_LIGHT = 7170;
	private static final int BEAD_OF_OBEDIENCE = 7171;
	private static final int GEM_OF_SAINTS = 7172;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(21520, 550000);
		CHANCES.put(21523, 584000);
		CHANCES.put(21524, 621000);
		CHANCES.put(21525, 621000);
		CHANCES.put(21526, 606000);
		CHANCES.put(21529, 625000);
		CHANCES.put(21530, 578000);
		CHANCES.put(21531, 690000);
		CHANCES.put(21532, 671000);
		CHANCES.put(21535, 693000);
		CHANCES.put(21536, 615000);
		CHANCES.put(21539, 762000);
		CHANCES.put(21540, 762000);
		CHANCES.put(21658, 690000);
	}
	
	// Rewards
	private static final Map<String, int[]> REWARDS = new HashMap<>();
	static
	{
		// @formatter:off
		REWARDS.put("adena", new int[]{0, 0, 100000});
		REWARDS.put("asofe", new int[]{4043, 13, 6400});
		REWARDS.put("thon", new int[]{4044, 13, 6400});
		REWARDS.put("enria", new int[]{4042, 6, 13600});
		REWARDS.put("mold", new int[]{4041, 6, 17200});
		// @formatter:on
	}
	
	public Q00627_HeartInSearchOfPower()
	{
		super(627, "Heart in Search of Power");
		registerQuestItems(BEAD_OF_OBEDIENCE);
		addStartNpc(NECROMANCER);
		addTalkId(NECROMANCER, ENFEUX);
		addKillId(CHANCES.keySet());
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
		
		if (event.equals("31518-01.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31518-03.htm"))
		{
			if (getQuestItemsCount(player, BEAD_OF_OBEDIENCE) == 300)
			{
				st.setCond(3, true);
				takeItems(player, BEAD_OF_OBEDIENCE, -1);
				giveItems(player, SEAL_OF_LIGHT, 1);
			}
			else
			{
				st.setCond(1);
				takeItems(player, BEAD_OF_OBEDIENCE, -1);
				htmltext = "31518-03a.htm";
			}
		}
		else if (event.equals("31519-01.htm"))
		{
			if (getQuestItemsCount(player, SEAL_OF_LIGHT) == 1)
			{
				st.setCond(4, true);
				takeItems(player, SEAL_OF_LIGHT, 1);
				giveItems(player, GEM_OF_SAINTS, 1);
			}
		}
		else if (REWARDS.containsKey(event))
		{
			if (getQuestItemsCount(player, GEM_OF_SAINTS) == 1)
			{
				htmltext = "31518-07.htm";
				takeItems(player, GEM_OF_SAINTS, 1);
				
				if (REWARDS.get(event)[0] > 0)
				{
					giveItems(player, REWARDS.get(event)[0], REWARDS.get(event)[1]);
				}
				
				giveAdena(player, REWARDS.get(event)[2], true);
				
				st.exitQuest(true, true);
			}
			else
			{
				htmltext = "31518-7.htm";
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
				htmltext = (player.getLevel() < 60) ? "31518-00a.htm" : "31518-00.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case NECROMANCER:
					{
						if (cond == 1)
						{
							htmltext = "31518-01a.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31518-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "31518-04.htm";
						}
						else if (cond == 4)
						{
							htmltext = "31518-05.htm";
						}
						break;
					}
					case ENFEUX:
					{
						if (cond == 3)
						{
							htmltext = "31519-00.htm";
						}
						else if (cond == 4)
						{
							htmltext = "31519-02.htm";
						}
						break;
					}
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
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			giveItems(player, BEAD_OF_OBEDIENCE, 1);
			if (getQuestItemsCount(player, BEAD_OF_OBEDIENCE) < 300)
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(2, true);
			}
		}
	}
}
