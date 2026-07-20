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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Maktakien
 */
public class GetOnVehicle extends ServerPacket
{
	private final int _objectId;
	private final int _boatObjId;
	private final Location _pos;
	
	/**
	 * @param charObjId
	 * @param boatObjId
	 * @param pos
	 */
	public GetOnVehicle(int charObjId, int boatObjId, Location pos)
	{
		_objectId = charObjId;
		_boatObjId = boatObjId;
		_pos = pos;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.GET_ON_VEHICLE.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_boatObjId);
		buffer.writeInt(_pos.getX());
		buffer.writeInt(_pos.getY());
		buffer.writeInt(_pos.getZ());
	}
}
