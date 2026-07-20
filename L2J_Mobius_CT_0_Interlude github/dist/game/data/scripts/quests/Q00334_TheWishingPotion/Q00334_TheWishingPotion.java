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
package quests.Q00334_TheWishingPotion;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Adapted from FirstTeam Interlude
 */
public class Q00334_TheWishingPotion extends Quest
{
	private static final int GRIMA = 27135;
	private static final int SUCCUBUS_OF_SEDUCTION = 27136;
	private static final int GREAT_DEMON_KING = 27138;
	private static final int SECRET_KEEPER_TREE = 27139;
	private static final int SANCHES = 27153;
	private static final int BONAPARTERIUS = 27154;
	private static final int RAMSEBALIUS = 27155;
	private static final int TORAI = 30557;
	private static final int ALCHEMIST_MATILD = 30738;
	private static final int RUPINA = 30742;
	private static final int WISDOM_CHEST = 30743;
	private static final int WHISPERING_WIND = 20078;
	private static final int ANT_SOLDIER = 20087;
	private static final int ANT_WARRIOR_CAPTAIN = 20088;
	private static final int SILENOS = 20168;
	private static final int TYRANT = 20192;
	private static final int TYRANT_KINGPIN = 20193;
	private static final int AMBER_BASILISK = 20199;
	private static final int HORROR_MIST_RIPPER = 20227;
	private static final int TURAK_BUGBEAR = 20248;
	private static final int TURAK_BUGBEAR_WARRIOR = 20249;
	private static final int GLASS_JAGUAR = 20250;
	private static final int DEMONS_TUNIC_ID = 441;
	private static final int DEMONS_STOCKINGS_ID = 472;
	private static final int NECKLACE_OF_GRACE_ID = 931;
	private static final int SPELLBOOK_ICEBOLT_ID = 1049;
	private static final int SPELLBOOK_BATTLEHEAL_ID = 1050;
	private static final int DEMONS_TUNIC_FABRIC_ID = 1979;
	private static final int DEMONS_STOCKINGS_PATTERN_ID = 1980;
	private static final int DEMONS_BOOTS_ID = 2435;
	private static final int DEMONS_GLOVES_ID = 2459;
	private static final int DEMONS_BOOTS_FABRIC_ID = 2952;
	private static final int DEMONS_GLOVES_FABRIC_ID = 2953;
	private static final int WISH_POTION_ID = 3467;
	private static final int ANCIENT_CROWN_ID = 3468;
	private static final int CERTIFICATE_OF_ROYALTY_ID = 3469;
	private static final int GOLD_BAR_ID = 3470;
	private static final int ALCHEMY_TEXT_ID = 3678;
	private static final int SECRET_BOOK_ID = 3679;
	private static final int POTION_RECIPE_1_ID = 3680;
	private static final int POTION_RECIPE_2_ID = 3681;
	private static final int MATILDS_ORB_ID = 3682;
	private static final int FORBIDDEN_LOVE_SCROLL_ID = 3683;
	private static final int HEART_OF_PAAGRIO_ID = 3943;
	private static final int AMBER_SCALE_ID = 3684;
	private static final int WIND_SOULSTONE_ID = 3685;
	private static final int GLASS_EYE_ID = 3686;
	private static final int HORROR_ECTOPLASM_ID = 3687;
	private static final int SILENOS_HORN_ID = 3688;
	private static final int ANT_SOLDIER_APHID_ID = 3689;
	private static final int TYRANTS_CHITIN_ID = 3690;
	private static final int BUGBEAR_BLOOD_ID = 3691;
	private static final int DROP_CHANCE_FORBIDDEN_LOVE_SCROLL_ID = 3;
	private static final int DROP_CHANCE_NECKLACE_OF_GRACE_ID = 5;
	private static final int DROP_CHANCE_GOLD_BAR_ID = 10;
	private static final int[][] DROPLIST_COND =
	{// @formatter:off
		{1, 2, SECRET_KEEPER_TREE, 0, SECRET_BOOK_ID, 1, 100, 1},
		{3, 0, AMBER_BASILISK, 0, AMBER_SCALE_ID, 1, 15, 1},
		{3, 0, WHISPERING_WIND, 0, WIND_SOULSTONE_ID, 1, 20, 1},
		{3, 0, GLASS_JAGUAR, 0, GLASS_EYE_ID, 1, 35, 1},
		{3, 0, HORROR_MIST_RIPPER, 0, HORROR_ECTOPLASM_ID, 1, 15, 1},
		{3, 0, SILENOS, 0, SILENOS_HORN_ID, 1, 30, 1},
		{3, 0, ANT_SOLDIER, 0, ANT_SOLDIER_APHID_ID, 1, 40, 1},
		{3, 0, ANT_WARRIOR_CAPTAIN, 0, ANT_SOLDIER_APHID_ID, 1, 40, 1},
		{3, 0, TYRANT, 0, TYRANTS_CHITIN_ID, 1, 50, 1},
		{3, 0, TYRANT_KINGPIN, 0, TYRANTS_CHITIN_ID, 1, 50, 1},
		{3, 0, TURAK_BUGBEAR, 0, BUGBEAR_BLOOD_ID, 1, 15, 1},
		{3, 0, TURAK_BUGBEAR_WARRIOR, 0, BUGBEAR_BLOOD_ID, 1, 25, 1}
		
	}; // @formatter:on
	
