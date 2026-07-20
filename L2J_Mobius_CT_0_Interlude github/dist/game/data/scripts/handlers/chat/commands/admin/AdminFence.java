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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.FenceState;
import org.l2jmobius.gameserver.model.actor.instance.Fence;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Sahar, Nik64
 */
public class AdminFence implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_addfence",
		"admin_setfencestate",
		"admin_removefence",
		"admin_listfence",
		"admin_gofence"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_addfence":
			{
				try
				{
					final int width = Integer.parseInt(st.nextToken());
					final int length = Integer.parseInt(st.nextToken());
					final int height = Integer.parseInt(st.nextToken());
					if ((width < 1) || (length < 1))
					{
						activeChar.sendSysMessage("Width and length values must be positive numbers.");
						return false;
					}
					
					if ((height < 1) || (height > 3))
					{
						activeChar.sendSysMessage("The range for height can only be 1-3.");
						return false;
					}
					
					FenceData.getInstance().spawnFence(activeChar.getX(), activeChar.getY(), activeChar.getZ(), width, length, height, activeChar.getInstanceId(), FenceState.CLOSED);
					activeChar.sendSysMessage("Fence added succesfully.");
				}
				catch (NoSuchElementException | NumberFormatException e)
				{
					activeChar.sendSysMessage("Format must be: //addfence <width> <length> <height>");
				}
				break;
			}
			case "admin_setfencestate":
			{
				try
				{
					final int objId = Integer.parseInt(st.nextToken());
					final int fenceTypeOrdinal = Integer.parseInt(st.nextToken());
					if ((fenceTypeOrdinal < 0) || (fenceTypeOrdinal >= FenceState.values().length))
					{
						activeChar.sendSysMessage("Specified FenceType is out of range. Only 0-" + (FenceState.values().length - 1) + " are permitted.");
					}
					else
					{
						final WorldObject obj = World.getInstance().findObject(objId);
						if (obj instanceof Fence)
						{
							final Fence fence = (Fence) obj;
							final FenceState state = FenceState.values()[fenceTypeOrdinal];
							fence.setState(state);
							activeChar.sendSysMessage("Fence " + fence.getName() + "[" + fence.getId() + "]'s state has been changed to " + state.toString());
						}
						else
						{
							activeChar.sendSysMessage("Target is not a fence.");
						}
					}
				}
				catch (NoSuchElementException | NumberFormatException e)
				{
					activeChar.sendSysMessage("Format mustr be: //setfencestate <fenceObjectId> <fenceState>");
				}
				break;
			}
			case "admin_removefence":
			{
				try
				{
					final int objId = Integer.parseInt(st.nextToken());
					final WorldObject obj = World.getInstance().findObject(objId);
					if (obj instanceof Fence)
					{
						((Fence) obj).deleteMe();
						activeChar.sendSysMessage("Fence removed succesfully.");
					}
					else
					{
						activeChar.sendSysMessage("Target is not a fence.");
					}
				}
				catch (Exception e)
				{
					activeChar.sendSysMessage("Invalid object ID or target was not found.");
				}
				
				sendHtml(activeChar, 0);
				break;
			}
			case "admin_listfence":
			{
				int page = 0;
				if (st.hasMoreTokens())
				{
					page = Integer.parseInt(st.nextToken());
				}
				
				sendHtml(activeChar, page);
				break;
			}
			case "admin_gofence":
			{
				try
				{
					final int objId = Integer.parseInt(st.nextToken());
					final WorldObject obj = World.getInstance().findObject(objId);
					if (obj != null)
					{
						activeChar.teleToLocation(obj);
					}
				}
				catch (Exception e)
				{
					activeChar.sendSysMessage("Invalid object ID or target was not found.");
				}
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void sendHtml(Player activeChar, int page)
	{
		final PageResult result = PageBuilder.newBuilder(FenceData.getInstance().getFences().values(), 10, "bypass -h admin_listfence").currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, fence, sb) ->
		{
			sb.append("<tr><td>");
			sb.append(fence.getName() == null ? fence.getId() : fence.getName());
			sb.append("</td><td>");
			sb.append("<button value=\"Go\" action=\"bypass -h admin_gofence ");
			sb.append(fence.getId());
			sb.append("\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			sb.append("</td><td>");
			sb.append("<button value=\"Hide\" action=\"bypass -h admin_setfencestate ");
			sb.append(fence.getId());
			sb.append(" 0");
			sb.append("\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			sb.append("</td><td>");
			sb.append("<button value=\"Off\" action=\"bypass -h admin_setfencestate ");
			sb.append(fence.getId());
			sb.append(" 1");
			sb.append("\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			sb.append("</td><td>");
			sb.append("<button value=\"On\" action=\"bypass -h admin_setfencestate ");
			sb.append(fence.getId());
			sb.append(" 2");
			sb.append("\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			sb.append("</td><td>");
			sb.append("<button value=\"X\" action=\"bypass -h admin_removefence ");
			sb.append(fence.getId());
			sb.append("\" width=30 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			sb.append("</td></tr>");
		}).build();
		
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/fences.htm");
		if (result.getPages() > 1)
		{
			html.replace("%pages%", "<table width=280 cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		
		html.replace("%fences%", result.getBodyTemplate().toString());
		activeChar.sendPacket(html);
	}
}
