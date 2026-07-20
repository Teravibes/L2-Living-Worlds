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
import org.l2jmobius.gameserver.LoginServerThread.SessionKey;

/**
 * Sends the session key parts (play/login) for an account to the login server.<br>
 * Accepts {@code LoginServerThread.SessionKey} to match callsites.
 * <ul>
 * <li>Opcode: 0x05.</li>
 * <li>Payload: account (String), playOk1 (int), playOk2 (int), loginOk1 (int), loginOk2 (int).</li>
 * </ul>
 * @author BazookaRpm
 */
public class PlayerAuthRequest extends BaseWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x05;
	
	// Data.
	private final String _account;
	private final SessionKey _key;
	
	/**
	 * @param account account name
	 * @param key session key (LoginServerThread.SessionKey)
	 */
	public PlayerAuthRequest(String account, SessionKey key)
	{
		_account = (account != null) ? account : "";
		_key = key;
	}
	
	/**
	 * Serializes opcode, account and the four session integers.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_account);
		writeInt((_key != null) ? _key.playOkID1 : 0);
		writeInt((_key != null) ? _key.playOkID2 : 0);
		writeInt((_key != null) ? _key.loginOkID1 : 0);
		writeInt((_key != null) ? _key.loginOkID2 : 0);
	}
}
