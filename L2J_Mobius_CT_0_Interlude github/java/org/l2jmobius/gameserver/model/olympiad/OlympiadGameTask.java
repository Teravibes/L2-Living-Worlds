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
package org.l2jmobius.gameserver.model.olympiad;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author ascharot
 */
class OlympiadGameTask implements Runnable
{
	protected static final Logger _log = Logger.getLogger(OlympiadGameTask.class.getName());
	public OlympiadGame _game = null;
	protected static final long BATTLE_PERIOD = OlympiadConfig.OLYMPIAD_BATTLE; // 6 mins
	
	// Buffs
	private static final SkillHolder[] FIGHTER_BUFFS =
	{
		new SkillHolder(1204, 2), // Wind Walk
		new SkillHolder(1086, 1), // Haste
	};
	private static final SkillHolder[] MAGE_BUFFS =
	{
		new SkillHolder(1204, 2), // Wind Walk
		new SkillHolder(1085, 1), // Acumen
	};
	
	private boolean _terminated = false;
	private boolean _started = false;
	
	public boolean isTerminated()
	{
		return _terminated || _game._aborted;
	}
	
	public boolean isStarted()
	{
		return _started;
	}
	
	public OlympiadGameTask(OlympiadGame game)
	{
		_game = game;
	}
	
	protected boolean checkBattleStatus()
	{
		final boolean pOneCrash = ((_game._playerOne == null) || _game._playerOneDisconnected);
		final boolean pTwoCrash = ((_game._playerTwo == null) || _game._playerTwoDisconnected);
		if (pOneCrash || pTwoCrash || _game._aborted)
		{
			return false;
		}
		
		return true;
	}
	
