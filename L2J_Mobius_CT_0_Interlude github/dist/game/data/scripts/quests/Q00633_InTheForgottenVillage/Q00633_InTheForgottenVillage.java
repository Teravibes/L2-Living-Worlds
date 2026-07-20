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
package quests.Q00633_InTheForgottenVillage;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00633_InTheForgottenVillage extends Quest
{
	// NPCs
	private static final int MINA = 31388;
	
	// Items
	private static final int RIB_BONE = 7544;
	private static final int ZOMBIE_LIVER = 7545;
	
	// Monsters / Drop chances
	private static final Map<Integer, Integer> MOBS = new HashMap<>();
	static
	{
		MOBS.put(21557, 328000); // Bone Snatcher
		MOBS.put(21558, 328000); // Bone Snatcher
		MOBS.put(21559, 337000); // Bone Maker
		MOBS.put(21560, 337000); // Bone Shaper
		MOBS.put(21563, 342000); // Bone Collector
		MOBS.put(21564, 348000); // Skull Collector
		MOBS.put(21565, 351000); // Bone Animator
		MOBS.put(21566, 359000); // Skull Animator
		MOBS.put(21567, 359000); // Bone Slayer
		MOBS.put(21572, 365000); // Bone Sweeper
		MOBS.put(21574, 383000); // Bone Grinder
		MOBS.put(21575, 383000); // Bone Grinder
		MOBS.put(21580, 385000); // Bone Caster
		MOBS.put(21581, 395000); // Bone Puppeteer
		MOBS.put(21583, 397000); // Bone Scavenger
		MOBS.put(21584, 401000); // Bone Scavenger
	}
	private static final Map<Integer, Integer> UNDEADS = new HashMap<>();
	static
	{
		UNDEADS.put(21553, 347000); // Trampled Man
		UNDEADS.put(21554, 347000); // Trampled Man
		UNDEADS.put(21561, 450000); // Sacrificed Man
		UNDEADS.put(21578, 501000); // Behemoth Zombie
		UNDEADS.put(21596, 359000); // Requiem Lord
		UNDEADS.put(21597, 370000); // Requiem Behemoth
		UNDEADS.put(21598, 441000); // Requiem Behemoth
		UNDEADS.put(21599, 395000); // Requiem Priest
		UNDEADS.put(21600, 408000); // Requiem Behemoth
		UNDEADS.put(21601, 411000); // Requiem Behemoth
	}
	
	public Q00633_InTheForgottenVillage()
	{
		super(633, "In the Forgotten Village");
		registerQuestItems(RIB_BONE, ZOMBIE_LIVER);
		addStartNpc(MINA);
		addTalkId(MINA);
		addKillId(MOBS.keySet());
		addKillId(UNDEADS.keySet());
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
			case "31388-04.htm":
			{
				st.startQuest();
				break;
			}
			case "31388-10.htm":
			{
				takeItems(player, RIB_BONE, -1);
				st.exitQuest(true, true);
				break;
			}
			case "31388-09.htm":
			{
				if (getQuestItemsCount(player, RIB_BONE) >= 200)
				{
					htmltext = "31388-08.htm";
					takeItems(player, RIB_BONE, 200);
					giveAdena(player, 25000, true);
					addExpAndSp(player, 305235, 0);
				}
				
				st.setCond(1, true);
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
				htmltext = (player.getLevel() < 65) ? "31388-03.htm" : "31388-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				if (cond == 1)
				{
					htmltext = "31388-06.htm";
				}
				else if (cond == 2)
				{
					htmltext = "31388-05.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final int npcId = npc.getId();
		if (UNDEADS.containsKey(npcId))
		{
			final QuestState st = getRandomPartyMemberState(player, -1, 3, npc);
			if ((st == null) || !st.isStarted())
			{
				return;
			}
			
			if (getRandom(1000000) < UNDEADS.get(npcId))
			{
				final Player partyMember = st.getPlayer();
				giveItems(partyMember, ZOMBIE_LIVER, 1);
				playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		else if (MOBS.containsKey(npcId))
		{
			final Player partyMember = getRandomPartyMember(player, 1);
			if (partyMember == null)
			{
				return;
			}
			
			final QuestState st = getQuestState(partyMember, false);
			if (getRandom(1000000) < MOBS.get(npcId))
			{
				giveItems(partyMember, RIB_BONE, 1);
				if (getQuestItemsCount(partyMember, RIB_BONE) < 200)
				{
					playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				else
				{
					st.setCond(2, true);
				}
			}
		}
	}
}
