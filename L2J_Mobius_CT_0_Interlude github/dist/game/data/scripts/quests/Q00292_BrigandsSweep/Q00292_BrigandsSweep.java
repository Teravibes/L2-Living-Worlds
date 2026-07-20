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
package quests.Q00292_BrigandsSweep;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00292_BrigandsSweep extends Quest
{
	// NPCs
	private static final int SPIRON = 30532;
	private static final int BALANKI = 30533;
	
	// Monsters
	private static final int GOBLIN_BRIGAND = 20322;
	private static final int GOBLIN_BRIGAND_LEADER = 20323;
	private static final int GOBLIN_BRIGAND_LIEUTENANT = 20324;
	private static final int GOBLIN_SNOOPER = 20327;
	private static final int GOBLIN_LORD = 20528;
	
	// Items
	private static final int GOBLIN_NECKLACE = 1483;
	private static final int GOBLIN_PENDANT = 1484;
	private static final int GOBLIN_LORD_PENDANT = 1485;
	private static final int SUSPICIOUS_MEMO = 1486;
	private static final int SUSPICIOUS_CONTRACT = 1487;
	
	public Q00292_BrigandsSweep()
	{
		super(292, "Brigands Sweep");
		registerQuestItems(GOBLIN_NECKLACE, GOBLIN_PENDANT, GOBLIN_LORD_PENDANT, SUSPICIOUS_MEMO, SUSPICIOUS_CONTRACT);
		addStartNpc(SPIRON);
		addTalkId(SPIRON, BALANKI);
		addKillId(GOBLIN_BRIGAND, GOBLIN_BRIGAND_LEADER, GOBLIN_BRIGAND_LIEUTENANT, GOBLIN_SNOOPER, GOBLIN_LORD);
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
		
		if (event.equals("30532-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30532-06.htm"))
		{
			st.exitQuest(true, true);
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
				if (player.getRace() != Race.DWARF)
				{
					htmltext = "30532-00.htm";
				}
				else if (player.getLevel() < 5)
				{
					htmltext = "30532-01.htm";
				}
				else
				{
					htmltext = "30532-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case SPIRON:
					{
						final int goblinNecklaces = getQuestItemsCount(player, GOBLIN_NECKLACE);
						final int goblinPendants = getQuestItemsCount(player, GOBLIN_PENDANT);
						final int goblinLordPendants = getQuestItemsCount(player, GOBLIN_LORD_PENDANT);
						final int suspiciousMemos = getQuestItemsCount(player, SUSPICIOUS_MEMO);
						final int countAll = goblinNecklaces + goblinPendants + goblinLordPendants;
						final boolean hasContract = hasQuestItems(player, SUSPICIOUS_CONTRACT);
						if (countAll == 0)
						{
							htmltext = "30532-04.htm";
						}
						else
						{
							if (hasContract)
							{
								htmltext = "30532-10.htm";
							}
							else if (suspiciousMemos > 0)
							{
								if (suspiciousMemos > 1)
								{
									htmltext = "30532-09.htm";
								}
								else
								{
									htmltext = "30532-08.htm";
								}
							}
							else
							{
								htmltext = "30532-05.htm";
							}
							
							takeItems(player, GOBLIN_NECKLACE, -1);
							takeItems(player, GOBLIN_PENDANT, -1);
							takeItems(player, GOBLIN_LORD_PENDANT, -1);
							if (hasContract)
							{
								st.setCond(1);
								takeItems(player, SUSPICIOUS_CONTRACT, -1);
							}
							
							int reward = (12 * goblinNecklaces) + (36 * goblinPendants) + (33 * goblinLordPendants) + ((hasContract) ? 1120 : 0);
							if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD && (countAll >= 10))
							{
								reward += 1000;
							}
							
							giveAdena(player, reward, true);
						}
						break;
					}
					case BALANKI:
					{
						if (!hasQuestItems(player, SUSPICIOUS_CONTRACT))
						{
							htmltext = "30533-01.htm";
						}
						else
						{
							htmltext = "30533-02.htm";
							st.setCond(1);
							takeItems(player, SUSPICIOUS_CONTRACT, -1);
							giveAdena(player, 1500, true);
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
		
		final int chance = getRandom(10);
		if (chance > 5)
		{
			switch (npc.getId())
			{
				case GOBLIN_BRIGAND:
				case GOBLIN_SNOOPER:
				case GOBLIN_BRIGAND_LIEUTENANT:
				{
					giveItems(player, GOBLIN_NECKLACE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					break;
				}
				case GOBLIN_BRIGAND_LEADER:
				{
					giveItems(player, GOBLIN_PENDANT, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					break;
				}
				case GOBLIN_LORD:
				{
					giveItems(player, GOBLIN_LORD_PENDANT, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					break;
				}
			}
		}
		else if ((chance > 4) && st.isCond(1))
		{
			giveItems(player, SUSPICIOUS_MEMO, 1);
			if (getQuestItemsCount(player, SUSPICIOUS_MEMO) < 3)
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				takeItems(player, SUSPICIOUS_MEMO, -1);
				giveItems(player, SUSPICIOUS_CONTRACT, 1);
				st.setCond(2, true);
			}
		}
	}
}
