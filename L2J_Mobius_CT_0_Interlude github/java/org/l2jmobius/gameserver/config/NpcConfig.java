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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;

/**
 * This class loads all the NPC related configurations.
 * @author Mobius
 */
public class NpcConfig
{
	private static final Logger LOGGER = Logger.getLogger(NpcConfig.class.getName());
	
	// File
	private static final String NPC_CONFIG_FILE = "./config/NPC.ini";
	
	// Constants
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean ALT_ATTACKABLE_NPCS;
	public static boolean ALT_GAME_VIEWNPC;
	public static boolean SHOW_NPC_LEVEL;
	public static boolean SHOW_NPC_AGGRESSION;
	public static boolean ATTACKABLES_CAMP_PLAYER_CORPSES;
	public static boolean SHOW_CREST_WITHOUT_QUEST;
	public static boolean ENABLE_RANDOM_ENCHANT_EFFECT;
	public static int DECAY_TIME_TASK;
	public static int DEFAULT_CORPSE_TIME;
	public static int SPOILED_CORPSE_EXTEND_TIME;
	public static int CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY;
	public static int MAX_AGGRO_RANGE;
	public static int MAX_DRIFT_RANGE;
	public static boolean AGGRO_DISTANCE_CHECK_ENABLED;
	public static int AGGRO_DISTANCE_CHECK_RANGE;
	public static boolean AGGRO_DISTANCE_CHECK_RAIDS;
	public static int AGGRO_DISTANCE_CHECK_RAID_RANGE;
	public static boolean AGGRO_DISTANCE_CHECK_INSTANCES;
	public static boolean AGGRO_DISTANCE_CHECK_RESTORE_LIFE;
	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static boolean ALLOW_WYVERN_UPGRADER;
	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_PDEFENCE_MULTIPLIER;
	public static double RAID_MDEFENCE_MULTIPLIER;
	public static double RAID_PATTACK_MULTIPLIER;
	public static double RAID_MATTACK_MULTIPLIER;
	public static float RAID_MIN_RESPAWN_MULTIPLIER;
	public static float RAID_MAX_RESPAWN_MULTIPLIER;
	public static double RAID_MINION_RESPAWN_TIMER;
	public static Map<Integer, Integer> MINIONS_RESPAWN_TIME;
	public static boolean FORCE_DELETE_MINIONS;
	public static boolean RAID_DISABLE_CURSE;
	public static int RAID_CHAOS_TIME;
	public static int GRAND_CHAOS_TIME;
	public static int MINION_CHAOS_TIME;
	public static int INVENTORY_MAXIMUM_PET;
	public static double PET_HP_REGEN_MULTIPLIER;
	public static double PET_MP_REGEN_MULTIPLIER;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(NPC_CONFIG_FILE);
		ANNOUNCE_MAMMON_SPAWN = config.getBoolean("AnnounceMammonSpawn", false);
		ALT_MOB_AGRO_IN_PEACEZONE = config.getBoolean("AltMobAgroInPeaceZone", true);
		ALT_ATTACKABLE_NPCS = config.getBoolean("AltAttackableNpcs", true);
		ALT_GAME_VIEWNPC = config.getBoolean("AltGameViewNpc", false);
		SHOW_NPC_LEVEL = config.getBoolean("ShowNpcLevel", false);
		SHOW_NPC_AGGRESSION = config.getBoolean("ShowNpcAggression", false);
		ATTACKABLES_CAMP_PLAYER_CORPSES = config.getBoolean("AttackablesCampPlayerCorpses", false);
		SHOW_CREST_WITHOUT_QUEST = config.getBoolean("ShowCrestWithoutQuest", false);
		ENABLE_RANDOM_ENCHANT_EFFECT = config.getBoolean("EnableRandomEnchantEffect", false);
		DECAY_TIME_TASK = config.getInt("DecayTimeTask", 5000);
		DEFAULT_CORPSE_TIME = config.getInt("DefaultCorpseTime", 7);
		SPOILED_CORPSE_EXTEND_TIME = config.getInt("SpoiledCorpseExtendTime", 10);
		CORPSE_CONSUME_SKILL_ALLOWED_TIME_BEFORE_DECAY = config.getInt("CorpseConsumeSkillAllowedTimeBeforeDecay", 2000);
		MAX_AGGRO_RANGE = config.getInt("MaxAggroRange", 450);
		MAX_DRIFT_RANGE = config.getInt("MaxDriftRange", 300);
		AGGRO_DISTANCE_CHECK_ENABLED = config.getBoolean("AggroDistanceCheckEnabled", true);
		AGGRO_DISTANCE_CHECK_RANGE = config.getInt("AggroDistanceCheckRange", 1500);
		AGGRO_DISTANCE_CHECK_RAIDS = config.getBoolean("AggroDistanceCheckRaids", false);
		AGGRO_DISTANCE_CHECK_RAID_RANGE = config.getInt("AggroDistanceCheckRaidRange", 3000);
		AGGRO_DISTANCE_CHECK_INSTANCES = config.getBoolean("AggroDistanceCheckInstances", false);
		AGGRO_DISTANCE_CHECK_RESTORE_LIFE = config.getBoolean("AggroDistanceCheckRestoreLife", true);
		GUARD_ATTACK_AGGRO_MOB = config.getBoolean("GuardAttackAggroMob", false);
		ALLOW_WYVERN_UPGRADER = config.getBoolean("AllowWyvernUpgrader", false);
		RAID_HP_REGEN_MULTIPLIER = config.getDouble("RaidHpRegenMultiplier", 100) / 100;
		RAID_MP_REGEN_MULTIPLIER = config.getDouble("RaidMpRegenMultiplier", 100) / 100;
		RAID_PDEFENCE_MULTIPLIER = config.getDouble("RaidPDefenceMultiplier", 100) / 100;
		RAID_MDEFENCE_MULTIPLIER = config.getDouble("RaidMDefenceMultiplier", 100) / 100;
		RAID_PATTACK_MULTIPLIER = config.getDouble("RaidPAttackMultiplier", 100) / 100;
		RAID_MATTACK_MULTIPLIER = config.getDouble("RaidMAttackMultiplier", 100) / 100;
		RAID_MIN_RESPAWN_MULTIPLIER = config.getFloat("RaidMinRespawnMultiplier", 1.0f);
		RAID_MAX_RESPAWN_MULTIPLIER = config.getFloat("RaidMaxRespawnMultiplier", 1.0f);
		RAID_MINION_RESPAWN_TIMER = config.getInt("RaidMinionRespawnTime", 300000);
		
