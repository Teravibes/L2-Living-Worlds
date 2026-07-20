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
package org.l2jmobius.gameserver.data;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author BiTi
 */
public class HeroSkillTable
{
	private static final Skill[] _heroSkills = new Skill[5];
	private static final int[] _heroSkillsId =
	{
		395,
		396,
		1374,
		1375,
		1376
	};
	
	HeroSkillTable()
	{
		for (int i = 0; i < _heroSkillsId.length; i++)
		{
			_heroSkills[i] = SkillData.getInstance().getSkill(_heroSkillsId[i], 1);
		}
	}
	
	public static HeroSkillTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static Skill[] getHeroSkills()
	{
		return _heroSkills;
	}
	
	public static boolean isHeroSkill(int skillId)
	{
		/*
		 * Do not perform checks directly on L2Skill array, it will cause errors due to SkillTable not initialized
		 */
		for (int id : _heroSkillsId)
		{
			if (id == skillId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static class SingletonHolder
	{
		protected static final HeroSkillTable _instance = new HeroSkillTable();
	}
}
