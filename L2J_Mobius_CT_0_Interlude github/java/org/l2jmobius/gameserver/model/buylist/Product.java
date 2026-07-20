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
package org.l2jmobius.gameserver.model.buylist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.taskmanagers.BuyListTaskManager;

/**
 * @author NosBit
 */
public class Product
{
	private static final Logger LOGGER = Logger.getLogger(Product.class.getName());
	
	private final int _buyListId;
	private final ItemTemplate _item;
	private final int _price;
	private final long _restockDelay;
	private final int _maxCount;
	private AtomicInteger _count = null;
	
	public Product(int buyListId, ItemTemplate item, int price, long restockDelay, int maxCount)
	{
		_buyListId = buyListId;
		_item = item;
		_price = price;
		_restockDelay = restockDelay * 60000;
		_maxCount = maxCount;
		if (hasLimitedStock())
		{
			_count = new AtomicInteger(maxCount);
		}
	}
	
	public int getBuyListId()
	{
		return _buyListId;
	}
	
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	public int getItemId()
	{
		return _item.getId();
	}
	
	public int getPrice()
	{
		return _price < 0 ? _item.getReferencePrice() : _price;
	}
	
	public long getRestockDelay()
	{
		return _restockDelay;
	}
	
	public long getMaxCount()
	{
		return _maxCount;
	}
	
	public int getCount()
	{
		if (_count == null)
		{
			return 0;
		}
		
		final int count = _count.get();
		return count > 0 ? count : 0;
	}
	
	public void setCount(int currentCount)
	{
		if (_count == null)
		{
			_count = new AtomicInteger();
		}
		
		_count.set(currentCount);
	}
	
	public boolean decreaseCount(int value)
	{
		if (_count == null)
		{
			return false;
		}
		
		BuyListTaskManager.getInstance().add(this, System.currentTimeMillis() + _restockDelay);
		
		final boolean result = _count.addAndGet(-value) >= 0;
		save();
		return result;
	}
	
	public boolean hasLimitedStock()
	{
		return _maxCount > -1;
	}
	
	public void restartRestockTask(long nextRestockTime)
	{
		final long remainTime = nextRestockTime - System.currentTimeMillis();
		if (remainTime > 0)
		{
			BuyListTaskManager.getInstance().update(this, remainTime);
		}
		else
		{
			restock();
		}
	}
	
	public void restock()
	{
		setCount(_maxCount);
		save();
	}
	
	private void save()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `buylists`(`buylist_id`, `item_id`, `count`, `next_restock_time`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `count` = ?, `next_restock_time` = ?"))
		{
			statement.setInt(1, _buyListId);
			statement.setInt(2, _item.getId());
			statement.setInt(3, getCount());
			statement.setInt(5, getCount());
			
			final long nextRestockTime = BuyListTaskManager.getInstance().getRestockDelay(this);
			if (nextRestockTime > 0)
			{
				statement.setLong(4, nextRestockTime);
				statement.setLong(6, nextRestockTime);
			}
			else
			{
				statement.setLong(4, 0);
				statement.setLong(6, 0);
			}
			
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to save Product buylist_id:" + _buyListId + " item_id:" + _item.getId(), e);
		}
	}
}
