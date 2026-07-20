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
 * Class for the Fishing Monsters object.
 * @author nonom
 */
public class FishingMonster
{
	private final int _userMinLevel;
	private final int _userMaxLevel;
	private final int _fishingMonsterId;
	private final int _probability;
	
	public FishingMonster(StatSet set)
	{
		_userMinLevel = set.getInt("userMinLevel");
		_userMaxLevel = set.getInt("userMaxLevel");
		_fishingMonsterId = set.getInt("fishingMonsterId");
		_probability = set.getInt("probability");
	}
	
	/**
	 * @return the minimum user level.
	 */
	public int getUserMinLevel()
	{
		return _userMinLevel;
	}
	
	/**
	 * @return the maximum user level.
	 */
	public int getUserMaxLevel()
	{
		return _userMaxLevel;
	}
	
	/**
	 * @return the fishing monster Id.
	 */
	public int getFishingMonsterId()
	{
		return _fishingMonsterId;
	}
	
	/**
	 * @return the probability.
	 */
	public int getProbability()
	{
		return _probability;
	}
}
