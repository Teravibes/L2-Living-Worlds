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
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.model.actor.Creature;

/**
 * Movement task manager class.
 * @author Mobius
 */
public class MovementTaskManager
{
	protected static final Logger LOGGER = Logger.getLogger(MovementTaskManager.class.getName());
	
	private static final Set<Set<Creature>> POOLS_CREATURE = ConcurrentHashMap.newKeySet();
	private static final Set<Set<Creature>> POOLS_PLAYER = ConcurrentHashMap.newKeySet();
	private static final int POOL_SIZE_CREATURE = 1000;
	private static final int POOL_SIZE_PLAYER = 500;
	private static final int TASK_DELAY_CREATURE = 100;
	private static final int TASK_DELAY_PLAYER = 50;
	
	protected MovementTaskManager()
	{
	}
	
	private class Movement implements Runnable
	{
		private final Set<Creature> _creatures;
		
		public Movement(Set<Creature> creatures)
		{
			_creatures = creatures;
		}
		
		@Override
		public void run()
		{
			if (_creatures.isEmpty())
			{
				return;
			}
			
			Creature creature;
			final Iterator<Creature> iterator = _creatures.iterator();
			while (iterator.hasNext())
			{
				creature = iterator.next();
				try
				{
					if (creature.updatePosition())
					{
						iterator.remove();
						creature.getAI().notifyAction(Action.ARRIVED);
					}
				}
				catch (Exception e)
				{
					iterator.remove();
					LOGGER.warning("MovementTaskManager: Problem updating position of " + creature);
					LOGGER.warning(TraceUtil.getStackTrace(e));
				}
			}
		}
	}
	
	/**
	 * Add a Creature to moving objects of MovementTaskManager.
	 * @param creature The Creature to add to moving objects of MovementTaskManager.
	 */
	public synchronized void registerMovingObject(Creature creature)
	{
		if (creature.isPlayer())
		{
			for (Set<Creature> pool : POOLS_PLAYER)
			{
				if (pool.contains(creature))
				{
					return;
				}
			}
			
			for (Set<Creature> pool : POOLS_PLAYER)
			{
				if (pool.size() < POOL_SIZE_PLAYER)
				{
					pool.add(creature);
					return;
				}
			}
			
			final Set<Creature> pool = ConcurrentHashMap.newKeySet(POOL_SIZE_PLAYER);
			pool.add(creature);
			ThreadPool.schedulePriorityTaskAtFixedRate(new Movement(pool), TASK_DELAY_PLAYER, TASK_DELAY_PLAYER);
			POOLS_PLAYER.add(pool);
		}
		else
		{
			for (Set<Creature> pool : POOLS_CREATURE)
			{
				if (pool.contains(creature))
				{
					return;
				}
			}
			
			for (Set<Creature> pool : POOLS_CREATURE)
			{
				if (pool.size() < POOL_SIZE_CREATURE)
				{
					pool.add(creature);
					return;
				}
			}
			
			final Set<Creature> pool = ConcurrentHashMap.newKeySet(POOL_SIZE_CREATURE);
			pool.add(creature);
			ThreadPool.scheduleAtFixedRate(new Movement(pool), TASK_DELAY_CREATURE, TASK_DELAY_CREATURE);
			POOLS_CREATURE.add(pool);
		}
	}
	
	public static final MovementTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MovementTaskManager INSTANCE = new MovementTaskManager();
	}
}
