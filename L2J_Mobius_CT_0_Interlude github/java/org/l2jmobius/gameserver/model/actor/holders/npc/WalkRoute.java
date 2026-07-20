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
package org.l2jmobius.gameserver.model.actor.holders.npc;

import java.util.List;
import java.util.Objects;

/**
 * Defines an NPC walking route.<br>
 * Stores an ordered node list and repeat policy.
 * <ul>
 * <li>Fail-fast validation for route name and node list.</li>
 * <li>Repeat is enabled only for repeat types 0..2.</li>
 * <li>Provides helpers for route traversal.</li>
 * </ul>
 * @author BazookaRpm
 */
public class WalkRoute
{
	// Constants.
	private static final byte REPEAT_MIN = 0;
	private static final byte REPEAT_MID = 1;
	private static final byte REPEAT_MAX = 2;
	
	// Route Definition.
	private final String _name;
	private final List<WalkNode> _nodeList;
	
	// Repeat Policy.
	private final boolean _repeatWalk;
	private boolean _stopAfterCycle;
	private final byte _repeatType;
	
	public WalkRoute(String name, List<WalkNode> route, boolean repeat, byte repeatType)
	{
		_name = Objects.requireNonNull(name, "name");
		_nodeList = Objects.requireNonNull(route, "route");
		
		if (_nodeList.isEmpty())
		{
			throw new IllegalArgumentException("WalkRoute node list is empty: " + _name);
		}
		
		for (int i = 0; i < _nodeList.size(); i++)
		{
			if (_nodeList.get(i) == null)
			{
				throw new IllegalArgumentException("WalkRoute has null node at index " + i + ": " + _name);
			}
		}
		
		_repeatType = repeatType;
		
		boolean supported = false;
		switch (_repeatType)
		{
			case REPEAT_MIN:
			case REPEAT_MID:
			case REPEAT_MAX:
			{
				supported = true;
				break;
			}
			default:
			{
				supported = false;
				break;
			}
		}
		
		if ((_repeatType < REPEAT_MIN) || (_repeatType > REPEAT_MAX))
		{
			supported = false;
		}
		
		_repeatWalk = repeat && supported;
	}
	
	public boolean repeatWalk()
	{
		return _repeatWalk;
	}
	
	public byte getRepeatType()
	{
		return _repeatType;
	}
	
	public boolean doOnce()
	{
		return _stopAfterCycle;
	}
	
	public WalkNode getLastNode()
	{
		final int lastIndex = _nodeList.size() - 1;
		return _nodeList.get(lastIndex);
	}
	
	public int getNodesCount()
	{
		return _nodeList.size();
	}
	
	public List<WalkNode> getNodeList()
	{
		return _nodeList;
	}
	
	public String getName()
	{
		return _name;
	}
}
