/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @version $Revision: 1.7.2.4.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestDestroyItem extends ClientPacket
{
	private int _objectId;
	private int _count;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_count = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_count < 1)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DESTROY_IT_BECAUSE_THE_NUMBER_IS_INCORRECT);
			if (_count < 0)
			{
				PunishmentManager.handleIllegalPlayerAction(player, "[RequestDestroyItem] Character " + player.getName() + " of account " + player.getAccountName() + " tried to destroy item with oid " + _objectId + " but has count < 0!", GeneralConfig.DEFAULT_PUNISH);
			}
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are destroying items too fast.");
			return;
		}
		
		int count = _count;
		if (player.isProcessingTransaction() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}
		
		final Item itemToRemove = player.getInventory().getItemByObjectId(_objectId);
		
		// if we can't find the requested item, it is actually a cheat
		if (itemToRemove == null)
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		// Cannot discard item that the skill is consuming
		if (player.isCastingNow() && (player.getCurrentSkill() != null) && (player.getCurrentSkill().getSkill().getItemConsumeId() == itemToRemove.getId()))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		// Cannot discard item that the skill is consuming
		if (player.isCastingSimultaneouslyNow() && (player.getLastSimultaneousSkillCast() != null) && (player.getLastSimultaneousSkillCast().getItemConsumeId() == itemToRemove.getId()))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}
		
		final int itemId = itemToRemove.getId();
		if (!GeneralConfig.DESTROY_ALL_ITEMS && ((!player.isGM() && !itemToRemove.isDestroyable()) || CursedWeaponsManager.getInstance().isCursed(itemId)))
		{
			if (itemToRemove.isHeroItem())
			{
				player.sendPacket(SystemMessageId.HERO_WEAPONS_CANNOT_BE_DESTROYED);
			}
			else
			{
				player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_DISCARDED);
			}
			return;
		}
		
		if (!itemToRemove.isStackable() && (count > 1))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "[RequestDestroyItem] Character " + player.getName() + " of account " + player.getAccountName() + " tried to destroy a non-stackable item with oid " + _objectId + " but has count > 1!", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		if (!player.getInventory().canManipulateWithItemId(itemToRemove.getId()))
		{
			player.sendMessage("You cannot use this item.");
			return;
		}
		
		if (_count > itemToRemove.getCount())
		{
			count = itemToRemove.getCount();
		}
		
		if (itemToRemove.getTemplate().isPetItem())
		{
			// Check if the player has a summoned pet or mount active with the same object ID.
			final Summon summon = player.getSummon();
			if (((summon != null) && (summon.getControlObjectId() == _objectId)) || (player.isMounted() && (player.getMountObjectID() == _objectId)))
			{
				player.sendPacket(SystemMessageId.YOUR_PET_HAS_BEEN_SUMMONED_AND_MAY_NOT_BE_DESTROYED);
				return;
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?"))
			{
				statement.setInt(1, _objectId);
				statement.execute();
			}
			catch (Exception e)
			{
				PacketLogger.warning("Could not delete pet objectid: " + e.getMessage());
			}
		}
		
		// if (itemToRemove.isTimeLimitedItem())
		// {
		// itemToRemove.endOfLife();
		// }
		
		if (itemToRemove.isEquipped())
		{
			if (itemToRemove.isEnchanted())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.THE_EQUIPMENT_S1_S2_HAS_BEEN_REMOVED);
				sm.addInt(itemToRemove.getEnchantLevel());
				sm.addItemName(itemToRemove);
				player.sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_DISARMED);
				sm.addItemName(itemToRemove);
				player.sendPacket(sm);
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item itm : player.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getLocationSlot()))
			{
				iu.addModifiedItem(itm);
			}
			
			player.sendPacket(iu); // Sent inventory update for unequip instantly.
		}
		
		final Item removedItem = player.getInventory().destroyItem(ItemProcessType.DESTROY, itemToRemove, count, player, null);
		if (removedItem == null)
		{
			return;
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		if (removedItem.getCount() == 0)
		{
			iu.addRemovedItem(removedItem);
		}
		else
		{
			iu.addModifiedItem(removedItem);
		}
		
		player.sendPacket(iu); // Sent inventory update for destruction instantly.
		
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		
		final SystemMessage sm;
		if (count > 1)
		{
			sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
			sm.addItemName(removedItem);
			sm.addInt(count);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
			sm.addItemName(removedItem);
		}
		
		player.sendPacket(sm);
	}
}
