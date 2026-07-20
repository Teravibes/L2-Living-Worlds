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
package org.l2jmobius.commons.network.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory for creating and managing threads for MMO server tasks.<br>
 * Provides custom naming conventions and priority management for better thread identification and performance tuning.
 * <ul>
 * <li>Generates unique thread names with pool and thread sequence numbers.</li>
 * <li>Enforces priority constraints based on thread group limitations.</li>
 * <li>Handles integer overflow for thread sequence numbering.</li>
 * </ul>
 * @author BazookaRpm
 */
public class MMOThreadFactory implements ThreadFactory
{
	// Constants.
	private static final String DEFAULT_BASE_NAME = "Thread";
	private static final int INITIAL_SEQUENCE_VALUE = 1;
	private static final int STACK_SIZE = 0; // Use JVM default stack size.
	
	// Global pool sequence counter.
	private static final AtomicInteger POOL_SEQUENCE = new AtomicInteger(INITIAL_SEQUENCE_VALUE);
	
	// Thread naming and priority.
	private final AtomicInteger _threadSequence = new AtomicInteger(INITIAL_SEQUENCE_VALUE);
	private final String _threadPrefix;
	private final int _threadPriority;
	
	/**
	 * Creates a new MMO thread factory with specified base name and priority.
	 * @param baseName base name for thread naming
	 * @param priority thread priority level
	 */
	public MMOThreadFactory(String baseName, int priority)
	{
		final String safeBaseName = ((baseName == null) || baseName.isEmpty()) ? DEFAULT_BASE_NAME : baseName;
		_threadPrefix = safeBaseName + "-MMO-pool-" + POOL_SEQUENCE.getAndIncrement() + "-thread-";
		
		// Clamp priority to valid range.
		if (priority < Thread.MIN_PRIORITY)
		{
			_threadPriority = Thread.MIN_PRIORITY;
		}
		else if (priority > Thread.MAX_PRIORITY)
		{
			_threadPriority = Thread.MAX_PRIORITY;
		}
		else
		{
			_threadPriority = priority;
		}
	}
	
	/**
	 * Creates a new thread for the given task with configured naming and priority.
	 * @param task the runnable task
	 * @return newly created thread
	 */
	@Override
	public Thread newThread(Runnable task)
	{
		final int threadIndex = nextIndex();
		final Thread thread = new Thread(null, task, _threadPrefix + threadIndex, STACK_SIZE);
		
		// Apply priority respecting thread group constraints.
		final ThreadGroup threadGroup = thread.getThreadGroup();
		final int groupMaxPriority = (threadGroup != null) ? threadGroup.getMaxPriority() : Thread.MAX_PRIORITY;
		final int effectivePriority = (_threadPriority > groupMaxPriority) ? groupMaxPriority : _threadPriority;
		
		thread.setPriority(effectivePriority);
		thread.setDaemon(false);
		
		return thread;
	}
	
	/**
	 * Gets the next thread index with overflow protection.
	 * @return next available thread index
	 */
	private int nextIndex()
	{
		final int currentValue = _threadSequence.getAndIncrement();
		return (currentValue == Integer.MIN_VALUE) ? _threadSequence.updateAndGet(x -> (x <= 0) ? INITIAL_SEQUENCE_VALUE : x) : currentValue; // Handle integer overflow by resetting to 1.
	}
}
