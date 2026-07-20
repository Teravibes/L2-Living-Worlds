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
package org.l2jmobius.gameserver.util;

/**
 * Utility class for basic arithmetic operations with support for byte, short, int, and double types.<br>
 * Provides methods for addition, multiplication, division, and clamping values.
 * @author Mobius
 */
public class MathUtil
{
	/**
	 * Adds two byte values.
	 * @param a the first byte value
	 * @param b the second byte value
	 * @return the sum of {@code a} and {@code b} as a byte
	 */
	public static byte add(byte a, byte b)
	{
		return (byte) (a + b);
	}
	
	/**
	 * Adds two short values.
	 * @param a the first short value
	 * @param b the second short value
	 * @return the sum of {@code a} and {@code b} as a short
	 */
	public static short add(short a, short b)
	{
		return (short) (a + b);
	}
	
	/**
	 * Adds two int values.
	 * @param a the first int value
	 * @param b the second int value
	 * @return the sum of {@code a} and {@code b} as an int
	 */
	public static int add(int a, int b)
	{
		return a + b;
	}
	
	/**
	 * Adds two double values.
	 * @param a the first double value
	 * @param b the second double value
	 * @return the sum of {@code a} and {@code b} as a double
	 */
	public static double add(double a, double b)
	{
		return a + b;
	}
	
	/**
	 * Multiplies two byte values.
	 * @param a the first byte value
	 * @param b the second byte value
	 * @return the product of {@code a} and {@code b} as a byte
	 */
	public static byte mul(byte a, byte b)
	{
		return (byte) (a * b);
	}
	
	/**
	 * Multiplies two short values.
	 * @param a the first short value
	 * @param b the second short value
	 * @return the product of {@code a} and {@code b} as a short
	 */
	public static short mul(short a, short b)
	{
		return (short) (a * b);
	}
	
	/**
	 * Multiplies two int values.
	 * @param a the first int value
	 * @param b the second int value
	 * @return the product of {@code a} and {@code b} as an int
	 */
	public static int mul(int a, int b)
	{
		return a * b;
	}
	
	/**
	 * Multiplies two double values.
	 * @param a the first double value
	 * @param b the second double value
	 * @return the product of {@code a} and {@code b} as a double
	 */
	public static double mul(double a, double b)
	{
		return a * b;
	}
	
