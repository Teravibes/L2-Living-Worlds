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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.model.Location;

/**
 * This class loads all the custom community board related configurations.
 * @author Mobius
 */
public class CommunityBoardConfig
{
	// File
	private static final String COMMUNITY_BOARD_CONFIG_FILE = "./config/Custom/CommunityBoard.ini";
	
	// Constants
	public static boolean CUSTOM_CB_ENABLED;
	public static int COMMUNITYBOARD_CURRENCY;
	public static boolean COMMUNITYBOARD_ENABLE_MULTISELLS;
	public static boolean COMMUNITYBOARD_ENABLE_TELEPORTS;
	public static boolean COMMUNITYBOARD_ENABLE_BUFFS;
	public static boolean COMMUNITYBOARD_ENABLE_HEAL;
	public static boolean COMMUNITYBOARD_ENABLE_DELEVEL;
	public static int COMMUNITYBOARD_TELEPORT_PRICE;
	public static int COMMUNITYBOARD_BUFF_PRICE;
	public static int COMMUNITYBOARD_HEAL_PRICE;
	public static int COMMUNITYBOARD_DELEVEL_PRICE;
	public static boolean COMMUNITYBOARD_PEACE_ONLY;
	public static boolean COMMUNITYBOARD_COMBAT_DISABLED;
	public static boolean COMMUNITYBOARD_KARMA_DISABLED;
	public static boolean COMMUNITYBOARD_CAST_ANIMATIONS;
	public static boolean COMMUNITY_PREMIUM_SYSTEM_ENABLED;
	public static int COMMUNITY_PREMIUM_COIN_ID;
	public static int COMMUNITY_PREMIUM_PRICE_PER_DAY;
	public static Set<Integer> COMMUNITY_AVAILABLE_BUFFS;
	public static Map<String, Location> COMMUNITY_AVAILABLE_TELEPORTS;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(COMMUNITY_BOARD_CONFIG_FILE);
		CUSTOM_CB_ENABLED = config.getBoolean("CustomCommunityBoard", false);
		COMMUNITYBOARD_CURRENCY = config.getInt("CommunityCurrencyId", 57);
		COMMUNITYBOARD_ENABLE_MULTISELLS = config.getBoolean("CommunityEnableMultisells", true);
		COMMUNITYBOARD_ENABLE_TELEPORTS = config.getBoolean("CommunityEnableTeleports", true);
		COMMUNITYBOARD_ENABLE_BUFFS = config.getBoolean("CommunityEnableBuffs", true);
		COMMUNITYBOARD_ENABLE_HEAL = config.getBoolean("CommunityEnableHeal", true);
		COMMUNITYBOARD_ENABLE_DELEVEL = config.getBoolean("CommunityEnableDelevel", false);
		COMMUNITYBOARD_TELEPORT_PRICE = config.getInt("CommunityTeleportPrice", 0);
		COMMUNITYBOARD_BUFF_PRICE = config.getInt("CommunityBuffPrice", 0);
		COMMUNITYBOARD_HEAL_PRICE = config.getInt("CommunityHealPrice", 0);
		COMMUNITYBOARD_DELEVEL_PRICE = config.getInt("CommunityDelevelPrice", 0);
		COMMUNITYBOARD_PEACE_ONLY = config.getBoolean("CommunityBoardPeaceOnly", false);
		COMMUNITYBOARD_COMBAT_DISABLED = config.getBoolean("CommunityCombatDisabled", true);
		COMMUNITYBOARD_KARMA_DISABLED = config.getBoolean("CommunityKarmaDisabled", true);
		COMMUNITYBOARD_CAST_ANIMATIONS = config.getBoolean("CommunityCastAnimations", false);
		COMMUNITY_PREMIUM_SYSTEM_ENABLED = config.getBoolean("CommunityPremiumSystem", false);
		COMMUNITY_PREMIUM_COIN_ID = config.getInt("CommunityPremiumBuyCoinId", 57);
		COMMUNITY_PREMIUM_PRICE_PER_DAY = config.getInt("CommunityPremiumPricePerDay", 1000000);
		final String[] allowedBuffs = config.getString("CommunityAvailableBuffs", "").split(",");
		COMMUNITY_AVAILABLE_BUFFS = new HashSet<>(allowedBuffs.length);
		for (String s : allowedBuffs)
		{
			COMMUNITY_AVAILABLE_BUFFS.add(Integer.parseInt(s));
		}
		final String[] availableTeleports = config.getString("CommunityTeleportList", "").split(";");
		COMMUNITY_AVAILABLE_TELEPORTS = new HashMap<>(availableTeleports.length);
		for (String s : availableTeleports)
		{
			final String[] splitInfo = s.split(",");
			COMMUNITY_AVAILABLE_TELEPORTS.put(splitInfo[0], new Location(Integer.parseInt(splitInfo[1]), Integer.parseInt(splitInfo[2]), Integer.parseInt(splitInfo[3])));
		}
	}
}
