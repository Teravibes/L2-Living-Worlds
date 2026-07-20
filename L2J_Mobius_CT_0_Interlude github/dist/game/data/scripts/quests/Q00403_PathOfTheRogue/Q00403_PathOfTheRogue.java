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
package quests.Q00403_PathOfTheRogue;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00403_PathOfTheRogue extends Quest
{
	// NPCs
	private static final int BEZIQUE = 30379;
	private static final int NETI = 30425;
	
	// Items
	private static final int BEZIQUE_LETTER = 1180;
	private static final int NETI_BOW = 1181;
	private static final int NETI_DAGGER = 1182;
	private static final int SPARTOI_BONES = 1183;
	private static final int HORSESHOE_OF_LIGHT = 1184;
	private static final int MOST_WANTED_LIST = 1185;
	private static final int STOLEN_JEWELRY = 1186;
	private static final int STOLEN_TOMES = 1187;
	private static final int STOLEN_RING = 1188;
	private static final int STOLEN_NECKLACE = 1189;
	private static final int BEZIQUE_RECOMMENDATION = 1190;
	
	public Q00403_PathOfTheRogue()
	{
		super(403, "Path to a Rogue");
		registerQuestItems(BEZIQUE_LETTER, NETI_BOW, NETI_DAGGER, SPARTOI_BONES, HORSESHOE_OF_LIGHT, MOST_WANTED_LIST, STOLEN_JEWELRY, STOLEN_TOMES, STOLEN_RING, STOLEN_NECKLACE);
		addStartNpc(BEZIQUE);
		addTalkId(BEZIQUE, NETI);
		addKillId(20035, 20042, 20045, 20051, 20054, 20060, 27038);
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
			case "30379-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.ROGUE) ? "30379-02a.htm" : "30379-02.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30379-02.htm";
				}
				else if (hasQuestItems(player, BEZIQUE_RECOMMENDATION))
				{
					htmltext = "30379-04.htm";
				}
				break;
			}
			case "30379-06.htm":
			{
				st.startQuest();
				giveItems(player, BEZIQUE_LETTER, 1);
				break;
			}
			case "30425-05.htm":
			{
				st.setCond(2, true);
				giveItems(player, NETI_BOW, 1);
				giveItems(player, NETI_DAGGER, 1);
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
				htmltext = "30379-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case BEZIQUE:
					{
						if (cond == 1)
						{
							htmltext = "30379-07.htm";
						}
						else if ((cond == 2) || (cond == 3))
						{
							htmltext = "30379-10.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30379-08.htm";
							st.setCond(5, true);
							takeItems(player, HORSESHOE_OF_LIGHT, 1);
							giveItems(player, MOST_WANTED_LIST, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30379-11.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30379-09.htm";
							takeItems(player, NETI_BOW, 1);
							takeItems(player, NETI_DAGGER, 1);
							takeItems(player, STOLEN_JEWELRY, 1);
							takeItems(player, STOLEN_NECKLACE, 1);
							takeItems(player, STOLEN_RING, 1);
							takeItems(player, STOLEN_TOMES, 1);
							giveItems(player, BEZIQUE_RECOMMENDATION, 1);
							addExpAndSp(player, 3200, 1500);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(true, true);
						}
						break;
					}
					case NETI:
					{
						if (cond == 1)
						{
							htmltext = "30425-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30425-06.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30425-07.htm";
							st.setCond(4, true);
							takeItems(player, SPARTOI_BONES, 10);
							giveItems(player, HORSESHOE_OF_LIGHT, 1);
						}
						else if (cond > 3)
						{
							htmltext = "30425-08.htm";
						}
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
		
		final int equippedItemId = player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND);
		if ((equippedItemId != NETI_BOW) && (equippedItemId != NETI_DAGGER))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case 20035:
			case 20045:
			case 20051:
			{
				if (st.isCond(2) && (getRandom(10) < 2))
				{
					if (getQuestItemsCount(player, SPARTOI_BONES) < 10)
					{
						giveItems(player, SPARTOI_BONES, 1);
						
						if (getQuestItemsCount(player, SPARTOI_BONES) >= 10)
						{
							st.setCond(3, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case 20042:
			{
				if (st.isCond(2) && (getRandom(10) < 3))
				{
					if (getQuestItemsCount(player, SPARTOI_BONES) < 10)
					{
						giveItems(player, SPARTOI_BONES, 1);
						
						if (getQuestItemsCount(player, SPARTOI_BONES) >= 10)
						{
							st.setCond(3, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case 20054:
			case 20060:
			{
				if (st.isCond(2) && (getRandom(10) < 8))
				{
					if (getQuestItemsCount(player, SPARTOI_BONES) < 10)
					{
						giveItems(player, SPARTOI_BONES, 1);
						
						if (getQuestItemsCount(player, SPARTOI_BONES) >= 10)
						{
							st.setCond(3, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
			case 27038:
			{
				if (st.isCond(5))
				{
					final int randomItem = getRandom(STOLEN_JEWELRY, STOLEN_NECKLACE);
					if (!hasQuestItems(player, randomItem))
					{
						giveItems(player, randomItem, 1);
						if (hasQuestItems(player, STOLEN_JEWELRY, STOLEN_TOMES, STOLEN_RING, STOLEN_NECKLACE))
						{
							st.setCond(6, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
				}
				break;
			}
		}
	}
}
