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
package org.l2jmobius.gameserver.model.skill.holders;

import org.l2jmobius.gameserver.model.actor.Player;

public class EnchantSkillLearn
{
	// These two build the primary key.
	private final int _id;
	private final int _level;
	
	// Not needed, just for easier debugging.
	private final String _name;
	private final int _spCost;
	private final int _baseLevel;
	private final int _minSkillLevel;
	private final int _exp;
	private final byte _rate76;
	private final byte _rate77;
	private final byte _rate78;
	private final byte _rate79;
	private final byte _rate80;
	
	public EnchantSkillLearn(int id, int level, int minSkillLevel, int baseLevel, String name, int spCost, int exp, byte rate76, byte rate77, byte rate78, byte rate79, byte rate80)
	{
		_id = id;
		_level = level;
		_baseLevel = baseLevel;
		_minSkillLevel = minSkillLevel;
		_name = name.intern();
		_spCost = spCost;
		_exp = exp;
		_rate76 = rate76;
		_rate77 = rate77;
		_rate78 = rate78;
		_rate79 = rate79;
		_rate80 = rate80;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getBaseLevel()
	{
		return _baseLevel;
	}
	
	public int getMinSkillLevel()
	{
		return _minSkillLevel;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getSpCost()
	{
		return _spCost;
	}
	
	public int getExp()
	{
		return _exp;
	}
	
	public byte getRate(Player player)
	{
		switch (player.getLevel())
		{
			case 76:
			{
				return _rate76;
			}
			case 77:
			{
				return _rate77;
			}
			case 78:
			{
				return _rate78;
			}
			case 79:
			{
				return _rate79;
			}
			default:
			{
				return _rate80;
			}
		}
	}
}
