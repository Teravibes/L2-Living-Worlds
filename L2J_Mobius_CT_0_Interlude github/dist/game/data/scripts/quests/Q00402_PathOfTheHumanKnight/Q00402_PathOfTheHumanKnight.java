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
package quests.Q00402_PathOfTheHumanKnight;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00402_PathOfTheHumanKnight extends Quest
{
	// NPCs
	private static final int SIR_KLAUS_VASPER = 30417;
	private static final int BATHIS = 30332;
	private static final int RAYMOND = 30289;
	private static final int BEZIQUE = 30379;
	private static final int LEVIAN = 30037;
	private static final int GILBERT = 30039;
	private static final int BIOTIN = 30031;
	private static final int SIR_AARON_TANFORD = 30653;
	private static final int SIR_COLLIN_WINDAWOOD = 30311;
	
	// Items
	private static final int SWORD_OF_RITUAL = 1161;
	private static final int COIN_OF_LORDS_1 = 1162;
	private static final int COIN_OF_LORDS_2 = 1163;
	private static final int COIN_OF_LORDS_3 = 1164;
	private static final int COIN_OF_LORDS_4 = 1165;
	private static final int COIN_OF_LORDS_5 = 1166;
	private static final int COIN_OF_LORDS_6 = 1167;
	private static final int GLUDIO_GUARD_MARK_1 = 1168;
	private static final int BUGBEAR_NECKLACE = 1169;
	private static final int EINHASAD_CHURCH_MARK_1 = 1170;
	private static final int EINHASAD_CRUCIFIX = 1171;
	private static final int GLUDIO_GUARD_MARK_2 = 1172;
	private static final int SPIDER_LEG = 1173;
	private static final int EINHASAD_CHURCH_MARK_2 = 1174;
	private static final int LIZARDMAN_TOTEM = 1175;
	private static final int GLUDIO_GUARD_MARK_3 = 1176;
	private static final int GIANT_SPIDER_HUSK = 1177;
	private static final int EINHASAD_CHURCH_MARK_3 = 1178;
	private static final int HORRIBLE_SKULL = 1179;
	private static final int MARK_OF_ESQUIRE = 1271;
	
	public Q00402_PathOfTheHumanKnight()
	{
		super(402, "Path to a Human Knight");
		registerQuestItems(MARK_OF_ESQUIRE, COIN_OF_LORDS_1, COIN_OF_LORDS_2, COIN_OF_LORDS_3, COIN_OF_LORDS_4, COIN_OF_LORDS_5, COIN_OF_LORDS_6, GLUDIO_GUARD_MARK_1, BUGBEAR_NECKLACE, EINHASAD_CHURCH_MARK_1, EINHASAD_CRUCIFIX, GLUDIO_GUARD_MARK_2, SPIDER_LEG, EINHASAD_CHURCH_MARK_2, LIZARDMAN_TOTEM, GLUDIO_GUARD_MARK_3, GIANT_SPIDER_HUSK, EINHASAD_CHURCH_MARK_3, LIZARDMAN_TOTEM, GLUDIO_GUARD_MARK_3, GIANT_SPIDER_HUSK, EINHASAD_CHURCH_MARK_3, HORRIBLE_SKULL);
		addStartNpc(SIR_KLAUS_VASPER);
		addTalkId(SIR_KLAUS_VASPER, BATHIS, RAYMOND, BEZIQUE, LEVIAN, GILBERT, BIOTIN, SIR_AARON_TANFORD, SIR_COLLIN_WINDAWOOD);
		addKillId(20775, 27024, 20038, 20043, 20050, 20030, 20027, 20024, 20103, 20106, 20108, 20404);
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
			case "30417-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.KNIGHT) ? "30417-02a.htm" : "30417-03.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30417-02.htm";
				}
				else if (hasQuestItems(player, SWORD_OF_RITUAL))
				{
					htmltext = "30417-04.htm";
				}
				break;
			}
			case "30417-08.htm":
			{
				st.startQuest();
				giveItems(player, MARK_OF_ESQUIRE, 1);
				break;
			}
			case "30332-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				giveItems(player, GLUDIO_GUARD_MARK_1, 1);
				break;
			}
			case "30289-03.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				giveItems(player, EINHASAD_CHURCH_MARK_1, 1);
				break;
			}
			case "30379-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				giveItems(player, GLUDIO_GUARD_MARK_2, 1);
				break;
			}
			case "30037-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				giveItems(player, EINHASAD_CHURCH_MARK_2, 1);
				break;
			}
			case "30039-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				giveItems(player, GLUDIO_GUARD_MARK_3, 1);
				break;
			}
			case "30031-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				giveItems(player, EINHASAD_CHURCH_MARK_3, 1);
				break;
			}
			case "30417-13.htm":
			case "30417-14.htm":
			{
				final int coinCount = getQuestItemsCount(player, COIN_OF_LORDS_1) + getQuestItemsCount(player, COIN_OF_LORDS_2) + getQuestItemsCount(player, COIN_OF_LORDS_3) + getQuestItemsCount(player, COIN_OF_LORDS_4) + getQuestItemsCount(player, COIN_OF_LORDS_5) + getQuestItemsCount(player, COIN_OF_LORDS_6);
				takeItems(player, COIN_OF_LORDS_1, -1);
				takeItems(player, COIN_OF_LORDS_2, -1);
				takeItems(player, COIN_OF_LORDS_3, -1);
				takeItems(player, COIN_OF_LORDS_4, -1);
				takeItems(player, COIN_OF_LORDS_5, -1);
				takeItems(player, COIN_OF_LORDS_6, -1);
				takeItems(player, MARK_OF_ESQUIRE, 1);
				giveItems(player, SWORD_OF_RITUAL, 1);
				addExpAndSp(player, 3200, 1500 + (1920 * (coinCount - 3)));
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(true, true);
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
				htmltext = "30417-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case SIR_KLAUS_VASPER:
					{
						final int coins = getQuestItemsCount(player, COIN_OF_LORDS_1) + getQuestItemsCount(player, COIN_OF_LORDS_2) + getQuestItemsCount(player, COIN_OF_LORDS_3) + getQuestItemsCount(player, COIN_OF_LORDS_4) + getQuestItemsCount(player, COIN_OF_LORDS_5) + getQuestItemsCount(player, COIN_OF_LORDS_6);
						if (coins < 3)
						{
							htmltext = "30417-09.htm";
						}
						else if (coins == 3)
						{
							htmltext = "30417-10.htm";
						}
						else if ((coins > 3) && (coins < 6))
						{
							htmltext = "30417-11.htm";
						}
						else if (coins == 6)
						{
							htmltext = "30417-12.htm";
							takeItems(player, COIN_OF_LORDS_1, -1);
							takeItems(player, COIN_OF_LORDS_2, -1);
							takeItems(player, COIN_OF_LORDS_3, -1);
							takeItems(player, COIN_OF_LORDS_4, -1);
							takeItems(player, COIN_OF_LORDS_5, -1);
							takeItems(player, COIN_OF_LORDS_6, -1);
							takeItems(player, MARK_OF_ESQUIRE, 1);
							giveItems(player, SWORD_OF_RITUAL, 1);
							addExpAndSp(player, 3200, 7260);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case BATHIS:
					{
						if (hasQuestItems(player, COIN_OF_LORDS_1))
						{
							htmltext = "30332-05.htm";
						}
						else if (hasQuestItems(player, GLUDIO_GUARD_MARK_1))
						{
							if (getQuestItemsCount(player, BUGBEAR_NECKLACE) < 10)
							{
								htmltext = "30332-03.htm";
							}
							else
							{
								htmltext = "30332-04.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
								takeItems(player, BUGBEAR_NECKLACE, -1);
								takeItems(player, GLUDIO_GUARD_MARK_1, 1);
								giveItems(player, COIN_OF_LORDS_1, 1);
							}
						}
						else
						{
							htmltext = "30332-01.htm";
						}
						break;
					}
					case RAYMOND:
					{
						if (hasQuestItems(player, COIN_OF_LORDS_2))
						{
							htmltext = "30289-06.htm";
						}
						else if (hasQuestItems(player, EINHASAD_CHURCH_MARK_1))
						{
							if (getQuestItemsCount(player, EINHASAD_CRUCIFIX) < 12)
							{
								htmltext = "30289-04.htm";
							}
							else
							{
								htmltext = "30289-05.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
								takeItems(player, EINHASAD_CRUCIFIX, -1);
								takeItems(player, EINHASAD_CHURCH_MARK_1, 1);
								giveItems(player, COIN_OF_LORDS_2, 1);
							}
						}
						else
						{
							htmltext = "30289-01.htm";
						}
						break;
					}
					case BEZIQUE:
					{
						if (hasQuestItems(player, COIN_OF_LORDS_3))
						{
							htmltext = "30379-05.htm";
						}
						else if (hasQuestItems(player, GLUDIO_GUARD_MARK_2))
						{
							if (getQuestItemsCount(player, SPIDER_LEG) < 20)
							{
								htmltext = "30379-03.htm";
							}
							else
							{
								htmltext = "30379-04.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
								takeItems(player, SPIDER_LEG, -1);
								takeItems(player, GLUDIO_GUARD_MARK_2, 1);
								giveItems(player, COIN_OF_LORDS_3, 1);
							}
						}
						else
						{
							htmltext = "30379-01.htm";
						}
						break;
					}
					case LEVIAN:
					{
						if (hasQuestItems(player, COIN_OF_LORDS_4))
						{
							htmltext = "30037-05.htm";
						}
						else if (hasQuestItems(player, EINHASAD_CHURCH_MARK_2))
						{
							if (getQuestItemsCount(player, LIZARDMAN_TOTEM) < 20)
							{
								htmltext = "30037-03.htm";
							}
							else
							{
								htmltext = "30037-04.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
								takeItems(player, LIZARDMAN_TOTEM, -1);
								takeItems(player, EINHASAD_CHURCH_MARK_2, 1);
								giveItems(player, COIN_OF_LORDS_4, 1);
							}
						}
						else
						{
							htmltext = "30037-01.htm";
						}
						break;
					}
					case GILBERT:
					{
						if (hasQuestItems(player, COIN_OF_LORDS_5))
						{
							htmltext = "30039-05.htm";
						}
						else if (hasQuestItems(player, GLUDIO_GUARD_MARK_3))
						{
							if (getQuestItemsCount(player, GIANT_SPIDER_HUSK) < 20)
							{
								htmltext = "30039-03.htm";
							}
							else
							{
								htmltext = "30039-04.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
								takeItems(player, GIANT_SPIDER_HUSK, -1);
								takeItems(player, GLUDIO_GUARD_MARK_3, 1);
								giveItems(player, COIN_OF_LORDS_5, 1);
							}
						}
						else
						{
							htmltext = "30039-01.htm";
						}
						break;
					}
					case BIOTIN:
					{
						if (hasQuestItems(player, COIN_OF_LORDS_6))
						{
							htmltext = "30031-05.htm";
						}
						else if (hasQuestItems(player, EINHASAD_CHURCH_MARK_3))
						{
							if (getQuestItemsCount(player, HORRIBLE_SKULL) < 10)
							{
								htmltext = "30031-03.htm";
							}
							else
							{
								htmltext = "30031-04.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
								takeItems(player, HORRIBLE_SKULL, -1);
								takeItems(player, EINHASAD_CHURCH_MARK_3, 1);
								giveItems(player, COIN_OF_LORDS_6, 1);
							}
						}
						else
						{
							htmltext = "30031-01.htm";
						}
						break;
					}
					case SIR_AARON_TANFORD:
					{
						htmltext = "30653-01.htm";
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
			case 20775: // Bugbear Raider
			{
				if (hasQuestItems(player, GLUDIO_GUARD_MARK_1) && (getQuestItemsCount(player, BUGBEAR_NECKLACE) < 10))
				{
					giveItems(player, BUGBEAR_NECKLACE, 1);
					if (getQuestItemsCount(player, BUGBEAR_NECKLACE) < 10)
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
			case 27024: // Undead Priest
			{
				if (hasQuestItems(player, EINHASAD_CHURCH_MARK_1) && (getQuestItemsCount(player, EINHASAD_CRUCIFIX) < 12) && (getRandom(10) < 5))
				{
					giveItems(player, EINHASAD_CRUCIFIX, 1);
					if (getQuestItemsCount(player, EINHASAD_CRUCIFIX) < 10)
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
			case 20038: // Poison Spider
			case 20043: // Arachnid Tracker
			case 20050: // Arachnid Predator
			{
				if (hasQuestItems(player, GLUDIO_GUARD_MARK_2) && (getQuestItemsCount(player, SPIDER_LEG) < 20))
				{
					giveItems(player, SPIDER_LEG, 1);
					if (getQuestItemsCount(player, SPIDER_LEG) < 20)
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
			case 20030: // Langk Lizardman
			case 20027: // Langk Lizardman Scout
			case 20024: // Langk Lizardman Warrior
			{
				if (hasQuestItems(player, EINHASAD_CHURCH_MARK_2) && (getQuestItemsCount(player, LIZARDMAN_TOTEM) < 20) && (getRandom(10) < 5))
				{
					giveItems(player, LIZARDMAN_TOTEM, 1);
					if (getQuestItemsCount(player, LIZARDMAN_TOTEM) < 20)
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
			case 20103: // Giant Spider
			case 20106: // Talon Spider
			case 20108: // Blade Spider
			{
				if (hasQuestItems(player, GLUDIO_GUARD_MARK_3) && (getQuestItemsCount(player, GIANT_SPIDER_HUSK) < 20) && (getRandom(10) < 4))
				{
					giveItems(player, GIANT_SPIDER_HUSK, 1);
					if (getQuestItemsCount(player, GIANT_SPIDER_HUSK) < 20)
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
			case 20404: // Silent Horror
			{
				if (hasQuestItems(player, EINHASAD_CHURCH_MARK_3) && (getQuestItemsCount(player, HORRIBLE_SKULL) < 10) && (getRandom(10) < 4))
				{
					giveItems(player, HORRIBLE_SKULL, 1);
					if (getQuestItemsCount(player, HORRIBLE_SKULL) < 10)
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
}
