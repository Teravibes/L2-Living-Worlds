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

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.ZoneBuildManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Mobius
 */
public class AdminZoneBuild implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_zone_build",
		"admin_zone_build_save",
		"admin_zone_build_delete",
		"admin_zone_build_toggle",
		"admin_zone_build_clear"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_zone_build"))
		{
			sendHtml(activeChar);
			return true;
		}
		else if (command.startsWith("admin_zone_build_toggle"))
		{
			if (activeChar.getTeleMode() == 3)
			{
				activeChar.setTeleMode(0);
				activeChar.sendMessage("Draw zone disabled. You can move.");
			}
			else
			{
				activeChar.setTeleMode(3);
				activeChar.sendMessage("Draw zone active. Click on point to record.");
			}
			
			sendHtml(activeChar);
			return true;
		}
		else if (command.startsWith("admin_zone_build_clear"))
		{
			ZoneBuildManager.getInstance().clearZone(activeChar);
			sendHtml(activeChar);
			return true;
		}
		else if (command.startsWith("admin_zone_build_delete"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (st.hasMoreTokens())
			{
				final String nextT = st.nextToken();
				if (StringUtil.isNumeric(nextT))
				{
					final int entry = Integer.parseInt(nextT);
					ZoneBuildManager.getInstance().deleteEntry(activeChar, entry);
					sendHtml(activeChar);
					return true;
				}
			}
		}
		else if (command.equals("admin_zone_build_save"))
		{
			activeChar.addAction(PlayerAction.ADMIN_SAVE_ZONE);
			activeChar.sendPacket(new ConfirmDlg("Do you wish to save the zone?"));
			return true;
		}
		
		return false;
	}
	
	public void sendHtml(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/zone_builder.htm");
		html.replace("%paths%", ZoneBuildManager.getInstance().getPathsForHtml(activeChar));
		html.replace("%movement%", activeChar.getTeleMode() == 3 ? "Disable Draw" : "Enable Draw");
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
