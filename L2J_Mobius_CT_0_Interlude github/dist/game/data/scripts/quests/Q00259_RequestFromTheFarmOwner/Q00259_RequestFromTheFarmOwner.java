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
package quests.Q00259_RequestFromTheFarmOwner;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00259_RequestFromTheFarmOwner extends Quest
{
	// NPCs
	private static final int EDMOND = 30497;
	private static final int MARIUS = 30405;
	
	// Monsters
	private static final int GIANT_SPIDER = 20103;
	private static final int TALON_SPIDER = 20106;
	private static final int BLADE_SPIDER = 20108;
	
	// Items
	private static final int GIANT_SPIDER_SKIN = 1495;
	
	// Rewards
	private static final int ADENA = 57;
	private static final int HEALING_POTION = 1061;
	private static final int WOODEN_ARROW = 17;
	
	public Q00259_RequestFromTheFarmOwner()
	{
		super(259, "Rancher's Plea");
		registerQuestItems(GIANT_SPIDER_SKIN);
		addStartNpc(EDMOND);
		addTalkId(EDMOND, MARIUS);
		addKillId(GIANT_SPIDER, TALON_SPIDER, BLADE_SPIDER);
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
			case "30497-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30497-06.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30405-04.htm":
			{
				if (getQuestItemsCount(player, GIANT_SPIDER_SKIN) >= 10)
				{
					takeItems(player, GIANT_SPIDER_SKIN, 10);
					rewardItems(player, HEALING_POTION, 1);
				}
				else
				{
					htmltext = "<html><body>Incorrect item count</body></html>";
				}
				break;
			}
			case "30405-05.htm":
			{
				if (getQuestItemsCount(player, GIANT_SPIDER_SKIN) >= 10)
				{
					takeItems(player, GIANT_SPIDER_SKIN, 10);
					rewardItems(player, WOODEN_ARROW, 50);
				}
				else
				{
					htmltext = "<html><body>Incorrect item count</body></html>";
				}
				break;
			}
			case "30405-07.htm":
			{
				if (getQuestItemsCount(player, GIANT_SPIDER_SKIN) >= 10)
				{
					htmltext = "30405-06.htm";
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
				htmltext = (player.getLevel() < 15) ? "30497-01.htm" : "30497-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int count = getQuestItemsCount(player, GIANT_SPIDER_SKIN);
				switch (npc.getId())
				{
					case EDMOND:
					{
						if (count == 0)
						{
							htmltext = "30497-04.htm";
						}
						else
						{
							htmltext = "30497-05.htm";
							takeItems(player, GIANT_SPIDER_SKIN, -1);
							int reward = count * 25;
							if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD && (count >= 10))
							{
								reward += 250;
							}
							
							rewardItems(player, ADENA, reward);
						}
						break;
					}
					case MARIUS:
					{
						htmltext = (count < 10) ? "30405-01.htm" : "30405-02.htm";
						break;
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
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		giveItems(player, GIANT_SPIDER_SKIN, 1);
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
	}
}
