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
package quests.Q00120_PavelsLastResearch;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

import quests.Q00114_ResurrectionOfAnOldManager.Q00114_ResurrectionOfAnOldManager;

/**
 * @author Mobius
 * @note Based on python script
 */
public class Q00120_PavelsLastResearch extends Quest
{
	// NPCs
	private static final int YUMI = 32041;
	private static final int WEATHER1 = 32042; // north
	private static final int WEATHER2 = 32043; // east
	private static final int WEATHER3 = 32044; // west
	private static final int BOOKSHELF = 32045;
	private static final int STONES = 32046;
	private static final int WENDY = 32047;
	
	// Items
	private static final int EAR_BINDING = 854;
	private static final int REPORT = 8058;
	private static final int REPORT2 = 8059;
	private static final int ENIGMA = 8060;
	private static final int FLOWER = 8290;
	private static final int HEART = 8291;
	private static final int NECKLACE = 8292;
	
	public Q00120_PavelsLastResearch()
	{
		super(120, "Pavel's Research");
		addStartNpc(STONES);
		addTalkId(BOOKSHELF, STONES, WEATHER1, WEATHER2, WEATHER3, WENDY, YUMI);
		registerQuestItems(FLOWER, REPORT, REPORT2, ENIGMA, HEART, NECKLACE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "32041-03.htm":
			{
				qs.setCond(3, true);
				break;
			}
			case "32041-04.htm":
			{
				qs.setCond(4, true);
				break;
			}
			case "32041-12.htm":
			{
				qs.setCond(8, true);
				break;
			}
			case "32041-16.htm":
			{
				qs.setCond(16, true);
				giveItems(player, ENIGMA, 1);
				break;
			}
			case "32041-22.htm":
			{
				qs.setCond(17, true);
				takeItems(player, ENIGMA, 1);
				break;
			}
			case "32041-32.htm":
			{
				takeItems(player, NECKLACE, 1);
				giveItems(player, EAR_BINDING, 1);
				qs.exitQuest(true, true);
				break;
			}
			case "32042-06.htm":
			{
				if (qs.isCond(10))
				{
					if ((qs.getInt("talk") + qs.getInt("talk1")) == 2)
					{
						qs.setCond(11, true);
						qs.set("talk", "0");
						qs.set("talk1", "0");
					}
					else
					{
						htmltext = "32042-03.htm";
					}
				}
				break;
			}
			case "32042-10.htm":
			{
				if ((qs.getInt("talk") + qs.getInt("talk1") + qs.getInt("talk2")) == 3)
				{
					htmltext = "32042-14.htm";
				}
				break;
			}
			case "32042-11.htm":
			{
				if (qs.getInt("talk") == 0)
				{
					qs.set("talk", "1");
				}
				break;
			}
			case "32042-12.htm":
			{
				if (qs.getInt("talk1") == 0)
				{
					qs.set("talk1", "1");
				}
				break;
			}
			case "32042-13.htm":
			{
				if (qs.getInt("talk2") == 0)
				{
					qs.set("talk2", "1");
				}
				break;
			}
			case "32042-15.htm":
			{
				qs.setCond(12, true);
				qs.set("talk", "0");
				qs.set("talk1", "0");
				qs.set("talk2", "0");
				break;
			}
			case "32043-06.htm":
			{
				if (qs.isCond(17))
				{
					if ((qs.getInt("talk") + qs.getInt("talk1")) == 2)
					{
						qs.setCond(18, true);
						qs.set("talk", "0");
						qs.set("talk1", "0");
					}
					else
					{
						htmltext = "32043-03.htm";
					}
				}
				break;
			}
			case "32043-15.htm":
			{
				if ((qs.getInt("talk") + qs.getInt("talk1")) == 2)
				{
					htmltext = "32043-29.htm";
				}
				break;
			}
			case "32043-18.htm":
			{
				if (qs.getInt("talk") == 1)
				{
					htmltext = "32043-21.htm";
				}
				break;
			}
			case "32043-20.htm":
			{
				qs.set("talk", "1");
				playSound(player, QuestSound.AMBSOUND_DRONE);
				break;
			}
			case "32043-28.htm":
			{
				qs.set("talk1", "1");
				break;
			}
			case "32043-30.htm":
			{
				qs.setCond(19);
				qs.set("talk", "0");
				qs.set("talk1", "0");
				break;
			}
			case "32044-06.htm":
			{
				if (qs.isCond(20))
				{
					if ((qs.getInt("talk") + qs.getInt("talk1")) == 2)
					{
						qs.setCond(21, true);
						qs.set("talk", "0");
						qs.set("talk1", "0");
					}
					else
					{
						htmltext = "32044-03.htm";
					}
				}
				break;
			}
			case "32044-08.htm":
			{
				if ((qs.getInt("talk") + qs.getInt("talk1")) == 2)
				{
					htmltext = "32044-11.htm";
				}
				break;
			}
			case "32044-09.htm":
			{
				if (qs.getInt("talk") == 0)
				{
					qs.set("talk", "1");
				}
				break;
			}
			case "32044-10.htm":
			{
				if (qs.getInt("talk1") == 0)
				{
					qs.set("talk1", "1");
				}
				break;
			}
			case "32044-17.htm":
			{
				qs.setCond(22, true);
				qs.set("talk", "0");
				qs.set("talk1", "0");
				break;
			}
			case "32045-02.htm":
			{
				qs.setCond(15, true);
				giveItems(player, REPORT2, 1);
				npc.broadcastPacket(new MagicSkillUse(npc, qs.getPlayer(), 5073, 5, 1500, 0));
				break;
			}
			case "32046-04.htm":
			case "32046-05.htm":
			{
				qs.exitQuest(true);
				break;
			}
			case "32046-06.htm":
			{
				if (qs.getPlayer().getLevel() >= 50)
				{
					qs.startQuest();
				}
				else
				{
					htmltext = "32046-00.htm";
					qs.exitQuest(true);
				}
				break;
			}
			case "32046-08.htm":
			{
				qs.setCond(2);
				break;
			}
			case "32046-12.htm":
			{
				qs.setCond(6, true);
				giveItems(player, FLOWER, 1);
				break;
			}
			case "32046-22.htm":
			{
				qs.setCond(10, true);
				break;
			}
			case "32046-29.htm":
			{
				qs.setCond(13, true);
				break;
			}
			case "32046-35.htm":
			{
				qs.setCond(20, true);
				break;
			}
			case "32046-38.htm":
			{
				qs.setCond(23, true);
				giveItems(player, HEART, 1);
				break;
			}
			case "32047-06.htm":
			{
				qs.setCond(5, true);
				break;
			}
			case "32047-10.htm":
			{
				qs.setCond(7, true);
				takeItems(player, FLOWER, 1);
				break;
			}
			case "32047-15.htm":
			{
				qs.setCond(9, true);
				break;
			}
			case "32047-18.htm":
			{
				qs.setCond(14, true);
				break;
			}
			case "32047-26.htm":
			{
				qs.setCond(24, true);
				takeItems(player, HEART, 1);
				break;
			}
			case "32047-32.htm":
			{
				qs.setCond(25, true);
				giveItems(player, NECKLACE, 1);
				break;
			}
			case "w1_1":
			{
				qs.set("talk", "1");
				htmltext = "32042-04.htm";
				break;
			}
			case "w1_2":
			{
				qs.set("talk1", "1");
				htmltext = "32042-05.htm";
				break;
			}
			case "w2_1":
			{
				qs.set("talk", "1");
				htmltext = "32043-04.htm";
				break;
			}
			case "w2_2":
			{
				qs.set("talk1", "1");
				htmltext = "32043-05.htm";
				break;
			}
			case "w3_1":
			{
				qs.set("talk", "1");
				htmltext = "32044-04.htm";
				break;
			}
			case "w3_2":
			{
				qs.set("talk1", "1");
				htmltext = "32044-05.htm";
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState qs = getQuestState(player, true);
		if (qs == null)
		{
			return htmltext;
		}
		
		final int state = qs.getState();
		final int npcId = npc.getId();
		final int cond = qs.getCond();
		if (state == State.COMPLETED)
		{
			htmltext = getAlreadyCompletedMsg(player);
		}
		else if (npcId == STONES)
		{
			if (state == State.CREATED)
			{
				final QuestState qs2 = player.getQuestState(Q00114_ResurrectionOfAnOldManager.class.getSimpleName());
				if (qs2 != null)
				{
					if ((player.getLevel() >= 49) && (qs2.getState() == State.COMPLETED))
					{
						htmltext = "32046-01.htm";
					}
					else
					{
						htmltext = "32046-00.htm";
						qs.exitQuest(true);
					}
				}
				else
				{
					htmltext = "32046-00.htm";
					qs.exitQuest(true);
				}
			}
			else if (cond == 1)
			{
				htmltext = "32046-06.htm";
			}
			else if (cond == 2)
			{
				htmltext = "32046-09.htm";
			}
			else if (cond == 5)
			{
				htmltext = "32046-10.htm";
			}
			else if (cond == 6)
			{
				htmltext = "32046-13.htm";
			}
			else if (cond == 9)
			{
				htmltext = "32046-14.htm";
			}
			else if (cond == 10)
			{
				htmltext = "32046-23.htm";
			}
			else if (cond == 12)
			{
				htmltext = "32046-26.htm";
			}
			else if (cond == 13)
			{
				htmltext = "32046-30.htm";
			}
			else if (cond == 19)
			{
				htmltext = "32046-31.htm";
			}
			else if (cond == 20)
			{
				htmltext = "32046-36.htm";
			}
			else if (cond == 22)
			{
				htmltext = "32046-37.htm";
			}
			else if (cond == 23)
			{
				htmltext = "32046-39.htm";
			}
		}
		else if (npcId == WENDY)
		{
			if ((cond >= 2) && (cond <= 4))
			{
				htmltext = "32047-01.htm";
			}
			else if (cond == 5)
			{
				htmltext = "32047-07.htm";
			}
			else if (cond == 6)
			{
				htmltext = "32047-08.htm";
			}
			else if (cond == 7)
			{
				htmltext = "32047-11.htm";
			}
			else if (cond == 8)
			{
				htmltext = "32047-12.htm";
			}
			else if (cond == 9)
			{
				htmltext = "32047-15.htm";
			}
			else if (cond == 13)
			{
				htmltext = "32047-16.htm";
			}
			else if (cond == 14)
			{
				htmltext = "32047-19.htm";
			}
			else if (cond == 15)
			{
				htmltext = "32047-20.htm";
			}
			else if (cond == 23)
			{
				htmltext = "32047-21.htm";
			}
			else if (cond == 24)
			{
				htmltext = "32047-26.htm";
			}
			else if (cond == 25)
			{
				htmltext = "32047-33.htm";
			}
		}
		else if (npcId == YUMI)
		{
			if (cond == 2)
			{
				htmltext = "32041-01.htm";
			}
			else if (cond == 3)
			{
				htmltext = "32041-05.htm";
			}
			else if (cond == 4)
			{
				htmltext = "32041-06.htm";
			}
			else if (cond == 7)
			{
				htmltext = "32041-07.htm";
			}
			else if (cond == 8)
			{
				htmltext = "32041-13.htm";
			}
			else if (cond == 15)
			{
				htmltext = "32041-14.htm";
			}
			else if (cond == 16)
			{
				if (getQuestItemsCount(player, REPORT2) == 0)
				{
					htmltext = "32041-17.htm";
				}
				else
				{
					htmltext = "32041-18.htm";
				}
			}
			else if (cond == 17)
			{
				htmltext = "32041-22.htm";
			}
			else if (cond == 25)
			{
				htmltext = "32041-26.htm";
			}
		}
		else if (npcId == WEATHER1)
		{
			if (cond == 10)
			{
				htmltext = "32042-01.htm";
			}
			else if (cond == 11)
			{
				if ((qs.getInt("talk") + qs.getInt("talk1") + qs.getInt("talk2")) == 3)
				{
					htmltext = "32042-14.htm";
				}
				else
				{
					htmltext = "32042-06.htm";
				}
			}
			else if (cond == 12)
			{
				htmltext = "32042-15.htm";
			}
		}
		else if (npcId == WEATHER2)
		{
			if (cond == 17)
			{
				htmltext = "32043-01.htm";
			}
			else if (cond == 18)
			{
				if ((qs.getInt("talk") + qs.getInt("talk1")) == 2)
				{
					htmltext = "32043-29.htm";
				}
				else
				{
					htmltext = "32043-06.htm";
				}
			}
			else if (cond == 19)
			{
				htmltext = "32043-30.htm";
			}
		}
		else if (npcId == WEATHER3)
		{
			if (cond == 20)
			{
				htmltext = "32044-01.htm";
			}
			else if (cond == 21)
			{
				htmltext = "32044-06.htm";
			}
			else if (cond > 21)
			{
				htmltext = "32044-18.htm";
			}
		}
		else if (npcId == BOOKSHELF)
		{
			if (cond == 14)
			{
				htmltext = "32045-01.htm";
			}
			else if (cond == 15)
			{
				htmltext = "32045-03.htm";
			}
		}
		
		return htmltext;
	}
}
