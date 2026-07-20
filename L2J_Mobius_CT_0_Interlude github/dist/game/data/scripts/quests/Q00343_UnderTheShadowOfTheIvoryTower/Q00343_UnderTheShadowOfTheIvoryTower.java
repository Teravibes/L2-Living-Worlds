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
package quests.Q00343_UnderTheShadowOfTheIvoryTower;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;

/**
 * Adapted from FirstTeam Interlude
 */
public class Q00343_UnderTheShadowOfTheIvoryTower extends Quest
{
	// NPCs
	private static final int CEMA = 30834;
	private static final int ICARUS = 30835;
	private static final int MARSHA = 30934;
	private static final int TRUMPIN = 30935;
	private static final int[] MONSTERS =
	{
		20563,
		20564,
		20565,
		20566
	};
	
	// Items
	private static final int ORB = 4364;
	private static final int ECTOPLASM = 4365;
	
	// Misc
	private static final int CHANCE = 50;
	private static final int[] ALLOWED_CLASSES =
	{
		11,
		12,
		13,
		14,
		26,
		27,
		28,
		39,
		40,
		41
	};
	
	public Q00343_UnderTheShadowOfTheIvoryTower()
	{
		super(343, "Under the Shadow of the Ivory Tower");
		addStartNpc(CEMA);
		addTalkId(CEMA, ICARUS, MARSHA, TRUMPIN);
		addKillId(MONSTERS);
		registerQuestItems(ORB);
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
		
		final int random1 = getRandom(3);
		final int random2 = getRandom(2);
		final int orbs = getQuestItemsCount(player, ORB);
		switch (event)
		{
			case "30834-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30834-08.htm":
			{
				if (orbs > 0)
				{
					giveAdena(player, orbs * 125, true);
					takeItems(player, ORB, -1);
				}
				else
				{
					htmltext = "30834-08.htm";
				}
				break;
			}
			case "30834-09.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30934-02.htm":
			case "30934-03.htm":
			{
				if (orbs < 10)
				{
					htmltext = "noorbs.htm";
				}
				else if ("30934-03.htm".equals(event))
				{
					if (orbs >= 10)
					{
						takeItems(player, ORB, 10);
						st.set("playing", "1");
					}
					else
					{
						htmltext = "noorbs.htm";
					}
				}
				break;
			}
			case "30934-04.htm":
			{
				if (st.getInt("playing") > 0)
				{
					switch (random1)
					{
						case 0:
						{
							htmltext = "30934-05.htm";
							giveItems(player, ORB, 10);
							break;
						}
						case 1:
						{
							htmltext = "30934-06.htm";
							break;
						}
						default:
						{
							htmltext = "30934-04.htm";
							giveItems(player, ORB, 20);
							break;
						}
					}
					
					st.unset("playing");
				}
				else
				{
					htmltext = "Player is cheating";
					takeItems(player, ORB, -1);
					st.exitQuest(true);
				}
				break;
			}
			case "30934-05.htm":
			{
				if (st.getInt("playing") > 0)
				{
					switch (random1)
					{
						case 0:
						{
							htmltext = "30934-04.htm";
							giveItems(player, ORB, 20);
							break;
						}
						case 1:
						{
							htmltext = "30934-05.htm";
							giveItems(player, ORB, 10);
							break;
						}
						default:
						{
							htmltext = "30934-06.htm";
							break;
						}
					}
					
					st.unset("playing");
				}
				else
				{
					htmltext = "Player is cheating";
					takeItems(player, ORB, -1);
					st.exitQuest(true);
				}
				break;
			}
			case "30934-06.htm":
			{
				if (st.getInt("playing") > 0)
				{
					switch (random1)
					{
						case 0:
						{
							htmltext = "30934-04.htm";
							giveItems(player, ORB, 20);
							break;
						}
						case 1:
						{
							htmltext = "30934-06.htm";
							break;
						}
						default:
						{
							htmltext = "30934-05.htm";
							giveItems(player, ORB, 10);
							break;
						}
					}
					
					st.unset("playing");
				}
				else
				{
					htmltext = "Player is cheating";
					takeItems(player, ORB, -1);
					st.exitQuest(true);
				}
				break;
			}
			case "30935-02.htm":
			case "30935-03.htm":
			{
				st.unset("toss");
				if (orbs < 10)
				{
					htmltext = "noorbs.htm";
				}
				break;
			}
			case "30935-05.htm":
			{
				if (orbs >= 10)
				{
					if (random2 == 0)
					{
						final int toss = st.getInt("toss");
						if (toss == 4)
						{
							st.unset("toss");
							giveItems(player, ORB, 150);
							htmltext = "30935-07.htm";
						}
						else
						{
							st.set("toss", String.valueOf(toss + 1));
							htmltext = "30935-04.htm";
						}
					}
					else
					{
						st.unset("toss");
						takeItems(player, ORB, 10);
					}
				}
				else
				{
					htmltext = "noorbs.htm";
				}
				break;
			}
			case "30935-06.htm":
			{
				if (orbs >= 10)
				{
					final int toss = st.getInt("toss");
					st.unset("toss");
					switch (toss)
					{
						case 1:
						{
							giveItems(player, ORB, 10);
							break;
						}
						case 2:
						{
							giveItems(player, ORB, 30);
							break;
						}
						case 3:
						{
							giveItems(player, ORB, 70);
							break;
						}
						case 4:
						{
							giveItems(player, ORB, 150);
							break;
						}
					}
				}
				else
				{
					htmltext = "noorbs.htm";
				}
				break;
			}
			case "30835-02.htm":
			{
				if (getQuestItemsCount(player, ECTOPLASM) > 0)
				{
					takeItems(player, ECTOPLASM, 1);
					final int random3 = getRandom(1000);
					if (random3 <= 119)
					{
						giveItems(player, 955, 1);
					}
					else if (random3 <= 169)
					{
						giveItems(player, 951, 1);
					}
					else if (random3 <= 329)
					{
						giveItems(player, 2511, (getRandom(200) + 401));
					}
					else if (random3 <= 559)
					{
						giveItems(player, 2510, (getRandom(200) + 401));
					}
					else if (random3 <= 561)
					{
						giveItems(player, 316, 1);
					}
					else if (random3 <= 578)
					{
						giveItems(player, 630, 1);
					}
					else if (random3 <= 579)
					{
						giveItems(player, 188, 1);
					}
					else if (random3 <= 581)
					{
						giveItems(player, 885, 1);
					}
					else if (random3 <= 582)
					{
						giveItems(player, 103, 1);
					}
					else if (random3 <= 584)
					{
						giveItems(player, 917, 1);
					}
					else
					{
						giveItems(player, 736, 1);
					}
				}
				else
				{
					htmltext = "30835-03.htm";
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
		
		switch (npc.getId())
		{
			case CEMA:
			{
				if (!st.isStarted())
				{
					for (int classId : ALLOWED_CLASSES)
					{
						if ((st.getPlayer().getPlayerClass().getId() == classId) && (st.getPlayer().getLevel() >= 40))
						{
							htmltext = "30834-01.htm";
						}
					}
					
					if (!"30834-01.htm".equals(htmltext))
					{
						htmltext = "30834-07.htm";
						st.exitQuest(true);
					}
				}
				else if (getQuestItemsCount(player, ORB) > 0)
				{
					htmltext = "30834-06.htm";
				}
				else
				{
					htmltext = "30834-05.htm";
				}
				break;
			}
			case ICARUS:
			{
				htmltext = "30835-01.htm";
				break;
			}
			case MARSHA:
			{
				htmltext = "30934-01.htm";
				break;
			}
			case TRUMPIN:
			{
				htmltext = "30935-01.htm";
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
		
		if (getRandom(100) < CHANCE)
		{
			giveItems(player, ORB, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
}
