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
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author devScarlet, mrTJO
 */
public class ServerObjectInfo extends ServerPacket
{
	private final Npc _activeChar;
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _heading;
	private final int _displayId;
	private final boolean _isAttackable;
	private final double _collisionHeight;
	private final double _collisionRadius;
	private final String _name;
	
	public ServerObjectInfo(Npc activeChar, Creature actor)
	{
		_activeChar = activeChar;
		_displayId = _activeChar.getTemplate().getDisplayId();
		_isAttackable = _activeChar.isAutoAttackable(actor);
		_collisionHeight = _activeChar.getCollisionHeight();
		_collisionRadius = _activeChar.getCollisionRadius();
		_x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_name = _activeChar.getTemplate().isUsingServerSideName() ? _activeChar.getTemplate().getName() : "";
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SERVER_OBJECT_INFO.writeId(this, buffer);
		buffer.writeInt(_activeChar.getObjectId());
		buffer.writeInt(_displayId + 1000000);
		buffer.writeString(_name); // name
		buffer.writeInt(_isAttackable);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(_heading);
		buffer.writeDouble(1.0); // movement multiplier
		buffer.writeDouble(1.0); // attack speed multiplier
		buffer.writeDouble(_collisionRadius);
		buffer.writeDouble(_collisionHeight);
		buffer.writeInt((int) (_isAttackable ? _activeChar.getCurrentHp() : 0));
		buffer.writeInt(_isAttackable ? _activeChar.getMaxHp() : 0);
		buffer.writeInt(1); // object type
		buffer.writeInt(0); // special effects
	}
}
