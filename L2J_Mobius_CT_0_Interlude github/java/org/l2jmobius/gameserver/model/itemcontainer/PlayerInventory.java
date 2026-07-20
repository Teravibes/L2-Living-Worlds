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
package org.l2jmobius.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemAdd;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemDestroy;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemDrop;
import org.l2jmobius.gameserver.model.events.holders.actor.player.inventory.OnPlayerItemTransfer;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.ItemVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.util.ArrayUtil;

public class PlayerInventory extends Inventory
{
	private static final Logger LOGGER = Logger.getLogger(PlayerInventory.class.getName());
	
	private final Player _owner;
	private Item _adena;
	private Item _ancientAdena;
	
	private int[] _blockItems = null;
	
	/**
	 * Block modes:
	 * <ul>
	 * <li>-1 - no block
	 * <li>0 - block items from _invItems, allow usage of other items
	 * <li>1 - allow usage of items from _invItems, block other items
	 * </ul>
	 */
	private int _blockMode = -1;
	
	public PlayerInventory(Player owner)
	{
		_owner = owner;
	}
	
	@Override
	public Player getOwner()
	{
		return _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	public Item getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public int getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public Item getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public int getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	/**
	 * Retrieves a collection of unique buyable items based on the specified conditions.
	 * @param allowAdena {@code true} to include Adena in the results, {@code false} to exclude it.
	 * @param allowAncientAdena {@code true} to include Ancient Adena in the results, {@code false} to exclude it.
	 * @param onlyAvailable {@code true} to include only items that are currently available to the owner, {@code false} to include all items regardless of availability.
	 * @param checkSellable {@code true} to check if items can be sold.
	 * @return a collection of {@link Item} objects that match the specified criteria.
	 */
	public Collection<Item> getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable, boolean checkSellable)
	{
		final List<Item> result = new LinkedList<>();
		for (Item item : _items)
		{
			if (!allowAdena && (item.getId() == ADENA_ID))
			{
				continue;
			}
			
			if (!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID))
			{
				continue;
			}
			
			boolean isDuplicate = false;
			for (Item addedItem : result)
			{
				if (addedItem.getId() == item.getId())
				{
					isDuplicate = true;
					break;
				}
			}
			
			if (!isDuplicate && (!onlyAvailable || ((!checkSellable || item.isSellable()) && item.isAvailable(_owner, false, false))))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a collection of unique items based on their enchant level and the specified conditions.
	 * @param allowAdena {@code true} to include Adena in the results, {@code false} to exclude it.
	 * @param allowAncientAdena {@code true} to include Ancient Adena in the results, {@code false} to exclude it.
	 * @param onlyAvailable {@code true} to include only items that are currently available to the owner, {@code false} to include all items regardless of availability.
	 * @return a collection of {@link Item} objects that match the specified criteria and have unique IDs and enchant levels.
	 */
	public Collection<Item> getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		final List<Item> result = new LinkedList<>();
		for (Item item : _items)
		{
			if ((item == null) || (!allowAdena && (item.getId() == ADENA_ID)) || (!allowAncientAdena && (item.getId() == ANCIENT_ADENA_ID)))
			{
				continue;
			}
			
			boolean isDuplicate = false;
			for (Item addedItem : result)
			{
				if ((addedItem.getId() == item.getId()) && (addedItem.getEnchantLevel() == item.getEnchantLevel()))
				{
					isDuplicate = true;
					break;
				}
			}
			
			if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(_owner, false, false))))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * @param itemId : ID of item
	 * @param includeEquipped : include equipped items
	 * @return Collection<Item> : matching items from inventory
	 */
	public List<Item> getAllItemsByItemId(int itemId, boolean includeEquipped)
	{
		final List<Item> result = new LinkedList<>();
		for (Item item : _items)
		{
			if ((itemId == item.getId()) && (includeEquipped || !item.isEquipped()))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * @param itemId
	 * @param enchantment
	 * @return
	 */
	public List<Item> getAllItemsByItemId(int itemId, int enchantment)
	{
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * @param itemId : ID of item
	 * @param enchantment : enchant level of item
	 * @param includeEquipped : include equipped items
	 * @return Collection<Item> : matching items from inventory
	 */
	public List<Item> getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped)
	{
		final List<Item> result = new LinkedList<>();
		for (Item item : _items)
		{
			if ((itemId == item.getId()) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped()))
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * @param allowAdena
	 * @param allowNonTradeable
	 * @param feightable
	 * @return the list of items in inventory available for transaction
	 */
	public Collection<Item> getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean feightable)
	{
		final List<Item> result = new LinkedList<>();
		for (Item item : _items)
		{
			if (!item.isAvailable(_owner, allowAdena, allowNonTradeable) || !canManipulateWithItemId(item.getId()))
			{
				continue;
			}
			else if (feightable)
			{
				if ((item.getItemLocation() == ItemLocation.INVENTORY) && item.isFreightable())
				{
					result.add(item);
				}
				continue;
			}
			
			result.add(item);
		}
		
		return result;
	}
	
	/**
	 * Get all augmented items
	 * @return
	 */
	public Collection<Item> getAugmentedItems()
	{
		final List<Item> result = new LinkedList<>();
		for (Item item : _items)
		{
			if (item.isAugmented())
			{
				result.add(item);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the list of items in inventory available for transaction adjusted by tradeList
	 * @param tradeList
	 * @return Item : items in inventory
	 */
	public Collection<TradeItem> getAvailableItems(TradeList tradeList)
	{
		final List<TradeItem> result = new LinkedList<>();
		for (Item item : _items)
		{
			if ((item != null) && item.isAvailable(_owner, false, false))
			{
				final TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null)
				{
					result.add(adjItem);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Adjust TradeItem according his status in inventory
	 * @param item : Item to be adjusted
	 */
	public void adjustAvailableItem(TradeItem item)
	{
		boolean notAllEquipped = false;
		for (Item adjItem : getAllItemsByItemId(item.getItem().getId()))
		{
			if (adjItem.isEquipable())
			{
				if (!adjItem.isEquipped())
				{
					notAllEquipped |= true;
				}
			}
			else
			{
				notAllEquipped |= true;
				break;
			}
		}
		
		if (notAllEquipped)
		{
			final Item adjItem = getItemByItemId(item.getItem().getId());
			item.setObjectId(adjItem.getObjectId());
			item.setEnchant(adjItem.getEnchantLevel());
			
			if (adjItem.getCount() < item.getCount())
			{
				item.setCount(adjItem.getCount());
			}
			
			return;
		}
		
		item.setCount(0);
	}
	
	/**
	 * Adds adena to PcInventory
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(ItemProcessType process, int count, Player actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes adena to PcInventory
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : Player Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return boolean : true if adena was reduced
	 */
	public boolean reduceAdena(ItemProcessType process, int count, Player actor, Object reference)
	{
		if (count > 0)
		{
			return destroyItemByItemId(process, ADENA_ID, count, actor, reference) != null;
		}
		
		return false;
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAncientAdena(ItemProcessType process, int count, Player actor, Object reference)
	{
		if (count > 0)
		{
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : Player Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return boolean : true if adena was reduced
	 */
	public boolean reduceAncientAdena(ItemProcessType process, int count, Player actor, Object reference)
	{
		return (count > 0) && (destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference) != null);
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	@Override
	public Item addItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		final Item addedItem = super.addItem(process, item, actor, reference);
		if (addedItem != null)
		{
			if ((addedItem.getId() == ADENA_ID) && !addedItem.equals(_adena))
			{
				_adena = addedItem;
			}
			else if ((addedItem.getId() == ANCIENT_ADENA_ID) && !addedItem.equals(_ancientAdena))
			{
				_ancientAdena = addedItem;
			}
			
			// Notify to scripts
			if ((actor != null) && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_ADD, actor))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, addedItem), actor);
			}
		}
		
		return addedItem;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param actor : Player Player requesting the item creation
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	@Override
	public Item addItem(ItemProcessType process, int itemId, int count, Player actor, Object reference)
	{
		final Item item = super.addItem(process, itemId, count, actor, reference);
		if (item != null)
		{
			if ((item.getId() == ADENA_ID) && !item.equals(_adena))
			{
				_adena = item;
			}
			else if ((item.getId() == ANCIENT_ADENA_ID) && !item.equals(_ancientAdena))
			{
				_ancientAdena = item;
			}
			
			if (actor != null)
			{
				// Send inventory update packet
				final InventoryUpdate playerIU = new InventoryUpdate();
				if (item.isStackable() && (item.getCount() > count))
				{
					playerIU.addModifiedItem(item);
				}
				else
				{
					playerIU.addNewItem(item);
				}
				
				actor.sendInventoryUpdate(playerIU);
				
				// Update current load as well
				final StatusUpdate su = new StatusUpdate(actor);
				su.addAttribute(StatusUpdate.CUR_LOAD, actor.getCurrentLoad());
				actor.sendPacket(su);
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_ADD, actor))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, item), actor);
				}
			}
		}
		
		return item;
	}
	
	/**
	 * Transfers item to another inventory and checks _adena and _ancientAdena
	 * @param process ItemProcessType identifier of process triggering this action
	 * @param objectId Item Identifier of the item to be transfered
	 * @param count Quantity of items to be transfered
	 * @param target the item container for the item to be transfered.
	 * @param actor the player requesting the item transfer
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the new item or the updated item in inventory
	 */
	@Override
	public Item transferItem(ItemProcessType process, int objectId, int count, ItemContainer target, Player actor, Object reference)
	{
		final Item item = super.transferItem(process, objectId, count, target, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_TRANSFER, item.getTemplate()))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemTransfer(actor, item, target), item.getTemplate());
		}
		
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be destroyed
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public Item destroyItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be destroyed
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public Item destroyItem(ItemProcessType process, Item item, int count, Player actor, Object reference)
	{
		final Item destroyedItem = super.destroyItem(process, item, count, actor, reference);
		
		if ((_adena != null) && (_adena.getCount() <= 0))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && (_ancientAdena.getCount() <= 0))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if ((destroyedItem != null) && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DESTROY, destroyedItem.getTemplate()))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDestroy(actor, destroyedItem), destroyedItem.getTemplate());
		}
		
		return destroyedItem;
	}
	
	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public Item destroyItem(ItemProcessType process, int objectId, int count, Player actor, Object reference)
	{
		final Item item = getItemByObjectId(objectId);
		return item == null ? null : destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <b>itemId</b> and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : Player requesting the item destroy
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public Item destroyItemByItemId(ItemProcessType process, int itemId, int count, Player actor, Object reference)
	{
		// Attempt to find non equipped items.
		Item destroyItem = null;
		final Collection<Item> items = getAllItemsByItemId(itemId);
		for (Item item : items)
		{
			destroyItem = item;
			if (!destroyItem.isEquipped())
			{
				break;
			}
		}
		
		// No item found.
		if (destroyItem == null)
		{
			return null;
		}
		
		// Support destroying multiple non stackable items.
		if (!destroyItem.isStackable() && (count > 1))
		{
			if (getInventoryItemCount(itemId, -1, false) >= count)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				long destroyed = 0;
				for (Item item : items)
				{
					if (!item.isEquipped() && (destroyItem(process, item, 1, actor, reference) != null))
					{
						iu.addRemovedItem(item);
						if (++destroyed == count)
						{
							_owner.sendInventoryUpdate(iu);
							refreshWeight();
							return item;
						}
					}
				}
			}
			else
			{
				return null;
			}
		}
		
		// Single item or stackable.
		return destroyItem(process, destroyItem, count, actor, reference);
	}
	
	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public Item dropItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		final Item droppedItem = super.dropItem(process, item, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if ((droppedItem != null) && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, droppedItem.getTemplate()))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, droppedItem, droppedItem.getLocation()), droppedItem.getTemplate());
		}
		
		return droppedItem;
	}
	
	/**
	 * Drop item from inventory by using its <b>objectID</b> and checks _adena and _ancientAdena
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public Item dropItem(ItemProcessType process, int objectId, int count, Player actor, Object reference)
	{
		final Item item = super.dropItem(process, objectId, count, actor, reference);
		
		if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId())))
		{
			_adena = null;
		}
		
		if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId())))
		{
			_ancientAdena = null;
		}
		
		// Notify to scripts
		if ((item != null) && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_DROP, item.getTemplate()))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, item, item.getLocation()), item.getTemplate());
		}
		
		return item;
	}
	
	/**
	 * Adds item to inventory for further adjustments.
	 * @param item : Item to be added from inventory
	 */
	@Override
	protected void addItem(Item item)
	{
		super.addItem(item);
	}
	
	/**
	 * <b>Overloaded</b>, when removes item from inventory, remove also owner shortcuts.
	 * @param item : Item to be removed from inventory
	 */
	@Override
	protected boolean removeItem(Item item)
	{
		// Removes any reference to the item from Shortcut bar
		_owner.removeItemFromShortcut(item.getObjectId());
		
		// Removes active Enchant Scroll
		if (item.getObjectId() == _owner.getActiveEnchantItemId())
		{
			_owner.setActiveEnchantItemId(Player.ID_NONE);
		}
		
		if (item.getId() == ADENA_ID)
		{
			_adena = null;
		}
		else if (item.getId() == ANCIENT_ADENA_ID)
		{
			_ancientAdena = null;
		}
		
		return super.removeItem(item);
	}
	
	/**
	 * @return the quantity of items in the inventory
	 */
	public int getNonQuestSize()
	{
		return _items.size();
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	public void refreshWeight()
	{
		super.refreshWeight();
		_owner.refreshOverloaded();
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}
	
	public static int[][] restoreVisibleInventory(int objectId)
	{
		final int[][] paperdoll = new int[31][3];
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'"))
		{
			ps.setInt(1, objectId);
			try (ResultSet invdata = ps.executeQuery())
			{
				while (invdata.next())
				{
					final int slot = invdata.getInt("loc_data");
					paperdoll[slot][0] = invdata.getInt("object_id");
					if (TransmogConfig.ENABLE_TRANSMOG)
					{
						final ItemVariables vars = new ItemVariables(paperdoll[slot][0]);
						paperdoll[slot][1] = vars.getInt(ItemVariables.TRANSMOG_ID, invdata.getInt("item_id"));
					}
					else
					{
						final int itemId = invdata.getInt("item_id");
						final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
						paperdoll[slot][1] = template == null ? itemId : template.getDisplayId();
					}
					
					paperdoll[slot][2] = invdata.getInt("enchant_level");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore inventory: " + e.getMessage(), e);
		}
		
		return paperdoll;
	}
	
	/**
	 * @param itemList the items that needs to be validated.
	 * @param sendMessage if {@code true} will send a message of inventory full.
	 * @param sendSkillMessage if {@code true} will send a message of skill not available.
	 * @return {@code true} if the inventory isn't full after taking new items and items weight add to current load doesn't exceed max weight load.
	 */
	public boolean checkInventorySlotsAndWeight(List<ItemTemplate> itemList, boolean sendMessage, boolean sendSkillMessage)
	{
		int lootWeight = 0;
		int requiredSlots = 0;
		if (itemList != null)
		{
			for (ItemTemplate item : itemList)
			{
				// If the item is not stackable or is stackable and not present in inventory, will need a slot.
				if (!item.isStackable() || (getInventoryItemCount(item.getId(), -1) <= 0))
				{
					requiredSlots++;
				}
				
				lootWeight += item.getWeight();
			}
		}
		
		final boolean inventoryStatusOK = validateCapacity(requiredSlots) && validateWeight(lootWeight);
		if (!inventoryStatusOK && sendMessage)
		{
			_owner.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			if (sendSkillMessage)
			{
				_owner.sendPacket(SystemMessageId.WEIGHT_AND_VOLUME_LIMIT_HAS_BEEN_EXCEEDED_THAT_SKILL_IS_CURRENTLY_UNAVAILABLE);
			}
		}
		
		return inventoryStatusOK;
	}
	
	/**
	 * If the item is not stackable or is stackable and not present in inventory, will need a slot.
	 * @param item the item to validate.
	 * @return {@code true} if there is enough room to add the item inventory.
	 */
	public boolean validateCapacity(Item item)
	{
		int slots = 0;
		if (!item.isStackable() || ((getInventoryItemCount(item.getId(), -1) <= 0) && !item.getTemplate().hasExImmediateEffect()))
		{
			slots++;
		}
		
		return validateCapacity(slots, item.isQuestItem());
	}
	
	/**
	 * If the item is not stackable or is stackable and not present in inventory, will need a slot.
	 * @param itemId the item Id for the item to validate.
	 * @return {@code true} if there is enough room to add the item inventory.
	 */
	public boolean validateCapacityByItemId(int itemId)
	{
		int slots = 0;
		final Item invItem = getItemByItemId(itemId);
		if ((invItem == null) || !invItem.isStackable())
		{
			slots++;
		}
		
		return validateCapacity(slots, ItemData.getInstance().getTemplate(itemId).isQuestItem());
	}
	
	@Override
	public boolean validateCapacity(long slots)
	{
		return validateCapacity(slots, false);
	}
	
	public boolean validateCapacity(long slots, boolean questItem)
	{
		return ((slots == 0) && !PlayerConfig.AUTO_LOOT_SLOT_LIMIT) || ((getNonQuestSize() + slots) <= _owner.getInventoryLimit());
	}
	
	@Override
	public boolean validateWeight(long weight)
	{
		// Disable weight check for GMs.
		if (_owner.isGM() && _owner.getDietMode() && _owner.getAccessLevel().allowTransaction())
		{
			return true;
		}
		
		return ((_totalWeight + weight) <= _owner.getMaxLoad());
	}
	
	/**
	 * Set inventory block for specified IDs<br>
	 * array reference is used for {@link PlayerInventory#_blockItems}
	 * @param items array of Ids to block/allow
	 * @param mode blocking mode {@link PlayerInventory#_blockMode}
	 */
	public void setInventoryBlock(int[] items, int mode)
	{
		_blockMode = mode;
		_blockItems = items;
		
		_owner.sendItemList(false);
	}
	
	/**
	 * Unblock blocked itemIds
	 */
	public void unblock()
	{
		_blockMode = -1;
		_blockItems = null;
		
		_owner.sendItemList(false);
	}
	
	/**
	 * Check if player inventory is in block mode.
	 * @return true if some itemIds blocked
	 */
	public boolean hasInventoryBlock()
	{
		return (_blockMode > -1) && (_blockItems != null) && (_blockItems.length > 0);
	}
	
	/**
	 * Block all player items
	 */
	public void blockAllItems()
	{
		// temp fix, some id must be sended
		setInventoryBlock(new int[]
		{
			(ItemData.getInstance().getArraySize() + 2)
		}, 1);
	}
	
	/**
	 * Return block mode
	 * @return int {@link PlayerInventory#_blockMode}
	 */
	public int getBlockMode()
	{
		return _blockMode;
	}
	
	/**
	 * Return int[] array with blocked item ids
	 * @return int[] array
	 */
	public int[] getBlockItems()
	{
		return _blockItems;
	}
	
	/**
	 * Check if player can use item by itemid
	 * @param itemId int
	 * @return true if can use
	 */
	public boolean canManipulateWithItemId(int itemId)
	{
		return ((_blockMode != 0) || !ArrayUtil.contains(_blockItems, itemId)) && ((_blockMode != 1) || ArrayUtil.contains(_blockItems, itemId));
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
	
	/**
	 * Apply skills of inventory items
	 */
	public void applyItemSkills()
	{
		for (Item item : _items)
		{
			item.giveSkillsToOwner();
		}
	}
}
