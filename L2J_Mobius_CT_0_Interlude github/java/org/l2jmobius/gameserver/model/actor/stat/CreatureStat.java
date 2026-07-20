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
package org.l2jmobius.gameserver.model.actor.stat;

import java.util.Arrays;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Calculator;
import org.l2jmobius.gameserver.model.stats.MoveType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.TraitType;
import org.l2jmobius.gameserver.model.zone.ZoneId;

public class CreatureStat
{
	private static final int DIVINE_INSPIRATION = 1405;
	
	// Elementals
	private static final byte FIRE = 0;
	private static final byte WATER = 1;
	private static final byte WIND = 2;
	private static final byte EARTH = 3;
	private static final byte HOLY = 4;
	private static final byte DARK = 5;
	
	private final Creature _creature;
	private long _exp = 0;
	private long _sp = 0;
	private byte _level = 1;
	private final float[] _attackTraits = new float[TraitType.values().length];
	private final int[] _attackTraitsCount = new int[TraitType.values().length];
	private final float[] _defenceTraits = new float[TraitType.values().length];
	private final int[] _defenceTraitsCount = new int[TraitType.values().length];
	private final int[] _traitsInvul = new int[TraitType.values().length];
	
	/** Creature's maximum buff count. */
	private int _maxBuffCount = PlayerConfig.BUFFS_MAX_AMOUNT;
	
	/** Speed multiplier set by admin gmspeed command */
	private double _gmSpeedMultiplier = 1;
	
	public CreatureStat(Creature creature)
	{
		_creature = creature;
		Arrays.fill(_attackTraits, 1.0f);
		Arrays.fill(_defenceTraits, 1.0f);
	}
	
	public double calcStat(Stat stat, double init)
	{
		return calcStat(stat, init, null, null);
	}
	
	/**
	 * Calculate the new value of the state with modifiers that will be applied on the targeted Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * A Creature owns a table of Calculators called <b>_calculators</b>. Each Calculator (a calculator per state) own a table of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...) :<br>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<br>
	 * When the calc method of a calculator is launched, each mathematical function is called according to its priority <b>_order</b>.<br>
	 * Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order.<br>
	 * The result of the calculation is stored in the value property of an Env class instance.
	 * @param stat The stat to calculate the new value with modifiers
	 * @param initVal The initial value of the stat before applying modifiers
	 * @param target The Creature whose properties will be used in the calculation (ex : CON, INT...)
	 * @param skill The Skill whose properties will be used in the calculation (ex : Level...)
	 * @return
	 */
	public double calcStat(Stat stat, double initVal, Creature target, Skill skill)
	{
		double value = initVal;
		if (stat == null)
		{
			return value;
		}
		
		final int id = stat.ordinal();
		final Calculator calc = _creature.getCalculators()[id];
		
		// If no Func object found, no modifier is applied
		if ((calc == null) || (calc.size() == 0))
		{
			return value;
		}
		
		// Launch the calculation
		value = calc.calc(_creature, target, skill, value);
		
		// avoid some troubles with negative stats (some stats should never be negative)
		if (value <= 0)
		{
			switch (stat)
			{
				case MAX_HP:
				case MAX_MP:
				case MAX_CP:
				case MAGIC_DEFENCE:
				case POWER_DEFENCE:
				case POWER_ATTACK:
				case MAGIC_ATTACK:
				case POWER_ATTACK_SPEED:
				case MAGIC_ATTACK_SPEED:
				case SHIELD_DEFENCE:
				case STAT_CON:
				case STAT_DEX:
				case STAT_INT:
				case STAT_MEN:
				case STAT_STR:
				case STAT_WIT:
				{
					value = 1.0;
					break;
				}
			}
		}
		
		return value;
	}
	
	/**
	 * @return the Accuracy (base+modifier) of the Creature in function of the Weapon Expertise Penalty.
	 */
	public int getAccuracy()
	{
		return (int) Math.round(calcStat(Stat.ACCURACY_COMBAT, 0, null, null));
	}
	
	public Creature getActiveChar()
	{
		return _creature;
	}
	
