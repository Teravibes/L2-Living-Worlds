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
package quests.Q00365_DevilsLegacy;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.Skill;

public class Q00365_DevilsLegacy extends Quest
{
	// NPCs
	private static final int RANDOLF = 30095;
	private static final int COLLOB = 30092;
	
	// Item
	private static final int PIRATE_TREASURE_CHEST = 5873;
	
	public Q00365_DevilsLegacy()
	{
		super(365, "Devil's Legacy");
		registerQuestItems(PIRATE_TREASURE_CHEST);
		addStartNpc(RANDOLF);
		addTalkId(RANDOLF, COLLOB);
		addKillId(20836, 20845, 21629, 21630); // Pirate Zombie && Pirate Zombie Captain.
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
			case "30095-02.htm":
			{
				st.startQuest();
				break;
			}
			case "30095-06.htm":
			{
				st.exitQuest(true, true);
				break;
			}
			case "30092-05.htm":
			{
				if (!hasQuestItems(player, PIRATE_TREASURE_CHEST))
				{
					htmltext = "30092-02.htm";
				}
				else if (getQuestItemsCount(player, 57) < 600)
				{
					htmltext = "30092-03.htm";
				}
				else
				{
					takeItems(player, PIRATE_TREASURE_CHEST, 1);
					takeItems(player, 57, 600);
					
					int i0;
					if (getRandom(100) < 80)
					{
						i0 = getRandom(100);
						if (i0 < 1)
						{
							giveItems(player, 955, 1);
						}
						else if (i0 < 4)
						{
							giveItems(player, 956, 1);
						}
						else if (i0 < 36)
						{
							giveItems(player, 1868, 1);
						}
						else if (i0 < 68)
						{
							giveItems(player, 1884, 1);
						}
						else
						{
							giveItems(player, 1872, 1);
						}
						
						htmltext = "30092-05.htm";
					}
					else
					{
						i0 = getRandom(1000);
						if (i0 < 10)
						{
							giveItems(player, 951, 1);
						}
						else if (i0 < 40)
						{
							giveItems(player, 952, 1);
						}
						else if (i0 < 60)
						{
							giveItems(player, 955, 1);
						}
						else if (i0 < 260)
						{
							giveItems(player, 956, 1);
						}
						else if (i0 < 445)
						{
							giveItems(player, 1879, 1);
						}
						else if (i0 < 630)
						{
							giveItems(player, 1880, 1);
						}
						else if (i0 < 815)
						{
							giveItems(player, 1882, 1);
						}
						else
						{
							giveItems(player, 1881, 1);
						}
						
						htmltext = "30092-06.htm";
						
						// Curse effect !
						final Skill skill = SkillData.getInstance().getSkill(4082, 1);
						if ((skill != null) && !player.isAffectedBySkill(skill.getId()))
						{
							skill.applyEffects(npc, player);
						}
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
		String htmltext = Quest.getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() < 39) ? "30095-00.htm" : "30095-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case RANDOLF:
					{
						if (!hasQuestItems(player, PIRATE_TREASURE_CHEST))
						{
							htmltext = "30095-03.htm";
						}
						else
						{
							htmltext = "30095-05.htm";
							
							final int reward = getQuestItemsCount(player, PIRATE_TREASURE_CHEST) * 1600;
							
							takeItems(player, PIRATE_TREASURE_CHEST, -1);
							giveAdena(player, reward, true);
						}
						break;
					}
					case COLLOB:
					{
						htmltext = "30092-01.htm";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(player, -1, 3, npc);
		if (qs != null)
		{
			giveItemRandomly(qs.getPlayer(), npc, PIRATE_TREASURE_CHEST, 1, 0, npc.getId() == 20836 ? 0.36 : 0.52, true);
		}
	}
}
