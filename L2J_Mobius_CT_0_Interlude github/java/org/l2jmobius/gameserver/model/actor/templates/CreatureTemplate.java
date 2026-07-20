/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.actor.templates;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.MoveType;

/**
 * Character template.
 * @author Zoey76
 */
public class CreatureTemplate extends ListenersContainer
{
	// BaseStats
	private int _baseSTR;
	private int _baseCON;
	private int _baseDEX;
	private int _baseINT;
	private int _baseWIT;
	private int _baseMEN;
	protected float _baseHpMax;
	private float _baseCpMax;
	protected float _baseMpMax;
	private float _baseHpReg;
	private float _baseMpReg;
	protected int _basePAtk;
	protected int _baseMAtk;
	protected int _basePDef;
	protected int _baseMDef;
	private int _basePAtkSpd;
	private int _baseMAtkSpd;
	private int _baseAttackRange;
	private int _randomDamage;
	private WeaponType _baseAttackType;
	private int _baseShldDef;
	private int _baseShldRate;
	private int _baseCritRate;
	private int _baseMCritRate;
	
	// SpecialStats
	private int _baseBreath;
	private int _baseFire;
	private int _baseWind;
	private int _baseWater;
	private int _baseEarth;
	private int _baseHoly;
	private int _baseDark;
	private double _baseFireRes;
	private double _baseWindRes;
	private double _baseWaterRes;
	private double _baseEarthRes;
	private double _baseHolyRes;
	private double _baseDarkRes;
	private double _baseElementRes;
	/** For client info use {@link #_fCollisionRadius} */
	private int _collisionRadius;
	/** For client info use {@link #_fCollisionHeight} */
	private int _collisionHeight;
	
	private float _fCollisionRadius;
	private float _fCollisionHeight;
	
	private final double[] _moveType = new double[MoveType.values().length];
	/** The creature's race. */
	private Race _race;
	
	public CreatureTemplate(StatSet set)
	{
		set(set);
	}
	
	public void set(StatSet set)
	{
		// Base stats
		_baseSTR = set.getInt("baseSTR", 0);
		_baseCON = set.getInt("baseCON", 0);
		_baseDEX = set.getInt("baseDEX", 0);
		_baseINT = set.getInt("baseINT", 0);
		_baseWIT = set.getInt("baseWIT", 0);
		_baseMEN = set.getInt("baseMEN", 0);
		_baseHpMax = set.getFloat("baseHpMax", 0);
		_baseCpMax = set.getFloat("baseCpMax", 0);
		_baseMpMax = set.getFloat("baseMpMax", 0);
		_baseHpReg = set.getFloat("baseHpReg", 0);
		_baseMpReg = set.getFloat("baseMpReg", 0);
		_basePAtk = set.getInt("basePAtk", 0);
		_baseMAtk = set.getInt("baseMAtk", 0);
		_basePDef = set.getInt("basePDef", 0);
		_baseMDef = set.getInt("baseMDef", 0);
		_basePAtkSpd = set.getInt("basePAtkSpd", 300);
		_baseMAtkSpd = set.getInt("baseMAtkSpd", 333);
		_baseShldDef = set.getInt("baseShldDef", 0);
		_baseAttackRange = set.getInt("baseAtkRange", 40);
		_randomDamage = set.getInt("baseRndDam", 0);
		_baseAttackType = set.getEnum("baseAtkType", WeaponType.class, WeaponType.FIST);
		_baseShldRate = set.getInt("baseShldRate", 0);
		_baseCritRate = set.getInt("baseCritRate", 4);
		_baseMCritRate = set.getInt("baseMCritRate", 5);
		
		// SpecialStats
		_baseBreath = set.getInt("baseBreath", 100);
		_baseFire = set.getInt("baseFire", 0);
		_baseWind = set.getInt("baseWind", 0);
		_baseWater = set.getInt("baseWater", 0);
		_baseEarth = set.getInt("baseEarth", 0);
		_baseHoly = set.getInt("baseHoly", 0);
		_baseDark = set.getInt("baseDark", 0);
		_baseFireRes = set.getInt("baseFireRes", 0);
		_baseWindRes = set.getInt("baseWindRes", 0);
		_baseWaterRes = set.getInt("baseWaterRes", 0);
		_baseEarthRes = set.getInt("baseEarthRes", 0);
		_baseHolyRes = set.getInt("baseHolyRes", 0);
		_baseDarkRes = set.getInt("baseDarkRes", 0);
		_baseElementRes = set.getInt("baseElementRes", 0);
		
		// Geometry
		_fCollisionHeight = set.getFloat("collisionHeight", 0);
		_fCollisionRadius = set.getFloat("collisionRadius", 0);
		_collisionRadius = (int) _fCollisionRadius;
		_collisionHeight = (int) _fCollisionHeight;
		
		// speed.
		Arrays.fill(_moveType, 1);
		setBaseMoveSpeed(MoveType.RUN, set.getDouble("baseRunSpd", 120));
		setBaseMoveSpeed(MoveType.WALK, set.getDouble("baseWalkSpd", 50));
		setBaseMoveSpeed(MoveType.FAST_SWIM, set.getDouble("baseSwimRunSpd", getBaseMoveSpeed(MoveType.RUN)));
		setBaseMoveSpeed(MoveType.SLOW_SWIM, set.getDouble("baseSwimWalkSpd", getBaseMoveSpeed(MoveType.WALK)));
		setBaseMoveSpeed(MoveType.FAST_FLY, set.getDouble("baseFlyRunSpd", getBaseMoveSpeed(MoveType.RUN)));
		setBaseMoveSpeed(MoveType.SLOW_FLY, set.getDouble("baseFlyWalkSpd", getBaseMoveSpeed(MoveType.WALK)));
	}
	
