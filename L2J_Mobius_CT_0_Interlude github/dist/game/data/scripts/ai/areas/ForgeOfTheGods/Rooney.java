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
package ai.areas.ForgeOfTheGods;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Rooney AI
 * @author malyelfik
 */
public class Rooney extends Script
{
	// NPC
	private static final int ROONEY = 32049;
	
	// Locations
	private static final Location[] LOCATIONS =
	{
		new Location(175937, -112167, -5550),
		new Location(178896, -112425, -5860),
		new Location(180628, -115992, -6135),
		new Location(183010, -114753, -6135),
		new Location(184496, -116773, -6135),
		new Location(181857, -109491, -5865),
		new Location(178917, -107633, -5853),
		new Location(178804, -110080, -5853),
		new Location(182221, -106806, -6025),
		new Location(186488, -109715, -5915),
		new Location(183847, -119231, -3113),
		new Location(185193, -120342, -3113),
		new Location(188047, -120867, -3113),
		new Location(189734, -120471, -3113),
		new Location(188754, -118940, -3313),
		new Location(190022, -116803, -3313),
		new Location(188443, -115814, -3313),
		new Location(186421, -114614, -3313),
		new Location(185188, -113307, -3313),
		new Location(187378, -112946, -3313),
		new Location(189815, -113425, -3313),
		new Location(189301, -111327, -3313),
		new Location(190289, -109176, -3313),
		new Location(187783, -110478, -3313),
		new Location(185889, -109990, -3313),
		new Location(181881, -109060, -3695),
		new Location(183570, -111344, -3675),
		new Location(182077, -112567, -3695),
		new Location(180127, -112776, -3698),
		new Location(179155, -108629, -3695),
		new Location(176282, -109510, -3698),
		new Location(176071, -113163, -3515),
		new Location(179376, -117056, -3640),
		new Location(179760, -115385, -3640),
		new Location(177950, -119691, -4140),
		new Location(177037, -120820, -4340),
		new Location(181125, -120148, -3702),
		new Location(182212, -117969, -3352),
		new Location(186074, -118154, -3312)
	};
	
	private Rooney()
	{
		addCreatureSeeId(ROONEY);
		addSpawn(ROONEY, getRandomEntry(LOCATIONS), false, 0);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("TELEPORT") && !npc.isDecayed())
		{
			final int aiVal = npc.getScriptValue();
			switch (aiVal)
			{
				case 1:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, "Hurry hurry");
					break;
				}
				case 2:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, "I am not that type of person who stays in one place for a long time");
					break;
				}
				case 3:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, "It's hard for me to keep standing like this");
					break;
				}
				case 4:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, "Why don't I go that way this time");
					break;
				}
				default:
				{
					npc.teleToLocation(getRandomEntry(LOCATIONS), false);
					npc.setScriptValue(0);
					return null;
				}
			}
			
			npc.setScriptValue(aiVal + 1);
			startQuestTimer("TELEPORT", 60000, npc, null);
		}
		
		return null;
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, "Welcome!");
			startQuestTimer("TELEPORT", 60000, npc, null);
			npc.setScriptValue(1);
		}
	}
	
	public static void main(String[] args)
	{
		new Rooney();
	}
}
