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
package org.l2jmobius.gameserver.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.handler.ActionClickHandler;
import org.l2jmobius.gameserver.handler.ActionShiftHandler;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.instance.Servitor;
import org.l2jmobius.gameserver.model.events.ListenersContainer;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.interfaces.IPositionable;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Base class for all interactive objects.
 */
public abstract class WorldObject extends ListenersContainer implements IPositionable
{
	/** Name */
	private String _name;
	/** Object ID */
	private int _objectId;
	/** World Region */
	private WorldRegion _worldRegion;
	/** Location */
	private final Location _location = new Location(0, 0, -10000);
	/** Instance type */
	private InstanceType _instanceType;
	private boolean _isSpawned;
	private boolean _isInvisible;
	private Map<String, Object> _scripts;
	
	public WorldObject(int objectId)
	{
		setInstanceType(InstanceType.WorldObject);
		_objectId = objectId;
	}
	
	/**
	 * Gets the instance type of object.
	 * @return the instance type
	 */
	public InstanceType getInstanceType()
	{
		return _instanceType;
	}
	
	/**
	 * Sets the instance type.
	 * @param newInstanceType the instance type to set
	 */
	protected void setInstanceType(InstanceType newInstanceType)
	{
		_instanceType = newInstanceType;
	}
	
	/**
	 * Verifies if object is of any given instance types.
	 * @param instanceTypes the instance types to verify
	 * @return {@code true} if object is of any given instance types, {@code false} otherwise
	 */
	public boolean isInstanceTypes(InstanceType... instanceTypes)
	{
		return _instanceType.isTypes(instanceTypes);
	}
	
	public void onAction(Player player)
	{
		onAction(player, true);
	}
	
