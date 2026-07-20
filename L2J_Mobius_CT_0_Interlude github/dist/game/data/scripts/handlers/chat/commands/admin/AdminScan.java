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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.RaidBossSpawnManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.formatters.BypassParserFormatter;
import org.l2jmobius.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.FormatUtil;

/**
 * @author NosBit, Mobius
 */
public class AdminScan implements IAdminCommandHandler
{
	private static final String SPACE = " ";
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_scan",
		"admin_deleteNpcByObjectId"
	};
	
	private static final int DEFAULT_RADIUS = 1000;
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		switch (actualCommand.toLowerCase())
		{
			case "admin_scan":
			{
				processBypass(activeChar, command);
				break;
			}
			case "admin_deletenpcbyobjectid":
			{
				if (!st.hasMoreElements())
				{
					activeChar.sendSysMessage("Usage: //deletenpcbyobjectid objectId=<object_id>");
					return false;
				}
				
				final int objectId = parseInt(command, "objectId", 0);
				if (objectId == 0)
				{
					activeChar.sendSysMessage("objectId is not set or invalid!");
					return false;
				}
				
				final WorldObject target = World.getInstance().findObject(objectId);
				final Npc npc = target instanceof Npc ? target.asNpc() : null;
				if (npc == null)
				{
					activeChar.sendSysMessage("NPC does not exist or object_id does not belong to an NPC");
					return false;
				}
				
				npc.deleteMe();
				
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					spawn.stopRespawn();
					if (RaidBossSpawnManager.getInstance().isDefined(spawn.getId()))
					{
						RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
					}
					else
					{
						SpawnData.getInstance().deleteSpawn(spawn);
					}
				}
				
				activeChar.sendMessage(npc.getName() + " has been deleted.");
				break;
			}
		}
		
		return true;
	}
	
	private void processBypass(Player activeChar, String command)
	{
		final int id = parseInt(command, "id", 0);
		final String name = parseString(command, "name", null);
		final int radius = parseInt(command, "radius", parseInt(command, "range", DEFAULT_RADIUS));
		final int page = parseInt(command, "page", 0);
		
		final Predicate<Npc> condition;
		if (id > 0)
		{
			condition = npc -> npc.getId() == id;
		}
		else if (name != null)
		{
			condition = npc -> npc.getName().toLowerCase().startsWith(name.toLowerCase());
		}
		else
		{
			condition = npc -> true;
		}
		
		sendNpcList(activeChar, radius, page, condition);
	}
	
	private void sendNpcList(Player activeChar, int radius, int page, Predicate<Npc> condition)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/admin/scan.htm");
		
		// @formatter:off
		final PageResult result = PageBuilder.newBuilder(World.getInstance().getVisibleObjectsInRange(activeChar, Npc.class, radius, condition), 15, "bypass -h admin_scan")
			.currentPage(page)
			.pageHandler(NextPrevPageHandler.INSTANCE)
			.formatter(BypassParserFormatter.INSTANCE)
			.style(ButtonsStyle.INSTANCE)
			.bodyHandler((pages, character, sb) ->
		{
			final String npcName = character.getName();
			sb.append("<tr>");
			sb.append("<td width=\"45\">").append(character.getId()).append("</td>");
			sb.append("<td><a action=\"bypass -h admin_move_to ").append(character.getX()).append(SPACE).append(character.getY()).append(SPACE).append(character.getZ()).append("\">").append(npcName.isEmpty() ? "No name NPC" : npcName).append("</a></td>");
			sb.append("<td width=\"60\">").append(FormatUtil.formatAdena(Math.round(activeChar.calculateDistance2D(character)))).append("</td>");
			sb.append("<td width=\"54\"><a action=\"bypass -h admin_deleteNpcByObjectId objectId=").append(character.getObjectId()).append("\"><font color=\"LEVEL\">Delete</font></a></td>");
			sb.append("</tr>");
		}).build();
		// @formatter:on
		
		if (result.getPages() > 1)
		{
			html.replace("%pages%", "<center><table width=\"100%\" cellspacing=0><tr>" + result.getPagerTemplate() + "</tr></table></center>");
		}
		else
		{
			html.replace("%pages%", "");
		}
		
		html.replace("%data%", result.getBodyTemplate().toString());
		activeChar.sendPacket(html);
	}
	
	private int parseInt(String command, String paramName, int defaultValue)
	{
		final Pattern pattern = Pattern.compile(paramName + "=([^\\s]+)");
		final Matcher matcher = pattern.matcher(command);
		if (matcher.find())
		{
			try
			{
				return Integer.parseInt(matcher.group(1).trim());
			}
			catch (NumberFormatException e)
			{
				// Ignore and return default.
			}
		}
		
		return defaultValue;
	}
	
	private String parseString(String command, String paramName, String defaultValue)
	{
		final Pattern pattern = Pattern.compile(paramName + "=([^\\s]+)");
		final Matcher matcher = pattern.matcher(command);
		if (matcher.find())
		{
			return matcher.group(1).trim();
		}
		
		return defaultValue;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
