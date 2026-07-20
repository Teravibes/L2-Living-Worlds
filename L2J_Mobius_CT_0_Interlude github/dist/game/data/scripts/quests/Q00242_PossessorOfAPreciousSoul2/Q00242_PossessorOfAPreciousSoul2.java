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
package quests.Q00242_PossessorOfAPreciousSoul2;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00242_PossessorOfAPreciousSoul2 extends Quest
{
	// NPCs
	private static final int VIRGIL = 31742;
	private static final int KASSANDRA = 31743;
	private static final int OGMAR = 31744;
	private static final int MYSTERIOUS_KNIGHT = 31751;
	private static final int ANGEL_CORPSE = 31752;
	private static final int KALIS = 30759;
	private static final int MATILD = 30738;
	private static final int CORNERSTONE = 31748;
	private static final int FALLEN_UNICORN = 31746;
	private static final int PURE_UNICORN = 31747;
	
	// Monsters
	private static final int RESTRAINER_OF_GLORY = 27317;
	
	// Items
	private static final int VIRGIL_LETTER = 7677;
	private static final int GOLDEN_HAIR = 7590;
	private static final int SORCERY_INGREDIENT = 7596;
	private static final int ORB_OF_BINDING = 7595;
	private static final int CARADINE_LETTER = 7678;
	
	// Misc
	private static boolean _unicorn = false;
	
	public Q00242_PossessorOfAPreciousSoul2()
	{
		super(242, "Possessor of a Precious Soul - 2");
		registerQuestItems(GOLDEN_HAIR, SORCERY_INGREDIENT, ORB_OF_BINDING);
		addStartNpc(VIRGIL);
		addTalkId(VIRGIL, KASSANDRA, OGMAR, MYSTERIOUS_KNIGHT, ANGEL_CORPSE, KALIS, MATILD, CORNERSTONE, FALLEN_UNICORN, PURE_UNICORN);
		addKillId(RESTRAINER_OF_GLORY);
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
			case "31743-05.htm":
			{
				st.setCond(2, true);
				break;
			}
			case "31744-02.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "31751-02.htm":
			{
				st.setCond(4, true);
				st.set("angel", "0");
				break;
			}
			case "30759-02.htm":
			{
				st.setCond(7, true);
				break;
			}
			case "30759-05.htm":
			{
				if (hasQuestItems(player, SORCERY_INGREDIENT))
				{
					st.set("orb", "0");
					st.set("cornerstone", "0");
					st.setCond(9, true);
					takeItems(player, GOLDEN_HAIR, 1);
					takeItems(player, SORCERY_INGREDIENT, 1);
				}
				else
				{
					st.setCond(7);
					htmltext = "30759-02.htm";
				}
				break;
			}
			case "30738-02.htm":
			{
				st.setCond(8, true);
				giveItems(player, SORCERY_INGREDIENT, 1);
				break;
			}
			case "31748-03.htm":
			{
				if (hasQuestItems(player, ORB_OF_BINDING))
				{
					npc.doDie(npc);
					takeItems(player, ORB_OF_BINDING, 1);
					
					int cornerstones = st.getInt("cornerstone");
					cornerstones++;
					if (cornerstones == 4)
					{
						st.unset("orb");
						st.unset("cornerstone");
						st.setCond(10, true);
					}
					else
					{
						st.set("cornerstone", Integer.toString(cornerstones));
					}
				}
				else
				{
					htmltext = null;
				}
				break;
			}
			case "spu":
			{
				addSpawn(PURE_UNICORN, 85884, -76588, -3470, 0, false, 0);
				return null;
			}
			case "dspu":
			{
				npc.getSpawn().stopRespawn();
				npc.deleteMe();
				startQuestTimer("sfu", 2000, null, player, false);
				return null;
			}
			case "sfu":
			{
				final Npc unicorn = addSpawn(FALLEN_UNICORN, 85884, -76588, -3470, 0, false, 0);
				unicorn.getSpawn().startRespawn();
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
				if (hasQuestItems(player, VIRGIL_LETTER))
				{
					if (!player.isSubClassActive() || (player.getLevel() < 60))
					{
						htmltext = "31742-02.htm";
					}
					else
					{
						st.startQuest();
						takeItems(player, VIRGIL_LETTER, 1);
						htmltext = "31742-03.htm";
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
				
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case VIRGIL:
					{
						if (cond == 1)
						{
							htmltext = "31742-04.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31742-05.htm";
						}
						break;
					}
					case KASSANDRA:
					{
						if (cond == 1)
						{
							htmltext = "31743-01.htm";
						}
						else if (cond == 2)
						{
							htmltext = "31743-06.htm";
						}
						else if (cond == 11)
						{
							htmltext = "31743-07.htm";
							giveItems(player, CARADINE_LETTER, 1);
							addExpAndSp(player, 455764, 0);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case OGMAR:
					{
						if (cond == 2)
						{
							htmltext = "31744-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "31744-03.htm";
						}
						break;
					}
					case MYSTERIOUS_KNIGHT:
					{
						if (cond == 3)
						{
							htmltext = "31751-01.htm";
						}
						else if (cond == 4)
						{
							htmltext = "31751-03.htm";
						}
						else if (cond == 5)
						{
							if (hasQuestItems(player, GOLDEN_HAIR))
							{
								htmltext = "31751-04.htm";
								st.setCond(6, true);
							}
							else
							{
								htmltext = "31751-03.htm";
								st.setCond(4);
							}
						}
						else if (cond == 6)
						{
							htmltext = "31751-05.htm";
						}
						break;
					}
					case ANGEL_CORPSE:
					{
						if (cond == 4)
						{
							npc.doDie(npc);
							int hair = st.getInt("angel");
							hair++;
							
							if (hair == 4)
							{
								htmltext = "31752-02.htm";
								st.unset("angel");
								st.setCond(5, true);
								giveItems(player, GOLDEN_HAIR, 1);
							}
							else
							{
								st.set("angel", Integer.toString(hair));
								htmltext = "31752-01.htm";
							}
						}
						else if (cond == 5)
						{
							htmltext = "31752-01.htm";
						}
						break;
					}
					case KALIS:
					{
						if (cond == 6)
						{
							htmltext = "30759-01.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30759-03.htm";
						}
						else if (cond == 8)
						{
							if (hasQuestItems(player, SORCERY_INGREDIENT))
							{
								htmltext = "30759-04.htm";
							}
							else
							{
								htmltext = "30759-03.htm";
								st.setCond(7);
							}
						}
						else if (cond == 9)
						{
							htmltext = "30759-06.htm";
						}
						break;
					}
					case MATILD:
					{
						if (cond == 7)
						{
							htmltext = "30738-01.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30738-03.htm";
						}
						break;
					}
					case CORNERSTONE:
					{
						if (cond == 9)
						{
							if (hasQuestItems(player, ORB_OF_BINDING))
							{
								htmltext = "31748-02.htm";
							}
							else
							{
								htmltext = "31748-01.htm";
							}
						}
						break;
					}
					case FALLEN_UNICORN:
					{
						if (cond == 9)
						{
							htmltext = "31746-01.htm";
						}
						else if (cond == 10)
						{
							if (!_unicorn) // Global variable check to prevent multiple spawns
							{
								_unicorn = true;
								npc.getSpawn().stopRespawn(); // Despawn fallen unicorn
								npc.deleteMe();
								startQuestTimer("spu", 3000, npc, player, false);
							}
							
							htmltext = "31746-02.htm";
						}
						break;
					}
					case PURE_UNICORN:
					{
						if (cond == 10)
						{
							st.setCond(11, true);
							if (_unicorn) // Global variable check to prevent multiple spawns
							{
								_unicorn = false;
								startQuestTimer("dspu", 3000, npc, player, false);
							}
							
							htmltext = "31747-01.htm";
						}
						else if (cond == 11)
						{
							htmltext = "31747-02.htm";
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
		if ((st == null) || !st.isCond(9) || !player.isSubClassActive())
		{
			return;
		}
		
		int orbs = st.getInt("orb"); // check orbs internally, because player can use them before he gets them all
		if (orbs < 4)
		{
			orbs++;
			st.set("orb", Integer.toString(orbs));
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			giveItems(player, ORB_OF_BINDING, 1);
		}
	}
}
