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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.EventMonster;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.item.OnItemCreate;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * Manages the creation and destruction of items within the game.
 * @author Mobius
 */
public class ItemManager
{
	private static final Logger LOGGER = Logger.getLogger(ItemManager.class.getName());
	private static final Logger LOGGER_ITEMS = Logger.getLogger("item");
	
	private ItemManager()
	{
	}
	
	/**
	 * Creates a new item based on the specified parameters and logs the creation process.<br>
	 * This overloaded version does not require a reference object.
	 * @param process an ItemProcessType identifier of the process triggering this action
	 * @param itemId the item ID of the item to be created
	 * @param count the quantity of the item for stackable items
	 * @param actor the player requesting the item creation
	 * @return the created item
	 */
	public static Item createItem(ItemProcessType process, int itemId, int count, Player actor)
	{
		return createItem(process, itemId, count, actor, null);
	}
	
	/**
	 * Creates an item with a specified ID, quantity, and logs the creation.<br>
	 * This method handles loot privileges, adds the item to the world, configures logging settings and notifies scripts of the creation.
	 * @param process an ItemProcessType identifier for the process triggering the action (e.g., "LOOT" or "BUY")
	 * @param itemId the unique ID of the item to create
	 * @param count the quantity of the item for stackable items
	 * @param actor the creature (typically a player) requesting the item creation
	 * @param reference an object referencing the action, such as an NPC selling an item or previous item in transformation
	 * @return the created item with the specified parameters
	 */
	public static Item createItem(ItemProcessType process, int itemId, int count, Creature actor, Object reference)
	{
		// Create and Init the Item corresponding to the Item Identifier.
		final Item item = new Item(IdManager.getInstance().getNextId(), itemId);
		if ((process == ItemProcessType.LOOT) && !PlayerConfig.AUTO_LOOT_ITEM_IDS.contains(itemId))
		{
			ScheduledFuture<?> itemLootShedule;
			if ((reference instanceof Attackable) && ((Attackable) reference).isRaid()) // Loot privilege for raids.
			{
				// if in CommandChannel and was killing a World/RaidBoss.
				final Attackable raid = (Attackable) reference;
				if ((raid.getFirstCommandChannelAttacked() != null) && !PlayerConfig.AUTO_LOOT_RAIDS)
				{
					item.setOwnerId(raid.getFirstCommandChannelAttacked().getLeaderObjectId());
					itemLootShedule = ThreadPool.schedule(new ResetOwner(item), PlayerConfig.LOOT_RAIDS_PRIVILEGE_INTERVAL);
					item.setItemLootShedule(itemLootShedule);
				}
			}
			else if (!PlayerConfig.AUTO_LOOT || ((reference instanceof EventMonster) && ((EventMonster) reference).eventDropOnGround()))
			{
				item.setOwnerId(actor.getObjectId());
				itemLootShedule = ThreadPool.schedule(new ResetOwner(item), 15000);
				item.setItemLootShedule(itemLootShedule);
			}
		}
		
		// Add the Item object to the World.
		World.getInstance().addObject(item);
		
		// Set Item parameters.
		if (item.isStackable() && (count > 1))
		{
			item.setCount(count);
		}
		
		if ((GeneralConfig.LOG_ITEMS && ((!GeneralConfig.LOG_ITEMS_SMALL_LOG) && (!GeneralConfig.LOG_ITEMS_IDS_ONLY))) || (GeneralConfig.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == Inventory.ADENA_ID))) || (GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(item.getId())))
		{
			if (item.isEnchanted())
			{
				LOGGER_ITEMS.info(StringUtil.concat("CREATE:", String.valueOf(process), ", item ", String.valueOf(item.getObjectId()), ":+", String.valueOf(item.getEnchantLevel()), " ", item.getTemplate().getName(), "(", String.valueOf(item.getCount()), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
			}
			else
			{
				LOGGER_ITEMS.info(StringUtil.concat("CREATE:", String.valueOf(process), ", item ", String.valueOf(item.getObjectId()), ":", item.getTemplate().getName(), "(", String.valueOf(item.getCount()), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
			}
		}
		
		if ((actor != null) && actor.isGM() && GeneralConfig.GMAUDIT)
		{
			String referenceName = "no-reference";
			if (reference instanceof WorldObject)
			{
				referenceName = (((WorldObject) reference).getName() != null ? ((WorldObject) reference).getName() : "no-name");
			}
			else if (reference instanceof String)
			{
				referenceName = (String) reference;
			}
			
			final String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
			GMAudit.logAction(String.valueOf(actor), StringUtil.concat(String.valueOf(process), "(id: ", String.valueOf(itemId), " count: ", String.valueOf(count), " name: ", item.getItemName(), " objId: ", String.valueOf(item.getObjectId()), ")"), targetName, StringUtil.concat("Object referencing this action is: ", referenceName));
		}
		
		// Notify to scripts.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_CREATE, item.getTemplate()))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnItemCreate(item, actor, reference), item.getTemplate());
		}
		
		return item;
	}
	
	/**
	 * Destroys the specified item, making it unusable, removes it from the world and logs the deletion.<br>
	 * This method also handles special cases, such as deleting pets associated with pet control items.
	 * @param process an ItemProcessType identifier for the process triggering this action
	 * @param item the item instance to be destroyed
	 * @param actor the player requesting the item destruction
	 * @param reference an object referencing the action, such as an NPC or another item
	 */
	public static void destroyItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		synchronized (item)
		{
			final long old = item.getCount();
			item.setCount(0);
			item.setOwnerId(0);
			item.setItemLocation(ItemLocation.VOID);
			item.setLastChange(Item.REMOVED);
			
			World.getInstance().removeObject(item);
			IdManager.getInstance().releaseId(item.getObjectId());
			
			if ((process != null) && (process != ItemProcessType.NONE))
			{
				if ((GeneralConfig.LOG_ITEMS && ((!GeneralConfig.LOG_ITEMS_SMALL_LOG) && (!GeneralConfig.LOG_ITEMS_IDS_ONLY))) || (GeneralConfig.LOG_ITEMS_SMALL_LOG && (item.isEquipable() || (item.getId() == Inventory.ADENA_ID))) || (GeneralConfig.LOG_ITEMS_IDS_ONLY && GeneralConfig.LOG_ITEMS_IDS_LIST.contains(item.getId())))
				{
					if (item.isEnchanted())
					{
						LOGGER_ITEMS.info(StringUtil.concat("DELETE:", String.valueOf(process), ", item ", String.valueOf(item.getObjectId()), ":+", String.valueOf(item.getEnchantLevel()), " ", item.getTemplate().getName(), "(", String.valueOf(item.getCount()), "), PrevCount(", String.valueOf(old), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
					}
					else
					{
						LOGGER_ITEMS.info(StringUtil.concat("DELETE:", String.valueOf(process), ", item ", String.valueOf(item.getObjectId()), ":", item.getTemplate().getName(), "(", String.valueOf(item.getCount()), "), PrevCount(", String.valueOf(old), "), ", String.valueOf(actor), ", ", String.valueOf(reference)));
					}
				}
				
				if ((actor != null) && actor.isGM() && GeneralConfig.GMAUDIT)
				{
					String referenceName = "no-reference";
					if (reference instanceof WorldObject)
					{
						referenceName = (((WorldObject) reference).getName() != null ? ((WorldObject) reference).getName() : "no-name");
					}
					else if (reference instanceof String)
					{
						referenceName = (String) reference;
					}
					
					final String targetName = (actor.getTarget() != null ? actor.getTarget().getName() : "no-target");
					GMAudit.logAction(String.valueOf(actor), StringUtil.concat(String.valueOf(process), "(id: ", String.valueOf(item.getId()), " count: ", String.valueOf(item.getCount()), " itemObjId: ", String.valueOf(item.getObjectId()), ")"), targetName, StringUtil.concat("Object referencing this action is: ", referenceName));
				}
			}
			
			// If it's a pet control item, delete the pet as well.
			if (item.getTemplate().isPetItem())
			{
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
				{
					// Delete the pet in database.
					statement.setInt(1, item.getObjectId());
					statement.execute();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "ItemManager: Could not delete pet objectid:", e);
				}
			}
		}
	}
	
	/**
	 * Runnable class that resets the owner of an item to 0 after a specified time interval. <br>
	 * This is typically used for loot items that need a temporary owner before becoming free to collect.
	 */
	protected static class ResetOwner implements Runnable
	{
		Item _item;
		
		public ResetOwner(Item item)
		{
			_item = item;
		}
		
		@Override
		public void run()
		{
			// Set owner id to 0 only when location is VOID.
			if (_item.getItemLocation() == ItemLocation.VOID)
			{
				_item.setOwnerId(0);
			}
			
			_item.setItemLootShedule(null);
		}
	}
}
