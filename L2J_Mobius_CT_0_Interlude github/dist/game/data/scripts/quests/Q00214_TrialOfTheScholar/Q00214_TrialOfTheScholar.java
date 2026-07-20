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
package quests.Q00214_TrialOfTheScholar;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00214_TrialOfTheScholar extends Quest
{
	// NPCs
	private static final int SYLVAIN = 30070;
	private static final int LUCAS = 30071;
	private static final int VALKON = 30103;
	private static final int DIETER = 30111;
	private static final int JUREK = 30115;
	private static final int EDROC = 30230;
	private static final int RAUT = 30316;
	private static final int POITAN = 30458;
	private static final int MIRIEN = 30461;
	private static final int MARIA = 30608;
	private static final int CRETA = 30609;
	private static final int CRONOS = 30610;
	private static final int TRIFF = 30611;
	private static final int CASIAN = 30612;
	
	// Monsters
	private static final int MONSTER_EYE_DESTROYER = 20068;
	private static final int MEDUSA = 20158;
	private static final int GHOUL = 20201;
	private static final int SHACKLE_1 = 20235;
	private static final int SHACKLE_2 = 20279;
	private static final int BREKA_ORC_SHAMAN = 20269;
	private static final int FETTERED_SOUL = 20552;
	private static final int GRANDIS = 20554;
	private static final int ENCHANTED_GARGOYLE = 20567;
	private static final int LETO_LIZARDMAN_WARRIOR = 20580;
	
	// Items
	private static final int MIRIEN_SIGIL_1 = 2675;
	private static final int MIRIEN_SIGIL_2 = 2676;
	private static final int MIRIEN_SIGIL_3 = 2677;
	private static final int MIRIEN_INSTRUCTION = 2678;
	private static final int MARIA_LETTER_1 = 2679;
	private static final int MARIA_LETTER_2 = 2680;
	private static final int LUCAS_LETTER = 2681;
	private static final int LUCILLA_HANDBAG = 2682;
	private static final int CRETA_LETTER_1 = 2683;
	private static final int CRETA_PAINTING_1 = 2684;
	private static final int CRETA_PAINTING_2 = 2685;
	private static final int CRETA_PAINTING_3 = 2686;
	private static final int BROWN_SCROLL_SCRAP = 2687;
	private static final int CRYSTAL_OF_PURITY_1 = 2688;
	private static final int HIGH_PRIEST_SIGIL = 2689;
	private static final int GRAND_MAGISTER_SIGIL = 2690;
	private static final int CRONOS_SIGIL = 2691;
	private static final int SYLVAIN_LETTER = 2692;
	private static final int SYMBOL_OF_SYLVAIN = 2693;
	private static final int JUREK_LIST = 2694;
	private static final int MONSTER_EYE_DESTROYER_SKIN = 2695;
	private static final int SHAMAN_NECKLACE = 2696;
	private static final int SHACKLE_SCALP = 2697;
	private static final int SYMBOL_OF_JUREK = 2698;
	private static final int CRONOS_LETTER = 2699;
	private static final int DIETER_KEY = 2700;
	private static final int CRETA_LETTER_2 = 2701;
	private static final int DIETER_LETTER = 2702;
	private static final int DIETER_DIARY = 2703;
	private static final int RAUT_LETTER_ENVELOPE = 2704;
	private static final int TRIFF_RING = 2705;
	private static final int SCRIPTURE_CHAPTER_1 = 2706;
	private static final int SCRIPTURE_CHAPTER_2 = 2707;
	private static final int SCRIPTURE_CHAPTER_3 = 2708;
	private static final int SCRIPTURE_CHAPTER_4 = 2709;
	private static final int VALKON_REQUEST = 2710;
	private static final int POITAN_NOTES = 2711;
	private static final int STRONG_LIQUOR = 2713;
	private static final int CRYSTAL_OF_PURITY_2 = 2714;
	private static final int CASIAN_LIST = 2715;
	private static final int GHOUL_SKIN = 2716;
	private static final int MEDUSA_BLOOD = 2717;
	private static final int FETTERED_SOUL_ICHOR = 2718;
	private static final int ENCHANTED_GARGOYLE_NAIL = 2719;
	private static final int SYMBOL_OF_CRONOS = 2720;
	
	// Rewards
	private static final int MARK_OF_SCHOLAR = 2674;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00214_TrialOfTheScholar()
	{
		super(214, "Trial of the Scholar");
		registerQuestItems(MIRIEN_SIGIL_1, MIRIEN_SIGIL_2, MIRIEN_SIGIL_3, MIRIEN_INSTRUCTION, MARIA_LETTER_1, MARIA_LETTER_2, LUCAS_LETTER, LUCILLA_HANDBAG, CRETA_LETTER_1, CRETA_PAINTING_1, CRETA_PAINTING_2, CRETA_PAINTING_3, BROWN_SCROLL_SCRAP, CRYSTAL_OF_PURITY_1, HIGH_PRIEST_SIGIL, GRAND_MAGISTER_SIGIL, CRONOS_SIGIL, SYLVAIN_LETTER, SYMBOL_OF_SYLVAIN, JUREK_LIST, MONSTER_EYE_DESTROYER_SKIN, SHAMAN_NECKLACE, SHACKLE_SCALP, SYMBOL_OF_JUREK, CRONOS_LETTER, DIETER_KEY, CRETA_LETTER_2, DIETER_LETTER, DIETER_DIARY, RAUT_LETTER_ENVELOPE, TRIFF_RING, SCRIPTURE_CHAPTER_1, SCRIPTURE_CHAPTER_2, SCRIPTURE_CHAPTER_3, SCRIPTURE_CHAPTER_4, VALKON_REQUEST, POITAN_NOTES, STRONG_LIQUOR, CRYSTAL_OF_PURITY_2, CASIAN_LIST, GHOUL_SKIN, MEDUSA_BLOOD, FETTERED_SOUL_ICHOR, ENCHANTED_GARGOYLE_NAIL, SYMBOL_OF_CRONOS);
		addStartNpc(MIRIEN);
		addTalkId(MIRIEN, SYLVAIN, LUCAS, VALKON, DIETER, JUREK, EDROC, RAUT, POITAN, MARIA, CRETA, CRONOS, TRIFF, CASIAN);
		addKillId(MONSTER_EYE_DESTROYER, MEDUSA, GHOUL, SHACKLE_1, SHACKLE_2, BREKA_ORC_SHAMAN, FETTERED_SOUL, GRANDIS, ENCHANTED_GARGOYLE, LETO_LIZARDMAN_WARRIOR);
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
			case "30461-04.htm":
			{
				st.startQuest();
				giveItems(player, MIRIEN_SIGIL_1, 1);
				if (!player.getVariables().getBoolean("secondClassChange35", false))
				{
					htmltext = "30461-04a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_35.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange35", true);
				}
				break;
			}
			case "30461-09.htm":
			{
				if (player.getLevel() < 36)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					giveItems(player, MIRIEN_INSTRUCTION, 1);
				}
				else
				{
					htmltext = "30461-10.htm";
					st.setCond(19, true);
					takeItems(player, MIRIEN_SIGIL_2, 1);
					takeItems(player, SYMBOL_OF_JUREK, 1);
					giveItems(player, MIRIEN_SIGIL_3, 1);
				}
				break;
			}
			case "30070-02.htm":
			{
				st.setCond(2, true);
				giveItems(player, HIGH_PRIEST_SIGIL, 1);
				giveItems(player, SYLVAIN_LETTER, 1);
				break;
			}
			case "30608-02.htm":
			{
				st.setCond(3, true);
				takeItems(player, SYLVAIN_LETTER, 1);
				giveItems(player, MARIA_LETTER_1, 1);
				break;
			}
			case "30608-08.htm":
			{
				st.setCond(7, true);
				takeItems(player, CRETA_LETTER_1, 1);
				giveItems(player, LUCILLA_HANDBAG, 1);
				break;
			}
			case "30608-14.htm":
			{
				st.setCond(13, true);
				takeItems(player, BROWN_SCROLL_SCRAP, -1);
				takeItems(player, CRETA_PAINTING_3, 1);
				giveItems(player, CRYSTAL_OF_PURITY_1, 1);
				break;
			}
			case "30115-03.htm":
			{
				st.setCond(16, true);
				giveItems(player, GRAND_MAGISTER_SIGIL, 1);
				giveItems(player, JUREK_LIST, 1);
				break;
			}
			case "30071-04.htm":
			{
				st.setCond(10, true);
				takeItems(player, CRETA_PAINTING_2, 1);
				giveItems(player, CRETA_PAINTING_3, 1);
				break;
			}
			case "30609-05.htm":
			{
				st.setCond(6, true);
				takeItems(player, MARIA_LETTER_2, 1);
				giveItems(player, CRETA_LETTER_1, 1);
				break;
			}
			case "30609-09.htm":
			{
				st.setCond(8, true);
				takeItems(player, LUCILLA_HANDBAG, 1);
				giveItems(player, CRETA_PAINTING_1, 1);
				break;
			}
			case "30609-14.htm":
			{
				st.setCond(22, true);
				takeItems(player, DIETER_KEY, 1);
				giveItems(player, CRETA_LETTER_2, 1);
				break;
			}
			case "30610-10.htm":
			{
				st.setCond(20, true);
				giveItems(player, CRONOS_LETTER, 1);
				giveItems(player, CRONOS_SIGIL, 1);
				break;
			}
			case "30610-14.htm":
			{
				st.setCond(31, true);
				takeItems(player, CRONOS_SIGIL, 1);
				takeItems(player, DIETER_DIARY, 1);
				takeItems(player, SCRIPTURE_CHAPTER_1, 1);
				takeItems(player, SCRIPTURE_CHAPTER_2, 1);
				takeItems(player, SCRIPTURE_CHAPTER_3, 1);
				takeItems(player, SCRIPTURE_CHAPTER_4, 1);
				takeItems(player, TRIFF_RING, 1);
				giveItems(player, SYMBOL_OF_CRONOS, 1);
				break;
			}
			case "30111-05.htm":
			{
				st.setCond(21, true);
				takeItems(player, CRONOS_LETTER, 1);
				giveItems(player, DIETER_KEY, 1);
				break;
			}
			case "30111-09.htm":
			{
				st.setCond(23, true);
				takeItems(player, CRETA_LETTER_2, 1);
				giveItems(player, DIETER_DIARY, 1);
				giveItems(player, DIETER_LETTER, 1);
				break;
			}
			case "30230-02.htm":
			{
				st.setCond(24, true);
				takeItems(player, DIETER_LETTER, 1);
				giveItems(player, RAUT_LETTER_ENVELOPE, 1);
				break;
			}
			case "30316-02.htm":
			{
				st.setCond(25, true);
				takeItems(player, RAUT_LETTER_ENVELOPE, 1);
				giveItems(player, SCRIPTURE_CHAPTER_1, 1);
				giveItems(player, STRONG_LIQUOR, 1);
				break;
			}
			case "30611-04.htm":
			{
				st.setCond(26, true);
				takeItems(player, STRONG_LIQUOR, 1);
				giveItems(player, TRIFF_RING, 1);
				break;
			}
			case "30103-04.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, VALKON_REQUEST, 1);
				break;
			}
			case "30612-04.htm":
			{
				st.setCond(28, true);
				giveItems(player, CASIAN_LIST, 1);
				break;
			}
			case "30612-07.htm":
			{
				st.setCond(30, true);
				takeItems(player, CASIAN_LIST, 1);
				takeItems(player, ENCHANTED_GARGOYLE_NAIL, -1);
				takeItems(player, FETTERED_SOUL_ICHOR, -1);
				takeItems(player, GHOUL_SKIN, -1);
				takeItems(player, MEDUSA_BLOOD, -1);
				takeItems(player, POITAN_NOTES, 1);
				giveItems(player, SCRIPTURE_CHAPTER_4, 1);
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
				if ((player.getPlayerClass() != PlayerClass.WIZARD) && (player.getPlayerClass() != PlayerClass.ELVEN_WIZARD) && (player.getPlayerClass() != PlayerClass.DARK_WIZARD))
				{
					htmltext = "30461-01.htm";
				}
				else if (player.getLevel() < 35)
				{
					htmltext = "30461-02.htm";
				}
				else
				{
					htmltext = "30461-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MIRIEN:
					{
						if (cond < 14)
						{
							htmltext = "30461-05.htm";
						}
						else if (cond == 14)
						{
							htmltext = "30461-06.htm";
							st.setCond(15, true);
							takeItems(player, MIRIEN_SIGIL_1, 1);
							takeItems(player, SYMBOL_OF_SYLVAIN, 1);
							giveItems(player, MIRIEN_SIGIL_2, 1);
						}
						else if ((cond > 14) && (cond < 18))
						{
							htmltext = "30461-07.htm";
						}
						else if (cond == 18)
						{
							if (!hasQuestItems(player, MIRIEN_INSTRUCTION))
							{
								htmltext = "30461-08.htm";
							}
							else
							{
								if (player.getLevel() < 36)
								{
									htmltext = "30461-11.htm";
								}
								else
								{
									htmltext = "30461-12.htm";
									st.setCond(19, true);
									takeItems(player, MIRIEN_INSTRUCTION, 1);
									takeItems(player, MIRIEN_SIGIL_2, 1);
									takeItems(player, SYMBOL_OF_JUREK, 1);
									giveItems(player, MIRIEN_SIGIL_3, 1);
								}
							}
						}
						else if ((cond > 18) && (cond < 31))
						{
							htmltext = "30461-13.htm";
						}
						else if (cond == 31)
						{
							htmltext = "30461-14.htm";
							takeItems(player, MIRIEN_SIGIL_3, 1);
							takeItems(player, SYMBOL_OF_CRONOS, 1);
							giveItems(player, MARK_OF_SCHOLAR, 1);
							addExpAndSp(player, 80265, 30000);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case SYLVAIN:
					{
						if (cond == 1)
						{
							htmltext = "30070-01.htm";
						}
						else if (cond < 13)
						{
							htmltext = "30070-03.htm";
						}
						else if (cond == 13)
						{
							htmltext = "30070-04.htm";
							st.setCond(14, true);
							takeItems(player, CRYSTAL_OF_PURITY_1, 1);
							takeItems(player, HIGH_PRIEST_SIGIL, 1);
							giveItems(player, SYMBOL_OF_SYLVAIN, 1);
						}
						else if (cond == 14)
						{
							htmltext = "30070-05.htm";
						}
						else if (cond > 14)
						{
							htmltext = "30070-06.htm";
						}
						break;
					}
					case MARIA:
					{
						if (cond == 2)
						{
							htmltext = "30608-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30608-03.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30608-04.htm";
							st.setCond(5, true);
							takeItems(player, LUCAS_LETTER, 1);
							giveItems(player, MARIA_LETTER_2, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30608-05.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30608-06.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30608-09.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30608-10.htm";
							st.setCond(9, true);
							takeItems(player, CRETA_PAINTING_1, 1);
							giveItems(player, CRETA_PAINTING_2, 1);
						}
						else if (cond == 9)
						{
							htmltext = "30608-11.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30608-12.htm";
							st.setCond(11, true);
						}
						else if (cond == 11)
						{
							htmltext = "30608-12.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30608-13.htm";
						}
						else if (cond == 13)
						{
							htmltext = "30608-15.htm";
						}
						else if (hasAtLeastOneQuestItem(player, SYMBOL_OF_SYLVAIN, MIRIEN_SIGIL_2))
						{
							htmltext = "30608-16.htm";
						}
						else if (cond > 18)
						{
							if (!hasQuestItems(player, VALKON_REQUEST))
							{
								htmltext = "30608-17.htm";
							}
							else
							{
								htmltext = "30608-18.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								takeItems(player, VALKON_REQUEST, 1);
								giveItems(player, CRYSTAL_OF_PURITY_2, 1);
							}
						}
						break;
					}
					case JUREK:
					{
						if (cond == 15)
						{
							htmltext = "30115-01.htm";
						}
						else if (cond == 16)
						{
							htmltext = "30115-04.htm";
						}
						else if (cond == 17)
						{
							htmltext = "30115-05.htm";
							st.setCond(18, true);
							takeItems(player, GRAND_MAGISTER_SIGIL, 1);
							takeItems(player, JUREK_LIST, 1);
							takeItems(player, MONSTER_EYE_DESTROYER_SKIN, -1);
							takeItems(player, SHACKLE_SCALP, -1);
							takeItems(player, SHAMAN_NECKLACE, -1);
							giveItems(player, SYMBOL_OF_JUREK, 1);
						}
						else if (cond == 18)
						{
							htmltext = "30115-06.htm";
						}
						else if (cond > 18)
						{
							htmltext = "30115-07.htm";
						}
						break;
					}
					case LUCAS:
					{
						if (cond == 3)
						{
							htmltext = "30071-01.htm";
							st.setCond(4, true);
							takeItems(player, MARIA_LETTER_1, 1);
							giveItems(player, LUCAS_LETTER, 1);
						}
						else if ((cond > 3) && (cond < 9))
						{
							htmltext = "30071-02.htm";
						}
						else if (cond == 9)
						{
							htmltext = "30071-03.htm";
						}
						else if ((cond == 10) || (cond == 11))
						{
							htmltext = "30071-05.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30071-06.htm";
						}
						else if (cond > 12)
						{
							htmltext = "30071-07.htm";
						}
						break;
					}
					case CRETA:
					{
						if (cond == 5)
						{
							htmltext = "30609-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30609-06.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30609-07.htm";
						}
						else if ((cond > 7) && (cond < 13))
						{
							htmltext = "30609-10.htm";
						}
						else if ((cond >= 13) && (cond < 19))
						{
							htmltext = "30609-11.htm";
						}
						else if (cond == 21)
						{
							htmltext = "30609-12.htm";
						}
						else if (cond > 21)
						{
							htmltext = "30609-15.htm";
						}
						break;
					}
					case CRONOS:
					{
						if (cond == 19)
						{
							htmltext = "30610-01.htm";
						}
						else if ((cond > 19) && (cond < 30))
						{
							htmltext = "30610-11.htm";
						}
						else if (cond == 30)
						{
							htmltext = "30610-12.htm";
						}
						else if (cond == 31)
						{
							htmltext = "30610-15.htm";
						}
						break;
					}
					case DIETER:
					{
						if (cond == 20)
						{
							htmltext = "30111-01.htm";
						}
						else if (cond == 21)
						{
							htmltext = "30111-06.htm";
						}
						else if (cond == 22)
						{
							htmltext = "30111-07.htm";
						}
						else if (cond == 23)
						{
							htmltext = "30111-10.htm";
						}
						else if (cond == 24)
						{
							htmltext = "30111-11.htm";
						}
						else if ((cond > 24) && (cond < 31))
						{
							htmltext = (!hasQuestItems(player, SCRIPTURE_CHAPTER_1, SCRIPTURE_CHAPTER_2, SCRIPTURE_CHAPTER_3, SCRIPTURE_CHAPTER_4)) ? "30111-12.htm" : "30111-13.htm";
						}
						else if (cond == 31)
						{
							htmltext = "30111-15.htm";
						}
						break;
					}
					case EDROC:
					{
						if (cond == 23)
						{
							htmltext = "30230-01.htm";
						}
						else if (cond == 24)
						{
							htmltext = "30230-03.htm";
						}
						else if (cond > 24)
						{
							htmltext = "30230-04.htm";
						}
						break;
					}
					case RAUT:
					{
						if (cond == 24)
						{
							htmltext = "30316-01.htm";
						}
						else if (cond == 25)
						{
							htmltext = "30316-04.htm";
						}
						else if (cond > 25)
						{
							htmltext = "30316-05.htm";
						}
						break;
					}
					case TRIFF:
					{
						if (cond == 25)
						{
							htmltext = "30611-01.htm";
						}
						else if (cond > 25)
						{
							htmltext = "30611-05.htm";
						}
						break;
					}
					case VALKON:
					{
						if (hasQuestItems(player, TRIFF_RING))
						{
							if (!hasQuestItems(player, SCRIPTURE_CHAPTER_2))
							{
								if (!hasQuestItems(player, VALKON_REQUEST))
								{
									if (!hasQuestItems(player, CRYSTAL_OF_PURITY_2))
									{
										htmltext = "30103-01.htm";
									}
									else
									{
										htmltext = "30103-06.htm";
										playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
										takeItems(player, CRYSTAL_OF_PURITY_2, 1);
										giveItems(player, SCRIPTURE_CHAPTER_2, 1);
									}
								}
								else
								{
									htmltext = "30103-05.htm";
								}
							}
							else
							{
								htmltext = "30103-07.htm";
							}
						}
						break;
					}
					case POITAN:
					{
						if ((cond == 26) || (cond == 27))
						{
							if (!hasQuestItems(player, POITAN_NOTES))
							{
								htmltext = "30458-01.htm";
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								giveItems(player, POITAN_NOTES, 1);
							}
							else
							{
								htmltext = "30458-02.htm";
							}
						}
						else if ((cond == 28) || (cond == 29))
						{
							htmltext = "30458-03.htm";
						}
						else if (cond == 30)
						{
							htmltext = "30458-04.htm";
						}
						break;
					}
					case CASIAN:
					{
						if (((cond == 26) || (cond == 27)) && hasQuestItems(player, POITAN_NOTES))
						{
							if (hasQuestItems(player, SCRIPTURE_CHAPTER_1, SCRIPTURE_CHAPTER_2, SCRIPTURE_CHAPTER_3))
							{
								htmltext = "30612-02.htm";
							}
							else
							{
								htmltext = "30612-01.htm";
								if (cond == 26)
								{
									st.setCond(27, true);
								}
							}
						}
						else if (cond == 28)
						{
							htmltext = "30612-05.htm";
						}
						else if (cond == 29)
						{
							htmltext = "30612-06.htm";
						}
						else if (cond == 30)
						{
							htmltext = "30612-08.htm";
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
			case LETO_LIZARDMAN_WARRIOR:
			{
				if (st.isCond(11) && getRandomBoolean())
				{
					giveItems(player, BROWN_SCROLL_SCRAP, 1);
					if (getQuestItemsCount(player, BROWN_SCROLL_SCRAP) >= 5)
					{
						st.setCond(12, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case SHACKLE_1:
			case SHACKLE_2:
			{
				if (st.isCond(16) && (getQuestItemsCount(player, SHACKLE_SCALP) < 2) && getRandomBoolean())
				{
					giveItems(player, SHACKLE_SCALP, 1);
					if ((getQuestItemsCount(player, SHACKLE_SCALP) >= 2) && (getQuestItemsCount(player, MONSTER_EYE_DESTROYER_SKIN) == 5) && (getQuestItemsCount(player, SHAMAN_NECKLACE) == 5))
					{
						st.setCond(17, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case MONSTER_EYE_DESTROYER:
			{
				if (st.isCond(16) && (getQuestItemsCount(player, MONSTER_EYE_DESTROYER_SKIN) < 5) && getRandomBoolean())
				{
					giveItems(player, MONSTER_EYE_DESTROYER_SKIN, 1);
					if ((getQuestItemsCount(player, MONSTER_EYE_DESTROYER_SKIN) >= 5) && (getQuestItemsCount(player, SHACKLE_SCALP) == 2) && (getQuestItemsCount(player, SHAMAN_NECKLACE) == 5))
					{
						st.setCond(17, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case BREKA_ORC_SHAMAN:
			{
				if (st.isCond(16) && (getQuestItemsCount(player, SHAMAN_NECKLACE) < 5) && getRandomBoolean())
				{
					giveItems(player, SHAMAN_NECKLACE, 1);
					if ((getQuestItemsCount(player, SHAMAN_NECKLACE) >= 5) && (getQuestItemsCount(player, SHACKLE_SCALP) == 2) && (getQuestItemsCount(player, MONSTER_EYE_DESTROYER_SKIN) == 5))
					{
						st.setCond(17, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
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
				if (hasQuestItems(player, TRIFF_RING) && !hasQuestItems(player, SCRIPTURE_CHAPTER_3) && (getRandom(100) < 30))
				{
					giveItems(player, SCRIPTURE_CHAPTER_3, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case GHOUL:
			{
				if (st.isCond(28) && (getQuestItemsCount(player, GHOUL_SKIN) < 10))
				{
					giveItems(player, GHOUL_SKIN, 1);
					if ((getQuestItemsCount(player, GHOUL_SKIN) >= 10) && (getQuestItemsCount(player, MEDUSA_BLOOD) == 12) && (getQuestItemsCount(player, FETTERED_SOUL_ICHOR) == 5) && (getQuestItemsCount(player, ENCHANTED_GARGOYLE_NAIL) == 5))
					{
						st.setCond(29, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
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
				if (st.isCond(28) && (getQuestItemsCount(player, MEDUSA_BLOOD) < 12))
				{
					giveItems(player, MEDUSA_BLOOD, 1);
					if ((getQuestItemsCount(player, MEDUSA_BLOOD) >= 12) && (getQuestItemsCount(player, GHOUL_SKIN) == 10) && (getQuestItemsCount(player, FETTERED_SOUL_ICHOR) == 5) && (getQuestItemsCount(player, ENCHANTED_GARGOYLE_NAIL) == 5))
					{
						st.setCond(29, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
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
				if (st.isCond(28) && (getQuestItemsCount(player, FETTERED_SOUL_ICHOR) < 5))
				{
					giveItems(player, FETTERED_SOUL_ICHOR, 1);
					if ((getQuestItemsCount(player, FETTERED_SOUL_ICHOR) >= 5) && (getQuestItemsCount(player, MEDUSA_BLOOD) == 12) && (getQuestItemsCount(player, GHOUL_SKIN) == 10) && (getQuestItemsCount(player, ENCHANTED_GARGOYLE_NAIL) == 5))
					{
						st.setCond(29, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case ENCHANTED_GARGOYLE:
			{
				if (st.isCond(28) && (getQuestItemsCount(player, ENCHANTED_GARGOYLE_NAIL) < 5))
				{
					giveItems(player, ENCHANTED_GARGOYLE_NAIL, 1);
					if ((getQuestItemsCount(player, ENCHANTED_GARGOYLE_NAIL) >= 5) && (getQuestItemsCount(player, MEDUSA_BLOOD) == 12) && (getQuestItemsCount(player, GHOUL_SKIN) == 10) && (getQuestItemsCount(player, FETTERED_SOUL_ICHOR) == 5))
					{
						st.setCond(29, true);
						playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
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
