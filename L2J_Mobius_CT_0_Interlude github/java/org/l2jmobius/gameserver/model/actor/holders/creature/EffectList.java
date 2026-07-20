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
package org.l2jmobius.gameserver.model.actor.holders.creature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.EffectScope;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.serverpackets.AbnormalStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import org.l2jmobius.gameserver.network.serverpackets.PartySpelled;
import org.l2jmobius.gameserver.network.serverpackets.ShortBuffStatusUpdate;

/**
 * Effect lists.<br>
 * Holds all the buff infos that are affecting a creature.<br>
 * Manages the logic that controls whether a buff is added, remove, replaced or set inactive.<br>
 * Uses maps with skill ID as key and buff info DTO as value to avoid iterations.<br>
 * Methods may resemble List interface, although it doesn't implement such interface.
 * @author Zoey76, Mobius
 */
public class EffectList
{
	private static final Logger LOGGER = Logger.getLogger(EffectList.class.getName());
	/** Queue containing all effects from buffs for this effect list. */
	private final Queue<BuffInfo> _buffs = new ConcurrentLinkedQueue<>();
	/** Queue containing all dances/songs for this effect list. */
	private final Queue<BuffInfo> _dances = new ConcurrentLinkedQueue<>();
	/** Queue containing all toggle for this effect list. */
	private final Queue<BuffInfo> _toggles = new ConcurrentLinkedQueue<>();
	/** Queue containing all debuffs for this effect list. */
	private final Queue<BuffInfo> _debuffs = new ConcurrentLinkedQueue<>();
	/** Queue containing all passives for this effect list. They bypass most of the actions and they are not included in most operations. */
	private final Queue<BuffInfo> _passives = new ConcurrentLinkedQueue<>();
	/** Map containing the all stacked effect in progress for each abnormal type. */
	private final Map<AbnormalType, BuffInfo> _stackedEffects = new ConcurrentHashMap<>();
	/** Set containing all {@code AbnormalType}s that shouldn't be added to this creature effect list. */
	private final Set<AbnormalType> _blockedAbnormalTypes = EnumSet.noneOf(AbnormalType.class);
	/** Short buff skill ID. */
	private BuffInfo _shortBuff = null;
	/** If {@code true} this effect list has buffs removed on any action. */
	private volatile boolean _hasBuffsRemovedOnAnyAction = false;
	/** If {@code true} this effect list has buffs removed on damage. */
	private volatile boolean _hasBuffsRemovedOnDamage = false;
	/** If {@code true} this effect list has debuffs removed on damage. */
	private volatile boolean _hasDebuffsRemovedOnDamage = false;
	/** Effect flags. */
	private int _effectFlags;
	/** If {@code true} only party icons need to be updated. */
	private boolean _partyOnly = false;
	/** The owner of this effect list. */
	private final Creature _owner;
	/** Hidden buffs count, prevents iterations. */
	private final AtomicInteger _hiddenBuffs = new AtomicInteger();
	/** Delay task **/
	private ScheduledFuture<?> _updateEffectIconTask;
	
	/**
	 * Constructor for effect list.
	 * @param owner the creature that owns this effect list
	 */
	public EffectList(Creature owner)
	{
		_owner = owner;
	}
	
	/**
	 * Gets buff skills.
	 * @return the buff skills
	 */
	public Queue<BuffInfo> getBuffs()
	{
		return _buffs;
	}
	
	/**
	 * Gets dance/song skills.
	 * @return the dance/song skills
	 */
	public Queue<BuffInfo> getDances()
	{
		return _dances;
	}
	
	/**
	 * Gets toggle skills.
	 * @return the toggle skills
	 */
	public Queue<BuffInfo> getToggles()
	{
		return _toggles;
	}
	
	/**
	 * Gets debuff skills.
	 * @return the debuff skills
	 */
	public Queue<BuffInfo> getDebuffs()
	{
		return _debuffs;
	}
	
	/**
	 * Gets passive skills.
	 * @return the passive skills
	 */
	public Queue<BuffInfo> getPassives()
	{
		return _passives;
	}
	
	/**
	 * Gets all the effects on this effect list.
	 * @return all the effects on this effect list
	 */
	public List<BuffInfo> getEffects()
	{
		if (isEmpty())
		{
			return Collections.<BuffInfo> emptyList();
		}
		
		final List<BuffInfo> buffs = new LinkedList<>();
		if (hasBuffs())
		{
			buffs.addAll(_buffs);
		}
		
		if (hasDances())
		{
			buffs.addAll(_dances);
		}
		
		if (hasToggles())
		{
			buffs.addAll(_toggles);
		}
		
		if (hasDebuffs())
		{
			buffs.addAll(_debuffs);
		}
		
		return buffs;
	}
	
