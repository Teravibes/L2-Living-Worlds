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
package quests.Q00415_PathOfTheOrcMonk;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00415_PathOfTheOrcMonk extends Quest
{
	// NPCs
	private static final int GANTAKI = 30587;
	private static final int ROSHEEK = 30590;
	private static final int KASMAN = 30501;
	private static final int TORUKU = 30591;
	private static final int AREN = 32056;
	private static final int MOIRA = 31979;
	
	// Items
	private static final int POMEGRANATE = 1593;
	private static final int LEATHER_POUCH_1 = 1594;
	private static final int LEATHER_POUCH_2 = 1595;
	private static final int LEATHER_POUCH_3 = 1596;
	private static final int LEATHER_POUCH_FULL_1 = 1597;
	private static final int LEATHER_POUCH_FULL_2 = 1598;
	private static final int LEATHER_POUCH_FULL_3 = 1599;
	private static final int KASHA_BEAR_CLAW = 1600;
	private static final int KASHA_BLADE_SPIDER_TALON = 1601;
	private static final int SCARLET_SALAMANDER_SCALE = 1602;
	private static final int FIERY_SPIRIT_SCROLL = 1603;
	private static final int ROSHEEK_LETTER = 1604;
	private static final int GANTAKI_LETTER_OF_RECOMMENDATION = 1605;
	private static final int FIG = 1606;
	private static final int LEATHER_POUCH_4 = 1607;
	private static final int LEATHER_POUCH_FULL_4 = 1608;
	private static final int VUKU_ORC_TUSK = 1609;
	private static final int RATMAN_FANG = 1610;
	private static final int LANG_KLIZARDMAN_TOOTH = 1611;
	private static final int FELIM_LIZARDMAN_TOOTH = 1612;
	private static final int IRON_WILL_SCROLL = 1613;
	private static final int TORUKU_LETTER = 1614;
	private static final int KHAVATARI_TOTEM = 1615;
	private static final int KASHA_SPIDER_TOOTH = 8545;
	private static final int HORN_OF_BAAR_DRE_VANUL = 8546;
	
	public Q00415_PathOfTheOrcMonk()
	{
		super(415, "Path to a Monk");
		registerQuestItems(POMEGRANATE, LEATHER_POUCH_1, LEATHER_POUCH_2, LEATHER_POUCH_3, LEATHER_POUCH_FULL_1, LEATHER_POUCH_FULL_2, LEATHER_POUCH_FULL_3, KASHA_BEAR_CLAW, KASHA_BLADE_SPIDER_TALON, SCARLET_SALAMANDER_SCALE, FIERY_SPIRIT_SCROLL, ROSHEEK_LETTER, GANTAKI_LETTER_OF_RECOMMENDATION, FIG, LEATHER_POUCH_4, LEATHER_POUCH_FULL_4, VUKU_ORC_TUSK, RATMAN_FANG, LANG_KLIZARDMAN_TOOTH, FELIM_LIZARDMAN_TOOTH, IRON_WILL_SCROLL, TORUKU_LETTER, KASHA_SPIDER_TOOTH, HORN_OF_BAAR_DRE_VANUL);
		addStartNpc(GANTAKI);
		addTalkId(GANTAKI, ROSHEEK, KASMAN, TORUKU, AREN, MOIRA);
		addKillId(20014, 20017, 20024, 20359, 20415, 20476, 20478, 20479, 21118);
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
			case "30587-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.ORC_FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.ORC_MONK) ? "30587-02a.htm" : "30587-02.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30587-03.htm";
				}
				else if (hasQuestItems(player, KHAVATARI_TOTEM))
				{
					htmltext = "30587-04.htm";
				}
				break;
			}
			case "30587-06.htm":
			{
				st.startQuest();
				giveItems(player, POMEGRANATE, 1);
				break;
			}
			case "30587-09a.htm":
			{
				st.setCond(9, true);
				takeItems(player, ROSHEEK_LETTER, 1);
				giveItems(player, GANTAKI_LETTER_OF_RECOMMENDATION, 1);
				break;
			}
			case "30587-09b.htm":
			{
				st.setCond(14, true);
				takeItems(player, ROSHEEK_LETTER, 1);
				break;
			}
			case "32056-03.htm":
			{
				st.setCond(15, true);
				break;
			}
			case "32056-08.htm":
			{
				st.setCond(20, true);
				break;
			}
			case "31979-03.htm":
			{
				takeItems(player, FIERY_SPIRIT_SCROLL, 1);
				giveItems(player, KHAVATARI_TOTEM, 1);
				addExpAndSp(player, 3200, 4230);
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
				htmltext = "30587-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case GANTAKI:
					{
						if (cond == 1)
						{
							htmltext = "30587-07.htm";
						}
						else if ((cond > 1) && (cond < 8))
						{
							htmltext = "30587-08.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30587-09.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30587-10.htm";
						}
						else if (cond > 9)
						{
							htmltext = "30587-11.htm";
						}
						break;
					}
					case ROSHEEK:
					{
						if (cond == 1)
						{
							htmltext = "30590-01.htm";
							st.setCond(2, true);
							takeItems(player, POMEGRANATE, 1);
							giveItems(player, LEATHER_POUCH_1, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30590-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30590-03.htm";
							st.setCond(4, true);
							takeItems(player, LEATHER_POUCH_FULL_1, 1);
							giveItems(player, LEATHER_POUCH_2, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30590-04.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30590-05.htm";
							st.setCond(6, true);
							takeItems(player, LEATHER_POUCH_FULL_2, 1);
							giveItems(player, LEATHER_POUCH_3, 1);
						}
						else if (cond == 6)
						{
							htmltext = "30590-06.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30590-07.htm";
							st.setCond(8, true);
							takeItems(player, LEATHER_POUCH_FULL_3, 1);
							giveItems(player, FIERY_SPIRIT_SCROLL, 1);
							giveItems(player, ROSHEEK_LETTER, 1);
						}
						else if (cond == 8)
						{
							htmltext = "30590-08.htm";
						}
						else if (cond > 8)
						{
							htmltext = "30590-09.htm";
						}
						break;
					}
					case KASMAN:
					{
						if (cond == 9)
						{
							htmltext = "30501-01.htm";
							st.setCond(10, true);
							takeItems(player, GANTAKI_LETTER_OF_RECOMMENDATION, 1);
							giveItems(player, FIG, 1);
						}
						else if (cond == 10)
						{
							htmltext = "30501-02.htm";
						}
						else if ((cond == 11) || (cond == 12))
						{
							htmltext = "30501-03.htm";
						}
						else if (cond == 13)
						{
							htmltext = "30501-04.htm";
							takeItems(player, FIERY_SPIRIT_SCROLL, 1);
							takeItems(player, IRON_WILL_SCROLL, 1);
							takeItems(player, TORUKU_LETTER, 1);
							giveItems(player, KHAVATARI_TOTEM, 1);
							addExpAndSp(player, 3200, 1500);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case TORUKU:
					{
						if (cond == 10)
						{
							htmltext = "30591-01.htm";
							st.setCond(11, true);
							takeItems(player, FIG, 1);
							giveItems(player, LEATHER_POUCH_4, 1);
						}
						else if (cond == 11)
						{
							htmltext = "30591-02.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30591-03.htm";
							st.setCond(13, true);
							takeItems(player, LEATHER_POUCH_FULL_4, 1);
							giveItems(player, IRON_WILL_SCROLL, 1);
							giveItems(player, TORUKU_LETTER, 1);
						}
						else if (cond == 13)
						{
							htmltext = "30591-04.htm";
						}
						break;
					}
					case AREN:
					{
						if (cond == 14)
						{
							htmltext = "32056-01.htm";
						}
						else if (cond == 15)
						{
							htmltext = "32056-04.htm";
						}
						else if (cond == 16)
						{
							htmltext = "32056-05.htm";
							st.setCond(17, true);
							takeItems(player, KASHA_SPIDER_TOOTH, -1);
						}
						else if (cond == 17)
						{
							htmltext = "32056-06.htm";
						}
						else if (cond == 18)
						{
							htmltext = "32056-07.htm";
							st.setCond(19, true);
							takeItems(player, HORN_OF_BAAR_DRE_VANUL, -1);
						}
						else if (cond == 20)
						{
							htmltext = "32056-09.htm";
						}
						break;
					}
					case MOIRA:
					{
						if (cond == 20)
						{
							htmltext = "31979-01.htm";
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
		
		final Weapon weapon = player.getActiveWeaponItem();
		if ((weapon != null) && (weapon.getItemType() != WeaponType.DUALFIST) && (weapon.getItemType() != WeaponType.FIST))
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_GIVEUP);
			st.exitQuest(true);
			return;
		}
		
		switch (npc.getId())
		{
			case 20479:
			{
				if (st.isCond(2))
				{
					giveItems(player, KASHA_BEAR_CLAW, 1);
					if (getQuestItemsCount(player, KASHA_BEAR_CLAW) < 5)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(3, true);
						takeItems(player, KASHA_BEAR_CLAW, -1);
						takeItems(player, LEATHER_POUCH_1, 1);
						giveItems(player, LEATHER_POUCH_FULL_1, 1);
					}
				}
				break;
			}
			case 20478:
			{
				if (st.isCond(4))
				{
					giveItems(player, KASHA_BLADE_SPIDER_TALON, 1);
					if (getQuestItemsCount(player, KASHA_BLADE_SPIDER_TALON) < 5)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(5, true);
						takeItems(player, KASHA_BLADE_SPIDER_TALON, -1);
						takeItems(player, LEATHER_POUCH_2, 1);
						giveItems(player, LEATHER_POUCH_FULL_2, 1);
					}
				}
				else if (st.isCond(15) && getRandomBoolean())
				{
					giveItems(player, KASHA_SPIDER_TOOTH, 1);
					if (getQuestItemsCount(player, KASHA_SPIDER_TOOTH) < 6)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(16, true);
					}
				}
				break;
			}
			case 20476:
			{
				if (st.isCond(15) && getRandomBoolean())
				{
					giveItems(player, KASHA_SPIDER_TOOTH, 1);
					if (getQuestItemsCount(player, KASHA_SPIDER_TOOTH) < 6)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(16, true);
					}
				}
				break;
			}
			case 20415:
			{
				if (st.isCond(6))
				{
					giveItems(player, SCARLET_SALAMANDER_SCALE, 1);
					if (getQuestItemsCount(player, SCARLET_SALAMANDER_SCALE) < 5)
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(7, true);
						takeItems(player, SCARLET_SALAMANDER_SCALE, -1);
						takeItems(player, LEATHER_POUCH_3, 1);
						giveItems(player, LEATHER_POUCH_FULL_3, 1);
					}
				}
				break;
			}
			case 20014:
			{
				if (st.isCond(11))
				{
					if (getQuestItemsCount(player, FELIM_LIZARDMAN_TOOTH) < 3)
					{
						giveItems(player, FELIM_LIZARDMAN_TOOTH, 1);
					}
					
					if ((getQuestItemsCount(player, FELIM_LIZARDMAN_TOOTH) >= 3) && (getQuestItemsCount(player, RATMAN_FANG) >= 3) && (getQuestItemsCount(player, LANG_KLIZARDMAN_TOOTH) >= 3) && (getQuestItemsCount(player, VUKU_ORC_TUSK) >= 3))
					{
						st.setCond(12, true);
						takeItems(player, VUKU_ORC_TUSK, -1);
						takeItems(player, RATMAN_FANG, -1);
						takeItems(player, LANG_KLIZARDMAN_TOOTH, -1);
						takeItems(player, FELIM_LIZARDMAN_TOOTH, -1);
						takeItems(player, LEATHER_POUCH_4, 1);
						giveItems(player, LEATHER_POUCH_FULL_4, 1);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20017:
			{
				if (st.isCond(11))
				{
					if (getQuestItemsCount(player, VUKU_ORC_TUSK) < 3)
					{
						giveItems(player, VUKU_ORC_TUSK, 1);
					}
					
					if ((getQuestItemsCount(player, VUKU_ORC_TUSK) >= 3) && (getQuestItemsCount(player, RATMAN_FANG) >= 3) && (getQuestItemsCount(player, LANG_KLIZARDMAN_TOOTH) >= 3) && (getQuestItemsCount(player, FELIM_LIZARDMAN_TOOTH) >= 3))
					{
						st.setCond(12, true);
						takeItems(player, VUKU_ORC_TUSK, -1);
						takeItems(player, RATMAN_FANG, -1);
						takeItems(player, LANG_KLIZARDMAN_TOOTH, -1);
						takeItems(player, FELIM_LIZARDMAN_TOOTH, -1);
						takeItems(player, LEATHER_POUCH_4, 1);
						giveItems(player, LEATHER_POUCH_FULL_4, 1);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20024:
			{
				if (st.isCond(11))
				{
					if (getQuestItemsCount(player, LANG_KLIZARDMAN_TOOTH) < 3)
					{
						giveItems(player, LANG_KLIZARDMAN_TOOTH, 1);
					}
					
					if ((getQuestItemsCount(player, LANG_KLIZARDMAN_TOOTH) >= 3) && (getQuestItemsCount(player, RATMAN_FANG) >= 3) && (getQuestItemsCount(player, FELIM_LIZARDMAN_TOOTH) >= 3) && (getQuestItemsCount(player, VUKU_ORC_TUSK) >= 3))
					{
						st.setCond(12, true);
						takeItems(player, VUKU_ORC_TUSK, -1);
						takeItems(player, RATMAN_FANG, -1);
						takeItems(player, LANG_KLIZARDMAN_TOOTH, -1);
						takeItems(player, FELIM_LIZARDMAN_TOOTH, -1);
						takeItems(player, LEATHER_POUCH_4, 1);
						giveItems(player, LEATHER_POUCH_FULL_4, 1);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20359:
			{
				if (st.isCond(11))
				{
					if (getQuestItemsCount(player, RATMAN_FANG) < 3)
					{
						giveItems(player, RATMAN_FANG, 1);
					}
					
					if ((getQuestItemsCount(player, RATMAN_FANG) >= 3) && (getQuestItemsCount(player, LANG_KLIZARDMAN_TOOTH) >= 3) && (getQuestItemsCount(player, FELIM_LIZARDMAN_TOOTH) >= 3) && (getQuestItemsCount(player, VUKU_ORC_TUSK) >= 3))
					{
						st.setCond(12, true);
						takeItems(player, VUKU_ORC_TUSK, -1);
						takeItems(player, RATMAN_FANG, -1);
						takeItems(player, LANG_KLIZARDMAN_TOOTH, -1);
						takeItems(player, FELIM_LIZARDMAN_TOOTH, -1);
						takeItems(player, LEATHER_POUCH_4, 1);
						giveItems(player, LEATHER_POUCH_FULL_4, 1);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 21118:
			{
				if (st.isCond(17))
				{
					st.setCond(18, true);
					giveItems(player, HORN_OF_BAAR_DRE_VANUL, 1);
				}
				break;
			}
		}
	}
}
