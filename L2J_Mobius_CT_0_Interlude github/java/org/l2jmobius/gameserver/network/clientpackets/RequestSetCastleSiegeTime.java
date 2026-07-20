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

import java.util.Calendar;
import java.util.Date;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author UnAfraid
 */
public class RequestSetCastleSiegeTime extends ClientPacket
{
	private int _castleId;
	private long _time;
	
	@Override
	protected void readImpl()
	{
		_castleId = readInt();
		_time = readInt();
		_time *= 1000;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if ((player == null) || (castle == null))
		{
			PacketLogger.warning("[C]RequestSetCastleSiegeTime: activeChar: " + player + " castle: " + castle + " castleId: " + _castleId);
			return;
		}
		
		if ((castle.getOwnerId() > 0) && (castle.getOwnerId() != player.getClanId()))
		{
			PacketLogger.warning("[C]RequestSetCastleSiegeTime: activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to change siege date of not his own castle!");
		}
		else if (!player.isClanLeader())
		{
			PacketLogger.warning("[C]RequestSetCastleSiegeTime: activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to change siege date but is not clan leader!");
		}
		else if (!castle.isTimeRegistrationOver())
		{
			if (isSiegeTimeValid(castle.getSiegeDate().getTimeInMillis(), _time))
			{
				castle.getSiegeDate().setTimeInMillis(_time);
				castle.setTimeRegistrationOver(true);
				castle.getSiege().saveSiegeDate();
				final SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_ANNOUNCED_THE_CASTLE_SIEGE_TIME);
				msg.addCastleId(_castleId);
				Broadcast.toAllOnlinePlayers(msg);
				player.sendPacket(new SiegeInfo(castle, player));
			}
			else
			{
				PacketLogger.warning("[C]RequestSetCastleSiegeTime: activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to an invalid time (" + new Date(_time) + " !");
			}
		}
		else
		{
			PacketLogger.warning("[C]RequestSetCastleSiegeTime: activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to change siege date but currently not possible!");
		}
	}
	
	private static boolean isSiegeTimeValid(long siegeDate, long choosenDate)
	{
		final Calendar cal1 = Calendar.getInstance();
		cal1.setTimeInMillis(siegeDate);
		cal1.set(Calendar.MINUTE, 0);
		cal1.set(Calendar.SECOND, 0);
		
		final Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(choosenDate);
		
		for (int hour : FeatureConfig.SIEGE_HOUR_LIST)
		{
			cal1.set(Calendar.HOUR_OF_DAY, hour);
			if (isEqual(cal1, cal2, Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static boolean isEqual(Calendar cal1, Calendar cal2, int... fields)
	{
		for (int field : fields)
		{
			if (cal1.get(field) != cal2.get(field))
			{
				return false;
			}
		}
		
		return true;
	}
}
