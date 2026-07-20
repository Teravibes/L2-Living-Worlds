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
package quests.Q00374_WhisperOfDreamsPart1;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00374_WhisperOfDreamsPart1 extends Quest
{
	// NPCs
	private static final int MANAKIA = 30515;
	private static final int TORAI = 30557;
	
	// Monsters
	private static final int CAVE_BEAST = 20620;
	private static final int DEATH_WAVE = 20621;
	
	// Items
	private static final int CAVE_BEAST_TOOTH = 5884;
	private static final int DEATH_WAVE_LIGHT = 5885;
	private static final int SEALED_MYSTERIOUS_STONE = 5886;
	private static final int MYSTERIOUS_STONE = 5887;
	
	// Rewards
	private static final int[][] REWARDS =
	{
		// @formatter:off
		{5486, 3, 2950}, // Dark Crystal, 3x, 2950 adena
		{5487, 2, 18050}, // Nightmare, 2x, 18050 adena
		{5488, 2, 18050}, // Majestic, 2x, 18050 adena
		{5485, 4, 10450}, // Tallum Tunic, 4, 10450 adena
		{5489, 6, 15550}, // Tallum Stockings, 6, 15550 adena
		// @formatter:on
	};
	
	public Q00374_WhisperOfDreamsPart1()
	{
		super(374, "Whisper of Dreams, Part 1");
		registerQuestItems(DEATH_WAVE_LIGHT, CAVE_BEAST_TOOTH, SEALED_MYSTERIOUS_STONE, MYSTERIOUS_STONE);
		addStartNpc(MANAKIA);
		addTalkId(MANAKIA, TORAI);
		addKillId(CAVE_BEAST, DEATH_WAVE);
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
		
		// Manakia
		if (event.equals("30515-03.htm"))
		{
			st.startQuest();
			st.set("condStone", "1");
		}
		else if (event.startsWith("30515-06-"))
		{
			if ((getQuestItemsCount(player, CAVE_BEAST_TOOTH) >= 65) && (getQuestItemsCount(player, DEATH_WAVE_LIGHT) >= 65))
			{
				htmltext = "30515-06.htm";
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				
				final int[] reward = REWARDS[Integer.parseInt(event.substring(9, 10))];
				
				takeItems(player, CAVE_BEAST_TOOTH, -1);
				takeItems(player, DEATH_WAVE_LIGHT, -1);
				
				giveAdena(player, reward[2], true);
				giveItems(player, reward[0], reward[1]);
			}
			else
			{
				htmltext = "30515-07.htm";
			}
		}
		else if (event.equals("30515-08.htm"))
		{
			st.exitQuest(true, true);
		}
		// Torai
		else if (event.equals("30557-02.htm"))
		{
			if (st.isCond(2) && hasQuestItems(player, SEALED_MYSTERIOUS_STONE))
			{
				st.setCond(3, true);
				takeItems(player, SEALED_MYSTERIOUS_STONE, -1);
				giveItems(player, MYSTERIOUS_STONE, 1);
			}
			else
			{
				htmltext = "30557-03.htm";
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
				htmltext = (player.getLevel() < 56) ? "30515-01.htm" : "30515-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MANAKIA:
					{
						if (!hasQuestItems(player, SEALED_MYSTERIOUS_STONE))
						{
							if ((getQuestItemsCount(player, CAVE_BEAST_TOOTH) >= 65) && (getQuestItemsCount(player, DEATH_WAVE_LIGHT) >= 65))
							{
								htmltext = "30515-05.htm";
							}
							else
							{
								htmltext = "30515-04.htm";
							}
						}
						else
						{
							if (cond == 1)
							{
								htmltext = "30515-09.htm";
								st.setCond(2, true);
							}
							else
							{
								htmltext = "30515-10.htm";
							}
						}
						break;
					}
					case TORAI:
					{
						if ((cond == 2) && hasQuestItems(player, SEALED_MYSTERIOUS_STONE))
						{
							htmltext = "30557-01.htm";
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
		// Drop tooth or light to anyone.
		Player partyMember = getRandomPartyMemberState(player, State.STARTED);
		if (partyMember == null)
		{
			return;
		}
		
		QuestState st = partyMember.getQuestState(getName());
		if (st == null)
		{
			return;
		}
		
		if (getRandomBoolean())
		{
			giveItems(st.getPlayer(), (npc.getId() == CAVE_BEAST) ? CAVE_BEAST_TOOTH : DEATH_WAVE_LIGHT, Rnd.get(1, 65));
		}
		
		// Drop sealed mysterious stone to party member who still need it.
		partyMember = getRandomPartyMember(player, "condStone", "1");
		if (partyMember == null)
		{
			return;
		}
		
		st = partyMember.getQuestState(getName());
		if (st == null)
		{
			return;
		}
		
		if (Rnd.get(100) < 1)
		{
			giveItems(st.getPlayer(), SEALED_MYSTERIOUS_STONE, 1);
			st.unset("condStone");
		}
	}
}
