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

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.model.siege.manor.Seed;
import org.l2jmobius.gameserver.model.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author l3x
 */
public class ExShowSeedInfo extends ServerPacket
{
	private final List<SeedProduction> _seeds;
	private final int _manorId;
	private final boolean _hideButtons;
	
	public ExShowSeedInfo(int manorId, boolean nextPeriod, boolean hideButtons)
	{
		_manorId = manorId;
		_hideButtons = hideButtons;
		final CastleManorManager manor = CastleManorManager.getInstance();
		_seeds = (nextPeriod && !manor.isManorApproved()) ? null : manor.getSeedProduction(manorId, nextPeriod);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_SEED_INFO.writeId(this, buffer);
		buffer.writeByte(_hideButtons); // Hide "Seed Purchase" button
		buffer.writeInt(_manorId); // Manor ID
		buffer.writeInt(0); // Unknown
		if (_seeds == null)
		{
			buffer.writeInt(0);
			return;
		}
		
		buffer.writeInt(_seeds.size());
		for (SeedProduction seed : _seeds)
		{
			buffer.writeInt(seed.getId()); // Seed id
			buffer.writeInt(seed.getAmount()); // Left to buy
			buffer.writeInt(seed.getStartAmount()); // Started amount
			buffer.writeInt(seed.getPrice()); // Sell Price
			final Seed s = CastleManorManager.getInstance().getSeed(seed.getId());
			if (s == null)
			{
				buffer.writeInt(0); // Seed level
				buffer.writeByte(1); // Reward 1
				buffer.writeInt(0); // Reward 1 - item id
				buffer.writeByte(1); // Reward 2
				buffer.writeInt(0); // Reward 2 - item id
			}
			else
			{
				buffer.writeInt(s.getLevel()); // Seed level
				buffer.writeByte(1); // Reward 1
				buffer.writeInt(s.getReward(1)); // Reward 1 - item id
				buffer.writeByte(1); // Reward 2
				buffer.writeInt(s.getReward(2)); // Reward 2 - item id
			}
		}
	}
}
