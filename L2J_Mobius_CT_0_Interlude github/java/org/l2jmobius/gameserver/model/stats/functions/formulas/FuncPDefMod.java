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
package org.l2jmobius.gameserver.model.stats.functions.formulas;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncPDefMod extends AbstractFunction
{
	private static final FuncPDefMod _fmm_instance = new FuncPDefMod();
	
	public static AbstractFunction getInstance()
	{
		return _fmm_instance;
	}
	
	private FuncPDefMod()
	{
		super(Stat.POWER_DEFENCE, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		if (effector.isPlayer())
		{
			final Player p = effector.asPlayer();
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_CHEST))
			{
				value -= p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_CHEST);
			}
			
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_LEGS) || (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_CHEST) && (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR)))
			{
				value -= p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_LEGS);
			}
			
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_HEAD))
			{
				value -= p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_HEAD);
			}
			
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_FEET))
			{
				value -= p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_FEET);
			}
			
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_GLOVES))
			{
				value -= p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_GLOVES);
			}
			
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_UNDER))
			{
				value -= p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_UNDER);
			}
			
			if (!p.getInventory().isPaperdollSlotEmpty(Inventory.PAPERDOLL_CLOAK))
			{
				value -= p.getTemplate().getBaseDefBySlot(Inventory.PAPERDOLL_CLOAK);
			}
		}
		
		return value * effector.getLevelMod();
	}
}
