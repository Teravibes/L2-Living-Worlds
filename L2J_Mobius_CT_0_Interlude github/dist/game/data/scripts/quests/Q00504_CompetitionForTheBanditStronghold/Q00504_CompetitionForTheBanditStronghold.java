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
package quests.Q00504_CompetitionForTheBanditStronghold;

import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;

/**
 * @author LordWinter
 */
public final class Q00504_CompetitionForTheBanditStronghold extends Quest
{
	private static final int MESSENGER = 35437;
	
	private static final int TARLK_AMULET = 4332;
	private static final int TROPHY_OF_ALLIANCE = 5009;
	
	private static final int[] MOBS =
	{
		20570,
		20571,
		20572,
		20573,
		20574
	};
	
	private static final SiegableHall BANDIT_STRONGHOLD = CHSiegeManager.getInstance().getSiegableHall(35);
	
	public Q00504_CompetitionForTheBanditStronghold()
	{
		super(504, "Competition for the Bandit Stronghold");
		
		addStartNpc(MESSENGER);
		addTalkId(MESSENGER);
		
		for (int mob : MOBS)
		{
			addKillId(mob);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (BANDIT_STRONGHOLD.getSiege().getAttackers().size() >= 5)
				{
					htmltext = "35437-00.htm";
				}
				else
				{
					htmltext = "35437-01.htm";
					st.startQuest();
				}
				break;
			}
			case State.STARTED:
			{
				if (getQuestItemsCount(player, TARLK_AMULET) < 30)
				{
					htmltext = "35437-02.htm";
				}
				else
				{
					takeItems(player, TARLK_AMULET, 30);
					rewardItems(player, TROPHY_OF_ALLIANCE, 1);
					st.exitQuest(true);
					htmltext = "35437-03.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState st = getQuestState(killer, false);
		if (st == null)
		{
			return;
		}
		
		if (st.isStarted() && st.isCond(1))
		{
			giveItems(killer, TARLK_AMULET, 1);
			if (getQuestItemsCount(killer, TARLK_AMULET) < 30)
			{
				playSound(killer, QuestSound.ITEMSOUND_QUEST_MIDDLE);
			}
			else
			{
				st.setCond(2, true);
			}
		}
	}
}
