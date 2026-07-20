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
package quests.Q00219_TestimonyOfFate;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00219_TestimonyOfFate extends Quest
{
	// NPCs
	private static final int KAIRA = 30476;
	private static final int METHEUS = 30614;
	private static final int IXIA = 30463;
	private static final int ALDER_SPIRIT = 30613;
	private static final int ROA = 30114;
	private static final int NORMAN = 30210;
	private static final int THIFIELL = 30358;
	private static final int ARKENIA = 30419;
	private static final int BLOODY_PIXY = 31845;
	private static final int BLIGHT_TREANT = 31850;
	
	// Monsters
	private static final int HANGMAN_TREE = 20144;
	private static final int MARSH_STAKATO = 20157;
	private static final int MEDUSA = 20158;
	private static final int TYRANT = 20192;
	private static final int TYRANT_KINGPIN = 20193;
	private static final int DEAD_SEEKER = 20202;
	private static final int MARSH_STAKATO_WORKER = 20230;
	private static final int MARSH_STAKATO_SOLDIER = 20232;
	private static final int MARSH_SPIDER = 20233;
	private static final int MARSH_STAKATO_DRONE = 20234;
	private static final int BREKA_ORC_OVERLORD = 20270;
	private static final int GRANDIS = 20554;
	private static final int LETO_LIZARDMAN_OVERLORD = 20582;
	private static final int KARUL_BUGBEAR = 20600;
	private static final int BLACK_WILLOW_LURKER = 27079;
	
	// Items
	private static final int KAIRA_LETTER = 3173;
	private static final int METHEUS_FUNERAL_JAR = 3174;
	private static final int KASANDRA_REMAINS = 3175;
	private static final int HERBALISM_TEXTBOOK = 3176;
	private static final int IXIA_LIST = 3177;
	private static final int MEDUSA_ICHOR = 3178;
	private static final int MARSH_SPIDER_FLUIDS = 3179;
	private static final int DEAD_SEEKER_DUNG = 3180;
	private static final int TYRANT_BLOOD = 3181;
	private static final int NIGHTSHADE_ROOT = 3182;
	private static final int BELLADONNA = 3183;
	private static final int ALDER_SKULL_1 = 3184;
	private static final int ALDER_SKULL_2 = 3185;
	private static final int ALDER_RECEIPT = 3186;
	private static final int REVELATIONS_MANUSCRIPT = 3187;
	private static final int KAIRA_RECOMMENDATION = 3189;
	private static final int KAIRA_INSTRUCTIONS = 3188;
	private static final int PALUS_CHARM = 3190;
	private static final int THIFIELL_LETTER = 3191;
	private static final int ARKENIA_NOTE = 3192;
	private static final int PIXY_GARNET = 3193;
	private static final int GRANDIS_SKULL = 3194;
	private static final int KARUL_BUGBEAR_SKULL = 3195;
	private static final int BREKA_OVERLORD_SKULL = 3196;
	private static final int LETO_OVERLORD_SKULL = 3197;
	private static final int RED_FAIRY_DUST = 3198;
	private static final int BLIGHT_TREANT_SEED = 3199;
	private static final int BLACK_WILLOW_LEAF = 3200;
	private static final int BLIGHT_TREANT_SAP = 3201;
	private static final int ARKENIA_LETTER = 3202;
	
	// Rewards
	private static final int MARK_OF_FATE = 3172;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	// Cond 6 drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(DEAD_SEEKER, 500000);
		CHANCES.put(TYRANT, 500000);
		CHANCES.put(TYRANT_KINGPIN, 600000);
		CHANCES.put(MEDUSA, 500000);
		CHANCES.put(MARSH_STAKATO, 400000);
		CHANCES.put(MARSH_STAKATO_WORKER, 300000);
		CHANCES.put(MARSH_STAKATO_SOLDIER, 500000);
		CHANCES.put(MARSH_STAKATO_DRONE, 600000);
		CHANCES.put(MARSH_SPIDER, 500000);
	}
	
	public Q00219_TestimonyOfFate()
	{
		super(219, "Testimony of Fate");
		registerQuestItems(KAIRA_LETTER, METHEUS_FUNERAL_JAR, KASANDRA_REMAINS, HERBALISM_TEXTBOOK, IXIA_LIST, MEDUSA_ICHOR, MARSH_SPIDER_FLUIDS, DEAD_SEEKER_DUNG, TYRANT_BLOOD, NIGHTSHADE_ROOT, BELLADONNA, ALDER_SKULL_1, ALDER_SKULL_2, ALDER_RECEIPT, REVELATIONS_MANUSCRIPT, KAIRA_RECOMMENDATION, KAIRA_INSTRUCTIONS, PALUS_CHARM, THIFIELL_LETTER, ARKENIA_NOTE, PIXY_GARNET, GRANDIS_SKULL, KARUL_BUGBEAR_SKULL, BREKA_OVERLORD_SKULL, LETO_OVERLORD_SKULL, RED_FAIRY_DUST, BLIGHT_TREANT_SEED, BLACK_WILLOW_LEAF, BLIGHT_TREANT_SAP, ARKENIA_LETTER);
		addStartNpc(KAIRA);
		addTalkId(KAIRA, METHEUS, IXIA, ALDER_SPIRIT, ROA, NORMAN, THIFIELL, ARKENIA, BLOODY_PIXY, BLIGHT_TREANT);
		addKillId(HANGMAN_TREE, MARSH_STAKATO, MEDUSA, TYRANT, TYRANT_KINGPIN, DEAD_SEEKER, MARSH_STAKATO_WORKER, MARSH_STAKATO_SOLDIER, MARSH_SPIDER, MARSH_STAKATO_DRONE, BREKA_ORC_OVERLORD, GRANDIS, LETO_LIZARDMAN_OVERLORD, KARUL_BUGBEAR, BLACK_WILLOW_LURKER);
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
			case "30476-05.htm":
			{
				st.startQuest();
				giveItems(player, KAIRA_LETTER, 1);
				if (!player.getVariables().getBoolean("secondClassChange37", false))
				{
					htmltext = "30476-05a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_37.get(player.getRace().ordinal()));
					player.getVariables().set("secondClassChange37", true);
				}
				break;
			}
			case "30114-04.htm":
			{
				st.setCond(12, true);
				takeItems(player, ALDER_SKULL_2, 1);
				giveItems(player, ALDER_RECEIPT, 1);
				break;
			}
			case "30476-12.htm":
			{
				if (player.getLevel() < 38)
				{
					htmltext = "30476-13.htm";
					st.setCond(14, true);
					giveItems(player, KAIRA_INSTRUCTIONS, 1);
				}
				else
				{
					st.setCond(15, true);
					takeItems(player, REVELATIONS_MANUSCRIPT, 1);
					giveItems(player, KAIRA_RECOMMENDATION, 1);
				}
				break;
			}
			case "30419-02.htm":
			{
				st.setCond(17, true);
				takeItems(player, THIFIELL_LETTER, 1);
				giveItems(player, ARKENIA_NOTE, 1);
				break;
			}
			case "31845-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, PIXY_GARNET, 1);
				break;
			}
			case "31850-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, BLIGHT_TREANT_SEED, 1);
				break;
			}
			case "30419-05.htm":
			{
				st.setCond(18, true);
				takeItems(player, ARKENIA_NOTE, 1);
				takeItems(player, BLIGHT_TREANT_SAP, 1);
				takeItems(player, RED_FAIRY_DUST, 1);
				giveItems(player, ARKENIA_LETTER, 1);
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
				if (player.getRace() != Race.DARK_ELF)
				{
					htmltext = "30476-02.htm";
				}
				else if ((player.getLevel() < 37) || (player.getPlayerClass().level() != 1))
				{
					htmltext = "30476-01.htm";
				}
				else
				{
					htmltext = "30476-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case KAIRA:
					{
						if (cond == 1)
						{
							htmltext = "30476-06.htm";
						}
						else if ((cond == 2) || (cond == 3))
						{
							htmltext = "30476-07.htm";
						}
						else if ((cond > 3) && (cond < 9))
						{
							htmltext = "30476-08.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30476-09.htm";
							st.setCond(10, true);
							takeItems(player, ALDER_SKULL_1, 1);
							addSpawn(ALDER_SPIRIT, player, false, 0);
						}
						else if ((cond > 9) && (cond < 13))
						{
							htmltext = "30476-10.htm";
						}
						else if (cond == 13)
						{
							htmltext = "30476-11.htm";
						}
						else if (cond == 14)
						{
							if (player.getLevel() < 38)
							{
								htmltext = "30476-14.htm";
							}
							else
							{
								htmltext = "30476-12.htm";
								st.setCond(15, true);
								takeItems(player, KAIRA_INSTRUCTIONS, 1);
								takeItems(player, REVELATIONS_MANUSCRIPT, 1);
								giveItems(player, KAIRA_RECOMMENDATION, 1);
							}
						}
						else if (cond == 15)
						{
							htmltext = "30476-16.htm";
						}
						else if (cond > 15)
						{
							htmltext = "30476-17.htm";
						}
						break;
					}
					case METHEUS:
					{
						if (cond == 1)
						{
							htmltext = "30614-01.htm";
							st.setCond(2, true);
							takeItems(player, KAIRA_LETTER, 1);
							giveItems(player, METHEUS_FUNERAL_JAR, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30614-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30614-03.htm";
							st.setCond(4);
							st.setCond(5, true);
							takeItems(player, KASANDRA_REMAINS, 1);
							giveItems(player, HERBALISM_TEXTBOOK, 1);
						}
						else if ((cond > 3) && (cond < 8))
						{
							htmltext = "30614-04.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30614-05.htm";
							st.setCond(9, true);
							takeItems(player, BELLADONNA, 1);
							giveItems(player, ALDER_SKULL_1, 1);
						}
						else if (cond > 8)
						{
							htmltext = "30614-06.htm";
						}
						break;
					}
					case IXIA:
					{
						if (cond == 5)
						{
							htmltext = "30463-01.htm";
							st.setCond(6, true);
							takeItems(player, HERBALISM_TEXTBOOK, 1);
							giveItems(player, IXIA_LIST, 1);
						}
						else if (cond == 6)
						{
							htmltext = "30463-02.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30463-03.htm";
							st.setCond(8, true);
							takeItems(player, IXIA_LIST, 1);
							takeItems(player, DEAD_SEEKER_DUNG, -1);
							takeItems(player, MARSH_SPIDER_FLUIDS, -1);
							takeItems(player, MEDUSA_ICHOR, -1);
							takeItems(player, NIGHTSHADE_ROOT, -1);
							takeItems(player, TYRANT_BLOOD, -1);
							giveItems(player, BELLADONNA, 1);
						}
						else if (cond == 8)
						{
							htmltext = "30463-04.htm";
						}
						else if (cond > 8)
						{
							htmltext = "30463-05.htm";
						}
						break;
					}
					case ALDER_SPIRIT:
					{
						if (cond == 10)
						{
							htmltext = "30613-01.htm";
							st.setCond(11, true);
							giveItems(player, ALDER_SKULL_2, 1);
							npc.deleteMe();
						}
						break;
					}
					case ROA:
					{
						if (cond == 11)
						{
							htmltext = "30114-01.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30114-05.htm";
						}
						else if (cond > 12)
						{
							htmltext = "30114-06.htm";
						}
						break;
					}
					case NORMAN:
					{
						if (cond == 12)
						{
							htmltext = "30210-01.htm";
							st.setCond(13, true);
							takeItems(player, ALDER_RECEIPT, 1);
							giveItems(player, REVELATIONS_MANUSCRIPT, 1);
						}
						else if (cond > 12)
						{
							htmltext = "30210-02.htm";
						}
						break;
					}
					case THIFIELL:
					{
						if (cond == 15)
						{
							htmltext = "30358-01.htm";
							st.setCond(16, true);
							takeItems(player, KAIRA_RECOMMENDATION, 1);
							giveItems(player, PALUS_CHARM, 1);
							giveItems(player, THIFIELL_LETTER, 1);
						}
						else if (cond == 16)
						{
							htmltext = "30358-02.htm";
						}
						else if (cond == 17)
						{
							htmltext = "30358-03.htm";
						}
						else if (cond == 18)
						{
							htmltext = "30358-04.htm";
							takeItems(player, PALUS_CHARM, 1);
							takeItems(player, ARKENIA_LETTER, 1);
							giveItems(player, MARK_OF_FATE, 1);
							addExpAndSp(player, 68183, 1750);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case ARKENIA:
					{
						if (cond == 16)
						{
							htmltext = "30419-01.htm";
						}
						else if (cond == 17)
						{
							htmltext = (hasQuestItems(player, BLIGHT_TREANT_SAP) && hasQuestItems(player, RED_FAIRY_DUST)) ? "30419-04.htm" : "30419-03.htm";
						}
						else if (cond == 18)
						{
							htmltext = "30419-06.htm";
						}
						break;
					}
					case BLOODY_PIXY:
					{
						if (cond == 17)
						{
							if (hasQuestItems(player, PIXY_GARNET))
							{
								if ((getQuestItemsCount(player, GRANDIS_SKULL) >= 10) && (getQuestItemsCount(player, KARUL_BUGBEAR_SKULL) >= 10) && (getQuestItemsCount(player, BREKA_OVERLORD_SKULL) >= 10) && (getQuestItemsCount(player, LETO_OVERLORD_SKULL) >= 10))
								{
									htmltext = "31845-04.htm";
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									takeItems(player, BREKA_OVERLORD_SKULL, -1);
									takeItems(player, GRANDIS_SKULL, -1);
									takeItems(player, KARUL_BUGBEAR_SKULL, -1);
									takeItems(player, LETO_OVERLORD_SKULL, -1);
									takeItems(player, PIXY_GARNET, 1);
									giveItems(player, RED_FAIRY_DUST, 1);
								}
								else
								{
									htmltext = "31845-03.htm";
								}
							}
							else if (hasQuestItems(player, RED_FAIRY_DUST))
							{
								htmltext = "31845-05.htm";
							}
							else
							{
								htmltext = "31845-01.htm";
							}
						}
						else if (cond == 18)
						{
							htmltext = "31845-05.htm";
						}
						break;
					}
					case BLIGHT_TREANT:
					{
						if (cond == 17)
						{
							if (hasQuestItems(player, BLIGHT_TREANT_SEED))
							{
								if (hasQuestItems(player, BLACK_WILLOW_LEAF))
								{
									htmltext = "31850-04.htm";
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									takeItems(player, BLACK_WILLOW_LEAF, 1);
									takeItems(player, BLIGHT_TREANT_SEED, 1);
									giveItems(player, BLIGHT_TREANT_SAP, 1);
								}
								else
								{
									htmltext = "31850-03.htm";
								}
							}
							else if (hasQuestItems(player, BLIGHT_TREANT_SAP))
							{
								htmltext = "31850-05.htm";
							}
							else
							{
								htmltext = "31850-01.htm";
							}
						}
						else if (cond == 18)
						{
							htmltext = "31850-05.htm";
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
			case HANGMAN_TREE:
			{
				if (st.isCond(2))
				{
					st.setCond(3, true);
					takeItems(player, METHEUS_FUNERAL_JAR, 1);
					giveItems(player, KASANDRA_REMAINS, 1);
				}
				break;
			}
			case DEAD_SEEKER:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, DEAD_SEEKER_DUNG) < 10) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					giveItems(player, DEAD_SEEKER_DUNG, 1);
					if ((getQuestItemsCount(player, DEAD_SEEKER_DUNG) >= 10) && (getQuestItemsCount(player, TYRANT_BLOOD) >= 10) && (getQuestItemsCount(player, MEDUSA_ICHOR) >= 10) && (getQuestItemsCount(player, NIGHTSHADE_ROOT) >= 10) && (getQuestItemsCount(player, MARSH_SPIDER_FLUIDS) >= 10))
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
			case TYRANT:
			case TYRANT_KINGPIN:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, TYRANT_BLOOD) < 10) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					giveItems(player, TYRANT_BLOOD, 1);
					if ((getQuestItemsCount(player, TYRANT_BLOOD) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_DUNG) >= 10) && (getQuestItemsCount(player, MEDUSA_ICHOR) >= 10) && (getQuestItemsCount(player, NIGHTSHADE_ROOT) >= 10) && (getQuestItemsCount(player, MARSH_SPIDER_FLUIDS) >= 10))
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
				if (st.isCond(6) && (getQuestItemsCount(player, MEDUSA_ICHOR) < 10) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					giveItems(player, MEDUSA_ICHOR, 1);
					if ((getQuestItemsCount(player, MEDUSA_ICHOR) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_DUNG) >= 10) && (getQuestItemsCount(player, TYRANT_BLOOD) >= 10) && (getQuestItemsCount(player, NIGHTSHADE_ROOT) >= 10) && (getQuestItemsCount(player, MARSH_SPIDER_FLUIDS) >= 10))
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
			case MARSH_STAKATO:
			case MARSH_STAKATO_WORKER:
			case MARSH_STAKATO_SOLDIER:
			case MARSH_STAKATO_DRONE:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, NIGHTSHADE_ROOT) < 10) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					giveItems(player, NIGHTSHADE_ROOT, 1);
					if ((getQuestItemsCount(player, NIGHTSHADE_ROOT) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_DUNG) >= 10) && (getQuestItemsCount(player, TYRANT_BLOOD) >= 10) && (getQuestItemsCount(player, MEDUSA_ICHOR) >= 10) && (getQuestItemsCount(player, MARSH_SPIDER_FLUIDS) >= 10))
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
			case MARSH_SPIDER:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, MARSH_SPIDER_FLUIDS) < 10) && (getRandom(1000000) < CHANCES.get(npcId)))
				{
					giveItems(player, MARSH_SPIDER_FLUIDS, 1);
					if ((getQuestItemsCount(player, MARSH_SPIDER_FLUIDS) >= 10) && (getQuestItemsCount(player, DEAD_SEEKER_DUNG) >= 10) && (getQuestItemsCount(player, TYRANT_BLOOD) >= 10) && (getQuestItemsCount(player, MEDUSA_ICHOR) >= 10) && (getQuestItemsCount(player, NIGHTSHADE_ROOT) >= 10))
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
			case GRANDIS:
			{
				if (hasQuestItems(player, PIXY_GARNET) && (getQuestItemsCount(player, GRANDIS_SKULL) < 10))
				{
					giveItems(player, GRANDIS_SKULL, 1);
				}
				break;
			}
			case LETO_LIZARDMAN_OVERLORD:
			{
				if (hasQuestItems(player, PIXY_GARNET) && (getQuestItemsCount(player, LETO_OVERLORD_SKULL) < 10))
				{
					giveItems(player, LETO_OVERLORD_SKULL, 1);
				}
				break;
			}
			case BREKA_ORC_OVERLORD:
			{
				if (hasQuestItems(player, PIXY_GARNET) && (getQuestItemsCount(player, BREKA_OVERLORD_SKULL) < 10))
				{
					giveItems(player, BREKA_OVERLORD_SKULL, 1);
				}
				break;
			}
			case KARUL_BUGBEAR:
			{
				if (hasQuestItems(player, PIXY_GARNET) && (getQuestItemsCount(player, KARUL_BUGBEAR_SKULL) < 10))
				{
					giveItems(player, KARUL_BUGBEAR_SKULL, 1);
				}
				break;
			}
			case BLACK_WILLOW_LURKER:
			{
				if (hasQuestItems(player, BLIGHT_TREANT_SEED) && (getQuestItemsCount(player, BLACK_WILLOW_LEAF) < 1))
				{
					giveItems(player, BLACK_WILLOW_LEAF, 1);
				}
				break;
			}
		}
	}
}
