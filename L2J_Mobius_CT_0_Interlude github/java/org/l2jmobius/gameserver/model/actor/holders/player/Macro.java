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

import java.util.List;

public class Macro
{
	private int _id;
	private final int _icon;
	private final String _name;
	private final String _descr;
	private final String _acronym;
	private final List<MacroCmd> _commands;
	
	/**
	 * Constructor for macros.
	 * @param id the macro ID
	 * @param icon the icon ID
	 * @param name the macro name
	 * @param descr the macro description
	 * @param acronym the macro acronym
	 * @param list the macro command list
	 */
	public Macro(int id, int icon, String name, String descr, String acronym, List<MacroCmd> list)
	{
		_id = id;
		_icon = icon;
		_name = name;
		_descr = descr;
		_acronym = acronym;
		_commands = list;
	}
	
	/**
	 * Gets the macro ID.
	 * @return the macro ID
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Sets the macro ID.
	 * @param id the macro ID
	 */
	public void setId(int id)
	{
		_id = id;
	}
	
	/**
	 * Gets the macro icon ID.
	 * @return the icon
	 */
	public int getIcon()
	{
		return _icon;
	}
	
	/**
	 * Gets the macro name.
	 * @return the name
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Gets the macro description.
	 * @return the description
	 */
	public String getDescr()
	{
		return _descr;
	}
	
	/**
	 * Gets the macro acronym.
	 * @return the acronym
	 */
	public String getAcronym()
	{
		return _acronym;
	}
	
	/**
	 * Gets the macro command list.
	 * @return the macro command list
	 */
	public List<MacroCmd> getCommands()
	{
		return _commands;
	}
}
