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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.WarehouseItem;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.recipe.RecipeList;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.item.type.MaterialType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.WarehouseListType;

public class SortedWareHouseWithdrawalList extends ServerPacket
{
	public static final int PRIVATE = 1;
	public static final int CLAN = 2;
	public static final int CASTLE = 3; // not sure
	public static final int FREIGHT = 4; // not sure
	
	/** sort order A..Z */
	public static final byte A2Z = 1;
	/** sort order Z..A */
	public static final byte Z2A = -1;
	/** sort order Grade non..S */
	public static final byte GRADE = 2;
	/** sort order Recipe Level 1..9 */
	public static final byte LEVEL = 3;
	/** sort order type */
	public static final byte TYPE = 4;
	/** sort order body part (wearing) */
	public static final byte WEAR = 5;
	/** Maximum Items to put into list */
	public static final int MAX_SORT_LIST_ITEMS = 300;
	
	private int _playerAdena;
	private List<WarehouseItem> _objects = new ArrayList<>();
	private int _whType;
	
	/**
	 * This will instantiate the Warehouselist the Player asked for
	 * @param player who calls for the itemlist
	 * @param type is the Warehouse Type
	 * @param itemtype is the Itemtype to sort for
	 * @param sortorder is the integer Sortorder like 1 for A..Z (use public constant)
	 */
	public SortedWareHouseWithdrawalList(Player player, int type, WarehouseListType itemtype, byte sortorder)
	{
		_whType = type;
		_playerAdena = player.getAdena();
		if (player.getActiveWarehouse() == null)
		{
			// Something went wrong!
			PacketLogger.warning("Error while sending withdraw request to: " + player.getName());
			return;
		}
		
		switch (itemtype)
		{
			case WEAPON:
			{
				_objects = createWeaponList(player.getActiveWarehouse().getItems());
				break;
			}
			case ARMOR:
			{
				_objects = createArmorList(player.getActiveWarehouse().getItems());
				break;
			}
			case ETCITEM:
			{
				_objects = createEtcItemList(player.getActiveWarehouse().getItems());
				break;
			}
			case MATERIAL:
			{
				_objects = createMatList(player.getActiveWarehouse().getItems());
				break;
			}
			case RECIPE:
			{
				_objects = createRecipeList(player.getActiveWarehouse().getItems());
				break;
			}
			case AMULETT:
			{
				_objects = createAmulettList(player.getActiveWarehouse().getItems());
				break;
			}
			case SPELLBOOK:
			{
				_objects = createSpellbookList(player.getActiveWarehouse().getItems());
				break;
			}
			case CONSUMABLE:
			{
				_objects = createConsumableList(player.getActiveWarehouse().getItems());
				break;
			}
			case SHOT:
			{
				_objects = createShotList(player.getActiveWarehouse().getItems());
				break;
			}
			case SCROLL:
			{
				_objects = createScrollList(player.getActiveWarehouse().getItems());
				break;
			}
			case SEED:
			{
				_objects = createSeedList(player.getActiveWarehouse().getItems());
				break;
			}
			case OTHER:
			{
				_objects = createOtherList(player.getActiveWarehouse().getItems());
				break;
			}
			case ALL:
			default:
			{
				_objects = createAllList(player.getActiveWarehouse().getItems());
				break;
			}
		}
		
		try
		{
			switch (sortorder)
			{
				case A2Z:
				case Z2A:
				{
					Collections.sort(_objects, new WarehouseItemNameComparator(sortorder));
					break;
				}
				case GRADE:
				{
					if ((itemtype == WarehouseListType.ARMOR) || (itemtype == WarehouseListType.WEAPON))
					{
						Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
						Collections.sort(_objects, new WarehouseItemGradeComparator(A2Z));
					}
					break;
				}
				case LEVEL:
				{
					if (itemtype == WarehouseListType.RECIPE)
					{
						Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
						Collections.sort(_objects, new WarehouseItemRecipeComparator(A2Z));
					}
					break;
				}
				case TYPE:
				{
					if (itemtype == WarehouseListType.MATERIAL)
					{
						Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
						Collections.sort(_objects, new WarehouseItemTypeComparator(A2Z));
					}
					break;
				}
				case WEAR:
				{
					if (itemtype == WarehouseListType.ARMOR)
					{
						Collections.sort(_objects, new WarehouseItemNameComparator(A2Z));
						Collections.sort(_objects, new WarehouseItemBodypartComparator(A2Z));
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			// Ignore.
		}
	}
	
	/**
	 * This public method return the integer of the Sortorder by its name. If you want to have another, add the Comparator and the Constant.
	 * @param order
	 * @return the integer of the sortorder or 1 as default value
	 */
	public static byte getOrder(String order)
	{
		if (order == null)
		{
			return A2Z;
		}
		else if (order.startsWith("A2Z"))
		{
			return A2Z;
		}
		else if (order.startsWith("Z2A"))
		{
			return Z2A;
		}
		else if (order.startsWith("GRADE"))
		{
			return GRADE;
		}
		else if (order.startsWith("TYPE"))
		{
			return TYPE;
		}
		else if (order.startsWith("WEAR"))
		{
			return WEAR;
		}
		else
		{
			try
			{
				return Byte.parseByte(order);
			}
			catch (NumberFormatException ex)
			{
				return A2Z;
			}
		}
	}
	
	/**
	 * This is the common Comparator to sort the items by Name
	 */
	private static class WarehouseItemNameComparator implements Comparator<WarehouseItem>
	{
		private byte order = 0;
		
		protected WarehouseItemNameComparator(byte sortOrder)
		{
			order = sortOrder;
		}
		
		@Override
		public int compare(WarehouseItem o1, WarehouseItem o2)
		{
			if ((o1.getType2() == ItemTemplate.TYPE2_MONEY) && (o2.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? Z2A : A2Z);
			}
			
			if ((o2.getType2() == ItemTemplate.TYPE2_MONEY) && (o1.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? A2Z : Z2A);
			}
			
			final String s1 = o1.getItemName();
			final String s2 = o2.getItemName();
			return (order == A2Z ? s1.compareTo(s2) : s2.compareTo(s1));
		}
	}
	
	/**
	 * This Comparator is used to sort by Recipe Level
	 */
	private static class WarehouseItemRecipeComparator implements Comparator<WarehouseItem>
	{
		private int order = 0;
		private RecipeData rd = null;
		
		protected WarehouseItemRecipeComparator(int sortOrder)
		{
			order = sortOrder;
			rd = RecipeData.getInstance();
		}
		
		@Override
		public int compare(WarehouseItem o1, WarehouseItem o2)
		{
			if ((o1.getType2() == ItemTemplate.TYPE2_MONEY) && (o2.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? Z2A : A2Z);
			}
			
			if ((o2.getType2() == ItemTemplate.TYPE2_MONEY) && (o1.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? A2Z : Z2A);
			}
			
			if ((o1.isEtcItem() && (o1.getItemType() == EtcItemType.RECIPE)) && (o2.isEtcItem() && (o2.getItemType() == EtcItemType.RECIPE)))
			{
				try
				{
					final RecipeList rp1 = rd.getRecipeByItemId(o1.getItemId());
					final RecipeList rp2 = rd.getRecipeByItemId(o2.getItemId());
					if (rp1 == null)
					{
						return (order == A2Z ? A2Z : Z2A);
					}
					
					if (rp2 == null)
					{
						return (order == A2Z ? Z2A : A2Z);
					}
					
					final Integer i1 = rp1.getLevel();
					final Integer i2 = rp2.getLevel();
					return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
				}
				catch (Exception e)
				{
					return 0;
				}
			}
			
			final String s1 = o1.getItemName();
			final String s2 = o2.getItemName();
			return (order == A2Z ? s1.compareTo(s2) : s2.compareTo(s1));
		}
	}
	
	/**
	 * This Comparator is used to sort the Items by BodyPart
	 */
	private static class WarehouseItemBodypartComparator implements Comparator<WarehouseItem>
	{
		private byte order = 0;
		
		protected WarehouseItemBodypartComparator(byte sortOrder)
		{
			order = sortOrder;
		}
		
		@Override
		public int compare(WarehouseItem o1, WarehouseItem o2)
		{
			if ((o1.getType2() == ItemTemplate.TYPE2_MONEY) && (o2.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? Z2A : A2Z);
			}
			
			if ((o2.getType2() == ItemTemplate.TYPE2_MONEY) && (o1.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? A2Z : Z2A);
			}
			
			final Integer i1 = o1.getBodyPart().getMask();
			final Integer i2 = o2.getBodyPart().getMask();
			return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
		}
	}
	
	/**
	 * This Comparator is used to sort by the Item Grade (e.g. Non..S-Grade)
	 */
	private static class WarehouseItemGradeComparator implements Comparator<WarehouseItem>
	{
		byte order = 0;
		
		protected WarehouseItemGradeComparator(byte sortOrder)
		{
			order = sortOrder;
		}
		
		@Override
		public int compare(WarehouseItem o1, WarehouseItem o2)
		{
			if ((o1.getType2() == ItemTemplate.TYPE2_MONEY) && (o2.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? Z2A : A2Z);
			}
			
			if ((o2.getType2() == ItemTemplate.TYPE2_MONEY) && (o1.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? A2Z : Z2A);
			}
			
			final CrystalType i1 = o1.getItemGrade();
			final CrystalType i2 = o2.getItemGrade();
			return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
		}
	}
	
	/**
	 * This Comparator will sort by Item Type. Unfortunatly this will only have a good result if the Database Table for the ETCITEM.TYPE column is fixed!
	 */
	private static class WarehouseItemTypeComparator implements Comparator<WarehouseItem>
	{
		byte order = 0;
		
		protected WarehouseItemTypeComparator(byte sortOrder)
		{
			order = sortOrder;
		}
		
		@Override
		public int compare(WarehouseItem o1, WarehouseItem o2)
		{
			if ((o1.getType2() == ItemTemplate.TYPE2_MONEY) && (o2.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? Z2A : A2Z);
			}
			
			if ((o2.getType2() == ItemTemplate.TYPE2_MONEY) && (o1.getType2() != ItemTemplate.TYPE2_MONEY))
			{
				return (order == A2Z ? A2Z : Z2A);
			}
			
			try
			{
				final MaterialType i1 = o1.getItem().getMaterialType();
				final MaterialType i2 = o2.getItem().getMaterialType();
				return (order == A2Z ? i1.compareTo(i2) : i2.compareTo(i1));
			}
			catch (Exception e)
			{
				return 0;
			}
		}
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Weapon</li>
	 * <li>Arrow</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createWeaponList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if ((item.isWeapon() || (item.getTemplate().getType2() == ItemTemplate.TYPE2_WEAPON) || (item.isEtcItem() && (item.getItemType() == EtcItemType.ARROW)) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Armor</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createArmorList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if ((item.isArmor() || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Everything which is no Weapon/Armor</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createEtcItemList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if ((item.isEtcItem() || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Materials</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createMatList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.MATERIAL)) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Recipes</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createRecipeList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.RECIPE)) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Amulett</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createAmulettList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && (item.getItemName().toUpperCase().startsWith("AMULET"))) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Spellbook & Dwarven Drafts</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createSpellbookList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && (!item.getItemName().toUpperCase().startsWith("AMULET"))) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Consumables (Potions, Shots, ...)</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createConsumableList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && ((item.getEtcItem().getItemType() == EtcItemType.SCROLL) || (item.getEtcItem().getItemType() == EtcItemType.SHOT))) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Shots</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createShotList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.SHOT)) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Scrolls/Potions</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createScrollList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.SCROLL)) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Seeds</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createSeedList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && (item.getEtcItem().getItemType() == EtcItemType.SEED)) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>Everything which is no Weapon/Armor, Material, Recipe, Spellbook, Scroll or Shot</li>
	 * <li>Money</li><br>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createOtherList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (((item.isEtcItem() && ((item.getEtcItem().getItemType() != EtcItemType.MATERIAL) && (item.getEtcItem().getItemType() != EtcItemType.RECIPE) && (item.getEtcItem().getItemType() != EtcItemType.SCROLL) && (item.getEtcItem().getItemType() != EtcItemType.SHOT))) || (item.getTemplate().getType2() == ItemTemplate.TYPE2_MONEY)) && (list.size() < MAX_SORT_LIST_ITEMS))
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	/**
	 * This method is used to limit the given Warehouse List to:
	 * <li>no limit</li> This may sound strange but we return the given Array as a List<L2WarehouseItem>
	 * @param items complete Warehouse List
	 * @return limited Item List
	 */
	private List<WarehouseItem> createAllList(Collection<Item> items)
	{
		final List<WarehouseItem> list = new ArrayList<>();
		for (Item item : items)
		{
			if (list.size() < MAX_SORT_LIST_ITEMS)
			{
				list.add(new WarehouseItem(item));
			}
		}
		
		return list;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.WAREHOUSE_WITHDRAW_LIST.writeId(this, buffer);
		/*
		 * 0x01-Private Warehouse 0x02-Clan Warehouse 0x03-Castle Warehouse 0x04-Warehouse
		 */
		buffer.writeShort(_whType);
		buffer.writeInt(_playerAdena);
		buffer.writeShort(_objects.size());
		for (WarehouseItem item : _objects)
		{
			buffer.writeShort(item.getItem().getType1());
			buffer.writeInt(item.getObjectId());
			buffer.writeInt(item.getItemId());
			buffer.writeInt((int) item.getCount());
			buffer.writeShort(item.getItem().getType2());
			buffer.writeShort(item.getCustomType1());
			buffer.writeInt(item.getItem().getBodyPart().getMask());
			buffer.writeShort(item.getEnchantLevel());
			buffer.writeShort(0);
			buffer.writeShort(item.getCustomType2());
			buffer.writeInt(item.getObjectId());
			if (item.isAugmented())
			{
				buffer.writeInt(0x0000FFFF & item.getAugmentationId());
				buffer.writeInt(item.getAugmentationId() >> 16);
			}
			else
			{
				buffer.writeLong(0);
			}
		}
	}
}
