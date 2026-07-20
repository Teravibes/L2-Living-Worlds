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
package quests.Q00417_PathOfTheScavenger;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00417_PathOfTheScavenger extends Quest
{
	// NPCs
	private static final int RAUT = 30316;
	private static final int SHARI = 30517;
	private static final int MION = 30519;
	private static final int PIPPI = 30524;
	private static final int BRONK = 30525;
	private static final int ZIMENF = 30538;
	private static final int TOMA = 30556;
	private static final int TORAI = 30557;
	private static final int YASHENI = 31958;
	
	// Monsters
	private static final int HUNTER_TARANTULA = 20403;
	private static final int PLUNDER_TARANTULA = 20508;
	private static final int HUNTER_BEAR = 20777;
	private static final int HONEY_BEAR = 27058;
	
	// Items
	private static final int RING_OF_RAVEN = 1642;
	private static final int PIPPI_LETTER = 1643;
	private static final int RAUT_TELEPORT_SCROLL = 1644;
	private static final int SUCCUBUS_UNDIES = 1645;
	private static final int MION_LETTER = 1646;
	private static final int BRONK_INGOT = 1647;
	private static final int SHARI_AXE = 1648;
	private static final int ZIMENF_POTION = 1649;
	private static final int BRONK_PAY = 1650;
	private static final int SHARI_PAY = 1651;
	private static final int ZIMENF_PAY = 1652;
	private static final int BEAR_PICTURE = 1653;
	private static final int TARANTULA_PICTURE = 1654;
	private static final int HONEY_JAR = 1655;
	private static final int BEAD = 1656;
	private static final int BEAD_PARCEL_1 = 1657;
	private static final int BEAD_PARCEL_2 = 8543;
	
	public Q00417_PathOfTheScavenger()
	{
		super(417, "Path to become a Scavenger");
		registerQuestItems(PIPPI_LETTER, RAUT_TELEPORT_SCROLL, SUCCUBUS_UNDIES, MION_LETTER, BRONK_INGOT, SHARI_AXE, ZIMENF_POTION, BRONK_PAY, SHARI_PAY, ZIMENF_PAY, BEAR_PICTURE, TARANTULA_PICTURE, HONEY_JAR, BEAD, BEAD_PARCEL_1, BEAD_PARCEL_2);
		addStartNpc(PIPPI);
		addTalkId(RAUT, SHARI, MION, PIPPI, BRONK, ZIMENF, TOMA, TORAI, YASHENI);
		addKillId(HUNTER_TARANTULA, PLUNDER_TARANTULA, HUNTER_BEAR, HONEY_BEAR);
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
			case "30524-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.DWARVEN_FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.SCAVENGER) ? "30524-02a.htm" : "30524-08.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30524-02.htm";
				}
				else if (hasQuestItems(player, RING_OF_RAVEN))
				{
					htmltext = "30524-04.htm";
				}
				else
				{
					st.startQuest();
					giveItems(player, PIPPI_LETTER, 1);
				}
				break;
			}
			case "30519_1":
			{
				final int random = getRandom(3);
				htmltext = "30519-0" + (random + 2) + ".htm";
				st.setCond(2, true);
				takeItems(player, PIPPI_LETTER, -1);
				giveItems(player, ZIMENF_POTION - random, 1);
				break;
			}
			case "30519_2":
			{
				final int random = getRandom(3);
				htmltext = "30519-0" + (random + 2) + ".htm";
				takeItems(player, BRONK_PAY, -1);
				takeItems(player, SHARI_PAY, -1);
				takeItems(player, ZIMENF_PAY, -1);
				giveItems(player, ZIMENF_POTION - random, 1);
				break;
			}
			case "30519-07.htm":
			{
				st.set("id", String.valueOf(st.getInt("id") + 1));
				break;
			}
			case "30519-09.htm":
			{
				final int id = st.getInt("id");
				if ((id / 10) < 2)
				{
					htmltext = "30519-07.htm";
					st.set("id", String.valueOf(id + 1));
				}
				else if ((id / 10) == 2)
				{
					st.set("id", String.valueOf(id + 1));
				}
				else if ((id / 10) >= 3)
				{
					htmltext = "30519-10.htm";
					st.setCond(4, true);
					takeItems(player, SHARI_AXE, -1);
					takeItems(player, ZIMENF_POTION, -1);
					takeItems(player, BRONK_INGOT, -1);
					giveItems(player, MION_LETTER, 1);
				}
				break;
			}
			case "30519-11.htm":
			{
				if (getRandomBoolean())
				{
					htmltext = "30519-06.htm";
				}
				break;
			}
			case "30556-05b.htm":
			{
				st.setCond(9, true);
				takeItems(player, BEAD, -1);
				takeItems(player, TARANTULA_PICTURE, 1);
				giveItems(player, BEAD_PARCEL_1, 1);
				break;
			}
			case "30556-06b.htm":
			{
				st.setCond(12, true);
				takeItems(player, BEAD, -1);
				takeItems(player, TARANTULA_PICTURE, 1);
				giveItems(player, BEAD_PARCEL_2, 1);
				break;
			}
			case "30316-02.htm":
			case "30316-03.htm":
			{
				st.setCond(10, true);
				takeItems(player, BEAD_PARCEL_1, 1);
				giveItems(player, RAUT_TELEPORT_SCROLL, 1);
				break;
			}
			case "30557-03.htm":
			{
				st.setCond(11, true);
				takeItems(player, RAUT_TELEPORT_SCROLL, 1);
				giveItems(player, SUCCUBUS_UNDIES, 1);
				break;
			}
			case "31958-02.htm":
			{
				takeItems(player, BEAD_PARCEL_2, 1);
				giveItems(player, RING_OF_RAVEN, 1);
				addExpAndSp(player, 3200, 7080);
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
				htmltext = "30524-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case PIPPI:
					{
						if (cond == 1)
						{
							htmltext = "30524-06.htm";
						}
						else if (cond > 1)
						{
							htmltext = "30524-07.htm";
						}
						break;
					}
					case MION:
					{
						if (hasQuestItems(player, PIPPI_LETTER))
						{
							htmltext = "30519-01.htm";
						}
						else if (hasAtLeastOneQuestItem(player, BRONK_INGOT, SHARI_AXE, ZIMENF_POTION))
						{
							final int id = st.getInt("id");
							if ((id / 10) == 0)
							{
								htmltext = "30519-05.htm";
							}
							else
							{
								htmltext = "30519-08.htm";
							}
						}
						else if (hasAtLeastOneQuestItem(player, BRONK_PAY, SHARI_PAY, ZIMENF_PAY))
						{
							final int id = st.getInt("id");
							if (id < 50)
							{
								htmltext = "30519-12.htm";
							}
							else
							{
								htmltext = "30519-15.htm";
								st.setCond(4, true);
								takeItems(player, BRONK_PAY, -1);
								takeItems(player, SHARI_PAY, -1);
								takeItems(player, ZIMENF_PAY, -1);
								giveItems(player, MION_LETTER, 1);
							}
						}
						else if (cond == 4)
						{
							htmltext = "30519-13.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30519-14.htm";
						}
						break;
					}
					case SHARI:
					{
						if (hasQuestItems(player, SHARI_AXE))
						{
							final int id = st.getInt("id");
							if (id < 20)
							{
								htmltext = "30517-01.htm";
							}
							else
							{
								htmltext = "30517-02.htm";
								st.setCond(3, true);
							}
							
							st.set("id", String.valueOf(id + 10));
							takeItems(player, SHARI_AXE, 1);
							giveItems(player, SHARI_PAY, 1);
						}
						else if (hasQuestItems(player, SHARI_PAY))
						{
							htmltext = "30517-03.htm";
						}
						break;
					}
					case BRONK:
					{
						if (hasQuestItems(player, BRONK_INGOT))
						{
							final int id = st.getInt("id");
							if (id < 20)
							{
								htmltext = "30525-01.htm";
							}
							else
							{
								htmltext = "30525-02.htm";
								st.setCond(3, true);
							}
							
							st.set("id", String.valueOf(id + 10));
							takeItems(player, BRONK_INGOT, 1);
							giveItems(player, BRONK_PAY, 1);
						}
						else if (hasQuestItems(player, BRONK_PAY))
						{
							htmltext = "30525-03.htm";
						}
						break;
					}
					case ZIMENF:
					{
						if (hasQuestItems(player, ZIMENF_POTION))
						{
							final int id = st.getInt("id");
							if (id < 20)
							{
								htmltext = "30538-01.htm";
							}
							else
							{
								htmltext = "30538-02.htm";
								st.setCond(3, true);
							}
							
							st.set("id", String.valueOf(id + 10));
							takeItems(player, ZIMENF_POTION, 1);
							giveItems(player, ZIMENF_PAY, 1);
						}
						else if (hasQuestItems(player, ZIMENF_PAY))
						{
							htmltext = "30538-03.htm";
						}
						break;
					}
					case TOMA:
					{
						if (cond == 4)
						{
							htmltext = "30556-01.htm";
							st.setCond(5, true);
							takeItems(player, MION_LETTER, 1);
							giveItems(player, BEAR_PICTURE, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30556-02.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30556-03.htm";
							st.setCond(7, true);
							takeItems(player, HONEY_JAR, -1);
							takeItems(player, BEAR_PICTURE, 1);
							giveItems(player, TARANTULA_PICTURE, 1);
						}
						else if (cond == 7)
						{
							htmltext = "30556-04.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30556-05a.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30556-06a.htm";
						}
						else if ((cond == 10) || (cond == 11))
						{
							htmltext = "30556-07.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30556-06c.htm";
						}
						break;
					}
					case RAUT:
					{
						if (cond == 9)
						{
							htmltext = "30316-01.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30316-04.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30316-05.htm";
							takeItems(player, SUCCUBUS_UNDIES, 1);
							giveItems(player, RING_OF_RAVEN, 1);
							addExpAndSp(player, 3200, 7080);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case TORAI:
					{
						if (cond == 10)
						{
							htmltext = "30557-01.htm";
						}
						break;
					}
					case YASHENI:
					{
						if (cond == 12)
						{
							htmltext = "31958-01.htm";
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
			case HUNTER_BEAR:
			{
				if (st.isCond(5))
				{
					final int step = st.getInt("step");
					if (step > 20)
					{
						if (((step - 20) * 10) >= getRandom(100))
						{
							addSpawn(HONEY_BEAR, npc, false, 300000);
							st.unset("step");
						}
						else
						{
							st.set("step", String.valueOf(step + 1));
						}
					}
					else
					{
						st.set("step", String.valueOf(step + 1));
					}
				}
				break;
			}
			case HONEY_BEAR:
			{
				if (st.isCond(5) && (npc.asAttackable().getSpoilerObjectId() == player.getObjectId()))
				{
					if (getQuestItemsCount(player, HONEY_JAR) < 5)
					{
						giveItems(player, HONEY_JAR, 1);
						
						if (getQuestItemsCount(player, HONEY_JAR) >= 5)
						{
							st.setCond(6, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case HUNTER_TARANTULA:
			case PLUNDER_TARANTULA:
			{
				if (st.isCond(7) && (npc.asAttackable().getSpoilerObjectId() == player.getObjectId()) && (getRandom(100) < (npc.getId() == HUNTER_TARANTULA ? 33 : 60)))
				{
					if (getQuestItemsCount(player, BEAD) < 20)
					{
						giveItems(player, BEAD, 1);
						
						if (getQuestItemsCount(player, BEAD) >= 20)
						{
							st.setCond(8, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
		}
	}
}
