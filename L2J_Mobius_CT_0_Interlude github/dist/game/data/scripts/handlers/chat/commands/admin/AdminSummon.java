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

import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author poltomb
 */
public class AdminSummon implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_summon"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		int id;
		int count = 1;
		final String[] data = command.split(" ");
		if (data.length < 2)
		{
			activeChar.sendSysMessage("Usage: //summon <id> [count]");
			return false;
		}
		
		try
		{
			id = Integer.parseInt(data[1]);
			if (data.length > 2)
			{
				count = Integer.parseInt(data[2]);
			}
		}
		catch (NumberFormatException nfe)
		{
			activeChar.sendSysMessage("Incorrect format for command 'summon'");
			return false;
		}
		
		final String subCommand;
		if (id < 1000000)
		{
			subCommand = "admin_create_item";
		}
		else
		{
			subCommand = "admin_spawn_once";
			activeChar.sendSysMessage("This is only a temporary spawn.  The mob(s) will NOT respawn.");
			id -= 1000000;
		}
		
		if ((id > 0) && (count > 0))
		{
			AdminCommandHandler.getInstance().onCommand(activeChar, subCommand + " " + id + " " + count, true);
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
