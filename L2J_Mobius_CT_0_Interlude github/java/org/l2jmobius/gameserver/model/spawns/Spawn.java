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
package org.l2jmobius.gameserver.model.spawns;

import java.lang.reflect.Constructor;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.RandomSpawnsConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcPersonalAIData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.model.zone.type.NpcSpawnTerritory;
import org.l2jmobius.gameserver.model.zone.type.WaterZone;
import org.l2jmobius.gameserver.taskmanagers.RespawnTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class manages the spawn and respawn of a group of Npcs that are in the same are and have the same type.<br>
 * <b><u>Concept</u>:</b><br>
 * Npc can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.<br>
 * The heading of the Npc can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).
 * @author Nightmare, Mobius
 */
public class Spawn extends Location
{
	protected static final Logger LOGGER = Logger.getLogger(Spawn.class.getName());
	
	/** String identifier of this spawn */
	private String _name;
	/** The link on the NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private NpcTemplate _template;
	/** The maximum number of Npc that can manage this Spawn */
	private int _maximumCount;
	/** The current number of Npc managed by this Spawn */
	private int _currentCount;
	/** The current number of SpawnTask in progress or stand by of this Spawn */
	public int _scheduledCount;
	/** The identifier of the location area where Npc can be spawned */
	private int _locationId;
	/** Link to NPC spawn territory */
	private NpcSpawnTerritory _spawnTerritory = null;
	/** Minimum respawn delay */
	private int _respawnMinDelay;
	/** Maximum respawn delay */
	private int _respawnMaxDelay;
	/** Maximum distance monsters can be pulled away from spawn. */
	private int _chaseRange = 0;
	/** The generic constructor of Npc managed by this Spawn */
	private Constructor<? extends Npc> _constructor;
	/** If True an Npc is respawned each time that another is killed */
	private boolean _doRespawn = true;
	private static Set<SevenSignsFestival> _spawnListeners = ConcurrentHashMap.newKeySet(1);
	private final Deque<Npc> _spawnedNpcs = new ConcurrentLinkedDeque<>();
	private boolean _randomWalk = false; // Is random walk
	private int _spawnTemplateId = 0;
	private Location _spawnLocation = null;
	
	/**
	 * Constructor of Spawn.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Each Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...).<br>
	 * All of those properties are stored in a different NpcTemplate for each type of Spawn. Each template is loaded once in the server cache memory (reduce memory use).<br>
	 * When a new instance of Spawn is created, server just create a link between the instance and the template.<br>
	 * This link is stored in <b>_template</b> Each Npc is linked to a Spawn that manages its spawn and respawn (delay, location...).<br>
	 * This link is stored in <b>_spawn</b> of the Npc.<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <ul>
	 * <li>Set the _template of the Spawn</li>
	 * <li>Calculate the implementationName used to generate the generic constructor of Npc managed by this Spawn</li>
	 * <li>Create the generic constructor of Npc managed by this Spawn</li>
	 * </ul>
	 * @param template The NpcTemplate to link to this Spawn
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws ClassCastException when template type is not subclass of Npc
	 */
	public Spawn(NpcTemplate template) throws ClassNotFoundException, NoSuchMethodException, ClassCastException
	{
		super(0, 0, -10000);
		
		// Set the _template of the Spawn
		_template = template;
		
		if (_template == null)
		{
			return;
		}
		
		_constructor = Class.forName("org.l2jmobius.gameserver.model.actor.instance." + _template.getType()).asSubclass(Npc.class).getConstructor(NpcTemplate.class);
	}
	
	/**
	 * Creates a spawn.
	 * @param npcId the NPC ID
	 * @throws ClassCastException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 */
	public Spawn(int npcId) throws ClassNotFoundException, NoSuchMethodException, ClassCastException
	{
		this(NpcData.getInstance().getTemplate(npcId));
	}
	
	/**
	 * @return the maximum number of Npc that this Spawn can manage.
	 */
	public int getAmount()
	{
		return _maximumCount;
	}
	
	/**
	 * @return the String Identifier of this spawn.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Set the String Identifier of this spawn.
	 * @param name
	 */
	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * @return the Identifier of the location area where Npc can be spawned.
	 */
	public int getLocationId()
	{
		return _locationId;
	}
	
	/**
	 * Gets the NPC ID.
	 * @return the NPC ID
	 */
	public int getId()
	{
		return _template.getId();
	}
	
