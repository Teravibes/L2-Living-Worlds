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
 * Class for the Fishing Rod object.
 * @author nonom
 */
public class FishingRod
{
	private final int _fishingRodId;
	private final int _fishingRodItemId;
	private final int _fishingRodLevel;
	private final String _fishingRodName;
	private final double _fishingRodDamage;
	
	public FishingRod(StatSet set)
	{
		_fishingRodId = set.getInt("fishingRodId");
		_fishingRodItemId = set.getInt("fishingRodItemId");
		_fishingRodLevel = set.getInt("fishingRodLevel");
		_fishingRodName = set.getString("fishingRodName");
		_fishingRodDamage = set.getDouble("fishingRodDamage");
	}
	
	/**
	 * @return the fishing rod Id.
	 */
	public int getFishingRodId()
	{
		return _fishingRodId;
	}
	
	/**
	 * @return the fishing rod Item Id.
	 */
	public int getFishingRodItemId()
	{
		return _fishingRodItemId;
	}
	
	/**
	 * @return the fishing rod Level.
	 */
	public int getFishingRodLevel()
	{
		return _fishingRodLevel;
	}
	
	/**
	 * @return the fishing rod Item Name.
	 */
	public String getFishingRodItemName()
	{
		return _fishingRodName;
	}
	
	/**
	 * @return the fishing rod Damage.
	 */
	public double getFishingRodDamage()
	{
		return _fishingRodDamage;
	}
}
