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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Gnacik
 */
public class ExListPartyMatchingWaitingRoom extends ServerPacket
{
	private final Player _player;
	// private final int _page;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _mode;
	private final List<Player> _members;
	
	public ExListPartyMatchingWaitingRoom(Player player, int page, int minLevel, int maxLevel, int mode)
	{
		_player = player;
		// _page = page;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_mode = mode;
		_members = new ArrayList<>();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_LIST_PARTY_MATCHING_WAITING_ROOM.writeId(this, buffer);
		if (_mode == 0)
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			return;
		}
		
		for (Player cha : PartyMatchWaitingList.getInstance().getPlayers())
		{
			if ((cha == null) || (cha == _player))
			{
				continue;
			}
			
			if (!cha.isPartyWaiting())
			{
				PartyMatchWaitingList.getInstance().removePlayer(cha);
				continue;
			}
			else if ((cha.getLevel() < _minLevel) || (cha.getLevel() > _maxLevel))
			{
				continue;
			}
			
			_members.add(cha);
		}
		
		buffer.writeInt(1); // Page?
		buffer.writeInt(_members.size());
		for (Player member : _members)
		{
			buffer.writeString(member.getName());
			buffer.writeInt(member.getActiveClass());
			buffer.writeInt(member.getLevel());
		}
	}
}