	/**
	 * Divides one byte by another.
	 * @param a the dividend byte value
	 * @param b the divisor byte value
	 * @return the result of {@code a} divided by {@code b} as a byte
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static byte div(byte a, byte b)
	{
		if (b == 0)
		{
			throw new ArithmeticException("Division by zero is not allowed for byte values.");
		}
		
		return (byte) (a / b);
	}
	
	/**
	 * Divides one short by another.
	 * @param a the dividend short value
	 * @param b the divisor short value
	 * @return the result of {@code a} divided by {@code b} as a short
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static short div(short a, short b)
	{
		if (b == 0)
		{
			throw new ArithmeticException("Division by zero is not allowed for short values.");
		}
		
		return (short) (a / b);
	}
	
	/**
	 * Divides one int by another.
	 * @param a the dividend int value
	 * @param b the divisor int value
	 * @return the result of {@code a} divided by {@code b} as an int
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static int div(int a, int b)
	{
		if (b == 0)
		{
			throw new ArithmeticException("Division by zero is not allowed for int values.");
		}
		
		return a / b;
	}
	
	/**
	 * Divides one double by another.
	 * @param a the dividend double value
	 * @param b the divisor double value
	 * @return the result of {@code a} divided by {@code b} as a double
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static double div(double a, double b)
	{
		if (b == 0d)
		{
			throw new ArithmeticException("Division by zero is not allowed for double values.");
		}
		
		return a / b;
	}
	
	/**
	 * Clamps an integer to a specified range.
	 * @param value the value to clamp
	 * @param min the minimum allowable value
	 * @param max the maximum allowable value
	 * @return the clamped value, ensuring it is within the range [{@code min}, {@code max}]
	 */
	public static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}
	
	/**
	 * Clamps a long to a specified range.
	 * @param value the long value to clamp
	 * @param min the minimum allowable value
	 * @param max the maximum allowable value
	 * @return the clamped value, ensuring it is within the range [{@code min}, {@code max}]
	 */
	public static long clamp(long value, long min, long max)
	{
		return Math.max(min, Math.min(max, value));
	}
	
	/**
	 * Clamps a double to a specified range.
	 * @param value the double value to clamp
	 * @param min the minimum allowable value
	 * @param max the maximum allowable value
	 * @return the clamped value, ensuring it is within the range [{@code min}, {@code max}]
	 */
	public static double clamp(double value, double min, double max)
	{
		return Math.max(min, Math.min(max, value));
	}
	
	/**
	 * Scales an integer value from one range to another.
	 * @param value the value to scale
	 * @param sourceMin the minimum of the source range
	 * @param sourceMax the maximum of the source range
	 * @param targetMin the minimum of the target range
	 * @param targetMax the maximum of the target range
	 * @return the scaled value within the target range
	 */
	public static int scaleToRange(int value, int sourceMin, int sourceMax, int targetMin, int targetMax)
	{
		final int clampedValue = clamp(value, sourceMin, sourceMax);
		return (((clampedValue - sourceMin) * (targetMax - targetMin)) / (sourceMax - sourceMin)) + targetMin;
	}
	
	/**
	 * Scales a long value from one range to another.
	 * @param value the value to scale
	 * @param sourceMin the minimum of the source range
	 * @param sourceMax the maximum of the source range
	 * @param targetMin the minimum of the target range
	 * @param targetMax the maximum of the target range
	 * @return the scaled value within the target range
	 */
	public static long scaleToRange(long value, long sourceMin, long sourceMax, long targetMin, long targetMax)
	{
		final long clampedValue = clamp(value, sourceMin, sourceMax);
		return (((clampedValue - sourceMin) * (targetMax - targetMin)) / (sourceMax - sourceMin)) + targetMin;
	}
	
	/**
	 * Scales a double value from one range to another.
	 * @param value the value to scale
	 * @param sourceMin the minimum of the source range
	 * @param sourceMax the maximum of the source range
	 * @param targetMin the minimum of the target range
	 * @param targetMax the maximum of the target range
	 * @return the scaled value within the target range
	 */
	public static double scaleToRange(double value, double sourceMin, double sourceMax, double targetMin, double targetMax)
	{
		final double clampedValue = clamp(value, sourceMin, sourceMax);
		return (((clampedValue - sourceMin) * (targetMax - targetMin)) / (sourceMax - sourceMin)) + targetMin;
	}
	
	/**
	 * Returns the index of the minimum value in an array of integers.
	 * @param array the array of integers to search
	 * @return the index of the minimum value
	 */
	public static int getIndexOfMinValue(int... array)
	{
		int minIndex = 0;
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] < array[minIndex])
			{
				minIndex = i;
			}
		}
		
		return minIndex;
	}
	
	/**
	 * Returns the index of the maximum value in an array of integers.
	 * @param array the array of integers to search
	 * @return the index of the maximum value
	 */
	public static int getIndexOfMaxValue(int... array)
	{
		int maxIndex = 0;
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > array[maxIndex])
			{
				maxIndex = i;
			}
		}
		
		return maxIndex;
	}
	
	/**
	 * Finds the minimum value in an array of integers.
	 * @param values the array of integers
	 * @return the minimum value in the array
	 */
	public static int min(int... values)
	{
		int minValue = values[0];
		for (int value : values)
		{
			if (value < minValue)
			{
				minValue = value;
			}
		}
		
		return minValue;
	}
	
	/**
	 * Finds the maximum value in an array of integers.
	 * @param values the array of integers
	 * @return the maximum value in the array
	 */
	public static int max(int... values)
	{
		int maxValue = values[0];
		for (int value : values)
		{
			if (value > maxValue)
			{
				maxValue = value;
			}
		}
		
		return maxValue;
	}
	
	/**
	 * Finds the minimum value in an array of longs.
	 * @param values the array of longs
	 * @return the minimum value in the array
	 */
	public static long min(long... values)
	{
		long minValue = values[0];
		for (long value : values)
		{
			if (value < minValue)
			{
				minValue = value;
			}
		}
		
		return minValue;
	}
	
	/**
	 * Finds the maximum value in an array of longs.
	 * @param values the array of longs
	 * @return the maximum value in the array
	 */
	public static long max(long... values)
	{
		long maxValue = values[0];
		for (long value : values)
		{
			if (value > maxValue)
			{
				maxValue = value;
			}
		}
		
		return maxValue;
	}
	
	/**
	 * Finds the minimum value in an array of doubles.
	 * @param values the array of doubles
	 * @return the minimum value in the array
	 */
	public static double min(double... values)
	{
		double minValue = values[0];
		for (double value : values)
		{
			if (value < minValue)
			{
				minValue = value;
			}
		}
		
		return minValue;
	}
	
	/**
	 * Finds the maximum value in an array of doubles.
	 * @param values the array of doubles
	 * @return the maximum value in the array
	 */
	public static double max(double... values)
	{
		double maxValue = values[0];
		for (double value : values)
		{
			if (value > maxValue)
			{
				maxValue = value;
			}
		}
		
		return maxValue;
	}
}
