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

import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.formatters.BypassParserFormatter;
import org.l2jmobius.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Mobius
 */
public class AdminHelp implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_help",
		"admin_commands",
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_help"))
		{
			AdminHtml.showAdminHtml(activeChar, "help.htm");
		}
		else if (command.startsWith("admin_commands"))
		{
			final int page = Integer.parseInt(command.contains("page") ? command.substring(15).replace("page=", "") : "1");
			final PageResult result = PageBuilder.newBuilder(AdminData.getInstance().getAdminCommandAccessRights(), 5, "bypass -h admin_commands").currentPage(page).pageHandler(NextPrevPageHandler.INSTANCE).formatter(BypassParserFormatter.INSTANCE).style(ButtonsStyle.INSTANCE).bodyHandler((pages, access, sb) ->
			{
				sb.append("<tr><td height=40 width=400><font color=\"LEVEL\">//");
				sb.append(access.getCommand().substring(6));
				sb.append("</font></td><td height=40 width=400>");
				sb.append(access.getDescription());
				sb.append("</td></tr>");
			}).build();
			
			final NpcHtmlMessage html = new NpcHtmlMessage();
			html.setFile(activeChar, "data/html/admin/help/commands.htm");
			if (result.getPages() > 1)
			{
				html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
			}
			else
			{
				html.replace("%pages%", "");
			}
			
			html.replace("%list%", result.getBodyTemplate().toString());
			activeChar.sendPacket(html);
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
