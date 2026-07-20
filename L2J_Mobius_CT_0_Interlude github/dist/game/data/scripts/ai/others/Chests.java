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
package ai.others;

import java.util.List;

import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Chest;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;

/**
 * Chest AI implementation.
 * @author Fulminus
 */
public class Chests extends Script
{
	// NPCs
	// @formatter:off
	private static final int[] TREASURE_CHESTS =
	{
		18265, 18266, 18267, 18268, 18269, 18270, 18271, 18272, 18273, 18274,
		18275, 18276, 18277, 18278, 18279, 18280, 18281, 18282, 18283, 18284,
		18285, 18286, 18287, 18288, 18289, 18290, 18291, 18292, 18293, 18294,
		18295, 18296, 18297, 18298, 21671, 21694, 21717, 21740, 21763, 21786,
		21801, 21802, 21803, 21804, 21805, 21806, 21807, 21808, 21809, 21810,
		21811, 21812, 21813, 21814, 21815, 21816, 21817, 21818, 21819, 21820,
		21821, 21822
	};
	// @formatter:on

	private Chests()
	{
		addAttackId(TREASURE_CHESTS);
		addSkillSeeId(TREASURE_CHESTS);
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (npc instanceof Chest)
		{
			// this behavior is only run when the target of skill is the passed npc (chest)
			// i.e. when the player is attempting to open the chest using a skill
			boolean found = false;
			for (WorldObject target : targets)
			{
				if (target == npc)
				{
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				return;
			}
			
			// keys / unlock skills are handled by the OpenChest skill effect
			if (skill.getTargetType() == TargetType.UNLOCKABLE)
			{
				return;
			}

			// only a box explodes when forced open by an offensive skill; a mimic retaliates as a normal monster
			final Chest chest = ((Chest) npc);
			if (chest.isBox() && !chest.isInteracted())
			{
				chest.setInteracted();
				chest.treasureBomb();
			}
		}
	}

	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc instanceof Chest)
		{
			// only a box explodes on the first hit; a mimic retaliates as a normal monster
			final Chest chest = ((Chest) npc);
			if (chest.isBox() && !chest.isInteracted())
			{
				chest.setInteracted();
				chest.treasureBomb();
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Chests();
	}
}
