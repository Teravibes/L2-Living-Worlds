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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.model.actor.holders.npc.BuffSkillHolder;

/**
 * Loads available scheme buffer skills and manages player schemes in memory and database.<br>
 * Provides category-aware lookups to support duplicated skill IDs across different categories.
 * <ul>
 * <li>Loads skills from {@value #SKILLS_XML_PATH}.</li>
 * <li>Loads and saves player schemes from/to database.</li>
 * <li>Resolves category-aware skill holders for manual pages.</li>
 * </ul>
 * @author Mobius, BazookaRpm
 */
public class SchemeBufferTable
{
	private static final Logger LOGGER = Logger.getLogger(SchemeBufferTable.class.getName());
	
	// Constants.
	private static final String SKILLS_XML_PATH = "./data/SchemeBufferSkills.xml";
	private static final String LOAD_SCHEMES = "SELECT * FROM buffer_schemes";
	private static final String DELETE_SCHEMES = "TRUNCATE TABLE buffer_schemes";
	private static final String INSERT_SCHEME = "INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)";
	private static final String TYPE_MAGE_GROUP = "MAGE_GROUP";
	private static final String TYPE_FIGHTER_GROUP = "FIGHTER_GROUP";
	private static final String DB_COLUMN_OBJECT_ID = "object_id";
	private static final String DB_COLUMN_SCHEME_NAME = "scheme_name";
	private static final String DB_COLUMN_SKILLS = "skills";
	private static final String SKILL_SEPARATOR = ",";
	
	// Player schemes storage.
	private final Map<Integer, Map<String, List<Integer>>> _schemesTable = new ConcurrentHashMap<>();
	
	// Skills registry.
	private final Map<Integer, BuffSkillHolder> _availableBuffs = new LinkedHashMap<>();
	private final Map<String, List<Integer>> _skillIdsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final Map<String, Map<Integer, BuffSkillHolder>> _availableBuffsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final List<String> _skillTypesOrder = new ArrayList<>();
	
	/**
	 * Initializes skills registry and loads player schemes.
	 */
	public SchemeBufferTable()
	{
		loadAvailableBuffs();
		loadPlayerSchemes();
		LOGGER.info("SchemeBufferTable: Loaded " + _schemesTable.size() + " players and " + _availableBuffs.size() + " available skills.");
	}
	
	/**
	 * Saves all loaded schemes to database by truncating and inserting current in-memory data.
	 */
	public void saveSchemes()
	{
		try (Connection connection = DatabaseFactory.getConnection())
		{
			try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_SCHEMES))
			{
				deleteStatement.execute();
			}
			
