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
package quests.Q00023_LidiasHeart;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;

import quests.Q00022_TragedyInVonHellmannForest.Q00022_TragedyInVonHellmannForest;

public class Q00023_LidiasHeart extends Quest
{
	// NPCs
	private static final int INNOCENTIN = 31328;
	private static final int BROKEN_BOOKSHELF = 31526;
	private static final int GHOST_OF_VON_HELLMANN = 31524;
	private static final int TOMBSTONE = 31523;
	private static final int VIOLET = 31386;
	private static final int BOX = 31530;
	
	// NPC instance
	private Npc _ghost = null;
	
	// Items
	private static final int FOREST_OF_DEADMAN_MAP = 7063;
	private static final int SILVER_KEY = 7149;
	private static final int LIDIA_HAIRPIN = 7148;
	private static final int LIDIA_DIARY = 7064;
	private static final int SILVER_SPEAR = 7150;
	
	public Q00023_LidiasHeart()
	{
		super(23, "Lidia's Heart");
		
		registerQuestItems(FOREST_OF_DEADMAN_MAP, SILVER_KEY, LIDIA_DIARY, SILVER_SPEAR);
		
		addStartNpc(INNOCENTIN);
		addTalkId(INNOCENTIN, BROKEN_BOOKSHELF, GHOST_OF_VON_HELLMANN, VIOLET, BOX, TOMBSTONE);
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
			case "31328-02.htm":
			{
				st.startQuest();
				giveItems(player, FOREST_OF_DEADMAN_MAP, 1);
				giveItems(player, SILVER_KEY, 1);
				break;
			}
			case "31328-06.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "31526-05.htm":
			{
				if (!hasQuestItems(player, LIDIA_HAIRPIN))
				{
					giveItems(player, LIDIA_HAIRPIN, 1);
					if (hasQuestItems(player, LIDIA_DIARY))
					{
						st.setCond(4, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case "31526-11.htm":
			{
				if (!hasQuestItems(player, LIDIA_DIARY))
				{
					giveItems(player, LIDIA_DIARY, 1);
					if (hasQuestItems(player, LIDIA_HAIRPIN))
					{
						st.setCond(4, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case "31328-11.htm":
			{
				if (st.getCond() < 5)
				{
					st.setCond(5, true);
				}
				break;
			}
			case "31328-19.htm":
			{
				st.setCond(6, true);
				break;
			}
			case "31524-04.htm":
			{
				st.setCond(7, true);
				takeItems(player, LIDIA_DIARY, 1);
				break;
			}
			case "31523-02.htm":
			{
				if (_ghost == null)
				{
					_ghost = addSpawn(31524, 51432, -54570, -3136, 0, false, 60000);
					_ghost.broadcastSay(ChatType.GENERAL, "Who awoke me?");
					startQuestTimer("ghost_cleanup", 58000, null, player, false);
				}
				break;
			}
			case "31523-05.htm":
			{
				// Don't launch twice the same task...
				if (getQuestTimer("tomb_digger", null, player) == null)
				{
					startQuestTimer("tomb_digger", 10000, null, player, false);
				}
				break;
			}
			case "tomb_digger":
			{
				htmltext = "31523-06.htm";
				st.setCond(8, true);
				giveItems(player, SILVER_KEY, 1);
				break;
			}
			case "31530-02.htm":
			{
				st.setCond(10, true);
				takeItems(player, SILVER_KEY, 1);
				giveItems(player, SILVER_SPEAR, 1);
				break;
			}
			case "ghost_cleanup":
			{
				_ghost = null;
				return null;
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
				final QuestState st2 = player.getQuestState(Q00022_TragedyInVonHellmannForest.class.getSimpleName());
				if ((st2 != null) && st2.isCompleted())
				{
					if (player.getLevel() >= 64)
					{
						htmltext = "31328-01.htm";
					}
					else
					{
						htmltext = "31328-00a.htm";
					}
				}
				else
				{
					htmltext = "31328-00.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case INNOCENTIN:
					{
						if (cond == 1)
						{
							htmltext = "31328-03.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31328-07.htm";
						}
						else if (cond == 4)
						{
							htmltext = "31328-08.htm";
						}
						else if (cond == 5)
						{
							htmltext = "31328-10.htm";
						}
						else if (cond > 5)
						{
							htmltext = "31328-21.htm";
						}
						break;
					}
					case BROKEN_BOOKSHELF:
					{
						if (cond == 2)
						{
							htmltext = "31526-00.htm";
							st.setCond(3, true);
						}
						else if (cond == 3)
						{
							if (!hasQuestItems(player, LIDIA_DIARY))
							{
								htmltext = (!hasQuestItems(player, LIDIA_HAIRPIN)) ? "31526-02.htm" : "31526-06.htm";
							}
							else if (!hasQuestItems(player, LIDIA_HAIRPIN))
							{
								htmltext = "31526-12.htm";
							}
						}
						else if (cond > 3)
						{
							htmltext = "31526-13.htm";
						}
						break;
					}
					case GHOST_OF_VON_HELLMANN:
					{
						if (cond == 6)
						{
							htmltext = "31524-01.htm";
						}
						else if (cond > 6)
						{
							htmltext = "31524-05.htm";
						}
						break;
					}
					case TOMBSTONE:
					{
						if (cond == 6)
						{
							htmltext = (_ghost == null) ? "31523-01.htm" : "31523-03.htm";
						}
						else if (cond == 7)
						{
							htmltext = "31523-04.htm";
						}
						else if (cond > 7)
						{
							htmltext = "31523-06.htm";
						}
						break;
					}
					case VIOLET:
					{
						if (cond == 8)
						{
							htmltext = "31386-01.htm";
							st.setCond(9, true);
						}
						else if (cond == 9)
						{
							htmltext = "31386-02.htm";
						}
						else if (cond == 10)
						{
							if (hasQuestItems(player, SILVER_SPEAR))
							{
								htmltext = "31386-03.htm";
								takeItems(player, SILVER_SPEAR, 1);
								giveAdena(player, 100000, true);
								st.exitQuest(false, true);
							}
							else
							{
								htmltext = "31386-02.htm";
								st.setCond(9);
							}
						}
						break;
					}
					case BOX:
					{
						if (cond == 9)
						{
							htmltext = "31530-01.htm";
						}
						else if (cond == 10)
						{
							htmltext = "31530-03.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (npc.getId() == VIOLET)
				{
					htmltext = "31386-04.htm";
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
		}
		
		return htmltext;
	}
}
