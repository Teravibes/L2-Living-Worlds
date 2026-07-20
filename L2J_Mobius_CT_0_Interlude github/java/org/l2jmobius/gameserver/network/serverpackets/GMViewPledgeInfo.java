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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class GMViewPledgeInfo extends ServerPacket
{
	private final Clan _clan;
	private final Player _player;
	
	public GMViewPledgeInfo(Clan clan, Player player)
	{
		_clan = clan;
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.GM_VIEW_PLEDGE_INFO.writeId(this, buffer);
		buffer.writeString(_player.getName());
		buffer.writeInt(_clan.getId());
		buffer.writeInt(0);
		buffer.writeString(_clan.getName());
		buffer.writeString(_clan.getLeaderName());
		buffer.writeInt(_clan.getCrestId()); // -> no, it's no longer used (nuocnam) fix by game
		buffer.writeInt(_clan.getLevel());
		buffer.writeInt(_clan.getCastleId());
		buffer.writeInt(_clan.getHideoutId());
		buffer.writeInt(_clan.getRank());
		buffer.writeInt(_clan.getReputationScore());
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(_clan.getAllyId()); // c2
		buffer.writeString(_clan.getAllyName()); // c2
		buffer.writeInt(_clan.getAllyCrestId()); // c2
		buffer.writeInt(_clan.isAtWar()); // c3
		
		final Collection<ClanMember> members = _clan.getMembers();
		buffer.writeInt(members.size());
		for (ClanMember member : members)
		{
			if (member != null)
			{
				buffer.writeString(member.getName());
				buffer.writeInt(member.getLevel());
				buffer.writeInt(member.getClassId());
				buffer.writeInt(member.getSex());
				buffer.writeInt(member.getRaceOrdinal());
				buffer.writeInt(member.isOnline() ? member.getObjectId() : 0);
				buffer.writeInt(member.getSponsor() != 0);
			}
		}
	}
}
