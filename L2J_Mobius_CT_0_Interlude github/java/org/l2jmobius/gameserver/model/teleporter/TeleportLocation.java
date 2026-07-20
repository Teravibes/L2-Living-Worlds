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
package org.l2jmobius.gameserver.model.teleporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;

/**
 * @author UnAfraid
 */
public class TeleportLocation extends Location
{
	private final int _id;
	private final String _name;
	private final int _feeId;
	private final int _feeCount;
	private final List<Integer> _castleId;
	
	public TeleportLocation(int id, StatSet set)
	{
		super(set);
		_id = id;
		_name = set.getString("name", null);
		_feeId = set.getInt("feeId", Inventory.ADENA_ID);
		_feeCount = set.getInt("feeCount", 0);
		
		final String castleIds = set.getString("castleId", "");
		if (castleIds.isEmpty())
		{
			_castleId = Collections.emptyList();
		}
		else if (!castleIds.contains(";"))
		{
			_castleId = Collections.singletonList(Integer.parseInt(castleIds));
		}
		else
		{
			_castleId = new ArrayList<>();
			for (String castleId : castleIds.split(";"))
			{
				_castleId.add(Integer.parseInt(castleId));
			}
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getFeeId()
	{
		return _feeId;
	}
	
	public int getFeeCount()
	{
		return _feeCount;
	}
	
	public List<Integer> getCastleId()
	{
		return _castleId;
	}
}
