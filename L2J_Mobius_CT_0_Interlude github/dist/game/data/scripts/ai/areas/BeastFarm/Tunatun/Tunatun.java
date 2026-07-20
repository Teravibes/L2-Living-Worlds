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
package ai.areas.BeastFarm.Tunatun;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;

import quests.Q00020_BringUpWithLove.Q00020_BringUpWithLove;

/**
 * Beast Herder Tunatun AI.
 * @author Adry_85
 */
public class Tunatun extends Script
{
	// NPC
	private static final int TUNATUN = 31537;
	
	// Item
	private static final int BEAST_HANDLERS_WHIP = 15473;
	
	// Misc
	private static final int MIN_LEVEL = 82;
	
	private Tunatun()
	{
		addStartNpc(TUNATUN);
		addFirstTalkId(TUNATUN);
		addTalkId(TUNATUN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("Whip".equals(event))
		{
			if (hasQuestItems(player, BEAST_HANDLERS_WHIP))
			{
				return "31537-01.html";
			}
			
			final QuestState qs = player.getQuestState(Q00020_BringUpWithLove.class.getSimpleName());
			if ((qs == null) && (player.getLevel() < MIN_LEVEL))
			{
				return "31537-02.html";
			}
			else if ((qs != null) || (player.getLevel() >= MIN_LEVEL))
			{
				giveItems(player, BEAST_HANDLERS_WHIP, 1);
				return "31537-03.html";
			}
		}
		
		return event;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new Tunatun();
	}
}
