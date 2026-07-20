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

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.sql.AnnouncementsTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.announce.Announcement;
import org.l2jmobius.gameserver.model.announce.AnnouncementType;
import org.l2jmobius.gameserver.model.announce.AutoAnnouncement;
import org.l2jmobius.gameserver.model.announce.IAnnouncement;
import org.l2jmobius.gameserver.model.html.PageBuilder;
import org.l2jmobius.gameserver.model.html.PageResult;
import org.l2jmobius.gameserver.model.html.formatters.BypassParserFormatter;
import org.l2jmobius.gameserver.model.html.pagehandlers.NextPrevPageHandler;
import org.l2jmobius.gameserver.model.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * @author UnAfraid
 */
public class AdminAnnouncements implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_announce",
		"admin_announce_crit",
		"admin_announce_screen",
		"admin_announces",
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		final String cmd = st.hasMoreTokens() ? st.nextToken() : "";
		switch (cmd)
		{
			case "admin_announce":
			case "admin_announce_crit":
			case "admin_announce_screen":
			{
				if (!st.hasMoreTokens())
				{
					activeChar.sendSysMessage("Syntax: //announce <text to announce here>");
					return false;
				}
				
				String announce = st.nextToken();
				while (st.hasMoreTokens())
				{
					announce += " " + st.nextToken();
				}
				
				if (cmd.equals("admin_announce_screen"))
				{
					Broadcast.toAllOnlinePlayersOnScreen(announce);
				}
				else
				{
					if (GeneralConfig.GM_ANNOUNCER_NAME)
					{
						announce = announce + " [" + activeChar.getName() + "]";
					}
					
					Broadcast.toAllOnlinePlayers(announce, cmd.equals("admin_announce_crit"));
				}
				
				AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
				break;
			}
			case "admin_announces":
			{
				final String subCmd = st.hasMoreTokens() ? st.nextToken() : "";
				switch (subCmd)
				{
					case "add":
					{
						if (!st.hasMoreTokens())
						{
							final String content = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/announces/announces-add.htm");
							HtmlUtil.sendCBHtml(activeChar, content);
							break;
						}
						
						final String annType = st.nextToken();
						final AnnouncementType type = AnnouncementType.findByName(annType);
						
						// ************************************
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final String annInitDelay = st.nextToken();
						if (!StringUtil.isNumeric(annInitDelay))
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final int initDelay = Integer.parseInt(annInitDelay) * 1000;
						
						// ************************************
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final String annDelay = st.nextToken();
						if (!StringUtil.isNumeric(annDelay))
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final int delay = Integer.parseInt(annDelay) * 1000;
						if ((delay < (10 * 1000)) && ((type == AnnouncementType.AUTO_NORMAL) || (type == AnnouncementType.AUTO_CRITICAL)))
						{
							activeChar.sendSysMessage("Delay cannot be less then 10 seconds!");
							break;
						}
						
						// ************************************
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final String annRepeat = st.nextToken();
						if (!StringUtil.isNumeric(annRepeat))
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						int repeat = Integer.parseInt(annRepeat);
						if (repeat == 0)
						{
							repeat = -1;
						}
						
						// ************************************
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						String content = st.nextToken();
						while (st.hasMoreTokens())
						{
							content += " " + st.nextToken();
						}
						
						// ************************************
						final IAnnouncement announce;
						if ((type == AnnouncementType.AUTO_CRITICAL) || (type == AnnouncementType.AUTO_NORMAL))
						{
							announce = new AutoAnnouncement(type, content, activeChar.getName(), initDelay, delay, repeat);
						}
						else
						{
							announce = new Announcement(type, content, activeChar.getName());
						}
						
						AnnouncementsTable.getInstance().addAnnouncement(announce);
						activeChar.sendSysMessage("Announcement has been successfully added!");
						return onCommand("admin_announces list", activeChar);
					}
					case "edit":
					{
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces edit <id>");
							break;
						}
						
						final String annId = st.nextToken();
						if (!StringUtil.isNumeric(annId))
						{
							activeChar.sendSysMessage("Syntax: //announces edit <id>");
							break;
						}
						
						final int id = Integer.parseInt(annId);
						final IAnnouncement announce = AnnouncementsTable.getInstance().getAnnounce(id);
						if (announce == null)
						{
							activeChar.sendSysMessage("Announcement does not exist!");
							break;
						}
						
						if (!st.hasMoreTokens())
						{
							String content = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/announces/announces-edit.htm");
							final String announcementId = Integer.toString(announce.getId());
							final String announcementType = announce.getType().name();
							String announcementInital = "0";
							String announcementDelay = "0";
							String announcementRepeat = "0";
							final String announcementAuthor = announce.getAuthor();
							final String announcementContent = announce.getContent();
							if (announce instanceof AutoAnnouncement)
							{
								final AutoAnnouncement autoAnnounce = (AutoAnnouncement) announce;
								announcementInital = Long.toString(autoAnnounce.getInitial() / 1000);
								announcementDelay = Long.toString(autoAnnounce.getDelay() / 1000);
								announcementRepeat = Integer.toString(autoAnnounce.getRepeat());
							}
							
							content = content.replace("%id%", announcementId);
							content = content.replace("%type%", announcementType);
							content = content.replace("%initial%", announcementInital);
							content = content.replace("%delay%", announcementDelay);
							content = content.replace("%repeat%", announcementRepeat);
							content = content.replace("%author%", announcementAuthor);
							content = content.replace("%content%", announcementContent);
							HtmlUtil.sendCBHtml(activeChar, content);
							break;
						}
						
						final String annType = st.nextToken();
						final AnnouncementType type = AnnouncementType.findByName(annType);
						switch (announce.getType())
						{
							case AUTO_CRITICAL:
							case AUTO_NORMAL:
							{
								switch (type)
								{
									case AUTO_CRITICAL:
									case AUTO_NORMAL:
									{
										break;
									}
									default:
									{
										activeChar.sendSysMessage("Announce type can be changed only to AUTO_NORMAL or AUTO_CRITICAL!");
										return false;
									}
								}
								break;
							}
							case NORMAL:
							case CRITICAL:
							{
								switch (type)
								{
									case NORMAL:
									case CRITICAL:
									{
										break;
									}
									default:
									{
										activeChar.sendSysMessage("Announce type can be changed only to NORMAL or CRITICAL!");
										return false;
									}
								}
								break;
							}
						}
						
						// ************************************
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final String annInitDelay = st.nextToken();
						if (!StringUtil.isNumeric(annInitDelay))
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final int initDelay = Integer.parseInt(annInitDelay);
						
						// ************************************
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final String annDelay = st.nextToken();
						if (!StringUtil.isNumeric(annDelay))
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final int delay = Integer.parseInt(annDelay);
						if ((delay < 10) && ((type == AnnouncementType.AUTO_NORMAL) || (type == AnnouncementType.AUTO_CRITICAL)))
						{
							activeChar.sendSysMessage("Delay cannot be less then 10 seconds!");
							break;
						}
						
						// ************************************
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						final String annRepeat = st.nextToken();
						if (!StringUtil.isNumeric(annRepeat))
						{
							activeChar.sendSysMessage("Syntax: //announces add <type> <delay> <repeat> <text>");
							break;
						}
						
						int repeat = Integer.parseInt(annRepeat);
						if (repeat == 0)
						{
							repeat = -1;
						}
						
						// ************************************
						String content = "";
						if (st.hasMoreTokens())
						{
							content = st.nextToken();
							while (st.hasMoreTokens())
							{
								content += " " + st.nextToken();
							}
						}
						
						if (content.isEmpty())
						{
							content = announce.getContent();
						}
						
						// ************************************
						announce.setType(type);
						announce.setContent(content);
						announce.setAuthor(activeChar.getName());
						if (announce instanceof AutoAnnouncement)
						{
							final AutoAnnouncement autoAnnounce = (AutoAnnouncement) announce;
							autoAnnounce.setInitial(initDelay * 1000);
							autoAnnounce.setDelay(delay * 1000);
							autoAnnounce.setRepeat(repeat);
						}
						
						announce.updateMe();
						activeChar.sendSysMessage("Announcement has been successfully edited!");
						return onCommand("admin_announces list", activeChar);
					}
					case "remove":
					{
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces remove <announcement id>");
							break;
						}
						
						final String token = st.nextToken();
						if (!StringUtil.isNumeric(token))
						{
							activeChar.sendSysMessage("Syntax: //announces remove <announcement id>");
							break;
						}
						
						final int id = Integer.parseInt(token);
						if (AnnouncementsTable.getInstance().deleteAnnouncement(id))
						{
							activeChar.sendSysMessage("Announcement has been successfully removed!");
						}
						else
						{
							activeChar.sendSysMessage("Announcement does not exist!");
						}
						
						return onCommand("admin_announces list", activeChar);
					}
					case "restart":
					{
						if (!st.hasMoreTokens())
						{
							for (IAnnouncement announce : AnnouncementsTable.getInstance().getAllAnnouncements())
							{
								if (announce instanceof AutoAnnouncement)
								{
									final AutoAnnouncement autoAnnounce = (AutoAnnouncement) announce;
									autoAnnounce.restartMe();
								}
							}
							
							activeChar.sendSysMessage("Auto announcements has been successfully restarted!");
							break;
						}
						
						final String token = st.nextToken();
						if (!StringUtil.isNumeric(token))
						{
							activeChar.sendSysMessage("Syntax: //announces show <announcement id>");
							break;
						}
						
						final int id = Integer.parseInt(token);
						final IAnnouncement announce = AnnouncementsTable.getInstance().getAnnounce(id);
						if (announce != null)
						{
							if (announce instanceof AutoAnnouncement)
							{
								final AutoAnnouncement autoAnnounce = (AutoAnnouncement) announce;
								autoAnnounce.restartMe();
								activeChar.sendSysMessage("Auto announcement has been successfully restarted");
							}
							else
							{
								activeChar.sendSysMessage("This option has effect only on auto announcements!");
							}
						}
						else
						{
							activeChar.sendSysMessage("Announcement does not exist!");
						}
						break;
					}
					case "show":
					{
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Syntax: //announces show <announcement id>");
							break;
						}
						
						final String token = st.nextToken();
						if (!StringUtil.isNumeric(token))
						{
							activeChar.sendSysMessage("Syntax: //announces show <announcement id>");
							break;
						}
						
						final int id = Integer.parseInt(token);
						final IAnnouncement announce = AnnouncementsTable.getInstance().getAnnounce(id);
						if (announce != null)
						{
							String content = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/announces/announces-show.htm");
							final String announcementId = Integer.toString(announce.getId());
							final String announcementType = announce.getType().name();
							String announcementInital = "0";
							String announcementDelay = "0";
							String announcementRepeat = "0";
							final String announcementAuthor = announce.getAuthor();
							final String announcementContent = announce.getContent();
							if (announce instanceof AutoAnnouncement)
							{
								final AutoAnnouncement autoAnnounce = (AutoAnnouncement) announce;
								announcementInital = Long.toString(autoAnnounce.getInitial() / 1000);
								announcementDelay = Long.toString(autoAnnounce.getDelay() / 1000);
								announcementRepeat = Integer.toString(autoAnnounce.getRepeat());
							}
							
							content = content.replace("%id%", announcementId);
							content = content.replace("%type%", announcementType);
							content = content.replace("%initial%", announcementInital);
							content = content.replace("%delay%", announcementDelay);
							content = content.replace("%repeat%", announcementRepeat);
							content = content.replace("%author%", announcementAuthor);
							content = content.replace("%content%", announcementContent);
							HtmlUtil.sendCBHtml(activeChar, content);
							break;
						}
						
						activeChar.sendSysMessage("Announcement does not exist!");
						return onCommand("admin_announces list", activeChar);
					}
					case "list":
					{
						int page = 0;
						if (st.hasMoreTokens())
						{
							final String token = st.nextToken().replace("page=", "");
							if (StringUtil.isNumeric(token))
							{
								page = Integer.parseInt(token);
							}
						}
						
						String content = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/announces/announces-list.htm");
						final PageResult result = PageBuilder.newBuilder(AnnouncementsTable.getInstance().getAllAnnouncements(), 10, "bypass admin_announces list").currentPage(page).pageHandler(NextPrevPageHandler.INSTANCE).formatter(BypassParserFormatter.INSTANCE).style(ButtonsStyle.INSTANCE).bodyHandler((pages, announcement, sb) ->
						{
							sb.append("<tr>");
							sb.append("<td width=5></td>");
							sb.append("<td width=80>" + announcement.getId() + "</td>");
							sb.append("<td width=100>" + announcement.getType() + "</td>");
							sb.append("<td width=90>" + announcement.getAuthor() + "</td>");
							if ((announcement.getType() == AnnouncementType.AUTO_NORMAL) || (announcement.getType() == AnnouncementType.AUTO_CRITICAL))
							{
								sb.append("<td width=60><button action=\"bypass admin_announces restart " + announcement.getId() + "\" value=\"Restart\" width=\"60\" height=\"21\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
							}
							else
							{
								sb.append("<td width=60><button action=\"\" value=\"\" width=\"60\" height=\"21\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
							}
							
							if (announcement.getType() == AnnouncementType.EVENT)
							{
								sb.append("<td width=60><button action=\"bypass admin_announces show " + announcement.getId() + "\" value=\"Show\" width=\"60\" height=\"21\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
								sb.append("<td width=60></td>");
							}
							else
							{
								sb.append("<td width=60><button action=\"bypass admin_announces show " + announcement.getId() + "\" value=\"Show\" width=\"60\" height=\"21\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
								sb.append("<td width=60><button action=\"bypass admin_announces edit " + announcement.getId() + "\" value=\"Edit\" width=\"60\" height=\"21\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
							}
							sb.append("<td width=60><button action=\"bypass admin_announces remove " + announcement.getId() + "\" value=\"Remove\" width=\"60\" height=\"21\" back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
							sb.append("<td width=5></td>");
							sb.append("</tr>");
						}).build();
						
						content = content.replace("%pages%", result.getPagerTemplate().toString());
						content = content.replace("%announcements%", result.getBodyTemplate().toString());
						HtmlUtil.sendCBHtml(activeChar, content);
						break;
					}
				}
			}
		}
		
		return false;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
