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
package org.l2jmobius.gameserver.model.events.holders.actor.creature;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * An instantly executed event when Creature is attacked by Creature.
 * @author UnAfraid
 */
public class OnCreatureDamageReceived implements IBaseEvent
{
	private Creature _attacker;
	private Creature _target;
	private double _damage;
	private Skill _skill;
	private boolean _crit;
	private boolean _damageOverTime;
	
	public OnCreatureDamageReceived()
	{
	}
	
	public Creature getAttacker()
	{
		return _attacker;
	}
	
	public synchronized void setAttacker(Creature attacker)
	{
		_attacker = attacker;
	}
	
	public Creature getTarget()
	{
		return _target;
	}
	
	public synchronized void setTarget(Creature target)
	{
		_target = target;
	}
	
	public double getDamage()
	{
		return _damage;
	}
	
	public synchronized void setDamage(double damage)
	{
		_damage = damage;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public synchronized void setSkill(Skill skill)
	{
		_skill = skill;
	}
	
	public boolean isCritical()
	{
		return _crit;
	}
	
	public synchronized void setCritical(boolean crit)
	{
		_crit = crit;
	}
	
	public boolean isDamageOverTime()
	{
		return _damageOverTime;
	}
	
	public synchronized void setDamageOverTime(boolean damageOverTime)
	{
		_damageOverTime = damageOverTime;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_DAMAGE_RECEIVED;
	}
}
