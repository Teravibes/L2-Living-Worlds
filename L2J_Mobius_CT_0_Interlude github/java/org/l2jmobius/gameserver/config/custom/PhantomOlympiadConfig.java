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
 * Loads the phantom Olympiad settings (config/Custom/PhantomOlympiad.ini). The Olympiad itself (schedule, match sizes,
 * points and rewards) stays in the stock config/Olympiad.ini.
 */
public class PhantomOlympiadConfig
{
	public static boolean PHANTOM_OLYMPIAD_ENABLED;
	public static int PHANTOM_OLYMPIAD_ROSTER_SIZE;
	public static int PHANTOM_OLYMPIAD_MIN_LEVEL;
	public static int PHANTOM_OLYMPIAD_MAX_LEVEL;
	public static int PHANTOM_OLYMPIAD_RIVALS_PER_CLASS;
	public static int PHANTOM_OLYMPIAD_SIGN_UP_CHANCE_PERCENT;
	public static int PHANTOM_OLYMPIAD_REST_SECONDS;
	public static boolean PHANTOM_OLYMPIAD_REJOIN_PARTY;

	public static void load(String baseConfigPath)
	{
		final String phantomOlympiadConfigFile = String.format("./%s/Custom/PhantomOlympiad.ini", baseConfigPath);
		final ConfigReader config = new ConfigReader(phantomOlympiadConfigFile);
		PHANTOM_OLYMPIAD_ENABLED = config.getBoolean("PhantomOlympiadEnabled", true);
		PHANTOM_OLYMPIAD_ROSTER_SIZE = config.getInt("PhantomOlympiadRosterSize", 40);
		PHANTOM_OLYMPIAD_MIN_LEVEL = config.getInt("PhantomOlympiadMinLevel", 76);
		PHANTOM_OLYMPIAD_MAX_LEVEL = config.getInt("PhantomOlympiadMaxLevel", 80);
		PHANTOM_OLYMPIAD_RIVALS_PER_CLASS = config.getInt("PhantomOlympiadRivalsPerClass", 6);
		PHANTOM_OLYMPIAD_SIGN_UP_CHANCE_PERCENT = config.getInt("PhantomOlympiadSignUpChancePercent", 10);
		PHANTOM_OLYMPIAD_REST_SECONDS = config.getInt("PhantomOlympiadRestSeconds", 180);
		PHANTOM_OLYMPIAD_REJOIN_PARTY = config.getBoolean("PhantomOlympiadRejoinParty", true);
	}
}
