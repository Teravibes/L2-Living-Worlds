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
package ai.areas.PlainsOfDion;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * AI for mobs in Plains of Dion (near Floran Village).
 * @author Gladicek
 */
public class PlainsOfDion extends Script
{
	private static final int[] DELU_LIZARDMEN =
	{
		21104, // Delu Lizardman Supplier
		21105, // Delu Lizardman Special Agent
		21107, // Delu Lizardman Commander
	};
	
	private static final String[] MONSTERS_MSG =
	{
		"$s1! How dare you interrupt our fight! Hey guys, help!",
		"$s1! Hey! We're having a duel here!",
		"The duel is over! Attack!",
		"Foul! Kill the coward!",
		"How dare you interrupt a sacred duel! You must be taught a lesson!"
	};
	
	private static final String[] MONSTERS_ASSIST_MSG =
	{
		"Die, you coward!",
		"Kill the coward!",
		"What are you looking at?"
	};
	
	private PlainsOfDion()
	{
		addAttackId(DELU_LIZARDMEN);
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			final int i = getRandom(5);
			if (i < 2)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_MSG[i].replace("$s1", player.getName()));
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_MSG[i]);
			}
			
			World.getInstance().forEachVisibleObjectInRange(npc, Monster.class, npc.getTemplate().getClanHelpRange(), obj ->
			{
				if (ArrayUtil.contains(DELU_LIZARDMEN, obj.getId()) && !obj.isAttackingNow() && !obj.isDead() && GeoEngine.getInstance().canSeeTarget(npc, obj))
				{
					addAttackDesire(obj, player);
					obj.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_ASSIST_MSG[getRandom(3)]);
				}
			});
			npc.setScriptValue(1);
		}
	}
	
	public static void main(String[] args)
	{
		new PlainsOfDion();
	}
}
