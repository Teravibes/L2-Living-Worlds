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
package org.l2jmobius.gameserver.model.item.holders;

/**
 * A simple DTO for items; contains item ID and count.
 * @author UnAfraid
 */
public class ItemHolder
{
	private final int _id;
	private final int _count;
	
	public ItemHolder(int id, int count)
	{
		_id = id;
		_count = count;
	}
	
	/**
	 * @return the ID of the item contained in this object
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the count of items contained in this object
	 */
	public int getCount()
	{
		return _count;
	}
	
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + _id + ", count: " + _count;
	}
}
