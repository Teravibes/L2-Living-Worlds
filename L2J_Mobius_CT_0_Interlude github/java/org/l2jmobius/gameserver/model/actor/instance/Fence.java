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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.FenceState;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ExColosseumFenceInfo;

/**
 * @author HorridoJoho
 */
public class Fence extends WorldObject
{
	private final int _xMin;
	private final int _xMax;
	private final int _yMin;
	private final int _yMax;
	
	private final String _name;
	private final int _width;
	private final int _length;
	
	private FenceState _state;
	private int[] _heightFences;
	
	public Fence(int x, int y, String name, int width, int length, int height, FenceState state)
	{
		super(IdManager.getInstance().getNextId());
		
		_xMin = x - (width / 2);
		_xMax = x + (width / 2);
		_yMin = y - (length / 2);
		_yMax = y + (length / 2);
		_name = name;
		_width = width;
		_length = length;
		_state = state;
		if (height > 1)
		{
			_heightFences = new int[height - 1];
			for (int i = 0; i < _heightFences.length; i++)
			{
				_heightFences[i] = IdManager.getInstance().getNextId();
			}
		}
	}
	
	@Override
	public int getId()
	{
		return getObjectId();
	}
	
	@Override
	public String getName()
	{
		return _name;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new ExColosseumFenceInfo(this));
		if (_heightFences != null)
		{
			for (int objId : _heightFences)
			{
				player.sendPacket(new ExColosseumFenceInfo(objId, getX(), getY(), getZ(), _width, _length, _state));
			}
		}
	}
	
	@Override
	public boolean decayMe()
	{
		if (_heightFences != null)
		{
			final DeleteObject[] deleteObjects = new DeleteObject[_heightFences.length];
			for (int i = 0; i < _heightFences.length; i++)
			{
				deleteObjects[i] = new DeleteObject(_heightFences[i]);
			}
			
			World.getInstance().forEachVisibleObject(this, Player.class, player ->
			{
				for (int i = 0; i < _heightFences.length; i++)
				{
					player.sendPacket(deleteObjects[i]);
				}
			});
		}
		
		return super.decayMe();
	}
	
	public boolean deleteMe()
	{
		decayMe();
		
		FenceData.getInstance().removeFence(this);
		return false;
	}
	
	public FenceState getState()
	{
		return _state;
	}
	
	public void setState(FenceState type)
	{
		_state = type;
		broadcastInfo();
	}
	
	public int getWidth()
	{
		return _width;
	}
	
	public int getLength()
	{
		return _length;
	}
	
	public int getXMin()
	{
		return _xMin;
	}
	
	public int getYMin()
	{
		return _yMin;
	}
	
	public int getXMax()
	{
		return _xMax;
	}
	
	public int getYMax()
	{
		return _yMax;
	}
	
	@Override
	public boolean isFence()
	{
		return true;
	}
}
