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
package quests.Q00293_TheHiddenVeins;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

public class Q00293_TheHiddenVeins extends Quest
{
	// NPCs
	private static final int FILAUR = 30535;
	private static final int CHINCHIRIN = 30539;
	
	// Monsters
	private static final int UTUKU_ORC = 20446;
	private static final int UTUKU_ARCHER = 20447;
	private static final int UTUKU_GRUNT = 20448;
	
	// Items
	private static final int CHRYSOLITE_ORE = 1488;
	private static final int TORN_MAP_FRAGMENT = 1489;
	private static final int HIDDEN_VEIN_MAP = 1490;
	
	// Reward
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	
	public Q00293_TheHiddenVeins()
	{
		super(293, "The Hidden Veins");
		registerQuestItems(CHRYSOLITE_ORE, TORN_MAP_FRAGMENT, HIDDEN_VEIN_MAP);
		addStartNpc(FILAUR);
		addTalkId(FILAUR, CHINCHIRIN);
		addKillId(UTUKU_ORC, UTUKU_ARCHER, UTUKU_GRUNT);
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
			case "30535-03.htm":
			{
				st.startQuest();
				break;
			}
			case "30535-06.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30539-02.htm":
			{
				if (getQuestItemsCount(player, TORN_MAP_FRAGMENT) >= 4)
				{
					htmltext = "30539-03.htm";
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					takeItems(player, TORN_MAP_FRAGMENT, 4);
					giveItems(player, HIDDEN_VEIN_MAP, 1);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (player.getRace() != Race.DWARF)
				{
					htmltext = "30535-00.htm";
				}
				else if (player.getLevel() < 6)
				{
					htmltext = "30535-01.htm";
				}
				else
				{
					htmltext = "30535-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case FILAUR:
					{
						final int chrysoliteOres = getQuestItemsCount(player, CHRYSOLITE_ORE);
						final int hiddenVeinMaps = getQuestItemsCount(player, HIDDEN_VEIN_MAP);
						if ((chrysoliteOres + hiddenVeinMaps) == 0)
						{
							htmltext = "30535-04.htm";
						}
						else
						{
							if (hiddenVeinMaps > 0)
							{
								if (chrysoliteOres > 0)
								{
									htmltext = "30535-09.htm";
								}
								else
								{
									htmltext = "30535-08.htm";
								}
							}
							else
							{
								htmltext = "30535-05.htm";
							}
							
							int reward = (chrysoliteOres * 5) + (hiddenVeinMaps * 500);
							if (!GeneralConfig.ALT_VILLAGES_REPEATABLE_QUEST_REWARD)
							{
								if (chrysoliteOres >= 10)
								{
									reward += 2000;
								}
								
								if (hiddenVeinMaps >= 10)
								{
									reward += 2000;
								}
							}
							
							takeItems(player, CHRYSOLITE_ORE, -1);
							takeItems(player, HIDDEN_VEIN_MAP, -1);
							giveAdena(player, reward, true);
							
							// Give newbie reward if player is eligible.
							if (player.isNewbie() && (st.getInt("Reward") == 0))
							{
								int newPlayerRewardsReceived = player.getVariables().getInt(PlayerVariables.NEWBIE_SHOTS_RECEIVED, 0);
								if (newPlayerRewardsReceived < 1)
								{
									giveItems(player, SOULSHOT_FOR_BEGINNERS, 6000);
									st.playTutorialVoice("tutorial_voice_026");
									st.set("Reward", "1");
									player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
								}
							}
						}
						break;
					}
					case CHINCHIRIN:
					{
						htmltext = "30539-01.htm";
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
		
		final int chance = getRandom(100);
		if (chance > 50)
		{
			giveItems(player, CHRYSOLITE_ORE, 1);
		}
		else if (chance < 5)
		{
			giveItems(player, TORN_MAP_FRAGMENT, 1);
		}
	}
}
