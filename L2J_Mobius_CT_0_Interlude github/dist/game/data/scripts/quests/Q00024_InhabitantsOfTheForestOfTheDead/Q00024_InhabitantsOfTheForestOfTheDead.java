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
package quests.Q00024_InhabitantsOfTheForestOfTheDead;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.enums.ChatType;

import quests.Q00023_LidiasHeart.Q00023_LidiasHeart;

public class Q00024_InhabitantsOfTheForestOfTheDead extends Quest
{
	// NPCs
	private static final int DORIAN = 31389;
	private static final int WIZARD = 31522;
	private static final int TOMBSTONE = 31531;
	private static final int MAID_OF_LIDIA = 31532;
	private static final int[] MOBS =
	{
		21557, // BONE_SNATCHER
		21558, // BONE_SNATCHER_A
		21560, // BONE_SHAPER
		21563, // BONE_COLLECTOR
		21564, // SKULL_COLLECTOR
		21565, // BONE_ANIMATOR
		21566, // SKULL_ANIMATOR
		21567, // BONE_SLAYER
	};
	
	// Items
	private static final int LETTER = 7065;
	private static final int HAIRPIN = 7148;
	private static final int TOTEM = 7151;
	private static final int FLOWER = 7152;
	private static final int SILVER_CROSS = 7153;
	private static final int BROKEN_SILVER_CROSS = 7154;
	private static final int SUSPICIOUS_TOTEM = 7156;
	
