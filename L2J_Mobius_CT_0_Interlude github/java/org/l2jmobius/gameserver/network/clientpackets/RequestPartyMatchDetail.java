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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchRoom;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.model.groups.matching.PartyMatchWaitingList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExManagePartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.PartyMatchDetail;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Gnacik
 */
public class RequestPartyMatchDetail extends ClientPacket
{
	private int _roomid;
	@SuppressWarnings("unused")
	private int _unk1;
	@SuppressWarnings("unused")
	private int _unk2;
	@SuppressWarnings("unused")
	private int _unk3;
	
	@Override
	protected void readImpl()
	{
		_roomid = readInt();
		
		// If player click on Room all unk are 0
		// If player click AutoJoin values are -1 1 1
		_unk1 = readInt();
		_unk2 = readInt();
		_unk3 = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_roomid);
		if (room == null)
		{
			return;
		}
		
		if ((player.getLevel() >= room.getMinLevel()) && (player.getLevel() <= room.getMaxLevel()))
		{
			// Remove from waiting list
			PartyMatchWaitingList.getInstance().removePlayer(player);
			
			player.setPartyRoom(_roomid);
			
			player.sendPacket(new PartyMatchDetail(room));
			player.sendPacket(new ExPartyRoomMember(room, 0));
			for (Player member : room.getPartyMembers())
			{
				if (member == null)
				{
					continue;
				}
				
				member.sendPacket(new ExManagePartyRoomMember(player, room, 0));
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ENTERED_THE_PARTY_ROOM);
				sm.addString(player.getName());
				member.sendPacket(sm);
			}
			
			room.addMember(player);
			
			// Info Broadcast
			player.broadcastUserInfo();
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIREMENTS_TO_ENTER_THAT_PARTY_ROOM);
		}
	}
}
