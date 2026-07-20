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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.siege.manor.Seed;
import org.l2jmobius.gameserver.model.siege.manor.SeedProduction;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * @author l3x
 */
public class RequestSetSeed extends ClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private int _manorId;
	private List<SeedProduction> _items;
	
	@Override
	protected void readImpl()
	{
		_manorId = readInt();
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int itemId = readInt();
			final int sales = readInt();
			final int price = readInt();
			if ((itemId < 1) || (sales < 0) || (price < 0))
			{
				_items.clear();
				return;
			}
			
			if (sales > 0)
			{
				_items.add(new SeedProduction(itemId, sales, price, sales));
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items.isEmpty())
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (!manor.isModifiablePeriod())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check player privileges
		if ((player.getClan() == null) || (player.getClan().getCastleId() != _manorId) || !player.hasAccess(ClanAccess.CASTLE_MANOR) || !player.getLastFolkNPC().canInteract(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Filter seeds with start amount lower than 0 and incorrect price
		final List<SeedProduction> list = new ArrayList<>(_items.size());
		for (SeedProduction sp : _items)
		{
			final Seed s = manor.getSeed(sp.getId());
			if ((s != null) && (sp.getStartAmount() <= s.getSeedLimit()) && (sp.getPrice() >= s.getSeedMinPrice()) && (sp.getPrice() <= s.getSeedMaxPrice()))
			{
				list.add(sp);
			}
		}
		
		// Save new list
		manor.setNextSeedProduction(list, _manorId);
	}
}
