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
package org.l2jmobius.gameserver.model.fishing;

import org.l2jmobius.gameserver.model.StatSet;

/**
 * Class for the Fish object.
 * @author nonom
 */
public class Fish implements Cloneable
{
	private final int _fishId;
	private final int _itemId;
	private final String _itemName;
	private int _fishGroup;
	private final int _fishLevel;
	private final double _fishBiteRate;
	private final double _fishGuts;
	private final int _fishHp;
	private final int _fishMaxLength;
	private final double _fishLengthRate;
	private final double _hpRegen;
	private final int _startCombatTime;
	private final int _combatDuration;
	private final int _gutsCheckTime;
	private final double _gutsCheckProbability;
	private final double _cheatingProb;
	private final int _fishGrade;
	
	public Fish(StatSet set)
	{
		_fishId = set.getInt("fishId");
		_itemId = set.getInt("itemId");
		_itemName = set.getString("itemName");
		_fishGroup = getGroupId(set.getString("fishGroup"));
		_fishLevel = set.getInt("fishLevel");
		_fishBiteRate = set.getDouble("fishBiteRate"); // TODO: Support needed.
		_fishGuts = set.getDouble("fishGuts");
		_fishHp = set.getInt("fishHp");
		_fishMaxLength = set.getInt("fishMaxLength"); // TODO: Support needed.
		_fishLengthRate = set.getDouble("fishLengthRate"); // TODO: Support needed.
		_hpRegen = set.getDouble("hpRegen");
		_startCombatTime = set.getInt("startCombatTime");
		_combatDuration = set.getInt("combatDuration");
		_gutsCheckTime = set.getInt("gutsCheckTime");
		_gutsCheckProbability = set.getDouble("gutsCheckProbability"); // TODO: Support needed.
		_cheatingProb = set.getDouble("cheatingProb"); // TODO: Support needed.
		_fishGrade = getGradeId(set.getString("fishGrade"));
	}
	
	@Override
	public Fish clone()
	{
		try
		{
			return (Fish) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			return null;
		}
	}
	
	/**
	 * @return the fish Id.
	 */
	public int getFishId()
	{
		return _fishId;
	}
	
	/**
	 * @return the fish Item Id.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * @return the fish Item name Id.
	 */
	public String getItemName()
	{
		return _itemName;
	}
	
	/**
	 * @return the fish Group.
	 */
	public int getFishGroup()
	{
		return _fishGroup;
	}
	
	/**
	 * @return the fish Level.
	 */
	public int getFishLevel()
	{
		return _fishLevel;
	}
	
	/**
	 * @return the fish Bite Rate.
	 */
	public double getFishBiteRate()
	{
		return _fishBiteRate;
	}
	
	/**
	 * @return the fish Guts.
	 */
	public double getFishGuts()
	{
		return _fishGuts;
	}
	
	/**
	 * @return the fish Hp.
	 */
	public int getFishHp()
	{
		return _fishHp;
	}
	
	/**
	 * @return the fish Max length.
	 */
	public int getFishMaxLength()
	{
		return _fishMaxLength;
	}
	
	/**
	 * @return the fish Length rate.
	 */
	public double getFishLengthRate()
	{
		return _fishLengthRate;
	}
	
	/**
	 * @return the fish Hp regen.
	 */
	public double getHpRegen()
	{
		return _hpRegen;
	}
	
	/**
	 * @return the fish start Combat time.
	 */
	public int getStartCombatTime()
	{
		return _startCombatTime;
	}
	
	/**
	 * @return the fish Combat duration.
	 */
	public int getCombatDuration()
	{
		return _combatDuration;
	}
	
	/**
	 * @return the fish Guts check time.
	 */
	public int getGutsCheckTime()
	{
		return _gutsCheckTime;
	}
	
	/**
	 * @return the fish Guts Check probability.
	 */
	public double getGutsCheckProbability()
	{
		return _gutsCheckProbability;
	}
	
	/**
	 * @return the fish Cheating prob.
	 */
	public double getCheatingProb()
	{
		return _cheatingProb;
	}
	
	/**
	 * @return the fish Grade.
	 */
	public int getFishGrade()
	{
		return _fishGrade;
	}
	
	/**
	 * @param fg the fish Group.
	 */
	public void setFishGroup(int fg)
	{
		_fishGroup = fg;
	}
	
	/**
	 * @param name the Group Name.
	 * @return the fish Group Id.
	 */
	private int getGroupId(String name)
	{
		switch (name)
		{
			case "swift":
			{
				return 1;
			}
			case "ugly":
			{
				return 2;
			}
			case "fish_box":
			{
				return 3;
			}
			case "easy_wide":
			{
				return 4;
			}
			case "easy_swift":
			{
				return 5;
			}
			case "easy_ugly":
			{
				return 6;
			}
			case "hard_wide":
			{
				return 7;
			}
			case "hard_swift":
			{
				return 8;
			}
			case "hard_ugly":
			{
				return 9;
			}
			case "hs_fish":
			{
				return 10; // FIXME: Verify the ID
			}
			case "wide":
			default:
			{
				return 0;
			}
		}
	}
	
	/**
	 * @param name the Grade Name.
	 * @return the fish Grade Id.
	 */
	private int getGradeId(String name)
	{
		switch (name)
		{
			case "fish_easy":
			{
				return 0;
			}
			case "fish_hard":
			{
				return 2;
			}
			case "fish_normal":
			default:
			{
				return 1;
			}
		}
	}
}
