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

package org.l2jmobius.loginserver.network.gameserverpackets;

import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.LoginController;
import org.l2jmobius.loginserver.SessionKey;
import org.l2jmobius.loginserver.network.loginserverpackets.PlayerAuthResponse;

/**
 * Reads an authentication request from a game server and validates it against the stored session key.<br>
 * On success, the login client is removed from the authenticated list and a positive response is sent.
 * <ul>
 * <li>Consumes already-processed packet id.</li>
 * <li>Builds a {@link SessionKey} from four 32-bit integers.</li>
 * <li>Compares using {@code SessionKey.equals(Object)}.</li>
 * </ul>
 * @author -Wooden-, BazookaRpm
 */
public class PlayerAuthRequest extends BaseReadablePacket
{
	public PlayerAuthRequest(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		final String account = readString();
		final int playKey1 = readInt();
		final int playKey2 = readInt();
		final int loginKey1 = readInt();
		final int loginKey2 = readInt();
		
		final SessionKey sessionKey = new SessionKey(loginKey1, loginKey2, playKey1, playKey2);
		final SessionKey storedKey = LoginController.getInstance().getKeyForAccount(account);
		
		if ((storedKey != null) && storedKey.equals(sessionKey))
		{
			LoginController.getInstance().removeAuthedLoginClient(account);
			server.sendPacket(new PlayerAuthResponse(account, true));
		}
		else
		{
			server.sendPacket(new PlayerAuthResponse(account, false));
		}
	}
}
