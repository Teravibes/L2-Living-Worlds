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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.l2jmobius.gameserver.managers.FakePlayerNameFactory;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link FakePlayerNameFactory}.<br>
 * The point of interest is the <b>seeded</b> generator: stable "regular" identities depend on the same
 * seed always producing the same name across restarts, so a change to the syllable pools or the build
 * order (which would silently rename every regular) must trip a check here. The golden values below are
 * from {@code java.util.Random}, whose sequence is specified and stable across JDKs.
 *
 * <p>Run from the project root ("L2J_Mobius_CT_0_Interlude github"):
 * <pre>
 *   javac -d build/test-classes \
 *         "java/org/l2jmobius/commons/util/Rnd.java" \
 *         "java/org/l2jmobius/gameserver/managers/FakePlayerNameFactory.java" \
 *         "tests/java/FakePlayerNameFactoryTest.java"
 *   java -cp build/test-classes FakePlayerNameFactoryTest
 * </pre>
 * Exit code is 0 when every check passes, 1 otherwise.
 */
public class FakePlayerNameFactoryTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		testSeededIsDeterministic();
		testSeededGoldenValues();
		testSeededNameShape();
		testRandomNamesAreUniqueAndValid();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	private static void testSeededIsDeterministic()
	{
		// The whole reason this generator exists: same seed -> same name (stable identity across restarts).
		eq(FakePlayerNameFactory.generateName(new Random(42)), FakePlayerNameFactory.generateName(new Random(42)), "same seed yields the same name");
		truth(!FakePlayerNameFactory.generateName(new Random(1)).equals(FakePlayerNameFactory.generateName(new Random(2))), "different seeds yield different names");
	}

	private static void testSeededGoldenValues()
	{
		// Pin the exact composition so a pool reorder / build change that would rename regulars is caught.
		eq("Pyrdil", FakePlayerNameFactory.generateName(new Random(1)), "seed 1 -> Pyrdil");
		eq("Coror", FakePlayerNameFactory.generateName(new Random(2)), "seed 2 -> Coror");
		eq("Ulrok", FakePlayerNameFactory.generateName(new Random(42)), "seed 42 -> Ulrok");
		eq("Caddoth", FakePlayerNameFactory.generateName(new Random(123)), "seed 123 -> Caddoth");
		eq("Quorarus", FakePlayerNameFactory.generateName(new Random(7)), "seed 7 -> Quorarus");
	}

	private static void testSeededNameShape()
	{
		// Over a wide spread of seeds every name is client-legal: non-empty, <= 16 chars, letters only.
		for (int seed = 0; seed < 500; seed++)
		{
			final String name = FakePlayerNameFactory.generateName(new Random(seed));
			truth(!name.isEmpty(), "seed " + seed + " name is non-empty");
			truth(name.length() <= 16, "seed " + seed + " name within 16 chars");
			truth(name.matches("^\\p{L}+$"), "seed " + seed + " name is letters only (was: " + name + ")");
		}
	}

	private static void testRandomNamesAreUniqueAndValid()
	{
		// The random generator dedups via its own USED_NAMES set: a batch must be all-distinct and client-legal.
		final Set<String> seen = new HashSet<>();
		for (int i = 0; i < 300; i++)
		{
			final String name = FakePlayerNameFactory.generateName();
			truth(!name.isEmpty(), "random name is non-empty");
			truth(name.length() <= 16, "random name within 16 chars (was: " + name + ")");
			truth(name.matches("^\\p{L}+[0-9]*$"), "random name is letters (with optional numeric fallback suffix): " + name);
			truth(seen.add(name.toLowerCase()), "random name is unique: " + name);
		}
	}

	// ===== tiny assertion helpers =====

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
