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
package org.l2jmobius.gameserver.model.spawns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Auto Spawn handler.<br>
 * Allows spawning of a NPC object based on a timer.<br>
 * (From the official idea used for the Merchant and Blacksmith of Mammon)<br>
 * General Usage: - Call registerSpawn() with the parameters listed below.<br>
 * int npcId int[][] spawnPoints or specify NULL to add points later.<br>
 * int initialDelay (If < 0 = default value) int respawnDelay (If < 0 = default value)<br>
 * int despawnDelay (If < 0 = default value or if = 0, function disabled)<br>
 * spawnPoints is a standard two-dimensional int array containing X,Y and Z coordinates.<br>
 * The default respawn/despawn delays are currently every hour (as for Mammon on official servers).<br>
 * The resulting AutoSpawnInstance object represents the newly added spawn index.<br>
 * The internal methods of this object can be used to adjust random spawning, for instance a call to setRandomSpawn(1, true); would set the spawn at index 1 to be randomly rather than sequentially-based.<br>
 * Also they can be used to specify the number of NPC instances to spawn using setSpawnCount(), and broadcast a message to all users using setBroadcast().<br>
 * Random Spawning = OFF by default Broadcasting = OFF by default
 * @author Tempy
 */
public class AutoSpawnHandler
{
	protected static final Logger LOGGER = Logger.getLogger(AutoSpawnHandler.class.getName());
	
	private static final int DEFAULT_INITIAL_SPAWN = 30000; // 30 seconds after registration
	private static final int DEFAULT_RESPAWN = 3600000; // 1 hour in millisecs
	private static final int DEFAULT_DESPAWN = 3600000; // 1 hour in millisecs
	
	protected Map<Integer, AutoSpawnInstance> _registeredSpawns = new ConcurrentHashMap<>();
	protected Map<Integer, ScheduledFuture<?>> _runningSpawns = new ConcurrentHashMap<>();
	
	protected boolean _activeState = true;
	
	protected AutoSpawnHandler()
	{
		restoreSpawnData();
	}
	
	public int size()
	{
		return _registeredSpawns.size();
	}
	
	public void reload()
	{
		// unload
		unload();
		
		// create clean list
		_registeredSpawns.clear();
		_runningSpawns.clear();
		
		// load
		restoreSpawnData();
	}
	
	public void unload()
	{
		// stop all timers
		for (ScheduledFuture<?> sf : _runningSpawns.values())
		{
			if (sf != null)
			{
				sf.cancel(true);
			}
		}
		
		// unregister all registered spawns
		for (AutoSpawnInstance asi : _registeredSpawns.values())
		{
			if (asi != null)
			{
				removeSpawn(asi);
			}
		}
	}
	
