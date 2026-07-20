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
import org.l2jmobius.loginserver.LoginController;

/**
 * @author mrTJO
 */
public class PlayerTracert extends BaseReadablePacket
{
	protected static final Logger LOGGER = Logger.getLogger(PlayerTracert.class.getName());
	
	public PlayerTracert(byte[] decrypt)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		final String account = readString();
		final String pcIp = readString();
		final String hop1 = readString();
		final String hop2 = readString();
		final String hop3 = readString();
		final String hop4 = readString();
		LoginController.getInstance().setAccountLastTracert(account, pcIp, hop1, hop2, hop3, hop4);
	}
}
