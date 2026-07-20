/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * Holds all skill learn data for all NPCs.
 * @author xban1x
 */
public class SkillLearnData implements IXmlReader
{
	private final Map<Integer, List<PlayerClass>> _skillLearn = new HashMap<>();
	
	protected SkillLearnData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_skillLearn.clear();
		parseDatapackFile("data/SkillLearn.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skillLearn.size() + " skill learn data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(list_node.getNodeName()))
					{
						final List<PlayerClass> classIds = new ArrayList<>();
						for (Node c = list_node.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("classId".equalsIgnoreCase(c.getNodeName()))
							{
								classIds.add(PlayerClass.getPlayerClass(Integer.parseInt(c.getTextContent())));
							}
						}
						
						_skillLearn.put(parseInteger(list_node.getAttributes(), "id"), classIds);
					}
				}
			}
		}
	}
	
	/**
	 * @param npcId
	 * @return {@link List} of {@link PlayerClass}'s that this npcId can teach.
	 */
	public List<PlayerClass> getSkillLearnData(int npcId)
	{
		return _skillLearn.get(npcId);
	}
	
	/**
	 * Gets the single instance of SkillLearnData.
	 * @return single instance of SkillLearnData
	 */
	public static SkillLearnData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillLearnData INSTANCE = new SkillLearnData();
	}
}