			try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_SCHEME))
			{
				for (Entry<Integer, Map<String, List<Integer>>> playerEntry : _schemesTable.entrySet())
				{
					for (Entry<String, List<Integer>> schemeEntry : playerEntry.getValue().entrySet())
					{
						final StringBuilder skillsBuilder = new StringBuilder();
						for (int skillId : schemeEntry.getValue())
						{
							skillsBuilder.append(skillId).append(SKILL_SEPARATOR);
						}
						
						if (skillsBuilder.length() > 0)
						{
							skillsBuilder.setLength(skillsBuilder.length() - SKILL_SEPARATOR.length());
						}
						
						insertStatement.setInt(1, playerEntry.getKey());
						insertStatement.setString(2, schemeEntry.getKey());
						insertStatement.setString(3, skillsBuilder.toString());
						insertStatement.addBatch();
					}
				}
				insertStatement.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Error while saving schemes. Players: " + _schemesTable.size() + ".", e);
		}
	}
	
	/**
	 * Registers or replaces a scheme for the given player.
	 * @param playerId
	 * @param schemeName
	 * @param skillIds
	 */
	public void setScheme(int playerId, String schemeName, List<Integer> skillIds)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.computeIfAbsent(playerId, key -> new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER));
		if ((schemes.size() >= SchemeBufferConfig.BUFFER_MAX_SCHEMES) && !schemes.containsKey(schemeName))
		{
			return;
		}
		
		final List<Integer> safeList = (skillIds != null) ? Collections.unmodifiableList(new ArrayList<>(skillIds)) : Collections.emptyList();
		schemes.put(schemeName, safeList);
	}
	
	/**
	 * Deletes a scheme for the given player.
	 * @param playerId
	 * @param schemeName
	 * @return true if removed.
	 */
	public boolean deleteScheme(int playerId, String schemeName)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}
		
		final boolean removed = schemes.remove(schemeName) != null;
		if (schemes.isEmpty())
		{
			_schemesTable.remove(playerId);
		}
		return removed;
	}
	
	/**
	 * Adds a skill to a scheme (copy-on-write). No-op if scheme doesn't exist.
	 * @param playerId
	 * @param schemeName
	 * @param skillId
	 * @return true if added.
	 */
	public boolean addSkillToScheme(int playerId, String schemeName, int skillId)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}
		
		final List<Integer> current = schemes.get(schemeName);
		if (current == null)
		{
			return false;
		}
		
		if (current.contains(skillId))
		{
			return false;
		}
		
		final List<Integer> updated = new ArrayList<>(current);
		updated.add(skillId);
		schemes.put(schemeName, Collections.unmodifiableList(updated));
		return true;
	}
	
	/**
	 * Removes a skill from a scheme (copy-on-write). No-op if scheme doesn't exist.
	 * @param playerId
	 * @param schemeName
	 * @param skillId
	 * @return true if removed.
	 */
	public boolean removeSkillFromScheme(int playerId, String schemeName, int skillId)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}
		
		final List<Integer> current = schemes.get(schemeName);
		if (current == null)
		{
			return false;
		}
		
		if (!current.contains(skillId))
		{
			return false;
		}
		
		final List<Integer> updated = new ArrayList<>(current);
		updated.remove(Integer.valueOf(skillId));
		schemes.put(schemeName, Collections.unmodifiableList(updated));
		return true;
	}
	
	/**
	 * Returns the scheme map for a player (read-only view).
	 * @param playerId
	 * @return the schemes map or null.
	 */
	public Map<String, List<Integer>> getPlayerSchemes(int playerId)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		return (schemes != null) ? Collections.unmodifiableMap(schemes) : null;
	}
	
	/**
	 * Returns the skill ID list for a specific player scheme.
	 * @param playerId
	 * @param schemeName
	 * @return the scheme skill list or an empty list.
	 */
	public List<Integer> getScheme(int playerId, String schemeName)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if ((schemes == null) || (schemes.get(schemeName) == null))
		{
			return Collections.emptyList();
		}
		return schemes.get(schemeName);
	}
	
	/**
	 * Returns the skill ID list for a category/type without copying.<br>
	 * The returned list is immutable and safe to share across threads.
	 * @param groupType
	 * @return the category skill IDs.
	 */
	public List<Integer> getSkillsIdsByType(String groupType)
	{
		return _skillIdsByType.getOrDefault(groupType, Collections.emptyList());
	}
	
	/**
	 * Returns available category/type names for scheme editing.<br>
	 * Internal groups (MAGE_GROUP / FIGHTER_GROUP) are hidden from the edit UI.
	 * @return the list of visible category names.
	 */
	public List<String> getSkillTypes()
	{
		final List<String> skillTypes = new ArrayList<>(_skillTypesOrder.size());
		for (String type : _skillTypesOrder)
		{
			if (type.equalsIgnoreCase(TYPE_MAGE_GROUP) || type.equalsIgnoreCase(TYPE_FIGHTER_GROUP))
			{
				continue;
			}
			skillTypes.add(type);
		}
		return skillTypes;
	}
	
	/**
	 * Returns the global holder for a skill ID.
	 * @param skillId
	 * @return the global holder or null.
	 */
	public BuffSkillHolder getAvailableBuff(int skillId)
	{
		return _availableBuffs.get(skillId);
	}
	
	/**
	 * Returns the category-aware holder for a skill ID.
	 * @param groupType
	 * @param skillId
	 * @return the resolved holder or null.
	 */
	public BuffSkillHolder getAvailableBuff(String groupType, int skillId)
	{
		final Map<Integer, BuffSkillHolder> holdersByType = _availableBuffsByType.get(groupType);
		return (holdersByType != null) ? holdersByType.getOrDefault(skillId, _availableBuffs.get(skillId)) : _availableBuffs.get(skillId);
	}
	
	/**
	 * Returns the singleton instance.
	 * @return the instance.
	 */
	public static SchemeBufferTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private void loadAvailableBuffs()
	{
		int categoryCount = 0;
		int skillCount = 0;
		
		try
		{
			final Node rootNode = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(SKILLS_XML_PATH)).getDocumentElement();
			for (Node categoryNode = (rootNode != null) ? rootNode.getFirstChild() : null; categoryNode != null; categoryNode = categoryNode.getNextSibling())
			{
				if (!"category".equalsIgnoreCase(categoryNode.getNodeName()))
				{
					continue;
				}
				
				final NamedNodeMap categoryAttributes = categoryNode.getAttributes();
				if (categoryAttributes == null)
				{
					continue;
				}
				
				final Node typeNode = categoryAttributes.getNamedItem("type");
				if (typeNode == null)
				{
					continue;
				}
				
				final String category = typeNode.getNodeValue();
				if (!_skillIdsByType.containsKey(category))
				{
					_skillIdsByType.put(category, new ArrayList<>());
					_availableBuffsByType.put(category, new LinkedHashMap<>());
					_skillTypesOrder.add(category);
					categoryCount++;
				}
				
				for (Node buffNode = categoryNode.getFirstChild(); buffNode != null; buffNode = buffNode.getNextSibling())
				{
					if (!"buff".equalsIgnoreCase(buffNode.getNodeName()))
					{
						continue;
					}
					
					final NamedNodeMap attributes = buffNode.getAttributes();
					if (attributes == null)
					{
						continue;
					}
					
					final Node idNode = attributes.getNamedItem("id");
					final Node levelNode = attributes.getNamedItem("level");
					final Node priceNode = attributes.getNamedItem("price");
					if ((idNode == null) || (levelNode == null) || (priceNode == null))
					{
						continue;
					}
					
					final int skillId = Integer.parseInt(idNode.getNodeValue());
					final int level = Integer.parseInt(levelNode.getNodeValue());
					final int price = Integer.parseInt(priceNode.getNodeValue());
					final Node descNode = attributes.getNamedItem("desc");
					final String description = (descNode != null) ? descNode.getNodeValue() : "";
					
					final List<Integer> skillIds = _skillIdsByType.get(category);
					if (!skillIds.contains(skillId))
					{
						skillIds.add(skillId);
					}
					
					final BuffSkillHolder holder = new BuffSkillHolder(skillId, level, price, category, description);
					_availableBuffsByType.get(category).put(skillId, holder);
					
					final BuffSkillHolder existing = _availableBuffs.get(skillId);
					if ((existing == null) || (holder.getLevel() > existing.getLevel()))
					{
						_availableBuffs.put(skillId, holder);
					}
					
					skillCount++;
				}
			}
			
			for (Entry<String, List<Integer>> entry : _skillIdsByType.entrySet())
			{
				entry.setValue(Collections.unmodifiableList(entry.getValue()));
			}
			
			LOGGER.info("SchemeBufferTable: Loaded " + categoryCount + " categories and " + skillCount + " entries from " + SKILLS_XML_PATH + ".");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load skills XML: " + SKILLS_XML_PATH + ".", e);
		}
	}
	
	private void loadPlayerSchemes()
	{
		int entryCount = 0;
		
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(LOAD_SCHEMES);
			ResultSet resultSet = statement.executeQuery())
		{
			while (resultSet.next())
			{
				final int objectId = resultSet.getInt(DB_COLUMN_OBJECT_ID);
				final String schemeName = resultSet.getString(DB_COLUMN_SCHEME_NAME);
				final String skills = resultSet.getString(DB_COLUMN_SKILLS);
				if ((schemeName == null) || (skills == null))
				{
					continue;
				}
				
				final String[] split = skills.split(SKILL_SEPARATOR);
				final List<Integer> schemeSkillIds = new ArrayList<>(split.length);
				
				for (String token : split)
				{
					if (token.isEmpty())
					{
						continue;
					}
					
					final int skillId;
					try
					{
						skillId = Integer.parseInt(token);
					}
					catch (NumberFormatException e)
					{
						LOGGER.warning("SchemeBufferTable: Invalid skill id '" + token + "' for scheme '" + schemeName + "' (objectId: " + objectId + ").");
						continue;
					}
					
					if (_availableBuffs.containsKey(skillId))
					{
						schemeSkillIds.add(skillId);
					}
				}
				
				setScheme(objectId, schemeName, schemeSkillIds);
				entryCount++;
			}
			
			LOGGER.info("SchemeBufferTable: Loaded " + entryCount + " scheme entries from database.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load buff schemes from database.", e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final SchemeBufferTable INSTANCE = new SchemeBufferTable();
	}
}
