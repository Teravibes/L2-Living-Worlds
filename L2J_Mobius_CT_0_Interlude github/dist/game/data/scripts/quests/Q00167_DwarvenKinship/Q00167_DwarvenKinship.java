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
package quests.Q00167_DwarvenKinship;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00167_DwarvenKinship extends Quest
{
	// NPCs
	private static final int CARLON = 30350;
	private static final int NORMAN = 30210;
	private static final int HAPROCK = 30255;
	
	// Items
	private static final int CARLON_LETTER = 1076;
	private static final int NORMAN_LETTER = 1106;
	
	public Q00167_DwarvenKinship()
	{
		super(167, "Dwarven Kinship");
		registerQuestItems(CARLON_LETTER, NORMAN_LETTER);
		addStartNpc(CARLON);
		addTalkId(CARLON, HAPROCK, NORMAN);
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
			case "30350-04.htm":
			{
				st.startQuest();
				giveItems(player, CARLON_LETTER, 1);
				break;
			}
			case "30255-03.htm":
			{
				st.setCond(2);
				takeItems(player, CARLON_LETTER, 1);
				giveItems(player, NORMAN_LETTER, 1);
				giveAdena(player, 2000, true);
				break;
			}
			case "30255-04.htm":
			{
				takeItems(player, CARLON_LETTER, 1);
				giveAdena(player, 3000, true);
				st.exitQuest(false, true);
				break;
			}
			case "30210-02.htm":
			{
				takeItems(player, NORMAN_LETTER, 1);
				giveAdena(player, 20000, true);
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
				htmltext = (player.getLevel() < 15) ? "30350-02.htm" : "30350-03.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case CARLON:
					{
						if (cond == 1)
						{
							htmltext = "30350-05.htm";
						}
						break;
					}
					case HAPROCK:
					{
						if (cond == 1)
						{
							htmltext = "30255-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30255-05.htm";
						}
						break;
					}
					case NORMAN:
					{
						if (cond == 2)
						{
							htmltext = "30210-01.htm";
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
