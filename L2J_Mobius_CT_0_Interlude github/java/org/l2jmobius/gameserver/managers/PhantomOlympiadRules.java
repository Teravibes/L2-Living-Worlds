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

import org.l2jmobius.commons.util.Rnd;

/**
 * Pure decision rules for the phantom Olympiad roster ({@link PhantomOlympiadManager}). Everything here takes its
 * inputs as parameters and touches no game state, so it is unit tested without a server.
 */
public class PhantomOlympiadRules
{
	/** Interlude third classes are the contiguous class ids 88 (Duelist) to 118 (Maestro). */
	public static final int FIRST_THIRD_CLASS_ID = 88;
	public static final int LAST_THIRD_CLASS_ID = 118;
	public static final int THIRD_CLASS_COUNT = (LAST_THIRD_CLASS_ID - FIRST_THIRD_CLASS_ID) + 1;
	/**
	 * Third classes the roster never uses (owner's choice): the support classes, whose combat is a party healer's or
	 * buffer's, and the summoners, whose servitors a phantom does not control. Cardinal, Hierophant, Eva's Saint,
	 * Shillien Saint, Dominator, Doomcryer; Arcana Lord, Elemental Master, Spectral Master.
	 */
	private static final int[] EXCLUDED_CLASS_IDS =
	{
		97,
		98,
		105,
		112,
		115,
		116,
		96,
		104,
		111
	};
	/** How many third classes the roster fills. */
	public static final int FIGHTING_CLASS_COUNT = THIRD_CLASS_COUNT - EXCLUDED_CLASS_IDS.length;
	/** Third class starts at 76 and the Interlude cap is 80, so a roster noble is always in this band. */
	public static final int NOBLE_LEVEL_MIN = 76;
	public static final int NOBLE_LEVEL_MAX = 80;
	/** Stock Olympiad.registerNoble refuses a classed sign-up below 3 points and a non-classed one below 5. */
	public static final int CLASSED_MIN_POINTS = 3;
	public static final int NON_CLASSED_MIN_POINTS = 5;

	private PhantomOlympiadRules()
	{
	}

	/** @return {@code true} if {@code classId} is an Interlude third class. */
	public static boolean isThirdClass(int classId)
	{
		return (classId >= FIRST_THIRD_CLASS_ID) && (classId <= LAST_THIRD_CLASS_ID);
	}

