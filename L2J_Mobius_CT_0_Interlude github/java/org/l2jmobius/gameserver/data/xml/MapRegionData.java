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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.model.zone.type.RespawnZone;

/**
 * Manages map regions, spawn points and teleportation locations across the game world.<br>
 * Handles player respawning based on location, karma status, clan ownership and race restrictions.
 * <ul>
 * <li>Region-based spawn point management for towns and special locations.</li>
 * <li>Karma and chaotic player respawn handling.</li>
 * <li>Castle and clan hall teleportation.</li>
 * <li>Race-based region restrictions and alternate spawn points.</li>
 * </ul>
 * @author Mobius
 */
public class MapRegionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MapRegionData.class.getName());
	
	// Constants.
	private static final String DEFAULT_RESPAWN = "talking_island_town";
	private static final String DEFAULT_TOWN = "Aden Castle Town";
	private static final int MAP_REGION_X_OFFSET = 20; // (9 + 11) center tile offset.
	private static final int MAP_REGION_Y_OFFSET = 18; // (10 + 8) center tile offset.
	private static final int COORDINATE_SHIFT = 15; // Bit shift for region calculation.
	private static final int GRID_SIZE = 32; // Spatial grid dimensions for fast lookups.
	
	/**
	 * Compact region data holder to improve cache locality and reduce memory overhead.<br>
	 * All region properties stored together in a single object for efficient access.<br>
	 * Package-private to keep implementation details hidden.
	 */
	private static class RegionData
	{
		final String town;
		final int locId;
		final int castle;
		final int bbs;
		List<int[]> maps;
		List<Location> spawnLocs;
		List<Location> otherSpawnLocs;
		List<Location> chaoticSpawnLocs;
		List<Location> banishSpawnLocs;
		Map<Race, String> bannedRaces;
		
		RegionData(String town, int locId, int castle, int bbs)
		{
			this.town = town;
			this.locId = locId;
			this.castle = castle;
			this.bbs = bbs;
		}
	}
	
	// Primary region storage - single HashMap for all regions.
	private final Map<String, RegionData> _regions = new HashMap<>();
	
	// Spatial index for O(1) coordinate-to-region lookups.
	private final RegionData[][] _spatialGrid = new RegionData[GRID_SIZE][GRID_SIZE];
	
	// Cached frequently accessed regions to avoid HashMap lookups.
	private RegionData _defaultRespawnRegion;
	
	protected MapRegionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_regions.clear();
		
		// Clear spatial grid.
		for (int i = 0; i < GRID_SIZE; i++)
		{
			for (int j = 0; j < GRID_SIZE; j++)
			{
				_spatialGrid[i][j] = null;
			}
		}
		
		parseDatapackDirectory("data/mapregion", false);
		
		// Build spatial index after loading all regions.
		buildSpatialIndex();
		
		// Cache frequently accessed regions.
		_defaultRespawnRegion = _regions.get(DEFAULT_RESPAWN);
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _regions.size() + " map regions.");
	}
	
	/**
	 * Builds spatial grid index for O(1) coordinate lookups.<br>
	 * Maps region coordinates directly to grid cells for instant access.
	 */
	private void buildSpatialIndex()
	{
		for (RegionData region : _regions.values())
		{
			if (region.maps != null)
			{
				for (int[] map : region.maps)
				{
					final int x = map[0];
					final int y = map[1];
					if ((x >= 0) && (x < GRID_SIZE) && (y >= 0) && (y < GRID_SIZE))
					{
						_spatialGrid[x][y] = region;
					}
				}
			}
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		String name;
		String town;
		int locId;
		int castle;
		int bbs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("region".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						name = attrs.getNamedItem("name").getNodeValue();
						town = attrs.getNamedItem("town").getNodeValue();
						locId = parseInteger(attrs, "locId");
						castle = parseInteger(attrs, "castle");
						bbs = parseInteger(attrs, "bbs");
						
						final RegionData region = new RegionData(town, locId, castle, bbs);
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("respawnPoint".equalsIgnoreCase(c.getNodeName()))
							{
								final int spawnX = parseInteger(attrs, "X");
								final int spawnY = parseInteger(attrs, "Y");
								final int spawnZ = parseInteger(attrs, "Z");
								final boolean other = parseBoolean(attrs, "isOther", false);
								final boolean chaotic = parseBoolean(attrs, "isChaotic", false);
								final boolean banish = parseBoolean(attrs, "isBanish", false);
								if (other)
								{
									if (region.otherSpawnLocs == null)
									{
										region.otherSpawnLocs = new ArrayList<>();
									}
									
									region.otherSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
								else if (chaotic)
								{
									if (region.chaoticSpawnLocs == null)
									{
										region.chaoticSpawnLocs = new ArrayList<>();
									}
									
									region.chaoticSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
								else if (banish)
								{
									if (region.banishSpawnLocs == null)
									{
										region.banishSpawnLocs = new ArrayList<>();
									}
									
									region.banishSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
								else
								{
									if (region.spawnLocs == null)
									{
										region.spawnLocs = new ArrayList<>();
									}
									
									region.spawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
							}
							else if ("map".equalsIgnoreCase(c.getNodeName()))
							{
								if (region.maps == null)
								{
									region.maps = new ArrayList<>();
								}
								
								region.maps.add(new int[]
								{
									parseInteger(attrs, "X"),
									parseInteger(attrs, "Y")
								});
							}
							else if ("banned".equalsIgnoreCase(c.getNodeName()))
							{
								if (region.bannedRaces == null)
								{
									region.bannedRaces = new EnumMap<>(Race.class);
								}
								
								region.bannedRaces.put(Race.valueOf(attrs.getNamedItem("race").getNodeValue()), attrs.getNamedItem("point").getNodeValue());
							}
						}
						
						_regions.put(name, region);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a normal spawn location for the specified region.<br>
	 * Returns random spawn if configuration enabled, otherwise first spawn point.
	 * @param region
	 * @return the spawn location or {@code null} if region has no spawns.
	 */
	private Location getSpawnLoc(RegionData region)
	{
		if (region.spawnLocs == null)
		{
			return null;
		}
		
		if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
		{
			return region.spawnLocs.get(Rnd.get(region.spawnLocs.size()));
		}
		
		return region.spawnLocs.get(0);
	}
	
	/**
	 * Retrieves a chaotic spawn location for karma players in the specified region.<br>
	 * Falls back to normal spawn if no chaotic spawn exists.
	 * @param region
	 * @return the spawn location.
	 */
	private Location getChaoticSpawnLoc(RegionData region)
	{
		if (region.chaoticSpawnLocs != null)
		{
			if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
			{
				return region.chaoticSpawnLocs.get(Rnd.get(region.chaoticSpawnLocs.size()));
			}
			
			return region.chaoticSpawnLocs.get(0);
		}
		
		return getSpawnLoc(region);
	}
	
	/**
	 * Finds the region containing the specified coordinates using spatial grid for O(1) lookup.
	 * @param locX
	 * @param locY
	 * @return the region or {@code null} if no region found.
	 */
	public RegionData getMapRegion(int locX, int locY)
	{
		final int regionX = getMapRegionX(locX);
		final int regionY = getMapRegionY(locY);
		
		// O(1) lookup via spatial grid.
		if ((regionX >= 0) && (regionX < GRID_SIZE) && (regionY >= 0) && (regionY < GRID_SIZE))
		{
			return _spatialGrid[regionX][regionY];
		}
		
		return null;
	}
	
	/**
	 * Retrieves the location ID for the region at the specified coordinates.
	 * @param locX
	 * @param locY
	 * @return the location ID or {@code 0} if no region found.
	 */
	public int getMapRegionLocId(int locX, int locY)
	{
		final RegionData region = getMapRegion(locX, locY);
		return region != null ? region.locId : 0;
	}
	
	/**
	 * Finds the region containing the specified world object.
	 * @param obj
	 * @return the region or {@code null} if no region found.
	 */
	public RegionData getMapRegion(WorldObject obj)
	{
		return getMapRegion(obj.getX(), obj.getY());
	}
	
	/**
	 * Retrieves the location ID for the region containing the specified world object.
	 * @param obj
	 * @return the location ID or {@code 0} if no region found.
	 */
	public int getMapRegionLocId(WorldObject obj)
	{
		return getMapRegionLocId(obj.getX(), obj.getY());
	}
	
	/**
	 * Calculates the map region X-coordinate index from world position.
	 * @param posX
	 * @return the region X index.
	 */
	public int getMapRegionX(int posX)
	{
		return (posX >> COORDINATE_SHIFT) + MAP_REGION_X_OFFSET;
	}
	
	/**
	 * Calculates the map region Y-coordinate index from world position.
	 * @param posY
	 * @return the region Y index.
	 */
	public int getMapRegionY(int posY)
	{
		return (posY >> COORDINATE_SHIFT) + MAP_REGION_Y_OFFSET;
	}
	
	/**
	 * Retrieves the closest town name based on creature position.
	 * @param creature
	 * @return the town name or default town if no region found.
	 */
	public String getClosestTownName(Creature creature)
	{
		final RegionData region = getMapRegion(creature);
		return region == null ? DEFAULT_TOWN : region.town;
	}
	
	/**
	 * Retrieves the castle ID associated with the area where the creature is located.
	 * @param creature
	 * @return the castle ID, or {@code 0} if no region found.
	 */
	public int getAreaCastle(Creature creature)
	{
		final RegionData region = getMapRegion(creature);
		return region == null ? 0 : region.castle;
	}
	
	/**
	 * Determines the teleportation location based on creature type, status and teleport destination.<br>
	 * Handles castle, fortress, clan hall teleports, siege flags, karma spawns and faction bases.
	 * @param creature
	 * @param teleportWhere
	 * @return the teleport destination location.
	 */
	public Location getTeleToLocation(Creature creature, TeleportWhereType teleportWhere)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			
			Castle castle = null;
			ClanHall clanhall = null;
			final Clan clan = player.getClan();
			
			// Flying players in Gracia cannot use teleports to Aden continent.
			if ((clan != null) && !player.isFlying())
			{
				// Clan hall teleportation.
				if (teleportWhere == TeleportWhereType.CLANHALL)
				{
					clanhall = ClanHallTable.getInstance().getAbstractHallByOwner(clan);
					if (clanhall != null)
					{
						final ClanHallZone zone = clanhall.getZone();
						if (zone != null)
						{
							if (player.getKarma() > 0)
							{
								return zone.getChaoticSpawnLoc();
							}
							
							return zone.getSpawnLoc();
						}
					}
				}
				
				// Castle teleportation.
				if (teleportWhere == TeleportWhereType.CASTLE)
				{
					// Check if player is on castle ground and clan is defender.
					castle = CastleManager.getInstance().getCastleByOwner(clan);
					if (castle == null)
					{
						castle = CastleManager.getInstance().getCastle(player);
						if (!((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().getDefenderClan(clan) != null)))
						{
							castle = null;
						}
					}
					
					if ((castle != null) && (castle.getResidenceId() > 0))
					{
						if (player.getKarma() > 0)
						{
							return castle.getResidenceZone().getChaoticSpawnLoc();
						}
						
						return castle.getResidenceZone().getSpawnLoc();
					}
				}
				
				// Siege flag teleportation.
				if (teleportWhere == TeleportWhereType.SIEGEFLAG)
				{
					castle = CastleManager.getInstance().getCastle(player);
					if (castle != null)
					{
						if (castle.getSiege().isInProgress())
						{
							final Set<Npc> flags = castle.getSiege().getFlag(clan);
							if ((flags != null) && !flags.isEmpty())
							{
								return flags.stream().findAny().get().getLocation();
							}
						}
					}
				}
			}
			
			// Karma player respawn outside city.
			if (player.getKarma() > 0)
			{
				return getNearestKarmaRespawn(player);
			}
			
			// Respawn far from castle during siege if clan is participating.
			castle = CastleManager.getInstance().getCastle(player);
			if ((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().checkIsDefender(clan) || castle.getSiege().checkIsAttacker(clan)) && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN))
			{
				return castle.getResidenceZone().getOtherSpawnLoc();
			}
			
			// Instance exit location.
			if (player.getInstanceId() > 0)
			{
				final Instance inst = InstanceManager.getInstance().getInstance(player.getInstanceId());
				if (inst != null)
				{
					final Location loc = inst.getExitLoc();
					if (loc != null)
					{
						return loc;
					}
				}
			}
			
			// Faction system base respawn.
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_RESPAWN_AT_BASE)
			{
				if (player.isGood())
				{
					return FactionSystemConfig.FACTION_GOOD_BASE_LOCATION;
				}
				
				if (player.isEvil())
				{
					return FactionSystemConfig.FACTION_EVIL_BASE_LOCATION;
				}
			}
		}
		
		return getNearestTownRespawn(creature);
	}
	
	/**
	 * Retrieves the nearest karma respawn location for a player with negative reputation.<br>
	 * Checks respawn zones first, then falls back to region-based spawns.
	 * @param player
	 * @return the karma respawn location.
	 */
	public Location getNearestKarmaRespawn(Player player)
	{
		try
		{
			final RespawnZone zone = ZoneManager.getInstance().getZone(player, RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegionChaoticSpawnLoc(player, zone.getRespawnPoint(player));
			}
			
			final RegionData region = getMapRegion(player);
			if (region != null)
			{
				// Check for banned races and redirect to alternate region.
				if ((region.bannedRaces != null) && region.bannedRaces.containsKey(player.getRace()))
				{
					final String alternateRegionName = region.bannedRaces.get(player.getRace());
					final RegionData alternateRegion = _regions.get(alternateRegionName);
					if (alternateRegion != null)
					{
						return getChaoticSpawnLoc(alternateRegion);
					}
				}
				
				return getChaoticSpawnLoc(region);
			}
		}
		catch (Exception e)
		{
			// Fall through to default handling.
		}
		
		// Use cached default region.
		if (_defaultRespawnRegion != null)
		{
			return getChaoticSpawnLoc(_defaultRespawnRegion);
		}
		
		return null;
	}
	
	/**
	 * Retrieves the nearest town respawn location for the specified creature.<br>
	 * Checks respawn zones first, then falls back to region-based spawns.
	 * @param creature
	 * @return the town respawn location.
	 */
	public Location getNearestTownRespawn(Creature creature)
	{
		try
		{
			final RespawnZone zone = ZoneManager.getInstance().getZone(creature, RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegionSpawnLoc(creature, zone.getRespawnPoint(creature.asPlayer()));
			}
			
			final RegionData region = getMapRegion(creature);
			if (region != null)
			{
				// Check for banned races and redirect to alternate region.
				if (creature.isPlayer())
				{
					final Player player = creature.asPlayer();
					if ((region.bannedRaces != null) && region.bannedRaces.containsKey(player.getRace()))
					{
						final String alternateRegionName = region.bannedRaces.get(player.getRace());
						final RegionData alternateRegion = _regions.get(alternateRegionName);
						if (alternateRegion != null)
						{
							return getSpawnLoc(alternateRegion);
						}
					}
				}
				
				return getSpawnLoc(region);
			}
		}
		catch (Exception e)
		{
			// Fall through to default respawn.
		}
		
		// Use cached default region.
		if (_defaultRespawnRegion != null)
		{
			return getSpawnLoc(_defaultRespawnRegion);
		}
		
		return null;
	}
	
	/**
	 * Retrieves the restart region spawn location accounting for race restrictions.<br>
	 * Redirects to alternate region if creature's race is banned.
	 * @param creature
	 * @param point
	 * @return the restart spawn location.
	 */
	public Location getRestartRegionSpawnLoc(Creature creature, String point)
	{
		try
		{
			final Player player = creature.asPlayer();
			RegionData region = _regions.get(point);
			if (region == null)
			{
				region = _defaultRespawnRegion;
			}
			
			if ((region.bannedRaces != null) && region.bannedRaces.containsKey(player.getRace()))
			{
				return getRestartRegionSpawnLoc(player, region.bannedRaces.get(player.getRace()));
			}
			
			return getSpawnLoc(region);
		}
		catch (Exception e)
		{
			if (_defaultRespawnRegion != null)
			{
				return getSpawnLoc(_defaultRespawnRegion);
			}
			
			return null;
		}
	}
	
	/**
	 * Retrieves the restart region chaotic spawn location accounting for race restrictions.<br>
	 * Redirects to alternate region if creature's race is banned.
	 * @param creature
	 * @param point
	 * @return the restart chaotic spawn location.
	 */
	public Location getRestartRegionChaoticSpawnLoc(Creature creature, String point)
	{
		try
		{
			final Player player = creature.asPlayer();
			RegionData region = _regions.get(point);
			if (region == null)
			{
				region = _defaultRespawnRegion;
			}
			
			if ((region.bannedRaces != null) && region.bannedRaces.containsKey(player.getRace()))
			{
				return getRestartRegionChaoticSpawnLoc(player, region.bannedRaces.get(player.getRace()));
			}
			
			return getChaoticSpawnLoc(region);
		}
		catch (Exception e)
		{
			if (_defaultRespawnRegion != null)
			{
				return getChaoticSpawnLoc(_defaultRespawnRegion);
			}
			
			return null;
		}
	}
	
	/**
	 * Retrieves the location ID for the restart region based on creature's race and point.<br>
	 * If the creature's race is banned in the specified region, an alternate region is provided.
	 * @param creature
	 * @param point
	 * @return the location ID of the restart region, or {@code 0} if not found.
	 */
	public int getRestartRegionLocId(Creature creature, String point)
	{
		try
		{
			final Player player = creature.asPlayer();
			RegionData region = _regions.get(point);
			if (region == null)
			{
				return _defaultRespawnRegion != null ? _defaultRespawnRegion.locId : 0;
			}
			
			if ((region.bannedRaces != null) && region.bannedRaces.containsKey(player.getRace()))
			{
				return getRestartRegionLocId(player, region.bannedRaces.get(player.getRace()));
			}
			
			return region.locId;
		}
		catch (Exception e)
		{
			return _defaultRespawnRegion != null ? _defaultRespawnRegion.locId : 0;
		}
	}
	
	/**
	 * Retrieves the location ID for the restart region based on player's race and point.<br>
	 * Convenience overload for Player parameter.
	 * @param player
	 * @param point
	 * @return the location ID of the restart region, or {@code 0} if not found.
	 */
	public int getRestartRegionLocId(Player player, String point)
	{
		return getRestartRegionLocId((Creature) player, point);
	}
	
	/**
	 * Retrieves the spawn location for the specified region name.<br>
	 * Returns random spawn if configuration enabled, otherwise first spawn point.
	 * @param regionName
	 * @return the spawn location or {@code null} if region not found or has no spawns.
	 */
	public Location getSpawnLocByRegionName(String regionName)
	{
		final RegionData region = _regions.get(regionName);
		if (region == null)
		{
			return null;
		}
		
		if (region.spawnLocs == null)
		{
			return null;
		}
		
		if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
		{
			return region.spawnLocs.get(Rnd.get(region.spawnLocs.size()));
		}
		
		return region.spawnLocs.get(0);
	}
	
	/**
	 * Retrieves the town name for the specified region.
	 * @param regionName
	 * @return the town name or {@code null} if region not found.
	 */
	public String getMapRegionTown(String regionName)
	{
		final RegionData region = _regions.get(regionName);
		return region != null ? region.town : null;
	}
	
	/**
	 * Retrieves the bulletin board system ID for the specified location.<br>
	 * Falls back to default respawn region BBS if location has no region.
	 * @param loc
	 * @return the BBS ID.
	 */
	public int getBBs(ILocational loc)
	{
		final RegionData region = getMapRegion(loc.getX(), loc.getY());
		if (region != null)
		{
			return region.bbs;
		}
		
		// Use cached default region.
		return _defaultRespawnRegion != null ? _defaultRespawnRegion.bbs : 0;
	}
	
	public static MapRegionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MapRegionData INSTANCE = new MapRegionData();
	}
}
