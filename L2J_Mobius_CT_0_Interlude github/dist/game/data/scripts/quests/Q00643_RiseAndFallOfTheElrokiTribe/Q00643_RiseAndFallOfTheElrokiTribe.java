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
package quests.Q00643_RiseAndFallOfTheElrokiTribe;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00643_RiseAndFallOfTheElrokiTribe extends Quest
{
	// NPCs
	private static final int SINGSING = 32106;
	private static final int KARAKAWEI = 32117;
	
	// Items
	private static final int BONES = 8776;
	
	public Q00643_RiseAndFallOfTheElrokiTribe()
	{
		super(643, "Rise and Fall of the Elroki Tribe");
		registerQuestItems(BONES);
		addStartNpc(SINGSING);
		addTalkId(SINGSING, KARAKAWEI);
		addKillId(22208, 22209, 22210, 22211, 22212, 22213, 22221, 22222, 22226, 22227);
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
			case "32106-03.htm":
			{
				st.startQuest();
				break;
			}
			case "32106-07.htm":
			{
				final int count = getQuestItemsCount(player, BONES);
				takeItems(player, BONES, count);
				giveAdena(player, count * 1374, true);
				break;
			}
			case "32106-09.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "32117-03.htm":
			{
				final int count = getQuestItemsCount(player, BONES);
				if (count >= 300)
				{
					takeItems(player, BONES, 300);
					rewardItems(player, getRandom(8712, 8722), 5);
				}
				else
				{
					htmltext = "32117-04.htm";
				}
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
				htmltext = (player.getLevel() < 75) ? "32106-00.htm" : "32106-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case SINGSING:
					{
						htmltext = (hasQuestItems(player, BONES)) ? "32106-06.htm" : "32106-05.htm";
						break;
					}
					case KARAKAWEI:
					{
						htmltext = "32117-01.htm";
						break;
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
		final QuestState st = getRandomPartyMemberState(player, -1, 3, npc);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		if (getRandom(100) < 75)
		{
			final Player partyMember = st.getPlayer();
			giveItems(partyMember, BONES, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
