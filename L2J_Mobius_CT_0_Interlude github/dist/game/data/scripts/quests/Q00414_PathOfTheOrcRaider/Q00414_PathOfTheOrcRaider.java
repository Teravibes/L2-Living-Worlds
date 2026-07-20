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
package quests.Q00414_PathOfTheOrcRaider;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00414_PathOfTheOrcRaider extends Quest
{
	// NPCs
	private static final int KARUKIA = 30570;
	private static final int KASMAN = 30501;
	private static final int TAZEER = 31978;
	
	// Monsters
	private static final int GOBLIN_TOMB_RAIDER_LEADER = 20320;
	private static final int KURUKA_RATMAN_LEADER = 27045;
	private static final int UMBAR_ORC = 27054;
	private static final int TIMORA_ORC = 27320;
	
	// Items
	private static final int GREEN_BLOOD = 1578;
	private static final int GOBLIN_DWELLING_MAP = 1579;
	private static final int KURUKA_RATMAN_TOOTH = 1580;
	private static final int BETRAYER_REPORT_1 = 1589;
	private static final int BETRAYER_REPORT_2 = 1590;
	private static final int HEAD_OF_BETRAYER = 1591;
	private static final int MARK_OF_RAIDER = 1592;
	private static final int TIMORA_ORC_HEAD = 8544;
	
	public Q00414_PathOfTheOrcRaider()
	{
		super(414, "Path to an Orc Raider");
		registerQuestItems(GREEN_BLOOD, GOBLIN_DWELLING_MAP, KURUKA_RATMAN_TOOTH, BETRAYER_REPORT_1, BETRAYER_REPORT_2, HEAD_OF_BETRAYER, TIMORA_ORC_HEAD);
		addStartNpc(KARUKIA);
		addTalkId(KARUKIA, KASMAN, TAZEER);
		addKillId(GOBLIN_TOMB_RAIDER_LEADER, KURUKA_RATMAN_LEADER, UMBAR_ORC, TIMORA_ORC);
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
			case "30570-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.ORC_FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.ORC_RAIDER) ? "30570-02a.htm" : "30570-03.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30570-02.htm";
				}
				else if (hasQuestItems(player, MARK_OF_RAIDER))
				{
					htmltext = "30570-04.htm";
				}
				else
				{
					st.startQuest();
					giveItems(player, GOBLIN_DWELLING_MAP, 1);
				}
				break;
			}
			case "30570-07a.htm":
			{
				st.setCond(3, true);
				takeItems(player, GOBLIN_DWELLING_MAP, 1);
				takeItems(player, KURUKA_RATMAN_TOOTH, -1);
				giveItems(player, BETRAYER_REPORT_1, 1);
				giveItems(player, BETRAYER_REPORT_2, 1);
				break;
			}
			case "30570-07b.htm":
			{
				st.setCond(5, true);
				takeItems(player, GOBLIN_DWELLING_MAP, 1);
				takeItems(player, KURUKA_RATMAN_TOOTH, -1);
				break;
			}
			case "31978-03.htm":
			{
				st.setCond(6, true);
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
				htmltext = "30570-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case KARUKIA:
					{
						if (cond == 1)
						{
							htmltext = "30570-06.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30570-07.htm";
						}
						else if ((cond == 3) || (cond == 4))
						{
							htmltext = "30570-08.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30570-07b.htm";
						}
						break;
					}
					case KASMAN:
					{
						if (cond == 3)
						{
							htmltext = hasQuestItems(player, HEAD_OF_BETRAYER) ? "30501-02.htm" : "30501-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30501-03.htm";
							takeItems(player, BETRAYER_REPORT_1, 1);
							takeItems(player, BETRAYER_REPORT_2, 1);
							takeItems(player, HEAD_OF_BETRAYER, -1);
							giveItems(player, MARK_OF_RAIDER, 1);
							addExpAndSp(player, 3200, 2360);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case TAZEER:
					{
						if (cond == 5)
						{
							htmltext = "31978-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = "31978-04.htm";
						}
						else if (cond == 7)
						{
							htmltext = "31978-05.htm";
							takeItems(player, TIMORA_ORC_HEAD, 1);
							giveItems(player, MARK_OF_RAIDER, 1);
							addExpAndSp(player, 3200, 2360);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
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
			case GOBLIN_TOMB_RAIDER_LEADER:
			{
				if (st.isCond(1))
				{
					if (getQuestItemsCount(player, GREEN_BLOOD) <= getRandom(20))
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, GREEN_BLOOD, 1);
					}
					else
					{
						takeItems(player, GREEN_BLOOD, -1);
						addSpawn(KURUKA_RATMAN_LEADER, npc, false, 300000);
					}
				}
				break;
			}
			case KURUKA_RATMAN_LEADER:
			{
				if (st.isCond(1))
				{
					if (getQuestItemsCount(player, KURUKA_RATMAN_TOOTH) < 10)
					{
						giveItems(player, KURUKA_RATMAN_TOOTH, 1);
						
						if (getQuestItemsCount(player, KURUKA_RATMAN_TOOTH) >= 10)
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
			case UMBAR_ORC:
			{
				if (st.isCond(3) && (getQuestItemsCount(player, HEAD_OF_BETRAYER) < 2) && (getRandom(10) < 2))
				{
					giveItems(player, HEAD_OF_BETRAYER, 1);
					
					if (getQuestItemsCount(player, HEAD_OF_BETRAYER) >= 2)
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
			case TIMORA_ORC:
			{
				if (st.isCond(6) && (getRandom(10) < 6))
				{
					giveItems(player, TIMORA_ORC_HEAD, 1);
					st.setCond(7, true);
				}
				break;
			}
		}
	}
}
