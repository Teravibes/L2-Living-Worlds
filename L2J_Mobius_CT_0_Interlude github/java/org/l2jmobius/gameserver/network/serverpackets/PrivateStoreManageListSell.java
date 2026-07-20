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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.TradeItem;

public class PrivateStoreManageListSell extends ServerPacket
{
	private final int _objId;
	private final int _playerAdena;
	private final boolean _packageSale;
	private final Collection<TradeItem> _itemList;
	private final Collection<TradeItem> _sellList;
	
	public PrivateStoreManageListSell(Player player, boolean isPackageSale)
	{
		_objId = player.getObjectId();
		_playerAdena = player.getAdena();
		player.getSellList().updateItems();
		_packageSale = isPackageSale;
		_itemList = player.getInventory().getAvailableItems(player.getSellList());
		_sellList = player.getSellList().getItems();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PRIVATE_STORE_MANAGE_LIST_SELL.writeId(this, buffer);
		
		// section 1
		buffer.writeInt(_objId);
		buffer.writeInt(_packageSale); // Package sell
		buffer.writeInt(_playerAdena);
		
		// section2
		buffer.writeInt(_itemList.size()); // for potential sells
		for (TradeItem item : _itemList)
		{
			buffer.writeInt(item.getItem().getType2());
			buffer.writeInt(item.getObjectId());
			buffer.writeInt(item.getItem().getId());
			buffer.writeInt(item.getCount());
			buffer.writeShort(0);
			buffer.writeShort(item.getEnchant()); // enchant level
			buffer.writeShort(item.getCustomType2());
			buffer.writeInt(item.getItem().getBodyPart().getMask());
			buffer.writeInt(item.getPrice()); // store price
		}
		
		// section 3
		buffer.writeInt(_sellList.size()); // count for any items already added for sell
		for (TradeItem item : _sellList)
		{
			buffer.writeInt(item.getItem().getType2());
			buffer.writeInt(item.getObjectId());
			buffer.writeInt(item.getItem().getId());
			buffer.writeInt(item.getCount());
			buffer.writeShort(0);
			buffer.writeShort(item.getEnchant()); // enchant level
			buffer.writeShort(0);
			buffer.writeInt(item.getItem().getBodyPart().getMask());
			buffer.writeInt(item.getPrice()); // your price
			buffer.writeInt(item.getItem().getReferencePrice()); // store price
		}
	}
}
