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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerItemUnequip;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;

/**
 * This class manages inventory
 * @version $Revision: 1.13.2.9.2.12 $ $Date: 2005/03/29 23:15:15 $ rewritten 23.2.2006 by Advi
 */
public abstract class Inventory extends ItemContainer
{
	protected static final Logger LOGGER = Logger.getLogger(Inventory.class.getName());
	
	public interface PaperdollListener
	{
		void notifyEquiped(int slot, Item inst, Inventory inventory);
		
		void notifyUnequiped(int slot, Item inst, Inventory inventory);
	}
	
	// Common Items
	public static final int ADENA_ID = 57;
	public static final int ANCIENT_ADENA_ID = 5575;
	
	public static final int MAX_ADENA = PlayerConfig.MAX_ADENA;
	
	public static final int PAPERDOLL_UNDER = 0;
	public static final int PAPERDOLL_LEAR = 1;
	public static final int PAPERDOLL_REAR = 2;
	public static final int PAPERDOLL_NECK = 3;
	public static final int PAPERDOLL_LFINGER = 4;
	public static final int PAPERDOLL_RFINGER = 5;
	public static final int PAPERDOLL_HEAD = 6;
	public static final int PAPERDOLL_RHAND = 7;
	public static final int PAPERDOLL_LHAND = 8;
	public static final int PAPERDOLL_GLOVES = 9;
	public static final int PAPERDOLL_CHEST = 10;
	public static final int PAPERDOLL_LEGS = 11;
	public static final int PAPERDOLL_FEET = 12;
	public static final int PAPERDOLL_CLOAK = 13;
	public static final int PAPERDOLL_FACE = 14;
	public static final int PAPERDOLL_HAIR = 15;
	public static final int PAPERDOLL_HAIR2 = 16;
	public static final int PAPERDOLL_TOTALSLOTS = 17;
	
	// Speed percentage mods
	public static final double MAX_ARMOR_WEIGHT = 12000;
	
	private final Item[] _paperdoll;
	private final List<PaperdollListener> _paperdollListeners;
	
	// protected to be accessed from child classes only
	protected int _totalWeight;
	
	// used to quickly check for using of items of special type
	private int _wearedMask;
	
	// Recorder of alterations in inventory
	private static class ChangeRecorder implements PaperdollListener
	{
		private final Inventory _inventory;
		private final List<Item> _changed = new ArrayList<>(1);
		
		/**
		 * Constructor of the ChangeRecorder
		 * @param inventory
		 */
		ChangeRecorder(Inventory inventory)
		{
			_inventory = inventory;
			_inventory.addPaperdollListener(this);
		}
		
		/**
		 * Add alteration in inventory when item equipped
		 * @param slot
		 * @param item
		 * @param inventory
		 */
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			_changed.add(item);
		}
		
