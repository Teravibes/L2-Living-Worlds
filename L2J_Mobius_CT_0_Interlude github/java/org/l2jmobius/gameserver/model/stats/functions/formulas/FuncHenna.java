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

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncHenna extends AbstractFunction
{
	private static final Map<Stat, FuncHenna> _fh_instance = new EnumMap<>(Stat.class);
	
	public static AbstractFunction getInstance(Stat st)
	{
		if (!_fh_instance.containsKey(st))
		{
			_fh_instance.put(st, new FuncHenna(st));
		}
		
		return _fh_instance.get(st);
	}
	
	private FuncHenna(Stat stat)
	{
		super(stat, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		
		// Should not apply henna bonus to summons.
		if (effector.isPlayer())
		{
			final Player pc = effector.asPlayer();
			switch (getStat())
			{
				case STAT_STR:
				{
					value += pc.getHennaStatSTR();
					break;
				}
				case STAT_CON:
				{
					value += pc.getHennaStatCON();
					break;
				}
				case STAT_DEX:
				{
					value += pc.getHennaStatDEX();
					break;
				}
				case STAT_INT:
				{
					value += pc.getHennaStatINT();
					break;
				}
				case STAT_WIT:
				{
					value += pc.getHennaStatWIT();
					break;
				}
				case STAT_MEN:
				{
					value += pc.getHennaStatMEN();
					break;
				}
			}
		}
		
		return value;
	}
}