		final String[] split = config.getString("CustomMinionsRespawnTime", "").split(";");
		MINIONS_RESPAWN_TIME = new HashMap<>(split.length);
		for (String prop : split)
		{
			final String[] propSplit = prop.split(",");
			if (propSplit.length != 2)
			{
				LOGGER.warning(StringUtil.concat("[CustomMinionsRespawnTime]: invalid config property -> CustomMinionsRespawnTime \"", prop, "\""));
			}
			try
			{
				MINIONS_RESPAWN_TIME.put(Integer.parseInt(propSplit[0]), Integer.parseInt(propSplit[1]));
			}
			catch (NumberFormatException nfe)
			{
				if (!prop.isEmpty())
				{
					LOGGER.warning(StringUtil.concat("[CustomMinionsRespawnTime]: invalid config property -> CustomMinionsRespawnTime \"", propSplit[0], "\"", propSplit[1]));
				}
			}
		}
		
		FORCE_DELETE_MINIONS = config.getBoolean("ForceDeleteMinions", false);
		RAID_DISABLE_CURSE = config.getBoolean("DisableRaidCurse", false);
		RAID_CHAOS_TIME = config.getInt("RaidChaosTime", 10);
		GRAND_CHAOS_TIME = config.getInt("GrandChaosTime", 10);
		MINION_CHAOS_TIME = config.getInt("MinionChaosTime", 10);
		INVENTORY_MAXIMUM_PET = config.getInt("MaximumSlotsForPet", 12);
		PET_HP_REGEN_MULTIPLIER = config.getDouble("PetHpRegenMultiplier", 100) / 100;
		PET_MP_REGEN_MULTIPLIER = config.getDouble("PetMpRegenMultiplier", 100) / 100;
	}
}