	protected boolean checkDefaulted()
	{
		_game._playerOne = World.getInstance().getPlayer(_game._playerOneID);
		_game._players.set(0, _game._playerOne);
		_game._playerTwo = World.getInstance().getPlayer(_game._playerTwoID);
		_game._players.set(1, _game._playerTwo);
		
		for (int i = 0; i < 2; i++)
		{
			boolean defaulted = false;
			final Player player = _game._players.get(i);
			if (player != null)
			{
				player.setOlympiadGameId(_game._stadiumID);
			}
			
			final Player otherPlayer = _game._players.get(i ^ 1);
			SystemMessage sm = null;
			
			if (player == null)
			{
				defaulted = true;
			}
			else if (player.isDead())
			{
				sm = new SystemMessage(SystemMessageId.YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_WHILE_DEAD);
				sm.addPcName(player);
				defaulted = true;
			}
			else if (player.isSubClassActive())
			{
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_CHANGED_FROM_YOUR_MAIN_CLASS_TO_A_SUBCLASS_AND_THEREFORE_ARE_REMOVED_FROM_THE_GRAND_OLYMPIAD_GAMES_WAITING_LIST);
				sm.addPcName(player);
				defaulted = true;
			}
			else if (player.isCursedWeaponEquipped())
			{
				sm = new SystemMessage(SystemMessageId.IF_YOU_POSSESS_S1_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
				sm.addItemName(player.getCursedWeaponEquippedId());
				defaulted = true;
			}
			else if ((player.getInventoryLimit() * 0.8) <= player.getInventory().getSize())
			{
				sm = new SystemMessage(SystemMessageId.YOU_CAN_T_JOIN_A_GRAND_OLYMPIAD_GAME_MATCH_WITH_THAT_MUCH_STUFF_ON_YOU_REDUCE_YOUR_WEIGHT_TO_BELOW_80_PERCENT_FULL_AND_REQUEST_TO_JOIN_AGAIN);
				sm.addPcName(player);
				defaulted = true;
			}
			else if ((DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OLYMPIAD_ID, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP))
			{
				final NpcHtmlMessage message = new NpcHtmlMessage(player.getLastHtmlActionOriginId());
				message.setFile(player, "data/html/mods/OlympiadIPRestriction.htm");
				message.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP)));
				player.sendPacket(message);
				defaulted = true;
			}
			
			if (defaulted)
			{
				if ((player != null) && (sm != null))
				{
					player.sendPacket(sm);
				}
				
				if (otherPlayer != null)
				{
					otherPlayer.sendPacket(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
				}
				
				if (i == 0)
				{
					_game._playerOneDefaulted = true;
				}
				else
				{
					_game._playerTwoDefaulted = true;
				}
			}
		}
		
		return _game._playerOneDefaulted || _game._playerTwoDefaulted;
	}
	
	@Override
	public void run()
	{
		_started = true;
		if (_game != null)
		{
			if ((_game._playerOne == null) || (_game._playerTwo == null))
			{
				return;
			}
			
			if (teleportCountdown())
			{
				runGame();
			}
			
			_game.validateWinner();
			_game.PlayersStatusBack();
			_game.cleanEffects();
			
			if (_game._gamestarted)
			{
				_game._gamestarted = false;
				try
				{
					_game.portPlayersBack();
				}
				catch (Exception e)
				{
					_log.log(Level.WARNING, "Exception on portPlayersBack(): " + e.getMessage(), e);
				}
			}
			
			if (OlympiadManager.STADIUMS[_game._stadiumID].getSpectators() != null)
			{
				for (Player spec : OlympiadManager.STADIUMS[_game._stadiumID].getSpectators())
				{
					if (spec != null)
					{
						spec.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
					}
				}
			}
			
			_game.clearPlayers();
			OlympiadManager.getInstance().removeGame(_game);
			_game = null;
			_terminated = true;
		}
	}
	
	protected void healplayer()
	{
		for (Player player : _game._players)
		{
			if (player == null)
			{
				continue;
			}
			
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
		}
	}
	
	private void applyBuffs()
	{
		for (Player player : _game._players)
		{
			if (player == null)
			{
				continue;
			}
			
			SkillHolder[] buffs = player.isMageClass() ? MAGE_BUFFS : FIGHTER_BUFFS;
			for (SkillHolder buff : buffs)
			{
				Skill skill = buff.getSkill();
				if (skill != null)
				{
					player.broadcastPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
					skill.applyEffects(player, player);
				}
			}
		}
	}
	
	private boolean runGame()
	{
		SystemMessage sm;
		
		// Checking for opponents and teleporting to arena
		if (checkDefaulted())
		{
			return false;
		}
		
		_game.portPlayersToArena();
		_game.removals();
		if (OlympiadConfig.OLYMPIAD_ANNOUNCE_GAMES)
		{
			_game.announceGame();
		}
		
		try
		{
			Thread.sleep(5000);
		}
		catch (Exception e)
		{
			// Ignore.
		}
		
		synchronized (this)
		{
			if (!OlympiadGame._battleStarted)
			{
				OlympiadGame._battleStarted = true;
			}
		}
		
		byte step = 10;
		for (byte i = 60; i > 0; i -= step)
		{
			sm = new SystemMessage(SystemMessageId.THE_GRAND_OLYMPIAD_MATCH_WILL_START_IN_S1_SECOND_S);
			sm.addInt(i);
			_game.broadcastMessage(sm, true);
			
			// Apply buffs at 0 seconds remaining.
			if (i == 0)
			{
				applyBuffs();
				healplayer();
			}
			
			switch (i)
			{
				case 10:
					_game._damageP1 = 0;
					_game._damageP2 = 0;
					step = 5;
					break;
				case 5:
					step = 1;
					break;
			}
			
			try
			{
				Thread.sleep(step * 1000);
			}
			catch (Exception e)
			{
				// Ignore.
			}
		}
		
		if (!checkBattleStatus())
		{
			return false;
		}
		
		if (!_game.makeCompetitionStart())
		{
			return false;
		}
		
		// TODO: Check if this can be removed.
		_game._playerOne.broadcastInfo();
		_game._playerTwo.broadcastInfo();
		
		_game._playerOne.sendPacket(new ExOlympiadUserInfo(_game._playerTwo, 1));
		_game._playerTwo.sendPacket(new ExOlympiadUserInfo(_game._playerOne, 1));
		
		if (OlympiadManager.STADIUMS[_game._stadiumID].getSpectators() != null)
		{
			for (Player spec : OlympiadManager.STADIUMS[_game._stadiumID].getSpectators())
			{
				if (spec != null)
				{
					spec.sendPacket(new ExOlympiadUserInfo(_game._playerOne, 1));
					spec.sendPacket(new ExOlympiadUserInfo(_game._playerTwo, 2));
				}
			}
		}
		
		// Wait 3 mins (Battle)
		for (int i = 0; i < BATTLE_PERIOD; i += 10000)
		{
			try
			{
				Thread.sleep(10000);
				
				// If game haveWinner then stop waiting battle_period
				// and validate winner
				if (_game.haveWinner())
				{
					break;
				}
			}
			catch (Exception e)
			{
				// Ignore.
			}
		}
		
		// TODO: Check if this can be removed.
		_game._playerOne.broadcastInfo();
		_game._playerTwo.broadcastInfo();
		
		return checkBattleStatus();
	}
	
	private boolean teleportCountdown()
	{
		SystemMessage sm;
		
		// Waiting for teleport to arena
		byte step = 60;
		for (int i = OlympiadConfig.OLYMPIAD_WAIT_TIME; i > 0; i -= step)
		{
			sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
			sm.addInt(i);
			_game.broadcastMessage(sm, false);
			
			switch (i)
			{
				case 60:
					step = 30;
					break;
				case 30:
					step = 15;
					break;
				case 15:
					step = 5;
					break;
				case 5:
					step = 1;
					break;
			}
			
			try
			{
				Thread.sleep(step * 1000);
			}
			catch (InterruptedException e)
			{
				return false;
			}
		}
		
		return true;
	}
}
