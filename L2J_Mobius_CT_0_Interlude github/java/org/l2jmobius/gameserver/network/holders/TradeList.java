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
package org.l2jmobius.gameserver.network.holders;

import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.MAX_ADENA;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Advi
 */
public class TradeList
{
	private static final Logger LOGGER = Logger.getLogger(TradeList.class.getName());
	
	private final Player _owner;
	private Player _partner;
	private final Set<TradeItem> _items = ConcurrentHashMap.newKeySet();
	private String _title;
	private boolean _packaged;
	
	private boolean _confirmed = false;
	private boolean _locked = false;
	
	public TradeList(Player owner)
	{
		_owner = owner;
	}
	
	public Player getOwner()
	{
		return _owner;
	}
	
	public void setPartner(Player partner)
	{
		_partner = partner;
	}
	
	public Player getPartner()
	{
		return _partner;
	}
	
	public void setTitle(String title)
	{
		_title = title;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public boolean isLocked()
	{
		return _locked;
	}
	
	public boolean isConfirmed()
	{
		return _confirmed;
	}
	
	public boolean isPackaged()
	{
		return _packaged;
	}
	
	public void setPackaged(boolean value)
	{
		_packaged = value;
	}
	
	/**
	 * @return all items from TradeList
	 */
	public Collection<TradeItem> getItems()
	{
		return _items;
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param inventory
	 * @return Item : items in inventory
	 */
	public Collection<TradeItem> getAvailableItems(PlayerInventory inventory)
	{
		final List<TradeItem> list = new LinkedList<>();
		for (TradeItem item : _items)
		{
			item = new TradeItem(item, item.getCount(), item.getPrice());
			inventory.adjustAvailableItem(item);
			list.add(item);
		}
		
		return list;
	}
	
	/**
	 * @return Item List size
	 */
	public int getItemCount()
	{
		return _items.size();
	}
	
	/**
	 * Adjust available item from Inventory by the one in this list
	 * @param item : Item to be adjusted
	 * @return TradeItem representing adjusted item
	 */
	public TradeItem adjustAvailableItem(Item item)
	{
		if (item.isStackable())
		{
			for (TradeItem exclItem : _items)
			{
				if (exclItem.getItem().getId() == item.getId())
				{
					return item.getCount() <= exclItem.getCount() ? null : new TradeItem(item, item.getCount() - exclItem.getCount(), item.getReferencePrice());
				}
			}
		}
		
		return new TradeItem(item, item.getCount(), item.getReferencePrice());
	}
	
	/**
	 * Add simplified item to TradeList
	 * @param objectId : int
	 * @param count : int
	 * @return
	 */
	public TradeItem addItem(int objectId, int count)
	{
		return addItem(objectId, count, 0);
	}
	
	/**
	 * Add item to TradeList
	 * @param objectId : int
	 * @param count : long
	 * @param price : long
	 * @return
	 */
	public synchronized TradeItem addItem(int objectId, int count, int price)
	{
		if (_locked)
		{
			return null;
		}
		
		final WorldObject o = World.getInstance().findObject(objectId);
		if (!(o instanceof Item))
		{
			return null;
		}
		
		final Item item = (Item) o;
		if (!(item.isTradeable() || (_owner.isGM() && GeneralConfig.GM_TRADE_RESTRICTED_ITEMS)) || item.isQuestItem())
		{
			return null;
		}
		
		if (!_owner.getInventory().canManipulateWithItemId(item.getId()))
		{
			return null;
		}
		
		if ((count <= 0) || (count > item.getCount()))
		{
			return null;
		}
		
		if (!item.isStackable() && (count > 1))
		{
			return null;
		}
		
		if ((MAX_ADENA / count) < price)
		{
			return null;
		}
		
		for (TradeItem checkitem : _items)
		{
			if (checkitem.getObjectId() == objectId)
			{
				return null;
			}
		}
		
		final TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);
		
		// If Player has already confirmed this trade, invalidate the confirmation
		invalidateConfirmation();
		return titem;
	}
	
	/**
	 * Add item to TradeList
	 * @param itemId
	 * @param count
	 * @param price
	 * @return
	 */
	public synchronized TradeItem addItemByItemId(int itemId, int count, int price)
	{
		if (_locked)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		if (item == null)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to add invalid item to TradeList!");
			return null;
		}
		
		if (!item.isTradeable() || item.isQuestItem())
		{
			return null;
		}
		
		if (!item.isStackable() && (count > 1))
		{
			LOGGER.warning(_owner.getName() + ": Attempt to add non-stackable item to TradeList with count > 1!");
			return null;
		}
		
		if ((MAX_ADENA / count) < price)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to overflow adena !");
			return null;
		}
		
