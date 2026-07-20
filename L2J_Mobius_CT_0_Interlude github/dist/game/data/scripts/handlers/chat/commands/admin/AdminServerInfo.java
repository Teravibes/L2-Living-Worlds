/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.chat.commands.admin;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * @author St3eT
 */
public class AdminServerInfo implements IAdminCommandHandler
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("hh:mm a");
	private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
	private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_serverinfo"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_serverinfo"))
		{
			final MemoryUsage heapMemoryUsage = MEMORY_MX_BEAN.getHeapMemoryUsage();
			final long freeMemory = heapMemoryUsage.getMax() - heapMemoryUsage.getUsed();
			final int threadCount = THREAD_MX_BEAN.getThreadCount();
			final int daemonCount = THREAD_MX_BEAN.getDaemonThreadCount();
			final int nonDaemonCount = threadCount - daemonCount;
			final int peakCount = THREAD_MX_BEAN.getPeakThreadCount();
			final long totalCount = THREAD_MX_BEAN.getTotalStartedThreadCount();
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/serverinfo.htm"));
			html.replace("%os_name%", System.getProperty("os.name"));
			html.replace("%os_ver%", System.getProperty("os.version"));
			html.replace("%slots%", getPlayersCount("ALL") + "/" + ServerConfig.MAXIMUM_ONLINE_USERS);
			html.replace("%gameTime%", GameTimeTaskManager.getInstance().getGameHour() + ":" + GameTimeTaskManager.getInstance().getGameMinute());
			html.replace("%dayNight%", GameTimeTaskManager.getInstance().isNight() ? "Night" : "Day");
			html.replace("%geodata%", GeoEngineConfig.PATHFINDING > 0 ? "Enabled" : "Disabled");
			html.replace("%serverTime%", SDF.format(new Date(System.currentTimeMillis())));
			html.replace("%serverUpTime%", getServerUpTime());
			html.replace("%onlineAll%", getPlayersCount("ALL"));
			html.replace("%offlineTrade%", getPlayersCount("OFF_TRADE"));
			html.replace("%onlineGM%", getPlayersCount("GM"));
			html.replace("%onlineReal%", getPlayersCount("ALL_REAL"));
			html.replace("%usedMem%", (MEMORY_MX_BEAN.getHeapMemoryUsage().getUsed() / 0x100000) + " Mb");
			html.replace("%freeMem%", (freeMemory / 0x100000) + " Mb");
			html.replace("%totalMem%", (MEMORY_MX_BEAN.getHeapMemoryUsage().getMax() / 0x100000) + " Mb");
			html.replace("%live%", threadCount);
			html.replace("%nondaemon%", nonDaemonCount);
			html.replace("%daemon%", daemonCount);
			html.replace("%peak%", peakCount);
			html.replace("%totalstarted%", totalCount);
			for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans())
			{
				html.replace("%gcol%", gcBean.getName());
				html.replace("%colcount%", gcBean.getCollectionCount());
				html.replace("%coltime%", gcBean.getCollectionTime());
			}
			
			activeChar.sendPacket(html);
		}
		
		return true;
	}
	
	private String getServerUpTime()
	{
		long time = System.currentTimeMillis() - GameServer.getStartTime();
		
		final long days = TimeUnit.MILLISECONDS.toDays(time);
		time -= TimeUnit.DAYS.toMillis(days);
		final long hours = TimeUnit.MILLISECONDS.toHours(time);
		time -= TimeUnit.HOURS.toMillis(hours);
		return days + " Days, " + hours + " Hours, " + TimeUnit.MILLISECONDS.toMinutes(time) + " Minutes";
	}
	
	private int getPlayersCount(String type)
	{
		switch (type)
		{
			case "ALL":
			{
				return World.getInstance().getPlayers().size();
			}
			case "OFF_TRADE":
			{
				int offlineCount = 0;
				
				final Collection<Player> objs = World.getInstance().getPlayers();
				for (Player player : objs)
				{
					if ((player.getClient() == null) || player.getClient().isDetached())
					{
						offlineCount++;
					}
				}
				
				return offlineCount;
			}
			case "GM":
			{
				int onlineGMcount = 0;
				for (Player gm : AdminData.getInstance().getAllGms(true))
				{
					if ((gm != null) && gm.isOnline() && (gm.getClient() != null) && !gm.getClient().isDetached())
					{
						onlineGMcount++;
					}
				}
				
				return onlineGMcount;
			}
			case "ALL_REAL":
			{
				final Set<String> realPlayers = new HashSet<>();
				for (Player onlinePlayer : World.getInstance().getPlayers())
				{
					if ((onlinePlayer != null) && (onlinePlayer.getClient() != null) && !onlinePlayer.getClient().isDetached())
					{
						realPlayers.add(onlinePlayer.getIPAddress());
					}
				}
				
				return realPlayers.size();
			}
		}
		
		return 0;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
