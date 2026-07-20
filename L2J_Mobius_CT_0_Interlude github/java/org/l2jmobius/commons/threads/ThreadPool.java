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
package org.l2jmobius.commons.threads;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;

/**
 * Provides methods to schedule tasks with delays, fixed rates and immediate execution.<br>
 * Manages multiple thread pools including scheduled tasks, instant execution and high priority scheduling.
 * <ul>
 * <li>Scheduled thread pool for delayed and recurring tasks.</li>
 * <li>Instant thread pool for immediate task execution.</li>
 * <li>High priority scheduled thread pool for critical tasks.</li>
 * <li>Automatic task purging and cleanup mechanisms.</li>
 * </ul>
 * @author Mobius
 */
public class ThreadPool
{
	private static final Logger LOGGER = Logger.getLogger(ThreadPool.class.getName());
	
	// Constants.
	private static final long ONE_HUNDRED_YEARS_MS = 3155695200000L;
	private static final long MIN_DELAY_MS = 0L;
	private static final long PURGE_INTERVAL_MS = 60000L;
	private static final int INSTANT_POOL_KEEP_ALIVE_MINUTES = 1;
	
	// Thread Pool Executors.
	private static ScheduledThreadPoolExecutor HIGH_PRIORITY_SCHEDULED_POOL;
	private static ScheduledThreadPoolExecutor SCHEDULED_POOL;
	private static ThreadPoolExecutor INSTANT_POOL;
	
	/**
	 * Initializes thread pool executors and starts maintenance tasks.
	 */
	public static void init()
	{
		LOGGER.info("ThreadPool: Initializing.");
		
		// Load configurations.
		ThreadConfig.load();
		
		// Configure High Priority ScheduledThreadPoolExecutor.
		if (ThreadConfig.HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE > 0)
		{
			HIGH_PRIORITY_SCHEDULED_POOL = new ScheduledThreadPoolExecutor(ThreadConfig.HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE, new ThreadProvider("L2jMobius High Priority ScheduledThread", ThreadPriority.PRIORITY_8), new ThreadPoolExecutor.CallerRunsPolicy());
			LOGGER.info(StringUtil.concat("...scheduled pool executor with ", String.valueOf(ThreadConfig.HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE), " high priority threads."));
		}
		
		// Configure ScheduledThreadPoolExecutor.
		SCHEDULED_POOL = new ScheduledThreadPoolExecutor(ThreadConfig.SCHEDULED_THREAD_POOL_SIZE, new ThreadProvider("L2jMobius ScheduledThread"), new ThreadPoolExecutor.CallerRunsPolicy());
		SCHEDULED_POOL.setRejectedExecutionHandler(new RejectedExecutionHandlerImpl());
		SCHEDULED_POOL.setRemoveOnCancelPolicy(true);
		SCHEDULED_POOL.prestartAllCoreThreads();
		
		// Configure ThreadPoolExecutor.
		INSTANT_POOL = new ThreadPoolExecutor(ThreadConfig.INSTANT_THREAD_POOL_SIZE, Integer.MAX_VALUE, INSTANT_POOL_KEEP_ALIVE_MINUTES, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadProvider("L2jMobius Thread"));
		INSTANT_POOL.setRejectedExecutionHandler(new RejectedExecutionHandlerImpl());
		INSTANT_POOL.prestartAllCoreThreads();
		
		// Schedule the purge task.
		scheduleAtFixedRate(ThreadPool::purge, PURGE_INTERVAL_MS, PURGE_INTERVAL_MS);
		
		// Log thread pool configuration.
		LOGGER.info(StringUtil.concat("...scheduled pool executor with ", String.valueOf(ThreadConfig.SCHEDULED_THREAD_POOL_SIZE), " total threads."));
		LOGGER.info(StringUtil.concat("...instant pool executor with ", String.valueOf(ThreadConfig.INSTANT_THREAD_POOL_SIZE), " total threads."));
	}
	
	/**
	 * Purges cancelled tasks from all thread pools to free memory.
	 */
	public static void purge()
	{
		SCHEDULED_POOL.purge();
		INSTANT_POOL.purge();
		if (HIGH_PRIORITY_SCHEDULED_POOL != null)
		{
			HIGH_PRIORITY_SCHEDULED_POOL.purge();
		}
	}
	
