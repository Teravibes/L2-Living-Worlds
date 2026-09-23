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

import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.managers.PhantomPvpManager;

/**
 * Standalone (no JUnit, no game server) regression harness for the pure decision functions of
 * {@link PhantomPvpManager} - the phantom PvP decision layer. The stand-or-flee math and the flee-threshold
 * shaping (bravery and level gap) take all their inputs as parameters, so they are verified deterministically
 * without a live world. The config-gating methods are also checked against the (unloaded) config defaults.
 *
 * <p>Run from the project root ("L2J_Mobius_CT_0_Interlude github"):
 * <pre>
 *   javac -d build/test-classes \
 *         "java/org/l2jmobius/commons/util/Rnd.java" \
 *         "java/org/l2jmobius/commons/time/TimeUtil.java" \
 *         "java/org/l2jmobius/commons/util/ConfigReader.java" \
 *         "java/org/l2jmobius/gameserver/config/custom/FakePlayersConfig.java" \
 *         "java/org/l2jmobius/gameserver/managers/PhantomPvpManager.java" \
 *         "tests/java/PhantomPvpManagerTest.java"
 *   java -cp build/test-classes PhantomPvpManagerTest
 * </pre>
 * Exit code is 0 when every check passes, 1 otherwise.
 */
public class PhantomPvpManagerTest
{
	private static int checks = 0;
	private static int failures = 0;

	// The design's default base flee threshold (docs/PVP_SYSTEM_DESIGN.md / FakePlayers.ini).
	private static final int BASE = 30;
	private static final int NEUTRAL = 50; // bravery midpoint that leaves the threshold unshifted

	public static void main(String[] args)
	{
		testFleeThresholdBravery();
		testFleeThresholdLevelGap();
		testFleeThresholdClamped();
		testShouldFlee();
		testHopelessLevelGapAlwaysFlees();
		testGatingOffByDefault();
		testGatingMasterSwitch();
		testPersonalityRollsInRange();
		testInitiateLevelBand();
		testReactRollBoundaries();
		testSizingEnrichment();
		testOpponentKind();
		testDriverGateComposition();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	/** Neutral bravery leaves the base unchanged; a timid phantom flees sooner (higher threshold), a brave one later. */
	private static void testFleeThresholdBravery()
	{
		final int neutral = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 40);
		eq(BASE, neutral, "neutral bravery, equal levels -> base threshold");

		final int timid = PhantomPvpManager.effectiveFleeHpPercent(BASE, 0, 40, 40);
		final int brave = PhantomPvpManager.effectiveFleeHpPercent(BASE, 100, 40, 40);
		gt(timid, neutral, "timid (bravery 0) flees sooner than neutral");
		lt(brave, neutral, "brave (bravery 100) flees later than neutral");
		// The swing is symmetric around neutral (both +/- BRAVERY_SWING = 15).
		eq(BASE + PhantomPvpManager.BRAVERY_SWING, timid, "timid threshold is base + full bravery swing");
		eq(BASE - PhantomPvpManager.BRAVERY_SWING, brave, "brave threshold is base - full bravery swing");
	}

