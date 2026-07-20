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
package quests.Q00375_WhisperOfDreamsPart2;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00375_WhisperOfDreamsPart2 extends Quest
{
	// NPCs
	private static final int MANAKIA = 30515;
	
	// Monsters
	private static final int KARIK = 20629;
	private static final int CAVE_HOWLER = 20624;
	
	// Items
	private static final int MYSTERIOUS_STONE = 5887;
	private static final int KARIK_HORN = 5888;
	private static final int CAVE_HOWLER_SKULL = 5889;
	
	// Rewards : A grade robe recipes
	private static final int[] REWARDS =
	{
		5348,
		5350,
		5352
	};
	
	public Q00375_WhisperOfDreamsPart2()
	{
		super(375, "Whisper of Dreams, Part 2");
		registerQuestItems(KARIK_HORN, CAVE_HOWLER_SKULL);
		addStartNpc(MANAKIA);
		addTalkId(MANAKIA);
		addKillId(KARIK, CAVE_HOWLER);
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
		
		// Manakia
		if (event.equals("30515-03.htm"))
		{
			st.startQuest();
			takeItems(player, MYSTERIOUS_STONE, 1);
		}
		else if (event.equals("30515-07.htm"))
		{
			st.exitQuest(true, true);
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
				htmltext = (!hasQuestItems(player, MYSTERIOUS_STONE) || (player.getLevel() < 60)) ? "30515-01.htm" : "30515-02.htm";
				break;
			}
			case State.STARTED:
			{
				if ((getQuestItemsCount(player, KARIK_HORN) >= 100) && (getQuestItemsCount(player, CAVE_HOWLER_SKULL) >= 100))
				{
					htmltext = "30515-05.htm";
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					takeItems(player, KARIK_HORN, 100);
					takeItems(player, CAVE_HOWLER_SKULL, 100);
					giveItems(player, REWARDS[getRandom(REWARDS.length)], 1);
				}
				else
				{
					htmltext = "30515-04.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		// Drop horn or skull to anyone.
		final Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return;
		}
		
		final QuestState st = partyMember.getQuestState(getName());
		if (st == null)
		{
			return;
		}
		
		switch (npc.getId())
		{
			case KARIK:
			{
				giveItems(st.getPlayer(), KARIK_HORN, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
			case CAVE_HOWLER:
			{
				if (Rnd.get(10) < 9)
				{
					giveItems(st.getPlayer(), CAVE_HOWLER_SKULL, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
