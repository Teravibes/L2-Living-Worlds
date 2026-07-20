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
package org.l2jmobius.gameserver.model.item.enchant;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;

/**
 * @author UnAfraid
 */
public abstract class AbstractEnchantItem
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractEnchantItem.class.getName());
	
	private static final Set<EtcItemType> ENCHANT_TYPES = EnumSet.noneOf(EtcItemType.class);
	static
	{
		ENCHANT_TYPES.add(EtcItemType.ANCIENT_CRYSTAL_ENCHANT_AM);
		ENCHANT_TYPES.add(EtcItemType.ANCIENT_CRYSTAL_ENCHANT_WP);
		ENCHANT_TYPES.add(EtcItemType.BLESS_SCRL_ENCHANT_AM);
		ENCHANT_TYPES.add(EtcItemType.BLESS_SCRL_ENCHANT_WP);
		ENCHANT_TYPES.add(EtcItemType.SCRL_ENCHANT_AM);
		ENCHANT_TYPES.add(EtcItemType.SCRL_ENCHANT_WP);
		ENCHANT_TYPES.add(EtcItemType.SCRL_INC_ENCHANT_PROP_AM);
		ENCHANT_TYPES.add(EtcItemType.SCRL_INC_ENCHANT_PROP_WP);
	}
	
	private final int _id;
	private final CrystalType _grade;
	private final int _maxEnchantLevel;
	private final double _bonusRate;
	
	public AbstractEnchantItem(StatSet set)
	{
		_id = set.getInt("id");
		if (getItem() == null)
		{
			throw new NullPointerException();
		}
		else if (!ENCHANT_TYPES.contains(getItem().getItemType()))
		{
			throw new IllegalAccessError();
		}
		
		_grade = set.getEnum("targetGrade", CrystalType.class, CrystalType.NONE);
		_maxEnchantLevel = set.getInt("maxEnchant", 65535);
		_bonusRate = set.getDouble("bonusRate", 0);
	}
	
	/**
	 * @return id of current item
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return bonus chance that would be added
	 */
	public double getBonusRate()
	{
		return _bonusRate;
	}
	
	/**
	 * @return {@link ItemTemplate} current item/scroll
	 */
	public ItemTemplate getItem()
	{
		return ItemData.getInstance().getTemplate(_id);
	}
	
	/**
	 * @return grade of the item/scroll.
	 */
	public CrystalType getGrade()
	{
		return _grade;
	}
	
	/**
	 * @return {@code true} if scroll is for weapon, {@code false} for armor
	 */
	public abstract boolean isWeapon();
	
	/**
	 * @return the maximum enchant level that this scroll/item can be used with
	 */
	public int getMaxEnchantLevel()
	{
		return _maxEnchantLevel;
	}
	
	/**
	 * @param itemToEnchant the item to be enchanted
	 * @return {@code true} if this support item can be used with the item to be enchanted, {@code false} otherwise
	 */
	public boolean isValid(Item itemToEnchant)
	{
		if (itemToEnchant == null)
		{
			return false;
		}
		else if (!itemToEnchant.isEnchantable())
		{
			return false;
		}
		else if (!isValidItemType(itemToEnchant.getTemplate().getType2()))
		{
			return false;
		}
		else if ((_maxEnchantLevel != 0) && (itemToEnchant.getEnchantLevel() >= _maxEnchantLevel))
		{
			return false;
		}
		else if (_grade != itemToEnchant.getTemplate().getCrystalTypePlus())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * @param type2
	 * @return {@code true} if current type2 is valid to be enchanted, {@code false} otherwise
	 */
	private boolean isValidItemType(int type2)
	{
		if (type2 == ItemTemplate.TYPE2_WEAPON)
		{
			return isWeapon();
		}
		else if ((type2 == ItemTemplate.TYPE2_SHIELD_ARMOR) || (type2 == ItemTemplate.TYPE2_ACCESSORY))
		{
			return !isWeapon();
		}
		
		return false;
	}
}
