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

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * See Through Silent Move AI.
 * @author Gigiikun
 */
public class SeeThroughSilentMove extends Script
{
	// @formatter:off
	private static final int[] MONSTERS =
	{
		18001, 18002, 22199, 22215, 22216, 22217,
		29009,
		29010, 29011, 29012, 29013
	};
	// @formatter:on
	
	private SeeThroughSilentMove()
	{
		addSpawnId(MONSTERS);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.isAttackable())
		{
			npc.asAttackable().setSeeThroughSilentMove(true);
		}
	}
	
	public static void main(String[] args)
	{
		new SeeThroughSilentMove();
	}
}
