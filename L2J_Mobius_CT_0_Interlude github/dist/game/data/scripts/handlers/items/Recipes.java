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
package handlers.items;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.recipe.RecipeList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Zoey76
 */
public class Recipes implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}
		
		if (!PlayerConfig.IS_CRAFTING_ENABLED)
		{
			playable.sendMessage("Crafting is disabled, you cannot register this recipe.");
			return false;
		}
		
		final Player player = playable.asPlayer();
		if (player.isCrafting())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_ALTER_YOUR_RECIPE_BOOK_WHILE_ENGAGED_IN_MANUFACTURING);
			return false;
		}
		
		final RecipeList rp = RecipeData.getInstance().getRecipeByItemId(item.getId());
		if (rp == null)
		{
			return false;
		}
		
		if (player.hasRecipeList(rp.getId()))
		{
			player.sendPacket(SystemMessageId.THAT_RECIPE_IS_ALREADY_REGISTERED);
			return false;
		}
		
		boolean canCraft = false;
		boolean recipeLevel = false;
		boolean recipeLimit = false;
		if (rp.isDwarvenRecipe())
		{
			canCraft = player.hasDwarvenCraft();
			recipeLevel = (rp.getLevel() > player.getDwarvenCraft());
			recipeLimit = (player.getDwarvenRecipeBook().size() >= player.getDwarfRecipeLimit());
		}
		else
		{
			canCraft = player.hasCommonCraft();
			recipeLevel = (rp.getLevel() > player.getCommonCraft());
			recipeLimit = (player.getCommonRecipeBook().size() >= player.getCommonRecipeLimit());
		}
		
		if (!canCraft)
		{
			player.sendPacket(SystemMessageId.THE_RECIPE_CANNOT_BE_REGISTERED_YOU_DO_NOT_HAVE_THE_ABILITY_TO_CREATE_ITEMS);
			return false;
		}
		
		if (recipeLevel)
		{
			player.sendPacket(SystemMessageId.YOUR_CREATE_ITEM_LEVEL_IS_TOO_LOW_TO_REGISTER_THIS_RECIPE);
			return false;
		}
		
		if (recipeLimit)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_BE_REGISTERED);
			sm.addInt(rp.isDwarvenRecipe() ? player.getDwarfRecipeLimit() : player.getCommonRecipeLimit());
			player.sendPacket(sm);
			return false;
		}
		
		if (rp.isDwarvenRecipe())
		{
			player.registerDwarvenRecipeList(rp, true);
		}
		else
		{
			player.registerCommonRecipeList(rp, true);
		}
		
		player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, null, false);
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED);
		sm.addItemName(item);
		player.sendPacket(sm);
		return true;
	}
}
