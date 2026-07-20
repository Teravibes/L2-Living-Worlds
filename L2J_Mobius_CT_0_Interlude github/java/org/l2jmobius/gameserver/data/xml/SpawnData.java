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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.managers.DayNightSpawnManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.model.zone.ZoneForm;
import org.l2jmobius.gameserver.model.zone.form.ZoneCuboid;
import org.l2jmobius.gameserver.model.zone.form.ZoneCylinder;
import org.l2jmobius.gameserver.model.zone.form.ZoneNPoly;
import org.l2jmobius.gameserver.model.zone.type.NpcSpawnTerritory;

/**
 * @author Mobius
 */
public class SpawnData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SpawnData.class.getName());
	
	private static final String OTHER_XML_FOLDER = "data/spawns/Others";
	
	private final Map<Integer, String> _spawnTemplates = new ConcurrentHashMap<>();
	private int _spawnCount = 0;
	
	protected SpawnData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_spawnTemplates.put(0, "None");
		if (!DevelopmentConfig.NO_SPAWNS)
		{
			LOGGER.info(getClass().getSimpleName() + ": Initializing spawns...");
			parseDatapackDirectory("data/spawns", true);
			LOGGER.info(getClass().getSimpleName() + ": " + _spawnCount + " spawns have been initialized!");
		}
	}
	
	/**
	 * Verifies if the template exists and it's spawnable.
	 * @param npcId the NPC ID
	 * @return {@code true} if the NPC ID belongs to an spawnable tempalte, {@code false} otherwise
	 */
	private boolean checkTemplate(int npcId)
	{
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
		if (npcTemplate == null)
		{
			// Log a warning for invalid NPC IDs outside the fake player range [80000, 89999].
			// This hardcoded check provides a resource-constrained solution to prevent unnecessary logging.
			if (FakePlayersConfig.FAKE_PLAYERS_ENABLED || ((npcId < 80000) && (npcId > 89999)))
			{
				LOGGER.warning(getClass().getSimpleName() + ": Requested spawn for non-existing NPC: " + npcId + ".");
			}
			
			return false;
		}
		
		if (npcTemplate.isType("SiegeGuard") || npcTemplate.isType("RaidBoss"))
		{
			// Do not spawn.
			return false;
		}
		
		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED && npcTemplate.isFakePlayer())
		{
			return false;
		}
		
		return true;
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		for (Node list = document.getFirstChild(); list != null; list = list.getNextSibling())
		{
			if (list.getNodeName().equalsIgnoreCase("list"))
			{
				attrs = list.getAttributes();
				
				// Skip disabled spawnlists.
				if (!Boolean.parseBoolean(attrs.getNamedItem("enabled").getNodeValue()))
				{
					continue;
				}
				
				for (Node param = list.getFirstChild(); param != null; param = param.getNextSibling())
				{
					attrs = param.getAttributes();
					if (param.getNodeName().equalsIgnoreCase("spawn"))
					{
						String territoryName = null;
						String spawnName = null;
						Map<String, Integer> map = null;
						
						// Check, if spawn name specified.
						if (attrs.getNamedItem("name") != null)
						{
							spawnName = parseString(attrs, "name");
						}
						
						// Check, if spawn territory specified.
						if (attrs.getNamedItem("zone") != null)
						{
							territoryName = parseString(attrs, "zone");
						}
						
						for (Node npctag = param.getFirstChild(); npctag != null; npctag = npctag.getNextSibling())
						{
							attrs = npctag.getAttributes();
							
							// Check if there are any AI parameters.
							if (npctag.getNodeName().equalsIgnoreCase("AIData"))
							{
								attrs = npctag.getAttributes();
								if (map == null)
								{
									map = new HashMap<>();
								}
								
								for (Node c = npctag.getFirstChild(); c != null; c = c.getNextSibling())
								{
									// Skip odd nodes.
									if (c.getNodeName().equals("#text"))
									{
										continue;
									}
									
									int val;
									switch (c.getNodeName())
									{
										case "disableRandomAnimation":
										case "disableRandomWalk":
										{
											val = Boolean.parseBoolean(c.getTextContent()) ? 1 : 0;
											break;
										}
										default:
										{
											val = Integer.parseInt(c.getTextContent());
										}
									}
									
									map.put(c.getNodeName(), val);
								}
							}
							// Check for NPC spawn territories.
							else if (npctag.getNodeName().equalsIgnoreCase("territory"))
							{
								if (ZoneManager.getInstance().spawnTerritoryExists(territoryName))
								{
									continue;
								}
								
								final int minZ = parseInteger(attrs, "minZ");
								final int maxZ = parseInteger(attrs, "maxZ");
								final String zoneShape = parseString(attrs, "shape", "NPoly");
								
								final List<int[]> rs = new ArrayList<>();
								int[][] coords;
								ZoneForm zoneForm = null;
								try
								{
									for (Node c = npctag.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("node".equalsIgnoreCase(c.getNodeName()))
										{
											attrs = c.getAttributes();
											final int[] point = new int[2];
											point[0] = parseInteger(attrs, "x");
											point[1] = parseInteger(attrs, "y");
											rs.add(point);
										}
									}
									
									coords = rs.toArray(new int[rs.size()][2]);
									rs.clear();
									
									if ((coords == null) || (coords.length == 0))
									{
										LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: missing data for spawn territory: " + territoryName + " XML file: " + file.getName());
										continue;
									}
									
									// Create this zone. Parsing for cuboids is a bit different than for other polygons cuboids need exactly 2 points to be defined.
									// Other polygons need at least 3 (one per vertex).
									if (zoneShape.equalsIgnoreCase("Cuboid"))
									{
										if (coords.length == 2)
										{
											zoneForm = new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ);
										}
										else
										{
											LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Missing cuboid vertex data for territory: " + territoryName + " in file: " + file.getName());
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("NPoly"))
									{
										// nPoly needs to have at least 3 vertices.
										if (coords.length > 2)
										{
											final int[] aX = new int[coords.length];
											final int[] aY = new int[coords.length];
											for (int i = 0; i < coords.length; i++)
											{
												aX[i] = coords[i][0];
												aY[i] = coords[i][1];
											}
											
											zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
										}
										else
										{
											LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Bad data for territory: " + territoryName + " in file: " + file.getName());
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("Cylinder"))
									{
										// A Cylinder zone requires a center point at x,y and a radius.
										final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
										if ((coords.length == 1) && (zoneRad > 0))
										{
											zoneForm = new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad);
										}
										else
										{
											LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Bad data for territory: " + territoryName + " in file: " + file.getName());
											continue;
										}
									}
									else
									{
										LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Unknown shape: \"" + zoneShape + "\"  for territory: " + territoryName + " in file: " + file.getName());
										continue;
									}
								}
								catch (Exception e)
								{
									LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": SpawnTable: Failed to load territory " + territoryName + " coordinates: " + e.getMessage(), e);
								}
								
								ZoneManager.getInstance().addSpawnTerritory(territoryName, new NpcSpawnTerritory(territoryName, zoneForm));
							}
							// Check for NPC banned spawn territories.
							else if (npctag.getNodeName().equalsIgnoreCase("banned_territory"))
							{
								if (!ZoneManager.getInstance().spawnTerritoryExists(territoryName))
								{
									LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: banned_territory " + territoryName + " must be set after normal territory.");
									continue;
								}
								
								final int minZ = parseInteger(attrs, "minZ");
								final int maxZ = parseInteger(attrs, "maxZ");
								final String zoneShape = parseString(attrs, "shape", "NPoly");
								
								final List<int[]> rs = new ArrayList<>();
								int[][] coords;
								ZoneForm zoneForm = null;
								try
								{
									for (Node c = npctag.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("node".equalsIgnoreCase(c.getNodeName()))
										{
											attrs = c.getAttributes();
											final int[] point = new int[2];
											point[0] = parseInteger(attrs, "x");
											point[1] = parseInteger(attrs, "y");
											rs.add(point);
										}
									}
									
									coords = rs.toArray(new int[rs.size()][2]);
									rs.clear();
									
									if ((coords == null) || (coords.length == 0))
									{
										LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: missing data for spawn banned_territory: " + territoryName + " XML file: " + file.getName());
										continue;
									}
									
									// Create this zone. Parsing for cuboids is a bit different than for other polygons cuboids need exactly 2 points to be defined.
									// Other polygons need at least 3 (one per vertex).
									if (zoneShape.equalsIgnoreCase("Cuboid"))
									{
										if (coords.length == 2)
										{
											zoneForm = new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ);
										}
										else
										{
											LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Missing cuboid vertex data for banned_territory: " + territoryName + " in file: " + file.getName());
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("NPoly"))
									{
										// nPoly needs to have at least 3 vertices.
										if (coords.length > 2)
										{
											final int[] aX = new int[coords.length];
											final int[] aY = new int[coords.length];
											for (int i = 0; i < coords.length; i++)
											{
												aX[i] = coords[i][0];
												aY[i] = coords[i][1];
											}
											
											zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
										}
										else
										{
											LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Bad data for banned_territory: " + territoryName + " in file: " + file.getName());
											continue;
										}
									}
									else if (zoneShape.equalsIgnoreCase("Cylinder"))
									{
										// A Cylinder zone requires a center point at x,y and a radius.
										final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
										if ((coords.length == 1) && (zoneRad > 0))
										{
											zoneForm = new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad);
										}
										else
										{
											LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Bad data for banned_territory: " + territoryName + " in file: " + file.getName());
											continue;
										}
									}
									else
									{
										LOGGER.warning(getClass().getSimpleName() + ": SpawnTable: Unknown shape: \"" + zoneShape + "\"  for banned_territory: " + territoryName + " in file: " + file.getName());
										continue;
									}
								}
								catch (Exception e)
								{
									LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": SpawnTable: Failed to load banned_territory " + territoryName + " coordinates: " + e.getMessage(), e);
								}
								
								ZoneManager.getInstance().getSpawnTerritory(territoryName).addBannedTerritory(zoneForm);
							}
							// Check for NPC spawns.
							else if (npctag.getNodeName().equalsIgnoreCase("npc"))
							{
								// Mandatory.
								final int templateId = parseInteger(attrs, "id");
								
								// Avoid spawning unwanted spawns.
								if (!checkTemplate(templateId))
								{
									continue;
								}
								
								// Coordinates are optional, if territory is specified; mandatory otherwise.
								int x = 0;
								int y = 0;
								int z = 0;
								
								try
								{
									x = parseInteger(attrs, "x");
									y = parseInteger(attrs, "y");
									z = parseInteger(attrs, "z");
								}
								catch (NullPointerException npe)
								{
									// x, y, z can be unspecified, if this spawn is territory based, do nothing.
								}
								
								if ((x == 0) && (y == 0) && (territoryName == null)) // Both coordinates and zone are unspecified.
								{
									LOGGER.warning("XML Spawnlist: Spawn could not be initialized, both coordinates and zone are unspecified for ID " + templateId);
									continue;
								}
								
								final Node countNode = attrs.getNamedItem("count");
								final int count = countNode == null ? 1 : parseInteger(attrs, "count");
								
								for (int i = 0; i < count; i++)
								{
									final StatSet spawnInfo = new StatSet();
									spawnInfo.set("npcTemplateid", templateId);
									spawnInfo.set("x", x);
									spawnInfo.set("y", y);
									spawnInfo.set("z", z);
									spawnInfo.set("territoryName", territoryName);
									spawnInfo.set("spawnName", spawnName);
									
									// Trying to read optional parameters.
									if (attrs.getNamedItem("heading") != null)
									{
										spawnInfo.set("heading", parseInteger(attrs, "heading"));
									}
									
									if (attrs.getNamedItem("respawnDelay") != null)
									{
										spawnInfo.set("respawnDelay", parseInteger(attrs, "respawnDelay"));
									}
									
									if (attrs.getNamedItem("respawnRandom") != null)
									{
										spawnInfo.set("respawnRandom", parseInteger(attrs, "respawnRandom"));
									}
									
									if (attrs.getNamedItem("chaseRange") != null)
									{
										spawnInfo.set("chaseRange", parseInteger(attrs, "chaseRange"));
									}
									
									if (attrs.getNamedItem("periodOfDay") != null)
									{
										final String period = attrs.getNamedItem("periodOfDay").getNodeValue();
										if (period.equalsIgnoreCase("day") || period.equalsIgnoreCase("night"))
										{
											spawnInfo.set("periodOfDay", period.equalsIgnoreCase("day") ? 1 : 2);
										}
									}
									
									spawnInfo.set("fileName", file.getPath());
									_spawnCount += addSpawn(spawnInfo, map);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Creates NPC spawn
	 * @param spawnInfo StatSet of spawn parameters
	 * @param aiData Map of specific AI parameters for this spawn
	 * @return count NPC instances, spawned by this spawn
	 */
	private int addSpawn(StatSet spawnInfo, Map<String, Integer> aiData)
	{
		Spawn spawnDat;
		int ret = 0;
		try
		{
			spawnDat = new Spawn(spawnInfo.getInt("npcTemplateid"));
			spawnDat.setAmount(spawnInfo.getInt("count", 1));
			spawnDat.setXYZ(spawnInfo.getInt("x", 0), spawnInfo.getInt("y", 0), spawnInfo.getInt("z", 0));
			spawnDat.setHeading(spawnInfo.getInt("heading", -1));
			spawnDat.setRespawnDelay(spawnInfo.getInt("respawnDelay", 0), spawnInfo.getInt("respawnRandom", 0));
			spawnDat.setChaseRange(spawnInfo.getInt("chaseRange", 0));
			spawnDat.setLocationId(spawnInfo.getInt("locId", 0));
			final String territoryName = spawnInfo.getString("territoryName", "");
			final String spawnName = spawnInfo.getString("spawnName", "");
			if (!spawnName.isEmpty())
			{
				spawnDat.setName(spawnName);
			}
			
			if (!territoryName.isEmpty())
			{
				spawnDat.setSpawnTerritory(ZoneManager.getInstance().getSpawnTerritory(territoryName));
			}
			
			// Register AI Data for this spawn.
			NpcPersonalAIData.getInstance().storeData(spawnDat, aiData);
			switch (spawnInfo.getInt("periodOfDay", 0))
			{
				case 0: // Default
				{
					ret += spawnDat.init();
					break;
				}
				case 1: // Day
				{
					DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
					ret = 1;
					break;
				}
				case 2: // Night
				{
					DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
					ret = 1;
					break;
				}
			}
			
			final String fileName = spawnInfo.getString("fileName", "None");
			if (_spawnTemplates.values().contains(fileName))
			{
				for (Entry<Integer, String> entry : _spawnTemplates.entrySet())
				{
					if (entry.getValue().equals(fileName))
					{
						spawnDat.setSpawnTemplateId(entry.getKey());
						break;
					}
				}
			}
			else
			{
				final int newId = _spawnTemplates.size();
				_spawnTemplates.put(newId, fileName);
				spawnDat.setSpawnTemplateId(newId);
			}
			
			SpawnTable.getInstance().addSpawn(spawnDat);
		}
		catch (Exception e)
		{
			// Problem with initializing spawn, go to next one.
			LOGGER.log(Level.WARNING, "Spawn could not be initialized: " + e.getMessage(), e);
		}
		
		return ret;
	}
	
	/**
	 * Adds a new spawn to the {@link SpawnTable} and writes its details to an XML file.<br>
	 * If the XML file for the corresponding coordinates already exists, the spawn is appended to it.<br>
	 * If the file does not exist, a new one is created.
	 * @param spawn the {@link Spawn} object to add.
	 */
	public synchronized void addNewSpawn(Spawn spawn)
	{
		SpawnTable.getInstance().addSpawn(spawn);
		
		// Create output directory if it doesn't exist.
		final File outputDirectory = new File(OTHER_XML_FOLDER);
		if (!outputDirectory.exists())
		{
			boolean result = false;
			try
			{
				outputDirectory.mkdir();
				result = true;
			}
			catch (SecurityException se)
			{
				// Ignore.
			}
			
			if (result)
			{
				LOGGER.info(getClass().getSimpleName() + ": Created directory: " + OTHER_XML_FOLDER);
			}
		}
		
		// XML file for spawn.
		final int x = ((spawn.getX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
		final int y = ((spawn.getY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
		final File spawnFile = new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
		
		// Write info to XML.
		final String spawnId = String.valueOf(spawn.getId());
		final String spawnCount = String.valueOf(spawn.getAmount());
		final String spawnX = String.valueOf(spawn.getX());
		final String spawnY = String.valueOf(spawn.getY());
		final String spawnZ = String.valueOf(spawn.getZ());
		final String spawnHeading = String.valueOf(spawn.getHeading());
		final String spawnDelay = String.valueOf(spawn.getRespawnDelay() / 1000);
		if (spawnFile.exists()) // Update.
		{
			final File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
			try
			{
				final BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
				final BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
				String currentLine;
				while ((currentLine = reader.readLine()) != null)
				{
					if (currentLine.contains("</spawn>"))
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
						final String title = template.getTitle();
						final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
						writer.write("		<npc id=\"" + spawnId + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnDelay=\"" + spawnDelay + "\" /> <!-- " + name + " -->" + System.lineSeparator());
						writer.write(currentLine + System.lineSeparator());
						continue;
					}
					
					writer.write(currentLine + System.lineSeparator());
				}
				
				writer.close();
				reader.close();
				spawnFile.delete();
				tempFile.renameTo(spawnFile);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Could not store spawn in the spawn XML files: " + e);
			}
		}
		else // New file.
		{
			try
			{
				final NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
				final String title = template.getTitle();
				final String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
				final BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile));
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
				writer.write("<list enabled=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../xsd/spawns.xsd\">" + System.lineSeparator());
				writer.write("	<spawn name=\"" + x + "_" + y + "\">" + System.lineSeparator());
				writer.write("		<npc id=\"" + spawnId + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "") + "\" x=\"" + spawnX + "\" y=\"" + spawnY + "\" z=\"" + spawnZ + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "") + "\" respawnDelay=\"" + spawnDelay + "\" /> <!-- " + name + " -->" + System.lineSeparator());
				writer.write("	</spawn>" + System.lineSeparator());
				writer.write("</list>" + System.lineSeparator());
				writer.close();
				LOGGER.info(getClass().getSimpleName() + ": Created file: " + OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be added to the spawn XML files: " + e);
			}
		}
	}
	
	/**
	 * Deletes a spawn from the {@link SpawnTable} and its associated XML entry if it exists.<br>
	 * If the file becomes empty after deletion, it is removed.
	 * @param spawn the {@link Spawn} object to delete.
	 */
	public synchronized void deleteSpawn(Spawn spawn)
	{
		SpawnTable.getInstance().removeSpawn(spawn);
		
		final int x = ((spawn.getX() - World.WORLD_X_MIN) >> 15) + World.TILE_X_MIN;
		final int y = ((spawn.getY() - World.WORLD_Y_MIN) >> 15) + World.TILE_Y_MIN;
		final int npcSpawnTemplateId = spawn.getNpcSpawnTemplateId();
		final Location spawnLocation = spawn.getSpawnLocation();
		final File spawnFile = npcSpawnTemplateId > 0 ? new File(_spawnTemplates.get(npcSpawnTemplateId)) : new File(OTHER_XML_FOLDER + "/" + x + "_" + y + ".xml");
		final File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");
		try
		{
			final BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
			final BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			
			boolean found = false; // In XML you can have more than one spawn with same coords.
			boolean isMultiLine = false; // In case spawn has more stats.
			boolean lastLineFound = false; // Used to check for empty file.
			int lineCount = 0;
			String currentLine;
			
			final NpcSpawnTerritory npcSpawnTerritory = spawn.getSpawnTerritory();
			if (npcSpawnTerritory == null)
			{
				final String spawnId = String.valueOf(spawn.getId());
				final String spawnX = String.valueOf(spawnLocation != null ? spawnLocation.getX() : spawn.getX());
				final String spawnY = String.valueOf(spawnLocation != null ? spawnLocation.getY() : spawn.getY());
				final String spawnZ = String.valueOf(spawnLocation != null ? spawnLocation.getZ() : spawn.getZ());
				
				while ((currentLine = reader.readLine()) != null)
				{
					if (!found)
					{
						if (isMultiLine)
						{
							if (currentLine.contains("</npc>"))
							{
								found = true;
							}
							continue;
						}
						
						if (currentLine.contains(spawnId) && currentLine.contains(spawnX) && currentLine.contains(spawnY) && currentLine.contains(spawnZ))
						{
							if (!currentLine.contains("/>") && !currentLine.contains("</npc>"))
							{
								isMultiLine = true;
							}
							else
							{
								found = true;
							}
							continue;
						}
					}
					
					writer.write(currentLine + System.lineSeparator());
					if (currentLine.contains("</list>"))
					{
						lastLineFound = true;
					}
					
					if (!lastLineFound)
					{
						lineCount++;
					}
				}
			}
			else
			{
				SEARCH: while ((currentLine = reader.readLine()) != null)
				{
					if (!found)
					{
						if (isMultiLine)
						{
							if (currentLine.contains("</spawn>"))
							{
								found = true;
							}
							continue;
						}
						
						if (currentLine.contains('"' + npcSpawnTerritory.getName() + '"'))
						{
							isMultiLine = true;
							continue SEARCH;
						}
					}
					
					writer.write(currentLine + System.lineSeparator());
					if (currentLine.contains("</list>"))
					{
						lastLineFound = true;
					}
					
					if (!lastLineFound)
					{
						lineCount++;
					}
				}
			}
			
			writer.close();
			reader.close();
			spawnFile.delete();
			tempFile.renameTo(spawnFile);
			
			// Delete empty file.
			if (lineCount < 5)
			{
				LOGGER.info(getClass().getSimpleName() + ": Deleted empty file: " + spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/'));
				spawnFile.delete();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Spawn " + spawn + " could not be removed from the spawn XML files: " + e);
		}
	}
	
	/**
	 * Retrieves the file path associated with the specified spawn template ID.
	 * @param templateId the unique ID of the spawn template.
	 * @return the file path corresponding to the specified template ID, or {@code null} if not found.
	 */
	public String getSpawnFile(int templateId)
	{
		return _spawnTemplates.get(templateId);
	}
	
	public static SpawnData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnData INSTANCE = new SpawnData();
	}
}
