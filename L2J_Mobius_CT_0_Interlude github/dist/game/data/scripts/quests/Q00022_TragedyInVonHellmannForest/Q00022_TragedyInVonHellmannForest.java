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
package quests.Q00022_TragedyInVonHellmannForest;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.QuestTimer;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.LocationUtil;

import quests.Q00021_HiddenTruth.Q00021_HiddenTruth;

public class Q00022_TragedyInVonHellmannForest extends Quest
{
	// NPCs
	private static final int INNOCENTIN = 31328;
	private static final int TIFAREN = 31334;
	private static final int WELL = 31527;
	private static final int GHOST_OF_PRIEST = 31528;
	private static final int GHOST_OF_ADVENTURER = 31529;
	
	// Monsters
	private static final int[] MOBS =
	{
		21553, // Trampled Man
		21554, // Trampled Man
		21555, // Slaughter Executioner
		21556, // Slaughter Executioner
		21561, // Sacrificed Man
	};
	private static final int SOUL_OF_WELL = 27217;
	
	// Items
	private static final int CROSS_OF_EINHASAD = 7141;
	private static final int LOST_SKULL_OF_ELF = 7142;
	private static final int LETTER_OF_INNOCENTIN = 7143;
	private static final int JEWEL_OF_ADVENTURER_1 = 7144;
	private static final int JEWEL_OF_ADVENTURER_2 = 7145;
	private static final int SEALED_REPORT_BOX = 7146;
	private static final int REPORT_BOX = 7147;
	
	// Misc
	private static final int MIN_LEVEL = 63;
	private static final Location PRIEST_LOC = new Location(38354, -49777, -1128);
	private static final Location SOUL_WELL_LOC = new Location(34706, -54590, -2054);
	private static int _tifarenOwner = 0;
	private static Npc _soulWellNpc = null;
	
