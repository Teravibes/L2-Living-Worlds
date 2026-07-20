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
package quests.Q00688_DefeatTheElrokianRaiders;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00688_DefeatTheElrokianRaiders extends Quest
{
	// NPC
	private static final int DINN = 32105;
	
	// Monster
	private static final int ELROKI = 22214;
	
	// Item
	private static final int DINOSAUR_FANG_NECKLACE = 8785;
	
	public Q00688_DefeatTheElrokianRaiders()
	{
		super(688, "Defeat the Elrokian Raiders!");
		registerQuestItems(DINOSAUR_FANG_NECKLACE);
		addStartNpc(DINN);
		addTalkId(DINN);
		addKillId(ELROKI);
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
			case "32105-03.htm":
			{
				st.startQuest();
				break;
			}
			case "32105-08.htm":
			{
				final int count = getQuestItemsCount(player, DINOSAUR_FANG_NECKLACE);
				if (count > 0)
				{
					takeItems(player, DINOSAUR_FANG_NECKLACE, -1);
					giveAdena(player, count * 3000, true);
				}
				
				st.exitQuest(true, true);
				break;
			}
			case "32105-06.htm":
			{
				final int count = getQuestItemsCount(player, DINOSAUR_FANG_NECKLACE);
				takeItems(player, DINOSAUR_FANG_NECKLACE, -1);
				giveAdena(player, count * 3000, true);
				break;
			}
			case "32105-07.htm":
			{
				final int count = getQuestItemsCount(player, DINOSAUR_FANG_NECKLACE);
				if (count >= 100)
				{
					takeItems(player, DINOSAUR_FANG_NECKLACE, 100);
					giveAdena(player, 450000, true);
				}
				else
				{
					htmltext = "32105-04.htm";
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
				htmltext = (player.getLevel() < 75) ? "32105-00.htm" : "32105-01.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (!hasQuestItems(player, DINOSAUR_FANG_NECKLACE)) ? "32105-04.htm" : "32105-05.htm";
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
		
		if (getRandomBoolean())
		{
			final Player partyMember = st.getPlayer();
			giveItems(partyMember, DINOSAUR_FANG_NECKLACE, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
