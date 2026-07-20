/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00163_LegacyOfThePoet;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Legacy of the Poet (163)
 * @author xban1x
 */
public class Q00163_LegacyOfThePoet extends Quest
{
	// NPC
	private static final int STARDEN = 30220;
	
	// Monsters
	private static final int[] MONSTERS =
	{
		20372, // Baraq Orc Fighter
		20373, // Baraq Orc Warrior Leader
	};
	
	// Items
	private static final int RUMIELS_1ST_POEM = 1038;
	private static final int RUMIELS_2ND_POEM = 1039;
	private static final int RUMIELS_3RD_POEM = 1040;
	private static final int RUMIELS_4TH_POEM = 1041;
	
	// Misc
	private static final int MIN_LEVEL = 11;
	
	public Q00163_LegacyOfThePoet()
	{
		super(163, "Legacy of the Poet");
		addStartNpc(STARDEN);
		addTalkId(STARDEN);
		addKillId(MONSTERS);
		registerQuestItems(RUMIELS_1ST_POEM, RUMIELS_2ND_POEM, RUMIELS_3RD_POEM, RUMIELS_4TH_POEM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs != null)
		{
			switch (event)
			{
				case "30220-03.html":
				case "30220-04.html":
				{
					htmltext = event;
					break;
				}
				case "30220-05.htm":
				{
					qs.startQuest();
					htmltext = event;
					break;
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getRace() != Race.DARK_ELF) ? (player.getLevel() >= MIN_LEVEL) ? "30220-02.htm" : "30220-01.htm" : "30220-00.htm";
				break;
			}
			case State.STARTED:
			{
				if (hasQuestItems(player, RUMIELS_1ST_POEM, RUMIELS_2ND_POEM, RUMIELS_3RD_POEM, RUMIELS_4TH_POEM))
				{
					giveAdena(player, 13890, true);
					qs.exitQuest(false, true);
					htmltext = "30220-07.html";
				}
				else
				{
					htmltext = "30220-06.html";
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
		if ((qs != null) && qs.isCond(1))
		{
			if ((getRandom(10) == 0) && !hasQuestItems(killer, RUMIELS_1ST_POEM))
			{
				giveItems(killer, RUMIELS_1ST_POEM, 1);
				if (hasQuestItems(killer, RUMIELS_2ND_POEM, RUMIELS_3RD_POEM, RUMIELS_4TH_POEM))
				{
					qs.setCond(2, true);
				}
				else
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
			
			if ((getRandom(10) > 7) && !hasQuestItems(killer, RUMIELS_2ND_POEM))
			{
				giveItems(killer, RUMIELS_2ND_POEM, 1);
				if (hasQuestItems(killer, RUMIELS_1ST_POEM, RUMIELS_3RD_POEM, RUMIELS_4TH_POEM))
				{
					qs.setCond(2, true);
				}
				else
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
			
			if ((getRandom(10) > 7) && !hasQuestItems(killer, RUMIELS_3RD_POEM))
			{
				giveItems(killer, RUMIELS_3RD_POEM, 1);
				if (hasQuestItems(killer, RUMIELS_1ST_POEM, RUMIELS_2ND_POEM, RUMIELS_4TH_POEM))
				{
					qs.setCond(2, true);
				}
				else
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
			
			if ((getRandom(10) > 5) && !hasQuestItems(killer, RUMIELS_4TH_POEM))
			{
				giveItems(killer, RUMIELS_4TH_POEM, 1);
				if (hasQuestItems(killer, RUMIELS_1ST_POEM, RUMIELS_2ND_POEM, RUMIELS_3RD_POEM))
				{
					qs.setCond(2, true);
				}
				else
				{
					playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
			}
		}
	}
}
