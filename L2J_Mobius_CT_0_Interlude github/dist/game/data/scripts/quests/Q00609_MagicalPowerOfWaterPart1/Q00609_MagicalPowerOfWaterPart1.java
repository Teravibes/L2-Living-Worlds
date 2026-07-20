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
package quests.Q00609_MagicalPowerOfWaterPart1;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.State;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Magical Power of Water - Part 1 (609)
 * @author Joxit
 */
public class Q00609_MagicalPowerOfWaterPart1 extends Quest
{
	// NPCs
	private static final int WAHKAN = 31371;
	private static final int ASEFA = 31372;
	private static final int UDANS_BOX = 31561;
	private static final int UDANS_EYE = 31684;
	
	// Monsters
	private static final int[] VARKA_MOBS =
	{
		21350, // Varka Silenos Recruit
		21351, // Varka Silenos Footman
		21353, // Varka Silenos Scout
		21354, // Varka Silenos Hunter
		21355, // Varka Silenos Shaman
		21357, // Varka Silenos Priest
		21358, // Varka Silenos Warrior
		21360, // Varka Silenos Medium
		21361, // Varka Silenos Magus
		21362, // Varka Silenos Officer
		21364, // Varka Silenos Seer
		21365, // Varka Silenos Great Magus
		21366, // Varka Silenos General
		21368, // Varka Silenos Great Seer
		21369, // Varka's Commander
		21370, // Varka's Elite Guard
		21371, // Varka's Head Magus
		21372, // Varka's Head Guard
		21373, // Varka's Prophet
		21374, // Prophet's Guard
		21375, // Disciple of Prophet
	};
	
	// Items
	private static final int KEY = 1661;
	private static final int STOLEN_GREEN_TOTEM = 7237;
	private static final int WISDOM_STONE = 7081;
	private static final int GREEN_TOTEM = 7238;
	private static final int[] KETRA_MARKS =
	{
		7211, // Mark of Ketra's Alliance - Level 1
		7212, // Mark of Ketra's Alliance - Level 2
		7213, // Mark of Ketra's Alliance - Level 3
		7214, // Mark of Ketra's Alliance - Level 4
		7215, // Mark of Ketra's Alliance - Level 5
	};
	
	// Skills
	private static final SkillHolder GOW = new SkillHolder(4547, 1); // Gaze of Watcher
	private static final SkillHolder DISPEL_GOW = new SkillHolder(4548, 1); // Quest - Dispel Watcher Gaze
	// Misc
	private static final int MIN_LEVEL = 74;
	
	public Q00609_MagicalPowerOfWaterPart1()
	{
		super(609, "Magical Power of Water - Part 1");
		addStartNpc(WAHKAN);
		addTalkId(ASEFA, WAHKAN, UDANS_BOX);
		addAttackId(VARKA_MOBS);
		registerQuestItems(STOLEN_GREEN_TOTEM);
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
			case "31371-02.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "open_box":
			{
				if (!hasQuestItems(player, KEY))
				{
					htmltext = "31561-02.html";
				}
				else if (qs.isCond(2))
				{
					if (qs.isSet("spawned"))
					{
						takeItems(player, KEY, 1);
						htmltext = "31561-04.html";
					}
					else
					{
						giveItems(player, STOLEN_GREEN_TOTEM, 1);
						takeItems(player, KEY, 1);
						qs.setCond(3, true);
						htmltext = "31561-03.html";
					}
				}
				break;
			}
			case "eye_despawn":
			{
				npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, "Udan has already seen your face!"));
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
			final Npc eye = addSpawn(UDANS_EYE, npc);
			eye.broadcastPacket(new NpcSay(eye, ChatType.NPC_GENERAL, "You can't avoid the eyes of Udan!"));
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
			case WAHKAN:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? (hasAtLeastOneQuestItem(player, KETRA_MARKS)) ? "31371-01.htm" : "31371-00a.html" : "31371-00b.html";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(1))
						{
							htmltext = "31371-03.html";
						}
						break;
					}
				}
				break;
			}
			case ASEFA:
			{
				if (qs.isStarted())
				{
					switch (qs.getCond())
					{
						case 1:
						{
							htmltext = "31372-01.html";
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
								htmltext = "31372-03.html";
							}
							else
							{
								htmltext = "31372-02.html";
							}
							break;
						}
						case 3:
						{
							giveItems(player, GREEN_TOTEM, 1);
							giveItems(player, WISDOM_STONE, 1);
							qs.exitQuest(true, true);
							htmltext = "31372-04.html";
							break;
						}
					}
				}
				break;
			}
			case UDANS_BOX:
			{
				if (qs.isCond(2))
				{
					htmltext = "31561-01.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
}
