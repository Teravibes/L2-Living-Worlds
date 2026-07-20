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
package org.l2jmobius.gameserver.model.interfaces;

import org.l2jmobius.gameserver.model.Location;

/**
 * Object world location storage and update interface.
 * @author Zoey76
 */
public interface IPositionable extends ILocational
{
	/**
	 * Sets all three coordinates of this object.
	 * @param x the new X coordinate
	 * @param y the new Y coordinate
	 * @param z the new Z coordinate
	 */
	void setXYZ(int x, int y, int z);
	
	/**
	 * Sets all three coordinates of this object.
	 * @param loc the object whose coordinates to use
	 */
	void setXYZ(ILocational loc);
	
	/**
	 * Sets the heading of this object.
	 * @param heading the new heading
	 */
	void setHeading(int heading);
	
	/**
	 * Changes the instance zone ID of this object.
	 * @param instanceId the ID of the instance zone to put this object in (0 - not in any instance)
	 */
	void setInstanceId(int instanceId);
	
	/**
	 * Changes the location of this object.
	 * @param loc the new location
	 */
	void setLocation(Location loc);
}
