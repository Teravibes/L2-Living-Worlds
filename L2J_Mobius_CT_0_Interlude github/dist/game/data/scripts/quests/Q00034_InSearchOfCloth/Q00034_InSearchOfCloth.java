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
package quests.Q00034_InSearchOfCloth;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00037_MakeFormalWear.Q00037_MakeFormalWear;

public class Q00034_InSearchOfCloth extends Quest
{
	// NPCs
	private static final int RADIA = 30088;
	private static final int RALFORD = 30165;
	private static final int VARAN = 30294;
	
	// Monsters
	private static final int TRISALIM_SPIDER = 20560;
	private static final int TRISALIM_TARANTULA = 20561;
	
	// Items
	private static final int SPINNERET = 7528;
	private static final int SUEDE = 1866;
	private static final int THREAD = 1868;
	private static final int SPIDERSILK = 7161;
	
	// Rewards
	private static final int MYSTERIOUS_CLOTH = 7076;
	
	public Q00034_InSearchOfCloth()
	{
		super(34, "In Search of Cloth");
		registerQuestItems(SPINNERET, SPIDERSILK);
		addStartNpc(RADIA);
		addTalkId(RADIA, RALFORD, VARAN);
		addKillId(TRISALIM_SPIDER, TRISALIM_TARANTULA);
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
			case "30088-1.htm":
			{
				st.startQuest();
				break;
			}
			case "30294-1.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "30088-3.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "30165-1.htm":
			{
				st.setCond(4, true);
				break;
			}
			case "30165-3.htm":
			{
				st.setCond(6, true);
				takeItems(player, SPINNERET, 10);
				giveItems(player, SPIDERSILK, 1);
				break;
			}
			case "30088-5.htm":
			{
				if ((getQuestItemsCount(player, SUEDE) >= 3000) && (getQuestItemsCount(player, THREAD) >= 5000) && hasQuestItems(player, SPIDERSILK))
				{
					takeItems(player, SPIDERSILK, 1);
					takeItems(player, SUEDE, 3000);
					takeItems(player, THREAD, 5000);
					giveItems(player, MYSTERIOUS_CLOTH, 1);
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = "30088-4a.htm";
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
						htmltext = "30088-0.htm";
					}
					else
					{
						htmltext = "30088-0a.htm";
					}
				}
				else
				{
					htmltext = "30088-0b.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case RADIA:
					{
						if (cond == 1)
						{
							htmltext = "30088-1a.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30088-2.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30088-3a.htm";
						}
						else if (cond == 6)
						{
							if ((getQuestItemsCount(player, SUEDE) < 3000) || (getQuestItemsCount(player, THREAD) < 5000) || !hasQuestItems(player, SPIDERSILK))
							{
								htmltext = "30088-4a.htm";
							}
							else
							{
								htmltext = "30088-4.htm";
							}
						}
						break;
					}
					case VARAN:
					{
						if (cond == 1)
						{
							htmltext = "30294-0.htm";
						}
						else if (cond > 1)
						{
							htmltext = "30294-1a.htm";
						}
						break;
					}
					case RALFORD:
					{
						if (cond == 3)
						{
							htmltext = "30165-0.htm";
						}
						else if ((cond == 4) && (getQuestItemsCount(player, SPINNERET) < 10))
						{
							htmltext = "30165-1a.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30165-2.htm";
						}
						else if (cond > 5)
						{
							htmltext = "30165-3a.htm";
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
		final Player member = getRandomPartyMember(player, 4);
		if ((member != null) && getRandomBoolean())
		{
			final QuestState qs = getQuestState(member, false);
			giveItems(member, SPINNERET, 1);
			if (getQuestItemsCount(member, SPINNERET) >= 10)
			{
				qs.setCond(5, true);
			}
			else
			{
				playSound(member, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
}
