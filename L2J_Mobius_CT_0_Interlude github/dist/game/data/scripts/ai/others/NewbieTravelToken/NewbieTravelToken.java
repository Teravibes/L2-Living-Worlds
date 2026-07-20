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
package ai.others.NewbieTravelToken;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Newbie Travel Token AI.
 * @author Plim
 */
public class NewbieTravelToken extends Script
{
	// Item
	private static final int NEWBIE_TRAVEL_TOKEN = 8542;
	
	// NPC Id - Teleport Location
	private static final Map<Integer, Location> DATA = new HashMap<>();
	
	private NewbieTravelToken()
	{
		// Initialize Map
		DATA.put(30600, new Location(12160, 16554, -4583)); // DE
		DATA.put(30601, new Location(115594, -177993, -912)); // DW
		DATA.put(30599, new Location(45470, 48328, -3059)); // EV
		DATA.put(30602, new Location(-45067, -113563, -199)); // OV
		DATA.put(30598, new Location(-84053, 243343, -3729)); // TI
		
		for (int npcId : DATA.keySet())
		{
			addStartNpc(npcId);
			addTalkId(npcId);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (StringUtil.isNumeric(event))
		{
			final int npcId = Integer.parseInt(event);
			if (DATA.containsKey(npcId))
			{
				if (hasQuestItems(player, NEWBIE_TRAVEL_TOKEN))
				{
					takeItems(player, NEWBIE_TRAVEL_TOKEN, 1);
					player.teleToLocation(DATA.get(npcId), false);
				}
				else
				{
					player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
				}
				
				return super.onEvent(event, npc, player);
			}
		}
		
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		return player.getLevel() >= 20 ? "cant-travel.htm" : npc.getId() + ".htm";
	}
	
	public static void main(String[] args)
	{
		new NewbieTravelToken();
	}
}
