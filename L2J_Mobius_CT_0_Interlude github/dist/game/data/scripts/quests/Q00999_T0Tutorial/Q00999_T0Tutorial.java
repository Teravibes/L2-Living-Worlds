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
package quests.Q00999_T0Tutorial;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.script.State;

public class Q00999_T0Tutorial extends Script
{
	// Items
	private static final int RECOMMENDATION_01 = 1067;
	private static final int RECOMMENDATION_02 = 1068;
	private static final int LEAF_OF_MOTHERTREE = 1069;
	private static final int BLOOD_OF_JUNDIN = 1070;
	private static final int LICENSE_OF_MINER = 1498;
	private static final int VOUCHER_OF_FLAME = 1496;
	private static final int SOULSHOT_NOVICE = 5789;
	private static final int SPIRITSHOT_NOVICE = 5790;
	private static final int BLUE_GEM = 6353;
	
	// NPCs
	private static final int[] NPCS =
	{
		30008,
		30009,
		30017,
		30019,
		30129,
		30131,
		30573,
		30575,
		30370,
		30528,
		30530,
		30400,
		30401,
		30402,
		30403,
		30404
	};
	private static final HashMap<Object, Object[]> Event = new HashMap<>();
	private static final Map<Integer, Talk> Talks = new HashMap<>();
	
	private static class Talk
	{
		public int _raceId;
		public String[] _htmlfiles;
		public int _npcType;
		public int _item;
		
		public Talk(int raceId, String[] htmlfiles, int npcType, int item)
		{
			_raceId = raceId;
			_htmlfiles = htmlfiles;
			_npcType = npcType;
			_item = item;
		}
	}
	
