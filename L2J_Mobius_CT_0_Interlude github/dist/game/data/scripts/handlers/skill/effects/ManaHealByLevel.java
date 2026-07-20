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
package handlers.skill.effects;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Mana Heal By Level effect implementation.
 * @author UnAfraid
 */
public class ManaHealByLevel extends AbstractEffect
{
	private final double _power;
	
	public ManaHealByLevel(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MANAHEAL_BY_LEVEL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effected == null) || effected.isDead() || effected.isDoor() || effected.isInvul())
		{
			return;
		}
		
		double amount = _power;
		
		// Recharged MP influenced by difference between target level and skill level.
		// If target is within 5 levels or lower then skill level there's no penalty.
		amount = effected.calcStat(Stat.MANA_CHARGE, amount, null, null);
		if (effected.getLevel() > skill.getMagicLevel())
		{
			final int levelDiff = effected.getLevel() - skill.getMagicLevel();
			
			// if target is too high compared to skill level, the amount of recharged mp gradually decreases.
			if (levelDiff == 6)
			{
				amount *= 0.9; // only 90% effective
			}
			else if (levelDiff == 7)
			{
				amount *= 0.8; // 80%
			}
			else if (levelDiff == 8)
			{
				amount *= 0.7; // 70%
			}
			else if (levelDiff == 9)
			{
				amount *= 0.6; // 60%
			}
			else if (levelDiff == 10)
			{
				amount *= 0.5; // 50%
			}
			else if (levelDiff == 11)
			{
				amount *= 0.4; // 40%
			}
			else if (levelDiff == 12)
			{
				amount *= 0.3; // 30%
			}
			else if (levelDiff == 13)
			{
				amount *= 0.2; // 20%
			}
			else if (levelDiff == 14)
			{
				amount *= 0.1; // 10%
			}
			else if (levelDiff >= 15)
			{
				amount = 0; // 0mp recharged
			}
		}
		
		// Prevents overheal and negative amount.
		amount = Math.max(Math.min(amount, effected.getMaxRecoverableMp() - effected.getCurrentMp()), 0);
		if (amount != 0)
		{
			effected.setCurrentMp(amount + effected.getCurrentMp());
		}
		
		final SystemMessage sm = new SystemMessage(effector.getObjectId() != effected.getObjectId() ? SystemMessageId.S2_MP_HAS_BEEN_RESTORED_BY_S1 : SystemMessageId.S1_MP_HAS_BEEN_RESTORED);
		if (effector.getObjectId() != effected.getObjectId())
		{
			sm.addString(effector.getName());
		}
		
		sm.addInt((int) amount);
		effected.sendPacket(sm);
	}
}
