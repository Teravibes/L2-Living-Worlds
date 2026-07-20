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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.ConquerableHallSiegeConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.model.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author BiggBoss
 */
public class CHSiegeManager
{
	private static final Logger LOGGER = Logger.getLogger(CHSiegeManager.class.getName());
	private static final String SQL_LOAD_HALLS = "SELECT * FROM siegable_clanhall";
	
	private final Map<Integer, SiegableHall> _siegableHalls = new HashMap<>();
	
	protected CHSiegeManager()
	{
		loadClanHalls();
	}
	
	private void loadClanHalls()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery(SQL_LOAD_HALLS))
		{
			_siegableHalls.clear();
			
			while (rs.next())
			{
				final int id = rs.getInt("clanHallId");
				final StatSet set = new StatSet();
				set.set("id", id);
				set.set("name", rs.getString("name"));
				set.set("ownerId", rs.getInt("ownerId"));
				set.set("desc", rs.getString("desc"));
				set.set("location", rs.getString("location"));
				set.set("nextSiege", rs.getLong("nextSiege"));
				set.set("siegeLength", rs.getLong("siegeLength"));
				set.set("scheduleConfig", rs.getString("schedule_config"));
				final SiegableHall hall = new SiegableHall(set);
				_siegableHalls.put(id, hall);
				ClanHallTable.addClanHall(hall);
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _siegableHalls.size() + " conquerable clan halls.");
		}
		catch (Exception e)
		{
			LOGGER.warning("CHSiegeManager: Could not load siegable clan halls!:" + e.getMessage());
		}
	}
	
	public Map<Integer, SiegableHall> getConquerableHalls()
	{
		return _siegableHalls;
	}
	
	public SiegableHall getSiegableHall(int clanHall)
	{
		return _siegableHalls.get(clanHall);
	}
	
	public SiegableHall getNearbyClanHall(Creature creature)
	{
		return getNearbyClanHall(creature.getX(), creature.getY(), 10000);
	}
	
	public SiegableHall getNearbyClanHall(int x, int y, int maxDist)
	{
		ClanHallZone zone = null;
		for (Entry<Integer, SiegableHall> ch : _siegableHalls.entrySet())
		{
			zone = ch.getValue().getZone();
			if ((zone != null) && (zone.getDistanceToZone(x, y) < maxDist))
			{
				return ch.getValue();
			}
		}
		
		return null;
	}
	
	public ClanHallSiegeEngine getSiege(Creature creature)
	{
		final SiegableHall hall = getNearbyClanHall(creature);
		return hall == null ? null : hall.getSiege();
	}
	
	public void registerClan(Clan clan, SiegableHall hall, Player player)
	{
		if (clan.getLevel() < ConquerableHallSiegeConfig.CHS_CLAN_MINLEVEL)
		{
			player.sendMessage("Only clans of level " + ConquerableHallSiegeConfig.CHS_CLAN_MINLEVEL + " or higher may register for a castle siege");
		}
		else if (hall.isWaitingBattle())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_DEADLINE_TO_REGISTER_FOR_THE_SIEGE_OF_S1_HAS_PASSED);
			sm.addString(hall.getName());
			player.sendPacket(sm);
		}
		else if (hall.isInSiege())
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE);
		}
		else if (hall.getOwnerId() == clan.getId())
		{
			player.sendPacket(SystemMessageId.THE_CLAN_THAT_OWNS_THE_CASTLE_IS_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE);
		}
		else if ((clan.getCastleId() != 0) || (clan.getHideoutId() != 0))
		{
			player.sendPacket(SystemMessageId.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE);
		}
		else if (hall.getSiege().checkIsAttacker(clan))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
		}
		else if (isClanParticipating(clan))
		{
			player.sendPacket(SystemMessageId.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE);
		}
		else if (hall.getSiege().getAttackers().size() >= ConquerableHallSiegeConfig.CHS_MAX_ATTACKERS)
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_ATTACKER_SIDE);
		}
		else
		{
			hall.addAttacker(clan);
		}
	}
	
	public void unRegisterClan(Clan clan, SiegableHall hall)
	{
		if (!hall.isRegistering())
		{
			return;
		}
		
		hall.removeAttacker(clan);
	}
	
	public boolean isClanParticipating(Clan clan)
	{
		for (SiegableHall hall : _siegableHalls.values())
		{
			if ((hall.getSiege() != null) && hall.getSiege().checkIsAttacker(clan))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void onServerShutDown()
	{
		for (SiegableHall hall : _siegableHalls.values())
		{
			if (hall.getSiege() == null)
			{
				continue;
			}
			
			hall.getSiege().saveAttackers();
		}
	}
	
	public static CHSiegeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CHSiegeManager INSTANCE = new CHSiegeManager();
	}
}
