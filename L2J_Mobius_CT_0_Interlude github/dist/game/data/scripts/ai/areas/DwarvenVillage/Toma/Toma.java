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
package ai.areas.DwarvenVillage.Toma;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Mobius
 */
public class Toma extends Script
{
	// NPC
	private static final int TOMA = 30556;
	
	// Locations
	private static final Location[] LOCATIONS =
	{
		new Location(151680, -174891, -1782),
		new Location(154153, -220105, -3402),
		new Location(178834, -184336, -355, 41400)
	};
	
	// Misc
	private static final int TELEPORT_DELAY = 1800000; // 30 minutes
	private static Npc _toma;
	
	private Toma()
	{
		addFirstTalkId(TOMA);
		onEvent("RESPAWN_TOMA", null, null);
		startQuestTimer("RESPAWN_TOMA", TELEPORT_DELAY, null, null, true);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("RESPAWN_TOMA"))
		{
			if (_toma != null)
			{
				_toma.deleteMe();
			}
			
			_toma = addSpawn(TOMA, getRandomEntry(LOCATIONS), false, TELEPORT_DELAY);
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "30556.htm";
	}
	
	public static void main(String[] args)
	{
		new Toma();
	}
}
