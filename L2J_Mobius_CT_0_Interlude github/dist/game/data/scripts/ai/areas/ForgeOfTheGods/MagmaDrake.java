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
package ai.areas.ForgeOfTheGods;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Mobius
 */
public class MagmaDrake extends Script
{
	// NPCs
	private static final int MAGMA_DRAKE = 21657;
	private static final int MAGMA_DRAKE_MINION = 21393;
	
	private MagmaDrake()
	{
		addKillId(MAGMA_DRAKE);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Creature attacker = isSummon ? killer.getSummon() : killer;
		for (int i = 0; i < 5; i++)
		{
			final Attackable newNpc = addSpawn(MAGMA_DRAKE_MINION, npc.getX(), npc.getY(), npc.getZ() + 20, npc.getHeading(), false, 0, true).asAttackable();
			newNpc.setRunning();
			newNpc.addDamageHate(attacker, 0, 500);
			newNpc.getAI().setIntention(Intention.ATTACK, attacker);
		}
	}
	
	public static void main(String[] args)
	{
		new MagmaDrake();
	}
}
