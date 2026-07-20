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
package org.l2jmobius.gameserver.model.script;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

public class QuestTimer
{
	protected final String _name;
	protected final Quest _quest;
	protected final Npc _npc;
	protected final Player _player;
	protected final boolean _isRepeating;
	protected ScheduledFuture<?> _scheduler;
	
	public QuestTimer(Quest quest, String name, long time, Npc npc, Player player, boolean repeating)
	{
		_quest = quest;
		_name = name;
		_npc = npc;
		_player = player;
		_isRepeating = repeating;
		
		if (repeating)
		{
			_scheduler = ThreadPool.scheduleAtFixedRate(new ScheduleTimerTask(), time, time); // Prepare auto end task
		}
		else
		{
			_scheduler = ThreadPool.schedule(new ScheduleTimerTask(), time); // Prepare auto end task
		}
		
		if (npc != null)
		{
			npc.addQuestTimer(this);
		}
		
		if (player != null)
		{
			player.addQuestTimer(this);
		}
	}
	
	public void cancel()
	{
		cancelTask();
		
		if (_npc != null)
		{
			_npc.removeQuestTimer(this);
		}
		
		if (_player != null)
		{
			_player.removeQuestTimer(this);
		}
	}
	
	public void cancelTask()
	{
		if ((_scheduler != null) && !_scheduler.isDone() && !_scheduler.isCancelled())
		{
			_scheduler.cancel(false);
			_scheduler = null;
		}
		
		_quest.removeQuestTimer(this);
	}
	
	/**
	 * public method to compare if this timer matches with the key attributes passed.
	 * @param quest : Quest instance to which the timer is attached
	 * @param name : Name of the timer
	 * @param npc : Npc instance attached to the desired timer (null if no npc attached)
	 * @param player : Player instance attached to the desired timer (null if no player attached)
	 * @return boolean
	 */
	public boolean equals(Quest quest, String name, Npc npc, Player player)
	{
		if ((quest == null) || (quest != _quest))
		{
			return false;
		}
		
		if ((name == null) || !name.equals(_name))
		{
			return false;
		}
		
		return (npc == _npc) && (player == _player);
	}
	
	public boolean isActive()
	{
		return (_scheduler != null) && !_scheduler.isCancelled() && !_scheduler.isDone();
	}
	
	public boolean isRepeating()
	{
		return _isRepeating;
	}
	
	public Quest getQuest()
	{
		return _quest;
	}
	
	public Npc getNpc()
	{
		return _npc;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	@Override
	public String toString()
	{
		return _name;
	}
	
	public class ScheduleTimerTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_scheduler == null)
			{
				return;
			}
			
			if (!_isRepeating)
			{
				cancel();
			}
			
			_quest.notifyEvent(_name, _npc, _player);
		}
	}
}
