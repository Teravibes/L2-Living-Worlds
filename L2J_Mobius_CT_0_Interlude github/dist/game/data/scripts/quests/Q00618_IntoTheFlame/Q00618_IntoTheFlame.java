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
package quests.Q00618_IntoTheFlame;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00618_IntoTheFlame extends Quest
{
	// NPCs
	private static final int KLEIN = 31540;
	private static final int HILDA = 31271;
	
	// Items
	private static final int VACUALITE_ORE = 7265;
	private static final int VACUALITE = 7266;
	
	// Reward
	private static final int FLOATING_STONE = 7267;
	
	public Q00618_IntoTheFlame()
	{
		super(618, "Into the Flame");
		registerQuestItems(VACUALITE_ORE, VACUALITE);
		addStartNpc(KLEIN);
		addTalkId(KLEIN, HILDA);
		
		// Kookaburras, Bandersnatches, Grendels
		addKillId(21274, 21275, 21276, 21277, 21282, 21283, 21284, 21285, 21290, 21291, 21292, 21293);
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
			case "31540-03.htm":
			{
				st.startQuest();
				break;
			}
			case "31540-05.htm":
			{
				takeItems(player, VACUALITE, 1);
				giveItems(player, FLOATING_STONE, 1);
				st.exitQuest(true, true);
				break;
			}
			case "31271-02.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "31271-05.htm":
			{
				st.setCond(4, true);
				takeItems(player, VACUALITE_ORE, -1);
				giveItems(player, VACUALITE, 1);
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
				htmltext = (player.getLevel() < 60) ? "31540-01.htm" : "31540-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case KLEIN:
					{
						htmltext = (cond == 4) ? "31540-04.htm" : "31540-03.htm";
						break;
					}
					case HILDA:
					{
						if (cond == 1)
						{
							htmltext = "31271-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31271-03.htm";
						}
						else if (cond == 3)
						{
							htmltext = "31271-04.htm";
						}
						else if (cond == 4)
						{
							htmltext = "31271-06.htm";
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
		
		if (getRandomBoolean())
		{
			giveItems(partyMember, VACUALITE_ORE, 1);
			if (getQuestItemsCount(partyMember, VACUALITE_ORE) < 50)
			{
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(3, true);
			}
		}
	}
}
