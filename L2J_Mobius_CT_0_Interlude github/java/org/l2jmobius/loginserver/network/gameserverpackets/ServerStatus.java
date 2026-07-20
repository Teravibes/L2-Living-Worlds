/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.loginserver.network.gameserverpackets;

import java.util.logging.Logger;

import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.loginserver.GameServerTable;
import org.l2jmobius.loginserver.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.GameServerThread;

/**
 * @author -Wooden-
 */
public class ServerStatus extends BaseReadablePacket
{
	protected static final Logger LOGGER = Logger.getLogger(ServerStatus.class.getName());
	
	// Server list ids.
	public static final int SERVER_LIST_STATUS = 0x01;
	public static final int SERVER_TYPE = 0x02;
	public static final int SERVER_LIST_SQUARE_BRACKET = 0x03;
	public static final int MAX_PLAYERS = 0x04;
	public static final int TEST_SERVER = 0x05;
	public static final int SERVER_AGE = 0x06;
	
	// Server status.
	public static final int STATUS_AUTO = 0x00;
	public static final int STATUS_GOOD = 0x01;
	public static final int STATUS_NORMAL = 0x02;
	public static final int STATUS_FULL = 0x03;
	public static final int STATUS_DOWN = 0x04;
	public static final int STATUS_GM_ONLY = 0x05;
	
	// Server types.
	public static final int SERVER_NORMAL = 0x01;
	public static final int SERVER_RELAX = 0x02;
	public static final int SERVER_TEST = 0x04;
	public static final int SERVER_NOLABEL = 0x08;
	public static final int SERVER_CREATION_RESTRICTED = 0x10;
	public static final int SERVER_EVENT = 0x20;
	public static final int SERVER_FREE = 0x40;
	
	// Server ages.
	public static final int SERVER_AGE_ALL = 0x00;
	public static final int SERVER_AGE_15 = 0x0F;
	public static final int SERVER_AGE_18 = 0x12;
	
	public static final int ON = 0x01;
	public static final int OFF = 0x00;
	
	public ServerStatus(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		final GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(server.getServerId());
		if (gsi != null)
		{
			final int size = readInt();
			for (int i = 0; i < size; i++)
			{
				final int type = readInt();
				final int value = readInt();
				switch (type)
				{
					case SERVER_LIST_STATUS:
					{
						gsi.setStatus(value);
						break;
					}
					case SERVER_LIST_SQUARE_BRACKET:
					{
						gsi.setShowingBrackets(value == ON);
						break;
					}
					case MAX_PLAYERS:
					{
						gsi.setMaxPlayers(value);
						break;
					}
					case SERVER_TYPE:
					{
						gsi.setServerType(value);
						break;
					}
					case SERVER_AGE:
					{
						gsi.setAgeLimit(value);
						break;
					}
				}
			}
		}
	}
}
