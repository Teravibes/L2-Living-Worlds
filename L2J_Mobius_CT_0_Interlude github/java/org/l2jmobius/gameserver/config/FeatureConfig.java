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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;

/**
 * This class loads all the feature related configurations.
 * @author Mobius
 */
public class FeatureConfig
{
	// File
	private static final String FEATURE_CONFIG_FILE = "./config/Feature.ini";
	
	// Constants
	public static long CH_TELE_FEE_RATIO;
	public static int CH_TELE1_FEE;
	public static int CH_TELE2_FEE;
	public static long CH_SUPPORT_FEE_RATIO;
	public static int CH_SUPPORT1_FEE;
	public static int CH_SUPPORT2_FEE;
	public static int CH_SUPPORT3_FEE;
	public static int CH_SUPPORT4_FEE;
	public static int CH_SUPPORT5_FEE;
	public static int CH_SUPPORT6_FEE;
	public static int CH_SUPPORT7_FEE;
	public static int CH_SUPPORT8_FEE;
	public static long CH_MPREG_FEE_RATIO;
	public static int CH_MPREG1_FEE;
	public static int CH_MPREG2_FEE;
	public static int CH_MPREG3_FEE;
	public static int CH_MPREG4_FEE;
	public static int CH_MPREG5_FEE;
	public static long CH_HPREG_FEE_RATIO;
	public static int CH_HPREG1_FEE;
	public static int CH_HPREG2_FEE;
	public static int CH_HPREG3_FEE;
	public static int CH_HPREG4_FEE;
	public static int CH_HPREG5_FEE;
	public static int CH_HPREG6_FEE;
	public static int CH_HPREG7_FEE;
	public static int CH_HPREG8_FEE;
	public static int CH_HPREG9_FEE;
	public static int CH_HPREG10_FEE;
	public static int CH_HPREG11_FEE;
	public static int CH_HPREG12_FEE;
	public static int CH_HPREG13_FEE;
	public static long CH_EXPREG_FEE_RATIO;
	public static int CH_EXPREG1_FEE;
	public static int CH_EXPREG2_FEE;
	public static int CH_EXPREG3_FEE;
	public static int CH_EXPREG4_FEE;
	public static int CH_EXPREG5_FEE;
	public static int CH_EXPREG6_FEE;
	public static int CH_EXPREG7_FEE;
	public static long CH_ITEM_FEE_RATIO;
	public static int CH_ITEM1_FEE;
	public static int CH_ITEM2_FEE;
	public static int CH_ITEM3_FEE;
	public static long CH_CURTAIN_FEE_RATIO;
	public static int CH_CURTAIN1_FEE;
	public static int CH_CURTAIN2_FEE;
	public static long CH_FRONT_FEE_RATIO;
	public static int CH_FRONT1_FEE;
	public static int CH_FRONT2_FEE;
	public static boolean CH_BUFF_FREE;
	public static List<Integer> SIEGE_HOUR_LIST;
	public static long CS_TELE_FEE_RATIO;
	public static int CS_TELE1_FEE;
	public static int CS_TELE2_FEE;
	public static long CS_SUPPORT_FEE_RATIO;
	public static int CS_SUPPORT1_FEE;
	public static int CS_SUPPORT2_FEE;
	public static long CS_MPREG_FEE_RATIO;
	public static int CS_MPREG1_FEE;
	public static int CS_MPREG2_FEE;
	public static long CS_HPREG_FEE_RATIO;
	public static int CS_HPREG1_FEE;
	public static int CS_HPREG2_FEE;
	public static long CS_EXPREG_FEE_RATIO;
	public static int CS_EXPREG1_FEE;
	public static int CS_EXPREG2_FEE;
	public static int OUTER_DOOR_UPGRADE_PRICE2;
	public static int OUTER_DOOR_UPGRADE_PRICE3;
	public static int OUTER_DOOR_UPGRADE_PRICE5;
	public static int INNER_DOOR_UPGRADE_PRICE2;
	public static int INNER_DOOR_UPGRADE_PRICE3;
	public static int INNER_DOOR_UPGRADE_PRICE5;
	public static int WALL_UPGRADE_PRICE2;
	public static int WALL_UPGRADE_PRICE3;
	public static int WALL_UPGRADE_PRICE5;
	public static int TRAP_UPGRADE_PRICE1;
	public static int TRAP_UPGRADE_PRICE2;
	public static int TRAP_UPGRADE_PRICE3;
	public static int TRAP_UPGRADE_PRICE4;
	public static long FS_TELE_FEE_RATIO;
	public static int FS_TELE1_FEE;
	public static int FS_TELE2_FEE;
	public static long FS_SUPPORT_FEE_RATIO;
	public static int FS_SUPPORT1_FEE;
	public static int FS_SUPPORT2_FEE;
	public static long FS_MPREG_FEE_RATIO;
	public static int FS_MPREG1_FEE;
	public static int FS_MPREG2_FEE;
	public static long FS_HPREG_FEE_RATIO;
	public static int FS_HPREG1_FEE;
	public static int FS_HPREG2_FEE;
	public static long FS_EXPREG_FEE_RATIO;
	public static int FS_EXPREG1_FEE;
	public static int FS_EXPREG2_FEE;
	public static int FS_UPDATE_FRQ;
	public static int FS_BLOOD_OATH_COUNT;
	public static int FS_MAX_SUPPLY_LEVEL;
	public static int FS_FEE_FOR_CASTLE;
	public static int FS_MAX_OWN_TIME;
	public static boolean ALT_SEVENSIGNS_OPEN_CATACUMBS;
	public static boolean ALT_SEVENSIGNS_OPEN_NECROPOLIS;
	public static boolean ALT_GAME_CASTLE_DAWN;
	public static boolean ALT_GAME_CASTLE_DUSK;
	public static boolean ALT_GAME_REQUIRE_CLAN_CASTLE;
	public static int ALT_FESTIVAL_MIN_PLAYER;
	public static int ALT_MAXIMUM_PLAYER_CONTRIB;
	public static long ALT_FESTIVAL_MANAGER_START;
	public static long ALT_FESTIVAL_LENGTH;
	public static long ALT_FESTIVAL_CYCLE_LENGTH;
	public static long ALT_FESTIVAL_FIRST_SPAWN;
	public static long ALT_FESTIVAL_FIRST_SWARM;
	public static long ALT_FESTIVAL_SECOND_SPAWN;
	public static long ALT_FESTIVAL_SECOND_SWARM;
	public static long ALT_FESTIVAL_CHEST_SPAWN;
	public static double ALT_SIEGE_DAWN_GATES_PDEF_MULT;
	public static double ALT_SIEGE_DUSK_GATES_PDEF_MULT;
	public static double ALT_SIEGE_DAWN_GATES_MDEF_MULT;
	public static double ALT_SIEGE_DUSK_GATES_MDEF_MULT;
	public static boolean ALT_STRICT_SEVENSIGNS;
	public static boolean ALT_SEVENSIGNS_LAZY_UPDATE;
	public static boolean ALT_SEVENSIGNS_NO_SCHEDULE;
	public static int SSQ_DAWN_TICKET_QUANTITY;
	public static int SSQ_DAWN_TICKET_PRICE;
	public static int SSQ_DAWN_TICKET_BUNDLE;
	public static int SSQ_MANORS_AGREEMENT_ID;
	public static int SSQ_JOIN_DAWN_ADENA_FEE;
	public static int TAKE_FORT_POINTS;
	public static int LOOSE_FORT_POINTS;
	public static int TAKE_CASTLE_POINTS;
	public static int LOOSE_CASTLE_POINTS;
	public static int CASTLE_DEFENDED_POINTS;
	public static int FESTIVAL_WIN_POINTS;
	public static int HERO_POINTS;
	public static int ROYAL_GUARD_COST;
	public static int KNIGHT_UNIT_COST;
	public static int KNIGHT_REINFORCE_COST;
	public static int BALLISTA_POINTS;
	public static int BLOODALLIANCE_POINTS;
	public static int BLOODOATH_POINTS;
	public static int KNIGHTSEPAULETTE_POINTS;
	public static int REPUTATION_SCORE_PER_KILL;
	public static int JOIN_ACADEMY_MIN_REP_SCORE;
	public static int JOIN_ACADEMY_MAX_REP_SCORE;
	public static int RAID_RANKING_1ST;
	public static int RAID_RANKING_2ND;
	public static int RAID_RANKING_3RD;
	public static int RAID_RANKING_4TH;
	public static int RAID_RANKING_5TH;
	public static int RAID_RANKING_6TH;
	public static int RAID_RANKING_7TH;
	public static int RAID_RANKING_8TH;
	public static int RAID_RANKING_9TH;
	public static int RAID_RANKING_10TH;
	public static int RAID_RANKING_UP_TO_50TH;
	public static int RAID_RANKING_UP_TO_100TH;
	public static int CLAN_LEVEL_6_COST;
	public static int CLAN_LEVEL_7_COST;
	public static int CLAN_LEVEL_8_COST;
	public static int CLAN_LEVEL_9_COST;
	public static int CLAN_LEVEL_10_COST;
	public static int CLAN_LEVEL_6_REQUIREMENT;
	public static int CLAN_LEVEL_7_REQUIREMENT;
	public static int CLAN_LEVEL_8_REQUIREMENT;
	public static int CLAN_LEVEL_9_REQUIREMENT;
	public static int CLAN_LEVEL_10_REQUIREMENT;
	public static boolean ALLOW_WYVERN_ALWAYS;
	public static boolean ALLOW_WYVERN_DURING_SIEGE;
	public static boolean ALLOW_MOUNTS_DURING_SIEGE;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FEATURE_CONFIG_FILE);
		CH_TELE_FEE_RATIO = config.getLong("ClanHallTeleportFunctionFeeRatio", 604800000);
		CH_TELE1_FEE = config.getInt("ClanHallTeleportFunctionFeeLvl1", 7000);
		CH_TELE2_FEE = config.getInt("ClanHallTeleportFunctionFeeLvl2", 14000);
		CH_SUPPORT_FEE_RATIO = config.getLong("ClanHallSupportFunctionFeeRatio", 86400000);
		CH_SUPPORT1_FEE = config.getInt("ClanHallSupportFeeLvl1", 2500);
		CH_SUPPORT2_FEE = config.getInt("ClanHallSupportFeeLvl2", 5000);
		CH_SUPPORT3_FEE = config.getInt("ClanHallSupportFeeLvl3", 7000);
		CH_SUPPORT4_FEE = config.getInt("ClanHallSupportFeeLvl4", 11000);
		CH_SUPPORT5_FEE = config.getInt("ClanHallSupportFeeLvl5", 21000);
		CH_SUPPORT6_FEE = config.getInt("ClanHallSupportFeeLvl6", 36000);
		CH_SUPPORT7_FEE = config.getInt("ClanHallSupportFeeLvl7", 37000);
		CH_SUPPORT8_FEE = config.getInt("ClanHallSupportFeeLvl8", 52000);
		CH_MPREG_FEE_RATIO = config.getLong("ClanHallMpRegenerationFunctionFeeRatio", 86400000);
		CH_MPREG1_FEE = config.getInt("ClanHallMpRegenerationFeeLvl1", 2000);
		CH_MPREG2_FEE = config.getInt("ClanHallMpRegenerationFeeLvl2", 3750);
		CH_MPREG3_FEE = config.getInt("ClanHallMpRegenerationFeeLvl3", 6500);
		CH_MPREG4_FEE = config.getInt("ClanHallMpRegenerationFeeLvl4", 13750);
		CH_MPREG5_FEE = config.getInt("ClanHallMpRegenerationFeeLvl5", 20000);
		CH_HPREG_FEE_RATIO = config.getLong("ClanHallHpRegenerationFunctionFeeRatio", 86400000);
		CH_HPREG1_FEE = config.getInt("ClanHallHpRegenerationFeeLvl1", 700);
		CH_HPREG2_FEE = config.getInt("ClanHallHpRegenerationFeeLvl2", 800);
		CH_HPREG3_FEE = config.getInt("ClanHallHpRegenerationFeeLvl3", 1000);
		CH_HPREG4_FEE = config.getInt("ClanHallHpRegenerationFeeLvl4", 1166);
		CH_HPREG5_FEE = config.getInt("ClanHallHpRegenerationFeeLvl5", 1500);
		CH_HPREG6_FEE = config.getInt("ClanHallHpRegenerationFeeLvl6", 1750);
		CH_HPREG7_FEE = config.getInt("ClanHallHpRegenerationFeeLvl7", 2000);
		CH_HPREG8_FEE = config.getInt("ClanHallHpRegenerationFeeLvl8", 2250);
		CH_HPREG9_FEE = config.getInt("ClanHallHpRegenerationFeeLvl9", 2500);
		CH_HPREG10_FEE = config.getInt("ClanHallHpRegenerationFeeLvl10", 3250);
		CH_HPREG11_FEE = config.getInt("ClanHallHpRegenerationFeeLvl11", 3270);
		CH_HPREG12_FEE = config.getInt("ClanHallHpRegenerationFeeLvl12", 4250);
		CH_HPREG13_FEE = config.getInt("ClanHallHpRegenerationFeeLvl13", 5166);
		CH_EXPREG_FEE_RATIO = config.getLong("ClanHallExpRegenerationFunctionFeeRatio", 86400000);
		CH_EXPREG1_FEE = config.getInt("ClanHallExpRegenerationFeeLvl1", 3000);
		CH_EXPREG2_FEE = config.getInt("ClanHallExpRegenerationFeeLvl2", 6000);
		CH_EXPREG3_FEE = config.getInt("ClanHallExpRegenerationFeeLvl3", 9000);
		CH_EXPREG4_FEE = config.getInt("ClanHallExpRegenerationFeeLvl4", 15000);
		CH_EXPREG5_FEE = config.getInt("ClanHallExpRegenerationFeeLvl5", 21000);
		CH_EXPREG6_FEE = config.getInt("ClanHallExpRegenerationFeeLvl6", 23330);
		CH_EXPREG7_FEE = config.getInt("ClanHallExpRegenerationFeeLvl7", 30000);
		CH_ITEM_FEE_RATIO = config.getLong("ClanHallItemCreationFunctionFeeRatio", 86400000);
		CH_ITEM1_FEE = config.getInt("ClanHallItemCreationFunctionFeeLvl1", 30000);
		CH_ITEM2_FEE = config.getInt("ClanHallItemCreationFunctionFeeLvl2", 70000);
		CH_ITEM3_FEE = config.getInt("ClanHallItemCreationFunctionFeeLvl3", 140000);
		CH_CURTAIN_FEE_RATIO = config.getLong("ClanHallCurtainFunctionFeeRatio", 604800000);
		CH_CURTAIN1_FEE = config.getInt("ClanHallCurtainFunctionFeeLvl1", 2000);
		CH_CURTAIN2_FEE = config.getInt("ClanHallCurtainFunctionFeeLvl2", 2500);
		CH_FRONT_FEE_RATIO = config.getLong("ClanHallFrontPlatformFunctionFeeRatio", 259200000);
		CH_FRONT1_FEE = config.getInt("ClanHallFrontPlatformFunctionFeeLvl1", 1300);
		CH_FRONT2_FEE = config.getInt("ClanHallFrontPlatformFunctionFeeLvl2", 4000);
		CH_BUFF_FREE = config.getBoolean("AltClanHallMpBuffFree", false);
		SIEGE_HOUR_LIST = new ArrayList<>();
		for (String hour : config.getString("SiegeHourList", "").split(","))
		{
			if (StringUtil.isNumeric(hour))
			{
				SIEGE_HOUR_LIST.add(Integer.parseInt(hour));
			}
		}
		CS_TELE_FEE_RATIO = config.getLong("CastleTeleportFunctionFeeRatio", 604800000);
		CS_TELE1_FEE = config.getInt("CastleTeleportFunctionFeeLvl1", 1000);
		CS_TELE2_FEE = config.getInt("CastleTeleportFunctionFeeLvl2", 10000);
		CS_SUPPORT_FEE_RATIO = config.getLong("CastleSupportFunctionFeeRatio", 604800000);
		CS_SUPPORT1_FEE = config.getInt("CastleSupportFeeLvl1", 49000);
		CS_SUPPORT2_FEE = config.getInt("CastleSupportFeeLvl2", 120000);
		CS_MPREG_FEE_RATIO = config.getLong("CastleMpRegenerationFunctionFeeRatio", 604800000);
		CS_MPREG1_FEE = config.getInt("CastleMpRegenerationFeeLvl1", 45000);
		CS_MPREG2_FEE = config.getInt("CastleMpRegenerationFeeLvl2", 65000);
		CS_HPREG_FEE_RATIO = config.getLong("CastleHpRegenerationFunctionFeeRatio", 604800000);
		CS_HPREG1_FEE = config.getInt("CastleHpRegenerationFeeLvl1", 12000);
		CS_HPREG2_FEE = config.getInt("CastleHpRegenerationFeeLvl2", 20000);
		CS_EXPREG_FEE_RATIO = config.getLong("CastleExpRegenerationFunctionFeeRatio", 604800000);
		CS_EXPREG1_FEE = config.getInt("CastleExpRegenerationFeeLvl1", 63000);
		CS_EXPREG2_FEE = config.getInt("CastleExpRegenerationFeeLvl2", 70000);
		OUTER_DOOR_UPGRADE_PRICE2 = config.getInt("OuterDoorUpgradePriceLvl2", 3000000);
		OUTER_DOOR_UPGRADE_PRICE3 = config.getInt("OuterDoorUpgradePriceLvl3", 4000000);
		OUTER_DOOR_UPGRADE_PRICE5 = config.getInt("OuterDoorUpgradePriceLvl5", 5000000);
		INNER_DOOR_UPGRADE_PRICE2 = config.getInt("InnerDoorUpgradePriceLvl2", 750000);
		INNER_DOOR_UPGRADE_PRICE3 = config.getInt("InnerDoorUpgradePriceLvl3", 900000);
		INNER_DOOR_UPGRADE_PRICE5 = config.getInt("InnerDoorUpgradePriceLvl5", 1000000);
		WALL_UPGRADE_PRICE2 = config.getInt("WallUpgradePriceLvl2", 1600000);
		WALL_UPGRADE_PRICE3 = config.getInt("WallUpgradePriceLvl3", 1800000);
		WALL_UPGRADE_PRICE5 = config.getInt("WallUpgradePriceLvl5", 2000000);
		TRAP_UPGRADE_PRICE1 = config.getInt("TrapUpgradePriceLvl1", 3000000);
		TRAP_UPGRADE_PRICE2 = config.getInt("TrapUpgradePriceLvl2", 4000000);
		TRAP_UPGRADE_PRICE3 = config.getInt("TrapUpgradePriceLvl3", 5000000);
		TRAP_UPGRADE_PRICE4 = config.getInt("TrapUpgradePriceLvl4", 6000000);
		FS_TELE_FEE_RATIO = config.getLong("FortressTeleportFunctionFeeRatio", 604800000);
		FS_TELE1_FEE = config.getInt("FortressTeleportFunctionFeeLvl1", 1000);
		FS_TELE2_FEE = config.getInt("FortressTeleportFunctionFeeLvl2", 10000);
		FS_SUPPORT_FEE_RATIO = config.getLong("FortressSupportFunctionFeeRatio", 86400000);
		FS_SUPPORT1_FEE = config.getInt("FortressSupportFeeLvl1", 7000);
		FS_SUPPORT2_FEE = config.getInt("FortressSupportFeeLvl2", 17000);
		FS_MPREG_FEE_RATIO = config.getLong("FortressMpRegenerationFunctionFeeRatio", 86400000);
		FS_MPREG1_FEE = config.getInt("FortressMpRegenerationFeeLvl1", 6500);
		FS_MPREG2_FEE = config.getInt("FortressMpRegenerationFeeLvl2", 9300);
		FS_HPREG_FEE_RATIO = config.getLong("FortressHpRegenerationFunctionFeeRatio", 86400000);
		FS_HPREG1_FEE = config.getInt("FortressHpRegenerationFeeLvl1", 2000);
		FS_HPREG2_FEE = config.getInt("FortressHpRegenerationFeeLvl2", 3500);
		FS_EXPREG_FEE_RATIO = config.getLong("FortressExpRegenerationFunctionFeeRatio", 86400000);
		FS_EXPREG1_FEE = config.getInt("FortressExpRegenerationFeeLvl1", 9000);
		FS_EXPREG2_FEE = config.getInt("FortressExpRegenerationFeeLvl2", 10000);
		FS_UPDATE_FRQ = config.getInt("FortressPeriodicUpdateFrequency", 360);
		FS_BLOOD_OATH_COUNT = config.getInt("FortressBloodOathCount", 1);
		FS_MAX_SUPPLY_LEVEL = config.getInt("FortressMaxSupplyLevel", 6);
		FS_FEE_FOR_CASTLE = config.getInt("FortressFeeForCastle", 25000);
		FS_MAX_OWN_TIME = config.getInt("FortressMaximumOwnTime", 168);
		ALT_SEVENSIGNS_OPEN_CATACUMBS = config.getBoolean("AltOpenCatacumbs", false);
		ALT_SEVENSIGNS_OPEN_NECROPOLIS = config.getBoolean("AltOpenNecropolis", false);
		ALT_GAME_CASTLE_DAWN = config.getBoolean("AltCastleForDawn", true);
		ALT_GAME_CASTLE_DUSK = config.getBoolean("AltCastleForDusk", true);
		ALT_GAME_REQUIRE_CLAN_CASTLE = config.getBoolean("AltRequireClanCastle", false);
		ALT_FESTIVAL_MIN_PLAYER = config.getInt("AltFestivalMinPlayer", 5);
		ALT_MAXIMUM_PLAYER_CONTRIB = config.getInt("AltMaxPlayerContrib", 1000000);
		ALT_FESTIVAL_MANAGER_START = config.getLong("AltFestivalManagerStart", 120000);
		ALT_FESTIVAL_LENGTH = config.getLong("AltFestivalLength", 1080000);
		ALT_FESTIVAL_CYCLE_LENGTH = config.getLong("AltFestivalCycleLength", 2280000);
		ALT_FESTIVAL_FIRST_SPAWN = config.getLong("AltFestivalFirstSpawn", 120000);
		ALT_FESTIVAL_FIRST_SWARM = config.getLong("AltFestivalFirstSwarm", 300000);
		ALT_FESTIVAL_SECOND_SPAWN = config.getLong("AltFestivalSecondSpawn", 540000);
		ALT_FESTIVAL_SECOND_SWARM = config.getLong("AltFestivalSecondSwarm", 720000);
		ALT_FESTIVAL_CHEST_SPAWN = config.getLong("AltFestivalChestSpawn", 900000);
		ALT_SIEGE_DAWN_GATES_PDEF_MULT = config.getDouble("AltDawnGatesPdefMult", 1.1);
		ALT_SIEGE_DUSK_GATES_PDEF_MULT = config.getDouble("AltDuskGatesPdefMult", 0.8);
		ALT_SIEGE_DAWN_GATES_MDEF_MULT = config.getDouble("AltDawnGatesMdefMult", 1.1);
		ALT_SIEGE_DUSK_GATES_MDEF_MULT = config.getDouble("AltDuskGatesMdefMult", 0.8);
		ALT_STRICT_SEVENSIGNS = config.getBoolean("StrictSevenSigns", true);
		ALT_SEVENSIGNS_LAZY_UPDATE = config.getBoolean("AltSevenSignsLazyUpdate", true);
		ALT_SEVENSIGNS_NO_SCHEDULE = config.getBoolean("SevenSignsNoSchedule", true);
		SSQ_DAWN_TICKET_QUANTITY = config.getInt("SevenSignsDawnTicketQuantity", 300);
		SSQ_DAWN_TICKET_PRICE = config.getInt("SevenSignsDawnTicketPrice", 1000);
		SSQ_DAWN_TICKET_BUNDLE = config.getInt("SevenSignsDawnTicketBundle", 10);
		SSQ_MANORS_AGREEMENT_ID = config.getInt("SevenSignsManorsAgreementId", 6388);
		SSQ_JOIN_DAWN_ADENA_FEE = config.getInt("SevenSignsJoinDawnFee", 50000);
		TAKE_FORT_POINTS = config.getInt("TakeFortPoints", 200);
		LOOSE_FORT_POINTS = config.getInt("LooseFortPoints", 0);
		TAKE_CASTLE_POINTS = config.getInt("TakeCastlePoints", 1500);
		LOOSE_CASTLE_POINTS = config.getInt("LooseCastlePoints", 3000);
		CASTLE_DEFENDED_POINTS = config.getInt("CastleDefendedPoints", 750);
		FESTIVAL_WIN_POINTS = config.getInt("FestivalOfDarknessWin", 200);
		HERO_POINTS = config.getInt("HeroPoints", 1000);
		ROYAL_GUARD_COST = config.getInt("CreateRoyalGuardCost", 5000);
		KNIGHT_UNIT_COST = config.getInt("CreateKnightUnitCost", 10000);
		KNIGHT_REINFORCE_COST = config.getInt("ReinforceKnightUnitCost", 5000);
		BALLISTA_POINTS = config.getInt("KillBallistaPoints", 30);
		BLOODALLIANCE_POINTS = config.getInt("BloodAlliancePoints", 500);
		BLOODOATH_POINTS = config.getInt("BloodOathPoints", 200);
		KNIGHTSEPAULETTE_POINTS = config.getInt("KnightsEpaulettePoints", 20);
		REPUTATION_SCORE_PER_KILL = config.getInt("ReputationScorePerKill", 1);
		JOIN_ACADEMY_MIN_REP_SCORE = config.getInt("CompleteAcademyMinPoints", 190);
		JOIN_ACADEMY_MAX_REP_SCORE = config.getInt("CompleteAcademyMaxPoints", 650);
		RAID_RANKING_1ST = config.getInt("1stRaidRankingPoints", 1250);
		RAID_RANKING_2ND = config.getInt("2ndRaidRankingPoints", 900);
		RAID_RANKING_3RD = config.getInt("3rdRaidRankingPoints", 700);
		RAID_RANKING_4TH = config.getInt("4thRaidRankingPoints", 600);
		RAID_RANKING_5TH = config.getInt("5thRaidRankingPoints", 450);
		RAID_RANKING_6TH = config.getInt("6thRaidRankingPoints", 350);
		RAID_RANKING_7TH = config.getInt("7thRaidRankingPoints", 300);
		RAID_RANKING_8TH = config.getInt("8thRaidRankingPoints", 200);
		RAID_RANKING_9TH = config.getInt("9thRaidRankingPoints", 150);
		RAID_RANKING_10TH = config.getInt("10thRaidRankingPoints", 100);
		RAID_RANKING_UP_TO_50TH = config.getInt("UpTo50thRaidRankingPoints", 25);
		RAID_RANKING_UP_TO_100TH = config.getInt("UpTo100thRaidRankingPoints", 12);
		CLAN_LEVEL_6_COST = config.getInt("ClanLevel6Cost", 5000);
		CLAN_LEVEL_7_COST = config.getInt("ClanLevel7Cost", 10000);
		CLAN_LEVEL_8_COST = config.getInt("ClanLevel8Cost", 20000);
		CLAN_LEVEL_9_COST = config.getInt("ClanLevel9Cost", 40000);
		CLAN_LEVEL_10_COST = config.getInt("ClanLevel10Cost", 40000);
		CLAN_LEVEL_6_REQUIREMENT = config.getInt("ClanLevel6Requirement", 30);
		CLAN_LEVEL_7_REQUIREMENT = config.getInt("ClanLevel7Requirement", 50);
		CLAN_LEVEL_8_REQUIREMENT = config.getInt("ClanLevel8Requirement", 80);
		CLAN_LEVEL_9_REQUIREMENT = config.getInt("ClanLevel9Requirement", 120);
		CLAN_LEVEL_10_REQUIREMENT = config.getInt("ClanLevel10Requirement", 140);
		ALLOW_WYVERN_ALWAYS = config.getBoolean("AllowRideWyvernAlways", false);
		ALLOW_WYVERN_DURING_SIEGE = config.getBoolean("AllowRideWyvernDuringSiege", true);
		ALLOW_MOUNTS_DURING_SIEGE = config.getBoolean("AllowRideMountsDuringSiege", false);
	}
}
