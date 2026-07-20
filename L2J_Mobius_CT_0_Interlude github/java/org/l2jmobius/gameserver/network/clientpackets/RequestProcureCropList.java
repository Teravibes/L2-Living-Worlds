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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.UniqueItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.manor.CropProcure;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author l3x
 */
public class RequestProcureCropList extends ClientPacket
{
	private static final int BATCH_LENGTH = 16; // length of the one item
	
	private List<CropHolder> _items = null;
	
	@Override
	protected void readImpl()
	{
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int objId = readInt();
			final int itemId = readInt();
			final int manorId = readInt();
			int cnt = readInt();
			if (cnt > Integer.MAX_VALUE)
			{
				cnt = Integer.MAX_VALUE;
			}
			
			if ((objId < 1) || (itemId < 1) || (manorId < 0) || (cnt < 0))
			{
				_items = null;
				return;
			}
			
			_items.add(new CropHolder(objId, itemId, cnt, manorId));
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (manor.isUnderMaintenance())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Npc manager = player.getLastFolkNPC();
		if (!(manager instanceof Merchant) || !manager.canInteract(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int castleId = manager.getCastle().getResidenceId();
		if (manager.getTemplate().getParameters().getInt("manor_id", -1) != castleId)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		int slots = 0;
		int weight = 0;
		for (CropHolder i : _items)
		{
			final Item item = player.getInventory().getItemByObjectId(i.getObjectId());
			if ((item == null) || (item.getCount() < i.getCount()) || (item.getId() != i.getId()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final CropProcure cp = i.getCropProcure();
			if ((cp == null) || (cp.getAmount() < i.getCount()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final ItemTemplate template = ItemData.getInstance().getTemplate(i.getRewardId());
			weight += (i.getCount() * template.getWeight());
			if (!template.isStackable())
			{
				slots += i.getCount();
			}
			else if (player.getInventory().getItemByItemId(i.getRewardId()) == null)
			{
				slots++;
			}
		}
		
		if (!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}
		else if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		// Used when Config.ALT_MANOR_SAVE_ALL_ACTIONS == true
		final int updateListSize = GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS ? _items.size() : 0;
		final List<CropProcure> updateList = new ArrayList<>(updateListSize);
		
		// Proceed the purchase
		for (CropHolder i : _items)
		{
			final int rewardPrice = ItemData.getInstance().getTemplate(i.getRewardId()).getReferencePrice();
			if (rewardPrice == 0)
			{
				continue;
			}
			
			final int rewardItemCount = i.getPrice() / rewardPrice;
			if (rewardItemCount < 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getId());
				sm.addInt(i.getCount());
				player.sendPacket(sm);
				continue;
			}
			
			// Fee for selling to other manors
			final int fee = (castleId == i.getManorId()) ? 0 : ((int) (i.getPrice() * 0.05));
			if ((fee != 0) && (player.getAdena() < fee))
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1);
				sm.addItemName(i.getId());
				sm.addInt(i.getCount());
				player.sendPacket(sm);
				
				sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				player.sendPacket(sm);
				continue;
			}
			
			final CropProcure cp = i.getCropProcure();
			if (!cp.decreaseAmount(i.getCount()) || ((fee > 0) && !player.reduceAdena(ItemProcessType.FEE, fee, manager, true)) || !player.destroyItem(ItemProcessType.FEE, i.getObjectId(), i.getCount(), manager, true))
			{
				continue;
			}
			
			player.addItem(ItemProcessType.REWARD, i.getRewardId(), rewardItemCount, manager, true);
			if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				updateList.add(cp);
			}
		}
		
		if (GeneralConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			manor.updateCurrentProcure(castleId, updateList);
		}
	}
	
	private class CropHolder extends UniqueItemHolder
	{
		private final int _manorId;
		private CropProcure _cp;
		private int _rewardId = 0;
		
		public CropHolder(int objectId, int id, int count, int manorId)
		{
			super(id, objectId, count);
			_manorId = manorId;
		}
		
		public int getManorId()
		{
			return _manorId;
		}
		
		public int getPrice()
		{
			return getCount() * _cp.getPrice();
		}
		
		public CropProcure getCropProcure()
		{
			if (_cp == null)
			{
				_cp = CastleManorManager.getInstance().getCropProcure(_manorId, getId(), false);
			}
			
			return _cp;
		}
		
		public int getRewardId()
		{
			if (_rewardId == 0)
			{
				_rewardId = CastleManorManager.getInstance().getSeedByCrop(_cp.getId()).getReward(_cp.getReward());
			}
			
			return _rewardId;
		}
	}
}
