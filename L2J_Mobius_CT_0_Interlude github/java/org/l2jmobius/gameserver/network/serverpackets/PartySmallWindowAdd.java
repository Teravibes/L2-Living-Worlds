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

public class PartySmallWindowAdd extends ServerPacket
{
	private final Player _member;
	private final Party _party;
	
	public PartySmallWindowAdd(Player member, Party party)
	{
		_member = member;
		_party = party;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PARTY_SMALL_WINDOW_ADD.writeId(this, buffer);
		buffer.writeInt(_party.getLeaderObjectId()); // c3
		buffer.writeInt(_party.getDistributionType().getId()); // buffer.writeInt(4); ?? //c3
		buffer.writeInt(_member.getObjectId());
		buffer.writeString(_member.getName());
		buffer.writeInt((int) _member.getCurrentCp()); // c4
		buffer.writeInt(_member.getMaxCp()); // c4
		buffer.writeInt((int) _member.getCurrentHp());
		buffer.writeInt(_member.getMaxHp());
		buffer.writeInt((int) _member.getCurrentMp());
		buffer.writeInt(_member.getMaxMp());
		buffer.writeInt(_member.getLevel());
		buffer.writeInt(_member.getPlayerClass().getId());
		buffer.writeInt(0); // ?
		buffer.writeInt(0); // ?
	}
}
