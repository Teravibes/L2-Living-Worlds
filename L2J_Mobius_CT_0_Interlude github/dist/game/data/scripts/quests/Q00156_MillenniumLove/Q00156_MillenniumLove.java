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
package quests.Q00156_MillenniumLove;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00156_MillenniumLove extends Quest
{
	// NPCs
	private static final int LILITH = 30368;
	private static final int BAENEDES = 30369;
	
	// Items
	private static final int LILITH_LETTER = 1022;
	private static final int THEON_DIARY = 1023;
	
	public Q00156_MillenniumLove()
	{
		super(156, "Millennium Love");
		registerQuestItems(LILITH_LETTER, THEON_DIARY);
		addStartNpc(LILITH);
		addTalkId(LILITH, BAENEDES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30368-04.htm":
			{
				st.startQuest();
				giveItems(player, LILITH_LETTER, 1);
				break;
			}
			case "30369-02.htm":
			{
				st.setCond(2, true);
				takeItems(player, LILITH_LETTER, 1);
				giveItems(player, THEON_DIARY, 1);
				break;
			}
			case "30369-03.htm":
			{
				takeItems(player, LILITH_LETTER, 1);
				addExpAndSp(player, 3000, 0);
				st.exitQuest(false, true);
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
				htmltext = (player.getLevel() < 15) ? "30368-00.htm" : "30368-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case LILITH:
					{
						if (hasQuestItems(player, LILITH_LETTER))
						{
							htmltext = "30368-05.htm";
						}
						else if (hasQuestItems(player, THEON_DIARY))
						{
							htmltext = "30368-06.htm";
							takeItems(player, THEON_DIARY, 1);
							giveItems(player, 5250, 1);
							addExpAndSp(player, 3000, 0);
							st.exitQuest(false, true);
						}
						break;
					}
					case BAENEDES:
					{
						if (hasQuestItems(player, LILITH_LETTER))
						{
							htmltext = "30369-01.htm";
						}
						else if (hasQuestItems(player, THEON_DIARY))
						{
							htmltext = "30369-04.htm";
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
}
