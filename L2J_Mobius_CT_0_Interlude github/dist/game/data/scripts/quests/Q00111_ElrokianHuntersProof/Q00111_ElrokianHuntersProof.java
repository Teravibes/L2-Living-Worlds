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
package quests.Q00111_ElrokianHuntersProof;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;

public class Q00111_ElrokianHuntersProof extends Quest
{
	// NPCs
	private static final int MARQUEZ = 32113;
	private static final int MUSHIKA = 32114;
	private static final int ASAMAH = 32115;
	private static final int KIRIKASHIN = 32116;
	
	// Items
	private static final int DIARY_FRAGMENT = 8768;
	private static final int EXPEDITION_MEMBERS_LETTER = 8769;
	private static final int ORNITHOMINUS_CLAW = 8770;
	private static final int DEINONYCHUS_BONE = 8771;
	private static final int PACHYCEPHALOSAURUS_SKIN = 8772;
	private static final int PRACTICE_ELROKIAN_TRAP = 8773;
	
	// Mobs
	private static final Map<Integer, ItemChanceHolder> MOBS_DROP_CHANCES = new HashMap<>();
	static
	{
		MOBS_DROP_CHANCES.put(22196, new ItemChanceHolder(DIARY_FRAGMENT, 0.51, 4)); // velociraptor_leader
		MOBS_DROP_CHANCES.put(22197, new ItemChanceHolder(DIARY_FRAGMENT, 0.51, 4)); // velociraptor
		MOBS_DROP_CHANCES.put(22198, new ItemChanceHolder(DIARY_FRAGMENT, 0.51, 4)); // velociraptor_s
		MOBS_DROP_CHANCES.put(22218, new ItemChanceHolder(DIARY_FRAGMENT, 0.25, 4)); // velociraptor_n
		MOBS_DROP_CHANCES.put(22200, new ItemChanceHolder(ORNITHOMINUS_CLAW, 0.66, 11)); // ornithomimus_leader
		MOBS_DROP_CHANCES.put(22201, new ItemChanceHolder(ORNITHOMINUS_CLAW, 0.33, 11)); // ornithomimus
		MOBS_DROP_CHANCES.put(22202, new ItemChanceHolder(ORNITHOMINUS_CLAW, 0.66, 11)); // ornithomimus_s
		MOBS_DROP_CHANCES.put(22219, new ItemChanceHolder(ORNITHOMINUS_CLAW, 0.33, 11)); // ornithomimus_n
		MOBS_DROP_CHANCES.put(22203, new ItemChanceHolder(DEINONYCHUS_BONE, 0.65, 11)); // deinonychus_leader
		MOBS_DROP_CHANCES.put(22204, new ItemChanceHolder(DEINONYCHUS_BONE, 0.32, 11)); // deinonychus
		MOBS_DROP_CHANCES.put(22205, new ItemChanceHolder(DEINONYCHUS_BONE, 0.66, 11)); // deinonychus_s
		MOBS_DROP_CHANCES.put(22220, new ItemChanceHolder(DEINONYCHUS_BONE, 0.32, 11)); // deinonychus_n
		MOBS_DROP_CHANCES.put(22208, new ItemChanceHolder(PACHYCEPHALOSAURUS_SKIN, 0.50, 11)); // pachycephalosaurus_ldr
		MOBS_DROP_CHANCES.put(22209, new ItemChanceHolder(PACHYCEPHALOSAURUS_SKIN, 0.50, 11)); // pachycephalosaurus
		MOBS_DROP_CHANCES.put(22210, new ItemChanceHolder(PACHYCEPHALOSAURUS_SKIN, 0.50, 11)); // pachycephalosaurus_s
		MOBS_DROP_CHANCES.put(22221, new ItemChanceHolder(PACHYCEPHALOSAURUS_SKIN, 0.49, 11)); // pachycephalosaurus_n
	}
	
