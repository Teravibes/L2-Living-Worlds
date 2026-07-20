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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.GMViewPledgeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * <b>Pledge Manipulation:</b><br>
 * <li>With target in a character without clan:<br>
 * //pledge create clanname
 * <li>With target in a clan leader:<br>
 * //pledge info<br>
 * //pledge dismiss<br>
 * //pledge setlevel level<br>
 * //pledge rep reputation_points
 */
public class AdminPledge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pledge"
	};
	
	private static final int MAX_CLAN_LEVEL = 8;
	private static final int REP_POINTS_REWARD_LEVEL = 5;
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.nextToken();
		if (cmd == null)
		{
			return false;
		}
		
		switch (cmd)
		{
			case "admin_pledge":
			{
				final WorldObject target = activeChar.getTarget();
				Player player = null;
				if (target instanceof Player)
				{
					player = target.asPlayer();
				}
				else
				{
					player = activeChar;
				}
				
				final String name = player.getName();
				String action = null;
				String parameter = null;
				if (st.hasMoreTokens())
				{
					action = st.nextToken(); // create|info|dismiss|setlevel|rep
				}
				
				if (action == null)
				{
					activeChar.sendSysMessage("Not allowed Action on Clan");
					showMainPage(activeChar);
					return false;
				}
				
				if (!action.equals("create") && !player.isClanLeader())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(name));
					showMainPage(activeChar);
					return false;
				}
				
				if (st.hasMoreTokens())
				{
					parameter = st.nextToken(); // clanname|nothing|nothing|level|rep_points
				}
				
				switch (action)
				{
					case "create":
					{
						if ((parameter == null) || (parameter.length() == 0))
						{
							activeChar.sendSysMessage("Please, enter clan name.");
							showMainPage(activeChar);
							return false;
						}
						
						final long cet = player.getClanCreateExpiryTime();
						player.setClanCreateExpiryTime(0);
						final Clan clan = ClanTable.getInstance().createClan(player, parameter);
						if (clan != null)
						{
							activeChar.sendSysMessage("Clan " + parameter + " created. Leader: " + player.getName());
							return true;
						}
						
						player.setClanCreateExpiryTime(cet);
						activeChar.sendSysMessage("There was a problem while creating the clan.");
						showMainPage(activeChar);
						return false;
					}
					case "dismiss":
					{
						ClanTable.getInstance().destroyClan(player.getClanId());
						final Clan clan = player.getClan();
						if (clan == null)
						{
							activeChar.sendSysMessage("Clan disbanded.");
							return true;
						}
						
						activeChar.sendSysMessage("There was a problem while destroying the clan.");
						showMainPage(activeChar);
						return false;
					}
					case "info":
					{
						final Clan clan;
						if (parameter != null)
						{
							clan = ClanTable.getInstance().getClanByName(parameter);
						}
						else
						{
							clan = player.getClan();
						}
						
						activeChar.sendPacket(new GMViewPledgeInfo(clan, player));
						return true;
					}
					case "setlevel":
					{
						if (parameter == null)
						{
							activeChar.sendSysMessage("Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						final Clan clan = player.getClan();
						int level = clan.getLevel();
						try
						{
							level = Integer.parseInt(parameter);
						}
						catch (NumberFormatException nfe)
						{
							activeChar.sendSysMessage("Level incorrect.");
							activeChar.sendSysMessage("Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						if ((level >= 0) && (level <= MAX_CLAN_LEVEL))
						{
							clan.changeLevel(level);
							activeChar.sendSysMessage("You set level " + level + " for clan " + clan.getName());
							return true;
						}
						
						activeChar.sendSysMessage("Level incorrect.");
						activeChar.sendSysMessage("Usage: //pledge <setlevel|rep> <number>");
						showMainPage(activeChar);
						return false;
					}
					case "rep":
					{
						if (parameter == null)
						{
							activeChar.sendSysMessage("Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						final Clan clan = player.getClan();
						int points = clan.getReputationScore();
						try
						{
							points = Integer.parseInt(parameter);
						}
						catch (NumberFormatException nfe)
						{
							activeChar.sendSysMessage("Points incorrect.");
							activeChar.sendSysMessage("Usage: //pledge <setlevel|rep> <number>");
							showMainPage(activeChar);
							return false;
						}
						
						if (clan.getLevel() < REP_POINTS_REWARD_LEVEL)
						{
							activeChar.sendSysMessage("Only clans of level 5 or above may receive reputation points.");
							showMainPage(activeChar);
							return false;
						}
						
						try
						{
							clan.addReputationScore(points);
							activeChar.sendSysMessage("You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Their current score is " + clan.getReputationScore());
							showMainPage(activeChar);
							return false;
						}
						catch (Exception e)
						{
							activeChar.sendSysMessage("Usage: //pledge <rep> <number>");
						}
						break;
					}
					default:
					{
						activeChar.sendSysMessage("Clan Action not allowed...");
						showMainPage(activeChar);
						return false;
					}
				}
			}
			default:
			{
				activeChar.sendSysMessage("Clan command not allowed");
				showMainPage(activeChar);
			}
		}
		
		return false;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void showMainPage(Player activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "game_menu.htm");
	}
}
