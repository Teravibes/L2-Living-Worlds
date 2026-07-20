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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ShowBoard extends ServerPacket
{
	private final String _content;
	private int _showBoard = 1; // 1 show, 0 hide
	
	public ShowBoard(String htmlCode, String id)
	{
		_content = id + "\u0008" + htmlCode;
	}
	
	public ShowBoard(List<String> arg)
	{
		final StringBuilder builder = new StringBuilder(256).append("1002\u0008");
		for (String str : arg)
		{
			builder.append(str).append("\u0008");
		}
		
		_content = builder.toString();
	}
	
	/**
	 * Hides the community board
	 */
	public ShowBoard()
	{
		_showBoard = 0;
		_content = "";
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SHOW_BOARD.writeId(this, buffer);
		buffer.writeByte(_showBoard); // c4 1 to show community 00 to hide
		buffer.writeString("bypass _bbshome"); // top
		buffer.writeString("bypass _bbsgetfav"); // favorite
		buffer.writeString("bypass _bbsloc"); // region
		buffer.writeString("bypass _bbsclan"); // clan
		buffer.writeString("bypass _bbsmemo"); // memo
		buffer.writeString("bypass _bbsmail"); // mail
		buffer.writeString("bypass _bbsfriends"); // friends
		buffer.writeString("bypass bbs_add_fav"); // add fav.
		buffer.writeString(_content);
	}
}
