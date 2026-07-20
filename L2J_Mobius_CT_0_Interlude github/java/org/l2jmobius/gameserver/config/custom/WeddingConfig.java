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
 * This class loads all the custom wedding related configurations.
 * @author Mobius
 */
public class WeddingConfig
{
	// File
	private static final String WEDDING_CONFIG_FILE = "./config/Custom/Wedding.ini";
	
	// Constants
	public static boolean ALLOW_WEDDING;
	public static int WEDDING_PRICE;
	public static boolean WEDDING_PUNISH_INFIDELITY;
	public static boolean WEDDING_TELEPORT;
	public static int WEDDING_TELEPORT_PRICE;
	public static int WEDDING_TELEPORT_DURATION;
	public static boolean WEDDING_SAMESEX;
	public static boolean WEDDING_FORMALWEAR;
	public static int WEDDING_DIVORCE_COSTS;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(WEDDING_CONFIG_FILE);
		ALLOW_WEDDING = config.getBoolean("AllowWedding", false);
		WEDDING_PRICE = config.getInt("WeddingPrice", 250000000);
		WEDDING_PUNISH_INFIDELITY = config.getBoolean("WeddingPunishInfidelity", true);
		WEDDING_TELEPORT = config.getBoolean("WeddingTeleport", true);
		WEDDING_TELEPORT_PRICE = config.getInt("WeddingTeleportPrice", 50000);
		WEDDING_TELEPORT_DURATION = config.getInt("WeddingTeleportDuration", 60);
		WEDDING_SAMESEX = config.getBoolean("WeddingAllowSameSex", false);
		WEDDING_FORMALWEAR = config.getBoolean("WeddingFormalWear", true);
		WEDDING_DIVORCE_COSTS = config.getInt("WeddingDivorceCosts", 20);
	}
}
