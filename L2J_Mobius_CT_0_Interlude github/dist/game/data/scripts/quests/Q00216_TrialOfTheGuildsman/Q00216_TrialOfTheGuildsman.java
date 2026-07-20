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
package quests.Q00216_TrialOfTheGuildsman;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00216_TrialOfTheGuildsman extends Quest
{
	// NPCs
	private static final int VALKON = 30103;
	private static final int NORMAN = 30210;
	private static final int ALTRAN = 30283;
	private static final int PINTER = 30298;
	private static final int DUNING = 30688;
	
	// Monsters
	private static final int ANT = 20079;
	private static final int ANT_CAPTAIN = 20080;
	private static final int GRANITE_GOLEM = 20083;
	private static final int MANDRAGORA_SPROUT = 20154;
	private static final int MANDRAGORA_SAPLING = 20155;
	private static final int MANDRAGORA_BLOSSOM = 20156;
	private static final int SILENOS = 20168;
	private static final int STRAIN = 20200;
	private static final int GHOUL = 20201;
	private static final int DEAD_SEEKER = 20202;
	private static final int BREKA_ORC_SHAMAN = 20269;
	private static final int BREKA_ORC_OVERLORD = 20270;
	private static final int BREKA_ORC_WARRIOR = 20271;
	
	// Items
	private static final int RECIPE_JOURNEYMAN_RING = 3024;
	private static final int RECIPE_AMBER_BEAD = 3025;
	private static final int VALKON_RECOMMENDATION = 3120;
	private static final int MANDRAGORA_BERRY = 3121;
	private static final int ALTRAN_INSTRUCTIONS = 3122;
	private static final int ALTRAN_RECOMMENDATION_1 = 3123;
	private static final int ALTRAN_RECOMMENDATION_2 = 3124;
	private static final int NORMAN_INSTRUCTIONS = 3125;
	private static final int NORMAN_RECEIPT = 3126;
	private static final int DUNING_INSTRUCTIONS = 3127;
	private static final int DUNING_KEY = 3128;
	private static final int NORMAN_LIST = 3129;
	private static final int GRAY_BONE_POWDER = 3130;
	private static final int GRANITE_WHETSTONE = 3131;
	private static final int RED_PIGMENT = 3132;
	private static final int BRAIDED_YARN = 3133;
	private static final int JOURNEYMAN_GEM = 3134;
	private static final int PINTER_INSTRUCTIONS = 3135;
	private static final int AMBER_BEAD = 3136;
	private static final int AMBER_LUMP = 3137;
	private static final int JOURNEYMAN_DECO_BEADS = 3138;
	private static final int JOURNEYMAN_RING = 3139;
	
	// Rewards
	private static final int MARK_OF_GUILDSMAN = 3119;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00216_TrialOfTheGuildsman()
	{
		super(216, "Trial of the Guildsman");
		registerQuestItems(RECIPE_JOURNEYMAN_RING, RECIPE_AMBER_BEAD, VALKON_RECOMMENDATION, MANDRAGORA_BERRY, ALTRAN_INSTRUCTIONS, ALTRAN_RECOMMENDATION_1, ALTRAN_RECOMMENDATION_2, NORMAN_INSTRUCTIONS, NORMAN_RECEIPT, DUNING_INSTRUCTIONS, DUNING_KEY, NORMAN_LIST, GRAY_BONE_POWDER, GRANITE_WHETSTONE, RED_PIGMENT, BRAIDED_YARN, JOURNEYMAN_GEM, PINTER_INSTRUCTIONS, AMBER_BEAD, AMBER_LUMP, JOURNEYMAN_DECO_BEADS, JOURNEYMAN_RING);
		addStartNpc(VALKON);
		addTalkId(VALKON, NORMAN, ALTRAN, PINTER, DUNING);
		addKillId(ANT, ANT_CAPTAIN, GRANITE_GOLEM, MANDRAGORA_SPROUT, MANDRAGORA_SAPLING, MANDRAGORA_BLOSSOM, SILENOS, STRAIN, GHOUL, DEAD_SEEKER, BREKA_ORC_SHAMAN, BREKA_ORC_OVERLORD, BREKA_ORC_WARRIOR);
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
			case "30103-06.htm":
			{
				if (getQuestItemsCount(player, 57) >= 2000)
				{
					st.startQuest();
					takeItems(player, 57, 2000);
					giveItems(player, VALKON_RECOMMENDATION, 1);
					
					if (!player.getVariables().getBoolean("secondClassChange35", false))
					{
						htmltext = "30103-06d.htm";
						giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_35.get(player.getPlayerClass().getId()));
						player.getVariables().set("secondClassChange35", true);
					}
				}
				else
				{
					htmltext = "30103-05a.htm";
				}
				break;
			}
			case "30103-06c.htm":
			case "30103-07c.htm":
			{
				if (st.getCond() < 3)
				{
					st.setCond(3, true);
				}
				break;
			}
			case "30103-09a.htm":
			case "30103-09b.htm":
			{
				takeItems(player, ALTRAN_INSTRUCTIONS, 1);
				takeItems(player, JOURNEYMAN_RING, -1);
				giveItems(player, MARK_OF_GUILDSMAN, 1);
				addExpAndSp(player, 80993, 12250);
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(false, true);
				break;
			}
			case "30210-04.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, ALTRAN_RECOMMENDATION_1, 1);
				giveItems(player, NORMAN_INSTRUCTIONS, 1);
				giveItems(player, NORMAN_RECEIPT, 1);
				break;
			}
			case "30210-10.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, NORMAN_LIST, 1);
				break;
			}
			case "30283-03.htm":
			{
				st.setCond(5, true);
				takeItems(player, MANDRAGORA_BERRY, 1);
				takeItems(player, VALKON_RECOMMENDATION, 1);
				giveItems(player, ALTRAN_INSTRUCTIONS, 1);
				giveItems(player, ALTRAN_RECOMMENDATION_1, 1);
				giveItems(player, ALTRAN_RECOMMENDATION_2, 1);
				giveItems(player, RECIPE_JOURNEYMAN_RING, 1);
				break;
			}
			case "30298-04.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, ALTRAN_RECOMMENDATION_2, 1);
				giveItems(player, PINTER_INSTRUCTIONS, 1);
				
				// Artisan receives a recipe to craft Amber Beads, while spoiler case is handled in onKill section.
				if (player.getPlayerClass() == PlayerClass.ARTISAN)
				{
					htmltext = "30298-05.htm";
					giveItems(player, RECIPE_AMBER_BEAD, 1);
				}
				break;
			}
			case "30688-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, NORMAN_RECEIPT, 1);
				giveItems(player, DUNING_INSTRUCTIONS, 1);
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
				if ((player.getPlayerClass() != PlayerClass.SCAVENGER) && (player.getPlayerClass() != PlayerClass.ARTISAN))
				{
					htmltext = "30103-01.htm";
				}
				else if (player.getLevel() < 35)
				{
					htmltext = "30103-02.htm";
				}
				else
				{
					htmltext = "30103-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case VALKON:
					{
						if (cond == 1)
						{
							htmltext = "30103-06c.htm";
						}
						else if (cond < 5)
						{
							htmltext = "30103-07.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30103-08.htm";
						}
						else if (cond == 6)
						{
							htmltext = (getQuestItemsCount(player, JOURNEYMAN_RING) == 7) ? "30103-09.htm" : "30103-08.htm";
						}
						break;
					}
					case ALTRAN:
					{
						if (cond < 4)
						{
							htmltext = "30283-01.htm";
							if (cond == 1)
							{
								st.setCond(2, true);
							}
						}
						else if (cond == 4)
						{
							htmltext = "30283-02.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30283-04.htm";
						}
						break;
					}
					case NORMAN:
					{
						if (cond == 5)
						{
							if (hasQuestItems(player, ALTRAN_RECOMMENDATION_1))
							{
								htmltext = "30210-01.htm";
							}
							else if (hasQuestItems(player, NORMAN_RECEIPT))
							{
								htmltext = "30210-05.htm";
							}
							else if (hasQuestItems(player, DUNING_INSTRUCTIONS))
							{
								htmltext = "30210-06.htm";
							}
							else if (getQuestItemsCount(player, DUNING_KEY) == 30)
							{
								htmltext = "30210-07.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								takeItems(player, DUNING_KEY, -1);
							}
							else if (hasQuestItems(player, NORMAN_LIST))
							{
								if ((getQuestItemsCount(player, GRAY_BONE_POWDER) == 70) && (getQuestItemsCount(player, GRANITE_WHETSTONE) == 70) && (getQuestItemsCount(player, RED_PIGMENT) == 70) && (getQuestItemsCount(player, BRAIDED_YARN) == 70))
								{
									htmltext = "30210-12.htm";
									takeItems(player, NORMAN_INSTRUCTIONS, 1);
									takeItems(player, NORMAN_LIST, 1);
									takeItems(player, BRAIDED_YARN, -1);
									takeItems(player, GRANITE_WHETSTONE, -1);
									takeItems(player, GRAY_BONE_POWDER, -1);
									takeItems(player, RED_PIGMENT, -1);
									giveItems(player, JOURNEYMAN_GEM, 7);
									
									if (getQuestItemsCount(player, JOURNEYMAN_DECO_BEADS) == 7)
									{
										st.setCond(6, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
								else
								{
									htmltext = "30210-11.htm";
								}
							}
						}
						break;
					}
					case DUNING:
					{
						if (cond == 5)
						{
							if (hasQuestItems(player, NORMAN_RECEIPT))
							{
								htmltext = "30688-01.htm";
							}
							else if (hasQuestItems(player, DUNING_INSTRUCTIONS))
							{
								if (getQuestItemsCount(player, DUNING_KEY) < 30)
								{
									htmltext = "30688-03.htm";
								}
								else
								{
									htmltext = "30688-04.htm";
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									takeItems(player, DUNING_INSTRUCTIONS, 1);
								}
							}
							else
							{
								htmltext = "30688-05.htm";
							}
						}
						break;
					}
					case PINTER:
					{
						if (cond == 5)
						{
							if (hasQuestItems(player, ALTRAN_RECOMMENDATION_2))
							{
								htmltext = (player.getLevel() < 36) ? "30298-01.htm" : "30298-02.htm";
							}
							else if (hasQuestItems(player, PINTER_INSTRUCTIONS))
							{
								if (getQuestItemsCount(player, AMBER_BEAD) < 70)
								{
									htmltext = "30298-06.htm";
								}
								else
								{
									htmltext = "30298-07.htm";
									takeItems(player, AMBER_BEAD, -1);
									takeItems(player, PINTER_INSTRUCTIONS, 1);
									giveItems(player, JOURNEYMAN_DECO_BEADS, 7);
									
									if (getQuestItemsCount(player, JOURNEYMAN_GEM) == 7)
									{
										st.setCond(6, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
							}
						}
						else if (hasQuestItems(player, JOURNEYMAN_DECO_BEADS))
						{
							htmltext = "30298-08.htm";
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
		
		switch (npc.getId())
		{
			case MANDRAGORA_SPROUT:
			case MANDRAGORA_SAPLING:
			case MANDRAGORA_BLOSSOM:
			{
				if (st.isCond(3))
				{
					giveItems(player, MANDRAGORA_BERRY, 1);
					st.setCond(4, true);
				}
				break;
			}
			case BREKA_ORC_WARRIOR:
			case BREKA_ORC_OVERLORD:
			case BREKA_ORC_SHAMAN:
			{
				if (hasQuestItems(player, DUNING_INSTRUCTIONS) && (getQuestItemsCount(player, DUNING_KEY) < 30))
				{
					giveItems(player, DUNING_KEY, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case GHOUL:
			case STRAIN:
			{
				if (hasQuestItems(player, NORMAN_LIST) && (getQuestItemsCount(player, GRAY_BONE_POWDER) < 70))
				{
					giveItems(player, GRAY_BONE_POWDER, 5);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case GRANITE_GOLEM:
			{
				if (hasQuestItems(player, NORMAN_LIST) && (getQuestItemsCount(player, GRANITE_WHETSTONE) < 70))
				{
					giveItems(player, GRANITE_WHETSTONE, 7);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case DEAD_SEEKER:
			{
				if (hasQuestItems(player, NORMAN_LIST) && (getQuestItemsCount(player, RED_PIGMENT) < 70))
				{
					giveItems(player, RED_PIGMENT, 7);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case SILENOS:
			{
				if (hasQuestItems(player, NORMAN_LIST) && (getQuestItemsCount(player, BRAIDED_YARN) < 70))
				{
					giveItems(player, BRAIDED_YARN, 10);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case ANT:
			case ANT_CAPTAIN:
			{
				int count = 0;
				if ((player.getPlayerClass() == PlayerClass.SCAVENGER) && npc.isSweepActive() && (npc.asAttackable().getSpoilerObjectId() == player.getObjectId()))
				{
					count += 5;
				}
				
				if (getRandomBoolean() && (player.getPlayerClass() == PlayerClass.ARTISAN))
				{
					giveItems(player, AMBER_LUMP, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				
				if ((getQuestItemsCount(player, AMBER_BEAD) + count) < 70)
				{
					count += 5;
				}
				
				if (count > 0)
				{
					giveItemRandomly(player, npc, AMBER_BEAD, count, 70, 1, true);
				}
				break;
			}
		}
	}
}
