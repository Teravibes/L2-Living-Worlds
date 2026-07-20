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
package quests.Q00325_GrimCollector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00325_GrimCollector extends Quest
{
	// NPCs
	private static final int CURTIS = 30336;
	private static final int VARSAK = 30342;
	private static final int SAMED = 30434;
	
	// Items
	private static final int ANATOMY_DIAGRAM = 1349;
	private static final int ZOMBIE_HEAD = 1350;
	private static final int ZOMBIE_HEART = 1351;
	private static final int ZOMBIE_LIVER = 1352;
	private static final int SKULL = 1353;
	private static final int RIB_BONE = 1354;
	private static final int SPINE = 1355;
	private static final int ARM_BONE = 1356;
	private static final int THIGH_BONE = 1357;
	private static final int COMPLETE_SKELETON = 1358;
	
	// Drops
	private static final Map<Integer, List<ItemHolder>> DROPLIST = new HashMap<>();
	static
	{
		DROPLIST.put(20026, Arrays.asList(new ItemHolder(ZOMBIE_HEAD, 30), new ItemHolder(ZOMBIE_HEART, 50), new ItemHolder(ZOMBIE_LIVER, 75)));
		DROPLIST.put(20029, Arrays.asList(new ItemHolder(ZOMBIE_HEAD, 30), new ItemHolder(ZOMBIE_HEART, 52), new ItemHolder(ZOMBIE_LIVER, 75)));
		DROPLIST.put(20035, Arrays.asList(new ItemHolder(SKULL, 5), new ItemHolder(RIB_BONE, 15), new ItemHolder(SPINE, 29), new ItemHolder(THIGH_BONE, 79)));
		DROPLIST.put(20042, Arrays.asList(new ItemHolder(SKULL, 6), new ItemHolder(RIB_BONE, 19), new ItemHolder(ARM_BONE, 69), new ItemHolder(THIGH_BONE, 86)));
		DROPLIST.put(20045, Arrays.asList(new ItemHolder(SKULL, 9), new ItemHolder(SPINE, 59), new ItemHolder(ARM_BONE, 77), new ItemHolder(THIGH_BONE, 97)));
		DROPLIST.put(20051, Arrays.asList(new ItemHolder(SKULL, 9), new ItemHolder(RIB_BONE, 59), new ItemHolder(SPINE, 79), new ItemHolder(ARM_BONE, 100)));
		DROPLIST.put(20457, Arrays.asList(new ItemHolder(ZOMBIE_HEAD, 40), new ItemHolder(ZOMBIE_HEART, 60), new ItemHolder(ZOMBIE_LIVER, 80)));
		DROPLIST.put(20458, Arrays.asList(new ItemHolder(ZOMBIE_HEAD, 40), new ItemHolder(ZOMBIE_HEART, 70), new ItemHolder(ZOMBIE_LIVER, 100)));
		DROPLIST.put(20514, Arrays.asList(new ItemHolder(SKULL, 6), new ItemHolder(RIB_BONE, 21), new ItemHolder(SPINE, 30), new ItemHolder(ARM_BONE, 31), new ItemHolder(THIGH_BONE, 64)));
		DROPLIST.put(20515, Arrays.asList(new ItemHolder(SKULL, 5), new ItemHolder(RIB_BONE, 20), new ItemHolder(SPINE, 31), new ItemHolder(ARM_BONE, 33), new ItemHolder(THIGH_BONE, 69)));
	}
	
	public Q00325_GrimCollector()
	{
		super(325, "Grim Collector");
		registerQuestItems(ZOMBIE_HEAD, ZOMBIE_HEART, ZOMBIE_LIVER, SKULL, RIB_BONE, SPINE, ARM_BONE, THIGH_BONE, COMPLETE_SKELETON, ANATOMY_DIAGRAM);
		addStartNpc(CURTIS);
		addTalkId(CURTIS, VARSAK, SAMED);
		addKillId(DROPLIST.keySet());
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
			case "30336-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30434-03.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, ANATOMY_DIAGRAM, 1);
				break;
			}
			case "30434-06.htm":
			{
				takeItems(player, ANATOMY_DIAGRAM, -1);
				payback(player);
				st.exitQuest(true, true);
				break;
			}
			case "30434-07.htm":
			{
				payback(player);
				break;
			}
			case "30434-09.htm":
			{
				final int skeletons = getQuestItemsCount(player, COMPLETE_SKELETON);
				if (skeletons > 0)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					takeItems(player, COMPLETE_SKELETON, -1);
					giveAdena(player, 543 + (341 * skeletons), true);
				}
				break;
			}
			case "30342-03.htm":
			{
				if (!hasQuestItems(player, SPINE, ARM_BONE, SKULL, RIB_BONE, THIGH_BONE))
				{
					htmltext = "30342-02.htm";
				}
				else
				{
					takeItems(player, SPINE, 1);
					takeItems(player, SKULL, 1);
					takeItems(player, ARM_BONE, 1);
					takeItems(player, RIB_BONE, 1);
					takeItems(player, THIGH_BONE, 1);
					
					if (getRandom(10) < 9)
					{
						giveItems(player, COMPLETE_SKELETON, 1);
					}
					else
					{
						htmltext = "30342-04.htm";
					}
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
				htmltext = (player.getLevel() < 15) ? "30336-01.htm" : "30336-02.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case CURTIS:
					{
						htmltext = (!hasQuestItems(player, ANATOMY_DIAGRAM)) ? "30336-04.htm" : "30336-05.htm";
						break;
					}
					case SAMED:
					{
						if (!hasQuestItems(player, ANATOMY_DIAGRAM))
						{
							htmltext = "30434-01.htm";
						}
						else
						{
							if (getNumberOfPieces(player) == 0)
							{
								htmltext = "30434-04.htm";
							}
							else
							{
								htmltext = !hasQuestItems(player, COMPLETE_SKELETON) ? "30434-05.htm" : "30434-08.htm";
							}
						}
						break;
					}
					case VARSAK:
					{
						htmltext = "30342-01.htm";
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
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		if (hasQuestItems(player, ANATOMY_DIAGRAM))
		{
			final int chance = getRandom(100);
			for (ItemHolder drop : DROPLIST.get(npc.getId()))
			{
				if (chance < drop.getCount())
				{
					giveItems(player, drop.getId(), 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					break;
				}
			}
		}
	}
	
	private static int getNumberOfPieces(Player player)
	{
		return getQuestItemsCount(player, ZOMBIE_HEAD) + getQuestItemsCount(player, SPINE) + getQuestItemsCount(player, ARM_BONE) + getQuestItemsCount(player, ZOMBIE_HEART) + getQuestItemsCount(player, ZOMBIE_LIVER) + getQuestItemsCount(player, SKULL) + getQuestItemsCount(player, RIB_BONE) + getQuestItemsCount(player, THIGH_BONE) + getQuestItemsCount(player, COMPLETE_SKELETON);
	}
	
	private void payback(Player player)
	{
		final int count = getNumberOfPieces(player);
		if (count > 0)
		{
			int reward = (30 * getQuestItemsCount(player, ZOMBIE_HEAD)) + (20 * getQuestItemsCount(player, ZOMBIE_HEART)) + (20 * getQuestItemsCount(player, ZOMBIE_LIVER)) + (100 * getQuestItemsCount(player, SKULL)) + (40 * getQuestItemsCount(player, RIB_BONE)) + (14 * getQuestItemsCount(player, SPINE)) + (14 * getQuestItemsCount(player, ARM_BONE)) + (14 * getQuestItemsCount(player, THIGH_BONE)) + (341 * getQuestItemsCount(player, COMPLETE_SKELETON));
			if (count > 10)
			{
				reward += 1629;
			}
			
			if (hasQuestItems(player, COMPLETE_SKELETON))
			{
				reward += 543;
			}
			
			takeItems(player, ZOMBIE_HEAD, -1);
			takeItems(player, ZOMBIE_HEART, -1);
			takeItems(player, ZOMBIE_LIVER, -1);
			takeItems(player, SKULL, -1);
			takeItems(player, RIB_BONE, -1);
			takeItems(player, SPINE, -1);
			takeItems(player, ARM_BONE, -1);
			takeItems(player, THIGH_BONE, -1);
			takeItems(player, COMPLETE_SKELETON, -1);
			
			giveAdena(player, reward, true);
		}
	}
}
