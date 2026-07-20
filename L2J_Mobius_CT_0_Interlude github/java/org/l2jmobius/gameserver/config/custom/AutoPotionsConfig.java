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
 * This class loads all the custom auto potions related configurations.
 * @author Mobius
 */
public class AutoPotionsConfig
{
	// File
	private static final String AUTO_POTIONS_CONFIG_FILE = "./config/Custom/AutoPotions.ini";
	
	// Constants
	public static boolean AUTO_POTIONS_ENABLED;
	public static boolean AUTO_POTIONS_IN_OLYMPIAD;
	public static int AUTO_POTION_MIN_LEVEL;
	public static boolean AUTO_CP_ENABLED;
	public static boolean AUTO_HP_ENABLED;
	public static boolean AUTO_MP_ENABLED;
	public static int AUTO_CP_PERCENTAGE;
	public static int AUTO_HP_PERCENTAGE;
	public static int AUTO_MP_PERCENTAGE;
	public static Set<Integer> AUTO_CP_ITEM_IDS = new HashSet<>();
	public static Set<Integer> AUTO_HP_ITEM_IDS = new HashSet<>();
	public static Set<Integer> AUTO_MP_ITEM_IDS = new HashSet<>();
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(AUTO_POTIONS_CONFIG_FILE);
		AUTO_POTIONS_ENABLED = config.getBoolean("AutoPotionsEnabled", false);
		AUTO_POTIONS_IN_OLYMPIAD = config.getBoolean("AutoPotionsInOlympiad", false);
		AUTO_POTION_MIN_LEVEL = config.getInt("AutoPotionMinimumLevel", 1);
		AUTO_CP_ENABLED = config.getBoolean("AutoCpEnabled", true);
		AUTO_HP_ENABLED = config.getBoolean("AutoHpEnabled", true);
		AUTO_MP_ENABLED = config.getBoolean("AutoMpEnabled", true);
		AUTO_CP_PERCENTAGE = config.getInt("AutoCpPercentage", 70);
		AUTO_HP_PERCENTAGE = config.getInt("AutoHpPercentage", 70);
		AUTO_MP_PERCENTAGE = config.getInt("AutoMpPercentage", 70);
		
		AUTO_CP_ITEM_IDS.clear();
		for (String s : config.getString("AutoCpItemIds", "0").split(","))
		{
			AUTO_CP_ITEM_IDS.add(Integer.parseInt(s));
		}
		
		AUTO_HP_ITEM_IDS.clear();
		for (String s : config.getString("AutoHpItemIds", "0").split(","))
		{
			AUTO_HP_ITEM_IDS.add(Integer.parseInt(s));
		}
		
		AUTO_MP_ITEM_IDS.clear();
		for (String s : config.getString("AutoMpItemIds", "0").split(","))
		{
			AUTO_MP_ITEM_IDS.add(Integer.parseInt(s));
		}
	}
}
