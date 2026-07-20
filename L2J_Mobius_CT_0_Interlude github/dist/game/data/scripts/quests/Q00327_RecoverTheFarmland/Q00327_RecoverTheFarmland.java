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
package quests.Q00327_RecoverTheFarmland;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00327_RecoverTheFarmland extends Quest
{
	// NPCs
	private static final int LEIKAN = 30382;
	private static final int PIOTUR = 30597;
	private static final int IRIS = 30034;
	private static final int ASHA = 30313;
	private static final int NESTLE = 30314;
	
	// Monsters
	private static final int TUREK_ORC_WARLORD = 20495;
	private static final int TUREK_ORC_ARCHER = 20496;
	private static final int TUREK_ORC_SKIRMISHER = 20497;
	private static final int TUREK_ORC_SUPPLIER = 20498;
	private static final int TUREK_ORC_FOOTMAN = 20499;
	private static final int TUREK_ORC_SENTINEL = 20500;
	private static final int TUREK_ORC_SHAMAN = 20501;
	
	// Items
	private static final int LEIKAN_LETTER = 5012;
	private static final int TUREK_DOGTAG = 1846;
	private static final int TUREK_MEDALLION = 1847;
	private static final int CLAY_URN_FRAGMENT = 1848;
	private static final int BRASS_TRINKET_PIECE = 1849;
	private static final int BRONZE_MIRROR_PIECE = 1850;
	private static final int JADE_NECKLACE_BEAD = 1851;
	private static final int ANCIENT_CLAY_URN = 1852;
	private static final int ANCIENT_BRASS_TIARA = 1853;
	private static final int ANCIENT_BRONZE_MIRROR = 1854;
	private static final int ANCIENT_JADE_NECKLACE = 1855;
	
	// Rewards
	private static final int ADENA = 57;
	private static final int SOULSHOT_D = 1463;
	private static final int SPIRITSHOT_D = 2510;
	private static final int HEALING_POTION = 1061;
	private static final int HASTE_POTION = 734;
	private static final int POTION_OF_ALACRITY = 735;
	private static final int SCROLL_OF_ESCAPE = 736;
	private static final int SCROLL_OF_RESURRECTION = 737;
	
	// Chances
	private static final int[][] DROPLIST =
	{
		// @formatter:off
		{TUREK_ORC_ARCHER, 140000, TUREK_DOGTAG},
		{TUREK_ORC_SKIRMISHER, 70000, TUREK_DOGTAG},
		{TUREK_ORC_SUPPLIER, 120000, TUREK_DOGTAG},
		{TUREK_ORC_FOOTMAN, 100000, TUREK_DOGTAG},
		{TUREK_ORC_SENTINEL, 80000, TUREK_DOGTAG},
		{TUREK_ORC_SHAMAN, 90000, TUREK_MEDALLION},
		{TUREK_ORC_WARLORD, 180000, TUREK_MEDALLION}
		// @formatter:on
	};
	
	// Exp
	private static final Map<Integer, Integer> EXP_REWARD = new HashMap<>();
	static
	{
		EXP_REWARD.put(ANCIENT_CLAY_URN, 2766);
		EXP_REWARD.put(ANCIENT_BRASS_TIARA, 3227);
		EXP_REWARD.put(ANCIENT_BRONZE_MIRROR, 3227);
		EXP_REWARD.put(ANCIENT_JADE_NECKLACE, 3919);
	}
	
	public Q00327_RecoverTheFarmland()
	{
		super(327, "Recover the Farmland");
		registerQuestItems(LEIKAN_LETTER);
		addStartNpc(LEIKAN, PIOTUR);
		addTalkId(LEIKAN, PIOTUR, IRIS, ASHA, NESTLE);
		addKillId(TUREK_ORC_WARLORD, TUREK_ORC_ARCHER, TUREK_ORC_SKIRMISHER, TUREK_ORC_SUPPLIER, TUREK_ORC_FOOTMAN, TUREK_ORC_SENTINEL, TUREK_ORC_SHAMAN);
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
			case "30597-03.htm":
			{
				if ((st.getCond() < 1))
				{
					st.startQuest();
				}
				break;
			}
			case "30597-06.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30382-03.htm":
			{
				st.startQuest();
				st.setCond(2);
				giveItems(player, LEIKAN_LETTER, 1);
				break;
			}
			case "30313-02.htm":
			{
				if (getQuestItemsCount(player, CLAY_URN_FRAGMENT) >= 5)
				{
					takeItems(player, CLAY_URN_FRAGMENT, 5);
					if (getRandom(6) < 5)
					{
						htmltext = "30313-03.htm";
						rewardItems(player, ANCIENT_CLAY_URN, 1);
					}
					else
					{
						htmltext = "30313-10.htm";
					}
				}
				break;
			}
			case "30313-04.htm":
			{
				if (getQuestItemsCount(player, BRASS_TRINKET_PIECE) >= 5)
				{
					takeItems(player, BRASS_TRINKET_PIECE, 5);
					if (getRandom(7) < 6)
					{
						htmltext = "30313-05.htm";
						rewardItems(player, ANCIENT_BRASS_TIARA, 1);
					}
					else
					{
						htmltext = "30313-10.htm";
					}
				}
				break;
			}
			case "30313-06.htm":
			{
				if (getQuestItemsCount(player, BRONZE_MIRROR_PIECE) >= 5)
				{
					takeItems(player, BRONZE_MIRROR_PIECE, 5);
					if (getRandom(7) < 6)
					{
						htmltext = "30313-07.htm";
						rewardItems(player, ANCIENT_BRONZE_MIRROR, 1);
					}
					else
					{
						htmltext = "30313-10.htm";
					}
				}
				break;
			}
			case "30313-08.htm":
			{
				if (getQuestItemsCount(player, JADE_NECKLACE_BEAD) >= 5)
				{
					takeItems(player, JADE_NECKLACE_BEAD, 5);
					if (getRandom(8) < 7)
					{
						htmltext = "30313-09.htm";
						rewardItems(player, ANCIENT_JADE_NECKLACE, 1);
					}
					else
					{
						htmltext = "30313-10.htm";
					}
				}
				break;
			}
			case "30034-03.htm":
			{
				final int n = getQuestItemsCount(player, CLAY_URN_FRAGMENT);
				if (n == 0)
				{
					htmltext = "30034-02.htm";
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, CLAY_URN_FRAGMENT, n);
					addExpAndSp(player, n * 307, 0);
				}
				break;
			}
			case "30034-04.htm":
			{
				final int n = getQuestItemsCount(player, BRASS_TRINKET_PIECE);
				if (n == 0)
				{
					htmltext = "30034-02.htm";
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, BRASS_TRINKET_PIECE, n);
					addExpAndSp(player, n * 368, 0);
				}
				break;
			}
			case "30034-05.htm":
			{
				final int n = getQuestItemsCount(player, BRONZE_MIRROR_PIECE);
				if (n == 0)
				{
					htmltext = "30034-02.htm";
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, BRONZE_MIRROR_PIECE, n);
					addExpAndSp(player, n * 368, 0);
				}
				break;
			}
			case "30034-06.htm":
			{
				final int n = getQuestItemsCount(player, JADE_NECKLACE_BEAD);
				if (n == 0)
				{
					htmltext = "30034-02.htm";
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, JADE_NECKLACE_BEAD, n);
					addExpAndSp(player, n * 430, 0);
				}
				break;
			}
			case "30034-07.htm":
			{
				boolean isRewarded = false;
				for (int i = 1852; i < 1856; i++)
				{
					final int n = getQuestItemsCount(player, i);
					if (n > 0)
					{
						takeItems(player, i, n);
						addExpAndSp(player, n * EXP_REWARD.get(i), 0);
						isRewarded = true;
					}
				}
				
				if (!isRewarded)
				{
					htmltext = "30034-02.htm";
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case "30314-03.htm":
			{
				if (!hasQuestItems(player, ANCIENT_CLAY_URN))
				{
					htmltext = "30314-07.htm";
				}
				else
				{
					takeItems(player, ANCIENT_CLAY_URN, 1);
					rewardItems(player, SOULSHOT_D, 70 + getRandom(41));
				}
				break;
			}
			case "30314-04.htm":
			{
				if (!hasQuestItems(player, ANCIENT_BRASS_TIARA))
				{
					htmltext = "30314-07.htm";
				}
				else
				{
					takeItems(player, ANCIENT_BRASS_TIARA, 1);
					final int rnd = getRandom(100);
					if (rnd < 40)
					{
						rewardItems(player, HEALING_POTION, 1);
					}
					else if (rnd < 84)
					{
						rewardItems(player, HASTE_POTION, 1);
					}
					else
					{
						rewardItems(player, POTION_OF_ALACRITY, 1);
					}
				}
				break;
			}
			case "30314-05.htm":
			{
				if (!hasQuestItems(player, ANCIENT_BRONZE_MIRROR))
				{
					htmltext = "30314-07.htm";
				}
				else
				{
					takeItems(player, ANCIENT_BRONZE_MIRROR, 1);
					rewardItems(player, (getRandom(100) < 59) ? SCROLL_OF_ESCAPE : SCROLL_OF_RESURRECTION, 1);
				}
				break;
			}
			case "30314-06.htm":
			{
				if (!hasQuestItems(player, ANCIENT_JADE_NECKLACE))
				{
					htmltext = "30314-07.htm";
				}
				else
				{
					takeItems(player, ANCIENT_JADE_NECKLACE, 1);
					rewardItems(player, SPIRITSHOT_D, 50 + getRandom(41));
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
				htmltext = npc.getId() + ((player.getLevel() < 25) ? "-01.htm" : "-02.htm");
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case PIOTUR:
					{
						if (!hasQuestItems(player, LEIKAN_LETTER))
						{
							if (hasAtLeastOneQuestItem(player, TUREK_DOGTAG, TUREK_MEDALLION))
							{
								htmltext = "30597-05.htm";
								
								if (cond < 4)
								{
									st.setCond(4, true);
								}
								
								final int dogtag = getQuestItemsCount(player, TUREK_DOGTAG);
								final int medallion = getQuestItemsCount(player, TUREK_MEDALLION);
								takeItems(player, TUREK_DOGTAG, -1);
								takeItems(player, TUREK_MEDALLION, -1);
								rewardItems(player, ADENA, (dogtag * 40) + (medallion * 50) + (((dogtag + medallion) >= 10) ? 619 : 0));
							}
							else
							{
								htmltext = "30597-04.htm";
							}
						}
						else
						{
							htmltext = "30597-03a.htm";
							st.setCond(3, true);
							takeItems(player, LEIKAN_LETTER, 1);
						}
						break;
					}
					case LEIKAN:
					{
						if (cond == 2)
						{
							htmltext = "30382-04.htm";
						}
						else if ((cond == 3) || (cond == 4))
						{
							htmltext = "30382-05.htm";
							st.setCond(5, true);
						}
						else if (cond == 5)
						{
							htmltext = "30382-05.htm";
						}
						break;
					}
					default:
					{
						htmltext = npc.getId() + "-01.htm";
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
		
		for (int[] npcData : DROPLIST)
		{
			if (npcData[0] == npc.getId())
			{
				giveItems(player, npcData[2], 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				
				if (getRandom(1000000) < npcData[1])
				{
					giveItems(player, getRandom(1848, 1851), 1);
				}
				break;
			}
		}
	}
}
