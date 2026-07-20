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
package quests.Q00035_FindGlitteringJewelry;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00037_MakeFormalWear.Q00037_MakeFormalWear;

public class Q00035_FindGlitteringJewelry extends Quest
{
	// NPCs
	private static final int ELLIE = 30091;
	private static final int FELTON = 30879;
	
	// Items
	private static final int ROUGH_JEWEL = 7162;
	private static final int ORIHARUKON = 1893;
	private static final int SILVER_NUGGET = 1873;
	private static final int THONS = 4044;
	
	// Reward
	private static final int JEWEL_BOX = 7077;
	
	public Q00035_FindGlitteringJewelry()
	{
		super(35, "Find Glittering Jewelry");
		registerQuestItems(ROUGH_JEWEL);
		addStartNpc(ELLIE);
		addTalkId(ELLIE, FELTON);
		addKillId(20135); // Alligator
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
			case "30091-1.htm":
			{
				st.startQuest();
				break;
			}
			case "30879-1.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "30091-3.htm":
			{
				st.setCond(4, true);
				takeItems(player, ROUGH_JEWEL, 10);
				break;
			}
			case "30091-5.htm":
			{
				if ((getQuestItemsCount(player, ORIHARUKON) >= 5) && (getQuestItemsCount(player, SILVER_NUGGET) >= 500) && (getQuestItemsCount(player, THONS) >= 150))
				{
					takeItems(player, ORIHARUKON, 5);
					takeItems(player, SILVER_NUGGET, 500);
					takeItems(player, THONS, 150);
					giveItems(player, JEWEL_BOX, 1);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "30091-4a.htm";
				}
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
				if (player.getLevel() >= 60)
				{
					final QuestState fwear = player.getQuestState(Q00037_MakeFormalWear.class.getSimpleName());
					if ((fwear != null) && fwear.isCond(6))
					{
						htmltext = "30091-0.htm";
					}
					else
					{
						htmltext = "30091-0a.htm";
					}
				}
				else
				{
					htmltext = "30091-0b.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case ELLIE:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "30091-1a.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30091-2.htm";
						}
						else if (cond == 4)
						{
							htmltext = ((getQuestItemsCount(player, ORIHARUKON) >= 5) && (getQuestItemsCount(player, SILVER_NUGGET) >= 500) && (getQuestItemsCount(player, THONS) >= 150)) ? "30091-4.htm" : "30091-4a.htm";
						}
						break;
					}
					case FELTON:
					{
						if (cond == 1)
						{
							htmltext = "30879-0.htm";
						}
						else if (cond > 1)
						{
							htmltext = "30879-1a.htm";
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
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player member = getRandomPartyMember(player, 2);
		if ((member != null) && getRandomBoolean())
		{
			giveItems(member, ROUGH_JEWEL, 1);
			if (getQuestItemsCount(member, ROUGH_JEWEL) >= 10)
			{
				getQuestState(member, false).setCond(3, true);
			}
			else
			{
				playSound(member, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
}
