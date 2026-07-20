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

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class QuestList extends ServerPacket
{
	final Player _player;
	
	public QuestList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		/**
		 * <pre>
		 * This text was wrote by XaKa
		 * QuestList packet structure:
		 * {
		 * 		1 byte - 0x80
		 * 		2 byte - Number of Quests
		 * 		for Quest in AvailibleQuests
		 * 		{
		 * 			4 byte - Quest ID
		 * 			4 byte - Quest Status
		 * 		}
		 * }
		 * 
		 * NOTE: The following special constructs are true for the 4-byte Quest Status:
		 * If the most significant bit is 0, this means that no progress-step got skipped.
		 * In this case, merely passing the rank of the latest step gets the client to mark
		 * it as current and mark all previous steps as complete.
		 * If the most significant bit is 1, it means that some steps may have been skipped.
		 * In that case, each bit represents a quest step (max 30) with 0 indicating that it was
		 * skipped and 1 indicating that it either got completed or is currently active (the client
		 * will automatically assume the largest step as active and all smaller ones as completed).
		 * For example, the following bit sequences will yield the same results:
		 * 1000 0000 0000 0000 0000 0011 1111 1111: Indicates some steps may be skipped but each of
		 * the first 10 steps did not get skipped and current step is the 10th.
		 * 0000 0000 0000 0000 0000 0000 0000 1010: Indicates that no steps were skipped and current is the 10th.
		 * It is speculated that the latter will be processed faster by the client, so it is preferred when no
		 * steps have been skipped.
		 * However, the sequence "1000 0000 0000 0000 0000 0010 1101 1111" indicates that the current step is
		 * the 10th but the 6th and 9th are not to be shown at all (not completed, either).
		 * </pre>
		 */
		final List<Quest> quests = _player.getAllActiveQuests();
		ServerPackets.QUEST_LIST.writeId(this, buffer);
		buffer.writeShort(quests.size());
		for (Quest q : quests)
		{
			buffer.writeInt(q.getId());
			final QuestState qs = _player.getQuestState(q.getName());
			if (qs == null)
			{
				buffer.writeInt(0);
				continue;
			}
			
			final int states = qs.getInt("__compltdStateFlags");
			if (states > 0)
			{
				buffer.writeInt(states);
			}
			else
			{
				buffer.writeInt(qs.getCond());
			}
		}
		
		// packet.writeB(new byte[128]);
	}
}
