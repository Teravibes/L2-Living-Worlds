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
package org.l2jmobius.commons.config;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the database related configurations.
 * @author Mobius
 */
public class DatabaseConfig
{
	// File
	private static final String DATABASE_CONFIG_FILE = "./config/Database.ini";
	
	// Constants
	public static String DATABASE_DRIVER;
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	public static boolean DATABASE_TEST_CONNECTIONS;
	public static boolean BACKUP_DATABASE;
	public static String MYSQL_BIN_PATH;
	public static String BACKUP_PATH;
	public static int BACKUP_DAYS;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(DATABASE_CONFIG_FILE);
		DATABASE_DRIVER = config.getString("Driver", "com.mysql.cj.jdbc.Driver");
		DATABASE_URL = config.getString("URL", "jdbc:mysql://localhost/l2jmobius");
		DATABASE_LOGIN = config.getString("Login", "root");
		DATABASE_PASSWORD = config.getString("Password", "");
		DATABASE_MAX_CONNECTIONS = config.getInt("MaximumDatabaseConnections", 10);
		DATABASE_TEST_CONNECTIONS = config.getBoolean("TestDatabaseConnections", false);
		BACKUP_DATABASE = config.getBoolean("BackupDatabase", false);
		MYSQL_BIN_PATH = config.getString("MySqlBinLocation", "C:/xampp/mysql/bin/");
		BACKUP_PATH = config.getString("BackupPath", "../backup/");
		BACKUP_DAYS = config.getInt("BackupDays", 30);
	}
}
