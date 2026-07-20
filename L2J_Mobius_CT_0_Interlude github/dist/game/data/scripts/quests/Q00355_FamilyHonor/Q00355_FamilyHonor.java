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
package quests.Q00355_FamilyHonor;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.util.LocationUtil;

public class Q00355_FamilyHonor extends Quest
{
	private static class DropInfo
	{
		public int _firstChance;
		public int _secondChance;
		
		public DropInfo(int firstChance, int secondChance)
		{
			_firstChance = firstChance;
			_secondChance = secondChance;
		}
		
		public int getFirstChance()
		{
			return _firstChance;
		}
		
		public int getSecondChance()
		{
			return _secondChance;
		}
	}
	
	// NPCs
	private static final int GALIBREDO = 30181;
	private static final int PATRIN = 30929;
	
	// Monsters
	private static final int TIMAK_ORC_TROOP_LEADER = 20767;
	private static final int TIMAK_ORC_TROOP_SHAMAN = 20768;
	private static final int TIMAK_ORC_TROOP_WARRIOR = 20769;
	private static final int TIMAK_ORC_TROOP_ARCHER = 20770;
	
	// Items
	private static final int GALIBREDO_BUST = 4252;
	private static final int WORK_OF_BERONA = 4350;
	private static final int STATUE_PROTOTYPE = 4351;
	private static final int STATUE_ORIGINAL = 4352;
	private static final int STATUE_REPLICA = 4353;
	private static final int STATUE_FORGERY = 4354;
	
	// Drop chances
	private static final Map<Integer, DropInfo> MOBS = new HashMap<>();
	static
	{
		MOBS.put(TIMAK_ORC_TROOP_LEADER, new DropInfo(560, 684)); // timak_orc_troop_leader
		MOBS.put(TIMAK_ORC_TROOP_SHAMAN, new DropInfo(530, 650)); // timak_orc_troop_shaman
		MOBS.put(TIMAK_ORC_TROOP_WARRIOR, new DropInfo(420, 516)); // timak_orc_troop_warrior
		MOBS.put(TIMAK_ORC_TROOP_ARCHER, new DropInfo(440, 560)); // timak_orc_troop_archer
	}
	
	public Q00355_FamilyHonor()
	{
		super(355, "Family Honor");
		registerQuestItems(GALIBREDO_BUST);
		addStartNpc(GALIBREDO);
		addTalkId(GALIBREDO, PATRIN);
		addKillId(TIMAK_ORC_TROOP_LEADER, TIMAK_ORC_TROOP_SHAMAN, TIMAK_ORC_TROOP_WARRIOR, TIMAK_ORC_TROOP_ARCHER);
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
			case "30181-2.htm":
			{
				st.startQuest();
				break;
			}
			case "30181-4b.htm":
			{
				final int count = getQuestItemsCount(player, GALIBREDO_BUST);
				if (count > 0)
				{
					htmltext = "30181-4.htm";
					
					int reward = count * 232;
					if (count >= 100)
					{
						htmltext = "30181-4a.htm";
						reward += 5000;
					}
					
					takeItems(player, GALIBREDO_BUST, count);
					giveAdena(player, reward, true);
				}
				break;
			}
			case "30929-7.htm":
			{
				if (hasQuestItems(player, WORK_OF_BERONA))
				{
					takeItems(player, WORK_OF_BERONA, 1);
					
					final int appraising = getRandom(100);
					if (appraising < 20)
					{
						htmltext = "30929-2.htm";
					}
					else if (appraising < 40)
					{
						htmltext = "30929-3.htm";
						giveItems(player, STATUE_REPLICA, 1);
					}
					else if (appraising < 60)
					{
						htmltext = "30929-4.htm";
						giveItems(player, STATUE_ORIGINAL, 1);
					}
					else if (appraising < 80)
					{
						htmltext = "30929-5.htm";
						giveItems(player, STATUE_FORGERY, 1);
					}
					else
					{
						htmltext = "30929-6.htm";
						giveItems(player, STATUE_PROTOTYPE, 1);
					}
				}
				break;
			}
			case "30181-6.htm":
			{
				st.exitQuest(true, true);
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
				htmltext = (player.getLevel() < 36) ? "30181-0a.htm" : "30181-0.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case GALIBREDO:
					{
						htmltext = (hasQuestItems(player, GALIBREDO_BUST)) ? "30181-3a.htm" : "30181-3.htm";
						break;
					}
					case PATRIN:
					{
						htmltext = "30929-0.htm";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs == null) || !LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true))
		{
			return;
		}
		
		final DropInfo info = MOBS.get(npc.getId());
		final int random = getRandom(1000);
		if (random < info.getFirstChance())
		{
			giveItemRandomly(killer, npc, GALIBREDO_BUST, 1, 0, 1, true);
		}
		else if (random < info.getSecondChance())
		{
			giveItemRandomly(killer, npc, WORK_OF_BERONA, 1, 0, 1, true);
		}
	}
}
