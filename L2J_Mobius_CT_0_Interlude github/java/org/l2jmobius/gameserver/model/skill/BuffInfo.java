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
package org.l2jmobius.gameserver.model.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectTaskInfo;
import org.l2jmobius.gameserver.model.effects.EffectTickTask;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Buff Info.<br>
 * Complex DTO that holds all the information for a given buff (or debuff or dance/song) set of effects issued by an skill.
 * @author Zoey76, Mobius
 */
public class BuffInfo
{
	// Data
	/** Data. */
	private final Creature _effector;
	private final Creature _effected;
	private final Skill _skill;
	/** The effects. */
	private final List<AbstractEffect> _effects = new ArrayList<>(1);
	
	// Tasks
	/** Effect tasks for ticks. */
	private final Map<AbstractEffect, EffectTaskInfo> _tasks = new ConcurrentHashMap<>();
	
	// Time and ticks
	/** Abnormal time. */
	private int _abnormalTime;
	/** The game ticks at the start of this effect. */
	private final int _periodStartTicks;
	
	// Misc
	/** If {@code true} then this effect has been cancelled. */
	private volatile SkillFinishType _finishType = SkillFinishType.NORMAL;
	/** If {@code true} then this effect is in use (or has been stop because an Herb took place). */
	private boolean _isInUse = true;
	
	/**
	 * Buff Info constructor.
	 * @param effector
	 * @param effected
	 * @param skill
	 */
	public BuffInfo(Creature effector, Creature effected, Skill skill)
	{
		_effector = effector;
		_effected = effected;
		_skill = skill;
		_abnormalTime = Formulas.calcEffectAbnormalTime(effector, effected, skill);
		_periodStartTicks = GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Gets the effects on this buff info.
	 * @return the effects
	 */
	public List<AbstractEffect> getEffects()
	{
		return _effects;
	}
	
	/**
	 * Adds an effect to this buff info.
	 * @param effect the effect to add
	 */
	public void addEffect(AbstractEffect effect)
	{
		_effects.add(effect);
	}
	
	/**
	 * Adds an effect task to this buff info.
	 * @param effect the effect that owns the task
	 * @param effectTaskInfo the task info
	 */
	private void addTask(AbstractEffect effect, EffectTaskInfo effectTaskInfo)
	{
		_tasks.put(effect, effectTaskInfo);
	}
	
	/**
	 * Gets the task for the given effect.
	 * @param effect the effect
	 * @return the task
	 */
	private EffectTaskInfo getEffectTask(AbstractEffect effect)
	{
		return _tasks.get(effect);
	}
	
	/**
	 * Gets the skill that created this buff info.
	 * @return the skill
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Gets the calculated abnormal time.
	 * @return the abnormal time
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Sets the abnormal time.
	 * @param abnormalTime the abnormal time to set
	 */
	public void setAbnormalTime(int abnormalTime)
	{
		_abnormalTime = abnormalTime;
	}
	
	/**
	 * Gets the period start ticks.
	 * @return the period start
	 */
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	/**
	 * Get the remaining time in seconds for this buff info.
	 * @return the elapsed time
	 */
	public int getTime()
	{
		return _abnormalTime - ((GameTimeTaskManager.getInstance().getGameTicks() - _periodStartTicks) / GameTimeTaskManager.TICKS_PER_SECOND);
	}
	
	/**
	 * Verify if this buff info has been cancelled.
	 * @return {@code true} if this buff info has been cancelled, {@code false} otherwise
	 */
	public boolean isRemoved()
	{
		return _finishType == SkillFinishType.REMOVED;
	}
	
	/**
	 * Set the buff info to removed.
	 * @param type the SkillFinishType to set
	 */
	public void setFinishType(SkillFinishType type)
	{
		_finishType = type;
	}
	
	/**
	 * Verify if this buff info is in use.
	 * @return {@code true} if this buff info is in use, {@code false} otherwise
	 */
	public boolean isInUse()
	{
		return _isInUse;
	}
	
	/**
	 * Set the buff info to in use.
	 * @param value the value to set
	 */
	public void setInUse(boolean value)
	{
		_isInUse = value;
	}
	
	/**
	 * Gets the character that launched the buff.
	 * @return the effector
	 */
	public Creature getEffector()
	{
		return _effector;
	}
	
	/**
	 * Gets the target of the skill.
	 * @return the effected
	 */
	public Creature getEffected()
	{
		return _effected;
	}
	
	/**
	 * Stops all the effects for this buff info.<br>
	 * Removes effects stats.<br>
	 * <b>It will not remove the buff info from the effect list</b>.<br>
	 * Instead call {@link EffectList#stopSkillEffects(SkillFinishType, Skill)}
	 * @param type determines the system message that will be sent.
	 * @param broadcast if {@code true} broadcast abnormal visual effects
	 */
	public void stopAllEffects(SkillFinishType type, boolean broadcast)
	{
		setFinishType(type);
		
		// Remove this buff info from BuffFinishTask.
		_effected.removeBuffInfoTime(this);
		finishEffects(broadcast);
	}
	
	public void initializeEffects()
	{
		if ((_effected == null) || (_skill == null))
		{
			return;
		}
		
		// When effects are initialized, the successfully landed.
		if (_effected.isPlayer() && (_effected.asPlayer().hasEnteredWorld() || PlayerConfig.SHOW_EFFECT_MESSAGES_ON_LOGIN) && !_skill.isPassive())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_EFFECTS_OF_S1_FLOW_THROUGH_YOU);
			sm.addSkillName(_skill);
			_effected.sendPacket(sm);
		}
		
