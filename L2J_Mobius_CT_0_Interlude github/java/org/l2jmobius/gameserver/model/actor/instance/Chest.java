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
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * This class manages all chest.
 * @author Julian
 */
public class Chest extends Monster
{
	// Chest NPC ids that are real boxes (openable with a key, explode when hit).
	// Every other chest id is a mimic: it cannot be opened and turns hostile instead.
	// @formatter:off
	private static final int[] BOXES =
	{
		18265, 18266, 18267, 18268, 18269, 18270, 18271, 18272, 18273, 18274,
		18275, 18276, 18277, 18278, 18279, 18280, 18281, 18282, 18283, 18284,
		18285, 18286, 18287, 18288, 18289, 18290, 18291, 18292, 18293, 18294,
		18295, 18296, 18297, 18298
	};
	// @formatter:on

	private final boolean _isBox;
	private volatile boolean _isInteracted;
	private volatile boolean _specialDrop;

	/**
	 * Creates a chest.
	 * @param template the chest NPC template
	 */
	public Chest(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Chest);
		_isBox = ArrayUtil.contains(BOXES, template.getId());
		setRandomWalking(false);
		_specialDrop = false;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInteracted = false;
		_specialDrop = false;
		setMustRewardExpSp(true);
		// onSpawn restores random walking from the template; a chest (box or mimic) must stay put until provoked.
		setRandomWalking(false);
	}

	/**
	 * @return {@code true} if this chest is a box (can be opened with a key and explodes when hit), {@code false} if it is a mimic.
	 */
	public boolean isBox()
	{
		return _isBox;
	}

	public synchronized boolean isInteracted()
	{
		return _isInteracted;
	}
	
	public synchronized void setInteracted()
	{
		_isInteracted = true;
	}
	
	public synchronized void setSpecialDrop()
	{
		_specialDrop = true;
	}
	
	@Override
	public void doItemDrop(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		int id = getTemplate().getId();
		if (!_specialDrop)
		{
			if ((id >= 18265) && (id <= 18286))
			{
				id += 3536;
			}
			else if ((id == 18287) || (id == 18288))
			{
				id = 21671;
			}
			else if ((id == 18289) || (id == 18290))
			{
				id = 21694;
			}
			else if ((id == 18291) || (id == 18292))
			{
				id = 21717;
			}
			else if ((id == 18293) || (id == 18294))
			{
				id = 21740;
			}
			else if ((id == 18295) || (id == 18296))
			{
				id = 21763;
			}
			else if ((id == 18297) || (id == 18298))
			{
				id = 21786;
			}
		}
		
		super.doItemDrop(NpcData.getInstance().getTemplate(id), lastAttacker);
	}
	
	/**
	 * Makes the chest explode by casting Treasure Bomb (skill 4143) on itself.<br>
	 * The skill is an area effect centered on the chest, so it must target self - otherwise a distant
	 * attacker would be out of cast range and the chest would never explode.
	 */
	public void treasureBomb()
	{
		final int skillLevel = Math.max(1, Math.min(10, Math.round(getLevel() / 10f)));
		final Skill skill = SkillData.getInstance().getSkill(4143, skillLevel);
		if (skill != null)
		{
			setTarget(this);
			doCast(skill);
		}
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		// a box never moves; a mimic must be able to chase whoever provokes it
		return _isBox || super.isMovementDisabled();
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
