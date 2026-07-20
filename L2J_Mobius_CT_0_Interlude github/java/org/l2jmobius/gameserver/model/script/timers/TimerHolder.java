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
package org.l2jmobius.gameserver.model.script.timers;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author UnAfraid
 * @param <T>
 */
public class TimerHolder<T> implements Runnable
{
	private final T _event;
	private final StatSet _params;
	private final long _time;
	private final Npc _npc;
	private final Player _player;
	private final boolean _isRepeating;
	private final IEventTimerEvent<T> _eventScript;
	private final IEventTimerCancel<T> _cancelScript;
	private final TimerExecutor<T> _postExecutor;
	private final ScheduledFuture<?> _task;
	
	public TimerHolder(T event, StatSet params, long time, Npc npc, Player player, boolean isRepeating, IEventTimerEvent<T> eventScript, IEventTimerCancel<T> cancelScript, TimerExecutor<T> postExecutor)
	{
		Objects.requireNonNull(event, getClass().getSimpleName() + ": \"event\" cannot be null!");
		Objects.requireNonNull(eventScript, getClass().getSimpleName() + ": \"script\" cannot be null!");
		Objects.requireNonNull(postExecutor, getClass().getSimpleName() + ": \"postExecutor\" cannot be null!");
		_event = event;
		_params = params;
		_time = time;
		_npc = npc;
		_player = player;
		_isRepeating = isRepeating;
		_eventScript = eventScript;
		_cancelScript = cancelScript;
		_postExecutor = postExecutor;
		_task = isRepeating ? ThreadPool.scheduleAtFixedRate(this, _time, _time) : ThreadPool.schedule(this, _time);
		
		if (npc != null)
		{
			npc.addTimerHolder(this);
		}
		
		if (player != null)
		{
			player.addTimerHolder(this);
		}
	}
	
	/**
	 * @return the event/key of this timer
	 */
	public T getEvent()
	{
		return _event;
	}
	
	/**
	 * @return the parameters of this timer
	 */
	public StatSet getParams()
	{
		return _params;
	}
	
	/**
	 * @return the npc of this timer
	 */
	public Npc getNpc()
	{
		return _npc;
	}
	
	/**
	 * @return the player of this timer
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * @return {@code true} if the timer will repeat itself, {@code false} otherwise
	 */
	public boolean isRepeating()
	{
		return _isRepeating;
	}
	
	/**
	 * Cancels this timer.
	 */
	public void cancelTimer()
	{
		if (_npc != null)
		{
			_npc.removeTimerHolder(this);
		}
		
		if (_player != null)
		{
			_player.removeTimerHolder(this);
		}
		
		if ((_task == null) || _task.isCancelled() || _task.isDone())
		{
			return;
		}
		
		_task.cancel(false);
		_cancelScript.onTimerCancel(this);
	}
	
	/**
	 * Cancels task related to this quest timer.
	 */
	public void cancelTask()
	{
		if ((_task != null) && !_task.isDone() && !_task.isCancelled())
		{
			_task.cancel(false);
		}
	}
	
	/**
	 * @return the remaining time of the timer, or -1 in case it doesn't exist.
	 */
	public long getRemainingTime()
	{
		if ((_task == null) || _task.isCancelled() || _task.isDone())
		{
			return -1;
		}
		
		return _task.getDelay(TimeUnit.MILLISECONDS);
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code true} if event, npc, player are equals to the ones stored in this TimerHolder, {@code false} otherwise
	 */
	public boolean isEqual(T event, Npc npc, Player player)
	{
		return _event.equals(event) && (_npc == npc) && (_player == player);
	}
	
	/**
	 * @param timer the other timer to be compared with.
	 * @return {@code true} of both of timers' npc, event and player match, {@code false} otherwise.
	 */
	public boolean isEqual(TimerHolder<T> timer)
	{
		return _event.equals(timer._event) && (_npc == timer._npc) && (_player == timer._player);
	}
	
	@Override
	public void run()
	{
		// Notify the post executor to remove this timer from the map
		_postExecutor.onTimerPostExecute(this);
		
		// Notify the script that the event has been fired.
		_eventScript.onTimerEvent(this);
	}
	
	@Override
	public String toString()
	{
		return "event: " + _event + " params: " + _params + " time: " + _time + " npc: " + _npc + " player: " + _player + " repeating: " + _isRepeating + " script: " + _eventScript.getClass().getSimpleName() + " postExecutor: " + _postExecutor.getClass().getSimpleName();
	}
}
