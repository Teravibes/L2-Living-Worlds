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
package quests.Q00335_TheSongOfTheHunter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Adapted from FirstTeam Interlude
 */
public class Q00335_TheSongOfTheHunter extends Quest
{
	private static final int GREY = 30744;
	private static final int TOR = 30745;
	private static final int CYBELLIN = 30746;
	private static final int BREKA_ORC_WARRIOR = 20271;
	private static final int WINDSUS = 20553;
	private static final int TARLK_BUGBEAR_WARRIOR = 20571;
	private static final int BREKA_OVERLORD_HAKA = 27140;
	private static final int BREKA_OVERLORD_JAKA = 27141;
	private static final int BREKA_OVERLORD_MARKA = 27142;
	private static final int WINDSUS_ALEPH = 27143;
	private static final int TARLK_RAIDER_ATHU = 27144;
	private static final int TARLK_RAIDER_LANKA = 27145;
	private static final int TARLK_RAIDER_TRISKA = 27146;
	private static final int TARLK_RAIDER_MOTURA = 27147;
	private static final int TARLK_RAIDER_KALATH = 27148;
	private static final int CYBELLINS_DAGGER = 3471;
	private static final int CIRCLE_HUNTER_LICENSE1 = 3692;
	private static final int CIRCLE_HUNTER_LICENSE2 = 3693;
	private static final int LAUREL_LEAF_PIN = 3694;
	private static final int TEST_INSTRUCTIONS1 = 3695;
	private static final int TEST_INSTRUCTIONS2 = 3696;
	private static final int CYBELLINS_REQUEST = 3697;
	private static final int GUARDIAN_BASILISK_SCALE = 3709;
	private static final int KARUT_WEED = 3710;
	private static final int HAKAS_HEAD = 3711;
	private static final int JAKAS_HEAD = 3712;
	private static final int MARKAS_HEAD = 3713;
	private static final int WINDSUS_ALEPH_SKIN = 3714;
	private static final int INDIGO_RUNESTONE = 3715;
	private static final int SPORESEA_SEED = 3716;
	private static final int TIMAK_ORC_TOTEM = 3717;
	private static final int TRISALIM_SILK = 3718;
	private static final int AMBROSIUS_FRUIT = 3719;
	private static final int BALEFIRE_CRYSTAL = 3720;
	private static final int IMPERIAL_ARROWHEAD = 3721;
	private static final int ATHUS_HEAD = 3722;
	private static final int LANKAS_HEAD = 3723;
	private static final int TRISKAS_HEAD = 3724;
	private static final int MOTURAS_HEAD = 3725;
	private static final int KALATHS_HEAD = 3726;
	// @formatter:off
	private static final int[] Q_BLOOD_CRYSTAL =
	{
		3708, 3698, 3699, 3700, 3701, 3702, 3703, 3704, 3705, 3706, 3707
	};
	private static final int[] Q_BLOOD_CRYSTAL_LIZARDMEN =
	{
		20578, 20579, 20580, 20581, 20582, 20641, 20642, 20643, 20644, 20645
	};
	private static final int[][][] ITEMS_1ST_CIRCLE =
	{
		{{GUARDIAN_BASILISK_SCALE}, {40}, {20550, 75}},
		{{KARUT_WEED}, {20}, {20581, 50}},
		{{HAKAS_HEAD, JAKAS_HEAD, MARKAS_HEAD}, {3}},
		{{WINDSUS_ALEPH_SKIN}, {1}, {WINDSUS_ALEPH, 100}},
		{{INDIGO_RUNESTONE}, {20}, {20563, 50}, {20565, 50}},
		{{SPORESEA_SEED}, {30}, {20555, 70}}
	};
	private static final int[][][] ITEMS_2ND_CIRCLE =
	{
		{{TIMAK_ORC_TOTEM}, {20}, {20586, 50}},
		{{TRISALIM_SILK}, {20}, {20560, 50}, {20561, 50}},
		{{AMBROSIUS_FRUIT}, {30}, {20591, 75}, {20597, 75}},
		{{BALEFIRE_CRYSTAL}, {20}, {20675, 50}},
		{{IMPERIAL_ARROWHEAD}, {20}, {20660, 50}},
		{{ATHUS_HEAD, LANKAS_HEAD, TRISKAS_HEAD, MOTURAS_HEAD, KALATHS_HEAD}, {5}}
	};
	// @formatter:on
	private static final Request[] REQUESTS1 =
	{
		new Request(3727, 3769, 40, 4500, "C: 40 Totems of Kadesh").addDrop(20578, 80).addDrop(20579, 83),
		new Request(3728, 3770, 50, 7500, "C: 50 Jade Necklaces of Timak").addDrop(20586, 89).addDrop(20588, 100),
		new Request(3729, 3771, 50, 7300, "C: 50 Enchanted Golem Shards").addDrop(20565, 100),
		new Request(3730, 3772, 30, 5500, "C: 30 Pieces Monster Eye Meat").addDrop(20556, 50),
		new Request(3731, 3773, 40, 5000, "C: 40 Eggs of Dire Wyrm").addDrop(20557, 80),
		new Request(3732, 3774, 100, 6500, "C: 100 Claws of Guardian Basilisk").addDrop(20550, 150),
		new Request(3733, 3775, 50, 4400, "C: 50 Revenant Chains").addDrop(20552, 100),
		new Request(3734, 3776, 30, 5200, "C: 30 Windsus Tusks").addDrop(WINDSUS, 50),
		new Request(3735, 3777, 100, 7600, "C: 100 Skulls of Grandis").addDrop(20554, 200),
		new Request(3736, 3778, 50, 4900, "C: 50 Taik Obsidian Amulets").addDrop(20631, 100).addDrop(20632, 93),
		new Request(3737, 3779, 30, 7600, "C: 30 Heads of Karul Bugbear").addDrop(20600, 50),
		new Request(3738, 3780, 40, 7200, "C: 40 Ivory Charms of Tamlin").addDrop(20601, 62).addDrop(20602, 80),
		new Request(3739, 3781, 1, 4500, "B: Situation Preparation - Leto Chief").addSpawn(20582, 27157, 10).addDrop(27157, 100),
		new Request(3740, 3782, 50, 9500, "B: 50 Enchanted Gargoyle Horns").addDrop(20567, 50),
		new Request(3741, 3783, 50, 5800, "B: 50 Coiled Serpent Totems").addDrop(20269, 93).addDrop(BREKA_ORC_WARRIOR, 100),
		new Request(3742, 3784, 1, 4500, "B: Situation Preparation - Sorcerer Catch of Leto").addSpawn(20581, 27156, 10).addDrop(27156, 100),
		new Request(3743, 3785, 1, 7000, "B: Situation Preparation - Timak Raider Kaikee").addSpawn(20586, 27158, 10).addDrop(27158, 100),
		new Request(3744, 3786, 30, 10000, "B: 30 Kronbe Venom Sacs").addDrop(20603, 50),
		new Request(3745, 3787, 30, 18000, "A: 30 Eva's Charm").addDrop(20562, 50),
		new Request(3746, 3788, 1, 12000, "A: Titan's Tablet").addSpawn(20554, 27160, 10).addDrop(27160, 100),
		new Request(3747, 3789, 1, 15000, "A: Book of Shunaiman").addSpawn(20600, 27164, 10).addDrop(27164, 100)
	};
	private static final Request[] REQUESTS2 =
	{
		new Request(3748, 3790, 40, 6200, "C: 40 Rotting Tree Spores").addDrop(20558, 67),
		new Request(3749, 3791, 40, 5900, "C: 40 Trisalim Venom Sacs").addDrop(20560, 66).addDrop(20561, 75),
		new Request(3750, 3792, 50, 7200, "C: 50 Totems of Taik Orc").addDrop(20633, 53).addDrop(20634, 99),
		new Request(3751, 3793, 40, 7200, "C: 40 Harit Barbed Necklaces").addDrop(20641, 88).addDrop(20642, 88).addDrop(20643, 91),
		new Request(3752, 3794, 20, 8700, "C: 20 Coins of Ancient Empire").addDrop(20661, 50).addSpawn(20661, 27149, 5).addDrop(20662, 52).addSpawn(20662, 27149, 5).addDrop(27149, 300),
		new Request(3753, 3795, 30, 11600, "C: 30 Skins of Farkran").addDrop(20667, 90),
		new Request(3754, 3796, 40, 6200, "C: 40 Tempest Shards").addDrop(20589, 49).addSpawn(20589, 27149, 5).addDrop(27149, 500),
		new Request(3755, 3797, 40, 7800, "C: 40 Tsunami Shards").addDrop(20590, 51).addSpawn(20590, 27149, 5).addDrop(27149, 500),
		new Request(3756, 3798, 40, 7800, "C: 40 Manes of Pan Ruem").addDrop(20592, 80).addDrop(20598, 100),
		new Request(3757, 3799, 40, 7000, "C: 40 Hamadryad Shard").addDrop(20594, 64).addSpawn(20594, 27149, 5).addDrop(27149, 500),
		new Request(3758, 3800, 30, 7100, "C: 30 Manes of Vanor Silenos").addDrop(20682, 70).addDrop(20683, 85).addDrop(20684, 90),
		new Request(3759, 3801, 30, 13400, "C: 30 Totems of Tarlk Bugbears").addDrop(TARLK_BUGBEAR_WARRIOR, 63),
		new Request(3760, 3802, 1, 8200, "B: Situation Preparation - Overlord Okun of Timak").addSpawn(20588, 27159, 10).addDrop(27159, 100),
		new Request(3761, 3803, 1, 5300, "B: Situation Preparation - Overlord Kakran of Taik").addSpawn(20634, 27161, 10).addDrop(27161, 100),
		new Request(3762, 3804, 40, 8800, "B: 40 Narcissus Soulstones").addDrop(20639, 86).addSpawn(20639, 27149, 5).addDrop(27149, 500),
		new Request(3763, 3805, 20, 11000, "B: 20 Eyes of Deprived").addDrop(20664, 77),
		new Request(3764, 3806, 20, 8800, "B: 20 Unicorn Horns").addDrop(20593, 68).addDrop(20599, 86),
		new Request(3765, 3807, 1, 5500, "B: Golden Mane of Silenos").addSpawn(20686, 27163, 10).addDrop(27163, 100),
		new Request(3766, 3808, 20, 16000, "A: 20 Skulls of Executed Person").addDrop(20659, 73),
		new Request(3767, 3809, 1, 18000, "A: Bust of Travis").addSpawn(20662, 27162, 10).addDrop(27162, 100),
		new Request(3768, 3810, 10, 18000, "A: 10 Swords of Cadmus").addDrop(20676, 64)
	};
	
