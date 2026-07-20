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
package quests.Q00226_TestOfTheHealer;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00226_TestOfTheHealer extends Quest
{
	// NPCs
	private static final int BANDELLOS = 30473;
	private static final int SORIUS = 30327;
	private static final int ALLANA = 30424;
	private static final int PERRIN = 30428;
	private static final int GUPU = 30658;
	private static final int ORPHAN_GIRL = 30659;
	private static final int WINDY_SHAORING = 30660;
	private static final int MYSTERIOUS_DARKELF = 30661;
	private static final int PIPER_LONGBOW = 30662;
	private static final int SLEIN_SHINING_BLADE = 30663;
	private static final int KAIN_FLYING_KNIFE = 30664;
	private static final int KRISTINA = 30665;
	private static final int DAURIN_HAMMERCRUSH = 30674;
	
	// Monsters
	private static final int LETO_LIZARDMAN_LEADER = 27123;
	private static final int LETO_LIZARDMAN_ASSASSIN = 27124;
	private static final int LETO_LIZARDMAN_SNIPER = 27125;
	private static final int LETO_LIZARDMAN_WIZARD = 27126;
	private static final int LETO_LIZARDMAN_LORD = 27127;
	private static final int TATOMA = 27134;
	
	// Items
	private static final int REPORT_OF_PERRIN = 2810;
	private static final int KRISTINA_LETTER = 2811;
	private static final int PICTURE_OF_WINDY = 2812;
	private static final int GOLDEN_STATUE = 2813;
	private static final int WINDY_PEBBLES = 2814;
	private static final int ORDER_OF_SORIUS = 2815;
	private static final int SECRET_LETTER_1 = 2816;
	private static final int SECRET_LETTER_2 = 2817;
	private static final int SECRET_LETTER_3 = 2818;
	private static final int SECRET_LETTER_4 = 2819;
	
	// Rewards
	private static final int MARK_OF_HEALER = 2820;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	// Misc
	private Npc _tatoma;
	private Npc _letoLeader;
	
	public Q00226_TestOfTheHealer()
	{
		super(226, "Test of the Healer");
		registerQuestItems(REPORT_OF_PERRIN, KRISTINA_LETTER, PICTURE_OF_WINDY, GOLDEN_STATUE, WINDY_PEBBLES, ORDER_OF_SORIUS, SECRET_LETTER_1, SECRET_LETTER_2, SECRET_LETTER_3, SECRET_LETTER_4);
		addStartNpc(BANDELLOS);
		addTalkId(BANDELLOS, SORIUS, ALLANA, PERRIN, GUPU, ORPHAN_GIRL, WINDY_SHAORING, MYSTERIOUS_DARKELF, PIPER_LONGBOW, SLEIN_SHINING_BLADE, KAIN_FLYING_KNIFE, KRISTINA, DAURIN_HAMMERCRUSH);
		addKillId(LETO_LIZARDMAN_LEADER, LETO_LIZARDMAN_ASSASSIN, LETO_LIZARDMAN_SNIPER, LETO_LIZARDMAN_WIZARD, LETO_LIZARDMAN_LORD, TATOMA);
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
			case "30473-04.htm":
			{
				st.startQuest();
				giveItems(player, REPORT_OF_PERRIN, 1);
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30473-04a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30473-09.htm":
			{
				takeItems(player, GOLDEN_STATUE, 1);
				giveItems(player, MARK_OF_HEALER, 1);
				addExpAndSp(player, 134839, 50000);
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				st.exitQuest(false, true);
				break;
			}
			case "30428-02.htm":
			{
				st.setCond(2, true);
				if (_tatoma == null)
				{
					_tatoma = addSpawn(TATOMA, -93254, 147559, -2679, 0, false, 0);
					startQuestTimer("tatoma_despawn", 200000, null, player, false);
				}
				break;
			}
			case "30658-02.htm":
			{
				if (getQuestItemsCount(player, 57) >= 100000)
				{
					st.setCond(7, true);
					takeItems(player, 57, 100000);
					giveItems(player, PICTURE_OF_WINDY, 1);
				}
				else
				{
					htmltext = "30658-05.htm";
				}
				break;
			}
			case "30658-03.htm":
			{
				st.set("gupu", "1");
				break;
			}
			case "30658-07.htm":
			{
				st.setCond(9, true);
				break;
			}
			case "30660-03.htm":
			{
				st.setCond(8, true);
				takeItems(player, PICTURE_OF_WINDY, 1);
				giveItems(player, WINDY_PEBBLES, 1);
				break;
			}
			case "30674-02.htm":
			{
				st.setCond(11, true);
				playSound(player, QuestSound.ITEMSOUND_QUEST_BEFORE_BATTLE);
				takeItems(player, ORDER_OF_SORIUS, 1);
				if (_letoLeader == null)
				{
					_letoLeader = addSpawn(LETO_LIZARDMAN_LEADER, -97441, 106585, -3405, 0, false, 0);
					startQuestTimer("leto_leader_despawn", 200000, null, player, false);
				}
				break;
			}
			case "30665-02.htm":
			{
				st.setCond(22, true);
				takeItems(player, SECRET_LETTER_1, 1);
				takeItems(player, SECRET_LETTER_2, 1);
				takeItems(player, SECRET_LETTER_3, 1);
				takeItems(player, SECRET_LETTER_4, 1);
				giveItems(player, KRISTINA_LETTER, 1);
				break;
			}
			case "tatoma_despawn":
			{
				_tatoma.deleteMe();
				_tatoma = null;
				return null;
			}
			case "leto_leader_despawn":
			{
				_letoLeader.deleteMe();
				_letoLeader = null;
				return null;
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
				if ((player.getPlayerClass() != PlayerClass.KNIGHT) && (player.getPlayerClass() != PlayerClass.ELVEN_KNIGHT) && (player.getPlayerClass() != PlayerClass.CLERIC) && (player.getPlayerClass() != PlayerClass.ORACLE))
				{
					htmltext = "30473-01.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30473-02.htm";
				}
				else
				{
					htmltext = "30473-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case BANDELLOS:
					{
						if (cond < 23)
						{
							htmltext = "30473-05.htm";
						}
						else
						{
							if (!hasQuestItems(player, GOLDEN_STATUE))
							{
								htmltext = "30473-06.htm";
								giveItems(player, MARK_OF_HEALER, 1);
								addExpAndSp(player, 118304, 26250);
								player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
								st.exitQuest(false, true);
							}
							else
							{
								htmltext = "30473-07.htm";
							}
						}
						break;
					}
					case PERRIN:
					{
						if (cond < 3)
						{
							htmltext = "30428-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30428-03.htm";
							st.setCond(4, true);
							takeItems(player, REPORT_OF_PERRIN, 1);
						}
						else
						{
							htmltext = "30428-04.htm";
						}
						break;
					}
					case ORPHAN_GIRL:
					{
						htmltext = "30659-0" + getRandom(1, 5) + ".htm";
						break;
					}
					case ALLANA:
					{
						if (cond == 4)
						{
							htmltext = "30424-01.htm";
							st.setCond(5, true);
						}
						else if (cond > 4)
						{
							htmltext = "30424-02.htm";
						}
						break;
					}
					case GUPU:
					{
						if ((st.getInt("gupu") == 1) && (cond != 9))
						{
							htmltext = "30658-07.htm";
							st.setCond(9, true);
						}
						else if (cond == 5)
						{
							htmltext = "30658-01.htm";
							st.setCond(6, true);
						}
						else if (cond == 6)
						{
							htmltext = "30658-01.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30658-04.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30658-06.htm";
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							takeItems(player, WINDY_PEBBLES, 1);
							giveItems(player, GOLDEN_STATUE, 1);
						}
						else if (cond > 8)
						{
							htmltext = "30658-07.htm";
						}
						break;
					}
					case WINDY_SHAORING:
					{
						if (cond == 7)
						{
							htmltext = "30660-01.htm";
						}
						else if (hasQuestItems(player, WINDY_PEBBLES))
						{
							htmltext = "30660-04.htm";
						}
						break;
					}
					case SORIUS:
					{
						if (cond == 9)
						{
							htmltext = "30327-01.htm";
							st.setCond(10, true);
							giveItems(player, ORDER_OF_SORIUS, 1);
						}
						else if ((cond > 9) && (cond < 22))
						{
							htmltext = "30327-02.htm";
						}
						else if (cond == 22)
						{
							htmltext = "30327-03.htm";
							st.setCond(23, true);
							takeItems(player, KRISTINA_LETTER, 1);
						}
						else if (cond == 23)
						{
							htmltext = "30327-04.htm";
						}
						break;
					}
					case DAURIN_HAMMERCRUSH:
					{
						if (cond == 10)
						{
							htmltext = "30674-01.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30674-02a.htm";
							if (_letoLeader == null)
							{
								_letoLeader = addSpawn(LETO_LIZARDMAN_LEADER, -97441, 106585, -3405, 0, false, 0);
								startQuestTimer("leto_leader_despawn", 200000, null, player, false);
							}
						}
						else if (cond == 12)
						{
							htmltext = "30674-03.htm";
							st.setCond(13, true);
						}
						else if (cond > 12)
						{
							htmltext = "30674-04.htm";
						}
						break;
					}
					case PIPER_LONGBOW:
					case SLEIN_SHINING_BLADE:
					case KAIN_FLYING_KNIFE:
					{
						if ((cond == 13) || (cond == 14))
						{
							htmltext = npc.getId() + "-01.htm";
						}
						else if ((cond > 14) && (cond < 19))
						{
							htmltext = npc.getId() + "-02.htm";
						}
						else if ((cond > 18) && (cond < 22))
						{
							htmltext = npc.getId() + "-03.htm";
							st.setCond(21, true);
						}
						break;
					}
					case MYSTERIOUS_DARKELF:
					{
						if (cond == 13)
						{
							htmltext = "30661-01.htm";
							st.setCond(14);
							playSound(player, QuestSound.ITEMSOUND_QUEST_BEFORE_BATTLE);
							addSpawn(LETO_LIZARDMAN_ASSASSIN, player, true, 300000);
							addSpawn(LETO_LIZARDMAN_ASSASSIN, player, true, 300000);
							addSpawn(LETO_LIZARDMAN_ASSASSIN, player, true, 300000);
						}
						else if (cond == 14)
						{
							htmltext = "30661-01.htm";
							checkSpawn(LETO_LIZARDMAN_ASSASSIN, 3, player);
						}
						else if (cond == 15)
						{
							htmltext = "30661-02.htm";
							st.setCond(16);
							playSound(player, QuestSound.ITEMSOUND_QUEST_BEFORE_BATTLE);
							addSpawn(LETO_LIZARDMAN_SNIPER, player, true, 300000);
							addSpawn(LETO_LIZARDMAN_SNIPER, player, true, 300000);
							addSpawn(LETO_LIZARDMAN_SNIPER, player, true, 300000);
						}
						else if (cond == 16)
						{
							htmltext = "30661-02.htm";
							checkSpawn(LETO_LIZARDMAN_SNIPER, 3, player);
						}
						else if (cond == 17)
						{
							htmltext = "30661-03.htm";
							st.setCond(18);
							playSound(player, QuestSound.ITEMSOUND_QUEST_BEFORE_BATTLE);
							addSpawn(LETO_LIZARDMAN_WIZARD, player, true, 300000);
							addSpawn(LETO_LIZARDMAN_WIZARD, player, true, 300000);
							addSpawn(LETO_LIZARDMAN_LORD, player, true, 300000);
						}
						else if (cond == 18)
						{
							htmltext = "30661-03.htm";
							checkSpawn(LETO_LIZARDMAN_WIZARD, 2, player);
							checkSpawn(LETO_LIZARDMAN_LORD, 1, player);
						}
						else if (cond == 19)
						{
							htmltext = "30661-04.htm";
							st.setCond(20, true);
						}
						else if ((cond == 20) || (cond == 21))
						{
							htmltext = "30661-04.htm";
						}
						break;
					}
					case KRISTINA:
					{
						if ((cond > 18) && (cond < 22))
						{
							htmltext = "30665-01.htm";
						}
						else if (cond > 21)
						{
							htmltext = "30665-04.htm";
						}
						else
						{
							htmltext = "30665-03.htm";
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
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		final int cond = st.getCond();
		switch (npc.getId())
		{
			case TATOMA:
			{
				if ((cond == 1) || (cond == 2))
				{
					st.setCond(3, true);
				}
				
				_tatoma = null;
				cancelQuestTimer("tatoma_despawn", null, player);
				break;
			}
			case LETO_LIZARDMAN_LEADER:
			{
				if ((cond == 10) || (cond == 11))
				{
					st.setCond(12, true);
					giveItems(player, SECRET_LETTER_1, 1);
				}
				
				_letoLeader = null;
				cancelQuestTimer("leto_leader_despawn", null, player);
				break;
			}
			case LETO_LIZARDMAN_ASSASSIN:
			{
				if ((cond == 13) || (cond == 14))
				{
					st.setCond(15, true);
					giveItems(player, SECRET_LETTER_2, 1);
				}
				break;
			}
			case LETO_LIZARDMAN_SNIPER:
			{
				if ((cond == 15) || (cond == 16))
				{
					st.setCond(17, true);
					giveItems(player, SECRET_LETTER_3, 1);
				}
				break;
			}
			case LETO_LIZARDMAN_LORD:
			{
				if ((cond == 17) || (cond == 18))
				{
					st.setCond(19, true);
					giveItems(player, SECRET_LETTER_4, 1);
				}
				break;
			}
		}
	}
	
	private void checkSpawn(int npcId, int count, Player player)
	{
		int found = 0;
		while (found < count)
		{
			found = 0;
			for (Npc nearby : World.getInstance().getVisibleObjects(player, Npc.class))
			{
				if (nearby.getId() == npcId)
				{
					found++;
				}
			}
			
			if (found < count)
			{
				addSpawn(npcId, player, true, 300000);
			}
		}
	}
}
