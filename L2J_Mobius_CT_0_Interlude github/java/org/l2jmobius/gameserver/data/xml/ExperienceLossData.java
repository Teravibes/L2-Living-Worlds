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
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;

/**
 * @author Mobius
 */
public class ExperienceLossData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ExperienceLossData.class.getName());
	
	private final double[] _levelPercentLost = new double[PlayerConfig.PLAYER_MAXIMUM_LEVEL];
	
	protected ExperienceLossData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		Arrays.fill(_levelPercentLost, 1d);
		parseDatapackFile("data/stats/players/experienceLoss.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (_levelPercentLost.length - 1) + " levels.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "experience", experienceNode ->
		{
			final NamedNodeMap attrs = experienceNode.getAttributes();
			final int level = parseInteger(attrs, "level");
			if (level >= PlayerConfig.PLAYER_MAXIMUM_LEVEL)
			{
				return;
			}
			
			_levelPercentLost[level] = parseDouble(attrs, "val");
		}));
	}
	
	/**
	 * Retrieves the experience point (XP) percentage lost for a specified level.
	 * @param level the level for which to retrieve the XP loss percentage.
	 * @return the XP loss percentage for the specified level.
	 */
	public double getPercentLost(int level)
	{
		if (level >= PlayerConfig.PLAYER_MAXIMUM_LEVEL)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Requested too high level (" + level + ").");
			return _levelPercentLost[_levelPercentLost.length - 1];
		}
		
		return _levelPercentLost[level];
	}
	
	public static ExperienceLossData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ExperienceLossData INSTANCE = new ExperienceLossData();
	}
}
