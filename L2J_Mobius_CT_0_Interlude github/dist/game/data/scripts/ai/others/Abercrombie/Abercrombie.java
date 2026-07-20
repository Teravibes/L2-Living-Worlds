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
package ai.others.Abercrombie;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Mercenary Supplier Abercrombie AI.
 * @author Zoey76
 */
public class Abercrombie extends Script
{
	// NPC
	private static final int ABERCROMBIE = 31555;
	
	// Items
	private static final int GOLDEN_RAM_BADGE_RECRUIT = 7246;
	private static final int GOLDEN_RAM_BADGE_SOLDIER = 7247;
	
	public Abercrombie()
	{
		addFirstTalkId(ABERCROMBIE);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext;
		if (hasQuestItems(player, GOLDEN_RAM_BADGE_SOLDIER))
		{
			htmltext = "31555-07.html";
		}
		else if (hasQuestItems(player, GOLDEN_RAM_BADGE_RECRUIT))
		{
			htmltext = "31555-01.html";
		}
		else
		{
			htmltext = "31555-09.html";
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new Abercrombie();
	}
}
