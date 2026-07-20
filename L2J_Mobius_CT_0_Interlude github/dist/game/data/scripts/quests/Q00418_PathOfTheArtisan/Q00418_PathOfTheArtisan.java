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
package quests.Q00418_PathOfTheArtisan;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00418_PathOfTheArtisan extends Quest
{
	// NPCs
	private static final int SILVERA = 30527;
	private static final int KLUTO = 30317;
	private static final int PINTER = 30298;
	private static final int OBI = 32052;
	private static final int HITCHI = 31963;
	private static final int LOCKIRIN = 30531;
	private static final int RYDEL = 31956;
	
	// Items
	private static final int SILVERA_RING = 1632;
	private static final int FIRST_PASS_CERTIFICATE = 1633;
	private static final int SECOND_PASS_CERTIFICATE = 1634;
	private static final int FINAL_PASS_CERTIFICATE = 1635;
	private static final int BOOGLE_RATMAN_TOOTH = 1636;
	private static final int BOOGLE_RATMAN_LEADER_TOOTH = 1637;
	private static final int KLUTO_LETTER = 1638;
	private static final int FOOTPRINT_OF_THIEF = 1639;
	private static final int STOLEN_SECRET_BOX = 1640;
	private static final int SECRET_BOX = 1641;
	
	public Q00418_PathOfTheArtisan()
	{
		super(418, "Path to an Artisan");
		registerQuestItems(SILVERA_RING, FIRST_PASS_CERTIFICATE, SECOND_PASS_CERTIFICATE, BOOGLE_RATMAN_TOOTH, BOOGLE_RATMAN_LEADER_TOOTH, KLUTO_LETTER, FOOTPRINT_OF_THIEF, STOLEN_SECRET_BOX, SECRET_BOX);
		addStartNpc(SILVERA);
		addTalkId(SILVERA, KLUTO, PINTER, OBI, HITCHI, LOCKIRIN, RYDEL);
		addKillId(20389, 20390, 20017);
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
			case "30527-05.htm":
			{
				if (player.getPlayerClass() != PlayerClass.DWARVEN_FIGHTER)
				{
					htmltext = (player.getPlayerClass() == PlayerClass.ARTISAN) ? "30527-02a.htm" : "30527-02.htm";
				}
				else if (player.getLevel() < 19)
				{
					htmltext = "30527-03.htm";
				}
				else if (hasQuestItems(player, FINAL_PASS_CERTIFICATE))
				{
					htmltext = "30527-04.htm";
				}
				break;
			}
			case "30527-06.htm":
			{
				st.startQuest();
				giveItems(player, SILVERA_RING, 1);
				break;
			}
			case "30527-08a.htm":
			{
				st.setCond(3, true);
				takeItems(player, BOOGLE_RATMAN_LEADER_TOOTH, -1);
				takeItems(player, BOOGLE_RATMAN_TOOTH, -1);
				takeItems(player, SILVERA_RING, 1);
				giveItems(player, FIRST_PASS_CERTIFICATE, 1);
				break;
			}
			case "30527-08b.htm":
			{
				st.setCond(8, true);
				takeItems(player, BOOGLE_RATMAN_LEADER_TOOTH, -1);
				takeItems(player, BOOGLE_RATMAN_TOOTH, -1);
				takeItems(player, SILVERA_RING, 1);
				break;
			}
			case "30317-04.htm":
			case "30317-07.htm":
			{
				st.setCond(4, true);
				giveItems(player, KLUTO_LETTER, 1);
				break;
			}
			case "30317-10.htm":
			{
				takeItems(player, FIRST_PASS_CERTIFICATE, 1);
				takeItems(player, SECOND_PASS_CERTIFICATE, 1);
				takeItems(player, SECRET_BOX, 1);
				giveItems(player, FINAL_PASS_CERTIFICATE, 1);
				addExpAndSp(player, 3200, 6980);
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(true, true);
				break;
			}
			case "30317-12.htm":
			case "30531-05.htm":
			case "32052-11.htm":
			case "31963-10.htm":
			case "31956-04.htm":
			{
				takeItems(player, FIRST_PASS_CERTIFICATE, 1);
				takeItems(player, SECOND_PASS_CERTIFICATE, 1);
				takeItems(player, SECRET_BOX, 1);
				giveItems(player, FINAL_PASS_CERTIFICATE, 1);
				addExpAndSp(player, 3200, 3490);
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(true, true);
				break;
			}
			case "30298-03.htm":
			{
				st.setCond(5, true);
				takeItems(player, KLUTO_LETTER, -1);
				giveItems(player, FOOTPRINT_OF_THIEF, 1);
				break;
			}
			case "30298-06.htm":
			{
				st.setCond(7, true);
				takeItems(player, FOOTPRINT_OF_THIEF, -1);
				takeItems(player, STOLEN_SECRET_BOX, -1);
				giveItems(player, SECOND_PASS_CERTIFICATE, 1);
				giveItems(player, SECRET_BOX, 1);
				break;
			}
			case "32052-06.htm":
			{
				st.setCond(9, true);
				break;
			}
			case "31963-04.htm":
			{
				st.setCond(10, true);
				break;
			}
			case "31963-05.htm":
			{
				st.setCond(11, true);
				break;
			}
			case "31963-07.htm":
			{
				st.setCond(12, true);
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
				htmltext = "30527-01.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case SILVERA:
					{
						if (cond == 1)
						{
							htmltext = "30527-07.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30527-08.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30527-09.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30527-09a.htm";
						}
						break;
					}
					case KLUTO:
					{
						if (cond == 3)
						{
							htmltext = "30317-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30317-08.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30317-09.htm";
						}
						break;
					}
					case PINTER:
					{
						if (cond == 4)
						{
							htmltext = "30298-01.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30298-04.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30298-05.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30298-07.htm";
						}
						break;
					}
					case OBI:
					{
						if (cond == 8)
						{
							htmltext = "32052-01.htm";
						}
						else if (cond == 9)
						{
							htmltext = "32052-06a.htm";
						}
						else if (cond == 11)
						{
							htmltext = "32052-07.htm";
						}
						break;
					}
					case HITCHI:
					{
						if (cond == 9)
						{
							htmltext = "31963-01.htm";
						}
						else if (cond == 10)
						{
							htmltext = "31963-04.htm";
						}
						else if (cond == 11)
						{
							htmltext = "31963-06a.htm";
						}
						else if (cond == 12)
						{
							htmltext = "31963-08.htm";
						}
						break;
					}
					case LOCKIRIN:
					{
						if (cond == 10)
						{
							htmltext = "30531-01.htm";
						}
						break;
					}
					case RYDEL:
					{
						if (cond == 12)
						{
							htmltext = "31956-01.htm";
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
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		switch (npc.getId())
		{
			case 20389:
			{
				if (st.isCond(1) && (getRandom(10) < 7))
				{
					if (getQuestItemsCount(player, BOOGLE_RATMAN_TOOTH) < 10)
					{
						giveItems(player, BOOGLE_RATMAN_TOOTH, 1);
					}
					
					if ((getQuestItemsCount(player, BOOGLE_RATMAN_TOOTH) >= 10) && (getQuestItemsCount(player, BOOGLE_RATMAN_LEADER_TOOTH) >= 2))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20390:
			{
				if (st.isCond(1) && (getRandom(10) < 5))
				{
					if (getQuestItemsCount(player, BOOGLE_RATMAN_LEADER_TOOTH) < 2)
					{
						giveItems(player, BOOGLE_RATMAN_LEADER_TOOTH, 1);
					}
					
					if ((getQuestItemsCount(player, BOOGLE_RATMAN_LEADER_TOOTH) >= 2) && (getQuestItemsCount(player, BOOGLE_RATMAN_TOOTH) >= 10))
					{
						st.setCond(2, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case 20017:
			{
				if (st.isCond(5) && (getRandom(10) < 2))
				{
					giveItems(player, STOLEN_SECRET_BOX, 1);
					st.setCond(6, true);
				}
				break;
			}
		}
	}
}
