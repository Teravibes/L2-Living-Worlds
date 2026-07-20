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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Custom script to remove custom key drop from NPC XMLs.<br>
 * Used to access more conveniently Pagan Temple.
 * @author Mobius
 */
public class PaganKeys extends Script
{
	// Items
	private static final int ANTEROOM_KEY = 8273;
	private static final int CHAPEL_KEY = 8274;
	private static final int KEY_OF_DARKNESS = 8275;
	
	// NPCs
	private static final int ZOMBIE_WORKER = 22140;
	private static final int TRIOLS_LAYPERSON = 22142;
	private static final int TRIOLS_PRIEST = 22168;
	
	// Misc
	private static final int ANTEROOM_KEY_CHANCE = 10;
	private static final int CHAPEL_KEY_CHANCE = 10;
	private static final int KEY_OF_DARKNESS_CHANCE = 10;
	
	private PaganKeys()
	{
		addKillId(ZOMBIE_WORKER, TRIOLS_LAYPERSON, TRIOLS_PRIEST);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case ZOMBIE_WORKER:
			{
				if (getRandom(100) < ANTEROOM_KEY_CHANCE)
				{
					if (PlayerConfig.AUTO_LOOT)
					{
						giveItems(killer, ANTEROOM_KEY, 1);
					}
					else
					{
						npc.dropItem(killer, ANTEROOM_KEY, 1);
					}
				}
				break;
			}
			case TRIOLS_LAYPERSON:
			{
				if (getRandom(100) < CHAPEL_KEY_CHANCE)
				{
					if (PlayerConfig.AUTO_LOOT)
					{
						giveItems(killer, CHAPEL_KEY, 1);
					}
					else
					{
						npc.dropItem(killer, CHAPEL_KEY, 1);
					}
				}
				break;
			}
			case TRIOLS_PRIEST:
			{
				if (getRandom(100) < KEY_OF_DARKNESS_CHANCE)
				{
					if (PlayerConfig.AUTO_LOOT)
					{
						giveItems(killer, KEY_OF_DARKNESS, 1);
					}
					else
					{
						npc.dropItem(killer, KEY_OF_DARKNESS, 1);
					}
				}
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new PaganKeys();
	}
}
