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
package org.l2jmobius.gameserver.geoengine.geodata;

/**
 * @author HorridoJoho
 */
public interface IBlock
{
	int TYPE_FLAT = 0;
	int TYPE_COMPLEX = 1;
	int TYPE_MULTILAYER = 2;
	
	/** Cells in a block on the x axis */
	int BLOCK_CELLS_X = 8;
	/** Cells in a block on the y axis */
	int BLOCK_CELLS_Y = 8;
	/** Cells in a block */
	int BLOCK_CELLS = BLOCK_CELLS_X * BLOCK_CELLS_Y;
	
	boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe);
	
	void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe);
	
	void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe);
	
	short getNearestNswe(int geoX, int geoY, int worldZ);
	
	int getNearestZ(int geoX, int geoY, int worldZ);
	
	int getNextLowerZ(int geoX, int geoY, int worldZ);
	
	int getNextHigherZ(int geoX, int geoY, int worldZ);
}