		// Creates a task that will stop all the effects.
		if (_abnormalTime > 0)
		{
			_effected.addBuffInfoTime(this);
		}
		
		boolean update = false;
		for (AbstractEffect effect : _effects)
		{
			if (effect.isInstant() || (_effected.isDead() && !_skill.isPassive()))
			{
				continue;
			}
			
			// Call on start.
			effect.onStart(_effector, _effected, _skill);
			
			// Do not add continuous effect if target just died from the initial effect, otherwise they'll be ticked forever.
			if (_effected.isDead() && !_skill.isPassive())
			{
				continue;
			}
			
			// If it's a continuous effect, if has ticks schedule a task with period, otherwise schedule a simple task to end it.
			if (effect.getTicks() > 0)
			{
				// The task for the effect ticks.
				final EffectTickTask effectTask = new EffectTickTask(this, effect);
				addTask(effect, new EffectTaskInfo(effectTask, ThreadPool.scheduleAtFixedRate(effectTask, effect.getTicks() * PlayerConfig.EFFECT_TICK_RATIO, effect.getTicks() * PlayerConfig.EFFECT_TICK_RATIO)));
			}
			
			// Add stats.
			_effected.addStatFuncs(effect.getStatFuncs(_effector, _effected, _skill));
			
			update = true;
		}
		
