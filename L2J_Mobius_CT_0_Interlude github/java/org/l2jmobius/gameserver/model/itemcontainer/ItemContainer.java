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
package org.l2jmobius.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.ItemVariables;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * @author Advi
 */
public abstract class ItemContainer
{
	protected static final Logger LOGGER = Logger.getLogger(ItemContainer.class.getName());
	
	protected final Set<Item> _items = ConcurrentHashMap.newKeySet(1);
	
	protected ItemContainer()
	{
	}
	
	protected abstract Creature getOwner();
	
	protected abstract ItemLocation getBaseLocation();
	
	public String getName()
	{
		return "ItemContainer";
	}
	
	/**
	 * @return int the owner object Id
	 */
	public int getOwnerId()
	{
		return getOwner() == null ? 0 : getOwner().getObjectId();
	}
	
	/**
	 * @return the quantity of items in the inventory
	 */
	public int getSize()
	{
		return _items.size();
	}
	
	/**
	 * Gets the items in inventory.
	 * @return the items in inventory.
	 */
	public Collection<Item> getItems()
	{
		return _items;
	}
	
	/**
	 * @param itemId the item Id
	 * @return the item from inventory by itemId
	 */
	public Item getItemByItemId(int itemId)
	{
		for (Item item : _items)
		{
			if (item.getId() == itemId)
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * @return true if player got item for self resurrection
	 */
	public boolean haveItemForSelfResurrection()
	{
		for (Item item : _items)
		{
			if ((item != null) && item.getTemplate().isAllowSelfResurrection())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @param itemId the item Id
	 * @return the items list from inventory by using its itemId
	 */
	public Collection<Item> getAllItemsByItemId(int itemId)
	{
		final List<Item> result = new LinkedList<>();
		for (Item item : _items)
		{
			if (itemId == item.getId())
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * @param itemId the item Id
	 * @param itemToIgnore used during the loop, to avoid returning the same item
	 * @return the item from inventory by itemId
	 */
	public Item getItemByItemId(int itemId, Item itemToIgnore)
	{
		for (Item item : _items)
		{
			if ((item.getId() == itemId) && !item.equals(itemToIgnore))
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * @param objectId the item object Id
	 * @return item from inventory by objectId
	 */
	public Item getItemByObjectId(int objectId)
	{
		for (Item item : _items)
		{
			if (objectId == item.getObjectId())
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the inventory item count by item Id and enchant level including equipped items.
	 * @param itemId the item Id
	 * @param enchantLevel the item enchant level, use -1 to match any enchant level
	 * @return the inventory item count
	 */
	public int getInventoryItemCount(int itemId, int enchantLevel)
	{
		return getInventoryItemCount(itemId, enchantLevel, true);
	}
	
	/**
	 * Gets the inventory item count by item Id and enchant level, may include equipped items.
	 * @param itemId the item Id
	 * @param enchantLevel the item enchant level, use -1 to match any enchant level
	 * @param includeEquipped if {@code true} includes equipped items in the result
	 * @return the inventory item count
	 */
	public int getInventoryItemCount(int itemId, int enchantLevel, boolean includeEquipped)
	{
		int count = 0;
		for (Item item : _items)
		{
			if ((item.getId() == itemId) && ((item.getEnchantLevel() == enchantLevel) || (enchantLevel < 0)) && (includeEquipped || !item.isEquipped()))
			{
				if (item.isStackable())
				{
					return item.getCount();
				}
				
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Adds item to inventory
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	public Item addItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		Item newItem = item;
		final Item olditem = getItemByItemId(newItem.getId());
		
		// If stackable item is found in inventory just add to current quantity
		if ((olditem != null) && olditem.isStackable())
		{
			final int count = newItem.getCount();
			olditem.changeCount(process, count, actor, reference);
			olditem.setLastChange(Item.MODIFIED);
			
			// And destroys the item
			ItemManager.destroyItem(process, newItem, actor, reference);
			newItem.updateDatabase();
			newItem = olditem;
		}
		else // If item hasn't be found in inventory, create new one
		{
			newItem.setOwnerId(process, getOwnerId(), actor, reference);
			newItem.setItemLocation(getBaseLocation());
			newItem.setLastChange((Item.ADDED));
			
			// Add item in inventory
			addItem(newItem);
		}
		
		refreshWeight();
		return newItem;
	}
	
	/**
	 * Adds item to inventory
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	public Item addItem(ItemProcessType process, int itemId, int count, Player actor, Object reference)
	{
		Item item = getItemByItemId(itemId);
		
		// If stackable item is found in inventory just add to current quantity
		if ((item != null) && item.isStackable())
		{
			item.changeCount(process, count, actor, reference);
			item.setLastChange(Item.MODIFIED);
		}
		else // If item hasn't be found in inventory, create new one
		{
			final InventoryUpdate iu = new InventoryUpdate();
			for (int i = 0; i < count; i++)
			{
				final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
				if (template == null)
				{
					LOGGER.warning("Invalid ItemId (" + itemId + ") requested by " + (actor != null ? actor : process));
					return null;
				}
				
				item = ItemManager.createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
				item.setOwnerId(getOwnerId());
				item.setItemLocation(getBaseLocation());
				item.setLastChange(Item.ADDED);
				item.setEnchantLevel(template.getDefaultEnchantLevel());
				
				// Add item in inventory
				addItem(item);
				
				// Add additional items to InventoryUpdate.
				if ((count > 1) && (i < (count - 1)))
				{
					iu.addNewItem(item);
				}
				
				// If stackable, end loop as entire count is included in 1 instance of item
				if (template.isStackable() || !GeneralConfig.MULTIPLE_ITEM_DROP)
				{
					break;
				}
			}
			
			// If additional items where created send InventoryUpdate.
			if ((count > 1) && (item != null) && !item.isStackable() && (item.getItemLocation() == ItemLocation.INVENTORY))
			{
				actor.sendInventoryUpdate(iu);
			}
		}
		
		refreshWeight();
		return item;
	}
	
	/**
	 * Transfers item to another inventory
	 * @param process ItemProcessType identifier of process triggering this action
	 * @param objectId Item Identifier of the item to be transfered
	 * @param countValue Quantity of items to be transfered
	 * @param target the item container where the item will be moved.
	 * @param actor Player requesting the item transfer
	 * @param reference Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	public Item transferItem(ItemProcessType process, int objectId, int countValue, ItemContainer target, Player actor, Object reference)
	{
		if (target == null)
		{
			return null;
		}
		
		final Item sourceitem = getItemByObjectId(objectId);
		if (sourceitem == null)
		{
			return null;
		}
		
		Item targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getId()) : null;
		synchronized (sourceitem)
		{
			// check if this item still present in this container
			if (getItemByObjectId(objectId) != sourceitem)
			{
				return null;
			}
			
			// Check if requested quantity is available
			int count = countValue;
			if (count > sourceitem.getCount())
			{
				count = sourceitem.getCount();
			}
			
			// If possible, move entire item object
			if ((sourceitem.getCount() == count) && (targetitem == null) && !sourceitem.isStackable())
			{
				removeItem(sourceitem);
				target.addItem(process, sourceitem, actor, reference);
				targetitem = sourceitem;
			}
			else
			{
				if (sourceitem.getCount() > count) // If possible, only update counts
				{
					sourceitem.changeCount(process, -count, actor, reference);
				}
				else // Otherwise destroy old item
				{
					removeItem(sourceitem);
					ItemManager.destroyItem(process, sourceitem, actor, reference);
				}
				
				if (targetitem != null) // If possible, only update counts
				{
					targetitem.changeCount(process, count, actor, reference);
				}
				else // Otherwise add new item
				{
					targetitem = target.addItem(process, sourceitem.getId(), count, actor, reference);
				}
			}
			
			// Updates database
			sourceitem.updateDatabase(true);
			if ((targetitem != sourceitem) && (targetitem != null))
			{
				targetitem.updateDatabase();
			}
			
			if (sourceitem.isAugmented())
			{
				sourceitem.getAugmentation().removeBonus(actor);
			}
			
			refreshWeight();
			target.refreshWeight();
		}
		
		return targetitem;
	}
	
	/**
	 * Destroy item from inventory and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be destroyed
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item destroyItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be destroyed
	 * @param count
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item destroyItem(ItemProcessType process, Item item, int count, Player actor, Object reference)
	{
		synchronized (item)
		{
			// Adjust item quantity
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(Item.MODIFIED);
				refreshWeight();
			}
			else
			{
				if (item.getCount() < count)
				{
					return null;
				}
				
				final boolean removed = removeItem(item);
				if (!removed)
				{
					return null;
				}
				
				ItemManager.destroyItem(process, item, actor, reference);
				item.updateDatabase();
				refreshWeight();
				
				item.stopAllTasks();
			}
		}
		
		return item;
	}
	
	/**
	 * Destroy item from inventory by using its <b>objectID</b> and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item destroyItem(ItemProcessType process, int objectId, int count, Player actor, Object reference)
	{
		final Item item = getItemByObjectId(objectId);
		return item == null ? null : destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <b>itemId</b> and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item destroyItemByItemId(ItemProcessType process, int itemId, int count, Player actor, Object reference)
	{
		final Item item = getItemByItemId(itemId);
		return item == null ? null : destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy all items from inventory and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void destroyAllItems(ItemProcessType process, Player actor, Object reference)
	{
		for (Item item : _items)
		{
			destroyItem(process, item, actor, reference);
		}
	}
	
	/**
	 * @return warehouse Adena.
	 */
	public int getAdena()
	{
		for (Item item : _items)
		{
			if (item.getId() == Inventory.ADENA_ID)
			{
				return item.getCount();
			}
		}
		
		return 0;
	}
	
	/**
	 * Adds item to inventory for further adjustments.
	 * @param item : Item to be added from inventory
	 */
	protected void addItem(Item item)
	{
		_items.add(item);
	}
	
	/**
	 * Removes item from inventory for further adjustments.
	 * @param item : Item to be removed from inventory
	 * @return
	 */
	protected boolean removeItem(Item item)
	{
		return _items.remove(item);
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	protected void refreshWeight()
	{
	}
	
	/**
	 * Delete item object from world
	 */
	public void deleteMe()
	{
		if ((this instanceof PlayerInventory) || (this instanceof PlayerWarehouse) || (getOwner() != null))
		{
			for (Item item : _items)
			{
				item.updateDatabase(true);
				item.stopAllTasks();
				
				final ItemVariables vars = item.getScript(ItemVariables.class);
				if (vars != null)
				{
					vars.saveNow();
				}
			}
		}
		
		for (Item item : _items)
		{
			World.getInstance().removeObject(item);
		}
		
		_items.clear();
	}
	
	/**
	 * Update database with items in inventory
	 */
	public void updateDatabase()
	{
		if (getOwner() != null)
		{
			for (Item item : _items)
			{
				item.updateDatabase(true);
			}
		}
	}
	
	/**
	 * Get back items in container from database
	 */
	public void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=?)"))
		{
			ps.setInt(1, getOwnerId());
			ps.setString(2, getBaseLocation().name());
			try (ResultSet rs = ps.executeQuery())
			{
				Item item;
				while (rs.next())
				{
					item = Item.restoreFromDb(getOwnerId(), rs);
					if (item == null)
					{
						continue;
					}
					
					World.getInstance().addObject(item);
					
					final Player owner = getOwner() != null ? getOwner().asPlayer() : null;
					
					// If stackable item is found in inventory just add to current quantity
					if (item.isStackable() && (getItemByItemId(item.getId()) != null))
					{
						addItem(ItemProcessType.RESTORE, item, owner, null);
					}
					else
					{
						addItem(item);
					}
				}
			}
			
			refreshWeight();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore container:", e);
		}
	}
	
	public boolean validateCapacity(long slots)
	{
		return true;
	}
	
	public boolean validateWeight(long weight)
	{
		return true;
	}
	
	/**
	 * If the item is stackable validates 1 slot, if the item isn't stackable validates the item count.
	 * @param itemId the item Id to verify
	 * @param count amount of item's weight to validate
	 * @return {@code true} if the item doesn't exist or it validates its slot count
	 */
	public boolean validateCapacityByItemId(int itemId, long count)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return (template == null) || (template.isStackable() ? validateCapacity(1) : validateCapacity(count));
	}
	
	/**
	 * @param itemId the item Id to verify
	 * @param count amount of item's weight to validate
	 * @return {@code true} if the item doesn't exist or it validates its weight
	 */
	public boolean validateWeightByItemId(int itemId, long count)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return (template == null) || validateWeight(template.getWeight() * count);
	}
}
