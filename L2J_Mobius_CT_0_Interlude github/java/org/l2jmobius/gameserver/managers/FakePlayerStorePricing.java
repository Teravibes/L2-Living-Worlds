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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure, side-effect-free pricing / quantity / text helpers extracted from {@link FakePlayerStoreFactory}.<br>
 * These are the money-safety and formatting decisions of the fake-player store system - clamping a
 * whisper-negotiated price into a sane band, clamping deal quantities, sizing bulk stacks, formatting a
 * store sign, and tokenising an item phrase. They touch no game state or randomness, so they can be
 * unit-tested with a plain JDK - see {@code tests/java/FakePlayerStorePricingTest.java}. The factory
 * delegates to them so the tested logic is the logic that actually runs (no drifting copy).
 */
public final class FakePlayerStorePricing
{
	// Filler words to ignore when matching a trade-ad phrase to an item name.
	private static final Set<String> MATCH_STOPWORDS = Set.of("grade", "gr", "the", "a", "an", "of", "for", "pls", "plz", "pm", "cheap", "each", "ea", "some", "any", "my", "g", "lvl");

	private FakePlayerStorePricing()
	{
		// Utility holder - not instantiable.
	}

	/**
	 * Clamp a whisper-negotiated unit price into a sane band around the item reference price. The agreed
	 * price is trust-based on the LLM, so without this a player (or a trade-chat prompt injection) could talk
	 * a bot into selling a rare item for 1 adena or buying junk for billions. Haggling still works within the
	 * band; only absurd values get pinned. When the bot is selling, the floor matters most (don't sell cheap);
	 * when buying, the ceiling matters most (don't overpay).
	 * @param unitPrice the agreed adena per unit (already {@code > 0})
	 * @param referencePrice the item reference price
	 * @param selling {@code true} if the bot sells to the player, {@code false} if it buys from the player
	 * @return the clamped unit price, at least 1
	 */
	public static int clampDealPrice(int unitPrice, int referencePrice, boolean selling)
	{
		if (referencePrice <= 0)
		{
			return Math.max(1, unitPrice);
		}
		final double lowFactor = selling ? 0.5 : 0.1;
		final double highFactor = selling ? 3.0 : 1.5;
		final long low = Math.max(1L, Math.round(referencePrice * lowFactor));
		final long high = Math.max(low, Math.round(referencePrice * highFactor));
		return (int) Math.max(low, Math.min(high, unitPrice));
	}

	/**
	 * Clamp a deal's stack count. Non-stackable items are always a single piece; a requested count is pinned
	 * to [1, 2,000,000] so a negotiated/injected quantity can never overflow or drain a stack; otherwise the
	 * caller's auto-sized fallback is used (at least 1).
	 * @param stackable whether the item stacks
	 * @param requestedCount the requested stack count, or 0 to auto-size
	 * @param fallbackCount the auto-sized count to use when nothing was requested
	 * @return the normalised count, at least 1
	 */
	public static int normalizedDealCount(boolean stackable, int requestedCount, int fallbackCount)
	{
		if (!stackable)
		{
			return 1;
		}
		if (requestedCount > 0)
		{
			return Math.max(1, Math.min(requestedCount, 2_000_000));
		}
		return Math.max(1, fallbackCount);
	}

	/**
	 * The believable stack-size band for a stackable good, scaled inversely to its unit value (cheap shots in
	 * the thousands, pricey mats in single digits). The caller rolls a random amount within the band.
	 * @param referencePrice the item reference price
	 * @return a two-element {@code {min, max}} range
	 */
	public static int[] bulkAmountRange(int referencePrice)
	{
		if (referencePrice <= 10)
		{
			return new int[]
			{
				2000,
				15000
			}; // shots, arrows, cheap mats
		}
		if (referencePrice <= 60)
		{
			return new int[]
			{
				300,
				3000
			};
		}
		if (referencePrice <= 600)
		{
			return new int[]
			{
				30,
				400
			};
		}
		if (referencePrice <= 6000)
		{
			return new int[]
			{
				3,
				40
			};
		}
		return new int[]
		{
			1,
			8
		}; // pricey mats / scrolls
	}

	/**
	 * Compact shop-sign abbreviation for a soulshot/spiritshot item name, or {@code null} if the name is not a
	 * shot. Soulshot -&gt; "SS", Spiritshot -&gt; "SPS", Blessed Spiritshot -&gt; "BSS", with the grade appended
	 * ("SSD", "BSSD", or "SS NG" for No Grade) - the shorthand L2 players actually use on shop signs.
	 * Case-insensitive and tolerant of the datapack's ": D-Grade" / " D grade" spellings.
	 * @param name the item name
	 * @return the abbreviation, or {@code null} when the name is not a soulshot/spiritshot
	 */
	public static String shotAbbrev(String name)
	{
		if (name == null)
		{
			return null;
		}
		final String lower = name.toLowerCase(Locale.US);
		final boolean spirit = lower.contains("spiritshot");
		final boolean soul = lower.contains("soulshot");
		if (!spirit && !soul)
		{
			return null;
		}
		final String base = spirit ? (lower.contains("blessed") ? "BSS" : "SPS") : "SS";
		return base + shotGradeTag(lower);
	}

	/** The grade suffix for a shot name: "NG" (spaced) for No Grade, else the bare grade letter, else empty. */
	private static String shotGradeTag(String lower)
	{
		if (lower.contains("no grade"))
		{
			return " NG";
		}
		for (String grade : new String[]
		{
			"d",
			"c",
			"b",
			"a",
			"s"
		})
		{
			if (lower.contains(grade + "-grade") || lower.contains(grade + " grade"))
			{
				return grade.toUpperCase(Locale.US);
			}
		}
		return "";
	}

	/**
	 * The shop-sign label for a shot line: {@link #shotAbbrev} plus, when {@code withPrice}, the per-unit price in
	 * adena ("SSD 100a"). This is the classic L2 vendor convention for advertising shots.
	 * @param name the item name
	 * @param unitPrice the per-unit price in adena
	 * @param withPrice whether to append the per-unit price (sellers advertise it; buy signs omit it)
	 * @return the sign label, or {@code null} when the name is not a shot
	 */
	public static String shotSign(String name, int unitPrice, boolean withPrice)
	{
		final String abbr = shotAbbrev(name);
		if (abbr == null)
		{
			return null;
		}
		return withPrice ? (abbr + " " + amount(unitPrice) + "a") : abbr;
	}

	/** 15000 -&gt; "15k", 1500 -&gt; "1.5k", 800 -&gt; "800". */
	public static String amount(int count)
	{
		if (count >= 1000)
		{
			final double thousands = count / 1000.0;
			return (thousands == Math.floor(thousands) ? String.valueOf((int) thousands) : String.format(Locale.US, "%.1f", thousands)) + "k";
		}
		return String.valueOf(count);
	}

	/** Normalises a name/phrase to lowercase word tokens: strips punctuation, drops filler, folds plurals. */
	public static List<String> matchTokens(String text)
	{
		final List<String> tokens = new ArrayList<>();
		if (text == null)
		{
			return tokens;
		}
		for (String word : text.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim().split(" "))
		{
			if (word.isEmpty() || MATCH_STOPWORDS.contains(word))
			{
				continue;
			}
			if ((word.length() > 3) && word.endsWith("s"))
			{
				word = word.substring(0, word.length() - 1); // crude singularize: soulshots -> soulshot
			}
			tokens.add(word);
		}
		return tokens;
	}
}
