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
package quests.Q00363_SorrowfulSoundOfFlute;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00363_SorrowfulSoundOfFlute extends Quest
{
	// NPCs
	private static final int NANARIN = 30956;
	private static final int OPIX = 30595;
	private static final int ALDO = 30057;
	private static final int RANSPO = 30594;
	private static final int HOLVAS = 30058;
	private static final int BARBADO = 30959;
	private static final int POITAN = 30458;
	
	// Items
	private static final int NANARIN_FLUTE = 4319;
	private static final int BLACK_BEER = 4320;
	private static final int CLOTHES = 4318;
	
	// Reward
	private static final int THEME_OF_SOLITUDE = 4420;
	
	public Q00363_SorrowfulSoundOfFlute()
	{
		super(363, "Sorrowful Sound of Flute");
		registerQuestItems(NANARIN_FLUTE, BLACK_BEER, CLOTHES);
		addStartNpc(NANARIN);
		addTalkId(NANARIN, OPIX, ALDO, RANSPO, HOLVAS, BARBADO, POITAN);
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
			case "30956-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30956-05.htm":
			{
				st.setCond(3, true);
				giveItems(player, CLOTHES, 1);
				break;
			}
			case "30956-06.htm":
			{
				st.setCond(3, true);
				giveItems(player, NANARIN_FLUTE, 1);
				break;
			}
			case "30956-07.htm":
			{
				st.setCond(3, true);
				giveItems(player, BLACK_BEER, 1);
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
				htmltext = (player.getLevel() < 15) ? "30956-03.htm" : "30956-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case NANARIN:
					{
						if (cond == 1)
						{
							htmltext = "30956-02.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30956-04.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30956-08.htm";
						}
						else if (cond == 4)
						{
							if (st.getInt("success") == 1)
							{
								htmltext = "30956-09.htm";
								giveItems(player, THEME_OF_SOLITUDE, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
							}
							else
							{
								htmltext = "30956-10.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_GIVEUP);
							}
							
							st.exitQuest(true);
						}
						break;
					}
					case OPIX:
					case POITAN:
					case ALDO:
					case RANSPO:
					case HOLVAS:
					{
						htmltext = npc.getId() + "-01.htm";
						if (cond == 1)
						{
							st.setCond(2, true);
						}
						break;
					}
					case BARBADO:
					{
						if (cond == 3)
						{
							st.setCond(4, true);
							
							if (hasQuestItems(player, NANARIN_FLUTE))
							{
								htmltext = "30959-02.htm";
								st.set("success", "1");
							}
							else
							{
								htmltext = "30959-01.htm";
							}
							
							takeItems(player, BLACK_BEER, -1);
							takeItems(player, CLOTHES, -1);
							takeItems(player, NANARIN_FLUTE, -1);
						}
						else if (cond == 4)
						{
							htmltext = "30959-03.htm";
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
