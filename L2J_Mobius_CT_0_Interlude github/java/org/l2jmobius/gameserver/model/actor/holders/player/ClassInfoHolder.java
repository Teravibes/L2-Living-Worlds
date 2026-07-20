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

import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * A holder containing information about a player class.
 * @author Mobius
 */
public class ClassInfoHolder
{
	private final PlayerClass _playerClass;
	private final PlayerClass _parentClass;
	private final String _className;
	
	/**
	 * Constructs a new ClassInfoHolder with the specified class information.
	 * @param playerClass the {@link PlayerClass} representing the player class
	 * @param parentClass the parent {@link PlayerClass} from which this class evolves, or {@code null} if this is a base class
	 * @param className the display name of the class
	 */
	public ClassInfoHolder(PlayerClass playerClass, PlayerClass parentClass, String className)
	{
		_playerClass = playerClass;
		_parentClass = parentClass;
		_className = className;
	}
	
	/**
	 * Gets the player class type.
	 * @return the {@link PlayerClass} representing the player current class
	 */
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	/**
	 * Gets the parent class of this player class.
	 * @return the parent {@link PlayerClass}, or {@code null} if this is a base class
	 */
	public PlayerClass getParentClass()
	{
		return _parentClass;
	}
	
	/**
	 * Gets the display name of the player class.
	 * @return the class name as a String
	 */
	public String getClassName()
	{
		return _className;
	}
}
