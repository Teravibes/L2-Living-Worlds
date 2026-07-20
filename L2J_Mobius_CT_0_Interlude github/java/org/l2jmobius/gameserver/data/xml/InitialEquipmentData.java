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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.holders.InitialEquipment;

/**
 * @author Mobius
 */
public class InitialEquipmentData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(InitialEquipmentData.class.getName());
	
	private final Map<PlayerClass, List<InitialEquipment>> _classEquipment = new EnumMap<>(PlayerClass.class);
	
	protected InitialEquipmentData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_classEquipment.clear();
		parseDatapackFile("data/stats/players/initialEquipment.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classEquipment.size() + " initial equipment data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "equipment", equipmentNode ->
		{
			final Map<String, Object> attributes = parseAttributes(equipmentNode);
			final List<InitialEquipment> equipment = new ArrayList<>();
			forEach(equipmentNode, "item", itemNode ->
			{
				final StatSet set = new StatSet();
				parseAttributes(itemNode).forEach(set::set);
				equipment.add(new InitialEquipment(set));
			});
			
			final PlayerClass playerClass = PlayerClass.getPlayerClass(Integer.parseInt((String) attributes.get("classId")));
			_classEquipment.put(playerClass, equipment);
		}));
	}
	
	/**
	 * Retrieves the initial equipment items associated with a specified class.
	 * @param playerClass the {@link PlayerClass} representing the class for which the initial equipment is required
	 * @return a {@link Collection} of {@link InitialEquipment} objects representing the initial equipment for the specified class, or {@code null} if no equipment is found for the given class
	 */
	public Collection<InitialEquipment> getClassEquipment(PlayerClass playerClass)
	{
		return _classEquipment.get(playerClass);
	}
	
	public static InitialEquipmentData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InitialEquipmentData INSTANCE = new InitialEquipmentData();
	}
}
