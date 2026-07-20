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
package quests.Q00632_NecromancersRequest;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00632_NecromancersRequest extends Quest
{
	// Monsters
	private static final int[] VAMPIRES =
	{
		21568,
		21573,
		21582,
		21585,
		21586,
		21587,
		21588,
		21589,
		21590,
		21591,
		21592,
		21593,
		21594,
		21595
	};
	private static final int[] UNDEADS =
	{
		21547,
		21548,
		21549,
		21551,
		21552,
		21555,
		21556,
		21562,
		21571,
		21576,
		21577,
		21579
	};
	
	// Items
	private static final int VAMPIRE_HEART = 7542;
	private static final int ZOMBIE_BRAIN = 7543;
	
	public Q00632_NecromancersRequest()
	{
		super(632, "Necromancer's Request");
		registerQuestItems(VAMPIRE_HEART, ZOMBIE_BRAIN);
		addStartNpc(31522); // Mysterious Wizard
		addTalkId(31522);
		addKillId(VAMPIRES);
		addKillId(UNDEADS);
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
		
		if (event.equals("31522-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31522-06.htm"))
		{
			if (getQuestItemsCount(player, VAMPIRE_HEART) >= 200)
			{
				st.setCond(1, true);
				takeItems(player, VAMPIRE_HEART, -1);
				giveAdena(player, 120000, true);
			}
			else
			{
				htmltext = "31522-09.htm";
			}
		}
		else if (event.equals("31522-08.htm"))
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
				htmltext = (player.getLevel() < 63) ? "31522-01.htm" : "31522-02.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (getQuestItemsCount(player, VAMPIRE_HEART) >= 200) ? "31522-05.htm" : "31522-04.htm";
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
		
		final Player partyMember = st.getPlayer();
		for (int undead : UNDEADS)
		{
			if ((undead == npc.getId()) && (getRandom(100) < 33))
			{
				giveItems(partyMember, ZOMBIE_BRAIN, 1);
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				return;
			}
		}
		
		if (st.isCond(1) && getRandomBoolean())
		{
			giveItems(partyMember, VAMPIRE_HEART, 1);
			if (getQuestItemsCount(partyMember, VAMPIRE_HEART) < 200)
			{
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else
			{
				st.setCond(2, true);
			}
		}
	}
}
