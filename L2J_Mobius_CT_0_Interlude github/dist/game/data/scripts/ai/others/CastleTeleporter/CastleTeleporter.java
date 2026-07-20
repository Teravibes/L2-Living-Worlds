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
package ai.others.CastleTeleporter;

import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Castle Teleporter AI.
 * @author malyelfik
 */
public class CastleTeleporter extends Script
{
	// Teleporter IDs
	private static final int[] NPCS =
	{
		35095, // Mass Gatekeeper (Gludio)
		35137, // Mass Gatekeeper (Dion)
		35179, // Mass Gatekeeper (Giran)
		35221, // Mass Gatekeeper (Oren)
		35266, // Mass Gatekeeper (Aden)
		35311, // Mass Gatekeeper (Innadril)
		35355, // Mass Gatekeeper (Goddard)
		35502, // Mass Gatekeeper (Rune)
		35547, // Mass Gatekeeper (Schuttgart)
	};
	
	private CastleTeleporter()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("TELEPORT_START"))
		{
			if (npc.isScriptValue(0))
			{
				final Siege siege = npc.getCastle().getSiege();
				final int time = (siege.isInProgress() && (siege.getControlTowerCount() == 0)) ? 480000 : 30000;
				startQuestTimer("TELEPORT_EXECUTE", time, npc, null);
				npc.setScriptValue(1);
			}
			
			return "teleporter-03.html";
		}
		
		if (event.equalsIgnoreCase("TELEPORT_EXECUTE"))
		{
			final int region = MapRegionData.getInstance().getMapRegionLocId(npc.getX(), npc.getY());
			final NpcSay msg = new NpcSay(npc, ChatType.NPC_SHOUT, "The defenders of " + npc.getCastle().getName() + " castle will be teleported to the inner castle.");
			npc.getCastle().oustAllPlayers();
			npc.setScriptValue(0);
			
			// TODO: Is it possible to get all the players for that region, instead of all players?
			for (Player pl : World.getInstance().getPlayers())
			{
				if (region == MapRegionData.getInstance().getMapRegionLocId(pl))
				{
					pl.sendPacket(msg);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Siege siege = npc.getCastle().getSiege();
		return npc.isScriptValue(0) ? (siege.isInProgress() && (siege.getControlTowerCount() == 0)) ? "teleporter-02.html" : "teleporter-01.html" : "teleporter-03.html";
	}
	
	public static void main(String[] args)
	{
		new CastleTeleporter();
	}
}
