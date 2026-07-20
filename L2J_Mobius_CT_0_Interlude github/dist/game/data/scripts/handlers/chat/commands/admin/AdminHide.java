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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author lord_rex
 */
public class AdminHide implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_hide"
	};
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		try
		{
			final String param = st.nextToken();
			switch (param)
			{
				case "on":
				{
					if (!player.setHiding(true))
					{
						player.sendSysMessage("Currently, you cannot be seen.");
						return true;
					}
					
					player.sendSysMessage("Now, you cannot be seen.");
					return true;
				}
				case "off":
				{
					if (!player.setHiding(false))
					{
						player.sendSysMessage("Currently, you can be seen.");
						return true;
					}
					
					player.sendSysMessage("Now, you can be seen.");
					return true;
				}
				default:
				{
					player.sendSysMessage("//hide [on|off]");
					return false;
				}
			}
		}
		catch (Exception e)
		{
			player.sendSysMessage("//hide [on|off]");
			return false;
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
