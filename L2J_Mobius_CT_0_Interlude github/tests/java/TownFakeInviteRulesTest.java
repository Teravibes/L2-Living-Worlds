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

import org.l2jmobius.gameserver.managers.TownFakeInviteRules;
import org.l2jmobius.gameserver.managers.TownFakeInviteRules.Refusal;

/**
 * Standalone (no JUnit, no game server) regression harness for {@link TownFakeInviteRules} (FPC-110): a party invite to
 * a town fake must obey the same inviter rules as a normal invite, and a server-side join may only add a member for a
 * solo player or the party leader.
 */
public class TownFakeInviteRulesTest
{
	private static int checks = 0;
	private static int failures = 0;

	public static void main(String[] args)
	{
		testAllowed();
		testRefusals();
		testRuleOrder();
		testMayAddMember();

		System.out.println();
		System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
		if (failures > 0)
		{
			System.exit(1);
		}
		System.out.println("OK");
	}

	// Arguments: partyBanned, onEvent, cursedWeapon, jailed, olympiad, processingRequest, inParty, leader, inRift, memberCount.

	private static void testAllowed()
	{
		eq(Refusal.NONE, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, false, false, false, false, 0), "solo player may invite");
		eq(Refusal.NONE, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, false, true, true, false, 8), "leader with a free slot may invite");
	}

	private static void testRefusals()
	{
		eq(Refusal.NOT_LEADER, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, false, true, false, false, 3), "a non-leader cannot invite");
		eq(Refusal.PARTY_BANNED, TownFakeInviteRules.inviterRefusal(true, false, false, false, false, false, false, false, false, 0), "a party-banned player cannot invite");
		eq(Refusal.EVENT, TownFakeInviteRules.inviterRefusal(false, true, false, false, false, false, false, false, false, 0), "an event player cannot invite");
		eq(Refusal.CURSED_WEAPON, TownFakeInviteRules.inviterRefusal(false, false, true, false, false, false, false, false, false, 0), "a cursed weapon holder cannot invite");
		eq(Refusal.JAILED, TownFakeInviteRules.inviterRefusal(false, false, false, true, false, false, false, false, false, 0), "a jailed player cannot invite");
		eq(Refusal.OLYMPIAD, TownFakeInviteRules.inviterRefusal(false, false, false, false, true, false, false, false, false, 0), "an Olympiad player cannot invite");
		eq(Refusal.BUSY, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, true, false, false, false, 0), "a player waiting on another request cannot invite");
		eq(Refusal.DIMENSIONAL_RIFT, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, false, true, true, true, 4), "a rift party cannot invite");
		eq(Refusal.PARTY_FULL, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, false, true, true, false, 9), "a full party cannot invite");
	}

	private static void testRuleOrder()
	{
		// The stock handler checks the ban before the leader; a banned non-leader reports the ban.
		eq(Refusal.PARTY_BANNED, TownFakeInviteRules.inviterRefusal(true, false, false, false, false, false, true, false, false, 3), "ban is reported before leadership");
		// Leadership, not size, is reported to a non-leader of a full party.
		eq(Refusal.NOT_LEADER, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, false, true, false, false, 9), "leadership is reported before a full party");
		// Party-only rules are ignored for a solo player whatever the party arguments say.
		eq(Refusal.NONE, TownFakeInviteRules.inviterRefusal(false, false, false, false, false, false, false, false, true, 9), "solo player ignores party-only rules");
	}

	private static void testMayAddMember()
	{
		truth(TownFakeInviteRules.mayAddMember(false, false, 0), "solo owner may add (a new party is formed)");
		truth(TownFakeInviteRules.mayAddMember(true, true, 8), "leader with a free slot may add");
		truth(!TownFakeInviteRules.mayAddMember(true, false, 3), "a non-leader may not add");
		truth(!TownFakeInviteRules.mayAddMember(true, true, 9), "a full party may not grow");
	}

	private static void eq(Object expected, Object actual, String what)
	{
		checks++;
		if (!expected.equals(actual))
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
