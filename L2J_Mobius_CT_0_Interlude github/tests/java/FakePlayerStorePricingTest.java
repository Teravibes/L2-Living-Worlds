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

import java.util.Arrays;
import java.util.List;

import org.l2jmobius.gameserver.managers.FakePlayerStorePricing;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link FakePlayerStorePricing}.<br>
 * These lock down the money-safety and formatting logic of the fake-player store: the negotiated-price
 * sanity band (the guard against a trade-chat prompt injection selling rares for 1 adena or overpaying
 * billions), deal-quantity clamping, bulk-stack sizing bands, sign formatting, and item-phrase tokenising.
 *
 * <p>Run from the project root ("L2J_Mobius_CT_0_Interlude github"):
 * <pre>
 *   javac -d build/test-classes \
 *         "java/org/l2jmobius/gameserver/managers/FakePlayerStorePricing.java" \
 *         "tests/java/FakePlayerStorePricingTest.java"
 *   java -cp build/test-classes FakePlayerStorePricingTest
 * </pre>
 * Exit code is 0 when every check passes, 1 otherwise.
 */
public class FakePlayerStorePricingTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		testClampDealPrice();
		testNormalizedDealCount();
		testBulkAmountRange();
		testAmount();
		testShotAbbrev();
		testShotSign();
		testMatchTokens();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	private static void testClampDealPrice()
	{
		// Selling (ref 1000): band is [500, 3000]. Absurd lows/highs get pinned; a fair haggle passes through.
		eq(500, FakePlayerStorePricing.clampDealPrice(1, 1000, true), "sell: 1 adena pinned up to the 0.5x floor");
		eq(3000, FakePlayerStorePricing.clampDealPrice(9_999_999, 1000, true), "sell: absurd high pinned to the 3x ceiling");
		eq(1500, FakePlayerStorePricing.clampDealPrice(1500, 1000, true), "sell: fair haggle within band passes through");
		// Buying (ref 1000): band is [100, 1500] - tighter ceiling so the bot never overpays.
		eq(100, FakePlayerStorePricing.clampDealPrice(1, 1000, false), "buy: 1 adena pinned up to the 0.1x floor");
		eq(1500, FakePlayerStorePricing.clampDealPrice(9_999_999, 1000, false), "buy: overpay pinned to the 1.5x ceiling");
		// Reference price unknown: fall back to at-least-1, never below.
		eq(5, FakePlayerStorePricing.clampDealPrice(5, 0, true), "no reference price -> keep the agreed price");
		eq(1, FakePlayerStorePricing.clampDealPrice(0, 0, true), "no reference price, zero agreed -> at least 1");
	}

	private static void testNormalizedDealCount()
	{
		eq(1, FakePlayerStorePricing.normalizedDealCount(false, 5000, 42), "non-stackable is always a single piece");
		eq(5000, FakePlayerStorePricing.normalizedDealCount(true, 5000, 42), "requested count is used when stackable");
		eq(2_000_000, FakePlayerStorePricing.normalizedDealCount(true, 9_000_000, 42), "requested count clamped to 2,000,000");
		eq(42, FakePlayerStorePricing.normalizedDealCount(true, 0, 42), "no request -> auto-sized fallback");
		eq(1, FakePlayerStorePricing.normalizedDealCount(true, 0, 0), "fallback is at least 1");
	}

	private static void testBulkAmountRange()
	{
		range(2000, 15000, FakePlayerStorePricing.bulkAmountRange(10), "<=10 is the cheap/bulk band");
		range(300, 3000, FakePlayerStorePricing.bulkAmountRange(11), "just over 10 steps to the next band");
		range(300, 3000, FakePlayerStorePricing.bulkAmountRange(60), "<=60 band");
		range(30, 400, FakePlayerStorePricing.bulkAmountRange(600), "<=600 band");
		range(3, 40, FakePlayerStorePricing.bulkAmountRange(6000), "<=6000 band");
		range(1, 8, FakePlayerStorePricing.bulkAmountRange(6001), "expensive goods -> single digits");
	}

	private static void testAmount()
	{
		eq("15k", FakePlayerStorePricing.amount(15000), "15000 -> 15k");
		eq("1k", FakePlayerStorePricing.amount(1000), "1000 -> 1k");
		eq("1.5k", FakePlayerStorePricing.amount(1500), "1500 -> 1.5k");
		eq("2.5k", FakePlayerStorePricing.amount(2500), "2500 -> 2.5k");
		eq("800", FakePlayerStorePricing.amount(800), "under 1000 stays plain");
		eq("999", FakePlayerStorePricing.amount(999), "999 stays plain");
	}

	private static void testShotAbbrev()
	{
		// Soulshot -> SS, Spiritshot -> SPS, Blessed Spiritshot -> BSS; grade appended (No Grade -> " NG").
		eq("SSD", FakePlayerStorePricing.shotAbbrev("Soulshot: D-Grade"), "soulshot D -> SSD");
		eq("SSC", FakePlayerStorePricing.shotAbbrev("Soulshot: C-grade"), "soulshot C (lowercase form) -> SSC");
		eq("SSS", FakePlayerStorePricing.shotAbbrev("Soulshot: S Grade"), "soulshot S (space form) -> SSS");
		eq("SS NG", FakePlayerStorePricing.shotAbbrev("Soulshot: No Grade"), "soulshot no grade -> SS NG");
		eq("SPSA", FakePlayerStorePricing.shotAbbrev("Spiritshot: A-Grade"), "spiritshot A -> SPSA");
		eq("BSSD", FakePlayerStorePricing.shotAbbrev("Blessed Spiritshot: D-Grade"), "blessed spiritshot D -> BSSD");
		eq("BSS NG", FakePlayerStorePricing.shotAbbrev("Blessed Spiritshot: No Grade for Beginners"), "blessed spiritshot no grade -> BSS NG");
		eq(null, FakePlayerStorePricing.shotAbbrev("Iron Ore"), "non-shot -> null");
		eq(null, FakePlayerStorePricing.shotAbbrev(null), "null name -> null");
	}

	private static void testShotSign()
	{
		eq("SSD 100a", FakePlayerStorePricing.shotSign("Soulshot: D-Grade", 100, true), "selling shot shows per-unit adena");
		eq("SSD 1.5ka", FakePlayerStorePricing.shotSign("Soulshot: D-Grade", 1500, true), "large per-unit price uses k suffix");
		eq("SSD", FakePlayerStorePricing.shotSign("Soulshot: D-Grade", 100, false), "buy sign omits the price");
		eq(null, FakePlayerStorePricing.shotSign("Iron Ore", 100, true), "non-shot -> null sign");
	}

	private static void testMatchTokens()
	{
		// Filler ("grade") dropped, punctuation stripped, plural folded, single-letter grade kept.
		eq(Arrays.asList("soulshot", "d"), FakePlayerStorePricing.matchTokens("Soulshots: D-grade"), "shot phrase tokenises to [soulshot, d]");
		eq(Arrays.asList("iron", "ore"), FakePlayerStorePricing.matchTokens("iron ore"), "plain two-word item");
		eq(Arrays.asList("arrow"), FakePlayerStorePricing.matchTokens("arrows"), "plural folded to singular");
		eq(Arrays.asList("bss"), FakePlayerStorePricing.matchTokens("cheap bss pls pm"), "stopwords cheap/pls/pm dropped");
		eq(0, FakePlayerStorePricing.matchTokens("   ").size(), "blank phrase -> no tokens");
		eq(0, FakePlayerStorePricing.matchTokens(null).size(), "null phrase -> no tokens");
	}

	// ===== tiny assertion helpers =====

	private static void range(int min, int max, int[] actual, String what)
	{
		eq(2, actual.length, what + " (two-element range)");
		eq(min, actual[0], what + " (min)");
		eq(max, actual[1], what + " (max)");
	}

	private static void eq(Object expected, Object actual, String what)
	{
		checks++;
		if ((expected == null) ? (actual != null) : !expected.equals(actual))
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + expected + "] but got [" + actual + "]");
		}
	}

	private static void eq(List<String> expected, List<String> actual, String what)
	{
		checks++;
		if (!expected.equals(actual))
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected " + expected + " but got " + actual);
		}
	}
}
