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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.ConquerableHallSiegeConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.siege.Siegable;
import org.l2jmobius.gameserver.model.siege.SiegeClan;
import org.l2jmobius.gameserver.model.siege.SiegeClanType;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author BiggBoss
 */
public abstract class ClanHallSiegeEngine extends Script implements Siegable
{
	private static final String SQL_LOAD_ATTACKERS = "SELECT attacker_id FROM clanhall_siege_attackers WHERE clanhall_id = ?";
	private static final String SQL_SAVE_ATTACKERS = "INSERT INTO clanhall_siege_attackers VALUES (?,?)";
	private static final String SQL_LOAD_GUARDS = "SELECT * FROM clanhall_siege_guards WHERE clanHallId = ?";
	
	public static final int FORTRESS_RESSISTANCE = 21;
	public static final int DEVASTATED_CASTLE = 34;
	public static final int BANDIT_STRONGHOLD = 35;
	public static final int RAINBOW_SPRINGS = 62;
	public static final int BEAST_FARM = 63;
	public static final int FORTRESS_OF_DEAD = 64;
	
	protected final Logger LOGGER;
	
	private final Map<Integer, SiegeClan> _attackers = new ConcurrentHashMap<>();
	private Collection<Spawn> _guards;
	
	public SiegableHall _hall;
	public ScheduledFuture<?> _siegeTask;
	public boolean _missionAccomplished = false;
	
	public ClanHallSiegeEngine(int hallId)
	{
		LOGGER = Logger.getLogger(getClass().getName());
		_hall = CHSiegeManager.getInstance().getSiegableHall(hallId);
		_hall.setSiege(this);
		
		_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		LOGGER.config(_hall.getName() + " siege scheduled for " + _hall.getSiegeDate().getTime() + ".");
		loadAttackers();
	}
	
