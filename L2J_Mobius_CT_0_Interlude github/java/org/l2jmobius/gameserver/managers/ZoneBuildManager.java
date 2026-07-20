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
package org.l2jmobius.gameserver.managers;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * @author Mobius
 */
public class ZoneBuildManager
{
	private static final String HTML_DELETE_BUTTON = "<button value=\"Delete\" action=\"bypass -h admin_zone_build_delete %d\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
	private static final Map<Player, List<Location>> PLAYER_LOCATIONS = new ConcurrentHashMap<>();
	
	public void addPoint(Player player, Location location)
	{
		List<Location> locations = PLAYER_LOCATIONS.get(player);
		if (locations == null)
		{
			locations = new LinkedList<>();
			PLAYER_LOCATIONS.put(player, locations);
		}
		
		locations.add(location);
		player.sendMessage("Point saved " + location);
		displayZones(player);
		
		AdminCommandHandler.getInstance().onCommand(player, "admin_zone_build", false);
	}
	
	public void displayZones(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations != null) && !locations.isEmpty())
		{
			int packetCount = 1;
			ExServerPrimitive packet = new ExServerPrimitive("ZoneBuilder" + packetCount, locations.get(0).getX(), locations.get(0).getY(), 65535 + locations.get(0).getZ());
			packet.addPoint("" + 0, Color.RED, true, locations.get(0).getX(), locations.get(0).getY(), locations.get(0).getZ());
			for (int i = 1; i < locations.size(); i++)
			{
				if ((i % 10) == 0)
				{
					packetCount++;
					player.sendPacket(packet);
					packet = new ExServerPrimitive("ZoneBuilder" + packetCount, locations.get(i - 1).getX(), locations.get(i - 1).getY(), 65535 + locations.get(i - 1).getZ());
				}
				
				packet.addPoint("" + i, Color.RED, true, locations.get(i).getX(), locations.get(i).getY(), locations.get(i).getZ());
				packet.addLine(Color.GREEN, locations.get(i - 1).getX(), locations.get(i - 1).getY(), locations.get(i - 1).getZ(), locations.get(i).getX(), locations.get(i).getY(), locations.get(i).getZ());
			}
			
			player.sendPacket(packet);
		}
	}
	
	public List<Location> getLocations(Player player)
	{
		return PLAYER_LOCATIONS.get(player);
	}
	
	public String getPathsForHtml(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations != null) && !locations.isEmpty())
		{
			final StringBuilder sb = new StringBuilder(locations.size() * 50);
			sb.append("<table width=300>");
			for (int i = 0; i < locations.size(); i++)
			{
				final Location location = locations.get(i);
				sb.append("<tr>");
				sb.append("<td width=20>" + i + "</td>");
				sb.append("<td width=120>" + "X: " + location.getX() + " Y: " + location.getY() + " Z: " + location.getZ() + "</td>");
				sb.append("<td width=80>" + String.format(HTML_DELETE_BUTTON, i) + "</td>");
				sb.append("</tr>");
			}
			sb.append("</table>");
			return sb.toString();
		}
		
		return "";
	}
	
	public void clearZone(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.remove(player);
		if ((locations != null) && !locations.isEmpty())
		{
			int packetCount = 1;
			ExServerPrimitive packet = new ExServerPrimitive("ZoneBuilder" + packetCount, player.getX(), player.getY(), -16000);
			packet.addPoint(Color.GREEN, 0, 0, Short.MIN_VALUE);
			for (int i = 1; i < locations.size(); i++)
			{
				if ((i % 10) == 0)
				{
					packetCount++;
					player.sendPacket(packet);
					packet = new ExServerPrimitive("ZoneBuilder" + packetCount, locations.get(i - 1).getX(), locations.get(i - 1).getY(), -16000);
					packet.addPoint(Color.GREEN, 0, 0, Short.MIN_VALUE);
				}
			}
			
			player.sendPacket(packet);
		}
	}
	
	public void buildZone(Player player)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations == null) || locations.isEmpty())
		{
			player.sendMessage("No path entries to save.");
			return;
		}
		
		try
		{
			final long currentTime = System.currentTimeMillis();
			final String fileName = "data/zones/" + player.getName() + "-" + currentTime + ".xml";
			final File spawnFile = new File(fileName);
			final BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile));
			
			final StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			sb.append("<list enabled=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../xsd/zones.xsd\">\n");
			sb.append("\t<zone name=\"").append(player.getName()).append("_Zone_").append(currentTime).append("\" type=\"ScriptZone\" shape=\"NPoly\" minZ=\"").append(locations.get(0).getZ() - 1000).append("\" maxZ=\"").append(locations.get(0).getZ() + 1000).append("\">\n");
			
			for (Location location : locations)
			{
				sb.append("\t\t<node X=\"").append(location.getX()).append("\" Y=\"").append(location.getY()).append("\" />\n");
			}
			
			sb.append("\t</zone>\n");
			sb.append("</list>");
			
			writer.write(sb.toString());
			writer.close();
			
			player.sendMessage("Zone saved at " + fileName);
			AdminCommandHandler.getInstance().onCommand(player, "admin_zone_build_clear", false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void deleteEntry(Player player, int entry)
	{
		final List<Location> locations = PLAYER_LOCATIONS.get(player);
		if ((locations == null) || (entry < 0) || (entry >= locations.size()))
		{
			return;
		}
		
		final List<Location> modifiedLocations = new LinkedList<>();
		for (int i = 0; i < locations.size(); i++)
		{
			if (i != entry)
			{
				modifiedLocations.add(locations.get(i));
			}
		}
		
		if (modifiedLocations.size() <= 1)
		{
			clearZone(player);
		}
		else
		{
			PLAYER_LOCATIONS.put(player, modifiedLocations);
			displayZones(player);
		}
	}
	
	public static ZoneBuildManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneBuildManager INSTANCE = new ZoneBuildManager();
	}
}
