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
package org.l2jmobius.commons.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dedicated thread to periodically monitor for deadlocks and provide detailed reports when detected.<br>
 * Allows for custom action when a deadlock is found by executing a user-defined callback.
 * @author Mobius
 */
public class DeadlockWatcher extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(DeadlockWatcher.class.getName());
	
	private static final int MAX_STACK_DEPTH = 30; // Max stack trace elements to log.
	private static final int MAX_DEADLOCK_THREADS = 20; // Max threads to process in full report.
	
	private final Duration _checkInterval;
	private final Runnable _deadlockCallback;
	private final ThreadMXBean _threadMXBean;
	
	public DeadlockWatcher(Duration checkInterval, Runnable deadlockCallback)
	{
		super("DeadlockWatcher");
		_checkInterval = checkInterval;
		_deadlockCallback = deadlockCallback;
		_threadMXBean = ManagementFactory.getThreadMXBean();
	}
	
	@Override
	public void run()
	{
		LOGGER.info("DeadlockWatcher: Thread started.");
		
		while (!isInterrupted())
		{
			try
			{
				// Detect deadlocks and handle them if found.
				final long[] deadlockedThreadIds = _threadMXBean.findDeadlockedThreads();
				if (deadlockedThreadIds != null)
				{
					LOGGER.warning("DeadlockWatcher: Deadlock detected!");
					
					// Build detailed deadlock report.
					if (deadlockedThreadIds.length > MAX_DEADLOCK_THREADS)
					{
						generateMinimalDeadlockReport(deadlockedThreadIds);
					}
					else
					{
						generateDeadlockReport(deadlockedThreadIds);
					}
					
					// Invoke callback if set.
					if (_deadlockCallback != null)
					{
						try
						{
							_deadlockCallback.run();
						}
						catch (Exception e)
						{
							LOGGER.log(Level.SEVERE, "DeadlockWatcher: Exception in deadlock callback: ", e);
						}
					}
				}
				
				Thread.sleep(_checkInterval.toMillis());
			}
			catch (InterruptedException e)
			{
				LOGGER.info("DeadlockWatcher: Thread interrupted and will exit.");
				Thread.currentThread().interrupt();
				break;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "DeadlockWatcher: Exception during deadlock check: ", e);
			}
		}
		
		LOGGER.info("DeadlockWatcher: Thread terminated.");
	}
	
	private void generateDeadlockReport(long[] deadlockedThreadIds)
	{
		LOGGER.warning("========== DEADLOCK REPORT ==========");
		
		for (ThreadInfo info : _threadMXBean.getThreadInfo(deadlockedThreadIds, true, true))
		{
			if (info == null)
			{
				continue;
			}
			
			LOGGER.warning("Thread: " + info.getThreadName() + " (ID: " + info.getThreadId() + ")");
			LOGGER.warning("State: " + info.getThreadState());
			final String lockName = info.getLockName();
			if (lockName != null)
			{
				LOGGER.warning("Waiting for lock: " + lockName);
				LOGGER.warning("Lock owner: " + info.getLockOwnerName() + " (ID: " + info.getLockOwnerId() + ")");
			}
			
			LOGGER.warning("Stack trace:");
			final StackTraceElement[] stack = info.getStackTrace();
			for (int i = 0; i < Math.min(stack.length, MAX_STACK_DEPTH); i++)
			{
				LOGGER.warning("\tat " + stack[i]);
			}
			
			if (stack.length > MAX_STACK_DEPTH)
			{
				LOGGER.warning("\t... (stack trace truncated)");
			}
			
			final MonitorInfo[] lockedMonitors = info.getLockedMonitors();
			if (lockedMonitors.length > 0)
			{
				LOGGER.warning("Locked monitors:");
				for (MonitorInfo monitor : lockedMonitors)
				{
					LOGGER.warning("\t- " + monitor.getClassName() + " at line " + monitor.getLockedStackFrame().getLineNumber());
				}
			}
			
			final LockInfo[] lockedSynchronizers = info.getLockedSynchronizers();
			if (lockedSynchronizers.length > 0)
			{
				LOGGER.warning("Locked synchronizers:");
				for (LockInfo lock : lockedSynchronizers)
				{
					LOGGER.warning("\t- " + lock.getClassName());
				}
			}
		}
		
		LOGGER.warning("========== END DEADLOCK REPORT ==========");
	}
	
	private void generateMinimalDeadlockReport(long[] deadlockedThreadIds)
	{
		LOGGER.warning("========== MINIMAL DEADLOCK REPORT ==========");
		
		for (long id : deadlockedThreadIds)
		{
			final ThreadInfo info = _threadMXBean.getThreadInfo(id, 10);
			if (info != null)
			{
				LOGGER.warning("Thread: " + info.getThreadName() + " (State: " + info.getThreadState() + ")");
				final String lockName = info.getLockName();
				if (lockName != null)
				{
					LOGGER.warning("\tWaiting for: " + lockName);
				}
				
				final StackTraceElement[] stack = info.getStackTrace();
				final int frames = Math.min(5, stack.length);
				for (int i = 0; i < frames; i++)
				{
					LOGGER.warning("\tat " + stack[i]);
				}
			}
		}
		
		LOGGER.warning("========== END MINIMAL REPORT ==========");
	}
}
