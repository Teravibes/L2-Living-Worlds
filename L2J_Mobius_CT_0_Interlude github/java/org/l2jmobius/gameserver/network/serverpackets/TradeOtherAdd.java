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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

/**
 * @author Yme
 */
public class TradeOtherAdd extends ServerPacket
{
	private final TradeItem _item;
	
	public TradeOtherAdd(TradeItem item)
	{
		_item = item;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.TRADE_OTHER_ADD.writeId(this, buffer);
		buffer.writeShort(1); // item count
		buffer.writeShort(_item.getItem().getType1()); // item type1
		buffer.writeInt(_item.getObjectId());
		buffer.writeInt(_item.getItem().getDisplayId());
		buffer.writeInt(_item.getCount());
		buffer.writeShort(_item.getItem().getType2()); // item type2
		buffer.writeShort(_item.getCustomType1());
		buffer.writeInt(_item.getItem().getBodyPart().getMask()); // rev 415 slot 0006-lr.ear 0008-neck 0030-lr.finger 0040-head 0080-?? 0100-l.hand 0200-gloves 0400-chest 0800-pants 1000-feet 2000-?? 4000-r.hand 8000-r.hand
		buffer.writeShort(_item.getEnchant()); // enchant level
		buffer.writeShort(0);
		buffer.writeShort(_item.getCustomType2());
	}
}
