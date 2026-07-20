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

import org.l2jmobius.commons.network.base.BaseReadablePacket;
import org.l2jmobius.loginserver.GameServerThread;

/**
 * @author -Wooden-
 */
public class PlayerInGame extends BaseReadablePacket
{
	public PlayerInGame(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		final int size = readShort();
		for (int i = 0; i < size; i++)
		{
			final String account = readString();
			server.addAccountOnGameServer(account);
		}
	}
}
