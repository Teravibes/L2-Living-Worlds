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
package quests.Q00234_FatesWhisper;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestSound;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class Q00234_FatesWhisper extends Quest
{
	// Items
	private static final int REIRIA_SOUL_ORB = 4666;
	private static final int KERMON_INFERNIUM_SCEPTER = 4667;
	private static final int GOLKONDA_INFERNIUM_SCEPTER = 4668;
	private static final int HALLATE_INFERNIUM_SCEPTER = 4669;
	private static final int INFERNIUM_VARNISH = 4672;
	private static final int REORIN_HAMMER = 4670;
	private static final int REORIN_MOLD = 4671;
	private static final int PIPETTE_KNIFE = 4665;
	private static final int RED_PIPETTE_KNIFE = 4673;
	private static final int CRYSTAL_B = 1460;
	
	// Reward
	private static final int STAR_OF_DESTINY = 5011;
	
	// Chest Spawn
	private static final Map<Integer, Integer> CHEST_SPAWN = new HashMap<>();
	static
	{
		CHEST_SPAWN.put(25035, 31027);
		CHEST_SPAWN.put(25054, 31028);
		CHEST_SPAWN.put(25126, 31029);
		CHEST_SPAWN.put(25220, 31030);
	}
	
	// Weapons
	private static final Map<Integer, String> WEAPONS = new HashMap<>();
	static
	{
		WEAPONS.put(79, "Sword of Damascus");
		WEAPONS.put(97, "Lance");
		WEAPONS.put(171, "Deadman's Glory");
		WEAPONS.put(175, "Art of Battle Axe");
		WEAPONS.put(210, "Staff of Evil Spirits");
		WEAPONS.put(234, "Demon Dagger");
		WEAPONS.put(268, "Bellion Cestus");
		WEAPONS.put(287, "Bow of Peril");
		WEAPONS.put(2626, "Samurai Dual-sword");
		WEAPONS.put(7883, "Guardian Sword");
		WEAPONS.put(7889, "Wizard's Tear");
		WEAPONS.put(7893, "Kaim Vanul's Bones");
		WEAPONS.put(7901, "Star Buster");
	}
	
	public Q00234_FatesWhisper()
	{
		super(234, "Fate's Whisper");
		registerQuestItems(PIPETTE_KNIFE, RED_PIPETTE_KNIFE);
		addStartNpc(31002);
		addTalkId(31002, 30182, 30847, 30178, 30833, 31028, 31029, 31030, 31027);
		
		// The 4 bosses which spawn chests
		addKillId(25035, 25054, 25126, 25220);
		
		// Baium
		addAttackId(29020);
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
		
		if (event.equals("31002-03.htm"))
		{
			st.startQuest();
		}
		else if (event.equals("30182-01c.htm"))
		{
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			giveItems(player, INFERNIUM_VARNISH, 1);
		}
		else if (event.equals("30178-01a.htm"))
		{
			st.setCond(6, true);
		}
		else if (event.equals("30833-01b.htm"))
		{
			st.setCond(7, true);
			giveItems(player, PIPETTE_KNIFE, 1);
		}
		else if (event.startsWith("selectBGrade_"))
		{
			if (st.getInt("bypass") == 1)
			{
				return null;
			}
			
			final String bGradeId = event.replace("selectBGrade_", "");
			st.set("weaponId", bGradeId);
			htmltext = getHtm(player, "31002-13.htm").replace("%weaponname%", WEAPONS.get(st.getInt("weaponId")));
		}
		else if (event.startsWith("confirmWeapon"))
		{
			st.set("bypass", "1");
			htmltext = getHtm(player, "31002-14.htm").replace("%weaponname%", WEAPONS.get(st.getInt("weaponId")));
		}
		else if (event.startsWith("selectAGrade_"))
		{
			if (st.getInt("bypass") == 1)
			{
				final int itemId = st.getInt("weaponId");
				if (hasQuestItems(player, itemId))
				{
					final int aGradeItemId = Integer.parseInt(event.replace("selectAGrade_", ""));
					htmltext = getHtm(player, "31002-12.htm").replace("%weaponname%", ItemData.getInstance().getTemplate(aGradeItemId).getName());
					takeItems(player, itemId, 1);
					giveItems(player, aGradeItemId, 1);
					giveItems(player, STAR_OF_DESTINY, 1);
					player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
					st.exitQuest(false, true);
				}
				else
				{
					htmltext = getHtm(player, "31002-15.htm").replace("%weaponname%", WEAPONS.get(itemId));
				}
			}
			else
			{
				htmltext = "31002-16.htm";
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
				htmltext = (player.getLevel() < 75) ? "31002-01.htm" : "31002-02.htm";
				break;
			}
			case State.STARTED:
			{
				final int cond = st.getCond();
				switch (npc.getId())
				{
					case 31002:
					{
						if (cond == 1)
						{
							if (!hasQuestItems(player, REIRIA_SOUL_ORB))
							{
								htmltext = "31002-04b.htm";
							}
							else
							{
								htmltext = "31002-05.htm";
								st.setCond(2, true);
								takeItems(player, REIRIA_SOUL_ORB, 1);
							}
						}
						else if (cond == 2)
						{
							if (!hasQuestItems(player, KERMON_INFERNIUM_SCEPTER) || !hasQuestItems(player, GOLKONDA_INFERNIUM_SCEPTER) || !hasQuestItems(player, HALLATE_INFERNIUM_SCEPTER))
							{
								htmltext = "31002-05c.htm";
							}
							else
							{
								htmltext = "31002-06.htm";
								st.setCond(3, true);
								takeItems(player, GOLKONDA_INFERNIUM_SCEPTER, 1);
								takeItems(player, HALLATE_INFERNIUM_SCEPTER, 1);
								takeItems(player, KERMON_INFERNIUM_SCEPTER, 1);
							}
						}
						else if (cond == 3)
						{
							if (!hasQuestItems(player, INFERNIUM_VARNISH))
							{
								htmltext = "31002-06b.htm";
							}
							else
							{
								htmltext = "31002-07.htm";
								st.setCond(4, true);
								takeItems(player, INFERNIUM_VARNISH, 1);
							}
						}
						else if (cond == 4)
						{
							if (!hasQuestItems(player, REORIN_HAMMER))
							{
								htmltext = "31002-07b.htm";
							}
							else
							{
								htmltext = "31002-08.htm";
								st.setCond(5, true);
								takeItems(player, REORIN_HAMMER, 1);
							}
						}
						else if ((cond > 4) && (cond < 8))
						{
							htmltext = "31002-08b.htm";
						}
						else if (cond == 8)
						{
							htmltext = "31002-09.htm";
							st.setCond(9, true);
							takeItems(player, REORIN_MOLD, 1);
						}
						else if (cond == 9)
						{
							if (getQuestItemsCount(player, CRYSTAL_B) < 984)
							{
								htmltext = "31002-09b.htm";
							}
							else
							{
								htmltext = "31002-BGradeList.htm";
								st.setCond(10, true);
								takeItems(player, CRYSTAL_B, 984);
							}
						}
						else if (cond == 10)
						{
							// If a weapon is selected
							if (st.getInt("bypass") == 1)
							{
								// If you got it in the inventory
								final int itemId = st.getInt("weaponId");
								htmltext = getHtm(player, (hasQuestItems(player, itemId)) ? "31002-AGradeList.htm" : "31002-15.htm").replace("%weaponname%", WEAPONS.get(itemId));
							}
							// B weapon is still not selected
							else
							{
								htmltext = "31002-BGradeList.htm";
							}
						}
						break;
					}
					case 30182:
					{
						if (cond == 3)
						{
							htmltext = !hasQuestItems(player, INFERNIUM_VARNISH) ? "30182-01.htm" : "30182-02.htm";
						}
						break;
					}
					case 30847:
					{
						if ((cond == 4) && !hasQuestItems(player, REORIN_HAMMER))
						{
							htmltext = "30847-01.htm";
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							giveItems(player, REORIN_HAMMER, 1);
						}
						else if ((cond >= 4) && hasQuestItems(player, REORIN_HAMMER))
						{
							htmltext = "30847-02.htm";
						}
						break;
					}
					case 30178:
					{
						if (cond == 5)
						{
							htmltext = "30178-01.htm";
						}
						else if (cond > 5)
						{
							htmltext = "30178-02.htm";
						}
						break;
					}
					case 30833:
					{
						if (cond == 6)
						{
							htmltext = "30833-01.htm";
						}
						else if (cond == 7)
						{
							if (hasQuestItems(player, PIPETTE_KNIFE) && !hasQuestItems(player, RED_PIPETTE_KNIFE))
							{
								htmltext = "30833-02.htm";
							}
							else
							{
								htmltext = "30833-03.htm";
								st.setCond(8, true);
								takeItems(player, RED_PIPETTE_KNIFE, 1);
								giveItems(player, REORIN_MOLD, 1);
							}
						}
						else if (cond > 7)
						{
							htmltext = "30833-04.htm";
						}
						break;
					}
					case 31027:
					{
						if ((cond == 1) && !hasQuestItems(player, REIRIA_SOUL_ORB))
						{
							htmltext = "31027-01.htm";
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							giveItems(player, REIRIA_SOUL_ORB, 1);
						}
						else
						{
							htmltext = "31027-02.htm";
						}
						break;
					}
					case 31028:
					case 31029:
					case 31030:
					{
						final int itemId = npc.getId() - 26361;
						if ((cond == 2) && !hasQuestItems(player, itemId))
						{
							htmltext = npc.getId() + "-01.htm";
							playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
							giveItems(player, itemId, 1);
						}
						else
						{
							htmltext = npc.getId() + "-02.htm";
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
		final QuestState st = attacker.getQuestState(getName());
		if ((st == null) || !st.isCond(7))
		{
			return;
		}
		
		if ((attacker.getActiveWeaponItem() != null) && (attacker.getActiveWeaponItem().getId() == PIPETTE_KNIFE) && !hasQuestItems(attacker, RED_PIPETTE_KNIFE))
		{
			playSound(attacker, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			takeItems(attacker, PIPETTE_KNIFE, 1);
			giveItems(attacker, RED_PIPETTE_KNIFE, 1);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		addSpawn(CHEST_SPAWN.get(npc.getId()), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 120000);
	}
}
