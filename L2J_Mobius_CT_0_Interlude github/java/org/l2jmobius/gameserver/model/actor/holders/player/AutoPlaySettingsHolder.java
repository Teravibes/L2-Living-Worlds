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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mobius
 */
public class AutoPlaySettingsHolder
{
	private final AtomicInteger _options = new AtomicInteger();
	private final AtomicBoolean _pickup = new AtomicBoolean(true);
	private final AtomicInteger _nextTargetMode = new AtomicInteger(1);
	private final AtomicBoolean _shortRange = new AtomicBoolean();
	private final AtomicBoolean _respectfulHunting = new AtomicBoolean(true);
	private final AtomicInteger _autoPotionPercent = new AtomicInteger(50);
	
	public AutoPlaySettingsHolder()
	{
	}
	
	public int getOptions()
	{
		return _options.get();
	}
	
	public void setOptions(int options)
	{
		_options.set(options);
	}
	
	public boolean doPickup()
	{
		return _pickup.get();
	}
	
	public void setPickup(boolean value)
	{
		_pickup.set(value);
	}
	
	public int getNextTargetMode()
	{
		return _nextTargetMode.get();
	}
	
	public void setNextTargetMode(int nextTargetMode)
	{
		_nextTargetMode.set(nextTargetMode);
	}
	
	public boolean isShortRange()
	{
		return _shortRange.get();
	}
	
	public void setShortRange(boolean value)
	{
		_shortRange.set(value);
	}
	
	public boolean isRespectfulHunting()
	{
		return _respectfulHunting.get();
	}
	
	public void setRespectfulHunting(boolean value)
	{
		_respectfulHunting.set(value);
	}
	
	public int getAutoPotionPercent()
	{
		return _autoPotionPercent.get();
	}
	
	public void setAutoPotionPercent(int value)
	{
		_autoPotionPercent.set(value);
	}
}
