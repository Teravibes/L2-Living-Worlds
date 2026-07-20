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
package quests.Q00298_LizardmensConspiracy;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00298_LizardmensConspiracy extends Quest
{
	// NPCs
	private static final int PRAGA = 30333;
	private static final int ROHMER = 30344;
	
	// Items
	private static final int PATROL_REPORT = 7182;
	private static final int WHITE_GEM = 7183;
	private static final int RED_GEM = 7184;
	
	public Q00298_LizardmensConspiracy()
	{
		super(298, "Lizardmen's Conspiracy");
		registerQuestItems(PATROL_REPORT, WHITE_GEM, RED_GEM);
		addStartNpc(PRAGA);
		addTalkId(PRAGA, ROHMER);
		addKillId(20926, 20927, 20922, 20923, 20924);
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
			case "30333-1.htm":
			{
				st.startQuest();
				giveItems(player, PATROL_REPORT, 1);
				break;
			}
			case "30344-1.htm":
			{
				st.setCond(2, true);
				takeItems(player, PATROL_REPORT, 1);
				break;
			}
			case "30344-4.htm":
			{
				if (st.isCond(3))
				{
					htmltext = "30344-3.htm";
					takeItems(player, WHITE_GEM, -1);
					takeItems(player, RED_GEM, -1);
					addExpAndSp(player, 0, 42000);
					st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < 25) ? "30333-0b.htm" : "30333-0a.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case PRAGA:
					{
						htmltext = "30333-2.htm";
						break;
					}
					case ROHMER:
					{
						if (st.isCond(1))
						{
							htmltext = (hasQuestItems(player, PATROL_REPORT)) ? "30344-0.htm" : "30344-0a.htm";
						}
						else
						{
							htmltext = "30344-2.htm";
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
		final QuestState qs = getRandomPartyMemberState(player, 2, 3, npc);
		if (qs == null)
		{
			return;
		}
		
		final Player partyMember = qs.getPlayer();
		
		final QuestState st = getQuestState(partyMember, false);
		if (st == null)
		{
			return;
		}
		
		switch (npc.getId())
		{
			case 20922:
			{
				if (getRandom(100) < 40)
				{
					giveItems(player, WHITE_GEM, 1);
					if (getQuestItemsCount(player, WHITE_GEM) < 50)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (getQuestItemsCount(player, RED_GEM) >= 50)
					{
						st.setCond(3, true);
					}
				}
				break;
			}
			case 20923:
			{
				if (getRandom(100) < 45)
				{
					giveItems(player, WHITE_GEM, 1);
					if (getQuestItemsCount(player, WHITE_GEM) < 50)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (getQuestItemsCount(player, RED_GEM) >= 50)
					{
						st.setCond(3, true);
					}
				}
				break;
			}
			case 20924:
			{
				if (getRandom(100) < 35)
				{
					giveItems(player, WHITE_GEM, 1);
					if (getQuestItemsCount(player, WHITE_GEM) < 50)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (getQuestItemsCount(player, RED_GEM) >= 50)
					{
						st.setCond(3, true);
					}
				}
				break;
			}
			case 20926:
			case 20927:
			{
				if (getRandom(100) < 40)
				{
					giveItems(player, RED_GEM, 1);
					if (getQuestItemsCount(player, RED_GEM) < 50)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (getQuestItemsCount(player, WHITE_GEM) >= 50)
					{
						st.setCond(3, true);
					}
				}
				break;
			}
		}
	}
}
