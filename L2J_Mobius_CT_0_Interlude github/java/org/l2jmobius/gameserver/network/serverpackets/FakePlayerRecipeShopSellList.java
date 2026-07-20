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
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerCraftItem;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * The manufacture (recipe) store window for a fake-player crafter. Mirrors {@link RecipeShopSellList}'s
 * wire format but reads the offered recipes from the NPC instead of a {@code Player}.
 */
public class FakePlayerRecipeShopSellList extends ServerPacket
{
	private final Npc _npc;
	private final int _buyerAdena;
	private final List<FakePlayerCraftItem> _recipes;

	public FakePlayerRecipeShopSellList(Player buyer, Npc npc)
	{
		_npc = npc;
		_buyerAdena = buyer.getAdena();
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		_recipes = look == null ? Collections.emptyList() : look.getCraftItems();
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.RECIPE_SHOP_SELL_LIST.writeId(this, buffer);
		buffer.writeInt(_npc.getObjectId());
		buffer.writeInt((int) _npc.getCurrentMp()); // crafter's MP
		buffer.writeInt(_npc.getMaxMp());
		buffer.writeInt(_buyerAdena);
		buffer.writeInt(_recipes.size());
		for (FakePlayerCraftItem recipe : _recipes)
		{
			buffer.writeInt(recipe.getRecipeListId());
			buffer.writeInt(0); // unknown
			buffer.writeInt(recipe.getFee());
		}
	}
}
