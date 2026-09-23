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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Serializes stateful private conversation turns per conversation key, before the brain request starts (FPC-059,
 * FPC-064). Every stateful private mode (OFFER, PARTY, BUDDY, BUDDYCHAT, WHISPER, FRIEND) routes its turns through
 * here, so ordering is one shared mechanism rather than several managers with different semantics: at most one brain
 * request is in flight per conversation, turns run in submit (player-message) order, and different conversations
 * proceed in parallel.
 *
 * <p>The key mirrors the brain's own mutable-state key exactly (FPC-073): {@code fpc_brain.py} keys its per-turn lock
 * and conversation history by {@code (X-Player, X-FPC)} - the human and the bot NAME - with no mode component, and
 * every {@code _PRIVATE_TURN_MODES} request shares that one lock/history. So the Java key here is {@code (owner, bot)}
 * by name too: different modes to the same bot must serialize on the SAME key, or an OFFER opener and a later WHISPER
 * reply (or a BUDDYCHAT opener and a BUDDY reply) could still be appended to the shared Python history out of order.
 *
 * <p>The queue is bounded and aged (FPC-063): each conversation holds at most {@link #MAX_DEPTH} pending turns (a
 * burst beyond that is dropped rather than accumulating without bound), and a turn that waited past {@link #MAX_AGE_MS}
 * from its submit time is dropped when it reaches the head instead of running - because a stale conversational reply is
 * worse than silence, and the {@link BrainExecutor} freshness clock alone does not cover the wait in THIS queue (it
 * only starts once a turn is handed to the executor).
 */
public final class BrainConversationExecutor
{
	private static final Logger LOGGER = Logger.getLogger(BrainConversationExecutor.class.getName());

	private static final int MAX_DEPTH = 4; // pending turns per conversation
	private static final long MAX_AGE_MS = 45_000; // from submit (player-message arrival) to reaching the head

	private static final AtomicLong DEPTH_DROPPED = new AtomicLong();
	private static final AtomicLong STALE_DROPPED = new AtomicLong();

	/**
	 * A conversation turn. When it reaches the head it is started with a completion callback that MUST run exactly
	 * once (on every path - the work ran, was dropped, timed out, or failed) so the next turn is released. Wrapping the
	 * brain call as {@code BrainExecutor.runBrainWork(work, onComplete)} satisfies that contract.
	 */
	@FunctionalInterface
	public interface Turn
	{
		void start(Runnable onComplete);
	}

	private static final class Entry
	{
		final long enqueuedAt;
		final Turn turn;

		Entry(long enqueuedAt, Turn turn)
		{
			this.enqueuedAt = enqueuedAt;
			this.turn = turn;
		}
	}

	private static final Map<String, Deque<Entry>> QUEUES = new HashMap<>();

	private BrainConversationExecutor()
	{
	}

	/**
	 * Build a stable conversation key that mirrors the brain's {@code (X-Player, X-FPC)} lock/history key (FPC-073).
	 * There is deliberately no mode component: every private mode to the same bot from the same player is ONE
	 * conversation in {@code fpc_brain.py} and must serialize together here, so the bot must be identified by the same
	 * name Java sends as {@code X-FPC} (never an object id, which the brain never sees).
	 * @param owner the human side of the conversation (the {@code X-Player} name)
	 * @param bot the bot side of the conversation (the {@code X-FPC} name)
	 * @return a key unique to this (owner, bot) conversation, shared across all of its private modes
	 */
	public static String key(String owner, String bot)
	{
		return (owner == null ? "" : owner.toLowerCase()) + "|" + (bot == null ? "" : bot.toLowerCase());
	}

	/**
	 * Submit a turn for a conversation. It starts immediately if the conversation is idle, otherwise it queues behind
	 * the in-flight turn and runs in order.
	 * @param key the conversation key (see {@link #key})
	 * @param turn the turn to run
	 * @return {@code true} if the turn was accepted; {@code false} if the conversation queue was full and it was dropped
	 */
	public static boolean submit(String key, Turn turn)
	{
		final Entry entry = new Entry(System.currentTimeMillis(), turn);
		final boolean startNow;
		synchronized (QUEUES)
		{
			final Deque<Entry> queue = QUEUES.computeIfAbsent(key, k -> new ArrayDeque<>());
			if (queue.size() >= MAX_DEPTH)
			{
				final long dropped = DEPTH_DROPPED.incrementAndGet();
				if ((dropped == 1) || ((dropped % 25) == 0))
				{
					LOGGER.warning(BrainConversationExecutor.class.getSimpleName() + ": conversation queue full for a bot, dropped " //
						+ dropped + " over-depth turn(s) (a burst of messages to one conversation).");
				}
				return false;
			}
			queue.addLast(entry);
			startNow = (queue.size() == 1); // the head is the running turn; a lone entry means nothing is in flight
		}
		if (startNow)
		{
			runHead(key);
		}
		return true;
	}

	private static void runHead(String key)
	{
		Entry toStart = null;
		synchronized (QUEUES)
		{
			final Deque<Entry> queue = QUEUES.get(key);
			if (queue == null)
			{
				return;
			}
			final long now = System.currentTimeMillis();
			while (!queue.isEmpty())
			{
				final Entry head = queue.peekFirst();
				if ((now - head.enqueuedAt) <= MAX_AGE_MS)
				{
					toStart = head;
					break;
				}
				queue.pollFirst(); // FPC-063: this turn waited too long; drop it rather than run it as if fresh
				final long stale = STALE_DROPPED.incrementAndGet();
				if ((stale == 1) || ((stale % 25) == 0))
				{
					LOGGER.warning(BrainConversationExecutor.class.getSimpleName() + ": dropped " + stale //
						+ " stale conversation turn(s) (waited longer than " + (MAX_AGE_MS / 1000) + "s before running).");
				}
			}
			if (queue.isEmpty())
			{
				QUEUES.remove(key);
			}
		}
		if (toStart != null)
		{
			toStart.turn.start(() -> advance(key));
		}
	}

	private static void advance(String key)
	{
		synchronized (QUEUES)
		{
			final Deque<Entry> queue = QUEUES.get(key);
			if (queue == null)
			{
				return;
			}
			queue.pollFirst(); // remove the finished head
			if (queue.isEmpty())
			{
				QUEUES.remove(key); // idle conversation: don't retain an empty queue
				return;
			}
		}
		runHead(key);
	}

	/** @return the number of turns dropped because a conversation queue was full. */
	public static long depthDroppedCount()
	{
		return DEPTH_DROPPED.get();
	}

	/** @return the number of turns dropped because they waited past the age cap before running. */
	public static long staleDroppedCount()
	{
		return STALE_DROPPED.get();
	}
}
