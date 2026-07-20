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
package quests.Q00021_HiddenTruth;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;

public class Q00021_HiddenTruth extends Quest
{
	// NPCs
	private static final int MYSTERIOUS_WIZARD = 31522;
	private static final int TOMBSTONE = 31523;
	private static final int VON_HELLMAN_DUKE = 31524;
	private static final int VON_HELLMAN_PAGE = 31525;
	private static final int BROKEN_BOOKSHELF = 31526;
	private static final int AGRIPEL = 31348;
	private static final int DOMINIC = 31350;
	private static final int BENEDICT = 31349;
	private static final int INNOCENTIN = 31328;
	
	// Items
	private static final int CROSS_OF_EINHASAD = 7140;
	private static final int CROSS_OF_EINHASAD_NEXT_QUEST = 7141;
	
	private static final Location[] PAGE_LOCS =
	{
		new Location(51992, -54424, -3160),
		new Location(52328, -53400, -3160),
		new Location(51928, -51656, -3096)
	};
	
	private Npc _duke;
	private Npc _page;
	
	public Q00021_HiddenTruth()
	{
		super(21, "Hidden Truth");
		registerQuestItems(CROSS_OF_EINHASAD);
		addStartNpc(MYSTERIOUS_WIZARD);
		addTalkId(MYSTERIOUS_WIZARD, TOMBSTONE, VON_HELLMAN_DUKE, VON_HELLMAN_PAGE, BROKEN_BOOKSHELF, AGRIPEL, DOMINIC, BENEDICT, INNOCENTIN);
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
			case "31522-02.htm":
			{
				st.startQuest();
				break;
			}
			case "31523-03.htm":
			{
				st.setCond(2, true);
				spawnTheDuke(player);
				break;
			}
			case "31524-06.htm":
			{
				st.setCond(3, true);
				spawnThePage(player);
				break;
			}
			case "31526-08.htm":
			{
				st.setCond(5, true);
				break;
			}
			case "31526-14.htm":
			{
				st.setCond(6, true);
				giveItems(player, CROSS_OF_EINHASAD, 1);
				break;
			}
			case "1":
			{
				_page.getAI().setIntention(Intention.MOVE_TO, PAGE_LOCS[0]);
				_page.broadcastSay(ChatType.GENERAL, "Follow me...");
				startQuestTimer("2", 5000, _page, player, false);
				return null;
			}
			case "2":
			{
				_page.getAI().setIntention(Intention.MOVE_TO, PAGE_LOCS[1]);
				startQuestTimer("3", 12000, _page, player, false);
				return null;
			}
			case "3":
			{
				_page.getAI().setIntention(Intention.MOVE_TO, PAGE_LOCS[2]);
				startQuestTimer("4", 18000, _page, player, false);
				return null;
			}
			case "4":
			{
				st.set("end_walk", "1");
				_page.broadcastSay(ChatType.GENERAL, "Please check this bookcase, " + player.getName() + ".");
				startQuestTimer("5", 47000, _page, player, false);
				return null;
			}
			case "5":
			{
				_page.broadcastSay(ChatType.GENERAL, "I'm confused! Maybe it's time to go back.");
				return null;
			}
			case "31328-05.htm":
			{
				if (hasQuestItems(player, CROSS_OF_EINHASAD))
				{
					takeItems(player, CROSS_OF_EINHASAD, 1);
					giveItems(player, CROSS_OF_EINHASAD_NEXT_QUEST, 1);
					st.exitQuest(false, true);
				}
				break;
			}
			case "dukeDespawn":
			{
				_duke.deleteMe();
				_duke = null;
				return null;
			}
			case "pageDespawn":
			{
				_page.deleteMe();
				_page = null;
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
				htmltext = (player.getLevel() < 63) ? "31522-03.htm" : "31522-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MYSTERIOUS_WIZARD:
					{
						htmltext = "31522-05.htm";
						break;
					}
					case TOMBSTONE:
					{
						if (cond == 1)
						{
							htmltext = "31523-01.htm";
						}
						else if ((cond == 2) || (cond == 3))
						{
							htmltext = "31523-04.htm";
							spawnTheDuke(player);
						}
						else if (cond > 3)
						{
							htmltext = "31523-04.htm";
						}
						break;
					}
					case VON_HELLMAN_DUKE:
					{
						if (cond == 2)
						{
							htmltext = "31524-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "31524-07.htm";
							spawnThePage(player);
						}
						else if (cond > 3)
						{
							htmltext = "31524-07a.htm";
						}
						break;
					}
					case VON_HELLMAN_PAGE:
					{
						if (cond == 3)
						{
							if (st.getInt("end_walk") == 1)
							{
								htmltext = "31525-02.htm";
								st.setCond(4, true);
							}
							else
							{
								htmltext = "31525-01.htm";
							}
						}
						else if (cond == 4)
						{
							htmltext = "31525-02.htm";
						}
						break;
					}
					case BROKEN_BOOKSHELF:
					{
						if (((cond == 3) && (st.getInt("end_walk") == 1)) || (cond == 4))
						{
							htmltext = "31526-01.htm";
							st.setCond(5, true);
							
							if (_page != null)
							{
								cancelQuestTimer("5", _page, player);
								cancelQuestTimer("pageDespawn", _page, player);
								_page.deleteMe();
								_page = null;
							}
							
							if (_duke != null)
							{
								cancelQuestTimer("dukeDespawn", _duke, player);
								_duke.deleteMe();
								_duke = null;
							}
						}
						else if (cond == 5)
						{
							htmltext = "31526-10.htm";
						}
						else if (cond > 5)
						{
							htmltext = "31526-15.htm";
						}
						break;
					}
					case AGRIPEL:
					case BENEDICT:
					case DOMINIC:
					{
						if (((cond == 6) || (cond == 7)) && hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							final int npcId = npc.getId();
							
							// For cond 6, make checks until cond 7 is activated.
							if (cond == 6)
							{
								int npcId1 = 0;
								int npcId2 = 0;
								if (npcId == AGRIPEL)
								{
									npcId1 = BENEDICT;
									npcId2 = DOMINIC;
								}
								else if (npcId == BENEDICT)
								{
									npcId1 = AGRIPEL;
									npcId2 = DOMINIC;
								}
								else if (npcId == DOMINIC)
								{
									npcId1 = AGRIPEL;
									npcId2 = BENEDICT;
								}
								
								if ((st.getInt(String.valueOf(npcId1)) == 1) && (st.getInt(String.valueOf(npcId2)) == 1))
								{
									st.setCond(7, true);
								}
								else
								{
									st.set(String.valueOf(npcId), "1");
								}
							}
							
							htmltext = npcId + "-01.htm";
						}
						break;
					}
					case INNOCENTIN:
					{
						if ((cond == 7) && hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							htmltext = "31328-01.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				if (npc.getId() == INNOCENTIN)
				{
					htmltext = "31328-06.htm";
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
	
	private void spawnTheDuke(Player player)
	{
		if (_duke == null)
		{
			_duke = addSpawn(VON_HELLMAN_DUKE, 51432, -54570, -3136, 0, false, 0);
			_duke.broadcastSay(ChatType.GENERAL, "Who awoke me?");
			startQuestTimer("dukeDespawn", 300000, _duke, player, false);
		}
	}
	
	private void spawnThePage(Player player)
	{
		if (_page == null)
		{
			_page = addSpawn(VON_HELLMAN_PAGE, 51608, -54520, -3168, 0, false, 0);
			_page.broadcastSay(ChatType.GENERAL, "My master has instructed me to be your guide, " + player.getName() + ".");
			startQuestTimer("1", 4000, _page, player, false);
			startQuestTimer("pageDespawn", 90000, _page, player, false);
		}
	}
}
