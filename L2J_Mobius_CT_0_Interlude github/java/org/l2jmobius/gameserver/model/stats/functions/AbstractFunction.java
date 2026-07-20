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
package org.l2jmobius.gameserver.model.stats.functions;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * A Function object is a component of a Calculator created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).<br>
 * In fact, each calculator is a table of functions object in which each function represents a mathematics function:<br>
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<br>
 * When the calc method of a calculator is launched, each mathematics function is called according to its priority <b>_order</b>.<br>
 * Indeed, functions with lowest priority order is executed first and functions with the same order are executed in unspecified order.<br>
 * @author Zoey76
 */
public abstract class AbstractFunction
{
	/** Logger. */
	protected static final Logger LOG = Logger.getLogger(AbstractFunction.class.getName());
	/** Statistics, that is affected by this function (See Creature.CALCULATOR_XXX constants) */
	private final Stat _stat;
	/**
	 * Order of functions calculation.<br>
	 * Functions with lower order are executed first.<br>
	 * Functions with the same order are executed in unspecified order.<br>
	 * Usually add/subtract functions has lowest order,<br>
	 * then bonus/penalty functions (multiply/divide) are applied, then functions that do more complex<br>
	 * calculations (non-linear functions).
	 */
	private final int _order;
	/**
	 * Owner can be an armor, weapon, skill, system event, quest, etc.<br>
	 * Used to remove all functions added by this owner.
	 */
	private final Object _funcOwner;
	/** Function may be disabled by attached condition. */
	private final Condition _applyCond;
	/** The value. */
	private final double _value;
	
	/**
	 * Constructor of Func.
	 * @param stat the stat
	 * @param order the order
	 * @param owner the owner
	 * @param value the value
	 * @param applyCond the apply condition
	 */
	public AbstractFunction(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		_stat = stat;
		_order = order;
		_funcOwner = owner;
		_value = value;
		_applyCond = applyCond;
	}
	
	/**
	 * Gets the apply condition
	 * @return the apply condition
	 */
	public Condition getApplyCond()
	{
		return _applyCond;
	}
	
	/**
	 * Gets the fuction owner.
	 * @return the function owner
	 */
	public Object getFuncOwner()
	{
		return _funcOwner;
	}
	
	/**
	 * Gets the function order.
	 * @return the order
	 */
	public int getOrder()
	{
		return _order;
	}
	
	/**
	 * Gets the stat.
	 * @return the stat
	 */
	public Stat getStat()
	{
		return _stat;
	}
	
	/**
	 * Gets the value.
	 * @return the value
	 */
	public double getValue()
	{
		return _value;
	}
	
	/**
	 * Run the mathematics function of the Func.
	 * @param effector the effector
	 * @param effected the effected
	 * @param skill the skill
	 * @param initVal the initial value
	 * @return the calculated value
	 */
	public abstract double calc(Creature effector, Creature effected, Skill skill, double initVal);
}
