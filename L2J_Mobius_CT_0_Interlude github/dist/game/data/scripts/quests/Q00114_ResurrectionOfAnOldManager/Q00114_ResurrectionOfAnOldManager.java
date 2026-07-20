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
package quests.Q00114_ResurrectionOfAnOldManager;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import quests.Q00121_PavelTheGiant.Q00121_PavelTheGiant;

public class Q00114_ResurrectionOfAnOldManager extends Quest
{
	// NPCs
	private static final int NEWYEAR = 31961;
	private static final int YUMI = 32041;
	private static final int STONE = 32046;
	private static final int WENDY = 32047;
	private static final int BOX = 32050;
	
	// Monsters
	private static final int GOLEM = 27318;
	
	// Items
	private static final int LETTER = 8288;
	private static final int DETECTOR = 8090;
	private static final int DETECTOR_2 = 8091;
	private static final int STARSTONE = 8287;
	private static final int STARSTONE_2 = 8289;
	
	public Q00114_ResurrectionOfAnOldManager()
	{
		super(114, "Resurrection of an Old Manager");
		registerQuestItems(LETTER, DETECTOR, DETECTOR_2, STARSTONE, STARSTONE_2);
		addStartNpc(YUMI);
		addTalkId(YUMI, WENDY, BOX, STONE, NEWYEAR);
		addKillId(GOLEM);
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
			case "32041-02.htm":
			{
				st.startQuest();
				st.set("golemSpawned", "0");
				break;
			}
			case "32041-06.htm":
			{
				st.set("talk", "1");
				break;
			}
			case "32041-07.htm":
			{
				st.setCond(2, true);
				st.set("talk", "0");
				break;
			}
			case "32041-10.htm":
			{
				final int choice = st.getInt("choice");
				if (choice == 1)
				{
					htmltext = "32041-10.htm";
				}
				else if (choice == 2)
				{
					htmltext = "32041-10a.htm";
				}
				else if (choice == 3)
				{
					htmltext = "32041-10b.htm";
				}
				break;
			}
			case "32041-11.htm":
			{
				st.set("talk", "1");
				break;
			}
			case "32041-18.htm":
			{
				st.set("talk", "2");
				break;
			}
			case "32041-20.htm":
			{
				st.setCond(6, true);
				st.set("talk", "0");
				break;
			}
			case "32041-25.htm":
			{
				st.setCond(17, true);
				giveItems(player, DETECTOR, 1);
				break;
			}
			case "32041-28.htm":
			{
				st.set("talk", "1");
				takeItems(player, DETECTOR_2, 1);
				break;
			}
			case "32041-31.htm":
			{
				if (st.getInt("choice") > 1)
				{
					htmltext = "32041-37.htm";
				}
				break;
			}
			case "32041-32.htm":
			{
				st.setCond(21, true);
				giveItems(player, LETTER, 1);
				break;
			}
			case "32041-36.htm":
			{
				st.setCond(20, true);
				break;
			}
			case "32046-02.htm":
			{
				st.setCond(19, true);
				break;
			}
			case "32046-06.htm":
			{
				st.exitQuest(false, true);
				break;
			}
			case "32047-01.htm":
			{
				final int talk = st.getInt("talk");
				final int talk1 = st.getInt("talk1");
				if ((talk == 1) && (talk1 == 1))
				{
					htmltext = "32047-04.htm";
				}
				else if ((talk == 2) && (talk1 == 2) && (st.getInt("talk2") == 2))
				{
					htmltext = "32047-08.htm";
				}
				break;
			}
			case "32047-02.htm":
			{
				if (st.getInt("talk") == 0)
				{
					st.set("talk", "1");
				}
				break;
			}
			case "32047-03.htm":
			{
				if (st.getInt("talk1") == 0)
				{
					st.set("talk1", "1");
				}
				break;
			}
			case "32047-05.htm":
			{
				st.setCond(3, true);
				st.set("talk", "0");
				st.set("choice", "1");
				st.unset("talk1");
				break;
			}
			case "32047-06.htm":
			{
				st.setCond(4, true);
				st.set("talk", "0");
				st.set("choice", "2");
				st.unset("talk1");
				break;
			}
			case "32047-07.htm":
			{
				st.setCond(5, true);
				st.set("talk", "0");
				st.set("choice", "3");
				st.unset("talk1");
				break;
			}
			case "32047-13.htm":
			{
				st.setCond(7, true);
				break;
			}
			case "32047-13a.htm":
			{
				st.setCond(10, true);
				break;
			}
			case "32047-15.htm":
			{
				if (st.getInt("talk") == 0)
				{
					st.set("talk", "1");
				}
				break;
			}
			case "32047-15a.htm":
			{
				if (st.getInt("golemSpawned") == 0)
				{
					final Npc golem = addSpawn(GOLEM, 96977, -110625, -3322, 0, true, 0);
					golem.broadcastSay(ChatType.GENERAL, "You, " + player.getName() + ", you attacked Wendy. Prepare to die!");
					golem.asAttackable().addDamageHate(player, 0, 999);
					golem.getAI().setIntention(Intention.ATTACK, player);
					st.set("golemSpawned", "1");
					startQuestTimer("golemDespawn", 900000, golem, player, false);
				}
				else
				{
					htmltext = "32047-19a.htm";
				}
				break;
			}
			case "32047-17a.htm":
			{
				st.setCond(12, true);
				break;
			}
			case "32047-20.htm":
			{
				st.set("talk", "2");
				break;
			}
			case "32047-23.htm":
			{
				st.setCond(13, true);
				st.set("talk", "0");
				break;
			}
			case "32047-25.htm":
			{
				st.setCond(15, true);
				takeItems(player, STARSTONE, 1);
				break;
			}
			case "32047-30.htm":
			{
				st.set("talk", "2");
				break;
			}
			case "32047-33.htm":
			{
				final int cond = st.getCond();
				if (cond == 7)
				{
					st.setCond(8, true);
					st.set("talk", "0");
				}
				else if (cond == 8)
				{
					st.setCond(9, true);
					htmltext = "32047-34.htm";
				}
				break;
			}
			case "32047-34.htm":
			{
				st.setCond(9, true);
				break;
			}
			case "32047-38.htm":
			{
				if (getQuestItemsCount(player, 57) >= 3000)
				{
					st.setCond(26, true);
					takeItems(player, 57, 3000);
					giveItems(player, STARSTONE_2, 1);
				}
				else
				{
					htmltext = "32047-39.htm";
				}
				break;
			}
			case "32050-02.htm":
			{
				st.set("talk", "1");
				playSound(player, QuestSound.ITEMSOUND_ARMOR_WOOD);
				break;
			}
			case "32050-04.htm":
			{
				st.setCond(14, true);
				st.set("talk", "0");
				giveItems(player, STARSTONE, 1);
				break;
			}
			case "31961-02.htm":
			{
				st.setCond(22, true);
				takeItems(player, LETTER, 1);
				giveItems(player, STARSTONE_2, 1);
				break;
			}
			case "golemDespawn":
			{
				st.unset("golemSpawned");
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
				final QuestState pavelReq = player.getQuestState(Q00121_PavelTheGiant.class.getSimpleName());
				htmltext = ((pavelReq == null) || !pavelReq.isCompleted() || (player.getLevel() < 49)) ? "32041-00.htm" : "32041-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				final int talk = st.getInt("talk");
				
				switch (npc.getId())
				{
					case YUMI:
					{
						if (cond == 1)
						{
							if (talk == 0)
							{
								htmltext = "32041-02.htm";
							}
							else
							{
								htmltext = "32041-06.htm";
							}
						}
						else if (cond == 2)
						{
							htmltext = "32041-08.htm";
						}
						else if ((cond > 2) && (cond < 6))
						{
							if (talk == 0)
							{
								htmltext = "32041-09.htm";
							}
							else if (talk == 1)
							{
								htmltext = "32041-11.htm";
							}
							else
							{
								htmltext = "32041-18.htm";
							}
						}
						else if (cond == 6)
						{
							htmltext = "32041-21.htm";
						}
						else if ((cond == 9) || (cond == 12) || (cond == 16))
						{
							htmltext = "32041-22.htm";
						}
						else if (cond == 17)
						{
							htmltext = "32041-26.htm";
						}
						else if (cond == 19)
						{
							if (talk == 0)
							{
								htmltext = "32041-27.htm";
							}
							else
							{
								htmltext = "32041-28.htm";
							}
						}
						else if (cond == 20)
						{
							htmltext = "32041-36.htm";
						}
						else if (cond == 21)
						{
							htmltext = "32041-33.htm";
						}
						else if ((cond == 22) || (cond == 26))
						{
							htmltext = "32041-34.htm";
							st.setCond(27, true);
						}
						else if (cond == 27)
						{
							htmltext = "32041-35.htm";
						}
						break;
					}
					case WENDY:
					{
						if (cond == 2)
						{
							if ((talk == 0) && (st.getInt("talk1") == 0))
							{
								htmltext = "32047-01.htm";
							}
							else if ((talk == 1) && (st.getInt("talk1") == 1))
							{
								htmltext = "32047-04.htm";
							}
						}
						else if (cond == 3)
						{
							htmltext = "32047-09.htm";
						}
						else if ((cond == 4) || (cond == 5))
						{
							htmltext = "32047-09a.htm";
						}
						else if (cond == 6)
						{
							final int choice = st.getInt("choice");
							if (choice == 1)
							{
								if (talk == 0)
								{
									htmltext = "32047-10.htm";
								}
								else if (talk == 1)
								{
									htmltext = "32047-20.htm";
								}
							}
							else if (choice == 2)
							{
								htmltext = "32047-10a.htm";
							}
							else if (choice == 3)
							{
								if (talk == 0)
								{
									htmltext = "32047-14.htm";
								}
								else if (talk == 1)
								{
									htmltext = "32047-15.htm";
								}
								else
								{
									htmltext = "32047-20.htm";
								}
							}
						}
						else if (cond == 7)
						{
							if (talk == 0)
							{
								htmltext = "32047-14.htm";
							}
							else if (talk == 1)
							{
								htmltext = "32047-15.htm";
							}
							else
							{
								htmltext = "32047-20.htm";
							}
						}
						else if (cond == 8)
						{
							htmltext = "32047-30.htm";
						}
						else if (cond == 9)
						{
							htmltext = "32047-27.htm";
						}
						else if (cond == 10)
						{
							htmltext = "32047-14a.htm";
						}
						else if (cond == 11)
						{
							htmltext = "32047-16a.htm";
						}
						else if (cond == 12)
						{
							htmltext = "32047-18a.htm";
						}
						else if (cond == 13)
						{
							htmltext = "32047-23.htm";
						}
						else if (cond == 14)
						{
							htmltext = "32047-24.htm";
						}
						else if (cond == 15)
						{
							htmltext = "32047-26.htm";
							st.setCond(16, true);
						}
						else if (cond == 16)
						{
							htmltext = "32047-27.htm";
						}
						else if (cond == 20)
						{
							htmltext = "32047-35.htm";
						}
						else if (cond == 26)
						{
							htmltext = "32047-40.htm";
						}
						break;
					}
					case BOX:
					{
						if (cond == 13)
						{
							if (talk == 0)
							{
								htmltext = "32050-01.htm";
							}
							else
							{
								htmltext = "32050-03.htm";
							}
						}
						else if (cond == 14)
						{
							htmltext = "32050-05.htm";
						}
						break;
					}
					case STONE:
					{
						if (cond == 17)
						{
							st.setCond(18, true);
							takeItems(player, DETECTOR, 1);
							giveItems(player, DETECTOR_2, 1);
							player.sendPacket(new ExShowScreenMessage("The radio signal detector is responding. # A suspicious pile of stones catches your eye.", 4500));
							return null;
						}
						else if (cond == 18)
						{
							htmltext = "32046-01.htm";
						}
						else if (cond == 19)
						{
							htmltext = "32046-02.htm";
						}
						else if (cond == 27)
						{
							htmltext = "32046-03.htm";
						}
						break;
					}
					case NEWYEAR:
					{
						if (cond == 21)
						{
							htmltext = "31961-01.htm";
						}
						else if (cond == 22)
						{
							htmltext = "31961-03.htm";
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
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isCond(10))
		{
			return;
		}
		
		npc.broadcastSay(ChatType.GENERAL, "This enemy is far too powerful for me to fight. I must withdraw!");
		st.setCond(11, true);
		st.unset("golemSpawned");
	}
}
