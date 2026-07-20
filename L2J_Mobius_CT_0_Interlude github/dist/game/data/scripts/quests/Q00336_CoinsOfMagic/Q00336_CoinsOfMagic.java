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
package quests.Q00336_CoinsOfMagic;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

/**
 * Adapted from FirstTeam Interlude
 */
public class Q00336_CoinsOfMagic extends Quest
{
	private static final int COIN_DIAGRAM = 3811;
	private static final int KALDIS_COIN = 3812;
	private static final int MEMBERSHIP_1 = 3813;
	private static final int MEMBERSHIP_2 = 3814;
	private static final int MEMBERSHIP_3 = 3815;
	private static final int BLOOD_MEDUSA = 3472;
	private static final int BLOOD_WEREWOLF = 3473;
	private static final int BLOOD_BASILISK = 3474;
	private static final int BLOOD_DREVANUL = 3475;
	private static final int BLOOD_SUCCUBUS = 3476;
	private static final int GOLD_WYVERN = 3482;
	private static final int GOLD_KNIGHT = 3483;
	private static final int GOLD_GIANT = 3484;
	private static final int GOLD_DRAKE = 3485;
	private static final int GOLD_WYRM = 3486;
	private static final int SILVER_UNICORN = 3490;
	private static final int SILVER_FAIRY = 3491;
	private static final int SILVER_DRYAD = 3492;
	private static final int SILVER_GOLEM = 3494;
	private static final int SILVER_UNDINE = 3495;
	// private static final int[] BASIC_COINS =
	// {
	// BLOOD_MEDUSA,
	// GOLD_WYVERN,
	// SILVER_UNICORN
	// };
	private static final int SORINT = 30232;
	private static final int BERNARD = 30702;
	private static final int PAGE = 30696;
	private static final int HAGGER = 30183;
	private static final int STAN = 30200;
	private static final int RALFORD = 30165;
	private static final int FERRIS = 30847;
	private static final int COLLOB = 30092;
	private static final int PANO = 30078;
	private static final int DUNING = 30688;
	private static final int LORAIN = 30673;
	private static final int TIMAK_ORC_ARCHER = 20584;
	private static final int TIMAK_ORC_SOLDIER = 20585;
	private static final int TIMAK_ORC_SHAMAN = 20587;
	private static final int LAKIN = 20604;
	private static final int TORTURED_UNDEAD = 20678;
	private static final int HATAR_HANISHEE = 20663;
	private static final int SHACKLE = 20235;
	private static final int TIMAK_ORC = 20583;
	private static final int HEADLESS_KNIGHT = 20146;
	private static final int ROYAL_CAVE_SERVANT = 20240;
	private static final int MALRUK_SUCCUBUS_TUREN = 20245;
	private static final int FORMOR = 20568;
	private static final int FORMOR_ELDER = 20569;
	private static final int VANOR_SILENOS_SHAMAN = 20685;
	private static final int TARLK_BUGBEAR_HIGH_WARRIOR = 20572;
	private static final int OEL_MAHUM = 20161;
	private static final int OEL_MAHUM_WARRIOR = 20575;
	private static final int HARIT_LIZARDMAN_MATRIARCH = 20645;
	private static final int HARIT_LIZARDMAN_SHAMAN = 20644;
	private static final int GRAVE_LICH = 21003;
	private static final int DOOM_SERVANT = 21006;
	private static final int DOOM_ARCHER = 21008;
	private static final int DOOM_KNIGHT = 20674;
	private static final int KOOKABURRA2 = 21276;
	private static final int KOOKABURRA3 = 21275;
	private static final int KOOKABURRA4 = 21274;
	private static final int ANTELOPE2 = 21278;
	private static final int ANTELOPE3 = 21279;
	private static final int ANTELOPE4 = 21280;
	private static final int BANDERSNATCH2 = 21282;
	private static final int BANDERSNATCH3 = 21284;
	private static final int BANDERSNATCH4 = 21283;
	private static final int BUFFALO2 = 21287;
	private static final int BUFFALO3 = 21288;
	private static final int BUFFALO4 = 21286;
	private static final int CLAWS_OF_SPLENDOR = 21521;
	private static final int WISDOM_OF_SPLENDOR = 21526;
	private static final int PUNISHMENT_OF_SPLENDOR = 21531;
	private static final int WAILING_OF_SPLENDOR = 21539;
	private static final int HUNGERED_CORPSE = 20954;
	private static final int BLOODY_GHOST = 20960;
	private static final int NIHIL_INVADER = 20957;
	private static final int DARK_GUARD = 20959;
	// @formatter:off
	private static final int[][] PROMOTE =
	{
		new int[0],
		new int[0], {SILVER_DRYAD, BLOOD_BASILISK, BLOOD_SUCCUBUS, SILVER_UNDINE, GOLD_GIANT, GOLD_WYRM}, {BLOOD_WEREWOLF, GOLD_DRAKE, SILVER_FAIRY, BLOOD_DREVANUL, GOLD_KNIGHT, SILVER_GOLEM}
	};
	private static final int[][] EXCHANGE_LEVEL =
	{
		{PAGE, 3}, {LORAIN, 3}, {HAGGER, 3}, {RALFORD, 2}, {STAN, 2}, {DUNING, 2}, {FERRIS, 1}, {COLLOB, 1}, {PANO, 1}
	};
	private static final int[][] DROPLIST =
	{
		{TIMAK_ORC_ARCHER, BLOOD_MEDUSA}, {TIMAK_ORC_SOLDIER, BLOOD_MEDUSA}, {TIMAK_ORC_SHAMAN, 3472}, {LAKIN, 3472}, {TORTURED_UNDEAD, 3472}, {HATAR_HANISHEE, 3472}, {TIMAK_ORC, GOLD_WYVERN}, {SHACKLE, GOLD_WYVERN}, {HEADLESS_KNIGHT, GOLD_WYVERN},
		{ROYAL_CAVE_SERVANT, GOLD_WYVERN}, {MALRUK_SUCCUBUS_TUREN, GOLD_WYVERN}, {FORMOR, SILVER_UNICORN}, {FORMOR_ELDER, SILVER_UNICORN}, {VANOR_SILENOS_SHAMAN, SILVER_UNICORN}, {TARLK_BUGBEAR_HIGH_WARRIOR, SILVER_UNICORN}, {OEL_MAHUM, SILVER_UNICORN}, {OEL_MAHUM_WARRIOR, SILVER_UNICORN}
	};
	private static final int[] MONSTERS =
	{
		GRAVE_LICH, DOOM_SERVANT, DOOM_ARCHER, DOOM_KNIGHT, KOOKABURRA2, KOOKABURRA3, KOOKABURRA4, ANTELOPE2, ANTELOPE3, ANTELOPE4, BANDERSNATCH2, BANDERSNATCH3, BANDERSNATCH4, BUFFALO2, BUFFALO3, BUFFALO4, CLAWS_OF_SPLENDOR, WISDOM_OF_SPLENDOR, PUNISHMENT_OF_SPLENDOR, WAILING_OF_SPLENDOR, HUNGERED_CORPSE, BLOODY_GHOST, NIHIL_INVADER, DARK_GUARD
	};
	// @formatter:on
	
