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

import java.util.Calendar;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.ClanHallAuctionManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.model.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles all siege commands.
 * @author Zoey76
 */
public class AdminSiege implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		// Castle commands
		"admin_siege",
		"admin_add_attacker",
		"admin_add_defender",
		"admin_add_guard",
		"admin_list_siege_clans",
		"admin_clear_siege_list",
		"admin_move_defenders",
		"admin_spawn_doors",
		"admin_endsiege",
		"admin_startsiege",
		"admin_setsiegetime",
		"admin_setcastle",
		"admin_removecastle",
		
		// Clan hall commands
		"admin_clanhall",
		"admin_clanhallset",
		"admin_clanhalldel",
		"admin_clanhallopendoors",
		"admin_clanhallclosedoors",
		"admin_clanhallteleportself"
	};
	
	@Override
	public boolean onCommand(String commandValue, Player activeChar)
	{
		String command = commandValue;
		final StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken(); // Get actual command
		
		// Get castle
		Castle castle = null;
		ClanHall clanhall = null;
		if (st.hasMoreTokens())
		{
			Player player = null;
			if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				player = activeChar.getTarget().asPlayer();
			}
			
			String val = st.nextToken();
			if (command.startsWith("admin_clanhall"))
			{
				if (StringUtil.isNumeric(val))
				{
					clanhall = ClanHallTable.getInstance().getClanHallById(Integer.parseInt(val));
					Clan clan = null;
					switch (command)
					{
						case "admin_clanhallset":
						{
							if ((player == null) || (player.getClan() == null))
							{
								activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
								return false;
							}
							
							if (clanhall.getOwnerId() > 0)
							{
								activeChar.sendSysMessage("This Clan Hall is not free!");
								return false;
							}
							
							clan = player.getClan();
							if (clan.getHideoutId() > 0)
							{
								activeChar.sendSysMessage("You have already a Clan Hall!");
								return false;
							}
							
							if (!clanhall.isSiegableHall())
							{
								ClanHallTable.getInstance().setOwner(clanhall.getId(), clan);
								if (ClanHallAuctionManager.getInstance().getAuction(clanhall.getId()) != null)
								{
									ClanHallAuctionManager.getInstance().getAuction(clanhall.getId()).deleteAuctionFromDB();
								}
							}
							else
							{
								clanhall.setOwner(clan);
								clan.setHideoutId(clanhall.getId());
							}
							break;
						}
						case "admin_clanhalldel":
						{
							if (!clanhall.isSiegableHall())
							{
								if (!ClanHallTable.getInstance().isFree(clanhall.getId()))
								{
									ClanHallTable.getInstance().setFree(clanhall.getId());
									ClanHallAuctionManager.getInstance().initNPC(clanhall.getId());
								}
								else
								{
									activeChar.sendSysMessage("This Clan Hall is already free!");
								}
							}
							else
							{
								final int oldOwner = clanhall.getOwnerId();
								if (oldOwner > 0)
								{
									clanhall.free();
									clan = ClanTable.getInstance().getClan(oldOwner);
									if (clan != null)
									{
										clan.setHideoutId(0);
										clan.broadcastClanStatus();
									}
								}
							}
							break;
						}
						case "admin_clanhallopendoors":
						{
							clanhall.openCloseDoors(true);
							break;
						}
						case "admin_clanhallclosedoors":
						{
							clanhall.openCloseDoors(false);
							break;
						}
						case "admin_clanhallteleportself":
						{
							final ClanHallZone zone = clanhall.getZone();
							if (zone != null)
							{
								activeChar.teleToLocation(zone.getSpawnLoc(), true);
							}
							break;
						}
						default:
						{
							if (!clanhall.isSiegableHall())
							{
								showClanHallPage(activeChar, clanhall);
							}
							else
							{
								showSiegableHallPage(activeChar, (SiegableHall) clanhall);
							}
							break;
						}
					}
				}
			}
			else
			{
				castle = CastleManager.getInstance().getCastle(val);
				switch (command)
				{
					case "admin_add_attacker":
					{
						if (player == null)
						{
							activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
						}
						else
						{
							castle.getSiege().registerAttacker(player, true);
						}
						break;
					}
					case "admin_add_defender":
					{
						if (player == null)
						{
							activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
						}
						else
						{
							castle.getSiege().registerDefender(player, true);
						}
						break;
					}
					case "admin_add_guard":
					{
						if (st.hasMoreTokens())
						{
							val = st.nextToken();
							if (StringUtil.isNumeric(val))
							{
								castle.getSiege().getSiegeGuardManager().addSiegeGuard(activeChar, Integer.parseInt(val));
								break;
							}
						}
						
						// If doesn't have more tokens or token is not a number.
						activeChar.sendSysMessage("Usage: //add_guard castle npcId");
						break;
					}
					case "admin_clear_siege_list":
					{
						castle.getSiege().clearSiegeClan();
						break;
					}
					case "admin_endsiege":
					{
						castle.getSiege().endSiege();
						break;
					}
					case "admin_list_siege_clans":
					{
						castle.getSiege().listRegisterClan(activeChar);
						break;
					}
					case "admin_move_defenders":
					{
						activeChar.sendSysMessage("Not implemented yet.");
						break;
					}
					case "admin_setcastle":
					{
						if ((player == null) || (player.getClan() == null))
						{
							activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
						}
						else
						{
							castle.setOwner(player.getClan());
						}
						break;
					}
					case "admin_removecastle":
					{
						final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
						if (clan != null)
						{
							castle.removeOwner(clan);
						}
						else
						{
							activeChar.sendSysMessage("Unable to remove castle.");
						}
						break;
					}
					case "admin_setsiegetime":
					{
						if (st.hasMoreTokens())
						{
							final long currentTime = System.currentTimeMillis();
							final Calendar calendar = Calendar.getInstance();
							calendar.setTimeInMillis(castle.getSiegeDate().getTimeInMillis());
							val = st.nextToken();
							if ("month".equals(val))
							{
								final int month = calendar.get(Calendar.MONTH) + Integer.parseInt(st.nextToken());
								if ((calendar.getActualMinimum(Calendar.MONTH) > month) || (calendar.getActualMaximum(Calendar.MONTH) < month))
								{
									activeChar.sendSysMessage("Unable to change Siege Date - Incorrect month value only " + calendar.getActualMinimum(Calendar.MONTH) + "-" + calendar.getActualMaximum(Calendar.MONTH) + " is accepted!");
									return false;
								}
								
								calendar.set(Calendar.MONTH, month);
							}
							else if ("day".equals(val))
							{
								final int day = Integer.parseInt(st.nextToken());
								if ((calendar.getActualMinimum(Calendar.DAY_OF_MONTH) > day) || (calendar.getActualMaximum(Calendar.DAY_OF_MONTH) < day))
								{
									activeChar.sendSysMessage("Unable to change Siege Date - Incorrect day value only " + calendar.getActualMinimum(Calendar.DAY_OF_MONTH) + "-" + calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + " is accepted!");
									return false;
								}
								
								calendar.set(Calendar.DAY_OF_MONTH, day);
							}
							else if ("hour".equals(val))
							{
								final int hour = Integer.parseInt(st.nextToken());
								if ((calendar.getActualMinimum(Calendar.HOUR_OF_DAY) > hour) || (calendar.getActualMaximum(Calendar.HOUR_OF_DAY) < hour))
								{
									activeChar.sendSysMessage("Unable to change Siege Date - Incorrect hour value only " + calendar.getActualMinimum(Calendar.HOUR_OF_DAY) + "-" + calendar.getActualMaximum(Calendar.HOUR_OF_DAY) + " is accepted!");
									return false;
								}
								
								calendar.set(Calendar.HOUR_OF_DAY, hour);
							}
							else if ("min".equals(val))
							{
								final int min = Integer.parseInt(st.nextToken());
								if ((calendar.getActualMinimum(Calendar.MINUTE) > min) || (calendar.getActualMaximum(Calendar.MINUTE) < min))
								{
									activeChar.sendSysMessage("Unable to change Siege Date - Incorrect minute value only " + calendar.getActualMinimum(Calendar.MINUTE) + "-" + calendar.getActualMaximum(Calendar.MINUTE) + " is accepted!");
									return false;
								}
								
								calendar.set(Calendar.MINUTE, min);
							}
							
							if (calendar.getTimeInMillis() < currentTime)
							{
								activeChar.sendSysMessage("Unable to change Siege Date");
							}
							else if (calendar.getTimeInMillis() != castle.getSiegeDate().getTimeInMillis())
							{
								castle.getSiegeDate().setTimeInMillis(calendar.getTimeInMillis());
								castle.getSiege().saveSiegeDate();
								activeChar.sendSysMessage("Castle siege time for castle " + castle.getName() + " has been changed.");
							}
						}
						
						showSiegeTimePage(activeChar, castle);
						break;
					}
					case "admin_spawn_doors":
					{
						castle.spawnDoor();
						break;
					}
					case "admin_startsiege":
					{
						castle.getSiege().startSiege();
						break;
					}
					default:
					{
						showSiegePage(activeChar, castle.getName());
						break;
					}
				}
			}
		}
		else
		{
			showCastleSelectPage(activeChar);
		}
		
		return true;
	}
	
	/**
	 * Show castle select page.
	 * @param player the player
	 */
	private void showCastleSelectPage(Player player)
	{
		int i = 0;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setFile(player, "data/html/admin/castles.htm");
		final StringBuilder cList = new StringBuilder(500);
		for (Castle castle : CastleManager.getInstance().getCastles())
		{
			if (castle != null)
			{
				final String name = castle.getName();
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_siege " + name + "\">" + name + "</a></td>");
				i++;
			}
			
			if (i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		
		adminReply.replace("%castles%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
		{
			if (hall != null)
			{
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_chsiege_siegablehall " + hall.getId() + "\">" + hall.getName() + "</a></td>");
				i++;
			}
			
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		
		adminReply.replace("%siegableHalls%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (ClanHall clanhall : ClanHallTable.getInstance().getClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">" + clanhall.getName() + "</a></td>");
				i++;
			}
			
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		
		adminReply.replace("%clanhalls%", cList.toString());
		cList.setLength(0);
		i = 0;
		for (ClanHall clanhall : ClanHallTable.getInstance().getFreeClanHalls().values())
		{
			if (clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">" + clanhall.getName() + "</a></td>");
				i++;
			}
			
			if (i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		
		adminReply.replace("%freeclanhalls%", cList.toString());
		player.sendPacket(adminReply);
	}
	
	/**
	 * Show the siege page.
	 * @param player the player
	 * @param castleName the castle name
	 */
	private void showSiegePage(Player player, String castleName)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setFile(player, "data/html/admin/castle.htm");
		adminReply.replace("%castleName%", castleName);
		player.sendPacket(adminReply);
	}
	
	/**
	 * Show the siege time page.
	 * @param player the player
	 * @param castle the castle
	 */
	private void showSiegeTimePage(Player player, Castle castle)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setFile(player, "data/html/admin/castlesiegetime.htm");
		adminReply.replace("%castleName%", castle.getName());
		adminReply.replace("%time%", castle.getSiegeDate().getTime().toString());
		final Calendar newDay = Calendar.getInstance();
		boolean isSunday = false;
		if (newDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
		{
			isSunday = true;
		}
		else
		{
			newDay.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		}
		
		if (!SevenSigns.getInstance().isDateInSealValidPeriod(newDay))
		{
			newDay.add(Calendar.DAY_OF_MONTH, 7);
		}
		
		if (isSunday)
		{
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", newDay.get(Calendar.MONTH) + "/" + newDay.get(Calendar.DAY_OF_MONTH));
			newDay.add(Calendar.DAY_OF_MONTH, 13);
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", newDay.get(Calendar.MONTH) + "/" + newDay.get(Calendar.DAY_OF_MONTH));
		}
		else
		{
			adminReply.replace("%saturdaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%saturday%", newDay.get(Calendar.MONTH) + "/" + newDay.get(Calendar.DAY_OF_MONTH));
			newDay.add(Calendar.DAY_OF_MONTH, 1);
			adminReply.replace("%sundaylink%", String.valueOf(newDay.get(Calendar.DAY_OF_YEAR)));
			adminReply.replace("%sunday%", newDay.get(Calendar.MONTH) + "/" + newDay.get(Calendar.DAY_OF_MONTH));
		}
		
		player.sendPacket(adminReply);
	}
	
	/**
	 * Show the clan hall page.
	 * @param player the player
	 * @param clanhall the clan hall
	 */
	private void showClanHallPage(Player player, ClanHall clanhall)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage();
		adminReply.setFile(player, "data/html/admin/clanhall.htm");
		adminReply.replace("%clanhallName%", clanhall.getName());
		adminReply.replace("%clanhallId%", String.valueOf(clanhall.getId()));
		final Clan owner = ClanTable.getInstance().getClan(clanhall.getOwnerId());
		adminReply.replace("%clanhallOwner%", (owner == null) ? "None" : owner.getName());
		player.sendPacket(adminReply);
	}
	
	/**
	 * Show the siegable hall page.
	 * @param player the player
	 * @param hall the siegable hall
	 */
	private void showSiegableHallPage(Player player, SiegableHall hall)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setFile(null, "data/html/admin/siegablehall.htm");
		msg.replace("%clanhallId%", String.valueOf(hall.getId()));
		msg.replace("%clanhallName%", hall.getName());
		if (hall.getOwnerId() > 0)
		{
			final Clan owner = ClanTable.getInstance().getClan(hall.getOwnerId());
			msg.replace("%clanhallOwner%", (owner != null) ? owner.getName() : "No Owner");
		}
		else
		{
			msg.replace("%clanhallOwner%", "No Owner");
		}
		
		player.sendPacket(msg);
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