	/**
	 * @return the baseHpMax
	 */
	public float getBaseHpMax()
	{
		return _baseHpMax;
	}
	
	/**
	 * @return the _baseFire
	 */
	public int getBaseFire()
	{
		return _baseFire;
	}
	
	/**
	 * @return the _baseWind
	 */
	public int getBaseWind()
	{
		return _baseWind;
	}
	
	/**
	 * @return the _baseWater
	 */
	public int getBaseWater()
	{
		return _baseWater;
	}
	
	/**
	 * @return the _baseEarth
	 */
	public int getBaseEarth()
	{
		return _baseEarth;
	}
	
	/**
	 * @return the _baseHoly
	 */
	public int getBaseHoly()
	{
		return _baseHoly;
	}
	
	/**
	 * @return the _baseDark
	 */
	public int getBaseDark()
	{
		return _baseDark;
	}
	
	/**
	 * @return the _baseFireRes
	 */
	public double getBaseFireRes()
	{
		return _baseFireRes;
	}
	
	/**
	 * @return the _baseWindRes
	 */
	public double getBaseWindRes()
	{
		return _baseWindRes;
	}
	
	/**
	 * @return the _baseWaterRes
	 */
	public double getBaseWaterRes()
	{
		return _baseWaterRes;
	}
	
	/**
	 * @return the _baseEarthRes
	 */
	public double getBaseEarthRes()
	{
		return _baseEarthRes;
	}
	
	/**
	 * @return the _baseHolyRes
	 */
	public double getBaseHolyRes()
	{
		return _baseHolyRes;
	}
	
	/**
	 * @return the _baseDarkRes
	 */
	public double getBaseDarkRes()
	{
		return _baseDarkRes;
	}
	
	/**
	 * @return the _baseElementRes
	 */
	public double getBaseElementRes()
	{
		return _baseElementRes;
	}
	
	/**
	 * @return the baseSTR
	 */
	public int getBaseSTR()
	{
		return _baseSTR;
	}
	
	/**
	 * @return the baseCON
	 */
	public int getBaseCON()
	{
		return _baseCON;
	}
	
	/**
	 * @return the baseDEX
	 */
	public int getBaseDEX()
	{
		return _baseDEX;
	}
	
	/**
	 * @return the baseINT
	 */
	public int getBaseINT()
	{
		return _baseINT;
	}
	
	/**
	 * @return the baseWIT
	 */
	public int getBaseWIT()
	{
		return _baseWIT;
	}
	
	/**
	 * @return the baseMEN
	 */
	public int getBaseMEN()
	{
		return _baseMEN;
	}
	
	/**
	 * @return the baseCpMax
	 */
	public float getBaseCpMax()
	{
		return _baseCpMax;
	}
	
	/**
	 * @return the baseMpMax
	 */
	public float getBaseMpMax()
	{
		return _baseMpMax;
	}
	
	/**
	 * @return the baseHpReg
	 */
	public float getBaseHpReg()
	{
		return _baseHpReg;
	}
	
	/**
	 * @return the baseMpReg
	 */
	public float getBaseMpReg()
	{
		return _baseMpReg;
	}
	
	/**
	 * @return the basePAtk
	 */
	public int getBasePAtk()
	{
		return _basePAtk;
	}
	
	/**
	 * @return the baseMAtk
	 */
	public int getBaseMAtk()
	{
		return _baseMAtk;
	}
	
	/**
	 * @return the basePDef
	 */
	public int getBasePDef()
	{
		return _basePDef;
	}
	
	/**
	 * @return the baseMDef
	 */
	public int getBaseMDef()
	{
		return _baseMDef;
	}
	
	/**
	 * @return the basePAtkSpd
	 */
	public int getBasePAtkSpd()
	{
		return _basePAtkSpd;
	}
	
	/**
	 * @return the baseMAtkSpd
	 */
	public int getBaseMAtkSpd()
	{
		return _baseMAtkSpd;
	}
	
	/**
	 * @return the random damage
	 */
	public int getRandomDamage()
	{
		return _randomDamage;
	}
	
