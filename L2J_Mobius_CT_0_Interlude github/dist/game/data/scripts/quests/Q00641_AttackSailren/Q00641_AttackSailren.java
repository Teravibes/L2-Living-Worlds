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
package quests.Q00641_AttackSailren;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

import quests.Q00126_TheNameOfEvil2.Q00126_TheNameOfEvil2;

public class Q00641_AttackSailren extends Quest
{
	// NPC
	private static final int STATUE = 32109;
	
	// Quest Item
	private static final int GAZKH_FRAGMENT = 8782;
	private static final int GAZKH = 8784;
	
	public Q00641_AttackSailren()
	{
		super(641, "Attack Sailren!");
		registerQuestItems(GAZKH_FRAGMENT);
		addStartNpc(STATUE);
		addTalkId(STATUE);
		addKillId(22196, 22197, 22198, 22199, 22218, 22223);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return null;
		}
		
		String htmltext = event;
		
		if (event.equals("32109-5.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("32109-8.htm"))
		{
			if (getQuestItemsCount(player, GAZKH_FRAGMENT) >= 30)
			{
				npc.broadcastPacket(new MagicSkillUse(npc, player, 5089, 1, 3000, 0));
				takeItems(player, GAZKH_FRAGMENT, -1);
				giveItems(player, GAZKH, 1);
				st.exitQuest(true, true);
			}
			else
			{
				htmltext = "32109-6.htm";
				st.setCond(1);
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
				if (player.getLevel() < 77)
				{
					htmltext = "32109-3.htm";
				}
				else
				{
					final QuestState st2 = player.getQuestState(Q00126_TheNameOfEvil2.class.getSimpleName());
					htmltext = ((st2 != null) && st2.isCompleted()) ? "32109-1.htm" : "32109-2.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "32109-5.htm";
				}
				else if (cond == 2)
				{
					htmltext = "32109-7.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState qs = getRandomPartyMemberState(player, 1, 3, npc);
		if (qs == null)
		{
			return;
		}
		
		final Player partyMember = qs.getPlayer();
		final QuestState st = getQuestState(partyMember, false);
		if (st == null)
		{
			return;
		}
		
		if (getRandom(100) < 5)
		{
			giveItems(partyMember, GAZKH_FRAGMENT, 1);
			if (getQuestItemsCount(partyMember, GAZKH_FRAGMENT) < 30)
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
