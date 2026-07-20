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
package quests.Q00117_TheOceanOfDistantStars;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.util.LocationUtil;

public class Q00117_TheOceanOfDistantStars extends Quest
{
	// NPCs
	private static final int ABEY = 32053;
	private static final int GHOST = 32054;
	private static final int ANCIENT_GHOST = 32055;
	private static final int OBI = 32052;
	private static final int BOX = 32076;
	
	// Monsters
	private static final int BANDIT_WARRIOR = 22023;
	private static final int BANDIT_INSPECTOR = 22024;
	
	// Items
	private static final int GREY_STAR = 8495;
	private static final int ENGRAVED_HAMMER = 8488;
	
	public Q00117_TheOceanOfDistantStars()
	{
		super(117, "The Ocean of Distant Stars");
		registerQuestItems(GREY_STAR, ENGRAVED_HAMMER);
		addStartNpc(ABEY);
		addTalkId(ABEY, ANCIENT_GHOST, GHOST, OBI, BOX);
		addKillId(BANDIT_WARRIOR, BANDIT_INSPECTOR);
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
			case "32053-02.htm":
			{
				st.startQuest();
				break;
			}
			case "32055-02.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "32052-02.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "32053-04.htm":
			{
				st.setCond(4, true);
				break;
			}
			case "32076-02.htm":
			{
				st.setCond(5, true);
				giveItems(player, ENGRAVED_HAMMER, 1);
				break;
			}
			case "32053-06.htm":
			{
				st.setCond(6, true);
				break;
			}
			case "32052-04.htm":
			{
				st.setCond(7, true);
				break;
			}
			case "32052-06.htm":
			{
				st.setCond(9, true);
				takeItems(player, GREY_STAR, 1);
				break;
			}
			case "32055-04.htm":
			{
				st.setCond(10, true);
				takeItems(player, ENGRAVED_HAMMER, 1);
				break;
			}
			case "32054-03.htm":
			{
				addExpAndSp(player, 63591, 0);
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
				htmltext = (player.getLevel() < 39) ? "32053-00.htm" : "32053-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ANCIENT_GHOST:
					{
						if (cond == 1)
						{
							htmltext = "32055-01.htm";
						}
						else if ((cond > 1) && (cond < 9))
						{
							htmltext = "32055-02.htm";
						}
						else if (cond == 9)
						{
							htmltext = "32055-03.htm";
						}
						else if (cond > 9)
						{
							htmltext = "32055-05.htm";
						}
						break;
					}
					case OBI:
					{
						if (cond == 2)
						{
							htmltext = "32052-01.htm";
						}
						else if ((cond > 2) && (cond < 6))
						{
							htmltext = "32052-02.htm";
						}
						else if (cond == 6)
						{
							htmltext = "32052-03.htm";
						}
						else if (cond == 7)
						{
							htmltext = "32052-04.htm";
						}
						else if (cond == 8)
						{
							htmltext = "32052-05.htm";
						}
						else if (cond > 8)
						{
							htmltext = "32052-06.htm";
						}
						break;
					}
					case ABEY:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "32053-02.htm";
						}
						else if (cond == 3)
						{
							htmltext = "32053-03.htm";
						}
						else if (cond == 4)
						{
							htmltext = "32053-04.htm";
						}
						else if (cond == 5)
						{
							htmltext = "32053-05.htm";
						}
						else if (cond > 5)
						{
							htmltext = "32053-06.htm";
						}
						break;
					}
					case BOX:
					{
						if (cond == 4)
						{
							htmltext = "32076-01.htm";
						}
						else if (cond > 4)
						{
							htmltext = "32076-03.htm";
						}
						break;
					}
					case GHOST:
					{
						if (cond == 10)
						{
							htmltext = "32054-01.htm";
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(killer, 7, 3, npc);
		if ((qs == null) || !LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true))
		{
			return;
		}
		
		if (giveItemRandomly(killer, npc, GREY_STAR, 1, 1, 0.2, true))
		{
			qs.setCond(8);
		}
	}
}
