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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.EnchantSkillLearn;

/**
 * @author Mobius
 */
public class EnchantSkillTreeData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantSkillTreeData.class.getName());
	
	private final List<EnchantSkillLearn> _enchantSkillTrees = new ArrayList<>();
	
	protected EnchantSkillTreeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_enchantSkillTrees.clear();
		parseDatapackFile("data/EnchantSkillTreeData.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		int prevSkillId = -1;
		
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skill".equals(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int id = parseInteger(attrs, "id");
						final int level = parseInteger(attrs, "level");
						final String name = parseString(attrs, "name");
						final int baseLevel = parseInteger(attrs, "baseLevel");
						final int minskillLevel = parseInteger(attrs, "minSkillLevel");
						final int sp = parseInteger(attrs, "sp");
						final int exp = parseInteger(attrs, "exp");
						final byte rate76 = parseByte(attrs, "rate76");
						final byte rate77 = parseByte(attrs, "rate77");
						final byte rate78 = parseByte(attrs, "rate78");
						final byte rate79 = parseByte(attrs, "rate79");
						final byte rate80 = parseByte(attrs, "rate80");
						if (prevSkillId != id)
						{
							prevSkillId = id;
						}
						
						_enchantSkillTrees.add(new EnchantSkillLearn(id, level, minskillLevel, baseLevel, name, sp, exp, rate76, rate77, rate78, rate79, rate80));
					}
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _enchantSkillTrees.size() + " enchant skills.");
	}
	
	public List<EnchantSkillLearn> getAvailableEnchantSkills(Player player)
	{
		if (player.getLevel() < 76)
		{
			return Collections.emptyList();
		}
		
		final List<EnchantSkillLearn> result = new ArrayList<>();
		final List<EnchantSkillLearn> skills = new ArrayList<>();
		skills.addAll(_enchantSkillTrees);
		
		final Collection<Skill> knownSkills = player.getAllSkills();
		for (EnchantSkillLearn skillLearn : skills)
		{
			SEARCH: for (Skill skill : knownSkills)
			{
				if (skill.getId() == skillLearn.getId())
				{
					if (skill.getLevel() == skillLearn.getMinSkillLevel())
					{
						// This is the next level of a skill that we know.
						result.add(skillLearn);
					}
					break SEARCH;
				}
			}
		}
		
		return result;
	}
	
	public int getSkillSpCost(Player player, Skill skill)
	{
		int skillCost = 100000000;
		for (EnchantSkillLearn enchantSkillLearn : getAvailableEnchantSkills(player))
		{
			if (enchantSkillLearn.getId() != skill.getId())
			{
				continue;
			}
			
			if (enchantSkillLearn.getLevel() != skill.getLevel())
			{
				continue;
			}
			
			if (76 > player.getLevel())
			{
				continue;
			}
			
			skillCost = enchantSkillLearn.getSpCost();
		}
		
		return skillCost;
	}
	
	public int getSkillExpCost(Player player, Skill skill)
	{
		int skillCost = 100000000;
		for (EnchantSkillLearn enchantSkillLearn : getAvailableEnchantSkills(player))
		{
			if (enchantSkillLearn.getId() != skill.getId())
			{
				continue;
			}
			
			if (enchantSkillLearn.getLevel() != skill.getLevel())
			{
				continue;
			}
			
			if (76 > player.getLevel())
			{
				continue;
			}
			
			skillCost = enchantSkillLearn.getExp();
		}
		
		return skillCost;
	}
	
	public byte getSkillRate(Player player, Skill skill)
	{
		for (EnchantSkillLearn enchantSkillLearn : getAvailableEnchantSkills(player))
		{
			if (enchantSkillLearn.getId() != skill.getId())
			{
				continue;
			}
			
			if (enchantSkillLearn.getLevel() != skill.getLevel())
			{
				continue;
			}
			
			return enchantSkillLearn.getRate(player);
		}
		
		return 0;
	}
	
	public static EnchantSkillTreeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantSkillTreeData INSTANCE = new EnchantSkillTreeData();
	}
}