	public Q00335_TheSongOfTheHunter()
	{
		super(335, "Song of the Hunter");
		addStartNpc(GREY);
		addTalkId(GREY, CYBELLIN, TOR);
		addKillId(BREKA_OVERLORD_HAKA);
		addKillId(BREKA_OVERLORD_JAKA);
		addKillId(BREKA_OVERLORD_MARKA);
		addKillId(TARLK_RAIDER_ATHU);
		addKillId(TARLK_RAIDER_LANKA);
		addKillId(TARLK_RAIDER_TRISKA);
		addKillId(TARLK_RAIDER_MOTURA);
		addKillId(TARLK_RAIDER_KALATH);
		addKillId(Q_BLOOD_CRYSTAL_LIZARDMEN);
		final List<Integer> questItems = new ArrayList<>();
		for (int[][] ItemsCond : ITEMS_1ST_CIRCLE)
		{
			for (int i : ItemsCond[0])
			{
				questItems.add(i);
			}
			
			for (int i = 2; i < ItemsCond.length; ++i)
			{
				addKillId(ItemsCond[i][0]);
			}
		}
		
		for (int[][] ItemsCond : ITEMS_2ND_CIRCLE)
		{
			for (int i : ItemsCond[0])
			{
				questItems.add(i);
			}
			
			for (int i = 2; i < ItemsCond.length; ++i)
			{
				addKillId(ItemsCond[i][0]);
			}
		}
		
		for (Request r : REQUESTS1)
		{
			questItems.add(r.request_id);
			questItems.add(r.request_item);
			for (int id : r.droplist.keySet())
			{
				addKillId(id);
			}
			
			for (int id : r.spawnlist.keySet())
			{
				addKillId(id);
			}
		}
		
		for (Request r : REQUESTS2)
		{
			questItems.add(r.request_id);
			questItems.add(r.request_item);
			for (int id : r.droplist.keySet())
			{
				addKillId(id);
			}
			
			for (int id : r.spawnlist.keySet())
			{
				addKillId(id);
			}
		}
		
		questItems.add(CIRCLE_HUNTER_LICENSE1);
		questItems.add(CIRCLE_HUNTER_LICENSE2);
		questItems.add(LAUREL_LEAF_PIN);
		questItems.add(TEST_INSTRUCTIONS1);
		questItems.add(TEST_INSTRUCTIONS2);
		questItems.add(CYBELLINS_REQUEST);
		questItems.add(CYBELLINS_DAGGER);
		for (int i : Q_BLOOD_CRYSTAL)
		{
			questItems.add(i);
		}
		
		registerQuestItems(questItems.stream().mapToInt(i -> i).toArray());
	}
	
