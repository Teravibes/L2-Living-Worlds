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
package org.l2jmobius.gameserver.model.olympiad;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author GodKratos
 */
class OlympiadStadium
{
	private boolean _freeToUse = true;
	private final int[] _coords = new int[3];
	private final List<Player> _spectators;
	
	public boolean isFreeToUse()
	{
		return _freeToUse;
	}
	
	public void setStadiaBusy()
	{
		_freeToUse = false;
	}
	
	public void setStadiaFree()
	{
		_freeToUse = true;
	}
	
	public int[] getCoordinates()
	{
		return _coords;
	}
	
	public OlympiadStadium(int x, int y, int z)
	{
		_coords[0] = x;
		_coords[1] = y;
		_coords[2] = z;
		_spectators = new CopyOnWriteArrayList<>();
	}
	
	protected void addSpectator(int id, Player spec, boolean storeCoords)
	{
		final Location loc = new Location(getCoordinates()[0], getCoordinates()[1], getCoordinates()[2]);
		spec.enterOlympiadObserverMode(loc, id, storeCoords);
		_spectators.add(spec);
	}
	
	protected List<Player> getSpectators()
	{
		return _spectators;
	}
	
	protected void removeSpectator(Player spec)
	{
		if ((_spectators != null) && _spectators.contains(spec))
		{
			_spectators.remove(spec);
		}
	}
}
