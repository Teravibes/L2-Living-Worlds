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
package org.l2jmobius.loginserver.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the server related configurations.
 * @author Mobius
 */
public class LoginConfig
{
	private static final Logger LOGGER = Logger.getLogger(LoginConfig.class.getName());
	
	// File
	private static final String SERVER_CONFIG_FILE = "./config/Server.ini";
	
	// Constants
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static String LOGIN_BIND_ADDRESS;
	public static int PORT_LOGIN;
	public static File DATAPACK_ROOT;
	public static boolean ACCEPT_NEW_GAMESERVER;
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static boolean LOGIN_SERVER_SCHEDULE_RESTART;
	public static long LOGIN_SERVER_SCHEDULE_RESTART_TIME;
	public static boolean SHOW_LICENCE;
	public static boolean AUTO_CREATE_ACCOUNTS;
	public static boolean FLOOD_PROTECTION;
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(SERVER_CONFIG_FILE);
		GAME_SERVER_LOGIN_HOST = config.getString("LoginHostname", "127.0.0.1");
		GAME_SERVER_LOGIN_PORT = config.getInt("LoginPort", 9013);
		LOGIN_BIND_ADDRESS = config.getString("LoginserverHostname", "0.0.0.0");
		PORT_LOGIN = config.getInt("LoginserverPort", 2106);
		try
		{
			DATAPACK_ROOT = new File(config.getString("DatapackRoot", ".").replaceAll("\\\\", "/")).getCanonicalFile();
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Error setting datapack root!", e);
			DATAPACK_ROOT = new File(".");
		}
		ACCEPT_NEW_GAMESERVER = config.getBoolean("AcceptNewGameServer", true);
		LOGIN_TRY_BEFORE_BAN = config.getInt("LoginTryBeforeBan", 5);
		LOGIN_BLOCK_AFTER_BAN = config.getInt("LoginBlockAfterBan", 900);
		LOGIN_SERVER_SCHEDULE_RESTART = config.getBoolean("LoginRestartSchedule", false);
		LOGIN_SERVER_SCHEDULE_RESTART_TIME = config.getLong("LoginRestartTime", 24);
		SHOW_LICENCE = config.getBoolean("ShowLicence", true);
		AUTO_CREATE_ACCOUNTS = config.getBoolean("AutoCreateAccounts", true);
		FLOOD_PROTECTION = config.getBoolean("EnableFloodProtection", true);
		FAST_CONNECTION_LIMIT = config.getInt("FastConnectionLimit", 15);
		NORMAL_CONNECTION_TIME = config.getInt("NormalConnectionTime", 700);
		FAST_CONNECTION_TIME = config.getInt("FastConnectionTime", 350);
		MAX_CONNECTION_PER_IP = config.getInt("MaxConnectionPerIP", 50);
	}
}
