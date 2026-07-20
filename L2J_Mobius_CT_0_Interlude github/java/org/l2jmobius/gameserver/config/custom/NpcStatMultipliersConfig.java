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
 * This class loads all the custom NPC stat multiplier related configurations.
 * @author Mobius
 */
public class NpcStatMultipliersConfig
{
	// File
	private static final String NPC_STAT_MULTIPLIERS_CONFIG_FILE = "./config/Custom/NpcStatMultipliers.ini";
	
	// Constants
	public static boolean ENABLE_NPC_STAT_MULTIPLIERS;
	public static double MONSTER_HP_MULTIPLIER;
	public static double MONSTER_MP_MULTIPLIER;
	public static double MONSTER_PATK_MULTIPLIER;
	public static double MONSTER_MATK_MULTIPLIER;
	public static double MONSTER_PDEF_MULTIPLIER;
	public static double MONSTER_MDEF_MULTIPLIER;
	public static double MONSTER_AGRRO_RANGE_MULTIPLIER;
	public static double MONSTER_CLAN_HELP_RANGE_MULTIPLIER;
	public static double RAIDBOSS_HP_MULTIPLIER;
	public static double RAIDBOSS_MP_MULTIPLIER;
	public static double RAIDBOSS_PATK_MULTIPLIER;
	public static double RAIDBOSS_MATK_MULTIPLIER;
	public static double RAIDBOSS_PDEF_MULTIPLIER;
	public static double RAIDBOSS_MDEF_MULTIPLIER;
	public static double RAIDBOSS_AGRRO_RANGE_MULTIPLIER;
	public static double RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER;
	public static double GUARD_HP_MULTIPLIER;
	public static double GUARD_MP_MULTIPLIER;
	public static double GUARD_PATK_MULTIPLIER;
	public static double GUARD_MATK_MULTIPLIER;
	public static double GUARD_PDEF_MULTIPLIER;
	public static double GUARD_MDEF_MULTIPLIER;
	public static double GUARD_AGRRO_RANGE_MULTIPLIER;
	public static double GUARD_CLAN_HELP_RANGE_MULTIPLIER;
	public static double DEFENDER_HP_MULTIPLIER;
	public static double DEFENDER_MP_MULTIPLIER;
	public static double DEFENDER_PATK_MULTIPLIER;
	public static double DEFENDER_MATK_MULTIPLIER;
	public static double DEFENDER_PDEF_MULTIPLIER;
	public static double DEFENDER_MDEF_MULTIPLIER;
	public static double DEFENDER_AGRRO_RANGE_MULTIPLIER;
	public static double DEFENDER_CLAN_HELP_RANGE_MULTIPLIER;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(NPC_STAT_MULTIPLIERS_CONFIG_FILE);
		ENABLE_NPC_STAT_MULTIPLIERS = config.getBoolean("EnableNpcStatMultipliers", false);
		MONSTER_HP_MULTIPLIER = config.getDouble("MonsterHP", 1.0);
		MONSTER_MP_MULTIPLIER = config.getDouble("MonsterMP", 1.0);
		MONSTER_PATK_MULTIPLIER = config.getDouble("MonsterPAtk", 1.0);
		MONSTER_MATK_MULTIPLIER = config.getDouble("MonsterMAtk", 1.0);
		MONSTER_PDEF_MULTIPLIER = config.getDouble("MonsterPDef", 1.0);
		MONSTER_MDEF_MULTIPLIER = config.getDouble("MonsterMDef", 1.0);
		MONSTER_AGRRO_RANGE_MULTIPLIER = config.getDouble("MonsterAggroRange", 1.0);
		MONSTER_CLAN_HELP_RANGE_MULTIPLIER = config.getDouble("MonsterClanHelpRange", 1.0);
		RAIDBOSS_HP_MULTIPLIER = config.getDouble("RaidbossHP", 1.0);
		RAIDBOSS_MP_MULTIPLIER = config.getDouble("RaidbossMP", 1.0);
		RAIDBOSS_PATK_MULTIPLIER = config.getDouble("RaidbossPAtk", 1.0);
		RAIDBOSS_MATK_MULTIPLIER = config.getDouble("RaidbossMAtk", 1.0);
		RAIDBOSS_PDEF_MULTIPLIER = config.getDouble("RaidbossPDef", 1.0);
		RAIDBOSS_MDEF_MULTIPLIER = config.getDouble("RaidbossMDef", 1.0);
		RAIDBOSS_AGRRO_RANGE_MULTIPLIER = config.getDouble("RaidbossAggroRange", 1.0);
		RAIDBOSS_CLAN_HELP_RANGE_MULTIPLIER = config.getDouble("RaidbossClanHelpRange", 1.0);
		GUARD_HP_MULTIPLIER = config.getDouble("GuardHP", 1.0);
		GUARD_MP_MULTIPLIER = config.getDouble("GuardMP", 1.0);
		GUARD_PATK_MULTIPLIER = config.getDouble("GuardPAtk", 1.0);
		GUARD_MATK_MULTIPLIER = config.getDouble("GuardMAtk", 1.0);
		GUARD_PDEF_MULTIPLIER = config.getDouble("GuardPDef", 1.0);
		GUARD_MDEF_MULTIPLIER = config.getDouble("GuardMDef", 1.0);
		GUARD_AGRRO_RANGE_MULTIPLIER = config.getDouble("GuardAggroRange", 1.0);
		GUARD_CLAN_HELP_RANGE_MULTIPLIER = config.getDouble("GuardClanHelpRange", 1.0);
		DEFENDER_HP_MULTIPLIER = config.getDouble("DefenderHP", 1.0);
		DEFENDER_MP_MULTIPLIER = config.getDouble("DefenderMP", 1.0);
		DEFENDER_PATK_MULTIPLIER = config.getDouble("DefenderPAtk", 1.0);
		DEFENDER_MATK_MULTIPLIER = config.getDouble("DefenderMAtk", 1.0);
		DEFENDER_PDEF_MULTIPLIER = config.getDouble("DefenderPDef", 1.0);
		DEFENDER_MDEF_MULTIPLIER = config.getDouble("DefenderMDef", 1.0);
		DEFENDER_AGRRO_RANGE_MULTIPLIER = config.getDouble("DefenderAggroRange", 1.0);
		DEFENDER_CLAN_HELP_RANGE_MULTIPLIER = config.getDouble("DefenderClanHelpRange", 1.0);
	}
}