	/** @return {@code true} if {@code classId} is a third class the phantom roster uses (not support, not summoner). */
	public static boolean isRosterClass(int classId)
	{
		if (!isThirdClass(classId))
		{
			return false;
		}
		for (int excluded : EXCLUDED_CLASS_IDS)
		{
			if (excluded == classId)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the level band {min, max} a new noble is created in: the configured values clamped to 76-80, with
	 *         min never above max
	 */
	public static int[] levelBand(int configMin, int configMax)
	{
		final int min = Math.max(NOBLE_LEVEL_MIN, Math.min(NOBLE_LEVEL_MAX, configMin));
		final int max = Math.max(min, Math.min(NOBLE_LEVEL_MAX, configMax));
		return new int[]
		{
			min,
			max
		};
	}

	/** @return a random level inside the clamped band (inclusive). */
	public static int rollLevel(int configMin, int configMax)
	{
		final int[] band = levelBand(configMin, configMax);
		return Rnd.get(band[0], band[1]);
	}

	/** @return how many nobles must still be created to reach {@code wanted}, never negative. */
	public static int missing(int existing, int wanted)
	{
		return Math.max(0, wanted - Math.max(0, existing));
	}

	/**
	 * @return {@code true} if a noble with these points may sign up for this match type (the stock point minimums)
	 */
	public static boolean hasPointsFor(boolean classed, int points)
	{
		return points >= (classed ? CLASSED_MIN_POINTS : NON_CLASSED_MIN_POINTS);
	}

	/**
	 * Whether an idle noble is free to sign up at all right now.
	 * @param inCompetition the competition is open
	 * @param registered already on a waiting list
	 * @param inMatch currently in (or being moved to) a match
	 * @param dead dead or pending revive
	 * @param now current time
	 * @param restUntil end of its rest after the last match (0 = never fought)
	 */
	public static boolean isFree(boolean inCompetition, boolean registered, boolean inMatch, boolean dead, long now, long restUntil)
	{
		return inCompetition && !registered && !inMatch && !dead && (now >= restUntil);
	}

	/**
	 * The sign-up roll for one free noble.
	 * @param playerWaiting a real player is signed up (and this noble can fill that pool): always sign up
	 * @param waitingPhantoms how many roster nobles already wait in this pool
	 * @param poolCap how many waiting nobles are enough (above it nobody else joins)
	 * @param chancePercent the background chance per check when no real player is signed up
	 * @param roll a 0-99 roll
	 */
	public static boolean shouldSignUp(boolean playerWaiting, int waitingPhantoms, int poolCap, int chancePercent, int roll)
	{
		if (waitingPhantoms >= poolCap)
		{
			return false;
		}
		if (playerWaiting)
		{
			return true;
		}
		return roll < Math.max(0, Math.min(100, chancePercent));
	}

	/**
	 * How many roster nobles may wait in one pool. The stock matcher starts matches once a pool reaches its match size
	 * and pairs entrants at random, so an odd pool leaves one entrant waiting for the next batch. The cap therefore
	 * fills a pool to at least its match size, rounded up to an even count, counting the real players already waiting
	 * in it. That way nobody, the real players included, is left over.
	 * @param participants the stock OlympiadClassedParticipants or OlympiadNonClassedParticipants value
	 * @param realWaiting how many real players wait in this pool
	 */
	public static int poolCap(int participants, int realWaiting)
	{
		final int real = Math.max(0, realWaiting);
		final int target = Math.max(Math.max(2, participants), real);
		final int even = ((target % 2) == 0) ? target : (target + 1);
		return even - real;
	}

	/**
	 * A party member's hold: while its owner is in a match, and for {@code delayMs} after the owner leaves match mode
	 * (the stock return teleport runs just after the mode is cleared), the member waits for the owner instead of being
	 * released for having lost its party.
	 * @param ownerInMatch the owner is in Olympiad mode now
	 * @param ownerLeftAt when the owner was last seen leaving a match (0 = no recent match)
	 */
	public static boolean holdsParty(boolean ownerInMatch, long ownerLeftAt, long now, long delayMs)
	{
		if (ownerInMatch)
		{
			return true;
		}
		return (ownerLeftAt > 0) && ((now - ownerLeftAt) < delayMs);
	}

	/**
	 * How many nobles of a class the base roster holds: {@code rosterSize} spread over the roster classes in id order,
	 * so every class gets {@code rosterSize / FIGHTING_CLASS_COUNT} and the first {@code rosterSize % FIGHTING_CLASS_COUNT}
	 * classes one more.
	 * @param rank the class's position among the roster classes (0 = Duelist)
	 */
	public static int baseQuota(int rank, int rosterSize)
	{
		final int size = Math.max(0, rosterSize);
		return (size / FIGHTING_CLASS_COUNT) + ((rank < (size % FIGHTING_CLASS_COUNT)) ? 1 : 0);
	}

	/**
	 * Picks the class the next base-roster noble gets: the lowest roster class id still below its {@link #baseQuota}.
	 * Rival nobles added for a player's class only count toward that class's own quota, so they never take a base slot
	 * from another class and the base roster always ends up spread over every roster class. Support classes and
	 * summoners are skipped.
	 * @param countsByClass nobles per class (base and rivals), indexed by {@code classId - FIRST_THIRD_CLASS_ID}
	 * @return the class id, or 0 when the base roster is complete
	 */
	public static int nextBaseClass(int[] countsByClass, int rosterSize)
	{
		int rank = 0;
		for (int i = 0; i < THIRD_CLASS_COUNT; i++)
		{
			if (!isRosterClass(FIRST_THIRD_CLASS_ID + i))
			{
				continue;
			}
			final int count = (i < countsByClass.length) ? countsByClass[i] : 0;
			if (count < baseQuota(rank, rosterSize))
			{
				return FIRST_THIRD_CLASS_ID + i;
			}
			rank++;
		}
		return 0;
	}
}
