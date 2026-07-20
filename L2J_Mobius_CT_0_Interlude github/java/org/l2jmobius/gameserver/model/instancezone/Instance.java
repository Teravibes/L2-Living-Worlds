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
package org.l2jmobius.gameserver.model.instancezone;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.InstanceReenterTimeHolder;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Main class for game instances.
 * @author evill33t, GodKratos
 */
public class Instance
{
	private static final Logger LOGGER = Logger.getLogger(Instance.class.getName());
	
	private final int _id;
	private int _templateId = -1;
	private int _ejectTime = GeneralConfig.EJECT_DEAD_PLAYER_TIME;
	/** Allow random walk for NPCs, global parameter. */
	private boolean _allowRandomWalk = true;
	private final Collection<Integer> _players = ConcurrentHashMap.newKeySet();
	private final Collection<Npc> _npcs = ConcurrentHashMap.newKeySet();
	private final Collection<StatSet> _doorTemplates = ConcurrentHashMap.newKeySet();
	private final Map<Integer, Door> _doors = new ConcurrentHashMap<>();
	private final Collection<StatSet> _spawnTemplates = ConcurrentHashMap.newKeySet();
	private Collection<Location> _enterLocations = ConcurrentHashMap.newKeySet();
	private Location _exitLocation = null;
	private boolean _allowSummon = true;
	private long _emptyDestroyTime = -1;
	private long _lastLeft = -1;
	private long _instanceStartTime = -1;
	private long _instanceEndTime = -1;
	private boolean _isPvP = false;
	private boolean _showTimer = false;
	private boolean _isTimerIncrease = true;
	private String _timerText = "";
	
	// Instance reset data
	private InstanceReenterType _type = InstanceReenterType.NONE;
	private final List<InstanceReenterTimeHolder> _resetData = new ArrayList<>();
	
	// Instance remove buffs data
	private InstanceRemoveBuffType _removeBuffType = InstanceRemoveBuffType.NONE;
	private final List<Integer> _exceptionList = new ArrayList<>();
	
	protected ScheduledFuture<?> _checkTimeUpTask = null;
	protected final Map<Integer, ScheduledFuture<?>> _ejectDeadTasks = new ConcurrentHashMap<>();
	
	public Instance(int id)
	{
		_id = id;
		_instanceStartTime = System.currentTimeMillis();
	}
	
	/**
	 * @return the ID of this instance.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Get template ID of instance world.
	 * @return instance template ID
	 */
	public int getTemplateId()
	{
		return _templateId;
	}
	
	/**
	 * @return the eject time
	 */
	public int getEjectTime()
	{
		return _ejectTime;
	}
	
	/**
	 * @param ejectTime the player eject time upon death
	 */
	public void setEjectTime(int ejectTime)
	{
		_ejectTime = ejectTime;
	}
	
	/**
	 * @return whether summon friend type skills are allowed for this instance
	 */
	public boolean isSummonAllowed()
	{
		return _allowSummon;
	}
	
	/**
	 * Sets the status for the instance for summon friend type skills
	 * @param value
	 */
	public void setAllowSummon(boolean value)
	{
		_allowSummon = value;
	}
	
	/**
	 * Returns true if entire instance is PvP zone
	 * @return
	 */
	public boolean isPvP()
	{
		return _isPvP;
	}
	
	/**
	 * Sets PvP zone status of the instance
	 * @param value
	 */
	public void setPvP(boolean value)
	{
		_isPvP = value;
	}
	
	/**
	 * Set the instance duration task
	 * @param duration in milliseconds
	 */
	public void setDuration(int duration)
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
		
