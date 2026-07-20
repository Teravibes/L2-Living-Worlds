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
package org.l2jmobius.gameserver.model.skill;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;

/**
 * @author Mobius
 */
public class BuffFinishTask
{
	private final Map<BuffInfo, AtomicInteger> _buffInfos = new ConcurrentHashMap<>();
	private ScheduledFuture<?> _task = null;
	private boolean _stopped = false;
	
	private class BuffFinishRunnable implements Runnable
	{
		@Override
		public void run()
		{
			for (Entry<BuffInfo, AtomicInteger> entry : _buffInfos.entrySet())
			{
				final BuffInfo info = entry.getKey();
				final Creature effected = info.getEffected();
				if ((effected != null) && (entry.getValue().incrementAndGet() > info.getAbnormalTime()))
				{
					ThreadPool.execute(() -> effected.getEffectList().stopSkillEffects(SkillFinishType.NORMAL, info.getSkill().getId()));
				}
			}
		}
	}
	
	public synchronized void removeBuffInfo(BuffInfo info)
	{
		_buffInfos.remove(info);
		
		if (_buffInfos.isEmpty() && (_task != null))
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	public synchronized void addBuffInfo(BuffInfo info)
	{
		_buffInfos.put(info, new AtomicInteger());
		
		if ((_task == null) && !_stopped)
		{
			_task = ThreadPool.scheduleAtFixedRate(new BuffFinishRunnable(), 0, 1000);
		}
	}
	
	public synchronized void start()
	{
		_stopped = false;
		
		if (!_buffInfos.isEmpty() && (_task == null))
		{
			_task = ThreadPool.scheduleAtFixedRate(new BuffFinishRunnable(), 0, 1000);
		}
	}
	
	public synchronized void stop()
	{
		_stopped = true;
		
		if (_task != null)
		{
			_task.cancel(true);
		}
	}
}
