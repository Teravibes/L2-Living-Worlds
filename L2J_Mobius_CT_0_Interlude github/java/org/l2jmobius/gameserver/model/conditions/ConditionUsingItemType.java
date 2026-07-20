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
package org.l2jmobius.gameserver.model.conditions;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * The Class ConditionUsingItemType.
 * @author mkizub, naker
 */
public class ConditionUsingItemType extends Condition
{
	private final boolean _armor;
	private final int _mask;
	
	/**
	 * Instantiates a new condition using item type.
	 * @param mask the mask
	 */
	public ConditionUsingItemType(int mask)
	{
		_mask = mask;
		_armor = (_mask & (ArmorType.MAGIC.mask() | ArmorType.LIGHT.mask() | ArmorType.HEAVY.mask())) != 0;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effector == null)
		{
			return false;
		}
		
		if (!effector.isPlayer())
		{
			return !_armor && ((_mask & effector.getAttackType().mask()) != 0);
		}
		
		final Inventory inv = effector.getInventory();
		if (_armor)
		{
			final Item chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			final Item legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
			if ((chest == null) && (legs == null))
			{
				return (ArmorType.NONE.mask() & _mask) == ArmorType.NONE.mask();
			}
			
			if (chest != null)
			{
				if (legs == null)
				{
					final BodyPart chestBodyPart = chest.getTemplate().getBodyPart();
					if (chestBodyPart == BodyPart.FULL_ARMOR)
					{
						final int chestMask = chest.getTemplate().getItemMask();
						return (_mask & chestMask) != 0;
					}
					
					return (ArmorType.NONE.mask() & _mask) == ArmorType.NONE.mask();
				}
				
				final int chestMask = chest.getTemplate().getItemMask();
				final int legMask = legs.getTemplate().getItemMask();
				if (chestMask == legMask)
				{
					return (_mask & chestMask) != 0;
				}
				
				return (ArmorType.NONE.mask() & _mask) == ArmorType.NONE.mask();
			}
		}
		
		return (_mask & inv.getWearedMask()) != 0;
	}
}
