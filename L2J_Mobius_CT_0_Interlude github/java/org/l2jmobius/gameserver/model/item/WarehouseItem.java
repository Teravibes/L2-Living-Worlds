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
package org.l2jmobius.gameserver.model.item;

import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.model.item.type.ItemType;

/**
 * This class contains Item<br>
 * Use to sort Item of :
 * <ul>
 * <li>Armor</li>
 * <li>EtcItem</li>
 * <li>Weapon</li>
 * </ul>
 * @version $Revision: 1.7.2.2.2.5 $ $Date: 2005/04/06 18:25:18 $
 */
public class WarehouseItem
{
	private final ItemTemplate _item;
	private final int _object;
	private final long _count;
	private final int _owner;
	private final int _locationSlot;
	private final int _enchant;
	private final CrystalType _grade;
	private boolean _isAugmented;
	private int _augmentationId;
	private final int _customType1;
	private final int _customType2;
	private final int _mana;
	private final int _time;
	
	public WarehouseItem(Item item)
	{
		_item = item.getTemplate();
		_object = item.getObjectId();
		_count = item.getCount();
		_owner = item.getOwnerId();
		_locationSlot = item.getLocationSlot();
		_enchant = item.getEnchantLevel();
		_customType1 = item.getCustomType1();
		_customType2 = item.getCustomType2();
		_grade = item.getTemplate().getCrystalType();
		if (item.isAugmented())
		{
			_isAugmented = true;
			_augmentationId = item.getAugmentation().getAugmentationId();
		}
		else
		{
			_isAugmented = false;
		}
		
		_mana = item.getMana();
		_time = item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -1;
	}
	
	/**
	 * @return the item.
	 */
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	/**
	 * @return the unique objectId.
	 */
	public int getObjectId()
	{
		return _object;
	}
	
	/**
	 * @return the owner.
	 */
	public int getOwnerId()
	{
		return _owner;
	}
	
	/**
	 * @return the location slot.
	 */
	public int getLocationSlot()
	{
		return _locationSlot;
	}
	
	/**
	 * @return the count.
	 */
	public long getCount()
	{
		return _count;
	}
	
	/**
	 * @return the first type.
	 */
	public int getType1()
	{
		return _item.getType1();
	}
	
	/**
	 * @return the second type.
	 */
	public int getType2()
	{
		return _item.getType2();
	}
	
	/**
	 * @return the second type.
	 */
	public ItemType getItemType()
	{
		return _item.getItemType();
	}
	
	/**
	 * @return the ItemId.
	 */
	public int getItemId()
	{
		return _item.getId();
	}
	
	/**
	 * @return the part of body used with this item.
	 */
	public BodyPart getBodyPart()
	{
		return _item.getBodyPart();
	}
	
	/**
	 * @return the enchant level.
	 */
	public int getEnchantLevel()
	{
		return _enchant;
	}
	
	/**
	 * @return the item grade
	 */
	public CrystalType getItemGrade()
	{
		return _grade;
	}
	
	/**
	 * @return {@code true} if the item is a weapon, {@code false} otherwise.
	 */
	public boolean isWeapon()
	{
		return _item instanceof Weapon;
	}
	
	/**
	 * @return {@code true} if the item is an armor, {@code false} otherwise.
	 */
	public boolean isArmor()
	{
		return _item instanceof Armor;
	}
	
	/**
	 * @return {@code true} if the item is an etc item, {@code false} otherwise.
	 */
	public boolean isEtcItem()
	{
		return _item instanceof EtcItem;
	}
	
	/**
	 * @return the name of the item
	 */
	public String getItemName()
	{
		return _item.getName();
	}
	
	/**
	 * @return {@code true} if the item is augmented, {@code false} otherwise.
	 */
	public boolean isAugmented()
	{
		return _isAugmented;
	}
	
	/**
	 * @return the augmentation If.
	 */
	public int getAugmentationId()
	{
		return _augmentationId;
	}
	
	/**
	 * @return the name of the item
	 */
	public String getName()
	{
		return _item.getName();
	}
	
	public int getCustomType1()
	{
		return _customType1;
	}
	
	public int getCustomType2()
	{
		return _customType2;
	}
	
	public int getMana()
	{
		return _mana;
	}
	
	public int getTime()
	{
		return _time;
	}
	
	/**
	 * @return the name of the item
	 */
	@Override
	public String toString()
	{
		return _item.toString();
	}
}
