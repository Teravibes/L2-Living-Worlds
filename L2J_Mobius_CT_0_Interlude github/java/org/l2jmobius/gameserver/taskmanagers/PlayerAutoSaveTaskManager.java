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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author Mobius
 */
public class PlayerAutoSaveTaskManager implements Runnable
{
	private static final Map<Player, Long> PLAYER_TIMES = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected PlayerAutoSaveTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public void run()
	{
		if (_working)
		{
			return;
		}
		
		_working = true;
		
		if (!PLAYER_TIMES.isEmpty())
		{
			final long currentTime = System.currentTimeMillis();
			final Iterator<Entry<Player, Long>> iterator = PLAYER_TIMES.entrySet().iterator();
			Entry<Player, Long> entry;
			Player player;
			Long time;
			
			while (iterator.hasNext())
			{
				entry = iterator.next();
				player = entry.getKey();
				time = entry.getValue();
				
				if (currentTime > time)
				{
					if ((player != null) && player.isOnline())
					{
						player.autoSave();
						PLAYER_TIMES.put(player, currentTime + GeneralConfig.CHAR_DATA_STORE_INTERVAL);
						break; // Prevent SQL flood.
					}
					
					iterator.remove();
				}
			}
		}
		
		_working = false;
	}
	
	public void add(Player player)
	{
		PLAYER_TIMES.put(player, System.currentTimeMillis() + GeneralConfig.CHAR_DATA_STORE_INTERVAL);
	}
	
	public void remove(Player player)
	{
		PLAYER_TIMES.remove(player);
	}
	
	public static PlayerAutoSaveTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerAutoSaveTaskManager INSTANCE = new PlayerAutoSaveTaskManager();
	}
}
