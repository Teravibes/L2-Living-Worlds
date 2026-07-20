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
 * This class loads all the custom nobless master related configurations.
 * @author Mobius
 */
public class NoblessMasterConfig
{
	// File
	private static final String NOBLESS_MASTER_CONFIG_FILE = "./config/Custom/NoblessMaster.ini";
	
	// Constants
	public static boolean NOBLESS_MASTER_ENABLED;
	public static int NOBLESS_MASTER_NPCID;
	public static int NOBLESS_MASTER_LEVEL_REQUIREMENT;
	public static int NOBLESS_MASTER_ITEM_ID;
	public static int NOBLESS_MASTER_ITEM_COUNT;
	public static boolean NOBLESS_MASTER_REWARD_TIARA;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(NOBLESS_MASTER_CONFIG_FILE);
		NOBLESS_MASTER_ENABLED = config.getBoolean("Enabled", false);
		NOBLESS_MASTER_NPCID = config.getInt("NpcId", 1003000);
		NOBLESS_MASTER_LEVEL_REQUIREMENT = config.getInt("LevelRequirement", 80);
		NOBLESS_MASTER_ITEM_ID = config.getInt("ItemId", 57);
		NOBLESS_MASTER_ITEM_COUNT = config.getInt("ItemCount", 0);
		NOBLESS_MASTER_REWARD_TIARA = config.getBoolean("RewardTiara", false);
	}
}
