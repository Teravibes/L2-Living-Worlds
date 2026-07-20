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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * Polymorphing on attack monsters AI.
 * @author Slyce
 */
public class PolymorphingOnAttack extends Script
{
	private static final Map<Integer, List<Integer>> MOBSPAWNS = new HashMap<>();
	static
	{
		MOBSPAWNS.put(21258, Arrays.asList(21259, 100, 100, -1)); // Fallen Orc Shaman -> Sharp Talon Tiger (always polymorphs)
		MOBSPAWNS.put(21261, Arrays.asList(21262, 100, 20, 0)); // Ol Mahum Transcender 1st stage
		MOBSPAWNS.put(21262, Arrays.asList(21263, 100, 10, 1)); // Ol Mahum Transcender 2nd stage
		MOBSPAWNS.put(21263, Arrays.asList(21264, 100, 5, 2)); // Ol Mahum Transcender 3rd stage
		MOBSPAWNS.put(21265, Arrays.asList(21271, 100, 33, 0)); // Cave Ant Larva -> Cave Ant
		MOBSPAWNS.put(21266, Arrays.asList(21269, 100, 100, -1)); // Cave Ant Larva -> Cave Ant (always polymorphs)
		MOBSPAWNS.put(21267, Arrays.asList(21270, 100, 100, -1)); // Cave Ant Larva -> Cave Ant Soldier (always polymorphs)
		MOBSPAWNS.put(21271, Arrays.asList(21272, 66, 10, 1)); // Cave Ant -> Cave Ant Soldier
		MOBSPAWNS.put(21272, Arrays.asList(21273, 33, 5, 2)); // Cave Ant Soldier -> Cave Noble Ant
		MOBSPAWNS.put(21521, Arrays.asList(21522, 100, 30, -1)); // Claws of Splendor
		MOBSPAWNS.put(21524, Arrays.asList(21525, 100, 30, -1)); // Blade of Splendor
		MOBSPAWNS.put(21527, Arrays.asList(21528, 100, 30, -1)); // Anger of Splendor
		MOBSPAWNS.put(21531, Arrays.asList(21658, 100, 30, -1)); // Punishment of Splendor
		MOBSPAWNS.put(21533, Arrays.asList(21534, 100, 30, -1)); // Alliance of Splendor
		MOBSPAWNS.put(21537, Arrays.asList(21538, 100, 30, -1)); // Fang of Splendor
		MOBSPAWNS.put(21539, Arrays.asList(21540, 100, 30, -1)); // Wailing of Splendor
	}
	
	protected static final String[][] MOBTEXTS =
	{
		new String[]
		{
			"Enough fooling around. Get ready to die!",
			"You idiot! I've just been toying with you!",
			"Now the fun starts!"
		},
		new String[]
		{
			"I must admit, no one makes my blood boil quite like you do!",
			"Now the battle begins!",
			"Witness my true power!"
		},
		new String[]
		{
			"Prepare to die!",
			"I'll double my strength!",
			"You have more skill than I thought!"
		}
	};
	
	private PolymorphingOnAttack()
	{
		addAttackId(MOBSPAWNS.keySet());
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isSpawned() && !npc.isDead())
		{
			final List<Integer> tmp = MOBSPAWNS.get(npc.getId());
			if ((tmp != null) && (npc.getCurrentHp() <= ((npc.getMaxHp() * tmp.get(1)) / 100.0)) && (getRandom(100) < tmp.get(2)))
			{
				if (tmp.get(3) >= 0)
				{
					final String npcString = MOBTEXTS[tmp.get(3)][getRandom(MOBTEXTS[tmp.get(3)].length)];
					npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, npcString));
				}
				
				npc.deleteMe();
				final Attackable newNpc = addSpawn(tmp.get(0), npc.getX(), npc.getY(), npc.getZ() + 20, npc.getHeading(), false, 0, true).asAttackable();
				final Creature originalAttacker = isSummon ? attacker.getSummon() : attacker;
				newNpc.setRunning();
				newNpc.addDamageHate(originalAttacker, 0, 500);
				newNpc.getAI().setIntention(Intention.ATTACK, originalAttacker);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new PolymorphingOnAttack();
	}
}
