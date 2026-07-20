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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PartySmallWindowAll extends ServerPacket
{
	private final Party _party;
	private final Player _exclude;
	
	public PartySmallWindowAll(Player exclude, Party party)
	{
		_exclude = exclude;
		_party = party;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PARTY_SMALL_WINDOW_ALL.writeId(this, buffer);
		buffer.writeInt(_party.getLeaderObjectId());
		buffer.writeInt(_party.getDistributionType().getId());
		buffer.writeInt(_party.getMemberCount() - 1);
		for (Player member : _party.getMembers())
		{
			if ((member != null) && (member != _exclude))
			{
				buffer.writeInt(member.getObjectId());
				buffer.writeString(member.getName());
				buffer.writeInt((int) member.getCurrentCp()); // c4
				buffer.writeInt(member.getMaxCp()); // c4
				buffer.writeInt((int) member.getCurrentHp());
				buffer.writeInt(member.getMaxHp());
				buffer.writeInt((int) member.getCurrentMp());
				buffer.writeInt(member.getMaxMp());
				buffer.writeInt(member.getLevel());
				buffer.writeInt(member.getPlayerClass().getId());
				buffer.writeInt(0); // buffer.writeInt(1); ??
				buffer.writeInt(member.getRace().ordinal());
			}
		}
	}
}
