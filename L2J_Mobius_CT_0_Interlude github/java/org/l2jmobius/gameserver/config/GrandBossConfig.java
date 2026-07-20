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
 * This class loads all the grand boss related configurations.
 * @author Mobius
 */
public class GrandBossConfig
{
	// File
	private static final String GRANDBOSS_CONFIG_FILE = "./config/GrandBoss.ini";
	
	// Constants
	public static int ANTHARAS_WAIT_TIME;
	public static int ANTHARAS_SPAWN_INTERVAL;
	public static int ANTHARAS_SPAWN_RANDOM;
	public static boolean ANTHARAS_RECOGNIZE_HERO;
	public static int VALAKAS_WAIT_TIME;
	public static int VALAKAS_SPAWN_INTERVAL;
	public static int VALAKAS_SPAWN_RANDOM;
	public static boolean VALAKAS_RECOGNIZE_HERO;
	public static int BAIUM_SPAWN_INTERVAL;
	public static int BAIUM_SPAWN_RANDOM;
	public static boolean BAIUM_RECOGNIZE_HERO;
	public static int CORE_SPAWN_INTERVAL;
	public static int CORE_SPAWN_RANDOM;
	public static int ORFEN_SPAWN_INTERVAL;
	public static int ORFEN_SPAWN_RANDOM;
	public static int QUEEN_ANT_SPAWN_INTERVAL;
	public static int QUEEN_ANT_SPAWN_RANDOM;
	public static int ZAKEN_SPAWN_INTERVAL;
	public static int ZAKEN_SPAWN_RANDOM;
	public static int FRINTEZZA_SPAWN_INTERVAL;
	public static int FRINTEZZA_SPAWN_RANDOM;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(GRANDBOSS_CONFIG_FILE);
		ANTHARAS_WAIT_TIME = config.getInt("AntharasWaitTime", 30);
		ANTHARAS_SPAWN_INTERVAL = config.getInt("IntervalOfAntharasSpawn", 264);
		ANTHARAS_SPAWN_RANDOM = config.getInt("RandomOfAntharasSpawn", 72);
		ANTHARAS_RECOGNIZE_HERO = config.getBoolean("AntharasRecognizeHero", true);
		VALAKAS_WAIT_TIME = config.getInt("ValakasWaitTime", 30);
		VALAKAS_SPAWN_INTERVAL = config.getInt("IntervalOfValakasSpawn", 264);
		VALAKAS_SPAWN_RANDOM = config.getInt("RandomOfValakasSpawn", 72);
		VALAKAS_RECOGNIZE_HERO = config.getBoolean("ValakasRecognizeHero", true);
		BAIUM_SPAWN_INTERVAL = config.getInt("IntervalOfBaiumSpawn", 168);
		BAIUM_SPAWN_RANDOM = config.getInt("RandomOfBaiumSpawn", 48);
		BAIUM_RECOGNIZE_HERO = config.getBoolean("BaiumRecognizeHero", true);
		CORE_SPAWN_INTERVAL = config.getInt("IntervalOfCoreSpawn", 60);
		CORE_SPAWN_RANDOM = config.getInt("RandomOfCoreSpawn", 24);
		ORFEN_SPAWN_INTERVAL = config.getInt("IntervalOfOrfenSpawn", 48);
		ORFEN_SPAWN_RANDOM = config.getInt("RandomOfOrfenSpawn", 20);
		QUEEN_ANT_SPAWN_INTERVAL = config.getInt("IntervalOfQueenAntSpawn", 36);
		QUEEN_ANT_SPAWN_RANDOM = config.getInt("RandomOfQueenAntSpawn", 17);
		ZAKEN_SPAWN_INTERVAL = config.getInt("IntervalOfZakenSpawn", 36);
		ZAKEN_SPAWN_RANDOM = config.getInt("RandomOfZakenSpawn", 17);
		FRINTEZZA_SPAWN_INTERVAL = config.getInt("IntervalOfFrintezzaSpawn", 48);
		FRINTEZZA_SPAWN_RANDOM = config.getInt("RandomOfFrintezzaSpawn", 8);
	}
}
