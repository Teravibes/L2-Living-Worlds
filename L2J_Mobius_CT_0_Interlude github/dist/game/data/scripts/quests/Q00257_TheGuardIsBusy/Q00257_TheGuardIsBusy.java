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
package quests.Q00257_TheGuardIsBusy;

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

public class Q00257_TheGuardIsBusy extends Quest
{
	// Items
	private static final int GLUDIO_LORD_MARK = 1084;
	private static final int ORC_AMULET = 752;
	private static final int ORC_NECKLACE = 1085;
	private static final int WEREWOLF_FANG = 1086;
	
	// Newbie Items
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	
	public Q00257_TheGuardIsBusy()
	{
		super(257, "The Guard is Busy");
		registerQuestItems(ORC_AMULET, ORC_NECKLACE, WEREWOLF_FANG, GLUDIO_LORD_MARK);
		addStartNpc(30039); // Gilbert
		addTalkId(30039);
		addKillId(20006, 20093, 20096, 20098, 20130, 20131, 20132, 20342, 20343);
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
		
		if (event.equals("30039-03.htm"))
		{
			st.startQuest();
			giveItems(player, GLUDIO_LORD_MARK, 1);
		}
		else if (event.equals("30039-05.htm"))
		{
			takeItems(player, GLUDIO_LORD_MARK, 1);
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
				htmltext = (player.getLevel() < 6) ? "30039-01.htm" : "30039-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int amulets = getQuestItemsCount(player, ORC_AMULET);
				final int necklaces = getQuestItemsCount(player, ORC_NECKLACE);
				final int fangs = getQuestItemsCount(player, WEREWOLF_FANG);
				
				if ((amulets + necklaces + fangs) == 0)
				{
					htmltext = "30039-04.htm";
				}
				else
				{
					htmltext = "30039-07.htm";
					
					int reward = (10 * amulets) + (20 * fangs) + (20 * necklaces);
					if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD && ((amulets + necklaces + fangs) >= 10))
					{
						reward += 1000;
					}
					
					takeItems(player, ORC_AMULET, -1);
					takeItems(player, ORC_NECKLACE, -1);
					takeItems(player, WEREWOLF_FANG, -1);
					giveAdena(player, reward, true);
					
					// Give newbie reward if player is eligible.
					if (player.isNewbie() && (st.getInt("Reward") == 0))
					{
						int newPlayerRewardsReceived = player.getVariables().getInt(PlayerVariables.NEWBIE_SHOTS_RECEIVED, 0);
						if (newPlayerRewardsReceived < 1)
						{
							st.showQuestionMark(26);
							st.set("Reward", "1");
							
							if (player.isMageClass() && (player.getRace() != Race.ORC))
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
			case 20006:
			case 20130:
			case 20131:
			{
				if (Rnd.get(10) < 5)
				{
					giveItems(player, ORC_AMULET, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20093:
			case 20096:
			case 20098:
			{
				if (Rnd.get(10) < 5)
				{
					giveItems(player, ORC_NECKLACE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20342:
			{
				if (Rnd.get(10) < 2)
				{
					giveItems(player, WEREWOLF_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20343:
			{
				if (Rnd.get(10) < 4)
				{
					giveItems(player, WEREWOLF_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case 20132:
			{
				if (Rnd.get(10) < 5)
				{
					giveItems(player, WEREWOLF_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
