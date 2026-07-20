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
package org.l2jmobius.gameserver.model.conditions;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.BaseStat;

/**
 * The Class ConditionPlayerBaseStats.
 * @author mkizub
 */
public class ConditionPlayerBaseStats extends Condition
{
	private final BaseStat _stat;
	private final int _value;
	
	/**
	 * Instantiates a new condition player base stats.
	 * @param creature the player
	 * @param stat the stat
	 * @param value the value
	 */
	public ConditionPlayerBaseStats(Creature creature, BaseStat stat, int value)
	{
		super();
		_stat = stat;
		_value = value;
	}
	
	/**
	 * Test impl.
	 * @return true, if successful
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		switch (_stat)
		{
			case INT:
			{
				return player.getINT() >= _value;
			}
			case STR:
			{
				return player.getSTR() >= _value;
			}
			case CON:
			{
				return player.getCON() >= _value;
			}
			case DEX:
			{
				return player.getDEX() >= _value;
			}
			case MEN:
			{
				return player.getMEN() >= _value;
			}
			case WIT:
			{
				return player.getWIT() >= _value;
			}
		}
		
		return false;
	}
}
