/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.holders;

import java.time.DayOfWeek;

/**
 * Simple class for storing Reenter Data for Instances.
 * @author FallenAngel
 */
public class InstanceReenterTimeHolder
{
	private final DayOfWeek _day;
	private final int _hour;
	private final int _minute;
	private final long _time;
	
	public InstanceReenterTimeHolder(long time)
	{
		_time = time;
		_day = null;
		_hour = -1;
		_minute = -1;
	}
	
	public InstanceReenterTimeHolder(DayOfWeek day, int hour, int minute)
	{
		_time = -1;
		_day = day;
		_hour = hour;
		_minute = minute;
	}
	
	public long getTime()
	{
		return _time;
	}
	
	public DayOfWeek getDay()
	{
		return _day;
	}
	
	public int getHour()
	{
		return _hour;
	}
	
	public int getMinute()
	{
		return _minute;
	}
}
