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
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Magical damage MP effect.
 * @author Adry_85
 */
public class MagicalDamageMp extends AbstractEffect
{
	public MagicalDamageMp(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isInvul())
		{
			return false;
		}
		
		if (!Formulas.calcMagicAffected(effector, effected, skill))
		{
			if (effector.isPlayer())
			{
				effector.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
			}
			
			if (effected.isPlayer())
			{
				effected.sendMessage(effected.getName() + " resisted " + effector.getName() + "'s magic.");
			}
			
			return false;
		}
		
		return true;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		final boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final byte shld = Formulas.calcShldUse(effector, effected, skill);
		final boolean mcrit = Formulas.calcMCrit(effector.getMCriticalHit(effected, skill));
		final double damage = Formulas.calcManaDam(effector, effected, skill, shld, sps, bss, mcrit);
		final double mp = (damage > effected.getCurrentMp() ? effected.getCurrentMp() : damage);
		if (damage > 0)
		{
			effected.stopEffectsOnDamage(true);
			effected.setCurrentMp(effected.getCurrentMp() - mp);
		}
		
		if (effected.isPlayer())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S_MP_HAS_BEEN_DRAINED_BY_S1);
			sm.addString(effector.getName());
			sm.addInt((int) mp);
			effected.sendPacket(sm);
		}
		
		if (effector.isPlayer())
		{
			final SystemMessage sm2 = new SystemMessage(SystemMessageId.YOUR_OPPONENT_S_MP_WAS_REDUCED_BY_S1);
			sm2.addInt((int) mp);
			effector.sendPacket(sm2);
		}
	}
}