	public Q00999_T0Tutorial()
	{
		// @formatter:off
		Event.put("30008_02", new Object[]{"30008-03.htm", -84058, 243239, -3730, RECOMMENDATION_01, 0x00, SOULSHOT_NOVICE, 200, 0x00, 0, 0});
		Event.put("30008_04", new Object[]{"30008-04.htm", -84058, 243239, -3730, 0, 0x00, 0, 0, 0, 0, 0});
		Event.put("30017_02", new Object[]{"30017-03.htm", -84058, 243239, -3730, RECOMMENDATION_02, 0x0a, SPIRITSHOT_NOVICE, 100, 0x00, 0, 0});
		Event.put("30017_04", new Object[]{"30017-04.htm", -84058, 243239, -3730, 0, 0x0a, 0, 0, 0x00, 0, 0});
		Event.put("30370_02", new Object[]{"30370-03.htm", 45491, 48359, -3086, LEAF_OF_MOTHERTREE, 0x19, SPIRITSHOT_NOVICE, 100, 0x12, SOULSHOT_NOVICE, 200});
		Event.put("30370_04", new Object[]{"30370-04.htm", 45491, 48359, -3086, 0, 0x19, 0, 0, 0x12, 0, 0});
		Event.put("30129_02", new Object[]{"30129-03.htm", 12116, 16666, -4610, BLOOD_OF_JUNDIN, 0x26, SPIRITSHOT_NOVICE, 100, 0x1f, SOULSHOT_NOVICE, 200});
		Event.put("30129_04", new Object[]{"30129-04.htm", 12116, 16666, -4610, 0, 0x26, 0, 0, 0x1f, 0, 0});
		Event.put("30528_02", new Object[]{"30528-03.htm", 115642, -178046, -941, LICENSE_OF_MINER, 0x35, SOULSHOT_NOVICE, 200, 0x00, 0, 0});
		Event.put("30528_04", new Object[]{"30528-04.htm", 115642, -178046, -941, 0, 0x35, 0, 0, 0x00, 0, 0});
		Event.put("30573_02", new Object[]{"30573-03.htm", -45067, -113549, -235, VOUCHER_OF_FLAME, 0x31, SPIRITSHOT_NOVICE, 100, 0x2c, SOULSHOT_NOVICE, 200});
		Event.put("30573_04", new Object[]{"30573-04.htm", -45067, -113549, -235, 0, 0x31, 0, 0, 0x2c, 0, 0});
		Talks.put(30017, new Talk(0, new String[]{"30017-01.htm", "30017-02.htm", "30017-04.htm"}, 0, 0));
		Talks.put(30008, new Talk(0, new String[]{"30008-01.htm", "30008-02.htm", "30008-04.htm"}, 0, 0));
		Talks.put(30370, new Talk(1, new String[]{"30370-01.htm", "30370-02.htm", "30370-04.htm"}, 0, 0));
		Talks.put(30129, new Talk(2, new String[]{"30129-01.htm", "30129-02.htm", "30129-04.htm"}, 0, 0));
		Talks.put(30573, new Talk(3, new String[]{"30573-01.htm", "30573-02.htm", "30573-04.htm"}, 0, 0));
		Talks.put(30528, new Talk(4, new String[]{"30528-01.htm", "30528-02.htm", "30528-04.htm"}, 0, 0));
		Talks.put(30018, new Talk(0, new String[]{"30131-01.htm", "", "30019-03a.htm", "30019-04.htm",}, 1, RECOMMENDATION_02));
		Talks.put(30019, new Talk(0, new String[]{"30131-01.htm", "", "30019-03a.htm", "30019-04.htm",}, 1, RECOMMENDATION_02));
		Talks.put(30020, new Talk(0, new String[]{"30131-01.htm", "", "30019-03a.htm", "30019-04.htm",}, 1, RECOMMENDATION_02));
		Talks.put(30021, new Talk(0, new String[]{"30131-01.htm", "", "30019-03a.htm", "30019-04.htm",}, 1, RECOMMENDATION_02));
		Talks.put(30009, new Talk(0, new String[]{"30530-01.htm", "30009-03.htm", "", "30009-04.htm",}, 1, RECOMMENDATION_01));
		Talks.put(30011, new Talk(0, new String[]{"30530-01.htm", "30009-03.htm", "", "30009-04.htm",}, 1, RECOMMENDATION_01));
		Talks.put(30012, new Talk(0, new String[]{"30530-01.htm", "30009-03.htm", "", "30009-04.htm",}, 1, RECOMMENDATION_01));
		Talks.put(30056, new Talk(0, new String[]{"30530-01.htm", "30009-03.htm", "", "30009-04.htm",}, 1, RECOMMENDATION_01));
		Talks.put(30400, new Talk(1, new String[]{"30131-01.htm", "30400-03.htm", "30400-03a.htm", "30400-04.htm",}, 1, LEAF_OF_MOTHERTREE));
		Talks.put(30401, new Talk(1, new String[]{"30131-01.htm", "30400-03.htm", "30400-03a.htm", "30400-04.htm",}, 1, LEAF_OF_MOTHERTREE));
		Talks.put(30402, new Talk(1, new String[]{"30131-01.htm", "30400-03.htm", "30400-03a.htm", "30400-04.htm",}, 1, LEAF_OF_MOTHERTREE));
		Talks.put(30403, new Talk(1, new String[]{"30131-01.htm", "30400-03.htm", "30400-03a.htm", "30400-04.htm",}, 1, LEAF_OF_MOTHERTREE));
		Talks.put(30131, new Talk(2, new String[]{"30131-01.htm", "30131-03.htm", "30131-03a.htm", "30131-04.htm",}, 1, BLOOD_OF_JUNDIN));
		Talks.put(30404, new Talk(2, new String[]{"30131-01.htm", "30131-03.htm", "30131-03a.htm", "30131-04.htm",}, 1, BLOOD_OF_JUNDIN));
		Talks.put(30574, new Talk(3, new String[]{"30575-01.htm", "30575-03.htm", "30575-03a.htm", "30575-04.htm",}, 1, VOUCHER_OF_FLAME));
		Talks.put(30575, new Talk(3, new String[]{"30575-01.htm", "30575-03.htm", "30575-03a.htm", "30575-04.htm",}, 1, VOUCHER_OF_FLAME));
		Talks.put(30530, new Talk(4, new String[]{"30530-01.htm", "30530-03.htm", "", "30530-04.htm",}, 1, LICENSE_OF_MINER));
		// @formatter:on
		for (int startNpc : NPCS)
		{
			addStartNpc(startNpc);
		}
		
		for (int FirstTalkId : NPCS)
		{
			addFirstTalkId(FirstTalkId);
		}
		
		for (int TalkId : NPCS)
		{
			addTalkId(TalkId);
		}
		
		addKillId(18342);
		addKillId(20001);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (PlayerConfig.DISABLE_TUTORIAL)
		{
			return null;
		}
		
		String htmltext = event;
		
		final QuestState st = player.getQuestState(getName());
		final QuestState qs = player.getQuestState("Q00255_Tutorial");
		if (qs == null)
		{
			return htmltext;
		}
		
		final int Ex = qs.getInt("Ex");
		if (event.equalsIgnoreCase("TimerEx_NewbieHelper"))
		{
			if (Ex == 0)
			{
				if (player.getPlayerClass().isMage())
				{
					st.playTutorialVoice("tutorial_voice_009b");
				}
				else
				{
					st.playTutorialVoice("tutorial_voice_009a");
				}
				
				qs.set("Ex", "1");
			}
			else if (Ex == 3)
			{
				st.playTutorialVoice("tutorial_voice_010a");
				qs.set("Ex", "4");
			}
			
			return null;
		}
		else if (event.equalsIgnoreCase("TimerEx_GrandMaster"))
		{
			if (Ex >= 4)
			{
				st.showQuestionMark(7);
				playSound(player, "ItemSound.quest_tutorial");
				st.playTutorialVoice("tutorial_voice_025");
			}
			
			return null;
		}
		else
		{
			final Object[] map = Event.get(event);
			htmltext = (String) map[0];
			final int radarX = (Integer) map[1];
			final int radarY = (Integer) map[2];
			final int radarZ = (Integer) map[3];
			final int item = (Integer) map[4];
			final int classId1 = (Integer) map[5];
			final int gift1 = (Integer) map[6];
			final int count1 = (Integer) map[7];
			final int classId2 = (Integer) map[8];
			final int gift2 = (Integer) map[9];
			final int count2 = (Integer) map[10];
			if (radarX != 0)
			{
				addRadar(player, radarX, radarY, radarZ);
			}
			
			if ((getQuestItemsCount(player, item) > 0) && (st.getInt("onlyone") == 0))
			{
				addExpAndSp(player, 0, 50);
				startQuestTimer("TimerEx_GrandMaster", 60000, npc, player);
				takeItems(player, item, 1);
				st.set("step", "3");
				if (Ex <= 3)
				{
					qs.set("Ex", "4");
				}
				
				if (player.getPlayerClass().getId() == classId1)
				{
					giveItems(player, gift1, count1);
					if (gift1 == SPIRITSHOT_NOVICE)
					{
						st.playTutorialVoice("tutorial_voice_027");
					}
					else
					{
						st.playTutorialVoice("tutorial_voice_026");
					}
				}
				else if ((player.getPlayerClass().getId() == classId2) && (gift2 != 0))
				{
					giveItems(player, gift2, count2);
					st.playTutorialVoice("tutorial_voice_026");
				}
				
				st.set("step", "4");
				st.set("onlyone", "1");
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (PlayerConfig.DISABLE_TUTORIAL)
		{
			return null;
		}
		
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			st = newQuestState(player);
		}
		
		final QuestState qs = player.getQuestState("Q00255_Tutorial");
		if ((qs == null) || qs.isCompleted())
		{
			npc.showChatWindow(player);
			return null;
		}
		
		final int onlyone = st.getInt("onlyone");
		final int Ex = qs.getInt("Ex");
		final int step = st.getInt("step");
		final Talk talk = Talks.get(npc.getId());
		if (((player.getLevel() >= 10) || (onlyone != 0)) && (talk._npcType == 1))
		{
			htmltext = "30575-05.htm";
		}
		else if ((onlyone == 0) && (player.getLevel() < 10))
		{
			if ((talk != null) && (player.getRace().ordinal() == talk._raceId))
			{
				htmltext = talk._htmlfiles[0];
				if (talk._npcType == 1)
				{
					if ((step == 0) && (Ex < 0))
					{
						qs.set("Ex", "0");
						startQuestTimer("TimerEx_NewbieHelper", 30000, npc, player);
						if (player.getPlayerClass().isMage())
						{
							st.set("step", "1");
							st.setState(State.STARTED);
						}
						else
						{
							htmltext = "30530-01.htm";
							st.set("step", "1");
							st.setState(State.STARTED);
						}
					}
					else if ((step == 1) && !hasQuestItems(player, talk._item) && (Ex <= 2))
					{
						if (hasQuestItems(player, BLUE_GEM))
						{
							takeItems(player, BLUE_GEM, -1);
							giveItems(player, talk._item, 1);
							st.set("step", "2");
							qs.set("Ex", "3");
							startQuestTimer("TimerEx_NewbieHelper", 30000, npc, player);
							qs.set("ucMemo", "3");
							if (player.getPlayerClass().isMage())
							{
								st.playTutorialVoice("tutorial_voice_027");
								giveItems(player, SPIRITSHOT_NOVICE, 100);
								htmltext = talk._htmlfiles[2];
								if (htmltext.equals(""))
								{
									htmltext = "<html><body>I`m sorry. I only help warriors. Please go to another Newbie Helper who may assist you.</body></html>";
								}
							}
							else
							{
								st.playTutorialVoice("tutorial_voice_026");
								giveItems(player, SOULSHOT_NOVICE, 200);
								htmltext = talk._htmlfiles[1];
								if (htmltext.equals(""))
								{
									htmltext = "<html><body>I`m sorry. I only help mystics. Please go to another Newbie Helper who may assist you.</body></html>";
								}
							}
						}
						else
						{
							if (player.getPlayerClass().isMage())
							{
								if (player.getRace() == Race.ORC)
								{
									htmltext = "30575-02.htm";
								}
								else
								{
									htmltext = "30131-02.htm";
								}
							}
							else
							{
								htmltext = "30530-02.htm";
							}
						}
					}
					else if (step == 2)
					{
						htmltext = talk._htmlfiles[3];
					}
				}
				else if (talk._npcType == 0)
				{
					if (step == 1)
					{
						htmltext = talk._htmlfiles[0];
					}
					else if (step == 2)
					{
						htmltext = talk._htmlfiles[1];
					}
					else if (step == 3)
					{
						htmltext = talk._htmlfiles[2];
					}
				}
			}
		}
		else if ((step == 4) && (player.getLevel() < 10))
		{
			htmltext = npc.getId() + "-04.htm";
		}
		
		if ((htmltext == null) || htmltext.equals(""))
		{
			npc.showChatWindow(player);
		}
		
		npc.showChatWindow(player);
		
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			return;
		}
		
		final QuestState qs = player.getQuestState("Q00255_Tutorial");
		if (qs == null)
		{
			return;
		}
		
		final int Ex = qs.getInt("Ex");
		if (Ex <= 1)
		{
			st.playTutorialVoice("tutorial_voice_011");
			st.showQuestionMark(3);
			qs.set("Ex", "2");
		}
		
		if ((Ex == 2) && !hasQuestItems(player, BLUE_GEM))
		{
			npc.asMonster().dropItem(player, BLUE_GEM, 1);
			playSound(player, "ItemSound.quest_tutorial");
			st.set("step", "1");
		}
	}
}
