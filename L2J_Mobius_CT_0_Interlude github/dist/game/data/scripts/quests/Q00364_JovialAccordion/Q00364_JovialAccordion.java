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
package quests.Q00364_JovialAccordion;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00364_JovialAccordion extends Quest
{
	// NPCs
	private static final int BARBADO = 30959;
	private static final int SWAN = 30957;
	private static final int SABRIN = 30060;
	private static final int XABER = 30075;
	private static final int CLOTH_CHEST = 30961;
	private static final int BEER_CHEST = 30960;
	
	// Items
	private static final int KEY_1 = 4323;
	private static final int KEY_2 = 4324;
	private static final int STOLEN_BEER = 4321;
	private static final int STOLEN_CLOTHES = 4322;
	private static final int ECHO = 4421;
	
	public Q00364_JovialAccordion()
	{
		super(364, "Jovial Accordion");
		registerQuestItems(KEY_1, KEY_2, STOLEN_BEER, STOLEN_CLOTHES);
		addStartNpc(BARBADO);
		addTalkId(BARBADO, SWAN, SABRIN, XABER, CLOTH_CHEST, BEER_CHEST);
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
			case "30959-02.htm":
			{
				st.startQuest();
				st.set("items", "0");
				break;
			}
			case "30957-02.htm":
			{
				st.setCond(2, true);
				giveItems(player, KEY_1, 1);
				giveItems(player, KEY_2, 1);
				break;
			}
			case "30960-04.htm":
			{
				if (hasQuestItems(player, KEY_2))
				{
					takeItems(player, KEY_2, 1);
					if (getRandomBoolean())
					{
						htmltext = "30960-02.htm";
						giveItems(player, STOLEN_BEER, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case "30961-04.htm":
			{
				if (hasQuestItems(player, KEY_1))
				{
					takeItems(player, KEY_1, 1);
					if (getRandomBoolean())
					{
						htmltext = "30961-02.htm";
						giveItems(player, STOLEN_CLOTHES, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
				htmltext = (player.getLevel() < 15) ? "30959-00.htm" : "30959-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				final int stolenItems = st.getInt("items");
				switch (npc.getId())
				{
					case BARBADO:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "30959-03.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30959-04.htm";
							giveItems(player, ECHO, 1);
							st.exitQuest(true, true);
						}
						break;
					}
					case SWAN:
					{
						if (cond == 1)
						{
							htmltext = "30957-01.htm";
						}
						else if (cond == 2)
						{
							if (stolenItems > 0)
							{
								st.setCond(3, true);
								
								if (stolenItems == 2)
								{
									htmltext = "30957-04.htm";
									giveAdena(player, 100, true);
								}
								else
								{
									htmltext = "30957-05.htm";
								}
							}
							else
							{
								if (!hasQuestItems(player, KEY_1) && !hasQuestItems(player, KEY_2))
								{
									htmltext = "30957-06.htm";
									st.exitQuest(true, true);
								}
								else
								{
									htmltext = "30957-03.htm";
								}
							}
						}
						else if (cond == 3)
						{
							htmltext = "30957-07.htm";
						}
						break;
					}
					case BEER_CHEST:
					{
						htmltext = "30960-03.htm";
						if ((cond == 2) && hasQuestItems(player, KEY_2))
						{
							htmltext = "30960-01.htm";
						}
						break;
					}
					case CLOTH_CHEST:
					{
						htmltext = "30961-03.htm";
						if ((cond == 2) && hasQuestItems(player, KEY_1))
						{
							htmltext = "30961-01.htm";
						}
						break;
					}
					case SABRIN:
					{
						if (hasQuestItems(player, STOLEN_BEER))
						{
							htmltext = "30060-01.htm";
							st.set("items", String.valueOf(stolenItems + 1));
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							takeItems(player, STOLEN_BEER, 1);
						}
						else
						{
							htmltext = "30060-02.htm";
						}
						break;
					}
					case XABER:
					{
						if (hasQuestItems(player, STOLEN_CLOTHES))
						{
							htmltext = "30075-01.htm";
							st.set("items", String.valueOf(stolenItems + 1));
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							takeItems(player, STOLEN_CLOTHES, 1);
						}
						else
						{
							htmltext = "30075-02.htm";
						}
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
}
