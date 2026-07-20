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

import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.loginserverpackets.game.ServerStatus;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles the admin commands that acts on the login
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2007/07/31 10:05:56 $
 */
public class AdminLogin implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_server_gm_only",
		"admin_server_all",
		"admin_server_max_player",
		"admin_server_list_type",
		"admin_server_list_age",
		"admin_server_login"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_server_gm_only"))
		{
			gmOnly();
			activeChar.sendSysMessage("Server is now GM only");
			showMainPage(activeChar);
		}
		else if (command.equals("admin_server_all"))
		{
			allowToAll();
			activeChar.sendSysMessage("Server is not GM only anymore");
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_server_max_player"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String number = st.nextToken();
				try
				{
					LoginServerThread.getInstance().setMaxPlayer(Integer.parseInt(number));
					activeChar.sendSysMessage("maxPlayer set to " + number);
					showMainPage(activeChar);
				}
				catch (NumberFormatException e)
				{
					activeChar.sendSysMessage("Max players must be a number.");
				}
			}
			else
			{
				activeChar.sendSysMessage("Format is server_max_player <max>");
			}
		}
		else if (command.startsWith("admin_server_list_type"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			final int tokens = st.countTokens();
			if (tokens > 1)
			{
				st.nextToken();
				final String[] modes = new String[tokens - 1];
				for (int i = 0; i < (tokens - 1); i++)
				{
					modes[i] = st.nextToken().trim();
				}
				
				int newType = 0;
				try
				{
					newType = Integer.parseInt(modes[0]);
				}
				catch (NumberFormatException e)
				{
					newType = ServerConfig.getServerTypeId(modes);
				}
				
				if (ServerConfig.SERVER_LIST_TYPE != newType)
				{
					ServerConfig.SERVER_LIST_TYPE = newType;
					LoginServerThread.getInstance().sendServerType();
					activeChar.sendSysMessage("Server Type changed to " + getServerTypeName(newType));
					showMainPage(activeChar);
				}
				else
				{
					activeChar.sendSysMessage("Server Type is already " + getServerTypeName(newType));
					showMainPage(activeChar);
				}
			}
			else
			{
				activeChar.sendSysMessage("Format is server_list_type <normal/relax/test/nolabel/restricted/event/free>");
			}
		}
		else if (command.startsWith("admin_server_list_age"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String mode = st.nextToken();
				int age = 0;
				try
				{
					age = Integer.parseInt(mode);
					if (ServerConfig.SERVER_LIST_AGE != age)
					{
						ServerConfig.SERVER_LIST_TYPE = age;
						LoginServerThread.getInstance().sendServerStatus(ServerStatus.SERVER_AGE, age);
						activeChar.sendSysMessage("Server Age changed to " + age);
						showMainPage(activeChar);
					}
					else
					{
						activeChar.sendSysMessage("Server Age is already " + age);
						showMainPage(activeChar);
					}
				}
				catch (NumberFormatException e)
				{
					activeChar.sendSysMessage("Age must be a number");
				}
			}
			else
			{
				activeChar.sendSysMessage("Format is server_list_age <number>");
			}
		}
		else if (command.equals("admin_server_login"))
		{
			showMainPage(activeChar);
		}
		
		return true;
	}
	
	/**
	 * @param activeChar
	 */
	private void showMainPage(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/login.htm");
		html.replace("%server_name%", LoginServerThread.getInstance().getServerName());
		html.replace("%status%", LoginServerThread.getInstance().getStatusString());
		html.replace("%clock%", getServerTypeName(ServerConfig.SERVER_LIST_TYPE));
		html.replace("%brackets%", String.valueOf(ServerConfig.SERVER_LIST_BRACKET));
		html.replace("%max_players%", String.valueOf(LoginServerThread.getInstance().getMaxPlayer()));
		activeChar.sendPacket(html);
	}
	
	private String getServerTypeName(int serverType)
	{
		String nameType = "";
		for (int i = 0; i < 7; i++)
		{
			final int currentType = serverType & (int) Math.pow(2, i);
			if (currentType > 0)
			{
				if (!nameType.isEmpty())
				{
					nameType += "+";
				}
				
				switch (currentType)
				{
					case 0x01:
					{
						nameType += "Normal";
						break;
					}
					case 0x02:
					{
						nameType += "Relax";
						break;
					}
					case 0x04:
					{
						nameType += "Test";
						break;
					}
					case 0x08:
					{
						nameType += "NoLabel";
						break;
					}
					case 0x10:
					{
						nameType += "Restricted";
						break;
					}
					case 0x20:
					{
						nameType += "Event";
						break;
					}
					case 0x40:
					{
						nameType += "Free";
						break;
					}
				}
			}
		}
		
		return nameType;
	}
	
	private void allowToAll()
	{
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
		GeneralConfig.SERVER_GMONLY = false;
	}
	
	private void gmOnly()
	{
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_GM_ONLY);
		GeneralConfig.SERVER_GMONLY = true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
