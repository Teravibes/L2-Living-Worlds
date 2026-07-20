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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;

/**
 * @author mkizub, Zoey76
 */
public class PlayerTemplate extends CreatureTemplate
{
	private final PlayerClass _playerClass;
	
	private final float[] _baseHp;
	private final float[] _baseMp;
	private final float[] _baseCp;
	
	private final double[] _baseHpReg;
	private final double[] _baseMpReg;
	private final double[] _baseCpReg;
	
	private final float _fCollisionHeightFemale;
	private final float _fCollisionRadiusFemale;
	
	private final int _baseSafeFallHeight;
	
	private final List<Location> _creationPoints;
	private final Map<Integer, Integer> _baseSlotDef;
	
	public PlayerTemplate(StatSet set, List<Location> creationPoints)
	{
		super(set);
		_playerClass = PlayerClass.getPlayerClass(set.getInt("classId"));
		setRace(_playerClass.getRace());
		_baseHp = new float[ExperienceData.getInstance().getMaxLevel()];
		_baseMp = new float[ExperienceData.getInstance().getMaxLevel()];
		_baseCp = new float[ExperienceData.getInstance().getMaxLevel()];
		_baseHpReg = new double[ExperienceData.getInstance().getMaxLevel()];
		_baseMpReg = new double[ExperienceData.getInstance().getMaxLevel()];
		_baseCpReg = new double[ExperienceData.getInstance().getMaxLevel()];
		_baseSlotDef = new HashMap<>(12);
		_baseSlotDef.put(Inventory.PAPERDOLL_CHEST, set.getInt("basePDefchest", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LEGS, set.getInt("basePDeflegs", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_HEAD, set.getInt("basePDefhead", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_FEET, set.getInt("basePDeffeet", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_GLOVES, set.getInt("basePDefgloves", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_UNDER, set.getInt("basePDefunderwear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_CLOAK, set.getInt("basePDefcloak", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_REAR, set.getInt("baseMDefrear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LEAR, set.getInt("baseMDeflear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_RFINGER, set.getInt("baseMDefrfinger", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LFINGER, set.getInt("baseMDefrfinger", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_NECK, set.getInt("baseMDefneck", 0));
		
		_fCollisionRadiusFemale = set.getFloat("collisionFemaleradius");
		_fCollisionHeightFemale = set.getFloat("collisionFemaleheight");
		_baseSafeFallHeight = set.getInt("baseSafeFall", 333);
		_creationPoints = creationPoints;
	}
	
	/**
	 * @return the template PlayerClass.
	 */
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	/**
	 * @return random Location of created character spawn.
	 */
	public Location getCreationPoint()
	{
		return _creationPoints.get(Rnd.get(_creationPoints.size()));
	}
	
	/**
	 * Sets the value of level upgain parameter.
	 * @param paramName name of parameter
	 * @param level corresponding character level
	 * @param value value of parameter
	 */
	public void setUpgainValue(String paramName, int level, double value)
	{
		switch (paramName)
		{
			case "hp":
			{
				_baseHp[level] = (float) value;
				break;
			}
			case "mp":
			{
				_baseMp[level] = (float) value;
				break;
			}
			case "cp":
			{
				_baseCp[level] = (float) value;
				break;
			}
			case "hpRegen":
			{
				_baseHpReg[level] = value;
				break;
			}
			case "mpRegen":
			{
				_baseMpReg[level] = value;
				break;
			}
			case "cpRegen":
			{
				_baseCpReg[level] = value;
				break;
			}
		}
	}
	
	/**
	 * @param level character level to return value
	 * @return the baseHpMax for given character level
	 */
	public float getBaseHpMax(int level)
	{
		return _baseHp[level];
	}
	
	/**
	 * @param level character level to return value
	 * @return the baseMpMax for given character level
	 */
	public float getBaseMpMax(int level)
	{
		return _baseMp[level];
	}
	
	/**
	 * @param level character level to return value
	 * @return the baseCpMax for given character level
	 */
	public float getBaseCpMax(int level)
	{
		return _baseCp[level];
	}
	
	/**
	 * @param level character level to return value
	 * @return the base HP Regeneration for given character level
	 */
	public double getBaseHpRegen(int level)
	{
		return _baseHpReg[level];
	}
	
	/**
	 * @param level character level to return value
	 * @return the base MP Regeneration for given character level
	 */
	public double getBaseMpRegen(int level)
	{
		return _baseMpReg[level];
	}
	
	/**
	 * @param level character level to return value
	 * @return the base HP Regeneration for given character level
	 */
	public double getBaseCpRegen(int level)
	{
		return _baseCpReg[level];
	}
	
	/**
	 * @param slotId id of inventory slot to return value
	 * @return defense value of character for EMPTY given slot
	 */
	public int getBaseDefBySlot(int slotId)
	{
		return _baseSlotDef.containsKey(slotId) ? _baseSlotDef.get(slotId) : 0;
	}
	
	/**
	 * @return the template collision height for female characters.
	 */
	public float getFCollisionHeightFemale()
	{
		return _fCollisionHeightFemale;
	}
	
	/**
	 * @return the template collision radius for female characters.
	 */
	public float getFCollisionRadiusFemale()
	{
		return _fCollisionRadiusFemale;
	}
	
	/**
	 * @return the safe fall height.
	 */
	public int getSafeFallHeight()
	{
		return _baseSafeFallHeight;
	}
}
