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
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author -Wooden-, KenM
 */
public class ExStorageMaxCount extends ServerPacket
{
	private final int _inventory;
	private final int _warehouse;
	private final int _clan;
	private final int _privateSell;
	private final int _privateBuy;
	private final int _receipeD;
	private final int _recipe;
	private final int _inventoryExtraSlots;
	
	public ExStorageMaxCount(Player player)
	{
		_inventory = player.getInventoryLimit();
		_warehouse = player.getWareHouseLimit();
		_privateSell = player.getPrivateSellStoreLimit();
		_privateBuy = player.getPrivateBuyStoreLimit();
		_clan = PlayerConfig.WAREHOUSE_SLOTS_CLAN;
		_receipeD = player.getDwarfRecipeLimit();
		_recipe = player.getCommonRecipeLimit();
		_inventoryExtraSlots = (int) player.getStat().calcStat(Stat.INV_LIM, 0, null, null);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_STORAGE_MAX_COUNT.writeId(this, buffer);
		buffer.writeInt(_inventory);
		buffer.writeInt(_warehouse);
		buffer.writeInt(_clan);
		buffer.writeInt(_privateSell);
		buffer.writeInt(_privateBuy);
		buffer.writeInt(_receipeD);
		buffer.writeInt(_recipe);
		buffer.writeInt(_inventoryExtraSlots); // Belt inventory slots increase count
	}
}
