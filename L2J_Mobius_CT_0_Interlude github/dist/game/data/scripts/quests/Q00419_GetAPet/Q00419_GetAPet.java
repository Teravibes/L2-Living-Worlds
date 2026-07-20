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
package quests.Q00419_GetAPet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00419_GetAPet extends Quest
{
	// NPCs
	private static final int MARTIN = 30731;
	private static final int BELLA = 30256;
	private static final int METTY = 30072;
	private static final int ELLIE = 30091;
	
	// Items
	private static final int ANIMAL_LOVER_LIST = 3417;
	private static final int ANIMAL_SLAYER_LIST_1 = 3418;
	private static final int ANIMAL_SLAYER_LIST_2 = 3419;
	private static final int ANIMAL_SLAYER_LIST_3 = 3420;
	private static final int ANIMAL_SLAYER_LIST_4 = 3421;
	private static final int ANIMAL_SLAYER_LIST_5 = 3422;
	private static final int BLOODY_FANG = 3423;
	private static final int BLOODY_CLAW = 3424;
	private static final int BLOODY_NAIL = 3425;
	private static final int BLOODY_KASHA_FANG = 3426;
	private static final int BLOODY_TARANTULA_NAIL = 3427;
	
	// Reward
	private static final int WOLF_COLLAR = 2375;
	
	// Droplist
	private static final Map<Integer, int[]> DROPLIST = new HashMap<>();
	static
	{
		// @formatter:off
		DROPLIST.put(20103, new int[]{BLOODY_FANG, 600000});
		DROPLIST.put(20106, new int[]{BLOODY_FANG, 750000});
		DROPLIST.put(20108, new int[]{BLOODY_FANG, 1000000});
		DROPLIST.put(20460, new int[]{BLOODY_CLAW, 600000});
		DROPLIST.put(20308, new int[]{BLOODY_CLAW, 750000});
		DROPLIST.put(20466, new int[]{BLOODY_CLAW, 1000000});
		DROPLIST.put(20025, new int[]{BLOODY_NAIL, 600000});
		DROPLIST.put(20105, new int[]{BLOODY_NAIL, 750000});
		DROPLIST.put(20034, new int[]{BLOODY_NAIL, 1000000});
		DROPLIST.put(20474, new int[]{BLOODY_KASHA_FANG, 600000});
		DROPLIST.put(20476, new int[]{BLOODY_KASHA_FANG, 750000});
		DROPLIST.put(20478, new int[]{BLOODY_KASHA_FANG, 1000000});
		DROPLIST.put(20403, new int[]{BLOODY_TARANTULA_NAIL, 750000});
		DROPLIST.put(20508, new int[]{BLOODY_TARANTULA_NAIL, 1000000});
		// @formatter:on
	}
	
	public Q00419_GetAPet()
	{
		super(419, "Get a Pet");
		registerQuestItems(ANIMAL_LOVER_LIST, ANIMAL_SLAYER_LIST_1, ANIMAL_SLAYER_LIST_2, ANIMAL_SLAYER_LIST_3, ANIMAL_SLAYER_LIST_4, ANIMAL_SLAYER_LIST_5, BLOODY_FANG, BLOODY_CLAW, BLOODY_NAIL, BLOODY_KASHA_FANG, BLOODY_TARANTULA_NAIL);
		addStartNpc(MARTIN);
		addTalkId(MARTIN, BELLA, ELLIE, METTY);
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
			case "task":
			{
				final int race = player.getRace().ordinal();
				htmltext = "30731-0" + (race + 4) + ".htm";
				st.startQuest();
				giveItems(player, ANIMAL_SLAYER_LIST_1 + race, 1);
				break;
			}
			case "30731-12.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				takeItems(player, ANIMAL_SLAYER_LIST_1, 1);
				takeItems(player, ANIMAL_SLAYER_LIST_2, 1);
				takeItems(player, ANIMAL_SLAYER_LIST_3, 1);
				takeItems(player, ANIMAL_SLAYER_LIST_4, 1);
				takeItems(player, ANIMAL_SLAYER_LIST_5, 1);
				takeItems(player, BLOODY_FANG, -1);
				takeItems(player, BLOODY_CLAW, -1);
				takeItems(player, BLOODY_NAIL, -1);
				takeItems(player, BLOODY_KASHA_FANG, -1);
				takeItems(player, BLOODY_TARANTULA_NAIL, -1);
				giveItems(player, ANIMAL_LOVER_LIST, 1);
				break;
			}
			case "30256-03.htm":
			{
				st.set("progress", String.valueOf(st.getInt("progress") | 1));
				if (st.getInt("progress") == 7)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case "30072-02.htm":
			{
				st.set("progress", String.valueOf(st.getInt("progress") | 2));
				if (st.getInt("progress") == 7)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case "30091-02.htm":
			{
				st.set("progress", String.valueOf(st.getInt("progress") | 4));
				if (st.getInt("progress") == 7)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
				break;
			}
			case "test":
			{
				st.set("answers", "0");
				st.set("quiz", "20 21 22 23 24 25 26 27 28 29 30 31 32 33");
				return checkQuestions(st);
			}
			case "wrong":
			{
				st.set("wrong", String.valueOf(st.getInt("wrong") + 1));
				return checkQuestions(st);
			}
			case "right":
			{
				st.set("correct", String.valueOf(st.getInt("correct") + 1));
				return checkQuestions(st);
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
				htmltext = (player.getLevel() < 15) ? "30731-01.htm" : "30731-02.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case MARTIN:
					{
						if (hasAtLeastOneQuestItem(player, ANIMAL_SLAYER_LIST_1, ANIMAL_SLAYER_LIST_2, ANIMAL_SLAYER_LIST_3, ANIMAL_SLAYER_LIST_4, ANIMAL_SLAYER_LIST_5))
						{
							final int proofs = getQuestItemsCount(player, BLOODY_FANG) + getQuestItemsCount(player, BLOODY_CLAW) + getQuestItemsCount(player, BLOODY_NAIL) + getQuestItemsCount(player, BLOODY_KASHA_FANG) + getQuestItemsCount(player, BLOODY_TARANTULA_NAIL);
							if (proofs == 0)
							{
								htmltext = "30731-09.htm";
							}
							else if (proofs < 50)
							{
								htmltext = "30731-10.htm";
							}
							else
							{
								htmltext = "30731-11.htm";
							}
						}
						else if (st.getInt("progress") == 7)
						{
							htmltext = "30731-13.htm";
						}
						else
						{
							htmltext = "30731-16.htm";
						}
						break;
					}
					case BELLA:
					{
						htmltext = "30256-01.htm";
						break;
					}
					case METTY:
					{
						htmltext = "30072-01.htm";
						break;
					}
					case ELLIE:
					{
						htmltext = "30091-01.htm";
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
		
		final int[] drop = DROPLIST.get(npc.getId());
		if (hasQuestItems(player, drop[0] - 5))
		{
			if (getQuestItemsCount(player, drop[0]) < 50)
			{
				giveItems(player, drop[0], 1);
				if (getQuestItemsCount(player, drop[0]) < 50)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				else
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				}
			}
		}
	}
	
	private static String checkQuestions(QuestState st)
	{
		final int answers = st.getInt("correct") + (st.getInt("wrong"));
		if (answers < 10)
		{
			final String[] questions = st.get("quiz").split(" ");
			final int index = getRandom(questions.length - 1);
			final String question = questions[index];
			if (questions.length > (10 - answers))
			{
				questions[index] = questions[questions.length - 1];
				st.set("quiz", String.join(" ", Arrays.copyOf(questions, questions.length - 1)));
			}
			
			return "30731-" + question + ".htm";
		}
		
		if (st.getInt("wrong") > 0)
		{
			st.unset("progress");
			st.unset("answers");
			st.unset("quiz");
			st.unset("wrong");
			st.unset("correct");
			return "30731-14.htm";
		}
		
		takeItems(st.getPlayer(), ANIMAL_LOVER_LIST, 1);
		giveItems(st.getPlayer(), WOLF_COLLAR, 1);
		st.exitQuest(true, true);
		
		return "30731-15.htm";
	}
}
