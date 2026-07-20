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
package quests.Q00231_TestOfTheMaestro;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00231_TestOfTheMaestro extends Quest
{
	// NPCs
	private static final int LOCKIRIN = 30531;
	private static final int SPIRON = 30532;
	private static final int BALANKI = 30533;
	private static final int KEEF = 30534;
	private static final int FILAUR = 30535;
	private static final int ARIN = 30536;
	private static final int TOMA = 30556;
	private static final int CROTO = 30671;
	private static final int DUBABAH = 30672;
	private static final int LORAIN = 30673;
	
	// Monsters
	private static final int KING_BUGBEAR = 20150;
	private static final int GIANT_MIST_LEECH = 20225;
	private static final int STINGER_WASP = 20229;
	private static final int MARSH_SPIDER = 20233;
	private static final int EVIL_EYE_LORD = 27133;
	
	// Items
	private static final int RECOMMENDATION_OF_BALANKI = 2864;
	private static final int RECOMMENDATION_OF_FILAUR = 2865;
	private static final int RECOMMENDATION_OF_ARIN = 2866;
	private static final int LETTER_OF_SOLDER_DETACHMENT = 2868;
	private static final int PAINT_OF_KAMURU = 2869;
	private static final int NECKLACE_OF_KAMURU = 2870;
	private static final int PAINT_OF_TELEPORT_DEVICE = 2871;
	private static final int TELEPORT_DEVICE = 2872;
	private static final int ARCHITECTURE_OF_KRUMA = 2873;
	private static final int REPORT_OF_KRUMA = 2874;
	private static final int INGREDIENTS_OF_ANTIDOTE = 2875;
	private static final int STINGER_WASP_NEEDLE = 2876;
	private static final int MARSH_SPIDER_WEB = 2877;
	private static final int BLOOD_OF_LEECH = 2878;
	private static final int BROKEN_TELEPORT_DEVICE = 2916;
	
	// Rewards
	private static final int MARK_OF_MAESTRO = 2867;
	private static final int DIMENSIONAL_DIAMOND = 7562;
	
	public Q00231_TestOfTheMaestro()
	{
		super(231, "Test of the Maestro");
		registerQuestItems(RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_FILAUR, RECOMMENDATION_OF_ARIN, LETTER_OF_SOLDER_DETACHMENT, PAINT_OF_KAMURU, NECKLACE_OF_KAMURU, PAINT_OF_TELEPORT_DEVICE, TELEPORT_DEVICE, ARCHITECTURE_OF_KRUMA, REPORT_OF_KRUMA, INGREDIENTS_OF_ANTIDOTE, STINGER_WASP_NEEDLE, MARSH_SPIDER_WEB, BLOOD_OF_LEECH, BROKEN_TELEPORT_DEVICE);
		addStartNpc(LOCKIRIN);
		addTalkId(LOCKIRIN, SPIRON, BALANKI, KEEF, FILAUR, ARIN, TOMA, CROTO, DUBABAH, LORAIN);
		addKillId(GIANT_MIST_LEECH, STINGER_WASP, MARSH_SPIDER, EVIL_EYE_LORD);
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
			case "30531-04.htm":
			{
				st.startQuest();
				if (!player.getVariables().getBoolean("secondClassChange39", false))
				{
					htmltext = "30531-04a.htm";
					giveItems(player, DIMENSIONAL_DIAMOND, DF_REWARD_39.get(player.getPlayerClass().getId()));
					player.getVariables().set("secondClassChange39", true);
				}
				break;
			}
			case "30533-02.htm":
			{
				st.set("bCond", "1");
				break;
			}
			case "30671-02.htm":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				giveItems(player, PAINT_OF_KAMURU, 1);
				break;
			}
			case "TELEPORT":
			{
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, PAINT_OF_TELEPORT_DEVICE, 1);
				giveItems(player, BROKEN_TELEPORT_DEVICE, 1);
				player.teleToLocation(140352, -194133, -3146);
				startQuestTimer("spawn_bugbears", 5000, null, player, false);
				break;
			}
			case "30673-04.htm":
			{
				st.set("fCond", "2");
				playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				takeItems(player, BLOOD_OF_LEECH, -1);
				takeItems(player, INGREDIENTS_OF_ANTIDOTE, 1);
				takeItems(player, MARSH_SPIDER_WEB, -1);
				takeItems(player, STINGER_WASP_NEEDLE, -1);
				giveItems(player, REPORT_OF_KRUMA, 1);
				break;
			}
			case "spawn_bugbears":
			{
				final Attackable bugbear1 = addSpawn(KING_BUGBEAR, 140333, -194153, -3138, 0, false, 200000).asAttackable();
				bugbear1.addDamageHate(player, 0, 999);
				bugbear1.getAI().setIntention(Intention.ATTACK, player);
				final Attackable bugbear2 = addSpawn(KING_BUGBEAR, 140395, -194147, -3146, 0, false, 200000).asAttackable();
				bugbear2.addDamageHate(player, 0, 999);
				bugbear2.getAI().setIntention(Intention.ATTACK, player);
				final Attackable bugbear3 = addSpawn(KING_BUGBEAR, 140304, -194082, -3157, 0, false, 200000).asAttackable();
				bugbear3.addDamageHate(player, 0, 999);
				bugbear3.getAI().setIntention(Intention.ATTACK, player);
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
				if (player.getPlayerClass() != PlayerClass.ARTISAN)
				{
					htmltext = "30531-01.htm";
				}
				else if (player.getLevel() < 39)
				{
					htmltext = "30531-02.htm";
				}
				else
				{
					htmltext = "30531-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case LOCKIRIN:
					{
						final int cond = st.getCond();
						if (cond == 1)
						{
							htmltext = "30531-05.htm";
						}
						else if (cond == 2)
						{
							htmltext = "30531-06.htm";
							takeItems(player, RECOMMENDATION_OF_ARIN, 1);
							takeItems(player, RECOMMENDATION_OF_BALANKI, 1);
							takeItems(player, RECOMMENDATION_OF_FILAUR, 1);
							giveItems(player, MARK_OF_MAESTRO, 1);
							addExpAndSp(player, 46000, 5900);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case SPIRON:
					{
						htmltext = "30532-01.htm";
						break;
					}
					case KEEF:
					{
						htmltext = "30534-01.htm";
						break;
					}
					case BALANKI:
					{
						final int bCond = st.getInt("bCond");
						if (bCond == 0)
						{
							htmltext = "30533-01.htm";
						}
						else if (bCond == 1)
						{
							htmltext = "30533-03.htm";
						}
						else if (bCond == 2)
						{
							htmltext = "30533-04.htm";
							st.set("bCond", "3");
							takeItems(player, LETTER_OF_SOLDER_DETACHMENT, 1);
							giveItems(player, RECOMMENDATION_OF_BALANKI, 1);
							
							if (hasQuestItems(player, RECOMMENDATION_OF_ARIN, RECOMMENDATION_OF_FILAUR))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if (bCond == 3)
						{
							htmltext = "30533-05.htm";
						}
						break;
					}
					case CROTO:
					{
						final int bCond = st.getInt("bCond");
						if (bCond == 1)
						{
							if (!hasQuestItems(player, PAINT_OF_KAMURU))
							{
								htmltext = "30671-01.htm";
							}
							else if (!hasQuestItems(player, NECKLACE_OF_KAMURU))
							{
								htmltext = "30671-03.htm";
							}
							else
							{
								htmltext = "30671-04.htm";
								st.set("bCond", "2");
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								takeItems(player, NECKLACE_OF_KAMURU, 1);
								takeItems(player, PAINT_OF_KAMURU, 1);
								giveItems(player, LETTER_OF_SOLDER_DETACHMENT, 1);
							}
						}
						else if (bCond > 1)
						{
							htmltext = "30671-05.htm";
						}
						break;
					}
					case DUBABAH:
					{
						htmltext = "30672-01.htm";
						break;
					}
					case ARIN:
					{
						final int aCond = st.getInt("aCond");
						if (aCond == 0)
						{
							htmltext = "30536-01.htm";
							st.set("aCond", "1");
							giveItems(player, PAINT_OF_TELEPORT_DEVICE, 1);
						}
						else if (aCond == 1)
						{
							htmltext = "30536-02.htm";
						}
						else if (aCond == 2)
						{
							htmltext = "30536-03.htm";
							st.set("aCond", "3");
							takeItems(player, TELEPORT_DEVICE, -1);
							giveItems(player, RECOMMENDATION_OF_ARIN, 1);
							
							if (hasQuestItems(player, RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_FILAUR))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if (aCond == 3)
						{
							htmltext = "30536-04.htm";
						}
						break;
					}
					case TOMA:
					{
						final int aCond = st.getInt("aCond");
						if (aCond == 1)
						{
							if (!hasQuestItems(player, BROKEN_TELEPORT_DEVICE))
							{
								htmltext = "30556-01.htm";
							}
							else if (!hasQuestItems(player, TELEPORT_DEVICE))
							{
								htmltext = "30556-06.htm";
								st.set("aCond", "2");
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
								takeItems(player, BROKEN_TELEPORT_DEVICE, 1);
								giveItems(player, TELEPORT_DEVICE, 5);
							}
						}
						else if (aCond > 1)
						{
							htmltext = "30556-07.htm";
						}
						break;
					}
					case FILAUR:
					{
						final int fCond = st.getInt("fCond");
						if (fCond == 0)
						{
							htmltext = "30535-01.htm";
							st.set("fCond", "1");
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							giveItems(player, ARCHITECTURE_OF_KRUMA, 1);
						}
						else if (fCond == 1)
						{
							htmltext = "30535-02.htm";
						}
						else if (fCond == 2)
						{
							htmltext = "30535-03.htm";
							st.set("fCond", "3");
							takeItems(player, REPORT_OF_KRUMA, 1);
							giveItems(player, RECOMMENDATION_OF_FILAUR, 1);
							
							if (hasQuestItems(player, RECOMMENDATION_OF_BALANKI, RECOMMENDATION_OF_ARIN))
							{
								st.setCond(2, true);
							}
							else
							{
								playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							}
						}
						else if (fCond == 3)
						{
							htmltext = "30535-04.htm";
						}
						break;
					}
					case LORAIN:
					{
						final int fCond = st.getInt("fCond");
						if (fCond == 1)
						{
							if (!hasQuestItems(player, REPORT_OF_KRUMA))
							{
								if (!hasQuestItems(player, INGREDIENTS_OF_ANTIDOTE))
								{
									htmltext = "30673-01.htm";
									playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
									takeItems(player, ARCHITECTURE_OF_KRUMA, 1);
									giveItems(player, INGREDIENTS_OF_ANTIDOTE, 1);
								}
								else if ((getQuestItemsCount(player, STINGER_WASP_NEEDLE) < 10) || (getQuestItemsCount(player, MARSH_SPIDER_WEB) < 10) || (getQuestItemsCount(player, BLOOD_OF_LEECH) < 10))
								{
									htmltext = "30673-02.htm";
								}
								else
								{
									htmltext = "30673-03.htm";
								}
							}
						}
						else if (fCond > 1)
						{
							htmltext = "30673-05.htm";
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
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		final QuestState st = getQuestState(player, false);
		if ((st == null) || !st.isCond(1))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case GIANT_MIST_LEECH:
			{
				if (hasQuestItems(player, INGREDIENTS_OF_ANTIDOTE) && (getQuestItemsCount(player, BLOOD_OF_LEECH) < 10))
				{
					giveItems(player, BLOOD_OF_LEECH, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case STINGER_WASP:
			{
				if (hasQuestItems(player, INGREDIENTS_OF_ANTIDOTE) && (getQuestItemsCount(player, STINGER_WASP_NEEDLE) < 10))
				{
					giveItems(player, STINGER_WASP_NEEDLE, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case MARSH_SPIDER:
			{
				if (hasQuestItems(player, INGREDIENTS_OF_ANTIDOTE) && (getQuestItemsCount(player, MARSH_SPIDER_WEB) < 10))
				{
					giveItems(player, MARSH_SPIDER_WEB, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
			case EVIL_EYE_LORD:
			{
				if (hasQuestItems(player, PAINT_OF_KAMURU) && !hasQuestItems(player, NECKLACE_OF_KAMURU))
				{
					giveItems(player, NECKLACE_OF_KAMURU, 1);
					playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
				}
				break;
			}
		}
	}
}
