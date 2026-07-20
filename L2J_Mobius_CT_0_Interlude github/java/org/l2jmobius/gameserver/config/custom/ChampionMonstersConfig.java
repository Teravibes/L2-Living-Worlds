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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * This class loads all the custom champion monster related configurations.
 * @author Mobius
 */
public class ChampionMonstersConfig
{
	// File
	private static final String CHAMPION_MONSTERS_CONFIG_FILE = "./config/Custom/ChampionMonsters.ini";
	
	// Constants
	public static boolean CHAMPION_ENABLE;
	public static boolean CHAMPION_PASSIVE;
	public static int CHAMPION_FREQUENCY;
	public static String CHAMP_TITLE;
	public static boolean SHOW_CHAMPION_AURA;
	public static int CHAMP_MIN_LEVEL;
	public static int CHAMP_MAX_LEVEL;
	public static int CHAMPION_HP;
	public static float CHAMPION_HP_REGEN;
	public static float CHAMPION_REWARDS_EXP_SP;
	public static float CHAMPION_REWARDS_CHANCE;
	public static float CHAMPION_REWARDS_AMOUNT;
	public static float CHAMPION_ADENAS_REWARDS_CHANCE;
	public static float CHAMPION_ADENAS_REWARDS_AMOUNT;
	public static float CHAMPION_ATK;
	public static float CHAMPION_SPD_ATK;
	public static int CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE;
	public static int CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE;
	public static List<ItemHolder> CHAMPION_REWARD_ITEMS = new ArrayList<>();
	public static boolean CHAMPION_ENABLE_VITALITY;
	public static boolean CHAMPION_ENABLE_IN_INSTANCES;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CHAMPION_MONSTERS_CONFIG_FILE);
		CHAMPION_ENABLE = config.getBoolean("ChampionEnable", false);
		CHAMPION_PASSIVE = config.getBoolean("ChampionPassive", false);
		CHAMPION_FREQUENCY = config.getInt("ChampionFrequency", 0);
		CHAMP_TITLE = config.getString("ChampionTitle", "Champion");
		SHOW_CHAMPION_AURA = config.getBoolean("ChampionAura", true);
		CHAMP_MIN_LEVEL = config.getInt("ChampionMinLevel", 20);
		CHAMP_MAX_LEVEL = config.getInt("ChampionMaxLevel", 60);
		CHAMPION_HP = config.getInt("ChampionHp", 7);
		CHAMPION_HP_REGEN = config.getFloat("ChampionHpRegen", 1);
		CHAMPION_REWARDS_EXP_SP = config.getFloat("ChampionRewardsExpSp", 8);
		CHAMPION_REWARDS_CHANCE = config.getFloat("ChampionRewardsChance", 8);
		CHAMPION_REWARDS_AMOUNT = config.getFloat("ChampionRewardsAmount", 1);
		CHAMPION_ADENAS_REWARDS_CHANCE = config.getFloat("ChampionAdenasRewardsChance", 1);
		CHAMPION_ADENAS_REWARDS_AMOUNT = config.getFloat("ChampionAdenasRewardsAmount", 1);
		CHAMPION_ATK = config.getFloat("ChampionAtk", 1);
		CHAMPION_SPD_ATK = config.getFloat("ChampionSpdAtk", 1);
		CHAMPION_REWARD_LOWER_LEVEL_ITEM_CHANCE = config.getInt("ChampionRewardLowerLvlItemChance", 0);
		CHAMPION_REWARD_HIGHER_LEVEL_ITEM_CHANCE = config.getInt("ChampionRewardHigherLvlItemChance", 0);
		
		CHAMPION_REWARD_ITEMS.clear();
		for (String s : config.getString("ChampionRewardItems", "4356,10").split(";"))
		{
			if (s.isEmpty())
			{
				continue;
			}
			CHAMPION_REWARD_ITEMS.add(new ItemHolder(Integer.parseInt(s.split(",")[0]), Integer.parseInt(s.split(",")[1])));
		}
		
		CHAMPION_ENABLE_VITALITY = config.getBoolean("ChampionEnableVitality", false);
		CHAMPION_ENABLE_IN_INSTANCES = config.getBoolean("ChampionEnableInInstances", false);
	}
}
