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
 * This class loads all the threadpool related configurations.
 * @author Mobius
 */
public class ThreadConfig
{
	// File
	private static final String THREADS_CONFIG_FILE = "./config/Threads.ini";
	
	// Constants
	public static int SCHEDULED_THREAD_POOL_SIZE;
	public static int HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE;
	public static int INSTANT_THREAD_POOL_SIZE;
	public static boolean THREADS_FOR_LOADING;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(THREADS_CONFIG_FILE);
		
		SCHEDULED_THREAD_POOL_SIZE = config.getInt("ScheduledThreadPoolSize", -1);
		if (SCHEDULED_THREAD_POOL_SIZE == -1)
		{
			SCHEDULED_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 4;
		}
		
		INSTANT_THREAD_POOL_SIZE = config.getInt("InstantThreadPoolSize", -1);
		if (INSTANT_THREAD_POOL_SIZE == -1)
		{
			INSTANT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
		}
		
		if ((SCHEDULED_THREAD_POOL_SIZE > 2) && (INSTANT_THREAD_POOL_SIZE > 2))
		{
			HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE = Math.max(2, SCHEDULED_THREAD_POOL_SIZE / 4);
		}
		else
		{
			HIGH_PRIORITY_SCHEDULED_THREAD_POOL_SIZE = 0;
		}
		
		if (config.containsKey("ThreadsForLoading"))
		{
			THREADS_FOR_LOADING = config.getBoolean("ThreadsForLoading", false);
		}
		else
		{
			THREADS_FOR_LOADING = false;
		}
	}
}
