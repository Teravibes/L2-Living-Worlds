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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure, side-effect-free parsing helpers extracted from {@link FakePlayerChatManager}.<br>
 * These translate what a player or the LLM brain typed into structured values (trade quantities, meet
 * spots, party levels, control-tag parsing). They touch no game state, so they can be unit-tested with a
 * plain JDK - see {@code tests/java/FakePlayerChatParsingTest.java}. Keeping them here, and having the
 * manager delegate to them, means the tested logic is the logic that actually runs (no drifting copy).
 */
public final class FakePlayerChatParsing
{
	// The brain appends [[MEET:spot]] to a whisper when it agrees to walk over; we act on it then strip it.
	// The closing is deliberately tolerant: Ollama sometimes emits a malformed close ("[[MEET:gk])", "[[MEET:gk]",
	// "[[MEET:gk))") and a strict "\]\]" would neither act on nor strip it, leaking the tag into visible chat. We
	// accept one or two of "]" / ")" as the close so every variant is caught and removed.
	public static final Pattern MEET_TAG = Pattern.compile("\\[\\[\\s*MEET\\s*:\\s*([a-zA-Z]+)\\s*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	// [[SHOP:SELL|BUY:<item>:<price>]] - the bot commits to actually trading a specific item at a price.
	public static final Pattern SHOP_TAG = Pattern.compile("\\[\\[\\s*SHOP\\s*:\\s*(SELL|BUY)\\s*:\\s*([^:\\]]+?)\\s*:\\s*(\\d+)\\s*(kk|k)?\\s*\\]\\]", Pattern.CASE_INSENSITIVE);
	public static final Pattern TRADE_AD = Pattern.compile("(wts|selling|s>|wtb|buying|b>)\\s*(.*)", Pattern.CASE_INSENSITIVE);
	public static final Pattern TRADE_QUANTITY = Pattern.compile("\\b(\\d{1,9})(k|kk|m)?\\b", Pattern.CASE_INSENSITIVE);
	public static final Pattern LFP_TRIGGER = Pattern.compile("\\b(lfm|lfp|lfg|lf|looking for|need|recruit|wanna pt|party up|join.*pt|more for|ppl for|pst)\\b", Pattern.CASE_INSENSITIVE);
	public static final Pattern LFP_LEVEL = Pattern.compile("(?:level|lvl|lv)\\s*\\.?\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE);

	private FakePlayerChatParsing()
	{
		// Utility holder - not instantiable.
	}

	/**
	 * Extract a bulk quantity from a WTS/WTB phrase, applying k/kk/m suffixes and clamping to a safe cap.
	 * @param phrase the free-text remainder of the trade ad (e.g. "ssd 5k")
	 * @param stackable whether the resolved item stacks; non-stackable items always yield 0
	 * @return the first sane quantity in (0, 2000000], or 0 when none is found
	 */
	public static int parseTradeQuantity(String phrase, boolean stackable)
	{
		if ((phrase == null) || !stackable)
		{
			return 0;
		}
		final Matcher matcher = TRADE_QUANTITY.matcher(phrase);
		while (matcher.find())
		{
			long value = Long.parseLong(matcher.group(1));
			final String suffix = matcher.group(2);
			if ("k".equalsIgnoreCase(suffix))
			{
				value *= 1000L;
			}
			else if ("kk".equalsIgnoreCase(suffix) || "m".equalsIgnoreCase(suffix))
			{
				value *= 1000000L;
			}

			if ((value > 0) && (value <= 2_000_000L))
			{
				return (int) value;
			}
		}
		return 0;
	}

	/**
	 * Apply the trailing k/kk multiplier from a {@code [[SHOP:...]]} price.<br>
	 * NOTE: preserves the manager's original {@code int} arithmetic exactly (no 'm' suffix here, unlike
	 * {@link #parseTradeQuantity}); a price wide enough to overflow {@code int} after {@code *1000000} would
	 * wrap just as it did before this extraction. Behaviour is intentionally unchanged.
	 * @param price the base price parsed from the tag
	 * @param mult the suffix group ("k", "kk", or null)
	 * @return the multiplied price
	 */
	public static int applyShopPriceMultiplier(int price, String mult)
	{
		if ("k".equalsIgnoreCase(mult))
		{
			return price * 1000;
		}
		if ("kk".equalsIgnoreCase(mult))
		{
			return price * 1000000;
		}
		return price;
	}

	/** @return the level requested in an LFP shout (1-80), or 0 when none is given (match the recruiter). */
	public static int parseLfpLevel(String text)
	{
		final Matcher matcher = LFP_LEVEL.matcher(text);
		if (matcher.find())
		{
			final int level = Integer.parseInt(matcher.group(1));
			if ((level >= 1) && (level <= 80))
			{
				return level;
			}
		}
		return 0;
	}

	/** @return {@code true} if the text reads like a party call (has an LFM/LFP trigger). */
	public static boolean looksLikeLfp(String text)
	{
		return LFP_TRIGGER.matcher(text).find();
	}

	/** @return {@code true} if the text opens with a WTS/WTB trade-ad marker. */
	public static boolean looksLikeTradeAd(String text)
	{
		return TRADE_AD.matcher(text).find();
	}

	/** @return the count number immediately before position {@code pos} in the text (e.g. "2 dd" -> 2), else 1. */
	public static int countBefore(CharSequence text, int pos)
	{
		int i = pos - 1;
		while ((i >= 0) && (text.charAt(i) == ' '))
		{
			i--;
		}
		int end = i;
		while ((i >= 0) && Character.isDigit(text.charAt(i)))
		{
			i--;
		}
		if (i < end)
		{
			try
			{
				return Math.max(1, Math.min(6, Integer.parseInt(text.subSequence(i + 1, end + 1).toString())));
			}
			catch (NumberFormatException e)
			{
				return 1;
			}
		}
		return 1;
	}

	/** A "N x &lt;token&gt;" recruit request parsed from a party call, before the token is resolved to a role/class. */
	public static final class RoleRequest
	{
		public final String token;
		public final int count;

		public RoleRequest(String token, int count)
		{
			this.token = token;
			this.count = count;
		}
	}

	/**
	 * Phase 2 of party-call parsing: the pure counting state machine over the leftover words (after specific
	 * class names have been consumed). Handles "lvl/level/lv N" (the number is a level, not a count), numeric
	 * counts clamped to 1-6, and a fresh count per word. Emits one {@link RoleRequest} per non-numeric word in
	 * order; whether a token actually names a role (and any plural fallback) is resolved by the caller, which
	 * also enforces the party-wide recruit cap. Uses no game types, so it is unit-testable on a plain JDK.
	 * @param remainingText the text left after phase-1 class names were blanked (case-insensitive)
	 * @return the requested (token, count) pairs, in reading order
	 */
	public static List<RoleRequest> parseRoleRequests(String remainingText)
	{
		final List<RoleRequest> requests = new ArrayList<>();
		if (remainingText == null)
		{
			return requests;
		}

		int pendingCount = 1;
		boolean levelToken = false; // the number right after "lvl"/"level"/"lv" is a level, not a count
		for (String token : remainingText.toLowerCase().split("[^a-z0-9]+"))
		{
			if (token.isEmpty())
			{
				continue;
			}
			if (token.equals("lvl") || token.equals("level") || token.equals("lv"))
			{
				levelToken = true;
				continue;
			}
			if (token.matches("\\d+"))
			{
				if (levelToken)
				{
					levelToken = false; // consume the level number; don't treat it as a count
					continue;
				}
				pendingCount = Math.max(1, Math.min(6, Integer.parseInt(token)));
				continue;
			}
			levelToken = false;
			requests.add(new RoleRequest(token, pendingCount));
			pendingCount = 1; // a number only applies to the class/role word right after it
		}
		return requests;
	}

	/**
	 * Canonicalise a free-text meet spot (and its common aliases) to one of a fixed set of destinations.
	 * @param spot the raw spot from a {@code [[MEET:spot]]} tag
	 * @return one of "gatekeeper", "warehouse", "shop", or "cancel"; unknown/blank spots default to "gatekeeper"
	 */
	public static String normalizeMeetSpot(String spot)
	{
		if (spot == null)
		{
			return "gatekeeper";
		}

		final String normalized = spot.trim().toLowerCase();
		switch (normalized)
		{
			case "gk":
			case "gate":
			case "gatekeeper":
			{
				return "gatekeeper";
			}
			case "wh":
			case "warehouse":
			case "ware":
			{
				return "warehouse";
			}
			case "shop":
			case "store":
			case "merchant":
			{
				return "shop";
			}
			case "cancel":
			case "no":
			case "nvm":
			case "nevermind":
			{
				return "cancel";
			}
		}

		return "gatekeeper";
	}
}
