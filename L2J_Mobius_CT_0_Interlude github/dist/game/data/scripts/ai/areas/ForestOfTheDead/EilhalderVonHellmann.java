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
package ai.areas.ForestOfTheDead;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Mobius
 */
public class EilhalderVonHellmann extends Script
{
	private static final int EILHALDER_VON_HELLMANN = 25328;
	private static final Location SPAWN_LOCATION = new Location(59090, -42188, -3003);
	private static Npc _npcInstance;
	
	private EilhalderVonHellmann()
	{
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc != null)
		{
			if (npc.isInCombat())
			{
				startQuestTimer("despawn", 30000, _npcInstance, null);
			}
			else
			{
				npc.deleteMe();
				_npcInstance = null;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@RegisterEvent(EventType.ON_DAY_NIGHT_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDayNightChange(OnDayNightChange event)
	{
		if (!event.isNight())
		{
			if (_npcInstance != null)
			{
				if (!_npcInstance.isInCombat())
				{
					_npcInstance.deleteMe();
					_npcInstance = null;
				}
				else
				{
					startQuestTimer("despawn", 30000, _npcInstance, null);
				}
			}
		}
		else if (_npcInstance == null)
		{
			_npcInstance = addSpawn(EILHALDER_VON_HELLMANN, SPAWN_LOCATION);
		}
	}
	
	public static void main(String[] args)
	{
		new EilhalderVonHellmann();
	}
}
