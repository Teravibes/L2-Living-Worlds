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
package org.l2jmobius.gameserver.model.clan;

/**
 * @author Mobius
 */
public enum ClanAccess
{
	NONE,
	INVITE_MEMBER,
	ASSIGN_TITLE,
	ACCESS_WAREHOUSE,
	MODIFY_RANKS,
	WAR_DECLARATION,
	REMOVE_MEMBER,
	CHANGE_CREST,
	DISMISS_MENTEE,
	MEMBER_FAME,
	ACCESS_AIRSHIP,
	HALL_OPEN_DOOR,
	HALL_FUNCTIONS,
	HALL_AUCTION,
	HALL_BANISH,
	HALL_MANAGE_FUNCTIONS,
	CASTLE_OPEN_DOOR,
	CASTLE_MANOR,
	CASTLE_SIEGE,
	CASTLE_FUNCTIONS,
	CASTLE_BANISH,
	CASTLE_VAULT,
	CASTLE_MERCENARIES,
	CASTLE_MANAGE_FUNCTIONS;
	
	public int getMask()
	{
		return 1 << ordinal();
	}
}
