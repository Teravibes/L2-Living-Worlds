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

import java.util.Calendar;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * Shows the Siege Info<br>
 * <br>
 * c = c9<br>
 * d = CastleID<br>
 * d = Show Owner Controls (0x00 default || >=0x02(mask?) owner)<br>
 * d = Owner ClanID<br>
 * S = Owner ClanName<br>
 * S = Owner Clan LeaderName<br>
 * d = Owner AllyID<br>
 * S = Owner AllyName<br>
 * d = current time (seconds)<br>
 * d = Siege time (seconds) (0 for selectable)<br>
 * d = (UNKNOW) Siege Time Select Related?
 * @author KenM
 */
public class SiegeInfo extends ServerPacket
{
	private Castle _castle;
	private ClanHall _hall;
	private final Player _player;
	
	public SiegeInfo(Castle castle, Player player)
	{
		_castle = castle;
		_player = player;
	}
	
	public SiegeInfo(ClanHall hall, Player player)
	{
		_hall = hall;
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SIEGE_INFO.writeId(this, buffer);
		if (_castle != null)
		{
			buffer.writeInt(_castle.getResidenceId());
			final int ownerId = _castle.getOwnerId();
			buffer.writeInt((ownerId == _player.getClanId()) && _player.isClanLeader());
			buffer.writeInt(ownerId);
			if (ownerId > 0)
			{
				final Clan owner = ClanTable.getInstance().getClan(ownerId);
				if (owner != null)
				{
					buffer.writeString(owner.getName()); // Clan Name
					buffer.writeString(owner.getLeaderName()); // Clan Leader Name
					buffer.writeInt(owner.getAllyId()); // Ally ID
					buffer.writeString(owner.getAllyName()); // Ally Name
				}
				else
				{
					PacketLogger.warning("Null owner for castle: " + _castle.getName());
				}
			}
			else
			{
				buffer.writeString(""); // Clan Name
				buffer.writeString(""); // Clan Leader Name
				buffer.writeInt(0); // Ally ID
				buffer.writeString(""); // Ally Name
			}
			
			buffer.writeInt((int) (System.currentTimeMillis() / 1000));
			if (!_castle.isTimeRegistrationOver() && _player.isClanLeader() && (_player.getClanId() == _castle.getOwnerId()))
			{
				final Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(_castle.getSiegeDate().getTimeInMillis());
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				buffer.writeInt(0);
				buffer.writeInt(FeatureConfig.SIEGE_HOUR_LIST.size());
				for (int hour : FeatureConfig.SIEGE_HOUR_LIST)
				{
					cal.set(Calendar.HOUR_OF_DAY, hour);
					buffer.writeInt((int) (cal.getTimeInMillis() / 1000));
				}
			}
			else
			{
				buffer.writeInt((int) (_castle.getSiegeDate().getTimeInMillis() / 1000));
				buffer.writeInt(0);
			}
		}
		else
		{
			buffer.writeInt(_hall.getId());
			final int ownerId = _hall.getOwnerId();
			buffer.writeInt(((ownerId == _player.getClanId()) && (_player.isClanLeader())));
			buffer.writeInt(ownerId);
			if (ownerId > 0)
			{
				final Clan owner = ClanTable.getInstance().getClan(ownerId);
				if (owner != null)
				{
					buffer.writeString(owner.getName()); // Clan Name
					buffer.writeString(owner.getLeaderName()); // Clan Leader Name
					buffer.writeInt(owner.getAllyId()); // Ally ID
					buffer.writeString(owner.getAllyName()); // Ally Name
				}
				else
				{
					PacketLogger.warning("Null owner for siegable hall: " + _hall.getName());
				}
			}
			else
			{
				buffer.writeString(""); // Clan Name
				buffer.writeString(""); // Clan Leader Name
				buffer.writeInt(0); // Ally ID
				buffer.writeString(""); // Ally Name
			}
			
			buffer.writeInt((int) (System.currentTimeMillis() / 1000));
			buffer.writeInt((int) ((CHSiegeManager.getInstance().getSiegableHall(_hall.getId()).getNextSiegeTime()) / 1000));
			buffer.writeInt(0); // number of choices?
		}
	}
}
