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
package handlers.items;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.Dice;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.LocationUtil;

public class RollingDice implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		final Player player = playable.asPlayer();
		final int itemId = item.getId();
		if (player.isInOlympiadMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THAT_ITEM_IN_A_GRAND_OLYMPIAD_GAMES_MATCH);
			return false;
		}
		
		final int number = rollDice(player);
		if (number == 0)
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIME_TRY_AGAIN_LATER);
			return false;
		}
		
		// Mobius: Retail dice position land calculation.
		final double angle = LocationUtil.convertHeadingToDegree(player.getHeading());
		final double radian = Math.toRadians(angle);
		final double course = Math.toRadians(180);
		final int x1 = (int) (Math.cos(Math.PI + radian + course) * 40);
		final int y1 = (int) (Math.sin(Math.PI + radian + course) * 40);
		final int x = player.getX() + x1;
		final int y = player.getY() + y1;
		final int z = player.getZ();
		final Location destination = GeoEngine.getInstance().getValidLocation(player.getX(), player.getY(), player.getZ(), x, y, z, player.getInstanceId());
		Broadcast.toSelfAndKnownPlayers(player, new Dice(player.getObjectId(), itemId, number, destination.getX(), destination.getY(), destination.getZ()));
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ROLLED_S2);
		sm.addString(player.getName());
		sm.addInt(number);
		
		player.sendPacket(sm);
		if (player.isInsideZone(ZoneId.PEACE))
		{
			Broadcast.toKnownPlayers(player, sm);
		}
		else if (player.isInParty()) // TODO: Verify this!
		{
			player.getParty().broadcastToPartyMembers(player, sm);
		}
		
		return true;
	}
	
	private int rollDice(Player player)
	{
		// Check if the dice is ready
		if (!player.getClient().getFloodProtectors().canRollDice())
		{
			return 0;
		}
		
		return Rnd.get(1, 6);
	}
}
