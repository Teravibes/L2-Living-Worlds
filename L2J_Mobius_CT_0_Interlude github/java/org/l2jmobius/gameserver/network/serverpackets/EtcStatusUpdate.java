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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Luca Baldi
 */
public class EtcStatusUpdate extends ServerPacket
{
	private final Player _player;
	
	public EtcStatusUpdate(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.ETC_STATUS_UPDATE.writeId(this, buffer);
		buffer.writeInt(_player.getCharges()); // 1-7 increase force, level
		buffer.writeInt(_player.getWeightPenalty()); // 1-4 weight penalty, level (1=50%, 2=66.6%, 3=80%, 4=100%)
		buffer.writeInt(_player.getMessageRefusal() || _player.isChatBanned() || _player.isSilenceMode()); // 1 = block all chat
		buffer.writeInt(_player.isInsideZone(ZoneId.DANGER_AREA)); // 1 = danger area
		buffer.writeInt((_player.getExpertiseWeaponPenalty() > 0) || (_player.getExpertiseArmorPenalty() > 0)); // Weapon Grade Penalty [1-4] - Armor Grade Penalty [1-4]
		buffer.writeInt(_player.hasCharmOfCourage() ? 1 : 0); // 1 = charm of courage (allows resurrection on the same spot upon death on the siege battlefield)
		buffer.writeInt(_player.getDeathPenaltyBuffLevel()); // 1-15 death penalty, level (combat ability decreased due to death)
	}
}
