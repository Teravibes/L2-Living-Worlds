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
package org.l2jmobius.gameserver.model.siege.clanhalls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.siege.SiegeClan;
import org.l2jmobius.gameserver.model.siege.SiegeClanType;
import org.l2jmobius.gameserver.model.zone.type.SiegableHallZone;
import org.l2jmobius.gameserver.model.zone.type.SiegeZone;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;

/**
 * @author BiggBoss
 */
public class SiegableHall extends ClanHall
{
	private static final String SQL_SAVE = "UPDATE siegable_clanhall SET ownerId=?, nextSiege=? WHERE clanHallId=?";
	
	private Calendar _nextSiege;
	private final long _siegeLength;
	private final int[] _scheduleConfig =
	{
		7,
		0,
		0,
		12,
		0
	};
	
	private SiegeStatus _status = SiegeStatus.REGISTERING;
	private SiegeZone _siegeZone;
	
	private ClanHallSiegeEngine _siege;
	
	public SiegableHall(StatSet set)
	{
		super(set);
		_siegeLength = set.getLong("siegeLength");
		final String[] rawSchConfig = set.getString("scheduleConfig").split(";");
		if (rawSchConfig.length == 5)
		{
			for (int i = 0; i < 5; i++)
			{
				try
				{
					_scheduleConfig[i] = Integer.parseInt(rawSchConfig[i]);
				}
				catch (Exception e)
				{
					LOGGER.warning("SiegableHall - " + getName() + ": Wrong schedule_config parameters!");
				}
			}
		}
		else
		{
			LOGGER.warning(getName() + ": Wrong schedule_config value in siegable_halls table, using default (7 days)");
		}
		
		_nextSiege = Calendar.getInstance();
		final long nextSiege = set.getLong("nextSiege");
		if ((nextSiege - System.currentTimeMillis() - 3600000) < 0)
		{
			updateNextSiege();
		}
		else
		{
			_nextSiege.setTimeInMillis(nextSiege);
		}
		
		if (getOwnerId() != 0)
		{
			_isFree = false;
			loadFunctions();
		}
	}
	
	public void spawnDoor()
	{
		spawnDoor(false);
	}
	
	public void spawnDoor(boolean isDoorWeak)
	{
		for (Door door : getDoors())
		{
			if (door.isDead())
			{
				door.doRevive();
				if (isDoorWeak)
				{
					door.setCurrentHp(door.getMaxHp() / 2);
				}
				else
				{
					door.setCurrentHp(door.getMaxHp());
				}
			}
			
			if (door.isOpen())
			{
				door.closeMe();
			}
		}
	}
	
	@Override
	public void updateDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_SAVE))
		{
			ps.setInt(1, getOwnerId());
			ps.setLong(2, getNextSiegeTime());
			ps.setInt(3, getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: SiegableHall.updateDb(): " + e.getMessage(), e);
		}
	}
	
	public void setSiege(ClanHallSiegeEngine siegable)
	{
		_siege = siegable;
		_siegeZone.setSiegeInstance(siegable);
	}
	
	public ClanHallSiegeEngine getSiege()
	{
		return _siege;
	}
	
	public Calendar getSiegeDate()
	{
		return _nextSiege;
	}
	
	public long getNextSiegeTime()
	{
		return _nextSiege.getTimeInMillis();
	}
	
	public long getSiegeLength()
	{
		return _siegeLength;
	}
	
	public void setNextSiegeDate(long date)
	{
		_nextSiege.setTimeInMillis(date);
	}
	
	public void setNextSiegeDate(Calendar c)
	{
		_nextSiege = c;
	}
	
	public void updateNextSiege()
	{
		final Calendar callendar = Calendar.getInstance();
		callendar.add(Calendar.DAY_OF_YEAR, _scheduleConfig[0]);
		callendar.add(Calendar.MONTH, _scheduleConfig[1]);
		callendar.add(Calendar.YEAR, _scheduleConfig[2]);
		callendar.set(Calendar.HOUR_OF_DAY, _scheduleConfig[3]);
		callendar.set(Calendar.MINUTE, _scheduleConfig[4]);
		callendar.set(Calendar.SECOND, 0);
		setNextSiegeDate(callendar);
		updateDb();
	}
	
	public void addAttacker(Clan clan)
	{
		if (_siege != null)
		{
			_siege.getAttackers().put(clan.getId(), new SiegeClan(clan.getId(), SiegeClanType.ATTACKER));
		}
	}
	
	public void removeAttacker(Clan clan)
	{
		if (_siege != null)
		{
			_siege.getAttackers().remove(clan.getId());
		}
	}
	
	public boolean isRegistered(Clan clan)
	{
		return (_siege != null) && _siege.checkIsAttacker(clan);
	}
	
	public SiegeStatus getSiegeStatus()
	{
		return _status;
	}
	
	public boolean isRegistering()
	{
		return _status == SiegeStatus.REGISTERING;
	}
	
	public boolean isInSiege()
	{
		return _status == SiegeStatus.RUNNING;
	}
	
	public boolean isWaitingBattle()
	{
		return _status == SiegeStatus.WAITING_BATTLE;
	}
	
	public void updateSiegeStatus(SiegeStatus status)
	{
		_status = status;
	}
	
	public SiegeZone getSiegeZone()
	{
		return _siegeZone;
	}
	
	public void setSiegeZone(SiegeZone zone)
	{
		_siegeZone = zone;
	}
	
	public void updateSiegeZone(boolean active)
	{
		_siegeZone.setActive(active);
	}
	
	public void showSiegeInfo(Player player)
	{
		player.sendPacket(new SiegeInfo(this, player));
	}
	
	@Override
	public boolean isSiegableHall()
	{
		return true;
	}
	
	@Override
	public SiegableHallZone getZone()
	{
		return (SiegableHallZone) super.getZone();
	}
}
