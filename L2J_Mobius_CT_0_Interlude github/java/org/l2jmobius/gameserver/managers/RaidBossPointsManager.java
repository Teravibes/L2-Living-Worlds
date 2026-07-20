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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;

/**
 * @author Kerberos, JIV
 * @version 8/24/10
 */
public class RaidBossPointsManager
{
	private static final Logger LOGGER = Logger.getLogger(RaidBossPointsManager.class.getName());
	
	private final Map<Integer, Map<Integer, Integer>> _list = new ConcurrentHashMap<>();
	
	public RaidBossPointsManager()
	{
		init();
		
		// Start reset task at 00:10 and repeat every 24 hours.
		ThreadPool.scheduleAtFixedRate(this::resetRaidPoints, TimeUtil.getNextTime(0, 10).getTimeInMillis() - System.currentTimeMillis(), 86400000);
	}
	
	private void init()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT `charId`,`boss_id`,`points` FROM `character_raid_points`"))
		{
			while (rs.next())
			{
				final int charId = rs.getInt("charId");
				final int bossId = rs.getInt("boss_id");
				final int points = rs.getInt("points");
				Map<Integer, Integer> values = _list.get(charId);
				if (values == null)
				{
					values = new HashMap<>();
				}
				
				values.put(bossId, points);
				_list.put(charId, values);
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _list.size() + " Characters Raid Points.");
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load raid points ", e);
		}
	}
	
	private void resetRaidPoints()
	{
		final Calendar calendar = Calendar.getInstance();
		if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
		{
			return;
		}
		
		// Reward clan reputation points.
		final Map<Integer, Integer> rankList = RaidBossPointsManager.getInstance().getRankList();
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			for (Entry<Integer, Integer> entry : rankList.entrySet())
			{
				if ((entry.getValue() <= 100) && clan.isMember(entry.getKey()))
				{
					int reputation = 0;
					switch (entry.getValue())
					{
						case 1:
						{
							reputation = FeatureConfig.RAID_RANKING_1ST;
							break;
						}
						case 2:
						{
							reputation = FeatureConfig.RAID_RANKING_2ND;
							break;
						}
						case 3:
						{
							reputation = FeatureConfig.RAID_RANKING_3RD;
							break;
						}
						case 4:
						{
							reputation = FeatureConfig.RAID_RANKING_4TH;
							break;
						}
						case 5:
						{
							reputation = FeatureConfig.RAID_RANKING_5TH;
							break;
						}
						case 6:
						{
							reputation = FeatureConfig.RAID_RANKING_6TH;
							break;
						}
						case 7:
						{
							reputation = FeatureConfig.RAID_RANKING_7TH;
							break;
						}
						case 8:
						{
							reputation = FeatureConfig.RAID_RANKING_8TH;
							break;
						}
						case 9:
						{
							reputation = FeatureConfig.RAID_RANKING_9TH;
							break;
						}
						case 10:
						{
							reputation = FeatureConfig.RAID_RANKING_10TH;
							break;
						}
						default:
						{
							if (entry.getValue() <= 50)
							{
								reputation = FeatureConfig.RAID_RANKING_UP_TO_50TH;
							}
							else
							{
								reputation = FeatureConfig.RAID_RANKING_UP_TO_100TH;
							}
							break;
						}
					}
					
					clan.addReputationScore(reputation);
				}
			}
		}
		
		cleanUp();
		LOGGER.info(getClass().getSimpleName() + ": Reset task launched.");
	}
	
	public void updatePointsInDB(Player player, int raidId, int points)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("REPLACE INTO character_raid_points (`charId`,`boss_id`,`points`) VALUES (?,?,?)"))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, raidId);
			ps.setInt(3, points);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't update char raid points for player: " + player, e);
		}
	}
	
	public void addPoints(Player player, int bossId, int points)
	{
		final Map<Integer, Integer> tmpPoint = _list.computeIfAbsent(player.getObjectId(), _ -> new HashMap<>());
		updatePointsInDB(player, bossId, tmpPoint.merge(bossId, points, Integer::sum));
	}
	
	public int getPointsByOwnerId(int ownerId)
	{
		final Map<Integer, Integer> tmpPoint = _list.get(ownerId);
		int totalPoints = 0;
		if ((tmpPoint == null) || tmpPoint.isEmpty())
		{
			return 0;
		}
		
		for (int points : tmpPoint.values())
		{
			totalPoints += points;
		}
		
		return totalPoints;
	}
	
	public Map<Integer, Integer> getList(Player player)
	{
		return _list.get(player.getObjectId());
	}
	
	public void cleanUp()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE from character_raid_points WHERE charId > 0"))
		{
			statement.executeUpdate();
			_list.clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Couldn't clean raid points", e);
		}
	}
	
	public int calculateRanking(int playerObjId)
	{
		final Map<Integer, Integer> rank = getRankList();
		if (rank.containsKey(playerObjId))
		{
			return rank.get(playerObjId);
		}
		
		return 0;
	}
	
	public Map<Integer, Integer> getRankList()
	{
		final Map<Integer, Integer> tmpPoints = new HashMap<>();
		for (int ownerId : _list.keySet())
		{
			final int totalPoints = getPointsByOwnerId(ownerId);
			if (totalPoints != 0)
			{
				tmpPoints.put(ownerId, totalPoints);
			}
		}
		
		final List<Entry<Integer, Integer>> list = new ArrayList<>(tmpPoints.entrySet());
		list.sort(Comparator.comparing(Entry<Integer, Integer>::getValue).reversed());
		int ranking = 1;
		final Map<Integer, Integer> tmpRanking = new HashMap<>();
		for (Entry<Integer, Integer> entry : list)
		{
			tmpRanking.put(entry.getKey(), ranking++);
		}
		
		return tmpRanking;
	}
	
	public static RaidBossPointsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidBossPointsManager INSTANCE = new RaidBossPointsManager();
	}
}
