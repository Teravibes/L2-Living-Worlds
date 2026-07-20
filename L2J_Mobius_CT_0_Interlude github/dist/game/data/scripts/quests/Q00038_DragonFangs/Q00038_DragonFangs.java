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
package quests.Q00038_DragonFangs;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00038_DragonFangs extends Quest
{
	// NPCs
	private static final int LUIS = 30386;
	private static final int IRIS = 30034;
	private static final int ROHMER = 30344;
	
	// Items
	private static final int FEATHER_ORNAMENT = 7173;
	private static final int TOOTH_OF_TOTEM = 7174;
	private static final int TOOTH_OF_DRAGON = 7175;
	private static final int LETTER_OF_IRIS = 7176;
	private static final int LETTER_OF_ROHMER = 7177;
	
	// Reward { item, adena }
	private static final int[][] REWARD =
	{
		// @formatter:off
		{45, 5200},
		{627, 1500},
		{1123, 3200},
		{605, 3200}
		// @formatter:on
	};
	
	// Droplist
	private static final Map<Integer, int[]> DROPLIST = new HashMap<>();
	static
	{
		// @formatter:off
		
		DROPLIST.put(21100, new int[]{1, FEATHER_ORNAMENT, 100, 1000000});
		DROPLIST.put(20357, new int[]{1, FEATHER_ORNAMENT, 100, 1000000});
		
		DROPLIST.put(21101, new int[]{6, TOOTH_OF_DRAGON, 50, 500000});
		DROPLIST.put(20356, new int[]{6, TOOTH_OF_DRAGON, 50, 500000});
		// @formatter:on
	}
	
	public Q00038_DragonFangs()
	{
		super(38, "Dragon Fangs");
		registerQuestItems(FEATHER_ORNAMENT, TOOTH_OF_TOTEM, TOOTH_OF_DRAGON, LETTER_OF_IRIS, LETTER_OF_ROHMER);
		addStartNpc(LUIS);
		addTalkId(LUIS, IRIS, ROHMER);
		addKillId(DROPLIST.keySet());
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
			case "30386-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30386-04.htm":
			{
				st.setCond(3, true);
				takeItems(player, FEATHER_ORNAMENT, 100);
				giveItems(player, TOOTH_OF_TOTEM, 1);
				break;
			}
			case "30034-02a.htm":
			{
				if (hasQuestItems(player, TOOTH_OF_TOTEM))
				{
					htmltext = "30034-02.htm";
					st.setCond(4, true);
					takeItems(player, TOOTH_OF_TOTEM, 1);
					giveItems(player, LETTER_OF_IRIS, 1);
				}
				break;
			}
			case "30344-02a.htm":
			{
				if (hasQuestItems(player, LETTER_OF_IRIS))
				{
					htmltext = "30344-02.htm";
					st.setCond(5, true);
					takeItems(player, LETTER_OF_IRIS, 1);
					giveItems(player, LETTER_OF_ROHMER, 1);
				}
				break;
			}
			case "30034-04a.htm":
			{
				if (hasQuestItems(player, LETTER_OF_ROHMER))
				{
					htmltext = "30034-04.htm";
					st.setCond(6, true);
					takeItems(player, LETTER_OF_ROHMER, 1);
				}
				break;
			}
			case "30034-06a.htm":
			{
				if (getQuestItemsCount(player, TOOTH_OF_DRAGON) >= 50)
				{
					final int position = getRandom(REWARD.length);
					htmltext = "30034-06.htm";
					takeItems(player, TOOTH_OF_DRAGON, 50);
					giveItems(player, REWARD[position][0], 1);
					giveAdena(player, REWARD[position][1], true);
					st.exitQuest(false, true);
				}
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
				htmltext = (player.getLevel() < 19) ? "30386-01a.htm" : "30386-01.htm";
				break;
			
			case State.STARTED:
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case LUIS:
						if (cond == 1)
						{
							htmltext = "30386-02a.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30386-03.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30386-03a.htm";
						}
						break;
					
					case IRIS:
						if (cond == 3)
						{
							htmltext = "30034-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30034-02b.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30034-03.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30034-05a.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30034-05.htm";
						}
						break;
					
					case ROHMER:
						if (cond == 4)
						{
							htmltext = "30344-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30344-03.htm";
						}
						break;
				}
				break;
			
			case State.COMPLETED:
				htmltext = getAlreadyCompletedMsg(player);
				break;
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
		
		final int[] droplist = DROPLIST.get(npc.getId());
		if ((droplist != null) && st.isCond(droplist[0]))
		{
			final int itemId = droplist[1];
			final int amount = droplist[2];
			final int chance = droplist[3];
			
			if (getRandom(1000000) < chance)
			{
				giveItems(player, itemId, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				
				if (getQuestItemsCount(player, itemId) >= amount)
				{
					st.setCond(droplist[0] + 1, true);
				}
			}
		}
	}
}