	private static int calcItemsConds(Player player, int[][][] itemConds)
	{
		int result = 0;
		for (int[][] itemCond : itemConds)
		{
			int count = 0;
			for (int i : itemCond[0])
			{
				count += getQuestItemsCount(player, i);
			}
			
			if (count >= itemCond[1][0])
			{
				++result;
			}
		}
		
		return result;
	}
	
	private void delItemsConds(Player player, int[][][] itemConds)
	{
		for (int[][] itemCond : itemConds)
		{
			for (int i : itemCond[0])
			{
				takeItems(player, i, -1);
			}
		}
	}
	
	private static int getBloodCrystalLevel(Player player)
	{
		for (int i = Q_BLOOD_CRYSTAL.length - 1; i >= 0; --i)
		{
			if (getQuestItemsCount(player, Q_BLOOD_CRYSTAL[i]) > 0)
			{
				return i;
			}
		}
		
		return -1;
	}
	
	private static boolean bloodCrystal2Adena(Player player, int bloodCrystalLevel)
	{
		if (bloodCrystalLevel < 2)
		{
			return false;
		}
		
		for (int i : Q_BLOOD_CRYSTAL)
		{
			takeItems(player, i, -1);
		}
		
		giveAdena(player, (3400 * (int) Math.pow(2, bloodCrystalLevel - 2)), true);
		return true;
	}
	
