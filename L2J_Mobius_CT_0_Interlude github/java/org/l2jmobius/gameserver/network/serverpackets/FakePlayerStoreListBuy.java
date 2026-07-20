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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerStoreItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * The BUY store window for a fake-player vendor. Lists <b>everything</b> the bot wants; items the
 * viewer does not own come through with a sellable amount of 0 so the client greys them out, exactly
 * like a real player's buy store. Mirrors {@link PrivateStoreListBuy}'s wire format.
 */
public class FakePlayerStoreListBuy extends ServerPacket
{
	private static class Row
	{
		final int itemObjectId; // the viewer's item (0 when they don't own it)
		final int itemId;
		final int enchant;
		final int sellable; // how many the viewer can actually sell (0 = greyed out)
		final int referencePrice;
		final int bodyMask;
		final int type2;
		final int price;
		final int wanted; // how many the bot wants

		Row(int itemObjectId, int itemId, int enchant, int sellable, int referencePrice, int bodyMask, int type2, int price, int wanted)
		{
			this.itemObjectId = itemObjectId;
			this.itemId = itemId;
			this.enchant = enchant;
			this.sellable = sellable;
			this.referencePrice = referencePrice;
			this.bodyMask = bodyMask;
			this.type2 = type2;
			this.price = price;
			this.wanted = wanted;
		}
	}

	private final int _objectId;
	private final int _playerAdena;
	private final List<Row> _rows = new ArrayList<>();

	public FakePlayerStoreListBuy(Player player, Npc npc)
	{
		_objectId = npc.getObjectId();
		_playerAdena = player.getAdena();
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		if (look == null)
		{
			return;
		}
		for (FakePlayerStoreItem demand : look.getStoreItems())
		{
			final ItemTemplate template = demand.getItem();
			if (template == null)
			{
				continue;
			}
			// Match against a tradeable, non-equipped copy the viewer owns; absent that, list it greyed.
			final Item owned = player.getInventory().getItemByItemId(demand.getItemId());
			final boolean canSell = (owned != null) && owned.isTradeable() && !owned.isEquipped();
			final int objectId = canSell ? owned.getObjectId() : 0;
			final int enchant = canSell ? owned.getEnchantLevel() : 0;
			final int sellable = canSell ? Math.min(demand.getCount(), owned.getCount()) : 0;
			_rows.add(new Row(objectId, template.getId(), enchant, sellable, template.getReferencePrice(), template.getBodyPart().getMask(), template.getType2(), demand.getPrice(), demand.getCount()));
		}
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PRIVATE_STORE_BUY_LIST.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_playerAdena);
		buffer.writeInt(_rows.size());
		for (Row row : _rows)
		{
			buffer.writeInt(row.itemObjectId);
			buffer.writeInt(row.itemId);
			buffer.writeShort(row.enchant);
			buffer.writeInt(row.sellable); // max the viewer can sell (0 -> greyed out)
			buffer.writeInt(row.referencePrice);
			buffer.writeShort(0);
			buffer.writeInt(row.bodyMask);
			buffer.writeShort(row.type2);
			buffer.writeInt(row.price); // buyer's price
			buffer.writeInt(row.wanted); // max the bot will take
		}
	}
}
