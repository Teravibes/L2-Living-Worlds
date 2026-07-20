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
package org.l2jmobius.loginserver.network;

import java.util.logging.Logger;

import org.l2jmobius.commons.network.PacketHandler;
import org.l2jmobius.commons.network.ReadableBuffer;
import org.l2jmobius.commons.network.ReadablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.loginserver.enums.LoginFailReason;

/**
 * @author Mobius
 */
public class LoginPacketHandler implements PacketHandler<LoginClient>
{
	private static final Logger LOGGER = Logger.getLogger(LoginPacketHandler.class.getName());
	
	@Override
	public ReadablePacket<LoginClient> handlePacket(ReadableBuffer buffer, LoginClient client)
	{
		// Read packet id.
		final int packetId;
		try
		{
			packetId = Byte.toUnsignedInt(buffer.readByte());
		}
		catch (Exception e)
		{
			LOGGER.warning("LoginPacketHandler: Problem receiving packet id from " + client);
			LOGGER.warning(TraceUtil.getStackTrace(e));
			client.close(LoginFailReason.REASON_ACCESS_FAILED);
			return null;
		}
		
		// Check if packet id is within valid range.
		if ((packetId < 0) || (packetId >= LoginClientPackets.PACKET_ARRAY.length))
		{
			return null;
		}
		
		// Find packet enum.
		final LoginClientPackets packetEnum = LoginClientPackets.PACKET_ARRAY[packetId];
		if (packetEnum == null)
		{
			return null;
		}
		
		// Check connection state.
		if (!packetEnum.getConnectionStates().contains(client.getConnectionState()))
		{
			return null;
		}
		
		// Create new LoginClientPacket.
		return packetEnum.newPacket();
	}
}
