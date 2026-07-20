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
package org.l2jmobius.gameserver.model.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SiegeScheduleData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.MercTicketManager;
import org.l2jmobius.gameserver.managers.SiegeGuardManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.instance.ControlTower;
import org.l2jmobius.gameserver.model.actor.instance.FlameTower;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeOwnerChange;
import org.l2jmobius.gameserver.model.events.holders.sieges.castle.OnCastleSiegeStart;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

public class Siege implements Siegable
{
	protected static final Logger LOGGER = Logger.getLogger(Siege.class.getName());
	
	// typeId's
	public static final byte OWNER = -1;
	public static final byte DEFENDER = 0;
	public static final byte ATTACKER = 1;
	public static final byte DEFENDER_NOT_APPROVED = 2;
	
	private int _controlTowerCount;
	
	// must support Concurrent Modifications
	private final Collection<SiegeClan> _attackerClans = ConcurrentHashMap.newKeySet();
	private final Collection<SiegeClan> _defenderClans = ConcurrentHashMap.newKeySet();
	private final Collection<SiegeClan> _defenderWaitingClans = ConcurrentHashMap.newKeySet();
	
	// Castle setting
	private final List<ControlTower> _controlTowers = new ArrayList<>();
	private final List<FlameTower> _flameTowers = new ArrayList<>();
	private final Castle _castle;
	boolean _isInProgress = false;
	private boolean _isNormalSide = true; // true = Atk is Atk, false = Atk is Def
	protected boolean _isRegistrationOver = false;
	protected Calendar _siegeEndDate;
	private SiegeGuardManager _siegeGuardManager;
	protected ScheduledFuture<?> _scheduledStartSiegeTask = null;
	protected int _firstOwnerClanId = -1;
	
	public Siege(Castle castle)
	{
		_castle = castle;
		_siegeGuardManager = new SiegeGuardManager(getCastle());
		final SiegeScheduleDate schedule = SiegeScheduleData.getInstance().getScheduleDateForCastleId(_castle.getResidenceId());
		if ((schedule != null) && schedule.siegeEnabled())
		{
			startAutoTask();
		}
	}
	
	public class ScheduleEndSiegeTask implements Runnable
	{
		private final Castle _castleInst;
		
		public ScheduleEndSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}
		
