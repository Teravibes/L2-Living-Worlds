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
package ai.areas.PaganTemple;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Altar Gatekeeper retail door-state announcer for Pagan Temple altar 3rd floor (32051).<br>
 * Four instances spawn simultaneously when the altar doors open; only the first one shouts the retail NCSoft message to keep the burst from quadrupling the chat.
 * @author Altur
 */
public class AltarGatekeeper extends Script
{
	private static final int ALTAR_GATEKEEPER = 32051;
	private static final long SHOUT_COOLDOWN = 30_000L;
	
	private static volatile long _lastShoutAt;
	
	private AltarGatekeeper()
	{
		addSpawnId(ALTAR_GATEKEEPER);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final long now = System.currentTimeMillis();
		if ((now - _lastShoutAt) < SHOUT_COOLDOWN)
		{
			return;
		}
		
		_lastShoutAt = now;
		npc.broadcastSay(ChatType.NPC_GENERAL, "The door to the 3rd floor of the altar is now open.");
	}
	
	public static void main(String[] args)
	{
		new AltarGatekeeper();
	}
}
