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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.data.HeroSkillTable;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.spawns.Spawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExOlympiadMode;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author GodKratos
 */
class OlympiadGame
{
	protected static final Logger LOGGER = Logger.getLogger(OlympiadGame.class.getName());
	protected static final Logger _logResults = Logger.getLogger("olympiad");
	protected final CompetitionType _type;
	protected boolean _aborted;
	protected boolean _gamestarted;
	protected boolean _playerOneDisconnected;
	protected boolean _playerTwoDisconnected;
	protected boolean _playerOneDefaulted;
	protected boolean _playerTwoDefaulted;
	protected String _playerOneName;
	protected String _playerTwoName;
	protected int _playerOneID = 0;
	protected int _playerTwoID = 0;
	protected int _playerOneClass = 0;
	protected int _playerTwoClass = 0;
	protected static final int OLY_MANAGER = 31688;
	private static final String POINTS = "olympiad_points";
	private static final String COMP_DONE = "competitions_done";
	private static final String COMP_WON = "competitions_won";
	private static final String COMP_LOST = "competitions_lost";
	private static final String COMP_DRAWN = "competitions_drawn";
	protected static boolean _battleStarted;
	protected static boolean _gameIsStarted;
	
	protected long _startTime = 0;
	
	public int _damageP1 = 0;
	public int _damageP2 = 0;
	
	public Player _playerOne;
	public Player _playerTwo;
	protected List<Player> _players;
	private final int[] _stadiumPort;
	private int x1, y1, z1, x2, y2, z2;
	public final int _stadiumID;
	protected SystemMessage _sm;
	private SystemMessage _sm2;
	private SystemMessage _sm3;
	
	protected OlympiadGame(int id, CompetitionType type, List<Player> list)
	{
		_aborted = false;
		_gamestarted = false;
		_stadiumID = id;
		_playerOneDisconnected = false;
		_playerTwoDisconnected = false;
		_type = type;
		_stadiumPort = OlympiadManager.STADIUMS[id].getCoordinates();
		
		if (list != null)
		{
			_players = list;
			_playerOne = list.get(0);
			_playerTwo = list.get(1);
			
			try
			{
				_playerOneName = _playerOne.getName();
				_playerTwoName = _playerTwo.getName();
				_playerOne.setOlympiadGameId(id);
				_playerTwo.setOlympiadGameId(id);
				_playerOneID = _playerOne.getObjectId();
				_playerTwoID = _playerTwo.getObjectId();
				_playerOneClass = _playerOne.getBaseClass();
				_playerTwoClass = _playerTwo.getBaseClass();
			}
			catch (Exception e)
			{
				_aborted = true;
				clearPlayers();
			}
		}
		else
		{
			_aborted = true;
			clearPlayers();
		}
	}
	
	public boolean isAborted()
	{
		return _aborted;
	}
	
	protected void clearPlayers()
	{
		_playerOne = null;
		_playerTwo = null;
		_players = null;
		_playerOneName = "";
		_playerTwoName = "";
		_playerOneID = 0;
		_playerTwoID = 0;
	}
	
	protected void handleDisconnect(Player player)
	{
		if (_gamestarted)
		{
			if (player == _playerOne)
			{
				_playerOneDisconnected = true;
			}
			else if (player == _playerTwo)
			{
				_playerTwoDisconnected = true;
			}
		}
	}
	
