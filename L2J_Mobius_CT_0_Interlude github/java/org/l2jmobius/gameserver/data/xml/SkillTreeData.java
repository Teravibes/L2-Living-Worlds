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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.enums.player.SocialClass;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.holders.SkillLearn;

/**
 * This class loads and manage the characters and pledges skills trees.<br>
 * Here can be found the following skill trees:<br>
 * <ul>
 * <li>Class skill trees: player skill trees for each class.</li>
 * <li>Transfer skill trees: player skill trees for each healer class.</li>
 * <li>Collect skill tree: player skill tree for Gracia related skills.</li>
 * <li>Fishing skill tree: player skill tree for fishing related skills.</li>
 * <li>Transform skill tree: player skill tree for transformation related skills.</li>
 * <li>Sub-Class skill tree: player skill tree for sub-class related skills.</li>
 * <li>Noble skill tree: player skill tree for noblesse related skills.</li>
 * <li>Hero skill tree: player skill tree for heroes related skills.</li>
 * <li>GM skill tree: player skill tree for Game Master related skills.</li>
 * <li>Common skill tree: custom skill tree for players, skills in this skill tree will be available for all players.</li>
 * <li>Pledge skill tree: clan skill tree for main clan.</li>
 * <li>Sub-Pledge skill tree: clan skill tree for sub-clans.</li>
 * </ul>
 * For easy customization of player class skill trees, the parent Id of each class is taken from the XML data, this means you can use a different class parent Id than in the normal game play, for example all 3rd class dagger users will have Treasure Hunter skills as 1st and 2nd class skills.<br>
 * For XML schema please refer to skillTrees.xsd in datapack in xsd folder and for parameters documentation refer to documentation.txt in skillTrees folder.<br>
 * @author Zoey76, Mobius
 */
public class SkillTreeData implements IXmlReader
{
	// ClassId, Map of Skill Hash Code, SkillLearn
	private final Map<PlayerClass, Map<Integer, SkillLearn>> _classSkillTrees = new ConcurrentHashMap<>();
	private final Map<PlayerClass, Map<Integer, SkillLearn>> _completeClassSkillTree = new HashMap<>();
	
	// Skill Hash Code, SkillLearn
	private final Map<Integer, SkillLearn> _fishingSkillTree = new ConcurrentHashMap<>();
	private final Map<Integer, SkillLearn> _pledgeSkillTree = new ConcurrentHashMap<>();
	private final Map<Integer, SkillLearn> _commonSkillTree = new ConcurrentHashMap<>();
	
	// Other skill trees
	private final Map<Integer, SkillLearn> _nobleSkillTree = new ConcurrentHashMap<>();
	private final Map<Integer, SkillLearn> _heroSkillTree = new ConcurrentHashMap<>();
	private final Map<Integer, SkillLearn> _gameMasterSkillTree = new ConcurrentHashMap<>();
	private final Map<Integer, SkillLearn> _gameMasterAuraSkillTree = new ConcurrentHashMap<>();
	
	// Checker, sorted arrays of hash codes
	private Map<Integer, int[]> _skillsByClassIdHashCodes; // Occupation skills
	private Map<Integer, int[]> _skillsByRaceHashCodes; // Race-specific Transformations
	private int[] _allSkillsHashCodes; // Fishing, Collection, Transformations, Common Skills.
	
	/** Parent class Ids are read from XML and stored in this map, to allow easy customization. */
	private final Map<PlayerClass, PlayerClass> _parentClassMap = new LinkedHashMap<>();
	
	private boolean _loading = true;
	
	/**
	 * Instantiates a new skill trees data.
	 */
	protected SkillTreeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_loading = true;
		_parentClassMap.clear();
		_classSkillTrees.clear();
		_fishingSkillTree.clear();
		_pledgeSkillTree.clear();
		_nobleSkillTree.clear();
		_heroSkillTree.clear();
		_gameMasterSkillTree.clear();
		_gameMasterAuraSkillTree.clear();
		
		// Load files.
		parseDatapackDirectory("data/stats/players/skillTrees/", true);
		
