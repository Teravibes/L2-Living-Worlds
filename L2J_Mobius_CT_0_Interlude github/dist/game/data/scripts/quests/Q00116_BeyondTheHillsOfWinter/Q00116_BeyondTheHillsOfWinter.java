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
package quests.Q00116_BeyondTheHillsOfWinter;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00116_BeyondTheHillsOfWinter extends Quest
{
	// NPCs
	private static final int FILAUR = 30535;
	private static final int OBI = 32052;
	
	// Items
	private static final int BANDAGE = 1833;
	private static final int ENERGY_STONE = 5589;
	private static final int THIEF_KEY = 1661;
	private static final int GOODS = 8098;
	
	// Reward
	private static final int SSD = 1463;
	
	public Q00116_BeyondTheHillsOfWinter()
	{
		super(116, "Beyond the Hills of Winter");
		registerQuestItems(GOODS);
		addStartNpc(FILAUR);
		addTalkId(FILAUR, OBI);
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
			case "30535-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30535-05.htm":
			{
				st.setCond(2, true);
				giveItems(player, GOODS, 1);
				break;
			}
			case "materials":
			{
				htmltext = "32052-02.htm";
				takeItems(player, GOODS, -1);
				rewardItems(player, SSD, 1650);
				st.exitQuest(false, true);
				break;
			}
			case "adena":
			{
				htmltext = "32052-02.htm";
				takeItems(player, GOODS, -1);
				giveAdena(player, 16500, true);
				st.exitQuest(false, true);
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
				htmltext = ((player.getLevel() < 30) || (player.getRace() != Race.DWARF)) ? "30535-00.htm" : "30535-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case FILAUR:
					{
						if (cond == 1)
						{
							if ((getQuestItemsCount(player, BANDAGE) >= 20) && (getQuestItemsCount(player, ENERGY_STONE) >= 5) && (getQuestItemsCount(player, THIEF_KEY) >= 10))
							{
								htmltext = "30535-03.htm";
								takeItems(player, BANDAGE, 20);
								takeItems(player, ENERGY_STONE, 5);
								takeItems(player, THIEF_KEY, 10);
							}
							else
							{
								htmltext = "30535-04.htm";
							}
						}
						else if (cond == 2)
						{
							htmltext = "30535-05.htm";
						}
						break;
					}
					case OBI:
					{
						if (cond == 2)
						{
							htmltext = "32052-00.htm";
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
