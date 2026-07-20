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
package quests.Q00416_PathOfTheOrcShaman;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00416_PathOfTheOrcShaman extends Quest
{
	// NPCs
	private static final int TATARU_ZU_HESTUI = 30585;
	private static final int UMOS = 30502;
	private static final int HESTUI_TOTEM_SPIRIT = 30592;
	private static final int DUDA_MARA_TOTEM_SPIRIT = 30593;
	private static final int MOIRA = 31979;
	private static final int TOTEM_SPIRIT_OF_GANDI = 32057;
	private static final int DEAD_LEOPARD_CARCASS = 32090;
	
	// Monsters
	private static final int VENOMOUS_SPIDER = 20038;
	private static final int ARACHNID_TRACKER = 20043;
	private static final int GRIZZLY_BEAR = 20335;
	private static final int SCARLET_SALAMANDER = 20415;
	private static final int KASHA_BLADE_SPIDER = 20478;
	private static final int KASHA_BEAR = 20479;
	private static final int DURKA_SPIRIT = 27056;
	private static final int BLACK_LEOPARD = 27319;
	
	// Items
	private static final int FIRE_CHARM = 1616;
	private static final int KASHA_BEAR_PELT = 1617;
	private static final int KASHA_BLADE_SPIDER_HUSK = 1618;
	private static final int FIERY_EGG_1 = 1619;
	private static final int HESTUI_MASK = 1620;
	private static final int FIERY_EGG_2 = 1621;
	private static final int TOTEM_SPIRIT_CLAW = 1622;
	private static final int TATARU_LETTER = 1623;
	private static final int FLAME_CHARM = 1624;
	private static final int GRIZZLY_BLOOD = 1625;
	private static final int BLOOD_CAULDRON = 1626;
	private static final int SPIRIT_NET = 1627;
	private static final int BOUND_DURKA_SPIRIT = 1628;
	private static final int DURKA_PARASITE = 1629;
	private static final int TOTEM_SPIRIT_BLOOD = 1630;
	private static final int MASK_OF_MEDIUM = 1631;
	
	public Q00416_PathOfTheOrcShaman()
	{
		super(416, "Path to an Orc Shaman");
		registerQuestItems(FIRE_CHARM, KASHA_BEAR_PELT, KASHA_BLADE_SPIDER_HUSK, FIERY_EGG_1, HESTUI_MASK, FIERY_EGG_2, TOTEM_SPIRIT_CLAW, TATARU_LETTER, FLAME_CHARM, GRIZZLY_BLOOD, BLOOD_CAULDRON, SPIRIT_NET, BOUND_DURKA_SPIRIT, DURKA_PARASITE, TOTEM_SPIRIT_BLOOD);
		addStartNpc(TATARU_ZU_HESTUI);
		addTalkId(TATARU_ZU_HESTUI, UMOS, HESTUI_TOTEM_SPIRIT, DUDA_MARA_TOTEM_SPIRIT, MOIRA, TOTEM_SPIRIT_OF_GANDI, DEAD_LEOPARD_CARCASS);
		addKillId(VENOMOUS_SPIDER, ARACHNID_TRACKER, GRIZZLY_BEAR, SCARLET_SALAMANDER, KASHA_BLADE_SPIDER, KASHA_BEAR, DURKA_SPIRIT, BLACK_LEOPARD);
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
			case "30585-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.ORC_MAGE)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.ORC_SHAMAN) ? "30585-02a.htm" : "30585-02.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30585-03.htm";
				}
				else if (hasQuestItems(player, MASK_OF_MEDIUM))
				{
					htmltext = "30585-04.htm";
				}
				break;
			}
			case "30585-06.htm":
			{
				st.startQuest();
				giveItems(player, FIRE_CHARM, 1);
				break;
			}
			case "30585-11b.htm":
			{
				st.setCond(5, true);
				takeItems(player, TOTEM_SPIRIT_CLAW, 1);
				giveItems(player, TATARU_LETTER, 1);
				break;
			}
			case "30585-11c.htm":
			{
				st.setCond(12, true);
				takeItems(player, TOTEM_SPIRIT_CLAW, 1);
				break;
			}
			case "30592-03.htm":
			{
				st.setCond(4, true);
				takeItems(player, HESTUI_MASK, 1);
				takeItems(player, FIERY_EGG_2, 1);
				giveItems(player, TOTEM_SPIRIT_CLAW, 1);
				break;
			}
			case "30593-03.htm":
			{
				st.setCond(9, true);
				takeItems(player, BLOOD_CAULDRON, 1);
				giveItems(player, SPIRIT_NET, 1);
				break;
			}
			case "32057-02.htm":
			{
				st.setCond(14, true);
				break;
			}
			case "32057-05.htm":
			{
				st.setCond(21, true);
				break;
			}
			case "32090-04.htm":
			{
				st.setCond(18, true);
				break;
			}
			case "30502-07.htm":
			{
				takeItems(player, TOTEM_SPIRIT_BLOOD, -1);
				giveItems(player, MASK_OF_MEDIUM, 1);
				addExpAndSp(player, 3200, 2600);
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
				htmltext = "30585-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case TATARU_ZU_HESTUI:
					{
						if (cond == 1)
						{
							htmltext = "30585-07.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30585-08.htm";
							st.setCond(3, true);
							takeItems(player, FIERY_EGG_1, 1);
							takeItems(player, FIRE_CHARM, 1);
							takeItems(player, KASHA_BEAR_PELT, 1);
							takeItems(player, KASHA_BLADE_SPIDER_HUSK, 1);
							giveItems(player, FIERY_EGG_2, 1);
							giveItems(player, HESTUI_MASK, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30585-09.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30585-10.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30585-12.htm";
						}
						else if ((cond > 5) && (cond < 12))
						{
							htmltext = "30585-13.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30585-11c.htm";
						}
						break;
					}
					case HESTUI_TOTEM_SPIRIT:
					{
						if (cond == 3)
						{
							htmltext = "30592-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30592-04.htm";
						}
						else if ((cond > 4) && (cond < 12))
						{
							htmltext = "30592-05.htm";
						}
						break;
					}
					case UMOS:
					{
						if (cond == 5)
						{
							htmltext = "30502-01.htm";
							st.setCond(6, true);
							takeItems(player, TATARU_LETTER, 1);
							giveItems(player, FLAME_CHARM, 1);
						}
						else if (cond == 6)
						{
							htmltext = "30502-02.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30502-03.htm";
							st.setCond(8, true);
							takeItems(player, FLAME_CHARM, 1);
							takeItems(player, GRIZZLY_BLOOD, 3);
							giveItems(player, BLOOD_CAULDRON, 1);
						}
						else if (cond == 8)
						{
							htmltext = "30502-04.htm";
						}
						else if ((cond == 9) || (cond == 10))
						{
							htmltext = "30502-05.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30502-06.htm";
						}
						break;
					}
					case MOIRA:
					{
						if (cond == 12)
						{
							htmltext = "31979-01.htm";
							st.setCond(13, true);
						}
						else if ((cond > 12) && (cond < 21))
						{
							htmltext = "31979-02.htm";
						}
						else if (cond == 21)
						{
							htmltext = "31979-03.htm";
							giveItems(player, MASK_OF_MEDIUM, 1);
							addExpAndSp(player, 3200, 3250);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case TOTEM_SPIRIT_OF_GANDI:
					{
						if (cond == 13)
						{
							htmltext = "32057-01.htm";
						}
						else if ((cond > 13) && (cond < 20))
						{
							htmltext = "32057-03.htm";
						}
						else if (cond == 20)
						{
							htmltext = "32057-04.htm";
						}
						break;
					}
					case DUDA_MARA_TOTEM_SPIRIT:
					{
						if (cond == 8)
						{
							htmltext = "30593-01.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30593-04.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30593-05.htm";
							st.setCond(11, true);
							takeItems(player, BOUND_DURKA_SPIRIT, 1);
							giveItems(player, TOTEM_SPIRIT_BLOOD, 1);
						}
						else if (cond == 11)
						{
							htmltext = "30593-06.htm";
						}
						break;
					}
					case DEAD_LEOPARD_CARCASS:
					{
						if (cond == 14)
						{
							htmltext = "32090-01a.htm";
						}
						else if (cond == 15)
						{
							htmltext = "32090-01.htm";
							st.setCond(16, true);
						}
						else if (cond == 16)
						{
							htmltext = "32090-01b.htm";
						}
						else if (cond == 17)
						{
							htmltext = "32090-02.htm";
						}
						else if (cond == 18)
						{
							htmltext = "32090-05.htm";
						}
						else if (cond == 19)
						{
							htmltext = "32090-06.htm";
							st.setCond(20, true);
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
			case KASHA_BEAR:
			{
				if (st.isCond(1) && !hasQuestItems(player, KASHA_BEAR_PELT))
				{
					giveItems(player, KASHA_BEAR_PELT, 1);
					if (hasQuestItems(player, FIERY_EGG_1, KASHA_BLADE_SPIDER_HUSK))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case KASHA_BLADE_SPIDER:
			{
				if (st.isCond(1) && !hasQuestItems(player, KASHA_BLADE_SPIDER_HUSK))
				{
					giveItems(player, KASHA_BLADE_SPIDER_HUSK, 1);
					if (hasQuestItems(player, KASHA_BEAR_PELT, FIERY_EGG_1))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case SCARLET_SALAMANDER:
			{
				if (st.isCond(1) && !hasQuestItems(player, FIERY_EGG_1))
				{
					giveItems(player, FIERY_EGG_1, 1);
					if (hasQuestItems(player, KASHA_BEAR_PELT, KASHA_BLADE_SPIDER_HUSK))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case GRIZZLY_BEAR:
			{
				if (st.isCond(6))
				{
					if (getQuestItemsCount(player, GRIZZLY_BLOOD) < 3)
					{
						giveItems(player, GRIZZLY_BLOOD, 1);
						
						if (getQuestItemsCount(player, GRIZZLY_BLOOD) >= 3)
						{
							st.setCond(7, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case VENOMOUS_SPIDER:
			case ARACHNID_TRACKER:
			{
				if (st.isCond(9))
				{
					final int count = getQuestItemsCount(player, DURKA_PARASITE);
					final int rnd = getRandom(10);
					if (((count == 5) && (rnd < 1)) || (((count == 6) || (count == 7)) && (rnd < 2)) || (count >= 8))
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_BEFORE_BATTLE);
						takeItems(player, DURKA_PARASITE, -1);
						addSpawn(DURKA_SPIRIT, npc, false, 120000);
					}
					else
					{
						giveItems(player, DURKA_PARASITE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case DURKA_SPIRIT:
			{
				if (st.isCond(9))
				{
					st.setCond(10, true);
					takeItems(player, DURKA_PARASITE, -1);
					takeItems(player, SPIRIT_NET, 1);
					giveItems(player, BOUND_DURKA_SPIRIT, 1);
				}
				break;
			}
			case BLACK_LEOPARD:
			{
				if (st.isCond(14))
				{
					if (st.getInt("leopard") > 0)
					{
						st.setCond(15, true);
						
						if (getRandom(3) < 2)
						{
							npc.broadcastSay(ChatType.GENERAL, "My dear friend of " + player.getName() + ", who has gone on ahead of me!");
						}
					}
					else
					{
						st.set("leopard", "1");
					}
				}
				else if (st.isCond(16))
				{
					st.setCond(17, true);
					
					if (getRandom(3) < 2)
					{
						npc.broadcastSay(ChatType.GENERAL, "Listen to Tejakar Gandi, young Oroka! The spirit of the slain leopard is calling you, " + player.getName() + "!");
					}
				}
				else if (st.isCond(18))
				{
					st.setCond(19, true);
				}
				break;
			}
		}
	}
}
