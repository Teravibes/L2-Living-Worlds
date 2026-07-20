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

import java.util.Map;

import org.l2jmobius.gameserver.managers.RaidBossPointsManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBossRecord;

/**
 * Format: (ch) d
 * @author -Wooden-
 */
public class RequestGetBossRecord extends ClientPacket
{
	private int _bossId;
	
	@Override
	protected void readImpl()
	{
		_bossId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_bossId != 0)
		{
			PacketLogger.info("C5: RequestGetBossRecord: d: " + _bossId + " ActiveChar: " + player); // should be always 0, log it if is not 0 for furture research
		}
		
		final int points = RaidBossPointsManager.getInstance().getPointsByOwnerId(player.getObjectId());
		final int ranking = RaidBossPointsManager.getInstance().calculateRanking(player.getObjectId());
		final Map<Integer, Integer> list = RaidBossPointsManager.getInstance().getList(player);
		
		// trigger packet
		player.sendPacket(new ExGetBossRecord(ranking, points, list));
	}
}
