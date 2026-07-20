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
 * This class loads all the custom captcha related configurations.
 * @author Mobius
 */
public class CaptchaConfig
{
	// File
	private static final String CAPTCHA_CONFIG_FILE = "./config/Custom/Captcha.ini";
	
	// Constants
	public static boolean ENABLE_CAPTCHA;
	public static int KILL_COUNTER;
	public static int KILL_COUNTER_RANDOMIZATION;
	public static boolean KILL_COUNTER_RESET;
	public static int KILL_COUNTER_RESET_TIME;
	public static int VALIDATION_TIME;
	public static int CAPTCHA_ATTEMPTS;
	public static int PUNISHMENT;
	public static int JAIL_TIME;
	public static boolean DOUBLE_JAIL_TIME;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CAPTCHA_CONFIG_FILE);
		ENABLE_CAPTCHA = config.getBoolean("EnableCaptcha", false);
		KILL_COUNTER = config.getInt("KillCounter", 100);
		KILL_COUNTER_RANDOMIZATION = config.getInt("KillCounterRandomization", 50);
		KILL_COUNTER_RESET = config.getBoolean("KillCounterReset", false);
		KILL_COUNTER_RESET_TIME = config.getInt("KillCounterResetTime", 20) * 60000;
		VALIDATION_TIME = config.getInt("ValidationTime", 60);
		CAPTCHA_ATTEMPTS = config.getInt("CaptchaAttempts", 2);
		PUNISHMENT = config.getInt("Punishment", 0);
		JAIL_TIME = config.getInt("JailTime", 2);
		DOUBLE_JAIL_TIME = config.getBoolean("DoubleJailTime", false);
	}
}
