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
package org.l2jmobius.gameserver.model.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * @author godson
 */
public class Olympiad extends ListenersContainer
{
	protected static final Logger LOGGER = Logger.getLogger(Olympiad.class.getName());
	protected static final Logger LOGGER_OLYMPIAD = Logger.getLogger("olympiad");
	
	private static final Map<Integer, StatSet> NOBLES = new ConcurrentHashMap<>();
	protected static List<StatSet> HEROS_TO_BE;
	private static Map<Integer, Integer> NOBLES_RANK = new HashMap<>();
	private static List<Player> _nonClassBasedRegisters;
	private static Map<Integer, List<Player>> _classBasedRegisters;
	
	public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";
	private static final String OLYMPIAD_LOAD_DATA = "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id = 0";
	private static final String OLYMPIAD_SAVE_DATA = "INSERT INTO olympiad_data (id, current_cycle, period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, validation_end=?, next_weekly_change=?";
	private static final String OLYMPIAD_LOAD_NOBLES = "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId";
	private static final String OLYMPIAD_SAVE_NOBLES = "INSERT INTO olympiad_nobles (`charId`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`,`competitions_drawn`) VALUES (?,?,?,?,?,?,?)";
	private static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE olympiad_nobles SET olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ? WHERE charId = ?";
	private static final String OLYMPIAD_GET_HEROS = "SELECT olympiad_nobles.charId, characters.char_name FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 AND olympiad_nobles.competitions_won > 0 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC";
	private static final String GET_ALL_CLASSIFIED_NOBLESS = "SELECT charId from olympiad_nobles_eom WHERE competitions_done >= 9 ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC";
	private static final String GET_EACH_CLASS_LEADER = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_CURRENT = "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_SOULHOUND = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND (olympiad_nobles_eom.class_id = ? OR olympiad_nobles_eom.class_id = 133) AND olympiad_nobles_eom.competitions_done >= 9 ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_CURRENT_SOULHOUND = "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND (olympiad_nobles.class_id = ? OR olympiad_nobles.class_id = 133) AND olympiad_nobles.competitions_done >= 9 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
	
	private static final String REMOVE_UNCLAIMED_POINTS = "DELETE FROM character_variables WHERE charId=? AND var=?";
	private static final String INSERT_UNCLAIMED_POINTS = "INSERT INTO character_variables (charId, var, val) VALUES (?, ?, ?)";
	public static final String UNCLAIMED_OLYMPIAD_PASSES_VAR = "UNCLAIMED_OLYMPIAD_POINTS";
	
	private static final String OLYMPIAD_DELETE_ALL = "TRUNCATE olympiad_nobles";
	private static final String OLYMPIAD_MONTH_CLEAR = "TRUNCATE olympiad_nobles_eom";
	private static final String OLYMPIAD_MONTH_CREATE = "INSERT INTO olympiad_nobles_eom SELECT * FROM olympiad_nobles";
	private static final int[] HERO_IDS =
	{
		88,
		89,
		90,
		91,
		92,
		93,
		94,
		95,
		96,
		97,
		98,
		99,
		100,
		101,
		102,
		103,
		104,
		105,
		106,
		107,
		108,
		109,
		110,
		111,
		112,
		113,
		114,
		115,
		116,
		117,
		118,
		131,
		132,
		133,
		134
	};
	
	private static final int COMP_START = OlympiadConfig.OLYMPIAD_START_TIME; // 6PM
	private static final int COMP_MIN = OlympiadConfig.OLYMPIAD_MIN; // 00 mins
	private static final long COMP_PERIOD = OlympiadConfig.OLYMPIAD_CPERIOD; // 6 hours
	protected static final long WEEKLY_PERIOD = OlympiadConfig.OLYMPIAD_WPERIOD; // 1 week
	protected static final long VALIDATION_PERIOD = OlympiadConfig.OLYMPIAD_VPERIOD; // 24 hours
	
	protected static final int DEFAULT_POINTS = OlympiadConfig.OLYMPIAD_START_POINTS;
	protected static final int WEEKLY_POINTS = OlympiadConfig.OLYMPIAD_WEEKLY_POINTS;
	
	public static final String CHAR_ID = "charId";
	public static final String CLASS_ID = "class_id";
	public static final String CHAR_NAME = "char_name";
	public static final String POINTS = "olympiad_points";
	public static final String COMP_DONE = "competitions_done";
	public static final String COMP_WON = "competitions_won";
	public static final String COMP_LOST = "competitions_lost";
	public static final String COMP_DRAWN = "competitions_drawn";
	
	protected long _olympiadEnd;
	protected long _validationEnd;
	
	/**
	 * The current period of the olympiad.<br>
	 * <b>0 -</b> Competition period<br>
	 * <b>1 -</b> Validation Period
	 */
	protected int _period;
	protected long _nextWeeklyChange;
	protected int _currentCycle;
	private long _compEnd;
	private Calendar _compStart;
	protected static boolean _inCompPeriod;
	protected static boolean _compStarted = false;
	protected ScheduledFuture<?> _scheduledCompStart;
	protected ScheduledFuture<?> _scheduledCompEnd;
	protected ScheduledFuture<?> _scheduledOlympiadEnd;
	protected ScheduledFuture<?> _scheduledWeeklyTask;
	protected ScheduledFuture<?> _scheduledValdationTask;
	
