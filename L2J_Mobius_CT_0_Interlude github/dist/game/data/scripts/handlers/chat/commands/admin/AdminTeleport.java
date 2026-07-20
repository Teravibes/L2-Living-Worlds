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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles following admin commands: - show_moves - show_teleport - teleport_to_character - move_to - teleport_character
 * @version $Revision: 1.3.2.6.2.4 $ $Date: 2005/04/11 10:06:06 $ con.close() change and small typo fix by Zoey76 24/02/2011
 */
public class AdminTeleport implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminTeleport.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_moves",
		"admin_show_moves_other",
		"admin_show_teleport",
		"admin_teleport_to_character",
		"admin_teleportto",
		"admin_move_to",
		"admin_teleport_character",
		"admin_recall",
		"admin_walk",
		"teleportto",
		"recall",
		"admin_recall_npc",
		"admin_gonorth",
		"admin_gosouth",
		"admin_goeast",
		"admin_gowest",
		"admin_goup",
		"admin_godown",
		"admin_tele",
		"admin_teleto",
		"admin_instant_move",
		"admin_sendhome"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_teleto"))
		{
			activeChar.setTeleMode(1);
		}
		
		if (command.equals("admin_instant_move"))
		{
			activeChar.sendSysMessage("Instant move ready. Click where you want to go.");
			activeChar.setTeleMode(1);
		}
		
		if (command.equals("admin_teleto r"))
		{
			activeChar.sendSysMessage("Instant move ready. Click where you want to go.");
			activeChar.setTeleMode(2);
		}
		
		if (command.equals("admin_teleto end"))
		{
			activeChar.setTeleMode(0);
		}
		
		if (command.equals("admin_show_moves"))
		{
			AdminHtml.showAdminHtml(activeChar, "teleports.htm");
		}
		
		if (command.equals("admin_show_moves_other"))
		{
			AdminHtml.showAdminHtml(activeChar, "teleports/OtherLocations.htm");
		}
		else if (command.equals("admin_show_teleport"))
		{
			showTeleportCharWindow(activeChar);
		}
		else if (command.equals("admin_recall_npc"))
		{
			recallNPC(activeChar);
		}
		else if (command.equals("admin_teleport_to_character"))
		{
			teleportToCharacter(activeChar, activeChar.getTarget());
		}
		else if (command.startsWith("admin_walk"))
		{
			try
			{
				final String val = command.substring(11);
				final StringTokenizer st = new StringTokenizer(val);
				final int x = Integer.parseInt(st.nextToken());
				final int y = Integer.parseInt(st.nextToken());
				final int z = Integer.parseInt(st.nextToken());
				activeChar.getAI().setIntention(Intention.MOVE_TO, new Location(x, y, z, 0));
			}
			catch (Exception e)
			{
				// Not important.
			}
		}
		else if (command.startsWith("admin_move_to"))
		{
			try
			{
				final String val = command.substring(14);
				teleportTo(activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of empty or missing coordinates
				AdminHtml.showAdminHtml(activeChar, "teleports.htm");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendSysMessage("Usage: //move_to <x> <y> <z>");
				AdminHtml.showAdminHtml(activeChar, "teleports.htm");
			}
		}
		else if (command.startsWith("admin_teleport_character"))
		{
			try
			{
				final String val = command.substring(25);
				teleportCharacter(activeChar, val);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of empty coordinates
				activeChar.sendSysMessage("Wrong or no Coordinates given.");
				showTeleportCharWindow(activeChar); // back to character teleport
			}
		}
		else if (command.startsWith("admin_teleportto "))
		{
			try
			{
				final String targetName = command.substring(17);
				final Player player = World.getInstance().getPlayer(targetName);
				teleportToCharacter(activeChar, player);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Not important.
			}
		}
		else if (command.startsWith("admin_recall "))
		{
			try
			{
				final String[] param = command.split(" ");
				if (param.length != 2)
				{
					activeChar.sendSysMessage("Usage: //recall <playername>");
					return false;
				}
				
				final String targetName = param[1];
				final Player player = World.getInstance().getPlayer(targetName);
				if (player != null)
				{
					teleportCharacter(player, activeChar.getLocation(), activeChar);
				}
				else
				{
					changeCharacterPosition(activeChar, targetName);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Not important.
			}
		}
		else if (command.equals("admin_tele"))
		{
			showTeleportWindow(activeChar);
		}
		else if (command.startsWith("admin_go"))
		{
			int intVal = 150;
			int x = activeChar.getX();
			int y = activeChar.getY();
			int z = activeChar.getZ();
			try
			{
				final String val = command.substring(8);
				final StringTokenizer st = new StringTokenizer(val);
				final String dir = st.nextToken();
				if (st.hasMoreTokens())
				{
					intVal = Integer.parseInt(st.nextToken());
				}
				
				if (dir.equals("east"))
				{
					x += intVal;
				}
				else if (dir.equals("west"))
				{
					x -= intVal;
				}
				else if (dir.equals("north"))
				{
					y -= intVal;
				}
				else if (dir.equals("south"))
				{
					y += intVal;
				}
				else if (dir.equals("up"))
				{
					z += intVal;
				}
				else if (dir.equals("down"))
				{
					z -= intVal;
				}
				
				activeChar.teleToLocation(new Location(x, y, z));
				showTeleportWindow(activeChar);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //go<north|south|east|west|up|down> [offset] (default 150)");
			}
		}
		else if (command.startsWith("admin_sendhome"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // Skip command.
			if (st.countTokens() > 1)
			{
				activeChar.sendSysMessage("Usage: //sendhome <playername>");
			}
			else if (st.countTokens() == 1)
			{
				final String name = st.nextToken();
				final Player player = World.getInstance().getPlayer(name);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
					return false;
				}
				
				teleportHome(player);
			}
			else
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isPlayer())
				{
					teleportHome(target.asPlayer());
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
				}
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
	 * This method sends a player to it's home town.
	 * @param player the player to teleport.
	 */
	private void teleportHome(Player player)
	{
		String regionName;
		switch (player.getRace())
		{
			case ELF:
			{
				regionName = "elf_town";
				break;
			}
			case DARK_ELF:
			{
				regionName = "darkelf_town";
				break;
			}
			case ORC:
			{
				regionName = "orc_town";
				break;
			}
			case DWARF:
			{
				regionName = "dwarf_town";
				break;
			}
			case HUMAN:
			default:
			{
				regionName = "talking_island_town";
			}
		}
		
		player.teleToLocation(MapRegionData.getInstance().getSpawnLocByRegionName(regionName), true);
		player.setInstanceId(0);
		player.setIn7sDungeon(false);
	}
	
	private void teleportTo(Player activeChar, String coords)
	{
		try
		{
			final StringTokenizer st = new StringTokenizer(coords);
			final String x1 = st.nextToken();
			final int x = Integer.parseInt(x1);
			final String y1 = st.nextToken();
			final int y = Integer.parseInt(y1);
			final String z1 = st.nextToken();
			final int z = Integer.parseInt(z1);
			activeChar.getAI().setIntention(Intention.IDLE);
			activeChar.teleToLocation(x, y, z);
			activeChar.sendSysMessage("You have been teleported to " + coords);
		}
		catch (NoSuchElementException nsee)
		{
			activeChar.sendSysMessage("Wrong or no Coordinates given.");
		}
	}
	
	private void showTeleportWindow(Player activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "move.htm");
	}
	
	private void showTeleportCharWindow(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		final String replyMSG = "<html><title>Teleport Character</title><body>The character you will teleport is " + player.getName() + ".<br>Co-ordinate x<edit var=\"char_cord_x\" width=110>Co-ordinate y<edit var=\"char_cord_y\" width=110>Co-ordinate z<edit var=\"char_cord_z\" width=110><button value=\"Teleport\" action=\"bypass -h admin_teleport_character $char_cord_x $char_cord_y $char_cord_z\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"><button value=\"Teleport near you\" action=\"bypass -h admin_teleport_character " + activeChar.getX() + " " + activeChar.getY() + " " + activeChar.getZ() + "\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"><center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></center></body></html>";
		adminReply.setHtml(replyMSG);
		activeChar.sendPacket(adminReply);
	}
	
	private void teleportCharacter(Player activeChar, String coords)
	{
		final WorldObject target = activeChar.getTarget();
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
		}
		else
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(coords);
				final String x1 = st.nextToken();
				final int x = Integer.parseInt(x1);
				final String y1 = st.nextToken();
				final int y = Integer.parseInt(y1);
				final String z1 = st.nextToken();
				final int z = Integer.parseInt(z1);
				teleportCharacter(player, new Location(x, y, z), null);
			}
			catch (NoSuchElementException nsee)
			{
				// Not important.
			}
		}
	}
	
	/**
	 * @param player
	 * @param loc
	 * @param activeChar
	 */
	private void teleportCharacter(Player player, Location loc, Player activeChar)
	{
		if (player != null)
		{
			// Check for jail
			if (player.isJailed())
			{
				activeChar.sendSysMessage("Sorry, player " + player.getName() + " is in Jail.");
			}
			else
			{
				// Set player to same instance as GM teleporting.
				if ((activeChar != null) && (activeChar.getInstanceId() >= 0))
				{
					player.setInstanceId(activeChar.getInstanceId());
					activeChar.sendSysMessage("You have recalled " + player.getName());
				}
				else
				{
					player.setInstanceId(0);
				}
				
				player.sendMessage("Admin is teleporting you.");
				player.getAI().setIntention(Intention.IDLE);
				player.teleToLocation(loc, true);
			}
		}
	}
	
	private void teleportToCharacter(Player activeChar, WorldObject target)
	{
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		Player player = null;
		if (target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
		}
		else
		{
			// move to targets instance
			activeChar.setInstanceId(target.getInstanceId());
			
			final int x = player.getX();
			final int y = player.getY();
			final int z = player.getZ();
			activeChar.getAI().setIntention(Intention.IDLE);
			activeChar.teleToLocation(new Location(x, y, z), true);
			activeChar.sendSysMessage("You have teleported to character " + player.getName() + ".");
		}
	}
	
	private void changeCharacterPosition(Player activeChar, String name)
	{
		final int x = activeChar.getX();
		final int y = activeChar.getY();
		final int z = activeChar.getZ();
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE char_name=?");
			statement.setInt(1, x);
			statement.setInt(2, y);
			statement.setInt(3, z);
			statement.setString(4, name);
			statement.execute();
			final int count = statement.getUpdateCount();
			statement.close();
			if (count == 0)
			{
				activeChar.sendSysMessage("Character not found or position unaltered.");
			}
			else
			{
				activeChar.sendSysMessage("Player's [" + name + "] position is now set to (" + x + "," + y + "," + z + ").");
			}
		}
		catch (SQLException se)
		{
			activeChar.sendSysMessage("SQLException while changing offline character's position");
		}
	}
	
	private void recallNPC(Player activeChar)
	{
		final WorldObject obj = activeChar.getTarget();
		if ((obj instanceof Npc) && !obj.asNpc().isMinion() && !(obj instanceof RaidBoss) && !(obj instanceof GrandBoss))
		{
			final Npc target = obj.asNpc();
			Spawn spawn = target.getSpawn();
			if (spawn == null)
			{
				activeChar.sendSysMessage("Incorrect monster spawn.");
				LOGGER.warning("ERROR: NPC " + target.getObjectId() + " has a 'null' spawn.");
				return;
			}
			
			final int respawnTime = spawn.getRespawnDelay() / 1000;
			target.deleteMe();
			spawn.stopRespawn();
			SpawnData.getInstance().deleteSpawn(spawn);
			
			try
			{
				spawn = new Spawn(target.getTemplate().getId());
				spawn.setXYZ(activeChar);
				spawn.setAmount(1);
				spawn.setHeading(activeChar.getHeading());
				spawn.setRespawnDelay(respawnTime);
				if (activeChar.getInstanceId() >= 0)
				{
					spawn.setInstanceId(activeChar.getInstanceId());
				}
				else
				{
					spawn.setInstanceId(0);
				}
				
				SpawnData.getInstance().addNewSpawn(spawn);
				spawn.init();
				if (respawnTime <= 0)
				{
					spawn.stopRespawn();
				}
				
				activeChar.sendSysMessage("Created " + target.getTemplate().getName() + " on " + target.getObjectId() + ".");
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Target is not in game.");
			}
		}
		else if (obj instanceof RaidBoss)
		{
			final RaidBoss target = (RaidBoss) obj;
			final Spawn spawn = target.getSpawn();
			final double curHP = target.getCurrentHp();
			final double curMP = target.getCurrentMp();
			if (spawn == null)
			{
				activeChar.sendSysMessage("Incorrect raid spawn.");
				LOGGER.warning("ERROR: NPC Id" + target.getId() + " has a 'null' spawn.");
				return;
			}
			
			RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
			try
			{
				final Spawn spawnDat = new Spawn(target.getId());
				spawnDat.setXYZ(activeChar);
				spawnDat.setAmount(1);
				spawnDat.setHeading(activeChar.getHeading());
				spawnDat.setRespawnMinDelay(43200);
				spawnDat.setRespawnMaxDelay(129600);
				
				RaidBossSpawnManager.getInstance().addNewSpawn(spawnDat, 0, curHP, curMP, true);
			}
			catch (Exception e)
			{
				activeChar.sendPacket(SystemMessageId.YOUR_TARGET_CANNOT_BE_FOUND);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
	}
}
