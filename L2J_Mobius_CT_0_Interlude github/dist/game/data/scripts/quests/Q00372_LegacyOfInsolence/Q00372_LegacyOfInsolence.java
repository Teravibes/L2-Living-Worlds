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
package quests.Q00372_LegacyOfInsolence;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00372_LegacyOfInsolence extends Quest
{
	// NPCs
	private static final int WALDERAL = 30844;
	private static final int PATRIN = 30929;
	private static final int HOLLY = 30839;
	private static final int CLAUDIA = 31001;
	private static final int DESMOND = 30855;
	
	// Monsters
	private static final int[][] MONSTERS_DROPS =
	{
		// @formatter:off
		{20817, 20821, 20825, 20829, 21069, 21063}, // npcId
		{5966, 5966, 5966, 5967, 5968, 5969}, // parchment (red, blue, black, white)
		{300000, 400000, 460000, 400000, 250000, 250000} // rate
	};
	
	// Items
	private static final int[][] SCROLLS =
	{
		{5989, 6001}, // Walderal => 13 blueprints => parts, recipes.
		{5984, 5988}, // Holly -> 5x Imperial Genealogy -> Dark Crystal parts/Adena
		{5979, 5983}, // Patrin -> 5x Ancient Epic -> Tallum parts/Adena
		{5972, 5978}, // Claudia -> 7x Revelation of the Seals -> Nightmare parts/Adena
		{5972, 5978}, // Desmond -> 7x Revelation of the Seals -> Majestic parts/Adena
	};
	
	// Rewards matrice.
	private static final int[][][] REWARDS_MATRICE =
	{
		// Walderal DC choice
		{{13, 5496},{26, 5508},{40, 5525},{58, 5368},{76, 5392},{100, 5426}},
		// Walderal Tallum choice
		{{13, 5497},{26, 5509},{40, 5526},{58, 5370},{76, 5394},{100, 5428}},
		// Walderal NM choice
		{{20, 5502},{40, 5514},{58, 5527},{73, 5380},{87, 5404},{100, 5430}},
		// Walderal Maja choice
		{{20, 5503},{40, 5515},{58, 5528},{73, 5382},{87, 5406},{100, 5432}},
		// Holly DC parts + adenas.
		{{33, 5496},{66, 5508},{89, 5525},{100, 57}},
		// Patrin Tallum parts + adenas.
		{{33, 5497},{66, 5509},{89, 5526},{100, 57}},
		// Claudia NM parts + adenas.
		{{35, 5502},{70, 5514},{87, 5527},{100, 57}},
		// Desmond Maja choice
		{{35, 5503},{70, 5515},{87, 5528},{100, 57}}
		// @formatter:on
	};
	
	public Q00372_LegacyOfInsolence()
	{
		super(372, "Legacy of Insolence");
		addStartNpc(WALDERAL);
		addTalkId(WALDERAL, PATRIN, HOLLY, CLAUDIA, DESMOND);
		addKillId(MONSTERS_DROPS[0]);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30844-04.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30844-05b.htm"))
		{
			if (st.isCond(1))
			{
				st.setCond(2, true);
			}
		}
		else if (event.equals("30844-07.htm"))
		{
			for (int blueprint = 5989; blueprint <= 6001; blueprint++)
			{
				if (!hasQuestItems(player, blueprint))
				{
					htmltext = "30844-06.htm";
					break;
				}
			}
		}
		else if (event.startsWith("30844-07-"))
		{
			checkAndRewardItems(player, 0, Integer.parseInt(event.substring(9, 10)), WALDERAL);
		}
		else if (event.equals("30844-09.htm"))
		{
			st.exitQuest(true, true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() < 59) ? "30844-01.htm" : "30844-02.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case WALDERAL:
					{
						htmltext = "30844-05.htm";
						break;
					}
					case HOLLY:
					{
						htmltext = checkAndRewardItems(player, 1, 4, HOLLY);
						break;
					}
					case PATRIN:
					{
						htmltext = checkAndRewardItems(player, 2, 5, PATRIN);
						break;
					}
					case CLAUDIA:
					{
						htmltext = checkAndRewardItems(player, 3, 6, CLAUDIA);
						break;
					}
					case DESMOND:
					{
						htmltext = checkAndRewardItems(player, 4, 7, DESMOND);
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getRandomPartyMemberState(player, -1, 3, npc);
		if (st == null)
		{
			return;
		}
		
		final int npcId = npc.getId();
		for (int i = 0; i < MONSTERS_DROPS[0].length; i++)
		{
			if ((MONSTERS_DROPS[0][i] == npcId) && (getRandom(1000000) < MONSTERS_DROPS[2][i]))
			{
				final Player partyMember = st.getPlayer();
				giveItems(partyMember, MONSTERS_DROPS[1][i], 1);
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				break;
			}
		}
	}
	
	private static String checkAndRewardItems(Player player, int itemType, int rewardType, int npcId)
	{
		// Retrieve array with items to check.
		final int[] itemsToCheck = SCROLLS[itemType];
		
		// Check set of items.
		for (int item = itemsToCheck[0]; item <= itemsToCheck[1]; item++)
		{
			if (!hasQuestItems(player, item))
			{
				return npcId + ((npcId == WALDERAL) ? "-07a.htm" : "-01.htm");
			}
		}
		
		// Remove set of items.
		for (int item = itemsToCheck[0]; item <= itemsToCheck[1]; item++)
		{
			takeItems(player, item, 1);
		}
		
		// Retrieve array with rewards.
		final int[][] rewards = REWARDS_MATRICE[rewardType];
		final int chance = getRandom(100);
		for (int[] reward : rewards)
		{
			if (chance < reward[0])
			{
				rewardItems(player, reward[1], 1);
				return npcId + "-02.htm";
			}
		}
		
		return npcId + ((npcId == WALDERAL) ? "-07a.htm" : "-01.htm");
	}
}
