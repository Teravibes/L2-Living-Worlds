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
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author godson
 */
public class ExOlympiadUserInfo extends ServerPacket
{
	private final Player _player;
	private final int _side;
	
	public ExOlympiadUserInfo(Player player, int side)
	{
		_player = player;
		_side = side;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_OLYMPIAD_USER_INFO.writeId(this, buffer);
		buffer.writeByte(_side);
		buffer.writeInt(_player.getObjectId());
		buffer.writeString(_player.getName());
		buffer.writeInt(_player.getPlayerClass().getId());
		buffer.writeInt((int) _player.getCurrentHp());
		buffer.writeInt(_player.getMaxHp());
		buffer.writeInt((int) _player.getCurrentCp());
		buffer.writeInt(_player.getMaxCp());
	}
}
