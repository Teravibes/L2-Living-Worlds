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

import java.util.Arrays;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * This class loads all the custom class balance related configurations.
 * @author Mobius
 */
public class ClassBalanceConfig
{
	// File
	private static final String CLASS_BALANCE_CONFIG_FILE = "./config/Custom/ClassBalance.ini";
	
	// Constants
	public static float[] PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[119];
	public static float[] PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[119];
	public static float[] PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS = new float[119];
	public static float[] PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS = new float[119];
	public static float[] PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS = new float[119];
	public static float[] PLAYER_HEALING_SKILL_MULTIPLIERS = new float[119];
	public static float[] SKILL_MASTERY_CHANCE_MULTIPLIERS = new float[119];
	public static float[] SKILL_REUSE_MULTIPLIERS = new float[119];
	public static float[] EXP_AMOUNT_MULTIPLIERS = new float[119];
	public static float[] SP_AMOUNT_MULTIPLIERS = new float[119];
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CLASS_BALANCE_CONFIG_FILE);
		
		Arrays.fill(PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pveMagicalSkillDamageMultipliers = config.getString("PveMagicalSkillDamageMultipliers", "").trim().split(";");
		if (pveMagicalSkillDamageMultipliers.length > 0)
		{
			for (String info : pveMagicalSkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpMagicalSkillDamageMultipliers = config.getString("PvpMagicalSkillDamageMultipliers", "").trim().split(";");
		if (pvpMagicalSkillDamageMultipliers.length > 0)
		{
			for (String info : pvpMagicalSkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_MAGICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pveMagicalSkillDefenceMultipliers = config.getString("PveMagicalSkillDefenceMultipliers", "").trim().split(";");
		if (pveMagicalSkillDefenceMultipliers.length > 0)
		{
			for (String info : pveMagicalSkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pvpMagicalSkillDefenceMultipliers = config.getString("PvpMagicalSkillDefenceMultipliers", "").trim().split(";");
		if (pvpMagicalSkillDefenceMultipliers.length > 0)
		{
			for (String info : pvpMagicalSkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
		final String[] pveMagicalSkillCriticalChanceMultipliers = config.getString("PveMagicalSkillCriticalChanceMultipliers", "").trim().split(";");
		if (pveMagicalSkillCriticalChanceMultipliers.length > 0)
		{
			for (String info : pveMagicalSkillCriticalChanceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
		final String[] pvpMagicalSkillCriticalChanceMultipliers = config.getString("PvpMagicalSkillCriticalChanceMultipliers", "").trim().split(";");
		if (pvpMagicalSkillCriticalChanceMultipliers.length > 0)
		{
			for (String info : pvpMagicalSkillCriticalChanceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pveMagicalSkillCriticalDamageMultipliers = config.getString("PveMagicalSkillCriticalDamageMultipliers", "").trim().split(";");
		if (pveMagicalSkillCriticalDamageMultipliers.length > 0)
		{
			for (String info : pveMagicalSkillCriticalDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpMagicalSkillCriticalDamageMultipliers = config.getString("PvpMagicalSkillCriticalDamageMultipliers", "").trim().split(";");
		if (pvpMagicalSkillCriticalDamageMultipliers.length > 0)
		{
			for (String info : pvpMagicalSkillCriticalDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_MAGICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvePhysicalSkillDamageMultipliers = config.getString("PvePhysicalSkillDamageMultipliers", "").trim().split(";");
		if (pvePhysicalSkillDamageMultipliers.length > 0)
		{
			for (String info : pvePhysicalSkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalSkillDamageMultipliers = config.getString("PvpPhysicalSkillDamageMultipliers", "").trim().split(";");
		if (pvpPhysicalSkillDamageMultipliers.length > 0)
		{
			for (String info : pvpPhysicalSkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pvePhysicalSkillDefenceMultipliers = config.getString("PvePhysicalSkillDefenceMultipliers", "").trim().split(";");
		if (pvePhysicalSkillDefenceMultipliers.length > 0)
		{
			for (String info : pvePhysicalSkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalSkillDefenceMultipliers = config.getString("PvpPhysicalSkillDefenceMultipliers", "").trim().split(";");
		if (pvpPhysicalSkillDefenceMultipliers.length > 0)
		{
			for (String info : pvpPhysicalSkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
		final String[] pvePhysicalSkillCriticalChanceMultipliers = config.getString("PvePhysicalSkillCriticalChanceMultipliers", "").trim().split(";");
		if (pvePhysicalSkillCriticalChanceMultipliers.length > 0)
		{
			for (String info : pvePhysicalSkillCriticalChanceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalSkillCriticalChanceMultipliers = config.getString("PvpPhysicalSkillCriticalChanceMultipliers", "").trim().split(";");
		if (pvpPhysicalSkillCriticalChanceMultipliers.length > 0)
		{
			for (String info : pvpPhysicalSkillCriticalChanceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvePhysicalSkillCriticalDamageMultipliers = config.getString("PvePhysicalSkillCriticalDamageMultipliers", "").trim().split(";");
		if (pvePhysicalSkillCriticalDamageMultipliers.length > 0)
		{
			for (String info : pvePhysicalSkillCriticalDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalSkillCriticalDamageMultipliers = config.getString("PvpPhysicalSkillCriticalDamageMultipliers", "").trim().split(";");
		if (pvpPhysicalSkillCriticalDamageMultipliers.length > 0)
		{
			for (String info : pvpPhysicalSkillCriticalDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_SKILL_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvePhysicalAttackDamageMultipliers = config.getString("PvePhysicalAttackDamageMultipliers", "").trim().split(";");
		if (pvePhysicalAttackDamageMultipliers.length > 0)
		{
			for (String info : pvePhysicalAttackDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalAttackDamageMultipliers = config.getString("PvpPhysicalAttackDamageMultipliers", "").trim().split(";");
		if (pvpPhysicalAttackDamageMultipliers.length > 0)
		{
			for (String info : pvpPhysicalAttackDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_ATTACK_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS, 1f);
		final String[] pvePhysicalAttackDefenceMultipliers = config.getString("PvePhysicalAttackDefenceMultipliers", "").trim().split(";");
		if (pvePhysicalAttackDefenceMultipliers.length > 0)
		{
			for (String info : pvePhysicalAttackDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalAttackDefenceMultipliers = config.getString("PvpPhysicalAttackDefenceMultipliers", "").trim().split(";");
		if (pvpPhysicalAttackDefenceMultipliers.length > 0)
		{
			for (String info : pvpPhysicalAttackDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS, 1f);
		final String[] pvePhysicalAttackCriticalChanceMultipliers = config.getString("PvePhysicalAttackCriticalChanceMultipliers", "").trim().split(";");
		if (pvePhysicalAttackCriticalChanceMultipliers.length > 0)
		{
			for (String info : pvePhysicalAttackCriticalChanceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalAttackCriticalChanceMultipliers = config.getString("PvpPhysicalAttackCriticalChanceMultipliers", "").trim().split(";");
		if (pvpPhysicalAttackCriticalChanceMultipliers.length > 0)
		{
			for (String info : pvpPhysicalAttackCriticalChanceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_ATTACK_CRITICAL_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvePhysicalAttackCriticalDamageMultipliers = config.getString("PvePhysicalAttackCriticalDamageMultipliers", "").trim().split(";");
		if (pvePhysicalAttackCriticalDamageMultipliers.length > 0)
		{
			for (String info : pvePhysicalAttackCriticalDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpPhysicalAttackCriticalDamageMultipliers = config.getString("PvpPhysicalAttackCriticalDamageMultipliers", "").trim().split(";");
		if (pvpPhysicalAttackCriticalDamageMultipliers.length > 0)
		{
			for (String info : pvpPhysicalAttackCriticalDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_PHYSICAL_ATTACK_CRITICAL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pveBlowSkillDamageMultipliers = config.getString("PveBlowSkillDamageMultipliers", "").trim().split(";");
		if (pveBlowSkillDamageMultipliers.length > 0)
		{
			for (String info : pveBlowSkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_BLOW_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpBlowSkillDamageMultipliers = config.getString("PvpBlowSkillDamageMultipliers", "").trim().split(";");
		if (pvpBlowSkillDamageMultipliers.length > 0)
		{
			for (String info : pvpBlowSkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_BLOW_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pveBlowSkillDefenceMultipliers = config.getString("PveBlowSkillDefenceMultipliers", "").trim().split(";");
		if (pveBlowSkillDefenceMultipliers.length > 0)
		{
			for (String info : pveBlowSkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_BLOW_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pvpBlowSkillDefenceMultipliers = config.getString("PvpBlowSkillDefenceMultipliers", "").trim().split(";");
		if (pvpBlowSkillDefenceMultipliers.length > 0)
		{
			for (String info : pvpBlowSkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_BLOW_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pveEnergySkillDamageMultipliers = config.getString("PveEnergySkillDamageMultipliers", "").trim().split(";");
		if (pveEnergySkillDamageMultipliers.length > 0)
		{
			for (String info : pveEnergySkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS, 1f);
		final String[] pvpEnergySkillDamageMultipliers = config.getString("PvpEnergySkillDamageMultipliers", "").trim().split(";");
		if (pvpEnergySkillDamageMultipliers.length > 0)
		{
			for (String info : pvpEnergySkillDamageMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pveEnergySkillDefenceMultipliers = config.getString("PveEnergySkillDefenceMultipliers", "").trim().split(";");
		if (pveEnergySkillDefenceMultipliers.length > 0)
		{
			for (String info : pveEnergySkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS, 1f);
		final String[] pvpEnergySkillDefenceMultipliers = config.getString("PvpEnergySkillDefenceMultipliers", "").trim().split(";");
		if (pvpEnergySkillDefenceMultipliers.length > 0)
		{
			for (String info : pvpEnergySkillDefenceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(PLAYER_HEALING_SKILL_MULTIPLIERS, 1f);
		final String[] playerHealingSkillMultipliers = config.getString("PlayerHealingSkillMultipliers", "").trim().split(";");
		if (playerHealingSkillMultipliers.length > 0)
		{
			for (String info : playerHealingSkillMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					PLAYER_HEALING_SKILL_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(SKILL_MASTERY_CHANCE_MULTIPLIERS, 1f);
		final String[] skillMasteryChanceMultipliers = config.getString("SkillMasteryChanceMultipliers", "").trim().split(";");
		if (skillMasteryChanceMultipliers.length > 0)
		{
			for (String info : skillMasteryChanceMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					SKILL_MASTERY_CHANCE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(SKILL_REUSE_MULTIPLIERS, 1f);
		final String[] skillReuseMultipliers = config.getString("SkillReuseMultipliers", "").trim().split(";");
		if (skillReuseMultipliers.length > 0)
		{
			for (String info : skillReuseMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					SKILL_REUSE_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(EXP_AMOUNT_MULTIPLIERS, 1f);
		final String[] expAmountMultipliers = config.getString("ExpAmountMultipliers", "").trim().split(";");
		if (expAmountMultipliers.length > 0)
		{
			for (String info : expAmountMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					EXP_AMOUNT_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
		
		Arrays.fill(SP_AMOUNT_MULTIPLIERS, 1f);
		final String[] spAmountMultipliers = config.getString("SpAmountMultipliers", "").trim().split(";");
		if (spAmountMultipliers.length > 0)
		{
			for (String info : spAmountMultipliers)
			{
				final String[] classInfo = info.trim().split("[*]");
				if (classInfo.length == 2)
				{
					final String id = classInfo[0].trim();
					SP_AMOUNT_MULTIPLIERS[StringUtil.isNumeric(id) ? Integer.parseInt(id) : Enum.valueOf(PlayerClass.class, id).getId()] = Float.parseFloat(classInfo[1].trim());
				}
			}
		}
	}
}
