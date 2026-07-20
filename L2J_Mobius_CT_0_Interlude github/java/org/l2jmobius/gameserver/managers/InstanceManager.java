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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceWorld;

/**
 * Instance manager.
 * @author evill33t, GodKratos, Mobius
 */
public class InstanceManager implements IXmlReader
{
	private static final Map<Integer, Instance> INSTANCES = new ConcurrentHashMap<>();
	private final Map<Integer, InstanceWorld> _instanceWorlds = new ConcurrentHashMap<>();
	private int _dynamic = 300000;
	
	// InstanceId Names
	private static final Map<Integer, String> _instanceIdNames = new HashMap<>();
	
	// Instance templates
	private final Map<Integer, String> _instanceTemplates = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, Long>> _playerTimes = new ConcurrentHashMap<>();
	
	// SQL Queries
	private static final String ADD_INSTANCE_TIME = "INSERT INTO character_instance_time (charId,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?";
	private static final String RESTORE_INSTANCE_TIMES = "SELECT instanceId,time FROM character_instance_time WHERE charId=?";
	private static final String DELETE_INSTANCE_TIME = "DELETE FROM character_instance_time WHERE charId=? AND instanceId=?";
	
	protected InstanceManager()
	{
		// Creates the multiverse.
		INSTANCES.put(-1, new Instance(-1));
		LOGGER.info(getClass().getSimpleName() + ": Multiverse Instance created.");
		
		// Creates the universe.
		INSTANCES.put(0, new Instance(0));
		LOGGER.info(getClass().getSimpleName() + ": Universe Instance created.");
		load();
	}
	
	@Override
	public void load()
	{
		_instanceIdNames.clear();
		final File nameFile = new File("data/InstanceNames.xml");
		if (nameFile.exists())
		{
			parseDatapackFile("data/InstanceNames.xml");
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _instanceIdNames.size() + " instance names.");
		}
		
