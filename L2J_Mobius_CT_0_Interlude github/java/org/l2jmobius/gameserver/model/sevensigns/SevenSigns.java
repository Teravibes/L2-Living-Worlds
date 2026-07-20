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
package org.l2jmobius.gameserver.model.sevensigns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.spawns.AutoSpawnHandler;
import org.l2jmobius.gameserver.model.spawns.AutoSpawnHandler.AutoSpawnInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Seven Signs engine.
 * @author Tempy
 */
public class SevenSigns
{
	protected static final Logger LOGGER = Logger.getLogger(SevenSigns.class.getName());
	
	// Basic Seven Signs Constants
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
	
	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;
	
	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;
	
	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;
	
	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 00;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;
	
	// The quest event and seal validation periods last for approximately one week
	// with a 15 minutes "interval" period sandwiched between them.
	public static final int PERIOD_MINOR_LENGTH = 900000;
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;
	
	public static final int RECORD_SEVEN_SIGNS_ID = 5707;
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	
	// NPC Related Constants
	public static final int ORATOR_NPC_ID = 31094;
	public static final int PREACHER_NPC_ID = 31093;
	public static final int MAMMON_MERCHANT_ID = 31113;
	public static final int MAMMON_BLACKSMITH_ID = 31126;
	public static final int MAMMON_MARKETEER_ID = 31092;
	public static final int LILITH_NPC_ID = 25283;
	public static final int ANAKIM_NPC_ID = 25286;
	public static final int CREST_OF_DAWN_ID = 31170;
	public static final int CREST_OF_DUSK_ID = 31171;
	
	// Seal Stone Related Constants
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;
	
	protected static final int[] SEAL_STONE_IDS =
	{
		SEAL_STONE_BLUE_ID,
		SEAL_STONE_GREEN_ID,
		SEAL_STONE_RED_ID
	};
	
	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;
	
	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;
	
	private final Calendar _nextPeriodChange = Calendar.getInstance();
	
	protected int _activePeriod;
	protected int _currentCycle;
	protected double _dawnStoneScore;
	protected double _duskStoneScore;
	protected int _dawnFestivalScore;
	protected int _duskFestivalScore;
	protected int _compWinner;
	protected int _previousWinner;
	protected Calendar _lastSave = Calendar.getInstance();
	
	protected Map<Integer, StatSet> _signsPlayerData = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsSealOwners = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsDuskSealTotals = new LinkedHashMap<>();
	private final Map<Integer, Integer> _signsDawnSealTotals = new LinkedHashMap<>();
	
	private static final String LOAD_DATA = "SELECT charId, cabal, seal, red_stones, green_stones, blue_stones, ancient_adena_amount, contribution_score FROM seven_signs";
	
	private static final String LOAD_STATUS = "SELECT * FROM seven_signs_status WHERE id=0";
	
	private static final String INSERT_PLAYER = "INSERT INTO seven_signs (charId, cabal, seal) VALUES (?,?,?)";
	
	private static final String UPDATE_PLAYER = "UPDATE seven_signs SET cabal=?, seal=?, red_stones=?, green_stones=?, blue_stones=?, ancient_adena_amount=?, contribution_score=? WHERE charId=?";
	
	private static final String UPDATE_STATUS = "UPDATE seven_signs_status SET current_cycle=?, active_period=?, previous_winner=?, dawn_stone_score=?, dawn_festival_score=?, dusk_stone_score=?, dusk_festival_score=?, avarice_owner=?, gnosis_owner=?, strife_owner=?, avarice_dawn_score=?, gnosis_dawn_score=?, strife_dawn_score=?, avarice_dusk_score=?, gnosis_dusk_score=?, strife_dusk_score=?, festival_cycle=?, accumulated_bonus0=?, accumulated_bonus1=?, accumulated_bonus2=?,accumulated_bonus3=?, accumulated_bonus4=?, date=? WHERE id=0";
	
	protected SevenSigns()
	{
		try
		{
			restoreSevenSignsData();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Failed to load configuration: " + e.getMessage(), e);
		}
		
		LOGGER.info("SevenSigns: Currently in the " + getCurrentPeriodName() + " period!");
		initializeSeals();
		
		if (isSealValidationPeriod())
		{
			if (getCabalHighestScore() == CABAL_NULL)
			{
				LOGGER.info("SevenSigns: The competition ended with a tie last week.");
			}
			else
			{
				LOGGER.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " were victorious last week.");
			}
		}
		else if (getCabalHighestScore() == CABAL_NULL)
		{
			LOGGER.info("SevenSigns: The competition, if the current trend continues, will end in a tie this week.");
		}
		else
		{
			LOGGER.info("SevenSigns: The " + getCabalName(getCabalHighestScore()) + " are in the lead this week.");
		}
		
		// Solo "no schedule" mode: never arm the automatic weekly period change.
		// The cycle only advances when driven manually (admin //signs command or the
		// Dawn/Dusk priest dialog), so the restored period is left exactly as it was.
		if (FeatureConfig.ALT_SEVENSIGNS_NO_SCHEDULE)
		{
			LOGGER.info("SevenSigns: No-schedule mode is enabled - the period will not change automatically. Use //signs or a Priest of Dawn/Priestess of Dusk to advance it.");
			return;
		}

		long milliToChange = 0;
		if (isNextPeriodChangeInPast())
		{
			LOGGER.info("SevenSigns: Next period change was in the past (server was offline), changing periods now!");
		}
		else
		{
			setCalendarForNextPeriodChange();
			milliToChange = getMilliToPeriodChange();
		}

