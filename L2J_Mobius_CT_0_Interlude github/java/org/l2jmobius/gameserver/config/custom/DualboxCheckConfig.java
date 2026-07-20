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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;

/**
 * This class loads all the custom dualbox check related configurations.
 * @author Mobius
 */
public class DualboxCheckConfig
{
	private static final Logger LOGGER = Logger.getLogger(DualboxCheckConfig.class.getName());
	
	// File
	private static final String DUALBOX_CHECK_CONFIG_FILE = "./config/Custom/DualboxCheck.ini";
	
	// Constants
	public static int DUALBOX_CHECK_MAX_PLAYERS_PER_IP;
	public static int DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP;
	public static int DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP;
	public static int DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP;
	public static int DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP;
	public static boolean DUALBOX_COUNT_OFFLINE_TRADERS;
	public static Map<Integer, Integer> DUALBOX_CHECK_WHITELIST;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(DUALBOX_CHECK_CONFIG_FILE);
		DUALBOX_CHECK_MAX_PLAYERS_PER_IP = config.getInt("DualboxCheckMaxPlayersPerIP", 0);
		DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP = config.getInt("DualboxCheckMaxOlympiadParticipantsPerIP", 0);
		DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP = config.getInt("DualboxCheckMaxL2EventParticipantsPerIP", 0);
		DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP = config.getInt("DualboxCheckMaxOfflinePlayPerIP", 0);
		DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP = config.getInt("DualboxCheckMaxOfflinePlayPremiumPerIP", 0);
		DUALBOX_COUNT_OFFLINE_TRADERS = config.getBoolean("DualboxCountOfflineTraders", false);
		
		final String[] dualboxCheckWhiteList = config.getString("DualboxCheckWhitelist", "127.0.0.1,0").split(";");
		DUALBOX_CHECK_WHITELIST = new HashMap<>(dualboxCheckWhiteList.length);
		for (String entry : dualboxCheckWhiteList)
		{
			final String[] entrySplit = entry.split(",");
			if (entrySplit.length != 2)
			{
				LOGGER.warning(StringUtil.concat("DualboxCheck[DualboxCheckConfig.load()]: invalid config property -> DualboxCheckWhitelist \"", entry, "\""));
			}
			else
			{
				try
				{
					int num = Integer.parseInt(entrySplit[1]);
					num = num == 0 ? -1 : num;
					DUALBOX_CHECK_WHITELIST.put(InetAddress.getByName(entrySplit[0]).hashCode(), num);
				}
				catch (UnknownHostException e)
				{
					LOGGER.warning(StringUtil.concat("DualboxCheck[DualboxCheckConfig.load()]: invalid address -> DualboxCheckWhitelist \"", entrySplit[0], "\""));
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning(StringUtil.concat("DualboxCheck[DualboxCheckConfig.load()]: invalid number -> DualboxCheckWhitelist \"", entrySplit[1], "\""));
				}
			}
		}
	}
}
