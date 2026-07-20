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
package quests.Q00223_TestOfTheChampion;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00223_TestOfTheChampion extends Quest
{
	// NPCs
	private static final int ASCALON = 30624;
	private static final int GROOT = 30093;
	private static final int MOUEN = 30196;
	private static final int MASON = 30625;
	
	// Monsters
	private static final int HARPY = 20145;
	private static final int HARPY_MATRIARCH = 27088;
	private static final int MEDUSA = 20158;
	private static final int WINDSUS = 20553;
	private static final int ROAD_COLLECTOR = 27089;
	private static final int ROAD_SCAVENGER = 20551;
	private static final int LETO_LIZARDMAN = 20577;
	private static final int LETO_LIZARDMAN_ARCHER = 20578;
	private static final int LETO_LIZARDMAN_SOLDIER = 20579;
	private static final int LETO_LIZARDMAN_WARRIOR = 20580;
	private static final int LETO_LIZARDMAN_SHAMAN = 20581;
	private static final int LETO_LIZARDMAN_OVERLORD = 20582;
	private static final int BLOODY_AXE_ELITE = 20780;
	
	// Items
	private static final int ASCALON_LETTER_1 = 3277;
	private static final int MASON_LETTER = 3278;
	private static final int IRON_ROSE_RING = 3279;
	private static final int ASCALON_LETTER_2 = 3280;
	private static final int WHITE_ROSE_INSIGNIA = 3281;
	private static final int GROOT_LETTER = 3282;
	private static final int ASCALON_LETTER_3 = 3283;
	private static final int MOUEN_ORDER_1 = 3284;
	private static final int MOUEN_ORDER_2 = 3285;
	private static final int MOUEN_LETTER = 3286;
	private static final int HARPY_EGG = 3287;
	private static final int MEDUSA_VENOM = 3288;
	private static final int WINDSUS_BILE = 3289;
	private static final int BLOODY_AXE_HEAD = 3290;
	private static final int ROAD_RATMAN_HEAD = 3291;
	private static final int LETO_LIZARDMAN_FANG = 3292;
	
	// Rewards
	private static final int MARK_OF_CHAMPION = 3276;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00223_TestOfTheChampion()
	{
		super(223, "Test of the Champion");
		registerQuestItems(MASON_LETTER, MEDUSA_VENOM, WINDSUS_BILE, WHITE_ROSE_INSIGNIA, HARPY_EGG, GROOT_LETTER, MOUEN_LETTER, ASCALON_LETTER_1, IRON_ROSE_RING, BLOODY_AXE_HEAD, ASCALON_LETTER_2, ASCALON_LETTER_3, MOUEN_ORDER_1, ROAD_RATMAN_HEAD, MOUEN_ORDER_2, LETO_LIZARDMAN_FANG);
		addStartNpc(ASCALON);
		addTalkId(ASCALON, GROOT, MOUEN, MASON);
		addAttackId(HARPY, ROAD_SCAVENGER);
		addKillId(HARPY, MEDUSA, HARPY_MATRIARCH, ROAD_COLLECTOR, ROAD_SCAVENGER, WINDSUS, LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER, LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD, BLOODY_AXE_ELITE);
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
			case "30624-06.htm":
			{
				st.startQuest();
				giveItems(player, ASCALON_LETTER_1, 1);
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30624-06a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30624-10.htm":
			{
				st.setCond(5, true);
				takeItems(player, MASON_LETTER, 1);
				giveItems(player, ASCALON_LETTER_2, 1);
				break;
			}
			case "30624-14.htm":
			{
				st.setCond(9, true);
				takeItems(player, GROOT_LETTER, 1);
				giveItems(player, ASCALON_LETTER_3, 1);
				break;
			}
			case "30625-03.htm":
			{
				st.setCond(2, true);
				takeItems(player, ASCALON_LETTER_1, 1);
				giveItems(player, IRON_ROSE_RING, 1);
				break;
			}
			case "30093-02.htm":
			{
				st.setCond(6, true);
				takeItems(player, ASCALON_LETTER_2, 1);
				giveItems(player, WHITE_ROSE_INSIGNIA, 1);
				break;
			}
			case "30196-03.htm":
			{
				st.setCond(10, true);
				takeItems(player, ASCALON_LETTER_3, 1);
				giveItems(player, MOUEN_ORDER_1, 1);
				break;
			}
			case "30196-06.htm":
			{
				st.setCond(12, true);
				takeItems(player, MOUEN_ORDER_1, 1);
				takeItems(player, ROAD_RATMAN_HEAD, 1);
				giveItems(player, MOUEN_ORDER_2, 1);
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
				final PlayerClass playerClass = player.getPlayerClass();
				if ((playerClass != PlayerClass.WARRIOR) && (playerClass != PlayerClass.ORC_RAIDER))
				{
					htmltext = "30624-01.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30624-02.htm";
				}
				else
				{
					htmltext = (playerClass == PlayerClass.WARRIOR) ? "30624-03.htm" : "30624-04.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ASCALON:
					{
						if (cond == 1)
						{
							htmltext = "30624-07.htm";
						}
						else if (cond < 4)
						{
							htmltext = "30624-08.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30624-09.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30624-11.htm";
						}
						else if ((cond > 5) && (cond < 8))
						{
							htmltext = "30624-12.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30624-13.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30624-15.htm";
						}
						else if ((cond > 9) && (cond < 14))
						{
							htmltext = "30624-16.htm";
						}
						else if (cond == 14)
						{
							htmltext = "30624-17.htm";
							takeItems(player, MOUEN_LETTER, 1);
							giveItems(player, MARK_OF_CHAMPION, 1);
							addExpAndSp(player, 117454, 25000);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case MASON:
					{
						if (cond == 1)
						{
							htmltext = "30625-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30625-04.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30625-05.htm";
							st.setCond(4, true);
							takeItems(player, BLOODY_AXE_HEAD, -1);
							takeItems(player, IRON_ROSE_RING, 1);
							giveItems(player, MASON_LETTER, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30625-06.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30625-07.htm";
						}
						break;
					}
					case GROOT:
					{
						if (cond == 5)
						{
							htmltext = "30093-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30093-03.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30093-04.htm";
							st.setCond(8, true);
							takeItems(player, WHITE_ROSE_INSIGNIA, 1);
							takeItems(player, HARPY_EGG, -1);
							takeItems(player, MEDUSA_VENOM, -1);
							takeItems(player, WINDSUS_BILE, -1);
							giveItems(player, GROOT_LETTER, 1);
						}
						else if (cond == 8)
						{
							htmltext = "30093-05.htm";
						}
						else if (cond > 8)
						{
							htmltext = "30093-06.htm";
						}
						break;
					}
					case MOUEN:
					{
						if (cond == 9)
						{
							htmltext = "30196-01.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30196-04.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30196-05.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30196-07.htm";
						}
						else if (cond == 13)
						{
							htmltext = "30196-08.htm";
							st.setCond(14, true);
							takeItems(player, LETO_LIZARDMAN_FANG, -1);
							takeItems(player, MOUEN_ORDER_2, 1);
							giveItems(player, MOUEN_LETTER, 1);
						}
						else if (cond > 13)
						{
							htmltext = "30196-09.htm";
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
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		final QuestState st = getQuestState(attacker, false);
		if (st == null)
		{
			return;
		}
		
		switch (npc.getId())
		{
			case HARPY: // Possibility to spawn an HARPY _MATRIARCH.
			{
				if (st.isCond(6) && getRandomBoolean() && !npc.isScriptValue(1))
				{
					final Creature originalKiller = isPet ? attacker.getSummon() : attacker;
					
					// Spawn one or two matriarchs.
					for (int i = 1; i < ((getRandom(10) < 7) ? 2 : 3); i++)
					{
						final Attackable collector = addSpawn(HARPY_MATRIARCH, npc, true, 0).asAttackable();
						
						collector.setRunning();
						collector.addDamageHate(originalKiller, 0, 999);
						collector.getAI().setIntention(Intention.ATTACK, originalKiller);
					}
					
					npc.setScriptValue(1);
				}
				break;
			}
			case ROAD_SCAVENGER: // Possibility to spawn a Road Collector.
			{
				if (st.isCond(10) && getRandomBoolean() && !npc.isScriptValue(1))
				{
					final Creature originalKiller = isPet ? attacker.getSummon() : attacker;
					
					// Spawn one or two collectors.
					for (int i = 1; i < ((getRandom(10) < 7) ? 2 : 3); i++)
					{
						final Attackable collector = addSpawn(ROAD_COLLECTOR, npc, true, 0).asAttackable();
						collector.setRunning();
						collector.addDamageHate(originalKiller, 0, 999);
						collector.getAI().setIntention(Intention.ATTACK, originalKiller);
					}
					
					npc.setScriptValue(1);
				}
				break;
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		final int npcId = npc.getId();
		switch (npcId)
		{
			case BLOODY_AXE_ELITE:
			{
				if (st.isCond(2))
				{
					giveItems(player, BLOODY_AXE_HEAD, 1);
					if (getQuestItemsCount(player, BLOODY_AXE_HEAD) >= 100)
					{
						st.setCond(3, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case HARPY:
			case HARPY_MATRIARCH:
			{
				if (st.isCond(6) && getRandomBoolean() && (getQuestItemsCount(player, HARPY_EGG) < 30))
				{
					giveItems(player, HARPY_EGG, 1);
					if ((getQuestItemsCount(player, HARPY_EGG) >= 30) && (getQuestItemsCount(player, MEDUSA_VENOM) == 30) && (getQuestItemsCount(player, WINDSUS_BILE) == 30))
					{
						st.setCond(7, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case MEDUSA:
			{
				if (st.isCond(6) && getRandomBoolean() && (getQuestItemsCount(player, MEDUSA_VENOM) < 30))
				{
					giveItems(player, MEDUSA_VENOM, 1);
					if ((getQuestItemsCount(player, MEDUSA_VENOM) >= 30) && (getQuestItemsCount(player, HARPY_EGG) == 30) && (getQuestItemsCount(player, WINDSUS_BILE) == 30))
					{
						st.setCond(7, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case WINDSUS:
			{
				if (st.isCond(6) && getRandomBoolean() && (getQuestItemsCount(player, WINDSUS_BILE) < 30))
				{
					giveItems(player, WINDSUS_BILE, 1);
					if ((getQuestItemsCount(player, WINDSUS_BILE) >= 30) && (getQuestItemsCount(player, HARPY_EGG) == 30) && (getQuestItemsCount(player, MEDUSA_VENOM) == 30))
					{
						st.setCond(7, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case ROAD_COLLECTOR:
			case ROAD_SCAVENGER:
			{
				if (st.isCond(10))
				{
					giveItems(player, ROAD_RATMAN_HEAD, 1);
					if (getQuestItemsCount(player, ROAD_RATMAN_HEAD) >= 100)
					{
						st.setCond(11, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case LETO_LIZARDMAN:
			case LETO_LIZARDMAN_ARCHER:
			case LETO_LIZARDMAN_SOLDIER:
			case LETO_LIZARDMAN_WARRIOR:
			case LETO_LIZARDMAN_SHAMAN:
			case LETO_LIZARDMAN_OVERLORD:
			{
				if (st.isCond(12) && (getQuestItemsCount(player, LETO_LIZARDMAN_FANG) < 100) && (getRandom(1000000) < (500000 + (100000 * (npcId - 20577)))))
				{
					giveItems(player, ROAD_RATMAN_HEAD, 1);
					if (getQuestItemsCount(player, LETO_LIZARDMAN_FANG) >= 100)
					{
						st.setCond(13, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
		}
	}
}
