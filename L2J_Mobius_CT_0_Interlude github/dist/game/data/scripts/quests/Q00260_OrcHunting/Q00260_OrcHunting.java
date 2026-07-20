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
package quests.Q00260_OrcHunting;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

public class Q00260_OrcHunting extends Quest
{
	// NPC
	private static final int RAYEN = 30221;
	
	// Monsters
	private static final int KABOO_ORC = 20468;
	private static final int KABOO_ORC_ARCHER = 20469;
	private static final int KABOO_ORC_GRUNT = 20470;
	private static final int KABOO_ORC_FIGHTER = 20471;
	private static final int KABOO_ORC_FIGHTER_LEADER = 20472;
	private static final int KABOO_ORC_FIGHTER_LIEUTENANT = 20473;
	
	// Items
	private static final int ORC_AMULET = 1114;
	private static final int ORC_NECKLACE = 1115;
	
	// Newbie Items
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	
	public Q00260_OrcHunting()
	{
		super(260, "Hunt the Orcs");
		registerQuestItems(ORC_AMULET, ORC_NECKLACE);
		addStartNpc(RAYEN);
		addTalkId(RAYEN);
		addKillId(KABOO_ORC, KABOO_ORC_ARCHER, KABOO_ORC_GRUNT, KABOO_ORC_FIGHTER, KABOO_ORC_FIGHTER_LEADER, KABOO_ORC_FIGHTER_LIEUTENANT);
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
		
		if (event.equals("30221-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30221-06.htm"))
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
				if (player.getRace() != Race.ELF)
				{
					htmltext = "30221-00.htm";
				}
				else if (player.getLevel() < 6)
				{
					htmltext = "30221-01.htm";
				}
				else
				{
					htmltext = "30221-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int amulet = getQuestItemsCount(player, ORC_AMULET);
				final int necklace = getQuestItemsCount(player, ORC_NECKLACE);
				
				if ((amulet == 0) && (necklace == 0))
				{
					htmltext = "30221-04.htm";
				}
				else
				{
					htmltext = "30221-05.htm";
					
					int reward = (amulet * 12) + (necklace * 30);
					if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD && ((amulet + necklace) >= 10))
					{
						reward += 1000;
					}
					
					takeItems(player, ORC_AMULET, -1);
					takeItems(player, ORC_NECKLACE, -1);
					giveAdena(player, reward, true);
					
					// Give newbie reward if player is eligible.
					if (player.isNewbie() && (st.getInt("Reward") == 0))
					{
						int newPlayerRewardsReceived = player.getVariables().getInt(PlayerVariables.NEWBIE_SHOTS_RECEIVED, 0);
						if (newPlayerRewardsReceived < 1)
						{
							st.showQuestionMark(26);
							st.set("Reward", "1");
							
							if (player.isMageClass())
							{
								st.playTutorialVoice("tutorial_voice_027");
								giveItems(player, SPIRITSHOT_FOR_BEGINNERS, 3000);
								player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
							}
							else
							{
								st.playTutorialVoice("tutorial_voice_026");
								giveItems(player, SOULSHOT_FOR_BEGINNERS, 6000);
								player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
							}
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
		
		switch (npc.getId())
		{
			case KABOO_ORC:
			case KABOO_ORC_GRUNT:
			case KABOO_ORC_ARCHER:
			{
				if (Rnd.get(10) < 5)
				{
					giveItems(player, ORC_AMULET, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case KABOO_ORC_FIGHTER:
			case KABOO_ORC_FIGHTER_LEADER:
			case KABOO_ORC_FIGHTER_LIEUTENANT:
			{
				if (Rnd.get(10) < 5)
				{
					giveItems(player, ORC_NECKLACE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
