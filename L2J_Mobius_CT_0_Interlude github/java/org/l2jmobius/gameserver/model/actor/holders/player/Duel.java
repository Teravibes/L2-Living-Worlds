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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.enums.player.DuelResult;
import org.l2jmobius.gameserver.model.actor.instance.Door;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelReady;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelStart;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Duel
{
	protected static final Logger LOGGER = Logger.getLogger(Duel.class.getName());
	
	public static final int DUELSTATE_NODUEL = 0;
	public static final int DUELSTATE_DUELLING = 1;
	public static final int DUELSTATE_DEAD = 2;
	public static final int DUELSTATE_WINNER = 3;
	public static final int DUELSTATE_INTERRUPTED = 4;
	
	private static final PlaySound B04_S01 = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
	
	private static final int PARTY_DUEL_DURATION = 300;
	private static final int PLAYER_DUEL_DURATION = 120;
	
	private final int _duelId;
	private Player _playerA;
	private Player _playerB;
	private final boolean _partyDuel;
	private final Calendar _duelEndTime;
	private int _surrenderRequest = 0;
	private int _countdown = 5;
	private boolean _finished = false;
	
	private final Map<Integer, PlayerCondition> _playerConditions = new ConcurrentHashMap<>();
	private int _duelInstanceId;
	
	public Duel(Player playerA, Player playerB, int partyDuel, int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = partyDuel == 1;
		if (_partyDuel)
		{
			for (Player member : _playerA.getParty().getMembers())
			{
				member.setStartingDuel();
			}
			
			for (Player member : _playerB.getParty().getMembers())
			{
				member.setStartingDuel();
			}
		}
		else
		{
			_playerA.setStartingDuel();
			_playerB.setStartingDuel();
		}
		
		_duelEndTime = Calendar.getInstance();
		_duelEndTime.add(Calendar.SECOND, _partyDuel ? PARTY_DUEL_DURATION : PLAYER_DUEL_DURATION);
		setFinished(false);
		
		if (_partyDuel)
		{
			// inform players that they will be ported shortly
			final SystemMessage sm = new SystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
		}
		
		// Schedule duel start
		ThreadPool.schedule(new ScheduleStartDuelTask(this), 3000);
	}
	
	public static class PlayerCondition
	{
		private Player _player;
		private double _hp;
		private double _mp;
		private double _cp;
		private boolean _paDuel;
		private int _x;
		private int _y;
		private int _z;
		private Set<Skill> _debuffs;
		
		public PlayerCondition(Player player, boolean partyDuel)
		{
			if (player == null)
			{
				return;
			}
			
			_player = player;
			_hp = _player.getCurrentHp();
			_mp = _player.getCurrentMp();
			_cp = _player.getCurrentCp();
			_paDuel = partyDuel;
			if (_paDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}
		
		public void restoreCondition()
		{
			if (_player == null)
			{
				return;
			}
			
			_player.setCurrentHp(_hp);
			_player.setCurrentMp(_mp);
			_player.setCurrentCp(_cp);
			
			if (_paDuel)
			{
				teleportBack();
			}
			
			if (_debuffs != null) // Debuff removal
			{
				for (Skill skill : _debuffs)
				{
					if (skill != null)
					{
						_player.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
					}
				}
			}
		}
		
		public void registerDebuff(Skill debuff)
		{
			if (_debuffs == null)
			{
				_debuffs = ConcurrentHashMap.newKeySet();
			}
			
			_debuffs.add(debuff);
		}
		
		public void teleportBack()
		{
			if (_paDuel)
			{
				_player.teleToLocation(_x, _y, _z);
			}
		}
		
		public Player getPlayer()
		{
			return _player;
		}
	}
	
	public class ScheduleDuelTask implements Runnable
	{
		private final Duel _duel;
		
		public ScheduleDuelTask(Duel duel)
		{
			_duel = duel;
		}
		
		@Override
		public void run()
		{
			try
			{
				switch (_duel.checkEndDuelCondition())
				{
					case CANCELED:
					{
						// do not schedule duel end if it was interrupted
						setFinished(true);
						_duel.endDuel(DuelResult.CANCELED);
						break;
					}
					case CONTINUE:
					{
						ThreadPool.schedule(this, 1000);
						break;
					}
					default:
					{
						setFinished(true);
						playKneelAnimation();
						ThreadPool.schedule(new ScheduleEndDuelTask(_duel, _duel.checkEndDuelCondition()), 5000);
						InstanceManager.getInstance().destroyInstance(_duel.getDueldInstanceId());
						break;
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "There has been a problem while runing a duel task!", e);
			}
		}
	}
	
	public static class ScheduleStartDuelTask implements Runnable
	{
		private final Duel _duel;
		
		public ScheduleStartDuelTask(Duel duel)
		{
			_duel = duel;
		}
		
		@Override
		public void run()
		{
			try
			{
				// start/continue countdown
				final int count = _duel.countdown();
				if (count == 4)
				{
					// Save player conditions before teleporting players
					_duel.savePlayerConditions();
					
					// TODO: stadia manager needs a function to return an unused stadium for duels
					// currently only teleports to the same stadium
					_duel.teleportPlayers(-83760, -238825, -3331);
					
					// give players 20 seconds to complete teleport and get ready (its ought to be 30 on official..)
					ThreadPool.schedule(this, _duel.isPartyDuel() ? 20000 : 1);
				}
				else if (count > 0) // duel not started yet - continue countdown
				{
					ThreadPool.schedule(this, 1000);
				}
				else
				{
					_duel.startDuel();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public static class ScheduleEndDuelTask implements Runnable
	{
		private final Duel _duel;
		private final DuelResult _result;
		
		public ScheduleEndDuelTask(Duel duel, DuelResult result)
		{
			_duel = duel;
			_result = result;
		}
		
		@Override
		public void run()
		{
			try
			{
				_duel.endDuel(_result);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "There has been a problem while runing a duel end task!", e);
			}
		}
	}
	
	public int getDueldInstanceId()
	{
		return _duelInstanceId;
	}
	
	/**
	 * Stops all players from attacking. Used for duel timeout / interrupt.
	 */
	private void stopFighting()
	{
		final ActionFailed af = ActionFailed.STATIC_PACKET;
		if (_partyDuel)
		{
			for (Player temp : _playerA.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(Intention.ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
				if (temp.hasSummon() && (temp.getSummon() != null) && !temp.getSummon().isDead())
				{
					temp.getSummon().abortCast();
					temp.getSummon().getAI().setIntention(Intention.ACTIVE);
					temp.getSummon().setTarget(null);
					temp.getSummon().sendPacket(af);
				}
			}
			
			for (Player temp : _playerB.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(Intention.ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
				if (temp.hasSummon() && (temp.getSummon() != null) && !temp.getSummon().isDead())
				{
					temp.getSummon().abortCast();
					temp.getSummon().getAI().setIntention(Intention.ACTIVE);
					temp.getSummon().setTarget(null);
					temp.getSummon().sendPacket(af);
				}
			}
		}
		else
		{
			_playerA.abortCast();
			_playerB.abortCast();
			_playerA.getAI().setIntention(Intention.ACTIVE);
			_playerA.setTarget(null);
			_playerB.getAI().setIntention(Intention.ACTIVE);
			_playerB.setTarget(null);
			_playerA.sendPacket(af);
			_playerB.sendPacket(af);
			if (_playerA.hasSummon() && (_playerA.getSummon() != null) && !_playerA.getSummon().isDead())
			{
				_playerA.getSummon().abortCast();
				_playerA.getSummon().getAI().setIntention(Intention.ACTIVE);
				_playerA.getSummon().setTarget(null);
				_playerA.getSummon().sendPacket(af);
			}
			
			if (_playerB.hasSummon() && (_playerB.getSummon() != null) && !_playerB.getSummon().isDead())
			{
				_playerB.getSummon().abortCast();
				_playerB.getSummon().getAI().setIntention(Intention.ACTIVE);
				_playerB.getSummon().setTarget(null);
				_playerB.getSummon().sendPacket(af);
			}
		}
	}
	
	/**
	 * Check if a player engaged in pvp combat (only for 1on1 duels)
	 * @param sendMessage
	 * @return returns true if a duelist is engaged in Pvp combat
	 */
	public boolean isDuelistInPvp(boolean sendMessage)
	{
		if (_partyDuel)
		{
			// Party duels take place in arenas - should be no other players there
			return false;
		}
		else if ((_playerA.getPvpFlag() != 0) || (_playerB.getPvpFlag() != 0))
		{
			if (sendMessage)
			{
				final String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
				_playerA.sendMessage(engagedInPvP);
				_playerB.sendMessage(engagedInPvP);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Starts the duel
	 */
	public void startDuel()
	{
		if ((_playerA == null) || (_playerB == null) || _playerA.isInDuel() || _playerB.isInDuel())
		{
			_playerConditions.clear();
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		if (_partyDuel)
		{
			// Set duel state and team
			for (Player temp : _playerA.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setInDuel(_duelId);
				temp.setTeam(Team.BLUE);
				temp.broadcastUserInfo();
				broadcastToTeam2(new ExDuelUpdateUserInfo(temp));
			}
			
			for (Player temp : _playerB.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setInDuel(_duelId);
				temp.setTeam(Team.RED);
				temp.broadcastUserInfo();
				broadcastToTeam1(new ExDuelUpdateUserInfo(temp));
			}
			
			// Send duel packets
			broadcastToTeam1(ExDuelReady.PARTY_DUEL);
			broadcastToTeam2(ExDuelReady.PARTY_DUEL);
			broadcastToTeam1(ExDuelStart.PARTY_DUEL);
			broadcastToTeam2(ExDuelStart.PARTY_DUEL);
			
			// Open arena doors
			for (Door door : InstanceManager.getInstance().getInstance(getDueldInstanceId()).getDoors())
			{
				if ((door != null) && !door.isOpen())
				{
					door.openMe();
				}
			}
		}
		else
		{
			// set isInDuel() state
			_playerA.setInDuel(_duelId);
			_playerA.setTeam(Team.BLUE);
			_playerB.setInDuel(_duelId);
			_playerB.setTeam(Team.RED);
			
			// Send duel packets
			broadcastToTeam1(ExDuelReady.PLAYER_DUEL);
			broadcastToTeam2(ExDuelReady.PLAYER_DUEL);
			broadcastToTeam1(ExDuelStart.PLAYER_DUEL);
			broadcastToTeam2(ExDuelStart.PLAYER_DUEL);
			
			broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
			broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));
			_playerA.broadcastUserInfo();
			_playerB.broadcastUserInfo();
		}
		
		// play sound
		broadcastToTeam1(B04_S01);
		broadcastToTeam2(B04_S01);
		
		// start duelling task
		ThreadPool.schedule(new ScheduleDuelTask(this), 1000);
	}
	
	/**
	 * Save the current player condition: hp, mp, cp, location
	 */
	public void savePlayerConditions()
	{
		if (_partyDuel)
		{
			for (Player player : _playerA.getParty().getMembers())
			{
				_playerConditions.put(player.getObjectId(), new PlayerCondition(player, _partyDuel));
			}
			
			for (Player player : _playerB.getParty().getMembers())
			{
				_playerConditions.put(player.getObjectId(), new PlayerCondition(player, _partyDuel));
			}
		}
		else
		{
			_playerConditions.put(_playerA.getObjectId(), new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.put(_playerB.getObjectId(), new PlayerCondition(_playerB, _partyDuel));
		}
	}
	
	/**
	 * Restore player conditions
	 * @param abnormalDuelEnd true if the duel was the duel canceled
	 */
	public void restorePlayerConditions(boolean abnormalDuelEnd)
	{
		// update isInDuel() state for all players
		if (_partyDuel)
		{
			for (Player temp : _playerA.getParty().getMembers())
			{
				temp.setInDuel(0);
				temp.setTeam(Team.NONE);
				temp.broadcastUserInfo();
			}
			
			for (Player temp : _playerB.getParty().getMembers())
			{
				temp.setInDuel(0);
				temp.setTeam(Team.NONE);
				temp.broadcastUserInfo();
			}
		}
		else
		{
			_playerA.setInDuel(0);
			_playerA.setTeam(Team.NONE);
			_playerA.broadcastUserInfo();
			_playerB.setInDuel(0);
			_playerB.setTeam(Team.NONE);
			_playerB.broadcastUserInfo();
		}
		
		// if it is an abnormal DuelEnd do not restore hp, mp, cp
		if (abnormalDuelEnd)
		{
			return;
		}
		
		// restore player conditions
		_playerConditions.values().forEach(PlayerCondition::restoreCondition);
	}
	
	/**
	 * Get the duel id
	 * @return id
	 */
	public int getId()
	{
		return _duelId;
	}
	
	/**
	 * Returns the remaining time
	 * @return remaining time
	 */
	public int getRemainingTime()
	{
		return (int) (_duelEndTime.getTimeInMillis() - System.currentTimeMillis());
	}
	
	/**
	 * Get the player that requested the duel
	 * @return duel requester
	 */
	public Player getPlayerA()
	{
		return _playerA;
	}
	
	/**
	 * Get the player that was challenged
	 * @return challenged player
	 */
	public Player getPlayerB()
	{
		return _playerB;
	}
	
	/**
	 * Returns whether this is a party duel or not
	 * @return is party duel
	 */
	public boolean isPartyDuel()
	{
		return _partyDuel;
	}
	
	public void setFinished(boolean mode)
	{
		_finished = mode;
	}
	
	public boolean getFinished()
	{
		return _finished;
	}
	
	/**
	 * teleport all players to the given coordinates
	 * @param x
	 * @param y
	 * @param z
	 */
	public void teleportPlayers(int x, int y, int z)
	{
		// TODO: adjust the values if needed... or implement something better (especially using more then 1 arena)
		if (!_partyDuel)
		{
			return;
		}
		
		int offset = 0;
		for (Player temp : _playerA.getParty().getMembers())
		{
			temp.teleToLocation(new Location((x + offset) - 180, y - 150, z));
			offset += 40;
		}
		
		offset = 0;
		for (Player temp : _playerB.getParty().getMembers())
		{
			temp.teleToLocation(new Location((x + offset) - 180, y + 150, z));
			offset += 40;
		}
	}
	
	/**
	 * Broadcast a packet to the challenger team
	 * @param packet
	 */
	public void broadcastToTeam1(ServerPacket packet)
	{
		if (_playerA == null)
		{
			return;
		}
		
		if (_partyDuel && (_playerA.getParty() != null))
		{
			for (Player temp : _playerA.getParty().getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerA.sendPacket(packet);
		}
	}
	
	/**
	 * Broadcast a packet to the challenged team
	 * @param packet
	 */
	public void broadcastToTeam2(ServerPacket packet)
	{
		if (_playerB == null)
		{
			return;
		}
		
		if (_partyDuel && (_playerB.getParty() != null))
		{
			for (Player temp : _playerB.getParty().getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerB.sendPacket(packet);
		}
	}
	
	/**
	 * Get the duel winner
	 * @return winner
	 */
	public Player getWinner()
	{
		if (!_finished || (_playerA == null) || (_playerB == null))
		{
			return null;
		}
		
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		
		if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		
		return null;
	}
	
	/**
	 * Get the duel looser
	 * @return looser
	 */
	public Player getLooser()
	{
		if (!_finished || (_playerA == null) || (_playerB == null))
		{
			return null;
		}
		
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		
		return null;
	}
	
	/**
	 * Playback the bow animation for all loosers
	 */
	public void playKneelAnimation()
	{
		final Player looser = getLooser();
		if (looser == null)
		{
			return;
		}
		
		if (_partyDuel && (looser.getParty() != null))
		{
			for (Player temp : looser.getParty().getMembers())
			{
				temp.broadcastPacket(new SocialAction(temp.getObjectId(), 7));
			}
		}
		else
		{
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
		}
	}
	
	/**
	 * Do the countdown and send message to players if necessary
	 * @return current count
	 */
	public int countdown()
	{
		_countdown--;
		
		if (_countdown > 3)
		{
			return _countdown;
		}
		
		// Broadcast countdown to duelists
		SystemMessage sm = null;
		if (_countdown > 0)
		{
			sm = new SystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECOND_S);
			sm.addInt(_countdown);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);
		}
		
		broadcastToTeam1(sm);
		broadcastToTeam2(sm);
		
		return _countdown;
	}
	
	/**
	 * The duel has reached a state in which it can no longer continue
	 * @param result the duel result.
	 */
	public void endDuel(DuelResult result)
	{
		if ((_playerA == null) || (_playerB == null))
		{
			// clean up
			_playerConditions.clear();
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		// inform players of the result
		SystemMessage sm = null;
		switch (result)
		{
			case TEAM_1_WIN:
			case TEAM_2_SURRENDER:
			{
				restorePlayerConditions(false);
				sm = _partyDuel ? new SystemMessage(SystemMessageId.S1_S_PARTY_HAS_WON_THE_DUEL) : new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL);
				sm.addString(_playerA.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
			case TEAM_1_SURRENDER:
			case TEAM_2_WIN:
			{
				restorePlayerConditions(false);
				sm = _partyDuel ? new SystemMessage(SystemMessageId.S1_S_PARTY_HAS_WON_THE_DUEL) : new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL);
				sm.addString(_playerB.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
			case CANCELED:
			{
				stopFighting();
				
				// Don't restore hp, mp, cp
				restorePlayerConditions(true);
				
				// TODO: is there no other message for a canceled duel?
				// send SystemMessage
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
			case TIMEOUT:
			{
				stopFighting();
				
				// hp,mp,cp seem to be restored in a timeout too...
				restorePlayerConditions(false);
				
				// send SystemMessage
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
		}
		
		// Send end duel packet
		final ExDuelEnd duelEnd = _partyDuel ? ExDuelEnd.PARTY_DUEL : ExDuelEnd.PLAYER_DUEL;
		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
		
		// clean up
		_playerConditions.clear();
		DuelManager.getInstance().removeDuel(this);
	}
	
	/**
	 * Did a situation occur in which the duel has to be ended?
	 * @return DuelResult duel status
	 */
	public DuelResult checkEndDuelCondition()
	{
		// one of the players might leave during duel
		if ((_playerA == null) || (_playerB == null) || !_playerA.isOnline() || !_playerB.isOnline())
		{
			return DuelResult.CANCELED;
		}
		
		// got a duel surrender request?
		if (_surrenderRequest != 0)
		{
			return _surrenderRequest == 1 ? DuelResult.TEAM_1_SURRENDER : DuelResult.TEAM_2_SURRENDER;
		}
		// duel timed out
		else if (getRemainingTime() <= 0)
		{
			return DuelResult.TIMEOUT;
		}
		// Has a player been declared winner yet?
		else if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.TEAM_1_WIN;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on
			stopFighting();
			return DuelResult.TEAM_2_WIN;
		}
		
		// More end duel conditions for 1on1 duels
		else if (!_partyDuel)
		{
			// Duel was interrupted e.g.: player was attacked by mobs / other players
			if ((_playerA.getDuelState() == DUELSTATE_INTERRUPTED) || (_playerB.getDuelState() == DUELSTATE_INTERRUPTED))
			{
				return DuelResult.CANCELED;
			}
			
			// Are the players too far apart?
			if (!_playerA.isInsideRadius2D(_playerB, 1600))
			{
				return DuelResult.CANCELED;
			}
			
			// Did one of the players engage in PvP combat?
			if (isDuelistInPvp(true))
			{
				return DuelResult.CANCELED;
			}
			
			// is one of the players in a Siege, Peace or PvP zone?
			if (_playerA.isInsideZone(ZoneId.PEACE) || _playerB.isInsideZone(ZoneId.PEACE) || _playerA.isInsideZone(ZoneId.NO_PVP) || _playerB.isInsideZone(ZoneId.NO_PVP) || _playerA.isInsideZone(ZoneId.SIEGE) || _playerB.isInsideZone(ZoneId.SIEGE) || _playerA.isInsideZone(ZoneId.PVP) || _playerB.isInsideZone(ZoneId.PVP))
			{
				return DuelResult.CANCELED;
			}
		}
		
		return DuelResult.CONTINUE;
	}
	
	/**
	 * Register a surrender request
	 * @param player the player that surrenders.
	 */
	public void doSurrender(Player player)
	{
		// already received a surrender request
		if (_surrenderRequest != 0)
		{
			return;
		}
		
		// stop the fight
		stopFighting();
		
		// TODO: Can every party member cancel a party duel? or only the party leaders?
		if (_partyDuel)
		{
			if (_playerA.getParty().getMembers().contains(player))
			{
				_surrenderRequest = 1;
				for (Player temp : _playerA.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				
				for (Player temp : _playerB.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
			else if (_playerB.getParty().getMembers().contains(player))
			{
				_surrenderRequest = 2;
				for (Player temp : _playerB.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				
				for (Player temp : _playerA.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
		}
		else if (player == _playerA)
		{
			_surrenderRequest = 1;
			_playerA.setDuelState(DUELSTATE_DEAD);
			_playerB.setDuelState(DUELSTATE_WINNER);
		}
		else if (player == _playerB)
		{
			_surrenderRequest = 2;
			_playerB.setDuelState(DUELSTATE_DEAD);
			_playerA.setDuelState(DUELSTATE_WINNER);
		}
	}
	
	/**
	 * This function is called whenever a player was defeated in a duel
	 * @param player the player defeated.
	 */
	public void onPlayerDefeat(Player player)
	{
		// Set player as defeated
		player.setDuelState(DUELSTATE_DEAD);
		
		if (_partyDuel)
		{
			boolean teamdefeated = true;
			for (Player temp : player.getParty().getMembers())
			{
				if (temp.getDuelState() == DUELSTATE_DUELLING)
				{
					teamdefeated = false;
					break;
				}
			}
			
			if (teamdefeated)
			{
				final Player winner = _playerA.getParty().getMembers().contains(player) ? _playerB : _playerA;
				for (Player temp : winner.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
		}
		else
		{
			if ((player != _playerA) && (player != _playerB))
			{
				LOGGER.warning("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");
			}
			
			if (_playerA == player)
			{
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else
			{
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}
	
	/**
	 * This function is called whenever a player leaves a party
	 * @param player the player quitting.
	 */
	public void onRemoveFromParty(Player player)
	{
		// if it isn't a party duel ignore this
		if (!_partyDuel)
		{
			return;
		}
		
		// this player is leaving his party during party duel
		// if he's either playerA or playerB cancel the duel and port the players back
		if ((player == _playerA) || (player == _playerB))
		{
			for (PlayerCondition cond : _playerConditions.values())
			{
				cond.teleportBack();
				cond.getPlayer().setInDuel(0);
			}
			
			_playerA = null;
			_playerB = null;
		}
		else
		// teleport the player back & delete his PlayerCondition record
		{
			final PlayerCondition cond = _playerConditions.remove(player.getObjectId());
			if (cond != null)
			{
				cond.teleportBack();
			}
			
			player.setInDuel(0);
		}
	}
	
	public void onBuff(Player player, Skill debuff)
	{
		final PlayerCondition cond = _playerConditions.get(player.getObjectId());
		if (cond != null)
		{
			cond.registerDebuff(debuff);
		}
	}
}
