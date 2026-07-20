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
package quests.Q00503_PursuitOfClanAmbition;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * @author Jackass
 */
public class Q00503_PursuitOfClanAmbition extends Quest
{
	// NPCs
	private static final int MARTIEN = 30645;
	private static final int ATHREA = 30758;
	private static final int KALIS = 30759;
	private static final int GUSTAF = 30760;
	private static final int FRITZ = 30761;
	private static final int LUTZ = 30762;
	private static final int KURTZ = 30763;
	private static final int KUSTO = 30512;
	private static final int BALTHAZAR = 30764;
	private static final int RODEMAI = 30868;
	private static final int COFFER = 30765;
	private static final int CLEO = 30766;
	
	// Monsters
	private static final int THUNDER_WYRM = 20282;
	private static final int THUNDER_WYRM_TWO = 20243;
	private static final int DRAKE = 20137;
	private static final int DRAKE_TWO = 20285;
	private static final int BLITZ_WYRM = 27178;
	private static final int GIANT_SOLDIER = 20654;
	private static final int GIANT_SCOUT = 20656;
	private static final int GRAVE_GUARD = 20668;
	private static final int GRAVE_KEYMASTER = 27179;
	private static final int IMPERIAL_SLAVE = 27180;
	
	// Attack mob
	private static final int IMPERIAL_GRAVEKEEPER = 27181;
	
	// First part items
	private static final int G_LET_MARTIEN = 3866;
	private static final int TH_WYRM_EGGS = 3842;
	private static final int DRAKE_EGGS = 3841;
	private static final int BL_WYRM_EGGS = 3840;
	private static final int MI_DRAKE_EGGS = 3839;
	private static final int BROOCH = 3843;
	private static final int BL_ANVIL_COIN = 3871;
	
	// Second part items
	private static final int G_LET_BALTHAZAR = 3867;
	private static final int RECIPE_POWER_STONE = 3838;
	private static final int POWER_STONE = 3846;
	private static final int NEBULITE_CRYSTALS = 3844;
	private static final int BROKE_POWER_STONE = 3845;
	
	// Third part items
	private static final int G_LET_RODEMAI = 3868;
	private static final int IMP_KEYS = 3847;
	private static final int SCEPTER_JUDGEMENT = 3869;
	
	// Final item
	private static final int PROOF_ASPIRATION = 3870;
	
	// Droplist
	private static final int[][] DROPLIST =
	{
		// npcId, cond, MaxCount, chance, item1, item2 (giants), item3 (giants)
		// @formatter:off
		{THUNDER_WYRM, 2, 10, 200000, TH_WYRM_EGGS, 0, 0},
		{THUNDER_WYRM_TWO, 2, 10, 150000, TH_WYRM_EGGS, 0, 0},
		{DRAKE, 2, 10, 200000, DRAKE_EGGS, 0, 0},
		{DRAKE_TWO, 2, 10, 250000, DRAKE_EGGS, 0, 0},
		{BLITZ_WYRM, 2, 10, 1000000, BL_WYRM_EGGS, 0, 0},
		{GIANT_SOLDIER, 5, 10, 250000, NEBULITE_CRYSTALS, BROKE_POWER_STONE, POWER_STONE},
		{GIANT_SCOUT, 5, 10, 350000, NEBULITE_CRYSTALS, BROKE_POWER_STONE, POWER_STONE},
		{GRAVE_GUARD, 10, 0, 150000, 0, 0, 0},
		{GRAVE_KEYMASTER, 10, 6, 800000, IMP_KEYS, 0, 0},
		{IMPERIAL_GRAVEKEEPER, 10, 0, 0, 0, 0, 0}
		// @formatter:on
	};
	
