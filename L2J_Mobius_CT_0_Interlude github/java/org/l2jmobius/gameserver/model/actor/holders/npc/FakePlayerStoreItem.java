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
package org.l2jmobius.gameserver.model.actor.holders.npc;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.item.ItemTemplate;

/**
 * One line of a fake-player private store: an item, a remaining quantity and the price the bot
 * trades it at. For SELL stores this is something the bot offers (the {@code objectId} is a
 * synthetic, store-local id so the client can round-trip the purchase request). For BUY stores it
 * describes a wanted item (matched against the buyer's inventory by item id rather than by this id).
 */
public class FakePlayerStoreItem
{
	private final int _objectId;
	private final int _itemId;
	private final int _enchant;
	private int _count;
	private final int _price;

	public FakePlayerStoreItem(int objectId, int itemId, int enchant, int count, int price)
	{
		_objectId = objectId;
		_itemId = itemId;
		_enchant = enchant;
		_count = count;
		_price = price;
	}

	public int getObjectId()
	{
		return _objectId;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getEnchant()
	{
		return _enchant;
	}

	public int getCount()
	{
		return _count;
	}

	public int getPrice()
	{
		return _price;
	}

	/**
	 * Reduces the remaining quantity after a (partial) trade, clamped at zero.
	 * @param amount how many units changed hands
	 */
	public void decrease(int amount)
	{
		_count -= amount;
		if (_count < 0)
		{
			_count = 0;
		}
	}

	/**
	 * @return the item template (looks/grade/reference price) for this line, or {@code null} if unknown
	 */
	public ItemTemplate getItem()
	{
		return ItemData.getInstance().getTemplate(_itemId);
	}
}
