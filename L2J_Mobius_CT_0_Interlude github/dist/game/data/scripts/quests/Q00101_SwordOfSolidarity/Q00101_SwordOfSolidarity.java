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
package quests.Q00101_SwordOfSolidarity;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00101_SwordOfSolidarity extends Quest
{
	// NPCs
	private static final int ROIEN = 30008;
	private static final int ALTRAN = 30283;
	
	// Items
	private static final int BROKEN_SWORD_HANDLE = 739;
	private static final int BROKEN_BLADE_BOTTOM = 740;
	private static final int BROKEN_BLADE_TOP = 741;
	private static final int ROIENS_LETTER = 796;
	private static final int DIR_TO_RUINS = 937;
	private static final int ALTRANS_NOTE = 742;
	private static final int SWORD_OF_SOLIDARITY = 738;
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	private static final int LESSER_HEALING_POT = 1060;
	private static final int ECHO_BATTLE = 4412;
	private static final int ECHO_LOVE = 4413;
	private static final int ECHO_SOLITUDE = 4414;
	private static final int ECHO_FEAST = 4415;
	private static final int ECHO_CELEBRATION = 4416;
	
	public Q00101_SwordOfSolidarity()
	{
		super(101, "Sword of Solidarity");
		registerQuestItems(BROKEN_SWORD_HANDLE, BROKEN_BLADE_BOTTOM, BROKEN_BLADE_TOP);
		addStartNpc(ROIEN);
		addTalkId(ROIEN, ALTRAN);
		addKillId(20361, 20362);
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
		
		switch (event)
		{
			case "30008-03.htm":
			{
				st.startQuest();
				giveItems(player, ROIENS_LETTER, 1);
				break;
			}
			case "30283-02.htm":
			{
				st.setCond(2, true);
				takeItems(player, ROIENS_LETTER, 1);
				giveItems(player, DIR_TO_RUINS, 1);
				break;
			}
			case "30283-06.htm":
			{
				takeItems(player, BROKEN_SWORD_HANDLE, 1);
				giveItems(player, SWORD_OF_SOLIDARITY, 1);
				giveItems(player, LESSER_HEALING_POT, 100);
				
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
				
				giveItems(player, ECHO_BATTLE, 10);
				giveItems(player, ECHO_LOVE, 10);
				giveItems(player, ECHO_SOLITUDE, 10);
				giveItems(player, ECHO_FEAST, 10);
				giveItems(player, ECHO_CELEBRATION, 10);
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
				if (player.getRace() != Race.HUMAN)
				{
					htmltext = "30008-01a.htm";
				}
				else if (player.getLevel() < 9)
				{
					htmltext = "30008-01.htm";
				}
				else
				{
					htmltext = "30008-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = (st.getCond());
				switch (npc.getId())
				{
					case ROIEN:
					{
						if (cond == 1)
						{
							htmltext = "30008-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30008-03a.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30008-06.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30008-05.htm";
							st.setCond(5, true);
							takeItems(player, ALTRANS_NOTE, 1);
							giveItems(player, BROKEN_SWORD_HANDLE, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30008-05a.htm";
						}
						break;
					}
					case ALTRAN:
					{
						if (cond == 1)
						{
							htmltext = "30283-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30283-03.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30283-04.htm";
							st.setCond(4, true);
							takeItems(player, DIR_TO_RUINS, 1);
							takeItems(player, BROKEN_BLADE_TOP, 1);
							takeItems(player, BROKEN_BLADE_BOTTOM, 1);
							giveItems(player, ALTRANS_NOTE, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30283-04a.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30283-05.htm";
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
		if ((qs != null) && qs.isCond(2) && (getRandom(5) == 0))
		{
			if (!hasQuestItems(killer, BROKEN_BLADE_TOP))
			{
				giveItems(killer, BROKEN_BLADE_TOP, 1);
				if (hasQuestItems(killer, BROKEN_BLADE_BOTTOM))
				{
					qs.setCond(3, true);
				}
				else
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
			else if (!hasQuestItems(killer, BROKEN_BLADE_BOTTOM))
			{
				giveItems(killer, BROKEN_BLADE_BOTTOM, 1);
				if (hasQuestItems(killer, BROKEN_BLADE_TOP))
				{
					qs.setCond(3, true);
				}
				else
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
		}
	}
}
