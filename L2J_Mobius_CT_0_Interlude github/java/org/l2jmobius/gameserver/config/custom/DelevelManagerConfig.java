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
 * This class loads all the custom delevel manager related configurations.
 * @author Mobius
 */
public class DelevelManagerConfig
{
	// File
	private static final String DELEVEL_MANAGER_CONFIG_FILE = "./config/Custom/DelevelManager.ini";
	
	// Constants
	public static boolean DELEVEL_MANAGER_ENABLED;
	public static int DELEVEL_MANAGER_NPCID;
	public static int DELEVEL_MANAGER_ITEMID;
	public static int DELEVEL_MANAGER_ITEMCOUNT;
	public static int DELEVEL_MANAGER_MINIMUM_DELEVEL;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(DELEVEL_MANAGER_CONFIG_FILE);
		DELEVEL_MANAGER_ENABLED = config.getBoolean("Enabled", false);
		DELEVEL_MANAGER_NPCID = config.getInt("NpcId", 1002000);
		DELEVEL_MANAGER_ITEMID = config.getInt("RequiredItemId", 4356);
		DELEVEL_MANAGER_ITEMCOUNT = config.getInt("RequiredItemCount", 2);
		DELEVEL_MANAGER_MINIMUM_DELEVEL = config.getInt("MimimumDelevel", 20);
	}
}
