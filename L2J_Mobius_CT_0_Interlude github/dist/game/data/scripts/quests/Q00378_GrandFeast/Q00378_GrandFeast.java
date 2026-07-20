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
package quests.Q00378_GrandFeast;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00378_GrandFeast extends Quest
{
	// NPC
	private static final int RANSPO = 30594;
	
	// Items
	private static final int WINE_15 = 5956;
	private static final int WINE_30 = 5957;
	private static final int WINE_60 = 5958;
	private static final int MUSICAL_SCORE = 4421;
	private static final int SALAD_RECIPE = 1455;
	private static final int SAUCE_RECIPE = 1456;
	private static final int STEAK_RECIPE = 1457;
	private static final int RITRON_DESSERT = 5959;
	
	// Rewards
	private static final Map<String, int[]> REWARDS = new HashMap<>();
	static
	{
		// @formatter:off
		REWARDS.put("9", new int[]{847, 1, 5700});
		REWARDS.put("10", new int[]{846, 2, 0});
		REWARDS.put("12", new int[]{909, 1, 25400});
		REWARDS.put("17", new int[]{846, 2, 1200});
		REWARDS.put("18", new int[]{879, 1, 6900});
		REWARDS.put("20", new int[]{890, 2, 8500});
		REWARDS.put("33", new int[]{879, 1, 8100});
		REWARDS.put("34", new int[]{910, 1, 0});
		REWARDS.put("36", new int[]{848, 1, 2200});
		// @formatter:on
	}
	
	public Q00378_GrandFeast()
	{
		super(378, "Magnificent Feast");
		addStartNpc(RANSPO);
		addTalkId(RANSPO);
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
			case "30594-2.htm":
			{
				st.startQuest();
				break;
			}
			case "30594-4a.htm":
			{
				if (hasQuestItems(player, WINE_15))
				{
					st.setCond(2, true);
					st.set("score", "1");
					takeItems(player, WINE_15, 1);
				}
				else
				{
					htmltext = "30594-4.htm";
				}
				break;
			}
			case "30594-4b.htm":
			{
				if (hasQuestItems(player, WINE_30))
				{
					st.setCond(2, true);
					st.set("score", "2");
					takeItems(player, WINE_30, 1);
				}
				else
				{
					htmltext = "30594-4.htm";
				}
				break;
			}
			case "30594-4c.htm":
			{
				if (hasQuestItems(player, WINE_60))
				{
					st.setCond(2, true);
					st.set("score", "4");
					takeItems(player, WINE_60, 1);
				}
				else
				{
					htmltext = "30594-4.htm";
				}
				break;
			}
			case "30594-6.htm":
			{
				if (hasQuestItems(player, MUSICAL_SCORE))
				{
					st.setCond(3, true);
					takeItems(player, MUSICAL_SCORE, 1);
				}
				else
				{
					htmltext = "30594-5.htm";
				}
				break;
			}
			case "30594-8a.htm":
			{
				if (hasQuestItems(player, SALAD_RECIPE))
				{
					st.setCond(4, true);
					final int score = st.getInt("score");
					st.set("score", String.valueOf(score + 8));
					takeItems(player, SALAD_RECIPE, 1);
				}
				else
				{
					htmltext = "30594-8.htm";
				}
				break;
			}
			case "30594-8b.htm":
			{
				if (hasQuestItems(player, SAUCE_RECIPE))
				{
					st.setCond(4, true);
					final int score = st.getInt("score");
					st.set("score", String.valueOf(score + 16));
					takeItems(player, SAUCE_RECIPE, 1);
				}
				else
				{
					htmltext = "30594-8.htm";
				}
				break;
			}
			case "30594-8c.htm":
			{
				if (hasQuestItems(player, STEAK_RECIPE))
				{
					st.setCond(4, true);
					final int score = st.getInt("score");
					st.set("score", String.valueOf(score + 32));
					takeItems(player, STEAK_RECIPE, 1);
				}
				else
				{
					htmltext = "30594-8.htm";
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
			{
				htmltext = (player.getLevel() < 20) ? "30594-0.htm" : "30594-1.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "30594-3.htm";
				}
				else if (cond == 2)
				{
					htmltext = (!hasQuestItems(player, MUSICAL_SCORE)) ? "30594-5.htm" : "30594-5a.htm";
				}
				else if (cond == 3)
				{
					htmltext = "30594-7.htm";
				}
				else if (cond == 4)
				{
					final String score = st.get("score");
					if (REWARDS.containsKey(score) && hasQuestItems(player, RITRON_DESSERT))
					{
						htmltext = "30594-10.htm";
						
						takeItems(player, RITRON_DESSERT, 1);
						giveItems(player, REWARDS.get(score)[0], REWARDS.get(score)[1]);
						
						final int adena = REWARDS.get(score)[2];
						if (adena > 0)
						{
							giveAdena(player, adena, true);
						}
						
						st.exitQuest(true, true);
					}
					else
					{
						htmltext = "30594-9.htm";
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
