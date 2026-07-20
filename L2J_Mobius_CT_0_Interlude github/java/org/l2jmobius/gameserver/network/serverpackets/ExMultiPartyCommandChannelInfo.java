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
import org.l2jmobius.gameserver.model.groups.CommandChannel;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author chris_00
 */
public class ExMultiPartyCommandChannelInfo extends ServerPacket
{
	private final CommandChannel _channel;
	
	public ExMultiPartyCommandChannelInfo(CommandChannel channel)
	{
		_channel = channel;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_channel == null)
		{
			return;
		}
		
		ServerPackets.EX_MULTI_PARTY_COMMAND_CHANNEL_INFO.writeId(this, buffer);
		buffer.writeString(_channel.getLeader().getName());
		buffer.writeInt(0); // Channel loot 0 or 1
		buffer.writeInt(_channel.getMemberCount());
		buffer.writeInt(_channel.getParties().size());
		for (Party p : _channel.getParties())
		{
			buffer.writeString(p.getLeader().getName());
			buffer.writeInt(p.getLeaderObjectId());
			buffer.writeInt(p.getMemberCount());
		}
	}
}
