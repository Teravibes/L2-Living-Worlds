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
package quests.Q00662_AGameOfCards;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00662_AGameOfCards extends Quest
{
	// NPC
	private static final int KLUMP = 30845;
	
	// Quest Item
	private static final int RED_GEM = 8765;
	
	// Reward Items
	private static final int EW_S = 959;
	private static final int EW_A = 729;
	private static final int EW_B = 947;
	private static final int EW_C = 951;
	private static final int EW_D = 955;
	private static final int EA_D = 956;
	private static final int ZIGGO_GEMSTONE = 8868;
	
	// All cards
	private static final Map<Integer, String> CARDS = new HashMap<>();
	static
	{
		CARDS.put(0, "?");
		CARDS.put(1, "!");
		CARDS.put(2, "=");
		CARDS.put(3, "T");
		CARDS.put(4, "V");
		CARDS.put(5, "O");
		CARDS.put(6, "P");
		CARDS.put(7, "S");
		CARDS.put(8, "E");
		CARDS.put(9, "H");
		CARDS.put(10, "A");
		CARDS.put(11, "R");
		CARDS.put(12, "D");
		CARDS.put(13, "I");
		CARDS.put(14, "N");
	}
	
	// Drop chances
	private static final Map<Integer, Integer> CHANCES = new HashMap<>();
	static
	{
		CHANCES.put(18001, 232000); // Blood Queen
		CHANCES.put(20672, 357000); // Trives
		CHANCES.put(20673, 373000); // Falibati
		CHANCES.put(20674, 583000); // Doom Knight
		CHANCES.put(20677, 435000); // Tulben
		CHANCES.put(20955, 358000); // Ghostly Warrior
		CHANCES.put(20958, 283000); // Death Agent
		CHANCES.put(20959, 455000); // Dark Guard
		CHANCES.put(20961, 365000); // Bloody Knight
		CHANCES.put(20962, 348000); // Bloody Priest
		CHANCES.put(20965, 457000); // Chimera Piece
		CHANCES.put(20966, 493000); // Changed Creation
		CHANCES.put(20968, 418000); // Nonexistant Man
		CHANCES.put(20972, 350000); // Shaman of Ancient Times
		CHANCES.put(20973, 453000); // Forgotten Ancient People
		CHANCES.put(21002, 315000); // Doom Scout
		CHANCES.put(21004, 320000); // Dismal Pole
		CHANCES.put(21006, 335000); // Doom Servant
		CHANCES.put(21008, 462000); // Doom Archer
		CHANCES.put(21010, 397000); // Doom Warrior
		CHANCES.put(21109, 507000); // Hames Orc Scout
		CHANCES.put(21112, 552000); // Hames Orc Footman
		CHANCES.put(21114, 587000); // Cursed Guardian
		CHANCES.put(21116, 812000); // Hames Orc Overlord
		CHANCES.put(21278, 483000); // Antelope
		CHANCES.put(21279, 483000); // Antelope
		CHANCES.put(21280, 483000); // Antelope
		CHANCES.put(21281, 483000); // Antelope
		CHANCES.put(21286, 515000); // Buffalo
		CHANCES.put(21287, 515000); // Buffalo
		CHANCES.put(21288, 515000); // Buffalo
		CHANCES.put(21289, 515000); // Buffalo
		CHANCES.put(21508, 493000); // Splinter Stakato
		CHANCES.put(21510, 527000); // Splinter Stakato Soldier
		CHANCES.put(21513, 562000); // Needle Stakato
		CHANCES.put(21515, 598000); // Needle Stakato Soldier
		CHANCES.put(21520, 458000); // Eye of Splendor
		CHANCES.put(21526, 552000); // Wisdom of Splendor
		CHANCES.put(21530, 488000); // Victory of Splendor
		CHANCES.put(21535, 573000); // Signet of Splendor
	}
	
	public Q00662_AGameOfCards()
	{
		super(662, "A Game of Cards");
		registerQuestItems(RED_GEM);
		addStartNpc(KLUMP);
		addTalkId(KLUMP);
		addKillId(CHANCES.keySet());
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
			case "30845-03.htm":
			{
				st.startQuest();
				st.set("state", "0");
				st.set("stateEx", "0");
				break;
			}
			case "30845-04.htm":
			{
				final int state = st.getInt("state");
				final int stateEx = st.getInt("stateEx");
				if ((state == 0) && (stateEx == 0) && (getQuestItemsCount(player, RED_GEM) >= 50))
				{
					htmltext = "30845-05.htm";
				}
				break;
			}
			case "30845-07.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30845-11.htm":
			{
				final int state = st.getInt("state");
				final int stateEx = st.getInt("stateEx");
				if ((state == 0) && (stateEx == 0) && (getQuestItemsCount(player, RED_GEM) >= 50))
				{
					int i1 = getRandom(70) + 1;
					int i2 = getRandom(70) + 1;
					int i3 = getRandom(70) + 1;
					int i4 = getRandom(70) + 1;
					int i5 = getRandom(70) + 1;
					if (i1 >= 57)
					{
						i1 = i1 - 56;
					}
					else if (i1 >= 43)
					{
						i1 = i1 - 42;
					}
					else if (i1 >= 29)
					{
						i1 = i1 - 28;
					}
					else if (i1 >= 15)
					{
						i1 = i1 - 14;
					}
					
					if (i2 >= 57)
					{
						i2 = i2 - 56;
					}
					else if (i2 >= 43)
					{
						i2 = i2 - 42;
					}
					else if (i2 >= 29)
					{
						i2 = i2 - 28;
					}
					else if (i2 >= 15)
					{
						i2 = i2 - 14;
					}
					
					if (i3 >= 57)
					{
						i3 = i3 - 56;
					}
					else if (i3 >= 43)
					{
						i3 = i3 - 42;
					}
					else if (i3 >= 29)
					{
						i3 = i3 - 28;
					}
					else if (i3 >= 15)
					{
						i3 = i3 - 14;
					}
					
					if (i4 >= 57)
					{
						i4 = i4 - 56;
					}
					else if (i4 >= 43)
					{
						i4 = i4 - 42;
					}
					else if (i4 >= 29)
					{
						i4 = i4 - 28;
					}
					else if (i4 >= 15)
					{
						i4 = i4 - 14;
					}
					
					if (i5 >= 57)
					{
						i5 = i5 - 56;
					}
					else if (i5 >= 43)
					{
						i5 = i5 - 42;
					}
					else if (i5 >= 29)
					{
						i5 = i5 - 28;
					}
					else if (i5 >= 15)
					{
						i5 = i5 - 14;
					}
					
					st.set("state", String.valueOf((i4 * 1000000) + (i3 * 10000) + (i2 * 100) + i1));
					st.set("stateEx", String.valueOf(i5));
					takeItems(player, RED_GEM, 50);
				}
				break;
			}
			case "First":
			case "Second":
			case "Third":
			case "Fourth":
			case "Fifth":
			{
				final int state = st.getInt("state");
				final int stateEx = st.getInt("stateEx");
				int i0;
				int i1;
				int i2;
				int i3;
				int i4;
				int i5;
				int i6;
				int i8;
				int i9;
				i0 = state;
				i1 = stateEx;
				i5 = i1 % 100;
				i9 = i1 / 100;
				i1 = i0 % 100;
				i2 = (i0 % 10000) / 100;
				i3 = (i0 % 1000000) / 10000;
				i4 = (i0 % 100000000) / 1000000;
				switch (event)
				{
					case "First":
					{
						if ((i9 % 2) < 1)
						{
							i9 = i9 + 1;
						}
						break;
					}
					case "Second":
					{
						if ((i9 % 4) < 2)
						{
							i9 = i9 + 2;
						}
						break;
					}
					case "Third":
					{
						if ((i9 % 8) < 4)
						{
							i9 = i9 + 4;
						}
						break;
					}
					case "Fourth":
					{
						if ((i9 % 16) < 8)
						{
							i9 = i9 + 8;
						}
						break;
					}
					case "Fifth":
					{
						if ((i9 % 32) < 16)
						{
							i9 = i9 + 16;
						}
						break;
					}
				}
				
				if ((i9 % 32) < 31)
				{
					st.set("stateEx", String.valueOf((i9 * 100) + i5));
					htmltext = getHtm(player, "30845-12.htm");
				}
				else if ((i9 % 32) == 31)
				{
					i6 = 0;
					i8 = 0;
					if ((i1 >= 1) && (i1 <= 14) && (i2 >= 1) && (i2 <= 14) && (i3 >= 1) && (i3 <= 14) && (i4 >= 1) && (i4 <= 14) && (i5 >= 1) && (i5 <= 14))
					{
						if (i1 == i2)
						{
							i6 = i6 + 10;
							i8 = i8 + 8;
						}
						
						if (i1 == i3)
						{
							i6 = i6 + 10;
							i8 = i8 + 4;
						}
						
						if (i1 == i4)
						{
							i6 = i6 + 10;
							i8 = i8 + 2;
						}
						
						if (i1 == i5)
						{
							i6 = i6 + 10;
							i8 = i8 + 1;
						}
						
						if ((i6 % 100) < 10)
						{
							if ((i8 % 16) < 8)
							{
								if (((i8 % 8) < 4) && (i2 == i3))
								{
									i6 = i6 + 10;
									i8 = i8 + 4;
								}
								
								if (((i8 % 4) < 2) && (i2 == i4))
								{
									i6 = i6 + 10;
									i8 = i8 + 2;
								}
								
								if (((i8 % 2) < 1) && (i2 == i5))
								{
									i6 = i6 + 10;
									i8 = i8 + 1;
								}
							}
						}
						else if (((i6 % 10) == 0) && ((i8 % 16) < 8))
						{
							if (((i8 % 8) < 4) && (i2 == i3))
							{
								i6 = i6 + 1;
								i8 = i8 + 4;
							}
							
							if (((i8 % 4) < 2) && (i2 == i4))
							{
								i6 = i6 + 1;
								i8 = i8 + 2;
							}
							
							if (((i8 % 2) < 1) && (i2 == i5))
							{
								i6 = i6 + 1;
								i8 = i8 + 1;
							}
						}
						
						if ((i6 % 100) < 10)
						{
							if ((i8 % 8) < 4)
							{
								if (((i8 % 4) < 2) && (i3 == i4))
								{
									i6 = i6 + 10;
									i8 = i8 + 2;
								}
								
								if (((i8 % 2) < 1) && (i3 == i5))
								{
									i6 = i6 + 10;
									i8 = i8 + 1;
								}
							}
						}
						else if (((i6 % 10) == 0) && ((i8 % 8) < 4))
						{
							if (((i8 % 4) < 2) && (i3 == i4))
							{
								i6 = i6 + 1;
								i8 = i8 + 2;
							}
							
							if (((i8 % 2) < 1) && (i3 == i5))
							{
								i6 = i6 + 1;
								i8 = i8 + 1;
							}
						}
						
						if ((i6 % 100) < 10)
						{
							if (((i8 % 4) < 2) && ((i8 % 2) < 1) && (i4 == i5))
							{
								i6 = i6 + 10;
								i8 = i8 + 1;
							}
						}
						else if (((i6 % 10) == 0) && ((i8 % 4) < 2) && ((i8 % 2) < 1) && (i4 == i5))
						{
							i6 = i6 + 1;
							i8 = i8 + 1;
						}
					}
					
					if (i6 == 40)
					{
						giveReward(player, ZIGGO_GEMSTONE, 43);
						giveReward(player, EW_S, 3);
						giveReward(player, EW_A, 1);
						htmltext = getHtm(player, "30845-13.htm");
					}
					else if (i6 == 30)
					{
						giveReward(player, EW_S, 2);
						giveReward(player, EW_C, 2);
						htmltext = getHtm(player, "30845-14.htm");
					}
					else if ((i6 == 21) || (i6 == 12))
					{
						giveReward(player, EW_A, 1);
						giveReward(player, EW_B, 2);
						giveReward(player, EW_D, 1);
						htmltext = getHtm(player, "30845-15.htm");
					}
					else if (i6 == 20)
					{
						giveReward(player, EW_C, 2);
						htmltext = getHtm(player, "30845-16.htm");
					}
					else if (i6 == 11)
					{
						giveReward(player, EW_C, 1);
						htmltext = getHtm(player, "30845-17.htm");
					}
					else if (i6 == 10)
					{
						giveReward(player, EA_D, 2);
						htmltext = getHtm(player, "30845-18.htm");
					}
					else if (i6 == 0)
					{
						htmltext = getHtm(player, "30845-19.htm");
					}
					
					st.set("state", "0");
					st.set("stateEx", "0");
				}
				
				htmltext = htmltext.replace("%FontColor1%", ((i9 % 2) < 1) ? "ffff00" : "ff6f6f").replace("%Cell1%", ((i9 % 2) < 1) ? CARDS.get(0) : CARDS.get(i1));
				htmltext = htmltext.replace("%FontColor2%", ((i9 % 4) < 2) ? "ffff00" : "ff6f6f").replace("%Cell2%", ((i9 % 4) < 2) ? CARDS.get(0) : CARDS.get(i2));
				htmltext = htmltext.replace("%FontColor3%", ((i9 % 8) < 4) ? "ffff00" : "ff6f6f").replace("%Cell3%", ((i9 % 8) < 4) ? CARDS.get(0) : CARDS.get(i3));
				htmltext = htmltext.replace("%FontColor4%", ((i9 % 16) < 8) ? "ffff00" : "ff6f6f").replace("%Cell4%", ((i9 % 16) < 8) ? CARDS.get(0) : CARDS.get(i4));
				htmltext = htmltext.replace("%FontColor5%", ((i9 % 32) < 16) ? "ffff00" : "ff6f6f").replace("%Cell5%", ((i9 % 32) < 16) ? CARDS.get(0) : CARDS.get(i5));
				break;
			}
			case "30845-20.htm":
			{
				if (getQuestItemsCount(player, RED_GEM) < 50)
				{
					htmltext = "30845-21.htm";
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
				htmltext = (player.getLevel() < 61) ? "30845-02.htm" : "30845-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int state = st.getInt("state");
				final int stateEx = st.getInt("stateEx");
				if ((state == 0) && (stateEx == 0))
				{
					htmltext = (getQuestItemsCount(player, RED_GEM) < 50) ? "30845-04.htm" : "30845-05.htm";
				}
				else if ((state != 0) && (stateEx != 0))
				{
					int i0;
					int i1;
					int i2;
					int i3;
					int i4;
					int i5;
					int i9;
					i0 = state;
					i1 = stateEx;
					i5 = i1 % 100;
					i9 = i1 / 100;
					i1 = i0 % 100;
					i2 = (i0 % 10000) / 100;
					i3 = (i0 % 1000000) / 10000;
					i4 = (i0 % 100000000) / 1000000;
					htmltext = getHtm(player, "30845-11a.htm");
					htmltext = htmltext.replace("%FontColor1%", ((i9 % 2) < 1) ? "ffff00" : "ff6f6f").replace("%Cell1%", ((i9 % 2) < 1) ? CARDS.get(0) : CARDS.get(i1));
					htmltext = htmltext.replace("%FontColor2%", ((i9 % 4) < 2) ? "ffff00" : "ff6f6f").replace("%Cell2%", ((i9 % 4) < 2) ? CARDS.get(0) : CARDS.get(i2));
					htmltext = htmltext.replace("%FontColor3%", ((i9 % 8) < 4) ? "ffff00" : "ff6f6f").replace("%Cell3%", ((i9 % 8) < 4) ? CARDS.get(0) : CARDS.get(i3));
					htmltext = htmltext.replace("%FontColor4%", ((i9 % 16) < 8) ? "ffff00" : "ff6f6f").replace("%Cell4%", ((i9 % 16) < 8) ? CARDS.get(0) : CARDS.get(i4));
					htmltext = htmltext.replace("%FontColor5%", ((i9 % 32) < 16) ? "ffff00" : "ff6f6f").replace("%Cell5%", ((i9 % 32) < 16) ? CARDS.get(0) : CARDS.get(i5));
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getRandomPartyMemberState(player, -1, 3, npc);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		if (getRandom(1000000) < CHANCES.get(npc.getId()))
		{
			final Player partyMember = st.getPlayer();
			giveItems(partyMember, RED_GEM, 1);
			playSound(partyMember, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
	
	private void giveReward(Player player, int item, int count)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(item);
		if (template.isStackable())
		{
			giveItems(player, item, count);
		}
		else
		{
			for (int i = 0; i < count; i++)
			{
				giveItems(player, item, 1);
			}
		}
	}
}
