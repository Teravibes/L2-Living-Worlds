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
package quests.Q00229_TestOfWitchcraft;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00229_TestOfWitchcraft extends Quest
{
	// NPCs
	private static final int LARA = 30063;
	private static final int ALEXANDRIA = 30098;
	private static final int IKER = 30110;
	private static final int VADIN = 30188;
	private static final int NESTLE = 30314;
	private static final int SIR_KLAUS_VASPER = 30417;
	private static final int LEOPOLD = 30435;
	private static final int KAIRA = 30476;
	private static final int ORIM = 30630;
	private static final int RODERIK = 30631;
	private static final int ENDRIGO = 30632;
	private static final int EVERT = 30633;
	
	// Monsters
	private static final int DIRE_WYRM = 20557;
	private static final int ENCHANTED_STONE_GOLEM = 20565;
	private static final int LETO_LIZARDMAN = 20577;
	private static final int LETO_LIZARDMAN_ARCHER = 20578;
	private static final int LETO_LIZARDMAN_SOLDIER = 20579;
	private static final int LETO_LIZARDMAN_WARRIOR = 20580;
	private static final int LETO_LIZARDMAN_SHAMAN = 20581;
	private static final int LETO_LIZARDMAN_OVERLORD = 20582;
	private static final int TAMLIN_ORC = 20601;
	private static final int TAMLIN_ORC_ARCHER = 20602;
	private static final int NAMELESS_REVENANT = 27099;
	private static final int SKELETAL_MERCENARY = 27100;
	private static final int DREVANUL_PRINCE_ZERUEL = 27101;
	
	// Items
	private static final int ORIM_DIAGRAM = 3308;
	private static final int ALEXANDRIA_BOOK = 3309;
	private static final int IKER_LIST = 3310;
	private static final int DIRE_WYRM_FANG = 3311;
	private static final int LETO_LIZARDMAN_CHARM = 3312;
	private static final int EN_GOLEM_HEARTSTONE = 3313;
	private static final int LARA_MEMO = 3314;
	private static final int NESTLE_MEMO = 3315;
	private static final int LEOPOLD_JOURNAL = 3316;
	private static final int AKLANTOTH_GEM_1 = 3317;
	private static final int AKLANTOTH_GEM_2 = 3318;
	private static final int AKLANTOTH_GEM_3 = 3319;
	private static final int AKLANTOTH_GEM_4 = 3320;
	private static final int AKLANTOTH_GEM_5 = 3321;
	private static final int AKLANTOTH_GEM_6 = 3322;
	private static final int BRIMSTONE_1 = 3323;
	private static final int ORIM_INSTRUCTIONS = 3324;
	private static final int ORIM_LETTER_1 = 3325;
	private static final int ORIM_LETTER_2 = 3326;
	private static final int SIR_VASPER_LETTER = 3327;
	private static final int VADIN_CRUCIFIX = 3328;
	private static final int TAMLIN_ORC_AMULET = 3329;
	private static final int VADIN_SANCTIONS = 3330;
	private static final int IKER_AMULET = 3331;
	private static final int SOULTRAP_CRYSTAL = 3332;
	private static final int PURGATORY_KEY = 3333;
	private static final int ZERUEL_BIND_CRYSTAL = 3334;
	private static final int BRIMSTONE_2 = 3335;
	private static final int SWORD_OF_BINDING = 3029;
	
	// Rewards
	private static final int MARK_OF_WITCHCRAFT = 3307;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	// Misc
	private static boolean _drevanulPrinceZeruel = false;
	private static boolean _swordOfBinding = false;
	
	public Q00229_TestOfWitchcraft()
	{
		super(229, "Test of Witchcraft");
		registerQuestItems(ORIM_DIAGRAM, ALEXANDRIA_BOOK, IKER_LIST, DIRE_WYRM_FANG, LETO_LIZARDMAN_CHARM, EN_GOLEM_HEARTSTONE, LARA_MEMO, NESTLE_MEMO, LEOPOLD_JOURNAL, AKLANTOTH_GEM_1, AKLANTOTH_GEM_2, AKLANTOTH_GEM_3, AKLANTOTH_GEM_4, AKLANTOTH_GEM_5, AKLANTOTH_GEM_6, BRIMSTONE_1, ORIM_INSTRUCTIONS, ORIM_LETTER_1, ORIM_LETTER_2, SIR_VASPER_LETTER, VADIN_CRUCIFIX, TAMLIN_ORC_AMULET, VADIN_SANCTIONS, IKER_AMULET, SOULTRAP_CRYSTAL, PURGATORY_KEY, ZERUEL_BIND_CRYSTAL, BRIMSTONE_2, SWORD_OF_BINDING);
		addStartNpc(ORIM);
		addTalkId(LARA, ALEXANDRIA, IKER, VADIN, NESTLE, SIR_KLAUS_VASPER, LEOPOLD, KAIRA, ORIM, RODERIK, ENDRIGO, EVERT);
		addAttackId(NAMELESS_REVENANT, SKELETAL_MERCENARY, DREVANUL_PRINCE_ZERUEL);
		addKillId(DIRE_WYRM, ENCHANTED_STONE_GOLEM, LETO_LIZARDMAN, LETO_LIZARDMAN_ARCHER, LETO_LIZARDMAN_SOLDIER, LETO_LIZARDMAN_WARRIOR, LETO_LIZARDMAN_SHAMAN, LETO_LIZARDMAN_OVERLORD, TAMLIN_ORC, TAMLIN_ORC_ARCHER, NAMELESS_REVENANT, SKELETAL_MERCENARY, DREVANUL_PRINCE_ZERUEL);
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
			case "30630-08.htm":
			{
				st.startQuest();
				giveItems(player, ORIM_DIAGRAM, 1);
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30630-08a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30630-14.htm":
			{
				st.setCond(4, true);
				st.unset("gem456");
				takeItems(player, AKLANTOTH_GEM_1, 1);
				takeItems(player, AKLANTOTH_GEM_2, 1);
				takeItems(player, AKLANTOTH_GEM_3, 1);
				takeItems(player, AKLANTOTH_GEM_4, 1);
				takeItems(player, AKLANTOTH_GEM_5, 1);
				takeItems(player, AKLANTOTH_GEM_6, 1);
				takeItems(player, ALEXANDRIA_BOOK, 1);
				giveItems(player, BRIMSTONE_1, 1);
				addSpawn(DREVANUL_PRINCE_ZERUEL, 70381, 109638, -3726, 0, false, 120000);
				break;
			}
			case "30630-16.htm":
			{
				st.setCond(6, true);
				takeItems(player, BRIMSTONE_1, 1);
				giveItems(player, ORIM_INSTRUCTIONS, 1);
				giveItems(player, ORIM_LETTER_1, 1);
				giveItems(player, ORIM_LETTER_2, 1);
				break;
			}
			case "30630-22.htm":
			{
				takeItems(player, IKER_AMULET, 1);
				takeItems(player, ORIM_INSTRUCTIONS, 1);
				takeItems(player, PURGATORY_KEY, 1);
				takeItems(player, SWORD_OF_BINDING, 1);
				takeItems(player, ZERUEL_BIND_CRYSTAL, 1);
				giveItems(player, MARK_OF_WITCHCRAFT, 1);
				addExpAndSp(player, 139796, 40000);
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(false, true);
				break;
			}
			case "30098-03.htm":
			{
				st.setCond(2, true);
				st.set("gem456", "1");
				takeItems(player, ORIM_DIAGRAM, 1);
				giveItems(player, ALEXANDRIA_BOOK, 1);
				break;
			}
			case "30110-03.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, IKER_LIST, 1);
				break;
			}
			case "30110-08.htm":
			{
				takeItems(player, ORIM_LETTER_2, 1);
				giveItems(player, IKER_AMULET, 1);
				giveItems(player, SOULTRAP_CRYSTAL, 1);
				if (hasQuestItems(player, SWORD_OF_BINDING))
				{
					st.setCond(7, true);
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30476-02.htm":
			{
				giveItems(player, AKLANTOTH_GEM_2, 1);
				if (hasQuestItems(player, AKLANTOTH_GEM_1, AKLANTOTH_GEM_3) && (st.getInt("gem456") == 6))
				{
					st.setCond(3, true);
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30063-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, LARA_MEMO, 1);
				break;
			}
			case "30314-02.htm":
			{
				st.set("gem456", "2");
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, NESTLE_MEMO, 1);
				break;
			}
			case "30435-02.htm":
			{
				st.set("gem456", "3");
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, NESTLE_MEMO, 1);
				giveItems(player, LEOPOLD_JOURNAL, 1);
				break;
			}
			case "30417-03.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, ORIM_LETTER_1, 1);
				giveItems(player, SIR_VASPER_LETTER, 1);
				break;
			}
			case "30633-02.htm":
			{
				st.setCond(9, true);
				giveItems(player, BRIMSTONE_2, 1);
				if (!_drevanulPrinceZeruel)
				{
					addSpawn(DREVANUL_PRINCE_ZERUEL, 13395, 169807, -3708, 0, false, 299000);
					_drevanulPrinceZeruel = true;
					
					// Resets Drevanul Prince Zeruel
					startQuestTimer("zeruel_cleanup", 300000, null, player, false);
				}
				break;
			}
			case "zeruel_despawn":
			{
				npc.abortAttack();
				npc.decayMe();
				return null;
			}
			case "zeruel_cleanup":
			{
				_drevanulPrinceZeruel = false;
				return null;
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
				if ((player.getPlayerClass() != PlayerClass.KNIGHT) && (player.getPlayerClass() != PlayerClass.WIZARD) && (player.getPlayerClass() != PlayerClass.PALUS_KNIGHT))
				{
					htmltext = "30630-01.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30630-02.htm";
				}
				else
				{
					htmltext = (player.getPlayerClass() == PlayerClass.WIZARD) ? "30630-03.htm" : "30630-05.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				final int gem456 = st.getInt("gem456");
				switch (npc.getId())
				{
					case ORIM:
					{
						if (cond == 1)
						{
							htmltext = "30630-09.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30630-10.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30630-11.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30630-14.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30630-15.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30630-17.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30630-18.htm";
							st.setCond(8, true);
						}
						else if ((cond == 8) || (cond == 9))
						{
							htmltext = "30630-18.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30630-19.htm";
						}
						break;
					}
					case ALEXANDRIA:
					{
						if (cond == 1)
						{
							htmltext = "30098-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30098-04.htm";
						}
						else
						{
							htmltext = "30098-05.htm";
						}
						break;
					}
					case KAIRA:
					{
						if (hasQuestItems(player, AKLANTOTH_GEM_2))
						{
							htmltext = "30476-03.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30476-01.htm";
						}
						else if (cond > 3)
						{
							htmltext = "30476-04.htm";
						}
						break;
					}
					case IKER:
					{
						if (hasQuestItems(player, AKLANTOTH_GEM_1))
						{
							htmltext = "30110-06.htm";
						}
						else if (hasQuestItems(player, IKER_LIST))
						{
							if ((getQuestItemsCount(player, DIRE_WYRM_FANG) + getQuestItemsCount(player, LETO_LIZARDMAN_CHARM) + getQuestItemsCount(player, EN_GOLEM_HEARTSTONE)) < 60)
							{
								htmltext = "30110-04.htm";
							}
							else
							{
								htmltext = "30110-05.htm";
								takeItems(player, IKER_LIST, 1);
								takeItems(player, DIRE_WYRM_FANG, -1);
								takeItems(player, EN_GOLEM_HEARTSTONE, -1);
								takeItems(player, LETO_LIZARDMAN_CHARM, -1);
								giveItems(player, AKLANTOTH_GEM_1, 1);
								
								if (hasQuestItems(player, AKLANTOTH_GEM_2, AKLANTOTH_GEM_3) && (gem456 == 6))
								{
									st.setCond(3, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
						}
						else if (cond == 2)
						{
							htmltext = "30110-01.htm";
						}
						else if ((cond == 6) && !hasQuestItems(player, SOULTRAP_CRYSTAL))
						{
							htmltext = "30110-07.htm";
						}
						else if ((cond >= 6) && (cond < 10))
						{
							htmltext = "30110-09.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30110-10.htm";
						}
						break;
					}
					case LARA:
					{
						if (hasQuestItems(player, AKLANTOTH_GEM_3))
						{
							htmltext = "30063-04.htm";
						}
						else if (hasQuestItems(player, LARA_MEMO))
						{
							htmltext = "30063-03.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30063-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30063-05.htm";
						}
						break;
					}
					case RODERIK:
					case ENDRIGO:
					{
						if (hasAtLeastOneQuestItem(player, LARA_MEMO, AKLANTOTH_GEM_3))
						{
							htmltext = npc.getId() + "-01.htm";
						}
						break;
					}
					case NESTLE:
					{
						if (gem456 == 1)
						{
							htmltext = "30314-01.htm";
						}
						else if (gem456 == 2)
						{
							htmltext = "30314-03.htm";
						}
						else if (gem456 > 2)
						{
							htmltext = "30314-04.htm";
						}
						break;
					}
					case LEOPOLD:
					{
						if (gem456 == 2)
						{
							htmltext = "30435-01.htm";
						}
						else if ((gem456 > 2) && (gem456 < 6))
						{
							htmltext = "30435-03.htm";
						}
						else if (gem456 == 6)
						{
							htmltext = "30435-04.htm";
						}
						else if (cond > 3)
						{
							htmltext = "30435-05.htm";
						}
						break;
					}
					case SIR_KLAUS_VASPER:
					{
						if (hasAtLeastOneQuestItem(player, SIR_VASPER_LETTER, VADIN_CRUCIFIX))
						{
							htmltext = "30417-04.htm";
						}
						else if (hasQuestItems(player, VADIN_SANCTIONS))
						{
							htmltext = "30417-05.htm";
							takeItems(player, VADIN_SANCTIONS, 1);
							giveItems(player, SWORD_OF_BINDING, 1);
							
							if (hasQuestItems(player, SOULTRAP_CRYSTAL))
							{
								st.setCond(7, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if (cond == 6)
						{
							htmltext = "30417-01.htm";
						}
						else if (cond > 6)
						{
							htmltext = "30417-06.htm";
						}
						break;
					}
					case VADIN:
					{
						if (hasQuestItems(player, SIR_VASPER_LETTER))
						{
							htmltext = "30188-01.htm";
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							takeItems(player, SIR_VASPER_LETTER, 1);
							giveItems(player, VADIN_CRUCIFIX, 1);
						}
						else if (hasQuestItems(player, VADIN_CRUCIFIX))
						{
							if (getQuestItemsCount(player, TAMLIN_ORC_AMULET) < 20)
							{
								htmltext = "30188-02.htm";
							}
							else
							{
								htmltext = "30188-03.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								takeItems(player, TAMLIN_ORC_AMULET, -1);
								takeItems(player, VADIN_CRUCIFIX, -1);
								giveItems(player, VADIN_SANCTIONS, 1);
							}
						}
						else if (hasQuestItems(player, VADIN_SANCTIONS))
						{
							htmltext = "30188-04.htm";
						}
						else if (cond > 6)
						{
							htmltext = "30188-05.htm";
						}
						break;
					}
					case EVERT:
					{
						if ((cond == 7) || (cond == 8))
						{
							htmltext = "30633-01.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30633-02.htm";
							
							if (!_drevanulPrinceZeruel)
							{
								addSpawn(DREVANUL_PRINCE_ZERUEL, 13395, 169807, -3708, 0, false, 299000);
								_drevanulPrinceZeruel = true;
								
								// Resets Drevanul Prince Zeruel
								startQuestTimer("zeruel_cleanup", 300000, null, player, false);
							}
						}
						else if (cond == 10)
						{
							htmltext = "30633-03.htm";
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
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		switch (npc.getId())
		{
			case NAMELESS_REVENANT:
			{
				if (hasQuestItems(attacker, LARA_MEMO) && !npc.isScriptValue(1))
				{
					npc.setScriptValue(1);
					npc.broadcastSay(ChatType.GENERAL, "I absolutely cannot give it to you! It is my precious jewel!");
				}
				break;
			}
			case SKELETAL_MERCENARY:
			{
				if ((st.getInt("gem456") > 2) && (st.getInt("gem456") < 6) && !npc.isScriptValue(1))
				{
					npc.setScriptValue(1);
					npc.broadcastSay(ChatType.GENERAL, "I absolutely cannot give it to you! It is my precious jewel!");
				}
				break;
			}
			case DREVANUL_PRINCE_ZERUEL:
			{
				if (st.isCond(4) && !npc.isScriptValue(1))
				{
					st.setCond(5, true);
					
					npc.setScriptValue(1);
					npc.broadcastSay(ChatType.GENERAL, "I'll take your lives later!!");
					
					startQuestTimer("zeruel_despawn", 1000, npc, attacker, false);
				}
				else if (st.isCond(9) && _drevanulPrinceZeruel)
				{
					if (attacker.getInventory().getPaperdollItemId(7) == SWORD_OF_BINDING)
					{
						_swordOfBinding = true;
						
						if (!npc.isScriptValue(1))
						{
							npc.setScriptValue(1);
							npc.broadcastSay(ChatType.GENERAL, "That sword is really...!");
						}
					}
					else
					{
						_swordOfBinding = false;
					}
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
		
		switch (npc.getId())
		{
			case DIRE_WYRM:
			{
				if (hasQuestItems(player, IKER_LIST) && (getQuestItemsCount(player, DIRE_WYRM_FANG) < 20))
				{
					giveItems(player, DIRE_WYRM_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case ENCHANTED_STONE_GOLEM:
			{
				if (hasQuestItems(player, IKER_LIST) && (getQuestItemsCount(player, EN_GOLEM_HEARTSTONE) < 20))
				{
					giveItems(player, EN_GOLEM_HEARTSTONE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case LETO_LIZARDMAN:
			case LETO_LIZARDMAN_ARCHER:
			{
				if (hasQuestItems(player, IKER_LIST) && (getRandom(10) < 5) && (getQuestItemsCount(player, LETO_LIZARDMAN_CHARM) < 20))
				{
					giveItems(player, LETO_LIZARDMAN_CHARM, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case LETO_LIZARDMAN_SOLDIER:
			case LETO_LIZARDMAN_WARRIOR:
			{
				if (hasQuestItems(player, IKER_LIST) && (getRandom(10) < 6) && (getQuestItemsCount(player, LETO_LIZARDMAN_CHARM) < 20))
				{
					giveItems(player, LETO_LIZARDMAN_CHARM, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case LETO_LIZARDMAN_SHAMAN:
			case LETO_LIZARDMAN_OVERLORD:
			{
				if (hasQuestItems(player, IKER_LIST) && (getRandom(10) < 7) && (getQuestItemsCount(player, LETO_LIZARDMAN_CHARM) < 20))
				{
					giveItems(player, LETO_LIZARDMAN_CHARM, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case NAMELESS_REVENANT:
			{
				if (hasQuestItems(player, LARA_MEMO))
				{
					takeItems(player, LARA_MEMO, 1);
					giveItems(player, AKLANTOTH_GEM_3, 1);
					
					if (hasQuestItems(player, AKLANTOTH_GEM_1, AKLANTOTH_GEM_2) && (st.getInt("gem456") == 6))
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
			case SKELETAL_MERCENARY:
			{
				final int gem456 = st.getInt("gem456");
				if (gem456 == 3)
				{
					st.set("gem456", "4");
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					giveItems(player, AKLANTOTH_GEM_4, 1);
				}
				else if (gem456 == 4)
				{
					st.set("gem456", "5");
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					giveItems(player, AKLANTOTH_GEM_5, 1);
				}
				else if (gem456 == 5)
				{
					st.set("gem456", "6");
					takeItems(player, LEOPOLD_JOURNAL, 1);
					giveItems(player, AKLANTOTH_GEM_6, 1);
					
					if (hasQuestItems(player, AKLANTOTH_GEM_1, AKLANTOTH_GEM_2, AKLANTOTH_GEM_3))
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
			case TAMLIN_ORC_ARCHER:
			{
				if (hasQuestItems(player, VADIN_CRUCIFIX) && (getRandom(10) < 5) && (getQuestItemsCount(player, TAMLIN_ORC_AMULET) < 20))
				{
					giveItems(player, TAMLIN_ORC_AMULET, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case DREVANUL_PRINCE_ZERUEL:
			{
				if (st.isCond(9) && _drevanulPrinceZeruel)
				{
					if (_swordOfBinding)
					{
						st.setCond(10);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						takeItems(player, BRIMSTONE_2, 1);
						takeItems(player, SOULTRAP_CRYSTAL, 1);
						giveItems(player, PURGATORY_KEY, 1);
						giveItems(player, ZERUEL_BIND_CRYSTAL, 1);
						npc.broadcastSay(ChatType.GENERAL, "No! I haven't completely finished the command for destruction and slaughter yet!!!");
					}
					
					cancelQuestTimer("zeruel_cleanup", null, player);
					_drevanulPrinceZeruel = false;
				}
				break;
			}
		}
	}
}
