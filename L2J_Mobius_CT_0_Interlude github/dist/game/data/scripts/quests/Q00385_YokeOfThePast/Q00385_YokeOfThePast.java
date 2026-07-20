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
package quests.Q00385_YokeOfThePast;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00385_YokeOfThePast extends Quest
{
	// NPCs
	private static final int[] GATEKEEPER_ZIGGURAT =
	{
		31095,
		31096,
		31097,
		31098,
		31099,
		31100,
		31101,
		31102,
		31103,
		31104,
		31105,
		31106,
		31107,
		31108,
		31109,
		31110,
		31114,
		31115,
		31116,
		31117,
		31118,
		31119,
		31120,
		31121,
		31122,
		31123,
		31124,
		31125,
		31126
	};
	
	// Item
	private static final int ANCIENT_SCROLL = 5902;
	
	// Reward
	private static final int BLANK_SCROLL = 5965;
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(21208, 70000);
		CHANCES.put(21209, 80000);
		CHANCES.put(21210, 110000);
		CHANCES.put(21211, 110000);
		CHANCES.put(21213, 140000);
		CHANCES.put(21214, 190000);
		CHANCES.put(21215, 190000);
		CHANCES.put(21217, 240000);
		CHANCES.put(21218, 300000);
		CHANCES.put(21219, 300000);
		CHANCES.put(21221, 370000);
		CHANCES.put(21222, 460000);
		CHANCES.put(21223, 450000);
		CHANCES.put(21224, 500000);
		CHANCES.put(21225, 540000);
		CHANCES.put(21226, 660000);
		CHANCES.put(21227, 640000);
		CHANCES.put(21228, 700000);
		CHANCES.put(21229, 750000);
		CHANCES.put(21230, 910000);
		CHANCES.put(21231, 860000);
		CHANCES.put(21236, 120000);
		CHANCES.put(21237, 140000);
		CHANCES.put(21238, 190000);
		CHANCES.put(21239, 190000);
		CHANCES.put(21240, 220000);
		CHANCES.put(21241, 240000);
		CHANCES.put(21242, 300000);
		CHANCES.put(21243, 300000);
		CHANCES.put(21244, 340000);
		CHANCES.put(21245, 370000);
		CHANCES.put(21246, 460000);
		CHANCES.put(21247, 450000);
		CHANCES.put(21248, 500000);
		CHANCES.put(21249, 540000);
		CHANCES.put(21250, 660000);
		CHANCES.put(21251, 640000);
		CHANCES.put(21252, 700000);
		CHANCES.put(21253, 750000);
		CHANCES.put(21254, 910000);
		CHANCES.put(21255, 860000);
	}
	
	public Q00385_YokeOfThePast()
	{
		super(385, "Yoke of the Past");
		registerQuestItems(ANCIENT_SCROLL);
		addStartNpc(GATEKEEPER_ZIGGURAT);
		addTalkId(GATEKEEPER_ZIGGURAT);
		addKillId(CHANCES.keySet());
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
		
		if (event.equals("05.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("10.htm"))
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
				htmltext = (player.getLevel() < 20) ? "02.htm" : "01.htm";
				break;
			}
			case State.STARTED:
			{
				if (!hasQuestItems(player, ANCIENT_SCROLL))
				{
					htmltext = "08.htm";
				}
				else
				{
					htmltext = "09.htm";
					final int count = getQuestItemsCount(player, ANCIENT_SCROLL);
					takeItems(player, ANCIENT_SCROLL, -1);
					rewardItems(player, BLANK_SCROLL, count);
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
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			final Player partyMember = st.getPlayer();
			giveItems(partyMember, ANCIENT_SCROLL, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