	public Q00334_TheWishingPotion()
	{
		super(334, "The Wishing Potion");
		addStartNpc(ALCHEMIST_MATILD);
		addTalkId(ALCHEMIST_MATILD, TORAI, WISDOM_CHEST, RUPINA);
		registerQuestItems(ALCHEMY_TEXT_ID, SECRET_BOOK_ID, AMBER_SCALE_ID, WIND_SOULSTONE_ID, GLASS_EYE_ID, HORROR_ECTOPLASM_ID, SILENOS_HORN_ID, ANT_SOLDIER_APHID_ID, TYRANTS_CHITIN_ID, BUGBEAR_BLOOD_ID);
		for (int[] element : DROPLIST_COND)
		{
			addKillId(element[2]);
		}
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
			case "30738-03.htm":
			{
				st.startQuest();
				giveItems(player, ALCHEMY_TEXT_ID, 1);
				break;
			}
			case "30738-06.htm":
			{
				if (getQuestItemsCount(player, WISH_POTION_ID) == 0)
				{
					takeItems(player, ALCHEMY_TEXT_ID, -1);
					takeItems(player, SECRET_BOOK_ID, -1);
					if (getQuestItemsCount(player, POTION_RECIPE_1_ID) == 0)
					{
						giveItems(player, POTION_RECIPE_1_ID, 1);
					}
					
					if (getQuestItemsCount(player, POTION_RECIPE_2_ID) == 0)
					{
						giveItems(player, POTION_RECIPE_2_ID, 1);
					}
					
					if (getQuestItemsCount(player, MATILDS_ORB_ID) == 0)
					{
						htmltext = "30738-06.htm";
					}
					else
					{
						htmltext = "30738-12.htm";
					}
					
					st.setCond(3);
				}
				else if ((getQuestItemsCount(player, MATILDS_ORB_ID) >= 1) && (getQuestItemsCount(player, WISH_POTION_ID) >= 1))
				{
					htmltext = "30738-13.htm";
				}
				break;
			}
			case "30738-10.htm":
			{
				if (checkIngr(st, player))
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_FINISH);
					takeItems(player, ALCHEMY_TEXT_ID, -1);
					takeItems(player, SECRET_BOOK_ID, -1);
					takeItems(player, POTION_RECIPE_1_ID, -1);
					takeItems(player, POTION_RECIPE_2_ID, -1);
					takeItems(player, AMBER_SCALE_ID, -1);
					takeItems(player, WIND_SOULSTONE_ID, -1);
					takeItems(player, GLASS_EYE_ID, -1);
					takeItems(player, HORROR_ECTOPLASM_ID, -1);
					takeItems(player, SILENOS_HORN_ID, -1);
					takeItems(player, ANT_SOLDIER_APHID_ID, -1);
					takeItems(player, TYRANTS_CHITIN_ID, -1);
					takeItems(player, BUGBEAR_BLOOD_ID, -1);
					if (getQuestItemsCount(player, MATILDS_ORB_ID) == 0)
					{
						giveItems(player, MATILDS_ORB_ID, 1);
					}
					
					giveItems(player, WISH_POTION_ID, 1);
					st.setCond(0);
				}
				else
				{
					htmltext = "<html><head><body>You don't have required items</body></html>";
				}
				break;
			}
			case "30738-14.htm":
			{
				if (getQuestItemsCount(player, WISH_POTION_ID) >= 1)
				{
					htmltext = "30738-15.htm";
				}
				break;
			}
			case "30738-16.htm":
			{
				if (getQuestItemsCount(player, WISH_POTION_ID) >= 1)
				{
					takeItems(player, WISH_POTION_ID, 1);
					if (getRandom(100) < 50)
					{
						addSpawn(SUCCUBUS_OF_SEDUCTION, player);
						addSpawn(SUCCUBUS_OF_SEDUCTION, player);
						addSpawn(SUCCUBUS_OF_SEDUCTION, player);
					}
					else
					{
						addSpawn(RUPINA, player);
					}
				}
				else
				{
					htmltext = "30738-14.htm";
				}
				break;
			}
			case "30738-17.htm":
			{
				if (getQuestItemsCount(player, WISH_POTION_ID) >= 1)
				{
					takeItems(player, WISH_POTION_ID, 1);
					final int WISH_CHANCE = getRandom(100) + 1;
					if (WISH_CHANCE <= 33)
					{
						addSpawn(GRIMA, player);
						addSpawn(GRIMA, player);
						addSpawn(GRIMA, player);
					}
					else if (WISH_CHANCE >= 66)
					{
						giveAdena(player, 10000, true);
					}
					else if (getRandom(100) < 2)
					{
						giveAdena(player, (getRandom(10) + 1) * 1000000, true);
					}
					else
					{
						addSpawn(GRIMA, player);
						addSpawn(GRIMA, player);
						addSpawn(GRIMA, player);
					}
				}
				else
				{
					htmltext = "30738-14.htm";
				}
				break;
			}
			case "30738-18.htm":
			{
				if (getQuestItemsCount(player, WISH_POTION_ID) >= 1)
				{
					takeItems(player, WISH_POTION_ID, 1);
					final int WISH_CHANCE = getRandom(100) + 1;
					if (WISH_CHANCE <= 33)
					{
						giveItems(player, CERTIFICATE_OF_ROYALTY_ID, 1);
					}
					else if (WISH_CHANCE >= 66)
					{
						giveItems(player, ANCIENT_CROWN_ID, 1);
					}
					else
					{
						addSpawn(SANCHES, player);
					}
				}
				else
				{
					htmltext = "30738-14.htm";
				}
				break;
			}
			case "30738-19.htm":
			{
				if (getQuestItemsCount(player, WISH_POTION_ID) >= 1)
				{
					takeItems(player, 3467, 1);
					final int WISH_CHANCE = getRandom(100) + 1;
					if (WISH_CHANCE <= 33)
					{
						giveItems(player, SPELLBOOK_ICEBOLT_ID, 1);
					}
					else if (WISH_CHANCE <= 66)
					{
						giveItems(player, SPELLBOOK_BATTLEHEAL_ID, 1);
					}
					else
					{
						addSpawn(WISDOM_CHEST, player);
					}
				}
				else
				{
					htmltext = "30738-14.htm";
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
		
		final int npcId = npc.getId();
		final int id = st.getState();
		int cond = 0;
		if (id != 0)
		{
			cond = st.getCond();
		}
		
		switch (npcId)
		{
			case ALCHEMIST_MATILD:
			{
				if (cond == 0)
				{
					if (st.getPlayer().getLevel() <= 29)
					{
						htmltext = "30738-21.htm";
						st.exitQuest(true);
					}
					else if (getQuestItemsCount(player, MATILDS_ORB_ID) == 0)
					{
						htmltext = "30738-01.htm";
					}
					else if (getQuestItemsCount(player, 3467) == 0)
					{
						st.setCond(3);
						if (getQuestItemsCount(player, POTION_RECIPE_1_ID) == 0)
						{
							giveItems(player, POTION_RECIPE_1_ID, 1);
						}
						
						if (getQuestItemsCount(player, POTION_RECIPE_2_ID) == 0)
						{
							giveItems(player, POTION_RECIPE_2_ID, 1);
						}
						
						htmltext = "30738-12.htm";
					}
					else
					{
						htmltext = "30738-11.htm";
					}
				}
				else if ((cond == 1) && (getQuestItemsCount(player, ALCHEMY_TEXT_ID) == 1))
				{
					htmltext = "30738-04.htm";
				}
				else if (cond == 2)
				{
					if ((getQuestItemsCount(player, SECRET_BOOK_ID) == 1) && (getQuestItemsCount(player, ALCHEMY_TEXT_ID) == 1))
					{
						htmltext = "30738-05.htm";
					}
				}
				else if (cond == 4)
				{
					if (checkIngr(st, player))
					{
						htmltext = "30738-08.htm";
					}
					else
					{
						htmltext = "30738-07.htm";
					}
				}
				break;
			}
			case TORAI:
			{
				if (getQuestItemsCount(player, FORBIDDEN_LOVE_SCROLL_ID) >= 1)
				{
					takeItems(player, FORBIDDEN_LOVE_SCROLL_ID, 1);
					giveAdena(player, 500000, true);
					htmltext = "30557-01.htm";
				}
				else
				{
					htmltext = getNoQuestMsg(player);
				}
				break;
			}
			case WISDOM_CHEST:
			{
				final int dropChance = getRandom(100);
				if (dropChance < 20)
				{
					giveItems(player, SPELLBOOK_ICEBOLT_ID, 1);
					giveItems(player, SPELLBOOK_BATTLEHEAL_ID, 1);
					st.getPlayer().getTarget().decayMe();
					htmltext = "30743-06.htm";
				}
				else if (dropChance < 30)
				{
					giveItems(player, HEART_OF_PAAGRIO_ID, 1);
					st.getPlayer().getTarget().decayMe();
					htmltext = "30743-06.htm";
				}
				else
				{
					st.getPlayer().getTarget().decayMe();
					htmltext = "30743-0" + (getRandom(5) + 1) + ".htm";
				}
				break;
			}
			case RUPINA:
			{
				if (getRandom(100) < DROP_CHANCE_NECKLACE_OF_GRACE_ID)
				{
					giveItems(player, NECKLACE_OF_GRACE_ID, 1);
				}
				else
				{
					final int dropChance = getRandom(100) + 1;
					if (dropChance <= 25)
					{
						giveItems(player, DEMONS_TUNIC_FABRIC_ID, 1);
					}
					else if (dropChance <= 50)
					{
						giveItems(player, DEMONS_STOCKINGS_PATTERN_ID, 1);
					}
					else if (dropChance <= 75)
					{
						giveItems(player, DEMONS_BOOTS_FABRIC_ID, 1);
					}
					else
					{
						giveItems(player, DEMONS_GLOVES_FABRIC_ID, 1);
					}
				}
				
				st.getPlayer().getTarget().decayMe();
				htmltext = "30742-01.htm";
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return;
		}
		
		final int npcId = npc.getId();
		final int cond = st.getCond();
		for (int[] element : DROPLIST_COND)
		{
			if ((cond == element[0]) && (npcId == element[2]) && ((element[3] == 0) || (getQuestItemsCount(player, element[3]) > 0)))
			{
				if (element[5] == 0)
				{
					if (getRandom(100) < element[6])
					{
						giveItems(player, element[4], element[7]);
					}
				}
				else if ((getRandom(100) < element[6]) && (getQuestItemsCount(player, element[4]) < element[5]))
				{
					giveItems(player, element[4], element[7]);
					if (cond == 3)
					{
						checkIngr(st, player);
					}
					
					if ((element[1] != cond) && (element[1] != 0))
					{
						st.setCond(element[1]);
						st.setState(State.STARTED);
					}
				}
			}
		}
		
		final int dropChance = getRandom(100) + 1;
		if ((npcId == SUCCUBUS_OF_SEDUCTION) && (dropChance <= DROP_CHANCE_FORBIDDEN_LOVE_SCROLL_ID))
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			giveItems(player, FORBIDDEN_LOVE_SCROLL_ID, 1);
		}
		else if ((npcId == GRIMA) && (dropChance <= DROP_CHANCE_GOLD_BAR_ID))
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			giveItems(player, GOLD_BAR_ID, getRandom(5) + 1);
		}
		else if ((npcId == SANCHES) && (getRandom(100) < 50))
		{
			addSpawn(BONAPARTERIUS, player);
		}
		else if ((npcId == BONAPARTERIUS) && (getRandom(100) < 50))
		{
			addSpawn(RAMSEBALIUS, player);
		}
		else if ((npcId == RAMSEBALIUS) && (getRandom(100) < 50))
		{
			addSpawn(GREAT_DEMON_KING, player);
		}
		else if ((npcId == GREAT_DEMON_KING) && (getRandom(100) < 50))
		{
			if (dropChance <= 25)
			{
				giveItems(player, DEMONS_BOOTS_ID, 1);
			}
			else if (dropChance <= 50)
			{
				giveItems(player, DEMONS_GLOVES_ID, 1);
			}
			else if (dropChance <= 75)
			{
				giveItems(player, DEMONS_STOCKINGS_ID, 1);
			}
			else
			{
				giveItems(player, DEMONS_TUNIC_ID, 1);
			}
			
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
	
	public boolean checkIngr(QuestState st, Player player)
	{
		if ((getQuestItemsCount(player, AMBER_SCALE_ID) == 1) && (getQuestItemsCount(player, WIND_SOULSTONE_ID) == 1) && (getQuestItemsCount(player, GLASS_EYE_ID) == 1) && (getQuestItemsCount(player, HORROR_ECTOPLASM_ID) == 1) && (getQuestItemsCount(player, SILENOS_HORN_ID) == 1) && (getQuestItemsCount(player, ANT_SOLDIER_APHID_ID) == 1) && (getQuestItemsCount(player, TYRANTS_CHITIN_ID) == 1) && (getQuestItemsCount(player, BUGBEAR_BLOOD_ID) == 1))
		{
			st.setCond(4, true);
			return true;
		}
		
		st.setCond(3, true);
		return false;
	}
}
