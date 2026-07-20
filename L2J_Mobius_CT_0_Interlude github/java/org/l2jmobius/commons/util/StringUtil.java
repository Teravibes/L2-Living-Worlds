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
package org.l2jmobius.commons.util;

import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;

/**
 * Utility class for String operations, providing methods to efficiently build and format strings.
 * @author Mobius
 */
public class StringUtil
{
	/**
	 * Appends a string to a given StringBuilder.<br>
	 * This method avoids the unnecessary use of `public static void append(StringBuilder sb, String... args)`,<br>
	 * which could introduce overhead due to varargs processing when only a single argument is appended.
	 * @param sb the StringBuilder to append to
	 * @param arg the string to append
	 */
	public static void append(StringBuilder sb, String arg)
	{
		sb.append(arg);
	}
	
	/**
	 * Appends multiple strings to a given StringBuilder.
	 * @param sb the StringBuilder to append to
	 * @param args the strings to append
	 */
	public static void append(StringBuilder sb, String... args)
	{
		// Directly calculate the required capacity and ensure it in one step.
		int totalLength = sb.length();
		for (String arg : args)
		{
			totalLength += (arg != null ? arg.length() : 4);
		}
		
		sb.ensureCapacity(totalLength);
		
		// Append each argument.
		for (String arg : args)
		{
			sb.append(arg);
		}
	}
	
	/**
	 * Appends multiple objects to a given StringBuilder.
	 * @param sb the StringBuilder to append to
	 * @param args the objects to append
	 */
	public static void append(StringBuilder sb, Object... args)
	{
		// Calculate the total length and store converted strings.
		int totalLength = sb.length();
		final List<String> strings = new LinkedList<>();
		for (Object arg : args)
		{
			final String objectAsString = String.valueOf(arg);
			totalLength += objectAsString.length();
			strings.add(objectAsString);
		}
		
		sb.ensureCapacity(totalLength);
		
		// Append each stored string.
		for (String string : strings)
		{
			sb.append(string);
		}
	}
	
	/**
	 * Concatenates multiple strings into a single string.
	 * @param args the strings to concatenate
	 * @return the concatenated string
	 */
	public static String concat(String... args)
	{
		// Calculate the total length of all strings.
		int totalLength = 0;
		for (String arg : args)
		{
			totalLength += (arg != null ? arg.length() : 4);
		}
		
		// Append each argument.
		final StringBuilder sb = new StringBuilder(totalLength);
		for (String arg : args)
		{
			sb.append(arg);
		}
		
		return sb.toString();
	}
	
	/**
	 * Concatenates multiple objects into a single string.
	 * @param args the objects to concatenate
	 * @return the concatenated string
	 */
	public static String concat(Object... args)
	{
		// Calculate the total length and store converted strings.
		int totalLength = 0;
		final List<String> strings = new LinkedList<>();
		for (Object arg : args)
		{
			final String objectAsString = String.valueOf(arg);
			totalLength += objectAsString.length();
			strings.add(objectAsString);
		}
		
		// Append each stored string.
		final StringBuilder sb = new StringBuilder(totalLength);
		for (String string : strings)
		{
			sb.append(string);
		}
		
		return sb.toString();
	}
	
	/**
	 * Concatenates elements in an Iterable into a single string with a specified delimiter.
	 * @param <T> the type of elements in the iterable
	 * @param items the iterable collection of elements to join
	 * @param delimiter the delimiter to place between elements
	 * @return a single string with each element separated by the delimiter
	 */
	public static <T> String implode(Iterable<T> items, String delimiter)
	{
		final StringJoiner joiner = new StringJoiner(delimiter);
		for (T item : items)
		{
			joiner.add(item.toString());
		}
		
		return joiner.toString();
	}
	
	/**
	 * Concatenates elements in an array into a single string with a specified delimiter.
	 * @param <T> the type of elements in the array
	 * @param array the array of elements to join
	 * @param delimiter the delimiter to place between elements
	 * @return a single string with each element separated by the delimiter
	 */
	public static <T> String implode(T[] array, String delimiter)
	{
		final StringJoiner joiner = new StringJoiner(delimiter);
		for (T element : array)
		{
			joiner.add(element.toString());
		}
		
		return joiner.toString();
	}
	
	/**
	 * Capitalizes the first letter of a given string and converts the rest to lowercase.
	 * @param text the input string to be formatted
	 * @return the formatted string with the first letter capitalized and the rest in lowercase, or the original string if it is null or empty
	 */
	public static String capitalizeFirst(String text)
	{
		// Return the original if it's null or empty.
		if ((text == null) || text.isEmpty())
		{
			return text;
		}
		
		// Capitalize the first letter and set the remaining letters to lowercase.
		return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
	}
	
	/**
	 * Splits a camelCase or PascalCase string into words separated by a space.
	 * @param text the string to split into words
	 * @return a string with words separated by a space, or the original text if null or empty
	 */
	public static String separateWords(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return text;
		}
		
		final StringBuilder result = new StringBuilder();
		final char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			final char current = chars[i];
			
