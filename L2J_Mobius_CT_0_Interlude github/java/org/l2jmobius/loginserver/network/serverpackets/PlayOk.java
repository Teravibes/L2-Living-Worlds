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
package org.l2jmobius.loginserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.loginserver.SessionKey;
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * PlayOk packet that authorizes the client to enter the gameserver.<br>
 * Serializes the PlayOk pair required by the protocol.
 * <ul>
 * <li>Opcode 0x07.</li>
 * <li>Writes two PlayOk integers.</li>
 * </ul>
 * author Mobius, BazookaRpm
 */
public class PlayOk extends LoginServerPacket
{
	// Constants.
	private static final int OPCODE_PLAY_OK = 0x07;
	
	// Session key parts.
	private final int _playOkPart1;
	private final int _playOkPart2;
	
	/**
	 * Constructs the packet with the PlayOk pair.
	 * @param sessionKey
	 */
	public PlayOk(SessionKey sessionKey)
	{
		_playOkPart1 = sessionKey.getPlayOkID1();
		_playOkPart2 = sessionKey.getPlayOkID2();
	}
	
	/**
	 * Writes the PlayOk packet to the client buffer.
	 * @param client
	 * @param buffer
	 */
	@Override
	protected void writeImpl(LoginClient client, WritableBuffer buffer)
	{
		buffer.writeByte(OPCODE_PLAY_OK);
		buffer.writeInt(_playOkPart1);
		buffer.writeInt(_playOkPart2);
	}
}
