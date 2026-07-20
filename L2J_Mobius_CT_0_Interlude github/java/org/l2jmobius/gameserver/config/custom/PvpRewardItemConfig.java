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

/**
 * This class loads all the custom PVP reward item related configurations.
 * @author Mobius
 */
public class PvpRewardItemConfig
{
	// File
	private static final String PVP_REWARD_ITEM_CONFIG_FILE = "./config/Custom/PvpRewardItem.ini";
	
	// Constants
	public static boolean REWARD_PVP_ITEM;
	public static int REWARD_PVP_ITEM_ID;
	public static int REWARD_PVP_ITEM_AMOUNT;
	public static boolean REWARD_PVP_ITEM_MESSAGE;
	public static boolean REWARD_PK_ITEM;
	public static int REWARD_PK_ITEM_ID;
	public static int REWARD_PK_ITEM_AMOUNT;
	public static boolean REWARD_PK_ITEM_MESSAGE;
	public static boolean DISABLE_REWARDS_IN_INSTANCES;
	public static boolean DISABLE_REWARDS_IN_PVP_ZONES;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(PVP_REWARD_ITEM_CONFIG_FILE);
		REWARD_PVP_ITEM = config.getBoolean("RewardPvpItem", false);
		REWARD_PVP_ITEM_ID = config.getInt("RewardPvpItemId", 57);
		REWARD_PVP_ITEM_AMOUNT = config.getInt("RewardPvpItemAmount", 1000);
		REWARD_PVP_ITEM_MESSAGE = config.getBoolean("RewardPvpItemMessage", true);
		REWARD_PK_ITEM = config.getBoolean("RewardPkItem", false);
		REWARD_PK_ITEM_ID = config.getInt("RewardPkItemId", 57);
		REWARD_PK_ITEM_AMOUNT = config.getInt("RewardPkItemAmount", 500);
		REWARD_PK_ITEM_MESSAGE = config.getBoolean("RewardPkItemMessage", true);
		DISABLE_REWARDS_IN_INSTANCES = config.getBoolean("DisableRewardsInInstances", true);
		DISABLE_REWARDS_IN_PVP_ZONES = config.getBoolean("DisableRewardsInPvpZones", true);
	}
}
