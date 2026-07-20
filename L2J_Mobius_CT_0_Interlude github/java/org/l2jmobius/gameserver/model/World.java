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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

public class World
{
	private static final Logger LOGGER = Logger.getLogger(World.class.getName());
	
	public static volatile int MAX_CONNECTED_COUNT = 0;
	public static volatile int OFFLINE_TRADE_COUNT = 0;
	
	/** Bit shift, defines number of regions note, shifting by 15 will result in regions corresponding to map tiles shifting by 11 divides one tile to 16x16 regions. */
	public static final int SHIFT_BY = 11;
	
	public static final int TILE_SIZE = 32768;
	
	/** Map dimensions. */
	public static final int TILE_X_MIN = 16;
	public static final int TILE_Y_MIN = 10;
	public static final int TILE_X_MAX = 26;
	public static final int TILE_Y_MAX = 25;
	public static final int TILE_ZERO_COORD_X = 20;
	public static final int TILE_ZERO_COORD_Y = 18;
	public static final int WORLD_X_MIN = (TILE_X_MIN - TILE_ZERO_COORD_X) * TILE_SIZE;
	public static final int WORLD_Y_MIN = (TILE_Y_MIN - TILE_ZERO_COORD_Y) * TILE_SIZE;
	
	public static final int WORLD_X_MAX = ((TILE_X_MAX - TILE_ZERO_COORD_X) + 1) * TILE_SIZE;
	public static final int WORLD_Y_MAX = ((TILE_Y_MAX - TILE_ZERO_COORD_Y) + 1) * TILE_SIZE;
	
	/** Z coordinate limits */
	public static final int WORLD_Z_MIN = -16000;
	public static final int WORLD_Z_MAX = 16000;
	public static final int Z_REGION_SIZE = 2000;
	
