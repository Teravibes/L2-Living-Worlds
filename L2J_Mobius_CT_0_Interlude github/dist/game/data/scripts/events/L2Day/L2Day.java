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
package events.L2Day;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;

/**
 * L2 Day event AI.
 * @author Mobius
 */
public class L2Day extends LongTimeEvent
{
	// NPCs
	private static final Map<Integer, Integer> MANAGERS = new HashMap<>();
	static
	{
		MANAGERS.put(31854, 7117); // Talking Island Village
		MANAGERS.put(31855, 7118); // Elven Village
		MANAGERS.put(31856, 7119); // Dark Elven Village
		MANAGERS.put(31857, 7121); // Dwarven Village
		MANAGERS.put(31858, 7120); // Orc Village
	}
	
	// Items
	private static final int A = 3875;
	private static final int C = 3876;
	private static final int E = 3877;
	private static final int F = 3878;
	private static final int G = 3879;
	private static final int H = 3880;
	private static final int I = 3881;
	private static final int L = 3882;
	private static final int N = 3883;
	private static final int O = 3884;
	private static final int R = 3885;
	private static final int S = 3886;
	private static final int T = 3887;
	private static final int II = 3888;
	
	// Rewards
	private static final ItemChanceHolder[] L2_REWARDS =
	{
		new ItemChanceHolder(8947, 19, 1), // L2day - Rabbit Ears
		new ItemChanceHolder(8948, 16, 1), // L2day - Little Angel Wings
		new ItemChanceHolder(8949, 13, 1), // L2day - Fairy Antennae
		new ItemChanceHolder(3959, 10, 2), // Blessed Scroll of Resurrection (Event)
		new ItemChanceHolder(3958, 7, 2), // Blessed Scroll of Escape (Event)
		new ItemChanceHolder(8752, 4, 2), // High-Grade Life Stone - Level 76
		new ItemChanceHolder(8762, 1, 1), // Top-Grade Life Stone - Level 76
		new ItemChanceHolder(6660, 0, 1), // Ring of Queen Ant
	};
	private static final ItemChanceHolder[] NC_REWARDS =
	{
		new ItemChanceHolder(8948, 19, 1), // L2day - Little Angel Wings
		new ItemChanceHolder(8949, 16, 1), // L2day - Fairy Antennae
		new ItemChanceHolder(8950, 13, 1), // L2day - Feathered Hat
		new ItemChanceHolder(3959, 10, 1), // Blessed Scroll of Resurrection (Event)
		new ItemChanceHolder(3958, 7, 1), // Blessed Scroll of Escape (Event)
		new ItemChanceHolder(8742, 4, 2), // Mid-Grade Life Stone - Level 76
		new ItemChanceHolder(8752, 1, 1), // High-Grade Life Stone - Level 76
		new ItemChanceHolder(6661, 0, 1), // Earring of Orfen
	};
	private static final ItemChanceHolder[] CH_REWARDS =
	{
		new ItemChanceHolder(8949, 19, 1), // L2day - Fairy Antennae
		new ItemChanceHolder(8950, 16, 1), // L2day - Feathered Hat
		new ItemChanceHolder(8951, 13, 1), // L2day - Artisan's Goggles
		new ItemChanceHolder(3959, 10, 1), // Blessed Scroll of Resurrection (Event)
		new ItemChanceHolder(3958, 7, 1), // Blessed Scroll of Escape (Event)
		new ItemChanceHolder(8742, 4, 1), // Mid-Grade Life Stone - Level 76
		new ItemChanceHolder(8752, 1, 1), // High-Grade Life Stone - Level 76
		new ItemChanceHolder(6662, 0, 1), // Ring of Core
	};
	
	private L2Day()
	{
		addStartNpc(MANAGERS.keySet());
		addFirstTalkId(MANAGERS.keySet());
		addTalkId(MANAGERS.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		switch (event)
		{
			case "collect_l2":
			{
				if (hasQuestItems(player, L, I, N, E, A, G, II) && (getQuestItemsCount(player, E) > 1))
				{
					takeItems(player, 1, L, I, N, E, A, G, E, II);
					final int random = getRandom(100);
					if (random >= 95)
					{
						giveItems(player, MANAGERS.get(npc.getId()), 2);
					}
					else
					{
						for (ItemChanceHolder holder : L2_REWARDS)
						{
							if (random >= holder.getChance())
							{
								giveItems(player, holder);
								break;
							}
						}
					}
					
					htmltext = "manager-1.htm";
				}
				else
				{
					htmltext = "manager-no.htm";
				}
				break;
			}
			case "collect_nc":
			{
				if (hasQuestItems(player, N, C, S, O, F, T))
				{
					takeItems(player, 1, N, C, S, O, F, T);
					final int random = getRandom(100);
					if (random >= 95)
					{
						giveItems(player, MANAGERS.get(npc.getId()), 1);
					}
					else
					{
						for (ItemChanceHolder holder : NC_REWARDS)
						{
							if (random >= holder.getChance())
							{
								giveItems(player, holder);
								break;
							}
						}
					}
					
					htmltext = "manager-1.htm";
				}
				else
				{
					htmltext = "manager-no.htm";
				}
				break;
			}
			case "collect_ch":
			{
				if (hasQuestItems(player, C, H, R, O, N, I, L, E) && (getQuestItemsCount(player, C) > 1))
				{
					takeItems(player, 1, C, H, R, O, N, I, C, L, E);
					final int random = getRandom(100);
					if (random >= 95)
					{
						giveItems(player, MANAGERS.get(npc.getId()), 1);
					}
					else
					{
						for (ItemChanceHolder holder : CH_REWARDS)
						{
							if (random >= holder.getChance())
							{
								giveItems(player, holder);
								break;
							}
						}
					}
					
					htmltext = "manager-1.htm";
				}
				else
				{
					htmltext = "manager-no.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "manager-1.htm";
	}
	
	public static void main(String[] args)
	{
		new L2Day();
	}
}
