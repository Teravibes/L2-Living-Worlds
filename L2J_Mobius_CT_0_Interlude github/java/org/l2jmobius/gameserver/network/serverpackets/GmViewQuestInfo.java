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

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Tempy
 */
public class GmViewQuestInfo extends ServerPacket
{
	private final Player _player;
	
	public GmViewQuestInfo(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.GM_VIEW_QUEST_LIST.writeId(this, buffer);
		buffer.writeString(_player.getName());
		final List<Quest> questList = _player.getAllActiveQuests();
		if (questList.isEmpty())
		{
			buffer.writeByte(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			return;
		}
		
		buffer.writeShort(questList.size()); // quest count
		for (Quest q : questList)
		{
			buffer.writeInt(q.getId());
			final QuestState qs = _player.getQuestState(q.getName());
			if (qs == null)
			{
				buffer.writeInt(0);
				continue;
			}
			
			buffer.writeInt(qs.getCond()); // stage of quest progress
		}
	}
}
