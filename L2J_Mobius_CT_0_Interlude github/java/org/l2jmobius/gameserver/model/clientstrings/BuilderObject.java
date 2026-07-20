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
package org.l2jmobius.gameserver.model.clientstrings;

/**
 * @author Forsaiken
 */
public class BuilderObject extends Builder
{
	private final int _index;
	
	BuilderObject(int id)
	{
		if ((id < 1) || (id > 9))
		{
			throw new RuntimeException("Illegal Id: " + id);
		}
		
		_index = id - 1;
	}
	
	@Override
	public String toString(Object param)
	{
		return param == null ? "null" : param.toString();
	}
	
	@Override
	public String toString(Object... params)
	{
		if ((params == null) || (params.length == 0))
		{
			return "null";
		}
		
		return params[0].toString();
	}
	
	@Override
	public int getIndex()
	{
		return _index;
	}
	
	@Override
	public String toString()
	{
		return "[PARAM-" + (_index + 1) + "]";
	}
}
