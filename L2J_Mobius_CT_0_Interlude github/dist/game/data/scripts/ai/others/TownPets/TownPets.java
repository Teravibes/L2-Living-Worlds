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
package ai.others.TownPets;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.spawns.Spawn;

/**
 * Town Pet AI
 * @author Mobius
 */
public class TownPets extends Script
{
	// NPCs
	private static final int[] PETS =
	{
		31202, // Maximus
		31203, // Moon Dancer
		31204, // Georgio
		31205, // Katz
		31206, // Ten Ten
		31207, // Sardinia
		31208, // La Grange
		31209, // Misty Rain
		31266, // Kaiser
		31593, // Dorothy
		31606, // Alice de Catrina
		31609, // Aurora the Unicorn
		31625, // Yeti
		31630, // Kaleidos the Unicorn
		31703, // Saint's Follower_a
		31704, // Saint's Follower_b
		31758, // Rafi
		31955, // Ruby
	};
	
	// Misc
	private static final int MOVE_INTERVAL = 5000;
	
	private TownPets()
	{
		addSpawnId(PETS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc != null) && npc.isSpawned() && event.equals("move"))
		{
			if (npc.getWorldRegion().isActive())
			{
				final Spawn spawn = npc.getSpawn();
				final int locX = spawn.getX() + getRandom(-50, 50);
				final int locY = spawn.getY() + getRandom(-50, 50);
				final Location moveLocation = GeoEngine.getInstance().getValidLocation(npc.getX(), npc.getY(), npc.getZ(), locX, locY, npc.getZ(), 0);
				if (npc.calculateDistance3D(moveLocation) > 20)
				{
					npc.getAI().setIntention(Intention.MOVE_TO, moveLocation);
				}
				
				startQuestTimer("move", MOVE_INTERVAL, npc, null);
			}
			else
			{
				startQuestTimer("move", MOVE_INTERVAL * 3, npc, null);
			}
		}
		
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRunning();
		startQuestTimer("move", MOVE_INTERVAL, npc, null);
	}
	
	public static void main(String[] args)
	{
		new TownPets();
	}
}
