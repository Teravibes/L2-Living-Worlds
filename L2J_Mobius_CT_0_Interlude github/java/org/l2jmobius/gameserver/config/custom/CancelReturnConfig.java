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
 * This class loads all the cancel return related configurations.
 * @author Naker
 */
public class CancelReturnConfig
{
	// File
	private static final String CANCEL_RETURN_CONFIG_FILE = "./config/Custom/CancelReturn.ini";
	
	// Constants
	public static boolean CANCEL_RETURN_ON;
	public static boolean CANCEL_RETURN_MOB;
	public static boolean CANCEL_RETURN_PLAYER;
	public static boolean CANCEL_RETURN_PLAYER_OLYS;
	public static int TIME_TO_RETURN;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CANCEL_RETURN_CONFIG_FILE);
		CANCEL_RETURN_ON = config.getBoolean("CancelReturn", false);
		CANCEL_RETURN_MOB = config.getBoolean("ReturnMonster", true);
		CANCEL_RETURN_PLAYER = config.getBoolean("ReturnPlayer", true);
		CANCEL_RETURN_PLAYER_OLYS = config.getBoolean("ReturnPlayerOlys", false);
		TIME_TO_RETURN = config.getInt("TimeToReturn", 10) * 1000;
	}
}
