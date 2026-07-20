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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.item.holders.ItemInfo;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author UnAfraid
 */
public abstract class AbstractInventoryUpdate extends AbstractItemPacket
{
	private final Map<Integer, ItemInfo> _items = new HashMap<>();
	
	public AbstractInventoryUpdate()
	{
	}
	
	public AbstractInventoryUpdate(Item item)
	{
		addItem(item);
	}
	
	public AbstractInventoryUpdate(List<ItemInfo> items)
	{
		synchronized (_items)
		{
			for (ItemInfo item : items)
			{
				_items.put(item.getObjectId(), item);
			}
		}
	}
	
	public void addItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item));
		}
	}
	
	public void addNewItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item, 1));
		}
	}
	
	public void addModifiedItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item, 2));
		}
	}
	
	public void addRemovedItem(Item item)
	{
		synchronized (_items)
		{
			_items.put(item.getObjectId(), new ItemInfo(item, 3));
		}
	}
	
	public void addItems(Collection<Item> items)
	{
		synchronized (_items)
		{
			for (Item item : items)
			{
				_items.put(item.getObjectId(), new ItemInfo(item));
			}
		}
	}
	
	public void putAll(Map<Integer, ItemInfo> items)
	{
		synchronized (_items)
		{
			_items.putAll(items);
		}
	}
	
	public Map<Integer, ItemInfo> getItemEntries()
	{
		return _items;
	}
	
	public Collection<ItemInfo> getItems()
	{
		return _items.values();
	}
	
	protected void writeItems(WritableBuffer buffer)
	{
		synchronized (_items)
		{
			buffer.writeShort(_items.size());
			for (ItemInfo item : _items.values())
			{
				buffer.writeShort(item.getChange()); // Update type : 01-add, 02-modify, 03-remove
				writeItem(item, buffer);
			}
			
			_items.clear();
		}
	}
}
