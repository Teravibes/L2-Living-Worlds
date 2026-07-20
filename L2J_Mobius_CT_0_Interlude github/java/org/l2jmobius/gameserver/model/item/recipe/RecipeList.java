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
package org.l2jmobius.gameserver.model.item.recipe;

import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.holders.RecipeStatHolder;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * This class describes a Recipe used by Dwarf to craft Item. All RecipeList are made of RecipeHolder (1 line of the recipe : Item-Quantity needed).
 */
public class RecipeList
{
	/** The table containing all RecipeHolder (1 line of the recipe : Item-Quantity needed) of the RecipeList */
	private RecipeHolder[] _recipes;
	
	/** The table containing all RecipeStatHolder for the statUse parameter of the RecipeList */
	private RecipeStatHolder[] _statUse;
	
	/** The table containing all RecipeStatHolder for the altStatChange parameter of the RecipeList */
	private RecipeStatHolder[] _altStatChange;
	
	/** The Identifier of the Instance */
	private final int _id;
	
	/** The crafting level needed to use this RecipeList */
	private final int _level;
	
	/** The Identifier of the RecipeList */
	private final int _recipeId;
	
	/** The name of the RecipeList */
	private final String _recipeName;
	
	/** The crafting success rate when using the RecipeList */
	private final int _successRate;
	
	/** The Identifier of the Item crafted with this RecipeList */
	private final int _itemId;
	
	/** The quantity of Item crafted when using this RecipeList */
	private final int _count;
	
	/** If this a common or a dwarven recipe */
	private final boolean _isDwarvenRecipe;
	
	/**
	 * Constructor of RecipeList (create a new Recipe).
	 * @param set
	 */
	public RecipeList(StatSet set)
	{
		_recipes = new RecipeHolder[0];
		_statUse = new RecipeStatHolder[0];
		_altStatChange = new RecipeStatHolder[0];
		_id = set.getInt("id");
		_level = set.getInt("craftLevel");
		_recipeId = set.getInt("recipeId");
		_recipeName = set.getString("recipeName");
		_successRate = set.getInt("successRate");
		_itemId = set.getInt("itemId");
		_count = set.getInt("count");
		_isDwarvenRecipe = set.getBoolean("isDwarvenRecipe");
	}
	
	/**
	 * Add a RecipeHolder to the RecipeList (add a line Item-Quantity needed to the Recipe).
	 * @param recipe
	 */
	public void addRecipe(RecipeHolder recipe)
	{
		final int len = _recipes.length;
		final RecipeHolder[] tmp = new RecipeHolder[len + 1];
		System.arraycopy(_recipes, 0, tmp, 0, len);
		tmp[len] = recipe;
		_recipes = tmp;
	}
	
	/**
	 * Add a RecipeStatHolder of the statUse parameter to the RecipeList.
	 * @param statUse
	 */
	public void addStatUse(RecipeStatHolder statUse)
	{
		final int len = _statUse.length;
		final RecipeStatHolder[] tmp = new RecipeStatHolder[len + 1];
		System.arraycopy(_statUse, 0, tmp, 0, len);
		tmp[len] = statUse;
		_statUse = tmp;
	}
	
	/**
	 * Add a RecipeStatHolder of the altStatChange parameter to the RecipeList.
	 * @param statChange
	 */
	public void addAltStatChange(RecipeStatHolder statChange)
	{
		final int len = _altStatChange.length;
		final RecipeStatHolder[] tmp = new RecipeStatHolder[len + 1];
		System.arraycopy(_altStatChange, 0, tmp, 0, len);
		tmp[len] = statChange;
		_altStatChange = tmp;
	}
	
	/**
	 * @return the Identifier of the Instance.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the crafting level needed to use this RecipeList.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * @return the Identifier of the RecipeList.
	 */
	public int getRecipeId()
	{
		return _recipeId;
	}
	
	/**
	 * @return the name of the RecipeList.
	 */
	public String getRecipeName()
	{
		return _recipeName;
	}
	
	/**
	 * @return the crafting success rate when using the RecipeList.
	 */
	public int getSuccessRate()
	{
		return _successRate;
	}
	
	/**
	 * @return the Identifier of the Item crafted with this RecipeList.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * @return the quantity of Item crafted when using this RecipeList.
	 */
	public int getCount()
	{
		return _count;
	}
	
	/**
	 * @return {@code true} if this a Dwarven recipe or {@code false} if it is a Common recipe
	 */
	public boolean isDwarvenRecipe()
	{
		return _isDwarvenRecipe;
	}
	
	/**
	 * @return the table containing all RecipeHolder (1 line of the recipe : Item-Quantity needed) of the RecipeList.
	 */
	public RecipeHolder[] getRecipes()
	{
		return _recipes;
	}
	
	/**
	 * @return the table containing all RecipeStatHolder of the statUse parameter of the RecipeList.
	 */
	public RecipeStatHolder[] getStatUse()
	{
		return _statUse;
	}
	
	/**
	 * @return the table containing all RecipeStatHolder of the AltStatChange parameter of the RecipeList.
	 */
	public RecipeStatHolder[] getAltStatChange()
	{
		return _altStatChange;
	}
}
