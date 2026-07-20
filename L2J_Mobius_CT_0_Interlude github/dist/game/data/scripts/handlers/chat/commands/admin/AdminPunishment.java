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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentTask;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * @author UnAfraid, Skache
 */
public class AdminPunishment implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminPunishment.class.getName());
	
	private static final String PUNISHMENT = "data/html/admin/punishments/punishment.htm";
	private static final String PUNISHMENT_INFO = "data/html/admin/punishments/punishment-info.htm";
	private static final String PUNISHMENT_PLAYER = "data/html/admin/punishments/punishment-player.htm";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_punishment",
		"admin_punishment_add",
		"admin_punishment_remove",
		"admin_ban_acc",
		"admin_unban_acc",
		"admin_ban_hwid",
		"admin_unban_hwid",
		"admin_ban_chat",
		"admin_unban_chat",
		"admin_ban_char",
		"admin_unban_char",
		"admin_jail",
		"admin_unjail"
	};
	
	private static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		if (!st.hasMoreTokens())
		{
			return false;
		}
		
		final String cmd = st.nextToken();
		switch (cmd)
		{
			case "admin_punishment":
			{
				if (!st.hasMoreTokens())
				{
					String content = HtmCache.getInstance().getHtm(activeChar, PUNISHMENT);
					if (content != null)
					{
						content = content.replace("%punishments%", StringUtil.implode(PunishmentType.values(), ";"));
						content = content.replace("%affects%", StringUtil.implode(PunishmentAffect.values(), ";"));
						activeChar.sendPacket(new NpcHtmlMessage(0, 1, content));
					}
					else
					{
						LOGGER.warning(getClass().getSimpleName() + ": " + PUNISHMENT + " is missing.");
					}
				}
				else
				{
					final String subcmd = st.nextToken();
					switch (subcmd)
					{
						case "info":
						{
							String key = st.hasMoreTokens() ? st.nextToken() : null;
							final String af = st.hasMoreTokens() ? st.nextToken() : null;
							final String name = key;
							if ((key == null) || (af == null))
							{
								activeChar.sendSysMessage("Not enough data specified!");
								break;
							}
							
							final PunishmentAffect affect = PunishmentAffect.getByName(af);
							if (affect == null)
							{
								activeChar.sendSysMessage("Incorrect value specified for affect type!");
								break;
							}
							
							if (affect == PunishmentAffect.CHARACTER)
							{
								// Check if the character name exists.
								if (!CharInfoTable.getInstance().doesCharNameExist(name))
								{
									final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
									sm.addString(name);
									activeChar.sendPacket(sm);
									break;
								}
								
								key = findCharId(key); // Swap the name of the character with it's id.
							}
							
							String content = HtmCache.getInstance().getHtm(activeChar, PUNISHMENT_INFO);
							if (content != null)
							{
								final StringBuilder sb = new StringBuilder();
								for (PunishmentType type : PunishmentType.values())
								{
									if (PunishmentManager.getInstance().hasPunishment(key, affect, type))
									{
										final long expiration = PunishmentManager.getInstance().getPunishmentExpiration(key, affect, type);
										String expire = "never";
										if (expiration > 0)
										{
											// Synchronize date formatter since it is not thread safe.
											synchronized (DATE_FORMATTER)
											{
												expire = DATE_FORMATTER.format(new Date(expiration));
											}
										}
										
										sb.append("<tr><td><font color=\"LEVEL\">" + type + "</font></td><td>" + expire + "</td><td><a action=\"bypass -h admin_punishment_remove " + name + " " + affect + " " + type + "\">Remove</a></td></tr>");
									}
								}
								
								content = content.replace("%player_name%", name);
								content = content.replace("%punishments%", sb.toString());
								content = content.replace("%affects%", StringUtil.implode(PunishmentAffect.values(), ";"));
								content = content.replace("%affect_type%", affect.name());
								activeChar.sendPacket(new NpcHtmlMessage(0, 1, content));
							}
							else
							{
								LOGGER.warning(getClass().getSimpleName() + ": " + PUNISHMENT_INFO + " is missing.");
							}
							break;
						}
						case "player":
						{
							Player target = null;
							if (st.hasMoreTokens())
							{
								final String playerName = st.nextToken();
								if (playerName.isEmpty() && ((activeChar.getTarget() == null) || !activeChar.getTarget().isPlayer()))
								{
									return onCommand("admin_punishment", activeChar);
								}
								
								target = World.getInstance().getPlayer(playerName);
							}
							
							if ((target == null) && ((activeChar.getTarget() == null) || !activeChar.getTarget().isPlayer()))
							{
								activeChar.sendSysMessage("You must target player!");
								break;
							}
							
							if (target == null)
							{
								target = activeChar.getTarget().asPlayer();
							}
							
							String content = HtmCache.getInstance().getHtm(activeChar, PUNISHMENT_PLAYER);
							if (content != null)
							{
								content = content.replace("%player_name%", target.getName());
								content = content.replace("%punishments%", StringUtil.implode(PunishmentType.values(), ";"));
								content = content.replace("%acc%", target.getAccountName());
								content = content.replace("%char%", target.getName());
								content = content.replace("%ip%", target.getIPAddress());
								content = content.replace("%hwid%", (target.getClient() == null) || (target.getClient().getHardwareInfo() == null) ? "Unknown" : target.getClient().getHardwareInfo().getMacAddress());
								activeChar.sendPacket(new NpcHtmlMessage(0, 1, content));
							}
							else
							{
								LOGGER.warning(getClass().getSimpleName() + ": " + PUNISHMENT_PLAYER + " is missing.");
							}
							break;
						}
					}
				}
				break;
			}
			case "admin_punishment_add":
			{
				// Add new punishment
				String key = st.hasMoreTokens() ? st.nextToken() : null;
				final String af = st.hasMoreTokens() ? st.nextToken() : null;
				final String t = st.hasMoreTokens() ? st.nextToken() : null;
				final String exp = st.hasMoreTokens() ? st.nextToken() : null;
				String reason = st.hasMoreTokens() ? st.nextToken() : null;
				
				// Let's grab the other part of the reason if there is..
				if (reason != null)
				{
					while (st.hasMoreTokens())
					{
						reason += " " + st.nextToken();
					}
					
					if (!reason.isEmpty())
					{
						reason = reason.replaceAll("\\$", "\\\\\\$");
						reason = reason.replaceAll("\r\n", "<br1>");
						reason = reason.replace("<", "&lt;");
						reason = reason.replace(">", "&gt;");
					}
				}
				
				final String name = key;
				if ((key == null) || (af == null) || (t == null) || (exp == null) || (reason == null))
				{
					activeChar.sendSysMessage("Please fill all the fields!");
					break;
				}
				
				if (!StringUtil.isNumeric(exp) && !exp.equals("-1"))
				{
					activeChar.sendSysMessage("Incorrect value specified for expiration time!");
					break;
				}
				
				long expirationTime = Integer.parseInt(exp);
				if (expirationTime > 0)
				{
					expirationTime = System.currentTimeMillis() + (expirationTime * 60 * 1000);
				}
				
				final PunishmentAffect affect = PunishmentAffect.getByName(af);
				final PunishmentType type = PunishmentType.getByName(t);
				if ((affect == null) || (type == null))
				{
					activeChar.sendSysMessage("Incorrect value specified for affect/punishment type!");
					break;
				}
				
				if (affect == PunishmentAffect.CHARACTER)
				{
					// Check if the character name exists.
					if (!CharInfoTable.getInstance().doesCharNameExist(name))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
						sm.addString(name);
						activeChar.sendPacket(sm);
						break;
					}
					
					key = findCharId(key); // Swap the name of the character with it's id.
				}
				else if (affect == PunishmentAffect.IP)
				{
					try
					{
						final InetAddress addr = InetAddress.getByName(key);
						if (addr.isLoopbackAddress())
						{
							throw new UnknownHostException("You cannot ban any local address!");
						}
						else if (ServerConfig.GAME_SERVER_HOSTS.contains(addr.getHostAddress()))
						{
							throw new UnknownHostException("You cannot ban your gameserver's address!");
						}
					}
					catch (UnknownHostException e)
					{
						activeChar.sendSysMessage("You've entered an incorrect IP address!");
						activeChar.sendMessage(e.getMessage());
						break;
					}
				}
				
				// Check if we already put the same punishment on that guy ^^
				if (PunishmentManager.getInstance().hasPunishment(key, affect, type))
				{
					activeChar.sendSysMessage("Target is already affected by that punishment.");
					break;
				}
				
				// Punish him!
				PunishmentManager.getInstance().startPunishment(new PunishmentTask(key, affect, type, expirationTime, reason, activeChar.getName()));
				activeChar.sendSysMessage("Punishment " + type.name() + " have been applied to: " + affect + " " + name + "!");
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", cmd, affect.name(), name);
				return onCommand("admin_punishment info " + name + " " + affect.name(), activeChar);
			}
			case "admin_punishment_remove":
			{
				// Remove punishment.
				String key = st.hasMoreTokens() ? st.nextToken() : null;
				final String af = st.hasMoreTokens() ? st.nextToken() : null;
				final String t = st.hasMoreTokens() ? st.nextToken() : null;
				final String name = key;
				if ((key == null) || (af == null) || (t == null))
				{
					activeChar.sendSysMessage("Not enough data specified!");
					break;
				}
				
				final PunishmentAffect affect = PunishmentAffect.getByName(af);
				final PunishmentType type = PunishmentType.getByName(t);
				if ((affect == null) || (type == null))
				{
					activeChar.sendSysMessage("Incorrect value specified for affect/punishment type!");
					break;
				}
				
				if (affect == PunishmentAffect.CHARACTER)
				{
					// Check if the character name exists.
					if (!CharInfoTable.getInstance().doesCharNameExist(name))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
						sm.addString(name);
						activeChar.sendPacket(sm);
						break;
					}
					
					key = findCharId(key); // Swap the name of the character with it's id.
				}
				
				if (!PunishmentManager.getInstance().hasPunishment(key, affect, type))
				{
					activeChar.sendSysMessage("Target is not affected by that punishment!");
					break;
				}
				
				PunishmentManager.getInstance().stopPunishment(key, affect, type);
				activeChar.sendSysMessage("Punishment " + type.name() + " have been stopped to: " + affect + " " + name + "!");
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", cmd, affect.name(), name);
				return onCommand("admin_punishment info " + name + " " + affect.name(), activeChar);
			}
			case "admin_ban_char":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_add %s %s %s %s %s", st.nextToken(), PunishmentAffect.CHARACTER, PunishmentType.BAN, 0, "Banned by admin"), activeChar);
				}
				break;
			}
			case "admin_unban_char":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_remove %s %s %s", st.nextToken(), PunishmentAffect.CHARACTER, PunishmentType.BAN), activeChar);
				}
				break;
			}
			case "admin_ban_acc":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_add %s %s %s %s %s", st.nextToken(), PunishmentAffect.ACCOUNT, PunishmentType.BAN, 0, "Banned by admin"), activeChar);
				}
				break;
			}
			case "admin_unban_acc":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_remove %s %s %s", st.nextToken(), PunishmentAffect.ACCOUNT, PunishmentType.BAN), activeChar);
				}
				break;
			}
			case "admin_ban_hwid":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_add %s %s %s %s %s", st.nextToken(), PunishmentAffect.HWID, PunishmentType.BAN, 0, "Banned by admin"), activeChar);
				}
				break;
			}
			case "admin_unban_hwid":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_remove %s %s %s", st.nextToken(), PunishmentAffect.HWID, PunishmentType.BAN), activeChar);
				}
				break;
			}
			case "admin_ban_chat":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_add %s %s %s %s %s", st.nextToken(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN, 0, "Chat banned by admin"), activeChar);
				}
				break;
			}
			case "admin_unban_chat":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_remove %s %s %s", st.nextToken(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN), activeChar);
				}
				break;
			}
			case "admin_jail":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_add %s %s %s %s %s", st.nextToken(), PunishmentAffect.CHARACTER, PunishmentType.JAIL, 0, "Jailed by admin"), activeChar);
				}
				break;
			}
			case "admin_unjail":
			{
				if (st.hasMoreTokens())
				{
					return onCommand(String.format("admin_punishment_remove %s %s %s", st.nextToken(), PunishmentAffect.CHARACTER, PunishmentType.JAIL), activeChar);
				}
				break;
			}
		}
		
		return true;
	}
	
	private static String findCharId(String key)
	{
		final int charId = CharInfoTable.getInstance().getIdByName(key);
		if (charId > 0) // Yeah it's a char name!
		{
			return Integer.toString(charId);
		}
		
		return key;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
