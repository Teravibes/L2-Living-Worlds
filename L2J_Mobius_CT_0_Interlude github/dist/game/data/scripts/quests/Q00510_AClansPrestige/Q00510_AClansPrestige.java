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
package quests.Q00510_AClansPrestige;

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

public class Q00510_AClansPrestige extends Quest
{
	// NPC
	private static final int VALDIS = 31331;
	
	// Quest Item
	private static final int CLAW = 8767;
	
	// Reward
	private static final int CLAN_POINTS_REWARD = 50; // Quantity of points
	
	public Q00510_AClansPrestige()
	{
		super(510, "A Clan's Reputation");
		registerQuestItems(CLAW);
		addStartNpc(VALDIS);
		addTalkId(VALDIS);
		addKillId(22215, 22216, 22217);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("31331-3.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("31331-6.htm"))
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
				if (!player.isClanLeader())
				{
					st.exitQuest(true);
					htmltext = "31331-0.htm";
				}
				else if (player.getClan().getLevel() < 5)
				{
					st.exitQuest(true);
					htmltext = "31331-0.htm";
				}
				else
				{
					htmltext = "31331-1.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1))
				{
					final int count = getQuestItemsCount(player, CLAW);
					if (count > 0)
					{
						final int reward = (CLAN_POINTS_REWARD * count);
						takeItems(player, CLAW, -1);
						final Clan clan = player.getClan();
						clan.addReputationScore(reward);
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_SUCCESSFULLY_COMPLETED_A_CLAN_QUEST_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE).addInt(reward));
						clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
						htmltext = "31331-7.htm";
					}
					else
					{
						htmltext = "31331-4.htm";
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
			giveItems(clanLeader, CLAW, 1);
			playSound(clanLeader, QuestSound.ITEMSOUND_QUEST_MIDDLE);
		}
	}
}