		// Load instance templates
		_instanceTemplates.clear();
		final File instanceFolder = new File("data/instances");
		if (instanceFolder.exists())
		{
			parseDatapackDirectory("data/instances", true);
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _instanceTemplates.size() + " instance templates.");
		}
	}
	
	/**
	 * @param playerObjId
	 * @param id
	 * @return
	 */
	public long getInstanceTime(int playerObjId, int id)
	{
		if (!_playerTimes.containsKey(playerObjId))
		{
			restoreInstanceTimes(playerObjId);
		}
		
		if (_playerTimes.get(playerObjId).containsKey(id))
		{
			return _playerTimes.get(playerObjId).get(id);
		}
		
		return -1;
	}
	
	/**
	 * @param playerObjId
	 * @return
	 */
	public Map<Integer, Long> getAllInstanceTimes(int playerObjId)
	{
		if (!_playerTimes.containsKey(playerObjId))
		{
			restoreInstanceTimes(playerObjId);
		}
		
		return _playerTimes.get(playerObjId);
	}
	
	/**
	 * @param playerObjId
	 * @param id
	 * @param time
	 */
	public void setInstanceTime(int playerObjId, int id, long time)
	{
		if (!_playerTimes.containsKey(playerObjId))
		{
			restoreInstanceTimes(playerObjId);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(ADD_INSTANCE_TIME))
		{
			ps.setInt(1, playerObjId);
			ps.setInt(2, id);
			ps.setLong(3, time);
			ps.setLong(4, time);
			ps.execute();
			_playerTimes.get(playerObjId).put(id, time);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not insert character instance time data: " + e.getMessage());
		}
	}
	
	/**
	 * @param playerObjId
	 * @param id
	 */
	public void deleteInstanceTime(int playerObjId, int id)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_INSTANCE_TIME))
		{
			ps.setInt(1, playerObjId);
			ps.setInt(2, id);
			ps.execute();
			_playerTimes.get(playerObjId).remove(id);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not delete character instance time data: " + e.getMessage());
		}
	}
	
	/**
	 * @param playerObjId
	 */
	public void restoreInstanceTimes(int playerObjId)
	{
		if (_playerTimes.containsKey(playerObjId))
		{
			return; // already restored
		}
		
		_playerTimes.put(playerObjId, new ConcurrentHashMap<>());
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESTORE_INSTANCE_TIMES))
		{
			ps.setInt(1, playerObjId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int id = rs.getInt("instanceId");
					final long time = rs.getLong("time");
					if (time < System.currentTimeMillis())
					{
						deleteInstanceTime(playerObjId, id);
					}
					else
					{
						_playerTimes.get(playerObjId).put(id, time);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not delete character instance time data: " + e.getMessage());
		}
	}
	
	/**
	 * @param id
	 * @return
	 */
	public String getInstanceIdName(int id)
	{
		if (_instanceIdNames.containsKey(id))
		{
			return _instanceIdNames.get(id);
		}
		
		return ("UnknownInstance");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			switch (n.getNodeName())
			{
				case "list":
				{
					NamedNodeMap attrs;
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("instance".equals(d.getNodeName()))
						{
							attrs = d.getAttributes();
							_instanceIdNames.put(parseInteger(attrs, "id"), attrs.getNamedItem("name").getNodeValue());
						}
					}
					break;
				}
				case "instance":
				{
					final NamedNodeMap attrs = n.getAttributes();
					_instanceTemplates.put(parseInteger(attrs, "id"), file.getPath().substring(file.getPath().lastIndexOf(File.separator + "instances" + File.separator) + 11));
					break;
				}
			}
		}
	}
	
	/**
	 * @param world
	 */
	public void addWorld(InstanceWorld world)
	{
		_instanceWorlds.put(world.getInstanceId(), world);
	}
	
	/**
	 * @param instanceId
	 * @return
	 */
	public InstanceWorld getWorld(int instanceId)
	{
		return _instanceWorlds.get(instanceId);
	}
	
	/**
	 * @param object
	 * @return the InstanceWorld of the object
	 */
	public InstanceWorld getWorld(WorldObject object)
	{
		return _instanceWorlds.get(object.getInstanceId());
	}
	
	/**
	 * Check if the player have a World Instance where it's allowed to enter.
	 * @param player the player to check
	 * @return the instance world
	 */
	public InstanceWorld getPlayerWorld(Player player)
	{
		for (InstanceWorld temp : _instanceWorlds.values())
		{
			if ((temp != null) && (temp.isAllowed(player)))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	/**
	 * @param instanceid
	 */
	public void destroyInstance(int instanceid)
	{
		if (instanceid <= 0)
		{
			return;
		}
		
		final Instance temp = INSTANCES.get(instanceid);
		if (temp != null)
		{
			temp.removeNpcs();
			temp.removePlayers();
			temp.removeDoors();
			temp.cancelTimer();
			INSTANCES.remove(instanceid);
			_instanceWorlds.remove(instanceid);
		}
	}
	
	/**
	 * @param instanceid
	 * @return
	 */
	public Instance getInstance(int instanceid)
	{
		return INSTANCES.get(instanceid);
	}
	
	/**
	 * @return
	 */
	public Map<Integer, Instance> getInstances()
	{
		return INSTANCES;
	}
	
	/**
	 * @param objectId
	 * @return
	 */
	public int getPlayer(int objectId)
	{
		for (Instance temp : INSTANCES.values())
		{
			if (temp == null)
			{
				continue;
			}
			
			// check if the player is in any active instance
			if (temp.containsPlayer(objectId))
			{
				return temp.getId();
			}
		}
		
		// 0 is default instance aka the world
		return 0;
	}
	
	/**
	 * @param id
	 * @param templateId
	 * @return
	 */
	public boolean createInstanceFromTemplate(int id, int templateId)
	{
		if (getInstance(id) != null)
		{
			return false;
		}
		
		final Instance instance = new Instance(id);
		INSTANCES.put(id, instance);
		instance.loadInstanceTemplate(templateId);
		instance.spawnDoors();
		instance.spawnGroup("general");
		return true;
	}
	
	/**
	 * Create a new instance with a dynamic instance id based on a template (or null)
	 * @param templateId the instance template id
	 * @return
	 */
	public synchronized Instance createDynamicInstance(int templateId)
	{
		while (getInstance(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				LOGGER.warning(getClass().getSimpleName() + ": More then " + (Integer.MAX_VALUE - 300000) + " instances created");
				_dynamic = 300000;
			}
		}
		
		final Instance instance = new Instance(_dynamic);
		INSTANCES.put(_dynamic, instance);
		if (templateId > 0)
		{
			instance.loadInstanceTemplate(templateId);
			instance.spawnDoors();
			instance.spawnGroup("general");
		}
		
		return instance;
	}
	
	/**
	 * Get instance template file name by template ID.
	 * @param id template id of instance
	 * @return instance template if found, otherwise {@code null}
	 */
	public String getInstanceTemplateFileName(int id)
	{
		return _instanceTemplates.get(id);
	}
	
	/**
	 * Gets the single instance of {@code InstanceManager}.
	 * @return single instance of {@code InstanceManager}
	 */
	public static InstanceManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InstanceManager INSTANCE = new InstanceManager();
	}
}
