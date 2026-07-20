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
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PledgeShowInfoUpdate extends ServerPacket
{
	private final Clan _clan;
	
	public PledgeShowInfoUpdate(Clan clan)
	{
		_clan = clan;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PLEDGE_SHOW_INFO_UPDATE.writeId(this, buffer);
		
		// sending empty data so client will ask all the info in response ;)
		buffer.writeInt(_clan.getId());
		buffer.writeInt(_clan.getCrestId());
		buffer.writeInt(_clan.getLevel()); // clan level
		buffer.writeInt(_clan.getCastleId());
		buffer.writeInt(_clan.getHideoutId());
		buffer.writeInt(_clan.getRank());
		buffer.writeInt(_clan.getReputationScore()); // clan reputation score
		buffer.writeInt(0); // ?
		buffer.writeInt(0); // ?
		buffer.writeInt(_clan.getAllyId());
		buffer.writeString(_clan.getAllyName()); // c5
		buffer.writeInt(_clan.getAllyCrestId()); // c5
		buffer.writeInt(_clan.isAtWar()); // c5
	}
}
