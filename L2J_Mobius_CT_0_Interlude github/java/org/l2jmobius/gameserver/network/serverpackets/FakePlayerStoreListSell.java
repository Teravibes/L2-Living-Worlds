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

import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerStoreItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * The SELL store window for a fake-player vendor. Mirrors {@link PrivateStoreListSell}'s wire format
 * but reads from the NPC's generated stock instead of a {@code Player}'s trade list.
 */
public class FakePlayerStoreListSell extends ServerPacket
{
	private final int _objectId;
	private final int _playerAdena;
	private final List<FakePlayerStoreItem> _items;

	public FakePlayerStoreListSell(Player player, Npc npc)
	{
		_objectId = npc.getObjectId();
		_playerAdena = player.getAdena();
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		_items = look == null ? Collections.emptyList() : look.getStoreItems();
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PRIVATE_STORE_LIST_SELL.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(0); // not a package sell
		buffer.writeInt(_playerAdena);
		buffer.writeInt(_items.size());
		for (FakePlayerStoreItem entry : _items)
		{
			final ItemTemplate item = entry.getItem();
			buffer.writeInt(item.getType2());
			buffer.writeInt(entry.getObjectId());
			buffer.writeInt(item.getId());
			buffer.writeInt(entry.getCount());
			buffer.writeShort(0);
			buffer.writeShort(entry.getEnchant());
			buffer.writeShort(0);
			buffer.writeInt(item.getBodyPart().getMask());
			buffer.writeInt(entry.getPrice()); // vendor price
			buffer.writeInt(item.getReferencePrice()); // base price
		}
	}
}
