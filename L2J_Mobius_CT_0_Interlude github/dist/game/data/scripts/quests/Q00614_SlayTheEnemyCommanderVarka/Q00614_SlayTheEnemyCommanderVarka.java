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
package quests.Q00614_SlayTheEnemyCommanderVarka;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Slay the Enemy Commander! (Varka) (614)
 * @author malyelfik
 */
public class Q00614_SlayTheEnemyCommanderVarka extends Quest
{
	// NPC
	private static final int ASHAS = 31377;
	
	// Monster
	private static final int TAYR = 25302;
	
	// Items
	private static final int TAYR_HEAD = 7241;
	private static final int WISDOM_FEATHER = 7230;
	private static final int VARKA_ALLIANCE_FOUR = 7224;
	
	// Misc
	private static final int MIN_LEVEL = 75;
	
	public Q00614_SlayTheEnemyCommanderVarka()
	{
		super(614, "Slay the enemy commander!");
		addStartNpc(ASHAS);
		addTalkId(ASHAS);
		addKillId(TAYR);
		registerQuestItems(TAYR_HEAD);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, player, false))
		{
			giveItems(player, TAYR_HEAD, 1);
			qs.setCond(2, true);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31377-04.htm":
			{
				qs.startQuest();
				break;
			}
			case "31377-07.html":
			{
				if (hasQuestItems(player, TAYR_HEAD) && qs.isCond(2))
				{
					giveItems(player, WISDOM_FEATHER, 1);
					addExpAndSp(player, 10000, 0);
					qs.exitQuest(true, true);
				}
				else
				{
					htmltext = getNoQuestMsg(player);
				}
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? (hasQuestItems(player, VARKA_ALLIANCE_FOUR)) ? "31377-01.htm" : "31377-02.htm" : "31377-03.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (qs.isCond(2) && hasQuestItems(player, TAYR_HEAD)) ? "31377-05.html" : "31377-06.html";
				break;
			}
		}
		
		return htmltext;
	}
}
