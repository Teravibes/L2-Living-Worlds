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
package org.l2jmobius.gameserver.model.variables;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author UnAfraid, Mobius
 */
public abstract class AbstractVariables extends StatSet
{
	private final AtomicBoolean _hasChanges = new AtomicBoolean(false);
	
	protected final Set<String> _added = new HashSet<>(4);
	protected final Set<String> _modified = new HashSet<>(4);
	protected final Set<String> _deleted = new HashSet<>(4);
	protected final Lock _saveLock = new ReentrantLock();
	
	protected AbstractVariables()
	{
		super(ConcurrentHashMap::new);
	}
	
	/**
	 * Overriding following methods to prevent from doing useless database operations if there is no changes since player's login.
	 */
	
	@Override
	public void set(String name, boolean value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, byte value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, short value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, int value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, long value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, float value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, double value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, String value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, Enum<?> value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	@Override
	public void set(String name, Object value)
	{
		trackChange(name);
		super.set(name, value);
	}
	
	/**
	 * Track variable changes for optimized database operations.
	 * @param name the name of the variable
	 */
	protected void trackChange(String name)
	{
		_saveLock.lock();
		
		try
		{
			_hasChanges.compareAndSet(false, true);
			
			if (hasVariable(name))
			{
				_modified.add(name);
				_deleted.remove(name);
			}
			else
			{
				_added.add(name);
			}
		}
		finally
		{
			_saveLock.unlock();
		}
	}
	
	/**
	 * Put's entry to the variables and marks as changed if required (<i>Useful when restoring to do not save them again</i>).
	 * @param name the name of the variable
	 * @param value the value to set
	 * @param markAsChanged
	 */
	public void set(String name, String value, boolean markAsChanged)
	{
		if (markAsChanged)
		{
			trackChange(name);
		}
		
		super.set(name, value);
	}
	
	/**
	 * Return true if there exists a record for the variable name.
	 * @param name the name of the variable
	 * @return if the variable exists
	 */
	public boolean hasVariable(String name)
	{
		return getSet().containsKey(name);
	}
	
	/**
	 * @return {@code true} if changes are made since last load/save.
	 */
	public boolean hasChanges()
	{
		return _hasChanges.get();
	}
	
	/**
	 * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
	 * @param expect the extpected value
	 * @param update the update value
	 * @return {@code true} if successful. {@code false} return indicates that the actual value was not equal to the expected value.
	 */
	public boolean compareAndSetChanges(boolean expect, boolean update)
	{
		return _hasChanges.compareAndSet(expect, update);
	}
	
	/**
	 * Removes the given variable.
	 * @param name the name of the variable
	 */
	@Override
	public void remove(String name)
	{
		_saveLock.lock();
		
		try
		{
			_hasChanges.compareAndSet(false, true);
			
			if (hasVariable(name))
			{
				_added.remove(name);
				_modified.remove(name);
				_deleted.add(name);
			}
			
			getSet().remove(name);
		}
		finally
		{
			_saveLock.unlock();
		}
	}
	
	/**
	 * Clears change tracking sets.
	 */
	protected void clearChangeTracking()
	{
		_added.clear();
		_modified.clear();
		_deleted.clear();
	}
}
