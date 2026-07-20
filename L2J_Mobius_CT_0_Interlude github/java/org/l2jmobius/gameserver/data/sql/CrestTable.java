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
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.Crest;
import org.l2jmobius.gameserver.model.clan.enums.CrestType;

/**
 * Loads and saves crests from database.
 * @author NosBit
 */
public class CrestTable
{
	private static final Logger LOGGER = Logger.getLogger(CrestTable.class.getName());
	
	private final Map<Integer, Crest> _crests = new ConcurrentHashMap<>();
	private final AtomicInteger _nextId = new AtomicInteger(1);
	
	protected CrestTable()
	{
		load();
	}
	
	public synchronized void load()
	{
		_crests.clear();
		final Set<Integer> crestsInUse = new HashSet<>();
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getCrestId() != 0)
			{
				crestsInUse.add(clan.getCrestId());
			}
			
			if (clan.getCrestLargeId() != 0)
			{
				crestsInUse.add(clan.getCrestLargeId());
			}
			
			if (clan.getAllyCrestId() != 0)
			{
				crestsInUse.add(clan.getAllyCrestId());
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT `crest_id`, `data`, `type` FROM `crests` ORDER BY `crest_id` DESC");
			ResultSet rs = statement.executeQuery())
		{
			while (rs.next())
			{
				final int id = rs.getInt("crest_id");
				if (_nextId.get() <= id)
				{
					_nextId.set(id + 1);
				}
				
				// delete all unused crests except the last one we do not want to reuse
				// a crest id because client will display wrong crest if it is reused
				if (!crestsInUse.contains(id) && (id != (_nextId.get() - 1)))
				{
					removeCrest(id);
					continue;
				}
				
				final byte[] data = rs.getBytes("data");
				final CrestType crestType = CrestType.getById(rs.getInt("type"));
				if (crestType != null)
				{
					_crests.put(id, new Crest(id, data, crestType));
				}
				else
				{
					LOGGER.warning("Unknown crest type found in database. Type:" + rs.getInt("type"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "There was an error while loading crests from database:", e);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _crests.size() + " Crests.");
		
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			if ((clan.getCrestId() != 0) && (getCrest(clan.getCrestId()) == null))
			{
				LOGGER.info("Removing non-existent crest for clan " + clan.getName() + " [" + clan.getId() + "], crestId:" + clan.getCrestId());
				clan.setCrestId(0);
				clan.changeClanCrest(0);
			}
			
			if ((clan.getCrestLargeId() != 0) && (getCrest(clan.getCrestLargeId()) == null))
			{
				LOGGER.info("Removing non-existent large crest for clan " + clan.getName() + " [" + clan.getId() + "], crestLargeId:" + clan.getCrestLargeId());
				clan.setCrestLargeId(0);
				clan.changeLargeCrest(0);
			}
			
			if ((clan.getAllyCrestId() != 0) && (getCrest(clan.getAllyCrestId()) == null))
			{
				LOGGER.info("Removing non-existent ally crest for clan " + clan.getName() + " [" + clan.getId() + "], allyCrestId:" + clan.getAllyCrestId());
				clan.setAllyCrestId(0);
				clan.changeAllyCrest(0, true);
			}
		}
	}
	
	/**
	 * @param crestId The crest id
	 * @return {@code Crest} if crest is found, {@code null} if crest was not found.
	 */
	public Crest getCrest(int crestId)
	{
		return _crests.get(crestId);
	}
	
	/**
	 * Creates a {@code Crest} object and inserts it in database and cache.
	 * @param data
	 * @param crestType
	 * @return {@code Crest} on success, {@code null} on failure.
	 */
	public Crest createCrest(byte[] data, CrestType crestType)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `crests`(`crest_id`, `data`, `type`) VALUES(?, ?, ?)"))
		{
			final Crest crest = new Crest(_nextId.getAndIncrement(), data, crestType);
			statement.setInt(1, crest.getId());
			statement.setBytes(2, crest.getData());
			statement.setInt(3, crest.getType().getId());
			statement.executeUpdate();
			_crests.put(crest.getId(), crest);
			return crest;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "There was an error while saving crest in database:", e);
		}
		
		return null;
	}
	
	/**
	 * Removes crest from database and cache.
	 * @param crestId the id of crest to be removed.
	 */
	public void removeCrest(int crestId)
	{
		_crests.remove(crestId);
		
		// avoid removing last crest id we do not want to lose index...
		// because client will display wrong crest if it is reused
		if (crestId == (_nextId.get() - 1))
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM `crests` WHERE `crest_id`=?"))
		{
			statement.setInt(1, crestId);
			statement.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "There was an error while deleting crest from database:", e);
		}
	}
	
	/**
	 * @return The next crest id.
	 */
	public int getNextId()
	{
		return _nextId.getAndIncrement();
	}
	
	public static CrestTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CrestTable INSTANCE = new CrestTable();
	}
}
