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
package org.l2jmobius.loginserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.DatabaseConfig;
import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.database.DatabaseBackup;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.ConnectionManager;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.loginserver.config.LoginConfig;
import org.l2jmobius.loginserver.network.LoginClient;
import org.l2jmobius.loginserver.network.LoginPacketHandler;
import org.l2jmobius.loginserver.network.gameserverpackets.ServerStatus;
import org.l2jmobius.loginserver.ui.Gui;

/**
 * Bootstraps and drives the login server lifecycle.<br>
 * Coordinates configuration loading, core service initialization and network listener startup.
 * <ul>
 * <li>Initializes logging, configuration, database and thread pool.</li>
 * <li>Loads login controller and game server table.</li>
 * <li>Registers IP bans and starts listeners for game servers and login clients.</li>
 * </ul>
 * @author BazookaRpm
 */
public class LoginServer
{
	// Logger.
	public static final Logger LOGGER = Logger.getLogger(LoginServer.class.getName());
	
	// Configuration paths.
	private static final String LOG_DIRECTORY_NAME = "log";
	private static final String LOG_CONFIGURATION_PATH = "./log.cfg";
	private static final String BANNED_IPS_CONFIGURATION_PATH = "./banned_ip.cfg";
	
	// Time constants.
	private static final int MILLISECONDS_PER_HOUR = 3600000;
	
	// Process exit codes.
	private static final int EXIT_CODE_NORMAL_SHUTDOWN = 0;
	private static final int EXIT_CODE_RESTART_REQUEST = 2;
	private static final int EXIT_CODE_FATAL_ERROR = 1;
	
	// Protocol.
	public static final int PROTOCOL_REV = 0x0106;
	
	// Singleton instance.
	private static LoginServer _instance;
	
	// Network listeners.
	private GameServerListener _gameServerListener;
	
	// Login server status.
	private static volatile int _loginStatus = ServerStatus.STATUS_NORMAL;
	
	/**
	 * Creates a new login server instance and initializes all required services.
	 */
	private LoginServer()
	{
		initializeInterfaceLayer();
		initializeLoggingLayer();
		initializeCoreServices();
		initializeSecurityAndBans();
		configureScheduledRestart();
		initializeNetworkListeners();
	}
	
	/**
	 * Initializes interface configuration and optional GUI.
	 */
	private void initializeInterfaceLayer()
	{
		InterfaceConfig.load();
		
		if (!InterfaceConfig.ENABLE_GUI)
		{
			return;
		}
		
		System.out.println("LoginServer: Running in GUI mode.");
		new Gui();
	}
	
	/**
	 * Initializes logging directory and configuration from file.
	 */
	private void initializeLoggingLayer()
	{
		final File logDirectory = new File(".", LOG_DIRECTORY_NAME);
		if (!logDirectory.exists() && !logDirectory.mkdir())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Unable to create log directory at " + logDirectory.getAbsolutePath() + ".");
		}
		
