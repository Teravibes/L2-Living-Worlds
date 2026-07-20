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
package org.l2jmobius.gameserver.model.siege;

import java.util.List;

import org.l2jmobius.gameserver.model.Location;

/**
 * @author malyelfik
 */
public class TowerSpawn
{
	private final int _npcId;
	private final Location _location;
	private List<Integer> _zoneList = null;
	private int _upgradeLevel = 0;
	
	public TowerSpawn(int npcId, Location location)
	{
		_location = location;
		_npcId = npcId;
	}
	
	public TowerSpawn(int npcId, Location location, List<Integer> zoneList)
	{
		_location = location;
		_npcId = npcId;
		_zoneList = zoneList;
	}
	
	/**
	 * Gets the NPC ID.
	 * @return the NPC ID
	 */
	public int getId()
	{
		return _npcId;
	}
	
	public Location getLocation()
	{
		return _location;
	}
	
	public List<Integer> getZoneList()
	{
		return _zoneList;
	}
	
	public void setUpgradeLevel(int level)
	{
		_upgradeLevel = level;
	}
	
	public int getUpgradeLevel()
	{
		return _upgradeLevel;
	}
}
