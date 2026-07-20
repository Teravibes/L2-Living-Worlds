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

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author zabbix
 */
public class RequestLinkHtml extends ClientPacket
{
	private String _link;
	
	@Override
	protected void readImpl()
	{
		_link = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_link.isEmpty())
		{
			PacketLogger.warning(player + " sent empty html link!");
			return;
		}
		
		if (_link.contains(".."))
		{
			PacketLogger.warning(player + " sent invalid html link: link " + _link);
			return;
		}
		
		final int htmlObjectId = player.validateHtmlAction("link " + _link);
		if (htmlObjectId == -1)
		{
			PacketLogger.warning(player + " sent non cached html link: link " + _link);
			return;
		}
		
		if ((htmlObjectId > 0) && !LocationUtil.isInsideRangeOfObjectId(player, htmlObjectId, Npc.INTERACTION_DISTANCE))
		{
			// No logging here, this could be a common case
			return;
		}
		
		final String filename = "data/html/" + _link;
		final NpcHtmlMessage msg = new NpcHtmlMessage(htmlObjectId);
		msg.setFile(player, filename);
		player.sendPacket(msg);
	}
}