		final File logConfigurationFile = new File(LOG_CONFIGURATION_PATH);
		if (!logConfigurationFile.exists())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Logging configuration file is missing at " + logConfigurationFile.getAbsolutePath() + ". Using default logging settings.");
			return;
		}
		
		if (logConfigurationFile.isDirectory())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Logging configuration path points to a directory (" + logConfigurationFile.getAbsolutePath() + "). Using default logging settings.");
			return;
		}
		
		try (InputStream logConfigurationInputStream = new FileInputStream(logConfigurationFile))
		{
			LogManager.getLogManager().readConfiguration(logConfigurationInputStream);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to apply logging configuration from " + logConfigurationFile.getAbsolutePath() + ". Reason: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Initializes configuration, database connectivity, thread pool and login controller.
	 */
	private void initializeCoreServices()
	{
		LoginConfig.load();
		
		DatabaseFactory.init();
		
		ThreadPool.init();
		
		try
		{
			LoginController.load();
		}
		catch (GeneralSecurityException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: LoginController initialization failed due to security configuration error. Reason: " + e.getMessage(), e);
			System.exit(EXIT_CODE_FATAL_ERROR);
		}
		
		GameServerTable.getInstance();
	}
	
	/**
	 * Initializes security-related resources such as IP bans.
	 */
	private void initializeSecurityAndBans()
	{
		loadBanFile();
	}
	
	/**
	 * Configures an optional scheduled restart for the login server based on configuration values.
	 */
	private void configureScheduledRestart()
	{
		if (!LoginConfig.LOGIN_SERVER_SCHEDULE_RESTART)
		{
			return;
		}
		
		final long restartDelayHours = LoginConfig.LOGIN_SERVER_SCHEDULE_RESTART_TIME;
		final long restartDelayMillis = restartDelayHours * MILLISECONDS_PER_HOUR;
		if (restartDelayMillis <= 0)
		{
			LOGGER.warning("Login server restart scheduling is enabled but the computed delay is invalid. Configured hours: " + restartDelayHours + ".");
			return;
		}
		
		LOGGER.info("Login server restart scheduled in " + restartDelayHours + " hour(s). Computed delay: " + restartDelayMillis + " ms.");
		ThreadPool.schedule(() -> shutdown(true), restartDelayMillis);
	}
	
	/**
	 * Starts listeners for game servers and login clients.
	 */
	private void initializeNetworkListeners()
	{
		startGameServerListener();
		startLoginClientListener();
	}
	
	/**
	 * Starts the game server listener socket.
	 */
	private void startGameServerListener()
	{
		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			
			LOGGER.info("Game server listener is listening on " + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT + ".");
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: Game server listener could not be started on " + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT + ". Reason: " + e.getMessage(), e);
			System.exit(EXIT_CODE_FATAL_ERROR);
		}
	}
	
	/**
	 * Starts the login client listener using the generic connection manager.
	 */
	private void startLoginClientListener()
	{
		try
		{
			new ConnectionManager<>(new InetSocketAddress(LoginConfig.LOGIN_BIND_ADDRESS, LoginConfig.PORT_LOGIN), LoginClient::new, new LoginPacketHandler());
			LOGGER.info(getClass().getSimpleName() + ": Login client listener started on " + LoginConfig.LOGIN_BIND_ADDRESS + ":" + LoginConfig.PORT_LOGIN + ".");
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: Login client listener could not be started on " + LoginConfig.LOGIN_BIND_ADDRESS + ":" + LoginConfig.PORT_LOGIN + ". Reason: " + e.getMessage(), e);
			System.exit(EXIT_CODE_FATAL_ERROR);
		}
	}
	
	/**
	 * Gets the game server listener instance.
	 * @return The game server listener instance.
	 */
	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
	
	/**
	 * Loads banned IP definitions from configuration file and registers them into the login controller.
	 */
	public void loadBanFile()
	{
		final File bannedIpConfigurationFile = new File(BANNED_IPS_CONFIGURATION_PATH);
		if (!bannedIpConfigurationFile.exists() || !bannedIpConfigurationFile.isFile())
		{
			LOGGER.warning("IP bans configuration file (" + bannedIpConfigurationFile.getAbsolutePath() + ") does not exist or is not a regular file. No IP bans have been loaded.");
			return;
		}
		
		try (FileInputStream bannedIpFileInputStream = new FileInputStream(bannedIpConfigurationFile);
			InputStreamReader bannedIpReader = new InputStreamReader(bannedIpFileInputStream);
			LineNumberReader bannedIpLineReader = new LineNumberReader(bannedIpReader))
		{
			String rawLine;
			while ((rawLine = bannedIpLineReader.readLine()) != null)
			{
				processBanLine(bannedIpConfigurationFile, bannedIpLineReader, rawLine);
			}
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Error while reading IP bans configuration file (" + bannedIpConfigurationFile.getAbsolutePath() + "). Details: " + e.getMessage(), e);
		}
		
		LOGGER.info("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP Bans.");
	}
	
	/**
	 * Parses and registers a single IP ban definition line.
	 * @param sourceFile The configuration file being processed.
	 * @param lineReader The line reader used to obtain the current line number.
	 * @param rawLine The raw line text to parse.
	 */
	private void processBanLine(File sourceFile, LineNumberReader lineReader, String rawLine)
	{
		if (rawLine == null)
		{
			return;
		}
		
		final String trimmedLine = rawLine.trim();
		if (trimmedLine.isEmpty())
		{
			return;
		}
		
		if (trimmedLine.charAt(0) == '#')
		{
			return;
		}
		
		final int commentIndex = trimmedLine.indexOf('#');
		final String definitionPart = (commentIndex >= 0) ? trimmedLine.substring(0, commentIndex).trim() : trimmedLine;
		if (definitionPart.isEmpty())
		{
			LOGGER.warning("Skipped IP ban entry with empty definition in file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ".");
			return;
		}
		
		final String[] tokens = definitionPart.split("\\s+");
		if (tokens.length == 0)
		{
			LOGGER.warning("Skipped IP ban entry with no address token in file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ".");
			return;
		}
		
		final String ipAddress = tokens[0];
		long durationMillis = 0;
		
		if (tokens.length > 1)
		{
			final String durationToken = tokens[1];
			try
			{
				durationMillis = Long.parseLong(durationToken);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("Skipped IP ban entry due to invalid duration token '" + durationToken + "' in file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ".");
				return;
			}
		}
		
		try
		{
			LoginController.getInstance().addBanForAddress(ipAddress, durationMillis);
		}
		catch (Exception e)
		{
			LOGGER.warning("Skipped IP ban registration for address '" + ipAddress + "' from file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ". Reason: " + e.getMessage() + ".");
		}
	}
	
	/**
	 * Shuts down the login server and optionally signals restart to the wrapper.
	 * @param restart True to request a restart exit code.
	 */
	public void shutdown(boolean restart)
	{
		if (DatabaseConfig.BACKUP_DATABASE)
		{
			DatabaseBackup.performBackup("login");
		}
		
		Runtime.getRuntime().exit(restart ? EXIT_CODE_RESTART_REQUEST : EXIT_CODE_NORMAL_SHUTDOWN);
	}
	
	/**
	 * Gets the current login server status.
	 * @return The current login server status.
	 */
	public int getStatus()
	{
		return _loginStatus;
	}
	
	/**
	 * Sets the current login server status.
	 * @param status The new login server status.
	 */
	public void setStatus(int status)
	{
		_loginStatus = status;
	}
	
	/**
	 * Gets the login server singleton instance.
	 * @return The login server singleton instance.
	 */
	public static LoginServer getInstance()
	{
		return _instance;
	}
	
	/**
	 * Application entry point for the login server process.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		_instance = new LoginServer();
	}
}
