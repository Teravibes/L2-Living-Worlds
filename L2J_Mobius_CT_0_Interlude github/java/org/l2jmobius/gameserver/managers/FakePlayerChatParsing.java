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
	// FPC-062: SHOP is a bare transition signal. Java owns the side/item/price/count (FPC-055), so the tag body is
	// never read; this tolerant matcher accepts bare [[SHOP]], the legacy [[SHOP:SELL:item:price]] payload, and the
	// malformed closings weak models emit, so handleMeetRequest can both detect the signal and strip any SHOP form out
	// of the spoken line (a strict payload-only pattern used to leave a malformed SHOP tag visible in chat).
	public static final Pattern SHOP_TAG = Pattern.compile("\\[\\[\\s*SHOP\\b[^\\]\\)]*[\\]\\)]{1,2}", Pattern.CASE_INSENSITIVE);
	public static final Pattern TRADE_AD = Pattern.compile("(wts|selling|s>|wtb|buying|b>)\\s*(.*)", Pattern.CASE_INSENSITIVE);
	public static final Pattern TRADE_QUANTITY = Pattern.compile("\\b(\\d{1,9})(k|kk|m)?\\b", Pattern.CASE_INSENSITIVE);
	// A unit price the player explicitly marked in a trade ad. Only a number carrying a price cue counts, so a bare
	// number stays a quantity (see parseTradeQuantity): either "@<n>" ("ssd @300") or "<n><suffix?> <cue>" where the
	// cue is an adena/per-unit marker (adena, ad, a, ea, each, pc, per). "adena"/"ad" are listed before "a" so the
	// longer word wins the alternation. Group 1/2 = the "@" form digits/suffix; group 3/4 = the cue form digits/suffix.
	public static final Pattern TRADE_UNIT_PRICE = Pattern.compile("(?:@\\s*(\\d{1,9})(kk|k|m)?)|(?:\\b(\\d{1,9})(kk|k|m)?\\s*(?:adena|ad|ea|each|pc|per|a)\\b)", Pattern.CASE_INSENSITIVE);
	public static final Pattern LFP_TRIGGER = Pattern.compile("\\b(lfm|lfp|lfg|lf|looking for|need|recruit|wanna pt|party up|join.*pt|more for|ppl for|pst)\\b", Pattern.CASE_INSENSITIVE);
	public static final Pattern LFP_LEVEL = Pattern.compile("(?:level|lvl|lv)\\s*\\.?\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE);
	// Spoken amounts a player gives when a bot asks "how many?": a word or digit count followed by a magnitude word
	// ("a couple thousand", "few hundred", "2 thousand", "a million"). Digits with a k/kk/m suffix are handled by
	// TRADE_QUANTITY instead; this only covers the spelled-out magnitude words that regex misses.
	public static final Pattern MAGNITUDE_QUANTITY = Pattern.compile("\\b(\\d{1,4}|a|an|one|two|three|four|five|six|seven|eight|nine|ten|couple|pair|few|several)\\s+(?:of\\s+)?(hundred|thousand|k|kk|mil|million)\\b", Pattern.CASE_INSENSITIVE);
	// A vague "just give me a normal amount" answer, with no number at all: fall back to the item's default stack.
	public static final Pattern VAGUE_BULK = Pattern.compile("\\b(some|a\\s+bunch|bunch|a\\s+stack|stack|full\\s+stack|a\\s+lot|lots|plenty|whatever|however\\s+many|ur\\s+call|your\\s+call|dealers?\\s+choice)\\b", Pattern.CASE_INSENSITIVE);
	/** {@link #parseSpokenQuantity} sentinel: the player wants a normal amount but named no number - use the default stack. */
	public static final int SPOKEN_QUANTITY_DEFAULT = -1;

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
	 * Extract a unit price the player explicitly stated in a WTS/WTB ad (e.g. "wtb ssd 300 adena", "wts ss 5k ea",
	 * "ssd @300"). Only a number carrying a price cue is read as a price, so a bare quantity ("wtb 5k ssd") is never
	 * mistaken for one. The k/kk/m suffix is applied. This is deterministic (no LLM), and the caller still clamps the
	 * result into a sane band around the item's value, so an absurd or adversarial price cannot open an exploit store.
	 * @param phrase the free-text remainder of the trade ad (the words after WTS/WTB)
	 * @return the first stated unit price in (0, 2,000,000,000], or 0 when the player named no price
	 */
	public static int parseTradeUnitPrice(String phrase)
	{
		if (phrase == null)
		{
			return 0;
		}
		final Matcher matcher = TRADE_UNIT_PRICE.matcher(phrase);
		while (matcher.find())
		{
			final String digits = (matcher.group(1) != null) ? matcher.group(1) : matcher.group(3);
			if (digits == null)
			{
				continue;
			}
			final String suffix = (matcher.group(1) != null) ? matcher.group(2) : matcher.group(4);
			long value = Long.parseLong(digits);
			if ("k".equalsIgnoreCase(suffix))
			{
				value *= 1000L;
			}
			else if ("kk".equalsIgnoreCase(suffix) || "m".equalsIgnoreCase(suffix))
			{
				value *= 1000000L;
			}

			if ((value > 0) && (value <= 2_000_000_000L))
			{
				return (int) value;
			}
		}
		return 0;
	}

	/**
	 * Resolve an amount from a player's reply to "how many do you want?", tolerant of the vague ways people
	 * actually answer. Recognises, in order: a spelled-out or digit count with a magnitude word ("a couple
	 * thousand" = 2000, "few hundred" = 300, "2 thousand" = 2000, "a million" = 1,000,000, capped at 2,000,000);
	 * plain digits with an optional k/kk/m suffix ("5k" = 5000) via {@link #parseTradeQuantity}; and a vague bulk
	 * answer with no number ("some", "a stack", "whatever") which returns {@link #SPOKEN_QUANTITY_DEFAULT} so the
	 * caller uses the item's normal stack size.
	 * @param text the player's reply (strip any stated price first with {@link #stripStatedPrices})
	 * @return a concrete amount in (0, 2,000,000], {@link #SPOKEN_QUANTITY_DEFAULT} for a numberless bulk ask, or 0 when no amount was given
	 */
	public static int parseSpokenQuantity(String text)
	{
		if (text == null)
		{
			return 0;
		}
		final Matcher mag = MAGNITUDE_QUANTITY.matcher(text);
		if (mag.find())
		{
			final long value = (long) spokenNumber(mag.group(1)) * magnitudeValue(mag.group(2));
			if (value > 0)
			{
				return (int) Math.min(value, 2_000_000L);
			}
		}
		final int digits = parseTradeQuantity(text, true);
		if (digits > 0)
		{
			return digits;
		}
		return VAGUE_BULK.matcher(text).find() ? SPOKEN_QUANTITY_DEFAULT : 0;
	}

	/** A digit string or a small spelled-out count word to its value ("couple" -&gt; 2, "few" -&gt; 3); 0 if unknown. */
	private static int spokenNumber(String word)
	{
		if (word == null)
		{
			return 0;
		}
		final String w = word.toLowerCase();
		if (w.matches("\\d{1,4}"))
		{
			return Integer.parseInt(w);
		}
		switch (w)
		{
			case "a":
			case "an":
			case "one":
				return 1;
			case "two":
			case "couple":
			case "pair":
				return 2;
			case "three":
			case "few":
				return 3;
			case "four":
			case "several":
				return 4;
			case "five":
				return 5;
			case "six":
				return 6;
			case "seven":
				return 7;
			case "eight":
				return 8;
			case "nine":
				return 9;
			case "ten":
				return 10;
			default:
				return 0;
		}
	}

	/** A magnitude word to its multiplier: hundred -&gt; 100, thousand/k -&gt; 1000, kk/mil/million -&gt; 1,000,000. */
	private static long magnitudeValue(String magnitude)
	{
		switch (magnitude.toLowerCase())
		{
			case "hundred":
				return 100L;
			case "thousand":
			case "k":
				return 1000L;
			case "kk":
			case "mil":
			case "million":
				return 1_000_000L;
			default:
				return 1L;
		}
	}

	/**
	 * Blank out any explicitly-priced number ("300 adena", "5k ea", "@300") in a phrase so a following
	 * {@link #parseTradeQuantity} reads the amount, not the price. Without this, "5000 at 300 adena" or
	 * "300 adena each, i want 5000" could hand the price to the quantity parser (it takes the first sane number).
	 * @param phrase the raw phrase
	 * @return the phrase with priced tokens replaced by spaces (never {@code null})
	 */
	public static String stripStatedPrices(String phrase)
	{
		if (phrase == null)
		{
			return "";
		}
		return TRADE_UNIT_PRICE.matcher(phrase).replaceAll(" ");
	}

	/**
	 * Apply the trailing k/kk multiplier from a {@code [[SHOP:...]]} price (no 'm' suffix here, unlike
	 * {@link #parseTradeQuantity}). The multiplication is done in {@code long} and clamped to
	 * {@code Integer.MAX_VALUE} so a wide price cannot wrap to a negative or otherwise invalid value (FPC-004).
	 * @param price the base price parsed from the tag
	 * @param mult the suffix group ("k", "kk", or null)
	 * @return the multiplied price, never negative and never above {@code Integer.MAX_VALUE}
	 */
	public static int applyShopPriceMultiplier(int price, String mult)
	{
		long value = price;
		if ("k".equalsIgnoreCase(mult))
		{
			value = (long) price * 1000;
		}
		else if ("kk".equalsIgnoreCase(mult))
		{
			value = (long) price * 1000000;
		}
		if (value < 0)
		{
			return 0;
		}
		return (value > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) value;
	}

	/**
	 * Read a counteroffer unit price from a player's reply during a LIVE deal - the bot has already quoted a price
	 * and the two are haggling. This is stricter about intent than {@link #parseTradeUnitPrice} in one way and
	 * looser in another: an explicit per-unit price ("@12k", "12k each") is taken as-is, but because the deal
	 * context makes the intent unambiguous, a bare shorthand number ("12k?", "can you do 12k", "make it 10k") is
	 * also read as the counter here. A bare number with NO k/kk/m suffix and NO price cue is deliberately ignored,
	 * so a plain quantity ("i'll take 5000") is never mistaken for a price. The suffix multiplier is applied. This
	 * is deterministic (no LLM); the caller must still validate the result against the economy band (see
	 * {@link FakePlayerStoreFactory#dealPriceWithinBand}) before accepting it, so an absurd or adversarial counter
	 * cannot take hold.
	 * @param text the player's whispered reply during an active deal
	 * @return the counteroffer unit price in (0, 2,000,000,000], or 0 when the player named no price
	 */
	public static int parseCounterOffer(String text)
	{
		if (text == null)
		{
			return 0;
		}
		// An explicit per-unit price ("@12k", "12k ea/each/adena") is unambiguous - take it first.
		final int explicit = parseTradeUnitPrice(text);
		if (explicit > 0)
		{
			return explicit;
		}
		// Otherwise a shorthand number IS the counter in a live haggle, but only when it carries a k/kk/m suffix;
		// a bare number could be a quantity, so it is left to the quantity parser.
		final Matcher matcher = TRADE_QUANTITY.matcher(text);
		while (matcher.find())
		{
			final String suffix = matcher.group(2);
			if ((suffix == null) || suffix.isEmpty())
			{
				continue; // bare number - ambiguous with quantity, skip it
			}
			long value = Long.parseLong(matcher.group(1));
			if ("k".equalsIgnoreCase(suffix))
			{
				value *= 1000L;
			}
			else if ("kk".equalsIgnoreCase(suffix) || "m".equalsIgnoreCase(suffix))
			{
				value *= 1000000L;
			}
			if ((value > 0) && (value <= 2_000_000_000L))
			{
				return (int) value;
			}
		}
		return 0;
	}

	/** A phrase that marks a bare number as a PRICE proposal ("make it 17", "how about 17", "17?"). */
	private static final Pattern BARE_PRICE_CUE = Pattern.compile("(?i)(?:make it|how ?(?:about|bout)|can (?:you|u) do|could (?:you|u) do|lower to|down to|up to|i(?:'?ll)? (?:do|give|pay|go)|gimme for|do (?:it )?for)\\s+\\d");
	/** A bare number immediately followed by a price/agreement cue ("17?", "17 deal", "17 then"). */
	private static final Pattern BARE_PRICE_TRAILER = Pattern.compile("(?i)(?<![\\w.])\\d{1,7}\\s*(?:\\?|deal\\b|then\\b|works?\\b|ok(?:ay)?\\b)");
	/** A phrase that marks a bare number as a QUANTITY, so it is never read as a price. */
	private static final Pattern BARE_QTY_CUE = Pattern.compile("(?i)\\b(?:take|want|need|how many|pcs|pieces|stack|units?|of them|of the|buy|sell(?:ing)?)\\b");
	/** First bare number (no k/kk/m suffix) in the text. */
	private static final Pattern BARE_NUMBER = Pattern.compile("(?<![\\w.])(\\d{1,7})(?![\\w.])");

	/**
	 * Read a BARE-number price candidate ("17?", "make it 17") from a live-deal reply that {@link #parseCounterOffer}
	 * deliberately ignored because the number carried no k/kk/m suffix. This does NOT commit a price: it is only used
	 * to ask the player to confirm the scaled value (see {@link #scaleBareCounter}). It returns 0 - meaning "leave the
	 * deal unchanged, do not even ask" - unless the line has a clear price cue and NO quantity cue, so a stated amount
	 * ("i'll take 5000") is never treated as a price and a truly ambiguous bare number is left alone.
	 * @param text the player's whispered reply during an active deal
	 * @return the raw bare number the player typed (before any scaling), or 0 when none should be treated as a price
	 */
	public static int parseBareCounterCandidate(String text)
	{
		if ((text == null) || (parseCounterOffer(text) > 0))
		{
			return 0; // no text, or an explicit/suffixed price already handled by parseCounterOffer
		}
		if (BARE_QTY_CUE.matcher(text).find())
		{
			return 0; // reads as a quantity, not a price
		}
		if (!BARE_PRICE_CUE.matcher(text).find() && !BARE_PRICE_TRAILER.matcher(text).find())
		{
			return 0; // no price cue - genuinely ambiguous, leave the deal unchanged
		}
		final Matcher matcher = BARE_NUMBER.matcher(text);
		if (matcher.find())
		{
			final long value = Long.parseLong(matcher.group(1));
			if ((value > 0) && (value <= 2_000_000_000L))
			{
				return (int) value;
			}
		}
		return 0;
	}

	/**
	 * Scale a bare counter candidate to the most plausible price given the current quoted unit price, choosing among
	 * the literal value and its k/kk interpretations the one closest in magnitude to the current offer. With current
	 * price 15,000 a bare "17" scales to 17,000; with current price 15 it stays 17. Used only to phrase a clarification
	 * question, never to commit a price, so an imperfect guess is harmless - the player still confirms.
	 * @param bare the raw bare number from {@link #parseBareCounterCandidate}
	 * @param currentUnitPrice the bot's current quoted unit price; {@code <= 0} means no anchor (assume k shorthand)
	 * @return the plausible scaled price in (0, 2,000,000,000]
	 */
	public static int scaleBareCounter(int bare, int currentUnitPrice)
	{
		if (bare <= 0)
		{
			return 0;
		}
		if (currentUnitPrice <= 0)
		{
			return (int) Math.min(2_000_000_000L, bare * 1000L); // no anchor: bare numbers in trade are usually 'k'
		}
		final long[] candidates =
		{
			bare,
			bare * 1000L,
			bare * 1000000L
		};
		long best = bare;
		double bestDistance = Double.MAX_VALUE;
		for (long candidate : candidates)
		{
			if ((candidate <= 0) || (candidate > 2_000_000_000L))
			{
				continue;
			}
			final double distance = Math.abs(Math.log((double) candidate / currentUnitPrice));
			if (distance < bestDistance)
			{
				bestDistance = distance;
				best = candidate;
			}
		}
		return (int) best;
	}

	/** How far a bot will haggle from its own quoted price before it refuses: 15% either way. */
	public static final double COUNTER_HAGGLE_TOLERANCE = 0.15;

	/**
	 * The bot's negotiation policy: does it accept the player's counteroffer, or hold its price? This is a genuine
	 * business decision. The bot's own quoted price is the anchor: any counter that is better for the bot is always
	 * taken, and it will
	 * concede a small haggle up to {@link #COUNTER_HAGGLE_TOLERANCE} the wrong way, but a counter beyond that is
	 * refused so the bot keeps its price instead of the player unilaterally setting it by asking. Kept pure and
	 * deterministic so Java, not the model, owns accept/reject.
	 * @param counter the player's proposed unit price (from {@link #parseCounterOffer}); {@code <=0} means none
	 * @param offeredUnit the bot's current quoted unit price (its authoritative anchor); {@code <=0} means unknown
	 * @param selling {@code true} when the bot is the seller (a higher counter helps it), {@code false} when buyer
	 * @return {@code true} to accept the counter at that price, {@code false} to refuse and hold the quoted price
	 */
	public static boolean acceptsCounter(int counter, int offeredUnit, boolean selling)
	{
		if ((counter <= 0) || (offeredUnit <= 0))
		{
			return false;
		}
		if (selling)
		{
			// Bot sells: a counter at or above its ask is pure profit; below the ask it concedes down to the floor.
			final long floor = Math.round(offeredUnit * (1.0 - COUNTER_HAGGLE_TOLERANCE));
			return counter >= floor;
		}
		// Bot buys: a counter at or below its ask is pure profit; above the ask it concedes up to the ceiling.
		final long ceiling = Math.round(offeredUnit * (1.0 + COUNTER_HAGGLE_TOLERANCE));
		return counter <= ceiling;
	}

	// ===== Player-intent predicates for model action tags (FPC-044 / FPC-046) =====
	// A model action tag ([[DISBAND]], [[FOLLOW]], [[STAY]], ...) is a proposal; Java authorizes it against the
	// player's actual message before executing. These pure predicates are shared by the party and buddy managers so
	// the same "did the player actually order this?" logic is used - and unit-tested - in both places.

	private static boolean containsAnyLower(String low, String... needles)
	{
		for (String needle : needles)
		{
			if (low.contains(needle))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * {@code true} when the line is an explicit order for a companion to leave the party/service now. Negated or
	 * keep-in-party phrasings ("don't leave", "stay", "keep you") return {@code false}, so a hallucinated or negated
	 * [[DISBAND]] cannot release the companion against the player's wishes.
	 * @param text the player's latest message
	 * @return {@code true} to allow a dismiss, {@code false} to hold
	 */
	public static boolean isDismissOrder(String text)
	{
		if (text == null)
		{
			return false;
		}
		final String low = text.toLowerCase();
		if (containsAnyLower(low, "don't", "dont", "do not", "never", "no need", "stay", "keep you", "keep him", "keep her"))
		{
			return false;
		}
		return containsAnyLower(low, "disband", "leave the party", "leave party", "you can go", "you can leave", "dismiss", "you're free to go", "youre free to go", "ur free to go", "part ways", "we're done", "were done", "head out", "you can head");
	}

	/**
	 * {@code true} when the player's message tells a companion NOT to follow (or to hold position instead). Used to
	 * veto a [[FOLLOW]] tag the model emitted against a prohibition, without requiring a positive keyword for the
	 * common case (so a legitimate paraphrased "come on then" still lets the follow through).
	 * @param text the player's latest message
	 * @return {@code true} when following is negated this turn
	 */
	public static boolean negatesFollow(String text)
	{
		if (text == null)
		{
			return false;
		}
		final String low = text.toLowerCase();
		return containsAnyLower(low, "don't follow", "dont follow", "do not follow", "stop following", "stop follow", "quit following", "no need to follow", "stay here", "wait here", "hold position", "don't come", "dont come");
	}

	/**
	 * {@code true} when the player's message tells a companion NOT to stay (or to come along instead). Used to veto a
	 * [[STAY]] tag emitted against a prohibition.
	 * @param text the player's latest message
	 * @return {@code true} when staying is negated this turn
	 */
	public static boolean negatesStay(String text)
	{
		if (text == null)
		{
			return false;
		}
		final String low = text.toLowerCase();
		return containsAnyLower(low, "don't stay", "dont stay", "do not stay", "don't wait", "dont wait", "stop waiting", "follow me", "come with", "come along", "with me");
	}

	// Short affirmations count only as a whole word, so "ok" does not match inside "broke" and a meet-place word like
	// "gk" does not match inside a longer token. Multi-word cues are matched as substrings.
	private static final Set<String> _ACCEPT_WORDS = Set.of("ok", "okay", "okey", "yes", "yep", "yeah", "yup", "sure", "fine", "deal", "sold", "agreed", "agree", "coming", "omw", "gk", "gatekeeper", "warehouse", "wh");

	/**
	 * {@code true} when the player's latest message reads like agreement to close or meet on a trade: a clear
	 * affirmation ("ok", "deal", "sure"), an "I'll take/buy/sell it" phrase, "meet me"/"on my way", or naming a
	 * meeting place ("gk"). A message that calls the deal off (see {@link #isDealCancel}) or carries no such cue
	 * returns {@code false}, so a model [[SHOP]]/[[MEET]] tag cannot commit the deal on its own (FPC-060). This gates
	 * the state transition only; the item, side and price stay Java-owned.
	 * @param text the player's latest message
	 * @return {@code true} when the player's message supports committing/meeting this turn
	 */
	public static boolean isDealAccept(String text)
	{
		if (text == null)
		{
			return false;
		}
		final String low = text.toLowerCase(Locale.ROOT);
		if (isDealCancel(low))
		{
			return false;
		}
		if (containsAnyLower(low, "i'll take", "ill take", "take it", "i'll buy", "ill buy", "i'll sell", "ill sell", "sounds good", "works for me", "that works", "let's do it", "lets do it", "let's meet", "lets meet", "meet me", "on my way", "i'm coming", "im coming", "come to", "meet at", "see you at"))
		{
			return true;
		}
		for (String token : low.split("[^a-z]+"))
		{
			if (_ACCEPT_WORDS.contains(token))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * {@code true} when the player's latest message calls the trade off or clearly declines to proceed ("cancel",
	 * "forget it", "not interested", "no deal", "not coming"). Deliberately conservative and phrase-based, so an
	 * ambiguous line does not tear down a live deal: a model [[MEET:cancel]] tag runs only when both a deal exists
	 * and this returns true (FPC-061), and it also vetoes a false {@link #isDealAccept}.
	 * @param text the player's latest message
	 * @return {@code true} when the player's message expresses calling the deal off
	 */
	public static boolean isDealCancel(String text)
	{
		if (text == null)
		{
			return false;
		}
		final String low = text.toLowerCase(Locale.ROOT);
		return containsAnyLower(low, "cancel", "forget it", "forget the deal", "nvm", "never mind", "nevermind", "not interested", "no thanks", "no thank", "no deal", "not coming", "won't come", "wont come", "not gonna", "changed my mind", "change my mind", "call it off", "called off", "call off", "drop it", "not anymore", "no longer", "forget about it");
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

	/**
	 * @return the count number immediately before position {@code pos} in the text (e.g. "2 dd" -&gt; 2), else 1.
	 *         A number that is itself preceded by a level keyword ("lvl 80 pp", "level 80 pp") is that recruit's
	 *         LEVEL, not how many to spawn, so it is NOT read as a count and this returns 1.
	 */
	public static int countBefore(CharSequence text, int pos)
	{
		int i = pos - 1;
		while ((i >= 0) && (text.charAt(i) == ' '))
		{
			i--;
		}
		final int end = i;
		while ((i >= 0) && Character.isDigit(text.charAt(i)))
		{
			i--;
		}
		if (i < end)
		{
			// "lvl 80 pp" / "level 80 pp": the number is the requested level, not a count of pp. Don't treat it as one.
			if (precededByLevelKeyword(text, i))
			{
				return 1;
			}
			return clampRecruitCount(text.subSequence(i + 1, end + 1).toString());
		}
		return 1;
	}

	/**
	 * Parse a recruit party-slot count into the valid 1..6 range. A party holds at most six, and the digit token
	 * is regex-matched with no length bound, so an oversized value clamps to 6 and a malformed one falls back to 1
	 * rather than throwing {@link NumberFormatException} on a very long number (FPC-004).
	 * @param digits the matched digit token
	 * @return a slot count clamped to 1..6
	 */
	private static int clampRecruitCount(String digits)
	{
		// Any 2+ digit count is already past the six-slot cap, so clamp without parsing a wide (throw-prone) value.
		if (digits.length() > 1)
		{
			return 6;
		}
		try
		{
			return Math.max(1, Math.min(6, Integer.parseInt(digits)));
		}
		catch (NumberFormatException e)
		{
			return 1;
		}
	}

	/**
	 * @param text the full call text
	 * @param idx the index just before the digit run (the character here is a space or the last letter of the
	 *            preceding word, or -1 at the start of the text)
	 * @return {@code true} if the word immediately before the digits (skipping any spaces) is a level keyword
	 *         ("lvl", "level" or "lv"), which marks the digits as a level rather than a recruit count
	 */
	private static boolean precededByLevelKeyword(CharSequence text, int idx)
	{
		int i = idx;
		while ((i >= 0) && (text.charAt(i) == ' '))
		{
			i--;
		}
		final int wordEnd = i;
		while ((i >= 0) && Character.isLetter(text.charAt(i)))
		{
			i--;
		}
		if (i >= wordEnd)
		{
			return false; // no letters immediately before the number
		}
		final String word = text.subSequence(i + 1, wordEnd + 1).toString().toLowerCase();
		return word.equals("lvl") || word.equals("level") || word.equals("lv");
	}

	// Race adjectives a recruiter can put in front of a role ("elf archer", "dark elf tank"). Kept as bare strings
	// here (this class stays game-type-free); the manager maps them to the game's Race enum. "dark" is handled
	// specially in the loop so the two-word "dark elf" resolves to DARK_ELF rather than being read as plain Elf.
	private static final java.util.Set<String> RACE_TOKENS = java.util.Set.of("human", "elf", "elven", "orc", "orcish", "dwarf", "dwarven", "de", "delf", "darkelf");

	/**
	 * A "N x &lt;token&gt;" recruit request parsed from a party call, before the token is resolved to a role/class,
	 * plus an optional race adjective that preceded it ({@code null} = none; "dark_elf" for a two-word "dark elf").
	 */
	public static final class RoleRequest
	{
		public final String token;
		public final int count;
		public final String race;

		public RoleRequest(String token, int count)
		{
			this(token, count, null);
		}

		public RoleRequest(String token, int count, String race)
		{
			this.token = token;
			this.count = count;
			this.race = race;
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
		String pendingRace = null; // a race adjective attaches to the next role/class word ("elf" -> "elf archer")
		boolean levelToken = false; // the number right after "lvl"/"level"/"lv" is a level, not a count
		boolean sawDark = false; // "dark" seen; if "elf"/"elven" follows it is a DARK_ELF, else "dark" is ignored
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
				pendingCount = clampRecruitCount(token);
				continue;
			}
			levelToken = false;
			if (token.equals("dark"))
			{
				sawDark = true; // wait to see if "elf" follows
				continue;
			}
			if (RACE_TOKENS.contains(token))
			{
				pendingRace = (sawDark && (token.equals("elf") || token.equals("elven"))) ? "dark_elf" : token;
				sawDark = false;
				continue; // a race word is a modifier, not a recruit on its own
			}
			sawDark = false;
			requests.add(new RoleRequest(token, pendingCount, pendingRace));
			pendingCount = 1; // a number/race only applies to the class/role word right after it
			pendingRace = null;
		}
		return requests;
	}

	/**
	 * Optimal string alignment (Damerau-Levenshtein) distance between two lowercase tokens: insert, delete,
	 * substitute and adjacent transposition each cost 1. Used to forgive a single typo in a recruited class/role
	 * token, so "warcyer" -&gt; "warcryer" and "bishpo" -&gt; "bishop" still resolve.
	 * @return the edit distance, or {@link Integer#MAX_VALUE} if either input is {@code null}
	 */
	public static int editDistance(String a, String b)
	{
		if ((a == null) || (b == null))
		{
			return Integer.MAX_VALUE;
		}
		final int la = a.length();
		final int lb = b.length();
		if (la == 0)
		{
			return lb;
		}
		if (lb == 0)
		{
			return la;
		}
		int[] prevPrev = new int[lb + 1];
		int[] prev = new int[lb + 1];
		int[] curr = new int[lb + 1];
		for (int j = 0; j <= lb; j++)
		{
			prev[j] = j;
		}
		for (int i = 1; i <= la; i++)
		{
			curr[0] = i;
			final char ca = a.charAt(i - 1);
			for (int j = 1; j <= lb; j++)
			{
				final char cb = b.charAt(j - 1);
				final int cost = (ca == cb) ? 0 : 1;
				int v = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
				if ((i > 1) && (j > 1) && (ca == b.charAt(j - 2)) && (a.charAt(i - 2) == cb))
				{
					v = Math.min(v, prevPrev[j - 2] + 1); // adjacent transposition ("bishpo" <-> "bishop")
				}
				curr[j] = v;
			}
			final int[] tmp = prevPrev;
			prevPrev = prev;
			prev = curr;
			curr = tmp;
		}
		return prev[lb];
	}

	/**
	 * The edit-distance budget that still counts as "the same word" for a token of this length: exact for very
	 * short tokens (so 2-3 letter aliases like "pp"/"dd" are never fuzzy-matched to something else), one typo for
	 * normal words, two for long class names.
	 */
	public static int fuzzyBudget(int tokenLength)
	{
		if (tokenLength <= 3)
		{
			return 0;
		}
		if (tokenLength <= 7)
		{
			return 1;
		}
		return 2;
	}

	/** @return the smallest edit distance from {@code token} to any candidate, or {@link Integer#MAX_VALUE} if none. */
	public static int minDistance(String token, Iterable<String> candidates)
	{
		int best = Integer.MAX_VALUE;
		if (candidates != null)
		{
			for (String c : candidates)
			{
				final int d = editDistance(token, c);
				if (d < best)
				{
					best = d;
				}
			}
		}
		return best;
	}

	/**
	 * The single closest candidate to {@code token}, if it is within {@code maxDistance} AND unambiguous - a lone
	 * best. A tie between two different candidates returns {@code null} so the caller asks rather than guessing the
	 * wrong class.
	 * @return the closest candidate, or {@code null} if none is within budget or the best is a tie
	 */
	public static String nearestWithin(String token, Iterable<String> candidates, int maxDistance)
	{
		if ((token == null) || (candidates == null) || (maxDistance < 0))
		{
			return null;
		}
		String best = null;
		int bestDist = Integer.MAX_VALUE;
		boolean tie = false;
		for (String c : candidates)
		{
			final int d = editDistance(token, c);
			if (d < bestDist)
			{
				bestDist = d;
				best = c;
				tie = false;
			}
			else if ((d == bestDist) && !c.equals(best))
			{
				tie = true;
			}
		}
		return ((best == null) || (bestDist > maxDistance) || tie) ? null : best;
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
