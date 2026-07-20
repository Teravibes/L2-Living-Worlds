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
package quests.Q00105_SkirmishWithOrcs;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00105_SkirmishWithOrcs extends Quest
{
	// Monster
	private static final int KABOO_CHIEF_UOPH = 27059;
	private static final int KABOO_CHIEF_KRACHA = 27060;
	private static final int KABOO_CHIEF_BATOH = 27061;
	private static final int KABOO_CHIEF_TANUKIA = 27062;
	private static final int KABOO_CHIEF_TUREL = 27064;
	private static final int KABOO_CHIEF_ROKO = 27065;
	private static final int KABOO_CHIEF_KAMUT = 27067;
	private static final int KABOO_CHIEF_MURTIKA = 27068;
	
	// Item
	private static final int KENDELL_ORDER_1 = 1836;
	private static final int KENDELL_ORDER_2 = 1837;
	private static final int KENDELL_ORDER_3 = 1838;
	private static final int KENDELL_ORDER_4 = 1839;
	private static final int KENDELL_ORDER_5 = 1840;
	private static final int KENDELL_ORDER_6 = 1841;
	private static final int KENDELL_ORDER_7 = 1842;
	private static final int KENDELL_ORDER_8 = 1843;
	private static final int KABOO_CHIEF_TORC_1 = 1844;
	private static final int KABOO_CHIEF_TORC_2 = 1845;
	
	// Rewards
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	private static final int RED_SUNSET_STAFF = 754;
	private static final int RED_SUNSET_SWORD = 981;
	private static final int LESSER_HEALING_POT = 1060;
	private static final int ECHO_BATTLE = 4412;
	private static final int ECHO_LOVE = 4413;
	private static final int ECHO_SOLITUDE = 4414;
	private static final int ECHO_FEAST = 4415;
	private static final int ECHO_CELEBRATION = 4416;
	
	public Q00105_SkirmishWithOrcs()
	{
		super(105, "Skirmish with the Orcs");
		registerQuestItems(KENDELL_ORDER_1, KENDELL_ORDER_2, KENDELL_ORDER_3, KENDELL_ORDER_4, KENDELL_ORDER_5, KENDELL_ORDER_6, KENDELL_ORDER_7, KENDELL_ORDER_8, KABOO_CHIEF_TORC_1, KABOO_CHIEF_TORC_2);
		addStartNpc(30218); // Kendell
		addTalkId(30218);
		addKillId(KABOO_CHIEF_UOPH, KABOO_CHIEF_KRACHA, KABOO_CHIEF_BATOH, KABOO_CHIEF_TANUKIA, KABOO_CHIEF_TUREL, KABOO_CHIEF_ROKO, KABOO_CHIEF_KAMUT, KABOO_CHIEF_MURTIKA);
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
		
		if (event.equals("30218-03.htm"))
		{
			st.startQuest();
			giveItems(player, getRandom(1836, 1839), 1); // Kendell's orders 1 to 4.
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
					htmltext = "30218-00.htm";
				}
				else if (player.getLevel() < 10)
				{
					htmltext = "30218-01.htm";
				}
				else
				{
					htmltext = "30218-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "30218-05.htm";
				}
				else if (cond == 2)
				{
					htmltext = "30218-06.htm";
					st.setCond(3, true);
					takeItems(player, KABOO_CHIEF_TORC_1, 1);
					takeItems(player, KENDELL_ORDER_1, 1);
					takeItems(player, KENDELL_ORDER_2, 1);
					takeItems(player, KENDELL_ORDER_3, 1);
					takeItems(player, KENDELL_ORDER_4, 1);
					giveItems(player, getRandom(1840, 1843), 1); // Kendell's orders 5 to 8.
				}
				else if (cond == 3)
				{
					htmltext = "30218-07.htm";
				}
				else if (cond == 4)
				{
					htmltext = "30218-08.htm";
					takeItems(player, KABOO_CHIEF_TORC_2, 1);
					takeItems(player, KENDELL_ORDER_5, 1);
					takeItems(player, KENDELL_ORDER_6, 1);
					takeItems(player, KENDELL_ORDER_7, 1);
					takeItems(player, KENDELL_ORDER_8, 1);
					
					if (player.isMageClass())
					{
						giveItems(player, RED_SUNSET_STAFF, 1);
					}
					else
					{
						giveItems(player, RED_SUNSET_SWORD, 1);
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
					
					rewardItems(player, LESSER_HEALING_POT, 100);
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
			case KABOO_CHIEF_UOPH:
			case KABOO_CHIEF_KRACHA:
			case KABOO_CHIEF_BATOH:
			case KABOO_CHIEF_TANUKIA:
			{
				if (st.isCond(1) && hasQuestItems(player, npc.getId() - 25223)) // npcId - 25223 = itemId to verify.
				{
					st.setCond(2, true);
					giveItems(player, KABOO_CHIEF_TORC_1, 1);
				}
				break;
			}
			case KABOO_CHIEF_TUREL:
			case KABOO_CHIEF_ROKO:
			{
				if (st.isCond(3) && hasQuestItems(player, npc.getId() - 25224)) // npcId - 25224 = itemId to verify.
				{
					st.setCond(4, true);
					giveItems(player, KABOO_CHIEF_TORC_2, 1);
				}
				break;
			}
			case KABOO_CHIEF_KAMUT:
			case KABOO_CHIEF_MURTIKA:
			{
				if (st.isCond(3) && hasQuestItems(player, npc.getId() - 25225)) // npcId - 25225 = itemId to verify.
				{
					st.setCond(4, true);
					giveItems(player, KABOO_CHIEF_TORC_2, 1);
				}
				break;
			}
		}
	}
}
