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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.StringUtil;

/**
 * A UNIX cron-like pattern parser for scheduling tasks.<br>
 * Supports extended syntax with randomization and time offset modifiers for flexible task scheduling.
 * <ul>
 * <li>Standard cron format with 5 or 6 space-separated fields (minute, hour, day, month, weekday, optional week offset).</li>
 * <li>Extended modifiers: ~N for random delays, +N for time offsets, L for last day of month.</li>
 * <li>Multiple pattern support using pipe (|) separator for OR conditions.</li>
 * <li>Month and weekday name aliases (jan-dec, sun-sat) for improved readability.</li>
 * </ul>
 * @author Mobius
 */
public class SchedulingPattern
{
	// Constants.
	private static final int MINUTE_MIN = 0;
	private static final int MINUTE_MAX = 59;
	private static final int HOUR_MIN = 0;
	private static final int HOUR_MAX = 23;
	private static final int DAY_MIN = 1;
	private static final int DAY_MAX = 31;
	private static final int MONTH_MIN = 1;
	private static final int MONTH_MAX = 12;
	private static final int DAY_OF_WEEK_MIN = 0; // 0 = Sunday
	private static final int DAY_OF_WEEK_MAX = 6;
	private static final int LAST_DAY_MARKER = 32; // Special marker for last day of month.
	private static final int CALENDAR_MONTH_OFFSET = 1; // Calendar months are 0-based.
	private static final int CALENDAR_DAY_OF_WEEK_OFFSET = 1; // Convert to 0 = Sunday.
	private static final int SEARCH_LIMIT_YEARS = 4; // Maximum years to search for next match.
	private static final int MINIMUM_CRON_FIELDS = 5;
	private static final int MAXIMUM_CRON_FIELDS = 6;
	private static final int CRON_PARTS_EXPECTED = 2;
	private static final String PIPE_SEPARATOR = "\\|";
	private static final String WHITESPACE_PATTERN = "\\s+";
	private static final String FIELD_VALIDATION_REGEX = "^[0-9a-zA-Z*,\\-/:~+L]+$";
	private static final String NO_FUTURE_MATCH_MESSAGE = "No future match.";
	
	// Month aliases for improved readability.
	private static final Map<String, Integer> MONTH_ALIASES = new HashMap<>();
	static
	{
		MONTH_ALIASES.put("jan", 1);
		MONTH_ALIASES.put("feb", 2);
		MONTH_ALIASES.put("mar", 3);
		MONTH_ALIASES.put("apr", 4);
		MONTH_ALIASES.put("may", 5);
		MONTH_ALIASES.put("jun", 6);
		MONTH_ALIASES.put("jul", 7);
		MONTH_ALIASES.put("aug", 8);
		MONTH_ALIASES.put("sep", 9);
		MONTH_ALIASES.put("oct", 10);
		MONTH_ALIASES.put("nov", 11);
		MONTH_ALIASES.put("dec", 12);
	}
	
	// Day of week aliases for improved readability.
	private static final Map<String, Integer> DAY_ALIASES = new HashMap<>();
	static
	{
		DAY_ALIASES.put("sun", 0);
		DAY_ALIASES.put("mon", 1);
		DAY_ALIASES.put("tue", 2);
		DAY_ALIASES.put("wed", 3);
		DAY_ALIASES.put("thu", 4);
		DAY_ALIASES.put("fri", 5);
		DAY_ALIASES.put("sat", 6);
	}
	
	// Pattern data.
	private final String _originalPattern;
	private final List<CronExpression> _cronExpressions;
	
	/**
	 * Creates a new scheduling pattern from a cron-like string.
	 * @param pattern The cron pattern string
	 * @throws RuntimeException if the pattern is invalid.
	 */
	public SchedulingPattern(String pattern) throws RuntimeException
	{
		_originalPattern = Objects.requireNonNull(pattern, "Pattern cannot be null.");
		try
		{
			_cronExpressions = parsePattern(pattern);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Invalid scheduling pattern: " + pattern, e);
		}
	}
	
