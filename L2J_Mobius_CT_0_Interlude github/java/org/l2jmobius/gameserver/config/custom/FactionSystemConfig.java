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
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.model.Location;

/**
 * This class loads all the custom faction system related configurations.
 * @author Mobius
 */
public class FactionSystemConfig
{
	// File
	private static final String FACTION_SYSTEM_CONFIG_FILE = "./config/Custom/FactionSystem.ini";
	
	// Constants
	public static boolean FACTION_SYSTEM_ENABLED;
	public static Location FACTION_STARTING_LOCATION;
	public static Location FACTION_MANAGER_LOCATION;
	public static Location FACTION_GOOD_BASE_LOCATION;
	public static Location FACTION_EVIL_BASE_LOCATION;
	public static String FACTION_GOOD_TEAM_NAME;
	public static String FACTION_EVIL_TEAM_NAME;
	public static int FACTION_GOOD_NAME_COLOR;
	public static int FACTION_EVIL_NAME_COLOR;
	public static boolean FACTION_GUARDS_ENABLED;
	public static boolean FACTION_RESPAWN_AT_BASE;
	public static boolean FACTION_AUTO_NOBLESS;
	public static boolean FACTION_SPECIFIC_CHAT;
	public static boolean FACTION_BALANCE_ONLINE_PLAYERS;
	public static int FACTION_BALANCE_PLAYER_EXCEED_LIMIT;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FACTION_SYSTEM_CONFIG_FILE);
		String[] tempString;
		FACTION_SYSTEM_ENABLED = config.getBoolean("EnableFactionSystem", false);
		tempString = config.getString("StartingLocation", "85332,16199,-1252").split(",");
		FACTION_STARTING_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
		tempString = config.getString("ManagerSpawnLocation", "85712,15974,-1260,26808").split(",");
		FACTION_MANAGER_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]), tempString[3] != null ? Integer.parseInt(tempString[3]) : 0);
		tempString = config.getString("GoodBaseLocation", "45306,48878,-3058").split(",");
		FACTION_GOOD_BASE_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
		tempString = config.getString("EvilBaseLocation", "-44037,-113283,-237").split(",");
		FACTION_EVIL_BASE_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
		FACTION_GOOD_TEAM_NAME = config.getString("GoodTeamName", "Good");
		FACTION_EVIL_TEAM_NAME = config.getString("EvilTeamName", "Evil");
		FACTION_GOOD_NAME_COLOR = Integer.decode("0x" + config.getString("GoodNameColor", "00FF00"));
		FACTION_EVIL_NAME_COLOR = Integer.decode("0x" + config.getString("EvilNameColor", "0000FF"));
		FACTION_GUARDS_ENABLED = config.getBoolean("EnableFactionGuards", true);
		FACTION_RESPAWN_AT_BASE = config.getBoolean("RespawnAtFactionBase", true);
		FACTION_AUTO_NOBLESS = config.getBoolean("FactionAutoNobless", false);
		FACTION_SPECIFIC_CHAT = config.getBoolean("EnableFactionChat", true);
		FACTION_BALANCE_ONLINE_PLAYERS = config.getBoolean("BalanceOnlinePlayers", true);
		FACTION_BALANCE_PLAYER_EXCEED_LIMIT = config.getInt("BalancePlayerExceedLimit", 20);
	}
}
