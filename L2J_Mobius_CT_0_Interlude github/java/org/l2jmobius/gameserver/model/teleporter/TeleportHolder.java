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
package org.l2jmobius.gameserver.model.teleporter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Teleport holder
 * @author UnAfraid
 */
public class TeleportHolder
{
	private static final Logger LOGGER = Logger.getLogger(TeleportHolder.class.getName());
	
	private final String _name;
	private final TeleportType _type;
	private final List<TeleportLocation> _teleportData = new ArrayList<>();
	
	/**
	 * Constructor
	 * @param name name of teleport list
	 * @param type type of teleport list
	 */
	public TeleportHolder(String name, TeleportType type)
	{
		_name = name;
		_type = type;
	}
	
	/**
	 * Gets list identification (name).
	 * @return list name
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Gets type of teleport list.
	 * @return type of list
	 */
	public TeleportType getType()
	{
		return _type;
	}
	
	/**
	 * Create new teleport location in this holder.
	 * @param locData information about teleport location
	 */
	public void registerLocation(StatSet locData)
	{
		_teleportData.add(new TeleportLocation(_teleportData.size(), locData));
	}
	
	/**
	 * Gets teleport location with specific index.
	 * @param locationId index of location (begins with {@code 0})
	 * @return instance of {@link TeleportLocation} if found otherwise {@code null}
	 */
	public TeleportLocation getLocation(int locationId)
	{
		return _teleportData.get(locationId);
	}
	
	/**
	 * Gets all teleport locations registered in current holder.
	 * @return collection of {@link TeleportLocation}
	 */
	public List<TeleportLocation> getLocations()
	{
		return _teleportData;
	}
	
	/**
	 * Build HTML message from teleport list and send it to player.
	 * @param player receiver of HTML message
	 * @param npc teleporter
	 */
	public void showTeleportList(Player player, Npc npc)
	{
		showTeleportList(player, npc, "npc_" + npc.getObjectId() + "_teleport");
	}
	
	/**
	 * Build HTML message from teleport list and send it to player.
	 * @param player receiver of HTML message
	 * @param npc teleporter
	 * @param bypass bypass used while building message
	 */
	public void showTeleportList(Player player, Npc npc, String bypass)
	{
		if (isNoblesse() && !player.isNoble())
		{
			LOGGER.warning(player + " requested noblesse teleport without being noble!");
			return;
		}
		
		// Build html
		final StringBuilder sb = new StringBuilder();
		final StringBuilder sbF = new StringBuilder();
		for (TeleportLocation loc : _teleportData)
		{
			String finalName = loc.getName();
			String confirmDesc = loc.getName();
			if (shouldPayFee(player, loc))
			{
				final long fee = calculateFee(player, loc);
				if (fee != 0)
				{
					finalName += " - " + fee + " " + getItemName(loc.getFeeId());
				}
			}
			
			sb.append("<a action=\"bypass -h " + bypass + " " + _name + " " + loc.getId() + "\" msg=\"811;" + confirmDesc + "\">" + finalName + "</a><br1>");
		}
		sbF.append(sb.toString());
		
		// Send html message
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setFile(player, "data/html/teleporter/teleports.htm");
		msg.replace("%locations%", sbF.toString());
		player.sendPacket(msg);
	}
	
	/**
	 * Teleports player to final location
	 * @param player player being teleported
	 * @param npc teleporter
	 * @param locId destination
	 */
	public void doTeleport(Player player, Npc npc, int locId)
	{
		// Check if player is dead before other checks.
		if (player.isAlikeDead())
		{
			return;
		}
		
		if (isNoblesse() && !player.isNoble())
		{
			LOGGER.warning(player + " requested noblesse teleport without being noble!");
			return;
		}
		
		final TeleportLocation loc = getLocation(locId);
		if (loc == null)
		{
			LOGGER.warning(player + " requested unknown teleport location " + locId + " within list " + _name + "!");
			return;
		}
		
		// Check if castle is in siege
		if (!PlayerConfig.TELEPORT_WHILE_SIEGE_IN_PROGRESS)
		{
			for (int castleId : loc.getCastleId())
			{
				if (CastleManager.getInstance().getCastleById(castleId).getSiege().isInProgress())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
					return;
				}
			}
		}
		