		/**
		 * Add alteration in inventory when item unequipped
		 * @param slot
		 * @param item
		 * @param inventory
		 */
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			_changed.add(item);
		}
		
		/**
		 * Returns alterations in inventory
		 * @return Collection<Item> : Collection of altered items
		 */
		public List<Item> getChangedItems()
		{
			return _changed;
		}
	}
	
	private static class BowCrossRodListener implements PaperdollListener
	{
		private static BowCrossRodListener instance = new BowCrossRodListener();
		
		public static BowCrossRodListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			if (slot != PAPERDOLL_RHAND)
			{
				return;
			}
			
			if (item.getItemType() == WeaponType.BOW)
			{
				final Item arrow = inventory.getPaperdollItem(PAPERDOLL_LHAND);
				if (arrow != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			}
			else if (item.getItemType() == WeaponType.FISHINGROD)
			{
				final Item lure = inventory.getPaperdollItem(PAPERDOLL_LHAND);
				if (lure != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, null);
				}
			}
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			if (slot != PAPERDOLL_RHAND)
			{
				return;
			}
			
			if (item.getItemType() == WeaponType.BOW)
			{
				final Item arrow = inventory.findArrowForBow(item.getTemplate());
				if (arrow != null)
				{
					inventory.setPaperdollItem(PAPERDOLL_LHAND, arrow);
				}
			}
		}
	}
	
	private static class StatsListener implements PaperdollListener
	{
		private static StatsListener instance = new StatsListener();
		
		public static StatsListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			inventory.getOwner().removeStatsOwner(item);
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			inventory.getOwner().addStatFuncs(item.getStatFuncs(inventory.getOwner()));
		}
	}
	
	private static class ItemSkillsListener implements PaperdollListener
	{
		private static ItemSkillsListener instance = new ItemSkillsListener();
		
		public static ItemSkillsListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayer())
			{
				return;
			}
			
			final Player player = inventory.getOwner().asPlayer();
			Skill enchant4Skill;
			Skill itemSkill;
			final ItemTemplate it = item.getTemplate();
			boolean update = false;
			boolean updateTimeStamp = false;
			
			// Remove augmentation bonuses on unequip
			if (item.isAugmented())
			{
				item.getAugmentation().removeBonus(player);
			}
			
			// Remove skills bestowed from +4 armor
			if (item.getEnchantLevel() >= 4)
			{
				enchant4Skill = it.getEnchant4Skill();
				if (enchant4Skill != null)
				{
					player.removeSkill(enchant4Skill, false, enchant4Skill.isPassive());
					update = true;
				}
			}
			
			item.clearEnchantStats();
			
			final SkillHolder[] skills = it.getSkills();
			if (skills != null)
			{
				for (SkillHolder skillInfo : skills)
				{
					if (skillInfo == null)
					{
						continue;
					}
					
					itemSkill = skillInfo.getSkill();
					if (itemSkill != null)
					{
						player.removeSkill(itemSkill, false, itemSkill.isPassive());
						update = true;
					}
					else
					{
						LOGGER.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: " + skillInfo + ".");
					}
				}
			}
			
			if (item.isArmor())
			{
				for (Item itm : inventory.getItems())
				{
					if (!itm.isEquipped() || (itm.getTemplate().getSkills() == null) || itm.equals(item))
					{
						continue;
					}
					
					for (SkillHolder sk : itm.getTemplate().getSkills())
					{
						if (player.getSkillLevel(sk.getSkillId()) != 0)
						{
							continue;
						}
						
						itemSkill = sk.getSkill();
						if (itemSkill != null)
						{
							player.addSkill(itemSkill, false);
							if (itemSkill.isActive())
							{
								if (!player.hasSkillReuse(itemSkill.getReuseHashCode()))
								{
									final int equipDelay = item.getEquipReuseDelay();
									if (equipDelay > 0)
									{
										player.addTimeStamp(itemSkill, equipDelay);
										player.disableSkill(itemSkill, equipDelay);
									}
								}
								
								updateTimeStamp = true;
							}
							
							update = true;
						}
					}
				}
			}
			
			// Apply skill, if weapon have "skills on unequip"
			final Skill unequipSkill = it.getUnequipSkill();
			if (unequipSkill != null)
			{
				unequipSkill.activateSkill(player, Collections.singletonList(player));
			}
			
			if (update)
			{
				player.sendSkillList();
				
				if (updateTimeStamp)
				{
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayer())
			{
				return;
			}
			
			final Player player = inventory.getOwner().asPlayer();
			Skill enchant4Skill;
			Skill itemSkill;
			final ItemTemplate it = item.getTemplate();
			boolean update = false;
			boolean updateTimeStamp = false;
			
			// Apply augmentation bonuses on equip
			if (item.isAugmented())
			{
				item.getAugmentation().applyBonus(player);
			}
			
			// Add skills bestowed from +4 armor
			if (item.getEnchantLevel() >= 4)
			{
				enchant4Skill = it.getEnchant4Skill();
				if (enchant4Skill != null)
				{
					player.addSkill(enchant4Skill, false);
					update = true;
				}
			}
			
			final SkillHolder[] skills = it.getSkills();
			if (skills != null)
			{
				for (SkillHolder skillInfo : skills)
				{
					if (skillInfo == null)
					{
						continue;
					}
					
					itemSkill = skillInfo.getSkill();
					if (itemSkill != null)
					{
						itemSkill.setReferenceItemId(item.getId());
						player.addSkill(itemSkill, false);
						if (itemSkill.isActive())
						{
							if (!player.hasSkillReuse(itemSkill.getReuseHashCode()))
							{
								final int equipDelay = item.getEquipReuseDelay();
								if (equipDelay > 0)
								{
									player.addTimeStamp(itemSkill, equipDelay);
									player.disableSkill(itemSkill, equipDelay);
								}
							}
							
							updateTimeStamp = true;
						}
						
						update = true;
					}
					else
					{
						LOGGER.warning("Inventory.ItemSkillsListener.Weapon: Incorrect skill: " + skillInfo + ".");
					}
				}
			}
			
			if (update)
			{
				player.sendSkillList();
				
				if (updateTimeStamp)
				{
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
	}
	
	private static class ArmorSetListener implements PaperdollListener
	{
		private static ArmorSetListener instance = new ArmorSetListener();
		
		public static ArmorSetListener getInstance()
		{
			return instance;
		}
		
		@Override
		public void notifyEquiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayer())
			{
				return;
			}
			
			final Player player = inventory.getOwner().asPlayer();
			
			// Checks if player is wearing a chest item
			final Item chestItem = inventory.getPaperdollItem(PAPERDOLL_CHEST);
			if (chestItem == null)
			{
				return;
			}
			
			// Checks for armor set for the equipped chest.
			if (!ArmorSetData.getInstance().isArmorSet(chestItem.getId()))
			{
				return;
			}
			
			final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getId());
			boolean update = false;
			boolean updateTimeStamp = false;
			
			// Checks if equipped item is part of set
			if (armorSet.containItem(slot, item.getId()))
			{
				if (armorSet.containAll(player))
				{
					Skill itemSkill;
					final List<SkillHolder> skills = armorSet.getSkills();
					if (skills != null)
					{
						for (SkillHolder holder : skills)
						{
							itemSkill = holder.getSkill();
							if (itemSkill != null)
							{
								player.addSkill(itemSkill, false);
								if (itemSkill.isActive())
								{
									if (!player.hasSkillReuse(itemSkill.getReuseHashCode()))
									{
										final int equipDelay = item.getEquipReuseDelay();
										if (equipDelay > 0)
										{
											player.addTimeStamp(itemSkill, equipDelay);
											player.disableSkill(itemSkill, equipDelay);
										}
									}
									
									updateTimeStamp = true;
								}
								
								update = true;
							}
							else
							{
								LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
							}
						}
					}
					
					if (armorSet.containShield(player)) // has shield from set
					{
						for (SkillHolder holder : armorSet.getShieldSkillId())
						{
							if (holder.getSkill() != null)
							{
								player.addSkill(holder.getSkill(), false);
								update = true;
							}
							else
							{
								LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
							}
						}
					}
					
					if (armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
					{
						for (SkillHolder holder : armorSet.getEnchant6skillId())
						{
							if (holder.getSkill() != null)
							{
								player.addSkill(holder.getSkill(), false);
								update = true;
							}
							else
							{
								LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
							}
						}
					}
				}
			}
			else if (armorSet.containShield(item.getId()))
			{
				for (SkillHolder holder : armorSet.getShieldSkillId())
				{
					if (holder.getSkill() != null)
					{
						player.addSkill(holder.getSkill(), false);
						update = true;
					}
					else
					{
						LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
					}
				}
			}
			
			if (update)
			{
				player.sendSkillList();
				
				if (updateTimeStamp)
				{
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
		
		@Override
		public void notifyUnequiped(int slot, Item item, Inventory inventory)
		{
			if (!inventory.getOwner().isPlayer())
			{
				return;
			}
			
			final Player player = inventory.getOwner().asPlayer();
			boolean remove = false;
			Skill itemSkill;
			List<SkillHolder> skills = null;
			List<SkillHolder> shieldSkill = null; // shield skill
			List<SkillHolder> skillId6 = null; // enchant +6 skill
			if (slot == PAPERDOLL_CHEST)
			{
				if (!ArmorSetData.getInstance().isArmorSet(item.getId()))
				{
					return;
				}
				
				final ArmorSet armorSet = ArmorSetData.getInstance().getSet(item.getId());
				remove = true;
				skills = armorSet.getSkills();
				shieldSkill = armorSet.getShieldSkillId();
				skillId6 = armorSet.getEnchant6skillId();
			}
			else
			{
				final Item chestItem = inventory.getPaperdollItem(PAPERDOLL_CHEST);
				if (chestItem == null)
				{
					return;
				}
				
				final ArmorSet armorSet = ArmorSetData.getInstance().getSet(chestItem.getId());
				if (armorSet == null)
				{
					return;
				}
				
				if (armorSet.containItem(slot, item.getId())) // removed part of set
				{
					remove = true;
					skills = armorSet.getSkills();
					shieldSkill = armorSet.getShieldSkillId();
					skillId6 = armorSet.getEnchant6skillId();
				}
				else if (armorSet.containShield(item.getId())) // removed shield
				{
					remove = true;
					shieldSkill = armorSet.getShieldSkillId();
				}
			}
			
			if (remove)
			{
				if (skills != null)
				{
					for (SkillHolder holder : skills)
					{
						itemSkill = holder.getSkill();
						if (itemSkill != null)
						{
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						}
						else
						{
							LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
						}
					}
				}
				
				if (shieldSkill != null)
				{
					for (SkillHolder holder : shieldSkill)
					{
						itemSkill = holder.getSkill();
						if (itemSkill != null)
						{
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						}
						else
						{
							LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
						}
					}
				}
				
				if (skillId6 != null)
				{
					for (SkillHolder holder : skillId6)
					{
						itemSkill = holder.getSkill();
						if (itemSkill != null)
						{
							player.removeSkill(itemSkill, false, itemSkill.isPassive());
						}
						else
						{
							LOGGER.warning("Inventory.ArmorSetListener: Incorrect skill: " + holder + ".");
						}
					}
				}
				
				player.checkItemRestriction();
				player.sendSkillList();
			}
		}
	}
	
	/**
	 * Constructor of the inventory
	 */
	protected Inventory()
	{
		_paperdoll = new Item[PAPERDOLL_TOTALSLOTS];
		_paperdollListeners = new ArrayList<>();
		if (this instanceof PlayerInventory)
		{
			addPaperdollListener(ArmorSetListener.getInstance());
			addPaperdollListener(BowCrossRodListener.getInstance());
			addPaperdollListener(ItemSkillsListener.getInstance());
		}
		
		// common
		addPaperdollListener(StatsListener.getInstance());
	}
	
	protected abstract ItemLocation getEquipLocation();
	
	/**
	 * Returns the instance of new ChangeRecorder
	 * @return ChangeRecorder
	 */
	private ChangeRecorder newRecorder()
	{
		return new ChangeRecorder(this);
	}
	
	/**
	 * Drop item from inventory and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param item : Item to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item dropItem(ItemProcessType process, Item item, Player actor, Object reference)
	{
		if (item == null)
		{
			return null;
		}
		
		synchronized (item)
		{
			if (!_items.contains(item))
			{
				return null;
			}
			
			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setItemLocation(ItemLocation.VOID);
			item.setLastChange(Item.REMOVED);
			
			item.updateDatabase();
			refreshWeight();
		}
		
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <b>objectID</b> and updates database
	 * @param process : ItemProcessType identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return Item corresponding to the destroyed item or the updated item in inventory
	 */
	public Item dropItem(ItemProcessType process, int objectId, int count, Player actor, Object reference)
	{
		Item item = getItemByObjectId(objectId);
		if (item == null)
		{
			return null;
		}
		
		synchronized (item)
		{
			if (!_items.contains(item))
			{
				return null;
			}
			
			// Adjust item quantity and create new instance to drop
			// Directly drop entire item
			if (item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(Item.MODIFIED);
				item.updateDatabase();
				
				final Item newItem = ItemManager.createItem(process, item.getId(), count, actor, reference);
				newItem.updateDatabase();
				refreshWeight();
				return newItem;
			}
		}
		
		return dropItem(process, item, actor, reference);
	}
	
	/**
	 * Adds item to inventory for further adjustments and Equip it if necessary (itemlocation defined)
	 * @param item : Item to be added from inventory
	 */
	@Override
	protected void addItem(Item item)
	{
		super.addItem(item);
		if (item.isEquipped())
		{
			equipItem(item);
		}
		
		if (item.getTemplate().getSkills() != null)
		{
			final Player owner = getOwner().asPlayer();
			if (owner != null)
			{
				owner.sendSkillList(); // Refresh client skill window.
			}
		}
	}
	
	/**
	 * Removes item from inventory for further adjustments.
	 * @param item : Item to be removed from inventory
	 */
	@Override
	protected boolean removeItem(Item item)
	{
		// Unequip item if equiped
		for (int i = 0; i < _paperdoll.length; i++)
		{
			if (_paperdoll[i] == item)
			{
				unEquipItemInSlot(i);
			}
		}
		
		if (item.getTemplate().getSkills() != null)
		{
			final Player owner = getOwner().asPlayer();
			if (owner != null)
			{
				owner.sendSkillList(); // Refresh client skill window.
			}
		}
		
		return super.removeItem(item);
	}
	
	/**
	 * @param slot the slot.
	 * @return the item in the paperdoll slot
	 */
	public Item getPaperdollItem(int slot)
	{
		return _paperdoll[slot];
	}
	
	/**
	 * @param slot the slot.
	 * @return {@code true} if specified paperdoll slot is empty, {@code false} otherwise
	 */
	public boolean isPaperdollSlotEmpty(int slot)
	{
		return _paperdoll[slot] == null;
	}
	
	public static int getPaperdollIndex(BodyPart bodyPart)
	{
		if (bodyPart == null)
		{
			return -1;
		}
		
		return bodyPart.getPaperdollSlot();
	}
	
	/**
	 * Gets the paperdoll item in the specified body part slot.
	 * @param bodyPart the body part
	 * @return the paperdoll item, or {@code null} if none is equipped
	 */
	public Item getPaperdollItemByBodyPart(BodyPart bodyPart)
	{
		final int index = getPaperdollIndex(bodyPart);
		if (index == -1)
		{
			return null;
		}
		
		return _paperdoll[index];
	}
	
	/**
	 * Returns the ID of the item in the paperdoll slot
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemId(int slot)
	{
		final Item item = _paperdoll[slot];
		if (item != null)
		{
			if (TransmogConfig.ENABLE_TRANSMOG)
			{
				final int transmogId = item.getTransmogId();
				if (transmogId > 0)
				{
					return transmogId;
				}
			}
			
			return item.getId();
		}
		
		return 0;
	}
	
	/**
	 * Returns the first paperdoll item with the specific id
	 * @param itemId the item id
	 * @return Item
	 */
	public Item getPaperdollItemByItemId(int itemId)
	{
		for (int i = 0; i < _paperdoll.length; i++)
		{
			final Item item = _paperdoll[i];
			if ((item != null) && (item.getId() == itemId))
			{
				return item;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the ID of the item in the paperdoll slot
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemDisplayId(int slot)
	{
		final Item item = _paperdoll[slot];
		if (item != null)
		{
			if (TransmogConfig.ENABLE_TRANSMOG)
			{
				final int transmogId = item.getTransmogId();
				if (transmogId > 0)
				{
					return transmogId;
				}
			}
			
			return item.getDisplayId();
		}
		
		return 0;
	}
	
	public int getPaperdollAugmentationId(int slot)
	{
		final Item item = _paperdoll[slot];
		return ((item != null) && (item.getAugmentation() != null)) ? item.getAugmentation().getAugmentationId() : 0;
	}
	
	/**
	 * Returns the objectID associated to the item in the paperdoll slot
	 * @param slot : int pointing out the slot
	 * @return int designating the objectID
	 */
	public int getPaperdollObjectId(int slot)
	{
		final Item item = _paperdoll[slot];
		return (item != null) ? item.getObjectId() : 0;
	}
	
	/**
	 * Adds new inventory's paperdoll listener.
	 * @param listener the new listener
	 */
	public synchronized void addPaperdollListener(PaperdollListener listener)
	{
		if (!_paperdollListeners.contains(listener))
		{
			_paperdollListeners.add(listener);
		}
	}
	
	/**
	 * Removes a paperdoll listener.
	 * @param listener the listener to be deleted
	 */
	public synchronized void removePaperdollListener(PaperdollListener listener)
	{
		_paperdollListeners.remove(listener);
	}
	
	/**
	 * Equips an item in the given slot of the paperdoll.<br>
	 * <u><i>Remark :</i></u> The item <b>must be</b> in the inventory already.
	 * @param slot : int pointing out the slot of the paperdoll
	 * @param item : Item pointing out the item to add in slot
	 * @return Item designating the item placed in the slot before
	 */
	public synchronized Item setPaperdollItem(int slot, Item item)
	{
		final Item old = _paperdoll[slot];
		if (old != item)
		{
			if (old != null)
			{
				_paperdoll[slot] = null;
				
				// Put old item from paperdoll slot to base location
				old.setItemLocation(getBaseLocation());
				old.setLastChange(Item.MODIFIED);
				
				// Get the mask for paperdoll
				int mask = 0;
				for (int i = 0; i < PAPERDOLL_TOTALSLOTS; i++)
				{
					final Item pi = _paperdoll[i];
					if (pi != null)
					{
						mask |= pi.getTemplate().getItemMask();
					}
				}
				
				_wearedMask = mask;
				
				// Notify all paperdoll listener in order to unequip old item in slot
				for (PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
					{
						continue;
					}
					
					listener.notifyUnequiped(slot, old, this);
				}
				
				old.updateDatabase();
			}
			
			// Add new item in slot of paperdoll
			if (item != null)
			{
				_paperdoll[slot] = item;
				item.setItemLocation(getEquipLocation(), slot);
				item.setLastChange(Item.MODIFIED);
				_wearedMask |= item.getTemplate().getItemMask();
				for (PaperdollListener listener : _paperdollListeners)
				{
					if (listener == null)
					{
						continue;
					}
					
					listener.notifyEquiped(slot, item, this);
				}
				
				item.updateDatabase();
			}
		}
		
		// Notify to scripts
		if (old != null)
		{
			final Creature owner = getOwner();
			if ((owner != null) && owner.isPlayer() && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_ITEM_UNEQUIP, old.getTemplate()))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemUnequip(owner.asPlayer(), old), old.getTemplate());
			}
		}
		
		return old;
	}
	
	/**
	 * @return the mask of wore item
	 */
	public int getWearedMask()
	{
		return _wearedMask;
	}
	
	/**
	 * Unequips item in body slot and returns alterations.<br>
	 * <b>If you do not need return value use {@link Inventory#unEquipItemInBodySlot(BodyPart)} instead</b>
	 * @param bodyPart : the body part enum
	 * @return List<Item> : List of changes
	 */
	public List<Item> unEquipItemInBodySlotAndRecord(BodyPart bodyPart)
	{
		final ChangeRecorder recorder = newRecorder();
		try
		{
			unEquipItemInBodySlot(bodyPart);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		
		return recorder.getChangedItems();
	}
	
	/**
	 * Sets item in slot of the paperdoll to null value
	 * @param pdollSlot : int designating the slot
	 * @return Item designating the item in slot before change
	 */
	public Item unEquipItemInSlot(int pdollSlot)
	{
		return setPaperdollItem(pdollSlot, null);
	}
	
	/**
	 * Unequips item in slot and returns alterations<br>
	 * <b>If you do not need return value use {@link Inventory#unEquipItemInSlot(int)} instead</b>
	 * @param slot : int designating the slot
	 * @return Collection<Item> : Collection of items altered
	 */
	public Collection<Item> unEquipItemInSlotAndRecord(int slot)
	{
		final ChangeRecorder recorder = newRecorder();
		try
		{
			unEquipItemInSlot(slot);
			if (getOwner().isPlayer())
			{
				getOwner().asPlayer().refreshExpertisePenalty();
			}
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		
		return recorder.getChangedItems();
	}
	
	/**
	 * Unequips item in body slot
	 * @param bodyPart the body part enum
	 * @return {@link Item} designating the item placed in the slot
	 */
	public Item unEquipItemInBodySlot(BodyPart bodyPart)
	{
		// Special handling for HAIRALL.
		if (bodyPart == BodyPart.HAIRALL)
		{
			setPaperdollItem(PAPERDOLL_HAIR, null);
		}
		
		final int pdollSlot = BodyPart.getPaperdollIndex(bodyPart);
		if (pdollSlot >= 0)
		{
			final Item old = setPaperdollItem(pdollSlot, null);
			if ((old != null) && getOwner().isPlayer())
			{
				getOwner().asPlayer().refreshExpertisePenalty();
			}
			
			return old;
		}
		
		return null;
	}
	
	/**
	 * Equips item and returns list of alterations<br>
	 * <b>If you don't need return value use {@link Inventory#equipItem(Item)} instead</b>
	 * @param item : Item corresponding to the item
	 * @return Collection<Item> : Collection of alterations
	 */
	public Collection<Item> equipItemAndRecord(Item item)
	{
		final ChangeRecorder recorder = newRecorder();
		try
		{
			equipItem(item);
		}
		finally
		{
			removePaperdollListener(recorder);
		}
		
		return recorder.getChangedItems();
	}
	
	/**
	 * Equips item in slot of paperdoll.
	 * @param item : Item designating the item and slot used.
	 */
	public void equipItem(Item item)
	{
		final BodyPart bodyPart = item.getTemplate().getBodyPart();
		
		if (getOwner().isPlayer())
		{
			final Player player = getOwner().asPlayer();
			if (player.isInStoreMode())
			{
				return;
			}
			
			if (!player.isGM() && !player.isHero() && item.isHeroItem())
			{
				return;
			}
			
			if (player.isWearingFormalWear())
			{
				// Only chest target can pass this.
				switch (bodyPart)
				{
					case BodyPart.LR_HAND:
					case BodyPart.L_HAND:
					case BodyPart.R_HAND:
					{
						// Unequip formal wear when equipping weapon/shield.
						unEquipItemInBodySlotAndRecord(BodyPart.ALLDRESS);
						break;
					}
					case BodyPart.LEGS:
					case BodyPart.FEET:
					case BodyPart.GLOVES:
					case BodyPart.HEAD:
					{
						return;
					}
				}
			}
		}
		
		// Handle special cases.
		switch (bodyPart)
		{
			case LR_HAND:
			{
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L_HAND:
			{
				final Item rh = getPaperdollItem(PAPERDOLL_RHAND);
				if ((rh != null) && (rh.getTemplate().getBodyPart() == BodyPart.LR_HAND) && !(((rh.getItemType() == WeaponType.BOW) && (item.getItemType() == EtcItemType.ARROW)) || ((rh.getItemType() == WeaponType.FISHINGROD) && (item.getItemType() == EtcItemType.LURE))))
				{
					setPaperdollItem(PAPERDOLL_RHAND, null);
				}
				
				setPaperdollItem(PAPERDOLL_LHAND, item);
				break;
			}
			case R_HAND:
			{
				// Don't care about arrows, listener will unequip them (hopefully).
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L_EAR:
			case R_EAR:
			case LR_EAR:
			{
				if (_paperdoll[PAPERDOLL_LEAR] == null)
				{
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				else if (_paperdoll[PAPERDOLL_REAR] == null)
				{
					setPaperdollItem(PAPERDOLL_REAR, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				break;
			}
			case L_FINGER:
			case R_FINGER:
			case LR_FINGER:
			{
				if (_paperdoll[PAPERDOLL_LFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				else if (_paperdoll[PAPERDOLL_RFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				break;
			}
			case NECK:
			{
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			}
			case FULL_ARMOR:
			{
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			}
			case CHEST:
			{
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			}
			case LEGS:
			{
				final Item chest = getPaperdollItem(PAPERDOLL_CHEST);
				if ((chest != null) && (chest.getTemplate().getBodyPart() == BodyPart.FULL_ARMOR))
				{
					setPaperdollItem(PAPERDOLL_CHEST, null);
				}
				
				setPaperdollItem(PAPERDOLL_LEGS, item);
				break;
			}
			case FEET:
			{
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			}
			case GLOVES:
			{
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			}
			case HEAD:
			{
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			}
			case HAIR:
			{
				final Item hair = getPaperdollItem(PAPERDOLL_HAIR);
				if ((hair != null) && (hair.getTemplate().getBodyPart() == BodyPart.HAIRALL))
				{
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
				}
				
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			}
			case HAIR2:
			{
				final Item hair2 = getPaperdollItem(PAPERDOLL_HAIR);
				if ((hair2 != null) && (hair2.getTemplate().getBodyPart() == BodyPart.HAIRALL))
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR2, null);
				}
				
				setPaperdollItem(PAPERDOLL_HAIR2, item);
				break;
			}
			case HAIRALL:
			{
				setPaperdollItem(PAPERDOLL_HAIR2, null);
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			}
			case UNDERWEAR:
			{
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			}
			case BACK:
			{
				setPaperdollItem(PAPERDOLL_CLOAK, item);
				break;
			}
			case ALLDRESS:
			{
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);
				setPaperdollItem(PAPERDOLL_HEAD, null);
				setPaperdollItem(PAPERDOLL_FEET, null);
				setPaperdollItem(PAPERDOLL_GLOVES, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			}
			default:
			{
				LOGGER.warning("Unknown body slot " + bodyPart + " for Item ID: " + item.getId());
				break;
			}
		}
	}
	
	/**
	 * Refresh the weight of equipment loaded
	 */
	@Override
	protected void refreshWeight()
	{
		long weight = 0;
		for (Item item : _items)
		{
			if ((item != null) && (item.getTemplate() != null))
			{
				weight += item.getTemplate().getWeight() * item.getCount();
			}
		}
		
		_totalWeight = (int) Math.min(weight, Integer.MAX_VALUE);
	}
	
	/**
	 * @return the totalWeight.
	 */
	public int getTotalWeight()
	{
		return _totalWeight;
	}
	
	/**
	 * Return the Item of the arrows needed for this bow.
	 * @param bow : Item designating the bow
	 * @return Item pointing out arrows for bow
	 */
	public Item findArrowForBow(ItemTemplate bow)
	{
		if (bow == null)
		{
			return null;
		}
		
		Item arrow = null;
		for (Item item : _items)
		{
			if (item.isEtcItem() && (item.getTemplate().getCrystalTypePlus() == bow.getCrystalTypePlus()) && (item.getEtcItem().getItemType() == EtcItemType.ARROW))
			{
				arrow = item;
				break;
			}
		}
		
		// Get the Item corresponding to the item identifier and return it
		return arrow;
	}
	
	/**
	 * Get back items in inventory from database
	 */
	@Override
	public void restore()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id, item_id, count, enchant_level, loc, loc_data, custom_type1, custom_type2, mana_left, time FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data"))
		{
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
			statement.setString(3, getEquipLocation().name());
			try (ResultSet inv = statement.executeQuery())
			{
				Item item;
				while (inv.next())
				{
					item = Item.restoreFromDb(getOwnerId(), inv);
					if (item == null)
					{
						continue;
					}
					
					if (getOwner().isPlayer())
					{
						final Player player = getOwner().asPlayer();
						if (!player.isGM() && !player.isHero() && item.isHeroItem())
						{
							item.setItemLocation(ItemLocation.INVENTORY);
						}
					}
					
					World.getInstance().addObject(item);
					
					// If stackable item is found in inventory just add to current quantity
					if (item.isStackable() && (getItemByItemId(item.getId()) != null))
					{
						addItem(ItemProcessType.RESTORE, item, getOwner().asPlayer(), null);
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
			LOGGER.log(Level.WARNING, "Could not restore inventory: " + e.getMessage(), e);
		}
	}
	
	public boolean canEquipCloak()
	{
		return getOwner().asPlayer().getStat().canEquipCloak();
	}
	
	/**
	 * Re-notify to paperdoll listeners every equipped item.<br>
	 * Only used by player ClassId set methods.
	 */
	public void reloadEquippedItems()
	{
		int slot;
		for (Item item : _paperdoll)
		{
			if (item == null)
			{
				continue;
			}
			
			slot = item.getLocationSlot();
			for (PaperdollListener listener : _paperdollListeners)
			{
				if (listener == null)
				{
					continue;
				}
				
				listener.notifyUnequiped(slot, item, this);
				listener.notifyEquiped(slot, item, this);
			}
		}
	}
	
	/**
	 * Gets the items in paperdoll slots filtered by filter.
	 * @param filters multiple filters
	 * @return the filtered items in inventory
	 */
	@SafeVarargs
	public final Collection<Item> getPaperdollItems(Predicate<Item>... filters)
	{
		Predicate<Item> filter = Objects::nonNull;
		for (Predicate<Item> additionalFilter : filters)
		{
			filter = filter.and(additionalFilter);
		}
		
		final List<Item> items = new LinkedList<>();
		for (Item item : _paperdoll)
		{
			if (filter.test(item))
			{
				items.add(item);
			}
		}
		
		return items;
	}
}
