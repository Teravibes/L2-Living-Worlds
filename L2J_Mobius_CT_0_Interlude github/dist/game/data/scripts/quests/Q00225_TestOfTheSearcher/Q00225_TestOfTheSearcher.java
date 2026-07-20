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
package quests.Q00225_TestOfTheSearcher;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00225_TestOfTheSearcher extends Quest
{
	// NPCs
	private static final int ALEX = 30291;
	private static final int TYRA = 30420;
	private static final int TREE = 30627;
	private static final int STRONG_WOODEN_CHEST = 30628;
	private static final int LUTHER = 30690;
	private static final int LEIRYNN = 30728;
	private static final int BORYS = 30729;
	private static final int JAX = 30730;
	
	// Monsters
	private static final int HANGMAN_TREE = 20144;
	private static final int ROAD_SCAVENGER = 20551;
	private static final int GIANT_FUNGUS = 20555;
	private static final int DELU_LIZARDMAN_SHAMAN = 20781;
	private static final int DELU_CHIEF_KALKIS = 27093;
	private static final int NEER_BODYGUARD = 27092;
	
	// Items
	private static final int LUTHER_LETTER = 2784;
	private static final int ALEX_WARRANT = 2785;
	private static final int LEIRYNN_ORDER_1 = 2786;
	private static final int DELU_TOTEM = 2787;
	private static final int LEIRYNN_ORDER_2 = 2788;
	private static final int CHIEF_KALKI_FANG = 2789;
	private static final int LEIRYNN_REPORT = 2790;
	private static final int STRANGE_MAP = 2791;
	private static final int LAMBERT_MAP = 2792;
	private static final int ALEX_LETTER = 2793;
	private static final int ALEX_ORDER = 2794;
	private static final int WINE_CATALOG = 2795;
	private static final int TYRA_CONTRACT = 2796;
	private static final int RED_SPORE_DUST = 2797;
	private static final int MALRUKIAN_WINE = 2798;
	private static final int OLD_ORDER = 2799;
	private static final int JAX_DIARY = 2800;
	private static final int TORN_MAP_PIECE_1 = 2801;
	private static final int TORN_MAP_PIECE_2 = 2802;
	private static final int SOLT_MAP = 2803;
	private static final int MAKEL_MAP = 2804;
	private static final int COMBINED_MAP = 2805;
	private static final int RUSTED_KEY = 2806;
	private static final int GOLD_BAR = 2807;
	private static final int ALEX_RECOMMEND = 2808;
	
	// Rewards
	private static final int MARK_OF_SEARCHER = 2809;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	// Misc
	private static Npc _strongWoodenChest; // Used to avoid to spawn multiple instances.
	
	public Q00225_TestOfTheSearcher()
	{
		super(225, "Test of the Searcher");
		registerQuestItems(LUTHER_LETTER, ALEX_WARRANT, LEIRYNN_ORDER_1, DELU_TOTEM, LEIRYNN_ORDER_2, CHIEF_KALKI_FANG, LEIRYNN_REPORT, STRANGE_MAP, LAMBERT_MAP, ALEX_LETTER, ALEX_ORDER, WINE_CATALOG, TYRA_CONTRACT, RED_SPORE_DUST, MALRUKIAN_WINE, OLD_ORDER, JAX_DIARY, TORN_MAP_PIECE_1, TORN_MAP_PIECE_2, SOLT_MAP, MAKEL_MAP, COMBINED_MAP, RUSTED_KEY, GOLD_BAR, ALEX_RECOMMEND);
		addStartNpc(LUTHER);
		addTalkId(ALEX, TYRA, TREE, STRONG_WOODEN_CHEST, LUTHER, LEIRYNN, BORYS, JAX);
		addAttackId(DELU_LIZARDMAN_SHAMAN);
		addKillId(HANGMAN_TREE, ROAD_SCAVENGER, GIANT_FUNGUS, DELU_LIZARDMAN_SHAMAN, DELU_CHIEF_KALKIS, NEER_BODYGUARD);
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
			case "30690-05.htm":
			{
				st.startQuest();
				giveItems(player, LUTHER_LETTER, 1);
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30690-05a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30291-07.htm":
			{
				st.setCond(8, true);
				takeItems(player, LEIRYNN_REPORT, 1);
				takeItems(player, STRANGE_MAP, 1);
				giveItems(player, ALEX_LETTER, 1);
				giveItems(player, ALEX_ORDER, 1);
				giveItems(player, LAMBERT_MAP, 1);
				break;
			}
			case "30420-01a.htm":
			{
				st.setCond(10, true);
				takeItems(player, WINE_CATALOG, 1);
				giveItems(player, TYRA_CONTRACT, 1);
				break;
			}
			case "30730-01d.htm":
			{
				st.setCond(14, true);
				takeItems(player, OLD_ORDER, 1);
				giveItems(player, JAX_DIARY, 1);
				break;
			}
			case "30627-01a.htm":
			{
				if (_strongWoodenChest == null)
				{
					if (st.isCond(16))
					{
						st.setCond(17, true);
						giveItems(player, RUSTED_KEY, 1);
					}
					
					_strongWoodenChest = addSpawn(STRONG_WOODEN_CHEST, 10098, 157287, -2406, 0, false, 0);
					startQuestTimer("chest_despawn", 300000, null, player, false);
				}
				break;
			}
			case "30628-01a.htm":
			{
				if (!hasQuestItems(player, RUSTED_KEY))
				{
					htmltext = "30628-02.htm";
				}
				else
				{
					st.setCond(18, true);
					takeItems(player, RUSTED_KEY, -1);
					giveItems(player, GOLD_BAR, 20);
					
					_strongWoodenChest.deleteMe();
					_strongWoodenChest = null;
					cancelQuestTimer("chest_despawn", null, player);
				}
				break;
			}
			case "chest_despawn":
			{
				_strongWoodenChest.deleteMe();
				_strongWoodenChest = null;
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
				if ((player.getPlayerClass() != PlayerClass.ROGUE) && (player.getPlayerClass() != PlayerClass.ELVEN_SCOUT) && (player.getPlayerClass() != PlayerClass.ASSASSIN) && (player.getPlayerClass() != PlayerClass.SCAVENGER))
				{
					htmltext = "30690-01.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30690-02.htm";
				}
				else
				{
					htmltext = (player.getPlayerClass() == PlayerClass.SCAVENGER) ? "30690-04.htm" : "30690-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case LUTHER:
					{
						if (cond == 1)
						{
							htmltext = "30690-06.htm";
						}
						else if ((cond > 1) && (cond < 19))
						{
							htmltext = "30690-07.htm";
						}
						else if (cond == 19)
						{
							htmltext = "30690-08.htm";
							takeItems(player, ALEX_RECOMMEND, 1);
							giveItems(player, MARK_OF_SEARCHER, 1);
							addExpAndSp(player, 37831, 18750);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case ALEX:
					{
						if (cond == 1)
						{
							htmltext = "30291-01.htm";
							st.setCond(2, true);
							takeItems(player, LUTHER_LETTER, 1);
							giveItems(player, ALEX_WARRANT, 1);
						}
						else if (cond == 2)
						{
							htmltext = "30291-02.htm";
						}
						else if ((cond > 2) && (cond < 7))
						{
							htmltext = "30291-03.htm";
						}
						else if (cond == 7)
						{
							htmltext = "30291-04.htm";
						}
						else if ((cond > 7) && (cond < 13))
						{
							htmltext = "30291-08.htm";
						}
						else if ((cond > 12) && (cond < 16))
						{
							htmltext = "30291-09.htm";
						}
						else if ((cond > 15) && (cond < 18))
						{
							htmltext = "30291-10.htm";
						}
						else if (cond == 18)
						{
							htmltext = "30291-11.htm";
							st.setCond(19, true);
							takeItems(player, ALEX_ORDER, 1);
							takeItems(player, COMBINED_MAP, 1);
							takeItems(player, GOLD_BAR, -1);
							giveItems(player, ALEX_RECOMMEND, 1);
						}
						else if (cond == 19)
						{
							htmltext = "30291-12.htm";
						}
						break;
					}
					case LEIRYNN:
					{
						if (cond == 2)
						{
							htmltext = "30728-01.htm";
							st.setCond(3, true);
							takeItems(player, ALEX_WARRANT, 1);
							giveItems(player, LEIRYNN_ORDER_1, 1);
						}
						else if (cond == 3)
						{
							htmltext = "30728-02.htm";
						}
						else if (cond == 4)
						{
							htmltext = "30728-03.htm";
							st.setCond(5, true);
							takeItems(player, DELU_TOTEM, -1);
							takeItems(player, LEIRYNN_ORDER_1, 1);
							giveItems(player, LEIRYNN_ORDER_2, 1);
						}
						else if (cond == 5)
						{
							htmltext = "30728-04.htm";
						}
						else if (cond == 6)
						{
							htmltext = "30728-05.htm";
							st.setCond(7, true);
							takeItems(player, CHIEF_KALKI_FANG, 1);
							takeItems(player, LEIRYNN_ORDER_2, 1);
							giveItems(player, LEIRYNN_REPORT, 1);
						}
						else if (cond == 7)
						{
							htmltext = "30728-06.htm";
						}
						else if (cond > 7)
						{
							htmltext = "30728-07.htm";
						}
						break;
					}
					case BORYS:
					{
						if (cond == 8)
						{
							htmltext = "30729-01.htm";
							st.setCond(9, true);
							takeItems(player, ALEX_LETTER, 1);
							giveItems(player, WINE_CATALOG, 1);
						}
						else if ((cond > 8) && (cond < 12))
						{
							htmltext = "30729-02.htm";
						}
						else if (cond == 12)
						{
							htmltext = "30729-03.htm";
							st.setCond(13, true);
							takeItems(player, MALRUKIAN_WINE, 1);
							takeItems(player, WINE_CATALOG, 1);
							giveItems(player, OLD_ORDER, 1);
						}
						else if (cond == 13)
						{
							htmltext = "30729-04.htm";
						}
						else if (cond > 13)
						{
							htmltext = "30729-05.htm";
						}
						break;
					}
					case TYRA:
					{
						if (cond == 9)
						{
							htmltext = "30420-01.htm";
						}
						else if (cond == 10)
						{
							htmltext = "30420-02.htm";
						}
						else if (cond == 11)
						{
							htmltext = "30420-03.htm";
							st.setCond(12, true);
							takeItems(player, RED_SPORE_DUST, -1);
							takeItems(player, TYRA_CONTRACT, 1);
							giveItems(player, MALRUKIAN_WINE, 1);
						}
						else if (cond > 11)
						{
							htmltext = "30420-04.htm";
						}
						break;
					}
					case JAX:
					{
						if (cond == 13)
						{
							htmltext = "30730-01.htm";
						}
						else if (cond == 14)
						{
							htmltext = "30730-02.htm";
						}
						else if (cond == 15)
						{
							htmltext = "30730-03.htm";
							st.setCond(16, true);
							takeItems(player, LAMBERT_MAP, 1);
							takeItems(player, MAKEL_MAP, 1);
							takeItems(player, JAX_DIARY, 1);
							takeItems(player, SOLT_MAP, 1);
							giveItems(player, COMBINED_MAP, 1);
						}
						else if (cond > 15)
						{
							htmltext = "30730-04.htm";
						}
						break;
					}
					case TREE:
					{
						if ((cond == 16) || (cond == 17))
						{
							htmltext = "30627-01.htm";
						}
						break;
					}
					case STRONG_WOODEN_CHEST:
					{
						if (cond == 17)
						{
							htmltext = "30628-01.htm";
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
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		final QuestState st = getQuestState(attacker, false);
		if (st == null)
		{
			return;
		}
		
		if (hasQuestItems(attacker, LEIRYNN_ORDER_1) && !npc.isScriptValue(1))
		{
			npc.setScriptValue(1);
			addSpawn(NEER_BODYGUARD, npc, false, 200000);
		}
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
			case DELU_LIZARDMAN_SHAMAN:
			{
				if (st.isCond(3))
				{
					giveItems(player, DELU_TOTEM, 1);
					if (getQuestItemsCount(player, DELU_TOTEM) >= 10)
					{
						st.setCond(4, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case DELU_CHIEF_KALKIS:
			{
				if (st.isCond(5))
				{
					st.setCond(6, true);
					giveItems(player, CHIEF_KALKI_FANG, 1);
					giveItems(player, STRANGE_MAP, 1);
				}
				break;
			}
			case GIANT_FUNGUS:
			{
				if (st.isCond(10))
				{
					giveItems(player, RED_SPORE_DUST, 1);
					if (getQuestItemsCount(player, RED_SPORE_DUST) >= 10)
					{
						st.setCond(11, true);
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case ROAD_SCAVENGER:
			{
				if (st.isCond(14) && !hasQuestItems(player, SOLT_MAP) && getRandomBoolean())
				{
					giveItems(player, TORN_MAP_PIECE_1, 1);
					if (getQuestItemsCount(player, TORN_MAP_PIECE_1) >= 4)
					{
						takeItems(player, TORN_MAP_PIECE_1, -1);
						giveItems(player, SOLT_MAP, 1);
						if (hasQuestItems(player, MAKEL_MAP))
						{
							st.setCond(15, true);
						}
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
			case HANGMAN_TREE:
			{
				if (st.isCond(14) && !hasQuestItems(player, MAKEL_MAP) && getRandomBoolean())
				{
					giveItems(player, TORN_MAP_PIECE_2, 1);
					if (getQuestItemsCount(player, TORN_MAP_PIECE_2) >= 4)
					{
						takeItems(player, TORN_MAP_PIECE_2, -1);
						giveItems(player, MAKEL_MAP, 1);
						if (hasQuestItems(player, SOLT_MAP))
						{
							st.setCond(15, true);
						}
					}
					else
					{
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				break;
			}
		}
	}
}
