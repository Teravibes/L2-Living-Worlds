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
package quests.Q00316_DestroyPlagueCarriers;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00316_DestroyPlagueCarriers extends Quest
{
	// Monsters
	private static final int SUKAR_WERERAT = 20040;
	private static final int SUKAR_WERERAT_LEADER = 20047;
	private static final int VAROOL_FOULCLAW = 27020;
	
	// Items
	private static final int WERERAT_FANG = 1042;
	private static final int VAROOL_FOULCLAW_FANG = 1043;
	
	public Q00316_DestroyPlagueCarriers()
	{
		super(316, "Destroy Plague Carriers");
		registerQuestItems(WERERAT_FANG, VAROOL_FOULCLAW_FANG);
		addStartNpc(30155); // Ellenia
		addTalkId(30155);
		addKillId(SUKAR_WERERAT, SUKAR_WERERAT_LEADER, VAROOL_FOULCLAW);
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
		
		if (event.equals("30155-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30155-08.htm"))
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
				if (player.getRace() != Race.ELF)
				{
					htmltext = "30155-00.htm";
				}
				else if (player.getLevel() < 18)
				{
					htmltext = "30155-02.htm";
				}
				else
				{
					htmltext = "30155-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int ratFangs = getQuestItemsCount(player, WERERAT_FANG);
				final int varoolFangs = getQuestItemsCount(player, VAROOL_FOULCLAW_FANG);
				if ((ratFangs + varoolFangs) == 0)
				{
					htmltext = "30155-05.htm";
				}
				else
				{
					htmltext = "30155-07.htm";
					takeItems(player, WERERAT_FANG, -1);
					takeItems(player, VAROOL_FOULCLAW_FANG, -1);
					
					int reward = (ratFangs * 30) + (varoolFangs * 10000);
					if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD && (ratFangs >= 10))
					{
						reward += 5000;
					}
					
					giveAdena(player, reward, true);
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
			case SUKAR_WERERAT:
			case SUKAR_WERERAT_LEADER:
			{
				if (getRandom(10) < 4)
				{
					giveItems(player, WERERAT_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case VAROOL_FOULCLAW:
			{
				if (!hasQuestItems(player, VAROOL_FOULCLAW_FANG) && (getRandom(10) < 2))
				{
					giveItems(player, VAROOL_FOULCLAW_FANG, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
