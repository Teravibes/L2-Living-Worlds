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

/**
 * Pure rules for the playstyle fallback in {@link PhantomPlaystyleEngine#pickFallback}: when no playstyle entry fires
 * this tick, a phantom may cast one of its own offensive skills that its playstyle does not list (a level 80 Spectral
 * Dancer whose list only has Arrest left). The damage score is adapted from L2Solo's power versus MP heuristic:
 * stronger hits win and MP cost counts against. The flat score for a pure debuff is this project's own, and it ranks
 * a debuff below any damage skill, so important debuffs belong in the playstyles (FPC-134). Everything here takes
 * plain values, so it is unit tested in {@code tests/java/PhantomSkillFallbackRulesTest.java}.
 */
public final class PhantomSkillFallbackRules
{
	/** A skill the server rejected is not tried again for this long (a bad weapon, a lost target, a failed condition). */
	public static final long REJECT_BACKOFF_MS = 2000;

	/**
	 * Skills the fallback never casts: the ones the party manager owns (threat, survival, crowd control, rescue) or that
	 * would hurt the fight if cast on reuse. Aggression, Aura of Hate, Ultimate Defense, Sleep, Dryad Root, Resurrection,
	 * Provoke, Spoil, Spoil Festival, Sweeper.
	 */
	private static final int[] NEVER_CAST =
	{
		28,
		18,
		110,
		1069,
		1201,
		1016,
		286,
		254,
		302,
		42
	};

	private PhantomSkillFallbackRules()
	{
	}

	/** @return {@code true} if the fallback must leave this skill to its owner */
	public static boolean neverCast(int skillId)
	{
		for (int id : NEVER_CAST)
		{
			if (id == skillId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @param power the skill's power (0 for a pure debuff)
	 * @param mpConsume its MP cost
	 * @return the candidate's score; the highest usable candidate is cast
	 */
	public static double score(double power, int mpConsume)
	{
		final double damage = (power > 0) ? (100 + (Math.log(power + 1) / Math.log(2) * 35)) : 40;
		return damage - (Math.max(0, mpConsume) * 1.5);
	}

	/** @return {@code true} while a skill rejected at {@code rejectedAt} is still backed off at {@code now} */
	public static boolean backedOff(long rejectedAt, long now)
	{
		return (rejectedAt > 0) && ((now - rejectedAt) < REJECT_BACKOFF_MS);
	}
}
