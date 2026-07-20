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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.groups.PartyExpType;

/**
 * This class loads all the player related configurations.
 * @author Mobius
 */
public class PlayerConfig
{
	private static final Logger LOGGER = Logger.getLogger(PlayerConfig.class.getName());
	
	// File
	private static final String PLAYER_CONFIG_FILE = "./config/Player.ini";
	
	// Constants
	public static boolean PLAYER_DELEVEL;
	public static boolean DECREASE_SKILL_LEVEL;
	public static double ALT_WEIGHT_LIMIT;
	public static int RUN_SPD_BOOST;
	public static int DEATH_PENALTY_CHANCE;
	public static double RESPAWN_RESTORE_CP;
	public static double RESPAWN_RESTORE_HP;
	public static double RESPAWN_RESTORE_MP;
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static boolean ENABLE_MODIFY_SKILL_DURATION;
	public static Map<Integer, Integer> SKILL_DURATION_LIST;
	public static boolean ENABLE_MODIFY_SKILL_REUSE;
	public static Map<Integer, Integer> SKILL_REUSE_LIST;
	public static boolean AUTO_LEARN_SKILLS;
	public static boolean AUTO_LEARN_SKILLS_WITHOUT_ITEMS;
	public static boolean AUTO_LEARN_FS_SKILLS;
	public static boolean SHOW_EFFECT_MESSAGES_ON_LOGIN;
	public static boolean AUTO_LOOT_HERBS;
	public static byte BUFFS_MAX_AMOUNT;
	public static byte DANCES_MAX_AMOUNT;
	public static boolean DANCE_CANCEL_BUFF;
	public static boolean DANCE_CONSUME_ADDITIONAL_MP;
	public static boolean ALT_STORE_DANCES;
	public static boolean ALT_STORE_TOGGLES;
	public static boolean AUTO_LEARN_DIVINE_INSPIRATION;
	public static boolean ALT_GAME_CANCEL_BOW;
	public static boolean ALT_GAME_CANCEL_CAST;
	public static boolean ALT_GAME_MAGICFAILURES;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	public static boolean STORE_SKILL_COOLTIME;
	public static boolean SUBCLASS_STORE_SKILL_COOLTIME;
	public static boolean SUMMON_STORE_SKILL_COOLTIME;
	public static boolean ALT_GAME_SHIELD_BLOCKS;
	public static int ALT_PERFECT_SHLD_BLOCK;
	public static long EFFECT_TICK_RATIO;
	public static boolean FAKE_DEATH_UNTARGET;
	public static boolean FAKE_DEATH_DAMAGE_STAND;
	public static boolean CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL;
	public static boolean CALCULATE_DISTANCE_BOW_DAMAGE;
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean ES_SP_BOOK_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean ALT_GAME_SKILL_LEARN;
	public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
	public static boolean ALT_GAME_SUBCLASS_EVERYWHERE;
	public static boolean RESTORE_SERVITOR_ON_RECONNECT;
	public static boolean RESTORE_PET_ON_RECONNECT;
	public static int FEE_DELETE_TRANSFER_SKILLS;
	public static boolean ENABLE_VITALITY;
	public static boolean RECOVER_VITALITY_ON_RECONNECT;
	public static int STARTING_VITALITY_POINTS;
	public static boolean RAIDBOSS_USE_VITALITY;
	public static double MAX_BONUS_EXP;
	public static double MAX_BONUS_SP;
	public static int MAX_RUN_SPEED;
	public static int MAX_PATK;
	public static int MAX_MATK;
	public static int MAX_PCRIT_RATE;
	public static int MAX_MCRIT_RATE;
	public static int MAX_PATK_SPEED;
	public static int MAX_MATK_SPEED;
	public static int MAX_EVASION;
	public static int MIN_ABNORMAL_STATE_SUCCESS_RATE;
	public static int MAX_ABNORMAL_STATE_SUCCESS_RATE;
	public static long MAX_SP;
	public static byte PLAYER_MAXIMUM_LEVEL;
	public static byte MAX_SUBCLASS;
	public static byte BASE_SUBCLASS_LEVEL;
	public static byte MAX_SUBCLASS_LEVEL;
	public static int MAX_PVTSTORESELL_SLOTS_DWARF;
	public static int MAX_PVTSTORESELL_SLOTS_OTHER;
	public static int MAX_PVTSTOREBUY_SLOTS_DWARF;
	public static int MAX_PVTSTOREBUY_SLOTS_OTHER;
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_GM;
	public static int MAX_ITEM_IN_PACKET;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int ALT_FREIGHT_SLOTS;
	public static int ALT_FREIGHT_PRICE;
	public static int[] ENCHANT_BLACKLIST;
	public static boolean DISABLE_OVER_ENCHANTING;
	public static boolean OVER_ENCHANT_PROTECTION;
	public static IllegalActionPunishmentType OVER_ENCHANT_PUNISHMENT;
	public static int AUGMENTATION_NG_SKILL_CHANCE;
	public static int AUGMENTATION_NG_GLOW_CHANCE;
	public static int AUGMENTATION_MID_SKILL_CHANCE;
	public static int AUGMENTATION_MID_GLOW_CHANCE;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int AUGMENTATION_TOP_SKILL_CHANCE;
	public static int AUGMENTATION_TOP_GLOW_CHANCE;
	public static int AUGMENTATION_BASESTAT_CHANCE;
	public static boolean RETAIL_LIKE_AUGMENTATION;
	public static int[] RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
	public static int[] RETAIL_LIKE_AUGMENTATION_MID_CHANCE;
	public static int[] RETAIL_LIKE_AUGMENTATION_HIGH_CHANCE;
	public static int[] RETAIL_LIKE_AUGMENTATION_TOP_CHANCE;
	public static int[] AUGMENTATION_BLACKLIST;
	public static boolean ALT_ALLOW_AUGMENT_PVP_ITEMS;
	public static boolean ALT_ALLOW_AUGMENT_TRADE;
	public static boolean ALT_ALLOW_AUGMENT_DESTROY;
	public static double SOUL_CRYSTAL_CHANCE_MULTIPLIER;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_SHOP;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_TELEPORT;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_GK;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_TRADE;
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE;
	public static boolean FAME_SYSTEM_ENABLED;
	public static int MAX_PERSONAL_FAME_POINTS;
	public static int FORTRESS_ZONE_FAME_TASK_FREQUENCY;
	public static int FORTRESS_ZONE_FAME_AQUIRE_POINTS;
	public static int CASTLE_ZONE_FAME_TASK_FREQUENCY;
	public static int CASTLE_ZONE_FAME_AQUIRE_POINTS;
	public static boolean FAME_FOR_DEAD_PLAYERS;
	public static boolean IS_CRAFTING_ENABLED;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean ALT_GAME_CREATION;
	public static double ALT_GAME_CREATION_SPEED;
	public static double ALT_GAME_CREATION_XP_RATE;
	public static double ALT_GAME_CREATION_SP_RATE;
	public static boolean ALT_BLACKSMITH_USE_RECIPES;
	public static boolean ALT_CLAN_LEADER_INSTANT_ACTIVATION;
	public static int ALT_CLAN_JOIN_DAYS;
	public static int ALT_CLAN_CREATE_DAYS;
	public static int ALT_CLAN_DISSOLVE_DAYS;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
	public static boolean ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	public static boolean REMOVE_CASTLE_CIRCLETS;
	public static int ALT_PARTY_RANGE;
	public static boolean ALT_LEAVE_PARTY_LEADER;
	public static int STARTING_ADENA;
	public static byte STARTING_LEVEL;
	public static int STARTING_SP;
	public static int MAX_ADENA;
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_RAIDS;
	public static boolean AUTO_LOOT_SLOT_LIMIT;
	public static int LOOT_RAIDS_PRIVILEGE_INTERVAL;
	public static int LOOT_RAIDS_PRIVILEGE_CC_SIZE;
	public static Set<Integer> AUTO_LOOT_ITEM_IDS;
	public static boolean ENABLE_KEYBOARD_MOVEMENT;
	public static int UNSTUCK_INTERVAL;
	public static int TELEPORT_WATCHDOG_TIMEOUT;
	public static int PLAYER_SPAWN_PROTECTION;
	public static int PLAYER_TELEPORT_PROTECTION;
	public static boolean RANDOM_RESPAWN_IN_TOWN_ENABLED;
	public static boolean OFFSET_ON_TELEPORT_ENABLED;
	public static int MAX_OFFSET_ON_TELEPORT;
	public static boolean TELEPORT_WHILE_SIEGE_IN_PROGRESS;
	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;
	public static int MAX_FREE_TELEPORT_LEVEL;
	public static boolean ALT_RECOMMEND;
	public static int DELETE_DAYS;
	public static boolean DISCONNECT_AFTER_DEATH;
	public static PartyExpType PARTY_XP_CUTOFF_METHOD;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static int[][] PARTY_XP_CUTOFF_GAPS;
	public static int[] PARTY_XP_CUTOFF_GAP_PERCENTS;
	public static boolean DISABLE_TUTORIAL;
	public static boolean EXPERTISE_PENALTY;
	public static boolean STORE_RECIPE_SHOPLIST;
	public static String[] FORBIDDEN_NAMES;
	public static boolean SILENCE_MODE_EXCLUDE;
	public static boolean ALT_VALIDATE_TRIGGER_SKILLS;
	public static int PLAYER_MOVEMENT_BLOCK_TIME;
	public static boolean RANDOMIZE_AUTO_ATTACK_DAMAGE;
	public static boolean RANDOMIZE_PHYSICAL_SKILL_DAMAGE;
	public static boolean RANDOMIZE_MAGICAL_SKILL_DAMAGE;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(PLAYER_CONFIG_FILE);
		PLAYER_DELEVEL = config.getBoolean("Delevel", true);
		DECREASE_SKILL_LEVEL = config.getBoolean("DecreaseSkillOnDelevel", true);
		ALT_WEIGHT_LIMIT = config.getDouble("AltWeightLimit", 1);
		RUN_SPD_BOOST = config.getInt("RunSpeedBoost", 0);
		DEATH_PENALTY_CHANCE = config.getInt("DeathPenaltyChance", 20);
		RESPAWN_RESTORE_CP = config.getDouble("RespawnRestoreCP", 0) / 100;
		RESPAWN_RESTORE_HP = config.getDouble("RespawnRestoreHP", 65) / 100;
		RESPAWN_RESTORE_MP = config.getDouble("RespawnRestoreMP", 0) / 100;
		HP_REGEN_MULTIPLIER = config.getDouble("HpRegenMultiplier", 100) / 100;
		MP_REGEN_MULTIPLIER = config.getDouble("MpRegenMultiplier", 100) / 100;
		CP_REGEN_MULTIPLIER = config.getDouble("CpRegenMultiplier", 100) / 100;
		ENABLE_MODIFY_SKILL_DURATION = config.getBoolean("EnableModifySkillDuration", false);
		if (ENABLE_MODIFY_SKILL_DURATION)
		{
			final String[] propertySplit = config.getString("SkillDurationList", "").split(";");
			SKILL_DURATION_LIST = new HashMap<>(propertySplit.length);
			for (String skill : propertySplit)
			{
				final String[] skillSplit = skill.split(",");
				if (skillSplit.length != 2)
				{
					LOGGER.warning("[SkillDurationList]: invalid config property -> SkillDurationList " + skill);
				}
				else
				{
					try
					{
						SKILL_DURATION_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							LOGGER.warning(StringUtil.concat("[SkillDurationList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
						}
					}
				}
			}
		}
		ENABLE_MODIFY_SKILL_REUSE = config.getBoolean("EnableModifySkillReuse", false);
		if (ENABLE_MODIFY_SKILL_REUSE)
		{
			final String[] propertySplit = config.getString("SkillReuseList", "").split(";");
			SKILL_REUSE_LIST = new HashMap<>(propertySplit.length);
			for (String skill : propertySplit)
			{
				final String[] skillSplit = skill.split(",");
				if (skillSplit.length != 2)
				{
					LOGGER.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillReuseList \"", skill, "\""));
				}
				else
				{
					try
					{
						SKILL_REUSE_LIST.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							LOGGER.warning(StringUtil.concat("[SkillReuseList]: invalid config property -> SkillList \"", skillSplit[0], "\"", skillSplit[1]));
						}
					}
				}
			}
		}
		AUTO_LEARN_SKILLS = config.getBoolean("AutoLearnSkills", false);
		AUTO_LEARN_SKILLS_WITHOUT_ITEMS = config.getBoolean("AutoLearnSkillsWithoutItems", false);
		AUTO_LEARN_FS_SKILLS = config.getBoolean("AutoLearnForgottenScrollSkills", false);
		SHOW_EFFECT_MESSAGES_ON_LOGIN = config.getBoolean("ShowEffectMessagesOnLogin", false);
		AUTO_LOOT_HERBS = config.getBoolean("AutoLootHerbs", false);
		BUFFS_MAX_AMOUNT = config.getByte("MaxBuffAmount", (byte) 20);
		DANCES_MAX_AMOUNT = config.getByte("MaxDanceAmount", (byte) 12);
		DANCE_CANCEL_BUFF = config.getBoolean("DanceCancelBuff", false);
		DANCE_CONSUME_ADDITIONAL_MP = config.getBoolean("DanceConsumeAdditionalMP", true);
		ALT_STORE_DANCES = config.getBoolean("AltStoreDances", false);
		ALT_STORE_TOGGLES = config.getBoolean("AltStoreToggles", false);
		AUTO_LEARN_DIVINE_INSPIRATION = config.getBoolean("AutoLearnDivineInspiration", false);
		ALT_GAME_CANCEL_BOW = config.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow") || config.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
		ALT_GAME_CANCEL_CAST = config.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast") || config.getString("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
		ALT_GAME_MAGICFAILURES = config.getBoolean("MagicFailures", true);
		PLAYER_FAKEDEATH_UP_PROTECTION = config.getInt("PlayerFakeDeathUpProtection", 0);
		STORE_SKILL_COOLTIME = config.getBoolean("StoreSkillCooltime", true);
		SUBCLASS_STORE_SKILL_COOLTIME = config.getBoolean("SubclassStoreSkillCooltime", false);
		SUMMON_STORE_SKILL_COOLTIME = config.getBoolean("SummonStoreSkillCooltime", true);
		ALT_GAME_SHIELD_BLOCKS = config.getBoolean("AltShieldBlocks", false);
		ALT_PERFECT_SHLD_BLOCK = config.getInt("AltPerfectShieldBlockRate", 10);
		EFFECT_TICK_RATIO = config.getLong("EffectTickRatio", 666);
		FAKE_DEATH_UNTARGET = config.getBoolean("FakeDeathUntarget", false);
		FAKE_DEATH_DAMAGE_STAND = config.getBoolean("FakeDeathDamageStand", true);
		CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = config.getBoolean("CalculateMagicSuccessBySkillMagicLevel", true);
		CALCULATE_DISTANCE_BOW_DAMAGE = config.getBoolean("DistanceBowDamage", false);
		LIFE_CRYSTAL_NEEDED = config.getBoolean("LifeCrystalNeeded", true);
		ES_SP_BOOK_NEEDED = config.getBoolean("EnchantSkillSpBookNeeded", true);
		DIVINE_SP_BOOK_NEEDED = config.getBoolean("DivineInspirationSpBookNeeded", true);
		ALT_GAME_SKILL_LEARN = config.getBoolean("AltGameSkillLearn", false);
		ALT_GAME_SUBCLASS_WITHOUT_QUESTS = config.getBoolean("AltSubClassWithoutQuests", false);
		ALT_GAME_SUBCLASS_EVERYWHERE = config.getBoolean("AltSubclassEverywhere", false);
		RESTORE_SERVITOR_ON_RECONNECT = config.getBoolean("RestoreServitorOnReconnect", true);
		RESTORE_PET_ON_RECONNECT = config.getBoolean("RestorePetOnReconnect", true);
		FEE_DELETE_TRANSFER_SKILLS = config.getInt("FeeDeleteTransferSkills", 10000000);
		ENABLE_VITALITY = config.getBoolean("EnableVitality", false);
		RECOVER_VITALITY_ON_RECONNECT = config.getBoolean("RecoverVitalityOnReconnect", true);
		STARTING_VITALITY_POINTS = config.getInt("StartingVitalityPoints", 20000);
		RAIDBOSS_USE_VITALITY = config.getBoolean("RaidbossUseVitality", true);
		MAX_BONUS_EXP = config.getDouble("MaxExpBonus", 3.5);
		MAX_BONUS_SP = config.getDouble("MaxSpBonus", 3.5);
		MAX_RUN_SPEED = config.getInt("MaxRunSpeed", 250);
		MAX_PATK = config.getInt("MaxPAtk", 999999);
		MAX_MATK = config.getInt("MaxMAtk", 999999);
		MAX_PCRIT_RATE = config.getInt("MaxPCritRate", 500);
		MAX_MCRIT_RATE = config.getInt("MaxMCritRate", 200);
		MAX_PATK_SPEED = config.getInt("MaxPAtkSpeed", 1500);
		MAX_MATK_SPEED = config.getInt("MaxMAtkSpeed", 1999);
		MAX_EVASION = config.getInt("MaxEvasion", 250);
		MIN_ABNORMAL_STATE_SUCCESS_RATE = config.getInt("MinAbnormalStateSuccessRate", 10);
		MAX_ABNORMAL_STATE_SUCCESS_RATE = config.getInt("MaxAbnormalStateSuccessRate", 90);
		MAX_SP = config.getLong("MaxSp", 50000000000L) >= 0 ? config.getLong("MaxSp", 50000000000L) : Long.MAX_VALUE;
		PLAYER_MAXIMUM_LEVEL = config.getByte("MaximumPlayerLevel", (byte) 80);
		PLAYER_MAXIMUM_LEVEL++;
		MAX_SUBCLASS = config.getByte("MaxSubclass", (byte) 3);
		BASE_SUBCLASS_LEVEL = config.getByte("BaseSubclassLevel", (byte) 40);
		MAX_SUBCLASS_LEVEL = config.getByte("MaxSubclassLevel", (byte) 80);
		MAX_PVTSTORESELL_SLOTS_DWARF = config.getInt("MaxPvtStoreSellSlotsDwarf", 4);
		MAX_PVTSTORESELL_SLOTS_OTHER = config.getInt("MaxPvtStoreSellSlotsOther", 3);
		MAX_PVTSTOREBUY_SLOTS_DWARF = config.getInt("MaxPvtStoreBuySlotsDwarf", 5);
		MAX_PVTSTOREBUY_SLOTS_OTHER = config.getInt("MaxPvtStoreBuySlotsOther", 4);
		INVENTORY_MAXIMUM_NO_DWARF = config.getInt("MaximumSlotsForNoDwarf", 80);
		INVENTORY_MAXIMUM_DWARF = config.getInt("MaximumSlotsForDwarf", 100);
		INVENTORY_MAXIMUM_GM = config.getInt("MaximumSlotsForGMPlayer", 250);
		MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));
		WAREHOUSE_SLOTS_DWARF = config.getInt("MaximumWarehouseSlotsForDwarf", 120);
		WAREHOUSE_SLOTS_NO_DWARF = config.getInt("MaximumWarehouseSlotsForNoDwarf", 100);
		WAREHOUSE_SLOTS_CLAN = config.getInt("MaximumWarehouseSlotsForClan", 150);
		ALT_FREIGHT_SLOTS = config.getInt("MaximumFreightSlots", 200);
		ALT_FREIGHT_PRICE = config.getInt("FreightPrice", 1000);
		final String[] notenchantable = config.getString("EnchantBlackList", "7816,7817,7818,7819,7820,7821,7822,7823,7824,7825,7826,7827,7828,7829,7830,7831,13293,13294,13296").split(",");
		ENCHANT_BLACKLIST = new int[notenchantable.length];
		for (int i = 0; i < notenchantable.length; i++)
		{
			ENCHANT_BLACKLIST[i] = Integer.parseInt(notenchantable[i]);
		}
		Arrays.sort(ENCHANT_BLACKLIST);
		DISABLE_OVER_ENCHANTING = config.getBoolean("DisableOverEnchanting", true);
		OVER_ENCHANT_PROTECTION = config.getBoolean("OverEnchantProtection", true);
		OVER_ENCHANT_PUNISHMENT = IllegalActionPunishmentType.findByName(config.getString("OverEnchantPunishment", "JAIL"));
		AUGMENTATION_NG_SKILL_CHANCE = config.getInt("AugmentationNGSkillChance", 15);
		AUGMENTATION_NG_GLOW_CHANCE = config.getInt("AugmentationNGGlowChance", 0);
		AUGMENTATION_MID_SKILL_CHANCE = config.getInt("AugmentationMidSkillChance", 30);
		AUGMENTATION_MID_GLOW_CHANCE = config.getInt("AugmentationMidGlowChance", 40);
		AUGMENTATION_HIGH_SKILL_CHANCE = config.getInt("AugmentationHighSkillChance", 45);
		AUGMENTATION_HIGH_GLOW_CHANCE = config.getInt("AugmentationHighGlowChance", 70);
		AUGMENTATION_TOP_SKILL_CHANCE = config.getInt("AugmentationTopSkillChance", 60);
		AUGMENTATION_TOP_GLOW_CHANCE = config.getInt("AugmentationTopGlowChance", 100);
		AUGMENTATION_BASESTAT_CHANCE = config.getInt("AugmentationBaseStatChance", 1);
		RETAIL_LIKE_AUGMENTATION = config.getBoolean("RetailLikeAugmentation", true);
		String[] array = config.getString("RetailLikeAugmentationNoGradeChance", "55,35,7,3").split(",");
		RETAIL_LIKE_AUGMENTATION_NG_CHANCE = new int[array.length];
		for (int i = 0; i < 4; i++)
		{
			RETAIL_LIKE_AUGMENTATION_NG_CHANCE[i] = Integer.parseInt(array[i]);
		}
		array = config.getString("RetailLikeAugmentationMidGradeChance", "55,35,7,3").split(",");
		RETAIL_LIKE_AUGMENTATION_MID_CHANCE = new int[array.length];
		for (int i = 0; i < 4; i++)
		{
			RETAIL_LIKE_AUGMENTATION_MID_CHANCE[i] = Integer.parseInt(array[i]);
		}
		array = config.getString("RetailLikeAugmentationHighGradeChance", "55,35,7,3").split(",");
		RETAIL_LIKE_AUGMENTATION_HIGH_CHANCE = new int[array.length];
		for (int i = 0; i < 4; i++)
		{
			RETAIL_LIKE_AUGMENTATION_HIGH_CHANCE[i] = Integer.parseInt(array[i]);
		}
		array = config.getString("RetailLikeAugmentationTopGradeChance", "55,35,7,3").split(",");
		RETAIL_LIKE_AUGMENTATION_TOP_CHANCE = new int[array.length];
		for (int i = 0; i < 4; i++)
		{
			RETAIL_LIKE_AUGMENTATION_TOP_CHANCE[i] = Integer.parseInt(array[i]);
		}
		array = config.getString("AugmentationBlackList", "6656,6657,6658,6659,6660,6661,6662,8191").split(",");
		AUGMENTATION_BLACKLIST = new int[array.length];
		for (int i = 0; i < array.length; i++)
		{
			AUGMENTATION_BLACKLIST[i] = Integer.parseInt(array[i]);
		}
		Arrays.sort(AUGMENTATION_BLACKLIST);
		ALT_ALLOW_AUGMENT_PVP_ITEMS = config.getBoolean("AltAllowAugmentPvPItems", false);
		ALT_ALLOW_AUGMENT_TRADE = config.getBoolean("AltAllowAugmentTrade", false);
		ALT_ALLOW_AUGMENT_DESTROY = config.getBoolean("AltAllowAugmentDestroy", true);
		SOUL_CRYSTAL_CHANCE_MULTIPLIER = config.getDouble("SoulCrystalChanceMultiplier", 1);
		ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = config.getBoolean("AltKarmaPlayerCanBeKilledInPeaceZone", false);
		ALT_GAME_KARMA_PLAYER_CAN_SHOP = config.getBoolean("AltKarmaPlayerCanShop", true);
		ALT_GAME_KARMA_PLAYER_CAN_TELEPORT = config.getBoolean("AltKarmaPlayerCanTeleport", true);
		ALT_GAME_KARMA_PLAYER_CAN_USE_GK = config.getBoolean("AltKarmaPlayerCanUseGK", false);
		ALT_GAME_KARMA_PLAYER_CAN_TRADE = config.getBoolean("AltKarmaPlayerCanTrade", true);
		ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE = config.getBoolean("AltKarmaPlayerCanUseWareHouse", true);
		FAME_SYSTEM_ENABLED = config.getBoolean("EnableFameSystem", true);
		MAX_PERSONAL_FAME_POINTS = config.getInt("MaxPersonalFamePoints", 100000);
		FORTRESS_ZONE_FAME_TASK_FREQUENCY = config.getInt("FortressZoneFameTaskFrequency", 300);
		FORTRESS_ZONE_FAME_AQUIRE_POINTS = config.getInt("FortressZoneFameAquirePoints", 31);
		CASTLE_ZONE_FAME_TASK_FREQUENCY = config.getInt("CastleZoneFameTaskFrequency", 300);
		CASTLE_ZONE_FAME_AQUIRE_POINTS = config.getInt("CastleZoneFameAquirePoints", 125);
		FAME_FOR_DEAD_PLAYERS = config.getBoolean("FameForDeadPlayers", true);
		IS_CRAFTING_ENABLED = config.getBoolean("CraftingEnabled", true);
		DWARF_RECIPE_LIMIT = config.getInt("DwarfRecipeLimit", 50);
		COMMON_RECIPE_LIMIT = config.getInt("CommonRecipeLimit", 50);
		ALT_GAME_CREATION = config.getBoolean("AltGameCreation", false);
		ALT_GAME_CREATION_SPEED = config.getDouble("AltGameCreationSpeed", 1);
		ALT_GAME_CREATION_XP_RATE = config.getDouble("AltGameCreationXpRate", 1);
		ALT_GAME_CREATION_SP_RATE = config.getDouble("AltGameCreationSpRate", 1);
		ALT_BLACKSMITH_USE_RECIPES = config.getBoolean("AltBlacksmithUseRecipes", true);
		ALT_CLAN_LEADER_INSTANT_ACTIVATION = config.getBoolean("AltClanLeaderInstantActivation", false);
		ALT_CLAN_JOIN_DAYS = config.getInt("DaysBeforeJoinAClan", 1);
		ALT_CLAN_CREATE_DAYS = config.getInt("DaysBeforeCreateAClan", 10);
		ALT_CLAN_DISSOLVE_DAYS = config.getInt("DaysToPassToDissolveAClan", 7);
		ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = config.getInt("DaysBeforeJoinAllyWhenLeaved", 1);
		ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = config.getInt("DaysBeforeJoinAllyWhenDismissed", 1);
		ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = config.getInt("DaysBeforeAcceptNewClanWhenDismissed", 1);
		ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = config.getInt("DaysBeforeCreateNewAllyWhenDissolved", 1);
		ALT_MAX_NUM_OF_CLANS_IN_ALLY = config.getInt("AltMaxNumOfClansInAlly", 3);
		ALT_CLAN_MEMBERS_FOR_WAR = config.getInt("AltClanMembersForWar", 15);
		ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE = config.getBoolean("AltNewCharAlwaysIsNewbie", false);
		ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = config.getBoolean("AltMembersCanWithdrawFromClanWH", false);
		REMOVE_CASTLE_CIRCLETS = config.getBoolean("RemoveCastleCirclets", true);
		ALT_PARTY_RANGE = config.getInt("AltPartyRange", 1500);
		ALT_LEAVE_PARTY_LEADER = config.getBoolean("AltLeavePartyLeader", false);
		STARTING_ADENA = config.getInt("StartingAdena", 0);
		STARTING_LEVEL = config.getByte("StartingLevel", (byte) 1);
		STARTING_SP = config.getInt("StartingSP", 0);
		MAX_ADENA = config.getInt("MaxAdena", 2000000000);
		if (MAX_ADENA < 0)
		{
			MAX_ADENA = Integer.MAX_VALUE;
		}
		AUTO_LOOT = config.getBoolean("AutoLoot", false);
		AUTO_LOOT_RAIDS = config.getBoolean("AutoLootRaids", false);
		AUTO_LOOT_SLOT_LIMIT = config.getBoolean("AutoLootSlotLimit", false);
		LOOT_RAIDS_PRIVILEGE_INTERVAL = config.getInt("RaidLootRightsInterval", 900) * 1000;
		LOOT_RAIDS_PRIVILEGE_CC_SIZE = config.getInt("RaidLootRightsCCSize", 45);
		final String[] autoLootItemIds = config.getString("AutoLootItemIds", "0").split(",");
		AUTO_LOOT_ITEM_IDS = new HashSet<>(autoLootItemIds.length);
		for (String item : autoLootItemIds)
		{
			Integer itm = 0;
			try
			{
				itm = Integer.parseInt(item);
			}
			catch (NumberFormatException nfe)
			{
				LOGGER.warning("Auto loot item ids: Wrong ItemId passed: " + item);
				LOGGER.warning(nfe.getMessage());
			}
			if (itm != 0)
			{
				AUTO_LOOT_ITEM_IDS.add(itm);
			}
		}
		ENABLE_KEYBOARD_MOVEMENT = config.getBoolean("KeyboardMovement", true);
		UNSTUCK_INTERVAL = config.getInt("UnstuckInterval", 300);
		TELEPORT_WATCHDOG_TIMEOUT = config.getInt("TeleportWatchdogTimeout", 0);
		PLAYER_SPAWN_PROTECTION = config.getInt("PlayerSpawnProtection", 0);
		PLAYER_TELEPORT_PROTECTION = config.getInt("PlayerTeleportProtection", 0);
		RANDOM_RESPAWN_IN_TOWN_ENABLED = config.getBoolean("RandomRespawnInTownEnabled", true);
		OFFSET_ON_TELEPORT_ENABLED = config.getBoolean("OffsetOnTeleportEnabled", true);
		MAX_OFFSET_ON_TELEPORT = config.getInt("MaxOffsetOnTeleport", 50);
		TELEPORT_WHILE_SIEGE_IN_PROGRESS = config.getBoolean("TeleportWhileSiegeInProgress", true);
		PETITIONING_ALLOWED = config.getBoolean("PetitioningAllowed", true);
		MAX_PETITIONS_PER_PLAYER = config.getInt("MaxPetitionsPerPlayer", 5);
		MAX_PETITIONS_PENDING = config.getInt("MaxPetitionsPending", 25);
		MAX_FREE_TELEPORT_LEVEL = config.getInt("MaxFreeTeleportLevel", 40);
		ALT_RECOMMEND = config.getBoolean("AltRecommend", false);
		DELETE_DAYS = config.getInt("DeleteCharAfterDays", 7);
		DISCONNECT_AFTER_DEATH = config.getBoolean("DisconnectAfterDeath", true);
		PARTY_XP_CUTOFF_METHOD = Enum.valueOf(PartyExpType.class, config.getString("PartyXpCutoffMethod", "LEVEL").toUpperCase());
		PARTY_XP_CUTOFF_PERCENT = config.getDouble("PartyXpCutoffPercent", 3);
		PARTY_XP_CUTOFF_LEVEL = config.getInt("PartyXpCutoffLevel", 20);
		final String[] gaps = config.getString("PartyXpCutoffGaps", "0,9;10,14;15,99").split(";");
		PARTY_XP_CUTOFF_GAPS = new int[gaps.length][2];
		for (int i = 0; i < gaps.length; i++)
		{
			PARTY_XP_CUTOFF_GAPS[i] = new int[]
			{
				Integer.parseInt(gaps[i].split(",")[0]),
				Integer.parseInt(gaps[i].split(",")[1])
			};
		}
		final String[] percents = config.getString("PartyXpCutoffGapPercent", "100;30;0").split(";");
		PARTY_XP_CUTOFF_GAP_PERCENTS = new int[percents.length];
		for (int i = 0; i < percents.length; i++)
		{
			PARTY_XP_CUTOFF_GAP_PERCENTS[i] = Integer.parseInt(percents[i]);
		}
		DISABLE_TUTORIAL = config.getBoolean("DisableTutorial", false);
		EXPERTISE_PENALTY = config.getBoolean("ExpertisePenalty", true);
		STORE_RECIPE_SHOPLIST = config.getBoolean("StoreRecipeShopList", false);
		FORBIDDEN_NAMES = config.getString("ForbiddenNames", "").split(",");
		SILENCE_MODE_EXCLUDE = config.getBoolean("SilenceModeExclude", false);
		ALT_VALIDATE_TRIGGER_SKILLS = config.getBoolean("AltValidateTriggerSkills", false);
		PLAYER_MOVEMENT_BLOCK_TIME = config.getInt("NpcTalkBlockingTime", 0) * 1000;
		RANDOMIZE_AUTO_ATTACK_DAMAGE = config.getBoolean("RandomizeAutoAttackDamage", true);
		RANDOMIZE_PHYSICAL_SKILL_DAMAGE = config.getBoolean("RandomizePhysicalSkillDamage", true);
		RANDOMIZE_MAGICAL_SKILL_DAMAGE = config.getBoolean("RandomizeMagicalSkillDamage", true);
	}
}
