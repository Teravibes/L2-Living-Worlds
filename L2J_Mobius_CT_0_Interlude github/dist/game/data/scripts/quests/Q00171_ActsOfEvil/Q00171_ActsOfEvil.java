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
package quests.Q00171_ActsOfEvil;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00171_ActsOfEvil extends Quest
{
	// NPCs
	private static final int ALVAH = 30381;
	private static final int ARODIN = 30207;
	private static final int TYRA = 30420;
	private static final int ROLENTO = 30437;
	private static final int NETI = 30425;
	private static final int BURAI = 30617;
	
	// Items
	private static final int BLADE_MOLD = 4239;
	private static final int TYRA_BILL = 4240;
	private static final int RANGER_REPORT_1 = 4241;
	private static final int RANGER_REPORT_2 = 4242;
	private static final int RANGER_REPORT_3 = 4243;
	private static final int RANGER_REPORT_4 = 4244;
	private static final int WEAPON_TRADE_CONTRACT = 4245;
	private static final int ATTACK_DIRECTIVES = 4246;
	private static final int CERTIFICATE = 4247;
	private static final int CARGO_BOX = 4248;
	private static final int OL_MAHUM_HEAD = 4249;
	
	// Turek Orcs drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(20496, 530000);
		CHANCES.put(20497, 550000);
		CHANCES.put(20498, 510000);
		CHANCES.put(20499, 500000);
	}
	
	public Q00171_ActsOfEvil()
	{
		super(171, "Acts of Evil");
		registerQuestItems(BLADE_MOLD, TYRA_BILL, RANGER_REPORT_1, RANGER_REPORT_2, RANGER_REPORT_3, RANGER_REPORT_4, WEAPON_TRADE_CONTRACT, ATTACK_DIRECTIVES, CERTIFICATE, CARGO_BOX, OL_MAHUM_HEAD);
		addStartNpc(ALVAH);
		addTalkId(ALVAH, ARODIN, TYRA, ROLENTO, NETI, BURAI);
		addKillId(20496, 20497, 20498, 20499, 20062, 20064, 20066, 20438);
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
			case "30381-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30207-02.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "30381-04.htm":
			{
				st.setCond(5, true);
				break;
			}
			case "30381-07.htm":
			{
				st.setCond(7, true);
				takeItems(player, WEAPON_TRADE_CONTRACT, 1);
				break;
			}
			case "30437-03.htm":
			{
				st.setCond(9, true);
				giveItems(player, CARGO_BOX, 1);
				giveItems(player, CERTIFICATE, 1);
				break;
			}
			case "30617-04.htm":
			{
				st.setCond(10, true);
				takeItems(player, ATTACK_DIRECTIVES, 1);
				takeItems(player, CARGO_BOX, 1);
				takeItems(player, CERTIFICATE, 1);
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
				htmltext = (player.getLevel() < 27) ? "30381-01a.htm" : "30381-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ALVAH:
					{
						if (cond < 4)
						{
							htmltext = "30381-02a.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30381-03.htm";
						}
						else if (cond == 5)
						{
							if (hasQuestItems(player, RANGER_REPORT_1, RANGER_REPORT_2, RANGER_REPORT_3, RANGER_REPORT_4))
							{
								htmltext = "30381-05.htm";
								st.setCond(6, true);
								takeItems(player, RANGER_REPORT_1, 1);
								takeItems(player, RANGER_REPORT_2, 1);
								takeItems(player, RANGER_REPORT_3, 1);
								takeItems(player, RANGER_REPORT_4, 1);
							}
							else
							{
								htmltext = "30381-04a.htm";
							}
						}
						else if (cond == 6)
						{
							if (hasQuestItems(player, WEAPON_TRADE_CONTRACT, ATTACK_DIRECTIVES))
							{
								htmltext = "30381-06.htm";
							}
							else
							{
								htmltext = "30381-05a.htm";
							}
						}
						else if ((cond > 6) && (cond < 11))
						{
							htmltext = "30381-07a.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30381-08.htm";
							giveAdena(player, 90000, true);
							st.exitQuest(false, true);
						}
						break;
					}
					case ARODIN:
					{
						if (cond == 1)
						{
							htmltext = "30207-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30207-01a.htm";
						}
						else if (cond == 3)
						{
							if (hasQuestItems(player, TYRA_BILL))
							{
								htmltext = "30207-03.htm";
								st.setCond(4, true);
								takeItems(player, TYRA_BILL, 1);
							}
							else
							{
								htmltext = "30207-01a.htm";
							}
						}
						else if (cond > 3)
						{
							htmltext = "30207-03a.htm";
						}
						break;
					}
					case TYRA:
					{
						if (cond == 2)
						{
							if (getQuestItemsCount(player, BLADE_MOLD) >= 20)
							{
								htmltext = "30420-01.htm";
								st.setCond(3, true);
								takeItems(player, BLADE_MOLD, -1);
								giveItems(player, TYRA_BILL, 1);
							}
							else
							{
								htmltext = "30420-01b.htm";
							}
						}
						else if (cond == 3)
						{
							htmltext = "30420-01a.htm";
						}
						else if (cond > 3)
						{
							htmltext = "30420-02.htm";
						}
						break;
					}
					case NETI:
					{
						if (cond == 7)
						{
							htmltext = "30425-01.htm";
							st.setCond(8, true);
						}
						else if (cond > 7)
						{
							htmltext = "30425-02.htm";
						}
						break;
					}
					case ROLENTO:
					{
						if (cond == 8)
						{
							htmltext = "30437-01.htm";
						}
						else if (cond > 8)
						{
							htmltext = "30437-03a.htm";
						}
						break;
					}
					case BURAI:
					{
						if ((cond == 9) && hasQuestItems(player, CERTIFICATE, CARGO_BOX, ATTACK_DIRECTIVES))
						{
							htmltext = "30617-01.htm";
						}
						else if (cond == 10)
						{
							if (getQuestItemsCount(player, OL_MAHUM_HEAD) >= 30)
							{
								htmltext = "30617-05.htm";
								st.setCond(11, true);
								takeItems(player, OL_MAHUM_HEAD, -1);
								giveAdena(player, 8000, true);
							}
							else
							{
								htmltext = "30617-04a.htm";
							}
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
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		final int npcId = npc.getId();
		switch (npcId)
		{
			case 20496:
			case 20497:
			case 20498:
			case 20499:
			{
				if (st.isCond(2) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					final int count = getQuestItemsCount(player, BLADE_MOLD);
					if (count < 20)
					{
						giveItems(player, BLADE_MOLD, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					
					if ((count == 5) || ((count >= 10) && (getRandom(100) < 25)))
					{
						addSpawn(27190, player.getX(), player.getY(), player.getZ(), player.getHeading(), false, 0);
					}
				}
				break;
			}
			case 20062:
			case 20064:
			{
				if (st.isCond(5))
				{
					if (!hasQuestItems(player, RANGER_REPORT_1))
					{
						giveItems(player, RANGER_REPORT_1, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (getRandom(100) < 20)
					{
						if (!hasQuestItems(player, RANGER_REPORT_2))
						{
							giveItems(player, RANGER_REPORT_2, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else if (!hasQuestItems(player, RANGER_REPORT_3))
						{
							giveItems(player, RANGER_REPORT_3, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
						else if (!hasQuestItems(player, RANGER_REPORT_4))
						{
							giveItems(player, RANGER_REPORT_4, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case 20438:
			{
				if (st.isCond(6) && (getRandom(100) < 10) && !hasQuestItems(player, WEAPON_TRADE_CONTRACT, ATTACK_DIRECTIVES))
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					giveItems(player, WEAPON_TRADE_CONTRACT, 1);
					giveItems(player, ATTACK_DIRECTIVES, 1);
				}
				break;
			}
			case 20066:
			{
				if (st.isCond(10) && (getQuestItemsCount(player, OL_MAHUM_HEAD) < 30) && getRandomBoolean())
				{
					giveItems(player, OL_MAHUM_HEAD, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
