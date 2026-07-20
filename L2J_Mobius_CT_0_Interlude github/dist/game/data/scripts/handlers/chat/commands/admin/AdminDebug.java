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

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.geoengine.pathfinding.GeoLocation;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author Mobius
 */
public class AdminDebug implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_debug"
	};
	
	private static final Map<Player, ScheduledFuture<?>> PLAYER_DOOR_TASKS = new ConcurrentHashMap<>();
	private static final Map<Player, Location> PLAYER_DOOR_LOCATIONS = new ConcurrentHashMap<>();
	private static final int DEBUG_DOOR_DELAY = 3000;
	
	private static final Map<Player, ScheduledFuture<?>> PLAYER_GEO_TASKS = new ConcurrentHashMap<>();
	private static final Map<Player, Location> PLAYER_GEO_LOCATIONS = new ConcurrentHashMap<>();
	private static final int DEBUG_GEO_DELAY = 1500;
	
	private static final Map<Player, ScheduledFuture<?>> PLAYER_MOVE_TASKS = new ConcurrentHashMap<>();
	private static final Map<Player, Location> PLAYER_MOVE_LOCATIONS = new ConcurrentHashMap<>();
	private static final Map<Player, Location> PLAYER_MOVE_TO_LOCATIONS = new ConcurrentHashMap<>();
	private static final Map<Player, List<GeoLocation>> PLAYER_MOVE_PATHS = new ConcurrentHashMap<>();
	private static final int DEBUG_MOVE_DELAY = 100;
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command.toLowerCase(), " ");
		st.nextToken(); // Skip actual command.
		
		if (!st.hasMoreTokens())
		{
			showMenu(activeChar);
			return true;
		}
		
		final String subCommand = st.nextToken();
		switch (subCommand)
		{
			case "packet":
			case "packets":
			{
				// debug packets
				if (!st.hasMoreTokens())
				{
					setPacketDebugging(activeChar, !DevelopmentConfig.DEBUG_CLIENT_PACKETS);
					return true;
				}
				
				// debug packets on // debug packets off
				setPacketDebugging(activeChar, st.nextToken().equals("on"));
				if (command.contains("menu"))
				{
					showMenu(activeChar);
				}
				
				return true;
			}
			case "door":
			case "doors":
			{
				// debug doors
				if (!st.hasMoreTokens())
				{
					setDoorDebugging(activeChar, !PLAYER_DOOR_TASKS.containsKey(activeChar));
					return true;
				}
				
				// debug doors on // debug doors off
				setDoorDebugging(activeChar, st.nextToken().equals("on"));
				if (command.contains("menu"))
				{
					showMenu(activeChar);
				}
				
				return true;
			}
			case "geo":
			case "geodata":
			{
				// debug geodata
				if (!st.hasMoreTokens())
				{
					setGeodataDebugging(activeChar, !PLAYER_GEO_TASKS.containsKey(activeChar));
					return true;
				}
				
				// debug geodata on // debug geodata off
				setGeodataDebugging(activeChar, st.nextToken().equals("on"));
				if (command.contains("menu"))
				{
					showMenu(activeChar);
				}
				
				return true;
			}
			case "move":
			case "movement":
			case "path":
			case "pathfind":
			{
				// debug movement
				if (!st.hasMoreTokens())
				{
					setMovementDebugging(activeChar, !PLAYER_MOVE_TASKS.containsKey(activeChar));
					return true;
				}
				
				// debug movement on // debug movement off
				setMovementDebugging(activeChar, st.nextToken().equals("on"));
				if (command.contains("menu"))
				{
					showMenu(activeChar);
				}
				
				return true;
			}
		}
		
		activeChar.sendSysMessage("Usage: //debug <parameter> <value>");
		return false;
	}
	
	private void showMenu(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		String content = HtmCache.getInstance().getHtm(player, "data/html/admin/debug.htm");
		
		if (DevelopmentConfig.DEBUG_CLIENT_PACKETS)
		{
			content = content.replace("%packets_status%", "Disable");
			content = content.replace("%packets%", "packets off");
		}
		else
		{
			content = content.replace("%packets_status%", "Enable");
			content = content.replace("%packets%", "packets on");
		}
		
		if (PLAYER_DOOR_TASKS.containsKey(player))
		{
			content = content.replace("%doors_status%", "Disable");
			content = content.replace("%doors%", "doors off");
		}
		else
		{
			content = content.replace("%doors_status%", "Enable");
			content = content.replace("%doors%", "doors on");
		}
		
		if (PLAYER_GEO_TASKS.containsKey(player))
		{
			content = content.replace("%geodata_status%", "Disable");
			content = content.replace("%geodata%", "geodata off");
		}
		else
		{
			content = content.replace("%geodata_status%", "Enable");
			content = content.replace("%geodata%", "geodata on");
		}
		
		if (PLAYER_MOVE_TASKS.containsKey(player))
		{
			content = content.replace("%movement_status%", "Disable");
			content = content.replace("%movement%", "movement off");
		}
		else
		{
			content = content.replace("%movement_status%", "Enable");
			content = content.replace("%movement%", "movement on");
		}
		
		html.setHtml(content);
		player.sendPacket(html);
	}
	
	private static synchronized void setPacketDebugging(Player player, boolean enabled)
	{
		DevelopmentConfig.DEBUG_CLIENT_PACKETS = enabled;
		DevelopmentConfig.DEBUG_EX_CLIENT_PACKETS = enabled;
		DevelopmentConfig.DEBUG_SERVER_PACKETS = enabled;
		DevelopmentConfig.DEBUG_UNKNOWN_PACKETS = enabled;
		player.sendSysMessage("Packet debugging on console is " + (enabled ? "enabled." : "disabled."));
	}
	
	private static synchronized void setDoorDebugging(Player player, boolean enabled)
	{
		final ScheduledFuture<?> task = PLAYER_DOOR_TASKS.get(player);
		if (enabled)
		{
			if (task == null)
			{
				AdminCommandHandler.getInstance().onCommand(player, "admin_showdoors", false);
				PLAYER_DOOR_LOCATIONS.put(player, new Location(player));
				PLAYER_DOOR_TASKS.put(player, ThreadPool.scheduleAtFixedRate(() ->
				{
					if (player.isOnline())
					{
						if (LocationUtil.calculateDistance(player, PLAYER_DOOR_LOCATIONS.get(player), false, false) > 15)
						{
							PLAYER_DOOR_LOCATIONS.put(player, new Location(player));
							AdminCommandHandler.getInstance().onCommand(player, "admin_showdoors", false);
						}
					}
					else
					{
						final ScheduledFuture<?> existingTask = PLAYER_DOOR_TASKS.remove(player);
						if (existingTask != null)
						{
							existingTask.cancel(false);
							PLAYER_DOOR_LOCATIONS.remove(player);
						}
					}
				}, DEBUG_DOOR_DELAY, DEBUG_DOOR_DELAY));
			}
		}
		else if (task != null)
		{
			task.cancel(false);
			PLAYER_DOOR_TASKS.remove(player);
			PLAYER_DOOR_LOCATIONS.remove(player);
			ThreadPool.schedule(() -> AdminCommandHandler.getInstance().onCommand(player, "admin_showdoors off", false), DEBUG_DOOR_DELAY + 100);
		}
		
		player.sendSysMessage("Door debugging is " + (enabled ? "enabled." : "disabled."));
	}
	
	private static synchronized void setGeodataDebugging(Player player, boolean enabled)
	{
		final ScheduledFuture<?> task = PLAYER_GEO_TASKS.get(player);
		if (enabled)
		{
			if (task == null)
			{
				AdminCommandHandler.getInstance().onCommand(player, "admin_geogrid", false);
				PLAYER_GEO_LOCATIONS.put(player, new Location(player));
				PLAYER_GEO_TASKS.put(player, ThreadPool.scheduleAtFixedRate(() ->
				{
					if (player.isOnline())
					{
						if (!PLAYER_MOVE_PATHS.containsKey(player) && (LocationUtil.calculateDistance(player, PLAYER_GEO_LOCATIONS.get(player), false, false) > 15))
						{
							PLAYER_GEO_LOCATIONS.put(player, new Location(player));
							AdminCommandHandler.getInstance().onCommand(player, "admin_geogrid", false);
						}
					}
					else
					{
						final ScheduledFuture<?> existingTask = PLAYER_GEO_TASKS.remove(player);
						if (existingTask != null)
						{
							existingTask.cancel(false);
							PLAYER_GEO_LOCATIONS.remove(player);
						}
					}
				}, DEBUG_GEO_DELAY, DEBUG_GEO_DELAY));
			}
		}
		else if (task != null)
		{
			task.cancel(false);
			PLAYER_GEO_TASKS.remove(player);
			PLAYER_GEO_LOCATIONS.remove(player);
			ThreadPool.schedule(() -> AdminCommandHandler.getInstance().onCommand(player, "admin_geogrid off", false), DEBUG_GEO_DELAY + 100);
		}
		
		player.sendSysMessage("Geodata debugging is " + (enabled ? "enabled." : "disabled."));
	}
	
	private static synchronized void setMovementDebugging(Player player, boolean enabled)
	{
		final ScheduledFuture<?> task = PLAYER_MOVE_TASKS.get(player);
		if (enabled)
		{
			if (task == null)
			{
				if (player.isMoving())
				{
					player.sendSysMessage("Cannot start debugging while moving.");
					return;
				}
				
				drawMoveLine(player);
				PLAYER_MOVE_TASKS.put(player, ThreadPool.scheduleAtFixedRate(() ->
				{
					if (player.isOnline())
					{
						if (LocationUtil.calculateDistance(player, PLAYER_MOVE_LOCATIONS.get(player), false, false) > 15)
						{
							drawMoveLine(player);
						}
						else if (PLAYER_MOVE_TO_LOCATIONS.containsKey(player) || PLAYER_MOVE_PATHS.containsKey(player))
						{
							clearMoveLine(player);
						}
					}
					else
					{
						final ScheduledFuture<?> existingTask = PLAYER_MOVE_TASKS.remove(player);
						if (existingTask != null)
						{
							existingTask.cancel(false);
							PLAYER_MOVE_LOCATIONS.remove(player);
						}
					}
				}, DEBUG_MOVE_DELAY, DEBUG_MOVE_DELAY));
			}
		}
		else if (task != null)
		{
			task.cancel(false);
			PLAYER_MOVE_TASKS.remove(player);
			PLAYER_MOVE_LOCATIONS.remove(player);
			ThreadPool.schedule(() -> clearMoveLine(player), DEBUG_GEO_DELAY + 100);
		}
		
		player.sendSysMessage("Movement debugging is " + (enabled ? "enabled." : "disabled."));
	}
	
	private static void drawMoveLine(Player player)
	{
		if (!player.isMoving())
		{
			PLAYER_MOVE_LOCATIONS.put(player, new Location(player));
			return;
		}
		
		final List<GeoLocation> path = player.getGeoPath();
		if (path != null)
		{
			final List<GeoLocation> prevPath = PLAYER_MOVE_PATHS.get(player);
			if ((prevPath == null) || !prevPath.equals(path))
			{
				final ExServerPrimitive exsp = new ExServerPrimitive("DebugMove", player.getX(), player.getY(), -16000);
				exsp.addLine(Color.GREEN, player.getX(), player.getY(), player.getZ(), player.getXdestination(), player.getYdestination(), player.getZdestination());
				for (int i = 0; i < (path.size() - 1); i++)
				{
					final GeoLocation current = path.get(i);
					final GeoLocation next = path.get(i + 1);
					exsp.addLine(Color.BLUE, current.getX(), current.getY(), current.getZ(), next.getX(), next.getY(), next.getZ());
				}
				
				player.sendPacket(exsp);
				
				PLAYER_MOVE_PATHS.put(player, path);
			}
		}
		else if (!PLAYER_MOVE_TO_LOCATIONS.containsKey(player) || (LocationUtil.calculateDistance(new Location(player.getXdestination(), player.getYdestination(), player.getZdestination()), PLAYER_MOVE_TO_LOCATIONS.get(player), false, false) > 15))
		{
			final ExServerPrimitive exsp = new ExServerPrimitive("DebugMove", player.getX(), player.getY(), -16000);
			exsp.addLine(Color.GREEN, player.getX(), player.getY(), player.getZ(), player.getXdestination(), player.getYdestination(), player.getZdestination());
			player.sendPacket(exsp);
			
			PLAYER_MOVE_PATHS.remove(player);
			PLAYER_MOVE_TO_LOCATIONS.put(player, new Location(player.getXdestination(), player.getYdestination(), player.getZdestination()));
		}
	}
	
	private static void clearMoveLine(Player player)
	{
		if (PLAYER_MOVE_PATHS.containsKey(player) && player.isMoving())
		{
			return;
		}
		
		final ExServerPrimitive exsp = new ExServerPrimitive("DebugMove", player.getX(), player.getY(), -16000);
		exsp.addLine(Color.BLACK, player.getX(), player.getY(), -16000, player.getX(), player.getY(), -16000);
		player.sendPacket(exsp);
		
		PLAYER_MOVE_TO_LOCATIONS.remove(player);
		PLAYER_MOVE_PATHS.remove(player);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
