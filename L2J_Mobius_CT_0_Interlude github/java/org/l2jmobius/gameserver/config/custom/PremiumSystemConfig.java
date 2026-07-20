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
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the custom premium system related configurations.
 * @author Mobius
 */
public class PremiumSystemConfig
{
	private static final Logger LOGGER = Logger.getLogger(PremiumSystemConfig.class.getName());
	
	// File
	private static final String PREMIUM_SYSTEM_CONFIG_FILE = "./config/Custom/PremiumSystem.ini";
	
	// Constants
	public static boolean PREMIUM_SYSTEM_ENABLED;
	public static boolean PC_CAFE_ENABLED;
	public static boolean PC_CAFE_ONLY_PREMIUM;
	public static boolean PC_CAFE_RETAIL_LIKE;
	public static int PC_CAFE_REWARD_TIME;
	public static int PC_CAFE_MAX_POINTS;
	public static boolean PC_CAFE_ENABLE_DOUBLE_POINTS;
	public static int PC_CAFE_DOUBLE_POINTS_CHANCE;
	public static int ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS;
	public static double PC_CAFE_POINT_RATE;
	public static boolean PC_CAFE_RANDOM_POINT;
	public static boolean PC_CAFE_REWARD_LOW_EXP_KILLS;
	public static int PC_CAFE_LOW_EXP_KILLS_CHANCE;
	public static float PREMIUM_RATE_XP;
	public static float PREMIUM_RATE_SP;
	public static float PREMIUM_RATE_DROP_CHANCE;
	public static float PREMIUM_RATE_DROP_AMOUNT;
	public static float PREMIUM_RATE_SPOIL_CHANCE;
	public static float PREMIUM_RATE_SPOIL_AMOUNT;
	public static float PREMIUM_RATE_QUEST_XP;
	public static float PREMIUM_RATE_QUEST_SP;
	public static Map<Integer, Float> PREMIUM_RATE_DROP_CHANCE_BY_ID;
	public static Map<Integer, Float> PREMIUM_RATE_DROP_AMOUNT_BY_ID;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(PREMIUM_SYSTEM_CONFIG_FILE);
		PREMIUM_SYSTEM_ENABLED = config.getBoolean("EnablePremiumSystem", false);
		PC_CAFE_ENABLED = config.getBoolean("PcCafeEnabled", false);
		PC_CAFE_ONLY_PREMIUM = config.getBoolean("PcCafeOnlyPremium", false);
		PC_CAFE_RETAIL_LIKE = config.getBoolean("PcCafeRetailLike", true);
		PC_CAFE_REWARD_TIME = config.getInt("PcCafeRewardTime", 300000);
		PC_CAFE_MAX_POINTS = config.getInt("MaxPcCafePoints", 200000);
		if (PC_CAFE_MAX_POINTS < 0)
		{
			PC_CAFE_MAX_POINTS = 0;
		}
		PC_CAFE_ENABLE_DOUBLE_POINTS = config.getBoolean("DoublingAcquisitionPoints", false);
		PC_CAFE_DOUBLE_POINTS_CHANCE = config.getInt("DoublingAcquisitionPointsChance", 1);
		if ((PC_CAFE_DOUBLE_POINTS_CHANCE < 0) || (PC_CAFE_DOUBLE_POINTS_CHANCE > 100))
		{
			PC_CAFE_DOUBLE_POINTS_CHANCE = 1;
		}
		ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS = config.getInt("AcquisitionPointsRetailLikePoints", 10);
		PC_CAFE_POINT_RATE = config.getDouble("AcquisitionPointsRate", 1.0);
		PC_CAFE_RANDOM_POINT = config.getBoolean("AcquisitionPointsRandom", false);
		if (PC_CAFE_POINT_RATE < 0)
		{
			PC_CAFE_POINT_RATE = 1;
		}
		PC_CAFE_REWARD_LOW_EXP_KILLS = config.getBoolean("RewardLowExpKills", true);
		PC_CAFE_LOW_EXP_KILLS_CHANCE = config.getInt("RewardLowExpKillsChance", 50);
		if (PC_CAFE_LOW_EXP_KILLS_CHANCE < 0)
		{
			PC_CAFE_LOW_EXP_KILLS_CHANCE = 0;
		}
		if (PC_CAFE_LOW_EXP_KILLS_CHANCE > 100)
		{
			PC_CAFE_LOW_EXP_KILLS_CHANCE = 100;
		}
		PREMIUM_RATE_XP = config.getFloat("PremiumRateXp", 2);
		PREMIUM_RATE_SP = config.getFloat("PremiumRateSp", 2);
		PREMIUM_RATE_DROP_CHANCE = config.getFloat("PremiumRateDropChance", 2);
		PREMIUM_RATE_DROP_AMOUNT = config.getFloat("PremiumRateDropAmount", 1);
		PREMIUM_RATE_SPOIL_CHANCE = config.getFloat("PremiumRateSpoilChance", 2);
		PREMIUM_RATE_SPOIL_AMOUNT = config.getFloat("PremiumRateSpoilAmount", 1);
		PREMIUM_RATE_QUEST_XP = config.getFloat("PremiumRateQuestXp", 1);
		PREMIUM_RATE_QUEST_SP = config.getFloat("PremiumRateQuestSp", 1);
		final String[] premiumDropChanceMultiplier = config.getString("PremiumRateDropChanceByItemId", "").split(";");
		PREMIUM_RATE_DROP_CHANCE_BY_ID = new HashMap<>(premiumDropChanceMultiplier.length);
		if (!premiumDropChanceMultiplier[0].isEmpty())
		{
			for (String item : premiumDropChanceMultiplier)
			{
				final String[] itemSplit = item.split(",");
				if (itemSplit.length != 2)
				{
					LOGGER.warning("PremiumSystemConfig.load(): invalid config property -> PremiumRateDropChanceByItemId \"" + item + "\"");
				}
				else
				{
					try
					{
						PREMIUM_RATE_DROP_CHANCE_BY_ID.put(Integer.parseInt(itemSplit[0]), Float.parseFloat(itemSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!item.isEmpty())
						{
							LOGGER.warning("PremiumSystemConfig.load(): invalid config property -> PremiumRateDropChanceByItemId \"" + item + "\"");
						}
					}
				}
			}
		}
		final String[] premiumDropAmountMultiplier = config.getString("PremiumRateDropAmountByItemId", "").split(";");
		PREMIUM_RATE_DROP_AMOUNT_BY_ID = new HashMap<>(premiumDropAmountMultiplier.length);
		if (!premiumDropAmountMultiplier[0].isEmpty())
		{
			for (String item : premiumDropAmountMultiplier)
			{
				final String[] itemSplit = item.split(",");
				if (itemSplit.length != 2)
				{
					LOGGER.warning("PremiumSystemConfig.load(): invalid config property -> PremiumRateDropAmountByItemId \"" + item + "\"");
				}
				else
				{
					try
					{
						PREMIUM_RATE_DROP_AMOUNT_BY_ID.put(Integer.parseInt(itemSplit[0]), Float.parseFloat(itemSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!item.isEmpty())
						{
							LOGGER.warning("PremiumSystemConfig.load(): invalid config property -> PremiumRateDropAmountByItemId \"" + item + "\"");
						}
					}
				}
			}
		}
	}
}