	/**
	 * Creates and executes a one-shot action that becomes enabled after the given delay.
	 * @param runnable the task to execute
	 * @param delay the time from now to delay execution
	 * @return a ScheduledFuture representing pending completion of the task and whose get() method will return null upon completion
	 */
	public static ScheduledFuture<?> schedule(Runnable runnable, long delay)
	{
		try
		{
			return SCHEDULED_POOL.schedule(new RunnableWrapper(runnable), validateDelay(delay), TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			LOGGER.warning(StringUtil.concat("ThreadPool: Failed to schedule task ", runnable.getClass().getSimpleName(), " with delay ", String.valueOf(delay), "ms: ", e.getMessage(), System.lineSeparator(), String.valueOf(e.getStackTrace())));
			return null;
		}
	}
	
	/**
	 * Creates and executes a periodic action that becomes enabled first after the given initial delay.
	 * @param runnable the task to execute
	 * @param initialDelay the time to delay first execution
	 * @param period the period between successive executions
	 * @return a ScheduledFuture representing pending completion of the task and whose get() method will throw an exception upon cancellation
	 */
	public static ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period)
	{
		try
		{
			return SCHEDULED_POOL.scheduleAtFixedRate(new RunnableWrapper(runnable), validateDelay(initialDelay), validateDelay(period), TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			LOGGER.warning(StringUtil.concat("ThreadPool: Failed to schedule recurring task ", runnable.getClass().getSimpleName(), " with initial delay ", String.valueOf(initialDelay), "ms and period ", String.valueOf(period), "ms: ", e.getMessage(), System.lineSeparator(), String.valueOf(e.getStackTrace())));
			return null;
		}
	}
	
	/**
	 * Creates and executes a periodic action using high priority thread pool.<br>
	 * Designed for tasks requiring immediate or high-priority execution.
	 * @param runnable the task to execute
	 * @param initialDelay the time to delay first execution
	 * @param period the period between successive executions
	 * @return a ScheduledFuture representing pending completion of the task and whose get() method will throw an exception upon cancellation
	 */
	public static ScheduledFuture<?> schedulePriorityTaskAtFixedRate(Runnable runnable, long initialDelay, long period)
	{
		if (HIGH_PRIORITY_SCHEDULED_POOL == null)
		{
			return scheduleAtFixedRate(runnable, initialDelay, period);
		}
		
		try
		{
			return HIGH_PRIORITY_SCHEDULED_POOL.scheduleAtFixedRate(new RunnableWrapper(runnable), validateDelay(initialDelay), validateDelay(period), TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			LOGGER.warning(StringUtil.concat("ThreadPool: Failed to schedule high priority task ", runnable.getClass().getSimpleName(), " with initial delay ", String.valueOf(initialDelay), "ms and period ", String.valueOf(period), "ms: ", e.getMessage(), System.lineSeparator(), String.valueOf(e.getStackTrace())));
			return null;
		}
	}
	
	/**
	 * Executes the given task sometime in the future using the instant thread pool.
	 * @param runnable the task to execute
	 */
	public static void execute(Runnable runnable)
	{
		try
		{
			INSTANT_POOL.execute(new RunnableWrapper(runnable));
		}
		catch (Exception e)
		{
			LOGGER.warning(StringUtil.concat("ThreadPool: Failed to execute task ", runnable.getClass().getSimpleName(), ": ", e.getMessage(), System.lineSeparator(), String.valueOf(e.getStackTrace())));
		}
	}
	
	/**
	 * Validates delay value to ensure it falls within acceptable bounds.
	 * @param delay the delay to validate
	 * @return a valid delay value between MIN_DELAY_MS and ONE_HUNDRED_YEARS_MS
	 */
	private static long validateDelay(long delay)
	{
		if (delay < MIN_DELAY_MS)
		{
			LOGGER.warning(StringUtil.concat("ThreadPool: Invalid delay ", String.valueOf(delay), "ms is below minimum, using ", String.valueOf(MIN_DELAY_MS), "ms instead."));
			LOGGER.warning(TraceUtil.getStackTrace(new Exception()));
			return MIN_DELAY_MS;
		}
		
		if (delay > ONE_HUNDRED_YEARS_MS)
		{
			LOGGER.warning(StringUtil.concat("ThreadPool: Invalid delay ", String.valueOf(delay), "ms exceeds maximum, using ", String.valueOf(ONE_HUNDRED_YEARS_MS), "ms instead."));
			LOGGER.warning(TraceUtil.getStackTrace(new Exception()));
			return ONE_HUNDRED_YEARS_MS;
		}
		
		return delay;
	}
	
	/**
	 * Shutdown thread pooling system correctly.
	 */
	public static void shutdown()
	{
		try
		{
			LOGGER.info("ThreadPool: Shutting down all thread pools.");
			SCHEDULED_POOL.shutdownNow();
			INSTANT_POOL.shutdownNow();
			if (HIGH_PRIORITY_SCHEDULED_POOL != null)
			{
				HIGH_PRIORITY_SCHEDULED_POOL.shutdownNow();
			}
		}
		catch (Throwable t)
		{
			LOGGER.warning(StringUtil.concat("ThreadPool: Exception occurred during shutdown: ", t.getMessage()));
		}
	}
	
	/**
	 * Handles tasks rejected by ThreadPoolExecutor by running them in new thread or current thread.<br>
	 * Decision based on current thread priority to prevent blocking high priority operations.
	 */
	private static class RejectedExecutionHandlerImpl implements RejectedExecutionHandler
	{
		private static final Logger LOGGER = Logger.getLogger(RejectedExecutionHandlerImpl.class.getName());
		
		@Override
		public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor)
		{
			if (executor.isShutdown())
			{
				return;
			}
			
			LOGGER.warning(StringUtil.concat("ThreadPool: Task ", runnable.getClass().getSimpleName(), " rejected by executor ", String.valueOf(executor), ", attempting recovery execution."));
			
			// Run in new thread for high priority contexts, current thread otherwise.
			if (Thread.currentThread().getPriority() > Thread.NORM_PRIORITY)
			{
				new Thread(runnable).start();
			}
			else
			{
				runnable.run();
			}
		}
	}
	
	/**
	 * Wraps a Runnable to handle uncaught exceptions during execution.<br>
	 * Passes exceptions to the thread's uncaught exception handler for proper error management.
	 */
	private static class RunnableWrapper implements Runnable
	{
		private final Runnable _wrappedRunnable;
		
		/**
		 * Creates a new RunnableWrapper for the specified runnable.
		 * @param runnable the runnable to wrap
		 */
		public RunnableWrapper(Runnable runnable)
		{
			_wrappedRunnable = runnable;
		}
		
		@Override
		public void run()
		{
			try
			{
				_wrappedRunnable.run();
			}
			catch (Throwable t)
			{
				final Thread currentThread = Thread.currentThread();
				final UncaughtExceptionHandler exceptionHandler = currentThread.getUncaughtExceptionHandler();
				if (exceptionHandler != null)
				{
					exceptionHandler.uncaughtException(currentThread, t);
				}
			}
		}
	}
}
