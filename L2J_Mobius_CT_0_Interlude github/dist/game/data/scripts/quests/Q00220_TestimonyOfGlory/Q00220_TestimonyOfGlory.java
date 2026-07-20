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
package quests.Q00220_TestimonyOfGlory;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00220_TestimonyOfGlory extends Quest
{
	// NPCs
	private static final int KASMAN = 30501;
	private static final int VOKIAN = 30514;
	private static final int MANAKIA = 30515;
	private static final int KAKAI = 30565;
	private static final int TANAPI = 30571;
	private static final int VOLTAR = 30615;
	private static final int KEPRA = 30616;
	private static final int BURAI = 30617;
	private static final int HARAK = 30618;
	private static final int DRIKO = 30619;
	private static final int CHIANTA = 30642;
	
	// Monsters
	private static final int TYRANT = 20192;
	private static final int MARSH_STAKATO_DRONE = 20234;
	private static final int GUARDIAN_BASILISK = 20550;
	private static final int MANASHEN_GARGOYLE = 20563;
	private static final int TIMAK_ORC = 20583;
	private static final int TIMAK_ORC_ARCHER = 20584;
	private static final int TIMAK_ORC_SOLDIER = 20585;
	private static final int TIMAK_ORC_WARRIOR = 20586;
	private static final int TIMAK_ORC_SHAMAN = 20587;
	private static final int TIMAK_ORC_OVERLORD = 20588;
	private static final int TAMLIN_ORC = 20601;
	private static final int TAMLIN_ORC_ARCHER = 20602;
	private static final int RAGNA_ORC_OVERLORD = 20778;
	private static final int RAGNA_ORC_SEER = 20779;
	private static final int PASHIKA_SON_OF_VOLTAR = 27080;
	private static final int VULTUS_SON_OF_VOLTAR = 27081;
	private static final int ENKU_ORC_OVERLORD = 27082;
	private static final int MAKUM_BUGBEAR_THUG = 27083;
	private static final int REVENANT_OF_TANTOS_CHIEF = 27086;
	
	// Items
	private static final int VOKIAN_ORDER_1 = 3204;
	private static final int MANASHEN_SHARD = 3205;
	private static final int TYRANT_TALON = 3206;
	private static final int GUARDIAN_BASILISK_FANG = 3207;
	private static final int VOKIAN_ORDER_2 = 3208;
	private static final int NECKLACE_OF_AUTHORITY = 3209;
	private static final int CHIANTA_ORDER_1 = 3210;
	private static final int SCEPTER_OF_BREKA = 3211;
	private static final int SCEPTER_OF_ENKU = 3212;
	private static final int SCEPTER_OF_VUKU = 3213;
	private static final int SCEPTER_OF_TUREK = 3214;
	private static final int SCEPTER_OF_TUNATH = 3215;
	private static final int CHIANTA_ORDER_2 = 3216;
	private static final int CHIANTA_ORDER_3 = 3217;
	private static final int TAMLIN_ORC_SKULL = 3218;
	private static final int TIMAK_ORC_HEAD = 3219;
	private static final int SCEPTER_BOX = 3220;
	private static final int PASHIKA_HEAD = 3221;
	private static final int VULTUS_HEAD = 3222;
	private static final int GLOVE_OF_VOLTAR = 3223;
	private static final int ENKU_OVERLORD_HEAD = 3224;
	private static final int GLOVE_OF_KEPRA = 3225;
	private static final int MAKUM_BUGBEAR_HEAD = 3226;
	private static final int GLOVE_OF_BURAI = 3227;
	private static final int MANAKIA_LETTER_1 = 3228;
	private static final int MANAKIA_LETTER_2 = 3229;
	private static final int KASMAN_LETTER_1 = 3230;
	private static final int KASMAN_LETTER_2 = 3231;
	private static final int KASMAN_LETTER_3 = 3232;
	private static final int DRIKO_CONTRACT = 3233;
	private static final int STAKATO_DRONE_HUSK = 3234;
	private static final int TANAPI_ORDER = 3235;
	private static final int SCEPTER_OF_TANTOS = 3236;
	private static final int RITUAL_BOX = 3237;
	
	// Rewards
	private static final int MARK_OF_GLORY = 3203;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	// Checks & Instances
	private static boolean _sonsOfVoltar = false;
	private static boolean _enkuOrcOverlords = false;
	private static boolean _makumBugbearThugs = false;
	
	public Q00220_TestimonyOfGlory()
	{
		super(220, "Testimony of Glory");
		registerQuestItems(VOKIAN_ORDER_1, MANASHEN_SHARD, TYRANT_TALON, GUARDIAN_BASILISK_FANG, VOKIAN_ORDER_2, NECKLACE_OF_AUTHORITY, CHIANTA_ORDER_1, SCEPTER_OF_BREKA, SCEPTER_OF_ENKU, SCEPTER_OF_VUKU, SCEPTER_OF_TUREK, SCEPTER_OF_TUNATH, CHIANTA_ORDER_2, CHIANTA_ORDER_3, TAMLIN_ORC_SKULL, TIMAK_ORC_HEAD, SCEPTER_BOX, PASHIKA_HEAD, VULTUS_HEAD, GLOVE_OF_VOLTAR, ENKU_OVERLORD_HEAD, GLOVE_OF_KEPRA, MAKUM_BUGBEAR_HEAD, GLOVE_OF_BURAI, MANAKIA_LETTER_1, MANAKIA_LETTER_2, KASMAN_LETTER_1, KASMAN_LETTER_2, KASMAN_LETTER_3, DRIKO_CONTRACT, STAKATO_DRONE_HUSK, TANAPI_ORDER, SCEPTER_OF_TANTOS, RITUAL_BOX);
		addStartNpc(VOKIAN);
		addTalkId(KASMAN, VOKIAN, MANAKIA, KAKAI, TANAPI, VOLTAR, KEPRA, BURAI, HARAK, DRIKO, CHIANTA);
		addAttackId(RAGNA_ORC_OVERLORD, RAGNA_ORC_SEER, REVENANT_OF_TANTOS_CHIEF);
		addKillId(TYRANT, MARSH_STAKATO_DRONE, GUARDIAN_BASILISK, MANASHEN_GARGOYLE, TIMAK_ORC, TIMAK_ORC_ARCHER, TIMAK_ORC_SOLDIER, TIMAK_ORC_WARRIOR, TIMAK_ORC_SHAMAN, TIMAK_ORC_OVERLORD, TAMLIN_ORC, TAMLIN_ORC_ARCHER, RAGNA_ORC_OVERLORD, RAGNA_ORC_SEER, PASHIKA_SON_OF_VOLTAR, VULTUS_SON_OF_VOLTAR, ENKU_ORC_OVERLORD, MAKUM_BUGBEAR_THUG, REVENANT_OF_TANTOS_CHIEF);
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
			case "30514-05.htm":
			{
				st.startQuest();
				giveItems(player, VOKIAN_ORDER_1, 1);
				if (!player.getVariables().getBoolean("secondClassChange37", false))
				{
					htmltext = "30514-05a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_37.get(player.getRace().ordinal()));
					player.getVariables().set("secondClassChange37", true);
				}
				break;
			}
			case "30642-03.htm":
			{
				st.setCond(4, true);
				takeItems(player, VOKIAN_ORDER_2, 1);
				giveItems(player, CHIANTA_ORDER_1, 1);
				break;
			}
			case "30642-07.htm":
			{
				takeItems(player, CHIANTA_ORDER_1, 1);
				takeItems(player, KASMAN_LETTER_1, 1);
				takeItems(player, MANAKIA_LETTER_1, 1);
				takeItems(player, MANAKIA_LETTER_2, 1);
				takeItems(player, SCEPTER_OF_BREKA, 1);
				takeItems(player, SCEPTER_OF_ENKU, 1);
				takeItems(player, SCEPTER_OF_TUNATH, 1);
				takeItems(player, SCEPTER_OF_TUREK, 1);
				takeItems(player, SCEPTER_OF_VUKU, 1);
				if (player.getLevel() >= 37)
				{
					st.setCond(6, true);
					giveItems(player, CHIANTA_ORDER_3, 1);
				}
				else
				{
					htmltext = "30642-06.htm";
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					giveItems(player, CHIANTA_ORDER_2, 1);
				}
				break;
			}
			case "30501-02.htm":
			{
				if (!hasQuestItems(player, SCEPTER_OF_VUKU))
				{
					if (hasQuestItems(player, KASMAN_LETTER_1))
					{
						htmltext = "30501-04.htm";
					}
					else
					{
						htmltext = "30501-03.htm";
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, KASMAN_LETTER_1, 1);
					}
					
					addRadar(player, -2150, 124443, -3724);
				}
				break;
			}
			case "30501-05.htm":
			{
				if (!hasQuestItems(player, SCEPTER_OF_TUREK))
				{
					if (hasQuestItems(player, KASMAN_LETTER_2))
					{
						htmltext = "30501-07.htm";
					}
					else
					{
						htmltext = "30501-06.htm";
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, KASMAN_LETTER_2, 1);
					}
					
					addRadar(player, -94294, 110818, -3563);
				}
				break;
			}
			case "30501-08.htm":
			{
				if (!hasQuestItems(player, SCEPTER_OF_TUNATH))
				{
					if (hasQuestItems(player, KASMAN_LETTER_3))
					{
						htmltext = "30501-10.htm";
					}
					else
					{
						htmltext = "30501-09.htm";
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, KASMAN_LETTER_3, 1);
					}
					
					addRadar(player, -55217, 200628, -3724);
				}
				break;
			}
			case "30515-02.htm":
			{
				if (!hasQuestItems(player, SCEPTER_OF_BREKA))
				{
					if (hasQuestItems(player, MANAKIA_LETTER_1))
					{
						htmltext = "30515-04.htm";
					}
					else
					{
						htmltext = "30515-03.htm";
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, MANAKIA_LETTER_1, 1);
					}
					
					addRadar(player, 80100, 119991, -2264);
				}
				break;
			}
			case "30515-05.htm":
			{
				if (!hasQuestItems(player, SCEPTER_OF_ENKU))
				{
					if (hasQuestItems(player, MANAKIA_LETTER_2))
					{
						htmltext = "30515-07.htm";
					}
					else
					{
						htmltext = "30515-06.htm";
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						giveItems(player, MANAKIA_LETTER_2, 1);
					}
					
					addRadar(player, 19815, 189703, -3032);
				}
				break;
			}
			case "30615-04.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, MANAKIA_LETTER_1, 1);
				giveItems(player, GLOVE_OF_VOLTAR, 1);
				if (!_sonsOfVoltar)
				{
					addSpawn(PASHIKA_SON_OF_VOLTAR, 80117, 120039, -2259, 0, false, 200000);
					addSpawn(VULTUS_SON_OF_VOLTAR, 80058, 120038, -2259, 0, false, 200000);
					_sonsOfVoltar = true;
					
					// Resets Sons Of Voltar
					startQuestTimer("voltar_sons_cleanup", 201000, null, player, false);
				}
				break;
			}
			case "30616-05.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, MANAKIA_LETTER_2, 1);
				giveItems(player, GLOVE_OF_KEPRA, 1);
				if (!_enkuOrcOverlords)
				{
					addSpawn(ENKU_ORC_OVERLORD, 19894, 189743, -3074, 0, false, 200000);
					addSpawn(ENKU_ORC_OVERLORD, 19869, 189800, -3059, 0, false, 200000);
					addSpawn(ENKU_ORC_OVERLORD, 19818, 189818, -3047, 0, false, 200000);
					addSpawn(ENKU_ORC_OVERLORD, 19753, 189837, -3027, 0, false, 200000);
					_enkuOrcOverlords = true;
					
					// Resets Enku Orc Overlords
					startQuestTimer("enku_orcs_cleanup", 201000, null, player, false);
				}
				break;
			}
			case "30617-04.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, KASMAN_LETTER_2, 1);
				giveItems(player, GLOVE_OF_BURAI, 1);
				if (!_makumBugbearThugs)
				{
					addSpawn(MAKUM_BUGBEAR_THUG, -94292, 110781, -3701, 0, false, 200000);
					addSpawn(MAKUM_BUGBEAR_THUG, -94293, 110861, -3701, 0, false, 200000);
					_makumBugbearThugs = true;
					
					// Resets Makum Bugbear Thugs
					startQuestTimer("makum_bugbears_cleanup", 201000, null, player, false);
				}
				break;
			}
			case "30618-03.htm":
			{
				takeItems(player, KASMAN_LETTER_3, 1);
				giveItems(player, SCEPTER_OF_TUNATH, 1);
				if (hasQuestItems(player, SCEPTER_OF_BREKA, SCEPTER_OF_ENKU, SCEPTER_OF_VUKU, SCEPTER_OF_TUREK))
				{
					st.setCond(5, true);
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30619-03.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, KASMAN_LETTER_1, 1);
				giveItems(player, DRIKO_CONTRACT, 1);
				break;
			}
			case "30571-03.htm":
			{
				st.setCond(9, true);
				takeItems(player, SCEPTER_BOX, 1);
				giveItems(player, TANAPI_ORDER, 1);
				break;
			}
			case "voltar_sons_cleanup":
			{
				_sonsOfVoltar = false;
				return null;
			}
			case "enku_orcs_cleanup":
			{
				_enkuOrcOverlords = false;
				return null;
			}
			case "makum_bugbears_cleanup":
			{
				_makumBugbearThugs = false;
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
				if (player.getRace() != Race.ORC)
				{
					htmltext = "30514-01.htm";
				}
				else if (player.getLevel() < 37)
				{
					htmltext = "30514-02.htm";
				}
				else if (player.getPlayerClass().level() != 1)
				{
					htmltext = "30514-01a.htm";
				}
				else
				{
					htmltext = "30514-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case VOKIAN:
					{
						if (cond == 1)
						{
							htmltext = "30514-06.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30514-08.htm";
							st.setCond(3, true);
							takeItems(player, GUARDIAN_BASILISK_FANG, 10);
							takeItems(player, MANASHEN_SHARD, 10);
							takeItems(player, TYRANT_TALON, 10);
							takeItems(player, VOKIAN_ORDER_1, 1);
							giveItems(player, NECKLACE_OF_AUTHORITY, 1);
							giveItems(player, VOKIAN_ORDER_2, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30514-09.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30514-10.htm";
						}
						break;
					}
					case CHIANTA:
					{
						if (cond == 3)
						{
							htmltext = "30642-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30642-04.htm";
						}
						else if (cond == 5)
						{
							if (hasQuestItems(player, CHIANTA_ORDER_2))
							{
								if (player.getLevel() >= 37)
								{
									htmltext = "30642-09.htm";
									st.setCond(6, true);
									takeItems(player, CHIANTA_ORDER_2, 1);
									giveItems(player, CHIANTA_ORDER_3, 1);
								}
								else
								{
									htmltext = "30642-08.htm";
								}
							}
							else
							{
								htmltext = "30642-05.htm";
							}
						}
						else if (cond == 6)
						{
							htmltext = "30642-10.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30642-11.htm";
							st.setCond(8, true);
							takeItems(player, CHIANTA_ORDER_3, 1);
							takeItems(player, NECKLACE_OF_AUTHORITY, 1);
							takeItems(player, TAMLIN_ORC_SKULL, 20);
							takeItems(player, TIMAK_ORC_HEAD, 20);
							giveItems(player, SCEPTER_BOX, 1);
						}
						else if (cond == 8)
						{
							htmltext = "30642-12.htm";
						}
						else if (cond > 8)
						{
							htmltext = "30642-13.htm";
						}
						break;
					}
					case KASMAN:
					{
						if (hasQuestItems(player, CHIANTA_ORDER_1))
						{
							htmltext = "30501-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30501-11.htm";
						}
						break;
					}
					case MANAKIA:
					{
						if (hasQuestItems(player, CHIANTA_ORDER_1))
						{
							htmltext = "30515-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30515-08.htm";
						}
						break;
					}
					case VOLTAR:
					{
						if (cond > 3)
						{
							if (hasQuestItems(player, MANAKIA_LETTER_1))
							{
								htmltext = "30615-02.htm";
								removeRadar(player, 80100, 119991, -2264);
							}
							else if (hasQuestItems(player, GLOVE_OF_VOLTAR))
							{
								htmltext = "30615-05.htm";
								if (!_sonsOfVoltar)
								{
									addSpawn(PASHIKA_SON_OF_VOLTAR, 80117, 120039, -2259, 0, false, 200000);
									addSpawn(VULTUS_SON_OF_VOLTAR, 80058, 120038, -2259, 0, false, 200000);
									_sonsOfVoltar = true;
									
									// Resets Sons Of Voltar
									startQuestTimer("voltar_sons_cleanup", 201000, null, player, false);
								}
							}
							else if (hasQuestItems(player, PASHIKA_HEAD, VULTUS_HEAD))
							{
								htmltext = "30615-06.htm";
								takeItems(player, PASHIKA_HEAD, 1);
								takeItems(player, VULTUS_HEAD, 1);
								giveItems(player, SCEPTER_OF_BREKA, 1);
								
								if (hasQuestItems(player, SCEPTER_OF_ENKU, SCEPTER_OF_VUKU, SCEPTER_OF_TUREK, SCEPTER_OF_TUNATH))
								{
									st.setCond(5, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else if (hasQuestItems(player, SCEPTER_OF_BREKA))
							{
								htmltext = "30615-07.htm";
							}
							else if (hasQuestItems(player, CHIANTA_ORDER_1))
							{
								htmltext = "30615-01.htm";
							}
							else if (cond < 9)
							{
								htmltext = "30615-08.htm";
							}
						}
						break;
					}
					case KEPRA:
					{
						if (cond > 3)
						{
							if (hasQuestItems(player, MANAKIA_LETTER_2))
							{
								htmltext = "30616-02.htm";
								removeRadar(player, 19815, 189703, -3032);
							}
							else if (hasQuestItems(player, GLOVE_OF_KEPRA))
							{
								htmltext = "30616-05.htm";
								
								if (!_enkuOrcOverlords)
								{
									addSpawn(ENKU_ORC_OVERLORD, 19894, 189743, -3074, 0, false, 200000);
									addSpawn(ENKU_ORC_OVERLORD, 19869, 189800, -3059, 0, false, 200000);
									addSpawn(ENKU_ORC_OVERLORD, 19818, 189818, -3047, 0, false, 200000);
									addSpawn(ENKU_ORC_OVERLORD, 19753, 189837, -3027, 0, false, 200000);
									_enkuOrcOverlords = true;
									
									// Resets Enku Orc Overlords
									startQuestTimer("enku_orcs_cleanup", 201000, null, player, false);
								}
							}
							else if (getQuestItemsCount(player, ENKU_OVERLORD_HEAD) >= 4)
							{
								htmltext = "30616-06.htm";
								takeItems(player, ENKU_OVERLORD_HEAD, -1);
								giveItems(player, SCEPTER_OF_ENKU, 1);
								
								if (hasQuestItems(player, SCEPTER_OF_BREKA, SCEPTER_OF_VUKU, SCEPTER_OF_TUREK, SCEPTER_OF_TUNATH))
								{
									st.setCond(5, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else if (hasQuestItems(player, SCEPTER_OF_ENKU))
							{
								htmltext = "30616-07.htm";
							}
							else if (hasQuestItems(player, CHIANTA_ORDER_1))
							{
								htmltext = "30616-01.htm";
							}
							else if (cond < 9)
							{
								htmltext = "30616-08.htm";
							}
						}
						break;
					}
					case BURAI:
					{
						if (cond > 3)
						{
							if (hasQuestItems(player, KASMAN_LETTER_2))
							{
								htmltext = "30617-02.htm";
								removeRadar(player, -94294, 110818, -3563);
							}
							else if (hasQuestItems(player, GLOVE_OF_BURAI))
							{
								htmltext = "30617-04.htm";
								
								if (!_makumBugbearThugs)
								{
									addSpawn(MAKUM_BUGBEAR_THUG, -94292, 110781, -3701, 0, false, 200000);
									addSpawn(MAKUM_BUGBEAR_THUG, -94293, 110861, -3701, 0, false, 200000);
									_makumBugbearThugs = true;
									
									// Resets Makum Bugbear Thugs
									startQuestTimer("makum_bugbears_cleanup", 201000, null, player, false);
								}
							}
							else if (getQuestItemsCount(player, MAKUM_BUGBEAR_HEAD) == 2)
							{
								htmltext = "30617-05.htm";
								takeItems(player, MAKUM_BUGBEAR_HEAD, 2);
								giveItems(player, SCEPTER_OF_TUREK, 1);
								
								if (hasQuestItems(player, SCEPTER_OF_BREKA, SCEPTER_OF_VUKU, SCEPTER_OF_ENKU, SCEPTER_OF_TUNATH))
								{
									st.setCond(5, true);
								}
								else
								{
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								}
							}
							else if (hasQuestItems(player, SCEPTER_OF_TUREK))
							{
								htmltext = "30617-06.htm";
							}
							else if (hasQuestItems(player, CHIANTA_ORDER_1))
							{
								htmltext = "30617-01.htm";
							}
							else if (cond < 8)
							{
								htmltext = "30617-07.htm";
							}
						}
						break;
					}
					case HARAK:
					{
						if (cond > 3)
						{
							if (hasQuestItems(player, KASMAN_LETTER_3))
							{
								htmltext = "30618-02.htm";
								removeRadar(player, -55217, 200628, -3724);
							}
							else if (hasQuestItems(player, SCEPTER_OF_TUNATH))
							{
								htmltext = "30618-04.htm";
							}
							else if (hasQuestItems(player, CHIANTA_ORDER_1))
							{
								htmltext = "30618-01.htm";
							}
							else if (cond < 9)
							{
								htmltext = "30618-05.htm";
							}
						}
						break;
					}
					case DRIKO:
					{
						if (cond > 3)
						{
							if (hasQuestItems(player, KASMAN_LETTER_1))
							{
								htmltext = "30619-02.htm";
								removeRadar(player, -2150, 124443, -3724);
							}
							else if (hasQuestItems(player, DRIKO_CONTRACT))
							{
								if (getQuestItemsCount(player, STAKATO_DRONE_HUSK) == 30)
								{
									htmltext = "30619-05.htm";
									takeItems(player, DRIKO_CONTRACT, 1);
									takeItems(player, STAKATO_DRONE_HUSK, 30);
									giveItems(player, SCEPTER_OF_VUKU, 1);
									
									if (hasQuestItems(player, SCEPTER_OF_BREKA, SCEPTER_OF_TUREK, SCEPTER_OF_ENKU, SCEPTER_OF_TUNATH))
									{
										st.setCond(5, true);
									}
									else
									{
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									}
								}
								else
								{
									htmltext = "30619-04.htm";
								}
							}
							else if (hasQuestItems(player, SCEPTER_OF_VUKU))
							{
								htmltext = "30619-06.htm";
							}
							else if (hasQuestItems(player, CHIANTA_ORDER_1))
							{
								htmltext = "30619-01.htm";
							}
							else if (cond < 8)
							{
								htmltext = "30619-07.htm";
							}
						}
						break;
					}
					case TANAPI:
					{
						if (cond == 8)
						{
							htmltext = "30571-01.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30571-04.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30571-05.htm";
							st.setCond(11, true);
							takeItems(player, SCEPTER_OF_TANTOS, 1);
							takeItems(player, TANAPI_ORDER, 1);
							giveItems(player, RITUAL_BOX, 1);
						}
						else if (cond == 11)
						{
							htmltext = "30571-06.htm";
						}
						break;
					}
					case KAKAI:
					{
						if ((cond > 7) && (cond < 11))
						{
							htmltext = "30565-01.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30565-02.htm";
							takeItems(player, RITUAL_BOX, 1);
							giveItems(player, MARK_OF_GLORY, 1);
							addExpAndSp(player, 91457, 2500);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
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
			case RAGNA_ORC_OVERLORD:
			case RAGNA_ORC_SEER:
			{
				if (st.isCond(9) && npc.isScriptValue(0))
				{
					npc.broadcastSay(ChatType.GENERAL, "Is it a lackey of Kakai?!");
					npc.setScriptValue(1);
				}
				break;
			}
			case REVENANT_OF_TANTOS_CHIEF:
			{
				if (st.isCond(9))
				{
					if (npc.isScriptValue(0))
					{
						npc.broadcastSay(ChatType.GENERAL, "How regretful! Unjust dishonor!");
						npc.setScriptValue(1);
					}
					else if (npc.isScriptValue(1) && ((npc.getCurrentHp() / npc.getMaxHp()) < 0.33))
					{
						npc.broadcastSay(ChatType.GENERAL, "Indignant and unfair death!");
						npc.setScriptValue(2);
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
			case TYRANT:
			{
				if (st.isCond(1) && (getQuestItemsCount(player, TYRANT_TALON) < 10) && (getRandom(10) < 5))
				{
					giveItems(player, TYRANT_TALON, 1);
					if ((getQuestItemsCount(player, TYRANT_TALON) >= 10) && ((getQuestItemsCount(player, GUARDIAN_BASILISK_FANG) + getQuestItemsCount(player, MANASHEN_SHARD)) == 20))
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
			case GUARDIAN_BASILISK:
			{
				if (st.isCond(1) && (getQuestItemsCount(player, GUARDIAN_BASILISK_FANG) < 10) && (getRandom(10) < 5))
				{
					giveItems(player, GUARDIAN_BASILISK_FANG, 1);
					if ((getQuestItemsCount(player, GUARDIAN_BASILISK_FANG) >= 10) && ((getQuestItemsCount(player, TYRANT_TALON) + getQuestItemsCount(player, MANASHEN_SHARD)) == 20))
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
			case MANASHEN_GARGOYLE:
			{
				if (st.isCond(1) && (getQuestItemsCount(player, MANASHEN_SHARD) < 10) && (getRandom(100) < 75))
				{
					giveItems(player, MANASHEN_SHARD, 1);
					if ((getQuestItemsCount(player, MANASHEN_SHARD) >= 10) && ((getQuestItemsCount(player, TYRANT_TALON) + getQuestItemsCount(player, GUARDIAN_BASILISK_FANG)) == 20))
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
			case MARSH_STAKATO_DRONE:
			{
				if (hasQuestItems(player, DRIKO_CONTRACT) && (getQuestItemsCount(player, STAKATO_DRONE_HUSK) < 30) && (getRandom(100) < 75))
				{
					giveItems(player, STAKATO_DRONE_HUSK, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case PASHIKA_SON_OF_VOLTAR:
			{
				if (hasQuestItems(player, GLOVE_OF_VOLTAR) && !hasQuestItems(player, PASHIKA_HEAD))
				{
					giveItems(player, PASHIKA_HEAD, 1);
					if (hasQuestItems(player, VULTUS_HEAD))
					{
						takeItems(player, GLOVE_OF_VOLTAR, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case VULTUS_SON_OF_VOLTAR:
			{
				if (hasQuestItems(player, GLOVE_OF_VOLTAR) && !hasQuestItems(player, VULTUS_HEAD))
				{
					giveItems(player, VULTUS_HEAD, 1);
					if (hasQuestItems(player, PASHIKA_HEAD))
					{
						takeItems(player, GLOVE_OF_VOLTAR, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case ENKU_ORC_OVERLORD:
			{
				if (hasQuestItems(player, GLOVE_OF_KEPRA))
				{
					giveItems(player, ENKU_OVERLORD_HEAD, 1);
					if ((getQuestItemsCount(player, ENKU_OVERLORD_HEAD) >= 4))
					{
						takeItems(player, GLOVE_OF_KEPRA, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case MAKUM_BUGBEAR_THUG:
			{
				if (hasQuestItems(player, GLOVE_OF_BURAI))
				{
					giveItems(player, MAKUM_BUGBEAR_HEAD, 1);
					if ((getQuestItemsCount(player, MAKUM_BUGBEAR_HEAD) >= 2))
					{
						takeItems(player, GLOVE_OF_BURAI, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case TIMAK_ORC:
			case TIMAK_ORC_ARCHER:
			case TIMAK_ORC_SOLDIER:
			case TIMAK_ORC_WARRIOR:
			case TIMAK_ORC_SHAMAN:
			case TIMAK_ORC_OVERLORD:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, TIMAK_ORC_HEAD) < 20) && (getRandom(1000000) < (500000 + ((npc.getId() - 20583) * 100000))))
				{
					giveItems(player, TIMAK_ORC_HEAD, 1);
					if ((getQuestItemsCount(player, TIMAK_ORC_HEAD) >= 20) && (getQuestItemsCount(player, TAMLIN_ORC_SKULL) >= 20))
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
			case TAMLIN_ORC:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, TAMLIN_ORC_SKULL) < 20) && (getRandom(1000000) < 500000))
				{
					giveItems(player, TAMLIN_ORC_SKULL, 1);
					if ((getQuestItemsCount(player, TAMLIN_ORC_SKULL) >= 20) && (getQuestItemsCount(player, TIMAK_ORC_HEAD) >= 20))
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
			case TAMLIN_ORC_ARCHER:
			{
				if (st.isCond(6) && (getQuestItemsCount(player, TAMLIN_ORC_SKULL) < 20) && (getRandom(1000000) < 600000))
				{
					giveItems(player, TAMLIN_ORC_SKULL, 1);
					if ((getQuestItemsCount(player, TAMLIN_ORC_SKULL) >= 20) && (getQuestItemsCount(player, TIMAK_ORC_HEAD) >= 20))
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
			case RAGNA_ORC_OVERLORD:
			case RAGNA_ORC_SEER:
			{
				if (st.isCond(9))
				{
					npc.broadcastSay(ChatType.GENERAL, "Too late!");
					addSpawn(REVENANT_OF_TANTOS_CHIEF, npc, true, 200000);
				}
				break;
			}
			case REVENANT_OF_TANTOS_CHIEF:
			{
				if (st.isCond(9))
				{
					st.setCond(10, true);
					giveItems(player, SCEPTER_OF_TANTOS, 1);
					npc.broadcastSay(ChatType.GENERAL, "I'll get revenge someday!!");
				}
				break;
			}
		}
	}
}
