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
package quests.Q00222_TestOfTheDuelist;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00222_TestOfTheDuelist extends Quest
{
	// NPC
	private static final int KAIEN = 30623;
	
	// Monsters
	private static final int PUNCHER = 20085;
	private static final int NOBLE_ANT_LEADER = 20090;
	private static final int MARSH_STAKATO_DRONE = 20234;
	private static final int DEAD_SEEKER = 20202;
	private static final int BREKA_ORC_OVERLORD = 20270;
	private static final int FETTERED_SOUL = 20552;
	private static final int LETO_LIZARDMAN_OVERLORD = 20582;
	private static final int ENCHANTED_MONSTEREYE = 20564;
	private static final int TAMLIN_ORC = 20601;
	private static final int TAMLIN_ORC_ARCHER = 20602;
	private static final int EXCURO = 20214;
	private static final int KRATOR = 20217;
	private static final int GRANDIS = 20554;
	private static final int TIMAK_ORC_OVERLORD = 20588;
	private static final int LAKIN = 20604;
	
	// Items
	private static final int ORDER_GLUDIO = 2763;
	private static final int ORDER_DION = 2764;
	private static final int ORDER_GIRAN = 2765;
	private static final int ORDER_OREN = 2766;
	private static final int ORDER_ADEN = 2767;
	private static final int PUNCHER_SHARD = 2768;
	private static final int NOBLE_ANT_FEELER = 2769;
	private static final int DRONE_CHITIN = 2770;
	private static final int DEAD_SEEKER_FANG = 2771;
	private static final int OVERLORD_NECKLACE = 2772;
	private static final int FETTERED_SOUL_CHAIN = 2773;
	private static final int CHIEF_AMULET = 2774;
	private static final int ENCHANTED_EYE_MEAT = 2775;
	private static final int TAMRIN_ORC_RING = 2776;
	private static final int TAMRIN_ORC_ARROW = 2777;
	private static final int FINAL_ORDER = 2778;
	private static final int EXCURO_SKIN = 2779;
	private static final int KRATOR_SHARD = 2780;
	private static final int GRANDIS_SKIN = 2781;
	private static final int TIMAK_ORC_BELT = 2782;
	private static final int LAKIN_MACE = 2783;
	
	// Rewards
	private static final int MARK_OF_DUELIST = 2762;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00222_TestOfTheDuelist()
	{
		super(222, "Test of the Duelist");
		registerQuestItems(ORDER_GLUDIO, ORDER_DION, ORDER_GIRAN, ORDER_OREN, ORDER_ADEN, FINAL_ORDER, PUNCHER_SHARD, NOBLE_ANT_FEELER, DRONE_CHITIN, DEAD_SEEKER_FANG, OVERLORD_NECKLACE, FETTERED_SOUL_CHAIN, CHIEF_AMULET, ENCHANTED_EYE_MEAT, TAMRIN_ORC_RING, TAMRIN_ORC_ARROW, EXCURO_SKIN, KRATOR_SHARD, GRANDIS_SKIN, TIMAK_ORC_BELT, LAKIN_MACE);
		addStartNpc(KAIEN);
		addTalkId(KAIEN);
		addKillId(PUNCHER, NOBLE_ANT_LEADER, MARSH_STAKATO_DRONE, DEAD_SEEKER, BREKA_ORC_OVERLORD, FETTERED_SOUL, LETO_LIZARDMAN_OVERLORD, ENCHANTED_MONSTEREYE, TAMLIN_ORC, TAMLIN_ORC_ARCHER, EXCURO, KRATOR, GRANDIS, TIMAK_ORC_OVERLORD, LAKIN);
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
			case "30623-04.htm":
			{
				if (player.getRace() == Race.ORC)
				{
					htmltext = "30623-05.htm";
				}
				break;
			}
			case "30623-07.htm":
			{
				st.startQuest();
				st.setCond(2);
				giveItems(player, ORDER_GLUDIO, 1);
				giveItems(player, ORDER_DION, 1);
				giveItems(player, ORDER_GIRAN, 1);
				giveItems(player, ORDER_OREN, 1);
				giveItems(player, ORDER_ADEN, 1);
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30623-07a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30623-16.htm":
			{
				if (st.isCond(3))
				{
					st.setCond(4, true);
					
					takeItems(player, ORDER_GLUDIO, 1);
					takeItems(player, ORDER_DION, 1);
					takeItems(player, ORDER_GIRAN, 1);
					takeItems(player, ORDER_OREN, 1);
					takeItems(player, ORDER_ADEN, 1);
					
					takeItems(player, PUNCHER_SHARD, -1);
					takeItems(player, NOBLE_ANT_FEELER, -1);
					takeItems(player, DRONE_CHITIN, -1);
					takeItems(player, DEAD_SEEKER_FANG, -1);
					takeItems(player, OVERLORD_NECKLACE, -1);
					takeItems(player, FETTERED_SOUL_CHAIN, -1);
					takeItems(player, CHIEF_AMULET, -1);
					takeItems(player, ENCHANTED_EYE_MEAT, -1);
					takeItems(player, TAMRIN_ORC_RING, -1);
					takeItems(player, TAMRIN_ORC_ARROW, -1);
					
					giveItems(player, FINAL_ORDER, 1);
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
				final int classId = player.getPlayerClass().getId();
				if ((classId != 0x01) && (classId != 0x2f) && (classId != 0x13) && (classId != 0x20))
				{
					htmltext = "30623-02.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30623-01.htm";
				}
				else
				{
					htmltext = "30623-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 2)
				{
					htmltext = "30623-07a.htm";
				}
				else if (cond == 3)
				{
					htmltext = "30623-13.htm";
				}
				else if (cond == 4)
				{
					htmltext = "30623-17.htm";
				}
				else if (cond == 5)
				{
					htmltext = "30623-18.htm";
					takeItems(player, FINAL_ORDER, 1);
					takeItems(player, EXCURO_SKIN, -1);
					takeItems(player, KRATOR_SHARD, -1);
					takeItems(player, GRANDIS_SKIN, -1);
					takeItems(player, TIMAK_ORC_BELT, -1);
					takeItems(player, LAKIN_MACE, -1);
					giveItems(player, MARK_OF_DUELIST, 1);
					addExpAndSp(player, 47015, 20000);
					player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
					st.exitQuest(false, true);
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
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		if (st.isCond(2))
		{
			switch (npc.getId())
			{
				case PUNCHER:
				{
					if (getQuestItemsCount(player, PUNCHER_SHARD) < 10)
					{
						giveItems(player, PUNCHER_SHARD, 1);
						if ((getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case NOBLE_ANT_LEADER:
				{
					if (getQuestItemsCount(player, NOBLE_ANT_FEELER) < 10)
					{
						giveItems(player, NOBLE_ANT_FEELER, 1);
						if ((getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case MARSH_STAKATO_DRONE:
				{
					if (getQuestItemsCount(player, DRONE_CHITIN) < 10)
					{
						giveItems(player, DRONE_CHITIN, 1);
						if ((getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case DEAD_SEEKER:
				{
					if (getQuestItemsCount(player, DEAD_SEEKER_FANG) < 10)
					{
						giveItems(player, DEAD_SEEKER_FANG, 1);
						if ((getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case BREKA_ORC_OVERLORD:
				{
					if (getQuestItemsCount(player, OVERLORD_NECKLACE) < 10)
					{
						giveItems(player, OVERLORD_NECKLACE, 1);
						if ((getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case FETTERED_SOUL:
				{
					if (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) < 10)
					{
						giveItems(player, FETTERED_SOUL_CHAIN, 1);
						if ((getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case LETO_LIZARDMAN_OVERLORD:
				{
					if (getQuestItemsCount(player, CHIEF_AMULET) < 10)
					{
						giveItems(player, CHIEF_AMULET, 1);
						if ((getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case ENCHANTED_MONSTEREYE:
				{
					if (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) < 10)
					{
						giveItems(player, ENCHANTED_EYE_MEAT, 1);
						if ((getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case TAMLIN_ORC:
				{
					if (getQuestItemsCount(player, TAMRIN_ORC_RING) < 10)
					{
						giveItems(player, TAMRIN_ORC_RING, 1);
						if ((getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10))
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
				case TAMLIN_ORC_ARCHER:
				{
					if (getQuestItemsCount(player, TAMRIN_ORC_ARROW) < 10)
					{
						giveItems(player, TAMRIN_ORC_ARROW, 1);
						if ((getQuestItemsCount(player, TAMRIN_ORC_ARROW) >= 10) && (getQuestItemsCount(player, PUNCHER_SHARD) >= 10) && (getQuestItemsCount(player, NOBLE_ANT_FEELER) >= 10) && (getQuestItemsCount(player, DRONE_CHITIN) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_FANG) >= 10) && (getQuestItemsCount(player, OVERLORD_NECKLACE) >= 10) && (getQuestItemsCount(player, FETTERED_SOUL_CHAIN) >= 10) && (getQuestItemsCount(player, CHIEF_AMULET) >= 10) && (getQuestItemsCount(player, ENCHANTED_EYE_MEAT) >= 10) && (getQuestItemsCount(player, TAMRIN_ORC_RING) >= 10))
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
			}
		}
		else if (st.isCond(4))
		{
			switch (npc.getId())
			{
				case EXCURO:
				{
					if (getQuestItemsCount(player, EXCURO_SKIN) < 3)
					{
						giveItems(player, EXCURO_SKIN, 1);
						if ((getQuestItemsCount(player, EXCURO_SKIN) >= 3) && (getQuestItemsCount(player, KRATOR_SHARD) >= 3) && (getQuestItemsCount(player, LAKIN_MACE) >= 3) && (getQuestItemsCount(player, GRANDIS_SKIN) >= 3) && (getQuestItemsCount(player, TIMAK_ORC_BELT) >= 3))
						{
							st.setCond(5, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case KRATOR:
				{
					if (getQuestItemsCount(player, KRATOR_SHARD) < 3)
					{
						giveItems(player, KRATOR_SHARD, 1);
						if ((getQuestItemsCount(player, KRATOR_SHARD) >= 3) && (getQuestItemsCount(player, EXCURO_SKIN) >= 3) && (getQuestItemsCount(player, LAKIN_MACE) >= 3) && (getQuestItemsCount(player, GRANDIS_SKIN) >= 3) && (getQuestItemsCount(player, TIMAK_ORC_BELT) >= 3))
						{
							st.setCond(5, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case LAKIN:
				{
					if (getQuestItemsCount(player, LAKIN_MACE) < 3)
					{
						giveItems(player, LAKIN_MACE, 1);
						if ((getQuestItemsCount(player, LAKIN_MACE) >= 3) && (getQuestItemsCount(player, EXCURO_SKIN) >= 3) && (getQuestItemsCount(player, KRATOR_SHARD) >= 3) && (getQuestItemsCount(player, GRANDIS_SKIN) >= 3) && (getQuestItemsCount(player, TIMAK_ORC_BELT) >= 3))
						{
							st.setCond(5, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case GRANDIS:
				{
					if (getQuestItemsCount(player, GRANDIS_SKIN) < 3)
					{
						giveItems(player, GRANDIS_SKIN, 1);
						if ((getQuestItemsCount(player, GRANDIS_SKIN) >= 3) && (getQuestItemsCount(player, EXCURO_SKIN) >= 3) && (getQuestItemsCount(player, KRATOR_SHARD) >= 3) && (getQuestItemsCount(player, LAKIN_MACE) >= 3) && (getQuestItemsCount(player, TIMAK_ORC_BELT) >= 3))
						{
							st.setCond(5, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case TIMAK_ORC_OVERLORD:
				{
					if (getQuestItemsCount(player, TIMAK_ORC_BELT) < 3)
					{
						giveItems(player, TIMAK_ORC_BELT, 1);
						if ((getQuestItemsCount(player, TIMAK_ORC_BELT) >= 3) && (getQuestItemsCount(player, EXCURO_SKIN) >= 3) && (getQuestItemsCount(player, KRATOR_SHARD) >= 3) && (getQuestItemsCount(player, LAKIN_MACE) >= 3) && (getQuestItemsCount(player, GRANDIS_SKIN) >= 3))
						{
							st.setCond(5, true);
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
}
