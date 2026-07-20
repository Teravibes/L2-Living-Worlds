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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CategoryType;

/**
 * @author NosBit, xban1x
 */
public class CategoryData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CategoryData.class.getName());
	
	private final Map<CategoryType, Set<Integer>> _categories = new EnumMap<>(CategoryType.class);
	
	protected CategoryData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_categories.clear();
		parseDatapackFile("data/CategoryData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _categories.size() + " categories.");
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
					if ("category".equalsIgnoreCase(list_node.getNodeName()))
					{
						final NamedNodeMap attrs = list_node.getAttributes();
						final CategoryType categoryType = CategoryType.findByName(attrs.getNamedItem("name").getNodeValue());
						if (categoryType == null)
						{
							LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Can't find category by name :" + attrs.getNamedItem("name").getNodeValue());
							continue;
						}
						
						final Set<Integer> ids = new HashSet<>();
						for (Node category_node = list_node.getFirstChild(); category_node != null; category_node = category_node.getNextSibling())
						{
							if ("id".equalsIgnoreCase(category_node.getNodeName()))
							{
								ids.add(Integer.parseInt(category_node.getTextContent()));
							}
						}
						
						_categories.put(categoryType, ids);
					}
				}
			}
		}
	}
	
	/**
	 * Checks if a specified ID is present in a given category type.
	 * @param type the category type to check
	 * @param id the ID to be checked within the category
	 * @return {@code true} if the ID is in the specified category, {@code false} if the ID is not in the category or if the category was not found
	 */
	public boolean isInCategory(CategoryType type, int id)
	{
		final Set<Integer> category = getCategoryByType(type);
		if (category == null)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Can't find category type :" + type);
			return false;
		}
		
		return category.contains(id);
	}
	
	/**
	 * Retrieves the category set associated with the specified category type.
	 * @param type the category type to retrieve
	 * @return a {@code Set} containing all IDs within the specified category, or {@code null} if the category does not exist
	 */
	public Set<Integer> getCategoryByType(CategoryType type)
	{
		return _categories.get(type);
	}
	
	public static CategoryData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CategoryData INSTANCE = new CategoryData();
	}
}
