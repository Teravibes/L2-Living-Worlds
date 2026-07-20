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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.model.siege.manor.Seed;
import org.l2jmobius.gameserver.model.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author l3x
 */
public class ExShowSeedSetting extends ServerPacket
{
	private final int _manorId;
	private final Set<Seed> _seeds;
	private final Map<Integer, SeedProduction> _current = new HashMap<>();
	private final Map<Integer, SeedProduction> _next = new HashMap<>();
	
	public ExShowSeedSetting(int manorId)
	{
		final CastleManorManager manor = CastleManorManager.getInstance();
		_manorId = manorId;
		_seeds = manor.getSeedsForCastle(_manorId);
		for (Seed s : _seeds)
		{
			// Current period
			SeedProduction sp = manor.getSeedProduct(manorId, s.getSeedId(), false);
			if (sp != null)
			{
				_current.put(s.getSeedId(), sp);
			}
			
			// Next period
			sp = manor.getSeedProduct(manorId, s.getSeedId(), true);
			if (sp != null)
			{
				_next.put(s.getSeedId(), sp);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_SEED_SETTING.writeId(this, buffer);
		buffer.writeInt(_manorId); // manor id
		buffer.writeInt(_seeds.size()); // size
		SeedProduction sp;
		for (Seed s : _seeds)
		{
			buffer.writeInt(s.getSeedId()); // seed id
			buffer.writeInt(s.getLevel()); // level
			buffer.writeByte(1);
			buffer.writeInt(s.getReward(1)); // reward 1 id
			buffer.writeByte(1);
			buffer.writeInt(s.getReward(2)); // reward 2 id
			buffer.writeInt(s.getSeedLimit()); // next sale limit
			buffer.writeInt(s.getSeedReferencePrice()); // price for castle to produce 1
			buffer.writeInt(s.getSeedMinPrice()); // min seed price
			buffer.writeInt(s.getSeedMaxPrice()); // max seed price
			
			// Current period
			if (_current.containsKey(s.getSeedId()))
			{
				sp = _current.get(s.getSeedId());
				buffer.writeInt(sp.getStartAmount()); // sales
				buffer.writeInt(sp.getPrice()); // price
			}
			else
			{
				buffer.writeInt(0);
				buffer.writeInt(0);
			}
			
			// Next period
			if (_next.containsKey(s.getSeedId()))
			{
				sp = _next.get(s.getSeedId());
				buffer.writeInt(sp.getStartAmount()); // sales
				buffer.writeInt(sp.getPrice()); // price
			}
			else
			{
				buffer.writeInt(0);
				buffer.writeInt(0);
			}
		}
		
		_current.clear();
		_next.clear();
	}
}
