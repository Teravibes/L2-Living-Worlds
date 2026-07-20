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
package quests.Q00102_SeaOfSporesFever;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00102_SeaOfSporesFever extends Quest
{
	// NPCs
	private static final int ALBERIUS = 30284;
	private static final int COBENDELL = 30156;
	private static final int BERROS = 30217;
	private static final int VELTRESS = 30219;
	private static final int RAYEN = 30221;
	private static final int GARTRANDELL = 30285;
	
	// Items
	private static final int ALBERIUS_LETTER = 964;
	private static final int EVERGREEN_AMULET = 965;
	private static final int DRYAD_TEARS = 966;
	private static final int ALBERIUS_LIST = 746;
	private static final int COBENDELL_MEDICINE_1 = 1130;
	private static final int COBENDELL_MEDICINE_2 = 1131;
	private static final int COBENDELL_MEDICINE_3 = 1132;
	private static final int COBENDELL_MEDICINE_4 = 1133;
	private static final int COBENDELL_MEDICINE_5 = 1134;
	
	// Rewards
	private static final int SPIRITSHOT_NO_GRADE = 2509;
	private static final int SOULSHOT_NO_GRADE = 1835;
	private static final int SWORD_OF_SENTINEL = 743;
	private static final int STAFF_OF_SENTINEL = 744;
	private static final int LESSER_HEALING_POT = 1060;
	private static final int ECHO_BATTLE = 4412;
	private static final int ECHO_LOVE = 4413;
	private static final int ECHO_SOLITUDE = 4414;
	private static final int ECHO_FEAST = 4415;
	private static final int ECHO_CELEBRATION = 4416;
	
	// Newbie Rewards
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	
	public Q00102_SeaOfSporesFever()
	{
		super(102, "Sea of Spores Fever");
		registerQuestItems(ALBERIUS_LETTER, EVERGREEN_AMULET, DRYAD_TEARS, COBENDELL_MEDICINE_1, COBENDELL_MEDICINE_2, COBENDELL_MEDICINE_3, COBENDELL_MEDICINE_4, COBENDELL_MEDICINE_5, ALBERIUS_LIST);
		addStartNpc(ALBERIUS);
		addTalkId(ALBERIUS, COBENDELL, BERROS, RAYEN, GARTRANDELL, VELTRESS);
		addKillId(20013, 20019);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30284-02.htm"))
		{
			st.startQuest();
			giveItems(player, ALBERIUS_LETTER, 1);
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
				if (player.getRace() != Race.ELF)
				{
					htmltext = "30284-00.htm";
				}
				else if (player.getLevel() < 12)
				{
					htmltext = "30284-08.htm";
				}
				else
				{
					htmltext = "30284-07.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ALBERIUS:
					{
						if (cond == 1)
						{
							htmltext = "30284-03.htm";
						}
						else if ((cond == 2) || (cond == 3))
						{
							htmltext = "30284-09.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30284-04.htm";
							st.setCond(5, true);
							st.set("medicines", "4");
							takeItems(player, COBENDELL_MEDICINE_1, 1);
							giveItems(player, ALBERIUS_LIST, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30284-05.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30284-06.htm";
							takeItems(player, ALBERIUS_LIST, 1);
							
							if (player.isMageClass())
							{
								giveItems(player, STAFF_OF_SENTINEL, 1);
								rewardItems(player, SPIRITSHOT_NO_GRADE, 500);
							}
							else
							{
								giveItems(player, SWORD_OF_SENTINEL, 1);
								rewardItems(player, SOULSHOT_NO_GRADE, 1000);
							}
							
							// Give newbie reward if player is eligible
							if (player.isNewbie())
							{
								int newPlayerRewardsReceived = player.getVariables().getInt(PlayerVariables.NEWBIE_SHOTS_RECEIVED, 0);
								if (newPlayerRewardsReceived < 2)
								{
									st.showQuestionMark(26);
									if (player.isMageClass())
									{
										st.playTutorialVoice("tutorial_voice_027");
										giveItems(player, SPIRITSHOT_FOR_BEGINNERS, 3000);
										player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
									}
									else
									{
										st.playTutorialVoice("tutorial_voice_026");
										giveItems(player, SOULSHOT_FOR_BEGINNERS, 7000);
										player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
									}
								}
							}
							
							giveItems(player, LESSER_HEALING_POT, 100);
							giveItems(player, ECHO_BATTLE, 10);
							giveItems(player, ECHO_LOVE, 10);
							giveItems(player, ECHO_SOLITUDE, 10);
							giveItems(player, ECHO_FEAST, 10);
							giveItems(player, ECHO_CELEBRATION, 10);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case COBENDELL:
					{
						if (cond == 1)
						{
							htmltext = "30156-03.htm";
							st.setCond(2, true);
							takeItems(player, ALBERIUS_LETTER, 1);
							giveItems(player, EVERGREEN_AMULET, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30156-04.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30156-07.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30156-05.htm";
							st.setCond(4, true);
							takeItems(player, DRYAD_TEARS, -1);
							takeItems(player, EVERGREEN_AMULET, 1);
							giveItems(player, COBENDELL_MEDICINE_1, 1);
							giveItems(player, COBENDELL_MEDICINE_2, 1);
							giveItems(player, COBENDELL_MEDICINE_3, 1);
							giveItems(player, COBENDELL_MEDICINE_4, 1);
							giveItems(player, COBENDELL_MEDICINE_5, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30156-06.htm";
						}
						break;
					}
					case BERROS:
					{
						if (cond == 5)
						{
							htmltext = "30217-01.htm";
							checkItem(st, COBENDELL_MEDICINE_2);
						}
						break;
					}
					case VELTRESS:
					{
						if (cond == 5)
						{
							htmltext = "30219-01.htm";
							checkItem(st, COBENDELL_MEDICINE_3);
						}
						break;
					}
					case RAYEN:
					{
						if (cond == 5)
						{
							htmltext = "30221-01.htm";
							checkItem(st, COBENDELL_MEDICINE_4);
						}
						break;
					}
					case GARTRANDELL:
					{
						if (cond == 5)
						{
							htmltext = "30285-01.htm";
							checkItem(st, COBENDELL_MEDICINE_5);
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(2) && (getRandom(10) < 3))
		{
			giveItems(killer, DRYAD_TEARS, 1);
			if (getQuestItemsCount(killer, DRYAD_TEARS) < 10)
			{
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				qs.setCond(3, true);
			}
		}
	}
	
	private void checkItem(QuestState st, int itemId)
	{
		if (hasQuestItems(st.getPlayer(), itemId))
		{
			takeItems(st.getPlayer(), itemId, 1);
			
			final int medicinesLeft = st.getInt("medicines") - 1;
			if (medicinesLeft == 0)
			{
				st.setCond(6, true);
			}
			else
			{
				st.set("medicines", String.valueOf(medicinesLeft));
			}
		}
	}
}
