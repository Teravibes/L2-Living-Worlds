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
package org.l2jmobius.gameserver.model.actor.tasks.cubics;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.Cubic;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Cubic heal task.
 * @author Zoey76
 */
public class CubicHeal implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(CubicHeal.class.getName());
	private final Cubic _cubic;
	
	public CubicHeal(Cubic cubic)
	{
		_cubic = cubic;
	}
	
	@Override
	public void run()
	{
		if (_cubic == null)
		{
			return;
		}
		
		if (_cubic.getOwner().isDead() || !_cubic.getOwner().isOnline())
		{
			_cubic.stopAction();
			_cubic.getOwner().getCubics().remove(_cubic.getId());
			_cubic.getOwner().broadcastUserInfo();
			_cubic.cancelDisappear();
			return;
		}
		
		try
		{
			Skill skill = null;
			for (Skill s : _cubic.getSkills())
			{
				if (s.getId() == Cubic.SKILL_CUBIC_HEAL)
				{
					skill = s;
					break;
				}
			}
			
			if (skill == null)
			{
				return;
			}
			
			_cubic.cubicTargetForHeal();
			final Creature target = _cubic.getTarget();
			if ((target != null) && !target.isDead() && ((target.getMaxHp() - target.getCurrentHp()) > skill.getPower()))
			{
				skill.activateSkill(_cubic, Collections.singletonList(target));
				_cubic.getOwner().broadcastPacket(new MagicSkillUse(_cubic.getOwner(), target, skill.getId(), skill.getLevel(), 0, 0));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
	}
}
