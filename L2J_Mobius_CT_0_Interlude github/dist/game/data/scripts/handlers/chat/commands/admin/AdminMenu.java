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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * This class handles following admin commands: - handles every admin menu command
 * @version $Revision: 1.3.2.6.2.4 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminMenu implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminMenu.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_char_manage",
		"admin_teleport_character_to_menu",
		"admin_recall_char_menu",
		"admin_recall_party_menu",
		"admin_recall_clan_menu",
		"admin_goto_char_menu",
		"admin_kick_menu",
		"admin_kill_menu",
		"admin_ban_menu",
		"admin_unban_menu"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_char_manage"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_teleport_character_to_menu"))
		{
			final String[] data = command.split(" ");
			if (data.length == 5)
			{
				final String playerName = data[1];
				final Player player = World.getInstance().getPlayer(playerName);
				if (player != null)
				{
					teleportCharacter(player, new Location(Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4])), activeChar, "Admin is teleporting you.");
				}
			}
			
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_recall_char_menu"))
		{
			try
			{
				final String targetName = command.substring(23);
				final Player player = World.getInstance().getPlayer(targetName);
				teleportCharacter(player, activeChar.getLocation(), activeChar, "Admin is teleporting you.");
				activeChar.setTarget(null);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Not important.
			}
		}
		else if (command.startsWith("admin_recall_party_menu"))
		{
			try
			{
				if (command.length() <= 23)
				{
					activeChar.sendSysMessage("Usage: //recall_party_menu <playerName>");
					return true;
				}
				
				final String targetName = command.substring(24);
				final Player player = World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
					return true;
				}
				
				if (!player.isInParty())
				{
					activeChar.sendSysMessage("Player is not in party.");
					teleportCharacter(player, activeChar.getLocation(), activeChar, "Admin is teleporting you.");
					return true;
				}
				
				for (Player pm : player.getParty().getMembers())
				{
					teleportCharacter(pm, activeChar.getLocation(), activeChar, "Your party is being teleported by an Admin.");
				}
				
				activeChar.sendSysMessage("You have teleported the party of " + player.getParty().getLeader().getName() + " (" + player.getParty().getMemberCount() + " members).");
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
		}
		else if (command.startsWith("admin_recall_clan_menu"))
		{
			try
			{
				if (command.length() <= 22)
				{
					activeChar.sendSysMessage("Usage: //recall_clan_menu <playerName>");
					return true;
				}
				
				final String targetName = command.substring(23);
				final Player player = World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
					return true;
				}
				
				final Clan clan = player.getClan();
				if (clan == null)
				{
					activeChar.sendSysMessage("Player is not in a clan.");
					teleportCharacter(player, activeChar.getLocation(), activeChar, "Admin is teleporting you.");
					return true;
				}
				
				for (Player member : clan.getOnlineMembers(0))
				{
					teleportCharacter(member, activeChar.getLocation(), activeChar, "Your clan is being teleported by an Admin.");
				}
				
				activeChar.sendSysMessage("You have teleported the clan \"" + clan.getName() + "\" (" + clan.getOnlineMembers(0).size() + " members).");
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
		}
		else if (command.startsWith("admin_goto_char_menu"))
		{
			try
			{
				final String targetName = command.substring(21);
				final Player player = World.getInstance().getPlayer(targetName);
				if (player == null)
				{
					activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_CURRENTLY_ONLINE);
					return true;
				}
				
				activeChar.setInstanceId(player.getInstanceId());
				teleportToCharacter(activeChar, player);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				// Not important.
			}
		}
		else if (command.equals("admin_kill_menu"))
		{
			handleKill(activeChar);
		}
		else if (command.startsWith("admin_kick_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				st.nextToken();
				final String player = st.nextToken();
				final Player plyr = World.getInstance().getPlayer(player);
				String text;
				if (plyr != null)
				{
					Disconnection.of(plyr).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					text = "You kicked " + plyr.getName() + " from the game.";
				}
				else
				{
					text = "Player " + player + " was not found in the game.";
				}
				
				activeChar.sendMessage(text);
			}
			
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_ban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				final String subCommand = "admin_ban_char";
				AdminCommandHandler.getInstance().onCommand(activeChar, subCommand + command.substring(14), true);
			}
			
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_unban_menu"))
		{
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1)
			{
				final String subCommand = "admin_unban_char";
				AdminCommandHandler.getInstance().onCommand(activeChar, subCommand + command.substring(16), true);
			}
			
			showMainPage(activeChar);
		}
		
		return true;
	}
	
	private void handleKill(Player activeChar)
	{
		handleKill(activeChar, null);
	}
	
	private void handleKill(Player activeChar, String player)
	{
		final WorldObject obj = activeChar.getTarget();
		Creature target = obj.asCreature();
		String filename = "main_menu.htm";
		if (player != null)
		{
			final Player plyr = World.getInstance().getPlayer(player);
			if (plyr != null)
			{
				target = plyr;
				activeChar.sendSysMessage("You killed " + plyr.getName());
			}
		}
		
		if (target != null)
		{
			if (target.isPlayer())
			{
				target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar, null);
				filename = "charmanage.htm";
			}
			else if (ChampionMonstersConfig.CHAMPION_ENABLE && target.isChampion())
			{
				target.reduceCurrentHp((target.getMaxHp() * ChampionMonstersConfig.CHAMPION_HP) + 1, activeChar, null);
			}
			else
			{
				target.reduceCurrentHp(target.getMaxHp() + 1, activeChar, null);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
		
		AdminHtml.showAdminHtml(activeChar, filename);
	}
	
	private void teleportCharacter(Player player, Location loc, Player activeChar, String message)
	{
		if (player != null)
		{
			player.sendMessage(message);
			player.teleToLocation(loc, true);
		}
		
		showMainPage(activeChar);
	}
	
	private void teleportToCharacter(Player activeChar, WorldObject target)
	{
		Player player = null;
		if ((target != null) && target.isPlayer())
		{
			player = target.asPlayer();
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if (player.getObjectId() == activeChar.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ON_YOURSELF);
		}
		else
		{
			activeChar.setInstanceId(player.getInstanceId());
			activeChar.teleToLocation(player.getLocation(), true);
			activeChar.sendSysMessage("You're teleporting yourself to character " + player.getName());
		}
		
		showMainPage(activeChar);
	}
	
	private void showMainPage(Player activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "charmanage.htm");
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
