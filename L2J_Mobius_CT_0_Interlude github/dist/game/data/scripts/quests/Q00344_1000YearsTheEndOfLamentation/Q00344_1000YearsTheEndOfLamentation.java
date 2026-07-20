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
package quests.Q00344_1000YearsTheEndOfLamentation;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00344_1000YearsTheEndOfLamentation extends Quest
{
	// NPCs
	private static final int GILMORE = 30754;
	private static final int RODEMAI = 30756;
	private static final int ORVEN = 30857;
	private static final int KAIEN = 30623;
	private static final int GARVARENTZ = 30704;
	
	// Items
	private static final int ARTICLE_DEAD_HERO = 4269;
	private static final int OLD_KEY = 4270;
	private static final int OLD_HILT = 4271;
	private static final int OLD_TOTEM = 4272;
	private static final int CRUCIFIX = 4273;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(20236, 380000);
		CHANCES.put(20237, 490000);
		CHANCES.put(20238, 460000);
		CHANCES.put(20239, 490000);
		CHANCES.put(20240, 530000);
		CHANCES.put(20272, 380000);
		CHANCES.put(20273, 490000);
		CHANCES.put(20274, 460000);
		CHANCES.put(20275, 490000);
		CHANCES.put(20276, 530000);
	}
	
	public Q00344_1000YearsTheEndOfLamentation()
	{
		super(344, "1000 years, the End of Lamentation");
		registerQuestItems(ARTICLE_DEAD_HERO, OLD_KEY, OLD_HILT, OLD_TOTEM, CRUCIFIX);
		addStartNpc(GILMORE);
		addTalkId(GILMORE, RODEMAI, ORVEN, GARVARENTZ, KAIEN);
		addKillId(20236, 20237, 20238, 20239, 20240, 20272, 20273, 20274, 20275, 20276);
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
			case "30754-04.htm":
			{
				st.startQuest();
				break;
			}
			case "30754-07.htm":
			{
				if (st.get("success") != null)
				{
					st.setCond(1, true);
					st.unset("success");
				}
				break;
			}
			case "30754-08.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30754-06.htm":
			{
				if (!hasQuestItems(player, ARTICLE_DEAD_HERO))
				{
					htmltext = "30754-06a.htm";
				}
				else
				{
					final int amount = getQuestItemsCount(player, ARTICLE_DEAD_HERO);
					
					takeItems(player, ARTICLE_DEAD_HERO, -1);
					giveAdena(player, amount * 60, true);
					
					// Special item, % based on actual number of qItems.
					if (getRandom(1000) < Math.min(10, Math.max(1, amount / 10)))
					{
						htmltext = "30754-10.htm";
					}
				}
				break;
			}
			case "30754-11.htm":
			{
				final int random = getRandom(4);
				if (random < 1)
				{
					htmltext = "30754-12.htm";
					giveItems(player, OLD_KEY, 1);
				}
				else if (random < 2)
				{
					htmltext = "30754-13.htm";
					giveItems(player, OLD_HILT, 1);
				}
				else if (random < 3)
				{
					htmltext = "30754-14.htm";
					giveItems(player, OLD_TOTEM, 1);
				}
				else
				{
					giveItems(player, CRUCIFIX, 1);
				}
				
				st.setCond(2, true);
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
				htmltext = (player.getLevel() < 48) ? "30754-01.htm" : "30754-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case GILMORE:
					{
						if (cond == 1)
						{
							htmltext = (hasQuestItems(player, ARTICLE_DEAD_HERO)) ? "30754-05.htm" : "30754-09.htm";
						}
						else if (cond == 2)
						{
							htmltext = (st.get("success") != null) ? "30754-16.htm" : "30754-15.htm";
						}
						break;
					}
					default:
					{
						if (cond == 2)
						{
							if (st.get("success") != null)
							{
								htmltext = npc.getId() + "-02.htm";
							}
							else
							{
								rewards(st, npc.getId());
								htmltext = npc.getId() + "-01.htm";
							}
						}
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	private void rewards(QuestState st, int npcId)
	{
		final Player player = st.getPlayer();
		switch (npcId)
		{
			case ORVEN:
			{
				if (hasQuestItems(player, CRUCIFIX))
				{
					st.set("success", "1");
					takeItems(player, CRUCIFIX, -1);
					
					final int chance = getRandom(100);
					if (chance < 80)
					{
						giveItems(player, 1875, 19);
					}
					else if (chance < 95)
					{
						giveItems(player, 952, 5);
					}
					else
					{
						giveItems(player, 2437, 1);
					}
				}
				break;
			}
			case GARVARENTZ:
			{
				if (hasQuestItems(player, OLD_TOTEM))
				{
					st.set("success", "1");
					takeItems(player, OLD_TOTEM, -1);
					
					final int chance = getRandom(100);
					if (chance < 55)
					{
						giveItems(player, 1882, 70);
					}
					else if (chance < 99)
					{
						giveItems(player, 1881, 50);
					}
					else
					{
						giveItems(player, 191, 1);
					}
				}
				break;
			}
			case KAIEN:
			{
				if (hasQuestItems(player, OLD_HILT))
				{
					st.set("success", "1");
					takeItems(player, OLD_HILT, -1);
					
					final int chance = getRandom(100);
					if (chance < 60)
					{
						giveItems(player, 1874, 25);
					}
					else if (chance < 85)
					{
						giveItems(player, 1887, 10);
					}
					else if (chance < 99)
					{
						giveItems(player, 951, 1);
					}
					else
					{
						giveItems(player, 133, 1);
					}
				}
				break;
			}
			case RODEMAI:
			{
				if (hasQuestItems(player, OLD_KEY))
				{
					st.set("success", "1");
					takeItems(player, OLD_KEY, -1);
					
					final int chance = getRandom(100);
					if (chance < 80)
					{
						giveItems(player, 1879, 55);
					}
					else if (chance < 95)
					{
						giveItems(player, 951, 1);
					}
					else
					{
						giveItems(player, 885, 1);
					}
				}
				break;
			}
		}
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
			giveItems(player, ARTICLE_DEAD_HERO, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
