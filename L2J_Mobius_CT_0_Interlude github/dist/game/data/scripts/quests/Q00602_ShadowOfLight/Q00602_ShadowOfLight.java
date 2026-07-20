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
package quests.Q00602_ShadowOfLight;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00602_ShadowOfLight extends Quest
{
	private static final int EYE_OF_DARKNESS = 7189;
	private static final int[][] REWARDS =
	{
		// @formatter:off
		{6699, 40000, 120000, 20000, 20},
		{6698, 60000, 110000, 15000, 40},
		{6700, 40000, 150000, 10000, 50},
		{0, 100000, 140000, 11250, 100}
		// @formatter:on
	};
	
	public Q00602_ShadowOfLight()
	{
		super(602, "Shadow of Light");
		registerQuestItems(EYE_OF_DARKNESS);
		addStartNpc(31683); // Eye of Argos
		addTalkId(31683);
		addKillId(21299, 21304);
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
		
		if (event.equals("31683-02.htm"))
		{
			if (player.getLevel() < 68)
			{
				htmltext = "31683-02a.htm";
			}
			else
			{
				st.startQuest();
			}
		}
		else if (event.equals("31683-05.htm"))
		{
			takeItems(player, EYE_OF_DARKNESS, -1);
			
			final int random = getRandom(100);
			for (int[] element : REWARDS)
			{
				if (random < element[4])
				{
					giveAdena(player, element[1], true);
					
					if (element[0] != 0)
					{
						giveItems(player, element[0], 3);
					}
					
					addExpAndSp(player, element[2], element[3]);
					break;
				}
			}
			
			st.exitQuest(true, true);
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
				htmltext = "31683-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "31683-03.htm";
				}
				else if (cond == 2)
				{
					htmltext = "31683-04.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState qs = getRandomPartyMemberState(player, 1, 3, npc);
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
		
		if (getRandom(100) < (npc.getId() == 21299 ? 45 : 50))
		{
			giveItems(partyMember, EYE_OF_DARKNESS, 1);
			if (getQuestItemsCount(partyMember, EYE_OF_DARKNESS) < 100)
			{
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(2, true);
			}
		}
	}
}
