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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.siege.manor.CropProcure;
import org.l2jmobius.gameserver.model.siege.manor.Seed;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author l3x
 */
public class ExShowSellCropList extends ServerPacket
{
	private final int _manorId;
	private final Map<Integer, Item> _cropsItems = new HashMap<>();
	private final Map<Integer, CropProcure> _castleCrops = new HashMap<>();
	
	public ExShowSellCropList(PlayerInventory inventory, int manorId)
	{
		_manorId = manorId;
		for (int cropId : CastleManorManager.getInstance().getCropIds())
		{
			final Item item = inventory.getItemByItemId(cropId);
			if (item != null)
			{
				_cropsItems.put(cropId, item);
			}
		}
		
		for (CropProcure crop : CastleManorManager.getInstance().getCropProcure(_manorId, false))
		{
			if (_cropsItems.containsKey(crop.getId()) && (crop.getAmount() > 0))
			{
				_castleCrops.put(crop.getId(), crop);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_SELL_CROP_LIST.writeId(this, buffer);
		buffer.writeInt(_manorId); // manor id
		buffer.writeInt(_cropsItems.size()); // size
		for (Item item : _cropsItems.values())
		{
			final Seed seed = CastleManorManager.getInstance().getSeedByCrop(item.getId());
			buffer.writeInt(item.getObjectId()); // Object id
			buffer.writeInt(item.getId()); // crop id
			buffer.writeInt(seed.getLevel()); // seed level
			buffer.writeByte(1);
			buffer.writeInt(seed.getReward(1)); // reward 1 id
			buffer.writeByte(1);
			buffer.writeInt(seed.getReward(2)); // reward 2 id
			if (_castleCrops.containsKey(item.getId()))
			{
				final CropProcure crop = _castleCrops.get(item.getId());
				buffer.writeInt(_manorId); // manor
				buffer.writeInt(crop.getAmount()); // buy residual
				buffer.writeInt(crop.getPrice()); // buy price
				buffer.writeByte(crop.getReward()); // reward
			}
			else
			{
				buffer.writeInt(0xFFFFFFFF); // manor
				buffer.writeInt(0); // buy residual
				buffer.writeInt(0); // buy price
				buffer.writeByte(0); // reward
			}
			
			buffer.writeInt(item.getCount()); // my crops
		}
	}
}