		@Override
		public void run()
		{
			if (!_isInProgress)
			{
				return;
			}
			
			try
			{
				final long timeRemaining = _siegeEndDate.getTimeInMillis() - System.currentTimeMillis();
				if (timeRemaining > 3600000)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HOUR_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
					sm.addInt(2);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 3600000); // Prepare task for 1 hr left.
				}
				else if ((timeRemaining <= 3600000) && (timeRemaining > 600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_MINUTE_S_UNTIL_CASTLE_SIEGE_CONCLUSION);
					sm.addInt((int) timeRemaining / 60000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.CASTLE_SIEGE_S1_SECOND_S_LEFT);
					sm.addInt((int) timeRemaining / 1000);
					announceToPlayer(sm, true);
					ThreadPool.schedule(new ScheduleEndSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down
				}
				else
				{
					_castleInst.getSiege().endSiege();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public class ScheduleStartSiegeTask implements Runnable
	{
		private final Castle _castleInst;
		
		public ScheduleStartSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}
		
		@Override
		public void run()
		{
			_scheduledStartSiegeTask.cancel(false);
			if (_isInProgress)
			{
				return;
			}
			
			try
			{
				final long currentTime = System.currentTimeMillis();
				if (!isTimeRegistrationOver())
				{
					final long regTimeRemaining = getTimeRegistrationOverDate().getTimeInMillis() - currentTime;
					if (regTimeRemaining > 0)
					{
						_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), regTimeRemaining);
						return;
					}
					
					endTimeRegistration(true);
				}
				
				final long timeRemaining = getSiegeDate().getTimeInMillis() - currentTime;
				if (timeRemaining > 86400000)
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 86400000); // Prepare task for 24 before siege start to end registration
				}
				else if ((timeRemaining <= 86400000) && (timeRemaining > 13600000))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
					sm.addCastleId(getCastle().getResidenceId());
					Broadcast.toAllOnlinePlayers(sm);
					_isRegistrationOver = true;
					clearSiegeWaitingClan();
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 13600000); // Prepare task for 1 hr left before siege start.
				}
				else if ((timeRemaining <= 13600000) && (timeRemaining > 600000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if ((timeRemaining <= 600000) && (timeRemaining > 300000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if ((timeRemaining <= 300000) && (timeRemaining > 10000))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down
				}
				else if ((timeRemaining <= 10000) && (timeRemaining > 0))
				{
					_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down
				}
				else
				{
					_castleInst.getSiege().startSiege();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	@Override
	public void endSiege()
	{
		if (_isInProgress)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_FINISHED);
			sm.addCastleId(_castle.getResidenceId());
			Broadcast.toAllOnlinePlayers(sm);
			if (_castle.getOwnerId() > 0)
			{
				final Clan clan = ClanTable.getInstance().getClan(_castle.getOwnerId());
				sm = new SystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
				sm.addString(clan.getName());
				sm.addString(_castle.getName());
				Broadcast.toAllOnlinePlayers(sm);
				
				if (clan.getId() == _firstOwnerClanId)
				{
					// Owner is unchanged
					clan.increaseBloodAllianceCount();
				}
				else
				{
					_castle.setTicketBuyCount(0);
					for (ClanMember member : clan.getMembers())
					{
						if (member != null)
						{
							final Player player = member.getPlayer();
							if ((player != null) && player.isNoble())
							{
								Hero.getInstance().setCastleTaken(player.getObjectId(), _castle.getResidenceId());
							}
						}
					}
				}
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
				sm.addCastleId(_castle.getResidenceId());
				Broadcast.toAllOnlinePlayers(sm);
			}
			
			for (SiegeClan attackerClan : getAttackerClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(attackerClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
			}
			
			for (SiegeClan defenderClan : getDefenderClans())
			{
				final Clan clan = ClanTable.getInstance().getClan(defenderClan.getClanId());
				if (clan == null)
				{
					continue;
				}
				
				clan.clearSiegeKills();
				clan.clearSiegeDeaths();
			}
			
			_castle.updateClansReputation();
			removeFlags(); // Removes all flags. Note: Remove flag before teleporting players
			teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN); // Teleport to the second closest town
			_isInProgress = false; // Flag so that siege instance can be started
			updatePlayerSiegeStateFlags(true);
			saveCastleSiege(); // Save castle specific data
			clearSiegeClan(); // Clear siege clan from db
			removeTowers(); // Remove all towers from this castle
			_siegeGuardManager.unspawnSiegeGuard(); // Remove all spawned siege guard from this castle
			if (_castle.getOwnerId() > 0)
			{
				_siegeGuardManager.removeMercs();
			}
			
			_castle.spawnDoor(); // Respawn door to castle
			_castle.setFirstMidVictory(false);
			_castle.getZone().setActive(false);
			_castle.getZone().updateZoneStatusForCharactersInside();
			_castle.getZone().setSiegeInstance(null);
			
			// Notify to scripts.
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_FINISH, _castle))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeFinish(this), _castle);
			}
		}
	}
	
	private void removeDefender(SiegeClan sc)
	{
		if (sc != null)
		{
			getDefenderClans().remove(sc);
		}
	}
	
	private void removeAttacker(SiegeClan sc)
	{
		if (sc != null)
		{
			getAttackerClans().remove(sc);
		}
	}
	
	private void addDefender(SiegeClan sc, SiegeClanType type)
	{
		if (sc == null)
		{
			return;
		}
		
		sc.setType(type);
		getDefenderClans().add(sc);
	}
	
	private void addAttacker(SiegeClan sc)
	{
		if (sc == null)
		{
			return;
		}
		
		sc.setType(SiegeClanType.ATTACKER);
		getAttackerClans().add(sc);
	}
	
	/**
	 * When control of castle changed during siege.
	 */
	public void midVictory()
	{
		if (_isInProgress) // Siege still in progress
		{
			if (_castle.getOwnerId() > 0)
			{
				_siegeGuardManager.removeMercs(); // Remove all merc entry from db
			}
			
			if (getDefenderClans().isEmpty() && // If defender doesn't exist (Pc vs Npc)
				(getAttackerClans().size() == 1)) // Only 1 attacker
			{
				final SiegeClan scNewOwner = getAttackerClan(_castle.getOwnerId());
				removeAttacker(scNewOwner);
				addDefender(scNewOwner, SiegeClanType.OWNER);
				endSiege();
				return;
			}
			
			if (_castle.getOwnerId() > 0)
			{
				final int allyId = ClanTable.getInstance().getClan(_castle.getOwnerId()).getAllyId();
				
				// If defender doesn't exist (Pc vs Npc) and only an alliance attacks and the player's clan is in an alliance
				if (getDefenderClans().isEmpty() && (allyId != 0))
				{
					boolean allinsamealliance = true;
					for (SiegeClan sc : getAttackerClans())
					{
						if ((sc != null) && (ClanTable.getInstance().getClan(sc.getClanId()).getAllyId() != allyId))
						{
							allinsamealliance = false;
						}
					}
					
					if (allinsamealliance)
					{
						final SiegeClan scNewOwner = getAttackerClan(_castle.getOwnerId());
						removeAttacker(scNewOwner);
						addDefender(scNewOwner, SiegeClanType.OWNER);
						endSiege();
						return;
					}
				}
				
				for (SiegeClan sc : getDefenderClans())
				{
					if (sc != null)
					{
						removeDefender(sc);
						addAttacker(sc);
					}
				}
				
				final SiegeClan scNewOwner = getAttackerClan(_castle.getOwnerId());
				removeAttacker(scNewOwner);
				addDefender(scNewOwner, SiegeClanType.OWNER);
				
				// The player's clan is in an alliance
				for (Clan clan : ClanTable.getInstance().getClanAllies(allyId))
				{
					final SiegeClan sc = getAttackerClan(clan.getId());
					if (sc != null)
					{
						removeAttacker(sc);
						addDefender(sc, SiegeClanType.DEFENDER);
					}
				}
				
				_castle.setFirstMidVictory(true);
				teleportPlayer(SiegeTeleportWhoType.Attacker, TeleportWhereType.SIEGEFLAG); // Teleport to the second closest town
				teleportPlayer(SiegeTeleportWhoType.Spectator, TeleportWhereType.TOWN); // Teleport to the second closest town
				removeDefenderFlags(); // Removes defenders' flags
				_castle.removeUpgrade(); // Remove all castle upgrade
				_castle.spawnDoor(true); // Respawn door to castle but make them weaker (50% hp)
				removeTowers(); // Remove all towers from this castle
				_controlTowerCount = 0; // Each new siege midvictory CT are completely respawned.
				spawnControlTower();
				spawnFlameTower();
				updatePlayerSiegeStateFlags(false);
				
				// Notify to scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_OWNER_CHANGE, _castle))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeOwnerChange(this), _castle);
				}
			}
		}
	}
	
	/**
	 * When siege starts.
	 */
	@Override
	public void startSiege()
	{
		if (!_isInProgress)
		{
			_firstOwnerClanId = _castle.getOwnerId();
			if (getAttackerClans().isEmpty())
			{
				SystemMessage sm;
				if (_firstOwnerClanId <= 0)
				{
					sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S_SIEGE_WAS_CANCELED_BECAUSE_THERE_WERE_NO_CLANS_THAT_PARTICIPATED);
					final Clan ownerClan = ClanTable.getInstance().getClan(_firstOwnerClanId);
					ownerClan.increaseBloodAllianceCount();
				}
				
				sm.addCastleId(_castle.getResidenceId());
				Broadcast.toAllOnlinePlayers(sm);
				saveCastleSiege();
				return;
			}
			
			_isNormalSide = true; // Atk is now atk
			_isInProgress = true; // Flag so that same siege instance cannot be started again
			loadSiegeClan(); // Load siege clan from db
			updatePlayerSiegeStateFlags(false);
			teleportPlayer(SiegeTeleportWhoType.NotOwner, TeleportWhereType.TOWN); // Teleport to the closest town
			_controlTowerCount = 0;
			spawnControlTower(); // Spawn control tower
			spawnFlameTower(); // Spawn control tower
			_castle.spawnDoor(); // Spawn door
			spawnSiegeGuard(); // Spawn siege guard
			MercTicketManager.getInstance().deleteTickets(_castle.getResidenceId()); // remove the tickets from the ground
			_castle.getZone().setSiegeInstance(this);
			_castle.getZone().setActive(true);
			_castle.getZone().updateZoneStatusForCharactersInside();
			
			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, SiegeManager.getInstance().getSiegeLength());
			ThreadPool.schedule(new ScheduleEndSiegeTask(_castle), 1000); // Prepare auto end task
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_STARTED);
			sm.addCastleId(_castle.getResidenceId());
			Broadcast.toAllOnlinePlayers(sm);
			
			// Notify to scripts.
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CASTLE_SIEGE_START, _castle))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCastleSiegeStart(this), _castle);
			}
		}
	}
	
	/**
	 * Announce to player.
	 * @param message The SystemMessage to send to player
	 * @param bothSides True - broadcast to both attackers and defenders. False - only to defenders.
	 */
	public void announceToPlayer(SystemMessage message, boolean bothSides)
	{
		for (SiegeClan siegeClans : getDefenderClans())
		{
			for (Player member : ClanTable.getInstance().getClan(siegeClans.getClanId()).getOnlineMembers(0))
			{
				member.sendPacket(message);
			}
		}
		
		if (bothSides)
		{
			for (SiegeClan siegeClans : getAttackerClans())
			{
				for (Player member : ClanTable.getInstance().getClan(siegeClans.getClanId()).getOnlineMembers(0))
				{
					if (member != null)
					{
						member.sendPacket(message);
					}
				}
			}
		}
	}
	
	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		Clan clan;
		for (SiegeClan siegeclan : getAttackerClans())
		{
			if (siegeclan == null)
			{
				continue;
			}
			
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 1);
					member.setSiegeSide(getCastle().getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				
				member.updateUserInfo();
				World.getInstance().forEachVisibleObject(member, Player.class, player ->
				{
					if (!member.isVisibleFor(player))
					{
						return;
					}
					
					player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
					if (member.hasSummon())
					{
						player.sendPacket(new RelationChanged(member.getSummon(), member.getRelation(player), member.isAutoAttackable(player)));
					}
				});
			}
		}
		
		for (SiegeClan siegeclan : getDefenderClans())
		{
			if (siegeclan == null)
			{
				continue;
			}
			
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player member : clan.getOnlineMembers(0))
			{
				if (member == null)
				{
					continue;
				}
				
				if (clear)
				{
					member.setSiegeState((byte) 0);
					member.setSiegeSide(0);
					member.setInSiege(false);
					member.stopFameTask();
				}
				else
				{
					member.setSiegeState((byte) 2);
					member.setSiegeSide(getCastle().getResidenceId());
					if (checkIfInZone(member))
					{
						member.setInSiege(true);
						member.startFameTask(PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY * 1000, PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS);
					}
				}
				
				member.updateUserInfo();
				World.getInstance().forEachVisibleObject(member, Player.class, player ->
				{
					if (!member.isVisibleFor(player))
					{
						return;
					}
					
					player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
					if (member.hasSummon())
					{
						player.sendPacket(new RelationChanged(member.getSummon(), member.getRelation(player), member.isAutoAttackable(player)));
					}
				});
			}
		}
	}
	
	/**
	 * Approve clan as defender for siege
	 * @param clanId The int of player's clan id
	 */
	public void approveSiegeDefenderClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}
		
		saveSiegeClan(ClanTable.getInstance().getClan(clanId), DEFENDER, true);
		loadSiegeClan();
	}
	
	/**
	 * @param object
	 * @return true if object is inside the zone
	 */
	public boolean checkIfInZone(WorldObject object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return true if object is inside the zone
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return _isInProgress && getCastle().checkIfInZone(x, y, z); // Castle zone during siege
	}
	
	/**
	 * Return true if clan is attacker
	 * @param clan The Clan of the player
	 */
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		return getAttackerClan(clan) != null;
	}
	
	/**
	 * Return true if clan is defender
	 * @param clan The Clan of the player
	 */
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		return getDefenderClan(clan) != null;
	}
	
	/**
	 * @param clan The Clan of the player
	 * @return true if clan is defender waiting approval
	 */
	public boolean checkIsDefenderWaiting(Clan clan)
	{
		return getDefenderWaitingClan(clan) != null;
	}
	
	/** Clear all registered siege clans from database for castle */
	public void clearSiegeClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=?"))
		{
			ps.setInt(1, getCastle().getResidenceId());
			ps.execute();
			
			if (getCastle().getOwnerId() > 0)
			{
				try (PreparedStatement delete = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?"))
				{
					delete.setInt(1, getCastle().getOwnerId());
					delete.execute();
				}
			}
			
			getAttackerClans().clear();
			getDefenderClans().clear();
			_defenderWaitingClans.clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: clearSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/** Clear all siege clans waiting for approval from database for castle */
	public void clearSiegeWaitingClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and type = 2"))
		{
			ps.setInt(1, getCastle().getResidenceId());
			ps.execute();
			
			_defenderWaitingClans.clear();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: clearSiegeWaitingClan(): " + e.getMessage(), e);
		}
	}
	
	/** Return list of Player registered as attacker in the zone. */
	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> players = new ArrayList<>();
		Clan clan;
		for (SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (Player player : clan.getOnlineMembers(0))
			{
				if (player.isInSiege())
				{
					players.add(player);
				}
			}
		}
		
		return players;
	}
	
	/**
	 * @return list of Player in the zone.
	 */
	public List<Player> getPlayersInZone()
	{
		return getCastle().getZone().getPlayersInside();
	}
	
	/**
	 * @return list of Player owning the castle in the zone.
	 */
	public List<Player> getOwnersInZone()
	{
		final List<Player> players = new ArrayList<>();
		Clan clan;
		for (SiegeClan siegeclan : getDefenderClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			if (clan.getId() != getCastle().getOwnerId())
			{
				continue;
			}
			
			for (Player player : clan.getOnlineMembers(0))
			{
				if (player.isInSiege())
				{
					players.add(player);
				}
			}
		}
		
		return players;
	}
	
	/**
	 * @return list of Player not registered as attacker or defender in the zone.
	 */
	public List<Player> getSpectatorsInZone()
	{
		final List<Player> players = new ArrayList<>();
		for (Player player : getCastle().getZone().getPlayersInside())
		{
			if (player == null)
			{
				continue;
			}
			
			if (!player.isInSiege())
			{
				players.add(player);
			}
		}
		
		return players;
	}
	
	/**
	 * Control Tower was killed
	 */
	public void killedCT()
	{
		_controlTowerCount--;
		if (_controlTowerCount < 0)
		{
			_controlTowerCount = 0;
		}
	}
	
	/**
	 * Remove the flag that was killed
	 * @param flag
	 */
	public void killedFlag(Npc flag)
	{
		if (flag == null)
		{
			return;
		}
		
		for (SiegeClan clan : getAttackerClans())
		{
			if (clan.removeFlag(flag))
			{
				return;
			}
		}
	}
	
	/**
	 * Display list of registered clans
	 * @param player
	 */
	public void listRegisterClan(Player player)
	{
		player.sendPacket(new SiegeInfo(getCastle(), player));
	}
	
	/**
	 * Register clan as attacker
	 * @param player The Player of the player trying to register
	 */
	public void registerAttacker(Player player)
	{
		registerAttacker(player, false);
	}
	
	public void registerAttacker(Player player, boolean force)
	{
		if (player.getClan() == null)
		{
			return;
		}
		
		final int allyId = getCastle().getOwnerId() != 0 ? ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId() : 0;
		if ((allyId != 0) && (player.getClan().getAllyId() == allyId) && !force)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_ON_THE_ATTACKING_SIDE_BECAUSE_YOU_ARE_PART_OF_AN_ALLIANCE_WITH_THE_CLAN_THAT_OWNS_THE_CASTLE);
			return;
		}
		
		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), getCastle().getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
			}
			else
			{
				saveSiegeClan(player.getClan(), ATTACKER, false); // Save to database
			}
			return;
		}
		
		if (checkIfCanRegister(player, ATTACKER))
		{
			saveSiegeClan(player.getClan(), ATTACKER, false); // Save to database
		}
	}
	
	/**
	 * Register a clan as defender.
	 * @param player the player to register
	 */
	public void registerDefender(Player player)
	{
		registerDefender(player, false);
	}
	
	public void registerDefender(Player player, boolean force)
	{
		if (getCastle().getOwnerId() <= 0)
		{
			player.sendMessage("You cannot register as a defender because " + getCastle().getName() + " is owned by NPC.");
			return;
		}
		
		if (force)
		{
			if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), getCastle().getResidenceId()))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
			}
			else
			{
				saveSiegeClan(player.getClan(), DEFENDER_NOT_APPROVED, false); // Save to database
			}
			return;
		}
		
		if (checkIfCanRegister(player, DEFENDER_NOT_APPROVED))
		{
			saveSiegeClan(player.getClan(), DEFENDER_NOT_APPROVED, false); // Save to database
		}
	}
	
	/**
	 * Remove clan from siege
	 * @param clanId The int of player's clan id
	 */
	public void removeSiegeClan(int clanId)
	{
		if (clanId <= 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id=? and clan_id=?"))
		{
			ps.setInt(1, getCastle().getResidenceId());
			ps.setInt(2, clanId);
			ps.execute();
			
			loadSiegeClan();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: removeSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Remove clan from siege
	 * @param clan clan being removed
	 */
	public void removeSiegeClan(Clan clan)
	{
		if ((clan == null) || (clan.getCastleId() == getCastle().getResidenceId()) || !SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getResidenceId()))
		{
			return;
		}
		
		removeSiegeClan(clan.getId());
	}
	
	/**
	 * Remove clan from siege
	 * @param player The Player of player/clan being removed
	 */
	public void removeSiegeClan(Player player)
	{
		removeSiegeClan(player.getClan());
	}
	
	/**
	 * Start the auto tasks.
	 */
	public void startAutoTask()
	{
		correctSiegeDateTime();
		
		LOGGER.info("Siege of " + getCastle().getName() + ": " + getCastle().getSiegeDate().getTime());
		loadSiegeClan();
		
		// Schedule siege auto start
		if (_scheduledStartSiegeTask != null)
		{
			_scheduledStartSiegeTask.cancel(false);
		}
		
		_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(getCastle()), 1000);
	}
	
	/**
	 * Teleport players
	 * @param teleportWho
	 * @param teleportWhere
	 */
	public void teleportPlayer(SiegeTeleportWhoType teleportWho, TeleportWhereType teleportWhere)
	{
		final List<Player> players;
		switch (teleportWho)
		{
			case Owner:
			{
				players = getOwnersInZone();
				break;
			}
			case NotOwner:
			{
				players = getPlayersInZone();
				final Iterator<Player> it = players.iterator();
				while (it.hasNext())
				{
					final Player player = it.next();
					if ((player == null) || player.inObserverMode() || ((player.getClanId() > 0) && (player.getClanId() == getCastle().getOwnerId())))
					{
						it.remove();
					}
				}
				break;
			}
			case Attacker:
			{
				players = getAttackersInZone();
				break;
			}
			case Spectator:
			{
				players = getSpectatorsInZone();
				break;
			}
			default:
			{
				players = Collections.emptyList();
			}
		}
		
		for (Player player : players)
		{
			if (player.isGM() || player.isJailed())
			{
				continue;
			}
			
			player.teleToLocation(teleportWhere);
		}
	}
	
	/**
	 * Add clan as attacker
	 * @param clanId The int of clan's id
	 */
	private void addAttacker(int clanId)
	{
		getAttackerClans().add(new SiegeClan(clanId, SiegeClanType.ATTACKER)); // Add registered attacker to attacker list
	}
	
	/**
	 * Add clan as defender
	 * @param clanId The int of clan's id
	 */
	private void addDefender(int clanId)
	{
		getDefenderClans().add(new SiegeClan(clanId, SiegeClanType.DEFENDER)); // Add registered defender to defender list
	}
	
	/**
	 * <p>
	 * Add clan as defender with the specified type
	 * </p>
	 * @param clanId The int of clan's id
	 * @param type the type of the clan
	 */
	private void addDefender(int clanId, SiegeClanType type)
	{
		getDefenderClans().add(new SiegeClan(clanId, type));
	}
	
	/**
	 * Add clan as defender waiting approval
	 * @param clanId The int of clan's id
	 */
	private void addDefenderWaiting(int clanId)
	{
		_defenderWaitingClans.add(new SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING)); // Add registered defender to defender list
	}
	
	/**
	 * @param player The Player of the player trying to register
	 * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
	 * @return true if the player can register.
	 */
	private boolean checkIfCanRegister(Player player, byte typeId)
	{
		if (_isRegistrationOver)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_DEADLINE_TO_REGISTER_FOR_THE_SIEGE_OF_S1_HAS_PASSED);
			sm.addCastleId(getCastle().getResidenceId());
			player.sendPacket(sm);
		}
		else if (_isInProgress)
		{
			player.sendPacket(SystemMessageId.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE);
		}
		else if ((player.getClan() == null) || (player.getClan().getLevel() < SiegeManager.getInstance().getSiegeClanMinLevel()))
		{
			player.sendPacket(SystemMessageId.ONLY_CLANS_OF_LEVEL_4_OR_HIGHER_MAY_REGISTER_FOR_A_CASTLE_SIEGE);
		}
		else if (player.getClan().getId() == getCastle().getOwnerId())
		{
			player.sendPacket(SystemMessageId.THE_CLAN_THAT_OWNS_THE_CASTLE_IS_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE);
		}
		else if (player.getClan().getCastleId() > 0)
		{
			player.sendPacket(SystemMessageId.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE);
		}
		else if (SiegeManager.getInstance().checkIsRegistered(player.getClan(), getCastle().getResidenceId()))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_A_SIEGE_BATTLE);
		}
		else if (checkIfAlreadyRegisteredForSameDay(player.getClan()))
		{
			player.sendPacket(SystemMessageId.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE);
		}
		else if ((typeId == ATTACKER) && (getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans()))
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_ATTACKER_SIDE);
		}
		else if (((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROVED) || (typeId == OWNER)) && ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= SiegeManager.getInstance().getDefenderMaxClans()))
		{
			player.sendPacket(SystemMessageId.NO_MORE_REGISTRATIONS_MAY_BE_ACCEPTED_FOR_THE_DEFENDER_SIDE);
		}
		else
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param clan The Clan of the player trying to register
	 * @return true if the clan has already registered to a siege for the same day.
	 */
	public boolean checkIfAlreadyRegisteredForSameDay(Clan clan)
	{
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (siege == this)
			{
				continue;
			}
			
			if (siege.getSiegeDate().get(Calendar.DAY_OF_WEEK) == getSiegeDate().get(Calendar.DAY_OF_WEEK))
			{
				if (siege.checkIsAttacker(clan))
				{
					return true;
				}
				
				if (siege.checkIsDefender(clan))
				{
					return true;
				}
				
				if (siege.checkIsDefenderWaiting(clan))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Return the correct siege date as Calendar.
	 */
	public void correctSiegeDateTime()
	{
		boolean corrected = false;
		if (getCastle().getSiegeDate().getTimeInMillis() < System.currentTimeMillis())
		{
			// Since siege has past reschedule it to the next one
			// This is usually caused by server being down
			corrected = true;
			setNextSiegeDate();
		}
		
		if (corrected)
		{
			saveSiegeDate();
		}
	}
	
	/** Load siege clans. */
	private void loadSiegeClan()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT clan_id,type FROM siege_clans where castle_id=?"))
		{
			getAttackerClans().clear();
			getDefenderClans().clear();
			_defenderWaitingClans.clear();
			
			// Add castle owner as defender (add owner first so that they are on the top of the defender list)
			if (getCastle().getOwnerId() > 0)
			{
				addDefender(getCastle().getOwnerId(), SiegeClanType.OWNER);
			}
			
			ps.setInt(1, getCastle().getResidenceId());
			try (ResultSet rs = ps.executeQuery())
			{
				int typeId;
				while (rs.next())
				{
					typeId = rs.getInt("type");
					if (typeId == DEFENDER)
					{
						addDefender(rs.getInt("clan_id"));
					}
					else if (typeId == ATTACKER)
					{
						addAttacker(rs.getInt("clan_id"));
					}
					else if (typeId == DEFENDER_NOT_APPROVED)
					{
						addDefenderWaiting(rs.getInt("clan_id"));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadSiegeClan(): " + e.getMessage(), e);
		}
	}
	
	/** Remove all spawned towers. */
	private void removeTowers()
	{
		for (FlameTower ct : _flameTowers)
		{
			ct.deleteMe();
		}
		
		for (ControlTower ct : _controlTowers)
		{
			ct.deleteMe();
		}
		
		_flameTowers.clear();
		_controlTowers.clear();
	}
	
	/** Remove all flags. */
	private void removeFlags()
	{
		for (SiegeClan sc : getAttackerClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
		
		for (SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}
	
	/** Remove flags from defenders. */
	private void removeDefenderFlags()
	{
		for (SiegeClan sc : getDefenderClans())
		{
			if (sc != null)
			{
				sc.removeFlags();
			}
		}
	}
	
	/** Save castle siege related to database. */
	private void saveCastleSiege()
	{
		setNextSiegeDate(); // Set the next set date for 2 weeks from now
		
		// Schedule Time registration end
		getTimeRegistrationOverDate().setTimeInMillis(System.currentTimeMillis());
		getTimeRegistrationOverDate().add(Calendar.DAY_OF_MONTH, 1);
		getCastle().setTimeRegistrationOver(false);
		
		saveSiegeDate(); // Save the new date
		startAutoTask(); // Prepare auto start siege and end registration
	}
	
	/** Save siege date to database. */
	public void saveSiegeDate()
	{
		if (_scheduledStartSiegeTask != null)
		{
			_scheduledStartSiegeTask.cancel(true);
			_scheduledStartSiegeTask = ThreadPool.schedule(new ScheduleStartSiegeTask(getCastle()), 1000);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE castle SET siegeDate = ?, regTimeEnd = ?, regTimeOver = ?  WHERE id = ?"))
		{
			ps.setLong(1, getSiegeDate().getTimeInMillis());
			ps.setLong(2, getTimeRegistrationOverDate().getTimeInMillis());
			ps.setString(3, String.valueOf(isTimeRegistrationOver()));
			ps.setInt(4, getCastle().getResidenceId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: saveSiegeDate(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Save registration to database.
	 * @param clan The Clan of player
	 * @param typeId -1 = owner 0 = defender, 1 = attacker, 2 = defender waiting
	 * @param isUpdateRegistration
	 */
	private void saveSiegeClan(Clan clan, byte typeId, boolean isUpdateRegistration)
	{
		if (clan.getCastleId() > 0)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if ((typeId == DEFENDER) || (typeId == DEFENDER_NOT_APPROVED) || (typeId == OWNER))
			{
				if ((getDefenderClans().size() + getDefenderWaitingClans().size()) >= SiegeManager.getInstance().getDefenderMaxClans())
				{
					return;
				}
			}
			else if (getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans())
			{
				return;
			}
			
			if (!isUpdateRegistration)
			{
				try (PreparedStatement ps = con.prepareStatement("INSERT INTO siege_clans (clan_id,castle_id,type,castle_owner) values (?,?,?,0)"))
				{
					ps.setInt(1, clan.getId());
					ps.setInt(2, getCastle().getResidenceId());
					ps.setInt(3, typeId);
					ps.execute();
				}
			}
			else
			{
				try (PreparedStatement ps = con.prepareStatement("UPDATE siege_clans SET type = ? WHERE castle_id = ? AND clan_id = ?"))
				{
					ps.setInt(1, typeId);
					ps.setInt(2, getCastle().getResidenceId());
					ps.setInt(3, clan.getId());
					ps.execute();
				}
			}
			
			if ((typeId == DEFENDER) || (typeId == OWNER))
			{
				addDefender(clan.getId());
			}
			else if (typeId == ATTACKER)
			{
				addAttacker(clan.getId());
			}
			else if (typeId == DEFENDER_NOT_APPROVED)
			{
				addDefenderWaiting(clan.getId());
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: saveSiegeClan(Pledge clan, int typeId, boolean isUpdateRegistration): " + e.getMessage(), e);
		}
	}
	
	/** Set the date for the next siege. */
	private void setNextSiegeDate()
	{
		final SiegeScheduleDate holder = SiegeScheduleData.getInstance().getScheduleDateForCastleId(_castle.getResidenceId());
		if ((holder == null) || !holder.siegeEnabled())
		{
			return;
		}
		
		final Calendar calendar = _castle.getSiegeDate();
		if (calendar.getTimeInMillis() < System.currentTimeMillis())
		{
			calendar.setTimeInMillis(System.currentTimeMillis());
		}
		
		calendar.set(Calendar.DAY_OF_WEEK, holder.getDay());
		calendar.set(Calendar.HOUR_OF_DAY, holder.getHour());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		
		if (calendar.before(Calendar.getInstance()))
		{
			calendar.add(Calendar.WEEK_OF_YEAR, SiegeManager.getInstance().getSiegeCycle());
		}
		
		if (CastleManager.getInstance().getSiegeDates(calendar.getTimeInMillis()) < holder.getMaxConcurrent())
		{
			CastleManager.getInstance().registerSiegeDate(getCastle().getResidenceId(), calendar.getTimeInMillis());
			
			Broadcast.toAllOnlinePlayers(new SystemMessage(SystemMessageId.S1_HAS_ANNOUNCED_THE_CASTLE_SIEGE_TIME).addCastleId(_castle.getResidenceId()));
			
			// Allow registration for next siege
			_isRegistrationOver = false;
		}
		else
		{
			// Deny registration for next siege
			_isRegistrationOver = true;
		}
	}
	
	/**
	 * Spawn control tower.
	 */
	private void spawnControlTower()
	{
		for (TowerSpawn ts : SiegeManager.getInstance().getControlTowers(getCastle().getResidenceId()))
		{
			try
			{
				final Spawn spawn = new Spawn(ts.getId());
				spawn.setLocation(ts.getLocation());
				_controlTowers.add((ControlTower) spawn.doSpawn(false));
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getName() + ": Cannot spawn control tower! " + e);
			}
		}
		
		_controlTowerCount = _controlTowers.size();
	}
	
	/**
	 * Spawn flame tower.
	 */
	private void spawnFlameTower()
	{
		for (TowerSpawn ts : SiegeManager.getInstance().getFlameTowers(getCastle().getResidenceId()))
		{
			try
			{
				final Spawn spawn = new Spawn(ts.getId());
				spawn.setLocation(ts.getLocation());
				final FlameTower tower = (FlameTower) spawn.doSpawn(false);
				tower.setUpgradeLevel(ts.getUpgradeLevel());
				tower.setZoneList(ts.getZoneList());
				_flameTowers.add(tower);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getName() + ": Cannot spawn flame tower! " + e);
			}
		}
	}
	
	/**
	 * Spawn siege guard.
	 */
	private void spawnSiegeGuard()
	{
		getSiegeGuardManager().spawnSiegeGuard();
		
		// Register guard to the closest Control Tower
		// When CT dies, so do all the guards that it controls
		for (Spawn spawn : getSiegeGuardManager().getSiegeGuardSpawn())
		{
			if (spawn == null)
			{
				continue;
			}
			
			ControlTower closestCt = null;
			double distanceClosest = Integer.MAX_VALUE;
			for (ControlTower ct : _controlTowers)
			{
				if (ct == null)
				{
					continue;
				}
				
				final double distance = ct.calculateDistance3D(spawn);
				if (distance < distanceClosest)
				{
					closestCt = ct;
					distanceClosest = distance;
				}
			}
			
			if (closestCt != null)
			{
				closestCt.registerGuard(spawn);
			}
		}
	}
	
	@Override
	public SiegeClan getAttackerClan(Clan clan)
	{
		return clan == null ? null : getAttackerClan(clan.getId());
	}
	
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		for (SiegeClan sc : getAttackerClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		
		return null;
	}
	
	@Override
	public Collection<SiegeClan> getAttackerClans()
	{
		return _isNormalSide ? _attackerClans : _defenderClans;
	}
	
	public int getAttackerRespawnDelay()
	{
		return SiegeManager.getInstance().getAttackerRespawnDelay();
	}
	
	public Castle getCastle()
	{
		return _castle == null ? null : _castle;
	}
	
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		return clan == null ? null : getDefenderClan(clan.getId());
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		for (SiegeClan sc : getDefenderClans())
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		
		return null;
	}
	
	@Override
	public Collection<SiegeClan> getDefenderClans()
	{
		return _isNormalSide ? _defenderClans : _attackerClans;
	}
	
	public SiegeClan getDefenderWaitingClan(Clan clan)
	{
		return clan == null ? null : getDefenderWaitingClan(clan.getId());
	}
	
	public SiegeClan getDefenderWaitingClan(int clanId)
	{
		for (SiegeClan sc : _defenderWaitingClans)
		{
			if ((sc != null) && (sc.getClanId() == clanId))
			{
				return sc;
			}
		}
		
		return null;
	}
	
	public Collection<SiegeClan> getDefenderWaitingClans()
	{
		return _defenderWaitingClans;
	}
	
	public boolean isInProgress()
	{
		return _isInProgress;
	}
	
	public boolean isRegistrationOver()
	{
		return _isRegistrationOver;
	}
	
	public boolean isTimeRegistrationOver()
	{
		return getCastle().isTimeRegistrationOver();
	}
	
	@Override
	public Calendar getSiegeDate()
	{
		return getCastle().getSiegeDate();
	}
	
	public Calendar getTimeRegistrationOverDate()
	{
		return getCastle().getTimeRegistrationOverDate();
	}
	
	public void endTimeRegistration(boolean automatic)
	{
		getCastle().setTimeRegistrationOver(true);
		if (!automatic)
		{
			saveSiegeDate();
		}
	}
	
	@Override
	public Set<Npc> getFlag(Clan clan)
	{
		if (clan != null)
		{
			final SiegeClan sc = getAttackerClan(clan);
			if (sc != null)
			{
				return sc.getFlag();
			}
		}
		
		return null;
	}
	
	public SiegeGuardManager getSiegeGuardManager()
	{
		if (_siegeGuardManager == null)
		{
			_siegeGuardManager = new SiegeGuardManager(getCastle());
		}
		
		return _siegeGuardManager;
	}
	
	public int getControlTowerCount()
	{
		return _controlTowerCount;
	}
	
	@Override
	public boolean giveFame()
	{
		return true;
	}
	
	@Override
	public int getFameFrequency()
	{
		return PlayerConfig.CASTLE_ZONE_FAME_TASK_FREQUENCY;
	}
	
	@Override
	public int getFameAmount()
	{
		return PlayerConfig.CASTLE_ZONE_FAME_AQUIRE_POINTS;
	}
	
	@Override
	public void updateSiege()
	{
	}
}
