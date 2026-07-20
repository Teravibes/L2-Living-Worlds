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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.residences.AuctionableHall;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.residences.ClanHall.ClanHallFunction;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Steuf
 */
public class AgitDecoInfo extends ServerPacket
{
	private final AuctionableHall _clanHall;
	
	public AgitDecoInfo(AuctionableHall clanHall)
	{
		_clanHall = clanHall;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.AGIT_DECO_INFO.writeId(this, buffer);
		buffer.writeInt(_clanHall.getId());
		
		// Fireplace
		ClanHallFunction function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_HP);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
		}
		else if (((_clanHall.getGrade() == 0) && (function.getLevel() < 220)) || ((_clanHall.getGrade() == 1) && (function.getLevel() < 160)) || ((_clanHall.getGrade() == 2) && (function.getLevel() < 260)) || ((_clanHall.getGrade() == 3) && (function.getLevel() < 300)))
		{
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
		}
		
		// Carpet - Statue
		function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_MP);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
			buffer.writeByte(0);
		}
		else if ((((_clanHall.getGrade() == 0) || (_clanHall.getGrade() == 1)) && (function.getLevel() < 25)) || ((_clanHall.getGrade() == 2) && (function.getLevel() < 30)) || ((_clanHall.getGrade() == 3) && (function.getLevel() < 40)))
		{
			buffer.writeByte(1);
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
			buffer.writeByte(2);
		}
		
		// Chandelier
		function = _clanHall.getFunction(ClanHall.FUNC_RESTORE_EXP);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
		}
		else if (((_clanHall.getGrade() == 0) && (function.getLevel() < 25)) || ((_clanHall.getGrade() == 1) && (function.getLevel() < 30)) || ((_clanHall.getGrade() == 2) && (function.getLevel() < 40)) || ((_clanHall.getGrade() == 3) && (function.getLevel() < 50)))
		{
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
		}
		
		// Mirror
		function = _clanHall.getFunction(ClanHall.FUNC_TELEPORT);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
		}
		else if (function.getLevel() < 2)
		{
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
		}
		
		// Crystal
		buffer.writeByte(0);
		
		// Curtain
		function = _clanHall.getFunction(ClanHall.FUNC_DECO_CURTAINS);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
		}
		else if (function.getLevel() <= 1)
		{
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
		}
		
		// Magic Curtain
		function = _clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
		}
		else if (((_clanHall.getGrade() == 0) && (function.getLevel() < 2)) || (function.getLevel() < 3))
		{
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
		}
		
		// Support? - Flag
		function = _clanHall.getFunction(ClanHall.FUNC_SUPPORT);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
			buffer.writeByte(0);
		}
		else if (((_clanHall.getGrade() == 0) && (function.getLevel() < 2)) || ((_clanHall.getGrade() == 1) && (function.getLevel() < 4)) || ((_clanHall.getGrade() == 2) && (function.getLevel() < 5)) || ((_clanHall.getGrade() == 3) && (function.getLevel() < 8)))
		{
			buffer.writeByte(1);
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
			buffer.writeByte(2);
		}
		
		// Front platform
		function = _clanHall.getFunction(ClanHall.FUNC_DECO_FRONTPLATEFORM);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
		}
		else if (function.getLevel() <= 1)
		{
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
		}
		
		// Item create?
		function = _clanHall.getFunction(ClanHall.FUNC_ITEM_CREATE);
		if ((function == null) || (function.getLevel() == 0))
		{
			buffer.writeByte(0);
		}
		else if (((_clanHall.getGrade() == 0) && (function.getLevel() < 2)) || (function.getLevel() < 3))
		{
			buffer.writeByte(1);
		}
		else
		{
			buffer.writeByte(2);
		}
	}
}