	/**
	 * Validates whether a string is a valid scheduling pattern.
	 * @param schedulingPattern The pattern to validate
	 * @return true if valid, false otherwise
	 */
	public static boolean validate(String schedulingPattern)
	{
		if (schedulingPattern == null)
		{
			return false;
		}
		
		try
		{
			// Lightweight validation without full parsing.
			final String[] orPatterns = schedulingPattern.split(PIPE_SEPARATOR);
			for (String orPattern : orPatterns)
			{
				final String[] fields = orPattern.trim().split(WHITESPACE_PATTERN);
				if ((fields.length < MINIMUM_CRON_FIELDS) || (fields.length > MAXIMUM_CRON_FIELDS))
				{
					return false;
				}
				
				// Basic syntax validation for each field.
				if (!isValidField(fields[0]) || !isValidField(fields[1]) || !isValidField(fields[2]) || !isValidField(fields[3]) || !isValidField(fields[4]))
				{
					return false;
				}
				
				// Validate optional week offset field.
				if (fields.length == MAXIMUM_CRON_FIELDS)
				{
					final String weekField = fields[5].trim();
					if (!weekField.startsWith("+") || !StringUtil.isNumeric(weekField.substring(1)))
					{
						return false;
					}
				}
			}
			
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Lightweight field validation for pattern syntax checking.
	 * @param field the field to validate
	 * @return true if field has valid basic syntax
	 */
	private static boolean isValidField(String field)
	{
		if ((field == null) || field.trim().isEmpty())
		{
			return false;
		}
		
		// Check for valid characters and basic syntax.
		return field.matches(FIELD_VALIDATION_REGEX);
	}
	
	/**
	 * Checks if the given timestamp matches this pattern.
	 * @param timezone The timezone to use
	 * @param millis The timestamp in milliseconds
	 * @return true if the timestamp matches
	 */
	public boolean match(TimeZone timezone, long millis)
	{
		final Calendar calendar = Calendar.getInstance(timezone);
		calendar.setTimeInMillis(millis);
		
		// Normalize to minute precision.
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		// Use traditional loop for better performance in hot paths.
		for (CronExpression cronExpression : _cronExpressions)
		{
			if (cronExpression.matches(calendar))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the given timestamp matches this pattern using system timezone.
	 * @param millis The timestamp in milliseconds
	 * @return true if the timestamp matches
	 */
	public boolean match(long millis)
	{
		return match(TimeZone.getDefault(), millis);
	}
	
	/**
	 * Finds the next matching time after the given timestamp.
	 * @param timezone The timezone to use
	 * @param millis The timestamp to search after
	 * @return The next matching timestamp in milliseconds
	 */
	public long next(TimeZone timezone, long millis)
	{
		long earliestMatch = -1L;
		
		// Use traditional loop for better performance in hot paths.
		for (CronExpression cronExpression : _cronExpressions)
		{
			final long nextMatch = cronExpression.getNextMatch(millis, timezone);
			if ((nextMatch > millis) && ((earliestMatch == -1L) || (nextMatch < earliestMatch)))
			{
				earliestMatch = nextMatch;
			}
		}
		
		return earliestMatch;
	}
	
	/**
	 * Finds the next matching time after the given timestamp using system timezone.
	 * @param millis The timestamp to search after
	 * @return The next matching timestamp in milliseconds
	 */
	public long next(long millis)
	{
		return next(TimeZone.getDefault(), millis);
	}
	
	/**
	 * Gets delay from a specific time until next match.
	 * @param millis The base timestamp
	 * @return Delay in milliseconds until next match after millis
	 */
	public long nextFrom(long millis)
	{
		final long nextMatch = next(millis);
		return nextMatch > millis ? nextMatch - millis : -1;
	}
	
	/**
	 * Gets delay from current time until next match.
	 * @return Delay in milliseconds until next match from now
	 */
	public long nextFromNow()
	{
		return nextFrom(System.currentTimeMillis());
	}
	
	/**
	 * Gets the delay in milliseconds until the next match from now.
	 * @return Delay in milliseconds until next match
	 */
	public long getDelayToNextFromNow()
	{
		return nextFromNow();
	}
	
	/**
	 * Gets delay with offset subtraction.
	 * @param offsetInMinutes Offset to subtract from delay
	 * @return Adjusted delay in milliseconds
	 */
	public long getOffsettedDelayToNextFromNow(int offsetInMinutes)
	{
		final long delay = getDelayToNextFromNow();
		final long offsetMillis = TimeUnit.MINUTES.toMillis(offsetInMinutes);
		return Math.max(0, delay - offsetMillis);
	}
	
	/**
	 * Gets the next matching time from now as a formatted date string.
	 * @return Formatted date string of next match
	 */
	public String getNextAsFormattedDateString()
	{
		final long nextMatch = next(System.currentTimeMillis());
		return nextMatch > 0 ? new Date(nextMatch).toString() : NO_FUTURE_MATCH_MESSAGE;
	}
	
	@Override
	public String toString()
	{
		return _originalPattern;
	}
	
	/**
	 * Parses the pattern string into cron expressions.
	 * @param pattern the pattern string to parse
	 * @return list of cron expressions
	 */
	private List<CronExpression> parsePattern(String pattern)
	{
		final List<CronExpression> result = new ArrayList<>();
		
		// Split on pipe for OR expressions.
		final String[] orPatterns = pattern.split(PIPE_SEPARATOR);
		
		for (String orPattern : orPatterns)
		{
			final String[] fields = orPattern.trim().split(WHITESPACE_PATTERN);
			if ((fields.length < MINIMUM_CRON_FIELDS) || (fields.length > MAXIMUM_CRON_FIELDS))
			{
				throw new IllegalArgumentException("Pattern must have 5 or 6 fields: " + orPattern);
			}
			
			try
			{
				// Parse fields with extended syntax support.
				final ExtendedFieldResult minuteResult = parseExtendedField(fields[0]);
				final ExtendedFieldResult hourResult = parseExtendedField(fields[1]);
				final ExtendedFieldResult dayResult = parseExtendedField(fields[2]);
				
				final FieldMatcher minuteMatcher = parseField(minuteResult.pattern, MINUTE_MIN, MINUTE_MAX, null);
				final FieldMatcher hourMatcher = parseField(hourResult.pattern, HOUR_MIN, HOUR_MAX, null);
				final FieldMatcher dayMatcher = parseField(dayResult.pattern, DAY_MIN, DAY_MAX, null);
				final FieldMatcher monthMatcher = parseField(fields[3], MONTH_MIN, MONTH_MAX, MONTH_ALIASES);
				final FieldMatcher dayOfWeekMatcher = parseField(fields[4], DAY_OF_WEEK_MIN, DAY_OF_WEEK_MAX, DAY_ALIASES);
				
				// Parse optional week offset (6th field).
				int weekOffset = 0;
				if (fields.length == MAXIMUM_CRON_FIELDS)
				{
					final String weekField = fields[5].trim();
					if (weekField.startsWith("+"))
					{
						weekOffset = Integer.parseInt(weekField.substring(1));
					}
					else
					{
						throw new IllegalArgumentException("Week offset must start with '+': " + weekField);
					}
				}
				
				result.add(new CronExpression(minuteMatcher, hourMatcher, dayMatcher, monthMatcher, dayOfWeekMatcher, minuteResult.randomModifier, hourResult.randomModifier, hourResult.addModifier, dayResult.addModifier, weekOffset));
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("Invalid pattern format: " + orPattern, e);
			}
		}
		
		return result;
	}
	
	/**
	 * Result of parsing an extended field with modifiers.
	 */
	private static class ExtendedFieldResult
	{
		final String pattern;
		final int randomModifier;
		final int addModifier;
		
		ExtendedFieldResult(String pattern, int randomModifier, int addModifier)
		{
			this.pattern = pattern;
			this.randomModifier = randomModifier;
			this.addModifier = addModifier;
		}
	}
	
	/**
	 * Parses a field that may contain extended syntax modifiers.<br>
	 * Format: [modifier:]pattern where modifier can be ~N or +N.
	 * @param field the field to parse
	 * @return extended field result with pattern and modifiers
	 */
	private ExtendedFieldResult parseExtendedField(String field)
	{
		if (!field.contains(":"))
		{
			return new ExtendedFieldResult(field, 0, 0);
		}
		
		final String[] parts = field.split(":");
		if (parts.length != CRON_PARTS_EXPECTED)
		{
			throw new IllegalArgumentException("Invalid extended field format: " + field);
		}
		
		final String modifier = parts[0];
		final String pattern = parts[1];
		
		int randomModifier = 0;
		int addModifier = 0;
		
		if (modifier.startsWith("~"))
		{
			randomModifier = Integer.parseInt(modifier.substring(1));
		}
		else if (modifier.startsWith("+"))
		{
			addModifier = Integer.parseInt(modifier.substring(1));
		}
		else if (!modifier.isEmpty())
		{
			throw new IllegalArgumentException("Unknown modifier: " + modifier);
		}
		
		return new ExtendedFieldResult(pattern, randomModifier, addModifier);
	}
	
	/**
	 * Parses a field value with support for wildcards, ranges, lists and step values.
	 * @param field the field string to parse
	 * @param min minimum allowed value
	 * @param max maximum allowed value
	 * @param aliases optional aliases map for named values
	 * @return field matcher for the parsed field
	 */
	private FieldMatcher parseField(String field, int min, int max, Map<String, Integer> aliases)
	{
		if ("*".equals(field))
		{
			return new WildcardMatcher();
		}
		
		final Set<Integer> values = new HashSet<>();
		final String[] parts = field.split(",");
		for (String part : parts)
		{
			values.addAll(parseFieldPart(part.trim(), min, max, aliases));
		}
		
		return new ValueSetMatcher(values);
	}
	
	/**
	 * Parses a single field part with support for ranges and step values.
	 * @param part the field part to parse
	 * @param min minimum allowed value
	 * @param max maximum allowed value
	 * @param aliases optional aliases map for named values
	 * @return set of integer values matching the part
	 */
	private Set<Integer> parseFieldPart(String part, int min, int max, Map<String, Integer> aliases)
	{
		final Set<Integer> values = new HashSet<>();
		
		// Handle step values (e.g., */5 or 1-10/2).
		final String[] stepParts = part.split("/");
		final int step = stepParts.length > 1 ? Integer.parseInt(stepParts[1]) : 1;
		final String rangePart = stepParts[0];
		
		if ("*".equals(rangePart))
		{
			// */step pattern.
			for (int i = min; i <= max; i += step)
			{
				values.add(i);
			}
		}
		else if (rangePart.contains("-"))
		{
			// Range pattern (e.g., 1-5 or mon-fri).
			final String[] range = rangePart.split("-", 2);
			final int start = parseValue(range[0], aliases);
			final int end = parseValue(range[1], aliases);
			if (start <= end)
			{
				for (int i = start; i <= end; i += step)
				{
					values.add(i);
				}
			}
			else
			{
				// Wrap-around range (e.g., fri-mon for days).
				for (int i = start; i <= max; i += step)
				{
					values.add(i);
				}
				
				for (int i = min; i <= end; i += step)
				{
					values.add(i);
				}
			}
		}
		else // Single value.
		{
			values.add(parseValue(rangePart, aliases));
		}
		
		return values;
	}
	
	/**
	 * Parses a single value with support for aliases and special markers.
	 * @param value the value string to parse
	 * @param aliases optional aliases map for named values
	 * @return parsed integer value
	 */
	private int parseValue(String value, Map<String, Integer> aliases)
	{
		if ("L".equalsIgnoreCase(value))
		{
			return LAST_DAY_MARKER; // Special marker for last day of month.
		}
		
		if ((aliases != null) && aliases.containsKey(value.toLowerCase()))
		{
			return aliases.get(value.toLowerCase());
		}
		
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid value: " + value, e);
		}
	}
	
	/**
	 * Interface for matching field values against calendar dates.
	 */
	private interface FieldMatcher
	{
		boolean matches(int value, Calendar calendar);
	}
	
	/**
	 * Matcher that accepts any value (wildcard).
	 */
	private static class WildcardMatcher implements FieldMatcher
	{
		@Override
		public boolean matches(int value, Calendar calendar)
		{
			return true;
		}
	}
	
	/**
	 * Matcher that checks against a predefined set of values.
	 */
	private static class ValueSetMatcher implements FieldMatcher
	{
		private final Set<Integer> _values;
		
		ValueSetMatcher(Set<Integer> values)
		{
			_values = new HashSet<>(values);
		}
		
		@Override
		public boolean matches(int value, Calendar calendar)
		{
			if (_values.contains(value))
			{
				return true;
			}
			
			// Handle last day of month (L).
			if (_values.contains(LAST_DAY_MARKER) && isLastDayOfMonth(calendar))
			{
				return true;
			}
			
			return false;
		}
		
		/**
		 * Checks if the calendar date is the last day of the month.
		 * @param calendar the calendar to check
		 * @return true if it's the last day of the month
		 */
		private boolean isLastDayOfMonth(Calendar calendar)
		{
			final int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
			final int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			return currentDay == lastDay;
		}
	}
	
	/**
	 * Represents a single cron expression with extended modifiers.
	 */
	private static class CronExpression
	{
		// Field matchers.
		private final FieldMatcher _minuteMatcher;
		private final FieldMatcher _hourMatcher;
		private final FieldMatcher _dayMatcher;
		private final FieldMatcher _monthMatcher;
		private final FieldMatcher _dayOfWeekMatcher;
		
		// Extended syntax modifiers.
		private final int _minuteRandomModifier;
		private final int _hourRandomModifier;
		private final int _hourAddModifier;
		private final int _dayAddModifier;
		private final int _weekOffset;
		
		CronExpression(FieldMatcher minuteMatcher, FieldMatcher hourMatcher, FieldMatcher dayMatcher, FieldMatcher monthMatcher, FieldMatcher dayOfWeekMatcher, int minuteRandomModifier, int hourRandomModifier, int hourAddModifier, int dayAddModifier, int weekOffset)
		{
			_minuteMatcher = minuteMatcher;
			_hourMatcher = hourMatcher;
			_dayMatcher = dayMatcher;
			_monthMatcher = monthMatcher;
			_dayOfWeekMatcher = dayOfWeekMatcher;
			_minuteRandomModifier = minuteRandomModifier;
			_hourRandomModifier = hourRandomModifier;
			_hourAddModifier = hourAddModifier;
			_dayAddModifier = dayAddModifier;
			_weekOffset = weekOffset;
		}
		
		/**
		 * Checks if the calendar date matches this cron expression.
		 * @param calendar the calendar to test
		 * @return true if the date matches
		 */
		boolean matches(Calendar calendar)
		{
			// Create a copy for testing with offsets applied.
			final Calendar testCalendar = Calendar.getInstance(calendar.getTimeZone());
			testCalendar.setTimeInMillis(calendar.getTimeInMillis());
			
			// Apply reverse offsets for matching (subtract what would be added).
			if (_weekOffset != 0)
			{
				testCalendar.add(Calendar.WEEK_OF_YEAR, -_weekOffset);
			}
			
			if (_dayAddModifier != 0)
			{
				testCalendar.add(Calendar.DAY_OF_YEAR, -_dayAddModifier);
			}
			
			if (_hourAddModifier != 0)
			{
				testCalendar.add(Calendar.HOUR_OF_DAY, -_hourAddModifier);
			}
			
			final int minute = testCalendar.get(Calendar.MINUTE);
			final int hour = testCalendar.get(Calendar.HOUR_OF_DAY);
			final int day = testCalendar.get(Calendar.DAY_OF_MONTH);
			final int month = testCalendar.get(Calendar.MONTH) + CALENDAR_MONTH_OFFSET; // Calendar months are 0-based.
			final int dayOfWeek = testCalendar.get(Calendar.DAY_OF_WEEK) - CALENDAR_DAY_OF_WEEK_OFFSET; // Convert to 0 = Sunday.
			return _minuteMatcher.matches(minute, testCalendar) && _hourMatcher.matches(hour, testCalendar) && _dayMatcher.matches(day, testCalendar) && _monthMatcher.matches(month, testCalendar) && _dayOfWeekMatcher.matches(dayOfWeek, testCalendar);
		}
		
		/**
		 * Finds the next matching time after the specified timestamp.
		 * @param afterMillis timestamp to search after
		 * @param timeZone timezone for calculation
		 * @return next matching timestamp in milliseconds
		 */
		long getNextMatch(long afterMillis, TimeZone timeZone)
		{
			final Calendar calendar = Calendar.getInstance(timeZone);
			calendar.setTimeInMillis(afterMillis);
			calendar.add(Calendar.MINUTE, 1); // Start from next minute.
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			
			// Search up to 4 years in the future to avoid infinite loops.
			final Calendar endCalendar = Calendar.getInstance(timeZone);
			endCalendar.setTimeInMillis(afterMillis);
			endCalendar.add(Calendar.YEAR, SEARCH_LIMIT_YEARS);
			
			// Reuse single calendar instance for result calculations.
			final Calendar resultCalendar = Calendar.getInstance(timeZone);
			
			while (calendar.before(endCalendar))
			{
				if (matches(calendar))
				{
					// Apply forward offsets and randomization.
					resultCalendar.setTimeInMillis(calendar.getTimeInMillis());
					
					// Apply fixed offsets.
					if (_weekOffset != 0)
					{
						resultCalendar.add(Calendar.WEEK_OF_YEAR, _weekOffset);
					}
					
					if (_dayAddModifier != 0)
					{
						resultCalendar.add(Calendar.DAY_OF_YEAR, _dayAddModifier);
					}
					
					if (_hourAddModifier != 0)
					{
						resultCalendar.add(Calendar.HOUR_OF_DAY, _hourAddModifier);
					}
					
					// Apply random offsets.
					if (_hourRandomModifier > 0)
					{
						resultCalendar.add(Calendar.HOUR_OF_DAY, Rnd.get(_hourRandomModifier + 1));
					}
					
					if (_minuteRandomModifier > 0)
					{
						resultCalendar.add(Calendar.MINUTE, Rnd.get(_minuteRandomModifier + 1));
					}
					
					return resultCalendar.getTimeInMillis();
				}
				
				calendar.add(Calendar.MINUTE, 1);
			}
			
			return -1; // No match found.
		}
	}
}
