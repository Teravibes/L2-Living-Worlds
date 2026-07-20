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

import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * Class explanation:<br>
 * For item counting or checking purposes. When you don't want to modify inventory<br>
 * class contains itemId, quantity, ownerId, referencePrice, but not objectId<br>
 * is stored, this will be only "list" of items with it's owner
 */
public class RecipeItemInfo
{
	private final int _itemId;
	private int _quantity;
	private final int _referencePrice;
	private final String _itemName;
	
	/**
	 * @param item
	 * @param quantity of that item
	 */
	public RecipeItemInfo(Item item, int quantity)
	{
		super();
		_itemId = item.getId();
		_quantity = quantity;
		_itemName = item.getTemplate().getName();
		_referencePrice = item.getReferencePrice();
	}
	
	/**
	 * @return the quantity.
	 */
	public int getQuantity()
	{
		return _quantity;
	}
	
	/**
	 * @param quantity The quantity to set.
	 */
	public void setQuantity(int quantity)
	{
		_quantity = quantity;
	}
	
	public int getReferencePrice()
	{
		return _referencePrice;
	}
	
	/**
	 * @return the itemId.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * @return the itemName.
	 */
	public String getItemName()
	{
		return _itemName;
	}
}
