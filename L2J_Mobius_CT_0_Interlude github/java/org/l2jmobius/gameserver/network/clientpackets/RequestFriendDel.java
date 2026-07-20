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
package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.L2Friend;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestFriendDel extends ClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		SystemMessage sm;
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int id = CharInfoTable.getInstance().getIdByName(_name);
		if (id == -1)
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_NOT_ON_YOUR_FRIEND_LIST);
			sm.addString(_name);
			player.sendPacket(sm);
			return;
		}
		
		if (!player.getFriendList().contains(id))
		{
			sm = new SystemMessage(SystemMessageId.S1_IS_NOT_ON_YOUR_FRIEND_LIST);
			sm.addString(_name);
			player.sendPacket(sm);
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_friends WHERE (charId=? AND friendId=?) OR (charId=? AND friendId=?)"))
		{
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, id);
			statement.setInt(3, id);
			statement.setInt(4, player.getObjectId());
			statement.execute();
			
			// Player deleted from your friend list
			sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			player.sendPacket(sm);
			
			player.getFriendList().remove(Integer.valueOf(id));
			
			final Player target = World.getInstance().getPlayer(_name);
			if (target != null)
			{
				target.getFriendList().remove(Integer.valueOf(player.getObjectId()));
				player.sendPacket(new L2Friend(target, 3));
				target.sendPacket(new L2Friend(player, 3));
			}
		}
		catch (Exception e)
		{
			PacketLogger.warning(getClass().getSimpleName() + ": Could not delele friend objectId: " + e.getMessage());
		}
	}
}
