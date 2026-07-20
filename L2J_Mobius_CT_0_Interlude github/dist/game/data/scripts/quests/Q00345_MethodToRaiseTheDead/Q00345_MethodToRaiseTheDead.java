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
package quests.Q00345_MethodToRaiseTheDead;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00345_MethodToRaiseTheDead extends Quest
{
	// NPCs
	private static final int XENOVIA = 30912;
	private static final int DOROTHY = 30970;
	private static final int ORPHEUS = 30971;
	private static final int MEDIUM_JAR = 30973;
	
	// Items
	private static final int VICTIM_ARM_BONE = 4274;
	private static final int VICTIM_THIGH_BONE = 4275;
	private static final int VICTIM_SKULL = 4276;
	private static final int VICTIM_RIB_BONE = 4277;
	private static final int VICTIM_SPINE = 4278;
	private static final int USELESS_BONE_PIECES = 4280;
	private static final int POWDER_TO_SUMMON_DEAD_SOULS = 4281;
	
	// Rewards
	private static final int BILL_OF_IASON_HEINE = 4407;
	private static final int IMPERIAL_DIAMOND = 3456;
	
	public Q00345_MethodToRaiseTheDead()
	{
		super(345, "Method to Raise the Dead");
		registerQuestItems(VICTIM_ARM_BONE, VICTIM_THIGH_BONE, VICTIM_SKULL, VICTIM_RIB_BONE, VICTIM_SPINE, POWDER_TO_SUMMON_DEAD_SOULS, USELESS_BONE_PIECES);
		addStartNpc(DOROTHY);
		addTalkId(DOROTHY, XENOVIA, MEDIUM_JAR, ORPHEUS);
		addKillId(20789, 20791);
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
			case "30970-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30970-06.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "30912-04.htm":
			{
				if (player.getAdena() >= 1000)
				{
					htmltext = "30912-03.htm";
					st.setCond(3, true);
					takeItems(player, 57, 1000);
					giveItems(player, POWDER_TO_SUMMON_DEAD_SOULS, 1);
				}
				break;
			}
			case "30973-04.htm":
			{
				if (st.isCond(3))
				{
					final int chance = getRandom(3);
					if (chance == 0)
					{
						st.setCond(6, true);
						htmltext = "30973-02a.htm";
					}
					else if (chance == 1)
					{
						st.setCond(6, true);
						htmltext = "30973-02b.htm";
					}
					else
					{
						st.setCond(7, true);
						htmltext = "30973-02c.htm";
					}
					
					takeItems(player, POWDER_TO_SUMMON_DEAD_SOULS, -1);
					takeItems(player, VICTIM_ARM_BONE, -1);
					takeItems(player, VICTIM_THIGH_BONE, -1);
					takeItems(player, VICTIM_SKULL, -1);
					takeItems(player, VICTIM_RIB_BONE, -1);
					takeItems(player, VICTIM_SPINE, -1);
				}
				break;
			}
			case "30971-02a.htm":
			{
				if (hasQuestItems(player, USELESS_BONE_PIECES))
				{
					htmltext = "30971-02.htm";
				}
				break;
			}
			case "30971-03.htm":
			{
				if (hasQuestItems(player, USELESS_BONE_PIECES))
				{
					final int amount = getQuestItemsCount(player, USELESS_BONE_PIECES) * 104;
					takeItems(player, USELESS_BONE_PIECES, -1);
					giveAdena(player, amount, true);
				}
				else
				{
					htmltext = "30971-02a.htm";
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
				htmltext = (player.getLevel() < 35) ? "30970-00.htm" : "30970-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case DOROTHY:
					{
						if (cond == 1)
						{
							htmltext = (!hasQuestItems(player, VICTIM_ARM_BONE, VICTIM_THIGH_BONE, VICTIM_SKULL, VICTIM_RIB_BONE, VICTIM_SPINE)) ? "30970-04.htm" : "30970-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30970-07.htm";
						}
						else if ((cond > 2) && (cond < 6))
						{
							htmltext = "30970-08.htm";
						}
						else
						{
							// Shared part between cond 6 and 7.
							final int amount = getQuestItemsCount(player, USELESS_BONE_PIECES) * 70;
							takeItems(player, USELESS_BONE_PIECES, -1);
							
							// Scaried little girl
							if (cond == 7)
							{
								htmltext = "30970-10.htm";
								giveAdena(player, 3040 + amount, true);
								
								// Reward can be either an Imperial Diamond or bills.
								if (getRandom(100) < 10)
								{
									giveItems(player, IMPERIAL_DIAMOND, 1);
								}
								else
								{
									giveItems(player, BILL_OF_IASON_HEINE, 5);
								}
							}
							// Friends of Dorothy
							else
							{
								htmltext = "30970-09.htm";
								giveAdena(player, 5390 + amount, true);
								giveItems(player, BILL_OF_IASON_HEINE, 3);
							}
							
							st.exitQuest(true, true);
						}
						break;
					}
					case XENOVIA:
					{
						if (cond == 2)
						{
							htmltext = "30912-01.htm";
						}
						else if (cond > 2)
						{
							htmltext = "30912-06.htm";
						}
						break;
					}
					case MEDIUM_JAR:
					{
						htmltext = "30973-01.htm";
						break;
					}
					case ORPHEUS:
					{
						htmltext = "30971-01.htm";
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
		
		if (getRandom(4) == 0)
		{
			final int randomPart = getRandom(VICTIM_ARM_BONE, VICTIM_SPINE);
			if (!hasQuestItems(player, randomPart))
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, randomPart, 1);
				return;
			}
		}
		
		playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		giveItems(player, USELESS_BONE_PIECES, 1);
	}
}
