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
package quests.Q00603_DaimonTheWhiteEyedPart1;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00603_DaimonTheWhiteEyedPart1 extends Quest
{
	// NPCs
	private static final int EYE_OF_ARGOS = 31683;
	private static final int MYSTERIOUS_TABLET_1 = 31548;
	private static final int MYSTERIOUS_TABLET_2 = 31549;
	private static final int MYSTERIOUS_TABLET_3 = 31550;
	private static final int MYSTERIOUS_TABLET_4 = 31551;
	private static final int MYSTERIOUS_TABLET_5 = 31552;
	
	// Monsters
	private static final int CANYON_BANDERSNATCH_SLAVE = 21297;
	private static final int BUFFALO_SLAVE = 21299;
	private static final int GRENDEL_SLAVE = 21304;
	
	// Items
	private static final int EVIL_SPIRIT_BEADS = 7190;
	private static final int BROKEN_CRYSTAL = 7191;
	private static final int UNFINISHED_SUMMON_CRYSTAL = 7192;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(CANYON_BANDERSNATCH_SLAVE, 500000);
		CHANCES.put(BUFFALO_SLAVE, 519000);
		CHANCES.put(GRENDEL_SLAVE, 673000);
	}
	
	public Q00603_DaimonTheWhiteEyedPart1()
	{
		super(603, "Daimon the White-Eyed - Part 1");
		registerQuestItems(EVIL_SPIRIT_BEADS, BROKEN_CRYSTAL);
		addStartNpc(EYE_OF_ARGOS);
		addTalkId(EYE_OF_ARGOS, MYSTERIOUS_TABLET_1, MYSTERIOUS_TABLET_2, MYSTERIOUS_TABLET_3, MYSTERIOUS_TABLET_4, MYSTERIOUS_TABLET_5);
		addKillId(BUFFALO_SLAVE, GRENDEL_SLAVE, CANYON_BANDERSNATCH_SLAVE);
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
			case "31683-03.htm":
			{
				st.startQuest();
				break;
			}
			case "31683-06.htm":
			{
				if (getQuestItemsCount(player, BROKEN_CRYSTAL) > 4)
				{
					st.setCond(7, true);
					takeItems(player, BROKEN_CRYSTAL, -1);
				}
				else
				{
					htmltext = "31683-07.htm";
				}
				break;
			}
			case "31683-10.htm":
			{
				if (getQuestItemsCount(player, EVIL_SPIRIT_BEADS) > 199)
				{
					takeItems(player, EVIL_SPIRIT_BEADS, -1);
					giveItems(player, UNFINISHED_SUMMON_CRYSTAL, 1);
					st.exitQuest(true, true);
				}
				else
				{
					st.setCond(7);
					htmltext = "31683-11.htm";
				}
				break;
			}
			case "31548-02.htm":
			{
				st.setCond(2, true);
				giveItems(player, BROKEN_CRYSTAL, 1);
				break;
			}
			case "31549-02.htm":
			{
				st.setCond(3, true);
				giveItems(player, BROKEN_CRYSTAL, 1);
				break;
			}
			case "31550-02.htm":
			{
				st.setCond(4, true);
				giveItems(player, BROKEN_CRYSTAL, 1);
				break;
			}
			case "31551-02.htm":
			{
				st.setCond(5, true);
				giveItems(player, BROKEN_CRYSTAL, 1);
				break;
			}
			case "31552-02.htm":
			{
				st.setCond(6, true);
				giveItems(player, BROKEN_CRYSTAL, 1);
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
				htmltext = (player.getLevel() < 73) ? "31683-02.htm" : "31683-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case EYE_OF_ARGOS:
					{
						if (cond < 6)
						{
							htmltext = "31683-04.htm";
						}
						else if (cond == 6)
						{
							htmltext = "31683-05.htm";
						}
						else if (cond == 7)
						{
							htmltext = "31683-08.htm";
						}
						else if (cond == 8)
						{
							htmltext = "31683-09.htm";
						}
						break;
					}
					case MYSTERIOUS_TABLET_1:
					{
						if (cond == 1)
						{
							htmltext = "31548-01.htm";
						}
						else
						{
							htmltext = "31548-03.htm";
						}
						break;
					}
					case MYSTERIOUS_TABLET_2:
					{
						if (cond == 2)
						{
							htmltext = "31549-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "31549-03.htm";
						}
						break;
					}
					case MYSTERIOUS_TABLET_3:
					{
						if (cond == 3)
						{
							htmltext = "31550-01.htm";
						}
						else if (cond > 3)
						{
							htmltext = "31550-03.htm";
						}
						break;
					}
					case MYSTERIOUS_TABLET_4:
					{
						if (cond == 4)
						{
							htmltext = "31551-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "31551-03.htm";
						}
						break;
					}
					case MYSTERIOUS_TABLET_5:
					{
						if (cond == 5)
						{
							htmltext = "31552-01.htm";
						}
						else if (cond > 5)
						{
							htmltext = "31552-03.htm";
						}
						break;
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
		final Player partyMember = getRandomPartyMember(player, 7);
		if (partyMember == null)
		{
			return;
		}
		
		final QuestState st = getQuestState(partyMember, false);
		if (st == null)
		{
			return;
		}
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			giveItems(partyMember, EVIL_SPIRIT_BEADS, 1);
			if (getQuestItemsCount(partyMember, EVIL_SPIRIT_BEADS) < 200)
			{
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(8, true);
			}
		}
	}
}
