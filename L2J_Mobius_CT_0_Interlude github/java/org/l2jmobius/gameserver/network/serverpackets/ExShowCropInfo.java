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
import org.l2jmobius.gameserver.model.siege.manor.CropProcure;
import org.l2jmobius.gameserver.model.siege.manor.Seed;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author l3x
 */
public class ExShowCropInfo extends ServerPacket
{
	private final List<CropProcure> _crops;
	private final int _manorId;
	private final boolean _hideButtons;
	
	public ExShowCropInfo(int manorId, boolean nextPeriod, boolean hideButtons)
	{
		_manorId = manorId;
		_hideButtons = hideButtons;
		final CastleManorManager manor = CastleManorManager.getInstance();
		_crops = (nextPeriod && !manor.isManorApproved()) ? null : manor.getCropProcure(manorId, nextPeriod);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_CROP_INFO.writeId(this, buffer);
		buffer.writeByte(_hideButtons); // Hide "Crop Sales" button
		buffer.writeInt(_manorId); // Manor ID
		buffer.writeInt(0);
		if (_crops == null)
		{
			buffer.writeInt(0);
			return;
		}
		
		buffer.writeInt(_crops.size());
		for (CropProcure crop : _crops)
		{
			buffer.writeInt(crop.getId()); // Crop id
			buffer.writeInt(crop.getAmount()); // Buy residual
			buffer.writeInt(crop.getStartAmount()); // Buy
			buffer.writeInt(crop.getPrice()); // Buy price
			buffer.writeByte(crop.getReward()); // Reward
			final Seed seed = CastleManorManager.getInstance().getSeedByCrop(crop.getId());
			if (seed == null)
			{
				buffer.writeInt(0); // Seed level
				buffer.writeByte(1); // Reward 1
				buffer.writeInt(0); // Reward 1 - item id
				buffer.writeByte(1); // Reward 2
				buffer.writeInt(0); // Reward 2 - item id
			}
			else
			{
				buffer.writeInt(seed.getLevel()); // Seed level
				buffer.writeByte(1); // Reward 1
				buffer.writeInt(seed.getReward(1)); // Reward 1 - item id
				buffer.writeByte(1); // Reward 2
				buffer.writeInt(seed.getReward(2)); // Reward 2 - item id
			}
		}
	}
}
