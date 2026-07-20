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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Audits Game Master's actions.
 */
public class GMAudit
{
	private static final Logger LOGGER = Logger.getLogger("gmaudit");
	static
	{
		new File("log/GMAudit").mkdirs();
	}
	
	// List of characters not allowed in file names.
	private static final char[] ILLEGAL_CHARACTERS =
	{
		// @formatter:off
		'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'
		// @formatter:on
	};
	
	/**
	 * Logs a Game Master's action to a dedicated file. If the Game Master's name contains invalid characters, they are replaced with underscores.<br>
	 * If the name is still invalid, it is replaced with "INVALID_GM_NAME_<current date>".
	 * @param gmName the name of the Game Master performing the action
	 * @param action the action performed by the Game Master
	 * @param target the target's name associated with the action
	 * @param params additional parameters or details related to the action
	 */
	public static void logAction(String gmName, String action, String target, String params)
	{
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
		final String timestamp = dateFormat.format(new Date());
		
		// Sanitize the GM's name for the filename.
		String sanitizedGmName = sanitizeFileName(gmName);
		if (!isValidFileName(sanitizedGmName))
		{
			sanitizedGmName = "INVALID_GM_NAME_" + timestamp;
		}
		
		// Create the audit file and log the action.
		final File logFile = new File("log/GMAudit/" + sanitizedGmName + ".txt");
		try (FileWriter writer = new FileWriter(logFile, true))
		{
			writer.write(timestamp + ">" + gmName + ">" + action + ">" + target + ">" + params + System.lineSeparator());
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "Could not save GMAudit log for GM " + gmName + ":", e);
		}
	}
	
	/**
	 * Logs a Game Master's action to a dedicated file without additional parameters.
	 * @param gmName the name of the Game Master performing the action
	 * @param action the action performed by the Game Master
	 * @param target the target's name associated with the action
	 */
	public static void logAction(String gmName, String action, String target)
	{
		logAction(gmName, action, target, "");
	}
	
	/**
	 * Replaces any illegal characters in a string with underscores to make it suitable for use in filenames.
	 * @param name the string to sanitize
	 * @return a sanitized version of the string, with illegal characters replaced by underscores
	 */
	private static String sanitizeFileName(String name)
	{
		String sanitized = name;
		for (char illegalChar : ILLEGAL_CHARACTERS)
		{
			sanitized = sanitized.replace(illegalChar, '_');
		}
		
		return sanitized;
	}
	
	/**
	 * Checks if a filename is valid by attempting to resolve its canonical path.
	 * @param name the file name to validate
	 * @return {@code true} if the file name is valid, {@code false} otherwise
	 */
	private static boolean isValidFileName(String name)
	{
		final File file = new File(name);
		try
		{
			file.getCanonicalPath();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}
}
