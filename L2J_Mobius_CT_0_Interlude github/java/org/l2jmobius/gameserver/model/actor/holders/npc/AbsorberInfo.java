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
package org.l2jmobius.gameserver.model.actor.holders.npc;

/**
 * @author xban1x
 */
public class AbsorberInfo
{
	private int _objectId;
	private double _absorbedHp;
	
	public AbsorberInfo(int objectId, double pAbsorbedHp)
	{
		_objectId = objectId;
		_absorbedHp = pAbsorbedHp;
	}
	
	public double getAbsorbedHp()
	{
		return _absorbedHp;
	}
	
	public void setAbsorbedHp(double absorbedHp)
	{
		_absorbedHp = absorbedHp;
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public void setObjectId(int objectId)
	{
		_objectId = objectId;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return (this == obj) || ((obj instanceof AbsorberInfo) && (((AbsorberInfo) obj).getObjectId() == _objectId));
	}
	
	@Override
	public int hashCode()
	{
		return _objectId;
	}
}
