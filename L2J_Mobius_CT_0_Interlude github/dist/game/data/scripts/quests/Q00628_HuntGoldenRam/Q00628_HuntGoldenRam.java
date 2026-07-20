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
package quests.Q00628_HuntGoldenRam;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00628_HuntGoldenRam extends Quest
{
	// NPCs
	private static final int KAHMAN = 31554;
	
	// Items
	private static final int SPLINTER_STAKATO_CHITIN = 7248;
	private static final int NEEDLE_STAKATO_CHITIN = 7249;
	private static final int GOLDEN_RAM_BADGE_RECRUIT = 7246;
	private static final int GOLDEN_RAM_BADGE_SOLDIER = 7247;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(21508, 500000);
		CHANCES.put(21509, 430000);
		CHANCES.put(21510, 521000);
		CHANCES.put(21511, 575000);
		CHANCES.put(21512, 746000);
		CHANCES.put(21513, 500000);
		CHANCES.put(21514, 430000);
		CHANCES.put(21515, 520000);
		CHANCES.put(21516, 531000);
		CHANCES.put(21517, 744000);
	}
	
	public Q00628_HuntGoldenRam()
	{
		super(628, "Hunt of the Golden Ram Mercenary Force");
		registerQuestItems(SPLINTER_STAKATO_CHITIN, NEEDLE_STAKATO_CHITIN, GOLDEN_RAM_BADGE_RECRUIT, GOLDEN_RAM_BADGE_SOLDIER);
		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);
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
		
		switch (event)
		{
			case "31554-02.htm":
			{
				st.startQuest();
				break;
			}
			case "31554-03a.htm":
			{
				if ((getQuestItemsCount(player, SPLINTER_STAKATO_CHITIN) >= 100) && st.isCond(1)) // Giving GOLDEN_RAM_BADGE_RECRUIT Medals
				{
					htmltext = "31554-04.htm";
					st.setCond(2, true);
					takeItems(player, SPLINTER_STAKATO_CHITIN, -1);
					giveItems(player, GOLDEN_RAM_BADGE_RECRUIT, 1);
				}
				break;
			}
			case "31554-07.htm":
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
				htmltext = (player.getLevel() < 66) ? "31554-01a.htm" : "31554-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					if (getQuestItemsCount(player, SPLINTER_STAKATO_CHITIN) >= 100)
					{
						htmltext = "31554-03.htm";
					}
					else
					{
						htmltext = "31554-03a.htm";
					}
				}
				else if (cond == 2)
				{
					if ((getQuestItemsCount(player, SPLINTER_STAKATO_CHITIN) >= 100) && (getQuestItemsCount(player, NEEDLE_STAKATO_CHITIN) >= 100))
					{
						htmltext = "31554-05.htm";
						st.setCond(3);
						playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
						takeItems(player, SPLINTER_STAKATO_CHITIN, -1);
						takeItems(player, NEEDLE_STAKATO_CHITIN, -1);
						takeItems(player, GOLDEN_RAM_BADGE_RECRUIT, 1);
						giveItems(player, GOLDEN_RAM_BADGE_SOLDIER, 1);
					}
					else if (!hasQuestItems(player, SPLINTER_STAKATO_CHITIN) && !hasQuestItems(player, NEEDLE_STAKATO_CHITIN))
					{
						htmltext = "31554-04b.htm";
					}
					else
					{
						htmltext = "31554-04a.htm";
					}
				}
				else if (cond == 3)
				{
					htmltext = "31554-05a.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getRandomPartyMemberState(player, -1, 3, npc);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		final Player partyMember = st.getPlayer();
		final int cond = st.getCond();
		final int npcId = npc.getId();
		switch (npcId)
		{
			case 21508:
			case 21509:
			case 21510:
			case 21511:
			case 21512:
			{
				if (((cond == 1) || (cond == 2)) && (getQuestItemsCount(partyMember, SPLINTER_STAKATO_CHITIN) < 100) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					giveItems(partyMember, SPLINTER_STAKATO_CHITIN, 1);
					if (getQuestItemsCount(partyMember, SPLINTER_STAKATO_CHITIN) < 100)
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
			case 21513:
			case 21514:
			case 21515:
			case 21516:
			case 21517:
			{
				if ((cond == 2) && (getQuestItemsCount(partyMember, NEEDLE_STAKATO_CHITIN) < 100) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					giveItems(partyMember, NEEDLE_STAKATO_CHITIN, 1);
					if (getQuestItemsCount(partyMember, NEEDLE_STAKATO_CHITIN) < 100)
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
		}
	}
}
