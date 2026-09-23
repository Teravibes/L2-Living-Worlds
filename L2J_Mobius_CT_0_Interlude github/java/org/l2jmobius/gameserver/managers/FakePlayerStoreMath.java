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
package org.l2jmobius.gameserver.managers;

/**
 * Pure, dependency-free arithmetic for FakePlayer store transactions, extracted so the money/stock invariants can be
 * unit-tested without the live inventory/store types (FPC-008). {@link FakePlayerStoreManager} calls these from inside
 * its per-store lock; the rules they encode are the FPC-001 (overflow-safe totalling), FPC-002/FPC-003 (no oversell)
 * decisions.
 */
public final class FakePlayerStoreMath
{
	private FakePlayerStoreMath()
	{
	}

	/**
	 * A line subtotal computed in {@code long} so a large {@code count * price} cannot wrap {@code int} before the
	 * caller range-checks the running total.
	 * @param count units on this line (assumed already validated as positive by the caller)
	 * @param price unit price
	 * @return {@code (long) count * price}
	 */
	public static long lineTotal(int count, int price)
	{
		return (long) count * price;
	}

	/**
	 * FPC-001: a chargeable/payable total must fit {@code 1..Integer.MAX_VALUE}, otherwise narrowing it to {@code int}
	 * (as {@code reduceAdena}/{@code addAdena} require) could produce a nonpositive value that moves no adena while the
	 * goods still change hands.
	 * @param total the running total as a {@code long}
	 * @return {@code true} when the total can be safely charged/paid as an {@code int}
	 */
	public static boolean totalInRange(long total)
	{
		return (total >= 1) && (total <= Integer.MAX_VALUE);
	}

	/**
	 * FPC-003 (buy, all-or-nothing): whether the full requested amount fits in the currently available stock. A buy
	 * line is only valid if the whole line can be served, so the caller rejects the request when this is {@code false}.
	 * @param requested units the buyer wants on this line (cumulative across duplicate lines for the same entry)
	 * @param available the entry's current remaining stock
	 * @return {@code true} when {@code requested} is at least 1 and no more than {@code available}
	 */
	public static boolean fits(int requested, int available)
	{
		return (requested >= 1) && (requested <= available);
	}

	/**
	 * FPC-002/FPC-003 (sell, partial): how many units may actually be taken this line, given how many of the same
	 * demand line were already reserved by earlier lines in the same request. Never negative, never more than the
	 * remaining demand, never more than requested.
	 * @param requested units the seller offers on this line (already capped by what they own)
	 * @param available the demand line's current remaining count
	 * @param alreadyTaken units of this same demand line reserved by earlier lines in this request
	 * @return the grantable amount, in {@code 0..min(requested, available - alreadyTaken)}
	 */
	public static int grantable(int requested, int available, int alreadyTaken)
	{
		if (requested < 1)
		{
			return 0;
		}
		final int remaining = available - alreadyTaken;
		if (remaining <= 0)
		{
			return 0;
		}
		return Math.min(requested, remaining);
	}
}
