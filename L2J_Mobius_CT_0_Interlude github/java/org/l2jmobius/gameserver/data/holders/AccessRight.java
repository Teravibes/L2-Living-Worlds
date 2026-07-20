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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author HorridoJoho, Mobius
 */
public class AccessRight
{
	private final String _command;
	private final String _description;
	private final int _level;
	private final boolean _confirmDlg;
	
	/**
	 * Initializes the access right for an admin command using a StatSet configuration.
	 * @param set the StatSet containing configuration for the admin command
	 */
	public AccessRight(StatSet set)
	{
		_command = "admin_" + set.getString("command");
		_description = set.getString("description", "");
		_confirmDlg = set.getBoolean("confirmDlg", false);
		_level = set.getInt("accessLevel", 100);
	}
	
	/**
	 * Initializes the access right for an admin command with specific values.
	 * @param command the command name (with "admin_" prefix)
	 * @param description the description associated with this command
	 * @param confirm whether the command requires confirmation before execution
	 * @param level the access level required to execute the command
	 */
	public AccessRight(String command, String description, boolean confirm, int level)
	{
		_command = command;
		_description = description;
		_confirmDlg = confirm;
		_level = level;
	}
	
	/**
	 * Gets the command associated with this access right.
	 * @return the full command string, prefixed with "admin_"
	 */
	public String getCommand()
	{
		return _command;
	}
	
	/**
	 * Gets the description associated with this command.
	 * @return the description string
	 */
	public String getDescription()
	{
		return _description;
	}
	
	/**
	 * Checks whether the admin command requires confirmation before execution.
	 * @return {@code true} if confirmation is required, {@code false} otherwise
	 */
	public boolean requireConfirm()
	{
		return _confirmDlg;
	}
	
	/**
	 * Checks whether the provided access level grants permission to use the admin command.
	 * @param playerAccessLevel the access level of the player attempting to use the command
	 * @return {@code true} if the character's access level matches or exceeds the required access level, {@code false} otherwise
	 */
	public boolean hasAccess(AccessLevel playerAccessLevel)
	{
		// Check if the required access level is null.
		final AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(_level);
		if (accessLevel == null)
		{
			return false;
		}
		
		// Check if the character's access level matches the required access level.
		if (accessLevel.getLevel() == playerAccessLevel.getLevel())
		{
			return true;
		}
		
		// Check if the character's access level has child access to the required access level.
		if (playerAccessLevel.hasChildAccess(accessLevel))
		{
			return true;
		}
		
		// If none of the above conditions are met, return false.
		return false;
	}
}
