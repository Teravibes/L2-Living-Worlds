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
 * Notifies the login server that a player has logged out.
 * <ul>
 * <li>Opcode: 0x03.</li>
 * <li>Payload: player name (String).</li>
 * </ul>
 * @author BazookaRpm
 */
public class PlayerLogout extends BaseWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x03;
	
	// Data.
	private final String _player;
	
	/**
	 * @param player player name
	 */
	public PlayerLogout(String player)
	{
		_player = (player != null) ? player : "";
	}
	
	/**
	 * Serializes opcode and player name.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_player);
	}
}
