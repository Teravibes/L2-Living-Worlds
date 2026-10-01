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

import org.l2jmobius.gameserver.managers.PhantomOlympiadRules;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link PhantomOlympiadRules}, the pure decision rules
 * of the phantom Olympiad roster: roster class spread, level band, point minimums, sign-up gating, pool caps and the
 * party hold across a match.
 *
 * <p>Run from the project root ("L2J_Mobius_CT_0_Interlude github"):
 * <pre>
 *   javac -d build/test-classes \
 *         "java/org/l2jmobius/commons/util/Rnd.java" \
 *         "java/org/l2jmobius/gameserver/managers/PhantomOlympiadRules.java" \
 *         "tests/java/PhantomOlympiadRulesTest.java"
 *   java -cp build/test-classes PhantomOlympiadRulesTest
 * </pre>
 * Exit code is 0 when every check passes, 1 otherwise.
 */
public class PhantomOlympiadRulesTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		testThirdClasses();
		testNextBaseClass();
		testLevelBand();
		testMissing();
		testPoints();
		testIsFree();
		testShouldSignUp();
		testPoolCap();
		testHoldsParty();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	/** Interlude third classes are ids 88 (Duelist) to 118 (Maestro): 31 classes. */
	private static void testThirdClasses()
	{
		eq(31, PhantomOlympiadRules.THIRD_CLASS_COUNT, "31 third classes");
		eqBool(true, PhantomOlympiadRules.isThirdClass(88), "Duelist is third class");
		eqBool(true, PhantomOlympiadRules.isThirdClass(118), "Maestro is third class");
		eqBool(false, PhantomOlympiadRules.isThirdClass(87), "87 is not");
		eqBool(false, PhantomOlympiadRules.isThirdClass(119), "119 is not");
		eqBool(false, PhantomOlympiadRules.isThirdClass(2), "Gladiator (2nd class) is not");
	}

	/**
	 * The base roster is its configured size spread over the 22 roster classes (support classes and summoners are
	 * left out); rivals never take another class's base slot.
	 */
	private static void testNextBaseClass()
	{
		eq(22, PhantomOlympiadRules.FIGHTING_CLASS_COUNT, "31 third classes minus 6 support and 3 summoners");
		eqBool(true, PhantomOlympiadRules.isRosterClass(88), "Duelist is a roster class");
		eqBool(true, PhantomOlympiadRules.isRosterClass(118), "Maestro is a roster class");
		eqBool(false, PhantomOlympiadRules.isRosterClass(112), "Shillien Saint is left out");
		eqBool(false, PhantomOlympiadRules.isRosterClass(97), "Cardinal is left out");
		eqBool(false, PhantomOlympiadRules.isRosterClass(116), "Doomcryer is left out");
		eqBool(false, PhantomOlympiadRules.isRosterClass(96), "Arcana Lord is left out");
		eqBool(false, PhantomOlympiadRules.isRosterClass(111), "Spectral Master is left out");
		eqBool(false, PhantomOlympiadRules.isRosterClass(2), "a 2nd class is not a roster class");

		eq(2, PhantomOlympiadRules.baseQuota(0, 40), "40: Duelist gets 2");
		eq(2, PhantomOlympiadRules.baseQuota(17, 40), "40: the 18th roster class gets 2");
		eq(1, PhantomOlympiadRules.baseQuota(18, 40), "40: the 19th roster class gets 1");
		eq(0, PhantomOlympiadRules.baseQuota(0, 0), "size 0: no quota");
		eq(3, PhantomOlympiadRules.baseQuota(21, 66), "66 = 3 per roster class");

		final int[] counts = new int[PhantomOlympiadRules.THIRD_CLASS_COUNT];
		eq(88, PhantomOlympiadRules.nextBaseClass(counts, 40), "empty roster starts at Duelist");
		eq(0, PhantomOlympiadRules.nextBaseClass(counts, 0), "size 0: nothing to create");

		// Build a 40-noble base roster from nothing.
		int created = 0;
		for (int classId; (classId = PhantomOlympiadRules.nextBaseClass(counts, 40)) != 0; created++)
		{
			eqBool(true, PhantomOlympiadRules.isRosterClass(classId), "only roster classes are created (" + classId + ")");
			counts[classId - PhantomOlympiadRules.FIRST_THIRD_CLASS_ID]++;
		}
		eq(40, created, "exactly 40 base nobles are created");
		int ones = 0;
		int twos = 0;
		for (int count : counts)
		{
			if (count == 1)
			{
				ones++;
			}
			else if (count == 2)
			{
				twos++;
			}
		}
		eq(4, ones, "40 nobles: 4 classes with one noble");
		eq(18, twos, "40 nobles: 18 classes with two");

		// Rivals added first: six Adventurers (93) for a player, then the base roster is built.
		final int[] withRivals = new int[PhantomOlympiadRules.THIRD_CLASS_COUNT];
		withRivals[93 - PhantomOlympiadRules.FIRST_THIRD_CLASS_ID] = 6;
		created = 0;
		for (int classId; (classId = PhantomOlympiadRules.nextBaseClass(withRivals, 40)) != 0; created++)
		{
			eqBool(true, classId != 93, "rivals already cover Adventurer's base slots");
			withRivals[classId - PhantomOlympiadRules.FIRST_THIRD_CLASS_ID]++;
		}
		eq(38, created, "the other 21 roster classes still get their 38 base slots");
		int missingClasses = 0;
		for (int i = 0; i < withRivals.length; i++)
		{
			if (PhantomOlympiadRules.isRosterClass(PhantomOlympiadRules.FIRST_THIRD_CLASS_ID + i) && (withRivals[i] == 0))
			{
				missingClasses++;
			}
		}
		eq(0, missingClasses, "every roster class has a noble even with rivals on the roster");

		eq(101, PhantomOlympiadRules.nextBaseClass(new int[]
		{
			2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 0, 2, 2
		}, 40), "a short counts array reads the missing classes as 0 and skips 96-98 (first is 101)");
		eq(0, PhantomOlympiadRules.nextBaseClass(new int[]
		{
			5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5
		}, 40), "a roster above its size creates nothing more");
	}

	/** Levels are clamped to the third-class band 76-80, and min never exceeds max. */
	private static void testLevelBand()
	{
		band(76, 80, PhantomOlympiadRules.levelBand(76, 80), "defaults");
		band(76, 80, PhantomOlympiadRules.levelBand(1, 99), "out of range clamps");
		band(78, 78, PhantomOlympiadRules.levelBand(78, 78), "fixed level");
		band(79, 79, PhantomOlympiadRules.levelBand(79, 77), "max below min lifts max to min");
		for (int i = 0; i < 200; i++)
		{
			final int level = PhantomOlympiadRules.rollLevel(77, 79);
			if ((level < 77) || (level > 79))
			{
				eq(77, level, "rolled level inside 77-79");
				return;
			}
		}
		eqBool(true, true, "200 rolls stayed inside 77-79");
	}

	private static void testMissing()
	{
		eq(40, PhantomOlympiadRules.missing(0, 40), "empty roster misses all");
		eq(0, PhantomOlympiadRules.missing(45, 40), "never negative");
		eq(5, PhantomOlympiadRules.missing(1, 6), "rivals: one exists, five missing");
		eq(0, PhantomOlympiadRules.missing(3, 0), "0 wanted turns it off");
	}

	/** Stock minimums: 3 points for classed, 5 for non-classed. */
	private static void testPoints()
	{
		eqBool(true, PhantomOlympiadRules.hasPointsFor(true, 3), "classed at 3");
		eqBool(false, PhantomOlympiadRules.hasPointsFor(true, 2), "classed below 3");
		eqBool(true, PhantomOlympiadRules.hasPointsFor(false, 5), "non-classed at 5");
		eqBool(false, PhantomOlympiadRules.hasPointsFor(false, 4), "non-classed below 5");
	}

	private static void testIsFree()
	{
		eqBool(true, PhantomOlympiadRules.isFree(true, false, false, false, 1000, 0), "idle, never fought");
		eqBool(false, PhantomOlympiadRules.isFree(false, false, false, false, 1000, 0), "competition closed");
		eqBool(false, PhantomOlympiadRules.isFree(true, true, false, false, 1000, 0), "already waiting");
		eqBool(false, PhantomOlympiadRules.isFree(true, false, true, false, 1000, 0), "in a match");
		eqBool(false, PhantomOlympiadRules.isFree(true, false, false, true, 1000, 0), "dead");
		eqBool(false, PhantomOlympiadRules.isFree(true, false, false, false, 1000, 1001), "still resting");
		eqBool(true, PhantomOlympiadRules.isFree(true, false, false, false, 1001, 1001), "rest just over");
	}

	private static void testShouldSignUp()
	{
		eqBool(true, PhantomOlympiadRules.shouldSignUp(true, 0, 5, 0, 99), "player waiting: always, even at 0 percent");
		eqBool(false, PhantomOlympiadRules.shouldSignUp(true, 5, 5, 100, 0), "pool at cap: never");
		eqBool(true, PhantomOlympiadRules.shouldSignUp(false, 0, 10, 10, 9), "background roll under the chance");
		eqBool(false, PhantomOlympiadRules.shouldSignUp(false, 0, 10, 10, 10), "background roll at the chance fails");
		eqBool(false, PhantomOlympiadRules.shouldSignUp(false, 0, 10, 0, 0), "0 percent: only with a player");
		eqBool(true, PhantomOlympiadRules.shouldSignUp(false, 0, 10, 150, 99), "chance clamps to 100");
		eqBool(false, PhantomOlympiadRules.shouldSignUp(false, 0, 10, -5, 0), "negative chance clamps to 0");
	}

	/** Pools fill to their match size rounded up to even, so nobody (the player included) is left over. */
	private static void testPoolCap()
	{
		eq(10, PhantomOlympiadRules.poolCap(9, 0), "non-classed 9, phantoms only: 10");
		eq(9, PhantomOlympiadRules.poolCap(9, 1), "non-classed 9 with a player: 9 nobles + the player");
		eq(8, PhantomOlympiadRules.poolCap(9, 2), "non-classed 9 with two players: 8 nobles, pool of 10");
		eq(7, PhantomOlympiadRules.poolCap(9, 3), "non-classed 9 with three players: 7 nobles, pool of 10");
		eq(6, PhantomOlympiadRules.poolCap(5, 0), "classed 5, phantoms only: 6");
		eq(5, PhantomOlympiadRules.poolCap(5, 1), "classed 5 with a player: 5 nobles + the player");
		eq(4, PhantomOlympiadRules.poolCap(5, 2), "classed 5 with two players: 4 nobles, pool of 6");
		eq(4, PhantomOlympiadRules.poolCap(4, 0), "an even size stays");
		eq(3, PhantomOlympiadRules.poolCap(4, 1), "an even size with a player: one fewer");
		eq(2, PhantomOlympiadRules.poolCap(0, 0), "size floors at 2");
		eq(1, PhantomOlympiadRules.poolCap(1, 1), "size 1 floors at 2: one noble for the player");
		eq(1, PhantomOlympiadRules.poolCap(5, 7), "more players than the size: one noble evens the pool");
		eq(0, PhantomOlympiadRules.poolCap(5, 8), "an even crowd of players needs no nobles");
		eq(6, PhantomOlympiadRules.poolCap(5, -1), "a negative count reads as none");
	}

	/** Party members wait while the owner is in a match and for a short delay after it. */
	private static void testHoldsParty()
	{
		eqBool(true, PhantomOlympiadRules.holdsParty(true, 0, 1000, 3000), "in the match");
		eqBool(false, PhantomOlympiadRules.holdsParty(false, 0, 1000, 3000), "no recent match");
		eqBool(true, PhantomOlympiadRules.holdsParty(false, 1000, 3999, 3000), "just back: still held");
		eqBool(false, PhantomOlympiadRules.holdsParty(false, 1000, 4000, 3000), "delay over: rejoin");
	}

	private static void band(int min, int max, int[] actual, String what)
	{
		eq(min, actual[0], what + " (min)");
		eq(max, actual[1], what + " (max)");
	}

	private static void eq(int expected, int actual, String what)
	{
		checks++;
		if (expected != actual)
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + expected + "] but got [" + actual + "]");
		}
	}

	private static void eqBool(boolean expected, boolean actual, String what)
	{
		checks++;
		if (expected != actual)
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + expected + "] but got [" + actual + "]");
		}
	}
}
