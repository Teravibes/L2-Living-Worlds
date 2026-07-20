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
package quests.Q00108_JumbleTumbleDiamondFuss;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.util.LocationUtil;

public class Q00108_JumbleTumbleDiamondFuss extends Quest
{
	// NPCs
	private static final int GOUPH = 30523;
	private static final int REEP = 30516;
	private static final int MURDOC = 30521;
	private static final int AIRY = 30522;
	private static final int BRUNON = 30526;
	private static final int MARON = 30529;
	private static final int TOROCCO = 30555;
	
	// Monsters
	private static final int GOBLIN_BRIGAND_LEADER = 20323;
	private static final int GOBLIN_BRIGAND_LIEUTENANT = 20324;
	private static final int BLADE_BAT = 20480;
	
	// Items
	private static final int GOUPH_CONTRACT = 1559;
	private static final int REEP_CONTRACT = 1560;
	private static final int ELVEN_WINE = 1561;
	private static final int BRUNON_DICE = 1562;
	private static final int BRUNON_CONTRACT = 1563;
	private static final int AQUAMARINE = 1564;
	private static final int CHRYSOBERYL = 1565;
	private static final int GEM_BOX = 1566;
	private static final int COAL_PIECE = 1567;
	private static final int BRUNON_LETTER = 1568;
	private static final int BERRY_TART = 1569;
	private static final int BAT_DIAGRAM = 1570;
	private static final int STAR_DIAMOND = 1571;
	
	// Rewards
	private static final int SILVERSMITH_HAMMER = 1511;
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	private static final int ECHO_BATTLE = 4412;
	private static final int ECHO_LOVE = 4413;
	private static final int ECHO_SOLITUDE = 4414;
	private static final int ECHO_FEAST = 4415;
	private static final int ECHO_CELEBRATION = 4416;
	private static final int LESSER_HEALING_POTION = 1060;
	
	// Misc
	private static final int MAX_GEM_COUNT = 10;
	private static final Map<Integer, Double> GOBLIN_DROP_CHANCES = new HashMap<>();
	static
	{
		GOBLIN_DROP_CHANCES.put(GOBLIN_BRIGAND_LEADER, 0.8);
		GOBLIN_DROP_CHANCES.put(GOBLIN_BRIGAND_LIEUTENANT, 0.6);
	}
	
