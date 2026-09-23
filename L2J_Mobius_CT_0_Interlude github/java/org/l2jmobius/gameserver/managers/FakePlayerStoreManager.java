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
package org.l2jmobius.gameserver.managers;

import static org.l2jmobius.gameserver.model.actor.Npc.INTERACTION_DISTANCE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerAppearance;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerCraftItem;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerStoreItem;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.recipe.RecipeList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.RequestTrade;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerRecipeShopItemInfo;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerRecipeShopSellList;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerStoreListBuy;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerStoreListSell;

/**
 * Turns the visual fake-player vendors into working shops. It sends the right store window when a
 * vendor is clicked and carries out real purchases/sales: adena moves, items transfer and the vendor's
 * stock is decremented. A vendor that sells out closes its store (the sign disappears); fresh stock is
 * generated for every vendor on server start, so the market repopulates each restart.
 * <p>
 * SELL/PACKAGE and BUY are fully transactional; CRAFT vendors are deployed as finished-goods SELL
 * stores, so they are purchasable too.
 */
public class FakePlayerStoreManager
{
	private FakePlayerStoreManager()
	{
	}

	/**
	 * Opens the appropriate store window for a clicked vendor bot.
	 * @return {@code true} if a store window was sent (caller should skip the normal chat/follow)
	 */
	public static boolean openStore(Player player, Npc npc)
	{
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		if ((look == null) || (look.getPrivateStoreType() == 0))
		{
			return false;
		}
		if (look.getPrivateStoreType() == PrivateStoreType.BUY.getId())
		{
			player.sendPacket(new FakePlayerStoreListBuy(player, npc));
		}
		else if (look.getPrivateStoreType() == PrivateStoreType.MANUFACTURE.getId())
		{
			player.sendPacket(new FakePlayerRecipeShopSellList(player, npc));
		}
		else
		{
			player.sendPacket(new FakePlayerStoreListSell(player, npc));
		}
		return true;
	}

	/**
	 * Sends the recipe preview the client asks for after a recipe is selected in a crafter's shop.
	 */
	public static void makeInfo(Player player, Npc npc, int recipeId)
	{
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		if ((look != null) && (look.getPrivateStoreType() == PrivateStoreType.MANUFACTURE.getId()))
		{
			player.sendPacket(new FakePlayerRecipeShopItemInfo(npc, recipeId));
		}
	}

	/**
	 * Player has a crafter bot make an item: the customer supplies the materials and pays the fee, the
	 * bot rolls the recipe's success rate and (on success) hands over the product.
	 */
	public static void craft(Player player, Npc npc, int recipeListId)
	{
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		if ((look == null) || (look.getPrivateStoreType() != PrivateStoreType.MANUFACTURE.getId()) || !commonChecks(player, npc))
		{
			return;
		}
		if (player.isCrafting())
		{
			player.sendMessage("You are currently in Craft Mode.");
			return;
		}

		// The recipe must be one this crafter actually offers.
		FakePlayerCraftItem offered = null;
		for (FakePlayerCraftItem entry : look.getCraftItems())
		{
			if (entry.getRecipeListId() == recipeListId)
			{
				offered = entry;
				break;
			}
		}
		final RecipeList recipe = offered == null ? null : RecipeData.getInstance().getRecipeList(recipeListId);
		if (recipe == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Fee + materials are checked up front so nothing is consumed on a doomed attempt.
		if (player.getAdena() < offered.getFee())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return;
		}
		for (RecipeHolder material : recipe.getRecipes())
		{
			final Item owned = player.getInventory().getItemByItemId(material.getItemId());
			if ((owned == null) || (owned.getCount() < material.getQuantity()))
			{
				player.sendMessage("You do not have the materials needed to craft this.");
				return;
			}
		}

		if (!player.reduceAdena(ItemProcessType.FEE, offered.getFee(), npc, true))
		{
			return;
		}
		for (RecipeHolder material : recipe.getRecipes())
		{
			player.destroyItemByItemId(ItemProcessType.CRAFT, material.getItemId(), material.getQuantity(), npc, true);
		}

		// Success uses the recipe's own rate; on failure the materials are still spent (as in normal craft).
		if (Rnd.get(100) < recipe.getSuccessRate())
		{
			player.addItem(ItemProcessType.CRAFT, recipe.getItemId(), recipe.getCount(), npc, true);
		}
		else
		{
			player.sendMessage("The attempt to craft the item has failed.");
		}
		player.sendPacket(new FakePlayerRecipeShopSellList(player, npc)); // refresh adena/MP in the window
	}

