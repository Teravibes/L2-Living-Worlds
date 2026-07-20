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
package quests.Q00413_PathOfTheShillienOracle;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00413_PathOfTheShillienOracle extends Quest
{
	// NPCs
	private static final int SIDRA = 30330;
	private static final int ADONIUS = 30375;
	private static final int TALBOT = 30377;
	
	// Items
	private static final int SIDRA_LETTER = 1262;
	private static final int BLANK_SHEET = 1263;
	private static final int BLOODY_RUNE = 1264;
	private static final int GARMIEL_BOOK = 1265;
	private static final int PRAYER_OF_ADONIUS = 1266;
	private static final int PENITENT_MARK = 1267;
	private static final int ASHEN_BONES = 1268;
	private static final int ANDARIEL_BOOK = 1269;
	private static final int ORB_OF_ABYSS = 1270;
	
	public Q00413_PathOfTheShillienOracle()
	{
		super(413, "Path to a Shillien Oracle");
		registerQuestItems(SIDRA_LETTER, BLANK_SHEET, BLOODY_RUNE, GARMIEL_BOOK, PRAYER_OF_ADONIUS, PENITENT_MARK, ASHEN_BONES, ANDARIEL_BOOK);
		addStartNpc(SIDRA);
		addTalkId(SIDRA, ADONIUS, TALBOT);
		addKillId(20776, 20457, 20458, 20514, 20515);
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
			case "30330-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.DARK_MAGE)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.SHILLIEN_ORACLE) ? "30330-02a.htm" : "30330-03.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30330-02.htm";
				}
				else if (hasQuestItems(player, ORB_OF_ABYSS))
				{
					htmltext = "30330-04.htm";
				}
				break;
			}
			case "30330-06.htm":
			{
				st.startQuest();
				giveItems(player, SIDRA_LETTER, 1);
				break;
			}
			case "30377-02.htm":
			{
				st.setCond(2, true);
				takeItems(player, SIDRA_LETTER, 1);
				giveItems(player, BLANK_SHEET, 5);
				break;
			}
			case "30375-04.htm":
			{
				st.setCond(5, true);
				takeItems(player, PRAYER_OF_ADONIUS, 1);
				giveItems(player, PENITENT_MARK, 1);
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
				htmltext = "30330-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case SIDRA:
					{
						if (cond == 1)
						{
							htmltext = "30330-07.htm";
						}
						else if ((cond > 1) && (cond < 4))
						{
							htmltext = "30330-08.htm";
						}
						else if ((cond > 3) && (cond < 7))
						{
							htmltext = "30330-09.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30330-10.htm";
							takeItems(player, ANDARIEL_BOOK, 1);
							takeItems(player, GARMIEL_BOOK, 1);
							giveItems(player, ORB_OF_ABYSS, 1);
							addExpAndSp(player, 3200, 3120);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case TALBOT:
					{
						if (cond == 1)
						{
							htmltext = "30377-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = (hasQuestItems(player, BLOODY_RUNE)) ? "30377-04.htm" : "30377-03.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30377-05.htm";
							st.setCond(4, true);
							takeItems(player, BLOODY_RUNE, -1);
							giveItems(player, GARMIEL_BOOK, 1);
							giveItems(player, PRAYER_OF_ADONIUS, 1);
						}
						else if ((cond > 3) && (cond < 7))
						{
							htmltext = "30377-06.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30377-07.htm";
						}
						break;
					}
					case ADONIUS:
					{
						if (cond == 4)
						{
							htmltext = "30375-01.htm";
						}
						else if (cond == 5)
						{
							htmltext = (hasQuestItems(player, ASHEN_BONES)) ? "30375-05.htm" : "30375-06.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30375-07.htm";
							st.setCond(7, true);
							takeItems(player, ASHEN_BONES, -1);
							takeItems(player, PENITENT_MARK, -1);
							giveItems(player, ANDARIEL_BOOK, 1);
						}
						else if (cond == 7)
						{
							htmltext = "30375-08.htm";
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
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		if (npc.getId() == 20776)
		{
			if (st.isCond(2))
			{
				if (getQuestItemsCount(player, BLOODY_RUNE) < 5)
				{
					takeItems(player, BLANK_SHEET, 1);
					giveItems(player, BLOODY_RUNE, 1);
					
					if (getQuestItemsCount(player, BLOODY_RUNE) >= 5)
					{
						st.setCond(3, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
			}
		}
		else if (st.isCond(5))
		{
			if (getQuestItemsCount(player, ASHEN_BONES) < 10)
			{
				giveItems(player, ASHEN_BONES, 1);
				
				if (getQuestItemsCount(player, ASHEN_BONES) >= 10)
				{
					st.setCond(6, true);
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
		}
	}
}
