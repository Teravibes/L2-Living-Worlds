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
package org.l2jmobius.gameserver.config;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the development related configurations.
 * @author Mobius
 */
public class DevelopmentConfig
{
	// File
	private static final String DEVELOPMENT_CONFIG_FILE = "./config/Development.ini";
	
	// Constants
	public static boolean LOG_SERVER_LOAD_TIMES;
	public static boolean HTML_ACTION_CACHE_DEBUG;
	public static boolean NO_QUESTS;
	public static boolean NO_SPAWNS;
	public static boolean SHOW_QUEST_LOAD_IN_LOGS;
	public static boolean SHOW_SCRIPT_LOAD_IN_LOGS;
	public static boolean DEBUG_CLIENT_PACKETS;
	public static boolean DEBUG_EX_CLIENT_PACKETS;
	public static boolean DEBUG_SERVER_PACKETS;
	public static boolean DEBUG_UNKNOWN_PACKETS;
	public static Set<String> EXCLUDED_DEBUG_PACKETS;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(DEVELOPMENT_CONFIG_FILE);
		LOG_SERVER_LOAD_TIMES = config.getBoolean("LogServerLoadTimes", false);
		HTML_ACTION_CACHE_DEBUG = config.getBoolean("HtmlActionCacheDebug", false);
		NO_QUESTS = config.getBoolean("NoQuests", false);
		NO_SPAWNS = config.getBoolean("NoSpawns", false);
		SHOW_QUEST_LOAD_IN_LOGS = config.getBoolean("ShowQuestLoadInLogs", false);
		SHOW_SCRIPT_LOAD_IN_LOGS = config.getBoolean("ShowScriptLoadInLogs", false);
		DEBUG_CLIENT_PACKETS = config.getBoolean("DebugClientPackets", false);
		DEBUG_EX_CLIENT_PACKETS = config.getBoolean("DebugExClientPackets", false);
		DEBUG_SERVER_PACKETS = config.getBoolean("DebugServerPackets", false);
		DEBUG_UNKNOWN_PACKETS = config.getBoolean("DebugUnknownPackets", true);
		final String[] packets = config.getString("ExcludedPacketList", "").trim().split(",");
		EXCLUDED_DEBUG_PACKETS = new HashSet<>(packets.length);
		for (String packet : packets)
		{
			EXCLUDED_DEBUG_PACKETS.add(packet.trim());
		}
	}
}
