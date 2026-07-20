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

import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.network.gameserverpackets.BlowFishKey;
import org.l2jmobius.loginserver.network.gameserverpackets.ChangeAccessLevel;
import org.l2jmobius.loginserver.network.gameserverpackets.ChangePassword;
import org.l2jmobius.loginserver.network.gameserverpackets.GameServerAuth;
import org.l2jmobius.loginserver.network.gameserverpackets.PlayerAuthRequest;
import org.l2jmobius.loginserver.network.gameserverpackets.PlayerInGame;
import org.l2jmobius.loginserver.network.gameserverpackets.PlayerLogout;
import org.l2jmobius.loginserver.network.gameserverpackets.PlayerTracert;
import org.l2jmobius.loginserver.network.gameserverpackets.ReplyCharacters;
import org.l2jmobius.loginserver.network.gameserverpackets.RequestTempBan;
import org.l2jmobius.loginserver.network.gameserverpackets.ServerStatus;
import org.l2jmobius.loginserver.network.loginserverpackets.LoginServerFail;

/**
 * Handles routing of packets received from game server connections.
 * @author BazookaRpm
 */
public class GameServerPacketHandler
{
	private static final Logger LOGGER = Logger.getLogger(GameServerPacketHandler.class.getName());
	
	public enum GameServerState
	{
		CONNECTED,
		BF_CONNECTED,
		AUTHED
	}
	
	private GameServerPacketHandler()
	{
	}
	
	public static BaseReadablePacket handlePacket(byte[] data, GameServerThread server)
	{
		if ((data == null) || (data.length == 0))
		{
			LOGGER.warning("Received empty packet from " + server);
			server.forceClose(LoginServerFail.NOT_AUTHED);
			return null;
		}
		
		final int opcode = data[0] & 0xFF;
		final GameServerState state = server.getLoginConnectionState();
		switch (state)
		{
			case CONNECTED:
			{
				switch (opcode)
				{
					case 0x00:
					{
						return new BlowFishKey(data, server);
					}
					default:
					{
						logInvalidOpcode(opcode, state, server);
						return null;
					}
				}
			}
			case BF_CONNECTED:
			{
				switch (opcode)
				{
					case 0x01:
					{
						return new GameServerAuth(data, server);
					}
					default:
					{
						logInvalidOpcode(opcode, state, server);
						return null;
					}
				}
			}
			case AUTHED:
			{
				switch (opcode)
				{
					case 0x02:
					{
						return new PlayerInGame(data, server);
					}
					case 0x03:
					{
						return new PlayerLogout(data, server);
					}
					case 0x04:
					{
						return new ChangeAccessLevel(data, server);
					}
					case 0x05:
					{
						return new PlayerAuthRequest(data, server);
					}
					case 0x06:
					{
						return new ServerStatus(data, server);
					}
					case 0x07:
					{
						return new PlayerTracert(data);
					}
					case 0x08:
					{
						return new ReplyCharacters(data, server);
					}
					case 0x09: // RequestSendMail (not implemented)
					{
						return null;
					}
					case 0x0A:
					{
						return new RequestTempBan(data);
					}
					case 0x0B:
					{
						new ChangePassword(data);
						return null;
					}
					default:
					{
						logInvalidOpcode(opcode, state, server);
						return null;
					}
				}
			}
			default:
			{
				LOGGER.warning("Unknown state " + state + " from " + server);
				server.forceClose(LoginServerFail.NOT_AUTHED);
				return null;
			}
		}
	}
	
	private static void logInvalidOpcode(int opcode, GameServerState state, GameServerThread server)
	{
		LOGGER.warning("Unknown opcode (" + Integer.toHexString(opcode).toUpperCase() + ") in state " + state + " from " + server + ", closing connection.");
		server.forceClose(LoginServerFail.NOT_AUTHED);
	}
}
