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
package org.l2jmobius.gameserver.model.actor.holders.creature;

import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Simple class containing all necessary information to maintain<br>
 * valid time stamps and reuse for skills and items reuse upon re-login.<br>
 * <b>Filter this carefully as it becomes redundant to store reuse for small delays.</b>
 * @author Yesod, Zoey76, Mobius
 */
public class TimeStamp
{
	/** Item or skill ID. */
	private final int _id1;
	/** Item object ID or skill level. */
	private final int _id2;
	/** Item or skill reuse time. */
	private final long _reuse;
	/** Time stamp. */
	private volatile long _stamp;
	/** Shared reuse group. */
	private final int _group;
	
	/**
	 * Skill time stamp constructor.
	 * @param skill the skill upon the stamp will be created.
	 * @param reuse the reuse time for this skill.
	 * @param systime overrides the system time with a customized one.
	 */
	public TimeStamp(Skill skill, long reuse, long systime)
	{
		_id1 = skill.getId();
		_id2 = skill.getLevel();
		_reuse = reuse;
		_stamp = systime > 0 ? systime : reuse != 0 ? System.currentTimeMillis() + reuse : 0;
		_group = -1;
	}
	
	/**
	 * Item time stamp constructor.
	 * @param item the item upon the stamp will be created.
	 * @param reuse the reuse time for this item.
	 * @param systime overrides the system time with a customized one.
	 */
	public TimeStamp(Item item, long reuse, long systime)
	{
		_id1 = item.getId();
		_id2 = item.getObjectId();
		_reuse = reuse;
		_stamp = systime > 0 ? systime : reuse != 0 ? System.currentTimeMillis() + reuse : 0;
		_group = item.getSharedReuseGroup();
	}
	
	/**
	 * Gets the time stamp.
	 * @return the time stamp, either the system time where this time stamp was created or the custom time assigned
	 */
	public long getStamp()
	{
		return _stamp;
	}
	
	/**
	 * Gets the item ID.
	 * @return the item ID
	 */
	public int getItemId()
	{
		return _id1;
	}
	
	/**
	 * Gets the item object ID.
	 * @return the item object ID
	 */
	public int getItemObjectId()
	{
		return _id2;
	}
	
	/**
	 * Gets the skill ID.
	 * @return the skill ID
	 */
	public int getSkillId()
	{
		return _id1;
	}
	
	/**
	 * Gets the skill level.
	 * @return the skill level
	 */
	public int getSkillLevel()
	{
		return _id2;
	}
	
	/**
	 * Gets the reuse.
	 * @return the reuse
	 */
	public long getReuse()
	{
		return _reuse;
	}
	
	/**
	 * Get the shared reuse group.<br>
	 * Only used on items.
	 * @return the shared reuse group
	 */
	public int getSharedReuseGroup()
	{
		return _group;
	}
	
	/**
	 * Gets the remaining time.
	 * @return the remaining time for this time stamp to expire
	 */
	public long getRemaining()
	{
		if (_stamp == 0)
		{
			return 0;
		}
		
		final long remainingTime = _stamp - System.currentTimeMillis();
		if (remainingTime <= 0)
		{
			_stamp = 0;
			return 0;
		}
		
		return remainingTime;
	}
	
	/**
	 * Verifies if the reuse delay has passed.
	 * @return {@code true} if this time stamp has expired, {@code false} otherwise
	 */
	public boolean hasNotPassed()
	{
		if (_stamp == 0)
		{
			return false;
		}
		
		if (System.currentTimeMillis() >= _stamp)
		{
			_stamp = 0;
			return false;
		}
		
		return true;
	}
}
