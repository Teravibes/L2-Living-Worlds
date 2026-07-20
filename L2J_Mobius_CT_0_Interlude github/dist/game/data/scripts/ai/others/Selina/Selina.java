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
package ai.others.Selina;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * Mercenary Medic Selina AI.
 * @author Zoey76
 */
public class Selina extends Script
{
	// NPC
	private static final int SELINA = 31556;
	
	// Items
	private static final int GOLDEN_RAM_BADGE_RECRUIT = 7246;
	private static final int GOLDEN_RAM_BADGE_SOLDIER = 7247;
	private static final int GOLDEN_RAM_COIN = 7251;
	
	// Skills
	private static final Map<String, BuffHolder> BUFFS = new HashMap<>();
	static
	{
		BUFFS.put("4359", new BuffHolder(4359, 2, 2)); // Focus
		BUFFS.put("4360", new BuffHolder(4360, 2, 2)); // Death Whisper
		BUFFS.put("4345", new BuffHolder(4345, 3, 3)); // Might
		BUFFS.put("4355", new BuffHolder(4355, 2, 3)); // Acumen
		BUFFS.put("4352", new BuffHolder(4352, 1, 3)); // Berserker Spirit
		BUFFS.put("4354", new BuffHolder(4354, 2, 3)); // Vampiric Rage
		BUFFS.put("4356", new BuffHolder(4356, 1, 6)); // Empower
		BUFFS.put("4357", new BuffHolder(4357, 2, 6)); // Haste
	}
	
	public Selina()
	{
		addStartNpc(SELINA);
		addTalkId(SELINA);
		addFirstTalkId(SELINA);
		addSpellFinishedId(SELINA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final BuffHolder buff = BUFFS.get(event);
		if (buff != null)
		{
			if ((getQuestItemsCount(player, GOLDEN_RAM_COIN) >= buff.getCost()))
			{
				castSkill(npc, player, buff);
				return super.onEvent(event, npc, player);
			}
		}
		else
		{
			LOGGER.warning(Selina.class.getSimpleName() + " AI: player " + player + " sent invalid bypass: " + event);
		}
		
		return "31556-02.html";
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext;
		if (hasQuestItems(player, GOLDEN_RAM_BADGE_SOLDIER))
		{
			htmltext = "31556-08.html";
		}
		else if (hasQuestItems(player, GOLDEN_RAM_BADGE_RECRUIT))
		{
			htmltext = "31556-01.html";
		}
		else
		{
			htmltext = "31556-09.html";
		}
		
		return htmltext;
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final BuffHolder buff = BUFFS.get(Integer.toString(skill.getId()));
		if (buff != null)
		{
			takeItems(player, GOLDEN_RAM_COIN, buff.getCost());
		}
	}
	
	public static void main(String[] args)
	{
		new Selina();
	}
	
	private static class BuffHolder extends SkillHolder
	{
		private final int _cost;
		
		public BuffHolder(int skillId, int skillLevel, int cost)
		{
			super(skillId, skillLevel);
			_cost = cost;
		}
		
		public int getCost()
		{
			return _cost;
		}
	}
}