	public Q00503_PursuitOfClanAmbition()
	{
		super(503, "Pursuit of Clan Ambition!");
		registerQuestItems(MI_DRAKE_EGGS, BL_WYRM_EGGS, DRAKE_EGGS, TH_WYRM_EGGS, BROOCH, NEBULITE_CRYSTALS, BROKE_POWER_STONE, POWER_STONE, IMP_KEYS, G_LET_MARTIEN, G_LET_BALTHAZAR, G_LET_RODEMAI, SCEPTER_JUDGEMENT);
		addStartNpc(GUSTAF);
		addTalkId(MARTIEN, ATHREA, KALIS, GUSTAF, FRITZ, LUTZ, KURTZ, KUSTO, BALTHAZAR, RODEMAI, COFFER, CLEO);
		addKillId(THUNDER_WYRM_TWO, THUNDER_WYRM, DRAKE, DRAKE_TWO, BLITZ_WYRM, GIANT_SOLDIER, GIANT_SCOUT, GRAVE_GUARD, GRAVE_KEYMASTER, IMPERIAL_GRAVEKEEPER);
		addAttackId(IMPERIAL_GRAVEKEEPER);
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
			case "30760-08.htm":
			{
				st.startQuest();
				giveItems(player, G_LET_MARTIEN, 1);
				break;
			}
			case "30760-12.htm":
			{
				giveItems(player, G_LET_BALTHAZAR, 1);
				st.setCond(4);
				break;
			}
			case "30760-16.htm":
			{
				giveItems(player, G_LET_RODEMAI, 1);
				st.setCond(7);
				break;
			}
			case "30760-20.htm":
			{
				takeItems(player, SCEPTER_JUDGEMENT, -1);
				giveItems(player, PROOF_ASPIRATION, 1);
				addExpAndSp(player, 0, 250000);
				st.exitQuest(false, true);
				finishQuestToClan(player);
				break;
			}
			case "30760-22.htm":
			{
				st.setCond(1);
				break;
			}
			case "30760-23.htm":
			{
				takeItems(player, SCEPTER_JUDGEMENT, -1);
				giveItems(player, PROOF_ASPIRATION, 1);
				addExpAndSp(player, 0, 250000);
				st.exitQuest(false, true);
				finishQuestToClan(player);
				break;
			}
			case "30645-03.htm":
			{
				setQuestToClanMembers(player);
				takeItems(player, G_LET_MARTIEN, -1);
				st.setCond(2);
				st.set("kurt", "0");
				break;
			}
			case "30763-02.htm":
			{
				giveItems(player, MI_DRAKE_EGGS, 6);
				giveItems(player, BROOCH, 1);
				st.set("kurt", "1");
				break;
			}
			case "30762-02.htm":
			{
				giveItems(player, MI_DRAKE_EGGS, 4);
				giveItems(player, BL_WYRM_EGGS, 3);
				addSpawn(BLITZ_WYRM, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
				addSpawn(BLITZ_WYRM, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
				st.set("lutz", "1");
				break;
			}
			case "30761-02.htm":
			{
				giveItems(player, BL_WYRM_EGGS, 3);
				addSpawn(BLITZ_WYRM, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
				addSpawn(BLITZ_WYRM, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
				st.set("fritz", "1");
				break;
			}
			case "30512-03.htm":
			{
				takeItems(player, BROOCH, 1);
				giveItems(player, BL_ANVIL_COIN, 1);
				st.set("kurt", "2");
				break;
			}
			case "30764-03.htm":
			{
				takeItems(player, G_LET_BALTHAZAR, -1);
				st.setCond(5);
				break;
			}
			case "30764-05.htm":
			{
				takeItems(player, G_LET_BALTHAZAR, -1);
				st.setCond(5);
				break;
			}
			case "30764-06.htm":
			{
				takeItems(player, BL_ANVIL_COIN, -1);
				giveItems(player, RECIPE_POWER_STONE, 1);
				break;
			}
			case "30868-04.htm":
			{
				takeItems(player, G_LET_RODEMAI, -1);
				st.setCond(8);
				break;
			}
			case "30868-06a.htm":
			{
				st.setCond(10);
				break;
			}
			case "30868-10.htm":
			{
				st.setCond(12);
				break;
			}
			case "30766-04.htm":
			{
				st.setCond(9);
				npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, "Blood and honor!"));
				final Npc sister1 = addSpawn(KALIS, 160665, 21209, -3710, npc.getHeading(), false, 180000);
				sister1.broadcastPacket(new NpcSay(sister1, ChatType.NPC_GENERAL, "Ambition and power!"));
				final Npc sister2 = addSpawn(ATHREA, 160665, 21291, -3710, npc.getHeading(), false, 180000);
				sister2.broadcastPacket(new NpcSay(sister2, ChatType.NPC_GENERAL, "War and death!"));
				break;
			}
			case "Open":
			{
				if (getQuestItemsCount(player, IMP_KEYS) < 6)
				{
					htmltext = "30765-03a.htm";
				}
				else
				{
					htmltext = "30765-03.htm";
					st.setCond(11);
					takeItems(player, IMP_KEYS, 6);
					giveItems(player, SCEPTER_JUDGEMENT, 1);
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
				if (player.getClan() == null)
				{
					htmltext = "30760-01.htm";
					st.exitQuest(true);
				}
				else if (player.isClanLeader())
				{
					if (hasQuestItems(player, PROOF_ASPIRATION))
					{
						htmltext = "30760-03.htm";
						st.exitQuest(true);
					}
					else if (player.getClan().getLevel() != 4)
					{
						htmltext = "30760-02.htm";
						st.exitQuest(true);
					}
					else
					{
						htmltext = "30760-04.htm";
					}
				}
				else
				{
					htmltext = "30760-04t.htm";
					st.exitQuest(true);
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				int memberCond = 0;
				if (getClanLeaderQuestState(player, npc) != null)
				{
					memberCond = getClanLeaderQuestState(player, npc).getCond();
				}
				
				switch (npc.getId())
				{
					case GUSTAF:
					{
						if (player.isClanLeader())
						{
							if (cond == 1)
							{
								htmltext = "30760-09.htm";
							}
							else if (cond == 2)
							{
								htmltext = "30760-10.htm";
							}
							else if (cond == 3)
							{
								htmltext = "30760-11.htm";
							}
							else if (cond == 4)
							{
								htmltext = "30760-13.htm";
							}
							else if (cond == 5)
							{
								htmltext = "30760-14.htm";
							}
							else if (cond == 6)
							{
								htmltext = "30760-15.htm";
							}
							else if (cond == 7)
							{
								htmltext = "30760-17.htm";
							}
							else if (cond == 12)
							{
								htmltext = "30760-19.htm";
							}
							else if (cond == 13)
							{
								htmltext = "30760-24.htm";
							}
							else if (hasQuestItems(player, SCEPTER_JUDGEMENT))
							{
								htmltext = "30760-19.htm";
							}
							else
							{
								htmltext = "30760-18.htm";
							}
						}
						else
						{
							if (memberCond == 3)
							{
								htmltext = "30760-11t.htm";
							}
							else if (memberCond == 4)
							{
								htmltext = "30760-15t.htm";
							}
							else if (memberCond == 12)
							{
								htmltext = "30760-19t.htm";
							}
							else if (memberCond == 13)
							{
								htmltext = "30766-24t.htm";
							}
						}
						break;
					}
					case MARTIEN:
					{
						if (player.isClanLeader())
						{
							if (cond == 1)
							{
								htmltext = "30645-02.htm";
							}
							else if (cond == 2)
							{
								if ((getQuestItemsCount(player, MI_DRAKE_EGGS) > 9) && (getQuestItemsCount(player, BL_WYRM_EGGS) > 9) && (getQuestItemsCount(player, DRAKE_EGGS) > 9) && (getQuestItemsCount(player, TH_WYRM_EGGS) > 9))
								{
									htmltext = "30645-05.htm";
									st.setCond(3);
									takeItems(player, MI_DRAKE_EGGS, -1);
									takeItems(player, BL_WYRM_EGGS, -1);
									takeItems(player, DRAKE_EGGS, -1);
									takeItems(player, TH_WYRM_EGGS, -1);
								}
								else
								{
									htmltext = "30645-04.htm";
								}
							}
							else if (cond == 3)
							{
								htmltext = "30645-07.htm";
							}
							else
							{
								htmltext = "30645-08.htm";
							}
						}
						else
						{
							if ((memberCond == 1) || (memberCond == 2) || (memberCond == 3))
							{
								htmltext = "30645-01.htm";
							}
						}
						break;
					}
					case LUTZ:
					{
						if (player.isClanLeader() && (cond == 2))
						{
							if (st.getInt("lutz") == 1)
							{
								htmltext = "30762-03.htm";
							}
							else
							{
								htmltext = "30762-01.htm";
							}
						}
						break;
					}
					case KURTZ:
					{
						if (player.isClanLeader() && (cond == 2))
						{
							if (st.getInt("kurt") == 1)
							{
								htmltext = "30763-03.htm";
							}
							else
							{
								htmltext = "30763-01.htm";
							}
						}
						break;
					}
					case FRITZ:
					{
						if (player.isClanLeader() && (cond == 2))
						{
							if (st.getInt("fritz") == 1)
							{
								htmltext = "30761-03.htm";
							}
							else
							{
								htmltext = "30761-01.htm";
							}
						}
						break;
					}
					case KUSTO:
					{
						if (player.isClanLeader())
						{
							if (getQuestItemsCount(player, BROOCH) == 1)
							{
								if (st.getInt("kurt") == 0)
								{
									htmltext = "30512-01.htm";
								}
								else if (st.getInt("kurt") == 1)
								{
									htmltext = "30512-02.htm";
								}
								else
								{
									htmltext = "30512-04.htm";
								}
							}
						}
						else
						{
							if ((memberCond > 2) && (memberCond < 6))
							{
								htmltext = "30512-01a.htm";
							}
						}
						break;
					}
					case BALTHAZAR:
					{
						if (player.isClanLeader())
						{
							if (cond == 4)
							{
								if (st.getInt("kurt") == 2)
								{
									htmltext = "30764-04.htm";
								}
								else
								{
									htmltext = "30764-02.htm";
								}
							}
							else if (cond == 5)
							{
								if ((getQuestItemsCount(player, POWER_STONE) > 9) && (getQuestItemsCount(player, NEBULITE_CRYSTALS) > 9))
								{
									htmltext = "30764-08.htm";
									takeItems(player, POWER_STONE, -1);
									takeItems(player, NEBULITE_CRYSTALS, -1);
									takeItems(player, BROOCH, -1);
									st.setCond(6);
								}
								else
								{
									htmltext = "30764-07.htm";
								}
							}
							else if (cond == 6)
							{
								htmltext = "30764-09.htm";
							}
						}
						else
						{
							if (memberCond == 4)
							{
								htmltext = "30764-01.htm";
							}
						}
						break;
					}
					case RODEMAI:
					{
						if (player.isClanLeader())
						{
							if (cond == 7)
							{
								htmltext = "30868-02.htm";
							}
							else if (cond == 8)
							{
								htmltext = "30868-05.htm";
							}
							else if (cond == 9)
							{
								htmltext = "30868-06.htm";
							}
							else if (cond == 10)
							{
								htmltext = "30868-08.htm";
							}
							else if (cond == 11)
							{
								htmltext = "30868-09.htm";
							}
							else if (cond == 12)
							{
								htmltext = "30868-11.htm";
							}
						}
						else
						{
							if (memberCond == 7)
							{
								htmltext = "30868-01.htm";
							}
							else if ((memberCond == 9) || (memberCond == 10))
							{
								htmltext = "30868-07.htm";
							}
						}
						break;
					}
					case CLEO:
					{
						if (player.isClanLeader())
						{
							if (cond == 8)
							{
								htmltext = "30766-02.htm";
							}
							else if (cond == 9)
							{
								htmltext = "30766-05.htm";
							}
							else if (cond == 10)
							{
								htmltext = "30766-06.htm";
							}
							else if ((cond == 11) || (cond == 12) || (cond == 13))
							{
								htmltext = "30766-07.htm";
							}
						}
						else
						{
							if (memberCond == 8)
							{
								htmltext = "30766-01.htm";
							}
						}
						break;
					}
					case COFFER:
					{
						if (player.isClanLeader())
						{
							if (cond == 10)
							{
								htmltext = "30765-01.htm";
							}
						}
						else
						{
							if (memberCond == 10)
							{
								htmltext = "30765-02.htm";
							}
						}
						break;
					}
					case KALIS:
					{
						if (player.isClanLeader())
						{
							htmltext = "30759-01.htm";
						}
						break;
					}
					case ATHREA:
					{
						if (player.isClanLeader())
						{
							htmltext = "30758-01.htm";
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
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((skill == null) && ((npc.getMaxHp() / 2) > npc.getCurrentHp()))
		{
			if (getRandom(100) < 4)
			{
				addSpawn(IMPERIAL_SLAVE, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 0);
			}
			else
			{
				attacker.teleToLocation(185462, 20342, -3250);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		QuestState st = null;
		st = getClanLeaderQuestState(player, npc);
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		for (int[] element : DROPLIST)
		{
			if (element[0] == npc.getId())
			{
				final int cond = element[1];
				if (st.getCond() == cond)
				{
					final int maxCount = element[2];
					final int chance = element[3];
					final int item1 = element[4];
					final int item2 = element[5];
					final int item3 = element[6];
					if (item1 != 0)
					{
						giveItemRandomly(st.getPlayer(), item1, 1, maxCount, chance / 1000000d, true);
					}
					else
					{
						if (element[0] == IMPERIAL_GRAVEKEEPER)
						{
							final Npc coffer = addSpawn(COFFER, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 180000);
							coffer.broadcastPacket(new NpcSay(coffer, ChatType.NPC_GENERAL, "Curse of the gods on the one that defiles the property of the empire!"));
						}
						else if ((element[0] == GRAVE_GUARD) && (getQuestItemsCount(st.getPlayer(), IMP_KEYS) < 6) && (getRandom(50) < chance))
						{
							addSpawn(GRAVE_KEYMASTER, player.getX(), player.getY(), player.getZ(), player.getHeading(), true, 0);
						}
					}
					
					if ((item2 != 0) && (item3 != 0))
					{
						if (getRandom(4) == 0)
						{
							giveItemRandomly(st.getPlayer(), item2, 1, maxCount, chance / 1000000d, true);
						}
						else
						{
							giveItemRandomly(st.getPlayer(), item3, 1, maxCount, chance / 1000000d, true);
						}
					}
				}
			}
		}
	}
}
