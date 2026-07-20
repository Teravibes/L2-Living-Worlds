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
package org.l2jmobius.gameserver.model;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.instance.Fence;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.taskmanagers.RandomAnimationTaskManager;

public class WorldRegion
{
	/** Set containing visible objects in this world region. */
	private final Set<WorldObject> _visibleObjects = ConcurrentHashMap.newKeySet();
	/** Set containing doors in this world region. */
	private final Set<Door> _doors = ConcurrentHashMap.newKeySet();
	/** Set containing fences in this world region. */
	private final Set<Fence> _fences = ConcurrentHashMap.newKeySet();
	/** Array containing nearby regions forming this world region's effective area. */
	private WorldRegion[] _surroundingRegions;
	private final ConcurrentHashMap<WorldRegion, Boolean> _surroundingRegionCache = new ConcurrentHashMap<>();
	private final int _regionX;
	private final int _regionY;
	private final int _regionZ;
	private boolean _active = GeneralConfig.GRIDS_ALWAYS_ON;
	private ScheduledFuture<?> _neighborsTask = null;
	private final AtomicInteger _activeNeighbors = new AtomicInteger();
	
	public WorldRegion(int regionX, int regionY, int regionZ)
	{
		_regionX = regionX;
		_regionY = regionY;
		_regionZ = regionZ;
	}
	
