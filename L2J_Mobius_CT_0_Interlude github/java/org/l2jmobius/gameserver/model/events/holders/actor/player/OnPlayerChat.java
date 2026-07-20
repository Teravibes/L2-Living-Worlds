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
package org.l2jmobius.gameserver.model.events.holders.actor.player;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * @author UnAfraid
 */
public class OnPlayerChat implements IBaseEvent
{
	private final Player _player;
	private final Player _target;
	private final String _text;
	private final ChatType _type;
	
	public OnPlayerChat(Player player, Player target, String text, ChatType type)
	{
		_player = player;
		_target = target;
		_text = text;
		_type = type;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public Player getTarget()
	{
		return _target;
	}
	
	public String getText()
	{
		return _text;
	}
	
	public ChatType getChatType()
	{
		return _type;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_CHAT;
	}
}