	public Spawn SpawnBuffer(int xPos, int yPos, int zPos, int npcId)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		try
		{
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(xPos, yPos, zPos);
			spawn.setAmount(1);
			spawn.setHeading(0);
			spawn.setRespawnDelay(1);
			SpawnTable.getInstance().addSpawn(spawn);
			spawn.init();
			spawn.stopRespawn();
			return spawn;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
			return null;
		}
	}
	
	protected void removals()
	{
		if (_aborted)
		{
			return;
		}
		
		if ((_playerOne == null) || (_playerTwo == null))
		{
			return;
		}
		
		if (_playerOneDisconnected || _playerTwoDisconnected)
		{
			return;
		}
		
		for (Player player : _players)
		{
			try
			{
				// Remove Buffs
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				
				// Remove Clan Skills
				final Clan clan = player.getClan();
				if (clan != null)
				{
					for (Skill skill : clan.getAllSkills())
					{
						player.removeSkill(skill, false, true);
					}
					
					if (clan.getCastleId() > 0)
					{
						final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
						if (castle != null)
						{
							castle.removeResidentialSkills(player);
						}
					}
				}
				
				// Abort casting if player casting
				if (player.isCastingNow())
				{
					player.abortCast();
				}
				
				// Force the character to be visible
				player.setInvisible(false);
				
				// Remove Hero Skills
				if (player.isHero())
				{
					for (Skill skill : HeroSkillTable.getHeroSkills())
					{
						player.removeSkill(skill, false);
					}
				}
				
				// Heal Player fully
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				
				// Remove Summon's Buffs
				if (player.hasSummon())
				{
					final Summon summon = player.getSummon();
					summon.stopAllEffects();
					
					if (summon.isPet())
					{
						summon.unSummon(player);
					}
				}
				
				// stop any cubic that has been given by other player.
				player.stopCubicsByOthers();
				
				// Remove player from his party
				if (player.getParty() != null)
				{
					final Party party = player.getParty();
					party.removePartyMember(player, null);
				}
				
				// Remove Agathion
				if (player.getAgathionId() > 0)
				{
					player.setAgathionId(0);
					player.broadcastUserInfo();
				}
				
				player.checkItemRestriction();
				
				// Remove shot automation
				player.disableAutoShotsAll();
				
				// Discharge any active shots
				final Item item = player.getActiveWeaponInstance();
				if (item != null)
				{
					item.unChargeAllShots();
				}
				
				// enable skills with cool time <= 15 minutes
				for (Skill skill : player.getAllSkills())
				{
					if (skill.getReuseDelay() <= 900000)
					{
						player.enableSkill(skill);
					}
				}
				
				player.sendSkillList();
				player.sendPacket(new SkillCoolTime(player));
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "", e);
			}
		}
	}
	
	protected boolean portPlayersToArena()
	{
		final boolean _playerOneCrash = ((_playerOne == null) || _playerOneDisconnected);
		final boolean _playerTwoCrash = ((_playerTwo == null) || _playerTwoDisconnected);
		
		if (_playerOneCrash || _playerTwoCrash || _aborted)
		{
			_playerOne = null;
			_playerTwo = null;
			_aborted = true;
			return false;
		}
		
		try
		{
			x1 = _playerOne.getX();
			y1 = _playerOne.getY();
			z1 = _playerOne.getZ();
			
			x2 = _playerTwo.getX();
			y2 = _playerTwo.getY();
			z2 = _playerTwo.getZ();
			
			if (_playerOne.isSitting())
			{
				_playerOne.standUp();
			}
			
			if (_playerTwo.isSitting())
			{
				_playerTwo.standUp();
			}
			
			_playerOne.setTarget(null);
			_playerTwo.setTarget(null);
			
			_gamestarted = true;
			
			_playerOne.setInstanceId(0);
			_playerOne.teleToLocation(_stadiumPort[0] + 900, _stadiumPort[1], _stadiumPort[2], false);
			_playerTwo.setInstanceId(0);
			_playerTwo.teleToLocation(_stadiumPort[0] - 900, _stadiumPort[1], _stadiumPort[2], false);
			
			_playerOne.sendPacket(new ExOlympiadMode(2));
			_playerTwo.sendPacket(new ExOlympiadMode(2));
			
			_playerOne.setInOlympiadMode(true);
			_playerOne.setOlympiadStart(false);
			_playerOne.setOlympiadSide(1);
			
			_playerTwo.setInOlympiadMode(true);
			_playerTwo.setOlympiadStart(false);
			_playerTwo.setOlympiadSide(2);
			
			_gameIsStarted = false;
		}
		catch (NullPointerException e)
		{
			LOGGER.log(Level.WARNING, "", e);
			return false;
		}
		
		return true;
	}
	
	protected void cleanEffects()
	{
		if ((_playerOne == null) || (_playerTwo == null))
		{
			return;
		}
		
		if (_playerOneDisconnected || _playerTwoDisconnected)
		{
			return;
		}
		
		for (Player player : _players)
		{
			try
			{
				player.stopAllEffectsExceptThoseThatLastThroughDeath();
				player.clearCharges();
				if (player.getAgathionId() > 0)
				{
					player.setAgathionId(0);
				}
				
				if (player.hasSummon())
				{
					final Summon summon = player.getSummon();
					summon.stopAllEffects();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "cleanEffects()", e);
			}
		}
	}
	
	protected void portPlayersBack()
	{
		if (_playerOne != null)
		{
			_playerOne.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
			_playerOne.teleToLocation(x1, y1, z1, true);
		}
		
		if (_playerTwo != null)
		{
			_playerTwo.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
			_playerTwo.teleToLocation(x2, y2, z2, true);
		}
	}
	
	protected void PlayersStatusBack()
	{
		for (Player player : _players)
		{
			if (player == null)
			{
				continue;
			}
			
			try
			{
				if (Olympiad.getInstance().playerInStadia(player))
				{
					if (player.isDead())
					{
						player.setDead(false);
					}
					
					player.getStatus().startHpMpRegeneration();
					player.setCurrentCp(player.getMaxCp());
					player.setCurrentHp(player.getMaxHp());
					player.setCurrentMp(player.getMaxMp());
				}
				
				player.setInOlympiadMode(false);
				player.setOlympiadStart(false);
				player.setOlympiadSide(-1);
				player.setOlympiadGameId(-1);
				player.sendPacket(new ExOlympiadMode(0));
				
				// Add Clan Skills
				final Clan clan = player.getClan();
				if (clan != null)
				{
					for (Skill skill : clan.getAllSkills())
					{
						if (skill.getMinPledgeClass() <= player.getPledgeClass())
						{
							player.addSkill(skill, false);
						}
					}
					
					if (clan.getCastleId() > 0)
					{
						final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
						if (castle != null)
						{
							castle.giveResidentialSkills(player);
						}
					}
				}
				
				// Add Hero Skills
				if (player.isHero())
				{
					for (Skill skill : HeroSkillTable.getHeroSkills())
					{
						player.addSkill(skill, false);
					}
				}
				
				player.sendSkillList();
				
				if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
				{
					AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, player);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "portPlayersToArena()", e);
			}
		}
	}
	
	protected boolean haveWinner()
	{
		if (_aborted || (_playerOne == null) || (_playerTwo == null) || _playerOneDisconnected || _playerTwoDisconnected)
		{
			return true;
		}
		
		double playerOneHp = 0;
		
		try
		{
			if (_playerOne.getOlympiadGameId() != -1)
			{
				playerOneHp = _playerOne.getCurrentHp();
			}
		}
		catch (Exception e)
		{
			playerOneHp = 0;
		}
		
		double playerTwoHp = 0;
		try
		{
			if (_playerTwo.getOlympiadGameId() != -1)
			{
				playerTwoHp = _playerTwo.getCurrentHp();
			}
		}
		catch (Exception e)
		{
			playerTwoHp = 0;
		}
		
		return (playerTwoHp <= 0) || (playerOneHp <= 0);
	}
	
	protected void validateWinner()
	{
		if (_aborted)
		{
			return;
		}
		
		final boolean _pOneCrash = ((_playerOne == null) || _playerOneDisconnected);
		final boolean _pTwoCrash = ((_playerTwo == null) || _playerTwoDisconnected);
		
		final int _div;
		final int _gpreward;
		
		final String classed;
		switch (_type)
		{
			case NON_CLASSED:
				_div = 5;
				_gpreward = OlympiadConfig.OLYMPIAD_NONCLASSED_RITEM_C;
				classed = "no";
				break;
			default:
				_div = 3;
				_gpreward = OlympiadConfig.OLYMPIAD_CLASSED_RITEM_C;
				classed = "yes";
				break;
		}
		
		final StatSet playerOneStat = Olympiad.getNobleStats(_playerOneID);
		final StatSet playerTwoStat = Olympiad.getNobleStats(_playerTwoID);
		
		final int playerOnePlayed = playerOneStat.getInt(COMP_DONE);
		final int playerTwoPlayed = playerTwoStat.getInt(COMP_DONE);
		final int playerOneWon = playerOneStat.getInt(COMP_WON);
		final int playerTwoWon = playerTwoStat.getInt(COMP_WON);
		final int playerOneLost = playerOneStat.getInt(COMP_LOST);
		final int playerTwoLost = playerTwoStat.getInt(COMP_LOST);
		final int playerOneDrawn = playerOneStat.getInt(COMP_DRAWN);
		final int playerTwoDrawn = playerTwoStat.getInt(COMP_DRAWN);
		
		final int playerOnePoints = playerOneStat.getInt(POINTS);
		final int playerTwoPoints = playerTwoStat.getInt(POINTS);
		final int pointDiff = Math.min(Math.min(playerOnePoints, playerTwoPoints) / _div, OlympiadConfig.OLYMPIAD_MAX_POINTS);
		
		// Check for if a player defaulted before battle started
		if (_playerOneDefaulted || _playerTwoDefaulted)
		{
			if (_playerOneDefaulted)
			{
				final int lostPoints = Math.min(playerOnePoints / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
				playerOneStat.set(POINTS, playerOnePoints - lostPoints);
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
				sm.addString(_playerOneName);
				sm.addInt(lostPoints);
				broadcastMessage(sm, false);
				
				if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
				{
					final LogRecord record = new LogRecord(Level.INFO, _playerOneName + " default");
					record.setParameters(new Object[]
					{
						_playerOneName,
						_playerTwoName,
						0,
						0,
						0,
						0,
						lostPoints,
						classed
					});
					_logResults.log(record);
				}
			}
			
			if (_playerTwoDefaulted)
			{
				final int lostPoints = Math.min(playerTwoPoints / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
				playerTwoStat.set(POINTS, playerTwoPoints - lostPoints);
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
				sm.addString(_playerTwoName);
				sm.addInt(lostPoints);
				broadcastMessage(sm, false);
				
				if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
				{
					final LogRecord record = new LogRecord(Level.INFO, _playerTwoName + " default");
					record.setParameters(new Object[]
					{
						_playerOneName,
						_playerTwoName,
						0,
						0,
						0,
						0,
						lostPoints,
						classed
					});
					_logResults.log(record);
				}
			}
			return;
		}
		
		// Create results for players if a player crashed
		if (_pOneCrash || _pTwoCrash)
		{
			if (_pOneCrash && !_pTwoCrash)
			{
				try
				{
					playerOneStat.set(POINTS, playerOnePoints - pointDiff);
					playerOneStat.set(COMP_LOST, playerOneLost + 1);
					
					if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
					{
						final LogRecord record = new LogRecord(Level.INFO, _playerOneName + " crash");
						record.setParameters(new Object[]
						{
							_playerOneName,
							_playerTwoName,
							0,
							0,
							0,
							0,
							pointDiff,
							classed
						});
						_logResults.log(record);
					}
					
					playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
					playerTwoStat.set(COMP_WON, playerTwoWon + 1);
					
					_sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_S1_YOU_WIN_THE_MATCH);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
					_sm.addString(_playerTwoName);
					broadcastMessage(_sm, true);
					_sm2.addString(_playerTwoName);
					_sm2.addInt(pointDiff);
					broadcastMessage(_sm2, false);
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerTwo, _playerOne, _type), Olympiad.getInstance());
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + e.getMessage(), e);
				}
			}
			else if (_pTwoCrash && !_pOneCrash)
			{
				try
				{
					playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
					playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
					
					if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
					{
						final LogRecord record = new LogRecord(Level.INFO, _playerTwoName + " crash");
						record.setParameters(new Object[]
						{
							_playerOneName,
							_playerTwoName,
							0,
							0,
							0,
							0,
							pointDiff,
							classed
						});
						_logResults.log(record);
					}
					
					playerOneStat.set(POINTS, playerOnePoints + pointDiff);
					playerOneStat.set(COMP_WON, playerOneWon + 1);
					
					_sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_S1_YOU_WIN_THE_MATCH);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
					_sm.addString(_playerOneName);
					broadcastMessage(_sm, true);
					_sm2.addString(_playerOneName);
					_sm2.addInt(pointDiff);
					broadcastMessage(_sm2, false);
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerOne, _playerTwo, _type), Olympiad.getInstance());
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + e.getMessage(), e);
				}
			}
			else if (_pOneCrash && _pTwoCrash)
			{
				try
				{
					playerOneStat.set(POINTS, playerOnePoints - pointDiff);
					playerOneStat.set(COMP_LOST, playerOneLost + 1);
					
					playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
					playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
					
					if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
					{
						final LogRecord record = new LogRecord(Level.INFO, "both crash");
						record.setParameters(new Object[]
						{
							_playerOneName,
							_playerTwoName,
							0,
							0,
							0,
							0,
							pointDiff,
							classed
						});
						_logResults.log(record);
					}
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, _playerOne, _type), Olympiad.getInstance());
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, _playerTwo, _type), Olympiad.getInstance());
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Exception on validateWinnder(): " + e.getMessage(), e);
				}
			}
			
			playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
			playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);
			return;
		}
		
		double playerOneHp = 0;
		if (!_playerOne.isDead())
		{
			playerOneHp = _playerOne.getCurrentHp() + _playerOne.getCurrentCp();
		}
		
		double playerTwoHp = 0;
		if (!_playerTwo.isDead())
		{
			playerTwoHp = _playerTwo.getCurrentHp() + _playerTwo.getCurrentCp();
		}
		
		_sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_S1_YOU_WIN_THE_MATCH);
		_sm2 = new SystemMessage(SystemMessageId.S1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
		_sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
		
		// if players crashed, search if they've relogged
		_playerOne = World.getInstance().getPlayer(_playerOneID);
		_players.set(0, _playerOne);
		_playerTwo = World.getInstance().getPlayer(_playerTwoID);
		_players.set(1, _playerTwo);
		
		String winner = "draw";
		
		// Calculate Fight time
		final long fightTime = (System.currentTimeMillis() - _startTime);
		
		if ((_playerOne == null) && (_playerTwo == null))
		{
			playerOneStat.set(COMP_DRAWN, playerOneDrawn + 1);
			playerTwoStat.set(COMP_DRAWN, playerTwoDrawn + 1);
			_sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
			broadcastMessage(_sm, true);
		}
		else if ((_playerTwo == null) || !_playerTwo.isOnline() || ((playerTwoHp == 0) && (playerOneHp != 0)) || ((_damageP1 > _damageP2) && (playerTwoHp != 0) && (playerOneHp != 0)))
		{
			playerOneStat.set(POINTS, playerOnePoints + pointDiff);
			playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);
			playerOneStat.set(COMP_WON, playerOneWon + 1);
			playerTwoStat.set(COMP_LOST, playerTwoLost + 1);
			
			_sm.addString(_playerOneName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerOneName);
			_sm2.addInt(pointDiff);
			broadcastMessage(_sm2, false);
			_sm3.addString(_playerTwoName);
			_sm3.addInt(pointDiff);
			broadcastMessage(_sm3, false);
			winner = _playerOneName + " won";
			
			try
			{
				// Save Fight Result
				saveResults(_playerOneID, _playerTwoID, _playerOneClass, _playerTwoClass, 1, _startTime, fightTime, (_type == CompetitionType.CLASSED ? 1 : 0));
				
				final Item item = _playerOne.getInventory().addItem(ItemProcessType.REWARD, OlympiadConfig.OLYMPIAD_BATTLE_REWARD_ITEM, _gpreward, _playerOne, null);
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerOne.sendInventoryUpdate(iu);
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
				sm.addItemName(item);
				sm.addInt(_gpreward);
				_playerOne.sendPacket(sm);
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerOne, _playerTwo, _type), Olympiad.getInstance());
				}
			}
			catch (Exception e)
			{
				// Ignore.
			}
		}
		else if ((_playerOne == null) || !_playerOne.isOnline() || ((playerOneHp == 0) && (playerTwoHp != 0)) || ((_damageP2 > _damageP1) && (playerOneHp != 0) && (playerTwoHp != 0)))
		{
			playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
			playerOneStat.set(POINTS, playerOnePoints - pointDiff);
			playerTwoStat.set(COMP_WON, playerTwoWon + 1);
			playerOneStat.set(COMP_LOST, playerOneLost + 1);
			
			_sm.addString(_playerTwoName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerTwoName);
			_sm2.addInt(pointDiff);
			broadcastMessage(_sm2, false);
			_sm3.addString(_playerOneName);
			_sm3.addInt(pointDiff);
			broadcastMessage(_sm3, false);
			winner = _playerTwoName + " won";
			
			try
			{
				// Save Fight Result
				saveResults(_playerOneID, _playerTwoID, _playerOneClass, _playerTwoClass, 2, _startTime, fightTime, (_type == CompetitionType.CLASSED ? 1 : 0));
				
				final Item item = _playerTwo.getInventory().addItem(ItemProcessType.REWARD, OlympiadConfig.OLYMPIAD_BATTLE_REWARD_ITEM, _gpreward, _playerTwo, null);
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerTwo.sendInventoryUpdate(iu);
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
				sm.addItemName(item);
				sm.addInt(_gpreward);
				_playerTwo.sendPacket(sm);
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerTwo, _playerOne, _type), Olympiad.getInstance());
				}
			}
			catch (Exception e)
			{
				// Ignore.
			}
		}
		else
		{
			// Save Fight Result
			saveResults(_playerOneID, _playerTwoID, _playerOneClass, _playerTwoClass, 0, _startTime, fightTime, (_type == CompetitionType.CLASSED ? 1 : 0));
			
			_sm = new SystemMessage(SystemMessageId.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE);
			broadcastMessage(_sm, true);
			final int pointOneDiff = Math.min(playerOnePoints / 5, OlympiadConfig.OLYMPIAD_MAX_POINTS);
			final int pointTwoDiff = Math.min(playerTwoPoints / 5, OlympiadConfig.OLYMPIAD_MAX_POINTS);
			playerOneStat.set(POINTS, playerOnePoints - pointOneDiff);
			playerTwoStat.set(POINTS, playerTwoPoints - pointTwoDiff);
			playerOneStat.set(COMP_DRAWN, playerOneDrawn + 1);
			playerTwoStat.set(COMP_DRAWN, playerTwoDrawn + 1);
			_sm2 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
			_sm2.addString(_playerOneName);
			_sm2.addInt(pointOneDiff);
			broadcastMessage(_sm2, false);
			_sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES);
			_sm3.addString(_playerTwoName);
			_sm3.addInt(pointTwoDiff);
			broadcastMessage(_sm3, false);
		}
		
		playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
		playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);
		
		if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
		{
			final LogRecord record = new LogRecord(Level.INFO, winner);
			record.setParameters(new Object[]
			{
				_playerOneName,
				_playerTwoName,
				playerOneHp,
				playerTwoHp,
				_damageP1,
				_damageP2,
				pointDiff,
				classed
			});
			_logResults.log(record);
		}
		
		byte step = 10;
		for (byte i = 40; i > 0; i -= step)
		{
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_BACK_TO_TOWN_IN_S1_SECOND_S);
			_sm.addInt(i);
			broadcastMessage(_sm, false);
			switch (i)
			{
				case 10:
				{
					step = 5;
					break;
				}
				case 5:
				{
					step = 1;
					break;
				}
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
	}
	
	protected boolean makeCompetitionStart()
	{
		_startTime = System.currentTimeMillis();
		if (_aborted)
		{
			return false;
		}
		
		_sm = new SystemMessage(SystemMessageId.THE_MATCH_HAS_STARTED_FIGHT);
		broadcastMessage(_sm, true);
		_gameIsStarted = true;
		try
		{
			for (Player player : _players)
			{
				player.setOlympiadStart(true);
				player.updateEffectIcons();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
			_aborted = true;
			return false;
		}
		
		return true;
	}
	
	protected void addDamage(Player player, int damage)
	{
		if ((_playerOne == null) || (_playerTwo == null))
		{
			return;
		}
		
		if (player == _playerOne)
		{
			if (!_playerTwo.isInvul())
			{
				_damageP1 += damage;
			}
		}
		else if (player == _playerTwo)
		{
			if (!_playerOne.isInvul())
			{
				_damageP2 += damage;
			}
		}
	}
	
	protected String getTitle()
	{
		return _playerOneName + " / " + _playerTwoName;
	}
	
	protected Player[] getPlayers()
	{
		if ((_players == null) || _players.isEmpty())
		{
			return null;
		}
		
		final Player[] players = new Player[_players.size()];
		_players.toArray(players);
		
		return players;
	}
	
	protected void broadcastMessage(SystemMessage sm, boolean toAll)
	{
		for (Player player : _players)
		{
			if (player != null)
			{
				player.sendPacket(sm);
			}
		}
		
		if (toAll && (OlympiadManager.STADIUMS[_stadiumID].getSpectators() != null))
		{
			for (Player spec : OlympiadManager.STADIUMS[_stadiumID].getSpectators())
			{
				if (spec != null)
				{
					spec.sendPacket(sm);
				}
			}
		}
	}
	
	protected void announceGame()
	{
		String gameType = null;
		switch (_type)
		{
			case NON_CLASSED:
			{
				gameType = "class-free individual match";
				break;
			}
			default:
			{
				gameType = "class-specific individual match";
				break;
			}
		}
		
		for (Spawn spawn : SpawnTable.getInstance().getSpawns(OLY_MANAGER))
		{
			final Npc manager = spawn.getLastSpawn();
			if (manager != null)
			{
				manager.broadcastPacket(new CreatureSay(manager, ChatType.NPC_SHOUT, manager.getName(), "Olympiad " + gameType + " is going to begin in Arena " + (_stadiumID + 1) + " in a moment."));
			}
		}
	}
	
	protected void saveResults(int playerOne, int playerTwo, int playerOneClass, int playerTwoClass, int winner, long startTime, long fightTime, int classed)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, winner, start, time, classed) values(?,?,?,?,?,?,?,?)"))
		{
			statement.setInt(1, playerOne);
			statement.setInt(2, playerTwo);
			statement.setInt(3, playerOneClass);
			statement.setInt(4, playerTwoClass);
			statement.setInt(5, winner);
			statement.setLong(6, startTime);
			statement.setLong(7, fightTime);
			statement.setInt(8, classed);
			statement.execute();
		}
		catch (SQLException e)
		{
			if (LOGGER.isLoggable(Level.SEVERE))
			{
				LOGGER.log(Level.SEVERE, "SQL exception while saving olympiad fight.", e);
			}
		}
	}
}
