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
package org.l2jmobius.gameserver.model.groups.matching;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;

/**
 * @author Gnacik
 */
public class PartyMatchRoomList
{
	private int _maxid = 1;
	private final Map<Integer, PartyMatchRoom> _rooms;
	
	protected PartyMatchRoomList()
	{
		_rooms = new ConcurrentHashMap<>();
	}
	
	public synchronized void addPartyMatchRoom(int id, PartyMatchRoom room)
	{
		_rooms.put(id, room);
		_maxid++;
	}
	
	public void deleteRoom(int id)
	{
		for (Player _member : getRoom(id).getPartyMembers())
		{
			if (_member == null)
			{
				continue;
			}
			
			_member.sendPacket(new ExClosePartyRoom());
			_member.sendPacket(SystemMessageId.THE_PARTY_ROOM_HAS_BEEN_DISBANDED);
			
			_member.setPartyRoom(0);
			// _member.setPartyMatching(0);
			_member.broadcastUserInfo();
		}
		
		_rooms.remove(id);
	}
	
	public PartyMatchRoom getRoom(int id)
	{
		return _rooms.get(id);
	}
	
	public Collection<PartyMatchRoom> getRooms()
	{
		return _rooms.values();
	}
	
	public int getPartyMatchRoomCount()
	{
		return _rooms.size();
	}
	
	public int getMaxId()
	{
		return _maxid;
	}
	
	public PartyMatchRoom getPlayerRoom(Player player)
	{
		for (PartyMatchRoom _room : _rooms.values())
		{
			for (Player member : _room.getPartyMembers())
			{
				if (member.equals(player))
				{
					return _room;
				}
			}
		}
		
		return null;
	}
	
	public int getPlayerRoomId(Player player)
	{
		for (PartyMatchRoom _room : _rooms.values())
		{
			for (Player member : _room.getPartyMembers())
			{
				if (member.equals(player))
				{
					return _room.getId();
				}
			}
		}
		
		return -1;
	}
	
	public static PartyMatchRoomList getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PartyMatchRoomList INSTANCE = new PartyMatchRoomList();
	}
}
