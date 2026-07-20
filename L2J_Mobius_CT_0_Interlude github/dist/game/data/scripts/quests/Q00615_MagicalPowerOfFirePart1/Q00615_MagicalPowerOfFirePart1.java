/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00615_MagicalPowerOfFirePart1;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Magical Power of Fire - Part 1 (615)
 * @author Joxit
 */
public class Q00615_MagicalPowerOfFirePart1 extends Quest
{
	// NPCs
	private static final int NARAN = 31378;
	private static final int UDAN = 31379;
	private static final int ASEFA_BOX = 31559;
	private static final int ASEFA_EYE = 31684;
	
	// Monsters
	private static final int[] KETRA_MOBS =
	{
		21324, // Ketra Orc Footman
		21325, // Ketra's War Hound
		21327, // Ketra Orc Raider
		21328, // Ketra Orc Scout
		21329, // Ketra Orc Shaman
		21331, // Ketra Orc Warrior
		21332, // Ketra Orc Lieutenant
		21334, // Ketra Orc Medium
		21335, // Ketra Orc Elite Soldier
		21336, // Ketra Orc White Captain
		21338, // Ketra Orc Seer
		21339, // Ketra Orc General
		21340, // Ketra Orc Battalion Commander
		21342, // Ketra Orc Grand Seer
		21343, // Ketra Commander
		21344, // Ketra Elite Guard
		21345, // Ketra's Head Shaman
		21346, // Ketra's Head Guard
		21347, // Ketra Prophet
		21348, // Prophet's Guard
		21349, // Prophet's Aide
	};
	
	// Items
	private static final int KEY = 1661;
	private static final int STOLEN_RED_TOTEM = 7242;
	private static final int WISDOM_STONE = 7081;
	private static final int RED_TOTEM = 7243;
	private static final int[] VARKA_MARKS =
	{
		7221, // Mark of Varka's Alliance - Level 1
		7222, // Mark of Varka's Alliance - Level 2
		7223, // Mark of Varka's Alliance - Level 3
		7224, // Mark of Varka's Alliance - Level 4
		7225, // Mark of Varka's Alliance - Level 5
	};
	
	// Skills
	private static final SkillHolder GOW = new SkillHolder(4547, 1); // Gaze of Watcher
	private static final SkillHolder DISPEL_GOW = new SkillHolder(4548, 1); // Quest - Dispel Watcher Gaze
	// Misc
	private static final int MIN_LEVEL = 74;
	
	public Q00615_MagicalPowerOfFirePart1()
	{
		super(615, "Magical Power of Fire - Part 1");
		addStartNpc(NARAN);
		addTalkId(UDAN, NARAN, ASEFA_BOX);
		addAttackId(KETRA_MOBS);
		registerQuestItems(STOLEN_RED_TOTEM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "31378-02.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "open_box":
			{
				if (!hasQuestItems(player, KEY))
				{
					htmltext = "31559-02.html";
				}
				else if (qs.isCond(2))
				{
					if (qs.isSet("spawned"))
					{
						takeItems(player, KEY, 1);
						htmltext = "31559-04.html";
					}
					else
					{
						giveItems(player, STOLEN_RED_TOTEM, 1);
						takeItems(player, KEY, 1);
						qs.setCond(3, true);
						htmltext = "31559-03.html";
					}
				}
				break;
			}
			case "eye_despawn":
			{
				npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, "Asefa has already seen your face!"));
				npc.deleteMe();
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final QuestState qs = getQuestState(attacker, false);
		if ((qs != null) && qs.isCond(2) && !qs.isSet("spawned"))
		{
			qs.set("spawned", "1");
			npc.setTarget(attacker);
			npc.doCast(GOW.getSkill());
			final Npc eye = addSpawn(ASEFA_EYE, npc);
			eye.broadcastPacket(new NpcSay(eye, ChatType.NPC_GENERAL, "You can't avoid the eyes of Asefa!"));
			startQuestTimer("eye_despawn", 10000, eye, attacker);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case NARAN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? (hasAtLeastOneQuestItem(player, VARKA_MARKS)) ? "31378-01.htm" : "31378-00a.html" : "31378-00b.html";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "31378-03.html";
						}
						break;
					}
				}
				break;
			}
			case UDAN:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "31379-01.html";
							qs.setCond(2, true);
							break;
						}
						case 2:
						{
							if (qs.isSet("spawned"))
							{
								qs.unset("spawned");
								npc.setTarget(player);
								npc.doCast(DISPEL_GOW.getSkill());
								htmltext = "31379-03.html";
							}
							else
							{
								htmltext = "31379-02.html";
							}
							break;
						}
						case 3:
						{
							giveItems(player, RED_TOTEM, 1);
							giveItems(player, WISDOM_STONE, 1);
							qs.exitQuest(true, true);
							htmltext = "31379-04.html";
							break;
						}
					}
				}
				break;
			}
			case ASEFA_BOX:
			{
				if (qs.isCond(2))
				{
					htmltext = "31559-01.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
