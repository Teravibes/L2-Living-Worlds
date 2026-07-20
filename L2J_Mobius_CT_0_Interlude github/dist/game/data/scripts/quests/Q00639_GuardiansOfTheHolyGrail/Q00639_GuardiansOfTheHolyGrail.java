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
package quests.Q00639_GuardiansOfTheHolyGrail;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00639_GuardiansOfTheHolyGrail extends Quest
{
	// NPCs
	private static final int DOMINIC = 31350;
	private static final int GREMORY = 32008;
	private static final int HOLY_GRAIL = 32028;
	
	// Items
	private static final int SCRIPTURE = 8069;
	private static final int WATER_BOTTLE = 8070;
	private static final int HOLY_WATER_BOTTLE = 8071;
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(22122, 760000);
		CHANCES.put(22123, 750000);
		CHANCES.put(22124, 590000);
		CHANCES.put(22125, 580000);
		CHANCES.put(22126, 590000);
		CHANCES.put(22127, 580000);
		CHANCES.put(22128, 170000);
		CHANCES.put(22129, 590000);
		CHANCES.put(22130, 850000);
		CHANCES.put(22131, 920000);
		CHANCES.put(22132, 580000);
		CHANCES.put(22133, 930000);
		CHANCES.put(22134, 230000);
		CHANCES.put(22135, 580000);
	}
	
	public Q00639_GuardiansOfTheHolyGrail()
	{
		super(639, "Guardians of the Holy Grail");
		registerQuestItems(SCRIPTURE, WATER_BOTTLE, HOLY_WATER_BOTTLE);
		addStartNpc(DOMINIC);
		addTalkId(DOMINIC, GREMORY, HOLY_GRAIL);
		addKillId(CHANCES.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31350-03.htm":
			case "31350-07.htm":
			case "32008-02.htm":
			case "32008-03.htm":
			case "32008-04.htm":
			{
				htmltext = event;
				break;
			}
			case "31350-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "31350-08.htm":
			{
				final int count = getQuestItemsCount(player, SCRIPTURE);
				takeItems(player, SCRIPTURE, -1);
				giveAdena(player, (1625 * count) + ((count >= 10) ? 33940 : 0), true);
				htmltext = event;
				break;
			}
			case "31350-09.htm":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "32008-05.htm":
			{
				qs.setCond(2, true);
				giveItems(player, WATER_BOTTLE, 1);
				htmltext = event;
				break;
			}
			case "32008-09.htm":
			{
				qs.setCond(4, true);
				takeItems(player, HOLY_WATER_BOTTLE, 1);
				htmltext = event;
				break;
			}
			case "32008-12.htm":
			{
				if (getQuestItemsCount(player, SCRIPTURE) >= 4000)
				{
					takeItems(player, SCRIPTURE, 4000);
					rewardItems(player, 959, 1);
					htmltext = "32008-11.htm";
				}
				else
				{
					htmltext = event;
				}
				break;
			}
			case "32008-14.htm":
			{
				if (getQuestItemsCount(player, SCRIPTURE) >= 400)
				{
					takeItems(player, SCRIPTURE, 400);
					rewardItems(player, 960, 1);
					htmltext = "32008-13.htm";
				}
				else
				{
					htmltext = event;
				}
				break;
			}
			case "32028-02.htm":
			{
				qs.setCond(3, true);
				takeItems(player, WATER_BOTTLE, 1);
				giveItems(player, HOLY_WATER_BOTTLE, 1);
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = player.getLevel() < 73 ? "31350-02.htm" : "31350-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case DOMINIC:
					{
						htmltext = hasQuestItems(player, SCRIPTURE) ? "31350-05.htm" : "31350-06.htm";
						break;
					}
					case GREMORY:
					{
						switch (qs.getCond())
						{
							case 1:
							{
								htmltext = "32008-01.htm";
								break;
							}
							case 2:
							{
								htmltext = "32008-06.htm";
								break;
							}
							case 3:
							{
								htmltext = "32008-08.htm";
								break;
							}
							case 4:
							{
								htmltext = "32008-10.htm";
								break;
							}
						}
						break;
					}
					case HOLY_GRAIL:
					{
						if (qs.isCond(2))
						{
							htmltext = "32028-01.htm";
						}
						else if (qs.getCond() > 2)
						{
							htmltext = "32028-03.htm";
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 1, 3, npc);
		if ((qs != null) && (Rnd.get(1000000) < CHANCES.get(npc.getId())))
		{
			giveItemRandomly(qs.getPlayer(), npc, SCRIPTURE, 1, 0, 1, true);
		}
	}
}