	/**
	 * @return min respawn delay.
	 */
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}
	
	/**
	 * @return max respawn delay.
	 */
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}
	
	/**
	 * Set the maximum number of Npc that this Spawn can manage.
	 * @param amount
	 */
	public void setAmount(int amount)
	{
		_maximumCount = amount;
	}
	
	/**
	 * Set the Identifier of the location area where Npc can be spawned.
	 * @param id
	 */
	public void setLocationId(int id)
	{
		_locationId = id;
	}
	
	/**
	 * Set Minimum Respawn Delay.
	 * @param date
	 */
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}
	
	/**
	 * Set Maximum Respawn Delay.
	 * @param date
	 */
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	
	/**
	 * Decrease the current number of Npc of this Spawn and if necessary create a SpawnTask to launch after the respawn Delay. <b><u>Actions</u>:</b>
	 * <li>Decrease the current number of Npc of this Spawn</li>
	 * <li>Check if respawn is possible to prevent multiple respawning caused by lag</li>
	 * <li>Update the current number of SpawnTask in progress or stand by of this Spawn</li>
	 * <li>Create a new SpawnTask to launch after the respawn Delay</li> <font color=#FF0000><b><u>Caution</u>: A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</b></font>
	 * @param oldNpc
	 */
	public void decreaseCount(Npc oldNpc)
	{
		// sanity check
		if (_currentCount <= 0)
		{
			return;
		}
		
		// Decrease the current number of Npc of this Spawn
		_currentCount--;
		
		// Remove this NPC from list of spawned
		_spawnedNpcs.remove(oldNpc);
		
		// Check if respawn is possible to prevent multiple respawning caused by lag
		if (_doRespawn && ((_scheduledCount + _currentCount) < _maximumCount))
		{
			// Update the current number of SpawnTask in progress or stand by of this Spawn
			_scheduledCount++;
			
			// Schedule the next respawn.
			RespawnTaskManager.getInstance().add(oldNpc, System.currentTimeMillis() + (hasRespawnRandom() ? Rnd.get(_respawnMinDelay, _respawnMaxDelay) : _respawnMinDelay));
		}
	}
	
	/**
	 * Create the initial spawning and set _doRespawn to False, if respawn time set to 0, or set it to True otherwise.
	 * @return The number of Npc that were spawned
	 */
	public int init()
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn();
		}
		
		_doRespawn = _respawnMinDelay > 0;
		
		return _currentCount;
	}
	
	/**
	 * @return true if respawn enabled
	 */
	public boolean isRespawnEnabled()
	{
		return _doRespawn;
	}
	
	/**
	 * Set _doRespawn to False to stop respawn in this Spawn.
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}
	
	/**
	 * Set _doRespawn to True to start or restart respawn in this Spawn.
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}
	
	public Npc doSpawn()
	{
		return _doRespawn ? doSpawn(false) : null;
	}
	
	/**
	 * Create the Npc, add it to the world and lauch its OnSpawn action.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * Npc can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.<br>
	 * The heading of the Npc can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<br>
	 * <br>
	 * <b><u>Actions for an random spawn into location area</u>:<i> (if Locx=0 and Locy=0)</i></b>
	 * <ul>
	 * <li>Get Npc Init parameters and its generate an Identifier</li>
	 * <li>Call the constructor of the Npc</li>
	 * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the Spawn</li>
	 * <li>Set the position of the Npc</li>
	 * <li>Set the HP and MP of the Npc to the max</li>
	 * <li>Set the heading of the Npc (random heading if not defined : value=-1)</li>
	 * <li>Link the Npc to this Spawn</li>
	 * <li>Init other values of the Npc (ex : from its CreatureTemplate for INT, STR, DEX...) and add it in the world</li>
	 * <li>Launch the action OnSpawn fo the Npc</li>
	 * <li>Increase the current number of Npcs managed by this Spawn</li>
	 * </ul>
	 * @param isSummonSpawn
	 * @return
	 */
	public Npc doSpawn(boolean isSummonSpawn)
	{
		try
		{
			// Check if the spawn is not a Pet, Decoy or Trap spawn.
			if (_template.isType("Pet") || _template.isType("Decoy") || _template.isType("Trap"))
			{
				_currentCount++;
				return null;
			}
			
			// Call the constructor of the Npc
			final Npc npc = _constructor.newInstance(_template);
			npc.setInstanceId(getInstanceId()); // Must be done before object is spawned into visible world
			if (isSummonSpawn)
			{
				npc.setShowSummonAnimation(isSummonSpawn);
			}
			
			return initializeNpc(npc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "NPC " + _template.getId() + " class not found", e);
		}
		
		return null;
	}
	
	/**
	 * @param npc
	 * @return
	 */
	private Npc initializeNpc(Npc npc)
	{
		// Make it alive
		npc.setDead(false);
		
		// Reset decay info
		npc.setDecayed(false);
		
		int newlocx = 0;
		int newlocy = 0;
		int newlocz = -10000;
		
		// If Locx and Locy are not defined, the Npc must be spawned in an area defined by location or spawn territory.
		if (_spawnTerritory != null)
		{
			final Location loc = _spawnTerritory.getRandomPoint();
			newlocx = loc.getX();
			newlocy = loc.getY();
			newlocz = loc.getZ();
			setLocation(loc);
			setHeading(-1);
		}
		else if ((getX() == 0) && (getY() == 0))
		{
			LOGGER.warning("NPC " + npc + " doesn't have spawn location!");
			return null;
		}
		else
		{
			// The Npc is spawned at the exact position (Lox, Locy, Locz)
			newlocx = _spawnLocation != null ? _spawnLocation.getX() : getX();
			newlocy = _spawnLocation != null ? _spawnLocation.getY() : getY();
			newlocz = _spawnLocation != null ? _spawnLocation.getZ() : getZ();
			
			if (_spawnLocation == null)
			{
				_spawnLocation = new Location(newlocx, newlocy, newlocz);
			}
		}
		
		// Check if npc is in water.
		final WaterZone water = ZoneManager.getInstance().getZone(newlocx, newlocy, newlocz, WaterZone.class);
		
		// If random spawn system is enabled.
		if (RandomSpawnsConfig.ENABLE_RANDOM_MONSTER_SPAWNS && (getHeading() != -1) && npc.isMonster() && !npc.isQuestMonster() && !WalkingManager.getInstance().isTargeted(npc) && (getInstanceId() == 0) && !getTemplate().isUndying() && !npc.isRaid() && !npc.isRaidMinion() && !npc.isFlying() && (water == null) && !RandomSpawnsConfig.MOBS_LIST_NOT_RANDOM.contains(npc.getId()))
		{
			final int randX = newlocx + Rnd.get(RandomSpawnsConfig.MOB_MIN_SPAWN_RANGE, RandomSpawnsConfig.MOB_MAX_SPAWN_RANGE);
			final int randY = newlocy + Rnd.get(RandomSpawnsConfig.MOB_MIN_SPAWN_RANGE, RandomSpawnsConfig.MOB_MAX_SPAWN_RANGE);
			if (GeoEngine.getInstance().canMoveToTarget(newlocx, newlocy, newlocz, randX, randY, newlocz, getInstanceId()) //
				&& GeoEngine.getInstance().canSeeTarget(newlocx, newlocy, newlocz, randX, randY, newlocz, getInstanceId()))
			{
				newlocx = randX;
				newlocy = randY;
				setXYZ(newlocx, newlocy, newlocz);
				setHeading(-1);
			}
		}
		
		// Correct Z of monsters.
		if (npc.isMonster() && !npc.isFlying() && (water == null))
		{
			// Do not correct Z distances greater than 300.
			final int geoZ = GeoEngine.getInstance().getHeight(newlocx, newlocy, newlocz);
			if (LocationUtil.calculateDistance(newlocx, newlocy, newlocz, newlocx, newlocy, geoZ, true, true) < 300)
			{
				newlocz = geoZ;
			}
			
			// Prevent new z exceeding spawn territory high z.
			if (_spawnTerritory != null)
			{
				final int highZ = _spawnTerritory.getHighZ();
				if (highZ < newlocz)
				{
					newlocz = highZ;
				}
			}
		}
		
		npc.stopAllEffects();
		
		// Set the HP and MP of the Npc to the max
		npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
		
		// Clear script variables
		if (npc.hasVariables())
		{
			npc.getVariables().getSet().clear();
		}
		
		// Set is not random walk default value
		npc.setRandomWalking(_randomWalk);
		
		// Set the heading of the Npc (random heading if not defined)
		if (getHeading() == -1)
		{
			npc.setHeading(Rnd.get(61794));
		}
		else
		{
			npc.setHeading(getHeading());
		}
		
		// Reset summoner
		npc.setSummoner(null);
		
		// Reset summoned list
		npc.resetSummonedNpcs();
		
		// Link the Npc to this Spawn
		npc.setSpawn(this);
		
		// Spawn NPC
		npc.spawnMe(newlocx, newlocy, newlocz);
		
		// Make sure info is broadcasted in instances
		if (npc.getInstanceId() > 0)
		{
			npc.broadcastInfo();
		}
		
		notifyNpcSpawned(npc);
		
		// Check for overriden by spawnlist AIData
		if (_name != null)
		{
			NpcPersonalAIData.getInstance().initializeNpcParameters(npc, this, _name);
		}
		
		_spawnedNpcs.add(npc);
		
		// Increase the current number of Npcs managed by this Spawn
		_currentCount++;
		
		// Minions
		if (npc.isMonster() && NpcData.getMasterMonsterIDs().contains(npc.getId()))
		{
			npc.asMonster().getMinionList().spawnMinions(npc.getTemplate().getParameters().getMinionList("Privates"));
		}
		
		if (npc.isAttackable())
		{
			npc.asAttackable().setChampion(false);
		}
		
		// Set champion on next spawn
		if (ChampionMonstersConfig.CHAMPION_ENABLE && npc.isMonster() && !npc.isQuestMonster() && !_template.isUndying() && !npc.isRaid() && !npc.isRaidMinion() && (ChampionMonstersConfig.CHAMPION_FREQUENCY > 0) && (npc.getLevel() >= ChampionMonstersConfig.CHAMP_MIN_LEVEL) && (npc.getLevel() <= ChampionMonstersConfig.CHAMP_MAX_LEVEL) && (ChampionMonstersConfig.CHAMPION_ENABLE_IN_INSTANCES || (getInstanceId() == 0)) && (Rnd.get(100) < ChampionMonstersConfig.CHAMPION_FREQUENCY))
		{
			npc.asAttackable().setChampion(true);
		}
		
		return npc;
	}
	
	public static void addSpawnListener(SevenSignsFestival listener)
	{
		_spawnListeners.add(listener);
	}
	
	public static void removeSpawnListener(SevenSignsFestival listener)
	{
		_spawnListeners.remove(listener);
	}
	
	public static void notifyNpcSpawned(Npc npc)
	{
		for (SevenSignsFestival listener : _spawnListeners)
		{
			listener.npcSpawned(npc);
		}
	}
	
	/**
	 * Set bounds for random calculation and delay for respawn
	 * @param delay delay in seconds
	 * @param randomInterval random interval in seconds
	 */
	public void setRespawnDelay(int delay, int randomInterval)
	{
		if (delay != 0)
		{
			if (delay < 0)
			{
				LOGGER.warning("respawn delay is negative for spawn:" + this);
			}
			
			final int minDelay = delay - randomInterval;
			final int maxDelay = delay + randomInterval;
			
			_respawnMinDelay = Math.max(1, minDelay) * 1000;
			_respawnMaxDelay = Math.max(1, maxDelay) * 1000;
		}
		else
		{
			_respawnMinDelay = 0;
			_respawnMaxDelay = 0;
		}
	}
	
	public void setRespawnDelay(int delay)
	{
		setRespawnDelay(delay, 0);
	}
	
	public int getRespawnDelay()
	{
		return (_respawnMinDelay + _respawnMaxDelay) / 2;
	}
	
	public boolean hasRespawnRandom()
	{
		return _respawnMinDelay != _respawnMaxDelay;
	}
	
	public void setChaseRange(int chaseRange)
	{
		_chaseRange = chaseRange;
	}
	
	public int getChaseRange()
	{
		return _chaseRange;
	}
	
	public void setSpawnTerritory(NpcSpawnTerritory territory)
	{
		_spawnTerritory = territory;
	}
	
	public NpcSpawnTerritory getSpawnTerritory()
	{
		return _spawnTerritory;
	}
	
	public boolean isTerritoryBased()
	{
		return (_spawnTerritory != null) && (getX() == 0) && (getY() == 0);
	}
	
	public Npc getLastSpawn()
	{
		return _spawnedNpcs.peekLast();
	}
	
	public Deque<Npc> getSpawnedNpcs()
	{
		return _spawnedNpcs;
	}
	
	public void respawnNpc(Npc oldNpc)
	{
		if (_doRespawn)
		{
			// oldNpc.refreshId();
			initializeNpc(oldNpc);
		}
	}
	
	public NpcTemplate getTemplate()
	{
		return _template;
	}
	
	public boolean getRandomWalking()
	{
		return _randomWalk;
	}
	
	public void setRandomWalking(boolean value)
	{
		_randomWalk = value;
	}
	
	public void setSpawnTemplateId(int npcSpawnTemplateId)
	{
		_spawnTemplateId = npcSpawnTemplateId;
	}
	
	public int getNpcSpawnTemplateId()
	{
		return _spawnTemplateId;
	}
	
	public Location getSpawnLocation()
	{
		return _spawnLocation;
	}
	
	@Override
	public String toString()
	{
		return "Spawn ID: " + _template.getId() + " X: " + getX() + " Y: " + getY() + " Z: " + getZ() + " Heading: " + getHeading();
	}
}
