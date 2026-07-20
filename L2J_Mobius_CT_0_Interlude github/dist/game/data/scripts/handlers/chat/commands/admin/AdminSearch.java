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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author CostyKiller
 */
public class AdminSearch implements IAdminCommandHandler
{
	private static final int PAGE_LIMIT = 15;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_search"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_search"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			if (!st.hasMoreTokens())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0);
				html.setFile(activeChar, "data/html/admin/search.htm");
				html.replace("%items%", "");
				html.replace("%pages%", "");
				activeChar.sendPacket(html);
			}
			else
			{
				final String item = st.nextToken();
				int page = 1;
				if (st.hasMoreTokens())
				{
					try
					{
						page = Integer.parseInt(st.nextToken());
					}
					catch (NumberFormatException e)
					{
						page = 1;
					}
				}
				
				results(activeChar, item, page);
			}
		}
		
		return true;
	}
	
	private void results(Player player, String text, int page)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player, "data/html/admin/search.htm");
		
		List<ItemTemplate> items = new ArrayList<>();
		for (ItemTemplate itemName : ItemData.getInstance().getAllItems())
		{
			if ((itemName != null) && itemName.getName().toLowerCase().contains(text.toLowerCase()))
			{
				items.add(itemName);
			}
		}
		
		if (items.isEmpty())
		{
			html.replace("%items%", "<tr><td>No items found with word " + text + ".</td></tr>");
			html.replace("%pages%", "");
			player.sendPacket(html);
			return;
		}
		
		final int max = Math.min(100, HtmlUtil.countPageNumber(items.size(), PAGE_LIMIT));
		items = items.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, items.size()));
		
		final StringBuilder sb = new StringBuilder();
		for (ItemTemplate item : items)
		{
			StringUtil.append(sb, "<tr><td align=center><button value=\\\"Add\\\" action=\\\"bypass admin_create_item " //
				, String.valueOf(item.getId()) //
				, "\" width=32 height=15 back=\"sek.cbui94\" fore=\"sek.cbui94\"></td><td><img src=\\\"" //
				, item.getIcon() //
				, "\" width=32 height=32></td><td>"//
				, String.valueOf(item.getDisplayId()) //
				, "</td><td>" //
				, (item.isStackable() ? "<font name=\"hs8\" color=008000 size=-1>ST</font>" : "<font name=\"hs8\" color=ff0000 size=-1>ST</font>") //
				, "</td><td align=center>"//
				, getItemGrade(item) //
				, "</td><td>" //
				, getFontedWord(text, item.getName()) //
				, "</td></tr>");
		}
		
		html.replace("%items%", sb.toString());
		
		sb.setLength(0);
		for (int i = 0; i < max; i++)
		{
			final int pagenr = i + 1;
			if (page == pagenr)
			{
				StringUtil.append(sb, +pagenr + "&nbsp;");
			}
			else
			{
				StringUtil.append(sb, "<a action=\"bypass -h admin_search " + text + " " + pagenr + "\">" + pagenr + "</a>&nbsp;");
			}
		}
		
		html.replace("%pages%", sb.toString());
		player.sendPacket(html);
	}
	
	private String getItemGrade(ItemTemplate item)
	{
		switch (item.getCrystalType())
		{
			case NONE:
			{
				return "<font name=\"hs8\" color=ae9977 size=-1>NG</font>";
			}
			case D:
			{
				return "<font name=\\\"hs8\\\" color=ae9977 size=-1>D</font>";
			}
			case C:
			{
				return "<font name=\\\"hs8\\\" color=ae9977 size=-1>C</font>";
			}
			case B:
			{
				return "<font name=\\\"hs8\\\" color=ae9977 size=-1>B</font>";
			}
			case A:
			{
				return "<font name=\\\"hs8\\\" color=ae9977 size=-1>A</font>";
			}
			case S:
			{
				return "<font name=\\\"hs8\\\" color=ae9977 size=-1>S</font>";
			}
			default:
			{
				return String.valueOf(item.getItemGrade());
			}
		}
	}
	
	private String getFontedWord(String text, String name)
	{
		final int position = name.toLowerCase().indexOf(text.toLowerCase());
		final StringBuilder str = new StringBuilder(name);
		final String font = "<font color=\"LEVEL\">";
		str.insert(position, font);
		str.insert(position + (font.length() + text.length()), "</font>");
		return str.toString();
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
