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

import java.util.Collection;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.FormatUtil;

/**
 * Admin PC Points manage admin commands.
 */
public class AdminPcCafePoints implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pccafepoints",
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		if (actualCommand.equals("admin_pccafepoints"))
		{
			if (st.hasMoreTokens())
			{
				final String action = st.nextToken();
				final Player target = getTarget(activeChar);
				if ((target == null) || !st.hasMoreTokens())
				{
					return false;
				}
				
				int value = 0;
				try
				{
					value = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					showMenuHtml(activeChar);
					activeChar.sendSysMessage("Invalid Value!");
					return false;
				}
				
				switch (action)
				{
					case "set":
					{
						if (value > PremiumSystemConfig.PC_CAFE_MAX_POINTS)
						{
							showMenuHtml(activeChar);
							activeChar.sendSysMessage("You cannot set more than " + PremiumSystemConfig.PC_CAFE_MAX_POINTS + " PC points!");
							return false;
						}
						
						if (value < 0)
						{
							value = 0;
						}
						
						target.setPcCafePoints(value);
						target.sendMessage("Admin set your PC Cafe point(s) to " + value + "!");
						activeChar.sendSysMessage("You set " + value + " PC Cafe point(s) to player " + target.getName());
						target.sendPacket(new ExPCCafePointInfo(value, value, 0));
						break;
					}
					case "increase":
					{
						if (target.getPcCafePoints() == PremiumSystemConfig.PC_CAFE_MAX_POINTS)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have max count of PC points!");
							return false;
						}
						
						int pcCafeCount = Math.min(target.getPcCafePoints() + value, PremiumSystemConfig.PC_CAFE_MAX_POINTS);
						if (pcCafeCount < 0)
						{
							pcCafeCount = PremiumSystemConfig.PC_CAFE_MAX_POINTS;
						}
						
						target.setPcCafePoints(pcCafeCount);
						target.sendMessage("Admin increased your PC Cafe point(s) by " + value + "!");
						activeChar.sendSysMessage("You increased PC Cafe point(s) of " + target.getName() + " by " + value);
						target.sendPacket(new ExPCCafePointInfo(pcCafeCount, value, 0));
						break;
					}
					case "decrease":
					{
						if (target.getPcCafePoints() == 0)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have min count of PC points!");
							return false;
						}
						
						final int pcCafeCount = Math.max(target.getPcCafePoints() - value, 0);
						target.setPcCafePoints(pcCafeCount);
						target.sendMessage("Admin decreased your PC Cafe point(s) by " + value + "!");
						activeChar.sendSysMessage("You decreased PC Cafe point(s) of " + target.getName() + " by " + value);
						target.sendPacket(new ExPCCafePointInfo(target.getPcCafePoints(), -value, 0));
						break;
					}
					case "rewardOnline":
					{
						int range = 0;
						try
						{
							range = Integer.parseInt(st.nextToken());
						}
						catch (Exception e)
						{
						}
						
						if (range <= 0)
						{
							final int count = increaseForAll(World.getInstance().getPlayers(), value);
							activeChar.sendSysMessage("You increased PC Cafe point(s) of all online players (" + count + ") by " + value + ".");
						}
						else if (range > 0)
						{
							final int count = increaseForAll(World.getInstance().getVisibleObjectsInRange(activeChar, Player.class, range), value);
							activeChar.sendSysMessage("You increased PC Cafe point(s) of all players (" + count + ") in range " + range + " by " + value + ".");
						}
						break;
					}
				}
			}
			
			showMenuHtml(activeChar);
		}
		
		return true;
	}
	
	private int increaseForAll(Collection<Player> playerList, int value)
	{
		int counter = 0;
		for (Player temp : playerList)
		{
			if ((temp != null) && (temp.isOnlineInt() == 1))
			{
				if (temp.getPcCafePoints() == Integer.MAX_VALUE)
				{
					continue;
				}
				
				int pcCafeCount = Math.min(temp.getPcCafePoints() + value, Integer.MAX_VALUE);
				if (pcCafeCount < 0)
				{
					pcCafeCount = Integer.MAX_VALUE;
				}
				
				temp.setPcCafePoints(pcCafeCount);
				temp.sendMessage("Admin increased your PC Cafe point(s) by " + value + "!");
				temp.sendPacket(new ExPCCafePointInfo(pcCafeCount, value, 0));
				counter++;
			}
		}
		
		return counter;
	}
	
	private Player getTarget(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		final Player targetPlayer = target != null ? target.asPlayer() : null;
		return targetPlayer != null ? targetPlayer : activeChar;
	}
	
	private void showMenuHtml(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		final Player target = getTarget(activeChar);
		final int points = target.getPcCafePoints();
		html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/pccafe.htm"));
		html.replace("%points%", FormatUtil.formatAdena(points));
		html.replace("%targetName%", target.getName());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