	/** Facing a higher-level enemy raises the threshold (flee sooner); out-leveling the enemy lowers it. */
	private static void testFleeThresholdLevelGap()
	{
		final int even = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 40);
		final int outmatched = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 45); // enemy 5 higher
		final int dominant = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 45, 40); // phantom 5 higher
		gt(outmatched, even, "enemy 5 levels higher -> flee sooner");
		lt(dominant, even, "phantom 5 levels higher -> flee later");
		eq(BASE + (5 * PhantomPvpManager.LEVEL_GAP_HP_WEIGHT), outmatched, "level term is gap * weight");
	}

	/** The result is always clamped into a sane HP percentage band. */
	private static void testFleeThresholdClamped()
	{
		// Hugely outmatched, timid, high base: still capped at the ceiling.
		final int high = PhantomPvpManager.effectiveFleeHpPercent(85, 0, 20, 60);
		le(high, PhantomPvpManager.FLEE_HP_CEIL, "threshold never exceeds the ceiling");
		// Dominant, brave, low base: still at least the floor.
		final int low = PhantomPvpManager.effectiveFleeHpPercent(5, 100, 60, 20);
		ge(low, PhantomPvpManager.FLEE_HP_FLOOR, "threshold never drops below the floor");
	}

	/** shouldFlee is exactly "current HP at or below the effective threshold" (for a non-hopeless gap). */
	private static void testShouldFlee()
	{
		final int threshold = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 40); // == 30
		eqBool(true, PhantomPvpManager.shouldFlee(threshold, BASE, NEUTRAL, 40, 40), "HP == threshold -> flee");
		eqBool(true, PhantomPvpManager.shouldFlee(threshold - 1, BASE, NEUTRAL, 40, 40), "HP below threshold -> flee");
		eqBool(false, PhantomPvpManager.shouldFlee(threshold + 1, BASE, NEUTRAL, 40, 40), "HP above threshold -> stand");
		eqBool(false, PhantomPvpManager.shouldFlee(100, BASE, NEUTRAL, 40, 40), "full HP, even fight -> stand");
	}

	/** A hopeless level gap makes a phantom flee at any HP, even full. */
	private static void testHopelessLevelGapAlwaysFlees()
	{
		final int self = 40;
		final int hopelessEnemy = self + PhantomPvpManager.HOPELESS_LEVEL_GAP; // exactly the hopeless gap
		eqBool(true, PhantomPvpManager.shouldFlee(100, BASE, 100, self, hopelessEnemy), "hopeless gap at full HP + max bravery -> flee");
		eqBool(false, PhantomPvpManager.shouldFlee(100, BASE, NEUTRAL, self, hopelessEnemy - 1), "one below the hopeless gap, full HP -> stand");
	}

	/** Initiation level band: never stomp a target far below, never start on one hopelessly above; self-defense is unaffected. */
	private static void testInitiateLevelBand()
	{
		final int max = 6; // PhantomPvpMaxLevelGapAbovePlayer default
		eqBool(true, PhantomPvpManager.mayInitiateByLevel(40, 40, max), "even levels -> may initiate");
		eqBool(true, PhantomPvpManager.mayInitiateByLevel(46, 40, max), "exactly max levels above -> may initiate");
		eqBool(false, PhantomPvpManager.mayInitiateByLevel(47, 40, max), "one over the max above -> no stomp");
		eqBool(false, PhantomPvpManager.mayInitiateByLevel(50, 40, max), "far above the target -> no stomp");
		final int hopeless = PhantomPvpManager.HOPELESS_LEVEL_GAP;
		eqBool(false, PhantomPvpManager.mayInitiateByLevel(40, 40 + hopeless, max), "target hopelessly above -> do not start");
		eqBool(true, PhantomPvpManager.mayInitiateByLevel(40, (40 + hopeless) - 1, max), "one below the hopeless gap -> may initiate");
	}

	/** The react roll is a pure percentage of the configured chance: 0 never engages, 100 always does. */
	private static void testReactRollBoundaries()
	{
		FakePlayersConfig.PHANTOM_PVP_REACT_CHANCE_PERCENT = 0;
		boolean anyAtZero = false;
		for (int i = 0; i < 500; i++)
		{
			if (PhantomPvpManager.rollReactEngage())
			{
				anyAtZero = true;
			}
		}
		eqBool(false, anyAtZero, "react chance 0 never engages");

		FakePlayersConfig.PHANTOM_PVP_REACT_CHANCE_PERCENT = 100;
		boolean allAtFull = true;
		for (int i = 0; i < 500; i++)
		{
			if (!PhantomPvpManager.rollReactEngage())
			{
				allAtFull = false;
			}
		}
		eqBool(true, allAtFull, "react chance 100 always engages");

		FakePlayersConfig.PHANTOM_PVP_REACT_CHANCE_PERCENT = 0; // restore the unloaded default
	}

	/** Phase 2 sizing: outnumbering the enemy lets a phantom stand longer; being outnumbered or out of mana flees sooner. */
	private static void testSizingEnrichment()
	{
		// Ally advantage lowers the threshold (stand longer); disadvantage raises it (flee sooner). Base 30, even fight.
		final int outnumber = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 40, 3, false);
		final int outnumbered = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 40, -3, false);
		lt(outnumber, BASE, "outnumbering the enemy -> flee later (lower threshold)");
		gt(outnumbered, BASE, "being outnumbered -> flee sooner (higher threshold)");
		eq(BASE - (3 * PhantomPvpManager.ALLY_ADVANTAGE_HP_WEIGHT), outnumber, "ally term is advantage * weight");
		eq(BASE + (3 * PhantomPvpManager.ALLY_ADVANTAGE_HP_WEIGHT), outnumbered, "disadvantage term is symmetric");

		// The ally term is clamped, so a huge advantage cannot swing the threshold without limit.
		final int huge = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 40, 100, false);
		eq(BASE - PhantomPvpManager.ALLY_ADVANTAGE_HP_MAX, huge, "ally advantage is clamped to the max");

		// An out-of-mana caster flees sooner by the full penalty.
		final int lowMp = PhantomPvpManager.effectiveFleeHpPercent(BASE, NEUTRAL, 40, 40, 0, true);
		eq(BASE + PhantomPvpManager.CASTER_LOW_MP_PENALTY, lowMp, "out-of-mana caster adds the full penalty");

		// shouldFlee honors the situational inputs.
		eqBool(false, PhantomPvpManager.shouldFlee(BASE, BASE, NEUTRAL, 40, 40, 3, false), "outnumbering, HP at base -> stand");
		eqBool(true, PhantomPvpManager.shouldFlee(BASE, BASE, NEUTRAL, 40, 40, -3, false), "outnumbered, HP at base -> flee");
		eqBool(true, PhantomPvpManager.shouldFlee(BASE + 10, BASE, NEUTRAL, 40, 40, 0, true), "out-of-mana caster flees above the base HP");
	}

	/** The phantom-versus-phantom participant gate: a real player is always allowed; a phantom only when enabled. */
	private static void testOpponentKind()
	{
		eqBool(true, PhantomPvpManager.opponentKindAllowed(false, false), "real player opponent always allowed (between off)");
		eqBool(true, PhantomPvpManager.opponentKindAllowed(false, true), "real player opponent always allowed (between on)");
		eqBool(true, PhantomPvpManager.opponentKindAllowed(true, true), "phantom opponent allowed when between-phantoms on");
		eqBool(false, PhantomPvpManager.opponentKindAllowed(true, false), "phantom opponent blocked when between-phantoms off");
	}

	/**
	 * The PvP driver must run whenever the master switch is on, even if self-defense is off, because it also drives
	 * react-to-flagged and party-defense engagements. This pins the gate composition the pvpCombat tick depends on.
	 */
	private static void testDriverGateComposition()
	{
		FakePlayersConfig.PHANTOM_PVP_ENABLED = true;
		FakePlayersConfig.PHANTOM_PVP_SELF_DEFENSE = false;
		FakePlayersConfig.PHANTOM_PVP_REACT_TO_FLAGGED = true;
		FakePlayersConfig.PHANTOM_PVP_PARTY_DEFENSE = true;
		eqBool(true, PhantomPvpManager.pvpEnabled(), "master on -> driver runs even with self-defense off");
		eqBool(false, PhantomPvpManager.selfDefenseEnabled(), "self-defense reports off");
		eqBool(true, PhantomPvpManager.reactToFlaggedEnabled(), "react-to-flagged still on with self-defense off");
		eqBool(true, PhantomPvpManager.partyDefenseEnabled(), "party defense still on with self-defense off");
		// Restore the unloaded defaults so test ordering does not matter.
		FakePlayersConfig.PHANTOM_PVP_ENABLED = false;
		FakePlayersConfig.PHANTOM_PVP_SELF_DEFENSE = false;
		FakePlayersConfig.PHANTOM_PVP_REACT_TO_FLAGGED = false;
		FakePlayersConfig.PHANTOM_PVP_PARTY_DEFENSE = false;
	}

	/** With config never loaded, all statics are false/0, so every gate is off (safe default). */
	private static void testGatingOffByDefault()
	{
		eqBool(false, PhantomPvpManager.pvpEnabled(), "master switch off by default");
		eqBool(false, PhantomPvpManager.selfDefenseEnabled(), "self-defense off while master off");
		eqBool(false, PhantomPvpManager.reactToFlaggedEnabled(), "flag reaction off while master off");
		eqBool(false, PhantomPvpManager.duelsEnabled(), "duels off while master off");
		eqBool(false, PhantomPvpManager.partyDefenseEnabled(), "party defense off while master off");
		eqBool(false, PhantomPvpManager.betweenPhantomsEnabled(), "phantom-vs-phantom off while master off");
		eqBool(false, PhantomPvpManager.openWorldGankEnabled(), "ganking off while master off");
	}

	/** Every behavior gate is AND-ed with the master switch: master off forces the behavior off even when its own flag is on. */
	private static void testGatingMasterSwitch()
	{
		FakePlayersConfig.PHANTOM_PVP_ENABLED = false;
		FakePlayersConfig.PHANTOM_PVP_SELF_DEFENSE = true;
		eqBool(false, PhantomPvpManager.selfDefenseEnabled(), "self-defense flag on but master off -> still off");

		FakePlayersConfig.PHANTOM_PVP_ENABLED = true;
		eqBool(true, PhantomPvpManager.selfDefenseEnabled(), "both master and self-defense on -> on");

		FakePlayersConfig.PHANTOM_PVP_SELF_DEFENSE = false;
		eqBool(false, PhantomPvpManager.selfDefenseEnabled(), "master on but self-defense flag off -> off");

		// Restore the unloaded defaults so ordering of tests does not matter.
		FakePlayersConfig.PHANTOM_PVP_ENABLED = false;
		FakePlayersConfig.PHANTOM_PVP_SELF_DEFENSE = false;
	}

	/** Personality rolls stay inside their declared ranges across many samples. */
	private static void testPersonalityRollsInRange()
	{
		FakePlayersConfig.PHANTOM_PVP_AGGRESSOR_PERCENT = 15;
		boolean allBraveryInRange = true;
		int aggressorCount = 0;
		final int samples = 2000;
		for (int i = 0; i < samples; i++)
		{
			final int bravery = PhantomPvpManager.rollBravery();
			if ((bravery < PhantomPvpManager.BRAVERY_MIN) || (bravery > PhantomPvpManager.BRAVERY_MAX))
			{
				allBraveryInRange = false;
			}
			if (PhantomPvpManager.rollAggressor())
			{
				aggressorCount++;
			}
		}
		eqBool(true, allBraveryInRange, "every bravery roll is within [MIN, MAX]");
		// With a 15% aggressor rate over 2000 samples, the count should land well inside a wide sanity band.
		final int pct = (aggressorCount * 100) / samples;
		eqBool(true, (pct >= 5) && (pct <= 30), "aggressor share near 15% (got " + pct + "%)");

		// A 0% rate must never roll an aggressor; a 100% rate must always roll one.
		FakePlayersConfig.PHANTOM_PVP_AGGRESSOR_PERCENT = 0;
		boolean anyAtZero = false;
		for (int i = 0; i < 500; i++)
		{
			anyAtZero |= PhantomPvpManager.rollAggressor();
		}
		eqBool(false, anyAtZero, "0% aggressor rate never rolls an aggressor");

		FakePlayersConfig.PHANTOM_PVP_AGGRESSOR_PERCENT = 100;
		boolean allAtHundred = true;
		for (int i = 0; i < 500; i++)
		{
			allAtHundred &= PhantomPvpManager.rollAggressor();
		}
		eqBool(true, allAtHundred, "100% aggressor rate always rolls an aggressor");

		FakePlayersConfig.PHANTOM_PVP_AGGRESSOR_PERCENT = 0; // restore
	}

	// ===== tiny assertion helpers =====

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

	private static void gt(int a, int b, String what)
	{
		checks++;
		if (a <= b)
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + a + "] > [" + b + "]");
		}
	}

	private static void lt(int a, int b, String what)
	{
		checks++;
		if (a >= b)
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + a + "] < [" + b + "]");
		}
	}

	private static void ge(int a, int b, String what)
	{
		checks++;
		if (a < b)
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + a + "] >= [" + b + "]");
		}
	}

	private static void le(int a, int b, String what)
	{
		checks++;
		if (a > b)
		{
			failures++;
			System.out.println("FAIL: " + what + " -> expected [" + a + "] <= [" + b + "]");
		}
	}
}
