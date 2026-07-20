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

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Gordon AI
 * @author TOFIZ, malyelfik
 */
public class Gordon extends Script
{
	private static final int GORDON = 29095;
	
	private Gordon()
	{
		addSpawnId(GORDON);
		addCreatureSeeId(GORDON);
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() && creature.asPlayer().isCursedWeaponEquipped())
		{
			addAttackDesire(npc, creature);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.asAttackable().setCanReturnToSpawnPoint(false);
	}
	
	public static void main(String[] args)
	{
		new Gordon();
	}
}