	public Q00024_InhabitantsOfTheForestOfTheDead()
	{
		super(24, "Inhabitants of the Forest of the Dead");
		
		addStartNpc(DORIAN);
		addTalkId(DORIAN, TOMBSTONE, MAID_OF_LIDIA, WIZARD);
		registerQuestItems(FLOWER, SILVER_CROSS, BROKEN_SILVER_CROSS, LETTER, HAIRPIN, TOTEM);
		addKillId(MOBS);
		addAggroRangeEnterId(25332);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return event;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31389-03.htm":
			{
				st.startQuest();
				st.set("state", "1");
				giveItems(player, FLOWER, 1);
				break;
			}
			case "31389-08.htm":
			{
				st.set("state", "3");
				break;
			}
			case "31389-13.htm":
			{
				st.setCond(3, true);
				st.set("state", "4");
				giveItems(player, SILVER_CROSS, 1);
				break;
			}
			case "31389-18.htm":
			{
				playSound(player, QuestSound.INTERFACESOUND_CHARSTAT_OPEN);
				break;
			}
			case "31389-19.htm":
			{
				st.setCond(5, true);
				st.set("state", "5");
				takeItems(player, BROKEN_SILVER_CROSS, -1);
				break;
			}
			case "31522-03.htm":
			{
				st.set("state", "12");
				takeItems(player, TOTEM, -1);
				break;
			}
			case "31522-08.htm":
			{
				st.setCond(11, true);
				st.set("state", "13");
				break;
			}
			case "31522-17.htm":
			{
				st.set("state", "14");
				break;
			}
			case "31522-21.htm":
			{
				giveItems(player, SUSPICIOUS_TOTEM, 1);
				st.exitQuest(false, true);
				break;
			}
			case "31532-04.htm":
			{
				st.setCond(6, true);
				st.set("state", "6");
				giveItems(player, LETTER, 1);
				break;
			}
			case "31532-06.htm":
			{
				if (hasQuestItems(player, HAIRPIN))
				{
					st.set("state", "8");
					takeItems(player, LETTER, -1);
					takeItems(player, HAIRPIN, -1);
				}
				else
				{
					st.setCond(7);
					st.set("state", "7");
					htmltext = "31532-07.htm";
				}
				break;
			}
			case "31532-10.htm":
			{
				st.set("state", "9");
				break;
			}
			case "31532-14.htm":
			{
				st.set("state", "10");
				break;
			}
			case "31532-19.htm":
			{
				st.setCond(9, true);
				st.set("state", "11");
				break;
			}
			case "31531-02.htm":
			{
				st.setCond(2, true);
				st.set("state", "2");
				takeItems(player, FLOWER, -1);
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
				final QuestState st2 = player.getQuestState(Q00023_LidiasHeart.class.getSimpleName());
				if ((st2 != null) && st2.isCompleted() && (player.getLevel() >= 65))
				{
					htmltext = "31389-01.htm";
				}
				else
				{
					htmltext = "31389-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				int state = st.getInt("state");
				switch (npc.getId())
				{
					case 31389:
					{
						if (state == 1)
						{
							htmltext = "31389-04.htm";
							return htmltext;
						}
						else if (state == 2)
						{
							htmltext = "31389-05.htm";
							return htmltext;
						}
						else if (state == 3)
						{
							htmltext = "31389-09.htm";
							return htmltext;
						}
						else if (state == 4)
						{
							if (hasQuestItems(player, SILVER_CROSS))
							{
								htmltext = "31389-14.htm";
							}
							else if (hasQuestItems(player, BROKEN_SILVER_CROSS))
							{
								htmltext = "31389-15.htm";
								return htmltext;
							}
							
							return htmltext;
						}
						else if (state == 5)
						{
							htmltext = "31389-20.htm";
							return htmltext;
						}
						else
						{
							if ((state == 7) && !hasQuestItems(player, HAIRPIN))
							{
								htmltext = "31389-21.htm";
								st.setCond(8, true);
								giveItems(player, HAIRPIN, 1);
							}
							else if (((state == 7) && hasQuestItems(player, HAIRPIN)) || (state == 6))
							{
								htmltext = "31389-22.htm";
								return htmltext;
							}
							
							return htmltext;
						}
					}
					case 31522:
					{
						if ((state == 11) && hasQuestItems(player, TOTEM))
						{
							htmltext = "31522-01.htm";
							return htmltext;
						}
						else if (state == 12)
						{
							htmltext = "31522-04.htm";
							return htmltext;
						}
						else
						{
							if (state == 13)
							{
								htmltext = "31522-09.htm";
							}
							else if (state == 14)
							{
								htmltext = "31522-18.htm";
								return htmltext;
							}
							
							return htmltext;
						}
					}
					case 31531:
					{
						if ((state == 1) && hasQuestItems(player, FLOWER))
						{
							htmltext = "31531-01.htm";
							playSound(player, QuestSound.AMDSOUND_WIND_LOOT);
						}
						else if (state == 2)
						{
							htmltext = "31531-03.htm";
							return htmltext;
						}
						
						return htmltext;
					}
					case 31532:
					{
						if (state == 5)
						{
							htmltext = "31532-01.htm";
							return htmltext;
						}
						else if ((state == 6) && hasQuestItems(player, LETTER))
						{
							htmltext = "31532-05.htm";
							return htmltext;
						}
						else if (state == 7)
						{
							htmltext = "31532-07a.htm";
							return htmltext;
						}
						else if (state == 8)
						{
							htmltext = "31532-08.htm";
							return htmltext;
						}
						else if (state == 9)
						{
							htmltext = "31532-11.htm";
							return htmltext;
						}
						else
						{
							if (state == 10)
							{
								htmltext = "31532-15.htm";
							}
							else if (state == 11)
							{
								htmltext = "31532-20.htm";
								return htmltext;
							}
							
							return htmltext;
						}
					}
					default:
					{
						return htmltext;
					}
				}
			}
			case State.COMPLETED:
			{
				if (npc.getId() == 31522)
				{
					htmltext = "31522-22.htm";
				}
				else
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isPet)
	{
		if (isPet)
		{
			npc.getAttackByList().remove(player.getSummon());
		}
		else
		{
			npc.getAttackByList().remove(player);
			final QuestState st = player.getQuestState(getName());
			if ((st != null) && (getQuestItemsCount(player, SILVER_CROSS) > 0))
			{
				takeItems(player, SILVER_CROSS, -1);
				giveItems(player, BROKEN_SILVER_CROSS, 1);
				st.setCond(4);
				npc.broadcastSay(ChatType.GENERAL, "That sign!");
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = getQuestState(player, false);
		if ((st != null) && st.isCond(9) && (getRandom(100) < 10))
		{
			giveItems(player, TOTEM, 1);
			st.setCond(10, true);
		}
	}
}
