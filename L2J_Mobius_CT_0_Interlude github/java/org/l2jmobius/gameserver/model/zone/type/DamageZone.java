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
package org.l2jmobius.gameserver.model.zone.type;

import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * A damage zone
 * @author durgus
 */
public class DamageZone extends ZoneType
{
	private int _damageHPPerSec;
	private int _damageMPPerSec;
	
	private int _castleId;
	private Castle _castle;
	
	private int _startTask;
	private int _reuseTask;
	protected volatile Future<?> _task;
	
	public DamageZone(int id)
	{
		super(id);
		
		// Setup default damage
		_damageHPPerSec = 200;
		_damageMPPerSec = 0;
		
		// Setup default start / reuse time
		_startTask = 10;
		_reuseTask = 5000;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
		
		setTargetType(InstanceType.Playable); // default only playabale
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgHPSec"))
		{
			_damageHPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("dmgMPSec"))
		{
			_damageMPPerSec = Integer.parseInt(value);
		}
		else if (name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("initialDelay"))
		{
			_startTask = Integer.parseInt(value);
		}
		else if (name.equalsIgnoreCase("reuse"))
		{
			_reuseTask = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		Future<?> task = _task;
		if ((task == null) && ((_damageHPPerSec != 0) || (_damageMPPerSec != 0)))
		{
			final Player player = creature.asPlayer();
			final Castle castle = getCastle();
			if (castle != null) // Castle zone
			{
				if (!(castle.getSiege().isInProgress() && (player != null) && (player.getSiegeState() != 2))) // Siege and no defender
				{
					return;
				}
			}
			
			synchronized (this)
			{
				task = _task;
				if (task == null)
				{
					_task = task = ThreadPool.scheduleAtFixedRate(new ApplyDamage(), _startTask, _reuseTask);
				}
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (getCharactersInside().isEmpty() && (_task != null))
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	protected int getHPDamagePerSecond()
	{
		return _damageHPPerSec;
	}
	
	protected int getMPDamagePerSecond()
	{
		return _damageMPPerSec;
	}
	
	protected Castle getCastle()
	{
		if ((_castleId > 0) && (_castle == null))
		{
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		}
		
		return _castle;
	}
	
	private class ApplyDamage implements Runnable
	{
		private final Castle _castle;
		
		protected ApplyDamage()
		{
			_castle = getCastle();
		}
		
		@Override
		public void run()
		{
			if (getCharactersInside().isEmpty())
			{
				if (_task != null)
				{
					_task.cancel(false);
					_task = null;
				}
				return;
			}
			
			if (!isEnabled())
			{
				return;
			}
			
			boolean siege = false;
			if (_castle != null)
			{
				siege = _castle.getSiege().isInProgress();
				
				// Castle zones active only during siege.
				if (!siege)
				{
					_task.cancel(false);
					_task = null;
					return;
				}
			}
			
			for (Creature character : getCharactersInside())
			{
				if ((character != null) && character.isPlayer() && !character.isDead())
				{
					if (siege)
					{
						// During siege defenders not affected.
						final Player player = character.asPlayer();
						if ((player != null) && player.isInSiege() && (player.getSiegeState() == 2))
						{
							continue;
						}
					}
					
					final double multiplier = 1 + (character.calcStat(Stat.DAMAGE_ZONE_VULN, 0, null, null) / 100);
					if (getHPDamagePerSecond() != 0)
					{
						character.reduceCurrentHp(getHPDamagePerSecond() * multiplier, character, null);
					}
					
					if (getMPDamagePerSecond() != 0)
					{
						character.reduceCurrentMp(getMPDamagePerSecond() * multiplier);
					}
				}
			}
		}
	}
}
