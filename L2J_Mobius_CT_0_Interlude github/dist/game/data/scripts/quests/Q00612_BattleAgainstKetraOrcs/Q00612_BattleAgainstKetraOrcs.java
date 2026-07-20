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
package quests.Q00612_BattleAgainstKetraOrcs;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Battle against Ketra Orcs (612)
 * @author malyelfik
 */
public class Q00612_BattleAgainstKetraOrcs extends Quest
{
	// NPC
	private static final int ASHAS = 31377;
	
	// Monsters
	private static final Map<Integer, Integer> MOBS = new HashMap<>();
	static
	{
		MOBS.put(21324, 500); // Ketra Orc Footman
		MOBS.put(21327, 510); // Ketra Orc Raider
		MOBS.put(21328, 522); // Ketra Orc Scout
		MOBS.put(21329, 519); // Ketra Orc Shaman
		MOBS.put(21331, 529); // Ketra Orc Warrior
		MOBS.put(21332, 529); // Ketra Orc Lieutenant
		MOBS.put(21334, 539); // Ketra Orc Medium
		MOBS.put(21336, 548); // Ketra Orc White Captain
		MOBS.put(21338, 558); // Ketra Orc Seer
		MOBS.put(21339, 568); // Ketra Orc General
		MOBS.put(21340, 568); // Ketra Orc Battalion Commander
		MOBS.put(21342, 578); // Ketra Orc Grand Seer
		MOBS.put(21343, 664); // Ketra Commander
		MOBS.put(21345, 713); // Ketra's Head Shaman
		MOBS.put(21347, 738); // Ketra Prophet
	}
	
	// Items
	private static final int SEED = 7187;
	private static final int MOLAR = 7234;
	
	// Misc
	private static final int MIN_LEVEL = 74;
	private static final int MOLAR_COUNT = 100;
	
	public Q00612_BattleAgainstKetraOrcs()
	{
		super(612, "War with Ketra Orcs");
		addStartNpc(ASHAS);
		addTalkId(ASHAS);
		addKillId(MOBS.keySet());
		registerQuestItems(MOLAR);
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
			case "31377-03.htm":
			{
				qs.startQuest();
				break;
			}
			case "31377-06.html":
			{
				break;
			}
			case "31377-07.html":
			{
				if (getQuestItemsCount(player, MOLAR) < MOLAR_COUNT)
				{
					return "31377-08.html";
				}
				
				takeItems(player, MOLAR, MOLAR_COUNT);
				giveItems(player, SEED, 20);
				break;
			}
			case "31377-09.html":
			{
				qs.exitQuest(true, true);
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
		final Player member = getRandomPartyMember(killer, 1);
		if ((member != null) && (getRandom(1000) < MOBS.get(npc.getId())))
		{
			giveItems(member, MOLAR, 1);
			playSound(member, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "31377-01.htm" : "31377-02.htm";
				break;
			}
			case State.STARTED:
			{
				htmltext = (hasQuestItems(player, MOLAR)) ? "31377-04.html" : "31377-05.html";
				break;
			}
		}
		
		return htmltext;
	}
}
