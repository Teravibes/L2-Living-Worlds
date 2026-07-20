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
package org.l2jmobius.loginserver.network.loginserverpackets;

import org.l2jmobius.commons.network.base.BaseWritablePacket;

/**
 * @author Nik
 */
public class ChangePasswordResponse extends BaseWritablePacket
{
	public ChangePasswordResponse(String characterName, String msgToSend)
	{
		writeByte(0x06);
		writeString(characterName);
		writeString(msgToSend);
	}
}
