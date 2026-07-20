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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;

/**
 * @author Forsaiken, Mobius
 */
public class PlayerTemplateData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PlayerTemplateData.class.getName());
	
	private final Map<PlayerClass, PlayerTemplate> _playerTemplates = new ConcurrentHashMap<>();
	final AtomicInteger _levelUpGainCount = new AtomicInteger();
	
	protected PlayerTemplateData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_playerTemplates.clear();
		_levelUpGainCount.set(0);
		parseDatapackDirectory("data/stats/players/templates", true);
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _playerTemplates.size() + " player templates.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _levelUpGainCount.get() + " level up gain records.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final int maxLevel = ExperienceData.getInstance().getMaxLevel();
		
		NamedNodeMap attributes;
		int classId = 0;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("classId".equalsIgnoreCase(d.getNodeName()))
					{
						classId = Integer.parseInt(d.getTextContent());
					}
					else if ("staticData".equalsIgnoreCase(d.getNodeName()))
					{
						final StatSet set = new StatSet();
						set.set("classId", classId);
						final List<Location> creationPoints = new ArrayList<>();
						for (Node nd = d.getFirstChild(); nd != null; nd = nd.getNextSibling())
						{
							// Skip odd nodes.
							if (nd.getNodeName().equals("#text"))
							{
								continue;
							}
							
							if (nd.getChildNodes().getLength() > 1)
							{
								for (Node cnd = nd.getFirstChild(); cnd != null; cnd = cnd.getNextSibling())
								{
									// Use CreatureTemplate(superclass) fields for male collision height and collision radius.
									if (nd.getNodeName().equalsIgnoreCase("collisionMale"))
									{
										if (cnd.getNodeName().equalsIgnoreCase("radius"))
										{
											set.set("collisionRadius", cnd.getTextContent());
										}
										else if (cnd.getNodeName().equalsIgnoreCase("height"))
										{
											set.set("collisionHeight", cnd.getTextContent());
										}
									}
									
									if ("node".equalsIgnoreCase(cnd.getNodeName()))
									{
										attributes = cnd.getAttributes();
										creationPoints.add(new Location(parseInteger(attributes, "x"), parseInteger(attributes, "y"), parseInteger(attributes, "z")));
									}
									else if ("walk".equalsIgnoreCase(cnd.getNodeName()))
									{
										set.set("baseWalkSpd", cnd.getTextContent());
									}
									else if ("run".equalsIgnoreCase(cnd.getNodeName()))
									{
										set.set("baseRunSpd", cnd.getTextContent());
									}
									else if ("slowSwim".equals(cnd.getNodeName()))
									{
										set.set("baseSwimWalkSpd", cnd.getTextContent());
									}
									else if ("fastSwim".equals(cnd.getNodeName()))
									{
										set.set("baseSwimRunSpd", cnd.getTextContent());
									}
									else if (!cnd.getNodeName().equals("#text"))
									{
										set.set(nd.getNodeName() + cnd.getNodeName(), cnd.getTextContent());
									}
								}
							}
							else
							{
								set.set(nd.getNodeName(), nd.getTextContent());
							}
						}
						
						// Calculate total pdef and mdef from parts.
						set.set("basePDef", set.getInt("basePDefchest", 0) + set.getInt("basePDeflegs", 0) + set.getInt("basePDefhead", 0) + set.getInt("basePDeffeet", 0) + set.getInt("basePDefgloves", 0) + set.getInt("basePDefunderwear", 0) + set.getInt("basePDefcloak", 0));
						set.set("baseMDef", set.getInt("baseMDefrear", 0) + set.getInt("baseMDeflear", 0) + set.getInt("baseMDefrfinger", 0) + set.getInt("baseMDefrfinger", 0) + set.getInt("baseMDefneck", 0));
						_playerTemplates.put(PlayerClass.getPlayerClass(classId), new PlayerTemplate(set, creationPoints));
					}
					else if ("lvlUpgainData".equalsIgnoreCase(d.getNodeName()))
					{
						int level = 0;
						final PlayerTemplate template = _playerTemplates.get(PlayerClass.getPlayerClass(classId));
						for (Node lvlNode = d.getFirstChild(); lvlNode != null; lvlNode = lvlNode.getNextSibling())
						{
							if ("level".equalsIgnoreCase(lvlNode.getNodeName()))
							{
								attributes = lvlNode.getAttributes();
								level = parseInteger(attributes, "val");
								if (level > (maxLevel - 1))
								{
									return;
								}
								
								for (Node valNode = lvlNode.getFirstChild(); valNode != null; valNode = valNode.getNextSibling())
								{
									final String nodeName = valNode.getNodeName();
									if (nodeName.startsWith("hp") || nodeName.startsWith("mp") || nodeName.startsWith("cp"))
									{
										template.setUpgainValue(nodeName, level, Double.parseDouble(valNode.getTextContent()));
										_levelUpGainCount.incrementAndGet();
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the {@link PlayerTemplate} associated with the specified {@link PlayerClass}.
	 * @param playerClass the {@link PlayerClass} for which to retrieve the template.
	 * @return the {@link PlayerTemplate} associated with the given {@link PlayerClass}, or {@code null} if no template is found.
	 */
	public PlayerTemplate getTemplate(PlayerClass playerClass)
	{
		return _playerTemplates.get(playerClass);
	}
	
	/**
	 * Retrieves the {@link PlayerTemplate} associated with the specified class ID.
	 * @param classId the integer ID of the class for which to retrieve the template.
	 * @return the {@link PlayerTemplate} associated with the given class ID, or {@code null} if no template is found.
	 */
	public PlayerTemplate getTemplate(int classId)
	{
		return _playerTemplates.get(PlayerClass.getPlayerClass(classId));
	}
	
	public static PlayerTemplateData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerTemplateData INSTANCE = new PlayerTemplateData();
	}
}
