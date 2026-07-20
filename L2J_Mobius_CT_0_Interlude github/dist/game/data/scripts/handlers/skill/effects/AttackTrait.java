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

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.model.conditions.Condition;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.TraitType;

/**
 * Attack Trait effect implementation.
 * @author NosBit
 */
public class AttackTrait extends AbstractEffect
{
	private final Map<TraitType, Float> _attackTraits = new EnumMap<>(TraitType.class);
	
	public AttackTrait(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		if (params.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": this effect must have parameters!");
			return;
		}
		
		for (Entry<String, Object> param : params.getSet().entrySet())
		{
			_attackTraits.put(TraitType.valueOf(param.getKey()), (Float.parseFloat((String) param.getValue()) + 100) / 100);
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final CreatureStat charStat = effected.getStat();
		synchronized (charStat.getAttackTraits())
		{
			for (Entry<TraitType, Float> trait : _attackTraits.entrySet())
			{
				if (charStat.getAttackTraitsCount()[trait.getKey().ordinal()] == 0)
				{
					continue;
				}
				
				charStat.getAttackTraits()[trait.getKey().ordinal()] /= trait.getValue();
				charStat.getAttackTraitsCount()[trait.getKey().ordinal()]--;
			}
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		final CreatureStat charStat = effected.getStat();
		synchronized (charStat.getAttackTraits())
		{
			for (Entry<TraitType, Float> trait : _attackTraits.entrySet())
			{
				charStat.getAttackTraits()[trait.getKey().ordinal()] *= trait.getValue();
				charStat.getAttackTraitsCount()[trait.getKey().ordinal()]++;
			}
		}
	}
}
