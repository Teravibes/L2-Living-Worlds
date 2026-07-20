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
package org.l2jmobius.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.zone.ZoneForm;

/**
 * Just dummy zone, needs only for geometry calculations
 * @author GKR, Mobius
 */
public class NpcSpawnTerritory
{
	private final String _name;
	private final ZoneForm _territory;
	private List<ZoneForm> _bannedTerritories;
	
	public NpcSpawnTerritory(String name, ZoneForm territory)
	{
		_name = name;
		_territory = territory;
	}
	
	public void addBannedTerritory(ZoneForm territory)
	{
		if (_bannedTerritories == null)
		{
			_bannedTerritories = new ArrayList<>(1);
		}
		
		_bannedTerritories.add(territory);
	}
	
	public String getName()
	{
		return _name;
	}
	
	public Location getRandomPoint()
	{
		int count = 0; // Prevent infinite loop.
		Location location;
		
		final Location centerPoint = _territory.getCenterPoint();
		final int centerX = centerPoint.getX();
		final int centerY = centerPoint.getY();
		final int centerZ = centerPoint.getZ();
		int randomX;
		int randomY;
		int randomZ;
		
		if (_bannedTerritories != null)
		{
			SEARCH: while (count++ < 100)
			{
				location = _territory.getRandomPoint();
				randomX = location.getX();
				randomY = location.getY();
				randomZ = location.getZ();
				
				for (ZoneForm territory : _bannedTerritories)
				{
					if (territory.isInsideZone(randomX, randomY, randomZ))
					{
						continue SEARCH;
					}
				}
				
				if (GeoEngine.getInstance().getHeight(randomX, randomY, randomZ) > _territory.getHighZ())
				{
					continue;
				}
				
				if (!GeoEngine.getInstance().canSeeTarget(randomX, randomY, randomZ, centerX, centerY, centerZ, 0))
				{
					continue;
				}
				
				return location;
			}
			
			count = 0;
			SEARCH_NO_GEO: while (count++ < 100)
			{
				location = _territory.getRandomPoint();
				randomX = location.getX();
				randomY = location.getY();
				randomZ = location.getZ();
				
				for (ZoneForm territory : _bannedTerritories)
				{
					if (territory.isInsideZone(randomX, randomY, randomZ))
					{
						continue SEARCH_NO_GEO;
					}
				}
				
				return location;
			}
		}
		
		count = 0;
		while (count++ < 100)
		{
			location = _territory.getRandomPoint();
			randomX = location.getX();
			randomY = location.getY();
			randomZ = location.getZ();
			
			if (GeoEngine.getInstance().getHeight(randomX, randomY, randomZ) > _territory.getHighZ())
			{
				continue;
			}
			
			if (!GeoEngine.getInstance().canSeeTarget(randomX, randomY, randomZ, centerX, centerY, centerZ, 0))
			{
				continue;
			}
			
			return location;
		}
		
		return _territory.getRandomPoint();
	}
	
	public boolean isInsideZone(int x, int y, int z)
	{
		return _territory.isInsideZone(x, y, z);
	}
	
	public int getHighZ()
	{
		return _territory.getHighZ();
	}
	
	public void visualizeZone(int z)
	{
		_territory.visualizeZone(z);
	}
}
