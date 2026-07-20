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
package quests.Q00233_TestOfTheWarSpirit;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00233_TestOfTheWarSpirit extends Quest
{
	// NPCs
	private static final int VIVYAN = 30030;
	private static final int SARIEN = 30436;
	private static final int RACOY = 30507;
	private static final int SOMAK = 30510;
	private static final int MANAKIA = 30515;
	private static final int ORIM = 30630;
	private static final int ANCESTOR_MARTANKUS = 30649;
	private static final int PEKIRON = 30682;
	
	// Monsters
	private static final int NOBLE_ANT = 20089;
	private static final int NOBLE_ANT_LEADER = 20090;
	private static final int MEDUSA = 20158;
	private static final int PORTA = 20213;
	private static final int EXCURO = 20214;
	private static final int MORDEO = 20215;
	private static final int LETO_LIZARDMAN_SHAMAN = 20581;
	private static final int LETO_LIZARDMAN_OVERLORD = 20582;
	private static final int TAMLIN_ORC = 20601;
	private static final int TAMLIN_ORC_ARCHER = 20602;
	private static final int STENOA_GORGON_QUEEN = 27108;
	
	// Items
	private static final int VENDETTA_TOTEM = 2880;
	private static final int TAMLIN_ORC_HEAD = 2881;
	private static final int WARSPIRIT_TOTEM = 2882;
	private static final int ORIM_CONTRACT = 2883;
	private static final int PORTA_EYE = 2884;
	private static final int EXCURO_SCALE = 2885;
	private static final int MORDEO_TALON = 2886;
	private static final int BRAKI_REMAINS_1 = 2887;
	private static final int PEKIRON_TOTEM = 2888;
	private static final int TONAR_SKULL = 2889;
	private static final int TONAR_RIBBONE = 2890;
	private static final int TONAR_SPINE = 2891;
	private static final int TONAR_ARMBONE = 2892;
	private static final int TONAR_THIGHBONE = 2893;
	private static final int TONAR_REMAINS_1 = 2894;
	private static final int MANAKIA_TOTEM = 2895;
	private static final int HERMODT_SKULL = 2896;
	private static final int HERMODT_RIBBONE = 2897;
	private static final int HERMODT_SPINE = 2898;
	private static final int HERMODT_ARMBONE = 2899;
	private static final int HERMODT_THIGHBONE = 2900;
	private static final int HERMODT_REMAINS_1 = 2901;
	private static final int RACOY_TOTEM = 2902;
	private static final int VIVYAN_LETTER = 2903;
	private static final int INSECT_DIAGRAM_BOOK = 2904;
	private static final int KIRUNA_SKULL = 2905;
	private static final int KIRUNA_RIBBONE = 2906;
	private static final int KIRUNA_SPINE = 2907;
	private static final int KIRUNA_ARMBONE = 2908;
	private static final int KIRUNA_THIGHBONE = 2909;
	private static final int KIRUNA_REMAINS_1 = 2910;
	private static final int BRAKI_REMAINS_2 = 2911;
	private static final int TONAR_REMAINS_2 = 2912;
	private static final int HERMODT_REMAINS_2 = 2913;
	private static final int KIRUNA_REMAINS_2 = 2914;
	
	// Rewards
	private static final int MARK_OF_WARSPIRIT = 2879;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00233_TestOfTheWarSpirit()
	{
		super(233, "Test of the War Spirit");
		registerQuestItems(VENDETTA_TOTEM, TAMLIN_ORC_HEAD, WARSPIRIT_TOTEM, ORIM_CONTRACT, PORTA_EYE, EXCURO_SCALE, MORDEO_TALON, BRAKI_REMAINS_1, PEKIRON_TOTEM, TONAR_SKULL, TONAR_RIBBONE, TONAR_SPINE, TONAR_ARMBONE, TONAR_THIGHBONE, TONAR_REMAINS_1, MANAKIA_TOTEM, HERMODT_SKULL, HERMODT_RIBBONE, HERMODT_SPINE, HERMODT_ARMBONE, HERMODT_THIGHBONE, HERMODT_REMAINS_1, RACOY_TOTEM, VIVYAN_LETTER, INSECT_DIAGRAM_BOOK, KIRUNA_SKULL, KIRUNA_RIBBONE, KIRUNA_SPINE, KIRUNA_ARMBONE, KIRUNA_THIGHBONE, KIRUNA_REMAINS_1, BRAKI_REMAINS_2, TONAR_REMAINS_2, HERMODT_REMAINS_2, KIRUNA_REMAINS_2);
		addStartNpc(SOMAK);
		addTalkId(SOMAK, VIVYAN, SARIEN, RACOY, MANAKIA, ORIM, ANCESTOR_MARTANKUS, PEKIRON);
		addKillId(NOBLE_ANT, NOBLE_ANT_LEADER, MEDUSA, PORTA, EXCURO, MORDEO, LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD, TAMLIN_ORC, TAMLIN_ORC_ARCHER, STENOA_GORGON_QUEEN);
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
			case "30510-05.htm":
			{
				st.startQuest();
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30510-05e.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30630-04.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, ORIM_CONTRACT, 1);
				break;
			}
			case "30507-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, RACOY_TOTEM, 1);
				break;
			}
			case "30030-04.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, VIVYAN_LETTER, 1);
				break;
			}
			case "30682-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, PEKIRON_TOTEM, 1);
				break;
			}
			case "30515-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, MANAKIA_TOTEM, 1);
				break;
			}
			case "30649-03.htm":
			{
				takeItems(player, TAMLIN_ORC_HEAD, -1);
				takeItems(player, WARSPIRIT_TOTEM, -1);
				takeItems(player, BRAKI_REMAINS_2, -1);
				takeItems(player, HERMODT_REMAINS_2, -1);
				takeItems(player, KIRUNA_REMAINS_2, -1);
				takeItems(player, TONAR_REMAINS_2, -1);
				giveItems(player, MARK_OF_WARSPIRIT, 1);
				addExpAndSp(player, 63483, 17500);
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(false, true);
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
				if (player.getPlayerClass() == PlayerClass.ORC_SHAMAN)
				{
					htmltext = (player.getLevel() < 39) ? "30510-03.htm" : "30510-04.htm";
				}
				else
				{
					htmltext = (player.getRace() == Race.ORC) ? "30510-02.htm" : "30510-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case SOMAK:
					{
						if (cond == 1)
						{
							htmltext = "30510-06.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30510-07.htm";
							st.setCond(3, true);
							takeItems(player, BRAKI_REMAINS_1, 1);
							takeItems(player, HERMODT_REMAINS_1, 1);
							takeItems(player, KIRUNA_REMAINS_1, 1);
							takeItems(player, TONAR_REMAINS_1, 1);
							giveItems(player, VENDETTA_TOTEM, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30510-08.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30510-09.htm";
							st.setCond(5, true);
							takeItems(player, VENDETTA_TOTEM, 1);
							giveItems(player, BRAKI_REMAINS_2, 1);
							giveItems(player, HERMODT_REMAINS_2, 1);
							giveItems(player, KIRUNA_REMAINS_2, 1);
							giveItems(player, TONAR_REMAINS_2, 1);
							giveItems(player, WARSPIRIT_TOTEM, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30510-10.htm";
						}
						break;
					}
					case ORIM:
					{
						if ((cond == 1) && !hasQuestItems(player, BRAKI_REMAINS_1))
						{
							if (!hasQuestItems(player, ORIM_CONTRACT))
							{
								htmltext = "30630-01.htm";
							}
							else if ((getQuestItemsCount(player, PORTA_EYE) + getQuestItemsCount(player, EXCURO_SCALE) + getQuestItemsCount(player, MORDEO_TALON)) == 30)
							{
								htmltext = "30630-06.htm";
								takeItems(player, EXCURO_SCALE, 10);
								takeItems(player, MORDEO_TALON, 10);
								takeItems(player, PORTA_EYE, 10);
								takeItems(player, ORIM_CONTRACT, 1);
								giveItems(player, BRAKI_REMAINS_1, 1);
								
								if (hasQuestItems(player, HERMODT_REMAINS_1, KIRUNA_REMAINS_1, TONAR_REMAINS_1))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30630-05.htm";
							}
						}
						else
						{
							htmltext = "30630-07.htm";
						}
						break;
					}
					case RACOY:
					{
						if ((cond == 1) && !hasQuestItems(player, KIRUNA_REMAINS_1))
						{
							if (!hasQuestItems(player, RACOY_TOTEM))
							{
								htmltext = "30507-01.htm";
							}
							else if (hasQuestItems(player, VIVYAN_LETTER))
							{
								htmltext = "30507-04.htm";
							}
							else if (hasQuestItems(player, INSECT_DIAGRAM_BOOK))
							{
								if (hasQuestItems(player, KIRUNA_ARMBONE, KIRUNA_RIBBONE, KIRUNA_SKULL, KIRUNA_SPINE, KIRUNA_THIGHBONE))
								{
									htmltext = "30507-06.htm";
									takeItems(player, INSECT_DIAGRAM_BOOK, 1);
									takeItems(player, RACOY_TOTEM, 1);
									takeItems(player, KIRUNA_ARMBONE, 1);
									takeItems(player, KIRUNA_RIBBONE, 1);
									takeItems(player, KIRUNA_SKULL, 1);
									takeItems(player, KIRUNA_SPINE, 1);
									takeItems(player, KIRUNA_THIGHBONE, 1);
									giveItems(player, KIRUNA_REMAINS_1, 1);
									
									if (hasQuestItems(player, BRAKI_REMAINS_1, HERMODT_REMAINS_1, TONAR_REMAINS_1))
									{
										st.setCond(2, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
								else
								{
									htmltext = "30507-05.htm";
								}
							}
							else
							{
								htmltext = "30507-03.htm";
							}
						}
						else
						{
							htmltext = "30507-07.htm";
						}
						break;
					}
					case VIVYAN:
					{
						if ((cond == 1) && hasQuestItems(player, RACOY_TOTEM))
						{
							if (hasQuestItems(player, VIVYAN_LETTER))
							{
								htmltext = "30030-05.htm";
							}
							else if (hasQuestItems(player, INSECT_DIAGRAM_BOOK))
							{
								htmltext = "30030-06.htm";
							}
							else
							{
								htmltext = "30030-01.htm";
							}
						}
						else
						{
							htmltext = "30030-07.htm";
						}
						break;
					}
					case SARIEN:
					{
						if ((cond == 1) && hasQuestItems(player, RACOY_TOTEM))
						{
							if (hasQuestItems(player, VIVYAN_LETTER))
							{
								htmltext = "30436-01.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								takeItems(player, VIVYAN_LETTER, 1);
								giveItems(player, INSECT_DIAGRAM_BOOK, 1);
							}
							else if (hasQuestItems(player, INSECT_DIAGRAM_BOOK))
							{
								htmltext = "30436-02.htm";
							}
						}
						else
						{
							htmltext = "30436-03.htm";
						}
						break;
					}
					case PEKIRON:
					{
						if ((cond == 1) && !hasQuestItems(player, TONAR_REMAINS_1))
						{
							if (!hasQuestItems(player, PEKIRON_TOTEM))
							{
								htmltext = "30682-01.htm";
							}
							else if (hasQuestItems(player, TONAR_ARMBONE, TONAR_RIBBONE, TONAR_SKULL, TONAR_SPINE, TONAR_THIGHBONE))
							{
								htmltext = "30682-04.htm";
								takeItems(player, PEKIRON_TOTEM, 1);
								takeItems(player, TONAR_ARMBONE, 1);
								takeItems(player, TONAR_RIBBONE, 1);
								takeItems(player, TONAR_SKULL, 1);
								takeItems(player, TONAR_SPINE, 1);
								takeItems(player, TONAR_THIGHBONE, 1);
								giveItems(player, TONAR_REMAINS_1, 1);
								
								if (hasQuestItems(player, BRAKI_REMAINS_1, HERMODT_REMAINS_1, KIRUNA_REMAINS_1))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30682-03.htm";
							}
						}
						else
						{
							htmltext = "30682-05.htm";
						}
						break;
					}
					case MANAKIA:
					{
						if ((cond == 1) && !hasQuestItems(player, HERMODT_REMAINS_1))
						{
							if (!hasQuestItems(player, MANAKIA_TOTEM))
							{
								htmltext = "30515-01.htm";
							}
							else if (hasQuestItems(player, HERMODT_ARMBONE, HERMODT_RIBBONE, HERMODT_SKULL, HERMODT_SPINE, HERMODT_THIGHBONE))
							{
								htmltext = "30515-04.htm";
								takeItems(player, MANAKIA_TOTEM, 1);
								takeItems(player, HERMODT_ARMBONE, 1);
								takeItems(player, HERMODT_RIBBONE, 1);
								takeItems(player, HERMODT_SKULL, 1);
								takeItems(player, HERMODT_SPINE, 1);
								takeItems(player, HERMODT_THIGHBONE, 1);
								giveItems(player, HERMODT_REMAINS_1, 1);
								
								if (hasQuestItems(player, BRAKI_REMAINS_1, KIRUNA_REMAINS_1, TONAR_REMAINS_1))
								{
									st.setCond(2, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else
							{
								htmltext = "30515-03.htm";
							}
						}
						else
						{
							htmltext = "30515-05.htm";
						}
						break;
					}
					case ANCESTOR_MARTANKUS:
					{
						if (cond == 5)
						{
							htmltext = "30649-01.htm";
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
			case PORTA:
			{
				if (hasQuestItems(player, ORIM_CONTRACT) && (getQuestItemsCount(player, PORTA_EYE) < 10))
				{
					giveItems(player, PORTA_EYE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case EXCURO:
			{
				if (hasQuestItems(player, ORIM_CONTRACT) && (getQuestItemsCount(player, EXCURO_SCALE) < 10))
				{
					giveItems(player, EXCURO_SCALE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case MORDEO:
			{
				if (hasQuestItems(player, ORIM_CONTRACT) && (getQuestItemsCount(player, MORDEO_TALON) < 10))
				{
					giveItems(player, MORDEO_TALON, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case NOBLE_ANT:
			case NOBLE_ANT_LEADER:
			{
				if (hasQuestItems(player, INSECT_DIAGRAM_BOOK))
				{
					final int rndAnt = getRandom(100);
					if (rndAnt > 70)
					{
						if (hasQuestItems(player, KIRUNA_THIGHBONE))
						{
							if (!hasQuestItems(player, KIRUNA_ARMBONE))
							{
								giveItems(player, KIRUNA_ARMBONE, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if (!hasQuestItems(player, KIRUNA_THIGHBONE))
						{
							giveItems(player, KIRUNA_THIGHBONE, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					else if (rndAnt > 40)
					{
						if (hasQuestItems(player, KIRUNA_SPINE))
						{
							if (!hasQuestItems(player, KIRUNA_RIBBONE))
							{
								giveItems(player, KIRUNA_RIBBONE, 1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if (!hasQuestItems(player, KIRUNA_SPINE))
						{
							giveItems(player, KIRUNA_SPINE, 1);
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					else if ((rndAnt > 10) && !hasQuestItems(player, KIRUNA_SKULL))
					{
						giveItems(player, KIRUNA_SKULL, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case LETO_LIZARDMAN_SHAMAN:
			case LETO_LIZARDMAN_OVERLORD:
			{
				if (hasQuestItems(player, PEKIRON_TOTEM) && getRandomBoolean())
				{
					if (!hasQuestItems(player, TONAR_SKULL))
					{
						giveItems(player, TONAR_SKULL, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (!hasQuestItems(player, TONAR_RIBBONE))
					{
						giveItems(player, TONAR_RIBBONE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (!hasQuestItems(player, TONAR_SPINE))
					{
						giveItems(player, TONAR_SPINE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (!hasQuestItems(player, TONAR_ARMBONE))
					{
						giveItems(player, TONAR_ARMBONE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (!hasQuestItems(player, TONAR_THIGHBONE))
					{
						giveItems(player, TONAR_THIGHBONE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case MEDUSA:
			{
				if (hasQuestItems(player, MANAKIA_TOTEM) && getRandomBoolean())
				{
					if (!hasQuestItems(player, HERMODT_RIBBONE))
					{
						giveItems(player, HERMODT_RIBBONE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (!hasQuestItems(player, HERMODT_SPINE))
					{
						giveItems(player, HERMODT_SPINE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (!hasQuestItems(player, HERMODT_ARMBONE))
					{
						giveItems(player, HERMODT_ARMBONE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else if (!hasQuestItems(player, HERMODT_THIGHBONE))
					{
						giveItems(player, HERMODT_THIGHBONE, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case STENOA_GORGON_QUEEN:
			{
				if (hasQuestItems(player, MANAKIA_TOTEM) && !hasQuestItems(player, HERMODT_SKULL))
				{
					giveItems(player, HERMODT_SKULL, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case TAMLIN_ORC:
			case TAMLIN_ORC_ARCHER:
			{
				if (hasQuestItems(player, VENDETTA_TOTEM) && !st.isCond(4) && getRandomBoolean() && (getQuestItemsCount(player, TAMLIN_ORC_HEAD) < 13))
				{
					giveItems(player, TAMLIN_ORC_HEAD, 1);
					if (getQuestItemsCount(player, TAMLIN_ORC_HEAD) >= 13)
					{
						st.setCond(4, true);
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
