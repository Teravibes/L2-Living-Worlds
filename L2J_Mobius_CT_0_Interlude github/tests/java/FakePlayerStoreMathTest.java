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

import org.l2jmobius.gameserver.managers.FakePlayerStoreMath;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link FakePlayerStoreMath} - the pure money/stock
 * arithmetic behind the FakePlayer store transactions (FPC-008). These lock down the invariants of FPC-001
 * (overflow-safe totalling), FPC-002 and FPC-003 (no oversell of stock or demand): the transaction manager runs these
 * exact decisions inside its per-store lock, so testing them here fails the suite against the vulnerable behavior even
 * though the live Player/inventory types cannot be built in this dependency-free lane.
 *
 * <p>Run from the project root ("L2J_Mobius_CT_0_Interlude github"):
 * <pre>
 *   javac -d build/test-classes \
 *         "java/org/l2jmobius/gameserver/managers/FakePlayerStoreMath.java" \
 *         "tests/java/FakePlayerStoreMathTest.java"
 *   java -cp build/test-classes FakePlayerStoreMathTest
 * </pre>
 * Exit code is 0 when every check passes, 1 otherwise.
 */
public class FakePlayerStoreMathTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		testLineTotal();
		testTotalInRange();
		testFits();
		testGrantable();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	private static void testLineTotal()
	{
		eq(0L, FakePlayerStoreMath.lineTotal(0, 500), "0 count -> 0");
		eq(2500L, FakePlayerStoreMath.lineTotal(5, 500), "5 x 500 -> 2500");
		// FPC-001/004: a wide line stays exact in long instead of wrapping int (2,000,000 x 2,000,000 = 4e12).
		eq(4_000_000_000_000L, FakePlayerStoreMath.lineTotal(2_000_000, 2_000_000), "wide line does not wrap int");
		eq((long) Integer.MAX_VALUE * Integer.MAX_VALUE, FakePlayerStoreMath.lineTotal(Integer.MAX_VALUE, Integer.MAX_VALUE), "max x max exact in long");
	}

	private static void testTotalInRange()
	{
		truth(FakePlayerStoreMath.totalInRange(1), "1 is in range");
		truth(FakePlayerStoreMath.totalInRange(Integer.MAX_VALUE), "Integer.MAX_VALUE is in range");
		truth(!FakePlayerStoreMath.totalInRange(0), "0 is rejected");
		truth(!FakePlayerStoreMath.totalInRange(-1), "negative is rejected");
		truth(!FakePlayerStoreMath.totalInRange((long) Integer.MAX_VALUE + 1), "MAX+1 is rejected (would narrow negative)");
	}

	private static void testFits()
	{
		// Buy is all-or-nothing: the whole line must fit the available stock.
		truth(FakePlayerStoreMath.fits(5, 5), "exact stock fits");
		truth(FakePlayerStoreMath.fits(1, 10), "within stock fits");
		truth(!FakePlayerStoreMath.fits(6, 5), "over stock does not fit (no oversell)");
		truth(!FakePlayerStoreMath.fits(0, 5), "zero request does not fit");
		truth(!FakePlayerStoreMath.fits(1, 0), "empty stock never fits");
	}

	private static void testGrantable()
	{
		// Sell is partial: take as much as fits the remaining demand, never more.
		eq(5, FakePlayerStoreMath.grantable(5, 10, 0), "request under demand -> full");
		eq(5, FakePlayerStoreMath.grantable(10, 5, 0), "request over demand -> capped to demand (no overpay)");
		eq(3, FakePlayerStoreMath.grantable(5, 10, 7), "earlier rows already took 7 -> only 3 left");
		eq(0, FakePlayerStoreMath.grantable(5, 10, 10), "demand exhausted -> 0");
		eq(0, FakePlayerStoreMath.grantable(5, 10, 12), "over-reserved demand never goes negative -> 0");
		eq(0, FakePlayerStoreMath.grantable(0, 10, 0), "zero request -> 0");
	}

	private static void eq(long expected, long actual, String what)
	{
		checks++;
		if (expected != actual)
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + expected + "] but got [" + actual + "]");
		}
	}

	private static void truth(boolean condition, String what)
	{
		checks++;
		if (!condition)
		{
			failures++;
			System.out.println("FAIL: " + what);
		}
	}
}
