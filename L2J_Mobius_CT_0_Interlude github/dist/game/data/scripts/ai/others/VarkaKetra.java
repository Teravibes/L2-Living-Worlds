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
package ai.others;

import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.LocationUtil;

import quests.Q00605_AllianceWithKetraOrcs.Q00605_AllianceWithKetraOrcs;
import quests.Q00606_BattleAgainstVarkaSilenos.Q00606_BattleAgainstVarkaSilenos;
import quests.Q00607_ProveYourCourageKetra.Q00607_ProveYourCourageKetra;
import quests.Q00608_SlayTheEnemyCommanderKetra.Q00608_SlayTheEnemyCommanderKetra;
import quests.Q00609_MagicalPowerOfWaterPart1.Q00609_MagicalPowerOfWaterPart1;
import quests.Q00610_MagicalPowerOfWaterPart2.Q00610_MagicalPowerOfWaterPart2;
import quests.Q00611_AllianceWithVarkaSilenos.Q00611_AllianceWithVarkaSilenos;
import quests.Q00612_BattleAgainstKetraOrcs.Q00612_BattleAgainstKetraOrcs;
import quests.Q00613_ProveYourCourageVarka.Q00613_ProveYourCourageVarka;
import quests.Q00614_SlayTheEnemyCommanderVarka.Q00614_SlayTheEnemyCommanderVarka;
import quests.Q00615_MagicalPowerOfFirePart1.Q00615_MagicalPowerOfFirePart1;
import quests.Q00616_MagicalPowerOfFirePart2.Q00616_MagicalPowerOfFirePart2;

/**
 * Varka Silenos Barracks and Ketra Orc Outpost AI.
 * @author malyelfik
 */
public class VarkaKetra extends Script
{
	// Monsters
	private static final int[] KETRA =
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
		25299, // Ketra's Hero Hekaton (Raid Boss)
		25302, // Ketra's Commander Tayr (Raid Boss)
		25305, // Ketra's Chief Brakki (Raid Boss)
		25306, // Soul of Fire Nastron (Raid Boss)
	};
	private static final int[] VARKA =
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
		25309, // Varka's Hero Shadith (Raid Boss)
		25312, // Varka's Commander Mos (Raid Boss)
		25315, // Varka's Chief Horus (Raid Boss)
		25316, // Soul of Water Ashutar (Raid Boss)
	};
	
	// Items
	private static final int[] KETRA_MARKS =
	{
		7211, // Mark of Ketra's Alliance - Level 1
		7212, // Mark of Ketra's Alliance - Level 2
		7213, // Mark of Ketra's Alliance - Level 3
		7214, // Mark of Ketra's Alliance - Level 4
		7215, // Mark of Ketra's Alliance - Level 5
	};
	private static final int[] VARKA_MARKS =
	{
		7221, // Mark of Varka's Alliance - Level 1
		7222, // Mark of Varka's Alliance - Level 2
		7223, // Mark of Varka's Alliance - Level 3
		7224, // Mark of Varka's Alliance - Level 4
		7225, // Mark of Varka's Alliance - Level 5
	};
	
	// Quests
	private static final String[] KETRA_QUESTS =
	{
		Q00605_AllianceWithKetraOrcs.class.getSimpleName(),
		Q00606_BattleAgainstVarkaSilenos.class.getSimpleName(),
		Q00607_ProveYourCourageKetra.class.getSimpleName(),
		Q00608_SlayTheEnemyCommanderKetra.class.getSimpleName(),
		Q00609_MagicalPowerOfWaterPart1.class.getSimpleName(),
		Q00610_MagicalPowerOfWaterPart2.class.getSimpleName()
	};
	private static final String[] VARKA_QUESTS =
	{
		Q00611_AllianceWithVarkaSilenos.class.getSimpleName(),
		Q00612_BattleAgainstKetraOrcs.class.getSimpleName(),
		Q00613_ProveYourCourageVarka.class.getSimpleName(),
		Q00614_SlayTheEnemyCommanderVarka.class.getSimpleName(),
		Q00615_MagicalPowerOfFirePart1.class.getSimpleName(),
		Q00616_MagicalPowerOfFirePart2.class.getSimpleName()
	};
	
	private VarkaKetra()
	{
		addKillId(KETRA);
		addKillId(VARKA);
		addNpcHateId(KETRA);
		addNpcHateId(VARKA);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		if (LocationUtil.checkIfInRange(1500, player, npc, false))
		{
			if (ArrayUtil.contains(KETRA, npc.getId()) && hasAtLeastOneQuestItem(player, KETRA_MARKS))
			{
				decreaseAlliance(player, KETRA_MARKS);
				exitQuests(player, KETRA_QUESTS);
			}
			else if (ArrayUtil.contains(VARKA, npc.getId()) && hasAtLeastOneQuestItem(player, VARKA_MARKS))
			{
				decreaseAlliance(player, VARKA_MARKS);
				exitQuests(player, VARKA_QUESTS);
			}
		}
	}
	
	private void decreaseAlliance(Player player, int[] marks)
	{
		for (int i = 0; i < marks.length; i++)
		{
			if (hasQuestItems(player, marks[i]))
			{
				takeItems(player, marks[i], -1);
				if (i > 0)
				{
					giveItems(player, marks[i - 1], 1);
				}
				return;
			}
		}
	}
	
	private void exitQuests(Player player, String[] quests)
	{
		for (String quest : quests)
		{
			final QuestState qs = player.getQuestState(quest);
			if ((qs != null) && qs.isStarted())
			{
				qs.exitQuest(true);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
	}
	
	@Override
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		if (ArrayUtil.contains(KETRA, mob.getId()))
		{
			return !hasAtLeastOneQuestItem(player, KETRA_MARKS);
		}
		
		return !hasAtLeastOneQuestItem(player, VARKA_MARKS);
	}
	
	public static void main(String[] args)
	{
		new VarkaKetra();
	}
}
