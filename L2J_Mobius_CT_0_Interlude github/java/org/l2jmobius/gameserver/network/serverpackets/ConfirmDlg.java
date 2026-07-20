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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage.SMParam;

/**
 * @author kombat
 */
public class ConfirmDlg extends ServerPacket
{
	private int _time;
	private int _requesterId;
	private final SystemMessage _systemMessage;
	
	public ConfirmDlg(SystemMessageId smId)
	{
		_systemMessage = new SystemMessage(smId);
	}
	
	public ConfirmDlg(int id)
	{
		_systemMessage = new SystemMessage(id);
	}
	
	public ConfirmDlg(String text)
	{
		_systemMessage = new SystemMessage(SystemMessageId.S1_3);
		_systemMessage.addString(text);
	}
	
	public ConfirmDlg addTime(int time)
	{
		_time = time;
		return this;
	}
	
	public ConfirmDlg addRequesterId(int id)
	{
		_requesterId = id;
		return this;
	}
	
	public SystemMessage getSystemMessage()
	{
		return _systemMessage;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CONFIRM_DLG.writeId(this, buffer);
		final SMParam[] params = _systemMessage.getParams();
		buffer.writeInt(_systemMessage.getId());
		buffer.writeInt(params.length);
		for (SMParam param : params)
		{
			buffer.writeInt(param.getType());
			switch (param.getType())
			{
				case SystemMessage.TYPE_TEXT:
				case SystemMessage.TYPE_PLAYER_NAME:
				{
					buffer.writeString(param.getStringValue());
					break;
				}
				case SystemMessage.TYPE_LONG_NUMBER:
				{
					buffer.writeLong(param.getLongValue());
					break;
				}
				case SystemMessage.TYPE_ITEM_NAME:
				case SystemMessage.TYPE_CASTLE_NAME:
				case SystemMessage.TYPE_INT_NUMBER:
				case SystemMessage.TYPE_NPC_NAME:
				case SystemMessage.TYPE_ELEMENT_NAME:
				case SystemMessage.TYPE_SYSTEM_STRING:
				case SystemMessage.TYPE_INSTANCE_NAME:
				case SystemMessage.TYPE_DOOR_NAME:
				{
					buffer.writeInt(param.getIntValue());
					break;
				}
				case SystemMessage.TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // SkillId
					buffer.writeInt(array[1]); // SkillLevel
					break;
				}
				case SystemMessage.TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // x
					buffer.writeInt(array[1]); // y
					buffer.writeInt(array[2]); // z
					break;
				}
			}
		}
		
		buffer.writeInt(_time);
		buffer.writeInt(_requesterId);
	}
}
