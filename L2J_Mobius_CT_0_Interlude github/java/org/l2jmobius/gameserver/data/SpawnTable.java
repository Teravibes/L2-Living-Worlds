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
package org.l2jmobius.gameserver.data;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.spawns.Spawn;

/**
 * @author Mobius
 */
public class SpawnTable
{
	private final Map<Integer, Set<Spawn>> _npcSpawns = new ConcurrentHashMap<>();
	
	protected SpawnTable()
	{
	}
	
	/**
	 * Retrieves the complete spawn table containing all NPC spawns.
	 * @return a map where keys are NPC IDs and values are sets of spawns for those NPCs.
	 */
	public Map<Integer, Set<Spawn>> getSpawnTable()
	{
		return _npcSpawns;
	}
	
	/**
	 * Retrieves the set of spawns for the specified NPC ID.
	 * @param npcId the ID of the NPC to get spawns for.
	 * @return a set of spawns for the given NPC ID, or an empty set if no spawns are present.
	 */
	public Set<Spawn> getSpawns(int npcId)
	{
		return _npcSpawns.getOrDefault(npcId, Collections.emptySet());
	}
	
	/**
	 * Gets the count of spawns for the specified NPC ID.
	 * @param npcId the ID of the NPC to get the spawn count for.
	 * @return the number of spawns associated with the given NPC ID.
	 */
	public int getSpawnCount(int npcId)
	{
		return getSpawns(npcId).size();
	}
	
	/**
	 * Retrieves any spawn for the specified NPC ID.
	 * @param npcId the ID of the NPC to get a spawn for.
	 * @return a spawn associated with the given NPC ID, or {@code null} if none are present.
	 */
	public Spawn getAnySpawn(int npcId)
	{
		return getSpawns(npcId).stream().findFirst().orElse(null);
	}
	
	/**
	 * Adds a spawn to the spawn table.<br>
	 * If the spawn set for the specified NPC ID does not exist, a new set is created and the spawn is added to it.
	 * @param spawn the NPC spawn to add.
	 */
	public void addSpawn(Spawn spawn)
	{
		_npcSpawns.computeIfAbsent(spawn.getId(), _ -> ConcurrentHashMap.newKeySet(1)).add(spawn);
	}
	
	/**
	 * Removes a spawn from the spawn table.<br>
	 * If the spawn set becomes empty after removal, the set itself is also removed from the table.
	 * @param spawn the NPC spawn to remove.
	 */
	public void removeSpawn(Spawn spawn)
	{
		final Set<Spawn> set = _npcSpawns.get(spawn.getId());
		if (set != null)
		{
			set.remove(spawn);
			if (set.isEmpty())
			{
				_npcSpawns.remove(spawn.getId());
			}
		}
	}
	
	public static SpawnTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpawnTable INSTANCE = new SpawnTable();
	}
}
