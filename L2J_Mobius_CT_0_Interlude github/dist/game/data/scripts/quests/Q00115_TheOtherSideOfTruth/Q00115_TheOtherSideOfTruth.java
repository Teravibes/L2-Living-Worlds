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
package quests.Q00115_TheOtherSideOfTruth;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * @author Mobius
 * @note Based on python script
 */
public class Q00115_TheOtherSideOfTruth extends Quest
{
	// NPCs
	private static final int MISA = 32018;
	private static final int SUSPICIOUS = 32019;
	private static final int RAFFORTY = 32020;
	private static final int SCULPTURE1 = 32021;
	private static final int KIERRE = 32022;
	private static final int SCULPTURE2 = 32077;
	private static final int SCULPTURE3 = 32078;
	private static final int SCULPTURE4 = 32079;
	
	// Items
	private static final int LETTER = 8079;
	private static final int LETTER2 = 8080;
	private static final int TABLET = 8081;
	private static final int REPORT = 8082;
	
	public Q00115_TheOtherSideOfTruth()
	{
		super(115, "The Other Side of Truth");
		addStartNpc(RAFFORTY);
		addTalkId(RAFFORTY, MISA, SCULPTURE1, SCULPTURE2, SCULPTURE3, SCULPTURE4, KIERRE);
		registerQuestItems(LETTER, LETTER2, TABLET, REPORT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "32018-04.htm":
			{
				qs.setCond(7, true);
				takeItems(player, LETTER2, 1);
				break;
			}
			case "32020-02.htm":
			{
				qs.startQuest();
				break;
			}
			case "32020-05.htm":
			{
				qs.setCond(3, true);
				takeItems(player, LETTER, 1);
				break;
			}
			case "32020-06.htm":
			case "32020-08a.htm":
			{
				qs.exitQuest(true, true);
				break;
			}
			case "32020-08.htm":
			case "32020-07a.htm":
			{
				qs.setCond(4, true);
				break;
			}
			case "32020-12.htm":
			{
				qs.setCond(5, true);
				break;
			}
			case "32020-16.htm":
			{
				qs.setCond(10, true);
				takeItems(player, REPORT, 1);
				break;
			}
			case "32020-18.htm":
			{
				if (getQuestItemsCount(player, TABLET) == 0)
				{
					qs.setCond(11, true);
					htmltext = "32020-19.htm";
				}
				else
				{
					qs.exitQuest(false, true);
					giveAdena(player, 115673, true);
					addExpAndSp(player, 493595, 40442);
				}
				break;
			}
			case "32020-19.htm":
			{
				qs.setCond(11, true);
				break;
			}
			case "32022-02.htm":
			{
				qs.setCond(9, true);
				final Npc man = addSpawn(SUSPICIOUS, 104562, -107598, -3688, 0, false, 4000);
				man.broadcastSay(ChatType.GENERAL, "We meet again.");
				startQuestTimer("2", 3700, man, player);
				giveItems(player, REPORT, 1);
				break;
			}
			case "Sculpture-04.htm":
			{
				qs.set("talk", "1");
				htmltext = "Sculpture-05.htm";
				qs.set("" + npc.getId(), "1");
				break;
			}
			case "Sculpture-04a.htm":
			{
				qs.setCond(8, true);
				final Npc man = addSpawn(SUSPICIOUS, 117890, -126478, -2584, 0, false, 4000);
				man.broadcastSay(ChatType.GENERAL, "This looks like the right place...");
				startQuestTimer("1", 3700, man, player);
				htmltext = "Sculpture-04.htm";
				if ((qs.getInt("" + SCULPTURE1) == 0) && (qs.getInt("" + SCULPTURE2) == 0))
				{
					giveItems(player, TABLET, 1);
				}
				break;
			}
			case "Sculpture-05.htm":
			{
				qs.set("" + npc.getId(), "1");
				break;
			}
			case "1":
			{
				npc.broadcastSay(ChatType.GENERAL, "I see someone. Is this fate?");
				break;
			}
			case "2":
			{
				npc.broadcastSay(ChatType.GENERAL, "Don't bother trying to find out more about me. Follow your own destiny.");
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
		
		final int state = qs.getState();
		final int npcId = npc.getId();
		final int cond = qs.getCond();
		if (state == State.COMPLETED)
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npcId == RAFFORTY)
		{
			if (state == State.CREATED)
			{
				if (qs.getPlayer().getLevel() >= 53)
				{
					htmltext = "32020-01.htm";
				}
				else
				{
					htmltext = "32020-00.htm";
					qs.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "32020-03.htm";
			}
			else if (cond == 2)
			{
				htmltext = "32020-04.htm";
			}
			else if (cond == 3)
			{
				htmltext = "32020-05.htm";
			}
			else if (cond == 4)
			{
				htmltext = "32020-11.htm";
			}
			else if (cond == 5)
			{
				htmltext = "32020-13.htm";
				giveItems(player, LETTER2, 1);
				qs.setCond(6, true);
			}
			else if (cond == 6)
			{
				htmltext = "32020-14.htm";
			}
			else if (cond == 9)
			{
				htmltext = "32020-15.htm";
			}
			else if (cond == 10)
			{
				htmltext = "32020-17.htm";
			}
			else if (cond == 11)
			{
				htmltext = "32020-20.htm";
			}
			else if (cond == 12)
			{
				htmltext = "32020-18.htm";
				qs.exitQuest(false, true);
				giveAdena(player, 60044, true);
			}
		}
		else if (npcId == MISA)
		{
			if (cond == 1)
			{
				htmltext = "32018-01.htm";
				giveItems(player, LETTER, 1);
				qs.setCond(2, true);
			}
			else if (cond == 2)
			{
				htmltext = "32018-02.htm";
			}
			else if (cond == 6)
			{
				htmltext = "32018-03.htm";
			}
			else if (cond == 7)
			{
				htmltext = "32018-05.htm";
			}
		}
		else if (npcId == SCULPTURE1)
		{
			if (cond == 7)
			{
				if (qs.getInt("" + npcId) == 1)
				{
					htmltext = "Sculpture-02.htm";
				}
				else if (qs.getInt("talk") == 1)
				{
					htmltext = "Sculpture-06.htm";
				}
				else
				{
					htmltext = "Sculpture-03.htm";
				}
			}
			else if (cond == 8)
			{
				htmltext = "Sculpture-04.htm";
			}
			else if (cond == 11)
			{
				giveItems(player, TABLET, 1);
				qs.setCond(12, true);
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
			{
				htmltext = "Sculpture-08.htm";
			}
		}
		else if (npcId == SCULPTURE2)
		{
			if (cond == 7)
			{
				if (qs.getInt("" + npcId) == 1)
				{
					htmltext = "Sculpture-02.htm";
				}
				else if (qs.getInt("talk") == 1)
				{
					htmltext = "Sculpture-06.htm";
				}
				else
				{
					htmltext = "Sculpture-03.htm";
				}
			}
			else if (cond == 8)
			{
				htmltext = "Sculpture-04.htm";
			}
			else if (cond == 11)
			{
				giveItems(player, TABLET, 1);
				qs.setCond(12, true);
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
			{
				htmltext = "Sculpture-08.htm";
			}
		}
		else if (npcId == SCULPTURE3)
		{
			if (cond == 7)
			{
				if (qs.getInt("" + npcId) == 1)
				{
					htmltext = "Sculpture-02.htm";
				}
				else
				{
					htmltext = "Sculpture-01.htm";
					qs.set("" + npcId, "1");
				}
			}
			else if (cond == 8)
			{
				htmltext = "Sculpture-04.htm";
			}
			else if (cond == 11)
			{
				giveItems(player, TABLET, 1);
				qs.setCond(12, true);
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
			{
				htmltext = "Sculpture-08.htm";
			}
		}
		else if (npcId == SCULPTURE4)
		{
			if (cond == 7)
			{
				if (qs.getInt("" + npcId) == 1)
				{
					htmltext = "Sculpture-02.htm";
				}
				else
				{
					htmltext = "Sculpture-01.htm";
					qs.set("" + npcId, "1");
				}
			}
			else if (cond == 8)
			{
				htmltext = "Sculpture-04.htm";
			}
			else if (cond == 11)
			{
				giveItems(player, TABLET, 1);
				qs.setCond(12, true);
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
			{
				htmltext = "Sculpture-08.htm";
			}
		}
		else if (npcId == KIERRE)
		{
			if (cond == 8)
			{
				htmltext = "32022-01.htm";
			}
			else if (cond == 9)
			{
				htmltext = "32022-03.htm";
			}
		}
		
		return htmltext;
	}
}