	private void genList(QuestState st)
	{
		// final int grade_c = 12;
		// final int grade_b = 6;
		// final int grade_a = 3;
		if ((st.get("list") == null) || st.get("list").isEmpty())
		{
			final long laurelLeafPinCount = getQuestItemsCount(st.getPlayer(), LAUREL_LEAF_PIN);
			final int[] list = new int[5];
			if (laurelLeafPinCount < 4L)
			{
				if ((laurelLeafPinCount == 0) || (getRandom(100) < 80))
				{
					for (int i = 0; i < 5; ++i)
					{
						list[i] = getRandom(12);
					}
				}
				else
				{
					list[0] = 12 + getRandom(6);
					list[1] = getRandom(12);
					list[2] = getRandom(6);
					list[3] = 6 + getRandom(6);
					list[4] = getRandom(12);
				}
			}
			else if (getRandom(100) < 20)
			{
				list[0] = 12 + getRandom(6);
				list[1] = getRandom(100) < 5 ? (18 + getRandom(3)) : getRandom(12);
				list[2] = getRandom(6);
				list[3] = 6 + getRandom(6);
				list[4] = getRandom(12);
			}
			else
			{
				list[0] = getRandom(12);
				list[1] = getRandom(100) < 5 ? (18 + getRandom(3)) : getRandom(12);
				list[2] = getRandom(6);
				list[3] = 6 + getRandom(6);
				list[4] = getRandom(12);
			}
			
			boolean sortFlag;
			do
			{
				sortFlag = false;
				for (int j = 1; j < list.length; ++j)
				{
					if (list[j] < list[j - 1])
					{
						final int tmp = list[j];
						list[j] = list[j - 1];
						list[j - 1] = tmp;
						sortFlag = true;
					}
				}
			}
			while (sortFlag);
			int packedlist = 0;
			try
			{
				packedlist = packInt(list, 5);
			}
			catch (Exception e)
			{
				// Ignore.
			}
			
			st.set("list", String.valueOf(packedlist));
		}
	}
	
	private static String formatList(QuestState st, Request[] requests)
	{
		String result = "<html><head><body>Guild Member Tor:<br>%reply%<br>%reply%<br>%reply%<br>%reply%<br>%reply%<br></body></html>";
		final int[] listpacked = unpackInt(st.getInt("list"), 5);
		for (int i = 0; i <= 5; ++i)
		{
			final String s = "<a action=\"bypass Script Q00335_TheSongOfTheHunter 30745-request-" + requests[listpacked[i]].request_id + "\">" + requests[listpacked[i]].text + "</a>";
			result = result.replaceFirst("%reply%", s);
		}
		
		return result;
	}
	
	private static Request getCurrentRequest(Player player, Request[] requests)
	{
		for (Request r : requests)
		{
			if (getQuestItemsCount(player, r.request_id) > 0)
			{
				return r;
			}
		}
		
		return null;
	}
	
