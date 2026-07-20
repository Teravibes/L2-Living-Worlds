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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.loginserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.loginserver.enums.LoginFailReason;
import org.l2jmobius.loginserver.network.ConnectionState;
import org.l2jmobius.loginserver.network.serverpackets.GGAuth;

/**
 * Handles the client GameGuard authentication packet.<br>
 * Validates the received session identifier and advances the connection state when successful.
 * <ul>
 * <li>Reads and discards the GameGuard payload integers.</li>
 * <li>Verifies that the received session id matches the client session id.</li>
 * <li>Sets {@link ConnectionState#AUTHED_GG} and replies with {@link GGAuth} on success.</li>
 * <li>Closes the connection with {@link LoginFailReason#REASON_ACCESS_FAILED} on mismatch.</li>
 * </ul>
 * Format: ddddd.
 * @author BazookaRpm
 */
public class AuthGameGuard extends LoginClientPacket
{
	private static final Logger LOGGER = Logger.getLogger(AuthGameGuard.class.getName());
	
	private static final int GAME_GUARD_PAYLOAD_SIZE = 20;
	private static final int GAME_GUARD_RESERVED_DWORD_COUNT = 4;
	
	private int _receivedSessionId;
	
	/**
	 * Reads and validates the GameGuard authentication payload.<br>
	 * Expects one session id followed by four reserved integers.
	 * @return {@code true} if the payload was fully read, {@code false} otherwise.
	 */
	@Override
	protected boolean readImpl()
	{
		final int remainingBytes = remaining();
		if (remainingBytes < GAME_GUARD_PAYLOAD_SIZE)
		{
			if (LOGGER.isLoggable(Level.FINE))
			{
				LOGGER.fine("AuthGameGuard: Invalid payload length " + remainingBytes + " (expected " + GAME_GUARD_PAYLOAD_SIZE + ").");
			}
			return false;
		}
		
		_receivedSessionId = readInt();
		
		for (int i = 0; i < GAME_GUARD_RESERVED_DWORD_COUNT; i++)
		{
			readInt(); // Reserved GameGuard data.
		}
		
		return true;
	}
	
	/**
	 * Validates the GameGuard session and updates the client connection state.
	 */
	@Override
	public void run()
	{
		final int clientSessionId = getClient().getSessionId();
		if (_receivedSessionId != clientSessionId)
		{
			if (LOGGER.isLoggable(Level.FINE))
			{
				LOGGER.fine("AuthGameGuard: Session mismatch. Received=" + _receivedSessionId + ", expected=" + clientSessionId + ".");
			}
			
			getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
			return;
		}
		
		getClient().setConnectionState(ConnectionState.AUTHED_GG);
		getClient().sendPacket(new GGAuth(clientSessionId));
	}
}
