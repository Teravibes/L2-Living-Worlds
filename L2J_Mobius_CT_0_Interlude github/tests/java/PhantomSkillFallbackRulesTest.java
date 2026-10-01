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
import org.l2jmobius.gameserver.managers.PhantomSkillFallbackRules;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link PhantomSkillFallbackRules}: the scoring, the
 * never-cast list and the rejected-cast backoff of the playstyle fallback.
 */
public class PhantomSkillFallbackRulesTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		truth(PhantomSkillFallbackRules.score(2000, 80) > PhantomSkillFallbackRules.score(1000, 80), "a stronger hit scores higher at the same MP");
		truth(PhantomSkillFallbackRules.score(1000, 40) > PhantomSkillFallbackRules.score(1000, 90), "a cheaper skill scores higher at the same power");
		truth(PhantomSkillFallbackRules.score(500, 60) > PhantomSkillFallbackRules.score(0, 60), "any damage skill beats a pure debuff at the same MP");
		truth(PhantomSkillFallbackRules.score(0, -5) == PhantomSkillFallbackRules.score(0, 0), "a negative MP cost counts as zero");

		truth(PhantomSkillFallbackRules.neverCast(28), "Aggression is left to the party manager");
		truth(PhantomSkillFallbackRules.neverCast(110), "Ultimate Defense is left to the party manager");
		truth(PhantomSkillFallbackRules.neverCast(1069), "Sleep is never cast by the fallback");
		truth(PhantomSkillFallbackRules.neverCast(254), "Spoil is never cast by the fallback");
		truth(!PhantomSkillFallbackRules.neverCast(1), "Triple Slash may be cast");
		truth(!PhantomSkillFallbackRules.neverCast(402), "Arrest may be cast");

		final long now = 50_000;
		truth(!PhantomSkillFallbackRules.backedOff(0, now), "never rejected: free");
		truth(PhantomSkillFallbackRules.backedOff(now - 500, now), "rejected half a second ago: resting");
		truth(!PhantomSkillFallbackRules.backedOff(now - PhantomSkillFallbackRules.REJECT_BACKOFF_MS, now), "backoff over");

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	private static void truth(boolean condition, String label)
	{
		checks++;
		if (!condition)
		{
			failures++;
			System.out.println("FAIL: " + label);
		}
	}
}
