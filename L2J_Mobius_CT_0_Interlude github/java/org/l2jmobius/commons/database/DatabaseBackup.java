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
package org.l2jmobius.commons.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.config.DatabaseConfig;

/**
 * @author Mobius
 */
public class DatabaseBackup
{
	public static void performBackup(String description)
	{
		// Delete old files.
		if (DatabaseConfig.BACKUP_DAYS > 0)
		{
			final long cut = LocalDateTime.now().minusDays(DatabaseConfig.BACKUP_DAYS).toEpochSecond(ZoneOffset.UTC);
			final Path path = Paths.get(DatabaseConfig.BACKUP_PATH);
			try
			{
				Files.list(path).filter(n ->
				{
					try
					{
						return Files.getLastModifiedTime(n).to(TimeUnit.SECONDS) < cut;
					}
					catch (Exception ex)
					{
						return false;
					}
				}).forEach(n ->
				{
					try
					{
						Files.delete(n);
					}
					catch (Exception ex)
					{
						// Ignore.
					}
				});
			}
			catch (Exception e)
			{
				// Ignore.
			}
		}
		
		// Dump to file.
		final String mysqldumpPath = System.getProperty("os.name").toLowerCase().contains("win") ? DatabaseConfig.MYSQL_BIN_PATH : "";
		try
		{
			// Java 17
			// final Process process = Runtime.getRuntime().exec(mysqldumpPath + "mysqldump -u " + Config.DATABASE_LOGIN + (Config.DATABASE_PASSWORD.trim().isEmpty() ? "" : " -p" + Config.DATABASE_PASSWORD) + " "
			// + Config.DATABASE_URL.replace("jdbc:mysql://", "").replaceAll(".*\\/|\\?.*", "") + " -r " + Config.BACKUP_PATH + description + new SimpleDateFormat("_yyyy_MM_dd_HH_mm'.sql'").format(new Date()));
			// Java 18
			final String backupFileName = DatabaseConfig.BACKUP_PATH + description + new SimpleDateFormat("_yyyy_MM_dd_HH_mm'.sql'").format(new Date());
			final String databaseName = DatabaseConfig.DATABASE_URL.replace("jdbc:mysql://", "").replaceAll(".*\\/|\\?.*", "");
			final String[] command =
			{
				mysqldumpPath + "mysqldump",
				"-u",
				DatabaseConfig.DATABASE_LOGIN,
				DatabaseConfig.DATABASE_PASSWORD.trim().isEmpty() ? "" : "-p" + DatabaseConfig.DATABASE_PASSWORD,
				databaseName,
				"-r",
				backupFileName
			};
			
			final Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
		}
		catch (Exception e)
		{
			// Ignore.
		}
	}
}