		// Schedule a time for the next period change.
		ThreadPool.schedule(new SevenSignsPeriodChange(), milliToChange);
		LOGGER.info("SevenSigns: Next period begins in " + TimeUtil.formatDuration(milliToChange));
	}
	
	private boolean isNextPeriodChangeInPast()
	{
		final Calendar lastPeriodChange = Calendar.getInstance();
		switch (_activePeriod)
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
			{
				lastPeriodChange.set(Calendar.DAY_OF_WEEK, PERIOD_START_DAY);
				lastPeriodChange.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				lastPeriodChange.set(Calendar.MINUTE, PERIOD_START_MINS);
				lastPeriodChange.set(Calendar.SECOND, 0);
				
				// if we hit next week, just turn back 1 week
				if (Calendar.getInstance().before(lastPeriodChange))
				{
					lastPeriodChange.add(Calendar.HOUR, -24 * 7);
				}
				break;
			}
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
			{
				// because of the short duration of this period, just check it from last save
				lastPeriodChange.setTimeInMillis(_lastSave.getTimeInMillis() + PERIOD_MINOR_LENGTH);
				break;
			}
		}
		
		// because of previous "date" column usage, check only if it already contains usable data for us
		return (_lastSave.getTimeInMillis() > 7) && _lastSave.before(lastPeriodChange);
	}
	
	/**
	 * Registers all random spawns and auto-chats for Seven Signs NPCs, along with spawns for the Preachers of Doom and Orators of Revelations at the beginning of the Seal Validation period.
	 */
	public void spawnSevenSignsNPC()
	{
		final AutoSpawnInstance merchantSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_MERCHANT_ID, false);
		final AutoSpawnInstance blacksmithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(MAMMON_BLACKSMITH_ID, false);
		final AutoSpawnInstance lilithSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(LILITH_NPC_ID, false);
		final AutoSpawnInstance anakimSpawn = AutoSpawnHandler.getInstance().getAutoSpawnInstance(ANAKIM_NPC_ID, false);
		final List<AutoSpawnInstance> crestOfDawnSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(CREST_OF_DAWN_ID);
		final List<AutoSpawnInstance> crestOfDuskSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(CREST_OF_DUSK_ID);
		final List<AutoSpawnInstance> oratorSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(ORATOR_NPC_ID);
		final List<AutoSpawnInstance> preacherSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(PREACHER_NPC_ID);
		final List<AutoSpawnInstance> marketeerSpawns = AutoSpawnHandler.getInstance().getAutoSpawnInstances(MAMMON_MARKETEER_ID);
		if (isSealValidationPeriod() || isCompResultsPeriod())
		{
			for (AutoSpawnInstance spawnInst : marketeerSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
			}
			
			if ((getSealOwner(SEAL_GNOSIS) == getCabalHighestScore()) && (getSealOwner(SEAL_GNOSIS) != CABAL_NULL))
			{
				if (!NpcConfig.ANNOUNCE_MAMMON_SPAWN)
				{
					blacksmithSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(blacksmithSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, true);
				}
				
				for (AutoSpawnInstance spawnInst : oratorSpawns)
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
				
				for (AutoSpawnInstance spawnInst : preacherSpawns)
				{
					if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(spawnInst.getObjectId(), true).isSpawnActive())
					{
						AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, true);
					}
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, false);
				for (AutoSpawnInstance spawnInst : oratorSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
				
				for (AutoSpawnInstance spawnInst : preacherSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
				}
			}
			
			if ((getSealOwner(SEAL_AVARICE) == getCabalHighestScore()) && (getSealOwner(SEAL_AVARICE) != CABAL_NULL))
			{
				if (!NpcConfig.ANNOUNCE_MAMMON_SPAWN)
				{
					merchantSpawn.setBroadcast(false);
				}
				
				if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(merchantSpawn.getObjectId(), true).isSpawnActive())
				{
					AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, true);
				}
				
				switch (getCabalHighestScore())
				{
					case CABAL_DAWN:
					{
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(lilithSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);
						for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
						{
							if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(dawnCrest.getObjectId(), true).isSpawnActive())
							{
								AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, true);
							}
						}
						
						for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
						{
							AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
						}
						break;
					}
					case CABAL_DUSK:
					{
						if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(anakimSpawn.getObjectId(), true).isSpawnActive())
						{
							AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, true);
						}
						
						AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
						for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
						{
							if (!AutoSpawnHandler.getInstance().getAutoSpawnInstance(duskCrest.getObjectId(), true).isSpawnActive())
							{
								AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, true);
							}
						}
						
						for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
						{
							AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
						}
						break;
					}
				}
			}
			else
			{
				AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
				AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);
				for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
				}
				
				for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
				{
					AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
				}
			}
		}
		else
		{
			AutoSpawnHandler.getInstance().setSpawnActive(merchantSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(blacksmithSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(lilithSpawn, false);
			AutoSpawnHandler.getInstance().setSpawnActive(anakimSpawn, false);
			for (AutoSpawnInstance dawnCrest : crestOfDawnSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(dawnCrest, false);
			}
			
			for (AutoSpawnInstance duskCrest : crestOfDuskSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(duskCrest, false);
			}
			
			for (AutoSpawnInstance spawnInst : oratorSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (AutoSpawnInstance spawnInst : preacherSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
			
			for (AutoSpawnInstance spawnInst : marketeerSpawns)
			{
				AutoSpawnHandler.getInstance().setSpawnActive(spawnInst, false);
			}
		}
	}
	
	public static SevenSigns getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	public static int calcContributionScore(int blueCount, int greenCount, int redCount)
	{
		int contrib = blueCount * BLUE_CONTRIB_POINTS;
		contrib += greenCount * GREEN_CONTRIB_POINTS;
		contrib += redCount * RED_CONTRIB_POINTS;
		return contrib;
	}
	
	public static int calcAncientAdenaReward(int blueCount, int greenCount, int redCount)
	{
		int reward = blueCount * SEAL_STONE_BLUE_VALUE;
		reward += greenCount * SEAL_STONE_GREEN_VALUE;
		reward += redCount * SEAL_STONE_RED_VALUE;
		return reward;
	}
	
	public static String getCabalShortName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
			{
				return "dawn";
			}
			case CABAL_DUSK:
			{
				return "dusk";
			}
		}
		
		return "No Cabal";
	}
	
	public static String getCabalName(int cabal)
	{
		switch (cabal)
		{
			case CABAL_DAWN:
			{
				return "Lords of Dawn";
			}
			case CABAL_DUSK:
			{
				return "Revolutionaries of Dusk";
			}
		}
		
		return "No Cabal";
	}
	
	public static String getSealName(int seal, boolean shortName)
	{
		String sealName = (!shortName) ? "Seal of " : "";
		
		switch (seal)
		{
			case SEAL_AVARICE:
			{
				sealName += "Avarice";
				break;
			}
			case SEAL_GNOSIS:
			{
				sealName += "Gnosis";
				break;
			}
			case SEAL_STRIFE:
			{
				sealName += "Strife";
				break;
			}
		}
		
		return sealName;
	}
	
	public int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	public int getCurrentPeriod()
	{
		return _activePeriod;
	}
	
	private final int getDaysToPeriodChange()
	{
		final int numDays = _nextPeriodChange.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;
		if (numDays < 0)
		{
			return 0 - numDays;
		}
		
		return 7 - numDays;
	}
	
	public long getMilliToPeriodChange()
	{
		return (_nextPeriodChange.getTimeInMillis() - System.currentTimeMillis());
	}
	
	protected void setCalendarForNextPeriodChange()
	{
		// Calculate the number of days until the next period
		// A period starts at 18:00 pm (local time), like on official servers.
		switch (_activePeriod)
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
			{
				int daysToChange = getDaysToPeriodChange();
				if (daysToChange == 7)
				{
					if (_nextPeriodChange.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
					{
						daysToChange = 0;
					}
					else if ((_nextPeriodChange.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR) && (_nextPeriodChange.get(Calendar.MINUTE) < PERIOD_START_MINS))
					{
						daysToChange = 0;
					}
				}
				
				// Otherwise...
				if (daysToChange > 0)
				{
					_nextPeriodChange.add(Calendar.DATE, daysToChange);
				}
				
				_nextPeriodChange.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_nextPeriodChange.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			}
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
			{
				_nextPeriodChange.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
			}
		}
		
		LOGGER.info("SevenSigns: Next period change set to " + _nextPeriodChange.getTime());
	}
	
	public String getCurrentPeriodName()
	{
		String periodName = null;
		
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
			{
				periodName = "Quest Event Initialization";
				break;
			}
			case PERIOD_COMPETITION:
			{
				periodName = "Competition (Quest Event)";
				break;
			}
			case PERIOD_COMP_RESULTS:
			{
				periodName = "Quest Event Results";
				break;
			}
			case PERIOD_SEAL_VALIDATION:
			{
				periodName = "Seal Validation";
				break;
			}
		}
		
		return periodName;
	}
	
	/**
	 * @return {@code true} if it's competition period, {@code false} otherwise
	 */
	public boolean isCompetitionPeriod()
	{
		return (_activePeriod == PERIOD_COMPETITION);
	}
	
	public boolean isSealValidationPeriod()
	{
		return (_activePeriod == PERIOD_SEAL_VALIDATION);
	}
	
	public boolean isCompResultsPeriod()
	{
		return (_activePeriod == PERIOD_COMP_RESULTS);
	}
	
	/**
	 * returns true if the given date is in Seal Validation or in Quest Event Results period
	 * @param date
	 * @return
	 */
	public boolean isDateInSealValidPeriod(Calendar date)
	{
		final long nextPeriodChange = getMilliToPeriodChange();
		long nextQuestStart = 0;
		long nextValidStart = 0;
		long tillDate = date.getTimeInMillis() - System.currentTimeMillis();
		while (((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH)) < tillDate)
		{
			tillDate -= ((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH));
		}
		while (tillDate < 0)
		{
			tillDate += ((2 * PERIOD_MAJOR_LENGTH) + (2 * PERIOD_MINOR_LENGTH));
		}
		
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
			{
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextQuestStart = nextValidStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			}
			case PERIOD_COMPETITION:
			{
				nextValidStart = nextPeriodChange;
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			}
			case PERIOD_COMP_RESULTS:
			{
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextValidStart = nextQuestStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			}
			case PERIOD_SEAL_VALIDATION:
			{
				nextQuestStart = nextPeriodChange;
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			}
		}
		
		if (((nextQuestStart < tillDate) && (tillDate < nextValidStart)) || ((nextValidStart < nextQuestStart) && ((tillDate < nextValidStart) || (nextQuestStart < tillDate))))
		{
			return false;
		}
		
		return true;
	}
	
	public int getCurrentScore(int cabal)
	{
		final double totalStoneScore = _dawnStoneScore + _duskStoneScore;
		
		switch (cabal)
		{
			case CABAL_NULL:
			{
				return 0;
			}
			case CABAL_DAWN:
			{
				return Math.round((float) (_dawnStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _dawnFestivalScore;
			}
			case CABAL_DUSK:
			{
				return Math.round((float) (_duskStoneScore / ((float) totalStoneScore == 0 ? 1 : totalStoneScore)) * 500) + _duskFestivalScore;
			}
		}
		
		return 0;
	}
	
	public double getCurrentStoneScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL:
			{
				return 0;
			}
			case CABAL_DAWN:
			{
				return _dawnStoneScore;
			}
			case CABAL_DUSK:
			{
				return _duskStoneScore;
			}
		}
		
		return 0;
	}
	
	public int getCurrentFestivalScore(int cabal)
	{
		switch (cabal)
		{
			case CABAL_NULL:
			{
				return 0;
			}
			case CABAL_DAWN:
			{
				return _dawnFestivalScore;
			}
			case CABAL_DUSK:
			{
				return _duskFestivalScore;
			}
		}
		
		return 0;
	}
	
	public int getCabalHighestScore()
	{
		if (getCurrentScore(CABAL_DUSK) == getCurrentScore(CABAL_DAWN))
		{
			return CABAL_NULL;
		}
		else if (getCurrentScore(CABAL_DUSK) > getCurrentScore(CABAL_DAWN))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_DAWN;
		}
	}
	
	public int getSealOwner(int seal)
	{
		return _signsSealOwners.get(seal);
	}
	
	public int getSealProportion(int seal, int cabal)
	{
		if (cabal == CABAL_NULL)
		{
			return 0;
		}
		else if (cabal == CABAL_DUSK)
		{
			return _signsDuskSealTotals.get(seal);
		}
		else
		{
			return _signsDawnSealTotals.get(seal);
		}
	}
	
	public int getTotalMembers(int cabal)
	{
		int cabalMembers = 0;
		final String cabalName = getCabalShortName(cabal);
		for (StatSet sevenDat : _signsPlayerData.values())
		{
			if (sevenDat.getString("cabal").equals(cabalName))
			{
				cabalMembers++;
			}
		}
		
		return cabalMembers;
	}
	
	public int getPlayerStoneContrib(int objectId)
	{
		final StatSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		int stoneCount = 0;
		stoneCount += currPlayer.getInt("red_stones");
		stoneCount += currPlayer.getInt("green_stones");
		stoneCount += currPlayer.getInt("blue_stones");
		return stoneCount;
	}
	
	public int getPlayerContribScore(int objectId)
	{
		final StatSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		return currPlayer.getInt("contribution_score");
	}
	
	public int getPlayerAdenaCollect(int objectId)
	{
		final StatSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return 0;
		}
		
		return currPlayer.getInt("ancient_adena_amount");
	}
	
	public int getPlayerSeal(int objectId)
	{
		final StatSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return SEAL_NULL;
		}
		
		return currPlayer.getInt("seal");
	}
	
	public int getPlayerCabal(int objectId)
	{
		final StatSet currPlayer = _signsPlayerData.get(objectId);
		if (currPlayer == null)
		{
			return CABAL_NULL;
		}
		
		final String playerCabal = currPlayer.getString("cabal");
		if (playerCabal.equalsIgnoreCase("dawn"))
		{
			return CABAL_DAWN;
		}
		else if (playerCabal.equalsIgnoreCase("dusk"))
		{
			return CABAL_DUSK;
		}
		else
		{
			return CABAL_NULL;
		}
	}
	
	/**
	 * Restores all Seven Signs data and settings, usually called at server startup.
	 */
	protected void restoreSevenSignsData()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(LOAD_DATA))
			{
				StatSet sevenDat = null;
				int charObjId;
				while (rs.next())
				{
					charObjId = rs.getInt("charId");
					sevenDat = new StatSet();
					sevenDat.set("charId", charObjId);
					sevenDat.set("cabal", rs.getString("cabal"));
					sevenDat.set("seal", rs.getInt("seal"));
					sevenDat.set("red_stones", rs.getInt("red_stones"));
					sevenDat.set("green_stones", rs.getInt("green_stones"));
					sevenDat.set("blue_stones", rs.getInt("blue_stones"));
					sevenDat.set("ancient_adena_amount", rs.getDouble("ancient_adena_amount"));
					sevenDat.set("contribution_score", rs.getDouble("contribution_score"));
					_signsPlayerData.put(charObjId, sevenDat);
				}
			}
			
			try (Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(LOAD_STATUS))
			{
				while (rs.next())
				{
					_currentCycle = rs.getInt("current_cycle");
					_activePeriod = rs.getInt("active_period");
					_previousWinner = rs.getInt("previous_winner");
					_dawnStoneScore = rs.getDouble("dawn_stone_score");
					_dawnFestivalScore = rs.getInt("dawn_festival_score");
					_duskStoneScore = rs.getDouble("dusk_stone_score");
					_duskFestivalScore = rs.getInt("dusk_festival_score");
					_signsSealOwners.put(SEAL_AVARICE, rs.getInt("avarice_owner"));
					_signsSealOwners.put(SEAL_GNOSIS, rs.getInt("gnosis_owner"));
					_signsSealOwners.put(SEAL_STRIFE, rs.getInt("strife_owner"));
					
					_signsDawnSealTotals.put(SEAL_AVARICE, rs.getInt("avarice_dawn_score"));
					_signsDawnSealTotals.put(SEAL_GNOSIS, rs.getInt("gnosis_dawn_score"));
					_signsDawnSealTotals.put(SEAL_STRIFE, rs.getInt("strife_dawn_score"));
					_signsDuskSealTotals.put(SEAL_AVARICE, rs.getInt("avarice_dusk_score"));
					_signsDuskSealTotals.put(SEAL_GNOSIS, rs.getInt("gnosis_dusk_score"));
					_signsDuskSealTotals.put(SEAL_STRIFE, rs.getInt("strife_dusk_score"));
					
					_lastSave.setTimeInMillis(rs.getLong("date"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Unable to load Seven Signs data from database: " + e.getMessage(), e);
		}
		
		// Festival data is loaded now after the Seven Signs engine data.
	}
	
	/**
	 * Saves all Seven Signs player data.<br>
	 * Should be called on period change and shutdown only.
	 */
	public void saveSevenSignsData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_PLAYER))
		{
			for (StatSet sevenDat : _signsPlayerData.values())
			{
				ps.setString(1, sevenDat.getString("cabal"));
				ps.setInt(2, sevenDat.getInt("seal"));
				ps.setInt(3, sevenDat.getInt("red_stones"));
				ps.setInt(4, sevenDat.getInt("green_stones"));
				ps.setInt(5, sevenDat.getInt("blue_stones"));
				ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
				ps.setDouble(7, sevenDat.getDouble("contribution_score"));
				ps.setInt(8, sevenDat.getInt("charId"));
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Unable to save data to database: " + e.getMessage(), e);
		}
	}
	
	public void saveSevenSignsData(int objectId)
	{
		final StatSet sevenDat = _signsPlayerData.get(objectId);
		if (sevenDat == null)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_PLAYER))
		{
			ps.setString(1, sevenDat.getString("cabal"));
			ps.setInt(2, sevenDat.getInt("seal"));
			ps.setInt(3, sevenDat.getInt("red_stones"));
			ps.setInt(4, sevenDat.getInt("green_stones"));
			ps.setInt(5, sevenDat.getInt("blue_stones"));
			ps.setDouble(6, sevenDat.getDouble("ancient_adena_amount"));
			ps.setDouble(7, sevenDat.getDouble("contribution_score"));
			ps.setInt(8, sevenDat.getInt("charId"));
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Unable to save data to database: " + e.getMessage(), e);
		}
	}
	
	public void saveSevenSignsStatus()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_STATUS))
		{
			ps.setInt(1, _currentCycle);
			ps.setInt(2, _activePeriod);
			ps.setInt(3, _previousWinner);
			ps.setDouble(4, _dawnStoneScore);
			ps.setInt(5, _dawnFestivalScore);
			ps.setDouble(6, _duskStoneScore);
			ps.setInt(7, _duskFestivalScore);
			ps.setInt(8, _signsSealOwners.get(SEAL_AVARICE));
			ps.setInt(9, _signsSealOwners.get(SEAL_GNOSIS));
			ps.setInt(10, _signsSealOwners.get(SEAL_STRIFE));
			ps.setInt(11, _signsDawnSealTotals.get(SEAL_AVARICE));
			ps.setInt(12, _signsDawnSealTotals.get(SEAL_GNOSIS));
			ps.setInt(13, _signsDawnSealTotals.get(SEAL_STRIFE));
			ps.setInt(14, _signsDuskSealTotals.get(SEAL_AVARICE));
			ps.setInt(15, _signsDuskSealTotals.get(SEAL_GNOSIS));
			ps.setInt(16, _signsDuskSealTotals.get(SEAL_STRIFE));
			ps.setInt(17, SevenSignsFestival.getInstance().getCurrentFestivalCycle());
			for (int i = 0; i < SevenSignsFestival.FESTIVAL_COUNT; i++)
			{
				ps.setInt(18 + i, SevenSignsFestival.getInstance().getAccumulatedBonus(i));
			}
			
			_lastSave = Calendar.getInstance();
			ps.setLong(18 + SevenSignsFestival.FESTIVAL_COUNT, _lastSave.getTimeInMillis());
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "SevenSigns: Unable to save data to database: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Used to reset the cabal details of all players, and update the database.<br>
	 * Primarily used when beginning a new cycle, and should otherwise never be called.
	 */
	protected void resetPlayerData()
	{
		// Reset each player's contribution data as well as seal and cabal.
		for (StatSet sevenDat : _signsPlayerData.values())
		{
			// Reset the player's cabal and seal information
			sevenDat.set("cabal", "");
			sevenDat.set("seal", SEAL_NULL);
			sevenDat.set("contribution_score", 0);
		}
	}
	
	/**
	 * Used to specify cabal-related details for the specified player.<br>
	 * This method checks to see if the player has registered before and will update the database if necessary.
	 * @param objectId
	 * @param chosenCabal
	 * @param chosenSeal
	 * @return the cabal ID the player has joined.
	 */
	public int setPlayerInfo(int objectId, int chosenCabal, int chosenSeal)
	{
		StatSet currPlayerData = _signsPlayerData.get(objectId);
		if (currPlayerData != null)
		{
			// If the seal validation period has passed,
			// cabal information was removed and so "re-register" player
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			_signsPlayerData.put(objectId, currPlayerData);
		}
		else
		{
			currPlayerData = new StatSet();
			currPlayerData.set("charId", objectId);
			currPlayerData.set("cabal", getCabalShortName(chosenCabal));
			currPlayerData.set("seal", chosenSeal);
			currPlayerData.set("red_stones", 0);
			currPlayerData.set("green_stones", 0);
			currPlayerData.set("blue_stones", 0);
			currPlayerData.set("ancient_adena_amount", 0);
			currPlayerData.set("contribution_score", 0);
			_signsPlayerData.put(objectId, currPlayerData);
			
			// Update data in database, as we have a new player signing up.
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(INSERT_PLAYER))
			{
				ps.setInt(1, objectId);
				ps.setString(2, getCabalShortName(chosenCabal));
				ps.setInt(3, chosenSeal);
				ps.execute();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.SEVERE, "SevenSigns: Failed to save data: " + e.getMessage(), e);
			}
		}
		
		// Increasing Seal total score for the player chosen Seal.
		if ("dawn".equals(currPlayerData.getString("cabal")))
		{
			_signsDawnSealTotals.put(chosenSeal, _signsDawnSealTotals.get(chosenSeal) + 1);
		}
		else
		{
			_signsDuskSealTotals.put(chosenSeal, _signsDuskSealTotals.get(chosenSeal) + 1);
		}
		
		if (!FeatureConfig.ALT_SEVENSIGNS_LAZY_UPDATE)
		{
			saveSevenSignsStatus();
		}
		
		return chosenCabal;
	}
	
	/**
	 * Returns the amount of ancient adena the specified player can claim, if any.<br>
	 * If removeReward = True, all the ancient adena owed to them is removed, then DB is updated.
	 * @param objectId
	 * @param removeReward
	 * @return
	 */
	public int getAncientAdenaReward(int objectId, boolean removeReward)
	{
		final StatSet currPlayer = _signsPlayerData.get(objectId);
		final int rewardAmount = currPlayer.getInt("ancient_adena_amount");
		currPlayer.set("red_stones", 0);
		currPlayer.set("green_stones", 0);
		currPlayer.set("blue_stones", 0);
		currPlayer.set("ancient_adena_amount", 0);
		if (removeReward)
		{
			_signsPlayerData.put(objectId, currPlayer);
			if (!FeatureConfig.ALT_SEVENSIGNS_LAZY_UPDATE)
			{
				saveSevenSignsData(objectId);
				saveSevenSignsStatus();
			}
		}
		
		return rewardAmount;
	}
	
	/**
	 * Used to add the specified player's seal stone contribution points to the current total for their cabal.<br>
	 * Returns the point score the contribution was worth.<br>
	 * Each stone count <b>must be</b> broken down and specified by the stone's color.
	 * @param objectId
	 * @param blueCount
	 * @param greenCount
	 * @param redCount
	 * @return
	 */
	public int addPlayerStoneContrib(int objectId, int blueCount, int greenCount, int redCount)
	{
		final StatSet currPlayer = _signsPlayerData.get(objectId);
		final int contribScore = calcContributionScore(blueCount, greenCount, redCount);
		final int totalAncientAdena = currPlayer.getInt("ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
		final int totalContribScore = currPlayer.getInt("contribution_score") + contribScore;
		if (totalContribScore > FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB)
		{
			return -1;
		}
		
		currPlayer.set("red_stones", currPlayer.getInt("red_stones") + redCount);
		currPlayer.set("green_stones", currPlayer.getInt("green_stones") + greenCount);
		currPlayer.set("blue_stones", currPlayer.getInt("blue_stones") + blueCount);
		currPlayer.set("ancient_adena_amount", totalAncientAdena);
		currPlayer.set("contribution_score", totalContribScore);
		_signsPlayerData.put(objectId, currPlayer);
		
		switch (getPlayerCabal(objectId))
		{
			case CABAL_DAWN:
			{
				_dawnStoneScore += contribScore;
				break;
			}
			case CABAL_DUSK:
			{
				_duskStoneScore += contribScore;
				break;
			}
		}
		
		if (!FeatureConfig.ALT_SEVENSIGNS_LAZY_UPDATE)
		{
			saveSevenSignsData(objectId);
			saveSevenSignsStatus();
		}
		
		return contribScore;
	}
	
	/**
	 * Adds the specified number of festival points to the specified cabal.<br>
	 * Remember, the same number of points are <b>deducted from the rival cabal</b> to maintain proportionality.
	 * @param cabal
	 * @param amount
	 */
	public void addFestivalScore(int cabal, int amount)
	{
		if (cabal == CABAL_DUSK)
		{
			_duskFestivalScore += amount;
			
			// To prevent negative scores!
			if (_dawnFestivalScore >= amount)
			{
				_dawnFestivalScore -= amount;
			}
		}
		else
		{
			_dawnFestivalScore += amount;
			if (_duskFestivalScore >= amount)
			{
				_duskFestivalScore -= amount;
			}
		}
	}
	
	/**
	 * Send info on the current Seven Signs period to the specified player.
	 * @param player
	 */
	public void sendCurrentPeriodMsg(Player player)
	{
		switch (_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
			{
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_PREPARATIONS_HAVE_BEGUN_FOR_THE_NEXT_QUEST_EVENT);
				break;
			}
			case PERIOD_COMPETITION:
			{
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_BEGUN_SPEAK_WITH_A_PRIEST_OF_DAWN_OR_DUSK_PRIESTESS_IF_YOU_WISH_TO_PARTICIPATE_IN_THE_EVENT);
				break;
			}
			case PERIOD_COMP_RESULTS:
			{
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_QUEST_EVENT_HAS_ENDED_RESULTS_ARE_BEING_TALLIED);
				break;
			}
			case PERIOD_SEAL_VALIDATION:
			{
				player.sendPacket(SystemMessageId.SEVEN_SIGNS_THIS_IS_THE_SEAL_VALIDATION_PERIOD_A_NEW_QUEST_EVENT_PERIOD_BEGINS_NEXT_MONDAY);
				break;
			}
		}
	}
	
	/**
	 * Sends the built-in system message specified by sysMsgId to all online players.
	 * @param sysMsgId
	 */
	public void sendMessageToAll(SystemMessageId sysMsgId)
	{
		Broadcast.toAllOnlinePlayers(new SystemMessage(sysMsgId));
	}
	
	/**
	 * Used to initialize the seals for each cabal.<br>
	 * (Used at startup or at beginning of a new cycle).<br>
	 * This method should be called after <b>resetSeals()</b> and <b>calcNewSealOwners()</b> on a new cycle.
	 */
	protected void initializeSeals()
	{
		for (Entry<Integer, Integer> e : _signsSealOwners.entrySet())
		{
			if (e.getValue() != CABAL_NULL)
			{
				if (isSealValidationPeriod())
				{
					LOGGER.info("SevenSigns: The " + getCabalName(e.getValue()) + " have won the " + getSealName(e.getKey(), false) + ".");
				}
				else
				{
					LOGGER.info("SevenSigns: The " + getSealName(e.getKey(), false) + " is currently owned by " + getCabalName(e.getValue()) + ".");
				}
			}
			else
			{
				LOGGER.info("SevenSigns: The " + getSealName(e.getKey(), false) + " remains unclaimed.");
			}
		}
	}
	
	/**
	 * Only really used at the beginning of a new cycle, this method resets all seal-related data.
	 */
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}
	
	/**
	 * Calculates the ownership of the three Seals of the Seven Signs, based on various criterion.<br>
	 * Should only ever called at the beginning of a new cycle.
	 */
	protected void calcNewSealOwners()
	{
		for (Integer currSeal : _signsDawnSealTotals.keySet())
		{
			final int prevSealOwner = _signsSealOwners.get(currSeal);
			int newSealOwner = CABAL_NULL;
			final int dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			final int totalDawnMembers = getTotalMembers(CABAL_DAWN) == 0 ? 1 : getTotalMembers(CABAL_DAWN);
			final int dawnPercent = Math.round(((float) dawnProportion / totalDawnMembers) * 100);
			final int duskProportion = getSealProportion(currSeal, CABAL_DUSK);
			final int totalDuskMembers = getTotalMembers(CABAL_DUSK) == 0 ? 1 : getTotalMembers(CABAL_DUSK);
			final int duskPercent = Math.round(((float) duskProportion / totalDuskMembers) * 100);
			
			/**
			 * If a Seal was already closed or owned by the opponent and the new winner wants to assume ownership of the Seal, 35% or more of the members of the Cabal must have chosen the Seal.<br>
			 * If they chose less than 35%, they cannot own the Seal.<br>
			 * If the Seal was owned by the winner in the previous Seven Signs, they can retain that seal if 10% or more members have chosen it.<br>
			 * If they want to possess a new Seal, at least 35% of the members of the Cabal must have chosen the new Seal.
			 */
			switch (prevSealOwner)
			{
				case CABAL_NULL:
				{
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
						{
							newSealOwner = CABAL_NULL;
							break;
						}
						case CABAL_DAWN:
						{
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
						case CABAL_DUSK:
						{
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
					}
					break;
				}
				case CABAL_DAWN:
				{
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
						{
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
						case CABAL_DAWN:
						{
							if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
						case CABAL_DUSK:
						{
							if (duskPercent >= 35)
							{
								newSealOwner = CABAL_DUSK;
							}
							else if (dawnPercent >= 10)
							{
								newSealOwner = CABAL_DAWN;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
					}
					break;
				}
				case CABAL_DUSK:
				{
					switch (getCabalHighestScore())
					{
						case CABAL_NULL:
						{
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
						case CABAL_DAWN:
						{
							if (dawnPercent >= 35)
							{
								newSealOwner = CABAL_DAWN;
							}
							else if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
						case CABAL_DUSK:
						{
							if (duskPercent >= 10)
							{
								newSealOwner = CABAL_DUSK;
							}
							else
							{
								newSealOwner = CABAL_NULL;
							}
							break;
						}
					}
					break;
				}
			}
			
			_signsSealOwners.put(currSeal, newSealOwner);
			
			// Alert all online players to new seal status.
			switch (currSeal)
			{
				case SEAL_AVARICE:
				{
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_AVARICE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_AVARICE);
					}
					break;
				}
				case SEAL_GNOSIS:
				{
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS);
					}
					break;
				}
				case SEAL_STRIFE:
				{
					if (newSealOwner == CABAL_DAWN)
					{
						sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_STRIFE);
					}
					else if (newSealOwner == CABAL_DUSK)
					{
						sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_STRIFE);
					}
					
					CastleManager.getInstance().validateTaxes(newSealOwner);
					break;
				}
			}
		}
	}
	
	/**
	 * This method is called to remove all players from catacombs and necropolises, who belong to the losing cabal.<br>
	 * Should only ever called at the beginning of Seal Validation.
	 * @param compWinner
	 */
	protected void teleLosingCabalFromDungeons(String compWinner)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			final StatSet currPlayer = _signsPlayerData.get(player.getObjectId());
			if (isSealValidationPeriod() || isCompResultsPeriod())
			{
				if (!player.isGM() && player.isIn7sDungeon() && ((currPlayer == null) || !currPlayer.getString("cabal").equals(compWinner)))
				{
					player.teleToLocation(TeleportWhereType.TOWN);
					player.setIn7sDungeon(false);
					player.sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
				}
			}
			else
			{
				if (!player.isGM() && player.isIn7sDungeon() && ((currPlayer == null) || !currPlayer.getString("cabal").isEmpty()))
				{
					player.teleToLocation(TeleportWhereType.TOWN);
					player.setIn7sDungeon(false);
					player.sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
				}
			}
		}
	}
	
	/**
	 * The primary controller of period change of the Seven Signs system.<br>
	 * This runs all related tasks depending on the period that is about to begin.
	 * @author Tempy
	 */
	protected class SevenSignsPeriodChange implements Runnable
	{
		@Override
		public void run()
		{
			// Remember the period check here refers to the period just ENDED!
			final int periodEnded = _activePeriod;
			_activePeriod++;
			
			switch (periodEnded)
			{
				case PERIOD_COMP_RECRUITING: // Initialization
				{
					// Start the Festival of Darkness cycle.
					SevenSignsFestival.getInstance().startFestivalManager();
					
					// Send message that Competition has begun.
					sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_BEGUN_VISIT_A_PRIEST_OF_DAWN_OR_PRIESTESS_OF_DUSK_TO_PARTICIPATE_IN_THE_EVENT);
					break;
				}
				case PERIOD_COMPETITION: // Results Calculation
				{
					// Send message that Competition has ended.
					sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_ENDED_THE_NEXT_QUEST_EVENT_WILL_START_IN_ONE_WEEK);
					final int compWinner = getCabalHighestScore();
					
					// Schedule a stop of the festival engine and reward highest ranking members from cycle.
					// Note: getFestivalManagerSchedule() lazily starts the manager if needed, so this is
					// safe to call even when the competition is closed manually (no-schedule mode).
					SevenSignsFestival.getInstance().getFestivalManagerSchedule().cancel(false);
					SevenSignsFestival.getInstance().rewardHighestRanked();
					calcNewSealOwners();
					switch (compWinner)
					{
						case CABAL_DAWN:
						{
							sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_WON);
							break;
						}
						case CABAL_DUSK:
						{
							sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_WON);
							break;
						}
					}
					
					_previousWinner = compWinner;
					
					// Reset Castle ticket buy count
					final List<Castle> castles = CastleManager.getInstance().getCastles();
					for (Castle castle : castles)
					{
						castle.setTicketBuyCount(0);
					}
					break;
				}
				case PERIOD_COMP_RESULTS: // Seal Validation
				{
					// Perform initial Seal Validation set up.
					initializeSeals();
					
					// Buff/Debuff members of the event when Seal of Strife captured.
					giveCPMult(getSealOwner(SEAL_STRIFE));
					
					// Send message that Seal Validation has begun.
					sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_BEGUN);
					LOGGER.info("SevenSigns: The " + getCabalName(_previousWinner) + " have won the competition with " + getCurrentScore(_previousWinner) + " points!");
					break;
				}
				case PERIOD_SEAL_VALIDATION: // Reset for New Cycle
				{
					// Ensure a cycle restart when this period ends.
					_activePeriod = PERIOD_COMP_RECRUITING;
					
					// Send message that Seal Validation has ended.
					sendMessageToAll(SystemMessageId.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_ENDED);
					
					// Clear Seal of Strife influence.
					removeCPMult();
					
					// Reset all data
					resetPlayerData();
					resetSeals();
					_currentCycle++;
					
					// Reset all Festival-related data and remove any unused blood offerings.
					// NOTE: A full update of Festival data in the database is also performed.
					SevenSignsFestival.getInstance().resetFestivalData(false);
					_dawnStoneScore = 0;
					_duskStoneScore = 0;
					_dawnFestivalScore = 0;
					_duskFestivalScore = 0;
					break;
				}
			}
			
			// Make sure all Seven Signs data is saved for future use.
			saveSevenSignsData();
			saveSevenSignsStatus();
			
			teleLosingCabalFromDungeons(getCabalShortName(getCabalHighestScore()));
			
			// final SSQInfo ss = new SSQInfo();
			// Broadcast.toAllOnlinePlayers(ss);
			spawnSevenSignsNPC();
			
			LOGGER.info("SevenSigns: The " + getCurrentPeriodName() + " period has begun!");

			// In no-schedule mode the next change is never armed - transitions are driven
			// manually - so the cycle simply stops here until the next manual trigger.
			if (FeatureConfig.ALT_SEVENSIGNS_NO_SCHEDULE)
			{
				return;
			}

			setCalendarForNextPeriodChange();

			final SevenSignsPeriodChange sspc = new SevenSignsPeriodChange();
			ThreadPool.schedule(sspc, getMilliToPeriodChange());
		}
	}

	/**
	 * Manually advances the Seven Signs cycle by a single period, executing the same
	 * transition logic as the scheduled task. Intended for no-schedule (solo) mode, where
	 * the automatic weekly timer is disabled and the cycle is driven on demand.
	 */
	public synchronized void forcePeriodChange()
	{
		new SevenSignsPeriodChange().run();
	}

	/**
	 * Drives the cycle forward until the Seal Validation period is reached, closing out the
	 * current competition. The highest-scoring cabal wins and its chosen seals are validated
	 * (which is what opens the Anakim/Lilith rooms and grants the seal effects). If nobody has
	 * contributed there is no winner and no seal is captured - callers should warn about that
	 * beforehand. Only meaningful in no-schedule mode.
	 */
	public synchronized void forceSealValidation()
	{
		// The state machine always passes through Seal Validation within a full cycle,
		// so this terminates in at most a handful of steps; the counter is a safety net.
		int guard = 0;
		while ((_activePeriod != PERIOD_SEAL_VALIDATION) && (guard++ < 8))
		{
			forcePeriodChange();
		}
	}

	/**
	 * Drives the cycle forward until the Competition period is reached, starting a fresh cycle.
	 * This resets player registrations and seal ownership, so the player must register again
	 * afterwards. Only meaningful in no-schedule mode.
	 */
	public synchronized void forceNewCycle()
	{
		// Always run at least one full lap, so the reset that clears registrations and seals
		// (the Seal Validation -> new cycle transition) actually executes even when we are
		// already in the Competition period. A plain "while (!= COMPETITION)" would be a no-op
		// in that case and leave the player still registered to their old cabal.
		int guard = 0;
		do
		{
			forcePeriodChange();
		}
		while ((_activePeriod != PERIOD_COMPETITION) && (guard++ < 8));
	}

	/**
	 * @return a short human-readable summary of the current Seven Signs state, for admin feedback.
	 */
	public String getSummaryText()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("Seven Signs: ").append(getCurrentPeriodName());
		sb.append(" | Leader: ").append(getCabalName(getCabalHighestScore()));
		sb.append(" | Avarice: ").append(getCabalName(getSealOwner(SEAL_AVARICE)));
		sb.append(", Gnosis: ").append(getCabalName(getSealOwner(SEAL_GNOSIS)));
		sb.append(", Strife: ").append(getCabalName(getSealOwner(SEAL_STRIFE)));
		return sb.toString();
	}
	
	public boolean checkIsDawnPostingTicket(int itemId)
	{
		// TODO: I think it should be some kind of a list in the datapack for compare.
		if ((itemId > 6114) && (itemId < 6175))
		{
			return true;
		}
		
		if ((itemId > 6801) && (itemId < 6812))
		{
			return true;
		}
		
		if ((itemId > 7997) && (itemId < 8008))
		{
			return true;
		}
		
		if ((itemId > 7940) && (itemId < 7951))
		{
			return true;
		}
		
		if ((itemId > 6294) && (itemId < 6307))
		{
			return true;
		}
		
		if ((itemId > 6831) && (itemId < 6834))
		{
			return true;
		}
		
		if ((itemId > 8027) && (itemId < 8030))
		{
			return true;
		}
		
		if ((itemId > 7970) && (itemId < 7973))
		{
			return true;
		}
		
		return false;
	}
	
	public boolean checkIsRookiePostingTicket(int itemId)
	{
		// TODO: I think it should be some kind of a list in the datapack for compare.
		if ((itemId > 6174) && (itemId < 6295))
		{
			return true;
		}
		
		if ((itemId > 6811) && (itemId < 6832))
		{
			return true;
		}
		
		if ((itemId > 7950) && (itemId < 7971))
		{
			return true;
		}
		
		if ((itemId > 8007) && (itemId < 8028))
		{
			return true;
		}
		
		return false;
	}
	
	public void giveCPMult(int strifeOwner)
	{
		for (Player player : World.getInstance().getPlayers())
		{
			// Gives "Victor of War" passive skill to all online characters with Cabal, which controls Seal of Strife
			final int cabal = getPlayerCabal(player.getObjectId());
			if (cabal != CABAL_NULL)
			{
				if (cabal == strifeOwner)
				{
					player.addSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
				}
				else
				{
					// Gives "The Vanquished of War" passive skill to all online characters with Cabal, which does not control Seal of Strife
					player.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
				}
			}
		}
	}
	
	public void removeCPMult()
	{
		// Remove SevenSigns' buffs/debuffs.
		for (Player player : World.getInstance().getPlayers())
		{
			player.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
			player.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}
	}
	
	public boolean checkSummonConditions(Player player)
	{
		if (player == null)
		{
			return true;
		}
		
		// Golems cannot be summoned by Dusk when the Seal of Strife is controlled by the Dawn
		if (isSealValidationPeriod() && (getSealOwner(SEAL_STRIFE) == CABAL_DAWN) && (getPlayerCabal(player.getObjectId()) == CABAL_DUSK))
		{
			player.sendPacket(SystemMessageId.DUE_TO_THE_AFFECTS_OF_THE_SEAL_OF_STRIFE_IT_IS_NOT_POSSIBLE_TO_SUMMON_AT_THIS_TIME);
			return true;
		}
		
		return false;
	}
	
	private static class SingletonHolder
	{
		protected static final SevenSigns INSTANCE = new SevenSigns();
	}
}
