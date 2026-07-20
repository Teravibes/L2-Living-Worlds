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
package quests.Q00636_TruthBeyondTheGate;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * @author Mobius, Skache
 * @note Based on python script
 */
public class Q00636_TruthBeyondTheGate extends Quest
{
	// NPCs
	private static final int ELIYAH = 31329;
	private static final int FLAURON = 32010;
	
	// Item
	private static final int VISITOR_MARK = 8064;
	private static final int FADED_VISITOR_MARK = 8065;
	
	public Q00636_TruthBeyondTheGate()
	{
		super(636, "Truth Beyond the Gate");
		addStartNpc(ELIYAH);
		addTalkId(ELIYAH, FLAURON);
		addEnterZoneId(30100);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState qs = player.getQuestState(getName());
		if (qs == null)
		{
			return htmltext;
		}
		
		if (event.equals("31329-04.htm"))
		{
			qs.startQuest();
		}
		else if (event.equals("32010-02.htm"))
		{
			giveItems(player, VISITOR_MARK, 1);
			qs.unset("cond");
			qs.exitQuest(true, true);
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		
		final int npcId = npc.getId();
		final int id = qs.getState();
		final int cond = qs.getCond();
		if ((cond == 0) && (id == State.CREATED))
		{
			if (npcId == ELIYAH)
			{
				if (player.getLevel() > 72)
				{
					htmltext = "31329-02.htm";
				}
				else
				{
					htmltext = "31329-01.htm";
					qs.exitQuest(true);
				}
			}
		}
		else if (id == State.STARTED)
		{
			if (npcId == ELIYAH)
			{
				htmltext = "31329-05.htm";
			}
			else if (npcId == FLAURON)
			{
				if (cond == 1)
				{
					htmltext = "32010-01.htm";
					qs.setCond(2);
				}
				else
				{
					htmltext = "32010-03.htm";
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			
			if (player.getInventory().getItemByItemId(VISITOR_MARK) != null)
			{
				if (player.destroyItemByItemId(ItemProcessType.QUEST, VISITOR_MARK, 1, player, false))
				{
					giveItems(player, FADED_VISITOR_MARK, 1);
				}
			}
		}
	}
}