		// Cache the complete class skill trees.
		_completeClassSkillTree.clear();
		for (Entry<PlayerClass, Map<Integer, SkillLearn>> entry : _classSkillTrees.entrySet())
		{
			final Map<Integer, SkillLearn> skillTree = new LinkedHashMap<>();
			
			// Add all skills that belong to all classes.
			skillTree.putAll(_commonSkillTree);
			
			final PlayerClass entryPlayerClass = entry.getKey();
			PlayerClass currentPlayerClass = entryPlayerClass;
			
			final LinkedList<PlayerClass> classSequence = new LinkedList<>();
			while (currentPlayerClass != null)
			{
				classSequence.addFirst(currentPlayerClass);
				currentPlayerClass = _parentClassMap.get(currentPlayerClass);
			}
			
			for (PlayerClass cid : classSequence)
			{
				final Map<Integer, SkillLearn> classSkillTree = _classSkillTrees.get(cid);
				if (classSkillTree != null)
				{
					skillTree.putAll(classSkillTree);
				}
			}
			
			_completeClassSkillTree.put(entryPlayerClass, skillTree);
		}
		
		// Generate check arrays.
		generateCheckArrays();
		
		// Logs a report with skill trees info.
		report();
		
		_loading = false;
	}
	
	/**
	 * Parse a skill tree file and store it into the correct skill tree.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		int cId = -1;
		PlayerClass playerClass = null;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skillTree".equalsIgnoreCase(d.getNodeName()))
					{
						final Map<Integer, SkillLearn> classSkillTree = new HashMap<>();
						final Map<Integer, SkillLearn> trasferSkillTree = new HashMap<>();
						final String type = d.getAttributes().getNamedItem("type").getNodeValue();
						Node attr = d.getAttributes().getNamedItem("classId");
						if (attr != null)
						{
							cId = Integer.parseInt(attr.getNodeValue());
							playerClass = PlayerClass.getPlayerClass(cId);
						}
						else
						{
							cId = -1;
						}
						
						attr = d.getAttributes().getNamedItem("parentClassId");
						if (attr != null)
						{
							final int parentClassId = Integer.parseInt(attr.getNodeValue());
							if ((cId > -1) && (cId != parentClassId) && (parentClassId > -1) && !_parentClassMap.containsKey(playerClass))
							{
								_parentClassMap.put(playerClass, PlayerClass.getPlayerClass(parentClassId));
							}
						}
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("skill".equalsIgnoreCase(c.getNodeName()))
							{
								final StatSet learnSkillSet = new StatSet();
								NamedNodeMap attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									attr = attrs.item(i);
									learnSkillSet.set(attr.getNodeName(), attr.getNodeValue());
								}
								
								final SkillLearn skillLearn = new SkillLearn(learnSkillSet);
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									attrs = b.getAttributes();
									switch (b.getNodeName())
									{
										case "item":
										{
											skillLearn.addRequiredItem(new ItemHolder(parseInteger(attrs, "id"), parseInteger(attrs, "count")));
											break;
										}
										case "preRequisiteSkill":
										{
											skillLearn.addPreReqSkill(new SkillHolder(parseInteger(attrs, "id"), parseInteger(attrs, "lvl")));
											break;
										}
										case "race":
										{
											skillLearn.addRace(Race.valueOf(b.getTextContent()));
											break;
										}
										case "residenceId":
										{
											skillLearn.addResidenceId(Integer.parseInt(b.getTextContent()));
											break;
										}
										case "socialClass":
										{
											skillLearn.setSocialClass(Enum.valueOf(SocialClass.class, b.getTextContent()));
											break;
										}
										case "subClassConditions":
										{
											skillLearn.addSubclassConditions(parseInteger(attrs, "slot"), parseInteger(attrs, "lvl"));
											break;
										}
									}
								}
								
								final int skillHashCode = SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel());
								switch (type)
								{
									case "classSkillTree":
									{
										if (cId != -1)
										{
											classSkillTree.put(skillHashCode, skillLearn);
										}
										else
										{
											_commonSkillTree.put(skillHashCode, skillLearn);
										}
										break;
									}
									case "transferSkillTree":
									{
										trasferSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "fishingSkillTree":
									{
										_fishingSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "pledgeSkillTree":
									{
										_pledgeSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "nobleSkillTree":
									{
										_nobleSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "heroSkillTree":
									{
										_heroSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterSkillTree":
									{
										_gameMasterSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterAuraSkillTree":
									{
										_gameMasterAuraSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									default:
									{
										LOGGER.warning(getClass().getSimpleName() + ": Unknown Skill Tree type: " + type + "!");
									}
								}
							}
						}
						
						if (type.equals("classSkillTree") && (cId > -1))
						{
							if (!_classSkillTrees.containsKey(playerClass))
							{
								_classSkillTrees.put(playerClass, classSkillTree);
							}
							else
							{
								_classSkillTrees.get(playerClass).putAll(classSkillTree);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Method to get the complete skill tree for a given class id.<br>
	 * Include all skills common to all classes.<br>
	 * Includes all parent skill trees.
	 * @param playerClass the class skill tree Id
	 * @return the complete Class Skill Tree including skill trees from parent class for a given {@code playerClass}
	 */
	public Map<Integer, SkillLearn> getCompleteClassSkillTree(PlayerClass playerClass)
	{
		return _completeClassSkillTree.getOrDefault(playerClass, Collections.emptyMap());
	}
	
	/**
	 * Gets the common skill tree.
	 * @return the complete Common Skill Tree
	 */
	public Map<Integer, SkillLearn> getCommonSkillTree()
	{
		return _commonSkillTree;
	}
	
	/**
	 * Gets the fishing skill tree.
	 * @return the complete Fishing Skill Tree
	 */
	public Map<Integer, SkillLearn> getFishingSkillTree()
	{
		return _fishingSkillTree;
	}
	
	/**
	 * Gets the pledge skill tree.
	 * @return the complete Clan Skill Tree
	 */
	public Map<Integer, SkillLearn> getPledgeSkillTree()
	{
		return _pledgeSkillTree;
	}
	
	/**
	 * Gets the noble skill tree.
	 * @return the complete Noble Skill Tree
	 */
	public Map<Integer, Skill> getNobleSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillData st = SkillData.getInstance();
		for (Entry<Integer, SkillLearn> e : _nobleSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
		}
		
		return tree;
	}
	
	/**
	 * Gets the hero skill tree.
	 * @return the complete Hero Skill Tree
	 */
	public Map<Integer, Skill> getHeroSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillData st = SkillData.getInstance();
		for (Entry<Integer, SkillLearn> e : _heroSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
		}
		
		return tree;
	}
	
	/**
	 * Gets the Game Master skill tree.
	 * @return the complete Game Master Skill Tree
	 */
	public Map<Integer, Skill> getGMSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillData st = SkillData.getInstance();
		for (Entry<Integer, SkillLearn> e : _gameMasterSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
		}
		
		return tree;
	}
	
	/**
	 * Gets the Game Master Aura skill tree.
	 * @return the complete Game Master Aura Skill Tree
	 */
	public Map<Integer, Skill> getGMAuraSkillTree()
	{
		final Map<Integer, Skill> tree = new HashMap<>();
		final SkillData st = SkillData.getInstance();
		for (Entry<Integer, SkillLearn> e : _gameMasterAuraSkillTree.entrySet())
		{
			tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
		}
		
		return tree;
	}
	
	/**
	 * Gets the available skills.
	 * @param player the learning skill player
	 * @param playerClass the learning skill class Id
	 * @param includeByFs if {@code true} skills from Forgotten Scroll will be included
	 * @param includeAutoGet if {@code true} Auto-Get skills will be included
	 * @return all available skills for a given {@code player}, {@code playerClass}, {@code includeByFs} and {@code includeAutoGet}
	 */
	public List<SkillLearn> getAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeAutoGet)
	{
		return getAvailableSkills(player, playerClass, includeByFs, includeAutoGet, true, player.getSkills());
	}
	
	/**
	 * Gets the available skills.
	 * @param player the learning skill player
	 * @param playerClass the learning skill class Id
	 * @param includeByFs if {@code true} skills from Forgotten Scroll will be included
	 * @param includeAutoGet if {@code true} Auto-Get skills will be included
	 * @param includeRequiredItems if {@code true} skills that have required items will be added
	 * @param existingSkills the complete Map of currently known skills.
	 * @return all available skills for a given {@code player}, {@code playerClass}, {@code includeByFs} and {@code includeAutoGet}
	 */
	private List<SkillLearn> getAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems, Map<Integer, Skill> existingSkills)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final Map<Integer, SkillLearn> skills = getCompleteClassSkillTree(playerClass);
		if (skills.isEmpty())
		{
			// The Skill Tree for this class is undefined.
			LOGGER.warning(getClass().getSimpleName() + ": Skilltree for class " + playerClass + " is not defined!");
			return result;
		}
		
		for (SkillLearn skill : skills.values())
		{
			if (((skill.getSkillId() == CommonSkill.DIVINE_INSPIRATION.getId()) && (!PlayerConfig.AUTO_LEARN_DIVINE_INSPIRATION && includeAutoGet) && !player.isGM()))
			{
				continue;
			}
			
			if (((!includeAutoGet || !skill.isAutoGet()) && !skill.isLearnedByNpc() && (!includeByFs || !skill.isLearnedByFS())) || (player.getLevel() < skill.getGetLevel()))
			{
				continue;
			}
			
			// Forgotten Scroll requirements checked above.
			if (!includeRequiredItems && !skill.getRequiredItems().isEmpty() && !skill.isLearnedByFS())
			{
				continue;
			}
			
			final Skill oldSkill = existingSkills.get(skill.getSkillId());
			if (oldSkill != null)
			{
				if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
				{
					result.add(skill);
				}
			}
			else if (skill.getSkillLevel() == 1)
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Used by auto learn configuration.
	 * @param player
	 * @param playerClass
	 * @param includeByFs if {@code true} forgotten scroll skills present in the skill tree will be added
	 * @param includeAutoGet if {@code true} auto-get skills present in the skill tree will be added
	 * @param includeRequiredItems if {@code true} skills that have required items will be added
	 * @return a list of auto learnable skills for the player.
	 */
	public Collection<Skill> getAllAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems)
	{
		final Map<Integer, Skill> result = new HashMap<>();
		for (Skill skill : player.getSkills().values())
		{
			// Adding only skills that can be learned by the player.
			if (isSkillAllowed(player, skill))
			{
				result.put(skill.getId(), skill);
			}
		}
		
		// Keep learning skills until no more are available.
		Collection<SkillLearn> learnableSkills;
		Collection<SkillLearn> previousLearnableSkills = null;
		while (true)
		{
			learnableSkills = getAvailableSkills(player, playerClass, includeByFs, includeAutoGet, includeRequiredItems, result);
			if (learnableSkills.isEmpty())
			{
				break;
			}
			
			// Break if no new skills became learnable since last iteration.
			if (learnableSkills.equals(previousLearnableSkills))
			{
				break;
			}
			
			for (SkillLearn skillLearn : learnableSkills)
			{
				final Skill skill = SkillData.getInstance().getSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel());
				result.put(skill.getId(), skill);
			}
			
			// Store reference for convergence check.
			previousLearnableSkills = learnableSkills;
		}
		
		return result.values();
	}
	
	/**
	 * Gets the available auto get skills.
	 * @param player the player requesting the Auto-Get skills
	 * @return all the available Auto-Get skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableAutoGetSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final Map<Integer, SkillLearn> skills = getCompleteClassSkillTree(player.getPlayerClass());
		if (skills.isEmpty())
		{
			// The Skill Tree for this class is undefined, so we return an empty list.
			LOGGER.warning(getClass().getSimpleName() + ": Skill Tree for this class Id(" + player.getPlayerClass() + ") is not defined!");
			return result;
		}
		
		final Race race = player.getRace();
		for (SkillLearn skill : skills.values())
		{
			if (!skill.getRaces().isEmpty() && !skill.getRaces().contains(race))
			{
				continue;
			}
			
			if (skill.isAutoGet() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() < skill.getSkillLevel())
					{
						result.add(skill);
					}
				}
				else
				{
					result.add(skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Dwarvens will get additional dwarven only fishing skills.
	 * @param player the player
	 * @return all the available Fishing skills for a given {@code player}
	 */
	public List<SkillLearn> getAvailableFishingSkills(Player player)
	{
		final List<SkillLearn> result = new LinkedList<>();
		final Race playerRace = player.getRace();
		for (SkillLearn skill : _fishingSkillTree.values())
		{
			// If skill is Race specific and the player's race isn't allowed, skip it.
			if (!skill.getRaces().isEmpty() && !skill.getRaces().contains(playerRace))
			{
				continue;
			}
			
			if (skill.isLearnedByNpc() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Gets the available pledge skills.
	 * @param clan the pledge skill learning clan
	 * @return all the available Clan skills for a given {@code clan}
	 */
	public List<SkillLearn> getAvailablePledgeSkills(Clan clan)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (!skill.isResidencialSkill() && (clan.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = clan.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if ((oldSkill.getLevel() + 1) == skill.getSkillLevel())
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Gets the available pledge skills.
	 * @param clan the pledge skill learning clan
	 * @return all the available pledge skills for a given {@code clan}
	 */
	public Map<Integer, SkillLearn> getMaxPledgeSkills(Clan clan)
	{
		final Map<Integer, SkillLearn> result = new HashMap<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (!skill.isResidencialSkill() && (clan.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = clan.getSkills().get(skill.getSkillId());
				if ((oldSkill == null) || (oldSkill.getLevel() < skill.getSkillLevel()))
				{
					result.put(skill.getSkillId(), skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Gets the available residential skills.
	 * @param residenceId the id of the Castle, Fort, Territory
	 * @return all the available Residential skills for a given {@code residenceId}
	 */
	public List<SkillLearn> getAvailableResidentialSkills(int residenceId)
	{
		final List<SkillLearn> result = new LinkedList<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (skill.isResidencialSkill() && skill.getResidenceIds().contains(residenceId))
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Just a wrapper for all skill trees.
	 * @param skillType the skill type
	 * @param id the skill Id
	 * @param lvl the skill level
	 * @param player the player learning the skill
	 * @return the skill learn for the specified parameters
	 */
	public SkillLearn getSkillLearn(AcquireSkillType skillType, int id, int lvl, Player player)
	{
		SkillLearn sl = null;
		switch (skillType)
		{
			case CLASS:
			{
				sl = getClassSkill(id, lvl, player.getLearningClass());
				break;
			}
			case FISHING:
			{
				sl = getFishingSkill(id, lvl);
				break;
			}
			case PLEDGE:
			{
				sl = getPledgeSkill(id, lvl);
				break;
			}
		}
		
		return sl;
	}
	
	/**
	 * Gets the class skill.
	 * @param id the class skill Id
	 * @param lvl the class skill level.
	 * @param playerClass the class skill tree Id
	 * @return the class skill from the Class Skill Trees for a given {@code playerClass}, {@code id} and {@code lvl}
	 */
	public SkillLearn getClassSkill(int id, int lvl, PlayerClass playerClass)
	{
		return getCompleteClassSkillTree(playerClass).get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the fishing skill.
	 * @param id the fishing skill Id
	 * @param lvl the fishing skill level
	 * @return Fishing skill from the Fishing Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getFishingSkill(int id, int lvl)
	{
		return _fishingSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the pledge skill.
	 * @param id the pledge skill Id
	 * @param lvl the pledge skill level
	 * @return the pledge skill from the Clan Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getPledgeSkill(int id, int lvl)
	{
		return _pledgeSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the common skill.
	 * @param id the common skill Id.
	 * @param lvl the common skill level
	 * @return the common skill from the Common Skill Tree for a given {@code id} and {@code lvl}
	 */
	public SkillLearn getCommonSkill(int id, int lvl)
	{
		return _commonSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Gets the minimum level for new skill.
	 * @param player the player that requires the minimum level
	 * @param skillTree the skill tree to search the minimum get level
	 * @return the minimum level for a new skill for a given {@code player} and {@code skillTree}
	 */
	public int getMinLevelForNewSkill(Player player, Map<Integer, SkillLearn> skillTree)
	{
		int minLevel = 0;
		if (skillTree.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": SkillTree is not defined for getMinLevelForNewSkill!");
		}
		else
		{
			for (SkillLearn s : skillTree.values())
			{
				if (s.isLearnedByNpc() && (player.getLevel() < s.getGetLevel()) && ((minLevel == 0) || (minLevel > s.getGetLevel())))
				{
					minLevel = s.getGetLevel();
				}
			}
		}
		
		return minLevel;
	}
	
	/**
	 * Checks if is hero skill.
	 * @param skillId the Id of the skill to check
	 * @param skillLevel the level of the skill to check, if it's -1 only Id will be checked
	 * @return {@code true} if the skill is present in the Hero Skill Tree, {@code false} otherwise
	 */
	public boolean isHeroSkill(int skillId, int skillLevel)
	{
		if (_heroSkillTree.containsKey(SkillData.getSkillHashCode(skillId, skillLevel)))
		{
			return true;
		}
		
		for (SkillLearn skill : _heroSkillTree.values())
		{
			if ((skill.getSkillId() == skillId) && (skillLevel == -1))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if is GM skill.
	 * @param skillId the Id of the skill to check
	 * @param skillLevel the level of the skill to check, if it's -1 only Id will be checked
	 * @return {@code true} if the skill is present in the Game Master Skill Trees, {@code false} otherwise
	 */
	public boolean isGMSkill(int skillId, int skillLevel)
	{
		if (skillLevel <= 0)
		{
			return _gameMasterSkillTree.values().stream().anyMatch(s -> s.getSkillId() == skillId) //
				|| _gameMasterAuraSkillTree.values().stream().anyMatch(s -> s.getSkillId() == skillId);
		}
		
		final int hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
		return _gameMasterSkillTree.containsKey(hashCode) || _gameMasterAuraSkillTree.containsKey(hashCode);
	}
	
	/**
	 * Checks if a skill is a Clan skill.
	 * @param skillId the Id of the skill to check
	 * @param skillLevel the level of the skill to check
	 * @return {@code true} if the skill is present in the Clan or Subpledge Skill Trees, {@code false} otherwise
	 */
	public boolean isClanSkill(int skillId, int skillLevel)
	{
		final int hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
		return _pledgeSkillTree.containsKey(hashCode);
	}
	
	/**
	 * Adds the skills.
	 * @param gmchar the player to add the Game Master skills
	 * @param auraSkills if {@code true} it will add "GM Aura" skills, else will add the "GM regular" skills
	 */
	public void addSkills(Player gmchar, boolean auraSkills)
	{
		final Collection<SkillLearn> skills = auraSkills ? _gameMasterAuraSkillTree.values() : _gameMasterSkillTree.values();
		final SkillData st = SkillData.getInstance();
		for (SkillLearn sl : skills)
		{
			gmchar.addSkill(st.getSkill(sl.getSkillId(), sl.getSkillLevel()), false); // Don't Save GM skills to database
		}
	}
	
	/**
	 * Create and store hash values for skills for easy and fast checks.
	 */
	private void generateCheckArrays()
	{
		int index;
		int[] skillHashes;
		
		// Class-specific skills.
		Map<Integer, SkillLearn> skillLearnMap;
		final Set<PlayerClass> playerClassSet = _classSkillTrees.keySet();
		_skillsByClassIdHashCodes = new HashMap<>(playerClassSet.size());
		for (PlayerClass playerClass : playerClassSet)
		{
			index = 0;
			skillLearnMap = new HashMap<>(getCompleteClassSkillTree(playerClass));
			skillHashes = new int[skillLearnMap.size()];
			for (int skillHash : skillLearnMap.keySet())
			{
				skillHashes[index++] = skillHash;
			}
			
			skillLearnMap.clear();
			Arrays.sort(skillHashes);
			_skillsByClassIdHashCodes.put(playerClass.getId(), skillHashes);
		}
		
		// Race-specific skills from Fishing and Transformation skill trees.
		final List<Integer> skillHashList = new LinkedList<>();
		_skillsByRaceHashCodes = new HashMap<>(Race.values().length);
		for (Race race : Race.values())
		{
			for (SkillLearn skillLearn : _fishingSkillTree.values())
			{
				if (skillLearn.getRaces().contains(race))
				{
					skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
				}
			}
			
			index = 0;
			skillHashes = new int[skillHashList.size()];
			for (int skillHash : skillHashList)
			{
				skillHashes[index++] = skillHash;
			}
			
			Arrays.sort(skillHashes);
			_skillsByRaceHashCodes.put(race.ordinal(), skillHashes);
			skillHashList.clear();
		}
		
		// Skills available for all classes and races.
		for (SkillLearn skillLearn : _commonSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		for (SkillLearn skillLearn : _fishingSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		_allSkillsHashCodes = new int[skillHashList.size()];
		int hashIndex = 0;
		for (int skillHash : skillHashList)
		{
			_allSkillsHashCodes[hashIndex++] = skillHash;
		}
		
		Arrays.sort(_allSkillsHashCodes);
	}
	
	/**
	 * Verify if the give skill is valid for the given player.<br>
	 * GM's skills are excluded for GM players
	 * @param player the player to verify the skill
	 * @param skill the skill to be verified
	 * @return {@code true} if the skill is allowed to the given player
	 */
	public boolean isSkillAllowed(Player player, Skill skill)
	{
		if (skill.isExcludedFromCheck())
		{
			return true;
		}
		
		if (player.isGM() && skill.isGMSkill())
		{
			return true;
		}
		
		// Prevent accidental skill remove during reload
		if (_loading)
		{
			return true;
		}
		
		final int maxLevel = SkillData.getInstance().getMaxLevel(skill.getId());
		final int hashCode = SkillData.getSkillHashCode(skill.getId(), Math.min(skill.getLevel(), maxLevel));
		if (Arrays.binarySearch(_skillsByClassIdHashCodes.get(player.getPlayerClass().getId()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_skillsByRaceHashCodes.get(player.getRace().ordinal()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_allSkillsHashCodes, hashCode) >= 0)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Logs current Skill Trees skills count.
	 */
	private void report()
	{
		int classSkillTreeCount = 0;
		for (Map<Integer, SkillLearn> classSkillTree : _classSkillTrees.values())
		{
			classSkillTreeCount += classSkillTree.size();
		}
		
		int dwarvenOnlyFishingSkillCount = 0;
		for (SkillLearn fishSkill : _fishingSkillTree.values())
		{
			if (fishSkill.getRaces().contains(Race.DWARF))
			{
				dwarvenOnlyFishingSkillCount++;
			}
		}
		
		int resSkillCount = 0;
		for (SkillLearn pledgeSkill : _pledgeSkillTree.values())
		{
			if (pledgeSkill.isResidencialSkill())
			{
				resSkillCount++;
			}
		}
		
		final String className = getClass().getSimpleName();
		LOGGER.info(className + ": Loaded " + classSkillTreeCount + " Class Skills for " + _classSkillTrees.size() + " class skill trees.");
		LOGGER.info(className + ": Loaded " + _fishingSkillTree.size() + " fishing skills, " + dwarvenOnlyFishingSkillCount + " Dwarven only fishing skills.");
		LOGGER.info(className + ": Loaded " + _pledgeSkillTree.size() + " clan skills, " + (_pledgeSkillTree.size() - resSkillCount) + " for clan and " + resSkillCount + " residential.");
		LOGGER.info(className + ": Loaded " + _nobleSkillTree.size() + " noble skills.");
		LOGGER.info(className + ": Loaded " + _heroSkillTree.size() + " hero skills.");
		LOGGER.info(className + ": Loaded " + _gameMasterSkillTree.size() + " game master skills.");
		LOGGER.info(className + ": Loaded " + _gameMasterAuraSkillTree.size() + " game master aura skills.");
		final int commonSkills = _commonSkillTree.size();
		if (commonSkills > 0)
		{
			LOGGER.info(className + ": Loaded " + commonSkills + " common skills.");
		}
	}
	
	/**
	 * Gets the single instance of SkillTreesData.
	 * @return the only instance of this class
	 */
	public static SkillTreeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Singleton holder for the SkillTreesData class.
	 */
	private static class SingletonHolder
	{
		protected static final SkillTreeData INSTANCE = new SkillTreeData();
	}
}
