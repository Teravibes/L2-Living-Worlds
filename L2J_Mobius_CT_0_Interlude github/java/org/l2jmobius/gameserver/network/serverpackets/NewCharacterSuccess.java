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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class NewCharacterSuccess extends ServerPacket
{
	private final List<PlayerTemplate> _chars = new ArrayList<>();
	
	public void addChar(PlayerTemplate template)
	{
		_chars.add(template);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHAR_TEMPLATES.writeId(this, buffer);
		buffer.writeInt(_chars.size());
		for (PlayerTemplate chr : _chars)
		{
			if (chr == null)
			{
				continue;
			}
			
			// TODO: Unhardcode these
			buffer.writeInt(chr.getRace().ordinal());
			buffer.writeInt(chr.getPlayerClass().getId());
			buffer.writeInt(0x46);
			buffer.writeInt(chr.getBaseSTR());
			buffer.writeInt(0x0A);
			buffer.writeInt(0x46);
			buffer.writeInt(chr.getBaseDEX());
			buffer.writeInt(0x0A);
			buffer.writeInt(0x46);
			buffer.writeInt(chr.getBaseCON());
			buffer.writeInt(0x0A);
			buffer.writeInt(0x46);
			buffer.writeInt(chr.getBaseINT());
			buffer.writeInt(0x0A);
			buffer.writeInt(0x46);
			buffer.writeInt(chr.getBaseWIT());
			buffer.writeInt(0x0A);
			buffer.writeInt(0x46);
			buffer.writeInt(chr.getBaseMEN());
			buffer.writeInt(0x0A);
		}
	}
}
