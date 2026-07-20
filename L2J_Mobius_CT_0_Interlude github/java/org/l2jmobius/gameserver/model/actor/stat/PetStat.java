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

import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PetStat extends SummonStat
{
	public PetStat(Pet activeChar)
	{
		super(activeChar);
	}
	
	public boolean addExp(int value)
	{
		final Pet pet = getActiveChar();
		if (pet.isUncontrollable() || !super.addExp(value))
		{
			return false;
		}
		
		pet.updateAndBroadcastStatus(1);
		
		// The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
		pet.updateEffectIcons(true);
		
		return true;
	}
	
	public boolean addExpAndSp(double addToExp, double addToSp)
	{
		final long finalExp = Math.round(addToExp);
		final Pet pet = getActiveChar();
		if (pet.isUncontrollable() || !addExp(finalExp))
		{
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_GAINED_S1_EXPERIENCE_POINTS);
		sm.addLong(finalExp);
		pet.updateAndBroadcastStatus(1);
		pet.sendPacket(sm);
		return true;
	}
	
	@Override
	public boolean addLevel(byte value)
	{
		if ((getLevel() + value) > (getMaxLevel() - 1))
		{
			return false;
		}
		
		final boolean levelIncreased = super.addLevel(value);
		
		// Sync up exp with current level
		// if (getExp() > getExpForLevel(getLevel() + 1) || getExp() < getExpForLevel(getLevel())) setExp(Experience.LEVEL[getLevel()]);
		
		final Pet pet = getActiveChar();
		final StatusUpdate su = new StatusUpdate(pet);
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		pet.broadcastPacket(su);
		if (levelIncreased)
		{
			pet.broadcastPacket(new SocialAction(pet.getObjectId(), SocialAction.LEVEL_UP));
		}
		
		// Send a Server->Client packet PetInfo to the Player
		pet.updateAndBroadcastStatus(1);
		
		if (pet.getControlItem() != null)
		{
			pet.getControlItem().setEnchantLevel(getLevel());
		}
		
		return levelIncreased;
	}
	
	@Override
	public long getExpForLevel(int level)
	{
		final Pet pet = getActiveChar();
		try
		{
			return PetDataTable.getInstance().getPetLevelData(pet.getId(), level).getPetMaxExp();
		}
		catch (NullPointerException e)
		{
			if (pet != null)
			{
				LOGGER.warning("Pet objectId:" + pet.getObjectId() + ", NpcId:" + pet.getId() + ", level:" + level + " is missing data from pets_stats table!");
			}
			
			throw e;
		}
	}
	
	@Override
	public Pet getActiveChar()
	{
		return super.getActiveChar().asPet();
	}
	
	public int getFeedBattle()
	{
		return getActiveChar().getPetLevelData().getPetFeedBattle();
	}
	
	public int getFeedNormal()
	{
		return getActiveChar().getPetLevelData().getPetFeedNormal();
	}
	
	@Override
	public void setLevel(byte value)
	{
		final Pet pet = getActiveChar();
		pet.setPetData(PetDataTable.getInstance().getPetLevelData(pet.getTemplate().getId(), value));
		if (pet.getPetLevelData() == null)
		{
			throw new IllegalArgumentException("No pet data for npc: " + pet.getTemplate().getId() + " level: " + value);
		}
		
		pet.stopFeed();
		super.setLevel(value);
		
		pet.startFeed();
		
		if (pet.getControlItem() != null)
		{
			pet.getControlItem().setEnchantLevel(getLevel());
		}
	}
	
	public int getMaxFeed()
	{
		return getActiveChar().getPetLevelData().getPetMaxFeed();
	}
	
	@Override
	public int getMaxHp()
	{
		return (int) calcStat(Stat.MAX_HP, getActiveChar().getPetLevelData().getPetMaxHP(), null, null);
	}
	
	@Override
	public int getMaxMp()
	{
		return (int) calcStat(Stat.MAX_MP, getActiveChar().getPetLevelData().getPetMaxMP(), null, null);
	}
	
	@Override
	public double getMAtk(Creature target, Skill skill)
	{
		return calcStat(Stat.MAGIC_ATTACK, getActiveChar().getPetLevelData().getPetMAtk(), target, skill);
	}
	
	@Override
	public double getMDef(Creature target, Skill skill)
	{
		return calcStat(Stat.MAGIC_DEFENCE, getActiveChar().getPetLevelData().getPetMDef(), target, skill);
	}
	
	@Override
	public double getPAtk(Creature target)
	{
		return calcStat(Stat.POWER_ATTACK, getActiveChar().getPetLevelData().getPetPAtk(), target, null);
	}
	
	@Override
	public double getPDef(Creature target)
	{
		if (target != null)
		{
			if (target.isPlayable())
			{
				return ClassBalanceConfig.PVP_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[getActiveChar().asPlayer().getPlayerClass().getId()] * calcStat(Stat.POWER_DEFENCE, getActiveChar().getPetLevelData().getPetPDef(), target, null);
			}
			
			return ClassBalanceConfig.PVE_PHYSICAL_ATTACK_DEFENCE_MULTIPLIERS[getActiveChar().asPlayer().getPlayerClass().getId()] * calcStat(Stat.POWER_DEFENCE, getActiveChar().getPetLevelData().getPetPDef(), target, null);
		}
		
		return calcStat(Stat.POWER_DEFENCE, getActiveChar().getPetLevelData().getPetPDef(), target, null);
	}
	
	@Override
	public double getPAtkSpd()
	{
		return getActiveChar().isHungry() ? super.getPAtkSpd() / 2 : super.getPAtkSpd();
	}
	
	@Override
	public int getMAtkSpd()
	{
		return getActiveChar().isHungry() ? super.getMAtkSpd() / 2 : super.getMAtkSpd();
	}
	
	@Override
	public int getMaxLevel()
	{
		return ExperienceData.getInstance().getMaxPetLevel();
	}
}
