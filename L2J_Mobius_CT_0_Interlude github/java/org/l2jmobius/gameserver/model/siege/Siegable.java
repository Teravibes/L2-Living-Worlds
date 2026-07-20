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
package org.l2jmobius.gameserver.model.siege;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;

/**
 * @author JIV
 */
public interface Siegable
{
	void startSiege();
	
	void endSiege();
	
	SiegeClan getAttackerClan(int clanId);
	
	SiegeClan getAttackerClan(Clan clan);
	
	Collection<SiegeClan> getAttackerClans();
	
	List<Player> getAttackersInZone();
	
	boolean checkIsAttacker(Clan clan);
	
	SiegeClan getDefenderClan(int clanId);
	
	SiegeClan getDefenderClan(Clan clan);
	
	Collection<SiegeClan> getDefenderClans();
	
	boolean checkIsDefender(Clan clan);
	
	Set<Npc> getFlag(Clan clan);
	
	Calendar getSiegeDate();
	
	boolean giveFame();
	
	int getFameFrequency();
	
	int getFameAmount();
	
	void updateSiege();
}