	public Q00111_ElrokianHuntersProof()
	{
		super(111, "Elrokian Hunter's Proof");
		registerQuestItems(DIARY_FRAGMENT, EXPEDITION_MEMBERS_LETTER, ORNITHOMINUS_CLAW, DEINONYCHUS_BONE, PACHYCEPHALOSAURUS_SKIN, PRACTICE_ELROKIAN_TRAP);
		addStartNpc(MARQUEZ);
		addTalkId(MARQUEZ, MUSHIKA, ASAMAH, KIRIKASHIN);
		addKillId(MOBS_DROP_CHANCES.keySet());
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
			case "32113-002.htm":
			{
				st.startQuest();
				break;
			}
			case "32115-002.htm":
			{
				st.setCond(3, true);
				break;
			}
			case "32113-009.htm":
			{
				st.setCond(4, true);
				break;
			}
			case "32113-018.htm":
			{
				st.setCond(6, true);
				takeItems(player, DIARY_FRAGMENT, -1);
				giveItems(player, EXPEDITION_MEMBERS_LETTER, 1);
				break;
			}
			case "32116-003.htm":
			{
				st.setCond(7);
				playSound(player, QuestSound.ETCSOUND_ELROKI_SONG_FULL);
				break;
			}
			case "32116-005.htm":
			{
				st.setCond(8, true);
				break;
			}
			case "32115-004.htm":
			{
				st.setCond(9, true);
				break;
			}
			case "32115-006.htm":
			{
				st.setCond(10, true);
				break;
			}
			case "32116-007.htm":
			{
				takeItems(player, PRACTICE_ELROKIAN_TRAP, 1);
				giveItems(player, 8763, 1);
				giveItems(player, 8764, 100);
				giveAdena(player, 1022636, true);
				st.exitQuest(false, true);
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
				htmltext = (player.getLevel() < 75) ? "32113-000.htm" : "32113-001.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case MARQUEZ:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "32113-002.htm";
						}
						else if (cond == 3)
						{
							htmltext = "32113-003.htm";
						}
						else if (cond == 4)
						{
							htmltext = "32113-009.htm";
						}
						else if (cond == 5)
						{
							htmltext = "32113-010.htm";
						}
						break;
					}
					case MUSHIKA:
					{
						if (cond == 1)
						{
							st.setCond(2, true);
						}
						
						htmltext = "32114-001.htm";
						break;
					}
					case ASAMAH:
					{
						if (cond == 2)
						{
							htmltext = "32115-001.htm";
						}
						else if (cond == 3)
						{
							htmltext = "32115-002.htm";
						}
						else if (cond == 8)
						{
							htmltext = "32115-003.htm";
						}
						else if (cond == 9)
						{
							htmltext = "32115-004.htm";
						}
						else if (cond == 10)
						{
							htmltext = "32115-006.htm";
						}
						else if (cond == 11)
						{
							htmltext = "32115-007.htm";
							st.setCond(12, true);
							takeItems(player, DEINONYCHUS_BONE, -1);
							takeItems(player, ORNITHOMINUS_CLAW, -1);
							takeItems(player, PACHYCEPHALOSAURUS_SKIN, -1);
							giveItems(player, PRACTICE_ELROKIAN_TRAP, 1);
						}
						break;
					}
					case KIRIKASHIN:
					{
						if (cond < 6)
						{
							htmltext = "32116-008.htm";
						}
						else if (cond == 6)
						{
							htmltext = "32116-001.htm";
							takeItems(player, EXPEDITION_MEMBERS_LETTER, 1);
						}
						else if (cond == 7)
						{
							htmltext = "32116-004.htm";
						}
						else if (cond == 12)
						{
							htmltext = "32116-006.htm";
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
		final QuestState qs = getRandomPartyMemberState(player, -1, 3, npc);
		if (qs != null)
		{
			final ItemChanceHolder item = MOBS_DROP_CHANCES.get(npc.getId());
			if (item.getCount() == qs.getMemoState())
			{
				if (qs.isCond(4) && giveItemRandomly(qs.getPlayer(), npc, item.getId(), 1, 50, item.getChance(), true))
				{
					qs.setCond(5);
				}
				else if (qs.isCond(10) && giveItemRandomly(qs.getPlayer(), npc, item.getId(), 1, 10, item.getChance(), true) && (getQuestItemsCount(qs.getPlayer(), ORNITHOMINUS_CLAW) >= 10) && (getQuestItemsCount(qs.getPlayer(), DEINONYCHUS_BONE) >= 10) && (getQuestItemsCount(qs.getPlayer(), PACHYCEPHALOSAURUS_SKIN) >= 10))
				{
					qs.setCond(11);
				}
			}
		}
	}
}
