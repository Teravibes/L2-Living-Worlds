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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Mobius
 */
public class PetSkillData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PetSkillData.class.getName());
	private final Map<Integer, Map<Long, SkillHolder>> _skillTrees = new HashMap<>();
	
	protected PetSkillData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_skillTrees.clear();
		parseDatapackFile("data/PetSkillData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skillTrees.size() + " skills.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int npcId = parseInteger(attrs, "npcId");
						final int skillId = parseInteger(attrs, "skillId");
						final int skillLevel = parseInteger(attrs, "skillLevel");
						Map<Long, SkillHolder> skillTree = _skillTrees.get(npcId);
						if (skillTree == null)
						{
							skillTree = new HashMap<>();
							_skillTrees.put(npcId, skillTree);
						}
						
						if (SkillData.getInstance().getSkill(skillId, skillLevel == 0 ? 1 : skillLevel) != null)
						{
							skillTree.put((long) SkillData.getSkillHashCode(skillId, skillLevel + 1), new SkillHolder(skillId, skillLevel));
						}
						else
						{
							LOGGER.info(getClass().getSimpleName() + ": Could not find skill with id " + skillId + ", level " + skillLevel + " for NPC " + npcId + ".");
						}
					}
				}
			}
		}
	}
	
	/**
	 * Determines the highest available skill level for a specified skill ID based on the pet's level.
	 * @param pet the {@link Summon} pet whose available skill level is being checked.
	 * @param skillId the ID of the skill to retrieve the available level for.
	 * @return the highest available level for the specified skill ID, or 0 if the pet has no skills assigned.
	 */
	public int getAvailableLevel(Summon pet, int skillId)
	{
		int level = 0;
		if (!_skillTrees.containsKey(pet.getId()))
		{
			// LOGGER.warning(getClass().getSimpleName() + ": Pet id " + pet.getId() + " does not have any skills assigned.");
			return level;
		}
		
		for (SkillHolder skillHolder : _skillTrees.get(pet.getId()).values())
		{
			if (skillHolder.getSkillId() != skillId)
			{
				continue;
			}
			
			if (skillHolder.getSkillLevel() == 0)
			{
				if (pet.getLevel() < 70)
				{
					level = pet.getLevel() / 10;
					if (level <= 0)
					{
						level = 1;
					}
				}
				else
				{
					level = 7 + ((pet.getLevel() - 70) / 5);
				}
				
				// formula usable for skill that have 10 or more skill levels
				final int maxLevel = SkillData.getInstance().getMaxLevel(skillHolder.getSkillId());
				if (level > maxLevel)
				{
					level = maxLevel;
				}
				break;
			}
			else if ((1 <= pet.getLevel()) && (skillHolder.getSkillLevel() > level))
			{
				level = skillHolder.getSkillLevel();
			}
		}
		
		return level;
	}
	
	/**
	 * Retrieves a list of available skill IDs for a specified pet.
	 * @param pet the {@link Summon} pet whose available skill IDs are being retrieved.
	 * @return a {@link List} of skill IDs available for the given pet, or an empty list if no skills are assigned.
	 */
	public List<Integer> getAvailableSkills(Summon pet)
	{
		final List<Integer> skillIds = new ArrayList<>();
		if (!_skillTrees.containsKey(pet.getId()))
		{
			// LOGGER.warning(getClass().getSimpleName() + ": Pet id " + pet.getId() + " does not have any skills assigned.");
			return skillIds;
		}
		
		for (SkillHolder skillHolder : _skillTrees.get(pet.getId()).values())
		{
			if (skillIds.contains(skillHolder.getSkillId()))
			{
				continue;
			}
			
			skillIds.add(skillHolder.getSkillId());
		}
		
		return skillIds;
	}
	
	/**
	 * Retrieves a list of known skills for a specified pet.
	 * @param pet the {@link Summon} pet whose known skills are being retrieved.
	 * @return a {@link List} of {@link Skill} objects known by the given pet, or an empty list if the pet has no skills.
	 */
	public List<Skill> getKnownSkills(Summon pet)
	{
		final List<Skill> skills = new ArrayList<>();
		if (!_skillTrees.containsKey(pet.getId()))
		{
			return skills;
		}
		
		for (SkillHolder skillHolder : _skillTrees.get(pet.getId()).values())
		{
			final Skill skill = skillHolder.getSkill();
			if (skills.contains(skill))
			{
				continue;
			}
			
			skills.add(skill);
		}
		
		return skills;
	}
	
	/**
	 * Retrieves a specific known skill for a pet by skill ID.
	 * @param pet the {@link Summon} pet whose known skill is being retrieved.
	 * @param skillId the ID of the skill to retrieve.
	 * @return the {@link Skill} object corresponding to the specified skill ID, or {@code null} if the skill is not known by the pet.
	 */
	public Skill getKnownSkill(Summon pet, int skillId)
	{
		if (!_skillTrees.containsKey(pet.getId()))
		{
			return null;
		}
		
		for (SkillHolder skillHolder : _skillTrees.get(pet.getId()).values())
		{
			if (skillHolder.getSkillId() == skillId)
			{
				return skillHolder.getSkill();
			}
		}
		
		return null;
	}
	
	public static PetSkillData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetSkillData INSTANCE = new PetSkillData();
	}
}
