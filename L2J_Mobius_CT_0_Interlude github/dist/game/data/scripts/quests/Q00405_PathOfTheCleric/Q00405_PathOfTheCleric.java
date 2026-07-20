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
package quests.Q00405_PathOfTheCleric;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00405_PathOfTheCleric extends Quest
{
	// NPCs
	private static final int GALLINT = 30017;
	private static final int ZIGAUNT = 30022;
	private static final int VIVYAN = 30030;
	private static final int PRAGA = 30333;
	private static final int SIMPLON = 30253;
	private static final int LIONEL = 30408;
	
	// Items
	private static final int LETTER_OF_ORDER_1 = 1191;
	private static final int LETTER_OF_ORDER_2 = 1192;
	private static final int LIONEL_BOOK = 1193;
	private static final int BOOK_OF_VIVYAN = 1194;
	private static final int BOOK_OF_SIMPLON = 1195;
	private static final int BOOK_OF_PRAGA = 1196;
	private static final int CERTIFICATE_OF_GALLINT = 1197;
	private static final int PENDANT_OF_MOTHER = 1198;
	private static final int NECKLACE_OF_MOTHER = 1199;
	private static final int LIONEL_COVENANT = 1200;
	
	// Reward
	private static final int MARK_OF_FATE = 1201;
	
	public Q00405_PathOfTheCleric()
	{
		super(405, "Path to a Cleric");
		registerQuestItems(LETTER_OF_ORDER_1, BOOK_OF_SIMPLON, BOOK_OF_PRAGA, BOOK_OF_VIVYAN, NECKLACE_OF_MOTHER, PENDANT_OF_MOTHER, LETTER_OF_ORDER_2, LIONEL_BOOK, CERTIFICATE_OF_GALLINT, LIONEL_COVENANT);
		addStartNpc(ZIGAUNT);
		addTalkId(ZIGAUNT, SIMPLON, PRAGA, VIVYAN, LIONEL, GALLINT);
		addKillId(20029, 20026);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30022-05.htm"))
		{
			st.startQuest();
			giveItems(player, LETTER_OF_ORDER_1, 1);
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
				if (player.getPlayerClass() != PlayerClass.MAGE)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.CLERIC) ? "30022-02a.htm" : "30022-02.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30022-03.htm";
				}
				else if (hasQuestItems(player, MARK_OF_FATE))
				{
					htmltext = "30022-04.htm";
				}
				else
				{
					htmltext = "30022-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ZIGAUNT:
					{
						if (cond == 1)
						{
							htmltext = "30022-06.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30022-08.htm";
							st.setCond(3, true);
							takeItems(player, BOOK_OF_PRAGA, 1);
							takeItems(player, BOOK_OF_VIVYAN, 1);
							takeItems(player, BOOK_OF_SIMPLON, 3);
							takeItems(player, LETTER_OF_ORDER_1, 1);
							giveItems(player, LETTER_OF_ORDER_2, 1);
						}
						else if ((cond > 2) && (cond < 6))
						{
							htmltext = "30022-07.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30022-09.htm";
							takeItems(player, LETTER_OF_ORDER_2, 1);
							takeItems(player, LIONEL_COVENANT, 1);
							giveItems(player, MARK_OF_FATE, 1);
							addExpAndSp(player, 3200, 5610);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case SIMPLON:
					{
						if ((cond == 1) && !hasQuestItems(player, BOOK_OF_SIMPLON))
						{
							htmltext = "30253-01.htm";
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							giveItems(player, BOOK_OF_SIMPLON, 3);
						}
						else if ((cond > 1) || hasQuestItems(player, BOOK_OF_SIMPLON))
						{
							htmltext = "30253-02.htm";
						}
						break;
					}
					case PRAGA:
					{
						if (cond == 1)
						{
							if (!hasQuestItems(player, BOOK_OF_PRAGA) && !hasQuestItems(player, NECKLACE_OF_MOTHER) && hasQuestItems(player, BOOK_OF_SIMPLON))
							{
								htmltext = "30333-01.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								giveItems(player, NECKLACE_OF_MOTHER, 1);
							}
							else if (!hasQuestItems(player, PENDANT_OF_MOTHER))
							{
								htmltext = "30333-02.htm";
							}
							else if (hasQuestItems(player, PENDANT_OF_MOTHER))
							{
								htmltext = "30333-03.htm";
								takeItems(player, NECKLACE_OF_MOTHER, 1);
								takeItems(player, PENDANT_OF_MOTHER, 1);
								giveItems(player, BOOK_OF_PRAGA, 1);
								
								if (hasQuestItems(player, BOOK_OF_VIVYAN))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
						}
						else if ((cond > 1) || (hasQuestItems(player, BOOK_OF_PRAGA)))
						{
							htmltext = "30333-04.htm";
						}
						break;
					}
					case VIVYAN:
					{
						if ((cond == 1) && !hasQuestItems(player, BOOK_OF_VIVYAN) && hasQuestItems(player, BOOK_OF_SIMPLON))
						{
							htmltext = "30030-01.htm";
							giveItems(player, BOOK_OF_VIVYAN, 1);
							
							if (hasQuestItems(player, BOOK_OF_PRAGA))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if ((cond > 1) || hasQuestItems(player, BOOK_OF_VIVYAN))
						{
							htmltext = "30030-02.htm";
						}
						break;
					}
					case LIONEL:
					{
						if (cond < 3)
						{
							htmltext = "30408-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30408-01.htm";
							st.setCond(4, true);
							giveItems(player, LIONEL_BOOK, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30408-03.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30408-04.htm";
							st.setCond(6, true);
							takeItems(player, CERTIFICATE_OF_GALLINT, 1);
							giveItems(player, LIONEL_COVENANT, 1);
						}
						else if (cond == 6)
						{
							htmltext = "30408-05.htm";
						}
						break;
					}
					case GALLINT:
					{
						if (cond == 4)
						{
							htmltext = "30017-01.htm";
							st.setCond(5, true);
							takeItems(player, LIONEL_BOOK, 1);
							giveItems(player, CERTIFICATE_OF_GALLINT, 1);
						}
						else if (cond > 4)
						{
							htmltext = "30017-02.htm";
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
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		if (hasQuestItems(player, NECKLACE_OF_MOTHER) && !hasQuestItems(player, PENDANT_OF_MOTHER))
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
			giveItems(player, PENDANT_OF_MOTHER, 1);
		}
	}
}
