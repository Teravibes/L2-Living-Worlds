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

import java.util.Map;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 ** @author Gnacik
 */
public class ShopPreviewInfo extends ServerPacket
{
	private final Map<Integer, Integer> _itemlist;
	
	public ShopPreviewInfo(Map<Integer, Integer> itemlist)
	{
		_itemlist = itemlist;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SHOP_PREVIEW_INFO.writeId(this, buffer);
		buffer.writeInt(Inventory.PAPERDOLL_TOTALSLOTS);
		
		// Slots
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_REAR));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LEAR));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_NECK));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_RFINGER));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LFINGER));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_HEAD));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_RHAND));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LHAND));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_GLOVES));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_CHEST));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_LEGS));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_FEET));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_CLOAK));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_RHAND));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_HAIR));
		buffer.writeInt(getFromList(Inventory.PAPERDOLL_UNDER));
	}
	
	private int getFromList(int key)
	{
		return (_itemlist.containsKey(key) ? _itemlist.get(key) : 0);
	}
}
