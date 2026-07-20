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

import java.util.List;
import java.util.regex.Matcher;

import org.l2jmobius.gameserver.managers.FakePlayerChatParsing;
import org.l2jmobius.gameserver.managers.FakePlayerChatParsing.RoleRequest;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link FakePlayerChatParsing}.<br>
 * These lock down the pure trade/party/control-tag parsing that the fake-player chat system relies on -
 * quantity caps, price multipliers, meet-spot aliases, LFP levels and the tolerant control-tag patterns.
 *
 * <p>Run from the project root ("L2J_Mobius_CT_0_Interlude github"):
 * <pre>
 *   javac -d build/test-classes \
 *         "java/org/l2jmobius/gameserver/managers/FakePlayerChatParsing.java" \
 *         "tests/java/FakePlayerChatParsingTest.java"
 *   java -cp build/test-classes FakePlayerChatParsingTest
 * </pre>
 * Exit code is 0 when every check passes, 1 otherwise.
 */
public class FakePlayerChatParsingTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		testTradeQuantity();
		testShopPriceMultiplier();
		testLfpLevel();
		testLooksLikeLfp();
		testLooksLikeTradeAd();
		testNormalizeMeetSpot();
		testMeetTagPattern();
		testShopTagPattern();
		testCountBefore();
		testParseRoleRequests();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	private static void testTradeQuantity()
	{
		// Plain, k and kk/m suffixes on a stackable item.
		eq(5000, FakePlayerChatParsing.parseTradeQuantity("ssd 5k", true), "5k -> 5000");
		eq(2000000, FakePlayerChatParsing.parseTradeQuantity("adena 2m", true), "2m -> 2,000,000 (at cap)");
		eq(1000000, FakePlayerChatParsing.parseTradeQuantity("adena 1kk", true), "1kk -> 1,000,000");
		eq(42, FakePlayerChatParsing.parseTradeQuantity("42", true), "bare number -> 42");
		// Over the 2,000,000 cap is rejected, and the scanner keeps looking for a later sane quantity.
		eq(0, FakePlayerChatParsing.parseTradeQuantity("3m", true), "3m over cap -> 0");
		eq(500, FakePlayerChatParsing.parseTradeQuantity("9m or 500", true), "over-cap then valid -> 500");
		// Guards.
		eq(0, FakePlayerChatParsing.parseTradeQuantity("5k", false), "non-stackable -> 0");
		eq(0, FakePlayerChatParsing.parseTradeQuantity(null, true), "null phrase -> 0");
		eq(0, FakePlayerChatParsing.parseTradeQuantity("no digits here", true), "no number -> 0");
	}

	private static void testShopPriceMultiplier()
	{
		eq(500, FakePlayerChatParsing.applyShopPriceMultiplier(500, null), "no suffix keeps price");
		eq(5000, FakePlayerChatParsing.applyShopPriceMultiplier(5, "k"), "k -> *1000");
		eq(5000000, FakePlayerChatParsing.applyShopPriceMultiplier(5, "kk"), "kk -> *1,000,000");
		eq(5000, FakePlayerChatParsing.applyShopPriceMultiplier(5, "K"), "suffix is case-insensitive");
	}

	private static void testLfpLevel()
	{
		eq(57, FakePlayerChatParsing.parseLfpLevel("lfm buffer lvl 57"), "lvl 57");
		eq(40, FakePlayerChatParsing.parseLfpLevel("need healer level 40"), "level 40");
		eq(30, FakePlayerChatParsing.parseLfpLevel("lf pt lv 30"), "lv 30");
		eq(0, FakePlayerChatParsing.parseLfpLevel("lfm buffer"), "no level -> 0 (match recruiter)");
		eq(0, FakePlayerChatParsing.parseLfpLevel("level 99"), "out-of-range 99 -> 0");
	}

	private static void testLooksLikeLfp()
	{
		truth(FakePlayerChatParsing.looksLikeLfp("lfm 1 more dd"), "lfm is a party call");
		truth(FakePlayerChatParsing.looksLikeLfp("looking for a healer"), "looking for ...");
		truth(!FakePlayerChatParsing.looksLikeLfp("selling soulshots cheap"), "trade ad is not a party call");
	}

	private static void testLooksLikeTradeAd()
	{
		truth(FakePlayerChatParsing.looksLikeTradeAd("WTS soulshots"), "WTS is a trade ad");
		truth(FakePlayerChatParsing.looksLikeTradeAd("b> adena"), "b> is a trade ad");
		truth(!FakePlayerChatParsing.looksLikeTradeAd("hi there anyone around"), "chit-chat is not a trade ad");
	}

	private static void testNormalizeMeetSpot()
	{
		eq("gatekeeper", FakePlayerChatParsing.normalizeMeetSpot("gk"), "gk -> gatekeeper");
		eq("warehouse", FakePlayerChatParsing.normalizeMeetSpot(" WH "), "WH (trim/case) -> warehouse");
		eq("shop", FakePlayerChatParsing.normalizeMeetSpot("store"), "store -> shop");
		eq("cancel", FakePlayerChatParsing.normalizeMeetSpot("nvm"), "nvm -> cancel");
		eq("gatekeeper", FakePlayerChatParsing.normalizeMeetSpot("somewhere"), "unknown -> gatekeeper");
		eq("gatekeeper", FakePlayerChatParsing.normalizeMeetSpot(null), "null -> gatekeeper");
	}

	private static void testMeetTagPattern()
	{
		// The tolerant close must catch the well-formed AND the malformed variants Ollama emits.
		truth(FakePlayerChatParsing.MEET_TAG.matcher("ok [[MEET:gk]]").find(), "well-formed [[MEET:gk]]");
		truth(FakePlayerChatParsing.MEET_TAG.matcher("ok [[MEET:gk]").find(), "malformed single ] close");
		truth(FakePlayerChatParsing.MEET_TAG.matcher("ok [[MEET:gk))").find(), "malformed )) close");
		final Matcher m = FakePlayerChatParsing.MEET_TAG.matcher("sure [[MEET:warehouse]] see you");
		truth(m.find(), "capturing find");
		eq("warehouse", m.group(1), "captures the spot token");
	}

	private static void testShopTagPattern()
	{
		final Matcher m = FakePlayerChatParsing.SHOP_TAG.matcher("deal [[SHOP:SELL:soulshot:5k]]");
		truth(m.find(), "SHOP tag matches");
		eq("SELL", m.group(1).toUpperCase(), "side captured");
		eq("soulshot", m.group(2), "item captured");
		eq("5", m.group(3), "price digits captured");
		eq("k", m.group(4), "price suffix captured");
	}

	private static void testCountBefore()
	{
		// "2 dd" - the count sits just before the role word at index 2.
		eq(2, FakePlayerChatParsing.countBefore("2 dd", 2), "'2 dd' -> 2");
		eq(1, FakePlayerChatParsing.countBefore("dd", 0), "no leading number -> 1");
		eq(6, FakePlayerChatParsing.countBefore("10 tanks", 3), "10 clamps to the 6 cap");
		eq(1, FakePlayerChatParsing.countBefore("0 dd", 2), "0 clamps up to 1");
		eq(3, FakePlayerChatParsing.countBefore("3   dd", 4), "extra spaces between count and word -> 3");
	}

	private static void testParseRoleRequests()
	{
		// A count applies to the very next word only; a fresh word defaults to 1.
		List<RoleRequest> r = FakePlayerChatParsing.parseRoleRequests("2 dd healer");
		eq(2, r.size(), "'2 dd healer' -> two requests");
		req(r.get(0), "dd", 2, "first request is 2 x dd");
		req(r.get(1), "healer", 1, "second request is 1 x healer");

		// "lvl N" / "level N" / "lv N": the number is a level, not a count, and must not become a count.
		r = FakePlayerChatParsing.parseRoleRequests("lfm buffer lvl 57");
		req(last(r), "buffer", 1, "level number is not counted as a recruit count");

		// A count after a consumed level still applies to the following word.
		r = FakePlayerChatParsing.parseRoleRequests("lvl 40 2 dd");
		req(last(r), "dd", 2, "count after a level token still counts");

		// Numeric count is clamped to 6.
		req(FakePlayerChatParsing.parseRoleRequests("10 dd").get(0), "dd", 6, "count clamps to 6");

		// Plurals are emitted verbatim (caller resolves the singular).
		req(FakePlayerChatParsing.parseRoleRequests("3 mages").get(0), "mages", 3, "plural token kept verbatim");

		// Empty / null inputs are safe.
		eq(0, FakePlayerChatParsing.parseRoleRequests("").size(), "empty text -> no requests");
		eq(0, FakePlayerChatParsing.parseRoleRequests(null).size(), "null text -> no requests");
	}

	// ===== tiny assertion helpers =====

	private static RoleRequest last(List<RoleRequest> list)
	{
		return list.get(list.size() - 1);
	}

	private static void req(RoleRequest actual, String token, int count, String what)
	{
		eq(token, actual.token, what + " (token)");
		eq(count, actual.count, what + " (count)");
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