	/** Calculated offset used so top left region is 0,0 */
	public static final int OFFSET_X = Math.abs(WORLD_X_MIN >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(WORLD_Y_MIN >> SHIFT_BY);
	
	/** Number of regions. */
	private static final int REGIONS_X = (WORLD_X_MAX >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (WORLD_Y_MAX >> SHIFT_BY) + OFFSET_Y;
	private static final int REGIONS_Z = (Math.abs(WORLD_Z_MIN) + Math.abs(WORLD_Z_MAX)) / Z_REGION_SIZE;
	
	/** Map containing all the players in game. */
	private static final Map<Integer, Player> _allPlayers = new ConcurrentHashMap<>();
	/** Map containing all the Good players in game. */
	private static final Map<Integer, Player> _allGoodPlayers = new ConcurrentHashMap<>();
	/** Map containing all the Evil players in game. */
	private static final Map<Integer, Player> _allEvilPlayers = new ConcurrentHashMap<>();
	/** Map containing all visible objects. */
	private static final Map<Integer, WorldObject> _allObjects = new ConcurrentHashMap<>();
	/** Map with the pets instances and their owner ID. */
	private static final Map<Integer, Pet> _petsInstance = new ConcurrentHashMap<>();
	
	private static final WorldRegion[][][] _worldRegions = new WorldRegion[REGIONS_X + 1][REGIONS_Y + 1][REGIONS_Z];
	
	/** Constructor of World. */
	protected World()
	{
		// Initialize regions.
		for (int x = 0; x <= REGIONS_X; x++)
		{
			for (int y = 0; y <= REGIONS_Y; y++)
			{
				for (int z = 0; z < REGIONS_Z; z++)
				{
					_worldRegions[x][y][z] = new WorldRegion(x, y, z);
				}
			}
		}
		
		// Set surrounding regions.
		for (int rx = 0; rx <= REGIONS_X; rx++)
		{
			for (int ry = 0; ry <= REGIONS_Y; ry++)
			{
				for (int rz = 0; rz < REGIONS_Z; rz++)
				{
					final List<WorldRegion> surroundingRegions = new ArrayList<>();
					for (int sx = rx - 1; sx <= (rx + 1); sx++)
					{
						for (int sy = ry - 1; sy <= (ry + 1); sy++)
						{
							for (int sz = rz - 1; sz <= (rz + 1); sz++)
							{
								if (((sx >= 0) && (sx < REGIONS_X) && (sy >= 0) && (sy < REGIONS_Y) && (sz >= 0) && (sz < REGIONS_Z)))
								{
									surroundingRegions.add(_worldRegions[sx][sy][sz]);
								}
							}
						}
					}
					
					WorldRegion[] regionArray = new WorldRegion[surroundingRegions.size()];
					regionArray = surroundingRegions.toArray(regionArray);
					_worldRegions[rx][ry][rz].setSurroundingRegions(regionArray);
				}
			}
		}
		
		// When GUI is enabled World is called early. So we need to skip this log.
		if (!InterfaceConfig.ENABLE_GUI)
		{
			LOGGER.info(getClass().getSimpleName() + ": (" + REGIONS_X + " by " + REGIONS_Y + ") World Region Grid set up.");
		}
	}
	
	/**
	 * Adds an object to the world.<br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Withdraw an item from the warehouse, create an item</li>
	 * <li>Spawn a Creature (PC, NPC, Pet)</li>
	 * </ul>
	 * @param object
	 */
	public void addObject(WorldObject object)
	{
		_allObjects.putIfAbsent(object.getObjectId(), object);
		
		// if (_allObjects.putIfAbsent(object.getObjectId(), object) != null)
		// {
		// LOGGER.warning(getClass().getSimpleName() + ": Object " + object + " already exists in the world. Stack Trace: " + CommonUtil.getTraceString(Thread.currentThread().getStackTrace()));
		// }
		
		if (object.isPlayer())
		{
			final Player newPlayer = object.asPlayer();
			if (newPlayer.isTeleporting()) // TODO: Drop when we stop removing player from the world while teleporting.
			{
				return;
			}
			
			final Player existingPlayer = _allPlayers.putIfAbsent(object.getObjectId(), newPlayer);
			if (existingPlayer != null)
			{
				Disconnection.of(existingPlayer).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
				Disconnection.of(newPlayer).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
				LOGGER.warning(getClass().getSimpleName() + ": Duplicate character!? Disconnected both characters (" + newPlayer.getName() + ")");
			}
			else if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
			{
				addFactionPlayerToWorld(newPlayer);
			}
		}
	}
	
	/**
	 * Removes an object from the world.<br>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Delete item from inventory, transfer Item from inventory to warehouse</li>
	 * <li>Crystallize item</li>
	 * <li>Remove NPC/PC/Pet from the world</li>
	 * </ul>
	 * @param object the object to remove
	 */
	public void removeObject(WorldObject object)
	{
		_allObjects.remove(object.getObjectId());
		if (object.isPlayer())
		{
			final Player player = object.asPlayer();
			if (player.isTeleporting()) // TODO: Drop when we stop removing player from the world while teleporting.
			{
				return;
			}
			
			_allPlayers.remove(object.getObjectId());
			
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
			{
				if (player.isGood())
				{
					_allGoodPlayers.remove(player.getObjectId());
				}
				else if (player.isEvil())
				{
					_allEvilPlayers.remove(player.getObjectId());
				}
			}
		}
	}
	
	/**
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>Client packets : Action, AttackRequest, RequestJoinParty, RequestJoinPledge...</li>
	 * </ul>
	 * @param objectId Identifier of the WorldObject
	 * @return the WorldObject object that belongs to an ID or null if no object found.
	 */
	public WorldObject findObject(int objectId)
	{
		return _allObjects.get(objectId);
	}
	
	public Collection<WorldObject> getVisibleObjects()
	{
		return _allObjects.values();
	}
	
	/**
	 * Get the count of all visible objects in world.
	 * @return count off all World objects
	 */
	public int getVisibleObjectsCount()
	{
		return _allObjects.size();
	}
	
	public Collection<Player> getPlayers()
	{
		return _allPlayers.values();
	}
	
	public Collection<Player> getAllGoodPlayers()
	{
		return _allGoodPlayers.values();
	}
	
	public Collection<Player> getAllEvilPlayers()
	{
		return _allEvilPlayers.values();
	}
	
	/**
	 * <b>If you have access to player objectId use {@link #getPlayer(int playerObjId)}</b>
	 * @param name Name of the player to get Instance
	 * @return the player instance corresponding to the given name.
	 */
	public Player getPlayer(String name)
	{
		return getPlayer(CharInfoTable.getInstance().getIdByName(name));
	}
	
	/**
	 * @param objectId of the player to get Instance
	 * @return the player instance corresponding to the given object ID.
	 */
	public Player getPlayer(int objectId)
	{
		return _allPlayers.get(objectId);
	}
	
	/**
	 * @param ownerId ID of the owner
	 * @return the pet instance from the given ownerId.
	 */
	public Pet getPet(int ownerId)
	{
		return _petsInstance.get(ownerId);
	}
	
	/**
	 * Add the given pet instance from the given ownerId.
	 * @param ownerId ID of the owner
	 * @param pet Pet of the pet
	 * @return
	 */
	public Pet addPet(int ownerId, Pet pet)
	{
		return _petsInstance.put(ownerId, pet);
	}
	
	/**
	 * Remove the given pet instance.
	 * @param ownerId ID of the owner
	 */
	public void removePet(int ownerId)
	{
		_petsInstance.remove(ownerId);
	}
	
	/**
	 * This operation is quite heave as it iterates all world visible objects.
	 * @param npcId the id of the NPC to find.
	 * @return the first NPC found corresponding to the given id.
	 */
	public Npc getNpc(int npcId)
	{
		for (WorldObject wo : getVisibleObjects())
		{
			if (wo.isNpc() && (wo.getId() == npcId))
			{
				return wo.asNpc();
			}
		}
		
		return null;
	}
	
	/**
	 * Add a WorldObject in the world. <b><u>Concept</u>:</b> WorldObject (including Player) are identified in <b>_visibleObjects</b> of his current WorldRegion and in <b>_knownObjects</b> of other surrounding Creatures<br>
	 * Player are identified in <b>_allPlayers</b> of World, in <b>_allPlayers</b> of his current WorldRegion and in <b>_knownPlayer</b> of other surrounding Creatures <b><u> Actions</u>:</b>
	 * <li>Add the WorldObject object in _allPlayers* of World</li>
	 * <li>Add the WorldObject object in _gmList** of GmListTable</li>
	 * <li>Add object in _knownObjects and _knownPlayer* of all surrounding WorldRegion Creatures</li>
	 * <li>If object is a Creature, add all surrounding WorldObject in its _knownObjects and all surrounding Player in its _knownPlayer</li><br>
	 * <i>* only if object is a Player</i><br>
	 * <i>** only if object is a GM Player</i> <font color=#FF0000><b><u>Caution</u>: This method DOESN'T ADD the object in _visibleObjects and _allPlayers* of WorldRegion (need synchronisation)</b></font><br>
	 * <font color=#FF0000><b><u>Caution</u>: This method DOESN'T ADD the object to _allObjects and _allPlayers* of World (need synchronisation)</b></font> <b><u> Example of use</u>:</b>
	 * <li>Drop an Item</li>
	 * <li>Spawn a Creature</li>
	 * <li>Apply Death Penalty of a Player</li><br>
	 * @param object L2object to add in the world
	 * @param newRegion WorldRegion in wich the object will be add (not used)
	 */
	public void addVisibleObject(WorldObject object, WorldRegion newRegion)
	{
		if (!newRegion.isActive())
		{
			return;
		}
		
		forEachVisibleObject(object, WorldObject.class, wo ->
		{
			if (object.isPlayer() && wo.isVisibleFor(object.asPlayer()))
			{
				wo.sendInfo(object.asPlayer());
				if (wo.isCreature())
				{
					final CreatureAI ai = wo.asCreature().getAI();
					if (ai != null)
					{
						ai.describeStateToPlayer(object.asPlayer());
						if (wo.isMonster() && (ai.getIntention() == Intention.IDLE))
						{
							ai.setIntention(Intention.ACTIVE);
						}
					}
				}
			}
			
			if (wo.isPlayer() && object.isVisibleFor(wo.asPlayer()))
			{
				object.sendInfo(wo.asPlayer());
				if (object.isCreature())
				{
					final CreatureAI ai = object.asCreature().getAI();
					if (ai != null)
					{
						ai.describeStateToPlayer(wo.asPlayer());
						if (object.isMonster() && (ai.getIntention() == Intention.IDLE))
						{
							ai.setIntention(Intention.ACTIVE);
						}
					}
				}
			}
		});
	}
	
	public static void addFactionPlayerToWorld(Player player)
	{
		if (player.isGood())
		{
			_allGoodPlayers.put(player.getObjectId(), player);
		}
		else if (player.isEvil())
		{
			_allEvilPlayers.put(player.getObjectId(), player);
		}
	}
	
	/**
	 * Remove a WorldObject from the world. <b><u>Concept</u>:</b> WorldObject (including Player) are identified in <b>_visibleObjects</b> of his current WorldRegion and in <b>_knownObjects</b> of other surrounding Creatures<br>
	 * Player are identified in <b>_allPlayers</b> of World, in <b>_allPlayers</b> of his current WorldRegion and in <b>_knownPlayer</b> of other surrounding Creatures <b><u> Actions</u>:</b>
	 * <li>Remove the WorldObject object from _allPlayers* of World</li>
	 * <li>Remove the WorldObject object from _visibleObjects and _allPlayers* of WorldRegion</li>
	 * <li>Remove the WorldObject object from _gmList** of GmListTable</li>
	 * <li>Remove object from _knownObjects and _knownPlayer* of all surrounding WorldRegion Creatures</li>
	 * <li>If object is a Creature, remove all WorldObject from its _knownObjects and all Player from its _knownPlayer</li> <font color=#FF0000><b><u>Caution</u>: This method DOESN'T REMOVE the object from _allObjects of World</b></font> <i>* only if object is a Player</i><br>
	 * <i>** only if object is a GM Player</i> <b><u> Example of use</u>:</b>
	 * <li>Pickup an Item</li>
	 * <li>Decay a Creature</li><br>
	 * @param object L2object to remove from the world
	 * @param oldRegion WorldRegion in which the object was before removing
	 */
	public void removeVisibleObject(WorldObject object, WorldRegion oldRegion)
	{
		if ((object == null) || (oldRegion == null))
		{
			return;
		}
		
		oldRegion.removeVisibleObject(object);
		
		// Go through all surrounding WorldRegion Creatures
		final WorldRegion[] surroundingRegions = oldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if (wo == object)
				{
					continue;
				}
				
				if (object.isCreature())
				{
					final Creature objectCreature = object.asCreature();
					final CreatureAI ai = objectCreature.getAI();
					if (ai != null)
					{
						ai.notifyAction(Action.FORGET_OBJECT, wo);
					}
					
					if (objectCreature.getTarget() == wo)
					{
						objectCreature.setTarget(null);
					}
					
					if (object.isPlayer())
					{
						object.sendPacket(new DeleteObject(wo));
					}
				}
				
				if (wo.isCreature())
				{
					final Creature woCreature = wo.asCreature();
					final CreatureAI ai = woCreature.getAI();
					if (ai != null)
					{
						ai.notifyAction(Action.FORGET_OBJECT, object);
					}
					
					if (woCreature.getTarget() == object)
					{
						woCreature.setTarget(null);
					}
					
					if (wo.isPlayer())
					{
						wo.sendPacket(new DeleteObject(object));
					}
				}
			}
		}
	}
	
