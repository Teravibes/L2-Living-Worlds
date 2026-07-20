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
package org.l2jmobius.gameserver.data.sql;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.instance.Servitor;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Nyaran
 */
public class SummonEffectTable
{
	/** Servitors **/
	
	// Map tree
	// -> key: charObjectId, value: classIndex Map
	// --> key: classIndex, value: servitors Map
	// ---> key: servitorSkillId, value: Effects list
	private final Map<Integer, Map<Integer, Map<Integer, Collection<SummonEffect>>>> _servitorEffects = new ConcurrentHashMap<>();
	
	private Map<Integer, Collection<SummonEffect>> getServitorEffects(Player owner)
	{
		final Map<Integer, Map<Integer, Collection<SummonEffect>>> servitorMap = _servitorEffects.get(owner.getObjectId());
		if (servitorMap == null)
		{
			return null;
		}
		
		return servitorMap.get(owner.getClassIndex());
	}
	
	private Collection<SummonEffect> getServitorEffects(Player owner, int referenceSkill)
	{
		return containsOwner(owner) ? getServitorEffects(owner).get(referenceSkill) : null;
	}
	
	private boolean containsOwner(Player owner)
	{
		return _servitorEffects.getOrDefault(owner.getObjectId(), Collections.emptyMap()).containsKey(owner.getClassIndex());
	}
	
	private void removeEffects(Collection<SummonEffect> effects, int skillId)
	{
		if ((effects != null) && !effects.isEmpty())
		{
			for (SummonEffect effect : effects)
			{
				final Skill skill = effect.getSkill();
				if ((skill != null) && (skill.getId() == skillId))
				{
					effects.remove(effect);
				}
			}
		}
	}
	
	private void applyEffects(Summon summon, Collection<SummonEffect> summonEffects)
	{
		if (summonEffects == null)
		{
			return;
		}
		
		for (SummonEffect se : summonEffects)
		{
			if (se != null)
			{
				se.getSkill().applyEffects(summon, summon, false, se.getEffectCurTime());
			}
		}
	}
	
	public boolean containsSkill(Player owner, int referenceSkill)
	{
		return containsOwner(owner) && getServitorEffects(owner).containsKey(referenceSkill);
	}
	
	public void clearServitorEffects(Player owner, int referenceSkill)
	{
		if (containsOwner(owner))
		{
			getServitorEffects(owner).getOrDefault(referenceSkill, Collections.emptyList()).clear();
		}
	}
	
	public void addServitorEffect(Player owner, int referenceSkill, Skill skill, int effectCurTime)
	{
		_servitorEffects.putIfAbsent(owner.getObjectId(), new ConcurrentHashMap<>());
		_servitorEffects.get(owner.getObjectId()).putIfAbsent(owner.getClassIndex(), new ConcurrentHashMap<>());
		getServitorEffects(owner).putIfAbsent(referenceSkill, ConcurrentHashMap.newKeySet());
		getServitorEffects(owner).get(referenceSkill).add(new SummonEffect(skill, effectCurTime));
	}
	
	public void removeServitorEffects(Player owner, int referenceSkill, int skillId)
	{
		removeEffects(getServitorEffects(owner, referenceSkill), skillId);
	}
	
	public void applyServitorEffects(Servitor servitor, Player owner, int referenceSkill)
	{
		applyEffects(servitor, getServitorEffects(owner, referenceSkill));
	}
	
	/** Pets **/
	private final Map<Integer, Collection<SummonEffect>> _petEffects = new ConcurrentHashMap<>(); // key: petItemObjectId, value: Effects list
	
	public void addPetEffect(int controlObjectId, Skill skill, int effectCurTime)
	{
		_petEffects.computeIfAbsent(controlObjectId, _ -> ConcurrentHashMap.newKeySet()).add(new SummonEffect(skill, effectCurTime));
	}
	
	public boolean containsPetId(int controlObjectId)
	{
		return _petEffects.containsKey(controlObjectId);
	}
	
	public void applyPetEffects(Pet pet, int controlObjectId)
	{
		applyEffects(pet, _petEffects.get(controlObjectId));
	}
	
	public void clearPetEffects(int controlObjectId)
	{
		_petEffects.getOrDefault(controlObjectId, Collections.emptyList()).clear();
	}
	
	public void removePetEffects(int controlObjectId, int skillId)
	{
		removeEffects(_petEffects.get(controlObjectId), skillId);
	}
	
	private class SummonEffect
	{
		Skill _skill;
		int _effectCurTime;
		
		public SummonEffect(Skill skill, int effectCurTime)
		{
			_skill = skill;
			_effectCurTime = effectCurTime;
		}
		
		public Skill getSkill()
		{
			return _skill;
		}
		
		public int getEffectCurTime()
		{
			return _effectCurTime;
		}
	}
	
	public static SummonEffectTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SummonEffectTable INSTANCE = new SummonEffectTable();
	}
}
