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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Basic instance zone data transfer object.
 * @author Zoey76
 */
public class InstanceWorld
{
	private Instance _instance;
	private final Set<Integer> _allowed = ConcurrentHashMap.newKeySet();
	private final StatSet _parameters = new StatSet();
	
	/**
	 * Sets the instance.
	 * @param instance the instance
	 */
	public void setInstance(Instance instance)
	{
		_instance = instance;
	}
	
	/**
	 * Gets the dynamically generated instance ID.
	 * @return the instance ID
	 */
	public int getInstanceId()
	{
		return _instance.getId();
	}
	
	/**
	 * Get template ID of instance world.
	 * @return instance template ID
	 */
	public int getTemplateId()
	{
		return _instance.getTemplateId();
	}
	
	public List<Player> getAllowed()
	{
		final List<Player> allowed = new ArrayList<>(_allowed.size());
		for (int playerId : _allowed)
		{
			final Player player = World.getInstance().getPlayer(playerId);
			if (player != null)
			{
				allowed.add(player);
			}
		}
		
		return allowed;
	}
	
	public void removeAllowed(Player player)
	{
		_allowed.remove(player.getObjectId());
	}
	
	/**
	 * Add player who can enter to instance.
	 * @param player player instance
	 */
	public void addAllowed(Player player)
	{
		if (!_allowed.contains(player.getObjectId()))
		{
			_allowed.add(player.getObjectId());
		}
	}
	
	/**
	 * Check if player can enter to instance.
	 * @param player player itself
	 * @return {@code true} when can enter, otherwise {@code false}
	 */
	public boolean isAllowed(Player player)
	{
		return _allowed.contains(player.getObjectId());
	}
	
	/**
	 * Teleport player out of instance.
	 * @param player player that should be moved out
	 */
	public void ejectPlayer(Player player)
	{
		if ((player != null) && (player.getInstanceId() == _instance.getId()))
		{
			player.setInstanceId(0);
			if (_instance.getExitLoc() != null)
			{
				player.teleToLocation(_instance.getExitLoc(), true);
			}
			else
			{
				player.teleToLocation(TeleportWhereType.TOWN);
			}
		}
	}
	
	/**
	 * Set instance world parameter.
	 * @param key parameter name
	 * @param value parameter value
	 */
	public void setParameter(String key, Object value)
	{
		if (value == null)
		{
			_parameters.remove(key);
		}
		else
		{
			_parameters.set(key, value);
		}
	}
	
	/**
	 * Get instance world parameters.
	 * @return instance parameters
	 */
	public StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Get status of instance world.
	 * @return instance status, otherwise 0
	 */
	public int getStatus()
	{
		return _parameters.getInt("INSTANCE_STATUS", 0);
	}
	
	/**
	 * Check if instance status is equal to {@code status}.
	 * @param status number used for status comparison
	 * @return {@code true} when instance status and {@code status} are equal, otherwise {@code false}
	 */
	public boolean isStatus(int status)
	{
		return getStatus() == status;
	}
	
	/**
	 * Set status of instance world.
	 * @param value new world status
	 */
	public void setStatus(int value)
	{
		_parameters.set("INSTANCE_STATUS", value);
	}
	
	/**
	 * Increment instance world status
	 * @return new world status
	 */
	public int incStatus()
	{
		final int status = getStatus() + 1;
		setStatus(status);
		return status;
	}
	
	/**
	 * Get spawned NPCs from instance.
	 * @return set of NPCs from instance
	 */
	public Collection<Npc> getNpcs()
	{
		return _instance.getNpcs();
	}
	
