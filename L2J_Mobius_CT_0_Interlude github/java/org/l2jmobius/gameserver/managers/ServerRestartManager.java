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
package org.l2jmobius.gameserver.managers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.config.ServerConfig;

/**
 * @author Gigi, Mobius
 */
public class ServerRestartManager
{
	static final Logger LOGGER = Logger.getLogger(ServerRestartManager.class.getName());
	
	private String nextRestartTime = "unknown";
	
	protected ServerRestartManager()
	{
		try
		{
			final Calendar currentTime = Calendar.getInstance();
			final Calendar restartTime = Calendar.getInstance();
			Calendar lastRestart = null;
			long delay = 0;
			long lastDelay = 0;
			
			for (String scheduledTime : ServerConfig.SERVER_RESTART_SCHEDULE)
			{
				final String[] splitTime = scheduledTime.trim().split(":");
				restartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTime[0]));
				restartTime.set(Calendar.MINUTE, Integer.parseInt(splitTime[1]));
				restartTime.set(Calendar.SECOND, 00);
				
				if (restartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					restartTime.add(Calendar.DAY_OF_WEEK, 1);
				}
				
				if (!ServerConfig.SERVER_RESTART_DAYS.isEmpty())
				{
					while (!ServerConfig.SERVER_RESTART_DAYS.contains(restartTime.get(Calendar.DAY_OF_WEEK)))
					{
						restartTime.add(Calendar.DAY_OF_WEEK, 1);
					}
				}
				
				delay = restartTime.getTimeInMillis() - currentTime.getTimeInMillis();
				if (lastDelay == 0)
				{
					lastDelay = delay;
					lastRestart = restartTime;
				}
				
				if (delay < lastDelay)
				{
					lastDelay = delay;
					lastRestart = restartTime;
				}
			}
			
			if (lastRestart != null)
			{
				if (ServerConfig.SERVER_RESTART_DAYS.isEmpty() || (ServerConfig.SERVER_RESTART_DAYS.size() == 7))
				{
					nextRestartTime = new SimpleDateFormat("HH:mm").format(lastRestart.getTime());
				}
				else
				{
					nextRestartTime = new SimpleDateFormat("MMMM d'" + getDayNumberSuffix(lastRestart.get(Calendar.DAY_OF_MONTH)) + "' HH:mm", Locale.UK).format(lastRestart.getTime());
				}
				
				ThreadPool.schedule(new ServerRestartTask(), lastDelay - (ServerConfig.SERVER_RESTART_SCHEDULE_COUNTDOWN * 1000));
				LOGGER.info("Scheduled server restart at " + lastRestart.getTime() + ".");
			}
		}
		catch (Exception e)
		{
			LOGGER.info("The scheduled server restart config is not set properly, please correct it!");
		}
	}
	
	private String getDayNumberSuffix(int day)
	{
		switch (day)
		{
			case 1:
			case 21:
			case 31:
			{
				return "st";
			}
			case 2:
			case 22:
			{
				return "nd";
			}
			case 3:
			case 23:
			{
				return "rd";
			}
			default:
			{
				return "th";
			}
		}
	}
	
	public String getNextRestartTime()
	{
		return nextRestartTime;
	}
	
	class ServerRestartTask implements Runnable
	{
		@Override
		public void run()
		{
			Shutdown.getInstance().startShutdown(null, ServerConfig.SERVER_RESTART_SCHEDULE_COUNTDOWN, true);
		}
	}
	
	public static ServerRestartManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ServerRestartManager INSTANCE = new ServerRestartManager();
	}
}
