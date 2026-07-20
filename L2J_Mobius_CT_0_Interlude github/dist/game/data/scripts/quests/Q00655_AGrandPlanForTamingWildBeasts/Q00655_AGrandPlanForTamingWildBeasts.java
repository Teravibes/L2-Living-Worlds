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
package quests.Q00655_AGrandPlanForTamingWildBeasts;

import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.siege.clanhalls.SiegableHall;

/**
 * @author LordWinter
 */
public class Q00655_AGrandPlanForTamingWildBeasts extends Quest
{
	private static final int MESSENGER = 35627;
	
	private static final int STONE = 8084;
	private static final int TRAINER_LICENSE = 8293;
	
	private static final SiegableHall BEAST_STRONGHOLD = CHSiegeManager.getInstance().getSiegableHall(63);
	
	public Q00655_AGrandPlanForTamingWildBeasts()
	{
		super(655, "A Grand Plan for Taming Wild Beasts");
		
		addStartNpc(MESSENGER);
		addTalkId(MESSENGER);
	}
	
	@Override
	public final String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (BEAST_STRONGHOLD.getSiege().getAttackers().size() >= 5)
				{
					htmltext = "35627-00.htm";
				}
				else
				{
					htmltext = "35627-01.htm";
					st.startQuest();
				}
				break;
			}
			case State.STARTED:
			{
				if (getQuestItemsCount(player, STONE) < 10)
				{
					htmltext = "35627-02.htm";
				}
				else
				{
					takeItems(player, STONE, 10);
					giveItems(player, TRAINER_LICENSE, 1);
					st.exitQuest(true, true);
					htmltext = "35627-03.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	public static void checkCrystalofPurity(Player player)
	{
		final QuestState st = player.getQuestState(Q00655_AGrandPlanForTamingWildBeasts.class.getSimpleName());
		if ((st != null) && st.isCond(1) && (getQuestItemsCount(player, STONE) < 10))
		{
			giveItems(player, STONE, 1);
		}
	}
}
