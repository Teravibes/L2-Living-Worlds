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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.holders.player.ClassInfoHolder;

/**
 * Loads and manages the list of player classes and their associated information.
 * @author Zoey76, Mobius
 */
public class ClassListData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClassListData.class.getName());
	
	private final Map<PlayerClass, ClassInfoHolder> _classData = new ConcurrentHashMap<>();
	
	protected ClassListData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_classData.clear();
		parseDatapackFile("data/stats/players/classList.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classData.size() + " class data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "class", classNode ->
		{
			final NamedNodeMap attrs = classNode.getAttributes();
			final PlayerClass playerClass = PlayerClass.getPlayerClass(parseInteger(attrs, "classId"));
			final PlayerClass parentPlayerClass = parseInteger(attrs, "parentClassId", -1) > 0 ? PlayerClass.getPlayerClass(parseInteger(attrs, "parentClassId")) : null;
			final String className = parseString(attrs, "name");
			_classData.put(playerClass, new ClassInfoHolder(playerClass, parentPlayerClass, className));
		}));
	}
	
	/**
	 * Gets the complete map of all player classes and their associated information.
	 * @return an unmodifiable map containing all class data, where keys are {@link PlayerClass} enum values and values are their corresponding {@link ClassInfoHolder} objects
	 */
	public Map<PlayerClass, ClassInfoHolder> getClassList()
	{
		return _classData;
	}
	
	/**
	 * Gets the class information for a specific {@link PlayerClass}.
	 * @param playerClass the class identifier as a {@link PlayerClass} enum value
	 * @return the {@link ClassInfoHolder} containing information about the specified class, or {@code null} if the class is not found
	 */
	public ClassInfoHolder getClass(PlayerClass playerClass)
	{
		return _classData.get(playerClass);
	}
	
	/**
	 * Gets the class information for a specific class ID.
	 * @param classId the numeric identifier of the class
	 * @return the {@link ClassInfoHolder} containing information about the specified class, or {@code null} if the class ID is invalid or not found
	 */
	public ClassInfoHolder getClass(int classId)
	{
		final PlayerClass id = PlayerClass.getPlayerClass(classId);
		return (id != null) ? _classData.get(id) : null;
	}
	
	public static ClassListData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassListData INSTANCE = new ClassListData();
	}
}