	public Q00108_JumbleTumbleDiamondFuss()
	{
		super(108, "Jumble, Tumble, Diamond Fuss");
		registerQuestItems(GOUPH_CONTRACT, REEP_CONTRACT, ELVEN_WINE, BRUNON_DICE, BRUNON_CONTRACT, AQUAMARINE, CHRYSOBERYL, GEM_BOX, COAL_PIECE, BRUNON_LETTER, BERRY_TART, BAT_DIAGRAM, STAR_DIAMOND);
		addStartNpc(GOUPH);
		addTalkId(GOUPH, REEP, MURDOC, AIRY, BRUNON, MARON, TOROCCO);
		addKillId(GOBLIN_BRIGAND_LEADER, GOBLIN_BRIGAND_LIEUTENANT, BLADE_BAT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		final String htmltext = event;
		if (st == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30523-03.htm":
			{
				st.startQuest();
				giveItems(player, GOUPH_CONTRACT, 1);
				break;
			}
			case "30555-02.htm":
			{
				st.setCond(3, true);
				takeItems(player, REEP_CONTRACT, 1);
				giveItems(player, ELVEN_WINE, 1);
				break;
			}
			case "30526-02.htm":
			{
				st.setCond(5, true);
				takeItems(player, BRUNON_DICE, 1);
				giveItems(player, BRUNON_CONTRACT, 1);
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
				if (player.getRace() != Race.DWARF)
				{
					htmltext = "30523-00.htm";
				}
				else if (player.getLevel() < 10)
				{
					htmltext = "30523-01.htm";
				}
				else
				{
					htmltext = "30523-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case GOUPH:
					{
						if (cond == 1)
						{
							htmltext = "30523-04.htm";
						}
						else if ((cond > 1) && (cond < 7))
						{
							htmltext = "30523-05.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30523-06.htm";
							st.setCond(8, true);
							takeItems(player, GEM_BOX, 1);
							giveItems(player, COAL_PIECE, 1);
						}
						else if ((cond > 7) && (cond < 12))
						{
							htmltext = "30523-07.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30523-08.htm";
							takeItems(player, STAR_DIAMOND, -1);
							giveItems(player, SILVERSMITH_HAMMER, 1);
							giveItems(player, LESSER_HEALING_POTION, 100);
							
							// Give newbie reward if player is eligible
							if (player.isNewbie())
							{
								int newPlayerRewardsReceived = player.getVariables().getInt(PlayerVariables.NEWBIE_SHOTS_RECEIVED, 0);
								if (newPlayerRewardsReceived < 2)
								{
									st.showQuestionMark(26);
									if (player.isMageClass())
									{
										st.playTutorialVoice("tutorial_voice_027");
										giveItems(player, SPIRITSHOT_FOR_BEGINNERS, 3000);
										player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
									}
									else
									{
										st.playTutorialVoice("tutorial_voice_026");
										giveItems(player, SOULSHOT_FOR_BEGINNERS, 7000);
										player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
									}
								}
							}
							
							giveItems(player, ECHO_BATTLE, 10);
							giveItems(player, ECHO_LOVE, 10);
							giveItems(player, ECHO_SOLITUDE, 10);
							giveItems(player, ECHO_FEAST, 10);
							giveItems(player, ECHO_CELEBRATION, 10);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case REEP:
					{
						if (cond == 1)
						{
							htmltext = "30516-01.htm";
							st.setCond(2, true);
							takeItems(player, GOUPH_CONTRACT, 1);
							giveItems(player, REEP_CONTRACT, 1);
						}
						else if (cond > 1)
						{
							htmltext = "30516-02.htm";
						}
						break;
					}
					case TOROCCO:
					{
						if (cond == 2)
						{
							htmltext = "30555-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30555-03.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30555-04.htm";
						}
						else if (cond > 7)
						{
							htmltext = "30555-05.htm";
						}
						break;
					}
					case MARON:
					{
						if (cond == 3)
						{
							htmltext = "30529-01.htm";
							st.setCond(4, true);
							takeItems(player, ELVEN_WINE, 1);
							giveItems(player, BRUNON_DICE, 1);
						}
						else if (cond == 4)
						{
							htmltext = "30529-02.htm";
						}
						else if (cond > 4)
						{
							htmltext = "30529-03.htm";
						}
						break;
					}
					case BRUNON:
					{
						if (cond == 4)
						{
							htmltext = "30526-01.htm";
						}
						else if (cond == 5)
						{
							htmltext = "30526-03.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30526-04.htm";
							st.setCond(7, true);
							takeItems(player, BRUNON_CONTRACT, 1);
							takeItems(player, AQUAMARINE, -1);
							takeItems(player, CHRYSOBERYL, -1);
							giveItems(player, GEM_BOX, 1);
						}
						else if (cond == 7)
						{
							htmltext = "30526-05.htm";
						}
						else if (cond == 8)
						{
							htmltext = "30526-06.htm";
							st.setCond(9, true);
							takeItems(player, COAL_PIECE, 1);
							giveItems(player, BRUNON_LETTER, 1);
						}
						else if (cond == 9)
						{
							htmltext = "30526-07.htm";
						}
						else if (cond > 9)
						{
							htmltext = "30526-08.htm";
						}
						break;
					}
					case MURDOC:
					{
						if (cond == 9)
						{
							htmltext = "30521-01.htm";
							st.setCond(10, true);
							takeItems(player, BRUNON_LETTER, 1);
							giveItems(player, BERRY_TART, 1);
						}
						else if (cond == 10)
						{
							htmltext = "30521-02.htm";
						}
						else if (cond > 10)
						{
							htmltext = "30521-03.htm";
						}
						break;
					}
					case AIRY:
					{
						if (cond == 10)
						{
							htmltext = "30522-01.htm";
							st.setCond(11, true);
							takeItems(player, BERRY_TART, 1);
							giveItems(player, BAT_DIAGRAM, 1);
						}
						else if (cond == 11)
						{
							htmltext = getRandomBoolean() ? "30522-02.htm" : "30522-04.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30522-03.htm";
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, npc, killer, true))
		{
			switch (npc.getId())
			{
				case GOBLIN_BRIGAND_LEADER:
				case GOBLIN_BRIGAND_LIEUTENANT:
				{
					if (qs.isCond(5) && hasQuestItems(killer, BRUNON_CONTRACT))
					{
						final double dropChance = GOBLIN_DROP_CHANCES.get(npc.getId());
						boolean playSound = false;
						if (giveItemRandomly(killer, npc, AQUAMARINE, 1, MAX_GEM_COUNT, dropChance, false))
						{
							if (getQuestItemsCount(killer, CHRYSOBERYL) >= MAX_GEM_COUNT)
							{
								qs.setCond(6, true);
								break;
							}
							
							playSound = true;
						}
						
						if (giveItemRandomly(killer, npc, CHRYSOBERYL, 1, MAX_GEM_COUNT, dropChance, false))
						{
							if (getQuestItemsCount(killer, AQUAMARINE) >= MAX_GEM_COUNT)
							{
								qs.setCond(6, true);
								break;
							}
							
							playSound = true;
						}
						
						if (playSound)
						{
							playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				}
				case BLADE_BAT:
				{
					if (qs.isCond(11) && hasQuestItems(killer, BAT_DIAGRAM) && giveItemRandomly(killer, npc, STAR_DIAMOND, 1, 1, 0.2, true))
					{
						takeItems(killer, BAT_DIAGRAM, -1);
						qs.setCond(12);
					}
					break;
				}
			}
		}
	}
}
