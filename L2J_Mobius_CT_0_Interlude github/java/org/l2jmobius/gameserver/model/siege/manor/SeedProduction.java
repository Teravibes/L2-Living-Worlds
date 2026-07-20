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
package org.l2jmobius.gameserver.model.siege.manor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xban1x
 */
public class SeedProduction
{
	private final int _seedId;
	private final int _price;
	private final int _startAmount;
	private final AtomicInteger _amount;
	
	public SeedProduction(int id, int amount, int price, int startAmount)
	{
		_seedId = id;
		_amount = new AtomicInteger(amount);
		_price = price;
		_startAmount = startAmount;
	}
	
	public int getId()
	{
		return _seedId;
	}
	
	public int getAmount()
	{
		return _amount.get();
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public int getStartAmount()
	{
		return _startAmount;
	}
	
	public void setAmount(int amount)
	{
		_amount.set(amount);
	}
	
	public boolean decreaseAmount(int value)
	{
		int current;
		int next;
		do
		{
			current = _amount.get();
			next = current - value;
			if (next < 0)
			{
				return false;
			}
		}
		while (!_amount.compareAndSet(current, next));
		return true;
	}
}
