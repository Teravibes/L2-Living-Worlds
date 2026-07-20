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
package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.scripting.ScriptEngine;

public class ScriptManager
{
	private static final Logger LOGGER = Logger.getLogger(ScriptManager.class.getName());
	
	/** Map containing all the quests. */
	private final Map<String, Quest> _quests = new ConcurrentHashMap<>();
	/** Map containing all the scripts. */
	private final Map<String, Quest> _scripts = new ConcurrentHashMap<>();
	
	protected ScriptManager()
	{
	}
	
	public void reload(String questFolder)
	{
		final Quest q = getScript(questFolder);
		if (q != null)
		{
			q.reload();
		}
	}
	
	/**
	 * Reloads a the quest by ID.
	 * @param questId the ID of the quest to be reloaded
	 */
	public void reload(int questId)
	{
		final Quest q = getQuest(questId);
		if (q != null)
		{
			q.reload();
		}
	}
	
	/**
	 * Unload all quests and scripts and reload them.
	 */
	public void reloadAllScripts()
	{
		unloadAllScripts();
		
		LOGGER.info("Reloading all server scripts.");
		
		try
		{
			ScriptEngine.getInstance().executeScriptList();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed executing script list!", e);
		}
		
		getInstance().report();
	}
	
	/**
	 * Unload all quests and scripts.
	 */
	public void unloadAllScripts()
	{
		LOGGER.info("Unloading all server scripts.");
		
		// Unload quests.
		for (Quest quest : _quests.values())
		{
			if (quest != null)
			{
				quest.unload(false);
			}
		}
		
		_quests.clear();
		
		// Unload scripts.
		for (Quest script : _scripts.values())
		{
			if (script != null)
			{
				script.unload(false);
			}
		}
		
		_scripts.clear();
	}
	
	/**
	 * Logs how many quests and scripts are loaded.
	 */
	public void report()
	{
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _quests.size() + " quests.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _scripts.size() + " scripts.");
	}
	
	/**
	 * Calls {@link Quest#onSave()} in all quests and scripts.
	 */
	public void save()
	{
		// Save quests.
		for (Quest quest : _quests.values())
		{
			quest.onSave();
		}
		
		// Save scripts.
		for (Quest script : _scripts.values())
		{
			script.onSave();
		}
	}
	
	/**
	 * Retrieves a Quest object by its name from either the _quests or _scripts map.<br>
	 * This method first checks the _quests map for the specified name.<br>
	 * If not found, it falls back to checking the _scripts map.
	 * @param name the name of the quest/script to retrieve
	 * @return the Quest object associated with the given name if found in either map, or null if the name is not present in either map
	 */
	public Quest getScript(String name)
	{
		if (_quests.containsKey(name))
		{
			return _quests.get(name);
		}
		
		return _scripts.get(name);
	}
	
	/**
	 * Gets a quest by ID.
	 * @param questId the ID of the quest to get
	 * @return if found, the quest, {@code null} otherwise
	 */
	public Quest getQuest(int questId)
	{
		for (Quest q : _quests.values())
		{
			if (q.getId() == questId)
			{
				return q;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a new quest.
	 * @param quest the quest to be added
	 */
	public void addQuest(Quest quest)
	{
		if (quest == null)
		{
			throw new IllegalArgumentException("Quest argument cannot be null");
		}
		
		// FIXME: unloading the old quest at this point is a tad too late.
		// the new quest has already initialized itself and read the data, starting
		// an unpredictable number of tasks with that data. The old quest will now
		// save data which will never be read.
		// However, requesting the newQuest to re-read the data is not necessarily a
		// good option, since the newQuest may have already started timers, spawned NPCs
		// or taken any other action which it might re-take by re-reading the data.
		// the current solution properly closes the running tasks of the old quest but
		// ignores the data; perhaps the least of all evils...
		final Quest old = _quests.put(quest.getName(), quest);
		if (old != null)
		{
			old.unload();
			LOGGER.info("Replaced quest " + old.getName() + " (" + old.getId() + ") with a new version!");
		}
		
		if (DevelopmentConfig.SHOW_QUEST_LOAD_IN_LOGS)
		{
			final String questName = quest.getName().contains("_") ? quest.getName().substring(quest.getName().indexOf('_') + 1) : quest.getName();
			LOGGER.info("Loaded quest " + StringUtil.separateWords(questName) + ".");
		}
	}
	
	/**
	 * Removes a script.
	 * @param script the script to remove
	 * @return {@code true} if the script was removed, {@code false} otherwise
	 */
	public boolean removeScript(Quest script)
	{
		if (_quests.containsKey(script.getName()))
		{
			_quests.remove(script.getName());
			return true;
		}
		else if (_scripts.containsKey(script.getName()))
		{
			_scripts.remove(script.getName());
			return true;
		}
		
		return false;
	}
	
	public Map<String, Quest> getQuests()
	{
		return _quests;
	}
	
	public boolean unload(Quest ms)
	{
		ms.onSave();
		return removeScript(ms);
	}
	
	/**
	 * Gets all the registered scripts.
	 * @return all the scripts
	 */
	public Map<String, Quest> getScripts()
	{
		return _scripts;
	}
	
	/**
	 * Adds a script.
	 * @param script the script to be added
	 */
	public void addScript(Quest script)
	{
		final Quest old = _scripts.put(script.getClass().getSimpleName(), script);
		if (old != null)
		{
			old.unload();
			LOGGER.info("Replaced script " + old.getName() + " with a new version!");
		}
		
		if (DevelopmentConfig.SHOW_SCRIPT_LOAD_IN_LOGS)
		{
			LOGGER.info("Loaded script " + StringUtil.separateWords(script.getClass().getSimpleName()) + ".");
		}
	}
	
	/**
	 * Gets the single instance of {@code ScriptManager}.
	 * @return single instance of {@code ScriptManager}
	 */
	public static ScriptManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptManager INSTANCE = new ScriptManager();
	}
}
