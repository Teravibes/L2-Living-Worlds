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
package org.l2jmobius.gameserver.model.actor.holders.npc;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;

/**
 * Manages minion spawning, tracking and lifecycle for monster masters.<br>
 * Handles automatic minion spawning, respawning, combat assistance and teleportation synchronization.
 * <ul>
 * <li>Spawns minions based on master configuration and tracks alive minions.</li>
 * <li>Manages minion respawning with configurable timers for raids and regular monsters.</li>
 * <li>Coordinates combat assistance between master and minions during attacks.</li>
 * <li>Synchronizes minion positions when master teleports to maintain formation.</li>
 * </ul>
 * @author luisantonioa, DS, Mobius
 */
public class MinionList
{
	// Constants.
	private static final int SPAWN_OFFSET_RADIUS = 200;
	private static final int MINIMUM_SPAWN_RADIUS = 30;
	private static final int BASE_AGGRO_AMOUNT = 1;
	private static final int MASTER_AGGRO_AMOUNT = 10;
	private static final int RAID_AGGRO_MULTIPLIER = 10;
	private static final int INVALID_RESPAWN_TIME = -1;
	private static final int NO_RESPAWN = 0;
	
	// Master Monster Management.
	protected final Monster _master;
	
	// Minion Tracking.
	private final Set<Monster> _spawnedMinions = ConcurrentHashMap.newKeySet();
	private final Set<ScheduledFuture<?>> _respawnTasks = ConcurrentHashMap.newKeySet();
	
	/**
	 * Creates a new minion list for the specified master monster.
	 * @param master the monster that will control the minions
	 */
	public MinionList(Monster master)
	{
		if (master == null)
		{
			throw new NullPointerException("MinionList: Master monster cannot be null!");
		}
		
		_master = master;
	}
	
	/**
	 * Gets the list of currently spawned and alive minions.
	 * @return list of spawned minions
	 */
	public Collection<Monster> getSpawnedMinions()
	{
		return _spawnedMinions;
	}
	
