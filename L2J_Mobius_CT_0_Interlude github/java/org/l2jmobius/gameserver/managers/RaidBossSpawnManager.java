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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.model.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.model.spawns.Spawn;

/**
 * Raid Boss spawn manager.
 * @author godson
 */
public class RaidBossSpawnManager
{
	private static final Logger LOGGER = Logger.getLogger(RaidBossSpawnManager.class.getName());
	
	protected static final Map<Integer, RaidBoss> _bosses = new ConcurrentHashMap<>();
	protected static final Map<Integer, Spawn> _spawns = new ConcurrentHashMap<>();
	protected static final Map<Integer, StatSet> _storedInfo = new ConcurrentHashMap<>();
	protected static final Map<Integer, ScheduledFuture<?>> _schedules = new ConcurrentHashMap<>();
	
	/**
	 * Instantiates a new raid boss spawn manager.
	 */
	protected RaidBossSpawnManager()
	{
		load();
	}
	
	/**
	 * Load.
	 */
	public void load()
	{
		if (!_spawns.isEmpty())
		{
			for (Spawn spawn : _spawns.values())
			{
				deleteSpawn(spawn, false);
			}
		}
		
		_bosses.clear();
		_spawns.clear();
		_storedInfo.clear();
		_schedules.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM raidboss_spawnlist ORDER BY boss_id"))
		{
			while (rs.next())
			{
				final Spawn spawnDat = new Spawn(rs.getInt("boss_id"));
				spawnDat.setXYZ(rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
				spawnDat.setAmount(rs.getInt("amount"));
				spawnDat.setHeading(rs.getInt("heading"));
				spawnDat.setRespawnDelay(rs.getInt("respawn_delay"), rs.getInt("respawn_random"));
				addNewSpawn(spawnDat, rs.getLong("respawn_time"), rs.getDouble("currentHP"), rs.getDouble("currentMP"), false);
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _bosses.size() + " Instances");
			LOGGER.info(getClass().getSimpleName() + ": Scheduled " + _schedules.size() + " Instances");
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load raidboss_spawnlist table");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while initializing RaidBossSpawnManager: " + e.getMessage(), e);
		}
	}
	
	private static class SpawnSchedule implements Runnable
	{
		private static final Logger LOGGER = Logger.getLogger(SpawnSchedule.class.getName());
		
		private final int bossId;
		
		/**
		 * Instantiates a new spawn schedule.
		 * @param npcId the npc id
		 */
		public SpawnSchedule(int npcId)
		{
			bossId = npcId;
		}
		
		@Override
		public void run()
		{
			final RaidBoss raidboss = (RaidBoss) _spawns.get(bossId).doSpawn();
			if (raidboss != null)
			{
				raidboss.setRaidStatus(RaidBossStatus.ALIVE);
				
				final StatSet info = new StatSet();
				info.set("currentHP", raidboss.getCurrentHp());
				info.set("currentMP", raidboss.getCurrentMp());
				info.set("respawnTime", 0);
				_storedInfo.put(bossId, info);
				
				LOGGER.info(getClass().getSimpleName() + ": Spawning Raid Boss " + raidboss.getName());
				_bosses.put(bossId, raidboss);
			}
			
			_schedules.remove(bossId);
		}
	}
	
	/**
	 * Update status.
	 * @param boss the boss
	 * @param isBossDead the is boss dead
	 */
	public void updateStatus(RaidBoss boss, boolean isBossDead)
	{
		final StatSet info = _storedInfo.get(boss.getId());
		if (info == null)
		{
			return;
		}
		
		if (isBossDead)
		{
			boss.setRaidStatus(RaidBossStatus.DEAD);
			
			final int respawnMinDelay = (int) (boss.getSpawn().getRespawnMinDelay() * NpcConfig.RAID_MIN_RESPAWN_MULTIPLIER);
			final int respawnMaxDelay = (int) (boss.getSpawn().getRespawnMaxDelay() * NpcConfig.RAID_MAX_RESPAWN_MULTIPLIER);
			final int respawnDelay = Rnd.get(respawnMinDelay, respawnMaxDelay);
			final long respawnTime = System.currentTimeMillis() + respawnDelay;
			info.set("currentHP", boss.getMaxHp());
			info.set("currentMP", boss.getMaxMp());
			info.set("respawnTime", respawnTime);
			if (!_schedules.containsKey(boss.getId()) && ((respawnMinDelay > 0) || (respawnMaxDelay > 0)))
			{
				final Calendar time = Calendar.getInstance();
				time.setTimeInMillis(respawnTime);
				LOGGER.info(getClass().getSimpleName() + ": Updated " + boss.getName() + " respawn time to " + time.getTime());
				_schedules.put(boss.getId(), ThreadPool.schedule(new SpawnSchedule(boss.getId()), respawnDelay));
				updateDb();
			}
		}
		else
		{
			boss.setRaidStatus(RaidBossStatus.ALIVE);
			
			info.set("currentHP", boss.getCurrentHp());
			info.set("currentMP", boss.getCurrentMp());
			info.set("respawnTime", 0);
		}
		
		_storedInfo.put(boss.getId(), info);
	}
	
