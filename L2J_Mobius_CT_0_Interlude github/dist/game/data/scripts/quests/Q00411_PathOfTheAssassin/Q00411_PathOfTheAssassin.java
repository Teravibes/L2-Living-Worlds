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
package quests.Q00411_PathOfTheAssassin;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00411_PathOfTheAssassin extends Quest
{
	// NPCs
	private static final int TRISKEL = 30416;
	private static final int ARKENIA = 30419;
	private static final int LEIKAN = 30382;
	
	// Items
	private static final int SHILEN_CALL = 1245;
	private static final int ARKENIA_LETTER = 1246;
	private static final int LEIKAN_NOTE = 1247;
	private static final int MOONSTONE_BEAST_MOLAR = 1248;
	private static final int SHILEN_TEARS = 1250;
	private static final int ARKENIA_RECOMMENDATION = 1251;
	private static final int IRON_HEART = 1252;
	
	public Q00411_PathOfTheAssassin()
	{
		super(411, "Path to an Assassin");
		registerQuestItems(SHILEN_CALL, ARKENIA_LETTER, LEIKAN_NOTE, MOONSTONE_BEAST_MOLAR, SHILEN_TEARS, ARKENIA_RECOMMENDATION);
		addStartNpc(TRISKEL);
		addTalkId(TRISKEL, ARKENIA, LEIKAN);
		addKillId(27036, 20369);
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
			case "30416-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.DARK_FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.ASSASSIN) ? "30416-02a.htm" : "30416-02.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30416-03.htm";
				}
				else if (hasQuestItems(player, IRON_HEART))
				{
					htmltext = "30416-04.htm";
				}
				else
				{
					st.startQuest();
					giveItems(player, SHILEN_CALL, 1);
				}
				break;
			}
			case "30419-05.htm":
			{
				st.setCond(2, true);
				takeItems(player, SHILEN_CALL, 1);
				giveItems(player, ARKENIA_LETTER, 1);
				break;
			}
			case "30382-03.htm":
			{
				st.setCond(3, true);
				takeItems(player, ARKENIA_LETTER, 1);
				giveItems(player, LEIKAN_NOTE, 1);
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
				htmltext = "30416-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case TRISKEL:
					{
						if (cond == 1)
						{
							htmltext = "30416-11.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30416-07.htm";
						}
						else if ((cond == 3) || (cond == 4))
						{
							htmltext = "30416-08.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30416-09.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30416-10.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30416-06.htm";
							takeItems(player, ARKENIA_RECOMMENDATION, 1);
							giveItems(player, IRON_HEART, 1);
							addExpAndSp(player, 3200, 3930);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case ARKENIA:
					{
						if (cond == 1)
						{
							htmltext = "30419-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30419-07.htm";
						}
						else if ((cond == 3) || (cond == 4))
						{
							htmltext = "30419-10.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30419-11.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30419-08.htm";
							st.setCond(7, true);
							takeItems(player, SHILEN_TEARS, -1);
							giveItems(player, ARKENIA_RECOMMENDATION, 1);
						}
						else if (cond == 7)
						{
							htmltext = "30419-09.htm";
						}
						break;
					}
					case LEIKAN:
					{
						if (cond == 2)
						{
							htmltext = "30382-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = (!hasQuestItems(player, MOONSTONE_BEAST_MOLAR)) ? "30382-05.htm" : "30382-06.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30382-07.htm";
							st.setCond(5, true);
							takeItems(player, MOONSTONE_BEAST_MOLAR, -1);
							takeItems(player, LEIKAN_NOTE, -1);
						}
						else if (cond == 5)
						{
							htmltext = "30382-09.htm";
						}
						else if (cond > 5)
						{
							htmltext = "30382-08.htm";
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
		
		if (npc.getId() == 20369)
		{
			if (st.isCond(3))
			{
				if (getQuestItemsCount(player, MOONSTONE_BEAST_MOLAR) < 10)
				{
					giveItems(player, MOONSTONE_BEAST_MOLAR, 1);
					
					if (getQuestItemsCount(player, MOONSTONE_BEAST_MOLAR) >= 10)
					{
						st.setCond(4, true);
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
			st.setCond(6, true);
			giveItems(player, SHILEN_TEARS, 1);
		}
	}
}
