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
package handlers.bypass.npc;

import java.util.logging.Level;

import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.BroadcastingTower;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class Observation implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"observesiege",
		"observeoracle",
		"observe"
	};
	
	private static final int[][] LOCATIONS =
	{
		// @formatter:off
		// Gludio
		{-18347, 114000, -2360, 500},
		{-18347, 113090, -2085, 500},
		// Dion
		{22321, 155785, -2604, 500},
		{22321, 156693, -2279, 500},
		// Giran
		{112000, 144864, -2445, 500},
		{112833, 144859, -2152, 500},
		// Innadril
		{116260, 244600, -775, 500},
		{116260, 245446, -374, 500},
		// Oren
		{78100, 36950, -2242, 500},
		{78947, 36955, -1867, 500},
		// Aden
		{147457, 9601, -233, 500},
		{147665, 8361, 479, 500},
		// Goddard
		{147542, -43543, -1328, 500},
		{147471, -45631, -1347, 500},
		// Rune
		{20598, -49113, -300, 500},
		{18246, -49156, -574, 500},
		// Schuttgart
		{77541, -147447, 353, 500},
		{77541, -149722, 363, 500},
		// Coliseum
		{148416, 46724, -3000, 80},
		{149500, 46724, -3000, 80},
		{150511, 46724, -3000, 80},
		// Dusk
		{-77200, 88500, -4800, 500},
		{-75320, 87135, -4800, 500},
		{-76840, 85770, -4800, 500},
		{-76840, 85770, -4800, 500},
		{-79950, 85165, -4800, 500},
		// Dawn
		{-79185, 112725, -4300, 500},
		{-76175, 113330, -4300, 500},
		{-74305, 111965, -4300, 500},
		{-75915, 110600, -4300, 500},
		{-78930, 110005, -4300, 500},
		// Monster Race Track
		{13152, 182546, -3207, 0},
		{11905, 182073, -3235, 0}
		// @formatter:on
	};
	
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!(target instanceof BroadcastingTower))
		{
			return false;
		}
		
		if (player.hasSummon())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_OBSERVE_A_SIEGE_WITH_A_PET_OR_SERVITOR_SUMMONED);
			return false;
		}
		
		if (player.isRegisteredOnEvent())
		{
			player.sendMessage("Cannot use while registered on an event");
			return false;
		}
		
		if (player.isInCombat())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_OBSERVE_WHILE_YOU_ARE_IN_COMBAT);
			return false;
		}
		
		final String _command = command.split(" ")[0].toLowerCase();
		final int param;
		try
		{
			param = Integer.parseInt(command.split(" ")[1]);
		}
		catch (NumberFormatException nfe)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), nfe);
			return false;
		}
		
		if ((param < 0) || (param > (LOCATIONS.length - 1)))
		{
			return false;
		}
		
		final int[] locCost = LOCATIONS[param];
		final Location loc = new Location(locCost[0], locCost[1], locCost[2]);
		final int cost = locCost[3];
		
		switch (_command)
		{
			case "observesiege":
			{
				if (SiegeManager.getInstance().getSiege(loc) != null)
				{
					doObserve(player, target.asNpc(), loc, cost);
				}
				else
				{
					player.sendPacket(SystemMessageId.OBSERVATION_IS_ONLY_POSSIBLE_DURING_A_SIEGE);
				}
				
				return true;
			}
			case "observeoracle": // Oracle Dusk/Dawn
			{
				doObserve(player, target.asNpc(), loc, cost);
				return true;
			}
			case "observe": // Observe
			{
				doObserve(player, target.asNpc(), loc, cost);
				return true;
			}
		}
		
		return false;
	}
	
	private void doObserve(Player player, Npc npc, Location pos, int cost)
	{
		if (player.reduceAdena(ItemProcessType.FEE, cost, npc, true))
		{
			// enter mode
			player.enterObserverMode(pos);
			player.sendItemList(false);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
