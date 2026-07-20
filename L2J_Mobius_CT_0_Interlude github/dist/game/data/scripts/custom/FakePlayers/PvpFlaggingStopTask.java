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
package custom.FakePlayers;

import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * TODO: Move it to Creature.
 * @author Mobius
 */
public class PvpFlaggingStopTask extends Script
{
	private PvpFlaggingStopTask()
	{
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc == null)
		{
			return null;
		}
		
		if (npc.isDead())
		{
			cancelQuestTimer("FLAG_CHECK", npc, null);
			cancelQuestTimer("FINISH_FLAG", npc, null);
			cancelQuestTimer("REMOVE_FLAG", npc, null);
			return null;
		}
		
		if (event.equals("FLAG_CHECK"))
		{
			final WorldObject target = npc.getTarget();
			if ((target != null) && (target.isPlayable() || target.isFakePlayer()))
			{
				npc.setScriptValue(1); // in combat
				cancelQuestTimer("FINISH_FLAG", npc, null);
				cancelQuestTimer("REMOVE_FLAG", npc, null);
				startQuestTimer("FINISH_FLAG", PvpConfig.PVP_NORMAL_TIME - 20000, npc, null);
				startQuestTimer("FLAG_CHECK", 5000, npc, null);
			}
		}
		else if (event.equals("FINISH_FLAG"))
		{
			if (npc.isScriptValue(1))
			{
				npc.setScriptValue(2); // blink status
				npc.broadcastInfo(); // update flag status
				startQuestTimer("REMOVE_FLAG", 20000, npc, null);
			}
		}
		else if (event.equals("REMOVE_FLAG"))
		{
			if (npc.isScriptValue(2))
			{
				npc.setScriptValue(0); // not in combat
				npc.broadcastInfo(); // update flag status
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new PvpFlaggingStopTask();
	}
}
