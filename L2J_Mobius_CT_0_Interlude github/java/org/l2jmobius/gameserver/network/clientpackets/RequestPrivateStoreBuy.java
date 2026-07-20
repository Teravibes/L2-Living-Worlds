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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.managers.FakePlayerStoreManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.holders.RequestTrade;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

public class RequestPrivateStoreBuy extends ClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private int _storePlayerId;
	private Set<RequestTrade> _items = null;
	
	@Override
	protected void readImpl()
	{
		_storePlayerId = readInt();
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new HashSet<>();
		for (int i = 0; i < count; i++)
		{
			final int objectId = readInt();
			int cnt = readInt();
			if (cnt > Integer.MAX_VALUE)
			{
				cnt = Integer.MAX_VALUE;
			}
			
			final int price = readInt();
			if ((objectId < 1) || (cnt < 1) || (price < 0))
			{
				_items = null;
				return;
			}
			
			_items.add(new RequestTrade(objectId, cnt, price));
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
		
		if (_items == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are buying items too fast.");
			return;
		}

		// Fake-player vendors are NPCs, not Players, so route their purchases to the dedicated manager.
		final WorldObject storeObject = World.getInstance().findObject(_storePlayerId);
		if ((storeObject != null) && storeObject.isNpc() && storeObject.asNpc().isFakePlayer())
		{
			FakePlayerStoreManager.buy(player, storeObject.asNpc(), _items);
			return;
		}

		final WorldObject object = World.getInstance().getPlayer(_storePlayerId);
		if (object == null)
		{
			return;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			return;
		}
		
		final Player storePlayer = object.asPlayer();
		if (!player.isInsideRadius3D(storePlayer, INTERACTION_DISTANCE))
		{
			return;
		}
		
		if ((player.getInstanceId() != storePlayer.getInstanceId()) && (player.getInstanceId() != -1))
		{
			return;
		}
		
		if (!((storePlayer.getPrivateStoreType() == PrivateStoreType.SELL) || (storePlayer.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL)))
		{
			return;
		}
		
		final TradeList storeList = storePlayer.getSellList();
		if (storeList == null)
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if ((storePlayer.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL) && (storeList.getItemCount() > _items.size()))
		{
			final String msgErr = "[RequestPrivateStoreBuy] " + player + " tried to buy less items than sold by package-sell, ban this player for bot usage!";
			PunishmentManager.handleIllegalPlayerAction(player, msgErr, GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		final int result = storeList.privateStoreBuy(player, _items);
		if (result > 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			if (result > 1)
			{
				PacketLogger.warning("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			}
			return;
		}
		
		// Update offline trade record, if realtime saving is enabled
		if (OfflineTradeConfig.OFFLINE_TRADE_ENABLE && OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME && ((storePlayer.getClient() == null) || storePlayer.getClient().isDetached()))
		{
			OfflineTraderTable.getInstance().onTransaction(storePlayer, storeList.getItemCount() == 0, false);
		}
		
		if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(PrivateStoreType.NONE);
			storePlayer.broadcastUserInfo();
		}
	}
}
