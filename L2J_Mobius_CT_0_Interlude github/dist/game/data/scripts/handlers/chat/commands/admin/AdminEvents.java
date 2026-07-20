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

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Event;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminEvents implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_event_menu",
		"admin_event_start",
		"admin_event_stop",
		"admin_event_start_menu",
		"admin_event_stop_menu",
		"admin_event_bypass"
	};
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (activeChar == null)
		{
			return false;
		}
		
		String eventName = "";
		String eventBypass = "";
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		if (st.hasMoreTokens())
		{
			eventName = st.nextToken();
		}
		
		if (st.hasMoreTokens())
		{
			eventBypass = st.nextToken();
		}
		
		if (command.contains("_menu"))
		{
			showMenu(activeChar);
		}
		
		if (command.startsWith("admin_event_start"))
		{
			try
			{
				if (eventName != null)
				{
					final Event event = (Event) ScriptManager.getInstance().getScript(eventName);
					if (event != null)
					{
						if (event.eventStart(activeChar))
						{
							activeChar.sendSysMessage("Event " + eventName + " started.");
							return true;
						}
						
						activeChar.sendSysMessage("There is problem starting " + eventName + " event.");
						return true;
					}
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //event_start <eventname>");
				return false;
			}
		}
		else if (command.startsWith("admin_event_stop"))
		{
			try
			{
				if (eventName != null)
				{
					final Event event = (Event) ScriptManager.getInstance().getScript(eventName);
					if (event != null)
					{
						if (event.eventStop())
						{
							activeChar.sendSysMessage("Event " + eventName + " stopped.");
							return true;
						}
						
						activeChar.sendSysMessage("There is problem with stoping " + eventName + " event.");
						return true;
					}
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //event_start <eventname>");
				return false;
			}
		}
		else if (command.startsWith("admin_event_bypass"))
		{
			try
			{
				if (eventName != null)
				{
					final Event event = (Event) ScriptManager.getInstance().getScript(eventName);
					if (event != null)
					{
						event.eventBypass(activeChar, eventBypass);
					}
				}
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //event_bypass <eventname> <bypass>");
				return false;
			}
		}
		
		return false;
	}
	
	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/gm_events.htm");
		final StringBuilder cList = new StringBuilder(500);
		for (Quest event : ScriptManager.getInstance().getScripts().values())
		{
			if (event instanceof Event)
			{
				cList.append("<tr><td><font color=\"LEVEL\">" + event.getName() + ":</font></td><br><td><button value=\"Start\" action=\"bypass -h admin_event_start_menu " + event.getName() + "\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td><td><button value=\"Stop\" action=\"bypass -h admin_event_stop_menu " + event.getName() + "\" width=65 height=21 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr>");
			}
		}
		
		html.replace("%LIST%", cList.toString());
		activeChar.sendPacket(html);
	}
}
