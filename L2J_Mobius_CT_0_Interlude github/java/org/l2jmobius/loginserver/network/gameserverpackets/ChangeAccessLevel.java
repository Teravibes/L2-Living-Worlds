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
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.LoginController;

/**
 * @author -Wooden-
 */
public class ChangeAccessLevel extends BaseReadablePacket
{
	protected static final Logger LOGGER = Logger.getLogger(ChangeAccessLevel.class.getName());
	
	public ChangeAccessLevel(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		final int level = readInt();
		final String account = readString();
		LoginController.getInstance().setAccountAccessLevel(account, level);
		LOGGER.info("Changed " + account + " access level to " + level);
	}
}
