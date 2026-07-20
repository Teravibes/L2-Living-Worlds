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

import org.l2jmobius.commons.network.base.BaseWritablePacket;

/**
 * Sends the player's PC IP and up to four routing hops (tracert) to the login server.<br>
 * Empty strings are allowed.
 * <ul>
 * <li>Opcode: 0x07.</li>
 * <li>Payload: account, pcIp, hop1..hop4 (Strings).</li>
 * </ul>
 * @author BazookaRpm
 */
public class PlayerTracert extends BaseWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x07;
	
	// Data.
	private final String _account;
	private final String _pcIp;
	private final String _hop1;
	private final String _hop2;
	private final String _hop3;
	private final String _hop4;
	
	/**
	 * @param account account name
	 * @param pcIp player IP
	 * @param hop1 first hop
	 * @param hop2 second hop
	 * @param hop3 third hop
	 * @param hop4 fourth hop
	 */
	public PlayerTracert(String account, String pcIp, String hop1, String hop2, String hop3, String hop4)
	{
		_account = (account != null) ? account : "";
		_pcIp = (pcIp != null) ? pcIp : "";
		_hop1 = (hop1 != null) ? hop1 : "";
		_hop2 = (hop2 != null) ? hop2 : "";
		_hop3 = (hop3 != null) ? hop3 : "";
		_hop4 = (hop4 != null) ? hop4 : "";
	}
	
	/**
	 * Serializes opcode, account, pcIp and four hops.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_account);
		writeString(_pcIp);
		writeString(_hop1);
		writeString(_hop2);
		writeString(_hop3);
		writeString(_hop4);
	}
}
