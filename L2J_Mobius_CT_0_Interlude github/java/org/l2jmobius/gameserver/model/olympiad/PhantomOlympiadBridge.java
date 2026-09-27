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
package org.l2jmobius.gameserver.model.olympiad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Project-owned bridge between the phantom Olympiad roster ({@code PhantomOlympiadManager}) and the stock Olympiad.
 * It lives in the stock package only to reach two package-private pieces without editing stock code: the matcher's
 * monitor and the waiting lists.
 * <p>
 * The stock waiting lists are a plain ArrayList and HashMap that the matcher ({@link OlympiadManager#run()}) reads
 * and changes while it holds its own monitor, and it releases that monitor whenever it waits between passes. Every
 * call here takes the same monitor, so a phantom sign-up never lands in the middle of a matching pass.
 */
public final class PhantomOlympiadBridge
{
	/** A copy of the waiting lists, by object id. */
	public static final class Pools
	{
		public final List<Integer> nonClassed = new ArrayList<>();
		public final Map<Integer, List<Integer>> classed = new HashMap<>();

		/** @return the class id whose classed pool holds {@code objectId}, or -1 if it is in none */
		public int classedPoolOf(int objectId)
		{
			for (Map.Entry<Integer, List<Integer>> entry : classed.entrySet())
			{
				if (entry.getValue().contains(objectId))
				{
					return entry.getKey();
				}
			}
			return -1;
		}
	}

	private PhantomOlympiadBridge()
	{
	}

	/** Signs a noble up the same way the Olympiad Manager NPC does, with every stock check. */
	public static boolean register(Player noble, boolean classed)
	{
		synchronized (OlympiadManager.getInstance())
		{
			return Olympiad.getInstance().registerNoble(noble, classed);
		}
	}

	/** Takes a waiting noble off its list (stock refuses once it is picked for a match). */
	public static boolean unregister(Player noble)
	{
		synchronized (OlympiadManager.getInstance())
		{
			return Olympiad.getInstance().isRegistered(noble) && Olympiad.getInstance().unRegisterNoble(noble);
		}
	}

	/** @return a copy of both waiting lists */
	public static Pools pools()
	{
		final Pools pools = new Pools();
		synchronized (OlympiadManager.getInstance())
		{
			final List<Player> nonClassed = Olympiad.getRegisteredNonClassBased();
			if (nonClassed != null)
			{
				for (Player player : nonClassed)
				{
					pools.nonClassed.add(player.getObjectId());
				}
			}
			final Map<Integer, List<Player>> classed = Olympiad.getRegisteredClassBased();
			if (classed != null)
			{
				for (Map.Entry<Integer, List<Player>> entry : classed.entrySet())
				{
					final List<Integer> ids = new ArrayList<>();
					for (Player player : entry.getValue())
					{
						ids.add(player.getObjectId());
					}
					pools.classed.put(entry.getKey(), ids);
				}
			}
		}
		return pools;
	}
}
