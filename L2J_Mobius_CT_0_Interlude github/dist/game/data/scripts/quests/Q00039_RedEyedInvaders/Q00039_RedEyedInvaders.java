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
package quests.Q00039_RedEyedInvaders;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00039_RedEyedInvaders extends Quest
{
	// NPCs
	private static final int BABENCO = 30334;
	private static final int BATHIS = 30332;
	
	// Monsters
	private static final int MAILLE_LIZARDMAN = 20919;
	private static final int MAILLE_LIZARDMAN_SCOUT = 20920;
	private static final int MAILLE_LIZARDMAN_GUARD = 20921;
	private static final int ARANEID = 20925;
	
	// Items
	private static final int BLACK_BONE_NECKLACE = 7178;
	private static final int RED_BONE_NECKLACE = 7179;
	private static final int INCENSE_POUCH = 7180;
	private static final int GEM_OF_MAILLE = 7181;
	// @formatter:off
	// First droplist
	private static final Map<Integer, int[]> FIRST_DP = new HashMap<>();
	static
	{
		FIRST_DP.put(MAILLE_LIZARDMAN_GUARD, new int[]{RED_BONE_NECKLACE, BLACK_BONE_NECKLACE, 1000000});
		FIRST_DP.put(MAILLE_LIZARDMAN, new int[]{BLACK_BONE_NECKLACE, RED_BONE_NECKLACE, 1000000});
		FIRST_DP.put(MAILLE_LIZARDMAN_SCOUT, new int[]{BLACK_BONE_NECKLACE, RED_BONE_NECKLACE, 1000000});
	}
	
	// Second droplist
	private static final Map<Integer, int[]> SECOND_DP = new HashMap<>();
	static
	{
		SECOND_DP.put(ARANEID, new int[]{GEM_OF_MAILLE, INCENSE_POUCH, 500000});
		SECOND_DP.put(MAILLE_LIZARDMAN_GUARD, new int[]{INCENSE_POUCH, GEM_OF_MAILLE, 300000});
		SECOND_DP.put(MAILLE_LIZARDMAN_SCOUT, new int[]{INCENSE_POUCH, GEM_OF_MAILLE, 250000});
	}
	// @formatter:on
	
	// Rewards
	private static final int GREEN_COLORED_LURE_HG = 6521;
	private static final int BABY_DUCK_RODE = 6529;
	private static final int FISHING_SHOT_NG = 6535;
	
	public Q00039_RedEyedInvaders()
	{
		super(39, "Red-Eyed Invaders");
		registerQuestItems(BLACK_BONE_NECKLACE, RED_BONE_NECKLACE, INCENSE_POUCH, GEM_OF_MAILLE);
		addStartNpc(BABENCO);
		addTalkId(BABENCO, BATHIS);
		addKillId(MAILLE_LIZARDMAN, MAILLE_LIZARDMAN_SCOUT, MAILLE_LIZARDMAN_GUARD, ARANEID);
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
		
		switch (event)
		{
			case "30334-1.htm":
			{
				st.startQuest();
				break;
			}
			case "30332-1.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "30332-3.htm":
			{
				st.setCond(4, true);
				takeItems(player, BLACK_BONE_NECKLACE, -1);
				takeItems(player, RED_BONE_NECKLACE, -1);
				break;
			}
			case "30332-5.htm":
			{
				takeItems(player, INCENSE_POUCH, -1);
				takeItems(player, GEM_OF_MAILLE, -1);
				giveItems(player, GREEN_COLORED_LURE_HG, 60);
				giveItems(player, BABY_DUCK_RODE, 1);
				giveItems(player, FISHING_SHOT_NG, 500);
				st.exitQuest(false, true);
				break;
			}
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
				htmltext = (player.getLevel() < 20) ? "30334-2.htm" : "30334-0.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case BABENCO:
					{
						htmltext = "30334-3.htm";
						break;
					}
					case BATHIS:
					{
						if (cond == 1)
						{
							htmltext = "30332-0.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30332-2a.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30332-2.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30332-3a.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30332-4.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final int npcId = npc.getId();
		QuestState qs = getRandomPartyMemberState(player, 2, 3, npc);
		if ((qs != null) && (npcId != ARANEID))
		{
			final int[] list = FIRST_DP.get(npcId);
			if (giveItemRandomly(qs.getPlayer(), npc, list[0], 1, 100, list[2] / 1000000d, true) && (getQuestItemsCount(qs.getPlayer(), list[1]) == 100))
			{
				qs.setCond(3, true);
			}
		}
		else
		{
			qs = getRandomPartyMemberState(player, 4, 3, npc);
			if ((qs != null) && (npcId != MAILLE_LIZARDMAN))
			{
				final int[] list = SECOND_DP.get(npcId);
				if (giveItemRandomly(qs.getPlayer(), npc, list[0], 1, 30, list[2] / 1000000d, true) && (getQuestItemsCount(qs.getPlayer(), list[1]) == 30))
				{
					qs.setCond(5, true);
				}
			}
		}
	}
}
