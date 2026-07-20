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
package org.l2jmobius.gameserver.network.loginserverpackets.game;

import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.base.BaseWritablePacket;

/**
 * Game server authentication request to the login server.<br>
 * Signature compatible with existing callsites.
 * <ul>
 * <li>Opcode: 0x01.</li>
 * <li>Payload: id (int), acceptAlt (byte), reserveHost (byte), port (short), maxPlayers (int), hexIdLen (int), hexId (bytes), pairs (int), subnets/hosts (String pairs).</li>
 * </ul>
 * @author BazookaRpm
 */
public class AuthRequest extends BaseWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x01;
	
	// Data.
	private final int _id;
	private final boolean _acceptAlternate;
	private final byte[] _hexId;
	private final int _port;
	private final boolean _reserveHost;
	private final int _maxPlayers;
	private final List<String> _subnets;
	private final List<String> _hosts;
	
	/**
	 * Auth constructor matching (id, acceptAlternate, hexid, port, reserveHost, maxplayer, subnets, hosts).
	 * @param id server id
	 * @param acceptAlternate allow alternate ID
	 * @param hexid server hexadecimal id
	 * @param port bind port
	 * @param reserveHost reserve host flag
	 * @param maxplayer max players
	 * @param subnets subnet list
	 * @param hosts host list
	 */
	public AuthRequest(int id, boolean acceptAlternate, byte[] hexid, int port, boolean reserveHost, int maxplayer, List<String> subnets, List<String> hosts)
	{
		_id = id;
		_acceptAlternate = acceptAlternate;
		_hexId = (hexid != null) ? hexid : new byte[0];
		_port = port;
		_reserveHost = reserveHost;
		_maxPlayers = maxplayer;
		_subnets = (subnets != null) ? subnets : Collections.emptyList();
		_hosts = (hosts != null) ? hosts : Collections.emptyList();
	}
	
	/**
	 * Serializes the full authentication payload including hexId and subnet/host pairs.
	 */
	@Override
	public void write()
	{
		final int pairs = Math.min(_subnets.size(), _hosts.size());
		
		writeByte(OPCODE);
		writeByte(_id);
		writeByte(_acceptAlternate ? 0x01 : 0x00);
		writeByte(_reserveHost ? 0x01 : 0x00);
		writeShort(_port);
		writeInt(_maxPlayers);
		writeInt(_hexId.length);
		writeBytes(_hexId);
		writeInt(pairs);
		for (int i = 0; i < pairs; i++)
		{
			writeString((_subnets.get(i) != null) ? _subnets.get(i) : "");
			writeString((_hosts.get(i) != null) ? _hosts.get(i) : "");
		}
	}
}