	/**
	 * Get spawned NPCs from instance with specific IDs.
	 * @param id IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	public List<Npc> getNpcs(int... id)
	{
		final List<Npc> result = new LinkedList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if (ArrayUtil.contains(id, npc.getId()))
			{
				result.add(npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Get spawned NPCs from instance with specific IDs and class type.
	 * @param <T>
	 * @param clazz
	 * @param ids IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final <T extends Creature> List<T> getNpcs(Class<T> clazz, int... ids)
	{
		final List<T> result = new LinkedList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if (((ids.length == 0) || ArrayUtil.contains(ids, npc.getId())) && clazz.isInstance(npc))
			{
				result.add((T) npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Get alive NPCs from instance.
	 * @return set of NPCs from instance
	 */
	public List<Npc> getAliveNpcs()
	{
		final List<Npc> result = new LinkedList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if (npc.getCurrentHp() > 0)
			{
				result.add(npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Get alive NPCs from instance with specific IDs.
	 * @param id IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	public List<Npc> getAliveNpcs(int... id)
	{
		final List<Npc> result = new LinkedList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if ((npc.getCurrentHp() > 0) && ArrayUtil.contains(id, npc.getId()))
			{
				result.add(npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Get spawned and alive NPCs from instance with specific IDs and class type.
	 * @param <T>
	 * @param clazz
	 * @param ids IDs of NPCs which should be found
	 * @return list of filtered NPCs from instance
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public final <T extends Creature> List<T> getAliveNpcs(Class<T> clazz, int... ids)
	{
		final List<T> result = new LinkedList<>();
		for (Npc npc : _instance.getNpcs())
		{
			if ((((ids.length == 0) || ArrayUtil.contains(ids, npc.getId())) && (npc.getCurrentHp() > 0)) && clazz.isInstance(npc))
			{
				result.add((T) npc);
			}
		}
		
		return result;
	}
	
	/**
	 * Get first found spawned NPC with specific ID.
	 * @param id ID of NPC to be found
	 * @return first found NPC with specified ID, otherwise {@code null}
	 */
	public Npc getNpc(int id)
	{
		for (Npc npc : _instance.getNpcs())
		{
			if (npc.getId() == id)
			{
				return npc;
			}
		}
		
		return null;
	}
	
	/**
	 * Spawns group of instance NPCs
	 * @param groupName the name of group from XML definition to spawn
	 * @return list of spawned NPCs
	 */
	public List<Npc> spawnGroup(String groupName)
	{
		return _instance.spawnGroup(groupName);
	}
	
	/**
	 * Open a door if it is present in the instance and it is not open.
	 * @param doorId the ID of the door to open
	 */
	public void openDoor(int doorId)
	{
		final Door door = _instance.getDoor(doorId);
		if ((door != null) && !door.isOpen())
		{
			door.openMe();
		}
	}
	
	/**
	 * Close a door if it is present in the instance and it is open.
	 * @param doorId the ID of the door to close
	 */
	public void closeDoor(int doorId)
	{
		final Door door = _instance.getDoor(doorId);
		if ((door != null) && door.isOpen())
		{
			door.closeMe();
		}
	}
	
	/**
	 * Get all doors spawned inside instance world.
	 * @return collection of spawned doors
	 */
	public Collection<Door> getDoors()
	{
		return _instance.getDoors();
	}
	
	/**
	 * Get spawned door by template ID.
	 * @param doorId template ID of door
	 * @return instance of door if found, otherwise {@code null}
	 */
	public Door getDoor(int doorId)
	{
		return _instance.getDoor(doorId);
	}
	
	/**
	 * Get count of players inside instance.
	 * @return players count inside instance
	 */
	public int getPlayersCount()
	{
		return _instance.getPlayers().size();
	}
	
	/**
	 * Destroy current instance world.<br>
	 * <b><font color=red>Use this method to destroy instance world properly.</font></b>
	 */
	public void destroy()
	{
		_instance.setEmptyDestroyTime(0);
		_instance.setDuration(1000);
	}
	
	/**
	 * Send packet to each player from instance world.
	 * @param packets packets to be send
	 */
	public void broadcastPacket(ServerPacket... packets)
	{
		for (int objectId : _instance.getPlayers())
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if ((player != null) && (player.getInstanceId() == _instance.getId()))
			{
				for (ServerPacket packet : packets)
				{
					player.sendPacket(packet);
				}
			}
		}
	}
}
