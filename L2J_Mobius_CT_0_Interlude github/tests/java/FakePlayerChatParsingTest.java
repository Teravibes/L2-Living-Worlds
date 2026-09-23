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
		testTradeUnitPrice();
		testSpokenQuantity();
		testShopPriceMultiplier();
		testParseCounterOffer();
		testAcceptsCounter();
		testBareCounterCandidate();
		testActionIntentPredicates();
		testDealIntentPredicates();
		testLfpLevel();
		testLooksLikeLfp();
		testLooksLikeTradeAd();
		testNormalizeMeetSpot();
		testMeetTagPattern();
		testShopTagPattern();
		testCountBefore();
		testParseRoleRequests();
		testFuzzyMatching();

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

	private static void testTradeUnitPrice()
	{
		// A number carrying a price cue is read as the unit price (with k/kk/m applied).
		eq(300, FakePlayerChatParsing.parseTradeUnitPrice("ssd 300 adena"), "300 adena -> 300");
		eq(300, FakePlayerChatParsing.parseTradeUnitPrice("ssd 300a"), "300a -> 300");
		eq(5000, FakePlayerChatParsing.parseTradeUnitPrice("ss 5k ea"), "5k ea -> 5000");
		eq(5000, FakePlayerChatParsing.parseTradeUnitPrice("ss 5k each"), "5k each -> 5000");
		eq(1200, FakePlayerChatParsing.parseTradeUnitPrice("ssd 1200 pc"), "1200 pc -> 1200");
		eq(250, FakePlayerChatParsing.parseTradeUnitPrice("ssd 250 per"), "250 per -> 250");
		eq(1000000, FakePlayerChatParsing.parseTradeUnitPrice("mats 1kk adena"), "1kk adena -> 1,000,000");
		eq(300, FakePlayerChatParsing.parseTradeUnitPrice("ssd @300"), "@300 -> 300");
		eq(5000, FakePlayerChatParsing.parseTradeUnitPrice("ssd @5k"), "@5k -> 5000");
		// A bare number is a quantity, not a price - never mistaken for one.
		eq(0, FakePlayerChatParsing.parseTradeUnitPrice("5k ssd"), "bare quantity -> no price");
		eq(0, FakePlayerChatParsing.parseTradeUnitPrice("1000 arrows"), "quantity + noun -> no price");
		// Quantity then a priced number: the priced one wins ("1000 ssd 5 adena each" -> 5).
		eq(5, FakePlayerChatParsing.parseTradeUnitPrice("1000 ssd 5 adena"), "quantity then priced -> 5");
		// Guards.
		eq(0, FakePlayerChatParsing.parseTradeUnitPrice(null), "null phrase -> 0");
		eq(0, FakePlayerChatParsing.parseTradeUnitPrice("just chatting"), "no number -> 0");
	}

	private static void testSpokenQuantity()
	{
		// Spelled-out or digit count + magnitude word.
		eq(2000, FakePlayerChatParsing.parseSpokenQuantity("a couple thousand"), "a couple thousand -> 2000");
		eq(3000, FakePlayerChatParsing.parseSpokenQuantity("few k"), "few k -> 3000");
		eq(300, FakePlayerChatParsing.parseSpokenQuantity("a few hundred"), "a few hundred -> 300");
		eq(2000, FakePlayerChatParsing.parseSpokenQuantity("2 thousand"), "2 thousand -> 2000");
		eq(1000, FakePlayerChatParsing.parseSpokenQuantity("a thousand"), "a thousand -> 1000");
		eq(1000000, FakePlayerChatParsing.parseSpokenQuantity("a million"), "a million -> 1,000,000");
		eq(2000000, FakePlayerChatParsing.parseSpokenQuantity("3 million"), "3 million capped to 2,000,000");
		// Plain digits (with suffix) fall through to the quantity parser.
		eq(5000, FakePlayerChatParsing.parseSpokenQuantity("gimme 5k"), "gimme 5k -> 5000");
		eq(500, FakePlayerChatParsing.parseSpokenQuantity("500 please"), "500 -> 500");
		// Vague bulk with no number -> use the default stack.
		eq(FakePlayerChatParsing.SPOKEN_QUANTITY_DEFAULT, FakePlayerChatParsing.parseSpokenQuantity("just give me a stack"), "a stack -> default");
		eq(FakePlayerChatParsing.SPOKEN_QUANTITY_DEFAULT, FakePlayerChatParsing.parseSpokenQuantity("some please"), "some -> default");
		eq(FakePlayerChatParsing.SPOKEN_QUANTITY_DEFAULT, FakePlayerChatParsing.parseSpokenQuantity("whatever you got"), "whatever -> default");
		// No amount at all.
		eq(0, FakePlayerChatParsing.parseSpokenQuantity("sounds good, gk"), "no amount -> 0");
		eq(0, FakePlayerChatParsing.parseSpokenQuantity(null), "null -> 0");
	}

	private static void testShopPriceMultiplier()
	{
		eq(500, FakePlayerChatParsing.applyShopPriceMultiplier(500, null), "no suffix keeps price");
		eq(5000, FakePlayerChatParsing.applyShopPriceMultiplier(5, "k"), "k -> *1000");
		eq(5000000, FakePlayerChatParsing.applyShopPriceMultiplier(5, "kk"), "kk -> *1,000,000");
		eq(5000, FakePlayerChatParsing.applyShopPriceMultiplier(5, "K"), "suffix is case-insensitive");
		// FPC-004: the multiplier is overflow-safe. A wide price that would wrap int is clamped to Integer.MAX_VALUE
		// rather than becoming negative, and a negative base can never produce a negative price.
		eq(Integer.MAX_VALUE, FakePlayerChatParsing.applyShopPriceMultiplier(Integer.MAX_VALUE, "kk"), "MAX * kk clamps, not wraps");
		eq(Integer.MAX_VALUE, FakePlayerChatParsing.applyShopPriceMultiplier(3000000, "kk"), "3,000,000 kk clamps to MAX");
		eq(Integer.MAX_VALUE, FakePlayerChatParsing.applyShopPriceMultiplier(2500000, "k"), "2,500,000 k clamps to MAX");
		eq(2000000000, FakePlayerChatParsing.applyShopPriceMultiplier(2000000, "k"), "in-range k still exact");
		eq(0, FakePlayerChatParsing.applyShopPriceMultiplier(-5, "k"), "negative base never yields a negative price");
	}

	private static void testParseCounterOffer()
	{
		// Bare shorthand numbers ARE the counter in a live haggle (the deal context disambiguates intent).
		eq(12000, FakePlayerChatParsing.parseCounterOffer("can you do 12k?"), "'can you do 12k' -> 12000");
		eq(12000, FakePlayerChatParsing.parseCounterOffer("12k?"), "'12k?' -> 12000");
		eq(10000, FakePlayerChatParsing.parseCounterOffer("make it 10k and deal"), "'make it 10k' -> 10000");
		eq(2000000, FakePlayerChatParsing.parseCounterOffer("2kk max"), "'2kk' -> 2,000,000");
		eq(1000000, FakePlayerChatParsing.parseCounterOffer("1m and its yours"), "'1m' -> 1,000,000");
		// Explicit per-unit price still works (delegates to parseTradeUnitPrice).
		eq(12000, FakePlayerChatParsing.parseCounterOffer("12k each"), "'12k each' -> 12000");
		eq(300, FakePlayerChatParsing.parseCounterOffer("@300"), "'@300' -> 300");
		// A bare number with no suffix and no price cue is a quantity, not a price -> ignored.
		eq(0, FakePlayerChatParsing.parseCounterOffer("i'll take 5000"), "bare quantity -> no counter");
		eq(0, FakePlayerChatParsing.parseCounterOffer("give me 200"), "bare number -> no counter");
		// A message with a quantity then a suffixed price reads the price ("500 ssd, 12k" -> 12000).
		eq(12000, FakePlayerChatParsing.parseCounterOffer("500 ssd for 12k"), "quantity then 12k -> 12000");
		// Nothing usable -> 0.
		eq(0, FakePlayerChatParsing.parseCounterOffer("sounds good"), "no number -> 0");
		eq(0, FakePlayerChatParsing.parseCounterOffer(null), "null -> 0");
	}

	private static void testAcceptsCounter()
	{
		// Bot SELLING at 14k: a small discount within 15% is accepted; a bigger cut is refused (bot holds price).
		truth(FakePlayerChatParsing.acceptsCounter(12000, 14000, true), "sell: 12k vs 14k ask -> accept");
		truth(FakePlayerChatParsing.acceptsCounter(14000, 14000, true), "sell: exact ask -> accept");
		truth(FakePlayerChatParsing.acceptsCounter(20000, 14000, true), "sell: player offers more -> accept");
		truth(FakePlayerChatParsing.acceptsCounter(11900, 14000, true), "sell: exactly at 15% floor -> accept");
		truth(!FakePlayerChatParsing.acceptsCounter(11899, 14000, true), "sell: just below floor -> refuse");
		truth(!FakePlayerChatParsing.acceptsCounter(8000, 14000, true), "sell: lowball -> refuse");
		// Bot BUYING at 300: paying a bit more is accepted; paying far more is refused (bot holds).
		truth(FakePlayerChatParsing.acceptsCounter(330, 300, false), "buy: 330 vs 300 offer -> accept");
		truth(FakePlayerChatParsing.acceptsCounter(250, 300, false), "buy: player asks less -> accept");
		truth(FakePlayerChatParsing.acceptsCounter(345, 300, false), "buy: exactly at 15% ceiling -> accept");
		truth(!FakePlayerChatParsing.acceptsCounter(346, 300, false), "buy: just above ceiling -> refuse");
		truth(!FakePlayerChatParsing.acceptsCounter(500, 300, false), "buy: overpay -> refuse");
		// Guards.
		truth(!FakePlayerChatParsing.acceptsCounter(0, 14000, true), "no counter -> refuse");
		truth(!FakePlayerChatParsing.acceptsCounter(12000, 0, true), "no anchor -> refuse");
	}

	private static void testBareCounterCandidate()
	{
		// FPC-042: a bare number with a PRICE cue is a candidate to CONFIRM (not commit); no cue -> leave alone.
		eq(17, FakePlayerChatParsing.parseBareCounterCandidate("make it 17"), "'make it 17' -> 17 candidate");
		eq(17, FakePlayerChatParsing.parseBareCounterCandidate("17?"), "'17?' -> 17 candidate");
		eq(20, FakePlayerChatParsing.parseBareCounterCandidate("how about 20"), "'how about 20' -> 20 candidate");
		eq(17, FakePlayerChatParsing.parseBareCounterCandidate("17 works"), "'17 works' -> 17 candidate");
		// A quantity cue means the bare number is an amount, never a price.
		eq(0, FakePlayerChatParsing.parseBareCounterCandidate("i'll take 5000"), "quantity 'take' -> no candidate");
		eq(0, FakePlayerChatParsing.parseBareCounterCandidate("i need 200"), "quantity 'need' -> no candidate");
		// A bare number with no cue at all is ambiguous -> leave the deal unchanged (no clarify).
		eq(0, FakePlayerChatParsing.parseBareCounterCandidate("5000"), "bare number, no cue -> no candidate");
		// An explicit/suffixed price is handled by parseCounterOffer, not treated as a bare candidate.
		eq(0, FakePlayerChatParsing.parseBareCounterCandidate("12k?"), "suffixed price -> no bare candidate");
		eq(0, FakePlayerChatParsing.parseBareCounterCandidate(null), "null -> no candidate");

		// scaleBareCounter picks the interpretation closest in magnitude to the current quoted price.
		eq(17000, FakePlayerChatParsing.scaleBareCounter(17, 15000), "17 vs 15k price -> 17,000");
		eq(17, FakePlayerChatParsing.scaleBareCounter(17, 15), "17 vs 15 adena price -> 17");
		eq(17000, FakePlayerChatParsing.scaleBareCounter(17, 0), "17 with no anchor -> 17,000 (k shorthand)");
		eq(2000000, FakePlayerChatParsing.scaleBareCounter(2, 1500000), "2 vs 1.5m price -> 2,000,000");
	}

	private static void testActionIntentPredicates()
	{
		// FPC-044/FPC-046: destructive DISBAND needs a real dismiss order; negation/keep phrasings hold.
		truth(FakePlayerChatParsing.isDismissOrder("you can go now, thanks"), "'you can go' -> dismiss");
		truth(FakePlayerChatParsing.isDismissOrder("alright disband"), "'disband' -> dismiss");
		truth(!FakePlayerChatParsing.isDismissOrder("don't leave the party"), "'don't leave' -> hold");
		truth(!FakePlayerChatParsing.isDismissOrder("stay with me"), "'stay' -> hold");
		truth(!FakePlayerChatParsing.isDismissOrder("hey how's it going"), "chit-chat -> hold");
		truth(!FakePlayerChatParsing.isDismissOrder(null), "null -> hold");

		// Reversible FOLLOW/STAY are vetoed only when the player negates them.
		truth(FakePlayerChatParsing.negatesFollow("don't follow me"), "'don't follow' -> veto follow");
		truth(FakePlayerChatParsing.negatesFollow("wait here"), "'wait here' -> veto follow");
		truth(!FakePlayerChatParsing.negatesFollow("come on then"), "'come on' -> allow follow");
		truth(!FakePlayerChatParsing.negatesFollow(null), "null -> allow follow");
		truth(FakePlayerChatParsing.negatesStay("follow me"), "'follow me' -> veto stay");
		truth(FakePlayerChatParsing.negatesStay("don't stay here"), "'don't stay' -> veto stay");
		truth(!FakePlayerChatParsing.negatesStay("hold this spot"), "'hold this spot' -> allow stay");
	}

	private static void testDealIntentPredicates()
	{
		// FPC-060: SHOP/MEET commit needs the player's own acceptance, not just the model tag.
		truth(FakePlayerChatParsing.isDealAccept("ok deal"), "'ok deal' -> accept");
		truth(FakePlayerChatParsing.isDealAccept("sure"), "'sure' -> accept");
		truth(FakePlayerChatParsing.isDealAccept("yeah i'll take it"), "'i'll take it' -> accept");
		truth(FakePlayerChatParsing.isDealAccept("gk"), "naming a meet place -> accept");
		truth(FakePlayerChatParsing.isDealAccept("meet me at the warehouse"), "'meet me' -> accept");
		truth(!FakePlayerChatParsing.isDealAccept("nah, not interested"), "'not interested' -> not accept");
		truth(!FakePlayerChatParsing.isDealAccept("no deal"), "'no deal' -> not accept");
		truth(!FakePlayerChatParsing.isDealAccept("hmm let me think"), "ambiguous -> not accept");
		truth(!FakePlayerChatParsing.isDealAccept("broke"), "'broke' does not match 'ok' substring");
		truth(!FakePlayerChatParsing.isDealAccept("can you do 5k"), "'5k' does not match 'k' as accept");
		truth(!FakePlayerChatParsing.isDealAccept(null), "null -> not accept");

		// FPC-061: MEET:cancel needs a real cancel intent; conservative and phrase-based.
		truth(FakePlayerChatParsing.isDealCancel("cancel it"), "'cancel' -> cancel");
		truth(FakePlayerChatParsing.isDealCancel("forget it"), "'forget it' -> cancel");
		truth(FakePlayerChatParsing.isDealCancel("changed my mind, not coming"), "'not coming' -> cancel");
		truth(FakePlayerChatParsing.isDealCancel("nvm"), "'nvm' -> cancel");
		truth(!FakePlayerChatParsing.isDealCancel("ok deal"), "acceptance -> not cancel");
		truth(!FakePlayerChatParsing.isDealCancel("no worries, meet me at gk"), "'no worries' -> not cancel");
		truth(!FakePlayerChatParsing.isDealCancel(null), "null -> not cancel");
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
		// FPC-062: SHOP is a bare transition signal (Java owns side/item/price), so the pattern only detects and
		// strips the tag in any form; it no longer captures a payload.
		truth(FakePlayerChatParsing.SHOP_TAG.matcher("deal [[SHOP]]").find(), "bare SHOP matches");
		truth(FakePlayerChatParsing.SHOP_TAG.matcher("deal [[SHOP:SELL:soulshot:5k]]").find(), "legacy payload SHOP still matches");
		truth(FakePlayerChatParsing.SHOP_TAG.matcher("ok [[SHOP:garbage]").find(), "malformed SHOP close still matches so it is stripped");
		truth(!FakePlayerChatParsing.SHOP_TAG.matcher("i went shopping today").find(), "prose 'shopping' does not match");
		eq("done", FakePlayerChatParsing.SHOP_TAG.matcher("done [[SHOP:SELL:x:5k]]").replaceAll("").trim(), "SHOP tag strips out");
	}

	private static void testCountBefore()
	{
		// "2 dd" - the count sits just before the role word at index 2.
		eq(2, FakePlayerChatParsing.countBefore("2 dd", 2), "'2 dd' -> 2");
		eq(1, FakePlayerChatParsing.countBefore("dd", 0), "no leading number -> 1");
		eq(6, FakePlayerChatParsing.countBefore("10 tanks", 3), "10 clamps to the 6 cap");
		eq(1, FakePlayerChatParsing.countBefore("0 dd", 2), "0 clamps up to 1");
		eq(3, FakePlayerChatParsing.countBefore("3   dd", 4), "extra spaces between count and word -> 3");
		// A number preceded by a level keyword is a LEVEL, not a count: "1 lvl 80 pp" must recruit 1 pp, not 80.
		eq(1, FakePlayerChatParsing.countBefore("1 lvl 80 pp", 9), "'lvl 80 pp' -> level, not a count");
		eq(1, FakePlayerChatParsing.countBefore("lfm level 80 pp", 13), "'level 80 pp' -> level, not a count");
		eq(1, FakePlayerChatParsing.countBefore("lf lv 76 se", 9), "'lv 76 se' -> level, not a count");
		// A genuine count right before the word is still read (no level keyword in front of it).
		eq(2, FakePlayerChatParsing.countBefore("lfm 2 pp", 6), "'2 pp' with no level keyword -> 2");
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

		// FPC-004: an oversized numeric count must clamp, not throw NumberFormatException on a very long number.
		req(FakePlayerChatParsing.parseRoleRequests("99999999999 dd").get(0), "dd", 6, "huge count clamps to 6 without throwing");

		// Plurals are emitted verbatim (caller resolves the singular).
		req(FakePlayerChatParsing.parseRoleRequests("3 mages").get(0), "mages", 3, "plural token kept verbatim");

		// A race adjective attaches to the NEXT role/class word and is not itself a request.
		r = FakePlayerChatParsing.parseRoleRequests("elf archer");
		eq(1, r.size(), "'elf archer' -> one request (race is a modifier, not a recruit)");
		reqRace(r.get(0), "archer", 1, "elf", "elf archer");
		// Two-word "dark elf" resolves to dark_elf, not plain elf.
		reqRace(FakePlayerChatParsing.parseRoleRequests("dark elf tank").get(0), "tank", 1, "dark_elf", "dark elf tank");
		// Count + race both apply to the following word.
		reqRace(FakePlayerChatParsing.parseRoleRequests("2 orc buffer").get(0), "buffer", 2, "orc", "2 orc buffer");
		// A short race alias works too.
		reqRace(FakePlayerChatParsing.parseRoleRequests("de nuker lvl 40").get(0), "nuker", 1, "de", "de nuker");
		// A race word with no following role is dropped (no phantom spawns from a bare race).
		eq(0, FakePlayerChatParsing.parseRoleRequests("elf").size(), "bare race word -> no request");
		// Race must PRECEDE the role; a trailing race does not attach.
		req(FakePlayerChatParsing.parseRoleRequests("healer elf").get(0), "healer", 1, "trailing race does not attach");
		eq(1, FakePlayerChatParsing.parseRoleRequests("healer elf").size(), "'healer elf' -> one request only");

		// Empty / null inputs are safe.
		eq(0, FakePlayerChatParsing.parseRoleRequests("").size(), "empty text -> no requests");
		eq(0, FakePlayerChatParsing.parseRoleRequests(null).size(), "null text -> no requests");
	}

	private static void testFuzzyMatching()
	{
		// Edit distance: identical, empty, and single insert/delete/substitute = 1.
		eq(0, FakePlayerChatParsing.editDistance("prophet", "prophet"), "identical -> 0");
		eq(7, FakePlayerChatParsing.editDistance("", "prophet"), "empty vs word -> length");
		eq(1, FakePlayerChatParsing.editDistance("warcyer", "warcryer"), "missing letter -> 1");
		eq(1, FakePlayerChatParsing.editDistance("prophrt", "prophet"), "one substitution -> 1");
		// Adjacent transposition costs 1 (Damerau), so "bishpo" is one typo from "bishop".
		eq(1, FakePlayerChatParsing.editDistance("bishpo", "bishop"), "adjacent transposition -> 1");

		// Length-scaled budget: short aliases are exact-only, longer names forgive one/two typos.
		eq(0, FakePlayerChatParsing.fuzzyBudget(2), "2-letter alias -> exact only");
		eq(1, FakePlayerChatParsing.fuzzyBudget(6), "6-letter word -> one typo");
		eq(2, FakePlayerChatParsing.fuzzyBudget(9), "long name -> two typos");

		// nearestWithin returns the lone closest inside budget; a tie returns null so the caller asks, not guesses.
		final List<String> cands = List.of("warcryer", "prophet", "bishop", "healer", "dancer");
		eq("warcryer", FakePlayerChatParsing.nearestWithin("warcyer", cands, 1), "typo resolves to nearest");
		eq(null, FakePlayerChatParsing.nearestWithin("zzzzzz", cands, 1), "nothing within budget -> null");
		truth(FakePlayerChatParsing.nearestWithin("xealer", List.of("healer", "dealer"), 1) == null, "ambiguous tie -> null");

		// minDistance is the closest of the whole set.
		eq(1, FakePlayerChatParsing.minDistance("warcyer", cands), "minDistance finds the 1-off candidate");
		eq(Integer.MAX_VALUE, FakePlayerChatParsing.minDistance("dd", java.util.List.of()), "empty candidates -> MAX_VALUE");
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

	private static void reqRace(RoleRequest actual, String token, int count, String race, String what)
	{
		eq(token, actual.token, what + " (token)");
		eq(count, actual.count, what + " (count)");
		eq(race, actual.race, what + " (race)");
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
