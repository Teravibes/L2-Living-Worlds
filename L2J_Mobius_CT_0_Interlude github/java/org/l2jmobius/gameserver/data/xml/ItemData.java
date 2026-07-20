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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.util.DocumentItem;

/**
 * This class serves as a container for all item templates in the game.
 */
public class ItemData
{
	private static final Logger LOGGER = Logger.getLogger(ItemData.class.getName());
	
	private ItemTemplate[] _allTemplates;
	private final Map<Integer, EtcItem> _etcItems = new HashMap<>();
	private final Map<Integer, Armor> _armors = new HashMap<>();
	private final Map<Integer, Weapon> _weapons = new HashMap<>();
	private final List<File> _itemFiles = new ArrayList<>();
	
	protected ItemData()
	{
		processDirectory("data/stats/items", _itemFiles);
		if (GeneralConfig.CUSTOM_ITEMS_LOAD)
		{
			processDirectory("data/stats/items/custom", _itemFiles);
		}
		
		load();
	}
	
	private void processDirectory(String dirName, List<File> list)
	{
		final File dir = new File(ServerConfig.DATAPACK_ROOT, dirName);
		if (!dir.exists())
		{
			LOGGER.warning("Directory " + dir.getAbsolutePath() + " does not exist.");
			return;
		}
		
		final File[] files = dir.listFiles();
		if (files != null)
		{
			for (File file : files)
			{
				if (file.isFile() && file.getName().toLowerCase().endsWith(".xml"))
				{
					list.add(file);
				}
			}
		}
	}
	
	private void load()
	{
		final Collection<ItemTemplate> items = ConcurrentHashMap.newKeySet();
		int highestId = 0;
		_armors.clear();
		_etcItems.clear();
		_weapons.clear();
		
		// If multithreading is enabled, use a thread pool to parse files.
		if (ThreadConfig.THREADS_FOR_LOADING)
		{
			final Collection<ScheduledFuture<?>> tasks = ConcurrentHashMap.newKeySet();
			for (File file : _itemFiles)
			{
				tasks.add(ThreadPool.schedule(() ->
				{
					final DocumentItem document = new DocumentItem(file);
					document.parse();
					items.addAll(document.getItemList());
				}, 0));
			}
			
			// Wait for all scheduled tasks to complete.
			while (!tasks.isEmpty())
			{
				for (ScheduledFuture<?> task : tasks)
				{
					if ((task == null) || task.isDone() || task.isCancelled())
					{
						tasks.remove(task);
					}
				}
			}
		}
		else // Parse files sequentially if multithreading is not enabled.
		{
			for (File file : _itemFiles)
			{
				final DocumentItem document = new DocumentItem(file);
				document.parse();
				items.addAll(document.getItemList());
			}
		}
		
		// Process each loaded item and organize them into their respective collections.
		for (ItemTemplate item : items)
		{
			if (highestId < item.getId())
			{
				highestId = item.getId();
			}
			
			if (item instanceof EtcItem)
			{
				_etcItems.put(item.getId(), (EtcItem) item);
			}
			else if (item instanceof Armor)
			{
				_armors.put(item.getId(), (Armor) item);
			}
			else
			{
				_weapons.put(item.getId(), (Weapon) item);
			}
		}
		
		// Log the highest item ID and create a fast lookup array based on this value.
		LOGGER.info(getClass().getSimpleName() + ": Highest item id used: " + highestId);
		_allTemplates = new ItemTemplate[highestId + 1];
		
		// Populate the fast lookup table with items from each category.
		for (Armor item : _armors.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		for (Weapon item : _weapons.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		for (EtcItem item : _etcItems.values())
		{
			_allTemplates[item.getId()] = item;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _etcItems.size() + " etc items.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _armors.size() + " armor items.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _weapons.size() + " weapon items.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (_etcItems.size() + _armors.size() + _weapons.size()) + " items in total.");
	}
	
	public void reload()
	{
		load();
		EnchantItemHPBonusData.getInstance().load();
	}
	
	/**
	 * Returns the item template corresponding to the specified item ID.
	 * @param id the ID of the item to retrieve
	 * @return the {@link ItemTemplate} associated with the specified ID, or {@code null} if the ID is out of bounds
	 */
	public ItemTemplate getTemplate(int id)
	{
		if ((id >= _allTemplates.length) || (id < 0))
		{
			return null;
		}
		
		return _allTemplates[id];
	}
	
	/**
	 * Returns a set of all armor item IDs available in the collection.
	 * @return a {@link Set} of integers representing the IDs of all armors
	 */
	public Set<Integer> getAllArmorsId()
	{
		return _armors.keySet();
	}
	
	/**
	 * Returns a collection of all armor items.
	 * @return a {@link Collection} of {@link Armor} objects representing all loaded armor items
	 */
	public Collection<Armor> getAllArmors()
	{
		return _armors.values();
	}
	
	/**
	 * Returns a set of all weapon item IDs available in the collection.
	 * @return a {@link Set} of integers representing the IDs of all weapons
	 */
	public Set<Integer> getAllWeaponsId()
	{
		return _weapons.keySet();
	}
	
	/**
	 * Returns a collection of all weapon items.
	 * @return a {@link Collection} of {@link Weapon} objects representing all loaded weapon items
	 */
	public Collection<Weapon> getAllWeapons()
	{
		return _weapons.values();
	}
	
	/**
	 * Returns a set of all miscellaneous item IDs (non-armor, non-weapon items) available in the collection.
	 * @return a {@link Set} of integers representing the IDs of all miscellaneous items
	 */
	public Set<Integer> getAllEtcItemsId()
	{
		return _etcItems.keySet();
	}
	
	/**
	 * Returns a collection of all miscellaneous items (non-armor, non-weapon items).
	 * @return a {@link Collection} of {@link EtcItem} objects representing all loaded miscellaneous items
	 */
	public Collection<EtcItem> getAllEtcItems()
	{
		return _etcItems.values();
	}
	
	/**
	 * Returns an array of all item templates, indexed by item ID for fast lookup.
	 * @return an array of {@link ItemTemplate} objects, where the index corresponds to the item ID
	 */
	public ItemTemplate[] getAllItems()
	{
		return _allTemplates;
	}
	
	/**
	 * Returns the size of the item template array.
	 * @return the length of the {@link ItemTemplate} array, representing the maximum item ID + 1
	 */
	public int getArraySize()
	{
		return _allTemplates.length;
	}
	
	public static ItemData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemData INSTANCE = new ItemData();
	}
}
