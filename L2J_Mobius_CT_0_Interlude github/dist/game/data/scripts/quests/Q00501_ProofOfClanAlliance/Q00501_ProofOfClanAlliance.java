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
package quests.Q00501_ProofOfClanAlliance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * @author Rootware
 */
public class Q00501_ProofOfClanAlliance extends Quest
{
	// NPC
	private static final int SIR_KRISTOF_RODEMAI = 30756;
	private static final int STATUE_OF_OFFERING = 30757;
	private static final int ATHREA = 30758;
	private static final int KALIS = 30759;
	
	// Monsters
	private static final int VANOR_SILENOS_SHAMAN = 20685;
	private static final int HARIT_LIZARDMAN_SHAMAN = 20644;
	private static final int OEL_MAHUM_WITCH_DOCTOR = 20576;
	
	// Chests
	private static final int BOX_OF_ATHREA_1 = 27173;
	private static final int BOX_OF_ATHREA_2 = 27174;
	private static final int BOX_OF_ATHREA_3 = 27175;
	private static final int BOX_OF_ATHREA_4 = 27176;
	private static final int BOX_OF_ATHREA_5 = 27177;
	
	// Items
	private static final int ADENA = 57;
	private static final int POTION_OF_RECOVERY = 3889;
	
	// Quest Items
	private static final int HERB_OF_HARIT = 3832;
	private static final int HERB_OF_VANOR = 3833;
	private static final int HERB_OF_OEL_MAHUM = 3834;
	private static final int BLOOD_OF_EVA = 3835;
	private static final int ATHREAS_COIN = 3836;
	private static final int SYMBOL_OF_LOYALTY = 3837;
	private static final int VOUCHER_OF_FAITH = 3873;
	private static final int ANTIDOTE_RECIPE_LIST = 3872;
	
	// Reward
	private static final int PROOF_OF_ALLIANCE = 3874;
	
	// Drops
	private static final Map<Integer, Integer> DROP = new HashMap<>();
	static
	{
		DROP.put(VANOR_SILENOS_SHAMAN, HERB_OF_VANOR);
		DROP.put(HARIT_LIZARDMAN_SHAMAN, HERB_OF_HARIT);
		DROP.put(OEL_MAHUM_WITCH_DOCTOR, HERB_OF_OEL_MAHUM);
	}
	
	// Chests spawns
	// @formatter:off
	private static final int[][] CHESTS_SPAWN =
	{
		{102273, 103433, -3512},
		{102190, 103379, -3524},
		{102107, 103325, -3533},
		{102024, 103271, -3500},
		{102327, 103350, -3511},
		{102244, 103296, -3518},
		{102161, 103242, -3529},
		{102078, 103188, -3500},
		{102381, 103267, -3538},
		{102298, 103213, -3532},
		{102215, 103159, -3520},
		{102132, 103105, -3513},
		{102435, 103184, -3515},
		{102352, 103130, -3522},
		{102269, 103076, -3533},
		{102186, 103022, -3541}
	};
	// @formatter:on
	
	// Chests
	private static final List<Integer> CHEST_IDS = new ArrayList<>();
	static
	{
		CHEST_IDS.add(BOX_OF_ATHREA_1);
		CHEST_IDS.add(BOX_OF_ATHREA_2);
		CHEST_IDS.add(BOX_OF_ATHREA_3);
		CHEST_IDS.add(BOX_OF_ATHREA_4);
		CHEST_IDS.add(BOX_OF_ATHREA_5);
	}
	
	// Trigger
	private static boolean _isSpawned = false;
	
