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
package org.l2jmobius.gameserver.geoengine.pathfinding;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.geoengine.geodata.Cell;

/**
 * @author Mobius
 */
public class GeoLocation
{
	private int _x;
	private int _y;
	private int _nswe;
	private int _geoHeight;
	
	public GeoLocation(int x, int y, int z)
	{
		set(x, y, z);
	}
	
	public void set(int x, int y, int z)
	{
		_x = x;
		_y = y;
		_nswe = 0; // Reset the bitmask.
		
		// Set the NSWE bitmask based on movement possibilities.
		final GeoEngine geoEngine = GeoEngine.getInstance();
		if (geoEngine.checkNearestNswe(x, y, z, Cell.NSWE_NORTH))
		{
			_nswe |= Cell.NSWE_NORTH;
		}
		
		if (geoEngine.checkNearestNswe(x, y, z, Cell.NSWE_EAST))
		{
			_nswe |= Cell.NSWE_EAST;
		}
		
		if (geoEngine.checkNearestNswe(x, y, z, Cell.NSWE_SOUTH))
		{
			_nswe |= Cell.NSWE_SOUTH;
		}
		
		if (geoEngine.checkNearestNswe(x, y, z, Cell.NSWE_WEST))
		{
			_nswe |= Cell.NSWE_WEST;
		}
		
		_geoHeight = geoEngine.getNearestZ(x, y, z);
	}
	
	public boolean canGoNorth()
	{
		return (_nswe & Cell.NSWE_NORTH) != 0;
	}
	
	public boolean canGoEast()
	{
		return (_nswe & Cell.NSWE_EAST) != 0;
	}
	
	public boolean canGoSouth()
	{
		return (_nswe & Cell.NSWE_SOUTH) != 0;
	}
	
	public boolean canGoWest()
	{
		return (_nswe & Cell.NSWE_WEST) != 0;
	}
	
	public boolean canGoNone()
	{
		return _nswe == 0;
	}
	
	public boolean canGoAll()
	{
		return _nswe == Cell.NSWE_ALL;
	}
	
	public int getX()
	{
		return GeoEngine.getWorldX(_x);
	}
	
	public int getY()
	{
		return GeoEngine.getWorldY(_y);
	}
	
	public int getZ()
	{
		return _geoHeight;
	}
	
	public void setZ(short z)
	{
	}
	
	public int getNodeX()
	{
		return _x;
	}
	
	public int getNodeY()
	{
		return _y;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + _x;
		result = (prime * result) + _y;
		
		// Combine the geo height and the NSWE bitmask into the hash.
		result = (prime * result) + (((_geoHeight & 0xFFFF) << 1) | _nswe);
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if ((obj == null) || (getClass() != obj.getClass()))
		{
			return false;
		}
		
		// Compare _x, _y, _geoHeight, and the bitmask _nswe directly.
		final GeoLocation other = (GeoLocation) obj;
		return (_x == other._x) && (_y == other._y) && (_geoHeight == other._geoHeight) && (_nswe == other._nswe);
	}
}
