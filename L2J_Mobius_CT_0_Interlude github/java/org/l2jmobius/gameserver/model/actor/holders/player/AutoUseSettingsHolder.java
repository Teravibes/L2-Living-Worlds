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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mobius
 */
public class AutoUseSettingsHolder
{
	private final Collection<Integer> _autoSupplyItems = ConcurrentHashMap.newKeySet();
	private final Collection<Integer> _autoActions = ConcurrentHashMap.newKeySet();
	private final Collection<Integer> _autoBuffs = ConcurrentHashMap.newKeySet();
	private final List<Integer> _autoSkills = new CopyOnWriteArrayList<>();
	private final AtomicInteger _autoPotionItem = new AtomicInteger();
	private int _skillIndex = 0;
	
	public AutoUseSettingsHolder()
	{
	}
	
	public Collection<Integer> getAutoSupplyItems()
	{
		return _autoSupplyItems;
	}
	
	public Collection<Integer> getAutoActions()
	{
		return _autoActions;
	}
	
	public Collection<Integer> getAutoBuffs()
	{
		return _autoBuffs;
	}
	
	public List<Integer> getAutoSkills()
	{
		return _autoSkills;
	}
	
	public int getAutoPotionItem()
	{
		return _autoPotionItem.get();
	}
	
	public void setAutoPotionItem(int itemId)
	{
		_autoPotionItem.set(itemId);
	}
	
	public boolean isAutoSkill(int skillId)
	{
		return _autoSkills.contains(skillId) || _autoBuffs.contains(skillId);
	}
	
	public Integer getNextSkillId()
	{
		if (_skillIndex >= _autoSkills.size())
		{
			_skillIndex = 0;
		}
		
		Integer skillId = Integer.MIN_VALUE;
		try
		{
			skillId = _autoSkills.get(_skillIndex);
		}
		catch (Exception e)
		{
			resetSkillOrder();
		}
		
		return skillId;
	}
	
	public void incrementSkillOrder()
	{
		_skillIndex++;
	}
	
	public void resetSkillOrder()
	{
		_skillIndex = 0;
	}
	
	public boolean isEmpty()
	{
		return _autoSupplyItems.isEmpty() && (_autoPotionItem.get() == 0) && _autoSkills.isEmpty() && _autoBuffs.isEmpty() && _autoActions.isEmpty();
	}
}
