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

import org.l2jmobius.gameserver.model.Location;

/**
 * Defines a single walking node for NPC routes.<br>
 * Extends {@link Location} and adds route execution metadata.
 * <ul>
 * <li>Delay tracking for per-node wait time.</li>
 * <li>Movement mode (walk/run) selection.</li>
 * <li>Optional chat text handling.</li>
 * </ul>
 * @author BazookaRpm
 */
public class WalkNode extends Location
{
	// Constants.
	private static final int NO_DELAY = 0;
	private static final byte MOVE_WALK = 0;
	private static final byte MOVE_RUN = 1;
	private static final byte CHAT_EMPTY = 0;
	private static final byte CHAT_PRESENT = 1;
	private static final String EMPTY_TEXT = "";
	
	// Node Data.
	private final int _delayMs;
	private final byte _moveMode;
	private final byte _chatState;
	private final String _chatText;
	
	/**
	 * Creates a new walking node.<br>
	 * Coordinates are stored in {@link Location} and metadata locally.
	 * @param moveX X coordinate.
	 * @param moveY Y coordinate.
	 * @param moveZ Z coordinate.
	 * @param delay Delay in milliseconds.
	 * @param runToLocation {@code true} to run to the node.
	 * @param chatText Optional chat text, or {@code null}.
	 */
	public WalkNode(int moveX, int moveY, int moveZ, int delay, boolean runToLocation, String chatText)
	{
		super(moveX, moveY, moveZ);
		
		_delayMs = delay;
		
		byte mode;
		if (runToLocation)
		{
			mode = MOVE_RUN;
		}
		else
		{
			mode = MOVE_WALK;
		}
		_moveMode = mode;
		
		String text = chatText;
		if (text == null)
		{
			text = EMPTY_TEXT;
		}
		
		byte state;
		if (text.isEmpty())
		{
			state = CHAT_EMPTY;
		}
		else
		{
			state = CHAT_PRESENT;
		}
		_chatState = state;
		_chatText = text;
	}
	
	/**
	 * Gets the configured delay.
	 * @return The delay
	 */
	public int getDelay()
	{
		final int value = _delayMs;
		if (value == NO_DELAY)
		{
			return NO_DELAY;
		}
		
		return value;
	}
	
	/**
	 * Checks whether the node should be reached running.
	 * @return {@code true} if running
	 */
	public boolean runToLocation()
	{
		return _moveMode == MOVE_RUN;
	}
	
	/**
	 * Gets the chat text for this node.
	 * @return The chat text
	 */
	public String getChatText()
	{
		if (_chatState == CHAT_PRESENT)
		{
			return _chatText;
		}
		
		return EMPTY_TEXT;
	}
}
