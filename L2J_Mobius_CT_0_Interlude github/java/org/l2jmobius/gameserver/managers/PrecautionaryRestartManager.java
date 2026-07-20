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

import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.actor.instance.RaidBoss;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius
 */
public class PrecautionaryRestartManager
{
	private static final Logger LOGGER = Logger.getLogger(PrecautionaryRestartManager.class.getName());
	
	private static final String SYSTEM_CPU_LOAD_VAR = "SystemCpuLoad";
	private static final String PROCESS_CPU_LOAD_VAR = "ProcessCpuLoad";
	
	private static boolean _restarting = false;
	
	protected PrecautionaryRestartManager()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			if (_restarting)
			{
				return;
			}
			
			if (ServerConfig.PRECAUTIONARY_RESTART_CPU && (getCpuLoad(SYSTEM_CPU_LOAD_VAR) > ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE))
			{
				if (serverBizzy())
				{
					return;
				}
				
				LOGGER.info("PrecautionaryRestartManager: CPU usage over " + ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
				LOGGER.info("PrecautionaryRestartManager: Server is using " + getCpuLoad(PROCESS_CPU_LOAD_VAR) + "%.");
				Broadcast.toAllOnlinePlayers("Server will restart in 10 minutes.", false);
				Shutdown.getInstance().startShutdown(null, 600, true);
			}
			
			if (ServerConfig.PRECAUTIONARY_RESTART_MEMORY && (getProcessRamLoad() > ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE))
			{
				if (serverBizzy())
				{
					return;
				}
				
				LOGGER.info("PrecautionaryRestartManager: Memory usage over " + ServerConfig.PRECAUTIONARY_RESTART_PERCENTAGE + "%.");
				Broadcast.toAllOnlinePlayers("Server will restart in 10 minutes.", false);
				Shutdown.getInstance().startShutdown(null, 600, true);
			}
		}, ServerConfig.PRECAUTIONARY_RESTART_DELAY, ServerConfig.PRECAUTIONARY_RESTART_DELAY);
	}
	
	private static double getCpuLoad(String var)
	{
		try
		{
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			final ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			final AttributeList list = mbs.getAttributes(name, new String[]
			{
				var
			});
			
			if (list.isEmpty())
			{
				return 0;
			}
			
			final Attribute att = (Attribute) list.get(0);
			final Double value = (Double) att.getValue();
			if (value == -1)
			{
				return 0;
			}
			
			return (value * 1000) / 10d;
		}
		catch (Exception e)
		{
		}
		
		return 0;
	}
	
	private static double getProcessRamLoad()
	{
		final Runtime runTime = Runtime.getRuntime();
		final long totalMemory = runTime.maxMemory();
		final long usedMemory = totalMemory - ((totalMemory - runTime.totalMemory()) + runTime.freeMemory());
		return (usedMemory * 100) / totalMemory;
	}
	
	private boolean serverBizzy()
	{
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if ((castle != null) && castle.getSiege().isInProgress())
			{
				return true;
			}
		}
		
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player == null) || player.isInOfflineMode())
			{
				continue;
			}
			
			if (player.isInOlympiadMode())
			{
				return true;
			}
			
			if (player.isOnEvent())
			{
				return true;
			}
			
			if (player.getInstanceId() > 0)
			{
				return true;
			}
			
			final WorldObject target = player.getTarget();
			if ((target instanceof RaidBoss) || (target instanceof GrandBoss))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void restartEnabled()
	{
		_restarting = true;
	}
	
	public void restartAborted()
	{
		_restarting = false;
	}
	
	public static PrecautionaryRestartManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PrecautionaryRestartManager INSTANCE = new PrecautionaryRestartManager();
	}
}
