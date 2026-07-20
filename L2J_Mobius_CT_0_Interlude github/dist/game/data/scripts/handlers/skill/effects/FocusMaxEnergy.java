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
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Focus Max Energy effect implementation.
 * @author Adry_85
 */
public class FocusMaxEnergy extends AbstractEffect
{
	public FocusMaxEnergy(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			final Skill sonicMastery = effected.getSkills().get(992);
			final Skill focusMastery = effected.getSkills().get(993);
			final int maxCharge = (sonicMastery != null) ? sonicMastery.getLevel() : (focusMastery != null) ? focusMastery.getLevel() : 0;
			if (maxCharge != 0)
			{
				final Player player = effected.asPlayer();
				final int count = maxCharge - player.getCharges();
				player.increaseCharges(count, maxCharge);
			}
		}
	}
}