	public void onAction(Player player, boolean interact)
	{
		final IActionClickHandler handler = ActionClickHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.onAction(player, this, interact);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onActionShift(Player player)
	{
		final IActionShiftHandler handler = ActionShiftHandler.getInstance().getHandler(getInstanceType());
		if (handler != null)
		{
			handler.onAction(player, this, true);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onForcedAttack(Player player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public void onSpawn()
	{
	}
	
	public boolean decayMe()
	{
		_isSpawned = false;
		World.getInstance().removeVisibleObject(this, _worldRegion);
		World.getInstance().removeObject(this);
		return true;
	}
	
	public void refreshId()
	{
		World.getInstance().removeObject(this);
		IdManager.getInstance().releaseId(getObjectId());
		_objectId = IdManager.getInstance().getNextId();
	}
	
	public boolean spawnMe()
	{
		synchronized (this)
		{
			// Set the x,y,z position of the WorldObject spawn and update its _worldregion
			_isSpawned = true;
			setWorldRegion(World.getInstance().getRegion(this));
			
			// Add the WorldObject spawn in the _allobjects of World
			World.getInstance().addObject(this);
			
			// Add the WorldObject spawn to _visibleObjects and if necessary to _allplayers of its WorldRegion
			_worldRegion.addVisibleObject(this);
		}
		
		// this can synchronize on others instances, so it's out of synchronized, to avoid deadlocks
		// Add the WorldObject spawn in the world as a visible object
		World.getInstance().addVisibleObject(this, getWorldRegion());
		
		onSpawn();
		
		return true;
	}
	
	public void spawnMe(int x, int y, int z)
	{
		synchronized (this)
		{
			int spawnX = x;
			if (spawnX > World.WORLD_X_MAX)
			{
				spawnX = World.WORLD_X_MAX - 5000;
			}
			
			if (spawnX < World.WORLD_X_MIN)
			{
				spawnX = World.WORLD_X_MIN + 5000;
			}
			
			int spawnY = y;
			if (spawnY > World.WORLD_Y_MAX)
			{
				spawnY = World.WORLD_Y_MAX - 5000;
			}
			
			if (spawnY < World.WORLD_Y_MIN)
			{
				spawnY = World.WORLD_Y_MIN + 5000;
			}
			
			// Set the x,y,z position of the WorldObject. If flagged with _isSpawned, setXYZ will automatically update world region, so avoid that.
			setXYZ(spawnX, spawnY, z);
		}
		
		// Spawn and update its _worldregion
		spawnMe();
	}
	
	/**
	 * Verify if object can be attacked.
	 * @return {@code true} if object can be attacked, {@code false} otherwise
	 */
	public boolean canBeAttacked()
	{
		return false;
	}
	
	public abstract boolean isAutoAttackable(Creature attacker);
	
	public boolean isSpawned()
	{
		return _isSpawned;
	}
	
	public void setSpawned(boolean value)
	{
		_isSpawned = value;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String value)
	{
		_name = value;
	}
	
	public int getId()
	{
		return 0;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public abstract void sendInfo(Player player);
	
	public void sendPacket(ServerPacket packet)
	{
	}
	
	public void sendPacket(SystemMessageId id)
	{
	}
	
	/**
	 * Returns this object as an {@code Attackable} if it is an instance of the {@code Attackable} class.<br>
	 * If this object does not extend the {@code Attackable} class, it returns {@code null}.
	 * @return the current object as an {@code Attackable} if applicable, otherwise {@code null}.
	 */
	public Attackable asAttackable()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Creature} if it is an instance of the {@code Creature} class.<br>
	 * If this object does not extend the {@code Creature} class, it returns {@code null}.
	 * @return the current object as a {@code Creature} if applicable, otherwise {@code null}.
	 */
	public Creature asCreature()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Door} if it is an instance of the {@code Door} class.<br>
	 * If this object does not extend the {@code Door} class, it returns {@code null}.
	 * @return the current object as a {@code Door} if applicable, otherwise {@code null}.
	 */
	public Door asDoor()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Monster} if it is an instance of the {@code Monster} class.<br>
	 * If this object does not extend the {@code Monster} class, it returns {@code null}.
	 * @return the current object as a {@code Monster} if applicable, otherwise {@code null}.
	 */
	public Monster asMonster()
	{
		return null;
	}
	
	/**
	 * Returns this object as an {@code Npc} if it is an instance of the {@code Npc} class.<br>
	 * If this object does not extend the {@code Npc} class, it returns {@code null}.
	 * @return the current object as an {@code Npc} if applicable, otherwise {@code null}.
	 */
	public Npc asNpc()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Pet} if it is an instance of the {@code Pet} class.<br>
	 * If this object does not extend the {@code Pet} class, it returns {@code null}.
	 * @return the current object as a {@code Pet} if applicable, otherwise {@code null}.
	 */
	public Pet asPet()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Playable} if it is an instance of the {@code Playable} class.<br>
	 * If this object does not extend the {@code Playable} class, it returns {@code null}.
	 * @return the current object as a {@code Playable} if applicable, otherwise {@code null}.
	 */
	public Playable asPlayable()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Player} if it is an instance of the {@code Player} class.<br>
	 * If this object does not extend the {@code Player} class, it returns {@code null}.
	 * @return the current object as a {@code Player} if applicable, otherwise {@code null}.
	 */
	public Player asPlayer()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Servitor} if it is an instance of the {@code Servitor} class.<br>
	 * If this object does not extend the {@code Servitor} class, it returns {@code null}.
	 * @return the current object as a {@code Servitor} if applicable, otherwise {@code null}.
	 */
	public Servitor asServitor()
	{
		return null;
	}
	
	/**
	 * Returns this object as a {@code Summon} if it is an instance of the {@code Summon} class.<br>
	 * If this object does not extend the {@code Summon} class, it returns {@code null}.
	 * @return the current object as a {@code Summon} if applicable, otherwise {@code null}.
	 */
	public Summon asSummon()
	{
		return null;
	}
	
	/**
	 * Verify if object is instance of Artefact.
	 * @return {@code true} if object is instance of Artefact, {@code false} otherwise.
	 */
	public boolean isArtefact()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Attackable.
	 * @return {@code true} if object is instance of Attackable, {@code false} otherwise.
	 */
	public boolean isAttackable()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Creature.
	 * @return {@code true} if object is instance of Creature, {@code false} otherwise.
	 */
	public boolean isCreature()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Cubic.
	 * @return {@code true} if object is instance of Cubic, {@code false} otherwise.
	 */
	public boolean isCubic()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Door.
	 * @return {@code true} if object is instance of Door, {@code false} otherwise.
	 */
	public boolean isDoor()
	{
		return false;
	}
	
	/**
	 * Verify if object is a fake player.
	 * @return {@code true} if object is a fake player, {@code false} otherwise.
	 */
	public boolean isFakePlayer()
	{
		return false;
	}
	
	/**
	 * Verifies if this object is a fence.
	 * @return {@code true} if object is Fence, {@code false} otherwise.
	 */
	public boolean isFence()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Item.
	 * @return {@code true} if object is instance of Item, {@code false} otherwise.
	 */
	public boolean isItem()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Monster.
	 * @return {@code true} if object is instance of Monster, {@code false} otherwise.
	 */
	public boolean isMonster()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Npc.
	 * @return {@code true} if object is instance of Npc, {@code false} otherwise.
	 */
	public boolean isNpc()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Pet.
	 * @return {@code true} if object is instance of Pet, {@code false} otherwise.
	 */
	public boolean isPet()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Playable.
	 * @return {@code true} if object is instance of Playable, {@code false} otherwise.
	 */
	public boolean isPlayable()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Player.
	 * @return {@code true} if object is instance of Player, {@code false} otherwise.
	 */
	public boolean isPlayer()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Servitor.
	 * @return {@code true} if object is instance of Servitor, {@code false} otherwise.
	 */
	public boolean isServitor()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Summon.
	 * @return {@code true} if object is instance of Summon, {@code false} otherwise.
	 */
	public boolean isSummon()
	{
		return false;
	}
	
	/**
	 * Verify if object is instance of Trap.
	 * @return {@code true} if object is instance of Trap, {@code false} otherwise.
	 */
	public boolean isTrap()
	{
		return false;
	}
	
	/**
	 * Verifies if this object is a vehicle.
	 * @return {@code true} if object is Vehicle, {@code false} otherwise.
	 */
	public boolean isVehicle()
	{
		return false;
	}
	
	/**
	 * Verifies if the object is a walker NPC.
	 * @return {@code true} if object is a walker NPC, {@code false} otherwise.
	 */
	public boolean isWalker()
	{
		return false;
	}
	
	/**
	 * @return {@code true} if object Can be targeted
	 */
	public boolean isTargetable()
	{
		return true;
	}
	
	/**
	 * Check if the object is in the given zone Id.
	 * @param zone the zone Id to check
	 * @return {@code true} if the object is in that zone Id
	 */
	public boolean isInsideZone(ZoneId zone)
	{
		return false;
	}
	
	/**
	 * Check if current object has charged shot.
	 * @param type of the shot to be checked.
	 * @return {@code true} if the object has charged shot
	 */
	public boolean isChargedShot(ShotType type)
	{
		return false;
	}
	
	/**
	 * Charging shot into the current object.
	 * @param type of the shot to be charged.
	 * @param charged
	 */
	public void setChargedShot(ShotType type, boolean charged)
	{
	}
	
	/**
	 * Try to recharge a shot.
	 * @param physical skill are using Soul shots.
	 * @param magical skill are using Spirit shots.
	 */
	public void rechargeShots(boolean physical, boolean magical)
	{
	}
	
	/**
	 * @param <T>
	 * @param script
	 * @return
	 */
	public <T> T addScript(T script)
	{
		if (_scripts == null)
		{
			// Double-checked locking
			synchronized (this)
			{
				if (_scripts == null)
				{
					_scripts = new ConcurrentHashMap<>();
				}
			}
		}
		
		_scripts.put(script.getClass().getName(), script);
		return script;
	}
	
	/**
	 * @param <T>
	 * @param script
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T removeScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		
		return (T) _scripts.remove(script.getName());
	}
	
	/**
	 * @param <T>
	 * @param script
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getScript(Class<T> script)
	{
		if (_scripts == null)
		{
			return null;
		}
		
		return (T) _scripts.get(script.getName());
	}
	
	public void removeStatusListener(Creature object)
	{
	}
	
	public void setXYZInvisible(int x, int y, int z)
	{
		int correctX = x;
		if (correctX > World.WORLD_X_MAX)
		{
			correctX = World.WORLD_X_MAX - 5000;
		}
		
		if (correctX < World.WORLD_X_MIN)
		{
			correctX = World.WORLD_X_MIN + 5000;
		}
		
		int correctY = y;
		if (correctY > World.WORLD_Y_MAX)
		{
			correctY = World.WORLD_Y_MAX - 5000;
		}
		
		if (correctY < World.WORLD_Y_MIN)
		{
			correctY = World.WORLD_Y_MIN + 5000;
		}
		
		setXYZ(correctX, correctY, z);
		setSpawned(false);
	}
	
	public void setLocationInvisible(ILocational loc)
	{
		setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
	}
	
	public WorldRegion getWorldRegion()
	{
		return _worldRegion;
	}
	
	public void setWorldRegion(WorldRegion region)
	{
		if ((region == null) && (_worldRegion != null))
		{
			_worldRegion.removeVisibleObject(this);
		}
		
		_worldRegion = region;
	}
	
	/**
	 * Gets the X coordinate.
	 * @return the X coordinate
	 */
	@Override
	public int getX()
	{
		return _location.getX();
	}
	
	/**
	 * Gets the Y coordinate.
	 * @return the Y coordinate
	 */
	@Override
	public int getY()
	{
		return _location.getY();
	}
	
	/**
	 * Gets the Z coordinate.
	 * @return the Z coordinate
	 */
	@Override
	public int getZ()
	{
		return _location.getZ();
	}
	
	/**
	 * Gets the heading.
	 * @return the heading
	 */
	@Override
	public int getHeading()
	{
		return _location.getHeading();
	}
	
	/**
	 * Gets the instance ID.
	 * @return the instance ID
	 */
	@Override
	public int getInstanceId()
	{
		return _location.getInstanceId();
	}
	
	/**
	 * Gets the location object.
	 * @return the location object
	 */
	@Override
	public Location getLocation()
	{
		return _location;
	}
	
	/**
	 * Sets the x, y, z coordinate.
	 * @param newX the X coordinate
	 * @param newY the Y coordinate
	 * @param newZ the Z coordinate
	 */
	@Override
	public void setXYZ(int newX, int newY, int newZ)
	{
		_location.setXYZ(newX, newY, newZ);
		
		if (_isSpawned)
		{
			final WorldRegion newRegion = World.getInstance().getRegion(this);
			if ((newRegion != null) && (newRegion != _worldRegion))
			{
				if (_worldRegion != null)
				{
					_worldRegion.removeVisibleObject(this);
				}
				
				newRegion.addVisibleObject(this);
				World.getInstance().switchRegion(this, newRegion);
				setWorldRegion(newRegion);
			}
		}
	}
	
	/**
	 * Sets the x, y, z coordinate.
	 * @param loc the location object
	 */
	@Override
	public void setXYZ(ILocational loc)
	{
		setXYZ(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Sets heading of object.
	 * @param newHeading the new heading
	 */
	@Override
	public void setHeading(int newHeading)
	{
		_location.setHeading(newHeading);
	}
	
	/**
	 * Sets the instance ID of object.<br>
	 * 0 - Global
	 * @param instanceId the ID of the instance
	 */
	@Override
	public void setInstanceId(int instanceId)
	{
		final int oldInstanceId = getInstanceId();
		if ((instanceId < 0) || (oldInstanceId == instanceId))
		{
			return;
		}
		
		final Instance oldI = InstanceManager.getInstance().getInstance(oldInstanceId);
		final Instance newI = InstanceManager.getInstance().getInstance(instanceId);
		if (newI == null)
		{
			return;
		}
		
		if (isPlayer())
		{
			final Player player = asPlayer();
			if ((oldInstanceId > 0) && (oldI != null))
			{
				oldI.removePlayer(_objectId);
				if (oldI.isShowTimer())
				{
					sendInstanceUpdate(oldI, true);
				}
			}
			
			if (instanceId > 0)
			{
				newI.addPlayer(_objectId);
				if (newI.isShowTimer())
				{
					sendInstanceUpdate(newI, false);
				}
			}
			
			if (player.hasSummon())
			{
				player.getSummon().setInstanceId(instanceId);
			}
		}
		else if (isNpc())
		{
			final Npc npc = asNpc();
			if ((oldInstanceId > 0) && (oldI != null))
			{
				oldI.removeNpc(npc);
			}
			
			if (instanceId > 0)
			{
				newI.addNpc(npc);
			}
		}
		
		_location.setInstanceId(instanceId);
	}
	
	/**
	 * Sends an instance update for player.
	 * @param instance the instance to update
	 * @param hide if {@code true} hide the player
	 */
	private void sendInstanceUpdate(Instance instance, boolean hide)
	{
		// final int startTime = (int) ((System.currentTimeMillis() - instance.getInstanceStartTime()) / 1000);
		// final int endTime = (int) ((instance.getInstanceEndTime() - instance.getInstanceStartTime()) / 1000);
		// if (instance.isTimerIncrease())
		// {
		// sendPacket(new ExSendUIEvent(asPlayer(), hide, true, startTime, endTime, instance.getTimerText()));
		// }
		// else
		// {
		// sendPacket(new ExSendUIEvent(asPlayer(), hide, false, endTime - startTime, 0, instance.getTimerText()));
		// }
	}
	
	/**
	 * Sets location of object.
	 * @param loc the location object
	 */
	@Override
	public void setLocation(Location loc)
	{
		_location.setXYZ(loc.getX(), loc.getY(), loc.getZ());
		_location.setHeading(loc.getHeading());
		_location.setInstanceId(loc.getInstanceId());
	}
	
	/**
	 * Calculates 2D distance between this WorldObject and given x, y, z.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @param z the Z coordinate
	 * @return distance between object and given x, y, z.
	 */
	public double calculateDistance2D(int x, int y, int z)
	{
		return Math.sqrt(Math.pow(x - getX(), 2) + Math.pow(y - getY(), 2));
	}
	
	/**
	 * Calculates the 2D distance between this WorldObject and given location.
	 * @param loc the location object
	 * @return distance between object and given location.
	 */
	public double calculateDistance2D(ILocational loc)
	{
		if (loc == null)
		{
			return Double.MAX_VALUE;
		}
		
		return calculateDistance2D(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Calculates the 3D distance between this WorldObject and given x, y, z.
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 * @param z the Z coordinate
	 * @return distance between object and given x, y, z.
	 */
	public double calculateDistance3D(int x, int y, int z)
	{
		return Math.sqrt(Math.pow(x - getX(), 2) + Math.pow(y - getY(), 2) + Math.pow(z - getZ(), 2));
	}
	
	/**
	 * Calculates 3D distance between this WorldObject and given location.
	 * @param loc the location object
	 * @return distance between object and given location.
	 */
	public double calculateDistance3D(ILocational loc)
	{
		if (loc == null)
		{
			return Double.MAX_VALUE;
		}
		
		return calculateDistance3D(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Calculates the angle in degrees from this object to the given object.<br>
	 * The return value can be described as how much this object has to turn<br>
	 * to have the given object directly in front of it.
	 * @param target the object to which to calculate the angle
	 * @return the angle this object has to turn to have the given object in front of it
	 */
	public double calculateDirectionTo(ILocational target)
	{
		return LocationUtil.calculateAngleFrom(this, target);
	}
	
	/**
	 * @return {@code true} if this object is invisible, {@code false} otherwise.
	 */
	public boolean isInvisible()
	{
		return _isInvisible;
	}
	
	/**
	 * Sets this object as invisible or not
	 * @param invisible
	 */
	public void setInvisible(boolean invisible)
	{
		_isInvisible = invisible;
		
		if (invisible)
		{
			final DeleteObject deletePacket = new DeleteObject(this);
			World.getInstance().forEachVisibleObject(this, Player.class, player ->
			{
				if (!isVisibleFor(player))
				{
					player.sendPacket(deletePacket);
				}
			});
		}
		
		// Broadcast information regarding the object to those which are suppose to see.
		broadcastInfo();
	}
	
	/**
	 * @param player
	 * @return {@code true} if player can see an invisible object if it's invisible, {@code false} otherwise.
	 */
	public boolean isVisibleFor(Player player)
	{
		return !_isInvisible || player.isGM();
	}
	
	/**
	 * Broadcasts describing info to known players.
	 */
	public void broadcastInfo()
	{
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			if (isVisibleFor(player))
			{
				sendInfo(player);
			}
		});
	}
	
	public boolean isInSurroundingRegion(WorldObject worldObject)
	{
		if (worldObject == null)
		{
			return false;
		}
		
		final WorldRegion worldRegion = worldObject.getWorldRegion();
		if (worldRegion == null)
		{
			return false;
		}
		
		if (_worldRegion == null)
		{
			return false;
		}
		
		return worldRegion.isSurroundingRegion(_worldRegion);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof WorldObject) && (((WorldObject) obj).getObjectId() == getObjectId());
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(":");
		sb.append(_name);
		sb.append("[");
		sb.append(_objectId);
		sb.append("]");
		return sb.toString();
	}
}
