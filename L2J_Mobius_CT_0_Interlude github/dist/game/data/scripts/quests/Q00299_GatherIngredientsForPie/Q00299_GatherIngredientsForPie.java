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
package quests.Q00299_GatherIngredientsForPie;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00299_GatherIngredientsForPie extends Quest
{
	// NPCs
	private static final int LARA = 30063;
	private static final int BRIGHT = 30466;
	private static final int EMILY = 30620;
	
	// Items
	private static final int FRUIT_BASKET = 7136;
	private static final int AVELLAN_SPICE = 7137;
	private static final int HONEY_POUCH = 7138;
	
	// Reward resources
	private static final int VARNISH = 1865;
	
	public Q00299_GatherIngredientsForPie()
	{
		super(299, "Gather Ingredients for Pie");
		registerQuestItems(FRUIT_BASKET, AVELLAN_SPICE, HONEY_POUCH);
		addStartNpc(EMILY);
		addTalkId(EMILY, LARA, BRIGHT);
		addKillId(20934, 20935); // Wasp Worker, Wasp Leader
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
			case "30620-1.htm":
			{
				st.startQuest();
				break;
			}
			case "30620-3.htm":
			{
				st.setCond(3, true);
				takeItems(player, HONEY_POUCH, -1);
				break;
			}
			case "30063-1.htm":
			{
				st.setCond(4, true);
				giveItems(player, AVELLAN_SPICE, 1);
				break;
			}
			case "30620-5.htm":
			{
				st.setCond(5, true);
				takeItems(player, AVELLAN_SPICE, 1);
				break;
			}
			case "30466-1.htm":
			{
				st.setCond(6, true);
				giveItems(player, FRUIT_BASKET, 1);
				break;
			}
			case "30620-7a.htm":
			{
				if (hasQuestItems(player, FRUIT_BASKET))
				{
					htmltext = "30620-7.htm";
					takeItems(player, FRUIT_BASKET, 1);
					if (getRandom(100) < 70)
					{
						giveAdena(player, 25000, true);
					}
					else
					{
						giveItems(player, VARNISH, 50);
					}
					
					st.exitQuest(true, true);
				}
				else
				{
					st.setCond(5);
				}
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
				htmltext = (player.getLevel() < 34) ? "30620-0a.htm" : "30620-0.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case EMILY:
					{
						if (cond == 1)
						{
							htmltext = "30620-1a.htm";
						}
						else if (cond == 2)
						{
							if (getQuestItemsCount(player, HONEY_POUCH) >= 100)
							{
								htmltext = "30620-2.htm";
							}
							else
							{
								htmltext = "30620-2a.htm";
								st.exitQuest(true);
							}
						}
						else if (cond == 3)
						{
							htmltext = "30620-3a.htm";
						}
						else if (cond == 4)
						{
							if (hasQuestItems(player, AVELLAN_SPICE))
							{
								htmltext = "30620-4.htm";
							}
							else
							{
								htmltext = "30620-4a.htm";
								st.exitQuest(true);
							}
						}
						else if (cond == 5)
						{
							htmltext = "30620-5a.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30620-6.htm";
						}
						break;
					}
					case LARA:
					{
						if (cond == 3)
						{
							htmltext = "30063-0.htm";
						}
						else if (cond > 3)
						{
							htmltext = "30063-1a.htm";
						}
						break;
					}
					case BRIGHT:
					{
						if (cond == 5)
						{
							htmltext = "30466-0.htm";
						}
						else if (cond > 5)
						{
							htmltext = "30466-1a.htm";
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
		final QuestState st = getRandomPartyMemberState(player, 1, 3, npc);
		if (st == null)
		{
			return;
		}
		
		if (getRandom(1000) < (npc.getId() == 20934 ? 571 : 625))
		{
			final Player partyMember = st.getPlayer();
			giveItems(partyMember, HONEY_POUCH, 1);
			if (getQuestItemsCount(partyMember, HONEY_POUCH) < 100)
			{
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(2, true);
			}
		}
	}
}
