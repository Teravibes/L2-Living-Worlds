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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.model.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author l3x
 */
public class BuyListSeed extends ServerPacket
{
	private final int _manorId;
	private final int _money;
	private final List<SeedProduction> _list = new ArrayList<>();
	
	public BuyListSeed(int currentMoney, int castleId)
	{
		_money = currentMoney;
		_manorId = castleId;
		for (SeedProduction s : CastleManorManager.getInstance().getSeedProduction(castleId, false))
		{
			if ((s.getAmount() > 0) && (s.getPrice() > 0))
			{
				_list.add(s);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.BUY_LIST_SEED.writeId(this, buffer);
		buffer.writeInt(_money); // current money
		buffer.writeInt(_manorId); // manor id
		if (!_list.isEmpty())
		{
			buffer.writeShort(_list.size()); // list length
			for (SeedProduction s : _list)
			{
				buffer.writeShort(4); // item->type1
				buffer.writeInt(0); // objectId
				buffer.writeInt(s.getId()); // item id
				buffer.writeInt(s.getAmount()); // item count
				buffer.writeShort(4); // item->type2
				buffer.writeShort(0); // unknown :)
				buffer.writeInt(s.getPrice()); // price
			}
			
			_list.clear();
		}
		else
		{
			buffer.writeShort(0);
		}
	}
}
