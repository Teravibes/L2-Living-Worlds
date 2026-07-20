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
package handlers.chat.commands.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Clan War Start, Under Attack List, War List user commands.
 * @author Tempy
 */
public class ClanWarsList implements IUserCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(ClanWarsList.class.getName());
	private static final int[] COMMAND_IDS =
	{
		88,
		89,
		90
	};
	
	// SQL queries
	private static final String ATTACK_LIST = "SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan1=? AND clan_id=clan2 AND clan2 NOT IN (SELECT clan1 FROM clan_wars WHERE clan2=?)";
	private static final String UNDER_ATTACK_LIST = "SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan2=? AND clan_id=clan1 AND clan1 NOT IN (SELECT clan2 FROM clan_wars WHERE clan1=?)";
	private static final String WAR_LIST = "SELECT clan_name,clan_id,ally_id,ally_name FROM clan_data,clan_wars WHERE clan1=? AND clan_id=clan2 AND clan2 IN (SELECT clan1 FROM clan_wars WHERE clan2=?)";
	
	@Override
	public boolean onCommand(int id, Player player)
	{
		if ((id != COMMAND_IDS[0]) && (id != COMMAND_IDS[1]) && (id != COMMAND_IDS[2]))
		{
			return false;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			player.sendPacket(SystemMessageId.NOT_JOINED_IN_ANY_CLAN);
			return false;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			String query;
			
			// Attack List
			if (id == 88)
			{
				player.sendPacket(SystemMessageId.CLANS_YOU_VE_DECLARED_WAR_ON);
				query = ATTACK_LIST;
			}
			// Under Attack List
			else if (id == 89)
			{
				player.sendPacket(SystemMessageId.CLANS_THAT_HAVE_DECLARED_WAR_ON_YOU);
				query = UNDER_ATTACK_LIST;
			}
			// War List
			else
			{
				player.sendPacket(SystemMessageId.WAR_LIST);
				query = WAR_LIST;
			}
			
			try (PreparedStatement ps = con.prepareStatement(query))
			{
				ps.setInt(1, clan.getId());
				ps.setInt(2, clan.getId());
				SystemMessage sm;
				try (ResultSet rs = ps.executeQuery())
				{
					String clanName;
					int allyId;
					while (rs.next())
					{
						clanName = rs.getString("clan_name");
						allyId = rs.getInt("ally_id");
						if (allyId > 0)
						{
							// Target With Ally
							sm = new SystemMessage(SystemMessageId.S1_S2_ALLIANCE);
							sm.addString(clanName);
							sm.addString(rs.getString("ally_name"));
						}
						else
						{
							// Target Without Ally
							sm = new SystemMessage(SystemMessageId.S1_NO_ALLIANCE_EXISTS);
							sm.addString(clanName);
						}
						
						player.sendPacket(sm);
					}
				}
			}
			
			player.sendPacket(SystemMessageId.EMPTY_3);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
		}
		
		return true;
	}
	
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
