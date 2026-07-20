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

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncArmorSet extends AbstractFunction
{
	private static final Map<Stat, FuncArmorSet> _fh_instance = new EnumMap<>(Stat.class);
	
	public static AbstractFunction getInstance(Stat st)
	{
		if (!_fh_instance.containsKey(st))
		{
			_fh_instance.put(st, new FuncArmorSet(st));
		}
		
		return _fh_instance.get(st);
	}
	
	private FuncArmorSet(Stat stat)
	{
		super(stat, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		
		// Should not apply armor set bonus to summons.
		if (effector.isPlayer())
		{
			final Player player = effector.asPlayer();
			final Item chest = player.getChestArmorInstance();
			if (chest != null)
			{
				final ArmorSet set = ArmorSetData.getInstance().getSet(chest.getId());
				if ((set != null) && set.containAll(player))
				{
					switch (getStat())
					{
						case STAT_STR:
						{
							value += set.getSTR();
							break;
						}
						case STAT_DEX:
						{
							value += set.getDEX();
							break;
						}
						case STAT_INT:
						{
							value += set.getINT();
							break;
						}
						case STAT_MEN:
						{
							value += set.getMEN();
							break;
						}
						case STAT_CON:
						{
							value += set.getCON();
							break;
						}
						case STAT_WIT:
						{
							value += set.getWIT();
							break;
						}
					}
				}
			}
		}
		
		return value;
	}
}
