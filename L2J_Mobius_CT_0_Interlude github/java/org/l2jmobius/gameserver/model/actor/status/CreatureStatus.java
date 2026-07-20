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
package org.l2jmobius.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.stats.Formulas;

public class CreatureStatus
{
	protected static final Logger LOGGER = Logger.getLogger(CreatureStatus.class.getName());
	
	private final Creature _creature;
	
	private double _currentHp = 0; // Current HP of the Creature
	private double _currentMp = 0; // Current MP of the Creature
	
	/** Array containing all clients that need to be notified about hp/mp updates of the Creature */
	private Set<Creature> _statusListener;
	
	private Future<?> _regTask;
	
	protected byte _flagsRegenActive = 0;
	
	protected static final byte REGEN_FLAG_CP = 4;
	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;
	
	public CreatureStatus(Creature creature)
	{
		_creature = creature;
	}
	
	/**
	 * Add the object to the list of Creature that must be informed of HP/MP updates of this Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Each Creature owns a list called <b>_statusListener</b> that contains all Player to inform of HP/MP updates.<br>
	 * Players who must be informed are players that target this Creature.<br>
	 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Target a PC or NPC</li>
	 * <ul>
	 * @param object Creature to add to the listener
	 */
	public void addStatusListener(Creature object)
	{
		if (object == _creature)
		{
			return;
		}
		
		getStatusListener().add(object);
	}
	
	/**
	 * Remove the object from the list of Creature that must be informed of HP/MP updates of this Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Each Creature owns a list called <b>_statusListener</b> that contains all Player to inform of HP/MP updates.<br>
	 * Players who must be informed are players that target this Creature.<br>
	 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.<br>
	 * <br>
	 * <b><u>Example of use </u>:</b>
	 * <ul>
	 * <li>Untarget a PC or NPC</li>
	 * </ul>
	 * @param object Creature to add to the listener
	 */
	public void removeStatusListener(Creature object)
	{
		getStatusListener().remove(object);
	}
	
	/**
	 * Return the list of Creature that must be informed of HP/MP updates of this Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Each Creature owns a list called <b>_statusListener</b> that contains all Player to inform of HP/MP updates.<br>
	 * Players who must be informed are players that target this Creature.<br>
	 * When a RegenTask is in progress sever just need to go through this list to send Server->Client packet StatusUpdate.
	 * @return The list of Creature to inform or null if empty
	 */
	public Set<Creature> getStatusListener()
	{
		if (_statusListener == null)
		{
			_statusListener = ConcurrentHashMap.newKeySet();
		}
		
		return _statusListener;
	}
	
	// place holder, only PcStatus has CP
	public void reduceCp(int value)
	{
	}
	