	public void switchRegion(WorldObject object, WorldRegion newRegion)
	{
		final WorldRegion oldRegion = object.getWorldRegion();
		if ((oldRegion == null) || (oldRegion == newRegion))
		{
			return;
		}
		
		final WorldRegion[] oldSurroundingRegions = oldRegion.getSurroundingRegions();
		for (int i = 0; i < oldSurroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = oldSurroundingRegions[i];
			if (newRegion.isSurroundingRegion(worldRegion))
			{
				continue;
			}
			
			final Collection<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if (wo == object)
				{
					continue;
				}
				
				if (object.isCreature())
				{
					final Creature objectCreature = object.asCreature();
					final CreatureAI ai = objectCreature.getAI();
					if (ai != null)
					{
						ai.notifyAction(Action.FORGET_OBJECT, wo);
					}
					
					if (objectCreature.getTarget() == wo)
					{
						objectCreature.setTarget(null);
					}
					
					if (object.isPlayer())
					{
						object.sendPacket(new DeleteObject(wo));
					}
				}
				
				if (wo.isCreature())
				{
					final Creature woCreature = wo.asCreature();
					final CreatureAI ai = woCreature.getAI();
					if (ai != null)
					{
						ai.notifyAction(Action.FORGET_OBJECT, object);
					}
					
					if (woCreature.getTarget() == object)
					{
						woCreature.setTarget(null);
					}
					
					if (wo.isPlayer())
					{
						wo.sendPacket(new DeleteObject(object));
					}
				}
			}
		}
		
