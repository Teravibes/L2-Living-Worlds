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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the olympiad related configurations.
 * @author Mobius
 */
public class OlympiadConfig
{
	// File
	private static final String OLYMPIAD_CONFIG_FILE = "./config/Olympiad.ini";
	
	// Constants
	public static boolean OLYMPIAD_ENABLED;
	public static int OLYMPIAD_START_TIME;
	public static int OLYMPIAD_MIN;
	public static long OLYMPIAD_CPERIOD;
	public static long OLYMPIAD_BATTLE;
	public static long OLYMPIAD_WPERIOD;
	public static long OLYMPIAD_VPERIOD;
	public static int OLYMPIAD_START_POINTS;
	public static int OLYMPIAD_WEEKLY_POINTS;
	public static int OLYMPIAD_CLASSED;
	public static int OLYMPIAD_NONCLASSED;
	public static int OLYMPIAD_REG_DISPLAY;
	public static int OLYMPIAD_BATTLE_REWARD_ITEM;
	public static int OLYMPIAD_CLASSED_RITEM_C;
	public static int OLYMPIAD_NONCLASSED_RITEM_C;
	public static int OLYMPIAD_COMP_RITEM;
	public static int OLYMPIAD_GP_PER_POINT;
	public static int OLYMPIAD_HERO_POINTS;
	public static int OLYMPIAD_MAX_POINTS;
	public static boolean OLYMPIAD_LOG_FIGHTS;
	public static boolean OLYMPIAD_SHOW_MONTHLY_WINNERS;
	public static boolean OLYMPIAD_ANNOUNCE_GAMES;
	public static Set<Integer> LIST_OLY_RESTRICTED_ITEMS = new HashSet<>();
	public static boolean OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS;
	public static int OLYMPIAD_ENCHANT_LIMIT;
	public static int OLYMPIAD_WAIT_TIME;
	public static boolean OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS;
	public static String OLYMPIAD_PERIOD;
	public static int OLYMPIAD_PERIOD_MULTIPLIER;
	public static List<Integer> OLYMPIAD_COMPETITION_DAYS;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(OLYMPIAD_CONFIG_FILE);
		OLYMPIAD_ENABLED = config.getBoolean("OlympiadEnabled", true);
		OLYMPIAD_START_TIME = config.getInt("OlympiadStartTime", 18);
		OLYMPIAD_MIN = config.getInt("OlympiadMin", 0);
		OLYMPIAD_CPERIOD = config.getLong("OlympiadCPeriod", 21600000);
		OLYMPIAD_BATTLE = config.getLong("OlympiadBattle", 360000);
		OLYMPIAD_WPERIOD = config.getLong("OlympiadWPeriod", 604800000);
		OLYMPIAD_VPERIOD = config.getLong("OlympiadVPeriod", 86400000);
		OLYMPIAD_START_POINTS = config.getInt("OlympiadStartPoints", 18);
		OLYMPIAD_WEEKLY_POINTS = config.getInt("OlympiadWeeklyPoints", 3);
		OLYMPIAD_CLASSED = config.getInt("OlympiadClassedParticipants", 5);
		OLYMPIAD_NONCLASSED = config.getInt("OlympiadNonClassedParticipants", 9);
		OLYMPIAD_REG_DISPLAY = config.getInt("OlympiadRegistrationDisplayNumber", 0);
		OLYMPIAD_BATTLE_REWARD_ITEM = config.getInt("OlympiadBattleRewItem", 6651);
		OLYMPIAD_CLASSED_RITEM_C = config.getInt("OlympiadClassedRewItemCount", 50);
		OLYMPIAD_NONCLASSED_RITEM_C = config.getInt("OlympiadNonClassedRewItemCount", 30);
		OLYMPIAD_COMP_RITEM = config.getInt("OlympiadCompRewItem", 6651);
		OLYMPIAD_GP_PER_POINT = config.getInt("OlympiadGPPerPoint", 1000);
		OLYMPIAD_HERO_POINTS = config.getInt("OlympiadHeroPoints", 100);
		OLYMPIAD_MAX_POINTS = config.getInt("OlympiadMaxPoints", 10);
		OLYMPIAD_LOG_FIGHTS = config.getBoolean("OlympiadLogFights", false);
		OLYMPIAD_SHOW_MONTHLY_WINNERS = config.getBoolean("OlympiadShowMonthlyWinners", true);
		OLYMPIAD_ANNOUNCE_GAMES = config.getBoolean("OlympiadAnnounceGames", true);
		final String olyRestrictedItems = config.getString("OlympiadRestrictedItems", "").trim();
		if (!olyRestrictedItems.isEmpty())
		{
			final String[] olyRestrictedItemsSplit = olyRestrictedItems.split(",");
			LIST_OLY_RESTRICTED_ITEMS = new HashSet<>(olyRestrictedItemsSplit.length);
			for (String id : olyRestrictedItemsSplit)
			{
				LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
			}
		}
		else // In case of reload with removal of all items ids.
		{
			LIST_OLY_RESTRICTED_ITEMS.clear();
		}
		OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS = config.getBoolean("OlympiadDisableBlessedSpiritShots", true);
		OLYMPIAD_ENCHANT_LIMIT = config.getInt("OlympiadEnchantLimit", -1);
		OLYMPIAD_WAIT_TIME = config.getInt("OlympiadWaitTime", 120);
		if ((OLYMPIAD_WAIT_TIME != 120) && (OLYMPIAD_WAIT_TIME != 60) && (OLYMPIAD_WAIT_TIME != 30) && (OLYMPIAD_WAIT_TIME != 15) && (OLYMPIAD_WAIT_TIME != 5))
		{
			OLYMPIAD_WAIT_TIME = 120;
		}
		OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS = config.getBoolean("OlympiadUseCustomPeriodSettings", false);
		OLYMPIAD_PERIOD = config.getString("OlympiadPeriod", "MONTH");
		OLYMPIAD_PERIOD_MULTIPLIER = config.getInt("OlympiadPeriodMultiplier", 1);
		OLYMPIAD_COMPETITION_DAYS = new ArrayList<>();
		for (String s : config.getString("OlympiadCompetitionDays", "1,2,3,4,5,6,7").split(","))
		{
			OLYMPIAD_COMPETITION_DAYS.add(Integer.parseInt(s));
		}
	}
}
