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
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;

/**
 * This class holds the Experience points for each level for players and pets.
 * <p>
 * It provides functionality to load experience data from an XML file and retrieve the experience points required for a given level. It also maintains the maximum levels for players and pets.
 * </p>
 * <p>
 * <strong>XML Structure:</strong>
 * </p>
 * The XML file must have a root element containing the `maxLevel` and `maxPetLevel` attributes, along with nested `experience` elements that define the experience points for each level.
 * @author Mobius
 */
public class ExperienceData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ExperienceData.class.getName());
	
	private long[] _expTable;
	
	private byte _maxLevel;
	private byte _maxPetLevel;
	
	protected ExperienceData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/stats/players/experience.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (_expTable.length - 1) + " levels.");
		LOGGER.info(getClass().getSimpleName() + ": Max Player Level is " + (_maxLevel - 1) + ".");
		LOGGER.info(getClass().getSimpleName() + ": Max Pet Level is " + (_maxPetLevel - 1) + ".");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, tableNode ->
		{
			final NamedNodeMap tableAttr = tableNode.getAttributes();
			_maxLevel = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxLevel").getNodeValue()) + 1);
			_maxPetLevel = (byte) (Byte.parseByte(tableAttr.getNamedItem("maxPetLevel").getNodeValue()) + 1);
			if (_maxLevel > PlayerConfig.PLAYER_MAXIMUM_LEVEL)
			{
				_maxLevel = PlayerConfig.PLAYER_MAXIMUM_LEVEL;
			}
			
			if (_maxPetLevel > (_maxLevel + 1))
			{
				_maxPetLevel = (byte) (_maxLevel + 1); // Pet level should not exceed owner level.
			}
			
			// Initialize the array with size _maxLevel + 1 (level 0 is unused).
			_expTable = new long[_maxLevel + 1];
			
			forEach(tableNode, "experience", experienceNode ->
			{
				final NamedNodeMap attrs = experienceNode.getAttributes();
				final int level = parseInteger(attrs, "level");
				if ((level > PlayerConfig.PLAYER_MAXIMUM_LEVEL) || (level > _maxLevel))
				{
					return;
				}
				
				_expTable[level] = parseLong(attrs, "tolevel");
			});
		});
	}
	
	/**
	 * Retrieves the experience points required to reach the specified level.
	 * @param level the level for which experience points are required
	 * @return the experience points needed to reach the specified level; if the specified level exceeds {@code Config.PLAYER_MAXIMUM_LEVEL}, returns the experience points for {@code Config.PLAYER_MAXIMUM_LEVEL}
	 */
	public long getExpForLevel(int level)
	{
		if (level > PlayerConfig.PLAYER_MAXIMUM_LEVEL)
		{
			return _expTable[PlayerConfig.PLAYER_MAXIMUM_LEVEL];
		}
		
		return _expTable[level];
	}
	
	/**
	 * Returns the maximum level acquirable by a player in the game.
	 * @return the maximum player level as an integer
	 */
	public byte getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Returns the maximum level acquirable by a pet in the game.
	 * @return the maximum pet level as an integer, limited by the player level
	 */
	public byte getMaxPetLevel()
	{
		return _maxPetLevel;
	}
	
	public static ExperienceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceData INSTANCE = new ExperienceData();
	}
}
