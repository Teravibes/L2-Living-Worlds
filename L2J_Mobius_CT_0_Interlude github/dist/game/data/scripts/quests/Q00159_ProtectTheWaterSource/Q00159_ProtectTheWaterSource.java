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
package quests.Q00159_ProtectTheWaterSource;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00159_ProtectTheWaterSource extends Quest
{
	// Items
	private static final int PLAGUE_DUST = 1035;
	private static final int HYACINTH_CHARM_1 = 1071;
	private static final int HYACINTH_CHARM_2 = 1072;
	
	public Q00159_ProtectTheWaterSource()
	{
		super(159, "Protect the Water Source");
		registerQuestItems(PLAGUE_DUST, HYACINTH_CHARM_1, HYACINTH_CHARM_2);
		addStartNpc(30154); // Asterios
		addTalkId(30154);
		addKillId(27017); // Plague Zombie
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
		
		if (event.equals("30154-04.htm"))
		{
			st.startQuest();
			giveItems(player, HYACINTH_CHARM_1, 1);
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
				if (player.getRace() != Race.ELF)
				{
					htmltext = "30154-00.htm";
				}
				else if (player.getLevel() < 12)
				{
					htmltext = "30154-02.htm";
				}
				else
				{
					htmltext = "30154-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "30154-05.htm";
				}
				else if (cond == 2)
				{
					htmltext = "30154-06.htm";
					st.setCond(3, true);
					takeItems(player, PLAGUE_DUST, -1);
					takeItems(player, HYACINTH_CHARM_1, 1);
					giveItems(player, HYACINTH_CHARM_2, 1);
				}
				else if (cond == 3)
				{
					htmltext = "30154-07.htm";
				}
				else if (cond == 4)
				{
					htmltext = "30154-08.htm";
					takeItems(player, HYACINTH_CHARM_2, 1);
					takeItems(player, PLAGUE_DUST, -1);
					giveAdena(player, 18250, true);
					st.exitQuest(false, true);
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
		if ((qs != null))
		{
			switch (qs.getCond())
			{
				case 1:
				{
					if ((getRandom(100) < 40) && hasQuestItems(killer, HYACINTH_CHARM_1) && !hasQuestItems(killer, PLAGUE_DUST))
					{
						giveItems(killer, PLAGUE_DUST, 1);
						qs.setCond(2, true);
					}
					break;
				}
				case 3:
				{
					long dust = getQuestItemsCount(killer, PLAGUE_DUST);
					if ((getRandom(100) < 40) && (dust < 5) && hasQuestItems(killer, HYACINTH_CHARM_2))
					{
						giveItems(killer, PLAGUE_DUST, 1);
						if ((++dust) >= 5)
						{
							qs.setCond(4, true);
						}
						else
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
			}
		}
	}
}
