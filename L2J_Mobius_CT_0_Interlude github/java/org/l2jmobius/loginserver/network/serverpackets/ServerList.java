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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.network.LoginClient;
import org.l2jmobius.loginserver.network.gameserverpackets.ServerStatus;

/**
 * ServerList
 * 
 * <pre>
 * Format: cc [cddcchhcdc]
 * 
 * c: server list size (number of servers)
 * c: ?
 * [ (repeat for each servers)
 * c: server id (ignored by client?)
 * d: server ip
 * d: server port
 * c: age limit (used by client?)
 * c: pvp or not (used by client?)
 * h: current number of players
 * h: max number of players
 * c: 0 if server is down
 * d: 2nd bit: clock
 *    3rd bit: won't display server name
 *    4th bit: test server (used by client?)
 * c: 0 if you don't want to display brackets in front of sever name
 * ]
 * </pre>
 * 
 * Server will be considered as Good when the number of online players<br>
 * is less than half the maximum. as Normal between half and 4/5<br>
 * and Full when there's more than 4/5 of the maximum number of players.
 */
public class ServerList extends LoginServerPacket
{
	protected static final Logger LOGGER = Logger.getLogger(ServerList.class.getName());
	
	private final List<ServerData> _servers;
	private final int _lastServer;
	private Map<Integer, Integer> _charsOnServers;
	private Map<Integer, long[]> _charsToDelete;
	
	class ServerData
	{
		protected byte[] _ip;
		protected int _port;
		protected int _ageLimit;
		protected boolean _pvp;
		protected int _currentPlayers;
		protected int _maxPlayers;
		protected boolean _brackets;
		protected boolean _clock;
		protected int _status;
		protected int _serverId;
		protected int _serverType;
		
		ServerData(LoginClient client, GameServerInfo gsi)
		{
			try
			{
				_ip = InetAddress.getByName(gsi.getServerAddress(InetAddress.getByName(client.getIp()))).getAddress();
			}
			catch (UnknownHostException e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
				_ip = new byte[4];
				_ip[0] = 127;
				_ip[1] = 0;
				_ip[2] = 0;
				_ip[3] = 1;
			}
			
			_port = gsi.getPort();
			_pvp = gsi.isPvp();
			_serverType = gsi.getServerType();
			_currentPlayers = gsi.getCurrentPlayerCount();
			_maxPlayers = gsi.getMaxPlayers();
			_ageLimit = 0;
			_brackets = gsi.isShowingBrackets();
			
			// If server GM-only - show status only to GMs
			_status = (client.getAccessLevel() < 0) || ((gsi.getStatus() == ServerStatus.STATUS_GM_ONLY) && (client.getAccessLevel() <= 0)) ? ServerStatus.STATUS_DOWN : gsi.getStatus();
			_serverId = gsi.getId();
		}
	}
	
	public ServerList(LoginClient client)
	{
		_servers = new ArrayList<>(GameServerTable.getInstance().getRegisteredGameServers().size());
		_lastServer = client.getLastServer();
		for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
		{
			_servers.add(new ServerData(client, gsi));
		}
		
		// Wait 500ms to reply with character list.
		int i = 0;
		while (((_charsOnServers == null) || (_charsToDelete == null)) && (i++ < 10))
		{
			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException ignored)
			{
			}
			
			_charsOnServers = client.getCharsOnServ();
			_charsToDelete = client.getCharsWaitingDelOnServ();
		}
	}
	
	@Override
	protected void writeImpl(LoginClient client, WritableBuffer buffer)
	{
		buffer.writeByte(0x04);
		buffer.writeByte(_servers.size());
		buffer.writeByte(_lastServer);
		for (ServerData server : _servers)
		{
			buffer.writeByte(server._serverId); // Server id.
			
			buffer.writeByte(server._ip[0] & 0xff);
			buffer.writeByte(server._ip[1] & 0xff);
			buffer.writeByte(server._ip[2] & 0xff);
			buffer.writeByte(server._ip[3] & 0xff);
			
			buffer.writeInt(server._port);
			buffer.writeByte(server._ageLimit); // Age Limit 0, 15, 18.
			buffer.writeByte(server._pvp ? 1 : 0);
			buffer.writeShort(server._currentPlayers);
			buffer.writeShort(server._maxPlayers);
			buffer.writeByte(server._status == ServerStatus.STATUS_DOWN ? 0 : 1);
			buffer.writeInt(server._serverType); // 1: Normal, 2: Relax, 4: Public Test, 8: No Label, 16: Character Creation Restricted, 32: Event, 64: Free.
			buffer.writeByte(server._brackets ? 1 : 0);
		}
		
		buffer.writeShort(0x00); // Unknown.
		if (_charsOnServers != null)
		{
			buffer.writeByte(_charsOnServers.size());
			for (Entry<Integer, Integer> entry : _charsOnServers.entrySet())
			{
				final int servId = entry.getKey();
				buffer.writeByte(servId);
				buffer.writeByte(entry.getValue());
				if ((_charsToDelete == null) || !_charsToDelete.containsKey(servId))
				{
					buffer.writeByte(0);
				}
				else
				{
					buffer.writeByte(_charsToDelete.get(servId).length);
					for (long deleteTime : _charsToDelete.get(servId))
					{
						buffer.writeInt((int) ((deleteTime - System.currentTimeMillis()) / 1000));
					}
				}
			}
		}
		else
		{
			buffer.writeByte(0);
		}
	}
}
