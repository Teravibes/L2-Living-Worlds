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
package ai.others.CastleBlacksmith;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Castle Blacksmith AI.
 * @author malyelfik
 */
public class CastleBlacksmith extends Script
{
	// Blacksmith IDs
	private static final int[] NPCS =
	{
		35098, // Blacksmith (Gludio)
		35140, // Blacksmith (Dion)
		35182, // Blacksmith (Giran)
		35224, // Blacksmith (Oren)
		35272, // Blacksmith (Aden)
		35314, // Blacksmith (Innadril)
		35361, // Blacksmith (Goddard)
		35507, // Blacksmith (Rune)
		35553, // Blacksmith (Schuttgart)
	};
	
	private CastleBlacksmith()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	private boolean hasRights(Player player, Npc npc)
	{
		return player.isGM() || npc.isMyLord(player) || ((player.getClanId() == npc.getCastle().getOwnerId()) && player.hasAccess(ClanAccess.CASTLE_MANOR));
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		return (event.equalsIgnoreCase(npc.getId() + "-02.html") && hasRights(player, npc)) ? event : null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return hasRights(player, npc) ? npc.getId() + "-01.html" : "no.html";
	}
	
	public static void main(String[] args)
	{
		new CastleBlacksmith();
	}
}
