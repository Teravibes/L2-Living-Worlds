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
package org.l2jmobius.gameserver.util;

import java.awt.Color;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.geodata.Cell;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator2D;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator3D;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * @author HorridoJoho, Mobius
 */
public class GeoUtils
{
	/**
	 * Draws a 2D line between two points in the world.
	 * @param player The player to send the debug line to.
	 * @param x The x coordinate of the starting point.
	 * @param y The y coordinate of the starting point.
	 * @param tx The x coordinate of the ending point.
	 * @param ty The y coordinate of the ending point.
	 * @param z The z coordinate of the line.
	 */
	public static void debug2DLine(Player player, int x, int y, int tx, int ty, int z)
	{
		final int gx = GeoEngine.getGeoX(x);
		final int gy = GeoEngine.getGeoY(y);
		final int tgx = GeoEngine.getGeoX(tx);
		final int tgy = GeoEngine.getGeoY(ty);
		
		final ExServerPrimitive prim = new ExServerPrimitive("Debug2DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoEngine.getWorldX(gx), GeoEngine.getWorldY(gy), z, GeoEngine.getWorldX(tgx), GeoEngine.getWorldY(tgy), z);
		
		final GridLineIterator2D iter = new GridLineIterator2D(gx, gy, tgx, tgy);
		while (iter.next())
		{
			final int wx = GeoEngine.getWorldX(iter.x());
			final int wy = GeoEngine.getWorldY(iter.y());
			prim.addPoint(Color.RED, wx, wy, z);
		}
		
		player.sendPacket(prim);
	}
	
	/**
	 * Draws a 3D line between two points in the world.
	 * @param player The player to send the debug line to.
	 * @param x The x coordinate of the starting point.
	 * @param y The y coordinate of the starting point.
	 * @param z The z coordinate of the starting point.
	 * @param tx The x coordinate of the ending point.
	 * @param ty The y coordinate of the ending point.
	 * @param tz The z coordinate of the ending point.
	 */
	public static void debug3DLine(Player player, int x, int y, int z, int tx, int ty, int tz)
	{
		final int gx = GeoEngine.getGeoX(x);
		final int gy = GeoEngine.getGeoY(y);
		final int tgx = GeoEngine.getGeoX(tx);
		final int tgy = GeoEngine.getGeoY(ty);
		
		final ExServerPrimitive prim = new ExServerPrimitive("Debug3DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoEngine.getWorldX(gx), GeoEngine.getWorldY(gy), z, GeoEngine.getWorldX(tgx), GeoEngine.getWorldY(tgy), tz);
		
		final GridLineIterator3D iter = new GridLineIterator3D(gx, gy, z, tgx, tgy, tz);
		iter.next();
		int prevX = iter.x();
		int prevY = iter.y();
		int wx = GeoEngine.getWorldX(prevX);
		int wy = GeoEngine.getWorldY(prevY);
		int wz = iter.z();
		prim.addPoint(Color.RED, wx, wy, wz);
		
		while (iter.next())
		{
			final int curX = iter.x();
			final int curY = iter.y();
			
			if ((curX != prevX) || (curY != prevY))
			{
				wx = GeoEngine.getWorldX(curX);
				wy = GeoEngine.getWorldY(curY);
				wz = iter.z();
				
				prim.addPoint(Color.RED, wx, wy, wz);
				
				prevX = curX;
				prevY = curY;
			}
		}
		
		player.sendPacket(prim);
	}
	
	/**
	 * Gets the color to use for a direction arrow based on whether the direction is valid.
	 * @param x The x coordinate of the cell.
	 * @param y The y coordinate of the cell.
	 * @param z The z coordinate of the cell.
	 * @param nswe The direction to check.
	 * @return Green if the direction is valid, red otherwise.
	 */
	private static Color getDirectionColor(int x, int y, int z, int nswe)
	{
		if (GeoEngine.getInstance().checkNearestNswe(x, y, z, nswe))
		{
			return Color.GREEN;
		}
		
		return Color.RED;
	}
	
	/**
	 * Draws a debug grid around the player.
	 * @param player The player to send the debug grid to.
	 */
	public static void debugGrid(Player player)
	{
		final int geoRadius = 10;
		final int blocksPerPacket = 20;
		
		int iBlock = blocksPerPacket;
		int iPacket = 0;
		
		ExServerPrimitive exsp = null;
		final int playerGx = GeoEngine.getGeoX(player.getX());
		final int playerGy = GeoEngine.getGeoY(player.getY());
		for (int dx = -geoRadius; dx <= geoRadius; ++dx)
		{
			for (int dy = -geoRadius; dy <= geoRadius; ++dy)
			{
				if (iBlock >= blocksPerPacket)
				{
					iBlock = 0;
					if (exsp != null)
					{
						++iPacket;
						player.sendPacket(exsp);
					}
					
					exsp = new ExServerPrimitive("DebugGrid_" + iPacket, player.getX(), player.getY(), -16000);
				}
				
				if (exsp == null)
				{
					throw new IllegalStateException();
				}
				
				final int gx = playerGx + dx;
				final int gy = playerGy + dy;
				
				final int x = GeoEngine.getWorldX(gx);
				final int y = GeoEngine.getWorldY(gy);
				final int z = GeoEngine.getInstance().getNearestZ(gx, gy, player.getZ());
				
				// north arrow
				Color col = getDirectionColor(gx, gy, z, Cell.NSWE_NORTH);
				exsp.addLine(col, x - 1, y - 7, z, x + 1, y - 7, z);
				exsp.addLine(col, x - 2, y - 6, z, x + 2, y - 6, z);
				exsp.addLine(col, x - 3, y - 5, z, x + 3, y - 5, z);
				exsp.addLine(col, x - 4, y - 4, z, x + 4, y - 4, z);
				
				// east arrow
				col = getDirectionColor(gx, gy, z, Cell.NSWE_EAST);
				exsp.addLine(col, x + 7, y - 1, z, x + 7, y + 1, z);
				exsp.addLine(col, x + 6, y - 2, z, x + 6, y + 2, z);
				exsp.addLine(col, x + 5, y - 3, z, x + 5, y + 3, z);
				exsp.addLine(col, x + 4, y - 4, z, x + 4, y + 4, z);
				
				// south arrow
				col = getDirectionColor(gx, gy, z, Cell.NSWE_SOUTH);
				exsp.addLine(col, x - 1, y + 7, z, x + 1, y + 7, z);
				exsp.addLine(col, x - 2, y + 6, z, x + 2, y + 6, z);
				exsp.addLine(col, x - 3, y + 5, z, x + 3, y + 5, z);
				exsp.addLine(col, x - 4, y + 4, z, x + 4, y + 4, z);
				
				col = getDirectionColor(gx, gy, z, Cell.NSWE_WEST);
				exsp.addLine(col, x - 7, y - 1, z, x - 7, y + 1, z);
				exsp.addLine(col, x - 6, y - 2, z, x - 6, y + 2, z);
				exsp.addLine(col, x - 5, y - 3, z, x - 5, y + 3, z);
				exsp.addLine(col, x - 4, y - 4, z, x - 4, y + 4, z);
				
				++iBlock;
			}
		}
		
		player.sendPacket(exsp);
	}
	
	/**
	 * Hides the debug grid around the player.
	 * @param player The player to hide the debug grid from.
	 */
	public static void hideDebugGrid(Player player)
	{
		final int geoRadius = 10;
		final int blocksPerPacket = 20;
		
		int iBlock = blocksPerPacket;
		int iPacket = 0;
		
		ExServerPrimitive exsp = null;
		final int playerGx = GeoEngine.getGeoX(player.getX());
		final int playerGy = GeoEngine.getGeoY(player.getY());
		for (int dx = -geoRadius; dx <= geoRadius; ++dx)
		{
			for (int dy = -geoRadius; dy <= geoRadius; ++dy)
			{
				if (iBlock >= blocksPerPacket)
				{
					iBlock = 0;
					if (exsp != null)
					{
						++iPacket;
						player.sendPacket(exsp);
					}
					
					exsp = new ExServerPrimitive("DebugGrid_" + iPacket, player.getX(), player.getY(), -16000);
				}
				
				if (exsp == null)
				{
					throw new IllegalStateException();
				}
				
				final int gx = playerGx + dx;
				final int gy = playerGy + dy;
				
				final int x = GeoEngine.getWorldX(gx);
				final int y = GeoEngine.getWorldY(gy);
				
				// Nothing.
				exsp.addLine(Color.BLACK, x, y, -16000, x, y, -16000);
				++iBlock;
			}
		}
		
		player.sendPacket(exsp);
	}
	
	/**
	 * Computes the NSWE direction based on the difference between two points.
	 * @param lastX The x coordinate of the previous point.
	 * @param lastY The y coordinate of the previous point.
	 * @param x The x coordinate of the current point.
	 * @param y The y coordinate of the current point.
	 * @return The NSWE direction.
	 */
	public static int computeNswe(int lastX, int lastY, int x, int y)
	{
		if (x > lastX) // east
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH_EAST;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH_EAST;
			}
			else
			{
				return Cell.NSWE_EAST;
			}
		}
		else if (x < lastX) // west
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH_WEST;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH_WEST;
			}
			else
			{
				return Cell.NSWE_WEST;
			}
		}
		else // unchanged x
		{
			if (y > lastY)
			{
				return Cell.NSWE_SOUTH;
			}
			else if (y < lastY)
			{
				return Cell.NSWE_NORTH;
			}
			else
			{
				throw new RuntimeException();
			}
		}
	}
}