	public Q00336_CoinsOfMagic()
	{
		super(336, "Coins of Magic");
		addStartNpc(SORINT);
		addTalkId(SORINT, BERNARD, PAGE, HAGGER, STAN, RALFORD, FERRIS, COLLOB, PANO, DUNING, LORAIN);
		for (int[] mob : DROPLIST)
		{
			addKillId(mob[0]);
		}
		
		addKillId(MONSTERS);
		addKillId(HARIT_LIZARDMAN_MATRIARCH);
		addKillId(HARIT_LIZARDMAN_SHAMAN);
		registerQuestItems(COIN_DIAGRAM, KALDIS_COIN, MEMBERSHIP_1, MEMBERSHIP_2, MEMBERSHIP_3);
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
		
		final int cond = st.getCond();
		if ("30702-06.htm".equals(event))
		{
			if (cond < 7)
			{
				st.setCond(7);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
			}
		}
		else if ("30232-22.htm".equals(event))
		{
			if (cond < 6)
			{
				st.setCond(6);
			}
		}
		else if ("30232-23.htm".equals(event))
		{
			if (cond < 5)
			{
				st.setCond(5);
			}
		}
		else if ("30702-02.htm".equals(event))
		{
			st.setCond(2);
		}
		else if ("30232-05.htm".equals(event))
		{
			st.startQuest();
			giveItems(player, COIN_DIAGRAM, 1);
		}
		else if ("30232-04.htm".equals(event) || "30232-18a.htm".equals(event))
		{
			st.exitQuest(true);
			playSound(player, QuestSound.ITEMSOUND_QUEST_GIVEUP);
		}
		else if ("raise".equals(event))
		{
			htmltext = promote(st);
		}
		
		return htmltext;
	}
	
