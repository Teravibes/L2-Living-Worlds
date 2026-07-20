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
package quests.Q00153_DeliverGoods;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00153_DeliverGoods extends Quest
{
	// NPCs
	private static final int JACKSON = 30002;
	private static final int SILVIA = 30003;
	private static final int ARNOLD = 30041;
	private static final int RANT = 30054;
	
	// Items
	private static final int DELIVERY_LIST = 1012;
	private static final int HEAVY_WOOD_BOX = 1013;
	private static final int CLOTH_BUNDLE = 1014;
	private static final int CLAY_POT = 1015;
	private static final int JACKSON_RECEIPT = 1016;
	private static final int SILVIA_RECEIPT = 1017;
	private static final int RANT_RECEIPT = 1018;
	
	// Rewards
	private static final int SOULSHOT_NO_GRADE = 1835;
	private static final int RING_OF_KNOWLEDGE = 875;
	
	public Q00153_DeliverGoods()
	{
		super(153, "Deliver Goods");
		registerQuestItems(DELIVERY_LIST, HEAVY_WOOD_BOX, CLOTH_BUNDLE, CLAY_POT, JACKSON_RECEIPT, SILVIA_RECEIPT, RANT_RECEIPT);
		addStartNpc(ARNOLD);
		addTalkId(JACKSON, SILVIA, ARNOLD, RANT);
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
		
		if (event.equals("30041-02.htm"))
		{
			st.startQuest();
			giveItems(player, DELIVERY_LIST, 1);
			giveItems(player, CLAY_POT, 1);
			giveItems(player, CLOTH_BUNDLE, 1);
			giveItems(player, HEAVY_WOOD_BOX, 1);
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
				htmltext = (player.getLevel() < 2) ? "30041-00.htm" : "30041-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case ARNOLD:
					{
						if (st.isCond(1))
						{
							htmltext = "30041-03.htm";
						}
						else if (st.isCond(2))
						{
							htmltext = "30041-04.htm";
							takeItems(player, DELIVERY_LIST, 1);
							takeItems(player, JACKSON_RECEIPT, 1);
							takeItems(player, SILVIA_RECEIPT, 1);
							takeItems(player, RANT_RECEIPT, 1);
							giveItems(player, RING_OF_KNOWLEDGE, 1);
							giveItems(player, RING_OF_KNOWLEDGE, 1);
							addExpAndSp(player, 600, 0);
							st.exitQuest(false, true);
						}
						break;
					}
					case JACKSON:
					{
						if (hasQuestItems(player, HEAVY_WOOD_BOX))
						{
							htmltext = "30002-01.htm";
							takeItems(player, HEAVY_WOOD_BOX, 1);
							giveItems(player, JACKSON_RECEIPT, 1);
							if (hasQuestItems(player, SILVIA_RECEIPT, RANT_RECEIPT))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else
						{
							htmltext = "30002-02.htm";
						}
						break;
					}
					case SILVIA:
					{
						if (hasQuestItems(player, CLOTH_BUNDLE))
						{
							htmltext = "30003-01.htm";
							takeItems(player, CLOTH_BUNDLE, 1);
							giveItems(player, SILVIA_RECEIPT, 1);
							giveItems(player, SOULSHOT_NO_GRADE, 3);
							if (hasQuestItems(player, JACKSON_RECEIPT, RANT_RECEIPT))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else
						{
							htmltext = "30003-02.htm";
						}
						break;
					}
					case RANT:
					{
						if (hasQuestItems(player, CLAY_POT))
						{
							htmltext = "30054-01.htm";
							takeItems(player, CLAY_POT, 1);
							giveItems(player, RANT_RECEIPT, 1);
							if (hasQuestItems(player, JACKSON_RECEIPT, SILVIA_RECEIPT))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else
						{
							htmltext = "30054-02.htm";
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
}
