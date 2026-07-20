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
package handlers.chat.commands.admin;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * @author Mobius
 */
public class AdminOnline implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_online"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equalsIgnoreCase("admin_online"))
		{
			final List<String> ips = new ArrayList<>();
			int total = 0;
			int online = 0;
			int offline = 0;
			int peace = 0;
			int notPeace = 0;
			int instanced = 0;
			int combat = 0;
			for (Player player : World.getInstance().getPlayers())
			{
				final String ip = player.getIPAddress();
				if ((ip != null) && !ips.contains(ip))
				{
					ips.add(ip);
				}
				
				total++;
				
				if (player.isInOfflineMode())
				{
					offline++;
				}
				else if (player.isOnline())
				{
					online++;
				}
				
				if (player.isInsideZone(ZoneId.PEACE))
				{
					peace++;
				}
				else
				{
					notPeace++;
				}
				
				if (player.getInstanceId() > 0)
				{
					instanced++;
				}
				
				if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) || (player.getPvpFlag() > 0) || player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE))
				{
					combat++;
				}
			}
			
			activeChar.sendSysMessage("Online Player Report");
			activeChar.sendSysMessage("Total count: " + total);
			activeChar.sendSysMessage("Total online: " + online);
			activeChar.sendSysMessage("Total offline: " + offline);
			activeChar.sendSysMessage("Max connected: " + World.MAX_CONNECTED_COUNT);
			activeChar.sendSysMessage("Unique IPs: " + ips.size());
			activeChar.sendSysMessage("In peace zone: " + peace);
			activeChar.sendSysMessage("Not in peace zone: " + notPeace);
			activeChar.sendSysMessage("In instances: " + instanced);
			activeChar.sendSysMessage("In combat: " + combat);
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
