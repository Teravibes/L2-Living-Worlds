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

import java.util.Arrays;
import java.util.logging.Logger;

import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.config.LoginConfig;
import org.l2jmobius.loginserver.network.GameServerPacketHandler.GameServerState;
import org.l2jmobius.loginserver.network.loginserverpackets.AuthResponse;
import org.l2jmobius.loginserver.network.loginserverpackets.LoginServerFail;

/**
 * Authenticates a game server connection and binds it to a registered server entry.<br>
 * The request publishes the server id, hexId and host pairs used for network routing, then completes registration.
 * <ul>
 * <li>Reads and validates packet sizes to avoid oversized allocations.</li>
 * <li>Matches or registers the server entry and attaches connection metadata.</li>
 * <li>Replies with AuthResponse and transitions the connection to AUTHED on success.</li>
 * </ul>
 * @author BazookaRpm
 */
public class GameServerAuth extends BaseReadablePacket
{
	// Logging.
	protected static final Logger LOGGER = Logger.getLogger(GameServerAuth.class.getName());
	
	// Packet Limits.
	private static final int MAX_HEX_ID_BYTES = 64;
	private static final int MAX_HOST_PAIRS = 32;
	private static final int HOST_STRINGS_PER_PAIR = 2;
	
	// Connection Context.
	private final GameServerThread _server;
	
	// Packet Data.
	private final int _desiredServerId;
	private final boolean _acceptAlternativeId;
	private final int _port;
	private final int _maxPlayers;
	private final byte[] _hexId;
	private final String[] _hosts;
	
	/**
	 * Reads the authentication packet and completes the registration process when valid.<br>
	 * Invalid payload sizes are rejected early to reduce DoS impact.
	 * @param decrypt
	 * @param server
	 */
	public GameServerAuth(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_server = server;
		
		final int desiredServerId = readByte();
		final boolean acceptAlternativeId = readByte() != 0;
		readByte(); // Reserved host flag, it is not used.
		
		final int port = readShort();
		final int maxPlayers = readInt();
		
		byte[] hexId = new byte[0];
		String[] hosts = new String[0];
		
		final int hexIdSize = readInt();
		if ((hexIdSize < 0) || (hexIdSize > MAX_HEX_ID_BYTES))
		{
			_desiredServerId = desiredServerId;
			_acceptAlternativeId = acceptAlternativeId;
			_port = port;
			_maxPlayers = maxPlayers;
			_hexId = hexId;
			_hosts = hosts;
			
			LOGGER.warning("Rejected GS auth due to invalid hexIdSize=" + hexIdSize + " for desiredServerId=" + desiredServerId + " (maxHexIdBytes=" + MAX_HEX_ID_BYTES + ").");
			_server.forceClose(LoginServerFail.REASON_WRONG_HEXID);
			return;
		}
		
		if (hexIdSize != 0)
		{
			hexId = readBytes(hexIdSize);
		}
		
		final int hostPairs = readInt();
		if ((hostPairs < 0) || (hostPairs > MAX_HOST_PAIRS))
		{
			_desiredServerId = desiredServerId;
			_acceptAlternativeId = acceptAlternativeId;
			_port = port;
			_maxPlayers = maxPlayers;
			_hexId = hexId;
			_hosts = hosts;
			
			LOGGER.warning("Rejected GS auth due to invalid hostPairs=" + hostPairs + " for desiredServerId=" + desiredServerId + " (maxHostPairs=" + MAX_HOST_PAIRS + ").");
			_server.forceClose(LoginServerFail.REASON_WRONG_HEXID);
			return;
		}
		
		final int hostStringCount = hostPairs * HOST_STRINGS_PER_PAIR;
		if (hostStringCount != 0)
		{
			hosts = new String[hostStringCount];
			for (int i = 0; i < hostStringCount; i++)
			{
				hosts[i] = readString();
			}
		}
		
		_desiredServerId = desiredServerId;
		_acceptAlternativeId = acceptAlternativeId;
		_port = port;
		_maxPlayers = maxPlayers;
		_hexId = hexId;
		_hosts = hosts;
		
		if (handleRegProcess())
		{
			_server.sendPacket(new AuthResponse(_server.getGameServerInfo().getId()));
			_server.setLoginConnectionState(GameServerState.AUTHED);
		}
	}
	
	private boolean handleRegProcess()
	{
		final GameServerTable gameServerTable = GameServerTable.getInstance();
		final int desiredId = _desiredServerId;
		
		final GameServerInfo registeredInfo = gameServerTable.getRegisteredGameServerById(desiredId);
		if (registeredInfo == null)
		{
			if (!LoginConfig.ACCEPT_NEW_GAMESERVER)
			{
				LOGGER.info("Rejected GS auth for desiredServerId=" + desiredId + " because new game servers are not accepted.");
				_server.forceClose(LoginServerFail.REASON_WRONG_HEXID);
				return false;
			}
			
			final GameServerInfo newInfo = new GameServerInfo(desiredId, _hexId, _server);
			if (!gameServerTable.register(desiredId, newInfo))
			{
				LOGGER.info("Rejected GS auth for desiredServerId=" + desiredId + " because the id became reserved during registration.");
				_server.forceClose(LoginServerFail.REASON_ID_RESERVED);
				return false;
			}
			
			_server.attachGameServerInfo(newInfo, _port, _hosts, _maxPlayers);
			gameServerTable.registerServerOnDB(newInfo);
			return true;
		}
		
		if (Arrays.equals(registeredInfo.getHexId(), _hexId))
		{
			synchronized (registeredInfo)
			{
				if (registeredInfo.isAuthed())
				{
					LOGGER.info("Rejected GS auth for desiredServerId=" + desiredId + " due to already authenticated session.");
					_server.forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
					return false;
				}
				
				_server.attachGameServerInfo(registeredInfo, _port, _hosts, _maxPlayers);
				return true;
			}
		}
		
		if (!LoginConfig.ACCEPT_NEW_GAMESERVER || !_acceptAlternativeId)
		{
			LOGGER.info("Rejected GS auth for desiredServerId=" + desiredId + " due to wrong hexId and no alternative id allowed.");
			_server.forceClose(LoginServerFail.REASON_WRONG_HEXID);
			return false;
		}
		
		final GameServerInfo alternativeInfo = new GameServerInfo(desiredId, _hexId, _server);
		if (!gameServerTable.registerWithFirstAvailableId(alternativeInfo))
		{
			LOGGER.info("Rejected GS auth for desiredServerId=" + desiredId + " due to no free alternative id available.");
			_server.forceClose(LoginServerFail.REASON_NO_FREE_ID);
			return false;
		}
		
		_server.attachGameServerInfo(alternativeInfo, _port, _hosts, _maxPlayers);
		gameServerTable.registerServerOnDB(alternativeInfo);
		return true;
	}
}
