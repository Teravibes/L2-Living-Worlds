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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;
import org.l2jmobius.gameserver.managers.PhantomManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.L2FriendSay;

/**
 * Recieve Private (Friend) Message - 0xCC Format: c SS S: Message S: Receiving Player
 * @author Tempy
 */
public class RequestSendFriendMsg extends ClientPacket
{
	private static Logger LOGGER_CHAT = Logger.getLogger("chat");
	
	private String _message;
	private String _reciever;
	
	@Override
	protected void readImpl()
	{
		_message = readString();
		_reciever = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_message == null) || _message.isEmpty() || (_message.length() > 300))
		{
			return;
		}
		
		final Player targetPlayer = World.getInstance().getPlayer(_reciever);
		if ((targetPlayer == null) || !targetPlayer.getFriendList().contains(player.getObjectId()))
		{
			player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
			return;
		}

		// A friend PM to a "regular" phantom is answered by the LLM brain in the regular's stable persona,
		// replying back over this same friend channel - the regular is clientless, so nothing else would run.
		if (PhantomManager.getInstance().isRegular(targetPlayer))
		{
			FakePlayerChatManager.getInstance().handleFriendMessage(player, targetPlayer, _message);
			return;
		}

		if (GeneralConfig.LOG_CHAT)
		{
			final StringBuilder sb = new StringBuilder();
			sb.append("PRIV_MSG [");
			sb.append(player);
			sb.append(" to ");
			sb.append(targetPlayer);
			sb.append("] ");
			sb.append(_message);
			LOGGER_CHAT.info(sb.toString());
		}
		
		targetPlayer.sendPacket(new L2FriendSay(player.getName(), _reciever, _message));
	}
}