	private String promote(QuestState st)
	{
		final int grade = st.getInt("grade");
		String html;
		if (grade == 1)
		{
			html = "30232-15.htm";
		}
		else
		{
			int h = 0;
			for (int i : PROMOTE[grade])
			{
				if (getQuestItemsCount(st.getPlayer(), i) > 0)
				{
					++h;
				}
			}
			
			if (h == 6)
			{
				final Player player = st.getPlayer();
				for (int i : PROMOTE[grade])
				{
					takeItems(player, i, 1);
				}
				
				html = "30232-" + (19 - grade) + ".htm";
				takeItems(player, KALDIS_COIN + grade, -1);
				giveItems(player, COIN_DIAGRAM + grade, 1);
				st.set("grade", "" + (grade - 1));
				if (grade == 3)
				{
					st.setCond(9);
				}
				else if (grade == 2)
				{
					st.setCond(11);
				}
				
				playSound(player, QuestSound.ITEMSOUND_QUEST_FANFARE_MIDDLE);
			}
			else
			{
				html = "30232-" + (16 - grade) + ".htm";
				if (grade == 3)
				{
					st.setCond(8);
				}
				else if (grade == 2)
				{
					st.setCond(9);
				}
			}
		}
		
		return html;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		final int npcId = npc.getId();
		final int id = st.getState();
		final int grade = st.getInt("grade");
		switch (npcId)
		{
			case SORINT:
			{
				if (id == State.CREATED)
				{
					if (st.getPlayer().getLevel() < 40)
					{
						htmltext = "30232-01.htm";
						st.exitQuest(true);
					}
					else
					{
						htmltext = "30232-02.htm";
					}
				}
				else if (getQuestItemsCount(player, COIN_DIAGRAM) > 0)
				{
					if (getQuestItemsCount(player, KALDIS_COIN) > 0)
					{
						takeItems(player, KALDIS_COIN, -1);
						takeItems(player, COIN_DIAGRAM, -1);
						giveItems(player, MEMBERSHIP_3, 1);
						st.set("grade", "3");
						st.setCond(4);
						playSound(player, QuestSound.ITEMSOUND_QUEST_FANFARE_MIDDLE);
						htmltext = "30232-07.htm";
					}
					else
					{
						htmltext = "30232-06.htm";
					}
				}
				else if (grade == 3)
				{
					htmltext = "30232-12.htm";
				}
				else if (grade == 2)
				{
					htmltext = "30232-11.htm";
				}
				else if (grade == 1)
				{
					htmltext = "30232-10.htm";
				}
				break;
			}
			case BERNARD:
			{
				if ((getQuestItemsCount(player, COIN_DIAGRAM) > 0) && (grade == 0))
				{
					htmltext = "30702-01.htm";
				}
				else if (grade == 3)
				{
					htmltext = "30702-05.htm";
				}
				break;
			}
			default:
			{
				for (int[] e : EXCHANGE_LEVEL)
				{
					if ((npcId == e[0]) && (grade <= e[1]))
					{
						htmltext = npcId + "-01.htm";
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
		
		final int cond = st.getCond();
		final int npcId = npc.getId();
		if ((npcId == HARIT_LIZARDMAN_MATRIARCH) || (npcId == HARIT_LIZARDMAN_SHAMAN))
		{
			if ((cond == 2) && (getRandom(1000) < 63))
			{
				giveItems(player, KALDIS_COIN, 1);
				st.setCond(3);
			}
			return;
		}
		
		final int grade = st.getInt("grade");
		final int chance = (npc.getLevel() + (grade * 3)) - 20;
		for (int[] e : DROPLIST)
		{
			if (e[0] == npcId)
			{
				if (getRandom(100) < chance)
				{
					giveItems(player, e[1], 1);
				}
				return;
			}
		}
		
		// TODO: getBaseHpConsumeRate was 0.
		// for (int u : MONSTERS)
		// {
		// if (u == npcId)
		// {
		// if (getRandom(100) < (chance * npc.getTemplate().getBaseHpConsumeRate()))
		// {
		// giveItems(player, BASIC_COINS[getRandom(BASIC_COINS.length)], 1);
		// }
		// return null;
		// }
		// }
	}
}
