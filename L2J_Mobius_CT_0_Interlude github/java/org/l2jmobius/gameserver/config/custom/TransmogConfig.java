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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the custom transmog related configurations.
 * @author Mobius
 */
public class TransmogConfig
{
	// File
	private static final String TRANSMOG_CONFIG_FILE = "./config/Custom/Transmog.ini";
	
	// Constants
	public static boolean ENABLE_TRANSMOG;
	public static boolean TRANSMOG_SHARE_ACCOUNT;
	public static int TRANSMOG_APPLY_COST;
	public static int TRANSMOG_REMOVE_COST;
	public static Set<Integer> TRANSMOG_BANNED_ITEM_IDS = new HashSet<>();
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(TRANSMOG_CONFIG_FILE);
		ENABLE_TRANSMOG = config.getBoolean("TransmogEnabled", false);
		TRANSMOG_SHARE_ACCOUNT = config.getBoolean("TransmogShareAccount", false);
		TRANSMOG_APPLY_COST = config.getInt("TransmogApplyCost", 0);
		TRANSMOG_REMOVE_COST = config.getInt("TransmogRemoveCost", 0);
		TRANSMOG_BANNED_ITEM_IDS.clear();
		final String transmogBannedItemIds = config.getString("TransmogBannedItemIds", "");
		if (!transmogBannedItemIds.isEmpty())
		{
			for (String s : transmogBannedItemIds.split(","))
			{
				TRANSMOG_BANNED_ITEM_IDS.add(Integer.parseInt(s.trim()));
			}
		}
	}
}
