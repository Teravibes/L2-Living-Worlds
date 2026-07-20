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
package org.l2jmobius.gameserver.model.actor.templates;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.instance.Door;

/**
 * Doors template.
 * @author JIV
 */
public class DoorTemplate extends CreatureTemplate
{
	private final int _doorId;
	private final int[] _nodeX;
	private final int[] _nodeY;
	private final int _nodeZ;
	private final int _height;
	private final int _posX;
	private final int _posY;
	private final int _posZ;
	private final int _emmiter;
	private final int _childDoorId;
	private final String _name;
	private final String _groupName;
	private final boolean _showHp;
	private final boolean _isWall;
	
	// -1 close, 0 nothing, 1 open
	private final byte _masterDoorClose;
	private final byte _masterDoorOpen;
	
	private final boolean _isTargetable;
	private final boolean _default_status;
	
	private int _openTime;
	private int _randomTime;
	private final int _closeTime;
	private final int _level;
	private final int _openType;
	private final boolean _checkCollision;
	private final boolean _isAttackableDoor;
	private final int _clanhallId;
	private final boolean _stealth;
	
	public DoorTemplate(StatSet set)
	{
		super(set);
		_doorId = set.getInt("id");
		_name = set.getString("name");
		
		// position
		final String[] pos = set.getString("pos").split(",");
		_height = set.getInt("height");
		_nodeZ = set.getInt("nodeZ");
		_nodeX = new int[4]; // 4 * x
		_nodeY = new int[4]; // 4 * y
		for (int i = 0; i < 4; i++)
		{
			final String[] split = set.getString("node" + (i + 1)).split(",");
			_nodeX[i] = Integer.parseInt(split[0]);
			_nodeY[i] = Integer.parseInt(split[1]);
		}
		
		_posX = Integer.parseInt(pos[0]);
		_posY = Integer.parseInt(pos[1]);
		_posZ = Math.min(Integer.parseInt(pos[2]), _nodeZ);
		
		// optional
		_emmiter = set.getInt("emitter_id", 0);
		_showHp = set.getBoolean("hp_showable", true);
		_isWall = set.getBoolean("is_wall", false);
		_groupName = set.getString("group", null);
		_childDoorId = set.getInt("child_id_event", -1);
		
		// true if door is opening
		String masterevent = set.getString("master_close_event", "act_nothing");
		_masterDoorClose = (byte) (masterevent.equals("act_open") ? 1 : masterevent.equals("act_close") ? -1 : 0);
		masterevent = set.getString("master_open_event", "act_nothing");
		_masterDoorOpen = (byte) (masterevent.equals("act_open") ? 1 : masterevent.equals("act_close") ? -1 : 0);
		_isTargetable = set.getBoolean("targetable", true);
		_default_status = set.getString("default_status", "close").equals("open");
		_closeTime = set.getInt("close_time", -1);
		_level = set.getInt("level", 0);
		_openType = set.getInt("open_method", 0);
		_checkCollision = set.getBoolean("check_collision", true);
		if ((_openType & Door.OPEN_BY_TIME) == Door.OPEN_BY_TIME)
		{
			_openTime = set.getInt("open_time");
			_randomTime = set.getInt("random_time", -1);
		}
		
		_isAttackableDoor = set.getBoolean("is_attackable", false);
		_clanhallId = set.getInt("clanhall_id", 0);
		_stealth = set.getBoolean("stealth", false);
	}
	
	/**
	 * Gets the door ID.
	 * @return the door ID
	 */
	public int getId()
	{
		return _doorId;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int[] getNodeX()
	{
		return _nodeX;
	}
	
	public int[] getNodeY()
	{
		return _nodeY;
	}
	
	public int getNodeZ()
	{
		return _nodeZ;
	}
	
	public int getHeight()
	{
		return _height;
	}
	
	public int getX()
	{
		return _posX;
	}
	
	public int getY()
	{
		return _posY;
	}
	
	public int getZ()
	{
		return _posZ;
	}
	
	public int getEmmiter()
	{
		return _emmiter;
	}
	
	public int getChildDoorId()
	{
		return _childDoorId;
	}
	
	public String getGroupName()
	{
		return _groupName;
	}
	
	public boolean isShowHp()
	{
		return _showHp;
	}
	
	public boolean isWall()
	{
		return _isWall;
	}
	
	public byte getMasterDoorOpen()
	{
		return _masterDoorOpen;
	}
	
	public byte getMasterDoorClose()
	{
		return _masterDoorClose;
	}
	
	public boolean isTargetable()
	{
		return _isTargetable;
	}
	
	public boolean isOpenByDefault()
	{
		return _default_status;
	}
	
	public int getOpenTime()
	{
		return _openTime;
	}
	
	public int getRandomTime()
	{
		return _randomTime;
	}
	
	public int getCloseTime()
	{
		return _closeTime;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getOpenType()
	{
		return _openType;
	}
	
	public boolean isCheckCollision()
	{
		return _checkCollision;
	}
	
	public boolean isAttackable()
	{
		return _isAttackableDoor;
	}
	
	public int getClanHallId()
	{
		return _clanhallId;
	}
	
	public boolean isStealth()
	{
		return _stealth;
	}
}