	/**
	 * Player buys from a SELL/PACKAGE vendor bot: validates the request, charges adena, hands over the
	 * items and decrements the bot's stock.
	 */
	public static void buy(Player player, Npc npc, Set<RequestTrade> items)
	{
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		if ((items == null) || items.isEmpty() || (look == null) || !commonChecks(player, npc))
		{
			return;
		}
		if ((look.getPrivateStoreType() != PrivateStoreType.SELL.getId()) && (look.getPrivateStoreType() != PrivateStoreType.PACKAGE_SELL.getId()))
		{
			return;
		}

		// FPC-003/FPC-072: run the WHOLE transaction under the per-store lock - validation, charge, grant, stock
		// decrement AND settle() together - so no other customer's transaction can interleave. The earlier
		// reserve-then-rollback released the lock during payment, which let a concurrent settle() close the store in
		// that gap and leave a rolled-back unit orphaned. The store lock is store-specific and taken only by this store
		// code, so holding it across the (in-memory) inventory ops creates no lock-ordering cycle with inventory locks.
		synchronized (look.storeLock())
		{
			long total = 0;
			final Map<FakePlayerStoreItem, Integer> plan = new LinkedHashMap<>();
			for (RequestTrade request : items)
			{
				final FakePlayerStoreItem entry = findByObjectId(look, request.getObjectId());
				if (entry == null)
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				final int want = plan.getOrDefault(entry, 0) + request.getCount();
				if ((request.getCount() < 1) || !FakePlayerStoreMath.fits(want, entry.getCount()) || (request.getPrice() != entry.getPrice()))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				total += FakePlayerStoreMath.lineTotal(request.getCount(), entry.getPrice());
				plan.put(entry, want);
			}

			// FPC-001: prove the total is inside 1..Integer.MAX_VALUE BEFORE narrowing it to int. A total above
			// Integer.MAX_VALUE would cast to a negative charge, and reduceAdena removes nothing for a nonpositive
			// count while still returning true, so the items would be granted for free.
			if (!FakePlayerStoreMath.totalInRange(total))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// FPC-014: a PACKAGE_SELL vendor is an all-or-nothing bundle, so enforce completeness server-side (the
			// wire package flag alone does not stop a crafted partial request). Every live stock line must appear
			// exactly once at its full remaining quantity; anything omitted or reduced is rejected with no state change.
			if (look.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL.getId())
			{
				final List<FakePlayerStoreItem> stock = look.getStoreItems();
				boolean complete = (plan.size() == stock.size());
				for (FakePlayerStoreItem entry : stock)
				{
					if (plan.getOrDefault(entry, 0) != entry.getCount())
					{
						complete = false;
						break;
					}
				}
				if (!complete)
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}

			if (total > player.getAdena())
			{
				player.sendMessage("You do not have enough adena.");
				player.sendPacket(new FakePlayerStoreListSell(player, npc));
				return;
			}
			if (!player.reduceAdena(ItemProcessType.BUY, (int) total, npc, true))
			{
				return;
			}

			// Payment succeeded: grant the items and decrement stock, still under the lock. No rollback is needed
			// because nothing was mutated until reduceAdena succeeded and no other transaction can interleave here.
			for (Map.Entry<FakePlayerStoreItem, Integer> bought : plan.entrySet())
			{
				final FakePlayerStoreItem entry = bought.getKey();
				player.addItem(ItemProcessType.BUY, entry.getItemId(), bought.getValue(), entry.getEnchant(), npc, true);
				entry.decrease(bought.getValue());
			}

			settle(npc, look);
		}

		FakePlayerBehaviorManager.getInstance().noteMeetInteraction(npc, player); // active trade -> don't time out
		player.sendPacket(new FakePlayerStoreListSell(player, npc));
	}

