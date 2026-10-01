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
import org.l2jmobius.gameserver.managers.FakePlayerGrudgeRules;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link FakePlayerGrudgeRules}: a town fake player
 * forgets a player whose name is white, and keeps fighting one who is flagged, red, in a PvP zone or in the Olympiad.
 */
public class FakePlayerGrudgeRulesTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		// Arguments: aggroPlayers, karma, pvpFlag, inPvpZone, inOlympiad.
		truth(FakePlayerGrudgeRules.forgets(false, 0, 0, false, false), "a white player is forgotten");
		truth(!FakePlayerGrudgeRules.forgets(false, 0, 1, false, false), "a flagged player is still fought");
		truth(!FakePlayerGrudgeRules.forgets(false, 0, 2, false, false), "a player whose flag is fading is still fought");
		truth(!FakePlayerGrudgeRules.forgets(false, 150, 0, false, false), "a red player is still fought");
		truth(!FakePlayerGrudgeRules.forgets(false, 0, 0, true, false), "a white player in a PvP zone is still fought");
		truth(!FakePlayerGrudgeRules.forgets(false, 0, 0, false, true), "an Olympiad player is left to the Olympiad rules");
		truth(!FakePlayerGrudgeRules.forgets(true, 0, 0, false, false), "FakePlayerAggroPlayers keeps the stock attack-on-sight behavior");

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
