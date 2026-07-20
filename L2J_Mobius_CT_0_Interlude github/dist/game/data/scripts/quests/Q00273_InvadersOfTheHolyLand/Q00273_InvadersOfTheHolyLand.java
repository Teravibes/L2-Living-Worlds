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
package quests.Q00273_InvadersOfTheHolyLand;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

public class Q00273_InvadersOfTheHolyLand extends Quest
{
	// Items
	private static final int BLACK_SOULSTONE = 1475;
	private static final int RED_SOULSTONE = 1476;
	
	// Reward
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	
	public Q00273_InvadersOfTheHolyLand()
	{
		super(273, "Invaders of the Holy Land");
		registerQuestItems(BLACK_SOULSTONE, RED_SOULSTONE);
		addStartNpc(30566); // Varkees
		addTalkId(30566);
		addKillId(20311, 20312, 20313);
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
		
		if (event.equals("30566-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30566-07.htm"))
		{
			st.exitQuest(true, true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (player.getRace() != Race.ORC)
				{
					htmltext = "30566-00.htm";
				}
				else if (player.getLevel() < 6)
				{
					htmltext = "30566-01.htm";
				}
				else
				{
					htmltext = "30566-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int red = getQuestItemsCount(player, RED_SOULSTONE);
				final int black = getQuestItemsCount(player, BLACK_SOULSTONE);
				if ((red + black) == 0)
				{
					htmltext = "30566-04.htm";
				}
				else
				{
					if (red == 0)
					{
						htmltext = "30566-05.htm";
					}
					else
					{
						htmltext = "30566-06.htm";
					}
					
					int reward = (black * 3) + (red * 10);
					if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD)
					{
						reward += ((black >= 10) ? ((red >= 1) ? 1800 : 1500) : 0);
					}
					
					takeItems(player, BLACK_SOULSTONE, -1);
					takeItems(player, RED_SOULSTONE, -1);
					giveAdena(player, reward, true);
					
					// Give newbie reward if player is eligible.
					if (player.isNewbie() && (st.getInt("Reward") == 0))
					{
						int newPlayerRewardsReceived = player.getVariables().getInt(PlayerVariables.NEWBIE_SHOTS_RECEIVED, 0);
						if (newPlayerRewardsReceived < 1)
						{
							giveItems(player, SOULSHOT_FOR_BEGINNERS, 6000);
							st.playTutorialVoice("tutorial_voice_026");
							st.set("Reward", "1");
							player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
						}
					}
				}
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
		int probability = 77;
		if (npcId == 20311)
		{
			probability = 90;
		}
		else if (npcId == 20312)
		{
			probability = 87;
		}
		
		if (getRandom(100) <= probability)
		{
			giveItems(player, BLACK_SOULSTONE, 1);
		}
		else
		{
			giveItems(player, RED_SOULSTONE, 1);
		}
		
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
	}
}
