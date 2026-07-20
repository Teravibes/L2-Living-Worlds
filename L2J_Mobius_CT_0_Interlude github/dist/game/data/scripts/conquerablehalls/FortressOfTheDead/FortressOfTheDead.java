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
package conquerablehalls.FortressOfTheDead;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Fortress of the Dead clan hall siege script.
 * @author BiggBoss
 */
public class FortressOfTheDead extends ClanHallSiegeEngine
{
	private static final int LIDIA = 35629;
	private static final int ALFRED = 35630;
	private static final int GISELLE = 35631;
	
	private static Map<Integer, Integer> _damageToLidia = new HashMap<>();
	
	public FortressOfTheDead()
	{
		super(FORTRESS_OF_DEAD);
		addKillId(LIDIA);
		addKillId(ALFRED);
		addKillId(GISELLE);
		
		addSpawnId(LIDIA);
		addSpawnId(ALFRED);
		addSpawnId(GISELLE);
		
		addAttackId(LIDIA);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == LIDIA)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Hmm, those who are not of the bloodline are coming this way to take over the castle?! Humph! The bitter grudges of the dead. You must not make light of their power!");
		}
		else if (npc.getId() == ALFRED)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Heh Heh... I see that the feast has begun! Be wary! The curse of the Hellmann family has poisoned this land!");
		}
		else if (npc.getId() == GISELLE)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Arise, my faithful servants! You, my people who have inherited the blood. It is the calling of my daughter. The feast of blood will now begin!");
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return;
		}
		
		synchronized (this)
		{
			final Clan clan = attacker.getClan();
			if ((clan != null) && checkIsAttacker(clan))
			{
				final int id = clan.getId();
				if ((id > 0) && _damageToLidia.containsKey(id))
				{
					int newDamage = _damageToLidia.get(id);
					newDamage += damage;
					_damageToLidia.put(id, newDamage);
				}
				else
				{
					_damageToLidia.put(id, damage);
				}
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
		
		final int npcId = npc.getId();
		if ((npcId == ALFRED) || (npcId == GISELLE))
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Aargh...! If I die, then the magic force field of blood will...!");
		}
		
		if (npcId == LIDIA)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, "Grarr! For the next 2 minutes or so, the game arena are will be cleaned. Throw any items you don't need to the floor now.");
			_missionAccomplished = true;
			synchronized (this)
			{
				cancelSiegeTask();
				endSiege();
			}
		}
	}
	
	@Override
	public Clan getWinner()
	{
		int counter = 0;
		int damagest = 0;
		for (Entry<Integer, Integer> e : _damageToLidia.entrySet())
		{
			final int damage = e.getValue();
			if (damage > counter)
			{
				counter = damage;
				damagest = e.getKey();
			}
		}
		
		return ClanTable.getInstance().getClan(damagest);
	}
	
	@Override
	public void startSiege()
	{
		// Siege must start at night
		final int hoursLeft = (GameTimeTaskManager.getInstance().getGameTime() / 60) % 24;
		if ((hoursLeft < 0) || (hoursLeft > 6))
		{
			cancelSiegeTask();
			final long scheduleTime = (24 - hoursLeft) * 10 * 60000;
			_siegeTask = ThreadPool.schedule(new SiegeStarts(), scheduleTime);
		}
		else
		{
			super.startSiege();
		}
	}
	
	public static void main(String[] args)
	{
		new FortressOfTheDead();
	}
}