	/**
	 * @return the Attack Speed multiplier (base+modifier) of the Creature to get proper animations.
	 */
	public float getAttackSpeedMultiplier()
	{
		return (float) ((1.1 * getPAtkSpd()) / _creature.getTemplate().getBasePAtkSpd());
	}
	
	/**
	 * @param target
	 * @param init
	 * @return the Critical Damage rate (base+modifier) of the Creature.
	 */
	public double getCriticalDmg(Creature target, double init)
	{
		return calcStat(Stat.CRITICAL_DAMAGE, init, target, null);
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return the Critical Hit rate (base+modifier) of the Creature.
	 */
	public int getCriticalHit(Creature target, Skill skill)
	{
		double val = calcStat(Stat.CRITICAL_RATE, _creature.getTemplate().getBaseCritRate(), target, skill);
		
		// if (!_creature.isGM())
		// {
		val = Math.min(val, PlayerConfig.MAX_PCRIT_RATE);
		// }
		
		return (int) (val + .5);
	}
	
	/**
	 * @param base
	 * @return the Critical Hit Pos rate of the Creature
	 */
	public int getCriticalHitPos(int base)
	{
		return (int) calcStat(Stat.CRITICAL_RATE_POS, base);
	}
	
	/**
	 * @param target
	 * @return the Attack Evasion rate (base+modifier) of the Creature.
	 */
	public int getEvasionRate(Creature target)
	{
		int val = (int) Math.round(calcStat(Stat.EVASION_RATE, 0, target, null));
		
		// if (!_creature.isGM())
		// {
		val = Math.min(val, PlayerConfig.MAX_EVASION);
		// }
		
		return val;
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public void setExp(long value)
	{
		_exp = value;
	}
	
	public byte getLevel()
	{
		return _level;
	}
	
	public void setLevel(byte value)
	{
		_level = value;
	}
	
	/**
	 * @param skill
	 * @return the Magical Attack range (base+modifier) of the Creature.
	 */
	public int getMagicalAttackRange(Skill skill)
	{
		if (skill != null)
		{
			return (int) calcStat(Stat.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill);
		}
		
		return _creature.getTemplate().getBaseAttackRange();
	}
	
	public int getMaxCp()
	{
		return (int) calcStat(Stat.MAX_CP, _creature.getTemplate().getBaseCpMax());
	}
	
	public int getMaxRecoverableCp()
	{
		return (int) calcStat(Stat.MAX_RECOVERABLE_CP, getMaxCp());
	}
	
	public int getMaxHp()
	{
		return (int) calcStat(Stat.MAX_HP, _creature.getTemplate().getBaseHpMax());
	}
	
	public int getMaxRecoverableHp()
	{
		return (int) calcStat(Stat.MAX_RECOVERABLE_HP, getMaxHp());
	}
	
	public int getMaxMp()
	{
		return (int) calcStat(Stat.MAX_MP, _creature.getTemplate().getBaseMpMax());
	}
	
	public int getMaxRecoverableMp()
	{
		return (int) calcStat(Stat.MAX_RECOVERABLE_MP, getMaxMp());
	}
	
	/**
	 * Return the MAtk (base+modifier) of the Creature.<br>
	 * <br>
	 * <b><u>Example of use</u>: Calculate Magic damage
	 * @param target The Creature targeted by the skill
	 * @param skill The Skill used against the target
	 * @return
	 */
	public double getMAtk(Creature target, Skill skill)
	{
		float bonusAtk = 1;
		if (ChampionMonstersConfig.CHAMPION_ENABLE && _creature.isChampion())
		{
			bonusAtk = ChampionMonstersConfig.CHAMPION_ATK;
		}
		
		if (_creature.isRaid())
		{
			bonusAtk *= NpcConfig.RAID_MATTACK_MULTIPLIER;
		}
		
		// Calculate modifiers Magic Attack
		return Math.min(calcStat(Stat.MAGIC_ATTACK, _creature.getTemplate().getBaseMAtk() * bonusAtk, target, skill), PlayerConfig.MAX_MATK);
	}
	
	/**
	 * @return the MAtk Speed (base+modifier) of the Creature in function of the Armour Expertise Penalty.
	 */
	public int getMAtkSpd()
	{
		float bonusSpdAtk = 1;
		if (ChampionMonstersConfig.CHAMPION_ENABLE && _creature.isChampion())
		{
			bonusSpdAtk = ChampionMonstersConfig.CHAMPION_SPD_ATK;
		}
		
		double val = calcStat(Stat.MAGIC_ATTACK_SPEED, _creature.getTemplate().getBaseMAtkSpd() * bonusSpdAtk);
		
		// if (!_creature.isGM())
		// {
		val = Math.min(val, PlayerConfig.MAX_MATK_SPEED);
		// }
		
		return (int) val;
	}
	
	/**
	 * @param target
	 * @param skill
	 * @return the Magic Critical Hit rate (base+modifier) of the Creature.
	 */
	public int getMCriticalHit(Creature target, Skill skill)
	{
		int val = (int) calcStat(Stat.MCRITICAL_RATE, 1, target, skill) * 10;
		
		if ((skill != null) && (target != null) && _creature.isPlayable())
		{
			if (target.isPlayable())
			{
				val *= ClassBalanceConfig.PVP_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[_creature.asPlayer().getPlayerClass().getId()];
			}
			else
			{
				val *= ClassBalanceConfig.PVE_MAGICAL_SKILL_CRITICAL_CHANCE_MULTIPLIERS[_creature.asPlayer().getPlayerClass().getId()];
			}
		}
		
		// if (!_creature.isGM())
		// {
		val = Math.min(val, PlayerConfig.MAX_MCRIT_RATE);
		// }
		
		return val;
	}
	
	/**
	 * <b><u>Example of use </u>: Calculate Magic damage.
	 * @param target The Creature targeted by the skill
	 * @param skill The Skill used against the target
	 * @return the MDef (base+modifier) of the Creature against a skill in function of abnormal effects in progress.
	 */
	public double getMDef(Creature target, Skill skill)
	{
		// Get the base MDef of the Creature
		double defence = _creature.getTemplate().getBaseMDef();
		
		// Calculate modifier for Raid Bosses
		if (_creature.isRaid())
		{
			defence *= NpcConfig.RAID_MDEFENCE_MULTIPLIER;
		}
		
		if ((skill != null) && (target != null) && target.isPlayable())
		{
			if (skill.isMagic())
			{
				defence *= ClassBalanceConfig.PVE_MAGICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
			}
			else
			{
				defence *= ClassBalanceConfig.PVE_PHYSICAL_SKILL_DEFENCE_MULTIPLIERS[target.asPlayer().getPlayerClass().getId()];
			}
		}
		
		// Calculate modifiers Magic Attack
		return calcStat(Stat.MAGIC_DEFENCE, defence, target, skill);
	}
	
	/**
	 * @return the CON of the Creature (base+modifier).
	 */
	public int getCON()
	{
		return (int) calcStat(Stat.STAT_CON, _creature.getTemplate().getBaseCON());
	}
	
	/**
	 * @return the DEX of the Creature (base+modifier).
	 */
	public int getDEX()
	{
		return (int) calcStat(Stat.STAT_DEX, _creature.getTemplate().getBaseDEX());
	}
	
	/**
	 * @return the INT of the Creature (base+modifier).
	 */
	public int getINT()
	{
		return (int) calcStat(Stat.STAT_INT, _creature.getTemplate().getBaseINT());
	}
	
	/**
	 * @return the MEN of the Creature (base+modifier).
	 */
	public int getMEN()
	{
		return (int) calcStat(Stat.STAT_MEN, _creature.getTemplate().getBaseMEN());
	}
	
	/**
	 * @return the STR of the Creature (base+modifier).
	 */
	public int getSTR()
	{
		return (int) calcStat(Stat.STAT_STR, _creature.getTemplate().getBaseSTR());
	}
	
	/**
	 * @return the WIT of the Creature (base+modifier).
	 */
	public int getWIT()
	{
		return (int) calcStat(Stat.STAT_WIT, _creature.getTemplate().getBaseWIT());
	}
	
	public double getMovementSpeedMultiplier()
	{
		double baseSpeed;
		if (_creature.isInsideZone(ZoneId.WATER))
		{
			baseSpeed = getBaseMoveSpeed(_creature.isRunning() ? MoveType.FAST_SWIM : MoveType.SLOW_SWIM);
		}
		else
		{
			baseSpeed = getBaseMoveSpeed(_creature.isRunning() ? MoveType.RUN : MoveType.WALK);
		}
		
		return getMoveSpeed() * (1. / baseSpeed);
	}
	
	public void setGmSpeedMultiplier(double multipier)
	{
		_gmSpeedMultiplier = multipier;
	}
	
	/**
	 * @return the RunSpeed (base+modifier) of the Creature in function of the Armour Expertise Penalty.
	 */
	public double getRunSpeed()
	{
		final double baseRunSpd = _creature.isInsideZone(ZoneId.WATER) ? getSwimRunSpeed() : getBaseMoveSpeed(MoveType.RUN);
		if (baseRunSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stat.MOVE_SPEED, baseRunSpd * _gmSpeedMultiplier, null, null);
	}
	
	/**
	 * @return the WalkSpeed (base+modifier) of the Creature.
	 */
	public double getWalkSpeed()
	{
		final double baseWalkSpd = _creature.isInsideZone(ZoneId.WATER) ? getSwimWalkSpeed() : getBaseMoveSpeed(MoveType.WALK);
		if (baseWalkSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stat.MOVE_SPEED, baseWalkSpd * _gmSpeedMultiplier);
	}
	
	/**
	 * @return the SwimRunSpeed (base+modifier) of the Creature.
	 */
	public double getSwimRunSpeed()
	{
		final double baseRunSpd = getBaseMoveSpeed(MoveType.FAST_SWIM);
		if (baseRunSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stat.MOVE_SPEED, baseRunSpd * _gmSpeedMultiplier, null, null);
	}
	
	/**
	 * @return the SwimWalkSpeed (base+modifier) of the Creature.
	 */
	public double getSwimWalkSpeed()
	{
		final double baseWalkSpd = getBaseMoveSpeed(MoveType.SLOW_SWIM);
		if (baseWalkSpd <= 0)
		{
			return 0;
		}
		
		return calcStat(Stat.MOVE_SPEED, baseWalkSpd * _gmSpeedMultiplier);
	}
	
	/**
	 * @param type movement type
	 * @return the base move speed of given movement type.
	 */
	public double getBaseMoveSpeed(MoveType type)
	{
		return _creature.getTemplate().getBaseMoveSpeed(type);
	}
	
	/**
	 * @return the RunSpeed (base+modifier) or WalkSpeed (base+modifier) of the Creature in function of the movement type.
	 */
	public double getMoveSpeed()
	{
		if (_creature.isInsideZone(ZoneId.WATER))
		{
			return _creature.isRunning() ? getSwimRunSpeed() : getSwimWalkSpeed();
		}
		
		return _creature.isRunning() ? getRunSpeed() : getWalkSpeed();
	}
	
	/**
	 * @param skill
	 * @return the MReuse rate (base+modifier) of the Creature.
	 */
	public double getMReuseRate(Skill skill)
	{
		return calcStat(Stat.MAGIC_REUSE_RATE, 1, null, skill);
	}
	
	/**
	 * @param target
	 * @return the PAtk (base+modifier) of the Creature.
	 */
	public double getPAtk(Creature target)
	{
		float bonusAtk = 1;
		if (ChampionMonstersConfig.CHAMPION_ENABLE && _creature.isChampion())
		{
			bonusAtk = ChampionMonstersConfig.CHAMPION_ATK;
		}
		
		if (_creature.isRaid())
		{
			bonusAtk *= NpcConfig.RAID_PATTACK_MULTIPLIER;
		}
		
		return Math.min(calcStat(Stat.POWER_ATTACK, _creature.getTemplate().getBasePAtk() * bonusAtk, target, null), PlayerConfig.MAX_PATK);
	}
	
	/**
	 * @return the PAtk Speed (base+modifier) of the Creature in function of the Armour Expertise Penalty.
	 */
	public double getPAtkSpd()
	{
		float bonusAtk = 1;
		if (ChampionMonstersConfig.CHAMPION_ENABLE && _creature.isChampion())
		{
			bonusAtk = ChampionMonstersConfig.CHAMPION_SPD_ATK;
		}
		
		return Math.round(calcStat(Stat.POWER_ATTACK_SPEED, _creature.getTemplate().getBasePAtkSpd() * bonusAtk, null, null));
	}
	
	/**
	 * @param target
	 * @return the PDef (base+modifier) of the Creature.
	 */
	public double getPDef(Creature target)
	{
		if (_creature.isPlayable() && (target != null))
		{
			if (target.isPlayable())
			{
				return ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[_creature.asPlayer().getPlayerClass().getId()] * calcStat(Stat.POWER_DEFENCE, _creature.isRaid() ? _creature.getTemplate().getBasePDef() * NpcConfig.RAID_PDEFENCE_MULTIPLIER : _creature.getTemplate().getBasePDef(), target, null);
			}
			
			return ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[_creature.asPlayer().getPlayerClass().getId()] * calcStat(Stat.POWER_DEFENCE, _creature.isRaid() ? _creature.getTemplate().getBasePDef() * NpcConfig.RAID_PDEFENCE_MULTIPLIER : _creature.getTemplate().getBasePDef(), target, null);
		}
		
		return calcStat(Stat.POWER_DEFENCE, _creature.isRaid() ? _creature.getTemplate().getBasePDef() * NpcConfig.RAID_PDEFENCE_MULTIPLIER : _creature.getTemplate().getBasePDef(), target, null);
	}
	
	/**
	 * @return the Physical Attack range (base+modifier) of the Creature.
	 */
	public int getPhysicalAttackRange()
	{
		int baseAttackRange;
		if (_creature.getActiveWeaponItem() == null)
		{
			baseAttackRange = _creature.getTemplate().getBaseAttackRange();
		}
		else // Use weapon pAtkRange stat.
		{
			baseAttackRange = 0;
		}
		
		return (int) calcStat(Stat.POWER_ATTACK_RANGE, baseAttackRange, null, null);
	}
	
	public int getPhysicalAttackAngle()
	{
		return 0;
	}
	
	/**
	 * @param target
	 * @return the weapon reuse modifier.
	 */
	public double getWeaponReuseModifier(Creature target)
	{
		return calcStat(Stat.ATK_REUSE, 1, target, null);
	}
	
	/**
	 * @return the ShieldDef rate (base+modifier) of the Creature.
	 */
	public int getShldDef()
	{
		return (int) calcStat(Stat.SHIELD_DEFENCE, 0);
	}
	
	public long getSp()
	{
		return _sp;
	}
	
	public void setSp(long value)
	{
		_sp = value;
	}
	
	/**
	 * @param skill
	 * @return the mpConsume.
	 */
	public int getMpConsume(Skill skill)
	{
		if (skill == null)
		{
			return 1;
		}
		
		double mpConsume = skill.getMpConsume();
		final double nextDanceMpCost = Math.ceil(skill.getMpConsume() / 2.);
		if (skill.isDance() && PlayerConfig.DANCE_CONSUME_ADDITIONAL_MP && (_creature != null) && (_creature.getDanceCount() > 0))
		{
			mpConsume += _creature.getDanceCount() * nextDanceMpCost;
		}
		
		mpConsume = calcStat(Stat.MP_CONSUME, mpConsume, null, skill);
		if (skill.isDance())
		{
			return (int) calcStat(Stat.DANCE_MP_CONSUME_RATE, mpConsume);
		}
		
		if (skill.isMagic())
		{
			return (int) calcStat(Stat.MAGICAL_MP_CONSUME_RATE, mpConsume);
		}
		
		return (int) calcStat(Stat.PHYSICAL_MP_CONSUME_RATE, mpConsume);
	}
	
	/**
	 * @param skill
	 * @return the mpInitialConsume.
	 */
	public int getMpInitialConsume(Skill skill)
	{
		return skill == null ? 1 : (int) calcStat(Stat.MP_CONSUME, skill.getMpInitialConsume(), null, skill);
	}
	
	public byte getAttackElement()
	{
		// temp fix starts
		int tempVal = 0;
		final int[] stats =
		{
			0,
			0,
			0,
			0,
			0,
			0
		};
		
		byte returnVal = -2;
		stats[0] = (int) calcStat(Stat.FIRE_POWER, _creature.getTemplate().getBaseFire());
		stats[1] = (int) calcStat(Stat.WATER_POWER, _creature.getTemplate().getBaseWater());
		stats[2] = (int) calcStat(Stat.WIND_POWER, _creature.getTemplate().getBaseWind());
		stats[3] = (int) calcStat(Stat.EARTH_POWER, _creature.getTemplate().getBaseEarth());
		stats[4] = (int) calcStat(Stat.HOLY_POWER, _creature.getTemplate().getBaseHoly());
		stats[5] = (int) calcStat(Stat.DARK_POWER, _creature.getTemplate().getBaseDark());
		for (byte x = 0; x < 6; x++)
		{
			if (stats[x] > tempVal)
			{
				returnVal = x;
				tempVal = stats[x];
			}
		}
		
		return returnVal;
		// temp fix ends
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		switch (attackAttribute)
		{
			case FIRE:
			{
				return (int) calcStat(Stat.FIRE_POWER, _creature.getTemplate().getBaseFire());
			}
			case WATER:
			{
				return (int) calcStat(Stat.WATER_POWER, _creature.getTemplate().getBaseWater());
			}
			case WIND:
			{
				return (int) calcStat(Stat.WIND_POWER, _creature.getTemplate().getBaseWind());
			}
			case EARTH:
			{
				return (int) calcStat(Stat.EARTH_POWER, _creature.getTemplate().getBaseEarth());
			}
			case HOLY:
			{
				return (int) calcStat(Stat.HOLY_POWER, _creature.getTemplate().getBaseHoly());
			}
			case DARK:
			{
				return (int) calcStat(Stat.DARK_POWER, _creature.getTemplate().getBaseDark());
			}
			default:
			{
				return 0;
			}
		}
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		switch (defenseAttribute)
		{
			case FIRE:
			{
				return (int) calcStat(Stat.FIRE_RES, _creature.getTemplate().getBaseFireRes());
			}
			case WATER:
			{
				return (int) calcStat(Stat.WATER_RES, _creature.getTemplate().getBaseWaterRes());
			}
			case WIND:
			{
				return (int) calcStat(Stat.WIND_RES, _creature.getTemplate().getBaseWindRes());
			}
			case EARTH:
			{
				return (int) calcStat(Stat.EARTH_RES, _creature.getTemplate().getBaseEarthRes());
			}
			case HOLY:
			{
				return (int) calcStat(Stat.HOLY_RES, _creature.getTemplate().getBaseHolyRes());
			}
			case DARK:
			{
				return (int) calcStat(Stat.DARK_RES, _creature.getTemplate().getBaseDarkRes());
			}
			default:
			{
				return (int) _creature.getTemplate().getBaseElementRes();
			}
		}
	}
	
	public float getAttackTrait(TraitType traitType)
	{
		return _attackTraits[traitType.ordinal()];
	}
	
	public float[] getAttackTraits()
	{
		return _attackTraits;
	}
	
	public boolean hasAttackTrait(TraitType traitType)
	{
		return _attackTraitsCount[traitType.ordinal()] > 0;
	}
	
	public int[] getAttackTraitsCount()
	{
		return _attackTraitsCount;
	}
	
	public float getDefenceTrait(TraitType traitType)
	{
		return _defenceTraits[traitType.ordinal()];
	}
	
	public float[] getDefenceTraits()
	{
		return _defenceTraits;
	}
	
	public boolean hasDefenceTrait(TraitType traitType)
	{
		return _defenceTraitsCount[traitType.ordinal()] > 0;
	}
	
	public int[] getDefenceTraitsCount()
	{
		return _defenceTraitsCount;
	}
	
	public boolean isTraitInvul(TraitType traitType)
	{
		return _traitsInvul[traitType.ordinal()] > 0;
	}
	
	public int[] getTraitsInvul()
	{
		return _traitsInvul;
	}
	
	/**
	 * Gets the maximum buff count.
	 * @return the maximum buff count
	 */
	public int getMaxBuffCount()
	{
		return _maxBuffCount + _creature.getSkillLevel(DIVINE_INSPIRATION);
	}
	
	/**
	 * Sets the maximum buff count.
	 * @param buffCount the buff count
	 */
	public void setMaxBuffCount(int buffCount)
	{
		_maxBuffCount = buffCount;
	}
}
