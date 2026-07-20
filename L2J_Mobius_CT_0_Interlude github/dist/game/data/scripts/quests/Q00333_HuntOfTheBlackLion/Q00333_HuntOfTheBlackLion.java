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
package quests.Q00333_HuntOfTheBlackLion;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00333_HuntOfTheBlackLion extends Quest
{
	// NPCs
	private static final int SOPHYA = 30735;
	private static final int REDFOOT = 30736;
	private static final int RUPIO = 30471;
	private static final int UNDRIAS = 30130;
	private static final int LOCKIRIN = 30531;
	private static final int MORGAN = 30737;
	
	// Needs for start
	private static final int BLACK_LION_MARK = 1369;
	
	// Quest items
	private static final int LION_CLAW = 3675;
	private static final int LION_EYE = 3676;
	private static final int GUILD_COIN = 3677;
	private static final int UNDEAD_ASH = 3848;
	private static final int BLOODY_AXE_INSIGNIA = 3849;
	private static final int DELU_FANG = 3850;
	private static final int STAKATO_TALON = 3851;
	private static final int SOPHYA_LETTER_1 = 3671;
	private static final int SOPHYA_LETTER_2 = 3672;
	private static final int SOPHYA_LETTER_3 = 3673;
	private static final int SOPHYA_LETTER_4 = 3674;
	private static final int CARGO_BOX_1 = 3440;
	private static final int CARGO_BOX_2 = 3441;
	private static final int CARGO_BOX_3 = 3442;
	private static final int CARGO_BOX_4 = 3443;
	private static final int GLUDIO_APPLE = 3444;
	private static final int CORN_MEAL = 3445;
	private static final int WOLF_PELTS = 3446;
	private static final int MOONSTONE = 3447;
	private static final int GLUDIO_WHEAT_FLOWER = 3448;
	private static final int SPIDERSILK_ROPE = 3449;
	private static final int ALEXANDRITE = 3450;
	private static final int SILVER_TEA = 3451;
	private static final int GOLEM_PART = 3452;
	private static final int FIRE_EMERALD = 3453;
	private static final int SILK_FROCK = 3454;
	private static final int PORCELAN_URN = 3455;
	private static final int IMPERIAL_DIAMOND = 3456;
	private static final int STATUE_SHILIEN_HEAD = 3457;
	private static final int STATUE_SHILIEN_TORSO = 3458;
	private static final int STATUE_SHILIEN_ARM = 3459;
	private static final int STATUE_SHILIEN_LEG = 3460;
	private static final int COMPLETE_STATUE = 3461;
	private static final int TABLET_FRAGMENT_1 = 3462;
	private static final int TABLET_FRAGMENT_2 = 3463;
	private static final int TABLET_FRAGMENT_3 = 3464;
	private static final int TABLET_FRAGMENT_4 = 3465;
	private static final int COMPLETE_TABLET = 3466;
	
	// Neutral items
	private static final int ADENA = 57;
	private static final int SWIFT_ATTACK_POTION = 735;
	private static final int SCROLL_OF_ESCAPE = 736;
	private static final int HEALING_POTION = 1061;
	private static final int SOULSHOT_D = 1463;
	private static final int SPIRITSHOT_D = 2510;
	
	// Tabs: Part, NpcId, ItemId, Item Chance, Box Id, Box Chance
	private static final int[][] DROPLIST =
	{
		// @formatter:off
		// Part #1 - Execution Ground
		{SOPHYA_LETTER_1, 20160, UNDEAD_ASH, 500000, CARGO_BOX_1, 90000}, // Neer Crawler
		{SOPHYA_LETTER_1, 20171, UNDEAD_ASH, 500000, CARGO_BOX_1, 60000}, // Specter
		{SOPHYA_LETTER_1, 20197, UNDEAD_ASH, 500000, CARGO_BOX_1, 70000}, // Sorrow Maiden
		{SOPHYA_LETTER_1, 20198, UNDEAD_ASH, 500000, CARGO_BOX_1, 80000}, // Neer Ghoul Berserker
		{SOPHYA_LETTER_1, 20200, UNDEAD_ASH, 500000, CARGO_BOX_1, 100000}, // Strain
		{SOPHYA_LETTER_1, 20201, UNDEAD_ASH, 500000, CARGO_BOX_1, 110000}, // Ghoul
		// Part #2 - Partisan Hideaway
		{SOPHYA_LETTER_2, 20207, BLOODY_AXE_INSIGNIA, 500000, CARGO_BOX_2, 60000}, // Ol Mahum Guerilla
		{SOPHYA_LETTER_2, 20208, BLOODY_AXE_INSIGNIA, 500000, CARGO_BOX_2, 70000}, // Ol Mahum Raider
		{SOPHYA_LETTER_2, 20209, BLOODY_AXE_INSIGNIA, 500000, CARGO_BOX_2, 80000}, // Ol Mahum Marksman
		{SOPHYA_LETTER_2, 20210, BLOODY_AXE_INSIGNIA, 500000, CARGO_BOX_2, 90000}, // Ol Mahum Sergeant
		{SOPHYA_LETTER_2, 20211, BLOODY_AXE_INSIGNIA, 500000, CARGO_BOX_2, 100000}, // Ol Mahum Captain
		// Part #3 - Near Giran Town
		{SOPHYA_LETTER_3, 20251, DELU_FANG, 500000, CARGO_BOX_3, 100000}, // Delu Lizardman
		{SOPHYA_LETTER_3, 20252, DELU_FANG, 500000, CARGO_BOX_3, 110000}, // Delu Lizardman Scout
		{SOPHYA_LETTER_3, 20253, DELU_FANG, 500000, CARGO_BOX_3, 120000}, // Delu Lizardman Warrior
		// Part #4 - Cruma Area
		{SOPHYA_LETTER_4, 20157, STAKATO_TALON, 500000, CARGO_BOX_4, 100000}, // Marsh Stakato
		{SOPHYA_LETTER_4, 20230, STAKATO_TALON, 500000, CARGO_BOX_4, 110000}, // Marsh Stakato Worker
		{SOPHYA_LETTER_4, 20232, STAKATO_TALON, 500000, CARGO_BOX_4, 120000}, // Marsh Stakato Soldier
		{SOPHYA_LETTER_4, 20234, STAKATO_TALON, 500000, CARGO_BOX_4, 130000}, // Marsh Stakato Drone
		// @formatter:on
	};
	
	public Q00333_HuntOfTheBlackLion()
	{
		super(333, "Hunt of the Black Lion");
		registerQuestItems(LION_CLAW, LION_EYE, GUILD_COIN, UNDEAD_ASH, BLOODY_AXE_INSIGNIA, DELU_FANG, STAKATO_TALON, SOPHYA_LETTER_1, SOPHYA_LETTER_2, SOPHYA_LETTER_3, SOPHYA_LETTER_4);
		addStartNpc(SOPHYA);
		addTalkId(SOPHYA, REDFOOT, RUPIO, UNDRIAS, LOCKIRIN, MORGAN);
		for (int[] i : DROPLIST)
		{
			addKillId(i[1]);
		}
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
			case "30735-04.htm":
			{
				st.startQuest();
				break;
			}
			case "30735-10.htm":
			{
				if (!hasQuestItems(player, SOPHYA_LETTER_1))
				{
					giveItems(player, SOPHYA_LETTER_1, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30735-11.htm":
			{
				if (!hasQuestItems(player, SOPHYA_LETTER_2))
				{
					giveItems(player, SOPHYA_LETTER_2, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30735-12.htm":
			{
				if (!hasQuestItems(player, SOPHYA_LETTER_3))
				{
					giveItems(player, SOPHYA_LETTER_3, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30735-13.htm":
			{
				if (!hasQuestItems(player, SOPHYA_LETTER_4))
				{
					giveItems(player, SOPHYA_LETTER_4, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30735-16.htm":
			{
				if (getQuestItemsCount(player, LION_CLAW) > 9)
				{
					takeItems(player, LION_CLAW, 10);
					
					final int eyes = getQuestItemsCount(player, LION_EYE);
					if (eyes < 5)
					{
						htmltext = "30735-17a.htm";
						
						giveItems(player, LION_EYE, 1);
						
						final int random = getRandom(100);
						if (random < 25)
						{
							giveItems(player, HEALING_POTION, 20);
						}
						else if (random < 50)
						{
							giveItems(player, player.isMageClass() ? SPIRITSHOT_D : SOULSHOT_D, player.isMageClass() ? 50 : 100);
						}
						else if (random < 75)
						{
							giveItems(player, SCROLL_OF_ESCAPE, 20);
						}
						else
						{
							giveItems(player, SWIFT_ATTACK_POTION, 3);
						}
					}
					else if (eyes < 9)
					{
						htmltext = "30735-18b.htm";
						
						giveItems(player, LION_EYE, 1);
						
						final int random = getRandom(100);
						if (random < 25)
						{
							giveItems(player, HEALING_POTION, 25);
						}
						else if (random < 50)
						{
							giveItems(player, player.isMageClass() ? SPIRITSHOT_D : SOULSHOT_D, player.isMageClass() ? 100 : 200);
						}
						else if (random < 75)
						{
							giveItems(player, SCROLL_OF_ESCAPE, 20);
						}
						else
						{
							giveItems(player, SWIFT_ATTACK_POTION, 3);
						}
					}
					else
					{
						htmltext = "30735-19b.htm";
						
						final int random = getRandom(100);
						if (random < 25)
						{
							giveItems(player, HEALING_POTION, 50);
						}
						else if (random < 50)
						{
							giveItems(player, player.isMageClass() ? SPIRITSHOT_D : SOULSHOT_D, player.isMageClass() ? 200 : 400);
						}
						else if (random < 75)
						{
							giveItems(player, SCROLL_OF_ESCAPE, 30);
						}
						else
						{
							giveItems(player, SWIFT_ATTACK_POTION, 4);
						}
					}
				}
				break;
			}
			case "30735-20.htm":
			{
				takeItems(player, SOPHYA_LETTER_1, -1);
				takeItems(player, SOPHYA_LETTER_2, -1);
				takeItems(player, SOPHYA_LETTER_3, -1);
				takeItems(player, SOPHYA_LETTER_4, -1);
				break;
			}
			case "30735-26.htm":
			{
				takeItems(player, LION_CLAW, -1);
				takeItems(player, LION_EYE, -1);
				takeItems(player, GUILD_COIN, -1);
				takeItems(player, BLACK_LION_MARK, -1);
				takeItems(player, SOPHYA_LETTER_1, -1);
				takeItems(player, SOPHYA_LETTER_2, -1);
				takeItems(player, SOPHYA_LETTER_3, -1);
				takeItems(player, SOPHYA_LETTER_4, -1);
				giveItems(player, ADENA, 12400);
				st.exitQuest(true, true);
				break;
			}
			case "30736-03.htm":
			{
				final boolean cargo1 = hasQuestItems(player, CARGO_BOX_1);
				final boolean cargo2 = hasQuestItems(player, CARGO_BOX_2);
				final boolean cargo3 = hasQuestItems(player, CARGO_BOX_3);
				final boolean cargo4 = hasQuestItems(player, CARGO_BOX_4);
				if ((cargo1 || cargo2 || cargo3 || cargo4) && (player.getAdena() > 649))
				{
					takeItems(player, ADENA, 650);
					
					if (cargo1)
					{
						takeItems(player, CARGO_BOX_1, 1);
					}
					else if (cargo2)
					{
						takeItems(player, CARGO_BOX_2, 1);
					}
					else if (cargo3)
					{
						takeItems(player, CARGO_BOX_3, 1);
					}
					else
					{
						takeItems(player, CARGO_BOX_4, 1);
					}
					
					final int i0 = getRandom(100);
					final int i1 = getRandom(100);
					if (i0 < 40)
					{
						if (i1 < 33)
						{
							htmltext = "30736-04a.htm";
							giveItems(player, GLUDIO_APPLE, 1);
						}
						else if (i1 < 66)
						{
							htmltext = "30736-04b.htm";
							giveItems(player, CORN_MEAL, 1);
						}
						else
						{
							htmltext = "30736-04c.htm";
							giveItems(player, WOLF_PELTS, 1);
						}
					}
					else if (i0 < 60)
					{
						if (i1 < 33)
						{
							htmltext = "30736-04d.htm";
							giveItems(player, MOONSTONE, 1);
						}
						else if (i1 < 66)
						{
							htmltext = "30736-04e.htm";
							giveItems(player, GLUDIO_WHEAT_FLOWER, 1);
						}
						else
						{
							htmltext = "30736-04f.htm";
							giveItems(player, SPIDERSILK_ROPE, 1);
						}
					}
					else if (i0 < 70)
					{
						if (i1 < 33)
						{
							htmltext = "30736-04g.htm";
							giveItems(player, ALEXANDRITE, 1);
						}
						else if (i1 < 66)
						{
							htmltext = "30736-04h.htm";
							giveItems(player, SILVER_TEA, 1);
						}
						else
						{
							htmltext = "30736-04i.htm";
							giveItems(player, GOLEM_PART, 1);
						}
					}
					else if (i0 < 75)
					{
						if (i1 < 33)
						{
							htmltext = "30736-04j.htm";
							giveItems(player, FIRE_EMERALD, 1);
						}
						else if (i1 < 66)
						{
							htmltext = "30736-04k.htm";
							giveItems(player, SILK_FROCK, 1);
						}
						else
						{
							htmltext = "30736-04l.htm";
							giveItems(player, PORCELAN_URN, 1);
						}
					}
					else if (i0 < 76)
					{
						htmltext = "30736-04m.htm";
						giveItems(player, IMPERIAL_DIAMOND, 1);
					}
					else if (getRandomBoolean())
					{
						htmltext = "30736-04n.htm";
						
						if (i1 < 25)
						{
							giveItems(player, STATUE_SHILIEN_HEAD, 1);
						}
						else if (i1 < 50)
						{
							giveItems(player, STATUE_SHILIEN_TORSO, 1);
						}
						else if (i1 < 75)
						{
							giveItems(player, STATUE_SHILIEN_ARM, 1);
						}
						else
						{
							giveItems(player, STATUE_SHILIEN_LEG, 1);
						}
					}
					else
					{
						htmltext = "30736-04o.htm";
						
						if (i1 < 25)
						{
							giveItems(player, TABLET_FRAGMENT_1, 1);
						}
						else if (i1 < 50)
						{
							giveItems(player, TABLET_FRAGMENT_2, 1);
						}
						else if (i1 < 75)
						{
							giveItems(player, TABLET_FRAGMENT_3, 1);
						}
						else
						{
							giveItems(player, TABLET_FRAGMENT_4, 1);
						}
					}
				}
				else
				{
					htmltext = "30736-05.htm";
				}
				break;
			}
			case "30736-07.htm":
			{
				final int state = st.getInt("state");
				if (player.getAdena() > (200 + (state * 200)))
				{
					if (state < 3)
					{
						final int i0 = getRandom(100);
						if (i0 < 5)
						{
							htmltext = "30736-08a.htm";
						}
						else if (i0 < 10)
						{
							htmltext = "30736-08b.htm";
						}
						else if (i0 < 15)
						{
							htmltext = "30736-08c.htm";
						}
						else if (i0 < 20)
						{
							htmltext = "30736-08d.htm";
						}
						else if (i0 < 25)
						{
							htmltext = "30736-08e.htm";
						}
						else if (i0 < 30)
						{
							htmltext = "30736-08f.htm";
						}
						else if (i0 < 35)
						{
							htmltext = "30736-08g.htm";
						}
						else if (i0 < 40)
						{
							htmltext = "30736-08h.htm";
						}
						else if (i0 < 45)
						{
							htmltext = "30736-08i.htm";
						}
						else if (i0 < 50)
						{
							htmltext = "30736-08j.htm";
						}
						else if (i0 < 55)
						{
							htmltext = "30736-08k.htm";
						}
						else if (i0 < 60)
						{
							htmltext = "30736-08l.htm";
						}
						else if (i0 < 65)
						{
							htmltext = "30736-08m.htm";
						}
						else if (i0 < 70)
						{
							htmltext = "30736-08n.htm";
						}
						else if (i0 < 75)
						{
							htmltext = "30736-08o.htm";
						}
						else if (i0 < 80)
						{
							htmltext = "30736-08p.htm";
						}
						else if (i0 < 85)
						{
							htmltext = "30736-08q.htm";
						}
						else if (i0 < 90)
						{
							htmltext = "30736-08r.htm";
						}
						else if (i0 < 95)
						{
							htmltext = "30736-08s.htm";
						}
						else
						{
							htmltext = "30736-08t.htm";
						}
						
						takeItems(player, ADENA, 200 + (state * 200));
						st.set("state", String.valueOf(state + 1));
					}
					else
					{
						htmltext = "30736-08.htm";
					}
				}
				break;
			}
			case "30471-03.htm":
			{
				if (hasQuestItems(player, STATUE_SHILIEN_HEAD, STATUE_SHILIEN_TORSO, STATUE_SHILIEN_ARM, STATUE_SHILIEN_LEG))
				{
					takeItems(player, STATUE_SHILIEN_HEAD, 1);
					takeItems(player, STATUE_SHILIEN_TORSO, 1);
					takeItems(player, STATUE_SHILIEN_ARM, 1);
					takeItems(player, STATUE_SHILIEN_LEG, 1);
					
					if (getRandomBoolean())
					{
						htmltext = "30471-04.htm";
						giveItems(player, COMPLETE_STATUE, 1);
					}
					else
					{
						htmltext = "30471-05.htm";
					}
				}
				break;
			}
			case "30471-06.htm":
			{
				if (hasQuestItems(player, TABLET_FRAGMENT_1, TABLET_FRAGMENT_2, TABLET_FRAGMENT_3, TABLET_FRAGMENT_4))
				{
					takeItems(player, TABLET_FRAGMENT_1, 1);
					takeItems(player, TABLET_FRAGMENT_2, 1);
					takeItems(player, TABLET_FRAGMENT_3, 1);
					takeItems(player, TABLET_FRAGMENT_4, 1);
					
					if (getRandomBoolean())
					{
						htmltext = "30471-07.htm";
						giveItems(player, COMPLETE_TABLET, 1);
					}
					else
					{
						htmltext = "30471-08.htm";
					}
				}
				break;
			}
			case "30130-04.htm":
			{
				if (hasQuestItems(player, COMPLETE_STATUE))
				{
					takeItems(player, COMPLETE_STATUE, 1);
					giveItems(player, ADENA, 30000);
				}
				break;
			}
			case "30531-04.htm":
			{
				if (hasQuestItems(player, COMPLETE_TABLET))
				{
					takeItems(player, COMPLETE_TABLET, 1);
					giveItems(player, ADENA, 30000);
				}
				break;
			}
			case "30737-06.htm":
			{
				final boolean cargo1 = hasQuestItems(player, CARGO_BOX_1);
				final boolean cargo2 = hasQuestItems(player, CARGO_BOX_2);
				final boolean cargo3 = hasQuestItems(player, CARGO_BOX_3);
				final boolean cargo4 = hasQuestItems(player, CARGO_BOX_4);
				if (cargo1 || cargo2 || cargo3 || cargo4)
				{
					if (cargo1)
					{
						takeItems(player, CARGO_BOX_1, 1);
					}
					else if (cargo2)
					{
						takeItems(player, CARGO_BOX_2, 1);
					}
					else if (cargo3)
					{
						takeItems(player, CARGO_BOX_3, 1);
					}
					else
					{
						takeItems(player, CARGO_BOX_4, 1);
					}
					
					final int coins = getQuestItemsCount(player, GUILD_COIN);
					if (coins < 40)
					{
						htmltext = "30737-03.htm";
						giveItems(player, ADENA, 100);
					}
					else if (coins < 80)
					{
						htmltext = "30737-04.htm";
						giveItems(player, ADENA, 200);
					}
					else
					{
						htmltext = "30737-05.htm";
						giveItems(player, ADENA, 300);
					}
					
					if (coins < 80)
					{
						giveItems(player, GUILD_COIN, 1);
					}
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
				if (player.getLevel() < 25)
				{
					htmltext = "30735-01.htm";
				}
				else if (!hasQuestItems(player, BLACK_LION_MARK))
				{
					htmltext = "30735-02.htm";
				}
				else
				{
					htmltext = "30735-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case SOPHYA:
					{
						if (!hasAtLeastOneQuestItem(player, SOPHYA_LETTER_1, SOPHYA_LETTER_2, SOPHYA_LETTER_3, SOPHYA_LETTER_4))
						{
							htmltext = "30735-14.htm";
						}
						else
						{
							if (!hasAtLeastOneQuestItem(player, UNDEAD_ASH, BLOODY_AXE_INSIGNIA, DELU_FANG, STAKATO_TALON))
							{
								htmltext = hasAtLeastOneQuestItem(player, CARGO_BOX_1, CARGO_BOX_2, CARGO_BOX_3, CARGO_BOX_4) ? "30735-15a.htm" : "30735-15.htm";
							}
							else
							{
								final int count = getQuestItemsCount(player, UNDEAD_ASH) + getQuestItemsCount(player, BLOODY_AXE_INSIGNIA) + getQuestItemsCount(player, DELU_FANG) + getQuestItemsCount(player, STAKATO_TALON);
								
								takeItems(player, UNDEAD_ASH, -1);
								takeItems(player, BLOODY_AXE_INSIGNIA, -1);
								takeItems(player, DELU_FANG, -1);
								takeItems(player, STAKATO_TALON, -1);
								giveItems(player, ADENA, count * 35);
								
								if ((count >= 20) && (count < 50))
								{
									giveItems(player, LION_CLAW, 1);
								}
								else if ((count >= 50) && (count < 100))
								{
									giveItems(player, LION_CLAW, 2);
								}
								else if (count >= 100)
								{
									giveItems(player, LION_CLAW, 3);
								}
								
								htmltext = hasAtLeastOneQuestItem(player, CARGO_BOX_1, CARGO_BOX_2, CARGO_BOX_3, CARGO_BOX_4) ? "30735-23.htm" : "30735-22.htm";
							}
						}
						break;
					}
					case REDFOOT:
					{
						htmltext = hasAtLeastOneQuestItem(player, CARGO_BOX_1, CARGO_BOX_2, CARGO_BOX_3, CARGO_BOX_4) ? "30736-02.htm" : "30736-01.htm";
						break;
					}
					case RUPIO:
					{
						if (hasQuestItems(player, STATUE_SHILIEN_HEAD, STATUE_SHILIEN_TORSO, STATUE_SHILIEN_ARM, STATUE_SHILIEN_LEG) || hasQuestItems(player, TABLET_FRAGMENT_1, TABLET_FRAGMENT_2, TABLET_FRAGMENT_3, TABLET_FRAGMENT_4))
						{
							htmltext = "30471-02.htm";
						}
						else
						{
							htmltext = "30471-01.htm";
						}
						break;
					}
					case UNDRIAS:
					{
						if (!hasQuestItems(player, COMPLETE_STATUE))
						{
							htmltext = hasQuestItems(player, STATUE_SHILIEN_HEAD, STATUE_SHILIEN_TORSO, STATUE_SHILIEN_ARM, STATUE_SHILIEN_LEG) ? "30130-02.htm" : "30130-01.htm";
						}
						else
						{
							htmltext = "30130-03.htm";
						}
						break;
					}
					case LOCKIRIN:
					{
						if (!hasQuestItems(player, COMPLETE_TABLET))
						{
							htmltext = hasQuestItems(player, TABLET_FRAGMENT_1, TABLET_FRAGMENT_2, TABLET_FRAGMENT_3, TABLET_FRAGMENT_4) ? "30531-02.htm" : "30531-01.htm";
						}
						else
						{
							htmltext = "30531-03.htm";
						}
						break;
					}
					case MORGAN:
					{
						htmltext = hasAtLeastOneQuestItem(player, CARGO_BOX_1, CARGO_BOX_2, CARGO_BOX_3, CARGO_BOX_4) ? "30737-02.htm" : "30737-01.htm";
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
		
		for (int[] info : DROPLIST)
		{
			if (hasQuestItems(player, info[0]) && (npc.getId() == info[1]))
			{
				boolean playSound = false;
				if (getRandom(1000000) < info[3])
				{
					giveItems(player, info[2], 1);
					playSound = true;
				}
				
				if (getRandom(1000000) < info[5])
				{
					giveItems(player, info[4], 1);
					playSound = true;
				}
				
				if (playSound)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
