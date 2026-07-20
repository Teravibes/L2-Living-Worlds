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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.enums.StatType;

/**
 * This class describes a RecipeList statUse and altStatChange component.
 */
public class RecipeStatHolder
{
	/** The Identifier of the statType */
	private final StatType _type;
	
	/** The value of the statType */
	private final int _value;
	
	/**
	 * Constructor of RecipeStatHolder.
	 * @param type
	 * @param value
	 */
	public RecipeStatHolder(String type, int value)
	{
		_type = Enum.valueOf(StatType.class, type);
		_value = value;
	}
	
	/**
	 * @return the the type of the RecipeStatHolder.
	 */
	public StatType getType()
	{
		return _type;
	}
	
	/**
	 * @return the value of the RecipeStatHolder.
	 */
	public int getValue()
	{
		return _value;
	}
}
