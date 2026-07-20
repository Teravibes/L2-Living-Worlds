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
 * This class loads all the custom sell buff related configurations.
 * @author Mobius
 */
public class SellBuffsConfig
{
	// File
	private static final String SELL_BUFFS_CONFIG_FILE = "./config/Custom/SellBuffs.ini";
	
	// Constants
	public static boolean SELLBUFF_ENABLED;
	public static int SELLBUFF_MP_MULTIPLER;
	public static int SELLBUFF_PAYMENT_ID;
	public static long SELLBUFF_MIN_PRICE;
	public static long SELLBUFF_MAX_PRICE;
	public static int SELLBUFF_MAX_BUFFS;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(SELL_BUFFS_CONFIG_FILE);
		SELLBUFF_ENABLED = config.getBoolean("SellBuffEnable", false);
		SELLBUFF_MP_MULTIPLER = config.getInt("MpCostMultipler", 1);
		SELLBUFF_PAYMENT_ID = config.getInt("PaymentID", 57);
		SELLBUFF_MIN_PRICE = config.getLong("MinimumPrice", 100000);
		SELLBUFF_MAX_PRICE = config.getLong("MaximumPrice", 100000000);
		SELLBUFF_MAX_BUFFS = config.getInt("MaxBuffs", 15);
	}
}