	private void restoreSpawnData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM random_spawn ORDER BY groupId ASC");
			PreparedStatement ps = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId=?"))
		{
			// Restore spawn group data, then the location data.
			while (rs.next())
			{
				// Register random spawn group, set various options on the created spawn instance.
				final AutoSpawnInstance spawnInst = registerSpawn(rs.getInt("npcId"), rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay"));
				spawnInst.setSpawnCount(rs.getInt("count"));
				spawnInst.setBroadcast(Boolean.parseBoolean(rs.getString("broadcastSpawn")));
				spawnInst.setRandomSpawn(Boolean.parseBoolean(rs.getString("randomSpawn")));
				
				// Restore the spawn locations for this spawn group/instance.
				ps.setInt(1, rs.getInt("groupId"));
				try (ResultSet rs2 = ps.executeQuery())
				{
					ps.clearParameters();
					while (rs2.next())
					{
						// Add each location to the spawn group/instance.
						spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AutoSpawnHandler: Could not restore spawn data: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Registers a spawn with the given parameters with the spawner, and marks it as active.<br>
	 * Returns a AutoSpawnInstance containing info about the spawn.
	 * @param npcId
	 * @param spawnPoints
	 * @param initialDelay (If < 0 = default value)
	 * @param respawnDelay (If < 0 = default value)
	 * @param despawnDelay (If < 0 = default value or if = 0, function disabled)
	 * @return AutoSpawnInstance spawnInst
	 */
	public AutoSpawnInstance registerSpawn(int npcId, int[][] spawnPoints, int initialDelay, int respawnDelay, int despawnDelay)
	{
		final AutoSpawnInstance newSpawn = new AutoSpawnInstance(npcId, initialDelay < 0 ? DEFAULT_INITIAL_SPAWN : initialDelay, respawnDelay < 0 ? DEFAULT_RESPAWN : respawnDelay, despawnDelay < 0 ? DEFAULT_DESPAWN : despawnDelay);
		if (spawnPoints != null)
		{
			for (int[] spawnPoint : spawnPoints)
			{
				newSpawn.addSpawnLocation(spawnPoint);
			}
		}
		
		final int newId = IdManager.getInstance().getNextId();
		newSpawn._objectId = newId;
		_registeredSpawns.put(newId, newSpawn);
		
		setSpawnActive(newSpawn, true);
		return newSpawn;
	}
	
	/**
	 * Registers a spawn with the given parameters with the spawner, and marks it as active.<br>
	 * Returns a AutoSpawnInstance containing info about the spawn.<br>
	 * <b>Warning:</b> Spawn locations must be specified separately using addSpawnLocation().
	 * @param npcId
	 * @param initialDelay (If < 0 = default value)
	 * @param respawnDelay (If < 0 = default value)
	 * @param despawnDelay (If < 0 = default value or if = 0, function disabled)
	 * @return AutoSpawnInstance spawnInst
	 */
	public AutoSpawnInstance registerSpawn(int npcId, int initialDelay, int respawnDelay, int despawnDelay)
	{
		return registerSpawn(npcId, null, initialDelay, respawnDelay, despawnDelay);
	}
	
	/**
	 * Remove a registered spawn from the list, specified by the given spawn instance.
	 * @param spawnInst
	 * @return boolean removedSuccessfully
	 */
	public boolean removeSpawn(AutoSpawnInstance spawnInst)
	{
		if (!isSpawnRegistered(spawnInst))
		{
			return false;
		}
		
		try
		{
			// Try to remove from the list of registered spawns if it exists.
			_registeredSpawns.remove(spawnInst.getId());
			
			// Cancel the currently associated running scheduled task.
			final ScheduledFuture<?> task = _runningSpawns.remove(spawnInst._objectId);
			if (task != null)
			{
				task.cancel(false);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AutoSpawnHandler: Could not auto spawn for NPC ID " + spawnInst._npcId + " (Object ID = " + spawnInst._objectId + "): " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Remove a registered spawn from the list, specified by the given spawn object ID.
	 * @param objectId
	 */
	public void removeSpawn(int objectId)
	{
		removeSpawn(_registeredSpawns.get(objectId));
	}
	
	/**
	 * Sets the active state of the specified spawn.
	 * @param spawnInst
	 * @param isActive
	 */
	public void setSpawnActive(AutoSpawnInstance spawnInst, boolean isActive)
	{
		if (spawnInst == null)
		{
			return;
		}
		
		final int objectId = spawnInst._objectId;
		if (!isSpawnRegistered(objectId))
		{
			return;
		}
		
		ScheduledFuture<?> spawnTask = null;
		if (isActive)
		{
			final AutoSpawner rs = new AutoSpawner(objectId);
			spawnTask = spawnInst._desDelay > 0 ? ThreadPool.scheduleAtFixedRate(rs, spawnInst._initDelay, spawnInst._resDelay) : ThreadPool.schedule(rs, spawnInst._initDelay);
			_runningSpawns.put(objectId, spawnTask);
		}
		else
		{
			final AutoDespawner rd = new AutoDespawner(objectId);
			spawnTask = _runningSpawns.remove(objectId);
			if (spawnTask != null)
			{
				spawnTask.cancel(false);
			}
			
			ThreadPool.schedule(rd, 0);
		}
		
		spawnInst.setSpawnActive(isActive);
	}
	
	/**
	 * Sets the active state of all auto spawn instances to that specified, and cancels the scheduled spawn task if necessary.
	 * @param isActive
	 */
	public void setAllActive(boolean isActive)
	{
		if (_activeState == isActive)
		{
			return;
		}
		
		for (AutoSpawnInstance spawnInst : _registeredSpawns.values())
		{
			setSpawnActive(spawnInst, isActive);
		}
		
		_activeState = isActive;
	}
	
	/**
	 * Returns the number of milliseconds until the next occurrence of the given spawn.
	 * @param spawnInst
	 * @return
	 */
	public long getTimeToNextSpawn(AutoSpawnInstance spawnInst)
	{
		final int objectId = spawnInst.getObjectId();
		return !isSpawnRegistered(objectId) ? -1 : _runningSpawns.containsKey(objectId) ? _runningSpawns.get(objectId).getDelay(TimeUnit.MILLISECONDS) : 0;
	}
	
	/**
	 * Attempts to return the AutoSpawnInstance associated with the given NPC or Object ID type.<br>
	 * Note: If isObjectId == false, returns first instance for the specified NPC ID.
	 * @param id
	 * @param isObjectId
	 * @return AutoSpawnInstance spawnInst
	 */
	public AutoSpawnInstance getAutoSpawnInstance(int id, boolean isObjectId)
	{
		if (isObjectId)
		{
			if (isSpawnRegistered(id))
			{
				return _registeredSpawns.get(id);
			}
		}
		else
		{
			for (AutoSpawnInstance spawnInst : _registeredSpawns.values())
			{
				if (spawnInst.getId() == id)
				{
					return spawnInst;
				}
			}
		}
		
		return null;
	}
	
	public List<AutoSpawnInstance> getAutoSpawnInstances(int npcId)
	{
		final List<AutoSpawnInstance> result = new LinkedList<>();
		for (AutoSpawnInstance spawnInst : _registeredSpawns.values())
		{
			if (spawnInst.getId() == npcId)
			{
				result.add(spawnInst);
			}
		}
		
		return result;
	}
	
	/**
	 * Tests if the specified object ID is assigned to an auto spawn.
	 * @param objectId
	 * @return isAssigned
	 */
	public boolean isSpawnRegistered(int objectId)
	{
		return _registeredSpawns.containsKey(objectId);
	}
	
	/**
	 * Tests if the specified spawn instance is assigned to an auto spawn.
	 * @param spawnInst
	 * @return boolean isAssigned
	 */
	public boolean isSpawnRegistered(AutoSpawnInstance spawnInst)
	{
		return _registeredSpawns.containsValue(spawnInst);
	}
	
	/**
	 * AutoSpawner class<br>
	 * This handles the main spawn task for an auto spawn instance, and initializes a despawner if required.
	 * @author Tempy
	 */
	private class AutoSpawner implements Runnable
	{
		private final int _objectId;
		
		protected AutoSpawner(int objectId)
		{
			_objectId = objectId;
		}
		
		@Override
		public void run()
		{
			try
			{
				// Retrieve the required spawn instance for this spawn task.
				final AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);
				
				// If the spawn is not scheduled to be active, cancel the spawn task.
				if (!spawnInst.isSpawnActive())
				{
					return;
				}
				
				final List<Location> locationList = spawnInst.getLocationList();
				
				// If there are no set co-ordinates, cancel the spawn task.
				if (locationList.isEmpty())
				{
					LOGGER.info("AutoSpawnHandler: No location co-ords specified for spawn instance (Object ID = " + _objectId + ").");
					return;
				}
				
				final int locationCount = locationList.size();
				int locationIndex = Rnd.get(locationCount);
				
				// If random spawning is disabled, the spawn at the next set of co-ordinates after the last.
				// If the index is greater than the number of possible spawns, reset the counter to zero.
				if (!spawnInst.isRandomSpawn())
				{
					locationIndex = spawnInst._lastLocIndex + 1;
					if (locationIndex == locationCount)
					{
						locationIndex = 0;
					}
					
					spawnInst._lastLocIndex = locationIndex;
				}
				
				// Set the X, Y and Z co-ordinates, where this spawn will take place.
				final int x = locationList.get(locationIndex).getX();
				final int y = locationList.get(locationIndex).getY();
				final int z = locationList.get(locationIndex).getZ();
				final int heading = locationList.get(locationIndex).getHeading();
				final Spawn newSpawn = new Spawn(spawnInst.getId());
				newSpawn.setXYZ(x, y, z);
				if (heading != -1)
				{
					newSpawn.setHeading(heading);
				}
				
				newSpawn.setAmount(spawnInst.getSpawnCount());
				if (spawnInst._desDelay == 0)
				{
					newSpawn.setRespawnDelay(spawnInst._resDelay);
				}
				
				// Add the new spawn information to the spawn table, but do not store it.
				SpawnTable.getInstance().addSpawn(newSpawn);
				Npc npcInst = null;
				if (spawnInst._spawnCount == 1)
				{
					npcInst = newSpawn.doSpawn();
					npcInst.setXYZ(npcInst.getX(), npcInst.getY(), npcInst.getZ());
					spawnInst.addNpc(npcInst);
				}
				else
				{
					for (int i = 0; i < spawnInst._spawnCount; i++)
					{
						npcInst = newSpawn.doSpawn();
						
						// To prevent spawning of more than one NPC in the exact same spot, move it slightly by a small random offset.
						npcInst.setXYZ(npcInst.getX() + Rnd.get(50), npcInst.getY() + Rnd.get(50), npcInst.getZ());
						
						// Add the NPC instance to the list of managed instances.
						spawnInst.addNpc(npcInst);
					}
				}
				
				newSpawn.stopRespawn();
				
				if (npcInst != null)
				{
					if (spawnInst.isRandomSpawn())
					{
						while (!World.getInstance().getVisibleObjectsInRange(npcInst, Npc.class, 5).isEmpty())
						{
							// LOGGER.log(Level.INFO, "AutoSpawnHandler: Random spawn location " + npcInst.getLocation() + " for " + npcInst + " is occupied. Teleporting...");
							npcInst.teleToLocation(locationList.get(Rnd.get(locationList.size())));
						}
					}
					
					if (spawnInst.isBroadcasting())
					{
						Broadcast.toAllOnlinePlayers("The " + npcInst.getName() + " has spawned near " + MapRegionData.getInstance().getClosestTownName(npcInst) + "!");
					}
				}
				
				// If there is no despawn time, do not create a despawn task.
				if (spawnInst.getDespawnDelay() > 0)
				{
					ThreadPool.schedule(new AutoDespawner(_objectId), spawnInst.getDespawnDelay() - 1000);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "AutoSpawnHandler: An error occurred while initializing spawn instance (Object ID = " + _objectId + "): " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * AutoDespawner Class<br>
	 * Simply used as a secondary class for despawning an auto spawn instance.
	 * @author Tempy
	 */
	private class AutoDespawner implements Runnable
	{
		private final int _objectId;
		
		protected AutoDespawner(int objectId)
		{
			_objectId = objectId;
		}
		
		@Override
		public void run()
		{
			try
			{
				final AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);
				if (spawnInst == null)
				{
					LOGGER.info("AutoSpawnHandler: No spawn registered for object ID = " + _objectId + ".");
					return;
				}
				
				for (Npc npcInst : spawnInst.getNpcList())
				{
					if (npcInst == null)
					{
						continue;
					}
					
					npcInst.deleteMe();
					SpawnTable.getInstance().removeSpawn(npcInst.getSpawn());
					spawnInst.removeNpc(npcInst);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "AutoSpawnHandler: An error occurred while despawning spawn (Object ID = " + _objectId + "): " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * AutoSpawnInstance Class<br>
	 * Stores information about a registered auto spawn.
	 * @author Tempy
	 */
	public static class AutoSpawnInstance
	{
		protected int _objectId;
		
		protected int _spawnIndex;
		
		protected int _npcId;
		
		protected int _initDelay;
		
		protected int _resDelay;
		
		protected int _desDelay;
		
		protected int _spawnCount = 1;
		
		protected int _lastLocIndex = -1;
		
		private final Queue<Npc> _npcList = new ConcurrentLinkedQueue<>();
		
		private final List<Location> _locList = new CopyOnWriteArrayList<>();
		
		private boolean _spawnActive;
		
		private boolean _randomSpawn = false;
		
		private boolean _broadcastAnnouncement = false;
		
		protected AutoSpawnInstance(int npcId, int initDelay, int respawnDelay, int despawnDelay)
		{
			_npcId = npcId;
			_initDelay = initDelay;
			_resDelay = respawnDelay;
			_desDelay = despawnDelay;
		}
		
		protected void setSpawnActive(boolean activeValue)
		{
			_spawnActive = activeValue;
		}
		
		protected boolean addNpc(Npc npcInst)
		{
			return _npcList.add(npcInst);
		}
		
		protected boolean removeNpc(Npc npcInst)
		{
			return _npcList.remove(npcInst);
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public int getInitialDelay()
		{
			return _initDelay;
		}
		
		public int getRespawnDelay()
		{
			return _resDelay;
		}
		
		public int getDespawnDelay()
		{
			return _desDelay;
		}
		
		/**
		 * Gets the NPC ID.
		 * @return the NPC ID
		 */
		public int getId()
		{
			return _npcId;
		}
		
		public int getSpawnCount()
		{
			return _spawnCount;
		}
		
		public List<Location> getLocationList()
		{
			return _locList;
		}
		
		public Queue<Npc> getNpcList()
		{
			return _npcList;
		}
		
		public List<Spawn> getSpawns()
		{
			final List<Spawn> npcSpawns = new ArrayList<>();
			for (Npc npcInst : _npcList)
			{
				npcSpawns.add(npcInst.getSpawn());
			}
			
			return npcSpawns;
		}
		
		public void setSpawnCount(int spawnCount)
		{
			_spawnCount = spawnCount;
		}
		
		public void setRandomSpawn(boolean randValue)
		{
			_randomSpawn = randValue;
		}
		
		public void setBroadcast(boolean broadcastValue)
		{
			_broadcastAnnouncement = broadcastValue;
		}
		
		public boolean isSpawnActive()
		{
			return _spawnActive;
		}
		
		public boolean isRandomSpawn()
		{
			return _randomSpawn;
		}
		
		public boolean isBroadcasting()
		{
			return _broadcastAnnouncement;
		}
		
		public boolean addSpawnLocation(int x, int y, int z, int heading)
		{
			return _locList.add(new Location(x, y, z, heading));
		}
		
		public boolean addSpawnLocation(int[] spawnLoc)
		{
			return (spawnLoc.length == 3) && addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
		}
		
		public Location removeSpawnLocation(int locIndex)
		{
			try
			{
				return _locList.remove(locIndex);
			}
			catch (IndexOutOfBoundsException e)
			{
				return null;
			}
		}
	}
	
	public static AutoSpawnHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoSpawnHandler INSTANCE = new AutoSpawnHandler();
	}
}
