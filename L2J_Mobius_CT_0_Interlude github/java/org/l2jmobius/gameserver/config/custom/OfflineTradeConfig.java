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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

/**
 * This class loads all the custom offline trade related configurations.
 * @author Mobius
 */
public class OfflineTradeConfig
{
	// File
	private static final String OFFLINE_TRADE_CONFIG_FILE = "./config/Custom/OfflineTrade.ini";
	
	// Constants
	public static boolean OFFLINE_TRADE_ENABLE;
	public static boolean OFFLINE_CRAFT_ENABLE;
	public static boolean OFFLINE_MODE_IN_PEACE_ZONE;
	public static boolean OFFLINE_MODE_NO_DAMAGE;
	public static boolean OFFLINE_SET_NAME_COLOR;
	public static int OFFLINE_NAME_COLOR;
	public static boolean OFFLINE_FAME;
	public static boolean RESTORE_OFFLINERS;
	public static int OFFLINE_MAX_DAYS;
	public static boolean OFFLINE_DISCONNECT_FINISHED;
	public static boolean OFFLINE_DISCONNECT_SAME_ACCOUNT;
	public static boolean STORE_OFFLINE_TRADE_IN_REALTIME;
	public static boolean ENABLE_OFFLINE_COMMAND;
	public static List<AbnormalVisualEffect> OFFLINE_ABNORMAL_EFFECTS = new ArrayList<>();
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(OFFLINE_TRADE_CONFIG_FILE);
		OFFLINE_TRADE_ENABLE = config.getBoolean("OfflineTradeEnable", false);
		OFFLINE_CRAFT_ENABLE = config.getBoolean("OfflineCraftEnable", false);
		OFFLINE_MODE_IN_PEACE_ZONE = config.getBoolean("OfflineModeInPeaceZone", false);
		OFFLINE_MODE_NO_DAMAGE = config.getBoolean("OfflineModeNoDamage", false);
		OFFLINE_SET_NAME_COLOR = config.getBoolean("OfflineSetNameColor", false);
		OFFLINE_NAME_COLOR = Integer.decode("0x" + config.getString("OfflineNameColor", "808080"));
		OFFLINE_FAME = config.getBoolean("OfflineFame", true);
		RESTORE_OFFLINERS = config.getBoolean("RestoreOffliners", false);
		OFFLINE_MAX_DAYS = config.getInt("OfflineMaxDays", 10);
		OFFLINE_DISCONNECT_FINISHED = config.getBoolean("OfflineDisconnectFinished", true);
		OFFLINE_DISCONNECT_SAME_ACCOUNT = config.getBoolean("OfflineDisconnectSameAccount", false);
		STORE_OFFLINE_TRADE_IN_REALTIME = config.getBoolean("StoreOfflineTradeInRealtime", true);
		ENABLE_OFFLINE_COMMAND = config.getBoolean("EnableOfflineCommand", true);
		
		OFFLINE_ABNORMAL_EFFECTS.clear();
		final String offlineAbnormalEffects = config.getString("OfflineAbnormalEffect", "").trim();
		if (!offlineAbnormalEffects.isEmpty())
		{
			for (String ave : offlineAbnormalEffects.split(","))
			{
				OFFLINE_ABNORMAL_EFFECTS.add(Enum.valueOf(AbnormalVisualEffect.class, ave.trim()));
			}
		}
	}
}
