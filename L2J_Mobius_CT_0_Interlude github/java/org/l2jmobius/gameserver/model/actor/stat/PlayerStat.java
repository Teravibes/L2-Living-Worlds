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
package org.l2jmobius.gameserver.model.actor.stat;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.status.PlayerStatus;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.stats.MoveType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PlayerStat extends PlayableStat
{
	private int _oldMaxHp; // stats watch
	private int _oldMaxMp; // stats watch
	private int _oldMaxCp; // stats watch
	private float _vitalityPoints = 1;
	private byte _vitalityLevel = 0;
	/** Player's maximum cubic count. */
	private int _maxCubicCount = 1;
	private boolean _cloakSlot = false;
	
	public static final int[] VITALITY_LEVELS =
	{
		240,
		2000,
		13000,
		17000,
		20000
	};
	
	public static final int MAX_VITALITY_POINTS = VITALITY_LEVELS[4];
	public static final int MIN_VITALITY_POINTS = 1;
	
	public PlayerStat(Player player)
	{
		super(player);
	}
	
	@Override
	public boolean addExp(long value)
	{
		final Player player = getActiveChar();
		
		// Allowed to gain exp?
		if (!getActiveChar().getAccessLevel().canGainExp())
		{
			return false;
		}
		
		if (!super.addExp(value))
		{
			return false;
		}
		
		// Set new karma
		if (!player.isCursedWeaponEquipped() && (player.getKarma() > 0) && !player.isInsideZone(ZoneId.PVP))
		{
			final int karmaLost = Formulas.calculateKarmaLost(player, value);
			if (karmaLost > 0)
			{
				player.setKarma(player.getKarma() - karmaLost);
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO_S1);
				msg.addInt(player.getKarma());
				player.sendPacket(msg);
			}
		}
		
		// EXP status update currently not used in retail
		player.updateUserInfo();
		return true;
	}
	
	public boolean addExpAndSp(double addToExpValue, double addToSpValue, boolean useBonuses)
	{
		final Player player = getActiveChar();
		
		// Allowed to gain exp/sp?
		if (!player.getAccessLevel().canGainExp())
		{
			return false;
		}
		
		double addToExp = addToExpValue;
		double addToSp = addToSpValue;
		
		double bonusExp = 1;
		double bonusSp = 1;
		if (useBonuses)
		{
			bonusExp = getExpBonusMultiplier();
			bonusSp = getSpBonusMultiplier();
		}
		
		addToExp *= bonusExp;
		addToSp *= bonusSp;
		double ratioTakenByPlayer = 0;
		
		// if this player has a pet and it is in his range he takes from the owner's Exp, give the pet Exp now
		if (player.hasPet() && (player.calculateDistance3D(player.getSummon()) < PlayerConfig.ALT_PARTY_RANGE))
		{
			final Pet pet = player.getSummon().asPet();
			ratioTakenByPlayer = pet.getPetLevelData().getOwnerExpTaken() / 100f;
			
			// only give exp/sp to the pet by taking from the owner if the pet has a non-zero, positive ratio
			// allow possible customizations that would have the pet earning more than 100% of the owner's exp/sp
			if (ratioTakenByPlayer > 1)
			{
				ratioTakenByPlayer = 1;
			}
			
			if (!pet.isDead())
			{
				pet.addExpAndSp(addToExp * (1 - ratioTakenByPlayer), addToSp * (1 - ratioTakenByPlayer));
			}
			
			// now adjust the max ratio to avoid the owner earning negative exp/sp
			addToExp *= ratioTakenByPlayer;
			addToSp *= ratioTakenByPlayer;
		}
		
		final long finalExp = Math.round(addToExp);
		final long finalSp = Math.round(addToSp);
		final boolean expAdded = addExp(finalExp);
		final boolean spAdded = addSp(finalSp);
		if (!expAdded && spAdded)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_SP);
			sm.addLong(finalSp);
			player.sendPacket(sm);
		}
		else if (expAdded && !spAdded)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_EXPERIENCE);
			sm.addLong(finalExp);
			player.sendPacket(sm);
		}
		else if ((finalExp > 0) || (finalSp > 0))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1_EXPERIENCE_AND_S2_SP);
			sm.addLong((long) addToExp);
			sm.addLong((long) addToSp);
			player.sendPacket(sm);
		}
		
		return true;
	}
	
	@Override
	public boolean removeExpAndSp(long addToExp, long addToSp)
	{
		return removeExpAndSp(addToExp, addToSp, true);
	}
	
	public boolean removeExpAndSp(long addToExp, long addToSp, boolean sendMessage)
	{
		final int level = getLevel();
		if (!super.removeExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		if (sendMessage)
		{
			// Send a Server->Client System Message to the Player
			SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_EXPERIENCE_HAS_DECREASED_BY_S1);
			sm.addLong(addToExp);
			final Player player = getActiveChar();
			player.sendPacket(sm);
			sm = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
			sm.addLong(addToSp);
			player.sendPacket(sm);
			if (getLevel() < level)
			{
				player.broadcastStatusUpdate();
			}
		}
		
		return true;
	}
	
	@Override
	public boolean addLevel(byte value)
	{
		if ((getLevel() + value) > (ExperienceData.getInstance().getMaxLevel() - 1))
		{
			return false;
		}
		
		// Notify to scripts
		final Player player = getActiveChar();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LEVEL_CHANGED, player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLevelChanged(player, getLevel(), getLevel() + value), player);
		}
		
		final boolean levelIncreased = super.addLevel(value);
		if (levelIncreased)
		{
			if (!PlayerConfig.DISABLE_TUTORIAL)
			{
				final QuestState qs = player.getQuestState("Q00255_Tutorial");
				if (qs != null)
				{
					qs.getQuest().notifyEvent("CE40", null, player);
				}
			}
			
			player.setCurrentCp(getMaxCp());
			player.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.LEVEL_UP));
			player.sendPacket(SystemMessageId.YOUR_LEVEL_HAS_INCREASED);
		}
		
		// Give AutoGet skills and all normal skills if Auto-Learn is activated.
		player.rewardSkills();
		
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.updateClanMember(player);
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(player));
		}
		
		if (player.isInParty())
		{
			player.getParty().recalculatePartyLevel(); // Recalculate the party level
		}
		
		// Synchronize level with pet if possible.
		if (player.hasPet())
		{
			final Pet pet = player.getSummon().asPet();
			if (pet.getPetData().isSynchLevel() && (pet.getLevel() != getLevel()))
			{
				final byte availableLevel = (byte) Math.min(pet.getPetData().getMaxLevel(), getLevel());
				pet.getStat().setLevel(availableLevel);
				pet.getStat().getExpForLevel(availableLevel);
				pet.setCurrentHp(pet.getMaxHp());
				pet.setCurrentMp(pet.getMaxMp());
				pet.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.LEVEL_UP));
				pet.updateAndBroadcastStatus(1);
			}
		}
		
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		player.sendPacket(su);
		
		// Update the overloaded status of the Player
		player.refreshOverloaded();
		
		// Update the expertise status of the Player
		player.refreshExpertisePenalty();
		
		// Send a Server->Client packet UserInfo to the Player
		player.updateUserInfo();
		return levelIncreased;
	}
	
	@Override
	public boolean addSp(long value)
	{
		if (!super.addSp(value))
		{
			return false;
		}
		
		final Player player = getActiveChar();
		final StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.SP, (int) getSp());
		player.sendPacket(su);
		
		return true;
	}
	
	@Override
	public long getExpForLevel(int level)
	{
		return ExperienceData.getInstance().getExpForLevel(level);
	}
	
	@Override
	public Player getActiveChar()
	{
		return super.getActiveChar().asPlayer();
	}
	
	@Override
	public long getExp()
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			return player.getSubClasses().get(player.getClassIndex()).getExp();
		}
		
		return super.getExp();
	}
	
	public long getBaseExp()
	{
		return super.getExp();
	}
	
	@Override
	public void setExp(long value)
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setExp(value);
		}
		else
		{
			super.setExp(value);
		}
	}
	
	/**
	 * Gets the maximum cubic count.
	 * @return the maximum cubic count
	 */
	public int getMaxCubicCount()
	{
		return _maxCubicCount;
	}
	
	/**
	 * Sets the maximum cubic count.
	 * @param cubicCount the maximum cubic count
	 */
	public void setMaxCubicCount(int cubicCount)
	{
		_maxCubicCount = cubicCount;
	}
	
	public boolean canEquipCloak()
	{
		if (!getActiveChar().hasEnteredWorld())
		{
			return true;
		}
		
		return _cloakSlot;
	}
	
	public void setCloakSlotStatus(boolean cloakSlot)
	{
		_cloakSlot = cloakSlot;
	}
	
	@Override
	public byte getLevel()
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			final SubClassHolder holder = player.getSubClasses().get(player.getClassIndex());
			if (holder != null)
			{
				return holder.getLevel();
			}
		}
		
		return super.getLevel();
	}
	
	public byte getBaseLevel()
	{
		return super.getLevel();
	}
	
	@Override
	public void setLevel(byte value)
	{
		byte level = value;
		if (level > (ExperienceData.getInstance().getMaxLevel() - 1))
		{
			level = (byte) (ExperienceData.getInstance().getMaxLevel() - 1);
		}
		
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setLevel(value);
		}
		else
		{
			super.setLevel(level);
		}
	}
	
	@Override
	public int getMaxCp()
	{
		// Get the Max CP (base+modifier) of the Player
		final Player player = getActiveChar();
		final int val = (player == null) ? 1 : (int) calcStat(Stat.MAX_CP, player.getTemplate().getBaseCpMax(player.getLevel()));
		if (val != _oldMaxCp)
		{
			_oldMaxCp = val;
			
			// Launch a regen task if the new Max CP is higher than the old one
			if (player != null)
			{
				final PlayerStatus status = player.getStatus();
				if (status.getCurrentCp() != val)
				{
					status.setCurrentCp(status.getCurrentCp()); // trigger start of regeneration
				}
			}
		}
		
		return val;
	}
	
	@Override
	public int getMaxHp()
	{
		// Get the Max HP (base+modifier) of the Player
		final Player player = getActiveChar();
		final int val = (player == null) ? 1 : (int) calcStat(Stat.MAX_HP, player.getTemplate().getBaseHpMax(player.getLevel()));
		if (val != _oldMaxHp)
		{
			_oldMaxHp = val;
			
			// Launch a regen task if the new Max HP is higher than the old one
			if (player != null)
			{
				final PlayerStatus status = player.getStatus();
				if (status.getCurrentHp() != val)
				{
					status.setCurrentHp(status.getCurrentHp()); // trigger start of regeneration
				}
			}
		}
		
		return val;
	}
	
	@Override
	public int getMaxMp()
	{
		// Get the Max MP (base+modifier) of the Player
		final Player player = getActiveChar();
		final int val = (player == null) ? 1 : (int) calcStat(Stat.MAX_MP, player.getTemplate().getBaseMpMax(player.getLevel()));
		if (val != _oldMaxMp)
		{
			_oldMaxMp = val;
			
			// Launch a regen task if the new Max MP is higher than the old one
			if (player != null)
			{
				final PlayerStatus status = player.getStatus();
				if (status.getCurrentMp() != val)
				{
					status.setCurrentMp(status.getCurrentMp()); // trigger start of regeneration
				}
			}
		}
		
		return val;
	}
	
	@Override
	public long getSp()
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			return player.getSubClasses().get(player.getClassIndex()).getSp();
		}
		
		return super.getSp();
	}
	
	public long getBaseSp()
	{
		return super.getSp();
	}
	
	@Override
	public void setSp(long value)
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setSp(value);
		}
		else
		{
			super.setSp(value);
		}
	}
	
	/**
	 * @param type movement type
	 * @return the base move speed of given movement type.
	 */
	@Override
	public double getBaseMoveSpeed(MoveType type)
	{
		final Player player = getActiveChar();
		if (player.isMounted())
		{
			final PetLevelData data = PetDataTable.getInstance().getPetLevelData(player.getMountNpcId(), player.getMountLevel());
			if (data != null)
			{
				return data.getSpeedOnRide(type);
			}
		}
		
		return super.getBaseMoveSpeed(type);
	}
	
	@Override
	public double getRunSpeed()
	{
		double val = super.getRunSpeed() + PlayerConfig.RUN_SPD_BOOST;
		
		// Apply max run speed cap.
		final Player player = getActiveChar();
		if ((val > PlayerConfig.MAX_RUN_SPEED) && !player.isGM())
		{
			return PlayerConfig.MAX_RUN_SPEED;
		}
		
		// Check for mount penalties
		if (player.isMounted())
		{
			// if level diff with mount >= 10, it decreases move speed by 50%
			if ((player.getMountLevel() - player.getLevel()) >= 10)
			{
				val /= 2;
			}
			
			// if mount is hungry, it decreases move speed by 50%
			if (player.isHungry())
			{
				val /= 2;
			}
		}
		
		return val;
	}
	
	@Override
	public double getWalkSpeed()
	{
		double val = super.getWalkSpeed() + PlayerConfig.RUN_SPD_BOOST;
		
		// Apply max run speed cap.
		final Player player = getActiveChar();
		if ((val > PlayerConfig.MAX_RUN_SPEED) && !player.isGM())
		{
			return PlayerConfig.MAX_RUN_SPEED;
		}
		
		if (player.isMounted())
		{
			// if level diff with mount >= 10, it decreases move speed by 50%
			if ((player.getMountLevel() - player.getLevel()) >= 10)
			{
				val /= 2;
			}
			
			// if mount is hungry, it decreases move speed by 50%
			if (player.isHungry())
			{
				val /= 2;
			}
		}
		
		return val;
	}
	
	@Override
	public double getPAtkSpd()
	{
		final double val = super.getPAtkSpd();
		if ((val > PlayerConfig.MAX_PATK_SPEED) && !getActiveChar().isGM())
		{
			return PlayerConfig.MAX_PATK_SPEED;
		}
		
		return val;
	}
	
	private void updateVitalityLevel(boolean quiet)
	{
		final byte level;
		if (_vitalityPoints <= VITALITY_LEVELS[0])
		{
			level = 0;
		}
		else if (_vitalityPoints <= VITALITY_LEVELS[1])
		{
			level = 1;
		}
		else if (_vitalityPoints <= VITALITY_LEVELS[2])
		{
			level = 2;
		}
		else if (_vitalityPoints <= VITALITY_LEVELS[3])
		{
			level = 3;
		}
		else
		{
			level = 4;
		}
		
		if (!quiet && (level != _vitalityLevel))
		{
			final Player player = getActiveChar();
			if (level < _vitalityLevel)
			{
				player.sendMessage("Your Vitality has decreased.");
			}
			else
			{
				player.sendMessage("Your Vitality has increased.");
			}
			
			if (level == 0)
			{
				player.sendMessage("Your Vitality is fully exhausted.");
			}
			else if (level == 4)
			{
				player.sendMessage("Your Vitality is at maximum.");
			}
		}
		
		_vitalityLevel = level;
	}
	
	/*
	 * Return current vitality points in integer format
	 */
	public int getVitalityPoints()
	{
		return (int) _vitalityPoints;
	}
	
	/*
	 * Set current vitality points to this value if quiet = true - does not send system messages
	 */
	public void setVitalityPoints(int value, boolean quiet)
	{
		final int points = Math.min(Math.max(value, MIN_VITALITY_POINTS), MAX_VITALITY_POINTS);
		if (points == _vitalityPoints)
		{
			return;
		}
		
		_vitalityPoints = points;
		updateVitalityLevel(quiet);
		// getActiveChar().sendPacket(new ExVitalityPointInfo(getVitalityPoints()));
	}
	
	public synchronized void updateVitalityPoints(float value, boolean useRates, boolean quiet)
	{
		if ((value == 0) || !PlayerConfig.ENABLE_VITALITY)
		{
			return;
		}
		
		float points = value;
		if (useRates)
		{
			final Player player = getActiveChar();
			if (player.isLucky())
			{
				return;
			}
			
			if (points < 0) // vitality consumed
			{
				final int stat = (int) calcStat(Stat.VITALITY_CONSUME_RATE, 1, player, null);
				if (stat == 0)
				{
					return;
				}
				
				if (stat < 0)
				{
					points = -points;
				}
			}
			
			if (points > 0)
			{
				// vitality increased
				points *= RatesConfig.RATE_VITALITY_GAIN;
			}
			else
			{
				// vitality decreased
				points *= RatesConfig.RATE_VITALITY_LOST;
			}
		}
		
		if (points > 0)
		{
			points = Math.min(_vitalityPoints + points, MAX_VITALITY_POINTS);
		}
		else
		{
			points = Math.max(_vitalityPoints + points, MIN_VITALITY_POINTS);
		}
		
		if (Math.abs(points - _vitalityPoints) <= 1e-6)
		{
			return;
		}
		
		_vitalityPoints = points;
		updateVitalityLevel(quiet);
	}
	
	public double getVitalityMultiplier()
	{
		double vitality = 1.0;
		if (PlayerConfig.ENABLE_VITALITY)
		{
			switch (getVitalityLevel())
			{
				case 1:
				{
					vitality = RatesConfig.RATE_VITALITY_LEVEL_1;
					break;
				}
				case 2:
				{
					vitality = RatesConfig.RATE_VITALITY_LEVEL_2;
					break;
				}
				case 3:
				{
					vitality = RatesConfig.RATE_VITALITY_LEVEL_3;
					break;
				}
				case 4:
				{
					vitality = RatesConfig.RATE_VITALITY_LEVEL_4;
					break;
				}
			}
		}
		
		return vitality;
	}
	
	/**
	 * @return the _vitalityLevel
	 */
	public byte getVitalityLevel()
	{
		return _vitalityLevel;
	}
	
	public double getExpBonusMultiplier()
	{
		double bonus = 1.0;
		double vitality = 1.0;
		double bonusExp = 1.0;
		
		// Bonus from Vitality System
		vitality = getVitalityMultiplier();
		
		// Bonus exp from skills
		bonusExp = 1 + (calcStat(Stat.BONUS_EXP, 0, null, null) / 100);
		if (vitality > 1.0)
		{
			bonus += (vitality - 1);
		}
		
		if (bonusExp > 1)
		{
			bonus += (bonusExp - 1);
		}
		
		// Check for abnormal bonuses
		bonus = Math.max(bonus, 1);
		if (PlayerConfig.MAX_BONUS_EXP > 0)
		{
			bonus = Math.min(bonus, PlayerConfig.MAX_BONUS_EXP);
		}
		
		return bonus;
	}
	
	public double getSpBonusMultiplier()
	{
		double bonus = 1.0;
		double vitality = 1.0;
		double bonusSp = 1.0;
		
		// Bonus from Vitality System
		vitality = getVitalityMultiplier();
		
		// Bonus sp from skills
		bonusSp = 1 + (calcStat(Stat.BONUS_SP, 0, null, null) / 100);
		if (vitality > 1.0)
		{
			bonus += (vitality - 1);
		}
		
		if (bonusSp > 1)
		{
			bonus += (bonusSp - 1);
		}
		
		// Check for abnormal bonuses
		bonus = Math.max(bonus, 1);
		if (PlayerConfig.MAX_BONUS_SP > 0)
		{
			bonus = Math.min(bonus, PlayerConfig.MAX_BONUS_SP);
		}
		
		return bonus;
	}
	
	public double getBonusDropAdenaMultiplier()
	{
		return 1 + (calcStat(Stat.BONUS_DROP_ADENA, 0, null, null) / 100);
	}
	
	public double getBonusDropAmountMultiplier()
	{
		return 1 + (calcStat(Stat.BONUS_DROP_AMOUNT, 0, null, null) / 100);
	}
	
	public double getBonusDropRateMultiplier()
	{
		return 1 + (calcStat(Stat.BONUS_DROP_RATE, 0, null, null) / 100);
	}
	
	public double getBonusSpoilRateMultiplier()
	{
		return 1 + (calcStat(Stat.BONUS_SPOIL_RATE, 0, null, null) / 100);
	}
}
