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
package org.l2jmobius.gameserver.model.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.functions.AbstractFunction;
import org.l2jmobius.gameserver.model.stats.functions.FuncTemplate;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;

/**
 * @author UnAfraid
 */
public class Options
{
	private final int _id;
	private final List<FuncTemplate> _funcs = new ArrayList<>();
	private Skill _activeSkill = null;
	private Skill _passiveSkill = null;
	private final List<OptionSkillHolder> _activationSkills = new ArrayList<>();
	
	/**
	 * @param id
	 */
	public Options(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public boolean hasFuncs()
	{
		return !_funcs.isEmpty();
	}
	
	public List<AbstractFunction> getStatFuncs(Item item, Creature creature)
	{
		if (_funcs.isEmpty())
		{
			return Collections.<AbstractFunction> emptyList();
		}
		
		final List<AbstractFunction> funcs = new ArrayList<>(_funcs.size());
		for (FuncTemplate fuctionTemplate : _funcs)
		{
			final AbstractFunction fuction = fuctionTemplate.getFunc(creature, creature, item, this);
			if (fuction != null)
			{
				funcs.add(fuction);
			}
		}
		
		return funcs;
	}
	
	public void addFunc(FuncTemplate template)
	{
		_funcs.add(template);
	}
	
	public boolean hasActiveSkill()
	{
		return _activeSkill != null;
	}
	
	public Skill getActiveSkill()
	{
		return _activeSkill;
	}
	
	public void setActiveSkill(Skill skill)
	{
		_activeSkill = skill;
	}
	
	public boolean hasPassiveSkill()
	{
		return _passiveSkill != null;
	}
	
	public Skill getPassiveSkill()
	{
		return _passiveSkill;
	}
	
	public void setPassiveSkill(Skill skill)
	{
		_passiveSkill = skill;
	}
	
	public boolean hasActivationSkills()
	{
		return !_activationSkills.isEmpty();
	}
	
	public boolean hasActivationSkills(OptionSkillType type)
	{
		for (OptionSkillHolder holder : _activationSkills)
		{
			if (holder.getSkillType() == type)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public List<OptionSkillHolder> getActivationSkills()
	{
		return _activationSkills;
	}
	
	public List<OptionSkillHolder> getActivationSkills(OptionSkillType type)
	{
		final List<OptionSkillHolder> temp = new ArrayList<>();
		for (OptionSkillHolder holder : _activationSkills)
		{
			if (holder.getSkillType() == type)
			{
				temp.add(holder);
			}
		}
		
		return temp;
	}
	
	public void addActivationSkill(OptionSkillHolder holder)
	{
		_activationSkills.add(holder);
	}
	
	public void apply(Player player)
	{
		if (!_funcs.isEmpty())
		{
			player.addStatFuncs(getStatFuncs(null, player));
		}
		
		if (hasActiveSkill())
		{
			addSkill(player, _activeSkill);
		}
		
		if (hasPassiveSkill())
		{
			addSkill(player, _passiveSkill);
		}
		
		if (!_activationSkills.isEmpty())
		{
			for (OptionSkillHolder holder : _activationSkills)
			{
				player.addTriggerSkill(holder);
			}
		}
		
		player.sendSkillList();
	}
	
	public void remove(Player player)
	{
		if (!_funcs.isEmpty())
		{
			player.removeStatsOwner(this);
		}
		
		if (hasActiveSkill())
		{
			player.removeSkill(_activeSkill, false, false);
		}
		
		if (hasPassiveSkill())
		{
			player.removeSkill(_passiveSkill, false, true);
		}
		
		if (!_activationSkills.isEmpty())
		{
			for (OptionSkillHolder holder : _activationSkills)
			{
				player.removeTriggerSkill(holder);
			}
		}
		
		player.sendSkillList();
	}
	
	private void addSkill(Player player, Skill skill)
	{
		boolean updateTimeStamp = false;
		player.addSkill(skill, false);
		if (skill.isActive())
		{
			final long remainingTime = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
			if (remainingTime > 0)
			{
				player.addTimeStamp(skill, remainingTime);
				player.disableSkill(skill, remainingTime);
			}
			
			updateTimeStamp = true;
		}
		
		if (updateTimeStamp)
		{
			player.sendPacket(new SkillCoolTime(player));
		}
	}
}
