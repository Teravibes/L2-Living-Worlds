/*
 * Copyright (c) 2013 L2jMobius
 *
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.managers;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;

/**
 * Shared bounded executor for ALL blocking brain HTTP calls (FPC-020).
 *
 * A brain request does a synchronous {@code HttpClient.send()} that can block for up to the bridge timeout (~45s).
 * Running that on the shared game {@code ThreadPool} let a slow or unreachable brain occupy scheduler threads that
 * combat AI, buff ticks, respawns and other scheduled gameplay also need. Every FakePlayer/Phantom brain client
 * (FakePlayerChatManager, PhantomPartyManager, PhantomBuddyManager) now routes its blocking call through here
 * instead, so external AI latency can never starve gameplay.
 *
 * Threads are daemon (they never block JVM shutdown). A single small bounded queue covers every brain client at
 * once, so the concurrency bound is global, not per-manager. When the queue is full, work is discarded and the bot
 * simply stays silent for that line - the same graceful degradation as a brain that is offline or times out - so a
 * backlog can never grow without bound. The human-like "thinking" delay stays on the game {@code ThreadPool} (it is
 * cheap and non-blocking); only the blocking work is handed here.
 */
public final class BrainExecutor
{
	private static final Logger LOGGER = Logger.getLogger(BrainExecutor.class.getName());

	// Global across every brain client. A brain call can block for the full bridge timeout, so worker count bounds
	// how many external HTTP waits can be in flight at once; the queue absorbs brief bursts before shedding load.
	private static final int WORKERS = 4;
	private static final int QUEUE_CAPACITY = 16;
	// Queue-freshness cap (FPC-020 review, finding 2): a chat reply that has waited too long in the queue is worse
	// than silence - the player has moved on. When a queued task finally starts, if it has already waited longer than
	// this it is dropped instead of run. The clock starts when the work is enqueued to this executor (after the
	// human "thinking" delay), so the delay itself is not counted. A small queue plus this cap bounds staleness
	// regardless of how slow the provider is.
	private static final long MAX_QUEUE_AGE_MS = 20_000;

	// Observability (FPC-020 review, finding 3): the overload policy still drops the work (the bot stays silent for
	// that line), but the drop is COUNTED and logged rather than silently discarded, so a saturated brain shows up in
	// the game log instead of looking like random silence. A dropped Java request never reaches the Python
	// diagnostics, so these counters are the only place overload/staleness is visible.
	private static final AtomicLong SUBMITTED = new AtomicLong();
	private static final AtomicLong DROPPED = new AtomicLong();
	private static final AtomicLong STALE = new AtomicLong();

	private static final ThreadPoolExecutor EXECUTOR = create();

	private BrainExecutor()
	{
	}

