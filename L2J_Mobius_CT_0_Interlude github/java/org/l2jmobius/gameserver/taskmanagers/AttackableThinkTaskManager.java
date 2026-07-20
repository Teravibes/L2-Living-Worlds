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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.model.actor.Attackable;

/**
 * @author Mobius
 */
public class AttackableThinkTaskManager
{
	private static final Set<Set<Attackable>> POOLS = ConcurrentHashMap.newKeySet();
	private static final int POOL_SIZE = 1000;
	private static final int TASK_DELAY = 1000;
	
	protected AttackableThinkTaskManager()
	{
	}
	
	private class AttackableThink implements Runnable
	{
		private final Set<Attackable> _attackables;
		
		public AttackableThink(Set<Attackable> attackables)
		{
			_attackables = attackables;
		}
		
		@Override
		public void run()
		{
			if (_attackables.isEmpty())
			{
				return;
			}
			
			CreatureAI ai;
			Attackable attackable;
			final Iterator<Attackable> iterator = _attackables.iterator();
			while (iterator.hasNext())
			{
				attackable = iterator.next();
				if (attackable.hasAI())
				{
					ai = attackable.getAI();
					if (ai != null)
					{
						ai.onActionThink();
					}
					else
					{
						iterator.remove();
					}
				}
				else
				{
					iterator.remove();
				}
			}
		}
	}
	
	public synchronized void add(Attackable attackable)
	{
		for (Set<Attackable> pool : POOLS)
		{
			if (pool.contains(attackable))
			{
				return;
			}
		}
		
		for (Set<Attackable> pool : POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.add(attackable);
				return;
			}
		}
		
		final Set<Attackable> pool = ConcurrentHashMap.newKeySet(POOL_SIZE);
		pool.add(attackable);
		ThreadPool.schedulePriorityTaskAtFixedRate(new AttackableThink(pool), TASK_DELAY, TASK_DELAY);
		POOLS.add(pool);
	}
	
	public void remove(Attackable attackable)
	{
		for (Set<Attackable> pool : POOLS)
		{
			if (pool.remove(attackable))
			{
				return;
			}
		}
	}
	
	public static AttackableThinkTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttackableThinkTaskManager INSTANCE = new AttackableThinkTaskManager();
	}
}
