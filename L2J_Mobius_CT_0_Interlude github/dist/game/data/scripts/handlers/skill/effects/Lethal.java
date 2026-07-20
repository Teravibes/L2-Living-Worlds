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
package handlers.skill.effects;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Lethal effect implementation.
 * @author Adry_85, BazookaRpm
 */
public class Lethal extends AbstractEffect
{
	private final int _fullLethal;
	private final int _halfLethal;
	
	public Lethal(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		_fullLethal = params.getInt("fullLethal", 0);
		_halfLethal = params.getInt("halfLethal", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (effector.isPlayer() && !effector.getAccessLevel().canGiveDamage())
		{
			return;
		}
		
		// retail: lethal blocked if the skill's MLvl is too low vs. target
		if ((skill != null) && (skill.getMagicLevel() < (effected.getLevel() - 6)))
		{
			return;
		}
		
		if (!effected.isLethalable() || effected.isInvul())
		{
			return;
		}
		
		final double attr = Formulas.calcAttributeBonus(effector, effected, skill);
		final double trait = Formulas.calcGeneralTraitBonus(effector, effected, (skill != null) ? skill.getTraitType() : null, false);
		final double mult = Math.max(0d, attr * trait);
		
		double fullChance = Math.clamp(_fullLethal * mult, 0d, 100d);
		double halfChance = Math.clamp(_halfLethal * mult, 0d, 100d);
		
		final double roll = (Rnd.get(1000000) / 10000.0);
		
		final double thFull = fullChance;
		final double thHalfSum = Math.clamp(fullChance + halfChance, 0d, 100d);
		
		// Lethal Strike
		if (roll < thFull)
		{
			// for Players CP and HP is set to 1.
			if (effected.isPlayer())
			{
				effected.notifyDamageReceived(effected.getCurrentHp() - 1, effector, skill, true, false);
				effected.setCurrentCp(1);
				effected.setCurrentHp(1);
				effected.sendPacket(SystemMessageId.LETHAL_STRIKE);
			}
			
			// for Monsters HP is set to 1.
			else if (effected.isMonster() || effected.isSummon())
			{
				effected.notifyDamageReceived(effected.getCurrentHp() - 1, effector, skill, true, false);
				effected.setCurrentHp(1);
			}
			effector.sendPacket(SystemMessageId.YOUR_LETHAL_STRIKE_WAS_SUCCESSFUL);
			return;
		}
		
		// Half-Kill
		if (roll < thHalfSum)
		{
			// for Players CP is set to 1.
			if (effected.isPlayer())
			{
				effected.setCurrentCp(1);
				effected.sendMessage("Half-Kill!");
				effected.sendMessage("Your CP was drained because you were hit with a Half-Kill skill.");
			}
			
			// for Monsters HP is set to 50%.
			else if (effected.isMonster() || effected.isSummon())
			{
				final double newHp = effected.getCurrentHp() * 0.5;
				effected.notifyDamageReceived(effected.getCurrentHp() - newHp, effector, skill, true, false);
				effected.setCurrentHp(newHp);
			}
			if (effector.isPlayer())
			{
				effector.sendMessage("Half-Kill!");
			}
		}
	}
}