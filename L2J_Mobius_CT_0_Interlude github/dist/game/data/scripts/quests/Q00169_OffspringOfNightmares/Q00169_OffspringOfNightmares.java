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
package quests.Q00169_OffspringOfNightmares;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00169_OffspringOfNightmares extends Quest
{
	// Items
	private static final int CRACKED_SKULL = 1030;
	private static final int PERFECT_SKULL = 1031;
	private static final int BONE_GAITERS = 31;
	
	public Q00169_OffspringOfNightmares()
	{
		super(169, "Offspring of Nightmares");
		registerQuestItems(CRACKED_SKULL, PERFECT_SKULL);
		addStartNpc(30145); // Vlasty
		addTalkId(30145);
		addKillId(20105, 20025);
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
		
		if (event.equals("30145-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30145-08.htm"))
		{
			final int reward = 17000 + (getQuestItemsCount(player, CRACKED_SKULL) * 20);
			takeItems(player, PERFECT_SKULL, -1);
			takeItems(player, CRACKED_SKULL, -1);
			giveItems(player, BONE_GAITERS, 1);
			giveAdena(player, reward, true);
			st.exitQuest(false, true);
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
				if (player.getRace() != Race.DARK_ELF)
				{
					htmltext = "30145-00.htm";
				}
				else if (player.getLevel() < 15)
				{
					htmltext = "30145-02.htm";
				}
				else
				{
					htmltext = "30145-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					if (hasQuestItems(player, CRACKED_SKULL))
					{
						htmltext = "30145-06.htm";
					}
					else
					{
						htmltext = "30145-05.htm";
					}
				}
				else if (cond == 2)
				{
					htmltext = "30145-07.htm";
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
		if ((qs != null) && qs.isStarted())
		{
			if ((getRandom(10) > 7) && !hasQuestItems(killer, PERFECT_SKULL))
			{
				giveItems(killer, PERFECT_SKULL, 1);
				qs.setCond(2, true);
			}
			else if (getRandom(10) > 4)
			{
				giveItems(killer, CRACKED_SKULL, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
}