	private static ThreadPoolExecutor create()
	{
		final AtomicInteger threadNumber = new AtomicInteger(1);
		final ThreadFactory factory = runnable ->
		{
			final Thread thread = new Thread(runnable, "Brain-" + threadNumber.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		};
		// Drop-and-count: keep the "bot stays silent under load" policy, but record every shed task and log it
		// occasionally (first drop, then every 25th) so a saturated brain is visible without spamming the log.
		final RejectedExecutionHandler dropAndCount = (runnable, exec) ->
		{
			final long dropped = DROPPED.incrementAndGet();
			if ((dropped == 1) || ((dropped % 25) == 0))
			{
				LOGGER.warning(BrainExecutor.class.getSimpleName() + ": brain executor saturated, dropped " + dropped //
					+ " brain task(s) so far (bots stay silent under load). queued=" + exec.getQueue().size() //
					+ " active=" + exec.getActiveCount());
			}
			// FPC-059: still run the completion callback so a per-conversation serial queue advances even when the
			// task itself is shed, otherwise that conversation would stall forever behind a dropped turn.
			if (runnable instanceof BrainTask task)
			{
				task.complete();
			}
		};
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(WORKERS, WORKERS, 60L, TimeUnit.SECONDS, //
			new ArrayBlockingQueue<>(QUEUE_CAPACITY), factory, dropAndCount);
		executor.allowCoreThreadTimeOut(true); // don't hold worker threads when chat is idle
		return executor;
	}

	/**
	 * Run brain-backed work (which does blocking HTTP) on the shared brain executor, never on the shared game
	 * scheduler. When the executor is saturated the work is dropped (the bot stays silent this line) and counted;
	 * see {@link #droppedCount()}.
	 * @param work the brain-backed work to run
	 */
	public static void runBrainWork(Runnable work)
	{
		submit(new BrainTask(work, null));
	}

	/**
	 * As {@link #runBrainWork(Runnable)}, but {@code onComplete} runs exactly once no matter what happens to the work
	 * (it ran, it went stale, the queue was saturated, or the executor was shutting down). FPC-059 uses this to
	 * advance a per-conversation serial queue: a turn that is silently shed must still release the next turn, or that
	 * conversation would stall forever.
	 * @param work the brain-backed work to run
	 * @param onComplete a callback run exactly once after the work runs or is dropped
	 */
	public static void runBrainWork(Runnable work, Runnable onComplete)
	{
		submit(new BrainTask(work, onComplete));
	}

	private static void submit(BrainTask task)
	{
		SUBMITTED.incrementAndGet();
		try
		{
			EXECUTOR.execute(task);
		}
		catch (RejectedExecutionException e)
		{
			// Shutting down (ordinary saturation is handled by the drop-and-count handler without throwing).
			DROPPED.incrementAndGet();
			task.complete();
		}
	}

	/**
	 * A brain task carrying its freshness deadline and a one-shot completion callback. If it has waited past
	 * {@link #MAX_QUEUE_AGE_MS} in the queue by the time a worker picks it up it is dropped instead of run (FPC-020
	 * review, finding 2): a stale conversational reply is worse than silence. The completion callback runs exactly
	 * once on every path - normal run, stale drop, saturation drop, or shutdown reject (FPC-059).
	 */
	private static final class BrainTask implements Runnable
	{
		private final Runnable _work;
		private final Runnable _onComplete;
		private final long _deadline;
		private final AtomicBoolean _completed = new AtomicBoolean();

		BrainTask(Runnable work, Runnable onComplete)
		{
			_work = work;
			_onComplete = onComplete;
			_deadline = System.currentTimeMillis() + MAX_QUEUE_AGE_MS;
		}

		@Override
		public void run()
		{
			try
			{
				if (System.currentTimeMillis() > _deadline)
				{
					final long stale = STALE.incrementAndGet();
					if ((stale == 1) || ((stale % 25) == 0))
					{
						LOGGER.warning(BrainExecutor.class.getSimpleName() + ": dropped " + stale //
							+ " stale brain task(s) (queued longer than " + (MAX_QUEUE_AGE_MS / 1000) //
							+ "s; a late reply is worse than silence).");
					}
					return;
				}
				_work.run();
			}
			finally
			{
				complete();
			}
		}

		void complete()
		{
			if ((_onComplete != null) && _completed.compareAndSet(false, true))
			{
				try
				{
					_onComplete.run();
				}
				catch (Exception e)
				{
					LOGGER.warning(BrainExecutor.class.getSimpleName() + ": brain task completion callback failed: " + e);
				}
			}
		}
	}

	/** @return the total number of brain tasks submitted since start. */
	public static long submittedCount()
	{
		return SUBMITTED.get();
	}

	/** @return the total number of brain tasks dropped due to saturation (or shutdown) since start. */
	public static long droppedCount()
	{
		return DROPPED.get();
	}

	/** @return the total number of brain tasks dropped because they went stale in the queue since start. */
	public static long staleCount()
	{
		return STALE.get();
	}

	/**
	 * Schedule brain-backed work to run after a human-like delay WITHOUT blocking the shared game scheduler: the
	 * delay stays on the game {@code ThreadPool} (cheap, non-blocking), and when it fires the blocking work is handed
	 * to the shared brain executor.
	 * @param work the brain-backed work to run
	 * @param delayMs the human-like "thinking" delay before it runs
	 */
	public static void scheduleBrainWork(Runnable work, long delayMs)
	{
		ThreadPool.schedule(() -> runBrainWork(work), delayMs);
	}
}
