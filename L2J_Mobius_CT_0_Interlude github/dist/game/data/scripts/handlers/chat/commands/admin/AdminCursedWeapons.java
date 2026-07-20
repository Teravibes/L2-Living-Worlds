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

import java.util.Collection;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.CursedWeapon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles following admin commands: - cw_info = displays cursed weapon status - cw_remove = removes a cursed weapon from the world, item id or name must be provided - cw_add = adds a cursed weapon into the world, item id or name must be provided. Target will be the weilder - cw_goto =
 * teleports GM to the specified cursed weapon - cw_reload = reloads instance manager
 * @version $Revision: 1.1.6.3 $ $Date: 2007/07/31 10:06:06 $
 */
public class AdminCursedWeapons implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_cw_info",
		"admin_cw_remove",
		"admin_cw_goto",
		"admin_cw_reload",
		"admin_cw_add",
		"admin_cw_info_menu"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final CursedWeaponsManager cwm = CursedWeaponsManager.getInstance();
		int id = 0;
		
		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.startsWith("admin_cw_info"))
		{
			if (!command.contains("menu"))
			{
				activeChar.sendSysMessage("====== Cursed Weapons: ======");
				for (CursedWeapon cw : cwm.getCursedWeapons())
				{
					activeChar.sendSysMessage("> " + cw.getName() + " (" + cw.getItemId() + ")");
					if (cw.isActivated())
					{
						final Player pl = cw.getPlayer();
						activeChar.sendSysMessage("  Player holding: " + (pl == null ? "null" : pl.getName()));
						activeChar.sendSysMessage("    Player karma: " + cw.getPlayerKarma());
						activeChar.sendSysMessage("    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
						activeChar.sendSysMessage("    Kills : " + cw.getNbKills());
					}
					else if (cw.isDropped())
					{
						activeChar.sendSysMessage("  Lying on the ground.");
						activeChar.sendSysMessage("    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
						activeChar.sendSysMessage("    Kills : " + cw.getNbKills());
					}
					else
					{
						activeChar.sendSysMessage("  Don't exist in the world.");
					}
					
					activeChar.sendPacket(SystemMessageId.EMPTY_3);
				}
			}
			else
			{
				final Collection<CursedWeapon> cws = cwm.getCursedWeapons();
				final StringBuilder replyMSG = new StringBuilder(cws.size() * 300);
				final NpcHtmlMessage adminReply = new NpcHtmlMessage();
				adminReply.setFile(activeChar, "data/html/admin/cwinfo.htm");
				for (CursedWeapon cw : cwm.getCursedWeapons())
				{
					final int itemId = cw.getItemId();
					replyMSG.append("<table width=270><tr><td>Name:</td><td>");
					replyMSG.append(cw.getName());
					replyMSG.append("</td></tr>");
					
					if (cw.isActivated())
					{
						final Player pl = cw.getPlayer();
						replyMSG.append("<tr><td>Weilder:</td><td>");
						replyMSG.append(pl == null ? "null" : pl.getName());
						replyMSG.append("</td></tr>");
						replyMSG.append("<tr><td>Karma:</td><td>");
						replyMSG.append(cw.getPlayerKarma());
						replyMSG.append("</td></tr>");
						replyMSG.append("<tr><td>Kills:</td><td>");
						replyMSG.append(cw.getPlayerPkKills());
						replyMSG.append("/");
						replyMSG.append(cw.getNbKills());
						replyMSG.append("</td></tr><tr><td>Time remaining:</td><td>");
						replyMSG.append(cw.getTimeLeft() / 60000);
						replyMSG.append(" min.</td></tr>");
						replyMSG.append("<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ");
						replyMSG.append(itemId);
						replyMSG.append("\" width=73 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
						replyMSG.append("<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ");
						replyMSG.append(itemId);
						replyMSG.append("\" width=73 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
					}
					else if (cw.isDropped())
					{
						replyMSG.append("<tr><td>Position:</td><td>Lying on the ground</td></tr><tr><td>Time remaining:</td><td>");
						replyMSG.append(cw.getTimeLeft() / 60000);
						replyMSG.append(" min.</td></tr><tr><td>Kills:</td><td>");
						replyMSG.append(cw.getNbKills());
						replyMSG.append("</td></tr><tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ");
						replyMSG.append(itemId);
						replyMSG.append("\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td>");
						replyMSG.append("<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ");
						replyMSG.append(itemId);
						replyMSG.append("\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td></tr>");
					}
					else
					{
						replyMSG.append("<tr><td>Position:</td><td>Doesn't exist.</td></tr><tr><td><button value=\"Give to Target\" action=\"bypass -h admin_cw_add ");
						replyMSG.append(itemId);
						replyMSG.append("\" width=95 height=21 back=\"bigbutton_over\" fore=\"bigbutton\"></td><td></td></tr>");
					}
					
					replyMSG.append("</table><br>");
				}
				
				adminReply.replace("%cwinfo%", replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
		}
		else if (command.startsWith("admin_cw_reload"))
		{
			cwm.reload();
		}
		else
		{
			CursedWeapon cw = null;
			try
			{
				String parameter = st.nextToken();
				if (parameter.matches("[0-9]*"))
				{
					id = Integer.parseInt(parameter);
				}
				else
				{
					parameter = parameter.replace('_', ' ');
					for (CursedWeapon cwp : cwm.getCursedWeapons())
					{
						if (cwp.getName().toLowerCase().contains(parameter.toLowerCase()))
						{
							id = cwp.getItemId();
							break;
						}
					}
				}
				
				cw = cwm.getCursedWeapon(id);
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Usage: //cw_remove|//cw_goto|//cw_add <itemid|name>");
			}
			
			if (cw == null)
			{
				activeChar.sendSysMessage("Unknown cursed weapon ID.");
				return false;
			}
			
			if (command.startsWith("admin_cw_remove "))
			{
				cw.endOfLife();
			}
			else if (command.startsWith("admin_cw_goto "))
			{
				cw.goTo(activeChar);
			}
			else if (command.startsWith("admin_cw_add"))
			{
				if (cw.isActive())
				{
					activeChar.sendSysMessage("This cursed weapon is already active.");
				}
				else
				{
					final WorldObject target = activeChar.getTarget();
					if ((target != null) && target.isPlayer())
					{
						target.asPlayer().addItem(ItemProcessType.QUEST, id, 1, target, true);
					}
					else
					{
						activeChar.addItem(ItemProcessType.QUEST, id, 1, activeChar, true);
					}
					
					cw.setEndTime(System.currentTimeMillis() + (cw.getDuration() * 60000));
					cw.reActivate();
				}
			}
			else
			{
				activeChar.sendSysMessage("Unknown command.");
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
