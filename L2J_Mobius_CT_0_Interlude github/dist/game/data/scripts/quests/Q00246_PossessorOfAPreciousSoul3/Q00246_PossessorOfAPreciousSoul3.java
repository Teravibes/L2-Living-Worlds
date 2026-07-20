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
package quests.Q00246_PossessorOfAPreciousSoul3;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00246_PossessorOfAPreciousSoul3 extends Quest
{
	// NPCs
	private static final int CARADINE = 31740;
	private static final int OSSIAN = 31741;
	private static final int LADD = 30721;
	
	// Items
	private static final int CARADINE_LETTER_2 = 7678;
	private static final int WATERBINDER = 7591;
	private static final int EVERGREEN = 7592;
	private static final int RAIN_SONG = 7593;
	private static final int RELIC_BOX = 7594;
	private static final int CARADINE_LETTER_3 = 7679;
	
	// Mobs
	private static final int PILGRIM_OF_SPLENDOR = 21541;
	private static final int JUDGE_OF_SPLENDOR = 21544;
	private static final int BARAKIEL = 25325;
	
	public Q00246_PossessorOfAPreciousSoul3()
	{
		super(246, "Possessor of a Precious Soul - 3");
		
		_questItemIds = new int[]
		{
			WATERBINDER,
			EVERGREEN,
			RAIN_SONG,
			RELIC_BOX
		};
		
		addStartNpc(CARADINE);
		addTalkId(CARADINE, OSSIAN, LADD);
		addKillId(PILGRIM_OF_SPLENDOR, JUDGE_OF_SPLENDOR, BARAKIEL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return htmltext;
		}
		
		// Caradine
		if (event.equalsIgnoreCase("31740-04.htm"))
		{
			st.startQuest();
			takeItems(player, CARADINE_LETTER_2, 1);
		}
		// Ossian
		else if (event.equalsIgnoreCase("31741-02.htm"))
		{
			st.setCond(2);
		}
		else if (event.equalsIgnoreCase("31741-05.htm"))
		{
			if (hasQuestItems(player, WATERBINDER) && hasQuestItems(player, EVERGREEN))
			{
				st.setCond(4);
				takeItems(player, WATERBINDER, 1);
				takeItems(player, EVERGREEN, 1);
			}
			else
			{
				htmltext = null;
			}
		}
		else if (event.equalsIgnoreCase("31741-08.htm"))
		{
			if (hasQuestItems(player, RAIN_SONG))
			{
				st.setCond(6);
				takeItems(player, RAIN_SONG, 1);
				giveItems(player, RELIC_BOX, 1);
			}
			else
			{
				htmltext = null;
			}
		}
		// Ladd
		else if (event.equalsIgnoreCase("30721-02.htm"))
		{
			if (hasQuestItems(player, RELIC_BOX))
			{
				takeItems(player, RELIC_BOX, 1);
				giveItems(player, CARADINE_LETTER_3, 1);
				addExpAndSp(player, 719843, 0);
				st.exitQuest(false);
			}
			else
			{
				htmltext = null;
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
				if (hasQuestItems(player, CARADINE_LETTER_2))
				{
					if (!player.isSubClassActive() || (player.getLevel() < 65))
					{
						htmltext = "31740-02.htm";
						st.exitQuest(true);
					}
					else
					{
						htmltext = "31740-01.htm";
					}
				}
				break;
			}
			case State.STARTED:
			{
				if (!player.isSubClassActive())
				{
					break;
				}
				
				switch (npc.getId())
				{
					case CARADINE:
					{
						if (st.isCond(1))
						{
							htmltext = "31740-05.htm";
						}
						break;
					}
					case OSSIAN:
					{
						switch (st.getCond())
						{
							case 1:
							{
								htmltext = "31741-01.htm";
								break;
							}
							case 2:
							{
								htmltext = "31741-03.htm";
								break;
							}
							case 3:
							{
								if (hasQuestItems(player, WATERBINDER) && hasQuestItems(player, EVERGREEN))
								{
									htmltext = "31741-04.htm";
								}
								break;
							}
							case 4:
							{
								htmltext = "31741-06.htm";
								break;
							}
							case 5:
							{
								if (hasQuestItems(player, RAIN_SONG))
								{
									htmltext = "31741-07.htm";
								}
								break;
							}
							case 6:
							{
								htmltext = "31741-09.htm";
								break;
							}
						}
						break;
					}
					case LADD:
					{
						if (st.isCond(6) && hasQuestItems(player, RELIC_BOX))
						{
							htmltext = "30721-01.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = getAlreadyCompletedMsg(player);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == BARAKIEL)
		{
			QuestState pst;
			if ((player.getParty() != null) && !player.getParty().getMembers().isEmpty())
			{
				for (Player member : player.getParty().getMembers())
				{
					if (member.calculateDistance3D(npc) > PlayerConfig.ALT_PARTY_RANGE)
					{
						continue;
					}
					
					pst = member.getQuestState(getName());
					if ((pst != null) && pst.isCond(4) && !hasQuestItems(member, RAIN_SONG))
					{
						giveItems(member, RAIN_SONG, 1);
						pst.setCond(5, true);
					}
				}
			}
			else
			{
				pst = player.getQuestState(getName());
				if ((pst != null) && pst.isCond(4) && !hasQuestItems(player, RAIN_SONG))
				{
					giveItems(player, RAIN_SONG, 1);
					pst.setCond(5, true);
				}
			}
		}
		else
		{
			if (!player.isSubClassActive())
			{
				return;
			}
			
			final QuestState st = player.getQuestState(getName());
			if ((st != null) && !st.isCond(2))
			{
				return;
			}
			
			if (Rnd.get(10) < 2)
			{
				final int neklaceOrRing = (npcId == PILGRIM_OF_SPLENDOR) ? WATERBINDER : EVERGREEN;
				if ((st != null) && !hasQuestItems(player, neklaceOrRing))
				{
					giveItems(player, neklaceOrRing, 1);
					if (!hasQuestItems(player, (npcId == PILGRIM_OF_SPLENDOR) ? EVERGREEN : WATERBINDER))
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					else
					{
						st.setCond(3, true);
					}
				}
			}
		}
	}
}
