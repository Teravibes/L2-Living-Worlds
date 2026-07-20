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
package org.l2jmobius.gameserver;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.DatabaseConfig;
import org.l2jmobius.commons.database.DatabaseBackup;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.AutoPlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.SchemeBufferTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.FishingChampionshipManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ItemsOnGroundManager;
import org.l2jmobius.gameserver.managers.PrecautionaryRestartManager;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.loginserverpackets.game.ServerStatus;
import org.l2jmobius.gameserver.network.serverpackets.ServerClose;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * This class provides the functions for shutting down and restarting the server.<br>
 * It closes all open client connections and saves all data.
 * @version $Revision: 1.2.4.5 $ $Date: 2005/03/27 15:29:09 $
 */
public class Shutdown extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(Shutdown.class.getName());
	
	private static final int SIGTERM = 0;
	private static final int GM_SHUTDOWN = 1;
	private static final int GM_RESTART = 2;
	private static final int ABORT = 3;
	private static final String[] MODE_TEXT =
	{
		"SIGTERM",
		"shutting down",
		"restarting",
		"aborting"
	};
	
	private static Shutdown _counterInstance;
	private static boolean _countdownFinished;
	
	private static volatile int _secondsShut;
	private static volatile int _shutdownMode;
	
	/**
	 * This function starts a shutdown count down (Copied from Function startShutdown())
	 * @param seconds seconds until shutdown
	 */
	private void sendServerQuit(int seconds)
	{
		final SystemMessage sysm = new SystemMessage(SystemMessageId.THE_SERVER_WILL_BE_COMING_DOWN_IN_S1_SECOND_S_PLEASE_FIND_A_SAFE_PLACE_TO_LOG_OUT);
		sysm.addInt(seconds);
		Broadcast.toAllOnlinePlayers(sysm);
	}
	
	/**
	 * Default constructor is only used internal to create the shutdown-hook instance
	 */
	protected Shutdown()
	{
		_secondsShut = -1;
		_shutdownMode = SIGTERM;
	}
	
	/**
	 * This creates a countdown instance of Shutdown.
	 * @param seconds how many seconds until shutdown
	 * @param restart true is the server shall restart after shutdown
	 */
	public Shutdown(int seconds, boolean restart)
	{
		_secondsShut = Math.max(0, seconds);
		_shutdownMode = restart ? GM_RESTART : GM_SHUTDOWN;
	}
	
	/**
	 * This function is called, when a new thread starts if this thread is the thread of getInstance, then this is the shutdown hook and we save all data and disconnect all clients.<br>
	 * After this thread ends, the server will completely exit if this is not the thread of getInstance, then this is a countdown thread.<br>
	 * We start the countdown, and when we finished it, and it was not aborted, we tell the shutdown-hook why we call exit, and then call exit when the exit status of the server is 1, startServer.sh / startServer.bat will restart the server.
	 */
	@Override
	public void run()
	{
		if (this == getInstance())
		{
			startShutdownActions();
			return;
		}
		
		if (_countdownFinished)
		{
			return;
		}
		
		// Send warnings and then call exit to start shutdown sequence.
		countdown();
		
		// Last point where logging is operational.
		LOGGER.warning("GM shutdown countdown is over. " + MODE_TEXT[_shutdownMode] + " NOW!");
		
		switch (_shutdownMode)
		{
			case GM_SHUTDOWN:
			{
				getInstance().setMode(GM_SHUTDOWN);
				startShutdownActions();
				System.exit(0);
				break;
			}
			case GM_RESTART:
			{
				getInstance().setMode(GM_RESTART);
				startShutdownActions();
				System.exit(2);
				break;
			}
			case ABORT:
			{
				LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
				break;
			}
		}
	}
	
	/**
	 * This functions starts a shutdown countdown.
	 * @param player GM who issued the shutdown command
	 * @param seconds seconds until shutdown
	 * @param restart true if the server will restart after shutdown
	 */
	public void startShutdown(Player player, int seconds, boolean restart)
	{
		_shutdownMode = restart ? GM_RESTART : GM_SHUTDOWN;
		
		if (player != null)
		{
			LOGGER.warning("GM: " + player.getName() + "(" + player.getObjectId() + ") issued shutdown command. " + MODE_TEXT[_shutdownMode] + " in " + seconds + " seconds!");
		}
		else
		{
			LOGGER.warning("Server scheduled restart issued shutdown command. " + (restart ? "Restart" : "Shutdown") + " in " + seconds + " seconds!");
		}
		
		if (_shutdownMode > 0)
		{
			switch (seconds)
			{
				case 540:
				case 480:
				case 420:
				case 360:
				case 300:
				case 240:
				case 180:
				case 120:
				case 60:
				case 30:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
				{
					break;
				}
				default:
				{
					sendServerQuit(seconds);
				}
			}
		}
		
		if (_counterInstance != null)
		{
			_counterInstance.abort();
		}
		
		if (ServerConfig.PRECAUTIONARY_RESTART_ENABLED)
		{
			PrecautionaryRestartManager.getInstance().restartEnabled();
		}
		
		// the main instance should only run for shutdown hook, so we start a new instance
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}
	
	/**
	 * This function aborts a running countdown.
	 * @param player GM who issued the abort command
	 */
	public void abort(Player player)
	{
		if (_countdownFinished)
		{
			LOGGER.warning("GM: " + (player != null ? player.getName() + "(" + player.getObjectId() + ") " : "") + "shutdown ABORT failed because countdown has finished.");
			return;
		}
		
		LOGGER.warning("GM: " + (player != null ? player.getName() + "(" + player.getObjectId() + ") " : "") + "issued shutdown ABORT. " + MODE_TEXT[_shutdownMode] + " has been stopped!");
		if (_counterInstance != null)
		{
			_counterInstance.abort();
			
			if (ServerConfig.PRECAUTIONARY_RESTART_ENABLED)
			{
				PrecautionaryRestartManager.getInstance().restartAborted();
			}
			
			Broadcast.toAllOnlinePlayers("Server aborts " + MODE_TEXT[_shutdownMode] + " and continues normal operation!", false);
		}
	}
	
	/**
	 * Set the shutdown mode.
	 * @param mode what mode shall be set
	 */
	private void setMode(int mode)
	{
		_shutdownMode = mode;
	}
	
	/**
	 * Set shutdown mode to ABORT.
	 */
	private void abort()
	{
		_shutdownMode = ABORT;
	}
	
	/**
	 * This counts the countdown and reports it to all players countdown is aborted if mode changes to ABORT.
	 */
	private void countdown()
	{
		try
		{
			while (_secondsShut > 0)
			{
				// Rehabilitate previous server status if shutdown is aborted.
				if (_shutdownMode == ABORT)
				{
					if (LoginServerThread.getInstance().getServerStatus() == ServerStatus.STATUS_DOWN)
					{
						LoginServerThread.getInstance().setServerStatus((GeneralConfig.SERVER_GMONLY) ? ServerStatus.STATUS_GM_ONLY : ServerStatus.STATUS_AUTO);
					}
					break;
				}
				
				switch (_secondsShut)
				{
					case 540:
					case 480:
					case 420:
					case 360:
					case 300:
					case 240:
					case 180:
					case 120:
					case 60:
					case 30:
					case 10:
					case 5:
					case 4:
					case 3:
					case 2:
					case 1:
					{
						sendServerQuit(_secondsShut);
					}
				}
				
				// Prevent players from logging in.
				if ((_secondsShut <= 60) && (LoginServerThread.getInstance().getServerStatus() != ServerStatus.STATUS_DOWN))
				{
					LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_DOWN);
				}
				
				_secondsShut--;
				
				Thread.sleep(1000);
			}
		}
		catch (Exception e)
		{
			// this will never happen
		}
	}
	
	/**
	 * Actions performed when shutdown countdown completes.
	 */
	private void startShutdownActions()
	{
		if (_countdownFinished)
		{
			return;
		}
		
		_countdownFinished = true;
		
		final TimeCounter tc = new TimeCounter();
		final TimeCounter tc1 = new TimeCounter();
		
		try
		{
			if ((OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE) && OfflineTradeConfig.RESTORE_OFFLINERS && !OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME)
			{
				OfflineTraderTable.getInstance().storeOffliners();
				LOGGER.info("Offline Traders Table: Offline shops stored(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
		}
		catch (Throwable t)
		{
			LOGGER.log(Level.WARNING, "Error saving offline shops.", t);
		}
		
		try
		{
			if ((OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS && AutoPlayConfig.ENABLE_AUTO_ASSIST))
			{
				OfflinePlayTable.getInstance().storeOfflineGroups();
				LOGGER.info("Offline Play Table: Offline play groups stored(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			}
		}
		catch (Throwable t)
		{
			LOGGER.log(Level.WARNING, "Error saving offline play groups.", t);
		}
		
		try
		{
			disconnectAllCharacters();
			LOGGER.info("All players disconnected and saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// ensure all services are stopped
		
		try
		{
			GameTimeTaskManager.getInstance().interrupt();
			LOGGER.info("Game Time Task Manager: Thread interruped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// stop all thread pools
		try
		{
			ThreadPool.shutdown();
			LOGGER.info("Thread Pool Manager: Manager has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		try
		{
			LoginServerThread.getInstance().interrupt();
			LOGGER.info("Login Server Thread: Thread interruped(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// last byebye, save all data and quit this server
		saveData();
		tc.restartCounter();
		
		// commit data, last chance
		try
		{
			DatabaseFactory.close();
			LOGGER.info("Database Factory: Database connection has been shut down(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		catch (Throwable t)
		{
			// ignore
		}
		
		// Backup database.
		if (DatabaseConfig.BACKUP_DATABASE)
		{
			DatabaseBackup.performBackup("game");
		}
		
		LOGGER.info("The server has been successfully shut down in " + (tc1.getEstimatedTime() / 1000) + "seconds.");
	}
	
	/**
	 * This sends a last byebye, disconnects all players and saves data.
	 */
	private void saveData()
	{
		switch (_shutdownMode)
		{
			case SIGTERM:
			{
				LOGGER.info("SIGTERM received. Shutting down NOW!");
				break;
			}
			case GM_SHUTDOWN:
			{
				LOGGER.info("GM shutdown received. Shutting down NOW!");
				break;
			}
			case GM_RESTART:
			{
				LOGGER.info("GM restart received. Restarting NOW!");
				break;
			}
		}
		
		final TimeCounter tc = new TimeCounter();
		
		// Seven Signs data is now saved along with Festival data.
		if (!SevenSigns.getInstance().isSealValidationPeriod())
		{
			SevenSignsFestival.getInstance().saveFestivalData(false);
			LOGGER.info("SevenSignsFestival: Festival data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		
		// Save Seven Signs data before closing. :)
		SevenSigns.getInstance().saveSevenSignsData();
		LOGGER.info("SevenSigns: Seven Signs data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		SevenSigns.getInstance().saveSevenSignsStatus();
		LOGGER.info("SevenSigns: Seven Signs status saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save all raidboss and GrandBoss status ^_^
		RaidBossSpawnManager.getInstance().cleanUp();
		LOGGER.info("RaidBossSpawnManager: All raidboss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		GrandBossManager.getInstance().cleanUp();
		LOGGER.info("GrandBossManager: All Grand Boss info saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		Olympiad.getInstance().saveOlympiadStatus();
		LOGGER.info("Olympiad System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		Hero.getInstance().shutdown();
		LOGGER.info("Hero System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		ClanTable.getInstance().shutdown();
		LOGGER.info("Clan System: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save Cursed Weapons data before closing.
		CursedWeaponsManager.getInstance().saveData();
		LOGGER.info("Cursed Weapons Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save all manor data
		if (!GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			CastleManorManager.getInstance().storeMe();
			LOGGER.info("Castle Manor Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		
		CHSiegeManager.getInstance().onServerShutDown();
		LOGGER.info("CHSiegeManager: Siegable hall attacker lists saved!");
		
		// Save all global (non-player specific) Quest data that needs to persist after reboot
		ScriptManager.getInstance().save();
		LOGGER.info("Script Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save all global variables data
		GlobalVariablesManager.getInstance().storeMe();
		LOGGER.info("Global Variables Manager: Variables saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save Fishing tournament data.
		if (GeneralConfig.ALT_FISH_CHAMPIONSHIP_ENABLED)
		{
			FishingChampionshipManager.getInstance().shutdown();
			LOGGER.info("Fishing Championship Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		
		// Schemes save.
		SchemeBufferTable.getInstance().saveSchemes();
		LOGGER.info("SchemeBufferTable: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		
		// Save items on ground before closing
		if (GeneralConfig.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().saveInDb();
			LOGGER.info("Items On Ground Manager: Data saved(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
			ItemsOnGroundManager.getInstance().cleanUp();
			LOGGER.info("Items On Ground Manager: Cleaned up(" + tc.getEstimatedTimeAndRestartCounter() + "ms).");
		}
		
		try
		{
			Thread.sleep(5000);
		}
		catch (Exception e)
		{
			// this will never happen
		}
	}
	
	/**
	 * This disconnects all clients from the server.
	 */
	private void disconnectAllCharacters()
	{
		for (Player player : World.getInstance().getPlayers())
		{
			Disconnection.of(player).storeAndDeleteWith(ServerClose.STATIC_PACKET);
		}
	}
	
	/**
	 * A simple class used to track down the estimated time of method executions.<br>
	 * Once this class is created, it saves the start time, and when you want to get the estimated time, use the getEstimatedTime() method.
	 */
	private static class TimeCounter
	{
		private long _startTime;
		
		protected TimeCounter()
		{
			restartCounter();
		}
		
		public void restartCounter()
		{
			_startTime = System.currentTimeMillis();
		}
		
		public long getEstimatedTimeAndRestartCounter()
		{
			final long toReturn = System.currentTimeMillis() - _startTime;
			restartCounter();
			return toReturn;
		}
		
		public long getEstimatedTime()
		{
			return System.currentTimeMillis() - _startTime;
		}
	}
	
	/**
	 * Get the shutdown-hook instance the shutdown-hook instance is created by the first call of this function, but it has to be registered externally.
	 * @return instance of Shutdown, to be used as shutdown hook
	 */
	public static Shutdown getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Shutdown INSTANCE = new Shutdown();
	}
}
