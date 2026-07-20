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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.RouteRecorder;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

public class ValidatePosition extends ClientPacket
{
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	
	@Override
	protected void readImpl()
	{
		_x = readInt();
		_y = readInt();
		_z = readInt();
		_heading = readInt();
		readInt(); // vehicle id
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || player.isTeleporting() || player.inObserverMode() || player.isCastingNow())
		{
			return;
		}
		
		final int realX = player.getX();
		final int realY = player.getY();
		int realZ = player.getZ();
		if ((_x == 0) && (_y == 0) && (realX != 0))
		{
			return;
		}
		
		if (player.isInVehicle())
		{
			return;
		}
		
		if (player.isFalling(_z))
		{
			return; // Disable validations during fall to avoid "jumping".
		}
		
		// Abnormal z read from client.
		if ((_z < -20000) || (_z > 20000))
		{
			final Location lastServerPosition = player.getLastServerPosition();
			if (lastServerPosition != null)
			{
				player.teleToLocation(lastServerPosition);
			}
			return;
		}
		
		final int dx = _x - realX;
		final int dy = _y - realY;
		final int dz = _z - realZ;
		final double diffSq = ((dx * dx) + (dy * dy));
		if (player.isFlying() || player.isInsideZone(ZoneId.WATER))
		{
			player.setXYZ(realX, realY, _z);
			if (diffSq > 90000)
			{
				player.sendPacket(new ValidateLocation(player));
			}
		}
		else if (diffSq < 360000) // If too large, messes observation.
		{
			if ((diffSq > 250000) || (Math.abs(dz) > 200))
			{
				if ((Math.abs(dz) > 200) && (Math.abs(dz) < 1500) && (Math.abs(_z - player.getClientZ()) < 800))
				{
					player.setXYZ(realX, realY, _z);
					realZ = _z;
				}
				else
				{
					player.sendPacket(new ValidateLocation(player));
				}
			}
		}
		
		// Check out of sync.
		if (player.calculateDistance3D(_x, _y, _z) > player.getStat().getMoveSpeed())
		{
			if (player.isBlinkActive())
			{
				ThreadPool.schedule(() -> player.setBlinkActive(false), 100);
			}
			else
			{
				player.setXYZ(_x, _y, realZ > _z ? GeoEngine.getInstance().getHeight(_x, _y, realZ) : _z);
			}
		}
		
		player.setClientX(_x);
		player.setClientY(_y);
		player.setClientZ(_z);
		player.setClientHeading(_heading); // No real need to validate heading.
		
		// Mobius: Check for possible door logout and move over exploit. Also checked at MoveToLocation.
		if (!DoorData.getInstance().checkIfDoorsBetween(realX, realY, realZ, _x, _y, _z, player.getInstanceId(), false))
		{
			player.setLastServerPosition(realX, realY, realZ);
		}

		if (player.isGM())
		{
			RouteRecorder.onPlayerMoved(player);
		}
	}
}
