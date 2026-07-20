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
package quests.Q00508_AClansReputation;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class Q00508_AClansReputation extends Quest
{
	// NPC
	private static final int SIR_ERIC_RODEMAI = 30868;
	
	// Raidbosses
	private static final int FLAMESTONE_GIANT = 25524;
	private static final int PALIBATI_QUEEN_THEMIS = 25252;
	private static final int HEKATON_PRIME = 25140;
	private static final int GARGOYLE_LORD_TIPHON = 25255;
	private static final int LAST_LESSER_GIANT_GLAKI = 25245;
	private static final int RAHHA = 25051;
	
	// Items
	private static final int NUCLEUS_OF_FLAMESTONE_GIANT = 8494;
	private static final int THEMIS_SCALE = 8277;
	private static final int NUCLEUS_OF_HEKATON_PRIME = 8279;
	private static final int TIPHON_SHARD = 8280;
	private static final int GLAKIS_NUCLEUS = 8281;
	private static final int RAHHAS_FANG = 8282;
	
	// Reward list (itemId, minClanPoints, maxClanPoints)
	private static final int[][] REWARD_LIST =
	{
		// @formatter:off
		{PALIBATI_QUEEN_THEMIS, THEMIS_SCALE, 65, 100},
		{HEKATON_PRIME, NUCLEUS_OF_HEKATON_PRIME, 40, 75},
		{GARGOYLE_LORD_TIPHON, TIPHON_SHARD, 30, 65},
		{LAST_LESSER_GIANT_GLAKI, GLAKIS_NUCLEUS, 105, 140},
		{RAHHA, RAHHAS_FANG, 40, 75},
		{FLAMESTONE_GIANT, NUCLEUS_OF_FLAMESTONE_GIANT, 60, 95}
	};
	
	// Radar
	private static final int[][] radar =
	{
		{192346, 21528, -3648},
		{191979, 54902, -7658},
		{170038, -26236, -3824},
		{171762, 55028, -5992},
		{117232, -9476, -3320},
		{144218, -5816, -4722}
		// @formatter:on
	};
	
	public Q00508_AClansReputation()
	{
		super(508, "A Clan's Reputation");
		registerQuestItems(THEMIS_SCALE, NUCLEUS_OF_HEKATON_PRIME, TIPHON_SHARD, GLAKIS_NUCLEUS, RAHHAS_FANG, NUCLEUS_OF_FLAMESTONE_GIANT);
		addStartNpc(SIR_ERIC_RODEMAI);
		addTalkId(SIR_ERIC_RODEMAI);
		addKillId(FLAMESTONE_GIANT, PALIBATI_QUEEN_THEMIS, HEKATON_PRIME, GARGOYLE_LORD_TIPHON, LAST_LESSER_GIANT_GLAKI, RAHHA);
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
		
		if (StringUtil.isNumeric(event))
		{
			final int evt = Integer.parseInt(event);
			st.set("raid", event);
			htmltext = "30868-" + event + ".htm";
			
			final int x = radar[evt - 1][0];
			final int y = radar[evt - 1][1];
			final int z = radar[evt - 1][2];
			if ((x + y + z) > 0)
			{
				addRadar(player, x, y, z);
			}
			
			st.startQuest();
		}
		else if (event.equals("30868-7.htm"))
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
		
		final Clan clan = player.getClan();
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (!player.isClanLeader())
				{
					st.exitQuest(true);
					htmltext = "30868-0a.htm";
				}
				else if (clan.getLevel() < 5)
				{
					st.exitQuest(true);
					htmltext = "30868-0b.htm";
				}
				else
				{
					htmltext = "30868-0c.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int raid = st.getInt("raid");
				if (st.isCond(1))
				{
					final int item = REWARD_LIST[raid - 1][1];
					final int count = getQuestItemsCount(player, item);
					final int reward = getRandom(REWARD_LIST[raid - 1][2], REWARD_LIST[raid - 1][3]);
					if (count == 0)
					{
						htmltext = "30868-" + raid + "a.htm";
					}
					else if (count == 1)
					{
						htmltext = "30868-" + raid + "b.htm";
						takeItems(player, item, 1);
						clan.addReputationScore(reward);
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_SUCCESSFULLY_COMPLETED_A_CLAN_QUEST_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE).addInt(reward));
						clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
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
		// Retrieve the QuestState of the clan leader.
		final QuestState st = getClanLeaderQuestState(player, npc);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		// Check if the clan leader is within 1500 range of the raid boss.
		final Player clanLeader = st.getPlayer();
		if (npc.calculateDistance3D(clanLeader) < PlayerConfig.ALT_PARTY_RANGE)
		{
			// Reward only if quest is set up on the correct index.
			final int raid = st.getInt("raid");
			if (REWARD_LIST[raid - 1][0] == npc.getId())
			{
				final int item = REWARD_LIST[raid - 1][1];
				if (!hasQuestItems(clanLeader, item))
				{
					giveItems(clanLeader, item, 1);
					playSound(clanLeader, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
			}
		}
	}
}
