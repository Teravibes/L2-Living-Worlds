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
 * When a town fake player (a stock FakePlayer NPC) keeps a grudge against a real player.<br>
 * A fake that is hit puts the attacker on its hate list, and the stock AI never clears that list when the player's
 * PvP flag or karma runs out, so a player who once hit a fake could be chased and killed long after his name turned
 * white (and the FAKE_PLAYER clan call spread it to the whole crowd). A white player is not a fair target: the fake
 * forgets him, and does not call its friends on him. Hitting a fake flags the player again, so a fake still fights back
 * against anyone who is actually attacking it. Hooked from {@code AttackableAI} through
 * {@link FakePlayerBehaviorManager#forgetsWhitePlayer}; the rule is unit-tested in
 * {@code tests/java/FakePlayerGrudgeRulesTest.java}.
 */
public final class FakePlayerGrudgeRules
{
	private FakePlayerGrudgeRules()
	{
	}
	
	/**
	 * @param aggroPlayers FakePlayerAggroPlayers: fakes are configured to attack any player on sight
	 * @param karma the player's karma
	 * @param pvpFlag the player's PvP flag
	 * @param inPvpZone the player stands in a PvP (combat) zone
	 * @param inOlympiad the player is in Olympiad mode
	 * @return {@code true} if a fake should drop its hate for this player
	 */
	public static boolean forgets(boolean aggroPlayers, int karma, int pvpFlag, boolean inPvpZone, boolean inOlympiad)
	{
		return !aggroPlayers && (karma <= 0) && (pvpFlag == 0) && !inPvpZone && !inOlympiad;
	}
}
