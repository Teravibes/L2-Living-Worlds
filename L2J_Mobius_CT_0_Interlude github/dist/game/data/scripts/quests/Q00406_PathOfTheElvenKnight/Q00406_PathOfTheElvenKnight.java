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
package quests.Q00406_PathOfTheElvenKnight;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00406_PathOfTheElvenKnight extends Quest
{
	// NPCs
	private static final int SORIUS = 30327;
	private static final int KLUTO = 30317;
	
	// Items
	private static final int SORIUS_LETTER = 1202;
	private static final int KLUTO_BOX = 1203;
	private static final int ELVEN_KNIGHT_BROOCH = 1204;
	private static final int TOPAZ_PIECE = 1205;
	private static final int EMERALD_PIECE = 1206;
	private static final int KLUTO_MEMO = 1276;
	
	public Q00406_PathOfTheElvenKnight()
	{
		super(406, "Path to an Elven Knight");
		registerQuestItems(SORIUS_LETTER, KLUTO_BOX, TOPAZ_PIECE, EMERALD_PIECE, KLUTO_MEMO);
		addStartNpc(SORIUS);
		addTalkId(SORIUS, KLUTO);
		addKillId(20035, 20042, 20045, 20051, 20054, 20060, 20782);
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
			case "30327-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.ELVEN_FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.ELVEN_KNIGHT) ? "30327-02a.htm" : "30327-02.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30327-03.htm";
				}
				else if (hasQuestItems(player, ELVEN_KNIGHT_BROOCH))
				{
					htmltext = "30327-04.htm";
				}
				break;
			}
			case "30327-06.htm":
			{
				st.startQuest();
				break;
			}
			case "30317-02.htm":
			{
				st.setCond(4, true);
				takeItems(player, SORIUS_LETTER, 1);
				giveItems(player, KLUTO_MEMO, 1);
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
				htmltext = "30327-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case SORIUS:
					{
						if (cond == 1)
						{
							htmltext = (!hasQuestItems(player, TOPAZ_PIECE)) ? "30327-07.htm" : "30327-08.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30327-09.htm";
							st.setCond(3, true);
							giveItems(player, SORIUS_LETTER, 1);
						}
						else if ((cond > 2) && (cond < 6))
						{
							htmltext = "30327-11.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30327-10.htm";
							takeItems(player, KLUTO_BOX, 1);
							takeItems(player, KLUTO_MEMO, 1);
							giveItems(player, ELVEN_KNIGHT_BROOCH, 1);
							addExpAndSp(player, 3200, 2280);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case KLUTO:
					{
						if (cond == 3)
						{
							htmltext = "30317-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = (!hasQuestItems(player, EMERALD_PIECE)) ? "30317-03.htm" : "30317-04.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30317-05.htm";
							st.setCond(6, true);
							takeItems(player, EMERALD_PIECE, -1);
							takeItems(player, TOPAZ_PIECE, -1);
							giveItems(player, KLUTO_BOX, 1);
						}
						else if (cond == 6)
						{
							htmltext = "30317-06.htm";
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
		
		switch (npc.getId())
		{
			case 20035:
			case 20042:
			case 20045:
			case 20051:
			case 20054:
			case 20060:
			{
				if (st.isCond(1) && (getRandom(10) < 7))
				{
					if (getQuestItemsCount(player, TOPAZ_PIECE) < 20)
					{
						giveItems(player, TOPAZ_PIECE, 1);
						
						if (getQuestItemsCount(player, TOPAZ_PIECE) >= 20)
						{
							st.setCond(2, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case 20782:
			{
				if (st.isCond(4) && (getRandom(10) < 5))
				{
					if (getQuestItemsCount(player, EMERALD_PIECE) < 20)
					{
						giveItems(player, EMERALD_PIECE, 1);
						
						if (getQuestItemsCount(player, EMERALD_PIECE) >= 20)
						{
							st.setCond(5, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
		}
	}
}
