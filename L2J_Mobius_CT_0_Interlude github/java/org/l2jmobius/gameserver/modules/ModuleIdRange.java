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
package org.l2jmobius.gameserver.modules;

/**
 * An inclusive id range a module reserves for a resource type, for example items 60000 to 60009. Both ends are
 * inclusive, so a single id is a range with equal ends. Used by the startup validation to detect when two modules claim
 * overlapping ids.
 *
 * @param low the first id in the range, inclusive
 * @param high the last id in the range, inclusive; never less than {@code low}
 */
public record ModuleIdRange(int low, int high)
{
	/**
	 * @param other another range
	 * @return {@code true} when this range and {@code other} share at least one id
	 */
	public boolean overlaps(ModuleIdRange other)
	{
		return (low <= other.high) && (other.low <= high);
	}

	/**
	 * @param id an id
	 * @return {@code true} when the id falls inside this range
	 */
	public boolean contains(int id)
	{
		return (id >= low) && (id <= high);
	}

	@Override
	public String toString()
	{
		return (low == high) ? Integer.toString(low) : (low + "-" + high);
	}
}