		// Validate conditions for NORMAL teleport
		if (isNormalTeleport())
		{
			if (!PlayerConfig.TELEPORT_WHILE_SIEGE_IN_PROGRESS && npc.getCastle().getSiege().isInProgress())
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
				msg.setFile(player, "data/html/teleporter/castleteleporter-busy.htm");
				msg.replace("%npcname%", npc.getName());
				player.sendPacket(msg);
				return;
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (player.getKarma() > 0))
			{
				player.sendMessage("Go away, you're not welcome here.");
				return;
			}
		}
		
		// Check if player should pay fee.
		if (shouldPayFee(player, loc) && !player.destroyItemByItemId(ItemProcessType.FEE, loc.getFeeId(), calculateFee(player, loc), npc, true))
		{
			if (loc.getFeeId() == Inventory.ADENA_ID)
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			}
			else
			{
				player.sendMessage("You do not have enough " + getItemName(loc.getFeeId()) + ".");
			}
			return;
		}
		
		player.teleToLocation(loc);
	}
	
	/**
	 * Check if player have to play fee or not.
	 * @param player player which request teleport
	 * @param loc location where player should be teleported
	 * @return {@code true} when all requirements are met otherwise {@code false}
	 */
	private boolean shouldPayFee(Player player, TeleportLocation loc)
	{
		return !isNormalTeleport() || (((player.getLevel() > PlayerConfig.MAX_FREE_TELEPORT_LEVEL) || player.isSubClassActive()) && ((loc.getFeeId() != 0) && (loc.getFeeCount() > 0)));
	}
	
	/**
	 * Calculate fee amount for requested teleport.<br>
	 * For characters below level 40 teleport service is free.<br>
	 * From 8.00 pm to 00.00 from Monday till Tuesday for all characters there's a 50% discount on teleportation services
	 * @param player player which request teleport
	 * @param loc location where player should be teleported
	 * @return fee amount
	 */
	private int calculateFee(Player player, TeleportLocation loc)
	{
		if (isNormalTeleport())
		{
			if (!player.isSubClassActive() && (player.getLevel() <= PlayerConfig.MAX_FREE_TELEPORT_LEVEL))
			{
				return 0;
			}
			
			final Calendar cal = Calendar.getInstance();
			final int hour = cal.get(Calendar.HOUR_OF_DAY);
			final int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			if ((hour >= 20) && ((dayOfWeek >= Calendar.MONDAY) && (dayOfWeek <= Calendar.TUESDAY)))
			{
				return loc.getFeeCount() / 2;
			}
		}
		
		return loc.getFeeCount();
	}
	
	private boolean isNormalTeleport()
	{
		return _type == TeleportType.NORMAL;
	}
	
	/**
	 * Check if teleport list is for noblesse or not.
	 * @return {@code true} if is for noblesse otherwise {@code false}
	 */
	public boolean isNoblesse()
	{
		return (_type == TeleportType.NOBLES_ADENA) || (_type == TeleportType.NOBLES_TOKEN);
	}
	
	/**
	 * Gets name of specified item.
	 * @param itemId template id of item
	 * @return item name
	 */
	private String getItemName(int itemId)
	{
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		if (item != null)
		{
			return item.getName();
		}
		
		switch (itemId)
		{
			case -100: // PC_CAFE_POINTS
			{
				return "Player Commendation Points";
			}
			case -200: // CLAN_REPUTATION
			{
				return "Clan Reputation Points";
			}
			case -300: // FAME
			{
				return "Fame";
			}
		}
		
		return "Unknown item: " + itemId;
	}
}
