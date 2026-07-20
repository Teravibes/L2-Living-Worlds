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
package org.l2jmobius.commons.time;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility class for time-related operations, such as parsing durations, scheduling future dates and formatting dates.
 * @author Mobius
 */
public class TimeUtil
{
	/**
	 * Parses a string duration (e.g., "5days", "2hours") into a {@link Duration} object.
	 * @param durationString the string representing the duration with a numeric value and time unit (e.g., "5days", "10hours", "2weeks").
	 * @return a {@link Duration} object representing the specified duration.
	 * @throws IllegalArgumentException if the input format is invalid or the unit is unrecognized.
	 */
	public static Duration parseDuration(String durationString)
	{
		int index = 0;
		while ((index < durationString.length()) && Character.isDigit(durationString.charAt(index)))
		{
			index++;
		}
		
		if ((index == 0) || (index == durationString.length()))
		{
			throw new IllegalArgumentException("Invalid duration format: " + durationString);
		}
		
		int durationValue;
		String durationUnit;
		try
		{
			durationValue = Integer.parseInt(durationString.substring(0, index));
			durationUnit = durationString.substring(index).toLowerCase();
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid duration format: " + durationString);
		}
		
		switch (durationUnit)
		{
			case "sec":
			case "secs":
			{
				return Duration.ofSeconds(durationValue);
			}
			case "min":
			case "mins":
			{
				return Duration.ofMinutes(durationValue);
			}
			case "hour":
			case "hours":
			{
				return Duration.ofHours(durationValue);
			}
			case "day":
			case "days":
			{
				return Duration.ofDays(durationValue);
			}
			case "week":
			case "weeks":
			{
				return Duration.ofDays(durationValue * 7L);
			}
			case "month":
			case "months":
			{
				return Duration.ofDays(durationValue * 30L);
			}
			case "year":
			case "years":
			{
				return Duration.ofDays(durationValue * 365L);
			}
			default:
			{
				throw new IllegalArgumentException("Unrecognized time unit: " + durationUnit);
			}
		}
	}
	
	/**
	 * Formats a duration in milliseconds into a user-friendly string, specifying the number of days, hours, minutes, seconds, and milliseconds (if any).
	 * @param millis the duration in milliseconds.
	 * @return a formatted string representing the duration.
	 */
	public static String formatDuration(long millis)
	{
		if (millis < 1)
		{
			return "0 milliseconds";
		}
		
		long days = millis / (24 * 60 * 60 * 1000);
		millis %= (24 * 60 * 60 * 1000);
		
		long hours = millis / (60 * 60 * 1000);
		millis %= (60 * 60 * 1000);
		
		long minutes = millis / (60 * 1000);
		millis %= (60 * 1000);
		
		long seconds = millis / 1000;
		millis %= 1000;
		
		final StringBuilder sb = new StringBuilder();
		if (days > 0)
		{
			sb.append(days).append(" day").append(days > 1 ? "s" : "").append(", ");
		}
		
		if (hours > 0)
		{
			sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(", ");
		}
		
		if (minutes > 0)
		{
			sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(", ");
		}
		
		if (seconds > 0)
		{
			sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "").append(", ");
		}
		
		if (millis > 0)
		{
			sb.append(millis).append(" millisecond").append(millis > 1 ? "s" : "");
		}
		
		// Remove the trailing comma and space, if present.
		if ((sb.length() > 2) && (sb.charAt(sb.length() - 2) == ','))
		{
			sb.setLength(sb.length() - 2);
		}
		
		return sb.toString();
	}
	
	/**
	 * Formats a date into a string based on the provided format pattern.
	 * @param date the {@link Date} object to format.
	 * @param format the date format pattern (e.g., "dd/MM/yyyy").
	 * @return a formatted date string or null if the date is null.
	 */
	public static String formatDate(Date date, String format)
	{
		return date == null ? null : new SimpleDateFormat(format).format(date);
	}
	
	/**
	 * Formats a date to a string in the "dd/MM/yyyy" format.
	 * @param date the {@link Date} object to format.
	 * @return a formatted date string or null if the date is null.
	 */
	public static String getDateString(Date date)
	{
		return formatDate(date, "dd/MM/yyyy");
	}
	
	/**
	 * Formats a date to a string in the "dd/MM/yyyy HH:mm:ss" format.
	 * @param date the {@link Date} object to format.
	 * @return a formatted date-time string or null if the date is null.
	 */
	public static String getDateTimeString(Date date)
	{
		return formatDate(date, "dd/MM/yyyy HH:mm:ss");
	}
	
	/**
	 * Formats a timestamp (in milliseconds) to a string in the "dd/MM/yyyy" format.
	 * @param millis the timestamp in milliseconds.
	 * @return a formatted date string.
	 */
	public static String getDateString(long millis)
	{
		return getDateString(new Date(millis));
	}
	
	/**
	 * Formats a timestamp (in milliseconds) to a string in the "dd/MM/yyyy HH:mm:ss" format.
	 * @param millis the timestamp in milliseconds.
	 * @return a formatted date-time string.
	 */
	public static String getDateTimeString(long millis)
	{
		return getDateTimeString(new Date(millis));
	}
	
	/**
	 * Gets the next occurrence of the specified day of the week, hour, and minute. If the specified time is in the past for today, the next week will be scheduled.
	 * @param dayOfWeek the desired day of the week (e.g., {@link Calendar#MONDAY}).
	 * @param hour the hour of the day (0-23).
	 * @param minute the minute of the hour (0-59).
	 * @return a {@link Calendar} object set to the next occurrence of the specified day and time.
	 */
	public static Calendar getNextDayTime(int dayOfWeek, int hour, int minute)
	{
		final Calendar calendar = Calendar.getInstance();
		final int today = calendar.get(Calendar.DAY_OF_WEEK);
		int daysUntilNext = ((dayOfWeek - today) + 7) % 7;
		if ((daysUntilNext == 0) && ((calendar.get(Calendar.HOUR_OF_DAY) > hour) || ((calendar.get(Calendar.HOUR_OF_DAY) == hour) && (calendar.get(Calendar.MINUTE) >= minute))))
		{
			daysUntilNext = 7; // Schedule for the next week if today's time has passed.
		}
		
		calendar.add(Calendar.DAY_OF_MONTH, daysUntilNext);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		return calendar;
	}
	
	/**
	 * Gets the next occurrence of the specified hour and minute on the current day. If the specified time is in the past for today, the next day will be scheduled.
	 * @param hour the hour of the day (0-23).
	 * @param minute the minute of the hour (0-59).
	 * @return a {@link Calendar} object set to the next occurrence of the specified time.
	 */
	public static Calendar getNextTime(int hour, int minute)
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		// If the target time has already passed today, schedule for the next day.
		if (calendar.before(Calendar.getInstance()))
		{
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		return calendar;
	}
}
