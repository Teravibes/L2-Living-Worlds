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
package org.l2jmobius.gameserver.model.effects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.model.stats.functions.FuncTemplate;

/**
 * Abstract effect implementation.
 * @author Zoey76, Mobius
 */
public abstract class AbstractEffect
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractEffect.class.getName());
	
	private final Condition _attachCond;
	private List<FuncTemplate> _funcTemplates;
	private final String _name;
	private final int _ticks;
	
	/**
	 * Abstract effect constructor.
	 * @param attachCond the attach condition
	 * @param applyCond the apply condition
	 * @param set the attributes
	 * @param params the parameters
	 */
	protected AbstractEffect(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		_attachCond = attachCond;
		_name = set.getString("name");
		_ticks = set.getInt("ticks", 0);
	}
	
	/**
	 * Creates an effect given the parameters.
	 * @param attachCond the attach condition
	 * @param applyCond the apply condition
	 * @param set the attributes
	 * @param params the parameters
	 * @return the new effect
	 */
	public static AbstractEffect createEffect(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		final String name = set.getString("name");
		final Class<? extends AbstractEffect> handler = EffectHandler.getInstance().getHandler(name);
		if (handler == null)
		{
			LOGGER.warning(AbstractEffect.class.getSimpleName() + ": Requested unexistent effect handler: " + name);
			return null;
		}
		
		final Constructor<?> constructor;
		try
		{
			constructor = handler.getConstructor(Condition.class, Condition.class, StatSet.class, StatSet.class);
		}
		catch (NoSuchMethodException | SecurityException e)
		{
			LOGGER.warning(AbstractEffect.class.getSimpleName() + ": Requested unexistent constructor for effect handler: " + name + ": " + e.getMessage());
			return null;
		}
		
		try
		{
			return (AbstractEffect) constructor.newInstance(attachCond, applyCond, set, params);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			LOGGER.warning(AbstractEffect.class.getSimpleName() + ": Unable to initialize effect handler: " + name + ": " + e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Tests the attach condition.
	 * @param caster the caster
	 * @param target the target
	 * @param skill the skill
	 * @return {@code true} if there isn't a condition to test or it's passed, {@code false} otherwise
	 */
	public boolean testConditions(Creature caster, Creature target, Skill skill)
	{
		return (_attachCond == null) || _attachCond.test(caster, target, skill);
	}
	
	/**
	 * Attaches a function template.
	 * @param f the function
	 */
	public void attach(FuncTemplate f)
	{
		if (_funcTemplates == null)
		{
			_funcTemplates = new ArrayList<>(1);
		}
		
		_funcTemplates.add(f);
	}
	
	/**
	 * Gets the effect name.
	 * @return the name
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Gets the effect ticks
	 * @return the ticks
	 */
	public int getTicks()
	{
		return _ticks;
	}
	
	public double getTicksMultiplier()
	{
		return (getTicks() * PlayerConfig.EFFECT_TICK_RATIO) / 1000f;
	}
	
	public List<FuncTemplate> getFuncTemplates()
	{
		return _funcTemplates;
	}
	
	/**
	 * Calculates whether this effects land or not.<br>
	 * If it lands will be scheduled and added to the character effect list.<br>
	 * Override in effect implementation to change behavior.<br>
	 * <b>Warning:</b> Must be used only for instant effects continuous effects will not call this they have their success handled by activate_rate.
	 * @param effector
	 * @param effected
	 * @param skill
	 * @return {@code true} if this effect land, {@code false} otherwise
	 */
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	/**
	 * Get this effect's type.
	 * @return the effect type
	 */
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	/**
	 * Verify if the buff can start.<br>
	 * Used for continuous effects.
	 * @param effector
	 * @param effected
	 * @param skill
	 * @return {@code true} if all the start conditions are meet, {@code false} otherwise
	 */
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
	}
	
	/**
	 * Called on each tick.<br>
	 * If the abnormal time is lesser than zero it will last forever.
	 * @param effector
	 * @param effected
	 * @param skill
	 * @return if {@code true} this effect will continue forever, if {@code false} it will stop after abnormal time has passed
	 */
	public boolean onActionTime(Creature effector, Creature effected, Skill skill)
	{
		return false;
	}
	
	/**
	 * Called when the effect is exited.
	 * @param effector
	 * @param effected
	 * @param skill
	 */
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
	}
	
	/**
	 * Get this effect's stats functions.
	 * @param caster the caster
	 * @param target the target
	 * @param skill the skill
	 * @return a list of stat functions.
	 */
	public List<AbstractFunction> getStatFuncs(Creature caster, Creature target, Skill skill)
	{
		if (_funcTemplates == null)
		{
			return Collections.<AbstractFunction> emptyList();
		}
		
		final List<AbstractFunction> functions = new ArrayList<>(_funcTemplates.size());
		for (FuncTemplate functionTemplate : _funcTemplates)
		{
			final AbstractFunction function = functionTemplate.getFunc(caster, target, skill, this);
			if (function != null)
			{
				functions.add(function);
			}
		}
		
		return functions;
	}
	
	/**
	 * Get the effect flags.
	 * @return bit flag for current effect
	 */
	public int getEffectFlags()
	{
		return EffectFlag.NONE.getMask();
	}
	
	@Override
	public String toString()
	{
		return "Effect " + _name;
	}
	
	public void decreaseForce()
	{
	}
	
	public void increaseEffect()
	{
	}
	
	public boolean checkCondition(Object obj)
	{
		return true;
	}
	
	/**
	 * Verify if this effect is an instant effect.
	 * @return {@code true} if this effect is instant, {@code false} otherwise
	 */
	public boolean isInstant()
	{
		return false;
	}
}
