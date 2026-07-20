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
import org.l2jmobius.gameserver.model.item.Henna;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class HennaItemRemoveInfo extends ServerPacket
{
	private final Player _player;
	private final Henna _henna;
	
	public HennaItemRemoveInfo(Henna henna, Player player)
	{
		_henna = henna;
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.HENNA_ITEM_REMOVE_INFO.writeId(this, buffer);
		buffer.writeInt(_henna.getDyeId()); // symbol Id
		buffer.writeInt(_henna.getDyeItemId()); // item id of dye
		buffer.writeInt(_henna.getCancelCount()); // total amount of dye require
		buffer.writeInt(_henna.getCancelFee()); // total amount of Adena require to remove symbol
		buffer.writeInt(_henna.isAllowedClass(_player.getPlayerClass())); // able to remove or not
		buffer.writeInt(_player.getAdena());
		buffer.writeInt(_player.getINT()); // current INT
		buffer.writeByte(_player.getINT() - _henna.getStatINT()); // equip INT
		buffer.writeInt(_player.getSTR()); // current STR
		buffer.writeByte(_player.getSTR() - _henna.getStatSTR()); // equip STR
		buffer.writeInt(_player.getCON()); // current CON
		buffer.writeByte(_player.getCON() - _henna.getStatCON()); // equip CON
		buffer.writeInt(_player.getMEN()); // current MEN
		buffer.writeByte(_player.getMEN() - _henna.getStatMEN()); // equip MEN
		buffer.writeInt(_player.getDEX()); // current DEX
		buffer.writeByte(_player.getDEX() - _henna.getStatDEX()); // equip DEX
		buffer.writeInt(_player.getWIT()); // current WIT
		buffer.writeByte(_player.getWIT() - _henna.getStatWIT()); // equip WIT
	}
}
