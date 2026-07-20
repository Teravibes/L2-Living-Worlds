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
package quests.Q00125_TheNameOfEvil1;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

import quests.Q00124_MeetingTheElroki.Q00124_MeetingTheElroki;

public class Q00125_TheNameOfEvil1 extends Quest
{
	private static final int MUSHIKA = 32114;
	private static final int KARAKAWEI = 32117;
	private static final int ULU_KAIMU = 32119;
	private static final int BALU_KAIMU = 32120;
	private static final int CHUTA_KAIMU = 32121;
	private static final int ORNITHOMIMUS_CLAW = 8779;
	private static final int DEINONYCHUS_BONE = 8780;
	private static final int EPITAPH_OF_WISDOM = 8781;
	private static final int GAZKH_FRAGMENT = 8782;
	private static final Map<Integer, Integer> ORNITHOMIMUS = new HashMap<>();
	private static final Map<Integer, Integer> DEINONYCHUS = new HashMap<>();
	static
	{
		ORNITHOMIMUS.put(22200, 661);
		ORNITHOMIMUS.put(22201, 330);
		ORNITHOMIMUS.put(22202, 661);
		ORNITHOMIMUS.put(22219, 327);
		ORNITHOMIMUS.put(22224, 327);
		DEINONYCHUS.put(22203, 651);
		DEINONYCHUS.put(22204, 326);
		DEINONYCHUS.put(22205, 651);
		DEINONYCHUS.put(22220, 319);
		DEINONYCHUS.put(22225, 319);
	}
	
	public Q00125_TheNameOfEvil1()
	{
		super(125, "The Name of Evil - 1");
		registerQuestItems(ORNITHOMIMUS_CLAW, DEINONYCHUS_BONE, EPITAPH_OF_WISDOM, GAZKH_FRAGMENT);
		addStartNpc(MUSHIKA);
		addTalkId(MUSHIKA, KARAKAWEI, ULU_KAIMU, BALU_KAIMU, CHUTA_KAIMU);
		addKillId(ORNITHOMIMUS.keySet());
		addKillId(DEINONYCHUS.keySet());
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
			case "32114-05.htm":
			{
				st.startQuest();
				break;
			}
			case "32114-09.htm":
			{
				st.setCond(2, true);
				giveItems(player, GAZKH_FRAGMENT, 1);
				break;
			}
			case "32117-08.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "32117-14.htm":
			{
				st.setCond(5, true);
				break;
			}
			case "32119-14.htm":
			{
				st.setCond(6, true);
				break;
			}
			case "32120-15.htm":
			{
				st.setCond(7, true);
				break;
			}
			case "32121-16.htm":
			{
				st.setCond(8, true);
				takeItems(player, GAZKH_FRAGMENT, -1);
				giveItems(player, EPITAPH_OF_WISDOM, 1);
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
				final QuestState first = player.getQuestState(Q00124_MeetingTheElroki.class.getSimpleName());
				if ((first != null) && first.isCompleted() && (player.getLevel() >= 76))
				{
					htmltext = "32114-01.htm";
				}
				else
				{
					htmltext = "32114-00.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MUSHIKA:
					{
						if (cond == 1)
						{
							htmltext = "32114-07.htm";
						}
						else if (cond == 2)
						{
							htmltext = "32114-10.htm";
						}
						else if ((cond > 2) && (cond < 8))
						{
							htmltext = "32114-11.htm";
						}
						else if (cond == 8)
						{
							htmltext = "32114-12.htm";
							takeItems(player, EPITAPH_OF_WISDOM, -1);
							st.exitQuest(false, true);
						}
						break;
					}
					case KARAKAWEI:
					{
						if (cond == 2)
						{
							htmltext = "32117-01.htm";
						}
						else if (cond == 3)
						{
							htmltext = "32117-09.htm";
						}
						else if (cond == 4)
						{
							if ((getQuestItemsCount(player, ORNITHOMIMUS_CLAW) >= 2) && (getQuestItemsCount(player, DEINONYCHUS_BONE) >= 2))
							{
								htmltext = "32117-10.htm";
								takeItems(player, ORNITHOMIMUS_CLAW, -1);
								takeItems(player, DEINONYCHUS_BONE, -1);
								playSound(player, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							}
							else
							{
								htmltext = "32117-09.htm";
								st.setCond(3);
							}
						}
						else if (cond == 5)
						{
							htmltext = "32117-15.htm";
						}
						else if ((cond == 6) || (cond == 7))
						{
							htmltext = "32117-16.htm";
						}
						else if (cond == 8)
						{
							htmltext = "32117-17.htm";
						}
						break;
					}
					case ULU_KAIMU:
					{
						if (cond == 5)
						{
							npc.doCast(SkillData.getInstance().getSkill(5089, 1));
							htmltext = "32119-01.htm";
						}
						else if (cond == 6)
						{
							htmltext = "32119-14.htm";
						}
						break;
					}
					case BALU_KAIMU:
					{
						if (cond == 6)
						{
							npc.doCast(SkillData.getInstance().getSkill(5089, 1));
							htmltext = "32120-01.htm";
						}
						else if (cond == 7)
						{
							htmltext = "32120-16.htm";
						}
						break;
					}
					case CHUTA_KAIMU:
					{
						if (cond == 7)
						{
							npc.doCast(SkillData.getInstance().getSkill(5089, 1));
							htmltext = "32121-01.htm";
						}
						else if (cond == 8)
						{
							htmltext = "32121-17.htm";
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
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Player partyMember = getRandomPartyMember(player, 3);
		if (partyMember == null)
		{
			return;
		}
		
		final QuestState qs = getQuestState(partyMember, false);
		final int npcId = npc.getId();
		if (ORNITHOMIMUS.containsKey(npcId))
		{
			if ((getQuestItemsCount(player, ORNITHOMIMUS_CLAW) < 2) && (getRandom(1000) < ORNITHOMIMUS.get(npcId)))
			{
				giveItems(player, ORNITHOMIMUS_CLAW, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		else if (DEINONYCHUS.containsKey(npcId) && (getQuestItemsCount(player, DEINONYCHUS_BONE) < 2) && (getRandom(1000) < DEINONYCHUS.get(npcId)))
		{
			giveItems(player, DEINONYCHUS_BONE, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
		
		if ((getQuestItemsCount(player, ORNITHOMIMUS_CLAW) == 2) && (getQuestItemsCount(player, DEINONYCHUS_BONE) == 2))
		{
			qs.setCond(4, true);
		}
	}
}