	/**
	 * Gets the effect list where the skill effects should be.
	 * @param skill the skill
	 * @return the effect list
	 */
	private Queue<BuffInfo> getEffectList(Skill skill)
	{
		if (skill == null)
		{
			return null;
		}
		
		final Queue<BuffInfo> effects;
		if (skill.isPassive())
		{
			effects = _passives;
		}
		else if (skill.isDebuff())
		{
			effects = _debuffs;
		}
		else if (skill.isDance())
		{
			effects = _dances;
		}
		else if (skill.isToggle())
		{
			effects = _toggles;
		}
		else
		{
			effects = _buffs;
		}
		
		return effects;
	}
	
	/**
	 * Gets the first effect for the given effect type.<br>
	 * Prevents initialization.<br>
	 * TODO: Remove this method after all the effect types gets replaced by abnormal skill types.
	 * @param type the effect type
	 * @return the first effect matching the given effect type
	 */
	public BuffInfo getFirstEffect(EffectType type)
	{
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				if (info != null)
				{
					for (AbstractEffect effect : info.getEffects())
					{
						if ((effect != null) && (effect.getEffectType() == type))
						{
							return info;
						}
					}
				}
			}
		}
		
		if (hasDances())
		{
			for (BuffInfo info : _dances)
			{
				if (info != null)
				{
					for (AbstractEffect effect : info.getEffects())
					{
						if ((effect != null) && (effect.getEffectType() == type))
						{
							return info;
						}
					}
				}
			}
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				if (info != null)
				{
					for (AbstractEffect effect : info.getEffects())
					{
						if ((effect != null) && (effect.getEffectType() == type))
						{
							return info;
						}
					}
				}
			}
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if (info != null)
				{
					for (AbstractEffect effect : info.getEffects())
					{
						if ((effect != null) && (effect.getEffectType() == type))
						{
							return info;
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Verifies if this effect list contains the given skill ID.<br>
	 * Prevents initialization.
	 * @param skillId the skill ID to verify
	 * @return {@code true} if the skill ID is present in the effect list, {@code false} otherwise
	 */
	public boolean isAffectedBySkill(int skillId)
	{
		return getBuffInfoBySkillId(skillId) != null;
	}
	
	/**
	 * Gets the buff info by skill ID.<br>
	 * Prevents initialization.
	 * @param skillId the skill ID
	 * @return the buff info
	 */
	public BuffInfo getBuffInfoBySkillId(int skillId)
	{
		BuffInfo info = null;
		
		if (hasBuffs())
		{
			for (BuffInfo b : _buffs)
			{
				if (b.getSkill().getId() == skillId)
				{
					info = b;
					break;
				}
			}
		}
		
		if (hasDances() && (info == null))
		{
			for (BuffInfo b : _dances)
			{
				if (b.getSkill().getId() == skillId)
				{
					info = b;
					break;
				}
			}
		}
		
		if (hasToggles() && (info == null))
		{
			for (BuffInfo b : _toggles)
			{
				if (b.getSkill().getId() == skillId)
				{
					info = b;
					break;
				}
			}
		}
		
		if (hasDebuffs() && (info == null))
		{
			for (BuffInfo b : _debuffs)
			{
				if (b.getSkill().getId() == skillId)
				{
					info = b;
					break;
				}
			}
		}
		
		if (hasPassives() && (info == null))
		{
			for (BuffInfo b : _passives)
			{
				if (b.getSkill().getId() == skillId)
				{
					info = b;
					break;
				}
			}
		}
		
		return info;
	}
	
	/**
	 * Gets a buff info by abnormal type.<br>
	 * It's O(1) for every buff in this effect list.
	 * @param type the abnormal skill type
	 * @return the buff info if it's present, {@code null} otherwise
	 */
	public BuffInfo getBuffInfoByAbnormalType(AbnormalType type)
	{
		return _stackedEffects.get(type);
	}
	
	/**
	 * Adds {@code AbnormalType}s to the blocked buff slot set.
	 * @param blockedAbnormalTypes the blocked buff slot set to add
	 */
	public void addBlockedAbnormalTypes(Set<AbnormalType> blockedAbnormalTypes)
	{
		_blockedAbnormalTypes.addAll(blockedAbnormalTypes);
	}
	
	/**
	 * Removes {@code AbnormalType}s from the blocked buff slot set.
	 * @param blockedBuffSlots the blocked buff slot set to remove
	 * @return {@code true} if the blocked buff slots set has been modified, {@code false} otherwise
	 */
	public boolean removeBlockedAbnormalTypes(Set<AbnormalType> blockedBuffSlots)
	{
		return _blockedAbnormalTypes.removeAll(blockedBuffSlots);
	}
	
	/**
	 * Gets all the blocked {@code AbnormalType}s for this creature effect list.
	 * @return the current blocked {@code AbnormalType}s set in unmodifiable view.
	 */
	public Set<AbnormalType> getBlockedAbnormalTypes()
	{
		return Collections.unmodifiableSet(_blockedAbnormalTypes);
	}
	
	/**
	 * Gets the Short Buff info.
	 * @return the short buff info
	 */
	public BuffInfo getShortBuff()
	{
		return _shortBuff;
	}
	
	/**
	 * Sets the Short Buff data and sends an update if the effected is a player.
	 * @param info the buff info
	 */
	public void shortBuffStatusUpdate(BuffInfo info)
	{
		if (_owner.isPlayer())
		{
			_shortBuff = info;
			if (info != null)
			{
				_owner.sendPacket(new ShortBuffStatusUpdate(info.getSkill().getId(), info.getSkill().getLevel(), info.getTime()));
			}
			else
			{
				_owner.sendPacket(ShortBuffStatusUpdate.RESET_SHORT_BUFF);
			}
		}
	}
	
	/**
	 * Checks if the given skill stacks with an existing one.
	 * @param skill the skill to verify
	 * @return {@code true} if this effect stacks with the given skill, {@code false} otherwise
	 */
	private boolean doesStack(Skill skill)
	{
		final AbnormalType type = skill.getAbnormalType();
		if (type.isNone() || isEmpty())
		{
			return false;
		}
		
		final Queue<BuffInfo> effects = getEffectList(skill);
		for (BuffInfo info : effects)
		{
			if ((info != null) && (info.getSkill().getAbnormalType() == type))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Gets the buffs count without including the hidden buffs (after getting an Herb buff).<br>
	 * Prevents initialization.
	 * @return the number of buffs in this creature effect list
	 */
	public int getBuffCount()
	{
		return hasBuffs() ? _buffs.size() - _hiddenBuffs.get() - (_shortBuff != null ? 1 : 0) : 0;
	}
	
	/**
	 * Gets the Songs/Dances count.<br>
	 * Prevents initialization.
	 * @return the number of Songs/Dances in this creature effect list
	 */
	public int getDanceCount()
	{
		return hasDances() ? _dances.size() : 0;
	}
	
	/**
	 * Gets the hidden buff count.
	 * @return the number of hidden buffs
	 */
	public int getHiddenBuffsCount()
	{
		return _hiddenBuffs.get();
	}
	
	/**
	 * Auxiliary method to stop all effects from a buff info and remove it from an effect list and stacked effects.
	 * @param info the buff info
	 */
	protected void stopAndRemove(BuffInfo info)
	{
		stopAndRemove(true, SkillFinishType.REMOVED, info, getEffectList(info.getSkill()));
	}
	
	/**
	 * Auxiliary method to stop all effects from a buff info and remove it from an effect list and stacked effects.
	 * @param broadcast if {@code true} broadcast abnormal visual effects
	 * @param info the buff info
	 * @param effects the effect list
	 */
	protected void stopAndRemove(boolean broadcast, BuffInfo info, Queue<BuffInfo> effects)
	{
		stopAndRemove(broadcast, SkillFinishType.REMOVED, info, effects);
	}
	
	/**
	 * Auxiliary method to stop all effects from a buff info and remove it from an effect list and stacked effects.
	 * @param broadcast if {@code true} broadcast abnormal visual effects
	 * @param type determines the system message that will be sent.
	 * @param info the buff info
	 * @param buffs the buff list
	 */
	private void stopAndRemove(boolean broadcast, SkillFinishType type, BuffInfo info, Queue<BuffInfo> buffs)
	{
		if (info == null)
		{
			return;
		}
		
		// Removes the buff from the given effect list.
		buffs.remove(info);
		
		// Stop the buff effects.
		info.stopAllEffects(type, broadcast);
		
		// If it's a hidden buff that ends, then decrease hidden buff count.
		if (!info.isInUse())
		{
			_hiddenBuffs.decrementAndGet();
		}
		else // Removes the buff from the stack.
		{
			_stackedEffects.remove(info.getSkill().getAbnormalType());
		}
		
		// If it's an herb that ends, check if there are hidden buffs.
		if (info.getSkill().isAbnormalInstant() && hasBuffs())
		{
			for (BuffInfo buff : _buffs)
			{
				if ((buff != null) && (buff.getSkill().getAbnormalType() == info.getSkill().getAbnormalType()) && !buff.isInUse())
				{
					// Sets the buff in use again.
					buff.setInUse(true);
					
					// Adds the stats.
					buff.addStats();
					
					// Adds the buff to the stack.
					_stackedEffects.put(buff.getSkill().getAbnormalType(), buff);
					
					// If it's a hidden buff that gets activated, then decrease hidden buff count.
					_hiddenBuffs.decrementAndGet();
					break;
				}
			}
		}
		
		if (type != SkillFinishType.REMOVED)
		{
			info.getSkill().applyEffectScope(EffectScope.END, info, true, false);
		}
	}
	
	/**
	 * Exits all effects in this effect list.<br>
	 * Stops all the effects, clear the effect lists and updates the effect flags and icons.
	 */
	public void stopAllEffects()
	{
		// Stop buffs.
		stopAllBuffs(false);
		
		// Stop dances and songs.
		stopAllDances(false);
		
		// Stop toggles.
		stopAllToggles(false);
		
		// Stop debuffs.
		stopAllDebuffs(false);
		
		_stackedEffects.clear();
		
		// Update effect flags, icons and ave.
		updateEffectList(true);
		_owner.updateAbnormalEffect();
	}
	
	/**
	 * Stops all effects in this effect list except those that last through death.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		boolean update = false;
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				if (!info.getSkill().isStayAfterDeath())
				{
					stopAndRemove(true, info, _buffs);
				}
			}
			
			update = true;
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if (!info.getSkill().isStayAfterDeath())
				{
					stopAndRemove(true, info, _debuffs);
				}
			}
			
			update = true;
		}
		
		if (hasDances())
		{
			for (BuffInfo info : _dances)
			{
				if (!info.getSkill().isStayAfterDeath())
				{
					stopAndRemove(true, info, _dances);
				}
			}
			
			update = true;
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				if (!info.getSkill().isStayAfterDeath())
				{
					stopAndRemove(true, info, _toggles);
				}
			}
			
			update = true;
		}
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Exits all effects in this effect list without excluding anything.<br>
	 * Stops all the effects, clear the effect lists and updates the effect flags and icons.
	 * @param update set to true to update the effect flags and icons.
	 * @param broadcast {@code true} to broadcast update packets, {@code false} otherwise.
	 */
	public void stopAllEffectsWithoutExclusions(boolean update, boolean broadcast)
	{
		for (BuffInfo info : _buffs)
		{
			stopAndRemove(broadcast, info, _buffs);
		}
		
		for (BuffInfo info : _dances)
		{
			stopAndRemove(broadcast, info, _dances);
		}
		
		for (BuffInfo info : _toggles)
		{
			stopAndRemove(broadcast, info, _toggles);
		}
		
		for (BuffInfo info : _debuffs)
		{
			stopAndRemove(broadcast, info, _debuffs);
		}
		
		for (BuffInfo info : _passives)
		{
			stopAndRemove(broadcast, info, _passives);
		}
		
		// Update stats, effect flags and icons.
		if (update)
		{
			updateEffectList(broadcast);
		}
	}
	
	/**
	 * Stop all effects that doesn't stay on sub-class change.
	 */
	public void stopAllEffectsNotStayOnSubclassChange()
	{
		boolean update = false;
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				if (!info.getSkill().isStayOnSubclassChange())
				{
					stopAndRemove(true, info, _buffs);
				}
			}
			
			update = true;
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if (!info.getSkill().isStayOnSubclassChange())
				{
					stopAndRemove(true, info, _debuffs);
				}
			}
			
			update = true;
		}
		
		if (hasDances())
		{
			for (BuffInfo info : _dances)
			{
				if (!info.getSkill().isStayOnSubclassChange())
				{
					stopAndRemove(true, info, _dances);
				}
			}
			
			update = true;
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				if (!info.getSkill().isStayOnSubclassChange())
				{
					stopAndRemove(true, info, _toggles);
				}
			}
			
			update = true;
		}
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Stops all the active buffs.
	 * @param update set to true to update the effect flags and icons
	 */
	public void stopAllBuffs(boolean update)
	{
		if (hasBuffs())
		{
			_buffs.forEach(b -> stopAndRemove(update, b, _buffs));
		}
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Stops all active toggle skills.<br>
	 * Performs an update.
	 */
	public void stopAllToggles()
	{
		stopAllToggles(true);
	}
	
	/**
	 * Stops all active toggle skills.
	 * @param update set to true to update the effect flags and icons
	 */
	public void stopAllToggles(boolean update)
	{
		if (!hasToggles())
		{
			return;
		}
		
		_toggles.forEach(b -> stopAndRemove(update, b, _toggles));
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Stops all active dances/songs skills.
	 * @param update set to true to update the effect flags and icons
	 */
	public void stopAllDances(boolean update)
	{
		if (!hasDances())
		{
			return;
		}
		
		_dances.forEach(b -> stopAndRemove(update, b, _dances));
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Stops all active dances/songs skills.
	 * @param update set to true to update the effect flags and icons
	 */
	public void stopAllDebuffs(boolean update)
	{
		if (!hasDebuffs())
		{
			return;
		}
		
		_debuffs.forEach(b -> stopAndRemove(update, b, _debuffs));
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Exit all effects having a specified type.<br>
	 * TODO: Remove after all effect types are replaced by abnormal skill types.
	 * @param type the type of the effect to stop
	 */
	public void stopEffects(EffectType type)
	{
		boolean update = false;
		final Consumer<BuffInfo> action = info ->
		{
			for (AbstractEffect effect : info.getEffects())
			{
				if ((effect != null) && (effect.getEffectType() == type))
				{
					stopAndRemove(info);
				}
			}
		};
		
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				if (info != null)
				{
					action.accept(info);
				}
			}
			
			update = true;
		}
		
		if (hasDances())
		{
			for (BuffInfo info : _dances)
			{
				if (info != null)
				{
					action.accept(info);
				}
			}
			
			update = true;
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				if (info != null)
				{
					action.accept(info);
				}
			}
			
			update = true;
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if (info != null)
				{
					action.accept(info);
				}
			}
			
			update = true;
		}
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Exits all effects created by a specific skill ID.<br>
	 * Removes the effects from the effect list.<br>
	 * Removes the stats from the creature.<br>
	 * Updates the effect flags and icons.<br>
	 * @param type determines the system message that will be sent.
	 * @param skillId the skill ID
	 */
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		final BuffInfo info = getBuffInfoBySkillId(skillId);
		if (info != null)
		{
			remove(type, info);
		}
	}
	
	/**
	 * Exits all effects created by a specific skill.<br>
	 * Removes the effects from the effect list.<br>
	 * Removes the stats from the creature.<br>
	 * Updates the effect flags and icons.<br>
	 * @param type determines the system message that will be sent.
	 * @param skill the skill
	 */
	public void stopSkillEffects(SkillFinishType type, Skill skill)
	{
		if (skill != null)
		{
			stopSkillEffects(type, skill.getId());
		}
	}
	
	/**
	 * Exits all effects created by a specific skill abnormal type.<br>
	 * It's O(1) for every effect in this effect list except passive effects.<br>
	 * @param removeType determines the system message that will be sent.
	 * @param abnormalType the skill abnormal type
	 * @return {@code true} if there was a buff info with the given abnormal type
	 */
	public boolean stopSkillEffects(SkillFinishType removeType, AbnormalType abnormalType)
	{
		final BuffInfo old = _stackedEffects.remove(abnormalType);
		if (old != null)
		{
			stopSkillEffects(removeType, old.getSkill());
			return true;
		}
		
		return false;
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set.<br>
	 * Called on any action except movement (attack, cast).
	 */
	public void stopEffectsOnAction()
	{
		if (!_hasBuffsRemovedOnAnyAction)
		{
			return;
		}
		
		boolean update = false;
		
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				if (info.getSkill().isRemovedOnAnyActionExceptMove())
				{
					stopAndRemove(true, info, _buffs);
				}
			}
			
			update = true;
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if (info.getSkill().isRemovedOnAnyActionExceptMove())
				{
					stopAndRemove(true, info, _debuffs);
				}
			}
			
			update = true;
		}
		
		if (hasDances())
		{
			for (BuffInfo info : _dances)
			{
				if (info.getSkill().isRemovedOnAnyActionExceptMove())
				{
					stopAndRemove(true, info, _dances);
				}
			}
			
			update = true;
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				if (info.getSkill().isRemovedOnAnyActionExceptMove())
				{
					stopAndRemove(true, info, _toggles);
				}
			}
			
			update = true;
		}
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	public void stopEffectsOnDamage(boolean awake)
	{
		if (!awake)
		{
			return;
		}
		
		boolean update = false;
		if (_hasBuffsRemovedOnDamage)
		{
			if (hasBuffs())
			{
				for (BuffInfo info : _buffs)
				{
					if ((info != null) && info.getSkill().isRemovedOnDamage())
					{
						stopAndRemove(true, info, _buffs);
					}
				}
				
				update = true;
			}
			
			if (hasDances())
			{
				for (BuffInfo info : _dances)
				{
					if ((info != null) && info.getSkill().isRemovedOnDamage())
					{
						stopAndRemove(true, info, _dances);
					}
				}
				
				update = true;
			}
			
			if (hasToggles())
			{
				for (BuffInfo info : _toggles)
				{
					if ((info != null) && info.getSkill().isRemovedOnDamage())
					{
						stopAndRemove(true, info, _toggles);
					}
				}
				
				update = true;
			}
		}
		
		if (_hasDebuffsRemovedOnDamage && hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if ((info != null) && info.getSkill().isRemovedOnDamage())
				{
					stopAndRemove(true, info, _debuffs);
				}
			}
			
			update = true;
		}
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		if (partyOnly)
		{
			_partyOnly = true;
		}
		
		// Update effect flags and icons.
		updateEffectList(true);
	}
	
	/**
	 * Verify if this effect list is empty.<br>
	 * Prevents initialization.
	 * @return {@code true} if this effect list contains any skills
	 */
	public boolean isEmpty()
	{
		return !hasBuffs() && !hasDances() && !hasDebuffs() && !hasToggles();
	}
	
	/**
	 * Verify if this effect list has buffs skills.<br>
	 * Prevents initialization.
	 * @return {@code true} if {@link #_buffs} is not {@code null} and is not empty
	 */
	public boolean hasBuffs()
	{
		return !_buffs.isEmpty();
	}
	
	/**
	 * Verify if this effect list has dance/song skills.<br>
	 * Prevents initialization.
	 * @return {@code true} if {@link #_dances} is not {@code null} and is not empty
	 */
	public boolean hasDances()
	{
		return !_dances.isEmpty();
	}
	
	/**
	 * Verify if this effect list has toggle skills.<br>
	 * Prevents initialization.
	 * @return {@code true} if {@link #_toggles} is not {@code null} and is not empty
	 */
	public boolean hasToggles()
	{
		return !_toggles.isEmpty();
	}
	
	/**
	 * Verify if this effect list has debuffs skills.<br>
	 * Prevents initialization.
	 * @return {@code true} if {@link #_debuffs} is not {@code null} and is not empty
	 */
	public boolean hasDebuffs()
	{
		return !_debuffs.isEmpty();
	}
	
	/**
	 * Verify if this effect list has passive skills.<br>
	 * Prevents initialization.
	 * @return {@code true} if {@link #_passives} is not {@code null} and is not empty
	 */
	public boolean hasPassives()
	{
		return !_passives.isEmpty();
	}
	
	/**
	 * Executes a procedure for all effects.<br>
	 * Prevents initialization.
	 * @param function the function to execute
	 * @param dances if {@code true} dances/songs will be included
	 */
	public void forEach(Function<BuffInfo, Boolean> function, boolean dances)
	{
		boolean update = false;
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				update |= function.apply(info);
			}
		}
		
		if (dances && hasDances())
		{
			for (BuffInfo info : _dances)
			{
				update |= function.apply(info);
			}
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				update |= function.apply(info);
			}
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				update |= function.apply(info);
			}
		}
		
		// Update effect flags and icons.
		updateEffectList(update);
	}
	
	/**
	 * Removes a set of effects from this effect list.
	 * @param type determines the system message that will be sent.
	 * @param info the effects to remove
	 */
	public void remove(SkillFinishType type, BuffInfo info)
	{
		if (info == null)
		{
			return;
		}
		
		// Remove the effect from creature effects.
		stopAndRemove(true, type, info, getEffectList(info.getSkill()));
		
		// Update effect flags and icons.
		updateEffectList(true);
	}
	
	/**
	 * Adds a set of effects to this effect list.
	 * @param info the buff info
	 */
	public void add(BuffInfo info)
	{
		if (info == null)
		{
			return;
		}
		
		// Support for blocked buff slots.
		final Skill skill = info.getSkill();
		if (_blockedAbnormalTypes.contains(skill.getAbnormalType()))
		{
			return;
		}
		
		// Passive effects are treated specially
		if (skill.isPassive())
		{
			// Passive effects don't need stack type!
			if (!skill.getAbnormalType().isNone())
			{
				LOGGER.warning("Passive " + skill + " with abnormal type: " + skill.getAbnormalType() + "!");
			}
			
			// Check for passive skill conditions.
			if (!skill.checkCondition(info.getEffector(), info.getEffected(), false))
			{
				return;
			}
			
			// Puts the effects in the list.
			for (BuffInfo b : _passives)
			{
				// Removes the old stats from the creature if the skill was present.
				if (b.getSkill().getId() == skill.getId())
				{
					b.setInUse(false);
					b.removeStats();
					_passives.remove(b);
				}
			}
			
			_passives.add(info);
			
			// Initialize effects.
			info.initializeEffects();
			return;
		}
		
		// Prevent adding and initializing buffs/effects on dead creatures.
		if (info.getEffected().isDead())
		{
			return;
		}
		
		// The old effect is removed using Map#remove(key) instead of Map#put(key, value) (that would be the wisest choice),
		// Because order matters and put method would insert in the same place it was before, instead of, at the end of the effect list
		// Where new buff should be placed
		if (skill.getAbnormalType().isNone())
		{
			stopSkillEffects(SkillFinishType.NORMAL, skill);
		}
		else // Verify stacked skills.
		{
			synchronized (this)
			{
				if (_stackedEffects.containsKey(skill.getAbnormalType()))
				{
					BuffInfo stackedInfo = _stackedEffects.get(skill.getAbnormalType());
					
					// Skills are only replaced if the incoming buff has greater or equal abnormal level.
					if ((stackedInfo != null) && (skill.getAbnormalLevel() >= stackedInfo.getSkill().getAbnormalLevel()))
					{
						// If it is an herb, set as not in use the lesser buff.
						// Effect will be present in the effect list.
						// Effects stats are removed and onActionTime() is not called.
						// But finish task continues to run, and ticks as well.
						if (skill.isAbnormalInstant())
						{
							if (stackedInfo.getSkill().isAbnormalInstant())
							{
								stopSkillEffects(SkillFinishType.NORMAL, skill.getAbnormalType());
								stackedInfo = _stackedEffects.get(skill.getAbnormalType());
							}
							
							if (stackedInfo != null)
							{
								stackedInfo.setInUse(false);
								
								// Remove stats
								stackedInfo.removeStats();
								_hiddenBuffs.incrementAndGet();
							}
						}
						else // Remove buff that will stack with the abnormal type.
						{
							if (stackedInfo.getSkill().isAbnormalInstant())
							{
								stopSkillEffects(SkillFinishType.NORMAL, skill.getAbnormalType());
							}
							
							stopSkillEffects(SkillFinishType.NORMAL, skill.getAbnormalType());
						}
					}
					else // If the new buff is a lesser buff, then don't add it.
					{
						return;
					}
				}
				
				_stackedEffects.put(skill.getAbnormalType(), info);
			}
		}
		
		// Select the map that holds the effects related to this skill.
		final Queue<BuffInfo> effects = getEffectList(skill);
		
		// Remove first buff when buff list is full.
		if (!skill.isDebuff() && !skill.isToggle() && !skill.is7Signs() && !doesStack(skill))
		{
			final int totalBuffs = getBuffCount() + getDanceCount();
			final int maxTotalBuffs = _owner.getStat().getMaxBuffCount();
			final int maxDances = PlayerConfig.DANCES_MAX_AMOUNT;
			final int currentDances = getDanceCount();
			if (((totalBuffs >= maxTotalBuffs) && !skill.isHealingPotionSkill()) || (skill.isDance() && (currentDances >= maxDances)))
			{
				// New list counting buff and dances/song all together.
				final List<BuffInfo> allEffects = new ArrayList<>();
				allEffects.addAll(_buffs);
				allEffects.addAll(_dances);
				
				BuffInfo toRemove = null;
				for (BuffInfo buffInfo : allEffects)
				{
					if (!buffInfo.isInUse())
					{
						continue;
					}
					
					// Check if limit of dance/song is exceeded.
					if (skill.isDance() && (currentDances >= maxDances) && !buffInfo.getSkill().isDance())
					{
						continue;
					}
					
					// Check the oldest buff to be removed.
					if ((toRemove == null) || (buffInfo.getPeriodStartTicks() < toRemove.getPeriodStartTicks()))
					{
						toRemove = buffInfo;
					}
				}
				
				if (toRemove != null)
				{
					final Queue<BuffInfo> list = getEffectList(toRemove.getSkill());
					if (list != null)
					{
						list.remove(toRemove);
						stopAndRemove(true, toRemove, list);
					}
				}
			}
		}
		
		// After removing old buff (same ID) or stacked buff (same abnormal type),
		// Add the buff to the end of the effect list.
		effects.add(info);
		
		// Initialize effects.
		info.initializeEffects();
		
		// Update effect flags and icons.
		updateEffectList(true);
	}
	
	/**
	 * Update effect icons.<br>
	 * Prevents initialization.
	 */
	private void updateEffectIcons()
	{
		if ((_owner == null) || !_owner.isPlayable())
		{
			return;
		}
		
		updateEffectFlags();
		
		if (_updateEffectIconTask == null)
		{
			_updateEffectIconTask = ThreadPool.schedule(() ->
			{
				AbnormalStatusUpdate asu = null;
				PartySpelled ps = null;
				ExOlympiadSpelledInfo os = null;
				boolean isSummon = false;
				
				if (_owner.isPlayer())
				{
					if (_partyOnly)
					{
						_partyOnly = false;
					}
					else
					{
						asu = new AbnormalStatusUpdate();
					}
					
					if (_owner.isInParty())
					{
						ps = new PartySpelled(_owner);
					}
					
					final Player player = _owner.asPlayer();
					if (player.isInOlympiadMode() && player.isOlympiadStart())
					{
						os = new ExOlympiadSpelledInfo(player);
					}
				}
				else if (_owner.isSummon())
				{
					isSummon = true;
					ps = new PartySpelled(_owner);
				}
				
				// Buffs.
				if (hasBuffs())
				{
					for (BuffInfo info : _buffs)
					{
						if (info.getSkill().isHealingPotionSkill())
						{
							shortBuffStatusUpdate(info);
						}
						else
						{
							addIcon(info, asu, ps, os, isSummon);
						}
					}
				}
				
				// Songs and dances.
				if (hasDances())
				{
					for (BuffInfo info : _dances)
					{
						addIcon(info, asu, ps, os, isSummon);
					}
				}
				
				// Toggles.
				if (hasToggles())
				{
					for (BuffInfo info : _toggles)
					{
						addIcon(info, asu, ps, os, isSummon);
					}
				}
				
				// Debuffs.
				if (hasDebuffs())
				{
					for (BuffInfo info : _debuffs)
					{
						addIcon(info, asu, ps, os, isSummon);
					}
				}
				
				if (asu != null)
				{
					_owner.sendPacket(asu);
				}
				
				if (ps != null)
				{
					if (_owner.isSummon())
					{
						final Player summonOwner = _owner.asSummon().getOwner();
						if (summonOwner != null)
						{
							summonOwner.sendPacket(ps);
						}
					}
					else if (_owner.isPlayer() && _owner.isInParty())
					{
						_owner.getParty().broadcastPacket(ps);
					}
				}
				
				if (os != null)
				{
					final List<Player> specs = Olympiad.getInstance().getSpectators(_owner.asPlayer().getOlympiadGameId());
					if ((specs != null) && !specs.isEmpty())
					{
						for (Player spec : specs)
						{
							if (spec != null)
							{
								spec.sendPacket(os);
							}
						}
					}
				}
				
				_updateEffectIconTask = null;
			}, 300);
		}
	}
	
	private void addIcon(BuffInfo info, AbnormalStatusUpdate asu, PartySpelled ps, ExOlympiadSpelledInfo os, boolean isSummon)
	{
		// Avoid null and not in use buffs.
		if ((info == null) || !info.isInUse())
		{
			return;
		}
		
		final Skill skill = info.getSkill();
		if (asu != null)
		{
			asu.addSkill(info);
		}
		
		if ((ps != null) && (isSummon || !skill.isToggle()))
		{
			ps.addSkill(info);
		}
		
		if (os != null)
		{
			os.addSkill(info);
		}
	}
	
	/**
	 * Wrapper to update abnormal icons and effect flags.
	 * @param update if {@code true} performs an update
	 */
	private void updateEffectList(boolean update)
	{
		if (update)
		{
			updateEffectIcons();
			computeEffectFlags();
		}
	}
	
	/**
	 * Updates effect flags.<br>
	 * TODO: Rework it to update in real time (add/remove/stop/activate/deactivate operations) and avoid iterations.
	 */
	private void updateEffectFlags()
	{
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				if (info == null)
				{
					continue;
				}
				
				if (info.getSkill().isRemovedOnAnyActionExceptMove())
				{
					_hasBuffsRemovedOnAnyAction = true;
				}
				
				if (info.getSkill().isRemovedOnDamage())
				{
					_hasBuffsRemovedOnDamage = true;
				}
			}
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				if (info == null)
				{
					continue;
				}
				
				if (info.getSkill().isRemovedOnAnyActionExceptMove())
				{
					_hasBuffsRemovedOnAnyAction = true;
				}
				
				if (info.getSkill().isRemovedOnDamage())
				{
					_hasBuffsRemovedOnDamage = true;
				}
			}
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if ((info != null) && info.getSkill().isRemovedOnDamage())
				{
					_hasDebuffsRemovedOnDamage = true;
				}
			}
		}
	}
	
	/**
	 * Recalculate effect bits flag.<br>
	 * TODO: Rework to update in real time and avoid iterations.
	 */
	private void computeEffectFlags()
	{
		int flags = 0;
		if (hasBuffs())
		{
			for (BuffInfo info : _buffs)
			{
				if (info != null)
				{
					for (AbstractEffect e : info.getEffects())
					{
						flags |= e.getEffectFlags();
					}
				}
			}
		}
		
		if (hasDebuffs())
		{
			for (BuffInfo info : _debuffs)
			{
				if (info != null)
				{
					for (AbstractEffect e : info.getEffects())
					{
						flags |= e.getEffectFlags();
					}
				}
			}
		}
		
		if (hasDances())
		{
			for (BuffInfo info : _dances)
			{
				if (info != null)
				{
					for (AbstractEffect e : info.getEffects())
					{
						flags |= e.getEffectFlags();
					}
				}
			}
		}
		
		if (hasToggles())
		{
			for (BuffInfo info : _toggles)
			{
				if (info != null)
				{
					for (AbstractEffect e : info.getEffects())
					{
						flags |= e.getEffectFlags();
					}
				}
			}
		}
		
		_effectFlags = flags;
	}
	
	/**
	 * Check if target is affected with special buff
	 * @param flag of special buff
	 * @return boolean true if affected
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return (_effectFlags & flag.getMask()) != 0;
	}
	
	/**
	 * Check if target is affected by AbnormalType.
	 * @param type the AbnormalType
	 * @return boolean true if affected
	 */
	public boolean isAffectedByAbnormalType(AbnormalType type)
	{
		return _stackedEffects.containsKey(type);
	}
}