		final TradeItem titem = new TradeItem(item, count, price);
		_items.add(titem);
		
		// If Player has already confirmed this trade, invalidate the confirmation
		invalidateConfirmation();
		return titem;
	}
	
	/**
	 * Remove item from TradeList
	 * @param objectId : int
	 * @param itemId
	 * @param count : int
	 * @return
	 */
	private synchronized TradeItem removeItem(int objectId, int itemId, int count)
	{
		if (_locked)
		{
			LOGGER.warning(_owner.getName() + ": Attempt to modify locked TradeList!");
			return null;
		}
		
		for (TradeItem titem : _items)
		{
			if ((titem.getObjectId() == objectId) || (titem.getItem().getId() == itemId))
			{
				// If Partner has already confirmed this trade, invalidate the confirmation
				if (_partner != null)
				{
					final TradeList partnerList = _partner.getActiveTradeList();
					if (partnerList == null)
					{
						LOGGER.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
						return null;
					}
					
					partnerList.invalidateConfirmation();
				}
				
				// Reduce item count or complete item
				if ((count != -1) && (titem.getCount() > count))
				{
					titem.setCount(titem.getCount() - count);
				}
				else
				{
					_items.remove(titem);
				}
				
				return titem;
			}
		}
		
		return null;
	}
	
	/**
	 * Update items in TradeList according their quantity in owner inventory
	 */
	public synchronized void updateItems()
	{
		for (TradeItem titem : _items)
		{
			final Item item = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if ((item == null) || (titem.getCount() < 1))
			{
				removeItem(titem.getObjectId(), -1, -1);
			}
			else if (item.getCount() < titem.getCount())
			{
				titem.setCount(item.getCount());
			}
		}
	}
	
	/**
	 * Lockes TradeList, no further changes are allowed
	 */
	public void lock()
	{
		_locked = true;
	}
	
	/**
	 * Clears item list
	 */
	public synchronized void clear()
	{
		_items.clear();
		_locked = false;
	}
	
	/**
	 * Confirms TradeList
	 * @return : boolean
	 */
	public boolean confirm()
	{
		if (_confirmed)
		{
			return true; // Already confirmed
		}
		
		// If Partner has already confirmed this trade, proceed exchange
		if (_partner != null)
		{
			final TradeList partnerList = _partner.getActiveTradeList();
			if (partnerList == null)
			{
				LOGGER.warning(_partner.getName() + ": Trading partner (" + _partner.getName() + ") is invalid in this trade!");
				return false;
			}
			
			// Synchronization order to avoid deadlock
			TradeList sync1;
			TradeList sync2;
			if (getOwner().getObjectId() > partnerList.getOwner().getObjectId())
			{
				sync1 = partnerList;
				sync2 = this;
			}
			else
			{
				sync1 = this;
				sync2 = partnerList;
			}
			
			synchronized (sync1)
			{
				synchronized (sync2)
				{
					_confirmed = true;
					if (partnerList.isConfirmed())
					{
						partnerList.lock();
						lock();
						if (!partnerList.validate() || !validate())
						{
							return false;
						}
						
						doExchange(partnerList);
					}
					else
					{
						_partner.onTradeConfirm(_owner);
					}
				}
			}
		}
		else
		{
			_confirmed = true;
		}
		
		return _confirmed;
	}
	
	/**
	 * Cancels TradeList confirmation
	 */
	private void invalidateConfirmation()
	{
		_confirmed = false;
	}
	
	/**
	 * Validates TradeList with owner inventory
	 * @return
	 */
	private boolean validate()
	{
		// Check for Owner validity
		if ((_owner == null) || (World.getInstance().getPlayer(_owner.getObjectId()) == null))
		{
			LOGGER.warning("Invalid owner of TradeList");
			return false;
		}
		
		// Check for Item validity
		for (TradeItem titem : _items)
		{
			final Item item = _owner.checkItemManipulation(titem.getObjectId(), titem.getCount(), "transfer");
			if ((item == null) || (item.getCount() < 1))
			{
				LOGGER.warning(_owner.getName() + ": Invalid Item in TradeList");
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Transfers all TradeItems from inventory to partner
	 * @param partner
	 * @param ownerIU
	 * @param partnerIU
	 * @return
	 */
	private boolean TransferItems(Player partner, InventoryUpdate ownerIU, InventoryUpdate partnerIU)
	{
		for (TradeItem titem : _items)
		{
			final Item oldItem = _owner.getInventory().getItemByObjectId(titem.getObjectId());
			if (oldItem == null)
			{
				return false;
			}
			
			final Item newItem = _owner.getInventory().transferItem(ItemProcessType.TRANSFER, titem.getObjectId(), titem.getCount(), partner.getInventory(), _owner, _partner);
			if (newItem == null)
			{
				return false;
			}
			
			// Add changes to inventory update packets
			if (ownerIU != null)
			{
				if ((oldItem.getCount() > 0) && (oldItem != newItem))
				{
					ownerIU.addModifiedItem(oldItem);
				}
				else
				{
					ownerIU.addRemovedItem(oldItem);
				}
			}
			
			if (partnerIU != null)
			{
				if (newItem.getCount() > titem.getCount())
				{
					partnerIU.addModifiedItem(newItem);
				}
				else
				{
					partnerIU.addNewItem(newItem);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * @param partner
	 * @return items slots count
	 */
	private int countItemsSlots(Player partner)
	{
		int slots = 0;
		for (TradeItem item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			final ItemTemplate template = ItemData.getInstance().getTemplate(item.getItem().getId());
			if (template == null)
			{
				continue;
			}
			
			if (!template.isStackable())
			{
				slots += item.getCount();
			}
			else if (partner.getInventory().getItemByItemId(item.getItem().getId()) == null)
			{
				slots++;
			}
		}
		
		return slots;
	}
	
	/**
	 * @return the weight of items in tradeList
	 */
	private int calcItemsWeight()
	{
		long weight = 0;
		for (TradeItem item : _items)
		{
			if (item == null)
			{
				continue;
			}
			
			final ItemTemplate template = ItemData.getInstance().getTemplate(item.getItem().getId());
			if (template == null)
			{
				continue;
			}
			
			weight += item.getCount() * template.getWeight();
		}
		
		return (int) Math.min(weight, Integer.MAX_VALUE);
	}
	
	/**
	 * Proceeds with trade
	 * @param partnerList
	 */
	private void doExchange(TradeList partnerList)
	{
		boolean success = false;
		
		// check weight and slots
		if (!getOwner().getInventory().validateWeight(partnerList.calcItemsWeight()) || !partnerList.getOwner().getInventory().validateWeight(calcItemsWeight()))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			_owner.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
		}
		else if (!getOwner().getInventory().validateCapacity(partnerList.countItemsSlots(getOwner())) || !partnerList.getOwner().getInventory().validateCapacity(countItemsSlots(partnerList.getOwner())))
		{
			partnerList.getOwner().sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			_owner.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
		}
		else
		{
			// Prepare inventory update packet
			final InventoryUpdate ownerIU = new InventoryUpdate();
			final InventoryUpdate partnerIU = new InventoryUpdate();
			
			// Transfer items
			partnerList.TransferItems(_owner, partnerIU, ownerIU);
			TransferItems(partnerList.getOwner(), ownerIU, partnerIU);
			_owner.sendInventoryUpdate(ownerIU);
			_partner.sendInventoryUpdate(partnerIU);
			
			// Update current load as well
			StatusUpdate playerSU = new StatusUpdate(_owner);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, _owner.getCurrentLoad());
			_owner.sendPacket(playerSU);
			playerSU = new StatusUpdate(_partner);
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, _partner.getCurrentLoad());
			_partner.sendPacket(playerSU);
			
			success = true;
		}
		
		// Finish the trade
		partnerList.getOwner().onTradeFinish(success);
		_owner.onTradeFinish(success);
	}
	
	/**
	 * Buy items from this PrivateStore list
	 * @param player
	 * @param items
	 * @return int: result of trading. 0 - ok, 1 - canceled (no adena), 2 - failed (item error)
	 */
	public synchronized int privateStoreBuy(Player player, Set<RequestTrade> items)
	{
		if (_locked)
		{
			return 1;
		}
		
		if (!validate())
		{
			lock();
			return 1;
		}
		
		if (!_owner.isOnline() || !player.isOnline())
		{
			return 1;
		}
		
		int slots = 0;
		int weight = 0;
		int totalPrice = 0;
		
		final PlayerInventory ownerInventory = _owner.getInventory();
		final PlayerInventory playerInventory = player.getInventory();
		for (RequestTrade item : items)
		{
			boolean found = false;
			for (TradeItem ti : _items)
			{
				if (ti.getObjectId() == item.getObjectId())
				{
					if (ti.getPrice() == item.getPrice())
					{
						if (ti.getCount() < item.getCount())
						{
							item.setCount(ti.getCount());
						}
						
						found = true;
					}
					break;
				}
			}
			
			// item with this objectId and price not found in tradelist
			if (!found)
			{
				if (_packaged)
				{
					PunishmentManager.handleIllegalPlayerAction(player, "[TradeList.privateStoreBuy()] " + player + " tried to cheat the package sell and buy only a part of the package! Ban this player for bot usage!", GeneralConfig.DEFAULT_PUNISH);
					return 2;
				}
				
				item.setCount(0);
				continue;
			}
			
			// check for overflow in the single item
			if ((MAX_ADENA / item.getCount()) < item.getPrice())
			{
				// private store attempting to overflow - disable it
				lock();
				return 1;
			}
			
			totalPrice += item.getCount() * item.getPrice();
			
			// check for overflow of the total price
			if ((MAX_ADENA < totalPrice) || (totalPrice < 0))
			{
				// private store attempting to overflow - disable it
				lock();
				return 1;
			}
			
			// Check if requested item is available for manipulation
			final Item oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
			if ((oldItem == null) || !oldItem.isTradeable())
			{
				// private store sell invalid item - disable it
				lock();
				return 2;
			}
			
			final ItemTemplate template = ItemData.getInstance().getTemplate(item.getItemId());
			if (template == null)
			{
				continue;
			}
			
			weight += item.getCount() * template.getWeight();
			if (!template.isStackable())
			{
				slots += item.getCount();
			}
			else if (playerInventory.getItemByItemId(item.getItemId()) == null)
			{
				slots++;
			}
		}
		
		if (totalPrice > playerInventory.getAdena())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return 1;
		}
		
		if (!playerInventory.validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return 1;
		}
		
		if (!playerInventory.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			return 1;
		}
		
		// Prepare inventory update packets
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		final Item adenaItem = playerInventory.getAdenaInstance();
		if (!playerInventory.reduceAdena(ItemProcessType.BUY, totalPrice, player, _owner))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return 1;
		}
		
		playerIU.addItem(adenaItem);
		ownerInventory.addAdena(ItemProcessType.SELL, totalPrice, _owner, player);
		
		// ownerIU.addItem(ownerInventory.getAdenaInstance());
		boolean ok = true;
		
		// Transfer items
		for (RequestTrade item : items)
		{
			if (item.getCount() == 0)
			{
				continue;
			}
			
			// Check if requested item is available for manipulation
			final Item oldItem = _owner.checkItemManipulation(item.getObjectId(), item.getCount(), "sell");
			if (oldItem == null)
			{
				// should not happens - validation already done
				lock();
				ok = false;
				break;
			}
			
			// Proceed with item transfer
			final Item newItem = ownerInventory.transferItem(ItemProcessType.TRANSFER, item.getObjectId(), item.getCount(), playerInventory, _owner, player);
			if (newItem == null)
			{
				ok = false;
				break;
			}
			
			removeItem(item.getObjectId(), -1, item.getCount());
			
			// Add changes to inventory update packets
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				ownerIU.addModifiedItem(oldItem);
			}
			else
			{
				ownerIU.addRemovedItem(oldItem);
			}
			
			if (newItem.getCount() > item.getCount())
			{
				playerIU.addModifiedItem(newItem);
			}
			else
			{
				playerIU.addNewItem(newItem);
			}
			
			// Send messages about the transaction to both players
			SystemMessage msg;
			if (newItem.isStackable())
			{
				msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				msg.addString(player.getName());
				msg.addItemName(newItem);
				msg.addInt(item.getCount());
				_owner.sendPacket(msg);
				msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S3_S2_S_FROM_S1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
				msg.addInt(item.getCount());
			}
			else
			{
				msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S2);
				msg.addString(player.getName());
				msg.addItemName(newItem);
				_owner.sendPacket(msg);
				msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S2_FROM_S1);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
			}
			
			player.sendPacket(msg);
		}
		
		// Send inventory update packet
		_owner.sendInventoryUpdate(ownerIU);
		player.sendInventoryUpdate(playerIU);
		return ok ? 0 : 2;
	}
	
	/**
	 * Sell items to this PrivateStore list
	 * @param player
	 * @param items
	 * @return : boolean true if success
	 */
	public synchronized boolean privateStoreSell(Player player, RequestTrade[] items)
	{
		if (_locked || !_owner.isOnline() || !player.isOnline())
		{
			return false;
		}
		
		boolean ok = false;
		
		final PlayerInventory ownerInventory = _owner.getInventory();
		final PlayerInventory playerInventory = player.getInventory();
		
		// Prepare inventory update packet
		final InventoryUpdate ownerIU = new InventoryUpdate();
		final InventoryUpdate playerIU = new InventoryUpdate();
		int totalPrice = 0;
		for (RequestTrade item : items)
		{
			// searching item in tradelist using itemId
			boolean found = false;
			for (TradeItem ti : _items)
			{
				if (ti.getItem().getId() == item.getItemId())
				{
					// price should be the same
					if (ti.getPrice() == item.getPrice())
					{
						// if requesting more than available - decrease count
						if (ti.getCount() < item.getCount())
						{
							item.setCount(ti.getCount());
						}
						
						found = item.getCount() > 0;
					}
					break;
				}
			}
			
			// not found any item in the tradelist with same itemId and price
			// maybe another player already sold this item ?
			if (!found)
			{
				continue;
			}
			
			// check for overflow in the single item
			if ((MAX_ADENA / item.getCount()) < item.getPrice())
			{
				lock();
				break;
			}
			
			final int _totalPrice = totalPrice + (item.getCount() * item.getPrice());
			
			// check for overflow of the total price
			if ((MAX_ADENA < _totalPrice) || (_totalPrice < 0))
			{
				lock();
				break;
			}
			
			if (ownerInventory.getAdena() < _totalPrice)
			{
				continue;
			}
			
			// Check if requested item is available for manipulation
			int objectId = item.getObjectId();
			Item oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
			// private store - buy use same objectId for buying several non-stackable items
			if (oldItem == null)
			{
				// searching other items using same itemId
				oldItem = playerInventory.getItemByItemId(item.getItemId());
				if (oldItem == null)
				{
					continue;
				}
				
				objectId = oldItem.getObjectId();
				oldItem = player.checkItemManipulation(objectId, item.getCount(), "sell");
				if (oldItem == null)
				{
					continue;
				}
			}
			
			if (oldItem.getId() != item.getItemId())
			{
				PunishmentManager.handleIllegalPlayerAction(player, player + " is cheating with sell items", GeneralConfig.DEFAULT_PUNISH);
				return false;
			}
			
			if (!oldItem.isTradeable())
			{
				continue;
			}
			
			// Proceed with item transfer
			final Item newItem = playerInventory.transferItem(ItemProcessType.TRANSFER, objectId, item.getCount(), ownerInventory, player, _owner);
			if (newItem == null)
			{
				continue;
			}
			
			removeItem(-1, item.getItemId(), item.getCount());
			ok = true;
			
			// increase total price only after successful transaction
			totalPrice = _totalPrice;
			
			// Add changes to inventory update packets
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			
			if (newItem.getCount() > item.getCount())
			{
				ownerIU.addModifiedItem(newItem);
			}
			else
			{
				ownerIU.addNewItem(newItem);
			}
			
			// Send messages about the transaction to both players
			SystemMessage msg;
			if (newItem.isStackable())
			{
				msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S3_S2_S_FROM_S1);
				msg.addString(player.getName());
				msg.addItemName(newItem);
				msg.addInt(item.getCount());
				_owner.sendPacket(msg);
				msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
				msg.addInt(item.getCount());
			}
			else
			{
				msg = new SystemMessage(SystemMessageId.YOU_HAVE_PURCHASED_S2_FROM_S1);
				msg.addString(player.getName());
				msg.addItemName(newItem);
				_owner.sendPacket(msg);
				msg = new SystemMessage(SystemMessageId.S1_PURCHASED_S2);
				msg.addString(_owner.getName());
				msg.addItemName(newItem);
			}
			
			player.sendPacket(msg);
		}
		
		if (totalPrice > 0)
		{
			// Transfer adena
			if (totalPrice > ownerInventory.getAdena())
			{
				// should not happens, just a precaution
				return false;
			}
			
			final Item adenaItem = ownerInventory.getAdenaInstance();
			ownerInventory.reduceAdena(ItemProcessType.BUY, totalPrice, _owner, player);
			ownerIU.addItem(adenaItem);
			playerInventory.addAdena(ItemProcessType.SELL, totalPrice, player, _owner);
			playerIU.addItem(playerInventory.getAdenaInstance());
		}
		
		if (ok)
		{
			// Send inventory update packet
			_owner.sendInventoryUpdate(ownerIU);
			player.sendInventoryUpdate(playerIU);
		}
		
		return ok;
	}
}
