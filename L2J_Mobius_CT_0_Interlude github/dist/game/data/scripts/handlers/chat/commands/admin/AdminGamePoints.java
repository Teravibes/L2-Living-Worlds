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

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Admin game point commands.
 * @author Mobius
 */
public class AdminGamePoints implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_add_game_points",
		"admin_count_game_points",
		"admin_gamepoints",
		"admin_set_game_points",
		"admin_subtract_game_points"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_add_game_points"))
		{
			try
			{
				final String val = command.substring(22);
				if (!addGamePoints(activeChar, val))
				{
					activeChar.sendSysMessage("Usage: //add_game_points count");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{ // Case of missing parameter
				activeChar.sendSysMessage("Usage: //add_game_points count");
			}
		}
		else if (command.equals("admin_count_game_points"))
		{
			if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				final Player target = activeChar.getTarget().asPlayer();
				activeChar.sendMessage(target.getName() + " has a total of " + target.getGamePoints() + " game points.");
			}
			else
			{
				activeChar.sendSysMessage("You must select a player first.");
			}
		}
		else if (command.equals("admin_gamepoints"))
		{
			openGamePointsMenu(activeChar);
		}
		else if (command.startsWith("admin_set_game_points"))
		{
			try
			{
				final String val = command.substring(22);
				if (!setGamePoints(activeChar, val))
				{
					activeChar.sendSysMessage("Usage: //set_game_points count");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of missing parameter.
				activeChar.sendSysMessage("Usage: //set_game_points count");
			}
		}
		else if (command.startsWith("admin_subtract_game_points"))
		{
			try
			{
				final String val = command.substring(27);
				if (!subtractGamePoints(activeChar, val))
				{
					activeChar.sendSysMessage("Usage: //subtract_game_points count");
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Case of missing parameter.
				activeChar.sendSysMessage("Usage: //subtract_game_points count");
			}
		}
		
		return true;
	}
	
	private void openGamePointsMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/game_points.htm");
		activeChar.sendPacket(html);
	}
	
	private boolean addGamePoints(Player admin, String value)
	{
		final WorldObject target = admin.getTarget();
		Player player = null;
		if (target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			admin.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}
		
		final Long points = Long.parseLong(value);
		if (points < 1)
		{
			admin.sendMessage("Invalid game point count.");
			return false;
		}
		
		final long currentPoints = player.getGamePoints();
		if (currentPoints < 1)
		{
			player.setGamePoints(points);
		}
		else
		{
			player.setGamePoints(currentPoints + points);
		}
		
		admin.sendMessage("Added " + points + " game points to " + player.getName() + ".");
		admin.sendMessage(player.getName() + " has now a total of " + player.getGamePoints() + " game points.");
		return true;
	}
	
	private boolean setGamePoints(Player admin, String value)
	{
		final WorldObject target = admin.getTarget();
		Player player = null;
		if (target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			admin.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}
		
		final Long points = Long.parseLong(value);
		if (points < 0)
		{
			admin.sendMessage("Invalid game point count.");
			return false;
		}
		
		player.setGamePoints(points);
		admin.sendMessage(player.getName() + " has now a total of " + points + " game points.");
		return true;
	}
	
	private boolean subtractGamePoints(Player admin, String value)
	{
		final WorldObject target = admin.getTarget();
		Player player = null;
		if (target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			admin.sendPacket(SystemMessageId.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}
		
		final Long points = Long.parseLong(value);
		if (points < 1)
		{
			admin.sendMessage("Invalid game point count.");
			return false;
		}
		
		final long currentPoints = player.getGamePoints();
		if (currentPoints <= points)
		{
			player.setGamePoints(0);
		}
		else
		{
			player.setGamePoints(currentPoints - points);
		}
		
		admin.sendMessage(player.getName() + " has now a total of " + player.getGamePoints() + " game points.");
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
