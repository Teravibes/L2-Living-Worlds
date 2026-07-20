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
 * This class loads all the custom PVP title color related configurations.
 * @author Mobius
 */
public class PvpTitleColorConfig
{
	// File
	private static final String PVP_TITLE_CONFIG_FILE = "./config/Custom/PvpTitleColor.ini";
	
	// Constants
	public static boolean PVP_COLOR_SYSTEM_ENABLED;
	public static int PVP_AMOUNT1;
	public static int PVP_AMOUNT2;
	public static int PVP_AMOUNT3;
	public static int PVP_AMOUNT4;
	public static int PVP_AMOUNT5;
	public static int NAME_COLOR_FOR_PVP_AMOUNT1;
	public static int NAME_COLOR_FOR_PVP_AMOUNT2;
	public static int NAME_COLOR_FOR_PVP_AMOUNT3;
	public static int NAME_COLOR_FOR_PVP_AMOUNT4;
	public static int NAME_COLOR_FOR_PVP_AMOUNT5;
	public static String TITLE_FOR_PVP_AMOUNT1;
	public static String TITLE_FOR_PVP_AMOUNT2;
	public static String TITLE_FOR_PVP_AMOUNT3;
	public static String TITLE_FOR_PVP_AMOUNT4;
	public static String TITLE_FOR_PVP_AMOUNT5;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(PVP_TITLE_CONFIG_FILE);
		PVP_COLOR_SYSTEM_ENABLED = config.getBoolean("EnablePvPColorSystem", false);
		PVP_AMOUNT1 = config.getInt("PvpAmount1", 500);
		PVP_AMOUNT2 = config.getInt("PvpAmount2", 1000);
		PVP_AMOUNT3 = config.getInt("PvpAmount3", 1500);
		PVP_AMOUNT4 = config.getInt("PvpAmount4", 2500);
		PVP_AMOUNT5 = config.getInt("PvpAmount5", 5000);
		NAME_COLOR_FOR_PVP_AMOUNT1 = Integer.decode("0x" + config.getString("ColorForAmount1", "00FF00"));
		NAME_COLOR_FOR_PVP_AMOUNT2 = Integer.decode("0x" + config.getString("ColorForAmount2", "00FF00"));
		NAME_COLOR_FOR_PVP_AMOUNT3 = Integer.decode("0x" + config.getString("ColorForAmount3", "00FF00"));
		NAME_COLOR_FOR_PVP_AMOUNT4 = Integer.decode("0x" + config.getString("ColorForAmount4", "00FF00"));
		NAME_COLOR_FOR_PVP_AMOUNT5 = Integer.decode("0x" + config.getString("ColorForAmount5", "00FF00"));
		TITLE_FOR_PVP_AMOUNT1 = config.getString("PvPTitleForAmount1", "Title");
		TITLE_FOR_PVP_AMOUNT2 = config.getString("PvPTitleForAmount2", "Title");
		TITLE_FOR_PVP_AMOUNT3 = config.getString("PvPTitleForAmount3", "Title");
		TITLE_FOR_PVP_AMOUNT4 = config.getString("PvPTitleForAmount4", "Title");
		TITLE_FOR_PVP_AMOUNT5 = config.getString("PvPTitleForAmount5", "Title");
	}
}
