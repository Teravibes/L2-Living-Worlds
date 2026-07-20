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

import static org.l2jmobius.gameserver.model.actor.Npc.INTERACTION_DISTANCE;
import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.MAX_ADENA;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Merchant;
import org.l2jmobius.gameserver.model.buylist.BuyListHolder;
import org.l2jmobius.gameserver.model.buylist.Product;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;

public class RequestBuyItem extends ClientPacket
{
	private static final int BATCH_LENGTH = 8;
	private static final int CUSTOM_CB_SELL_LIST = 423;
	
	private int _listId;
	private List<ItemHolder> _items = null;
	
	@Override
	protected void readImpl()
	{
		_listId = readInt();
		final int size = readInt();
		if ((size <= 0) || (size > PlayerConfig.MAX_ITEM_IN_PACKET) || ((size * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			final int itemId = readInt();
			final int count = readInt();
			if ((count > Integer.MAX_VALUE) || (itemId < 1) || (count < 1))
			{
				_items = null;
				return;
			}
			
			if (count > 10000) // Count check.
			{
				_items = null;
				return;
			}
			
			_items.add(new ItemHolder(itemId, count));
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are buying too fast.");
			return;
		}
		
		if (_items == null)
		{
			player.sendMessage("You cannot buy more than 10.000 items.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Alt game - Karma punishment
		if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_SHOP && (player.getKarma() > 0))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final WorldObject target = player.getTarget();
		Creature merchant = null;
		if (!player.isGM() && (_listId != CUSTOM_CB_SELL_LIST))
		{
			if (!(target instanceof Merchant) || (!player.isInsideRadius3D(target, INTERACTION_DISTANCE)) || (player.getInstanceId() != target.getInstanceId()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			merchant = target.asCreature();
		}
		
		double castleTaxRate = 0;
		double baseTaxRate = 0;
		if ((merchant == null) && !player.isGM() && (_listId != CUSTOM_CB_SELL_LIST))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final BuyListHolder buyList = BuyListData.getInstance().getBuyList(_listId);
		if (buyList == null)
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId, GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		if (merchant != null)
		{
			if (!buyList.isNpcAllowed(merchant.getId()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (merchant instanceof Merchant)
			{
				castleTaxRate = ((Merchant) merchant).getMpc().getCastleTaxRate();
				baseTaxRate = ((Merchant) merchant).getMpc().getBaseTaxRate();
			}
			else
			{
				baseTaxRate = 0.5;
			}
		}
		
		int subTotal = 0;
		
		// Check for buylist validity and calculates summary values
		long slots = 0;
		long weight = 0;
		for (ItemHolder i : _items)
		{
			int price = -1;
			
			final Product product = buyList.getProductByItemId(i.getId());
			if (product == null)
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getId(), GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			if (!product.getItem().isStackable() && (i.getCount() > 1))
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase invalid quantity of items at the same time.", GeneralConfig.DEFAULT_PUNISH);
				player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
				return;
			}
			
			price = product.getPrice();
			if ((product.getItemId() >= 3960) && (product.getItemId() <= 4026))
			{
				price *= RatesConfig.RATE_SIEGE_GUARDS_PRICE;
			}
			
			if (price < 0)
			{
				PacketLogger.warning("ERROR, no price found .. wrong buylist ??");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if ((price == 0) && !player.isGM() && GeneralConfig.ONLY_GM_ITEMS_FREE)
			{
				player.sendMessage("Ohh Cheat does not work? You have a problem now!");
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried buy item for 0 adena.", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			// trying to buy more then available
			if (product.hasLimitedStock() && (i.getCount() > product.getCount()))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if ((MAX_ADENA / i.getCount()) < price)
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			// first calculate price per item with tax, then multiply by count
			price = (int) (price * (1 + castleTaxRate + baseTaxRate));
			subTotal += i.getCount() * price;
			if (subTotal > MAX_ADENA)
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + MAX_ADENA + " adena worth of goods.", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			weight += i.getCount() * product.getItem().getWeight();
			if (player.getInventory().getItemByItemId(product.getItemId()) == null)
			{
				slots++;
			}
		}
		
		if (!player.isGM() && ((weight > Integer.MAX_VALUE) || (weight < 0) || !player.getInventory().validateWeight((int) weight)))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!player.isGM() && ((slots > Integer.MAX_VALUE) || (slots < 0) || !player.getInventory().validateCapacity((int) slots)))
		{
			player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Charge buyer and add tax to castle treasury if not owned by npc clan
		if ((subTotal < 0) || !player.reduceAdena(ItemProcessType.BUY, subTotal, player.getLastFolkNPC(), false))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Proceed the purchase
		for (ItemHolder i : _items)
		{
			final Product product = buyList.getProductByItemId(i.getId());
			if (product == null)
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getId(), GeneralConfig.DEFAULT_PUNISH);
				continue;
			}
			
			if (product.hasLimitedStock())
			{
				if (product.decreaseCount(i.getCount()))
				{
					player.getInventory().addItem(ItemProcessType.BUY, i.getId(), i.getCount(), player, merchant);
				}
			}
			else
			{
				player.getInventory().addItem(ItemProcessType.BUY, i.getId(), i.getCount(), player, merchant);
			}
		}
		
		// add to castle treasury
		if (merchant instanceof Merchant)
		{
			((Merchant) merchant).getCastle().addToTreasury((long) (subTotal * castleTaxRate));
		}
		
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendItemList(true);
		player.sendPacket(SystemMessageId.THE_TRANSACTION_IS_COMPLETE);
	}
}
