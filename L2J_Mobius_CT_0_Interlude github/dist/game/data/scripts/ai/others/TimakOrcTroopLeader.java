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
package ai.others;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Timak Orc Troop Leader AI.
 * @author Mobius
 */
public class TimakOrcTroopLeader extends Script
{
	private static final int TIMAK_ORC_TROOP_LEADER = 20767;
	private static final String[] ON_ATTACK_MSG =
	{
		"Come out, you children of darkness!",
		"Show yourselves!",
		"Destroy the enemy, my brothers!",
		"Forces of darkness! Follow me!"
	};
	
	private TimakOrcTroopLeader()
	{
		addAttackId(TIMAK_ORC_TROOP_LEADER);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isMonster())
		{
			final Monster monster = npc.asMonster();
			if (!monster.isTeleporting() && (getRandom(1, 100) <= npc.getTemplate().getParameters().getInt("SummonPrivateRate", 0)) && (monster.getMinionList().getSpawnedMinionCount() < 3))
			{
				for (MinionHolder is : npc.getTemplate().getParameters().getMinionList("Privates"))
				{
					boolean found = false;
					for (Monster minion : monster.getMinionList().getSpawnedMinions())
					{
						if (minion.getId() == is.getId())
						{
							found = true;
							break;
						}
					}
					
					if (!found)
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(ON_ATTACK_MSG));
						addMinion(monster, is.getId());
						break;
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new TimakOrcTroopLeader();
	}
}
