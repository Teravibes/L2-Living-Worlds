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
package org.l2jmobius.gameserver.model.multisell;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author DS
 */
public class Ingredient
{
	private int _itemId;
	private int _itemCount;
	private final int _enchantmentLevel;
	private boolean _isTaxIngredient;
	private boolean _maintainIngredient;
	private ItemTemplate _template = null;
	private ItemInfo _itemInfo = null;
	
	public Ingredient(StatSet set)
	{
		this(set.getInt("id"), set.getInt("count"), set.getInt("enchantmentLevel", 0), set.getBoolean("isTaxIngredient", false), set.getBoolean("maintainIngredient", false));
	}
	
	public Ingredient(int itemId, int itemCount, int enchantmentLevel, boolean isTaxIngredient, boolean maintainIngredient)
	{
		_itemId = itemId;
		_itemCount = itemCount;
		_enchantmentLevel = enchantmentLevel;
		_isTaxIngredient = isTaxIngredient;
		_maintainIngredient = maintainIngredient;
		if (_itemId > 0)
		{
			_template = ItemData.getInstance().getTemplate(_itemId);
		}
	}
	
	/**
	 * @return a new Ingredient instance with the same values as this.
	 */
	public Ingredient getCopy()
	{
		return new Ingredient(_itemId, _itemCount, _enchantmentLevel, _isTaxIngredient, _maintainIngredient);
	}
	
	public ItemTemplate getTemplate()
	{
		return _template;
	}
	
	public void setItemInfo(Item item)
	{
		_itemInfo = new ItemInfo(item);
	}
	
	public void setItemInfo(ItemInfo info)
	{
		_itemInfo = info;
	}
	
	public ItemInfo getItemInfo()
	{
		return _itemInfo;
	}
	
	public int getEnchantLevel()
	{
		return _itemInfo == null ? _enchantmentLevel : _itemInfo.getEnchantLevel();
	}
	
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public void setItemCount(int itemCount)
	{
		_itemCount = itemCount;
	}
	
	public int getItemCount()
	{
		return _itemCount;
	}
	
	public void setTaxIngredient(boolean isTaxIngredient)
	{
		_isTaxIngredient = isTaxIngredient;
	}
	
	public boolean isTaxIngredient()
	{
		return _isTaxIngredient;
	}
	
	public void setMaintainIngredient(boolean maintainIngredient)
	{
		_maintainIngredient = maintainIngredient;
	}
	
	public boolean getMaintainIngredient()
	{
		return _maintainIngredient;
	}
	
	public boolean isStackable()
	{
		return _template == null ? true : _template.isStackable();
	}
	
	public boolean isArmorOrWeapon()
	{
		return _template == null ? false : (_template instanceof Armor) || (_template instanceof Weapon);
	}
	
	public int getWeight()
	{
		return _template == null ? 0 : _template.getWeight();
	}
}
