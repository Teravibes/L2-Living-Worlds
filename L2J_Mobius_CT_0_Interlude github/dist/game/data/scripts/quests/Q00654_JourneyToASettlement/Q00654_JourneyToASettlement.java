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
package quests.Q00654_JourneyToASettlement;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00119_LastImperialPrince.Q00119_LastImperialPrince;

/**
 * @author Skache
 */
public class Q00654_JourneyToASettlement extends Quest
{
	// Items
	private static final int ANTELOPE_SKIN = 8072;
	private static final int FORCE_FIELD_REMOVAL_SCROLL = 8073;
	
	// NPCs
	private static final int NAMELESS_SPIRIT = 31453;
	private static final int CANYON_ANTELOPE = 21294;
	private static final int CANYON_ANTELOPE_SLAVE = 21295;
	
	// Drop
	private static final int DROP_CHANCE = 30; // Retail 30%
	private static final int DROP_AMOUNT = 1; // Retail 1
	
	public Q00654_JourneyToASettlement()
	{
		super(654, "Journey to a Settlement");
		registerQuestItems(ANTELOPE_SKIN);
		addStartNpc(NAMELESS_SPIRIT);
		addTalkId(NAMELESS_SPIRIT);
		addKillId(CANYON_ANTELOPE, CANYON_ANTELOPE_SLAVE);
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
			case "31453-02.htm":
			{
				st.startQuest();
				break;
			}
			case "31453-03.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "31453-06.htm":
			{
				takeItems(player, ANTELOPE_SKIN, -1);
				giveItems(player, FORCE_FIELD_REMOVAL_SCROLL, 1);
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
				final QuestState prevSt = player.getQuestState(Q00119_LastImperialPrince.class.getSimpleName());
				htmltext = ((prevSt == null) || !prevSt.isCompleted() || (player.getLevel() < 74)) ? "31453-00.htm" : "31453-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "31453-02.htm";
				}
				else if (cond == 2)
				{
					htmltext = "31453-04.htm";
				}
				else if (cond == 3)
				{
					htmltext = "31453-05.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs == null) || !qs.isCond(2))
		{
			return;
		}
		
		if ((getRandom(100) < DROP_CHANCE))
		{
			giveItems(player, ANTELOPE_SKIN, DROP_AMOUNT);
			qs.setCond(3, true);
		}
	}
}
