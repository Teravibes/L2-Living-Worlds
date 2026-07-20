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
package ai.areas.KetraOrcOutpust.KetraOrcSupport;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.teleporter.TeleportHolder;

/**
 * Ketra Orc Support AI.<br>
 * Original Jython script by Emperorc and Kerberos.
 * @authors Nyaran
 */
public class KetraOrcSupport extends Script
{
	private static class BuffsData
	{
		private final int _skill;
		private final int _cost;
		
		public BuffsData(int skill, int cost)
		{
			_skill = skill;
			_cost = cost;
		}
		
		public Skill getSkill()
		{
			return SkillData.getInstance().getSkill(_skill, 1);
		}
		
		public int getCost()
		{
			return _cost;
		}
	}
	
	// NPCs
	private static final int KADUN = 31370; // Hierarch
	private static final int WAHKAN = 31371; // Messenger
	private static final int ASEFA = 31372; // Soul Guide
	private static final int ATAN = 31373; // Grocer
	private static final int JAFF = 31374; // Warehouse Keeper
	private static final int JUMARA = 31375; // Trader
	private static final int KURFA = 31376; // Gate Keeper
	
	// Items
	private static final int HORN = 7186;
	private static final int[] KETRA_MARKS =
	{
		7211, // Mark of Ketra's Alliance - Level 1
		7212, // Mark of Ketra's Alliance - Level 2
		7213, // Mark of Ketra's Alliance - Level 3
		7214, // Mark of Ketra's Alliance - Level 4
		7215, // Mark of Ketra's Alliance - Level 5
	};
	
	// Misc
	private static final Map<Integer, BuffsData> BUFF = new HashMap<>();
	static
	{
		BUFF.put(1, new BuffsData(4359, 2)); // Focus: Requires 2 Buffalo Horns
		BUFF.put(2, new BuffsData(4360, 2)); // Death Whisper: Requires 2 Buffalo Horns
		BUFF.put(3, new BuffsData(4345, 3)); // Might: Requires 3 Buffalo Horns
		BUFF.put(4, new BuffsData(4355, 3)); // Acumen: Requires 3 Buffalo Horns
		BUFF.put(5, new BuffsData(4352, 3)); // Berserker: Requires 3 Buffalo Horns
		BUFF.put(6, new BuffsData(4354, 3)); // Vampiric Rage: Requires 3 Buffalo Horns
		BUFF.put(7, new BuffsData(4356, 6)); // Empower: Requires 6 Buffalo Horns
		BUFF.put(8, new BuffsData(4357, 6)); // Haste: Requires 6 Buffalo Horns
	}
	
	private KetraOrcSupport()
	{
		addFirstTalkId(KADUN, WAHKAN, ASEFA, ATAN, JAFF, JUMARA, KURFA);
		addTalkId(ASEFA, KURFA, JAFF);
		addStartNpc(KURFA, JAFF);
	}
	
	private int getAllianceLevel(Player player)
	{
		for (int i = 0; i < KETRA_MARKS.length; i++)
		{
			if (hasQuestItems(player, KETRA_MARKS[i]))
			{
				return i + 1;
			}
		}
		
		return 0;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (StringUtil.isNumeric(event) && BUFF.containsKey(Integer.parseInt(event)))
		{
			final BuffsData buff = BUFF.get(Integer.parseInt(event));
			if (getQuestItemsCount(player, HORN) >= buff.getCost())
			{
				takeItems(player, HORN, buff.getCost());
				npc.setTarget(player);
				npc.doCast(buff.getSkill());
				npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
			}
			else
			{
				htmltext = "31372-02.html";
			}
		}
		else if (event.equals("TELEPORT"))
		{
			final String listName = "list" + getAllianceLevel(player);
			final TeleportHolder holder = TeleporterData.getInstance().getHolder(npc.getId(), listName);
			if (holder != null)
			{
				holder.showTeleportList(player, npc);
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final int AllianceLevel = getAllianceLevel(player);
		switch (npc.getId())
		{
			case KADUN:
			{
				htmltext = (AllianceLevel > 0) ? "31370-friend.html" : "31370-no.html";
				break;
			}
			case WAHKAN:
			{
				htmltext = (AllianceLevel > 0) ? "31371-friend.html" : "31371-no.html";
				break;
			}
			case ASEFA:
			{
				htmltext = (AllianceLevel > 0) ? (AllianceLevel < 3) ? "31372-01.html" : "31372-04.html" : "31372-03.html";
				break;
			}
			case ATAN:
			{
				htmltext = (AllianceLevel > 0) ? "31373-friend.html" : "31373-no.html";
				break;
			}
			case JAFF:
			{
				htmltext = (AllianceLevel > 0) ? (AllianceLevel == 1) ? "31374-01.html" : "31374-02.html" : "31374-no.html";
				break;
			}
			case JUMARA:
			{
				switch (AllianceLevel)
				{
					case 1:
					case 2:
					{
						htmltext = "31375-01.html";
						break;
					}
					case 3:
					case 4:
					{
						htmltext = "31375-02.html";
						break;
					}
					case 5:
					{
						htmltext = "31375-03.html";
						break;
					}
					default:
					{
						htmltext = "31375-no.html";
						break;
					}
				}
				break;
			}
			case KURFA:
			{
				switch (AllianceLevel)
				{
					case 1:
					case 2:
					case 3:
					{
						htmltext = "31376-01.html";
						break;
					}
					case 4:
					{
						htmltext = "31376-02.html";
						break;
					}
					case 5:
					{
						htmltext = "31376-03.html";
						break;
					}
					default:
					{
						htmltext = "31376-no.html";
						break;
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new KetraOrcSupport();
	}
}