	/**
	 * Player sells to a BUY vendor bot: validates ownership, removes the items, pays the player and
	 * decrements the bot's remaining demand.
	 */
	public static void sell(Player player, Npc npc, RequestTrade[] items)
	{
		final FakePlayerAppearance look = npc.getFakePlayerAppearance();
		if ((items == null) || (items.length == 0) || (look == null) || !commonChecks(player, npc))
		{
			return;
		}
		if (look.getPrivateStoreType() != PrivateStoreType.BUY.getId())
		{
			return;
		}

		// One planned removal: the buyer's item instance, how many to take, and which demand line pays for it.
		record SellRow(int objectId, int want, FakePlayerStoreItem demand, int price) {}

		final Set<Integer> seenObjectIds = new HashSet<>();
		final List<SellRow> plan = new ArrayList<>();
		// FPC-003/FPC-072: run the WHOLE transaction under the per-store lock - validation, item removal, demand
		// decrement, payment AND settle() together - so no other seller's transaction can interleave. Two concurrent
		// sellers therefore serialize and cannot both validate against the same remaining demand, and no rollback is
		// needed: demand is decremented only for a row whose item actually destroyed. The store lock is store-specific
		// and taken only by this code, so holding it across the (in-memory) inventory ops is deadlock-safe.
		synchronized (look.storeLock())
		{
			final Map<FakePlayerStoreItem, Integer> taken = new LinkedHashMap<>();
			for (RequestTrade request : items)
			{
				// FPC-002: reject a repeated inventory object id. Two rows naming the same stack each see the full
				// owned count during validation, so without this a crafted packet could plan to sell one stack twice,
				// have the second removal fail, and still be paid for both.
				if (!seenObjectIds.add(request.getObjectId()))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				final Item owned = player.getInventory().getItemByObjectId(request.getObjectId());
				if ((owned == null) || (owned.getId() != request.getItemId()) || !owned.isTradeable() || owned.isEquipped())
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				final FakePlayerStoreItem demand = findByItemId(look, request.getItemId());
				if (demand == null)
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				final int alreadyTaken = taken.getOrDefault(demand, 0);
				final int want = FakePlayerStoreMath.grantable(Math.min(request.getCount(), owned.getCount()), demand.getCount(), alreadyTaken);
				if ((want < 1) || (request.getPrice() != demand.getPrice()))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				plan.add(new SellRow(owned.getObjectId(), want, demand, demand.getPrice()));
				taken.put(demand, alreadyTaken + want);
			}

			// FPC-070: prove the full payout fits 1..Integer.MAX_VALUE BEFORE removing any item, mirroring the FPC-001
			// BUY range check. Otherwise an extreme valid sale would destroy items worth more than Integer.MAX_VALUE and
			// then clamp the payment down, underpaying the player for goods already gone. Reject here, no state change.
			long plannedTotal = 0;
			for (SellRow row : plan)
			{
				plannedTotal += FakePlayerStoreMath.lineTotal(row.want(), row.price());
			}
			if (!FakePlayerStoreMath.totalInRange(plannedTotal))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// FPC-002: pay only for items actually removed. A row whose destroy succeeds decrements the bot's demand
			// and adds to the payout; a failed removal simply pays nothing and leaves demand untouched (no rollback).
			long paid = 0;
			for (SellRow row : plan)
			{
				if (player.destroyItem(ItemProcessType.SELL, row.objectId(), row.want(), npc, true))
				{
					row.demand().decrease(row.want());
					paid += FakePlayerStoreMath.lineTotal(row.want(), row.price());
				}
			}

			if (paid <= 0)
			{
				return;
			}
			// paid is bounded by the planned total, proven <= Integer.MAX_VALUE above (FPC-070), so the cast is safe.
			player.addAdena(ItemProcessType.SELL, (int) paid, npc, true);
			settle(npc, look);
		}

		FakePlayerBehaviorManager.getInstance().noteMeetInteraction(npc, player); // active trade -> don't time out
		player.sendPacket(new FakePlayerStoreListBuy(player, npc));
	}

	private static boolean commonChecks(Player player, Npc npc)
	{
		if (!player.isInsideRadius3D(npc, INTERACTION_DISTANCE))
		{
			return false;
		}
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		return true;
	}

	private static FakePlayerStoreItem findByObjectId(FakePlayerAppearance look, int objectId)
	{
		for (FakePlayerStoreItem entry : look.getStoreItems())
		{
			if (entry.getObjectId() == objectId)
			{
				return entry;
			}
		}
		return null;
	}

	private static FakePlayerStoreItem findByItemId(FakePlayerAppearance look, int itemId)
	{
		for (FakePlayerStoreItem entry : look.getStoreItems())
		{
			if ((entry.getItemId() == itemId) && (entry.getCount() > 0))
			{
				return entry;
			}
		}
		return null;
	}

	/**
	 * Drops sold-out lines; when the whole store empties, the vendor closes (sign removed). Vendors are
	 * not restocked at runtime on purpose — every server start generates fresh stock for them.
	 */
	private static void settle(Npc npc, FakePlayerAppearance look)
	{
		final List<FakePlayerStoreItem> remaining = new ArrayList<>();
		for (FakePlayerStoreItem entry : look.getStoreItems())
		{
			if (entry.getCount() > 0)
			{
				remaining.add(entry);
			}
		}
		if (!remaining.isEmpty())
		{
			look.setStoreItems(remaining);
			return;
		}
		// Sold out: close the store so it can't be reopened, and drop the sign for everyone nearby.
		look.setStoreItems(remaining);
		look.setStore(0, "");
		npc.updateAbnormalEffect();
	}
}