	/**
	 * @return the baseShldDef
	 */
	public int getBaseShldDef()
	{
		return _baseShldDef;
	}
	
	/**
	 * @return the baseShldRate
	 */
	public int getBaseShldRate()
	{
		return _baseShldRate;
	}
	
	/**
	 * @return the baseCritRate
	 */
	public int getBaseCritRate()
	{
		return _baseCritRate;
	}
	
	/**
	 * @return the baseMCritRate
	 */
	public int getBaseMCritRate()
	{
		return _baseMCritRate;
	}
	
	public void setBaseMoveSpeed(MoveType type, double value)
	{
		_moveType[type.ordinal()] = value;
	}
	
	public double getBaseMoveSpeed(MoveType mt)
	{
		return _moveType[mt.ordinal()];
	}
	
	/**
	 * @return the baseBreath
	 */
	public int getBaseBreath()
	{
		return _baseBreath;
	}
	
	/**
	 * @return the collisionRadius
	 */
	public int getCollisionRadius()
	{
		return _collisionRadius;
	}
	
	/**
	 * @return the collisionHeight
	 */
	public int getCollisionHeight()
	{
		return _collisionHeight;
	}
	
	/**
	 * @return the fCollisionRadius
	 */
	public float getFCollisionRadius()
	{
		return _fCollisionRadius;
	}
	
	/**
	 * @return the fCollisionHeight
	 */
	public float getFCollisionHeight()
	{
		return _fCollisionHeight;
	}
	
	/**
	 * @param baseFire the baseFire to set
	 */
	public void setBaseFire(int baseFire)
	{
		_baseFire = baseFire;
	}
	
	/**
	 * @param baseWater the baseWater to set
	 */
	public void setBaseWater(int baseWater)
	{
		_baseWater = baseWater;
	}
	
	/**
	 * @param baseEarth the baseEarth to set
	 */
	public void setBaseEarth(int baseEarth)
	{
		_baseEarth = baseEarth;
	}
	
	/**
	 * @param baseWind the baseWind to set
	 */
	public void setBaseWind(int baseWind)
	{
		_baseWind = baseWind;
	}
	
	/**
	 * @param baseHoly the baseHoly to set
	 */
	public void setBaseHoly(int baseHoly)
	{
		_baseHoly = baseHoly;
	}
	
	/**
	 * @param baseDark the baseDark to set
	 */
	public void setBaseDark(int baseDark)
	{
		_baseDark = baseDark;
	}
	
	/**
	 * @param baseFireRes the baseFireRes to set
	 */
	public void setBaseFireRes(double baseFireRes)
	{
		_baseFireRes = baseFireRes;
	}
	
	/**
	 * @param baseWaterRes the baseWaterRes to set
	 */
	public void setBaseWaterRes(double baseWaterRes)
	{
		_baseWaterRes = baseWaterRes;
	}
	
	/**
	 * @param baseEarthRes the baseEarthRes to set
	 */
	public void setBaseEarthRes(double baseEarthRes)
	{
		_baseEarthRes = baseEarthRes;
	}
	
	/**
	 * @param baseWindRes the baseWindRes to set
	 */
	public void setBaseWindRes(double baseWindRes)
	{
		_baseWindRes = baseWindRes;
	}
	
	/**
	 * @param baseHolyRes the baseHolyRes to set
	 */
	public void setBaseHolyRes(double baseHolyRes)
	{
		_baseHolyRes = baseHolyRes;
	}
	
	/**
	 * @param baseDarkRes the baseDarkRes to set
	 */
	public void setBaseDarkRes(double baseDarkRes)
	{
		_baseDarkRes = baseDarkRes;
	}
	
	/**
	 * @param baseElementRes
	 */
	public void setBaseElementRes(double baseElementRes)
	{
		_baseElementRes = baseElementRes;
	}
	
	/**
	 * @return the base attack type (Sword, Fist, Blunt, etc..)
	 */
	public WeaponType getBaseAttackType()
	{
		return _baseAttackType;
	}
	
	/**
	 * Sets base attack type.
	 * @param type
	 */
	public void setBaseAttackType(WeaponType type)
	{
		_baseAttackType = type;
	}
	
	/**
	 * @return the baseAtkRange
	 */
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	/**
	 * Sets base attack range.
	 * @param value
	 */
	public void setBaseAttackRange(int value)
	{
		_baseAttackRange = value;
	}
	
	/**
	 * Overridden in NpcTemplate
	 * @return the characters skills
	 */
	public Map<Integer, Skill> getSkills()
	{
		return Collections.emptyMap();
	}
	
	/**
	 * Gets the craeture's race.
	 * @return the race
	 */
	public Race getRace()
	{
		return _race;
	}
	
	/**
	 * Sets the creature's race.
	 * @param race the race
	 */
	public void setRace(Race race)
	{
		_race = race;
	}
}
