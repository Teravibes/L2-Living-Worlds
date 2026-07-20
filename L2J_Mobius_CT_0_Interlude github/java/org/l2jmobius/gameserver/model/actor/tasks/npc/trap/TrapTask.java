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
package org.l2jmobius.gameserver.model.actor.tasks.npc.trap;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.Trap;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

/**
 * Trap task.
 * @author Zoey76
 */
public class TrapTask implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(TrapTask.class.getName());
	private static final int TICK = 1000; // 1s
	private final Trap _trap;
	
	public TrapTask(Trap trap)
	{
		_trap = trap;
	}
	
	@Override
	public void run()
	{
		try
		{
			if (!_trap.isTriggered())
			{
				if (_trap.hasLifeTime())
				{
					_trap.setRemainingTime(_trap.getRemainingTime() - TICK);
					if (_trap.getRemainingTime() < (_trap.getLifeTime() - 15000))
					{
						_trap.broadcastPacket(new SocialAction(_trap.getObjectId(), 2));
					}
					
					if (_trap.getRemainingTime() <= 0)
					{
						switch (_trap.getSkill().getTargetType())
						{
							case AURA:
							case FRONT_AURA:
							case BEHIND_AURA:
							{
								_trap.triggerTrap(_trap);
								break;
							}
							default:
							{
								_trap.unSummon();
							}
						}
						return;
					}
				}
				
				for (Creature target : World.getInstance().getVisibleObjects(_trap, Creature.class))
				{
					if (_trap.checkTarget(target))
					{
						_trap.triggerTrap(target);
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.severe(TrapTask.class.getSimpleName() + ": " + e.getMessage());
			_trap.unSummon();
		}
	}
}
