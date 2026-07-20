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
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.BaseStat;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncMAtkMod extends AbstractFunction
{
	private static final FuncMAtkMod _fma_instance = new FuncMAtkMod();
	
	public static AbstractFunction getInstance()
	{
		return _fma_instance;
	}
	
	private FuncMAtkMod()
	{
		super(Stat.MAGIC_ATTACK, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		// Level Modifier^2 * INT Modifier^2
		final double lvlMod = effector.isPlayer() ? BaseStat.INT.calcBonus(effector.asPlayer()) : BaseStat.INT.calcBonus(effector);
		final double intMod = effector.isPlayer() ? effector.asPlayer().getLevelMod() : effector.getLevelMod();
		return initVal * Math.pow(lvlMod, 2) * Math.pow(intMod, 2);
	}
}
