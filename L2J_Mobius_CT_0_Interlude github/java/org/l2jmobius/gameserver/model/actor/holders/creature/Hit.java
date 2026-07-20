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
package org.l2jmobius.gameserver.model.actor.holders.creature;

import org.l2jmobius.gameserver.model.WorldObject;

/**
 * @author UnAfraid
 */
public class Hit
{
	private static final int HITFLAG_USESS = 0x10;
	private static final int HITFLAG_CRIT = 0x20;
	private static final int HITFLAG_SHLD = 0x40;
	private static final int HITFLAG_MISS = 0x80;
	
	private final int _targetId;
	private final int _damage;
	private int _flags = 0;
	
	public Hit(WorldObject target, int damage, boolean miss, boolean crit, byte shld, boolean soulshot, int ssGrade)
	{
		_targetId = target.getObjectId();
		_damage = damage;
		
		if (soulshot)
		{
			_flags |= HITFLAG_USESS | ssGrade;
		}
		
		if (crit)
		{
			_flags |= HITFLAG_CRIT;
		}
		
		if (shld > 0)
		{
			_flags |= HITFLAG_SHLD;
		}
		
		if (miss)
		{
			_flags |= HITFLAG_MISS;
		}
	}
	
	public int getTargetId()
	{
		return _targetId;
	}
	
	public int getDamage()
	{
		return _damage;
	}
	
	public int getFlags()
	{
		return _flags;
	}
}
