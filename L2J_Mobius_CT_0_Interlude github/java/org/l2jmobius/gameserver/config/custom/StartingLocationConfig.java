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
 * This class loads all the custom starting location related configurations.
 * @author Mobius
 */
public class StartingLocationConfig
{
	// File
	private static final String STARTING_LOCATION_CONFIG_FILE = "./config/Custom/StartingLocation.ini";
	
	// Constants
	public static boolean CUSTOM_STARTING_LOC;
	public static int CUSTOM_STARTING_LOC_X;
	public static int CUSTOM_STARTING_LOC_Y;
	public static int CUSTOM_STARTING_LOC_Z;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(STARTING_LOCATION_CONFIG_FILE);
		CUSTOM_STARTING_LOC = config.getBoolean("CustomStartingLocation", false);
		CUSTOM_STARTING_LOC_X = config.getInt("CustomStartingLocX", 50821);
		CUSTOM_STARTING_LOC_Y = config.getInt("CustomStartingLocY", 186527);
		CUSTOM_STARTING_LOC_Z = config.getInt("CustomStartingLocZ", -3625);
	}
}
