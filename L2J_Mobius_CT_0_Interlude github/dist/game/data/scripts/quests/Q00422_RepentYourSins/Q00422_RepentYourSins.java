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
package quests.Q00422_RepentYourSins;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00422_RepentYourSins extends Quest
{
	// NPCs
	private static final int BLACK_JUDGE = 30981;
	private static final int KATARI = 30668;
	private static final int PIOTUR = 30597;
	private static final int CASIAN = 30612;
	private static final int JOAN = 30718;
	private static final int PUSHKIN = 30300;
	
	// Items
	private static final int RATMAN_SCAVENGER_SKULL = 4326;
	private static final int TUREK_WAR_HOUND_TAIL = 4327;
	private static final int TYRANT_KINGPIN_HEART = 4328;
	private static final int TRISALIM_TARANTULA_VENOM_SAC = 4329;
	private static final int QITEM_PENITENT_MANACLES = 4330;
	private static final int MANUAL_OF_MANACLES = 4331;
	private static final int PENITENT_MANACLES = 4425;
	private static final int LEFT_PENITENT_MANACLES = 4426;
	private static final int SILVER_NUGGET = 1873;
	private static final int ADAMANTINE_NUGGET = 1877;
	private static final int BLACKSMITH_FRAME = 1892;
	private static final int COKES = 1879;
	private static final int STEEL = 1880;
	
	public Q00422_RepentYourSins()
	{
		super(422, "Repent Your Sins");
		registerQuestItems(RATMAN_SCAVENGER_SKULL, TUREK_WAR_HOUND_TAIL, TYRANT_KINGPIN_HEART, TRISALIM_TARANTULA_VENOM_SAC, MANUAL_OF_MANACLES, PENITENT_MANACLES, QITEM_PENITENT_MANACLES);
		addStartNpc(BLACK_JUDGE);
		addTalkId(BLACK_JUDGE, KATARI, PIOTUR, CASIAN, JOAN, PUSHKIN);
		addKillId(20039, 20494, 20193, 20561);
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
			case "Start":
			{
				st.startQuest();
				if (player.getLevel() <= 20)
				{
					htmltext = "30981-03.htm";
					st.setCond(2);
				}
				else if ((player.getLevel() >= 20) && (player.getLevel() <= 30))
				{
					htmltext = "30981-04.htm";
					st.setCond(3);
				}
				else if ((player.getLevel() >= 30) && (player.getLevel() <= 40))
				{
					htmltext = "30981-05.htm";
					st.setCond(4);
				}
				else
				{
					htmltext = "30981-06.htm";
					st.setCond(5);
				}
				break;
			}
			case "30981-11.htm":
			{
				if (!hasQuestItems(player, PENITENT_MANACLES))
				{
					final int cond = st.getCond();
					
					// Case you return back the qitem to Black Judge. She rewards you with the pet item.
					if (cond == 15)
					{
						st.setCond(16);
						st.set("level", String.valueOf(player.getLevel()));
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						takeItems(player, QITEM_PENITENT_MANACLES, -1);
						giveItems(player, PENITENT_MANACLES, 1);
					}
					// Case you return back to Black Judge with leftover of previous quest.
					else if (cond == 16)
					{
						st.set("level", String.valueOf(player.getLevel()));
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						takeItems(player, LEFT_PENITENT_MANACLES, -1);
						giveItems(player, PENITENT_MANACLES, 1);
					}
				}
				break;
			}
			case "30981-19.htm":
			{
				if (hasQuestItems(player, LEFT_PENITENT_MANACLES))
				{
					st.setState(State.STARTED);
					st.setCond(16, true);
				}
				break;
			}
			case "Pk":
			{
				final Summon pet = player.getSummon();
				
				// If Sin Eater is currently summoned, show a warning.
				if ((pet != null) && (pet.getId() == 12564))
				{
					htmltext = "30981-16.htm";
				}
				else if (findSinEaterLvl(player) > st.getInt("level"))
				{
					takeItems(player, PENITENT_MANACLES, 1);
					giveItems(player, LEFT_PENITENT_MANACLES, 1);
					
					final int removePkAmount = getRandom(10) + 1;
					
					// Player's PKs are lower than random amount ; finish the quest.
					if (player.getPkKills() <= removePkAmount)
					{
						htmltext = "30981-15.htm";
						st.exitQuest(true, true);
						
						player.setPkKills(0);
						player.updateUserInfo();
					}
					// Player's PK are bigger than random amount ; continue the quest.
					else
					{
						htmltext = "30981-14.htm";
						st.set("level", String.valueOf(player.getLevel()));
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
						
						player.setPkKills(player.getPkKills() - removePkAmount);
						player.updateUserInfo();
					}
				}
				break;
			}
			case "Quit":
			{
				htmltext = "30981-20.htm";
				takeItems(player, RATMAN_SCAVENGER_SKULL, -1);
				takeItems(player, TUREK_WAR_HOUND_TAIL, -1);
				takeItems(player, TYRANT_KINGPIN_HEART, -1);
				takeItems(player, TRISALIM_TARANTULA_VENOM_SAC, -1);
				takeItems(player, MANUAL_OF_MANACLES, -1);
				takeItems(player, PENITENT_MANACLES, -1);
				takeItems(player, QITEM_PENITENT_MANACLES, -1);
				st.exitQuest(true, true);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getAlreadyCompletedMsg(player);
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (player.getPkKills() >= 1)
				{
					htmltext = (hasQuestItems(player, LEFT_PENITENT_MANACLES)) ? "30981-18.htm" : "30981-02.htm";
				}
				else
				{
					htmltext = "30981-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case BLACK_JUDGE:
					{
						if (cond <= 9)
						{
							htmltext = "30981-07.htm";
						}
						else if ((cond > 9) && (cond < 14))
						{
							htmltext = "30981-08.htm";
							st.setCond(14, true);
							giveItems(player, MANUAL_OF_MANACLES, 1);
						}
						else if (cond == 14)
						{
							htmltext = "30981-09.htm";
						}
						else if (cond == 15)
						{
							htmltext = "30981-10.htm";
						}
						else if (cond == 16)
						{
							if (hasQuestItems(player, PENITENT_MANACLES))
							{
								htmltext = (findSinEaterLvl(player) > st.getInt("level")) ? "30981-13.htm" : "30981-12.htm";
							}
							else
							{
								htmltext = "30981-18.htm";
							}
						}
						break;
					}
					case KATARI:
					{
						if (cond == 2)
						{
							htmltext = "30668-01.htm";
							st.setCond(6, true);
						}
						else if (cond == 6)
						{
							if (getQuestItemsCount(player, RATMAN_SCAVENGER_SKULL) < 10)
							{
								htmltext = "30668-02.htm";
							}
							else
							{
								htmltext = "30668-03.htm";
								st.setCond(10, true);
								takeItems(player, RATMAN_SCAVENGER_SKULL, -1);
							}
						}
						else if (cond == 10)
						{
							htmltext = "30668-04.htm";
						}
						break;
					}
					case PIOTUR:
					{
						if (cond == 3)
						{
							htmltext = "30597-01.htm";
							st.setCond(7, true);
						}
						else if (cond == 7)
						{
							if (getQuestItemsCount(player, TUREK_WAR_HOUND_TAIL) < 10)
							{
								htmltext = "30597-02.htm";
							}
							else
							{
								htmltext = "30597-03.htm";
								st.setCond(11, true);
								takeItems(player, TUREK_WAR_HOUND_TAIL, -1);
							}
						}
						else if (cond == 11)
						{
							htmltext = "30597-04.htm";
						}
						break;
					}
					case CASIAN:
					{
						if (cond == 4)
						{
							htmltext = "30612-01.htm";
							st.setCond(8, true);
						}
						else if (cond == 8)
						{
							if (!hasQuestItems(player, TYRANT_KINGPIN_HEART))
							{
								htmltext = "30612-02.htm";
							}
							else
							{
								htmltext = "30612-03.htm";
								st.setCond(12, true);
								takeItems(player, TYRANT_KINGPIN_HEART, -1);
							}
						}
						else if (cond == 12)
						{
							htmltext = "30612-04.htm";
						}
						break;
					}
					case JOAN:
					{
						if (cond == 5)
						{
							htmltext = "30718-01.htm";
							st.setCond(9, true);
						}
						else if (cond == 9)
						{
							if (getQuestItemsCount(player, TRISALIM_TARANTULA_VENOM_SAC) < 3)
							{
								htmltext = "30718-02.htm";
							}
							else
							{
								htmltext = "30718-03.htm";
								st.setCond(13, true);
								takeItems(player, TRISALIM_TARANTULA_VENOM_SAC, -1);
							}
						}
						else if (cond == 13)
						{
							htmltext = "30718-04.htm";
						}
						break;
					}
					case PUSHKIN:
					{
						if ((cond == 14) && (getQuestItemsCount(player, MANUAL_OF_MANACLES) == 1))
						{
							if ((getQuestItemsCount(player, SILVER_NUGGET) < 10) || (getQuestItemsCount(player, STEEL) < 5) || (getQuestItemsCount(player, ADAMANTINE_NUGGET) < 2) || (getQuestItemsCount(player, COKES) < 10) || (getQuestItemsCount(player, BLACKSMITH_FRAME) < 1))
							{
								htmltext = "30300-02.htm";
							}
							else
							{
								htmltext = "30300-01.htm";
								st.setCond(15, true);
								
								takeItems(player, MANUAL_OF_MANACLES, 1);
								takeItems(player, SILVER_NUGGET, 10);
								takeItems(player, ADAMANTINE_NUGGET, 2);
								takeItems(player, COKES, 10);
								takeItems(player, STEEL, 5);
								takeItems(player, BLACKSMITH_FRAME, 1);
								
								giveItems(player, QITEM_PENITENT_MANACLES, 1);
							}
						}
						else if (hasAtLeastOneQuestItem(player, QITEM_PENITENT_MANACLES, PENITENT_MANACLES, LEFT_PENITENT_MANACLES))
						{
							htmltext = "30300-03.htm";
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
			case 20039:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, RATMAN_SCAVENGER_SKULL) < 10))
				{
					giveItems(player, RATMAN_SCAVENGER_SKULL, 1);
					if (getQuestItemsCount(player, RATMAN_SCAVENGER_SKULL) < 10)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
			case 20494:
			{
				if (st.isCond(7) && (getQuestItemsCount(player, TUREK_WAR_HOUND_TAIL) < 10))
				{
					giveItems(player, TUREK_WAR_HOUND_TAIL, 1);
					if (getQuestItemsCount(player, TUREK_WAR_HOUND_TAIL) < 10)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
			case 20193:
			{
				if (st.isCond(8) && !hasQuestItems(player, TYRANT_KINGPIN_HEART))
				{
					giveItems(player, TYRANT_KINGPIN_HEART, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case 20561:
			{
				if (st.isCond(9) && (getQuestItemsCount(player, TRISALIM_TARANTULA_VENOM_SAC) < 3))
				{
					giveItems(player, TRISALIM_TARANTULA_VENOM_SAC, 1);
					if (getQuestItemsCount(player, TRISALIM_TARANTULA_VENOM_SAC) < 3)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
				}
				break;
			}
		}
	}
	
	private static int findSinEaterLvl(Player player)
	{
		return player.getInventory().getItemByItemId(PENITENT_MANACLES).getEnchantLevel();
	}
}
