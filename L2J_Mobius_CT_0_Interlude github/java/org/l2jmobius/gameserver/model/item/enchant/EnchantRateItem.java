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
package org.l2jmobius.gameserver.model.item.enchant;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.item.ItemTemplate;

/**
 * @author UnAfraid, Mobius
 */
public class EnchantRateItem
{
	private final String _name;
	private final Set<Integer> _items = new HashSet<>();
	private int _slot;
	private Boolean _isMagicWeapon = null;
	
	public EnchantRateItem(String name)
	{
		_name = name;
	}
	
	/**
	 * @return name of enchant group.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Adds item id verification.
	 * @param id
	 */
	public void addItemId(int id)
	{
		_items.add(id);
	}
	
	/**
	 * Adds body slot verification.
	 * @param slot
	 */
	public void addSlot(int slot)
	{
		_slot |= slot;
	}
	
	/**
	 * Adds magic weapon verification.
	 * @param magicWeapon
	 */
	public void setMagicWeapon(boolean magicWeapon)
	{
		_isMagicWeapon = magicWeapon ? Boolean.TRUE : Boolean.FALSE;
	}
	
	/**
	 * @param item
	 * @return {@code true} if item can be used with this rate group, {@code false} otherwise.
	 */
	public boolean validate(ItemTemplate item)
	{
		if (!_items.isEmpty() && !_items.contains(item.getId()))
		{
			return false;
		}
		else if ((_slot != 0) && ((item.getBodyPart().getMask() & _slot) == 0))
		{
			return false;
		}
		else if ((_isMagicWeapon != null) && (item.isMagicWeapon() != _isMagicWeapon.booleanValue()))
		{
			return false;
		}
		
		return true;
	}
}
