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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.model.instancezone.Instance;

/**
 * Loads doors.
 * @author JIV, GodKratos, UnAfraid
 */
public class DoorData implements IXmlReader
{
	private static final Map<String, Set<Integer>> _groups = new HashMap<>();
	private final Map<Integer, Door> _doors = new HashMap<>();
	private final Map<Integer, StatSet> _templates = new HashMap<>();
	
	protected DoorData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_doors.clear();
		_groups.clear();
		parseDatapackFile("data/Doors.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node a = document.getFirstChild(); a != null; a = a.getNextSibling())
		{
			if ("list".equalsIgnoreCase(a.getNodeName()))
			{
				for (Node b = a.getFirstChild(); b != null; b = b.getNextSibling())
				{
					if ("door".equalsIgnoreCase(b.getNodeName()))
					{
						final NamedNodeMap attrs = b.getAttributes();
						final StatSet set = new StatSet();
						set.set("baseHpMax", 1); // Avoid doors without HP value created dead due to default value 0 in CreatureTemplate
						for (int i = 0; i < attrs.getLength(); i++)
						{
							final Node att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						makeDoor(set);
						_templates.put(set.getInt("id"), set);
					}
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _doors.size() + " doors.");
	}
	
	public void insertCollisionData(StatSet set)
	{
		int posX;
		int posY;
		int nodeX;
		int nodeY;
		int height;
		height = set.getInt("height");
		String[] pos = set.getString("node1").split(",");
		nodeX = Integer.parseInt(pos[0]);
		nodeY = Integer.parseInt(pos[1]);
		pos = set.getString("node2").split(",");
		posX = Integer.parseInt(pos[0]);
		posY = Integer.parseInt(pos[1]);
		int collisionRadius = Math.min(Math.abs(nodeX - posX), Math.abs(nodeY - posY)); // (max) radius for movement checks
		if (collisionRadius < 20)
		{
			collisionRadius = 20;
		}
		
		set.set("collisionRadius", collisionRadius);
		set.set("collisionHeight", height);
	}
	
	/**
	 * @param set
	 */
	private void makeDoor(StatSet set)
	{
		insertCollisionData(set);
		final DoorTemplate template = new DoorTemplate(set);
		final Door door = new Door(template);
		door.setCurrentHp(door.getMaxHp());
		door.spawnMe(template.getX(), template.getY(), template.getZ());
		_doors.put(door.getId(), door);
	}
	
	public StatSet getDoorTemplate(int doorId)
	{
		return _templates.get(doorId);
	}
	
	public Door getDoor(int doorId)
	{
		return _doors.get(doorId);
	}
	
	public static void addDoorGroup(String groupName, int doorId)
	{
		Set<Integer> set = _groups.get(groupName);
		if (set == null)
		{
			set = new HashSet<>();
			_groups.put(groupName, set);
		}
		
		set.add(doorId);
	}
	
	public static Set<Integer> getDoorsByGroup(String groupName)
	{
		return _groups.get(groupName);
	}
	
	public Collection<Door> getDoors()
	{
		return _doors.values();
	}
	
	public boolean checkIfDoorsBetween(Location start, Location end, int instanceId)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), instanceId);
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId)
	{
		return checkIfDoorsBetween(x, y, z, tx, ty, tz, instanceId, false);
	}
	
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, int instanceId, boolean doubleFaceCheck)
	{
		final Collection<Door> doors;
		if (instanceId < 1)
		{
			final WorldRegion region = World.getInstance().getRegion(x, y, z);
			if (region != null)
			{
				doors = region.getDoors();
			}
			else
			{
				doors = null;
			}
		}
		else
		{
			final Instance instance = InstanceManager.getInstance().getInstance(instanceId);
			if (instance != null)
			{
				doors = instance.getDoors();
			}
			else
			{
				doors = null;
			}
		}
		
		if ((doors == null) || doors.isEmpty())
		{
			return false;
		}
		
		for (Door doorInst : doors)
		{
			// check dead and open
			if ((instanceId != doorInst.getInstanceId()) || doorInst.isDead() || doorInst.isOpen() || !doorInst.checkCollision() || (doorInst.getX(0) == 0))
			{
				continue;
			}
			
			boolean intersectFace = false;
			for (int i = 0; i < 4; i++)
			{
				final int j = (i + 1) < 4 ? i + 1 : 0;
				
				// lower part of the multiplier fraction, if it is 0 we avoid an error and also know that the lines are parallel
				final int denominator = ((ty - y) * (doorInst.getX(i) - doorInst.getX(j))) - ((tx - x) * (doorInst.getY(i) - doorInst.getY(j)));
				if (denominator == 0)
				{
					continue;
				}
				
				// multipliers to the equations of the lines. If they are lower than 0 or bigger than 1, we know that segments don't intersect
				final float multiplier1 = (float) (((doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i))) - ((doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i)))) / denominator;
				final float multiplier2 = (float) (((tx - x) * (y - doorInst.getY(i))) - ((ty - y) * (x - doorInst.getX(i)))) / denominator;
				if ((multiplier1 >= 0) && (multiplier1 <= 1) && (multiplier2 >= 0) && (multiplier2 <= 1))
				{
					final int intersectZ = Math.round(z + (multiplier1 * (tz - z)));
					
					// now checking if the resulting point is between door's min and max z
					if ((intersectZ > doorInst.getZMin()) && (intersectZ < doorInst.getZMax()))
					{
						if (!doubleFaceCheck || intersectFace)
						{
							return true;
						}
						
						intersectFace = true;
					}
				}
			}
		}
		
		return false;
	}
	
	public static DoorData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DoorData INSTANCE = new DoorData();
	}
}
