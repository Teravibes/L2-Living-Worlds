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
package quests.Q00623_TheFinestFood;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00623_TheFinestFood extends Quest
{
	// NPC
	private static final int JEREMY = 31521;
	
	// Monsters
	private static final int FLAVA = 21316;
	private static final int BUFFALO = 21315;
	private static final int ANTELOPE = 21318;
	
	// Items
	private static final int LEAF_OF_FLAVA = 7199;
	private static final int BUFFALO_MEAT = 7200;
	private static final int ANTELOPE_HORN = 7201;
	
	public Q00623_TheFinestFood()
	{
		super(623, "The Finest Food");
		registerQuestItems(LEAF_OF_FLAVA, BUFFALO_MEAT, ANTELOPE_HORN);
		addStartNpc(JEREMY);
		addTalkId(JEREMY);
		addKillId(FLAVA, BUFFALO, ANTELOPE);
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
		
		if (event.equals("31521-02.htm"))
		{
			if (player.getLevel() >= 71)
			{
				st.startQuest();
			}
			else
			{
				htmltext = "31521-03.htm";
			}
		}
		else if (event.equals("31521-05.htm"))
		{
			takeItems(player, LEAF_OF_FLAVA, -1);
			takeItems(player, BUFFALO_MEAT, -1);
			takeItems(player, ANTELOPE_HORN, -1);
			
			final int luck = getRandom(100);
			if (luck < 11)
			{
				giveAdena(player, 25000, true);
				giveItems(player, 6849, 1);
			}
			else if (luck < 23)
			{
				giveAdena(player, 65000, true);
				giveItems(player, 6847, 1);
			}
			else if (luck < 33)
			{
				giveAdena(player, 25000, true);
				giveItems(player, 6851, 1);
			}
			else
			{
				giveAdena(player, 73000, true);
				addExpAndSp(player, 230000, 18250);
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
				htmltext = "31521-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "31521-06.htm";
				}
				else if (cond == 2)
				{
					if ((getQuestItemsCount(player, LEAF_OF_FLAVA) >= 100) && (getQuestItemsCount(player, BUFFALO_MEAT) >= 100) && (getQuestItemsCount(player, ANTELOPE_HORN) >= 100))
					{
						htmltext = "31521-04.htm";
					}
					else
					{
						htmltext = "31521-07.htm";
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
		
		switch (npc.getId())
		{
			case FLAVA:
			{
				if (getQuestItemsCount(partyMember, LEAF_OF_FLAVA) < 100)
				{
					giveItems(partyMember, LEAF_OF_FLAVA, 1);
					if ((getQuestItemsCount(partyMember, LEAF_OF_FLAVA) >= 100) && (getQuestItemsCount(partyMember, BUFFALO_MEAT) >= 100) && (getQuestItemsCount(partyMember, ANTELOPE_HORN) >= 100))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case BUFFALO:
			{
				if (getQuestItemsCount(partyMember, BUFFALO_MEAT) < 100)
				{
					giveItems(partyMember, BUFFALO_MEAT, 1);
					if ((getQuestItemsCount(partyMember, BUFFALO_MEAT) >= 100) && (getQuestItemsCount(partyMember, LEAF_OF_FLAVA) >= 100) && (getQuestItemsCount(partyMember, ANTELOPE_HORN) >= 100))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case ANTELOPE:
			{
				if (getQuestItemsCount(partyMember, ANTELOPE_HORN) < 100)
				{
					giveItems(partyMember, ANTELOPE_HORN, 1);
					if ((getQuestItemsCount(partyMember, ANTELOPE_HORN) >= 100) && (getQuestItemsCount(partyMember, LEAF_OF_FLAVA) >= 100) && (getQuestItemsCount(partyMember, BUFFALO_MEAT) >= 100))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
		}
	}
}
