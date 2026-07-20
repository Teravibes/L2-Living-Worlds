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
package quests.Q00637_ThroughOnceMore;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00637_ThroughOnceMore extends Quest
{
	// NPC
	private static final int FLAURON = 32010;
	
	// Items
	private static final int FADED_VISITOR_MARK = 8065;
	private static final int NECROMANCER_HEART = 8066;
	
	// Reward
	private static final int PAGAN_MARK = 8067;
	
	public Q00637_ThroughOnceMore()
	{
		super(637, "Through the Gate Once More");
		registerQuestItems(NECROMANCER_HEART);
		addStartNpc(FLAURON);
		addTalkId(FLAURON);
		addKillId(21565, 21566, 21567);
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
		
		if (event.equals("32010-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("32010-10.htm"))
		{
			st.exitQuest(true);
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
				if ((player.getLevel() < 73) || !hasQuestItems(player, FADED_VISITOR_MARK))
				{
					htmltext = "32010-01a.htm";
				}
				else if (hasQuestItems(player, PAGAN_MARK))
				{
					htmltext = "32010-00.htm";
				}
				else
				{
					htmltext = "32010-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(2))
				{
					if (getQuestItemsCount(player, NECROMANCER_HEART) == 10)
					{
						htmltext = "32010-06.htm";
						takeItems(player, FADED_VISITOR_MARK, 1);
						takeItems(player, NECROMANCER_HEART, -1);
						giveItems(player, PAGAN_MARK, 1);
						giveItems(player, 8273, 10);
						st.exitQuest(true, true);
					}
					else
					{
						st.setCond(1);
					}
				}
				else
				{
					htmltext = "32010-05.htm";
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
		
		if (getRandom(10) < 4)
		{
			giveItems(partyMember, NECROMANCER_HEART, 1);
			if (getQuestItemsCount(partyMember, NECROMANCER_HEART) < 10)
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
