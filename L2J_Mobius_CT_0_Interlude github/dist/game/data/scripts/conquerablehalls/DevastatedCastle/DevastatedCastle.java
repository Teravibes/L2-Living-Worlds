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
package conquerablehalls.DevastatedCastle;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Devastated Castle clan hall siege script.
 * @author BiggBoss
 */
public class DevastatedCastle extends ClanHallSiegeEngine
{
	private static final int GUSTAV = 35410;
	private static final int MIKHAIL = 35409;
	private static final int DIETRICH = 35408;
	
	private final Map<Integer, Integer> _damageToGustav = new ConcurrentHashMap<>();
	
	private DevastatedCastle()
	{
		super(DEVASTATED_CASTLE);
		addKillId(GUSTAV);
		addSpawnId(MIKHAIL, DIETRICH);
		addAttackId(GUSTAV);
	}
	
	@Override
	public void onSiegeStarts()
	{
		_damageToGustav.clear();
		super.onSiegeStarts();
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == MIKHAIL)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Glory to Aden, the Kingdom of the Lion! Glory to Sir Gustav, our immortal lord!");
		}
		else if (npc.getId() == DIETRICH)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Soldiers of Gustav, go forth and destroy the invaders!");
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return;
		}
		
		final Clan clan = attacker.getClan();
		if ((clan != null) && checkIsAttacker(clan))
		{
			_damageToGustav.merge(clan.getId(), damage, Integer::sum);
		}
		
		synchronized (this)
		{
			if (!npc.isCastingNow() && (npc.getCurrentHp() < (npc.getMaxHp() / 12)))
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, "This is unbelievable! Have I really been defeated? I shall return and take your head!");
				npc.getAI().setIntention(Intention.CAST, SkillData.getInstance().getSkill(4235, 1), npc);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return;
		}
		
		_missionAccomplished = true;
		cancelSiegeTask();
		endSiege();
	}
	
	@Override
	public Clan getWinner()
	{
		if (_damageToGustav.isEmpty())
		{
			return null;
		}
		
		final int clanId = Collections.max(_damageToGustav.entrySet(), Map.Entry.comparingByValue()).getKey();
		return ClanTable.getInstance().getClan(clanId);
	}
	
	public static void main(String[] args)
	{
		new DevastatedCastle();
	}
}
