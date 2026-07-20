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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class PrivateStoreManageListBuy extends ServerPacket
{
	private final int _objId;
	private final int _playerAdena;
	private final Collection<Item> _itemList;
	private final Collection<TradeItem> _buyList;
	
	public PrivateStoreManageListBuy(Player player)
	{
		_objId = player.getObjectId();
		_playerAdena = player.getAdena();
		_itemList = player.getInventory().getUniqueItems(false, true, true, false);
		_buyList = player.getBuyList().getItems();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PRIVATE_STORE_BUY_MANAGE_LIST.writeId(this, buffer);
		
		// section 1
		buffer.writeInt(_objId);
		buffer.writeInt(_playerAdena);
		
		// section2
		buffer.writeInt(_itemList.size()); // inventory items for potential buy
		for (Item item : _itemList)
		{
			buffer.writeInt(item.getId());
			buffer.writeShort(0); // show enchant level as 0, as you can't buy enchanted weapons
			buffer.writeInt(item.getCount());
			buffer.writeInt(item.getReferencePrice());
			buffer.writeShort(0);
			buffer.writeInt(item.getTemplate().getBodyPart().getMask());
			buffer.writeShort(item.getTemplate().getType2());
		}
		
		// section 3
		buffer.writeInt(_buyList.size()); // count for all items already added for buy
		for (TradeItem item : _buyList)
		{
			buffer.writeInt(item.getItem().getId());
			buffer.writeShort(0);
			buffer.writeInt(item.getCount());
			buffer.writeInt(item.getItem().getReferencePrice());
			buffer.writeShort(0);
			buffer.writeInt(item.getItem().getBodyPart().getMask());
			buffer.writeShort(item.getItem().getType2());
			buffer.writeInt(item.getPrice()); // your price
			buffer.writeInt(item.getItem().getReferencePrice()); // fixed store price
		}
	}
}