	public Q00501_ProofOfClanAlliance()
	{
		super(501, "Proof of Clan Alliance");
		registerQuestItems(HERB_OF_HARIT, HERB_OF_VANOR, HERB_OF_OEL_MAHUM, BLOOD_OF_EVA, ATHREAS_COIN, SYMBOL_OF_LOYALTY, VOUCHER_OF_FAITH, ANTIDOTE_RECIPE_LIST);
		addStartNpc(SIR_KRISTOF_RODEMAI, STATUE_OF_OFFERING);
		addTalkId(SIR_KRISTOF_RODEMAI, KALIS, STATUE_OF_OFFERING, ATHREA);
		addKillId(DROP.keySet());
		addKillId(CHEST_IDS);
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
			case "30756-07.htm":
			{
				st.startQuest();
				st.set("state", "1");
				break;
			}
			case "30759-03.htm":
			{
				st.setCond(2, true);
				st.set("state", "2");
				break;
			}
			case "30759-07.htm":
			{
				st.setCond(3, true);
				st.set("state", "3");
				takeItems(player, SYMBOL_OF_LOYALTY, 1);
				takeItems(player, SYMBOL_OF_LOYALTY, 1);
				takeItems(player, SYMBOL_OF_LOYALTY, 1);
				giveItems(player, ANTIDOTE_RECIPE_LIST, 1);
				SkillData.getInstance().getSkill(4082, 1).applyEffects(npc, player);
				startQuestTimer("poison", 60000, npc, player, true);
				break;
			}
			case "30757-03.htm":
			{
				if (getRandom(10) > 5)
				{
					final QuestState st2 = getClanLeaderQuestState(player, npc);
					st.setState(State.STARTED);
					st.set("symbol", "1");
					st2.set("symbols", String.valueOf(st2.getInt("symbols") + 1));
					giveItems(player, SYMBOL_OF_LOYALTY, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
					htmltext = "30757-04.htm";
				}
				else
				{
					castSkill(npc, player, 4083);
					startQuestTimer("die", 4000, npc, player, false);
				}
				break;
			}
			case "30758-03.htm":
			{
				if (!_isSpawned && (player.getAdena() >= 10000))
				{
					final QuestState st2 = getClanLeaderQuestState(player, npc);
					st2.set("state", "4");
					st2.set("bingo", "0");
					st2.set("chests", "0");
					takeItems(player, ADENA, 10000);
					for (int[] coords : CHESTS_SPAWN)
					{
						addSpawn(CHEST_IDS.get(getRandom(CHEST_IDS.size())), coords[0], coords[1], coords[2], 0, false, 0);
					}
					
					_isSpawned = true;
					startQuestTimer("despawn", 300000, null, player, false);
				}
				else
				{
					htmltext = "30758-03a.htm";
				}
				break;
			}
			case "30758-07.htm":
			{
				if (player.getAdena() >= 10000)
				{
					if (!_isSpawned)
					{
						takeItems(player, ADENA, 10000);
					}
				}
				else
				{
					htmltext = "30758-06.htm";
				}
				break;
			}
			case "die":
			{
				final QuestState st2 = getClanLeaderQuestState(player, npc);
				st.setState(State.STARTED);
				st.set("symbol", "1");
				st2.set("symbols", String.valueOf(st2.getInt("symbols") + 1));
				giveItems(player, SYMBOL_OF_LOYALTY, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ACCEPT);
				return null;
			}
			case "poison":
			{
				if (!hasAbnormal(player))
				{
					player.sendMessage("Are you noob?");
					cancelQuestTimer("poison", npc, player); // Cancel check timer
				}
				
				return null;
			}
			case "despawn":
			{
				_isSpawned = false;
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
				switch (npc.getId())
				{
					case SIR_KRISTOF_RODEMAI:
					{
						if (player.isClanLeader())
						{
							if (player.getClan().getLevel() == 3)
							{
								if (hasQuestItems(player, PROOF_OF_ALLIANCE))
								{
									htmltext = "30756-03.htm";
								}
								else
								{
									htmltext = "30756-04.htm";
								}
							}
							else if (player.getClan().getLevel() < 3)
							{
								htmltext = "30756-01.htm";
							}
							else
							{
								htmltext = "30756-02.htm";
							}
						}
						else
						{
							htmltext = "30756-05.htm";
						}
						break;
					}
					case STATUE_OF_OFFERING:
					{
						final QuestState cl = getClanLeaderQuestState(player, npc);
						if ((cl != null) && (cl.getInt("state") == 2) && (cl.getInt("symbols") < 3))
						{
							if (!player.isClanLeader())
							{
								if (player.getLevel() > 39)
								{
									htmltext = "30757-01.htm";
								}
								else
								{
									htmltext = "30757-02.htm";
								}
							}
							else
							{
								htmltext = "30757-01a.htm";
							}
						}
						else if (player.getClan() != null)
						{
							htmltext = "30757-06.htm";
						}
						break;
					}
				}
				break;
			}
			case State.STARTED:
			{
				final int state = st.getInt("state");
				switch (npc.getId())
				{
					case SIR_KRISTOF_RODEMAI:
					{
						if ((state == 6) && hasQuestItems(player, VOUCHER_OF_FAITH))
						{
							htmltext = "30756-09.htm";
							addExpAndSp(player, 0, 120000);
							takeItems(player, VOUCHER_OF_FAITH, -1);
							giveItems(player, PROOF_OF_ALLIANCE, 1);
							
							// htmltext = getAlreadyCompletedMsg();
							st.exitQuest(true, true);
						}
						else if (state > 0)
						{
							htmltext = "30756-10.htm";
						}
						break;
					}
					case STATUE_OF_OFFERING:
					{
						final QuestState cl = getClanLeaderQuestState(player, npc);
						if ((cl != null) && (cl.getInt("state") == 2) && (st.getInt("symbol") == 1))
						{
							htmltext = "30757-01b.htm";
						}
						break;
					}
					case KALIS:
					{
						if (player.isClanLeader())
						{
							if ((state == 1) && !hasQuestItems(player, SYMBOL_OF_LOYALTY))
							{
								htmltext = "30759-01.htm";
							}
							else if (state == 2)
							{
								if (getQuestItemsCount(player, SYMBOL_OF_LOYALTY) < 3)
								{
									htmltext = "30759-05.htm";
								}
								else
								{
									htmltext = "30759-06.htm";
								}
							}
							else if ((state > 2) && (state < 6) && !hasAbnormal(player))
							{
								st.setCond(1);
								st.set("state", "1");
								takeItems(player, ANTIDOTE_RECIPE_LIST, -1);
								htmltext = "30759-09.htm";
							}
							else if ((state > 2) && (state < 6) && hasAbnormal(player) && !hasAtLeastOneQuestItem(player, HERB_OF_HARIT, HERB_OF_VANOR, HERB_OF_OEL_MAHUM, BLOOD_OF_EVA))
							{
								htmltext = "30759-10.htm";
							}
							else if ((state == 5) && hasAbnormal(player) && hasAtLeastOneQuestItem(player, HERB_OF_HARIT, HERB_OF_VANOR, HERB_OF_OEL_MAHUM, BLOOD_OF_EVA))
							{
								st.setCond(4, true);
								st.set("state", "6");
								takeItems(player, ANTIDOTE_RECIPE_LIST, -1);
								takeItems(player, HERB_OF_HARIT, -1);
								takeItems(player, HERB_OF_VANOR, -1);
								takeItems(player, HERB_OF_OEL_MAHUM, -1);
								takeItems(player, BLOOD_OF_EVA, -1);
								giveItems(player, VOUCHER_OF_FAITH, 1);
								giveItems(player, POTION_OF_RECOVERY, 1);
								cancelQuestTimer("poison", npc, player); // Cancel check timer
								htmltext = "30759-08.htm";
							}
							else if (state == 6)
							{
								htmltext = "30759-11.htm";
							}
						}
						else
						{
							htmltext = "30759-12.htm";
						}
						break;
					}
					case ATHREA:
					{
						final QuestState cl = getClanLeaderQuestState(player, npc);
						if ((cl != null) && (cl.getInt("state") == 3) && hasQuestItems(cl.getPlayer(), ANTIDOTE_RECIPE_LIST) && !hasQuestItems(cl.getPlayer(), BLOOD_OF_EVA) && hasFirstHerb(player, cl.get("herbs")) && (getHerbs(cl.get("herbs")).size() == 3))
						{
							htmltext = "30758-01.htm";
						}
						else if ((cl != null) && (cl.getInt("state") == 4))
						{
							if (cl.getInt("bingo") == 4)
							{
								cl.set("state", "5");
								giveItems(player, BLOOD_OF_EVA, 1);
								htmltext = "30758-08.htm";
								cl.unset("chests");
								cl.unset("bingo");
							}
							else
							{
								htmltext = "30758-05.htm";
							}
						}
						else if ((cl != null) && (cl.getInt("state") == 5))
						{
							htmltext = "30758-09.htm";
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
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = player.getQuestState(getName());
		final QuestState cl = getClanLeaderQuestState(player, npc);
		if ((st == null) || (cl == null))
		{
			return;
		}
		
		final int npcId = npc.getId();
		if (DROP.containsKey(npcId) && (cl.getInt("state") == 3))
		{
			final int itemId = DROP.get(npcId);
			final List<Integer> herbs = getHerbs(cl.get("herbs"));
			if ((getRandom(10) == 1) && !hasQuestItems(player, itemId) && !hasOtherItems(player, itemId) && !herbs.contains(itemId))
			{
				if (herbs.isEmpty())
				{
					cl.set("herbs", String.valueOf(itemId));
				}
				else
				{
					cl.set("herbs", cl.get("herbs") + ";" + itemId);
				}
				
				giveItems(player, itemId, 1);
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
		else if (CHEST_IDS.contains(npcId) && (cl.getInt("state") == 4))
		{
			final int chests = cl.getInt("chests");
			final int bingo = cl.getInt("bingo");
			if ((((chests == 15) && (bingo == 3)) || ((chests == 14) && (bingo == 2)) || ((chests == 13) && (bingo == 1)) || ((chests == 12) && (bingo == 0))) || ((bingo < 4) && (getRandom(4) == 0)))
			{
				npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, "##########Bingo!##########"));
				cl.set("bingo", String.valueOf(bingo + 1));
			}
			
			cl.set("chests", String.valueOf(chests + 1));
			if (chests == 16)
			{
				_isSpawned = false;
			}
		}
	}
	
	public boolean hasOtherItems(Player player, int itemId)
	{
		switch (itemId)
		{
			case HERB_OF_VANOR:
			{
				if (hasQuestItems(player, HERB_OF_HARIT) || hasQuestItems(player, HERB_OF_OEL_MAHUM))
				{
					return true;
				}
				break;
			}
			case HERB_OF_HARIT:
			{
				if (hasQuestItems(player, HERB_OF_VANOR) || hasQuestItems(player, HERB_OF_OEL_MAHUM))
				{
					return true;
				}
				break;
			}
			case HERB_OF_OEL_MAHUM:
			{
				if (hasQuestItems(player, HERB_OF_HARIT) || hasQuestItems(player, HERB_OF_VANOR))
				{
					return true;
				}
				break;
			}
		}
		
		return false;
	}
	
	/**
	 * Verifies if the player has the poison.
	 * @param player the player to check
	 * @return {@code true} if the player has {@link AbnormalType#FATAL_POISON} abnormal
	 */
	private static boolean hasAbnormal(Player player)
	{
		return player.getEffectList().getBuffInfoByAbnormalType(AbnormalType.FATAL_POISON) != null;
	}
	
	public void castSkill(Npc npc, Player player, int skillId)
	{
		final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
		npc.setTarget(player);
		npc.doCast(skill);
	}
	
	private static List<Integer> getHerbs(String list)
	{
		final List<Integer> array = new ArrayList<>();
		if (list != null)
		{
			final String[] herbs = list.split(";");
			for (String herb : herbs)
			{
				array.add(Integer.parseInt(herb));
			}
		}
		
		return array;
	}
	
	public boolean hasFirstHerb(Player player, String list)
	{
		if (list != null)
		{
			final String[] herbs = list.split(";");
			if (hasQuestItems(player, Integer.parseInt(herbs[0])))
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public QuestState getClanLeaderQuestState(Player player, Npc npc)
	{
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return null;
		}
		
		final Player leader = clan.getLeader().getPlayer();
		if (leader == null)
		{
			return null;
		}
		
		return leader.getQuestState(getName());
	}
}