	public Q00022_TragedyInVonHellmannForest()
	{
		super(22, "Tragedy in von Hellmann Forest");
		addKillId(MOBS);
		addKillId(SOUL_OF_WELL);
		addAttackId(SOUL_OF_WELL);
		addStartNpc(TIFAREN);
		addTalkId(INNOCENTIN, TIFAREN, WELL, GHOST_OF_PRIEST, GHOST_OF_ADVENTURER);
		registerQuestItems(LOST_SKULL_OF_ELF, CROSS_OF_EINHASAD, REPORT_BOX, JEWEL_OF_ADVENTURER_1, JEWEL_OF_ADVENTURER_2, SEALED_REPORT_BOX);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState(getName());
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "31529-02.htm":
			case "31529-04.htm":
			case "31529-05.htm":
			case "31529-06.htm":
			case "31529-07.htm":
			case "31529-09.htm":
			case "31529-13.htm":
			case "31529-13a.htm":
			case "31528-02.htm":
			case "31528-05.htm":
			case "31528-06.htm":
			case "31528-07.htm":
			case "31328-13.htm":
			case "31328-06.htm":
			case "31328-05.htm":
			case "31328-02.htm":
			case "31328-07.htm":
			case "31328-08.htm":
			case "31328-14.htm":
			case "31328-15.htm":
			case "31328-16.htm":
			case "31328-17.htm":
			case "31328-18.htm":
			case "31334-12.htm":
			{
				htmltext = event;
				break;
			}
			case "31334-02.htm":
			{
				if (qs.isCreated())
				{
					final QuestState qs2 = player.getQuestState(Q00021_HiddenTruth.class.getSimpleName());
					if ((player.getLevel() >= MIN_LEVEL) && (qs2 != null) && qs2.isCompleted())
					{
						htmltext = event;
					}
					else
					{
						htmltext = "31334-03.htm";
					}
				}
				break;
			}
			case "31334-04.htm":
			{
				if (qs.isCreated())
				{
					qs.startQuest();
					htmltext = event;
				}
				break;
			}
			case "31334-07.htm":
			{
				if (!hasQuestItems(player, CROSS_OF_EINHASAD))
				{
					qs.setCond(2);
					htmltext = event;
				}
				else
				{
					htmltext = "31334-06.htm";
					qs.setCond(3);
				}
				break;
			}
			case "31334-08.htm":
			{
				if (qs.isCond(3))
				{
					qs.setCond(4, true);
					htmltext = event;
				}
				break;
			}
			case "31334-13.htm":
			{
				final int cond = qs.getCond();
				if (((5 <= cond) && (cond <= 7)) && hasQuestItems(player, CROSS_OF_EINHASAD))
				{
					if (_tifarenOwner == 0)
					{
						_tifarenOwner = player.getObjectId();
						final Npc ghost2 = addSpawn(GHOST_OF_PRIEST, PRIEST_LOC, true, 0);
						ghost2.setScriptValue(player.getObjectId());
						startQuestTimer("DESPAWN_GHOST2", 1000 * 120, ghost2, player);
						ghost2.broadcastSay(ChatType.GENERAL, "Did you call me, " + player.getName() + "?");
						if (((cond == 5) || (cond == 6)) && hasQuestItems(player, LOST_SKULL_OF_ELF))
						{
							takeItems(player, LOST_SKULL_OF_ELF, -1);
							qs.setCond(7, true);
						}
						
						htmltext = event;
					}
					else
					{
						qs.setCond(6);
						htmltext = "31334-14.htm";
					}
				}
				break;
			}
			case "31528-04.htm":
			{
				if (npc.getScriptValue() == player.getObjectId())
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
					htmltext = event;
				}
				break;
			}
			case "31528-08.htm":
			{
				final QuestTimer qt = getQuestTimer("DESPAWN_GHOST2", npc, player);
				if ((qt != null) && (npc.getScriptValue() == player.getObjectId()))
				{
					qt.cancel();
					npc.setScriptValue(0);
					startQuestTimer("DESPAWN_GHOST2", 1000 * 3, npc, player);
					qs.setCond(8, true);
					htmltext = event;
				}
				break;
			}
			case "DESPAWN_GHOST2":
			{
				_tifarenOwner = 0;
				if (npc.getScriptValue() != 0)
				{
					npc.broadcastSay(ChatType.GENERAL, "I_M_CONFUSED_MAYBE_IT_S_TIME_TO_GO_BACK");
				}
				
				npc.deleteMe();
				break;
			}
			case "31328-03.htm":
			{
				if (qs.isCond(8))
				{
					takeItems(player, CROSS_OF_EINHASAD, -1);
					htmltext = event;
				}
				break;
			}
			case "31328-09.htm":
			{
				if (qs.isCond(8))
				{
					giveItems(player, LETTER_OF_INNOCENTIN, 1);
					qs.setCond(9, true);
					htmltext = event;
				}
				break;
			}
			case "31328-11.htm":
			{
				if (qs.isCond(14) && hasQuestItems(player, REPORT_BOX))
				{
					takeItems(player, REPORT_BOX, -1);
					qs.setCond(15, true);
					htmltext = event;
				}
				break;
			}
			case "31328-19.htm":
			{
				if (qs.isCond(15))
				{
					qs.setCond(16, true);
					htmltext = event;
				}
				break;
			}
			case "31527-02.htm":
			{
				if (qs.isCond(10) && (_soulWellNpc == null))
				{
					_soulWellNpc = addSpawn(SOUL_OF_WELL, SOUL_WELL_LOC, true, 0);
					startQuestTimer("activateSoulOfWell", 90000, _soulWellNpc, player);
					startQuestTimer("despawnSoulOfWell", 120000, _soulWellNpc, player);
					_soulWellNpc.getAI().setIntention(Intention.ATTACK, player);
					
					htmltext = event;
				}
				else
				{
					htmltext = "31527-03.htm";
				}
				break;
			}
			case "activateSoulOfWell":
			{
				// this enables onAttack ELSE IF block which allows the player to proceed the quest
				npc.setScriptValue(1);
				break;
			}
			case "despawnSoulOfWell":
			{
				// if the player fails to proceed the quest in 2 minutes, the soul is unspawned
				if (!npc.isDead())
				{
					_soulWellNpc = null;
				}
				
				npc.deleteMe();
				break;
			}
			case "31529-03.htm":
			{
				if (qs.isCond(9) && hasQuestItems(player, LETTER_OF_INNOCENTIN))
				{
					qs.set("memoState", "8");
					htmltext = event;
				}
				break;
			}
			case "31529-08.htm":
			{
				if (qs.getInt("memoState") == 8)
				{
					qs.set("memoState", "9");
					htmltext = event;
				}
				break;
			}
			case "31529-11.htm":
			{
				if (qs.getInt("memoState") == 9)
				{
					giveItems(player, JEWEL_OF_ADVENTURER_1, 1);
					qs.setCond(10, true);
					qs.set("memoState", "10");
					htmltext = event;
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case TIFAREN:
			{
				switch (qs.getCond())
				{
					case 0:
					{
						if (qs.isCreated())
						{
							htmltext = "31334-01.htm";
						}
						else if (qs.isCompleted())
						{
							htmltext = getAlreadyCompletedMsg(player);
						}
						break;
					}
					case 1:
					case 3:
					{
						htmltext = "31334-05.htm";
						break;
					}
					case 4:
					case 5:
					{
						if (hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							if (!hasQuestItems(player, LOST_SKULL_OF_ELF))
							{
								htmltext = "31334-09.htm";
							}
							else if (_tifarenOwner == 0)
							{
								htmltext = "31334-10.htm";
							}
							else
							{
								htmltext = "31334-11.htm";
							}
						}
						break;
					}
					case 6:
					case 7:
					{
						if (hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							if (_tifarenOwner == 0)
							{
								htmltext = "31334-17.htm";
							}
							else if (_tifarenOwner == player.getObjectId())
							{
								htmltext = "31334-15.htm";
							}
							else
							{
								htmltext = "31334-16.htm";
								qs.setCond(6);
							}
						}
						break;
					}
					case 8:
					{
						if (hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							htmltext = "31334-18.htm";
						}
						break;
					}
				}
				break;
			}
			case GHOST_OF_PRIEST:
			{
				if (npc.getScriptValue() == player.getObjectId())
				{
					htmltext = "31528-01.htm";
				}
				else
				{
					htmltext = "31528-03.htm";
				}
				break;
			}
			case INNOCENTIN:
			{
				switch (qs.getCond())
				{
					case 2:
					{
						if (!hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							giveItems(player, CROSS_OF_EINHASAD, 1);
							qs.setCond(3);
							htmltext = "31328-01.htm";
						}
						break;
					}
					case 3:
					{
						if (hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							htmltext = "31328-01b.htm";
						}
						break;
					}
					case 8:
					{
						if (hasQuestItems(player, CROSS_OF_EINHASAD))
						{
							htmltext = "31328-02.htm";
						}
						else
						{
							htmltext = "31328-04.htm";
						}
						break;
					}
					case 9:
					{
						htmltext = "31328-09a.htm";
						break;
					}
					case 14:
					{
						if (hasQuestItems(player, REPORT_BOX))
						{
							htmltext = "31328-10.htm";
						}
						break;
					}
					case 15:
					{
						htmltext = "31328-12.htm";
						break;
					}
					case 16:
					{
						addExpAndSp(player, 345966, 31578);
						qs.exitQuest(false);
						if (player.getLevel() >= MIN_LEVEL)
						{
							htmltext = "31328-20.htm";
						}
						else
						{
							htmltext = "31328-21.htm";
						}
						break;
					}
				}
				break;
			}
			case WELL:
			{
				switch (qs.getCond())
				{
					case 10:
					{
						if (hasQuestItems(player, JEWEL_OF_ADVENTURER_1))
						{
							htmltext = "31527-01.htm";
							
						}
						break;
					}
					case 12:
					{
						if (hasQuestItems(player, JEWEL_OF_ADVENTURER_2) && !hasQuestItems(player, SEALED_REPORT_BOX))
						{
							giveItems(player, SEALED_REPORT_BOX, 1);
							qs.setCond(13);
							htmltext = "31527-04.htm";
						}
						break;
					}
					case 13:
					case 14:
					case 15:
					case 16:
					{
						htmltext = "31527-05.htm";
						break;
					}
				}
				break;
			}
			case GHOST_OF_ADVENTURER:
			{
				switch (qs.getCond())
				{
					case 9:
					{
						if (hasQuestItems(player, LETTER_OF_INNOCENTIN))
						{
							switch (qs.getInt("memoState"))
							{
								case 0:
								{
									htmltext = "31529-01.htm";
									break;
								}
								case 8:
								{
									htmltext = "31529-03a.htm";
									break;
								}
								case 9:
								{
									htmltext = "31529-10.htm";
									break;
								}
								default:
								{
									break;
								}
							}
						}
						break;
					}
					case 10:
					{
						if (hasQuestItems(player, JEWEL_OF_ADVENTURER_1))
						{
							final int id = qs.getInt("memoState");
							if (id == 10)
							{
								htmltext = "31529-12.htm";
							}
							else if (id == 11)
							{
								htmltext = "31529-14.htm";
							}
						}
						break;
					}
					case 11:
					{
						if (hasQuestItems(player, JEWEL_OF_ADVENTURER_2) && !hasQuestItems(player, SEALED_REPORT_BOX))
						{
							htmltext = "31529-15.htm";
							qs.setCond(12);
						}
						break;
					}
					case 13:
					{
						if (hasQuestItems(player, JEWEL_OF_ADVENTURER_2) && hasQuestItems(player, SEALED_REPORT_BOX))
						{
							giveItems(player, REPORT_BOX, 1);
							takeItems(player, SEALED_REPORT_BOX, -1);
							takeItems(player, JEWEL_OF_ADVENTURER_2, -1);
							qs.setCond(14);
							htmltext = "31529-16.htm";
						}
						break;
					}
					case 14:
					{
						if (hasQuestItems(player, REPORT_BOX))
						{
							htmltext = "31529-17.htm";
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
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final QuestState qs = attacker.getQuestState(getName());
		if ((qs != null) && qs.isCond(10) && hasQuestItems(attacker, JEWEL_OF_ADVENTURER_1))
		{
			if (qs.getInt("memoState") == 10)
			{
				qs.set("memoState", "11");
			}
			else if (npc.isScriptValue(1))
			{
				takeItems(attacker, JEWEL_OF_ADVENTURER_1, -1);
				giveItems(attacker, JEWEL_OF_ADVENTURER_2, 1);
				qs.setCond(11);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, killer, npc, true))
		{
			if (npc.getId() == SOUL_OF_WELL)
			{
				_soulWellNpc = null;
			}
			else
			{
				final QuestState qs = killer.getQuestState(getName());
				if ((qs != null) && qs.isCond(4) && hasQuestItems(killer, CROSS_OF_EINHASAD) && !hasQuestItems(killer, LOST_SKULL_OF_ELF) && (getRandom(100) < 10))
				{
					giveItems(killer, LOST_SKULL_OF_ELF, 1);
					qs.setCond(5, true);
				}
			}
		}
	}
}