	public void loadAttackers()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_LOAD_ATTACKERS))
		{
			ps.setInt(1, _hall.getId());
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int id = rset.getInt("attacker_id");
					_attackers.put(id, new SiegeClan(id, SiegeClanType.ATTACKER));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getName() + ": Could not load siege attackers!");
		}
	}
	
	public void saveAttackers()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM clanhall_siege_attackers WHERE clanhall_id = ?"))
		{
			ps.setInt(1, _hall.getId());
			ps.execute();
			
			if (!_attackers.isEmpty())
			{
				try (PreparedStatement insert = con.prepareStatement(SQL_SAVE_ATTACKERS))
				{
					for (SiegeClan clan : _attackers.values())
					{
						insert.setInt(1, _hall.getId());
						insert.setInt(2, clan.getClanId());
						insert.execute();
						insert.clearParameters();
					}
				}
			}
			
			LOGGER.config(getName() + ": Successfully saved attackers to database.");
		}
		catch (Exception e)
		{
			LOGGER.warning(getName() + ": Couldn't save attacker list!");
		}
	}
	
	public void loadGuards()
	{
		if (_guards != null)
		{
			_guards.forEach(Spawn::startRespawn);
			return;
		}
		
		_guards = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_LOAD_GUARDS))
		{
			ps.setInt(1, _hall.getId());
			final ResultSet rset = ps.executeQuery();
			while (rset.next())
			{
				final Spawn spawn = new Spawn(rset.getInt("npcId"));
				spawn.setXYZ(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
				spawn.setHeading(rset.getInt("heading"));
				spawn.setRespawnDelay(rset.getInt("respawnDelay"));
				spawn.setAmount(1);
				_guards.add(spawn);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getName() + ": Could not load siege guards!");
		}
	}
	
	private void spawnSiegeGuards()
	{
		for (Spawn guard : _guards)
		{
			guard.init();
			if (guard.getRespawnDelay() == 0)
			{
				guard.stopRespawn();
			}
		}
	}
	
	private void unSpawnSiegeGuards()
	{
		if (_guards != null)
		{
			for (Spawn guard : _guards)
			{
				guard.stopRespawn();
				if (guard.getLastSpawn() != null)
				{
					guard.getLastSpawn().deleteMe();
				}
			}
		}
	}
	
	@Override
	public Set<Npc> getFlag(Clan clan)
	{
		Set<Npc> result = null;
		final SiegeClan sClan = getAttackerClan(clan);
		if (sClan != null)
		{
			result = sClan.getFlag();
		}
		
		return result;
	}
	
	public Map<Integer, SiegeClan> getAttackers()
	{
		return _attackers;
	}
	
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		
		return _attackers.containsKey(clan.getId());
	}
	
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		return false;
	}
	
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		return _attackers.get(clanId);
	}
	
	@Override
	public SiegeClan getAttackerClan(Clan clan)
	{
		return getAttackerClan(clan.getId());
	}
	
	@Override
	public Collection<SiegeClan> getAttackerClans()
	{
		return Collections.unmodifiableCollection(_attackers.values());
	}
	
	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> attackers = new ArrayList<>();
		for (Player pc : _hall.getSiegeZone().getPlayersInside())
		{
			final Clan clan = pc.getClan();
			if ((clan != null) && _attackers.containsKey(clan.getId()))
			{
				attackers.add(pc);
			}
		}
		
		return attackers;
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		return null;
	}
	
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		return null;
	}
	
	@Override
	public List<SiegeClan> getDefenderClans()
	{
		return null;
	}
	
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			final SiegeClan clan = new SiegeClan(_hall.getOwnerId(), SiegeClanType.ATTACKER);
			_attackers.put(clan.getClanId(), new SiegeClan(clan.getClanId(), SiegeClanType.ATTACKER));
		}
		
		_hall.free();
		_hall.banishForeigners();
		final SystemMessage msg = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
		msg.addString(getName());
		Broadcast.toAllOnlinePlayers(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		
		_siegeTask = ThreadPool.schedule(new SiegeStarts(), 3600000);
	}
	
	@Override
	public void startSiege()
	{
		if ((_attackers.size() < 1) && (_hall.getId() != 21)) // Fortress of resistance don't have attacker list
		{
			onSiegeEnds();
			_attackers.clear();
			_hall.updateNextSiege();
			_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getSiegeDate().getTimeInMillis());
			_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(sm);
			return;
		}
		
		_hall.spawnDoor();
		loadGuards();
		spawnSiegeGuards();
		_hall.updateSiegeZone(true);
		
		final byte state = 1;
		for (SiegeClan sClan : _attackers.values())
		{
			final Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (Player pc : clan.getOnlineMembers(0))
			{
				pc.setSiegeState(state);
				pc.broadcastUserInfo();
				pc.setInHideoutSiege(true);
			}
		}
		
		_hall.updateSiegeStatus(SiegeStatus.RUNNING);
		onSiegeStarts();
		_siegeTask = ThreadPool.schedule(new SiegeEnds(), _hall.getSiegeLength());
	}
	
	@Override
	public void endSiege()
	{
		final SystemMessage end = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_FINISHED);
		end.addString(_hall.getName());
		Broadcast.toAllOnlinePlayers(end);
		
		final Clan winner = getWinner();
		SystemMessage finalMsg = null;
		if (_missionAccomplished && (winner != null))
		{
			_hall.setOwner(winner);
			winner.setHideoutId(_hall.getId());
			finalMsg = new SystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
			finalMsg.addString(winner.getName());
		}
		else
		{
			finalMsg = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
		}
		
		finalMsg.addString(_hall.getName());
		Broadcast.toAllOnlinePlayers(finalMsg);
		_missionAccomplished = false;
		_hall.updateSiegeZone(false);
		_hall.updateNextSiege();
		_hall.spawnDoor(false);
		_hall.banishForeigners();
		
		final byte state = 0;
		for (SiegeClan sClan : _attackers.values())
		{
			final Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (Player player : clan.getOnlineMembers(0))
			{
				player.setSiegeState(state);
				player.broadcastUserInfo();
				player.setInHideoutSiege(false);
			}
		}
		
		// Update pvp flag for winners when siege zone becomes inactive
		for (Creature chr : _hall.getSiegeZone().getCharactersInside())
		{
			if ((chr != null) && chr.isPlayer())
			{
				chr.asPlayer().startPvPFlag();
			}
		}
		
		_attackers.clear();
		
		onSiegeEnds();
		
		_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		LOGGER.config("Siege of " + _hall.getName() + " scheduled for " + _hall.getSiegeDate().getTime() + ".");
		_hall.updateSiegeStatus(SiegeStatus.REGISTERING);
		unSpawnSiegeGuards();
	}
	
	@Override
	public void updateSiege()
	{
		cancelSiegeTask();
		_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getNextSiegeTime() - 3600000);
		LOGGER.config(_hall.getName() + " siege scheduled for " + _hall.getSiegeDate().getTime() + ".");
	}
	
	public void cancelSiegeTask()
	{
		if (_siegeTask != null)
		{
			_siegeTask.cancel(false);
		}
	}
	
	@Override
	public Calendar getSiegeDate()
	{
		return _hall.getSiegeDate();
	}
	
	@Override
	public boolean giveFame()
	{
		return ConquerableHallSiegeConfig.CHS_ENABLE_FAME;
	}
	
	@Override
	public int getFameAmount()
	{
		return ConquerableHallSiegeConfig.CHS_FAME_AMOUNT;
	}
	
	@Override
	public int getFameFrequency()
	{
		return ConquerableHallSiegeConfig.CHS_FAME_FREQUENCY;
	}
	
	public void broadcastNpcSay(Npc npc, ChatType type, String message)
	{
		final NpcSay npcSay = new NpcSay(npc.getObjectId(), type, npc.getId(), message);
		final int sourceRegion = MapRegionData.getInstance().getMapRegionLocId(npc);
		for (Player pc : World.getInstance().getPlayers())
		{
			if ((pc != null) && (MapRegionData.getInstance().getMapRegionLocId(pc) == sourceRegion))
			{
				pc.sendPacket(npcSay);
			}
		}
	}
	
	public Location getInnerSpawnLoc(Player player)
	{
		return null;
	}
	
	public boolean canPlantFlag()
	{
		return true;
	}
	
	public boolean doorIsAutoAttackable()
	{
		return true;
	}
	
	public void onSiegeStarts()
	{
	}
	
	public void onSiegeEnds()
	{
	}
	
	public abstract Clan getWinner();
	
	public class PrepareOwner implements Runnable
	{
		@Override
		public void run()
		{
			prepareOwner();
		}
	}
	
	public class SiegeStarts implements Runnable
	{
		@Override
		public void run()
		{
			startSiege();
		}
	}
	
	public class SiegeEnds implements Runnable
	{
		@Override
		public void run()
		{
			endSiege();
		}
	}
}
