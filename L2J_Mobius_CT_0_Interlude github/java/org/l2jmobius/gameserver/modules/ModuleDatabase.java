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

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * Runs a module's install SQL script against the server database when the module is enabled. A module that needs storage
 * ships additive, namespaced tables and lists them in its manifest; the script is expected to be idempotent (for example
 * {@code CREATE TABLE IF NOT EXISTS}), since it runs on every enable. The platform provides only this install path: it
 * never drops a module's tables on its own, because those tables can hold player progress (framework section 3.5). The
 * opt-in cleanup script is run by an explicit action, not from here.
 */
public final class ModuleDatabase
{
	private ModuleDatabase()
	{
	}

	/**
	 * Executes an install script one statement at a time, using the server's connection pool. A single failing statement
	 * fails the whole install, which refuses the module, so the script must be safe to run against a database that may
	 * already hold its tables.
	 * @param moduleId the module the script belongs to, for messages
	 * @param sqlFile the resolved path to the script
	 * @throws ModuleException if the script is missing or any statement fails
	 */
	static void runScript(String moduleId, Path sqlFile) throws ModuleException
	{
		final File file = sqlFile.toFile();
		if (!file.isFile())
		{
			throw new ModuleException("Module '" + moduleId + "' declares a database script that does not exist: " + sqlFile + ".");
		}

		try (Connection connection = DatabaseFactory.getConnection();
			Statement statement = connection.createStatement();
			Scanner scanner = new Scanner(file))
		{
			final StringBuilder sb = new StringBuilder();
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine().trim();
				if (line.isEmpty() || line.startsWith("--"))
				{
					continue;
				}
				if (line.contains("--"))
				{
					line = line.split("--")[0].trim();
				}

				sb.append(line).append(' ');
				if (line.endsWith(";"))
				{
					final String sql = sb.toString().trim();
					sb.setLength(0);
					if (!sql.isEmpty())
					{
						statement.execute(sql);
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new ModuleException("Module '" + moduleId + "' database install failed: " + e.getMessage(), e);
		}
	}
}
