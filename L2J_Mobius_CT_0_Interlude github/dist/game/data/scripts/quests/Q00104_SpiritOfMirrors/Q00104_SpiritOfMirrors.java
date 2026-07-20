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
package quests.Q00104_SpiritOfMirrors;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00104_SpiritOfMirrors extends Quest
{
	// NPCs
	private static final int GALLINT = 30017;
	private static final int ARNOLD = 30041;
	private static final int JOHNSTONE = 30043;
	private static final int KENYOS = 30045;
	
	// Items
	private static final int GALLINS_OAK_WAND = 748;
	private static final int WAND_SPIRITBOUND_1 = 1135;
	private static final int WAND_SPIRITBOUND_2 = 1136;
	private static final int WAND_SPIRITBOUND_3 = 1137;
	
	// Rewards
	private static final int SPIRITSHOT_NO_GRADE = 2509;
	private static final int SOULSHOT_NO_GRADE = 1835;
	private static final int WAND_OF_ADEPT = 747;
	private static final int SPIRITSHOT_FOR_BEGINNERS = 5790;
	private static final int SOULSHOT_FOR_BEGINNERS = 5789;
	private static final int LESSER_HEALING_POT = 1060;
	private static final int ECHO_BATTLE = 4412;
	private static final int ECHO_LOVE = 4413;
	private static final int ECHO_SOLITUDE = 4414;
	private static final int ECHO_FEAST = 4415;
	private static final int ECHO_CELEBRATION = 4416;
	
	public Q00104_SpiritOfMirrors()
	{
		super(104, "Spirit of Mirrors");
		registerQuestItems(GALLINS_OAK_WAND, WAND_SPIRITBOUND_1, WAND_SPIRITBOUND_2, WAND_SPIRITBOUND_3);
		addStartNpc(GALLINT);
		addTalkId(GALLINT, ARNOLD, JOHNSTONE, KENYOS);
		
		addKillId(27003, 27004, 27005);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return htmltext;
		}
		
		if (event.equals("30017-03.htm"))
		{
			st.startQuest();
			giveItems(player, GALLINS_OAK_WAND, 1);
			giveItems(player, GALLINS_OAK_WAND, 1);
			giveItems(player, GALLINS_OAK_WAND, 1);
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
				if (player.getRace() != Race.HUMAN)
				{
					htmltext = "30017-00.htm";
				}
				else if (player.getLevel() < 10)
				{
					htmltext = "30017-01.htm";
				}
				else
				{
					htmltext = "30017-02.htm";
				}
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case GALLINT:
					{
						if ((cond == 1) || (cond == 2))
						{
							htmltext = "30017-04.htm";
						}
						else if (cond == 3)
						{
							htmltext = "30017-05.htm";
							
							takeItems(player, WAND_SPIRITBOUND_1, -1);
							takeItems(player, WAND_SPIRITBOUND_2, -1);
							takeItems(player, WAND_SPIRITBOUND_3, -1);
							
							giveItems(player, WAND_OF_ADEPT, 1);
							rewardItems(player, LESSER_HEALING_POT, 100);
							
							if (player.isMageClass())
							{
								giveItems(player, SPIRITSHOT_NO_GRADE, 500);
							}
							else
							{
								giveItems(player, SOULSHOT_NO_GRADE, 1000);
							}
							
							// Give newbie reward if player is eligible
							if (player.isNewbie())
							{
								int newPlayerRewardsReceived = player.getVariables().getInt(PlayerVariables.NEWBIE_SHOTS_RECEIVED, 0);
								if (newPlayerRewardsReceived < 2)
								{
									st.showQuestionMark(26);
									if (player.isMageClass())
									{
										st.playTutorialVoice("tutorial_voice_027");
										giveItems(player, SPIRITSHOT_FOR_BEGINNERS, 3000);
										player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
									}
									else
									{
										st.playTutorialVoice("tutorial_voice_026");
										giveItems(player, SOULSHOT_FOR_BEGINNERS, 7000);
										player.getVariables().set(PlayerVariables.NEWBIE_SHOTS_RECEIVED, ++newPlayerRewardsReceived);
									}
								}
							}
							
							giveItems(player, ECHO_BATTLE, 10);
							giveItems(player, ECHO_LOVE, 10);
							giveItems(player, ECHO_SOLITUDE, 10);
							giveItems(player, ECHO_FEAST, 10);
							giveItems(player, ECHO_CELEBRATION, 10);
							player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
							st.exitQuest(false, true);
						}
						break;
					}
					case KENYOS:
					case JOHNSTONE:
					case ARNOLD:
					{
						htmltext = npc.getId() + "-01.htm";
						if (cond == 1)
						{
							st.setCond(2, true);
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
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		if (player.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND) == GALLINS_OAK_WAND)
		{
			switch (npc.getId())
			{
				case 27003:
					if (!hasQuestItems(player, WAND_SPIRITBOUND_1))
					{
						takeItems(player, GALLINS_OAK_WAND, 1);
						giveItems(player, WAND_SPIRITBOUND_1, 1);
						if (hasQuestItems(player, WAND_SPIRITBOUND_2, WAND_SPIRITBOUND_3))
						{
							st.setCond(3, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				
				case 27004:
					if (!hasQuestItems(player, WAND_SPIRITBOUND_2))
					{
						takeItems(player, GALLINS_OAK_WAND, 1);
						giveItems(player, WAND_SPIRITBOUND_2, 1);
						if (hasQuestItems(player, WAND_SPIRITBOUND_1, WAND_SPIRITBOUND_3))
						{
							st.setCond(3, true);
						}
						else
						{
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						}
					}
					break;
				
				case 27005:
					if (!hasQuestItems(player, WAND_SPIRITBOUND_3))
					{
						takeItems(player, GALLINS_OAK_WAND, 1);
						giveItems(player, WAND_SPIRITBOUND_3, 1);
						if (hasQuestItems(player, WAND_SPIRITBOUND_1, WAND_SPIRITBOUND_2))
						{
							st.setCond(3, true);
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
