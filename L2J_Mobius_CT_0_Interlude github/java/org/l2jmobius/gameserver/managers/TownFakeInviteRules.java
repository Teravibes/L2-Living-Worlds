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
 * Pure rules for a party invite sent to a town fake player (FPC-110). A town fake is an NPC, so the invite never reaches
 * the stock target checks in {@code RequestJoinParty}; the inviter-side rules those checks enforce are restated here so
 * a town fake can only be invited by a player who could invite a real player. Every input is a plain value, so the rules
 * are tested without a live world ({@code tests/java/TownFakeInviteRulesTest.java}).
 */
public final class TownFakeInviteRules
{
	/** The largest party Interlude allows. */
	public static final int MAX_PARTY_SIZE = 9;

	/** Why an invite is refused, or {@link #NONE} when it may go ahead. */
	public enum Refusal
	{
		NONE,
		PARTY_BANNED,
		EVENT,
		CURSED_WEAPON,
		JAILED,
		OLYMPIAD,
		BUSY,
		NOT_LEADER,
		DIMENSIONAL_RIFT,
		PARTY_FULL
	}

	private TownFakeInviteRules()
	{
	}

	/**
	 * The inviter-side checks of a normal invite, in the same order the stock handler runs them. A town fake is never on
	 * an event, in the Olympiad, jailed or holding a cursed weapon, so any of those on the inviter refuses the invite,
	 * exactly as a mismatch with a real target would.
	 * @param partyBanned the inviter is party banned
	 * @param onEvent the inviter is registered on an event
	 * @param cursedWeapon the inviter holds a cursed weapon
	 * @param jailed the inviter is jailed
	 * @param olympiad the inviter is in Olympiad mode
	 * @param processingRequest the inviter is waiting on another request
	 * @param inParty the inviter is in a party
	 * @param leader the inviter leads that party (ignored when not in a party)
	 * @param inRift that party is in the Dimensional Rift (ignored when not in a party)
	 * @param memberCount that party's member count (ignored when not in a party)
	 * @return the first rule that refuses the invite, or {@link Refusal#NONE}
	 */
	public static Refusal inviterRefusal(boolean partyBanned, boolean onEvent, boolean cursedWeapon, boolean jailed, boolean olympiad, boolean processingRequest, boolean inParty, boolean leader, boolean inRift, int memberCount)
	{
		if (partyBanned)
		{
			return Refusal.PARTY_BANNED;
		}
		if (onEvent)
		{
			return Refusal.EVENT;
		}
		if (cursedWeapon)
		{
			return Refusal.CURSED_WEAPON;
		}
		if (jailed)
		{
			return Refusal.JAILED;
		}
		if (olympiad)
		{
			return Refusal.OLYMPIAD;
		}
		if (processingRequest)
		{
			return Refusal.BUSY;
		}
		if (inParty)
		{
			if (!leader)
			{
				return Refusal.NOT_LEADER;
			}
			if (inRift)
			{
				return Refusal.DIMENSIONAL_RIFT;
			}
			if (memberCount >= MAX_PARTY_SIZE)
			{
				return Refusal.PARTY_FULL;
			}
		}
		return Refusal.NONE;
	}

	/**
	 * Whether a server-side join into the owner's party may happen right now (defense in depth for every clientless
	 * member: recruits, friends and town fakes). Only a solo player or the party leader can add someone, and never past
	 * the size cap.
	 * @param inParty the owner is in a party
	 * @param leader the owner leads it (ignored when not in a party)
	 * @param memberCount its member count (ignored when not in a party)
	 * @return {@code true} if the member may be added
	 */
	public static boolean mayAddMember(boolean inParty, boolean leader, int memberCount)
	{
		return !inParty || (leader && (memberCount < MAX_PARTY_SIZE));
	}
}