		final WorldRegion[] newSurroundingRegions = newRegion.getSurroundingRegions();
		for (int i = 0; i < newSurroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = newSurroundingRegions[i];
			if (oldRegion.isSurroundingRegion(worldRegion))
			{
				continue;
			}
			
			final Collection<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || (wo.getInstanceId() != object.getInstanceId()))
				{
					continue;
				}
				
				if (object.isPlayer() && wo.isVisibleFor(object.asPlayer()))
				{
					wo.sendInfo(object.asPlayer());
					if (wo.isCreature())
					{
						final CreatureAI ai = wo.asCreature().getAI();
						if (ai != null)
						{
							ai.describeStateToPlayer(object.asPlayer());
							if (wo.isMonster() && (ai.getIntention() == Intention.IDLE))
							{
								ai.setIntention(Intention.ACTIVE);
							}
						}
					}
				}
				
				if (wo.isPlayer() && object.isVisibleFor(wo.asPlayer()))
				{
					object.sendInfo(wo.asPlayer());
					if (object.isCreature())
					{
						final CreatureAI ai = object.asCreature().getAI();
						if (ai != null)
						{
							ai.describeStateToPlayer(wo.asPlayer());
							if (object.isMonster() && (ai.getIntention() == Intention.IDLE))
							{
								ai.setIntention(Intention.ACTIVE);
							}
						}
					}
				}
			}
		}
	}
	
	public <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz)
	{
		final List<T> result = new LinkedList<>();
		forEachVisibleObject(object, clazz, result::add);
		return result;
	}
	
	public <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz, Predicate<T> predicate)
	{
		final List<T> result = new LinkedList<>();
		forEachVisibleObject(object, clazz, o ->
		{
			if (predicate.test(o))
			{
				result.add(o);
			}
		});
		
		return result;
	}
	
	public <T extends WorldObject> void forEachVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> c)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo))
				{
					continue;
				}
				
				if (wo.getInstanceId() != object.getInstanceId())
				{
					continue;
				}
				
				c.accept(clazz.cast(wo));
			}
		}
	}
	
	public <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range)
	{
		final List<T> result = new LinkedList<>();
		forEachVisibleObjectInRange(object, clazz, range, result::add);
		return result;
	}
	
	public <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate)
	{
		final List<T> result = new LinkedList<>();
		forEachVisibleObjectInRange(object, clazz, range, o ->
		{
			if (predicate.test(o))
			{
				result.add(o);
			}
		});
		
		return result;
	}
	
	public <T extends WorldObject> void forEachVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> c)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo))
				{
					continue;
				}
				
				if (wo.getInstanceId() != object.getInstanceId())
				{
					continue;
				}
				
				if (wo.calculateDistance3D(object) <= range)
				{
					c.accept(clazz.cast(wo));
				}
			}
		}
	}
	
	/**
	 * Calculate the current WorldRegions of the object according to its position (x,y). <b><u>Example of use</u>:</b>
	 * <li>Set position of a new WorldObject (drop, spawn...)</li>
	 * <li>Update position of a WorldObject after a movement</li><br>
	 * @param object the object
	 * @return
	 */
	public WorldRegion getRegion(WorldObject object)
	{
		try
		{
			final int z = object.getZ();
			final int regionZ = calculateRegionZ(z);
			return _worldRegions[(object.getX() >> SHIFT_BY) + OFFSET_X][(object.getY() >> SHIFT_BY) + OFFSET_Y][regionZ];
		}
		catch (ArrayIndexOutOfBoundsException e) // Precaution. Moved at invalid region?
		{
			disposeOutOfBoundsObject(object);
			return null;
		}
	}
	
	public WorldRegion getRegion(int x, int y, int z)
	{
		try
		{
			final int regionZ = calculateRegionZ(z);
			return _worldRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y][regionZ];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Incorrect world region X: " + ((x >> SHIFT_BY) + OFFSET_X) + " Y: " + ((y >> SHIFT_BY) + OFFSET_Y) + " Z: " + calculateRegionZ(z));
			return null;
		}
	}
	
	/**
	 * Calculate the region Z index based on Z coordinate
	 * @param z The Z coordinate
	 * @return The region Z index (0-3)
	 */
	private int calculateRegionZ(int z)
	{
		// Handle values outside the normal range.
		if (z < WORLD_Z_MIN)
		{
			return 0; // Bottom region.
		}
		if (z > WORLD_Z_MAX)
		{
			return REGIONS_Z - 1; // Top region.
		}
		
		// Calculate the region Z index (0-based).
		return (z - WORLD_Z_MIN) / Z_REGION_SIZE;
	}
	
	/**
	 * Returns the whole 3d array containing the world regions used by ZoneData.java to setup zones inside the world regions
	 * @return
	 */
	public WorldRegion[][][] getWorldRegions()
	{
		return _worldRegions;
	}
	
	public synchronized void disposeOutOfBoundsObject(WorldObject object)
	{
		if (object.isPlayer())
		{
			object.asCreature().stopMove(object.asPlayer().getLastServerPosition());
		}
		else if (object.isSummon())
		{
			final Summon summon = object.asSummon();
			summon.unSummon(summon.getOwner());
		}
		else if (_allObjects.remove(object.getObjectId()) != null)
		{
			if (object.isNpc())
			{
				final Npc npc = object.asNpc();
				LOGGER.warning("Deleting npc " + object.getName() + " NPCID[" + npc.getId() + "] from invalid location X:" + object.getX() + " Y:" + object.getY() + " Z:" + object.getZ());
				npc.deleteMe();
				
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					LOGGER.warning("Spawn location X:" + spawn.getX() + " Y:" + spawn.getY() + " Z:" + spawn.getZ() + " Heading:" + spawn.getHeading());
				}
			}
			else if (object.isCreature())
			{
				LOGGER.warning("Deleting object " + object.getName() + " OID[" + object.getObjectId() + "] from invalid location X:" + object.getX() + " Y:" + object.getY() + " Z:" + object.getZ());
				object.asCreature().deleteMe();
			}
			
			if (object.getWorldRegion() != null)
			{
				object.getWorldRegion().removeVisibleObject(object);
			}
		}
	}
	
	public static World getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final World INSTANCE = new World();
	}
}