		if (update)
		{
			// Add abnormal visual effects.
			addAbnormalVisualEffects();
		}
	}
	
	/**
	 * Called on each tick.<br>
	 * Verify if the effect should end and the effect task should be cancelled.
	 * @param effect the effect that is ticking
	 */
	public void onTick(AbstractEffect effect)
	{
		boolean continueForever = false;
		
		// If the effect is in use, allow it to affect the effected.
		if (_isInUse)
		{
			// Callback for on action time event.
			continueForever = effect.onActionTime(_effector, _effected, _skill);
		}
		
		if (!continueForever && _skill.isToggle())
		{
			final EffectTaskInfo task = getEffectTask(effect);
			if (task != null)
			{
				final ScheduledFuture<?> schedule = task.getScheduledFuture();
				if ((schedule != null) && !schedule.isCancelled() && !schedule.isDone())
				{
					schedule.cancel(true); // Don't allow to finish current run.
				}
				
				_effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _skill); // Remove the buff from the effect list.
			}
		}
	}
	
	public void finishEffects(boolean broadcast)
	{
		// Cancels the ticking task.
		for (EffectTaskInfo effectTask : _tasks.values())
		{
			final ScheduledFuture<?> schedule = effectTask.getScheduledFuture();
			if ((schedule != null) && !schedule.isCancelled() && !schedule.isDone())
			{
				schedule.cancel(true); // Don't allow to finish current run.
			}
		}
		
		// Remove stats
		removeStats();
		
		// Notify on exit.
		for (AbstractEffect effect : _effects)
		{
			// Instant effects shouldn't call onExit(..).
			if ((effect != null) && !effect.isInstant())
			{
				effect.onExit(_effector, _effected, _skill);
			}
		}
		
		// Remove abnormal visual effects.
		removeAbnormalVisualEffects(broadcast);
		
		// Set the proper system message.
		if (!(_effected.isSummon() && !_effected.asSummon().getOwner().hasSummon()))
		{
			SystemMessageId smId = null;
			if (_finishType == SkillFinishType.SILENT)
			{
				// smId is null.
			}
			else if (_skill.isToggle())
			{
				smId = SystemMessageId.S1_HAS_BEEN_ABORTED;
			}
			else if (_finishType == SkillFinishType.REMOVED)
			{
				smId = SystemMessageId.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED;
			}
			else if (!_skill.isPassive())
			{
				smId = SystemMessageId.S1_HAS_WORN_OFF;
			}
			
			if (smId != null)
			{
				final SystemMessage sm = new SystemMessage(smId);
				sm.addSkillName(_skill);
				_effected.sendPacket(sm);
			}
		}
		
		// Remove short buff.
		if (this == _effected.getEffectList().getShortBuff())
		{
			_effected.getEffectList().shortBuffStatusUpdate(null);
		}
	}
	
	public boolean isAbnormalType(AbnormalType type)
	{
		return _skill.getAbnormalType() == type;
	}
	
	/**
	 * Applies all the abnormal visual effects to the effected.<br>
	 * Prevents multiple updates.
	 */
	private void addAbnormalVisualEffects()
	{
		if (_skill.hasAbnormalVisualEffects())
		{
			_effected.startAbnormalVisualEffect(false, _skill.getAbnormalVisualEffects());
		}
		
		if (_effected.isPlayer() && _skill.hasAbnormalVisualEffectsEvent())
		{
			_effected.startAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsEvent());
		}
		
		if (_skill.hasAbnormalVisualEffectsSpecial())
		{
			_effected.startAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsSpecial());
		}
		
		// Update abnormal visual effects.
		_effected.updateAbnormalEffect();
	}
	
	/**
	 * Removes all the abnormal visual effects from the effected.<br>
	 * Prevents multiple updates.
	 * @param broadcast if {@code true} broadcast abnormal visual effects
	 */
	private void removeAbnormalVisualEffects(boolean broadcast)
	{
		if ((_effected == null) || (_skill == null))
		{
			return;
		}
		
		if (_skill.hasAbnormalVisualEffects())
		{
			_effected.stopAbnormalVisualEffect(false, _skill.getAbnormalVisualEffects());
		}
		
		if (_effected.isPlayer() && _skill.hasAbnormalVisualEffectsEvent())
		{
			_effected.stopAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsEvent());
		}
		
		if (_skill.hasAbnormalVisualEffectsSpecial())
		{
			_effected.stopAbnormalVisualEffect(false, _skill.getAbnormalVisualEffectsSpecial());
		}
		
		if (broadcast)
		{
			_effected.updateAbnormalEffect();
		}
	}
	
	/**
	 * Adds the buff stats.
	 */
	public void addStats()
	{
		_effects.forEach(effect -> _effected.addStatFuncs(effect.getStatFuncs(_effector, _effected, _skill)));
	}
	
	/**
	 * Removes the buff stats.
	 */
	public void removeStats()
	{
		_effects.forEach(_effected::removeStatsOwner);
		_effected.removeStatsOwner(_skill);
	}
	
	@Override
	public String toString()
	{
		return "BuffInfo [effector=" + _effector + ", effected=" + _effected + ", skill=" + _skill + ", effects=" + _effects + ", tasks=" + _tasks + ", abnormalTime=" + _abnormalTime + ", periodStartTicks=" + _periodStartTicks + ", isRemoved=" + isRemoved() + ", isInUse=" + _isInUse + "]";
	}
}
