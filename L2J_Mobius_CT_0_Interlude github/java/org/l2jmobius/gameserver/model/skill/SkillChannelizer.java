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
package org.l2jmobius.gameserver.model.skill;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Skill Channelizer implementation.
 * @author UnAfraid
 */
public class SkillChannelizer implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(SkillChannelizer.class.getName());
	
	private final Creature _channelizer;
	private List<Creature> _channelized;
	
	private Skill _skill;
	private ScheduledFuture<?> _task = null;
	
	public SkillChannelizer(Creature channelizer)
	{
		_channelizer = channelizer;
	}
	
	public Creature getChannelizer()
	{
		return _channelizer;
	}
	
	public List<Creature> getChannelized()
	{
		return _channelized;
	}
	
	public boolean hasChannelized()
	{
		return _channelized != null;
	}
	
	public void startChanneling(Skill skill)
	{
		// Verify for same status.
		if (isChanneling())
		{
			LOGGER.warning("Character: " + _channelizer + " is attempting to channel skill but he already does!");
			return;
		}
		
		// Start channeling.
		_skill = skill;
		_task = ThreadPool.scheduleAtFixedRate(this, skill.getChannelingTickInitialDelay(), skill.getChannelingTickInterval());
	}
	
	public void stopChanneling()
	{
		// Verify for same status.
		if (!isChanneling())
		{
			LOGGER.warning("Character: " + _channelizer + " is attempting to stop channel skill but he does not!");
			return;
		}
		
		// Cancel the task and unset it.
		_task.cancel(true);
		_task = null;
		
		// Cancel target channelization and unset it.
		if (_channelized != null)
		{
			for (Creature creature : _channelized)
			{
				creature.getSkillChannelized().removeChannelizer(_skill.getChannelingSkillId(), _channelizer);
			}
			
			_channelized = null;
		}
		
		// unset skill.
		_skill = null;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public boolean isChanneling()
	{
		return _task != null;
	}
	
	@Override
	public void run()
	{
		if (!isChanneling())
		{
			return;
		}
		
		final Skill skill = _skill;
		if (skill == null)
		{
			return;
		}
		
		try
		{
			if (skill.getMpPerChanneling() > 0)
			{
				// Validate mana per tick.
				if (_channelizer.getCurrentMp() < skill.getMpPerChanneling())
				{
					if (_channelizer.isPlayer())
					{
						_channelizer.sendPacket(SystemMessageId.YOUR_SKILL_WAS_REMOVED_DUE_TO_A_LACK_OF_MP);
					}
					
					_channelizer.abortCast();
					return;
				}
				
				// Reduce mana per tick
				_channelizer.reduceCurrentMp(skill.getMpPerChanneling());
			}
			
			// Apply channeling skills on the targets.
			if (skill.getChannelingSkillId() > 0)
			{
				final Skill baseSkill = SkillData.getInstance().getSkill(skill.getChannelingSkillId(), 1);
				if (baseSkill == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": skill " + skill + " couldn't find effect id skill: " + skill.getChannelingSkillId() + " !");
					_channelizer.abortCast();
					return;
				}
				
				final List<Creature> targetList = new LinkedList<>();
				for (WorldObject chars : skill.getTargetList(_channelizer))
				{
					if (chars.isCreature())
					{
						targetList.add(chars.asCreature());
						chars.asCreature().getSkillChannelized().addChannelizer(skill.getChannelingSkillId(), _channelizer);
					}
				}
				
				if (targetList.isEmpty())
				{
					return;
				}
				
				_channelized = targetList;
				
				for (Creature creature : _channelized)
				{
					if (!LocationUtil.checkIfInRange(skill.getEffectRange(), _channelizer, creature, true))
					{
						continue;
					}
					else if (!GeoEngine.getInstance().canSeeTarget(_channelizer, creature))
					{
						continue;
					}
					else
					{
						final int maxSkillLevel = SkillData.getInstance().getMaxLevel(skill.getChannelingSkillId());
						final int skillLevel = Math.min(creature.getSkillChannelized().getChannerlizersSize(skill.getChannelingSkillId()), maxSkillLevel);
						if (skillLevel == 0)
						{
							continue;
						}
						
						final BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(skill.getChannelingSkillId());
						if ((info == null) || (info.getSkill().getLevel() < skillLevel))
						{
							final Skill channelingSkill = SkillData.getInstance().getSkill(skill.getChannelingSkillId(), skillLevel);
							if (channelingSkill == null)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Non existent channeling skill requested: " + skill);
								_channelizer.abortCast();
								return;
							}
							
							// Update PvP status
							if (creature.isPlayable() && _channelizer.isPlayer() && channelingSkill.hasNegativeEffect())
							{
								_channelizer.asPlayer().updatePvPStatus(creature);
							}
							
							channelingSkill.applyEffects(_channelizer, creature);
							
							// Reduce shots.
							if (skill.useSpiritShot())
							{
								_channelizer.setChargedShot(_channelizer.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
							}
							else
							{
								_channelizer.setChargedShot(ShotType.SOULSHOTS, false);
							}
							
							// Shots are re-charged every cast.
							_channelizer.rechargeShots(skill.useSoulShot(), skill.useSpiritShot());
						}
						
						_channelizer.broadcastSkillPacket(new MagicSkillLaunched(_channelizer, skill.getId(), skill.getLevel(), creature), creature);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while channelizing skill: " + skill + " channelizer: " + _channelizer + " channelized: " + _channelized + "; ", e);
		}
	}
}
