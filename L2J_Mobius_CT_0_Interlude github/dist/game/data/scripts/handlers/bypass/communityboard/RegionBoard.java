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
package handlers.bypass.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Castle;

/**
 * Region board.
 * @author Zoey76, Skache
 */
public class RegionBoard implements IWriteBoardHandler
{
	private static final String CLAN_HALL_QUERY = "SELECT id, name, ownerId, location FROM clanhall WHERE location = ?";
	
	// Region data
	// @formatter:off
	private static final int[] REGIONS = { 1049, 1052, 1053, 1057, 1060, 1059, 1248, 1247, 1056 };
	// @formatter:on
	private static final String[] COMMANDS =
	{
		"_bbsloc"
	};
	
	private static class ClanHall
	{
		private final String name;
		private final int ownerId;
		
		public ClanHall(String name, int ownerId)
		{
			this.name = name;
			this.ownerId = ownerId;
		}
		
		public String getName()
		{
			return name;
		}
		
		public int getOwnerId()
		{
			return ownerId;
		}
	}
	
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.equals("_bbsloc"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Region", command);
			
			final String list = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region/region_list.html");
			final StringBuilder sb = new StringBuilder();
			final List<Castle> castles = CastleManager.getInstance().getCastles();
			
			for (int i = 0; i < REGIONS.length; i++)
			{
				final Castle castle = castles.get(i);
				final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
				
				String link = list.replace("%region_id%", String.valueOf(i)).replace("%region_name%", String.valueOf(REGIONS[i])).replace("%region_owning_clan%", (clan != null ? "<a action=\"bypass _bbsclan_clanhome;" + clan.getId() + "\">" + clan.getName() + "</a>" : "NPC")).replace("%region_owning_clan_alliance%", ((clan != null) && (clan.getAllyName() != null) ? clan.getAllyName() : "None")).replace("%region_tax_rate%", (castle.getTaxRate() * 100) + "%");
				sb.append(link);
			}
			
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region/region.html");
			html = html.replace("%region_list%", sb.toString());
			CommunityBoardHandler.separateAndSend(html, player);
		}
		else if (command.startsWith("_bbsloc;"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Region>", command);
			
			final String id = command.replace("_bbsloc;", "");
			if (!StringUtil.isNumeric(id))
			{
				LOG.warning(RegionBoard.class.getSimpleName() + ": " + player + " sent an invalid region bypass " + command + "!");
				return false;
			}
			
			final int regionId = Integer.parseInt(id);
			final List<Castle> castles = CastleManager.getInstance().getCastles();
			if ((regionId < 0) || (regionId >= castles.size()))
			{
				LOG.warning(RegionBoard.class.getSimpleName() + ": " + player + " requested invalid region ID " + regionId + "!");
				return false;
			}
			
			final Castle castle = castles.get(regionId);
			final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region/region_show.html");
			html = html.replace("%regionName%", castle.getName()).replace("%tax%", (castle.getTaxRate() * 100) + "%").replace("%lord%", clan != null ? clan.getLeaderName() : "NPC").replace("%clanName%", (clan != null ? "<a action=\"bypass _bbsclan_clanhome;" + clan.getId() + "\">" + clan.getName() + "</a>" : "NPC")).replace("%allyName%", ((clan != null) && (clan.getAllyName() != null) ? clan.getAllyName() : "None")).replace("%siegeDate%", new SimpleDateFormat("dd-MM-yyyy HH:mm").format(castle.getSiegeDate().getTimeInMillis()));
			
			final StringBuilder hallsList = new StringBuilder();
			hallsList.append("<br><br><table width=625 bgcolor=000000><tr><td width=5></td><td width=200>Clan Hall Name</td><td width=200>Owning Clan</td><td width=200>Clan Leader Name</td><td width=5></td></tr></table>");
			final Map<Integer, ClanHall> clanHalls = getClanHallsByLocation(castle.getName());
			if (clanHalls.isEmpty())
			{
				hallsList.append("<br><table width=610><tr><td align=\"center\">There are no clan halls in this region.</td></tr></table>");
			}
			else
			{
				for (ClanHall hall : clanHalls.values())
				{
					Clan hallOwner = ClanTable.getInstance().getClan(hall.getOwnerId());
					hallsList.append("<table><tr><td width=5></td><td width=200>").append(hall.getName()).append("</td><td width=200>").append(hallOwner != null ? "<a action=\"bypass _bbsclan_clanhome;" + hallOwner.getId() + "\">" + hallOwner.getName() + "</a>" : "None").append("</td><td width=200>").append(hallOwner != null ? hallOwner.getLeaderName() : "None").append("</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=630 height=1><br1>");
				}
			}
			
			html = html.replace("%hallsList%", hallsList.toString());
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return true;
	}
	
	private Map<Integer, ClanHall> getClanHallsByLocation(String location)
	{
		final Map<Integer, ClanHall> clanHalls = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CLAN_HALL_QUERY))
		{
			ps.setString(1, location);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					String name = rs.getString("name");
					int ownerId = rs.getInt("ownerId");
					ClanHall hall = new ClanHall(name, ownerId);
					clanHalls.put(rs.getInt("id"), hall);
				}
			}
		}
		catch (SQLException e)
		{
			LOG.warning(RegionBoard.class.getSimpleName() + ": Could not fetch clan halls data. " + e.getMessage());
		}
		
		return clanHalls;
	}
	
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// TODO: Implement.
		return false;
	}
}
