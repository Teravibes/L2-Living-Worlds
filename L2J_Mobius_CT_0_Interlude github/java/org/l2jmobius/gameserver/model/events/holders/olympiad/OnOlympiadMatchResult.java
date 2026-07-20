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
package org.l2jmobius.gameserver.model.events.holders.olympiad;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.model.olympiad.CompetitionType;

/**
 * @author UnAfraid
 */
public class OnOlympiadMatchResult implements IBaseEvent
{
	private final Player _winner;
	private final Player _loser;
	private final CompetitionType _type;
	
	public OnOlympiadMatchResult(Player winner, Player looser, CompetitionType type)
	{
		_winner = winner;
		_loser = looser;
		_type = type;
	}
	
	public Player getWinner()
	{
		return _winner;
	}
	
	public Player getLoser()
	{
		return _loser;
	}
	
	public CompetitionType getCompetitionType()
	{
		return _type;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_OLYMPIAD_MATCH_RESULT;
	}
}
