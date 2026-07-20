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
package org.l2jmobius.gameserver.model.events;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;

/**
 * @author UnAfraid, Mobius
 */
public class ListenersContainer
{
	private Map<EventType, Queue<AbstractEventListener>> _listeners = null;
	
	/**
	 * Registers listener for a callback when specified event is executed.
	 * @param listener
	 * @return
	 */
	public AbstractEventListener addListener(AbstractEventListener listener)
	{
		if (listener == null)
		{
			throw new NullPointerException("Listener cannot be null!");
		}
		
		getListeners().computeIfAbsent(listener.getType(), _ -> new PriorityBlockingQueue<>()).add(listener);
		return listener;
	}
	
	/**
	 * Unregisters listener for a callback when specified event is executed.
	 * @param listener
	 * @return
	 */
	public AbstractEventListener removeListener(AbstractEventListener listener)
	{
		if (listener == null)
		{
			throw new NullPointerException("Listener cannot be null!");
		}
		
		if (_listeners == null)
		{
			throw new NullPointerException("Listeners container is not initialized!");
		}
		
		final EventType type = listener.getType();
		final Queue<AbstractEventListener> eventListenerQueue = _listeners.get(type);
		if (eventListenerQueue == null)
		{
			throw new IllegalAccessError("Listeners container doesn't had " + type + " event type added!");
		}
		
		eventListenerQueue.remove(listener);
		if (eventListenerQueue.isEmpty())
		{
			_listeners.remove(type);
		}
		
		return listener;
	}
	
	public void removeListenerIf(EventType type, Predicate<? super AbstractEventListener> filter)
	{
		if (_listeners == null)
		{
			return;
		}
		
		for (AbstractEventListener listener : getListeners(type))
		{
			if (filter.test(listener))
			{
				listener.unregisterMe();
			}
		}
	}
	
	public void removeListenerIf(Predicate<? super AbstractEventListener> filter)
	{
		if (_listeners == null)
		{
			return;
		}
		
		for (Queue<AbstractEventListener> queue : getListeners().values())
		{
			for (AbstractEventListener listener : queue)
			{
				if (filter.test(listener))
				{
					listener.unregisterMe();
				}
			}
		}
	}
	
	public boolean hasListener(EventType type)
	{
		if ((_listeners != null) && _listeners.containsKey(type))
		{
			return true;
		}
		
		if (this instanceof Creature)
		{
			final Creature creature = (Creature) this;
			if (creature.getTemplate().hasListener(type))
			{
				return true;
			}
			
			if (creature.isMonster())
			{
				return Containers.Monsters().hasListener(type);
			}
			else if (creature.isNpc())
			{
				return Containers.Npcs().hasListener(type);
			}
			else if (creature.isPlayer())
			{
				return Containers.Players().hasListener(type);
			}
		}
		
		return false;
	}
	
	/**
	 * @param type
	 * @return {@code List} of {@link AbstractEventListener} by the specified type
	 */
	public Collection<AbstractEventListener> getListeners(EventType type)
	{
		if (_listeners != null)
		{
			final Collection<AbstractEventListener> eventListenerQueue = _listeners.get(type);
			if (eventListenerQueue != null)
			{
				return eventListenerQueue;
			}
		}
		
		return Collections.emptyList();
	}
	
	/**
	 * Creates the listeners container map if doesn't exist.
	 * @return the listeners container map.
	 */
	private Map<EventType, Queue<AbstractEventListener>> getListeners()
	{
		if (_listeners == null)
		{
			synchronized (this)
			{
				if (_listeners == null)
				{
					_listeners = new ConcurrentHashMap<>();
				}
			}
		}
		
		return _listeners;
	}
}
