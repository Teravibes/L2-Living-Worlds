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
package org.l2jmobius.gameserver.model.stats;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;

/**
 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).<br>
 * In fact, each calculator is a table of Func object in which each Func represents a mathematical function:<br>
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<br>
 * When the calc method of a calculator is launched, each mathematical function is called according to its priority <b>_order</b>.<br>
 * Indeed, Func with lowest priority order is executed first and Funcs with the same order are executed in unspecified order.<br>
 * The result of the calculation is stored in the value property of an Env class instance.<br>
 * Method addFunc and removeFunc permit to add and remove a Func object from a Calculator.
 */
public class Calculator
{
	/** Empty Func table definition */
	private static final AbstractFunction[] EMPTY_FUNCS = new AbstractFunction[0];
	
	/** Table of Func object */
	private AbstractFunction[] _functions;
	
	/**
	 * Constructor of Calculator (Init value : emptyFuncs).
	 */
	public Calculator()
	{
		_functions = EMPTY_FUNCS;
	}
	
	/**
	 * Constructor of Calculator (Init value : Calculator c).
	 * @param c
	 */
	public Calculator(Calculator c)
	{
		_functions = c._functions;
	}
	
	/**
	 * Check if 2 calculators are equals.
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static boolean equalsCals(Calculator c1, Calculator c2)
	{
		if (c1 == c2)
		{
			return true;
		}
		
		if ((c1 == null) || (c2 == null))
		{
			return false;
		}
		
		final AbstractFunction[] funcs1 = c1._functions;
		final AbstractFunction[] funcs2 = c2._functions;
		if (funcs1 == funcs2)
		{
			return true;
		}
		
		if (funcs1.length != funcs2.length)
		{
			return false;
		}
		
		if (funcs1.length == 0)
		{
			return true;
		}
		
		for (int i = 0; i < funcs1.length; i++)
		{
			if (funcs1[i] != funcs2[i])
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Return the number of Funcs in the Calculator.
	 * @return
	 */
	public int size()
	{
		return _functions.length;
	}
	
	/**
	 * Adds a function to the Calculator.
	 * @param function the function
	 */
	public synchronized void addFunc(AbstractFunction function)
	{
		final AbstractFunction[] funcs = _functions;
		final AbstractFunction[] tmp = new AbstractFunction[funcs.length + 1];
		final int order = function.getOrder();
		int i;
		for (i = 0; (i < funcs.length) && (order >= funcs[i].getOrder()); i++)
		{
			tmp[i] = funcs[i];
		}
		
		tmp[i] = function;
		for (; i < funcs.length; i++)
		{
			tmp[i + 1] = funcs[i];
		}
		
		_functions = tmp;
	}
	
	/**
	 * Removes a function from the Calculator.
	 * @param function the function
	 */
	public synchronized void removeFunc(AbstractFunction function)
	{
		final AbstractFunction[] funcs = _functions;
		final AbstractFunction[] tmp = new AbstractFunction[funcs.length - 1];
		int i;
		for (i = 0; (i < (funcs.length - 1)) && (function != funcs[i]); i++)
		{
			tmp[i] = funcs[i];
		}
		
		if (i == funcs.length)
		{
			return;
		}
		
		for (i++; i < funcs.length; i++)
		{
			tmp[i - 1] = funcs[i];
		}
		
		if (tmp.length == 0)
		{
			_functions = EMPTY_FUNCS;
		}
		else
		{
			_functions = tmp;
		}
	}
	
	/**
	 * Remove each Func with the specified owner of the Calculator.
	 * @param owner the owner
	 * @return a list of modified stats
	 */
	public synchronized List<Stat> removeOwner(Object owner)
	{
		final List<Stat> modifiedStats = new ArrayList<>();
		for (AbstractFunction func : _functions)
		{
			if (func.getFuncOwner() == owner)
			{
				modifiedStats.add(func.getStat());
				removeFunc(func);
			}
		}
		
		return modifiedStats;
	}
	
	/**
	 * Run each function of the Calculator.
	 * @param caster the caster
	 * @param target the target
	 * @param skill the skill
	 * @param initVal the initial value
	 * @return the calculated value
	 */
	public double calc(Creature caster, Creature target, Skill skill, double initVal)
	{
		double value = initVal;
		for (AbstractFunction func : _functions)
		{
			value = func.calc(caster, target, skill, value);
		}
		
		return value;
	}
	
	/**
	 * Get array of all function, do not use for add/remove
	 * @return
	 */
	public AbstractFunction[] getFunctions()
	{
		return _functions;
	}
}