	/**
	 * Adds the new spawn.
	 * @param spawnDat the spawn dat
	 * @param respawnTime the respawn time
	 * @param currentHP the current hp
	 * @param currentMP the current mp
	 * @param storeInDb the store in db
	 */
	public void addNewSpawn(Spawn spawnDat, long respawnTime, double currentHP, double currentMP, boolean storeInDb)
	{
		if ((spawnDat == null) || _spawns.containsKey(spawnDat.getId()))
		{
			return;
		}
		
		final int bossId = spawnDat.getId();
		final long currentTime = System.currentTimeMillis();
		SpawnTable.getInstance().addSpawn(spawnDat);
		if ((respawnTime == 0) || (currentTime > respawnTime))
		{
			final RaidBoss raidboss = (RaidBoss) spawnDat.doSpawn();
			if (raidboss != null)
			{
				raidboss.setCurrentHp(currentHP);
				raidboss.setCurrentMp(currentMP);
				raidboss.setRaidStatus(RaidBossStatus.ALIVE);
				
				_bosses.put(bossId, raidboss);
				
				final StatSet info = new StatSet();
				info.set("currentHP", currentHP);
				info.set("currentMP", currentMP);
				info.set("respawnTime", 0);
				_storedInfo.put(bossId, info);
			}
		}
		else
		{
			_schedules.put(bossId, ThreadPool.schedule(new SpawnSchedule(bossId), respawnTime - currentTime));
		}
		
		_spawns.put(bossId, spawnDat);
		
		if (storeInDb)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("INSERT INTO raidboss_spawnlist (boss_id,amount,loc_x,loc_y,loc_z,heading,respawn_time,currentHp,currentMp) VALUES(?,?,?,?,?,?,?,?,?)"))
			{
				ps.setInt(1, spawnDat.getId());
				ps.setInt(2, spawnDat.getAmount());
				ps.setInt(3, spawnDat.getX());
				ps.setInt(4, spawnDat.getY());
				ps.setInt(5, spawnDat.getZ());
				ps.setInt(6, spawnDat.getHeading());
				ps.setLong(7, respawnTime);
				ps.setDouble(8, currentHP);
				ps.setDouble(9, currentMP);
				ps.execute();
			}
			catch (Exception e)
			{
				// problem with storing spawn
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not store raidboss #" + bossId + " in the DB:" + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Delete spawn.
	 * @param spawnDat the spawn dat
	 * @param updateDb the update db
	 */
	public void deleteSpawn(Spawn spawnDat, boolean updateDb)
	{
		if (spawnDat == null)
		{
			return;
		}
		
		final int bossId = spawnDat.getId();
		if (!_spawns.containsKey(bossId))
		{
			return;
		}
		
		SpawnTable.getInstance().removeSpawn(spawnDat);
		_spawns.remove(bossId);
		
		if (_bosses.containsKey(bossId))
		{
			_bosses.remove(bossId);
		}
		
		if (_schedules.containsKey(bossId))
		{
			_schedules.remove(bossId).cancel(true);
		}
		
		if (_storedInfo.containsKey(bossId))
		{
			_storedInfo.remove(bossId);
		}
		
		if (updateDb)
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM raidboss_spawnlist WHERE boss_id=?"))
			{
				ps.setInt(1, bossId);
				ps.execute();
			}
			catch (Exception e)
			{
				// problem with deleting spawn
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not remove raidboss #" + bossId + " from DB: " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Update database.
	 */
	private void updateDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE raidboss_spawnlist SET respawn_time = ?, currentHP = ?, currentMP = ? WHERE boss_id = ?"))
		{
			for (Entry<Integer, StatSet> entry : _storedInfo.entrySet())
			{
				final Integer bossId = entry.getKey();
				if (bossId == null)
				{
					continue;
				}
				
				final RaidBoss boss = _bosses.get(bossId);
				if (boss == null)
				{
					continue;
				}
				
				if (boss.getRaidStatus() == RaidBossStatus.ALIVE)
				{
					updateStatus(boss, false);
				}
				
				final StatSet info = entry.getValue();
				if (info == null)
				{
					continue;
				}
				
				try
				{
					// TODO(Zoey76): Change this to use batch.
					ps.setLong(1, info.getLong("respawnTime"));
					ps.setDouble(2, boss.isDead() ? boss.getMaxHp() : info.getDouble("currentHP"));
					ps.setDouble(3, boss.isDead() ? boss.getMaxMp() : info.getDouble("currentMP"));
					ps.setInt(4, bossId);
					ps.executeUpdate();
					ps.clearParameters();
				}
				catch (SQLException e)
				{
					LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not update raidboss_spawnlist table " + e.getMessage(), e);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": SQL error while updating RaidBoss spawn to database: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Gets the all raid boss status.
	 * @return the all raid boss status
	 */
	public String[] getAllRaidBossStatus()
	{
		final String[] msg = new String[_bosses.size()];
		int index = 0;
		for (RaidBoss boss : _bosses.values())
		{
			msg[index++] = boss.getName() + ": " + boss.getRaidStatus().name();
		}
		
		return msg;
	}
	
	/**
	 * Gets the raid boss status.
	 * @param bossId the boss id
	 * @return the raid boss status
	 */
	public String getRaidBossStatus(int bossId)
	{
		String msg = "RaidBoss Status..." + System.lineSeparator();
		if (_bosses.containsKey(bossId))
		{
			final RaidBoss boss = _bosses.get(bossId);
			msg += boss.getName() + ": " + boss.getRaidStatus().name();
		}
		
		return msg;
	}
	
	/**
	 * Gets the raid boss status id.
	 * @param bossId the boss id
	 * @return the raid boss status id
	 */
	public RaidBossStatus getRaidBossStatusId(int bossId)
	{
		if (_bosses.containsKey(bossId))
		{
			return _bosses.get(bossId).getRaidStatus();
		}
		else if (_schedules.containsKey(bossId))
		{
			return RaidBossStatus.DEAD;
		}
		else
		{
			return RaidBossStatus.UNDEFINED;
		}
	}
	
	/**
	 * Notify spawn night boss.
	 * @param raidboss the raidboss
	 */
	public void notifySpawnNightBoss(RaidBoss raidboss)
	{
		final StatSet info = new StatSet();
		info.set("currentHP", raidboss.getCurrentHp());
		info.set("currentMP", raidboss.getCurrentMp());
		info.set("respawnTime", 0);
		raidboss.setRaidStatus(RaidBossStatus.ALIVE);
		
		_storedInfo.put(raidboss.getId(), info);
		
		LOGGER.info(getClass().getSimpleName() + ": Spawning Night Raid Boss " + raidboss.getName());
		_bosses.put(raidboss.getId(), raidboss);
	}
	
	/**
	 * Checks if the boss is defined.
	 * @param bossId the boss id
	 * @return {@code true} if is defined
	 */
	public boolean isDefined(int bossId)
	{
		return _spawns.containsKey(bossId);
	}
	
	/**
	 * Gets the bosses.
	 * @return the bosses
	 */
	public Map<Integer, RaidBoss> getBosses()
	{
		return _bosses;
	}
	
	/**
	 * Gets the spawns.
	 * @return the spawns
	 */
	public Map<Integer, Spawn> getSpawns()
	{
		return _spawns;
	}
	
	/**
	 * Gets the stored info.
	 * @return the stored info
	 */
	public Map<Integer, StatSet> getStoredInfo()
	{
		return _storedInfo;
	}
	
	/**
	 * Saves and clears the raid bosses status, including all schedules.
	 */
	public void cleanUp()
	{
		updateDb();
		
		_bosses.clear();
		
		for (ScheduledFuture<?> schedule : _schedules.values())
		{
			schedule.cancel(true);
		}
		
		_schedules.clear();
		
		_storedInfo.clear();
		_spawns.clear();
	}
	
	/**
	 * Gets the single instance of RaidBossSpawnManager.
	 * @return single instance of RaidBossSpawnManager
	 */
	public static RaidBossSpawnManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidBossSpawnManager INSTANCE = new RaidBossSpawnManager();
	}
}
