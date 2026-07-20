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

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the conquerable hall siege related configurations.
 * @author Mobius
 */
public class ConquerableHallSiegeConfig
{
	// File
	private static final String CONQUERABLE_HALL_SIEGE_CONFIG_FILE = "./config/ConquerableHallSiege.ini";
	
	// Constants
	public static int CHS_MAX_ATTACKERS;
	public static int CHS_CLAN_MINLEVEL;
	public static int CHS_MAX_FLAGS_PER_CLAN;
	public static boolean CHS_ENABLE_FAME;
	public static int CHS_FAME_AMOUNT;
	public static int CHS_FAME_FREQUENCY;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CONQUERABLE_HALL_SIEGE_CONFIG_FILE);
		CHS_MAX_ATTACKERS = config.getInt("MaxAttackers", 500);
		CHS_CLAN_MINLEVEL = config.getInt("MinClanLevel", 4);
		CHS_MAX_FLAGS_PER_CLAN = config.getInt("MaxFlagsPerClan", 1);
		CHS_ENABLE_FAME = config.getBoolean("EnableFame", false);
		CHS_FAME_AMOUNT = config.getInt("FameAmount", 0);
		CHS_FAME_FREQUENCY = config.getInt("FameFrequency", 0);
	}
}
