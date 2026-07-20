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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.l2jmobius.loginserver.network.clientpackets.AuthGameGuard;
import org.l2jmobius.loginserver.network.clientpackets.LoginClientPacket;
import org.l2jmobius.loginserver.network.clientpackets.RequestAuthLogin;
import org.l2jmobius.loginserver.network.clientpackets.RequestServerList;
import org.l2jmobius.loginserver.network.clientpackets.RequestServerLogin;

/**
 * @author Mobius
 */
public enum LoginClientPackets
{
	AUTH_GAME_GUARD(0x07, AuthGameGuard::new, ConnectionState.CONNECTED),
	REQUEST_AUTH_LOGIN(0x00, RequestAuthLogin::new, ConnectionState.AUTHED_GG),
	REQUEST_SERVER_LOGIN(0x02, RequestServerLogin::new, ConnectionState.AUTHED_LOGIN),
	REQUEST_SERVER_LIST(0x05, RequestServerList::new, ConnectionState.AUTHED_LOGIN),
	REQUEST_PI_AGREEMENT_CHECK(0x0E, null, ConnectionState.AUTHED_LOGIN),
	REQUEST_PI_AGREEMENT(0x0F, null, ConnectionState.AUTHED_LOGIN);
	
	public static final LoginClientPackets[] PACKET_ARRAY;
	static
	{
		final short maxPacketId = (short) Arrays.stream(values()).mapToInt(LoginClientPackets::getPacketId).max().orElse(0);
		PACKET_ARRAY = new LoginClientPackets[maxPacketId + 1];
		for (LoginClientPackets packet : values())
		{
			PACKET_ARRAY[packet.getPacketId()] = packet;
		}
	}
	
	private final short _packetId;
	private final Supplier<LoginClientPacket> _packetSupplier;
	private final Set<ConnectionState> _connectionStates;
	
	LoginClientPackets(int packetId, Supplier<LoginClientPacket> packetSupplier, ConnectionState... connectionStates)
	{
		// Packet id is an unsigned byte.
		if (packetId > 0xFF)
		{
			throw new IllegalArgumentException("Packet id must not be bigger than 0xFF");
		}
		
		_packetId = (short) packetId;
		_packetSupplier = packetSupplier != null ? packetSupplier : () -> null;
		_connectionStates = new HashSet<>(Arrays.asList(connectionStates));
	}
	
	public int getPacketId()
	{
		return _packetId;
	}
	
	public LoginClientPacket newPacket()
	{
		return _packetSupplier.get();
	}
	
	public Set<ConnectionState> getConnectionStates()
	{
		return _connectionStates;
	}
}