			// Check if the current character is uppercase and it's not the first character.
			if (Character.isUpperCase(current) && (i > 0) && Character.isLowerCase(chars[i - 1]))
			{
				result.append(' ');
			}
			
			result.append(current);
		}
		
		return result.toString();
	}
	
	/**
	 * Converts an enum constant's name to a formatted string with proper casing.<br>
	 * For example, an enum constant named "ENUM_CONSTANT_NAME" will be formatted as "Enum Constant Name".
	 * @param enumeration the enum constant to format
	 * @return a formatted string with each word capitalized
	 */
	public static String enumToString(Enum<?> enumeration)
	{
		final String name = enumeration.name().toLowerCase();
		final StringBuilder sb = new StringBuilder(name.length());
		
		boolean capitalizeNext = true;
		for (int i = 0; i < name.length(); i++)
		{
			char c = name.charAt(i);
			if (c == '_')
			{
				sb.append(" ");
				capitalizeNext = true;
			}
			else if (capitalizeNext)
			{
				sb.append(Character.toUpperCase(c));
				capitalizeNext = false;
			}
			else
			{
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Parse a string value to the appropriate data type.
	 * @param value the string value to parse
	 * @return the parsed value as Boolean, Long, Integer, Float, Double, or String
	 */
	public static Object parseValue(String value)
	{
		if (value == null)
		{
			return null;
		}
		
		final String val = value.trim();
		if (val.equalsIgnoreCase("true"))
		{
			return Boolean.TRUE;
		}
		else if (val.equalsIgnoreCase("false"))
		{
			return Boolean.FALSE;
		}
		else
		{
			try
			{
				// Try Long first (to avoid Integer parsing of larger numbers).
				return Long.valueOf(val);
			}
			catch (NumberFormatException e1)
			{
				try
				{
					// Try Integer.
					return Integer.valueOf(val);
				}
				catch (NumberFormatException e2)
				{
					try
					{
						// Try Float before Double to get single-precision when possible.
						return Float.valueOf(val);
					}
					catch (NumberFormatException e3)
					{
						try
						{
							// Try Double.
							return Double.valueOf(val);
						}
						catch (NumberFormatException e4)
						{
							return val;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Parses a string as an integer, returning a default value if parsing fails.
	 * @param text the string to parse
	 * @param defaultValue the value to return if parsing fails
	 * @return the parsed integer, or the default value if parsing fails
	 */
	public static int parseInt(String text, int defaultValue)
	{
		try
		{
			return Integer.parseInt(text);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Parses the next token from a StringTokenizer as an integer, returning a default value if parsing fails or if there are no more tokens.
	 * @param tokenizer the StringTokenizer containing tokens
	 * @param defaultValue the value to return if parsing fails or if there are no more tokens
	 * @return the parsed integer, or the default value if parsing fails or if there are no tokens
	 */
	public static int parseNextInt(StringTokenizer tokenizer, int defaultValue)
	{
		if (tokenizer.hasMoreTokens())
		{
			try
			{
				final String value = tokenizer.nextToken().trim();
				return Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
				// Parsing failed, fall back to default.
			}
		}
		
		return defaultValue;
	}
	
	/**
	 * Checks if the given text contains only letters and/or numbers.
	 * @param text the text to check
	 * @return {@code true} if {@code text} contains only alphanumeric characters, {@code false} otherwise
	 */
	public static boolean isAlphaNumeric(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		for (int i = 0; i < text.length(); i++)
		{
			if (!Character.isLetterOrDigit(text.charAt(i)))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the given text contains only digits.
	 * @param text the text to check
	 * @return {@code true} if {@code text} contains only numbers, {@code false} otherwise
	 */
	public static boolean isNumeric(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		for (int i = 0; i < text.length(); i++)
		{
			if (!Character.isDigit(text.charAt(i)))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the given text represents a valid integer.
	 * @param text the text to check
	 * @return {@code true} if {@code text} is an integer, {@code false} otherwise
	 */
	public static boolean isInteger(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		try
		{
			Integer.parseInt(text);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks if the given text represents a valid float.
	 * @param text the text to check
	 * @return {@code true} if {@code text} is a float, {@code false} otherwise
	 */
	public static boolean isFloat(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		try
		{
			Float.parseFloat(text);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks if the given text represents a valid double.
	 * @param text the text to check
	 * @return {@code true} if {@code text} is a double, {@code false} otherwise
	 */
	public static boolean isDouble(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		try
		{
			Double.parseDouble(text);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks if the given text matches any constant in the specified enum type.
	 * @param name the text to check
	 * @param enumType the class of the enum
	 * @param <T> the type of the enum
	 * @return {@code true} if {@code text} is a valid enum constant, {@code false} otherwise
	 */
	public static <T extends Enum<T>> boolean isEnum(String name, Class<T> enumType)
	{
		if ((name == null) || name.isEmpty())
		{
			return false;
		}
		
		try
		{
			Enum.valueOf(enumType, name);
			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}
}