	/**
	 * Reduce the current HP of the Creature and launch the doDie Task if necessary.
	 * @param value
	 * @param attacker
	 */
	public void reduceHp(double value, Creature attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	public void reduceHp(double value, Creature attacker, boolean isHpConsumption)
	{
		reduceHp(value, attacker, true, false, isHpConsumption);
	}
	
	public void reduceHp(double value, Creature attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		if (_creature.isDead())
		{
			return;
		}
		
		// invul handling
		if (_creature.isInvul() && !(isDOT || isHPConsumption))
		{
			return;
		}
		
		if (attacker != null)
		{
			final Player attackerPlayer = attacker.asPlayer();
			if ((attackerPlayer != null) && attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
			{
				return;
			}
		}
		
		if (!isDOT && !isHPConsumption)
		{
			_creature.stopEffectsOnDamage(awake);
			if (_creature.isStunned() && (Rnd.get(10) == 0))
			{
				_creature.stopStunning(true);
			}
		}
		
		if (value > 0)
		{
			setCurrentHp(Math.max(_currentHp - value, 0));
		}
		
		if ((_creature.getCurrentHp() < 0.5) && _creature.isMortal()) // Die
		{
			_creature.abortAttack();
			_creature.abortCast();
			
			_creature.doDie(attacker);
		}
	}
	
	public void reduceMp(double value)
	{
		setCurrentMp(Math.max(_currentMp - value, 0));
	}
	
	/**
	 * Start the HP/MP/CP Regeneration task.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Calculate the regen task period</li>
	 * <li>Launch the HP/MP/CP Regeneration task with Medium priority</li>
	 * </ul>
	 */
	public synchronized void startHpMpRegeneration()
	{
		if ((_regTask == null) && !_creature.isDead())
		{
			// Get the Regeneration period
			final int period = Formulas.getRegeneratePeriod(_creature);
			
			// Create the HP/MP/CP Regeneration task
			_regTask = ThreadPool.scheduleAtFixedRate(this::doRegeneration, period, period);
		}
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Set the RegenActive flag to False</li>
	 * <li>Stop the HP/MP/CP Regeneration task</li>
	 * </ul>
	 */
	public synchronized void stopHpMpRegeneration()
	{
		if (_regTask != null)
		{
			// Stop the HP/MP/CP Regeneration task
			_regTask.cancel(false);
			_regTask = null;
			
			// Set the RegenActive flag to false
			_flagsRegenActive = 0;
		}
	}
	
	// place holder, only PcStatus has CP
	public double getCurrentCp()
	{
		return 0;
	}
	
	// place holder, only PcStatus has CP
	public void setCurrentCp(double newCp)
	{
	}
	
	public double getCurrentHp()
	{
		return _currentHp;
	}
	
	public void setCurrentHp(double newHp)
	{
		setCurrentHp(newHp, true);
	}
	
	/**
	 * Sets the current hp of this character.
	 * @param newHp the new hp
	 * @param broadcastPacket if true StatusUpdate packet will be broadcasted.
	 * @return @{code true} if hp was changed, @{code false} otherwise.
	 */
	public boolean setCurrentHp(double newHp, boolean broadcastPacket)
	{
		// Get the Max HP of the Creature
		final int currentHp = (int) _currentHp;
		final double maxHp = _creature.getMaxHp();
		
		// Tempfix for updating UserInfo when skills like Final Frenzy and Final Fortress conditions activate.
		final double thirtyPercent = maxHp * 0.3;
		final boolean updateUserInfo = _creature.isPlayer() && (((currentHp < thirtyPercent) && (newHp > thirtyPercent)) || ((currentHp > thirtyPercent) && (newHp < thirtyPercent)));
		
		synchronized (this)
		{
			if (_creature.isDead())
			{
				return false;
			}
			
			if (newHp >= maxHp)
			{
				// Set the RegenActive flag to false
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		final boolean hpWasChanged = currentHp != _currentHp;
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform
		if (hpWasChanged && broadcastPacket)
		{
			_creature.broadcastStatusUpdate();
		}
		
		if (updateUserInfo)
		{
			_creature.asPlayer().updateUserInfo();
		}
		
		return hpWasChanged;
	}
	
	public void setCurrentHpMp(double newHp, double newMp)
	{
		final boolean hpChanged = setCurrentHp(newHp, false);
		final boolean mpChanged = setCurrentMp(newMp, false);
		if (hpChanged || mpChanged)
		{
			_creature.broadcastStatusUpdate();
		}
	}
	
	public double getCurrentMp()
	{
		return _currentMp;
	}
	
	public void setCurrentMp(double newMp)
	{
		setCurrentMp(newMp, true);
	}
	
	/**
	 * Sets the current mp of this character.
	 * @param newMp the new mp
	 * @param broadcastPacket if true StatusUpdate packet will be broadcasted.
	 * @return @{code true} if mp was changed, @{code false} otherwise.
	 */
	public boolean setCurrentMp(double newMp, boolean broadcastPacket)
	{
		// Get the Max MP of the Creature
		final int currentMp = (int) _currentMp;
		final int maxMp = _creature.getMaxMp();
		
		synchronized (this)
		{
			if (_creature.isDead())
			{
				return false;
			}
			
			if (newMp >= maxMp)
			{
				// Set the RegenActive flag to false
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				// Set the RegenActive flag to true
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		final boolean mpWasChanged = currentMp != _currentMp;
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform
		if (mpWasChanged && broadcastPacket)
		{
			_creature.broadcastStatusUpdate();
		}
		
		return mpWasChanged;
	}
	
	protected void doRegeneration()
	{
		// Modify the current HP/MP of the Creature and broadcast Server->Client packet StatusUpdate
		if (!_creature.isDead() && ((_currentHp < _creature.getMaxRecoverableHp()) || (_currentMp < _creature.getMaxRecoverableMp())))
		{
			final double newHp = _currentHp + Formulas.calcHpRegen(_creature);
			final double newMp = _currentMp + Formulas.calcMpRegen(_creature);
			setCurrentHpMp(newHp, newMp);
		}
		else
		{
			stopHpMpRegeneration();
		}
	}
	
	public Creature getActiveChar()
	{
		return _creature;
	}
}
