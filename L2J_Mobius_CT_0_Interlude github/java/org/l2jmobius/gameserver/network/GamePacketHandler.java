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
package org.l2jmobius.gameserver.network;

import java.util.logging.Logger;

import org.l2jmobius.commons.network.PacketHandler;
import org.l2jmobius.commons.network.ReadableBuffer;
import org.l2jmobius.commons.network.ReadablePacket;
import org.l2jmobius.commons.util.TraceUtil;

/**
 * @author Mobius
 */
public class GamePacketHandler implements PacketHandler<GameClient>
{
	private static final Logger LOGGER = Logger.getLogger(GamePacketHandler.class.getName());
	
	@Override
	public ReadablePacket<GameClient> handlePacket(ReadableBuffer buffer, GameClient client)
	{
		// Read packet id.
		final int packetId;
		try
		{
			packetId = Byte.toUnsignedInt(buffer.readByte());
		}
		catch (Exception e)
		{
			LOGGER.warning("PacketHandler: Problem receiving packet id from " + client);
			LOGGER.warning(TraceUtil.getStackTrace(e));
			client.closeNow();
			return null;
		}
		
		switch (packetId)
		{
			case 0xD0: // Ex client packet.
			{
				// Check if packet id is within valid range.
				final int exPacketId = Short.toUnsignedInt(buffer.readShort());
				if ((exPacketId < 0) || (exPacketId >= ExClientPackets.PACKET_ARRAY.length))
				{
					return null;
				}
				
				// Find packet enum.
				final ExClientPackets packetEnum = ExClientPackets.PACKET_ARRAY[exPacketId];
				if (packetEnum == null)
				{
					return null;
				}
				
				// Check connection state.
				if (!packetEnum.getConnectionStates().contains(client.getConnectionState()))
				{
					return null;
				}
				
				// Create new ClientPacket.
				return packetEnum.newPacket();
			}
			default: // Normal client packet.
			{
				// Check if packet id is within valid range.
				if ((packetId < 0) || (packetId >= ClientPackets.PACKET_ARRAY.length))
				{
					return null;
				}
				
				// Find packet enum.
				final ClientPackets packetEnum = ClientPackets.PACKET_ARRAY[packetId];
				if (packetEnum == null)
				{
					return null;
				}
				
				// Check connection state.
				if (!packetEnum.getConnectionStates().contains(client.getConnectionState()))
				{
					return null;
				}
				
				// Create new ClientPacket.
				return packetEnum.newPacket();
			}
		}
	}
}
