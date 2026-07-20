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
package org.l2jmobius.gameserver.model.actor.holders.npc;

import org.l2jmobius.commons.util.Rnd;

/**
 * This class hold info needed for minions spawns<br>
 * @author Zealar
 */
public class MinionHolder
{
	private final int _id;
	private final int _count;
	private final int _max;
	private final long _respawnTime;
	private final int _weightPoint;
	
	/**
	 * Constructs a minion holder.
	 * @param id the id
	 * @param count the count
	 * @param max the max count
	 * @param respawnTime the respawn time
	 * @param weightPoint the weight point
	 */
	public MinionHolder(int id, int count, int max, long respawnTime, int weightPoint)
	{
		_id = id;
		_count = count;
		_max = max;
		_respawnTime = respawnTime;
		_weightPoint = weightPoint;
	}
	
	/**
	 * @return the Identifier of the Minion to spawn.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the count of the Minions to spawn.
	 */
	public int getCount()
	{
		if (_max > _count)
		{
			return Rnd.get(_count, _max);
		}
		
		return _count;
	}
	
	/**
	 * @return the respawn time of the Minions.
	 */
	public long getRespawnTime()
	{
		return _respawnTime;
	}
	
	/**
	 * @return the weight point of the Minion.
	 */
	public int getWeightPoint()
	{
		return _weightPoint;
	}
}
