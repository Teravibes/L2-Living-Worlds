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

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Mobius
 */
public class Rnd
{
	private static final int MINIMUM_POSITIVE_INT = 1;
	private static final long MINIMUM_POSITIVE_LONG = 1L;
	private static final double MINIMUM_POSITIVE_DOUBLE = Double.longBitsToDouble(0x1L);
	
	/**
	 * @return a random boolean value.
	 */
	public static boolean nextBoolean()
	{
		return ThreadLocalRandom.current().nextBoolean();
	}
	
	/**
	 * Generates random bytes and places them into a user-supplied byte array. The number of random bytes produced is equal to the length of the byte array.
	 * @param bytes the byte array to fill with random bytes.
	 */
	public static void nextBytes(byte[] bytes)
	{
		ThreadLocalRandom.current().nextBytes(bytes);
	}
	
	/**
	 * @param bound (int)
	 * @return a random int value between zero (inclusive) and the specified bound (exclusive).
	 */
	public static int get(int bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextInt(bound);
	}
	
	/**
	 * @param origin (int)
	 * @param bound (int)
	 * @return a random int value between the specified origin (inclusive) and the specified bound (inclusive).
	 */
	public static int get(int origin, int bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextInt(origin, bound == Integer.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_INT);
	}
	
	/**
	 * @return a random int value.
	 */
	public static int nextInt()
	{
		return ThreadLocalRandom.current().nextInt();
	}
	
	/**
	 * @param bound (long)
	 * @return a random long value between zero (inclusive) and the specified bound (exclusive).
	 */
	public static long get(long bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextLong(bound);
	}
	
	/**
	 * @param origin (long)
	 * @param bound (long)
	 * @return a random long value between the specified origin (inclusive) and the specified bound (inclusive).
	 */
	public static long get(long origin, long bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextLong(origin, bound == Long.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_LONG);
	}
	
	/**
	 * @return a random long value.
	 */
	public static long nextLong()
	{
		return ThreadLocalRandom.current().nextLong();
	}
	
	/**
	 * @param bound (double)
	 * @return a random double value between zero (inclusive) and the specified bound (exclusive).
	 */
	public static double get(double bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextDouble(bound);
	}
	
	/**
	 * @param origin (double)
	 * @param bound (double)
	 * @return a random double value between the specified origin (inclusive) and the specified bound (inclusive).
	 */
	public static double get(double origin, double bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextDouble(origin, bound == Double.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_DOUBLE);
	}
	
	/**
	 * @return a random double value between zero (inclusive) and one (exclusive).
	 */
	public static double nextDouble()
	{
		return ThreadLocalRandom.current().nextDouble();
	}
	
	/**
	 * @return the next random, Gaussian ("normally") distributed double value with mean 0.0 and standard deviation 1.0 from this random number generator's sequence.
	 */
	public static double nextGaussian()
	{
		return ThreadLocalRandom.current().nextGaussian();
	}
}