	private static boolean isValidRequest(int id)
	{
		for (Request r : REQUESTS1)
		{
			if (r.request_id == id)
			{
				return true;
			}
		}
		
		for (Request r : REQUESTS2)
		{
			if (r.request_id == id)
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return event;
		}
		
		String htmltext = event;
		final int state = st.getState();
		if ("30744_03.htm".equals(htmltext))
		{
			if (state == 1)
			{
				if (getQuestItemsCount(player, TEST_INSTRUCTIONS1) == 0)
				{
					giveItems(player, TEST_INSTRUCTIONS1, 1);
				}
				
				st.startQuest();
			}
		}
		else if ("30744_09.htm".equals(htmltext))
		{
			if (state == 2)
			{
				if (getCurrentRequest(player, REQUESTS1) != null)
				{
					return "30744_09a.htm";
				}
				
				if (getQuestItemsCount(player, TEST_INSTRUCTIONS2) == 0)
				{
					playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
					giveItems(player, TEST_INSTRUCTIONS2, 1);
				}
			}
		}
		else if ("30744_16.htm".equals(htmltext))
		{
			if (state == 2)
			{
				if (getQuestItemsCount(player, LAUREL_LEAF_PIN) >= 20)
				{
					giveAdena(player, 20000, true);
					htmltext = "30744_17.htm";
				}
				
				st.exitQuest(true, true);
			}
		}
		else if ("30746_03.htm".equals(htmltext))
		{
			if (state == 2)
			{
				if ((getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE1) == 0) && (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE2) == 0))
				{
					return null;
				}
				
				if (getQuestItemsCount(player, CYBELLINS_DAGGER) == 0)
				{
					giveItems(player, CYBELLINS_DAGGER, 1);
				}
				
				if (getQuestItemsCount(player, CYBELLINS_REQUEST) == 0)
				{
					giveItems(player, CYBELLINS_REQUEST, 1);
				}
				
				for (int i : Q_BLOOD_CRYSTAL)
				{
					takeItems(player, i, -1);
				}
				
				playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
				giveItems(player, Q_BLOOD_CRYSTAL[1], 1);
			}
		}
		else if ("30746_06.htm".equals(htmltext))
		{
			if ((state == 2) && !bloodCrystal2Adena(player, getBloodCrystalLevel(player)))
			{
				return null;
			}
		}
		else if ("30746_10.htm".equals(htmltext))
		{
			if (state == 2)
			{
				takeItems(player, CYBELLINS_DAGGER, -1);
				takeItems(player, CYBELLINS_REQUEST, -1);
				for (int i : Q_BLOOD_CRYSTAL)
				{
					takeItems(player, i, -1);
				}
			}
		}
		else if ("30745_02.htm".equals(htmltext))
		{
			if ((state == 2) && (getQuestItemsCount(player, TEST_INSTRUCTIONS2) > 0))
			{
				return "30745_03.htm";
			}
		}
		else if ("30745_05b.htm".equals(htmltext))
		{
			if (state == 2)
			{
				if (getQuestItemsCount(player, LAUREL_LEAF_PIN) > 0)
				{
					takeItems(player, LAUREL_LEAF_PIN, 1);
				}
				
				for (Request r : REQUESTS1)
				{
					takeItems(player, r.request_id, -1);
					takeItems(player, r.request_item, -1);
				}
				
				for (Request r : REQUESTS2)
				{
					takeItems(player, r.request_id, -1);
					takeItems(player, r.request_item, -1);
				}
			}
		}
		else if (state == 2)
		{
			if ("30745-list1".equals(htmltext))
			{
				genList(st);
				return formatList(st, REQUESTS1);
			}
			
			if ("30745-list2".equals(htmltext))
			{
				genList(st);
				return formatList(st, REQUESTS2);
			}
			
			if (htmltext.startsWith("30745-request-"))
			{
				htmltext = htmltext.replaceFirst("30745-request-", "");
				int requestId;
				try
				{
					requestId = Integer.parseInt(htmltext);
				}
				catch (Exception e)
				{
					return null;
				}
				
				if (!isValidRequest(requestId))
				{
					return null;
				}
				
				giveItems(player, requestId, 1);
				return "30745-" + requestId + ".htm";
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = player.getQuestState(getName());
		final String htmltext = getNoQuestMsg(player);
		if (st == null)
		{
			return htmltext;
		}
		
		final int state = st.getState();
		final int npcId = npc.getId();
		if (state == 1)
		{
			if (npcId != GREY)
			{
				return htmltext;
			}
			
			if (st.getPlayer().getLevel() < 35)
			{
				st.exitQuest(true);
				return "30744_01.htm";
			}
			
			st.setCond(0);
			st.unset("list");
			return "30744_02.htm";
		}
		
		if (state != 2)
		{
			return htmltext;
		}
		
		if (npcId == GREY)
		{
			if (getQuestItemsCount(player, TEST_INSTRUCTIONS1) > 0)
			{
				if (calcItemsConds(player, ITEMS_1ST_CIRCLE) < 3)
				{
					return "30744_05.htm";
				}
				
				delItemsConds(player, ITEMS_1ST_CIRCLE);
				takeItems(player, TEST_INSTRUCTIONS1, -1);
				giveItems(player, CIRCLE_HUNTER_LICENSE1, 1);
				st.setCond(2, true);
				return "30744_06.htm";
			}
			
			if (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE1) > 0)
			{
				if (st.getPlayer().getLevel() < 45)
				{
					return "30744_07.htm";
				}
				
				if (getQuestItemsCount(player, TEST_INSTRUCTIONS2) == 0)
				{
					return "30744_08.htm";
				}
			}
			
			if (getQuestItemsCount(player, TEST_INSTRUCTIONS2) > 0)
			{
				if (calcItemsConds(player, ITEMS_2ND_CIRCLE) < 3)
				{
					return "30744_11.htm";
				}
				
				delItemsConds(player, ITEMS_2ND_CIRCLE);
				takeItems(player, TEST_INSTRUCTIONS2, -1);
				takeItems(player, CIRCLE_HUNTER_LICENSE1, -1);
				giveItems(player, CIRCLE_HUNTER_LICENSE2, 1);
				st.setCond(3, true);
				return "30744_12.htm";
			}
			else if (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE2) > 0)
			{
				return "30744_14.htm";
			}
		}
		
		if (npcId == CYBELLIN)
		{
			if ((getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE1) == 0) && (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE2) == 0))
			{
				return "30746_01.htm";
			}
			
			if (getQuestItemsCount(player, CYBELLINS_REQUEST) == 0)
			{
				return "30746_02.htm";
			}
			
			final int bloodCrystalLevel = getBloodCrystalLevel(player);
			if (bloodCrystalLevel == -1)
			{
				return "30746_08.htm";
			}
			
			if (bloodCrystalLevel == 0)
			{
				return "30746_09.htm";
			}
			
			if (bloodCrystalLevel == 1)
			{
				return "30746_04.htm";
			}
			
			if ((bloodCrystalLevel > 1) && (bloodCrystalLevel < 10))
			{
				return "30746_05.htm";
			}
			
			if ((bloodCrystalLevel == 10) && bloodCrystal2Adena(player, bloodCrystalLevel))
			{
				return "30746_05a.htm";
			}
		}
		
		if (npcId == TOR)
		{
			if ((getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE1) == 0) && (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE2) == 0))
			{
				return "30745_01a.htm";
			}
			
			if (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE1) > 0)
			{
				final Request request = getCurrentRequest(player, REQUESTS1);
				if (request != null)
				{
					return request.complete(st) ? "30745_06a.htm" : "30745_05.htm";
				}
				
				if (st.getPlayer().getLevel() < 45)
				{
					return "30745_01b.htm";
				}
				
				return (getQuestItemsCount(player, TEST_INSTRUCTIONS2) > 0) ? "30745_03.htm" : "30745_03a.htm";
			}
			else if (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE2) > 0)
			{
				final Request request = getCurrentRequest(player, REQUESTS2);
				if (request == null)
				{
					return "30745_03b.htm";
				}
				
				return request.complete(st) ? "30745_06b.htm" : "30745_05.htm";
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
		
		if (st.getState() != 2)
		{
			return;
		}
		
		final int npcId = npc.getId();
		int[][][] itemsCircle = null;
		if (getQuestItemsCount(player, TEST_INSTRUCTIONS1) > 0)
		{
			itemsCircle = ITEMS_1ST_CIRCLE;
		}
		else if (getQuestItemsCount(player, TEST_INSTRUCTIONS2) > 0)
		{
			itemsCircle = ITEMS_2ND_CIRCLE;
		}
		
		if (itemsCircle != null)
		{
			for (int[][] itemCond : itemsCircle)
			{
				for (int i = 2; i < itemCond.length; ++i)
				{
					if ((npcId == itemCond[i][0]) && (getRandom(100) < itemCond[i][1]) && (getQuestItemsCount(player, itemCond[0][0]) < itemCond[1][0]))
					{
						giveItems(player, itemCond[0][0], 1);
					}
				}
			}
			
			if (getQuestItemsCount(player, TEST_INSTRUCTIONS1) > 0)
			{
				final long hakasHeadCount = getQuestItemsCount(player, HAKAS_HEAD);
				final long jakasHeadCount = getQuestItemsCount(player, JAKAS_HEAD);
				final long markasHeadCount = getQuestItemsCount(player, MARKAS_HEAD);
				if (npcId == BREKA_ORC_WARRIOR)
				{
					if ((hakasHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(BREKA_OVERLORD_HAKA, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
					else if ((jakasHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(BREKA_OVERLORD_JAKA, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
					else if ((markasHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(BREKA_OVERLORD_MARKA, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
				}
				else if (npcId == BREKA_OVERLORD_HAKA)
				{
					if (hakasHeadCount == 0)
					{
						giveItems(player, HAKAS_HEAD, 1);
					}
				}
				else if (npcId == BREKA_OVERLORD_JAKA)
				{
					if (jakasHeadCount == 0)
					{
						giveItems(player, JAKAS_HEAD, 1);
					}
				}
				else if (npcId == BREKA_OVERLORD_MARKA)
				{
					if (markasHeadCount == 0)
					{
						giveItems(player, MARKAS_HEAD, 1);
					}
				}
				else if ((npcId == WINDSUS) && (getQuestItemsCount(player, WINDSUS_ALEPH_SKIN) == 0) && (getRandom(100) < 10))
				{
					addSpawn(WINDSUS_ALEPH, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
				}
			}
			else if (getQuestItemsCount(player, TEST_INSTRUCTIONS2) > 0)
			{
				final long athusHeadCount = getQuestItemsCount(player, ATHUS_HEAD);
				final long lankasHeadCount = getQuestItemsCount(player, LANKAS_HEAD);
				final long triskasHeadCount = getQuestItemsCount(player, TRISKAS_HEAD);
				final long moturasHeadCount = getQuestItemsCount(player, MOTURAS_HEAD);
				final long kalathsHeadCount = getQuestItemsCount(player, KALATHS_HEAD);
				if (npcId == TARLK_BUGBEAR_WARRIOR)
				{
					if ((athusHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(TARLK_RAIDER_ATHU, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
					else if ((lankasHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(TARLK_RAIDER_LANKA, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
					else if ((triskasHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(TARLK_RAIDER_TRISKA, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
					else if ((moturasHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(TARLK_RAIDER_MOTURA, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
					else if ((kalathsHeadCount == 0) && (getRandom(100) < 10))
					{
						addSpawn(TARLK_RAIDER_KALATH, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
					}
				}
				else if (npcId == TARLK_RAIDER_ATHU)
				{
					if (athusHeadCount == 0)
					{
						giveItems(player, ATHUS_HEAD, 1);
					}
				}
				else if (npcId == TARLK_RAIDER_LANKA)
				{
					if (lankasHeadCount == 0)
					{
						giveItems(player, LANKAS_HEAD, 1);
					}
				}
				else if (npcId == TARLK_RAIDER_TRISKA)
				{
					if (triskasHeadCount == 0)
					{
						giveItems(player, TRISKAS_HEAD, 1);
					}
				}
				else if (npcId == TARLK_RAIDER_MOTURA)
				{
					if (moturasHeadCount == 0)
					{
						giveItems(player, MOTURAS_HEAD, 1);
					}
				}
				else if ((npcId == TARLK_RAIDER_KALATH) && (kalathsHeadCount == 0))
				{
					giveItems(player, KALATHS_HEAD, 1);
				}
			}
		}
		
		if ((getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE1) > 0) || (getQuestItemsCount(player, CIRCLE_HUNTER_LICENSE2) > 0))
		{
			if ((getQuestItemsCount(player, CYBELLINS_REQUEST) > 0) && (st.getPlayer().getActiveWeaponItem() != null) && (st.getPlayer().getActiveWeaponItem().getId() == 3471))
			{
				final int bloodCrystalLevel = getBloodCrystalLevel(player);
				if ((bloodCrystalLevel > 0) && (bloodCrystalLevel < 10))
				{
					for (int lizardmen_id : Q_BLOOD_CRYSTAL_LIZARDMEN)
					{
						if (npcId == lizardmen_id)
						{
							if (getRandom(100) < 50)
							{
								takeItems(player, Q_BLOOD_CRYSTAL[bloodCrystalLevel], -1);
								playSound(player, bloodCrystalLevel < 6 ? QuestSound.ITEMSOUND_QUEST_MIDDLE : QuestSound.ITEMSOUND_QUEST_JACKPOT);
								giveItems(player, Q_BLOOD_CRYSTAL[bloodCrystalLevel + 1], 1);
							}
							else
							{
								for (int i : Q_BLOOD_CRYSTAL)
								{
									takeItems(player, i, -1);
								}
								
								giveItems(player, Q_BLOOD_CRYSTAL[0], 1);
							}
						}
					}
				}
			}
			
			Request request = getCurrentRequest(player, REQUESTS1);
			if (request == null)
			{
				request = getCurrentRequest(player, REQUESTS2);
			}
			
			if (request != null)
			{
				if (request.droplist.containsKey(npcId) && (getRandom(100) < request.droplist.get(npcId)) && (getQuestItemsCount(player, request.request_item) < request.request_count))
				{
					giveItems(player, request.request_item, 1);
				}
				
				if (request.spawnlist.containsKey(npcId) && (getQuestItemsCount(player, request.request_item) < request.request_count))
				{
					final int[] spawnChance = request.spawnlist.get(npcId);
					if (getRandom(100) < spawnChance[1])
					{
						addSpawn(spawnChance[0], npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
						if (spawnChance[0] == 27149)
						{
							npc.broadcastSay(ChatType.GENERAL, "Show me the pretty sparkling things! They're all mine!");
						}
					}
				}
			}
		}
		
		if (((npcId == 27160) || (npcId == 27162) || (npcId == 27164)) && (getRandom(100) < 50))
		{
			npc.broadcastSay(ChatType.GENERAL, "We'll take the property of the ancient empire!");
			addSpawn(27150, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
			addSpawn(27150, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 300000);
		}
	}
	
	public static class Request
	{
		public int request_id;
		public int request_item;
		public int request_count;
		public int reward_adena;
		public String text;
		public Map<Integer, Integer> droplist;
		public Map<Integer, int[]> spawnlist;
		
		public Request(int requestid, int requestitem, int requestcount, int rewardadena, String txt)
		{
			droplist = new HashMap<>();
			spawnlist = new HashMap<>();
			request_id = requestid;
			request_item = requestitem;
			request_count = requestcount;
			reward_adena = rewardadena;
			text = txt;
		}
		
		public Request addDrop(int killMobId, int chance)
		{
			droplist.put(killMobId, chance);
			return this;
		}
		
		public Request addSpawn(int killMobId, int spawnMobId, int chance)
		{
			try
			{
				spawnlist.put(killMobId, new int[]
				{
					spawnMobId,
					chance
				});
			}
			catch (Exception e)
			{
				// Ignore.
			}
			
			return this;
		}
		
		public boolean complete(QuestState st)
		{
			final Player player = st.getPlayer();
			if (getQuestItemsCount(player, request_item) < request_count)
			{
				return false;
			}
			
			takeItems(player, request_id, -1);
			takeItems(player, request_item, -1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
			giveItems(player, LAUREL_LEAF_PIN, 1);
			giveAdena(player, reward_adena, true);
			st.unset("list");
			return true;
		}
	}
	
	private static int packInt(int[] a, int bits) throws Exception
	{
		final int m = 32 / bits;
		if (a.length > m)
		{
			throw new Exception("Overflow");
		}
		
		int result = 0;
		final int mval = (int) Math.pow(2, bits);
		for (int i = 0; i < m; ++i)
		{
			result <<= bits;
			int next;
			if (a.length > i)
			{
				next = a[i];
				if ((next >= mval) || (next < 0))
				{
					throw new Exception("Overload, value is out of range");
				}
			}
			else
			{
				next = 0;
			}
			
			result += next;
		}
		
		return result;
	}
	
	private static int[] unpackInt(int value, int bits)
	{
		final int m = 32 / bits;
		final int mval = (int) Math.pow(2, bits);
		final int[] result = new int[m];
		int a = value;
		for (int i = m; i > 0; --i)
		{
			final int next = a;
			a >>= bits;
			result[i - 1] = next - (a * mval);
		}
		
		return result;
	}
}
