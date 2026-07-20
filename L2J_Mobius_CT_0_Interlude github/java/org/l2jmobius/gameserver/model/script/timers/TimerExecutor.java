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
package org.l2jmobius.gameserver.model.script.timers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author UnAfraid, Mobius
 * @param <T>
 */
public class TimerExecutor<T>
{
	private final Map<T, Set<TimerHolder<T>>> _timers = new ConcurrentHashMap<>();
	private final IEventTimerEvent<T> _eventListener;
	private final IEventTimerCancel<T> _cancelListener;
	
	public TimerExecutor(IEventTimerEvent<T> eventListener, IEventTimerCancel<T> cancelListener)
	{
		_eventListener = eventListener;
		_cancelListener = cancelListener;
	}
	
	/**
	 * Adds timer
	 * @param holder
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	private boolean addTimer(TimerHolder<T> holder)
	{
		final Set<TimerHolder<T>> timers = _timers.computeIfAbsent(holder.getEvent(), _ -> ConcurrentHashMap.newKeySet());
		removeAndCancelTimers(timers, holder::isEqual);
		return timers.add(holder);
	}
	
	/**
	 * Adds non-repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, StatSet params, long time, Npc npc, Player player, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, false, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Adds non-repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param time
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, long time, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, null, time, null, null, false, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Adds non-repeating timer
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, StatSet params, long time, Npc npc, Player player)
	{
		return addTimer(event, params, time, npc, player, _eventListener);
	}
	
	/**
	 * Adds non-repeating timer
	 * @param event
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addTimer(T event, long time, Npc npc, Player player)
	{
		return addTimer(event, null, time, npc, player, _eventListener);
	}
	
	/**
	 * Adds repeating timer (Lambda is supported on the last parameter)
	 * @param event
	 * @param params
	 * @param time
	 * @param npc
	 * @param player
	 * @param eventTimer
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	private boolean addRepeatingTimer(T event, StatSet params, long time, Npc npc, Player player, IEventTimerEvent<T> eventTimer)
	{
		return addTimer(new TimerHolder<>(event, params, time, npc, player, true, eventTimer, _cancelListener, this));
	}
	
	/**
	 * Adds repeating timer
	 * @param event
	 * @param time
	 * @param npc
	 * @param player
	 * @return {@code true} if timer were successfully added, {@code false} in case it exists already
	 */
	public boolean addRepeatingTimer(T event, long time, Npc npc, Player player)
	{
		return addRepeatingTimer(event, null, time, npc, player, _eventListener);
	}
	
	/**
	 * That method is executed right after notification to {@link IEventTimerEvent#onTimerEvent(TimerHolder)} in order to remove the holder from the _timers map
	 * @param holder
	 */
	public void onTimerPostExecute(TimerHolder<T> holder)
	{
		// Remove non repeating timer upon execute
		if (!holder.isRepeating())
		{
			final Set<TimerHolder<T>> timers = _timers.get(holder.getEvent());
			if ((timers == null) || timers.isEmpty())
			{
				return;
			}
			
			// Remove the timer
			removeAndCancelTimers(timers, holder::isEqual);
			
			// If there's no events inside that set remove it
			if (timers.isEmpty())
			{
				_timers.remove(holder.getEvent());
			}
		}
	}
	
	/**
	 * Cancels and removes all timers from the _timers map
	 */
	public void cancelAllTimers()
	{
		for (Set<TimerHolder<T>> set : _timers.values())
		{
			for (TimerHolder<T> timer : set)
			{
				timer.cancelTimer();
			}
		}
		
		_timers.clear();
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code true} if there is a timer with the given event npc and player parameters, {@code false} otherwise
	 */
	public boolean hasTimer(T event, Npc npc, Player player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		for (TimerHolder<T> holder : timers)
		{
			if (holder.isEqual(event, npc, player))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * @param event
	 * @return {@code true} if at least one timer for the given event were stopped, {@code false} otherwise
	 */
	public boolean cancelTimers(T event)
	{
		final Set<TimerHolder<T>> timers = _timers.remove(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		timers.forEach(TimerHolder::cancelTimer);
		return true;
	}
	
	/**
	 * @param event
	 * @param npc
	 * @param player
	 * @return {@code true} if timer for the given event, npc, player were stopped, {@code false} otheriwse
	 */
	public boolean cancelTimer(T event, Npc npc, Player player)
	{
		final Set<TimerHolder<T>> timers = _timers.get(event);
		if ((timers == null) || timers.isEmpty())
		{
			return false;
		}
		
		removeAndCancelTimers(timers, timer -> timer.isEqual(event, npc, player));
		return false;
	}
	
	/**
	 * Cancel all timers of specified npc
	 * @param npc
	 */
	public void cancelTimersOf(Npc npc)
	{
		removeAndCancelTimers(timer -> timer.getNpc() == npc);
	}
	
	/**
	 * Removes and Cancels all timers matching the condition
	 * @param condition
	 */
	private void removeAndCancelTimers(Predicate<TimerHolder<T>> condition)
	{
		Objects.requireNonNull(condition);
		
		final Collection<Set<TimerHolder<T>>> allTimers = _timers.values();
		for (Set<TimerHolder<T>> timers : allTimers)
		{
			removeAndCancelTimers(timers, condition);
		}
	}
	
	private void removeAndCancelTimers(Set<TimerHolder<T>> timers, Predicate<TimerHolder<T>> condition)
	{
		Objects.requireNonNull(timers);
		Objects.requireNonNull(condition);
		
		final Iterator<TimerHolder<T>> it = timers.iterator();
		while (it.hasNext())
		{
			final TimerHolder<T> timer = it.next();
			if (condition.test(timer))
			{
				it.remove();
				timer.cancelTimer();
			}
		}
	}
}