	Olympiad()
	{
		if (OlympiadConfig.OLYMPIAD_ENABLED)
		{
			load();
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.OLYMPIAD_ID);
			
			if (_period == 0)
			{
				init();
			}
		}
		else
		{
			LOGGER.log(Level.INFO, "Disabled.");
		}
	}
	
	public static Integer getStadiumCount()
	{
		return OlympiadManager.STADIUMS.length;
	}
	
	private void load()
	{
		NOBLES.clear();
		
		boolean loaded = false;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_DATA);
			ResultSet rset = statement.executeQuery())
		{
			while (rset.next())
			{
				_currentCycle = rset.getInt("current_cycle");
				_period = rset.getInt("period");
				_olympiadEnd = rset.getLong("olympiad_end");
				_validationEnd = rset.getLong("validation_end");
				_nextWeeklyChange = rset.getLong("next_weekly_change");
				loaded = true;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Error loading olympiad data from database: ", e);
		}
		
		if (!loaded)
		{
			_currentCycle = 1;
			_period = 0;
			_olympiadEnd = 0;
			_validationEnd = 0;
			_nextWeeklyChange = 0;
		}
		
		final long currentTime = System.currentTimeMillis();
		switch (_period)
		{
			case 0:
			{
				if ((_olympiadEnd == 0) || (_olympiadEnd < currentTime))
				{
					setNewOlympiadEnd();
				}
				else
				{
					scheduleWeeklyChange();
				}
				break;
			}
			case 1:
			{
				if (_validationEnd > currentTime)
				{
					loadNoblesRank();
					_scheduledValdationTask = ThreadPool.schedule(new ValidationEndTask(), getMillisToValidationEnd());
				}
				else
				{
					_currentCycle++;
					_period = 0;
					deleteNobles();
					setNewOlympiadEnd();
				}
				break;
			}
			default:
			{
				LOGGER.warning("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
				return;
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
			ResultSet rset = statement.executeQuery())
		{
			StatSet statData;
			while (rset.next())
			{
				statData = new StatSet();
				final int charId = rset.getInt(CHAR_ID);
				statData.set(CLASS_ID, rset.getInt(CLASS_ID));
				statData.set(CHAR_NAME, rset.getString(CHAR_NAME));
				final int compDone = rset.getInt(COMP_DONE);
				statData.set(POINTS, MathUtil.clamp(rset.getInt(POINTS), 0, (OlympiadConfig.OLYMPIAD_MAX_POINTS * compDone) + (OlympiadConfig.OLYMPIAD_WEEKLY_POINTS * 4)));
				statData.set(COMP_DONE, compDone);
				statData.set(COMP_WON, rset.getInt(COMP_WON));
				statData.set(COMP_LOST, rset.getInt(COMP_LOST));
				statData.set(COMP_DRAWN, rset.getInt(COMP_DRAWN));
				statData.set("to_save", false);
				
				NOBLES.put(charId, statData);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database: ", e);
		}
		
		LOGGER.info("Olympiad System: Loading Olympiad System....");
		if (_period == 0)
		{
			LOGGER.info("Olympiad System: Currently in Olympiad Period");
		}
		else
		{
			LOGGER.info("Olympiad System: Currently in Validation Period");
		}
		
		long milliToEnd;
		if (_period == 0)
		{
			milliToEnd = getMillisToOlympiadEnd();
		}
		else
		{
			milliToEnd = getMillisToValidationEnd();
		}
		
		final double numSecs = (milliToEnd / 1000) % 60;
		double countDown = ((milliToEnd / 1000.) - numSecs) / 60;
		final int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		final int numHours = (int) Math.floor(countDown % 24);
		final int numDays = (int) Math.floor((countDown - numHours) / 24);
		
		LOGGER.info("Olympiad System: " + numDays + " days, " + numHours + " hours and " + numMins + " mins until period ends.");
		
		if (_period == 0)
		{
			milliToEnd = getMillisToWeekChange();
			final double numSecs2 = (milliToEnd / 1000) % 60;
			double countDown2 = ((milliToEnd / 1000.) - numSecs2) / 60;
			final int numMins2 = (int) Math.floor(countDown % 60);
			countDown2 = (countDown2 - numMins) / 60;
			final int numHours2 = (int) Math.floor(countDown2 % 24);
			final int numDays2 = (int) Math.floor((countDown2 - numHours) / 24);
			
			LOGGER.info("Olympiad System: Next weekly change is in " + numDays2 + " days, " + numHours2 + " hours and " + numMins2 + " mins.");
		}
		
		LOGGER.info("Olympiad System: Loaded " + NOBLES.size() + " Nobles");
	}
	
	public void loadNoblesRank()
	{
		NOBLES_RANK.clear();
		final Map<Integer, Integer> tmpPlace = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_ALL_CLASSIFIED_NOBLESS);
			ResultSet rset = statement.executeQuery())
		{
			int place = 1;
			while (rset.next())
			{
				tmpPlace.put(rset.getInt(CHAR_ID), place++);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database for Ranking: ", e);
		}
		
		int rank1 = (int) Math.round(tmpPlace.size() * 0.01);
		int rank2 = (int) Math.round(tmpPlace.size() * 0.10);
		int rank3 = (int) Math.round(tmpPlace.size() * 0.25);
		int rank4 = (int) Math.round(tmpPlace.size() * 0.50);
		if (rank1 == 0)
		{
			rank1 = 1;
			rank2++;
			rank3++;
			rank4++;
		}
		
		for (Entry<Integer, Integer> chr : tmpPlace.entrySet())
		{
			if (chr.getValue() <= rank1)
			{
				NOBLES_RANK.put(chr.getKey(), 1);
			}
			else if (tmpPlace.get(chr.getKey()) <= rank2)
			{
				NOBLES_RANK.put(chr.getKey(), 2);
			}
			else if (tmpPlace.get(chr.getKey()) <= rank3)
			{
				NOBLES_RANK.put(chr.getKey(), 3);
			}
			else if (tmpPlace.get(chr.getKey()) <= rank4)
			{
				NOBLES_RANK.put(chr.getKey(), 4);
			}
			else
			{
				NOBLES_RANK.put(chr.getKey(), 5);
			}
		}
		
		// Store remaining hero reward points to player variables.
		for (int noblesId : NOBLES.keySet())
		{
			final int points = getNoblessePasses(noblesId);
			if (points > 0)
			{
				final Player player = World.getInstance().getPlayer(noblesId);
				if (player != null)
				{
					player.getVariables().set(UNCLAIMED_OLYMPIAD_PASSES_VAR, points);
				}
				else
				{
					// Remove previous record.
					try (Connection con = DatabaseFactory.getConnection();
						PreparedStatement statement = con.prepareStatement(REMOVE_UNCLAIMED_POINTS))
					{
						statement.setInt(1, noblesId);
						statement.setString(2, UNCLAIMED_OLYMPIAD_PASSES_VAR);
						statement.execute();
					}
					catch (SQLException e)
					{
						LOGGER.warning("Olympiad System: Couldn't remove unclaimed olympiad points from DB!");
					}
					
					// Add new value.
					try (Connection con = DatabaseFactory.getConnection();
						PreparedStatement statement = con.prepareStatement(INSERT_UNCLAIMED_POINTS))
					{
						statement.setInt(1, noblesId);
						statement.setString(2, UNCLAIMED_OLYMPIAD_PASSES_VAR);
						statement.setString(3, String.valueOf(points));
						statement.execute();
					}
					catch (SQLException e)
					{
						LOGGER.warning("Olympiad System: Couldn't store unclaimed olympiad points to DB!");
					}
				}
			}
		}
	}
	
	protected void init()
	{
		if (_period == 1)
		{
			return;
		}
		
		_nonClassBasedRegisters = new ArrayList<>();
		_classBasedRegisters = new HashMap<>();
		
		_compStart = Calendar.getInstance();
		if (OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS)
		{
			final int currentDay = _compStart.get(Calendar.DAY_OF_WEEK);
			boolean dayFound = false;
			int dayCounter = 0;
			for (int i = currentDay; i < 8; i++)
			{
				if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
				{
					dayFound = true;
					break;
				}
				
				dayCounter++;
			}
			
			if (!dayFound)
			{
				for (int i = 1; i < 8; i++)
				{
					if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
					{
						break;
					}
					
					dayCounter++;
				}
			}
			
			if (dayCounter > 0)
			{
				_compStart.add(Calendar.DAY_OF_MONTH, dayCounter);
			}
		}
		
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;
		
		if (_scheduledOlympiadEnd != null)
		{
			_scheduledOlympiadEnd.cancel(true);
		}
		
		_scheduledOlympiadEnd = ThreadPool.schedule(new OlympiadEndTask(), getMillisToOlympiadEnd());
		
		updateCompStatus();
	}
	
	protected class OlympiadEndTask implements Runnable
	{
		@Override
		public void run()
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.PERIOD_S1_OF_THE_GRAND_OLYMPIAD_GAMES_HAS_NOW_ENDED);
			sm.addInt(_currentCycle);
			
			Broadcast.toAllOnlinePlayers(sm);
			Broadcast.toAllOnlinePlayers("Olympiad Validation Period has began");
			
			if (_scheduledWeeklyTask != null)
			{
				_scheduledWeeklyTask.cancel(true);
			}
			
			saveNobleData();
			
			_period = 1;
			sortHerosToBe();
			Hero.getInstance().resetData();
			Hero.getInstance().computeNewHeroes(HEROS_TO_BE);
			
			saveOlympiadStatus();
			updateMonthlyData();
			
			final Calendar validationEnd = Calendar.getInstance();
			_validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;
			
			loadNoblesRank();
			_scheduledValdationTask = ThreadPool.schedule(new ValidationEndTask(), getMillisToValidationEnd());
		}
	}
	
	protected class ValidationEndTask implements Runnable
	{
		@Override
		public void run()
		{
			Broadcast.toAllOnlinePlayers("Olympiad Validation Period has ended");
			_period = 0;
			_currentCycle++;
			deleteNobles();
			setNewOlympiadEnd();
			init();
		}
	}
	
	public boolean registerNoble(Player noble, boolean classBased)
	{
		/*
		 * if (_compStarted) { noble.sendMessage("Cannot Register whilst competition is under way"); return false; }
		 */
		
		if (!_inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		
		if (!noble.isNoble())
		{
			noble.sendPacket(SystemMessageId.ONLY_NOBLESSE_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			return false;
		}
		
		/** Begin Olympiad Restrictions */
		if (noble.getBaseClass() != noble.getPlayerClass().getId())
		{
			noble.sendPacket(SystemMessageId.YOU_CANNOT_PARTICIPATE_IN_THE_GRAND_OLYMPIAD_GAMES_WITH_A_CHARACTER_IN_THEIR_SUBCLASS);
			return false;
		}
		
		if (noble.isCursedWeaponEquipped())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.IF_YOU_POSSESS_S1_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addItemName(noble.getCursedWeaponEquippedId());
			noble.sendPacket(sm);
			return false;
		}
		
		if ((noble.getInventoryLimit() * 0.8) <= noble.getInventory().getSize())
		{
			noble.sendPacket(SystemMessageId.YOU_CAN_T_JOIN_A_GRAND_OLYMPIAD_GAME_MATCH_WITH_THAT_MUCH_STUFF_ON_YOU_REDUCE_YOUR_WEIGHT_TO_BELOW_80_PERCENT_FULL_AND_REQUEST_TO_JOIN_AGAIN);
			return false;
		}
		
		if (getMillisToCompEnd() < 600000)
		{
			noble.sendPacket(SystemMessageId.THE_REQUEST_TO_PARTICIPATE_IN_THE_GAME_CANNOT_BE_MADE_STARTING_FROM_10_MINUTES_BEFORE_THE_END_OF_THE_GAME);
			return false;
		}
		
		if (noble.isRegisteredOnEvent())
		{
			noble.sendMessage("You can't join olympiad while participating on events.");
			return false;
		}
		/** End Olympiad Restrictions */
		
		if (_classBasedRegisters.containsKey(noble.getPlayerClass().getId()))
		{
			final List<Player> classed = _classBasedRegisters.get(noble.getPlayerClass().getId());
			for (Player participant : classed)
			{
				if (participant.getObjectId() == noble.getObjectId())
				{
					noble.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_JOINED_THE_WAITING_LIST_FOR_A_CLASS_SPECIFIC_MATCH);
					return false;
				}
			}
		}
		
		if (isRegisteredInComp(noble))
		{
			noble.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_JOINED_THE_WAITING_LIST_FOR_A_NON_CLASS_SPECIFIC_MATCH);
			return false;
		}
		
		if (!NOBLES.containsKey(noble.getObjectId()))
		{
			final StatSet statDat = new StatSet();
			statDat.set(CLASS_ID, noble.getPlayerClass().getId());
			statDat.set(CHAR_NAME, noble.getName());
			statDat.set(POINTS, DEFAULT_POINTS);
			statDat.set(COMP_DONE, 0);
			statDat.set(COMP_WON, 0);
			statDat.set(COMP_LOST, 0);
			statDat.set(COMP_DRAWN, 0);
			statDat.set("to_save", true);
			
			NOBLES.put(noble.getObjectId(), statDat);
		}
		
		if (classBased && (getNoblePoints(noble.getObjectId()) < 3))
		{
			noble.sendMessage("Cannot register when you have less than 3 points");
			return false;
		}
		
		if (!classBased && (getNoblePoints(noble.getObjectId()) < 5))
		{
			noble.sendMessage("Cannot register when you have less than 5 points");
			return false;
		}
		
		if (classBased)
		{
			if (_classBasedRegisters.containsKey(noble.getPlayerClass().getId()))
			{
				final List<Player> classed = _classBasedRegisters.get(noble.getPlayerClass().getId());
				classed.add(noble);
				
				_classBasedRegisters.remove(noble.getPlayerClass().getId());
				_classBasedRegisters.put(noble.getPlayerClass().getId(), classed);
			}
			else
			{
				final List<Player> classed = new ArrayList<>();
				classed.add(noble);
				
				_classBasedRegisters.put(noble.getPlayerClass().getId(), classed);
			}
			
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST_FOR_A_CLASS_SPECIFIC_MATCH);
		}
		else
		{
			_nonClassBasedRegisters.add(noble);
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REGISTERED_IN_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST_FOR_A_NON_CLASS_SPECIFIC_MATCH);
		}
		
		return true;
	}
	
	protected static int getNobleCount()
	{
		return NOBLES.size();
	}
	
	protected static StatSet getNobleStats(int playerId)
	{
		return NOBLES.get(playerId);
	}
	
	protected static List<Player> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}
	
	protected static Map<Integer, List<Player>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}
	
	protected static List<Integer> hasEnoughRegisteredClassed()
	{
		final List<Integer> result = new ArrayList<>();
		
		for (Integer classList : getRegisteredClassBased().keySet())
		{
			if (getRegisteredClassBased().get(classList).size() >= OlympiadConfig.OLYMPIAD_CLASSED)
			{
				result.add(classList);
			}
		}
		
		if (!result.isEmpty())
		{
			return result;
		}
		
		return null;
	}
	
	protected static boolean hasEnoughRegisteredNonClassed()
	{
		return Olympiad.getRegisteredNonClassBased().size() >= OlympiadConfig.OLYMPIAD_NONCLASSED;
	}
	
	protected static void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
	}
	
	public boolean isRegistered(Player noble)
	{
		boolean result = false;
		
		if ((_nonClassBasedRegisters != null) && _nonClassBasedRegisters.contains(noble))
		{
			result = true;
		}
		else if ((_classBasedRegisters != null) && _classBasedRegisters.containsKey(noble.getPlayerClass().getId()))
		{
			final List<Player> classed = _classBasedRegisters.get(noble.getPlayerClass().getId());
			if ((classed != null) && classed.contains(noble))
			{
				result = true;
			}
		}
		
		return result;
	}
	
	public boolean unRegisterNoble(Player noble)
	{
		if (!_inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}
		
		if (!noble.isNoble())
		{
			noble.sendPacket(SystemMessageId.ONLY_NOBLESSE_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			return false;
		}
		
		if (!isRegistered(noble))
		{
			noble.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_REGISTERED_ON_ANY_GRAND_OLYMPIAD_GAMES_WAITING_LIST);
			return false;
		}
		
		for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values())
		{
			if (game == null)
			{
				continue;
			}
			
			if ((game._playerOneID == noble.getObjectId()) || (game._playerTwoID == noble.getObjectId()))
			{
				noble.sendMessage("Can't deregister whilst you are already selected for a game");
				return false;
			}
		}
		
		if (_nonClassBasedRegisters.contains(noble))
		{
			_nonClassBasedRegisters.remove(noble);
		}
		else
		{
			final List<Player> classed = _classBasedRegisters.get(noble.getPlayerClass().getId());
			classed.remove(noble);
			
			_classBasedRegisters.remove(noble.getPlayerClass().getId());
			_classBasedRegisters.put(noble.getPlayerClass().getId(), classed);
		}
		
		noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REMOVED_FROM_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST);
		
		if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
		{
			AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, noble);
		}
		
		return true;
	}
	
	public void removeDisconnectedCompetitor(Player player)
	{
		if (OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()) != null)
		{
			OlympiadManager.getInstance().getOlympiadGame(player.getOlympiadGameId()).handleDisconnect(player);
		}
		
		final List<Player> classed = _classBasedRegisters.get(player.getPlayerClass().getId());
		
		if (_nonClassBasedRegisters.contains(player))
		{
			_nonClassBasedRegisters.remove(player);
		}
		else if ((classed != null) && classed.contains(player))
		{
			classed.remove(player);
			
			_classBasedRegisters.remove(player.getPlayerClass().getId());
			_classBasedRegisters.put(player.getPlayerClass().getId(), classed);
		}
	}
	
	public void notifyCompetitorDamage(Player player, int damage, int gameId)
	{
		if (OlympiadManager.getInstance().getOlympiadGames().get(gameId) != null)
		{
			OlympiadManager.getInstance().getOlympiadGames().get(gameId).addDamage(player, damage);
		}
	}
	
	private void updateCompStatus()
	{
		// _compStarted = false;
		
		final long milliToStart = getMillisToCompBegin();
		final double numSecs = (milliToStart / 1000) % 60;
		double countDown = ((milliToStart / 1000) - numSecs) / 60;
		final int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		final int numHours = (int) Math.floor(countDown % 24);
		final int numDays = (int) Math.floor((countDown - numHours) / 24);
		LOGGER.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
		LOGGER.info("Olympiad System: Event starts/started : " + _compStart.getTime());
		
		_scheduledCompStart = ThreadPool.schedule(() ->
		{
			if (isOlympiadEnd())
			{
				return;
			}
			
			_inCompPeriod = true;
			final OlympiadManager om = OlympiadManager.getInstance();
			
			Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.SHARPEN_YOUR_SWORDS_TIGHTEN_THE_STITCHINGS_IN_YOUR_ARMOR_AND_MAKE_HASTE_TO_A_GRAND_OLYMPIAD_MANAGER_BATTLES_IN_THE_GRAND_OLYMPIAD_GAMES_ARE_NOW_TAKING_PLACE));
			LOGGER.info("Olympiad System: Olympiad Game Started");
			LOGGER_OLYMPIAD.info("Result,Player1,Player2,Player1 HP,Player2 HP,Player1 Damage,Player2 Damage,Points,Classed");
			
			final Thread olyCycle = new Thread(om);
			olyCycle.start();
			
			final long regEnd = getMillisToCompEnd() - 600000;
			if (regEnd > 0)
			{
				ThreadPool.schedule(() -> Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_REGISTRATION_PERIOD_HAS_ENDED)), regEnd);
			}
			
			_scheduledCompEnd = ThreadPool.schedule(() ->
			{
				if (isOlympiadEnd())
				{
					return;
				}
				
				_inCompPeriod = false;
				Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.MUCH_CARNAGE_HAS_BEEN_LEFT_FOR_THE_CLEANUP_CREW_OF_THE_OLYMPIAD_STADIUM_BATTLES_IN_THE_GRAND_OLYMPIAD_GAMES_ARE_NOW_OVER));
				LOGGER.info("Olympiad System: Olympiad Game Ended");
				
				while (OlympiadGame._battleStarted)
				{
					try
					{
						// wait 1 minutes for end of pendings games
						Thread.sleep(60000);
					}
					catch (Exception e)
					{
						// Ignore.
					}
				}
				
				saveOlympiadStatus();
				
				init();
			}, getMillisToCompEnd());
		}, getMillisToCompBegin());
	}
	
	public long getMillisToOlympiadEnd()
	{
		// if (_olympiadEnd > System.currentTimeMillis())
		return (_olympiadEnd - System.currentTimeMillis());
		// return 10L;
	}
	
	public void manualSelectHeroes()
	{
		if (_scheduledOlympiadEnd != null)
		{
			_scheduledOlympiadEnd.cancel(true);
		}
		
		_scheduledOlympiadEnd = ThreadPool.schedule(new OlympiadEndTask(), 0);
	}
	
	protected long getMillisToValidationEnd()
	{
		final long currentTime = System.currentTimeMillis();
		if (_validationEnd > currentTime)
		{
			return (_validationEnd - currentTime);
		}
		
		return 10L;
	}
	
	public boolean isOlympiadEnd()
	{
		return (_period != 0);
	}
	
	protected void setNewOlympiadEnd()
	{
		final SystemMessage sm = new SystemMessage(SystemMessageId.PERIOD_S1_OF_THE_GRAND_OLYMPIAD_GAMES_HAS_STARTED);
		sm.addInt(_currentCycle);
		Broadcast.toAllOnlinePlayers(sm);
		
		if (!OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS)
		{
			final Calendar currentTime = Calendar.getInstance();
			currentTime.add(Calendar.MONTH, 1);
			currentTime.set(Calendar.DAY_OF_MONTH, 1);
			currentTime.set(Calendar.AM_PM, Calendar.AM);
			currentTime.set(Calendar.HOUR_OF_DAY, 12);
			currentTime.set(Calendar.MINUTE, 0);
			currentTime.set(Calendar.SECOND, 0);
			_olympiadEnd = currentTime.getTimeInMillis();
			
			final Calendar nextChange = Calendar.getInstance();
			_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
		}
		else
		{
			final Calendar currentTime = Calendar.getInstance();
			currentTime.set(Calendar.AM_PM, Calendar.AM);
			currentTime.set(Calendar.HOUR_OF_DAY, 12);
			currentTime.set(Calendar.MINUTE, 0);
			currentTime.set(Calendar.SECOND, 0);
			
			final Calendar nextChange = Calendar.getInstance();
			
			switch (OlympiadConfig.OLYMPIAD_PERIOD)
			{
				case "DAY":
				{
					currentTime.add(Calendar.DAY_OF_MONTH, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
					currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
					
					if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 14)
					{
						_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
					}
					else if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 7)
					{
						_nextWeeklyChange = nextChange.getTimeInMillis() + (WEEKLY_PERIOD / 2);
					}
					else
					{
						LOGGER.warning("Invalid config value for Config.OLYMPIAD_PERIOD_MULTIPLIER, must be >= 7");
					}
					break;
				}
				case "WEEK":
				{
					currentTime.add(Calendar.WEEK_OF_MONTH, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
					currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
					
					if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER > 1)
					{
						_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
					}
					else
					{
						_nextWeeklyChange = nextChange.getTimeInMillis() + (WEEKLY_PERIOD / 2);
					}
					break;
				}
				case "MONTH":
				{
					currentTime.add(Calendar.MONTH, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
					currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
					
					_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
					break;
				}
			}
			
			_olympiadEnd = currentTime.getTimeInMillis();
		}
		
		scheduleWeeklyChange();
	}
	
	public boolean inCompPeriod()
	{
		return _inCompPeriod;
	}
	
	private long getMillisToCompBegin()
	{
		final long currentTime = System.currentTimeMillis();
		if ((_compStart.getTimeInMillis() < currentTime) && (_compEnd > currentTime))
		{
			return 10;
		}
		
		if (_compStart.getTimeInMillis() > currentTime)
		{
			return _compStart.getTimeInMillis() - currentTime;
		}
		
		return setNewCompBegin();
	}
	
	private long setNewCompBegin()
	{
		_compStart = Calendar.getInstance();
		
		int currentDay = _compStart.get(Calendar.DAY_OF_WEEK);
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		
		// Today's competitions ended, start checking from next day.
		if (currentDay == _compStart.get(Calendar.DAY_OF_WEEK))
		{
			if (currentDay == Calendar.SATURDAY)
			{
				currentDay = Calendar.SUNDAY;
			}
			else
			{
				currentDay++;
			}
		}
		
		if (OlympiadConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS)
		{
			boolean dayFound = false;
			int dayCounter = 0;
			for (int i = currentDay; i < 8; i++)
			{
				if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
				{
					dayFound = true;
					break;
				}
				
				dayCounter++;
			}
			
			if (!dayFound)
			{
				for (int i = 1; i < 8; i++)
				{
					if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
					{
						break;
					}
					
					dayCounter++;
				}
			}
			
			if (dayCounter > 0)
			{
				_compStart.add(Calendar.DAY_OF_MONTH, dayCounter);
			}
		}
		
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;
		
		LOGGER.info("Olympiad System: New Schedule @ " + _compStart.getTime());
		
		return _compStart.getTimeInMillis() - System.currentTimeMillis();
	}
	
	protected long getMillisToCompEnd()
	{
		// if (_compEnd > System.currentTimeMillis())
		return (_compEnd - System.currentTimeMillis());
		// return 10L;
	}
	
	public long getMillisToWeekChange()
	{
		final long currentTime = System.currentTimeMillis();
		if (_nextWeeklyChange > currentTime)
		{
			return (_nextWeeklyChange - currentTime);
		}
		
		return 10L;
	}
	
	private void scheduleWeeklyChange()
	{
		_scheduledWeeklyTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			addWeeklyPoints();
			LOGGER.info("Olympiad System: Added weekly points to nobles");
			
			final Calendar nextChange = Calendar.getInstance();
			_nextWeeklyChange = nextChange.getTimeInMillis() + WEEKLY_PERIOD;
		}, getMillisToWeekChange(), WEEKLY_PERIOD);
	}
	
	protected void addWeeklyPoints()
	{
		if (_period == 1)
		{
			return;
		}
		
		for (StatSet nobleInfo : NOBLES.values())
		{
			nobleInfo.set(POINTS, MathUtil.clamp(nobleInfo.getInt(POINTS, 0) + WEEKLY_POINTS, 0, (nobleInfo.getInt(COMP_DONE, 0) * OlympiadConfig.OLYMPIAD_MAX_POINTS) + (OlympiadConfig.OLYMPIAD_WEEKLY_POINTS * 4)));
		}
	}
	
	public Map<Integer, String> getMatchList()
	{
		return OlympiadManager.getInstance().getAllTitles();
	}
	
	// returns the players for the given olympiad game Id
	public Player[] getPlayers(int id)
	{
		if (OlympiadManager.getInstance().getOlympiadGame(id) == null)
		{
			return null;
		}
		
		return OlympiadManager.getInstance().getOlympiadGame(id).getPlayers();
	}
	
	public int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public static void addSpectator(int id, Player spectator, boolean storeCoords)
	{
		if (!Olympiad.getInstance().inCompPeriod())
		{
			spectator.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
			return;
		}
		
		if (getInstance().isRegisteredInComp(spectator))
		{
			spectator.sendPacket(SystemMessageId.YOU_MAY_NOT_OBSERVE_A_GRAND_OLYMPIAD_GAMES_MATCH_WHILE_YOU_ARE_ON_THE_WAITING_LIST);
			return;
		}
		
		if (spectator.isRegisteredOnEvent())
		{
			spectator.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT);
			return;
		}
		
		OlympiadManager.STADIUMS[id].addSpectator(id, spectator, storeCoords);
		
		if (storeCoords)
		{
			handleSpectatorEnter(id, spectator); // npc bypass
		}
	}
	
	public static int getSpectatorArena(Player player)
	{
		for (int i = 0; i < OlympiadManager.STADIUMS.length; i++)
		{
			if (OlympiadManager.STADIUMS[i].getSpectators().contains(player))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public static void removeSpectator(int id, Player spectator)
	{
		try
		{
			OlympiadManager.STADIUMS[id].removeSpectator(spectator);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			LOGGER.log(Level.SEVERE, "removeSpectator()", e);
		}
	}
	
	public List<Player> getSpectators(int id)
	{
		try
		{
			if (OlympiadManager.getInstance().getOlympiadGame(id) == null)
			{
				return null;
			}
			
			return OlympiadManager.STADIUMS[id].getSpectators();
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return null;
		}
	}
	
	public Map<Integer, OlympiadGame> getOlympiadGames()
	{
		return OlympiadManager.getInstance().getOlympiadGames();
	}
	
	public boolean playerInStadia(Player player)
	{
		return (ZoneManager.getInstance().getOlympiadStadium(player) != null);
	}
	
	public int[] getWaitingList()
	{
		final int[] array = new int[2];
		
		if (!inCompPeriod())
		{
			return null;
		}
		
		int classCount = 0;
		
		if (_classBasedRegisters.size() != 0)
		{
			for (List<Player> classed : _classBasedRegisters.values())
			{
				classCount += classed.size();
			}
		}
		
		array[0] = classCount;
		array[1] = _nonClassBasedRegisters.size();
		
		return array;
	}
	
	/**
	 * Save noblesse data to database
	 */
	protected synchronized void saveNobleData()
	{
		if (NOBLES.isEmpty())
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final List<Entry<Integer, StatSet>> nobleList = new ArrayList<>(NOBLES.entrySet());
			for (Entry<Integer, StatSet> entry : nobleList)
			{
				final StatSet nobleInfo = entry.getValue();
				if (nobleInfo == null)
				{
					continue;
				}
				
				final int charId = entry.getKey();
				final int classId = nobleInfo.getInt(CLASS_ID);
				final int points = nobleInfo.getInt(POINTS);
				final int compDone = nobleInfo.getInt(COMP_DONE);
				final int compWon = nobleInfo.getInt(COMP_WON);
				final int compLost = nobleInfo.getInt(COMP_LOST);
				final int compDrawn = nobleInfo.getInt(COMP_DRAWN);
				final boolean toSave = nobleInfo.getBoolean("to_save");
				
				try (PreparedStatement statement = con.prepareStatement(toSave ? OLYMPIAD_SAVE_NOBLES : OLYMPIAD_UPDATE_NOBLES))
				{
					if (toSave)
					{
						statement.setInt(1, charId);
						statement.setInt(2, classId);
						statement.setInt(3, points);
						statement.setInt(4, compDone);
						statement.setInt(5, compWon);
						statement.setInt(6, compLost);
						statement.setInt(7, compDrawn);
						
						nobleInfo.set("to_save", false);
					}
					else
					{
						statement.setInt(1, points);
						statement.setInt(2, compDone);
						statement.setInt(3, compWon);
						statement.setInt(4, compLost);
						statement.setInt(5, compDrawn);
						statement.setInt(6, charId);
					}
					
					statement.execute();
					statement.close();
				}
				catch (SQLException e)
				{
					LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noble data for charId " + charId, e);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noblesse data to database: ", e);
		}
	}
	
	/**
	 * Save olympiad.properties file with current olympiad status and update noblesse table in database
	 */
	public void saveOlympiadStatus()
	{
		saveNobleData();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_SAVE_DATA))
		{
			statement.setInt(1, _currentCycle);
			statement.setInt(2, _period);
			statement.setLong(3, _olympiadEnd);
			statement.setLong(4, _validationEnd);
			statement.setLong(5, _nextWeeklyChange);
			statement.setInt(6, _currentCycle);
			statement.setInt(7, _period);
			statement.setLong(8, _olympiadEnd);
			statement.setLong(9, _validationEnd);
			statement.setLong(10, _nextWeeklyChange);
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save olympiad data to database: ", e);
		}
	}
	
	protected void updateMonthlyData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(OLYMPIAD_MONTH_CLEAR);
			PreparedStatement ps2 = con.prepareStatement(OLYMPIAD_MONTH_CREATE))
		{
			ps1.execute();
			ps2.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Olympiad System: Failed to update monthly noblese data: ", e);
		}
	}
	
	protected void sortHerosToBe()
	{
		if (_period != 1)
		{
			return;
		}
		
		LogRecord record;
		if (NOBLES != null)
		{
			LOGGER_OLYMPIAD.info("Noble,charid,classid,compDone,points");
			StatSet nobleInfo;
			for (Entry<Integer, StatSet> entry : NOBLES.entrySet())
			{
				nobleInfo = entry.getValue();
				if (nobleInfo == null)
				{
					continue;
				}
				
				final int charId = entry.getKey();
				final int classId = nobleInfo.getInt(CLASS_ID);
				final String charName = nobleInfo.getString(CHAR_NAME);
				final int points = nobleInfo.getInt(POINTS);
				final int compDone = nobleInfo.getInt(COMP_DONE);
				
				record = new LogRecord(Level.INFO, charName);
				record.setParameters(new Object[]
				{
					charId,
					classId,
					compDone,
					points
				});
				LOGGER_OLYMPIAD.log(record);
			}
		}
		
		HEROS_TO_BE = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_GET_HEROS))
		{
			StatSet hero;
			final List<StatSet> soulHounds = new ArrayList<>();
			for (int element : HERO_IDS)
			{
				statement.setInt(1, element);
				
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						hero = new StatSet();
						hero.set(CLASS_ID, element);
						hero.set(CHAR_ID, rset.getInt(CHAR_ID));
						hero.set(CHAR_NAME, rset.getString(CHAR_NAME));
						
						if ((element == 132) || (element == 133)) // Male & Female Soulhounds rank as one hero class
						{
							hero = NOBLES.get(hero.getInt(CHAR_ID));
							hero.set(CHAR_ID, rset.getInt(CHAR_ID));
							soulHounds.add(hero);
						}
						else
						{
							record = new LogRecord(Level.INFO, "Hero " + hero.getString(CHAR_NAME));
							record.setParameters(new Object[]
							{
								hero.getInt(CHAR_ID),
								hero.getInt(CLASS_ID)
							});
							LOGGER_OLYMPIAD.log(record);
							HEROS_TO_BE.add(hero);
						}
					}
				}
			}
			
			switch (soulHounds.size())
			{
				case 0:
				{
					break;
				}
				case 1:
				{
					hero = new StatSet();
					final StatSet winner = soulHounds.get(0);
					hero.set(CLASS_ID, winner.getInt(CLASS_ID));
					hero.set(CHAR_ID, winner.getInt(CHAR_ID));
					hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
					
					record = new LogRecord(Level.INFO, "Hero " + hero.getString(CHAR_NAME));
					record.setParameters(new Object[]
					{
						hero.getInt(CHAR_ID),
						hero.getInt(CLASS_ID)
					});
					LOGGER_OLYMPIAD.log(record);
					HEROS_TO_BE.add(hero);
					break;
				}
				case 2:
				{
					hero = new StatSet();
					StatSet winner;
					final StatSet hero1 = soulHounds.get(0);
					final StatSet hero2 = soulHounds.get(1);
					final int hero1Points = hero1.getInt(POINTS);
					final int hero2Points = hero2.getInt(POINTS);
					final int hero1Comps = hero1.getInt(COMP_DONE);
					final int hero2Comps = hero2.getInt(COMP_DONE);
					final int hero1Wins = hero1.getInt(COMP_WON);
					final int hero2Wins = hero2.getInt(COMP_WON);
					
					if (hero1Points > hero2Points)
					{
						winner = hero1;
					}
					else if (hero2Points > hero1Points)
					{
						winner = hero2;
					}
					else
					{
						if (hero1Comps > hero2Comps)
						{
							winner = hero1;
						}
						else if (hero2Comps > hero1Comps)
						{
							winner = hero2;
						}
						else
						{
							if (hero1Wins > hero2Wins)
							{
								winner = hero1;
							}
							else
							{
								winner = hero2;
							}
						}
					}
					
					hero.set(CLASS_ID, winner.getInt(CLASS_ID));
					hero.set(CHAR_ID, winner.getInt(CHAR_ID));
					hero.set(CHAR_NAME, winner.getString(CHAR_NAME));
					
					record = new LogRecord(Level.INFO, "Hero " + hero.getString(CHAR_NAME));
					record.setParameters(new Object[]
					{
						hero.getInt(CHAR_ID),
						hero.getInt(CLASS_ID)
					});
					LOGGER_OLYMPIAD.log(record);
					HEROS_TO_BE.add(hero);
					break;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Could not load heros from DB");
		}
	}
	
	public List<String> getClassLeaderBoard(int classId)
	{
		final List<String> names = new ArrayList<>();
		final String query = OlympiadConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS ? ((classId == 132) ? GET_EACH_CLASS_LEADER_SOULHOUND : GET_EACH_CLASS_LEADER) : ((classId == 132) ? GET_EACH_CLASS_LEADER_CURRENT_SOULHOUND : GET_EACH_CLASS_LEADER_CURRENT);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(query))
		{
			ps.setInt(1, classId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					names.add(rset.getString(CHAR_NAME));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Couldn't load olympiad leaders from DB!");
		}
		
		return names;
	}
	
	private int getNoblessePasses(int objectId)
	{
		if ((_period != 1) || NOBLES_RANK.isEmpty())
		{
			return 0;
		}
		
		if (!NOBLES_RANK.containsKey(objectId))
		{
			return 0;
		}
		
		final StatSet noble = NOBLES.get(objectId);
		if ((noble == null) || (noble.getInt(POINTS) == 0))
		{
			return 0;
		}
		
		// Hero point bonus
		int points = Hero.getInstance().isHero(objectId) ? OlympiadConfig.OLYMPIAD_HERO_POINTS : 0;
		
		points *= OlympiadConfig.OLYMPIAD_GP_PER_POINT;
		
		// This is a one time calculation.
		noble.set(POINTS, 0);
		
		return points;
	}
	
	public boolean isRegisteredInComp(Player player)
	{
		boolean result = isRegistered(player);
		
		if (_inCompPeriod)
		{
			for (OlympiadGame game : OlympiadManager.getInstance().getOlympiadGames().values())
			{
				if ((game._playerOneID == player.getObjectId()) || (game._playerTwoID == player.getObjectId()))
				{
					result = true;
					break;
				}
			}
		}
		
		return result;
	}
	
	public int getNoblePoints(int objId)
	{
		if (NOBLES.isEmpty())
		{
			return 0;
		}
		
		final StatSet noble = NOBLES.get(objId);
		if (noble == null)
		{
			return 0;
		}
		
		return noble.getInt(POINTS);
	}
	
	public int getLastNobleOlympiadPoints(int objId)
	{
		int result = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT olympiad_points FROM olympiad_nobles_eom WHERE charId = ?"))
		{
			ps.setInt(1, objId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.first())
				{
					result = rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load last olympiad points:", e);
		}
		
		return result;
	}
	
	public int getCompetitionDone(int objId)
	{
		if (NOBLES.isEmpty())
		{
			return 0;
		}
		
		final StatSet noble = NOBLES.get(objId);
		if (noble == null)
		{
			return 0;
		}
		
		return noble.getInt(COMP_DONE);
	}
	
	public int getCompetitionWon(int objId)
	{
		if (NOBLES.isEmpty())
		{
			return 0;
		}
		
		final StatSet noble = NOBLES.get(objId);
		if (noble == null)
		{
			return 0;
		}
		
		return noble.getInt(COMP_WON);
	}
	
	public int getCompetitionLost(int objId)
	{
		if (NOBLES.isEmpty())
		{
			return 0;
		}
		
		final StatSet noble = NOBLES.get(objId);
		if (noble == null)
		{
			return 0;
		}
		
		return noble.getInt(COMP_LOST);
	}
	
	protected void deleteNobles()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_DELETE_ALL))
		{
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Couldn't delete nobles from DB!");
		}
		
		NOBLES.clear();
	}
	
	public static void sendMatchList(Player player)
	{
		final NpcHtmlMessage message = new NpcHtmlMessage(0);
		message.setFile(player, Olympiad.OLYMPIAD_HTML_PATH + "olympiad_observe2.htm");
		
		final Map<Integer, String> matches = getInstance().getMatchList();
		for (int i = 0; i < Olympiad.getStadiumCount(); i++)
		{
			final int arenaId = i + 1;
			String state = "Initial State";
			String players = "&nbsp;";
			if (matches.containsKey(i))
			{
				if (OlympiadGame._gameIsStarted)
				{
					state = "Playing";
				}
				else
				{
					state = "Standby";
				}
				
				players = matches.get(i);
			}
			
			message.replace("%state" + arenaId + "%", state);
			message.replace("%players" + arenaId + "%", players);
		}
		
		player.sendPacket(message);
	}
	
	private static void handleSpectatorEnter(int gameId, Player player)
	{
		final OlympiadGame game = OlympiadManager.getInstance().getOlympiadGame(gameId);
		if (game != null)
		{
			if ((game._playerOne == null) || !game._playerOne.isInOlympiadMode() || !game._playerOne.isOlympiadStart())
			{
				return;
			}
			
			if ((game._playerTwo == null) || !game._playerTwo.isInOlympiadMode() || !game._playerTwo.isOlympiadStart())
			{
				return;
			}
			
			player.sendPacket(new ExOlympiadUserInfo(game._playerOne, 1));
			player.sendPacket(new ExOlympiadUserInfo(game._playerTwo, 2));
		}
	}
	
	public static void bypassChangeArena(String command, Player player)
	{
		if (!player.inObserverMode())
		{
			return;
		}
		
		final String[] commands = command.split(" ");
		final int id = Integer.parseInt(commands[1]);
		final int arena = getSpectatorArena(player);
		if (arena >= 0)
		{
			Olympiad.removeSpectator(arena, player);
			player.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
		}
		else
		{
			return;
		}
		
		Olympiad.addSpectator(id, player, false);
		
		handleSpectatorEnter(id, player);
	}
	
	public static Olympiad getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final Olympiad _instance = new Olympiad();
	}
}
