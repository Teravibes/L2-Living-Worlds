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
package quests.Q00362_BardsMandolin;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00362_BardsMandolin extends Quest
{
	// NPCs
	private static final int SWAN = 30957;
	private static final int NANARIN = 30956;
	private static final int GALION = 30958;
	private static final int WOODROW = 30837;
	
	// Items
	private static final int SWAN_FLUTE = 4316;
	private static final int SWAN_LETTER = 4317;
	
	public Q00362_BardsMandolin()
	{
		super(362, "Bard's Mandolin");
		registerQuestItems(SWAN_FLUTE, SWAN_LETTER);
		addStartNpc(SWAN);
		addTalkId(SWAN, NANARIN, GALION, WOODROW);
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
		
		if (event.equals("30957-3.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30957-7.htm") || event.equals("30957-8.htm"))
		{
			giveAdena(player, 10000, true);
			giveItems(player, 4410, 1);
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
				htmltext = (player.getLevel() < 15) ? "30957-2.htm" : "30957-1.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case SWAN:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "30957-4.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30957-5.htm";
							st.setCond(4, true);
							giveItems(player, SWAN_LETTER, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30957-5a.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30957-6.htm";
						}
						break;
					}
					case WOODROW:
					{
						if (cond == 1)
						{
							htmltext = "30837-1.htm";
							st.setCond(2, true);
						}
						else if (cond == 2)
						{
							htmltext = "30837-2.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30837-3.htm";
						}
						break;
					}
					case GALION:
					{
						if (cond == 2)
						{
							htmltext = "30958-1.htm";
							st.setCond(3);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							giveItems(player, SWAN_FLUTE, 1);
						}
						else if (cond > 2)
						{
							htmltext = "30958-2.htm";
						}
						break;
					}
					case NANARIN:
					{
						if (cond == 4)
						{
							htmltext = "30956-1.htm";
							st.setCond(5, true);
							takeItems(player, SWAN_FLUTE, 1);
							takeItems(player, SWAN_LETTER, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30956-2.htm";
						}
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
