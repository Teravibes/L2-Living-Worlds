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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerFreight;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

/**
 * @author -Wooden-
 * @author UnAfraid, mrTJO
 */
public class RequestPackageSend extends ClientPacket
{
	private static final int BATCH_LENGTH = 8; // length of the one item.
	
	private ItemHolder[] _items = null;
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			_items = null;
			return;
		}
		
		_items = new ItemHolder[count];
		for (int i = 0; i < count; i++)
		{
			final int objId = readInt();
			final int cnt = readInt();
			if ((objId < 1) || (cnt < 0))
			{
				_items = null;
				return;
			}
			
			_items[i] = new ItemHolder(objId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((_items == null) || (player == null) || !player.getAccountChars().containsKey(_objectId))
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are sending freight packages too fast.");
			return;
		}
		
		final Npc manager = player.getLastFolkNPC();
		if (((manager == null) || !player.isInsideRadius2D(manager, Npc.INTERACTION_DISTANCE)))
		{
			player.sendPacket(SystemMessageId.YOU_FAILED_AT_SENDING_THE_PACKAGE_BECAUSE_YOU_ARE_TOO_FAR_FROM_THE_WAREHOUSE);
			return;
		}
		
		// If there is an active enchanting process, cancel it.
		if (player.getActiveEnchantItemId() != Player.ID_NONE)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
			player.sendPacket(new EnchantResult(0));
			player.setActiveEnchantItemId(Player.ID_NONE);
			return;
		}
		
		// Check for active trade list.
		if (player.getActiveTradeList() != null)
		{
			return;
		}
		
		// Alt game - Karma punishment.
		if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (player.getKarma() > 0))
		{
			return;
		}
		
		final ItemContainer warehouse = new PlayerFreight(_objectId);
		if ((warehouse instanceof PlayerFreight) && !player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			warehouse.deleteMe();
			return;
		}
		
		// Freight price from config per item slot.
		final int fee = _items.length * PlayerConfig.ALT_FREIGHT_PRICE;
		long currentAdena = player.getAdena();
		int slots = 0;
		
		for (ItemHolder itemHolder : _items)
		{
			// Check validity of requested item.
			final Item item = player.checkItemManipulation(itemHolder.getId(), itemHolder.getCount(), "freight");
			if ((item == null) || !item.isTradeable() || item.isQuestItem())
			{
				PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
				warehouse.deleteMe();
				return;
			}
			
			// Calculate needed adena and slots.
			if (item.getId() == Inventory.ADENA_ID)
			{
				currentAdena -= itemHolder.getCount();
			}
			else if (!item.isStackable())
			{
				slots += itemHolder.getCount();
			}
			else if (warehouse.getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		// Item Max Limit Check.
		if (!warehouse.validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			warehouse.deleteMe();
			return;
		}
		
		// Check if enough adena and charge the fee.
		if ((currentAdena < fee) || !player.reduceAdena(ItemProcessType.FEE, fee, manager, false))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			warehouse.deleteMe();
			return;
		}
		
		// Proceed to the transfer.
		final InventoryUpdate playerIU = new InventoryUpdate();
		for (ItemHolder itemHolder : _items)
		{
			// Check validity of requested item.
			final Item oldItem = player.checkItemManipulation(itemHolder.getId(), itemHolder.getCount(), "deposit");
			if (oldItem == null)
			{
				PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
				warehouse.deleteMe();
				return;
			}
			
			final Item newItem = player.getInventory().transferItem(ItemProcessType.TRANSFER, itemHolder.getId(), itemHolder.getCount(), warehouse, player, null);
			if (newItem == null)
			{
				PacketLogger.warning("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
				continue;
			}
			
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			
			// Remove item objects from the world.
			World.getInstance().removeObject(oldItem);
			World.getInstance().removeObject(newItem);
		}
		
		warehouse.deleteMe();
		
		// Send updated item list to the player.
		player.sendPacket(playerIU);
		
		// Update current load status on player.
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}
}