		_checkTimeUpTask = ThreadPool.schedule(new CheckTimeUp(duration), 500);
		_instanceEndTime = System.currentTimeMillis() + duration + 500;
	}
	
	/**
	 * Set time before empty instance will be removed
	 * @param time in milliseconds
	 */
	public void setEmptyDestroyTime(long time)
	{
		_emptyDestroyTime = time;
	}
	
	/**
	 * Checks if the player exists within this instance
	 * @param objectId
	 * @return true if player exists in instance
	 */
	public boolean containsPlayer(int objectId)
	{
		return _players.contains(objectId);
	}
	
	/**
	 * Adds the specified player to the instance
	 * @param objectId Players object ID
	 */
	public void addPlayer(int objectId)
	{
		_players.add(objectId);
	}
	
	/**
	 * Removes the specified player from the instance list.
	 * @param objectId the player's object Id
	 */
	public void removePlayer(Integer objectId)
	{
		_players.remove(objectId);
		if (!_players.isEmpty() || (_emptyDestroyTime < 0))
		{
			return;
		}
		
		_lastLeft = System.currentTimeMillis();
		setDuration((int) (_instanceEndTime - System.currentTimeMillis() - 500));
	}
	
	public void addNpc(Npc npc)
	{
		_npcs.add(npc);
	}
	
	public void removeNpc(Npc npc)
	{
		if (npc.getSpawn() != null)
		{
			npc.getSpawn().stopRespawn();
		}
		
		_npcs.remove(npc);
	}
	
	/**
	 * Adds a door into the instance
	 * @param set - StatSet for initializing door
	 */
	public void addDoor(StatSet set)
	{
		if (_doorTemplates.contains(set))
		{
			LOGGER.warning("Door ID " + set.getInt("DoorId") + " already exists in instance " + _id);
			return;
		}
		
		_doorTemplates.add(set);
	}
	
	/**
	 * Spawn doors inside instance world.
	 */
	public void spawnDoors()
	{
		for (StatSet template : _doorTemplates)
		{
			// Create new door instance
			final int doorId = template.getInt("DoorId");
			final StatSet doorTemplate = DoorData.getInstance().getDoorTemplate(doorId);
			final Door newdoor = new Door(new DoorTemplate(doorTemplate));
			newdoor.setInstanceId(_id);
			newdoor.setCurrentHp(newdoor.getMaxHp());
			newdoor.spawnMe(newdoor.getTemplate().getX(), newdoor.getTemplate().getY(), newdoor.getTemplate().getZ());
			_doors.put(doorId, newdoor);
		}
	}
	
	public Collection<Integer> getPlayers()
	{
		return _players;
	}
	
	public Collection<Npc> getNpcs()
	{
		return _npcs;
	}
	
	public Collection<Door> getDoors()
	{
		return _doors.values();
	}
	
	public Door getDoor(int id)
	{
		return _doors.get(id);
	}
	
	public long getInstanceEndTime()
	{
		return _instanceEndTime;
	}
	
	public long getInstanceStartTime()
	{
		return _instanceStartTime;
	}
	
	public boolean isShowTimer()
	{
		return _showTimer;
	}
	
	public boolean isTimerIncrease()
	{
		return _isTimerIncrease;
	}
	
	public String getTimerText()
	{
		return _timerText;
	}
	
	/**
	 * @return the spawn location for this instance to be used when enter in instance
	 */
	public Collection<Location> getEnterLocs()
	{
		return _enterLocations;
	}
	
	/**
	 * Sets the spawn location for this instance to be used when enter in instance
	 * @param loc
	 */
	public void addEnterLoc(Location loc)
	{
		_enterLocations.add(loc);
	}
	
	/**
	 * @return the spawn location for this instance to be used when leaving the instance
	 */
	public Location getExitLoc()
	{
		return _exitLocation;
	}
	
	/**
	 * Sets the spawn location for this instance to be used when leaving the instance
	 * @param loc
	 */
	public void setExitLoc(Location loc)
	{
		_exitLocation = loc;
	}
	
	public void removePlayers()
	{
		for (Integer objectId : _players)
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if ((player != null) && (player.getInstanceId() == _id))
			{
				player.setInstanceId(0);
				if (_exitLocation != null)
				{
					player.teleToLocation(_exitLocation, true);
				}
				else
				{
					player.teleToLocation(TeleportWhereType.TOWN);
				}
			}
		}
		
		_players.clear();
	}
	
	public void removeNpcs()
	{
		for (Npc mob : _npcs)
		{
			if (mob != null)
			{
				if (mob.getSpawn() != null)
				{
					mob.getSpawn().stopRespawn();
				}
				
				mob.deleteMe();
			}
		}
		
		_npcs.clear();
	}
	
	public void removeDoors()
	{
		for (Door door : _doors.values())
		{
			if (door != null)
			{
				final WorldRegion region = door.getWorldRegion();
				door.decayMe();
				
				if (region != null)
				{
					region.removeVisibleObject(door);
				}
				
				World.getInstance().removeObject(door);
			}
		}
		
		_doors.clear();
	}
	
	/**
	 * Spawns group of instance NPCs
	 * @param groupName - name of group from XML definition to spawn
	 * @return list of spawned NPCs
	 */
	public List<Npc> spawnGroup(String groupName)
	{
		final List<Npc> spawnedNpcs = new ArrayList<>();
		for (StatSet set : _spawnTemplates)
		{
			if (set.getString("spawnGroup").equals(groupName))
			{
				try
				{
					final Spawn spawnDat = new Spawn(set.getInt("npcId"));
					spawnDat.setXYZ(set.getInt("x"), set.getInt("y"), set.getInt("z"));
					spawnDat.setAmount(1);
					spawnDat.setHeading(set.getInt("heading"));
					spawnDat.setRespawnDelay(set.getInt("respawn"), set.getInt("respawnRandom"));
					spawnDat.setInstanceId(_id);
					spawnDat.setRandomWalking(set.getBoolean("allowRandomWalk"));
					final Npc spawned = spawnDat.doSpawn(false);
					if ((set.getInt("delay") >= 0) && spawned.isAttackable())
					{
						spawned.asAttackable().setOnKillDelay(set.getInt("delay"));
					}
					
					if (set.getInt("respawn") == 0)
					{
						spawnDat.stopRespawn();
					}
					else
					{
						spawnDat.startRespawn();
					}
					
					spawnedNpcs.add(spawned);
				}
				catch (Exception e)
				{
					LOGGER.warning("Problem with Instance: " + e.toString());
				}
			}
		}
		
		return spawnedNpcs;
	}
	
	public void loadInstanceTemplate(int templateId)
	{
		// TODO: Cache templates.
		Document document = null;
		final File xml = new File("." + File.separator + "data" + File.separator + "instances" + File.separator + InstanceManager.getInstance().getInstanceTemplateFileName(templateId));
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			document = factory.newDocumentBuilder().parse(xml);
			for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("instance".equalsIgnoreCase(n.getNodeName()))
				{
					parseInstance(n);
				}
			}
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Instance: can not find " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Instance: error while loading " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
	}
	
	private void parseInstance(Node node) throws Exception
	{
		Node n = node;
		_templateId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		Node a = n.getAttributes().getNamedItem("ejectTime");
		if (a != null)
		{
			_ejectTime = 1000 * Integer.parseInt(a.getNodeValue());
		}
		
		a = n.getAttributes().getNamedItem("allowRandomWalk");
		if (a != null)
		{
			_allowRandomWalk = Boolean.parseBoolean(a.getNodeValue());
		}
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			switch (n.getNodeName().toLowerCase())
			{
				case "activitytime":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						_checkTimeUpTask = ThreadPool.schedule(new CheckTimeUp(Integer.parseInt(a.getNodeValue()) * 60000), 15000);
						_instanceEndTime = System.currentTimeMillis() + (Long.parseLong(a.getNodeValue()) * 60000) + 15000;
					}
					break;
				}
				case "allowsummon":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						setAllowSummon(Boolean.parseBoolean(a.getNodeValue()));
					}
					break;
				}
				case "emptydestroytime":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						_emptyDestroyTime = Long.parseLong(a.getNodeValue()) * 1000;
					}
					break;
				}
				case "showtimer":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						_showTimer = Boolean.parseBoolean(a.getNodeValue());
					}
					
					a = n.getAttributes().getNamedItem("increase");
					if (a != null)
					{
						_isTimerIncrease = Boolean.parseBoolean(a.getNodeValue());
					}
					
					a = n.getAttributes().getNamedItem("text");
					if (a != null)
					{
						_timerText = a.getNodeValue();
					}
					break;
				}
				case "pvpinstance":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						setPvP(Boolean.parseBoolean(a.getNodeValue()));
					}
					break;
				}
				case "doorlist":
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						int doorId = 0;
						if ("door".equalsIgnoreCase(d.getNodeName()))
						{
							doorId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
							final StatSet set = new StatSet();
							set.set("DoorId", doorId);
							for (Node bean = d.getFirstChild(); bean != null; bean = bean.getNextSibling())
							{
								if ("set".equalsIgnoreCase(bean.getNodeName()))
								{
									final NamedNodeMap attrs = bean.getAttributes();
									final String setname = attrs.getNamedItem("name").getNodeValue();
									final String value = attrs.getNamedItem("val").getNodeValue();
									set.set(setname, value);
								}
							}
							
							addDoor(set);
						}
					}
					break;
				}
				case "spawnlist":
				{
					for (Node group = n.getFirstChild(); group != null; group = group.getNextSibling())
					{
						if ("group".equalsIgnoreCase(group.getNodeName()))
						{
							final String spawnGroup = group.getAttributes().getNamedItem("name").getNodeValue();
							for (Node d = group.getFirstChild(); d != null; d = d.getNextSibling())
							{
								int npcId = 0;
								int x = 0;
								int y = 0;
								int z = 0;
								int heading = 0;
								int respawn = 0;
								int respawnRandom = 0;
								int delay = -1;
								Boolean allowRandomWalk = null;
								if ("npc".equalsIgnoreCase(d.getNodeName()))
								{
									npcId = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
									x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
									y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
									z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
									if (d.getAttributes().getNamedItem("heading") != null)
									{
										heading = Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue());
									}
									
									if (d.getAttributes().getNamedItem("respawn") != null)
									{
										respawn = Integer.parseInt(d.getAttributes().getNamedItem("respawn").getNodeValue());
									}
									
									if (d.getAttributes().getNamedItem("onKillDelay") != null)
									{
										delay = Integer.parseInt(d.getAttributes().getNamedItem("onKillDelay").getNodeValue());
									}
									
									if (d.getAttributes().getNamedItem("respawnRandom") != null)
									{
										respawnRandom = Integer.parseInt(d.getAttributes().getNamedItem("respawnRandom").getNodeValue());
									}
									
									if (d.getAttributes().getNamedItem("allowRandomWalk") != null)
									{
										allowRandomWalk = Boolean.parseBoolean(d.getAttributes().getNamedItem("allowRandomWalk").getNodeValue());
									}
									
									final StatSet spawnSet = new StatSet();
									spawnSet.set("spawnGroup", spawnGroup);
									spawnSet.set("npcId", npcId);
									spawnSet.set("x", x);
									spawnSet.set("y", y);
									spawnSet.set("z", z);
									spawnSet.set("heading", heading);
									spawnSet.set("delay", delay);
									spawnSet.set("respawn", respawn);
									spawnSet.set("respawnRandom", respawnRandom);
									if (allowRandomWalk == null)
									{
										spawnSet.set("allowRandomWalk", !_allowRandomWalk);
									}
									else
									{
										spawnSet.set("allowRandomWalk", !allowRandomWalk);
									}
									
									_spawnTemplates.add(spawnSet);
								}
							}
						}
					}
					break;
				}
				case "exitpoint":
				{
					final int x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
					final int y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
					final int z = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());
					_exitLocation = new Location(x, y, z);
					break;
				}
				case "spawnpoints":
				{
					_enterLocations = new ArrayList<>();
					for (Node loc = n.getFirstChild(); loc != null; loc = loc.getNextSibling())
					{
						if (loc.getNodeName().equals("Location"))
						{
							try
							{
								final int x = Integer.parseInt(loc.getAttributes().getNamedItem("x").getNodeValue());
								final int y = Integer.parseInt(loc.getAttributes().getNamedItem("y").getNodeValue());
								final int z = Integer.parseInt(loc.getAttributes().getNamedItem("z").getNodeValue());
								_enterLocations.add(new Location(x, y, z));
							}
							catch (Exception e)
							{
								LOGGER.log(Level.WARNING, "Error parsing instance xml: " + e.getMessage(), e);
							}
						}
					}
					break;
				}
				case "reenter":
				{
					a = n.getAttributes().getNamedItem("additionStyle");
					if (a != null)
					{
						_type = InstanceReenterType.valueOf(a.getNodeValue());
					}
					
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						long time = -1;
						DayOfWeek day = null;
						int hour = -1;
						int minute = -1;
						if ("reset".equalsIgnoreCase(d.getNodeName()))
						{
							a = d.getAttributes().getNamedItem("time");
							if (a != null)
							{
								time = Long.parseLong(a.getNodeValue());
								if (time > 0)
								{
									_resetData.add(new InstanceReenterTimeHolder(time));
									break;
								}
							}
							else if (time == -1)
							{
								a = d.getAttributes().getNamedItem("day");
								if (a != null)
								{
									day = DayOfWeek.valueOf(a.getNodeValue().toUpperCase());
								}
								
								a = d.getAttributes().getNamedItem("hour");
								if (a != null)
								{
									hour = Integer.parseInt(a.getNodeValue());
								}
								
								a = d.getAttributes().getNamedItem("minute");
								if (a != null)
								{
									minute = Integer.parseInt(a.getNodeValue());
								}
								
								_resetData.add(new InstanceReenterTimeHolder(day, hour, minute));
							}
						}
					}
					break;
				}
				case "removebuffs":
				{
					a = n.getAttributes().getNamedItem("type");
					if (a != null)
					{
						_removeBuffType = InstanceRemoveBuffType.valueOf(a.getNodeValue().toUpperCase());
					}
					
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("skill".equalsIgnoreCase(d.getNodeName()))
						{
							a = d.getAttributes().getNamedItem("id");
							if (a != null)
							{
								_exceptionList.add(Integer.parseInt(a.getNodeValue()));
							}
						}
					}
					break;
				}
			}
		}
	}
	
	protected void doCheckTimeUp(int value)
	{
		CreatureSay cs = null;
		int timeLeft;
		int interval;
		int remaining = value;
		if (_players.isEmpty() && (_emptyDestroyTime == 0))
		{
			remaining = 0;
			interval = 500;
		}
		else if (_players.isEmpty() && (_emptyDestroyTime > 0))
		{
			final Long emptyTimeLeft = (_lastLeft + _emptyDestroyTime) - System.currentTimeMillis();
			if (emptyTimeLeft <= 0)
			{
				interval = 0;
				remaining = 0;
			}
			else if ((remaining > 300000) && (emptyTimeLeft > 300000))
			{
				interval = 300000;
				remaining -= 300000;
			}
			else if ((remaining > 60000) && (emptyTimeLeft > 60000))
			{
				interval = 60000;
				remaining -= 60000;
			}
			else if ((remaining > 30000) && (emptyTimeLeft > 30000))
			{
				interval = 30000;
				remaining -= 30000;
			}
			else
			{
				interval = 10000;
				remaining -= 10000;
			}
		}
		else if (remaining > 300000)
		{
			timeLeft = remaining / 60000;
			interval = 300000;
			Broadcast.toPlayersInInstance("This dungeon will expire in " + timeLeft + " minute(s). You will be forced out of the dungeon when the time expires.", _id);
			remaining -= 300000;
		}
		else if (remaining > 60000)
		{
			timeLeft = remaining / 60000;
			interval = 60000;
			Broadcast.toPlayersInInstance("This dungeon will expire in " + timeLeft + " minute(s). You will be forced out of the dungeon when the time expires.", _id);
			remaining -= 60000;
		}
		else if (remaining > 30000)
		{
			timeLeft = remaining / 1000;
			interval = 30000;
			cs = new CreatureSay(null, ChatType.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining -= 30000;
		}
		else
		{
			timeLeft = remaining / 1000;
			interval = 10000;
			cs = new CreatureSay(null, ChatType.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining -= 10000;
		}
		
		if (cs != null)
		{
			for (Integer objectId : _players)
			{
				final Player player = World.getInstance().getPlayer(objectId);
				if ((player != null) && (player.getInstanceId() == _id))
				{
					player.sendPacket(cs);
				}
			}
		}
		
		cancelTimer();
		if (remaining >= 10000)
		{
			_checkTimeUpTask = ThreadPool.schedule(new CheckTimeUp(remaining), interval);
		}
		else
		{
			_checkTimeUpTask = ThreadPool.schedule(new TimeUp(), interval);
		}
	}
	
	public void cancelTimer()
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
	}
	
	public void cancelEjectDeadPlayer(Player player)
	{
		final ScheduledFuture<?> task = _ejectDeadTasks.remove(player.getObjectId());
		if (task != null)
		{
			task.cancel(true);
		}
	}
	
	/**
	 * This method is called when player dies inside instance.
	 * @param player
	 */
	public void notifyDeath(Player player)
	{
		if (!player.isOnEvent() && (_ejectTime > 0))
		{
			// Proper system message doesn't exist in epilogue client.
			player.sendMessage("If you are not resurrected within " + (_ejectTime / 1000 / 60) + " minutes, you will be expelled from the instance zone.");
			
			// Start eject task
			_ejectDeadTasks.put(player.getObjectId(), ThreadPool.schedule(() ->
			{
				if (player.isDead() && (player.getInstanceId() == _id))
				{
					player.setInstanceId(0);
					if (_exitLocation != null)
					{
						player.teleToLocation(_exitLocation, true);
					}
					else
					{
						player.teleToLocation(TeleportWhereType.TOWN);
					}
				}
			}, _ejectTime));
		}
	}
	
	public class CheckTimeUp implements Runnable
	{
		private final int _remaining;
		
		public CheckTimeUp(int remaining)
		{
			_remaining = remaining;
		}
		
		@Override
		public void run()
		{
			doCheckTimeUp(_remaining);
		}
	}
	
	public class TimeUp implements Runnable
	{
		@Override
		public void run()
		{
			InstanceManager.getInstance().destroyInstance(getId());
		}
	}
	
	public InstanceReenterType getReenterType()
	{
		return _type;
	}
	
	public void setReenterType(InstanceReenterType type)
	{
		_type = type;
	}
	
	public List<InstanceReenterTimeHolder> getReenterData()
	{
		return _resetData;
	}
	
	public boolean isRemoveBuffEnabled()
	{
		return _removeBuffType != InstanceRemoveBuffType.NONE;
	}
	
	public InstanceRemoveBuffType getRemoveBuffType()
	{
		return _removeBuffType;
	}
	
	public List<Integer> getBuffExceptionList()
	{
		return _exceptionList;
	}
}
