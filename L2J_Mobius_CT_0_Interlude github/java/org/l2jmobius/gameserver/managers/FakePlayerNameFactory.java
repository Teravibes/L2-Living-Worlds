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

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.util.Rnd;

/**
 * Pronounceable fake-player name generation, extracted from {@link FakePlayerAppearanceFactory}.<br>
 * Two flavours share the same syllable pools:
 * <ul>
 * <li>{@link #generateName()} - a globally-unique random name (deduplicated via {@code USED_NAMES}).</li>
 * <li>{@link #generateName(Random)} - a <b>deterministic</b> name from a caller-seeded RNG: the same seed
 * always yields the same name. This is what stable "regular" identities rely on to survive a restart, so
 * its determinism is a real regression surface - hence it lives here, testable on a plain JDK (no game
 * classpath). See {@code tests/java/FakePlayerNameFactoryTest.java}.</li>
 * </ul>
 * Only the seeded generator is fully pure; the random one keeps process-global dedup state on purpose.
 */
public final class FakePlayerNameFactory
{
	// Syllable pools used to build pronounceable names; combined 2-3 at a time.
	private static final String[] NAME_START =
	{
		"Ael", "Bro", "Cad", "Dra", "Eil", "Fen", "Gor", "Hal", "Ith", "Jor",
		"Kel", "Lyr", "Mor", "Nyx", "Orin", "Pyr", "Quor", "Rha", "Syl", "Tor",
		"Ul", "Vae", "Wyn", "Xan", "Yor", "Zeph", "Ash", "Bel", "Cor", "Dûn"
	};
	private static final String[] NAME_MID =
	{
		"a", "e", "i", "o", "u", "ar", "en", "il", "or", "yn",
		"ael", "eth", "ith", "ond", "ura", "iel", "ven", "dor", "rim", "lan"
	};
	private static final String[] NAME_END =
	{
		"or", "an", "el", "is", "us", "yr", "wen", "dil", "rok", "tha",
		"mir", "ras", "nor", "vyn", "zar", "lin", "doth", "wyn", "gar", "eth"
	};

	private static final Set<String> USED_NAMES = ConcurrentHashMap.newKeySet();

	private FakePlayerNameFactory()
	{
		// Utility holder - not instantiable.
	}

	/**
	 * @return a unique pronounceable name (max 16 chars, as the client expects)
	 */
	public static String generateName()
	{
		for (int attempt = 0; attempt < 50; attempt++)
		{
			final StringBuilder sb = new StringBuilder(NAME_START[Rnd.get(NAME_START.length)]);
			if (Rnd.nextBoolean())
			{
				sb.append(NAME_MID[Rnd.get(NAME_MID.length)]);
			}
			sb.append(NAME_END[Rnd.get(NAME_END.length)]);

			String name = sb.toString();
			if (name.length() > 16)
			{
				name = name.substring(0, 16);
			}
			if (USED_NAMES.add(name.toLowerCase()))
			{
				return name;
			}
		}

		// Fallback: guarantee uniqueness with a numeric suffix.
		String name;
		do
		{
			name = NAME_START[Rnd.get(NAME_START.length)] + Rnd.get(1000);
		}
		while (!USED_NAMES.add(name.toLowerCase()));
		return name;
	}

	/**
	 * Deterministic name variant built from a caller-supplied seeded RNG, so the same seed always yields the
	 * same name. Used for stable "regular" identities that must be identical across restarts (unlike the
	 * random {@link #generateName()}). Does not touch {@code USED_NAMES}: the caller owns uniqueness/dedup.
	 * @param rng a seeded random source (same seed -&gt; same name)
	 * @return a syllable-built name (from the same pools as the random generator), capped at 16 chars
	 */
	public static String generateName(Random rng)
	{
		final StringBuilder sb = new StringBuilder(NAME_START[rng.nextInt(NAME_START.length)]);
		if (rng.nextBoolean())
		{
			sb.append(NAME_MID[rng.nextInt(NAME_MID.length)]);
		}
		sb.append(NAME_END[rng.nextInt(NAME_END.length)]);
		final String name = sb.toString();
		return name.length() > 16 ? name.substring(0, 16) : name;
	}
}
