/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.zone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.type.PeaceZone;

/**
 * @author NosBit
 */
public class ZoneRegion
{
	private final int _regionX;
	private final int _regionY;
	private final Map<Integer, ZoneType> _zones = new ConcurrentHashMap<>();
	
	public ZoneRegion(int regionX, int regionY)
	{
		_regionX = regionX;
		_regionY = regionY;
	}
	
	public Map<Integer, ZoneType> getZones()
	{
		return _zones;
	}
	
	public int getRegionX()
	{
		return _regionX;
	}
	
	public int getRegionY()
	{
		return _regionY;
	}
	
	public void revalidateZones(Creature creature)
	{
		// do NOT update the world region while the character is still in the process of teleporting
		// Once the teleport is COMPLETED, revalidation occurs safely, at that time.
		if (creature.isTeleporting())
		{
			return;
		}
		
		for (ZoneType z : _zones.values())
		{
			z.revalidateInZone(creature);
		}
	}
	
	public void removeFromZones(Creature creature)
	{
		for (ZoneType z : _zones.values())
		{
			z.removeCharacter(creature);
		}
	}
	
	public boolean checkEffectRangeInsidePeaceZone(Skill skill, int x, int y, int z)
	{
		final int range = skill.getEffectRange();
		final int up = y + range;
		final int down = y - range;
		final int left = x + range;
		final int right = x - range;
		for (ZoneType e : _zones.values())
		{
			if (e instanceof PeaceZone)
			{
				if (e.isInsideZone(x, up, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, down, z))
				{
					return false;
				}
				
				if (e.isInsideZone(left, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(right, y, z))
				{
					return false;
				}
				
				if (e.isInsideZone(x, y, z))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	public void onDeath(Creature creature)
	{
		for (ZoneType z : _zones.values())
		{
			if (z.isInsideZone(creature))
			{
				z.onDieInside(creature);
			}
		}
	}
	
	public void onRevive(Creature creature)
	{
		for (ZoneType z : _zones.values())
		{
			if (z.isInsideZone(creature))
			{
				z.onReviveInside(creature);
			}
		}
	}
}
