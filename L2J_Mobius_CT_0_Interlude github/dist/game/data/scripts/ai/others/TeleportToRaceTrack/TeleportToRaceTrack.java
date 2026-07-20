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
package ai.others.TeleportToRaceTrack;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Monster Derby Track teleport AI.
 * @author Mobius
 */
public class TeleportToRaceTrack extends Script
{
	// NPC
	private static final int RACE_MANAGER = 30995;
	
	// Locations
	private static final Location RACE_TRACK_TELEPORT = new Location(12661, 181687, -3540);
	private static final Map<Integer, Location> TELEPORTER_LOCATIONS = new HashMap<>();
	static
	{
		TELEPORTER_LOCATIONS.put(30320, new Location(-80826, 149775, -3043)); // Richlin
		TELEPORTER_LOCATIONS.put(30256, new Location(-12672, 122776, -3116)); // Bella
		TELEPORTER_LOCATIONS.put(30059, new Location(15670, 142983, -2705)); // Trisha
		TELEPORTER_LOCATIONS.put(30080, new Location(83400, 147943, -3404)); // Clarissa
		TELEPORTER_LOCATIONS.put(30899, new Location(111409, 219364, -3545)); // Flauen
		TELEPORTER_LOCATIONS.put(30177, new Location(82956, 53162, -1495)); // Valentina
		TELEPORTER_LOCATIONS.put(30848, new Location(146331, 25762, -2018)); // Elisa
		TELEPORTER_LOCATIONS.put(30233, new Location(116819, 76994, -2714)); // Esmeralda
		TELEPORTER_LOCATIONS.put(31320, new Location(43835, -47749, -792)); // Ilyana
		TELEPORTER_LOCATIONS.put(31275, new Location(147930, -55281, -2728)); // Tatiana
		TELEPORTER_LOCATIONS.put(31964, new Location(87386, -143246, -1293)); // Bilia
		TELEPORTER_LOCATIONS.put(31210, new Location(12882, 181053, -3560)); // Race Track Gatekeeper
	}
	
	// Other
	private static final String MONSTER_RETURN = "MONSTER_RETURN";
	
	private TeleportToRaceTrack()
	{
		addStartNpc(RACE_MANAGER);
		addStartNpc(TELEPORTER_LOCATIONS.keySet());
		addTalkId(RACE_MANAGER);
		addTalkId(TELEPORTER_LOCATIONS.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("TELEPORT"))
		{
			if (npc.getId() == RACE_MANAGER)
			{
				final int returnId = player.getVariables().getInt(MONSTER_RETURN, -1);
				if (returnId > 30000)
				{
					player.teleToLocation(TELEPORTER_LOCATIONS.get(returnId));
					player.getVariables().remove(MONSTER_RETURN);
				}
				else
				{
					player.teleToLocation(TELEPORTER_LOCATIONS.get(30059)); // Default to Dion
				}
			}
			else
			{
				player.teleToLocation(RACE_TRACK_TELEPORT);
				player.getVariables().set(MONSTER_RETURN, npc.getId());
			}
			
			return null;
		}
		
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new TeleportToRaceTrack();
	}
}