	private void switchAI(boolean isOn)
	{
		if (_visibleObjects.isEmpty())
		{
			return;
		}
		
		if (!isOn)
		{
			for (WorldObject wo : _visibleObjects)
			{
				if (wo.isAttackable())
				{
					final Attackable mob = wo.asAttackable();
					
					// Set target to null and cancel attack or cast.
					mob.setTarget(null);
					
					// Stop movement.
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the Creature.
					mob.stopAllEffects();
					
					mob.clearAggroList();
					mob.getAttackByList().clear();
					
					// Teleport to spawn when too far away.
					final Spawn spawn = mob.getSpawn();
					if ((spawn != null) && (mob.calculateDistance2D(spawn) > NpcConfig.MAX_DRIFT_RANGE))
					{
						mob.teleToLocation(spawn);
					}
					
					// Stop the AI tasks.
					if (mob.hasAI())
					{
						mob.getAI().setIntention(Intention.IDLE);
						mob.getAI().stopAITask();
					}
					
					// Stop attack task.
					mob.abortAttack();
					
					RandomAnimationTaskManager.getInstance().remove(mob);
				}
				else if (wo.isNpc())
				{
					RandomAnimationTaskManager.getInstance().remove(wo.asNpc());
				}
			}
		}
		else
		{
			for (WorldObject wo : _visibleObjects)
			{
				if (wo.isAttackable())
				{
					// Start HP/MP/CP regeneration task.
					wo.asAttackable().getStatus().startHpMpRegeneration();
					RandomAnimationTaskManager.getInstance().add(wo.asNpc());
				}
				else if (wo.isNpc())
				{
					RandomAnimationTaskManager.getInstance().add(wo.asNpc());
				}
			}
		}
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	public void incrementActiveNeighbors()
	{
		_activeNeighbors.incrementAndGet();
	}
	
	public void decrementActiveNeighbors()
	{
		_activeNeighbors.decrementAndGet();
	}
	
	public boolean areNeighborsActive()
	{
		return GeneralConfig.GRIDS_ALWAYS_ON || (_activeNeighbors.get() > 0);
	}
	
	public boolean areNeighborsEmpty()
	{
		for (int i = 0; i < _surroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = _surroundingRegions[i];
			if (worldRegion.isActive())
			{
				final Collection<WorldObject> regionObjects = worldRegion.getVisibleObjects();
				if (regionObjects.isEmpty())
				{
					continue;
				}
				
				for (WorldObject wo : regionObjects)
				{
					if ((wo != null) && wo.isPlayable())
					{
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * This function turns this region's AI ON or OFF.
	 * @param value
	 */
	public synchronized void setActive(boolean value)
	{
		if (_active == value)
		{
			return;
		}
		
		_active = value;
		
		if (value)
		{
			for (int i = 0; i < _surroundingRegions.length; i++)
			{
				_surroundingRegions[i].incrementActiveNeighbors();
			}
		}
		else
		{
			for (int i = 0; i < _surroundingRegions.length; i++)
			{
				_surroundingRegions[i].decrementActiveNeighbors();
			}
		}
		
		// Turn the AI on or off to match the region's activation.
		switchAI(value);
	}
	
	/**
	 * Immediately sets self as active and starts a timer to set neighbors as active this timer is to avoid turning on neighbors in the case when a person just teleported into a region and then teleported out immediately...there is no reason to activate all the neighbors in that case.
	 */
	private void startActivation()
	{
		// First set self to active and do self-tasks...
		setActive(true);
		
		// If the timer to deactivate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// Then, set a timer to activate the neighbors.
			_neighborsTask = ThreadPool.schedule(() ->
			{
				for (int i = 0; i < _surroundingRegions.length; i++)
				{
					_surroundingRegions[i].setActive(true);
				}
			}, 1000 * GeneralConfig.GRID_NEIGHBOR_TURNON_TIME);
		}
	}
	
	/**
	 * starts a timer to set neighbors (including self) as inactive this timer is to avoid turning off neighbors in the case when a person just moved out of a region that he may very soon return to. There is no reason to turn self & neighbors off in that case.
	 */
	private void startDeactivation()
	{
		// If the timer to activate neighbors is running, cancel it.
		synchronized (this)
		{
			if (_neighborsTask != null)
			{
				_neighborsTask.cancel(true);
				_neighborsTask = null;
			}
			
			// Start a timer to "suggest" a deactivate to self and neighbors.
			// Suggest means: first check if a neighbor has Players in it. If not, deactivate.
			_neighborsTask = ThreadPool.schedule(() ->
			{
				for (int i = 0; i < _surroundingRegions.length; i++)
				{
					final WorldRegion worldRegion = _surroundingRegions[i];
					if (worldRegion.areNeighborsEmpty())
					{
						worldRegion.setActive(false);
					}
				}
			}, 1000 * GeneralConfig.GRID_NEIGHBOR_TURNOFF_TIME);
		}
	}
	
	/**
	 * Add the WorldObject in the WorldObjectHashSet(WorldObject) _visibleObjects containing WorldObject visible in this WorldRegion<br>
	 * If WorldObject is a Player, Add the Player in the HashSet(Player) _allPlayable containing Player of all player in game in this WorldRegion
	 * @param object
	 */
	public synchronized void addVisibleObject(WorldObject object)
	{
		if (object == null)
		{
			return;
		}
		
		_visibleObjects.add(object);
		
		if (object.isDoor())
		{
			for (int i = 0; i < _surroundingRegions.length; i++)
			{
				_surroundingRegions[i].addDoor(object.asDoor());
			}
		}
		else if (object.isFence())
		{
			for (int i = 0; i < _surroundingRegions.length; i++)
			{
				_surroundingRegions[i].addFence((Fence) object);
			}
		}
		
		// If this is the first player to enter the region, activate self and neighbors.
		if (object.isPlayable() && !_active && !GeneralConfig.GRIDS_ALWAYS_ON)
		{
			startActivation();
		}
	}
	
	/**
	 * Remove the WorldObject from the WorldObjectHashSet(WorldObject) _visibleObjects in this WorldRegion. If WorldObject is a Player, remove it from the HashSet(Player) _allPlayable of this WorldRegion
	 * @param object
	 */
	public synchronized void removeVisibleObject(WorldObject object)
	{
		if (object == null)
		{
			return;
		}
		
		if (_visibleObjects.isEmpty())
		{
			return;
		}
		
		_visibleObjects.remove(object);
		
		if (object.isDoor())
		{
			for (int i = 0; i < _surroundingRegions.length; i++)
			{
				_surroundingRegions[i].removeDoor(object.asDoor());
			}
		}
		else if (object.isFence())
		{
			for (int i = 0; i < _surroundingRegions.length; i++)
			{
				_surroundingRegions[i].removeFence((Fence) object);
			}
		}
		
		if (object.isPlayable() && areNeighborsEmpty() && !GeneralConfig.GRIDS_ALWAYS_ON)
		{
			startDeactivation();
		}
	}
	
	public Collection<WorldObject> getVisibleObjects()
	{
		return _visibleObjects;
	}
	
	public void addDoor(Door door)
	{
		_doors.add(door);
	}
	
	private void removeDoor(Door door)
	{
		_doors.remove(door);
	}
	
	public Collection<Door> getDoors()
	{
		return _doors;
	}
	
	public void addFence(Fence fence)
	{
		_fences.add(fence);
	}
	
	private void removeFence(Fence fence)
	{
		_fences.remove(fence);
	}
	
	public Collection<Fence> getFences()
	{
		return _fences;
	}
	
	public void setSurroundingRegions(WorldRegion[] regions)
	{
		_surroundingRegions = regions;
		
		// Make sure that this region is always the first region to improve bulk operations when this region should be updated first.
		for (int i = 0; i < _surroundingRegions.length; i++)
		{
			if (_surroundingRegions[i] == this)
			{
				final WorldRegion first = _surroundingRegions[0];
				_surroundingRegions[0] = this;
				_surroundingRegions[i] = first;
			}
		}
	}
	
	public WorldRegion[] getSurroundingRegions()
	{
		return _surroundingRegions;
	}
	
	/**
	 * Checks if the given region is a surrounding region of this region.
	 * @param region region to check
	 * @return true if the region is surrounding this region
	 */
	public boolean isSurroundingRegion(WorldRegion region)
	{
		if (region == null)
		{
			return false;
		}
		
		return _surroundingRegionCache.computeIfAbsent(region, r -> (_regionX >= (r.getRegionX() - 1)) && (_regionX <= (r.getRegionX() + 1)) && (_regionY >= (r.getRegionY() - 1)) && (_regionY <= (r.getRegionY() + 1)) && (_regionZ >= (r.getRegionZ() - 1)) && (_regionZ <= (r.getRegionZ() + 1)));
	}
	
	public int getRegionX()
	{
		return _regionX;
	}
	
	public int getRegionY()
	{
		return _regionY;
	}
	
	public int getRegionZ()
	{
		return _regionZ;
	}
	
	@Override
	public String toString()
	{
		return "(" + _regionX + ", " + _regionY + ", " + _regionZ + ")";
	}
}
