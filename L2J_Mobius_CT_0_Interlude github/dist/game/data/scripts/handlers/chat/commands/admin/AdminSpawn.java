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
package handlers.chat.commands.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.DayNightSpawnManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.spawns.AutoSpawnHandler;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.MapUtil;

/**
 * This class handles following admin commands: - show_spawns = shows menu - spawn_index lvl = shows menu for monsters with respective level - spawn_monster id = spawns monster id on target
 * @version $Revision: 1.2.2.5.2.5 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminSpawn implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminSpawn.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_spawns",
		"admin_spawnat",
		"admin_spawn",
		"admin_spawn_monster",
		"admin_spawn_index",
		"admin_unspawnall",
		"admin_respawnall",
		"admin_spawn_reload",
		"admin_spawn_misc",
		"admin_spawnmisc",
		"admin_npc_index",
		"admin_spawn_once",
		"admin_show_npcs",
		"admin_spawnnight",
		"admin_spawnday",
		"admin_instance_spawns",
		"admin_list_spawns",
		"admin_list_positions",
		"admin_spawn_debug_menu",
		"admin_spawn_debug_print",
		"admin_spawn_debug_print_menu",
		"admin_topspawncount",
		"admin_top_spawn_count",
		"admin_circlespawn"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_show_spawns"))
		{
			AdminHtml.showAdminHtml(activeChar, "spawns.htm");
		}
		else if (command.equalsIgnoreCase("admin_spawn_debug_menu"))
		{
			AdminHtml.showAdminHtml(activeChar, "spawns_debug.htm");
		}
		else if (command.startsWith("admin_spawn_debug_print"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			final WorldObject target = activeChar.getTarget();
			if (target instanceof Npc)
			{
				try
				{
					st.nextToken();
					final int type = Integer.parseInt(st.nextToken());
					printSpawn(target.asNpc(), type);
					if (command.contains("_menu"))
					{
						AdminHtml.showAdminHtml(activeChar, "spawns_debug.htm");
					}
				}
				catch (Exception e)
				{
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			}
		}
		else if (command.startsWith("admin_spawn_index"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final int level = Integer.parseInt(st.nextToken());
				int from = 0;
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException nsee)
				{
				}
				
				showMonsters(activeChar, level, from);
			}
			catch (Exception e)
			{
				AdminHtml.showAdminHtml(activeChar, "spawns.htm");
			}
		}
		else if (command.equals("admin_show_npcs"))
		{
			AdminHtml.showAdminHtml(activeChar, "npcs.htm");
		}
		else if (command.startsWith("admin_npc_index"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final String letter = st.nextToken();
				int from = 0;
				try
				{
					from = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException nsee)
				{
				}
				
				showNpcs(activeChar, letter, from);
			}
			catch (Exception e)
			{
				AdminHtml.showAdminHtml(activeChar, "npcs.htm");
			}
		}
		else if (command.startsWith("admin_instance_spawns"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final int instance = Integer.parseInt(st.nextToken());
				if (instance >= 300000)
				{
					final StringBuilder html = new StringBuilder(1500);
					html.append("<html><table width=\"100%\"><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td width=180><center><font color=\"LEVEL\">Spawns for " + instance + "</font></td><td width=45><button value=\"Back\" action=\"bypass -h admin_current_player\" width=45 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table><br><table width=\"100%\"><tr><td width=200>NpcName</td><td width=70>Action</td></tr>");
					int counter = 0;
					int skiped = 0;
					final Instance inst = InstanceManager.getInstance().getInstance(instance);
					if (inst != null)
					{
						for (Npc npc : inst.getNpcs())
						{
							if (!npc.isDead())
							{
								// Only 50 because of client html limitation
								if (counter < 50)
								{
									html.append("<tr><td>" + npc.getName() + "</td><td><a action=\"bypass -h admin_move_to " + npc.getX() + " " + npc.getY() + " " + npc.getZ() + "\">Go</a></td></tr>");
									counter++;
								}
								else
								{
									skiped++;
								}
							}
						}
						
						html.append("<tr><td>Skipped:</td><td>" + skiped + "</td></tr></table></body></html>");
						final NpcHtmlMessage ms = new NpcHtmlMessage();
						ms.setHtml(html.toString());
						activeChar.sendPacket(ms);
					}
					else
					{
						activeChar.sendSysMessage("Cannot find instance " + instance);
					}
				}
				else
				{
					activeChar.sendSysMessage("Invalid instance number.");
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage //instance_spawns <instance_number>");
			}
		}
		else if (command.startsWith("admin_unspawnall"))
		{
			Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_NPC_SERVER_IS_NOT_OPERATING_AT_THIS_TIME));
			
			// Unload all scripts.
			ScriptManager.getInstance().unloadAllScripts();
			
			// Delete all spawns.
			AutoSpawnHandler.getInstance().unload();
			RaidBossSpawnManager.getInstance().cleanUp();
			DayNightSpawnManager.getInstance().cleanUp();
			for (WorldObject obj : World.getInstance().getVisibleObjects())
			{
				if ((obj != null) && obj.isNpc())
				{
					final Npc target = obj.asNpc();
					target.deleteMe();
					final Spawn spawn = target.getSpawn();
					if (spawn != null)
					{
						spawn.stopRespawn();
						SpawnTable.getInstance().removeSpawn(spawn);
					}
				}
			}
			
			// Reload.
			ScriptManager.getInstance().reloadAllScripts();
			AdminData.getInstance().broadcastMessageToGMs("NPC unspawn completed!");
		}
		else if (command.startsWith("admin_spawnday"))
		{
			DayNightSpawnManager.getInstance().spawnDayCreatures();
		}
		else if (command.startsWith("admin_spawnnight"))
		{
			DayNightSpawnManager.getInstance().spawnNightCreatures();
		}
		else if (command.startsWith("admin_respawnall") || command.startsWith("admin_spawn_reload"))
		{
			// Unload all scripts.
			ScriptManager.getInstance().unloadAllScripts();
			
			// Delete all spawns.
			AutoSpawnHandler.getInstance().unload();
			RaidBossSpawnManager.getInstance().cleanUp();
			DayNightSpawnManager.getInstance().cleanUp();
			for (WorldObject obj : World.getInstance().getVisibleObjects())
			{
				if ((obj != null) && obj.isNpc())
				{
					final Npc target = obj.asNpc();
					target.deleteMe();
					final Spawn spawn = target.getSpawn();
					if (spawn != null)
					{
						spawn.stopRespawn();
						SpawnTable.getInstance().removeSpawn(spawn);
					}
				}
			}
			
			// Reload.
			SpawnData.getInstance().load();
			RaidBossSpawnManager.getInstance().load();
			AutoSpawnHandler.getInstance().reload();
			SevenSigns.getInstance().spawnSevenSignsNPC();
			ScriptManager.getInstance().reloadAllScripts();
			AdminData.getInstance().broadcastMessageToGMs("NPC respawn completed!");
		}
		else if (command.equals("admin_spawn_misc") || command.equals("admin_spawnmisc"))
		{
			LOGGER.info(activeChar + " is spawning GM misc room...");
			activeChar.sendSysMessage("WARNING: There are several known issues regarding this feature. Spawning all NPCs during runtime is STRONGLY NOT RECOMMENDED for live servers, just for developing environments.");
			
			final int npcCount = 100000;
			final int startX = -128670;
			final int startY = -230115;
			final int spawnZ = -15536;
			final int endX = -100432;
			final int distance = 200;
			
			int currentX = startX;
			int currentY = startY;
			for (int i = 1; i <= npcCount; i++)
			{
				final NpcTemplate template = NpcData.getInstance().getTemplate(i);
				if ((template == null) || (template.getId() != template.getDisplayId()) || template.isType("GrandBoss") || template.isFakePlayer())
				{
					continue;
				}
				
				try
				{
					final Spawn spawn = new Spawn(template);
					spawn.setXYZ(new Location(currentX, currentY, spawnZ));
					spawn.setAmount(1);
					spawn.setHeading(0); // ?
					spawn.setRespawnDelay(60);
					SpawnTable.getInstance().addSpawn(spawn);
					spawn.init();
					spawn.stopRespawn();
					
					final Npc npc = spawn.getLastSpawn();
					npc.setImmobilized(true);
					npc.disableCoreAI(true);
					npc.broadcastInfo(); // ?
					
					if (npc.isMonster())
					{
						final Monster monster = npc.asMonster();
						if (monster.hasMinions())
						{
							monster.getMinionList().getSpawnedMinions().forEach(Monster::decayMe);
						}
					}
				}
				catch (Exception e)
				{
				}
				
				currentX += distance;
				
				if (currentX > endX)
				{
					currentX = startX;
					currentY -= distance; // Move to the next line.
				}
			}
			
			activeChar.teleToLocation(new Location(currentX, currentY, spawnZ)); // Teleport player at GM Misc.
			LOGGER.info(activeChar + " spawned all NPCs at GM misc room." + System.lineSeparator() + "WARNING: There are several known issues regarding this feature. Spawning all NPCs during runtime is STRONGLY NOT RECOMMENDED for live servers, just for developing environments.");
		}
		else if (command.startsWith("admin_spawnat"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				final String id = st.nextToken();
				final String x = st.nextToken();
				final String y = st.nextToken();
				final String z = st.nextToken();
				int h = activeChar.getHeading();
				if (st.hasMoreTokens())
				{
					h = Integer.parseInt(st.nextToken());
				}
				
				spawnMonster(activeChar, Integer.parseInt(id), Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z), h);
			}
			catch (Exception e)
			{
				// Case of wrong or missing monster data.
				AdminHtml.showAdminHtml(activeChar, "spawns.htm");
			}
		}
		else if (command.startsWith("admin_spawn_monster") || command.startsWith("admin_spawn"))
		{
			try
			{
				// Create a StringTokenizer to split the command by spaces.
				final StringTokenizer st = new StringTokenizer(command, " ");
				
				// Get the first token (the command itself).
				final String cmd = st.nextToken();
				
				// Get the second token (the NPC ID or name).
				String npcId = st.nextToken();
				
				// If the second token is not a digit, search for the NPC template by name.
				if (!StringUtil.isNumeric(npcId))
				{
					// Initialize the variables.
					final StringBuilder searchParam = new StringBuilder();
					final String[] params = command.split(" ");
					NpcTemplate searchTemplate = null;
					NpcTemplate template = null;
					int pos = 1;
					
					// Iterate through the command parameters, starting from the second one.
					for (int i = 1; i < params.length; i++)
					{
						// Add the current parameter to the search parameter string.
						searchParam.append(params[i]);
						searchParam.append(" ");
						
						// Try to get the NPC template using the search parameter string.
						searchTemplate = NpcData.getInstance().getTemplateByName(searchParam.toString().trim());
						
						// If the template is found, update the position and the final template.
						if (searchTemplate != null)
						{
							template = searchTemplate;
							pos = i;
						}
					}
					
					// Check if an NPC template was found.
					if (template != null)
					{
						// Skip tokens that contain the name.
						for (int i = 1; i < pos; i++)
						{
							st.nextToken();
						}
						
						// Set the npcId based on template found.
						npcId = String.valueOf(template.getId());
					}
				}
				
				// Initialize mobCount to 1.
				int mobCount = 1;
				
				// If next token exists, set the mobCount value.
				if (st.hasMoreTokens())
				{
					mobCount = Integer.parseInt(st.nextToken());
				}
				
				// Initialize respawnTime to 60.
				int respawnTime = 60;
				
				// If next token exists, set the respawnTime value.
				if (st.hasMoreTokens())
				{
					respawnTime = Integer.parseInt(st.nextToken());
				}
				
				// Call the spawnMonster method with the appropriate parameters.
				spawnMonster(activeChar, npcId, respawnTime, mobCount, !cmd.equalsIgnoreCase("admin_spawn_once"));
			}
			catch (Exception e)
			{
				// Case of wrong or missing monster data.
				AdminHtml.showAdminHtml(activeChar, "spawns.htm");
			}
		}
		else if (command.startsWith("admin_list_spawns") || command.startsWith("admin_list_positions"))
		{
			int npcId = 0;
			int teleportIndex = -1;
			
			try
			{
				// Split the command into an array of words.
				final String[] params = command.split(" ");
				final StringBuilder searchParam = new StringBuilder();
				int pos = -1;
				
				// Concatenate all words in the command except the first and last word.
				for (String param : params)
				{
					pos++;
					if ((pos > 0) && (pos < (params.length - 1)))
					{
						searchParam.append(param);
						searchParam.append(" ");
					}
				}
				
				final String searchString = searchParam.toString().trim();
				
				// If the search string is a number, use it as the NPC ID.
				if (StringUtil.isNumeric(searchString))
				{
					npcId = Integer.parseInt(searchString);
				}
				else
				{
					// Otherwise, use it as the NPC name and look up the NPC ID.
					npcId = NpcData.getInstance().getTemplateByName(searchString).getId();
				}
				
				// If there are more than two words in the command, try to parse the last word as the teleport index.
				if (params.length > 2)
				{
					final String lastParam = params[params.length - 1];
					if (StringUtil.isNumeric(lastParam))
					{
						teleportIndex = Integer.parseInt(lastParam);
					}
				}
			}
			catch (Exception e)
			{
				// activeChar.sendSysMessage("Command format is //list_spawns <npcId|npc_name> [tele_index]");
			}
			
			// Call the findNpcs method with the parsed NPC ID and teleport index.
			findNpcs(activeChar, npcId, teleportIndex, command.startsWith("admin_list_positions"));
		}
		else if (command.startsWith("admin_topspawncount") || command.startsWith("admin_top_spawn_count"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int count = 5;
			if (st.hasMoreTokens())
			{
				final String nextToken = st.nextToken();
				if (StringUtil.isNumeric(nextToken))
				{
					count = Integer.parseInt(nextToken);
				}
				
				if (count <= 0)
				{
					return true;
				}
			}
			
			final Map<Integer, Integer> npcsFound = new HashMap<>();
			for (WorldObject obj : World.getInstance().getVisibleObjects())
			{
				if (!obj.isNpc())
				{
					continue;
				}
				
				final int npcId = obj.getId();
				if (npcsFound.containsKey(npcId))
				{
					npcsFound.put(npcId, npcsFound.get(npcId) + 1);
				}
				else
				{
					npcsFound.put(npcId, 1);
				}
			}
			
			activeChar.sendSysMessage("Top " + count + " spawn count.");
			for (Entry<Integer, Integer> entry : MapUtil.sortByValue(npcsFound, true).entrySet())
			{
				count--;
				if (count < 0)
				{
					break;
				}
				
				final int npcId = entry.getKey();
				activeChar.sendSysMessage(NpcData.getInstance().getTemplate(npcId).getName() + " (" + npcId + "): " + entry.getValue());
			}
		}
		else if (command.startsWith("admin_circlespawn"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				
				final String npcIdStr = st.nextToken();
				final int count = Integer.parseInt(st.nextToken());
				final int radius = Integer.parseInt(st.nextToken());
				
				int respawn = 60;
				if (st.hasMoreTokens())
				{
					respawn = Integer.parseInt(st.nextToken());
				}
				
				boolean permanent = true;
				if (st.hasMoreTokens())
				{
					permanent = Boolean.parseBoolean(st.nextToken());
				}
				
				NpcTemplate template;
				if (npcIdStr.matches("[0-9]+"))
				{
					template = NpcData.getInstance().getTemplate(Integer.parseInt(npcIdStr));
				}
				else
				{
					template = NpcData.getInstance().getTemplateByName(npcIdStr.replace('_', ' '));
				}
				
				if (template == null)
				{
					activeChar.sendMessage("NPC not found.");
					return true;
				}
				
				final int centerX = activeChar.getX();
				final int centerY = activeChar.getY();
				final int centerZ = activeChar.getZ();
				
				for (int i = 0; i < count; i++)
				{
					double angle = (2 * Math.PI * i) / count;
					int x = centerX + (int) (radius * Math.cos(angle));
					int y = centerY + (int) (radius * Math.sin(angle));
					int z = centerZ;
					
					final Spawn spawn = new Spawn(template);
					spawn.setXYZ(x, y, z);
					spawn.setAmount(1);
					spawn.setHeading(activeChar.getHeading());
					spawn.setRespawnDelay(respawn);
					
					if (activeChar.getInstanceId() > 0)
					{
						spawn.setInstanceId(activeChar.getInstanceId());
						permanent = false;
					}
					
					if (permanent)
					{
						SpawnData.getInstance().addNewSpawn(spawn);
					}
					else
					{
						SpawnTable.getInstance().addSpawn(spawn);
					}
					
					spawn.init();
					if (!permanent || (respawn <= 0))
					{
						spawn.stopRespawn();
					}
				}
				
				activeChar.sendMessage("Spawn circle created: " + count + template.getName());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Us: //circlespawn <npcId|name> <quantity> <radio> [respawn] [permanent]");
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Get all the spawn of a NPC.
	 * @param activeChar
	 * @param npcId
	 * @param teleportIndex
	 * @param showposition
	 */
	private void findNpcs(Player activeChar, int npcId, int teleportIndex, boolean showposition)
	{
		int index = 0;
		for (Spawn spawn : SpawnTable.getInstance().getSpawns(npcId))
		{
			index++;
			final Npc npc = spawn.getLastSpawn();
			if (teleportIndex > -1)
			{
				if (teleportIndex == index)
				{
					if (showposition && (npc != null))
					{
						activeChar.teleToLocation(npc.getLocation(), true);
					}
					else
					{
						activeChar.teleToLocation(spawn.getLocation(), true);
					}
					
					activeChar.sendSysMessage("You have been teleported.");
				}
			}
			else if (showposition && (npc != null))
			{
				activeChar.sendMessage(index + " - " + spawn.getTemplate().getName() + " (" + spawn + "): " + npc.getX() + " " + npc.getY() + " " + npc.getZ());
			}
			else
			{
				activeChar.sendMessage(index + " - " + spawn.getTemplate().getName() + " (" + spawn + "): " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ());
			}
		}
		
		if (index == 0)
		{
			final Npc npc = World.getInstance().getNpc(npcId);
			if (npc != null)
			{
				activeChar.teleToLocation(npc.getLocation(), npc.getInstanceId(), PlayerConfig.MAX_OFFSET_ON_TELEPORT);
				activeChar.sendMessage("The current spawn is not stored.");
			}
			else
			{
				activeChar.sendMessage("The specified teleport target does not exist.");
			}
		}
	}
	
	private void printSpawn(Npc target, int type)
	{
		final int i = target.getId();
		final int x = target.getSpawn().getX();
		final int y = target.getSpawn().getY();
		final int z = target.getSpawn().getZ();
		final int h = target.getSpawn().getHeading();
		switch (type)
		{
			default:
			case 0:
			{
				LOGGER.info("('',1," + i + "," + x + "," + y + "," + z + ",0,0," + h + ",60,0,0),");
				break;
			}
			case 1:
			{
				LOGGER.info("<spawn npcId=\"" + i + "\" x=\"" + x + "\" y=\"" + y + "\" z=\"" + z + "\" heading=\"" + h + "\" respawn=\"0\" />");
				break;
			}
			case 2:
			{
				LOGGER.info("{ " + i + ", " + x + ", " + y + ", " + z + ", " + h + " },");
				break;
			}
		}
	}
	
	private void spawnMonster(Player activeChar, String monsterIdValue, int respawnTime, int mobCount, boolean permanentValue)
	{
		WorldObject target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}
		
		NpcTemplate template;
		String monsterId = monsterIdValue;
		if (monsterId.matches("[0-9]*"))
		{
			// First parameter was an ID number
			template = NpcData.getInstance().getTemplate(Integer.parseInt(monsterId));
		}
		else
		{
			// First parameter wasn't just numbers so go by name not ID
			template = NpcData.getInstance().getTemplateByName(monsterId.replace('_', ' '));
		}
		
		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED && template.isFakePlayer())
		{
			activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
			return;
		}
		
		try
		{
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(target);
			spawn.setAmount(mobCount);
			spawn.setHeading(activeChar.getHeading());
			spawn.setRespawnDelay(respawnTime);
			
			boolean permanent = permanentValue;
			if (activeChar.getInstanceId() > 0)
			{
				spawn.setInstanceId(activeChar.getInstanceId());
				permanent = false;
			}
			else
			{
				spawn.setInstanceId(0);
			}
			
			// TODO add checks for GrandBossSpawnManager
			if (RaidBossSpawnManager.getInstance().isDefined(spawn.getId()))
			{
				activeChar.sendSysMessage("You cannot spawn another instance of " + template.getName() + ".");
			}
			else
			{
				if (template.isType("RaidBoss"))
				{
					spawn.setRespawnMinDelay(43200);
					spawn.setRespawnMaxDelay(129600);
					RaidBossSpawnManager.getInstance().addNewSpawn(spawn, 0, template.getBaseHpMax(), template.getBaseMpMax(), permanent);
				}
				else
				{
					if (permanent)
					{
						SpawnData.getInstance().addNewSpawn(spawn);
					}
					else
					{
						SpawnTable.getInstance().addSpawn(spawn);
					}
					
					spawn.init();
				}
				
				if (!permanent || (respawnTime <= 0))
				{
					spawn.stopRespawn();
				}
				
				activeChar.sendSysMessage("Created " + template.getName() + " on " + target.getObjectId());
			}
		}
		catch (Exception e)
		{
			activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
		}
	}
	
	private void spawnMonster(Player activeChar, int id, int x, int y, int z, int h)
	{
		WorldObject target = activeChar.getTarget();
		if (target == null)
		{
			target = activeChar;
		}
		
		final NpcTemplate template1 = NpcData.getInstance().getTemplate(id);
		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED && template1.isFakePlayer())
		{
			activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
			return;
		}
		
		try
		{
			final Spawn spawn = new Spawn(template1);
			spawn.setXYZ(x, y, z);
			spawn.setAmount(1);
			spawn.setHeading(h);
			spawn.setRespawnDelay(60);
			if (activeChar.getInstanceId() > 0)
			{
				spawn.setInstanceId(activeChar.getInstanceId());
			}
			
			SpawnData.getInstance().addNewSpawn(spawn);
			spawn.init();
			
			if (activeChar.getInstanceId() > 0)
			{
				spawn.stopRespawn();
			}
			
			spawn.getLastSpawn().broadcastInfo();
			activeChar.sendSysMessage("Created " + template1.getName() + " on " + target.getObjectId());
		}
		catch (Exception e)
		{
			activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
		}
	}
	
	private void showMonsters(Player activeChar, int level, int from)
	{
		final List<NpcTemplate> mobs = NpcData.getInstance().getAllMonstersOfLevel(level);
		final int mobsCount = mobs.size();
		final StringBuilder tb = new StringBuilder(500 + (mobsCount * 80));
		tb.append("<html><title>Spawn Monster:</title><body><p> Level : " + level + "<br>Total NPCs : " + mobsCount + "<br>");
		
		// Loop
		int i = from;
		for (int j = 0; (i < mobsCount) && (j < 50); i++, j++)
		{
			tb.append("<a action=\"bypass -h admin_spawn_monster " + mobs.get(i).getId() + "\">" + mobs.get(i).getName() + "</a><br1>");
		}
		
		if (i == mobsCount)
		{
			tb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		}
		else
		{
			tb.append("<br><center><button value=\"Next\" action=\"bypass -h admin_spawn_index " + level + " " + i + "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_spawns\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		}
		
		activeChar.sendPacket(new NpcHtmlMessage(tb.toString()));
	}
	
	private void showNpcs(Player activeChar, String starting, int from)
	{
		final List<NpcTemplate> mobs = NpcData.getInstance().getAllNpcStartingWith(starting);
		final int mobsCount = mobs.size();
		final StringBuilder tb = new StringBuilder(500 + (mobsCount * 80));
		tb.append("<html><title>Spawn Monster:</title><body><p> There are " + mobsCount + " Npcs whose name starts with " + starting + ":<br>");
		
		// Loop
		int i = from;
		for (int j = 0; (i < mobsCount) && (j < 50); i++, j++)
		{
			tb.append("<a action=\"bypass -h admin_spawn_monster " + mobs.get(i).getId() + "\">" + mobs.get(i).getName() + "</a><br1>");
		}
		
		if (i == mobsCount)
		{
			tb.append("<br><center><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		}
		else
		{
			tb.append("<br><center><button value=\"Next\" action=\"bypass -h admin_npc_index " + starting + " " + i + "\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><button value=\"Back\" action=\"bypass -h admin_show_npcs\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>");
		}
		
		activeChar.sendPacket(new NpcHtmlMessage(tb.toString()));
	}
}
