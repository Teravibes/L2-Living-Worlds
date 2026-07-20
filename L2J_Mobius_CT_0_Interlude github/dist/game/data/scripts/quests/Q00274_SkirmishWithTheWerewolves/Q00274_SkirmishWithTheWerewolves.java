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
package quests.Q00274_SkirmishWithTheWerewolves;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00274_SkirmishWithTheWerewolves extends Quest
{
	// Needed items
	private static final int NECKLACE_OF_VALOR = 1507;
	private static final int NECKLACE_OF_COURAGE = 1506;
	
	// Items
	private static final int MARAKU_WEREWOLF_HEAD = 1477;
	private static final int MARAKU_WOLFMEN_TOTEM = 1501;
	
	public Q00274_SkirmishWithTheWerewolves()
	{
		super(274, "Skirmish with the Werewolves");
		registerQuestItems(MARAKU_WEREWOLF_HEAD, MARAKU_WOLFMEN_TOTEM);
		addStartNpc(30569);
		addTalkId(30569);
		addKillId(20363, 20364);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		final String htmltext = event;
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30569-03.htm"))
		{
			st.startQuest();
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
				if (player.getRace() != Race.ORC)
				{
					htmltext = "30569-00.htm";
				}
				else if (player.getLevel() < 9)
				{
					htmltext = "30569-01.htm";
				}
				else if (hasAtLeastOneQuestItem(player, NECKLACE_OF_COURAGE, NECKLACE_OF_VALOR))
				{
					htmltext = "30569-02.htm";
				}
				else
				{
					htmltext = "30569-07.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1))
				{
					htmltext = "30569-04.htm";
				}
				else
				{
					htmltext = "30569-05.htm";
					
					final int amount = 3500 + (getQuestItemsCount(player, MARAKU_WOLFMEN_TOTEM) * 600);
					takeItems(player, MARAKU_WEREWOLF_HEAD, -1);
					takeItems(player, MARAKU_WOLFMEN_TOTEM, -1);
					giveAdena(player, amount, true);
					st.exitQuest(true, true);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		giveItems(player, MARAKU_WEREWOLF_HEAD, 1);
		if (getQuestItemsCount(player, MARAKU_WEREWOLF_HEAD) < 40)
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		else
		{
			st.setCond(2, true);
		}
		
		if (getRandom(100) < 6)
		{
			giveItems(player, MARAKU_WOLFMEN_TOTEM, 1);
		}
	}
}