	/**
	 * Spawns all required minions based on the master's minion configuration.<br>
	 * Only spawns minions that are missing to reach the required count for each minion type.
	 * @param minions the minion configuration holders specifying types and counts
	 */
	public void spawnMinions(Collection<MinionHolder> minions)
	{
		if (_master.isAlikeDead() || (minions == null))
		{
			return;
		}
		
		int minionCount;
		int minionId;
		int minionsToSpawn;
		for (MinionHolder minion : minions)
		{
			minionCount = minion.getCount();
			minionId = minion.getId();
			minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);
			if (minionsToSpawn > 0)
			{
				for (int i = 0; i < minionsToSpawn; i++)
				{
					spawnMinion(minionId);
				}
			}
		}
	}
	
	/**
	 * Registers a newly spawned minion to the tracking list.
	 * @param minion the minion that was spawned
	 */
	public void onMinionSpawn(Monster minion)
	{
		_spawnedMinions.add(minion);
	}
	
	/**
	 * Handles master death by cleaning up minions and respawn tasks.<br>
	 * Minions are deleted for raid bosses or when forced, otherwise they remain alive.
	 * @param force when true, forces deletion of spawned minions regardless of master type
	 */
	public void onMasterDie(boolean force)
	{
		if (_master.isRaid() || force || NpcConfig.FORCE_DELETE_MINIONS)
		{
			if (!_spawnedMinions.isEmpty())
			{
				for (Monster minion : _spawnedMinions)
				{
					if (minion != null)
					{
						minion.setLeader(null);
						minion.deleteMe();
					}
				}
				
				_spawnedMinions.clear();
			}
			
			if (!_respawnTasks.isEmpty())
			{
				for (ScheduledFuture<?> task : _respawnTasks)
				{
					if ((task != null) && !task.isCancelled() && !task.isDone())
					{
						task.cancel(true);
					}
				}
				
				_respawnTasks.clear();
			}
		}
	}
	
	/**
	 * Handles minion death by removing from tracking and scheduling respawn if configured.<br>
	 * Respawn behavior depends on master type and the specified respawn time parameter.
	 * @param minion the minion that died
	 * @param respawnTime respawn delay in milliseconds, -1 uses default values, 0 disables respawn
	 */
	public void onMinionDie(Monster minion, int respawnTime)
	{
		// Prevent memory leaks.
		if (respawnTime == NO_RESPAWN)
		{
			minion.setLeader(null);
		}
		
		_spawnedMinions.remove(minion);
		
		final int actualRespawnTime = respawnTime <= INVALID_RESPAWN_TIME ? _master.isRaid() ? (int) NpcConfig.RAID_MINION_RESPAWN_TIMER : NO_RESPAWN : respawnTime;
		if ((actualRespawnTime > NO_RESPAWN) && !_master.isAlikeDead())
		{
			_respawnTasks.add(ThreadPool.schedule(new MinionRespawnTask(minion), actualRespawnTime));
		}
	}
	
	/**
	 * Handles combat assistance by distributing aggro to master and available minions.<br>
	 * Master receives aggro if not in combat, and free minions receive scaled aggro amounts.
	 * @param caller the creature that was attacked (master or minion)
	 * @param attacker the creature that initiated the attack
	 */
	public void onAssist(Creature caller, Creature attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!_master.isAlikeDead() && !_master.isInCombat())
		{
			_master.addDamageHate(attacker, 0, BASE_AGGRO_AMOUNT);
		}
		
		final boolean callerIsMaster = caller == _master;
		int aggroAmount = callerIsMaster ? MASTER_AGGRO_AMOUNT : BASE_AGGRO_AMOUNT;
		if (_master.isRaid())
		{
			aggroAmount *= RAID_AGGRO_MULTIPLIER;
		}
		
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && !minion.isDead() && (callerIsMaster || !minion.isInCombat()))
			{
				minion.addDamageHate(attacker, 0, aggroAmount);
			}
		}
	}
	
	/**
	 * Teleports all alive and mobile minions to positions around the master.<br>
	 * Called when master teleports to maintain minion formation and proximity.
	 */
	public void onMasterTeleported()
	{
		final int spawnRadius = SPAWN_OFFSET_RADIUS;
		final int minimumRadius = (int) _master.getCollisionRadius() + MINIMUM_SPAWN_RADIUS;
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && !minion.isDead() && !minion.isMovementDisabled())
			{
				int newX = Rnd.get(minimumRadius * 2, spawnRadius * 2); // x
				int newY = Rnd.get(newX, spawnRadius * 2); // distance
				newY = (int) Math.sqrt((newY * newY) - (newX * newX)); // y
				if (newX > (spawnRadius + minimumRadius))
				{
					newX = (_master.getX() + newX) - spawnRadius;
				}
				else
				{
					newX = (_master.getX() - newX) + minimumRadius;
				}
				
				if (newY > (spawnRadius + minimumRadius))
				{
					newY = (_master.getY() + newY) - spawnRadius;
				}
				else
				{
					newY = (_master.getY() - newY) + minimumRadius;
				}
				
				minion.teleToLocation(new Location(newX, newY, _master.getZ()));
			}
		}
	}
	
	/**
	 * Spawns a single minion by template ID using the master as reference.
	 * @param minionTemplateId the NPC template ID of the minion to spawn
	 */
	private void spawnMinion(int minionTemplateId)
	{
		if (minionTemplateId == 0)
		{
			return;
		}
		
		spawnMinion(_master, minionTemplateId);
	}
	
	/**
	 * Task for respawning dead minions after a configured delay.<br>
	 * Restores the minion's state and inherits master's current aggro list.
	 */
	private class MinionRespawnTask implements Runnable
	{
		private final Monster _minion;
		
		/**
		 * Creates a respawn task for the specified minion.
		 * @param minion the minion to respawn
		 */
		public MinionRespawnTask(Monster minion)
		{
			_minion = minion;
		}
		
		@Override
		public void run()
		{
			// Minion can be already spawned or deleted.
			if (!_master.isAlikeDead() && _master.isSpawned() && !_minion.isSpawned())
			{
				// _minion.refreshId();
				initializeNpc(_master, _minion);
				
				// Assist master with existing aggro targets.
				if (!_master.getAggroList().isEmpty())
				{
					_minion.getAggroList().putAll(_master.getAggroList());
					_minion.getAI().setIntention(Intention.ATTACK, _minion.getAggroList().keySet().stream().findFirst().get());
				}
			}
		}
	}
	
	/**
	 * Creates and initializes a new minion instance in the world.<br>
	 * Sets up the minion's stats, position, leadership and spawns it near the master.
	 * @param master the monster that will control this minion
	 * @param minionTemplateId the NPC template ID for the minion type
	 * @return the spawned minion instance or null if template not found
	 */
	public static Monster spawnMinion(Monster master, int minionTemplateId)
	{
		// Get the template of the Minion to spawn
		final NpcTemplate minionTemplate = NpcData.getInstance().getTemplate(minionTemplateId);
		if (minionTemplate == null)
		{
			return null;
		}
		
		return initializeNpc(master, new Monster(minionTemplate));
	}
	
	/**
	 * Initializes a minion monster with full stats and spawns it near the master.<br>
	 * Resets the minion's state, assigns leadership, sets instance and broadcasts spawn.
	 * @param master the controlling monster
	 * @param minion the minion to initialize and spawn
	 * @return the initialized and spawned minion
	 */
	protected static Monster initializeNpc(Monster master, Monster minion)
	{
		minion.stopAllEffects();
		minion.setDead(false);
		minion.setDecayed(false);
		
		// Set the Minion HP, MP and Heading
		minion.setCurrentHpMp(minion.getMaxHp(), minion.getMaxMp());
		minion.setHeading(master.getHeading());
		
		// Set the Minion leader to this RaidBoss
		minion.setLeader(master);
		
		// Move monster to masters instance.
		minion.setInstanceId(master.getInstanceId());
		
		// Set custom Npc server side name and title
		if (minion.getTemplate().isUsingServerSideName())
		{
			minion.setName(minion.getTemplate().getName());
		}
		
		if (minion.getTemplate().isUsingServerSideTitle())
		{
			minion.setTitle(minion.getTemplate().getTitle());
		}
		
		// Initialize the position of the Minion and add it in the world as a visible object.
		final int spawnRadius = SPAWN_OFFSET_RADIUS;
		final int minimumRadius = (int) master.getCollisionRadius() + MINIMUM_SPAWN_RADIUS;
		int newX = Rnd.get(minimumRadius * 2, spawnRadius * 2); // x
		int newY = Rnd.get(newX, spawnRadius * 2); // distance
		newY = (int) Math.sqrt((newY * newY) - (newX * newX)); // y
		if (newX > (spawnRadius + minimumRadius))
		{
			newX = (master.getX() + newX) - spawnRadius;
		}
		else
		{
			newX = (master.getX() - newX) + minimumRadius;
		}
		
		if (newY > (spawnRadius + minimumRadius))
		{
			newY = (master.getY() + newY) - spawnRadius;
		}
		else
		{
			newY = (master.getY() - newY) + minimumRadius;
		}
		
		minion.spawnMe(newX, newY, master.getZ());
		
		// Make sure info is broadcasted in instances.
		if (minion.getInstanceId() > 0)
		{
			minion.broadcastInfo();
		}
		
		return minion;
	}
	
	/**
	 * Counts the number of spawned minions matching the specified template ID.
	 * @param minionTemplateId the template ID to count
	 * @return number of spawned minions with matching ID
	 */
	private final int countSpawnedMinionsById(int minionTemplateId)
	{
		int count = 0;
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && (minion.getId() == minionTemplateId))
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Gets the total number of spawned minions.
	 * @return total spawned minion count
	 */
	public int getSpawnedMinionCount()
	{
		return _spawnedMinions.size();
	}
}
