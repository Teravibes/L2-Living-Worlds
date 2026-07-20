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

import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Warrior Fishing Block AI.
 * @author Zoey76
 */
public class WarriorFishingBlock extends Script
{
	// Monsters
	private static final int[] MONSTERS =
	{
		18319, // Caught Frog
		18320, // Caught Undine
		18321, // Caught Rakul
		18322, // Caught Sea Giant
		18323, // Caught Sea Horse Soldier
		18324, // Caught Homunculus
		18325, // Caught Flava
		18326, // Caught Gigantic Eye
	};
	
	// NPC Strings
	private static final String[] NPC_STRINGS_ON_SPAWN =
	{
		"Croak, Croak! Food like $s1 in this place?!",
		"$s1, How lucky I am!",
		"Pray that you caught a wrong fish $s1!"
	};
	private static final String[] NPC_STRINGS_ON_ATTACK =
	{
		"Do you know what a frog tastes like?",
		"I will show you the power of a frog!",
		"I will swallow at a mouthful!"
	};
	private static final String[] NPC_STRINGS_ON_KILL =
	{
		"Ugh, no chance. How could this elder pass away like this!",
		"Croak! Croak! A frog is dying!",
		"A frog tastes bad! Yuck!"
	};
	
	// Misc
	private static final int CHANCE_TO_SHOUT_ON_ATTACK = 33;
	private static final int DESPAWN_TIME = 50; // 50 seconds to despawn
	
	public WarriorFishingBlock()
	{
		addAttackId(MONSTERS);
		addKillId(MONSTERS);
		addSpawnId(MONSTERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "SPAWN":
			{
				final WorldObject obj = npc.getTarget();
				if ((obj == null) || !obj.isPlayer())
				{
					npc.decayMe();
				}
				else
				{
					final Player target = obj.asPlayer();
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_SPAWN).replace("$s1!", target.getName()));
					npc.asAttackable().addDamageHate(target, 0, 2000);
					npc.getAI().notifyAction(Action.ATTACKED, target);
					npc.addAttackerToAttackByList(target);
					
					startQuestTimer("DESPAWN", DESPAWN_TIME * 1000, npc, target);
				}
				break;
			}
			case "DESPAWN":
			{
				npc.decayMe();
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (getRandom(100) < CHANCE_TO_SHOUT_ON_ATTACK)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_ATTACK));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_KILL));
		cancelQuestTimer("DESPAWN", npc, killer);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		cancelQuestTimer("SPAWN", npc, null);
		cancelQuestTimer("DESPAWN", npc, null);
		startQuestTimer("SPAWN", 2000, npc, null);
	}
	
	public static void main(String[] args)
	{
		new WarriorFishingBlock();
	}
}
